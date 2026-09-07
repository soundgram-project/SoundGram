package soundgram.music.wave;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import soundgram.music.MusicNetworkHelper;

public final class InnerTubeClient {

    private static final String TAG = "InnerTubeClient";
    private static final String BASE_URL = "https://music.youtube.com/youtubei/v1/";
    private static final String CLIENT_VERSION = "1.20231101.01.00";
    private static final String CLIENT_NAME = "WEB_REMIX";

    public static class RawTrack {
        public final String videoId;
        public final String title;
        public final String artist;
        public final int durationSec;

        public RawTrack(String videoId, String title, String artist, int durationSec) {
            this.videoId = videoId;
            this.title = title != null ? title : "";
            this.artist = artist != null ? artist : "";
            this.durationSec = durationSec;
        }

        @Override
        public String toString() {
            return artist + " - " + title + " (" + videoId + ")";
        }
    }

    public static class NextResult {
        public final List<RawTrack> tracks;
        public final String continuationToken;

        public NextResult(List<RawTrack> tracks, String continuationToken) {
            this.tracks = tracks != null ? tracks : new ArrayList<>();
            this.continuationToken = continuationToken;
        }
    }

    private InnerTubeClient() {}

    private static JSONObject buildContext() throws Exception {
        JSONObject client = new JSONObject();
        client.put("clientName", CLIENT_NAME);
        client.put("clientVersion", CLIENT_VERSION);
        client.put("hl", "ru");
        client.put("gl", "RU");

        JSONObject ctx = new JSONObject();
        ctx.put("client", client);
        return ctx;
    }

