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

    // ==========================================
    // НОВЫЕ СИНХРОННЫЕ МЕТОДЫ ДЛЯ DEEZER И HITMOS
    // ==========================================

    public static ArrayList<WebTrack> getGenreTracksSync(String genre) {
        ArrayList<WebTrack> res = new ArrayList<>();
        try {
            // Ищем плейлисты по жанру, чтобы получить максимально релевантные треки
            String searchHtml = fetchHtml("https://api.deezer.com/search/playlist?q=" + URLEncoder.encode(genre, "UTF-8"));
            JSONObject json = new JSONObject(searchHtml);
            if (json.has("data")) {
                JSONArray pList = json.getJSONArray("data");
                if (pList.length() > 0) {
                    // Берем случайный плейлист из топ-15
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
            // Очищаем от всяких (Remix) [Cover], чтобы Hitmos лучше находил
            query = query.replaceAll("(?i)\\[.*?\\]|\\(.*?\\)", "").trim();
            String html = fetchHtml("https://hitmos.me/search?q=" + URLEncoder.encode(query, "UTF-8"));
            ArrayList<WebTrack> tracks = new ArrayList<>();
            parseHtmlToResults(html, tracks, new ArrayList<>(), new ArrayList<>(), new HashSet<>());
            if (!tracks.isEmpty()) return tracks.get(0);
        } catch (Exception e) { FileLog.e(e); }
        return null;
    }

    // ==========================================
    // СТАРЫЕ МЕТОДЫ (ОСТАЮТСЯ БЕЗ ИЗМЕНЕНИЙ)
    // ==========================================

    private static String transliterate(String message) {
        char[] abcCyr = {'а','б','в','г','д','е','ё','ж','з','и','й','к','л','м','н','о','п','р','с','т','у','ф','х','ц','ч','ш','щ','ъ','ы','ь','э','ю','я'};
        String[] abcLat = {"a","b","v","g","d","e","e","zh","z","i","y","k","l","m","n","o","p","r","s","t","u","f","h","ts","ch","sh","sch","","y","","e","yu","ya"};
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {
            char c = message.toLowerCase().charAt(i);
            boolean found = false;
            for (int x = 0; x < abcCyr.length; x++) {
                if (c == abcCyr[x]) {
                    builder.append(abcLat[x]);
                    found = true;
                    break;
                }
            }
            if (!found) builder.append(c);
        }
        return builder.toString();
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

    public static void search(String query, SearchCallback callback) {
        new Thread(() -> {
            ArrayList<WebTrack> tracks = new ArrayList<>();
            ArrayList<WebArtist> artists = new ArrayList<>();
            ArrayList<WebAlbum> albums = new ArrayList<>();
            HashSet<String> seenIds = new HashSet<>();

            try {
                String htmlOriginal = fetchHtml("https://hitmos.me/search?q=" + URLEncoder.encode(query, "UTF-8"));
                parseHtmlToResults(htmlOriginal, tracks, artists, albums, seenIds);

                if (query.matches(".*[а-яА-ЯёЁ].*") && tracks.isEmpty()) {
                    String htmlTrans = fetchHtml("https://hitmos.me/search?q=" + URLEncoder.encode(transliterate(query), "UTF-8"));
                    parseHtmlToResults(htmlTrans, tracks, artists, albums, seenIds);
                }

                final String qLower = query.toLowerCase().trim();
                artists.sort((a, b) -> {
                    boolean aExact = a.name.toLowerCase().equalsIgnoreCase(qLower);
                    boolean bExact = b.name.toLowerCase().equalsIgnoreCase(qLower);
                    if (aExact && !bExact) return -1;
                    if (!aExact && bExact) return 1;
                    return 0;
                });

            } catch (Exception e) { FileLog.e("Search Error: " + e.getMessage()); }
            AndroidUtilities.runOnUIThread(() -> callback.onResult(tracks, artists, albums));
        }).start();
    }

    public static String fetchHtml(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        if (conn.getResponseCode() == 200) {
            InputStream is = conn.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int len;
            while ((len = is.read(b)) != -1) bos.write(b, 0, len);
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
                String albumsResp = fetchHtml("https://api.deezer.com/artist/" + artistId + "/albums?limit=25");
                JSONArray albumsArr = new JSONObject(albumsResp).getJSONArray("data");

                ArrayList<WebAlbum> finalAlbums = new ArrayList<>();
                for (int i = 0; i < albumsArr.length(); i++) {
                    JSONObject albObj = albumsArr.getJSONObject(i);
                    String title = albObj.getString("title");
                    String cover = albObj.getString("cover_xl");
                    finalAlbums.add(new WebAlbum(title, artistName, "SEARCH_ALBUM:" + title + "|||" + artistName, cover));
                }
                AndroidUtilities.runOnUIThread(() -> callback.onResult(finalAlbums, new ArrayList<>()));
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(callback::onFailed);
            }
        }).start();
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

                        String trackSearchUrl = "https://hitmos.me/search?q=" + URLEncoder.encode(artistName + " " + trackClean, "UTF-8");
                        String trackHtml = fetchHtml(trackSearchUrl);

                        ArrayList<WebTrack> searchTracks = new ArrayList<>();
                        parseHtmlToResults(trackHtml, searchTracks, new ArrayList<>(), new ArrayList<>(), new HashSet<>());

                        if (searchTracks.isEmpty()) {
                            trackHtml = fetchHtml("https://hitmos.me/search?q=" + URLEncoder.encode(trackClean, "UTF-8"));
                            parseHtmlToResults(trackHtml, searchTracks, new ArrayList<>(), new ArrayList<>(), new HashSet<>());
                        }

                        if (!searchTracks.isEmpty()) {
                            WebTrack wt = searchTracks.get(0);
                            wt.coverUrl = coverUrl;
                            finalCompiledTracks.add(wt);
                            AndroidUtilities.runOnUIThread(() -> callback.onTrackLoaded(wt));
                        }
                        Thread.sleep(150);
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


    public static void loadArtistProfile(String artistUrl, ArtistProfileCallback callback) {
        new Thread(() -> {
            try {
                String url = artistUrl.startsWith("http") ? artistUrl : "https://hitmos.me" + artistUrl;
                ArrayList<WebAlbum> albums = new ArrayList<>();
                ArrayList<WebTrack> tracks = new ArrayList<>();
                HashSet<String> seen = new HashSet<>();

                for (int page = 1; page <= 10; page++) {
                    String pageUrl = url + (url.endsWith("/") ? "" : "/") + "page/" + page + "/";
                    if (page == 1) pageUrl = url;

                    String html = fetchHtml(pageUrl);
                    if (html == null || html.isEmpty()) break;

                    int tracksBefore = tracks.size();
                    parseHtmlToResults(html, tracks, new ArrayList<>(), albums, seen);

                    if (tracks.size() > tracksBefore) {
                        ArrayList<WebTrack> newTracks = new ArrayList<>(tracks.subList(tracksBefore, tracks.size()));
                        AndroidUtilities.runOnUIThread(() -> {
                            for (WebTrack t : newTracks) callback.onTrackLoaded(t);
                        });
                    }

                    if (tracks.size() == tracksBefore) {
                        if (page == 1) continue;
                        String altPageUrl = url + (url.contains("?") ? "&" : "?") + "p=" + page;
                        html = fetchHtml(altPageUrl);

                        int altBefore = tracks.size();
                        parseHtmlToResults(html, tracks, new ArrayList<>(), albums, seen);

                        if (tracks.size() > altBefore) {
                            ArrayList<WebTrack> newTracks = new ArrayList<>(tracks.subList(altBefore, tracks.size()));
                            AndroidUtilities.runOnUIThread(() -> {
                                for (WebTrack t : newTracks) callback.onTrackLoaded(t);
                            });
                        }
                        if (tracks.size() == altBefore) break;
                    }
                }
                AndroidUtilities.runOnUIThread(() -> callback.onResult(albums, tracks));
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(callback::onFailed);
            }
        }).start();
    }

    public static void downloadTrack(String fileUrl, File destDir, DownloadCallback callback) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setRequestProperty("Referer", "https://hitmos.me/");
                conn.setInstanceFollowRedirects(true);
                conn.connect();

                int length = conn.getContentLength();
                File outputFile = new File(destDir, "track_" + System.currentTimeMillis() + ".mp3");

                InputStream is = new BufferedInputStream(conn.getInputStream());
                FileOutputStream os = new FileOutputStream(outputFile);

                byte[] data = new byte[8192];
                int count;
                long total = 0;
                while ((count = is.read(data)) != -1) {
                    total += count;
                    if (length > 0) {
                        int progress = (int) (total * 100 / length);
                        AndroidUtilities.runOnUIThread(() -> callback.onProgress(progress));
                    }
                    os.write(data, 0, count);
                }
                os.flush(); os.close(); is.close();
                AndroidUtilities.runOnUIThread(() -> callback.onResult(outputFile));
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(callback::onFailed);
            }
        }).start();
    }

    public static File downloadTrackSync(String fileUrl, File destDir) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Referer", "https://hitmos.me/");
            conn.setInstanceFollowRedirects(true);
            conn.connect();

            File outputFile = new File(destDir, "track_" + System.currentTimeMillis() + ".mp3");
            InputStream is = new BufferedInputStream(conn.getInputStream());
            FileOutputStream os = new FileOutputStream(outputFile);

            byte[] data = new byte[8192];
            int count;
            while ((count = is.read(data)) != -1) os.write(data, 0, count);
            os.flush(); os.close(); is.close();
            return outputFile;
        } catch (Exception e) { return null; }
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
