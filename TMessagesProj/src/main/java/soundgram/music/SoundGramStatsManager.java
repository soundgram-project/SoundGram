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

    public static SharedPreferences getStatsPrefs(int account) {
        long userId = UserConfig.getInstance(account).getClientUserId();
        String prefName = userId != 0 ? ("soundgram_stats_" + userId) : ("soundgram_stats_acc" + account);
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        if (account == 0 && !prefs.contains("total_seconds") && !prefs.contains("total_tracks")) {
            SharedPreferences legacy = ApplicationLoader.applicationContext.getSharedPreferences("soundgram_stats", Context.MODE_PRIVATE);
            long legSec = legacy.getLong("total_seconds", 0L);
            int legTr = legacy.getInt("total_tracks", 0);
            if (legSec > 0 || legTr > 0) {
                prefs.edit().putLong("total_seconds", legSec).putInt("total_tracks", legTr).apply();
            }
        }
        return prefs;
    }

    public static void recordPlaybackSecond(int account) {
        SharedPreferences prefs = getStatsPrefs(account);
        long s = prefs.getLong("total_seconds", 0L) + 1;
        prefs.edit().putLong("total_seconds", s).apply();
    }

    public static void recordTrackCompleted(int account) {
        SharedPreferences prefs = getStatsPrefs(account);
        int t = prefs.getInt("total_tracks", 0) + 1;
        prefs.edit().putInt("total_tracks", t).apply();

        long channelId = ChannelMusicFragment.getChannelIdFromFile(account);
        if (channelId != 0) {
            checkAndSync(account, channelId);
        }
    }

    public static long normalizeChannelId(long channelId) {
        if (channelId < -1000000000000L) {
            return -channelId - 1000000000000L;
        } else if (channelId > 1000000000000L) {
            return channelId - 1000000000000L;
        } else if (channelId < 0) {
            return -channelId;
        }
        return channelId;
    }

    public static void checkAndSync(int currentAccount, long channelId) {
        channelId = normalizeChannelId(channelId);
        if (channelId == 0) return;
        long now = System.currentTimeMillis();
        if (now - lastSyncTimestamp >= SYNC_INTERVAL_MS) {
            forceSync(currentAccount, channelId);
        }
    }

    public static void forceSync(int currentAccount, long channelId) {
        channelId = normalizeChannelId(channelId);
        if (channelId == 0 || isSyncing) return;
        if (!ApplicationLoader.isNetworkOnline()) return;

        isSyncing = true;
        lastSyncTimestamp = System.currentTimeMillis();
        final long normalizedId = channelId;

        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(normalizedId);
        if (chat == null) {
            chat = org.telegram.messenger.MessagesStorage.getInstance(currentAccount).getChatSync(normalizedId);
            if (chat != null) {
                MessagesController.getInstance(currentAccount).putChat(chat, true);
            }
        }
        if (chat == null) {
            long accessHash = ChannelMusicFragment.getChannelHashFromFile(currentAccount);
            TLRPC.TL_channel newChannel = new TLRPC.TL_channel();
            newChannel.id = normalizedId;
            newChannel.access_hash = accessHash;
            newChannel.broadcast = true;
            chat = newChannel;
            MessagesController.getInstance(currentAccount).putChat(chat, true);
        }

        TLObject req;
        if (ChatObject.isChannel(chat)) {
            TLRPC.TL_channels_getFullChannel channelReq = new TLRPC.TL_channels_getFullChannel();
            channelReq.channel = MessagesController.getInstance(currentAccount).getInputChannel(chat);
            req = channelReq;
        } else {
            TLRPC.TL_messages_getFullChat chatReq = new TLRPC.TL_messages_getFullChat();
            chatReq.chat_id = normalizedId;
            req = chatReq;
        }

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (error == null && response instanceof TLRPC.TL_messages_chatFull) {
                TLRPC.TL_messages_chatFull res = (TLRPC.TL_messages_chatFull) response;
                TLRPC.ChatFull chatFull = res.full_chat;
                String currentAbout = chatFull != null && chatFull.about != null ? chatFull.about : "";

                AndroidUtilities.runOnUIThread(() -> processSync(currentAccount, normalizedId, chatFull, currentAbout));
            } else {
                isSyncing = false;
            }
        });
    }

    private static void processSync(int currentAccount, long channelId, TLRPC.ChatFull chatFull, String currentAbout) {
        try {
            SharedPreferences prefs = getStatsPrefs(currentAccount);
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
        channelId = normalizeChannelId(channelId);
        if (channelId == 0) return;

        final long normalizedId = channelId;
        periodicSyncRunnable = new Runnable() {
            @Override
            public void run() {
                checkAndSync(currentAccount, normalizedId);
                AndroidUtilities.runOnUIThread(this, SYNC_INTERVAL_MS);
            }
        };

        // Запускаем первично и ставим таймер на каждые 10 минут
        checkAndSync(currentAccount, normalizedId);
        AndroidUtilities.runOnUIThread(periodicSyncRunnable, SYNC_INTERVAL_MS);
    }

    public static void stopPeriodicSync() {
        if (periodicSyncRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(periodicSyncRunnable);
            periodicSyncRunnable = null;
        }
    }
}
