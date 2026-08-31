package soundgram.music;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;

import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

public class SoundGramConfig {
    public static final String KEY = "soundgram_prefs";

    public static String chatListWallPath = "";
    public static String chatListWallVideoPath = "";
    public static String chatVideoPath = "";
    public static float chatListWallDimming = 0.0f;
    // Главный тумблер
    public static boolean stealthMode = true;
    public static boolean zeroChatGloballyEnabled = true;
    public static String activeZeroChatKey = null;
    public static HashMap<String, String> zeroChatKeys = new HashMap<>();
    public static HashMap<Long, String> customUserBadges = new HashMap<>();

    public static void load() {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(KEY, Context.MODE_PRIVATE);

        chatListWallPath = prefs.getString("chatlist_path", "");
        chatListWallVideoPath = prefs.getString("chatlist_video_path", "");
        chatVideoPath = prefs.getString("video_path", "");
        chatListWallDimming = prefs.getFloat("chatlist_dimming", 0.0f);
        stealthMode = prefs.getBoolean("stealth_mode", true);


        zeroChatGloballyEnabled = prefs.getBoolean("zc_enabled", false);
        try {
            String keysJson = prefs.getString("zc_keys", "{}");
            JSONObject obj = new JSONObject(keysJson);
            Iterator<String> keys = obj.keys();
            zeroChatKeys.clear();
            while (keys.hasNext()) {
                String k = keys.next();
                zeroChatKeys.put(k, obj.getString(k));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }

        try {
            String badgesJson = prefs.getString("custom_user_badges", "{}");
            JSONObject obj = new JSONObject(badgesJson);
            Iterator<String> keys = obj.keys();
            customUserBadges.clear();
            while (keys.hasNext()) {
                String k = keys.next();
                customUserBadges.put(Long.parseLong(k), obj.getString(k));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }

        fetchCustomBadges();
    }

    public static void save() {
        SharedPreferences.Editor editor = ApplicationLoader.applicationContext.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();

        editor.putString("chatlist_path", chatListWallPath);
        editor.putString("chatlist_video_path", chatListWallVideoPath);
        editor.putString("video_path", chatVideoPath);
        editor.putFloat("chatlist_dimming", chatListWallDimming);
        editor.putBoolean("stealth_mode", stealthMode);
        editor.putBoolean("zc_enabled", zeroChatGloballyEnabled);
        try {
            editor.putString("zc_keys", new JSONObject(zeroChatKeys).toString());
        } catch (Exception e) {
            FileLog.e(e);
        }
        editor.apply();
    }

    public static void fetchCustomBadges() {
        new Thread(() -> {
            try {
                String urlString = "https://raw.githubusercontent.com/soundgram-project/SoundGram_donate/main/user.txt?t=" + System.currentTimeMillis();
                URL url = new URL(urlString);
                URLConnection connection = url.openConnection();
                connection.setUseCaches(false);
                connection.setDefaultUseCaches(false);
                connection.setRequestProperty("Cache-Control", "no-cache");

                Scanner s = new Scanner(connection.getInputStream());
                HashMap<Long, String> tempMap = new HashMap<>();
                while (s.hasNextLine()) {
                    String line = s.nextLine().trim();
                    if (line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        String badge = parts[1].trim();
                        String[] ids = parts[0].split(",");
                        for (String idStr : ids) {
                            try { tempMap.put(Long.parseLong(idStr.trim()), badge); } catch (Exception e) {}
                        }
                    }
                }
                s.close();

                AndroidUtilities.runOnUIThread(() -> {
                    customUserBadges.clear();
                    customUserBadges.putAll(tempMap);
                    try {
                        SharedPreferences.Editor editor = ApplicationLoader.applicationContext.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
                        JSONObject obj = new JSONObject();
                        for (HashMap.Entry<Long, String> entry : customUserBadges.entrySet()) {
                            obj.put(String.valueOf(entry.getKey()), entry.getValue());
                        }
                        editor.putString("custom_user_badges", obj.toString());
                        editor.apply();
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                        NotificationCenter.getInstance(a).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                    }
                });
            } catch (Exception e) {
                FileLog.e("SoundGram Error: " + e.getMessage());
            }
        }).start();
    }
}
