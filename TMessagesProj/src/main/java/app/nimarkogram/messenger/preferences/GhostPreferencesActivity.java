package app.nimarkogram.messenger.preferences;

import android.view.View;

import com.radolyn.ayugram.messages.AyuMessagesController;
import com.radolyn.ayugram.utils.AyuState;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.IconBackgroundColors;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import app.nimarkogram.messenger.preferences.helpers.SettingsHelper;
import tw.nekomimi.nekogram.NekoConfig;
import xyz.nextalone.nagram.NaConfig;

public class GhostPreferencesActivity extends BasePreferencesActivity {

    private static final int ID_GHOST_MASTER = 1;
    private static final int ID_SEND_READ_MESSAGES = 2;
    private static final int ID_SEND_ONLINE = 3;
    private static final int ID_SEND_TYPING = 4;
    private static final int ID_SEND_READ_STORIES = 5;
    private static final int ID_SEND_OFFLINE_AFTER = 6;
    private static final int ID_MARK_READ_AFTER_SEND = 7;
    private static final int ID_SILENT_BY_DEFAULT = 8;
    private static final int ID_SHOW_IN_DRAWER = 9;
    private static final int ID_STATUS_INDICATOR = 10;

    private static final int ID_SAVE_DELETED = 20;
    private static final int ID_SAVE_MEDIA = 21;
    private static final int ID_SAVE_DELETED_BOT = 22;
    private static final int ID_SAVE_DELETED_BOT_USER = 23;
    private static final int ID_TRANSLUCENT_DELETED = 24;
    private static final int ID_USE_DELETED_ICON = 25;
    private static final int ID_VIEW_DELETED = 26;

    private static final int ID_SAVE_EDITS = 30;

    private static final int ID_CLEAR_DATABASE = 40;

    @Override
    public String getTitle() {
        return LocaleController.getString(R.string.GhostMode);
    }

    @Override
    public void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // Section: Ghost Mode Master
        items.add(UItem.asHeader(LocaleController.getString(R.string.GhostMode)));
        items.add(SettingsHelper.asSwitchCG(ID_GHOST_MASTER,
                        LocaleController.getString(R.string.GhostMode),
                        NekoConfig.isGhostModeActive()
                                ? LocaleController.getString(R.string.GhostModeEnabled)
                                : LocaleController.getString(R.string.GhostModeDisabled))
                .setChecked(NekoConfig.isGhostModeActive()));
        items.add(UItem.asShadow(null));

        // Section: Stealth Settings
        items.add(UItem.asHeader(LocaleController.getString(R.string.GhostEssentialsHeader)));
        items.add(SettingsHelper.asSwitchCG(ID_SEND_READ_MESSAGES,
                        LocaleController.getString(R.string.DontSendReadMessagePackets),
                        LocaleController.getString(R.string.SendReadMessagePacketsNotice))
                .setChecked(!NekoConfig.sendReadMessagePackets.Bool()));

        items.add(SettingsHelper.asSwitchCG(ID_SEND_ONLINE,
                        LocaleController.getString(R.string.DontSendOnlinePackets),
                        LocaleController.getString(R.string.SendOnlinePacketsNotice))
                .setChecked(!NekoConfig.sendOnlinePackets.Bool()));

        items.add(SettingsHelper.asSwitchCG(ID_SEND_TYPING,
                        LocaleController.getString(R.string.DontSendUploadProgress),
                        LocaleController.getString(R.string.SendUploadProgressNotice))
                .setChecked(!NekoConfig.sendUploadProgress.Bool()));

        items.add(SettingsHelper.asSwitchCG(ID_SEND_READ_STORIES,
                        LocaleController.getString(R.string.DontReadStoriesPackets),
                        LocaleController.getString(R.string.SendReadStoriesPacketsNotice))
                .setChecked(!NekoConfig.sendReadStoriesPackets.Bool()));

        items.add(SettingsHelper.asSwitchCG(ID_SEND_OFFLINE_AFTER,
                        LocaleController.getString(R.string.SendOfflinePacketAfterOnline),
                        LocaleController.getString(R.string.SendOfflinePacketAfterOnlineNotice))
                .setChecked(NekoConfig.sendOfflinePacketAfterOnline.Bool()));

