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
                                currentIconMap.put(key, icons.getString(key));
                            }
                        }
                        currentPackDir = packDir;
                    } catch (Exception e) {
                        FileLog.e("IconPackManager: Failed to load active pack", e);
                    }
                }
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

    public Drawable getDrawable(String resourceName) {
        if (currentPackDir == null) {
            return null;
        }
        if (Boolean.TRUE.equals(isFetchingOrig.get())) {
            return null;
        }

        String lookupName = resourceName;
        if ("proxy_on_solar".equals(resourceName)) {
            lookupName = "drawer_proxy_on";
        } else if ("proxy_off_solar".equals(resourceName)) {
            lookupName = "drawer_proxy_off";
        } else if ("ghost_solar".equals(resourceName)) {
            lookupName = "ghost_status";
        } else if ("ic_ab_search_solar".equals(resourceName)) {
            lookupName = "ic_ab_search";
        } else if ("msg_info_filled_solar".equals(resourceName)) {
            lookupName = "msg_info_filled";
        } else if ("msg_data_solar".equals(resourceName)) {
            lookupName = "msg_data";
        }

        String fileName = currentIconMap.get(lookupName);
        if (fileName == null) {
            try {
                int id = ApplicationLoader.applicationContext.getResources().getIdentifier(lookupName, "drawable", ApplicationLoader.applicationContext.getPackageName());
                if (id != 0) {
                    int solarId = app.nimarkogram.messenger.utils.icons.icon_replaces.SolarIconReplace.INSTANCE.getConversion(id);
                    if (solarId != id) {
                        String solarName = IconPackResources.apiBaseResources.getResourceEntryName(solarId);
                        fileName = currentIconMap.get(solarName);
                    } else {
                        int origId = app.nimarkogram.messenger.utils.icons.icon_replaces.SolarIconReplace.INSTANCE.getReverseConversion(id);
                        if (origId != id) {
                            String origName = IconPackResources.apiBaseResources.getResourceEntryName(origId);
                            fileName = currentIconMap.get(origName);
                        }
                    }
                }
            } catch (Exception ignore) {}
        }

        if (fileName == null) {
            java.util.List<String> fallbacks = getFallbackNames(lookupName);
            for (String fallback : fallbacks) {
                fileName = currentIconMap.get(fallback);
                if (fileName != null) {
                    break;
                }
            }
        }

        if (fileName == null) {
            return null;
        }


        if (drawableCache.containsKey(resourceName)) {
            Drawable cached = drawableCache.get(resourceName);
            if (cached != null && cached.getConstantState() != null) {
                return cached.getConstantState().newDrawable().mutate();
            }
            return cached;
        }

        File imgFile = new File(currentPackDir, fileName);
        if (imgFile.exists()) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                if (bitmap != null) {
                    int origW = -1;
                    int origH = -1;
                    try {
                        int id = ApplicationLoader.applicationContext.getResources().getIdentifier(resourceName, "drawable", ApplicationLoader.applicationContext.getPackageName());
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
            if (!metadataFile.exists()) return null;
            try {
                String jsonStr = readFileToString(metadataFile);
                JSONObject json = new JSONObject(jsonStr);
                IconPackInfo info = new IconPackInfo();
                info.packId = json.optString("packId", packDir.getName());
                info.packName = json.optString("packName", "Unknown");
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
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("metadata.json")) {
                    StringBuilder sb = new StringBuilder();
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = zis.read(buffer)) != -1) {
                        sb.append(new String(buffer, 0, read));
                    }
                    JSONObject json = new JSONObject(sb.toString());
                    IconPackInfo info = new IconPackInfo();
                    info.packId = json.optString("packId", zipFile.getName());
                    info.packName = json.optString("packName", "Unknown");
                    info.author = json.optString("author", "Unknown");
                    info.version = json.optString("version", "1.0");
                    return info;
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
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
        String[] suffixes = {"_solar", "_remix", "_filled", "_round", "_outline", "_solid", "_colored", "2"};
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
        String[] prefixes = {"msg_", "menu_", "chats_", "ic_ab_", "ic_", "chats_filter_", "filter_", "msg2_"};
        for (String p : prefixes) {
            if (stripped.startsWith(p)) {
                stripped = stripped.substring(p.length());
                break;
            }
        }

        if (!list.contains(stripped)) list.add(stripped);

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
