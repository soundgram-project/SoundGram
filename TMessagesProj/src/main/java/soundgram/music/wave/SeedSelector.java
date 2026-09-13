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
        GENRE_BEACONS.put("поп", Arrays.asList("-V_mLCl0Zws", "yYO-g0ArEHo", "OsfAnsMY21M"));
        GENRE_BEACONS.put("pop", GENRE_BEACONS.get("поп"));

        // Хип-хоп / Рэп (RU + топ)
        GENRE_BEACONS.put("хип-хоп", Arrays.asList("5-MahtpxqBQ", "tSLQTfsmrkY", "tqxRidAWER8"));
        GENRE_BEACONS.put("хип хоп", GENRE_BEACONS.get("хип-хоп"));
        GENRE_BEACONS.put("рэп", GENRE_BEACONS.get("хип-хоп"));
        GENRE_BEACONS.put("hip-hop", GENRE_BEACONS.get("хип-хоп"));
        GENRE_BEACONS.put("rap", GENRE_BEACONS.get("хип-хоп"));

        // Рок (RU + легенды)
        GENRE_BEACONS.put("рок", Arrays.asList("yUp01GbQxTw", "ihOxsYme6pk", "5qZQEq_C3vc"));
        GENRE_BEACONS.put("rock", GENRE_BEACONS.get("рок"));

        // Электроника / Клубная
        GENRE_BEACONS.put("электроника", Arrays.asList("LhOjN4t0YsY", "2NiyrtYegso", "LRPGqNeav_M"));
        GENRE_BEACONS.put("electronic", GENRE_BEACONS.get("электроника"));
        GENRE_BEACONS.put("edm", GENRE_BEACONS.get("электроника"));

        // Фонк (Phonk / Drift Phonk)
        GENRE_BEACONS.put("фонк", Arrays.asList("jIYdY_qDeuQ", "fzeoo8n8RZo", "cZfb2HU7c9o"));
        GENRE_BEACONS.put("phonk", GENRE_BEACONS.get("фонк"));

        // Гиперпоп
        GENRE_BEACONS.put("гиперпоп", Arrays.asList("H8MbDwkV9YM", "19fkWJ--GQo", "kpPt8NpYtJg"));
        GENRE_BEACONS.put("hyperpop", GENRE_BEACONS.get("гиперпоп"));

        // Пост-панк
        GENRE_BEACONS.put("пост-панк", Arrays.asList("s1ATTIQrmIQ", "n3EvWofNgIw", "1M_k7b1cAxM"));
        GENRE_BEACONS.put("пост панк", GENRE_BEACONS.get("пост-панк"));
        GENRE_BEACONS.put("постпанк", GENRE_BEACONS.get("пост-панк"));
        GENRE_BEACONS.put("post-punk", GENRE_BEACONS.get("пост-панк"));
        GENRE_BEACONS.put("postpunk", GENRE_BEACONS.get("пост-панк"));

        // Эмо-рэп
        GENRE_BEACONS.put("эмо-рэп", Arrays.asList("YgTEUXhHyMU", "hHtv2XMZlKs", "qxtOBMQxsHc"));
        GENRE_BEACONS.put("эмо рэп", GENRE_BEACONS.get("эмо-рэп"));
        GENRE_BEACONS.put("эморэп", GENRE_BEACONS.get("эмо-рэп"));
        GENRE_BEACONS.put("emo-rap", GENRE_BEACONS.get("эмо-рэп"));
        GENRE_BEACONS.put("emorap", GENRE_BEACONS.get("эмо-рэп"));

        // Металл / Метал
        GENRE_BEACONS.put("металл", Arrays.asList("CHIWNDAwTqQ", "kiw_VxQ2oXo", "rAjwN7ryg94"));
        GENRE_BEACONS.put("метал", GENRE_BEACONS.get("металл"));
        GENRE_BEACONS.put("metal", GENRE_BEACONS.get("металл"));

        // Альтернатива
        GENRE_BEACONS.put("альтернатива", Arrays.asList("IeYxMTVL74Y", "0-q83p_o-Lg", "ljUtuoFt-8c"));
        GENRE_BEACONS.put("alternative", GENRE_BEACONS.get("альтернатива"));

        // К-поп
        GENRE_BEACONS.put("к-поп", Arrays.asList("HaEYUJ2aRHs", "8mA6jIeojnk", "GyCAzYm1zvk"));
        GENRE_BEACONS.put("к поп", GENRE_BEACONS.get("к-поп"));
        GENRE_BEACONS.put("кпоп", GENRE_BEACONS.get("к-поп"));
        GENRE_BEACONS.put("k-pop", GENRE_BEACONS.get("к-поп"));
        GENRE_BEACONS.put("kpop", GENRE_BEACONS.get("к-поп"));

        // Инди
        GENRE_BEACONS.put("инди", Arrays.asList("haRv92SNt-E", "agS8ysvGjlY", "cULQhvuq1Zc"));
        GENRE_BEACONS.put("indie", GENRE_BEACONS.get("инди"));

        // R&B / Soul
        GENRE_BEACONS.put("r&b", Arrays.asList("f1r0250KKAI", "450p7goxZqg", "u9n7Cw-4_PE"));
        GENRE_BEACONS.put("rnb", GENRE_BEACONS.get("r&b"));
        GENRE_BEACONS.put("соул", GENRE_BEACONS.get("r&b"));
        GENRE_BEACONS.put("soul", GENRE_BEACONS.get("r&b"));

        // Джаз
        GENRE_BEACONS.put("джаз", Arrays.asList("fnyMzZiKfkY", "ZEcqHA7dbwM", "KJEzFvXx3Xw"));
        GENRE_BEACONS.put("jazz", GENRE_BEACONS.get("джаз"));

        // Блюз
        GENRE_BEACONS.put("блюз", Arrays.asList("JmsUkutNnI0", "_9IIDE_45Pc", "ihOxsYme6pk"));
        GENRE_BEACONS.put("blues", GENRE_BEACONS.get("блюз"));

        // Классика
        GENRE_BEACONS.put("классика", Arrays.asList("NbEfWg8VBho", "_FeJFs6LJjU", "pBGVfwOLU1w"));
        GENRE_BEACONS.put("классическая", GENRE_BEACONS.get("классика"));
        GENRE_BEACONS.put("classical", GENRE_BEACONS.get("классика"));
        GENRE_BEACONS.put("classic", GENRE_BEACONS.get("классика"));

        // Синтвейв
        GENRE_BEACONS.put("синтвейв", Arrays.asList("LfgNorryffc", "Qzp6Xigrts0", "YWIviF4rYcc"));
        GENRE_BEACONS.put("синт-вейв", GENRE_BEACONS.get("синтвейв"));
        GENRE_BEACONS.put("синт вейв", GENRE_BEACONS.get("синтвейв"));
        GENRE_BEACONS.put("synthwave", GENRE_BEACONS.get("синтвейв"));
        GENRE_BEACONS.put("retrowave", GENRE_BEACONS.get("синтвейв"));

        // Ло-фай
        GENRE_BEACONS.put("ло-фай", Arrays.asList("jgJWZDFoD1k", "XWLQKNvCepM", "xhzi2n-aZ58"));
        GENRE_BEACONS.put("лофай", GENRE_BEACONS.get("ло-фай"));
        GENRE_BEACONS.put("ло фай", GENRE_BEACONS.get("ло-фай"));
        GENRE_BEACONS.put("lo-fi", GENRE_BEACONS.get("ло-фай"));
        GENRE_BEACONS.put("lofi", GENRE_BEACONS.get("ло-фай"));

        // Эмбиент
        GENRE_BEACONS.put("эмбиент", Arrays.asList("akzO4lxACNY", "75O11W5EZAU", "LA0QB8HXJSw"));
        GENRE_BEACONS.put("ambient", GENRE_BEACONS.get("эмбиент"));

        // Дефолтный пул (микс популярных хитов)
        GENRE_BEACONS.put("default", Arrays.asList("-V_mLCl0Zws", "5-MahtpxqBQ", "yUp01GbQxTw", "LhOjN4t0YsY", "jIYdY_qDeuQ", "HaEYUJ2aRHs", "s1ATTIQrmIQ"));
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
