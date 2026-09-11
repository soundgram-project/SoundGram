package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import android.content.SharedPreferences;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;

public class LauncherIconController {
    private static final String PREF_NAME = "launcher_icon_pref";
    private static final String KEY_ACTIVE_ICON = "active_icon";

    public static void tryFixLauncherIconIfNeeded() {
        LauncherIcon active = getActiveIcon();
        if (active != null && isEnabled(active)) {
            return;
        }
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return;
            }
        }

        setIcon(LauncherIcon.DEFAULT);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT;
    }

    public static LauncherIcon getActiveIcon() {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx != null) {
                SharedPreferences pref = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                String name = pref.getString(KEY_ACTIVE_ICON, null);
                if (name != null) {
                    for (LauncherIcon icon : LauncherIcon.values()) {
                        if (icon.name().equalsIgnoreCase(name) || icon.key.equalsIgnoreCase(name)) {
                            return icon;
                        }
                    }
                }
            }
        } catch (Throwable ignore) {}

        for (LauncherIcon icon : LauncherIcon.values()) {
            if (icon != LauncherIcon.DEFAULT && isEnabled(icon)) {
                return icon;
            }
        }
        if (isEnabled(LauncherIcon.DEFAULT)) {
            return LauncherIcon.DEFAULT;
        }
        return LauncherIcon.DEFAULT;
    }

    public static int getActiveIconRes() {
        LauncherIcon icon = getActiveIcon();
        switch (icon) {
            case TELEGRAM:
                return R.mipmap.ic_launcher_dr;
            case VINTAGE:
                return R.mipmap.icon_6_launcher;
            case AQUA:
                return R.mipmap.icon_4_launcher;
            case PREMIUM:
                return R.mipmap.icon_3_launcher;
            case TURBO:
                return R.mipmap.icon_5_launcher;
            case NOX:
                return R.mipmap.icon_2_launcher;
            case DEFAULT:
            default:
                return R.mipmap.ic_launcher;
        }
    }

    public static Drawable getActiveIconDrawable(Context context) {
        if (context == null) context = ApplicationLoader.applicationContext;
        int res = getActiveIconRes();
        return ContextCompat.getDrawable(context, res);
    }

    public static Bitmap getActiveIconBitmap(Context context) {
        if (context == null) context = ApplicationLoader.applicationContext;
        Drawable d = getActiveIconDrawable(context);
        if (d instanceof BitmapDrawable) {
            return ((BitmapDrawable) d).getBitmap();
        }
        int width = d != null && d.getIntrinsicWidth() > 0 ? d.getIntrinsicWidth() : AndroidUtilities.dp(48);
        int height = d != null && d.getIntrinsicHeight() > 0 ? d.getIntrinsicHeight() : AndroidUtilities.dp(48);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        if (d != null) {
            d.setBounds(0, 0, width, height);
            d.draw(canvas);
        }
        return bitmap;
    }

    // Все алиасы в AndroidManifest.xml. Должны совпадать с ключами LauncherIcon enum.
    // При смене иконки все неактивные алиасы отключаются, активный — включается.
    private static String[] getAllLauncherKeys() {
        LauncherIcon[] icons = LauncherIcon.values();
        String[] keys = new String[icons.length + 1];
        for (int i = 0; i < icons.length; i++) {
            keys[i] = icons[i].key;
        }
        keys[icons.length] = "DefaultIcon";
        return keys;
    }

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        try {
            SharedPreferences pref = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            pref.edit().putString(KEY_ACTIVE_ICON, icon.name()).commit();
        } catch (Throwable e) {
            org.telegram.messenger.FileLog.e(e);
        }

        PackageManager pm = ctx.getPackageManager();
        try {
            String[] allKeys = getAllLauncherKeys();
            String activeKey = icon.key;
            // Включаем новую иконку с флагом DONT_KILL_APP, чтобы процесс не убивался на первой итерации
            ComponentName newComponent = icon.getComponentName(ctx);
            pm.setComponentEnabledSetting(newComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            // Затем отключаем все остальные
            for (String key : allKeys) {
                if (!key.equals(activeKey)) {
                    ComponentName comp = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
                    try {
                        pm.setComponentEnabledSetting(comp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    } catch (Exception ignore) {
                        // Алиас может отсутствовать в манифесте — игнорируем
                    }
                }
            }
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
        }

        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.appIconChanged, icon);
        refreshNotifications();
    }

    public static void refreshNotifications() {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
                    if (UserConfig.getInstance(i).isClientActivated()) {
                        NotificationsController.getInstance(i).showNotifications();
                    }
                }
            } catch (Throwable e) {
                org.telegram.messenger.FileLog.e(e);
            }
        });
    }

    public enum LauncherIcon {
        DEFAULT("BlueIcon", R.mipmap.ic_launcher_background, R.mipmap.ic_launcher_foreground, R.string.AppIconDefault),
        TELEGRAM("TelegramIcon", R.drawable.icon_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconTelegramOriginal),
        VINTAGE("VintageIcon", R.drawable.icon_6_background_sa, R.mipmap.icon_6_foreground_sa, R.string.AppIconVintage),
        AQUA("AquaIcon", R.drawable.icon_4_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconAqua),
        PREMIUM("PremiumIcon", R.drawable.icon_3_background_sa, R.mipmap.icon_3_foreground_sa, R.string.AppIconPremium),
        TURBO("TurboIcon", R.drawable.icon_5_background_sa, R.mipmap.icon_5_foreground_sa, R.string.AppIconTurbo),
        NOX("NoxIcon", R.mipmap.icon_2_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconNox);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
        }
    }
}
