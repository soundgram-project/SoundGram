
        package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.messenger.SoundGramConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

public class SoundGramAppearanceActivity extends BaseFragment {

    private UniversalAdapter listAdapter;
    private static final int PICK_CHAT_LIST_WALLPAPER = 101;
    private static final int PICK_CHAT_WALLPAPER = 102;

    @Override
    public View createView(Context context) {
        boolean isDark = Theme.isCurrentThemeDark();
        int[] gradientColors = isDark ? new int[]{0xFF0F2027, 0xFF203A43, 0xFF2C5364} : new int[]{0xFF8E2DE2, 0xFF4A00E0};

        actionBar.setBackground(new GradientDrawable(GradientDrawable.Orientation.BL_TR, gradientColors));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Кастомизация");
        actionBar.setTitleColor(0xFFFFFFFF);
        actionBar.setItemsColor(0xFFFFFFFF, false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });

        UniversalRecyclerView listView = new UniversalRecyclerView(this, (items, adapter) -> {
            listAdapter = adapter;

            // СЕКЦИЯ 1: ОБОИ В СПИСКЕ ЧАТОВ
            items.add(UItem.asHeader("Обои в списке чатов"));
            items.add(UItem.asButton(10, R.drawable.msg_video, SoundGramConfig.chatListWallPath.isEmpty() ? "Выбрать фон для списка" : "Изменить фон списка"));
            if (!SoundGramConfig.chatListWallPath.isEmpty()) {
                items.add(UItem.asButton(11, R.drawable.msg_delete, "Удалить фон списка"));
            }
            items.add(UItem.asShadow("Отображаются на главном экране за списком всех переписок (требуется перезагрузка)."));

            // СЕКЦИЯ 2: ОБОИ ВНУТРИ ЧАТОВ
            items.add(UItem.asHeader("Обои внутри чатов"));
            items.add(UItem.asButton(20, R.drawable.msg_video, SoundGramConfig.chatVideoPath.isEmpty() ? "Выбрать фон для чатов" : "Изменить фон чатов"));
            if (!SoundGramConfig.chatVideoPath.isEmpty()) {
                items.add(UItem.asButton(21, R.drawable.msg_delete, "Удалить обои чатов"));
            }
            items.add(UItem.asShadow("Фон, который будет виден во время диалога."));

            // СЕКЦИЯ 3: ИНТЕРФЕЙС
            items.add(UItem.asHeader("Интерфейс"));
            items.add(UItem.asCheck(3, "Локальный Premium").setChecked(SoundGramConfig.localPremium));
            items.add(UItem.asCheck(4, "Скрыть папку «Все»").setChecked(SoundGramConfig.hideAllFolder)); // ДОБАВИТЬ ЭТУ СТРОКУ

        }, (item, view, position, x, y) -> {
            if (item.id == 1) {
                SoundGramConfig.musicWallEnabled = !SoundGramConfig.musicWallEnabled;
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(SoundGramConfig.musicWallEnabled);
                }
                applyAndRestart();
            } else if (item.id == 10) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, 101);
            } else if (view instanceof TextCheckCell) {
                boolean check = !((TextCheckCell) view).isChecked();
                ((TextCheckCell) view).setChecked(check);
                if (item.id == 3) SoundGramConfig.localPremium = check;
                else if (item.id == 4) { // ДОБАВИТЬ ЭТОТ БЛОК
                    SoundGramConfig.hideAllFolder = check;
                    org.telegram.messenger.MessagesController.getInstance(org.telegram.messenger.UserConfig.selectedAccount).invalidateChatlistFolderUpdate(0);
                    applyAndRestart();
                }
                SoundGramConfig.save();
            } else if (item.id == 20) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("video/*");
                startActivityForResult(intent, 102);
            } else if (item.id == 11) {
                SoundGramConfig.chatListWallPath = "";
                applyAndRestart();
            } else if (item.id == 21) {
                SoundGramConfig.chatVideoPath = "";
                applyAndRestart();
            }
            else if (view instanceof TextCheckCell) {
                boolean check = !((TextCheckCell) view).isChecked();
                ((TextCheckCell) view).setChecked(check);
                if (item.id == 3) SoundGramConfig.localPremium = check;
                SoundGramConfig.save();
            }
        }, null);

        return fragmentView = listView;
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        super.onActivityResultFragment(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try { getParentActivity().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception e) {}
            if (requestCode == 101) SoundGramConfig.chatListWallPath = uri.toString();
            else if (requestCode == 102) SoundGramConfig.chatVideoPath = uri.toString();
            applyAndRestart();
        }
    }
    private void applyAndRestart() {
        SoundGramConfig.save();
        if (getParentActivity() != null) {
            android.widget.Toast.makeText(getParentActivity(), "Применение настроек...", android.widget.Toast.LENGTH_SHORT).show();
            AndroidUtilities.runOnUIThread(() -> {
                Intent intent = new Intent(getParentActivity(), LaunchActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                getParentActivity().startActivity(intent);
                Runtime.getRuntime().exit(0);
            }, 300);
        }
    }
}