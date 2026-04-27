package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import org.telegram.ui.SoundGramStyleManager;
import org.telegram.ui.SoundGramStyleSettingsFragment;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MediaController;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class SoundGramEQAlert extends BottomSheet {

    public SoundGramEQAlert(Context context) {
        super(context, false);
        SoundGramAudioFX.initFX(0);

        // ── Корневой контейнер — жидкое стекло ────────────────────────────────
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        // Полупрозрачный тёмный фон + скруглённые верхние углы + белая граница
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        int dialogBg = Theme.getColor(Theme.key_dialogBackground);
        gd.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(dialogBg, 230));
        float rad = AndroidUtilities.dp(24);
        gd.setCornerRadii(new float[]{rad, rad, rad, rad, 0, 0, 0, 0});
        gd.setStroke(AndroidUtilities.dp(1),
                androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF, 40));
        container.setBackground(SoundGramStyleManager.buildGlassDrawable(
                SoundGramStyleSettingsFragment.KEY_GLASS_EQ,
                SoundGramStyleSettingsFragment.KEY_ALPHA_EQ,
                SoundGramStyleSettingsFragment.KEY_COLOR_BG_EQ,
                SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_EQ,
                24f));
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            container.setElevation(AndroidUtilities.dp(8));
        }
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16),
                AndroidUtilities.dp(20), AndroidUtilities.dp(28));

        // ── Drag handle ────────────────────────────────────────────────────────
        View dragIndicator = new View(context);
        android.graphics.drawable.GradientDrawable handleBg = new android.graphics.drawable.GradientDrawable();
        handleBg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF, 70));
        handleBg.setCornerRadius(AndroidUtilities.dp(2));
        dragIndicator.setBackground(handleBg);
        container.addView(dragIndicator,
                LayoutHelper.createLinear(36, 4, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 20));

        // ── Шапка ─────────────────────────────────────────────────────────────
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText("AudioFX & Pitch");
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        headerRow.addView(title,
                LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        // Кнопка СБРОС — жидкое стекло с красным оттенком
        TextView resetBtn = new TextView(context);
        resetBtn.setText("СБРОС");
        resetBtn.setTextColor(0xFFFF5252);
        resetBtn.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 13);
        resetBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        android.graphics.drawable.GradientDrawable resetBg = new android.graphics.drawable.GradientDrawable();
        resetBg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFF5252, 30));
        resetBg.setCornerRadius(AndroidUtilities.dp(12));
        resetBg.setStroke(AndroidUtilities.dp(1),
                androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFF5252, 80));
        resetBtn.setBackground(resetBg);
        resetBtn.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(7),
                AndroidUtilities.dp(14), AndroidUtilities.dp(7));
        resetBtn.setOnClickListener(v -> {
            SoundGramAudioFX.resetFX();
            dismiss();
            new SoundGramEQAlert(context).show();
        });
        headerRow.addView(resetBtn,
                LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        container.addView(headerRow,
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                        0, 0, 0, 24));

        // ── Секция-разделитель helper ─────────────────────────────────────────
        // (используем вложенный метод ниже)

        // ── Эквалайзер в стеклянной карточке ─────────────────────────────────
        short bands = SoundGramAudioFX.getBandsCount();
        if (bands > 0) {
            // Стеклянная карточка EQ
            LinearLayout eqCard = new LinearLayout(context);
            eqCard.setOrientation(LinearLayout.VERTICAL);
            eqCard.setBackground(SoundGramStyleManager.buildGlassDrawable(
                    SoundGramStyleSettingsFragment.KEY_GLASS_EQ,
                    SoundGramStyleSettingsFragment.KEY_ALPHA_EQ,
                    SoundGramStyleSettingsFragment.KEY_COLOR_BG_EQ,
                    SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_EQ,
                    18f));
            eqCard.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12),
                    AndroidUtilities.dp(12), AndroidUtilities.dp(12));

            TextView eqLabel = new TextView(context);
            eqLabel.setText("Эквалайзер");
            eqLabel.setTextColor(
                    androidx.core.graphics.ColorUtils.setAlphaComponent(
                            Theme.getColor(Theme.key_dialogTextBlack), 160));
            eqLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 12);
            eqLabel.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            eqCard.addView(eqLabel,
                    LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                            0, 0, 0, 12));

            LinearLayout eqContainer = new LinearLayout(context);
            eqContainer.setOrientation(LinearLayout.HORIZONTAL);
            eqContainer.setGravity(Gravity.CENTER);
            eqContainer.setWeightSum(bands);

            int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);

            for (short i = 0; i < bands; i++) {
                final short bandIndex = i;
                LinearLayout bandCol = new LinearLayout(context);
                bandCol.setOrientation(LinearLayout.VERTICAL);
                bandCol.setGravity(Gravity.CENTER_HORIZONTAL);

                TextView freqText = new TextView(context);
                int freq = SoundGramAudioFX.getCenterFreq(bandIndex);
                String freqStr = freq >= 1000 ? (freq / 1000) + "k" : freq + "Hz";
                freqText.setText(freqStr);
                freqText.setTextColor(
                        androidx.core.graphics.ColorUtils.setAlphaComponent(
                                Theme.getColor(Theme.key_dialogTextGray2), 180));
                freqText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 10);
                freqText.setGravity(Gravity.CENTER_HORIZONTAL);
                bandCol.addView(freqText,
                        LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                                Gravity.CENTER_HORIZONTAL, 0, 0, 0, 6));

                FrameLayout barFrame = new FrameLayout(context);
                // Дорожка ползунка — жидкое стекло
                View trackView = new View(context);
                android.graphics.drawable.GradientDrawable trackBg = new android.graphics.drawable.GradientDrawable();
                trackBg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(accentColor, 35));
                trackBg.setCornerRadius(AndroidUtilities.dp(4));
                trackView.setBackground(trackBg);
                barFrame.addView(trackView, new FrameLayout.LayoutParams(
                        AndroidUtilities.dp(8), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL));

                android.widget.SeekBar bar = new android.widget.SeekBar(context);
                bar.setMax(3000);
                bar.setProgress(SoundGramAudioFX.getBandLevel(bandIndex) + 1500);
                bar.setRotation(-90);
                // Акцентный thumb
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    android.graphics.drawable.GradientDrawable thumbBg = new android.graphics.drawable.GradientDrawable();
                    thumbBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                    thumbBg.setColor(accentColor);
                    thumbBg.setSize(AndroidUtilities.dp(20), AndroidUtilities.dp(20));
                    thumbBg.setStroke(AndroidUtilities.dp(3), 0xCCFFFFFF);
                    bar.setThumb(thumbBg);
                    bar.setThumbOffset(AndroidUtilities.dp(10));
                }
                bar.setProgressTintList(android.content.res.ColorStateList.valueOf(accentColor));
                bar.setOnTouchListener((v, event) -> {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    return false;
                });
                bar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(android.widget.SeekBar s, int p, boolean fromUser) {
                        if (fromUser) SoundGramAudioFX.setBandLevel(bandIndex, (short) (p - 1500));
                    }
                    @Override public void onStartTrackingTouch(android.widget.SeekBar s) {}
                    @Override public void onStopTrackingTouch(android.widget.SeekBar s) {}
                });
                barFrame.addView(bar,
                        LayoutHelper.createFrame(140, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
                bandCol.addView(barFrame,
                        LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 140));

                eqContainer.addView(bandCol,
                        LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
            }
            eqCard.addView(eqContainer,
                    LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            container.addView(eqCard,
                    LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                            0, 0, 0, 16));
        }

        // ── Bass Boost в стеклянной карточке ─────────────────────────────────
        container.addView(
                makeGlassSliderCard(context, "BassBoost",
                        1000, SoundGramAudioFX.getBassLevel(),
                        (label, p) -> SoundGramAudioFX.setBassLevel((short) p)),
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                        0, 0, 0, 12));

        // ── Скорость ──────────────────────────────────────────────────────────
        final float[] speedHolder = {SoundGramAudioFX.getCurrentSpeed()};
        container.addView(
                makeGlassSliderCard(context,
                        String.format("Скорость: %.1fx", speedHolder[0]),
                        19,
                        Math.round((speedHolder[0] - 0.1f) / 0.1f),
                        (label, p) -> {
                            speedHolder[0] = 0.1f + (p * 0.1f);
                            label.setText(String.format("Скорость: %.1fx", speedHolder[0]));
                            SoundGramAudioFX.setCurrentSpeed(speedHolder[0]);
                            try { org.telegram.messenger.MediaController.getInstance()
                                    .setPlaybackSpeed(false, speedHolder[0]); }
                            catch (Exception ignored) {}
                        }),
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                        0, 0, 0, 12));

        // ── Тон ───────────────────────────────────────────────────────────────
        final float[] pitchHolder = {SoundGramAudioFX.getCurrentPitch()};
        container.addView(
                makeGlassSliderCard(context,
                        String.format("Тон: %.1fx", pitchHolder[0]),
                        19,
                        Math.round((pitchHolder[0] - 0.1f) / 0.1f),
                        (label, p) -> {
                            pitchHolder[0] = 0.1f + (p * 0.1f);
                            label.setText(String.format("Тон: %.1fx", pitchHolder[0]));
                            SoundGramAudioFX.setCurrentPitch(pitchHolder[0]);
                            try { org.telegram.messenger.MediaController.getInstance()
                                    .setPlaybackSpeed(false, SoundGramAudioFX.getCurrentSpeed()); }
                            catch (Exception ignored) {}
                        }),
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                        0, 0, 0, 4));

        android.widget.ScrollView scroll = new android.widget.ScrollView(context);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(container);
        setCustomView(scroll);
    }
    // ── helper: стеклянная карточка со слайдером ─────────────────────────────
    private View makeGlassSliderCard(Context context, String labelText, int max, int progress,
                                     SliderCallback callback) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(SoundGramStyleManager.buildGlassDrawable(
                SoundGramStyleSettingsFragment.KEY_GLASS_EQ,
                SoundGramStyleSettingsFragment.KEY_ALPHA_EQ,
                SoundGramStyleSettingsFragment.KEY_COLOR_BG_EQ,
                SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_EQ,
                16f));
        card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12),
                AndroidUtilities.dp(14), AndroidUtilities.dp(12));

        TextView label = new TextView(context);
        label.setText(labelText);
        label.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 14);
        label.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        card.addView(label,
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                        0, 0, 0, 10));

        int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);

        // Обёртка — как barFrame у вертикальных полос
        FrameLayout sliderFrame = new FrameLayout(context);

        // Фоновая дорожка — точно как trackView у вертикальных (accent с alpha 35)
        View trackView = new View(context);
        android.graphics.drawable.GradientDrawable trackBg = new android.graphics.drawable.GradientDrawable();
        trackBg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(accentColor, 35));
        trackBg.setCornerRadius(AndroidUtilities.dp(4));
        trackView.setBackground(trackBg);
        // Горизонтальная дорожка: высота 8dp, полная ширина, по центру
        sliderFrame.addView(trackView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(8), Gravity.CENTER_VERTICAL));

        android.widget.SeekBar bar = new android.widget.SeekBar(context);
        bar.setMax(max);
        bar.setProgress(progress);
        // Делаем стандартный трек прозрачным — видна только наша дорожка
        bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(accentColor));
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            bar.setBackground(null);
        }
        // Thumb — точно как у вертикальных полос
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            android.graphics.drawable.GradientDrawable thumbBg = new android.graphics.drawable.GradientDrawable();
            thumbBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            thumbBg.setColor(accentColor);
            thumbBg.setSize(AndroidUtilities.dp(20), AndroidUtilities.dp(20));
            thumbBg.setStroke(AndroidUtilities.dp(3), 0xCCFFFFFF);
            bar.setThumb(thumbBg);
            bar.setThumbOffset(AndroidUtilities.dp(10));
        }
        bar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar s, int p, boolean fromUser) {
                if (fromUser) callback.onChange(label, p);
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar s) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar s) {}
        });
        sliderFrame.addView(bar, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL));
        card.addView(sliderFrame,
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return card;
    }
    interface SliderCallback { void onChange(TextView label, int progress); }
}