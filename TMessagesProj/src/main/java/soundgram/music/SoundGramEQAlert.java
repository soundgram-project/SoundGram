package soundgram.music;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import soundgram.ui.SoundGramDesignTokens;

import java.util.ArrayList;
import java.util.Locale;

/**
 * SoundGramEQAlert — Эквалайзер и AudioFX SoundGram.
 * Точь в точь по дизайн-системе SoundGram и макету:
 * 1. Фон #111111, скругление сверху 28dp.
 * 2. Шапка: моно «звук» слева, чип «включён»/«выключен» справа.
 * 3. Крупное значение выбранной точки «+5,0» (44 px) и частота «910 гц · дб» моно.
 * 4. График АЧХ по 5 точкам (60, 230, 910, 3к, 14к) со сглаженной кривой, заливкой 14%
 *    и перетаскиваемыми точками (выбранная точка закрашена, остальные — кольца).
 * 5. Пресеты (Flat, Rock, Pop, Бас, zxc) в горизонтальном ряду с мини-кривыми.
 * 6. Две круглые ручки 88 px: «бас» (47%) и «3D-звук» (100%).
 * 7. Ползунок скорости «0,85×» и быстрые чипы «slowed + reverb», «nightcore».
 */
public class SoundGramEQAlert extends BottomSheet {

    private TextView statusChip;
    private TextView bigGainText;
    private TextView freqDetailText;
    private EqCurveView eqCurveView;
    private LinearLayout presetsContainer;
    private KnobView bassKnob;
    private TextView bassValText;
    private KnobView virtKnob;
    private TextView virtValText;
    private SeekBar speedSeekBar;
    private TextView speedValText;
    private SeekBar pitchSeekBar;
    private TextView pitchValText;
    private TextView chipSlowed;
    private TextView chipNightcore;

    public SoundGramEQAlert(Context context) {
        super(context, true);
        initView(context);
    }

    private void initView(Context context) {
        int sessionId = 0;
        try { sessionId = MediaController.getInstance().getAudioSessionId(); } catch (Exception ignored) {}
        SoundGramAudioFX.initFX(sessionId, false);

        // Root background: dynamic theme with top corners 28dp
        setApplyTopPadding(false);
        setApplyBottomPadding(false);

        FrameLayout rootSheet = new FrameLayout(context);
        rootSheet.setBackground(SoundGramDesignTokens.makeAsymmetricDrawable(SoundGramDesignTokens.getBgColor(), 28, 28, 0, 0));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setFillViewport(true);

        LinearLayout contentCol = new LinearLayout(context);
        contentCol.setOrientation(LinearLayout.VERTICAL);
        contentCol.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(12), AndroidUtilities.dp(18), AndroidUtilities.dp(28));

        // 0. Drag handle
        View handleView = new View(context);
        handleView.setBackground(SoundGramDesignTokens.makePillDrawable(SoundGramDesignTokens.getLineStrongColor(), 2));
        contentCol.addView(handleView, LayoutHelper.createLinear(36, 4, Gravity.CENTER_HORIZONTAL, 0, 2, 0, 16));

        // 1. Top row: «звук» (left) · чип «включён» / «выключен» (right)
        LinearLayout topHeaderRow = new LinearLayout(context);
        topHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        topHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(context);
        titleView.setText("звук");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        titleView.setTypeface(getMonoTypeface());
        titleView.setTextColor(SoundGramDesignTokens.getTextMutedColor());
        titleView.setLetterSpacing(0.02f);
        topHeaderRow.addView(titleView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        statusChip = new TextView(context);
        statusChip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        statusChip.setTypeface(getMonoTypeface());
        statusChip.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(6));
        updateStatusChipStyle(SoundGramAudioFX.isEqualizerEnabled());
        statusChip.setOnClickListener(v -> {
            boolean newState = !SoundGramAudioFX.isEqualizerEnabled();
            SoundGramAudioFX.setEqualizerEnabled(newState);
            updateStatusChipStyle(newState);
            if (eqCurveView != null) eqCurveView.invalidate();
        });
        topHeaderRow.addView(statusChip, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        contentCol.addView(topHeaderRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        // 2. Big gain readout: «+5,0» (44px) + «910 гц · дб» (mono 13px)
        LinearLayout gainRow = new LinearLayout(context);
        gainRow.setOrientation(LinearLayout.HORIZONTAL);
        gainRow.setGravity(Gravity.BOTTOM);

        bigGainText = new TextView(context);
        bigGainText.setText("+5,0");
        bigGainText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 44);
        bigGainText.setTypeface(AndroidUtilities.bold());
        bigGainText.setTextColor(SoundGramDesignTokens.getTextColor());
        gainRow.addView(bigGainText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 10, 0));

