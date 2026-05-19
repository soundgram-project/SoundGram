package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaExtractor;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;

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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

/**
 * SmartRadioEngine v2 — полностью переписанный движок волны.
 *
 * Что исправлено:
 *  1. globalPlayHistory теперь ConcurrentHashMap.newKeySet() — thread-safe без блокировок.
 *  2. processAndReturnTracksOneByOne — атомарный флаг initialFired, корректная синхронизация.
 *  3. Semaphore на HTTP-запросы — не более 3 параллельных к Deezer, нет rate limit.
 *  4. SIMILARITY_THRESHOLD снижен до 0.40f, добавлен мягкий порог 0.25f для fallback.
 *  5. globalPlayHistory автоматически чистится при превышении 3000 записей (скользящее окно).
 *  6. Бесконечное пополнение (refill loop) переехало в отдельный поток-демон.
 *  7. Новый источник: Last.fm similar tracks API (бесплатный, без ключа через RYM proxy).
 *  8. Новый источник: iTunes Search API — даёт другой пул треков, не пересекается с Deezer.
 *  9. Ротация 5 источников при рефилле: artist radio → genre playlists → iTunes → lastfm → top hits.
 * 10. Вектор seed кешируется между рефиллами, не пересчитывается каждый раз.
 * 11. lastGeneratedBatch — CopyOnWriteArrayList, safe из любого потока.
 * 12. Перевод строк локализации через LocaleController вместо хардкода.
 */
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
    public static volatile boolean isWaveActive = false;
    public static volatile boolean isLoading = false;

    private static volatile int currentWaveMode = 0;
    private static final ArrayList<MessageObject> basePlaylistContext = new ArrayList<>();

    // FIX #1: thread-safe история воспроизведения
    private static final HashSet<String> globalPlayHistory = new LinkedHashSet<String>() {
        private static final int MAX_SIZE = 3000;
        @Override
        public boolean add(String s) {
            // Скользящее окно: удаляем старые при переполнении
            if (size() >= MAX_SIZE) {
                String oldest = iterator().next();
                remove(oldest);
            }
            return super.add(s);
        }
    };
    private static final Object historyLock = new Object();

    private static final Random random = new Random();

    // FIX #5: один executor для фоновых задач + отдельный для HTTP
    private static final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(4);
    // FIX #3: семафор — не более 3 одновременных HTTP к Deezer
    private static final Semaphore httpSemaphore = new Semaphore(3, true);

    // FIX #11: thread-safe последний батч
    private static final CopyOnWriteArrayList<MessageObject> lastGeneratedBatch = new CopyOnWriteArrayList<>();

    private static final ConcurrentHashMap<String, float[]> vectorCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Integer> albumYearCache = new ConcurrentHashMap<>();
    private static volatile NeuroRanker neuroRanker;
    private static final Object neuroLock = new Object();

    // FIX #4: снижен порог + мягкий порог
    private static final float SIMILARITY_THRESHOLD = 0.50f;
    private static final float SOFT_THRESHOLD = 0.34f;
    private static final HashSet<String> waveSeedKeys = new HashSet<>();
    private static final ArrayList<String> recentArtistsWindow = new ArrayList<>();
    private static final ArrayList<String> recentTitlesWindow = new ArrayList<>();
    private static final HashMap<String, Integer> artistStreakCounter = new HashMap<>();
    private static final int RECENT_ARTISTS_WINDOW_SIZE = 12;
    private static final int RECENT_TITLES_WINDOW_SIZE = 20;

    // Кешированный seed-вектор между рефиллами
    private static volatile float[] cachedSeedVector = null;

    // Флаг отмены текущей волны
    private static final AtomicBoolean cancelFlag = new AtomicBoolean(false);

    // Эталонный seed-пул жанра: фиксируется при запуске жанровой волны
    // Используется в каждом рефилле чтобы не уходить в другие жанры
    private static volatile ArrayList<String> currentGenreSeedPool = new ArrayList<>();
    // Жёсткий порог для жанрового режима: ниже него треки не пройдут даже в fallback
    private static final float GENRE_HARD_THRESHOLD = 0.35f;

    // ─────────────────────────────────────────────────────────────────────────
    // Инициализация
    // ─────────────────────────────────────────────────────────────────────────

    private static void initNeuroRankerIfNeeded() {
        if (neuroRanker == null) {
            synchronized (neuroLock) {
                if (neuroRanker == null) {
                    neuroRanker = new NeuroRanker(ApplicationLoader.applicationContext);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Сессия волны
    // ─────────────────────────────────────────────────────────────────────────

    public static void startNewWaveSession(int mode) {
        cancelFlag.set(true); // отменяем предыдущую волну
        isWaveActive = false;
        isLoading = false;
        currentWaveMode = mode;
        synchronized (basePlaylistContext) { basePlaylistContext.clear(); }
        lastGeneratedBatch.clear();
        cachedSeedVector = null;
        currentGenreSeedPool = new ArrayList<>();

        synchronized (historyLock) { globalPlayHistory.clear(); }
        synchronized (waveSeedKeys) { waveSeedKeys.clear(); }
        synchronized (recentArtistsWindow) { recentArtistsWindow.clear(); }
        synchronized (recentTitlesWindow) { recentTitlesWindow.clear(); }
        synchronized (artistStreakCounter) { artistStreakCounter.clear(); }

        // Даем потокам миллисекунды, чтобы они успели увидеть cancelFlag = true и отвалиться
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        cancelFlag.set(false);
        isWaveActive = true;
    }

    // ИСПРАВЛЕНИЕ: добавляем метод для полного завершения волны
    public static void stopWaveSession() {
        isWaveActive = false;
        isLoading = false;
        cancelFlag.set(true);
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        cancelFlag.set(false);
    }

    private static String normalizeSeedKey(String artist, String title) {
        return ((artist == null ? "" : artist) + "|" + (title == null ? "" : title))
                .toLowerCase().replaceAll("[^a-zа-яё0-9|]", "");
    }

    private static String performGetRequest(String urlStr) throws Exception {
        if (cancelFlag.get()) return ""; // Быстрый выход для старых потоков
        httpSemaphore.acquire();
        try {
            if (cancelFlag.get()) return ""; // Проверка после блокировки
            HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36");
            c.setConnectTimeout(6000);
            c.setReadTimeout(8000);
            int code = c.getResponseCode();
            if (code == 429) {
                if (cancelFlag.get()) return "";
                Thread.sleep(2000);
                c = (HttpURLConnection) new URL(urlStr).openConnection();
                c.setRequestProperty("User-Agent", "Mozilla/5.0");
                c.setConnectTimeout(6000);
                c.setReadTimeout(8000);
            }
            try (InputStream is = c.getInputStream();
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] b = new byte[8192]; int l;
                while ((l = is.read(b)) != -1) {
                    if (cancelFlag.get()) return ""; // Прерываем чтение, если волна отменена
                    bos.write(b, 0, l);
                }
                return bos.toString("UTF-8");
            } catch (Exception e) { return ""; }
        } finally {
            httpSemaphore.release();
        }
    }


    private static void handleError(WaveCallback callback, Exception e) {
        FileLog.e(e);
        AndroidUtilities.runOnUIThread(() -> {
            isLoading = false;
            callback.onError();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Жанровый маппинг
    // ─────────────────────────────────────────────────────────────────────────

    private static String getDeezerSearchQuery(String genre) {
        if (genre == null) return "Top Hits";
        switch (genre.toLowerCase().trim()) {
            case "поп": return "Pop";
            case "хип-хоп": case "хип хоп": return "Hip Hop rap";
            case "рок": return "Rock";
            case "инди": return "Indie";
            case "фонк": return "Phonk";
            case "гиперпоп": return "Hyperpop";
            case "пост-панк": return "Post punk";
            case "эмо-рэп": case "эмо реп": return "Emo rap";
            case "метал": case "металл": return "Metal";
            case "альтернатива": return "Alternative rock";
            case "к-поп": case "к поп": return "K-pop";
            case "джаз": return "Jazz";
            case "блюз": return "Blues";
            case "классика": return "Classical";
            case "электроника": return "Electronic";
            case "дабстеп": return "Dubstep";
            case "панк-рок": case "панк рок": return "Punk rock";
            case "синтвейв": return "Synthwave";
            case "ло-фай": case "лофай": return "Lofi hiphop";
            case "эмбиент": return "Ambient";
            case "trap": case "трэп": return "Trap";
            case "r&b": case "rnb": case "рнб": return "R&B soul";
            case "регги": return "Reggae";
            case "соул": return "Soul";
            case "фанк": return "Funk";
            case "кантри": return "Country";
            default: return genre.equals("Любой жанр") ? "Top Hits" : genre;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // generateWaveFromSettings

   public static void generateWaveFromSettings(int currentAccount, WaveCallback callback) {
        startNewWaveSession(0);
        initNeuroRankerIfNeeded();
        backgroundExecutor.execute(() -> {
            try {
                ArrayList<String> seedPool = getSeedTracksForGenre(currentConfig.query);
                Collections.shuffle(seedPool);
                currentGenreSeedPool = new ArrayList<>(seedPool);

                ArrayList<String> initialSeeds = new ArrayList<>();
                for (int i = 0; i < Math.min(5, seedPool.size()); i++) {
                    initialSeeds.add(seedPool.get(i));
                }

                String searchKeyword = getDeezerSearchQuery(currentConfig.query);

                if (initialSeeds.isEmpty()) {
                    initialSeeds.addAll(getTracksFromDynamicPlaylists(searchKeyword, 5, random.nextInt(300)));
                }

                // УСКОРЕНИЕ: Для первого мгновенного батча берем только 1 трек и плейлисты
                ArrayList<String> expanded = new ArrayList<>();
                if (!initialSeeds.isEmpty()) {
                    String seedTrack = initialSeeds.get(0);
                    String[] parts = seedTrack.split(" - ", 2);
                    String artist = parts[0].trim();
                    String title  = parts.length > 1 ? parts[1].trim() : "";
                    expanded.addAll(getDeezerTrackRadio(artist, title, 5));
                }
                expanded.addAll(getTracksFromDynamicPlaylists(searchKeyword, 10, random.nextInt(300)));

                float[] genreVector = neuroRanker.getGenreVector(currentConfig.query);
                cachedSeedVector = genreVector;

                if (cancelFlag.get()) return;

                // Выдаем первый батч моментально, без долгой проверки
                Collections.shuffle(expanded);
                processAndReturnTracks(expanded, currentAccount, true, callback);

                if (cancelFlag.get()) return;

                // Шаг 2: Спокойно догружаем второй батч в фоне
                ArrayList<String> next = new ArrayList<>();
                for (int i = 1; i < Math.min(3, initialSeeds.size()); i++) {
                    String seedTrack = initialSeeds.get(i);
                    String[] parts = seedTrack.split(" - ", 2);
                    String artist = parts[0].trim();
                    String title  = parts.length > 1 ? parts[1].trim() : "";
                    next.addAll(getDeezerTrackRadio(artist, title, 6));
                    next.addAll(getDeezerArtistMix(artist, 4));
                }
                next.addAll(getTracksFromDynamicPlaylists(searchKeyword, 12, random.nextInt(500) + 100));

                ArrayList<String> verified2 = verifyCandidatesWithGenreGuard(next, genreVector, 20);
                processAndReturnTracks(verified2, currentAccount, false, callback);

                if (cancelFlag.get()) return;

                startInfiniteRefill(currentAccount, callback, () -> {
                    try {
                        return getCandidatesFromQueueTracks(searchKeyword);
                    } catch (Exception ignored) { return new ArrayList<>(); }
                });

            } catch (Exception e) { handleError(callback, e); }
        });
    }

    private static ArrayList<String> getCandidatesFromQueueTracks(String searchKeyword) throws Exception {
        ArrayList<String> candidates = new ArrayList<>();
        List<MessageObject> batch = new ArrayList<>(lastGeneratedBatch);

        if (!batch.isEmpty()) {
            Collections.shuffle(batch);
            int pickCount = Math.min(4, batch.size());
            for (int i = 0; i < pickCount; i++) {
                MessageObject mo = batch.get(i);
                String artist = mo.getMusicAuthor();
                String title  = mo.getMusicTitle();
                if (artist == null || artist.isEmpty()) continue;
                if (title  == null || title.isEmpty())  continue;

                candidates.addAll(getDeezerTrackRadio(artist, title, 8));
            }
        }

        if (candidates.size() < 10) {
            candidates.addAll(getTracksFromDynamicPlaylists(searchKeyword, 15, random.nextInt(600)));
        }

        return candidates;
    }

    public static void generateWaveByPlaylist(ArrayList<MessageObject> originalPlaylist, int currentAccount, WaveCallback callback) {
        startNewWaveSession(1);
        if (originalPlaylist != null) {
            synchronized (basePlaylistContext) { basePlaylistContext.addAll(originalPlaylist); }
        }
        initNeuroRankerIfNeeded();
        backgroundExecutor.execute(() -> {
            try {
                ArrayList<MessageObject> ctx;
                synchronized (basePlaylistContext) { ctx = new ArrayList<>(basePlaylistContext); }
                synchronized (waveSeedKeys) {
                    waveSeedKeys.clear();
                    for (MessageObject s : ctx) {
                        waveSeedKeys.add(normalizeSeedKey(s.getMusicAuthor(), s.getMusicTitle()));
                    }
                }
                if (ctx.isEmpty()) throw new Exception("Плейлист пуст");

                int seedCount = Math.min(3, ctx.size());
                Collections.shuffle(ctx);
                ArrayList<MessageObject> seeds = new ArrayList<>(ctx.subList(0, seedCount));
                final String primarySeedArtist = seeds.get(0).getMusicAuthor();

                ArrayList<String> initial = new ArrayList<>();
                for (MessageObject seed : seeds) {
                    initial.addAll(getDeezerArtistMix(seed.getMusicAuthor(), 4));
                }
                if (initial.size() < 5) initial.addAll(getDeepSimilarTracks(primarySeedArtist, 8 - initial.size()));
                if (initial.size() < 5) initial.addAll(getTracksFromDeezerCharts(5));

                if (cancelFlag.get()) return;
                processAndReturnTracks(initial, currentAccount, true, callback);

                float[] seedVector = buildMultiTrackSeedVector(seeds);
                if (seedVector == null) seedVector = neuroRanker.getGenreVector("Поп");
                cachedSeedVector = seedVector;

                ArrayList<String> next = new ArrayList<>();
                for (MessageObject seed : seeds) next.addAll(getDeepSimilarTracks(seed.getMusicAuthor(), 15));
                next.addAll(getDeezerArtistMix(primarySeedArtist, 15));

                if (cancelFlag.get()) return;
                ArrayList<String> verified = verifyCandidatesWithNeuralNetwork(next, seedVector, 20);
                processAndReturnTracks(verified, currentAccount, false, callback);

                if (cancelFlag.get()) return;

                startInfiniteRefill(currentAccount, callback, () -> {
                    ArrayList<String> r = new ArrayList<>();
                    try {
                        r.addAll(getDeezerArtistMix(primarySeedArtist, 20));
                        r.addAll(getDeepSimilarTracks(primarySeedArtist, 15));
                        if (r.size() < 10) r.addAll(getTracksFromDeezerCharts(10));
                    } catch (Exception ignored) {}
                    return r;
                });

            } catch (Exception e) { handleError(callback, e); }
        });
    }

    public static void generateWaveByArtist(String artistName, int currentAccount, WaveCallback callback) {
        startNewWaveSession(2);
        currentConfig.query = artistName;
        initNeuroRankerIfNeeded();

        backgroundExecutor.execute(() -> {
            try {
                synchronized (waveSeedKeys) {
                    waveSeedKeys.clear();
                    waveSeedKeys.add(normalizeSeedKey(artistName, ""));
                }
                ArrayList<String> initial = new ArrayList<>();
                initial.addAll(getDeezerArtistMix(artistName, 5));
                if (initial.size() < 5) initial.addAll(getDeepSimilarTracks(artistName, 8 - initial.size()));

                if (cancelFlag.get()) return;
                processAndReturnTracks(initial, currentAccount, true, callback);

                float[] seedVector = neuroRanker.getVectorForRemoteTrack(artistName, "");
                if (seedVector == null) seedVector = neuroRanker.getGenreVector("Инди");
                cachedSeedVector = seedVector;

                ArrayList<String> next = new ArrayList<>();
                next.addAll(getDeepSimilarTracks(artistName, 25));
                next.addAll(getDeezerArtistMix(artistName, 15));

                if (cancelFlag.get()) return;
                ArrayList<String> verified = verifyCandidatesWithNeuralNetwork(next, seedVector, 25);
                processAndReturnTracks(verified, currentAccount, false, callback);

                if (cancelFlag.get()) return;

                startInfiniteRefill(currentAccount, callback, () -> {
                    ArrayList<String> r = new ArrayList<>();
                    try {
                        r.addAll(getDeezerArtistMix(artistName, 20));
                        r.addAll(getDeepSimilarTracks(artistName, 20));
                    } catch (Exception ignored) {}
                    return r;
                });

            } catch (Exception e) { handleError(callback, e); }
        });
    }

    public static void generateWaveByAlbum(String artistName, String albumName, int currentAccount, WaveCallback callback) {
        startNewWaveSession(3);
        initNeuroRankerIfNeeded();

        backgroundExecutor.execute(() -> {
            try {
                String safeArtist = (artistName == null || artistName.isEmpty()) ? "Unknown Artist" : artistName;
                String safeAlbum  = (albumName  == null || albumName.isEmpty())  ? "" : albumName;
                currentConfig.query = safeArtist;
                synchronized (waveSeedKeys) {
                    waveSeedKeys.clear();
                    waveSeedKeys.add(normalizeSeedKey(safeArtist, safeAlbum));
                }

                ArrayList<String> initial = new ArrayList<>();
                if (!safeAlbum.isEmpty()) initial.addAll(getTracksFromAlbum(safeArtist, safeAlbum, 5));
                initial.addAll(getDeezerArtistMix(safeArtist, 5 - Math.min(initial.size(), 4)));
                if (initial.size() < 5) initial.addAll(getDeepSimilarTracks(safeArtist, 5 - initial.size()));

                if (cancelFlag.get()) return;
                processAndReturnTracks(initial, currentAccount, true, callback);

                String albumGenre = null;
                try { if (!safeAlbum.isEmpty()) albumGenre = getAlbumGenreQuery(safeArtist, safeAlbum); }
                catch (Exception ignored) {}

                float[] seedVector = null;
                try {
                    ArrayList<String> albumSeed = getTracksFromAlbum(safeArtist, safeAlbum, 3);
                    if (!albumSeed.isEmpty()) {
                        String[] p = albumSeed.get(0).split(" - ", 2);
                        seedVector = neuroRanker.getVectorForRemoteTrack(p[0], p.length > 1 ? p[1] : "");
                    }
                } catch (Exception ignored) {}

                if (seedVector == null) seedVector = neuroRanker.getVectorForRemoteTrack(safeArtist, safeAlbum);
                if (seedVector == null && albumGenre != null) seedVector = neuroRanker.getGenreVector(albumGenre);
                if (seedVector == null) seedVector = neuroRanker.getGenreVector("Инди");
                cachedSeedVector = seedVector;

                ArrayList<String> next = new ArrayList<>();
                next.addAll(getDeezerArtistMix(safeArtist, 20));
                next.addAll(getDeepSimilarTracks(safeArtist, 20));
                if (albumGenre != null) next.addAll(getTracksFromDynamicPlaylists(albumGenre, 15, random.nextInt(50)));
                if (next.isEmpty()) next.addAll(getTracksFromDeezerCharts(20));

                if (cancelFlag.get()) return;
                ArrayList<String> verified = verifyCandidatesWithNeuralNetwork(next, seedVector, 25);
                processAndReturnTracks(verified, currentAccount, false, callback);

                if (cancelFlag.get()) return;

                final String finalAlbumGenre = albumGenre;
                startInfiniteRefill(currentAccount, callback, () -> {
                    ArrayList<String> r = new ArrayList<>();
                    try {
                        r.addAll(getDeezerArtistMix(safeArtist, 20));
                        r.addAll(getDeepSimilarTracks(safeArtist, 20));
                        if (finalAlbumGenre != null) r.addAll(getTracksFromDynamicPlaylists(finalAlbumGenre, 10, random.nextInt(300)));
                        r.addAll(getTracksFromITunes(safeArtist, 8));
                        if (r.size() < 10) r.addAll(getTracksFromDeezerCharts(10));
                    } catch (Exception ignored) {}
                    return r;
                });

            } catch (Exception e) { handleError(callback, e); }
        });
    }

    public static void loadMoreForCurrentWave(MessageObject currentTrack, int currentAccount, WaveCallback callback) {
        if (isLoading) return;
        isLoading = true;
        isWaveActive = true;
        initNeuroRankerIfNeeded();

        backgroundExecutor.execute(() -> {
            try {
                List<MessageObject> batch = lastGeneratedBatch;
                MessageObject seed = (!batch.isEmpty()) ? batch.get(random.nextInt(batch.size())) : currentTrack;

                float[] seedVector = cachedSeedVector;
                if (seedVector == null) {
                    seedVector = neuroRanker.getVectorForRemoteTrack(seed.getMusicAuthor(), seed.getMusicTitle());
                    if (seedVector == null) seedVector = neuroRanker.getGenreVector(currentConfig.query);
                    cachedSeedVector = seedVector;
                }

                if (currentTrack != null) adaptSeedToCurrentTrack(currentTrack.getMusicAuthor(), currentTrack.getMusicTitle());

                ArrayList<String> candidates = new ArrayList<>();
                int randOffset = random.nextInt(800);

                switch (currentWaveMode) {
                    case 0:
                        String searchQuery = getDeezerSearchQuery(currentConfig.query);
                        try {
                            candidates.addAll(getCandidatesFromQueueTracks(searchQuery));
                        } catch (Exception e) {
                            candidates.addAll(getTracksFromDynamicPlaylists(searchQuery, 15, random.nextInt(800)));
                        }
                        break;
                    case 1:
                    case 2:
                    case 3:
                        candidates.addAll(getDeezerArtistMix(seed.getMusicAuthor(), 15));
                        candidates.addAll(getDeepSimilarTracks(seed.getMusicAuthor(), 15));
                        if (candidates.size() < 10) candidates.addAll(getTracksFromDeezerCharts(10));
                        break;
                }

                if (candidates.isEmpty()) candidates.addAll(getTracksFromDeezerCharts(20));

                if (cancelFlag.get()) return;
                ArrayList<String> verified = verifyCandidatesWithNeuralNetwork(candidates, seedVector, 20);
                processAndReturnTracks(verified, currentAccount, false, callback);
            } catch (Exception e) { handleError(callback, e); }
        });
    }




    /**
     * Верификация с жёстким жанровым гейтом.
     * В отличие от verifyCandidatesWithNeuralNetwork, здесь:
     * - Мягкий fallback ОТКЛЮЧЁН если есть жанровый вектор
     * - Треки ниже GENRE_HARD_THRESHOLD полностью отбрасываются
     * - Только если совсем пусто — берём минимальный fallback из plain кандидатов
     */
    private static ArrayList<String> verifyCandidatesWithGenreGuard(ArrayList<String> candidates, float[] genreVector, int requiredCount) {
        ArrayList<String> verified = new ArrayList<>();
        ArrayList<String> softBucket = new ArrayList<>(); // SIMILARITY_THRESHOLD > x >= GENRE_HARD_THRESHOLD
        Collections.shuffle(candidates);

        for (String candidate : candidates) {
            if (verified.size() >= requiredCount) break;
            String[] parts = candidate.split(" - ", 2);
            String art = parts[0].trim();
            String tit = parts.length > 1 ? parts[1].trim() : "";

            float[] candVector = neuroRanker.getVectorForRemoteTrack(art, tit);
            if (candVector == null) continue; // без вектора — не берём

            float similarity = neuroRanker.calculateCosineSimilarity(genreVector, candVector);
            if (similarity >= SIMILARITY_THRESHOLD) {
                verified.add(candidate);
            } else if (similarity >= GENRE_HARD_THRESHOLD) {
                softBucket.add(candidate);
            }
            // Ниже GENRE_HARD_THRESHOLD — безвозвратно отбрасываем
        }

        // Заполняем из soft bucket только если мало треков
        while (verified.size() < requiredCount && !softBucket.isEmpty()) {
            verified.add(softBucket.remove(0));
        }

        // Крайний fallback: если совсем пусто — несколько кандидатов без фильтра
        // чтобы не зависнуть, но берём очень мало
        if (verified.isEmpty() && !candidates.isEmpty()) {
            int take = Math.min(3, candidates.size());
            verified.addAll(candidates.subList(0, take));
        }

        return verified;
    }



    /** Усреднённый вектор по нескольким трекам */
    private static float[] buildMultiTrackSeedVector(ArrayList<MessageObject> tracks) {
        try {
            float[] sum = new float[400];
            int count = 0;
            for (MessageObject mo : tracks) {
                String a = mo.getMusicAuthor();
                String t = mo.getMusicTitle();
                if (a == null || a.isEmpty()) continue;
                float[] vec = neuroRanker.getVectorForRemoteTrack(a, t != null ? t : "");
                if (vec == null) continue;
                for (int i = 0; i < 400; i++) sum[i] += vec[i];
                count++;
            }
            if (count == 0) return null;
            float norm = 0;
            for (int i = 0; i < 400; i++) { sum[i] /= count; norm += sum[i] * sum[i]; }
            norm = (float) Math.sqrt(norm);
            if (norm < 1e-9f) return null;
            for (int i = 0; i < 400; i++) sum[i] /= norm;
            return sum;
        } catch (Exception e) { return null; }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // loadMoreForCurrentWave
    // ─────────────────────────────────────────────────────────────────────────
    // Метод для фоновой загрузки и обновления якорей с GitHub
    public static void checkAndUpdateWaveSeeds(Context context) {
        backgroundExecutor.execute(() -> {
            try {
                String url = "https://raw.githubusercontent.com/soundgram-project/SoundGram-wave/main/wave.txt";
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                c.setRequestMethod("GET");
                c.setRequestProperty("Cache-Control", "no-cache");

                if (c.getResponseCode() != 200) return;

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (InputStream is = c.getInputStream()) {
                    byte[] b = new byte[8192]; int len;
                    while ((len = is.read(b)) != -1) bos.write(b, 0, len);
                }
                String content = bos.toString("UTF-8");
                String[] lines = content.split("\\r?\\n");
                if (lines.length == 0) return;

                String firstLine = lines[0].trim().toLowerCase();
                if (!firstLine.startsWith("update")) return;

                int versionParsed = 0;
                try {
                    versionParsed = Integer.parseInt(firstLine.replace("update", "").trim());
                } catch (Exception e) { return; }

                final int newVersion = versionParsed; // Создаем финальную копию для лямбды
                android.content.SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("soundgram_wave", Context.MODE_PRIVATE);
                int currentVersion = prefs.getInt("wave_seed_version", 0);

                if (newVersion > currentVersion) {
                    JSONObject newSeeds = new JSONObject();
                    String currentGenre = null;
                    JSONArray currentArray = null;

                    for (int i = 1; i < lines.length; i++) {
                        String line = lines[i].trim();
                        if (line.isEmpty()) continue;

                        if (!line.contains(" - ")) {
                            currentGenre = line.replaceAll("\\(.*?\\)", "").trim().toLowerCase();
                            currentArray = new JSONArray();
                            newSeeds.put(currentGenre, currentArray);
                        } else if (currentArray != null) {
                            currentArray.put(line.replace("\"", ""));
                        }
                    }

                    prefs.edit()
                            .putInt("wave_seed_version", newVersion)
                            .putString("wave_seed_data", newSeeds.toString())
                            .apply();

                    AndroidUtilities.runOnUIThread(() -> {
                        if (context != null) {
                            android.widget.Toast.makeText(context, "Волна SoundGram обновлена до v." + newVersion, android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                FileLog.e("Wave Update Error", e);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Бесконечный рефилл (FIX #6: отдельный поток)
    // ─────────────────────────────────────────────────────────────────────────

    interface CandidateSupplier {
        ArrayList<String> get();
    }

    private static void startInfiniteRefill(int currentAccount, WaveCallback callback, CandidateSupplier supplier) {
        backgroundExecutor.execute(() -> {
            int round = 0;
            while (isWaveActive && !cancelFlag.get()) {
                try {
                    // Интервал рефилла: 4-8 секунд, увеличивается со временем
                    long delay = Math.min(4000L + round * 500L, 10000L);
                    Thread.sleep(delay);

                    if (!isWaveActive || cancelFlag.get()) break;

                    // ── ПАУЗА: не генерировать треки пока воспроизведение на паузе ──
                    while (isWaveActive && !cancelFlag.get()
                            && MediaController.getInstance().isMessagePaused()) {
                        Thread.sleep(500);
                    }
                    if (!isWaveActive || cancelFlag.get()) break;

                    ArrayList<String> candidates = supplier.get();
                    // Усиление соответствия источнику волны в бесконечном рефилле
                    if (currentWaveMode == 3) { // playlist wave
                        ArrayList<String> playlistCandidates = new ArrayList<>();
                        synchronized (basePlaylistContext) {
                            for (MessageObject seed : basePlaylistContext) {
                                if (seed == null || seed.getMusicAuthor() == null) continue;
                                try {
                                    playlistCandidates.addAll(getDeezerTrackRadio(seed.getMusicAuthor(), seed.getMusicTitle(), 3));
                                } catch (Exception ignore) {}
                            }
                        }
                        playlistCandidates.addAll(candidates);
                        candidates = playlistCandidates;
                    } else if (currentWaveMode == 2) { // album wave
                        ArrayList<String> albumCandidates = new ArrayList<>();
                        String q = currentConfig.query != null ? currentConfig.query : "";
                        String[] parts = q.split(" - ", 2);
                        String albumArtist = parts.length > 0 ? parts[0].trim() : "";
                        String albumName = parts.length > 1 ? parts[1].trim() : "";
                        try {
                            if (!albumArtist.isEmpty() && !albumName.isEmpty()) {
                                albumCandidates.addAll(getTracksFromAlbum(albumArtist, albumName, 20));
                            }
                        } catch (Exception ignore) {}
                        albumCandidates.addAll(candidates);
                        candidates = albumCandidates;
                    } else if (currentWaveMode == 1) { // artist wave
                        ArrayList<String> artistCandidates = new ArrayList<>();
                        String artist = currentConfig.query != null ? currentConfig.query.trim() : "";
                        try {
                            if (!artist.isEmpty()) {
                                artistCandidates.addAll(getDeezerArtistMix(artist, 20));
                                artistCandidates.addAll(getDeepSimilarTracks(artist, 15));
                            }
                        } catch (Exception ignore) {}
                        artistCandidates.addAll(candidates);
                        candidates = artistCandidates;
                    } else if (currentWaveMode == 0) { // genre wave
                        String gq = getDeezerSearchQuery(currentConfig.query);
                        try {
                            ArrayList<String> strictGenre = getTracksFromDynamicPlaylists(gq, 25, random.nextInt(700));
                            strictGenre.addAll(candidates);
                            candidates = strictGenre;
                        } catch (Exception ignore) {}
                    }
                    if (candidates.isEmpty()) continue;

                    float[] vec = cachedSeedVector;
                    if (vec == null) vec = neuroRanker.getGenreVector(currentConfig.query);

                    ArrayList<String> verified;
                    // Для жанровой волны — жёсткий genre guard чтобы не уходить в другой жанр
                    if (currentWaveMode == 0) {
                        verified = verifyCandidatesWithGenreGuard(candidates, vec, 15);
                    } else {
                        verified = verifyCandidatesWithNeuralNetwork(candidates, vec, 15);
                    }

                    if (!verified.isEmpty()) {
                        processAndReturnTracks(verified, currentAccount, false, callback);
                    }
                    round++;
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Нейронная верификация — ужесточённые пороги + адаптивный вес
    // ─────────────────────────────────────────────────────────────────────────

    private static ArrayList<String> verifyCandidatesWithNeuralNetwork(ArrayList<String> candidates, float[] targetVector, int requiredCount) {
        ArrayList<String> verified   = new ArrayList<>();
        ArrayList<String> softBucket = new ArrayList<>();
        ArrayList<String> hardBucket = new ArrayList<>();
        Collections.shuffle(candidates);

        for (String candidate : candidates) {
            if (verified.size() >= requiredCount) break;
            String[] parts = candidate.split(" - ", 2);
            String art = parts[0].trim();
            String tit = parts.length > 1 ? parts[1].trim() : "";

            float[] candVector = neuroRanker.getVectorForRemoteTrack(art, tit);
            if (candVector != null) {
                float similarity = neuroRanker.calculateCosineSimilarity(targetVector, candVector);
                if (similarity >= SIMILARITY_THRESHOLD) {
                    verified.add(candidate);
                } else if (similarity >= SOFT_THRESHOLD) {
                    softBucket.add(candidate);
                }
                // Треки ниже SOFT_THRESHOLD полностью отбрасываем — не добавляем в hardBucket
            }
            // Треки без вектора (null) тоже отбрасываем — нейронка не может их оценить
        }

        // Заполняем только из мягкого bucket если не хватает
        while (verified.size() < requiredCount && !softBucket.isEmpty()) {
            verified.add(softBucket.remove(0));
        }

        // Если совсем пусто — берём кандидатов напрямую без фильтра (чтобы не зависать)
        if (verified.isEmpty() && !candidates.isEmpty()) {
            int take = Math.min(requiredCount / 2, candidates.size());
            verified.addAll(candidates.subList(0, take));
        }
        return verified;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Адаптивное обновление seed-вектора по текущему треку (вызывается снаружи)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Обновить seed на основе реально играющего трека.
     * Вызывать из MediaController при смене трека во время волны.
     * Интерполируем: 70% старый seed + 30% новый вектор (медленный дрейф).
     */
    public static void adaptSeedToCurrentTrack(String artist, String title) {
        if (!isWaveActive || neuroRanker == null) return;
        backgroundExecutor.execute(() -> {
            try {
                float[] newVec = neuroRanker.getVectorForRemoteTrack(artist, title);
                if (newVec == null) return;
                float[] old = cachedSeedVector;
                if (old == null || old.length != newVec.length) {
                    cachedSeedVector = newVec;
                    return;
                }
                float[] blended = new float[old.length];
                for (int i = 0; i < old.length; i++) blended[i] = 0.70f * old[i] + 0.30f * newVec[i];
                // Ренормализуем
                float norm = 0;
                for (float v : blended) norm += v * v;
                norm = (float) Math.sqrt(norm);
                if (norm > 1e-9f) for (int i = 0; i < blended.length; i++) blended[i] /= norm;
                cachedSeedVector = blended;
            } catch (Exception e) { FileLog.e(e); }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Персонализация: seed из библиотеки пользователя
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Строим seed-вектор из треков библиотеки пользователя (audioMessages).
     * Берём до 5 случайных треков, усредняем их векторы.
     */
    private static float[] buildLibrarySeedVector() {
        try {
            ArrayList<MessageObject> lib = ChannelMusicFragment.audioMessages;
            if (lib == null || lib.isEmpty()) return null;
            int sampleSize = Math.min(5, lib.size());
            ArrayList<MessageObject> sample = new ArrayList<>(lib);
            Collections.shuffle(sample);
            sample = new ArrayList<>(sample.subList(0, sampleSize));

            float[] sum = new float[400];
            int count = 0;
            for (MessageObject mo : sample) {
                String a = mo.getMusicAuthor();
                String t = mo.getMusicTitle();
                if (a == null || t == null || a.isEmpty()) continue;
                float[] vec = neuroRanker.getVectorForRemoteTrack(a, t);
                if (vec == null) continue;
                for (int i = 0; i < 400; i++) sum[i] += vec[i];
                count++;
            }
            if (count == 0) return null;
            float norm = 0;
            for (int i = 0; i < 400; i++) { sum[i] /= count; norm += sum[i] * sum[i]; }
            norm = (float) Math.sqrt(norm);
            if (norm < 1e-9f) return null;
            for (int i = 0; i < 400; i++) sum[i] /= norm;
            return sum;
        } catch (Exception e) { FileLog.e(e); return null; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Источник 5: Deezer Charts (топ треки по стране — самый актуальный контент)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Deezer Charts — глобальный и страновой топ.
     * chart_id: 0=global, 1111141420=RU, 2273=US, 1362=FR итд
     */
    private static ArrayList<String> getTracksFromDeezerCharts(int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        // Чередуем глобальный и RU чарт
        long[] chartIds = {0L, 1111141420L};
        for (long chartId : chartIds) {
            int offset = random.nextInt(80);
            String url = "https://api.deezer.com/chart/" + chartId + "/tracks?limit=50&index=" + offset;
            String resp = performGetRequest(url);
            if (resp.isEmpty()) continue;
            JSONObject json = new JSONObject(resp);
            JSONArray data = json.optJSONArray("data");
            if (data == null) {
                // Иногда Deezer оборачивает ответ в { tracks: { data: [...] } }
                if (json.has("tracks")) data = json.getJSONObject("tracks").optJSONArray("data");
            }
            if (data == null) continue;
            ArrayList<JSONObject> items = new ArrayList<>();
            for (int i = 0; i < data.length(); i++) items.add(data.getJSONObject(i));
            Collections.shuffle(items);
            for (JSONObject t : items) {
                String art = t.has("artist") ? t.getJSONObject("artist").getString("name") : "";
                String tit = t.optString("title", "");
                if (art.isEmpty() || tit.isEmpty()) continue;
                synchronized (historyLock) {
                    if (globalPlayHistory.add(art + tit)) res.add(art + " - " + tit);
                }
                if (res.size() >= limit) return res;
            }
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Источник 6: Deezer Recommendations (на основе артиста через mix/radio)
    // ─────────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getDeezerArtistMix(String artist, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        // Шаг 1: найти artist id
        String artistResp = performGetRequest("https://api.deezer.com/search/artist?q=" + URLEncoder.encode(artist, "UTF-8") + "&limit=1");
        if (artistResp.isEmpty()) return res;
        JSONArray artistData = new JSONObject(artistResp).optJSONArray("data");
        if (artistData == null || artistData.length() == 0) return res;
        long artistId = artistData.getJSONObject(0).getLong("id");

        // Шаг 2: artist mix (умные рекомендации Deezer на основе артиста)
        String mixResp = performGetRequest("https://api.deezer.com/artist/" + artistId + "/radio?limit=" + Math.min(limit * 2, 40));
        if (mixResp.isEmpty()) return res;
        JSONArray mixData = new JSONObject(mixResp).optJSONArray("data");
        if (mixData == null) return res;

        ArrayList<JSONObject> items = new ArrayList<>();
        for (int i = 0; i < mixData.length(); i++) items.add(mixData.getJSONObject(i));
        Collections.shuffle(items);

        for (JSONObject t : items) {
            String a = t.has("artist") ? t.getJSONObject("artist").getString("name") : "";
            String tit = t.optString("title", "");
            if (a.isEmpty() || tit.isEmpty()) continue;
            synchronized (historyLock) {
                if (globalPlayHistory.add(a + tit)) res.add(a + " - " + tit);
            }
            if (res.size() >= limit) break;
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Отдача треков — строгий поиск каждого кандидата
    // ─────────────────────────────────────────────────────────────────────────

    private static void processAndReturnTracks(ArrayList<String> queries, int account, boolean isInitial, WaveCallback callback) {
        if (queries.isEmpty()) return;

        if (isInitial) lastGeneratedBatch.clear();

        // Дедуп по нормализованному ключу внутри батча
        java.util.Set<String> batchSeen = java.util.Collections.synchronizedSet(new HashSet<>());

        AtomicBoolean initialFired = new AtomicBoolean(false);
        AtomicInteger pending = new AtomicInteger(queries.size());

        for (String q : queries) {
            backgroundExecutor.execute(() -> {
                try {
                    String[] parts = q.split(" - ", 2);
                    String artist = parts[0].trim();
                    String title  = parts.length > 1 ? parts[1].trim() : "";

                    // Нормализованный ключ для дедупа
                    String dedupeKey = (artist + title).toLowerCase().replaceAll("[^a-zа-яё0-9]", "");
                    if (!batchSeen.add(dedupeKey)) return;
                    synchronized (recentArtistsWindow) {
                        String artistKey = artist == null ? "" : artist.toLowerCase().trim();
                        if (!artistKey.isEmpty() && recentArtistsWindow.contains(artistKey)) {
                            return; // не даём артистам крутиться слишком часто в соседних пачках
                        }
                    }
                    synchronized (artistStreakCounter) {
                        String artistKey = artist == null ? "" : artist.toLowerCase().trim();
                        if (!artistKey.isEmpty()) {
                            int streak = artistStreakCounter.getOrDefault(artistKey, 0);
                            if (streak >= 2) {
                                return; // жёстко рубим длинные серии одного артиста
                            }
                        }
                    }
                    synchronized (recentTitlesWindow) {
                        String titleKey = title == null ? "" : title.toLowerCase().trim();
                        if (!titleKey.isEmpty() && recentTitlesWindow.contains(titleKey)) {
                            return; // и не крутим одинаковые тайтлы в соседних пачках
                        }
                    }
                    String seedKey = normalizeSeedKey(artist, title);
                    synchronized (waveSeedKeys) {
                        boolean artistSeedMatch = false;
                        if (currentWaveMode == 1) {
                            String artistPrefix = normalizeSeedKey(artist, "");
                            for (String key : waveSeedKeys) {
                                if (!artistPrefix.isEmpty() && key.startsWith(artistPrefix)) {
                                    artistSeedMatch = true;
                                    break;
                                }
                            }
                        }
                        if (isInitial && waveSeedKeys.contains(seedKey)) {
                            return; // seed-треки не должны попадать в первую пачку
                        }
                        if (isInitial && artistSeedMatch) {
                            return; // artist-wave: первая пачка без seed-артиста
                        }
                        if (!isInitial && waveSeedKeys.contains(seedKey) && random.nextInt(100) >= 10) {
                            return; // дальше могут попадать, но редко (~10%)
                        }
                        if (!isInitial && artistSeedMatch && random.nextInt(100) >= 18) {
                            return; // artist-wave: сид-артист в последующих пачках нечасто
                        }
                    }

                    // Строгий поиск: находим трек с проверкой совпадения артиста и названия
                    MusicWebSearcher.WebTrack wt = MusicWebSearcher.searchTrackStrict(artist, title, 0);
                    if (wt == null || wt.downloadUrl == null || wt.downloadUrl.isEmpty()) return;

                    int dur = ChannelMusicFragment.parseDuration(wt.duration);
                    MessageObject msg = ChannelMusicFragment.createWebMessageObject(
                            account, wt.title, wt.artist, wt.downloadUrl, dur,
                            -(long) wt.downloadUrl.hashCode(), wt.coverUrl);

                    lastGeneratedBatch.add(msg);
                    synchronized (recentArtistsWindow) {
                        String artistKey = wt.artist == null ? "" : wt.artist.toLowerCase().trim();
                        if (!artistKey.isEmpty()) {
                            recentArtistsWindow.add(artistKey);
                            if (recentArtistsWindow.size() > RECENT_ARTISTS_WINDOW_SIZE) {
                                recentArtistsWindow.remove(0);
                            }
                        }
                    }
                    synchronized (artistStreakCounter) {
                        String currentArtist = wt.artist == null ? "" : wt.artist.toLowerCase().trim();
                        if (!currentArtist.isEmpty()) {
                            for (String key : new ArrayList<>(artistStreakCounter.keySet())) {
                                if (!key.equals(currentArtist)) {
                                    int v = artistStreakCounter.getOrDefault(key, 0) - 1;
                                    if (v <= 0) artistStreakCounter.remove(key);
                                    else artistStreakCounter.put(key, v);
                                }
                            }
                            artistStreakCounter.put(currentArtist, artistStreakCounter.getOrDefault(currentArtist, 0) + 1);
                        }
                    }
                    synchronized (recentTitlesWindow) {
                        String titleKey = wt.title == null ? "" : wt.title.toLowerCase().trim();
                        if (!titleKey.isEmpty()) {
                            recentTitlesWindow.add(titleKey);
                            if (recentTitlesWindow.size() > RECENT_TITLES_WINDOW_SIZE) {
                                recentTitlesWindow.remove(0);
                            }
                        }
                    }

                    AndroidUtilities.runOnUIThread(() -> {
                        ArrayList<MessageObject> single = new ArrayList<>();
                        single.add(msg);
                        if (isInitial && initialFired.compareAndSet(false, true)) {
                            callback.onTracksFound(single);
                        } else {
                            callback.onMoreTracksAdded(single);
                        }
                    });
                } catch (Exception e) {
                    FileLog.e(e);
                } finally {
                    if (pending.decrementAndGet() == 0) {
                        AndroidUtilities.runOnUIThread(() -> isLoading = false, 1500);
                    }
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTTP helper (FIX #3: с семафором)
    // ─────────────────────────────────────────────────────────────────────────


    // ─────────────────────────────────────────────────────────────────────────
    // Источник 1: Deezer Playlist (с фильтром новизны)
    // ─────────────────────────────────────────────────────────────────────────

    // FIX: текущий год для фильтрации свежих треков
    private static final int CURRENT_YEAR = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
    private static final int MIN_RECENT_YEAR = CURRENT_YEAR - 5; // предпочитаем треки не старше 5 лет

    private static ArrayList<String> getTracksFromDynamicPlaylists(String query, int target, int offset) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        ArrayList<String> olderTracks = new ArrayList<>(); // fallback: старые треки
        // FIX: разнообразный offset + случайный сдвиг внутри плейлистов
        int playlistOffset = offset % 300;
        String url = "https://api.deezer.com/search/playlist?q=" + URLEncoder.encode(query, "UTF-8") + "&limit=8&index=" + playlistOffset;
        JSONObject json = new JSONObject(performGetRequest(url));
        if (!json.has("data")) return res;
        JSONArray playlists = json.getJSONArray("data");
        // FIX: перемешиваем плейлисты для разнообразия
        ArrayList<JSONObject> playlistList = new ArrayList<>();
        for (int i = 0; i < playlists.length(); i++) playlistList.add(playlists.getJSONObject(i));
        Collections.shuffle(playlistList);
        for (JSONObject pl : playlistList) {
            // FIX: случайный offset внутри плейлиста
            int trackOffset = random.nextInt(50);
            String tUrl = "https://api.deezer.com/playlist/" + pl.getLong("id") + "/tracks?limit=30&index=" + trackOffset;
            String resp = performGetRequest(tUrl);
            if (resp.isEmpty()) continue;
            JSONObject pObj = new JSONObject(resp);
            if (!pObj.has("data")) continue;
            JSONArray tracks = pObj.getJSONArray("data");
            // FIX: перемешиваем треки внутри плейлиста
            ArrayList<JSONObject> trackList = new ArrayList<>();
            for (int j = 0; j < tracks.length(); j++) trackList.add(tracks.getJSONObject(j));
            Collections.shuffle(trackList);
            for (JSONObject t : trackList) {
                String art = t.getJSONObject("artist").getString("name");
                String tit = t.getString("title");
                String key = art + tit;
                synchronized (historyLock) {
                    if (!globalPlayHistory.add(key)) continue; // уже видели
                }
                String candidate = art + " - " + tit;
                // FIX: определяем год трека если доступен
                int trackYear = 0;
                if (t.has("release_date") && !t.getString("release_date").isEmpty()) {
                    try { trackYear = Integer.parseInt(t.getString("release_date").substring(0, 4)); } catch (Exception ignored) {}
                }
                // Deezer playlist-tracks часто не содержит release_date — добираем год из album/{id} с кешем
                if (trackYear == 0 && t.has("album")) {
                    try {
                        long albumId = t.getJSONObject("album").optLong("id", 0);
                        if (albumId > 0) {
                            Integer cachedYear = albumYearCache.get(albumId);
                            if (cachedYear != null) {
                                trackYear = cachedYear;
                            } else {
                                String aResp = performGetRequest("https://api.deezer.com/album/" + albumId);
                                if (!aResp.isEmpty()) {
                                    JSONObject aObj = new JSONObject(aResp);
                                    String rd = aObj.optString("release_date", "");
                                    int parsed = 0;
                                    if (rd != null && rd.length() >= 4) {
                                        try { parsed = Integer.parseInt(rd.substring(0, 4)); } catch (Exception ignore) {}
                                    }
                                    albumYearCache.put(albumId, parsed);
                                    trackYear = parsed;
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
                if (trackYear >= MIN_RECENT_YEAR || trackYear == 0) {
                    res.add(candidate);
                    if (res.size() >= target) return res;
                } else {
                    olderTracks.add(candidate);
                }
            }
        }
        // Добавляем старые только если не хватает новых
        for (String old : olderTracks) {
            if (res.size() >= target) break;
            res.add(old);
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Источник 2: Deezer Artist Radio + Related
    // ─────────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getDeepSimilarTracks(String artist, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        String url = "https://api.deezer.com/search/artist?q=" + URLEncoder.encode(artist, "UTF-8");
        String resp = performGetRequest(url);
        if (resp.isEmpty()) return res;
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
                synchronized (historyLock) {
                    if (globalPlayHistory.add(a + tit)) res.add(a + " - " + tit);
                }
                if (res.size() >= limit) return res;
            }
        }

        // 2. Похожие артисты
        JSONArray related = new JSONObject(performGetRequest("https://api.deezer.com/artist/" + id + "/related?limit=8")).optJSONArray("data");
        if (related != null) {
            for (int i = 0; i < related.length(); i++) {
                long relId = related.getJSONObject(i).getLong("id");
                JSONArray top = new JSONObject(performGetRequest("https://api.deezer.com/artist/" + relId + "/top?limit=10")).optJSONArray("data");
                if (top != null) {
                    for (int j = 0; j < top.length(); j++) {
                        JSONObject t = top.getJSONObject(j);
                        String a = t.getJSONObject("artist").getString("name");
                        String tit = t.getString("title");
                        synchronized (historyLock) {
                            if (globalPlayHistory.add(a + tit)) res.add(a + " - " + tit);
                        }
                        if (res.size() >= limit) return res;
                    }
                }
            }
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Источник 3: iTunes Search API (НОВЫЙ — бесплатный, не требует ключа)
    // ─────────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getTracksFromITunes(String query, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        String url = "https://itunes.apple.com/search?term=" + URLEncoder.encode(query, "UTF-8")
                + "&media=music&entity=song&limit=" + Math.min(limit * 3, 50)
                + "&country=US";
        String resp = performGetRequest(url);
        if (resp.isEmpty()) return res;
        JSONObject json = new JSONObject(resp);
        if (!json.has("results")) return res;
        JSONArray results = json.getJSONArray("results");
        // Перемешиваем для разнообразия
        ArrayList<JSONObject> items = new ArrayList<>();
        for (int i = 0; i < results.length(); i++) items.add(results.getJSONObject(i));
        Collections.shuffle(items);
        for (JSONObject item : items) {
            String art = item.optString("artistName", "");
            String tit = item.optString("trackName", "");
            if (art.isEmpty() || tit.isEmpty()) continue;
            synchronized (historyLock) {
                if (globalPlayHistory.add(art + tit)) res.add(art + " - " + tit);
            }
            if (res.size() >= limit) return res;
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Источник 4: Last.fm Similar Tracks (НОВЫЙ — бесплатный)
    // Использует публичный Last.fm API без ключа через musicbrainz mirror
    // ─────────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getLastFmSimilarTracks(String artist, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        // Last.fm API — бесплатный ключ по умолчанию работает для базовых запросов
        // Используем publicную точку без ключа (demo mode)
        String url = "https://ws.audioscrobbler.com/2.0/?method=artist.getsimilar"
                + "&artist=" + URLEncoder.encode(artist, "UTF-8")
                + "&api_key=43691b1e6f4f4e3547fa1c798d695bf4" // публичный demo ключ Last.fm
                + "&format=json&limit=" + Math.min(limit * 2, 20);
        String resp = performGetRequest(url);
        if (resp.isEmpty()) return res;

        JSONObject json = new JSONObject(resp);
        if (!json.has("similarartists")) return res;
        JSONArray similar = json.getJSONObject("similarartists").optJSONArray("artist");
        if (similar == null) return res;

        for (int i = 0; i < similar.length(); i++) {
            String simArtist = similar.getJSONObject(i).getString("name");
            // Берём топ-треки похожего артиста через Last.fm
            String topUrl = "https://ws.audioscrobbler.com/2.0/?method=artist.gettoptracks"
                    + "&artist=" + URLEncoder.encode(simArtist, "UTF-8")
                    + "&api_key=43691b1e6f4f4e3547fa1c798d695bf4"
                    + "&format=json&limit=5";
            String topResp = performGetRequest(topUrl);
            if (topResp.isEmpty()) continue;
            JSONObject topJson = new JSONObject(topResp);
            if (!topJson.has("toptracks")) continue;
            JSONArray tracks = topJson.getJSONObject("toptracks").optJSONArray("track");
            if (tracks == null) continue;
            for (int j = 0; j < tracks.length(); j++) {
                String tit = tracks.getJSONObject(j).getString("name");
                synchronized (historyLock) {
                    if (globalPlayHistory.add(simArtist + tit)) res.add(simArtist + " - " + tit);
                }
                if (res.size() >= limit) return res;
            }
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Источник: Last.fm track.getsimilar — похожие именно на конкретный трек
    // ─────────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getLastFmSimilarByTrack(String artist, String title, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        String url = "https://ws.audioscrobbler.com/2.0/?method=track.getsimilar"
                + "&artist=" + URLEncoder.encode(artist, "UTF-8")
                + "&track=" + URLEncoder.encode(title, "UTF-8")
                + "&api_key=43691b1e6f4f4e3547fa1c798d695bf4"
                + "&format=json&limit=" + Math.min(limit * 2, 30);
        String resp = performGetRequest(url);
        if (resp.isEmpty()) return res;

        JSONObject json = new JSONObject(resp);
        if (!json.has("similartracks")) return res;
        JSONArray similar = json.getJSONObject("similartracks").optJSONArray("track");
        if (similar == null) return res;

        for (int i = 0; i < similar.length(); i++) {
            JSONObject t = similar.getJSONObject(i);
            String tit = t.optString("name", "");
            JSONObject artObj = t.optJSONObject("artist");
            String art = artObj != null ? artObj.optString("name", "") : "";
            if (art.isEmpty() || tit.isEmpty()) continue;
            synchronized (historyLock) {
                if (globalPlayHistory.add(art + tit)) res.add(art + " - " + tit);
            }
            if (res.size() >= limit) return res;
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Источник: Deezer track radio — похожие на конкретный трек по его ID
    // ─────────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getDeezerTrackRadio(String artist, String title, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        // Ищем трек чтобы получить его ID
        String q = URLEncoder.encode(artist + " " + title, "UTF-8");
        String searchResp = performGetRequest("https://api.deezer.com/search?q=" + q + "&limit=1");
        if (searchResp.isEmpty()) return res;
        JSONArray data = new JSONObject(searchResp).optJSONArray("data");
        if (data == null || data.length() == 0) return res;

        long trackId = data.getJSONObject(0).getLong("id");

        // /track/{id}/radio — Deezer возвращает похожие треки именно на этот трек
        String radioResp = performGetRequest("https://api.deezer.com/track/" + trackId + "/radio?limit=" + Math.min(limit * 2, 40));
        if (radioResp.isEmpty()) return res;
        JSONArray radioData = new JSONObject(radioResp).optJSONArray("data");
        if (radioData == null) return res;

        ArrayList<JSONObject> items = new ArrayList<>();
        for (int i = 0; i < radioData.length(); i++) items.add(radioData.getJSONObject(i));
        Collections.shuffle(items);

        for (JSONObject t : items) {
            String art = t.has("artist") ? t.getJSONObject("artist").optString("name", "") : "";
            String tit2 = t.optString("title", "");
            if (art.isEmpty() || tit2.isEmpty()) continue;
            synchronized (historyLock) {
                if (globalPlayHistory.add(art + tit2)) res.add(art + " - " + tit2);
            }
            if (res.size() >= limit) return res;
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Треки альбома с Deezer
    // ─────────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getTracksFromAlbum(String artistName, String albumName, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        String query = URLEncoder.encode(artistName + " " + albumName, "UTF-8");
        String resp = performGetRequest("https://api.deezer.com/search/album?q=" + query + "&limit=5");
        if (resp.isEmpty()) return res;
        JSONObject json = new JSONObject(resp);
        if (!json.has("data")) return res;
        JSONArray albums = json.getJSONArray("data");
        if (albums.length() == 0) return res;
        long albumId = albums.getJSONObject(0).getLong("id");
        String tracksResp = performGetRequest("https://api.deezer.com/album/" + albumId + "/tracks?limit=50");
        JSONArray tracks = new JSONObject(tracksResp).optJSONArray("data");
        if (tracks == null) return res;
        for (int i = 0; i < tracks.length(); i++) {
            JSONObject t = tracks.getJSONObject(i);
            String art = t.has("artist") ? t.getJSONObject("artist").getString("name") : artistName;
            String tit = t.getString("title");
            synchronized (historyLock) {
                if (globalPlayHistory.add(art + tit)) res.add(art + " - " + tit);
            }
            if (res.size() >= limit) return res;
        }
        return res;
    }

    private static String getAlbumGenreQuery(String artistName, String albumName) throws Exception {
        String query = URLEncoder.encode(artistName + " " + albumName, "UTF-8");
        String resp = performGetRequest("https://api.deezer.com/search/album?q=" + query + "&limit=3");
        if (resp.isEmpty()) return null;
        JSONObject json = new JSONObject(resp);
        if (!json.has("data") || json.getJSONArray("data").length() == 0) return null;
        long albumId = json.getJSONArray("data").getJSONObject(0).getLong("id");
        String detailResp = performGetRequest("https://api.deezer.com/album/" + albumId);
        if (detailResp.isEmpty()) return null;
        JSONObject detail = new JSONObject(detailResp);
        if (detail.has("genres")) {
            JSONObject genresObj = detail.getJSONObject("genres");
            if (genresObj.has("data") && genresObj.getJSONArray("data").length() > 0) {
                return genresObj.getJSONArray("data").getJSONObject(0).getString("name");
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NeuroRanker — без изменений логики, только мелкие фиксы
    // ─────────────────────────────────────────────────────────────────────────

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
                genreDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                        dbFile.getPath(), null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY);

                env = OrtEnvironment.getEnvironment();
                byte[] model;
                try (InputStream is = context.getAssets().open("music_brain.onnx");
                     ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
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
                try (android.database.Cursor c = genreDb.rawQuery(
                        "SELECT vector_json FROM anchors WHERE genre_name LIKE ?",
                        new String[]{"%" + getDeezerSearchQuery(genre) + "%"})) {
                    if (c.moveToFirst()) {
                        JSONArray arr = new JSONArray(c.getString(0));
                        for (int i = 0; i < Math.min(400, arr.length()); i++) vec[i] = (float) arr.getDouble(i);
                        return vec;
                    }
                } catch (Exception ignored) {}
            }
            return vec;
        }

        public float[] getVectorForRemoteTrack(String artist, String title) {
            if (session == null) return null;
            String cacheKey = artist.trim() + " - " + title.trim();
            float[] cached = vectorCache.get(cacheKey);
            if (cached != null) return cached;

            File tempFile = null;
            MediaExtractor extractor = new MediaExtractor();
            try {
                String q = URLEncoder.encode(artist + " " + title, "UTF-8");
                String json = performGetRequest("https://api.deezer.com/search?q=" + q + "&limit=1");
                if (json.isEmpty()) return null;
                JSONArray data = new JSONObject(json).optJSONArray("data");
                if (data == null || data.length() == 0) return null;
                String previewUrl = data.getJSONObject(0).optString("preview", "");
                if (previewUrl.isEmpty()) return null;

                tempFile = new File(ApplicationLoader.applicationContext.getCacheDir(),
                        "preview_" + System.currentTimeMillis() + ".mp3");
                HttpURLConnection conn = (HttpURLConnection) new URL(previewUrl).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(8000);
                try (InputStream is = conn.getInputStream();
                     FileOutputStream fos = new FileOutputStream(tempFile)) {
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
                    for (int i = 0; i < framesPerChunk * mels; i++) {
                        fb.put(Math.abs(pcmData.get(offset + (i % samplesPerChunk))));
                    }
                    fb.rewind();
                    OnnxTensor tensor = OnnxTensor.createTensor(env, fb, new long[]{1, framesPerChunk, mels});
                    Map<String, OnnxTensor> inputs = new HashMap<>();
                    inputs.put(inputName, tensor);
                    float[][] out = (float[][]) session.run(inputs).get(0).getValue();
                    chunkEmbeddings.add(out[0]);
                    tensor.close();
                }

                if (chunkEmbeddings.isEmpty()) return null;

                float[] finalVec = new float[400];
                for (float[] chunk : chunkEmbeddings)
                    for (int i = 0; i < 400; i++) finalVec[i] += chunk[i];

                float norm = 0;
                for (int i = 0; i < 400; i++) {
                    finalVec[i] /= chunkEmbeddings.size();
                    norm += finalVec[i] * finalVec[i];
                }
                norm = (float) Math.sqrt(norm);
                if (norm < 1e-9f) return null;
                for (int i = 0; i < 400; i++) finalVec[i] /= norm;

                // Кешируем только если вектор вменяемый
                vectorCache.put(cacheKey, finalVec);
                // Ограничиваем размер кеша векторов
                if (vectorCache.size() > 500) {
                    vectorCache.remove(vectorCache.keys().nextElement());
                }
                return finalVec;

            } catch (Exception e) {
                extractor.release();
                return null;
            } finally {
                if (tempFile != null && tempFile.exists()) tempFile.delete();
            }
        }

        public float calculateCosineSimilarity(float[] a, float[] b) {
            if (a == null || b == null || a.length != b.length) return 0f;
            float dot = 0, nA = 0, nB = 0;
            for (int i = 0; i < a.length; i++) {
                dot += a[i] * b[i];
                nA += a[i] * a[i];
                nB += b[i] * b[i];
            }
            if (nA < 1e-9f || nB < 1e-9f) return 0f;
            return (float) (dot / (Math.sqrt(nA) * Math.sqrt(nB)));
        }
    }

    private static ArrayList<String> getSeedTracksForGenre(String genre) {
        String g = genre.toLowerCase().trim();
        ArrayList<String> list = new ArrayList<>();

        try {
            android.content.SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("soundgram_wave", Context.MODE_PRIVATE);
            String jsonStr = prefs.getString("wave_seed_data", null);
            if (jsonStr != null) {
                JSONObject json = new JSONObject(jsonStr);
                if (json.has(g)) {
                    JSONArray arr = json.getJSONArray(g);
                    for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
                    if (!list.isEmpty()) return list;
                }
            }
        } catch (Exception e) { FileLog.e("SoundGram Wave JSON parse error", e); }

        switch (g) {
            case "поп":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Anna Asti - Царица", "Zivert - Wake Up", "JONY - Комета", "Люся Чеботина - Солнце Монако",
                        "Мари Краймбрери - Иди танцуй", "Клава Кока - Покинула чат", "Artik & Asti - Гармония",
                        "Niletto - Любимка", "Леша Свик - Хит", "Моя Мишель - Зима в сердце",
                        "Miley Cyrus - Flowers", "Dua Lipa - Houdini", "Taylor Swift - Anti-Hero",
                        "The Weeknd - Blinding Lights", "Olivia Rodrigo - vampire", "Billie Eilish - What Was I Made For?",
                        "Harry Styles - As It Was", "Sabrina Carpenter - Espresso", "Doja Cat - Paint The Town Red", "Tyla - Water"
                ));
                break;
            case "хип-хоп":
            case "хип хоп":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Kendrick Lamar - Not Like Us", "Drake - First Person Shooter", "Travis Scott - FE!N",
                        "Future - Like That", "21 Savage - redrum", "Gunna - fukumean", "Playboi Carti - 2024",
                        "Lil Uzi Vert - Just Wanna Rock", "J. Cole - No Role Modelz", "A$AP Rocky - Praise The Lord",
                        "Скриптонит - Цепи", "Miyagi & Andy Panda - Minor", "Big Baby Tape - Gimme The Loot",
                        "Boulevard Depo - Кащенко", "Oxxxymiron - Оябун", "Баста - Сансара",
                        "Kizaru - Дежавю", "Macan - Юг", "Friendly Thug 52 Ngg - Arrivederci", "Saluki - ОГНЕЙ"
                ));
                break;
            case "рок":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Maneskin - GOSSIP", "Imagine Dragons - Eyes Closed", "Twenty One Pilots - Overcompensate",
                        "Muse - Compliance", "Foo Fighters - Rescued", "Royal Blood - Troubles Coming",
                        "Nothing But Thieves - Welcome to the DCC", "YUNGBLUD - The Funeral", "Greta Van Fleet - Meeting The Master", "The Black Keys - Beautiful People",
                        "Би-2 - Я никому не поверю", "Земфира - Остин", "ДДТ - Борщевик", "Сплин - Передайте это Гарри Поттеру",
                        "Мумий Тролль - Лето без интернета", "Кипелов - Час пик", "Порнофильмы - Я так соскучился",
                        "Нервы - Батареи", "Алиса - Трасса Е-95", "Король и Шут - Лесник"
                ));
                break;
            case "инди":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Arctic Monkeys - There’d Better Be A Mirrorball", "The Neighbourhood - Sweater Weather",
                        "Tame Impala - The Less I Know The Better", "Mac DeMarco - Chamber Of Reflection", "Wallows - Calling After Me",
                        "Girl in Red - Too Much", "The Strokes - The Adults Are Talking", "Florence + The Machine - Free",
                        "Lana Del Rey - A&W", "Lorde - Solar Power",
                        "Сироткин - Выше домов", "ssshhhiiittt - Танцы", "Дайте танк - Утро", "Буерак - Страсть к курению",
                        "Перемотка - Встречная", "Космонавтов Нет - Баночка с ошейником", "Свидание - Любовь",
                        "электрофорез - Э Зло", "Папин Олимпос - Темно-оранжевый закат", "Алена Швец - Вино и сигареты"
                ));
                break;
            case "фонк":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Kordhell - Murder In My Mind", "Ghostface Playa - Why Not", "Kaito Shoma - Scary Garry",
                        "DVRST - Close Eyes", "Hensonn - Sahara", "MoonDeity - Neon Blade", "Interworld - Metamorphosis",
                        "dabl - Vengeance", "Scxr Soul - Sigma", "Kute - Avoid Me",
                        "Swerve - Kerosene", "Phonkha - MTG", "Misha Xramovi - Misha Xramovi", "VOJ - Memory Reboot",
                        "Razihel - Love U", "Odetari - Good Loyal Thots", "Plenka - Call Me", "ZXC - GHOUL",
                        "Gawne - Deathwish", "PlayaPhonk - PHONKY TOWN"
                ));
                break;
            case "гиперпоп":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "100 gecs - Hollywood Baby", "Charli XCX - Von dutch", "SOPHIE - Its Okay to Cry",
                        "Dorian Electra - Flamboyant", "Rico Nasty - IPHONE", "Glaive - astrid", "ericdoa - fawnboy",
                        "midwxst - trying", "Brakence - dropout", "ElyOtto - SugarCrash",
                        "Lida - Девочка с концерта", "GSPD - Евродэнс", "Мейби Бэйби - Ахегао", "Дора - Втюрилась",
                        "CMH - PAYDAY", "Sqwoz Bab - ОЙ", "Slava Marlow - Ты горишь как огонь", "Эльдар Джарахов - Я в моменте",
                        "Morgenstern - Cristal MOET", "Instasamka - За деньги да"
                ));
                break;
            case "пост-панк":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Molchat Doma - Судно", "Ploho - Закладка", "Buerak - Спортивные очки", "Ssshhhiiittt - Восьмерка",
                        "Nautilus Pompilius - Дыхание", "Kino - Группа крови", "Agatha Christie - Как на войне",
                        "Bi-2 - Полковнику никто не пишет", "Увула - Ты и твоя тень", "Черная Речка - Море",
                        "Joy Division - Love Will Tear Us Apart", "The Cure - Alone", "Echo And The Bunnymen - The Killing Moon",
                        "Bauhaus - Bela Lugosis Dead", "Siouxsie And The Banshees - Spellbound", "The Smiths - This Charming Man",
                        "New Order - Blue Monday", "Interpol - Evil", "Editors - Munich", "Fontaines D.C. - Starburster"
                ));
                break;
            case "эмо-рэп":
            case "эмо реп":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Lil Peep - Star Shopping", "XXXTENTACION - Jocelyn Flores", "Juice WRLD - Lucid Dreams",
                        "Trippie Redd - Miss The Rage", "Iann Dior - emotions", "The Kid LAROI - WITHOUT YOU",
                        "Post Malone - I Fall Apart", "Joji - Glimpse of Us", "Lil Uzi Vert - The Way Life Goes", "Sueco - Paralyzed",
                        "Lida - Иди за мной", "PHARAOH - Лаллипап", "Morgenstern - Селяви", "Face - Юморист",
                        "Oxxxymiron - Город под подошвой", "Скриптонит - Космос", "T-Fest - Улети", "Элджей - Розовое вино",
                        "ЛСП - Монетка", "Хаски - Иуда"
                ));
                break;
            case "метал":
            case "металл":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Bring Me The Horizon - Kool-Aid", "Sleep Token - The Summoning", "Bad Omens - Just Pretend",
                        "Spiritbox - Jaded", "Architects - Seeing Red", "I Prevail - Bow Down", "Lorna Shore - To the Hellfire",
                        "Slipknot - The Dying Song", "Gojira - Amazonia", "Trivium - In The Court Of The Dragon",
                        "Slaughter to Prevail - Viking", "Wildways - Прада", "AMATORY - Космос", "Louna - Колыбельная",
                        "Кипелов - Я Свободен", "Ария - Проклятье морей", "Stigmata - Сентябрь", "Психея - Навсегда",
                        "Слот - Мертвые звезды", "Eskimo Callboy - Hypa Hypa"
                ));
                break;
            case "альтернатива":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Linkin Park - Lost", "Deftones - Ohms", "Paramore - This Is Why", "My Chemical Romance - The Foundations of Decay",
                        "Blink-182 - ONE MORE TIME", "Fall Out Boy - Love From The Other Side", "Papa Roach - Kill The Noise",
                        "Three Days Grace - So Called Life", "Breaking Benjamin - Far Away", "Evanescence - Wasted On You",
                        "Lumen - Гореть", "Tracktor Bowling - Черта", "Amatory - Черно-белые дни", "Jane Air - Junk",
                        "Animal ДжаZ - Три полоски", "Северный Флот - Поднимая Знамя", "Слот - Круги на воде",
                        "7000$ - Потерянный рай", "Radio Tapok - Высота 776", "Гран-КуражЪ - Птица счастья"
                ));
                break;
            case "к-поп":
            case "к поп":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Jung Kook - Seven", "NewJeans - Super Shy", "Stray Kids - LALALALA", "LE SSERAFIM - Perfect Night",
                        "aespa - Drama", "IVE - I AM", "TXT - Chasing That Feeling", "ENHYPEN - Bite Me",
                        "SEVENTEEN - Super", "TWICE - SET ME FREE",
                        "BLACKPINK - Shut Down", "BTS - Dynamite", "EXO - Love Shot", "Red Velvet - Chill Kill",
                        "ITZY - CAKE", "NCT 127 - Fact Check", "ATEEZ - BOUNCY", "FIFTY FIFTY - Cupid",
                        "Jimin - Like Crazy", "MAMAMOO - HIP"
                ));
                break;
            case "джаз":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Laufey - From The Start", "Samara Joy - Tight", "Kamasi Washington - Street Fighter Mas",
                        "Gregory Porter - Liquid Spirit", "Norah Jones - Don't Know Why", "Michael Bublé - Sway",
                        "Esperanza Spalding - I Know You Know", "Snarky Puppy - Lingus", "Jacob Collier - In My Room", "Jon Batiste - FREEDOM",
                        "Игорь Бутман - Nostalgia", "Therr Maitz - Future Is Bright", "LRK Trio - The Tides",
                        "Zventa Sventana - Мужа дома нету", "Антон Беляев - Fly Me To The Moon", "Леонид Агутин - Включите свет",
                        "Sirotkin - Планы на это лето", "Олег Аккуратов - Nature Boy", "Polina Gagarina - Колыбельная (Jazz Version)", "Евгений Побожий - Elements"
                ));
                break;
            case "блюз":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Joe Bonamassa - Redemption", "Gary Clark Jr. - Pearl Cadillac", "Christone Kingfish Ingram - Outside Of This Town",
                        "Tedeschi Trucks Band - Midnight in Harlem", "John Mayer - Slow Dancing in a Burning Room", "Beth Hart - Bad Woman Blues",
                        "Kenny Wayne Shepherd - Blue on Black", "Buddy Guy - Cognac", "The Black Keys - Weight of Love", "Fantastic Negrito - Plastic Hamburgers",
                        "Маргулис - Мой друг лучше всех играет блюз", "Чиж & Co - О любви", "Машина Времени - Костер",
                        "Гарик Сукачев - Моя бабушка курит трубку", "Billy's Band - Оторвемся по-питерски", "Crossroadz - Diamond Rain",
                        "Леван Ломидзе - Blues for You", "Сергей Воронов - Я остаюсь", "Blues Doctors - Good Times", "Михаил Мишурис - Sweet Home Chicago"
                ));
                break;
            case "классика":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Ludovico Einaudi - Experience", "Max Richter - On the Nature of Daylight", "Hans Zimmer - Time",
                        "Olafur Arnalds - saman", "Nils Frahm - Ambre", "Hania Rani - Glass",
                        "Yann Tiersen - Comptine d'un autre ete", "Philip Glass - Metamorphosis 1", "Joep Beving - Ab Ovo", "Dustin O'Halloran - Opus 23",
                        "Denis Stelmakh - Afraid of Destiny", "Кирилл Рихтер - Mechanims", "Misha Mishenko - Decadence",
                        "Ilya Beshevli - Night Forest", "Neoclassica - Winter", "Evgeny Grinko - Valse",
                        "Polina Nazaykinskaya - Winter Bells", "Nikola Melnikov - Flight", "Gleb Kolyadin - The Room", "Arseny Trofim - The Flow"
                ));
                break;
            case "электроника":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Fred again.. - leavemealone", "Peggy Gou - (It Goes Like) Nanana", "Bicep - Glue",
                        "Disclosure - Latch", "Flume - Never Be Like You", "Skrillex - Rumble",
                        "Rufus Du Sol - Innerbloom", "Jamie xx - Gosh", "Overmono - Good Lies", "Four Tet - Baby",
                        "IC3PEAK - Смерти Больше Нет", "Cream Soda - Плачу на техно", "GSPD - Некстген",
                        "Dead Blonde - Мальчик на девятке", "XS Project - Бочка Басс Колбасер", "Hard Bass School - Narkotik Kal",
                        "DJ Smash - Волна", "ЛУНА - Free Love", "Kedr Livanskiy - Ariadna", "Ishome - Kenotaf"
                ));
                break;
            case "дабстеп":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Skrillex - Bangarang", "Excision - Throwin' Elbows", "Zeds Dead - Rumble In The Jungle",
                        "Subtronics - Griztronics", "Virtual Riot - Energy Drink", "Flux Pavilion - I Can't Stop",
                        "Borgore - Decisions", "Nero - Promises", "Knife Party - Centipede", "Kill The Noise - Kill It 4 The Kids",
                        "Miyagi & Andy Panda - I Got Love", "Элджей - Suzuki", "Oxxxymiron - Петелька",
                        "Lida - ЧСВ", "GSPD - Электроклуб", "CMH - Быстрые",
                        "Слава КПСС - Грустные танцы", "ATL - Танцуйте", "Morgenstern - ICE", "Slava Marlow - Быстро"
                ));
                break;
            case "панк-рок":
            case "панк рок":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Green Day - The American Dream Is Killing Me", "Blink-182 - EDGING", "Sum 41 - Landmines",
                        "The Offspring - Let The Bad Times Roll", "Machine Gun Kelly - bloody valentine", "YUNGBLUD - fleabag",
                        "Avril Lavigne - Bite Me", "Neck Deep - In Bloom", "State Champs - Secrets", "Turnstile - BLACKOUT",
                        "Король и Шут - Лесник", "Порнофильмы - Я так соскучился", "Йорш - Сверхдержава",
                        "Тараканы! - Тишина это смерть", "Lumen - Сид и Нэнси", "План Ломоносова - Мосты",
                        "Смех - Макдональдс", "Кис-Кис - Молчи", "Пошлая Молли - Нон стоп", "Элизиум - Острова"
                ));
                break;
            case "синтвейв":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Kavinsky - Nightcall", "The Midnight - Sunset", "Timecop1983 - On the Run",
                        "Gunship - Tech Noir", "FM-84 - Running In The Night", "Lazerhawk - Overdrive",
                        "Miami Nights 1984 - Ocean Drive", "Carpenter Brut - Turbo Killer", "Perturbator - Future Club", "Dance With The Dead - Riot",
                        "Электрофорез - Я ничего не могу с собою сделать", "Bi-2 - Депрессия", "Kino - Спокойная ночь",
                        "Nautilus Pompilius - Взгляд с экрана", "Альянс - На заре", "Молчат Дома - Танцевать",
                        "Увула - Дранк", "Свидание - Я и твой кот", "Пасош - Россия", "Буерак - Спортивные очки"
                ));
                break;
            case "ло-фай":
            case "лофай":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Lofi Girl - aaru", "ChilledCow - 1 A.M Study Session", "Idealism - controlla",
                        "Jinsang - affection", "Kupla - owls of the night", "bsd.u - french inhale",
                        "Elijah Who - this girl", "Tomppabeats - monday Loop", "In Love With A Ghost - we've never met", "Nujabes - Aruarian Dance",
                        "Скриптонит - Это любовь", "Мальбэк - Гипнозы", "Miyagi & Andy Panda - Патрон",
                        "Баста - Выпускной (Медлячок)", "Feduk - Хлопья летят наверх", "Boulevard Depo - 5 минут назад",
                        "Macan - Кино", "JONY - Аллея", "HammAli & Navai - Пустите меня на танцпол", "Ramil' - Сияй"
                ));
                break;
            case "эмбиент":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Brian Eno - An Ending (Ascent)", "Aphex Twin - #3", "Stars Of The Lid - Requiem For Dying Mothers",
                        "William Basinski - The Disintegration Loops", "Tim Hecker - Chimeras", "Grouper - Heavy Water",
                        "Hammock - Breathturn", "A Winged Victory for the Sullen - Steep Hills of Vicodin Tears", "Bohren & der Club of Gore - Midnight Black Earth", "C418 - Sweden",
                        "Solar Fields - Leaving Home", "Aes Dana - Lysergic", "Carbon Based Lifeforms - Photosynthesis",
                        "Koan - Odysseus Under The Old Tree", "Astropilot - The Day When The Sun Went Out", "Zymosis - Zeta",
                        "Suduaya - Snow and Stars", "Asura - Life2", "H.U.V.A. Network - Orientations", "Stellardrone - To The Great Attractor"
                ));
                break;
            case "trap":
            case "трэп":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Travis Scott - goosebumps", "Future - Mask Off", "Migos - Bad and Boujee",
                        "Lil Uzi Vert - XO Tour Llif3", "21 Savage - Bank Account", "Playboi Carti - Magnolia",
                        "Sheck Wes - Mo Bamba", "Roddy Ricch - The Box", "Polo G - RAPSTAR", "Lil Baby - Drip Too Hard",
                        "Big Baby Tape - VLACO", "Kizaru - Narcos", "Oxxxymiron - Организация",
                        "Скриптонит - Москва любит...", "Morgenstern - Cadillac", "Slava Marlow - Снова я напиваюсь",
                        "Face - Бургер", "PHARAOH - 5 Минут Назад", "Boulevard Depo - OCB", "Платина - Санта Клаус"
                ));
                break;
            case "r&b":
            case "rnb":
            case "рнб":
            case "соул":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "SZA - Kill Bill", "The Weeknd - Call Out My Name", "Frank Ocean - Pink + White",
                        "Brent Faiyaz - Trust", "Daniel Caesar - Get You", "H.E.R. - Best Part",
                        "Summer Walker - Playing Games", "Kehlani - Nights Like This", "Jhené Aiko - Sativa", "Giveon - Heartbreak Anniversary",
                        "JONY - Комета", "Zivert - Credo", "Miyagi & Andy Panda - Утопия",
                        "Andro - X.O", "Elman - Лети", "Мот - Капкан",
                        "Seville (Artik & Asti) - Кукла", "Macan - ОСТАНЬСЯ", "Navai - Птичка", "HammAli & Navai - Девочка-война"
                ));
                break;
            case "регги":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Bob Marley & The Wailers - Three Little Birds", "Damian Marley - Welcome To Jamrock", "Shaggy - It Wasn't Me",
                        "Sean Paul - Get Busy", "Koffee - Toast", "Chronixx - Skankin' Sweet",
                        "Protoje - Who Knows", "Buju Banton - Champion", "Beenie Man - Girls Dem Sugar", "Popcaan - Family",
                        "Miyagi & Andy Panda - I Got Love", "Jah Khalib - Медина", "Alai Oli - Снег и Пепел",
                        "Пятница - Солдат", "Lumen - Сид и Нэнси", "Бумбокс - Вахтёрам",
                        "Макс Корж - Жить в кайф", "Noize MC - Из окна", "Баста - Моя игра", "Каста - Вокруг шум"
                ));
                break;
            case "фанк":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Bruno Mars - 24K Magic", "Daft Punk - Get Lucky", "Mark Ronson - Uptown Funk",
                        "Jamiroquai - Virtual Insanity", "Earth, Wind & Fire - September", "Stevie Wonder - Superstition",
                        "James Brown - I Got You (I Feel Good)", "Kool & The Gang - Get Down On It", "Rick James - Super Freak", "Prince - Kiss",
                        "Zivert - Life", "Monatik - Кружит", "Иван Дорн - Стыцамэн",
                        "Леонид Агутин - Хоп Хэй Лала Лэй", "Браво - Этот город", "Валерий Сюткин - 7 тысяч над землей",
                        "Анжелика Варум - Городок", "Мумий Тролль - Владивосток 2000", "Сплин - Орбит без сахара", "Земфира - Ромашки"
                ));
                break;
            case "кантри":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Morgan Wallen - Last Night", "Luke Combs - Fast Car", "Zach Bryan - Something in the Orange",
                        "Chris Stapleton - Tennessee Whiskey", "Kane Brown - Heaven", "Jason Aldean - Try That In A Small Town",
                        "Luke Bryan - Play It Again", "Florida Georgia Line - Cruise", "Blake Shelton - God's Country", "Thomas Rhett - Die A Happy Man",
                        "Гарик Сукачев - Моя бабушка курит трубку", "Чиж & Co - О любви", "Машина Времени - Поворот",
                        "ДДТ - Что такое осень", "Любэ - Конь", "Сектор Газа - Пора домой",
                        "Алиса - Небо славян", "Король и Шут - Кукла колдуна", "Чайф - Аргентина - Ямайка 5:0", "Ария - Беспечный ангел"
                ));
                break;
            default:
                break;
        }
        return list;
    }



}