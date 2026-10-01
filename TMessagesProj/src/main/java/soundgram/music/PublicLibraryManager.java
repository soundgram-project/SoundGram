package soundgram.music;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_update;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PublicLibraryManager {

    private static final String PREF_KEY = "soundgram_public_library";
    private static final String KEY_PHANTOM_DOC_ID = "phantom_doc_id_";
    private static final String KEY_PHANTOM_ACCESS_HASH = "phantom_access_hash_";
    private static final String KEY_PHANTOM_USERNAME = "phantom_username_";

    // 1-second silent MP3 raw audio frame payload (22050Hz mono 32kbps)
    private static final String SILENT_AUDIO_B64 =
            "//NwwAAAAAAAAAAAAEluZm8AAAAPAAAAKQAAEXIAEBAWFhwcHCIiKCgoLi40NDQ6OkBARkZGTExSUlJY" +
            "WF5eZGhoaGxscHB2dnx8fIKChISGhoaKipCQkpaWmpqcoKCirKyytrbAwMDKytDQ0NbW2tra4ODm5ujo" +
            "6u7u8vL4+Pr6/Pz///NwwAcAAAEpAMwAADd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+" +
            "3/7c7v773P//9+7/f/723e7//9/7fv////NwwAYAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/" +
            "3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv////NwwAgAAAEpAIwAAAd/yE94f/8437f8" +
            "/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv////NwwAkAAAEpAIwA" +
            "AAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv//" +
            "//NwwAoAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/" +
            "f/723e7//9/7fv////NwwAsAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+" +
            "3/7c7v773P//9+7/f/723e7//9/7fv////NwwAwAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/" +
            "3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv////NwwA0AAAEpAIwAAAd/yE94f/8437f8" +
            "/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv////NwwA4AAAEpAIwA" +
            "AAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv//" +
            "//NwwA8AAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/" +
            "f/723e7//9/7fv////NwwBAAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+" +
            "3/7c7v773P//9+7/f/723e7//9/7fv////NwwBEAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/" +
            "3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv////NwwBIAAAEpAIwAAAd/yE94f/8437f8" +
            "/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv////NwwBMAAAEpAIwA" +
            "AAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv//" +
            "//NwwBQAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/" +
            "f/723e7//9/7fv////NwwBUAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+" +
            "3/7c7v773P//9+7/f/723e7//9/7fv////NwwBYAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/" +
            "3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv////NwwBcAAAEpAIwAAAd/yE94f/8437f8" +
            "/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv////NwwBgAAAEpAIwA" +
            "AAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv//" +
            "//NwwBkAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/" +
            "f/723e7//9/7fv////NwwBoAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+" +
            "3/7c7v773P//9+7/f/723e7//9/7fv////NwwBsAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/" +
            "3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv////NwwBwAAAEpAIwAAAd/yE94f/8437f8" +
            "/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv////NwwB0AAAEpAIwA" +
            "AAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv//" +
            "//NwwB4AAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/" +
            "f/723e7//9/7fv////NwwB8AAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+" +
            "3/7c7v773P//9+7/f/723e7//9/7fv////NwwCAAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/" +
            "3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv////NwwCEAAAEpAIwAAAd/yE94f/8437f8" +
            "/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv////NwwCIAAAEpAIwA" +
            "AAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv//" +
            "//NwwCMAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/" +
            "f/723e7//9/7fv////NwwCQAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+" +
            "3/7c7v773P//9+7/f/723e7//9/7fv////NwwCUAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/" +
            "3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv////NwwCYAAAEpAIwAAAd/yE94f/8437f8" +
            "/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv////NwwCcAAAEpAIwA" +
            "AAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv//" +
            "//NwwCgAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/" +
            "f/723e7//9/7fv////NwwCkAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+" +
            "3/7c7v773P//9+7/f/723e7//9/7fv////NwwCoAAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/" +
            "3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv////NwwCsAAAEpAIwAAAd/yE94f/8437f8" +
            "/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv////NwwCwAAAEpAIwA" +
            "AAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/f/723e7//9/7fv//" +
            "//NwwC0AAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+3/7c7v773P//9+7/" +
            "f/723e7//9/7fv////NwwC4AAAEpAIwAAAd/yE94f/8437f8/xOAAoAAeQn//7D/3jP//4y7F43/843+" +
            "3/7c7v773P//9+7/f/723e7//9/7fv///";

    public static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
    }

    public static boolean isLibraryPublic(int account) {
        long userId = UserConfig.getInstance(account).getClientUserId();
        String key = userId != 0 ? ("library_public_" + userId) : ("library_public_acc" + account);
        return getPrefs().getBoolean(key, false);
    }

    public static void setLibraryPublic(int account, boolean value) {
        long userId = UserConfig.getInstance(account).getClientUserId();
        String key = userId != 0 ? ("library_public_" + userId) : ("library_public_acc" + account);
        getPrefs().edit().putBoolean(key, value).apply();
    }

    /**
     * Extracts channel username referenced in profile music document.
     */
    public static String extractLibraryUsername(TLRPC.Document document) {
        if (document == null) return null;
        String title = MessageObject.getMusicTitle(document, false);
        if (title != null) {
            Matcher m = Pattern.compile("@([a-zA-Z0-9_]{4,})").matcher(title);
            if (m.find()) return m.group(1);
        }
        String author = MessageObject.getMusicAuthor(document, false);
        if (author != null) {
            Matcher m = Pattern.compile("@([a-zA-Z0-9_]{4,})").matcher(author);
            if (m.find()) return m.group(1);
        }
        return null;
    }

    /**
     * Checks if a channel's description contains SoundGram statistics.
     */
    public static boolean isLibraryChannel(TLRPC.ChatFull chatFull) {
        if (chatFull == null || TextUtils.isEmpty(chatFull.about)) return false;
        return chatFull.about.contains("[SG_STATS:");
    }

    public static class ChannelMusicStats {
        public long totalSeconds;
        public long trackCount;

        public ChannelMusicStats(long totalSeconds, long trackCount) {
            this.totalSeconds = totalSeconds;
            this.trackCount = trackCount;
        }

        public String formatStats(String listeningTimePrefix) {
            long hours = totalSeconds / 3600;
            long mins = (totalSeconds % 3600) / 60;
            StringBuilder sb = new StringBuilder();
            sb.append(trackCount).append(" ").append(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.trck));
            if (hours > 0) {
                sb.append(" • ").append(hours).append(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.hour)).append(" ").append(mins).append(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.min));
            } else if (mins > 0) {
                sb.append(" • ").append(mins).append(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.min));
            }
            return sb.toString();
        }
    }

    /**
     * Parses tracks and seconds from chatFull description if present.
     */
    public static ChannelMusicStats parseStatsFromDescription(String about) {
        if (TextUtils.isEmpty(about)) return null;
        Pattern pattern = Pattern.compile("\\[SG_STATS:(\\d+):(\\d+)\\]");
        Matcher matcher = pattern.matcher(about);
        if (matcher.find()) {
            try {
                long seconds = Long.parseLong(matcher.group(1));
                long tracks = Long.parseLong(matcher.group(2));
                return new ChannelMusicStats(seconds, tracks);
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static ChannelMusicStats parseStatsFromDescription(TLRPC.ChatFull chatFull) {
        return parseStatsFromDescription(chatFull != null ? chatFull.about : null);
    }

    /**
     * Generates a 1-second silent MP3 with ID3v2 tags (TIT2 title, TPE1 artist).
     */
    public static boolean createSilentMp3WithTitle(File destFile, String title, String performer) {
        try {
            byte[] audioBytes = Base64.decode(SILENT_AUDIO_B64, Base64.DEFAULT);

            ByteArrayOutputStream id3Frames = new ByteArrayOutputStream();
            writeId3Frame(id3Frames, "TIT2", title != null ? title : "");
            writeId3Frame(id3Frames, "TPE1", performer != null ? performer : "SoundGram");

            byte[] framesBytes = id3Frames.toByteArray();
            int tagSize = framesBytes.length;

            ByteArrayOutputStream header = new ByteArrayOutputStream();
            header.write("ID3".getBytes(StandardCharsets.US_ASCII));
            header.write(3); // v2.3
            header.write(0);
            header.write(0); // flags

            // 4 bytes syncsafe size
            header.write((tagSize >> 21) & 0x7F);
            header.write((tagSize >> 14) & 0x7F);
            header.write((tagSize >> 7) & 0x7F);
            header.write(tagSize & 0x7F);

            if (destFile.getParentFile() != null && !destFile.getParentFile().exists()) {
                destFile.getParentFile().mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                fos.write(header.toByteArray());
                fos.write(framesBytes);
                fos.write(audioBytes);
                fos.flush();
            }
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    private static void writeId3Frame(ByteArrayOutputStream out, String frameId, String text) throws Exception {
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[1 + textBytes.length];
        payload[0] = 3; // UTF-8 encoding
        System.arraycopy(textBytes, 0, payload, 1, textBytes.length);

        out.write(frameId.getBytes(StandardCharsets.US_ASCII));
        int size = payload.length;
        out.write((size >> 24) & 0xFF);
        out.write((size >> 16) & 0xFF);
        out.write((size >> 8) & 0xFF);
        out.write(size & 0xFF);
        out.write(0);
        out.write(0); // flags
        out.write(payload);
    }

    /**
     * Creates and uploads the phantom track into the user's storage channel,
     * then pins it as the user's profile music.
     */
    public static void setupPhantomTrack(int currentAccount, long channelId, String username, Utilities.Callback<Boolean> callback) {
        long userId = UserConfig.getInstance(currentAccount).getClientUserId();
        String trackTitle = "@" + username;

        // Check if we already have a cached phantom track for this username
        long cachedDocId = getPrefs().getLong(KEY_PHANTOM_DOC_ID + userId, 0);
        long cachedHash = getPrefs().getLong(KEY_PHANTOM_ACCESS_HASH + userId, 0);
        String cachedUser = getPrefs().getString(KEY_PHANTOM_USERNAME + userId, "");

        if (cachedDocId != 0 && trackTitle.equals(cachedUser)) {
            TLRPC.TL_document doc = new TLRPC.TL_document();
            doc.id = cachedDocId;
            doc.access_hash = cachedHash;
            doc.file_reference = new byte[0];
            saveDocumentToProfile(currentAccount, doc, true, callback);
            return;
        }

        File cacheDir = ApplicationLoader.applicationContext.getCacheDir();
        File phantomFile = new File(cacheDir, "sg_phantom_" + userId + ".mp3");
        createSilentMp3WithTitle(phantomFile, trackTitle, "SoundGram");

        AccountInstance accountInstance = AccountInstance.getInstance(currentAccount);

        NotificationCenter.NotificationCenterDelegate delegate = new NotificationCenter.NotificationCenterDelegate() {
            @Override
            public void didReceivedNotification(int id, int account, Object... args) {
                if (id == NotificationCenter.didReceiveNewMessages && account == currentAccount) {
                    long dialogId = (Long) args[0];
                    if (dialogId == -channelId) {
                        ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[1];
                        if (arr != null) {
                            for (MessageObject mo : arr) {
                                if (mo != null && mo.isMusic() && trackTitle.equals(mo.getMusicTitle(false))) {
                                    NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.didReceiveNewMessages);
                                    TLRPC.Document doc = mo.getDocument();
                                    if (doc != null) {
                                        getPrefs().edit()
                                                .putLong(KEY_PHANTOM_DOC_ID + userId, doc.id)
                                                .putLong(KEY_PHANTOM_ACCESS_HASH + userId, doc.access_hash)
                                                .putString(KEY_PHANTOM_USERNAME + userId, trackTitle)
                                                .apply();
                                        saveDocumentToProfile(currentAccount, doc, true, callback);
                                    }
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        };

        NotificationCenter.getInstance(currentAccount).addObserver(delegate, NotificationCenter.didReceiveNewMessages);

        // Send silent phantom track to channel
        SendMessagesHelper.prepareSendingDocument(
                accountInstance,
                phantomFile.getAbsolutePath(),
                phantomFile.getAbsolutePath(),
                null,
                "",
                "audio/mpeg",
                -channelId,
                null,
                null,
                null,
                null,
                null,
                false,
                0,
                null,
                null,
                false
        );
    }

    /**
     * Unpins the phantom music track from user profile.
     */
    public static void unsavePhantomTrack(int currentAccount) {
        long userId = UserConfig.getInstance(currentAccount).getClientUserId();
        long cachedDocId = getPrefs().getLong(KEY_PHANTOM_DOC_ID + userId, 0);
        long cachedHash = getPrefs().getLong(KEY_PHANTOM_ACCESS_HASH + userId, 0);

        TLRPC.UserFull userFull = MessagesController.getInstance(currentAccount).getUserFull(userId);
        TLRPC.Document targetDoc = null;
        if (userFull != null && userFull.saved_music != null) {
            targetDoc = userFull.saved_music;
        } else if (cachedDocId != 0) {
            targetDoc = new TLRPC.TL_document();
            targetDoc.id = cachedDocId;
            targetDoc.access_hash = cachedHash;
            targetDoc.file_reference = new byte[0];
        }

        if (targetDoc != null) {
            saveDocumentToProfile(currentAccount, targetDoc, false, null);
        }
    }

    public static void saveDocumentToProfile(int currentAccount, TLRPC.Document document, boolean save, Utilities.Callback<Boolean> callback) {
        if (document == null) {
            if (callback != null) callback.run(false);
            return;
        }
        TLRPC.TL_account_saveMusic req = new TLRPC.TL_account_saveMusic();
        req.unsave = !save;
        req.id = new TLRPC.TL_inputDocument();
        req.id.id = document.id;
        req.id.access_hash = document.access_hash;
        req.id.file_reference = document.file_reference;
        if (req.id.file_reference == null) {
            req.id.file_reference = new byte[0];
        }

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> {
            boolean success = res instanceof TLRPC.TL_boolTrue;
            AndroidUtilities.runOnUIThread(() -> {
                long clientUserId = UserConfig.getInstance(currentAccount).getClientUserId();
                TLRPC.UserFull userFull = MessagesController.getInstance(currentAccount).getUserFull(clientUserId);
                if (userFull != null) {
                    if (save) {
                        userFull.flags2 |= TLObject.FLAG_21;
                        userFull.saved_music = document;
                    } else {
                        userFull.flags2 &= ~TLObject.FLAG_21;
                        userFull.saved_music = null;
                    }
                    MessagesStorage.getInstance(currentAccount).updateUserInfo(userFull, false);
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.profileMusicUpdated, clientUserId);
                }
                if (callback != null) callback.run(success);
            });
        });
    }

    /**
     * Clones a complete playlist from a foreign channel into the user's personal music channel.
     * All tracks are forwarded into the user's storage channel, generating new message IDs,
     * maintaining the exact original order, and creating/syncing a matching playlist file.
     */
    public static void clonePlaylistToMyLibrary(int currentAccount, long foreignChannelId, String playlistTitle, ArrayList<MessageObject> playlistMessages, Runnable onDone, Utilities.Callback<String> onError) {
        long myChannelId = ChannelMusicFragment.getChannelIdFromFile(currentAccount);
        if (myChannelId == 0) {
            if (onError != null) onError.run("No target music channel");
            return;
        }

        TLRPC.InputPeer toPeer = MessagesController.getInstance(currentAccount).getInputPeer(-myChannelId);
        TLRPC.InputPeer fromPeer = MessagesController.getInstance(currentAccount).getInputPeer(-foreignChannelId);
        if (toPeer == null || fromPeer == null) {
            if (onError != null) onError.run("Invalid channel peer");
            return;
        }

        ArrayList<MessageObject> channelTracks = new ArrayList<>();
        for (MessageObject msg : playlistMessages) {
            if (msg.messageOwner != null && msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) {
                // Stream track
            } else if (msg.getId() > 0) {
                channelTracks.add(msg);
            }
        }

        if (channelTracks.isEmpty()) {
            ArrayList<String> lines = new ArrayList<>();
            for (MessageObject msg : playlistMessages) {
                if (msg.messageOwner != null && msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) {
                    lines.add(msg.messageOwner.message);
                }
            }
            saveAndSyncClonedPlaylist(currentAccount, myChannelId, playlistTitle, lines);
            if (onDone != null) onDone.run();
            return;
        }

        TLRPC.TL_messages_forwardMessages req = new TLRPC.TL_messages_forwardMessages();
        req.from_peer = fromPeer;
        req.to_peer = toPeer;
        req.drop_author = true;
        for (MessageObject msg : channelTracks) {
            req.id.add(msg.getId());
            req.random_id.add(Utilities.random.nextLong());
        }

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (error != null || !(response instanceof TLRPC.Updates)) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (onError != null) onError.run(error != null ? error.text : "Forward failed");
                });
                return;
            }

            TLRPC.Updates updates = (TLRPC.Updates) response;
            ArrayList<Integer> newIds = new ArrayList<>();
            for (TLRPC.Update u : updates.updates) {
                if (u instanceof TL_update.TL_updateNewChannelMessage) {
                    newIds.add(((TL_update.TL_updateNewChannelMessage) u).message.id);
                } else if (u instanceof TL_update.TL_updateNewMessage) {
                    newIds.add(((TL_update.TL_updateNewMessage) u).message.id);
                }
            }

            ArrayList<String> newTrackLines = new ArrayList<>();
            int channelTrackIdx = 0;
            for (MessageObject msg : playlistMessages) {
                if (msg.messageOwner != null && msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) {
                    newTrackLines.add(msg.messageOwner.message);
                } else if (channelTrackIdx < newIds.size()) {
                    newTrackLines.add(String.valueOf(newIds.get(channelTrackIdx++)));
                }
            }

            AndroidUtilities.runOnUIThread(() -> {
                saveAndSyncClonedPlaylist(currentAccount, myChannelId, playlistTitle, newTrackLines);
                if (onDone != null) onDone.run();
            });
        });
    }

    private static void saveAndSyncClonedPlaylist(int currentAccount, long myChannelId, String playlistTitle, ArrayList<String> newTrackLines) {
        File dir = PlaylistSyncManager.getPlaylistsDir(currentAccount);
        if (dir == null) return;
        String safeTitle = playlistTitle != null && !playlistTitle.isEmpty() ? playlistTitle : "Playlist";
        File pFile = new File(dir, safeTitle + ".txt");
        int counter = 1;
        while (pFile.exists()) {
            PlaylistSyncManager.PlaylistData existing = PlaylistSyncManager.readPlaylistFile(pFile);
            if (existing.isDeleted) break;
            pFile = new File(dir, safeTitle + " (" + counter++ + ").txt");
        }

        PlaylistSyncManager.PlaylistData pData = new PlaylistSyncManager.PlaylistData();
        pData.title = pFile.getName().replace(".txt", "");
        pData.trackLines.addAll(newTrackLines);
        PlaylistSyncManager.writePlaylistFile(pFile, pData);
        PlaylistSyncManager.syncPlaylistToChannel(currentAccount, myChannelId, pFile);
    }

    /**
     * Forwards an individual track into the user's personal storage channel.
     */
    public static void copyTrackToMyLibrary(int currentAccount, long foreignChannelId, MessageObject msg, Runnable onDone) {
        long myChannelId = ChannelMusicFragment.getChannelIdFromFile(currentAccount);
        if (myChannelId == 0) return;
        if (msg.messageOwner != null && msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) {
            SendMessagesHelper.getInstance(currentAccount).sendMessage(
                    SendMessagesHelper.SendMessageParams.of(msg.messageOwner.message, -myChannelId, null, null, null, false, null, null, null, true, 0, 0, null, false)
            );
        } else {
            ArrayList<MessageObject> arr = new ArrayList<>();
            arr.add(msg);
            SendMessagesHelper.getInstance(currentAccount).sendMessage(arr, -myChannelId, false, false, true, 0, 0);
        }
        if (onDone != null) onDone.run();
    }
}
