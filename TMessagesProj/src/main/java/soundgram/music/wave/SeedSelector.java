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
        // Поп (RU + топ хиты)
        GENRE_BEACONS.put("поп", Arrays.asList(
                "Q5Z_h3EaL40", "k7hWJg7p3Zc", "0-7IHOXkiV8", "fJ9rUzIMcZQ", "OPf0YbXqDm0",
                "hT_nvWreIhg", "2Vv-BfVoq4g", "JGwWNGJdvx8", "SlPhMPnQ58k", "DyDfgMOUjCI",
                "bo_efYhYU2A", "Khlp_G9t1mE", "JFcgOboErZ0", "nYh-n7EOtMA", "ru0K8uYEZWw"
        ));
        GENRE_BEACONS.put("pop", GENRE_BEACONS.get("поп"));

        // Хип-хоп / Рэп (RU + топ)
        GENRE_BEACONS.put("хип-хоп", Arrays.asList(
                "nidQCt_85HC", "mO8i_Y3_8Xg", "y6F3p4Wv_vI", "_Yhyp-_hX2s", "tvTRZJ-4EyI",
                "Y2lXeygKqQY", "fPO76Jlnz6c", "U9BwWKXjVaI", "210R0ozmLwg", "V1Pl8CzNzCw",
                "gl1aHhXnN1k", "uelHwf8o7_U", "7pcG50F2b0A", "HYsz1hP0BFo", "I3t2y_0iE0U"
        ));
        GENRE_BEACONS.put("хип хоп", GENRE_BEACONS.get("хип-хоп"));
        GENRE_BEACONS.put("рэп", GENRE_BEACONS.get("хип-хоп"));
        GENRE_BEACONS.put("hip-hop", GENRE_BEACONS.get("хип-хоп"));
        GENRE_BEACONS.put("rap", GENRE_BEACONS.get("хип-хоп"));

        // Рок (RU + рок легенды)
        GENRE_BEACONS.put("рок", Arrays.asList(
                "V6rX2Pq0hVE", "kXYiU_JCYtU", "fJ9rUzIMcZQ", "hTWKbfoikeg", "eVTXPUF4Oz4",
                "1w7OgIMMRc4", "CSvFpBOe8eY", "lDK9QqIzhwk", "B32yjbCSVp8", "Nba3Tr_KhZU",
                "v2H4l9PenL4", "fKuV73u3xHQ", "7qukY4N1TGE", "6fVE8kOHElQ", "qN4ooNXhDYg"
        ));
        GENRE_BEACONS.put("rock", GENRE_BEACONS.get("рок"));

        // Электроника / Клубная
        GENRE_BEACONS.put("электроника", Arrays.asList(
                "ALZHF5UqnU4", "60ItHLz5WEA", "IxxstCcJlsc", "YqeW9_5kURI", "egtI2JQMMmc",
                "6n3pFFPSlW4", "p7YpLwXWvEQ", "DkeiKbqa02g", "5dbEhBKGOtY", "1G4isv_Fylg",
                "gCYcHz2FgFw", "3tmd-ClpJxA", "JR49dyo-y0E", "kdemFfbS5Uo", "8mYqG_01PFE"
        ));
        GENRE_BEACONS.put("electronic", GENRE_BEACONS.get("электроника"));
        GENRE_BEACONS.put("edm", GENRE_BEACONS.get("электроника"));

        // Фонк (Phonk / Drift Phonk)
        GENRE_BEACONS.put("фонк", Arrays.asList(
                "T1q70w_q63A", "Wha_bC9Z2pU", "sT2p3cI9gT0", "6rL3oZz3z2g", "Hk3_r7bCqFE",
                "zH3_8g0b2AE", "5lF4hX3x1E0", "8vJ3cO1k9bQ", "0kF3hP4x7jY", "9mB2wQ5z8L0",
                "3tK6oY2x5cA", "7lM1vB4y9p0", "2qZ5wT8c4kE", "4rX7bC1y0mY", "1tF5gK8x9pE"
        ));
        GENRE_BEACONS.put("phonk", GENRE_BEACONS.get("фонк"));

        // Гиперпоп
        GENRE_BEACONS.put("гиперпоп", Arrays.asList(
                "mP1Z8uFjD6s", "i_h32ZcQGgM", "4pY_qf9qj8E", "g07C8j_6f7o", "qyT_G_g3F0Y",
                "4F_l23w1o4E", "yGz4W_W9o6E", "z97qLNXeAMQ", "iTzF78_3E8g", "3R3fG7k128k"
        ));
        GENRE_BEACONS.put("hyperpop", GENRE_BEACONS.get("гиперпоп"));

        // Пост-панк
        GENRE_BEACONS.put("пост-панк", Arrays.asList(
                "HR5zpFs7YpY", "y7dZ6hVz1jE", "f0QkG8z9p6w", "T_6y71q3v_E", "k5wY8r6o1kE",
                "8s_s6fC8hG4", "9GkJ8Nz5EYY", "zuuObGsB0No", "X6Y1q8w7j2A", "3tM9p1v6z4E"
        ));
        GENRE_BEACONS.put("пост панк", GENRE_BEACONS.get("пост-панк"));
        GENRE_BEACONS.put("постпанк", GENRE_BEACONS.get("пост-панк"));
        GENRE_BEACONS.put("post-punk", GENRE_BEACONS.get("пост-панк"));
        GENRE_BEACONS.put("postpunk", GENRE_BEACONS.get("пост-панк"));

        // Эмо-рэп
        GENRE_BEACONS.put("эмо-рэп", Arrays.asList(
                "bH1K6t2F8Y4", "L8p6v0K4Y1A", "x8Y1p9q2k6E", "y5V1k8z9o4E", "1b9C6w7k3_A",
                "mzB1V9nY-6E", "FAucVPE38P4", "x000F8bHhX8", "p1K7t8w4z6E", "-jRKsiAOAA8"
        ));
        GENRE_BEACONS.put("эмо рэп", GENRE_BEACONS.get("эмо-рэп"));
        GENRE_BEACONS.put("эморэп", GENRE_BEACONS.get("эмо-рэп"));
        GENRE_BEACONS.put("emo-rap", GENRE_BEACONS.get("эмо-рэп"));
        GENRE_BEACONS.put("emorap", GENRE_BEACONS.get("эмо-рэп"));

        // Металл / Метал
        GENRE_BEACONS.put("металл", Arrays.asList(
                "W3q8Od5qJio", "CD-E-LDc384", "Wws7acXdUCQ", "iywaBOMvYLI", "SQtehnq1sVU",
                "AkF9TPy4gE0", "kIBeYoP9Wi0", "7lM1vB4y9p0", "jRGrNDV2NWo", "5rN4wX8c1k0"
        ));
        GENRE_BEACONS.put("метал", GENRE_BEACONS.get("металл"));
        GENRE_BEACONS.put("metal", GENRE_BEACONS.get("металл"));

        // Альтернатива
        GENRE_BEACONS.put("альтернатива", Arrays.asList(
                "eVTXPUF4Oz4", "kXYiU_JCYtU", "1tF5gK8x9pE", "z7G0o8w1q4E", "4pZ6o9y1k2A",
                "7kP1v4y8z0E", "hTWKbfoikeg", "XFkzRNyygfk", "YlUKcNNmywk", "VrZ4sMRYimw"
        ));
        GENRE_BEACONS.put("alternative", GENRE_BEACONS.get("альтернатива"));

        // К-поп
        GENRE_BEACONS.put("к-поп", Arrays.asList(
                "gdZLi9oWNZg", "ioNng23DkIM", "OvioeS1ZZ7o", "ArmDp-zijuc", "i0p1bmr0EmE",
                "WMweEpGlu_U", "gQlMMD8auMs", "TQTlCHxyuu8", "9bZkp7q19f0", "pSudEWBAYRE"
        ));
        GENRE_BEACONS.put("к поп", GENRE_BEACONS.get("к-поп"));
        GENRE_BEACONS.put("кпоп", GENRE_BEACONS.get("к-поп"));
        GENRE_BEACONS.put("k-pop", GENRE_BEACONS.get("к-поп"));
        GENRE_BEACONS.put("kpop", GENRE_BEACONS.get("к-поп"));

        // Инди
        GENRE_BEACONS.put("инди", Arrays.asList(
                "m7Bc3JBkW4s", "b1kbLwvqugk", "d1acEVmnVhI", "YQHsXMglC9A", "bpOSxM0rNPM",
                "a18p1A_L7Y0", "1u-niluB8HI", "79DijItQXMM", "rYEDA3JcQqw", "yPYZpwSpKmA"
        ));
        GENRE_BEACONS.put("indie", GENRE_BEACONS.get("инди"));

        // R&B / Soul
        GENRE_BEACONS.put("r&b", Arrays.asList(
                "f1r0250KKAI", "450p7goxZqg", "u9n7Cw-4_PE", "vGJTaP6anOU", "QcIy9NiNbmo",
                "C-u5WLJ9nkU", "73_1biulkYk", "xYtsL94vW4s", "F0r7vY4q1k0", "3pM5tC8y9bE"
        ));
        GENRE_BEACONS.put("rnb", GENRE_BEACONS.get("r&b"));
        GENRE_BEACONS.put("соул", GENRE_BEACONS.get("r&b"));
        GENRE_BEACONS.put("soul", GENRE_BEACONS.get("r&b"));

        // Джаз
        GENRE_BEACONS.put("джаз", Arrays.asList(
                "vmDDOFXSg2k", "PoPL7BExSQU", "fN6GZZm7_cQ", "zqNTltOGh5c", "8B1oPE8_mw8",
                "1cRNEv59_m8", "U47f5pX8k0E", "9pM2vP4z7b0", "3tK6wY1x8cE", "5rN4bC9y2p0"
        ));
        GENRE_BEACONS.put("jazz", GENRE_BEACONS.get("джаз"));

        // Блюз
        GENRE_BEACONS.put("блюз", Arrays.asList(
                "oica5jG7FpU", "4O_YMLDvvnw", "w5IOou6qN1o", "10qLYy6hiFQ", "0vo23vN460c",
                "sB6HyvC6bHE", "rOyj4ecwcEE", "Yd60nI43AAw", "8B1oPE8_mw8", "vmDDOFXSg2k"
        ));
        GENRE_BEACONS.put("blues", GENRE_BEACONS.get("блюз"));

        // Классика
        GENRE_BEACONS.put("классика", Arrays.asList(
                "fEOJQawykD0", "EoaPhxNubL0", "RxabLA7UQ9k", "mFWQgxWD_Kw", "9E6b3swbnWg",
                "4Tr0otuiQuU", "9rJoB7y6Ncs", "b_YHE4Sx-08", "WNcsUNKlAKw", "k1-TrAvp_xs"
        ));
        GENRE_BEACONS.put("классическая", GENRE_BEACONS.get("классика"));
        GENRE_BEACONS.put("classical", GENRE_BEACONS.get("классика"));
        GENRE_BEACONS.put("classic", GENRE_BEACONS.get("классика"));

        // Синтвейв
        GENRE_BEACONS.put("синтвейв", Arrays.asList(
                "MV_3Dpw-BRY", "rV3bKq1-a58", "er416XiUp4g", "RY66fdslhsg", "8GW6sLrK40k",
                "-EDtFZZpW_k", "U4Z9n8tK4gE", "7vK1t4w8z0E", "nLzjm9E128k", "3pM5tC8y9bE"
        ));
        GENRE_BEACONS.put("синт-вейв", GENRE_BEACONS.get("синтвейв"));
        GENRE_BEACONS.put("синт вейв", GENRE_BEACONS.get("синтвейв"));
        GENRE_BEACONS.put("synthwave", GENRE_BEACONS.get("синтвейв"));
        GENRE_BEACONS.put("retrowave", GENRE_BEACONS.get("синтвейв"));

        // Ло-фай
        GENRE_BEACONS.put("ло-фай", Arrays.asList(
                "b4pZ4_o3G1A", "o2Z8_q1x7bE", "pYw4g8z9o1E", "F0r7vY4q1k0", "8vJ3cO1k9bQ",
                "3tK6wY1x8cE", "9mB2wQ5z8L0", "1tF5gK8x9pE", "2qZ5wT8c4kE", "4rX7bC1y0mY"
        ));
        GENRE_BEACONS.put("лофай", GENRE_BEACONS.get("ло-фай"));
        GENRE_BEACONS.put("ло фай", GENRE_BEACONS.get("ло-фай"));
        GENRE_BEACONS.put("lo-fi", GENRE_BEACONS.get("ло-фай"));
        GENRE_BEACONS.put("lofi", GENRE_BEACONS.get("ло-фай"));

        // Эмбиент
        GENRE_BEACONS.put("эмбиент", Arrays.asList(
                "aKw5mbc774Q", "_AWIqXzvXHA", "UfcAVejslrU", "7lM1vB4y9p0", "0kF3hP4x7jY",
                "5dbEhBKGOtY", "60ItHLz5WEA", "mjnAE5go9dI", "gCYcHz2FgFw", "ALZHF5UqnU4"
        ));
        GENRE_BEACONS.put("ambient", GENRE_BEACONS.get("эмбиент"));

        // Дефолтный пул (микс популярных треков)
        GENRE_BEACONS.put("default", Arrays.asList(
                "nidQCt_85HC", "mO8i_Y3_8Xg", "Q5Z_h3EaL40", "y6F3p4Wv_vI", "V6rX2Pq0hVE",
                "T1q70w_q63A", "YkgkTh308XU", "kJQP7kiw5Fk", "0-7IHOXkiV8", "_Yhyp-_hX2s",
                "tvTRZJ-4EyI", "kXYiU_JCYtU", "ALZHF5UqnU4", "m7Bc3JBkW4s", "Wha_bC9Z2pU"
        ));
        GENRE_BEACONS.put("любой жанр", GENRE_BEACONS.get("default"));
        GENRE_BEACONS.put("любой", GENRE_BEACONS.get("default"));
    }

    private SeedSelector() {}

    public static String getGenreSearchQuery(String genre) {
        if (genre == null || genre.trim().isEmpty() || genre.equalsIgnoreCase("Любой жанр") || genre.equalsIgnoreCase("Любой") || genre.equalsIgnoreCase("default")) {
            return "лучшие популярные треки";
        }
        String key = genre.toLowerCase().trim();
        String norm = key.replace("-", "").replace(" ", "");
        if (norm.contains("рок") || norm.contains("rock")) return "русский рок популярное хиты";
        if (norm.contains("фонк") || norm.contains("phonk")) return "phonk drift популярное";
        if (norm.contains("постпанк") || norm.contains("postpunk")) return "пост-панк популярное";
        if (norm.contains("хипхоп") || norm.contains("hiphop") || norm.contains("рэп") || norm.contains("rap")) return "хип хоп рэп хиты";
        if (norm.contains("поп") || norm.contains("pop")) return "поп музыка популярные хиты";
        if (norm.contains("инди") || norm.contains("indie")) return "инди рок хиты";
        if (norm.contains("гиперпоп") || norm.contains("hyperpop")) return "гиперпоп hyperpop";
        if (norm.contains("эморэп") || norm.contains("emorap")) return "эмо рэп";
        if (norm.contains("метал") || norm.contains("metal")) return "метал рок хиты";
        if (norm.contains("альтернатива") || norm.contains("alternative")) return "альтернативный рок";
        if (norm.contains("кпоп") || norm.contains("kpop")) return "k-pop popular hits";
        if (norm.contains("джаз") || norm.contains("jazz")) return "джаз популярное";
        if (norm.contains("блюз") || norm.contains("blues")) return "блюз популярное";
        if (norm.contains("классика") || norm.contains("classical") || norm.contains("classic")) return "классическая музыка шедевры";
        if (norm.contains("электроника") || norm.contains("electronic") || norm.contains("edm")) return "электронная музыка edm";
        if (norm.contains("синтвейв") || norm.contains("synthwave") || norm.contains("retrowave")) return "synthwave retrowave";
        if (norm.contains("лофай") || norm.contains("lofi")) return "lofi hip hop chill";
        if (norm.contains("эмбиент") || norm.contains("ambient")) return "ambient music";
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
    public static List<String> selectSeedsByGenre(String genre) {
        return selectSeedsByGenre(genre, null);
    }

    public static List<String> selectSeedsByGenre(String genre, AtomicBoolean cancelFlag) {
        if (genre == null || genre.trim().isEmpty() || genre.equalsIgnoreCase("Любой жанр") || genre.equalsIgnoreCase("Любой")) {
            genre = "default";
        }

        // 1. Попытка получить свежие треки по жанру через InnerTube
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

        // 2. Фолбэк на статический пул маяков при отсутствии сети
        String key = genre.toLowerCase().trim();
        String normalizedKey = key.replace("-", "").replace(" ", "");

        List<String> pool = GENRE_BEACONS.get(key);
        if (pool == null || pool.isEmpty()) {
            pool = GENRE_BEACONS.get(normalizedKey);
        }
        if (pool == null || pool.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : GENRE_BEACONS.entrySet()) {
                String entryNorm = entry.getKey().replace("-", "").replace(" ", "");
                if (normalizedKey.contains(entryNorm) || entryNorm.contains(normalizedKey)) {
                    pool = entry.getValue();
                    break;
                }
            }
        }
        if (pool == null || pool.isEmpty()) {
            pool = GENRE_BEACONS.get("default");
        }

        List<String> copy = new ArrayList<>(pool);
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
            // Фолбэк на дефолтные сиды жанра
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
     * 1) Если переданы треки альбома, берем 2-3 трека и находим их videoId.
     * 2) Если треков нет, ищем по запросу (artist + " " + album).
     * 3) Каскадный фолбэк по артисту, альбому или дефолтным сидам (никогда не возвращает пустой список).
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

        // 2. Если результатов мало и есть реальные треки альбома — ищем по названию первых треков
        if (albumTracks != null && !albumTracks.isEmpty()) {
            List<MessageObject> candidates = new ArrayList<>(albumTracks);
            Collections.shuffle(candidates, sRandom);
            for (MessageObject msg : candidates) {
                if (cancelFlag != null && cancelFlag.get()) break;
                String author = msg.getMusicAuthor();
                String title = msg.getMusicTitle();
                if (title == null || title.trim().isEmpty()) continue;
                String q = (author != null && !author.trim().isEmpty() ? author.trim() + " - " : "") + title.trim();
                List<InnerTubeClient.RawTrack> found = InnerTubeClient.searchSongs(q, 2, cancelFlag);
                for (InnerTubeClient.RawTrack t : found) {
                    if (t.videoId != null && !t.videoId.isEmpty() && seenIds.add(t.videoId)) {
                        result.add(t.videoId);
                        if (result.size() >= 3) return result;
                    }
                }
                if (result.size() >= 2) return result;
            }
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
     * выбор 3 треков строго от разных артистов (или 3 любых если артист один).
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

        // Поиск videoId для каждого выбранного трека
        for (MessageObject msg : chosenMessages) {
            String author = msg.getMusicAuthor();
            String title = msg.getMusicTitle();
            String q = (author != null && !author.trim().isEmpty() ? author.trim() + " - " : "") + (title != null ? title.trim() : "");
            if (q.trim().isEmpty()) continue;
            List<InnerTubeClient.RawTrack> found = InnerTubeClient.searchSongs(q, 1, cancelFlag);
            if (!found.isEmpty() && found.get(0).videoId != null && !found.get(0).videoId.isEmpty()) {
                result.add(found.get(0).videoId);
            }
        }

        if (result.isEmpty()) {
            return selectSeedsByGenre("default");
        }
        return result;
    }
}
