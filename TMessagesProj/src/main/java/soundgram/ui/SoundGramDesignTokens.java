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
    // ── Theme Inherited Colors (Updated dynamically from Theme) ────────────────
    public static int COLOR_BG           = 0xFF111111; // Фон экранов
    public static int COLOR_SURFACE_1    = 0xFF1B1B1B; // Поля ввода, карточки
    public static int COLOR_SURFACE_2    = 0xFF1E1E1E; // Мини-плеер
    public static int COLOR_SURFACE_3    = 0xFF2A2A2A; // Плейсхолдер обложки
    public static int COLOR_LINE         = 0xFF2E2E2E; // Разделители, 0.5-1 px
    public static int COLOR_LINE_STRONG  = 0xFF3A3A3A; // Рамки чипов, дальние строки
    public static int COLOR_TEXT         = 0xFFEEEEEE; // Основной текст
    public static int COLOR_TEXT_2       = 0xFFBDBDBD; // Вторичный
    public static int COLOR_TEXT_SECONDARY = COLOR_TEXT_2;
    public static int COLOR_TEXT_MUTED   = 0xFF8A8A8A; // Моно-подписи
    public static int COLOR_TEXT_DIM     = 0xFF5C5C5C; // Неактивные строки
    public static int COLOR_ACCENT       = 0xFFC8F169; // Единственный акцент
    public static int COLOR_ACCENT_HOVER = 0xFFD8F58A; // Акцент при нажатии
    public static int COLOR_ON_ACCENT    = 0xFF111111; // Текст на акценте

    static {
        updateColors();
    }

    public static void updateColors() {
        try {
            int bg = Theme.getColor(Theme.key_windowBackgroundWhite);
            if (bg != 0) COLOR_BG = bg;
            int s1 = Theme.getColor(Theme.key_windowBackgroundGray);
            if (s1 != 0) COLOR_SURFACE_1 = s1;
            int s2 = Theme.getColor(Theme.key_chat_inBubble);
            if (s2 != 0) COLOR_SURFACE_2 = s2;
            int s3 = Theme.getColor(Theme.key_graySection);
            if (s3 != 0) COLOR_SURFACE_3 = s3;
            int line = Theme.getColor(Theme.key_divider);
            if (line != 0) {
                COLOR_LINE = line;
                COLOR_LINE_STRONG = line;
            }
            int text = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
            if (text != 0) COLOR_TEXT = text;
            int text2 = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);
            if (text2 != 0) {
                COLOR_TEXT_2 = text2;
                COLOR_TEXT_SECONDARY = text2;
                COLOR_TEXT_MUTED = text2;
                COLOR_TEXT_DIM = text2;
            }
            int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
            if (accent == 0) {
                accent = Theme.getColor(Theme.key_chats_actionBackground);
            }
            if (accent != 0) {
                COLOR_ACCENT = accent;
                COLOR_ACCENT_HOVER = accent;
            }
            int onAccent = Theme.getColor(Theme.key_featuredStickers_buttonText);
            if (onAccent != 0) COLOR_ON_ACCENT = onAccent;
        } catch (Exception ignored) {}
    }

    public static Drawable getSelectorDrawable() {
        return Theme.getSelectorDrawable(false);
    }

    public static Drawable getCircleSelectorDrawable() {
        int sel = Theme.getColor(Theme.key_listSelector);
        if (sel == 0) sel = 0x22888888;
        return Theme.createSelectorDrawable(sel, 1);
    }

    // ── Dynamic Telegram Theme Inherited Colors ───────────────────────────────
    public static int getBgColor() {
        updateColors();
        return COLOR_BG;
    }

    public static int getSurface1Color() {
        updateColors();
        return COLOR_SURFACE_1;
    }

    public static int getSurface2Color() {
        updateColors();
        return COLOR_SURFACE_2;
    }

    public static int getSurface3Color() {
        updateColors();
        return COLOR_SURFACE_3;
    }

    public static int getLineColor() {
        updateColors();
        return COLOR_LINE;
    }

    public static int getTextColor() {
        updateColors();
        return COLOR_TEXT;
    }

    public static int getText2Color() {
        updateColors();
        return COLOR_TEXT_2;
    }

    public static int getTextMutedColor() {
        updateColors();
        return COLOR_TEXT_MUTED;
    }

    public static int getTextDimColor() {
        updateColors();
        return COLOR_TEXT_DIM;
    }

    public static int getAccentColor() {
        updateColors();
        return COLOR_ACCENT;
    }

    public static int getOnAccentColor() {
        updateColors();
        return COLOR_ON_ACCENT;
    }

    public static int getLineStrongColor() {
        updateColors();
        return COLOR_LINE_STRONG;
    }

    public static int getPlaylistCardColor() {
        updateColors();
        int card = Theme.getColor(Theme.key_chat_inBubble);
        int bg = getBgColor();
        if (card == 0 || card == bg) {
            card = Theme.getColor(Theme.key_dialogBackground);
        }
        if (card == 0 || card == bg) {
            card = Theme.getColor(Theme.key_windowBackgroundGray);
        }
        if (card == bg) {
            if (AndroidUtilities.computePerceivedBrightness(bg) > 0.5f) {
                card = ColorUtils.blendARGB(bg, Color.BLACK, 0.08f);
            } else {
                card = ColorUtils.blendARGB(bg, Color.WHITE, 0.10f);
            }
        }
        return card;
    }

    public static GradientDrawable makePlaylistCardDrawable(float tl, float tr, float br, float bl) {
        return makeAsymmetricDrawable(getPlaylistCardColor(), getLineColor(), 1f, tl, tr, br, bl);
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