        freqDetailText = new TextView(context);
        freqDetailText.setText("910 гц · дб");
        freqDetailText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        freqDetailText.setTypeface(getMonoTypeface());
        freqDetailText.setTextColor(SoundGramDesignTokens.getTextMutedColor());
        freqDetailText.setLetterSpacing(0.02f);
        gainRow.addView(freqDetailText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
        contentCol.addView(gainRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // 3. 5-Band Interactive Equalizer Curve
        eqCurveView = new EqCurveView(context);
        eqCurveView.setOnBandChangedListener((bandIndex, freqText, dbValue) -> {
            bigGainText.setText(formatDb(dbValue));
            freqDetailText.setText(freqText + " · дб");
            refreshPresetsActiveState(-1);
        });
        contentCol.addView(eqCurveView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 185, 0, 0, 0, 16));

        // 4. Presets section
        LinearLayout presetsHeaderRow = new LinearLayout(context);
        presetsHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        presetsHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView presetsHeader = new TextView(context);
        presetsHeader.setText("пресеты");
        presetsHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        presetsHeader.setTypeface(getMonoTypeface());
        presetsHeader.setTextColor(SoundGramDesignTokens.getTextMutedColor());
        presetsHeader.setLetterSpacing(0.02f);
        presetsHeaderRow.addView(presetsHeader, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        TextView savePresetBtn = new TextView(context);
        savePresetBtn.setText("+ сохранить");
        savePresetBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        savePresetBtn.setTypeface(getMonoTypeface());
        savePresetBtn.setTextColor(SoundGramDesignTokens.getAccentColor());
        savePresetBtn.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(4));
        savePresetBtn.setOnClickListener(v -> showSavePresetDialog());
        presetsHeaderRow.addView(savePresetBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        contentCol.addView(presetsHeaderRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        HorizontalScrollView presetsScroll = new HorizontalScrollView(context);
        presetsScroll.setHorizontalScrollBarEnabled(false);
        presetsContainer = new LinearLayout(context);
        presetsContainer.setOrientation(LinearLayout.HORIZONTAL);
        presetsScroll.addView(presetsContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));
        contentCol.addView(presetsScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 68, 0, 0, 0, 22));

        // 5. Knobs row: «бас» (47%) · «3D-звук» (100%)
        LinearLayout knobsRow = new LinearLayout(context);
        knobsRow.setOrientation(LinearLayout.HORIZONTAL);
        knobsRow.setGravity(Gravity.CENTER);

        // Bass Knob
        LinearLayout bassCol = new LinearLayout(context);
        bassCol.setOrientation(LinearLayout.VERTICAL);
        bassCol.setGravity(Gravity.CENTER_HORIZONTAL);

        bassKnob = new KnobView(context);
        bassKnob.setProgress(SoundGramAudioFX.getBassLevel() / 1000f);
        bassCol.addView(bassKnob, LayoutHelper.createLinear(88, 88));

        TextView bassLabel = new TextView(context);
        bassLabel.setText("бас");
        bassLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        bassLabel.setTypeface(getMonoTypeface());
        bassLabel.setTextColor(SoundGramDesignTokens.getTextMutedColor());
        bassCol.addView(bassLabel, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 2));

        bassValText = new TextView(context);
        bassValText.setText(Math.round(bassKnob.getProgress() * 100) + "%");
        bassValText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        bassValText.setTypeface(AndroidUtilities.bold());
        bassValText.setTextColor(SoundGramDesignTokens.getTextColor());
        bassCol.addView(bassValText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        bassKnob.setOnKnobChangeListener((p, fromUser) -> {
            if (fromUser) {
                SoundGramAudioFX.setBassLevel((short) Math.round(p * 1000f));
            }
            bassValText.setText(Math.round(p * 100) + "%");
        });

        // 3D-Sound Knob
        LinearLayout virtCol = new LinearLayout(context);
        virtCol.setOrientation(LinearLayout.VERTICAL);
        virtCol.setGravity(Gravity.CENTER_HORIZONTAL);

        virtKnob = new KnobView(context);
        virtKnob.setProgress(SoundGramAudioFX.getVirtualizerLevel() / 1000f);
        virtCol.addView(virtKnob, LayoutHelper.createLinear(88, 88));

        TextView virtLabel = new TextView(context);
        virtLabel.setText("3D-звук");
        virtLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        virtLabel.setTypeface(getMonoTypeface());
        virtLabel.setTextColor(SoundGramDesignTokens.getTextMutedColor());
        virtCol.addView(virtLabel, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 2));

