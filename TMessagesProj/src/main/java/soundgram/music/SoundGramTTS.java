package soundgram.music;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

public class SoundGramTTS {

    public static void init(Context context) {
        // No-op: using system accessibility (TalkBack) without external TTS downloads
    }

    public static void speak(CharSequence text) {
        speak(null, text);
    }

    public static void speak(View view, CharSequence text) {
        if (TextUtils.isEmpty(text)) return;
        if (view != null) {
            try {
                view.setContentDescription(text);
                view.announceForAccessibility(text);
            } catch (Exception ignored) {}
        }
    }

    public static void attach(View view, CharSequence description) {
        if (view == null || TextUtils.isEmpty(description)) return;
        try {
            view.setContentDescription(description);
            view.setFocusable(true);
        } catch (Exception ignored) {}
    }

    public static void attachWithClick(View view, CharSequence description, View.OnClickListener onClickListener) {
        if (view == null) return;
        try {
            if (!TextUtils.isEmpty(description)) {
                view.setContentDescription(description);
                view.setFocusable(true);
            }
            if (onClickListener != null) {
                view.setOnClickListener(onClickListener);
            }
        } catch (Exception ignored) {}
    }
}

