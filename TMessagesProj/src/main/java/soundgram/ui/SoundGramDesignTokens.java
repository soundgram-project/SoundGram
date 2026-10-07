package soundgram.ui;

import android.graphics.drawable.GradientDrawable;
import org.telegram.messenger.AndroidUtilities;

/**
 * SoundGram design tokens and aesthetic primitives.
 * Analog radio metaphor: pure dark background (#111111), sole lime accent (#C8F169),
 * asymmetric corner radiuses, clean typography, no glassmorphism or emojis.
 */
public class SoundGramDesignTokens {
    // ── Color Tokens ───────────────────────────────────────────────────────────
    public static final int COLOR_BG           = 0xFF111111; // Фон экранов
    public static final int COLOR_SURFACE_1    = 0xFF1B1B1B; // Поля ввода, карточки
    public static final int COLOR_SURFACE_2    = 0xFF1E1E1E; // Мини-плеер
    public static final int COLOR_SURFACE_3    = 0xFF2A2A2A; // Плейсхолдер обложки
    public static final int COLOR_LINE         = 0xFF2E2E2E; // Разделители, 0.5-1 px
    public static final int COLOR_LINE_STRONG  = 0xFF3A3A3A; // Рамки чипов, дальние строки
    public static final int COLOR_TEXT         = 0xFFEEEEEE; // Основной текст
    public static final int COLOR_TEXT_2       = 0xFFBDBDBD; // Вторичный
    public static final int COLOR_TEXT_SECONDARY = COLOR_TEXT_2;
    public static final int COLOR_TEXT_MUTED   = 0xFF8A8A8A; // Моно-подписи
    public static final int COLOR_TEXT_DIM     = 0xFF5C5C5C; // Неактивные строки
    public static final int COLOR_ACCENT       = 0xFFC8F169; // Единственный акцент
    public static final int COLOR_ACCENT_HOVER = 0xFFD8F58A; // Акцент при нажатии
    public static final int COLOR_ON_ACCENT    = 0xFF111111; // Текст на акценте

    // ── Asymmetric Corner Radii (dp) ───────────────────────────────────────────
    // Обложка в плеере: 4 4 4 56
    // Главная карточка плейлиста: 4 4 4 56
    // Высокая карточка: 56 4 4 4
    // Малая карточка: 4 4 24 4
    // Обложка в списке: 4 4 4 14
    // Кнопка Play: 22
    // Мини-плеер: 18
    // Поля ввода: 12
    // Чипы: 999
    // Корпус экрана: 28

    public static GradientDrawable makeAsymmetricDrawable(int color, float tl, float tr, float br, float bl) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadii(new float[]{
                AndroidUtilities.dp(tl), AndroidUtilities.dp(tl),
                AndroidUtilities.dp(tr), AndroidUtilities.dp(tr),
                AndroidUtilities.dp(br), AndroidUtilities.dp(br),
                AndroidUtilities.dp(bl), AndroidUtilities.dp(bl)
        });
        return gd;
    }

    public static GradientDrawable makeAsymmetricDrawable(int color, int strokeColor, float strokeWidthDp, float tl, float tr, float br, float bl) {
        GradientDrawable gd = makeAsymmetricDrawable(color, tl, tr, br, bl);
        if (strokeWidthDp > 0) {
            gd.setStroke(AndroidUtilities.dp(strokeWidthDp), strokeColor);
        }
        return gd;
    }

    public static GradientDrawable makePillDrawable(int color, float radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(AndroidUtilities.dp(radiusDp));
        return gd;
    }

    public static GradientDrawable makeCardDrawable() {
        return makeAsymmetricDrawable(COLOR_SURFACE_1, 4, 4, 4, 24);
    }

    public static GradientDrawable makeMainPlaylistCardDrawable() {
        return makeAsymmetricDrawable(COLOR_SURFACE_1, 4, 4, 4, 56);
    }

    public static GradientDrawable makeTallCardDrawable() {
        return makeAsymmetricDrawable(COLOR_SURFACE_1, 56, 4, 4, 4);
    }

    public static GradientDrawable makeAccentChatCardDrawable() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(COLOR_ACCENT);
        gd.setCornerRadii(new float[]{
                AndroidUtilities.dp(4), AndroidUtilities.dp(4),
                AndroidUtilities.dp(40), AndroidUtilities.dp(40),
                AndroidUtilities.dp(4), AndroidUtilities.dp(4),
                AndroidUtilities.dp(4), AndroidUtilities.dp(4)
        });
        return gd;
    }

    public static GradientDrawable makeChipDrawable(boolean active) {
        GradientDrawable gd = new GradientDrawable();
        if (active) {
            gd.setColor(COLOR_ACCENT);
        } else {
            gd.setColor(COLOR_SURFACE_1);
            gd.setStroke(AndroidUtilities.dp(0.75f), COLOR_LINE_STRONG);
        }
        gd.setCornerRadius(AndroidUtilities.dp(999));
        return gd;
    }

    public static GradientDrawable makeMiniPlayerDrawable() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(COLOR_SURFACE_2);
        gd.setCornerRadius(AndroidUtilities.dp(18));
        return gd;
    }

    public static GradientDrawable makePlayButtonDrawable() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(COLOR_ACCENT);
        gd.setCornerRadius(AndroidUtilities.dp(22));
        return gd;
    }

    // ── Formatting ─────────────────────────────────────────────────────────────
    public static String formatTracksCount(int count) {
        int rem10 = count % 10;
        int rem100 = count % 100;
        if (rem100 >= 11 && rem100 <= 19) {
            return count + " треков";
        }
        if (rem10 == 1) {
            return count + " трек";
        }
        if (rem10 >= 2 && rem10 <= 4) {
            return count + " трека";
        }
        return count + " треков";
    }

    public static String formatDuration(int totalSeconds) {
        int totalMins = totalSeconds / 60;
        int hours = totalMins / 60;
        int mins = totalMins % 60;
        if (hours > 0) {
            return hours + " ч " + mins + " мин";
        } else {
            return mins + " мин";
        }
    }
}
