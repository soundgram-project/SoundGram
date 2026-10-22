package soundgram.music.wave;

import android.util.LruCache;

import org.telegram.messenger.FileLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import soundgram.music.SmartRadioEngine;

/**
 * SeedSelector — Интеллектуальный селектор сид-треков для Моей волны.
 * 100% YouTube Music.
 * Поддерживает 19 жанров, 4 градации настроения и строгое распределение по эпохам:
 * - 2022-2026: 50% (10 треков)
 * - 2015-2022: 20% (4 трека)
 * - 2010-2015: 10% (2 трека)
 * - < 2010: 20% (4 трека)
 * Всего ровно 20 канонических треков на каждое настроение каждого жанра.
 * Все сиды резолвятся в живые ID YouTube Music с Lru-кэшированием в памяти.
 */
public final class SeedSelector {

    private static final String TAG = "SeedSelector";
    private static final Random sRandom = new Random();

    // LRU кэш для мгновенного получения videoId канонических треков
    private static final LruCache<String, String> sSeedIdCache = new LruCache<>(1024);
    private static final ExecutorService sResolveExecutor = Executors.newFixedThreadPool(4, r -> new Thread(r, "seed-resolver"));

    public static final int MOOD_CALM = 0;
    public static final int MOOD_STEADY = 1;
    public static final int MOOD_ENERGETIC = 2;
    public static final int MOOD_MAX = 3;

    public static int parseMoodIndex(String mood, float progress) {
        if (progress >= 0f && progress <= 1f) {
            if (progress < 0.25f) return MOOD_CALM;
            if (progress < 0.50f) return MOOD_STEADY;
            if (progress < 0.75f) return MOOD_ENERGETIC;
            return MOOD_MAX;
        }
        if (mood != null) {
            String m = mood.toLowerCase();
            if (m.contains("спокой") || m.contains("calm") || m.contains("тихо") || m.contains("груст")) return MOOD_CALM;
            if (m.contains("ровн") || m.contains("steady") || m.contains("нейтр")) return MOOD_STEADY;
            if (m.contains("бодр") || m.contains("energetic") || m.contains("драйв")) return MOOD_ENERGETIC;
            if (m.contains("максим") || m.contains("max") || m.contains("взрыв") || m.contains("мощн")) return MOOD_MAX;
        }
        return MOOD_ENERGETIC;
    }

    public static String normalizeGenre(String rawGenre) {
        if (rawGenre == null || rawGenre.trim().isEmpty()) return "default";
        String g = rawGenre.toLowerCase().trim().replace("-", "").replace(" ", "");
        if (g.contains("любой") || g.contains("default") || g.contains("all")) return "default";
        if (g.contains("фонк") || g.contains("phonk")) return "фонк";
        if (g.contains("рэп") || g.contains("rap")) return "рэп";
        if (g.contains("хипхоп") || g.contains("hiphop")) return "хип-хоп";
        if (g.contains("рок") || g.contains("rock")) return "рок";
        if (g.contains("метал") || g.contains("metal")) return "металл";
        if (g.contains("постпанк") || g.contains("postpunk")) return "пост-панк";
        if (g.contains("лофай") || g.contains("lofi")) return "ло-фай";
        if (g.contains("поп") || g.contains("pop")) return "поп";
        if (g.contains("электроник") || g.contains("electronic") || g.contains("edm")) return "электроника";
        if (g.contains("альтернатив") || g.contains("alternative")) return "альтернатива";
        if (g.contains("инди") || g.contains("indie")) return "инди";
        if (g.contains("гиперпоп") || g.contains("hyperpop")) return "гиперпоп";
        if (g.contains("эморэп") || g.contains("emorap")) return "эмо-рэп";
        if (g.contains("кпоп") || g.contains("kpop")) return "к-поп";
        if (g.contains("синтвейв") || g.contains("synthwave") || g.contains("retrowave")) return "синтвейв";
        if (g.contains("джаз") || g.contains("jazz")) return "джаз";
        if (g.contains("блюз") || g.contains("blues")) return "блюз";
        if (g.contains("классик") || g.contains("classic")) return "классика";
        if (g.contains("эмбиент") || g.contains("ambient")) return "эмбиент";
        return rawGenre.trim().toLowerCase();
    }

    /**
     * Канонический сид-трек жанра: Артист и Название.
     */
    public static class CanonicalSeed {
        public final String artist;
        public final String title;

        public CanonicalSeed(String artist, String title) {
            this.artist = artist;
            this.title = title;
        }

        public String getQuery() {
            return (artist + " - " + title).trim();
        }

        @Override
        public String toString() {
            return getQuery();
        }
    }

    /**
     * Корзина эпох для конкретного настроения жанра.
     * Ровно 20 сид-треков на каждое настроение:
     * - era2022_2026: 10 треков (50%)
     * - era2015_2022: 4 трека (20%)
     * - era2010_2015: 2 трека (10%)
     * - eraPre2010:   4 трека (20%)
     */
    public static class EraSeedBucket {
        public final CanonicalSeed[] era2022_2026;
        public final CanonicalSeed[] era2015_2022;
        public final CanonicalSeed[] era2010_2015;
        public final CanonicalSeed[] eraPre2010;

        public EraSeedBucket(CanonicalSeed[] modern, CanonicalSeed[] late10s, CanonicalSeed[] early10s, CanonicalSeed[] classics) {
            this.era2022_2026 = modern;
            this.era2015_2022 = late10s;
            this.era2010_2015 = early10s;
            this.eraPre2010 = classics;
        }

        public CanonicalSeed sampleWeightedSeed() {
            int roll = sRandom.nextInt(100);
            if (roll < 50 && era2022_2026.length > 0) {
                return era2022_2026[sRandom.nextInt(era2022_2026.length)];
            } else if (roll < 70 && era2015_2022.length > 0) {
                return era2015_2022[sRandom.nextInt(era2015_2022.length)];
            } else if (roll < 80 && era2010_2015.length > 0) {
                return era2010_2015[sRandom.nextInt(era2010_2015.length)];
            } else if (eraPre2010.length > 0) {
                return eraPre2010[sRandom.nextInt(eraPre2010.length)];
            } else if (era2022_2026.length > 0) {
                return era2022_2026[sRandom.nextInt(era2022_2026.length)];
            }
            return null;
        }

        public List<CanonicalSeed> getAllSeeds() {
            List<CanonicalSeed> list = new ArrayList<>();
            if (era2022_2026 != null) Collections.addAll(list, era2022_2026);
            if (era2015_2022 != null) Collections.addAll(list, era2015_2022);
            if (era2010_2015 != null) Collections.addAll(list, era2010_2015);
            if (eraPre2010 != null) Collections.addAll(list, eraPre2010);
            return list;
        }

        public List<CanonicalSeed> pickSampleSeeds(int count) {
            List<CanonicalSeed> chosen = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            int attempts = 0;
            while (chosen.size() < count && attempts < 20) {
                attempts++;
                CanonicalSeed s = sampleWeightedSeed();
                if (s != null && seen.add(s.getQuery())) {
                    chosen.add(s);
                }
            }
            if (chosen.isEmpty() && era2022_2026.length > 0) {
                chosen.add(era2022_2026[0]);
            }
            return chosen;
        }
    }

    private static CanonicalSeed s(String artist, String title) {
        return new CanonicalSeed(artist, title);
    }

    private static final Map<String, EraSeedBucket[]> GENRE_MAP = new HashMap<>();

