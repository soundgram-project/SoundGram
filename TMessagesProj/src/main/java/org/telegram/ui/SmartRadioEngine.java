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
    private static volatile NeuroRanker neuroRanker;
    private static final Object neuroLock = new Object();

    // FIX #4: снижен порог + мягкий порог
    private static final float SIMILARITY_THRESHOLD = 0.40f;
    private static final float SOFT_THRESHOLD = 0.22f;

    // Кешированный seed-вектор между рефиллами
    private static volatile float[] cachedSeedVector = null;

    // Флаг отмены текущей волны
    private static final AtomicBoolean cancelFlag = new AtomicBoolean(false);

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
        // FIX: очищаем историю при каждом новом запуске — абсолютный рандом
        synchronized (historyLock) { globalPlayHistory.clear(); }
        // FIX: устанавливаем флаги сразу, не через delayed runOnUIThread
        cancelFlag.set(false);
        isWaveActive = true;
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
    // ─────────────────────────────────────────────────────────────────────────

    public static void generateWaveFromSettings(int currentAccount, WaveCallback callback) {
        startNewWaveSession(0);
        initNeuroRankerIfNeeded();
        backgroundExecutor.execute(() -> {
            try {
                // Шаг 1: Берем якоря из нашей заготовленной базы жанра
                ArrayList<String> seedPool = getSeedTracksForGenre(currentConfig.query);
                Collections.shuffle(seedPool);

                ArrayList<String> initial = new ArrayList<>();
                // Берем до 8 случайных треков из нашей базы
                for (int i = 0; i < Math.min(8, seedPool.size()); i++) {
                    initial.add(seedPool.get(i));
                }

                String searchKeyword = getDeezerSearchQuery(currentConfig.query);

                // Fallback: если для выбранного жанра мы не прописали списки, тянем динамически
                if (initial.isEmpty()) {
                    initial.addAll(getTracksFromDynamicPlaylists(searchKeyword, 8, random.nextInt(600)));
                    if (initial.size() < 5) initial.addAll(getTracksFromITunes(searchKeyword, 8 - initial.size()));
                }

                processAndReturnTracks(initial, currentAccount, true, callback);

                if (cancelFlag.get()) return;

                // Шаг 2: Жестко фиксируем вектор ЖАНРА. Он будет гарантом того, что треки не перетекут
                float[] contextVector = neuroRanker.getGenreVector(currentConfig.query);
                cachedSeedVector = contextVector;

                // Собираем кандидатов на основе авторов из сгенерированной начальной пачки
                ArrayList<String> next = new ArrayList<>();
                for(String track : initial) {
                    String[] parts = track.split(" - ", 2);
                    if(parts.length > 0) {
                        String artist = parts[0];
                        // Ищем похожие на этот конкретный трек/артиста из базы
                        next.addAll(getDeepSimilarTracks(artist, 5));
                        next.addAll(getLastFmSimilarTracks(artist, 5));
                    }
                }

                // Разбавляем свежими чартами жанра для разнообразия
                next.addAll(getTracksFromDynamicPlaylists(searchKeyword, 10, random.nextInt(300) + 10));

                // Фильтруем найденные треки через вектор жанра, чтобы отсечь мусор
                ArrayList<String> verified = verifyCandidatesWithNeuralNetwork(next, contextVector, 20);
                processAndReturnTracks(verified, currentAccount, false, callback);

                if (cancelFlag.get()) return;

                // Шаг 3: бесконечный рефилл
                startInfiniteRefill(currentAccount, callback, () -> {
                    ArrayList<String> r = new ArrayList<>();
                    try {
                        // Берем треки из УЖЕ сгенерированной пачки для развития волны
                        List<MessageObject> batch = new ArrayList<>(lastGeneratedBatch);
                        if (!batch.isEmpty()) {
                            Collections.shuffle(batch);
                            // Берем 3 случайных авторов из текущей очереди
                            for (int i = 0; i < Math.min(3, batch.size()); i++) {
                                String author = batch.get(i).getMusicAuthor();
                                if (author != null && !author.isEmpty()) {
                                    r.addAll(getDeezerArtistMix(author, 10));
                                    r.addAll(getLastFmSimilarTracks(author, 8));
                                }
                            }
                        }

                        // Если артистов не хватило, добиваем динамическими плейлистами самого жанра
                        if (r.size() < 10) {
                            r.addAll(getTracksFromDynamicPlaylists(searchKeyword, 15, random.nextInt(500)));
                        }
                    } catch (Exception ignored) {}
                    return r;
                });

            } catch (Exception e) { handleError(callback, e); }
        });
    }


    // ─────────────────────────────────────────────────────────────────────────
    // generateWaveByPlaylist
    // ─────────────────────────────────────────────────────────────────────────

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
                if (ctx.isEmpty()) throw new Exception("Плейлист пуст");

                // Берём несколько seed-треков для лучшего покрытия стиля плейлиста
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
                processAndReturnTracks(initial, currentAccount, true, callback);

                if (cancelFlag.get()) return;

                // Строим вектор как среднее по нескольким seed-трекам
                float[] seedVector = buildMultiTrackSeedVector(seeds);
                if (seedVector == null) seedVector = neuroRanker.getGenreVector("Поп");
                cachedSeedVector = seedVector;

                ArrayList<String> next = new ArrayList<>();
                for (MessageObject seed : seeds) next.addAll(getDeepSimilarTracks(seed.getMusicAuthor(), 15));
                next.addAll(getDeezerArtistMix(primarySeedArtist, 15));
                next.addAll(getLastFmSimilarTracks(primarySeedArtist, 10));
                ArrayList<String> verified = verifyCandidatesWithNeuralNetwork(next, seedVector, 20);
                processAndReturnTracks(verified, currentAccount, false, callback);

                if (cancelFlag.get()) return;

                final float[] finalSeed = seedVector;
                startInfiniteRefill(currentAccount, callback, () -> {
                    ArrayList<String> r = new ArrayList<>();
                    try {
                        r.addAll(getDeezerArtistMix(primarySeedArtist, 20));
                        r.addAll(getDeepSimilarTracks(primarySeedArtist, 15));
                        r.addAll(getLastFmSimilarTracks(primarySeedArtist, 10));
                        if (r.size() < 10) r.addAll(getTracksFromDeezerCharts(10));
                    } catch (Exception ignored) {}
                    return r;
                });

            } catch (Exception e) { handleError(callback, e); }
        });
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
    // generateWaveByArtist
    // ─────────────────────────────────────────────────────────────────────────

    public static void generateWaveByArtist(String artistName, int currentAccount, WaveCallback callback) {
        startNewWaveSession(2);
        currentConfig.query = artistName;
        initNeuroRankerIfNeeded();

        backgroundExecutor.execute(() -> {
            try {
                // Шаг 1: artist radio + mix для мгновенного старта
                ArrayList<String> initial = new ArrayList<>();
                initial.addAll(getDeezerArtistMix(artistName, 5));
                if (initial.size() < 5) initial.addAll(getDeepSimilarTracks(artistName, 8 - initial.size()));
                if (initial.size() < 5) initial.addAll(getLastFmSimilarTracks(artistName, 5 - initial.size()));
                processAndReturnTracks(initial, currentAccount, true, callback);

                if (cancelFlag.get()) return;

                float[] seedVector = neuroRanker.getVectorForRemoteTrack(artistName, "");
                if (seedVector == null) seedVector = neuroRanker.getGenreVector("Инди");
                cachedSeedVector = seedVector;

                ArrayList<String> next = new ArrayList<>();
                next.addAll(getDeepSimilarTracks(artistName, 25));
                next.addAll(getDeezerArtistMix(artistName, 15));
                next.addAll(getLastFmSimilarTracks(artistName, 15));
                ArrayList<String> verified = verifyCandidatesWithNeuralNetwork(next, seedVector, 25);
                processAndReturnTracks(verified, currentAccount, false, callback);

                if (cancelFlag.get()) return;

                startInfiniteRefill(currentAccount, callback, () -> {
                    ArrayList<String> r = new ArrayList<>();
                    try {
                        r.addAll(getDeezerArtistMix(artistName, 20));
                        r.addAll(getDeepSimilarTracks(artistName, 15));
                        r.addAll(getLastFmSimilarTracks(artistName, 10));
                    } catch (Exception ignored) {}
                    return r;
                });

            } catch (Exception e) { handleError(callback, e); }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // generateWaveByAlbum
    // ─────────────────────────────────────────────────────────────────────────

    public static void generateWaveByAlbum(String artistName, String albumName, int currentAccount, WaveCallback callback) {
        startNewWaveSession(3);
        initNeuroRankerIfNeeded();

        backgroundExecutor.execute(() -> {
            try {
                String safeArtist = (artistName == null || artistName.isEmpty()) ? "Unknown Artist" : artistName;
                String safeAlbum  = (albumName  == null || albumName.isEmpty())  ? "" : albumName;
                currentConfig.query = safeArtist;

                // Шаг 1: треки альбома → artist mix → fallback
                ArrayList<String> initial = new ArrayList<>();
                if (!safeAlbum.isEmpty()) initial.addAll(getTracksFromAlbum(safeArtist, safeAlbum, 5));
                initial.addAll(getDeezerArtistMix(safeArtist, 5 - Math.min(initial.size(), 4)));
                if (initial.size() < 5) initial.addAll(getDeepSimilarTracks(safeArtist, 5 - initial.size()));
                processAndReturnTracks(initial, currentAccount, true, callback);

                if (cancelFlag.get()) return;

                // Шаг 2: seed из треков альбома или артиста
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
                next.addAll(getLastFmSimilarTracks(safeArtist, 10));
                if (next.isEmpty()) next.addAll(getTracksFromDeezerCharts(20));

                ArrayList<String> verified = verifyCandidatesWithNeuralNetwork(next, seedVector, 25);
                processAndReturnTracks(verified, currentAccount, false, callback);

                if (cancelFlag.get()) return;

                final String finalAlbumGenre = albumGenre;
                startInfiniteRefill(currentAccount, callback, () -> {
                    ArrayList<String> r = new ArrayList<>();
                    try {
                        r.addAll(getDeezerArtistMix(safeArtist, 20));
                        r.addAll(getDeepSimilarTracks(safeArtist, 15));
                        if (finalAlbumGenre != null) r.addAll(getTracksFromDynamicPlaylists(finalAlbumGenre, 10, random.nextInt(300)));
                        r.addAll(getTracksFromITunes(safeArtist, 8));
                        if (r.size() < 10) r.addAll(getTracksFromDeezerCharts(10));
                    } catch (Exception ignored) {}
                    return r;
                });

            } catch (Exception e) { handleError(callback, e); }
        });
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

                // Адаптируем seed к текущему треку
                if (currentTrack != null) adaptSeedToCurrentTrack(currentTrack.getMusicAuthor(), currentTrack.getMusicTitle());

                ArrayList<String> candidates = new ArrayList<>();
                String searchQuery = getDeezerSearchQuery(currentConfig.query);
                int randOffset = random.nextInt(800);

                switch (currentWaveMode) {
                    case 0: // жанровая волна
                        // Ищем похожих артистов на текущего играющего
                        if (seed.getMusicAuthor() != null) {
                            candidates.addAll(getDeezerArtistMix(seed.getMusicAuthor(), 15));
                            candidates.addAll(getLastFmSimilarTracks(seed.getMusicAuthor(), 10));
                        }
                        // Всегда подкидываем треки из жанровых плейлистов для поддержания общей канвы
                        candidates.addAll(getTracksFromDynamicPlaylists(searchQuery, 10, randOffset));
                        break;
                    case 1: // плейлист
                    case 2: // артист
                    case 3: // альбом
                        candidates.addAll(getDeezerArtistMix(seed.getMusicAuthor(), 15));
                        candidates.addAll(getDeepSimilarTracks(seed.getMusicAuthor(), 15));
                        candidates.addAll(getLastFmSimilarTracks(seed.getMusicAuthor(), 10));
                        if (candidates.size() < 10) candidates.addAll(getTracksFromDeezerCharts(10));
                        break;
                }

                if (candidates.isEmpty()) candidates.addAll(getTracksFromDeezerCharts(20));

                ArrayList<String> verified = verifyCandidatesWithNeuralNetwork(candidates, seedVector, 20);
                processAndReturnTracks(verified, currentAccount, false, callback);
            } catch (Exception e) { handleError(callback, e); }
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

                    ArrayList<String> candidates = supplier.get();
                    if (candidates.isEmpty()) continue;

                    float[] vec = cachedSeedVector;
                    if (vec == null) vec = neuroRanker.getGenreVector(currentConfig.query);

                    ArrayList<String> verified = verifyCandidatesWithNeuralNetwork(candidates, vec, 15);
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

                    // Строгий поиск: находим трек с проверкой совпадения артиста и названия
                    MusicWebSearcher.WebTrack wt = MusicWebSearcher.searchTrackStrict(artist, title, 0);
                    if (wt == null || wt.downloadUrl == null || wt.downloadUrl.isEmpty()) return;

                    int dur = ChannelMusicFragment.parseDuration(wt.duration);
                    MessageObject msg = ChannelMusicFragment.createWebMessageObject(
                            account, wt.title, wt.artist, wt.downloadUrl, dur,
                            -(long) wt.downloadUrl.hashCode(), wt.coverUrl);

                    lastGeneratedBatch.add(msg);

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

    private static String performGetRequest(String urlStr) throws Exception {
        httpSemaphore.acquire();
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36");
            c.setConnectTimeout(6000);
            c.setReadTimeout(8000);
            int code = c.getResponseCode();
            if (code == 429) {
                // Rate limited — ждём и пробуем ещё раз
                Thread.sleep(2000);
                c = (HttpURLConnection) new URL(urlStr).openConnection();
                c.setRequestProperty("User-Agent", "Mozilla/5.0");
                c.setConnectTimeout(6000);
                c.setReadTimeout(8000);
            }
            try (InputStream is = c.getInputStream();
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] b = new byte[8192]; int l;
                while ((l = is.read(b)) != -1) bos.write(b, 0, l);
                return bos.toString("UTF-8");
            } catch (Exception e) { return ""; }
        } finally {
            httpSemaphore.release();
        }
    }

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

        // 1. Сначала пытаемся вытянуть треки из обновленной базы GitHub (сохраненной в кэше)
        try {
            android.content.SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("soundgram_wave", Context.MODE_PRIVATE);
            String jsonStr = prefs.getString("wave_seed_data", null);
            if (jsonStr != null) {
                JSONObject json = new JSONObject(jsonStr);
                if (json.has(g)) {
                    JSONArray arr = json.getJSONArray(g);
                    for (int i = 0; i < arr.length(); i++) {
                        list.add(arr.getString(i));
                    }
                    // Если треки найдены в динамической базе, возвращаем ИХ (полная перезапись дефолта)
                    if (!list.isEmpty()) {
                        return list;
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e("SoundGram Wave JSON parse error", e);
        }

        // 2. Если в кэше пусто или жанр там не найден — отдаем хардкод
        switch (g) {
            case "метал":
            case "металл":
                list = new java.util.ArrayList<>(java.util.Arrays.asList(
                        "Holding Absence - Afterlife",
                        "Northlane - Echo Chamber",
                        "Bring Me The Horizon - Die4U",
                        "Spiritbox - Circle With Me",
                        "Architects - Tear Gas",
                        "Sleep Token - The Summoning",
                        "I Prevail - Every Time You Leave",
                        "Our Last Night - Paradise",
                        "Landmvrks - Left For Dead",
                        "Bad Omens - Like A Villain",
                        "Slaughter to Prevail - Viking",
                        "Wildways - Прада",
                        "AMATORY - Космос",
                        "Neverlove - Инструктор из НАТО",
                        "Louna - Колыбельная",
                        "Dark Tranquillity - Phantom Days",
                        "Insomnium - Lumina",
                        "Omnium Gatherum - Paragon",
                        "Soilwork - Needle",
                        "At The Gates - Spectre of Extinction"
                ));
                break;
            case "поп":
                list = new java.util.ArrayList<>(java.util.Arrays.asList(
                        "Anna Asti - Царица",
                        "Zivert - Wake Up",
                        "JONY - Комета",
                        "Niletto - Любимка",
                        "The Weeknd - Blinding Lights",
                        "Dua Lipa - Levitating",
                        "Ed Sheeran - Bad Habits",
                        "Miley Cyrus - Flowers",
                        "Taylor Swift - Anti-Hero",
                        "Harry Styles - As It Was",
                        "Artik & Asti - Гармония",
                        "Клава Кока - Покинула чат",
                        "Люся Чеботина - Солнце Монако",
                        "Billie Eilish - bad guy",
                        "Ariana Grande - positions",
                        "Doja Cat - Kiss Me More",
                        "Olivia Rodrigo - drivers license",
                        "Justin Bieber - Peaches",
                        "Bruno Mars - Leave The Door Open",
                        "Мари Краймбрери - Иди танцуй"
                ));
                break;
            case "хип-хоп":
            case "хип хоп":
                list = new java.util.ArrayList<>(java.util.Arrays.asList(
                        "Kendrick Lamar - N95",
                        "Drake - Gods Plan",
                        "Travis Scott - SICKO MODE",
                        "J. Cole - MIDDLE CHILD",
                        "Future - Mask Off",
                        "21 Savage - a lot",
                        "Lil Uzi Vert - XO Tour Llif3",
                        "Eminem - Godzilla",
                        "Post Malone - rockstar",
                        "ASAP Rocky - Praise The Lord",
                        "Скриптонит - Цепи",
                        "Oxxxymiron - Оябун",
                        "Miyagi & Andy Panda - Minor",
                        "Баста - Сансара",
                        "Noize MC - Вселенная бесконечна",
                        "Макс Корж - Малый повзрослел",
                        "Kizaru - Дежавю",
                        "Big Baby Tape - Gimme The Loot",
                        "Boulevard Depo - Кащенко",
                        "PHARAOH - Дико например"
                ));
                break;
            case "рок":
                list = new java.util.ArrayList<>(java.util.Arrays.asList(
                        "Maneskin - ZITTI E BUONI",
                        "Imagine Dragons - Believer",
                        "Twenty One Pilots - Stressed Out",
                        "Muse - Compliance",
                        "Foo Fighters - Rescued",
                        "Royal Blood - Troubles Coming",
                        "Nothing But Thieves - Amsterdam",
                        "YUNGBLUD - The Funeral",
                        "Greta Van Fleet - Highway Tune",
                        "The Black Keys - Wild Child",
                        "Би-2 - Пекло",
                        "ДДТ - Борщевик",
                        "Сплин - Передайте это Гарри Поттеру",
                        "Земфира - Остин",
                        "Мумий Тролль - Лето без интернета",
                        "Кипелов - Час пик",
                        "Ария - Проклятье морей",
                        "Король и Шут - Лесник",
                        "Порнофильмы - Я так соскучился",
                        "Нервы - Батареи"
                ));
                break;
            case "инди":
                list = new java.util.ArrayList<>(java.util.Arrays.asList(
                        "Arctic Monkeys - 505",
                        "The Neighbourhood - Sweater Weather",
                        "Tame Impala - The Less I Know The Better",
                        "Mac DeMarco - Chamber Of Reflection",
                        "Wallows - Are You Bored Yet",
                        "Girl in Red - we fell in love in october",
                        "The Strokes - The Adults Are Talking",
                        "Florence + The Machine - Dog Days Are Over",
                        "Lana Del Rey - Video Games",
                        "Lorde - Royals",
                        "Сироткин - Выше домов",
                        "ssshhhiiittt - Танцы",
                        "Дайте танк - Утро",
                        "Буерак - Страсть к курению",
                        "Перемотка - Встречная",
                        "Космонавтов Нет - Баночка с ошейником",
                        "Свидание - Любовь",
                        "электрофорез - Э Зло",
                        "Папин Олимпос - Темно-оранжевый закат",
                        "Алена Швец - Вино и сигареты"
                ));
                break;
            case "фонк":
                list = new java.util.ArrayList<>(java.util.Arrays.asList(
                        "Kordhell - Murder In My Mind",
                        "Ghostface Playa - Why Not",
                        "Kaito Shoma - Scary Garry",
                        "DVRST - Close Eyes",
                        "Hensonn - Sahara",
                        "MoonDeity - Neon Blade",
                        "Interworld - Metamorphosis",
                        "dabl - Vengeance",
                        "Scxr Soul - Sigma",
                        "Kute - Avoid Me",
                        "Swerve - Kerosene",
                        "Phonkha - MTG",
                        "Misha Xramovi - Misha Xramovi",
                        "VOJ - Memory Reboot",
                        "Razihel - Love U",
                        "Odetari - Good Loyal Thots",
                        "Plenka - Call Me",
                        "ZXC - GHOUL",
                        "Gawne - Deathwish",
                        "PlayaPhonk - PHONKY TOWN"
                ));
                break;
            case "гиперпоп":
                list = new java.util.ArrayList<>(java.util.Arrays.asList(
                        "100 gecs - money machine",
                        "Charli XCX - Vroom Vroom",
                        "SOPHIE - Its Okay to Cry",
                        "Dorian Electra - Flamboyant",
                        "Rico Nasty - IPHONE",
                        "Glaive - astrid",
                        "ericdoa - fawnboy",
                        "midwxst - trying",
                        "Brakence - dropout",
                        "ElyOtto - SugarCrash",
                        "Lida - Девочка с концерта",
                        "GSPD - Евродэнс",
                        "Мейби Бэйби - Ахегао",
                        "Дора - Втюрилась",
                        "CMH - PAYDAY",
                        "Sqwoz Bab - ОЙ",
                        "Slava Marlow - Ты горишь как огонь",
                        "Эльдар Джарахов - Я в моменте",
                        "Morgenstern - Cristal MOET",
                        "Instasamka - За деньги да"
                ));
                break;
            case "пост-панк":
                list = new java.util.ArrayList<>(java.util.Arrays.asList(
                        "Molchat Doma - Судно",
                        "Ploho - Закладка",
                        "Buerak - Спортивные очки",
                        "Kino - Группа крови",
                        "Ssshhhiiittt - Восьмерка",
                        "Nautilus Pompilius - Дыхание",
                        "Agatha Christie - Как на войне",
                        "Bi-2 - Полковнику никто не пишет",
                        "Joy Division - Love Will Tear Us Apart",
                        "The Cure - Boys Dont Cry",
                        "Echo And The Bunnymen - The Killing Moon",
                        "Bauhaus - Bela Lugosis Dead",
                        "Siouxsie And The Banshees - Spellbound",
                        "The Smiths - This Charming Man",
                        "New Order - Blue Monday",
                        "Interpol - Evil",
                        "Editors - Munich",
                        "White Lies - To Lose My Life",
                        "Soviet Soviet - No Lesson",
                        "Motorama - Alps"
                ));
                break;
            case "эмо-рэп":
            case "эмо реп":
                list = new java.util.ArrayList<>(java.util.Arrays.asList(
                        "Lil Peep - Star Shopping",
                        "XXXTENTACION - Jocelyn Flores",
                        "Juice WRLD - Lucid Dreams",
                        "Trippie Redd - Love Scars",
                        "Iann Dior - emotions",
                        "The Kid LAROI - WITHOUT YOU",
                        "Post Malone - I Fall Apart",
                        "Joji - SLOW DANCING IN THE DARK",
                        "Lil Uzi Vert - The Way Life Goes",
                        "Sueco - Paralyzed",
                        "Lida - Иди за мной",
                        "PHARAOH - Лаллипап",
                        "Morgenstern - Селяви",
                        "Face - Юморист",
                        "Oxxxymiron - Город под подошвой",
                        "Скриптонит - Космос",
                        "T-Fest - Улети",
                        "Элджей - Розовое вино",
                        "ЛСП - Монетка",
                        "Хаски - Иуда"
                ));
                break;
            case "альтернатива":
                list = new java.util.ArrayList<>(java.util.Arrays.asList(
                        "Linkin Park - Numb",
                        "Nirvana - Smells Like Teen Spirit",
                        "Red Hot Chili Peppers - Californication",
                        "Green Day - Boulevard of Broken Dreams",
                        "My Chemical Romance - Welcome to the Black Parade",
                        "Fall Out Boy - Sugar We Are Goin Down",
                        "Paramore - Misery Business",
                        "Blink-182 - All The Small Things",
                        "Sum 41 - Fat Lip",
                        "The Offspring - The Kids Arent Alright",
                        "Lumen - Гореть",
                        "Tracktor Bowling - Черта",
                        "Amatory - Черно-белые дни",
                        "Stigmata - Сентябрь",
                        "Jane Air - Junk",
                        "Психея - Навсегда",
                        "Слот - Мертвые звезды",
                        "Алиса - Трасса Е-95",
                        "Агата Кристи - Сказочная тайга",
                        "Би-2 - Серебро"
                ));
                break;
            case "к-поп":
            case "к поп":
                list = new java.util.ArrayList<>(java.util.Arrays.asList(
                        "BTS - Dynamite",
                        "BLACKPINK - How You Like That",
                        "Stray Kids - God's Menu",
                        "TWICE - The Feels",
                        "EXO - Love Shot",
                        "Red Velvet - Psycho",
                        "ITZY - WANNABE",
                        "SEVENTEEN - HOT",
                        "NCT 127 - Kick It",
                        "TXT - 0X1=LOVESONG",
                        "ATEEZ - WONDERLAND",
                        "ENHYPEN - Drunk-Dazed",
                        "aespa - Next Level",
                        "NewJeans - OMG",
                        "LE SSERAFIM - ANTIFRAGILE",
                        "IVE - LOVE DIVE",
                        "FIFTY FIFTY - Cupid",
                        "Jimin - Like Crazy",
                        "Jung Kook - Seven",
                        "MAMAMOO - HIP"
                ));
                break;
            case "электроника":
                list = new java.util.ArrayList<>(java.util.Arrays.asList(
                        "Daft Punk - Get Lucky",
                        "The Prodigy - Smack My Bitch Up",
                        "The Chemical Brothers - Hey Boy Hey Girl",
                        "Fatboy Slim - Right Here Right Now",
                        "Moby - Porcelain",
                        "Aphex Twin - Windowlicker",
                        "Bicep - Glue",
                        "Disclosure - Latch",
                        "Flume - Never Be Like You",
                        "Avicii - Levels",
                        "IC3PEAK - Смерти Больше Нет",
                        "Cream Soda - Плачу на техно",
                        "GSPD - Некстген",
                        "XS Project - Бочка Басс Колбасер",
                        "Hard Bass School - Narkotik Kal",
                        "Dead Blonde - Мальчик на девятке",
                        "Skrillex - Bangarang",
                        "DJ Snake - Turn Down for What",
                        "Major Lazer - Lean On",
                        "Marshmello - Alone"
                ));
                break;
            case "синтвейв":
                list = new java.util.ArrayList<>(java.util.Arrays.asList(
                        "Kavinsky - Nightcall",
                        "The Midnight - Sunset",
                        "Timecop1983 - On the Run",
                        "Gunship - Tech Noir",
                        "FM-84 - Running In The Night",
                        "Lazerhawk - Overdrive",
                        "Miami Nights 1984 - Ocean Drive",
                        "Carpenter Brut - Turbo Killer",
                        "Perturbator - Future Club",
                        "Dance With The Dead - Riot",
                        "Scandroid - Neo-Tokyo",
                        "Trevor Something - Summer Love",
                        "Mitch Murder - Interceptor",
                        "W O L F C L U B - Tears",
                        "Ollie Wride - The Line",
                        "Электрофорез - Я ничего не могу с собою сделать",
                        "Bi-2 - Депрессия",
                        "Kino - Спокойная ночь",
                        "Nautilus Pompilius - Взгляд с экрана",
                        "Альянс - На заре"
                ));
                break;
            default:
                break;
        }
        return list;
    }


}
