
        package org.telegram.ui;

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
            try {
                return URLDecoder.decode(noHtml, "UTF-8");
            } catch (Exception e) {
                return noHtml;
            }
        }
    }

    public interface SearchCallback {
        void onResult(ArrayList<WebTrack> tracks);
    }

    public interface DownloadCallback {
        void onProgress(int percent);
        void onResult(File file);
        void onFailed();
    }

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

    public static void search(String query, SearchCallback callback) {
        new Thread(() -> {
            ArrayList<WebTrack> results = new ArrayList<>();
            HashSet<String> seenUrls = new HashSet<>();
            try {

                String htmlOriginal = fetchHtml("https://hitmos.me/search?q=" + URLEncoder.encode(query, "UTF-8"));
                parseHtmlToTracks(htmlOriginal, results, seenUrls);


                if (query.matches(".*[а-яА-ЯёЁ].*")) {
                    String htmlTrans = fetchHtml("https://hitmos.me/search?q=" + URLEncoder.encode(transliterate(query), "UTF-8"));
                    parseHtmlToTracks(htmlTrans, results, seenUrls);
                }
            } catch (Exception e) {
                FileLog.e("Search Error: " + e.getMessage());
            }
            AndroidUtilities.runOnUIThread(() -> callback.onResult(results));
        }).start();
    }

    private static String fetchHtml(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");



        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
        conn.setRequestProperty("Referer", "https://hitmos.me/");

        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        if (conn.getResponseCode() == 200) {
            InputStream is = conn.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[4096];
            int len;
            while ((len = is.read(b)) != -1) bos.write(b, 0, len);
            return bos.toString("UTF-8");
        }
        return "";
    }

    private static void parseHtmlToTracks(String html, ArrayList<WebTrack> results, HashSet<String> seenUrls) {
        if (html == null || html.isEmpty()) return;


        String[] blocks = html.split("class=\"track__info\"");
        for (int i = 1; i < blocks.length; i++) {
            String block = blocks[i];


            Matcher mUrl = Pattern.compile("href=\"([^\"]+\\.mp3[^\"]*)\"").matcher(block);
            String dUrl = "";
            if (mUrl.find()) {
                dUrl = mUrl.group(1);
            } else {

                Matcher mDataUrl = Pattern.compile("data-url=\"([^\"]+)\"").matcher(block);
                if (mDataUrl.find()) dUrl = mDataUrl.group(1);
            }

            if (dUrl.isEmpty()) continue;
            if (dUrl.startsWith("/")) dUrl = "https://hitmos.me" + dUrl;
            if (seenUrls.contains(dUrl)) continue;
            seenUrls.add(dUrl);

            String title = "Unknown", artist = "Unknown", duration = "0:00", cover = "";

            Matcher mTitle = Pattern.compile("class=\"track__title\"[^>]*>\\s*(.*?)\\s*</div>").matcher(block);
            if (mTitle.find()) title = mTitle.group(1);

            Matcher mArtist = Pattern.compile("class=\"track__desc\"[^>]*>\\s*(.*?)\\s*</div>").matcher(block);
            if (mArtist.find()) artist = mArtist.group(1);

            Matcher mTime = Pattern.compile("class=\"track__fulltime\"[^>]*>\\s*(.*?)\\s*</div>").matcher(block);
            if (mTime.find()) duration = mTime.group(1).trim();

            results.add(new WebTrack(title, artist, dUrl, duration, cover));
        }
    }

    public static void downloadTrack(String fileUrl, File destDir, DownloadCallback callback) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
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
                os.flush();
                os.close();
                is.close();
                AndroidUtilities.runOnUIThread(() -> callback.onResult(outputFile));
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(callback::onFailed);
            }
        }).start();
    }
}