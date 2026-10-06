package soundgram.ui;

// ═══════════════════════════════════════════════════════════════════════════════
// SoundGramStyleSettingsFragment.java — ПОЛНАЯ ПЕРЕРАБОТКА (Задача 1)
//
// Новая логика:
//  • Ползунок СПРАВА = полностью прозрачно (alpha=0)
//  • Ползунок СЛЕВА  = непрозрачная подложка (alpha=255)
//  • Тумблер Switch  = только убирает/показывает обводку
//  • Кнопка-пилюля   = левая половина показывает цвет подложки,
//                      правая половина показывает цвет обводки
//  • Нажатие на пилюлю = дропдаун "Для стекла / Для обводки",
//                        затем полноэкранный ColorPicker (как на скрине)
//  • Сброс   → берёт цвет из темы TG
//  • Готово  → применяет, мгновенно диспатчит StyleChanged
// ═══════════════════════════════════════════════════════════════════════════════

import static org.telegram.messenger.LocaleController.getString;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import soundgram.music.PublicLibraryManager;
import java.util.List;
import android.text.TextUtils;

public class SoundGramStyleSettingsFragment extends BaseFragment {

    // ── Prefs ──────────────────────────────────────────────────────────────────
    public static final String PREFS_NAME          = "soundgram_style";
    public static final String KEY_GLASS_TRACKS    = "glass_tracks";
    public static final String KEY_GLASS_ALBUMS    = "glass_albums";
    public static final String KEY_GLASS_WAVE      = "glass_wave";
    public static final String KEY_GLASS_PLAYLISTS = "glass_playlists";
    public static final String KEY_GLASS_PLAYER    = "glass_player";
    public static final String KEY_GLASS_MINI      = "glass_mini";
    public static final String KEY_GLASS_ARTIST    = "glass_artist";
    public static final String KEY_GLASS_EQ        = "glass_eq";

    // alpha = 0..255: 255=непрозрачно, 0=прозрачно
    public static final String KEY_ALPHA_TRACKS    = "alpha_tracks";
    public static final String KEY_ALPHA_ALBUMS    = "alpha_albums";
    public static final String KEY_ALPHA_WAVE      = "alpha_wave";
    public static final String KEY_ALPHA_PLAYLISTS = "alpha_playlists";
    public static final String KEY_ALPHA_PLAYER    = "alpha_player";
    public static final String KEY_ALPHA_MINI      = "alpha_mini";
    public static final String KEY_ALPHA_ARTIST    = "alpha_artist";
    public static final String KEY_ALPHA_EQ        = "alpha_eq";

    // кастомный цвет подложки (-1 = использовать тему TG)
    public static final String KEY_COLOR_BG_TRACKS    = "cbg_tracks";
    public static final String KEY_COLOR_BG_ALBUMS    = "cbg_albums";
    public static final String KEY_COLOR_BG_WAVE      = "cbg_wave";
    public static final String KEY_COLOR_BG_PLAYLISTS = "cbg_playlists";
    public static final String KEY_COLOR_BG_PLAYER    = "cbg_player";
    public static final String KEY_COLOR_BG_MINI      = "cbg_mini";
    public static final String KEY_COLOR_BG_ARTIST    = "cbg_artist";
    public static final String KEY_COLOR_BG_EQ        = "cbg_eq";

    // кастомный цвет обводки (-1 = использовать акцент TG)
    public static final String KEY_COLOR_STROKE_TRACKS    = "cs_tracks";
    public static final String KEY_COLOR_STROKE_ALBUMS    = "cs_albums";
    public static final String KEY_COLOR_STROKE_WAVE      = "cs_wave";
    public static final String KEY_COLOR_STROKE_PLAYLISTS = "cs_playlists";
    public static final String KEY_COLOR_STROKE_PLAYER    = "cs_player";
    public static final String KEY_COLOR_STROKE_MINI      = "cs_mini";
    public static final String KEY_COLOR_STROKE_ARTIST    = "cs_artist";
    public static final String KEY_COLOR_STROKE_EQ        = "cs_eq";

    public static final String KEY_WAVE_RAYS              = "wave_rays";
    public static final String KEY_WAVE_SPHERE_SIZE       = "wave_sphere_size";

    public static final int NOTIFICATION_STYLE_CHANGED = -700;

    // ── Статические хелперы ────────────────────────────────────────────────────
    public static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static float getWaveRaysMultiplier() {
        return prefs().getInt(KEY_WAVE_RAYS, 100) / 100f;
    }

    public static float getWaveSphereSizeMultiplier() {
        return prefs().getInt(KEY_WAVE_SPHERE_SIZE, 100) / 100f;
    }

    /** Stroke (обводка) включена? */
    public static boolean isStrokeEnabled(String keySwitch) {
        return prefs().getBoolean(keySwitch, false);
    }

    /** Glass (стекло/подложка) включено? Проверяет по булевому ключу (по умолч. true) */
    public static boolean isGlassEnabled(String keyGlass) {
        return prefs().getBoolean(keyGlass, true);
    }

    /** Alpha подложки 0..255 (255 = непрозрачно). Default = 180. */
    public static int getAlpha(String keyAlpha) {
        return prefs().getInt(keyAlpha, 180);
    }

    /** Цвет подложки: -1 = из темы TG */
    public static int getBgColor(String keyColor) {
        return prefs().getInt(keyColor, -1);
    }

    /** Цвет обводки: -1 = акцент темы */
    public static int getStrokeColor(String keyColor) {
        return prefs().getInt(keyColor, -1);
    }

    /** Реальный цвет подложки с учётом темы */
    public static int resolvedBgColor(String keyColor) {
        int saved = getBgColor(keyColor);
        return saved == -1 ? Theme.getColor(Theme.key_dialogBackground) : saved;
    }

    /** Реальный цвет обводки с учётом темы */
    public static int resolvedStrokeColor(String keyColor) {
        int saved = getStrokeColor(keyColor);
        return saved == -1 ? Theme.getColor(Theme.key_featuredStickers_addButton) : saved;
    }

    // ── Таблица строк: {title, subtitle, KEY_GLASS, KEY_ALPHA, KEY_COLOR_BG, KEY_COLOR_STROKE}
    // NOTE: titles/subtitles are loaded dynamically via getItemTitle/getItemSubtitle to support localization
    private static final String[][] ITEMS = {
            {null, null, KEY_GLASS_TRACKS,    KEY_ALPHA_TRACKS,    KEY_COLOR_BG_TRACKS,    KEY_COLOR_STROKE_TRACKS},
            {null, null, KEY_GLASS_ALBUMS,    KEY_ALPHA_ALBUMS,    KEY_COLOR_BG_ALBUMS,    KEY_COLOR_STROKE_ALBUMS},
            {null, null, KEY_GLASS_WAVE,      KEY_ALPHA_WAVE,      KEY_COLOR_BG_WAVE,      KEY_COLOR_STROKE_WAVE},
            {null, null, KEY_GLASS_PLAYLISTS, KEY_ALPHA_PLAYLISTS, KEY_COLOR_BG_PLAYLISTS, KEY_COLOR_STROKE_PLAYLISTS},
            {null, null, KEY_GLASS_PLAYER,    KEY_ALPHA_PLAYER,    KEY_COLOR_BG_PLAYER,    KEY_COLOR_STROKE_PLAYER},
            {null, null, KEY_GLASS_MINI,      KEY_ALPHA_MINI,      KEY_COLOR_BG_MINI,      KEY_COLOR_STROKE_MINI},
            {null, null, KEY_GLASS_ARTIST,    KEY_ALPHA_ARTIST,    KEY_COLOR_BG_ARTIST,    KEY_COLOR_STROKE_ARTIST},
            {null, null, KEY_GLASS_EQ,        KEY_ALPHA_EQ,        KEY_COLOR_BG_EQ,        KEY_COLOR_STROKE_EQ},
    };

