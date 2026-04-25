package org.telegram.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;

import java.util.ArrayList;

public class SoundGramStyleSettingsFragment extends BaseFragment {

    // ─── Константы ключей SharedPreferences ──────────────────────────────────
    public static final String PREFS_NAME          = "soundgram_style";
    public static final String KEY_GLASS_TRACKS    = "glass_tracks";
    public static final String KEY_GLASS_ALBUMS    = "glass_albums";
    public static final String KEY_GLASS_WAVE      = "glass_wave";
    public static final String KEY_GLASS_PLAYLISTS = "glass_playlists";
    public static final String KEY_GLASS_PLAYER    = "glass_player";
    public static final String KEY_GLASS_MINI      = "glass_mini";
    public static final String KEY_GLASS_ARTIST    = "glass_artist";
    public static final String KEY_GLASS_EQ        = "glass_eq";

    public static final String KEY_ALPHA_TRACKS    = "alpha_tracks";
    public static final String KEY_ALPHA_ALBUMS    = "alpha_albums";
    public static final String KEY_ALPHA_WAVE      = "alpha_wave";
    public static final String KEY_ALPHA_PLAYLISTS = "alpha_playlists";
    public static final String KEY_ALPHA_PLAYER    = "alpha_player";
    public static final String KEY_ALPHA_MINI      = "alpha_mini";
    public static final String KEY_ALPHA_ARTIST    = "alpha_artist";
    public static final String KEY_ALPHA_EQ        = "alpha_eq";

    /**
     * ID нотификации для мгновенного обновления мини-плеера и других компонентов.
     * Используй любой свободный ID из NotificationCenter (или добавь своё поле).
     */
    public static final int NOTIFICATION_STYLE_CHANGED = -700;

    // ─── Статика: читай настройки из любого места ─────────────────────────
    public static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isGlassEnabled(String key) {
        return prefs().getBoolean(key, true);
    }

    /** Возвращает alpha 0..255 для жидкого стекла данного элемента */
    public static int getGlassAlpha(String keyAlpha) {
        return prefs().getInt(keyAlpha, 180);
    }

    // ─── Данные: описания секций настроек ─────────────────────────────────
    private static final String[][] ITEMS = {
            // { заголовок, подзаголовок, ключ_switch, ключ_alpha }
            { "Треки",          "Библиотека, плейлисты, альбомы", KEY_GLASS_TRACKS,    KEY_ALPHA_TRACKS    },
            { "Альбомы",        "Карточки в сетке",               KEY_GLASS_ALBUMS,    KEY_ALPHA_ALBUMS    },
            { "Моя волна",      "Виджет и кнопки",                KEY_GLASS_WAVE,      KEY_ALPHA_WAVE      },
            { "Плейлисты",      "Горизонтальный список",          KEY_GLASS_PLAYLISTS, KEY_ALPHA_PLAYLISTS },
            { "Аудиоплеер",     "Полноэкранный плеер",            KEY_GLASS_PLAYER,    KEY_ALPHA_PLAYER    },
            { "Мини-плеер",     "Плавающая панель внизу",         KEY_GLASS_MINI,      KEY_ALPHA_MINI      },
            { "Профиль артиста","Шапка и треки артиста",           KEY_GLASS_ARTIST,    KEY_ALPHA_ARTIST    },
            { "Эквалайзер",     "AudioFX & Pitch",                KEY_GLASS_EQ,        KEY_ALPHA_EQ        },
    };

    private RecyclerListView recyclerView;
    private SettingsAdapter  settingsAdapter;

