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

        // Инди / Альтернатива
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

        // Джаз / Блюз
        GENRE_BEACONS.put("джаз", Arrays.asList(
                "vmDDOFXSg2k", "PoPL7BExSQU", "fN6GZZm7_cQ", "zqNTltOGh5c", "8B1oPE8_mw8",
                "1cRNEv59_m8", "U47f5pX8k0E", "9pM2vP4z7b0", "3tK6wY1x8cE", "5rN4bC9y2p0"
        ));
        GENRE_BEACONS.put("jazz", GENRE_BEACONS.get("джаз"));

        // Метал
        GENRE_BEACONS.put("метал", Arrays.asList(
                "CD-E-LDc384", "AkF9TPy4gE0", "CSvFpBOe8eY", "Nba3Tr_KhZU", "Wws7acXdUCQ",
                "kJQP7kiw5Fk", "7lM1vB4y9p0", "5rN4wX8c1k0", "3tK6oY2x5cA", "8vJ3cO1k9bQ"
        ));
        GENRE_BEACONS.put("metal", GENRE_BEACONS.get("метал"));

        // Дефолтный пул (микс популярных треков)
        GENRE_BEACONS.put("default", Arrays.asList(
                "nidQCt_85HC", "mO8i_Y3_8Xg", "Q5Z_h3EaL40", "y6F3p4Wv_vI", "V6rX2Pq0hVE",
                "T1q70w_q63A", "YkgkTh308XU", "kJQP7kiw5Fk", "0-7IHOXkiV8", "_Yhyp-_hX2s",
                "tvTRZJ-4EyI", "kXYiU_JCYtU", "ALZHF5UqnU4", "m7Bc3JBkW4s", "Wha_bC9Z2pU"
        ));
    }

    private SeedSelector() {}

    /**
     * Выбор сидов по жанру (3 случайных трека-маяка из справочника).
     * Работает синхронно и МГНОВЕННО (0 мс).
     */
    public static List<String> selectSeedsByGenre(String genre) {
        String key = genre != null ? genre.toLowerCase().trim() : "default";
        List<String> pool = GENRE_BEACONS.get(key);
        if (pool == null || pool.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : GENRE_BEACONS.entrySet()) {
                if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
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

        // 1. Если переданы реальные треки альбома
        if (albumTracks != null && !albumTracks.isEmpty()) {
            List<MessageObject> candidates = new ArrayList<>(albumTracks);
            Collections.shuffle(candidates, sRandom);
            for (MessageObject msg : candidates) {
                if (cancelFlag != null && cancelFlag.get()) break;
                String author = msg.getMusicAuthor();
                String title = msg.getMusicTitle();
                if (title == null || title.trim().isEmpty()) continue;
                String q = (author != null && !author.trim().isEmpty() ? author.trim() + " - " : "") + title.trim();
                List<InnerTubeClient.RawTrack> found = InnerTubeClient.searchSongs(q, 1, cancelFlag);
                if (!found.isEmpty() && found.get(0).videoId != null && !found.get(0).videoId.isEmpty()) {
                    if (seenIds.add(found.get(0).videoId)) {
                        result.add(found.get(0).videoId);
                        if (result.size() >= 2) return result;
                    }
                }
            }
        }

        // 2. Поиск по исполнителю + названию альбома
        String cleanArtist = artistName != null ? artistName.trim() : "";
        String cleanAlbum = albumTitle != null ? albumTitle.trim() : "";
        String query = (cleanArtist + " " + cleanAlbum).trim();
        if (!query.isEmpty()) {
            List<InnerTubeClient.RawTrack> songs = InnerTubeClient.searchSongs(query, 10, cancelFlag);
            for (InnerTubeClient.RawTrack t : songs) {
                if (t.videoId != null && !t.videoId.isEmpty() && seenIds.add(t.videoId)) {
                    result.add(t.videoId);
                    if (result.size() >= 2) return result;
                }
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
