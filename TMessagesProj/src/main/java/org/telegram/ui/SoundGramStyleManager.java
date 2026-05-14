package org.telegram.ui;

// ═══════════════════════════════════════════════════════════════════════════════
// SoundGramStyleManager.java — обновлён для новой логики цветов (Задача 1)
//
// Новые методы:
//   buildGlassDrawable() теперь читает кастомные цвета из prefs
//   dispatchStyleChanged() вызывается при любом изменении
// ═══════════════════════════════════════════════════════════════════════════════

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.ActionBar.Theme;

public class SoundGramStyleManager {

    // ─── Слушатели мгновенного обновления ─────────────────────────────────────
    public interface StyleListener { void onStyleChanged(); }

    private static final java.util.WeakHashMap<View, StyleListener> sListeners =
            new java.util.WeakHashMap<>();

    public static void addStyleListener(View v, StyleListener l)  { sListeners.put(v, l); }
    public static void removeStyleListener(View v)                 { sListeners.remove(v); }

    /** Вызывает onStyleChanged() у всех прикреплённых View немедленно */
    public static void dispatchStyleChanged() {
        for (java.util.Map.Entry<View, StyleListener> e : new java.util.ArrayList<>(sListeners.entrySet())) {
            View v = e.getKey(); StyleListener l = e.getValue();
            if (v != null && l != null && v.isAttachedToWindow()) v.post(l::onStyleChanged);
        }
    }

    // ─── Построение GradientDrawable ──────────────────────────────────────────

    /**
     * Строит drawable по ключам из SoundGramStyleSettingsFragment.
     *
     * @param keySwitch      ключ boolean — обводка вкл/выкл
     * @param keyAlpha       ключ int 0..255 — прозрачность подложки
     * @param keyBgColor     ключ int — кастомный цвет подложки (-1 = тема TG)
     * @param keyStrokeColor ключ int — кастомный цвет обводки (-1 = акцент TG)
     * @param cornerDp       радиус скругления dp
     */
    public static GradientDrawable buildGlassDrawable(
            String keySwitch,
            String keyAlpha,
            String keyBgColor,
            String keyStrokeColor,
            float  cornerDp) {

        SharedPreferences p = ApplicationLoader.applicationContext
                .getSharedPreferences(SoundGramStyleSettingsFragment.PREFS_NAME, Context.MODE_PRIVATE);

        boolean strokeOn = p.getBoolean(keySwitch, true);
        int alpha        = p.getInt(keyAlpha, 180);

        int bgColor = p.getInt(keyBgColor, -1);
        if (bgColor == -1) bgColor = Theme.getColor(Theme.key_dialogBackground);

        int strokeColor = p.getInt(keyStrokeColor, -1);
        if (strokeColor == -1) strokeColor = Theme.getColor(Theme.key_featuredStickers_addButton);

        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(AndroidUtilities.dp(cornerDp));
        gd.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(bgColor, alpha));
        if (strokeOn) {
            gd.setStroke(AndroidUtilities.dp(1),
                    androidx.core.graphics.ColorUtils.setAlphaComponent(strokeColor, 180));
        }
        return gd;
    }

    /**
     * Обратная совместимость: старый сигнатур (без кастомных цветов).
     * Используется в местах где ещё нет пар ключей цветов.
     * Цвета берутся из темы TG.
     */
    public static GradientDrawable buildGlassDrawable(
            String keySwitch, String keyAlpha, int accentColor, float cornerDp) {

        SharedPreferences p = ApplicationLoader.applicationContext
                .getSharedPreferences(SoundGramStyleSettingsFragment.PREFS_NAME, Context.MODE_PRIVATE);

        boolean strokeOn = p.getBoolean(keySwitch, true);
        int alpha        = p.getInt(keyAlpha, 180);
        int bgColor      = Theme.getColor(Theme.key_dialogBackground);

        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(AndroidUtilities.dp(cornerDp));
        gd.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(bgColor, alpha));
        if (strokeOn) {
            gd.setStroke(AndroidUtilities.dp(1),
                    androidx.core.graphics.ColorUtils.setAlphaComponent(accentColor, 180));
        }
        return gd;
    }

    public static void applyGlass(View view,
                                  String keySwitch, String keyAlpha,
                                  String keyBgColor, String keyStrokeColor,
                                  float cornerDp, Runnable afterApply) {
        Runnable fn = () -> {
            view.setBackground(buildGlassDrawable(
                    keySwitch, keyAlpha, keyBgColor, keyStrokeColor, cornerDp));
            if (afterApply != null) afterApply.run();
        };
        fn.run();
        addStyleListener(view, fn::run);
    }

}
