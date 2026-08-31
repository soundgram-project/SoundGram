/*
 * This file is part of NimarkoGram for Android.
 * Licensed under GNU GPL v2 or later. See LICENSE.
 * Copyright Ettacent, 2026.
 */

package app.nimarkogram.messenger.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.UserConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import app.nimarkogram.messenger.NimarkoConfig;

public final class NimarkoConfigBackup {

    private static final String MAGIC = "nimarkoconfig";
    private static final int VERSION = 4;
    private static final int MAX_BACKUP_BYTES = 2 * 1024 * 1024;
    private static final int MAX_NAMESPACES = 16;
    private static final int MAX_KEYS_PER_NAMESPACE = 2048;
    private static final int MAX_KEY_LENGTH = 160;
    private static final int MAX_STRING_LENGTH = 256 * 1024;
    private static final int MAX_SET_ITEMS = 4096;
    private static final Pattern SAFE_KEY = Pattern.compile("^[A-Za-z0-9_.:-]{1," + MAX_KEY_LENGTH + "}$");

    private static final String PREFS_MAIN = "nimarkoconfig";
    private static final String PREFS_UPDATE = "nimarko_update";

    private static final List<String> PORTABLE_NAMESPACES = Arrays.asList(
            PREFS_MAIN, PREFS_UPDATE);
    private static final Set<String> PORTABLE_NAMESPACE_SET = new HashSet<>(PORTABLE_NAMESPACES);

    // Ignored namespaces from previous versions for backwards compatibility with backups
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

    private static final Set<String> JSON_STRING_BACKED_MAIN_KEYS = new HashSet<>(Arrays.asList(
            "folderColors",
            "folderBadgeMode",
            "messageMenuOrder",
            "chatCompactOverrideOn",
            "chatCompactOverrideOff"));

    private static final Map<String, Map<String, String>> FIXED_SCHEMA = buildFixedSchema();

    private static Map<String, Map<String, String>> buildFixedSchema() {
        Map<String, Map<String, String>> out = new HashMap<>();
        out.put(PREFS_UPDATE, schema("autoOTA", "b"));
        return out;
    }

