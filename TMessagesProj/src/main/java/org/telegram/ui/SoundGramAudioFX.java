package org.telegram.ui;

import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;

public class SoundGramAudioFX {
    private static Equalizer equalizer;
    private static BassBoost bassBoost;
    private static PresetReverb reverb;

    private static Runnable sleepTimerRunnable;
    public static long sleepTimerEndTime = 0;

    // Глобальные переменные для хранения скорости и тона
    private static float currentSpeed = 1.0f;
    private static float currentPitch = 1.0f;

    // Добавь эти переменные в начало класса SoundGramAudioFX
    private static int currentSessionId = -1;
    private static short savedReverbPreset = 0;
    private static short savedBassLevel = 0;

    // Замени метод initFX на этот:
    public static void initFX(int audioSessionId) {
        try {
            // Если трек переключился (ID изменился), нужно убить старые эффекты
            if (currentSessionId != audioSessionId) {
                if (equalizer != null) { equalizer.release(); equalizer = null; }
                if (bassBoost != null) { bassBoost.release(); bassBoost = null; }
                if (reverb != null) { reverb.release(); reverb = null; }
                currentSessionId = audioSessionId;
            }

            if (equalizer == null) {
                equalizer = new Equalizer(0, audioSessionId);
                equalizer.setEnabled(true);
            }
            if (bassBoost == null) {
                bassBoost = new BassBoost(0, audioSessionId);
                if (bassBoost.getStrengthSupported()) bassBoost.setStrength(savedBassLevel);
                bassBoost.setEnabled(true);
            }
            if (reverb == null) {
                reverb = new PresetReverb(0, audioSessionId);
                reverb.setPreset(savedReverbPreset);
                reverb.setEnabled(savedReverbPreset != PresetReverb.PRESET_NONE);
            }
        } catch (Exception e) {
            FileLog.e("SoundGram AudioFX Error: " + e.getMessage());
        }
    }

    // Замени методы сохранения, чтобы они запоминали выбор:
    public static void setReverbPreset(short preset) {
        savedReverbPreset = preset;
        if (reverb != null) {
            reverb.setPreset(preset);
            reverb.setEnabled(preset != PresetReverb.PRESET_NONE);
        }
    }
    public static short getReverbPreset() { return savedReverbPreset; }

    public static void setBassLevel(short level) {
        savedBassLevel = level;
        if (bassBoost != null && bassBoost.getStrengthSupported()) bassBoost.setStrength(level);
    }


    public static float getCurrentSpeed() { return currentSpeed; }
    public static void setCurrentSpeed(float speed) { currentSpeed = speed; }

    public static float getCurrentPitch() { return currentPitch; }
    public static void setCurrentPitch(float pitch) { currentPitch = pitch; }

    public static int getReverbId() { return reverb != null ? reverb.getId() : 0; }

    public static short getBassLevel() { return bassBoost != null && bassBoost.getStrengthSupported() ? bassBoost.getRoundedStrength() : 0; }

    public static void setBandLevel(short band, short level) { if (equalizer != null) equalizer.setBandLevel(band, level); }
    public static short getBandLevel(short band) { return equalizer != null ? equalizer.getBandLevel(band) : 0; }
    public static short getBandsCount() { return equalizer != null ? equalizer.getNumberOfBands() : 0; }
    public static int getCenterFreq(short band) { return equalizer != null ? equalizer.getCenterFreq(band) / 1000 : 0; }


    public static void resetFX() {
        setBassLevel((short) 0);
        setReverbPreset(PresetReverb.PRESET_NONE);
        currentSpeed = 1.0f;
        currentPitch = 1.0f;
        if (equalizer != null) {
            for (short i = 0; i < equalizer.getNumberOfBands(); i++) equalizer.setBandLevel(i, (short) 0);
        }
        try { MediaController.getInstance().setPlaybackSpeed(false, 1.0f);
        } catch (Exception ignored) {}
    }

    public static void startSleepTimer(int minutes) {
        cancelSleepTimer();
        sleepTimerEndTime = System.currentTimeMillis() + (minutes * 60 * 1000L);
        sleepTimerRunnable = () -> {
            MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
            if (playing != null && !MediaController.getInstance().isMessagePaused()) MediaController.getInstance().pauseMessage(playing);
            sleepTimerEndTime = 0;
        };
        AndroidUtilities.runOnUIThread(sleepTimerRunnable, minutes * 60 * 1000L);
    }

    public static void cancelSleepTimer() {
        if (sleepTimerRunnable != null) AndroidUtilities.cancelRunOnUIThread(sleepTimerRunnable);
        sleepTimerRunnable = null;
        sleepTimerEndTime = 0;
    }
}
