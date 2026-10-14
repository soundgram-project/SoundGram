package soundgram.music;

import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MusicImporter {

    public static class TrackInfo {
        public final String title;
        public final String artist;

        public TrackInfo(String title, String artist) {
            this.title = title != null ? title.trim() : "";
            this.artist = artist != null ? artist.trim() : "";
        }

        @NonNull
        @Override
        public String toString() {
            if (!TextUtils.isEmpty(artist) && !TextUtils.isEmpty(title)) {
                return artist + " — " + title;
            }
            return !TextUtils.isEmpty(title) ? title : artist;
        }
    }

    public static class ParsedPlaylist {
        public String title = "";
        public String coverUrl = "";
        public boolean isPlaylist = false;
        public final List<TrackInfo> tracks = new ArrayList<>();
    }

    public interface ImportListener {
        void onParsingStarted();
        void onParsedTracks(List<TrackInfo> tracks, String playlistTitle);
        void onTrackProgress(int current, int total, String trackName, boolean success);
        void onFinished(int totalFound, int totalProcessed, String playlistTitle);
        void onCancelled(int totalFound, int totalProcessed);
        void onError(String error);
    }

    public static class ImportState {
        public boolean isRunning;
        public boolean isCancelled;
        public String playlistTitle = "";
        public int current;
        public int total;
        public String currentTrack = "";
        public boolean lastSuccess;
        public boolean isFinished;
        public int totalFound;
    }

    private static final ImportState currentState = new ImportState();
    private static final List<ImportListener> globalListeners = new CopyOnWriteArrayList<>();
    private static volatile boolean isCancelled = false;
    private static volatile boolean isRunning = false;

    public static ImportState getCurrentState() {
        return currentState;
    }

    public static boolean isImportRunning() {
        return isRunning;
    }

    public static void addListener(ImportListener listener) {
        if (listener != null && !globalListeners.contains(listener)) {
            globalListeners.add(listener);
        }
    }

    public static void removeListener(ImportListener listener) {
        if (listener != null) {
            globalListeners.remove(listener);
        }
    }

    public static void cancelImport() {
        isCancelled = true;
        MusicImportService.stop();
    }

    private static String makeTrackKey(String artist, String title) {
        if (artist == null) artist = "";
        if (title == null) title = "";
        String a = artist.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-zа-я0-9]", "");
        String t = title.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-zа-я0-9]", "");
        return a + "|" + t;
    }

    // ── Регулярные выражения ───────────────────────────────────────────────────
    private static final Pattern SPOTIFY_TRACK_PATTERN =
            Pattern.compile("open\\.spotify\\.com/(?:intl-[a-z]+/)?(?:embed/)?track/([a-zA-Z0-9]+)");
    private static final Pattern SPOTIFY_ALBUM_PATTERN =
            Pattern.compile("open\\.spotify\\.com/(?:intl-[a-z]+/)?(?:embed/)?album/([a-zA-Z0-9]+)");
    private static final Pattern SPOTIFY_PLAYLIST_PATTERN =
            Pattern.compile("open\\.spotify\\.com/(?:intl-[a-z]+/)?(?:embed/)?playlist/([a-zA-Z0-9]+)");

    private static final Pattern YANDEX_TRACK_PATTERN =
            Pattern.compile("(?:music\\.yandex\\.[a-z]+|ya\\.ru)/album/(\\d+)/track/(\\d+)");
    private static final Pattern YANDEX_PLAYLIST_UUID_PATTERN =
            Pattern.compile("(?:music\\.yandex\\.[a-z]+|ya\\.ru)/playlists/([a-f0-9\\-]+)");
    private static final Pattern YANDEX_PLAYLIST_USER_PATTERN =
            Pattern.compile("(?:music\\.yandex\\.[a-z]+|ya\\.ru)/users/([^/]+)/playlists/(\\d+)");
    private static final Pattern YANDEX_ALBUM_PATTERN =
            Pattern.compile("(?:music\\.yandex\\.[a-z]+|ya\\.ru)/album/(\\d+)");

    private static final Pattern YOUTUBE_PLAYLIST_PATTERN =
            Pattern.compile("(?:list=|(?:browse/|channel/)VL)([a-zA-Z0-9_-]+)");
    private static final Pattern YOUTUBE_CHANNEL_PATTERN =
            Pattern.compile("(?:music\\.youtube\\.com|youtube\\.com)/(?:channel/|c/|user/|@)([a-zA-Z0-9_.-]+)");
    private static final Pattern YOUTUBE_VIDEO_PATTERN =
            Pattern.compile("(?:youtu\\.be/|v=|watch\\?v=|shorts/)([a-zA-Z0-9_-]{11})");

    private static final Pattern VK_PLAYLIST_PATTERN =
            Pattern.compile("(?:vk\\.ru|vk\\.com)/.*?(?:audio_playlist|playlist/?)(-?\\d+)[_/-](\\d+)(?:[/_]([a-zA-Z0-9]+))?");
    private static final Pattern VK_AUDIOS_PATTERN =
            Pattern.compile("(?:vk\\.ru|vk\\.com)/(?:audios|audio\\?section=all&owner_id=)(-?\\d+)");
    private static final Pattern VK_GENERAL_PATTERN =
            Pattern.compile("vk\\.(?:com|ru)/(?:music|audio|wall)");

    private static final Pattern TELEGRAM_CHANNEL_PATTERN =
            Pattern.compile("(?:t\\.me|telegram\\.me)/(?:joinchat/|\\+)?([a-zA-Z0-9_+]+)(?:/(\\d+))?");

    // ── Парсинг входных данных ────────────────────────────────────────────────
    public static ParsedPlaylist parseInput(String input, int account) {
        MusicNetworkHelper.ensureBypassServiceRunning();
        ParsedPlaylist result = new ParsedPlaylist();
        if (input == null || input.trim().isEmpty()) {
            return result;
        }
        String trimmed = input.trim();

        // 1. YouTube & YouTube Music (плейлисты, треки, каналы/библиотеки)
        if (trimmed.contains("youtube.com") || trimmed.contains("youtu.be")) {
            parseYouTube(trimmed, result);
            if (!result.tracks.isEmpty()) return result;
        }

        // 2. Spotify
        if (trimmed.contains("spotify.com") || trimmed.contains("spotify.link")) {
            parseSpotify(trimmed, result);
            if (!result.tracks.isEmpty()) return result;
        }

        // 3. Яндекс Музыка (включая личные треки пользователя /users/.../tracks)
        if (trimmed.contains("music.yandex") || trimmed.contains("ya.ru")) {
            parseYandexMusic(trimmed, result);
            if (!result.tracks.isEmpty()) return result;
        }

        // 4. VK (плейлисты и аудиотека пользователя /audios...)
        if (VK_PLAYLIST_PATTERN.matcher(trimmed).find() || VK_GENERAL_PATTERN.matcher(trimmed).find() || trimmed.contains("vk.com") || trimmed.contains("vk.ru")) {
            parseVk(trimmed, result);
            if (!result.tracks.isEmpty()) return result;
        }

        // 5. Telegram / SoundGram каналы-библиотеки по ссылке (t.me/... или @...)
        if (trimmed.contains("t.me/") || trimmed.contains("telegram.me/") || (trimmed.startsWith("@") && !trimmed.contains(" "))) {
            parseTelegramChannel(trimmed, result, account);
            if (!result.tracks.isEmpty()) return result;
        }

        // 6. Текстовый список (построчный: Исполнитель - Трек)
        List<TrackInfo> textList = parseTextList(trimmed);
        result.tracks.addAll(textList);
        return result;
    }

    public static ParsedPlaylist parseInput(String input) {
        return parseInput(input, org.telegram.messenger.UserConfig.selectedAccount);
    }

    // ── Bypass Fetcher & Strict Matching ──────────────────────────────────────
    private static final String[] PROXY_MIRRORS = {
            "https://api.allorigins.win/raw?url=",
            "https://api.codetabs.com/v1/proxy?quest=",
            "https://corsproxy.org/?url=",
            "https://api.allorigins.hexatester.repl.co/raw?url="
    };

    public static String fetchUrlWithBypass(String urlString) {
        if (TextUtils.isEmpty(urlString)) return "";
        // 1. Прямое соединение через MusicNetworkHelper
        try {
            HttpURLConnection conn = MusicNetworkHelper.openConnection(urlString);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", MusicNetworkHelper.DEFAULT_USER_AGENT);
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9,ru;q=0.8");
            int code = conn.getResponseCode();
            if (code >= 200 && code < 400) {
                try (InputStream is = conn.getInputStream();
                     ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = is.read(buf)) != -1) {
                        bos.write(buf, 0, len);
                    }
                    String res = bos.toString("UTF-8");
                    if (!TextUtils.isEmpty(res)) {
                        return res;
                    }
                }
            }
        } catch (Exception e) {
            FileLog.d("MusicImporter: direct fetch failed for " + urlString + ": " + e.getMessage());
        }

        // 2. Резервный обход через r.jina.ai
        try {
            String jinaUrl = urlString.startsWith("https://r.jina.ai/") ? urlString : ("https://r.jina.ai/" + urlString);
            HttpURLConnection conn = MusicNetworkHelper.openConnection(jinaUrl);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("User-Agent", MusicNetworkHelper.DEFAULT_USER_AGENT);
            int code = conn.getResponseCode();
            if (code >= 200 && code < 400) {
                try (InputStream is = conn.getInputStream();
                     ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = is.read(buf)) != -1) {
                        bos.write(buf, 0, len);
                    }
                    String res = bos.toString("UTF-8");
                    if (!TextUtils.isEmpty(res) && res.length() > 50) {
                        return res;
                    }
                }
            }
        } catch (Exception e) {
            FileLog.d("MusicImporter: jina relay fetch failed for " + urlString + ": " + e.getMessage());
        }

        // 3. Резервный обход через общедоступные CORS/HTTP-реле
        for (String proxyPrefix : PROXY_MIRRORS) {
            try {
                String proxyUrl = proxyPrefix + URLEncoder.encode(urlString, "UTF-8");
                HttpURLConnection conn = MusicNetworkHelper.openConnection(proxyUrl);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(8000);
                conn.setReadTimeout(7000);
                conn.setRequestProperty("User-Agent", MusicNetworkHelper.DEFAULT_USER_AGENT);
                int code = conn.getResponseCode();
                if (code >= 200 && code < 400) {
                    try (InputStream is = conn.getInputStream();
                         ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = is.read(buf)) != -1) {
                            bos.write(buf, 0, len);
                        }
                        String res = bos.toString("UTF-8");
                        if (!TextUtils.isEmpty(res) && res.length() > 50) {
                            return res;
                        }
                    }
                }
            } catch (Exception e) {
                FileLog.d("MusicImporter: proxy fetch failed for " + urlString + " via " + proxyPrefix + ": " + e.getMessage());
            }
        }
        return "";
    }

    public static boolean isStrictTrackMatch(String targetArtist, String targetTitle, String foundArtist, String foundTitle) {
        if (TextUtils.isEmpty(targetTitle) || TextUtils.isEmpty(foundTitle)) {
            return false;
        }

        String cTargetTitle = cleanStringForMatch(targetTitle);
        String cFoundTitle = cleanStringForMatch(foundTitle);
        if (cTargetTitle.isEmpty() || cFoundTitle.isEmpty()) {
            return false;
        }

        // 1. Проверка артиста (если целевой артист известен)
        String cTargetArtist = cleanStringForMatch(targetArtist);
        String cFoundArtist = cleanStringForMatch(foundArtist);

        if (!cTargetArtist.isEmpty()) {
            if (cFoundArtist.isEmpty()) {
                if (!cFoundTitle.contains(cTargetArtist)) {
                    return false;
                }
            } else {
                String pTarget = getPrimaryArtist(cTargetArtist);
                String pFound = getPrimaryArtist(cFoundArtist);

                boolean artistMatches = pTarget.equals(pFound)
                        || cTargetArtist.contains(pFound)
                        || cFoundArtist.contains(pTarget)
                        || pTarget.contains(pFound)
                        || pFound.contains(pTarget)
                        || calculateSimilarity(pTarget, pFound) >= 0.78;

                if (!artistMatches) {
                    return false;
                }
            }
        }

        // 2. Строгая проверка названия
        if (cTargetTitle.equals(cFoundTitle)) {
            return true;
        }

        double sim = calculateSimilarity(cTargetTitle, cFoundTitle);
        if (sim >= 0.85) {
            return true;
        }

        // Если одно название является подстрокой другого, требуем высокое покрытие длины (>= 80%)
        if (cTargetTitle.contains(cFoundTitle) || cFoundTitle.contains(cTargetTitle)) {
            double lenRatio = (double) Math.min(cTargetTitle.length(), cFoundTitle.length()) /
                              (double) Math.max(cTargetTitle.length(), cFoundTitle.length());
            if (lenRatio >= 0.80) {
                return true;
            }
        }

        return false;
    }

    private static String cleanStringForMatch(String s) {
        if (s == null) return "";
        return s.replaceAll("(?i)\\[.*?\\]|\\(.*?\\)", "")
                .replaceAll("(?i)\\b(official\\s*video|music\\s*video|lyric\\s*video|audio|remastered|hd|4k|hq)\\b", "")
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-zа-я0-9]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String getPrimaryArtist(String cleanArtist) {
        if (cleanArtist == null || cleanArtist.isEmpty()) return "";
        String[] parts = cleanArtist.split("[,/&]|\\b(feat|ft|vs|and|с\\s+участием)\\b");
        if (parts.length > 0 && !parts[0].trim().isEmpty()) {
            return parts[0].trim();
        }
        return cleanArtist;
    }

    private static double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        int len1 = s1.length();
        int len2 = s2.length();
        if (len1 == 0 || len2 == 0) return 0.0;

        int[][] dp = new int[len1 + 1][len2 + 1];
        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            char c1 = s1.charAt(i - 1);
            for (int j = 1; j <= len2; j++) {
                char c2 = s2.charAt(j - 1);
                int cost = (c1 == c2) ? 0 : 1;
                dp[i][j] = Math.min(dp[i - 1][j] + 1, Math.min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost));
            }
        }

        int maxLen = Math.max(len1, len2);
        return 1.0 - ((double) dp[len1][len2] / (double) maxLen);
    }

    // ── Spotify ────────────────────────────────────────────────────────────────
    private static void parseSpotify(String url, ParsedPlaylist result) {
        try {
            Matcher mTrack = SPOTIFY_TRACK_PATTERN.matcher(url);
            if (mTrack.find()) {
                String trackId = mTrack.group(1);
                // 1. Jina relay for track (works in RU without VPN)
                try {
                    String jinaRes = fetchUrlWithBypass("https://r.jina.ai/https://open.spotify.com/track/" + trackId);
                    if (!TextUtils.isEmpty(jinaRes)) {
                        Matcher mJt = Pattern.compile("Title:\\s*([^-\\n\\r]+?)\\s*-\\s*song\\s+and\\s+lyrics\\s+by\\s+([^\\n\\r]+)").matcher(jinaRes);
                        if (mJt.find()) {
                            String title = mJt.group(1).trim();
                            String artist = mJt.group(2).replace("| Spotify", "").replace("- Spotify", "").trim();
                            if (!TextUtils.isEmpty(title)) {
                                result.tracks.add(new TrackInfo(title, artist));
                                result.isPlaylist = false;
                                return;
                            }
                        }
                    }
                } catch (Exception ignored) {}

                // 2. oEmbed via CORS relay
                try {
                    String oembed = fetchUrlWithBypass("https://open.spotify.com/oembed?url=" + URLEncoder.encode("https://open.spotify.com/track/" + trackId, "UTF-8"));
                    if (!TextUtils.isEmpty(oembed)) {
                        JSONObject obj = new JSONObject(oembed);
                        String title = obj.optString("title");
                        String author = obj.optString("author_name", "");
                        if (!TextUtils.isEmpty(title)) {
                            result.tracks.add(new TrackInfo(title, author));
                            result.isPlaylist = false;
                            return;
                        }
                    }
                } catch (Exception ignored) {}

                // 3. Fallback embed HTML
                String embedHtml = fetchUrlWithBypass("https://open.spotify.com/embed/track/" + trackId);
                extractTracksFromSpotifyEmbed(embedHtml, result);
                return;
            }

            Matcher mAlbum = SPOTIFY_ALBUM_PATTERN.matcher(url);
            if (mAlbum.find()) {
                String albumId = mAlbum.group(1);
                result.isPlaylist = true;

                // 1. Jina relay for album embed
                String jinaRes = fetchUrlWithBypass("https://r.jina.ai/https://open.spotify.com/embed/album/" + albumId);
                extractTracksFromSpotifyEmbed(jinaRes, result);
                if (!result.tracks.isEmpty()) return;

                // 2. Allorigins relay
                try {
                    String allorigins = fetchUrlWithBypass("https://api.allorigins.win/raw?url=" + URLEncoder.encode("https://open.spotify.com/embed/album/" + albumId, "UTF-8"));
                    extractTracksFromSpotifyEmbed(allorigins, result);
                    if (!result.tracks.isEmpty()) return;
                } catch (Exception ignored) {}

                // 3. Direct embed HTML
                String embedHtml = fetchUrlWithBypass("https://open.spotify.com/embed/album/" + albumId);
                extractTracksFromSpotifyEmbed(embedHtml, result);
                if (!result.tracks.isEmpty()) return;
            }

            Matcher mPlaylist = SPOTIFY_PLAYLIST_PATTERN.matcher(url);
            if (mPlaylist.find()) {
                String playlistId = mPlaylist.group(1);
                result.isPlaylist = true;

                // 1. Jina relay for playlist embed (verified: returns all tracks in clean markdown)
                String jinaRes = fetchUrlWithBypass("https://r.jina.ai/https://open.spotify.com/embed/playlist/" + playlistId);
                extractTracksFromSpotifyEmbed(jinaRes, result);
                if (!result.tracks.isEmpty()) return;

                // 2. Allorigins relay for playlist embed (full __NEXT_DATA__ JSON)
                try {
                    String allorigins = fetchUrlWithBypass("https://api.allorigins.win/raw?url=" + URLEncoder.encode("https://open.spotify.com/embed/playlist/" + playlistId, "UTF-8"));
                    extractTracksFromSpotifyEmbed(allorigins, result);
                    if (!result.tracks.isEmpty()) return;
                } catch (Exception ignored) {}

                // 3. Codetabs proxy
                try {
                    String codetabs = fetchUrlWithBypass("https://api.codetabs.com/v1/proxy?quest=" + URLEncoder.encode("https://open.spotify.com/embed/playlist/" + playlistId, "UTF-8"));
                    extractTracksFromSpotifyEmbed(codetabs, result);
                    if (!result.tracks.isEmpty()) return;
                } catch (Exception ignored) {}

                // 4. Direct embed HTML
                String embedHtml = fetchUrlWithBypass("https://open.spotify.com/embed/playlist/" + playlistId);
                extractTracksFromSpotifyEmbed(embedHtml, result);
                if (!result.tracks.isEmpty()) return;
            }

            if (result.tracks.isEmpty()) {
                String jinaRes = fetchUrlWithBypass("https://r.jina.ai/" + url);
                extractTracksFromSpotifyEmbed(jinaRes, result);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static void extractTracksFromSpotifyEmbed(String html, ParsedPlaylist result) {
        if (TextUtils.isEmpty(html)) return;
        try {
            Pattern scriptPattern = Pattern.compile("<script id=\"__NEXT_DATA__\"[^>]*>(.*?)</script>", Pattern.DOTALL);
            Matcher m = scriptPattern.matcher(html);
            if (m.find()) {
                String jsonStr = m.group(1);
                JSONObject root = new JSONObject(jsonStr);
                JSONObject entity = root.getJSONObject("props")
                        .getJSONObject("pageProps")
                        .getJSONObject("state")
                        .getJSONObject("data")
                        .getJSONObject("entity");

                if (TextUtils.isEmpty(result.title)) {
                    result.title = entity.optString("title", entity.optString("name", ""));
                }
                if (TextUtils.isEmpty(result.coverUrl)) {
                    JSONObject coverArt = entity.optJSONObject("coverArt");
                    if (coverArt != null) {
                        JSONArray sources = coverArt.optJSONArray("sources");
                        if (sources != null && sources.length() > 0) {
                            result.coverUrl = sources.getJSONObject(0).optString("url", "");
                        }
                    }
                }

                if (entity.has("trackList")) {
                    JSONArray tracks = entity.getJSONArray("trackList");
                    for (int i = 0; i < tracks.length(); i++) {
                        JSONObject t = tracks.getJSONObject(i);
                        String title = t.optString("title", t.optString("name", ""));
                        String artist = t.optString("subtitle", "");
                        if (TextUtils.isEmpty(artist) && t.has("artists")) {
                            JSONArray artists = t.optJSONArray("artists");
                            if (artists != null && artists.length() > 0) {
                                StringBuilder sb = new StringBuilder();
                                for (int a = 0; a < artists.length(); a++) {
                                    if (sb.length() > 0) sb.append(", ");
                                    sb.append(artists.getJSONObject(a).optString("name", ""));
                                }
                                artist = sb.toString();
                            }
                        }
                        if (!TextUtils.isEmpty(title)) {
                            result.tracks.add(new TrackInfo(title, artist));
                        }
                    }
                } else if ("track".equalsIgnoreCase(entity.optString("type"))) {
                    String title = entity.optString("title", entity.optString("name", ""));
                    String artist = entity.optString("subtitle", "");
                    if (TextUtils.isEmpty(artist) && entity.has("artists")) {
                        JSONArray artists = entity.optJSONArray("artists");
                        if (artists != null && artists.length() > 0) {
                            StringBuilder sb = new StringBuilder();
                            for (int a = 0; a < artists.length(); a++) {
                                if (sb.length() > 0) sb.append(", ");
                                sb.append(artists.getJSONObject(a).optString("name", ""));
                            }
                            artist = sb.toString();
                        }
                    }
                    if (!TextUtils.isEmpty(title)) {
                        result.tracks.add(new TrackInfo(title, artist));
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }

        // Fallback regex parsing
        if (result.tracks.isEmpty()) {
            Pattern trackRegex = Pattern.compile("\"title\":\"([^\"]+)\",\"subtitle\":\"([^\"]+)\"");
            Matcher tm = trackRegex.matcher(html);
            while (tm.find()) {
                String title = tm.group(1);
                String artist = tm.group(2);
                if (!"Spotify".equalsIgnoreCase(artist) && !TextUtils.isEmpty(title)) {
                    result.tracks.add(new TrackInfo(title, artist));
                }
            }
        }

        // Fallback for new Spotify initial-state (base64 encoded JSON)
        if (result.tracks.isEmpty()) {
            try {
                Pattern statePattern = Pattern.compile("<script id=\"initial-state\"[^>]*>(.*?)</script>", Pattern.DOTALL);
                Matcher sm = statePattern.matcher(html);
                if (sm.find()) {
                    String b64 = sm.group(1).trim();
                    byte[] decoded = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
                    String jsonStr = new String(decoded, "UTF-8");
                    Pattern trackInState = Pattern.compile("\"name\":\"([^\"]+)\"[^}]*?\"artists\":\\[\\{\"name\":\"([^\"]+)\"");
                    Matcher tsm = trackInState.matcher(jsonStr);
                    while (tsm.find()) {
                        String title = tsm.group(1);
                        String artist = tsm.group(2);
                        if (!"Spotify".equalsIgnoreCase(artist) && !TextUtils.isEmpty(title)) {
                            result.tracks.add(new TrackInfo(title, artist));
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // Extract title from Markdown if present
        if (TextUtils.isEmpty(result.title) && !TextUtils.isEmpty(html)) {
            Matcher tm = Pattern.compile("(?i)Title:\\s*([^\\n\\r]+)").matcher(html);
            if (tm.find()) {
                String raw = tm.group(1).trim();
                raw = raw.replaceAll("(?i)\\s*-\\s*(?:playlist by\\s+)?Spotify.*$", "")
                         .replaceAll("(?i)\\s*\\|\\s*Spotify.*$", "").trim();
                if (!raw.isEmpty()) {
                    result.title = raw;
                }
            }
        }

        // Fallback markdown parsing (from r.jina.ai relay)
        if (result.tracks.isEmpty()) {
            Pattern mdNumbered = Pattern.compile("(?m)^\\s*\\d+\\.\\s+###\\s+([^\\r\\n]+)[\\r\\n]+(?:\\s*[\\r\\n]+)?####\\s+(?:E\\s+)?([^\\r\\n]+)");
            Matcher mn = mdNumbered.matcher(html);
            while (mn.find()) {
                String title = mn.group(1).trim();
                String artist = mn.group(2).trim();
                if (!TextUtils.isEmpty(title) && !"Spotify".equalsIgnoreCase(title)) {
                    result.tracks.add(new TrackInfo(title, artist));
                }
            }
        }
        if (result.tracks.isEmpty()) {
            Pattern mdTrack = Pattern.compile("\\[([^\\]]+)\\]\\((?:https://open\\.spotify\\.com)?/track/[a-zA-Z0-9]+\\)(?:[^\\[]*\\[([^\\]]+)\\]\\((?:https://open\\.spotify\\.com)?/artist/[a-zA-Z0-9]+\\))?");
            Matcher m = mdTrack.matcher(html);
            while (m.find()) {
                String title = m.group(1).trim();
                String artist = m.group(2) != null ? m.group(2).trim() : "";
                if (!TextUtils.isEmpty(title) && !"Spotify".equalsIgnoreCase(title)) {
                    result.tracks.add(new TrackInfo(title, artist));
                }
            }
        }
        if (result.tracks.isEmpty()) {
            Pattern mdHeaderTrack = Pattern.compile("###\\s+([^\\n\\r]+)[\\r\\n]+(?:[E\\s]*[\\r\\n]+)?####\\s+(?:[E\\s]*\\[?)([^\\n\\r\\]]+)");
            Matcher m2 = mdHeaderTrack.matcher(html);
            while (m2.find()) {
                String title = m2.group(1).trim();
                String artist = m2.group(2).trim();
                if (!TextUtils.isEmpty(title) && !"Spotify".equalsIgnoreCase(title)) {
                    result.tracks.add(new TrackInfo(title, artist));
                }
            }
        }
        if (result.tracks.isEmpty()) {
            // Pattern for numbered list: "1. Artist - Track" or "1. Track - Artist" or markdown table: "| 1 | Track | Artist |"
            Pattern numList = Pattern.compile("(?m)^\\s*\\d+\\.\\s*\\*\\*([^*]+)\\*\\*\\s*(?:[-–—]\\s*([^\\r\\n]+))?");
            Matcher m3 = numList.matcher(html);
            while (m3.find()) {
                String title = m3.group(1).trim();
                String artist = m3.group(2) != null ? m3.group(2).trim() : "";
                if (!TextUtils.isEmpty(title) && !"Spotify".equalsIgnoreCase(title)) {
                    result.tracks.add(new TrackInfo(title, artist));
                }
            }
        }
    }

    // ── Яндекс Музыка ─────────────────────────────────────────────────────────
    private static void parseYandexMusic(String url, ParsedPlaylist result) {
        try {
            // 1. Одиночный трек
            Matcher mTrack = YANDEX_TRACK_PATTERN.matcher(url);
            if (mTrack.find()) {
                String trackId = mTrack.group(2);
                String apiJson = MusicWebSearcher.fetchHtml("https://api.music.yandex.net/tracks/" + trackId);
                JSONObject root = new JSONObject(apiJson);
                JSONArray resArr = root.optJSONArray("result");
                if (resArr != null && resArr.length() > 0) {
                    JSONObject t = resArr.getJSONObject(0);
                    String title = t.optString("title");
                    String artist = "";
                    JSONArray artists = t.optJSONArray("artists");
                    if (artists != null && artists.length() > 0) {
                        artist = artists.getJSONObject(0).optString("name");
                    }
                    if (!TextUtils.isEmpty(title)) {
                        result.tracks.add(new TrackInfo(title, artist));
                    }
                }
                return;
            }

            // 2. Плейлист по UUID (новые ссылки music.yandex.ru/playlists/<UUID>)
            Matcher mUuid = YANDEX_PLAYLIST_UUID_PATTERN.matcher(url);
            if (mUuid.find()) {
                String uuid = mUuid.group(1);
                String pageHtml = MusicWebSearcher.fetchHtml("https://music.yandex.ru/playlists/" + uuid);
                if (!TextUtils.isEmpty(pageHtml)) {
                    String uid = null;
                    String kind = null;

                    Pattern uidKindPattern = Pattern.compile("\"uid\":(\\d+),\"kind\":(\\d+)");
                    Matcher ukm = uidKindPattern.matcher(pageHtml);
                    if (ukm.find()) {
                        uid = ukm.group(1);
                        kind = ukm.group(2);
                    } else {
                        Pattern uidPattern = Pattern.compile("\"owner\":\\{\"uid\":(\\d+)");
                        Pattern kindPattern = Pattern.compile("\"kind\":(\\d+)");
                        Matcher um = uidPattern.matcher(pageHtml);
                        Matcher km = kindPattern.matcher(pageHtml);
                        if (um.find()) uid = um.group(1);
                        if (km.find()) kind = km.group(1);
                    }

                    if (uid != null && kind != null) {
                        parseYandexUserPlaylist(uid, kind, result);
                        result.isPlaylist = true;
                        return;
                    }
                }
            }

            // 3. Плейлист пользователя music.yandex.ru/users/<user>/playlists/<kind>
            Matcher mPlaylist = YANDEX_PLAYLIST_USER_PATTERN.matcher(url);
            if (mPlaylist.find()) {
                String user = mPlaylist.group(1);
                String kind = mPlaylist.group(2);
                parseYandexUserPlaylist(user, kind, result);
                result.isPlaylist = true;
                return;
            }

            // 3.1. Личные треки / библиотека пользователя music.yandex.ru/users/<user>/tracks
            Matcher mUserTracks = Pattern.compile("(?:music\\.yandex\\.[a-z]+|ya\\.ru)/users/([^/]+)/(?:tracks|likes)").matcher(url);
            if (mUserTracks.find()) {
                String user = mUserTracks.group(1);
                parseYandexUserPlaylist(user, "3", result);
                result.isPlaylist = false;
                if (!result.tracks.isEmpty()) return;
            }

            // 4. Альбом
            Matcher mAlbum = YANDEX_ALBUM_PATTERN.matcher(url);
            if (mAlbum.find()) {
                String albumId = mAlbum.group(1);
                parseYandexAlbum(albumId, result);
                result.isPlaylist = true;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static void parseYandexUserPlaylist(String user, String kind, ParsedPlaylist result) {
        try {
            String apiJson = MusicWebSearcher.fetchHtml("https://api.music.yandex.net/users/" + user + "/playlists/" + kind);
            JSONObject root = new JSONObject(apiJson);
            JSONObject res = root.optJSONObject("result");
            if (res != null) {
                if (TextUtils.isEmpty(result.title)) {
                    result.title = res.optString("title", "Плейлист Яндекс Музыки");
                }
                if (TextUtils.isEmpty(result.coverUrl)) {
                    JSONObject coverObj = res.optJSONObject("cover");
                    if (coverObj != null) {
                        String uri = coverObj.optString("uri", "");
                        if (!TextUtils.isEmpty(uri)) {
                            result.coverUrl = "https://" + uri.replace("%%", "400x400");
                        }
                    }
                    if (TextUtils.isEmpty(result.coverUrl)) {
                        String ogImage = res.optString("ogImage", "");
                        if (!TextUtils.isEmpty(ogImage)) {
                            result.coverUrl = "https://" + ogImage.replace("%%", "400x400");
                        }
                    }
                }

                if (res.has("tracks")) {
                    JSONArray tracks = res.getJSONArray("tracks");
                    for (int i = 0; i < tracks.length(); i++) {
                        JSONObject item = tracks.getJSONObject(i);
                        JSONObject t = item.optJSONObject("track");
                        if (t != null) {
                            String title = t.optString("title");
                            StringBuilder artists = new StringBuilder();
                            JSONArray artArr = t.optJSONArray("artists");
                            if (artArr != null) {
                                for (int a = 0; a < artArr.length(); a++) {
                                    if (artists.length() > 0) artists.append(", ");
                                    artists.append(artArr.getJSONObject(a).optString("name", ""));
                                }
                            }
                            if (!TextUtils.isEmpty(title)) {
                                result.tracks.add(new TrackInfo(title, artists.toString()));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static void parseYandexAlbum(String albumId, ParsedPlaylist result) {
        try {
            String apiJson = MusicWebSearcher.fetchHtml("https://api.music.yandex.net/albums/" + albumId + "/with-tracks");
            JSONObject root = new JSONObject(apiJson);
            JSONObject res = root.optJSONObject("result");
            if (res != null) {
                if (TextUtils.isEmpty(result.title)) {
                    result.title = res.optString("title", "Альбом");
                }
                if (TextUtils.isEmpty(result.coverUrl)) {
                    String coverUri = res.optString("coverUri", "");
                    if (!TextUtils.isEmpty(coverUri)) {
                        result.coverUrl = "https://" + coverUri.replace("%%", "400x400");
                    } else {
                        String ogImage = res.optString("ogImage", "");
                        if (!TextUtils.isEmpty(ogImage)) {
                            result.coverUrl = "https://" + ogImage.replace("%%", "400x400");
                        }
                    }
                }

                if (res.has("volumes")) {
                    JSONArray volumes = res.getJSONArray("volumes");
                    for (int v = 0; v < volumes.length(); v++) {
                        JSONArray vTracks = volumes.getJSONArray(v);
                        for (int i = 0; i < vTracks.length(); i++) {
                            JSONObject t = vTracks.getJSONObject(i);
                            String title = t.optString("title");
                            StringBuilder artists = new StringBuilder();
                            JSONArray artArr = t.optJSONArray("artists");
                            if (artArr != null) {
                                for (int a = 0; a < artArr.length(); a++) {
                                    if (artists.length() > 0) artists.append(", ");
                                    artists.append(artArr.getJSONObject(a).optString("name", ""));
                                }
                            }
                            if (!TextUtils.isEmpty(title)) {
                                result.tracks.add(new TrackInfo(title, artists.toString()));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    // ── YouTube & YouTube Music ────────────────────────────────────────────────
    private static void parseYouTube(String url, ParsedPlaylist result) {
        try {
            // 1. Плейлист, альбом или микс: music.youtube.com/playlist?list=... или youtube.com/playlist?list=...
            Matcher mPl = YOUTUBE_PLAYLIST_PATTERN.matcher(url);
            if (mPl.find()) {
                String playlistId = mPl.group(1);
                result.isPlaylist = true;
                parseYouTubePlaylistDirectApi(playlistId, result);
                if (result.tracks.isEmpty()) {
                    parseYouTubePlaylistHtml(playlistId, result);
                }
                if (result.tracks.isEmpty()) {
                    parseYouTubePlaylistInnerTube(playlistId, result);
                }
                if (!result.tracks.isEmpty()) return;
            }

            // 2. Канал / библиотека автора: youtube.com/@handle или music.youtube.com/channel/UC...
            Matcher mChan = YOUTUBE_CHANNEL_PATTERN.matcher(url);
            if (mChan.find()) {
                String chanRef = mChan.group(1);
                result.isPlaylist = false;
                String uploadsPl = null;
                if (chanRef.startsWith("UC") && chanRef.length() > 2) {
                    uploadsPl = "UU" + chanRef.substring(2);
                } else {
                    try {
                        String chanHtml = MusicWebSearcher.fetchHtml("https://www.youtube.com/@" + chanRef);
                        if (!TextUtils.isEmpty(chanHtml)) {
                            Matcher mc = Pattern.compile("\"(?:channelId|externalId)\":\"(UC[a-zA-Z0-9_-]+)\"").matcher(chanHtml);
                            if (mc.find()) {
                                uploadsPl = "UU" + mc.group(1).substring(2);
                            }
                        }
                    } catch (Exception ignored) {}
                }
                if (uploadsPl != null) {
                    parseYouTubePlaylistDirectApi(uploadsPl, result);
                    if (result.tracks.isEmpty()) {
                        parseYouTubePlaylistHtml(uploadsPl, result);
                    }
                    if (result.tracks.isEmpty()) {
                        parseYouTubePlaylistInnerTube(uploadsPl, result);
                    }
                    if (!result.tracks.isEmpty()) return;
                }
            }

            // 3. Одиночный трек / видео: youtu.be/... или youtube.com/watch?v=...
            Matcher mVid = YOUTUBE_VIDEO_PATTERN.matcher(url);
            if (mVid.find()) {
                String videoId = mVid.group(1);
                parseYouTubeSingleVideo(videoId, result);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static void parseYouTubePlaylistDirectApi(String playlistId, ParsedPlaylist result) {
        // 1. Invidious pool (unblocked, returns JSON with 100-200 tracks per page)
        String[] invInstances = new String[]{
                "https://inv.nadeko.net",
                "https://invidious.nerdvpn.de",
                "https://yewtu.be",
                "https://invidious.jing.rocks"
        };
        for (String base : invInstances) {
            try {
                String resp = fetchUrlWithBypass(base + "/api/v1/playlists/" + playlistId);
                if (!TextUtils.isEmpty(resp)) {
                    JSONObject json = new JSONObject(resp);
                    if (TextUtils.isEmpty(result.title)) {
                        result.title = json.optString("title", "Плейлист YouTube");
                    }
                    JSONArray videos = json.optJSONArray("videos");
                    if (videos != null && videos.length() > 0) {
                        for (int i = 0; i < videos.length(); i++) {
                            JSONObject v = videos.getJSONObject(i);
                            String title = v.optString("title", "");
                            String author = v.optString("author", "");
                            if (!TextUtils.isEmpty(title)) {
                                result.tracks.add(cleanYouTubeTrack(title, author));
                            }
                        }
                        if (!result.tracks.isEmpty()) return;
                    }
                }
            } catch (Exception ignored) {}
        }

        // 2. Piped pool
        String[] pipedInstances = new String[]{
                "https://pipedapi.ducks.party",
                "https://api.piped.private.coffee"
        };
        for (String base : pipedInstances) {
            try {
                String resp = fetchUrlWithBypass(base + "/playlists/" + playlistId);
                if (!TextUtils.isEmpty(resp)) {
                    JSONObject json = new JSONObject(resp);
                    if (TextUtils.isEmpty(result.title)) {
                        result.title = json.optString("name", "Плейлист YouTube");
                    }
                    JSONArray streams = json.optJSONArray("relatedStreams");
                    if (streams != null && streams.length() > 0) {
                        for (int i = 0; i < streams.length(); i++) {
                            JSONObject s = streams.getJSONObject(i);
                            String title = s.optString("title", "");
                            String uploader = s.optString("uploaderName", "");
                            if (!TextUtils.isEmpty(title)) {
                                result.tracks.add(cleanYouTubeTrack(title, uploader));
                            }
                        }
                        if (!result.tracks.isEmpty()) return;
                    }
                }
            } catch (Exception ignored) {}
        }

        // 3. Jina relay
        try {
            String jinaRes = fetchUrlWithBypass("https://r.jina.ai/https://www.youtube.com/playlist?list=" + playlistId);
            if (!TextUtils.isEmpty(jinaRes)) {
                if (TextUtils.isEmpty(result.title)) {
                    Matcher tm = Pattern.compile("(?i)Title:\\s*([^\\n\\r]+)").matcher(jinaRes);
                    if (tm.find()) {
                        result.title = tm.group(1).replace(" - YouTube", "").replace(" - YouTube Music", "").trim();
                    }
                }
                Pattern pYt = Pattern.compile("\\[([^\\]]+)\\]\\((?:https://www\\.youtube\\.com)?/watch\\?v=([a-zA-Z0-9_-]{11})");
                Matcher mYt = pYt.matcher(jinaRes);
                java.util.HashSet<String> seenIds = new java.util.HashSet<>();
                while (mYt.find()) {
                    String title = mYt.group(1).trim();
                    String vId = mYt.group(2);
                    if (seenIds.add(vId) && !title.equalsIgnoreCase("Play all") && !title.equalsIgnoreCase("PLAY ALL")
                            && !title.startsWith("Image ") && !title.startsWith("Skip to main")
                            && !title.equalsIgnoreCase("YouTube") && !title.equals(result.title)) {
                        result.tracks.add(cleanYouTubeTrack(title, ""));
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static void parseYouTubePlaylistHtml(String playlistId, ParsedPlaylist result) {
        try {
            String html = fetchUrlWithBypass("https://www.youtube.com/playlist?list=" + playlistId);
            if (TextUtils.isEmpty(html)) {
                html = fetchUrlWithBypass("https://music.youtube.com/playlist?list=" + playlistId);
            }
            if (TextUtils.isEmpty(html)) return;

            Matcher mJson = Pattern.compile("(?:var\\s+)?ytInitialData\\s*=\\s*(\\{.+?\\});<", Pattern.DOTALL).matcher(html);
            if (mJson.find()) {
                try {
                    JSONObject root = new JSONObject(mJson.group(1));
                    collectYouTubeJsonTracks(root, result);

                    if (TextUtils.isEmpty(result.title)) {
                        JSONObject metadata = root.optJSONObject("metadata");
                        if (metadata != null) {
                            JSONObject plMeta = metadata.optJSONObject("playlistMetadataRenderer");
                            if (plMeta != null) {
                                result.title = plMeta.optString("title", "");
                            }
                        }
                        if (TextUtils.isEmpty(result.title)) {
                            JSONObject microformat = root.optJSONObject("microformat");
                            if (microformat != null) {
                                JSONObject md = microformat.optJSONObject("microformatDataRenderer");
                                if (md != null) {
                                    result.title = md.optString("title", "");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }

            if (TextUtils.isEmpty(result.title)) {
                Matcher mTitle = Pattern.compile("<meta property=\"og:title\" content=\"([^\"]+)\"").matcher(html);
                if (mTitle.find()) {
                    result.title = mTitle.group(1).replace(" - YouTube", "").replace(" - YouTube Music", "").trim();
                }
            }
            if (TextUtils.isEmpty(result.coverUrl)) {
                Matcher mCover = Pattern.compile("<meta property=\"og:image\" content=\"([^\"]+)\"").matcher(html);
                if (mCover.find()) {
                    result.coverUrl = mCover.group(1);
                }
            }

            if (result.tracks.isEmpty()) {
                Pattern pTrack = Pattern.compile("\"title\":\\{\"runs\":\\[\\{\"text\":\"([^\"]+)\"\\}\\].*?\"shortBylineText\":\\{\"runs\":\\[\\{\"text\":\"([^\"]+)\"\\}\\]");
                Matcher mt = pTrack.matcher(html);
                while (mt.find()) {
                    String title = mt.group(1);
                    String author = mt.group(2);
                    if (!TextUtils.isEmpty(title)) {
                        result.tracks.add(cleanYouTubeTrack(title, author));
                    }
                }
            }

            if (result.tracks.isEmpty()) {
                String jinaRes = fetchUrlWithBypass("https://r.jina.ai/https://www.youtube.com/playlist?list=" + playlistId);
                if (!TextUtils.isEmpty(jinaRes)) {
                    Pattern pYt = Pattern.compile("(?:###\\s*)?\\[([^\\]]+)\\]\\(https://www\\.youtube\\.com/watch\\?v=([a-zA-Z0-9_-]+)[^\\)]*\\)");
                    Matcher mYt = pYt.matcher(jinaRes);
                    java.util.HashSet<String> seenIds = new java.util.HashSet<>();
                    while (mYt.find()) {
                        String title = mYt.group(1).trim();
                        String vId = mYt.group(2);
                        if (seenIds.add(vId) && !title.equalsIgnoreCase("Play all") && !title.equalsIgnoreCase("PLAY ALL") && !title.startsWith("Image ") && !title.startsWith("Skip to main")) {
                            result.tracks.add(cleanYouTubeTrack(title, ""));
                        }
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static void parseYouTubePlaylistInnerTube(String playlistId, ParsedPlaylist result) {
        try {
            List<soundgram.music.wave.InnerTubeClient.RawTrack> raw = soundgram.music.wave.InnerTubeClient.fetchPlaylist(playlistId, null);
            if (raw != null && !raw.isEmpty()) {
                if (TextUtils.isEmpty(result.title)) {
                    result.title = "Плейлист YouTube";
                }
                for (soundgram.music.wave.InnerTubeClient.RawTrack rt : raw) {
                    if (!TextUtils.isEmpty(rt.title)) {
                        result.tracks.add(cleanYouTubeTrack(rt.title, rt.artist));
                    }
                }
                if (!result.tracks.isEmpty()) return;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        try {
            for (String base : new String[]{
                    "https://pipedapi.ducks.party",
                    "https://api.piped.private.coffee",
                    "https://pipedapi.kavin.rocks",
                    "https://piped.video"
            }) {
                try {
                    String resp = fetchUrlWithBypass(base + "/playlists/" + playlistId);
                    if (!TextUtils.isEmpty(resp)) {
                        JSONObject json = new JSONObject(resp);
                        if (TextUtils.isEmpty(result.title)) {
                            result.title = json.optString("name", "Плейлист YouTube");
                        }
                        JSONArray streams = json.optJSONArray("relatedStreams");
                        if (streams != null && streams.length() > 0) {
                            for (int i = 0; i < streams.length(); i++) {
                                JSONObject s = streams.getJSONObject(i);
                                String title = s.optString("title", "");
                                String uploader = s.optString("uploaderName", "");
                                if (!TextUtils.isEmpty(title)) {
                                    result.tracks.add(cleanYouTubeTrack(title, uploader));
                                }
                            }
                            if (!result.tracks.isEmpty()) return;
                        }
                    }
                } catch (Exception ignored) {}
            }

            for (String base : new String[]{
                    "https://yewtu.be",
                    "https://inv.tux.pizza",
                    "https://invidious.nerdvpn.de",
                    "https://invidious.jing.rocks"
            }) {
                try {
                    String resp = fetchUrlWithBypass(base + "/api/v1/playlists/" + playlistId);
                    if (!TextUtils.isEmpty(resp)) {
                        JSONObject json = new JSONObject(resp);
                        if (TextUtils.isEmpty(result.title)) {
                            result.title = json.optString("title", "Плейлист YouTube");
                        }
                        JSONArray videos = json.optJSONArray("videos");
                        if (videos != null && videos.length() > 0) {
                            for (int i = 0; i < videos.length(); i++) {
                                JSONObject v = videos.getJSONObject(i);
                                String title = v.optString("title", "");
                                String author = v.optString("author", "");
                                if (!TextUtils.isEmpty(title)) {
                                    result.tracks.add(cleanYouTubeTrack(title, author));
                                }
                            }
                            if (!result.tracks.isEmpty()) return;
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static void parseYouTubeSingleVideo(String videoId, ParsedPlaylist result) {
        // 1. Piped streams (fast JSON, unblocked)
        for (String base : new String[]{"https://api.piped.private.coffee", "https://pipedapi.ducks.party"}) {
            try {
                String resp = fetchUrlWithBypass(base + "/streams/" + videoId);
                if (!TextUtils.isEmpty(resp)) {
                    JSONObject obj = new JSONObject(resp);
                    String title = obj.optString("title", "");
                    String author = obj.optString("uploader", "");
                    if (!TextUtils.isEmpty(title)) {
                        result.tracks.add(cleanYouTubeTrack(title, author));
                        result.isPlaylist = false;
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        // 2. Jina relay
        try {
            String jinaRes = fetchUrlWithBypass("https://r.jina.ai/https://www.youtube.com/watch?v=" + videoId);
            if (!TextUtils.isEmpty(jinaRes)) {
                Matcher mt = Pattern.compile("Title:\\s*([^\r\n]+)").matcher(jinaRes);
                if (mt.find()) {
                    String title = mt.group(1).replace(" - YouTube", "").replace(" - YouTube Music", "").trim();
                    result.tracks.add(cleanYouTubeTrack(title, ""));
                    result.isPlaylist = false;
                    return;
                }
            }
        } catch (Exception ignored) {}

        // 3. oEmbed / direct fallback
        try {
            String oembedUrl = "https://www.youtube.com/oembed?url=" + URLEncoder.encode("https://www.youtube.com/watch?v=" + videoId, "UTF-8") + "&format=json";
            String resp = fetchUrlWithBypass(oembedUrl);
            if (!TextUtils.isEmpty(resp)) {
                JSONObject obj = new JSONObject(resp);
                String title = obj.optString("title", "");
                String author = obj.optString("author_name", "");
                if (!TextUtils.isEmpty(title)) {
                    result.tracks.add(cleanYouTubeTrack(title, author));
                    result.isPlaylist = false;
                    return;
                }
            }
        } catch (Exception ignored) {}

        try {
            String html = fetchUrlWithBypass("https://www.youtube.com/watch?v=" + videoId);
            if (!TextUtils.isEmpty(html)) {
                Matcher mt = Pattern.compile("<meta property=\"og:title\" content=\"([^\"]+)\"").matcher(html);
                if (mt.find()) {
                    String title = mt.group(1).replace(" - YouTube", "").trim();
                    result.tracks.add(cleanYouTubeTrack(title, ""));
                    result.isPlaylist = false;
                }
            }
        } catch (Exception ignored) {}
    }

    private static void collectYouTubeJsonTracks(Object node, ParsedPlaylist result) {
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;

            if (obj.has("musicResponsiveListItemRenderer")) {
                try {
                    JSONObject r = obj.getJSONObject("musicResponsiveListItemRenderer");
                    JSONArray flexColumns = r.optJSONArray("flexColumns");
                    String title = null;
                    String artist = null;

                    if (flexColumns != null && flexColumns.length() > 0) {
                        JSONObject col0 = flexColumns.getJSONObject(0).optJSONObject("musicResponsiveListItemFlexColumnRenderer");
                        if (col0 != null) {
                            JSONObject tObj = col0.optJSONObject("text");
                            if (tObj != null) {
                                JSONArray runs = tObj.optJSONArray("runs");
                                if (runs != null && runs.length() > 0) {
                                    title = runs.getJSONObject(0).optString("text", null);
                                }
                            }
                        }
                    }
                    if (flexColumns != null && flexColumns.length() > 1) {
                        JSONObject col1 = flexColumns.getJSONObject(1).optJSONObject("musicResponsiveListItemFlexColumnRenderer");
                        if (col1 != null) {
                            JSONObject tObj = col1.optJSONObject("text");
                            if (tObj != null) {
                                JSONArray runs = tObj.optJSONArray("runs");
                                if (runs != null) {
                                    for (int i = 0; i < runs.length(); i++) {
                                        String text = runs.getJSONObject(i).optString("text", "");
                                        if (!TextUtils.isEmpty(text) && !text.equals(" • ") && !text.equals("Song") && !text.equals("Песня") && !text.contains(":")) {
                                            artist = text;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!TextUtils.isEmpty(title)) {
                        result.tracks.add(cleanYouTubeTrack(title, artist != null ? artist : ""));
                    }
                } catch (Exception ignored) {}
            } else if (obj.has("playlistVideoRenderer")) {
                try {
                    JSONObject pvr = obj.getJSONObject("playlistVideoRenderer");
                    String title = null;
                    JSONObject titleObj = pvr.optJSONObject("title");
                    if (titleObj != null) {
                        JSONArray runs = titleObj.optJSONArray("runs");
                        if (runs != null && runs.length() > 0) {
                            title = runs.getJSONObject(0).optString("text", null);
                        } else {
                            title = titleObj.optString("simpleText", null);
                        }
                    }
                    String byline = "";
                    JSONObject bylineObj = pvr.optJSONObject("shortBylineText");
                    if (bylineObj != null) {
                        JSONArray runs = bylineObj.optJSONArray("runs");
                        if (runs != null && runs.length() > 0) {
                            byline = runs.getJSONObject(0).optString("text", "");
                        }
                    }
                    if (!TextUtils.isEmpty(title)) {
                        result.tracks.add(cleanYouTubeTrack(title, byline));
                    }
                } catch (Exception ignored) {}
            } else if (obj.has("playlistPanelVideoRenderer")) {
                try {
                    JSONObject pvr = obj.getJSONObject("playlistPanelVideoRenderer");
                    String title = null;
                    JSONObject titleObj = pvr.optJSONObject("title");
                    if (titleObj != null) {
                        JSONArray runs = titleObj.optJSONArray("runs");
                        if (runs != null && runs.length() > 0) {
                            title = runs.getJSONObject(0).optString("text", null);
                        }
                    }
                    String byline = "";
                    JSONObject bylineObj = pvr.optJSONObject("shortBylineText");
                    if (bylineObj != null) {
                        JSONArray runs = bylineObj.optJSONArray("runs");
                        if (runs != null && runs.length() > 0) {
                            byline = runs.getJSONObject(0).optString("text", "");
                        }
                    }
                    if (!TextUtils.isEmpty(title)) {
                        result.tracks.add(cleanYouTubeTrack(title, byline));
                    }
                } catch (Exception ignored) {}
            }

            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                try {
                    collectYouTubeJsonTracks(obj.get(keys.next()), result);
                } catch (Exception ignored) {}
            }
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < arr.length(); i++) {
                try {
                    collectYouTubeJsonTracks(arr.get(i), result);
                } catch (Exception ignored) {}
            }
        }
    }

    private static TrackInfo cleanYouTubeTrack(String rawTitle, String rawAuthor) {
        if (rawTitle == null) rawTitle = "";
        if (rawAuthor == null) rawAuthor = "";

        String author = rawAuthor.trim();
        if (author.endsWith(" - Topic")) {
            author = author.substring(0, author.length() - 8).trim();
        }
        if (author.endsWith("VEVO")) {
            author = author.substring(0, author.length() - 4).trim();
        }

        String title = rawTitle.trim();
        title = title.replaceAll("(?i)\\s*[\\[\\(]\\s*(?:official|music|video|audio|lyrics?|visualizer|hd|4k|clip|клип|премьера|трек|релиз|hq|full audio|prod\\.)[^\\]\\)]*[\\]\\)]", "").trim();

        String artist = author;
        String sep = null;
        if (title.contains(" — ")) {
            sep = " — ";
        } else if (title.contains(" – ")) {
            sep = " – ";
        } else if (title.contains(" - ")) {
            sep = " - ";
        }

        if (sep != null) {
            String[] parts = title.split(Pattern.quote(sep), 2);
            if (parts.length == 2 && !parts[0].trim().isEmpty() && !parts[1].trim().isEmpty()) {
                artist = parts[0].trim();
                title = parts[1].trim();
            }
        }

        title = title.replaceAll("^[\"']+|[\"']+$", "").trim();
        artist = artist.replaceAll("^[\"']+|[\"']+$", "").trim();

        return new TrackInfo(title, artist);
    }

    // ── VK ────────────────────────────────────────────────────────────────────
    private static void parseVk(String url, ParsedPlaylist result) {
        try {
            // 1. Аудиотека пользователя vk.com/audios123456
            Matcher mAudios = VK_AUDIOS_PATTERN.matcher(url);
            if (mAudios.find()) {
                String ownerId = mAudios.group(1);
                result.title = "Музыка VK";
                result.isPlaylist = false;

                String postBody = "act=load_section&al=1&claim=0&offset=0&owner_id=" + ownerId
                        + "&playlist_id=-1&type=playlist";

                String response = postForm("https://vk.ru/al_audio.php", postBody, "windows-1251");
                if (TextUtils.isEmpty(response) || !response.contains("payload")) {
                    response = postForm("https://vk.com/al_audio.php", postBody, "windows-1251");
                }

                if (!TextUtils.isEmpty(response)) {
                    extractVkTracksFromJson(response, result);
                }
                if (result.tracks.isEmpty()) {
                    parseVkPageHtml(url, result);
                }
                if (!result.tracks.isEmpty()) return;
            }

            // 2. Плейлист VK
            Matcher mPl = VK_PLAYLIST_PATTERN.matcher(url);
            if (mPl.find()) {
                String ownerId = mPl.group(1);
                String playlistId = mPl.group(2);
                String accessHash = mPl.groupCount() >= 3 ? mPl.group(3) : "";
                if (accessHash == null) accessHash = "";

                String postBody = "act=load_section&al=1&claim=0&offset=0&owner_id=" + ownerId
                        + "&playlist_id=" + playlistId + "&type=playlist&access_hash=" + accessHash;

                String response = postForm("https://vk.ru/al_audio.php", postBody, "windows-1251");
                if (TextUtils.isEmpty(response) || !response.contains("payload")) {
                    response = postForm("https://vk.com/al_audio.php", postBody, "windows-1251");
                }

                if (!TextUtils.isEmpty(response)) {
                    extractVkTracksFromJson(response, result);
                    if (!result.tracks.isEmpty()) {
                        result.isPlaylist = true;
                        return;
                    }
                }
            }

            // 3. Fallback: парсинг HTML страницы VK
            parseVkPageHtml(url, result);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static void extractVkTracksFromJson(String response, ParsedPlaylist result) {
        try {
            JSONObject json = new JSONObject(response);
            JSONArray payload = json.optJSONArray("payload");
            if (payload != null && payload.length() > 1) {
                JSONArray arr = payload.optJSONArray(1);
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        Object elem = arr.get(i);
                        if (elem instanceof JSONObject) {
                            JSONObject obj = (JSONObject) elem;
                            if (TextUtils.isEmpty(result.title)) {
                                result.title = obj.optString("title", "Музыка VK");
                            }
                            if (TextUtils.isEmpty(result.coverUrl)) {
                                String cover = obj.optString("coverUrl", "");
                                if (TextUtils.isEmpty(cover)) {
                                    String grid = obj.optString("gridCovers", "");
                                    Matcher gm = Pattern.compile("url\\('([^']+)'\\)").matcher(grid);
                                    if (gm.find()) cover = gm.group(1);
                                }
                                result.coverUrl = cover;
                            }
                            JSONArray list = obj.optJSONArray("list");
                            if (list != null) {
                                for (int j = 0; j < list.length(); j++) {
                                    JSONArray t = list.optJSONArray(j);
                                    if (t != null && t.length() >= 5) {
                                        String title = t.optString(3);
                                        String artist = t.optString(4);
                                        if (!TextUtils.isEmpty(title)) {
                                            result.tracks.add(new TrackInfo(title, artist));
                                        }
                                    }
                                }
                            }
                        } else if (elem instanceof String) {
                            String htmlSnippet = (String) elem;
                            Matcher rm = Pattern.compile("class=\"audio_row__title_inner[^\"]*\">([^<]+)<.*?class=\"audio_row__performers[^\"]*\">([^<]+)<", Pattern.DOTALL).matcher(htmlSnippet);
                            while (rm.find()) {
                                String title = rm.group(1).trim();
                                String artist = rm.group(2).trim();
                                if (!TextUtils.isEmpty(title)) {
                                    result.tracks.add(new TrackInfo(title, artist));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static void parseVkPageHtml(String url, ParsedPlaylist result) {
        try {
            String html = MusicWebSearcher.fetchHtml(url);
            if (TextUtils.isEmpty(html)) return;

            Pattern pRow = Pattern.compile("class=\"audio_row__title_inner[^\"]*\">([^<]+)<.*?class=\"audio_row__performers[^\"]*\">([^<]+)<", Pattern.DOTALL);
            Matcher m = pRow.matcher(html);
            while (m.find()) {
                String title = m.group(1).trim();
                String artist = m.group(2).trim();
                if (!TextUtils.isEmpty(title)) {
                    result.tracks.add(new TrackInfo(title, artist));
                }
            }

            if (result.tracks.isEmpty()) {
                Pattern pOg = Pattern.compile("<meta property=\"og:title\" content=\"([^\"]+)\"");
                Matcher mog = pOg.matcher(html);
                if (mog.find()) {
                    String full = mog.group(1).trim();
                    if (full.contains(" — ") || full.contains(" - ")) {
                        String[] parts = full.split(" [—-] ", 2);
                        result.tracks.add(new TrackInfo(parts[1].trim(), parts[0].trim()));
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    // ── Telegram каналы-библиотеки ─────────────────────────────────────────────
    private static void parseTelegramChannel(String input, ParsedPlaylist result, int account) {
        try {
            String username = null;
            long channelId = 0;

            Matcher mTme = TELEGRAM_CHANNEL_PATTERN.matcher(input);
            if (mTme.find()) {
                String path = mTme.group(1);
                if ("c".equals(path) && mTme.group(2) != null) {
                    try {
                        channelId = Long.parseLong(mTme.group(2));
                    } catch (Exception ignored) {}
                } else if (path != null && !path.startsWith("+")) {
                    username = path;
                }
            } else if (input.startsWith("@")) {
                username = input.substring(1).trim();
            }

            TLRPC.Chat chat = null;
            TLRPC.InputPeer inputPeer = null;

            if (channelId != 0) {
                chat = MessagesController.getInstance(account).getChat(channelId);
                if (chat != null) {
                    inputPeer = MessagesController.getInstance(account).getInputPeer(-chat.id);
                }
            } else if (!TextUtils.isEmpty(username)) {
                TLObject obj = MessagesController.getInstance(account).getUserOrChat(username);
                if (obj instanceof TLRPC.Chat) {
                    chat = (TLRPC.Chat) obj;
                    inputPeer = MessagesController.getInstance(account).getInputPeer(-chat.id);
                } else {
                    final CountDownLatch latch = new CountDownLatch(1);
                    final TLRPC.Chat[] resolvedChat = new TLRPC.Chat[1];

                    TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
                    req.username = username;
                    ConnectionsManager.getInstance(account).sendRequest(req, (res, error) -> {
                        try {
                            if (res instanceof TLRPC.TL_contacts_resolvedPeer) {
                                TLRPC.TL_contacts_resolvedPeer peer = (TLRPC.TL_contacts_resolvedPeer) res;
                                MessagesController.getInstance(account).putChats(peer.chats, false);
                                MessagesController.getInstance(account).putUsers(peer.users, false);
                                if (!peer.chats.isEmpty()) {
                                    resolvedChat[0] = peer.chats.get(0);
                                }
                            }
                        } finally {
                            latch.countDown();
                        }
                    });

                    try {
                        latch.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {}

                    if (resolvedChat[0] != null) {
                        chat = resolvedChat[0];
                        inputPeer = MessagesController.getInstance(account).getInputPeer(-chat.id);
                    }
                }
            }

            if (chat == null || inputPeer == null) {
                return;
            }

            result.title = chat.title;
            result.isPlaylist = false;

            int offsetId = 0;
            int maxTracks = 500;
            int emptyCount = 0;

            while (result.tracks.size() < maxTracks && emptyCount < 2) {
                final CountDownLatch msgLatch = new CountDownLatch(1);
                final List<TLRPC.Message> messagesList = new ArrayList<>();

                TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
                req.peer = inputPeer;
                req.offset_id = offsetId;
                req.offset_date = 0;
                req.add_offset = 0;
                req.limit = 100;
                req.max_id = 0;
                req.min_id = 0;
                req.hash = 0;

                ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {
                    try {
                        if (response instanceof TLRPC.messages_Messages) {
                            TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                            messagesList.addAll(res.messages);
                        }
                    } finally {
                        msgLatch.countDown();
                    }
                });

                try {
                    msgLatch.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}

                if (messagesList.isEmpty()) {
                    emptyCount++;
                    break;
                }

                int lastId = offsetId;
                for (TLRPC.Message msg : messagesList) {
                    lastId = msg.id;
                    if (msg.message != null && msg.message.startsWith("SG_STREAM|||")) {
                        String[] parts = msg.message.split("\\|\\|\\|");
                        if (parts.length >= 3) {
                            String title = parts[1].trim();
                            String artist = parts[2].trim();
                            if (!TextUtils.isEmpty(title)) {
                                result.tracks.add(new TrackInfo(title, artist));
                            }
                        }
                    } else if (msg.media instanceof TLRPC.TL_messageMediaDocument && msg.media.document != null) {
                        TLRPC.Document doc = msg.media.document;
                        if (MessageObject.isMusicDocument(doc)) {
                            String title = "";
                            String artist = "";
                            for (TLRPC.DocumentAttribute attr : doc.attributes) {
                                if (attr instanceof TLRPC.TL_documentAttributeAudio) {
                                    title = attr.title != null ? attr.title.trim() : "";
                                    artist = attr.performer != null ? attr.performer.trim() : "";
                                    break;
                                } else if (attr instanceof TLRPC.TL_documentAttributeFilename) {
                                    title = attr.file_name != null ? attr.file_name.trim() : "";
                                }
                            }
                            if (TextUtils.isEmpty(title) && !TextUtils.isEmpty(artist)) {
                                title = artist;
                                artist = "";
                            }
                            if (!TextUtils.isEmpty(title)) {
                                if (title.endsWith(".mp3") || title.endsWith(".m4a") || title.endsWith(".flac")) {
                                    title = title.substring(0, title.lastIndexOf('.')).trim();
                                }
                                result.tracks.add(new TrackInfo(title, artist));
                            }
                        }
                    }
                }

                if (lastId == offsetId || messagesList.size() < 10) {
                    break;
                }
                offsetId = lastId;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static String postForm(String urlStr, String postData, String charset) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(12000);
            conn.setDoOutput(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream is = conn.getInputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
            }
            byte[] bytes = baos.toByteArray();
            try {
                return new String(bytes, charset);
            } catch (Exception e) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ── Текстовый список ───────────────────────────────────────────────────────
    public static List<TrackInfo> parseTextList(String text) {
        List<TrackInfo> list = new ArrayList<>();
        if (text == null) return list;
        String[] lines = text.split("\r?\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Удаляем начальную нумерацию
            line = line.replaceAll("^\\s*(?:\\[?\\d+\\]?[\\.\\)\\-:]|#\\d+)\\s*", "").trim();

            // Удаляем длительность в конце
            line = line.replaceAll("\\s*[\\(\\[]?\\d+:\\d+[\\)\\]]?\\s*$", "").trim();

            if (line.isEmpty()) continue;

            String[] parts = null;
            if (line.contains(" — ")) {
                parts = line.split(" — ", 2);
            } else if (line.contains(" – ")) {
                parts = line.split(" – ", 2);
            } else if (line.contains(" - ")) {
                parts = line.split(" - ", 2);
            } else if (line.contains(" : ")) {
                parts = line.split(" : ", 2);
            } else if (line.contains(" // ")) {
                parts = line.split(" // ", 2);
            } else if (line.contains(" | ")) {
                parts = line.split(" \\| ", 2);
            }

            if (parts != null && parts.length == 2) {
                String artist = parts[0].trim();
                String title = parts[1].trim();
                if (!title.isEmpty()) {
                    list.add(new TrackInfo(title, artist));
                    continue;
                }
            }

            if (line.length() >= 2) {
                list.add(new TrackInfo(line, ""));
            }
        }
        return list;
    }

    // ── Поиск через Хитмос, скачивание и добавление в библиотеку ─────────────────
    public static void startImport(int account, String input, ImportListener listener) {
        if (listener != null) {
            addListener(listener);
        }

        isCancelled = false;
        isRunning = true;
        currentState.isRunning = true;
        currentState.isCancelled = false;
        currentState.isFinished = false;
        currentState.current = 0;
        currentState.total = 0;
        currentState.totalFound = 0;
        currentState.currentTrack = "Подготовка...";

        notifyParsingStarted();

        new Thread(() -> {
            try {
                ParsedPlaylist parsed = parseInput(input, account);
                if (parsed.tracks.isEmpty()) {
                    isRunning = false;
                    currentState.isRunning = false;
                    notifyError("Не удалось распознать треки.\nПроверьте ссылку или вставьте список текстом (Исполнитель - Трек).");
                    return;
                }

                currentState.total = parsed.tracks.size();
                currentState.playlistTitle = parsed.title;
                notifyParsedTracks(parsed.tracks, parsed.title);
                MusicImportService.start(ApplicationLoader.applicationContext, parsed.title, parsed.tracks.size());

                long channelId = ChannelMusicFragment.getChannelIdFromFile(account);
                if (channelId == 0) {
                    isRunning = false;
                    currentState.isRunning = false;
                    MusicImportService.stop();
                    notifyError("Библиотека не найдена. Сначала откройте раздел музыки в приложении.");
                    return;
                }

                final long rawTargetId = Math.abs(channelId) > 1000000000000L ? Math.abs(channelId) - 1000000000000L : Math.abs(channelId);
                final java.util.concurrent.ConcurrentHashMap<String, Integer> trackKeyToMsgId = new java.util.concurrent.ConcurrentHashMap<>();
                for (org.telegram.messenger.MessageObject mo : ChannelMusicFragment.getAudioMessages(account)) {
                    if (mo.isMusic()) {
                        String k = makeTrackKey(mo.getMusicAuthor(), mo.getMusicTitle());
                        if (!k.isEmpty() && mo.getId() > 0) {
                            trackKeyToMsgId.put(k, mo.getId());
                        }
                    }
                }

                final NotificationCenter.NotificationCenterDelegate msgDelegate = (id, acc, args) -> {
                    if (acc != account) return;
                    if (id == NotificationCenter.messageReceivedByServer) {
                        int newId = (Integer) args[1];
                        long dialogId = (Long) args[3];
                        long rawDialogId = Math.abs(dialogId) > 1000000000000L ? Math.abs(dialogId) - 1000000000000L : Math.abs(dialogId);
                        if (rawDialogId == rawTargetId) {
                            if (args[2] instanceof org.telegram.tgnet.TLRPC.Message) {
                                org.telegram.tgnet.TLRPC.Message msg = (org.telegram.tgnet.TLRPC.Message) args[2];
                                if (msg.media != null && msg.media.document != null) {
                                    String t = org.telegram.messenger.MessageObject.getMusicTitle(msg.media.document, false);
                                    String a = org.telegram.messenger.MessageObject.getMusicAuthor(msg.media.document, false);
                                    String k = makeTrackKey(a, t);
                                    if (!k.isEmpty() && newId > 0) {
                                        trackKeyToMsgId.put(k, newId);
                                    }
                                }
                            }
                        }
                    } else if (id == NotificationCenter.didReceiveNewMessages) {
                        long dialogId = (Long) args[0];
                        long rawDialogId = Math.abs(dialogId) > 1000000000000L ? Math.abs(dialogId) - 1000000000000L : Math.abs(dialogId);
                        if (rawDialogId == rawTargetId && args[1] instanceof ArrayList) {
                            ArrayList<org.telegram.messenger.MessageObject> msgs = (ArrayList<org.telegram.messenger.MessageObject>) args[1];
                            if (msgs != null) {
                                for (org.telegram.messenger.MessageObject mo : msgs) {
                                    if (mo.isMusic()) {
                                        String k = makeTrackKey(mo.getMusicAuthor(), mo.getMusicTitle());
                                        if (!k.isEmpty() && mo.getId() > 0) {
                                            trackKeyToMsgId.put(k, mo.getId());
                                        }
                                    }
                                }
                            }
                        }
                    }
                };

                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getInstance(account).addObserver(msgDelegate, NotificationCenter.messageReceivedByServer);
                    NotificationCenter.getInstance(account).addObserver(msgDelegate, NotificationCenter.didReceiveNewMessages);
                });

                int foundCount = 0;
                int total = parsed.tracks.size();
                ArrayList<String> playlistTrackLines = new ArrayList<>();

                for (int i = 0; i < total; i++) {
                    if (isCancelled) {
                        break;
                    }

                    TrackInfo ti = parsed.tracks.get(i);
                    final int currentIndex = i + 1;
                    final String trackName = ti.toString();
                    currentState.current = currentIndex;
                    currentState.currentTrack = trackName;
                    MusicImportService.updateProgress(currentIndex, total, trackName);

                    MusicWebSearcher.WebTrack hitmosTrack = null;
                    try {
                        // 1. Точный поиск Hitmos со строгой проверкой
                        MusicWebSearcher.WebTrack candidate = MusicWebSearcher.resolveHitmosSync(ti.title, ti.artist);
                        if (candidate != null && isStrictTrackMatch(ti.artist, ti.title, candidate.artist, candidate.title)) {
                            hitmosTrack = candidate;
                        }

                        // 2. Если не найден, пробуем скомбинированный запрос, но СТРОГО валидируем совпадение
                        if (hitmosTrack == null && !TextUtils.isEmpty(ti.artist)) {
                            MusicWebSearcher.WebTrack comb = MusicWebSearcher.resolveHitmosSync(ti.artist + " " + ti.title, ti.artist);
                            if (comb != null && isStrictTrackMatch(ti.artist, ti.title, comb.artist, comb.title)) {
                                hitmosTrack = comb;
                            }
                        }

                        // 3. Общий поиск: перебираем результаты и берем ТОЛЬКО строго совпадающий трек
                        if (hitmosTrack == null) {
                            ArrayList<MusicWebSearcher.WebTrack> results =
                                    MusicWebSearcher.searchTracksSync(!TextUtils.isEmpty(ti.artist) ? ti.artist + " " + ti.title : ti.title);
                            if (results != null) {
                                for (MusicWebSearcher.WebTrack r : results) {
                                    if (r != null && !TextUtils.isEmpty(r.downloadUrl) && isStrictTrackMatch(ti.artist, ti.title, r.artist, r.title)) {
                                        hitmosTrack = r;
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }

                    boolean success = hitmosTrack != null && !TextUtils.isEmpty(hitmosTrack.downloadUrl);

                    if (success) {
                        // Скачиваем реальный MP3 файл для хранилища Telegram
                        File destFile = ChannelMusicFragment.getLocalTrackFile(ti.artist, ti.title);
                        boolean downloaded = destFile.exists() && destFile.length() > 1024;
                        if (!downloaded) {
                            downloaded = MusicWebSearcher.downloadTrackToExactFileSync(hitmosTrack.downloadUrl, destFile);
                        }
                        if (!downloaded) {
                            MusicWebSearcher.WebTrack fresh = MusicWebSearcher.resolveHitmosSync(ti.title, ti.artist);
                            if (fresh != null && !TextUtils.isEmpty(fresh.downloadUrl)) {
                                downloaded = MusicWebSearcher.downloadTrackToExactFileSync(fresh.downloadUrl, destFile);
                                if (downloaded && TextUtils.isEmpty(hitmosTrack.coverUrl)) {
                                    hitmosTrack.coverUrl = fresh.coverUrl;
                                }
                            }
                        }

                        if (downloaded && destFile.exists() && destFile.length() > 1024) {
                            foundCount++;
                            currentState.totalFound = foundCount;

                            // Скачиваем обложку трека для встраивания ID3 / превью
                            String thumbPath = null;
                            String coverUrl = !TextUtils.isEmpty(hitmosTrack.coverUrl) ? hitmosTrack.coverUrl : parsed.coverUrl;
                            if (!TextUtils.isEmpty(coverUrl)) {
                                try {
                                    File cacheDir = new File(ApplicationLoader.applicationContext.getCacheDir(), "audio_covers");
                                    if (!cacheDir.exists()) cacheDir.mkdirs();
                                    File cFile = new File(cacheDir, Math.abs(destFile.getAbsolutePath().hashCode()) + ".jpg");
                                    if (!cFile.exists() || cFile.length() == 0) {
                                        MusicWebSearcher.downloadTrackToExactFileSync(coverUrl, cFile);
                                    }
                                    if (cFile.exists() && cFile.length() > 0) {
                                        thumbPath = cFile.getAbsolutePath();
                                    }
                                } catch (Exception ignored) {}
                            }

                            // Отправляем реальный mp3-файл в канал облачного хранилища Telegram
                            ChannelMusicFragment.sendLocalAudioFileToChannel(destFile, ti.title, ti.artist, channelId, account, thumbPath);

                            // Пауза между отправками во избежание flood limit
                            SystemClock.sleep(350);
                        } else {
                            success = false;
                        }
                    }

                    currentState.lastSuccess = success;
                    notifyTrackProgress(currentIndex, total, trackName, success);
                }

                if (isCancelled) {
                    AndroidUtilities.runOnUIThread(() -> {
                        NotificationCenter.getInstance(account).removeObserver(msgDelegate, NotificationCenter.messageReceivedByServer);
                        NotificationCenter.getInstance(account).removeObserver(msgDelegate, NotificationCenter.didReceiveNewMessages);
                    });
                    isRunning = false;
                    currentState.isRunning = false;
                    currentState.isCancelled = true;
                    MusicImportService.stop();
                    notifyCancelled(foundCount, total);
                    return;
                }

                // ── Создание идентичного плейлиста в SoundGram с настоящими msgId ─────────────────
                if (parsed.isPlaylist) {
                    try {
                        // Ожидаем завершения отправки сообщений в Telegram и получения положительных ID
                        long waitStart = SystemClock.elapsedRealtime();
                        while (SystemClock.elapsedRealtime() - waitStart < 15000 && !isCancelled) {
                            for (org.telegram.messenger.MessageObject mo : ChannelMusicFragment.getAudioMessages(account)) {
                                if (mo.isMusic() && mo.getId() > 0) {
                                    String k = makeTrackKey(mo.getMusicAuthor(), mo.getMusicTitle());
                                    if (!k.isEmpty()) {
                                        trackKeyToMsgId.put(k, mo.getId());
                                    }
                                }
                            }
                            boolean allResolved = true;
                            for (TrackInfo ti : parsed.tracks) {
                                String k = makeTrackKey(ti.artist, ti.title);
                                if (!trackKeyToMsgId.containsKey(k)) {
                                    allResolved = false;
                                    break;
                                }
                            }
                            if (allResolved) break;
                            SystemClock.sleep(500);
                        }

                        // Финальное обновление треков из канала
                        for (org.telegram.messenger.MessageObject mo : ChannelMusicFragment.getAudioMessages(account)) {
                            if (mo.isMusic() && mo.getId() > 0) {
                                String k = makeTrackKey(mo.getMusicAuthor(), mo.getMusicTitle());
                                if (!k.isEmpty()) {
                                    trackKeyToMsgId.put(k, mo.getId());
                                }
                            }
                        }

                        // Собираем правильные ID треков в канале для плейлиста (только положительные числа!)
                        for (TrackInfo ti : parsed.tracks) {
                            String k = makeTrackKey(ti.artist, ti.title);
                            Integer msgId = trackKeyToMsgId.get(k);
                            if (msgId != null && msgId > 0) {
                                playlistTrackLines.add(String.valueOf(msgId));
                            }
                        }

                        if (!playlistTrackLines.isEmpty()) {
                            String pTitle = !TextUtils.isEmpty(parsed.title) ? parsed.title : "Импортированный плейлист";
                            String safeTitle = pTitle.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
                            if (TextUtils.isEmpty(safeTitle)) safeTitle = "Playlist_" + System.currentTimeMillis();

                            File pDir = PlaylistSyncManager.getPlaylistsDir(account);
                            if (pDir != null) {
                                // Скачиваем обложку плейлиста
                                if (!TextUtils.isEmpty(parsed.coverUrl)) {
                                    File pCoverFile = new File(pDir, safeTitle + "_cover.jpg");
                                    if (!pCoverFile.exists() || pCoverFile.length() == 0) {
                                        MusicWebSearcher.downloadTrackToExactFileSync(parsed.coverUrl, pCoverFile);
                                    }
                                }

                                // Записываем файл плейлиста с точным порядком треков
                                File pFile = new File(pDir, safeTitle + ".txt");
                                PlaylistSyncManager.PlaylistData pData = new PlaylistSyncManager.PlaylistData();
                                pData.title = safeTitle;
                                pData.trackLines.addAll(playlistTrackLines);
                                PlaylistSyncManager.writePlaylistFile(pFile, pData);
                                PlaylistSyncManager.syncPlaylistToChannel(account, channelId, pFile);
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e("SoundGram: failed to create playlist: " + e);
                    }
                }

                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getInstance(account).removeObserver(msgDelegate, NotificationCenter.messageReceivedByServer);
                    NotificationCenter.getInstance(account).removeObserver(msgDelegate, NotificationCenter.didReceiveNewMessages);
                });

                // Обновляем счётчик треков в библиотеке
                int prevCount = MessagesController.getMainSettings(account).getInt("sg_library_track_count", 0);
                MessagesController.getMainSettings(account).edit().putInt("sg_library_track_count", prevCount + foundCount).apply();
                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);

                isRunning = false;
                currentState.isRunning = false;
                currentState.isFinished = true;
                final int fFound = foundCount;
                final String fTitle = parsed.title;
                MusicImportService.stop();
                notifyFinished(fFound, total, fTitle);

            } catch (Exception e) {
                FileLog.e(e);
                isRunning = false;
                currentState.isRunning = false;
                MusicImportService.stop();
                notifyError("Произошла ошибка при импорте: " + e.getMessage());
            }
        }).start();
    }

    private static void notifyParsingStarted() {
        AndroidUtilities.runOnUIThread(() -> {
            for (ImportListener l : globalListeners) {
                l.onParsingStarted();
            }
        });
    }

    private static void notifyParsedTracks(List<TrackInfo> tracks, String title) {
        AndroidUtilities.runOnUIThread(() -> {
            for (ImportListener l : globalListeners) {
                l.onParsedTracks(tracks, title);
            }
        });
    }

    private static void notifyTrackProgress(int current, int total, String trackName, boolean success) {
        AndroidUtilities.runOnUIThread(() -> {
            for (ImportListener l : globalListeners) {
                l.onTrackProgress(current, total, trackName, success);
            }
        });
    }

    private static void notifyFinished(int totalFound, int totalProcessed, String playlistTitle) {
        AndroidUtilities.runOnUIThread(() -> {
            for (ImportListener l : globalListeners) {
                l.onFinished(totalFound, totalProcessed, playlistTitle);
            }
        });
    }

    private static void notifyCancelled(int totalFound, int totalProcessed) {
        AndroidUtilities.runOnUIThread(() -> {
            for (ImportListener l : globalListeners) {
                l.onCancelled(totalFound, totalProcessed);
            }
        });
    }

    private static void notifyError(String error) {
        AndroidUtilities.runOnUIThread(() -> {
            for (ImportListener l : globalListeners) {
                l.onError(error);
            }
        });
    }
}
