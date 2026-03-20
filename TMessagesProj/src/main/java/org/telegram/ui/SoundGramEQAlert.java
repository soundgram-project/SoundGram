package org.telegram.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MediaController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class SoundGramEQAlert extends Dialog {

    private boolean isUpdatingTimer = false;

    public SoundGramEQAlert(Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        SoundGramAudioFX.initFX(0);

        ScrollView scroll = new ScrollView(context);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(24), Theme.getColor(Theme.key_dialogBackground)));
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(28));
        scroll.addView(container);

        // --- ШАПКА ---
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText("AudioFX & Таймер");
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setTextSize(22);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        headerRow.addView(title, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        TextView resetBtn = new TextView(context);
        resetBtn.setText("СБРОС");
        resetBtn.setTextColor(0xFFE53935);
        resetBtn.setTextSize(14);
        resetBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        resetBtn.setOnClickListener(v -> {
            SoundGramAudioFX.resetFX();
            dismiss();
            new SoundGramEQAlert(context).show();
        });
        headerRow.addView(resetBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        container.addView(headerRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 28));

        // --- 1. ТАЙМЕР СНА ---
        TextView timerTitle = new TextView(context);
        timerTitle.setText("Таймер сна (минуты):");
        timerTitle.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        container.addView(timerTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        LinearLayout timerRow = new LinearLayout(context);
        timerRow.setOrientation(LinearLayout.HORIZONTAL);
        timerRow.setGravity(Gravity.CENTER_VERTICAL);

        EditText timerInput = new EditText(context);
        timerInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        timerInput.setText("30");
        timerInput.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        timerRow.addView(timerInput, LayoutHelper.createLinear(60, LayoutHelper.WRAP_CONTENT));

        SeekBar timerBar = new SeekBar(context);
        timerBar.setMax(120);
        timerBar.setProgress(30);
        timerRow.addView(timerBar, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
        container.addView(timerRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        timerBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    isUpdatingTimer = true;
                    timerInput.setText(String.valueOf(progress));
                    isUpdatingTimer = false;
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {} @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        timerInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isUpdatingTimer && s.length() > 0) {
                    try { timerBar.setProgress(Integer.parseInt(s.toString())); } catch (Exception ignored) {}
                }
            }
        });

        TextView startTimerBtn = new TextView(context);
        startTimerBtn.setText(SoundGramAudioFX.sleepTimerEndTime > 0 ? "Остановить таймер" : "Запустить таймер");
        startTimerBtn.setGravity(Gravity.CENTER);
        startTimerBtn.setTextColor(Color.WHITE);
        startTimerBtn.setTextSize(16);
        startTimerBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        startTimerBtn.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), SoundGramAudioFX.sleepTimerEndTime > 0 ? 0xFFE53935 : 0xFF1DB954));
        startTimerBtn.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14));
        startTimerBtn.setOnClickListener(v -> {
            if (SoundGramAudioFX.sleepTimerEndTime > 0) {
                SoundGramAudioFX.cancelSleepTimer();
                startTimerBtn.setText("Запустить таймер");
                startTimerBtn.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), 0xFF1DB954));
            } else {
                int mins = timerBar.getProgress();
                if (mins > 0) {
                    SoundGramAudioFX.startSleepTimer(mins);
                    startTimerBtn.setText("Остановить таймер");
                    startTimerBtn.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), 0xFFE53935));
                    Toast.makeText(context, "Таймер на " + mins + " мин.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        container.addView(startTimerBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 32));

        // --- 2. ПРОФЕССИОНАЛЬНЫЙ ЭКВАЛАЙЗЕР ---
        TextView eqTitle = new TextView(context);
        eqTitle.setText("Эквалайзер");
        eqTitle.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        eqTitle.setTextSize(16);
        eqTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        container.addView(eqTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        short bands = SoundGramAudioFX.getBandsCount();
        if (bands > 0) {
            // Исключаем HorizontalScrollView, используем веса (weightSum) для равномерного распределения
            LinearLayout eqContainer = new LinearLayout(context);
            eqContainer.setOrientation(LinearLayout.HORIZONTAL);
            eqContainer.setGravity(Gravity.CENTER);
            eqContainer.setWeightSum(bands);

            for (short i = 0; i < bands; i++) {
                final short bandIndex = i;
                LinearLayout bandCol = new LinearLayout(context);
                bandCol.setOrientation(LinearLayout.VERTICAL);
                bandCol.setGravity(Gravity.CENTER_HORIZONTAL);

                TextView freqText = new TextView(context);
                int freq = SoundGramAudioFX.getCenterFreq(bandIndex);
                String freqStr = freq >= 1000 ? (freq / 1000) + "k" : freq + "Hz";
                freqText.setText(freqStr);
                freqText.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
                freqText.setTextSize(11);
                bandCol.addView(freqText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

                FrameLayout barFrame = new FrameLayout(context);
                SeekBar bar = new SeekBar(context);
                bar.setMax(3000);
                bar.setProgress(SoundGramAudioFX.getBandLevel(bandIndex) + 1500);
                bar.setRotation(-90);

                bar.setOnTouchListener((v, event) -> {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    return false;
                });

                bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                        if (fromUser) SoundGramAudioFX.setBandLevel(bandIndex, (short) (p - 1500));
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {} @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                barFrame.addView(bar, LayoutHelper.createFrame(150, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
                bandCol.addView(barFrame, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 150));

                // Размещаем каждый столбец с весом 1.0f, чтобы они делили ширину экрана поровну без скролла
                eqContainer.addView(bandCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
            }
            container.addView(eqContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));
        }

        // --- 3. ОСТАЛЬНЫЕ ЭФФЕКТЫ ---
        container.addView(createSliderRow(context, "BassBoost", 1000, SoundGramAudioFX.getBassLevel(), progress -> SoundGramAudioFX.setBassLevel((short) progress)));

        TextView speedTitle = new TextView(context);
        speedTitle.setText(String.format("Скорость: %.1fx", SoundGramAudioFX.getCurrentSpeed()));
        speedTitle.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        container.addView(speedTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        SeekBar speedBar = new SeekBar(context);
        speedBar.setMax(19);
        speedBar.setProgress(Math.round((SoundGramAudioFX.getCurrentSpeed() - 0.1f) / 0.1f));
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speed = 0.1f + (progress * 0.1f);
                speedTitle.setText(String.format("Скорость: %.1fx", speed));
                if (fromUser) {
                    SoundGramAudioFX.setCurrentSpeed(speed);
                    try { MediaController.getInstance().setPlaybackSpeed(false, speed); } catch (Exception ignored) {}
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {} @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        container.addView(speedBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 24));

        TextView pitchTitle = new TextView(context);
        pitchTitle.setText(String.format("Тон: %.1fx", SoundGramAudioFX.getCurrentPitch()));
        pitchTitle.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        container.addView(pitchTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        SeekBar pitchBar = new SeekBar(context);
        pitchBar.setMax(19);
        pitchBar.setProgress(Math.round((SoundGramAudioFX.getCurrentPitch() - 0.1f) / 0.1f));
        pitchBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float pitch = 0.1f + (progress * 0.1f);
                pitchTitle.setText(String.format("Тон: %.1fx", pitch));
                if (fromUser) {
                    SoundGramAudioFX.setCurrentPitch(pitch);
                    try { MediaController.getInstance().setPlaybackSpeed(false, SoundGramAudioFX.getCurrentSpeed()); } catch (Exception ignored) {}
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {} @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        container.addView(pitchBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        setContentView(scroll);
    }

    private View createSliderRow(Context context, String title, int max, int progress, SliderCallback callback) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);

        TextView t = new TextView(context);
        t.setText(title);
        t.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        t.setTextSize(16);
        t.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        row.addView(t, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        SeekBar bar = new SeekBar(context);
        bar.setMax(max);
        bar.setProgress(progress);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) { if (fromUser) callback.onChange(p); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {} @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        row.addView(bar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 24));
        return row;
    }

    interface SliderCallback { void onChange(int progress); }

    @Override
    protected void onStart() {
        super.onStart();
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.90);
            int height = (int) (metrics.heightPixels * 0.75);
            window.setLayout(width, height);
            window.setGravity(Gravity.CENTER);
        }
    }
}
