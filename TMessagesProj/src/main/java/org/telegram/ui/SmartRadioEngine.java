package org.telegram.ui;

import android.content.Context;
import android.media.MediaExtractor;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

public class SmartRadioEngine {

    public interface WaveCallback {
        void onTracksFound(ArrayList<MessageObject> tracks);
        void onMoreTracksAdded(ArrayList<MessageObject> tracks);
        void onError();
    }

    public static class WaveConfig {
        public String query = "Любой жанр";
    }

    public static WaveConfig currentConfig = new WaveConfig();
    public static boolean isWaveActive = false;
    public static boolean isLoading = false;

    private static int currentWaveMode = 0;
    private static ArrayList<MessageObject> basePlaylistContext = new ArrayList<>();
    private static final HashSet<String> globalPlayHistory = new HashSet<>();
    private static final Random random = new Random();
    private static final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(5);

    private static final HashMap<String, float[]> vectorCache = new HashMap<>();
    private static NeuroRanker neuroRanker;
    private static final float SIMILARITY_THRESHOLD = 0.55f;
    private static final int BATCH_SIZE = 5;
    private static ArrayList<MessageObject> lastGeneratedBatch = new ArrayList<>();

    private static void initNeuroRankerIfNeeded() {
        if (neuroRanker == null) neuroRanker = new NeuroRanker(ApplicationLoader.applicationContext);
    }

    private static String getDeezerSearchQuery(String genre) {
        switch(genre.toLowerCase()) {
            case "поп": return "Pop";
            case "хип-хоп": return "Hip Hop rap";
            case "рок": return "Rock";
            case "инди": return "Indie";
            case "фонк": return "Phonk";
            case "гиперпоп": return "Hyperpop";
            case "пост-панк": return "Post punk";
            case "эмо-рэп":
            case "эмо реп": return "Emo rap";
            case "метал": return "Metal";
            case "альтернатива": return "Alternative rock";
            case "к-поп": return "K-pop";
            case "джаз": return "Jazz";
            case "блюз": return "Blues";
            case "классика": return "Classical";
            case "электроника": return "Electronic";
            case "дабстеп": return "Dubstep";
            case "панк-рок": return "Punk rock";
            case "синтвейв": return "Synthwave";
            case "ло-фай": return "Lofi hiphop";
            case "эмбиент": return "Ambient";
            default: return genre.equals("Любой жанр") ? "Top Hits" : genre;
        }
    }

    public static void generateWaveFromSettings(int currentAccount, WaveCallback callback) {
        startNewWaveSession(0);
        backgroundExecutor.execute(() -> {
            try {
                String searchKeyword = getDeezerSearchQuery(currentConfig.query);

                // 1. Первая пачка: мгновенный старт без нейронки (5 треков)
                ArrayList<String> initialCandidates = getTracksFromDynamicPlaylists(searchKeyword, 5, random.nextInt(100));
                processAndReturnTracksOneByOne(initialCandidates, currentAccount, true, callback);

                // 2. Вторая пачка: умный подбор через нейронку в фоне
                float[] contextVector = neuroRanker.getGenreVector(currentConfig.query);
                ArrayList<String> nextCandidates = getTracksFromDynamicPlaylists(searchKeyword, 5, random.nextInt(100) + 5);
                ArrayList<String> verified = verifyCandidatesWithNeuralNetwork(nextCandidates, contextVector, 20);
                processAndReturnTracksOneByOne(verified, currentAccount, false, callback);
            } catch (Exception e) { handleError(callback, e); }
        });
    }

