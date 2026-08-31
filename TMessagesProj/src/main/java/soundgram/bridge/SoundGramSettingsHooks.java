package soundgram.bridge;

import android.content.Context;
import org.telegram.ui.ActionBar.BaseFragment;
import soundgram.wsproxy.NekoWsProxyActivity;
import soundgram.icons.IconPacksActivity;
import soundgram.animation.TextAnimationSettingsActivity;
import soundgram.ui.SoundGramStyleSettingsFragment;

public class SoundGramSettingsHooks {

    public static boolean openSoundGramSettings(BaseFragment fragment, String key) {
        if (fragment == null || key == null) return false;
        
        switch (key) {
            case "wsProxy":
                fragment.presentFragment(new NekoWsProxyActivity());
                return true;
            case "iconPacks":
                fragment.presentFragment(new IconPacksActivity());
                return true;
            case "textAnimation":
                fragment.presentFragment(new TextAnimationSettingsActivity());
                return true;
            case "soundGramStyle":
                fragment.presentFragment(new SoundGramStyleSettingsFragment());
                return true;
        }
        return false;
    }
}
