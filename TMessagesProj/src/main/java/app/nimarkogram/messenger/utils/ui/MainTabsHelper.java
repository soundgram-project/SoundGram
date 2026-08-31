package app.nimarkogram.messenger.utils.ui;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.LaunchActivity;

public class MainTabsHelper {

    public static int getMainTabsMargin() {
        return 0;
    }

    public static int getMainTabsHeight() {
        return 48;
    }

    public static int getTabsViewWidth() {
        return AndroidUtilities.displaySize.x;
    }

    public static void openMusicTab() {
        try {
            LaunchActivity activity = LaunchActivity.instance;
            if (activity != null) {
                // switch to music tab
            }
        } catch (Exception ignore) {}
    }
}
