package soundgram.music;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SoundGramStatsManager {

    public static final long SYNC_INTERVAL_MS = 10 * 60 * 1000L; // 10 минут
    private static long lastSyncTimestamp = 0;
    private static boolean isSyncing = false;
    private static Runnable periodicSyncRunnable = null;

    public static void checkAndSync(int currentAccount, long channelId) {
        if (channelId == 0) return;
        long now = System.currentTimeMillis();
        if (now - lastSyncTimestamp >= SYNC_INTERVAL_MS) {
            forceSync(currentAccount, channelId);
        }
    }

    public static void forceSync(int currentAccount, long channelId) {
        if (channelId == 0 || isSyncing) return;
        if (!ApplicationLoader.isNetworkOnline()) return;

        isSyncing = true;
        lastSyncTimestamp = System.currentTimeMillis();

        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(channelId);
        if (chat == null) {
            isSyncing = false;
            return;
        }

        TLObject req;
        if (ChatObject.isChannel(chat)) {
            TLRPC.TL_channels_getFullChannel channelReq = new TLRPC.TL_channels_getFullChannel();
            channelReq.channel = MessagesController.getInstance(currentAccount).getInputChannel(chat);
            req = channelReq;
        } else {
            TLRPC.TL_messages_getFullChat chatReq = new TLRPC.TL_messages_getFullChat();
            chatReq.chat_id = channelId;
            req = chatReq;
        }

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (error == null && response instanceof TLRPC.TL_messages_chatFull) {
                TLRPC.TL_messages_chatFull res = (TLRPC.TL_messages_chatFull) response;
                TLRPC.ChatFull chatFull = res.full_chat;
                String currentAbout = chatFull != null && chatFull.about != null ? chatFull.about : "";

                AndroidUtilities.runOnUIThread(() -> processSync(currentAccount, channelId, chatFull, currentAbout));
            } else {
                isSyncing = false;
            }
        });
    }

    private static void processSync(int currentAccount, long channelId, TLRPC.ChatFull chatFull, String currentAbout) {
        try {
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("soundgram_stats", Context.MODE_PRIVATE);
            long localSeconds = prefs.getLong("total_seconds", 0L);
            int localTracks = prefs.getInt("total_tracks", 0);

            long remoteSeconds = -1;
            int remoteTracks = -1;

            if (!TextUtils.isEmpty(currentAbout)) {
                Pattern pattern = Pattern.compile("\\[SG_STATS:(\\d+):(\\d+)\\]");
                Matcher matcher = pattern.matcher(currentAbout);
                if (matcher.find()) {
                    try {
                        remoteSeconds = Long.parseLong(matcher.group(1));
                        remoteTracks = Integer.parseInt(matcher.group(2));
                    } catch (Exception ignored) {}
                }
            }

            if (remoteSeconds > localSeconds) {
                // Время в канале БОЛЬШЕ, чем в локальной памяти -> подгружаем из канала в память
                int finalTracks = Math.max(localTracks, remoteTracks);
                prefs.edit()
                        .putLong("total_seconds", remoteSeconds)
                        .putInt("total_tracks", finalTracks)
                        .apply();
            } else {
                // В локальной памяти время больше либо равно remote -> синхронизируем в канал
                String newAbout = formatStatsDescription(localSeconds, localTracks);
                if (!newAbout.equals(currentAbout)) {
                    MessagesController.getInstance(currentAccount).updateChatAbout(channelId, newAbout, chatFull);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isSyncing = false;
        }
    }

    public static String formatStatsDescription(long totalSeconds, int totalTracks) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder timeFormatted = new StringBuilder();
        if (days > 0) timeFormatted.append(days).append("д ");
        if (hours > 0 || days > 0) timeFormatted.append(hours).append("ч ");
        if (minutes > 0 || hours > 0 || days > 0) timeFormatted.append(minutes).append("мин ");
        timeFormatted.append(seconds).append("сек");

        return "📊 Статистика SoundGram:\n" +
               "🎵 Прослушано треков: " + totalTracks + "\n" +
               "⏱ Время прослушивания: " + timeFormatted.toString() + "\n" +
               "[SG_STATS:" + totalSeconds + ":" + totalTracks + "]";
    }

    public static void startPeriodicSync(int currentAccount, long channelId) {
        stopPeriodicSync();
        if (channelId == 0) return;

        periodicSyncRunnable = new Runnable() {
            @Override
            public void run() {
                checkAndSync(currentAccount, channelId);
                AndroidUtilities.runOnUIThread(this, SYNC_INTERVAL_MS);
            }
        };

        // Запускаем первично и ставим таймер на каждые 10 минут
        checkAndSync(currentAccount, channelId);
        AndroidUtilities.runOnUIThread(periodicSyncRunnable, SYNC_INTERVAL_MS);
    }

    public static void stopPeriodicSync() {
        if (periodicSyncRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(periodicSyncRunnable);
            periodicSyncRunnable = null;
        }
    }
}
