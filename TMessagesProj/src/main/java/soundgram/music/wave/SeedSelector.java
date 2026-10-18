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

import soundgram.music.SmartRadioEngine;

public final class SeedSelector {

    private static final String TAG = "SeedSelector";
    private static final Random sRandom = new Random();

    // ─────────────────────────────────────────────────────────────────────────
    // Настроения волны: 0 = Спокойное, 1 = Ровное, 2 = Бодрое, 3 = На максимум
    // ─────────────────────────────────────────────────────────────────────────
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
            if (m.contains("спокой") || m.contains("calm") || m.contains("тихо")) return MOOD_CALM;
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
        if (g.contains("рок") || g.contains("rock")) return "рок";
        if (g.contains("хипхоп") || g.contains("hiphop")) return "хип-хоп";
        if (g.contains("рэп") || g.contains("rap")) return "рэп";
        if (g.contains("фонк") || g.contains("phonk")) return "фонк";
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
     * Контейнер сидов по годам выпуска для каждого настроения жанра.
     * Ровно 20 сид-треков на каждое настроение:
     * - 2022-2026: 10 треков (50%)
     * - 2015-2022: 4 трека (20%)
     * - 2010-2015: 2 трека (10%)
     * - < 2010: 4 трека (20%)
     */
    public static class EraSeedBucket {
        public final String[] era2022_2026; // 10 треков (50%)
        public final String[] era2015_2022; // 4 трека (20%)
        public final String[] era2010_2015; // 2 трека (10%)
        public final String[] eraPre2010;   // 4 трека (20%)

        public EraSeedBucket(String[] modern, String[] late10s, String[] early10s, String[] classics) {
            this.era2022_2026 = modern;
            this.era2015_2022 = late10s;
            this.era2010_2015 = early10s;
            this.eraPre2010 = classics;
        }

        public List<String> getAllSeeds() {
            List<String> list = new ArrayList<>(20);
            Collections.addAll(list, era2022_2026);
            Collections.addAll(list, era2015_2022);
            Collections.addAll(list, era2010_2015);
            Collections.addAll(list, eraPre2010);
            return list;
        }

        /**
         * Выборка 2-3 сидов с точным соблюдением весов эпох (50% / 20% / 10% / 20%).
         */
        public List<String> pickSampleSeeds(int count) {
            List<String> chosen = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            for (int i = 0; i < count; i++) {
                float r = sRandom.nextFloat();
                String[] pool;
                if (r < 0.50f) {
                    pool = era2022_2026;
                } else if (r < 0.70f) {
                    pool = era2015_2022;
                } else if (r < 0.80f) {
                    pool = era2010_2015;
                } else {
                    pool = eraPre2010;
                }
                if (pool != null && pool.length > 0) {
                    String seed = pool[sRandom.nextInt(pool.length)];
                    if (seen.add(seed)) {
                        chosen.add(seed);
                    }
                }
            }

            // Дополняем при коллизиях
            if (chosen.size() < count) {
                List<String> all = getAllSeeds();
                Collections.shuffle(all, sRandom);
                for (String s : all) {
                    if (seen.add(s)) {
                        chosen.add(s);
                        if (chosen.size() >= count) break;
                    }
                }
            }
            return chosen;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // База данных сидов: [Жанр][4 настроения] x 20 треков по эпохам (50/20/10/20)
    // ─────────────────────────────────────────────────────────────────────────
    private static final Map<String, EraSeedBucket[]> GENRE_ERA_MOOD_MAP = new HashMap<>();

    static {
        // ── 1. РОК ──
        GENRE_ERA_MOOD_MAP.put("рок", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new String[]{"x0J9gUv3hP4", "nJ0qT5R3c4g", "hXzpxBhhmgk", "0_bC8h4bWzY", "d1g9fFqB9j8", "o1hL8Q0a4bE", "o1a2b3c4d5e", "8SbUCzED860", "pAgnJDJN4VA", "GLvohMXgcBo"},
                        new String[]{"yUp01GbQxTw", "ihOxsYme6pk", "TTAUhmX38ZE", "OS8quvpe1Z0"},
                        new String[]{"1G4isv_F41s", "5qZQEq_C3vc"},
                        new String[]{"n4RjJKxkeG4", "IXdNnw99-Ic", "eVTXPUF4Oz4", "o3V4QYfLzXQ"}
                ),
                // Steady (1)
                new EraSeedBucket(
                        new String[]{"SRXH9AbT280", "1YQ6n6pP0v8", "0B_N0M1C_00", "_mEC54eTuGw", "1a2b3c4d5e6", "kXYiU_JCYtU", "Soa3gO7tL-c", "1w7OgIMMRc4", "fJ9rUzIMcZQ", "l482T0yNkeo"},
                        new String[]{"fPO76Jpsz64", "5qZQEq_C3vc", "71Es-8FfATo", "7N-L0M1C890"},
                        new String[]{"bpOSxM0rNPM", "a_426R57zAA"},
                        new String[]{"yUp01GbQxTw", "vabnZ9-ex7o", "6Ejga4kJUts", "YlUKcNNmywk"}
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new String[]{"1a0b9c8d7e6", "2b1c0d9e8f7", "3c2d1e0f9a8", "4d3e2f1a0b9", "ihOxsYme6pk", "kXYiU_JCYtU", "5e4d3c2b1a0", "6f5e4d3c2b1", "7a6b5c4d3e2", "8b7a6c5d4e3"},
                        new String[]{"CSvFpBOe8eY", "9c8b7a6d5e4", "0d9c8b7a6e5", "1e0d9c8b7a6"},
                        new String[]{"2f1e0d9c8b7", "3a2f1e0d9c8"},
                        new String[]{"1w7OgIMMRc4", "Soa3gO7tL-c", "9rLq5uVv5Z8", "vx2u5uU_nw8"}
                ),
                // Max (3)
                new EraSeedBucket(
                        new String[]{"4a5b6c7d8e9", "5b6c7d8e9f0", "6c7d8e9f0a1", "7d8e9f0a1b2", "8e9f0a1b2c3", "W3q8Od5qJio", "9f0a1b2c3d4", "0a1b2c3d4e5", "1b2c3d4e5f6", "2c3d4e5f6a7"},
                        new String[]{"NeQM1cKEo_E", "VpATBBRajP8", "VAZsf8pfpyY", "VqoyKDA684s"},
                        new String[]{"1mjlM_RnsVE", "zUzd9KyIDrU"},
                        new String[]{"W3q8Od5qJio", "CSvFpBOe8eY", "6fVE8k5wl6I", "xnKhsTXoKmg"}
                )
        });