    private String getItemTitle(int index) {
        int[] ids = {R.string.sg_style_tracks_title, R.string.sg_style_albums_title,
                R.string.sg_style_wave_title, R.string.sg_style_playlists_title,
                R.string.sg_style_player_title, R.string.sg_style_mini_title,
                R.string.sg_style_artist_title, R.string.sg_style_eq_title};
        return getString(ids[index]);
    }

    private String getItemSubtitle(int index) {
        int[] ids = {R.string.sg_style_tracks_subtitle, R.string.sg_style_albums_subtitle,
                R.string.sg_style_wave_subtitle, R.string.sg_style_playlists_subtitle,
                R.string.sg_style_player_subtitle, R.string.sg_style_mini_subtitle,
                R.string.sg_style_artist_subtitle, R.string.sg_style_eq_subtitle};
        return getString(ids[index]);
    }

    private RecyclerListView recyclerView;
    private SettingsAdapter  settingsAdapter;

    // ── Fragment lifecycle ─────────────────────────────────────────────────────
    @Override public boolean onFragmentCreate() { super.onFragmentCreate(); return true; }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(getString(R.string.sg_style_title));
        actionBar.setOccupyStatusBar(true);
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { 
                if (id == -1) {
                    finishFragment(); 
                } else if (id == 1) {
                    prefs().edit().clear().apply();
                    notifyStyleChanged();
                    android.widget.Toast.makeText(context, "Настройки сброшены", android.widget.Toast.LENGTH_SHORT).show();
                    presentFragment(new SoundGramStyleSettingsFragment(), true);
                }
            }
        });

        org.telegram.ui.ActionBar.ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(1, getString(R.string.sg_eq_reset));

        SizeNotifierFrameLayout content = new SizeNotifierFrameLayout(context) {
            @Override protected void onDraw(Canvas c) {
                c.drawColor(Theme.getColor(Theme.key_windowBackgroundGray));
            }
        };
        content.setWillNotDraw(false);
        fragmentView = content;

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

    @Override
    public void onResume() {
        super.onResume();
        if (settingsAdapter != null) {
            settingsAdapter.notifyItemChanged(0);
        }
    }

    private void notifyStyleChanged() {
        SoundGramStyleManager.dispatchStyleChanged();
        for (int i = 0; i < 4; i++)
            NotificationCenter.getInstance(i).postNotificationName(NOTIFICATION_STYLE_CHANGED);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ADAPTER
    // ══════════════════════════════════════════════════════════════════════════
    private static class HeaderHolder extends RecyclerListView.Holder {
        final TextView tracksValueTextView;
        final TextView timeValueTextView;
        HeaderHolder(View itemView, TextView tracksValue, TextView timeValue) {
            super(itemView);
            this.tracksValueTextView = tracksValue;
            this.timeValueTextView = timeValue;
        }
    }

    private class SettingsAdapter extends RecyclerListView.SelectionAdapter {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM   = 1;
        private final Context ctx;
        public boolean isDetailsExpanded = false;
        SettingsAdapter(Context c) { ctx = c; }

        @Override public boolean isEnabled(RecyclerView.ViewHolder h) {
            return h.getItemViewType() == TYPE_ITEM;
        }
        @Override public int getItemCount() { return 1 + (isDetailsExpanded ? ITEMS.length : 0); }
        @Override public int getItemViewType(int p) { return p == 0 ? TYPE_HEADER : TYPE_ITEM; }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) return buildHeader(ctx);
            return new RecyclerListView.Holder(new GlassSettingCell(ctx));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int pos) {
            if (holder.getItemViewType() == TYPE_HEADER) {
                if (holder instanceof HeaderHolder) {
                    HeaderHolder hh = (HeaderHolder) holder;
                    SharedPreferences sp = soundgram.music.SoundGramStatsManager.getStatsPrefs(currentAccount);
                    int   tt = sp.getInt("total_tracks", 0);
                    long  ts = sp.getLong("total_seconds", 0L);
                    long  h  = ts / 3600, m = (ts % 3600) / 60;
                    if (hh.tracksValueTextView != null) {
                        hh.tracksValueTextView.setText(String.valueOf(tt));
                    }
                    if (hh.timeValueTextView != null) {
                        hh.timeValueTextView.setText(h + getString(R.string.hour) + " " + m + getString(R.string.min));
                    }
                }
                return;
            }
            String[] row = ITEMS[pos - 1];
            ((GlassSettingCell) holder.itemView).bind(getItemTitle(pos - 1), getItemSubtitle(pos - 1), row[2], row[3], row[4], row[5]);
        }
    }

    // ── Header (stats) ─────────────────────────────────────────────────────────
    private HeaderHolder buildHeader(Context ctx) {
        int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(16), AndroidUtilities.dp(8));

        TextView lbl = makeLabel(ctx, getString(R.string.sg_style_section_stats));
        root.addView(lbl, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        FrameLayout card = new FrameLayout(ctx);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        cardBg.setCornerRadius(AndroidUtilities.dp(20));
        card.setBackground(cardBg);
        if (Build.VERSION.SDK_INT >= 21) card.setElevation(AndroidUtilities.dp(2));

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(16),
                AndroidUtilities.dp(12), AndroidUtilities.dp(16));

        SharedPreferences sp = soundgram.music.SoundGramStatsManager.getStatsPrefs(currentAccount);
        int   tt = sp.getInt("total_tracks", 0);
        long  ts = sp.getLong("total_seconds", 0L);
        long  h  = ts / 3600, m = (ts % 3600) / 60;

        TextView[] tracksHolder = new TextView[1];
        TextView[] timeHolder = new TextView[1];

        row.addView(makeStatPill(ctx, "🎵", String.valueOf(tt), getString(R.string.sg_stats_tracks_label), accent, tracksHolder),
                LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 8, 0));
        row.addView(makeStatPill(ctx, "⏱", h + getString(R.string.hour) + " " + m + getString(R.string.min), getString(R.string.sg_stats_time_label), accent, timeHolder),
                LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 8, 0, 0, 0));
        card.addView(row, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        root.addView(card, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // ── Public library setting ────────────────────────────────────────────────
        FrameLayout pubCard = new FrameLayout(ctx);
        GradientDrawable pubCardBg = new GradientDrawable();
        pubCardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        pubCardBg.setCornerRadius(AndroidUtilities.dp(20));
        pubCard.setBackground(pubCardBg);
        if (Build.VERSION.SDK_INT >= 21) pubCard.setElevation(AndroidUtilities.dp(2));

        LinearLayout pubRow = new LinearLayout(ctx);
        pubRow.setOrientation(LinearLayout.HORIZONTAL);
        pubRow.setGravity(Gravity.CENTER_VERTICAL);
        pubRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        LinearLayout pubTextCol = new LinearLayout(ctx);
        pubTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView pubTitle = new TextView(ctx);
        pubTitle.setText(getString(R.string.sg_public_library_title));
        pubTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        pubTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        pubTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        pubTextCol.addView(pubTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView pubSub = new TextView(ctx);
        pubSub.setText(getString(R.string.sg_public_library_subtitle));
        pubSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        pubSub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        pubTextCol.addView(pubSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        pubRow.addView(pubTextCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        Switch pubSwitch = new Switch(ctx);
        pubSwitch.setChecked(PublicLibraryManager.isLibraryPublic(currentAccount));
        pubSwitch.setOnClickListener(v -> {
            long channelId = soundgram.music.ChannelMusicFragment.getChannelIdFromFile(currentAccount);
            if (channelId == 0) {
                pubSwitch.setChecked(false);
                BulletinFactory.of(SoundGramStyleSettingsFragment.this)
                        .createErrorBulletin(getString(R.string.sg_open_music_first))
                        .show();
                return;
            }
            org.telegram.tgnet.TLRPC.Chat chat = org.telegram.messenger.MessagesController.getInstance(currentAccount).getChat(channelId);
            String username = chat != null ? org.telegram.messenger.ChatObject.getPublicUsername(chat) : null;
            if (!android.text.TextUtils.isEmpty(username)) {
                boolean enable = pubSwitch.isChecked();
                if (enable) {
                    pubSwitch.setChecked(true);

                    Bulletin.ProgressTwoLineAnimatedTitleLottieLayout layout = new Bulletin.ProgressTwoLineAnimatedTitleLottieLayout(ctx, getResourceProvider());
                    layout.titleTextView.setText("Публикация библиотеки");
                    layout.subtitleTextView.setText("Отправка трека... 0%");
                    layout.setAnimation(R.raw.ic_save_to_music, "Box", "Arrow");
                    layout.setProgress(0.01f);

                    Bulletin progressBulletin = Bulletin.make(SoundGramStyleSettingsFragment.this, layout, 60000);
                    progressBulletin.setCanHide(false);
                    progressBulletin.show();

                    PublicLibraryManager.publishLibraryWithTrack(currentAccount, getParentActivity(), new PublicLibraryManager.PublishListener() {
                        @Override
                        public void onProgress(float progress) {
                            AndroidUtilities.runOnUIThread(() -> {
                                layout.setProgress(progress);
                                layout.subtitleTextView.setText("Отправка трека... " + (int) (progress * 100) + "%");
                            });
                        }

                        @Override
                        public void onDone(boolean success) {
                            AndroidUtilities.runOnUIThread(() -> {
                                if (progressBulletin != null) {
                                    progressBulletin.setCanHide(true);
                                    progressBulletin.hide();
                                }
                                pubSwitch.setChecked(success);
                                if (success) {
                                    BulletinFactory.of(SoundGramStyleSettingsFragment.this)
                                            .createSimpleBulletin(R.raw.contact_check, "Библиотека открыта")
                                            .show();
                                } else {
                                    BulletinFactory.of(SoundGramStyleSettingsFragment.this)
                                            .createErrorBulletin("Не удалось открыть библиотеку")
                                            .show();
                                }
                            });
                        }
                    });
                } else {
                    PublicLibraryManager.unsavePhantomTrack(currentAccount);
                    pubSwitch.setChecked(false);
                    BulletinFactory.of(SoundGramStyleSettingsFragment.this)
                            .createSimpleBulletin(R.raw.contact_check, "Библиотека закрыта")
                            .show();
                }
            } else {
                pubSwitch.setChecked(false);
                org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
                builder.setTitle(getString(R.string.sg_public_library_title));
                builder.setMessage(getString(R.string.sg_public_library_username_required));
                builder.setNegativeButton(getString(R.string.Cancel), null);
                builder.setPositiveButton(getString(R.string.sg_set_username), (d, w) -> {
                    presentFragment(new org.telegram.ui.ChatEditTypeActivity(channelId, false));
                });
                showDialog(builder.create());
            }
        });
        pubRow.addView(pubSwitch, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        pubCard.addView(pubRow);
        root.addView(pubCard, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // Rays count setting
        root.addView(makeLabel(ctx, getString(R.string.sg_sphere_settings)),
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        FrameLayout raysCard = new FrameLayout(ctx);
        GradientDrawable raysCardBg = new GradientDrawable();
        raysCardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        raysCardBg.setCornerRadius(AndroidUtilities.dp(20));
        raysCard.setBackground(raysCardBg);
        if (Build.VERSION.SDK_INT >= 21) raysCard.setElevation(AndroidUtilities.dp(2));

        LinearLayout raysRow = new LinearLayout(ctx);
        raysRow.setOrientation(LinearLayout.VERTICAL);
        raysRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        TextView raysTitle = new TextView(ctx);
        raysTitle.setText(getString(R.string.sg_sphere_rays_title));
        raysTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        raysTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        raysTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        raysRow.addView(raysTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView raysSub = new TextView(ctx);
        raysSub.setText(getString(R.string.sg_sphere_rays_subtitle));
        raysSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        raysSub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        raysRow.addView(raysSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 10));

        SeekBar raysSeek = new SeekBar(ctx);
        raysSeek.setMax(200); // 0 to 200 => 100 to 300 (1x to 3x)
        raysSeek.setProgress(prefs().getInt(KEY_WAVE_RAYS, 100) - 100);
        raysSeek.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
        raysSeek.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));
        raysSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser) {
                    prefs().edit().putInt(KEY_WAVE_RAYS, 100 + progress).apply();
                }
            }
        });
        raysRow.addView(raysSeek, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        
        raysCard.addView(raysRow);
        root.addView(raysCard, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // Sphere size setting
        FrameLayout sizeCard = new FrameLayout(ctx);
        GradientDrawable sizeCardBg = new GradientDrawable();
        sizeCardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        sizeCardBg.setCornerRadius(AndroidUtilities.dp(20));
        sizeCard.setBackground(sizeCardBg);
        if (Build.VERSION.SDK_INT >= 21) sizeCard.setElevation(AndroidUtilities.dp(2));

        LinearLayout sizeRow = new LinearLayout(ctx);
        sizeRow.setOrientation(LinearLayout.VERTICAL);
        sizeRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        TextView sizeTitle = new TextView(ctx);
        sizeTitle.setText(getString(R.string.sg_sphere_size_title));
        sizeTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        sizeTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        sizeTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        sizeRow.addView(sizeTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        int initialSize = prefs().getInt(KEY_WAVE_SPHERE_SIZE, 100);
        TextView sizeSub = new TextView(ctx);
        sizeSub.setText(String.format(getString(R.string.sg_sphere_size_subtitle), initialSize));
        sizeSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        sizeSub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        sizeRow.addView(sizeSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 10));

        SeekBar sizeSeek = new SeekBar(ctx);
        sizeSeek.setMax(100); // 0 to 100 => 70 to 170 (0.7x to 1.7x)
        sizeSeek.setProgress(initialSize - 70);
        sizeSeek.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
        sizeSeek.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));
        sizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser) {
                    int val = 70 + progress;
                    prefs().edit().putInt(KEY_WAVE_SPHERE_SIZE, val).apply();
                    sizeSub.setText(String.format(getString(R.string.sg_sphere_size_subtitle), val));
                }
            }
        });
        sizeRow.addView(sizeSeek, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        
        sizeCard.addView(sizeRow);
        root.addView(sizeCard, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // ── Crossfade setting ──────────────────────────────────────────────────────
        root.addView(makeLabel(ctx, "ВОСПРОИЗВЕДЕНИЕ И ПЕРЕХОДЫ"),
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        FrameLayout cfCard = new FrameLayout(ctx);
        GradientDrawable cfCardBg = new GradientDrawable();
        cfCardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        cfCardBg.setCornerRadius(AndroidUtilities.dp(20));
        cfCard.setBackground(cfCardBg);
        if (Build.VERSION.SDK_INT >= 21) cfCard.setElevation(AndroidUtilities.dp(2));

        LinearLayout cfRow = new LinearLayout(ctx);
        cfRow.setOrientation(LinearLayout.VERTICAL);
        cfRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        TextView cfTitle = new TextView(ctx);
        cfTitle.setText("Кроссфейд треков");
        cfTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        cfTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        cfTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        cfRow.addView(cfTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        int currentCfSec = soundgram.music.SoundGramConfig.crossfadeDurationSec;
        TextView cfSub = new TextView(ctx);
        cfSub.setText(currentCfSec == 0 ? "Выключен" : ("Плавный переход: " + currentCfSec + " сек."));
        cfSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        cfSub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        cfRow.addView(cfSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 10));

        SeekBar cfSeek = new SeekBar(ctx);
        cfSeek.setMax(12); // 0 to 12 seconds
        cfSeek.setProgress(currentCfSec);
        cfSeek.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
        cfSeek.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));
        cfSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser) {
                    soundgram.music.SoundGramConfig.setCrossfadeDurationSec(progress);
                    cfSub.setText(progress == 0 ? "Выключен" : ("Плавный переход: " + progress + " сек."));
                }
            }
        });
        cfRow.addView(cfSeek, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        cfCard.addView(cfRow);
        root.addView(cfCard, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // ── Import tracks setting ──────────────────────────────────────────────────
        root.addView(makeLabel(ctx, "ИМПОРТ МУЗЫКИ"),
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        FrameLayout importCard = new FrameLayout(ctx);
        GradientDrawable importCardBg = new GradientDrawable();
        importCardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        importCardBg.setCornerRadius(AndroidUtilities.dp(20));
        importCard.setBackground(importCardBg);
        if (Build.VERSION.SDK_INT >= 21) importCard.setElevation(AndroidUtilities.dp(2));

        LinearLayout importRow = new LinearLayout(ctx);
        importRow.setOrientation(LinearLayout.HORIZONTAL);
        importRow.setGravity(Gravity.CENTER_VERTICAL);
        importRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        LinearLayout importTextCol = new LinearLayout(ctx);
        importTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView importTitle = new TextView(ctx);
        importTitle.setText("Импорт треков");
        importTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        importTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        importTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        importTextCol.addView(importTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView importSub = new TextView(ctx);
        importSub.setText("Spotify, VK, Яндекс Музыка, текст");
        importSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        importSub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        importTextCol.addView(importSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        importRow.addView(importTextCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        TextView importBtn = new TextView(ctx);
        importBtn.setText("Импорт");
        importBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        importBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        importBtn.setTextColor(Color.WHITE);
        importBtn.setGravity(Gravity.CENTER);
        importBtn.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(accent);
        btnBg.setCornerRadius(AndroidUtilities.dp(16));
        importBtn.setBackground(btnBg);
        importRow.addView(importBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        importCard.addView(importRow);
        importCard.setOnClickListener(v -> showImportBottomSheet(ctx));
        root.addView(importCard, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // ── Master Surface Transparency + Live Mini Player Preview ─────────────
        root.addView(makeLabel(ctx, "ПРОЗРАЧНОСТЬ ПОВЕРХНОСТЕЙ"),
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        FrameLayout masterCard = new FrameLayout(ctx);
        GradientDrawable masterCardBg = new GradientDrawable();
        masterCardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        masterCardBg.setCornerRadius(AndroidUtilities.dp(20));
        masterCard.setBackground(masterCardBg);
        if (Build.VERSION.SDK_INT >= 21) masterCard.setElevation(AndroidUtilities.dp(2));

        LinearLayout masterCol = new LinearLayout(ctx);
        masterCol.setOrientation(LinearLayout.VERTICAL);
        masterCol.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16),
                AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        // Live preview mini-player
        FrameLayout previewMini = new FrameLayout(ctx);
        previewMini.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8),
                AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        int initialAlpha = prefs().getInt(KEY_ALPHA_MINI, 220);
        previewMini.setBackground(SoundGramDesignTokens.makeAsymmetricDrawable(
                androidx.core.graphics.ColorUtils.setAlphaComponent(SoundGramDesignTokens.COLOR_SURFACE_1, initialAlpha),
                SoundGramDesignTokens.COLOR_LINE, 1, 18, 18, 18, 18));

        LinearLayout miniRow = new LinearLayout(ctx);
        miniRow.setOrientation(LinearLayout.HORIZONTAL);
        miniRow.setGravity(Gravity.CENTER_VERTICAL);

        View miniCover = new View(ctx);
        miniCover.setBackground(SoundGramDesignTokens.makeAsymmetricDrawable(
                SoundGramDesignTokens.COLOR_SURFACE_3, SoundGramDesignTokens.COLOR_LINE, 1, 4, 4, 14, 4));
        miniRow.addView(miniCover, LayoutHelper.createLinear(44, 44, 0, 0, 12, 0));

        LinearLayout miniTextCol = new LinearLayout(ctx);
        miniTextCol.setOrientation(LinearLayout.VERTICAL);
        TextView miniTitle = new TextView(ctx);
        miniTitle.setText("моя волна");
        miniTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        miniTitle.setTypeface(AndroidUtilities.bold());
        miniTitle.setTextColor(SoundGramDesignTokens.COLOR_TEXT);
        miniTextCol.addView(miniTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView miniSub = new TextView(ctx);
        miniSub.setText("92.4 мгц · интерактивное превью");
        miniSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        miniSub.setTypeface(AndroidUtilities.getTypeface("fonts/rmono.ttf") != null ? AndroidUtilities.getTypeface("fonts/rmono.ttf") : android.graphics.Typeface.MONOSPACE);
        miniSub.setTextColor(SoundGramDesignTokens.COLOR_TEXT_MUTED);
        miniTextCol.addView(miniSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        miniRow.addView(miniTextCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        View miniPlay = new View(ctx);
        miniPlay.setBackground(SoundGramDesignTokens.makeAsymmetricDrawable(
                SoundGramDesignTokens.COLOR_ACCENT, SoundGramDesignTokens.COLOR_ACCENT, 0, 18, 18, 18, 18));
        miniRow.addView(miniPlay, LayoutHelper.createLinear(36, 36, 0, 0, 0, 0));

        previewMini.addView(miniRow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        masterCol.addView(previewMini, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        TextView masterLabel = new TextView(ctx);
        masterLabel.setText("Общая прозрачность: " + (int) ((1f - initialAlpha / 255f) * 100f) + "%");
        masterLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        masterLabel.setTypeface(AndroidUtilities.getTypeface("fonts/rmono.ttf") != null ? AndroidUtilities.getTypeface("fonts/rmono.ttf") : android.graphics.Typeface.MONOSPACE);
        masterLabel.setTextColor(SoundGramDesignTokens.COLOR_TEXT_MUTED);
        masterCol.addView(masterLabel, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        SeekBar masterSeek = new SeekBar(ctx);
        masterSeek.setMax(255);
        masterSeek.setProgress(255 - initialAlpha);
        masterSeek.setProgressTintList(android.content.res.ColorStateList.valueOf(SoundGramDesignTokens.COLOR_ACCENT));
        masterSeek.setThumbTintList(android.content.res.ColorStateList.valueOf(SoundGramDesignTokens.COLOR_ACCENT));
        masterSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser) {
                    int alpha = 255 - progress;
                    masterLabel.setText("Общая прозрачность: " + (int) ((progress / 255f) * 100f) + "%");
                    previewMini.setBackground(SoundGramDesignTokens.makeAsymmetricDrawable(
                            androidx.core.graphics.ColorUtils.setAlphaComponent(SoundGramDesignTokens.COLOR_SURFACE_1, alpha),
                            SoundGramDesignTokens.COLOR_LINE, 1, 18, 18, 18, 18));
                    prefs().edit()
                            .putInt(KEY_ALPHA_TRACKS, alpha)
                            .putInt(KEY_ALPHA_ALBUMS, alpha)
                            .putInt(KEY_ALPHA_WAVE, alpha)
                            .putInt(KEY_ALPHA_PLAYLISTS, alpha)
                            .putInt(KEY_ALPHA_PLAYER, alpha)
                            .putInt(KEY_ALPHA_MINI, alpha)
                            .putInt(KEY_ALPHA_ARTIST, alpha)
                            .putInt(KEY_ALPHA_EQ, alpha)
                            .apply();
                    notifyStyleChanged();
                }
            }
        });
        masterCol.addView(masterSeek, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        masterCard.addView(masterCol);
        root.addView(masterCard, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        CollapsibleSectionHeaderView detailsHeader = new CollapsibleSectionHeaderView(ctx, "настроить каждый элемент отдельно", true);
        detailsHeader.setOnToggleListener(collapsed -> {
            if (settingsAdapter != null) {
                settingsAdapter.isDetailsExpanded = !collapsed;
                settingsAdapter.notifyDataSetChanged();
            }
        });
        root.addView(detailsHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        return new HeaderHolder(root, tracksHolder[0], timeHolder[0]);
    }

    private TextView makeLabel(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        tv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        tv.setLetterSpacing(0.08f);
        return tv;
    }

    /**
     * SoundGram: карточка с тумблером «Сделать библиотеку публичной».
     * При включении сохраняет флаг через ChannelStorageManager.
     */


    private View makeStatPill(Context ctx, String emoji, String value, String label, int accent, TextView[] outValView) {
        LinearLayout p = new LinearLayout(ctx);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setGravity(Gravity.CENTER);
        p.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(14),
                AndroidUtilities.dp(8), AndroidUtilities.dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 18));
        bg.setCornerRadius(AndroidUtilities.dp(16));
        bg.setStroke(AndroidUtilities.dp(1),
                androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 60));
        p.setBackground(bg);
        for (String[] td : new String[][]{{emoji, "26", ""}, {value, "20", "bold"}, {label, "11", ""}}) {
            TextView tv = new TextView(ctx);
            tv.setText(td[0]);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, Float.parseFloat(td[1]));
            tv.setGravity(Gravity.CENTER);
            if (!td[2].isEmpty()) {
                tv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                if (outValView != null) {
                    outValView[0] = tv;
                }
            } else {
                tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            }
            p.addView(tv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        }
        return p;
    }

    private void showImportBottomSheet(Context ctx) {
        Context activityCtx = getParentActivity() != null ? getParentActivity() : ctx;
        BottomSheet.Builder builder = new BottomSheet.Builder(activityCtx);
        builder.setApplyBottomPadding(true);

        LinearLayout container = new LinearLayout(activityCtx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(20));

        int dialogBg = Theme.getColor(Theme.key_dialogBackground);
        int textColor = Theme.getColor(Theme.key_dialogTextBlack);
        int grayColor = Theme.getColor(Theme.key_dialogTextGray);
        int accent = Theme.getColor(Theme.key_featuredStickers_addButton);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(dialogBg);
        bg.setCornerRadii(new float[]{AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20), 0, 0, 0, 0});
        container.setBackground(bg);

        // Header
        TextView titleTv = new TextView(activityCtx);
        titleTv.setText("Импорт музыки");
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        titleTv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleTv.setTextColor(textColor);
        container.addView(titleTv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        TextView hintTv = new TextView(activityCtx);
        hintTv.setText("Вставьте ссылку на плейлист, библиотеку, альбом или трек (YouTube, YouTube Music, VK, Яндекс Музыка, Spotify, Telegram-канал) или список треков текстом (Исполнитель - Трек):");
        hintTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        hintTv.setTextColor(grayColor);
        hintTv.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        container.addView(hintTv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // Input view
        LinearLayout inputLayout = new LinearLayout(activityCtx);
        inputLayout.setOrientation(LinearLayout.VERTICAL);

        EditText inputEt = new EditText(activityCtx);
        inputEt.setHint("https://music.youtube.com/playlist?list=...\nhttps://open.spotify.com/playlist/...\nhttps://vk.com/audios12345678\nhttps://t.me/music_channel\nили:\nMiyagi - Minor\nImagine Dragons - Believer");
        inputEt.setHintTextColor(Theme.getColor(Theme.key_chat_messagePanelHint));
        inputEt.setTextColor(textColor);
        inputEt.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        inputEt.setMinLines(3);
        inputEt.setMaxLines(7);
        inputEt.setGravity(Gravity.TOP | Gravity.START);
        inputEt.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));

        GradientDrawable etBg = new GradientDrawable();
        etBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        etBg.setCornerRadius(AndroidUtilities.dp(12));
        etBg.setStroke(AndroidUtilities.dp(1), androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 40));
        inputEt.setBackground(etBg);
        inputLayout.addView(inputEt, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Quick buttons row (Paste clipboard, clear)
        LinearLayout actionsRow = new LinearLayout(activityCtx);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView pasteBtn = new TextView(activityCtx);
        pasteBtn.setText("📋 Вставить из буфера");
        pasteBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        pasteBtn.setTextColor(accent);
        pasteBtn.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
        pasteBtn.setOnClickListener(v -> {
            try {
                android.content.ClipboardManager cm = (android.content.ClipboardManager) activityCtx.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                    CharSequence clip = cm.getPrimaryClip().getItemAt(0).getText();
                    if (!TextUtils.isEmpty(clip)) {
                        inputEt.setText(clip);
                        inputEt.setSelection(clip.length());
                    }
                }
            } catch (Exception ignored) {}
        });
        actionsRow.addView(pasteBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        View spacer = new View(activityCtx);
        actionsRow.addView(spacer, LayoutHelper.createLinear(0, 0, 1f));

        TextView clearBtn = new TextView(activityCtx);
        clearBtn.setText("Очистить");
        clearBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        clearBtn.setTextColor(grayColor);
        clearBtn.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
        clearBtn.setOnClickListener(v -> inputEt.setText(""));
        actionsRow.addView(clearBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        inputLayout.addView(actionsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Progress layout (hidden initially)
        LinearLayout progressLayout = new LinearLayout(activityCtx);
        progressLayout.setOrientation(LinearLayout.VERTICAL);
        progressLayout.setVisibility(View.GONE);

        TextView statusTv = new TextView(activityCtx);
        statusTv.setText("Подготовка...");
        statusTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        statusTv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        statusTv.setTextColor(textColor);
        progressLayout.addView(statusTv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(activityCtx, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
        progressLayout.addView(progressBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        TextView trackDetailTv = new TextView(activityCtx);
        trackDetailTv.setText("");
        trackDetailTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        trackDetailTv.setTextColor(grayColor);
        trackDetailTv.setSingleLine(true);
        trackDetailTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        progressLayout.addView(trackDetailTv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        container.addView(inputLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        container.addView(progressLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Main action button (Импортировать / Готово)
        TextView actionBtn = new TextView(activityCtx);
        actionBtn.setText("Импортировать");
        actionBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        actionBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        actionBtn.setTextColor(Color.WHITE);
        actionBtn.setGravity(Gravity.CENTER);
        actionBtn.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14));

        GradientDrawable actionBg = new GradientDrawable();
        actionBg.setColor(accent);
        actionBg.setCornerRadius(AndroidUtilities.dp(16));
        actionBtn.setBackground(actionBg);

        container.addView(actionBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setCustomView(container);
        BottomSheet sheet = builder.create();
        sheet.setFocusable(true);

        actionBtn.setOnClickListener(v -> {
            String text = inputEt.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                BulletinFactory.of(SoundGramStyleSettingsFragment.this)
                        .createErrorBulletin("Вставьте ссылку или текст для импорта")
                        .show();
                return;
            }

            AndroidUtilities.hideKeyboard(inputEt);
            sheet.dismiss();

            soundgram.music.MusicImporter.startImport(currentAccount, text, null);

            BulletinFactory.of(SoundGramStyleSettingsFragment.this)
                    .createSimpleBulletin(R.raw.ic_save_to_music, "Импорт музыки запущен", "Прогресс отображается на вкладке музыки")
                    .show();
        });

        showDialog(sheet);
        inputEt.requestFocus();
        inputEt.postDelayed(() -> AndroidUtilities.showKeyboard(inputEt), 200);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GlassSettingCell
    //
    //  ┌─────────────────────────────────────────────────────────────┐
    //  │ [ColorPill]  Заголовок                       [Switch=обводка]│
    //  │              подзаголовок                                    │
    //  │ Прозрачность  ←════════════●════════════→                   │
    //  │               ЛЕВО=непрозр  ПРАВО=прозр                     │
    //  └─────────────────────────────────────────────────────────────┘
    // ══════════════════════════════════════════════════════════════════════════
    private class GlassSettingCell extends LinearLayout {
        private String  keySwitch, keyAlpha, keyBgColor, keyStrokeColor;
        private int     currentAlpha;
        private boolean strokeEnabled;

        private LinearLayout previewBox;
        private TextView     titleTv, subtitleTv;
        private Switch       strokeToggle;
        private SeekBar      alphaSeek;

        // Пилюля с цветами
        private View   bgColorSwatch;
        private View   strokeColorSwatch;

        GlassSettingCell(Context ctx) {
            super(ctx);
            setOrientation(LinearLayout.VERTICAL);
            setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            build(ctx);
        }

        private void build(Context ctx) {
            int accent = Theme.getColor(Theme.key_featuredStickers_addButton);

            // Outer padding
            setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(6),
                    AndroidUtilities.dp(16), AndroidUtilities.dp(6));

            // ── Карточка-превью ─────────────────────────────────────────────
            previewBox = new LinearLayout(ctx);
            previewBox.setOrientation(LinearLayout.VERTICAL);
            if (Build.VERSION.SDK_INT >= 21) previewBox.setElevation(AndroidUtilities.dp(2));

            // ── TopRow: цвет-пилюля | текст | switch ───────────────────────
            LinearLayout topRow = new LinearLayout(ctx);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);
            topRow.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(14),
                    AndroidUtilities.dp(14), AndroidUtilities.dp(14));

            // Пилюля с двумя цветами (левая = фон, правая = обводка)
            FrameLayout colorPill = buildColorPill(ctx);
            topRow.addView(colorPill, LayoutHelper.createLinear(
                    50, 28, Gravity.CENTER_VERTICAL, 0, 0, 14, 0));

            // Текст
            LinearLayout textCol = new LinearLayout(ctx);
            textCol.setOrientation(LinearLayout.VERTICAL);
            titleTv = new TextView(ctx);
            titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            titleTv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            titleTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            textCol.addView(titleTv, LayoutHelper.createLinear(
                    LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            subtitleTv = new TextView(ctx);
            subtitleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            subtitleTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            textCol.addView(subtitleTv, LayoutHelper.createLinear(
                    LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
            topRow.addView(textCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

            // Switch = управляет только обводкой
            strokeToggle = new Switch(ctx);
            strokeToggle.setThumbTintList(
                    android.content.res.ColorStateList.valueOf(accent));
            strokeToggle.setTrackTintList(new android.content.res.ColorStateList(
                    new int[][]{{-android.R.attr.state_checked}, {android.R.attr.state_checked}},
                    new int[]{
                            androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 70),
                            androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 180)}));
            topRow.addView(strokeToggle, LayoutHelper.createLinear(
                    LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
            previewBox.addView(topRow, LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            // ── Slider row ─────────────────────────────────────────────────
            LinearLayout sliderRow = new LinearLayout(ctx);
            sliderRow.setOrientation(LinearLayout.HORIZONTAL);
            sliderRow.setGravity(Gravity.CENTER_VERTICAL);
            sliderRow.setPadding(AndroidUtilities.dp(14), 0,
                    AndroidUtilities.dp(14), AndroidUtilities.dp(14));

            // Левая метка = "Непрозрачно"
            TextView leftLbl = new TextView(ctx);
            leftLbl.setText(getString(R.string.sg_opacity_opaque));
            leftLbl.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
            leftLbl.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            sliderRow.addView(leftLbl, LayoutHelper.createLinear(
                    LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));

            alphaSeek = new SeekBar(ctx);
            alphaSeek.setMax(255);
            // Инвертируем: максимум seekbar = непрозрачно (alpha=255), минимум = прозрачно (alpha=0)
            alphaSeek.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(accent));
            alphaSeek.setThumbTintList(
                    android.content.res.ColorStateList.valueOf(accent));
            sliderRow.addView(alphaSeek, LayoutHelper.createLinear(
                    0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 6, 0));

            // Правая метка = "Прозрачно"
            TextView rightLbl = new TextView(ctx);
            rightLbl.setText(getString(R.string.sg_opacity_transparent));
            rightLbl.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
            rightLbl.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            sliderRow.addView(rightLbl, LayoutHelper.createLinear(
                    LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            previewBox.addView(sliderRow, LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            addView(previewBox, LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            // ── Листенеры ──────────────────────────────────────────────────
            strokeToggle.setOnCheckedChangeListener((btn, checked) -> {
                strokeEnabled = checked;
                prefs().edit().putBoolean(keySwitch, checked).apply();
                refreshPreviewBg();
                notifyStyleChanged();
            });

            // Слайдер: ЛЕВО = непрозрачно (255), ПРАВО = прозрачно (0)
            alphaSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onStartTrackingTouch(SeekBar s) {}
                @Override public void onStopTrackingTouch(SeekBar s) { notifyStyleChanged(); }
                @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                    if (!fromUser) return;
                    currentAlpha = progress;
                    prefs().edit().putInt(keyAlpha, progress).apply();
                    refreshPreviewBg();
                }
            });
        }

        // Пилюля: левая половина = bgColor, правая = strokeColor
        private FrameLayout buildColorPill(Context ctx) {
            FrameLayout pill = new FrameLayout(ctx);
            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setCornerRadius(AndroidUtilities.dp(14));
            pillBg.setStroke(AndroidUtilities.dp(1),
                    androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF, 80));
            pill.setBackground(pillBg);
            pill.setClipToOutline(true);

            // Левая половина (цвет фона)
            bgColorSwatch = new View(ctx);
            pill.addView(bgColorSwatch, new FrameLayout.LayoutParams(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT) {{
                width = AndroidUtilities.dp(25);
            }});

            // Правая половина (цвет обводки)
            strokeColorSwatch = new View(ctx);
            pill.addView(strokeColorSwatch, new FrameLayout.LayoutParams(
                    AndroidUtilities.dp(25), LayoutHelper.MATCH_PARENT, Gravity.RIGHT));

            // Разделитель между половинками
            View divider = new View(ctx);
            divider.setBackgroundColor(0x40FFFFFF);
            pill.addView(divider, new FrameLayout.LayoutParams(
                    AndroidUtilities.dp(1), LayoutHelper.MATCH_PARENT, Gravity.CENTER_HORIZONTAL));

            pill.setOnClickListener(v -> showColorTargetMenu(v));
            return pill;
        }

        public void bind(String title, String subtitle,
                         String kSwitch, String kAlpha,
                         String kBgColor, String kStrokeColor) {
            keySwitch      = kSwitch;
            keyAlpha       = kAlpha;
            keyBgColor     = kBgColor;
            keyStrokeColor = kStrokeColor;

            strokeEnabled  = isStrokeEnabled(kSwitch);
            currentAlpha   = SoundGramStyleSettingsFragment.getAlpha(kAlpha);

            titleTv.setText(title);
            subtitleTv.setText(subtitle);

            strokeToggle.setOnCheckedChangeListener(null);
            strokeToggle.setChecked(strokeEnabled);
            strokeToggle.setOnCheckedChangeListener((btn, checked) -> {
                strokeEnabled = checked;
                prefs().edit().putBoolean(keySwitch, checked).apply();
                refreshPreviewBg();
                notifyStyleChanged();
            });

            // Слайдер: позиция = alpha (255 = слева = непрозрачно)
            alphaSeek.setProgress(currentAlpha);

            refreshSwatches();
            refreshPreviewBg();
        }

        /** Обновляет цветовые кружки пилюли */
        private void refreshSwatches() {
            bgColorSwatch.setBackgroundColor(resolvedBgColor(keyBgColor));
            strokeColorSwatch.setBackgroundColor(resolvedStrokeColor(keyStrokeColor));
        }

        /** Перерисовывает фон карточки-превью по текущим настройкам */
        private void refreshPreviewBg() {
            int bgCol  = resolvedBgColor(keyBgColor);
            int strCol = resolvedStrokeColor(keyStrokeColor);
            int radius = AndroidUtilities.dp(18);

            GradientDrawable gd = new GradientDrawable();
            gd.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(bgCol, currentAlpha));
            gd.setCornerRadius(radius);
            if (strokeEnabled) {
                gd.setStroke(AndroidUtilities.dp(1),
                        androidx.core.graphics.ColorUtils.setAlphaComponent(strCol, 180));
            }
            previewBox.setBackground(gd);
        }

        // ── Дропдаун: "Для стекла / Для обводки" ──────────────────────────
        private void showColorTargetMenu(View anchor) {
            if (getContext() == null) return;
            BottomSheet.Builder builder = new BottomSheet.Builder(getContext());
            LinearLayout container = new LinearLayout(getContext());
            container.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Theme.getColor(Theme.key_dialogBackground));
            float r = AndroidUtilities.dp(24);
            bg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
            container.setBackground(bg);
            container.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(24));

            // Handle
            View handle = new View(getContext());
            GradientDrawable hBg = new GradientDrawable();
            hBg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(
                    Theme.getColor(Theme.key_windowBackgroundWhiteGrayText), 80));
            hBg.setCornerRadius(AndroidUtilities.dp(2));
            handle.setBackground(hBg);
            FrameLayout hw = new FrameLayout(getContext());
            hw.addView(handle, new FrameLayout.LayoutParams(
                    AndroidUtilities.dp(40), AndroidUtilities.dp(4), Gravity.CENTER));
            container.addView(hw, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 24, 0, 0, 0, 12));

            // Заголовок
            TextView tit = new TextView(getContext());
            tit.setText(String.format(getString(R.string.sg_color_pick_title), titleTv.getText()));
            tit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
            tit.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            tit.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            tit.setGravity(Gravity.CENTER);
            container.addView(tit, LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 20, 0, 20, 20));

            // Опция: Для стекла
            container.addView(makeMenuRow(getContext(), getString(R.string.sg_color_for_glass), keyBgColor, true, builder));
            // Опция: Для обводки
            container.addView(makeMenuRow(getContext(), getString(R.string.sg_color_for_stroke), keyStrokeColor, false, builder));

            builder.setCustomView(container);
            // Показываем через BaseFragment (у нас есть доступ к showDialog через внешний фрагмент)
            SoundGramStyleSettingsFragment.this.showDialog(builder.create());
        }

        private View makeMenuRow(Context ctx, String label, String colorKey,
                                 boolean isBg, BottomSheet.Builder parentBuilder) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(14),
                    AndroidUtilities.dp(20), AndroidUtilities.dp(14));
            row.setBackground(Theme.createSelectorDrawable(
                    Theme.getColor(Theme.key_listSelector), 0));

            // Цветовой кружок
            View swatch = new View(ctx);
            int savedColor = isBg ? resolvedBgColor(colorKey) : resolvedStrokeColor(colorKey);
            GradientDrawable swatchBg = new GradientDrawable();
            swatchBg.setColor(savedColor);
            swatchBg.setShape(GradientDrawable.OVAL);
            swatchBg.setStroke(AndroidUtilities.dp(1),
                    androidx.core.graphics.ColorUtils.setAlphaComponent(0xFF000000, 30));
            swatch.setBackground(swatchBg);
            row.addView(swatch, LayoutHelper.createLinear(
                    22, 22, Gravity.CENTER_VERTICAL, 0, 0, 16, 0));

            TextView tv = new TextView(ctx);
            tv.setText(label);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            tv.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            row.addView(tv, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

            TextView chevron = new TextView(ctx);
            chevron.setText("›");
            chevron.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            chevron.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            row.addView(chevron, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            row.setOnClickListener(v -> {
                parentBuilder.getDismissRunnable().run();
                showColorPicker(ctx, label, colorKey, isBg);
            });
            return row;
        }

        // ── Полноэкранный ColorPicker (как на скрине) ─────────────────────
        private void showColorPicker(Context ctx, String forLabel, String colorKey, boolean isBg) {
            BottomSheet.Builder builder = new BottomSheet.Builder(
                    SoundGramStyleSettingsFragment.this.getParentActivity());

            LinearLayout root = new LinearLayout(ctx);
            root.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable rootBg = new GradientDrawable();
            rootBg.setColor(Theme.getColor(Theme.key_dialogBackground));
            float r = AndroidUtilities.dp(24);
            rootBg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
            root.setBackground(rootBg);
            root.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(8),
                    AndroidUtilities.dp(20), AndroidUtilities.dp(24));

            // Handle
            View handle = new View(ctx);
            GradientDrawable hbg = new GradientDrawable();
            hbg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(
                    Theme.getColor(Theme.key_windowBackgroundWhiteGrayText), 80));
            hbg.setCornerRadius(AndroidUtilities.dp(2));
            handle.setBackground(hbg);
            FrameLayout hw = new FrameLayout(ctx);
            hw.addView(handle, new FrameLayout.LayoutParams(
                    AndroidUtilities.dp(40), AndroidUtilities.dp(4), Gravity.CENTER));
            root.addView(hw, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 24, 0, 0, 0, 16));

            // Шапка: [заголовок]  [Сброс]
            FrameLayout headerRow = new FrameLayout(ctx);
            TextView pickerTitle = new TextView(ctx);
            pickerTitle.setText(forLabel);
            pickerTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
            pickerTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            pickerTitle.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            headerRow.addView(pickerTitle, LayoutHelper.createFrame(
                    LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL | Gravity.LEFT));

            TextView resetBtn = new TextView(ctx);
            resetBtn.setText(getString(R.string.sg_color_reset));
            resetBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            resetBtn.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
            headerRow.addView(resetBtn, LayoutHelper.createFrame(
                    LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL | Gravity.RIGHT));
            root.addView(headerRow, LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, 40, 0, 0, 0, 16));

            // Начальный цвет
            int initialColor = isBg ? resolvedBgColor(colorKey) : resolvedStrokeColor(colorKey);
            final int[] pickedColor = {initialColor};

            // ── Основной квадрат HSV (Saturation + Value) ─────────────────
            ColorSVPickerView svPicker = new ColorSVPickerView(ctx, initialColor);
            root.addView(svPicker, LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, 240, 0, 0, 0, 16));

            // ── Полоска оттенка (Hue bar) ──────────────────────────────────
            HueBarView hueBar = new HueBarView(ctx, initialColor);
            root.addView(hueBar, LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, 28, 0, 0, 0, 16));

            // Связываем: Hue bar → SV picker перерисовывается
            hueBar.setHueListener(hue -> {
                svPicker.setHue(hue);
                pickedColor[0] = svPicker.getColor();
            });
            svPicker.setColorListener(() -> {
                pickedColor[0] = svPicker.getColor();
            });

            // ── Hex поле ───────────────────────────────────────────────────
            LinearLayout hexRow = new LinearLayout(ctx);
            hexRow.setOrientation(LinearLayout.HORIZONTAL);
            hexRow.setGravity(Gravity.CENTER_VERTICAL);

            // Превью итогового цвета
            View previewSwatch = new View(ctx);
            GradientDrawable prevBg = new GradientDrawable();
            prevBg.setShape(GradientDrawable.OVAL);
            prevBg.setColor(initialColor);
            previewSwatch.setBackground(prevBg);
            hexRow.addView(previewSwatch, LayoutHelper.createLinear(
                    32, 32, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

            EditText hexEdit = new EditText(ctx);
            hexEdit.setText(String.format("%06X", 0xFFFFFF & initialColor));
            hexEdit.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            hexEdit.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            hexEdit.setHint("rrggbb");
            hexEdit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            hexEdit.setTypeface(AndroidUtilities.getTypeface("fonts/rmonospace.ttf"));
            hexEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
            GradientDrawable editBg = new GradientDrawable();
            editBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
            editBg.setCornerRadius(AndroidUtilities.dp(8));
            hexEdit.setBackground(editBg);
            hexEdit.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6),
                    AndroidUtilities.dp(10), AndroidUtilities.dp(6));
            hexRow.addView(hexEdit, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
            root.addView(hexRow, LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

            // Синхронизация hex ↔ pickers
            svPicker.setColorListener(() -> {
                int color = svPicker.getColor();
                pickedColor[0] = color;
                prevBg.setColor(color);
                String hex = String.format("%06X", 0xFFFFFF & color);
                if (!hexEdit.getText().toString().equalsIgnoreCase(hex))
                    hexEdit.setText(hex);
            });
            hexEdit.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void afterTextChanged(Editable e) {
                    if (e.length() == 6) {
                        try {
                            int color = Color.parseColor("#" + e.toString());
                            pickedColor[0] = color;
                            prevBg.setColor(color);
                            svPicker.setColor(color);
                            hueBar.setColor(color);
                        } catch (Exception ignored) {}
                    }
                }
            });

            // Сброс
            resetBtn.setOnClickListener(v -> {
                prefs().edit().putInt(colorKey, -1).apply();
                int def = isBg
                        ? Theme.getColor(Theme.key_dialogBackground)
                        : Theme.getColor(Theme.key_featuredStickers_addButton);
                pickedColor[0] = def;
                svPicker.setColor(def);
                hueBar.setColor(def);
                prevBg.setColor(def);
                hexEdit.setText(String.format("%06X", 0xFFFFFF & def));
                refreshSwatches();
                refreshPreviewBg();
                notifyStyleChanged();
                builder.getDismissRunnable().run();
            });

            // ── Кнопка «Готово» ────────────────────────────────────────────
            TextView doneBtn = new TextView(ctx);
            doneBtn.setText(getString(R.string.sg_color_done));
            doneBtn.setGravity(Gravity.CENTER);
            doneBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            doneBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            doneBtn.setTextColor(0xFFFFFFFF);
            int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
            GradientDrawable doneBg = new GradientDrawable();
            doneBg.setColor(accent);
            doneBg.setCornerRadius(AndroidUtilities.dp(14));
            doneBtn.setBackground(doneBg);
            doneBtn.setOnClickListener(v -> {
                // Применяем только при нажатии Готово
                prefs().edit().putInt(colorKey, pickedColor[0]).apply();
                refreshSwatches();
                refreshPreviewBg();
                notifyStyleChanged();
                builder.getDismissRunnable().run();
            });
            root.addView(doneBtn, LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, 52));

            builder.setCustomView(root);
            SoundGramStyleSettingsFragment.this.showDialog(builder.create());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ColorSVPickerView — квадрат Saturation × Value (как на скрине)
    // ══════════════════════════════════════════════════════════════════════════
    public static class ColorSVPickerView extends View {
        private float hue = 0f, sat = 1f, val = 1f;
        private final Paint huePaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint satPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumbPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumbBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Runnable colorListener;

        ColorSVPickerView(Context ctx, int initialColor) {
            super(ctx);
            setFromColor(initialColor);
            thumbPaint.setStyle(Paint.Style.FILL);
            thumbBorder.setStyle(Paint.Style.STROKE);
            thumbBorder.setStrokeWidth(AndroidUtilities.dp(2));
            thumbBorder.setColor(0xFFFFFFFF);
        }

        void setHue(float h) { hue = h; invalidate(); }
        void setColor(int c) { setFromColor(c); invalidate(); }
        void setColorListener(Runnable r) { colorListener = r; }
        int  getColor() { return Color.HSVToColor(new float[]{hue, sat, val}); }

        private void setFromColor(int c) {
            float[] hsv = new float[3];
            Color.colorToHSV(c, hsv);
            hue = hsv[0]; sat = hsv[1]; val = hsv[2];
        }

        @Override protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            if (w == 0 || h == 0) return;
            // Основной цвет (hue)
            huePaint.setShader(new LinearGradient(0, 0, w, 0,
                    Color.WHITE, Color.HSVToColor(new float[]{hue, 1f, 1f}),
                    Shader.TileMode.CLAMP));
            // Затемнение сверху вниз
            Shader darkShader = new LinearGradient(0, 0, 0, h,
                    Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP);
            satPaint.setShader(darkShader);

            RectF rect = new RectF(0, 0, w, h);
            float cornerR = AndroidUtilities.dp(12);
            canvas.save();
            canvas.clipRect(rect);
            canvas.drawRoundRect(rect, cornerR, cornerR, huePaint);
            canvas.drawRoundRect(rect, cornerR, cornerR, satPaint);
            canvas.restore();

            // Thumb
            float tx = sat * w;
            float ty = (1f - val) * h;
            thumbPaint.setColor(getColor());
            canvas.drawCircle(tx, ty, AndroidUtilities.dp(9), thumbBorder);
            canvas.drawCircle(tx, ty, AndroidUtilities.dp(7), thumbPaint);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            int w = getWidth(), h = getHeight();
            sat = Math.max(0, Math.min(1, e.getX() / w));
            val = Math.max(0, Math.min(1, 1f - e.getY() / h));
            invalidate();
            if (colorListener != null) colorListener.run();
            return true;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HueBarView — горизонтальная полоса оттенка
    // ══════════════════════════════════════════════════════════════════════════
    public static class HueBarView extends View {
        private float hue = 0f;
        private final Paint barPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumbBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        private java.util.function.Consumer<Float> hueListener;

        HueBarView(Context ctx, int initialColor) {
            super(ctx);
            float[] hsv = new float[3];
            Color.colorToHSV(initialColor, hsv);
            hue = hsv[0];
            thumbPaint.setStyle(Paint.Style.FILL);
            thumbBorder.setStyle(Paint.Style.STROKE);
            thumbBorder.setStrokeWidth(AndroidUtilities.dp(2));
            thumbBorder.setColor(0xFFFFFFFF);
        }

        void setHueListener(java.util.function.Consumer<Float> l) { hueListener = l; }
        void setColor(int c) {
            float[] hsv = new float[3]; Color.colorToHSV(c, hsv); hue = hsv[0]; invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            float r = h / 2f;
            // Радужный градиент
            int[] cols = new int[]{
                    0xFFFF0000, 0xFFFFFF00, 0xFF00FF00,
                    0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000
            };
            barPaint.setShader(new LinearGradient(0, 0, w, 0, cols, null, Shader.TileMode.CLAMP));
            RectF bar = new RectF(0, 0, w, h);
            canvas.drawRoundRect(bar, r, r, barPaint);

            // Thumb
            float tx = hue / 360f * w;
            thumbPaint.setColor(Color.HSVToColor(new float[]{hue, 1f, 1f}));
            canvas.drawCircle(tx, h / 2f, r, thumbBorder);
            canvas.drawCircle(tx, h / 2f, r - AndroidUtilities.dp(2), thumbPaint);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            hue = Math.max(0, Math.min(360, e.getX() / getWidth() * 360f));
            invalidate();
            if (hueListener != null) hueListener.accept(hue);
            return true;
        }
    }
}
