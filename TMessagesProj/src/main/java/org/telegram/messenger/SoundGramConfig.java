package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

public class SoundGramConfig {
    public static final String KEY = "soundgram_prefs";

    public static String chatListWallPath = "";
    public static String chatVideoPath = "";
    // Главный тумблер
    public static boolean stealthMode = true;
    public static boolean zeroChatGloballyEnabled = true;
    public static String activeZeroChatKey = null;
    public static HashMap<String, String> zeroChatKeys = new HashMap<>();
    public static HashMap<Long, String> customUserBadges = new HashMap<>();

    public static void load() {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(KEY, Context.MODE_PRIVATE);

        chatListWallPath = prefs.getString("chatlist_path", "");
        chatVideoPath = prefs.getString("video_path", "");
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
        fetchCustomBadges();
    }

    public static void save() {
        SharedPreferences.Editor editor = ApplicationLoader.applicationContext.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();

        editor.putString("chatlist_path", chatListWallPath);
        editor.putString("video_path", chatVideoPath);
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
                        try { tempMap.put(Long.parseLong(parts[0].trim()), parts[1].trim()); } catch (Exception e) {}
                    }
                }
                s.close();
                customUserBadges.clear();
                customUserBadges.putAll(tempMap);
            } catch (Exception e) {
                FileLog.e("SoundGram Error: " + e.getMessage());
            }
        }).start();
    }
}
