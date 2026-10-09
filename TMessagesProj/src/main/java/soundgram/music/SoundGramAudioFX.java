package soundgram.music;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import java.util.ArrayList;

public class SoundGramAudioFX {
    private static Equalizer equalizer;
    private static BassBoost bassBoost;
    private static PresetReverb reverb;
    private static Virtualizer virtualizer;
    private static LoudnessEnhancer loudnessEnhancer;

    private static Runnable sleepTimerRunnable;
    public static long sleepTimerEndTime = 0;

    private static float currentSpeed = 1.0f;
    private static float currentPitch = 1.0f;
    private static int currentSessionId = -1;
    private static short savedReverbPreset = 0;
    private static short savedBassLevel = 0;
    private static short savedVirtualizerLevel = 0;
    private static int savedLoudnessGain = 0;
    private static int currentEqPreset = -1; // -1 = Custom

    private static boolean equalizerEnabled = true;

    // Стандартные профили эквалайзера (mB)
    public static final String[] EQ_PRESET_NAMES = {
        "Flat", "Rock", "Pop", "Jazz", "Heavy Metal", "Hip-Hop", "Classical", "Dance", "Vocal Booster", "Bass Booster"
    };

    private static final short[][] EQ_PRESET_LEVELS = {
        {0, 0, 0, 0, 0},             // Flat
        {500, 300, -100, 300, 500},   // Rock
        {-100, 200, 500, 200, -100},  // Pop
        {400, 200, -200, 200, 500},   // Jazz
        {700, 400, 0, 400, 800},      // Heavy Metal
        {800, 500, 0, 200, 300},      // Hip-Hop
        {500, 300, -200, 400, 400},   // Classical
        {600, 0, 200, 400, 100},      // Dance
        {-200, 300, 700, 400, -100},  // Vocal Booster
        {900, 600, 200, 0, -200}      // Bass Booster
    };

    public static final String[] REVERB_PRESET_NAMES = {
        "Выкл", "Маленькая комната", "Средняя комната", "Большая комната", "Холл", "Концертный зал", "Пластинчатый"
    };

    public static final short[] REVERB_PRESET_VALUES = {
        PresetReverb.PRESET_NONE,
        PresetReverb.PRESET_SMALLROOM,
        PresetReverb.PRESET_MEDIUMROOM,
        PresetReverb.PRESET_LARGEROOM,
        PresetReverb.PRESET_MEDIUMHALL,
        PresetReverb.PRESET_LARGEHALL,
        PresetReverb.PRESET_PLATE
    };

