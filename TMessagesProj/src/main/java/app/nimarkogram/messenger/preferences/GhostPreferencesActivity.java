package app.nimarkogram.messenger.preferences;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.radolyn.ayugram.messages.AyuMessagesController;
import com.radolyn.ayugram.utils.AyuGhostPreferences;
import com.radolyn.ayugram.utils.AyuState;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.DialogsActivity;

import java.util.ArrayList;
import java.util.Set;

import app.nimarkogram.messenger.preferences.helpers.SettingsHelper;
import xyz.nextalone.nagram.NaConfig;

public class GhostPreferencesActivity extends BasePreferencesActivity {

    private static final int ID_GHOST_MASTER = 1;
    private static final int ID_INSTANT_OFFLINE = 2;
    private static final int ID_BLOCK_TYPING = 3;
    private static final int ID_BLOCK_READ = 4;
    private static final int ID_BLOCK_STORIES = 5;
    private static final int ID_BLOCK_CHANNEL_VIEWS = 6;
    private static final int ID_MUTE_SEND = 7;

    private static final int ID_SAVE_DELETED = 20;
    private static final int ID_SAVE_MEDIA = 21;
    private static final int ID_SAVE_DELETED_BOT = 22;
    private static final int ID_SAVE_DELETED_BOT_USER = 23;
    private static final int ID_TRANSLUCENT_DELETED = 24;
    private static final int ID_USE_DELETED_ICON = 25;
    private static final int ID_VIEW_DELETED = 26;
    private static final int ID_KEEP_ONCE = 27;
    private static final int ID_SAVE_EDITS = 28;
    private static final int ID_CLEAR_DATABASE = 29;

    private static final int ID_BLOCK_MEDIA_SEEN = 30;
    private static final int ID_ALLOW_SCREENSHOTS = 31;
    private static final int ID_NO_SCREENSHOT_NOTIFY = 32;
    private static final int ID_BYPASS_FORWARD = 33;
    private static final int ID_ALLOW_COPY = 34;
    private static final int ID_BYPASS_SAVE = 35;
    private static final int ID_BYPASS_STORY = 36;
    private static final int ID_HIDE_RECOMMENDED = 37;

    private static final int ID_PERCHAT_ENABLED = 40;
    private static final int ID_PC_MODE = 41;
    private static final int ID_PC_MANAGE = 42;

    private static boolean isRussian() {
        String lang = LocaleController.getInstance().getCurrentLocale().getLanguage();
        return lang != null && lang.startsWith("ru");
    }

    private static String loc(String ru, String en) {
        return isRussian() ? ru : en;
    }

    @Override
    public String getTitle() {
        return loc("Призрак", "Ghost");
    }

    @Override
    public void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // Section: Ghost Mode Master
        boolean active = AyuGhostPreferences.isGhostModeActive();
        items.add(SettingsHelper.asSwitchCG(ID_GHOST_MASTER,
                        loc("Призрачный режим", "Ghost mode"),
                        loc("Главный переключатель режима невидимки", "Master switch for ghost mode"))
                .setChecked(active));
        items.add(UItem.asShadow(null));

        // Section: Stealth Settings
        items.add(UItem.asHeader(loc("Скрытность", "Stealth")));

        items.add(SettingsHelper.asSwitchCG(ID_INSTANT_OFFLINE,
                        loc("Мгновенный оффлайн", "Instant offline"),
                        loc("Автоматически скрывать онлайн после любого действия", "Automatically hide online status after any action"))
                .setChecked(AyuGhostPreferences.isInstantOfflineConfigured()));

        items.add(SettingsHelper.asSwitchCG(ID_BLOCK_TYPING,
                        loc("Скрыть «печатает...»", "Hide typing status"),
                        loc("Собеседник не увидит, что вы набираете текст", "Others won't see when you're typing"))
                .setChecked(AyuGhostPreferences.isBlockTypingConfigured()));

        items.add(SettingsHelper.asSwitchCG(ID_BLOCK_READ,
                        loc("Не отправлять прочтение", "Don't send read receipts"),
                        loc("Сообщения останутся непрочитанными для отправителя", "Messages will appear unread to the sender"))
                .setChecked(AyuGhostPreferences.isBlockReadConfigured()));

        items.add(SettingsHelper.asSwitchCG(ID_BLOCK_STORIES,
                        loc("Скрыть просмотр историй", "Hide story views"),
                        loc("Автор не увидит вас в списке просмотревших", "Story author won't see you in the viewers list"))
                .setChecked(AyuGhostPreferences.isBlockStoriesConfigured()));

