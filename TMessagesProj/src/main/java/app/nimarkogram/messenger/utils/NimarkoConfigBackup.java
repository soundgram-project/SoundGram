/*
 * This file is part of SoundGram / NimarkoGram for Android.
 * Licensed under GNU GPL v2 or later. See LICENSE.
 * Copyright Ettacent, 2026.
 */

package app.nimarkogram.messenger.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import app.nimarkogram.messenger.NimarkoConfig;
import tw.nekomimi.nekogram.NekoConfig;
import com.radolyn.ayugram.utils.AyuGhostPreferences;
import soundgram.music.SoundGramConfig;
import soundgram.animation.TextAnimationLoader;

public final class NimarkoConfigBackup {

    public static final String MAGIC = "soundgram_config";
    public static final String LEGACY_MAGIC = "nimarkoconfig";
    private static final int VERSION = 5;
    private static final int MAX_BACKUP_BYTES = 5 * 1024 * 1024;
    private static final int MAX_STRING_LENGTH = 1024 * 1024;
    private static final int MAX_SET_ITEMS = 8192;
    private static final Pattern SAFE_KEY = Pattern.compile("^[A-Za-z0-9_.:-]{1,160}$");

    public static final String PREFS_MAIN = "nimarkoconfig";
    public static final String PREFS_UPDATE = "nimarko_update";
    public static final String PREFS_SOUNDGRAM = "soundgram_prefs";
    public static final String PREFS_SOUNDGRAM_CFG = "soundgram_config";
    public static final String PREFS_NEKO = "nkmrcfg";
    public static final String PREFS_TEXTANIM = "soundgram_textanim";
    public static final String PREFS_STYLE = "soundgram_style";
    public static final String PREFS_AUDIO_FX = "soundgram_fx";
    public static final String PREFS_ICONPACKS = "iconpacks";
    public static final String PREFS_WSBYPASS = "tgwsproxy";
    public static final String PREFS_PUBLIC_LIB = "soundgram_public_library";

    public static final List<String> PORTABLE_NAMESPACES = Arrays.asList(
            PREFS_MAIN,
            PREFS_UPDATE,
            PREFS_SOUNDGRAM,
            PREFS_SOUNDGRAM_CFG,
            PREFS_NEKO,
            PREFS_TEXTANIM,
            PREFS_STYLE,
            PREFS_AUDIO_FX,
            PREFS_ICONPACKS,
            PREFS_WSBYPASS,
            PREFS_PUBLIC_LIB
    );
    public static final Set<String> PORTABLE_NAMESPACE_SET = new HashSet<>(PORTABLE_NAMESPACES);

