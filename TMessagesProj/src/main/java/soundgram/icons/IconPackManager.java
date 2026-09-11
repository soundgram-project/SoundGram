package soundgram.icons;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import org.json.JSONObject;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class IconPackManager {

    private static volatile IconPackManager Instance = null;
    private final HashMap<String, String> currentIconMap = new HashMap<>();
    private final HashMap<String, Drawable> drawableCache = new HashMap<>();
    private File currentPackDir;
    private final ThreadLocal<Boolean> isFetchingOrig = new ThreadLocal<>();

    private static final HashMap<String, String[]> ALIASES = new HashMap<>();
    private static void addAliasBidirectional(String... items) {
        if (items == null || items.length <= 1) return;
        for (String item : items) {
            ArrayList<String> rest = new ArrayList<>();
            for (String other : items) {
                if (!other.equals(item)) rest.add(other);
            }
            ALIASES.put(item, rest.toArray(new String[0]));
        }
    }

    static {
        // Proxy
        addAliasBidirectional("proxy_on", "drawer_proxy_on", "proxy_on_solar", "menu_proxy", "proxy_check");
        addAliasBidirectional("proxy_off", "drawer_proxy_off", "proxy_off_solar");

        // Ghost
        addAliasBidirectional("ghost", "ghost_status", "ghost_solar", "ayu_ghost", "ayu_ghost_solar", "ayu_ghost_crossed");

        // Delete / Trash
        addAliasBidirectional("msg_delete", "delete", "ayu_trash_bin", "ayu_trash_bin_preview", "msg_delete_solar", "msg_delete_24_solar");

        // Close / Cross
        addAliasBidirectional("msg_close", "close", "ayu_cross", "ayu_cross_preview", "ic_ab_close", "ic_close_white", "ic_close", "msg_cancel", "pip_close", "pip_video_close", "miniplayer_close");

        // Secret / Lock / Spy
        addAliasBidirectional("msg_secret", "ayu_eye_crossed", "ayu_eye_crossed_preview", "msg_unseen", "msg_secret_solar", "list_secret", "msg_mini_lock2", "ic_lock_header", "msg2_secret", "outline_header_lock_24");

        // Block
        addAliasBidirectional("msg_block", "block", "msg2_block2", "msg_block2", "msg_block_solar", "msg_filled_blocked");

        // Edit / Pencil / FAB Compose
        addAliasBidirectional("msg_edit", "pencil", "mini_checklist_edited", "msg_edit_solar", "msg_edit_24", "filled_fab_compose_32", "floating_pencil", "edit_pencil", "intro_ic_pencil", "fab_compose", "write_pencil");

        // Boost
        addAliasBidirectional("msg_boost", "boosts", "msg_boost_solar", "large_boosts", "filled_boost_plus");

        // Bot / AI
        addAliasBidirectional("msg_bot", "ai_chat", "msg_bots", "msg_bots_solar", "filter_bots", "input_bot1", "input_bot2");

        // Bottom Navigation Tabs
        addAliasBidirectional("msg_chats", "tabs_chats_24", "tabs_chats", "tab_chats", "tab_chats_24", "chats_tab", "chats");
        addAliasBidirectional("tabs_chats_active_24", "tabs_chats_active", "tab_chats_active", "tab_chats_active_24", "tab_chats_filled", "tab_chats_selected", "tabs_chats_filled", "tabs_chats_selected");
        addAliasBidirectional("msg_calls", "tabs_calls_24", "tabs_calls", "tab_calls", "tab_calls_24", "calls_tab", "calls", "calls_call");
        addAliasBidirectional("tabs_calls_active_24", "tabs_calls_active", "tab_calls_active", "tab_calls_active_24", "tab_calls_filled", "tab_calls_selected", "tabs_calls_filled", "tabs_calls_selected");
        addAliasBidirectional("msg_contacts", "tabs_contacts_24", "tabs_contact_24", "tabs_contacts", "tabs_contact", "tab_contacts", "tab_contact", "contacts_tab", "contacts", "profile_list");
        addAliasBidirectional("tabs_contact_active_24", "tabs_contacts_active_24", "tabs_contacts_active", "tabs_contact_active", "tab_contacts_active", "tab_contact_active", "tab_contacts_filled", "tab_contact_filled", "tab_contacts_selected", "tab_contact_selected");
        addAliasBidirectional("outline_profile_settings", "msg_settings", "menu_settings", "tab_settings", "tab_settings_24", "tabs_settings_24", "tabs_settings", "settings", "smiles_tab_settings");
        addAliasBidirectional("filled_profile_settings", "msg_settings_old", "tab_settings_active", "tabs_settings_active", "tab_settings_active_24", "tabs_settings_active_24", "tab_settings_filled", "tabs_settings_filled", "tab_settings_selected", "tabs_settings_selected", "settings_active", "settings_filled", "menu_settings_active", "menu_settings_filled");
        addAliasBidirectional("msg_music", "tabs_music", "tab_music", "tabs_music_24", "tab_music_24", "menu_music", "music", "music_note", "headphones", "audio", "tab_audio", "tabs_audio", "soundgram_music", "soundgram", "tab_soundgram", "tabs_soundgram", "player", "music_tab", "filter_note", "settings_sounds", "files_music", "files_music_24", "search_music");
        addAliasBidirectional("tabs_music_active_24", "tabs_music_active", "tab_music_active", "tab_music_active_24", "tab_music_filled", "tabs_music_filled", "tab_music_selected", "tabs_music_selected", "music_active", "music_filled", "tab_audio_active", "tabs_audio_active", "audio_active", "audio_filled", "headphones_active", "headphones_filled", "music_note_active", "music_note_filled", "soundgram_active", "soundgram_filled", "filter_note_active", "filter_note_filled", "files_music_active");

        // Folder Tabs (singular / plural)
        addAliasBidirectional("filter_groups", "filter_group", "ayu_filter_groups", "msg_folders_groups");
        addAliasBidirectional("filter_channels", "filter_channel", "ayu_filter_channels", "msg_folders_channels");
        addAliasBidirectional("filter_all", "ayu_filter_all");

        // Search
        addAliasBidirectional("outline_header_search", "ic_ab_search", "menu_browser_search", "search", "msg_search", "msg_search_solar", "menu_search", "baseline_search_24");

        // Checkmarks: DOUBLE check
        addAliasBidirectional("msg_check", "msg_check_s", "round_check", "round_check2", "checkbig", "check_big", "list_check", "done", "msg_check_solar", "floating_check", "ic_ab_done", "account_check");
        // Checkmarks: SINGLE check
        addAliasBidirectional("msg_halfcheck", "msg_halfcheck_s", "list_halfcheck", "msg_halfcheck_solar");

        // Pins
        addAliasBidirectional("msg_pin", "chats_pin", "list_pin", "msg_pin_solar", "msg_pin_mini", "msg_pin_filled", "msg_pinnedlist");
        addAliasBidirectional("msg_unpin", "chats_unpin", "msg_unpin_solar");

        // Navigation
        addAliasBidirectional("msg_arrowright", "arrow_newchat", "msg_arrowright_solar", "arrow_more", "ic_mtrl_arrow_circle");
        addAliasBidirectional("msg_arrow_back", "msg_to_beginning", "ic_ab_back", "ic_ab_back_solar", "arrow_back", "preview_back", "msg_photo_back");
        addAliasBidirectional("ic_ab_other", "msg_actions", "preview_dots", "media_more");

        // Camera
        addAliasBidirectional("msg_camera", "camera", "msg_rear_camera", "camera_solar", "filled_premium_camera");

        // Info
        addAliasBidirectional("msg_info", "msg_info_filled", "filled_info", "msg_info_solar");

        // Data & Storage
        addAliasBidirectional("msg_data", "msg2_data", "msg_data_solar", "settings_data");

        // Share & Views
        addAliasBidirectional("msg_share", "filled_share", "outline_share_out_24", "msg_share_solar", "msg_shareout", "share", "action_share", "filled_button_share");
        addAliasBidirectional("msg_views", "filled_views", "msg_views_solar", "icon_views", "channel_views", "eye", "outline_eye_24");

        // Gift & Link
        addAliasBidirectional("msg_gift", "filled_gift_premium", "msg_gift_solar", "filled_gift_simple", "gift", "settings_gift");
        addAliasBidirectional("msg_link", "filled_link", "msg_link_solar", "msg_link2", "msg2_link2", "tabs_link_24");

        // TTL / Burn
        addAliasBidirectional("msg_autodelete", "msg2_autodelete", "ttl_burn", "ttl_burnt", "filled_fire", "burn_solar", "ttl_oneview");

        // Send & Input
        addAliasBidirectional("send_plane_24", "send_plane_26", "send_outline", "attach_send", "ic_send", "msg_send", "send_extera_24");
        addAliasBidirectional("input_attach", "msg_input_attach2", "ayu_input_attach", "mini_attach", "nav_edit_attach");

        // Reactions & Mentions
        addAliasBidirectional("reactionbutton", "menu_reactions", "msg_reactions", "msg_reactions2", "msg_reactions_filled", "filled_chatlist_reaction");
        addAliasBidirectional("mentionbutton", "filled_chatlist_mention");

        // Reply & Forward
        addAliasBidirectional("msg_reply", "menu_reply", "msg_reply_small", "input_reply", "filled_button_reply");
        addAliasBidirectional("msg_forward", "input_forward", "filled_forward");

        // Saved Messages & Archive
        addAliasBidirectional("chats_saved", "msg_saved", "menu_saved", "outline_saved_24");
        addAliasBidirectional("chats_archive", "msg_archive", "large_archive", "msg2_archived_stickers");

        // Audio player
        addAliasBidirectional("player_new_repeatall", "player_repeat", "miniplayer_repeat", "msg_repeat");
        addAliasBidirectional("player_new_repeatone", "player_repeat1");
        addAliasBidirectional("player_new_shuffle", "player_shuffle");
        addAliasBidirectional("player_new_order", "player_order");

        // Crop / draw
        addAliasBidirectional("media_crop", "msg_photo_crop");

        // icon_ prefix aliases (some icon packs use this naming convention)
        addAliasBidirectional("icon_settings", "msg_settings");
        addAliasBidirectional("icon_back", "ic_ab_back");
        addAliasBidirectional("icon_search", "ic_ab_search");
        addAliasBidirectional("icon_close", "msg_close");
        addAliasBidirectional("icon_delete", "msg_delete");
        addAliasBidirectional("icon_edit", "msg_edit");
        addAliasBidirectional("icon_share", "msg_share");
        addAliasBidirectional("icon_pin", "msg_pin");
        addAliasBidirectional("icon_forward", "msg_forward");
        addAliasBidirectional("icon_reply", "msg_reply");
        addAliasBidirectional("icon_mute", "msg_mute");
        addAliasBidirectional("icon_add", "msg_add");
        addAliasBidirectional("icon_check", "msg_check");
        addAliasBidirectional("icon_info", "msg_info");
        addAliasBidirectional("icon_camera", "msg_camera");
        addAliasBidirectional("icon_call", "msg_calls");
        addAliasBidirectional("icon_download", "msg_download");
        addAliasBidirectional("icon_photo", "msg_photos");
        addAliasBidirectional("icon_video", "msg_video");
        addAliasBidirectional("icon_file", "msg_files");
        addAliasBidirectional("icon_link", "msg_link");
        addAliasBidirectional("icon_bot", "msg_bot");
        addAliasBidirectional("icon_folder", "msg_folder");
        addAliasBidirectional("icon_sticker", "msg_stickers");
    }

    /**
     * Защищённые ключи иконок: значения этих ключей не могут быть переписаны
     * файлами начинающимися с settings_ или settings.
     */
    private static final java.util.Set<String> VIEWS_PROTECTED_KEYS = new java.util.HashSet<>(java.util.Arrays.asList(
            "msg_views", "msg_views_solar", "filled_views", "icon_views", "channel_views", "eye",
            "msg_data", "msg_data_solar", "settings_data", "msg2_data",
            "msg_calls", "msg_calls_solar"
    ));

    private static boolean isSettingsFile(String relPath) {
        if (relPath == null) return false;
        String lower = relPath.toLowerCase(java.util.Locale.ROOT);
        String fname = lower.contains("/") ? lower.substring(lower.lastIndexOf('/') + 1) : lower;
        return fname.startsWith("settings_") || fname.equals("settings.png")
                || fname.equals("settings.svg") || fname.equals("settings.webp");
    }

    public static IconPackManager getInstance() {
        if (Instance == null) {
            synchronized (IconPackManager.class) {
                if (Instance == null) {
                    Instance = new IconPackManager();
                }
            }
        }
        return Instance;
    }

    public IconPackManager() {
        Instance = this;
        loadActivePack();
    }

    public Boolean isFetchingOrig() {
        return isFetchingOrig.get();
    }

    private void loadActivePack() {
        currentIconMap.clear();
        drawableCache.clear();
        currentPackDir = null;

        if (ApplicationLoader.applicationContext == null) {
            return;
        }

        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("iconpacks", Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("enabled", true);
        String activePackId = prefs.getString("active_pack", null);

        if (enabled && activePackId != null) {
            File packDir = new File(getIconPacksDir(), activePackId);
            if (packDir.exists() && packDir.isDirectory()) {
                currentPackDir = packDir;
                scanPackFiles(packDir, "");
                File metadataFile = new File(packDir, "metadata.json");
                if (metadataFile.exists()) {
                    try {
                        String jsonStr = readFileToString(metadataFile);
                        JSONObject json = new JSONObject(jsonStr);
                        JSONObject icons = json.optJSONObject("icons");
                        if (icons != null) {
                            Iterator<String> keys = icons.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                String val = icons.getString(key);
                                File target = new File(packDir, val);
                                if (!target.exists()) {
                                    String alt = currentIconMap.get(val);
                                    if (alt != null && new File(packDir, alt).exists()) {
                                        val = alt;
                                    }
                                }
                                registerIconKey(key, val);
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e("IconPackManager: Failed to load active pack", e);
                    }
                }
            }
        }
    }

    private void registerIconKey(String key, String val) {
        if (key == null || key.isEmpty() || val == null || val.isEmpty()) return;
        addIconMapping(key, val);
        String lowerKey = key.toLowerCase(Locale.ROOT);
        if (!lowerKey.equals(key)) {
            addIconMapping(lowerKey, val);
        }
        if (key.contains("-")) {
            addIconMapping(key.replace('-', '_'), val);
        }
        if (key.contains("_")) {
            addIconMapping(key.replace('_', '-'), val);
        }
        if (key.endsWith(".png") || key.endsWith(".svg") || key.endsWith(".webp")) {
            int dot = key.lastIndexOf('.');
            String base = key.substring(0, dot);
            addIconMapping(base, val);
            addIconMapping(base.toLowerCase(Locale.ROOT), val);
        }

        String[] suffixes = {
            "_solar", "_remix", "_liquid", "_plumpy", "_outline",
            "_round", "_solid", "_colored", "_24", "_32", "_16", "_20",
            "_s", "_w", "_b", "_hw", "_ny", "_mini", "_small", "_large"
        };
        String stripped = key;
        for (int round = 0; round < 3; round++) {
            boolean changed = false;
            for (String s : suffixes) {
                if (stripped.endsWith(s)) {
                    stripped = stripped.substring(0, stripped.length() - s.length());
                    addIconMapping(stripped, val);
                    addIconMapping(stripped.toLowerCase(Locale.ROOT), val);
                    changed = true;
                    break;
                }
            }
            if (!changed) break;
        }

        if (key.endsWith("s") && key.length() > 3) {
            addIconMapping(key.substring(0, key.length() - 1), val);
        } else if (!key.endsWith("s")) {
            addIconMapping(key + "s", val);
        }

        if (key.startsWith("tabs_")) {
            addIconMapping("tab_" + key.substring(5), val);
        } else if (key.startsWith("tab_")) {
            addIconMapping("tabs_" + key.substring(4), val);
        }

        if (key.startsWith("filled_")) {
            addIconMapping(key.substring(7), val);
        } else if (key.startsWith("outline_")) {
            addIconMapping(key.substring(8), val);
        }

        if (key.startsWith("msg2_")) {
            addIconMapping("msg_" + key.substring(5), val);
        } else if (key.startsWith("msg_")) {
            addIconMapping("msg2_" + key.substring(4), val);
            addIconMapping("menu_" + key.substring(4), val);
        } else if (key.startsWith("menu_")) {
            addIconMapping("msg_" + key.substring(5), val);
        } else if (key.startsWith("settings_")) {
            // Безопасный маппинг settings_X → msg_X только для явно известных иконок настроек.
            // Wildcard-маппинг убран: он ломал msg_views, msg_data и другие иконки,
            // если иконпак содержал файлы settings_views.png, settings_data.png и т.д.
            String suffix = key.substring(9); // часть после "settings_"
            // Только иконки, которые реально относятся к настройкам:
            if (suffix.equals("arrow") || suffix.equals("account") || suffix.equals("ask")
                    || suffix.equals("business") || suffix.equals("calls") || suffix.equals("filters")
                    || suffix.equals("language") || suffix.equals("notifications")
                    || suffix.equals("passlock") || suffix.equals("premium")
                    || suffix.equals("privacy") || suffix.equals("stickers")
                    || suffix.equals("storage") || suffix.equals("themes")
                    || suffix.equals("main") || suffix.equals("general")) {
                addIconMapping("msg_" + suffix, val);
                addIconMapping("menu_" + suffix, val);
            }
            // Для "settings" как самостоятельного имени — маппим только на msg_settings
            // (через ALIASES это уже происходит автоматически)
        }

        String[] aliases = ALIASES.get(key);
        if (aliases != null) {
            for (String alias : aliases) {
                addIconMapping(alias, val);
            }
        }
    }

    private void scanPackFiles(File dir, String relPrefix) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                scanPackFiles(f, (relPrefix.isEmpty() ? "" : relPrefix + "/") + f.getName());
            } else {
                String fname = f.getName();
                String relPath = (relPrefix.isEmpty() ? "" : relPrefix + "/") + fname;
                String lower = fname.toLowerCase(Locale.ROOT);
                if (lower.endsWith(".png") || lower.endsWith(".svg") || lower.endsWith(".webp")) {
                    addIconMapping(fname, relPath);
                    addIconMapping(lower, relPath);
                    int dot = fname.lastIndexOf('.');
                    if (dot > 0) {
                        String base = fname.substring(0, dot);
                        boolean isPureDigits = true;
                        for (int i = 0; i < base.length(); i++) {
                            if (!Character.isDigit(base.charAt(i))) {
                                isPureDigits = false;
                                break;
                            }
                        }
                        if (!isPureDigits) {
                            registerIconKey(base, relPath);
                        }
                    }
                }
            }
        }
    }

    private void addIconMapping(String key, String relPath) {
        if (key == null || key.isEmpty() || relPath == null || relPath.isEmpty()) return;
        // Защита от неправильного маппинга: иконки просмотров и данных не должны маппиться на файлы settings_*
        String lowerKey = key.toLowerCase(Locale.ROOT);
        if (VIEWS_PROTECTED_KEYS.contains(lowerKey) && isSettingsFile(relPath)) {
            FileLog.d("IconPackManager: blocked settings-file mapping for protected key '" + key + "' -> '" + relPath + "'");
            return;
        }
        currentIconMap.putIfAbsent(key, relPath);
    }

    public boolean hasActivePack() {
        return currentPackDir != null;
    }

    public String getActivePackId() {
        if (currentPackDir == null) {
            return null;
        }
        return currentPackDir.getName();
    }

    private String resolveFromMap(String name) {
        if (name == null || name.isEmpty()) return null;
        String val = currentIconMap.get(name);
        if (val != null) return val;
        String lower = name.toLowerCase(Locale.ROOT);
        val = currentIconMap.get(lower);
        if (val != null) return val;

        if (name.contains("_")) {
            val = currentIconMap.get(name.replace('_', '-'));
            if (val != null) return val;
            val = currentIconMap.get(lower.replace('_', '-'));
            if (val != null) return val;
        }
        if (name.contains("-")) {
            val = currentIconMap.get(name.replace('-', '_'));
            if (val != null) return val;
            val = currentIconMap.get(lower.replace('-', '_'));
            if (val != null) return val;
        }

        String[] exts = {".png", ".svg", ".webp"};
        for (String ext : exts) {
            val = currentIconMap.get(name + ext);
            if (val != null) return val;
            val = currentIconMap.get(lower + ext);
            if (val != null) return val;
        }

        if (name.endsWith(".png") || name.endsWith(".svg") || name.endsWith(".webp")) {
            int dot = name.lastIndexOf('.');
            val = currentIconMap.get(name.substring(0, dot));
            if (val != null) return val;
            val = currentIconMap.get(lower.substring(0, dot));
            if (val != null) return val;
        }
        return null;
    }

    private String resolveIconFileName(String resourceName) {
        if (resourceName == null || resourceName.isEmpty()) return null;

        String fileName = resolveFromMap(resourceName);
        if (fileName != null) return fileName;

        String[] aliases = ALIASES.get(resourceName);
        if (aliases != null) {
            for (String alias : aliases) {
                fileName = resolveFromMap(alias);
                if (fileName != null) return fileName;
            }
        }

        String stripped = resourceName;
        String[] suffixes = {
            "_solar", "_remix", "_liquid", "_plumpy", "_filled", "_outline",
            "_round", "_solid", "_colored", "_24", "_32", "_16", "_20",
            "_s", "_w", "_b", "_hw", "_ny", "_mini", "_small", "_large",
            "_active", "_selected"
        };
        for (int round = 0; round < 3; round++) {
            boolean changed = false;
            for (String s : suffixes) {
                if (stripped.endsWith(s)) {
                    stripped = stripped.substring(0, stripped.length() - s.length());
                    fileName = resolveFromMap(stripped);
                    if (fileName != null) return fileName;
                    String[] strippedAliases = ALIASES.get(stripped);
                    if (strippedAliases != null) {
                        for (String a : strippedAliases) {
                            fileName = resolveFromMap(a);
                            if (fileName != null) return fileName;
                        }
                    }
                    changed = true;
                    break;
                }
            }
            if (!changed) break;
        }

        if (resourceName.startsWith("msg2_")) {
            fileName = resolveFromMap("msg_" + resourceName.substring(5));
            if (fileName != null) return fileName;
        } else if (resourceName.startsWith("msg_")) {
            fileName = resolveFromMap("msg2_" + resourceName.substring(4));
            if (fileName != null) return fileName;
            fileName = resolveFromMap("menu_" + resourceName.substring(4));
            if (fileName != null) return fileName;
        } else if (resourceName.startsWith("menu_")) {
            fileName = resolveFromMap("msg_" + resourceName.substring(5));
            if (fileName != null) return fileName;
        }

        if (resourceName.startsWith("tabs_")) {
            fileName = resolveFromMap("tab_" + resourceName.substring(5));
            if (fileName != null) return fileName;
        } else if (resourceName.startsWith("tab_")) {
            fileName = resolveFromMap("tabs_" + resourceName.substring(4));
            if (fileName != null) return fileName;
        }

        if (resourceName.startsWith("filled_")) {
            fileName = resolveFromMap(resourceName.substring(7));
            if (fileName != null) return fileName;
        } else if (resourceName.startsWith("outline_")) {
            fileName = resolveFromMap(resourceName.substring(8));
            if (fileName != null) return fileName;
        } else if (resourceName.startsWith("ic_ab_")) {
            fileName = resolveFromMap("msg_" + resourceName.substring(6));
            if (fileName != null) return fileName;
        }

        if (resourceName.endsWith("s") && resourceName.length() > 3) {
            String sing = resourceName.substring(0, resourceName.length() - 1);
            fileName = resolveFromMap(sing);
            if (fileName != null) return fileName;
            String[] singAliases = ALIASES.get(sing);
            if (singAliases != null) {
                for (String a : singAliases) {
                    fileName = resolveFromMap(a);
                    if (fileName != null) return fileName;
                }
            }
        }

        String[] tryAdd = {"_24", "_solar", "_s"};
        for (String add : tryAdd) {
            fileName = resolveFromMap(resourceName + add);
            if (fileName != null) return fileName;
        }

        return null;
    }

    public Drawable getDrawable(String resourceName) {
        if (currentPackDir == null || resourceName == null) {
            return null;
        }
        if (Boolean.TRUE.equals(isFetchingOrig.get())) {
            return null;
        }

        if (drawableCache.containsKey(resourceName)) {
            Drawable cached = drawableCache.get(resourceName);
            if (cached != null) {
                Drawable.ConstantState cs = cached.getConstantState();
                return cs != null ? cs.newDrawable().mutate() : cached;
            }
            return null;
        }

        String fileName = resolveIconFileName(resourceName);

        if (fileName == null) {
            drawableCache.put(resourceName, null);
            return null;
        }

        File imgFile = new File(currentPackDir, fileName);
        if (!imgFile.exists()) {
            drawableCache.put(resourceName, null);
            return null;
        }

        try {
            int origW = 0;
            int origH = 0;
            try {
                isFetchingOrig.set(Boolean.TRUE);
                if (IconPackResources.apiBaseResources != null && ApplicationLoader.applicationContext != null) {
                    String pkg = ApplicationLoader.applicationContext.getPackageName();
                    int resId = IconPackResources.apiBaseResources.getIdentifier(resourceName, "drawable", pkg);
                    if (resId == 0) {
                        String[] aliases = ALIASES.get(resourceName);
                        if (aliases != null) {
                            for (String alias : aliases) {
                                resId = IconPackResources.apiBaseResources.getIdentifier(alias, "drawable", pkg);
                                if (resId != 0) break;
                            }
                        }
                    }
                    if (resId != 0) {
                        Drawable orig = IconPackResources.apiBaseResources.getDrawable(resId);
                        if (orig != null) {
                            origW = orig.getIntrinsicWidth();
                            origH = orig.getIntrinsicHeight();
                        }
                    }
                }
            } catch (Throwable ignore) {
            } finally {
                isFetchingOrig.set(Boolean.FALSE);
            }

            if (origW <= 0 || origH <= 0) {
                origW = AndroidUtilities.dp(24);
                origH = AndroidUtilities.dp(24);
            }

            Bitmap bitmap = null;
            if (imgFile.getName().toLowerCase(Locale.ROOT).endsWith(".svg")) {
                try (FileInputStream fis = new FileInputStream(imgFile)) {
                    com.caverock.androidsvg.SVG svg = com.caverock.androidsvg.SVG.getFromInputStream(fis);
                    int targetW = origW;
                    int targetH = origH;
                    if (svg.getDocumentViewBox() != null) {
                        svg.setDocumentWidth(targetW);
                        svg.setDocumentHeight(targetH);
                    } else {
                        float docW = svg.getDocumentWidth();
                        float docH = svg.getDocumentHeight();
                        if (docW > 0 && docH > 0) {
                            svg.setDocumentViewBox(0, 0, docW, docH);
                        }
                        svg.setDocumentWidth(targetW);
                        svg.setDocumentHeight(targetH);
                    }
                    bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
                    android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                    svg.renderToCanvas(canvas);
                } catch (Throwable e) {
                    FileLog.e("IconPackManager: Failed to render SVG " + imgFile, e);
                }
            } else {
                bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                if (bitmap != null && origW > 0 && origH > 0) {
                    if (bitmap.getWidth() != origW || bitmap.getHeight() != origH) {
                        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, origW, origH, true);
                        if (scaled != bitmap) {
                            bitmap.recycle();
                            bitmap = scaled;
                        }
                    }
                }
            }

            if (bitmap != null) {
                bitmap.setDensity(ApplicationLoader.applicationContext.getResources().getDisplayMetrics().densityDpi);
                Drawable d;
                if (shouldDisableTinting(bitmap)) {
                    d = new NonTintableDrawable(new BitmapDrawable(ApplicationLoader.applicationContext.getResources(), bitmap));
                } else {
                    d = new BitmapDrawable(ApplicationLoader.applicationContext.getResources(), bitmap);
                }
                drawableCache.put(resourceName, d);
                if (d.getConstantState() != null) {
                    return d.getConstantState().newDrawable().mutate();
                }
                return d;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }

        drawableCache.put(resourceName, null);
        return null;
    }

    public static File getIconPacksDir() {
        File dir = new File(ApplicationLoader.getFilesDirFixed(), "iconpacks");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static class IconPackInfo {
        public String packId;
        public String packName;
        public String author;
        public String version;
        public File path;

        public static IconPackInfo fromMetadata(File packDir) {
            File metadataFile = new File(packDir, "metadata.json");
            if (!metadataFile.exists()) {
                File[] files = packDir.listFiles();
                if (files != null && files.length > 0) {
                    IconPackInfo info = new IconPackInfo();
                    info.packId = packDir.getName();
                    info.packName = packDir.getName();
                    info.author = "Local";
                    info.version = "1.0";
                    info.path = packDir;
                    return info;
                }
                return null;
            }
            try {
                String jsonStr = readFileToString(metadataFile);
                JSONObject json = new JSONObject(jsonStr);
                IconPackInfo info = new IconPackInfo();
                info.packId = json.optString("packId", packDir.getName());
                info.packName = json.optString("packName", packDir.getName());
                info.author = json.optString("author", "Unknown");
                info.version = json.optString("version", "1.0");
                info.path = packDir;
                return info;
            } catch (Exception e) {
                FileLog.e(e);
            }
            return null;
        }
    }

    public static IconPackInfo parseZipMetadata(File zipFile) {
        boolean hasAnyIcons = false;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (entryName.equals("metadata.json") || entryName.endsWith("/metadata.json")) {
                    StringBuilder sb = new StringBuilder();
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = zis.read(buffer)) != -1) {
                        sb.append(new String(buffer, 0, read));
                    }
                    JSONObject json = new JSONObject(sb.toString());
                    IconPackInfo info = new IconPackInfo();
                    String cleanName = zipFile.getName().replace(".zip", "");
                    info.packId = json.optString("packId", cleanName);
                    info.packName = json.optString("packName", cleanName);
                    info.author = json.optString("author", "Unknown");
                    info.version = json.optString("version", "1.0");
                    return info;
                }
                String lower = entryName.toLowerCase(Locale.ROOT);
                if (lower.endsWith(".png") || lower.endsWith(".svg") || lower.endsWith(".webp")) {
                    hasAnyIcons = true;
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        if (hasAnyIcons) {
            IconPackInfo info = new IconPackInfo();
            String cleanName = zipFile.getName().replace(".zip", "");
            info.packId = cleanName;
            info.packName = cleanName;
            info.author = "Local";
            info.version = "1.0";
            return info;
        }
        return null;
    }

    public static boolean installIconPack(File zipFile) {
        IconPackInfo info = parseZipMetadata(zipFile);
        if (info == null) return false;

        File targetDir = new File(getIconPacksDir(), info.packId);
        if (targetDir.exists()) {
            deleteDirectory(targetDir);
        }
        targetDir.mkdirs();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[4096];
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                File outFile = new File(targetDir, entry.getName());
                outFile.getParentFile().mkdirs();
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    int read;
                    while ((read = zis.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    public static void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteDirectory(child);
                }
            }
        }
        dir.delete();
    }

    public static ArrayList<IconPackInfo> getInstalledPacks() {
        ArrayList<IconPackInfo> list = new ArrayList<>();
        File dir = getIconPacksDir();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    IconPackInfo info = IconPackInfo.fromMetadata(f);
                    if (info != null) {
                        list.add(info);
                    }
                }
            }
        }
        return list;
    }

    public static void setActivePack(String packId) {
        SharedPreferences.Editor editor = ApplicationLoader.applicationContext.getSharedPreferences("iconpacks", Context.MODE_PRIVATE).edit();
        if (packId == null) {
            editor.remove("active_pack");
        } else {
            editor.putString("active_pack", packId);
            editor.putBoolean("enabled", true);
        }
        editor.commit();

        getInstance().loadActivePack();
        app.nimarkogram.messenger.icons.NimarkoIconResources.clearAllCaches();
        ApplicationLoader.reloadAppIconResources();
        if (org.telegram.ui.LaunchActivity.instance != null) {
            org.telegram.ui.LaunchActivity.instance.reloadResources();
        }
        org.telegram.ui.ActionBar.Theme.reloadAllResources(ApplicationLoader.applicationContext);
        AndroidUtilities.runOnUIThread(() -> {
            org.telegram.messenger.NotificationCenter.getGlobalInstance().postNotificationName(
                    org.telegram.messenger.NotificationCenter.needSetDayNightTheme,
                    org.telegram.ui.ActionBar.Theme.getActiveTheme(),
                    org.telegram.ui.ActionBar.Theme.isCurrentThemeNight(),
                    null,
                    -1
            );
        });
    }

    private static String readFileToString(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return new String(data, "UTF-8");
        }
    }

    private static boolean shouldDisableTinting(Bitmap bitmap) {
        if (bitmap == null) return false;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        boolean hasColoredPixels = false;
        float[] firstHsv = null;
        boolean isMultiColor = false;
        
        int stepX = Math.max(1, width / 20);
        int stepY = Math.max(1, height / 20);
        
        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                int pixel = bitmap.getPixel(x, y);
                int alpha = (pixel >> 24) & 0xff;
                if (alpha > 30) {
                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >> 8) & 0xff;
                    int b = pixel & 0xff;
                    int chroma = Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
                    if (chroma > 30) {
                        hasColoredPixels = true;
                        float[] hsv = new float[3];
                        android.graphics.Color.colorToHSV(pixel, hsv);
                        if (firstHsv == null) {
                            firstHsv = hsv;
                        } else {
                            float diffHue = Math.abs(hsv[0] - firstHsv[0]);
                            if (diffHue > 180) diffHue = 360 - diffHue;
                            float diffSat = Math.abs(hsv[1] - firstHsv[1]);
                            if (diffHue > 35 || diffSat > 0.35f) {
                                isMultiColor = true;
                            }
                        }
                    }
                }
            }
        }
        return hasColoredPixels && isMultiColor;
    }

    public static class NonTintableDrawable extends Drawable {
        private final Drawable wrapped;

        public NonTintableDrawable(Drawable wrapped) {
            this.wrapped = wrapped;
            if (wrapped != null) {
                wrapped.setBounds(getBounds());
            }
        }

        @Override
        public void draw(android.graphics.Canvas canvas) {
            if (wrapped != null) {
                wrapped.draw(canvas);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            if (wrapped != null) {
                wrapped.setAlpha(alpha);
            }
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            // Ignore to prevent tinting
        }

        @Override
        public void setColorFilter(int color, android.graphics.PorterDuff.Mode mode) {
            // Ignore to prevent tinting
        }

        @Override
        public void setTint(int tintColor) {
            // Ignore to prevent tinting
        }

        @Override
        public void setTintList(android.content.res.ColorStateList tint) {
            // Ignore to prevent tinting
        }

        @Override
        public void setTintMode(android.graphics.PorterDuff.Mode tintMode) {
            // Ignore to prevent tinting
        }

        @Override
        public int getOpacity() {
            return wrapped != null ? wrapped.getOpacity() : android.graphics.PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return wrapped != null ? wrapped.getIntrinsicWidth() : -1;
        }

        @Override
        public int getIntrinsicHeight() {
            return wrapped != null ? wrapped.getIntrinsicHeight() : -1;
        }

        @Override
        public void setBounds(int left, int top, int right, int bottom) {
            super.setBounds(left, top, right, bottom);
            if (wrapped != null) {
                wrapped.setBounds(left, top, right, bottom);
            }
        }

        @Override
        public void setBounds(android.graphics.Rect bounds) {
            super.setBounds(bounds);
            if (wrapped != null) {
                wrapped.setBounds(bounds);
            }
        }

        @Override
        protected void onBoundsChange(android.graphics.Rect bounds) {
            super.onBoundsChange(bounds);
            if (wrapped != null) {
                wrapped.setBounds(bounds);
            }
        }

        @Override
        public boolean isStateful() {
            return wrapped != null && wrapped.isStateful();
        }

        @Override
        protected boolean onStateChange(int[] state) {
            if (wrapped != null) {
                return wrapped.setState(state);
            }
            return super.onStateChange(state);
        }

        @Override
        public Drawable.ConstantState getConstantState() {
            return new Drawable.ConstantState() {
                @Override
                public Drawable newDrawable() {
                    Drawable wrappedClone = null;
                    if (wrapped != null && wrapped.getConstantState() != null) {
                        wrappedClone = wrapped.getConstantState().newDrawable().mutate();
                    }
                    return new NonTintableDrawable(wrappedClone);
                }

                @Override
                public int getChangingConfigurations() {
                    return 0;
                }
            };
        }
    }
}
