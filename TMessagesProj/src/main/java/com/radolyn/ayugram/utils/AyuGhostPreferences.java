package com.radolyn.ayugram.utils;

import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import tw.nekomimi.nekogram.NekoConfig;

/**
 * Ghost Mode Preferences adapted 1:1 from Phantomly.
 */
public class AyuGhostPreferences {

    public static final String KEY_STEALTH = "phantomly_stealth";
    public static final String KEY_INSTANT_OFFLINE = "phantomly_instant_offline";
    public static final String KEY_BLOCK_TYPING = "phantomly_block_typing";
    public static final String KEY_BLOCK_READ = "phantomly_block_read";
    public static final String KEY_BLOCK_STORIES = "phantomly_block_stories";
    public static final String KEY_BLOCK_CHANNEL_VIEWS = "phantomly_block_channel_views";
    public static final String KEY_MUTE_SEND = "phantomly_mute_send"; // 0: off, 1: always, 2: ghost only
    public static final String KEY_ALLOW_SCREENSHOTS = "phantomly_allow_screenshots";
    public static final String KEY_NO_SCREENSHOT_NOTIFY = "phantomly_no_screenshot_notify";
    public static final String KEY_BYPASS_FORWARD = "phantomly_bypass_forward";
    public static final String KEY_ALLOW_COPY = "phantomly_allow_copy";
    public static final String KEY_BYPASS_SAVE = "phantomly_bypass_save";
    public static final String KEY_BYPASS_STORY = "phantomly_bypass_story";
    public static final String KEY_KEEP_ONCE = "phantomly_keep_once";
    public static final String KEY_BLOCK_MEDIA_SEEN = "phantomly_block_media_seen";
    public static final String KEY_HIDE_RECOMMENDED = "phantomly_hide_recommended";
    public static final String KEY_PERCHAT_ENABLED = "phantomly_perchat_enabled";
    public static final String KEY_PC_MODE = "phantomly_pc_mode"; // 0: whitelist, 1: blacklist
    public static final String KEY_PC_CHATS = "phantomly_pc_chats";

    private static final Set<Long> perChatList = ConcurrentHashMap.newKeySet();
    private static volatile boolean perChatListLoaded = false;

    private static SharedPreferences getPrefs() {
        return NekoConfig.getPreferences();
    }

    // --- Master Ghost Switch (stealth) ---
    public static boolean isGhostModeActive() {
        return getStealth();
    }

    public static void setGhostMode(boolean value) {
        setStealth(value);
    }

    public static void toggleGhostMode() {
        setStealth(!getStealth());
    }

    public static boolean getStealth() {
        return getPrefs().getBoolean(KEY_STEALTH, true);
    }

    public static void setStealth(boolean value) {
        getPrefs().edit().putBoolean(KEY_STEALTH, value).apply();
    }

    // --- Instant Offline ---
    public static boolean getInstantOffline() {
        return getStealth() && getPrefs().getBoolean(KEY_INSTANT_OFFLINE, false);
    }

    public static boolean isInstantOfflineConfigured() {
        return getPrefs().getBoolean(KEY_INSTANT_OFFLINE, false);
    }

    public static void setInstantOffline(boolean value) {
        getPrefs().edit().putBoolean(KEY_INSTANT_OFFLINE, value).apply();
    }

    // --- Block Typing ---
    public static boolean getBlockTyping() {
        return getStealth() && getPrefs().getBoolean(KEY_BLOCK_TYPING, false);
    }

    public static boolean isBlockTypingConfigured() {
        return getPrefs().getBoolean(KEY_BLOCK_TYPING, false);
    }

    public static void setBlockTyping(boolean value) {
        getPrefs().edit().putBoolean(KEY_BLOCK_TYPING, value).apply();
    }

    // --- Block Read ---
    public static boolean getBlockRead() {
        return getStealth() && getPrefs().getBoolean(KEY_BLOCK_READ, false);
    }

    public static boolean isBlockReadConfigured() {
        return getPrefs().getBoolean(KEY_BLOCK_READ, false);
    }

    public static void setBlockRead(boolean value) {
        getPrefs().edit().putBoolean(KEY_BLOCK_READ, value).apply();
    }

    // --- Block Stories ---
    public static boolean getBlockStories() {
        return getStealth() && getPrefs().getBoolean(KEY_BLOCK_STORIES, false);
    }

    public static boolean isBlockStoriesConfigured() {
        return getPrefs().getBoolean(KEY_BLOCK_STORIES, false);
    }

