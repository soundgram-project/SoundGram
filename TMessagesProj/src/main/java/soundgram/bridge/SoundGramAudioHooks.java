package soundgram.bridge;

import soundgram.music.SoundGramAudioFX;

public class SoundGramAudioHooks {

    public static void onAudioSessionCreated(int audioSessionId) {
        if (audioSessionId > 0) {
            try {
                SoundGramAudioFX.attach(audioSessionId);
            } catch (Exception e) {
                // Ignore audio effect attachment failures gracefully
            }
        }
    }

    public static void detachAudioSession() {
        try {
            SoundGramAudioFX.detach();
        } catch (Exception e) {
            // Ignore
        }
    }

    public static boolean isHitmosStream(String url) {
        if (url == null) return false;
        return url.contains("hitmos.fm") || url.contains("pl1.hitmos") || url.contains("dw1.hitmos");
    }
}
