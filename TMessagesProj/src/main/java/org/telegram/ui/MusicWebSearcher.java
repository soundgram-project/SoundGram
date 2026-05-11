package org.telegram.ui;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MusicWebSearcher {

    public static class WebTrack {
        public String title;
        public String artist;
        public String downloadUrl;
        public String duration;
        public String coverUrl;

        public WebTrack(String title, String artist, String downloadUrl, String duration, String coverUrl) {
            this.title = cleanString(title);
            this.artist = cleanString(artist);
            this.downloadUrl = downloadUrl;
            this.duration = duration;
            this.coverUrl = coverUrl;
        }

        private String cleanString(String input) {
            if (input == null) return "Unknown";
            String noHtml = input.replaceAll("<[^>]*>", "").trim();
            try { return URLDecoder.decode(noHtml, "UTF-8"); } catch (Exception e) { return noHtml; }
        }
    }

    public static class WebArtist {
        public String name;
        public String artistUrl;
        public String imageUrl;

        public WebArtist(String name, String artistUrl, String imageUrl) {
            this.name = name;
            this.artistUrl = artistUrl;
            this.imageUrl = imageUrl;
        }
    }

    public static class WebAlbum {
        public String title;
        public String artist;
        public String albumUrl;
        public String coverUrl;

        public WebAlbum(String title, String artist, String albumUrl, String coverUrl) {
            this.title = title;
            this.artist = artist;
            this.albumUrl = albumUrl;
            this.coverUrl = coverUrl;
        }
    }

    public interface SearchCallback {
        void onResult(ArrayList<WebTrack> tracks, ArrayList<WebArtist> artists, ArrayList<WebAlbum> albums);
    }

    public interface DownloadCallback {
        void onProgress(int percent);
        void onResult(File file);
        void onFailed();
    }

    public interface ArtistProfileCallback {
        void onResult(ArrayList<WebAlbum> albums, ArrayList<WebTrack> tracks);
        default void onTrackLoaded(WebTrack track) {}
        void onFailed();
    }

    public interface StringCallback {
        void run(String result);
    }

    public static ArrayList<WebTrack> getGenreTracksSync(String genre) {
        ArrayList<WebTrack> res = new ArrayList<>();
        try {
            String searchHtml = fetchHtml("https://api.deezer.com/search/playlist?q=" + URLEncoder.encode(genre, "UTF-8"));
            JSONObject json = new JSONObject(searchHtml);
            if (json.has("data")) {
                JSONArray pList = json.getJSONArray("data");
                if (pList.length() > 0) {
                    int rIdx = new java.util.Random().nextInt(Math.min(15, pList.length()));
                    long pId = pList.getJSONObject(rIdx).getLong("id");

                    String tHtml = fetchHtml("https://api.deezer.com/playlist/" + pId + "/tracks?limit=50");
                    JSONArray tList = new JSONObject(tHtml).getJSONArray("data");

                    for (int i = 0; i < tList.length(); i++) {
                        JSONObject tObj = tList.getJSONObject(i);
                        String title = tObj.getString("title");
                        String artist = tObj.getJSONObject("artist").getString("name");
                        String cover = "";
                        if (tObj.has("album")) {
                            cover = tObj.getJSONObject("album").optString("cover_xl", "");
                        }
                        int dur = tObj.getInt("duration");
                        String durStr = (dur / 60) + ":" + String.format("%02d", dur % 60);
                        res.add(new WebTrack(title, artist, "", durStr, cover));
                    }
                }
            }
        } catch (Exception e) { FileLog.e(e); }
        return res;
    }

    public static ArrayList<String> getSimilarArtistsSync(String artistName) {
        ArrayList<String> res = new ArrayList<>();
        try {
            String searchHtml = fetchHtml("https://api.deezer.com/search/artist?q=" + URLEncoder.encode(artistName, "UTF-8"));
            JSONObject json = new JSONObject(searchHtml);
            if (json.has("data") && json.getJSONArray("data").length() > 0) {
                long id = json.getJSONArray("data").getJSONObject(0).getLong("id");

                String simHtml = fetchHtml("https://api.deezer.com/artist/" + id + "/related?limit=15");
                JSONArray arr = new JSONObject(simHtml).getJSONArray("data");
                for (int i = 0; i < arr.length(); i++) {
                    res.add(arr.getJSONObject(i).getString("name"));
                }
            }
        } catch (Exception e) { FileLog.e(e); }
        return res;
    }

    public static WebTrack resolveHitmosSync(String title, String artist) {
        try {
            String query = artist + " " + title;
            query = query.replaceAll("(?i)\\[.*?\\]|\\(.*?\\)", "").trim();
            String html = fetchHtml("https://hitmos.me/search?q=" + URLEncoder.encode(query, "UTF-8"));
            ArrayList<WebTrack> tracks = new ArrayList<>();
            parseHtmlToResults(html, tracks, new ArrayList<>(), new ArrayList<>(), new HashSet<>());
            if (!tracks.isEmpty()) return tracks.get(0);
        } catch (Exception e) { FileLog.e(e); }
        return null;
    }

    private static String extractImageSafe(String content) {
        String img = "";
        Matcher m1 = Pattern.compile("data-src=[\"']([^\"']+)[\"']").matcher(content);
        if (m1.find()) img = m1.group(1);
        else {
            Matcher m2 = Pattern.compile("src=[\"']([^\"']+)[\"']").matcher(content);
            if (m2.find()) {
                String s = m2.group(1);
                if (!s.contains(".js") && !s.contains(".css")) img = s;
            }
            else {
                Matcher m3 = Pattern.compile("url\\(['\"]?([^'\")]+)['\"]?\\)").matcher(content);
                if (m3.find()) img = m3.group(1);
            }
        }
        if (!img.isEmpty() && img.startsWith("/")) return "https://hitmos.me" + img;
        return img;
    }


    public static void loadAlbumWithDeezerFallback(String albumTitle, String artistName, ArtistProfileCallback callback) {
        new Thread(() -> {
            try {
                String cTitle = albumTitle.replaceAll("(?i)\\[.*?\\]|\\(.*?\\)", "").trim();
                String dSearch = "https://api.deezer.com/search/album?q=" + URLEncoder.encode(artistName + " " + cTitle, "UTF-8");
                String dResp = fetchHtml(dSearch);

                JSONObject dJson = new JSONObject(dResp);
                if (!dJson.has("data") || dJson.getJSONArray("data").length() == 0) {
                    dSearch = "https://api.deezer.com/search/album?q=" + URLEncoder.encode(cTitle, "UTF-8");
                    dResp = fetchHtml(dSearch);
                    dJson = new JSONObject(dResp);
                }

                if (dJson.has("data") && dJson.getJSONArray("data").length() > 0) {
                    long albumId = dJson.getJSONArray("data").getJSONObject(0).getLong("id");
                    String coverUrl = dJson.getJSONArray("data").getJSONObject(0).getString("cover_xl");
                    String tracksResp = fetchHtml("https://api.deezer.com/album/" + albumId + "/tracks");
                    JSONArray tracksArr = new JSONObject(tracksResp).getJSONArray("data");

                    ArrayList<WebTrack> finalCompiledTracks = new ArrayList<>();

                    for (int i = 0; i < tracksArr.length(); i++) {
                        String trackTitle = tracksArr.getJSONObject(i).getString("title");
                        String trackClean = trackTitle.replaceAll("(?i)\\[.*?\\]|\\(.*?\\)", "").trim();
                        int exactDuration = tracksArr.getJSONObject(i).getInt("duration");

                        String trackSearchUrl = "https://hitmos.me/search?q=" + URLEncoder.encode(artistName + " " + trackClean, "UTF-8");
                        String trackHtml = fetchHtml(trackSearchUrl);

                        ArrayList<WebTrack> searchTracks = new ArrayList<>();
                        parseHtmlToResults(trackHtml, searchTracks, new ArrayList<>(), new ArrayList<>(), new HashSet<>());

                        WebTrack bestMatch = null;
                        for (WebTrack wt : searchTracks) {
                            int hitmoDur = org.telegram.ui.ChannelMusicFragment.parseDuration(wt.duration);
                            // ИСПРАВЛЕНИЕ: Жесткая проверка по строкам, чтобы не путать ремиксы и чужие треки
                            boolean titleMatch = wt.title.toLowerCase().contains(trackClean.toLowerCase()) || trackClean.toLowerCase().contains(wt.title.toLowerCase());
                            boolean artistMatch = wt.artist.toLowerCase().contains(artistName.toLowerCase()) || artistName.toLowerCase().contains(wt.artist.toLowerCase());
                            if (titleMatch && ((artistMatch && Math.abs(hitmoDur - exactDuration) <= 10) || Math.abs(hitmoDur - exactDuration) <= 2)) {
                                bestMatch = wt;
                                break;
                            }

                        }

                        if (bestMatch != null) {
                            bestMatch.title = trackTitle;
                            bestMatch.artist = artistName;
                            bestMatch.coverUrl = coverUrl;

                            final WebTrack finalTrack = bestMatch;
                            finalCompiledTracks.add(finalTrack);

                            AndroidUtilities.runOnUIThread(() -> callback.onTrackLoaded(finalTrack));
                        }
                        Thread.sleep(100);
                    }
                    if(finalCompiledTracks.isEmpty()) AndroidUtilities.runOnUIThread(callback::onFailed);
                    else AndroidUtilities.runOnUIThread(() -> callback.onResult(new ArrayList<>(), finalCompiledTracks));
                } else {
                    AndroidUtilities.runOnUIThread(callback::onFailed);
                }
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(callback::onFailed);
            }
        }).start();
    }

    private static String transliterate(String input) {
        String[] abcSearch = {"а","б","в","g","д","е","ё","ж","з","и","й","к","л","м","н","о","п","р","с","т","у","ф","х","ц","ч","ш","щ","ъ","ы","ь","э","ю","я","капсайз","мэдкид"};
        String[] abcTrans = {"a","b","v","g","d","e","e","zh","z","i","y","k","l","m","n","o","p","r","s","t","u","f","h","ts","ch","sh","sh","","y","","e","yu","ya","cupsize","madk1d"};
        StringBuilder builder = new StringBuilder();
        String lower = input.toLowerCase();

        for (int i = 0; i < lower.length(); i++) {
            String s = lower.substring(i, i + 1);
            boolean found = false;
            for (int j = 0; j < abcSearch.length; j++) {
                if (abcSearch[j].equals(s)) {
                    builder.append(abcTrans[j]);
                    found = true;
                    break;
                }
            }
            if (!found) builder.append(s);
        }
        return builder.toString();
    }

    public static void search(String query, SearchCallback callback) {
        new Thread(() -> {
            ArrayList<WebTrack> tracks = new ArrayList<>();
            ArrayList<WebArtist> artists = new ArrayList<>();
            ArrayList<WebAlbum> albums = new ArrayList<>();
            HashSet<String> seenIds = new HashSet<>();

            try {
                String translitQuery = transliterate(query);
                String finalQuery = query.equals(translitQuery) ? query : query + " " + translitQuery;

                // Парсинг треков с Хитмо
                String hitmoHtml = fetchHtml("https://hitmos.me/search?q=" + URLEncoder.encode(query, "UTF-8"));
                parseHtmlToResults(hitmoHtml, tracks, new ArrayList<>(), new ArrayList<>(), seenIds);

                // ИСПРАВЛЕНИЕ: Ищем артистов без подклеивания транслита (Deezer это ненавидит)
                String artistSearch = "https://api.deezer.com/search/artist?q=" + URLEncoder.encode(query, "UTF-8");
                JSONObject aJson = new JSONObject(fetchHtml(artistSearch));
                if (aJson.has("data")) {
                    JSONArray data = aJson.getJSONArray("data");
                    for (int i = 0; i < data.length() && artists.size() < 10; i++) {
                        JSONObject obj = data.getJSONObject(i);
                        artists.add(new WebArtist(obj.getString("name"), "DEEZER_ARTIST:" + obj.getLong("id"), obj.getString("picture_xl")));
                    }
                }

                // ИСПРАВЛЕНИЕ: Ищем альбомы также без транслита
                String albumSearch = "https://api.deezer.com/search/album?q=" + URLEncoder.encode(query, "UTF-8");
                JSONObject alJson = new JSONObject(fetchHtml(albumSearch));
                if (alJson.has("data")) {
                    JSONArray data = alJson.getJSONArray("data");
                    for (int i = 0; i < data.length() && albums.size() < 10; i++) {
                        JSONObject obj = data.getJSONObject(i);
                        albums.add(new WebAlbum(obj.getString("title"), obj.getJSONObject("artist").getString("name"), "SEARCH_ALBUM:" + obj.getString("title") + "::::" + obj.getJSONObject("artist").getString("name"), obj.getString("cover_xl")));
                    }
                }
            } catch (Exception e) { FileLog.e(e); }
            AndroidUtilities.runOnUIThread(() -> callback.onResult(tracks, artists, albums));
        }).start();
    }


    public static void loadArtistProfile(String artistUrl, ArtistProfileCallback callback) {
        new Thread(() -> {
            try {
                String baseUrl = artistUrl.startsWith("http") ? artistUrl : "https://hitmos.me" + artistUrl;
                ArrayList<WebAlbum> albums = new ArrayList<>();
                ArrayList<WebTrack> tracks = new ArrayList<>();
                HashSet<String> seen = new HashSet<>();

                for (int page = 1; page <= 15; page++) {
                    String pageUrl;
                    if (page == 1) {
                        pageUrl = baseUrl;
                    } else {
                        if (baseUrl.contains("/search")) {
                            String[] parts = baseUrl.split("\\?", 2);
                            pageUrl = parts[0] + (parts[0].endsWith("/") ? "" : "/") + "page/" + page + "/?" + (parts.length > 1 ? parts[1] : "");
                        } else {
                            pageUrl = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "page/" + page + "/";
                        }
                    }

                    String html = fetchHtml(pageUrl);
                    if (html == null || html.isEmpty()) break;

                    int tracksBefore = tracks.size();
                    parseHtmlToResults(html, tracks, new ArrayList<>(), albums, seen);

                    if (tracks.size() > tracksBefore) {
                        ArrayList<WebTrack> newTracks = new ArrayList<>(tracks.subList(tracksBefore, tracks.size()));
                        AndroidUtilities.runOnUIThread(() -> {
                            for (WebTrack t : newTracks) callback.onTrackLoaded(t);
                        });
                    } else {
                        break;
                    }
                }

                // ИСПРАВЛЕНИЕ: Если треков нет, явно вызываем onFailed, чтобы триггернуть fallback
                if (tracks.isEmpty()) {
                    AndroidUtilities.runOnUIThread(callback::onFailed);
                } else {
                    AndroidUtilities.runOnUIThread(() -> callback.onResult(albums, tracks));
                }
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(callback::onFailed);
            }
        }).start();
    }

    public static void loadArtistTracksDeezer(long artistId, String artistName, ArtistProfileCallback callback) {
        new Thread(() -> {
            try {
                String url = "https://api.deezer.com/artist/" + artistId + "/top?limit=100";
                String html = fetchHtml(url);
                if (html.isEmpty()) {
                    AndroidUtilities.runOnUIThread(callback::onFailed);
                    return;
                }
                JSONObject json = new JSONObject(html);
                if (!json.has("data")) {
                    AndroidUtilities.runOnUIThread(callback::onFailed);
                    return;
                }
                JSONArray data = json.getJSONArray("data");
                String translitName = transliterate(artistName);

                for (int i = 0; i < data.length(); i++) {
                    JSONObject obj = data.getJSONObject(i);
                    String title = obj.optString("title", "");
                    if (title.isEmpty()) continue;
                    int duration = obj.optInt("duration", 0);
                    // Безопасное извлечение обложки
                    String cover = "";
                    if (obj.has("album")) {
                        cover = obj.getJSONObject("album").optString("cover_xl", "");
                    }

                    String cleanTitle = title.replaceAll("(?i)\\[.*?\\]|\\(.*?\\)", "").trim();

                    // Стратегия 1: "Имя артиста + название"
                    ArrayList<WebTrack> results = new ArrayList<>();
                    try {
                        String q1 = artistName + " " + cleanTitle;
                        parseHtmlToResults(fetchHtml("https://hitmos.me/search?q=" + URLEncoder.encode(q1, "UTF-8")), results, new ArrayList<>(), new ArrayList<>(), new HashSet<>());
                    } catch (Exception e) { FileLog.e(e); }

                    // Стратегия 2: транслитерация имени + название
                    if (results.isEmpty() && !translitName.equals(artistName)) {
                        try {
                            String q2 = translitName + " " + cleanTitle;
                            parseHtmlToResults(fetchHtml("https://hitmos.me/search?q=" + URLEncoder.encode(q2, "UTF-8")), results, new ArrayList<>(), new ArrayList<>(), new HashSet<>());
                        } catch (Exception e) { FileLog.e(e); }
                    }

                    // Стратегия 3: только название
                    if (results.isEmpty()) {
                        try {
                            parseHtmlToResults(fetchHtml("https://hitmos.me/search?q=" + URLEncoder.encode(cleanTitle, "UTF-8")), results, new ArrayList<>(), new ArrayList<>(), new HashSet<>());
                        } catch (Exception e) { FileLog.e(e); }
                    }

                    if (!results.isEmpty()) {
                        // Ищем лучший трек по длительности
                        WebTrack best = null;
                        for (WebTrack wt : results) {
                            int hitDur = org.telegram.ui.ChannelMusicFragment.parseDuration(wt.duration);
                            if (duration > 0 && Math.abs(hitDur - duration) <= 8) {
                                best = wt;
                                break;
                            }
                        }
                        if (best == null) best = results.get(0);

                        best.title = title;
                        best.artist = artistName;
                        best.coverUrl = cover;
                        final WebTrack finalTrack = best;
                        AndroidUtilities.runOnUIThread(() -> callback.onTrackLoaded(finalTrack));
                    }

                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                }
                AndroidUtilities.runOnUIThread(() -> callback.onResult(new ArrayList<>(), new ArrayList<>()));
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(callback::onFailed);
            }
        }).start();
    }
    /**
     * Следует редиректам (с правильными заголовками) и возвращает финальный URL.
     *
     * Проблема: hitmos.me редиректит на разные зеркала в зависимости от того,
     * включён ли VPN. ExoPlayer получает исходный URL и пытается открыть его
     * напрямую — без нужных заголовков (Referer, User-Agent) и без понимания
     * что нужно идти по редиректу на другой домен.
     *
     * Решение: мы сами делаем HEAD-запрос через системный прокси (который уже
     * настроен VPN), проходим все редиректы и возвращаем ExoPlayer уже
     * финальный прямой URL к аудио-файлу на зеркале.
     *
     * @param originalUrl исходный URL с hitmos.me
     * @return финальный URL после редиректов, или исходный если резолв не удался
     */
    /**
     * Резолвит финальный URL после всех редиректов.
     *
     * Ключевой момент: ExoPlayer НЕ умеет ходить через системный VPN-прокси,
     * поэтому нам нужен финальный прямой URL к аудио-файлу.
     * Стратегия:
     *  1. Пробуем через системный прокси (VPN) — получаем итоговый URL
     *  2. Если не получилось — пробуем напрямую без прокси
     *  Возвращённый URL ExoPlayer откроет сам напрямую.
     */
    public static String resolveRedirectUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isEmpty()) return originalUrl;
        if (originalUrl.startsWith("/")) return originalUrl;

        String resolved = null;

        try {
            resolved = resolveRedirectInternal(originalUrl, false, 0);
            if (resolved != null && !resolved.isEmpty()) return forceHttps(resolved);
        } catch (Exception e) {}

        try {
            resolved = resolveRedirectInternal(originalUrl, true, 0);
            if (resolved != null && !resolved.isEmpty()) return forceHttps(resolved);
        } catch (Exception e) {}

        try {
            resolved = resolveRedirectDirect(originalUrl, true, 0);
            if (resolved != null && !resolved.isEmpty()) return forceHttps(resolved);
        } catch (Exception e) {}

        try {
            resolved = resolveRedirectWithGet(originalUrl, 0);
            if (resolved != null && !resolved.isEmpty()) return forceHttps(resolved);
        } catch (Exception e) {}

        return forceHttps(originalUrl);
    }

    private static String forceHttps(String url) {
        if (url != null && url.startsWith("http://")) {
            return url.replaceFirst("http://", "https://");
        }
        return url;
    }


    private static String resolveRedirectWithGet(String urlString, int depth) throws Exception {
        if (depth > 10) return urlString;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY);
        if (conn instanceof javax.net.ssl.HttpsURLConnection) {
            try {
                javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
                sc.init(null, new javax.net.ssl.TrustManager[]{
                        new javax.net.ssl.X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
                            public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                            public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                        }
                }, new java.security.SecureRandom());
                ((javax.net.ssl.HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
                ((javax.net.ssl.HttpsURLConnection) conn).setHostnameVerifier((h, s) -> true);
            } catch (Exception ignored) {}
        }
        // Используем GET вместо HEAD и Accept-Language: ru для обхода геоблока
        try { conn.setRequestMethod("GET"); } catch (Exception e) {}
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:124.0) Gecko/124.0 Firefox/124.0");
        conn.setRequestProperty("Accept", "audio/mpeg,audio/*;q=0.9,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9");
        conn.setRequestProperty("Referer", "https://hitmos.me/");
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        // Не читаем тело — только заголовки
        int code = conn.getResponseCode();
        String location = conn.getHeaderField("Location");
        conn.disconnect();
        if ((code == 301 || code == 302 || code == 303 || code == 307 || code == 308) && location != null && !location.isEmpty()) {
            if (!location.startsWith("http")) location = new URL(url, location).toString();
            return resolveRedirectWithGet(location, depth + 1);
        }
        if (code >= 200 && code < 400) return urlString;
        return urlString;
    }

    private static String resolveRedirectDirect(String urlString, boolean trustAll, int depth) throws Exception {
        if (depth > 10) return urlString;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY);
        if (conn instanceof javax.net.ssl.HttpsURLConnection) {
            try {
                javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
                sc.init(null, new javax.net.ssl.TrustManager[]{
                        new javax.net.ssl.X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
                            public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                            public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                        }
                }, new java.security.SecureRandom());
                ((javax.net.ssl.HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
                ((javax.net.ssl.HttpsURLConnection) conn).setHostnameVerifier((h, s) -> true);
            } catch (Exception ignored) {}
        }
        try { conn.setRequestMethod("HEAD"); } catch (ProtocolException e) { conn.setRequestMethod("GET"); }
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:124.0) Gecko/124.0 Firefox/124.0");
        conn.setRequestProperty("Accept", "audio/mpeg,audio/*;q=0.9,*/*;q=0.8");
        conn.setRequestProperty("Referer", "https://hitmos.me/");
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        int code = conn.getResponseCode();
        if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
            String location = conn.getHeaderField("Location");
            conn.disconnect();
            if (location == null || location.isEmpty()) return urlString;
            if (!location.startsWith("http")) location = new URL(url, location).toString();
            return resolveRedirectDirect(location, trustAll, depth + 1);
        }
        conn.disconnect();
        return urlString;
    }

    private static String resolveRedirectInternal(String urlString, boolean trustAll, int depth)
            throws Exception {
        if (depth > 10) return urlString; // защита от петель

        URL url = new URL(urlString);

        // Используем системный прокси — он уже настроен VPN
        java.net.ProxySelector selector = java.net.ProxySelector.getDefault();
        java.util.List<java.net.Proxy> proxies = selector != null
                ? selector.select(url.toURI()) : null;
        java.net.Proxy proxy = (proxies != null && !proxies.isEmpty())
                ? proxies.get(0) : java.net.Proxy.NO_PROXY;

        HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);

        if (trustAll && conn instanceof javax.net.ssl.HttpsURLConnection) {
            try {
                javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
                sc.init(null, new javax.net.ssl.TrustManager[]{
                        new javax.net.ssl.X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new java.security.cert.X509Certificate[0];
                            }
                            public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                            public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                        }
                }, new java.security.SecureRandom());
                ((javax.net.ssl.HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
                ((javax.net.ssl.HttpsURLConnection) conn).setHostnameVerifier((h, s) -> true);
            } catch (Exception ignored) {}
        }

        try {
            conn.setRequestMethod("HEAD"); // только заголовки — не качаем файл
        } catch (ProtocolException ignored) {
            conn.setRequestMethod("GET");
        }
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Android 14; Mobile; rv:124.0) Gecko/124.0 Firefox/124.0");
        conn.setRequestProperty("Accept", "audio/mpeg,audio/*;q=0.9,*/*;q=0.8");
        conn.setRequestProperty("Referer", "https://hitmos.me/");
        conn.setInstanceFollowRedirects(false); // вручную, чтобы ловить http→https
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        try {
            int code = conn.getResponseCode();

            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null || location.isEmpty()) return urlString;
                // Относительный редирект → абсолютный
                if (!location.startsWith("http")) {
                    location = new URL(url, location).toString();
                }
                FileLog.d("resolveRedirectUrl: " + urlString + " → " + location);
                return resolveRedirectInternal(location, trustAll, depth + 1);
            }

            conn.disconnect();
            // Финальный URL — это тот, на котором получили 200/206
            FileLog.d("resolveRedirectUrl: resolved → " + urlString + " (code=" + code + ")");
            return urlString;

        } catch (javax.net.ssl.SSLException e) {
            conn.disconnect();
            if (!trustAll) {
                // VPN иногда ломает TLS — пробуем trust-all
                return resolveRedirectInternal(urlString, true, depth);
            }
            throw e;
        }
    }

    public static String fetchHtml(String urlString) throws Exception {
        try {
            return fetchHtmlInternal(urlString, false);
        } catch (javax.net.ssl.SSLException e) {
            // VPN ломает TLS — пробуем trust-all, затем напрямую
            try {
                return fetchHtmlInternal(urlString, true);
            } catch (Exception e2) {
                try {
                    return fetchHtmlDirect(urlString);
                } catch (Exception e3) {
                    FileLog.e("fetchHtml all attempts failed: " + e3);
                    throw e2;
                }
            }
        } catch (java.net.SocketTimeoutException e) {
            // Таймаут — пробуем напрямую без прокси
            try {
                return fetchHtmlDirect(urlString);
            } catch (Exception e2) {
                throw e;
            }
        } catch (java.net.SocketException e) {
            // ConnectException (подкласс SocketException) или обрыв соединения —
            // пробуем сначала trust-all, потом напрямую без прокси
            try {
                return fetchHtmlInternal(urlString, true);
            } catch (Exception e2) {
                try {
                    return fetchHtmlDirect(urlString);
                } catch (Exception e3) {
                    throw e;
                }
            }
        } catch (java.net.UnknownHostException e) {
            // DNS-блокировка при VPN — пробуем через системный прокси, затем напрямую
            try {
                return fetchHtmlViaSystemProxy(urlString);
            } catch (Exception e2) {
                try {
                    return fetchHtmlDirect(urlString);
                } catch (Exception e3) {
                    throw e;
                }
            }
        }
    }

    /** Прямое соединение без прокси — fallback когда локальный VPN-прокси не работает */
    private static String fetchHtmlDirect(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY);
        // trust-all для случаев когда VPN ломает TLS
        if (conn instanceof javax.net.ssl.HttpsURLConnection) {
            try {
                javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
                sc.init(null, new javax.net.ssl.TrustManager[]{
                        new javax.net.ssl.X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
                            public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                            public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                        }
                }, new java.security.SecureRandom());
                ((javax.net.ssl.HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
                ((javax.net.ssl.HttpsURLConnection) conn).setHostnameVerifier((h, s) -> true);
            } catch (Exception ignored) {}
        }
        applyStandardHeaders(conn);
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(12000);
        conn.setInstanceFollowRedirects(true);
        int code = conn.getResponseCode();
        if (code == 200 || code == 201 || code == 202) {
            java.io.InputStream raw = conn.getInputStream();
            String encoding = conn.getContentEncoding();
            java.io.InputStream is = "gzip".equalsIgnoreCase(encoding)
                    ? new java.util.zip.GZIPInputStream(raw) : new BufferedInputStream(raw);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[8192]; int len;
            while ((len = is.read(b)) != -1) bos.write(b, 0, len);
            is.close();
            return bos.toString("UTF-8");
        }
        return "";
    }

    /**
     * Фоллбек: запрос через системный прокси (работает при некоторых VPN-конфигурациях).
     */
    private static String fetchHtmlViaSystemProxy(String urlString) throws Exception {
        java.net.ProxySelector selector = java.net.ProxySelector.getDefault();
        java.util.List<java.net.Proxy> proxies = selector.select(new java.net.URI(urlString));
        java.net.Proxy proxy = (proxies != null && !proxies.isEmpty()) ? proxies.get(0) : java.net.Proxy.NO_PROXY;

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
        applyStandardHeaders(conn);
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(12000);
        conn.setInstanceFollowRedirects(true);

        int code = conn.getResponseCode();
        if (code == 200 || code == 201) {
            java.io.InputStream is = conn.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int len;
            while ((len = is.read(b)) != -1) bos.write(b, 0, len);
            is.close();
            return bos.toString("UTF-8");
        }
        return "";
    }

    private static void applyStandardHeaders(HttpURLConnection conn) {
        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setRequestProperty("Upgrade-Insecure-Requests", "1");
            conn.setRequestProperty("Cache-Control", "no-cache");
        } catch (ProtocolException e) {
            // Handle the case where the protocol is invalid (unlikely for "GET")
            e.printStackTrace();
        } catch (Exception e) {
            // Catch-all for other potential connection issues
            e.printStackTrace();
        }
    }


    private static String fetchHtmlInternal(String urlString, boolean trustAll) throws Exception {
        URL url = new URL(urlString);

        // Используем системный прокси если есть (помогает при VPN)
        java.net.ProxySelector selector = java.net.ProxySelector.getDefault();
        java.util.List<java.net.Proxy> proxies = selector != null
                ? selector.select(url.toURI()) : null;
        java.net.Proxy proxy = (proxies != null && !proxies.isEmpty() && proxies.get(0) != java.net.Proxy.NO_PROXY)
                ? proxies.get(0) : java.net.Proxy.NO_PROXY;

        HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);

        if (trustAll && conn instanceof javax.net.ssl.HttpsURLConnection) {
            try {
                javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
                sc.init(null, new javax.net.ssl.TrustManager[]{
                        new javax.net.ssl.X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new java.security.cert.X509Certificate[0];
                            }
                            public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                            public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                        }
                }, new java.security.SecureRandom());
                ((javax.net.ssl.HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
                ((javax.net.ssl.HttpsURLConnection) conn).setHostnameVerifier((hostname, session) -> true);
            } catch (Exception ignored) {}
        }

        applyStandardHeaders(conn);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setInstanceFollowRedirects(true);

        int responseCode = conn.getResponseCode();
        if (responseCode == 200 || responseCode == 201 || responseCode == 202) {
            // Автоматическая распаковка gzip если сервер вернул его
            java.io.InputStream raw = conn.getInputStream();
            String encoding = conn.getContentEncoding();
            java.io.InputStream is = "gzip".equalsIgnoreCase(encoding)
                    ? new java.util.zip.GZIPInputStream(raw) : new BufferedInputStream(raw);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int len;
            while ((len = is.read(b)) != -1) bos.write(b, 0, len);
            is.close();
            return bos.toString("UTF-8");
        }
        return "";
    }
    public static String transliteratePublic(String input) {
        return transliterate(input);
    }

    public static boolean downloadTrackToExactFileSync(String fileUrl, File destFile) {
        return downloadTrackInternal(fileUrl, destFile, false);
    }

    private static boolean downloadTrackInternal(String fileUrl, File destFile, boolean trustAll) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(fileUrl);

            // Используем системный прокси — ключевой фикс для VPN
            java.net.ProxySelector selector = java.net.ProxySelector.getDefault();
            java.util.List<java.net.Proxy> proxies = selector != null
                    ? selector.select(url.toURI()) : null;
            java.net.Proxy proxy = (proxies != null && !proxies.isEmpty())
                    ? proxies.get(0) : java.net.Proxy.NO_PROXY;

            conn = (HttpURLConnection) url.openConnection(proxy);

            if (trustAll && conn instanceof javax.net.ssl.HttpsURLConnection) {
                try {
                    javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
                    sc.init(null, new javax.net.ssl.TrustManager[]{
                            new javax.net.ssl.X509TrustManager() {
                                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                    return new java.security.cert.X509Certificate[0];
                                }
                                public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                                public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                            }
                    }, new java.security.SecureRandom());
                    ((javax.net.ssl.HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
                    ((javax.net.ssl.HttpsURLConnection) conn).setHostnameVerifier((h, s) -> true);
                } catch (Exception ignored) {}
            }

            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Android 14; Mobile; rv:124.0) Gecko/124.0 Firefox/124.0");
            conn.setRequestProperty("Accept", "audio/mpeg,audio/*;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Encoding", "identity"); // без gzip для бинарных данных
            conn.setRequestProperty("Referer", "https://hitmos.me/");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(60000);
            conn.setInstanceFollowRedirects(true);
            conn.connect();

            int code = conn.getResponseCode();
            // Обрабатываем редиректы вручную если нужно (301/302/307/308)
            if (code == 301 || code == 302 || code == 307 || code == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location != null && !location.isEmpty()) {
                    return downloadTrackInternal(location, destFile, trustAll);
                }
                return false;
            }

            if (code != 200 && code != 206) {
                conn.disconnect();
                // Пробуем с trust-all если не пробовали
                if (!trustAll) return downloadTrackInternal(fileUrl, destFile, true);
                return false;
            }

            destFile.getParentFile().mkdirs();
            if (destFile.exists()) destFile.delete();

            java.io.InputStream is = new BufferedInputStream(conn.getInputStream());
            FileOutputStream os = new FileOutputStream(destFile);
            byte[] buffer = new byte[16384];
            int count;
            while ((count = is.read(buffer)) != -1) os.write(buffer, 0, count);
            os.flush();
            os.close();
            is.close();
            conn.disconnect();

            // Проверяем что файл не пустой и похож на MP3
            if (destFile.length() < 1024) {
                destFile.delete();
                return false;
            }
            return true;

        } catch (javax.net.ssl.SSLException e) {
            if (conn != null) conn.disconnect();
            if (!trustAll) return downloadTrackInternal(fileUrl, destFile, true);
            FileLog.e("downloadTrack SSL failed: " + e);
            return false;
        } catch (java.net.ConnectException | java.net.SocketTimeoutException e) {
            if (conn != null) conn.disconnect();
            // Локальный прокси VPN не отвечает — пробуем напрямую без прокси
            FileLog.e("downloadTrack proxy connect failed, retrying direct: " + e);
            return downloadTrackDirect(fileUrl, destFile);
        } catch (Exception e) {
            if (conn != null) conn.disconnect();
            FileLog.e("downloadTrack failed: " + e);
            if (destFile.exists()) destFile.delete();
            return false;
        }
    }

    /** Скачивание напрямую без прокси — для работы при VPN с локальными прокси */
    private static boolean downloadTrackDirect(String fileUrl, File destFile) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(fileUrl);
            conn = (HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY);
            if (conn instanceof javax.net.ssl.HttpsURLConnection) {
                try {
                    javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
                    sc.init(null, new javax.net.ssl.TrustManager[]{
                            new javax.net.ssl.X509TrustManager() {
                                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
                                public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                                public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                            }
                    }, new java.security.SecureRandom());
                    ((javax.net.ssl.HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
                    ((javax.net.ssl.HttpsURLConnection) conn).setHostnameVerifier((h, s) -> true);
                } catch (Exception ignored) {}
            }
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:124.0) Gecko/124.0 Firefox/124.0");
            conn.setRequestProperty("Accept", "audio/mpeg,audio/*;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("Referer", "https://hitmos.me/");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(60000);
            conn.setInstanceFollowRedirects(true);
            conn.connect();
            int code = conn.getResponseCode();
            if (code == 301 || code == 302 || code == 307 || code == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location != null && !location.isEmpty()) return downloadTrackDirect(location, destFile);
                return false;
            }
            if (code != 200 && code != 206) { conn.disconnect(); return false; }
            destFile.getParentFile().mkdirs();
            if (destFile.exists()) destFile.delete();
            java.io.InputStream is = new BufferedInputStream(conn.getInputStream());
            FileOutputStream os = new FileOutputStream(destFile);
            byte[] buffer = new byte[16384]; int count;
            while ((count = is.read(buffer)) != -1) os.write(buffer, 0, count);
            os.flush(); os.close(); is.close(); conn.disconnect();
            if (destFile.length() < 1024) { destFile.delete(); return false; }
            return true;
        } catch (Exception e) {
            if (conn != null) conn.disconnect();
            FileLog.e("downloadTrackDirect failed: " + e);
            if (destFile.exists()) destFile.delete();
            return false;
        }
    }
    public static String getSafeFileName(String artist, String title) {
        String safe = (artist + " - " + title)
                .replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_")
                .replaceAll("\\s+", " ")
                .trim();
        if (safe.length() > 200) safe = safe.substring(0, 200).trim();
        return safe + ".mp3";
    }
    public static void parseHtmlToResults(String html, ArrayList<WebTrack> tracks, ArrayList<WebArtist> artists, ArrayList<WebAlbum> albums, HashSet<String> seen) {
        if (html == null || html.isEmpty()) return;

        String[] blocks = html.split("class=\"track__info\"");
        for (int i = 1; i < blocks.length; i++) {
            String block = blocks[i];
            String downloadUrl = extract(block, "href=\"([^\"]+\\.mp3[^\"]*)\"");
            if (downloadUrl == null) downloadUrl = extract(block, "data-url=\"([^\"]+)\"" +
                    "");
            if (downloadUrl == null) continue;

            if (!downloadUrl.startsWith("http")) downloadUrl = "https://hitmos.me" + downloadUrl;
            if (seen.contains("TR_" + downloadUrl)) continue;
            seen.add("TR_" + downloadUrl);

            String title = extract(block, "class=\"track__title\"[^>]*>\\s*(.*?)\\s*</");
            String artist = extract(block, "class=\"track__desc\"[^>]*>\\s*(.*?)\\s*</");
            String duration = extract(block, "class=\"track__fulltime\"[^>]*>\\s*(.*?)\\s*</");
            String cover = extractImageSafe(block);

            tracks.add(new WebTrack(
                    title != null ? title.replaceAll("<[^>]*>", "").trim() : "Unknown",
                    artist != null ? artist.replaceAll("<[^>]*>", "").trim() : "Unknown",
                    downloadUrl,
                    duration != null ? duration : "0:00",
                    cover
            ));
        }

        Matcher mArt = Pattern.compile("<a[^>]+href=[\"']([^\"']*/artist/[^\"']+)[\"'][^>]*>(.*?)</a>", Pattern.DOTALL).matcher(html);
        while (mArt.find()) {
            String url = mArt.group(1);
            if (!url.startsWith("http")) url = "https://hitmos.me" + url;
            String innerHtml = mArt.group(2);
            String name = innerHtml.replaceAll("<[^>]*>", "").trim();

            if (!android.text.TextUtils.isEmpty(name) && !seen.contains("ART_" + url)) {
                seen.add("ART_" + url);
                artists.add(new WebArtist(name, url, extractImageSafe(innerHtml)));
            }
        }

        Matcher mAlb = Pattern.compile("<a[^>]+href=[\"']([^\"']*/album/[^\"']+)[\"'][^>]*>(.*?)</a>", Pattern.DOTALL).matcher(html);
        while (mAlb.find()) {
            String url = mAlb.group(1);
            if (!url.startsWith("http")) url = "https://hitmos.me" + url;
            String innerHtml = mAlb.group(2);
            String title = innerHtml.replaceAll("<[^>]*>", "").trim();

            if (!android.text.TextUtils.isEmpty(title) && !title.toLowerCase().contains("скачать") && !seen.contains("ALB_" + url)) {
                seen.add("ALB_" + url);
                albums.add(new WebAlbum(title, "Unknown", url, extractImageSafe(innerHtml)));
            }
        }
    }

    private static String extract(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    public static void loadArtistAlbumsViaDeezer(String artistName, ArtistProfileCallback callback) {
        new Thread(() -> {
            try {
                String cleanArtist = artistName.replaceAll("(?i)\\[.*?\\]|\\(.*?\\)", "").trim();
                String searchUrl = "https://api.deezer.com/search/artist?q=" + URLEncoder.encode(cleanArtist, "UTF-8");
                String response = fetchHtml(searchUrl);
                if (response.isEmpty()) { AndroidUtilities.runOnUIThread(callback::onFailed); return; }

                JSONObject json = new JSONObject(response);
                if (!json.has("data") || json.getJSONArray("data").length() == 0) {
                    AndroidUtilities.runOnUIThread(callback::onFailed); return;
                }

                long artistId = json.getJSONArray("data").getJSONObject(0).getLong("id");
                String albumsResp = fetchHtml("https://api.deezer.com/artist/" + artistId + "/albums?limit=50");
                JSONArray albumsArr = new JSONObject(albumsResp).getJSONArray("data");

                ArrayList<WebAlbum> finalAlbums = new ArrayList<>();
                for (int i = 0; i < albumsArr.length(); i++) {
                    JSONObject albObj = albumsArr.getJSONObject(i);
                    String title = albObj.getString("title");
                    String cover = albObj.getString("cover_xl");
                    finalAlbums.add(new WebAlbum(title, artistName, "SEARCH_ALBUM:" + title + "::::" + artistName, cover));
                }
                AndroidUtilities.runOnUIThread(() -> callback.onResult(finalAlbums, new ArrayList<>()));
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(callback::onFailed);
            }
        }).start();
    }

    public static ArrayList<String> getArtistGenresSync(String artistName) {
        ArrayList<String> genres = new ArrayList<>();
        try {
            // Ищем артиста
            String searchResp = fetchHtml("https://api.deezer.com/search/artist?q="
                    + URLEncoder.encode(artistName, "UTF-8") + "&limit=1");
            JSONObject json = new JSONObject(searchResp);
            if (!json.has("data") || json.getJSONArray("data").length() == 0) return genres;

            long artistId = json.getJSONArray("data").getJSONObject(0).getLong("id");

            // Получаем топ-треки артиста и собираем жанры из их альбомов
            String topResp = fetchHtml("https://api.deezer.com/artist/" + artistId + "/top?limit=10");
            JSONObject topJson = new JSONObject(topResp);
            if (!topJson.has("data")) return genres;

            JSONArray tracks = topJson.getJSONArray("data");
            java.util.LinkedHashMap<String, Integer> genreCount = new java.util.LinkedHashMap<>();

            for (int i = 0; i < Math.min(tracks.length(), 5); i++) {
                JSONObject track = tracks.getJSONObject(i);
                if (!track.has("album")) continue;
                long albumId = track.getJSONObject("album").getLong("id");

                try {
                    String albumResp = fetchHtml("https://api.deezer.com/album/" + albumId);
                    JSONObject albumJson = new JSONObject(albumResp);
                    if (albumJson.has("genres")) {
                        JSONArray genresArr = albumJson.getJSONObject("genres").getJSONArray("data");
                        for (int j = 0; j < genresArr.length(); j++) {
                            String genre = genresArr.getJSONObject(j).getString("name");
                            genreCount.merge(genre, 1, Integer::sum);
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Сортируем по частоте
            genres.addAll(genreCount.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .map(Map.Entry::getKey)
                    .collect(java.util.stream.Collectors.toList()));

        } catch (Exception e) { FileLog.e(e); }
        return genres;
    }

    /**
     * Возвращает список жанров альбома через Deezer API.
     */
    public static ArrayList<String> getAlbumGenresSync(String albumTitle, String artistName) {
        ArrayList<String> genres = new ArrayList<>();
        try {
            String query = (artistName.isEmpty() ? albumTitle : artistName + " " + albumTitle);
            String searchResp = fetchHtml("https://api.deezer.com/search/album?q="
                    + URLEncoder.encode(query, "UTF-8") + "&limit=1");
            JSONObject json = new JSONObject(searchResp);
            if (!json.has("data") || json.getJSONArray("data").length() == 0) return genres;

            long albumId = json.getJSONArray("data").getJSONObject(0).getLong("id");
            String albumResp = fetchHtml("https://api.deezer.com/album/" + albumId);
            JSONObject albumJson = new JSONObject(albumResp);

            if (albumJson.has("genres")) {
                JSONArray genresArr = albumJson.getJSONObject("genres").getJSONArray("data");
                for (int i = 0; i < genresArr.length(); i++) {
                    genres.add(genresArr.getJSONObject(i).getString("name"));
                }
            }
        } catch (Exception e) { FileLog.e(e); }
        return genres;
    }
    /**
     * Полная загрузка треков артиста через дискографию Deezer.
     *
     * Стратегия:
     *  1. Ищем артиста в Deezer → получаем artistId
     *  2. Получаем все альбомы (/albums?limit=100, с пагинацией через next)
     *  3. Для каждого альбома: /album/{id}/tracks → список треков с duration
     *  4. Для каждого трека ищем скачиваемую версию на hitmoz (3 стратегии)
     *  5. Найденные треки отдаём через onTrackLoaded в реальном времени
     *
     * Лимит: до 500 треков (настраивается через MAX_TRACKS).
     * Дедупликация по «артист + title» чтобы одни и те же треки из разных
     * изданий альбома не дублировались.
     */
    public static void loadAllArtistTracksDeezer(long artistId, String artistName,
                                                 ArtistProfileCallback callback) {
        new Thread(() -> {
            final int MAX_TRACKS = 500;
            try {
                String translitName = transliterate(artistName);
                java.util.Set<String> seenTitles = java.util.Collections.synchronizedSet(new HashSet<>());

                // Шаг 1: получаем все альбомы артиста (пагинация)
                ArrayList<long[]> albumEntries = new ArrayList<>();
                ArrayList<String> albumCovers  = new ArrayList<>();
                String albumsUrl = "https://api.deezer.com/artist/" + artistId + "/albums?limit=100&index=0";
                while (albumsUrl != null) {
                    String resp = fetchHtml(albumsUrl);
                    if (resp == null || resp.isEmpty()) break;
                    JSONObject albumsJson = new JSONObject(resp);
                    if (!albumsJson.has("data")) break;
                    JSONArray albumsArr = albumsJson.getJSONArray("data");
                    for (int ai = 0; ai < albumsArr.length(); ai++) {
                        JSONObject albObj = albumsArr.getJSONObject(ai);
                        albumEntries.add(new long[]{albObj.getLong("id")});
                        albumCovers.add(albObj.optString("cover_xl", ""));
                    }
                    albumsUrl = albumsJson.has("next") ? albumsJson.getString("next") : null;
                }

                // Шаг 2: собираем все треки из всех альбомов
                ArrayList<String[]> allTracks = new ArrayList<>();
                for (int ai = 0; ai < albumEntries.size() && allTracks.size() < MAX_TRACKS; ai++) {
                    long albId = albumEntries.get(ai)[0];
                    String cover = albumCovers.get(ai);
                    String tracksResp = fetchHtml("https://api.deezer.com/album/" + albId + "/tracks");
                    if (tracksResp == null || tracksResp.isEmpty()) continue;
                    JSONObject tracksJson = new JSONObject(tracksResp);
                    if (!tracksJson.has("data")) continue;
                    JSONArray tracksArr = tracksJson.getJSONArray("data");
                    for (int ti = 0; ti < tracksArr.length() && allTracks.size() < MAX_TRACKS; ti++) {
                        JSONObject tObj = tracksArr.getJSONObject(ti);
                        String trackTitle = tObj.optString("title", "");
                        if (trackTitle.isEmpty()) continue;
                        String dedupeKey = (artistName + "|" + trackTitle).toLowerCase();
                        if (seenTitles.contains(dedupeKey)) continue;
                        seenTitles.add(dedupeKey);
                        allTracks.add(new String[]{trackTitle, String.valueOf(tObj.optInt("duration", 0)), cover});
                    }
                }

                if (allTracks.isEmpty()) {
                    AndroidUtilities.runOnUIThread(callback::onFailed);
                    return;
                }

                // Шаг 3: параллельный поиск на Hitmos с сохранением порядка дискографии — 8 потоков
                int totalDiscographyTracks = allTracks.size();
                ExecutorService executor = Executors.newFixedThreadPool(8);
                AtomicInteger foundCount = new AtomicInteger(0);
                CountDownLatch latch = new CountDownLatch(totalDiscographyTracks);
                WebTrack[] orderedDiscographyResults = new WebTrack[totalDiscographyTracks];

                for (int idx = 0; idx < totalDiscographyTracks; idx++) {
                    final int trackIdx = idx;
                    final String trackTitle = allTracks.get(idx)[0];
                    final int duration = Integer.parseInt(allTracks.get(idx)[1]);
                    final String cover = allTracks.get(idx)[2];
                    final String cleanTitle = trackTitle.replaceAll("(?i)\\[.*?\\]|\\(.*?\\)", "").trim();

                    executor.submit(() -> {
                        try {
                            ArrayList<WebTrack> results = new ArrayList<>();
                            try {
                                parseHtmlToResults(fetchHtml("https://hitmos.me/search?q=" + URLEncoder.encode(artistName + " " + cleanTitle, "UTF-8")), results, new ArrayList<>(), new ArrayList<>(), new HashSet<>());
                            } catch (Exception e) { FileLog.e(e); }
                            if (results.isEmpty() && !translitName.equals(artistName)) {
                                try {
                                    parseHtmlToResults(fetchHtml("https://hitmos.me/search?q=" + URLEncoder.encode(translitName + " " + cleanTitle, "UTF-8")), results, new ArrayList<>(), new ArrayList<>(), new HashSet<>());
                                } catch (Exception e) { FileLog.e(e); }
                            }
                            // Поиск только по названию без артиста убран — возвращает чужие треки
                            // Поиск только по названию без артиста убран — возвращает чужие треки
                            if (!results.isEmpty()) {
                                // Меняем strictArtist на false. Хелпер findBestMatch устроен так, что при false
                                // он все равно пропустит трек чужого артиста, ТОЛЬКО если длительность совпадет тютелька в тютельку (наберет 45+ очков).
                                WebTrack best = findBestMatch(results, cleanTitle, artistName, duration, false);

                                if (best != null) {

                                    best.title  = trackTitle;
                                    best.artist = artistName;
                                    if (!cover.isEmpty()) best.coverUrl = cover;
                                    foundCount.incrementAndGet();
                                    orderedDiscographyResults[trackIdx] = best;
                                }
                            }
                        } catch (Exception e) {
                            FileLog.e(e);
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                executor.shutdown();
                latch.await();

                if (foundCount.get() == 0) {
                    AndroidUtilities.runOnUIThread(callback::onFailed);
                } else {
                    ArrayList<WebTrack> orderedDiscographyTracks = new ArrayList<>();
                    for (WebTrack wt : orderedDiscographyResults) {
                        if (wt != null) orderedDiscographyTracks.add(wt);
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        for (WebTrack wt : orderedDiscographyTracks) callback.onTrackLoaded(wt);
                        callback.onResult(new ArrayList<>(), new ArrayList<>());
                    });
                }

            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(callback::onFailed);
            }
        }).start();
    }

    public static void fetchArtistPictureFromDeezer(String artistName, StringCallback callback) {
        new Thread(() -> {
            try {
                String urlString = "https://api.deezer.com/search/artist?q=" + URLEncoder.encode(artistName, "UTF-8");
                HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    InputStream is = conn.getInputStream();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] b = new byte[8192];
                    int len;
                    while ((len = is.read(b)) != -1) bos.write(b, 0, len);
                    String json = bos.toString("UTF-8");

                    org.json.JSONObject jsonObject = new org.json.JSONObject(json);
                    org.json.JSONArray data = jsonObject.getJSONArray("data");
                    if (data.length() > 0) {
                        String picUrl = data.getJSONObject(0).getString("picture_xl");
                        AndroidUtilities.runOnUIThread(() -> callback.run(picUrl));
                        return;
                    }
                }
                AndroidUtilities.runOnUIThread(() -> callback.run(null));
            } catch (Exception e) { AndroidUtilities.runOnUIThread(() -> callback.run(null)); }
        }).start();
    }
    /**
     * Строгий синхронный поиск трека по артисту и названию.
     * Используется SmartRadioEngine для поиска конкретных треков.
     * Параметр flags зарезервирован для будущего использования (передавайте 0).
     */
    public static WebTrack searchTrackStrict(String artist, String title, int flags) {
        try {
            String query = artist + " " + title;
            query = query.replaceAll("(?i)\\[.*?\\]|\\(.*?\\)", "").trim();
            String html = fetchHtml("https://hitmos.me/search?q=" + URLEncoder.encode(query, "UTF-8"));
            ArrayList<WebTrack> tracks = new ArrayList<>();
            parseHtmlToResults(html, tracks, new ArrayList<>(), new ArrayList<>(), new HashSet<>());
            if (!tracks.isEmpty()) {
                // Пробуем найти лучшее совпадение с проверкой артиста и тайтла
                WebTrack best = findBestMatch(tracks, title, artist, 0, false);
                if (best != null) return best;
                // Фолбэк — первый результат
                return tracks.get(0);
            }
        } catch (Exception e) { FileLog.e(e); }
        return null;
    }

    private static WebTrack findBestMatch(ArrayList<WebTrack> hitmosResults, String expectedTitle, String expectedArtist, int expectedDuration, boolean strictArtist) {
        String cleanExpectedTitle = expectedTitle.replaceAll("(?i)\\[.*?\\]|\\(.*?\\)", "").trim().toLowerCase();
        String cleanExpectedArtist = expectedArtist.toLowerCase().trim();

        WebTrack best = null;
        int bestScore = -1;

        for (WebTrack wt : hitmosResults) {
            String hitTitle = wt.title.toLowerCase();
            String hitArtist = wt.artist.toLowerCase().trim();

            boolean titleMatch = hitTitle.contains(cleanExpectedTitle) || cleanExpectedTitle.contains(hitTitle);

            // ИСПРАВЛЕНО: "Исполнитель точь в точь совпадает с именем"
            boolean exactArtistMatch = hitArtist.equals(cleanExpectedArtist);
            boolean artistMatch = exactArtistMatch || hitArtist.contains(cleanExpectedArtist) || cleanExpectedArtist.contains(hitArtist);

            if (!titleMatch) continue;

            // При strictArtist должно быть точное совпадение имени исполнителя
            if (strictArtist && !exactArtistMatch) continue;

            int hitDur = org.telegram.ui.ChannelMusicFragment.parseDuration(wt.duration);
            int durDiff = expectedDuration > 0 ? Math.abs(hitDur - expectedDuration) : 0;

            int score = 0;
            if (artistMatch) score += 50;
            if (exactArtistMatch) score += 50;
            score += 20;

            if (expectedDuration > 0 && durDiff <= 5) score += 50;
            else if (expectedDuration > 0 && durDiff <= 15) score += 25;
            else if (expectedDuration > 0 && durDiff > 30) score -= 20;

            if (score > bestScore) {
                bestScore = score;
                best = wt;
            }
        }

        if (strictArtist && bestScore >= 0) return best;
        if (bestScore >= 45) return best;
        return null;
    }


}