    public static void setBlockStories(boolean value) {
        getPrefs().edit().putBoolean(KEY_BLOCK_STORIES, value).apply();
    }

    // --- Block Channel Views ---
    public static boolean getBlockChannelViews() {
        return getStealth() && getPrefs().getBoolean(KEY_BLOCK_CHANNEL_VIEWS, false);
    }

    public static boolean isBlockChannelViewsConfigured() {
        return getPrefs().getBoolean(KEY_BLOCK_CHANNEL_VIEWS, false);
    }

    public static void setBlockChannelViews(boolean value) {
        getPrefs().edit().putBoolean(KEY_BLOCK_CHANNEL_VIEWS, value).apply();
    }

    // --- Mute Send (Silent Sending) ---
    public static int getMuteSend() {
        return getPrefs().getInt(KEY_MUTE_SEND, 0);
    }

    public static void setMuteSend(int value) {
        getPrefs().edit().putInt(KEY_MUTE_SEND, value).apply();
    }

    // --- Allow Screenshots ---
    public static boolean getAllowScreenshots() {
        return getStealth() && getPrefs().getBoolean(KEY_ALLOW_SCREENSHOTS, false);
    }

    public static boolean isAllowScreenshotsConfigured() {
        return getPrefs().getBoolean(KEY_ALLOW_SCREENSHOTS, false);
    }

    public static void setAllowScreenshots(boolean value) {
        getPrefs().edit().putBoolean(KEY_ALLOW_SCREENSHOTS, value).apply();
    }

    // --- No Screenshot Notify ---
    public static boolean getNoScreenshotNotify() {
        return getStealth() && getPrefs().getBoolean(KEY_NO_SCREENSHOT_NOTIFY, false);
    }

    public static boolean isNoScreenshotNotifyConfigured() {
        return getPrefs().getBoolean(KEY_NO_SCREENSHOT_NOTIFY, false);
    }

    public static void setNoScreenshotNotify(boolean value) {
        getPrefs().edit().putBoolean(KEY_NO_SCREENSHOT_NOTIFY, value).apply();
    }

    // --- Bypass Forward ---
    public static boolean getBypassForward() {
        return getStealth() && getPrefs().getBoolean(KEY_BYPASS_FORWARD, false);
    }

    public static boolean isBypassForwardConfigured() {
        return getPrefs().getBoolean(KEY_BYPASS_FORWARD, false);
    }

    public static void setBypassForward(boolean value) {
        getPrefs().edit().putBoolean(KEY_BYPASS_FORWARD, value).apply();
    }

    // --- Allow Copy ---
    public static boolean getAllowCopy() {
        return getStealth() && getPrefs().getBoolean(KEY_ALLOW_COPY, false);
    }

    public static boolean isAllowCopyConfigured() {
        return getPrefs().getBoolean(KEY_ALLOW_COPY, false);
    }

    public static void setAllowCopy(boolean value) {
        getPrefs().edit().putBoolean(KEY_ALLOW_COPY, value).apply();
    }

    // --- Bypass Save ---
    public static boolean getBypassSave() {
        return getStealth() && getPrefs().getBoolean(KEY_BYPASS_SAVE, false);
    }

    public static boolean isBypassSaveConfigured() {
        return getPrefs().getBoolean(KEY_BYPASS_SAVE, false);
    }

    public static void setBypassSave(boolean value) {
        getPrefs().edit().putBoolean(KEY_BYPASS_SAVE, value).apply();
    }

    // --- Bypass Story ---
    public static boolean getBypassStory() {
        return getStealth() && getPrefs().getBoolean(KEY_BYPASS_STORY, false);
    }

    public static boolean isBypassStoryConfigured() {
        return getPrefs().getBoolean(KEY_BYPASS_STORY, false);
    }

    public static void setBypassStory(boolean value) {
        getPrefs().edit().putBoolean(KEY_BYPASS_STORY, value).apply();
    }

    // --- Keep Once (View-Once media preservation) ---
    public static boolean getKeepOnce() {
        return getStealth() && getPrefs().getBoolean(KEY_KEEP_ONCE, false);
    }

    public static boolean isKeepOnceConfigured() {
        return getPrefs().getBoolean(KEY_KEEP_ONCE, false);
    }

    public static void setKeepOnce(boolean value) {
        getPrefs().edit().putBoolean(KEY_KEEP_ONCE, value).apply();
    }