        virtValText = new TextView(context);
        virtValText.setText(Math.round(virtKnob.getProgress() * 100) + "%");
        virtValText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        virtValText.setTypeface(AndroidUtilities.bold());
        virtValText.setTextColor(SoundGramDesignTokens.getTextColor());
        virtCol.addView(virtValText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        virtKnob.setOnKnobChangeListener((p, fromUser) -> {
            if (fromUser) {
                SoundGramAudioFX.setVirtualizerLevel((short) Math.round(p * 1000f));
            }
            virtValText.setText(Math.round(p * 100) + "%");
        });

        knobsRow.addView(bassCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
        knobsRow.addView(virtCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
        contentCol.addView(knobsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // 6. Speed slider: «скорость» (left) · «0,85×» (right accent)
        LinearLayout speedHeaderRow = new LinearLayout(context);
        speedHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        speedHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView speedLabel = new TextView(context);
        speedLabel.setText("скорость");
        speedLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        speedLabel.setTypeface(getMonoTypeface());
        speedLabel.setTextColor(SoundGramDesignTokens.getTextMutedColor());
        speedLabel.setLetterSpacing(0.02f);
        speedHeaderRow.addView(speedLabel, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        speedValText = new TextView(context);
        float currentSpeed = SoundGramAudioFX.getCurrentSpeed();
        speedValText.setText(formatSpeed(currentSpeed));
        speedValText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        speedValText.setTypeface(getMonoTypeface());
        speedValText.setTextColor(SoundGramDesignTokens.getAccentColor());
        speedHeaderRow.addView(speedValText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        contentCol.addView(speedHeaderRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        // Speed seek bar
        speedSeekBar = new SeekBar(context);
        speedSeekBar.setMax(100); // 0.50x to 1.50x
        int initialProgress = (int) Math.max(0, Math.min(100, (currentSpeed - 0.5f) * 100f));
        speedSeekBar.setProgress(initialProgress);
        speedSeekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(SoundGramDesignTokens.getAccentColor()));
        speedSeekBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(SoundGramDesignTokens.getSurface3Color()));
        if (Build.VERSION.SDK_INT >= 21) {
            speedSeekBar.setBackground(null);
            android.graphics.drawable.GradientDrawable thumb = new android.graphics.drawable.GradientDrawable();
            thumb.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            thumb.setColor(SoundGramDesignTokens.getAccentColor());
            thumb.setSize(AndroidUtilities.dp(16), AndroidUtilities.dp(16));
            speedSeekBar.setThumb(thumb);
            speedSeekBar.setThumbOffset(AndroidUtilities.dp(8));
        }
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                if (fromUser) {
                    float spd = 0.50f + (p / 100f);
                    SoundGramAudioFX.setCurrentSpeed(spd);
                    speedValText.setText(formatSpeed(spd));
                    try {
                        MediaController.getInstance().applyMusicPlaybackParams(spd, SoundGramAudioFX.getCurrentPitch());
                    } catch (Exception ignored) {}
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        contentCol.addView(speedSeekBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Pitch slider: «тон» (left) · «1,00×» (right accent)
        LinearLayout pitchHeaderRow = new LinearLayout(context);
        pitchHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        pitchHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView pitchLabel = new TextView(context);
        pitchLabel.setText("тон");
        pitchLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        pitchLabel.setTypeface(getMonoTypeface());
        pitchLabel.setTextColor(SoundGramDesignTokens.getTextMutedColor());
        pitchLabel.setLetterSpacing(0.02f);
        pitchHeaderRow.addView(pitchLabel, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        pitchValText = new TextView(context);
        float currentPitch = SoundGramAudioFX.getCurrentPitch();
        pitchValText.setText(formatSpeed(currentPitch));
        pitchValText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        pitchValText.setTypeface(getMonoTypeface());
        pitchValText.setTextColor(SoundGramDesignTokens.getAccentColor());
        pitchHeaderRow.addView(pitchValText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        contentCol.addView(pitchHeaderRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        pitchSeekBar = new SeekBar(context);
        pitchSeekBar.setMax(100); // 0.50x to 1.50x
        int initialPitchProgress = (int) Math.max(0, Math.min(100, (currentPitch - 0.5f) * 100f));
        pitchSeekBar.setProgress(initialPitchProgress);
        pitchSeekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(SoundGramDesignTokens.getAccentColor()));
        pitchSeekBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(SoundGramDesignTokens.getSurface3Color()));
        if (Build.VERSION.SDK_INT >= 21) {
            pitchSeekBar.setBackground(null);
            android.graphics.drawable.GradientDrawable thumb = new android.graphics.drawable.GradientDrawable();
            thumb.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            thumb.setColor(SoundGramDesignTokens.getAccentColor());
            thumb.setSize(AndroidUtilities.dp(16), AndroidUtilities.dp(16));
            pitchSeekBar.setThumb(thumb);
            pitchSeekBar.setThumbOffset(AndroidUtilities.dp(8));
        }
        pitchSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                if (fromUser) {
                    float ptc = 0.50f + (p / 100f);
                    SoundGramAudioFX.setCurrentPitch(ptc);
                    pitchValText.setText(formatSpeed(ptc));
                    try {
                        MediaController.getInstance().applyMusicPlaybackParams(SoundGramAudioFX.getCurrentSpeed(), ptc);
                    } catch (Exception ignored) {}
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        contentCol.addView(pitchSeekBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // 7. Quick rule chips: «slowed + reverb» · «nightcore»
        LinearLayout chipsRow = new LinearLayout(context);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setGravity(Gravity.START);

        chipSlowed = createRuleChip(context, "slowed + reverb", () -> {
            SoundGramAudioFX.applyQuickMode("slowed");
            updateUIFromState();
        });
        chipsRow.addView(chipSlowed, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        chipNightcore = createRuleChip(context, "nightcore", () -> {
            SoundGramAudioFX.applyQuickMode("nightcore");
            updateUIFromState();
        });
        chipsRow.addView(chipNightcore, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        contentCol.addView(chipsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        scrollView.addView(contentCol, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        rootSheet.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        setCustomView(rootSheet);
        buildPresetsList();
        updateUIFromState();
    }

    private TextView createRuleChip(Context context, String title, Runnable onClick) {
        TextView chip = new TextView(context);
        chip.setText(title);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        chip.setTypeface(getMonoTypeface());
        chip.setTextColor(SoundGramDesignTokens.getTextColor());
        chip.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        chip.setBackground(SoundGramDesignTokens.makeAsymmetricDrawable(SoundGramDesignTokens.getSurface1Color(),
                SoundGramDesignTokens.getLineStrongColor(), 1f, 12, 12, 12, 12));
        chip.setOnClickListener(v -> onClick.run());
        return chip;
    }

    private void updateStatusChipStyle(boolean enabled) {
        statusChip.setText(enabled ? "включён" : "выключен");
        statusChip.setTextColor(enabled ? SoundGramDesignTokens.getTextColor() : SoundGramDesignTokens.getTextDimColor());
        statusChip.setBackground(SoundGramDesignTokens.makeAsymmetricDrawable(
                enabled ? SoundGramDesignTokens.getSurface1Color() : SoundGramDesignTokens.getBgColor(),
                SoundGramDesignTokens.getLineStrongColor(), 1f, 999, 999, 999, 999));
    }

    private void updateUIFromState() {
        if (eqCurveView != null) {
            eqCurveView.invalidate();
            float db = eqCurveView.getCurrentGainDb();
            bigGainText.setText(formatDb(db));
            freqDetailText.setText(eqCurveView.getCurrentFreqText() + " · дб");
        }
        float spd = SoundGramAudioFX.getCurrentSpeed();
        speedValText.setText(formatSpeed(spd));
        speedSeekBar.setProgress((int) Math.max(0, Math.min(100, (spd - 0.5f) * 100f)));

        float ptc = SoundGramAudioFX.getCurrentPitch();
        if (pitchValText != null) pitchValText.setText(formatSpeed(ptc));
        if (pitchSeekBar != null) pitchSeekBar.setProgress((int) Math.max(0, Math.min(100, (ptc - 0.5f) * 100f)));

        bassKnob.setProgress(SoundGramAudioFX.getBassLevel() / 1000f);
        bassValText.setText(Math.round(bassKnob.getProgress() * 100) + "%");

        virtKnob.setProgress(SoundGramAudioFX.getVirtualizerLevel() / 1000f);
        virtValText.setText(Math.round(virtKnob.getProgress() * 100) + "%");
    }

    private String formatDb(float db) {
        return String.format(Locale.GERMAN, "%+.1f", db);
    }

    private String formatSpeed(float speed) {
        return String.format(Locale.GERMAN, "%.2f×", speed);
    }

    private android.graphics.Typeface getMonoTypeface() {
        android.graphics.Typeface tf = AndroidUtilities.getTypeface("fonts/rmono.ttf");
        return tf != null ? tf : android.graphics.Typeface.MONOSPACE;
    }

    // ── PRESETS ─────────────────────────────────────────────────────────────────
    private final String[] presetNames = {"Flat", "Rock", "Pop", "Бас"};
    private final float[][] presetCurves = {
            {0f, 0f, 0f, 0f, 0f},            // Flat
            {3.5f, 2.0f, -1.0f, 2.5f, 4.0f}, // Rock
            {-1.0f, 1.5f, 5.0f, 1.5f, -1.0f},// Pop (active in Photo 1: +5.0 at 910 Hz)
            {6.0f, 4.0f, 1.0f, 0f, -2.0f}    // Бас
    };

    private int activePresetIndex = SoundGramAudioFX.getCurrentEqPreset();

    private void buildPresetsList() {
        if (presetsContainer == null) return;
        presetsContainer.removeAllViews();
        Context ctx = getContext();

        for (int i = 0; i < presetNames.length; i++) {
            final int idx = i;
            final String name = presetNames[i];
            final float[] curve = presetCurves[i];

            PresetCardView card = new PresetCardView(ctx, name, curve, idx == activePresetIndex);
            card.setTag(idx);
            card.setOnClickListener(v -> {
                SoundGramAudioFX.applyEqPreset(idx);
                applyPresetCurve(curve, false);
                refreshPresetsActiveState(idx);
            });
            presetsContainer.addView(card, LayoutHelper.createLinear(76, 58, 0, 0, 8, 0));
        }

        // Add custom presets from SoundGramAudioFX
        ArrayList<SoundGramAudioFX.CustomPreset> custom = SoundGramAudioFX.getCustomPresets();
        for (int i = 0; i < custom.size(); i++) {
            final int customIdx = 100 + i;
            final int customListIdx = i;
            final SoundGramAudioFX.CustomPreset cp = custom.get(i);
            float[] curve = new float[5];
            for (int k = 0; k < 5 && k < cp.levels.length; k++) {
                curve[k] = cp.levels[k] / 100f;
            }
            PresetCardView card = new PresetCardView(ctx, cp.name, curve, customIdx == activePresetIndex);
            card.setTag(customIdx);
            card.setOnClickListener(v -> {
                SoundGramAudioFX.applyCustomPreset(cp, customListIdx);
                applyPresetCurve(curve, false);
                refreshPresetsActiveState(customIdx);
                updateUIFromState();
            });
            card.setOnLongClickListener(v -> {
                showDeleteCustomPresetDialog(customListIdx, cp.name);
                return true;
            });
            presetsContainer.addView(card, LayoutHelper.createLinear(76, 58, 0, 0, 8, 0));
        }
    }

    private void showSavePresetDialog() {
        Context context = getContext();
        if (context == null) return;
        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(context);
        builder.setTitle("Сохранить пресет");
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(6), AndroidUtilities.dp(24), 0);
        final android.widget.EditText editText = new android.widget.EditText(context);
        editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editText.setHint("Название пресета");
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(4), Theme.getColor(Theme.key_windowBackgroundGray)));
        editText.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
        container.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setView(container);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String name = editText.getText().toString().trim();
            if (!name.isEmpty()) {
                SoundGramAudioFX.saveCustomPreset(name);
                buildPresetsList();
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showDeleteCustomPresetDialog(int index, String name) {
        Context context = getContext();
        if (context == null) return;
        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(context);
        builder.setTitle("Удалить пресет");
        builder.setMessage("Удалить пресет «" + name + "»?");
        builder.setPositiveButton("Удалить", (dialog, which) -> {
            SoundGramAudioFX.deleteCustomPreset(index);
            buildPresetsList();
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void refreshPresetsActiveState(int selectedIndex) {
        activePresetIndex = selectedIndex;
        SoundGramAudioFX.setCurrentEqPreset(selectedIndex);
        if (presetsContainer == null) return;
        int childCount = presetsContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = presetsContainer.getChildAt(i);
            if (child instanceof PresetCardView) {
                Object tag = child.getTag();
                boolean active = (tag instanceof Integer) && ((Integer) tag == selectedIndex);
                ((PresetCardView) child).setActive(active);
            }
        }
    }

    private void applyPresetCurve(float[] curve, boolean updateFx) {
        if (eqCurveView != null) {
            eqCurveView.setBands(curve, updateFx);
            float db = eqCurveView.getCurrentGainDb();
            bigGainText.setText(formatDb(db));
            freqDetailText.setText(eqCurveView.getCurrentFreqText() + " · дб");
        }
    }

    // ── PRESET CARD VIEW ────────────────────────────────────────────────────────
    public static class PresetCardView extends View {
        private final String name;
        private final float[] curve;
        private boolean isActive;

        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint curvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint underlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Path path = new Path();

        public PresetCardView(Context context, String name, float[] curve, boolean isActive) {
            super(context);
            this.name = name;
            this.curve = curve;
            this.isActive = isActive;

            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(AndroidUtilities.dp(1));

            curvePaint.setStyle(Paint.Style.STROKE);
            curvePaint.setStrokeWidth(AndroidUtilities.dp(1.8f));
            curvePaint.setStrokeCap(Paint.Cap.ROUND);

            textPaint.setTextSize(AndroidUtilities.dp(11));
            textPaint.setTextAlign(Paint.Align.CENTER);
            android.graphics.Typeface tf = AndroidUtilities.getTypeface("fonts/rmono.ttf");
            textPaint.setTypeface(tf != null ? tf : android.graphics.Typeface.MONOSPACE);

            underlinePaint.setColor(SoundGramDesignTokens.getAccentColor());
            underlinePaint.setStrokeWidth(AndroidUtilities.dp(2));
            underlinePaint.setStrokeCap(Paint.Cap.ROUND);
        }

        public void setActive(boolean active) {
            this.isActive = active;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            // Background & Border
            rect.set(1, 1, w - 1, h - 3);
            Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setColor(SoundGramDesignTokens.getSurface1Color());
            bgPaint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(rect, AndroidUtilities.dp(10), AndroidUtilities.dp(10), bgPaint);

            borderPaint.setColor(isActive ? SoundGramDesignTokens.getAccentColor() : SoundGramDesignTokens.getLineStrongColor());
            borderPaint.setStrokeWidth(AndroidUtilities.dp(isActive ? 1.5f : 1f));
            canvas.drawRoundRect(rect, AndroidUtilities.dp(10), AndroidUtilities.dp(10), borderPaint);

            // Mini curve preview in top half
            curvePaint.setColor(isActive ? SoundGramDesignTokens.getAccentColor() : SoundGramDesignTokens.getTextMutedColor());
            path.reset();
            float padX = AndroidUtilities.dp(14);
            float usableW = w - padX * 2;
            float midY = AndroidUtilities.dp(20);
            float amp = AndroidUtilities.dp(10);

            for (int i = 0; i < curve.length; i++) {
                float x = padX + (i / (float) (curve.length - 1)) * usableW;
                float y = midY - (curve[i] / 15f) * amp;
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            canvas.drawPath(path, curvePaint);

            // Text label
            textPaint.setColor(isActive ? SoundGramDesignTokens.getAccentColor() : SoundGramDesignTokens.getText2Color());
            canvas.drawText(name, w / 2f, h - AndroidUtilities.dp(12), textPaint);

            // Active underline indicator
            if (isActive) {
                underlinePaint.setColor(SoundGramDesignTokens.getAccentColor());
                canvas.drawLine(w * 0.25f, h - 1, w * 0.75f, h - 1, underlinePaint);
            }
        }
    }

    // ── ROTARY KNOB VIEW ────────────────────────────────────────────────────────
    public static class KnobView extends View {
        public interface OnKnobChangeListener {
            void onProgressChanged(float progress, boolean fromUser);
        }

        private float progress = 0.5f; // 0.0 to 1.0
        private OnKnobChangeListener listener;

        private final Paint arcTrackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arcProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint centerDiscPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF arcRect = new RectF();
        private float lastTouchY;

        public KnobView(Context context) {
            super(context);
            init();
        }

        private void init() {
            arcTrackPaint.setStyle(Paint.Style.STROKE);
            arcTrackPaint.setStrokeWidth(AndroidUtilities.dp(5));
            arcTrackPaint.setColor(SoundGramDesignTokens.getSurface3Color());
            arcTrackPaint.setStrokeCap(Paint.Cap.ROUND);

            arcProgressPaint.setStyle(Paint.Style.STROKE);
            arcProgressPaint.setStrokeWidth(AndroidUtilities.dp(5));
            arcProgressPaint.setColor(SoundGramDesignTokens.getAccentColor());
            arcProgressPaint.setStrokeCap(Paint.Cap.ROUND);

            centerDiscPaint.setStyle(Paint.Style.FILL);
            centerDiscPaint.setColor(SoundGramDesignTokens.getSurface2Color());
        }

        public void setProgress(float p) {
            this.progress = Math.max(0f, Math.min(1f, p));
            invalidate();
        }

        public float getProgress() {
            return progress;
        }

        public void setOnKnobChangeListener(OnKnobChangeListener l) {
            this.listener = l;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchY = event.getY();
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dy = lastTouchY - event.getY();
                    lastTouchY = event.getY();
                    float delta = dy / AndroidUtilities.dp(120);
                    progress = Math.max(0f, Math.min(1f, progress + delta));
                    invalidate();
                    if (listener != null) {
                        listener.onProgressChanged(progress, true);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (listener != null) {
                        listener.onProgressChanged(progress, true);
                    }
                    return true;
            }
            return super.onTouchEvent(event);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            arcTrackPaint.setColor(SoundGramDesignTokens.getLineStrongColor());
            arcProgressPaint.setColor(SoundGramDesignTokens.getAccentColor());
            centerDiscPaint.setColor(SoundGramDesignTokens.getSurface1Color());

            float cx = w / 2f;
            float cy = h / 2f;
            float strokeW = AndroidUtilities.dp(5.5f);
            float radius = (Math.min(w, h) - strokeW * 2) / 2f;

            arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

            // 270 degree arc: start at 135 deg, sweep 270 deg
            canvas.drawArc(arcRect, 135f, 270f, false, arcTrackPaint);
            if (progress > 0) {
                canvas.drawArc(arcRect, 135f, 270f * progress, false, arcProgressPaint);
            }

            // Inner solid disc
            float innerRadius = radius - strokeW - AndroidUtilities.dp(3);
            canvas.drawCircle(cx, cy, innerRadius, centerDiscPaint);
        }
    }

    // ── 5-BAND EQUALIZER CURVE VIEW ─────────────────────────────────────────────
    public static class EqCurveView extends View {
        public interface OnBandChangedListener {
            void onBandChanged(int bandIndex, String freqText, float dbValue);
        }

        private static final String[] FREQ_LABELS = {"60", "230", "910", "3к", "14к"};
        private final float[] bandDbs = {-1.0f, 1.5f, 5.0f, 1.5f, -1.0f}; // Default: Pop matching Photo 1 (+5.0 at 910)
        private int selectedBand = 2; // Default: 910 Hz

        private OnBandChangedListener listener;

        private final Paint curvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dashGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pointFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pointRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pointHolePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path curvePath = new Path();
        private final Path fillPath = new Path();

        public EqCurveView(Context context) {
            super(context);
            init();
        }

        private void init() {
            curvePaint.setStyle(Paint.Style.STROKE);
            curvePaint.setStrokeWidth(AndroidUtilities.dp(3f));
            curvePaint.setColor(SoundGramDesignTokens.getAccentColor());
            curvePaint.setStrokeCap(Paint.Cap.ROUND);

            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setColor(Theme.multAlpha(SoundGramDesignTokens.getAccentColor(), 0.14f));

            gridPaint.setStyle(Paint.Style.STROKE);
            gridPaint.setStrokeWidth(AndroidUtilities.dp(0.75f));
            gridPaint.setColor(SoundGramDesignTokens.getLineColor());

            dashGridPaint.setStyle(Paint.Style.STROKE);
            dashGridPaint.setStrokeWidth(AndroidUtilities.dp(0.75f));
            dashGridPaint.setColor(SoundGramDesignTokens.getLineStrongColor());
            dashGridPaint.setPathEffect(new DashPathEffect(new float[]{AndroidUtilities.dp(4), AndroidUtilities.dp(4)}, 0));

            textPaint.setTextSize(AndroidUtilities.dp(11));
            textPaint.setColor(SoundGramDesignTokens.getTextMutedColor());
            android.graphics.Typeface tf = AndroidUtilities.getTypeface("fonts/rmono.ttf");
            textPaint.setTypeface(tf != null ? tf : android.graphics.Typeface.MONOSPACE);

            pointFillPaint.setStyle(Paint.Style.FILL);
            pointFillPaint.setColor(SoundGramDesignTokens.getAccentColor());

            pointRingPaint.setStyle(Paint.Style.STROKE);
            pointRingPaint.setStrokeWidth(AndroidUtilities.dp(3f));
            pointRingPaint.setColor(SoundGramDesignTokens.getAccentColor());

            pointHolePaint.setStyle(Paint.Style.FILL);
            pointHolePaint.setColor(SoundGramDesignTokens.getBgColor());

            // Sync with current hardware EQ if available
            short[] hwLevels = SoundGramAudioFX.getCurrentBandLevels();
            if (hwLevels != null && hwLevels.length >= 5) {
                for (int i = 0; i < 5; i++) {
                    bandDbs[i] = hwLevels[i] / 100f;
                }
            }
        }

        public void setOnBandChangedListener(OnBandChangedListener l) {
            this.listener = l;
        }

        public void setBands(float[] dbs) {
            setBands(dbs, true);
        }

        public void setBands(float[] dbs, boolean updateFx) {
            if (dbs == null) return;
            for (int i = 0; i < 5 && i < dbs.length; i++) {
                bandDbs[i] = Math.max(-15f, Math.min(15f, dbs[i]));
                if (updateFx) {
                    SoundGramAudioFX.setBandLevel((short) i, (short) Math.round(bandDbs[i] * 100f));
                }
            }
            invalidate();
        }

        public float getCurrentGainDb() {
            return bandDbs[selectedBand];
        }

        public String getCurrentFreqText() {
            return FREQ_LABELS[selectedBand] + " гц";
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return false;

            float padLeft = AndroidUtilities.dp(36);
            float padRight = AndroidUtilities.dp(18);
            float usableW = w - padLeft - padRight;
            float topY = AndroidUtilities.dp(24);
            float botY = h - AndroidUtilities.dp(36);
            float usableH = botY - topY;
            float midY = topY + usableH / 2f;

            float tx = event.getX();
            float ty = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    int closest = -1;
                    float minD = Float.MAX_VALUE;
                    for (int i = 0; i < 5; i++) {
                        float px = padLeft + (i / 4f) * usableW;
                        float dist = Math.abs(tx - px);
                        if (dist < minD) {
                            minD = dist;
                            closest = i;
                        }
                    }
                    if (closest != -1 && minD < AndroidUtilities.dp(36)) {
                        selectedBand = closest;
                        getParent().requestDisallowInterceptTouchEvent(true);
                        updateBandFromY(closest, ty, topY, usableH, midY);
                        return true;
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (selectedBand >= 0 && selectedBand < 5) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                        updateBandFromY(selectedBand, ty, topY, usableH, midY);
                        return true;
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    return true;
            }
            return super.onTouchEvent(event);
        }

        private void updateBandFromY(int band, float ty, float topY, float usableH, float midY) {
            float norm = (midY - ty) / (usableH / 2f); // -1.0 to +1.0
            float db = norm * 15f;
            db = Math.max(-15f, Math.min(15f, Math.round(db * 2f) / 2f)); // step 0.5 dB
            bandDbs[band] = db;
            SoundGramAudioFX.setBandLevel((short) band, (short) Math.round(db * 100f));
            invalidate();
            if (listener != null) {
                listener.onBandChanged(band, FREQ_LABELS[band] + " гц", db);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            curvePaint.setColor(SoundGramDesignTokens.getAccentColor());
            fillPaint.setColor(Theme.multAlpha(SoundGramDesignTokens.getAccentColor(), 0.14f));
            gridPaint.setColor(SoundGramDesignTokens.getLineColor());
            dashGridPaint.setColor(SoundGramDesignTokens.getLineStrongColor());
            textPaint.setColor(SoundGramDesignTokens.getTextMutedColor());
            pointFillPaint.setColor(SoundGramDesignTokens.getAccentColor());
            pointRingPaint.setColor(SoundGramDesignTokens.getAccentColor());
            pointHolePaint.setColor(SoundGramDesignTokens.getBgColor());

            float padLeft = AndroidUtilities.dp(36);
            float padRight = AndroidUtilities.dp(18);
            float usableW = w - padLeft - padRight;
            float topY = AndroidUtilities.dp(24);
            float botY = h - AndroidUtilities.dp(36);
            float usableH = botY - topY;
            float midY = topY + usableH / 2f;

            // Grid lines and labels
            // +15
            canvas.drawLine(padLeft, topY, w - padRight, topY, gridPaint);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("+15", padLeft - AndroidUtilities.dp(8), topY + AndroidUtilities.dp(4), textPaint);

            // 0 (dashed)
            Path dashPath = new Path();
            dashPath.moveTo(padLeft, midY);
            dashPath.lineTo(w - padRight, midY);
            canvas.drawPath(dashPath, dashGridPaint);
            canvas.drawText("0", padLeft - AndroidUtilities.dp(8), midY + AndroidUtilities.dp(4), textPaint);

            // -15
            canvas.drawLine(padLeft, botY, w - padRight, botY, gridPaint);
            canvas.drawText("-15", padLeft - AndroidUtilities.dp(8), botY + AndroidUtilities.dp(4), textPaint);

            // Calculate 5 point positions
            float[] px = new float[5];
            float[] py = new float[5];
            for (int i = 0; i < 5; i++) {
                px[i] = padLeft + (i / 4f) * usableW;
                float norm = bandDbs[i] / 15f;
                py[i] = midY - norm * (usableH / 2f);

                // Frequency labels below the graph
                textPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(FREQ_LABELS[i], px[i], botY + AndroidUtilities.dp(20), textPaint);
            }

            // Draw smooth cubic curve and fill
            curvePath.reset();
            fillPath.reset();

            curvePath.moveTo(px[0], py[0]);
            fillPath.moveTo(px[0], midY);
            fillPath.lineTo(px[0], py[0]);

            for (int i = 0; i < 4; i++) {
                float p0x = px[Math.max(0, i - 1)];
                float p0y = py[Math.max(0, i - 1)];
                float p1x = px[i];
                float p1y = py[i];
                float p2x = px[i + 1];
                float p2y = py[i + 1];
                float p3x = px[Math.min(4, i + 2)];
                float p3y = py[Math.min(4, i + 2)];

                float cp1x = p1x + (p2x - p0x) / 6.0f;
                float cp1y = p1y + (p2y - p0y) / 6.0f;
                float cp2x = p2x - (p3x - p1x) / 6.0f;
                float cp2y = p2y - (p3y - p1y) / 6.0f;

                curvePath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2x, p2y);
                fillPath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2x, p2y);
            }

            fillPath.lineTo(px[4], midY);
            fillPath.close();

            // 14% accent fill under curve
            canvas.drawPath(fillPath, fillPaint);
            // Curve stroke
            canvas.drawPath(curvePath, curvePaint);

            // Draw 5 points
            for (int i = 0; i < 5; i++) {
                if (i == selectedBand) {
                    // Selected: solid filled accent circle
                    canvas.drawCircle(px[i], py[i], AndroidUtilities.dp(8f), pointFillPaint);
                } else {
                    // Inactive: hollow ring with green outline and dark center
                    canvas.drawCircle(px[i], py[i], AndroidUtilities.dp(8f), pointHolePaint);
                    canvas.drawCircle(px[i], py[i], AndroidUtilities.dp(8f), pointRingPaint);
                }
            }
        }
    }
}