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
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
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
            String html = fetchHtml("https://eu.hitmoz.com/search?q=" + URLEncoder.encode(query, "UTF-8"));
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
        if (!img.isEmpty() && img.startsWith("/")) return "https://eu.hitmoz.com" + img;
        return img;
    }

    public static boolean downloadTrackToExactFileSync(String fileUrl, File destFile) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:115.0) Gecko/115.0 Firefox/115.0");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Referer", "https://eu.hitmoz.com/");
            conn.setInstanceFollowRedirects(true);
            conn.connect();

            if (destFile.exists()) destFile.delete();
            destFile.getParentFile().mkdirs();

            InputStream is = new BufferedInputStream(conn.getInputStream());
            FileOutputStream os = new FileOutputStream(destFile);

            byte[] data = new byte[8192];
            int count;
            while ((count = is.read(data)) != -1) os.write(data, 0, count);
            os.flush(); os.close(); is.close();
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            if(destFile.exists()) destFile.delete();
            return false;
        }
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

                        String trackSearchUrl = "https://eu.hitmoz.com/search?q=" + URLEncoder.encode(artistName + " " + trackClean, "UTF-8");
                        String trackHtml = fetchHtml(trackSearchUrl);

                        ArrayList<WebTrack> searchTracks = new ArrayList<>();
                        parseHtmlToResults(trackHtml, searchTracks, new ArrayList<>(), new ArrayList<>(), new HashSet<>());

                        WebTrack bestMatch = null;
                        for (WebTrack wt : searchTracks) {
                            int hitmoDur = org.telegram.ui.ChannelMusicFragment.parseDuration(wt.duration);
                            // ИСПРАВЛЕНИЕ: Жесткая проверка по строкам, чтобы не путать ремиксы и чужие треки
                            boolean titleMatch = wt.title.toLowerCase().contains(trackClean.toLowerCase()) || trackClean.toLowerCase().contains(wt.title.toLowerCase());
                            boolean artistMatch = wt.artist.toLowerCase().contains(artistName.toLowerCase()) || artistName.toLowerCase().contains(wt.artist.toLowerCase());

                            if (titleMatch && artistMatch && Math.abs(hitmoDur - exactDuration) <= 15) {
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
                String hitmoHtml = fetchHtml("https://eu.hitmoz.com/search?q=" + URLEncoder.encode(query, "UTF-8"));
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
                String baseUrl = artistUrl.startsWith("http") ? artistUrl : "https://eu.hitmoz.com" + artistUrl;
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

    public static String transliteratePublic(String input) {
        return transliterate(input);
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
                        parseHtmlToResults(fetchHtml("https://eu.hitmoz.com/search?q=" + URLEncoder.encode(q1, "UTF-8")), results, new ArrayList<>(), new ArrayList<>(), new HashSet<>());
                    } catch (Exception e) { FileLog.e(e); }

                    // Стратегия 2: транслитерация имени + название
                    if (results.isEmpty() && !translitName.equals(artistName)) {
                        try {
                            String q2 = translitName + " " + cleanTitle;
                            parseHtmlToResults(fetchHtml("https://eu.hitmoz.com/search?q=" + URLEncoder.encode(q2, "UTF-8")), results, new ArrayList<>(), new ArrayList<>(), new HashSet<>());
                        } catch (Exception e) { FileLog.e(e); }
                    }

                    // Стратегия 3: только название
                    if (results.isEmpty()) {
                        try {
                            parseHtmlToResults(fetchHtml("https://eu.hitmoz.com/search?q=" + URLEncoder.encode(cleanTitle, "UTF-8")), results, new ArrayList<>(), new ArrayList<>(), new HashSet<>());
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
    public static String fetchHtml(String urlString) throws Exception {
        try {
            return fetchHtmlInternal(urlString, false);
        } catch (javax.net.ssl.SSLException e) {
            // VPN может перехватывать SSL — пробуем с trust-all только для этого соединения
            return fetchHtmlInternal(urlString, true);
        }
    }


    private static String fetchHtmlInternal(String urlString, boolean trustAll) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Trust-all только per-connection, НЕ через SSLContext.setDefault() чтобы не сломать ExoPlayer
        if (trustAll && conn instanceof javax.net.ssl.HttpsURLConnection) {
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
                ((javax.net.ssl.HttpsURLConnection) conn).setHostnameVerifier((hostname, session) -> true);
            } catch (Exception ignored) {}
        }

        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
        conn.setRequestProperty("Connection", "keep-alive");
        conn.setRequestProperty("Upgrade-Insecure-Requests", "1");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setInstanceFollowRedirects(true);

        int responseCode = conn.getResponseCode();
        if (responseCode == 200 || responseCode == 201 || responseCode == 202) {
            InputStream is = conn.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int len;
            while ((len = is.read(b)) != -1) bos.write(b, 0, len);
            is.close();
            return bos.toString("UTF-8");
        }
        return "";
    }

    public static void parseHtmlToResults(String html, ArrayList<WebTrack> tracks, ArrayList<WebArtist> artists, ArrayList<WebAlbum> albums, HashSet<String> seen) {
        if (html == null || html.isEmpty()) return;

        String[] blocks = html.split("class=\"track__info\"");
        for (int i = 1; i < blocks.length; i++) {
            String block = blocks[i];
            String downloadUrl = extract(block, "href=\"([^\"]+\\.mp3[^\"]*)\"");
            if (downloadUrl == null) downloadUrl = extract(block, "data-url=\"([^\"]+)\"");
            if (downloadUrl == null) continue;

            if (!downloadUrl.startsWith("http")) downloadUrl = "https://eu.hitmoz.com" + downloadUrl;
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
            if (!url.startsWith("http")) url = "https://eu.hitmoz.com" + url;
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
            if (!url.startsWith("http")) url = "https://eu.hitmoz.com" + url;
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
}