        items.add(SettingsHelper.asSwitchCG(ID_MARK_READ_AFTER_SEND,
                        LocaleController.getString(R.string.MarkReadAfterSend),
                        LocaleController.getString(R.string.MarkReadAfterSendNotice))
                .setChecked(NekoConfig.markReadAfterSend.Bool()));

        items.add(SettingsHelper.asSwitchCG(ID_SILENT_BY_DEFAULT,
                        LocaleController.getString(R.string.SilentMessageByDefault),
                        null)
                .setChecked(NaConfig.INSTANCE.getSilentMessageByDefault().Bool()));

        items.add(SettingsHelper.asSwitchCG(ID_SHOW_IN_DRAWER,
                        LocaleController.getString(R.string.GhostModeInDrawer),
                        null)
                .setChecked(NekoConfig.showGhostInDrawer.Bool()));

        items.add(SettingsHelper.asSwitchCG(ID_STATUS_INDICATOR,
                        LocaleController.getString(R.string.GhostModeStatusIndicator),
                        null)
                .setChecked(NekoConfig.showGhostModeStatus.Bool()));
        items.add(UItem.asShadow(null));

        // Section: Deleted Messages (удалёнки)
        items.add(UItem.asHeader(LocaleController.getString(R.string.EnableSaveDeletedMessages)));
        items.add(SettingsHelper.asSwitchCG(ID_SAVE_DELETED,
                        LocaleController.getString(R.string.EnableSaveDeletedMessages),
                        null)
                .setChecked(NaConfig.INSTANCE.getEnableSaveDeletedMessages().Bool()));

        if (NaConfig.INSTANCE.getEnableSaveDeletedMessages().Bool()) {
            items.add(SettingsHelper.asSwitchCG(ID_SAVE_MEDIA,
                            LocaleController.getString(R.string.MessageSavingSaveMedia),
                            LocaleController.getString(R.string.MessageSavingSaveMediaHint))
                    .setChecked(NaConfig.INSTANCE.getMessageSavingSaveMedia().Bool()));

            items.add(SettingsHelper.asSwitchCG(ID_SAVE_DELETED_BOT,
                            LocaleController.getString(R.string.SaveDeletedMessageForBot),
                            null)
                    .setChecked(NaConfig.INSTANCE.getSaveDeletedMessageForBot().Bool()));

            items.add(SettingsHelper.asSwitchCG(ID_SAVE_DELETED_BOT_USER,
                            LocaleController.getString(R.string.SaveDeletedMessageForBotUser),
                            null)
                    .setChecked(NaConfig.INSTANCE.getSaveDeletedMessageForBotUser().Bool()));

            items.add(SettingsHelper.asSwitchCG(ID_TRANSLUCENT_DELETED,
                            LocaleController.getString(R.string.TranslucentDeletedMessages),
                            null)
                    .setChecked(NaConfig.INSTANCE.getTranslucentDeletedMessages().Bool()));

            items.add(SettingsHelper.asSwitchCG(ID_USE_DELETED_ICON,
                            LocaleController.getString(R.string.UseDeletedIcon),
                            null)
                    .setChecked(NaConfig.INSTANCE.getUseDeletedIcon().Bool()));

            items.add(asSettingsLink(ID_VIEW_DELETED, IconBackgroundColors.BLUE,
                    R.drawable.msg_delete_solar,
                    LocaleController.getString(R.string.ViewDeleted)));
        }
        items.add(UItem.asShadow(null));

        // Section: Edited Messages (изменёнки)
        items.add(UItem.asHeader(LocaleController.getString(R.string.EnableSaveEditsHistory)));
        items.add(SettingsHelper.asSwitchCG(ID_SAVE_EDITS,
                        LocaleController.getString(R.string.EnableSaveEditsHistory),
                        null)
                .setChecked(NaConfig.INSTANCE.getEnableSaveEditsHistory().Bool()));
        items.add(UItem.asShadow(null));