    private static String post(String endpoint, String jsonBody, AtomicBoolean cancelFlag) {
        if (cancelFlag != null && cancelFlag.get()) return "";
        try {
            URL url = new URL(BASE_URL + endpoint);
            HttpURLConnection conn = MusicNetworkHelper.openConnection(url);

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
            conn.setRequestProperty("Origin", "https://music.youtube.com");
            conn.setRequestProperty("Referer", "https://music.youtube.com/");
            conn.setRequestProperty("X-YouTube-Client-Name", "67"); // 67 = WEB_REMIX
            conn.setRequestProperty("X-YouTube-Client-Version", CLIENT_VERSION);

            byte[] bytes = jsonBody.getBytes("UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }

            int code = conn.getResponseCode();
            if (code / 100 != 2) {
                FileLog.w(TAG + " " + endpoint + " returned HTTP " + code);
                return "";
            }

            try (InputStream is = conn.getInputStream();
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = is.read(buf)) != -1) {
                    if (cancelFlag != null && cancelFlag.get()) return "";
                    bos.write(buf, 0, len);
                }
                return bos.toString("UTF-8");
            }
        } catch (Exception e) {
            FileLog.w(TAG + " post " + endpoint + " error: " + e.getMessage());
            return "";
        }
    }

    /**
     * Calls /next to get radio recommendations based on videoId or continuation token.
     */
    public static NextResult fetchRadioBatch(String videoId, String continuationToken, AtomicBoolean cancelFlag) {
        try {
            JSONObject body = new JSONObject();
            body.put("context", buildContext());

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
                return new NextResult(null, null);
            }

            String resp = post("next", body.toString(), cancelFlag);
            if (resp.isEmpty()) return new NextResult(null, null);

            JSONObject json = new JSONObject(resp);
            List<RawTrack> tracks = new ArrayList<>();
            collectPanelVideos(json, tracks);
            String nextContinuation = findContinuationToken(json);

            return new NextResult(tracks, nextContinuation);
        } catch (Exception e) {
            FileLog.e(TAG + " fetchRadioBatch error", e);
            return new NextResult(null, null);
        }
    }

    /**
     * Calls /search to search for songs by query (e.g. artist name).
     */
    public static List<RawTrack> searchSongs(String query, int limit, AtomicBoolean cancelFlag) {
        List<RawTrack> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) return results;
        try {
            JSONObject body = new JSONObject();
            body.put("context", buildContext());
            body.put("query", query.trim());
            // Songs filter params for music.youtube.com (Треки)
            body.put("params", "EgWKAQIIAWoSEAUQAxAJEAQQChAQEBUQDhAR");

            String resp = post("search", body.toString(), cancelFlag);
            if (resp.isEmpty()) {
                // Fallback without songs filter
                body.remove("params");
                resp = post("search", body.toString(), cancelFlag);
            }
            if (!resp.isEmpty()) {
                JSONObject json = new JSONObject(resp);
                collectSearchSongs(json, results, limit);
            }
            if (results.isEmpty() && body.has("params")) {
                body.remove("params");
                String respNoParams = post("search", body.toString(), cancelFlag);
                if (!respNoParams.isEmpty()) {
                    JSONObject jsonNoParams = new JSONObject(respNoParams);
                    collectSearchSongs(jsonNoParams, results, limit);
                }
            }
        } catch (Exception e) {
            FileLog.e(TAG + " searchSongs error", e);
        }
        return results;
    }

    private static String getRunsText(JSONObject obj) {
        if (obj == null) return null;
        try {
            JSONArray runs = obj.optJSONArray("runs");
            if (runs != null && runs.length() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < runs.length(); i++) {
                    String t = runs.getJSONObject(i).optString("text", null);
                    if (t != null) sb.append(t);
                }
                return sb.toString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static int parseDurationString(String str) {
        if (str == null || str.isEmpty()) return 0;
        try {
            String[] parts = str.trim().split(":");
            if (parts.length == 2) {
                return Integer.parseInt(parts[0].trim()) * 60 + Integer.parseInt(parts[1].trim());
            } else if (parts.length == 3) {
                return Integer.parseInt(parts[0].trim()) * 3600
                        + Integer.parseInt(parts[1].trim()) * 60
                        + Integer.parseInt(parts[2].trim());
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static void collectPanelVideos(Object node, List<RawTrack> out) {
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            if (obj.has("playlistPanelVideoRenderer")) {
                try {
                    JSONObject pvr = obj.getJSONObject("playlistPanelVideoRenderer");
                    String vid = pvr.optString("videoId", null);
                    String title = getRunsText(pvr.optJSONObject("title"));
                    String artist = getRunsText(pvr.optJSONObject("shortBylineText"));
                    if (artist == null || artist.trim().isEmpty()) {
                        artist = getRunsText(pvr.optJSONObject("longBylineText"));
                    }
                    String lengthText = getRunsText(pvr.optJSONObject("lengthText"));
                    int durSec = parseDurationString(lengthText);

                    if (vid != null && !vid.isEmpty() && title != null && !title.isEmpty()) {
                        out.add(new RawTrack(vid, title, artist != null ? artist : "", durSec));
                    }
                } catch (Exception ignored) {}
            }
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                try { collectPanelVideos(obj.get(k), out); } catch (Exception ignored) {}
            }
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < arr.length(); i++) {
                try { collectPanelVideos(arr.get(i), out); } catch (Exception ignored) {}
            }
        }
    }

    private static void collectSearchSongs(Object node, List<RawTrack> out, int limit) {
        if (out.size() >= limit) return;
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            if (obj.has("musicResponsiveListItemRenderer")) {
                try {
                    JSONObject r = obj.getJSONObject("musicResponsiveListItemRenderer");
                    String vid = null;
                    if (r.has("playlistItemData")) {
                        vid = r.getJSONObject("playlistItemData").optString("videoId", null);
                    }
                    JSONArray flexColumns = r.optJSONArray("flexColumns");
                    String title = null;
                    String artist = null;
                    int durSec = 0;

                    if (flexColumns != null) {
                        if (flexColumns.length() > 0) {
                            JSONObject col0 = flexColumns.getJSONObject(0).optJSONObject("musicResponsiveListItemFlexColumnRenderer");
                            if (col0 != null) title = getRunsText(col0.optJSONObject("text"));
                        }
                        if (flexColumns.length() > 1) {
                            JSONObject col1 = flexColumns.getJSONObject(1).optJSONObject("musicResponsiveListItemFlexColumnRenderer");
                            if (col1 != null) {
                                JSONObject textObj = col1.optJSONObject("text");
                                if (textObj != null) {
                                    JSONArray runs = textObj.optJSONArray("runs");
                                    if (runs != null && runs.length() > 0) {
                                        artist = runs.getJSONObject(0).optString("text", null);
                                    }
                                }
                            }
                        }
                    }

                    if (vid == null && r.has("navigationEndpoint")) {
                        JSONObject ep = r.getJSONObject("navigationEndpoint");
                        if (ep.has("watchEndpoint")) {
                            vid = ep.getJSONObject("watchEndpoint").optString("videoId", null);
                        }
                    }

                    if (vid != null && !vid.isEmpty() && title != null && !title.isEmpty()) {
                        out.add(new RawTrack(vid, title, artist != null ? artist : "", durSec));
                        if (out.size() >= limit) return;
                    }
                } catch (Exception ignored) {}
            }
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                try { collectSearchSongs(obj.get(k), out, limit); } catch (Exception ignored) {}
                if (out.size() >= limit) return;
            }
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < arr.length(); i++) {
                try { collectSearchSongs(arr.get(i), out, limit); } catch (Exception ignored) {}
                if (out.size() >= limit) return;
            }
        }
    }

    private static String findContinuationToken(Object node) {
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            if (obj.has("continuation") && obj.has("clickTrackingParams")) {
                String tok = obj.optString("continuation", null);
                if (tok != null && !tok.isEmpty()) return tok;
            }
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                try {
                    String f = findContinuationToken(obj.get(k));
                    if (f != null) return f;
                } catch (Exception ignored) {}
            }
        } else if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < arr.length(); i++) {
                try {
                    String f = findContinuationToken(arr.get(i));
                    if (f != null) return f;
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
}
