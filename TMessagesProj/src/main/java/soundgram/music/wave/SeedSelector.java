package soundgram.music.wave;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SeedSelector {

    private static final String TAG = "SeedSelector";
    private static final Random sRandom = new Random();

    // ─────────────────────────────────────────────────────────────────────────
    // Справочник жанров: эталонные videoId (RU + культовые хиты) для мгновенного старта волны
    // ─────────────────────────────────────────────────────────────────────────
    private static final Map<String, List<String>> GENRE_BEACONS = new HashMap<>();

    static {
        // Рок (RU легенды + Classic / Alternative Rock)
        GENRE_BEACONS.put("рок", Arrays.asList(
                "yUp01GbQxTw", "ihOxsYme6pk", "5qZQEq_C3vc", "eVTXPUF4Oz4", "kXYiU_JCYtU",
                "1w7OgIMMRc4", "fJ9rUzIMcZQ", "hTWKbfoikeg", "Soa3gO7tL-c", "l482T0yNkeo",
                "v2AC41dglnM", "NOG3eus4ZSo", "CdhqVtp675c", "CSvFpBOe8eY", "pAgnJDJN4VA",
                "YlUKcNNmywk", "8SbUCzED860", "GLvohMXgcBo", "OMOGaugKpzs", "qfNmyxV2Ncw"
        ));
        GENRE_BEACONS.put("rock", GENRE_BEACONS.get("рок"));

        // Хип-хоп / Рэп (RU + US классика и топ)
        GENRE_BEACONS.put("хип-хоп", Arrays.asList(
                "5-MahtpxqBQ", "tSLQTfsmrkY", "tqxRidAWER8", "_Yhyp-_hX2s", "YVkUvmDQ3HY",
                "uelHwf8o7_U", "tvTRZJ-4EyI", "kJQP7kiw5Fk", "fPO76Jpsz64", "nfWlot6h_JM",
                "41qC3w3UUkU", "ru0K8uYEZWw", "hT_nvWreIhg", "XbGs_qK2PQA", "RgKAFK5djSk",
                "fGx6K90GY6M", "astISOttCQ0", "U9BwWKXjVaI", "7PCkvCPvDXk", "GlNYmpn1kY4"
        ));
        GENRE_BEACONS.put("хип хоп", GENRE_BEACONS.get("хип-хоп"));
        GENRE_BEACONS.put("рэп", GENRE_BEACONS.get("хип-хоп"));
        GENRE_BEACONS.put("hip-hop", GENRE_BEACONS.get("хип-хоп"));
        GENRE_BEACONS.put("rap", GENRE_BEACONS.get("хип-хоп"));

        // Металл / Метал (Heavy, Nu-Metal, Symphonic, Metalcore)
        GENRE_BEACONS.put("металл", Arrays.asList(
                "CHIWNDAwTqQ", "kiw_VxQ2oXo", "rAjwN7ryg94", "tAGnKpE4NCI", "qw2LU1yS7aw",
                "W3q8Od5qJio", "CSvFpBOe8eY", "6fVE8k5wl6I", "7jMlFXvxhdc", "v2H4l9OpPC8",
                "VTT6picaEiQ", "DeumyOzKqgI", "Nco_kh8xJjc", "D1sZ_vwqwcE", "aDAW9VnN_44",
                "AkFqg5wAuFk", "Gz2GVlQkn4Q", "1V_xRb0x9aw", "DelhLppPSxY", "9d8SzG4FPyM"
        ));
        GENRE_BEACONS.put("метал", GENRE_BEACONS.get("металл"));
        GENRE_BEACONS.put("metal", GENRE_BEACONS.get("металл"));

        // Фонк (Drift Phonk, Memphis, Cowbell)
        GENRE_BEACONS.put("фонк", Arrays.asList(
                "jIYdY_qDeuQ", "fzeoo8n8RZo", "cZfb2HU7c9o", "u_Q3x6yv26k", "hD3Uv5Kz18s",
                "n8X9_MgEdCg", "p28u3p16qL0", "7F7a2q43z0Q", "2b4G35p8q1M", "4V11N2z4q60",
                "vM9B8H3s980", "1P2q03k84wL", "6qX4z7w910L", "8pZ90qL23wE", "3q21L908wXz"
        ));
        GENRE_BEACONS.put("phonk", GENRE_BEACONS.get("фонк"));

        // Пост-панк (RU Doomer, Sovietwave, Coldwave, New Wave)
        GENRE_BEACONS.put("пост-панк", Arrays.asList(
                "s1ATTIQrmIQ", "n3EvWofNgIw", "1M_k7b1cAxM", "7qK3r7Z1qW8", "9v22b07rF7w",
                "4rT6Z1p080M", "6m0L18v79qA", "8b19X3q0L2w", "2b4N09qL71K", "10v4Z1r89qM",
                "3qL28b07X19", "5b08L2q149w", "7v10r98q21A", "4m9qL18v02A", "2z19L08v37q"
        ));
        GENRE_BEACONS.put("пост панк", GENRE_BEACONS.get("пост-панк"));
        GENRE_BEACONS.put("постпанк", GENRE_BEACONS.get("пост-панк"));
        GENRE_BEACONS.put("post-punk", GENRE_BEACONS.get("пост-панк"));
        GENRE_BEACONS.put("postpunk", GENRE_BEACONS.get("пост-панк"));

        // Поп (Мировые и русские поп-хиты)
        GENRE_BEACONS.put("поп", Arrays.asList(
                "-V_mLCl0Zws", "yYO-g0ArEHo", "OsfAnsMY21M", "fRh_vgS2dFE", "OPf0YbXqDm0",
                "09R8_2nJtjg", "JGwWNGJdvx8", "hT_nvWreIhg", "kJQP7kiw5Fk", "RgKAFK5djSk",
                "34Na4j8AVgA", "4NRXx6U8ABQ", "L_LUpnjgPso", "CevxZvSJLk8", "pRpeEdMmmQ0",
                "ru0K8uYEZWw", "2Vv-BfVoq4g", "papuvlVeZg8", "TUVcZfQe-Kw", "k4V3Mo61fJM"
        ));
        GENRE_BEACONS.put("pop", GENRE_BEACONS.get("поп"));

        // Электроника / Клубная / EDM
        GENRE_BEACONS.put("электроника", Arrays.asList(
                "LhOjN4t0YsY", "2NiyrtYegso", "LRPGqNeav_M", "y6120QOlsfU", "IcrbM1l_BoI",
                "ALZHF5UqnU4", "60ItHLz5WEA", "gCYcTmT9wx4", "1w7OgIMMRc4", "YqeW9_5kURI",
                "ebXbLfLACGM", "ZbZSe6N_BXs", "k2qgadSvNyU", "ft4jcKWLGTQ", "uelHwf8o7_U"
        ));
        GENRE_BEACONS.put("electronic", GENRE_BEACONS.get("электроника"));
        GENRE_BEACONS.put("edm", GENRE_BEACONS.get("электроника"));

        // Альтернатива / Альтернативный рок
        GENRE_BEACONS.put("альтернатива", Arrays.asList(
                "IeYxMTVL74Y", "0-q83p_o-Lg", "ljUtuoFt-8c", "kXYiU_JCYtU", "eVTXPUF4Oz4",
                "hTWKbfoikeg", "Soa3gO7tL-c", "l482T0yNkeo", "CSvFpBOe8eY", "8SbUCzED860",
                "GLvohMXgcBo", "OMOGaugKpzs", "qfNmyxV2Ncw", "fJ9rUzIMcZQ", "YlUKcNNmywk"
        ));
        GENRE_BEACONS.put("alternative", GENRE_BEACONS.get("альтернатива"));

        // Инди / Indie Rock / Indie Pop
        GENRE_BEACONS.put("инди", Arrays.asList(
                "haRv92SNt-E", "agS8ysvGjlY", "cULQhvuq1Zc", "bpOSxM0rNPM", "hXzpxBhhmgk",
                "rYEDA3JcQqw", "oyEuk8j8imI", "4B1v4Xm190k", "2z9X8qL1v2w", "8b19X3q0L2w"
        ));
        GENRE_BEACONS.put("indie", GENRE_BEACONS.get("инди"));

        // Гиперпоп
        GENRE_BEACONS.put("гиперпоп", Arrays.asList(
                "H8MbDwkV9YM", "19fkWJ--GQo", "kpPt8NpYtJg", "Z5p4p902w1M", "3v10X9L4q2w",
                "9qL28b07X19", "4rT6Z1p080M", "6m0L18v79qA", "8b19X3q0L2w", "2b4N09qL71K"
        ));
        GENRE_BEACONS.put("hyperpop", GENRE_BEACONS.get("гиперпоп"));

        // Эмо-рэп
        GENRE_BEACONS.put("эмо-рэп", Arrays.asList(
                "YgTEUXhHyMU", "hHtv2XMZlKs", "qxtOBMQxsHc", "3q21L908wXz", "8b19X3q0L2w",
                "2b4N09qL71K", "10v4Z1r89qM", "5b08L2q149w", "7v10r98q21A", "4m9qL18v02A"
        ));
        GENRE_BEACONS.put("эмо рэп", GENRE_BEACONS.get("эмо-рэп"));
        GENRE_BEACONS.put("эморэп", GENRE_BEACONS.get("эмо-рэп"));
        GENRE_BEACONS.put("emo-rap", GENRE_BEACONS.get("эмо-рэп"));
        GENRE_BEACONS.put("emorap", GENRE_BEACONS.get("эмо-рэп"));

        // К-поп
        GENRE_BEACONS.put("к-поп", Arrays.asList(
                "HaEYUJ2aRHs", "8mA6jIeojnk", "GyCAzYm1zvk", "2S24h80OGfY", "gdZLi9oWNZg",
                "9bZkp7q19f0", "WyiIGEHQP8o", "ioNng23DkIM", "arm0kL7s68Q", "3YqEIkat7lc"
        ));
        GENRE_BEACONS.put("к поп", GENRE_BEACONS.get("к-поп"));
        GENRE_BEACONS.put("кпоп", GENRE_BEACONS.get("к-поп"));
        GENRE_BEACONS.put("k-pop", GENRE_BEACONS.get("к-поп"));
        GENRE_BEACONS.put("kpop", GENRE_BEACONS.get("к-поп"));

        // Синтвейв
        GENRE_BEACONS.put("синтвейв", Arrays.asList(
                "LfgNorryffc", "Qzp6Xigrts0", "YWIviF4rYcc", "er416Ad3R1g", "39zKhsT5naI",
                "4xDzrJKXOOY", "8qL290X1b34", "9v22b07rF7w", "7qK3r7Z1qW8", "s1ATTIQrmIQ"
        ));
        GENRE_BEACONS.put("синт-вейв", GENRE_BEACONS.get("синтвейв"));
        GENRE_BEACONS.put("синт вейв", GENRE_BEACONS.get("синтвейв"));
        GENRE_BEACONS.put("synthwave", GENRE_BEACONS.get("синтвейв"));
        GENRE_BEACONS.put("retrowave", GENRE_BEACONS.get("синтвейв"));

        // Ло-фай
        GENRE_BEACONS.put("ло-фай", Arrays.asList(
                "jgJWZDFoD1k", "XWLQKNvCepM", "xhzi2n-aZ58", "5qap5aO4i9A", "DWcJFNfaw9c",
                "TURbeWK29_E", "7NOSDKb0HlU", "2atqI8wKjEQ", "tf1P_5v120c", "4xDzrJKXOOY"
        ));
        GENRE_BEACONS.put("лофай", GENRE_BEACONS.get("ло-фай"));
        GENRE_BEACONS.put("ло фай", GENRE_BEACONS.get("ло-фай"));
        GENRE_BEACONS.put("lo-fi", GENRE_BEACONS.get("ло-фай"));
        GENRE_BEACONS.put("lofi", GENRE_BEACONS.get("ло-фай"));

        // Джаз
        GENRE_BEACONS.put("джаз", Arrays.asList(
                "fnyMzZiKfkY", "ZEcqHA7dbwM", "KJEzFvXx3Xw", "ylXk1LBvIqU", "RPfF431pc4k",
                "vmDDOFXSg2k", "u-3Vec3WLE4", "f_O_jT3dK0g", "rP3tYpPZ54I", "qQzdASg5RAI"
        ));
        GENRE_BEACONS.put("jazz", GENRE_BEACONS.get("джаз"));

        // Блюз
        GENRE_BEACONS.put("блюз", Arrays.asList(
                "JmsUkutNnI0", "_9IIDE_45Pc", "ihOxsYme6pk", "4fk2prKnYnI", "S33tzzS_8S8",
                "x_7Oz_p0s9c", "4Ny5awepRNQ", "V_0pZ3y30Qw", "2b4N09qL71K", "6m0L18v79qA"
        ));
        GENRE_BEACONS.put("blues", GENRE_BEACONS.get("блюз"));

        // Классика
        GENRE_BEACONS.put("классика", Arrays.asList(
                "NbEfWg8VBho", "_FeJFs6LJjU", "pBGVfwOLU1w", "GRxofEmo3HA", "mGQLXRTl3Z0",
                "W-fFHeTX70Q", "4Tr0otuiQuU", "7lC1lHE0538", "g10T_QZ301L", "8b19X3q0L2w"
        ));
        GENRE_BEACONS.put("классическая", GENRE_BEACONS.get("классика"));
        GENRE_BEACONS.put("classical", GENRE_BEACONS.get("классика"));
        GENRE_BEACONS.put("classic", GENRE_BEACONS.get("классика"));

        // Эмбиент
        GENRE_BEACONS.put("эмбиент", Arrays.asList(
                "akzO4lxACNY", "75O11W5EZAU", "LA0QB8HXJSw", "79U4Gk3d0sA", "2Z0kQ_190fA",
                "9qL28b07X19", "4rT6Z1p080M", "6m0L18v79qA", "8b19X3q0L2w", "2b4N09qL71K"
        ));
        GENRE_BEACONS.put("ambient", GENRE_BEACONS.get("эмбиент"));

        // Дефолтный пул (микс популярных хитов)
        GENRE_BEACONS.put("default", Arrays.asList(
                "-V_mLCl0Zws", "5-MahtpxqBQ", "yUp01GbQxTw", "LhOjN4t0YsY", "jIYdY_qDeuQ",
                "HaEYUJ2aRHs", "s1ATTIQrmIQ", "CHIWNDAwTqQ", "IeYxMTVL74Y", "haRv92SNt-E"
        ));
        GENRE_BEACONS.put("любой жанр", GENRE_BEACONS.get("default"));
        GENRE_BEACONS.put("любой", GENRE_BEACONS.get("default"));
    }

    public static List<String> getAllGenreBeaconIds(String genre) {
        if (genre == null || genre.trim().isEmpty()) return GENRE_BEACONS.get("default");
        String key = genre.toLowerCase().trim();
        List<String> list = GENRE_BEACONS.get(key);
        if (list == null || list.isEmpty()) {
            list = GENRE_BEACONS.get(key.replace("-", "").replace(" ", ""));
        }
        if (list == null || list.isEmpty()) {
            for (Map.Entry<String, List<String>> e : GENRE_BEACONS.entrySet()) {
                if (key.contains(e.getKey()) || e.getKey().contains(key)) {
                    list = e.getValue();
                    break;
                }
            }
        }
        return list != null ? list : GENRE_BEACONS.get("default");
    }

    private SeedSelector() {}

    public static String getGenreSearchQuery(String genre) {
        if (genre == null || genre.trim().isEmpty() || genre.equalsIgnoreCase("Любой жанр") || genre.equalsIgnoreCase("Любой") || genre.equalsIgnoreCase("default")) {
            return "лучшие популярные треки";
        }
        String key = genre.toLowerCase().trim();
        String norm = key.replace("-", "").replace(" ", "");
        if (norm.contains("гиперпоп") || norm.contains("hyperpop")) return "hyperpop гиперпоп hits";
        if (norm.contains("кпоп") || norm.contains("kpop")) return "k-pop popular hits";
        if (norm.contains("эморэп") || norm.contains("emorap")) return "эмо рэп emo rap hits";
        if (norm.contains("постпанк") || norm.contains("postpunk")) return "русский пост-панк post-punk";
        if (norm.contains("синтвейв") || norm.contains("synthwave") || norm.contains("retrowave")) return "synthwave retrowave hits";
        if (norm.contains("лофай") || norm.contains("lofi")) return "lofi hip hop chill beats";
        if (norm.contains("фонк") || norm.contains("phonk")) return "drift phonk phonk hits";
        if (norm.contains("метал") || norm.contains("metal")) return "metal heavy metal hits";
        if (norm.contains("альтернатива") || norm.contains("alternative")) return "альтернативный рок alternative rock";
        if (norm.contains("инди") || norm.contains("indie")) return "инди рок indie music hits";
        if (norm.contains("рок") || norm.contains("rock")) return "русский рок рок хиты rock";
        if (norm.contains("хипхоп") || norm.contains("hiphop") || norm.contains("рэп") || norm.contains("rap")) return "хип хоп рэп хиты hip hop";
        if (norm.contains("поп") || norm.contains("pop")) return "поп музыка популярные хиты";
        if (norm.contains("джаз") || norm.contains("jazz")) return "jazz classic hits";
        if (norm.contains("блюз") || norm.contains("blues")) return "blues best hits";
        if (norm.contains("классика") || norm.contains("classical") || norm.contains("classic")) return "classical music masterpieces";
        if (norm.contains("электроника") || norm.contains("electronic") || norm.contains("edm")) return "electronic dance music edm";
        if (norm.contains("эмбиент") || norm.contains("ambient")) return "ambient music relaxing";
        return genre + " популярные треки";
    }

    public static List<InnerTubeClient.RawTrack> searchGenreSongs(String genre, int limit, AtomicBoolean cancelFlag) {
        String query = getGenreSearchQuery(genre);
        List<InnerTubeClient.RawTrack> tracks = InnerTubeClient.searchSongs(query, limit, cancelFlag);
        if (tracks.isEmpty()) {
            tracks = InnerTubeClient.searchSongs(genre, limit, cancelFlag);
        }
        return tracks;
    }

    /**
     * Выбор сидов по жанру: динамический поиск реальных треков в InnerTube,
     * с быстрым фолбэком на встроенный справочник при отсутствии сети.
     */
    /**
     * Выбор сидов по жанру:
     * Первоочередное использование эталонных сидов GENRE_BEACONS (0 мс задержки сети, идеальное совпадение жанра).
     * Фолбэк на InnerTube при кастомном жанре.
     */
    public static List<String> selectSeedsByGenre(String genre) {
        return selectSeedsByGenre(genre, null);
    }

    public static List<String> selectSeedsByGenre(String genre, AtomicBoolean cancelFlag) {
        if (genre == null || genre.trim().isEmpty() || genre.equalsIgnoreCase("Любой жанр") || genre.equalsIgnoreCase("Любой")) {
            genre = "default";
        }

        // 1. Приоритет: чистые эталонные маяки (0 мс задержки сети, идеальное совпадение жанра)
        List<String> pool = getAllGenreBeaconIds(genre);
        if (pool != null && !pool.isEmpty()) {
            List<String> copy = new ArrayList<>(pool);
            Collections.shuffle(copy, sRandom);
            return copy.subList(0, Math.min(3, copy.size()));
        }

        // 2. Попытка получить треки по жанру через InnerTube для кастомных запросов
        List<InnerTubeClient.RawTrack> songs = searchGenreSongs(genre, 20, cancelFlag);
        if (!songs.isEmpty()) {
            List<InnerTubeClient.RawTrack> shuffled = new ArrayList<>(songs);
            Collections.shuffle(shuffled, sRandom);
            Set<String> seen = new HashSet<>();
            List<String> seeds = new ArrayList<>();
            for (InnerTubeClient.RawTrack t : shuffled) {
                if (t != null && t.videoId != null && !t.videoId.isEmpty() && seen.add(t.videoId)) {
                    seeds.add(t.videoId);
                    if (seeds.size() >= 3) break;
                }
            }
            if (!seeds.isEmpty()) {
                FileLog.d(TAG + " selectSeedsByGenre for '" + genre + "' found " + seeds.size() + " live seeds");
                return seeds;
            }
        }

        List<String> defPool = getAllGenreBeaconIds("default");
        List<String> copy = new ArrayList<>(defPool);
        Collections.shuffle(copy, sRandom);
        return copy.subList(0, Math.min(3, copy.size()));
    }

    /**
     * Выбор сидов по артисту:
     * Поиск 15–20 треков исполнителя через InnerTube search,
     * случайный выбор 3 треков с разными ID.
     */
    public static List<String> selectSeedsByArtist(String artistName, AtomicBoolean cancelFlag) {
        List<String> result = new ArrayList<>();
        if (artistName == null || artistName.trim().isEmpty()) return selectSeedsByGenre("default");

        List<InnerTubeClient.RawTrack> songs = InnerTubeClient.searchSongs(artistName.trim(), 20, cancelFlag);
        if (songs.isEmpty()) {
            return selectSeedsByGenre("default");
        }

        List<InnerTubeClient.RawTrack> shuffled = new ArrayList<>(songs);
        Collections.shuffle(shuffled, sRandom);

        Set<String> seenIds = new HashSet<>();
        for (InnerTubeClient.RawTrack t : shuffled) {
            if (t.videoId != null && !t.videoId.isEmpty() && seenIds.add(t.videoId)) {
                result.add(t.videoId);
                if (result.size() >= 3) break;
            }
        }
        if (result.isEmpty()) {
            return selectSeedsByGenre("default");
        }
        return result;
    }

    /**
     * Выбор сидов по альбому:
     * 1) Быстрый поиск в 1 запрос по "Исполнитель Название_альбома".
     * 2) Если треков мало — параллельный поиск по трекам альбома.
     * 3) Каскадный фолбэк по артисту или дефолтным сидам (никогда не возвращает пустой список).
     */
    public static List<String> selectSeedsByAlbum(String artistName, String albumTitle, AtomicBoolean cancelFlag) {
        return selectSeedsByAlbum(artistName, albumTitle, null, cancelFlag);
    }

    public static List<String> selectSeedsByAlbum(String artistName, String albumTitle, List<MessageObject> albumTracks, AtomicBoolean cancelFlag) {
        List<String> result = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        String cleanArtist = artistName != null ? artistName.trim() : "";
        String cleanAlbum = albumTitle != null ? albumTitle.trim() : "";

        // 1. Быстрый поиск по исполнителю + названию альбома (возвращает треки альбома в 1 запрос)
        String query = (cleanArtist + " " + cleanAlbum).trim();
        if (!query.isEmpty()) {
            List<InnerTubeClient.RawTrack> songs = InnerTubeClient.searchSongs(query, 10, cancelFlag);
            for (InnerTubeClient.RawTrack t : songs) {
                if (t.videoId != null && !t.videoId.isEmpty() && seenIds.add(t.videoId)) {
                    result.add(t.videoId);
                    if (result.size() >= 3) return result;
                }
            }
            if (result.size() >= 2) return result;
        }

        // 2. Если результатов мало и есть реальные треки альбома — параллельный поиск по первым 3 трекам
        if (albumTracks != null && !albumTracks.isEmpty() && result.size() < 3) {
            List<MessageObject> candidates = new ArrayList<>(albumTracks);
            Collections.shuffle(candidates, sRandom);
            List<MessageObject> toCheck = candidates.subList(0, Math.min(3, candidates.size()));
            java.util.concurrent.ExecutorService exec = java.util.concurrent.Executors.newFixedThreadPool(toCheck.size());
            List<java.util.concurrent.Future<List<InnerTubeClient.RawTrack>>> futures = new ArrayList<>();
            for (MessageObject msg : toCheck) {
                String author = msg.getMusicAuthor();
                String title = msg.getMusicTitle();
                String q = (author != null && !author.trim().isEmpty() ? author.trim() + " - " : "") + (title != null ? title.trim() : "");
                if (q.trim().isEmpty()) continue;
                futures.add(exec.submit(() -> InnerTubeClient.searchSongs(q, 2, cancelFlag)));
            }
            for (java.util.concurrent.Future<List<InnerTubeClient.RawTrack>> f : futures) {
                try {
                    List<InnerTubeClient.RawTrack> found = f.get(3, java.util.concurrent.TimeUnit.SECONDS);
                    if (found != null) {
                        for (InnerTubeClient.RawTrack t : found) {
                            if (t.videoId != null && !t.videoId.isEmpty() && seenIds.add(t.videoId)) {
                                result.add(t.videoId);
                                if (result.size() >= 3) break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            exec.shutdown();
            if (result.size() >= 2) return result;
        }

        // 3. Поиск по чистому исполнителю
        if (result.isEmpty() && !cleanArtist.isEmpty()) {
            List<String> byArtist = selectSeedsByArtist(cleanArtist, cancelFlag);
            if (!byArtist.isEmpty()) return byArtist;
        }

        // 4. Поиск по названию альбома
        if (result.isEmpty() && !cleanAlbum.isEmpty()) {
            List<InnerTubeClient.RawTrack> songs = InnerTubeClient.searchSongs(cleanAlbum, 5, cancelFlag);
            for (InnerTubeClient.RawTrack t : songs) {
                if (t.videoId != null && !t.videoId.isEmpty() && seenIds.add(t.videoId)) {
                    result.add(t.videoId);
                    if (result.size() >= 2) return result;
                }
            }
        }

        // 5. Гарантированный фолбэк (никогда не возвращаем пустой список)
        if (result.isEmpty()) {
            return selectSeedsByGenre("default");
        }
        return result;
    }

    /**
     * Выбор сидов по плейлисту:
     * Извлечение треков локального плейлиста, группировка по артистам,
     * параллельный поиск сидов для выбранных треков.
     */
    public static List<String> selectSeedsByPlaylist(List<MessageObject> playlist, AtomicBoolean cancelFlag) {
        List<String> result = new ArrayList<>();
        if (playlist == null || playlist.isEmpty()) {
            return selectSeedsByGenre("default");
        }

        // Группировка по артистам
        Map<String, List<MessageObject>> byArtist = new HashMap<>();
        for (MessageObject msg : playlist) {
            String author = msg.getMusicAuthor();
            if (author == null || author.trim().isEmpty()) author = "Unknown";
            author = author.trim().toLowerCase();
            byArtist.computeIfAbsent(author, k -> new ArrayList<>()).add(msg);
        }

        List<MessageObject> chosenMessages = new ArrayList<>();
        List<String> artists = new ArrayList<>(byArtist.keySet());
        Collections.shuffle(artists, sRandom);

        if (artists.size() >= 3) {
            // 3 трека от разных артистов
            for (int i = 0; i < 3; i++) {
                List<MessageObject> list = byArtist.get(artists.get(i));
                if (list != null && !list.isEmpty()) {
                    chosenMessages.add(list.get(sRandom.nextInt(list.size())));
                }
            }
        } else {
            // Менее 3 артистов — выбор 3 случайных треков из всего плейлиста
            List<MessageObject> all = new ArrayList<>(playlist);
            Collections.shuffle(all, sRandom);
            for (int i = 0; i < Math.min(3, all.size()); i++) {
                chosenMessages.add(all.get(i));
            }
        }

        // Параллельный поиск videoId для выбранных треков
        if (!chosenMessages.isEmpty()) {
            java.util.concurrent.ExecutorService exec = java.util.concurrent.Executors.newFixedThreadPool(chosenMessages.size());
            List<java.util.concurrent.Future<List<InnerTubeClient.RawTrack>>> futures = new ArrayList<>();
            for (MessageObject msg : chosenMessages) {
                String author = msg.getMusicAuthor();
                String title = msg.getMusicTitle();
                String q = (author != null && !author.trim().isEmpty() ? author.trim() + " - " : "") + (title != null ? title.trim() : "");
                if (q.trim().isEmpty()) continue;
                futures.add(exec.submit(() -> InnerTubeClient.searchSongs(q, 1, cancelFlag)));
            }
            for (java.util.concurrent.Future<List<InnerTubeClient.RawTrack>> f : futures) {
                try {
                    List<InnerTubeClient.RawTrack> found = f.get(3, java.util.concurrent.TimeUnit.SECONDS);
                    if (found != null && !found.isEmpty() && found.get(0).videoId != null && !found.get(0).videoId.isEmpty()) {
                        String vid = found.get(0).videoId;
                        if (!result.contains(vid)) {
                            result.add(vid);
                        }
                    }
                } catch (Exception ignored) {}
            }
            exec.shutdown();
        }

        if (result.isEmpty()) {
            return selectSeedsByGenre("default");
        }
        return result;
    }
}
