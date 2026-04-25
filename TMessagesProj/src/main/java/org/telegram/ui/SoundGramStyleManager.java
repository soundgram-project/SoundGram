package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.ActionBar.Theme;

/**
 * SoundGramStyleManager — центральный менеджер стиля жидкого стекла.
 *
 * Использование в MiniPlayer / других компонентах:
 *
 *   // 1. Подписаться на изменения (в onCreate / onAttach):
 *   SoundGramStyleManager.addStyleListener(myView, () -> applyGlassStyle());
 *
 *   // 2. Применить стиль:
 *   myCard.setBackground(
 *       SoundGramStyleManager.buildGlassDrawable(
 *           KEY_GLASS_MINI, KEY_ALPHA_MINI, accentColor, 18f));
 *
 *   // 3. Отписаться (в onDestroy / onDetach):
 *   SoundGramStyleManager.removeStyleListener(myView);
 */
public class SoundGramStyleManager {

    // ─── Интерфейс слушателя изменений стиля ─────────────────────────────────
    public interface StyleListener {
        void onStyleChanged();
    }

    // Хранилище слушателей: View → Listener (используем WeakHashMap чтобы не держать View в памяти)
    private static final java.util.WeakHashMap<View, StyleListener> sListeners =
            new java.util.WeakHashMap<>();

    /** Подписывает view на изменения стиля */
    public static void addStyleListener(View view, StyleListener listener) {
        sListeners.put(view, listener);
    }

    /** Отписывает view от изменений стиля */
    public static void removeStyleListener(View view) {
        sListeners.remove(view);
    }

    /**
     * Вызывается из SoundGramStyleSettingsFragment при любом изменении настройки.
     * Мгновенно обновляет все зарегистрированные компоненты.
     */
    public static void dispatchStyleChanged() {
        for (java.util.Map.Entry<View, StyleListener> entry : sListeners.entrySet()) {
            View v = entry.getKey();
            StyleListener l = entry.getValue();
            if (v != null && l != null && v.isAttachedToWindow()) {
                v.post(l::onStyleChanged);
            }
        }
    }

    // ─── Построение GradientDrawable ─────────────────────────────────────────

    /**
     * Возвращает готовый GradientDrawable для жидкого стекла по настройкам.
     *
     * @param keySwitch  ключ boolean (включено / выключено)
     * @param keyAlpha   ключ int 0..255 (прозрачность)
     * @param accentColor цвет обводки
     * @param cornerDp   радиус скругления в dp
     */
    public static GradientDrawable buildGlassDrawable(String keySwitch, String keyAlpha,
                                                      int accentColor, float cornerDp) {
        SharedPreferences prefs = ApplicationLoader.applicationContext
                .getSharedPreferences(SoundGramStyleSettingsFragment.PREFS_NAME, Context.MODE_PRIVATE);

        boolean glassOn = prefs.getBoolean(keySwitch, true);
        int alpha       = prefs.getInt(keyAlpha, 180);

        int bgColor = Theme.getColor(Theme.key_dialogBackground);
        float radius = AndroidUtilities.dp(cornerDp);

        GradientDrawable gd = new GradientDrawable();
        if (glassOn) {
            gd.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(bgColor, alpha));
            gd.setCornerRadius(radius);
            gd.setStroke(AndroidUtilities.dp(1),
                    androidx.core.graphics.ColorUtils.setAlphaComponent(accentColor, 90));
        } else {
            gd.setColor(bgColor);
            gd.setCornerRadius(radius);
        }
        return gd;
    }

    /**
     * Удобный метод: сразу применяет стиль к View и подписывает её на обновления.
     *
     * Пример:
     *   SoundGramStyleManager.applyGlass(
     *       miniPlayerCard,
     *       KEY_GLASS_MINI, KEY_ALPHA_MINI,
     *       accentColor, 22f,
     *       () -> { // доп. логика после обновления стиля, например invalidate() }
     *   );
     */
    public static void applyGlass(View view,
                                  String keySwitch, String keyAlpha,
                                  int accentColor, float cornerDp,
                                  Runnable afterApply) {
        Runnable applyFn = () -> {
            view.setBackground(buildGlassDrawable(keySwitch, keyAlpha, accentColor, cornerDp));
            if (afterApply != null) afterApply.run();
        };
        applyFn.run(); // Применить немедленно
        addStyleListener(view, applyFn::run); // Подписать на будущие изменения
    }
}