        items.add(SettingsHelper.asSwitchCG(ID_BLOCK_CHANNEL_VIEWS,
                        loc("Скрыть просмотры каналов", "Hide channel views"),
                        loc("Счётчик просмотров поста не увеличится", "The post view counter won't increase"))
                .setChecked(AyuGhostPreferences.isBlockChannelViewsConfigured()));

        int muteVal = AyuGhostPreferences.getMuteSend();
        String muteSubtitle = muteVal == 1
                ? loc("Всегда", "Always")
                : (muteVal == 2
                ? loc("Только в призрачном режиме", "Ghost mode only")
                : loc("Выключено", "Disabled"));
        items.add(UItem.asButton(ID_MUTE_SEND, R.drawable.msg_mute, loc("Беззвучная отправка", "Silent sending"), muteSubtitle));

        items.add(UItem.asShadow(null));

        // Section: Deleted & Ephemeral Messages
        items.add(UItem.asHeader(loc("Удалённое и исчезающее", "Deleted & Ephemeral")));

        items.add(SettingsHelper.asSwitchCG(ID_SAVE_DELETED,
                        loc("Сохранять удалённые", "Save deleted messages"),
                        loc("Перехватывать сообщения, которые удалил собеседник", "Intercept messages deleted by others"))
                .setChecked(NaConfig.INSTANCE.getEnableSaveDeletedMessages().Bool()));

        if (NaConfig.INSTANCE.getEnableSaveDeletedMessages().Bool()) {
            items.add(SettingsHelper.asSwitchCG(ID_SAVE_MEDIA,
                            LocaleController.getString(R.string.MessageSavingSaveMedia),
                            LocaleController.getString(R.string.MessageSavingSaveMediaHint))
                    .setChecked(NaConfig.INSTANCE.getMessageSavingSaveMedia().Bool()));

            items.add(SettingsHelper.asSwitchCG(ID_SAVE_DELETED_BOT,
                            LocaleController.getString(R.string.SaveDeletedMessageForBot))
                    .setChecked(NaConfig.INSTANCE.getSaveDeletedMessageForBot().Bool()));

            items.add(SettingsHelper.asSwitchCG(ID_SAVE_DELETED_BOT_USER,
                            LocaleController.getString(R.string.SaveDeletedMessageForBotUser))
                    .setChecked(NaConfig.INSTANCE.getSaveDeletedMessageForBotUser().Bool()));

            items.add(SettingsHelper.asSwitchCG(ID_TRANSLUCENT_DELETED,
                            LocaleController.getString(R.string.TranslucentDeletedMessages))
                    .setChecked(NaConfig.INSTANCE.getTranslucentDeletedMessages().Bool()));

            items.add(SettingsHelper.asSwitchCG(ID_USE_DELETED_ICON,
                            LocaleController.getString(R.string.UseDeletedIcon))
                    .setChecked(NaConfig.INSTANCE.getUseDeletedIcon().Bool()));

            items.add(UItem.asButton(ID_VIEW_DELETED, R.drawable.msg_delete_solar,
                    LocaleController.getString(R.string.ViewDeleted)));
        }

        items.add(SettingsHelper.asSwitchCG(ID_KEEP_ONCE,
                        loc("Сохранять исчезающие", "Save ephemeral media"),
                        loc("Фото, видео и кружки для однократного просмотра не удаляются", "View-once photos, videos and voice messages won't be deleted"))
                .setChecked(AyuGhostPreferences.isKeepOnceConfigured()));

        items.add(SettingsHelper.asSwitchCG(ID_SAVE_EDITS,
                        loc("Сохранять историю изменений", "Save edit history"),
                        loc("Сохранять все версии отредактированных сообщений", "Save all versions of edited messages"))
                .setChecked(NaConfig.INSTANCE.getEnableSaveEditsHistory().Bool()));

        items.add(UItem.asButton(ID_CLEAR_DATABASE, R.drawable.msg_clear_solar,
                LocaleController.getString(R.string.ClearMessageDatabase)).red());

        items.add(UItem.asShadow(null));

        // Section: Tweaks / Misc / TOS
        items.add(UItem.asHeader(loc("Обход ограничений (TOS)", "Restrictions bypass (TOS)")));

