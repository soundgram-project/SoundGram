package org.telegram.ui;

public interface MainTabsActivityController {
    void setTabsVisible(boolean visible);
    default void setTabsHiddenByOverlay(boolean hidden) {}
    default boolean openTab(app.nimarkogram.messenger.utils.ui.MainTabsManager.TabType type) { return false; }
}