        // Section: Maintenance
        items.add(UItem.asHeader(LocaleController.getString(R.string.ClearMessageDatabase)));
        items.add(asSettingsLink(ID_CLEAR_DATABASE, IconBackgroundColors.RED,
                R.drawable.msg_clear,
                LocaleController.getString(R.string.ClearMessageDatabase)).red());
        items.add(UItem.asShadow(null));
    }

    @Override
    public void onClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;
        if (id == ID_GHOST_MASTER) {
            NekoConfig.toggleGhostMode();
            AyuState.setAllowReadPacket(false, -1);
            NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_SEND_READ_MESSAGES) {
            boolean currentDontSend = !NekoConfig.sendReadMessagePackets.Bool();
            NekoConfig.sendReadMessagePackets.setConfigBool(currentDontSend);
            AyuState.setAllowReadPacket(false, -1);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_SEND_ONLINE) {
            boolean currentDontSend = !NekoConfig.sendOnlinePackets.Bool();
            NekoConfig.sendOnlinePackets.setConfigBool(currentDontSend);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_SEND_TYPING) {
            boolean currentDontSend = !NekoConfig.sendUploadProgress.Bool();
            NekoConfig.sendUploadProgress.setConfigBool(currentDontSend);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_SEND_READ_STORIES) {
            boolean currentDontSend = !NekoConfig.sendReadStoriesPackets.Bool();
            NekoConfig.sendReadStoriesPackets.setConfigBool(currentDontSend);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_SEND_OFFLINE_AFTER) {
            NekoConfig.sendOfflinePacketAfterOnline.toggleConfigBool();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_MARK_READ_AFTER_SEND) {
            NekoConfig.markReadAfterSend.toggleConfigBool();
            AyuState.setAllowReadPacket(false, -1);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_SILENT_BY_DEFAULT) {
            NaConfig.INSTANCE.getSilentMessageByDefault().toggleConfigBool();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_SHOW_IN_DRAWER) {
            NekoConfig.showGhostInDrawer.toggleConfigBool();
            NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_STATUS_INDICATOR) {
            NekoConfig.showGhostModeStatus.toggleConfigBool();
            NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_SAVE_DELETED) {
            NaConfig.INSTANCE.getEnableSaveDeletedMessages().toggleConfigBool();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_SAVE_MEDIA) {
            NaConfig.INSTANCE.getMessageSavingSaveMedia().toggleConfigBool();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_SAVE_DELETED_BOT) {
            NaConfig.INSTANCE.getSaveDeletedMessageForBot().toggleConfigBool();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_SAVE_DELETED_BOT_USER) {
            NaConfig.INSTANCE.getSaveDeletedMessageForBotUser().toggleConfigBool();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_TRANSLUCENT_DELETED) {
            NaConfig.INSTANCE.getTranslucentDeletedMessages().toggleConfigBool();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_USE_DELETED_ICON) {
            NaConfig.INSTANCE.getUseDeletedIcon().toggleConfigBool();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_SAVE_EDITS) {
            NaConfig.INSTANCE.getEnableSaveEditsHistory().toggleConfigBool();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_VIEW_DELETED) {
            presentFragment(new com.radolyn.ayugram.ui.AyuViewDeleted(0));
        } else if (id == ID_CLEAR_DATABASE) {
            new AlertDialog.Builder(getContext(), getResourceProvider())
                    .setTitle(LocaleController.getString(R.string.ClearMessageDatabase))
                    .setMessage(LocaleController.getString(R.string.AreYouSure))
                    .setPositiveButton(LocaleController.getString(R.string.Clear), (dialog, which) -> {
                        AlertDialog progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
                        progressDialog.setCanCancel(false);
                        progressDialog.show();
                        Utilities.globalQueue.postRunnable(() -> {
                            AyuMessagesController.getInstance().clean();
                            AndroidUtilities.runOnUIThread(() -> {
                                progressDialog.dismiss();
                                BulletinFactory.of(this).createSimpleBulletin(R.raw.done, LocaleController.getString(R.string.ClearMessageDatabaseNotification)).show();
                            });
                        });
                    })
                    .setNegativeButton(LocaleController.getString(R.string.Cancel), (d, w) -> d.dismiss())
                    .makeRed(AlertDialog.BUTTON_POSITIVE)
                    .show();
        }
    }
}
