package soundgram.music;

import android.content.Context;
import android.content.SharedPreferences;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SmartRadioEngine v3 — переработан под реальные условия РФ.
 *
 * Что убрано:
 *  - ONNX / NeuroRanker — скачивал 30-сек превью, медленно, ненадёжно, толку ноль
 *  - iTunes Search API — заблокирован в РФ
 *  - Last.fm как основной источник — заблокирован в РФ
 *
 * Что добавлено / изменено:
 *  - Deezer Genre API (/genre, /genre/{id}/artists) — основа жанровой волны
 *  - Deezer Radio stations (/radio/genres — тематические станции)
 *  - Deezer track/radio — похожие треки на конкретный трек (основа волны по артисту/альбому/плейлисту)
 *  - Deezer related artists — цепочки похожих артистов
 *  - Last.fm оставлен как дополнительный источник (работает при наличии прокси)
 *  - Схожесть треков определяется через Deezer-граф (radio/related), а не ONNX
 *  - Жанровая фильтрация: Deezer genre_id + whitelist артистов жанра + seed-треки
 *  - RU чарт для русского контента (40%)
 *
 * Архитектура волны:
 *  MODE 0 (жанр):       Deezer genre artists → их radio → фильтр по genre_id
 *  MODE 1 (артист):     artist/top → track/radio → related artists → их треки
 *  MODE 2 (альбом):     album tracks → track/radio → artist/related
 *  MODE 3 (плейлист):   seed tracks → track/radio → cross-seed related
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

    // ── История воспроизведения — скользящее окно 3000 ────────────────────
    private static final HashSet<String> globalPlayHistory = new LinkedHashSet<String>() {
        private static final int MAX_SIZE = 3000;
        @Override
        public boolean add(String s) {
            if (size() >= MAX_SIZE) remove(iterator().next());
            return super.add(s);
        }
    };
    private static final Object historyLock = new Object();

    private static final Random random = new Random();

    private static volatile ExecutorService backgroundExecutor = createExecutor();
    // Не более 4 одновременных HTTP к Deezer/YTM
    private static final Semaphore httpSemaphore = new Semaphore(4, true);

    private static ExecutorService createExecutor() {
        return new java.util.concurrent.ThreadPoolExecutor(
                0, 12,
                8L, java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>()
        );
    }

    private static final CopyOnWriteArrayList<MessageObject> lastGeneratedBatch = new CopyOnWriteArrayList<>();

    // Кеш Deezer genre_id: название жанра EN → ID
    private static final ConcurrentHashMap<String, Long> genreIdCache = new ConcurrentHashMap<>();
    // Кеш артистов жанра: genre_id → список artist_id
    private static final ConcurrentHashMap<Long, ArrayList<Long>> genreArtistCache = new ConcurrentHashMap<>();
    // Кеш альбомных годов
    private static final ConcurrentHashMap<Long, Integer> albumYearCache = new ConcurrentHashMap<>();

    private static final HashSet<String> waveSeedKeys = new HashSet<>();
    private static final ArrayList<String> recentArtistsWindow = new ArrayList<>();
    private static final ArrayList<String> recentTitlesWindow = new ArrayList<>();
    private static final HashMap<String, Integer> artistStreakCounter = new HashMap<>();
    private static final int RECENT_ARTISTS_WINDOW_SIZE = 15;
    private static final int RECENT_TITLES_WINDOW_SIZE = 25;

    private static final AtomicBoolean cancelFlag = new AtomicBoolean(false);

    // ── Skip Penalty ──────────────────────────────────────────────────────
    private static final ConcurrentHashMap<String, Integer> trackPenaltyMap = new ConcurrentHashMap<>();
    private static final Object penaltyLock = new Object();
    private static volatile boolean penaltiesLoaded = false;
    private static final int TRACK_PENALTY_HIDE = 15;

    // ── Library Boost ─────────────────────────────────────────────────────
    private static final ConcurrentHashMap<String, Integer> libraryBoostMap = new ConcurrentHashMap<>();
    private static final float LIKED_BOOST = 0.15f; // не используется напрямую, но можно расширить

    // ── RU контент ────────────────────────────────────────────────────────
    private static final float RUSSIAN_TRACKS_RATIO = 0.40f;
    private static int waveTrackCount = 0;

    // ── Жанровый пул seed-артистов (фиксируется при старте жанровой волны) ─
    private static volatile ArrayList<Long> currentGenreArtistPool = new ArrayList<>();

    private static final int CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR);
    private static final int MIN_RECENT_YEAR = CURRENT_YEAR - 5;

    // ── YTM InnerTube (только MODE 0 — жанровая волна) ─────────────────────
    private static final String YTM_BASE           = "https://music.youtube.com/youtubei/v1/";
    private static final String YTM_KEY            = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-KEHRIN7VO7";
    private static final String YTM_CLIENT_NAME    = "WEB_REMIX";
    private static final String YTM_CLIENT_VERSION = "1.20231101.01.00";

    /** videoId текущего якорного трека (начало ветки рекомендаций) */
    private static volatile String  ytmCurrentVideoId    = null;
    /** Токен продолжения YTM /next для pagination */
    private static volatile String  ytmContinuationToken = null;
    /** Счётчик треков с момента последней смены якоря (Anti-Drift) */
    private static final AtomicInteger ytmTracksSinceAnchor = new AtomicInteger(0);
    /** После N сыгранных треков принудительно берём новый якорь */
    private static final int YTM_ANTI_DRIFT_THRESHOLD = 15;
    /** Мьютекс для ytmCurrentVideoId / ytmContinuationToken */
    private static final Object ytmLock = new Object();

    // ── Персистентная 7-дневная история воспроизведения ─────────────────────
    private static final String PREFS_HISTORY  = "soundgram_radio_history_v1";
    private static final long   SEVEN_DAYS_MS  = 7L * 24 * 60 * 60 * 1000; // 604 800 000 мс
    private static final Object historyPersistLock = new Object();


    // ─────────────────────────────────────────────────────────────────────
    // Инициализация / сессия
    // ─────────────────────────────────────────────────────────────────────

    public static void startNewWaveSession(int mode) {
        cancelFlag.set(true);
        isWaveActive = false;
        isLoading = false;

        // Принудительно убиваем все висящие задачи (включая блокирующие HTTP-запросы)
        ExecutorService oldExec = backgroundExecutor;
        backgroundExecutor = createExecutor();
        oldExec.shutdownNow();

        currentWaveMode = mode;
        synchronized (basePlaylistContext) { basePlaylistContext.clear(); }
        lastGeneratedBatch.clear();
        currentGenreArtistPool = new ArrayList<>();

        synchronized (historyLock) { globalPlayHistory.clear(); }
        synchronized (waveSeedKeys) { waveSeedKeys.clear(); }
        synchronized (recentArtistsWindow) { recentArtistsWindow.clear(); }
        synchronized (recentTitlesWindow) { recentTitlesWindow.clear(); }
        synchronized (artistStreakCounter) { artistStreakCounter.clear(); }
        waveTrackCount = 0;

        // Сброс YTM-состояния при старте новой волны
        synchronized (ytmLock) { ytmCurrentVideoId = null; ytmContinuationToken = null; }
        ytmTracksSinceAnchor.set(0);

        backgroundExecutor.execute(SmartRadioEngine::loadPenaltiesIfNeeded);
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        cancelFlag.set(false);
        isWaveActive = true;
    }

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

    // ─────────────────────────────────────────────────────────────────────
    // Skip / Library API
    // ─────────────────────────────────────────────────────────────────────

    public static void reportSkip(String artist, String title, long playedMs) {
        if (playedMs >= 20_000L || artist == null || artist.isEmpty()) return;
        backgroundExecutor.execute(() -> {
            try {
                loadPenaltiesIfNeeded();
                String key = normalizeSeedKey(artist, title != null ? title : "");
                synchronized (penaltyLock) { trackPenaltyMap.merge(key, 5, Integer::sum); }
                savePenalties();
            } catch (Exception e) { FileLog.e(e); }
        });
    }

    public static void reportLibraryAdd(String artist, String title) {
        if (artist == null || artist.isEmpty()) return;
        backgroundExecutor.execute(() -> {
            try {
                loadPenaltiesIfNeeded();
                String artistKey = artist.toLowerCase().trim();
                String trackKey  = normalizeSeedKey(artist, title != null ? title : "");
                synchronized (penaltyLock) {
                    libraryBoostMap.merge(trackKey,  5, Integer::sum);
                    libraryBoostMap.merge(artistKey, 3, Integer::sum);
                }
                savePenalties();
            } catch (Exception e) { FileLog.e(e); }
        });
    }

    private static void loadPenaltiesIfNeeded() {
        if (penaltiesLoaded) return;
        synchronized (penaltyLock) {
            if (penaltiesLoaded) return;
            try {
                SharedPreferences prefs = ApplicationLoader.applicationContext
                        .getSharedPreferences("soundgram_skip_penalties", Context.MODE_PRIVATE);
                String json = prefs.getString("penalties_v3", null);
                if (json != null) {
                    JSONObject obj    = new JSONObject(json);
                    JSONObject trkObj = obj.optJSONObject("tracks");
                    JSONObject bstObj = obj.optJSONObject("boosts");
                    if (trkObj != null) {
                        java.util.Iterator<String> it = trkObj.keys();
                        while (it.hasNext()) { String k = it.next(); trackPenaltyMap.put(k, trkObj.getInt(k)); }
                    }
                    if (bstObj != null) {
                        java.util.Iterator<String> it = bstObj.keys();
                        while (it.hasNext()) { String k = it.next(); libraryBoostMap.put(k, bstObj.getInt(k)); }
                    }
                }
            } catch (Exception e) { FileLog.e(e); }
            penaltiesLoaded = true;
        }
    }

    private static void savePenalties() {
        try {
            JSONObject trkObj = new JSONObject();
            for (Map.Entry<String, Integer> e : trackPenaltyMap.entrySet()) trkObj.put(e.getKey(), e.getValue());
            JSONObject bstObj = new JSONObject();
            for (Map.Entry<String, Integer> e : libraryBoostMap.entrySet()) bstObj.put(e.getKey(), e.getValue());
            JSONObject root = new JSONObject();
            root.put("tracks", trkObj);
            root.put("boosts", bstObj);
            ApplicationLoader.applicationContext
                    .getSharedPreferences("soundgram_skip_penalties", Context.MODE_PRIVATE)
                    .edit().putString("penalties_v3", root.toString()).apply();
        } catch (Exception e) { FileLog.e(e); }
    }

    private static boolean isTrackFiltered(String artist, String title) {
        if (!penaltiesLoaded) loadPenaltiesIfNeeded();
        String trackKey = normalizeSeedKey(artist, title != null ? title : "");
        return trackPenaltyMap.getOrDefault(trackKey, 0) >= TRACK_PENALTY_HIDE;
    }

    // Повышаем приоритет трека если артист/трек в библиотеке
    private static boolean hasLibraryBoost(String artist, String title) {
        String artistKey = artist == null ? "" : artist.toLowerCase().trim();
        String trackKey  = normalizeSeedKey(artist, title != null ? title : "");
        return libraryBoostMap.getOrDefault(trackKey, 0) > 0
                || libraryBoostMap.getOrDefault(artistKey, 0) > 0;
    }

    // ─────────────────────────────────────────────────────────────────────
    // HTTP
    // ─────────────────────────────────────────────────────────────────────

    private static java.net.Proxy getTelegramProxy() {
        try {
            Class<?> sc = Class.forName("org.telegram.messenger.SharedConfig");
            Object proxy = sc.getField("currentProxy").get(null);
            if (proxy == null) return null;
            boolean enabled = false;
            for (String fn : new String[]{"useProxy", "proxyEnabled", "enabled"}) {
                try { enabled = sc.getField(fn).getBoolean(null); break; }
                catch (NoSuchFieldException ignored) {}
            }
            if (!enabled) return null;
            Class<?> pc = proxy.getClass();
            String address = (String) pc.getField("address").get(proxy);
            int port = pc.getField("port").getInt(proxy);
            if (address == null || address.isEmpty() || port <= 0) return null;
            try {
                String secret = (String) pc.getField("secret").get(proxy);
                if (secret != null && !secret.isEmpty()) return null; // MTProto proxy
            } catch (NoSuchFieldException ignored) {}
            return new java.net.Proxy(java.net.Proxy.Type.SOCKS,
                    new java.net.InetSocketAddress(address, port));
        } catch (Exception e) { return null; }
    }

    private static String performGetRequest(String urlStr) throws Exception {
        return performGetRequest(urlStr, false);
    }

    private static String performGetRequest(String urlStr, boolean useProxy) throws Exception {
        if (cancelFlag.get()) return "";
        httpSemaphore.acquire();
        try {
            if (cancelFlag.get()) return "";
            java.net.Proxy proxy = useProxy ? getTelegramProxy() : null;
            HttpURLConnection c = (HttpURLConnection) (proxy != null
                    ? new URL(urlStr).openConnection(proxy)
                    : new URL(urlStr).openConnection());
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36");
            c.setConnectTimeout(7000);
            c.setReadTimeout(10000);
            int code;
            try { code = c.getResponseCode(); } catch (Exception e) { return ""; }
            if (code == 429) {
                Thread.sleep(2000);
                if (cancelFlag.get()) return "";
                c = (HttpURLConnection) (proxy != null
                        ? new URL(urlStr).openConnection(proxy)
                        : new URL(urlStr).openConnection());
                c.setRequestProperty("User-Agent", "Mozilla/5.0");
                c.setConnectTimeout(7000);
                c.setReadTimeout(10000);
            }
            if (c.getResponseCode() / 100 != 2) return "";
            try (InputStream is = c.getInputStream();
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] b = new byte[8192]; int l;
                while ((l = is.read(b)) != -1) {
                    if (cancelFlag.get()) return "";
                    bos.write(b, 0, l);
                }
                return bos.toString("UTF-8");
            } catch (Exception e) { return ""; }
        } finally {
            httpSemaphore.release();
        }
    }


    // ─────────────────────────────────────────────────────────────────────
    // YTM InnerTube — HTTP POST
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Выполняет POST-запрос к YouTube Music InnerTube API.
     * Использует прокси Telegram если он включён.
     *
     * @param endpoint  имя эндпоинта ("search", "next", "browse")
     * @param jsonBody  тело запроса
     * @return ответ UTF-8 строкой или "" при ошибке
     */
    private static String performYtmPostRequest(String endpoint, String jsonBody) {
        if (cancelFlag.get()) return "";
        try {
            java.net.Proxy proxy = getTelegramProxy();
            FileLog.d("SmartRadio [Net]: Отправка POST к YTM /" + endpoint
                    + ". Использование прокси: " + (proxy != null));

            String urlStr = YTM_BASE + endpoint + "?key=" + YTM_KEY;
            HttpURLConnection conn = (HttpURLConnection) (proxy != null
                    ? new URL(urlStr).openConnection(proxy)
                    : new URL(urlStr).openConnection());

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(9000);
            conn.setReadTimeout(14000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("X-Goog-Api-Format-Version", "1");
            conn.setRequestProperty("X-YouTube-Client-Name", "67");          // WEB_REMIX = 67
            conn.setRequestProperty("X-YouTube-Client-Version", YTM_CLIENT_VERSION);
            conn.setRequestProperty("Origin", "https://music.youtube.com");
            conn.setRequestProperty("Referer", "https://music.youtube.com/");
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "Chrome/124.0.0.0 Safari/537.36");

            byte[] bodyBytes = jsonBody.getBytes("UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            try (OutputStream os = conn.getOutputStream()) { os.write(bodyBytes); }

            int code = conn.getResponseCode();
            if (code / 100 != 2) {
                FileLog.e("SmartRadio [Net]: YTM /" + endpoint + " вернул HTTP " + code);
                return "";
            }
            try (InputStream is = conn.getInputStream();
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192]; int len;
                while ((len = is.read(buf)) != -1) {
                    if (cancelFlag.get()) return "";
                    bos.write(buf, 0, len);
                }
                return bos.toString("UTF-8");
            }
        } catch (Exception e) {
            FileLog.e("SmartRadio [Net]: YTM POST error (" + endpoint + ")", e);
            return "";
        }
    }

    /** Строит стандартный InnerTube context для анонимной сессии WEB_REMIX. */
    private static JSONObject buildYtmContext() throws JSONException {
        JSONObject client = new JSONObject();
        client.put("clientName", YTM_CLIENT_NAME);
        client.put("clientVersion", YTM_CLIENT_VERSION);
        client.put("hl", "en");
        client.put("gl", "US");
        JSONObject ctx = new JSONObject();
        ctx.put("client", client);
        return ctx;
    }

    // ─────────────────────────────────────────────────────────────────────
    // YTM — рекурсивные JSON-парсеры
    // ─────────────────────────────────────────────────────────────────────

    /** Извлекает первый text из { "runs":[{"text":"..."}] } */
    private static String getRunsText(JSONObject obj) {
        if (obj == null) return null;
        try {
            JSONArray runs = obj.optJSONArray("runs");
            if (runs != null && runs.length() > 0)
                return runs.getJSONObject(0).optString("text", null);
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Рекурсивно собирает все playlistPanelVideoRenderer.
     * Каждый элемент: String[]{ videoId, title, artist }
     */
    private static void collectPanelVideos(Object node, List<String[]> out) {
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            if (obj.has("playlistPanelVideoRenderer")) {
                try {
                    JSONObject pvr = obj.getJSONObject("playlistPanelVideoRenderer");
                    String vid    = pvr.optString("videoId", null);
                    String title  = getRunsText(pvr.optJSONObject("title"));
                    String artist = getRunsText(pvr.optJSONObject("longBylineText"));
                    if (artist == null) artist = getRunsText(pvr.optJSONObject("shortBylineText"));
                    if (vid != null && !vid.isEmpty() && title != null && !title.isEmpty())
                        out.add(new String[]{vid, title, artist != null ? artist : ""});
                } catch (Exception ignored) {}
            }
            java.util.Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                try { collectPanelVideos(obj.get(k), out); } catch (Exception ignored) {}
            }
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < arr.length(); i++)
                try { collectPanelVideos(arr.get(i), out); } catch (Exception ignored) {}
        }
    }

    /**
     * Рекурсивно ищет первый videoId в JSON.
     * Используется при парсинге ответа /search.
     */
    private static String findFirstVideoId(Object node) {
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            if (obj.has("videoId")) {
                String v = obj.optString("videoId", null);
                if (v != null && !v.isEmpty()) return v;
            }
            java.util.Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                try {
                    String f = findFirstVideoId(obj.get(k));
                    if (f != null) return f;
                } catch (Exception ignored) {}
            }
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < arr.length(); i++)
                try {
                    String f = findFirstVideoId(arr.get(i));
                    if (f != null) return f;
                } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Рекурсивно ищет токен продолжения в ответе /next.
     * Признак nextContinuationData: объект с обоими полями "continuation" + "clickTrackingParams".
     */
    private static String findContinuationToken(Object node) {
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            if (obj.has("continuation") && obj.has("clickTrackingParams")) {
                String tok = obj.optString("continuation", null);
                if (tok != null && !tok.isEmpty()) return tok;
            }
            java.util.Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                try {
                    String f = findContinuationToken(obj.get(k));
                    if (f != null) return f;
                } catch (Exception ignored) {}
            }
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < arr.length(); i++)
                try {
                    String f = findContinuationToken(arr.get(i));
                    if (f != null) return f;
                } catch (Exception ignored) {}
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // YTM — высокоуровневые методы
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Ищет трек на YTM по исполнителю + названию и возвращает videoId.
     * Используется для нахождения якоря из seed-трека.
     */
    private static String searchYtmVideoId(String artist, String title) {
        try {
            JSONObject body = new JSONObject();
            body.put("context", buildYtmContext());
            body.put("query", (artist + " " + title).trim());
            String resp = performYtmPostRequest("search", body.toString());
            if (resp.isEmpty()) return null;
            return findFirstVideoId(new JSONObject(resp));
        } catch (Exception e) {
            FileLog.e("SmartRadio [YTM]: searchYtmVideoId error", e);
            return null;
        }
    }

    /**
     * Выбирает случайный seed-трек жанра и получает его videoId через YTM search.
     * Вызывается при инициализации волны и при Anti-Drift сбросе.
     *
     * @return новый videoId или старый ytmCurrentVideoId при неудаче
     */
    private static String refreshYtmAnchor(String genre) {
        ArrayList<String> seeds = getSeedTracksForGenre(genre);
        if (seeds.isEmpty()) return ytmCurrentVideoId;
        Collections.shuffle(seeds);
        for (int i = 0; i < Math.min(5, seeds.size()); i++) {
            String seed = seeds.get(i);
            String[] p  = seed.split(" - ", 2);
            String vid  = searchYtmVideoId(p[0].trim(), p.length > 1 ? p[1].trim() : "");
            if (vid != null && !vid.isEmpty()) {
                FileLog.d("SmartRadio [Init]: Якорь выбран: videoId=" + vid
                        + " (seed: " + seed + ")");
                return vid;
            }
        }
        FileLog.e("SmartRadio [Init]: Не удалось найти якорь через YTM search");
        return ytmCurrentVideoId;
    }

    /** Контейнер ответа YTM /next: список треков + токен продолжения */
    private static class YtmNextResult {
        final ArrayList<String> tracks;
        final String continuationToken;
        YtmNextResult(ArrayList<String> tracks, String continuationToken) {
            this.tracks = tracks;
            this.continuationToken = continuationToken;
        }
    }

    /**
     * Запрашивает рекомендации от YTM через /next.
     *
     * Режимы вызова:
     * - continuationToken != null → pagination существующей ветки
     * - videoId != null, token == null → новая ветка RDAMVM-радио от якоря
     *
     * Треки уже встречавшиеся в сессии (globalPlayHistory) пропускаются.
     */
    private static YtmNextResult getYtmRecommendations(String videoId, String continuationToken) {
        try {
            JSONObject body = new JSONObject();
            body.put("context", buildYtmContext());

            if (continuationToken != null && !continuationToken.isEmpty()) {
                body.put("continuation", continuationToken);
            } else if (videoId != null && !videoId.isEmpty()) {
                body.put("videoId", videoId);
                body.put("playlistId", "RDAMVM" + videoId);
                JSONObject watchCfg = new JSONObject();
                JSONObject musicCfg = new JSONObject();
                musicCfg.put("hasPersistentPlaylistPanel", false);
                musicCfg.put("musicVideoType", "MUSIC_VIDEO_TYPE_ATV");
                watchCfg.put("watchEndpointMusicConfig", musicCfg);
                body.put("watchEndpointMusicSupportedConfigs", watchCfg);
            } else {
                FileLog.e("SmartRadio [YTM]: getYtmRecommendations — нет ни videoId ни token");
                return null;
            }

            String resp = performYtmPostRequest("next", body.toString());
            if (resp.isEmpty()) return null;

            JSONObject json = new JSONObject(resp);

            List<String[]> videos = new ArrayList<>();
            collectPanelVideos(json, videos);
            FileLog.d("SmartRadio [YTM]: Получено " + videos.size() + " треков от алгоритма.");

            String token = findContinuationToken(json);

            ArrayList<String> tracks = new ArrayList<>();
            for (String[] v : videos) {
                String art = v[2].trim();
                String tit = v[1].trim();
                if (art.isEmpty() || tit.isEmpty()) continue;
                synchronized (historyLock) {
                    if (globalPlayHistory.add(art + tit))
                        tracks.add(art + " - " + tit);
                }
            }
            return new YtmNextResult(tracks, token);

        } catch (Exception e) {
            FileLog.e("SmartRadio [YTM]: getYtmRecommendations error", e);
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Персистентная история воспроизведения — 7 дней
    // ─────────────────────────────────────────────────────────────────────

    /** Нормализованный ключ трека для хранения в SharedPreferences. */
    private static String trackHistoryKey(String artist, String title) {
        return ((artist == null ? "" : artist.toLowerCase().trim())
                + "|"
                + (title  == null ? "" : title.toLowerCase().trim()))
                .replaceAll("[^a-z\u0430-\u044f\u0451|0-9]", "");
    }

    /**
     * Проверяет, разрешён ли трек к воспроизведению.
     * Возвращает false если трек звучал менее 7 дней назад.
     */
    private static boolean isTrackAllowed7Days(String artist, String title) {
        synchronized (historyPersistLock) {
            try {
                SharedPreferences prefs = ApplicationLoader.applicationContext
                        .getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE);
                String jsonStr = prefs.getString("history", null);
                if (jsonStr == null) return true;
                JSONObject map = new JSONObject(jsonStr);
                String key = trackHistoryKey(artist, title);
                if (!map.has(key)) return true;
                return (System.currentTimeMillis() - map.getLong(key)) >= SEVEN_DAYS_MS;
            } catch (Exception e) {
                FileLog.e("SmartRadio [History]: isTrackAllowed7Days error", e);
                return true;
            }
        }
    }

    /**
     * Записывает трек в персистентную историю (текущий timestamp).
     * Вызывается асинхронно после успешного резолва через Hitmos.
     */
    private static void recordTrackInHistory(String artist, String title) {
        backgroundExecutor.execute(() -> {
            synchronized (historyPersistLock) {
                try {
                    SharedPreferences prefs = ApplicationLoader.applicationContext
                            .getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE);
                    String jsonStr = prefs.getString("history", null);
                    JSONObject map = (jsonStr != null) ? new JSONObject(jsonStr) : new JSONObject();
                    map.put(trackHistoryKey(artist, title), System.currentTimeMillis());
                    prefs.edit().putString("history", map.toString()).apply();
                } catch (Exception e) {
                    FileLog.e("SmartRadio [History]: recordTrackInHistory error", e);
                }
            }
        });
    }

    /**
     * Удаляет из истории записи старше 7 дней.
     * Вызывается при каждом запуске новой волны.
     */
    private static void pruneOldHistory() {
        backgroundExecutor.execute(() -> {
            synchronized (historyPersistLock) {
                try {
                    SharedPreferences prefs = ApplicationLoader.applicationContext
                            .getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE);
                    String jsonStr = prefs.getString("history", null);
                    if (jsonStr == null) return;
                    JSONObject map = new JSONObject(jsonStr);
                    long now = System.currentTimeMillis();
                    java.util.Iterator<String> it = map.keys();
                    ArrayList<String> toRemove = new ArrayList<>();
                    while (it.hasNext()) {
                        String k = it.next();
                        try { if ((now - map.getLong(k)) >= SEVEN_DAYS_MS) toRemove.add(k); }
                        catch (Exception ignored) {}
                    }
                    for (String k : toRemove) map.remove(k);
                    prefs.edit().putString("history", map.toString()).apply();
                    if (!toRemove.isEmpty())
                        FileLog.d("SmartRadio [History]: Очистка — удалено "
                                + toRemove.size() + " устаревших записей.");
                } catch (Exception e) {
                    FileLog.e("SmartRadio [History]: pruneOldHistory error", e);
                }
            }
        });
    }

    private static void handleError(WaveCallback callback, Exception e) {
        FileLog.e(e);
        AndroidUtilities.runOnUIThread(() -> { isLoading = false; callback.onError(); });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Deezer Genre API
    // ─────────────────────────────────────────────────────────────────────

    /** Маппинг русского жанра → Deezer genre name (для поиска по /genre)
     *  Для субжанров где Deezer использует слишком широкую категорию (фонк→Rap/HipHop,
     *  синтвейв→Electro и т.д.) возвращаем null — движок использует seed-based подход. */
    private static String getDeezerGenreName(String genreRu) {
        if (genreRu == null) return null;
        switch (genreRu.toLowerCase().trim()) {
            case "поп":                          return "Pop";
            case "рэп":                          return "Rap/Hip Hop";
            case "хип-хоп": case "хип хоп":     return "Rap/Hip Hop";
            case "рок":                          return "Rock";
            case "метал": case "металл":         return "Metal";
            case "джаз":                         return "Jazz";
            case "блюз":                         return "Blues";
            case "классика":                     return "Classical";
            case "к-поп": case "к поп":         return "Pop";
            // Субжанры: Deezer категория слишком широкая — используем только seed+плейлисты
            case "фонк":                         return null; // Rap/Hip Hop слишком широко
            case "эмо-рэп": case "эмо реп":     return null; // то же самое
            case "гиперпоп":                     return null; // Pop слишком широко
            case "синтвейв":                     return null; // Electro слишком широко
            case "ло-фай": case "лофай":         return null; // Electro слишком широко
            case "эмбиент":                      return null; // Electro слишком широко
            case "пост-панк":                    return null; // Alternative слишком широко
            case "инди":                         return "Alternative"; // OK, достаточно специфично
            case "альтернатива":                 return "Alternative";
            case "электроника":                  return "Electro";
            default:                             return null;
        }
    }

    /** Deezer genre search query для поиска плейлистов */
    private static String getDeezerSearchQuery(String genre) {
        if (genre == null) return "Top Hits";
        switch (genre.toLowerCase().trim()) {
            case "поп":                          return "Pop";
            case "хип-хоп": case "хип хоп":     return "Hip Hop";
            case "рэп":                          return "Russian rap";
            case "рок":                          return "Rock";
            case "инди":                         return "Indie";
            case "фонк":                         return "Phonk";
            case "гиперпоп":                     return "Hyperpop";
            case "пост-панк":                    return "Post punk";
            case "эмо-рэп": case "эмо реп":     return "Emo rap";
            case "метал": case "металл":         return "Metal";
            case "альтернатива":                 return "Alternative rock";
            case "к-поп": case "к поп":         return "K-pop";
            case "джаз":                         return "Jazz";
            case "блюз":                         return "Blues";
            case "классика":                     return "Classical";
            case "электроника":                  return "Electronic";
            case "синтвейв":                     return "Synthwave";
            case "ло-фай": case "лофай":         return "Lofi hiphop";
            case "эмбиент":                      return "Ambient";
            default:                             return genre.equals("Любой жанр") ? "Top Hits" : genre;
        }
    }

    /**
     * Получает Deezer genre_id по названию жанра.
     * Использует /genre API — возвращает список жанров с ID.
     * Результат кешируется.
     */
    private static long getDeezerGenreId(String genreRu) throws Exception {
        String genreName = getDeezerGenreName(genreRu);
        if (genreName == null) return -1;
        Long cached = genreIdCache.get(genreName);
        if (cached != null) return cached;

        String resp = performGetRequest("https://api.deezer.com/genre");
        if (resp.isEmpty()) return -1;
        JSONObject json = new JSONObject(resp);
        JSONArray data = json.optJSONArray("data");
        if (data == null) return -1;
        for (int i = 0; i < data.length(); i++) {
            JSONObject g = data.getJSONObject(i);
            String name = g.optString("name", "");
            long id = g.optLong("id", -1);
            if (id > 0) genreIdCache.put(name, id);
            if (name.equalsIgnoreCase(genreName)) { genreIdCache.put(genreName, id); return id; }
        }
        return -1;
    }

    /**
     * Возвращает список artist_id для данного Deezer genre_id.
     * /genre/{id}/artists — официальный список артистов жанра.
     */
    private static ArrayList<Long> getDeezerGenreArtists(long genreId) throws Exception {
        ArrayList<Long> cached = genreArtistCache.get(genreId);
        if (cached != null && !cached.isEmpty()) return new ArrayList<>(cached);

        String resp = performGetRequest("https://api.deezer.com/genre/" + genreId + "/artists");
        if (resp.isEmpty()) return new ArrayList<>();
        JSONArray data = new JSONObject(resp).optJSONArray("data");
        if (data == null) return new ArrayList<>();
        ArrayList<Long> ids = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            long id = data.getJSONObject(i).optLong("id", -1);
            if (id > 0) ids.add(id);
        }
        genreArtistCache.put(genreId, ids);
        return ids;
    }

    /**
     * Треки для жанровой волны через Deezer genre API:
     * 1. Получаем genre_id
     * 2. Берём артистов жанра
     * 3. Для каждого артиста берём их ТОП-треки (НЕ radio — radio дрейфует в другие жанры)
     *
     * artist/top гарантирует что треки принадлежат артистам именно этого жанра.
     * artist/radio — Deezer "похожие треки", уходит в смежные жанры.
     */
    private static ArrayList<String> getTracksFromDeezerGenre(String genreRu, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        long genreId = getDeezerGenreId(genreRu);

        if (genreId > 0) {
            ArrayList<Long> artistIds = getDeezerGenreArtists(genreId);
            if (!artistIds.isEmpty()) {
                Collections.shuffle(artistIds);
                // Берём больше артистов, но меньше треков каждого — шире охват жанра
                int pick = Math.min(12, artistIds.size());
                int perArtist = Math.max(3, (limit / pick) + 2);
                for (int i = 0; i < pick && res.size() < limit; i++) {
                    long artistId = artistIds.get(i);
                    // artist/top — топ-треки артиста, не выходят за жанр
                    String topResp = performGetRequest(
                            "https://api.deezer.com/artist/" + artistId + "/top?limit=" + perArtist);
                    if (topResp.isEmpty()) continue;
                    JSONArray topData = new JSONObject(topResp).optJSONArray("data");
                    if (topData == null) continue;
                    ArrayList<JSONObject> items = new ArrayList<>();
                    for (int j = 0; j < topData.length(); j++) items.add(topData.getJSONObject(j));
                    Collections.shuffle(items);
                    for (JSONObject t : items) {
                        String art = t.has("artist") ? t.getJSONObject("artist").optString("name", "") : "";
                        String tit = t.optString("title", "");
                        if (art.isEmpty() || tit.isEmpty()) continue;
                        synchronized (historyLock) {
                            if (globalPlayHistory.add(art + tit)) res.add(art + " - " + tit);
                        }
                        if (res.size() >= limit) return res;
                    }
                }
            }
        }

        // Fallback только если genre_id не найден — не добавляем random плейлисты как дефолт
        // (плейлисты по keyword используются отдельно в generateWaveFromSettings)
        return res;
    }

    /**
     * Deezer Radio stations — тематические радиостанции Deezer.
     * /radio/genres — список всех жанровых радиостанций.
     * Это чистый жанровый контент без какой-либо фильтрации.
     */
    private static ArrayList<String> getTracksFromDeezerRadioStation(String genreRu, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        String genreName = getDeezerGenreName(genreRu);
        if (genreName == null) genreName = getDeezerSearchQuery(genreRu);

        // Получаем список жанровых радиостанций
        String resp = performGetRequest("https://api.deezer.com/radio/genres");
        if (resp.isEmpty()) return res;
        JSONArray data = new JSONObject(resp).optJSONArray("data");
        if (data == null) return res;

        // Ищем подходящую станцию по названию жанра
        long stationId = -1;
        String finalGenreName = genreName;
        for (int i = 0; i < data.length(); i++) {
            JSONObject station = data.getJSONObject(i);
            String name = station.optString("title", "");
            if (name.toLowerCase().contains(finalGenreName.toLowerCase())
                    || finalGenreName.toLowerCase().contains(name.toLowerCase())) {
                stationId = station.optLong("id", -1);
                break;
            }
        }
        if (stationId == -1) return res; // станция не найдена — не берём случайную

        String tracksResp = performGetRequest("https://api.deezer.com/radio/" + stationId + "/tracks?limit=50");
        if (tracksResp.isEmpty()) return res;
        JSONArray tracks = new JSONObject(tracksResp).optJSONArray("data");
        if (tracks == null) return res;

        ArrayList<JSONObject> items = new ArrayList<>();
        for (int i = 0; i < tracks.length(); i++) items.add(tracks.getJSONObject(i));
        Collections.shuffle(items);
        for (JSONObject t : items) {
            String art = t.has("artist") ? t.getJSONObject("artist").optString("name", "") : "";
            String tit = t.optString("title", "");
            if (art.isEmpty() || tit.isEmpty()) continue;
            synchronized (historyLock) {
                if (globalPlayHistory.add(art + tit)) res.add(art + " - " + tit);
            }
            if (res.size() >= limit) return res;
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Seed-based track radio — основа жанровой строгости
    // Берём хардкодные seed-треки жанра (гарантированно правильный жанр),
    // для каждого запускаем track/radio → Deezer возвращает треки похожие
    // именно на ЭТО произведение, а не на "артиста в целом".
    // Это намного точнее чем artist/radio который дрейфует в смежные жанры.
    // ─────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getGenreSeedRadioTracks(String genre, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        ArrayList<String> seeds = getSeedTracksForGenre(genre);
        if (seeds.isEmpty()) return res;
        Collections.shuffle(seeds);
        int pick = Math.min(6, seeds.size());
        CountDownLatch latch = new CountDownLatch(pick);
        ConcurrentLinkedQueue<String> q = new ConcurrentLinkedQueue<>();
        int perSeed = Math.max(4, (limit / pick) + 2);
        for (int i = 0; i < pick; i++) {
            final String seed = seeds.get(i);
            backgroundExecutor.execute(() -> {
                try {
                    String[] p = seed.split(" - ", 2);
                    q.addAll(getDeezerTrackRadio(p[0].trim(), p.length > 1 ? p[1].trim() : "", perSeed));
                } catch (Exception ignored) {} finally { latch.countDown(); }
            });
        }
        latch.await(8, TimeUnit.SECONDS);
        res.addAll(q);
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────
    // generateWaveFromSettings (MODE 0 — жанровая волна)
    // ─────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────
    // generateWaveFromSettings (MODE 0 — жанровая волна, InnerTube YTM)
    //
    // Архитектура:
    //   1. Seed-трек из getSeedTracksForGenre → searchYtmVideoId → якорный videoId
    //   2. YTM /next от якоря → 30-50 похожих треков
    //   3. Фильтр: 7-дневная история (isTrackAllowed7Days) + сессионная дедупликация
    //   4. Резолв через Hitmos (MusicWebSearcher.searchTrackStrict)
    //   5. Пуш в плеер поштучно (processAndReturnTracks)
    //   6. Бесконечный рефилл через continuationToken;
    //      каждые YTM_ANTI_DRIFT_THRESHOLD треков — новый якорь (Anti-Drift)
    // ─────────────────────────────────────────────────────────────────────

    public static void generateWaveFromSettings(int currentAccount, WaveCallback callback) {
        startNewWaveSession(0);
        backgroundExecutor.execute(() -> {
            try {
                String genre = currentConfig.query;

                // Удаляем протухшие записи истории при каждом запуске
                pruneOldHistory();

                // ── Шаг 1: Найти якорный videoId через YTM search ──────────────────
                String anchorVideoId = refreshYtmAnchor(genre);

                if (anchorVideoId == null) {
                    // YTM недоступен — деградируем к Deezer
                    FileLog.e("SmartRadio [Init]: YTM недоступен, переключаемся на Deezer fallback");
                    ArrayList<String> fallback = new ArrayList<>();
                    fallback.addAll(getTracksFromDeezerGenre(genre, 20));
                    fallback.addAll(getRussianTracksForGenre(genre, 10));
                    Collections.shuffle(fallback);
                    if (cancelFlag.get()) return;
                    processAndReturnTracks(fallback, currentAccount, true, callback);
                    startInfiniteRefill(currentAccount, callback, () -> {
                        ArrayList<String> r = new ArrayList<>();
                        try {
                            r.addAll(getTracksFromDeezerGenre(genre, 20));
                            r.addAll(getRussianTracksForGenre(genre, 10));
                            Collections.shuffle(r);
                        } catch (Exception ignored) {}
                        return r;
                    });
                    return;
                }

                synchronized (ytmLock) {
                    ytmCurrentVideoId    = anchorVideoId;
                    ytmContinuationToken = null;
                }
                ytmTracksSinceAnchor.set(0);
                FileLog.d("SmartRadio [Init]: Запуск волны жанра: " + genre
                        + ". Выбран якорь: " + anchorVideoId);

                // ── Шаг 2: Первый батч — YTM /next от якоря ────────────────────────
                YtmNextResult result1 = getYtmRecommendations(anchorVideoId, null);
                ArrayList<String> initial = new ArrayList<>();

                if (result1 != null) {
                    synchronized (ytmLock) { ytmContinuationToken = result1.continuationToken; }
                    int accepted = 0;
                    for (String t : result1.tracks) {
                        String[] p = t.split(" - ", 2);
                        String a   = p[0].trim();
                        String tit = p.length > 1 ? p[1].trim() : "";
                        if (!isTrackAllowed7Days(a, tit)) {
                            FileLog.w("SmartRadio [Filter]: Трек " + a + " - " + tit
                                    + " пропущен (был прослушан менее 7 дней назад).");
                            continue;
                        }
                        initial.add(t);
                        accepted++;
                    }
                    FileLog.d("SmartRadio [Queue]: В очередь добавлено " + accepted
                            + " свежих треков (батч 1).");
                }

                // Дополняем RU-контентом из жанровых Deezer-плейлистов
                try { initial.addAll(getRussianTracksForGenre(genre, 8)); }
                catch (Exception ignored) {}

                Collections.shuffle(initial);
                if (cancelFlag.get()) return;
                processAndReturnTracks(initial, currentAccount, true, callback);

                // ── Шаг 3: Второй батч — продолжаем через continuationToken ─────────
                String token2;
                synchronized (ytmLock) { token2 = ytmContinuationToken; }

                YtmNextResult result2 = getYtmRecommendations(null, token2);
                ArrayList<String> second = new ArrayList<>();

                if (result2 != null) {
                    synchronized (ytmLock) { ytmContinuationToken = result2.continuationToken; }
                    int accepted = 0;
                    for (String t : result2.tracks) {
                        String[] p = t.split(" - ", 2);
                        String a   = p[0].trim();
                        String tit = p.length > 1 ? p[1].trim() : "";
                        if (!isTrackAllowed7Days(a, tit)) {
                            FileLog.w("SmartRadio [Filter]: Трек " + a + " - " + tit
                                    + " пропущен (был прослушан менее 7 дней назад).");
                            continue;
                        }
                        second.add(t);
                        accepted++;
                    }
                    FileLog.d("SmartRadio [Queue]: В очередь добавлено " + accepted
                            + " свежих треков (батч 2).");
                }

                try { second.addAll(getRussianTracksForGenre(genre, 10)); }
                catch (Exception ignored) {}
                Collections.shuffle(second);

                if (cancelFlag.get()) return;
                processAndReturnTracks(second, currentAccount, false, callback);

                // ── Бесконечный рефилл ─────────────────────────────────────────────
                final String genreFinal = genre;
                startInfiniteRefill(currentAccount, callback, () -> {
                    ArrayList<String> r = new ArrayList<>();
                    try {
                        // Anti-Drift: каждые N треков берём новый якорь
                        if (ytmTracksSinceAnchor.get() >= YTM_ANTI_DRIFT_THRESHOLD) {
                            FileLog.d("SmartRadio [Anti-Drift]: Сброс ветки рекомендаций, "
                                    + "поиск нового якоря для удержания жанра.");
                            String newVid = refreshYtmAnchor(genreFinal);
                            synchronized (ytmLock) {
                                ytmCurrentVideoId    = newVid;
                                ytmContinuationToken = null;
                            }
                            ytmTracksSinceAnchor.set(0);
                        }

                        String vid, tok;
                        synchronized (ytmLock) {
                            vid = ytmCurrentVideoId;
                            tok = ytmContinuationToken;
                        }

                        YtmNextResult refill = (tok != null && !tok.isEmpty())
                                ? getYtmRecommendations(null, tok)
                                : getYtmRecommendations(vid, null);

                        if (refill != null) {
                            synchronized (ytmLock) { ytmContinuationToken = refill.continuationToken; }
                            int accepted = 0;
                            for (String t : refill.tracks) {
                                String[] p = t.split(" - ", 2);
                                String a   = p[0].trim();
                                String tit = p.length > 1 ? p[1].trim() : "";
                                if (!isTrackAllowed7Days(a, tit)) {
                                    FileLog.w("SmartRadio [Filter]: Трек " + a + " - " + tit
                                            + " пропущен (был прослушан менее 7 дней назад).");
                                    continue;
                                }
                                r.add(t);
                                accepted++;
                            }
                            FileLog.d("SmartRadio [Queue]: Рефилл добавил " + accepted
                                    + " свежих треков.");
                        }
                    } catch (Exception e) {
                        FileLog.e("SmartRadio [Refill]: ошибка рефилла MODE 0", e);
                    }
                    return r;
                });

            } catch (Exception e) { handleError(callback, e); }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // generateWaveByPlaylist (MODE 3)
    // ─────────────────────────────────────────────────────────────────────

    public static void generateWaveByPlaylist(ArrayList<MessageObject> originalPlaylist, int currentAccount, WaveCallback callback) {
        startNewWaveSession(3);
        if (originalPlaylist != null) {
            synchronized (basePlaylistContext) { basePlaylistContext.addAll(originalPlaylist); }
        }
        backgroundExecutor.execute(() -> {
            try {
                ArrayList<MessageObject> ctx;
                synchronized (basePlaylistContext) { ctx = new ArrayList<>(basePlaylistContext); }
                synchronized (waveSeedKeys) {
                    waveSeedKeys.clear();
                    for (MessageObject s : ctx) waveSeedKeys.add(normalizeSeedKey(s.getMusicAuthor(), s.getMusicTitle()));
                }
                if (ctx.isEmpty()) throw new Exception("Плейлист пуст");

                Collections.shuffle(ctx);
                int seedCount = Math.min(10, ctx.size());
                ArrayList<MessageObject> seeds = new ArrayList<>(ctx.subList(0, seedCount));

                // ── Первый батч: параллельно из 3 треков ──────────────────────────
                CountDownLatch latch1 = new CountDownLatch(Math.min(3, seeds.size()));
                ConcurrentLinkedQueue<String> q1 = new ConcurrentLinkedQueue<>();
                for (int i = 0; i < Math.min(3, seeds.size()); i++) {
                    final MessageObject seed = seeds.get(i);
                    backgroundExecutor.execute(() -> {
                        try {
                            q1.addAll(getDeezerTrackRadio(seed.getMusicAuthor(), seed.getMusicTitle(), 6));
                            q1.addAll(getDeezerRelatedArtistTracks(seed.getMusicAuthor(), 4));
                        } catch (Exception ignored) {} finally { latch1.countDown(); }
                    });
                }
                latch1.await(7, TimeUnit.SECONDS);
                ArrayList<String> initial = new ArrayList<>(q1);
                Collections.shuffle(initial);
                if (initial.size() < 5) initial.addAll(getTracksFromDeezerCharts(5));

                if (cancelFlag.get()) return;
                processAndReturnTracks(initial, currentAccount, true, callback);

                // ── Второй батч: все seed-треки ────────────────────────────────────
                CountDownLatch latch2 = new CountDownLatch(Math.min(10, seeds.size()));
                ConcurrentLinkedQueue<String> q2 = new ConcurrentLinkedQueue<>();
                for (int i = 0; i < Math.min(10, seeds.size()); i++) {
                    final MessageObject seed = seeds.get(i);
                    backgroundExecutor.execute(() -> {
                        try {
                            q2.addAll(getDeezerTrackRadio(seed.getMusicAuthor(), seed.getMusicTitle(), 8));
                            q2.addAll(getDeezerRelatedArtistTracks(seed.getMusicAuthor(), 5));
                            q2.addAll(getLastFmSimilarByTrack(seed.getMusicAuthor(), seed.getMusicTitle(), 4));
                        } catch (Exception ignored) {} finally { latch2.countDown(); }
                    });
                }
                latch2.await(12, TimeUnit.SECONDS);
                ArrayList<String> next = new ArrayList<>(q2);
                Collections.shuffle(next);

                if (cancelFlag.get()) return;
                processAndReturnTracks(next, currentAccount, false, callback);

                // ── Рефилл ────────────────────────────────────────────────────────
                startInfiniteRefill(currentAccount, callback, () -> {
                    ArrayList<String> r = new ArrayList<>();
                    try {
                        ArrayList<MessageObject> seedsCopy = new ArrayList<>(seeds);
                        Collections.shuffle(seedsCopy);
                        int pick = Math.min(4, seedsCopy.size());
                        CountDownLatch rl = new CountDownLatch(pick);
                        ConcurrentLinkedQueue<String> rq = new ConcurrentLinkedQueue<>();
                        for (int i = 0; i < pick; i++) {
                            final MessageObject s = seedsCopy.get(i);
                            backgroundExecutor.execute(() -> {
                                try {
                                    rq.addAll(getDeezerTrackRadio(s.getMusicAuthor(), s.getMusicTitle(), 10));
                                    rq.addAll(getDeezerRelatedArtistTracks(s.getMusicAuthor(), 6));
                                } catch (Exception ignored) {} finally { rl.countDown(); }
                            });
                        }
                        rl.await(10, TimeUnit.SECONDS);
                        r.addAll(rq);
                        if (r.size() < 10) r.addAll(getTracksFromDeezerCharts(10));
                        Collections.shuffle(r);
                    } catch (Exception ignored) {}
                    return r;
                });

            } catch (Exception e) { handleError(callback, e); }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // generateWaveByArtist (MODE 1)
    // ─────────────────────────────────────────────────────────────────────

    public static void generateWaveByArtist(String artistName, int currentAccount, WaveCallback callback) {
        startNewWaveSession(1);
        currentConfig.query = artistName;

        backgroundExecutor.execute(() -> {
            try {
                synchronized (waveSeedKeys) {
                    waveSeedKeys.clear();
                    waveSeedKeys.add(normalizeSeedKey(artistName, ""));
                }

                // Топ треки артиста
                ArrayList<String> artistTopTracks = getArtistTopTracks(artistName, 10);
                final ArrayList<String> seedTracks = artistTopTracks.isEmpty()
                        ? getDeezerArtistMix(artistName, 5)
                        : artistTopTracks;

                synchronized (waveSeedKeys) {
                    for (String t : seedTracks) {
                        String[] p = t.split(" - ", 2);
                        waveSeedKeys.add(normalizeSeedKey(p[0].trim(), p.length > 1 ? p[1].trim() : ""));
                    }
                }

                // ── Первый батч ────────────────────────────────────────────────────
                CountDownLatch latch1 = new CountDownLatch(Math.min(3, seedTracks.size()));
                ConcurrentLinkedQueue<String> q1 = new ConcurrentLinkedQueue<>();
                for (int i = 0; i < Math.min(3, seedTracks.size()); i++) {
                    final String st = seedTracks.get(i);
                    backgroundExecutor.execute(() -> {
                        try {
                            String[] p = st.split(" - ", 2);
                            q1.addAll(getDeezerTrackRadio(p[0].trim(), p.length > 1 ? p[1].trim() : "", 6));
                        } catch (Exception ignored) {} finally { latch1.countDown(); }
                    });
                }
                latch1.await(7, TimeUnit.SECONDS);
                ArrayList<String> initial = new ArrayList<>(q1);
                // Добавляем похожих артистов
                initial.addAll(getDeezerRelatedArtistTracks(artistName, 8));
                Collections.shuffle(initial);
                if (initial.size() < 5) initial.addAll(getDeepSimilarTracks(artistName, 5));

                if (cancelFlag.get()) return;
                processAndReturnTracks(initial, currentAccount, true, callback);

                // ── Второй батч: все seed-треки + Last.fm ─────────────────────────
                CountDownLatch latch2 = new CountDownLatch(seedTracks.size());
                ConcurrentLinkedQueue<String> q2 = new ConcurrentLinkedQueue<>();
                for (String st : seedTracks) {
                    backgroundExecutor.execute(() -> {
                        try {
                            String[] p = st.split(" - ", 2);
                            q2.addAll(getDeezerTrackRadio(p[0].trim(), p.length > 1 ? p[1].trim() : "", 8));
                            q2.addAll(getLastFmSimilarByTrack(p[0].trim(), p.length > 1 ? p[1].trim() : "", 4));
                        } catch (Exception ignored) {} finally { latch2.countDown(); }
                    });
                }
                latch2.await(12, TimeUnit.SECONDS);
                ArrayList<String> next = new ArrayList<>(q2);
                next.addAll(getDeezerRelatedArtistTracks(artistName, 10));
                next.addAll(getDeepSimilarTracks(artistName, 10));
                Collections.shuffle(next);

                if (cancelFlag.get()) return;
                processAndReturnTracks(next, currentAccount, false, callback);

                // ── Рефилл ────────────────────────────────────────────────────────
                startInfiniteRefill(currentAccount, callback, () -> {
                    ArrayList<String> r = new ArrayList<>();
                    try {
                        ArrayList<String> shuffled = new ArrayList<>(seedTracks);
                        Collections.shuffle(shuffled);
                        CountDownLatch rl = new CountDownLatch(Math.min(4, shuffled.size()));
                        ConcurrentLinkedQueue<String> rq = new ConcurrentLinkedQueue<>();
                        for (int i = 0; i < Math.min(4, shuffled.size()); i++) {
                            final String st = shuffled.get(i);
                            backgroundExecutor.execute(() -> {
                                try {
                                    String[] pp = st.split(" - ", 2);
                                    rq.addAll(getDeezerTrackRadio(pp[0].trim(), pp.length > 1 ? pp[1].trim() : "", 10));
                                } catch (Exception ignored) {} finally { rl.countDown(); }
                            });
                        }
                        rl.await(10, TimeUnit.SECONDS);
                        r.addAll(rq);
                        r.addAll(getDeezerRelatedArtistTracks(artistName, 10));
                        r.addAll(getDeepSimilarTracks(artistName, 8));
                        Collections.shuffle(r);
                    } catch (Exception ignored) {}
                    return r;
                });

            } catch (Exception e) { handleError(callback, e); }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // generateWaveByAlbum (MODE 2)
    // ─────────────────────────────────────────────────────────────────────

    public static void generateWaveByAlbum(String artistName, String albumName, int currentAccount, WaveCallback callback) {
        startNewWaveSession(2);
        backgroundExecutor.execute(() -> {
            try {
                String safeArtist = (artistName == null || artistName.isEmpty()) ? "Unknown Artist" : artistName;
                String safeAlbum  = (albumName  == null || albumName.isEmpty())  ? "" : albumName;
                currentConfig.query = safeArtist;
                synchronized (waveSeedKeys) {
                    waveSeedKeys.clear();
                    waveSeedKeys.add(normalizeSeedKey(safeArtist, safeAlbum));
                }

                ArrayList<String> albumTracks = !safeAlbum.isEmpty()
                        ? getTracksFromAlbum(safeArtist, safeAlbum, 10)
                        : new ArrayList<>();
                if (albumTracks.isEmpty()) albumTracks.addAll(getArtistTopTracks(safeArtist, 10));
                final ArrayList<String> seedTracks = albumTracks;

                synchronized (waveSeedKeys) {
                    for (String t : seedTracks) {
                        String[] p = t.split(" - ", 2);
                        waveSeedKeys.add(normalizeSeedKey(p[0].trim(), p.length > 1 ? p[1].trim() : ""));
                    }
                }

                // ── Первый батч ────────────────────────────────────────────────────
                CountDownLatch latch1 = new CountDownLatch(Math.min(3, seedTracks.size()));
                ConcurrentLinkedQueue<String> q1 = new ConcurrentLinkedQueue<>();
                for (int i = 0; i < Math.min(3, seedTracks.size()); i++) {
                    final String st = seedTracks.get(i);
                    backgroundExecutor.execute(() -> {
                        try {
                            String[] p = st.split(" - ", 2);
                            q1.addAll(getDeezerTrackRadio(p[0].trim(), p.length > 1 ? p[1].trim() : "", 6));
                        } catch (Exception ignored) {} finally { latch1.countDown(); }
                    });
                }
                latch1.await(7, TimeUnit.SECONDS);
                ArrayList<String> initial = new ArrayList<>(q1);
                initial.addAll(getDeezerRelatedArtistTracks(safeArtist, 5));
                Collections.shuffle(initial);
                if (initial.size() < 5) initial.addAll(getDeepSimilarTracks(safeArtist, 5));

                if (cancelFlag.get()) return;
                processAndReturnTracks(initial, currentAccount, true, callback);

                // ── Второй батч ────────────────────────────────────────────────────
                String albumGenre = null;
                try { if (!safeAlbum.isEmpty()) albumGenre = getAlbumGenreQuery(safeArtist, safeAlbum); }
                catch (Exception ignored) {}

                CountDownLatch latch2 = new CountDownLatch(seedTracks.size());
                ConcurrentLinkedQueue<String> q2 = new ConcurrentLinkedQueue<>();
                for (String st : seedTracks) {
                    backgroundExecutor.execute(() -> {
                        try {
                            String[] p = st.split(" - ", 2);
                            q2.addAll(getDeezerTrackRadio(p[0].trim(), p.length > 1 ? p[1].trim() : "", 8));
                            q2.addAll(getLastFmSimilarByTrack(p[0].trim(), p.length > 1 ? p[1].trim() : "", 4));
                        } catch (Exception ignored) {} finally { latch2.countDown(); }
                    });
                }
                latch2.await(12, TimeUnit.SECONDS);
                ArrayList<String> next = new ArrayList<>(q2);
                if (albumGenre != null) next.addAll(getTracksFromDynamicPlaylists(albumGenre, 10, random.nextInt(50)));
                next.addAll(getDeezerRelatedArtistTracks(safeArtist, 8));
                Collections.shuffle(next);

                if (cancelFlag.get()) return;
                processAndReturnTracks(next, currentAccount, false, callback);

                final String finalAlbumGenre = albumGenre;
                startInfiniteRefill(currentAccount, callback, () -> {
                    ArrayList<String> r = new ArrayList<>();
                    try {
                        ArrayList<String> shuffled = new ArrayList<>(seedTracks);
                        Collections.shuffle(shuffled);
                        CountDownLatch rl = new CountDownLatch(Math.min(4, shuffled.size()));
                        ConcurrentLinkedQueue<String> rq = new ConcurrentLinkedQueue<>();
                        for (int i = 0; i < Math.min(4, shuffled.size()); i++) {
                            final String st = shuffled.get(i);
                            backgroundExecutor.execute(() -> {
                                try {
                                    String[] pp = st.split(" - ", 2);
                                    rq.addAll(getDeezerTrackRadio(pp[0].trim(), pp.length > 1 ? pp[1].trim() : "", 10));
                                } catch (Exception ignored) {} finally { rl.countDown(); }
                            });
                        }
                        rl.await(10, TimeUnit.SECONDS);
                        r.addAll(rq);
                        if (finalAlbumGenre != null) r.addAll(getTracksFromDynamicPlaylists(finalAlbumGenre, 8, random.nextInt(300)));
                        r.addAll(getDeezerRelatedArtistTracks(safeArtist, 8));
                        if (r.size() < 10) r.addAll(getTracksFromDeezerCharts(10));
                        Collections.shuffle(r);
                    } catch (Exception ignored) {}
                    return r;
                });

            } catch (Exception e) { handleError(callback, e); }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // loadMoreForCurrentWave
    // ─────────────────────────────────────────────────────────────────────

    public static void loadMoreForCurrentWave(MessageObject currentTrack, int currentAccount, WaveCallback callback) {
        if (isLoading) return;
        isLoading = true;
        isWaveActive = true;

        backgroundExecutor.execute(() -> {
            try {
                List<MessageObject> batch = lastGeneratedBatch;
                MessageObject seed = (!batch.isEmpty()) ? batch.get(random.nextInt(batch.size())) : currentTrack;
                ArrayList<String> candidates = new ArrayList<>();

                switch (currentWaveMode) {
                    case 0: {
                        // Жанровая волна: загружаем следующую страницу через YTM /next
                        String genre0 = currentConfig.query;
                        try {
                            String vid0, tok0;
                            synchronized (ytmLock) {
                                vid0 = ytmCurrentVideoId;
                                tok0 = ytmContinuationToken;
                            }
                            YtmNextResult res0 = (tok0 != null && !tok0.isEmpty())
                                    ? getYtmRecommendations(null, tok0)
                                    : getYtmRecommendations(vid0, null);
                            if (res0 != null) {
                                synchronized (ytmLock) { ytmContinuationToken = res0.continuationToken; }
                                for (String t : res0.tracks) {
                                    String[] p  = t.split(" - ", 2);
                                    String a    = p[0].trim();
                                    String tit  = p.length > 1 ? p[1].trim() : "";
                                    if (isTrackAllowed7Days(a, tit)) candidates.add(t);
                                }
                            }
                        } catch (Exception ignored) {}
                        // Дополняем RU-контентом
                        try { candidates.addAll(getRussianTracksForGenre(genre0, 8)); }
                        catch (Exception ignored) {}
                        break;
                    }
                    case 1: case 2: case 3:
                        if (seed != null) {
                            candidates.addAll(getDeezerTrackRadio(seed.getMusicAuthor(), seed.getMusicTitle(), 15));
                            candidates.addAll(getDeezerRelatedArtistTracks(seed.getMusicAuthor(), 10));
                        }
                        candidates.addAll(getDeepSimilarTracks(seed != null ? seed.getMusicAuthor() : "", 10));
                        if (candidates.size() < 10) candidates.addAll(getTracksFromDeezerCharts(10));
                        break;
                }

                if (candidates.isEmpty()) candidates.addAll(getTracksFromDeezerCharts(20));
                Collections.shuffle(candidates);

                if (cancelFlag.get()) return;
                processAndReturnTracks(candidates, currentAccount, false, callback);
            } catch (Exception e) { handleError(callback, e); }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Бесконечный рефилл
    // ─────────────────────────────────────────────────────────────────────

    interface CandidateSupplier { ArrayList<String> get(); }

    private static void startInfiniteRefill(int currentAccount, WaveCallback callback, CandidateSupplier supplier) {
        backgroundExecutor.execute(() -> {
            int round = 0;
            while (isWaveActive && !cancelFlag.get()) {
                try {
                    long delay = Math.min(5000L + round * 500L, 12000L);
                    Thread.sleep(delay);
                    if (!isWaveActive || cancelFlag.get()) break;

                    // Пауза пока плеер на паузе
                    while (isWaveActive && !cancelFlag.get()
                            && MediaController.getInstance().isMessagePaused()) {
                        Thread.sleep(500);
                    }
                    if (!isWaveActive || cancelFlag.get()) break;

                    ArrayList<String> candidates = supplier.get();

                    // Дополнительный режимо-специфичный рефилл
                    if (currentWaveMode == 0) {
                        // Жанровая волна: каждые 2 рефилла добавляем RU контент
                        if (round % 2 == 0) {
                            try { candidates.addAll(getRussianTracksForGenre(currentConfig.query, 10)); }
                            catch (Exception ignore) {}
                        }
                    } else if (currentWaveMode == 1) {
                        // Волна по артисту: related artists
                        try { candidates.addAll(getDeezerRelatedArtistTracks(currentConfig.query, 10)); }
                        catch (Exception ignore) {}
                    } else if (currentWaveMode == 3) {
                        // Волна по плейлисту: track radio из пула seed
                        synchronized (basePlaylistContext) {
                            List<MessageObject> ctx = new ArrayList<>(basePlaylistContext);
                            if (!ctx.isEmpty()) {
                                MessageObject s = ctx.get(random.nextInt(ctx.size()));
                                try { candidates.addAll(getDeezerTrackRadio(s.getMusicAuthor(), s.getMusicTitle(), 8)); }
                                catch (Exception ignore) {}
                            }
                        }
                    }

                    if (candidates.isEmpty()) { round++; continue; }
                    Collections.shuffle(candidates);
                    processAndReturnTracks(candidates, currentAccount, false, callback);
                    round++;
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // processAndReturnTracks — отдача треков поштучно
    // ─────────────────────────────────────────────────────────────────────

    private static void processAndReturnTracks(ArrayList<String> queries, int account, boolean isInitial, WaveCallback callback) {
        if (queries.isEmpty()) return;
        if (isInitial) lastGeneratedBatch.clear();

        java.util.Set<String> batchSeen = Collections.synchronizedSet(new HashSet<>());
        AtomicBoolean initialFired = new AtomicBoolean(false);
        AtomicInteger pending = new AtomicInteger(queries.size());

        for (String q : queries) {
            backgroundExecutor.execute(() -> {
                try {
                    String[] parts = q.split(" - ", 2);
                    String artist = parts[0].trim();
                    String title  = parts.length > 1 ? parts[1].trim() : "";

                    String dedupeKey = (artist + title).toLowerCase().replaceAll("[^a-zа-яё0-9]", "");
                    if (!batchSeen.add(dedupeKey)) return;

                    if (isTrackFiltered(artist, title)) return;

                    // ── Жанровый whitelist-фильтр для MODE 0 ──────────────────────────
                    // Мягкий фильтр: пропускаем артиста если:
                    //  a) он есть в seed-whitelist хардкодных треков (приоритет), ИЛИ
                    //  b) он есть в Deezer genre artist list, ИЛИ
                    //  c) имя артиста содержит кириллицу (RU артист — в Deezer list неполно)
                    //  d) имя содержит CJK (к-поп жанр)
                    synchronized (recentArtistsWindow) {
                        String ak = artist.toLowerCase().trim();
                        if (!ak.isEmpty() && recentArtistsWindow.contains(ak)) return;
                    }
                    synchronized (artistStreakCounter) {
                        String ak = artist.toLowerCase().trim();
                        if (!ak.isEmpty() && artistStreakCounter.getOrDefault(ak, 0) >= 2) return;
                    }
                    synchronized (recentTitlesWindow) {
                        String tk = title.toLowerCase().trim();
                        if (!tk.isEmpty() && recentTitlesWindow.contains(tk)) return;
                    }
                    String seedKey = normalizeSeedKey(artist, title);
                    synchronized (waveSeedKeys) {
                        boolean artistSeedMatch = false;
                        if (currentWaveMode == 1) {
                            String ap = normalizeSeedKey(artist, "");
                            for (String k : waveSeedKeys) {
                                if (!ap.isEmpty() && k.startsWith(ap)) { artistSeedMatch = true; break; }
                            }
                        }
                        if (isInitial && waveSeedKeys.contains(seedKey)) return;
                        if (isInitial && artistSeedMatch) return;
                        if (!isInitial && waveSeedKeys.contains(seedKey) && random.nextInt(100) >= 10) return;
                        if (!isInitial && artistSeedMatch && random.nextInt(100) >= 18) return;
                    }

                    MusicWebSearcher.WebTrack wt = MusicWebSearcher.searchTrackStrict(artist, title, 0);
                    if (wt == null || wt.downloadUrl == null || wt.downloadUrl.isEmpty()) {
                        FileLog.e("SmartRadio [Hitmos]: Ошибка парсинга / не найден трек " + title);
                        return;
                    }
                    FileLog.d("SmartRadio [Hitmos]: Успешно найден mp3 для " + title
                            + ", отправка в монолитный плеер.");

                    int dur = ChannelMusicFragment.parseDuration(wt.duration);
                    MessageObject msg = ChannelMusicFragment.createWebMessageObject(
                            account, wt.title, wt.artist, wt.downloadUrl, dur,
                            -(long) wt.downloadUrl.hashCode(), wt.coverUrl);

                    lastGeneratedBatch.add(msg);
                    waveTrackCount++;
                    // MODE 0: персистентная 7-дневная история + счётчик Anti-Drift
                    if (currentWaveMode == 0) {
                        recordTrackInHistory(artist, title);
                        ytmTracksSinceAnchor.incrementAndGet();
                    }

                    synchronized (recentArtistsWindow) {
                        String ak = wt.artist == null ? "" : wt.artist.toLowerCase().trim();
                        if (!ak.isEmpty()) {
                            recentArtistsWindow.add(ak);
                            if (recentArtistsWindow.size() > RECENT_ARTISTS_WINDOW_SIZE) recentArtistsWindow.remove(0);
                        }
                    }
                    synchronized (artistStreakCounter) {
                        String ca = wt.artist == null ? "" : wt.artist.toLowerCase().trim();
                        if (!ca.isEmpty()) {
                            for (String k : new ArrayList<>(artistStreakCounter.keySet())) {
                                if (!k.equals(ca)) {
                                    int v = artistStreakCounter.getOrDefault(k, 0) - 1;
                                    if (v <= 0) artistStreakCounter.remove(k);
                                    else artistStreakCounter.put(k, v);
                                }
                            }
                            artistStreakCounter.put(ca, artistStreakCounter.getOrDefault(ca, 0) + 1);
                        }
                    }
                    synchronized (recentTitlesWindow) {
                        String tk = wt.title == null ? "" : wt.title.toLowerCase().trim();
                        if (!tk.isEmpty()) {
                            recentTitlesWindow.add(tk);
                            if (recentTitlesWindow.size() > RECENT_TITLES_WINDOW_SIZE) recentTitlesWindow.remove(0);
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

    // ─────────────────────────────────────────────────────────────────────
    // Deezer: track radio — похожие на конкретный трек
    // ─────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getDeezerTrackRadio(String artist, String title, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        String q = URLEncoder.encode(artist + " " + title, "UTF-8");
        String searchResp = performGetRequest("https://api.deezer.com/search?q=" + q + "&limit=1");
        if (searchResp.isEmpty()) return res;
        JSONArray data = new JSONObject(searchResp).optJSONArray("data");
        if (data == null || data.length() == 0) return res;
        long trackId = data.getJSONObject(0).getLong("id");

        String radioResp = performGetRequest("https://api.deezer.com/track/" + trackId + "/radio?limit=" + Math.min(limit * 2, 40));
        if (radioResp.isEmpty()) return res;
        JSONArray radioData = new JSONObject(radioResp).optJSONArray("data");
        if (radioData == null) return res;

        ArrayList<JSONObject> items = new ArrayList<>();
        for (int i = 0; i < radioData.length(); i++) items.add(radioData.getJSONObject(i));
        Collections.shuffle(items);
        for (JSONObject t : items) {
            String art = t.has("artist") ? t.getJSONObject("artist").optString("name", "") : "";
            String tit = t.optString("title", "");
            if (art.isEmpty() || tit.isEmpty()) continue;
            synchronized (historyLock) {
                if (globalPlayHistory.add(art + tit)) res.add(art + " - " + tit);
            }
            if (res.size() >= limit) return res;
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Deezer: related artists → их топ-треки
    // ─────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getDeezerRelatedArtistTracks(String artist, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        String artistResp = performGetRequest("https://api.deezer.com/search/artist?q=" + URLEncoder.encode(artist, "UTF-8") + "&limit=1");
        if (artistResp.isEmpty()) return res;
        JSONArray artistData = new JSONObject(artistResp).optJSONArray("data");
        if (artistData == null || artistData.length() == 0) return res;
        long artistId = artistData.getJSONObject(0).getLong("id");

        String relResp = performGetRequest("https://api.deezer.com/artist/" + artistId + "/related?limit=8");
        if (relResp.isEmpty()) return res;
        JSONArray related = new JSONObject(relResp).optJSONArray("data");
        if (related == null) return res;

        ArrayList<JSONObject> relList = new ArrayList<>();
        for (int i = 0; i < related.length(); i++) relList.add(related.getJSONObject(i));
        Collections.shuffle(relList);

        for (int i = 0; i < Math.min(4, relList.size()) && res.size() < limit; i++) {
            long relId = relList.get(i).getLong("id");
            String topResp = performGetRequest("https://api.deezer.com/artist/" + relId + "/top?limit=10");
            if (topResp.isEmpty()) continue;
            JSONArray top = new JSONObject(topResp).optJSONArray("data");
            if (top == null) continue;
            ArrayList<JSONObject> topList = new ArrayList<>();
            for (int j = 0; j < top.length(); j++) topList.add(top.getJSONObject(j));
            Collections.shuffle(topList);
            for (JSONObject t : topList) {
                String art = t.has("artist") ? t.getJSONObject("artist").optString("name", "") : "";
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

    // ─────────────────────────────────────────────────────────────────────
    // Deezer: dynamic playlists (поиск по ключевому слову)
    // ─────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getTracksFromDynamicPlaylists(String query, int target, int offset) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        ArrayList<String> olderTracks = new ArrayList<>();
        int playlistOffset = offset % 300;
        String url = "https://api.deezer.com/search/playlist?q=" + URLEncoder.encode(query, "UTF-8") + "&limit=8&index=" + playlistOffset;
        JSONObject json = new JSONObject(performGetRequest(url));
        if (!json.has("data")) return res;
        JSONArray playlists = json.getJSONArray("data");
        ArrayList<JSONObject> pList = new ArrayList<>();
        for (int i = 0; i < playlists.length(); i++) pList.add(playlists.getJSONObject(i));
        Collections.shuffle(pList);
        for (JSONObject pl : pList) {
            int trackOffset = random.nextInt(50);
            String tUrl = "https://api.deezer.com/playlist/" + pl.getLong("id") + "/tracks?limit=30&index=" + trackOffset;
            String resp = performGetRequest(tUrl);
            if (resp.isEmpty()) continue;
            JSONObject pObj = new JSONObject(resp);
            if (!pObj.has("data")) continue;
            JSONArray tracks = pObj.getJSONArray("data");
            ArrayList<JSONObject> tList = new ArrayList<>();
            for (int j = 0; j < tracks.length(); j++) tList.add(tracks.getJSONObject(j));
            Collections.shuffle(tList);
            for (JSONObject t : tList) {
                String art = t.getJSONObject("artist").getString("name");
                String tit = t.getString("title");
                String key = art + tit;
                synchronized (historyLock) { if (!globalPlayHistory.add(key)) continue; }
                String candidate = art + " - " + tit;
                int trackYear = 0;
                if (t.has("release_date") && !t.getString("release_date").isEmpty()) {
                    try { trackYear = Integer.parseInt(t.getString("release_date").substring(0, 4)); } catch (Exception ignored) {}
                }
                if (trackYear == 0 && t.has("album")) {
                    try {
                        long albumId = t.getJSONObject("album").optLong("id", 0);
                        if (albumId > 0) {
                            Integer cy = albumYearCache.get(albumId);
                            if (cy != null) {
                                trackYear = cy;
                            } else {
                                String aResp = performGetRequest("https://api.deezer.com/album/" + albumId);
                                if (!aResp.isEmpty()) {
                                    String rd = new JSONObject(aResp).optString("release_date", "");
                                    int py = 0;
                                    if (rd != null && rd.length() >= 4) {
                                        try { py = Integer.parseInt(rd.substring(0, 4)); } catch (Exception ignore) {}
                                    }
                                    albumYearCache.put(albumId, py);
                                    trackYear = py;
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
        for (String old : olderTracks) {
            if (res.size() >= target) break;
            res.add(old);
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Deezer: artist radio + related (getDeepSimilarTracks)
    // ─────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getDeepSimilarTracks(String artist, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        if (artist == null || artist.isEmpty()) return res;
        String url = "https://api.deezer.com/search/artist?q=" + URLEncoder.encode(artist, "UTF-8");
        String resp = performGetRequest(url);
        if (resp.isEmpty()) return res;
        JSONArray data = new JSONObject(resp).optJSONArray("data");
        if (data == null || data.length() == 0) return res;
        long id = data.getJSONObject(0).getLong("id");

        JSONArray radio = new JSONObject(performGetRequest("https://api.deezer.com/artist/" + id + "/radio")).optJSONArray("data");
        if (radio != null) {
            for (int i = 0; i < radio.length(); i++) {
                JSONObject t = radio.getJSONObject(i);
                String a = t.getJSONObject("artist").getString("name");
                String tit = t.getString("title");
                synchronized (historyLock) { if (globalPlayHistory.add(a + tit)) res.add(a + " - " + tit); }
                if (res.size() >= limit) return res;
            }
        }
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
                        synchronized (historyLock) { if (globalPlayHistory.add(a + tit)) res.add(a + " - " + tit); }
                        if (res.size() >= limit) return res;
                    }
                }
            }
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Deezer: топ-треки артиста
    // ─────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getArtistTopTracks(String artist, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        String artistResp = performGetRequest("https://api.deezer.com/search/artist?q=" + URLEncoder.encode(artist, "UTF-8") + "&limit=1");
        if (artistResp.isEmpty()) return res;
        JSONArray artistData = new JSONObject(artistResp).optJSONArray("data");
        if (artistData == null || artistData.length() == 0) return res;
        long artistId = artistData.getJSONObject(0).getLong("id");
        String topResp = performGetRequest("https://api.deezer.com/artist/" + artistId + "/top?limit=" + Math.min(limit, 50));
        if (topResp.isEmpty()) return res;
        JSONArray data = new JSONObject(topResp).optJSONArray("data");
        if (data == null) return res;
        for (int i = 0; i < data.length(); i++) {
            JSONObject t = data.getJSONObject(i);
            String art = t.has("artist") ? t.getJSONObject("artist").optString("name", artist) : artist;
            String tit = t.optString("title", "");
            if (tit.isEmpty()) continue;
            synchronized (historyLock) { if (globalPlayHistory.add(art + tit)) res.add(art + " - " + tit); }
            if (res.size() >= limit) break;
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Deezer: artist mix
    // ─────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getDeezerArtistMix(String artist, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        String artistResp = performGetRequest("https://api.deezer.com/search/artist?q=" + URLEncoder.encode(artist, "UTF-8") + "&limit=1");
        if (artistResp.isEmpty()) return res;
        JSONArray artistData = new JSONObject(artistResp).optJSONArray("data");
        if (artistData == null || artistData.length() == 0) return res;
        long artistId = artistData.getJSONObject(0).getLong("id");
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
            synchronized (historyLock) { if (globalPlayHistory.add(a + tit)) res.add(a + " - " + tit); }
            if (res.size() >= limit) break;
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Deezer: Charts (RU + Global)
    // ─────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getTracksFromDeezerCharts(int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        long[] chartIds = {0L, 1111141420L};
        for (long chartId : chartIds) {
            int offset = random.nextInt(80);
            String url = "https://api.deezer.com/chart/" + chartId + "/tracks?limit=50&index=" + offset;
            String resp = performGetRequest(url);
            if (resp.isEmpty()) continue;
            JSONObject json = new JSONObject(resp);
            JSONArray data = json.optJSONArray("data");
            if (data == null && json.has("tracks")) data = json.getJSONObject("tracks").optJSONArray("data");
            if (data == null) continue;
            ArrayList<JSONObject> items = new ArrayList<>();
            for (int i = 0; i < data.length(); i++) items.add(data.getJSONObject(i));
            Collections.shuffle(items);
            for (JSONObject t : items) {
                String art = t.has("artist") ? t.getJSONObject("artist").getString("name") : "";
                String tit = t.optString("title", "");
                if (art.isEmpty() || tit.isEmpty()) continue;
                synchronized (historyLock) { if (globalPlayHistory.add(art + tit)) res.add(art + " - " + tit); }
                if (res.size() >= limit) return res;
            }
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Русский контент — только жанровые плейлисты (чарт убран — нефильтрован)
    // ─────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getRussianTracksForGenre(String genre, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        // chart/1111141420 убран: он возвращает весь RU чарт без жанровой фильтрации —
        // фонк-волна получала Анну Асти, эмо-рэп волна — шансон и т.д.
        // Теперь только плейлисты с жанровым ключевым словом на русском.
        String ruQuery = getRussianGenreQuery(genre);
        if (ruQuery == null) ruQuery = "русская музыка";
        int plOffset = random.nextInt(150);
        String plResp = performGetRequest("https://api.deezer.com/search/playlist?q="
                + URLEncoder.encode(ruQuery, "UTF-8") + "&limit=8&index=" + plOffset);
        if (!plResp.isEmpty()) {
            JSONArray playlists = new JSONObject(plResp).optJSONArray("data");
            if (playlists != null) {
                ArrayList<JSONObject> plList = new ArrayList<>();
                for (int i = 0; i < playlists.length(); i++) plList.add(playlists.getJSONObject(i));
                Collections.shuffle(plList);
                for (int pi = 0; pi < Math.min(4, plList.size()); pi++) {
                    long plId = plList.get(pi).getLong("id");
                    int tOffset = random.nextInt(60);
                    String tResp = performGetRequest("https://api.deezer.com/playlist/" + plId + "/tracks?limit=30&index=" + tOffset);
                    if (tResp.isEmpty()) continue;
                    JSONArray tracks = new JSONObject(tResp).optJSONArray("data");
                    if (tracks == null) continue;
                    ArrayList<JSONObject> tList = new ArrayList<>();
                    for (int j = 0; j < tracks.length(); j++) tList.add(tracks.getJSONObject(j));
                    Collections.shuffle(tList);
                    for (JSONObject t : tList) {
                        String art = t.has("artist") ? t.getJSONObject("artist").optString("name", "") : "";
                        String tit = t.optString("title", "");
                        if (art.isEmpty() || tit.isEmpty()) continue;
                        synchronized (historyLock) { if (globalPlayHistory.add(art + tit)) res.add(art + " - " + tit); }
                        if (res.size() >= limit) return res;
                    }
                }
            }
        }
        return res;
    }

    private static String getRussianGenreQuery(String genre) {
        if (genre == null) return "русская музыка 2024";
        switch (genre.toLowerCase().trim()) {
            case "поп":                          return "русский поп 2024";
            case "рэп":                          return "русский рэп 2024";
            case "хип-хоп": case "хип хоп":     return "русский хип хоп 2024";
            case "рок":                          return "русский рок";
            case "инди":                         return "русское инди";
            case "фонк":                         return "русский фонк";
            case "эмо-рэп": case "эмо реп":     return "русский эмо рэп";
            case "пост-панк":                    return "русский пост панк";
            case "электроника":                  return "русская электроника";
            case "синтвейв":                     return "русский синтвейв";
            case "альтернатива":                 return "русская альтернатива";
            case "гиперпоп":                     return "русский гиперпоп";
            default:                             return "русская музыка 2024";
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Last.fm (через прокси — работает только если прокси настроен)
    // Используем только как дополнительный источник, не как основу.
    // ─────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getLastFmSimilarByTrack(String artist, String title, int limit) throws Exception {
        ArrayList<String> res = new ArrayList<>();
        String url = "https://ws.audioscrobbler.com/2.0/?method=track.getsimilar"
                + "&artist=" + URLEncoder.encode(artist, "UTF-8")
                + "&track=" + URLEncoder.encode(title, "UTF-8")
                + "&api_key=43691b1e6f4f4e3547fa1c798d695bf4"
                + "&format=json&limit=" + Math.min(limit * 2, 30);
        String resp = performGetRequest(url, true); // через прокси
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
            synchronized (historyLock) { if (globalPlayHistory.add(art + tit)) res.add(art + " - " + tit); }
            if (res.size() >= limit) return res;
        }
        return res;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Альбом: треки + жанр
    // ─────────────────────────────────────────────────────────────────────

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
            synchronized (historyLock) { if (globalPlayHistory.add(art + tit)) res.add(art + " - " + tit); }
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

    // ─────────────────────────────────────────────────────────────────────
    // Seed tracks (хардкод — для первого запуска без сети)
    // ─────────────────────────────────────────────────────────────────────

    private static ArrayList<String> getSeedTracksForGenre(String genre) {
        String g = genre.toLowerCase().trim();
        ArrayList<String> list = new ArrayList<>();

        try {
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("soundgram_wave", Context.MODE_PRIVATE);
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

            // ── ПОП ──────────────────────────────────────────────────────────
            // ~40% RU: Anna Asti, Zivert, JONY, Люся Чеботина, Мари Краймбрери,
            //          Клава Кока, Niletto, Artik & Asti, Zhanulka, HammAli & Navai
            case "поп":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU ~40% (12 из 30)
                        "Anna Asti - Царица",
                        "Anna Asti - Падала",
                        "Zivert - Credo",
                        "JONY - Комета",
                        "Люся Чеботина - Солнце Монако",
                        "Мари Краймбрери - Иди танцуй",
                        "Клава Кока - Покинула чат",
                        "Niletto - Любимка",
                        "Artik & Asti - Гармония",
                        "Zhanulka - Ненастоящий",
                        "HammAli & Navai - Птичка",
                        "Rauf & Faik - Воспоминания",
                        // INT (18 из 30)
                        "Sabrina Carpenter - Espresso",
                        "Sabrina Carpenter - Please Please Please",
                        "Sabrina Carpenter - Taste",
                        "Charli XCX - Apple",
                        "Charli XCX - 360",
                        "Charli XCX - Guess",
                        "Chappell Roan - Good Luck Babe",
                        "Chappell Roan - Pink Pony Club",
                        "Billie Eilish - Birds of a Feather",
                        "Billie Eilish - LUNCH",
                        "Dua Lipa - Houdini",
                        "Dua Lipa - Training Season",
                        "Benson Boone - Beautiful Things",
                        "Gracie Abrams - That's So True",
                        "Gracie Abrams - I Love You I'm Sorry",
                        "Tyla - Water",
                        "Miley Cyrus - Flowers",
                        "Olivia Rodrigo - vampire"
                )); break;

            // ── ХИП-ХОП (бумбэп / сознательный / олдскул-звук) ──────────────
            // ~40% RU: Oxxxymiron, Хаски, Рем Дигга, Каста, Баста, Noize MC,
            //          ATL, Loc-Dog, Смоки Мо, 25/17
            case "хип-хоп": case "хип хоп":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU (10 из 25)
                        "Oxxxymiron - Где нас нет",
                        "Oxxxymiron - Оябун",
                        "Хаски - Пуля",
                        "Хаски - Любимые песни",
                        "Рем Дигга - Прыжок",
                        "Каста - Вокруг шум",
                        "Баста - Моя игра",
                        "Noize MC - Выдыхай",
                        "Смоки Мо - Аэропорт",
                        "25/17 - Никто не спит",
                        // INT (15 из 25)
                        "Kendrick Lamar - Not Like Us",
                        "Kendrick Lamar - euphoria",
                        "J. Cole - Middle Child",
                        "J. Cole - Let There Be Light",
                        "JID - Surround Sound",
                        "Joey Badass - Land of the Free",
                        "Freddie Gibbs - 4 Thangs",
                        "Denzel Curry - Clout Cobain",
                        "Vince Staples - Fast Lane",
                        "Isaiah Rashad - Lay Wit Ya",
                        "JPEGMAFIA - TRUST NO ONE!",
                        "Tyler the Creator - SORRY NOT SORRY",
                        "Common - The Words",
                        "Lupe Fiasco - Words I Never Said",
                        "Run the Jewels - walking in the snow"
                )); break;

            // ── РЭП (трэп / дрилл / мамбарэп) ───────────────────────────────
            // ~40% RU: Скриптонит, Big Baby Tape, PHARAOH, Boulevard Depo,
            //          Kizaru, Saluki, GONE.Fludd, Платина, Cupsize, Shadowraze,
            //          Toxi$, LOVV66, Нурминский
            case "рэп":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU ~40% (12 из 30)
                        "Скриптонит - Цепи",
                        "Big Baby Tape - Gimme The Loot",
                        "Big Baby Tape - NAZI",
                        "PHARAOH - Грязь",
                        "Boulevard Depo - Кащенко",
                        "Kizaru - Дежавю",
                        "Saluki - ОГНЕЙ",
                        "GONE.Fludd - Не дыши",
                        "Платина - Хулиганы",
                        "Cupsize - Горит",
                        "LOVV66 - Ты горишь как огонь",
                        "Нурминский - Гравитация",
                        // INT (18 из 30)
                        "Travis Scott - FE!N",
                        "Travis Scott - MY EYES",
                        "Travis Scott - THANK GOD",
                        "Future - Like That",
                        "Future - WAIT FOR U",
                        "Metro Boomin - Superhero",
                        "21 Savage - redrum",
                        "21 Savage - Prove It",
                        "Playboi Carti - WHOLE LOTTA RED",
                        "Gunna - fukumean",
                        "Lil Baby - On Me",
                        "Rod Wave - Tombstone",
                        "Lil Durk - All My Life",
                        "Central Cee - Doja",
                        "Dave - Starlight",
                        "Fivio Foreign - 1000 Nights",
                        "Polo G - Hall of Fame",
                        "Don Toliver - No Idea"
                )); break;

            // ── РОК ──────────────────────────────────────────────────────────
            // ~40% RU: Би-2, Земфира, Сплин, Мумий Тролль, Нервы, Порнофильмы,
            //          ДДТ, Алиса, Король и Шут, Кипелов
            case "рок":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU (10 из 25)
                        "Би-2 - Я никому не поверю",
                        "Земфира - Остин",
                        "Сплин - Передайте это Гарри Поттеру",
                        "Нервы - Батареи",
                        "Порнофильмы - Я так соскучился",
                        "Мумий Тролль - Лето без интернета",
                        "ДДТ - Борщевик",
                        "Кипелов - Час пик",
                        "Король и Шут - Лесник",
                        "Алиса - Трасса Е-95",
                        // INT (15 из 25)
                        "Maneskin - GOSSIP",
                        "Maneskin - Honey (Are U Coming?)",
                        "Imagine Dragons - Eyes Closed",
                        "Twenty One Pilots - Overcompensate",
                        "Twenty One Pilots - Paladin Strait",
                        "Muse - Compliance",
                        "Muse - Kill or Be Killed",
                        "Royal Blood - Troubles Coming",
                        "Nothing But Thieves - Welcome to the DCC",
                        "YUNGBLUD - The Funeral",
                        "Greta Van Fleet - Meeting The Master",
                        "Foo Fighters - Rescued",
                        "The Black Keys - Beautiful People",
                        "Hozier - Too Sweet",
                        "Jack White - That Black Bat Licorice"
                )); break;

            // ── ИНДИ ─────────────────────────────────────────────────────────
            // ~40% RU: Кишлак, Zhanulka, Сироткин, Дайте танк, Буерак,
            //          Монеточка, Алена Швец, ГДР, Папин Олимпос, Электрофорез,
            //          Свидание, Увула, Пасош
            case "инди":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU (10 из 25)
                        "Кишлак - Мальчик",
                        "Кишлак - Голубая вода",
                        "Zhanulka - Ненастоящий",
                        "Сироткин - Выше домов",
                        "Дайте танк - Утро",
                        "Буерак - Страсть к курению",
                        "Монеточка - Каждый раз",
                        "Алена Швец - Вино и сигареты",
                        "ГДР - Мой сон",
                        "Папин Олимпос - Темно-оранжевый закат",
                        // INT (15 из 25)
                        "Arctic Monkeys - There'd Better Be A Mirrorball",
                        "Arctic Monkeys - Mr Schwarzenegger",
                        "Tame Impala - The Less I Know The Better",
                        "Wallows - Calling After Me",
                        "Wallows - Hard To Believe",
                        "Wet Leg - Chaise Longue",
                        "Wet Leg - Too Late Now",
                        "Mitski - My Love Mine All Mine",
                        "Mitski - Bug Like an Angel",
                        "Hozier - De Selby (Part 1)",
                        "Beabadoobee - Beaches",
                        "Girl in Red - Too Much",
                        "The Strokes - The Adults Are Talking",
                        "Lorde - Solar Power",
                        "Clairo - Sexy to Someone"
                )); break;

            // ── ФОНК ─────────────────────────────────────────────────────────
            // ~40% RU: Plenka, DVRST, ZXC, Scxr Soul (RU-сцена), Night Lovell
            case "фонк":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU ~40% (8 из 20) — русская фонк-сцена
                        "DVRST - Close Eyes",
                        "DVRST - Run",
                        "ZXC - GHOUL",
                        // INT (12 из 20)
                        "Kordhell - Murder In My Mind",
                        "Kordhell - ALONE",
                        "MoonDeity - Neon Blade",
                        "MoonDeity - PACK A PUNCH",
                        "Interworld - Metamorphosis",
                        "Odetari - Good Loyal Thots",
                        "Odetari - CYBERCRUNK",
                        "PlayaPhonk - PHONKY TOWN",
                        "Phonkha - MTG",
                        "Swerve - Kerosene",
                        "Ghostemane - HELLRAP",
                        "City Morgue - HELL OR HIGH WATER"
                )); break;

            // ── ГИПЕРПОП ─────────────────────────────────────────────────────
            // ~40% RU: GSPD, Мейби Бэйби, Дора, Instasamka, CMH, Kate WT,
            //          Lida, Slava Marlow, Sqwoz Bab
            case "гиперпоп":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU ~40% (8 из 20)
                        "GSPD - Евродэнс",
                        "GSPD - Некстген",
                        "GSPD - Батарейка",
                        "Мейби Бэйби - Ахегао",
                        "Дора - Втюрилась",
                        "Instasamka - За деньги да",
                        "CMH - PAYDAY",
                        "Lida - Девочка с концерта",
                        // INT (12 из 20)
                        "100 gecs - Hollywood Baby",
                        "100 gecs - I Got My Tooth Removed",
                        "Charli XCX - Von dutch",
                        "Charli XCX - 360",
                        "Glaive - astrid",
                        "Glaive - walk away",
                        "ericdoa - fawnboy",
                        "midwxst - trying",
                        "brakence - dropout",
                        "ElyOtto - SugarCrash",
                        "Dorian Electra - Flamboyant",
                        "SOPHIE - Its Okay to Cry"
                )); break;

            // ── ПОСТ-ПАНК ────────────────────────────────────────────────────
            // ~40% RU: Molchat Doma, Ploho, Кишлак, ГДР, Буерак, Увула,
            //          Черная Речка, Kino, Nautilus Pompilius, Ssshhhiiittt
            case "пост-панк":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU (10 из 25)
                        "Molchat Doma - Судно",
                        "Molchat Doma - Тоска",
                        "Ploho - Закладка",
                        "Кишлак - Мальчик",
                        "ГДР - Мой сон",
                        "Буерак - Спортивные очки",
                        "Ssshhhiiittt - Восьмерка",
                        "Kino - Группа крови",
                        "Nautilus Pompilius - Дыхание",
                        "Увула - Ты и твоя тень",
                        // INT (15 из 25)
                        "Fontaines D.C. - Starburster",
                        "Fontaines D.C. - Roman Holiday",
                        "Yard Act - The Overload",
                        "Shame - Alphabet",
                        "IDLES - Dancer",
                        "IDLES - Crawler",
                        "Interpol - Evil",
                        "The Cure - Alone",
                        "Editors - Munich",
                        "The National - Bloodbuzz Ohio",
                        "Bauhaus - Bela Lugosi's Dead",
                        "Joy Division - Love Will Tear Us Apart",
                        "Echo And The Bunnymen - The Killing Moon",
                        "Siouxsie and the Banshees - Cities in Dust",
                        "She Wants Revenge - Tear You Apart"
                )); break;

            // ── ЭМО-РЭП ──────────────────────────────────────────────────────
            // ~40% RU: Cupsize, Shadowraze, LOVV66, PHARAOH, Lida, Toxi$,
            //          Скриптонит, T-Fest, ЛСП, Элджей
            case "эмо-рэп": case "эмо реп":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU ~40% (12 из 30)
                        "Cupsize - Боль",
                        "Cupsize - Ранен",
                        "Cupsize - Пропасть",
                        "Shadowraze - Потерян",
                        "Shadowraze - Крылья",
                        "Shadowraze - Ночь",
                        "LOVV66 - Не со мной",
                        "LOVV66 - Ты горишь как огонь",
                        "PHARAOH - Лаллипап",
                        "Lida - Иди за мной",
                        "Toxi$ - Больно",
                        "T-Fest - Улети",
                        // INT (18 из 30)
                        "Lil Peep - Star Shopping",
                        "Lil Peep - Awful Things",
                        "Lil Peep - Falling Down",
                        "XXXTENTACION - Jocelyn Flores",
                        "XXXTENTACION - SAD!",
                        "XXXTENTACION - Moonlight",
                        "Juice WRLD - Lucid Dreams",
                        "Juice WRLD - Wishing Well",
                        "Juice WRLD - Come & Go",
                        "The Kid LAROI - WITHOUT YOU",
                        "The Kid LAROI - STAY",
                        "Joji - Glimpse of Us",
                        "Joji - Slow Dancing in the Dark",
                        "Post Malone - I Fall Apart",
                        "Trippie Redd - Miss The Rage",
                        "Iann Dior - emotions",
                        "Sueco - Paralyzed",
                        "Lil Uzi Vert - The Way Life Goes"
                )); break;

            // ── МЕТАЛ ────────────────────────────────────────────────────────
            // ~40% RU: Slaughter to Prevail, Wildways, AMATORY, Louna,
            //          Кипелов, Ария, Radio Tapok, Stigmata, Психея
            case "метал": case "металл":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU ~40% (10 из 25)
                        "Slaughter to Prevail - Viking",
                        "Slaughter to Prevail - Demolisher",
                        "Wildways - Прада",
                        "AMATORY - Космос",
                        "Louna - Колыбельная",
                        "Кипелов - Я Свободен",
                        "Radio Tapok - Высота 776",
                        "Ария - Проклятье морей",
                        "Stigmata - Сентябрь",
                        "Психея - Навсегда",
                        // INT (15 из 25)
                        "Bring Me The Horizon - Kool-Aid",
                        "Bring Me The Horizon - DArkSide",
                        "Sleep Token - The Summoning",
                        "Sleep Token - Chokehold",
                        "Bad Omens - Just Pretend",
                        "Bad Omens - The Death of Peace of Mind",
                        "Spiritbox - Jaded",
                        "Lorna Shore - To the Hellfire",
                        "Gojira - Amazonia",
                        "Architects - Seeing Red",
                        "Knocked Loose - Suffocate",
                        "Trivium - In The Court Of The Dragon",
                        "Slipknot - The Dying Song",
                        "Parkway Drive - The Void",
                        "Shadow of Intent - Underneath a Dying Star"
                )); break;

            // ── АЛЬТЕРНАТИВА ─────────────────────────────────────────────────
            // ~40% RU: Lumen, Нервы, Кис-Кис, Jane Air, Animal ДжаZ,
            //          Tracktor Bowling, Слот, 7000$, Гран-КуражЪ
            case "альтернатива":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU (10 из 25)
                        "Lumen - Гореть",
                        "Нервы - Сиги-буги",
                        "Нервы - Батареи",
                        "Кис-Кис - Молчи",
                        "Jane Air - Junk",
                        "Animal ДжаZ - Три полоски",
                        "Tracktor Bowling - Черта",
                        "Слот - Круги на воде",
                        "7000$ - Потерянный рай",
                        "Гран-КуражЪ - Птица счастья",
                        // INT (15 из 25)
                        "Linkin Park - Lost",
                        "Linkin Park - The Emptiness Machine",
                        "Deftones - Ohms",
                        "Paramore - This Is Why",
                        "Paramore - Running Out of Time",
                        "Blink-182 - ONE MORE TIME",
                        "Fall Out Boy - Love From The Other Side",
                        "My Chemical Romance - The Foundations of Decay",
                        "Papa Roach - Kill The Noise",
                        "Three Days Grace - So Called Life",
                        "Breaking Benjamin - Far Away",
                        "Evanescence - Wasted On You",
                        "Spiritbox - Holy Roller",
                        "Bad Omens - Just Pretend",
                        "Starset - Vessels"
                )); break;

            // ── К-ПОП ────────────────────────────────────────────────────────
            // Русских артистов в к-попе нет — держим 100% INT, не натягиваем
            case "к-поп": case "к поп":
                list = new ArrayList<>(java.util.Arrays.asList(
                        "Jung Kook - Seven",
                        "Jung Kook - Standing Next to You",
                        "NewJeans - Super Shy",
                        "NewJeans - OMG",
                        "NewJeans - Bubble Gum",
                        "Stray Kids - LALALALA",
                        "Stray Kids - Miroh",
                        "LE SSERAFIM - Perfect Night",
                        "LE SSERAFIM - UNFORGIVEN",
                        "aespa - Drama",
                        "aespa - Supernova",
                        "IVE - I AM",
                        "IVE - Baddie",
                        "IVE - Accendio",
                        "BLACKPINK - Shut Down",
                        "BLACKPINK - Pink Venom",
                        "BTS - Dynamite",
                        "TWICE - SET ME FREE",
                        "SEVENTEEN - Super",
                        "TXT - Chasing That Feeling",
                        "ENHYPEN - Bite Me",
                        "NCT 127 - Fact Check",
                        "ATEEZ - BOUNCY",
                        "ATEEZ - Fireworks",
                        "MAMAMOO - HIP"
                )); break;

            // ── ДЖАЗ ─────────────────────────────────────────────────────────
            // ~40% RU: Игорь Бутман, Олег Аккуратов, LRK Trio, Therr Maitz,
            //          Кешью, Jazz Parking, Евгений Побожий
            case "джаз":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU ~40% (8 из 20)
                        "Игорь Бутман - Nostalgia",
                        "Олег Аккуратов - Nature Boy",
                        "LRK Trio - The Tides",
                        "Therr Maitz - Future Is Bright",
                        "Кешью - Синяя птица",
                        "Jazz Parking - Осень в городе",
                        "Евгений Побожий - Elements",
                        "Антон Беляев - Fly Me To The Moon",
                        // INT (12 из 20)
                        "Laufey - From The Start",
                        "Laufey - Dreamer",
                        "Samara Joy - Tight",
                        "Samara Joy - Peach Plum Pear",
                        "Kamasi Washington - Street Fighter Mas",
                        "Gregory Porter - Liquid Spirit",
                        "Jacob Collier - In My Room",
                        "Norah Jones - Don't Know Why",
                        "Jon Batiste - FREEDOM",
                        "Snarky Puppy - Lingus",
                        "Esperanza Spalding - I Know You Know",
                        "Brad Mehldau - Knives Out"
                )); break;

            // ── БЛЮЗ ─────────────────────────────────────────────────────────
            // ~40% RU: Маргулис, Чиж & Co, Billy's Band, Crossroadz,
            //          Леван Ломидзе, Сергей Воронов
            case "блюз":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU ~40% (8 из 20)
                        "Маргулис - Мой друг лучше всех играет блюз",
                        "Чиж & Co - О любви",
                        "Billy's Band - Оторвемся по-питерски",
                        "Crossroadz - Diamond Rain",
                        "Леван Ломидзе - Blues for You",
                        "Сергей Воронов - Я остаюсь",
                        "Blues Doctors - Good Times",
                        "Михаил Мишурис - Sweet Home Chicago",
                        // INT (12 из 20)
                        "Joe Bonamassa - Redemption",
                        "Gary Clark Jr. - Pearl Cadillac",
                        "Christone Kingfish Ingram - Outside Of This Town",
                        "Tedeschi Trucks Band - Midnight in Harlem",
                        "John Mayer - Slow Dancing in a Burning Room",
                        "Beth Hart - Bad Woman Blues",
                        "Kenny Wayne Shepherd - Blue on Black",
                        "The Black Keys - Weight of Love",
                        "Fantastic Negrito - Plastic Hamburgers",
                        "Buddy Guy - Cognac",
                        "Marcus King - Beautiful Stranger",
                        "Larkin Poe - Bad Spell"
                )); break;

            // ── КЛАССИКА / НЕО-КЛАССИКА ──────────────────────────────────────
            // ~40% RU: Кирилл Рихтер, Evgeny Grinko, Ilya Beshevli,
            //          Глеб Колядин, Nikola Melnikov, Polina Nazaykinskaya,
            //          Пётр Налич, Аигел
            case "классика":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU ~40% (8 из 20) — российские неоклассики
                        "Кирилл Рихтер - Mechanisms",
                        "Кирилл Рихтер - Reborn",
                        "Евгений Гринько - Valse",
                        "Илья Бешевли - Night Forest",
                        "Илья Бешевли - Утро",
                        "Никола Мельников - Flight",
                        "Аигел - Пыяла",
                        "Пётр Налич - Зима",
                        // INT (12 из 20)
                        "Ludovico Einaudi - Experience",
                        "Ludovico Einaudi - Nuvole Bianche",
                        "Max Richter - On the Nature of Daylight",
                        "Hans Zimmer - Time",
                        "Olafur Arnalds - Near Light",
                        "Olafur Arnalds - Near Light (Nils Frahm Remix)",
                        "Nils Frahm - Says",
                        "Nils Frahm - All Melody",
                        "Yann Tiersen - Comptine d'un autre ete",
                        "Philip Glass - Metamorphosis 1",
                        "Dustin O'Halloran - Opus 23",
                        "Johann Johannsson - The Sun's Gone Dim"
                )); break;

            // ── ЭЛЕКТРОНИКА ──────────────────────────────────────────────────
            // ~40% RU: Kedr Livanskiy, IC3PEAK, Cream Soda, Tesla Boy,
            //          GSPD, Аигел, ЛУНА, Motorama, Pompeya
            case "электроника":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU ~40% (10 из 25)
                        "Кедр Ливанский - Ariadna",
                        "IC3PEAK - Смерти Больше Нет",
                        "Крем Сода - Плачу на техно",
                        "Tesla Boy - Electronic Life",
                        "GSPD - Некстген",
                        "Аигел - Татарин",
                        "ЛУНА - Free Love",
                        "Motorama - Alps",
                        "Pompeya - Flood",
                        "Электрофорез - Э Зло",
                        // INT (15 из 25)
                        "Fred again.. - leavemealone",
                        "Fred again.. - Danielle (smile on my face)",
                        "Peggy Gou - (It Goes Like) Nanana",
                        "Bicep - Glue",
                        "Bicep - Atlas",
                        "Disclosure - Latch",
                        "Flume - Never Be Like You",
                        "Overmono - Good Lies",
                        "Rufus Du Sol - Innerbloom",
                        "Four Tet - Baby",
                        "Jamie xx - Gosh",
                        "Floating Points - LesAlpx",
                        "Jon Hopkins - Open Eye Signal",
                        "Moderat - Bad Kingdom",
                        "Bonobo - Kong"
                )); break;

            // ── СИНТВЕЙВ ─────────────────────────────────────────────────────
            // ~40% RU: Молчат Дома, Альянс, Kino, Nautilus Pompilius,
            //          Электрофорез, Свидание, Увула, Mvsa
            case "синтвейв":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU (8 из 20)
                        "Молчат Дома - Танцевать",
                        "Альянс - На заре",
                        "Kino - Спокойная ночь",
                        "Nautilus Pompilius - Взгляд с экрана",
                        "Электрофорез - Я ничего не могу с собою сделать",
                        "Свидание - Я и твой кот",
                        "Увула - Дранк",
                        "Mvsa - Neon",
                        // INT (12 из 20)
                        "Kavinsky - Nightcall",
                        "Kavinsky - Reborn",
                        "The Midnight - Sunset",
                        "The Midnight - Vampires",
                        "Timecop1983 - On the Run",
                        "Gunship - Tech Noir",
                        "FM-84 - Running In The Night",
                        "Lazerhawk - Overdrive",
                        "Carpenter Brut - Turbo Killer",
                        "Perturbator - Future Club",
                        "Dance With The Dead - Riot",
                        "Waveshaper - Grid Runner"
                )); break;

            // ── ЛО-ФАЙ ───────────────────────────────────────────────────────
            // ~40% RU: Flovv, Мой Микрорайон, Flawed Mangoes, Tapecut,
            //          Pool Sounds, Elusive, Sickboyrari
            case "ло-фай": case "лофай":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU (8 из 20)
                        "Flovv - Грусть по пятницам",
                        "Мой Микрорайон - Кассеты",
                        "Flawed Mangoes - Апрель",
                        "Tapecut - Мягко",
                        "Pool Sounds - Тихо",
                        "Elusive - Ночной трамвай",
                        "Sickboyrari - Осень",
                        "Dvrst - Lofi Dreams",
                        // INT (12 из 20)
                        "Jinsang - affection",
                        "Kupla - owls of the night",
                        "bsd.u - french inhale",
                        "Tomppabeats - monday Loop",
                        "In Love With A Ghost - we've never met",
                        "Nujabes - Aruarian Dance",
                        "j'san - city lights",
                        "Elijah Who - this girl",
                        "Idealism - controlla",
                        "Lofi Girl - aaru",
                        "oneheart - snowfall",
                        "Mndsgn - Ode to the Mets"
                )); break;

            // ── ЭМБИЕНТ ──────────────────────────────────────────────────────
            // ~40% RU: Koan, Astropilot, Carbon Based Lifeforms (RU),
            //          Solar Fields (RU), Zymosis, Suduaya, Asura, H.U.V.A. Network
            case "эмбиент":
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU ~40% (8 из 20) — российская эмбиент-сцена
                        "Коан - В зелёном лесу",
                        "Астропилот - Апрель",
                        "Астропилот - Параллельный мир",
                        "Зимовья Зверей - Осень",
                        "Сетчатка - Плавание",
                        "Ян Тирсен - Бывает",
                        "Суркова - Облако",
                        "Юрий Каспарян - Сон",
                        // INT (12 из 20)
                        "Brian Eno - An Ending (Ascent)",
                        "Brian Eno - Thursday Afternoon",
                        "Aphex Twin - Rhubarb",
                        "Stars Of The Lid - Requiem For Dying Mothers",
                        "Tim Hecker - Chimeras",
                        "Grouper - Heavy Water",
                        "Hammock - Breathturn",
                        "C418 - Sweden",
                        "William Basinski - The Disintegration Loops I",
                        "A Winged Victory for the Sullen - Steep Hills of Vicodin Tears",
                        "Bohren & der Club of Gore - Midnight Black Earth",
                        "Johann Johannsson - The Sun's Gone Dim"
                )); break;

            // ── ЛЮБОЙ ЖАНР / DEFAULT ─────────────────────────────────────────
            // ~40% RU: смесь актуальных треков всех жанров
            default:
                list = new ArrayList<>(java.util.Arrays.asList(
                        // RU ~40% (12 из 30)
                        "Кишлак - Мальчик",
                        "Zhanulka - Ненастоящий",
                        "Anna Asti - Царица",
                        "Скриптонит - Цепи",
                        "Big Baby Tape - Gimme The Loot",
                        "Cupsize - Горит",
                        "Molchat Doma - Судно",
                        "IC3PEAK - Смерти Больше Нет",
                        "Cream Soda - Плачу на техно",
                        "Монеточка - Каждый раз",
                        "PHARAOH - Грязь",
                        "Нервы - Батареи",
                        // INT (18 из 30)
                        "Kendrick Lamar - Not Like Us",
                        "Sabrina Carpenter - Espresso",
                        "Charli XCX - Apple",
                        "Billie Eilish - Birds of a Feather",
                        "Travis Scott - FE!N",
                        "Fred again.. - leavemealone",
                        "Arctic Monkeys - There'd Better Be A Mirrorball",
                        "Mitski - My Love Mine All Mine",
                        "Bring Me The Horizon - Kool-Aid",
                        "Sleep Token - The Summoning",
                        "Tyla - Water",
                        "Hozier - Too Sweet",
                        "Laufey - From The Start",
                        "Benson Boone - Beautiful Things",
                        "Gracie Abrams - That's So True",
                        "Fontaines D.C. - Starburster",
                        "Bicep - Glue",
                        "Maneskin - GOSSIP"
                )); break;
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Обновление seed с GitHub
    // ─────────────────────────────────────────────────────────────────────

    public static void checkAndUpdateWaveSeeds(Context context) {
        backgroundExecutor.execute(() -> {
            try {
                String url = "https://raw.githubusercontent.com/soundgram-project/SoundGram-wave/main/wave.txt";
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(5000); c.setReadTimeout(5000);
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
                int newVersion = 0;
                try { newVersion = Integer.parseInt(firstLine.replace("update", "").trim()); } catch (Exception e) { return; }
                final int fv = newVersion;
                SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("soundgram_wave", Context.MODE_PRIVATE);
                if (fv <= prefs.getInt("wave_seed_version", 0)) return;
                JSONObject newSeeds = new JSONObject();
                String currentGenre = null; JSONArray currentArray = null;
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
                prefs.edit().putInt("wave_seed_version", fv)
                        .putString("wave_seed_data", newSeeds.toString()).apply();
                AndroidUtilities.runOnUIThread(() -> {
                    if (context != null)
                        android.widget.Toast.makeText(context, "Волна SoundGram обновлена до v." + fv, android.widget.Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) { FileLog.e("Wave Update Error", e); }
        });
    }
}
