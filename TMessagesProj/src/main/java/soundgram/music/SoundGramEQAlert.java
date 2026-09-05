package soundgram.music;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import soundgram.ui.SoundGramStyleManager;
import soundgram.ui.SoundGramStyleSettingsFragment;

import java.util.ArrayList;

/**
 * SoundGramEQAlert — Компактный всплывающий BottomSheet эквалайзер.
 * Кнопка "+ Сохранить" идет первой, затем идут пользовательские пресеты, затем встроенные.
 * Оснащен 10-полосным гибким графиком АЧХ с мгновенным откликом при драге.
 */
public class SoundGramEQAlert extends BottomSheet {

    private Switch eqMasterSwitch;
    private TextView masterStatusSub;
    private View statusDot;

    private EqCurveView eqCurveView;
    private LinearLayout presetsContainer;

    private SeekBar virtualizerSeekBar;
    private TextView virtualizerLabel;
    private SeekBar bassSeekBar;
    private TextView bassLabel;
    private SeekBar loudnessSeekBar;
    private TextView loudnessLabel;

    private SeekBar speedSeekBar;
    private TextView speedLabel;
    private SeekBar pitchSeekBar;
    private TextView pitchLabel;

    private LinearLayout reverbChipsList;

    public SoundGramEQAlert(Context context) {
        super(context, true);
        initView(context);
    }

    private void initView(Context context) {
        int sessionId = 0;
        try { sessionId = MediaController.getInstance().getAudioSessionId(); } catch (Exception ignored) {}
        SoundGramAudioFX.initFX(sessionId);

        int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);

