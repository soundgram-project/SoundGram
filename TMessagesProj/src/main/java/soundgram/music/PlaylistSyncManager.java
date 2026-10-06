package soundgram.music;

import android.text.TextUtils;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class PlaylistSyncManager {

    public static class PlaylistData {
        public String title = "";
        public int iconId = 0;
        public int msgId = 0;
        public boolean pendingSync = false;
        public boolean isDeleted = false;
        public MessageObject playlistMsg;
        public ArrayList<String> trackLines = new ArrayList<>();
    }

    public static File getPlaylistsDir(int account) {
        long userId = ChannelMusicFragment.getValidUserId(account);
        File baseDir = ApplicationLoader.applicationContext.getExternalFilesDir(null);
        if (baseDir == null) return null;
        File userDir = userId != 0 ? new File(baseDir, "user_" + userId) : new File(baseDir, "account_" + account);
        File dir = new File(userDir, "Playlists");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getPlaylistsDir() {
        return getPlaylistsDir(UserConfig.selectedAccount);
    }

    /**
     * Checks if a channel message text is a playlist message.
     * Playlist messages have the format:
     *   Title (or SG_PLAYLIST|||Title)
     *   icon=N (optional)
     *   SG_STREAM|||... or msgId lines
     */
    public static boolean isPlaylistMessage(String text) {
        if (text == null) return false;
        // Exclude standalone track, album, stats messages
        if (text.startsWith("SG_STREAM|||")) return false;
        if (text.startsWith("SG_ALBUM|||")) return false;
        if (text.startsWith("SG_STATS|||")) return false;

        // Explicit playlist markers
        if (text.startsWith("SG_PLAYLIST|||")) return true;
        if (text.contains("icon=") || text.contains("\nicon=")) return true;
        if (text.contains("MSG_ID=") || text.contains("\nMSG_ID=")) return true;

        // Multi-line message containing SG_STREAM||| (playlist container)
        if (text.contains("\nSG_STREAM|||")) return true;

        return false;
    }

    /**
     * Reads a playlist .txt file and parses metadata and track lines.
     */
    public static PlaylistData readPlaylistFile(File file) {
        PlaylistData data = new PlaylistData();
        if (file == null || !file.exists()) return data;

        String fileName = file.getName().replace(".txt", "");
        data.title = fileName;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if ("DELETED".equals(line)) {
                    data.isDeleted = true;
                    continue;
                }
                if (line.startsWith("icon=")) {
                    try {
                        data.iconId = Integer.parseInt(line.substring(5).trim());
                    } catch (Exception ignored) {}
                    continue;
                }
                if (line.startsWith("MSG_ID=")) {
                    try {
                        data.msgId = Integer.parseInt(line.substring(7).trim());
                    } catch (Exception ignored) {}
                    continue;
                }
                if (line.startsWith("STATUS=PENDING_SYNC")) {
                    data.pendingSync = true;
                    continue;
                }

                if (firstLine && !line.startsWith("SG_STREAM|||") && !isInteger(line)) {
                    data.title = line;
                    firstLine = false;
                    continue;
                }
                firstLine = false;
                data.trackLines.add(line);
            }
        } catch (IOException e) {
            FileLog.e(e);
        }
        return data;
    }

    private static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Writes local playlist file.
     */
    public static void writePlaylistFile(File file, PlaylistData data) {
        if (file == null) return;
        if (data.isDeleted) {
            try (FileWriter fw = new FileWriter(file, false)) {
                fw.write("DELETED\n");
                if (data.msgId > 0) {
                    fw.write("MSG_ID=" + data.msgId + "\n");
                }
                if (data.iconId > 0) {
                    fw.write("icon=" + data.iconId + "\n");
                }
            } catch (IOException e) {
                FileLog.e(e);
            }
            return;
        }

        try (FileWriter fw = new FileWriter(file, false)) {
            fw.write(data.title + "\n");
            fw.write("icon=" + data.iconId + "\n");
            if (data.msgId > 0) {
                fw.write("MSG_ID=" + data.msgId + "\n");
            }
            if (data.pendingSync) {
                fw.write("STATUS=PENDING_SYNC\n");
            }
            for (String track : data.trackLines) {
                fw.write(track + "\n");
            }
        } catch (IOException e) {
            FileLog.e(e);
        }
    }

    /**
     * Syncs a local playlist to the channel.
     */
    public static void syncPlaylistToChannel(int currentAccount, long channelId, File playlistFile) {
        if (playlistFile == null || channelId == 0) return;
        PlaylistData data = readPlaylistFile(playlistFile);

        if (data.isDeleted) {
            deletePlaylistFromChannel(currentAccount, channelId, playlistFile, data.msgId, data.iconId);
            return;
        }

        if (!ApplicationLoader.isNetworkOnline()) {
            data.pendingSync = true;
            writePlaylistFile(playlistFile, data);
            return;
        }

        String playlistName = playlistFile.getName().replace(".txt", "");
        File coverFile = new File(getPlaylistsDir(currentAccount), playlistName + "_cover.jpg");

        StringBuilder sb = new StringBuilder();
        sb.append(data.title).append("\n");
        sb.append("icon=").append(data.iconId).append("\n");
        boolean modified = false;
        ArrayList<String> sanitizedTrackLines = new ArrayList<>();
        for (String track : data.trackLines) {
            String trimmed = track != null ? track.trim() : "";
            if (isInteger(trimmed) && Integer.parseInt(trimmed) > 0) {
                sb.append(trimmed).append("\n");
                sanitizedTrackLines.add(trimmed);
            } else if (trimmed.startsWith("SG_STREAM|||")) {
                int resolvedId = resolveTrackIdFromChannel(currentAccount, trimmed);
                if (resolvedId > 0) {
                    sb.append(resolvedId).append("\n");
                    sanitizedTrackLines.add(String.valueOf(resolvedId));
                    modified = true;
                }
            }
        }
        if (modified || sanitizedTrackLines.size() != data.trackLines.size()) {
            data.trackLines.clear();
            data.trackLines.addAll(sanitizedTrackLines);
            writePlaylistFile(playlistFile, data);
        }

        String text = sb.toString().trim();
        int oldMsgId = data.msgId;

        if (oldMsgId > 0) {
            MessagesController.getInstance(currentAccount).deleteMessages(
                    new ArrayList<>(Arrays.asList(oldMsgId)), null, null, -channelId, 0, true, 0
            );
        }

        if (coverFile.exists()) {
            if (text.length() <= 1000) {
                SendMessagesHelper.prepareSendingPhoto(
                        AccountInstance.getInstance(currentAccount),
                        coverFile.getAbsolutePath(),
                        null, null,
                        -channelId,
                        null, null, null, null, null, null, null, 0, null, null,
                        true, 0, 0, false, text, null, 0, 0
                );
            } else {
                String caption = data.title + "\nicon=" + data.iconId;
                SendMessagesHelper.prepareSendingPhoto(
                        AccountInstance.getInstance(currentAccount),
                        coverFile.getAbsolutePath(),
                        null, null,
                        -channelId,
                        null, null, null, null, null, null, null, 0, null, null,
                        true, 0, 0, false, caption, null, 0, 0
                );
                SendMessagesHelper.getInstance(currentAccount).sendMessage(
                        SendMessagesHelper.SendMessageParams.of(text, -channelId, null, null, null, true, null, null, null, true, 0, 0, null, false)
                );
            }
        } else {
            SendMessagesHelper.getInstance(currentAccount).sendMessage(
                    SendMessagesHelper.SendMessageParams.of(text, -channelId, null, null, null, true, null, null, null, true, 0, 0, null, false)
            );
        }

        data.pendingSync = false;
        writePlaylistFile(playlistFile, data);
    }

    public static int resolveTrackIdFromChannel(int account, String streamLine) {
        if (TextUtils.isEmpty(streamLine)) return 0;
        String[] parts = streamLine.split("\\|\\|\\|");
        String title = parts.length > 1 ? parts[1].toLowerCase().trim() : "";
        String artist = parts.length > 2 ? parts[2].toLowerCase().trim() : "";
        ArrayList<org.telegram.messenger.MessageObject> audioMsgs = ChannelMusicFragment.getAudioMessages(account);
        for (org.telegram.messenger.MessageObject mo : audioMsgs) {
            if (mo.getId() > 0) {
                String moTitle = mo.getMusicTitle() != null ? mo.getMusicTitle().toLowerCase().trim() : "";
                String moArtist = mo.getMusicAuthor() != null ? mo.getMusicAuthor().toLowerCase().trim() : "";
                if (!title.isEmpty() && moTitle.equals(title)) {
                    if (artist.isEmpty() || moArtist.equals(artist) || moArtist.contains(artist) || artist.contains(moArtist)) {
                        return mo.getId();
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Deletes a playlist both locally and from the Telegram channel in 1 step.
     */
    public static void deletePlaylistFromChannel(int currentAccount, long channelId, File playlistFile, int msgId, int iconId) {
        if (playlistFile != null && playlistFile.exists()) {
            PlaylistData data = readPlaylistFile(playlistFile);
            data.isDeleted = true;
            if (msgId > 0) data.msgId = msgId;
            if (iconId > 0) data.iconId = iconId;
            writePlaylistFile(playlistFile, data);

            String playlistName = playlistFile.getName().replace(".txt", "");
            File coverFile = new File(getPlaylistsDir(currentAccount), playlistName + "_cover.jpg");
            if (coverFile.exists()) {
                coverFile.delete();
            }
        }

        int targetMsgId = msgId;
        if (targetMsgId <= 0 && playlistFile != null && playlistFile.exists()) {
            PlaylistData data = readPlaylistFile(playlistFile);
            targetMsgId = data.msgId;
        }

        if (targetMsgId > 0 && channelId != 0 && ApplicationLoader.isNetworkOnline()) {
            ArrayList<Integer> toDelete = new ArrayList<>();
            toDelete.add(targetMsgId);
            if (iconId > 0) {
                toDelete.add(iconId);
            }
            MessagesController.getInstance(currentAccount).deleteMessages(
                    toDelete, null, null, -channelId, 0, true, 0
            );
        }
    }

    public static void updatePlaylistMsgId(int currentAccount, String playlistTitle, int msgId) {
        if (playlistTitle == null || msgId == 0) return;
        File dir = getPlaylistsDir(currentAccount);
        if (dir == null) return;
        File localFile = new File(dir, playlistTitle + ".txt");
        if (localFile.exists()) {
            PlaylistData data = readPlaylistFile(localFile);
            if (data.msgId != msgId) {
                data.msgId = msgId;
                data.pendingSync = false;
                writePlaylistFile(localFile, data);
            }
        }
    }

    /**
     * Parses a channel message text into PlaylistData.
     */
    public static PlaylistData parseChannelPlaylistMessage(String text, int msgId) {
        if (text == null) return null;

        String[] lines = text.split("\n");
        if (lines.length == 0) return null;

        String rawTitle = lines[0].trim();
        if (rawTitle.startsWith("SG_PLAYLIST|||")) {
            rawTitle = rawTitle.substring("SG_PLAYLIST|||".length()).trim();
        }

        boolean firstLineIsIcon = rawTitle.startsWith("icon=");
        if (firstLineIsIcon) {
            rawTitle = "Плейлист";
        }
        if (TextUtils.isEmpty(rawTitle)) return null;

        PlaylistData data = new PlaylistData();
        data.title = rawTitle;
        data.msgId = msgId;

        int startIndex = firstLineIsIcon ? 0 : 1;
        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("icon=")) {
                try {
                    data.iconId = Integer.parseInt(line.substring(5).trim());
                } catch (Exception ignored) {}
                continue;
            }
            if (line.startsWith("MSG_ID=") || line.startsWith("STATUS=") || line.startsWith("SG_PLAYLIST|||")) {
                continue;
            }

            data.trackLines.add(line);
        }

        return data;
    }

    /**
     * Called when channel messages are loaded from cloud.
     */
    public static void syncOnChannelLoaded(int currentAccount, long channelId, ArrayList<MessageObject> loadedPlaylistMessages) {
        File dir = getPlaylistsDir(currentAccount);
        if (dir == null) return;

        File[] localFiles = dir.listFiles((d, name) -> name.endsWith(".txt"));
        HashSet<String> processedLocalNames = new HashSet<>();

        LinkedHashMap<String, PlaylistData> channelPlaylists = new LinkedHashMap<>();
        LinkedHashMap<String, MessageObject> channelPlaylistMsgs = new LinkedHashMap<>();
        ArrayList<Integer> duplicateMsgIdsToDelete = new ArrayList<>();

        if (loadedPlaylistMessages != null) {
            for (MessageObject msg : loadedPlaylistMessages) {
                if (msg == null || msg.messageOwner == null) continue;
                String text = msg.messageOwner.message;
                if (!isPlaylistMessage(text)) continue;

                PlaylistData parsed = parseChannelPlaylistMessage(text, msg.getId());
                if (parsed == null) continue;

                String key = parsed.title.toLowerCase().trim();

                if (channelPlaylists.containsKey(key)) {
                    duplicateMsgIdsToDelete.add(msg.getId());
                } else {
                    channelPlaylists.put(key, parsed);
                    channelPlaylistMsgs.put(key, msg);
                }
            }
        }

        for (Map.Entry<String, PlaylistData> entry : channelPlaylists.entrySet()) {
            PlaylistData channelData = entry.getValue();
            MessageObject channelMsg = channelPlaylistMsgs.get(entry.getKey());
            String safeFileName = channelData.title.replaceAll("[/\\\\:*?\"<>|]", "_");
            File localFile = new File(dir, safeFileName + ".txt");
            processedLocalNames.add(localFile.getName());

            File localCover = new File(dir, safeFileName + "_cover.jpg");
            if (!localCover.exists() && channelMsg != null && channelMsg.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto) {
                restoreCoverFromMessage(currentAccount, channelMsg, localCover);
            }

            if (localFile.exists()) {
                PlaylistData localData = readPlaylistFile(localFile);
                if (localData.isDeleted) {
                    deletePlaylistFromChannel(currentAccount, channelId, localFile, channelData.msgId, channelData.iconId);
                } else if (localData.pendingSync) {
                    syncPlaylistToChannel(currentAccount, channelId, localFile);
                } else {
                    if (localData.msgId != channelData.msgId) {
                        localData.msgId = channelData.msgId;
                        writePlaylistFile(localFile, localData);
                    }
                }
            } else {
                // LOCAL FILE MISSING — RESTORE FROM CHANNEL
                PlaylistData restoredData = new PlaylistData();
                restoredData.title = channelData.title;
                restoredData.iconId = channelData.iconId;
                restoredData.msgId = channelData.msgId;
                restoredData.pendingSync = false;
                restoredData.isDeleted = false;
                restoredData.trackLines.addAll(channelData.trackLines);

                writePlaylistFile(localFile, restoredData);
                FileLog.d("PlaylistSyncManager: restored playlist '" + channelData.title + "' with " + channelData.trackLines.size() + " tracks from channel");
            }
        }

        if (!duplicateMsgIdsToDelete.isEmpty() && channelId != 0) {
            MessagesController.getInstance(currentAccount).deleteMessages(
                    duplicateMsgIdsToDelete, null, null, -channelId, 0, true, 0
            );
        }

        if (localFiles != null) {
            for (File f : localFiles) {
                if (processedLocalNames.contains(f.getName())) continue;
                PlaylistData localData = readPlaylistFile(f);
                if (localData.isDeleted) {
                    if (f.exists()) f.delete();
                } else if (localData.pendingSync) {
                    syncPlaylistToChannel(currentAccount, channelId, f);
                }
            }
        }
    }

    private static void restoreCoverFromMessage(int currentAccount, MessageObject msg, File localCover) {
        try {
            TLRPC.TL_messageMediaPhoto mediaPhoto = (TLRPC.TL_messageMediaPhoto) msg.messageOwner.media;
            if (mediaPhoto.photo == null) return;

            TLRPC.PhotoSize closest = FileLoader.getClosestPhotoSizeWithSize(mediaPhoto.photo.sizes, 600);
            if (closest == null) return;

            File photoPath = FileLoader.getInstance(currentAccount).getPathToAttach(closest, true);
            if (photoPath != null && photoPath.exists()) {
                try {
                    AndroidUtilities.copyFile(photoPath, localCover);
                    FileLog.d("PlaylistSyncManager: restored cover to " + localCover.getName());
                } catch (Exception e) {
                    FileLog.e("PlaylistSyncManager: failed to copy cover", e);
                }
            } else {
                try {
                    ImageLocation imageLocation = ImageLocation.getForPhoto(closest, mediaPhoto.photo);
                    if (imageLocation != null) {
                        FileLoader.getInstance(currentAccount).loadFile(imageLocation, msg, null, 0, 1);
                    }
                } catch (Exception e) {
                    FileLog.e("PlaylistSyncManager: failed to initiate cover download", e);
                }
            }
        } catch (Exception e) {
            FileLog.e("PlaylistSyncManager: restoreCoverFromMessage error", e);
        }
    }
}
