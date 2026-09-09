package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
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

    // Все алиасы в AndroidManifest.xml. Должны совпадать с ключами LauncherIcon enum.
    // При смене иконки все неактивные алиасы отключаются, активный — включается.
    private static String[] getAllLauncherKeys() {
        LauncherIcon[] icons = LauncherIcon.values();
        String[] keys = new String[icons.length];
        for (int i = 0; i < icons.length; i++) {
            keys[i] = icons[i].key;
        }
        return keys;
    }

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        try {
            String[] allKeys = getAllLauncherKeys();
            String activeKey = icon.key;
            // Сначала включаем новую иконку
            // Флаг 0 (а не DONT_KILL_APP) — лончер подхватит изменение немедленно
            ComponentName newComponent = icon.getComponentName(ctx);
            pm.setComponentEnabledSetting(newComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0);
            // Затем отключаем все остальные
            for (String key : allKeys) {
                if (!key.equals(activeKey)) {
                    ComponentName comp = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
                    try {
                        pm.setComponentEnabledSetting(comp, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
                    } catch (Exception ignore) {
                        // Алиас может отсутствовать в манифесте — игнорируем
                    }
                }
            }
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
        }
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