        // Главный контейнер BottomSheet (компактные отступы)
        LinearLayout rootContainer = new LinearLayout(context);
        rootContainer.setOrientation(LinearLayout.VERTICAL);
        rootContainer.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(16));

        // 0. Полоса-хэндл
        View handleView = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_sheet_scrollUp), 100));
        handleBg.setCornerRadius(AndroidUtilities.dp(3));
        handleView.setBackground(handleBg);
        rootContainer.addView(handleView, LayoutHelper.createLinear(32, 4, Gravity.CENTER_HORIZONTAL, 0, 2, 0, 8));

        // 1. Компактный заголовок
        LinearLayout heroCard = new LinearLayout(context);
        heroCard.setOrientation(LinearLayout.HORIZONTAL);
        heroCard.setGravity(Gravity.CENTER_VERTICAL);
        heroCard.setBackground(SoundGramStyleManager.buildGlassDrawable(
                SoundGramStyleSettingsFragment.KEY_GLASS_EQ,
                SoundGramStyleSettingsFragment.KEY_ALPHA_EQ,
                SoundGramStyleSettingsFragment.KEY_COLOR_BG_EQ,
                SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_EQ,
                12f));
        heroCard.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));

        LinearLayout heroTextCol = new LinearLayout(context);
        heroTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView heroTitle = new TextView(context);
        heroTitle.setText(LocaleController.getString(R.string.sg_eq_hero_title));
        heroTitle.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        heroTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        heroTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        heroTextCol.addView(heroTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout statusRow = new LinearLayout(context);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);

        statusDot = new View(context);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(SoundGramAudioFX.isEqualizerEnabled() ? 0xFF00E676 : 0xFF888888);
        statusDot.setBackground(dotBg);
        statusRow.addView(statusDot, LayoutHelper.createLinear(7, 7, Gravity.CENTER_VERTICAL, 0, 0, 5, 0));

        masterStatusSub = new TextView(context);
        updateMasterStatusText(SoundGramAudioFX.isEqualizerEnabled());
        masterStatusSub.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        masterStatusSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        statusRow.addView(masterStatusSub, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        heroTextCol.addView(statusRow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 1, 0, 0));
        heroCard.addView(heroTextCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        eqMasterSwitch = new Switch(context);
        boolean enabled = SoundGramAudioFX.isEqualizerEnabled();
        eqMasterSwitch.setChecked(enabled);
        eqMasterSwitch.setContentDescription(LocaleController.getString(R.string.sg_eq_hero_switch_descr));
        eqMasterSwitch.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        if (Build.VERSION.SDK_INT >= 23) {
            android.content.res.ColorStateList thumbCsl = new android.content.res.ColorStateList(
                    new int[][]{ new int[]{ android.R.attr.state_checked }, new int[]{} },
                    new int[]{ accentColor, 0xFFCCCCCC });
            android.content.res.ColorStateList trackCsl = new android.content.res.ColorStateList(
                    new int[][]{ new int[]{ android.R.attr.state_checked }, new int[]{} },
                    new int[]{ ColorUtils.setAlphaComponent(accentColor, 120), 0x44888888 });
            eqMasterSwitch.setThumbTintList(thumbCsl);
            eqMasterSwitch.setTrackTintList(trackCsl);
        }
        eqMasterSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            SoundGramAudioFX.setEqualizerEnabled(isChecked);
            updateMasterStatusText(isChecked);
            if (eqCurveView != null) eqCurveView.invalidate();
        });
        heroCard.addView(eqMasterSwitch, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        rootContainer.addView(heroCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        // 2. Скроллируемая область эквалайзера и эффектов
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);

        makeSectionHeader(context, contentLayout, LocaleController.getString(R.string.sg_eq_section_graph));

        LinearLayout curveCard = new LinearLayout(context);
        curveCard.setOrientation(LinearLayout.VERTICAL);
        curveCard.setBackground(SoundGramStyleManager.buildGlassDrawable(
                SoundGramStyleSettingsFragment.KEY_GLASS_EQ,
                SoundGramStyleSettingsFragment.KEY_ALPHA_EQ,
                SoundGramStyleSettingsFragment.KEY_COLOR_BG_EQ,
                SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_EQ,
                12f));
        curveCard.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));

        eqCurveView = new EqCurveView(context);
        eqCurveView.setOnBandChangedListener(() -> { /* invalidate only, no preset rebuild during drag */ });
        curveCard.addView(eqCurveView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 145));
        contentLayout.addView(curveCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        makeSectionHeader(context, contentLayout, LocaleController.getString(R.string.sg_eq_section_presets));

        HorizontalScrollView eqPresetScroll = new HorizontalScrollView(context);
        eqPresetScroll.setHorizontalScrollBarEnabled(false);
        presetsContainer = new LinearLayout(context);
        presetsContainer.setOrientation(LinearLayout.HORIZONTAL);
        eqPresetScroll.addView(presetsContainer);
        contentLayout.addView(eqPresetScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        makeSectionHeader(context, contentLayout, LocaleController.getString(R.string.sg_eq_section_bass));

        virtualizerLabel = createSliderLabel(context);
        virtualizerSeekBar = createSliderBar(context, 1000, SoundGramAudioFX.getVirtualizerLevel());
        virtualizerSeekBar.setContentDescription(LocaleController.getString(R.string.sg_eq_virtualizer));
        virtualizerSeekBar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        virtualizerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) {
                    SoundGramAudioFX.setVirtualizerLevel((short) p);
                    virtualizerLabel.setText(LocaleController.formatString("sg_eq_virtualizer_label", R.string.sg_eq_virtualizer_label, Math.round(p / 10.0f)));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        contentLayout.addView(makeSliderCard(context, virtualizerLabel, virtualizerSeekBar),
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        bassLabel = createSliderLabel(context);
        bassSeekBar = createSliderBar(context, 1000, SoundGramAudioFX.getBassLevel());
        bassSeekBar.setContentDescription(LocaleController.getString(R.string.sg_eq_bass));
        bassSeekBar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        bassSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) {
                    SoundGramAudioFX.setBassLevel((short) p);
                    bassLabel.setText(LocaleController.formatString("sg_eq_bass_label", R.string.sg_eq_bass_label, Math.round(p / 10.0f)));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        contentLayout.addView(makeSliderCard(context, bassLabel, bassSeekBar),
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        if (Build.VERSION.SDK_INT >= 19) {
            loudnessLabel = createSliderLabel(context);
            loudnessSeekBar = createSliderBar(context, 1500, SoundGramAudioFX.getLoudnessGain());
            loudnessSeekBar.setContentDescription(LocaleController.getString(R.string.sg_eq_loudness));
            loudnessSeekBar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            loudnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                    if (fromUser) {
                        SoundGramAudioFX.setLoudnessGain(p);
                        loudnessLabel.setText(LocaleController.formatString("sg_eq_loudness_label", R.string.sg_eq_loudness_label, p / 100.0f));
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar s) {}
                @Override public void onStopTrackingTouch(SeekBar s) {}
            });
            contentLayout.addView(makeSliderCard(context, loudnessLabel, loudnessSeekBar),
                    LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
        }

        makeSectionHeader(context, contentLayout, LocaleController.getString(R.string.sg_eq_section_speed_pitch));

        speedLabel = createSliderLabel(context);
        speedSeekBar = createSliderBar(context, 19, Math.round((SoundGramAudioFX.getCurrentSpeed() - 0.1f) / 0.1f));
        speedSeekBar.setContentDescription(LocaleController.getString(R.string.sg_eq_speed_descr));
        speedSeekBar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) {
                    float spd = 0.1f + (p * 0.1f);
                    SoundGramAudioFX.setCurrentSpeed(spd);
                    speedLabel.setText(String.format(LocaleController.getString(R.string.sg_eq_speed), spd));
                    try { MediaController.getInstance().applyMusicPlaybackParams(spd, SoundGramAudioFX.getCurrentPitch()); } catch (Exception ignored) {}
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        contentLayout.addView(makeSliderCard(context, speedLabel, speedSeekBar),
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        pitchLabel = createSliderLabel(context);
        pitchSeekBar = createSliderBar(context, 19, Math.round((SoundGramAudioFX.getCurrentPitch() - 0.1f) / 0.1f));
        pitchSeekBar.setContentDescription(LocaleController.getString(R.string.sg_eq_pitch_descr));
        pitchSeekBar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        pitchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) {
                    float ptc = 0.1f + (p * 0.1f);
                    SoundGramAudioFX.setCurrentPitch(ptc);
                    pitchLabel.setText(String.format(LocaleController.getString(R.string.sg_eq_pitch), ptc));
                    try { MediaController.getInstance().applyMusicPlaybackParams(SoundGramAudioFX.getCurrentSpeed(), ptc); } catch (Exception ignored) {}
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        contentLayout.addView(makeSliderCard(context, pitchLabel, pitchSeekBar),
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        makeSectionHeader(context, contentLayout, LocaleController.getString(R.string.sg_eq_section_rules));
        HorizontalScrollView modeScroll = new HorizontalScrollView(context);
        modeScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout modeList = new LinearLayout(context);
        modeList.setOrientation(LinearLayout.HORIZONTAL);

        String[] modes = {
            LocaleController.getString(R.string.sg_eq_rule_slowed),
            LocaleController.getString(R.string.sg_eq_rule_nightcore),
            LocaleController.getString(R.string.sg_eq_rule_concert),
            LocaleController.getString(R.string.sg_eq_rule_bassmax)
        };
        String[] modeKeys = {"slowed", "nightcore", "concert", "bassmax"};

        for (int i = 0; i < modes.length; i++) {
            final String key = modeKeys[i];
            TextView modeChip = new TextView(context);
            modeChip.setText(modes[i]);
            modeChip.setContentDescription(modes[i]);
            modeChip.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            modeChip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
            modeChip.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            modeChip.setTextColor(0xFFFFFFFF);
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(Theme.getColor(Theme.key_chats_actionBackground));
            chipBg.setCornerRadius(AndroidUtilities.dp(10));
            modeChip.setBackground(chipBg);
            modeChip.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
            modeChip.setOnClickListener(v -> {
                SoundGramAudioFX.applyQuickMode(key);
                updateAllUI();
            });
            modeList.addView(modeChip, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));
        }
        modeScroll.addView(modeList);
        contentLayout.addView(modeScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        makeSectionHeader(context, contentLayout, LocaleController.getString(R.string.sg_eq_section_reverb));
        HorizontalScrollView reverbScroll = new HorizontalScrollView(context);
        reverbScroll.setHorizontalScrollBarEnabled(false);
        reverbChipsList = new LinearLayout(context);
        reverbChipsList.setOrientation(LinearLayout.HORIZONTAL);
        reverbScroll.addView(reverbChipsList);
        contentLayout.addView(reverbScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        ScrollView mainScrollView = new ScrollView(context);
        mainScrollView.setVerticalScrollBarEnabled(false);
        mainScrollView.addView(contentLayout);

        rootContainer.addView(mainScrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setCustomView(rootContainer);
        updateAllUI();
    }

    private void updateMasterStatusText(boolean isChecked) {
        if (masterStatusSub != null) {
            masterStatusSub.setText(LocaleController.getString(isChecked ? R.string.sg_eq_status_active : R.string.sg_eq_status_disabled));
        }
        if (statusDot != null) {
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(isChecked ? 0xFF00E676 : 0xFF888888);
            statusDot.setBackground(dotBg);
        }
    }

    /**
     * EqCurveView — 10-полосный интерактивный график АЧХ
     */
    public static class EqCurveView extends View {
        private Paint curvePaint;
        private Paint glowCurvePaint;
        private Paint fillPaint;
        private Paint gridPaint;
        private Paint nodePaint;
        private Paint nodeGlowPaint;
        private Paint textPaint;
        private Paint tooltipBgPaint;
        private Paint tooltipTextPaint;

        private int activeDragBand = -1;
        private long lastTapTime = 0;

        public interface OnBandChangedListener {
            void onBandChanged();
        }

        private OnBandChangedListener onBandChangedListener;

        public EqCurveView(Context context) {
            super(context);
            init();
        }

        public void setOnBandChangedListener(OnBandChangedListener listener) {
            this.onBandChangedListener = listener;
        }

        private void init() {
            int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);

            curvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            curvePaint.setStyle(Paint.Style.STROKE);
            curvePaint.setStrokeWidth(AndroidUtilities.dp(3f));
            curvePaint.setColor(accentColor);

            glowCurvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            glowCurvePaint.setStyle(Paint.Style.STROKE);
            glowCurvePaint.setStrokeWidth(AndroidUtilities.dp(7.5f));
            glowCurvePaint.setColor(ColorUtils.setAlphaComponent(accentColor, 90));

            fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fillPaint.setStyle(Paint.Style.FILL);

            gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            gridPaint.setStyle(Paint.Style.STROKE);
            gridPaint.setStrokeWidth(AndroidUtilities.dp(0.75f));
            gridPaint.setColor(ColorUtils.setAlphaComponent(accentColor, 35));

            nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            nodePaint.setStyle(Paint.Style.FILL);

            nodeGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            nodeGlowPaint.setStyle(Paint.Style.FILL);

            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize(AndroidUtilities.dp(9f));
            textPaint.setColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_dialogTextBlack), 150));
            textPaint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));

            tooltipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            tooltipBgPaint.setColor(accentColor);

            tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            tooltipTextPaint.setTextSize(AndroidUtilities.dp(9.5f));
            tooltipTextPaint.setColor(0xFFFFFFFF);
            tooltipTextPaint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            tooltipTextPaint.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return false;

            short count = SoundGramAudioFX.getBandsCount();
            if (count <= 0) count = 10;

            int paddingX = AndroidUtilities.dp(28);
            int paddingTop = AndroidUtilities.dp(18);
            int paddingBottom = AndroidUtilities.dp(22);
            int usableW = w - paddingX * 2;
            int usableH = h - paddingTop - paddingBottom;

            float touchX = event.getX();
            float touchY = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    int closest = -1;
                    float minDist = Float.MAX_VALUE;
                    for (int i = 0; i < count; i++) {
                        float nodeX = paddingX + (float) i / (count - 1) * usableW;
                        float dist = Math.abs(touchX - nodeX);
                        if (dist < minDist) {
                            minDist = dist;
                            closest = i;
                        }
                    }
                    if (closest != -1 && minDist < AndroidUtilities.dp(36)) {
                        activeDragBand = closest;
                        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);

                        long now = System.currentTimeMillis();
                        if (now - lastTapTime < 300) {
                            SoundGramAudioFX.setBandLevel((short) activeDragBand, (short) 0);
                            if (onBandChangedListener != null) onBandChangedListener.onBandChanged();
                            activeDragBand = -1;
                            invalidate();
                            return true;
                        }
                        lastTapTime = now;

                        updateBandFromTouch(activeDragBand, touchY, paddingTop, usableH);
                        invalidate();
                        return true;
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (activeDragBand >= 0 && activeDragBand < count) {
                        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                        updateBandFromTouch(activeDragBand, touchY, paddingTop, usableH);
                        invalidate();
                        return true;
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    activeDragBand = -1;
                    invalidate();
                    if (onBandChangedListener != null) onBandChangedListener.onBandChanged();
                    return true;
                }
            }
            return super.onTouchEvent(event);
        }

        private void updateBandFromTouch(int band, float touchY, int paddingTop, int usableH) {
            float clampedY = Math.max(paddingTop, Math.min(paddingTop + usableH, touchY));
            float centerY = paddingTop + usableH / 2.0f;
            float norm = (centerY - clampedY) / (usableH / 2.0f); // -1.0 .. +1.0
            short newLevel = (short) Math.round(norm * 1500f);
            SoundGramAudioFX.setBandLevel((short) band, newLevel);
            if (onBandChangedListener != null) {
                onBandChangedListener.onBandChanged();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            short count = SoundGramAudioFX.getBandsCount();
            if (count <= 0) count = 10;

            short[] bandLevels = SoundGramAudioFX.getCurrentBandLevels();

            int paddingX = AndroidUtilities.dp(28);
            int paddingTop = AndroidUtilities.dp(18);
            int paddingBottom = AndroidUtilities.dp(22);
            int usableW = w - paddingX * 2;
            int usableH = h - paddingTop - paddingBottom;
            int centerY = paddingTop + usableH / 2;

            canvas.drawLine(paddingX, centerY, w - paddingX, centerY, gridPaint);
            canvas.drawLine(paddingX, paddingTop, w - paddingX, paddingTop, gridPaint);
            canvas.drawLine(paddingX, h - paddingBottom, w - paddingX, h - paddingBottom, gridPaint);

            canvas.drawText("+15 dB", AndroidUtilities.dp(2), paddingTop + AndroidUtilities.dp(5), textPaint);
            canvas.drawText("0 dB", AndroidUtilities.dp(2), centerY + AndroidUtilities.dp(3), textPaint);
            canvas.drawText("-15 dB", AndroidUtilities.dp(2), h - paddingBottom, textPaint);

            float[] pointsX = new float[count];
            float[] pointsY = new float[count];

            for (int i = 0; i < count; i++) {
                pointsX[i] = paddingX + (float) i / (count - 1) * usableW;
                short lvl = (i < bandLevels.length) ? bandLevels[i] : 0;
                float norm = (float) lvl / 1500.0f; // -1.0..+1.0
                norm = Math.max(-1.0f, Math.min(1.0f, norm));
                pointsY[i] = centerY - norm * (usableH / 2.0f);

                int freq = SoundGramAudioFX.getCenterFreq((short) i);
                String freqStr = freq >= 1000 ? (freq / 1000) + "k" : freq + "";
                textPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(freqStr, pointsX[i], h - paddingBottom + AndroidUtilities.dp(14), textPaint);
            }

            Path curvePath = new Path();
            Path fillPath = new Path();

            curvePath.moveTo(pointsX[0], pointsY[0]);
            fillPath.moveTo(pointsX[0], centerY);
            fillPath.lineTo(pointsX[0], pointsY[0]);

            for (int i = 0; i < count - 1; i++) {
                float p0x = pointsX[Math.max(0, i - 1)];
                float p0y = pointsY[Math.max(0, i - 1)];
                float p1x = pointsX[i];
                float p1y = pointsY[i];
                float p2x = pointsX[i + 1];
                float p2y = pointsY[i + 1];
                float p3x = pointsX[Math.min(count - 1, i + 2)];
                float p3y = pointsY[Math.min(count - 1, i + 2)];

                float cp1x = p1x + (p2x - p0x) / 6.0f;
                float cp1y = p1y + (p2y - p0y) / 6.0f;
                float cp2x = p2x - (p3x - p1x) / 6.0f;
                float cp2y = p2y - (p3y - p1y) / 6.0f;

                curvePath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2x, p2y);
                fillPath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2x, p2y);
            }

            fillPath.lineTo(pointsX[count - 1], centerY);
            fillPath.close();

            int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);
            int secondColor = 0xFF9C27B0;

            LinearGradient fillGrad = new LinearGradient(
                    0, paddingTop, 0, h - paddingBottom,
                    new int[]{ ColorUtils.setAlphaComponent(accentColor, 130), ColorUtils.setAlphaComponent(secondColor, 60), ColorUtils.setAlphaComponent(accentColor, 5) },
                    null, Shader.TileMode.CLAMP
            );
            fillPaint.setShader(fillGrad);
            canvas.drawPath(fillPath, fillPaint);

            canvas.drawPath(curvePath, glowCurvePaint);
            canvas.drawPath(curvePath, curvePaint);

            for (int i = 0; i < count; i++) {
                boolean isActive = (activeDragBand == i);
                nodeGlowPaint.setColor(ColorUtils.setAlphaComponent(accentColor, isActive ? 170 : 100));
                canvas.drawCircle(pointsX[i], pointsY[i], AndroidUtilities.dp(isActive ? 9.5f : 6.5f), nodeGlowPaint);

                nodePaint.setColor(accentColor);
                canvas.drawCircle(pointsX[i], pointsY[i], AndroidUtilities.dp(isActive ? 5.5f : 4f), nodePaint);
                nodePaint.setColor(0xFFFFFFFF);
                canvas.drawCircle(pointsX[i], pointsY[i], AndroidUtilities.dp(isActive ? 2.5f : 1.8f), nodePaint);

                if (isActive) {
                    short lvl = (i < bandLevels.length) ? bandLevels[i] : 0;
                    float dbVal = lvl / 100.0f;
                    String tooltipStr = String.format("%+.1f dB", dbVal);
                    float tooltipW = AndroidUtilities.dp(44);
                    float tooltipH = AndroidUtilities.dp(18);
                    float rectLeft = pointsX[i] - tooltipW / 2f;
                    float rectTop = pointsY[i] - AndroidUtilities.dp(23);
                    RectF tipRect = new RectF(rectLeft, rectTop, rectLeft + tooltipW, rectTop + tooltipH);
                    canvas.drawRoundRect(tipRect, AndroidUtilities.dp(5), AndroidUtilities.dp(5), tooltipBgPaint);
                    canvas.drawText(tooltipStr, pointsX[i], rectTop + AndroidUtilities.dp(13), tooltipTextPaint);
                }
            }
        }
    }

    private void makeSectionHeader(Context context, LinearLayout parent, String title) {
        TextView header = new TextView(context);
        header.setText(title);
        header.setTextColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_dialogTextBlack), 170));
        header.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10.5f);
        header.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        parent.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 4));
    }

    private TextView createSliderLabel(Context context) {
        TextView label = new TextView(context);
        label.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        label.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        return label;
    }

    private SeekBar createSliderBar(Context context, int max, int progress) {
        int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);
        SeekBar bar = new SeekBar(context);
        bar.setMax(max);
        bar.setProgress(progress);
        bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(accentColor));
        if (Build.VERSION.SDK_INT >= 21) {
            bar.setBackground(null);
            GradientDrawable thumbBg = new GradientDrawable();
            thumbBg.setShape(GradientDrawable.OVAL);
            thumbBg.setColor(accentColor);
            thumbBg.setSize(AndroidUtilities.dp(15), AndroidUtilities.dp(15));
            thumbBg.setStroke(AndroidUtilities.dp(2f), 0xFFFFFFFF);
            bar.setThumb(thumbBg);
            bar.setThumbOffset(AndroidUtilities.dp(7.5f));
        }
        return bar;
    }

    private View makeSliderCard(Context context, TextView label, SeekBar bar) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(SoundGramStyleManager.buildGlassDrawable(
                SoundGramStyleSettingsFragment.KEY_GLASS_EQ,
                SoundGramStyleSettingsFragment.KEY_ALPHA_EQ,
                SoundGramStyleSettingsFragment.KEY_COLOR_BG_EQ,
                SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_EQ,
                10f));
        card.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(7), AndroidUtilities.dp(10), AndroidUtilities.dp(7));

        card.addView(label, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);
        FrameLayout sliderFrame = new FrameLayout(context);
        View trackView = new View(context);
        GradientDrawable trackBg = new GradientDrawable();
        trackBg.setColor(ColorUtils.setAlphaComponent(accentColor, 35));
        trackBg.setCornerRadius(AndroidUtilities.dp(3));
        trackView.setBackground(trackBg);
        sliderFrame.addView(trackView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(5), Gravity.CENTER_VERTICAL));
        sliderFrame.addView(bar, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        card.addView(sliderFrame, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return card;
    }

    private void updateAllUI() {
        if (getContext() == null) return;
        boolean enabled = SoundGramAudioFX.isEqualizerEnabled();
        eqMasterSwitch.setChecked(enabled);
        updateMasterStatusText(enabled);

        refreshPresetsList();
        refreshReverbList();
        if (eqCurveView != null) eqCurveView.invalidate();

        short virtLvl = SoundGramAudioFX.getVirtualizerLevel();
        virtualizerSeekBar.setProgress(virtLvl);
        virtualizerLabel.setText(LocaleController.formatString("sg_eq_virtualizer_label", R.string.sg_eq_virtualizer_label, Math.round(virtLvl / 10.0f)));

        short bassLvl = SoundGramAudioFX.getBassLevel();
        bassSeekBar.setProgress(bassLvl);
        bassLabel.setText(LocaleController.formatString("sg_eq_bass_label", R.string.sg_eq_bass_label, Math.round(bassLvl / 10.0f)));

        if (Build.VERSION.SDK_INT >= 19 && loudnessSeekBar != null) {
            int gain = SoundGramAudioFX.getLoudnessGain();
            loudnessSeekBar.setProgress(gain);
            loudnessLabel.setText(LocaleController.formatString("sg_eq_loudness_label", R.string.sg_eq_loudness_label, gain / 100.0f));
        }

        float spd = SoundGramAudioFX.getCurrentSpeed();
        speedSeekBar.setProgress(Math.round((spd - 0.1f) / 0.1f));
        speedLabel.setText(String.format(LocaleController.getString(R.string.sg_eq_speed), spd));

        float ptc = SoundGramAudioFX.getCurrentPitch();
        pitchSeekBar.setProgress(Math.round((ptc - 0.1f) / 0.1f));
        pitchLabel.setText(String.format(LocaleController.getString(R.string.sg_eq_pitch), ptc));
    }

    /**
     * Порядок списка пресетов:
     * 1. ПЕРВАЯ: Кнопка "+ Сохранить"
     * 2. ВТОРЫЕ: Пользовательские пресеты
     * 3. ТРЕТЬИ: Встроенные пресеты
     */
    private void refreshPresetsList() {
        if (presetsContainer == null || getContext() == null) return;
        presetsContainer.removeAllViews();

        int currentPreset = SoundGramAudioFX.getCurrentEqPreset();
        int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);

        // 1. ПЕРВАЯ: Кнопка "+ Сохранить"
        TextView addPresetBtn = new TextView(getContext());
        addPresetBtn.setText(LocaleController.getString(R.string.sg_eq_add_preset));
        addPresetBtn.setContentDescription(LocaleController.getString(R.string.sg_eq_add_preset_descr));
        addPresetBtn.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        addPresetBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
        addPresetBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        addPresetBtn.setTextColor(accentColor);
        GradientDrawable addBg = new GradientDrawable();
        addBg.setColor(ColorUtils.setAlphaComponent(accentColor, 35));
        addBg.setCornerRadius(AndroidUtilities.dp(10));
        addBg.setStroke(AndroidUtilities.dp(1), ColorUtils.setAlphaComponent(accentColor, 120));
        addPresetBtn.setBackground(addBg);
        addPresetBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
        addPresetBtn.setOnClickListener(v -> promptSaveCustomPreset());
        presetsContainer.addView(addPresetBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));

        // 2. ВТОРЫЕ: Пользовательские пресеты
        ArrayList<SoundGramAudioFX.CustomPreset> customPresets = SoundGramAudioFX.getCustomPresets();
        for (int i = 0; i < customPresets.size(); i++) {
            final int customIdx = i;
            final SoundGramAudioFX.CustomPreset cp = customPresets.get(i);
            boolean isSel = (currentPreset == 100 + customIdx);

            LinearLayout customChip = new LinearLayout(getContext());
            customChip.setOrientation(LinearLayout.HORIZONTAL);
            customChip.setGravity(Gravity.CENTER_VERTICAL);
            customChip.setContentDescription(LocaleController.formatString("sg_eq_custom_preset_descr", R.string.sg_eq_custom_preset_descr, cp.name));
            customChip.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            GradientDrawable cBg = new GradientDrawable();
            cBg.setColor(isSel ? Theme.getColor(Theme.key_chats_actionBackground) : ColorUtils.setAlphaComponent(accentColor, 40));
            cBg.setCornerRadius(AndroidUtilities.dp(10));
            customChip.setBackground(cBg);
            customChip.setPadding(AndroidUtilities.dp(9), AndroidUtilities.dp(5), AndroidUtilities.dp(5), AndroidUtilities.dp(5));

            TextView cpName = new TextView(getContext());
            cpName.setText("⭐ " + cp.name);
            cpName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
            cpName.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            cpName.setTextColor(isSel ? 0xFFFFFFFF : Theme.getColor(Theme.key_dialogTextBlack));
            customChip.addView(cpName, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            TextView delBtn = new TextView(getContext());
            delBtn.setText(" ✕");
            delBtn.setContentDescription(LocaleController.formatString("sg_eq_delete_preset_descr", R.string.sg_eq_delete_preset_descr, cp.name));
            delBtn.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            delBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
            delBtn.setTextColor(0xFFFF5252);
            delBtn.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
            delBtn.setOnClickListener(v -> {
                new AlertDialog.Builder(getContext())
                        .setTitle(LocaleController.getString(R.string.sg_eq_delete_preset_title))
                        .setMessage(LocaleController.formatString("sg_eq_delete_preset_msg", R.string.sg_eq_delete_preset_msg, cp.name))
                        .setPositiveButton(LocaleController.getString(R.string.Delete), (dialog, which) -> {
                            SoundGramAudioFX.deleteCustomPreset(customIdx);
                            refreshPresetsList();
                        })
                        .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                        .show();
            });
            customChip.addView(delBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            customChip.setOnClickListener(v -> {
                SoundGramAudioFX.applyCustomPreset(cp, customIdx);
                updateAllUI();
            });
            presetsContainer.addView(customChip, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));
        }

        // 3. ТРЕТЬИ: Встроенные пресеты
        for (int i = 0; i < SoundGramAudioFX.EQ_PRESET_NAMES.length; i++) {
            final int presetIdx = i;
            TextView presetChip = new TextView(getContext());
            presetChip.setText(SoundGramAudioFX.EQ_PRESET_NAMES[i]);
            presetChip.setContentDescription(LocaleController.formatString("sg_eq_preset_descr", R.string.sg_eq_preset_descr, SoundGramAudioFX.EQ_PRESET_NAMES[i]));
            presetChip.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            presetChip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
            presetChip.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            boolean isSel = (currentPreset == presetIdx);

            presetChip.setTextColor(isSel ? 0xFFFFFFFF : Theme.getColor(Theme.key_dialogTextBlack));
            GradientDrawable pBg = new GradientDrawable();
            pBg.setColor(isSel ? Theme.getColor(Theme.key_chats_actionBackground) : ColorUtils.setAlphaComponent(accentColor, 20));
            pBg.setCornerRadius(AndroidUtilities.dp(10));
            presetChip.setBackground(pBg);
            presetChip.setPadding(AndroidUtilities.dp(9), AndroidUtilities.dp(5), AndroidUtilities.dp(9), AndroidUtilities.dp(5));
            presetChip.setOnClickListener(v -> {
                SoundGramAudioFX.applyEqPreset(presetIdx);
                updateAllUI();
            });
            presetsContainer.addView(presetChip, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));
        }
    }

    private void promptSaveCustomPreset() {
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(LocaleController.getString(R.string.sg_eq_save_preset_title));

        final EditText input = new EditText(getContext());
        input.setHint(LocaleController.getString(R.string.sg_eq_preset_name_hint));
        FrameLayout container = new FrameLayout(getContext());
        container.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(6), AndroidUtilities.dp(18), AndroidUtilities.dp(6));
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                SoundGramAudioFX.saveCustomPreset(name);
                refreshPresetsList();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private void refreshReverbList() {
        if (reverbChipsList == null || getContext() == null) return;
        reverbChipsList.removeAllViews();

        short currentReverb = SoundGramAudioFX.getReverbPreset();
        int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);

        String[] icons = {"🚫", "🛖", "🏠", "🏛️", "🏟️", "🎼", "📻"};

        for (int i = 0; i < SoundGramAudioFX.REVERB_PRESET_VALUES.length; i++) {
            final short presetVal = SoundGramAudioFX.REVERB_PRESET_VALUES[i];
            TextView rChip = new TextView(getContext());
            String icon = i < icons.length ? icons[i] + " " : "";
            String rName = SoundGramAudioFX.getLocalizedReverbPresetName(i);
            rChip.setText(icon + rName);
            rChip.setContentDescription(LocaleController.formatString("sg_eq_reverb_descr", R.string.sg_eq_reverb_descr, rName));
            rChip.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            rChip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
            rChip.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            boolean isSel = (currentReverb == presetVal);

            rChip.setTextColor(isSel ? 0xFFFFFFFF : Theme.getColor(Theme.key_dialogTextBlack));
            GradientDrawable rBg = new GradientDrawable();
            rBg.setColor(isSel ? Theme.getColor(Theme.key_chats_actionBackground) : ColorUtils.setAlphaComponent(accentColor, 20));
            rBg.setCornerRadius(AndroidUtilities.dp(10));
            rChip.setBackground(rBg);
            rChip.setPadding(AndroidUtilities.dp(9), AndroidUtilities.dp(5), AndroidUtilities.dp(9), AndroidUtilities.dp(5));
            rChip.setOnClickListener(v -> {
                SoundGramAudioFX.setReverbPreset(presetVal);
                updateAllUI();
            });
            reverbChipsList.addView(rChip, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));
        }
    }
}