    public static void generateWaveByPlaylist(ArrayList<MessageObject> originalPlaylist, int currentAccount, WaveCallback callback) {
        startNewWaveSession(1);
        if (originalPlaylist != null) basePlaylistContext.addAll(originalPlaylist);
        backgroundExecutor.execute(() -> {
            try {
                if (basePlaylistContext.isEmpty()) throw new Exception("Плейлист пуст");
                MessageObject seedTrack = basePlaylistContext.get(random.nextInt(basePlaylistContext.size()));

                // 1. Первая пачка: мгновенный старт
                ArrayList<String> initialCandidates = getDeepSimilarTracks(seedTrack.getMusicAuthor(), 5);
                if(initialCandidates.isEmpty()) initialCandidates = getTracksFromDynamicPlaylists("Top Hits", 5, 0);
                processAndReturnTracksOneByOne(initialCandidates, currentAccount, true, callback);

                // 2. Вторая пачка: нейронка (в фоне)
                float[] seedVector = neuroRanker.getVectorForRemoteTrack(seedTrack.getMusicAuthor(), seedTrack.getMusicTitle());
                if (seedVector == null) seedVector = neuroRanker.getGenreVector("Поп");
                ArrayList<String> nextCandidates = getDeepSimilarTracks(seedTrack.getMusicAuthor(), 30);
                if(nextCandidates.isEmpty()) nextCandidates = getTracksFromDynamicPlaylists("Top Hits", 30, 0);
                ArrayList<String> verified = verifyCandidatesWithNeuralNetwork(nextCandidates, seedVector, 20);
                processAndReturnTracksOneByOne(verified, currentAccount, false, callback);
            } catch (Exception e) { handleError(callback, e); }
        });
    }

    private static ArrayList<String> verifyCandidatesWithNeuralNetwork(ArrayList<String> candidates, float[] targetVector, int requiredCount) {
        ArrayList<String> verified = new ArrayList<>();
        ArrayList<String> fallback = new ArrayList<>();
        Collections.shuffle(candidates);

        for (String candidate : candidates) {
            if (verified.size() >= requiredCount) break;
            String[] parts = candidate.split(" - ", 2);
            String art = parts[0];
            String tit = parts.length > 1 ? parts[1] : "";

            float[] candVector = neuroRanker.getVectorForRemoteTrack(art, tit);
            if (candVector != null) {
                float similarity = neuroRanker.calculateCosineSimilarity(targetVector, candVector);
                if (similarity >= SIMILARITY_THRESHOLD) {
                    verified.add(candidate);
                } else {
                    fallback.add(candidate);
                }
            } else {
                fallback.add(candidate);
            }
        }
        while (verified.size() < requiredCount && !fallback.isEmpty()) {
            verified.add(fallback.remove(0));
        }
        if (verified.isEmpty() && !candidates.isEmpty()) {
            verified.addAll(candidates.subList(0, Math.min(requiredCount, candidates.size())));
        }
        return verified;
    }

    public static void startNewWaveSession(int mode) {
        isWaveActive = false;
        isLoading = false;

        currentWaveMode = mode;
        basePlaylistContext.clear();
        lastGeneratedBatch.clear();

        isWaveActive = true;
    }

    private static void handleError(WaveCallback callback, Exception e) {
        AndroidUtilities.runOnUIThread(() -> { isLoading = false; callback.onError(); });
    }

    private static void processAndReturnTracksOneByOne(ArrayList<String> queries, int account, boolean isInitial, WaveCallback callback) {
        if (queries.isEmpty()) {
            return;
        }
        final boolean[] initialFired = {false};
        if (isInitial) lastGeneratedBatch.clear();

        for (String q : queries) {
            backgroundExecutor.execute(() -> {
                MusicWebSearcher.search(q, (tracks, artists, albums) -> {
                    if (tracks != null && !tracks.isEmpty()) {
                        MusicWebSearcher.WebTrack wt = tracks.get(0);
                        int dur = ChannelMusicFragment.parseDuration(wt.duration);
                        MessageObject msg = ChannelMusicFragment.createWebMessageObject(account, wt.title, wt.artist, wt.downloadUrl, dur, -(long)wt.downloadUrl.hashCode(), wt.coverUrl);

                        AndroidUtilities.runOnUIThread(() -> {
                            synchronized (lastGeneratedBatch) { lastGeneratedBatch.add(msg); }
                            ArrayList<MessageObject> single = new ArrayList<>();
                            single.add(msg);
                            if (isInitial && !initialFired[0]) {
                                initialFired[0] = true;
                                callback.onTracksFound(single);
                            } else {
                                callback.onMoreTracksAdded(single);
                            }
                        });
                    }
                });
            });
        }
        AndroidUtilities.runOnUIThread(() -> isLoading = false, 2000);
    }