    static {
        // ─────────────────────────────────────────────────────────────────────
        // 1. ФОНК
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("фонк", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("LXST CXNTURY", "ODIUM"), s("DVRST", "Dream Space"), s("KEDR", "Midnight Drift"),
                                s("PlayaPhonk", "Keraunos (Slowed)"), s("Hensonn", "Flare"), s("Øneheart", "snowfall"),
                                s("Pharmacist", "Overdose (Chill)"), s("Interworld", "Metamorphosis (Slowed)"),
                                s("Kordhell", "Land of Fire (Slowed)"), s("Tevvez", "Mystique")
                        },
                        new CanonicalSeed[]{
                                s("Soudiere", "Late Night"), s("DJ Smokey", "Yung Vamp"),
                                s("Roland Jones", "Hollow Point"), s("DJ Yung Vamp", "I Need More")
                        },
                        new CanonicalSeed[]{
                                s("SpaceGhostPurrp", "Mysterious Phonk"), s("Raider Klan", "Zone 3")
                        },
                        new CanonicalSeed[]{
                                s("Three 6 Mafia", "Late Nite Tip"), s("Tommy Wright III", "Meet Yo Maker"),
                                s("Koopsta Knicca", "Crucifix"), s("Project Pat", "Gorilla Pimp")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Ghostface Playa", "Why Not"), s("DVRST", "Close Eyes"), s("Kordhell", "Live Another Day"),
                                s("Interworld", "Rapture"), s("Hensonn", "Sahara"), s("Pharmacist", "North Memphis"),
                                s("KSLV Noh", "Chase"), s("PlayaPhonk", "Phonky Town"),
                                s("Twisted", "Worth Nothing"), s("Maniako", "Tokyo Drift Phonk")
                        },
                        new CanonicalSeed[]{
                                s("Freddie Dredd", "Cha Cha"), s("Baker Ya Maker", "Born 2 Lose"),
                                s("Devilish Trio", "Memories of Hell"), s("Soudiere", "Chrome")
                        },
                        new CanonicalSeed[]{
                                s("DJ Smokey", "Evil Wayz"), s("SpaceGhostPurrp", "God of Black")
                        },
                        new CanonicalSeed[]{
                                s("Three 6 Mafia", "Stay Fly"), s("Tommy Wright III", "Street Shit"),
                                s("Lord Infamous", "Anyone Out There"), s("Project Pat", "Chickenhead")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Kordhell", "Murder In My Mind"), s("Interworld", "Metamorphosis"), s("DVRST", "Automotivo"),
                                s("KSLV Noh", "Disaster"), s("SXMPRA", "COWBELL WARRIORS!"), s("Pharmacist", "Overdose"),
                                s("MUPP", "vendetta!"), s("PlayaPhonk", "Keraunos"),
                                s("Hensonn", "Sahara"), s("Kordhell", "Like Yx Hate Me")
                        },
                        new CanonicalSeed[]{
                                s("Ghostface Playa", "Swag"), s("Freddie Dredd", "GTG"),
                                s("Baker Ya Maker", "Devilish Intentions"), s("Redzed", "Rave in the Grave")
                        },
                        new CanonicalSeed[]{
                                s("DJ Smokey", "Kush Alienz"), s("SpaceGhostPurrp", "Raider Prayer")
                        },
                        new CanonicalSeed[]{
                                s("Three 6 Mafia", "Tear Da Club Up"), s("Tommy Wright III", "On the Run"),
                                s("Gangsta Boo", "Where Dem Dollas At"), s("Project Pat", "Out There")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Kordhell", "Killers From The Northside"), s("SXMPRA", "LOCK EM UP"), s("KSLV Noh", "Override"),
                                s("Maniako", "Extreme Drift"), s("Pharmacist", "Bamba"), s("Twisted", "Hardcore Cowbell"),
                                s("KSLV Noh", "Dynamic"), s("Interworld", "Raze"),
                                s("DVRST", "Komarovo Phonk"), s("Ghostface Playa", "Vengeance")
                        },
                        new CanonicalSeed[]{
                                s("Redzed", "Straight Outta Flames"), s("Freddie Dredd", "Evil Fantasy"),
                                s("Baker Ya Maker", "Grim"), s("Devilish Trio", "Southern Hostility")
                        },
                        new CanonicalSeed[]{
                                s("SpaceGhostPurrp", "Black God"), s("DJ Smokey", "Triple 6")
                        },
                        new CanonicalSeed[]{
                                s("Three 6 Mafia", "Hit A Muthafucka"), s("Tommy Wright III", "Killaz Don't Talk"),
                                s("Project Pat", "Blunt to My Lips"), s("Lord Infamous", "Damn I'm Crazed")
                        }
                )
        });

        // ─────────────────────────────────────────────────────────────────────
        // 2. РЭП И ХИП-ХОП
        // ─────────────────────────────────────────────────────────────────────
        EraSeedBucket[] rapBuckets = new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Kendrick Lamar", "Die Hard"), s("Drake", "Polar Opposites"), s("J. Cole", "Trae Tha Truth in Ibiza"),
                                s("Miyagi & Эндшпиль", "Silhouette"), s("Gunna", "rodeo dr"), s("Travis Scott", "TELEKINESIS"),
                                s("21 Savage", "redrum (slowed)"), s("Mac Miller", "The Spins"),
                                s("Saluki", "Огней"), s("Big Baby Tape", "Like A G6")
                        },
                        new CanonicalSeed[]{
                                s("Miyagi & Andy Panda", "Minor"), s("Kendrick Lamar", "LOVE."),
                                s("Post Malone", "White Iverson"), s("J. Cole", "Wet Dreamz")
                        },
                        new CanonicalSeed[]{
                                s("Drake", "Hold On, We're Going Home"), s("Kendrick Lamar", "Bitch, Don't Kill My Vibe")
                        },
                        new CanonicalSeed[]{
                                s("Nas", "The World Is Yours"), s("The Notorious B.I.G.", "Juicy"),
                                s("Snoop Dogg", "Gin and Juice"), s("2Pac", "Dear Mama")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Kendrick Lamar", "Not Like Us"), s("Drake", "First Person Shooter"), s("Metro Boomin", "Superhero"),
                                s("Big Baby Tape", "Million"), s("Gunna", "fukumean"), s("21 Savage", "née-nah"),
                                s("Future", "Like That"), s("Friendly Thug 52 NGG", "Calldown"),
                                s("Travis Scott", "MY EYES"), s("Oxxxymiron", "Красота и Уродство")
                        },
                        new CanonicalSeed[]{
                                s("Travis Scott", "goosebumps"), s("Kendrick Lamar", "HUMBLE."),
                                s("Kizaru", "Дежавю"), s("Big Baby Tape", "Gimme The Loot")
                        },
                        new CanonicalSeed[]{
                                s("A$AP Rocky", "Praise The Lord"), s("Drake", "Started From the Bottom")
                        },
                        new CanonicalSeed[]{
                                s("50 Cent", "In Da Club"), s("Eminem", "The Real Slim Shady"),
                                s("Dr. Dre", "Still D.R.E."), s("The Notorious B.I.G.", "Hypnotize")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Travis Scott", "FE!N"), s("Metro Boomin", "Type Shit"), s("21 Savage", "redrum"),
                                s("Playboi Carti", "2024"), s("Yeat", "Breathe"), s("Big Baby Tape", "DYNASTY"),
                                s("Kai Angel", "Slum Dogs"), s("Kizaru", "Mirror"),
                                s("Future", "Young Metro"), s("Chief Keef", "Tony Montana Flow")
                        },
                        new CanonicalSeed[]{
                                s("Travis Scott", "SICKO MODE"), s("Playboi Carti", "Magnolia"),
                                s("XXXTENTACION", "Look At Me!"), s("Sheck Wes", "Mo Bamba")
                        },
                        new CanonicalSeed[]{
                                s("Chief Keef", "Love Sosa"), s("Kanye West", "Black Skinhead")
                        },
                        new CanonicalSeed[]{
                                s("DMX", "X Gon' Give It To Ya"), s("Eminem", "Lose Yourself"),
                                s("50 Cent", "Many Men"), s("M.O.P.", "Ante Up")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Playboi Carti", "CARNIVAL"), s("Yeat", "IDGAF"), s("Travis Scott", "SIRENZ"),
                                s("City Morgue", "Skull & Crossbones"), s("Kai Angel", "HEAVY METAL"), s("Viperr", "Bleed"),
                                s("21 Savage", "Glock in My Lap"), s("Ken Carson", "Fighting My Demons"),
                                s("Lil Uzi Vert", "Suicide Doors"), s("Jasiah", "Break Shit")
                        },
                        new CanonicalSeed[]{
                                s("Playboi Carti", "Stop Breathing"), s("City Morgue", "Shinners 13"),
                                s("XXXTENTACION", "Take A Step Back"), s("Ski Mask The Slump God", "Faucet Failure")
                        },
                        new CanonicalSeed[]{
                                s("Waka Flocka Flame", "Hard in da Paint"), s("Kanye West", "Blood on the Leaves")
                        },
                        new CanonicalSeed[]{
                                s("DMX", "Party Up"), s("Three 6 Mafia", "Who Run It"),
                                s("M.O.P.", "Cold as Ice"), s("Onyx", "Slam")
                        }
                )
        };
        GENRE_MAP.put("рэп", rapBuckets);
        GENRE_MAP.put("хип-хоп", rapBuckets);

        // ─────────────────────────────────────────────────────────────────────
        // 3. РОК
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("рок", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Linkin Park", "The Emptiness Machine (Acoustic)"), s("Fontaines D.C.", "In The Modern World"),
                                s("The Smile", "Friend of a Friend"), s("Deftones", "Ceremony (Acoustic)"),
                                s("Arctic Monkeys", "There'd Better Be A Mirrorball"), s("Twenty One Pilots", "The Craving"),
                                s("Green Day", "Father to a Son"), s("Blink-182", "ONE MORE TIME"),
                                s("Red Hot Chili Peppers", "Not the One"), s("The Killers", "Spirit")
                        },
                        new CanonicalSeed[]{
                                s("Linkin Park", "One More Light"), s("Radiohead", "Daydreaming"),
                                s("Arctic Monkeys", "Star Treatment"), s("Red Hot Chili Peppers", "Black Summer")
                        },
                        new CanonicalSeed[]{
                                s("Arctic Monkeys", "Do I Wanna Know?"), s("Coldplay", "Fix You")
                        },
                        new CanonicalSeed[]{
                                s("Nirvana", "Something In The Way"), s("Pink Floyd", "Wish You Were Here"),
                                s("Radiohead", "Creep (Acoustic)"), s("Led Zeppelin", "Stairway to Heaven")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Linkin Park", "The Emptiness Machine"), s("Blink-182", "EDGING"), s("Green Day", "Dilemma"),
                                s("The Rolling Stones", "Angry"), s("Foo Fighters", "Rescued"), s("Twenty One Pilots", "Overcompensate"),
                                s("Red Hot Chili Peppers", "Tippa My Tongue"), s("The Black Keys", "Beautiful People"),
                                s("Maneskin", "Gossip"), s("Queens of the Stone Age", "Emotion Sickness")
                        },
                        new CanonicalSeed[]{
                                s("Maneskin", "Beggin'"), s("Royal Blood", "Figure It Out"),
                                s("The Black Keys", "Lo/Hi"), s("Ghost", "Square Hammer")
                        },
                        new CanonicalSeed[]{
                                s("The Black Keys", "Lonely Boy"), s("Foo Fighters", "Rope")
                        },
                        new CanonicalSeed[]{
                                s("Linkin Park", "In the End"), s("Red Hot Chili Peppers", "Californication"),
                                s("The White Stripes", "Seven Nation Army"), s("Queen", "Bohemian Rhapsody")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Linkin Park", "Heavy Is the Crown"), s("Green Day", "The American Dream Is Killing Me"),
                                s("Blink-182", "DANCE WITH ME"), s("Sum 41", "Landmines"),
                                s("Avenged Sevenfold", "Nobody"), s("Bring Me The Horizon", "Kool-Aid"),
                                s("Falling In Reverse", "All My Life"), s("Papa Roach", "Cut The Line"),
                                s("Three Days Grace", "So Called Life"), s("Shinedown", "Planet Zero")
                        },
                        new CanonicalSeed[]{
                                s("Bring Me The Horizon", "Can You Feel My Heart"), s("Linkin Park", "What I've Done"),
                                s("The Offspring", "Let The Bad Times Roll"), s("Greta Van Fleet", "Highway Tune")
                        },
                        new CanonicalSeed[]{
                                s("Linkin Park", "Burn It Down"), s("Foo Fighters", "Walk")
                        },
                        new CanonicalSeed[]{
                                s("Nirvana", "Smells Like Teen Spirit"), s("AC/DC", "Back In Black"),
                                s("System of a Down", "Toxicity"), s("Linkin Park", "Numb")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Falling In Reverse", "Ronald"), s("Bring Me The Horizon", "AmEN!"),
                                s("Linkin Park", "Heavy Is the Crown"), s("Bad Omens", "ARTIFICIAL SUICIDE"),
                                s("Architects", "Seeing Red"), s("I Prevail", "Body Bag"),
                                s("Slipknot", "The Dying Song"), s("Motionless In White", "Slaughterhouse"),
                                s("Avenged Sevenfold", "Mattel"), s("Ghostemane", "Anti-Icon")
                        },
                        new CanonicalSeed[]{
                                s("Bring Me The Horizon", "Mantra"), s("Slipknot", "Unsainted"),
                                s("Falling In Reverse", "Popular Monster"), s("System of a Down", "Genocidal Humanoidz")
                        },
                        new CanonicalSeed[]{
                                s("Slipknot", "The Devil in I"), s("Avenged Sevenfold", "Hail to the King")
                        },
                        new CanonicalSeed[]{
                                s("System of a Down", "Chop Suey!"), s("Rage Against The Machine", "Killing In The Name"),
                                s("Metallica", "Master of Puppets"), s("Slipknot", "Duality")
                        }
                )
        });

        // ─────────────────────────────────────────────────────────────────────
        // 4. МЕТАЛЛ
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("металл", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Sleep Token", "Are You Really Okay?"), s("Lorna Shore", "Pain Remains I"),
                                s("Spiritbox", "Constance (Acoustic)"), s("Opeth", "§1"),
                                s("Katatonia", "Opaline"), s("Architects", "Burn Down My House"),
                                s("Avenged Sevenfold", "Cosmic"), s("Leprous", "Like a Sunken Ship"),
                                s("Ghost", "Darkness at the Heart of My Love"), s("Alcest", "L'Envol")
                        },
                        new CanonicalSeed[]{
                                s("Gojira", "The Chant"), s("Slipknot", "Snuff (Live)"),
                                s("Ghost", "He Is"), s("Alcest", "Protection")
                        },
                        new CanonicalSeed[]{
                                s("Opeth", "In My Time of Need"), s("Insomnium", "While We Sleep")
                        },
                        new CanonicalSeed[]{
                                s("Metallica", "Nothing Else Matters"), s("Iron Maiden", "Wasting Love"),
                                s("Judas Priest", "Beyond the Realms of Death"), s("Black Sabbath", "Solitude")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Judas Priest", "Panic Attack"), s("Megadeth", "We'll Be Back"),
                                s("Avenged Sevenfold", "Nobody"), s("Iron Maiden", "The Writing on the Wall"),
                                s("Saxon", "Hell, Fire and Damnation"), s("Accept", "Humanoid"),
                                s("Bruce Dickinson", "Afterglow of Ragnarok"), s("Ghost", "Spillways"),
                                s("In Flames", "Meet Your Maker"), s("Amon Amarth", "The Great Heathen Army")
                        },
                        new CanonicalSeed[]{
                                s("Judas Priest", "Lightning Strike"), s("Megadeth", "Dystopia"),
                                s("Ghost", "Square Hammer"), s("Iron Maiden", "Speed of Light")
                        },
                        new CanonicalSeed[]{
                                s("Megadeth", "Public Enemy No. 1"), s("Anthrax", "Fight 'Em 'Til You Can't")
                        },
                        new CanonicalSeed[]{
                                s("Metallica", "Enter Sandman"), s("Iron Maiden", "The Trooper"),
                                s("Judas Priest", "Breaking the Law"), s("Black Sabbath", "Paranoid")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Lorna Shore", "Sun//Eater"), s("Spiritbox", "Jaded"), s("Architects", "Seeing Red"),
                                s("Bring Me The Horizon", "Darkside"), s("Bad Omens", "THE DEATH OF PEACE OF MIND"),
                                s("Bleed From Within", "Levitate"), s("Parkway Drive", "Glitch"),
                                s("Slipknot", "Hive Mind"), s("Motionless In White", "Masterpiece"), s("Ice Nine Kills", "The Shower Scene")
                        },
                        new CanonicalSeed[]{
                                s("Gojira", "Stranded"), s("Slipknot", "Psychosocial"),
                                s("Architects", "Animals"), s("Powerwolf", "Demons Are a Girl's Best Friend")
                        },
                        new CanonicalSeed[]{
                                s("Gojira", "L'Enfant Sauvage"), s("Slipknot", "The Negative One")
                        },
                        new CanonicalSeed[]{
                                s("Metallica", "Master of Puppets"), s("Pantera", "Cowboys from Hell"),
                                s("Slayer", "Raining Blood"), s("Megadeth", "Holy Wars")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Lorna Shore", "Into the Earth"), s("Slaughter to Prevail", "1984"),
                                s("Falling In Reverse", "Ronald"), s("Slaughter to Prevail", "Viking"),
                                s("Whitechapel", "A Bloodsoaked Symphony"), s("Shadow of Intent", "The Migrant"),
                                s("Brand of Sacrifice", "Exodus"), s("Bodysnatcher", "Murder"),
                                s("Distant", "Argent Justice"), s("Fit For An Autopsy", "Far From Heaven")
                        },
                        new CanonicalSeed[]{
                                s("Slaughter to Prevail", "Demolisher"), s("Lorna Shore", "To the Hellfire"),
                                s("Infant Annihilator", "Blasphemian"), s("Thy Art Is Murder", "Reign of Darkness")
                        },
                        new CanonicalSeed[]{
                                s("Whitechapel", "The Saw Is the Law"), s("Suicide Silence", "You Only Live Once")
                        },
                        new CanonicalSeed[]{
                                s("Cannibal Corpse", "Hammer Smashed Face"), s("Sepultura", "Roots Bloody Roots"),
                                s("Pantera", "Walk"), s("Slayer", "Angel of Death")
                        }
                )
        });

        // ─────────────────────────────────────────────────────────────────────
        // 5. ПОСТ-ПАНК
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("пост-панк", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Молчат Дома", "Сон"), s("Буерак", "Грустно"), s("Ploho", "Не будем прощаться"),
                                s("Черниковская Хата", "Ты не верь слезам"), s("Дурной Вкус", "Пластинки"),
                                s("Перемотка", "Как тебя покорить"), s("Электрофорез", "Зло"), s("Сруб", "Дорога"),
                                s("Сова", "Танец"), s("Утро", "Рассвет")
                        },
                        new CanonicalSeed[]{
                                s("Молчат Дома", "Клетка"), s("Буерак", "Страсть к курению"),
                                s("Ploho", "Закладка"), s("Черниковская Хата", "Белая ночь")
                        },
                        new CanonicalSeed[]{
                                s("Буерак", "Пролетариат"), s("Ploho", "Городские кошмары")
                        },
                        new CanonicalSeed[]{
                                s("Кино", "Пачка сигарет"), s("Joy Division", "Atmosphere"),
                                s("The Cure", "Pictures of You"), s("The Smiths", "Asleep")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Молчат Дома", "Белая Полоса"), s("Буерак", "Спортивные очки"),
                                s("Ploho", "Если бы ты была со мной"), s("Дурной Вкус", "Пластинки"),
                                s("Перемотка", "Встречная"), s("Черниковская Хата", "Ночное рандеву"),
                                s("Электрофорез", "Мертв внутри"), s("Fontaines D.C.", "Starburster"),
                                s("IDLES", "Dancer"), s("Сова", "Город")
                        },
                        new CanonicalSeed[]{
                                s("Молчат Дома", "Судно (Борис Рыжий)"), s("Буерак", "Танцы по расчету"),
                                s("Ploho", "Сердце получает пулю"), s("Перемотка", "Первый снег")
                        },
                        new CanonicalSeed[]{
                                s("Motorama", "Ghost"), s("Буерак", "Кожаные туфли")
                        },
                        new CanonicalSeed[]{
                                s("Joy Division", "Love Will Tear Us Apart"), s("The Cure", "Boys Don't Cry"),
                                s("Кино", "Спокойная ночь"), s("Bauhaus", "Bela Lugosi's Dead")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Fontaines D.C.", "Favourite"), s("IDLES", "Gift Horse"), s("Молчат Дома", "Дискотека"),
                                s("Буерак", "Культ тела"), s("Ploho", "Ядовитые ягоды"), s("Электрофорез", "Кузнечик"),
                                s("Дурной Вкус", "Видеокассета"), s("Squid", "Swing"),
                                s("Geese", "3D Country"), s("Yard Act", "We Make Hits")
                        },
                        new CanonicalSeed[]{
                                s("Fontaines D.C.", "Boys in the Better Land"), s("IDLES", "Never Fight A Man With A Perm"),
                                s("Молчат Дома", "Тоска"), s("Буерак", "На старых лыжах")
                        },
                        new CanonicalSeed[]{
                                s("Savages", "Husbands"), s("Motorama", "Heavy Wave")
                        },
                        new CanonicalSeed[]{
                                s("Joy Division", "Disorder"), s("New Order", "Blue Monday"),
                                s("Кино", "Группа крови"), s("Talking Heads", "Psycho Killer")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("IDLES", "POP POP POP"), s("Fontaines D.C.", "Starburster"),
                                s("Model/Actriz", "Mosquito"), s("Gilla Band", "Post Ryan"),
                                s("Squid", "Narrator"), s("Yard Act", "Dream Job"),
                                s("Protomartyr", "Make Way"), s("Ploho", "Черный день"),
                                s("Электрофорез", "Эшафот"), s("Шумы России", "Ритм")
                        },
                        new CanonicalSeed[]{
                                s("IDLES", "Colossus"), s("Fontaines D.C.", "Too Real"),
                                s("Girl Band", "Paul"), s("Shame", "Concrete")
                        },
                        new CanonicalSeed[]{
                                s("Savages", "She Will"), s("Iceage", "New Brigade")
                        },
                        new CanonicalSeed[]{
                                s("Joy Division", "Transmission"), s("The Fall", "Totally Wired"),
                                s("Gang of Four", "Damaged Goods"), s("Кино", "Дальше действовать будем мы")
                        }
                )
        });

        // ─────────────────────────────────────────────────────────────────────
        // 6. ЛО-ФАЙ
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("ло-фай", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Lofi Girl", "Cozy Winter"), s("Kudasaibeats", "The Girl I Haven't Met"),
                                s("Idealism", "Lonely"), s("Jinsang", "Affection"),
                                s("eevee", "Viola"), s("Saib", "Sakura Trees"),
                                s("Timmies", "Again"), s("potsu", "im closing my eyes"),
                                s("Kupla", "Kingdom in Blue"), s("WYS", "Snowman")
                        },
                        new CanonicalSeed[]{
                                s("Shiloh Dynasty", "Losing Interest"), s("potsu", "i'm closing my eyes"),
                                s("Kina", "Get You The Moon"), s("Joji", "Slow Dancing In The Dark (Lofi)")
                        },
                        new CanonicalSeed[]{
                                s("Nujabes", "Aruarian Dance"), s("Uyama Hiroto", "One Day")
                        },
                        new CanonicalSeed[]{
                                s("Nujabes", "Feather"), s("J Dilla", "Life"),
                                s("Nujabes", "Luv(sic)"), s("Pete Rock", "Pete's Jazz")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Lofi Fruits", "Dreams"), s("Saib", "In Your Arms"), s("ChilledCow", "Coffee Chill"),
                                s("Kavv", "Daylight"), s("Kupla", "Roots"), s("SwuM", "Let It Go"),
                                s("Linearwave", "Slow Life"), s("Flovry", "Late Night"),
                                s("Barnes Blvd.", "Perspectives"), s("Aso", "Coffee")
                        },
                        new CanonicalSeed[]{
                                s("Saib", "Spike Spiegel"), s("SwuM", "Purity"),
                                s("Tomppabeats", "Monday Loop"), s("Idealism", "Both of Us")
                        },
                        new CanonicalSeed[]{
                                s("Nujabes", "Luv (sic.) pt3"), s("Fat Jon", "Automated Technique")
                        },
                        new CanonicalSeed[]{
                                s("J Dilla", "So Far to Go"), s("Nujabes", "Counting Stars"),
                                s("Madlib", "Slim's Return"), s("Pete Rock", "Reminisce")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Saib", "Groovy"), s("L'Indecis", "Soulful"), s("SwuM", "Dusk"),
                                s("Birocratic", "Sleepy Dobie"), s("Brock Berrigan", "Four Wheels"),
                                s("Flamingosis", "Football Head"), s("Cookin Soul", "DOOM Lofi"),
                                s("ProleteR", "April Showers"), s("Gramatik", "Hit That Jive"), s("Blumentunn", "Morning")
                        },
                        new CanonicalSeed[]{
                                s("Flamingosis", "Down for the Fifth Time"), s("Cookin Soul", "Chicago"),
                                s("Gramatik", "Just Jammin'"), s("ProleteR", "Faidherbe Square")
                        },
                        new CanonicalSeed[]{
                                s("Gramatik", "So Much For Love"), s("Nujabes", "Battlecry")
                        },
                        new CanonicalSeed[]{
                                s("J Dilla", "Time: The Donut of the Heart"), s("DJ Shadow", "Midnight in a Perfect World"),
                                s("RJD2", "Ghostwriter"), s("Nujabes", "Shiki No Uta")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Cookin Soul", "Big Dilla"), s("Brock Berrigan", "The Scenic Route"),
                                s("Flamingosis", "Daymaker"), s("ProleteR", "Can't Stop Smiling"),
                                s("Gramatik", "Muy Tranquilo"), s("L'Indecis", "Playtime"),
                                s("Birocratic", "Tony's Belated Breakfast"), s("Saib", "Space Cowboy"),
                                s("Koresma", "Bridges"), s("Vanilla", "Summer")
                        },
                        new CanonicalSeed[]{
                                s("Vanilla", "Origin"), s("Flamingosis", "Sunset Park"),
                                s("Cookin Soul", "Tupac Tribute"), s("Gramatik", "Definite Choice")
                        },
                        new CanonicalSeed[]{
                                s("Gramatik", "The Unfallen Kingdom"), s("Nujabes", "Think Different")
                        },
                        new CanonicalSeed[]{
                                s("J Dilla", "Workinonit"), s("DJ Shadow", "Building Steam With a Grain of Salt"),
                                s("RJD2", "Smoke & Mirrors"), s("Madlib", "Mystic Bounce")
                        }
                )
        });

        // ─────────────────────────────────────────────────────────────────────
        // 7. ПОП
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("поп", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Billie Eilish", "WILDFLOWER"), s("Taylor Swift", "Fortnight"),
                                s("Sabrina Carpenter", "Lie to Girls"), s("Olivia Rodrigo", "traitor"),
                                s("Lana Del Rey", "Say Yes to Heaven"), s("SZA", "Snooze (Acoustic)"),
                                s("Gracie Abrams", "I love you, I'm sorry"), s("Adele", "Easy On Me"),
                                s("Harry Styles", "Matilda"), s("Chappell Roan", "Coffee")
                        },
                        new CanonicalSeed[]{
                                s("Billie Eilish", "ocean eyes"), s("Taylor Swift", "cardigan"),
                                s("Lorde", "Liability"), s("Sam Smith", "Stay With Me")
                        },
                        new CanonicalSeed[]{
                                s("Adele", "Someone Like You"), s("Bruno Mars", "When I Was Your Man")
                        },
                        new CanonicalSeed[]{
                                s("Coldplay", "Yellow"), s("Norah Jones", "Don't Know Why"),
                                s("Dido", "Thank You"), s("George Michael", "Careless Whisper")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Sabrina Carpenter", "Espresso"), s("Billie Eilish", "BIRDS OF A FEATHER"),
                                s("Taylor Swift", "Cruel Summer"), s("Chappell Roan", "Good Luck, Babe!"),
                                s("Dua Lipa", "Houdini"), s("Olivia Rodrigo", "vampire"),
                                s("The Weeknd", "Dancing in the Flames"), s("Ariana Grande", "we can't be friends"),
                                s("Harry Styles", "As It Was"), s("Tate McRae", "greedy")
                        },
                        new CanonicalSeed[]{
                                s("Dua Lipa", "Levitating"), s("The Weeknd", "Blinding Lights"),
                                s("Ariana Grande", "7 rings"), s("Post Malone", "Circles")
                        },
                        new CanonicalSeed[]{
                                s("Bruno Mars", "Locked Out of Heaven"), s("Katy Perry", "Roar")
                        },
                        new CanonicalSeed[]{
                                s("Michael Jackson", "Billie Jean"), s("Madonna", "Like a Prayer"),
                                s("Britney Spears", "Toxic"), s("Justin Timberlake", "Cry Me a River")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Sabrina Carpenter", "Taste"), s("Dua Lipa", "Training Season"),
                                s("Chappell Roan", "HOT TO GO!"), s("Charli xcx", "Apple"),
                                s("Tate McRae", "exes"), s("Troye Sivan", "Rush"),
                                s("Beyonce", "TEXAS HOLD 'EM"), s("Olivia Rodrigo", "bad idea right?"),
                                s("Billie Eilish", "LUNCH"), s("Sabrina Carpenter", "Please Please Please")
                        },
                        new CanonicalSeed[]{
                                s("Dua Lipa", "Don't Start Now"), s("Lady Gaga", "Rain On Me"),
                                s("The Weeknd", "Save Your Tears"), s("Doja Cat", "Say So")
                        },
                        new CanonicalSeed[]{
                                s("Katy Perry", "Firework"), s("Rihanna", "We Found Love")
                        },
                        new CanonicalSeed[]{
                                s("ABBA", "Dancing Queen"), s("Michael Jackson", "Beat It"),
                                s("Madonna", "Hung Up"), s("Britney Spears", "...Baby One More Time")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Charli xcx", "Von dutch"), s("Troye Sivan", "Got Me Started"),
                                s("Dua Lipa", "Dance The Night"), s("Chappell Roan", "Super Graphic Ultra Modern Girl"),
                                s("Kylie Minogue", "Padam Padam"), s("Peggy Gou", "(It Goes Like) Nanana"),
                                s("Kim Petras", "Heart to Break"), s("Charli xcx", "360"),
                                s("Tate McRae", "Run for the Hills"), s("Ava Max", "Million Dollar Baby")
                        },
                        new CanonicalSeed[]{
                                s("Lady Gaga", "Stupid Love"), s("Dua Lipa", "Physical"),
                                s("The Weeknd", "In Your Eyes"), s("Doja Cat", "Need to Know")
                        },
                        new CanonicalSeed[]{
                                s("Pitbull", "Give Me Everything"), s("Lady Gaga", "Bad Romance")
                        },
                        new CanonicalSeed[]{
                                s("Madonna", "Vogue"), s("Michael Jackson", "Don't Stop 'Til You Get Enough"),
                                s("Cher", "Believe"), s("Gloria Gaynor", "I Will Survive")
                        }
                )
        });

        // ─────────────────────────────────────────────────────────────────────
        // 8. ЭЛЕКТРОНИКА (EDM / Club / House)
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("электроника", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Fred again..", "Adore U"), s("Bicep", "Water"), s("Overmono", "Good Lies"),
                                s("Bonobo", "Shadows"), s("Four Tet", "Loved"), s("Jamie xx", "All You Children"),
                                s("Tourist", "A Little Bit Further"), s("Caribou", "Honey"),
                                s("Tycho", "Phantom"), s("Lane 8", "Grapevine")
                        },
                        new CanonicalSeed[]{
                                s("Bicep", "Glue"), s("Bonobo", "Kerala"),
                                s("Tycho", "Awake"), s("Lane 8", "Sunday Song")
                        },
                        new CanonicalSeed[]{
                                s("Disclosure", "Latch (Acoustic)"), s("Kaskade", "Eyes")
                        },
                        new CanonicalSeed[]{
                                s("Moby", "Porcelain"), s("Air", "All I Need"),
                                s("Massive Attack", "Teardrop"), s("Boards of Canada", "Dayvan Cowboy")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Fred again..", "leavemealone"), s("Peggy Gou", "I Believe in Love Again"),
                                s("Disclosure", "She's Gone, Dance On"), s("Fisher", "Take It Off"),
                                s("Dom Dolla", "Rhyme Dust"), s("John Summit", "Where You Are"),
                                s("Mau P", "Drugs From Amsterdam"), s("Calvin Harris", "Miracle"),
                                s("MEDUZA", "Phone"), s("Chris Lake", "Beggin'")
                        },
                        new CanonicalSeed[]{
                                s("Fisher", "Losing It"), s("MEDUZA", "Piece Of Your Heart"),
                                s("CamelPhat", "Cola"), s("Calvin Harris", "One Kiss")
                        },
                        new CanonicalSeed[]{
                                s("Avicii", "Levels"), s("Swedish House Mafia", "Don't You Worry Child")
                        },
                        new CanonicalSeed[]{
                                s("Daft Punk", "One More Time"), s("The Chemical Brothers", "Galvanize"),
                                s("Fatboy Slim", "Right Here, Right Now"), s("Faithless", "Insomnia")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Skrillex", "Rumble"), s("Fred again..", "Jungle"), s("John Summit", "Go Back"),
                                s("Dom Dolla", "Saving Up"), s("Chase & Status", "Baddadan"),
                                s("Sub Focus", "Desire (VIP)"), s("Hedex", "MHITR"),
                                s("Mau P", "Gimme That Bounce"), s("Tiësto", "Lay Low"), s("Martin Garrix", "Hero")
                        },
                        new CanonicalSeed[]{
                                s("Skrillex", "Bangarang"), s("The Prodigy", "Nasty"),
                                s("Martin Garrix", "Animals"), s("Tiësto", "The Business")
                        },
                        new CanonicalSeed[]{
                                s("Skrillex", "Scary Monsters and Nice Sprites"), s("David Guetta", "Titanium")
                        },
                        new CanonicalSeed[]{
                                s("The Prodigy", "Smack My Bitch Up"), s("Daft Punk", "Harder, Better, Faster, Stronger"),
                                s("Darude", "Sandstorm"), s("The Chemical Brothers", "Block Rockin' Beats")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Chase & Status", "Disconnect"), s("Bou", "Closer"), s("Dimension", "DJ Turn It Up"),
                                s("Sub Focus", "Vibration"), s("Hedex", "Back In The Day"),
                                s("Skrillex", "Hydrate"), s("Koven", "Chase the Sun"),
                                s("Netsky", "Hell in High Heels"), s("Wilkinson", "Here For You"), s("Kanine", "Light It Up")
                        },
                        new CanonicalSeed[]{
                                s("The Prodigy", "Firestarter"), s("Chase & Status", "All Goes Wrong"),
                                s("Pendulum", "Witchcraft"), s("Knife Party", "Bonfire")
                        },
                        new CanonicalSeed[]{
                                s("Pendulum", "Watercolour"), s("Skrillex", "First of the Year")
                        },
                        new CanonicalSeed[]{
                                s("The Prodigy", "Breathe"), s("Pendulum", "Tarantula"),
                                s("Scooter", "The Logical Song"), s("Fatboy Slim", "The Rockafeller Skank")
                        }
                )
        });

        // ─────────────────────────────────────────────────────────────────────
        // 9. ИНДИ (Indie / Dream Pop / Indie Rock)
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("инди", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Phoebe Bridgers", "Sidelines"), s("Boygenius", "Cool About It"),
                                s("Mitski", "My Love Mine All Mine"), s("Mac DeMarco", "Heart to Heart"),
                                s("Men I Trust", "Show Me How"), s("Clairo", "Juna"),
                                s("Laufey", "From The Start"), s("Faye Webster", "Lego Ring"),
                                s("Beach Fossils", "Don't Fade Away"), s("Cigarettes After Sex", "Tejano Blue")
                        },
                        new CanonicalSeed[]{
                                s("Phoebe Bridgers", "Motion Sickness"), s("Cigarettes After Sex", "Apocalypse"),
                                s("Clairo", "Pretty Girl"), s("Mac DeMarco", "Chamber of Reflection")
                        },
                        new CanonicalSeed[]{
                                s("Bon Iver", "Holocene"), s("Beach House", "Space Song")
                        },
                        new CanonicalSeed[]{
                                s("Elliott Smith", "Between the Bars"), s("The Shins", "New Slang"),
                                s("Sufjan Stevens", "Chicago"), s("Neutral Milk Hotel", "In the Aeroplane Over the Sea")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Boygenius", "Not Strong Enough"), s("The Last Dinner Party", "Nothing Matters"),
                                s("Wallows", "Calling After Me"), s("Alvvays", "Archie, Marry Me"),
                                s("Bleachers", "Tiny Moves"), s("Vampire Weekend", "Capricorn"),
                                s("Declan McKenna", "Nothing Works"), s("Beabadoobee", "Take A Bite"),
                                s("Dayglow", "Every Time I Cry"), s("Remi Wolf", "Cinderella")
                        },
                        new CanonicalSeed[]{
                                s("Tame Impala", "The Less I Know The Better"), s("Rex Orange County", "Best Friend"),
                                s("Alvvays", "Dreams Tonite"), s("Wallows", "Are You Bored Yet?")
                        },
                        new CanonicalSeed[]{
                                s("Foster The People", "Pumped Up Kicks"), s("Two Door Cinema Club", "What You Know")
                        },
                        new CanonicalSeed[]{
                                s("The Strokes", "Last Nite"), s("Arctic Monkeys", "I Bet You Look Good on the Dancefloor"),
                                s("Franz Ferdinand", "Take Me Out"), s("Phoenix", "1901")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("The Last Dinner Party", "Sinner"), s("Vampire Weekend", "Gen-X Cops"),
                                s("Declan McKenna", "The Key to Life on Earth"), s("The Vaccines", "Heartbreak Kid"),
                                s("IDLES", "Dancer"), s("Wallows", "A Warning"),
                                s("Dayglow", "Can I Call You Tonight?"), s("Courting", "Throw"),
                                s("The Reytons", "Market Street"), s("Inhaler", "Love Will Get You There")
                        },
                        new CanonicalSeed[]{
                                s("Foals", "My Number"), s("Two Door Cinema Club", "Undercover Martyn"),
                                s("The Wombats", "Greek Tragedy"), s("Catfish and the Bottlemen", "Cocoon")
                        },
                        new CanonicalSeed[]{
                                s("Phoenix", "Lisztomania"), s("Walk the Moon", "Shut Up and Dance")
                        },
                        new CanonicalSeed[]{
                                s("The Killers", "Mr. Brightside"), s("The Strokes", "Reptilia"),
                                s("Bloc Party", "Helicopter"), s("Kasabian", "Club Foot")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("IDLES", "Never Fight A Man With A Perm"), s("Fontaines D.C.", "Starburster"),
                                s("Geese", "Cowboy Nudes"), s("The Last Dinner Party", "Caesar on a TV Screen"),
                                s("Yard Act", "Overload"), s("Squid", "Pamphlets"),
                                s("Black Country, New Road", "Chaos Space Marine"), s("Shame", "Fingers of Steel"),
                                s("The Murder Capital", "Ethel"), s("English Teacher", "Nearly Daffodils")
                        },
                        new CanonicalSeed[]{
                                s("Parquet Courts", "Wide Awake"), s("Foals", "What Went Down"),
                                s("IDLES", "Danny Nedelko"), s("Wolf Alice", "Yuk Foo")
                        },
                        new CanonicalSeed[]{
                                s("The Black Keys", "Gold on the Ceiling"), s("Arctic Monkeys", "R U Mine?")
                        },
                        new CanonicalSeed[]{
                                s("The White Stripes", "Fell in Love with a Girl"), s("Yeah Yeah Yeahs", "Heads Will Roll"),
                                s("The Hives", "Hate to Say I Told You So"), s("The Vines", "Get Free")
                        }
                )
        });

        // ─────────────────────────────────────────────────────────────────────
        // 10. СИНТВЕЙВ (Synthwave / Retrowave / Darksynth)
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("синтвейв", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("The Midnight", "Heart Worth Breaking"), s("Timecop1983", "On the Run"),
                                s("Gunship", "Runaway"), s("FM-84", "Never Stop"),
                                s("Trevor Something", "Summer Love"), s("Home", "Resonance"),
                                s("Ollie Wride", "Never Far"), s("Mitch Murder", "Breeze"),
                                s("Miami Nights 1984", "Ocean Drive"), s("Kavinsky", "Cameo")
                        },
                        new CanonicalSeed[]{
                                s("The Midnight", "Sunset"), s("FM-84", "Running in the Night"),
                                s("Timecop1983", "Journeys"), s("Home", "Decay")
                        },
                        new CanonicalSeed[]{
                                s("Kavinsky", "Nightcall"), s("College", "A Real Hero")
                        },
                        new CanonicalSeed[]{
                                s("Vangelis", "Tears in Rain"), s("Tangerine Dream", "Love on a Real Train"),
                                s("Giorgio Moroder", "Chase"), s("Harold Faltermeyer", "Axel F")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("The Midnight", "Change Your Heart"), s("Gunship", "DooM Dance"),
                                s("Kavinsky", "Renegade"), s("Timecop1983", "Faded Touch"),
                                s("FM-84", "Atlas"), s("Ollie Wride", "Overcome"),
                                s("Scandroid", "Writing's on the Wall"), s("LeBrock", "Bright Lights"),
                                s("Waveshaper", "Hearts"), s("Lazerhawk", "Feel the Groove")
                        },
                        new CanonicalSeed[]{
                                s("Gunship", "Tech Noir"), s("The Midnight", "Vampires"),
                                s("Carpenter Brut", "Turbo Killer"), s("Lazerhawk", "Overdrive")
                        },
                        new CanonicalSeed[]{
                                s("Kavinsky", "Pacific Coast Highway"), s("Perturbator", "Future Club")
                        },
                        new CanonicalSeed[]{
                                s("John Carpenter", "Escape from New York"), s("Brad Fiedel", "Terminator Theme"),
                                s("Jan Hammer", "Crockett's Theme"), s("New Order", "Bizarre Love Triangle")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Gunship", "Monster In Paradise"), s("Carpenter Brut", "The Widow Maker"),
                                s("Dance With the Dead", "A New Fear"), s("Perturbator", "Excess"),
                                s("Mega Drive", "Narc"), s("Daniel Deluxe", "Darker Days"),
                                s("Waveshaper", "The Sphere"), s("Magic Sword", "In the Face of Evil"),
                                s("Lazerhawk", "King of the Streets"), s("Mick Gordon", "The Only Thing They Fear Is You")
                        },
                        new CanonicalSeed[]{
                                s("Carpenter Brut", "Roller Mobster"), s("Dance With the Dead", "Robeast"),
                                s("Perturbator", "She Moves Like a Knife"), s("Gunship", "Fly For Your Life")
                        },
                        new CanonicalSeed[]{
                                s("Kavinsky", "Protovision"), s("Power Glove", "Vengeance")
                        },
                        new CanonicalSeed[]{
                                s("Kraftwerk", "The Robots"), s("Giorgio Moroder", "From Here to Eternity"),
                                s("Gary Numan", "Cars"), s("Depeche Mode", "Enjoy the Silence")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Carpenter Brut", "Imaginary Fire"), s("Perturbator", "Death of the Soul"),
                                s("Dance With the Dead", "Hex"), s("GosT", "Prophecy"),
                                s("Mega Drive", "Acid Spit"), s("Daniel Deluxe", "Star Eater"),
                                s("We Are Magonia", "The Creature"), s("Master Boot Record", "C:\\> CHKDSK /F"),
                                s("Lazerpunk", "Speedracer"), s("Vector Seven", "Cyber Samurai")
                        },
                        new CanonicalSeed[]{
                                s("Carpenter Brut", "Turbo Killer"), s("Perturbator", "Neo Tokyo"),
                                s("GosT", "Behemoth"), s("Dance With the Dead", "Invader")
                        },
                        new CanonicalSeed[]{
                                s("Perturbator", "Humans Are Such Easy Prey"), s("Carpenter Brut", "Le Perv")
                        },
                        new CanonicalSeed[]{
                                s("The Prodigy", "Voodoo People"), s("Front 242", "Headhunter"),
                                s("Ministry", "Jesus Built My Hotrod"), s("KMFDM", "Megalomaniac")
                        }
                )
        });

        // ─────────────────────────────────────────────────────────────────────
        // 11. К-ПОП
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("к-поп", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("NewJeans", "Ditto"), s("BTS", "Take Two"), s("V", "Slow Dancing"),
                                s("IU", "Love wins all"), s("Taeyeon", "To. X"), s("Jungkook", "Hate You"),
                                s("LE SSERAFIM", "Good Bones"), s("TXT", "Chasing That Feeling (Acoustic)"),
                                s("SEVENTEEN", "Circles"), s("Red Velvet", "Chill Kill (Ballad)")
                        },
                        new CanonicalSeed[]{
                                s("BTS", "Spring Day"), s("IU", "eight"),
                                s("BLACKPINK", "Stay"), s("EXO", "Sing For You")
                        },
                        new CanonicalSeed[]{
                                s("Taeyang", "Eyes, Nose, Lips"), s("IU", "Through the Night")
                        },
                        new CanonicalSeed[]{
                                s("Big Bang", "Haru Haru"), s("Girls' Generation", "Dear Mom"),
                                s("TVXQ", "Mirotic (Ballad)"), s("BoA", "Only One")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("NewJeans", "Hype Boy"), s("ILLIT", "Magnetic"), s("IVE", "I AM"),
                                s("Jungkook", "Seven"), s("LE SSERAFIM", "Perfect Night"), s("aespa", "Spicy"),
                                s("SEVENTEEN", "Super"), s("Stray Kids", "LALALALA"), s("NMIXX", "DASH"), s("BABYMONSTER", "SHEESH")
                        },
                        new CanonicalSeed[]{
                                s("BLACKPINK", "DDU-DU DDU-DU"), s("BTS", "Dynamite"),
                                s("TWICE", "FANCY"), s("Red Velvet", "Psycho")
                        },
                        new CanonicalSeed[]{
                                s("EXO", "Growl"), s("BTS", "Blood Sweat & Tears")
                        },
                        new CanonicalSeed[]{
                                s("Girls' Generation", "Gee"), s("Big Bang", "Fantastic Baby"),
                                s("2NE1", "I Am The Best"), s("Super Junior", "Sorry, Sorry")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("aespa", "Supernova"), s("LE SSERAFIM", "EASY"), s("Stray Kids", "Chk Chk Boom"),
                                s("NewJeans", "Super Shy"), s("ATEEZ", "WORK"), s("IVE", "HEYA"),
                                s("ENHYPEN", "Bite Me"), s("KISS OF LIFE", "Sticky"), s("TXT", "Deja Vu"), s("RIIZE", "Boom Boom Bass")
                        },
                        new CanonicalSeed[]{
                                s("BLACKPINK", "Kill This Love"), s("BTS", "FIRE"),
                                s("ITZY", "WANNABE"), s("Stray Kids", "God's Menu")
                        },
                        new CanonicalSeed[]{
                                s("BIGBANG", "BANG BANG BANG"), s("BTS", "DOPE")
                        },
                        new CanonicalSeed[]{
                                s("2NE1", "Fire"), s("SHINee", "Ring Ding Dong"),
                                s("Super Junior", "Bonamana"), s("TVXQ", "Keep Your Head Down")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Stray Kids", "MANIAC"), s("aespa", "Armageddon"), s("LE SSERAFIM", "ANTIFRAGILE"),
                                s("ATEEZ", "BOUNCY"), s("NMIXX", "O.O"), s("XG", "WOKE UP"),
                                s("Stray Kids", "MEGAVERSE"), s("TREASURE", "BONA BONA"),
                                s("ITZY", "BORN TO BE"), s("SEVENTEEN", "MAESTRO")
                        },
                        new CanonicalSeed[]{
                                s("Stray Kids", "Thunderous"), s("BLACKPINK", "How You Like That"),
                                s("NCT 127", "Kick It"), s("Monsta X", "Shoot Out")
                        },
                        new CanonicalSeed[]{
                                s("BIGBANG", "FANTASTIC BABY"), s("EXO", "Monster")
                        },
                        new CanonicalSeed[]{
                                s("TVXQ", "Mirotic"), s("2NE1", "Can't Nobody"),
                                s("BIGBANG", "Lies"), s("B.A.P", "Warrior")
                        }
                )
        });

        // ─────────────────────────────────────────────────────────────────────
        // 12. ДЖАЗ (Jazz / Soul / Swing)
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("джаз", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Laufey", "Promise"), s("Norah Jones", "Running"),
                                s("Samara Joy", "Can't Get Out of This Mood"), s("Jon Batiste", "Butterfly"),
                                s("Robert Glasper", "Better Than I Imagined"), s("Esperanza Spalding", "Formwela 1"),
                                s("Kamasi Washington", "Prologue"), s("Chet Baker", "Almost Blue"),
                                s("Melody Gardot", "Sunset in the Blue"), s("Gregory Porter", "Hey Laura")
                        },
                        new CanonicalSeed[]{
                                s("Kamasi Washington", "Truth"), s("Gregory Porter", "Holding On"),
                                s("Robert Glasper", "Afro Blue"), s("Norah Jones", "Don't Know Why")
                        },
                        new CanonicalSeed[]{
                                s("Diana Krall", "The Look of Love"), s("Michael Buble", "Home")
                        },
                        new CanonicalSeed[]{
                                s("Miles Davis", "Blue in Green"), s("John Coltrane", "In a Sentimental Mood"),
                                s("Bill Evans", "Peace Piece"), s("Chet Baker", "My Funny Valentine")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Laufey", "From The Start"), s("Jon Batiste", "FREEDOM"),
                                s("Samara Joy", "Tight"), s("Kamasi Washington", "Dream State"),
                                s("Robert Glasper", "Black Superhero"), s("Yussef Dayes", "Tipper Tissu"),
                                s("Ezra Collective", "Victory Dance"), s("Alfa Mist", "Foreword"),
                                s("Nubya Garcia", "Fortify"), s("BADBADNOTGOOD", "Beside April")
                        },
                        new CanonicalSeed[]{
                                s("BADBADNOTGOOD", "Time Moves Slow"), s("Kamasi Washington", "Street Fighter Mas"),
                                s("Snarky Puppy", "Lingus"), s("Gregory Porter", "Liquid Spirit")
                        },
                        new CanonicalSeed[]{
                                s("Esperanza Spalding", "Black Gold"), s("Robert Glasper", "Ah Yeah")
                        },
                        new CanonicalSeed[]{
                                s("Miles Davis", "So What"), s("Dave Brubeck", "Take Five"),
                                s("Thelonious Monk", "Round Midnight"), s("Charles Mingus", "Moanin'")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Ezra Collective", "Ajala"), s("Jon Batiste", "Drink Water"),
                                s("Yussef Dayes", "Black Classical Music"), s("Kamasi Washington", "The Garden Path"),
                                s("Kokoroko", "We Give Thanks"), s("Nubya Garcia", "Source"),
                                s("Shabaka Hutchings", "Black Jesus"), s("Sons of Kemet", "Hustle"),
                                s("Trombone Shorty", "Come Back"), s("Ghost-Note", "Funk You!")
                        },
                        new CanonicalSeed[]{
                                s("Sons of Kemet", "My Queen Is Harriet Tubman"), s("Snarky Puppy", "What About Me?"),
                                s("Kamasi Washington", "Fists of Fury"), s("BADBADNOTGOOD", "Speaking Gently")
                        },
                        new CanonicalSeed[]{
                                s("Snarky Puppy", "Shofukan"), s("Trombone Shorty", "Do to Me")
                        },
                        new CanonicalSeed[]{
                                s("John Coltrane", "Giant Steps"), s("Dizzy Gillespie", "A Night in Tunisia"),
                                s("Duke Ellington", "Caravan"), s("Art Blakey", "Moanin'")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Ezra Collective", "God Gave Me Feet for Dancing"), s("Sons of Kemet", "Pick Up Your Burning Cross"),
                                s("Yussef Dayes", "Chasing the Drum"), s("Kamasi Washington", "Street Fighter Mas (Live)"),
                                s("The Comet Is Coming", "Code"), s("Shabaka and the Ancestors", "Go My Disciples"),
                                s("Snarky Puppy", "Take It!"), s("Ghost-Note", "Pace Maker"),
                                s("BADBADNOTGOOD", "Signal from the Noise"), s("Nubya Garcia", "Stand With Each Other")
                        },
                        new CanonicalSeed[]{
                                s("The Comet Is Coming", "Summon the Fire"), s("Snarky Puppy", "Grown Folks"),
                                s("Sons of Kemet", "Envision Yourself Levitating"), s("Kamasi Washington", "The Rhythm Changes")
                        },
                        new CanonicalSeed[]{
                                s("Christian Scott", "K.K.P.D."), s("Snarky Puppy", "Thing of Gold")
                        },
                        new CanonicalSeed[]{
                                s("John Coltrane", "Countdown"), s("Charles Mingus", "Better Get Hit in Yo' Soul"),
                                s("Buddy Rich", "Caravan"), s("Max Roach", "Driva Man")
                        }
                )
        });

        // ─────────────────────────────────────────────────────────────────────
        // 13. БЛЮЗ
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("блюз", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Gary Clark Jr.", "Alone Together"), s("Christone Kingfish Ingram", "Another Life Goes By"),
                                s("Marcus King", "Wildflowers & Wine"), s("Buddy Guy", "We Go Back"),
                                s("Joe Bonamassa", "Time Clocks"), s("Tedecshi Trucks Band", "I Am The Moon"),
                                s("Larkin Poe", "Mad as a Hatter"), s("Taj Mahal", "Queen Bee"),
                                s("Eric Gales", "Stand Up"), s("Keb' Mo'", "Good to Be")
                        },
                        new CanonicalSeed[]{
                                s("Gary Clark Jr.", "Our Love"), s("Christone Kingfish Ingram", "Listen"),
                                s("Joe Bonamassa", "Drive"), s("Marcus King", "Rita Is Gone")
                        },
                        new CanonicalSeed[]{
                                s("Gary Clark Jr.", "Please Come Home"), s("Joe Bonamassa", "Slow Train")
                        },
                        new CanonicalSeed[]{
                                s("B.B. King", "The Thrill Is Gone"), s("Muddy Waters", "I Can't Be Satisfied"),
                                s("Howlin' Wolf", "Smokestack Lightnin'"), s("Robert Johnson", "Cross Road Blues")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Gary Clark Jr.", "Maktub"), s("Christone Kingfish Ingram", "662"),
                                s("Marcus King", "Hard Working Man"), s("Buddy Guy", "Gunsmoke Blues"),
                                s("Larkin Poe", "Bad Spell"), s("Joe Bonamassa", "The Heart That Never Waits"),
                                s("The Black Keys", "Crawling Kingsnake"), s("Eric Gales", "You Don't Know the Blues"),
                                s("Tedeschi Trucks Band", "Hear My Dear"), s("Gov't Mule", "Heavy Load Blues")
                        },
                        new CanonicalSeed[]{
                                s("Gary Clark Jr.", "Bright Lights"), s("Christone Kingfish Ingram", "Fresh Out"),
                                s("Marcus King", "The Well"), s("The Black Keys", "I Got Mine")
                        },
                        new CanonicalSeed[]{
                                s("Gary Clark Jr.", "Ain't Messin 'Round"), s("Joe Bonamassa", "Dust Bowl")
                        },
                        new CanonicalSeed[]{
                                s("B.B. King", "Every Day I Have the Blues"), s("Stevie Ray Vaughan", "Pride and Joy"),
                                s("Muddy Waters", "Mannish Boy"), s("Albert King", "Born Under a Bad Sign")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Gary Clark Jr.", "Habits"), s("Christone Kingfish Ingram", "Not Gonna Lie"),
                                s("Marcus King", "Blood on the Tracks"), s("Larkin Poe", "Strike Gold"),
                                s("Buddy Guy", "I Left My Blues in San Francisco"), s("Joe Bonamassa", "Curtain Call"),
                                s("The Black Keys", "Stay in Your Grave"), s("Samantha Fish", "Faster"),
                                s("Kenny Wayne Shepherd", "Dirt on My Hands"), s("Eric Gales", "I Want My Crown")
                        },
                        new CanonicalSeed[]{
                                s("Gary Clark Jr.", "When My Train Pulls In"), s("Stevie Ray Vaughan", "Crossfire"),
                                s("Joe Bonamassa", "Mountain Time"), s("The Black Keys", "Lonely Boy")
                        },
                        new CanonicalSeed[]{
                                s("The Black Keys", "Howlin' for You"), s("Gary Clark Jr.", "Travis County")
                        },
                        new CanonicalSeed[]{
                                s("Stevie Ray Vaughan", "Texas Flood"), s("B.B. King", "Sweet Little Angel"),
                                s("Muddy Waters", "Got My Mojo Working"), s("Freddie King", "Going Down")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Gary Clark Jr.", "Hyperwave"), s("Christone Kingfish Ingram", "Rock & Roll"),
                                s("Larkin Poe", "Bolt Cutters & The Family Name"), s("Marcus King", "Lie Lie Lie"),
                                s("Joe Bonamassa", "Twenty-Four Hour Blues"), s("The Black Keys", "Going Down South"),
                                s("Buddy Guy", "Damn Right, I've Got the Blues"), s("Eric Gales", "Take Me Just as I Am"),
                                s("Samantha Fish", "Crowd Control"), s("Kenny Wayne Shepherd", "Best of Times")
                        },
                        new CanonicalSeed[]{
                                s("Gary Clark Jr.", "Numb"), s("Joe Bonamassa", "Sloe Gin"),
                                s("Stevie Ray Vaughan", "Voodoo Child"), s("Kenny Wayne Shepherd", "Blue on Black")
                        },
                        new CanonicalSeed[]{
                                s("The Black Keys", "She's Long Gone"), s("Buddy Guy", "Stay Around a Little Longer")
                        },
                        new CanonicalSeed[]{
                                s("Stevie Ray Vaughan", "Scuttle Buttin'"), s("Jimi Hendrix", "Red House"),
                                s("Freddie King", "Hide Away"), s("Albert Collins", "Frosty")
                        }
                )
        });

        // ─────────────────────────────────────────────────────────────────────
        // 14. КЛАССИКА
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("классика", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Ludovico Einaudi", "Underwater"), s("Max Richter", "Sleep"),
                                s("Olafur Arnalds", "Woven Song"), s("Joep Beving", "Sleeping Lotus"),
                                s("Hania Rani", "Eden"), s("Yiruma", "River Flows in You (2023)"),
                                s("Chad Lawson", "Stay"), s("Dustin O'Halloran", "Opus 23"),
                                s("Alexis Ffrench", "Bluebird"), s("Luke Howard", "In Stillness")
                        },
                        new CanonicalSeed[]{
                                s("Max Richter", "On the Nature of Daylight"), s("Ludovico Einaudi", "Nuvole Bianche"),
                                s("Olafur Arnalds", "Near Light"), s("Yiruma", "Kiss the Rain")
                        },
                        new CanonicalSeed[]{
                                s("Ludovico Einaudi", "Divenire"), s("Max Richter", "Spring 1")
                        },
                        new CanonicalSeed[]{
                                s("Claude Debussy", "Clair de Lune"), s("Frederic Chopin", "Nocturne Op. 9 No. 2"),
                                s("Erik Satie", "Gymnopedie No. 1"), s("Johann Sebastian Bach", "Air on the G String")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Ludovico Einaudi", "Flora"), s("Max Richter", "The Waves"),
                                s("Hans Zimmer", "Dune: Paul's Dream"), s("Hania Rani", "Don't Break My Heart"),
                                s("Olafur Arnalds", "Spiral"), s("Joep Beving", "Ab Ovo"),
                                s("Karl Jenkins", "Adiemus (2024)"), s("Alexis Ffrench", "Guiding Light"),
                                s("Yann Tiersen", "Ker al Loch"), s("Philip Glass", "Opening (2023)")
                        },
                        new CanonicalSeed[]{
                                s("Ludovico Einaudi", "Una Mattina"), s("Yann Tiersen", "Comptine d'un autre ete"),
                                s("Hans Zimmer", "Interstellar Theme"), s("Philip Glass", "Metamorphosis Two")
                        },
                        new CanonicalSeed[]{
                                s("Ludovico Einaudi", "Fly"), s("Hans Zimmer", "Time")
                        },
                        new CanonicalSeed[]{
                                s("Ludwig van Beethoven", "Moonlight Sonata"), s("Wolfgang Amadeus Mozart", "Piano Concerto No. 21"),
                                s("Pyotr Ilyich Tchaikovsky", "Swan Lake Theme"), s("Antonio Vivaldi", "The Four Seasons: Spring")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Hans Zimmer", "Dune: Herald of the Change"), s("Max Richter", "Summer 1"),
                                s("Ludovico Einaudi", "Experience (Live)"), s("Two Steps From Hell", "Never Give Up On Your Dreams"),
                                s("Audiomachine", "We Are Gods"), s("Thomas Bergersen", "Humanity"),
                                s("Brian Tyler", "Super Mario Suite"), s("Hania Rani", "Ghosts"),
                                s("Olafur Arnalds", "Re:member"), s("Max Richter", "Autumn 3")
                        },
                        new CanonicalSeed[]{
                                s("Two Steps From Hell", "Victory"), s("Two Steps From Hell", "Heart of Courage"),
                                s("Ludovico Einaudi", "Experience"), s("Hans Zimmer", "Flight")
                        },
                        new CanonicalSeed[]{
                                s("Two Steps From Hell", "Archangel"), s("Hans Zimmer", "Mombasa")
                        },
                        new CanonicalSeed[]{
                                s("Antonio Vivaldi", "The Four Seasons: Summer (Presto)"), s("Ludwig van Beethoven", "Symphony No. 5"),
                                s("Wolfgang Amadeus Mozart", "Rondo Alla Turca"), s("Johannes Brahms", "Hungarian Dance No. 5")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Hans Zimmer", "Dune: Armada"), s("Two Steps From Hell", "Myth"),
                                s("Thomas Bergersen", "Cruising in Space"), s("Audiomachine", "Guardians at the Gate"),
                                s("Max Richter", "Winter 1"), s("Hans Zimmer", "Gladiator Suite Live"),
                                s("London Symphony Orchestra", "Carmina Burana 2024"), s("Two Steps From Hell", "Dragonwing"),
                                s("Brian Tyler", "Formula 1 Theme"), s("Thomas Bergersen", "Empire of Angels")
                        },
                        new CanonicalSeed[]{
                                s("Two Steps From Hell", "Star Sky"), s("Two Steps From Hell", "Blackheart"),
                                s("Hans Zimmer", "Why Do We Fall?"), s("Thomas Bergersen", "Final Frontier")
                        },
                        new CanonicalSeed[]{
                                s("Two Steps From Hell", "Protectors of the Earth"), s("Hans Zimmer", "He's a Pirate")
                        },
                        new CanonicalSeed[]{
                                s("Carl Orff", "O Fortuna"), s("Ludwig van Beethoven", "Ode to Joy"),
                                s("Richard Wagner", "Ride of the Valkyries"), s("Pyotr Ilyich Tchaikovsky", "1812 Overture")
                        }
                )
        });

        // ─────────────────────────────────────────────────────────────────────
        // 15. ЭМБИЕНТ (Ambient / Drone / Atmospheric)
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("эмбиент", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Stars of the Lid", "The Tired Sounds of..."), s("Brian Eno", "Ambient 1: Music for Airports"),
                                s("Tim Hecker", "Lotus No Bows"), s("Hammock", "Love in the Void"),
                                s("C418", "Subwoofer Lullaby"), s("Steve Roach", "Structures from Silence"),
                                s("Hiroshi Yoshimura", "Music for Nine Post Cards"), s("Loscil", "Coast Range Arc"),
                                s("Grouper", "Poison Tree"), s("William Basinski", "The Disintegration Loops")
                        },
                        new CanonicalSeed[]{
                                s("Hammock", "Silencia"), s("C418", "Sweden"),
                                s("Tim Hecker", "Konoyo"), s("Stars of the Lid", "December Hunting for Bison")
                        },
                        new CanonicalSeed[]{
                                s("C418", "Wet Hands"), s("Hammock", "Departure Songs")
                        },
                        new CanonicalSeed[]{
                                s("Brian Eno", "An Ending (Ascent)"), s("Aphex Twin", "Selected Ambient Works #3"),
                                s("Harold Budd", "The Pavilion of Dreams"), s("Steve Roach", "Quiet Music")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Tim Hecker", "Monotony"), s("Hammock", "God Send Us a Flaw"),
                                s("Loscil", "Sturgeon Bank"), s("Kaitlyn Aurelia Smith", "Let's Turn It Else"),
                                s("Tycho", "Weather"), s("Solar Fields", "Formations"),
                                s("Carbon Based Lifeforms", "Seeker"), s("Biosphere", "Infrasonic"),
                                s("Marconi Union", "Weightless (2023 Edit)"), s("Abul Mogard", "Quiet Dreams")
                        },
                        new CanonicalSeed[]{
                                s("Carbon Based Lifeforms", "Derelict"), s("Solar Fields", "Sol"),
                                s("Tycho", "Epoch"), s("Hammock", "Mysterium")
                        },
                        new CanonicalSeed[]{
                                s("Carbon Based Lifeforms", "World of Sleepers"), s("Solar Fields", "Mirror's Edge Theme")
                        },
                        new CanonicalSeed[]{
                                s("Aphex Twin", "Rhubarb"), s("Boards of Canada", "Music Is Math"),
                                s("The Orb", "Little Fluffy Clouds"), s("Future Sound of London", "Papua New Guinea (Chill)")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Solar Fields", "Pulse"), s("Carbon Based Lifeforms", "Stochastic"),
                                s("Tycho", "Totem"), s("Jon Hopkins", "Music for Psychedelic Therapy"),
                                s("Max Cooper", "Symphony in C"), s("Rival Consoles", "Now Is"),
                                s("Floating Points", "Birth4000"), s("Oneohtrix Point Never", "A Barely Lit Path"),
                                s("Kelly Lee Owens", "Ballad (2024)"), s("Caterina Barbieri", "Broken Melody")
                        },
                        new CanonicalSeed[]{
                                s("Jon Hopkins", "Singularity"), s("Rival Consoles", "Untravel"),
                                s("Carbon Based Lifeforms", "Photosynthesis"), s("Max Cooper", "Platonic")
                        },
                        new CanonicalSeed[]{
                                s("Jon Hopkins", "Immunity"), s("Tycho", "Dive")
                        },
                        new CanonicalSeed[]{
                                s("Boards of Canada", "Dayvan Cowboy"), s("Aphex Twin", "Xtal"),
                                s("Orbital", "Halcyon On and On"), s("Underworld", "Rez")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Tim Hecker", "Keyed Out"), s("Oneohtrix Point Never", "Sticky Drama"),
                                s("Ben Frost", "Scope Neglect"), s("Rival Consoles", "Running"),
                                s("Max Cooper", "Ascent"), s("Jon Hopkins", "Collider"),
                                s("Floating Points", "Fast Forward"), s("Lorn", "Acid Rain (Max Edit)"),
                                s("Fennesz", "Agora"), s("The Haxan Cloak", "Excavation Part 2")
                        },
                        new CanonicalSeed[]{
                                s("Ben Frost", "A U R O R A"), s("Tim Hecker", "Virgins"),
                                s("Jon Hopkins", "Open Eye Signal"), s("Lorn", "Anvil")
                        },
                        new CanonicalSeed[]{
                                s("Ben Frost", "By the Throat"), s("The Haxan Cloak", "The Drop")
                        },
                        new CanonicalSeed[]{
                                s("Aphex Twin", "Come to Daddy"), s("Autechre", "Gantz Graf"),
                                s("Squarepusher", "Come On My Selector"), s("Boards of Canada", "Gyroscope")
                        }
                )
        });

        // ─────────────────────────────────────────────────────────────────────
        // 16. АЛЬТЕРНАТИВА
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("альтернатива", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Deftones", "Ceremony"), s("Linkin Park", "In the End (Piano)"),
                                s("Twenty One Pilots", "Backslide"), s("Radiohead", "Videotape"),
                                s("Smashing Pumpkins", "Space Age"), s("Placebo", "Happy Birthday in the Sky"),
                                s("Muse", "Ghosts (How Can I Move On)"), s("Three Days Grace", "Someone Who Cares"),
                                s("Chevelle", "Peach"), s("Bush", "More Than Machines")
                        },
                        new CanonicalSeed[]{
                                s("Radiohead", "True Love Waits"), s("Deftones", "Change (Acoustic)"),
                                s("Linkin Park", "Leave Out All the Rest"), s("Three Days Grace", "Never Too Late")
                        },
                        new CanonicalSeed[]{
                                s("Radiohead", "Karma Police"), s("Linkin Park", "Shadow of the Day")
                        },
                        new CanonicalSeed[]{
                                s("Nirvana", "All Apologies"), s("Smashing Pumpkins", "1979"),
                                s("R.E.M.", "Losing My Religion"), s("Radiohead", "No Surprises")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Twenty One Pilots", "Next Semester"), s("Linkin Park", "The Emptiness Machine"),
                                s("Deftones", "Genesis"), s("Muse", "Compliance"),
                                s("Smashing Pumpkins", "Beguiled"), s("Placebo", "Beautiful James"),
                                s("Nothing More", "IF IT DOESN'T HURT"), s("Bush", "Bullet Holes"),
                                s("Three Days Grace", "Lifetime"), s("Chevelle", "Self Destructor")
                        },
                        new CanonicalSeed[]{
                                s("Twenty One Pilots", "Stressed Out"), s("Muse", "Uprising"),
                                s("Three Days Grace", "The Mountain"), s("Deftones", "Prayers / Triangles")
                        },
                        new CanonicalSeed[]{
                                s("Linkin Park", "Waiting for the End"), s("Muse", "Madness")
                        },
                        new CanonicalSeed[]{
                                s("Smashing Pumpkins", "Today"), s("Radiohead", "Creep"),
                                s("Nirvana", "Come as You Are"), s("Foo Fighters", "Everlong")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Linkin Park", "Heavy Is the Crown"), s("Twenty One Pilots", "Jumpsuit"),
                                s("Muse", "Won't Stand Down"), s("Bring Me The Horizon", "Drown"),
                                s("Three Days Grace", "So Called Life"), s("Nothing More", "SPIRITS"),
                                s("I Prevail", "Bad Things"), s("Bad Omens", "Just Pretend"),
                                s("Falling In Reverse", "Voices In My Head"), s("Papa Roach", "Kill the Noise")
                        },
                        new CanonicalSeed[]{
                                s("Linkin Park", "Bleed It Out"), s("Twenty One Pilots", "Heathens"),
                                s("Three Days Grace", "Animal I Have Become"), s("Muse", "Supermassive Black Hole")
                        },
                        new CanonicalSeed[]{
                                s("Three Days Grace", "I Hate Everything About You"), s("Linkin Park", "Faint")
                        },
                        new CanonicalSeed[]{
                                s("Rage Against the Machine", "Guerrilla Radio"), s("Nirvana", "Breed"),
                                s("Smashing Pumpkins", "Bullet with Butterfly Wings"), s("Deftones", "My Own Summer")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Falling In Reverse", "Watch the World Burn"), s("Bring Me The Horizon", "AmEN!"),
                                s("Linkin Park", "QWERTY"), s("Bad Omens", "ARTIFICIAL SUICIDE"),
                                s("Nothing More", "HOUSE ON SAND"), s("I Prevail", "Bow Down"),
                                s("Architects", "Seeing Red"), s("Motionless In White", "Cyberhex"),
                                s("Papa Roach", "Stand Up"), s("Fever 333", "Bite Back")
                        },
                        new CanonicalSeed[]{
                                s("Bring Me The Horizon", "Throne"), s("Linkin Park", "Given Up"),
                                s("Three Days Grace", "Riot"), s("Slipknot", "Before I Forget")
                        },
                        new CanonicalSeed[]{
                                s("Linkin Park", "One Step Closer"), s("System of a Down", "B.Y.O.B.")
                        },
                        new CanonicalSeed[]{
                                s("Rage Against the Machine", "Bulls on Parade"), s("Deftones", "Headup"),
                                s("Nirvana", "Territorial Pissings"), s("Limp Bizkit", "Break Stuff")
                        }
                )
        });

        // ─────────────────────────────────────────────────────────────────────
        // 17. ГИПЕРПОП (Hyperpop / Glitchcore / Digicore)
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("гиперпоп", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("glaive", "as if"), s("ericdoa", "kickstand"), s("brakence", "venus fly trap"),
                                s("midwxst", "better off alone"), s("quinn", "ok!"), s("aldn", "glittr"),
                                s("daine", "boys wanna txt"), s("Jane Remover", "Lips"),
                                s("dltzk", "movies for guys"), s("underscores", "Locals (Girls like us)")
                        },
                        new CanonicalSeed[]{
                                s("brakence", "rosier"), s("glaive", "astrid"),
                                s("SOPHIE", "It's Okay to Cry"), s("Charli xcx", "claws")
                        },
                        new CanonicalSeed[]{
                                s("SOPHIE", "Bipp"), s("A.G. Cook", "Beautiful")
                        },
                        new CanonicalSeed[]{
                                s("Crystal Castles", "Untrust Us"), s("Imogen Heap", "Hide and Seek"),
                                s("Postal Service", "Such Great Heights"), s("Passion Pit", "Sleepyhead")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Charli xcx", "360"), s("brakence", "cbd"), s("glaive", "minnesota is place that exists"),
                                s("ericdoa", "sheaskedwhatmylifeislike"), s("underscores", "Cops and robbers"),
                                s("Jane Remover", "Census Designated"), s("midwxst", "riddle"),
                                s("Frost Children", "FLATLINE"), s("daine", "weekends"), s("aldn", "sink")
                        },
                        new CanonicalSeed[]{
                                s("100 gecs", "money machine"), s("Charli xcx", "party 4 u"),
                                s("glaive", "touché"), s("brakence", "dropout")
                        },
                        new CanonicalSeed[]{
                                s("Charli xcx", "Vroom Vroom"), s("SOPHIE", "Lemonade")
                        },
                        new CanonicalSeed[]{
                                s("Crystal Castles", "Crimewave"), s("Sleigh Bells", "Rill Rill"),
                                s("Uffie", "Pop the Glock"), s("M.I.A.", "Paper Planes")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Charli xcx", "Von dutch"), s("100 gecs", "Dumbest Girl in the World"),
                                s("brakence", "5g"), s("Frost Children", "SERPENT"),
                                s("underscores", "Count it up"), s("midwxst", "step back"),
                                s("ericdoa", "fooliemen"), s("glaive", "fuck this town"),
                                s("Jane Remover", "Fling"), s("quinn", "i dont want that many friends in the first place")
                        },
                        new CanonicalSeed[]{
                                s("100 gecs", "stupid horse"), s("Charli xcx", "Anthems"),
                                s("SOPHIE", "Immaterial"), s("Dorian Electra", "Flamboyant")
                        },
                        new CanonicalSeed[]{
                                s("SOPHIE", "Hard"), s("A.G. Cook", "Keri Baby")
                        },
                        new CanonicalSeed[]{
                                s("Crystal Castles", "Baptism"), s("Sleigh Bells", "Infinity Guitars"),
                                s("M.I.A.", "Bad Girls"), s("CSS", "Cansei de Ser Sexy")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("100 gecs", "Hollywood Baby"), s("Charli xcx", "Club classics"),
                                s("Frost Children", "LETHAL"), s("brakence", "intellect"),
                                s("underscores", "Spotlight"), s("Jane Remover", "Flash in the Pan"),
                                s("quinn", "it's over"), s("glaive", "poison"),
                                s("midwxst", "Troops"), s("ericdoa", "back n forth")
                        },
                        new CanonicalSeed[]{
                                s("100 gecs", "ringtone (Remix)"), s("SOPHIE", "Ponyboy"),
                                s("Dorian Electra", "Man to Man"), s("food house", "Thos Moser")
                        },
                        new CanonicalSeed[]{
                                s("SOPHIE", "Faceshopping"), s("100 gecs", "800db cloud")
                        },
                        new CanonicalSeed[]{
                                s("Crystal Castles", "Doe Deer"), s("Atari Teenage Riot", "Revolution Action"),
                                s("Sleigh Bells", "Treats"), s("Death Grips", "I've Seen Footage")
                        }
                )
        });

        // ─────────────────────────────────────────────────────────────────────
        // 18. ЭМО-РЭП (Emo Rap / Sad Trap)
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("эмо-рэп", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Juice WRLD", "Already Dead"), s("Lil Peep", "Star Shopping (Official)"),
                                s("XXXTENTACION", "Revenge"), s("Iann Dior", "let you"),
                                s("The Kid LAROI", "BLEED"), s("Poorstacy", "Choose Life"),
                                s("DC The Don", "Tell Shmily"), s("Sueco", "SOS"),
                                s("Lund", "Broken"), s("Trevor Daniel", "Falling")
                        },
                        new CanonicalSeed[]{
                                s("Lil Peep", "Star Shopping"), s("XXXTENTACION", "Jocelyn Flores"),
                                s("Juice WRLD", "Lucid Dreams"), s("Lil Peep", "Save That Shit")
                        },
                        new CanonicalSeed[]{
                                s("Bones", "Dirt"), s("Yung Lean", "Ginseng Strip 2002")
                        },
                        new CanonicalSeed[]{
                                s("Kid Cudi", "Day 'N' Nite"), s("Eminem", "Stan"),
                                s("Kanye West", "Welcome to Heartbreak"), s("The Streets", "Dry Your Eyes")
                        }
                ),
                // Steady (1)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Juice WRLD", "Face 2 Face"), s("Lil Peep", "Life is Beautiful"),
                                s("The Kid LAROI", "WITHOUT YOU"), s("Iann Dior", "emotions"),
                                s("Trippie Redd", "Taking A Walk"), s("Sueco", "Paralyzed"),
                                s("KennyHoopla", "how will i rest in peace if i'm buried by a highway?"),
                                s("Poorstacy", "Hills Have Eyes"), s("LØREN", "Need"), s("Powfu", "death bed")
                        },
                        new CanonicalSeed[]{
                                s("Juice WRLD", "All Girls Are The Same"), s("Lil Peep", "Falling Down"),
                                s("XXXTENTACION", "SAD!"), s("Trippie Redd", "Topanga")
                        },
                        new CanonicalSeed[]{
                                s("Lil Uzi Vert", "XO Tour Llif3"), s("Yung Lean", "Kyoto")
                        },
                        new CanonicalSeed[]{
                                s("Kid Cudi", "Pursuit of Happiness"), s("Eminem", "Mockingbird"),
                                s("Kanye West", "Coldest Winter"), s("Lupe Fiasco", "Daydreamin'")
                        }
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Juice WRLD", "Bandit"), s("The Kid LAROI", "STILL CHOSE YOU"),
                                s("Trippie Redd", "Miss the Rage"), s("Sueco", "Loser"),
                                s("KennyHoopla", "ESTELLA//"), s("Poorstacy", "Children of the Dark"),
                                s("Iann Dior", "Holding On"), s("DC The Don", "Worst Day"),
                                s("Jasiah", "Right Now"), s("Dro Kenji", "SUPERSTAR")
                        },
                        new CanonicalSeed[]{
                                s("Juice WRLD", "Robbery"), s("Trippie Redd", "Dark Knight Dummo"),
                                s("Lil Peep", "Witchblades"), s("XXXTENTACION", "Moonlight")
                        },
                        new CanonicalSeed[]{
                                s("Lil Uzi Vert", "Money Longer"), s("Bones", "HDMI")
                        },
                        new CanonicalSeed[]{
                                s("Linkin Park", "Points of Authority"), s("Fort Minor", "Remember the Name"),
                                s("Kid Cudi", "Memories"), s("Gym Class Heroes", "Cupid's Chokehold")
                        }
                ),
                // Max (3)
                new EraSeedBucket(
                        new CanonicalSeed[]{
                                s("Trippie Redd", "MP5"), s("Juice WRLD", "Syphilis"),
                                s("Sueco", "Next Ex"), s("Jasiah", "Crisis"),
                                s("KennyHoopla", "survivors guilt//"), s("Poorstacy", "Knife Party"),
                                s("Nascar Aloe", "American Wasteland"), s("City Morgue", "WHAT'S MY NAME"),
                                s("Kamiyada+", "Damage"), s("Lil Darkie", "GENOCIDE")
                        },
                        new CanonicalSeed[]{
                                s("XXXTENTACION", "Look At Me!"), s("City Morgue", "33rd Blakk Glass"),
                                s("Jasiah", "Case 19"), s("Scarlxrd", "Heart Attack")
                        },
                        new CanonicalSeed[]{
                                s("Bones", "RestInPeace"), s("Ghostemane", "Mercury")
                        },
                        new CanonicalSeed[]{
                                s("Linkin Park", "Faint"), s("Rage Against the Machine", "Bombtrack"),
                                s("Slipknot", "Spit It Out"), s("Onyx", "Shut 'Em Down")
                        }
                )
        });

        // ─────────────────────────────────────────────────────────────────────
        // 19. DEFAULT (Любой / Универсальный каталог)
        // ─────────────────────────────────────────────────────────────────────
        GENRE_MAP.put("default", GENRE_MAP.get("рэп"));
    }

    public static EraSeedBucket getEraBucket(String genre, int moodIndex) {
        String normalized = normalizeGenre(genre);
        EraSeedBucket[] buckets = GENRE_MAP.get(normalized);
        if (buckets == null) {
            buckets = GENRE_MAP.get("default");
        }
        if (moodIndex < 0 || moodIndex >= buckets.length) {
            moodIndex = MOOD_ENERGETIC;
        }
        return buckets[moodIndex];
    }

    public static List<CanonicalSeed> getCanonicalSeedsForGenre(String genre, int moodIndex) {
        String normalized = normalizeGenre(genre);
        EraSeedBucket[] buckets = GENRE_MAP.get(normalized);
        if (buckets == null) {
            buckets = GENRE_MAP.get("default");
        }
        if (buckets == null) return Collections.emptyList();
        if (moodIndex < 0 || moodIndex >= buckets.length) {
            moodIndex = MOOD_ENERGETIC;
        }
        List<CanonicalSeed> result = new ArrayList<>();
        if (buckets[moodIndex] != null) {
            result.addAll(buckets[moodIndex].getAllSeeds());
        }
        for (int i = 0; i < buckets.length; i++) {
            if (i != moodIndex && buckets[i] != null) {
                result.addAll(buckets[i].getAllSeeds());
            }
        }
        return result;
    }

    public static String getDeezerGenreQuery(String genre) {
        String g = normalizeGenre(genre);
        switch (g) {
            case "фонк": return "drift phonk";
            case "рок": return "rock";
            case "металл": return "metal";
            case "пост-панк": return "post-punk";
            case "ло-фай": return "lo-fi";
            case "поп": return "pop";
            case "электроника": return "electronic";
            case "инди": return "indie rock";
            case "синтвейв": return "synthwave";
            case "к-поп": return "k-pop";
            case "джаз": return "jazz";
            case "блюз": return "blues";
            case "классика": return "classical";
            case "эмбиент": return "ambient";
            case "альтернатива": return "alternative rock";
            case "гиперпоп": return "hyperpop";
            case "эмо-рэп": return "emo rap";
            case "рэп": return "rap";
            case "хип-хоп": return "hip hop";
            default: return "hits";
        }
    }

    /**
     * Резолвит канонический сид-трек (Артист + Название) в актуальный videoId YouTube Music.
     * Использует LruCache для мгновенной отдачи при повторных вызовах.
     */
    public static String resolveSeedToVideoId(CanonicalSeed seed, AtomicBoolean cancelFlag) {
        if (seed == null) return null;
        String query = seed.getQuery();
        String cached = sSeedIdCache.get(query);
        if (cached != null) {
            return cached;
        }

        try {
            List<InnerTubeClient.RawTrack> songs = InnerTubeClient.searchSongs(query, 2, cancelFlag);
            if (songs != null && !songs.isEmpty()) {
                for (InnerTubeClient.RawTrack t : songs) {
                    if (t != null && t.videoId != null && !t.videoId.isEmpty()) {
                        sSeedIdCache.put(query, t.videoId);
                        FileLog.d(TAG + " Resolved canonical seed [" + query + "] -> " + t.videoId);
                        return t.videoId;
                    }
                }
            }
        } catch (Exception e) {
            FileLog.w(TAG + " Failed to resolve seed [" + query + "]: " + e);
        }
        return null;
    }

    /**
     * Основной метод выборки сидов по жанру и настроению:
     * Берет сбалансированные канонические треки с точным распределением:
     * 50% 2022-2026, 20% 2015-2022, 10% 2010-2015, 20% <2010.
     * Параллельно резолвит их в YouTube Music videoId.
     */
    public static List<String> selectSeedsByGenre(String genre) {
        return selectSeedsByGenre(genre, SmartRadioEngine.currentConfig.mood, null);
    }

    public static List<String> selectSeedsByGenre(String genre, AtomicBoolean cancelFlag) {
        return selectSeedsByGenre(genre, SmartRadioEngine.currentConfig.mood, cancelFlag);
    }

    public static List<String> selectSeedsByGenre(String genre, String mood, AtomicBoolean cancelFlag) {
        int moodIndex = parseMoodIndex(mood, SmartRadioEngine.currentConfig.moodProgress);
        EraSeedBucket bucket = getEraBucket(genre, moodIndex);

        // 1. Выбираем 3 канонических сида согласно пропорциям эпох
        List<CanonicalSeed> sampleSeeds = bucket.pickSampleSeeds(3);
        List<String> resolvedVideoIds = new ArrayList<>();

        // 2. Параллельный резолвинг в YouTube Music videoId
        CompletionService<String> cs = new ExecutorCompletionService<>(sResolveExecutor);
        int submitted = 0;
        for (CanonicalSeed csSeed : sampleSeeds) {
            cs.submit(() -> resolveSeedToVideoId(csSeed, cancelFlag));
            submitted++;
        }

        for (int i = 0; i < submitted; i++) {
            try {
                Future<String> f = cs.poll(1500, TimeUnit.MILLISECONDS);
                if (f != null) {
                    String vid = f.get();
                    if (vid != null && !resolvedVideoIds.contains(vid)) {
                        resolvedVideoIds.add(vid);
                    }
                }
            } catch (Exception ignored) {}
        }

        // 3. Дополнительно обогащаем 1 свежим живым хитом YouTube Music для максимальной актуальности
        try {
            String normG = normalizeGenre(genre);
            String searchQ = normG + " hits 2024 2025";
            if (normG.equals("фонк")) {
                searchQ = (moodIndex >= MOOD_ENERGETIC) ? "drift phonk bass bangers" : "ambient chill phonk";
            }
            List<InnerTubeClient.RawTrack> live = InnerTubeClient.searchSongs(searchQ, 3, cancelFlag);
            if (live != null && !live.isEmpty()) {
                for (InnerTubeClient.RawTrack t : live) {
                    if (t != null && t.videoId != null && !resolvedVideoIds.contains(t.videoId)) {
                        resolvedVideoIds.add(0, t.videoId);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            FileLog.w(TAG + " Live search enrich error: " + e);
        }

        // Если из-за таймаута сети список пуст, берем первый попавшийся резолв
        if (resolvedVideoIds.isEmpty() && !sampleSeeds.isEmpty()) {
            String direct = resolveSeedToVideoId(sampleSeeds.get(0), cancelFlag);
            if (direct != null) resolvedVideoIds.add(direct);
        }

        return resolvedVideoIds;
    }

    public static List<String> selectSeedsByArtist(String artistName, AtomicBoolean cancelFlag) {
        List<String> result = new ArrayList<>();
        if (artistName == null || artistName.trim().isEmpty()) {
            return selectSeedsByGenre("default", cancelFlag);
        }
        String clean = artistName.trim();
        List<InnerTubeClient.RawTrack> songs = InnerTubeClient.searchSongs(clean + " лучшие песни", 6, cancelFlag);
        if (songs == null || songs.isEmpty()) {
            songs = InnerTubeClient.searchSongs(clean, 6, cancelFlag);
        }
        if (songs != null) {
            Set<String> seen = new HashSet<>();
            for (InnerTubeClient.RawTrack t : songs) {
                if (t != null && t.videoId != null && seen.add(t.videoId)) {
                    result.add(t.videoId);
                    if (result.size() >= 3) break;
                }
            }
        }
        if (result.isEmpty()) {
            return selectSeedsByGenre("default", cancelFlag);
        }
        return result;
    }

    public static List<String> selectSeedsByAlbum(String artistName, String albumTitle, AtomicBoolean cancelFlag) {
        return selectSeedsByAlbum(artistName, albumTitle, null, cancelFlag);
    }

    public static List<String> selectSeedsByAlbum(String artistName, String albumTitle, List<org.telegram.messenger.MessageObject> albumTracks, AtomicBoolean cancelFlag) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        if (albumTracks != null) {
            for (int i = 0; i < Math.min(3, albumTracks.size()); i++) {
                org.telegram.messenger.MessageObject m = albumTracks.get(i);
                if (m != null) {
                    String q = (m.getMusicAuthor() + " " + m.getMusicTitle()).trim();
                    List<InnerTubeClient.RawTrack> s = InnerTubeClient.searchSongs(q, 1, cancelFlag);
                    if (s != null && !s.isEmpty() && s.get(0).videoId != null && seen.add(s.get(0).videoId)) {
                        result.add(s.get(0).videoId);
                    }
                }
            }
        }

        if (result.isEmpty()) {
            String q = ((artistName != null ? artistName : "") + " " + (albumTitle != null ? albumTitle : "")).trim();
            List<InnerTubeClient.RawTrack> s = InnerTubeClient.searchSongs(q, 3, cancelFlag);
            if (s != null) {
                for (InnerTubeClient.RawTrack t : s) {
                    if (t != null && t.videoId != null && seen.add(t.videoId)) {
                        result.add(t.videoId);
                    }
                }
            }
        }

        if (result.isEmpty()) {
            return selectSeedsByArtist(artistName, cancelFlag);
        }
        return result;
    }

    public static List<String> selectSeedsByPlaylist(List<org.telegram.messenger.MessageObject> originalPlaylist, AtomicBoolean cancelFlag) {
        List<String> result = new ArrayList<>();
        if (originalPlaylist == null || originalPlaylist.isEmpty()) {
            return selectSeedsByGenre("default", cancelFlag);
        }
        Set<String> seen = new HashSet<>();
        List<org.telegram.messenger.MessageObject> shuffled = new ArrayList<>(originalPlaylist);
        Collections.shuffle(shuffled, sRandom);
        for (int i = 0; i < Math.min(3, shuffled.size()); i++) {
            org.telegram.messenger.MessageObject m = shuffled.get(i);
            if (m != null) {
                String q = (m.getMusicAuthor() + " " + m.getMusicTitle()).trim();
                List<InnerTubeClient.RawTrack> s = InnerTubeClient.searchSongs(q, 1, cancelFlag);
                if (s != null && !s.isEmpty() && s.get(0).videoId != null && seen.add(s.get(0).videoId)) {
                    result.add(s.get(0).videoId);
                }
            }
        }
        if (result.isEmpty()) {
            return selectSeedsByGenre("default", cancelFlag);
        }
        return result;
    }

    public static List<String> getGenreSearchQueries(String genre, int moodIndex) {
        List<String> queries = new ArrayList<>();
        String g = normalizeGenre(genre);
        switch (g) {
            case "фонк":
                if (moodIndex == MOOD_CALM) {
                    queries.add("ambient phonk chill drift slow");
                    queries.add("lofi phonk chillwave relaxing");
                } else if (moodIndex == MOOD_STEADY) {
                    queries.add("drift phonk classic memphis");
                    queries.add("phonk hits popular drift");
                } else if (moodIndex == MOOD_ENERGETIC) {
                    queries.add("cowbell phonk drift phonk gym");
                    queries.add("energetic phonk drift car beats");
                } else {
                    queries.add("aggressive drift phonk hardcore gym");
                    queries.add("fast phonk bass boosted 160bpm bangers");
                }
                break;
            case "рэп":
            case "хип-хоп":
                if (moodIndex == MOOD_CALM) {
                    queries.add("chill rap hip hop lofi");
                    queries.add("спокойный русский рэп лирика");
                } else if (moodIndex == MOOD_STEADY) {
                    queries.add("popular hip hop rap hits");
                    queries.add("русский рэп хиты новинки");
                } else if (moodIndex == MOOD_ENERGETIC) {
                    queries.add("hype trap rap bangers");
                    queries.add("бодрый русский рэп кач");
                } else {
                    queries.add("aggressive hard trap bass");
                    queries.add("лютый треп басс кач");
                }
                break;
            case "рок":
                if (moodIndex == MOOD_CALM) {
                    queries.add("acoustic rock soft ballads");
                    queries.add("спокойный русский рок лирика");
                } else if (moodIndex == MOOD_STEADY) {
                    queries.add("classic rock modern hits");
                    queries.add("русский рок популярное");
                } else if (moodIndex == MOOD_ENERGETIC) {
                    queries.add("hard rock alternative bangers");
                    queries.add("драйвовый рок русский рок");
                } else {
                    queries.add("punk rock aggressive hard rock");
                    queries.add("мощный рок взрывной драйв");
                }
                break;
            case "металл":
                if (moodIndex == MOOD_CALM) {
                    queries.add("symphonic metal acoustic ballads");
                    queries.add("atmospheric metal peaceful");
                } else if (moodIndex == MOOD_STEADY) {
                    queries.add("heavy metal classic melodic");
                    queries.add("тяжелый метал классика");
                } else if (moodIndex == MOOD_ENERGETIC) {
                    queries.add("modern metalcore nu metal energetic");
                    queries.add("бодрый металкор кач");
                } else {
                    queries.add("brutal deathcore heavy metal breakdown");
                    queries.add("extreme metal hardcore brutality");
                }
                break;
            case "пост-панк":
                if (moodIndex == MOOD_CALM) {
                    queries.add("советвейв думерский грустный постпанк");
                    queries.add("russian doomer sad coldwave");
                } else if (moodIndex == MOOD_STEADY) {
                    queries.add("русский пост-панк new wave classic");
                    queries.add("post-punk darkwave coldwave");
                } else if (moodIndex == MOOD_ENERGETIC) {
                    queries.add("драйвовый пост-панк быстрый");
                    queries.add("dance post-punk darkwave energetic");
                } else {
                    queries.add("fast post-punk synthpunk dance punk");
                    queries.add("энергичный панк пост-панк");
                }
                break;
            default:
                if (moodIndex == MOOD_CALM) {
                    queries.add(g + " chill acoustic calm peaceful");
                } else if (moodIndex == MOOD_STEADY) {
                    queries.add(g + " hits popular music");
                } else if (moodIndex == MOOD_ENERGETIC) {
                    queries.add(g + " upbeat energetic drive bangers");
                } else {
                    queries.add(g + " fast maximum energy extreme bass");
                }
                break;
        }
        return queries;
    }

    private SeedSelector() {}
}