    private static Map<String, String> schema(String... pairs) {
        Map<String, String> out = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) out.put(pairs[i], pairs[i + 1]);
        return out;
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null || SENSITIVE_KEYS.contains(key)) return true;
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.endsWith("token") || lower.endsWith("auth") || lower.endsWith("secret") || lower.endsWith("signature");
    }

    private static SharedPreferences prefs(String namespace) {
        return ApplicationLoader.applicationContext.getSharedPreferences(namespace, Context.MODE_PRIVATE);
    }

    private static Map<String, Object> portableSnapshot(String namespace) {
        SharedPreferences preferences = prefs(namespace);
        Map<String, ?> all = preferences.getAll();
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String expected = expectedType(namespace, entry.getKey(), entry.getValue());
            if (expected != null && expected.equals(typeOfValue(entry.getValue()))) {
                out.put(entry.getKey(), clonePortableValue(entry.getValue()));
            }
        }
        return out;
    }

    private static Object clonePortableValue(Object value) {
        if (value instanceof Set) return new HashSet<>((Set<?>) value);
        return value;
    }

    private static String expectedType(String namespace, String key) {
        if (key == null || !SAFE_KEY.matcher(key).matches() || isSensitiveKey(key)) return null;
        if (PREFS_MAIN.equals(namespace)) {
            if (isDeviceLocalSecurityKey(key)) return null;
            if (key.startsWith("notificationReaction_") || key.startsWith("notificationReactionEmoji_")) {
                return "s";
            }
            if (JSON_STRING_BACKED_MAIN_KEYS.contains(key)) return "s";
            if (isCustomSavedMessagesDialogKey(key)) return "l";
            try {
                Field field = NimarkoConfig.class.getDeclaredField(key);
                int mods = field.getModifiers();
                if (!Modifier.isStatic(mods) || Modifier.isFinal(mods)) return null;
                Class<?> type = field.getType();
                if (type == boolean.class || type == Boolean.class) return "b";
                if (type == int.class || type == Integer.class) return "i";
                if (type == long.class || type == Long.class) return "l";
                if (type == float.class || type == Float.class) return "f";
                if (type == String.class || CharSequence.class.isAssignableFrom(type)) return "s";
                if (Set.class.isAssignableFrom(type)) return "ss";
            } catch (Throwable ignore) {
            }
            return null;
        }
        Map<String, String> schema = FIXED_SCHEMA.get(namespace);
        return schema == null ? null : schema.get(key);
    }

    private static boolean isDeviceLocalSecurityKey(String key) {
        if (DEVICE_LOCAL_SECURITY_KEYS.contains(key)) return true;
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains("biometric") || normalized.contains("passcode");
    }

    private static boolean isCustomSavedMessagesDialogKey(String key) {
        final String prefix = "customSavedMessagesDialogId_a";
        if (key == null || !key.startsWith(prefix)) return false;
        String suffix = key.substring(prefix.length());
        if (suffix.isEmpty() || suffix.length() > 2 || suffix.charAt(0) == '0' && suffix.length() > 1) {
            return false;
        }
        int account = 0;
        for (int i = 0; i < suffix.length(); i++) {
            char c = suffix.charAt(i);
            if (c < '0' || c > '9') return false;
            account = account * 10 + c - '0';
        }
        return account >= 0 && account < UserConfig.MAX_ACCOUNT_COUNT;
    }

    private static String expectedType(String namespace, String key, Object value) {
        return expectedType(namespace, key);
    }

    private static String typeOfValue(Object value) {
        if (value instanceof Boolean) return "b";
        if (value instanceof Integer) return "i";
        if (value instanceof Long) return "l";
        if (value instanceof Float) return "f";
        if (value instanceof String) return "s";
        if (value instanceof Set) return "ss";
        return null;
    }

    private static JSONObject dumpPrefs(String namespace, SharedPreferences preferences) throws Exception {
        JSONObject data = new JSONObject();
        Map<String, ?> snapshot = preferences.getAll();
        for (Map.Entry<String, ?> entry : snapshot.entrySet()) {
            Object portableValue = entry.getValue();
            String type = typeOfValue(portableValue);
            if (type == null || !type.equals(expectedType(namespace, entry.getKey(), portableValue))) continue;
            JSONObject tagged = new JSONObject().put("t", type);
            if ("ss".equals(type)) tagged.put("v", new JSONArray((Set<?>) portableValue));
            else tagged.put("v", portableValue);
            data.put(entry.getKey(), tagged);
        }
        return data;
    }

    public static String exportJson() throws Exception {
        return NimarkoConfig.withSettingsTransaction(NimarkoConfigBackup::exportJsonLocked);
    }

    private static String exportJsonLocked() throws Exception {
        JSONObject namespaces = new JSONObject();
        for (String name : PORTABLE_NAMESPACES) namespaces.put(name, dumpPrefs(name, prefs(name)));
        JSONObject root = new JSONObject()
                .put("magic", MAGIC)
                .put("version", VERSION)
                .put("namespaces", namespaces);
        String result = root.toString(2);
        if (!withinSizeLimit(result)) throw new IllegalStateException("backup too large");
        return result;
    }

    private static final class PendingNamespace {
        final String name;
        final Map<String, Object> values = new LinkedHashMap<>();
        PendingNamespace(String name) { this.name = name; }
    }

    private static Object decodeValue(String type, Object raw) throws Exception {
        if ("b".equals(type)) {
            if (!(raw instanceof Boolean)) throw new IllegalArgumentException("bad boolean");
            return raw;
        } else if ("i".equals(type)) {
            if (!(raw instanceof Integer)) throw new IllegalArgumentException("bad int");
            return raw;
        } else if ("l".equals(type)) {
            if (!(raw instanceof Long) && !(raw instanceof Integer)) throw new IllegalArgumentException("bad long");
            return ((Number) raw).longValue();
        } else if ("f".equals(type)) {
            if (!(raw instanceof Number)) throw new IllegalArgumentException("bad float");
            double d = ((Number) raw).doubleValue();
            if (!Double.isFinite(d) || Math.abs(d) > Float.MAX_VALUE) throw new IllegalArgumentException("bad float");
            return (float) d;
        } else if ("s".equals(type)) {
            if (!(raw instanceof String) || ((String) raw).length() > MAX_STRING_LENGTH) {
                throw new IllegalArgumentException("bad string");
            }
            return raw;
        } else if ("ss".equals(type)) {
            if (!(raw instanceof JSONArray)) throw new IllegalArgumentException("bad string set");
            JSONArray array = (JSONArray) raw;
            if (array.length() > MAX_SET_ITEMS) throw new IllegalArgumentException("set too large");
            Set<String> set = new HashSet<>();
            for (int i = 0; i < array.length(); i++) {
                Object item = array.get(i);
                if (!(item instanceof String) || ((String) item).length() > MAX_STRING_LENGTH) {
                    throw new IllegalArgumentException("bad set item");
                }
                set.add((String) item);
            }
            return set;
        }
        throw new IllegalArgumentException("unknown type");
    }

    private static PendingNamespace validateNamespace(String name, JSONObject data) throws Exception {
        if (!PORTABLE_NAMESPACE_SET.contains(name) || data == null || data.length() > MAX_KEYS_PER_NAMESPACE) {
            throw new IllegalArgumentException("invalid namespace " + name);
        }
        PendingNamespace out = new PendingNamespace(name);
        Iterator<String> keys = data.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!SAFE_KEY.matcher(key).matches() || isSensitiveKey(key)) {
                throw new IllegalArgumentException("invalid key " + key + " in " + name);
            }
            JSONObject entry = data.getJSONObject(key);
            String type = entry.getString("t");
            Object raw = entry.get("v");
            String expected = expectedType(name, key);
            if (expected == null || !expected.equals(type)) {
                throw new IllegalArgumentException("type mismatch for " + key + " in " + name);
            }
            out.values.put(key, decodeValue(type, raw));
        }
        return out;
    }

    private static boolean replaceNamespace(PendingNamespace namespace) {
        return replaceNamespace(namespace.name, namespace.values);
    }

    @SuppressWarnings("unchecked")
    private static boolean replaceNamespace(String name, Map<String, Object> values) {
        SharedPreferences preferences = prefs(name);
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : preferences.getAll().keySet()) {
            if (expectedType(name, key) != null) editor.remove(key);
        }
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object v = entry.getValue();
            if (v instanceof Boolean) editor.putBoolean(entry.getKey(), (Boolean) v);
            else if (v instanceof Integer) editor.putInt(entry.getKey(), (Integer) v);
            else if (v instanceof Long) editor.putLong(entry.getKey(), (Long) v);
            else if (v instanceof Float) editor.putFloat(entry.getKey(), (Float) v);
            else if (v instanceof String) editor.putString(entry.getKey(), (String) v);
            else if (v instanceof Set) editor.putStringSet(entry.getKey(), (Set<String>) v);
        }
        if (!editor.commit()) return false;
        return verifyNamespaceContents(name, values);
    }

    private static boolean verifyNamespaceContents(String name, Map<String, Object> expected) {
        SharedPreferences preferences = prefs(name);
        Map<String, ?> snapshot = preferences.getAll();
        Map<String, Object> filtered = new HashMap<>();
        for (Map.Entry<String, ?> entry : snapshot.entrySet()) {
            String type = expectedType(name, entry.getKey(), entry.getValue());
            if (type != null && type.equals(typeOfValue(entry.getValue()))) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return Objects.equals(filtered, expected);
    }

    private static boolean rollbackImport(Throwable failure, List<String> touched,
                                          Map<String, Map<String, Object>> snapshots) {
        boolean preferencesRestored = true;
        if (snapshots != null) {
            for (String name : touched) {
                Map<String, Object> snapshot = snapshots.get(name);
                if (snapshot != null) {
                    try {
                        preferencesRestored &= replaceNamespace(name, snapshot);
                    } catch (Throwable t) {
                        preferencesRestored = false;
                        FileLog.e("Nimarko config rollback failed for " + name, t);
                    }
                }
            }
        }
        if (!preferencesRestored) {
            FileLog.e("Nimarko config import ROLLBACK FAILED: preferences=" + preferencesRestored, failure);
            return false;
        }
        FileLog.e("Nimarko config import failed; verified rollback completed", failure);
        return true;
    }

    public static boolean importJson(String json) {
        try {
            return NimarkoConfig.withSettingsTransaction(() -> importJsonLocked(json));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            FileLog.e("Nimarko config import settings transaction failed", e);
            return false;
        }
    }

    private static boolean importJsonLocked(String json) {
        if (json == null || json.length() == 0 || !withinSizeLimit(json)) return false;
        Map<String, Map<String, Object>> snapshots = null;
        ArrayList<String> touched = new ArrayList<>();
        boolean mutationStarted = false;
        int version = 0;
        try {
            JSONObject root = new JSONObject(json.trim());
            if (!MAGIC.equals(root.optString("magic", null))) return false;
            Object versionObject = root.opt("version");
            if (!(versionObject instanceof Integer)) return false;
            version = (Integer) versionObject;
            if (version < 1 || version > VERSION) return false;

            ArrayList<PendingNamespace> pending = new ArrayList<>();
            if (version == 1) {
                JSONObject data = root.optJSONObject("data");
                if (data == null) return false;
                pending.add(validateNamespace(PREFS_MAIN, data));
            } else {
                JSONObject namespaces = root.optJSONObject("namespaces");
                if (namespaces == null || namespaces.length() > MAX_NAMESPACES) return false;
                Iterator<String> names = namespaces.keys();
                while (names.hasNext()) {
                    String name = names.next();
                    if (IGNORED_NAMESPACES.contains(name)) continue;
                    if (!PORTABLE_NAMESPACE_SET.contains(name)) return false;
                    pending.add(validateNamespace(name, namespaces.optJSONObject(name)));
                }
                if (version >= 3) {
                    for (String name : PORTABLE_NAMESPACES) {
                        boolean found = false;
                        for (PendingNamespace p : pending) if (name.equals(p.name)) { found = true; break; }
                        if (!found) pending.add(new PendingNamespace(name));
                    }
                }
            }

            snapshots = new HashMap<>();
            for (PendingNamespace p : pending) snapshots.put(p.name, portableSnapshot(p.name));
            for (PendingNamespace p : pending) {
                touched.add(p.name);
                mutationStarted = true;
                if (!replaceNamespace(p)) {
                    throw new IllegalStateException("preferences commit verification failed for " + p.name);
                }
            }
        } catch (Throwable failure) {
            if (mutationStarted) {
                boolean rollbackSucceeded = rollbackImport(failure, touched, snapshots);
                if (!rollbackSucceeded) {
                    throw new IllegalStateException("Nimarko config import rollback failed", failure);
                }
            }
            FileLog.e("Nimarko config import failed safely", failure);
            return false;
        }
        return true;
    }

    private static boolean withinSizeLimit(String value) {
        return value != null && value.length() <= MAX_BACKUP_BYTES
                && value.getBytes(StandardCharsets.UTF_8).length <= MAX_BACKUP_BYTES;
    }

    public static boolean looksLikeBackup(String json) {
        if (json == null || json.length() == 0 || !withinSizeLimit(json)) return false;
        try {
            JSONObject root = new JSONObject(json.trim());
            return MAGIC.equals(root.optString("magic", null));
        } catch (Throwable ignore) {
            return false;
        }
    }

    private NimarkoConfigBackup() {}
}