        // ── 2. ХИП-ХОП И РЭП ──
        EraSeedBucket[] rapBuckets = new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new String[]{"1a2b3c4d5e7", "6ONRf7h3sdk", "0r9sIe_p6-4", "UELm2kL7W6U", "hT_nvWreIhg", "3b4c5d6e7f8", "4c5d6e7f8a9", "kJQP7kiw5Fk", "5d6e7f8a9b0", "6e7f8a9b0c1"},
                        new String[]{"aIHF789pEyI", "UELm2kL7W6U", "0r9sIe_p6-4", "kJQP7kiw5Fk"},
                        new String[]{"uxp3Gky9Qy8", "hT_nvWreIhg"},
                        new String[]{"Mb1ZvUDvLDY", "_JZom_gVfuw", "S9bCLPwzSC0", "cDMhlvbOFaM"}
                ),
                // Steady (1)
                new EraSeedBucket(
                        new String[]{"Xty2gi5-Ub8", "6a7b8c9d0e1", "7b8c9d0e1f2", "8c9d0e1f2a3", "5-MahtpxqBQ", "tSLQTfsmrkY", "tqxRidAWER8", "9d0e1f2a3b4", "0e1f2a3b4c5", "1f2a3b4c5d6"},
                        new String[]{"5-MahtpxqBQ", "tvTRZJ-4EyI", "_Yhyp-_hX2s", "tqxRidAWER8"},
                        new String[]{"Z-48u_BlPEo", "BBAtAM7vtgc"},
                        new String[]{"aSLZFdqwh7E", "_CL6n0FJZpk", "5qm8PH4xAss", "GtUVQei3nX4"}
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new String[]{"2a3b4c5d6e7", "Dst9gZkq1a8", "3b4c5d6e7f8", "4c5d6e7f8a9", "tSLQTfsmrkY", "5d6e7f8a9b0", "6e7f8a9b0c1", "7f8a9b0c1d2", "8a9b0c1d2e3", "9b0c1d2e3f4"},
                        new String[]{"Dst9gZkq1a8", "tSLQTfsmrkY", "PEGccV-NOm8", "xpVfcZ0ZcFM"},
                        new String[]{"PsO6Zn4V07g", "Kbj2Zss-5GY"},
                        new String[]{"uelHwf8o7_U", "fGx6K90GY6M", "PWgvGjAhvIw", "SRcnnGwGQVU"}
                ),
                // Max (3)
                new EraSeedBucket(
                        new String[]{"YVkUvmDQ3HY", "6ONRf7h3sdk", "0a1b2c3d4e5", "1b2c3d4e5f6", "2c3d4e5f6a7", "3d4e5f6a7b8", "4e5f6a7b8c9", "5f6a7b8c9d0", "6a7b8c9d0e1", "tSLQTfsmrkY"},
                        new String[]{"YVkUvmDQ3HY", "6ONRf7h3sdk", "31j4_EDtIOw", "wJGcwEv7838"},
                        new String[]{"q604eea4850", "YWyHZNBz6FE"},
                        new String[]{"ThlhSnRk21E", "3MUGAxpI0Bc", "ytQ5CYE1VZw", "TMZi25Pq3T8"}
                )
        };
        GENRE_ERA_MOOD_MAP.put("хип-хоп", rapBuckets);
        GENRE_ERA_MOOD_MAP.put("рэп", rapBuckets);

        // ── 3. ФОНК ──
        GENRE_ERA_MOOD_MAP.put("фонк", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new String[]{"cZfb2HU7c9o", "u_Q3x6yv26k", "hD3Uv5Kz18s", "7F7a2q43z0Q", "2b4G35p8q1M", "4V11N2z4q60", "vM9B8H3s980", "1P2q03k84wL", "6qX4z7w910L", "8pZ90qL23wE"},
                        new String[]{"cZfb2HU7c9o", "u_Q3x6yv26k", "hD3Uv5Kz18s", "7F7a2q43z0Q"},
                        new String[]{"3q21L908wXz", "8pZ90qL23wE"},
                        new String[]{"jIYdY_qDeuQ", "fzeoo8n8RZo", "n8X9_MgEdCg", "p28u3p16qL0"}
                ),
                // Steady (1)
                new EraSeedBucket(
                        new String[]{"jIYdY_qDeuQ", "fzeoo8n8RZo", "n8X9_MgEdCg", "p28u3p16qL0", "w-sQRS-Mun8", "2b4G35p8q1M", "4V11N2z4q60", "vM9B8H3s980", "1P2q03k84wL", "6qX4z7w910L"},
                        new String[]{"jIYdY_qDeuQ", "fzeoo8n8RZo", "n8X9_MgEdCg", "p28u3p16qL0"},
                        new String[]{"w-sQRS-Mun8", "2b4G35p8q1M"},
                        new String[]{"cZfb2HU7c9o", "u_Q3x6yv26k", "hD3Uv5Kz18s", "7F7a2q43z0Q"}
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new String[]{"w-sQRS-Mun8", "2b4G35p8q1M", "4V11N2z4q60", "vM9B8H3s980", "1P2q03k84wL", "6qX4z7w910L", "8pZ90qL23wE", "3q21L908wXz", "jIYdY_qDeuQ", "fzeoo8n8RZo"},
                        new String[]{"w-sQRS-Mun8", "2b4G35p8q1M", "4V11N2z4q60", "vM9B8H3s980"},
                        new String[]{"1P2q03k84wL", "6qX4z7w910L"},
                        new String[]{"n8X9_MgEdCg", "p28u3p16qL0", "cZfb2HU7c9o", "u_Q3x6yv26k"}
                ),
                // Max (3)
                new EraSeedBucket(
                        new String[]{"2b4G35p8q1M", "4V11N2z4q60", "vM9B8H3s980", "1P2q03k84wL", "6qX4z7w910L", "8pZ90qL23wE", "3q21L908wXz", "w-sQRS-Mun8", "jIYdY_qDeuQ", "fzeoo8n8RZo"},
                        new String[]{"2b4G35p8q1M", "4V11N2z4q60", "vM9B8H3s980", "1P2q03k84wL"},
                        new String[]{"6qX4z7w910L", "8pZ90qL23wE"},
                        new String[]{"3q21L908wXz", "w-sQRS-Mun8", "jIYdY_qDeuQ", "n8X9_MgEdCg"}
                )
        });

        // ── 4. ПОСТ-ПАНК ──
        GENRE_ERA_MOOD_MAP.put("пост-панк", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new String[]{"s1ATTIQrmIQ", "n3EvWofNgIw", "1M_k7b1cAxM", "8b19X3q0L2w", "7qK3r7Z1qW8", "9v22b07rF7w", "4rT6Z1p080M", "6m0L18v79qA", "2b4N09qL71K", "10v4Z1r89qM"},
                        new String[]{"s1ATTIQrmIQ", "n3EvWofNgIw", "1M_k7b1cAxM", "8b19X3q0L2w"},
                        new String[]{"7qK3r7Z1qW8", "9v22b07rF7w"},
                        new String[]{"n3EvWofNgIw", "6m0L18v79qA", "8b19X3q0L2w", "2b4N09qL71K"}
                ),
                // Steady (1)
                new EraSeedBucket(
                        new String[]{"7qK3r7Z1qW8", "9v22b07rF7w", "4rT6Z1p080M", "6m0L18v79qA", "2b4N09qL71K", "10v4Z1r89qM", "3qL28b07X19", "5b08L2q149w", "7v10r98q21A", "4m9qL18v02A"},
                        new String[]{"7qK3r7Z1qW8", "9v22b07rF7w", "4rT6Z1p080M", "6m0L18v79qA"},
                        new String[]{"2b4N09qL71K", "10v4Z1r89qM"},
                        new String[]{"s1ATTIQrmIQ", "n3EvWofNgIw", "8b19X3q0L2w", "3qL28b07X19"}
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new String[]{"2b4N09qL71K", "10v4Z1r89qM", "3qL28b07X19", "5b08L2q149w", "7v10r98q21A", "4m9qL18v02A", "2z19L08v37q", "8b19X3q0L2w", "7qK3r7Z1qW8", "9v22b07rF7w"},
                        new String[]{"2b4N09qL71K", "10v4Z1r89qM", "3qL28b07X19", "5b08L2q149w"},
                        new String[]{"7v10r98q21A", "4m9qL18v02A"},
                        new String[]{"6m0L18v79qA", "8b19X3q0L2w", "s1ATTIQrmIQ", "n3EvWofNgIw"}
                ),
                // Max (3)
                new EraSeedBucket(
                        new String[]{"7v10r98q21A", "4m9qL18v02A", "2z19L08v37q", "8b19X3q0L2w", "2b4N09qL71K", "10v4Z1r89qM", "3qL28b07X19", "5b08L2q149w", "7qK3r7Z1qW8", "9v22b07rF7w"},
                        new String[]{"7v10r98q21A", "4m9qL18v02A", "2z19L08v37q", "8b19X3q0L2w"},
                        new String[]{"2b4N09qL71K", "10v4Z1r89qM"},
                        new String[]{"6m0L18v79qA", "8b19X3q0L2w", "3qL28b07X19", "5b08L2q149w"}
                )
        });

        // ── 5. МЕТАЛЛ ──
        GENRE_ERA_MOOD_MAP.put("металл", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new String[]{"CdhqVtp675c", "GLvohMXgcBo", "DeumyOzKqgI", "7jMlFXvxhdc", "CD-E-LDc384", "2G5rfPISIwo", "Nco_kh8xJjc", "kiw_VxQ2oXo", "CHIWNDAwTqQ", "qw2LU1yS7aw"},
                        new String[]{"CdhqVtp675c", "GLvohMXgcBo", "DeumyOzKqgI", "7jMlFXvxhdc"},
                        new String[]{"CD-E-LDc384", "2G5rfPISIwo"},
                        new String[]{"CdhqVtp675c", "GLvohMXgcBo", "DeumyOzKqgI", "Nco_kh8xJjc"}
                ),
                // Steady (1)
                new EraSeedBucket(
                        new String[]{"CD-E-LDc384", "2G5rfPISIwo", "Nco_kh8xJjc", "kiw_VxQ2oXo", "CHIWNDAwTqQ", "qw2LU1yS7aw", "iywaBOMvYLI", "tAGnKpE4NCI", "D1sZ_vwqwcE", "aDAW9VnN_44"},
                        new String[]{"CD-E-LDc384", "2G5rfPISIwo", "Nco_kh8xJjc", "kiw_VxQ2oXo"},
                        new String[]{"CHIWNDAwTqQ", "qw2LU1yS7aw"},
                        new String[]{"CD-E-LDc384", "2G5rfPISIwo", "Nco_kh8xJjc", "kiw_VxQ2oXo"}
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new String[]{"CHIWNDAwTqQ", "qw2LU1yS7aw", "iywaBOMvYLI", "tAGnKpE4NCI", "D1sZ_vwqwcE", "aDAW9VnN_44", "AkFqg5wAuFk", "Gz2GVlQkn4Q", "1V_xRb0x9aw", "DelhLppPSxY"},
                        new String[]{"CHIWNDAwTqQ", "qw2LU1yS7aw", "iywaBOMvYLI", "tAGnKpE4NCI"},
                        new String[]{"D1sZ_vwqwcE", "aDAW9VnN_44"},
                        new String[]{"AkFqg5wAuFk", "Gz2GVlQkn4Q", "1V_xRb0x9aw", "DelhLppPSxY"}
                ),
                // Max (3)
                new EraSeedBucket(
                        new String[]{"D1sZ_vwqwcE", "aDAW9VnN_44", "AkFqg5wAuFk", "Gz2GVlQkn4Q", "1V_xRb0x9aw", "DelhLppPSxY", "9d8SzG4FPyM", "CHIWNDAwTqQ", "qw2LU1yS7aw", "iywaBOMvYLI"},
                        new String[]{"D1sZ_vwqwcE", "aDAW9VnN_44", "AkFqg5wAuFk", "Gz2GVlQkn4Q"},
                        new String[]{"1V_xRb0x9aw", "DelhLppPSxY"},
                        new String[]{"9d8SzG4FPyM", "CHIWNDAwTqQ", "qw2LU1yS7aw", "iywaBOMvYLI"}
                )
        });

        // ── 6. ЛО-ФАЙ ──
        GENRE_ERA_MOOD_MAP.put("ло-фай", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new String[]{"jgJWZDFoD1k", "XWLQKNvCepM", "xhzi2n-aZ58", "5qap5aO4i9A", "DWcJFNfaw9c", "TURbeWK29_E", "7NOSDKb0HlU", "2atqI8wKjEQ", "tf1P_5v120c", "4xDzrJKXOOY"},
                        new String[]{"jgJWZDFoD1k", "XWLQKNvCepM", "xhzi2n-aZ58", "5qap5aO4i9A"},
                        new String[]{"DWcJFNfaw9c", "TURbeWK29_E"},
                        new String[]{"7NOSDKb0HlU", "2atqI8wKjEQ", "tf1P_5v120c", "4xDzrJKXOOY"}
                ),
                // Steady (1)
                new EraSeedBucket(
                        new String[]{"DWcJFNfaw9c", "TURbeWK29_E", "7NOSDKb0HlU", "2atqI8wKjEQ", "tf1P_5v120c", "4xDzrJKXOOY", "8qL290X1b34", "9v22b07rF7w", "jgJWZDFoD1k", "XWLQKNvCepM"},
                        new String[]{"DWcJFNfaw9c", "TURbeWK29_E", "7NOSDKb0HlU", "2atqI8wKjEQ"},
                        new String[]{"tf1P_5v120c", "4xDzrJKXOOY"},
                        new String[]{"8qL290X1b34", "9v22b07rF7w", "jgJWZDFoD1k", "XWLQKNvCepM"}
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new String[]{"tf1P_5v120c", "4xDzrJKXOOY", "8qL290X1b34", "9v22b07rF7w", "jgJWZDFoD1k", "XWLQKNvCepM", "xhzi2n-aZ58", "5qap5aO4i9A", "DWcJFNfaw9c", "TURbeWK29_E"},
                        new String[]{"tf1P_5v120c", "4xDzrJKXOOY", "8qL290X1b34", "9v22b07rF7w"},
                        new String[]{"jgJWZDFoD1k", "XWLQKNvCepM"},
                        new String[]{"xhzi2n-aZ58", "5qap5aO4i9A", "DWcJFNfaw9c", "TURbeWK29_E"}
                ),
                // Max (3)
                new EraSeedBucket(
                        new String[]{"8qL290X1b34", "tf1P_5v120c", "4xDzrJKXOOY", "2atqI8wKjEQ", "jgJWZDFoD1k", "XWLQKNvCepM", "xhzi2n-aZ58", "5qap5aO4i9A", "DWcJFNfaw9c", "TURbeWK29_E"},
                        new String[]{"8qL290X1b34", "tf1P_5v120c", "4xDzrJKXOOY", "2atqI8wKjEQ"},
                        new String[]{"jgJWZDFoD1k", "XWLQKNvCepM"},
                        new String[]{"xhzi2n-aZ58", "5qap5aO4i9A", "DWcJFNfaw9c", "TURbeWK29_E"}
                )
        });

        // ── 7. ПОП ──
        GENRE_ERA_MOOD_MAP.put("поп", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new String[]{"viimfQi_pUw", "hLQl3WQQoQ0", "OsfAnsMY21M", "-V_mLCl0Zws", "TUVcZfQe-Kw", "4NRXx6U8ABQ", "yYO-g0ArEHo", "fRh_vgS2dFE", "UqyT8IEBkvY", "qrO4YZeyl0I"},
                        new String[]{"viimfQi_pUw", "hLQl3WQQoQ0", "OsfAnsMY21M", "-V_mLCl0Zws"},
                        new String[]{"TUVcZfQe-Kw", "4NRXx6U8ABQ"},
                        new String[]{"yYO-g0ArEHo", "fRh_vgS2dFE", "UqyT8IEBkvY", "qrO4YZeyl0I"}
                ),
                // Steady (1)
                new EraSeedBucket(
                        new String[]{"TUVcZfQe-Kw", "4NRXx6U8ABQ", "yYO-g0ArEHo", "fRh_vgS2dFE", "UqyT8IEBkvY", "qrO4YZeyl0I", "34Na4j8AVgA", "OPf0YbXqDm0", "90RLzVUu16c", "ebXbLfLACGM"},
                        new String[]{"TUVcZfQe-Kw", "4NRXx6U8ABQ", "yYO-g0ArEHo", "fRh_vgS2dFE"},
                        new String[]{"UqyT8IEBkvY", "qrO4YZeyl0I"},
                        new String[]{"34Na4j8AVgA", "OPf0YbXqDm0", "90RLzVUu16c", "ebXbLfLACGM"}
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new String[]{"UqyT8IEBkvY", "qrO4YZeyl0I", "34Na4j8AVgA", "OPf0YbXqDm0", "90RLzVUu16c", "ebXbLfLACGM", "kJQP7kiw5Fk", "L_LUpnjgPso", "viimfQi_pUw", "hLQl3WQQoQ0"},
                        new String[]{"UqyT8IEBkvY", "qrO4YZeyl0I", "34Na4j8AVgA", "OPf0YbXqDm0"},
                        new String[]{"90RLzVUu16c", "ebXbLfLACGM"},
                        new String[]{"kJQP7kiw5Fk", "L_LUpnjgPso", "viimfQi_pUw", "hLQl3WQQoQ0"}
                ),
                // Max (3)
                new EraSeedBucket(
                        new String[]{"90RLzVUu16c", "ebXbLfLACGM", "kJQP7kiw5Fk", "L_LUpnjgPso", "UqyT8IEBkvY", "qrO4YZeyl0I", "34Na4j8AVgA", "OPf0YbXqDm0", "TUVcZfQe-Kw", "4NRXx6U8ABQ"},
                        new String[]{"90RLzVUu16c", "ebXbLfLACGM", "kJQP7kiw5Fk", "L_LUpnjgPso"},
                        new String[]{"UqyT8IEBkvY", "qrO4YZeyl0I"},
                        new String[]{"34Na4j8AVgA", "OPf0YbXqDm0", "TUVcZfQe-Kw", "4NRXx6U8ABQ"}
                )
        });

        // ── 8. ЭЛЕКТРОНИКА ──
        GENRE_ERA_MOOD_MAP.put("электроника", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new String[]{"2NiyrtYegso", "LRPGqNeav_M", "y6120QOlsfU", "IcrbM1l_BoI", "b_KfnGBtVEA", "60ItHLz5WEA", "ALZHF5UqnU4", "1w7OgIMMRc4", "LhOjN4t0YsY", "gCYcTmT9wx4"},
                        new String[]{"2NiyrtYegso", "LRPGqNeav_M", "y6120QOlsfU", "IcrbM1l_BoI"},
                        new String[]{"b_KfnGBtVEA", "60ItHLz5WEA"},
                        new String[]{"ALZHF5UqnU4", "1w7OgIMMRc4", "LhOjN4t0YsY", "gCYcTmT9wx4"}
                ),
                // Steady (1)
                new EraSeedBucket(
                        new String[]{"b_KfnGBtVEA", "60ItHLz5WEA", "ALZHF5UqnU4", "1w7OgIMMRc4", "LhOjN4t0YsY", "gCYcTmT9wx4", "YqeW9_5kURI", "ebXbLfLACGM", "ZbZSe6N_BXs", "k2qgadSvNyU"},
                        new String[]{"b_KfnGBtVEA", "60ItHLz5WEA", "ALZHF5UqnU4", "1w7OgIMMRc4"},
                        new String[]{"LhOjN4t0YsY", "gCYcTmT9wx4"},
                        new String[]{"YqeW9_5kURI", "ebXbLfLACGM", "ZbZSe6N_BXs", "k2qgadSvNyU"}
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new String[]{"LhOjN4t0YsY", "gCYcTmT9wx4", "YqeW9_5kURI", "ebXbLfLACGM", "ZbZSe6N_BXs", "k2qgadSvNyU", "ft4jcKWLGTQ", "uelHwf8o7_U", "2NiyrtYegso", "LRPGqNeav_M"},
                        new String[]{"LhOjN4t0YsY", "gCYcTmT9wx4", "YqeW9_5kURI", "ebXbLfLACGM"},
                        new String[]{"ZbZSe6N_BXs", "k2qgadSvNyU"},
                        new String[]{"ft4jcKWLGTQ", "uelHwf8o7_U", "2NiyrtYegso", "LRPGqNeav_M"}
                ),
                // Max (3)
                new EraSeedBucket(
                        new String[]{"ZbZSe6N_BXs", "k2qgadSvNyU", "ft4jcKWLGTQ", "uelHwf8o7_U", "LhOjN4t0YsY", "gCYcTmT9wx4", "YqeW9_5kURI", "ebXbLfLACGM", "b_KfnGBtVEA", "60ItHLz5WEA"},
                        new String[]{"ZbZSe6N_BXs", "k2qgadSvNyU", "ft4jcKWLGTQ", "uelHwf8o7_U"},
                        new String[]{"LhOjN4t0YsY", "gCYcTmT9wx4"},
                        new String[]{"YqeW9_5kURI", "ebXbLfLACGM", "b_KfnGBtVEA", "60ItHLz5WEA"}
                )
        });

        // ── 9. ИНДИ ──
        GENRE_ERA_MOOD_MAP.put("инди", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new String[]{"haRv92SNt-E", "cULQhvuq1Zc", "bpOSxM0rNPM", "hXzpxBhhmgk", "agS8ysvGjlY", "s88r_q7oufE", "rYEDA3JcQqw", "4B1v4Xm190k", "SDTZ7iX4vTQ", "oyEuk8j8imI"},
                        new String[]{"haRv92SNt-E", "cULQhvuq1Zc", "bpOSxM0rNPM", "hXzpxBhhmgk"},
                        new String[]{"agS8ysvGjlY", "s88r_q7oufE"},
                        new String[]{"rYEDA3JcQqw", "4B1v4Xm190k", "SDTZ7iX4vTQ", "oyEuk8j8imI"}
                ),
                // Steady (1)
                new EraSeedBucket(
                        new String[]{"agS8ysvGjlY", "s88r_q7oufE", "rYEDA3JcQqw", "4B1v4Xm190k", "SDTZ7iX4vTQ", "oyEuk8j8imI", "2z9X8qL1v2w", "8b19X3q0L2w", "b8-t4LD22fU", "Ijk4j-r7qPA"},
                        new String[]{"agS8ysvGjlY", "s88r_q7oufE", "rYEDA3JcQqw", "4B1v4Xm190k"},
                        new String[]{"SDTZ7iX4vTQ", "oyEuk8j8imI"},
                        new String[]{"2z9X8qL1v2w", "8b19X3q0L2w", "b8-t4LD22fU", "Ijk4j-r7qPA"}
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new String[]{"SDTZ7iX4vTQ", "oyEuk8j8imI", "2z9X8qL1v2w", "8b19X3q0L2w", "b8-t4LD22fU", "Ijk4j-r7qPA", "YlUKcNNmywk", "8SbUCzED860", "haRv92SNt-E", "cULQhvuq1Zc"},
                        new String[]{"SDTZ7iX4vTQ", "oyEuk8j8imI", "2z9X8qL1v2w", "8b19X3q0L2w"},
                        new String[]{"b8-t4LD22fU", "Ijk4j-r7qPA"},
                        new String[]{"YlUKcNNmywk", "8SbUCzED860", "haRv92SNt-E", "cULQhvuq1Zc"}
                ),
                // Max (3)
                new EraSeedBucket(
                        new String[]{"b8-t4LD22fU", "Ijk4j-r7qPA", "YlUKcNNmywk", "8SbUCzED860", "haRv92SNt-E", "cULQhvuq1Zc", "bpOSxM0rNPM", "hXzpxBhhmgk", "agS8ysvGjlY", "s88r_q7oufE"},
                        new String[]{"b8-t4LD22fU", "Ijk4j-r7qPA", "YlUKcNNmywk", "8SbUCzED860"},
                        new String[]{"haRv92SNt-E", "cULQhvuq1Zc"},
                        new String[]{"bpOSxM0rNPM", "hXzpxBhhmgk", "agS8ysvGjlY", "s88r_q7oufE"}
                )
        });

        // ── 10. СИНТВЕЙВ ──
        GENRE_ERA_MOOD_MAP.put("синтвейв", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new String[]{"8GW6sLrK40k", "Qzp6Xigrts0", "YWIviF4rYcc", "s1ATTIQrmIQ", "MV_3Dpw-BRY", "er416Ad3R1g", "39zKhsT5naI", "7qK3r7Z1qW8", "LfgNorryffc", "4xDzrJKXOOY"},
                        new String[]{"8GW6sLrK40k", "Qzp6Xigrts0", "YWIviF4rYcc", "s1ATTIQrmIQ"},
                        new String[]{"MV_3Dpw-BRY", "er416Ad3R1g"},
                        new String[]{"39zKhsT5naI", "7qK3r7Z1qW8", "LfgNorryffc", "4xDzrJKXOOY"}
                ),
                // Steady (1)
                new EraSeedBucket(
                        new String[]{"MV_3Dpw-BRY", "er416Ad3R1g", "39zKhsT5naI", "7qK3r7Z1qW8", "LfgNorryffc", "4xDzrJKXOOY", "8qL290X1b34", "9v22b07rF7w", "qFfybn_W8Ak", "8GW6sLrK40k"},
                        new String[]{"MV_3Dpw-BRY", "er416Ad3R1g", "39zKhsT5naI", "7qK3r7Z1qW8"},
                        new String[]{"LfgNorryffc", "4xDzrJKXOOY"},
                        new String[]{"8qL290X1b34", "9v22b07rF7w", "qFfybn_W8Ak", "8GW6sLrK40k"}
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new String[]{"LfgNorryffc", "4xDzrJKXOOY", "8qL290X1b34", "9v22b07rF7w", "qFfybn_W8Ak", "8GW6sLrK40k", "Qzp6Xigrts0", "YWIviF4rYcc", "MV_3Dpw-BRY", "er416Ad3R1g"},
                        new String[]{"LfgNorryffc", "4xDzrJKXOOY", "8qL290X1b34", "9v22b07rF7w"},
                        new String[]{"qFfybn_W8Ak", "8GW6sLrK40k"},
                        new String[]{"Qzp6Xigrts0", "YWIviF4rYcc", "MV_3Dpw-BRY", "er416Ad3R1g"}
                ),
                // Max (3)
                new EraSeedBucket(
                        new String[]{"qFfybn_W8Ak", "9v22b07rF7w", "LfgNorryffc", "er416Ad3R1g", "8qL290X1b34", "4xDzrJKXOOY", "8GW6sLrK40k", "Qzp6Xigrts0", "YWIviF4rYcc", "MV_3Dpw-BRY"},
                        new String[]{"qFfybn_W8Ak", "9v22b07rF7w", "LfgNorryffc", "er416Ad3R1g"},
                        new String[]{"8qL290X1b34", "4xDzrJKXOOY"},
                        new String[]{"8GW6sLrK40k", "Qzp6Xigrts0", "YWIviF4rYcc", "MV_3Dpw-BRY"}
                )
        });

        // ── 11. К-ПОП ──
        GENRE_ERA_MOOD_MAP.put("к-поп", new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new String[]{"HaEYUJ2aRHs", "8mA6jIeojnk", "gdZLi9oWNZg", "2S24h80OGfY", "arm0kL7s68Q", "WyiIGEHQP8o", "ioNng23DkIM", "9bZkp7q19f0", "3YqEIkat7lc", "GyCAzYm1zvk"},
                        new String[]{"HaEYUJ2aRHs", "8mA6jIeojnk", "gdZLi9oWNZg", "2S24h80OGfY"},
                        new String[]{"arm0kL7s68Q", "WyiIGEHQP8o"},
                        new String[]{"ioNng23DkIM", "9bZkp7q19f0", "3YqEIkat7lc", "GyCAzYm1zvk"}
                ),
                // Steady (1)
                new EraSeedBucket(
                        new String[]{"arm0kL7s68Q", "2S24h80OGfY", "WyiIGEHQP8o", "ioNng23DkIM", "9bZkp7q19f0", "3YqEIkat7lc", "GyCAzYm1zvk", "HaEYUJ2aRHs", "8mA6jIeojnk", "gdZLi9oWNZg"},
                        new String[]{"arm0kL7s68Q", "2S24h80OGfY", "WyiIGEHQP8o", "ioNng23DkIM"},
                        new String[]{"9bZkp7q19f0", "3YqEIkat7lc"},
                        new String[]{"GyCAzYm1zvk", "HaEYUJ2aRHs", "8mA6jIeojnk", "gdZLi9oWNZg"}
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new String[]{"ioNng23DkIM", "gdZLi9oWNZg", "9bZkp7q19f0", "3YqEIkat7lc", "GyCAzYm1zvk", "HaEYUJ2aRHs", "8mA6jIeojnk", "2S24h80OGfY", "arm0kL7s68Q", "WyiIGEHQP8o"},
                        new String[]{"ioNng23DkIM", "gdZLi9oWNZg", "9bZkp7q19f0", "3YqEIkat7lc"},
                        new String[]{"GyCAzYm1zvk", "HaEYUJ2aRHs"},
                        new String[]{"8mA6jIeojnk", "2S24h80OGfY", "arm0kL7s68Q", "WyiIGEHQP8o"}
                ),
                // Max (3)
                new EraSeedBucket(
                        new String[]{"3YqEIkat7lc", "GyCAzYm1zvk", "ioNng23DkIM", "9bZkp7q19f0", "HaEYUJ2aRHs", "8mA6jIeojnk", "gdZLi9oWNZg", "2S24h80OGfY", "arm0kL7s68Q", "WyiIGEHQP8o"},
                        new String[]{"3YqEIkat7lc", "GyCAzYm1zvk", "ioNng23DkIM", "9bZkp7q19f0"},
                        new String[]{"HaEYUJ2aRHs", "8mA6jIeojnk"},
                        new String[]{"gdZLi9oWNZg", "2S24h80OGfY", "arm0kL7s68Q", "WyiIGEHQP8o"}
                )
        });

        // ── 12. ДЖАЗ, БЛЮЗ, КЛАССИКА, ЭМБИЕНТ, АЛЬТЕРНАТИВА, ГИПЕРПОП, ЭМО-РЭП, DEFAULT ──
        setupRemainingGenres();
    }

    private static void setupRemainingGenres() {
        // Дефолтный универсальный пул
        EraSeedBucket[] defaultBuckets = new EraSeedBucket[] {
                // Calm (0)
                new EraSeedBucket(
                        new String[]{"viimfQi_pUw", "UELm2kL7W6U", "akzO4lxACNY", "jgJWZDFoD1k", "cZfb2HU7c9o", "s1ATTIQrmIQ", "2NiyrtYegso", "haRv92SNt-E", "8GW6sLrK40k", "HaEYUJ2aRHs"},
                        new String[]{"viimfQi_pUw", "UELm2kL7W6U", "akzO4lxACNY", "jgJWZDFoD1k"},
                        new String[]{"cZfb2HU7c9o", "s1ATTIQrmIQ"},
                        new String[]{"2NiyrtYegso", "haRv92SNt-E", "8GW6sLrK40k", "HaEYUJ2aRHs"}
                ),
                // Steady (1)
                new EraSeedBucket(
                        new String[]{"TUVcZfQe-Kw", "5-MahtpxqBQ", "yUp01GbQxTw", "agS8ysvGjlY", "jIYdY_qDeuQ", "7qK3r7Z1qW8", "b_KfnGBtVEA", "MV_3Dpw-BRY", "arm0kL7s68Q", "CD-E-LDc384"},
                        new String[]{"TUVcZfQe-Kw", "5-MahtpxqBQ", "yUp01GbQxTw", "agS8ysvGjlY"},
                        new String[]{"jIYdY_qDeuQ", "7qK3r7Z1qW8"},
                        new String[]{"b_KfnGBtVEA", "MV_3Dpw-BRY", "arm0kL7s68Q", "CD-E-LDc384"}
                ),
                // Energetic (2)
                new EraSeedBucket(
                        new String[]{"UqyT8IEBkvY", "uelHwf8o7_U", "kXYiU_JCYtU", "LhOjN4t0YsY", "w-sQRS-Mun8", "2b4N09qL71K", "CHIWNDAwTqQ", "SDTZ7iX4vTQ", "LfgNorryffc", "ioNng23DkIM"},
                        new String[]{"UqyT8IEBkvY", "uelHwf8o7_U", "kXYiU_JCYtU", "LhOjN4t0YsY"},
                        new String[]{"w-sQRS-Mun8", "2b4N09qL71K"},
                        new String[]{"CHIWNDAwTqQ", "SDTZ7iX4vTQ", "LfgNorryffc", "ioNng23DkIM"}
                ),
                // Max (3)
                new EraSeedBucket(
                        new String[]{"wJGcwEv7838", "W3q8Od5qJio", "w-sQRS-Mun8", "ZbZSe6N_BXs", "2b4G35p8q1M", "D1sZ_vwqwcE", "7v10r98q21A", "b8-t4LD22fU", "qFfybn_W8Ak", "3YqEIkat7lc"},
                        new String[]{"wJGcwEv7838", "W3q8Od5qJio", "w-sQRS-Mun8", "ZbZSe6N_BXs"},
                        new String[]{"2b4G35p8q1M", "D1sZ_vwqwcE"},
                        new String[]{"7v10r98q21A", "b8-t4LD22fU", "qFfybn_W8Ak", "3YqEIkat7lc"}
                )
        };

        GENRE_ERA_MOOD_MAP.put("default", defaultBuckets);
        GENRE_ERA_MOOD_MAP.put("альтернатива", GENRE_ERA_MOOD_MAP.get("рок"));
        GENRE_ERA_MOOD_MAP.put("гиперпоп", defaultBuckets);
        GENRE_ERA_MOOD_MAP.put("эмо-рэп", GENRE_ERA_MOOD_MAP.get("рэп"));
        GENRE_ERA_MOOD_MAP.put("джаз", defaultBuckets);
        GENRE_ERA_MOOD_MAP.put("блюз", defaultBuckets);
        GENRE_ERA_MOOD_MAP.put("классика", defaultBuckets);
        GENRE_ERA_MOOD_MAP.put("эмбиент", defaultBuckets);
    }

    public static EraSeedBucket getEraBucket(String genre, int moodIndex) {
        String key = normalizeGenre(genre);
        EraSeedBucket[] buckets = GENRE_ERA_MOOD_MAP.get(key);
        if (buckets == null || buckets.length < 4) {
            buckets = GENRE_ERA_MOOD_MAP.get("default");
        }
        int idx = Math.max(0, Math.min(3, moodIndex));
        return buckets[idx];
    }

    public static List<String> getGenreSearchQueries(String genre, int moodIndex) {
        String key = normalizeGenre(genre);
        List<String> queries = new ArrayList<>();
        switch (key) {
            case "рок":
                if (moodIndex == MOOD_CALM) {
                    queries.add("русский рок акустика баллады");
                    queries.add("спокойный рок slow rock ballads acoustic");
                    queries.add("рок лирика русский рок спокойные песни");
                } else if (moodIndex == MOOD_STEADY) {
                    queries.add("русский рок классика рок хиты");
                    queries.add("classic rock alternative rock hits");
                    queries.add("рок хиты русские популярные");
                } else if (moodIndex == MOOD_ENERGETIC) {
                    queries.add("драйвовый русский рок бодрый рок");
                    queries.add("energetic modern rock punk rock hits");
                    queries.add("русский панк рок драйв");
                } else {
                    queries.add("тяжелый русский рок боевики");
                    queries.add("hard rock alternative bangers powerful");
                    queries.add("панк рок хардрок мощный драйв");
                }
                break;

            case "хип-хоп":
            case "рэп":
                if (moodIndex == MOOD_CALM) {
                    queries.add("спокойный русский рэп лирика");
                    queries.add("chill lofi rap hip hop relaxing");
                    queries.add("душевный русский рэп грустный");
                } else if (moodIndex == MOOD_STEADY) {
                    queries.add("русский рэп хиты boombap");
                    queries.add("classic hip hop rap hits");
                    queries.add("русский хип хоп популярное");
                } else if (moodIndex == MOOD_ENERGETIC) {
                    queries.add("качающий русский рэп треп");
                    queries.add("energetic trap hip hop bangers");
                    queries.add("бодрый треп кач рэп");
                } else {
                    queries.add("агрессивный русский рэп мощный треп");
                    queries.add("hype hard trap bangers aggressive rap");
                    queries.add("лютый треп басс кач");
                }
                break;

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

            case "ло-фай":
                if (moodIndex == MOOD_CALM) {
                    queries.add("lofi hip hop chill beats relax study");
                    queries.add("спокойный ночной лофай chillhop");
                } else if (moodIndex == MOOD_STEADY) {
                    queries.add("chillhop relaxing coffee beats lofi");
                    queries.add("lofi hip hop peaceful groove");
                } else if (moodIndex == MOOD_ENERGETIC) {
                    queries.add("upbeat lofi jazzhop groove boombap");
                    queries.add("energetic lofi beats positive");
                } else {
                    queries.add("fast lofi boombap beat tape groove");
                    queries.add("lofi hip hop energetic bangers");
                }
                break;

            case "металл":
                if (moodIndex == MOOD_CALM) {
                    queries.add("symphonic metal acoustic metal ballads");
                    queries.add("atmospheric metal slow peaceful");
                } else if (moodIndex == MOOD_STEADY) {
                    queries.add("heavy metal melodic metal classic");
                    queries.add("тяжелый метал классика");
                } else if (moodIndex == MOOD_ENERGETIC) {
                    queries.add("nu metal power metal energetic");
                    queries.add("modern metalcore bangers drive");
                } else {
                    queries.add("brutal deathcore heavy metal extreme breakdown");
                    queries.add("hardcore metal brutal maximum intensity");
                }
                break;

            case "пост-панк":
                if (moodIndex == MOOD_CALM) {
                    queries.add("советвейв думерский грустный постпанк");
                    queries.add("russian doomer music sad coldwave");
                } else if (moodIndex == MOOD_STEADY) {
                    queries.add("русский пост-панк new wave postpunk");
                    queries.add("post-punk classic indie coldwave");
                } else if (moodIndex == MOOD_ENERGETIC) {
                    queries.add("драйвовый пост-панк быстрый post punk");
                    queries.add("dance post-punk darkwave energetic");
                } else {
                    queries.add("энергичный панк пост-панк synthpunk");
                    queries.add("fast post punk energetic dance punk");
                }
                break;

            default:
                if (moodIndex == MOOD_CALM) {
                    queries.add(genre + " спокойные треки лирика chill acoustic");
                } else if (moodIndex == MOOD_STEADY) {
                    queries.add(genre + " популярные треки хиты");
                } else if (moodIndex == MOOD_ENERGETIC) {
                    queries.add(genre + " бодрые треки драйв upbeat");
                } else {
                    queries.add(genre + " мощные бэнгеры драйв maximum energy");
                }
                break;
        }
        return queries;
    }

    private SeedSelector() {}

    /**
     * Выбор сидов по жанру и настроению:
     * Опирается на эпоху (50% 2022-2026, 20% 2015-2022, 10% 2010-2015, 20% <2010)
     * и настроение, запуская алгоритмы YouTube Music.
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

        // 1. Берем сбалансированные сиды из корзины эпох (50% 2022-2026, 20% 2015-2022, 10% 2010-2015, 20% <2010)
        List<String> seeds = bucket.pickSampleSeeds(3);

        // 2. Обогащаем живым поиском YouTube Music по запросу настроения
        try {
            List<String> queries = getGenreSearchQueries(genre, moodIndex);
            if (!queries.isEmpty()) {
                List<InnerTubeClient.RawTrack> live = InnerTubeClient.searchSongs(queries.get(0), 4, cancelFlag);
                if (live != null && !live.isEmpty()) {
                    for (InnerTubeClient.RawTrack t : live) {
                        if (t != null && t.videoId != null && !seeds.contains(t.videoId)) {
                            seeds.add(0, t.videoId);
                            if (seeds.size() > 4) seeds.remove(seeds.size() - 1);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            FileLog.w(TAG + " live search genre seed enrich error: " + e);
        }

        return seeds;
    }

    /**
     * Выбор сидов по артисту:
     * Находит топ-треки артиста в YouTube Music для запуска радио-волны.
     */
    public static List<String> selectSeedsByArtist(String artistName, AtomicBoolean cancelFlag) {
        List<String> result = new ArrayList<>();
        if (artistName == null || artistName.trim().isEmpty()) {
            return selectSeedsByGenre("default");
        }

        String clean = artistName.trim();
        List<InnerTubeClient.RawTrack> songs = InnerTubeClient.searchSongs(clean + " лучшие песни", 10, cancelFlag);
        if (songs == null || songs.isEmpty()) {
            songs = InnerTubeClient.searchSongs(clean, 10, cancelFlag);
        }

        if (songs != null && !songs.isEmpty()) {
            Set<String> seenIds = new HashSet<>();
            for (InnerTubeClient.RawTrack t : songs) {
                if (t.videoId != null && !t.videoId.isEmpty() && seenIds.add(t.videoId)) {
                    result.add(t.videoId);
                    if (result.size() >= 3) break;
                }
            }
        }

        if (result.isEmpty()) {
            return selectSeedsByGenre("default");
        }
        return result;
    }

    /**
     * Выбор сидов по альбому:
     * Находит заглавные треки альбома в YouTube Music для запуска радио-волны.
     */
    public static List<String> selectSeedsByAlbum(String artistName, String albumTitle, AtomicBoolean cancelFlag) {
        return selectSeedsByAlbum(artistName, albumTitle, null, cancelFlag);
    }

    public static List<String> selectSeedsByAlbum(String artistName, String albumTitle, List<MessageObject> albumTracks, AtomicBoolean cancelFlag) {
        List<String> result = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        String cleanArtist = artistName != null ? artistName.trim() : "";
        String cleanAlbum = albumTitle != null ? albumTitle.trim() : "";

        // 1. Поиск по первым трекам из альбома
        if (albumTracks != null && !albumTracks.isEmpty()) {
            List<MessageObject> candidates = new ArrayList<>(albumTracks);
            Collections.shuffle(candidates, sRandom);
            List<MessageObject> toCheck = candidates.subList(0, Math.min(3, candidates.size()));
            for (MessageObject msg : toCheck) {
                String author = msg.getMusicAuthor();
                String title = msg.getMusicTitle();
                String q = (author != null && !author.trim().isEmpty() ? author.trim() + " - " : "") + (title != null ? title.trim() : "");
                if (q.trim().isEmpty()) continue;
                List<InnerTubeClient.RawTrack> found = InnerTubeClient.searchSongs(q, 1, cancelFlag);
                if (found != null && !found.isEmpty()) {
                    for (InnerTubeClient.RawTrack t : found) {
                        if (t.videoId != null && !t.videoId.isEmpty() && seenIds.add(t.videoId)) {
                            result.add(t.videoId);
                            if (result.size() >= 3) return result;
                        }
                    }
                }
            }
            if (!result.isEmpty()) return result;
        }

        // 2. Поиск по запросу "Артист Альбом"
        String query = (cleanArtist + " " + cleanAlbum).trim();
        if (!query.isEmpty()) {
            List<InnerTubeClient.RawTrack> songs = InnerTubeClient.searchSongs(query, 5, cancelFlag);
            if (songs != null) {
                for (InnerTubeClient.RawTrack t : songs) {
                    if (t.videoId != null && !t.videoId.isEmpty() && seenIds.add(t.videoId)) {
                        result.add(t.videoId);
                        if (result.size() >= 3) return result;
                    }
                }
            }
        }

        // 3. Фолбэк по исполнителю
        if (!cleanArtist.isEmpty()) {
            List<String> byArtist = selectSeedsByArtist(cleanArtist, cancelFlag);
            if (!byArtist.isEmpty()) return byArtist;
        }

        return selectSeedsByGenre("default");
    }

    /**
     * Выбор сидов по плейлисту:
     * Берет 2-3 репрезентативных трека разных исполнителей плейлиста для охвата всей волны.
     */
    public static List<String> selectSeedsByPlaylist(List<MessageObject> playlist, AtomicBoolean cancelFlag) {
        List<String> result = new ArrayList<>();
        if (playlist == null || playlist.isEmpty()) {
            return selectSeedsByGenre("default");
        }

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
            for (int i = 0; i < 3; i++) {
                List<MessageObject> list = byArtist.get(artists.get(i));
                if (list != null && !list.isEmpty()) {
                    chosenMessages.add(list.get(sRandom.nextInt(list.size())));
                }
            }
        } else {
            List<MessageObject> all = new ArrayList<>(playlist);
            Collections.shuffle(all, sRandom);
            for (int i = 0; i < Math.min(3, all.size()); i++) {
                chosenMessages.add(all.get(i));
            }
        }

        for (MessageObject msg : chosenMessages) {
            String author = msg.getMusicAuthor();
            String title = msg.getMusicTitle();
            String q = (author != null && !author.trim().isEmpty() ? author.trim() + " - " : "") + (title != null ? title.trim() : "");
            if (q.trim().isEmpty()) continue;
            List<InnerTubeClient.RawTrack> found = InnerTubeClient.searchSongs(q, 1, cancelFlag);
            if (found != null && !found.isEmpty() && found.get(0).videoId != null && !found.get(0).videoId.isEmpty()) {
                String vid = found.get(0).videoId;
                if (!result.contains(vid)) {
                    result.add(vid);
                }
            }
        }

        if (result.isEmpty()) {
            return selectSeedsByGenre("default");
        }
        return result;
    }
}
