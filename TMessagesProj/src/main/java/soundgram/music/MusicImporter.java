package soundgram.music;

import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
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

    public interface ImportListener {
        void onParsingStarted();
        void onParsedTracks(List<TrackInfo> tracks);
        void onTrackProgress(int current, int total, String trackName, boolean success);
        void onFinished(int totalFound, int totalProcessed);
        void onError(String error);
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
    private static final Pattern YANDEX_PLAYLIST_PATTERN =
            Pattern.compile("music\\.yandex\\.[a-z]+/users/([^/]+)/playlists/(\\d+)");
    private static final Pattern YANDEX_ALBUM_PATTERN =
            Pattern.compile("music\\.yandex\\.[a-z]+/album/(\\d+)");

    private static final Pattern VK_PATTERN =
            Pattern.compile("vk\\.com/(?:music|audio|wall)");

    // ── Парсинг входных данных ────────────────────────────────────────────────
    public static List<TrackInfo> parseInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String trimmed = input.trim();

        // 1. Spotify
        if (trimmed.contains("spotify.com") || trimmed.contains("spotify.link")) {
            List<TrackInfo> spotifyTracks = parseSpotify(trimmed);
            if (!spotifyTracks.isEmpty()) return spotifyTracks;
        }

        // 2. Яндекс Музыка
        if (trimmed.contains("music.yandex") || trimmed.contains("ya.ru")) {
            List<TrackInfo> yandexTracks = parseYandexMusic(trimmed);
            if (!yandexTracks.isEmpty()) return yandexTracks;
        }

        // 3. VK
        if (VK_PATTERN.matcher(trimmed).find()) {
            List<TrackInfo> vkTracks = parseVk(trimmed);
            if (!vkTracks.isEmpty()) return vkTracks;
        }

        // 4. Текстовый список (построчный: Исполнитель - Трек)
        return parseTextList(trimmed);
    }

    // ── Spotify ────────────────────────────────────────────────────────────────
    private static List<TrackInfo> parseSpotify(String url) {
        List<TrackInfo> list = new ArrayList<>();
        try {
            Matcher mTrack = SPOTIFY_TRACK_PATTERN.matcher(url);
            if (mTrack.find()) {
                String trackId = mTrack.group(1);
                String embedHtml = MusicWebSearcher.fetchHtml("https://open.spotify.com/embed/track/" + trackId);
                extractTracksFromSpotifyEmbed(embedHtml, list);
                if (list.isEmpty()) {
                    // Fallback to oEmbed
                    try {
                        String oembed = MusicWebSearcher.fetchHtml("https://open.spotify.com/oembed?url=" + URLEncoder.encode("https://open.spotify.com/track/" + trackId, "UTF-8"));
                        JSONObject obj = new JSONObject(oembed);
                        String title = obj.optString("title");
                        if (!TextUtils.isEmpty(title)) {
                            list.add(new TrackInfo(title, ""));
                        }
                    } catch (Exception ignored) {}
                }
                return list;
            }

            Matcher mAlbum = SPOTIFY_ALBUM_PATTERN.matcher(url);
            if (mAlbum.find()) {
                String albumId = mAlbum.group(1);
                String embedHtml = MusicWebSearcher.fetchHtml("https://open.spotify.com/embed/album/" + albumId);
                extractTracksFromSpotifyEmbed(embedHtml, list);
                return list;
            }

            Matcher mPlaylist = SPOTIFY_PLAYLIST_PATTERN.matcher(url);
            if (mPlaylist.find()) {
                String playlistId = mPlaylist.group(1);
                String embedHtml = MusicWebSearcher.fetchHtml("https://open.spotify.com/embed/playlist/" + playlistId);
                extractTracksFromSpotifyEmbed(embedHtml, list);
                return list;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    private static void extractTracksFromSpotifyEmbed(String html, List<TrackInfo> outList) {
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

                if (entity.has("trackList")) {
                    JSONArray tracks = entity.getJSONArray("trackList");
                    for (int i = 0; i < tracks.length(); i++) {
                        JSONObject t = tracks.getJSONObject(i);
                        String title = t.optString("title", t.optString("name", ""));
                        String artist = t.optString("subtitle", "");
                        if (TextUtils.isEmpty(artist) && t.has("artists")) {
                            JSONArray artists = t.optJSONArray("artists");
                            if (artists != null && artists.length() > 0) {
                                artist = artists.getJSONObject(0).optString("name", "");
                            }
                        }
                        if (!TextUtils.isEmpty(title)) {
                            outList.add(new TrackInfo(title, artist));
                        }
                    }
                } else if ("track".equalsIgnoreCase(entity.optString("type"))) {
                    String title = entity.optString("title", entity.optString("name", ""));
                    String artist = entity.optString("subtitle", "");
                    if (TextUtils.isEmpty(artist) && entity.has("artists")) {
                        JSONArray artists = entity.optJSONArray("artists");
                        if (artists != null && artists.length() > 0) {
                            artist = artists.getJSONObject(0).optString("name", "");
                        }
                    }
                    if (!TextUtils.isEmpty(title)) {
                        outList.add(new TrackInfo(title, artist));
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }

        // Запасной вариант: парсинг регулярными выражениями пар title/subtitle
        if (outList.isEmpty()) {
            Pattern trackRegex = Pattern.compile("\"title\":\"([^\"]+)\",\"subtitle\":\"([^\"]+)\"");
            Matcher tm = trackRegex.matcher(html);
            while (tm.find()) {
                String title = tm.group(1);
                String artist = tm.group(2);
                if (!"Spotify".equalsIgnoreCase(artist) && !TextUtils.isEmpty(title)) {
                    outList.add(new TrackInfo(title, artist));
                }
            }
        }
    }

    // ── Яндекс Музыка ─────────────────────────────────────────────────────────
    private static List<TrackInfo> parseYandexMusic(String url) {
        List<TrackInfo> list = new ArrayList<>();
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
                        list.add(new TrackInfo(title, artist));
                    }
                }
                return list;
            }

            // 2. Плейлист
            Matcher mPlaylist = YANDEX_PLAYLIST_PATTERN.matcher(url);
            if (mPlaylist.find()) {
                String user = mPlaylist.group(1);
                String kind = mPlaylist.group(2);
                String apiJson = MusicWebSearcher.fetchHtml("https://api.music.yandex.net/users/" + user + "/playlists/" + kind);
                JSONObject root = new JSONObject(apiJson);
                JSONObject res = root.optJSONObject("result");
                if (res != null && res.has("tracks")) {
                    JSONArray tracks = res.getJSONArray("tracks");
                    for (int i = 0; i < tracks.length(); i++) {
                        JSONObject item = tracks.getJSONObject(i);
                        JSONObject t = item.optJSONObject("track");
                        if (t != null) {
                            String title = t.optString("title");
                            String artist = "";
                            JSONArray artists = t.optJSONArray("artists");
                            if (artists != null && artists.length() > 0) {
                                artist = artists.getJSONObject(0).optString("name");
                            }
                            if (!TextUtils.isEmpty(title)) {
                                list.add(new TrackInfo(title, artist));
                            }
                        }
                    }
                }
                return list;
            }

            // 3. Альбом
            Matcher mAlbum = YANDEX_ALBUM_PATTERN.matcher(url);
            if (mAlbum.find()) {
                String albumId = mAlbum.group(1);
                try {
                    String pageHtml = MusicWebSearcher.fetchHtml("https://music.yandex.ru/album/" + albumId);
                    Pattern ldJsonPattern = Pattern.compile("<script type=\"application/ld\\+json\">([^<]+)</script>");
                    Matcher ldm = ldJsonPattern.matcher(pageHtml);
                    if (ldm.find()) {
                        JSONObject ld = new JSONObject(ldm.group(1));
                        String albumArtist = "";
                        if (ld.has("byArtist")) {
                            albumArtist = ld.getJSONObject("byArtist").optString("name", "");
                        }
                        if (ld.has("tracks")) {
                            JSONArray tracks = ld.getJSONArray("tracks");
                            for (int i = 0; i < tracks.length(); i++) {
                                JSONObject t = tracks.getJSONObject(i);
                                String title = t.optString("name");
                                if (!TextUtils.isEmpty(title)) {
                                    list.add(new TrackInfo(title, albumArtist));
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                if (list.isEmpty()) {
                    String apiJson = MusicWebSearcher.fetchHtml("https://api.music.yandex.net/albums/" + albumId + "/with-tracks");
                    JSONObject root = new JSONObject(apiJson);
                    JSONObject res = root.optJSONObject("result");
                    if (res != null && res.has("volumes")) {
                        JSONArray volumes = res.getJSONArray("volumes");
                        for (int v = 0; v < volumes.length(); v++) {
                            JSONArray vTracks = volumes.getJSONArray(v);
                            for (int i = 0; i < vTracks.length(); i++) {
                                JSONObject t = vTracks.getJSONObject(i);
                                String title = t.optString("title");
                                String artist = "";
                                JSONArray artists = t.optJSONArray("artists");
                                if (artists != null && artists.length() > 0) {
                                    artist = artists.getJSONObject(0).optString("name");
                                }
                                if (!TextUtils.isEmpty(title)) {
                                    list.add(new TrackInfo(title, artist));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    // ── VK ────────────────────────────────────────────────────────────────────
    private static List<TrackInfo> parseVk(String url) {
        List<TrackInfo> list = new ArrayList<>();
        try {
            String html = MusicWebSearcher.fetchHtml(url);
            if (!TextUtils.isEmpty(html)) {
                // Поиск audio_row
                Pattern pRow = Pattern.compile("class=\"audio_row__title_inner[^\"]*\">([^<]+)<.*?class=\"audio_row__performers[^\"]*\">([^<]+)<", Pattern.DOTALL);
                Matcher m = pRow.matcher(html);
                while (m.find()) {
                    String title = m.group(1).trim();
                    String artist = m.group(2).trim();
                    if (!TextUtils.isEmpty(title)) {
                        list.add(new TrackInfo(title, artist));
                    }
                }

                if (list.isEmpty()) {
                    // OpenGraph title fallback
                    Pattern pOg = Pattern.compile("<meta property=\"og:title\" content=\"([^\"]+)\"");
                    Matcher mog = pOg.matcher(html);
                    if (mog.find()) {
                        String full = mog.group(1).trim();
                        if (full.contains(" — ") || full.contains(" - ")) {
                            String[] parts = full.split(" [—-] ", 2);
                            list.add(new TrackInfo(parts[1].trim(), parts[0].trim()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    // ── Текстовый список ───────────────────────────────────────────────────────
    public static List<TrackInfo> parseTextList(String text) {
        List<TrackInfo> list = new ArrayList<>();
        if (text == null) return list;
        String[] lines = text.split("\r?\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Удаляем начальную нумерацию: "1. ", "01. ", "1) ", "[1] ", "#1 "
            line = line.replaceAll("^\\s*(?:\\[?\\d+\\]?[\\.\\)\\-:]|#\\d+)\\s*", "").trim();

            // Удаляем длительность в конце: "(3:45)", "[03:45]", "03:45"
            line = line.replaceAll("\\s*[\\(\\[]?\\d+:\\d+[\\)\\]]?\\s*$", "").trim();

            if (line.isEmpty()) continue;

            // Разделители: " — ", " – ", " - ", " : ", " // ", " | "
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

            // Строка без разделителя
            if (line.length() >= 2) {
                list.add(new TrackInfo(line, ""));
            }
        }
        return list;
    }

    // ── Поиск через Хитмос и добавление в библиотеку ────────────────────────────
    public static void startImport(int account, String input, ImportListener listener) {
        if (listener == null) return;
        AndroidUtilities.runOnUIThread(listener::onParsingStarted);

        new Thread(() -> {
            try {
                List<TrackInfo> tracks = parseInput(input);
                if (tracks.isEmpty()) {
                    AndroidUtilities.runOnUIThread(() ->
                            listener.onError("Не удалось распознать треки.\nПроверьте ссылку или вставьте список текстом (Исполнитель - Трек).")
                    );
                    return;
                }

                AndroidUtilities.runOnUIThread(() -> listener.onParsedTracks(tracks));

                long channelId = ChannelMusicFragment.getChannelIdFromFile(account);
                if (channelId == 0) {
                    AndroidUtilities.runOnUIThread(() ->
                            listener.onError("Библиотека не найдена. Сначала откройте раздел музыки в приложении.")
                    );
                    return;
                }

                int foundCount = 0;
                int total = tracks.size();

                for (int i = 0; i < total; i++) {
                    TrackInfo ti = tracks.get(i);
                    final int currentIndex = i + 1;
                    final String trackName = ti.toString();

                    MusicWebSearcher.WebTrack hitmosTrack = null;
                    try {
                        // 1. Точный поиск Hitmos
                        hitmosTrack = MusicWebSearcher.resolveHitmosSync(ti.title, ti.artist);

                        // 2. Если не найден, пробуем скомбинированный запрос
                        if (hitmosTrack == null && !TextUtils.isEmpty(ti.artist)) {
                            hitmosTrack = MusicWebSearcher.resolveHitmosSync(ti.artist + " " + ti.title, "");
                        }

                        // 3. Если всё ещё не найден, пробуем общий поиск
                        if (hitmosTrack == null) {
                            ArrayList<MusicWebSearcher.WebTrack> results =
                                    MusicWebSearcher.searchTracksSync(!TextUtils.isEmpty(ti.artist) ? ti.artist + " " + ti.title : ti.title);
                            if (results != null && !results.isEmpty()) {
                                hitmosTrack = results.get(0);
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }

                    boolean success = hitmosTrack != null && !TextUtils.isEmpty(hitmosTrack.downloadUrl);

                    if (success) {
                        foundCount++;
                        // Добавляем трек в канал библиотеки как SG_STREAM
                        String durationStr = hitmosTrack.duration != null ? hitmosTrack.duration : "3:00";
                        String coverUrl = hitmosTrack.coverUrl != null ? hitmosTrack.coverUrl : "";
                        long trackHashId = Math.abs((long) hitmosTrack.downloadUrl.hashCode());

                        String streamMsg = "SG_STREAM|||" + hitmosTrack.title + "|||" + hitmosTrack.artist + "|||"
                                + hitmosTrack.downloadUrl + "|||" + durationStr + "|||" + trackHashId + "|||" + coverUrl;

                        SendMessagesHelper.getInstance(account).sendMessage(
                                SendMessagesHelper.SendMessageParams.of(
                                        streamMsg,
                                        -channelId,
                                        null, null, null,
                                        true, null, null, null, true, 0, 0, null, false
                                )
                        );

                        // Пауза 250 мс, чтобы избежать лимита Telegram на отправку сообщений
                        SystemClock.sleep(250);
                    }

                    final boolean fSuccess = success;
                    AndroidUtilities.runOnUIThread(() ->
                            listener.onTrackProgress(currentIndex, total, trackName, fSuccess)
                    );
                }

                // Обновляем счётчик треков в библиотеке
                int prevCount = MessagesController.getMainSettings(account).getInt("sg_library_track_count", 0);
                MessagesController.getMainSettings(account).edit().putInt("sg_library_track_count", prevCount + foundCount).apply();
                NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);

                final int fFound = foundCount;
                AndroidUtilities.runOnUIThread(() -> listener.onFinished(fFound, total));

            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() ->
                        listener.onError("Произошла ошибка при импорте: " + e.getMessage())
                );
            }
        }).start();
    }
}