    private static ArrayList<String> getTracksFromDynamicPlaylists(String query, int target, int offset) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        String url = "https://api.deezer.com/search/playlist?q=" + URLEncoder.encode(query, "UTF-8") + "&limit=5&index=" + offset;
        JSONObject json = new JSONObject(performGetRequest(url));
        if (!json.has("data")) return res;
        JSONArray playlists = json.getJSONArray("data");
        for (int i = 0; i < playlists.length(); i++) {
            String tUrl = "https://api.deezer.com/playlist/" + playlists.getJSONObject(i).getLong("id") + "/tracks?limit=15";
            JSONArray tracks = new JSONObject(performGetRequest(tUrl)).getJSONArray("data");
            for (int j = 0; j < tracks.length(); j++) {
                JSONObject t = tracks.getJSONObject(j);
                String art = t.getJSONObject("artist").getString("name");
                String tit = t.getString("title");
                if (globalPlayHistory.add(art + tit)) res.add(art + " - " + tit);
                if (res.size() >= target) return res;
            }
        }
        return res;
    }

    private static ArrayList<String> getDeepSimilarTracks(String artist, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        String url = "https://api.deezer.com/search/artist?q=" + URLEncoder.encode(artist, "UTF-8");
        String resp = performGetRequest(url);
        if(resp.isEmpty()) return res;
        JSONArray data = new JSONObject(resp).optJSONArray("data");
        if (data == null || data.length() == 0) return res;
        long id = data.getJSONObject(0).getLong("id");

        // 1. Радио артиста
        JSONArray radio = new JSONObject(performGetRequest("https://api.deezer.com/artist/" + id + "/radio")).optJSONArray("data");
        if (radio != null) {
            for (int i = 0; i < radio.length(); i++) {
                JSONObject t = radio.getJSONObject(i);
                String a = t.getJSONObject("artist").getString("name");
                String tit = t.getString("title");
                if (globalPlayHistory.add(a + tit)) res.add(a + " - " + tit);
                if (res.size() >= limit) return res;
            }
        }

        // 2. Похожие артисты и их топ-треки
        JSONArray related = new JSONObject(performGetRequest("https://api.deezer.com/artist/" + id + "/related?limit=5")).optJSONArray("data");
        if (related != null) {
            for (int i = 0; i < related.length(); i++) {
                long relId = related.getJSONObject(i).getLong("id");
                JSONArray top = new JSONObject(performGetRequest("https://api.deezer.com/artist/" + relId + "/top?limit=10")).optJSONArray("data");
                if (top != null) {
                    for (int j = 0; j < top.length(); j++) {
                        JSONObject t = top.getJSONObject(j);
                        String a = t.getJSONObject("artist").getString("name");
                        String tit = t.getString("title");
                        if (globalPlayHistory.add(a + tit)) res.add(a + " - " + tit);
                        if (res.size() >= limit) return res;
                    }
                }
            }
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // НОВЫЙ метод: получаем треки конкретного альбома с Deezer (как "сид" для волны)
    // ─────────────────────────────────────────────────────────────────────────
    private static ArrayList<String> getTracksFromAlbum(String artistName, String albumName, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        // Ищем альбом по артисту + названию
        String query = URLEncoder.encode(artistName + " " + albumName, "UTF-8");
        String resp = performGetRequest("https://api.deezer.com/search/album?q=" + query + "&limit=5");
        if (resp.isEmpty()) return res;
        JSONObject json = new JSONObject(resp);
        if (!json.has("data")) return res;
        JSONArray albums = json.getJSONArray("data");
        if (albums.length() == 0) return res;

        long albumId = albums.getJSONObject(0).getLong("id");

        // Получаем все треки альбома
        String tracksResp = performGetRequest("https://api.deezer.com/album/" + albumId + "/tracks?limit=50");
        JSONArray tracks = new JSONObject(tracksResp).optJSONArray("data");
        if (tracks == null) return res;

        for (int i = 0; i < tracks.length(); i++) {
            JSONObject t = tracks.getJSONObject(i);
            String art = t.has("artist") ? t.getJSONObject("artist").getString("name") : artistName;
            String tit = t.getString("title");
            if (globalPlayHistory.add(art + tit)) res.add(art + " - " + tit);
            if (res.size() >= limit) return res;
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // НОВЫЙ метод: получаем жанры альбома с Deezer для более точного поиска
    // ─────────────────────────────────────────────────────────────────────────
    private static String getAlbumGenreQuery(String artistName, String albumName) throws Exception {
        String query = URLEncoder.encode(artistName + " " + albumName, "UTF-8");
        String resp = performGetRequest("https://api.deezer.com/search/album?q=" + query + "&limit=3");
        if (resp.isEmpty()) return null;
        JSONObject json = new JSONObject(resp);
        if (!json.has("data") || json.getJSONArray("data").length() == 0) return null;

        long albumId = json.getJSONArray("data").getJSONObject(0).getLong("id");

        // Детали альбома содержат genres
        String detailResp = performGetRequest("https://api.deezer.com/album/" + albumId);
        if (detailResp.isEmpty()) return null;
        JSONObject detail = new JSONObject(detailResp);

        // Пробуем взять жанр
        if (detail.has("genres")) {
            JSONObject genresObj = detail.getJSONObject("genres");
            if (genresObj.has("data") && genresObj.getJSONArray("data").length() > 0) {
                return genresObj.getJSONArray("data").getJSONObject(0).getString("name");
            }
        }
        return null;
    }

    public static void loadMoreForCurrentWave(MessageObject currentTrack, int currentAccount, WaveCallback callback) {
        if (isLoading || !isWaveActive) return;
        isLoading = true;
        initNeuroRankerIfNeeded();

        backgroundExecutor.execute(() -> {
            try {
                MessageObject seed = lastGeneratedBatch.isEmpty() ? currentTrack : lastGeneratedBatch.get(random.nextInt(lastGeneratedBatch.size()));
                float[] seedVector = neuroRanker.getVectorForRemoteTrack(seed.getMusicAuthor(), seed.getMusicTitle());
                if (seedVector == null) seedVector = neuroRanker.getGenreVector(currentConfig.query);

                ArrayList<String> candidates = new ArrayList<>();
                String searchQuery = currentConfig.query;
                if (currentWaveMode == 0 && random.nextInt(100) < 50) {
                    searchQuery = "russian " + searchQuery;
                }

                if (currentWaveMode == 0) {
                    candidates.addAll(getTracksFromDynamicPlaylists(getDeezerSearchQuery(searchQuery), 30, random.nextInt(300)));
                } else {
                    candidates.addAll(getDeepSimilarTracks(seed.getMusicAuthor(), 30));
                    if (candidates.size() < 10) {
                        candidates.addAll(getTracksFromDynamicPlaylists(getDeezerSearchQuery(searchQuery), 20, random.nextInt(100)));
                    }
                }

                if (candidates.isEmpty()) {
                    candidates.addAll(getTracksFromDynamicPlaylists("Top Hits", 10, random.nextInt(500)));
                }

                ArrayList<String> verified = verifyCandidatesWithNeuralNetwork(candidates, seedVector, 20);
                processAndReturnTracksOneByOne(verified, currentAccount, false, callback);
            } catch (Exception e) { handleError(callback, e); }
        });
    }

    public static void generateWaveByArtist(String artistName, int currentAccount, WaveCallback callback) {
        startNewWaveSession(2);
        currentConfig.query = artistName;
        initNeuroRankerIfNeeded();

        backgroundExecutor.execute(() -> {
            try {
                // 1. Быстрый старт — радио и похожие
                ArrayList<String> initialCandidates = getDeepSimilarTracks(artistName, 8);
                if (initialCandidates.isEmpty())
                    initialCandidates = getTracksFromDynamicPlaylists(getDeezerSearchQuery(artistName), 8, 0);
                if (initialCandidates.isEmpty())
                    initialCandidates = getTracksFromDynamicPlaylists("Top Hits", 8, 0);

                processAndReturnTracksOneByOne(initialCandidates, currentAccount, true, callback);

                // 2. Нейросеть-порция
                float[] seedVector = neuroRanker.getVectorForRemoteTrack(artistName, "");
                if (seedVector == null) seedVector = neuroRanker.getGenreVector("Инди");

                ArrayList<String> nextCandidates = getDeepSimilarTracks(artistName, 40);
                if (nextCandidates.isEmpty())
                    nextCandidates = getTracksFromDynamicPlaylists(getDeezerSearchQuery(artistName), 40, 0);
                if (nextCandidates.isEmpty())
                    nextCandidates = getTracksFromDynamicPlaylists("Top Hits", 40, random.nextInt(200));

                ArrayList<String> verified = verifyCandidatesWithNeuralNetwork(nextCandidates, seedVector, 25);
                processAndReturnTracksOneByOne(verified, currentAccount, false, callback);

                // 3. Пополнение — чтобы волна не умирала
                Thread.sleep(3000);
                if (isWaveActive) {
                    ArrayList<String> moreCandidates = getDeepSimilarTracks(artistName, 30);
                    if (moreCandidates.isEmpty())
                        moreCandidates = getTracksFromDynamicPlaylists("Top Hits", 30, random.nextInt(500));
                    ArrayList<String> moreVerified = verifyCandidatesWithNeuralNetwork(
                            moreCandidates, seedVector, 20);
                    processAndReturnTracksOneByOne(moreVerified, currentAccount, false, callback);
                }
            } catch (Exception e) { handleError(callback, e); }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ПЕРЕПИСАННЫЙ generateWaveByAlbum — логика как у generateWaveByArtist,
    // но использует треки альбома + жанры альбома для поиска похожего контента
    // ─────────────────────────────────────────────────────────────────────────
    public static void generateWaveByAlbum(String artistName, String albumName,
                                           int currentAccount, WaveCallback callback) {
        startNewWaveSession(3);
        initNeuroRankerIfNeeded();

        backgroundExecutor.execute(() -> {
            try {
                String safeArtist = (artistName == null || artistName.isEmpty()) ? "Unknown Artist" : artistName;
                String safeAlbum  = (albumName  == null || albumName.isEmpty())  ? ""              : albumName;
                currentConfig.query = safeArtist;

                // ── ШАГ 1: Быстрый старт ─────────────────────────────────────
                // Приоритет: треки самого альбома → радио артиста → похожие → топ хиты
                ArrayList<String> initialCandidates = new ArrayList<>();

                // a) Реальные треки альбома (самый релевантный контент)
                if (!safeAlbum.isEmpty()) {
                    initialCandidates.addAll(getTracksFromAlbum(safeArtist, safeAlbum, 8));
                }

                // b) Если треков альбома не хватает — добираем радио артиста
                if (initialCandidates.size() < 5) {
                    ArrayList<String> artistTracks = getDeepSimilarTracks(safeArtist, 8 - initialCandidates.size());
                    initialCandidates.addAll(artistTracks);
                }

                // c) Финальный фоллбэк
                if (initialCandidates.isEmpty()) {
                    initialCandidates = getTracksFromDynamicPlaylists(getDeezerSearchQuery(safeArtist), 8, 0);
                }
                if (initialCandidates.isEmpty()) {
                    initialCandidates = getTracksFromDynamicPlaylists("Top Hits", 8, 0);
                }

                processAndReturnTracksOneByOne(initialCandidates, currentAccount, true, callback);

                // ── ШАГ 2: Нейросеть-порция (по жанру альбома + треку-сиду) ─
                // Пробуем получить жанр альбома для более точного вектора
                String albumGenre = null;
                try {
                    if (!safeAlbum.isEmpty()) {
                        albumGenre = getAlbumGenreQuery(safeArtist, safeAlbum);
                    }
                } catch (Exception ignored) {}

                // Строим seed-вектор: сначала пробуем трек из альбома, потом артиста, потом жанр
                float[] seedVector = null;

                // Берём первый трек альбома как наиболее репрезентативный
                ArrayList<String> albumTracksForSeed = new ArrayList<>();
                if (!safeAlbum.isEmpty()) {
                    try {
                        albumTracksForSeed = getTracksFromAlbum(safeArtist, safeAlbum, 3);
                    } catch (Exception ignored) {}
                }

                if (!albumTracksForSeed.isEmpty()) {
                    String[] parts = albumTracksForSeed.get(0).split(" - ", 2);
                    String seedArt = parts[0];
                    String seedTit = parts.length > 1 ? parts[1] : "";
                    seedVector = neuroRanker.getVectorForRemoteTrack(seedArt, seedTit);
                }

                if (seedVector == null) {
                    seedVector = neuroRanker.getVectorForRemoteTrack(safeArtist, safeAlbum);
                }
                if (seedVector == null && albumGenre != null) {
                    seedVector = neuroRanker.getGenreVector(albumGenre);
                }
                if (seedVector == null) {
                    seedVector = neuroRanker.getGenreVector("Инди");
                }

                // Кандидаты: радио артиста + треки по жанру альбома
                ArrayList<String> nextCandidates = new ArrayList<>();

                // Радио артиста — самый релевантный источник
                nextCandidates.addAll(getDeepSimilarTracks(safeArtist, 25));

                // Плейлисты по жанру альбома (если знаем жанр)
                if (albumGenre != null && nextCandidates.size() < 40) {
                    nextCandidates.addAll(getTracksFromDynamicPlaylists(albumGenre, 20, random.nextInt(50)));
                }

                // Плейлисты по артисту
                if (nextCandidates.size() < 30) {
                    nextCandidates.addAll(getTracksFromDynamicPlaylists(getDeezerSearchQuery(safeArtist), 15, random.nextInt(30)));
                }

                // Фоллбэк
                if (nextCandidates.isEmpty()) {
                    nextCandidates = getTracksFromDynamicPlaylists("Top Hits", 40, random.nextInt(200));
                }

                ArrayList<String> verified = verifyCandidatesWithNeuralNetwork(nextCandidates, seedVector, 25);
                processAndReturnTracksOneByOne(verified, currentAccount, false, callback);

                // ── ШАГ 3: Бесконечное пополнение — волна не умирает ────────
                final float[] finalSeedVector = seedVector;
                int refillRound = 0;
                while (isWaveActive) {
                    Thread.sleep(4000);
                    if (!isWaveActive) break;

                    ArrayList<String> extra = new ArrayList<>();

                    // Чередуем: радио артиста и жанровые плейлисты
                    if (refillRound % 2 == 0) {
                        extra.addAll(getDeepSimilarTracks(safeArtist, 25));
                    }
                    if (albumGenre != null && extra.size() < 30) {
                        extra.addAll(getTracksFromDynamicPlaylists(albumGenre, 15, random.nextInt(300)));
                    }
                    if (extra.size() < 15) {
                        extra.addAll(getTracksFromDynamicPlaylists(getDeezerSearchQuery(safeArtist), 15, random.nextInt(200)));
                    }
                    if (extra.isEmpty()) {
                        extra = getTracksFromDynamicPlaylists("Top Hits", 30, random.nextInt(500));
                    }

                    ArrayList<String> extraVer = verifyCandidatesWithNeuralNetwork(extra, finalSeedVector, 20);
                    processAndReturnTracksOneByOne(extraVer, currentAccount, false, callback);
                    refillRound++;
                }
            } catch (Exception e) { handleError(callback, e); }
        });
    }

    private static String performGetRequest(String urlStr) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestProperty("User-Agent", "Mozilla/5.0");
        c.setConnectTimeout(5000); c.setReadTimeout(5000);
        try (InputStream is = c.getInputStream(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192]; int l;
            while ((l = is.read(b)) != -1) bos.write(b, 0, l);
            return bos.toString("UTF-8");
        } catch(Exception e) { return ""; }
    }

    private static class NeuroRanker {
        private OrtEnvironment env;
        private OrtSession session;
        private android.database.sqlite.SQLiteDatabase genreDb;
        private String inputName;

        public NeuroRanker(Context context) {
            try {
                File dbFile = context.getDatabasePath("genre_anchors.db");
                if (!dbFile.exists()) {
                    dbFile.getParentFile().mkdirs();
                    try (InputStream is = context.getAssets().open("genre_anchors.db");
                         FileOutputStream fos = new FileOutputStream(dbFile)) {
                        byte[] buffer = new byte[4096]; int len;
                        while ((len = is.read(buffer)) > 0) fos.write(buffer, 0, len);
                    }
                }
                genreDb = android.database.sqlite.SQLiteDatabase.openDatabase(dbFile.getPath(), null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY);

                env = OrtEnvironment.getEnvironment();
                byte[] model;
                try (InputStream is = context.getAssets().open("music_brain.onnx"); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    byte[] b = new byte[4096]; int r;
                    while ((r = is.read(b)) != -1) bos.write(b, 0, r);
                    model = bos.toByteArray();
                }
                session = env.createSession(model, new OrtSession.SessionOptions());
                inputName = session.getInputNames().iterator().next();
            } catch (Exception e) { FileLog.e(e); }
        }

        public float[] getGenreVector(String genre) {
            float[] vec = new float[400];
            if (genreDb != null && genreDb.isOpen()) {
                try (android.database.Cursor c = genreDb.rawQuery("SELECT vector_json FROM anchors WHERE genre_name LIKE ?", new String[]{"%" + getDeezerSearchQuery(genre) + "%"})) {
                    if (c.moveToFirst()) {
                        JSONArray arr = new JSONArray(c.getString(0));
                        for (int i = 0; i < 400; i++) vec[i] = (float) arr.getDouble(i);
                        return vec;
                    }
                } catch (Exception ignored) {}
            }
            return vec;
        }

        public float[] getVectorForRemoteTrack(String artist, String title) {
            if (session == null) return null;
            String cacheKey = artist + " - " + title;
            if (vectorCache.containsKey(cacheKey)) return vectorCache.get(cacheKey);

            File tempFile = null;
            MediaExtractor extractor = new MediaExtractor();
            try {
                String q = URLEncoder.encode(artist + " " + title, "UTF-8");
                String json = performGetRequest("https://api.deezer.com/search?q=" + q + "&limit=1");
                if(json.isEmpty()) return null;
                JSONArray data = new JSONObject(json).getJSONArray("data");
                if (data.length() == 0) return null;
                String previewUrl = data.getJSONObject(0).getString("preview");

                tempFile = new File(ApplicationLoader.applicationContext.getCacheDir(), "preview_" + System.currentTimeMillis() + ".mp3");
                HttpURLConnection conn = (HttpURLConnection) new URL(previewUrl).openConnection();
                try (InputStream is = conn.getInputStream(); FileOutputStream fos = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192]; int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) fos.write(buffer, 0, bytesRead);
                }

                extractor.setDataSource(tempFile.getAbsolutePath());
                extractor.selectTrack(0);

                ByteBuffer buf = ByteBuffer.allocate(8192);
                ArrayList<Float> pcmData = new ArrayList<>();
                while (extractor.readSampleData(buf, 0) >= 0 && pcmData.size() < 16000 * 3) {
                    buf.rewind();
                    while (buf.hasRemaining()) pcmData.add(buf.getShort() / 32768.0f);
                    extractor.advance();
                }
                extractor.release();

                if (pcmData.isEmpty()) return null;

                int framesPerChunk = 128;
                int mels = 96;
                int samplesPerChunk = framesPerChunk * 256;
                ArrayList<float[]> chunkEmbeddings = new ArrayList<>();

                for (int offset = 0; offset + samplesPerChunk < pcmData.size(); offset += samplesPerChunk) {
                    FloatBuffer fb = FloatBuffer.allocate(framesPerChunk * mels);
                    for (int i = 0; i < framesPerChunk * mels; i++) fb.put(Math.abs(pcmData.get(offset + (i % samplesPerChunk))));
                    fb.rewind();
                    OnnxTensor tensor = OnnxTensor.createTensor(env, fb, new long[]{1, framesPerChunk, mels});
                    Map<String, OnnxTensor> inputs = new HashMap<>();
                    inputs.put(inputName, tensor);
                    float[][] out = (float[][]) session.run(inputs).get(0).getValue();
                    chunkEmbeddings.add(out[0]);
                }

                if (chunkEmbeddings.isEmpty()) return null;

                float[] finalVec = new float[400];
                for (float[] chunk : chunkEmbeddings) {
                    for (int i = 0; i < 400; i++) finalVec[i] += chunk[i];
                }

                float norm = 0;
                for (int i = 0; i < 400; i++) {
                    finalVec[i] /= chunkEmbeddings.size();
                    norm += finalVec[i] * finalVec[i];
                }
                norm = (float) Math.sqrt(norm);
                if (norm < 1e-9f) return null;
                for (int i = 0; i < 400; i++) finalVec[i] /= norm;

                vectorCache.put(cacheKey, finalVec);
                return finalVec;

            } catch (Exception e) {
                extractor.release();
                return null;
            } finally {
                if (tempFile != null && tempFile.exists()) tempFile.delete();
            }
        }

        public float calculateCosineSimilarity(float[] a, float[] b) {
            float dot = 0, nA = 0, nB = 0;
            for (int i = 0; i < 400; i++) {
                dot += a[i] * b[i]; nA += a[i] * a[i]; nB += b[i] * b[i];
            }
            if (nA == 0 || nB == 0) return 0;
            return (float) (dot / (Math.sqrt(nA) * Math.sqrt(nB)));
        }
    }
}