    // ─── Создание Fragment ────────────────────────────────────────────────────
    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        return true;
    }

    @Override
    public View createView(Context context) {
        // ── ActionBar ────────────────────────────────────────────────────────
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Внешний вид");
        actionBar.setOccupyStatusBar(true);
        actionBar.setBackgroundColor(
                androidx.core.graphics.ColorUtils.setAlphaComponent(
                        Theme.getColor(Theme.key_actionBarDefault), 245));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        // ── Корневой контейнер ────────────────────────────────────────────────
        SizeNotifierFrameLayout content = new SizeNotifierFrameLayout(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                canvas.drawColor(Theme.getColor(Theme.key_windowBackgroundGray));
            }
        };
        content.setWillNotDraw(false);
        fragmentView = content;

        // ── RecyclerView ─────────────────────────────────────────────────────
        recyclerView = new RecyclerListView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setVerticalScrollBarEnabled(false);
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(0, 0, 0, AndroidUtilities.dp(16));

        settingsAdapter = new SettingsAdapter(context);
        recyclerView.setAdapter(settingsAdapter);

        content.addView(recyclerView,
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        return fragmentView;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // АДАПТЕР
    // ═══════════════════════════════════════════════════════════════════════════

    private class SettingsAdapter extends RecyclerListView.SelectionAdapter {

        // ViewType
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM   = 1;

        private final Context ctx;

        SettingsAdapter(Context context) {
            this.ctx = context;
        }

        @Override public boolean isEnabled(RecyclerView.ViewHolder h) {
            return h.getItemViewType() != TYPE_HEADER;
        }

        @Override public int getItemCount() {
            return 1 + ITEMS.length; // header + items
        }

        @Override public int getItemViewType(int position) {
            return position == 0 ? TYPE_HEADER : TYPE_ITEM;
        }

        // ── onCreateViewHolder ────────────────────────────────────────────────
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                return new RecyclerListView.Holder(buildStatsHeader(ctx));
            }
            return new RecyclerListView.Holder(new GlassSettingCell(ctx));
        }

        // ── onBindViewHolder ──────────────────────────────────────────────────
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == TYPE_HEADER) return;

            String[] item    = ITEMS[position - 1];
            String title     = item[0];
            String subtitle  = item[1];
            String keySwitch = item[2];
            String keyAlpha  = item[3];

            ((GlassSettingCell) holder.itemView)
                    .bind(title, subtitle, keySwitch, keyAlpha);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HEADER — блок статистики
    // ═══════════════════════════════════════════════════════════════════════════

    private View buildStatsHeader(Context context) {
        int accent = Theme.getColor(Theme.key_featuredStickers_addButton);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(
                AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        root.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // ── Заголовок «Статистика» ────────────────────────────────────────────
        TextView sectionTitle = new TextView(context);
        sectionTitle.setText("СТАТИСТИКА");
        sectionTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        sectionTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        sectionTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        sectionTitle.setLetterSpacing(0.08f);
        root.addView(sectionTitle, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                0, 0, 0, 12));

        // ── Стеклянная карточка статистики ───────────────────────────────────
        FrameLayout statsCard = new FrameLayout(context);
        GradientDrawable statsCardBg = new GradientDrawable();
        statsCardBg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(
                Theme.getColor(Theme.key_windowBackgroundWhite), 255));
        statsCardBg.setCornerRadius(AndroidUtilities.dp(20));
        if (Build.VERSION.SDK_INT >= 21) statsCard.setElevation(AndroidUtilities.dp(3));
        statsCard.setBackground(statsCardBg);

        LinearLayout statsRow = new LinearLayout(context);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setGravity(Gravity.CENTER);
        statsRow.setPadding(
                AndroidUtilities.dp(12), AndroidUtilities.dp(16),
                AndroidUtilities.dp(12), AndroidUtilities.dp(16));

        SharedPreferences statPrefs = ApplicationLoader.applicationContext
                .getSharedPreferences("soundgram_stats", Context.MODE_PRIVATE);
        int  totalTracks  = statPrefs.getInt("total_tracks", 0);
        long totalSeconds = statPrefs.getLong("total_seconds", 0L);
        long hours        = totalSeconds / 3600;
        long minutes      = (totalSeconds % 3600) / 60;

        statsRow.addView(makeStatPill(context, "🎵", String.valueOf(totalTracks), "треков", accent),
                LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, 8, 0));
        statsRow.addView(makeStatPill(context, "⏱", hours + "ч " + minutes + "м", "слушал", accent),
                LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 8, 0, 0, 0));

        statsCard.addView(statsRow, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        root.addView(statsCard, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                0, 0, 0, 20));

        // ── Заголовок секции настроек ─────────────────────────────────────────
        TextView settingsLabel = new TextView(context);
        settingsLabel.setText("СТИЛЬ ЖИДКОГО СТЕКЛА");
        settingsLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        settingsLabel.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        settingsLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        settingsLabel.setLetterSpacing(0.08f);
        root.addView(settingsLabel, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        return root;
    }

    private View makeStatPill(Context context, String emoji, String value, String label, int accent) {
        LinearLayout pill = new LinearLayout(context);
        pill.setOrientation(LinearLayout.VERTICAL);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(
                AndroidUtilities.dp(8), AndroidUtilities.dp(14),
                AndroidUtilities.dp(8), AndroidUtilities.dp(14));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 18));
        bg.setCornerRadius(AndroidUtilities.dp(16));
        bg.setStroke(AndroidUtilities.dp(1),
                androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 60));
        pill.setBackground(bg);

        TextView emojiTv = new TextView(context);
        emojiTv.setText(emoji);
        emojiTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 26);
        emojiTv.setGravity(Gravity.CENTER);
        pill.addView(emojiTv, LayoutHelper.createLinear(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 4));

        TextView valueTv = new TextView(context);
        valueTv.setText(value);
        valueTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        valueTv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        valueTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        valueTv.setGravity(Gravity.CENTER);
        pill.addView(valueTv, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 2));

        TextView labelTv = new TextView(context);
        labelTv.setText(label);
        labelTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        labelTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        labelTv.setGravity(Gravity.CENTER);
        pill.addView(labelTv, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        return pill;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GlassSettingCell — одна строка настройки
    //
    //  ┌──────────────────────────────────────────────────────────┐
    //  │  [иконка]  Заголовок                        [Switch]    │
    //  │            Подзаголовок                                  │
    //  ├──────────────────────────────────────────────────────────┤  ← только если switch ON
    //  │  Прозрачность  ══════════════●═══════════               │
    //  └──────────────────────────────────────────────────────────┘
    //
    //  previewBox — это LinearLayout(VERTICAL), поэтому row и sliderRow
    //  располагаются строго друг под другом, без наложений.
    // ═══════════════════════════════════════════════════════════════════════════

    private class GlassSettingCell extends FrameLayout {

        private TextView     titleTv;
        private TextView     subtitleTv;
        private Switch       toggle;
        private SeekBar      alphaSlider;
        private LinearLayout sliderRow;
        private LinearLayout previewBox;   // ← ИСПРАВЛЕНО: LinearLayout вместо FrameLayout

        private String  keySwitch;
        private String  keyAlpha;
        private int     currentAlpha;
        private boolean glassEnabled;

        public GlassSettingCell(Context context) {
            super(context);
            setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            build(context);
        }

        private void build(Context context) {
            int accent = Theme.getColor(Theme.key_featuredStickers_addButton);

            // ── Внешний отступ (горизонтальные поля + вертикальный gap) ───────
            // Сам GlassSettingCell — FrameLayout только для установки LayoutParams;
            // реальное содержимое живёт в previewBox внутри горизонтального padding.
            setPadding(
                    AndroidUtilities.dp(16), AndroidUtilities.dp(6),
                    AndroidUtilities.dp(16), AndroidUtilities.dp(6));

            // ── Стеклянная карточка-превью (VERTICAL LinearLayout) ─────────────
            // ФОН карточки = и есть превью жидкого стекла; меняется в реальном времени.
            previewBox = new LinearLayout(context);
            previewBox.setOrientation(LinearLayout.VERTICAL);
            if (Build.VERSION.SDK_INT >= 21) previewBox.setElevation(AndroidUtilities.dp(2));

            // ── Строка: иконка + тексты + switch ──────────────────────────────
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(
                    AndroidUtilities.dp(14), AndroidUtilities.dp(14),
                    AndroidUtilities.dp(14), AndroidUtilities.dp(14));

            // Иконка-превью (маленький стеклянный квадрат)
            View iconPreview = new View(context);
            GradientDrawable iconBg = new GradientDrawable();
            iconBg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 50));
            iconBg.setCornerRadius(AndroidUtilities.dp(8));
            iconBg.setStroke(AndroidUtilities.dp(1),
                    androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 120));
            iconPreview.setBackground(iconBg);
            row.addView(iconPreview, LayoutHelper.createLinear(
                    36, 36, Gravity.CENTER_VERTICAL, 0, 0, 14, 0));

            // Тексты
            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);

            titleTv = new TextView(context);
            titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            titleTv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            titleTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            textCol.addView(titleTv, LayoutHelper.createLinear(
                    LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            subtitleTv = new TextView(context);
            subtitleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            subtitleTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            textCol.addView(subtitleTv, LayoutHelper.createLinear(
                    LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            row.addView(textCol, LayoutHelper.createLinear(
                    0, LayoutHelper.WRAP_CONTENT, 1.0f));

            // Switch
            toggle = new Switch(context);
            toggle.setThumbTintList(
                    android.content.res.ColorStateList.valueOf(accent));
            toggle.setTrackTintList(
                    new android.content.res.ColorStateList(
                            new int[][]{
                                    new int[]{-android.R.attr.state_checked},
                                    new int[]{ android.R.attr.state_checked}
                            },
                            new int[]{
                                    androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 80),
                                    androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 180)
                            }));
            row.addView(toggle, LayoutHelper.createLinear(
                    LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL));

            // Добавляем row в previewBox (первый «слой» — верхний)
            previewBox.addView(row, LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            // ── Строка слайдера (второй «слой» — строго под row) ──────────────
            sliderRow = new LinearLayout(context);
            sliderRow.setOrientation(LinearLayout.HORIZONTAL);
            sliderRow.setGravity(Gravity.CENTER_VERTICAL);
            sliderRow.setPadding(
                    AndroidUtilities.dp(14), 0,
                    AndroidUtilities.dp(14), AndroidUtilities.dp(14));

            TextView alphaLabel = new TextView(context);
            alphaLabel.setText("Прозрачность");
            alphaLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            alphaLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            alphaLabel.setMinWidth(AndroidUtilities.dp(100));
            sliderRow.addView(alphaLabel, LayoutHelper.createLinear(
                    LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                    0, 0, 10, 0));

            alphaSlider = new SeekBar(context);
            alphaSlider.setMax(255);
            alphaSlider.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(accent));
            alphaSlider.setThumbTintList(
                    android.content.res.ColorStateList.valueOf(accent));
            sliderRow.addView(alphaSlider, LayoutHelper.createLinear(
                    0, LayoutHelper.WRAP_CONTENT, 1.0f));

            // Добавляем sliderRow в previewBox (второй «слой» — под row)
            previewBox.addView(sliderRow, LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            // ── Добавляем previewBox в саму ячейку (GlassSettingCell) ──────────
            addView(previewBox, LayoutHelper.createFrame(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            // ── Слушатели ─────────────────────────────────────────────────────
            toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                glassEnabled = isChecked;
                prefs().edit().putBoolean(keySwitch, isChecked).apply();
                updatePreviewBackground();
                animateSliderVisibility(isChecked);
                // Мгновенно уведомляем все компоненты (в т.ч. мини-плеер)
                notifyStyleChanged();
            });

            alphaSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onStartTrackingTouch(SeekBar sb) {}
                @Override public void onStopTrackingTouch(SeekBar sb) {
                    // Уведомляем при отпускании ползунка
                    notifyStyleChanged();
                }
                @Override
                public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                    if (!fromUser) return;
                    currentAlpha = progress;
                    prefs().edit().putInt(keyAlpha, progress).apply();
                    // Live-обновление превью карточки
                    updatePreviewBackground();
                }
            });
        }

        public void bind(String title, String subtitle, String keySwitch, String keyAlpha) {
            this.keySwitch    = keySwitch;
            this.keyAlpha     = keyAlpha;
            this.glassEnabled = isGlassEnabled(keySwitch);
            this.currentAlpha = getGlassAlpha(keyAlpha);

            titleTv.setText(title);
            subtitleTv.setText(subtitle);

            // Снимаем слушатель перед setChecked, чтобы не триггерить сохранение
            toggle.setOnCheckedChangeListener(null);
            toggle.setChecked(glassEnabled);
            toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                glassEnabled = isChecked;
                prefs().edit().putBoolean(keySwitch, isChecked).apply();
                updatePreviewBackground();
                animateSliderVisibility(isChecked);
                notifyStyleChanged();
            });

            alphaSlider.setProgress(currentAlpha);
            sliderRow.setVisibility(glassEnabled ? View.VISIBLE : View.GONE);
            sliderRow.setAlpha(glassEnabled ? 1f : 0f);
            updatePreviewBackground();
        }

        /**
         * Обновляет фон previewBox (= карточки ячейки) в зависимости от текущих настроек.
         * Именно так пользователь видит превью «как будет выглядеть» элемент.
         */
        private void updatePreviewBackground() {
            int accent  = Theme.getColor(Theme.key_featuredStickers_addButton);
            int bgColor = Theme.getColor(Theme.key_windowBackgroundWhite);
            int radius  = AndroidUtilities.dp(18);

            GradientDrawable bg = new GradientDrawable();
            if (glassEnabled) {
                bg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(bgColor, currentAlpha));
                bg.setCornerRadius(radius);
                bg.setStroke(AndroidUtilities.dp(1),
                        androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 90));
            } else {
                bg.setColor(bgColor);
                bg.setCornerRadius(radius);
                bg.setStroke(0, Color.TRANSPARENT);
            }
            previewBox.setBackground(bg);
            previewBox.invalidate();
        }

        /** Плавно показывает / скрывает SeekBar */
        private void animateSliderVisibility(boolean show) {
            if (show) {
                sliderRow.setVisibility(View.VISIBLE);
                sliderRow.setAlpha(0f);
                sliderRow.animate().alpha(1f).setDuration(200).start();
            } else {
                sliderRow.animate().alpha(0f).setDuration(160)
                        .withEndAction(() -> sliderRow.setVisibility(View.GONE))
                        .start();
            }
        }
    }

    // ─── Нотификация изменений стиля для мгновенного применения ──────────────
    /**
     * Отправляет широковещательное уведомление через NotificationCenter.
     * Мини-плеер и другие компоненты должны подписаться на NOTIFICATION_STYLE_CHANGED
     * и вызывать applyGlassStyle() при получении события.
     *
     * Пример подписки в MiniPlayer:
     *   NotificationCenter.getInstance(currentAccount)
     *       .addObserver(this, SoundGramStyleSettingsFragment.NOTIFICATION_STYLE_CHANGED);
     *
     *   @Override public void didReceivedNotification(int id, int account, Object... args) {
     *       if (id == SoundGramStyleSettingsFragment.NOTIFICATION_STYLE_CHANGED) {
     *           applyGlassStyle();
     *       }
     *   }
     */
    private void notifyStyleChanged() {
        for (int i = 0; i < 4; i++) {
            NotificationCenter.getInstance(i)
                    .postNotificationName(NOTIFICATION_STYLE_CHANGED);
        }
    }
}
