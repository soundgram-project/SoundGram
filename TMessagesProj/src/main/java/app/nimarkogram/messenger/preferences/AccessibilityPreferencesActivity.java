/*
 * Copyright github.com/arsLan4k1390, 2022-2026.
 * Licensed under GNU GPL v2 or later. See LICENSE.
 */
package app.nimarkogram.messenger.preferences;

import android.view.View;

import java.util.ArrayList;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import app.nimarkogram.messenger.NimarkoConfig;

public class AccessibilityPreferencesActivity extends BasePreferencesActivity {

    private static final int ID_HIDE_SHARE_TRANSCRIBE   = 1;
    private static final int ID_HIDE_ADMIN_TAGS         = 2;
    private static final int ID_HIDE_GROUP_NICKNAMES    = 3;
    private static final int ID_DISABLE_VOICE_AUTOPLAY  = 4;
    private static final int ID_DOWNLOAD_SOUND          = 5;
    private static final int ID_DOWNLOAD_PERCENT        = 6;
    private static final int ID_HIDE_COMPOSER_BUTTONS   = 7;
    private static final int ID_DISABLE_ALL_ANIMATIONS  = 8;

    @Override
    public String getTitle() {
        return LocaleController.getString(R.string.A11ySettingsTitle);
    }

    @Override
    public void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString(R.string.A11ySectionButtons)));
        items.add(UItem.asCheck(ID_HIDE_SHARE_TRANSCRIBE,
                LocaleController.getString(R.string.A11yHideShareAndTranscribe),
                LocaleController.getString(R.string.A11yHideShareAndTranscribeDesc),
                true)
                .setChecked(NimarkoConfig.a11yHideShareAndTranscribe));
        items.add(UItem.asCheck(ID_HIDE_ADMIN_TAGS,
                LocaleController.getString(R.string.A11yHideAdminTags),
                LocaleController.getString(R.string.A11yHideAdminTagsDesc),
                true)
                .setChecked(NimarkoConfig.a11yHideAdminTags));
        items.add(UItem.asCheck(ID_HIDE_GROUP_NICKNAMES,
                LocaleController.getString(R.string.A11yHideGroupNicknames),
                LocaleController.getString(R.string.A11yHideGroupNicknamesDesc),
                true)
                .setChecked(NimarkoConfig.a11yHideGroupNicknames));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.A11ySectionVoiceMedia)));
        items.add(UItem.asCheck(ID_DISABLE_VOICE_AUTOPLAY,
                LocaleController.getString(R.string.A11yDisableVoiceAutoplay),
                LocaleController.getString(R.string.A11yDisableVoiceAutoplayDesc),
                true)
                .setChecked(NimarkoConfig.a11yDisableVoiceAutoplay));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.A11ySectionDownloads)));
        items.add(UItem.asCheck(ID_DOWNLOAD_SOUND,
                LocaleController.getString(R.string.A11yDownloadSound),
                LocaleController.getString(R.string.A11yDownloadSoundDesc),
                true)
                .setChecked(NimarkoConfig.a11yDownloadSound));
        items.add(UItem.asCheck(ID_DOWNLOAD_PERCENT,
                LocaleController.getString(R.string.A11yDownloadPercentTalkback),
                LocaleController.getString(R.string.A11yDownloadPercentTalkbackDesc),
                true)
                .setChecked(NimarkoConfig.a11yDownloadPercentTalkback));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.A11ySectionComposer)));
        items.add(UItem.asCheck(ID_HIDE_COMPOSER_BUTTONS,
                LocaleController.getString(R.string.A11yHideComposerEditorButtons),
                LocaleController.getString(R.string.A11yHideComposerEditorButtonsDesc),
                true)
                .setChecked(NimarkoConfig.a11yHideComposerEditorButtons));
        items.add(UItem.asCheck(ID_DISABLE_ALL_ANIMATIONS,
                LocaleController.getString(R.string.A11yDisableAllAnimations),
                LocaleController.getString(R.string.A11yDisableAllAnimationsDesc),
                true)
                .setChecked(NimarkoConfig.a11yDisableAllAnimations));
        items.add(UItem.asShadow(null));
    }

    @Override
    public void onClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;
        if (id == ID_HIDE_SHARE_TRANSCRIBE) {
            NimarkoConfig.toggleA11yHideShareAndTranscribe();
            applyCheck(item, view, NimarkoConfig.a11yHideShareAndTranscribe);
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        } else if (id == ID_HIDE_ADMIN_TAGS) {
            NimarkoConfig.toggleA11yHideAdminTags();
            applyCheck(item, view, NimarkoConfig.a11yHideAdminTags);
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        } else if (id == ID_HIDE_GROUP_NICKNAMES) {
            NimarkoConfig.toggleA11yHideGroupNicknames();
            applyCheck(item, view, NimarkoConfig.a11yHideGroupNicknames);
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        } else if (id == ID_DISABLE_VOICE_AUTOPLAY) {
            NimarkoConfig.toggleA11yDisableVoiceAutoplay();
            applyCheck(item, view, NimarkoConfig.a11yDisableVoiceAutoplay);
        } else if (id == ID_DOWNLOAD_SOUND) {
            NimarkoConfig.toggleA11yDownloadSound();
            applyCheck(item, view, NimarkoConfig.a11yDownloadSound);
        } else if (id == ID_DOWNLOAD_PERCENT) {
            NimarkoConfig.toggleA11yDownloadPercentTalkback();
            applyCheck(item, view, NimarkoConfig.a11yDownloadPercentTalkback);
        } else if (id == ID_HIDE_COMPOSER_BUTTONS) {
            NimarkoConfig.toggleA11yHideComposerEditorButtons();
            applyCheck(item, view, NimarkoConfig.a11yHideComposerEditorButtons);
        } else if (id == ID_DISABLE_ALL_ANIMATIONS) {
            NimarkoConfig.toggleA11yDisableAllAnimations();
            applyCheck(item, view, NimarkoConfig.a11yDisableAllAnimations);
        }
    }

    private void applyCheck(UItem item, View view, boolean value) {
        item.checked = value;
        if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(value);
        }
    }
}
