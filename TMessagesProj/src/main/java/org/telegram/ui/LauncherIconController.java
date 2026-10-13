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
    private static volatile LauncherIcon sCachedActiveIcon = null;

    public static void tryFixLauncherIconIfNeeded() {
        sCachedActiveIcon = null;
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
        LauncherIcon cached = sCachedActiveIcon;
        if (cached != null) {
            return cached;
        }
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx != null) {
                SharedPreferences pref = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                String name = pref.getString(KEY_ACTIVE_ICON, null);
                if (name != null) {
                    for (LauncherIcon icon : LauncherIcon.values()) {
                        if (icon.name().equalsIgnoreCase(name) || icon.key.equalsIgnoreCase(name)) {
                            sCachedActiveIcon = icon;
                            return icon;
                        }
                    }
                }
            }
        } catch (Throwable ignore) {}

        for (LauncherIcon icon : LauncherIcon.values()) {
            if (icon != LauncherIcon.DEFAULT && isEnabled(icon)) {
                sCachedActiveIcon = icon;
                return icon;
            }
        }
        if (isEnabled(LauncherIcon.DEFAULT)) {
            sCachedActiveIcon = LauncherIcon.DEFAULT;
            return LauncherIcon.DEFAULT;
        }
        sCachedActiveIcon = LauncherIcon.DEFAULT;
        return LauncherIcon.DEFAULT;
    }

    public static int getActiveIconRes() {
        LauncherIcon icon = getActiveIcon();
        switch (icon) {
            case COSMOS:
                return R.mipmap.icon_cosmos_launcher;
            case ANIME:
                return R.mipmap.icon_anime_launcher;
            case NEON:
                return R.mipmap.icon_neon_launcher;
            case SUNSET:
                return R.mipmap.icon_sunset_launcher;
            case EMERALD:
                return R.mipmap.icon_emerald_launcher;
            case MIDNIGHT:
                return R.mipmap.icon_midnight_launcher;
            case GOLD:
                return R.mipmap.icon_gold_launcher;
            case CRIMSON:
                return R.mipmap.icon_crimson_launcher;
            case LAVENDER:
                return R.mipmap.icon_lavender_launcher;
            case MATRIX:
                return R.mipmap.icon_matrix_launcher;
            case SOLAR:
                return R.mipmap.icon_solar_launcher;
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
        sCachedActiveIcon = icon;
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

    public static int getActiveStartThemeRes() {
        LauncherIcon icon = getActiveIcon();
        if (icon == null) {
            return R.style.Theme_TMessages_Start_BlueIcon;
        }
        switch (icon) {
            case COSMOS:
                return R.style.Theme_TMessages_Start_CosmosIcon;
            case ANIME:
                return R.style.Theme_TMessages_Start_AnimeIcon;
            case NEON:
                return R.style.Theme_TMessages_Start_NeonIcon;
            case SUNSET:
                return R.style.Theme_TMessages_Start_SunsetIcon;
            case EMERALD:
                return R.style.Theme_TMessages_Start_EmeraldIcon;
            case MIDNIGHT:
                return R.style.Theme_TMessages_Start_MidnightIcon;
            case GOLD:
                return R.style.Theme_TMessages_Start_GoldIcon;
            case CRIMSON:
                return R.style.Theme_TMessages_Start_CrimsonIcon;
            case LAVENDER:
                return R.style.Theme_TMessages_Start_LavenderIcon;
            case MATRIX:
                return R.style.Theme_TMessages_Start_MatrixIcon;
            case SOLAR:
                return R.style.Theme_TMessages_Start_SolarIcon;
            case TELEGRAM:
                return R.style.Theme_TMessages_Start_TelegramIcon;
            case VINTAGE:
                return R.style.Theme_TMessages_Start_VintageIcon;
            case AQUA:
                return R.style.Theme_TMessages_Start_AquaIcon;
            case PREMIUM:
                return R.style.Theme_TMessages_Start_PremiumIcon;
            case TURBO:
                return R.style.Theme_TMessages_Start_TurboIcon;
            case NOX:
                return R.style.Theme_TMessages_Start_NoxIcon;
            case DEFAULT:
            default:
                return R.style.Theme_TMessages_Start_BlueIcon;
        }
    }

    public static void applyNotificationAppInfo(androidx.core.app.NotificationCompat.Builder builder, Context context) {
        if (builder == null) return;
        if (context == null) context = ApplicationLoader.applicationContext;
        try {
            android.content.pm.ApplicationInfo appInfo = new android.content.pm.ApplicationInfo(context.getApplicationInfo());
            appInfo.icon = getActiveIconRes();
            builder.getExtras().putParcelable("android.appInfo", appInfo);
        } catch (Throwable ignore) {}
    }

    public static void applyNotificationAppInfo(android.app.Notification.Builder builder, Context context) {
        if (builder == null) return;
        if (context == null) context = ApplicationLoader.applicationContext;
        try {
            android.content.pm.ApplicationInfo appInfo = new android.content.pm.ApplicationInfo(context.getApplicationInfo());
            appInfo.icon = getActiveIconRes();
            builder.getExtras().putParcelable("android.appInfo", appInfo);
        } catch (Throwable ignore) {}
    }

    public static void applyNotificationAppInfo(android.app.Notification notification, Context context) {
        if (notification == null) return;
        if (context == null) context = ApplicationLoader.applicationContext;
        try {
            android.content.pm.ApplicationInfo appInfo = new android.content.pm.ApplicationInfo(context.getApplicationInfo());
            appInfo.icon = getActiveIconRes();
            if (notification.extras != null) {
                notification.extras.putParcelable("android.appInfo", appInfo);
            }
        } catch (Throwable ignore) {}
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
            try {
                if (org.telegram.messenger.MediaController.getInstance().getPlayingMessageObject() != null) {
                    android.content.Intent intent = new android.content.Intent(ApplicationLoader.applicationContext, org.telegram.messenger.MusicPlayerService.class);
                    ApplicationLoader.applicationContext.startService(intent);
                }
            } catch (Throwable e) {
                org.telegram.messenger.FileLog.e(e);
            }
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && app.nimarkogram.messenger.NimarkoConfig.residentNotification) {
                    android.content.Intent intent = new android.content.Intent(ApplicationLoader.applicationContext, org.telegram.messenger.NotificationsService.class);
                    ApplicationLoader.applicationContext.startService(intent);
                }
            } catch (Throwable ignore) {}
        });
    }

    public enum LauncherIcon {
        DEFAULT("BlueIcon", R.mipmap.ic_launcher_background, R.mipmap.ic_launcher_foreground, R.string.AppIconDefault),
        TELEGRAM("TelegramIcon", R.drawable.icon_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconTelegramOriginal),
        COSMOS("CosmosIcon", R.drawable.icon_cosmos_background_sa, R.drawable.icon_cosmos_foreground_sa, R.string.AppIconCosmos),
        ANIME("AnimeIcon", R.drawable.icon_anime_background_sa, R.drawable.icon_anime_foreground_sa, R.string.AppIconAnime),
        NEON("NeonIcon", R.drawable.icon_neon_background_sa, R.drawable.icon_neon_foreground_sa, R.string.AppIconCyberNeon),
        SUNSET("SunsetIcon", R.drawable.icon_sunset_background_sa, R.drawable.icon_sunset_foreground_sa, R.string.AppIconSunset),
        EMERALD("EmeraldIcon", R.drawable.icon_emerald_background_sa, R.drawable.icon_emerald_foreground_sa, R.string.AppIconEmerald),
        MIDNIGHT("MidnightIcon", R.drawable.icon_midnight_background_sa, R.drawable.icon_midnight_foreground_sa, R.string.AppIconMidnight),
        GOLD("GoldIcon", R.drawable.icon_gold_background_sa, R.drawable.icon_gold_foreground_sa, R.string.AppIconGold),
        CRIMSON("CrimsonIcon", R.drawable.icon_crimson_background_sa, R.drawable.icon_crimson_foreground_sa, R.string.AppIconCrimson),
        LAVENDER("LavenderIcon", R.drawable.icon_lavender_background_sa, R.drawable.icon_lavender_foreground_sa, R.string.AppIconLavender),
        MATRIX("MatrixIcon", R.drawable.icon_matrix_background_sa, R.drawable.icon_matrix_foreground_sa, R.string.AppIconMatrix),
        SOLAR("SolarIcon", R.drawable.icon_solar_background_sa, R.drawable.icon_solar_foreground_sa, R.string.AppIconSolar),
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