    // --- Block Media Seen ---
    public static boolean getBlockMediaSeen() {
        return getStealth() && getPrefs().getBoolean(KEY_BLOCK_MEDIA_SEEN, false);
    }

    public static boolean isBlockMediaSeenConfigured() {
        return getPrefs().getBoolean(KEY_BLOCK_MEDIA_SEEN, false);
    }

    public static void setBlockMediaSeen(boolean value) {
        getPrefs().edit().putBoolean(KEY_BLOCK_MEDIA_SEEN, value).apply();
    }

    // --- Hide Recommended Channels ---
    public static boolean getHideRecommended() {
        return getStealth() && getPrefs().getBoolean(KEY_HIDE_RECOMMENDED, false);
    }

    public static boolean isHideRecommendedConfigured() {
        return getPrefs().getBoolean(KEY_HIDE_RECOMMENDED, false);
    }

    public static void setHideRecommended(boolean value) {
        getPrefs().edit().putBoolean(KEY_HIDE_RECOMMENDED, value).apply();
    }

    // --- Per-Chat Scoping ---
    public static boolean getPerchatEnabled() {
        return getPrefs().getBoolean(KEY_PERCHAT_ENABLED, false);
    }

    public static void setPerchatEnabled(boolean value) {
        getPrefs().edit().putBoolean(KEY_PERCHAT_ENABLED, value).apply();
    }

    public static int getPcMode() {
        return getPrefs().getInt(KEY_PC_MODE, 0); // 0: whitelist, 1: blacklist
    }

    public static void setPcMode(int value) {
        getPrefs().edit().putInt(KEY_PC_MODE, value).apply();
    }

    private static void ensurePerChatListLoaded() {
        if (!perChatListLoaded) {
            synchronized (perChatList) {
                if (!perChatListLoaded) {
                    Set<String> set = getPrefs().getStringSet(KEY_PC_CHATS, null);
                    if (set != null) {
                        for (String s : set) {
                            try {
                                perChatList.add(Long.parseLong(s));
                            } catch (Exception ignored) {}
                        }
                    }
                    perChatListLoaded = true;
                }
            }
        }
    }

    public static boolean isChatInPerChatList(long chatId) {
        ensurePerChatListLoaded();
        return perChatList.contains(Math.abs(chatId));
    }

    public static void addChatToPerChatList(long chatId) {
        ensurePerChatListLoaded();
        long key = Math.abs(chatId);
        perChatList.add(key);
        savePerChatList();
    }

    public static void removeChatFromPerChatList(long chatId) {
        ensurePerChatListLoaded();
        long key = Math.abs(chatId);
        perChatList.remove(key);
        savePerChatList();
    }

    public static Set<Long> getPerChatList() {
        ensurePerChatListLoaded();
        return new HashSet<>(perChatList);
    }

    private static void savePerChatList() {
        Set<String> set = new HashSet<>();
        for (Long l : perChatList) {
            set.add(String.valueOf(l));
        }
        getPrefs().edit().putStringSet(KEY_PC_CHATS, set).apply();
    }

    /**
     * Determines if Ghost Mode applies to a specific chat based on per-chat scoping rules.
     */
    public static boolean shouldApplyGhostToChat(long chatId) {
        if (!getStealth()) return false;
        if (!getPerchatEnabled()) return true;
        boolean inList = isChatInPerChatList(chatId);
        return getPcMode() == 0 ? inList : !inList;
    }

    // --- Backward compatibility methods for previous calls ---
    public static boolean getGhostModeReadExclusion(long chatId) {
        return !shouldApplyGhostToChat(chatId);
    }

    public static void setGhostModeReadExclusion(long chatId, boolean excluded) {
        if (excluded) {
            if (getPcMode() == 0) removeChatFromPerChatList(chatId);
            else addChatToPerChatList(chatId);
        } else {
            if (getPcMode() == 0) addChatToPerChatList(chatId);
            else removeChatFromPerChatList(chatId);
        }
    }

    public static boolean getGhostModeTypingExclusion(long chatId) {
        return !shouldApplyGhostToChat(chatId);
    }

    public static void setGhostModeTypingExclusion(long chatId, boolean excluded) {
        setGhostModeReadExclusion(chatId, excluded);
    }

    public static boolean getBlockRead(long chatId) {
        return getBlockRead() && shouldApplyGhostToChat(chatId);
    }

    public static boolean getBlockTyping(long chatId) {
        return getBlockTyping() && shouldApplyGhostToChat(chatId);
    }
}
