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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class IconPackManager {

    private static IconPackManager Instance = null;
    private HashMap<String, String> currentIconMap = new HashMap<>();
    private HashMap<String, Drawable> drawableCache = new HashMap<>();
    private File currentPackDir;
    private ThreadLocal<Boolean> isFetchingOrig = new ThreadLocal<>();

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
        loadActivePack();
    }

    private void loadActivePack() {
        currentIconMap.clear();
        drawableCache.clear();
        currentPackDir = null;

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
                                currentIconMap.put(key, val);
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e("IconPackManager: Failed to load active pack", e);
                    }
                }
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
                String lower = fname.toLowerCase(java.util.Locale.ROOT);
                if (lower.endsWith(".png") || lower.endsWith(".svg") || lower.endsWith(".webp")) {
                    addIconMapping(fname, relPath);
                    addIconMapping(lower, relPath);
                    int dot = fname.lastIndexOf('.');
                    if (dot > 0) {
                        String base = fname.substring(0, dot);
                        addIconVariants(base, relPath);
                        addIconVariants(base.toLowerCase(java.util.Locale.ROOT), relPath);
                    }
                }
            }
        }
    }

    private void addIconMapping(String key, String relPath) {
        if (key == null || key.isEmpty()) return;
        currentIconMap.putIfAbsent(key, relPath);
    }

    private void addIconVariants(String base, String relPath) {
        if (base == null || base.isEmpty()) return;
        addIconMapping(base, relPath);

        // Normalize dashes and underscores
        if (base.contains("-")) {
            addIconMapping(base.replace('-', '_'), relPath);
        }
        if (base.contains("_")) {
            addIconMapping(base.replace('_', '-'), relPath);
        }

        // Strip common prefixes
        String[] prefixes = {"ic_", "ic-", "icon_", "icon-", "msg_", "menu_", "chats_"};
        String strippedPrefix = base;
        for (String p : prefixes) {
            if (strippedPrefix.startsWith(p)) {
                strippedPrefix = strippedPrefix.substring(p.length());
                addIconMapping(strippedPrefix, relPath);
                if (strippedPrefix.contains("-")) addIconMapping(strippedPrefix.replace('-', '_'), relPath);
                if (strippedPrefix.contains("_")) addIconMapping(strippedPrefix.replace('_', '-'), relPath);
                break;
            }
        }

        // Add all bidirectional aliases so Telegram resource names match pack icons immediately
        for (String fb : getFallbackNames(base)) {
            addIconMapping(fb, relPath);
        }
        if (!strippedPrefix.equals(base)) {
            for (String fb : getFallbackNames(strippedPrefix)) {
                addIconMapping(fb, relPath);
            }
        }
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
        String lower = name.toLowerCase(java.util.Locale.ROOT);
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
            if (name.contains("_")) {
                val = currentIconMap.get(name.replace('_', '-') + ext);
                if (val != null) return val;
                val = currentIconMap.get(lower.replace('_', '-') + ext);
                if (val != null) return val;
            }
            if (name.contains("-")) {
                val = currentIconMap.get(name.replace('-', '_') + ext);
                if (val != null) return val;
                val = currentIconMap.get(lower.replace('-', '_') + ext);
                if (val != null) return val;
            }
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

    public Drawable getDrawable(String resourceName) {
        if (currentPackDir == null || resourceName == null) {
            return null;
        }
        if (Boolean.TRUE.equals(isFetchingOrig.get())) {
            return null;
        }

        // Fast memory cache check including negative cache (null value)
        if (drawableCache.containsKey(resourceName)) {
            Drawable cached = drawableCache.get(resourceName);
            if (cached != null) {
                Drawable.ConstantState cs = cached.getConstantState();
                return cs != null ? cs.newDrawable().mutate() : cached;
            }
            return null;
        }

        String lookupName = resourceName;
        if (lookupName.endsWith("_solar")) {
            lookupName = lookupName.substring(0, lookupName.length() - 6);
        }
        if ("proxy_on".equals(lookupName) || "proxy_on_solar".equals(resourceName)) {
            lookupName = "drawer_proxy_on";
        } else if ("proxy_off".equals(lookupName) || "proxy_off_solar".equals(resourceName)) {
            lookupName = "drawer_proxy_off";
        } else if ("ghost".equals(lookupName) || "ghost_solar".equals(resourceName)) {
            lookupName = "ghost_status";
        } else if ("ic_ab_search_solar".equals(resourceName)) {
            lookupName = "ic_ab_search";
        } else if ("msg_info_filled_solar".equals(resourceName)) {
            lookupName = "msg_info_filled";
        } else if ("msg_data_solar".equals(resourceName)) {
            lookupName = "msg_data";
        }

        String fileName = resolveFromMap(lookupName);
        if (fileName == null && !lookupName.equals(resourceName)) {
            fileName = resolveFromMap(resourceName);
        }
        if (fileName == null) {
            java.util.List<String> fallbacks = getFallbackNames(lookupName);
            for (String fallback : fallbacks) {
                fileName = resolveFromMap(fallback);
                if (fileName != null) {
                    break;
                }
            }
        }

        if (fileName == null) {
            try {
                int id = ApplicationLoader.applicationContext.getResources().getIdentifier(lookupName, "drawable", ApplicationLoader.applicationContext.getPackageName());
                if (id != 0) {
                    app.nimarkogram.messenger.icons.icon_replaces.SolarIconReplace solarReplace = new app.nimarkogram.messenger.icons.icon_replaces.SolarIconReplace();
                    int unwrapId = solarReplace.unwrap(id);
                    if (unwrapId != id && unwrapId != 0) {
                        String unwrapName = ApplicationLoader.applicationContext.getResources().getResourceEntryName(unwrapId);
                        fileName = resolveFromMap(unwrapName);
                    }
                    if (fileName == null) {
                        int solarId = solarReplace.wrap(id);
                        if (solarId != id && solarId != 0) {
                            String solarName = ApplicationLoader.applicationContext.getResources().getResourceEntryName(solarId);
                            fileName = resolveFromMap(solarName);
                        }
                    }
                }
            } catch (Exception ignore) {}
        }

        if (fileName == null) {
            drawableCache.put(resourceName, null);
            return null;
        }

        File imgFile = new File(currentPackDir, fileName);
        if (!imgFile.exists()) {
            drawableCache.put(resourceName, null);
            return null;
        }
        if (imgFile != null && imgFile.exists()) {
            try {
                int origW = -1;
                int origH = -1;
                try {
                    int id = ApplicationLoader.applicationContext.getResources().getIdentifier(resourceName, "drawable", ApplicationLoader.applicationContext.getPackageName());
                    if (id == 0) {
                        for (String fb : getFallbackNames(resourceName)) {
                            id = ApplicationLoader.applicationContext.getResources().getIdentifier(fb, "drawable", ApplicationLoader.applicationContext.getPackageName());
                            if (id != 0) break;
                        }
                    }
                    if (id != 0 && IconPackResources.apiBaseResources != null) {
                        isFetchingOrig.set(true);
                        try {
                            Drawable orig = IconPackResources.apiBaseResources.getDrawable(id);
                            if (orig != null) {
                                origW = orig.getIntrinsicWidth();
                                origH = orig.getIntrinsicHeight();
                            }
                        } finally {
                            isFetchingOrig.set(false);
                        }
                    }
                } catch (Exception ignore) {
                    isFetchingOrig.set(false);
                }

                if (origW <= 0) origW = AndroidUtilities.dp(24);
                if (origH <= 0) origH = AndroidUtilities.dp(24);

                Bitmap bitmap = null;
                if (imgFile.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".svg")) {
                    try (FileInputStream fis = new FileInputStream(imgFile)) {
                        com.caverock.androidsvg.SVG svg = com.caverock.androidsvg.SVG.getFromInputStream(fis);
                        int targetW = origW > 0 ? origW : AndroidUtilities.dp(24);
                        int targetH = origH > 0 ? origH : AndroidUtilities.dp(24);
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
                }

                if (bitmap != null) {
                    if (origW > 0 && origH > 0 && (bitmap.getWidth() != origW || bitmap.getHeight() != origH)) {
                        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, origW, origH, true);
                        if (scaled != bitmap) {
                            bitmap.recycle();
                            bitmap = scaled;
                        }
                    }

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
                String lower = entryName.toLowerCase(java.util.Locale.ROOT);
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

    private java.util.List<String> getFallbackNames(String name) {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (name == null || name.isEmpty()) {
            return list;
        }

        String base = name;
        String[] suffixes = {"_solar", "_remix", "_filled", "_round", "_outline", "_solid", "_colored", "2", "_24", "_dr", "_s", "_mini", "_active", "_pressed", "_fill", "_line", "_white", "_black"};
        for (String s : suffixes) {
            if (base.endsWith(s)) {
                base = base.substring(0, base.length() - s.length());
                if (!list.contains(base)) list.add(base);
            }
        }

        if (base.contains("arrowright")) list.add(base.replace("arrowright", "arrow_right"));
        if (base.contains("arrowleft")) list.add(base.replace("arrowleft", "arrow_left"));
        if (base.contains("arrowup")) list.add(base.replace("arrowup", "arrow_up"));
        if (base.contains("arrowdown")) list.add(base.replace("arrowdown", "arrow_down"));
        if (base.contains("arrow_right")) list.add(base.replace("arrow_right", "arrowright"));
        if (base.contains("arrow_left")) list.add(base.replace("arrow_left", "arrowleft"));

        String stripped = base;
        String[] prefixes = {"msg_", "menu_", "chats_", "ic_ab_", "ic_", "chats_filter_", "filter_", "msg2_", "input_", "profile_", "calls_", "calls_menu_", "attach_"};
        for (String p : prefixes) {
            if (stripped.startsWith(p)) {
                stripped = stripped.substring(p.length());
                break;
            }
        }

        if (!list.contains(stripped)) list.add(stripped);

        // Common aliases
        if ("settings".equals(stripped) || "preferences".equals(stripped)) { list.add("msg_settings"); list.add("menu_settings"); list.add("tab_settings"); list.add("settings_tab"); list.add("preferences"); list.add("settings"); list.add("gear"); list.add("cog"); }
        if ("calls".equals(stripped) || "call".equals(stripped) || "phone".equals(stripped)) { list.add("msg_calls"); list.add("menu_calls"); list.add("tab_calls"); list.add("calls_tab"); list.add("call"); list.add("calls"); list.add("phone"); list.add("profile_phone"); list.add("telephone"); }
        if ("folders".equals(stripped) || "folder".equals(stripped)) { list.add("msg_folders"); list.add("files_folder"); list.add("folder"); list.add("folders"); list.add("folder_open"); list.add("folder-open"); }
        if ("contacts".equals(stripped) || "contact".equals(stripped) || "users".equals(stripped) || "people".equals(stripped) || "user".equals(stripped)) { list.add("msg_contacts"); list.add("menu_contacts"); list.add("tab_contacts"); list.add("contacts_tab"); list.add("users"); list.add("contacts"); list.add("user"); list.add("people"); list.add("profile"); }
        if ("chats".equals(stripped) || "dialogs".equals(stripped) || "messages".equals(stripped) || "chat".equals(stripped) || "message".equals(stripped)) { list.add("tab_chats"); list.add("chats_tab"); list.add("msg_chats"); list.add("chats"); list.add("chat"); list.add("dialogs"); list.add("messages"); list.add("message"); list.add("bubble"); list.add("comment"); }
        if ("camera".equals(stripped)) { list.add("msg_camera"); list.add("camera_solar"); list.add("camera"); list.add("photo_camera"); }
        if ("secret".equals(stripped) || "privacy".equals(stripped) || "lock".equals(stripped) || "security".equals(stripped) || "shield".equals(stripped) || "permissions".equals(stripped)) { list.add("msg_secret"); list.add("msg_secret_solar"); list.add("msg_permissions"); list.add("list_secret"); list.add("lock"); list.add("privacy"); list.add("security"); list.add("shield"); list.add("msg_mini_lock2"); list.add("lock_outline"); list.add("lock-closed"); }
        if ("back".equals(stripped)) { list.add("ic_ab_back"); list.add("arrow_back"); list.add("ic_back"); list.add("arrow_left"); list.add("arrow-left"); list.add("chevron_left"); list.add("chevron-left"); }
        if ("search".equals(stripped) || "find".equals(stripped)) { list.add("ic_ab_search"); list.add("search"); list.add("outline_header_search"); list.add("ic_search"); list.add("magnifier"); list.add("find"); }
        if ("other".equals(stripped) || "more".equals(stripped) || "dots".equals(stripped) || "menu".equals(stripped)) { list.add("ic_ab_other"); list.add("menu_other"); list.add("more"); list.add("dots"); list.add("ic_more"); list.add("dots_vertical"); list.add("dots-vertical"); list.add("more_vert"); list.add("ellipsis"); }
        if ("share".equals(stripped)) { list.add("msg_share"); list.add("share"); list.add("share_arrow"); }
        if ("delete".equals(stripped) || "trash".equals(stripped) || "bin".equals(stripped)) { list.add("msg_delete"); list.add("delete"); list.add("trash"); list.add("bin"); list.add("remove"); }
        if ("edit".equals(stripped) || "pencil".equals(stripped) || "pen".equals(stripped)) { list.add("msg_edit"); list.add("msg_pencil"); list.add("edit"); list.add("pencil"); list.add("pen"); }
        if ("send".equals(stripped) || "plane".equals(stripped)) { list.add("attach_send"); list.add("msg_send"); list.add("send_plane_24"); list.add("send"); list.add("ic_send"); list.add("paper_plane"); list.add("paper-plane"); }
        if ("attach".equals(stripped) || "clip".equals(stripped) || "paperclip".equals(stripped)) { list.add("input_attach"); list.add("msg_input_attach2"); list.add("attach"); list.add("clip"); list.add("paperclip"); }
        if ("mic".equals(stripped) || "voice".equals(stripped) || "microphone".equals(stripped)) { list.add("input_mic"); list.add("msg_voice"); list.add("mic"); list.add("voice"); list.add("microphone"); }
        if ("video".equals(stripped) || "videocam".equals(stripped)) { list.add("input_video"); list.add("video"); list.add("videocam"); }
        if ("smile".equals(stripped) || "emoji".equals(stripped) || "sticker".equals(stripped) || "stickers".equals(stripped)) { list.add("input_smile"); list.add("smile"); list.add("emoji"); list.add("stickers"); list.add("sticker"); list.add("msg_stickers"); }
        if ("keyboard".equals(stripped)) { list.add("input_keyboard"); list.add("keyboard"); }
        if ("saved".equals(stripped) || "bookmarks".equals(stripped) || "bookmark".equals(stripped)) { list.add("chats_saved"); list.add("msg_saved"); list.add("saved"); list.add("msg_fave"); list.add("bookmarks"); list.add("bookmark"); list.add("star"); }
        if ("media".equals(stripped) || "photos".equals(stripped) || "photo".equals(stripped) || "gallery".equals(stripped) || "image".equals(stripped) || "images".equals(stripped)) { list.add("msg_media"); list.add("media"); list.add("photos"); list.add("photo"); list.add("gallery"); list.add("image"); list.add("images"); list.add("picture"); }
        if ("files".equals(stripped) || "file".equals(stripped) || "document".equals(stripped) || "docs".equals(stripped)) { list.add("msg_files"); list.add("msg_docs"); list.add("files"); list.add("file"); list.add("document"); list.add("doc"); }
        if ("music".equals(stripped) || "audio".equals(stripped) || "song".equals(stripped)) { list.add("msg_music"); list.add("music"); list.add("audio"); list.add("song"); list.add("sound"); list.add("note"); list.add("music_note"); }
        if ("archive".equals(stripped)) { list.add("chats_archive"); list.add("msg_archive"); list.add("archive"); list.add("box"); }
        if ("mute".equals(stripped) || "unmute".equals(stripped) || "bell".equals(stripped)) { list.add("list_mute"); list.add("list_unmute"); list.add("msg_mute"); list.add("msg_unmute"); list.add("mute"); list.add("unmute"); list.add("bell_off"); list.add("bell-off"); }
        if ("pin".equals(stripped) || "unpin".equals(stripped)) { list.add("list_pin"); list.add("msg_pin"); list.add("msg_unpin"); list.add("pin"); list.add("unpin"); list.add("pushpin"); }
        if ("notifications".equals(stripped) || "notification".equals(stripped) || "bell".equals(stripped)) { list.add("msg_notifications"); list.add("menu_notifications"); list.add("notifications"); list.add("notification"); list.add("bell"); }
        if ("theme".equals(stripped) || "night".equals(stripped) || "dark".equals(stripped) || "palette".equals(stripped)) { list.add("msg_theme"); list.add("msg_palette"); list.add("menu_night"); list.add("night"); list.add("moon"); list.add("palette"); list.add("theme"); }
        if ("qrcode".equals(stripped) || "qr".equals(stripped)) { list.add("header_qr_24"); list.add("msg_qrcode"); list.add("qr"); list.add("qrcode"); }
        if ("check".equals(stripped) || "halfcheck".equals(stripped) || "verified".equals(stripped) || "tick".equals(stripped) || "done".equals(stripped)) {
            list.add("list_check"); list.add("list_halfcheck"); list.add("msg_check"); list.add("msg_halfcheck"); list.add("msg_check_w"); list.add("msg_halfcheck_w");
            list.add("verified_check"); list.add("check"); list.add("double_check"); list.add("double-check"); list.add("check_all"); list.add("check-all"); list.add("tick"); list.add("done"); list.add("done_all"); list.add("read");
        }
        if ("newgroup".equals(stripped) || "group".equals(stripped)) { list.add("menu_newgroup"); list.add("newgroup"); list.add("new_group"); list.add("group_add"); list.add("group"); }
        if ("channel".equals(stripped)) { list.add("menu_channel"); list.add("channel"); list.add("new_channel"); list.add("megaphone"); list.add("broadcast"); }
        if ("info".equals(stripped) || "about".equals(stripped)) { list.add("msg_info"); list.add("msg_info_solar"); list.add("msg_info_filled"); list.add("menu_about"); list.add("info"); list.add("about"); }
        if ("download".equals(stripped)) { list.add("msg_download"); list.add("msg_mini_download"); list.add("download"); list.add("arrow_down"); }
        if ("upload".equals(stripped)) { list.add("msg_upload"); list.add("msg_mini_upload"); list.add("upload"); list.add("arrow_up"); }
        if ("copy".equals(stripped)) { list.add("msg_copy"); list.add("copy"); list.add("duplicate"); }
        if ("forward".equals(stripped)) { list.add("msg_forward"); list.add("msg_forward_solar"); list.add("forward"); list.add("arrow_forward"); }
        if ("reply".equals(stripped)) { list.add("msg_reply"); list.add("msg_reply_solar"); list.add("reply"); }
        if ("retry".equals(stripped) || "refresh".equals(stripped) || "sync".equals(stripped)) { list.add("msg_retry"); list.add("msg_retry_solar"); list.add("retry"); list.add("refresh"); list.add("sync"); list.add("reload"); }
        if ("view".equals(stripped)) { list.add("msg_view_file"); list.add("msg_view"); list.add("view"); list.add("eye"); }
        if ("log".equals(stripped) || "history".equals(stripped)) { list.add("msg_log"); list.add("msg_history"); list.add("history"); }

        // Language / Globe
        if ("language".equals(stripped) || "globe".equals(stripped) || "translate".equals(stripped)) {
            list.add("msg_language"); list.add("language"); list.add("globe"); list.add("translate"); list.add("web"); list.add("intl");
        }
        // Devices / Sessions
        if ("devices".equals(stripped) || "device".equals(stripped) || "sessions".equals(stripped)) {
            list.add("msg_devices"); list.add("devices"); list.add("device"); list.add("sessions"); list.add("laptop"); list.add("computer"); list.add("desktop");
        }
        // Storage / Data
        if ("data".equals(stripped) || "storage".equals(stripped) || "database".equals(stripped)) {
            list.add("msg_data"); list.add("msg_data_solar"); list.add("data"); list.add("storage"); list.add("database"); list.add("disk"); list.add("server");
        }
        // Premium / Stars
        if ("premium".equals(stripped) || "star".equals(stripped)) {
            list.add("msg_premium"); list.add("premium"); list.add("star"); list.add("vip"); list.add("crown");
        }
        // Boost / Rocket
        if ("boost".equals(stripped) || "rocket".equals(stripped)) {
            list.add("msg_boost"); list.add("boost"); list.add("rocket"); list.add("lightning");
        }
        // Gift
        if ("gift".equals(stripped)) {
            list.add("msg_gift"); list.add("gift"); list.add("present");
        }
        // Clock / Timer
        if ("clock".equals(stripped) || "timer".equals(stripped) || "time".equals(stripped)) {
            list.add("msg_clock"); list.add("clock"); list.add("timer"); list.add("time"); list.add("watch");
        }
        // Warning / Error
        if ("warning".equals(stripped) || "alert".equals(stripped) || "error".equals(stripped)) {
            list.add("msg_warning"); list.add("warning"); list.add("alert"); list.add("error");
        }
        // Location / Map
        if ("location".equals(stripped) || "map".equals(stripped)) {
            list.add("msg_location"); list.add("location"); list.add("map"); list.add("pin_drop");
        }
        // Link / Chain
        if ("link".equals(stripped) || "chain".equals(stripped)) {
            list.add("msg_link"); list.add("msg_link_2"); list.add("link"); list.add("chain");
        }
        // Audio Player controls
        if ("play".equals(stripped) || "play_arrow".equals(stripped)) {
            list.add("play"); list.add("play_arrow"); list.add("play_circle"); list.add("player_play");
        }
        if ("pause".equals(stripped)) {
            list.add("pause"); list.add("pause_circle"); list.add("player_pause");
        }
        if ("next".equals(stripped) || "skip_next".equals(stripped)) {
            list.add("next"); list.add("skip_next"); list.add("player_next");
        }
        if ("prev".equals(stripped) || "previous".equals(stripped) || "skip_previous".equals(stripped)) {
            list.add("prev"); list.add("previous"); list.add("skip_previous"); list.add("player_prev");
        }
        if ("shuffle".equals(stripped) || "random".equals(stripped)) {
            list.add("shuffle"); list.add("random"); list.add("player_shuffle");
        }
        if ("repeat".equals(stripped) || "loop".equals(stripped)) {
            list.add("repeat"); list.add("loop"); list.add("player_repeat");
        }
        if ("volume".equals(stripped) || "speaker".equals(stripped)) {
            list.add("volume"); list.add("speaker"); list.add("sound");
        }
        if ("equalizer".equals(stripped) || "tune".equals(stripped)) {
            list.add("equalizer"); list.add("sliders"); list.add("tune");
        }

        for (String p : prefixes) {
            String candidate = p + stripped;
            if (!list.contains(candidate)) list.add(candidate);
            if (!list.contains(candidate + "_solar")) list.add(candidate + "_solar");
            if (!list.contains(candidate + "2")) list.add(candidate + "2");
            if (!list.contains(candidate + "_filled")) list.add(candidate + "_filled");
        }

        return list;
    }
}