    public static String getLocalizedReverbPresetName(int i) {
        int[] resIds = {
            R.string.sg_reverb_none,
            R.string.sg_reverb_smallroom,
            R.string.sg_reverb_mediumroom,
            R.string.sg_reverb_largeroom,
            R.string.sg_reverb_mediumhall,
            R.string.sg_reverb_largehall,
            R.string.sg_reverb_plate
        };
        if (i >= 0 && i < resIds.length) {
            return LocaleController.getString(resIds[i]);
        }
        return (i >= 0 && i < REVERB_PRESET_NAMES.length) ? REVERB_PRESET_NAMES[i] : "";
    }

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("soundgram_fx", Context.MODE_PRIVATE);
    }

    public static void releaseEffects() {
        if (equalizer != null) { try { equalizer.release(); } catch (Exception ignored) {} equalizer = null; }
        if (bassBoost != null) { try { bassBoost.release(); } catch (Exception ignored) {} bassBoost = null; }
        if (reverb != null) { try { reverb.release(); } catch (Exception ignored) {} reverb = null; }
        if (virtualizer != null) { try { virtualizer.release(); } catch (Exception ignored) {} virtualizer = null; }
        if (android.os.Build.VERSION.SDK_INT >= 19 && loudnessEnhancer != null) {
            try { loudnessEnhancer.release(); } catch (Exception ignored) {} loudnessEnhancer = null;
        }
    }

    public static void attach(int audioSessionId) {
        initFX(audioSessionId, true);
    }

    public static void detach() {
        currentSessionId = -1;
        applyEqualizerState(false);
        releaseEffects();
    }

    public static void initFX(int audioSessionId) {
        initFX(audioSessionId, false);
    }

    public static void initFX(int audioSessionId, boolean force) {
        if (audioSessionId <= 0) return;
        try {
            if (currentSessionId != audioSessionId || equalizer == null) {
                releaseEffects();
                currentSessionId = audioSessionId;
            }

            SharedPreferences prefs = getPrefs();
            equalizerEnabled = prefs.getBoolean("eq_enabled", true);
            savedReverbPreset = (short) prefs.getInt("reverb", 0);
            savedBassLevel = (short) prefs.getInt("bass", 0);
            savedVirtualizerLevel = (short) prefs.getInt("virtualizer", 0);
            savedLoudnessGain = prefs.getInt("loudness", 0);
            currentSpeed = prefs.getFloat("speed", 1.0f);
            currentPitch = prefs.getFloat("pitch", 1.0f);
            currentEqPreset = prefs.getInt("eq_preset", -1);
            boolean musicPlaying = isMusicPlaying();

            try {
                if (equalizer == null) {
                    try {
                        equalizer = new Equalizer(0, audioSessionId);
                    } catch (Exception e1) {
                        try {
                            equalizer = new Equalizer(1000, audioSessionId);
                        } catch (Exception e2) {
                            FileLog.e("SoundGram EQ init err: " + e2);
                        }
                    }
                    if (equalizer != null) {
                        for (short i = 0; i < equalizer.getNumberOfBands(); i++) {
                            equalizer.setBandLevel(i, (short) prefs.getInt("band_" + i, 0));
                        }
                    }
                }
                if (equalizer != null) {
                    equalizer.setEnabled(equalizerEnabled && musicPlaying);
                }
            } catch (Exception e) { FileLog.e("SoundGram EQ init err: " + e); }

            try {
                if (bassBoost == null) {
                    bassBoost = new BassBoost(0, audioSessionId);
                }
                if (bassBoost.getStrengthSupported()) {
                    bassBoost.setStrength(savedBassLevel);
                }
                bassBoost.setEnabled(equalizerEnabled && musicPlaying && savedBassLevel > 0);
            } catch (Exception e) { FileLog.e("SoundGram BassBoost init err: " + e); }

            try {
                if (reverb == null) {
                    reverb = new PresetReverb(0, audioSessionId);
                }
                reverb.setPreset(savedReverbPreset);
                reverb.setEnabled(equalizerEnabled && musicPlaying && savedReverbPreset != PresetReverb.PRESET_NONE);
            } catch (Exception e) { FileLog.e("SoundGram Reverb init err: " + e); }

            try {
                if (virtualizer == null) {
                    virtualizer = new Virtualizer(0, audioSessionId);
                }
                if (virtualizer.getStrengthSupported()) {
                    virtualizer.setStrength(savedVirtualizerLevel);
                }
                virtualizer.setEnabled(equalizerEnabled && musicPlaying && savedVirtualizerLevel > 0);
            } catch (Exception e) { FileLog.e("SoundGram Virtualizer init err: " + e); }

            if (android.os.Build.VERSION.SDK_INT >= 19) {
                try {
                    if (loudnessEnhancer == null) {
                        loudnessEnhancer = new LoudnessEnhancer(audioSessionId);
                    }
                    loudnessEnhancer.setTargetGain(savedLoudnessGain);
                    loudnessEnhancer.setEnabled(equalizerEnabled && musicPlaying && savedLoudnessGain > 0);
                } catch (Exception e) { FileLog.e("SoundGram Loudness init err: " + e); }
            }
        } catch (Exception e) {
            FileLog.e("SoundGram AudioFX Error: " + e.getMessage());
        }
    }

    public static boolean isMusicPlaying() {
        try {
            MessageObject mo = MediaController.getInstance().getPlayingMessageObject();
            if (mo != null) {
                return !mo.isVoice() && !mo.isRoundVideo();
            }
            return !MediaController.getInstance().isMessagePaused();
        } catch (Exception e) {
            return true;
        }
    }

    public static void applyFXForCurrentMessage() {
        try {
            int sessionId = MediaController.getInstance().getAudioSessionId();
            if (sessionId > 0 && sessionId != currentSessionId) {
                currentSessionId = -1;
                initFX(sessionId);
            }
        } catch (Exception ignored) {}
        applyEqualizerState(equalizerEnabled);
        if (isMusicPlaying()) {
            MediaController.getInstance().applyMusicPlaybackParams(currentSpeed, currentPitch);
        }
    }

    public static boolean isEqualizerEnabled() {
        return getPrefs().getBoolean("eq_enabled", true);
    }

    public static void setEqualizerEnabled(boolean enabled) {
        getPrefs().edit().putBoolean("eq_enabled", enabled).apply();
        equalizerEnabled = enabled;
        if (enabled && (equalizer == null || currentSessionId <= 0)) {
            try {
                int sessionId = MediaController.getInstance().getAudioSessionId();
                if (sessionId > 0) {
                    initFX(sessionId, false);
                }
            } catch (Exception ignored) {}
        }
        applyEqualizerState(enabled);
    }

    public static boolean hasActiveBands() {
        if (equalizer == null) return false;
        try {
            short n = equalizer.getNumberOfBands();
            for (short i = 0; i < n; i++) {
                if (equalizer.getBandLevel(i) != 0) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static void applyEqualizerState(boolean enabled) {
        try {
            boolean effectiveEnabled = enabled && isMusicPlaying();
            boolean activeBands = hasActiveBands();
            if (equalizer != null) {
                try {
                    equalizer.setEnabled(effectiveEnabled && activeBands);
                } catch (Exception e) {
                    FileLog.e("SoundGram EQ setEnabled err: " + e);
                }
            }
            if (bassBoost != null && bassBoost.getStrengthSupported()) {
                try {
                    bassBoost.setEnabled(effectiveEnabled && savedBassLevel > 0);
                } catch (Exception e) {
                    FileLog.e("SoundGram BassBoost setEnabled err: " + e);
                }
            }
            if (reverb != null) {
                try {
                    reverb.setEnabled(effectiveEnabled && savedReverbPreset != PresetReverb.PRESET_NONE);
                } catch (Exception e) {
                    FileLog.e("SoundGram Reverb setEnabled err: " + e);
                }
            }
            if (virtualizer != null && virtualizer.getStrengthSupported()) {
                try {
                    virtualizer.setEnabled(effectiveEnabled && savedVirtualizerLevel > 0);
                } catch (Exception e) {
                    FileLog.e("SoundGram Virtualizer setEnabled err: " + e);
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= 19 && loudnessEnhancer != null) {
                try {
                    int targetGain = savedLoudnessGain;
                    if (effectiveEnabled && activeBands && targetGain == 0) {
                        targetGain = 180; // Компенсация затухания DSP эквалайзера, чтобы трек не чах
                    }
                    loudnessEnhancer.setTargetGain(targetGain);
                    loudnessEnhancer.setEnabled(effectiveEnabled && targetGain > 0);
                } catch (Exception e) {
                    FileLog.e("SoundGram Loudness setEnabled err: " + e);
                }
            }
            FileLog.d("SoundGram EQ state changed to: " + effectiveEnabled);
        } catch (Exception e) {
            FileLog.e("Failed to apply EQ state: " + e.getMessage());
        }
    }

    // ── Эффекты ─────────────────────────────────────────────────────────────

    public static void setReverbPreset(short preset) {
        savedReverbPreset = preset;
        getPrefs().edit().putInt("reverb", preset).apply();
        if (reverb != null && equalizerEnabled) {
            reverb.setPreset(preset);
            reverb.setEnabled(preset != PresetReverb.PRESET_NONE && isMusicPlaying());
        }
    }
    public static short getReverbPreset() { return savedReverbPreset; }
    public static int getReverbId() { return reverb != null ? reverb.getId() : 0; }

    public static void setBassLevel(short level) {
        savedBassLevel = level;
        getPrefs().edit().putInt("bass", level).apply();
        if (bassBoost != null && bassBoost.getStrengthSupported() && equalizerEnabled) {
            bassBoost.setStrength(level);
            bassBoost.setEnabled(level > 0 && isMusicPlaying());
        }
    }
    public static short getBassLevel() { return savedBassLevel; }

    public static void setVirtualizerLevel(short level) {
        savedVirtualizerLevel = level;
        getPrefs().edit().putInt("virtualizer", level).apply();
        if (virtualizer != null && virtualizer.getStrengthSupported() && equalizerEnabled) {
            virtualizer.setStrength(level);
            virtualizer.setEnabled(level > 0 && isMusicPlaying());
        }
    }
    public static short getVirtualizerLevel() { return savedVirtualizerLevel; }

    public static void setLoudnessGain(int gainMb) {
        savedLoudnessGain = gainMb;
        getPrefs().edit().putInt("loudness", gainMb).apply();
        if (android.os.Build.VERSION.SDK_INT >= 19 && loudnessEnhancer != null && equalizerEnabled) {
            loudnessEnhancer.setTargetGain(gainMb);
            loudnessEnhancer.setEnabled(gainMb > 0 && isMusicPlaying());
        }
    }
    public static int getLoudnessGain() { return savedLoudnessGain; }

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

    public static class CustomPreset {
        public String name;
        public short[] levels;
        public short bassLevel;
        public short virtualizerLevel;
        public short reverbPreset;
        public int loudnessGain;
        public float speed;
        public float pitch;

        public CustomPreset(String name, short[] levels, short bassLevel, short virtualizerLevel, short reverbPreset, int loudnessGain, float speed, float pitch) {
            this.name = name;
            this.levels = levels;
            this.bassLevel = bassLevel;
            this.virtualizerLevel = virtualizerLevel;
            this.reverbPreset = reverbPreset;
            this.loudnessGain = loudnessGain;
            this.speed = speed;
            this.pitch = pitch;
        }
    }

    public static ArrayList<CustomPreset> getCustomPresets() {
        ArrayList<CustomPreset> list = new ArrayList<>();
        try {
            String jsonStr = getPrefs().getString("user_custom_presets", "[]");
            org.json.JSONArray array = new org.json.JSONArray(jsonStr);
            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);
                String name = obj.optString("name", "Custom " + (i + 1));
                short[] levels = new short[0];
                if (obj.has("levels")) {
                    org.json.JSONArray levelsArr = obj.getJSONArray("levels");
                    levels = new short[levelsArr.length()];
                    for (int j = 0; j < levelsArr.length(); j++) {
                        levels[j] = (short) levelsArr.getInt(j);
                    }
                }
                short bassLevel = (short) obj.optInt("bassLevel", 0);
                short virtualizerLevel = (short) obj.optInt("virtualizerLevel", 0);
                short reverbPreset = (short) obj.optInt("reverbPreset", 0);
                int loudnessGain = obj.optInt("loudnessGain", 0);
                float speed = (float) obj.optDouble("speed", 1.0);
                float pitch = (float) obj.optDouble("pitch", 1.0);

                list.add(new CustomPreset(name, levels, bassLevel, virtualizerLevel, reverbPreset, loudnessGain, speed, pitch));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public static void saveCustomPreset(String name) {
        if (name == null || name.trim().isEmpty()) return;
        short[] levels = getCurrentBandLevels();
        short bass = getBassLevel();
        short virt = getVirtualizerLevel();
        short reverb = getReverbPreset();
        int loudness = getLoudnessGain();
        float spd = getCurrentSpeed();
        float ptc = getCurrentPitch();

        ArrayList<CustomPreset> current = getCustomPresets();
        current.add(new CustomPreset(name.trim(), levels, bass, virt, reverb, loudness, spd, ptc));
        saveCustomPresetsList(current);
    }

    public static void deleteCustomPreset(int index) {
        ArrayList<CustomPreset> current = getCustomPresets();
        if (index >= 0 && index < current.size()) {
            current.remove(index);
            saveCustomPresetsList(current);
        }
    }

    private static void saveCustomPresetsList(ArrayList<CustomPreset> list) {
        try {
            org.json.JSONArray array = new org.json.JSONArray();
            for (CustomPreset cp : list) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("name", cp.name);
                org.json.JSONArray levelsArr = new org.json.JSONArray();
                if (cp.levels != null) {
                    for (short lvl : cp.levels) {
                        levelsArr.put(lvl);
                    }
                }
                obj.put("levels", levelsArr);
                obj.put("bassLevel", cp.bassLevel);
                obj.put("virtualizerLevel", cp.virtualizerLevel);
                obj.put("reverbPreset", cp.reverbPreset);
                obj.put("loudnessGain", cp.loudnessGain);
                obj.put("speed", cp.speed);
                obj.put("pitch", cp.pitch);
                array.put(obj);
            }
            getPrefs().edit().putString("user_custom_presets", array.toString()).apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void applyCustomPreset(CustomPreset preset, int index) {
        if (preset == null) return;

        if (preset.levels != null) {
            short bands = getBandsCount();
            if (bands <= 0) bands = (short) preset.levels.length;
            for (short i = 0; i < bands; i++) {
                short lvl = (i < preset.levels.length) ? preset.levels[i] : 0;
                getPrefs().edit().putInt("band_" + i, lvl).apply();
                if (equalizer != null && equalizerEnabled) {
                    try { equalizer.setBandLevel(i, lvl); } catch (Exception ignored) {}
                }
            }
        }

        setBassLevel(preset.bassLevel);
        setVirtualizerLevel(preset.virtualizerLevel);
        setReverbPreset(preset.reverbPreset);
        setLoudnessGain(preset.loudnessGain);

        setCurrentSpeed(preset.speed);
        setCurrentPitch(preset.pitch);
        try {
            org.telegram.messenger.MediaController.getInstance().applyMusicPlaybackParams(preset.speed, preset.pitch);
        } catch (Exception ignored) {}

        currentEqPreset = 100 + index;
        getPrefs().edit().putInt("eq_preset", currentEqPreset).apply();
    }

    public static short[] getCurrentBandLevels() {
        short bands = getBandsCount();
        if (bands <= 0) bands = 5;
        short[] levels = new short[bands];
        for (short i = 0; i < bands; i++) {
            levels[i] = getBandLevel(i);
        }
        return levels;
    }

    public static void setBandLevel(short band, short level) {
        getPrefs().edit().putInt("band_" + band, level).apply();
        if (equalizer != null && equalizerEnabled) equalizer.setBandLevel(band, level);
        currentEqPreset = -1;
        getPrefs().edit().putInt("eq_preset", -1).apply();
    }
    public static short getBandLevel(short band) { return equalizer != null ? equalizer.getBandLevel(band) : (short) getPrefs().getInt("band_" + band, 0); }
    public static short getBandsCount() { return equalizer != null ? equalizer.getNumberOfBands() : 10; }
    public static int getCenterFreq(short band) {
        if (equalizer != null) return equalizer.getCenterFreq(band) / 1000;
        int[] defaultFreqs = {31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000};
        return (band >= 0 && band < defaultFreqs.length) ? defaultFreqs[band] : 1000;
    }

    public static int getCurrentEqPreset() { return currentEqPreset; }
    public static void setCurrentEqPreset(int preset) {
        currentEqPreset = preset;
        getPrefs().edit().putInt("eq_preset", preset).apply();
    }
    public static void applyEqPreset(int presetIndex) {
        if (presetIndex < 0 || presetIndex >= EQ_PRESET_LEVELS.length) return;
        short[] levels = EQ_PRESET_LEVELS[presetIndex];
        short bands = getBandsCount();
        if (bands <= 0) bands = (short) levels.length;
        for (short i = 0; i < bands; i++) {
            short lvl = (i < levels.length) ? levels[i] : 0;
            getPrefs().edit().putInt("band_" + i, lvl).apply();
            if (equalizer != null && equalizerEnabled) {
                try { equalizer.setBandLevel(i, lvl); } catch (Exception ignored) {}
            }
        }
        currentEqPreset = presetIndex;
        getPrefs().edit().putInt("eq_preset", presetIndex).apply();
    }

    public static void applyQuickMode(String mode) {
        if ("slowed".equalsIgnoreCase(mode)) {
            setCurrentSpeed(0.85f);
            setCurrentPitch(0.85f);
            setReverbPreset(PresetReverb.PRESET_LARGEHALL);
            setBassLevel((short) 400);
            setVirtualizerLevel((short) 300);
        } else if ("nightcore".equalsIgnoreCase(mode)) {
            setCurrentSpeed(1.25f);
            setCurrentPitch(1.25f);
            setReverbPreset(PresetReverb.PRESET_SMALLROOM);
            setBassLevel((short) 200);
        } else if ("concert".equalsIgnoreCase(mode)) {
            setCurrentSpeed(1.0f);
            setCurrentPitch(1.0f);
            setReverbPreset(PresetReverb.PRESET_LARGEHALL);
            setVirtualizerLevel((short) 800);
            setBassLevel((short) 300);
        } else if ("bassmax".equalsIgnoreCase(mode)) {
            setCurrentSpeed(1.0f);
            setCurrentPitch(1.0f);
            setBassLevel((short) 1000);
            setLoudnessGain(500);
            setReverbPreset(PresetReverb.PRESET_NONE);
            applyEqPreset(9); // Bass Booster preset
        }
        if (isMusicPlaying()) {
            MediaController.getInstance().applyMusicPlaybackParams(currentSpeed, currentPitch);
        }
    }

    public static void resetFX() {
        setBassLevel((short) 0);
        setVirtualizerLevel((short) 0);
        setLoudnessGain(0);
        setReverbPreset(PresetReverb.PRESET_NONE);
        setCurrentSpeed(1.0f);
        setCurrentPitch(1.0f);
        currentEqPreset = 0; // Flat
        if (equalizer != null) {
            for (short i = 0; i < equalizer.getNumberOfBands(); i++) setBandLevel(i, (short) 0);
        }
        try { MediaController.getInstance().applyMusicPlaybackParams(1.0f, 1.0f); } catch (Exception ignored) {}
    }

    public static void startSleepTimer(int minutes) {
        cancelSleepTimer();
        sleepTimerEndTime = System.currentTimeMillis() + (minutes * 60 * 1000L);
        sleepTimerRunnable = () -> {
            MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
            if (playing != null && !MediaController.getInstance().isMessagePaused()) {
                MediaController.getInstance().pauseMessage(playing);
            }
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
