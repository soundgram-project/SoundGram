
        package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.graphics.Outline;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

public class SoundGramSettingsActivity extends BaseFragment {

    private UniversalAdapter listAdapter;

    @Override
    public View createView(Context context) {
        // --- НАСТРОЙКА ACTION BAR (Жидкий градиент) ---
        boolean isDark = Theme.isCurrentThemeDark();
        int[] gradientColors = isDark ? new int[]{0xFF0F2027, 0xFF203A43, 0xFF2C5364} : new int[]{0xFF8E2DE2, 0xFF4A00E0};

        GradientDrawable abGradient = new GradientDrawable(GradientDrawable.Orientation.BL_TR, gradientColors);
        actionBar.setBackground(abGradient);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("SoundGram");
        actionBar.setTitleColor(0xFFFFFFFF);
        actionBar.setItemsColor(0xFFFFFFFF, false);
        actionBar.setItemsBackgroundColor(0x33FFFFFF, false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });

        // --- ГЛАВНЫЙ КОНТЕЙНЕР ---
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        // --- ПРЕМИУМ ШАПКА ---
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, AndroidUtilities.dp(30), 0, AndroidUtilities.dp(40));

        GradientDrawable headerGradient = new GradientDrawable(GradientDrawable.Orientation.BL_TR, gradientColors);
        headerGradient.setCornerRadii(new float[]{0,0, 0,0, AndroidUtilities.dp(30),AndroidUtilities.dp(30), AndroidUtilities.dp(30),AndroidUtilities.dp(30)});
        header.setBackground(headerGradient);

        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.soundgram_logo);
        // Исправлено отображение иконки: убрали setScale, используем CENTER_CROP и ViewOutlineProvider для скругления
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logo.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AndroidUtilities.dp(16)); // Радиус 16dp
            }
        });
        logo.setClipToOutline(true);
        // Увеличили размер контейнера до 100x100 dp, чтобы иконка не обрезалась
        header.addView(logo, LayoutHelper.createLinear(100, 100, 0, 0, 0, 16));

        TextView title = new TextView(context);
        title.setText("SoundGram");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(22);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        header.addView(title, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));


        TextView subtitle = new TextView(context);
        subtitle.setText("Добро пожаловать в будущее общения");
        subtitle.setTextColor(0xD0FFFFFF);
        subtitle.setTextSize(14);
        header.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));

        mainLayout.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // --- СПИСОК МЕНЮ ---
        UniversalRecyclerView listView = new UniversalRecyclerView(this, (items, adapter) -> {
            listAdapter = adapter;

            items.add(UItem.asHeader("Разделы мода"));
            items.add(UItem.asButton(1, R.drawable.msg_secret, "Приватность"));
            items.add(UItem.asShadow("Управление нечиталкой, скрытием онлайна и ключами шифрования."));

            items.add(UItem.asButton(2, R.drawable.msg_theme, "Внешний вид и Анимации"));
            items.add(UItem.asShadow("Настройка видео-обоев и интерфейса SoundGram."));

            // Добавлен раздел "Сообщество"
            items.add(UItem.asHeader("Сообщество"));
            items.add(UItem.asButton(4, R.drawable.msg_channel, "Канал SoundGram"));
            items.add(UItem.asButton(5, R.drawable.msg_groups, "Чат SoundGram"));
            items.add(UItem.asButton(6, R.drawable.mini_reply_user, "Разработчик"));

            // Версия сдвинута в самый низ
            items.add(UItem.asHeader("О моде"));
            items.add(UItem.asButton(3, R.drawable.msg_info, "3.0(music update)"));


        }, (item, view, position, x, y) -> {
            if (item.id == 1) {
                presentFragment(new SoundGramMainActivity());
            } else if (item.id == 2) {
                presentFragment(new SoundGramAppearanceActivity());
            } else if (item.id == 3) {
                Toast.makeText(context, "SoundGram Mod by LastSunsh1ne", Toast.LENGTH_SHORT).show();
            } else if (item.id == 4) {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/soundgram_project")));
            } else if (item.id == 5) {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/soundgram_chat")));
            } else if (item.id == 6) {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/vlsv_srg")));
            }
        }, null);

        mainLayout.addView(listView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return fragmentView = mainLayout;
    }

}