        items.add(SettingsHelper.asSwitchCG(ID_ALLOW_SCREENSHOTS,
                        loc("Разрешить скриншоты", "Allow screenshots"),
                        loc("Снимать скриншоты в защищённых чатах", "Take screenshots in protected chats"))
                .setChecked(AyuGhostPreferences.isAllowScreenshotsConfigured()));

        items.add(SettingsHelper.asSwitchCG(ID_NO_SCREENSHOT_NOTIFY,
                        loc("Скрывать свои скриншоты", "Hide my screenshots"),
                        loc("Собеседник не узнает о скриншоте в секретном чате", "The peer won't know about a screenshot in a secret chat"))
                .setChecked(AyuGhostPreferences.isNoScreenshotNotifyConfigured()));

        items.add(SettingsHelper.asSwitchCG(ID_BYPASS_FORWARD,
                        loc("Пересылка отовсюду", "Forward from anywhere"),
                        loc("Пересылать из чатов с запретом на пересылку", "Forward from chats with forwarding restrictions"))
                .setChecked(AyuGhostPreferences.isBypassForwardConfigured()));

        items.add(SettingsHelper.asSwitchCG(ID_ALLOW_COPY,
                        loc("Разрешить копирование", "Allow copying"),
                        loc("Копировать и выделять текст в чатах с запретом на пересылку", "Copy and select text in chats with forwarding restrictions"))
                .setChecked(AyuGhostPreferences.isAllowCopyConfigured()));

        items.add(SettingsHelper.asSwitchCG(ID_BYPASS_SAVE,
                        loc("Разрешить сохранение", "Allow saving"),
                        loc("Сохранять и скачивать медиа в чатах с запретом на пересылку", "Save and download media in chats with forwarding restrictions"))
                .setChecked(AyuGhostPreferences.isBypassSaveConfigured()));

        items.add(SettingsHelper.asSwitchCG(ID_BYPASS_STORY,
                        loc("Сохранение сторис", "Save restricted stories"),
                        loc("Сохранять сторис из чатов с запретом", "Save stories from chats with forwarding restrictions"))
                .setChecked(AyuGhostPreferences.isBypassStoryConfigured()));

        items.add(SettingsHelper.asSwitchCG(ID_BLOCK_MEDIA_SEEN,
                        loc("Не помечать медиа просмотренным", "Don't mark media as seen"),
                        loc("Голосовые, видео и кружки не отмечаются прослушанными", "Voice, video and round messages stay unmarked"))
                .setChecked(AyuGhostPreferences.isBlockMediaSeenConfigured()));

        items.add(SettingsHelper.asSwitchCG(ID_HIDE_RECOMMENDED,
                        loc("Скрыть похожие каналы", "Hide similar channels"),
                        loc("Убирает блоки рекомендованных каналов в чатах и профиле", "Removes recommended channel blocks in chats and profile"))
                .setChecked(AyuGhostPreferences.isHideRecommendedConfigured()));

        items.add(UItem.asShadow(null));

        // Section: Per-Chat Scoping
        items.add(UItem.asHeader(loc("По чатам", "Per-chat")));

        items.add(SettingsHelper.asSwitchCG(ID_PERCHAT_ENABLED,
                        loc("Действовать по списку чатов", "Scope by chat list"),
                        loc("Применять блокировки чтения, печати, историй и просмотров только к выбранным чатам", "Apply read, typing, story and view blocking only to selected chats"))
                .setChecked(AyuGhostPreferences.getPerchatEnabled()));

        if (AyuGhostPreferences.getPerchatEnabled()) {
            int pcMode = AyuGhostPreferences.getPcMode();
            String pcModeSubtitle = pcMode == 0
                    ? loc("Белый список (только выбранные)", "Whitelist (only selected)")
                    : loc("Чёрный список (все, кроме выбранных)", "Blacklist (all except selected)");
            items.add(UItem.asButton(ID_PC_MODE, loc("Режим списка", "List mode"), pcModeSubtitle));

            int count = AyuGhostPreferences.getPerChatList().size();
            String countText = count + " " + loc("чатов", "chats");
            items.add(UItem.asButton(ID_PC_MANAGE, loc("Управление списком чатов", "Manage chat list"), countText));
        }

