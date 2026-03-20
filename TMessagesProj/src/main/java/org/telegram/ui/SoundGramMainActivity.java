package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.SoundGramConfig;
import org.telegram.messenger.SoundGramVault;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

public class SoundGramMainActivity extends BaseFragment {

    private UniversalAdapter listAdapter;

    @Override
    public View createView(Context context) {
        boolean isDark = Theme.isCurrentThemeDark();
        int[] gradientColors = isDark ? new int[]{0xFF0F2027, 0xFF203A43, 0xFF2C5364} : new int[]{0xFF8E2DE2, 0xFF4A00E0};

        actionBar.setBackground(new GradientDrawable(GradientDrawable.Orientation.BL_TR, gradientColors));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Приватность");
        actionBar.setTitleColor(0xFFFFFFFF);
        actionBar.setItemsColor(0xFFFFFFFF, false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });

        UniversalRecyclerView listView = new UniversalRecyclerView(this, (items, adapter) -> {
            listAdapter = adapter;

            items.add(UItem.asHeader("Режим Призрака"));
            items.add(UItem.asCheck(2, "Нечиталка сообщений").setChecked(SoundGramConfig.ghostModeRead));
            items.add(UItem.asCheck(3, "Скрыть «печатает»").setChecked(SoundGramConfig.ghostModeTyping));
            items.add(UItem.asCheck(4, "Мгновенный оффлайн").setChecked(SoundGramConfig.instantOffline));

            items.add(UItem.asHeader("Хранилище"));
            items.add(UItem.asCheck(10, "Сохранять удаленные").setChecked(SoundGramConfig.interceptDeleted));
            items.add(UItem.asCheck(11, "Сохранять исчезающие").setChecked(SoundGramConfig.keepOnce));
            items.add(UItem.asButton(14, R.drawable.msg_clear, "Очистить хранилище"));

            items.add(UItem.asHeader("Оформление"));
            items.add(UItem.asCheck(20, "Красная метка удаленных").setChecked(SoundGramConfig.delAccent));
            items.add(UItem.asCheck(21, "Сворачивать цитату").setChecked(SoundGramConfig.collapsedQuote));

            items.add(UItem.asHeader("Шифрование"));
            items.add(UItem.asCheck(30, "Включить Шифрование").setChecked(SoundGramConfig.zeroChatGloballyEnabled));
            items.add(UItem.asButton(31, R.drawable.msg_add, "Создать новый ключ"));

            if (!SoundGramConfig.zeroChatKeys.isEmpty()) {
                items.add(UItem.asHeader("Ваши ключи шифрования"));
                for (String keyName : SoundGramConfig.zeroChatKeys.keySet()) {
                    String pass = SoundGramConfig.zeroChatKeys.get(keyName);
                    items.add(UItem.asButton(keyName.hashCode(), R.drawable.menu_passkey_add, keyName));
                    items.add(UItem.asShadow("Пароль ключа: " + pass + "\nНажмите на ключ выше для удаления."));
                }
            } else {
                items.add(UItem.asShadow("У вас пока нет созданных ключей. Ключ нужен для шифровки и расшифровки сообщений в чатах."));
            }

        }, (item, view, position, x, y) -> {
            if (view instanceof TextCheckCell) {
                boolean check = !((TextCheckCell) view).isChecked();
                ((TextCheckCell) view).setChecked(check);

                if (item.id == 2) SoundGramConfig.ghostModeRead = check;
                else if (item.id == 3) SoundGramConfig.ghostModeTyping = check;
                else if (item.id == 4) {
                    org.telegram.messenger.SoundGramConfig.instantOffline = check;
                    org.telegram.tgnet.tl.TL_account.updateStatus req = new org.telegram.tgnet.tl.TL_account.updateStatus();
                    req.offline = check;
                    org.telegram.tgnet.ConnectionsManager.getInstance(org.telegram.messenger.UserConfig.selectedAccount).sendRequest(req, null);
                }

                else if (item.id == 10) SoundGramConfig.interceptDeleted = check;
                else if (item.id == 11) SoundGramConfig.keepOnce = check;
                else if (item.id == 20) SoundGramConfig.delAccent = check;
                else if (item.id == 21) SoundGramConfig.collapsedQuote = check;
                else if (item.id == 30) SoundGramConfig.zeroChatGloballyEnabled = check;

                SoundGramConfig.save();
                listAdapter.update(true);
            } else if (item.id == 14) {
                SoundGramVault.getInstance().wipe();
                Toast.makeText(context, "Хранилище очищено", Toast.LENGTH_SHORT).show();
            } else if (item.id == 31) {
                showAddKeyDialog();
            } else {
                for (String keyName : SoundGramConfig.zeroChatKeys.keySet()) {
                    if (item.id == keyName.hashCode()) {
                        showDeleteKeyDialog(keyName);
                        break;
                    }
                }
            }
        }, null);

        listView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        return fragmentView = listView;
    }

    private void showAddKeyDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Новый ключ Шифрования");
        LinearLayout l = new LinearLayout(getParentActivity());
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(10), AndroidUtilities.dp(24), 0);

        final EditText ed1 = new EditText(getParentActivity());
        ed1.setHint("Название");
        ed1.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        ed1.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        l.addView(ed1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        final EditText ed2 = new EditText(getParentActivity());
        ed2.setHint("Секретный пароль");
        ed2.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        ed2.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        l.addView(ed2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        b.setView(l);
        b.setPositiveButton("Создать", (d, w) -> {
            String n = ed1.getText().toString().trim();
            String p = ed2.getText().toString().trim();
            if (!n.isEmpty() && !p.isEmpty()) {
                SoundGramConfig.zeroChatKeys.put(n, p);
                SoundGramConfig.save();
                listAdapter.update(true);
            }
        });
        b.setNegativeButton("Отмена", null);
        showDialog(b.create());
    }

    private void showDeleteKeyDialog(String keyName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Удаление ключа");
        builder.setMessage("Удалить ключ '" + keyName + "'?");
        builder.setPositiveButton("Удалить", (dialog, which) -> {
            SoundGramConfig.zeroChatKeys.remove(keyName);
            SoundGramConfig.save();
            listAdapter.update(true);
        });
        builder.setNegativeButton("Отмена", null);
        showDialog(builder.create());
    }
}
