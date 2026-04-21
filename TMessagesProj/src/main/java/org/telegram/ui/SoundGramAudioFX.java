package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;

public class SoundGramAudioFX {
    private static Equalizer equalizer;
    private static BassBoost bassBoost;
    private static PresetReverb reverb;

    private static Runnable sleepTimerRunnable;
    public static long sleepTimerEndTime = 0;

    private static float currentSpeed = 1.0f;
    private static float currentPitch = 1.0f;
    private static int currentSessionId = -1;
    private static short savedReverbPreset = 0;
    private static short savedBassLevel = 0;

    // Инициализируем SharedPreferences для SoundGram
    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("soundgram_fx", Context.MODE_PRIVATE);
    }

    public static void initFX(int audioSessionId) {
        try {
            if (currentSessionId != audioSessionId) {
                if (equalizer != null) { equalizer.release(); equalizer = null; }
                if (bassBoost != null) { bassBoost.release(); bassBoost = null; }
                if (reverb != null) { reverb.release(); reverb = null; }
                currentSessionId = audioSessionId;
            }

            // Читаем сохраненные настройки
            savedReverbPreset = (short) getPrefs().getInt("reverb", 0);
            savedBassLevel = (short) getPrefs().getInt("bass", 0);
            currentSpeed = getPrefs().getFloat("speed", 1.0f);
            currentPitch = getPrefs().getFloat("pitch", 1.0f);

            if (equalizer == null) {
                equalizer = new Equalizer(0, audioSessionId);
                equalizer.setEnabled(true);
                // Восстанавливаем ползунки эквалайзера
                for (short i = 0; i < equalizer.getNumberOfBands(); i++) {
                    equalizer.setBandLevel(i, (short) getPrefs().getInt("band_" + i, 0));
                }
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

    public static void setReverbPreset(short preset) {
        savedReverbPreset = preset;
        getPrefs().edit().putInt("reverb", preset).apply();
        if (reverb != null) {
            reverb.setPreset(preset);
            reverb.setEnabled(preset != PresetReverb.PRESET_NONE);
        }
    }
    public static short getReverbPreset() { return savedReverbPreset; }

    public static void setBassLevel(short level) {
        savedBassLevel = level;
        getPrefs().edit().putInt("bass", level).apply();
        if (bassBoost != null && bassBoost.getStrengthSupported()) bassBoost.setStrength(level);
    }

    public static float getCurrentSpeed() { return currentSpeed; }
    public static void setCurrentSpeed(float speed) {
        currentSpeed = speed;
        getPrefs().edit().putFloat("speed", speed).apply();
    }

    public static float getCurrentPitch() { return currentPitch; }
    public static void setCurrentPitch(float pitch) {
        currentPitch = pitch;
        getPrefs().edit().putFloat("pitch", pitch).apply();
    }

    public static int getReverbId() { return reverb != null ? reverb.getId() : 0; }
    public static short getBassLevel() { return bassBoost != null && bassBoost.getStrengthSupported() ? bassBoost.getRoundedStrength() : 0; }

    public static void setBandLevel(short band, short level) {
        getPrefs().edit().putInt("band_" + band, level).apply();
        if (equalizer != null) equalizer.setBandLevel(band, level);
    }
    public static short getBandLevel(short band) { return equalizer != null ? equalizer.getBandLevel(band) : 0; }
    public static short getBandsCount() { return equalizer != null ? equalizer.getNumberOfBands() : 0; }
    public static int getCenterFreq(short band) { return equalizer != null ? equalizer.getCenterFreq(band) / 1000 : 0; }

    public static void resetFX() {
        setBassLevel((short) 0);
        setReverbPreset(PresetReverb.PRESET_NONE);
        setCurrentSpeed(1.0f);
        setCurrentPitch(1.0f);
        if (equalizer != null) {
            for (short i = 0; i < equalizer.getNumberOfBands(); i++) setBandLevel(i, (short) 0);
        }
        try { MediaController.getInstance().setPlaybackSpeed(false, 1.0f); } catch (Exception ignored) {}
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