    private static final Set<String> IGNORED_NAMESPACES = new HashSet<>(Arrays.asList(
            "nimarko_banners", "nimarko_wsbypass", "nm_pillstack", "plugin_settings"));

    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
            "nimarkoMediaAuthToken", "voipRelayAuthToken", "wsRelayAuthToken", "wsInstallId",
            "auth_token", "mtproto_secret", "ws_relay_cred", "voip_relay_cred"));

    private static final Set<String> DEVICE_LOCAL_SECURITY_KEYS = new HashSet<>(Arrays.asList(
            "askBiometricsBeforeDelete",
            "askBiometricsToOpenChat",
            "askBiometricsToOpenSavedMessages",
            "lockedChatsBiometricTtlSec",
            "allowSystemPasscode",
            "askBiometricsToOpenArchive",
            "askBiometricsToOpenEncrypted",
            "askPasscodeBeforeDelete"));

    private static boolean isSensitiveKey(String key) {
        if (key == null || SENSITIVE_KEYS.contains(key)) return true;
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.endsWith("token") || lower.endsWith("auth") || lower.endsWith("secret") || lower.endsWith("signature");
    }

    private static boolean isDeviceLocalSecurityKey(String key) {
        if (DEVICE_LOCAL_SECURITY_KEYS.contains(key)) return true;
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains("biometric") || normalized.contains("passcode");
    }

    private static SharedPreferences prefs(String namespace) {
        return ApplicationLoader.applicationContext.getSharedPreferences(namespace, Context.MODE_PRIVATE);
    }

    private static String typeOfValue(Object value) {
        if (value instanceof Boolean) return "b";
        if (value instanceof Integer) return "i";
        if (value instanceof Long) return "l";
        if (value instanceof Float || value instanceof Double) return "f";
        if (value instanceof String) return "s";
        if (value instanceof Set) return "ss";
        return null;
    }

    private static JSONObject dumpPrefs(String namespace, SharedPreferences preferences) {
        JSONObject data = new JSONObject();
        try {
            Map<String, ?> snapshot = preferences.getAll();
            for (Map.Entry<String, ?> entry : snapshot.entrySet()) {
                String key = entry.getKey();
                if (key == null || !SAFE_KEY.matcher(key).matches() || isSensitiveKey(key)) continue;
                if (PREFS_MAIN.equals(namespace) && isDeviceLocalSecurityKey(key)) continue;
                Object portableValue = entry.getValue();
                String type = typeOfValue(portableValue);
                if (type == null) continue;
                JSONObject tagged = new JSONObject().put("t", type);
                if ("ss".equals(type)) {
                    tagged.put("v", new JSONArray((Set<?>) portableValue));
                } else {
                    tagged.put("v", portableValue);
                }
                data.put(key, tagged);
            }
        } catch (Throwable t) {
            FileLog.e("Failed to dump prefs for " + namespace, t);
        }
        return data;
    }

    public static String exportJson() throws Exception {
        return NimarkoConfig.withSettingsTransaction(NimarkoConfigBackup::exportJsonLocked);
    }

    private static String exportJsonLocked() throws Exception {
        JSONObject namespaces = new JSONObject();
        for (String name : PORTABLE_NAMESPACES) {
            namespaces.put(name, dumpPrefs(name, prefs(name)));
        }
        JSONObject root = new JSONObject()
                .put("magic", MAGIC)
                .put("version", VERSION)
                .put("namespaces", namespaces);
        String result = root.toString(2);
        if (!withinSizeLimit(result)) throw new IllegalStateException("backup too large");
        return result;
    }

    private static Object decodeRawValue(Object raw) {
        if (raw == null || raw == JSONObject.NULL) return null;
        if (raw instanceof Boolean) return raw;
        if (raw instanceof Integer) return raw;
        if (raw instanceof Long) return raw;
        if (raw instanceof Double || raw instanceof Float) {
            double d = ((Number) raw).doubleValue();
            if (!Double.isFinite(d) || Math.abs(d) > Float.MAX_VALUE) return null;
            return (float) d;
        }
        if (raw instanceof String) {
            String s = (String) raw;
            if (s.length() > MAX_STRING_LENGTH) return null;
            return s;
        }
        if (raw instanceof JSONArray) {
            JSONArray arr = (JSONArray) raw;
            if (arr.length() > MAX_SET_ITEMS) return null;
            Set<String> set = new HashSet<>();
            for (int i = 0; i < arr.length(); i++) {
                Object item = arr.opt(i);
                if (item instanceof String && ((String) item).length() <= MAX_STRING_LENGTH) {
                    set.add((String) item);
                }
            }
            return set;
        }
        return null;
    }

    private static Object decodeValue(String type, Object raw) {
        if (raw == null || raw == JSONObject.NULL) return null;
        if ("b".equals(type)) {
            if (raw instanceof Boolean) return raw;
            if (raw instanceof String) return Boolean.parseBoolean((String) raw);
            if (raw instanceof Number) return ((Number) raw).intValue() != 0;
            return null;
        } else if ("i".equals(type)) {
            if (raw instanceof Number) return ((Number) raw).intValue();
            if (raw instanceof String) {
                try { return Integer.parseInt((String) raw); } catch (Exception ignored) {}
            }
            return null;
        } else if ("l".equals(type)) {
            if (raw instanceof Number) return ((Number) raw).longValue();
            if (raw instanceof String) {
                try { return Long.parseLong((String) raw); } catch (Exception ignored) {}
            }
            return null;
        } else if ("f".equals(type)) {
            if (raw instanceof Number) {
                double d = ((Number) raw).doubleValue();
                if (Double.isFinite(d) && Math.abs(d) <= Float.MAX_VALUE) return (float) d;
            }
            if (raw instanceof String) {
                try { return Float.parseFloat((String) raw); } catch (Exception ignored) {}
            }
            return null;
        } else if ("s".equals(type)) {
            if (raw instanceof String) {
                String s = (String) raw;
                return s.length() <= MAX_STRING_LENGTH ? s : null;
            }
            return String.valueOf(raw);
        } else if ("ss".equals(type)) {
            if (raw instanceof JSONArray) {
                JSONArray array = (JSONArray) raw;
                if (array.length() > MAX_SET_ITEMS) return null;
                Set<String> set = new HashSet<>();
                for (int i = 0; i < array.length(); i++) {
                    Object item = array.opt(i);
                    if (item instanceof String && ((String) item).length() <= MAX_STRING_LENGTH) {
                        set.add((String) item);
                    }
                }
                return set;
            }
            return null;
        }
        return decodeRawValue(raw);
    }

    private static final class PendingNamespace {
        final String name;
        final Map<String, Object> values = new LinkedHashMap<>();
        PendingNamespace(String name) { this.name = name; }
    }

    private static PendingNamespace parseNamespaceData(String name, JSONObject data) {
        if (!PORTABLE_NAMESPACE_SET.contains(name) || data == null) return null;
        PendingNamespace out = new PendingNamespace(name);
        Iterator<String> keys = data.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key == null || !SAFE_KEY.matcher(key).matches() || isSensitiveKey(key)) continue;
            if (PREFS_MAIN.equals(name) && isDeviceLocalSecurityKey(key)) continue;
            try {
                Object entryObj = data.get(key);
                if (entryObj instanceof JSONObject) {
                    JSONObject entry = (JSONObject) entryObj;
                    String type = entry.optString("t", null);
                    if (type != null && entry.has("v")) {
                        Object decoded = decodeValue(type, entry.get("v"));
                        if (decoded != null) {
                            out.values.put(key, decoded);
                        }
                        continue;
                    }
                }
                Object decoded = decodeRawValue(entryObj);
                if (decoded != null) {
                    out.values.put(key, decoded);
                }
            } catch (Throwable t) {
                FileLog.e("Skipping key " + key + " in " + name, t);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static int writeNamespace(String name, Map<String, Object> values) {
        if (values == null || values.isEmpty()) return 0;
        SharedPreferences preferences = prefs(name);
        SharedPreferences.Editor editor = preferences.edit();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            Object v = entry.getValue();
            if (v instanceof Boolean) editor.putBoolean(key, (Boolean) v);
            else if (v instanceof Integer) editor.putInt(key, (Integer) v);
            else if (v instanceof Long) editor.putLong(key, (Long) v);
            else if (v instanceof Float) editor.putFloat(key, (Float) v);
            else if (v instanceof String) editor.putString(key, (String) v);
            else if (v instanceof Set) editor.putStringSet(key, (Set<String>) v);
        }
        editor.apply();
        return values.size();
    }

    public static boolean importJson(String json) {
        try {
            return NimarkoConfig.withSettingsTransaction(() -> importJsonLocked(json));
        } catch (Throwable e) {
            FileLog.e("SoundGram config import failed", e);
            return false;
        }
    }

    private static boolean importJsonLocked(String json) {
        if (json == null || json.length() == 0 || !withinSizeLimit(json)) return false;
        try {
            JSONObject root = new JSONObject(json.trim());
            ArrayList<PendingNamespace> pendingList = new ArrayList<>();

            if (root.has("namespaces")) {
                JSONObject namespaces = root.optJSONObject("namespaces");
                if (namespaces != null) {
                    Iterator<String> names = namespaces.keys();
                    while (names.hasNext()) {
                        String name = names.next();
                        if (IGNORED_NAMESPACES.contains(name)) continue;
                        JSONObject nsData = namespaces.optJSONObject(name);
                        if (nsData != null) {
                            PendingNamespace p = parseNamespaceData(name, nsData);
                            if (p != null && !p.values.isEmpty()) {
                                pendingList.add(p);
                            }
                        }
                    }
                }
            } else if (root.has("data")) {
                JSONObject data = root.optJSONObject("data");
                if (data != null) {
                    PendingNamespace p = parseNamespaceData(PREFS_MAIN, data);
                    if (p != null && !p.values.isEmpty()) {
                        pendingList.add(p);
                    }
                }
            } else {
                boolean foundNamespace = false;
                for (String name : PORTABLE_NAMESPACES) {
                    if (root.has(name)) {
                        JSONObject nsData = root.optJSONObject(name);
                        if (nsData != null) {
                            PendingNamespace p = parseNamespaceData(name, nsData);
                            if (p != null && !p.values.isEmpty()) {
                                pendingList.add(p);
                                foundNamespace = true;
                            }
                        }
                    }
                }
                if (!foundNamespace) {
                    PendingNamespace p = parseNamespaceData(PREFS_MAIN, root);
                    if (p != null && !p.values.isEmpty()) {
                        pendingList.add(p);
                    }
                }
            }

            if (pendingList.isEmpty()) {
                return false;
            }

            int totalWritten = 0;
            for (PendingNamespace p : pendingList) {
                totalWritten += writeNamespace(p.name, p.values);
            }

            if (totalWritten > 0) {
                applyAllConfigs();
                return true;
            }
            return false;
        } catch (Throwable t) {
            FileLog.e("SoundGram config importJsonLocked error", t);
            return false;
        }
    }

    public static void applyAllConfigs() {
        try {
            NimarkoConfig.reloadConfig();
        } catch (Throwable t) {
            FileLog.e(t);
        }
        try {
            NekoConfig.loadConfig(true);
        } catch (Throwable t) {
            FileLog.e(t);
        }
        try {
            AyuGhostPreferences.resetPerChatList();
        } catch (Throwable t) {
            FileLog.e(t);
        }
        try {
            SoundGramConfig.load();
        } catch (Throwable t) {
            FileLog.e(t);
        }
        try {
            TextAnimationLoader.updateSettings();
        } catch (Throwable t) {
            FileLog.e(t);
        }
        try {
            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                UserConfig.getInstance(a).loadConfig();
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
        AndroidUtilities.runOnUIThread(() -> {
            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.mainUserInfoChanged);
                NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.themeListUpdated);
                NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.dialogsNeedReload);
            }
        });
    }

    private static boolean withinSizeLimit(String value) {
        return value != null && value.length() <= MAX_BACKUP_BYTES
                && value.getBytes(StandardCharsets.UTF_8).length <= MAX_BACKUP_BYTES;
    }

    public static boolean looksLikeBackup(String json) {
        if (json == null || json.length() == 0 || !withinSizeLimit(json)) return false;
        try {
            JSONObject root = new JSONObject(json.trim());
            String magic = root.optString("magic", null);
            if (magic != null && (magic.equals(MAGIC) || magic.equals(LEGACY_MAGIC)
                    || magic.equals("soundgram") || magic.equals("cherrygram") || magic.equals("ayugram"))) {
                return true;
            }
            if (root.has("namespaces") || root.has("data") || root.has(PREFS_MAIN) || root.has(PREFS_SOUNDGRAM) || root.has(PREFS_NEKO)) {
                return true;
            }
            return root.length() > 0;
        } catch (Throwable ignore) {
            return false;
        }
    }

    private NimarkoConfigBackup() {}
}