        items.add(UItem.asShadow(null));
    }

    @Override
    public void onClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;
        if (id == ID_GHOST_MASTER) {
            AyuGhostPreferences.toggleGhostMode();
            AyuState.setAllowReadPacket(false, -1);
            NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_INSTANT_OFFLINE) {
            AyuGhostPreferences.setInstantOffline(!AyuGhostPreferences.isInstantOfflineConfigured());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_BLOCK_TYPING) {
            AyuGhostPreferences.setBlockTyping(!AyuGhostPreferences.isBlockTypingConfigured());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_BLOCK_READ) {
            AyuGhostPreferences.setBlockRead(!AyuGhostPreferences.isBlockReadConfigured());
            AyuState.setAllowReadPacket(false, -1);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_BLOCK_STORIES) {
            AyuGhostPreferences.setBlockStories(!AyuGhostPreferences.isBlockStoriesConfigured());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_BLOCK_CHANNEL_VIEWS) {
            AyuGhostPreferences.setBlockChannelViews(!AyuGhostPreferences.isBlockChannelViewsConfigured());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_MUTE_SEND) {
            showMuteSendDialog();
        } else if (id == ID_SAVE_DELETED) {
            NaConfig.INSTANCE.getEnableSaveDeletedMessages().toggleConfigBool();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_SAVE_MEDIA) {
            boolean clickedSwitch = view != null && x > view.getMeasuredWidth() - AndroidUtilities.dp(72);
            if (clickedSwitch) {
                NaConfig.INSTANCE.getMessageSavingSaveMedia().toggleConfigBool();
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            } else {
                if (!NaConfig.INSTANCE.getMessageSavingSaveMedia().Bool()) {
                    NaConfig.INSTANCE.getMessageSavingSaveMedia().setConfigBool(true);
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                }
                showSaveMediaDialog();
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
        } else if (id == ID_VIEW_DELETED) {
            presentFragment(new com.radolyn.ayugram.ui.AyuViewDeleted(0));
        } else if (id == ID_KEEP_ONCE) {
            AyuGhostPreferences.setKeepOnce(!AyuGhostPreferences.isKeepOnceConfigured());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_SAVE_EDITS) {
            NaConfig.INSTANCE.getEnableSaveEditsHistory().toggleConfigBool();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
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
        } else if (id == ID_BLOCK_MEDIA_SEEN) {
            AyuGhostPreferences.setBlockMediaSeen(!AyuGhostPreferences.isBlockMediaSeenConfigured());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_ALLOW_SCREENSHOTS) {
            AyuGhostPreferences.setAllowScreenshots(!AyuGhostPreferences.isAllowScreenshotsConfigured());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_NO_SCREENSHOT_NOTIFY) {
            AyuGhostPreferences.setNoScreenshotNotify(!AyuGhostPreferences.isNoScreenshotNotifyConfigured());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_BYPASS_FORWARD) {
            AyuGhostPreferences.setBypassForward(!AyuGhostPreferences.isBypassForwardConfigured());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_ALLOW_COPY) {
            AyuGhostPreferences.setAllowCopy(!AyuGhostPreferences.isAllowCopyConfigured());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_BYPASS_SAVE) {
            AyuGhostPreferences.setBypassSave(!AyuGhostPreferences.isBypassSaveConfigured());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_BYPASS_STORY) {
            AyuGhostPreferences.setBypassStory(!AyuGhostPreferences.isBypassStoryConfigured());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_HIDE_RECOMMENDED) {
            AyuGhostPreferences.setHideRecommended(!AyuGhostPreferences.isHideRecommendedConfigured());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_PERCHAT_ENABLED) {
            AyuGhostPreferences.setPerchatEnabled(!AyuGhostPreferences.getPerchatEnabled());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (id == ID_PC_MODE) {
            showPcModeDialog();
        } else if (id == ID_PC_MANAGE) {
            showManageChatsDialog();
        }
    }

    private void showMuteSendDialog() {
        if (getParentActivity() == null) return;
        CharSequence[] options = new CharSequence[] {
            loc("Выключено", "Disabled"),
            loc("Всегда", "Always"),
            loc("Только в призрачном режиме", "Ghost mode only")
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(loc("Беззвучная отправка", "Silent sending"));
        builder.setItems(options, (dialog, which) -> {
            AyuGhostPreferences.setMuteSend(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showPcModeDialog() {
        if (getParentActivity() == null) return;
        CharSequence[] options = new CharSequence[] {
            loc("Белый список (только выбранные)", "Whitelist (only selected)"),
            loc("Чёрный список (все, кроме выбранных)", "Blacklist (all except selected)")
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(loc("Режим списка", "List mode"));
        builder.setItems(options, (dialog, which) -> {
            AyuGhostPreferences.setPcMode(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showManageChatsDialog() {
        if (getParentActivity() == null) return;
        Set<Long> set = AyuGhostPreferences.getPerChatList();
        ArrayList<Long> list = new ArrayList<>(set);
        ArrayList<CharSequence> names = new ArrayList<>();
        names.add("+ " + loc("Добавить чат", "Add chat"));
        for (Long did : list) {
            String title;
            if (did > 0) {
                TLRPC.User user = getMessagesController().getUser(did);
                title = user != null ? UserObject.getUserName(user) : ("ID " + did);
            } else {
                TLRPC.Chat chat = getMessagesController().getChat(-did);
                title = chat != null ? chat.title : ("ID " + did);
            }
            names.add(title);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(loc("Чаты в списке", "Chats in list"));
        builder.setItems(names.toArray(new CharSequence[0]), (dialog, which) -> {
            if (which == 0) {
                openChatPicker();
            } else {
                long did = list.get(which - 1);
                showChatRemoveConfirmDialog(did);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Close), null);
        showDialog(builder.create());
    }

    private void showChatRemoveConfirmDialog(long did) {
        String title;
        if (did > 0) {
            TLRPC.User user = getMessagesController().getUser(did);
            title = user != null ? UserObject.getUserName(user) : ("ID " + did);
        } else {
            TLRPC.Chat chat = getMessagesController().getChat(-did);
            title = chat != null ? chat.title : ("ID " + did);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(loc("Удалить из списка?", "Remove from list?"));
        builder.setMessage(title);
        builder.setPositiveButton(LocaleController.getString(R.string.Delete), (d, w) -> {
            AyuGhostPreferences.removeChatFromPerChatList(did);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void openChatPicker() {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putBoolean("checkCanWrite", false);
        args.putBoolean("resetDelegate", false);
        args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_FORWARD);
        DialogsActivity fragment = new DialogsActivity(args);
        fragment.setDelegate((picker, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
            if (dids != null) {
                for (org.telegram.messenger.MessagesStorage.TopicKey k : dids) {
                    if (k != null) {
                        AyuGhostPreferences.addChatToPerChatList(k.dialogId);
                    }
                }
            }
            picker.finishFragment();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            return true;
        });
        presentFragment(fragment);
    }

    private void showSaveMediaDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString(R.string.MessageSavingSaveMedia));

        LinearLayout linearLayout = new LinearLayout(getParentActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        String[] items = new String[] {
            LocaleController.getString(R.string.MessageSavingSaveMediaInPrivateChats),
            LocaleController.getString(R.string.MessageSavingSaveMediaInPublicChannels),
            LocaleController.getString(R.string.MessageSavingSaveMediaInPrivateChannels),
            LocaleController.getString(R.string.MessageSavingSaveMediaInPublicGroups),
            LocaleController.getString(R.string.MessageSavingSaveMediaInPrivateGroups)
        };

        boolean[] checks = new boolean[] {
            NaConfig.INSTANCE.getSaveMediaInPrivateChats().Bool(),
            NaConfig.INSTANCE.getSaveMediaInPublicChannels().Bool(),
            NaConfig.INSTANCE.getSaveMediaInPrivateChannels().Bool(),
            NaConfig.INSTANCE.getSaveMediaInPublicGroups().Bool(),
            NaConfig.INSTANCE.getSaveMediaInPrivateGroups().Bool()
        };

        for (int i = 0; i < items.length; i++) {
            final int index = i;
            CheckBoxCell cell = new CheckBoxCell(getParentActivity(), 1);
            cell.setBackground(Theme.getSelectorDrawable(false));
            cell.setText(items[i], "", checks[i], false);
            cell.setOnClickListener(v -> {
                checks[index] = !checks[index];
                cell.setChecked(checks[index], true);
                switch (index) {
                    case 0:
                        NaConfig.INSTANCE.getSaveMediaInPrivateChats().setConfigBool(checks[index]);
                        break;
                    case 1:
                        NaConfig.INSTANCE.getSaveMediaInPublicChannels().setConfigBool(checks[index]);
                        break;
                    case 2:
                        NaConfig.INSTANCE.getSaveMediaInPrivateChannels().setConfigBool(checks[index]);
                        break;
                    case 3:
                        NaConfig.INSTANCE.getSaveMediaInPublicGroups().setConfigBool(checks[index]);
                        break;
                    case 4:
                        NaConfig.INSTANCE.getSaveMediaInPrivateGroups().setConfigBool(checks[index]);
                        break;
                }
            });
            linearLayout.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        builder.setView(linearLayout);
        builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
        showDialog(builder.create());
    }
}
