package soundgram.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import androidx.core.graphics.ColorUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/**
 * SoundGram design tokens and aesthetic primitives.
 * Dynamic Telegram theme inheritance with glowing diffusing surfaces and asymmetric shapes.
 * No borders, soft glowing radiance, clean typography.
 */
public class SoundGramDesignTokens {
    // ── Static Fallback Colors ──────────────────────────────────────────────────
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

    // ── Dynamic Telegram Theme Inherited Colors ───────────────────────────────
    public static int getBgColor() {
        return Theme.hasThemeKey(Theme.key_windowBackgroundWhite) ? Theme.getColor(Theme.key_windowBackgroundWhite) : COLOR_BG;
    }

    public static int getSurface1Color() {
        return Theme.hasThemeKey(Theme.key_windowBackgroundGray) ? Theme.getColor(Theme.key_windowBackgroundGray) : COLOR_SURFACE_1;
    }

    public static int getSurface2Color() {
        return Theme.hasThemeKey(Theme.key_chat_inBubble) ? Theme.getColor(Theme.key_chat_inBubble) : COLOR_SURFACE_2;
    }

    public static int getSurface3Color() {
        return Theme.hasThemeKey(Theme.key_graySection) ? Theme.getColor(Theme.key_graySection) : COLOR_SURFACE_3;
    }

    public static int getLineColor() {
        return Theme.hasThemeKey(Theme.key_divider) ? Theme.getColor(Theme.key_divider) : COLOR_LINE;
    }

    public static int getTextColor() {
        return Theme.hasThemeKey(Theme.key_windowBackgroundWhiteBlackText) ? Theme.getColor(Theme.key_windowBackgroundWhiteBlackText) : COLOR_TEXT;
    }

    public static int getText2Color() {
        return Theme.hasThemeKey(Theme.key_windowBackgroundWhiteGrayText) ? Theme.getColor(Theme.key_windowBackgroundWhiteGrayText) : COLOR_TEXT_2;
    }

    public static int getTextMutedColor() {
        return Theme.hasThemeKey(Theme.key_windowBackgroundWhiteGrayText) ? Theme.getColor(Theme.key_windowBackgroundWhiteGrayText) : COLOR_TEXT_MUTED;
    }

    public static int getAccentColor() {
        return Theme.hasThemeKey(Theme.key_featuredStickers_addButton) ? Theme.getColor(Theme.key_featuredStickers_addButton) : COLOR_ACCENT;
    }

    public static int getOnAccentColor() {
        return Theme.hasThemeKey(Theme.key_featuredStickers_buttonText) ? Theme.getColor(Theme.key_featuredStickers_buttonText) : COLOR_ON_ACCENT;
    }

    public static int getLineStrongColor() {
        return Theme.hasThemeKey(Theme.key_divider) ? Theme.getColor(Theme.key_divider) : COLOR_LINE_STRONG;
    }

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
        return makeAsymmetricDrawable(getSurface1Color(), getLineColor(), 0.75f, 4, 4, 4, 24);
    }

    public static GradientDrawable makeMainPlaylistCardDrawable() {
        return makeAsymmetricDrawable(getSurface1Color(), getLineColor(), 0.75f, 4, 4, 4, 56);
    }

    public static GradientDrawable makeTallCardDrawable() {
        return makeAsymmetricDrawable(getSurface1Color(), getLineColor(), 0.75f, 56, 4, 4, 4);
    }

    public static GradientDrawable makeAccentChatCardDrawable() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(getAccentColor());
        gd.setCornerRadii(new float[]{
                AndroidUtilities.dp(4), AndroidUtilities.dp(4),
                AndroidUtilities.dp(40), AndroidUtilities.dp(40),
                AndroidUtilities.dp(4), AndroidUtilities.dp(4),
                AndroidUtilities.dp(4), AndroidUtilities.dp(4)
        });
        return gd;
    }

    public static Drawable makeChipDrawable(boolean active) {
        GradientDrawable gd = new GradientDrawable();
        if (active) {
            gd.setColor(getAccentColor());
        } else {
            gd.setColor(getSurface1Color());
            gd.setStroke(AndroidUtilities.dp(0.75f), getLineStrongColor());
        }
        gd.setCornerRadius(AndroidUtilities.dp(999));
        return gd;
    }

    public static GradientDrawable makeMiniPlayerDrawable() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(getSurface2Color());
        gd.setCornerRadius(AndroidUtilities.dp(18));
        gd.setStroke(AndroidUtilities.dp(0.75f), getLineColor());
        return gd;
    }

    public static GradientDrawable makePlayButtonDrawable() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(getAccentColor());
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
