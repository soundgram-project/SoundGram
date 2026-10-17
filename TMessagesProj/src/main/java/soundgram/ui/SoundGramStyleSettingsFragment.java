package soundgram.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

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
import soundgram.music.ChannelMusicFragment;
import soundgram.music.PublicLibraryManager;
import soundgram.music.SoundGramConfig;
import soundgram.music.SoundGramEQAlert;
import soundgram.music.SoundGramStatsManager;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

import java.io.File;

/**
 * SoundGram Music Tab Settings in Material Design 3 (MD3).
 * Style customization sliders/colors have been removed in favor of consistent MD3 surfaces.
 */
public class SoundGramStyleSettingsFragment extends BaseFragment {

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

    public static final String KEY_COLOR_BG_TRACKS    = "cbg_tracks";
    public static final String KEY_COLOR_BG_ALBUMS    = "cbg_albums";
    public static final String KEY_COLOR_BG_WAVE      = "cbg_wave";
    public static final String KEY_COLOR_BG_PLAYLISTS = "cbg_playlists";
    public static final String KEY_COLOR_BG_PLAYER    = "cbg_player";
    public static final String KEY_COLOR_BG_MINI      = "cbg_mini";
    public static final String KEY_COLOR_BG_ARTIST    = "cbg_artist";
    public static final String KEY_COLOR_BG_EQ        = "cbg_eq";

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

    public static boolean isStrokeEnabled(String keySwitch) {
        return false;
    }

    public static boolean isGlassEnabled(String keyGlass) {
        return true;
    }

    public static int getAlpha(String keyAlpha) {
        return 255;
    }

    public static int getBgColor(String keyColor) {
        return -1;
    }

    public static int getStrokeColor(String keyColor) {
        return -1;
    }

    public static int resolvedBgColor(String keyColor) {
        return Theme.getColor(Theme.key_dialogBackground);
    }

    public static int resolvedStrokeColor(String keyColor) {
        return Theme.getColor(Theme.key_featuredStickers_addButton);
    }

    private TextView tracksCountView;
    private TextView listenTimeView;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        return true;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Настройки музыки");
        actionBar.setOccupyStatusBar(true);
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    prefs().edit().clear().apply();
                    SoundGramConfig.setCrossfadeDurationSec(0);
                    notifyStyleChanged();
                    android.widget.Toast.makeText(context, "Настройки сброшены", android.widget.Toast.LENGTH_SHORT).show();
                    presentFragment(new SoundGramStyleSettingsFragment(), true);
                }
            }
        });

        org.telegram.ui.ActionBar.ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(1, "Сброс");

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(32));

        int accent = Theme.getColor(Theme.key_featuredStickers_addButton);

        // 1. Stats MD3 Elevated Card
        content.addView(createSectionHeader(context, "ВАША СТАТИСТИКА"));
        FrameLayout statsCard = createMd3Card(context);
        LinearLayout statsRow = new LinearLayout(context);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setGravity(Gravity.CENTER);
        statsRow.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16));

        SharedPreferences sp = SoundGramStatsManager.getStatsPrefs(currentAccount);
        int tt = sp.getInt("total_tracks", 0);
        long ts = sp.getLong("total_seconds", 0L);
        long h = ts / 3600, m = (ts % 3600) / 60;

        TextView[] tHolder = new TextView[1];
        TextView[] timeHolder = new TextView[1];
        statsRow.addView(makeMd3StatPill(context, "🎵", String.valueOf(tt), "Треков в медиатеке", accent, tHolder),
                LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 8, 0));
        statsRow.addView(makeMd3StatPill(context, "⏱", h + " ч " + m + " мин", "Время прослушивания", accent, timeHolder),
                LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 8, 0, 0, 0));
        tracksCountView = tHolder[0];
        listenTimeView = timeHolder[0];

        statsCard.addView(statsRow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        content.addView(statsCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // 2. Music Channel Storage Binding MD3 Card
        content.addView(createSectionHeader(context, "ХРАНИЛИЩЕ МУЗЫКИ"));
        FrameLayout bindCard = createMd3Card(context);
        LinearLayout bindRow = new LinearLayout(context);
        bindRow.setOrientation(LinearLayout.HORIZONTAL);
        bindRow.setGravity(Gravity.CENTER_VERTICAL);
        bindRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        LinearLayout bindTextCol = new LinearLayout(context);
        bindTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView bindTitle = new TextView(context);
        bindTitle.setText("Канал для музыки");
        bindTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        bindTitle.setTypeface(AndroidUtilities.bold());
        bindTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        bindTextCol.addView(bindTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView bindSub = new TextView(context);
        long currentChannelId = ChannelMusicFragment.getChannelIdFromFile(currentAccount);
        TLRPC.Chat linkedChat = currentChannelId != 0 ? MessagesController.getInstance(currentAccount).getChat(currentChannelId) : null;
        if (linkedChat != null && !linkedChat.left && !linkedChat.kicked && !linkedChat.deactivated && !(linkedChat instanceof TLRPC.TL_channelForbidden)) {
            String chTitle = linkedChat.title;
            if (!TextUtils.isEmpty(linkedChat.username)) {
                chTitle += " (@" + linkedChat.username + ")";
            }
            bindSub.setText("Привязан: " + chTitle);
        } else if (currentChannelId != 0) {
            bindSub.setText("Канал ID " + currentChannelId + " (недоступен или удалён)");
        } else {
            bindSub.setText("Канал не привязан");
        }
        bindSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        bindSub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        bindTextCol.addView(bindSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
        bindRow.addView(bindTextCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 12, 0));

        TextView[] bindBtnHolder = new TextView[1];
        bindBtnHolder[0] = createMd3Button(context, currentChannelId != 0 ? "Изменить" : "Привязать", accent, () -> {
            ChannelMusicFragment.showAdminChannelPicker(getParentActivity(), currentAccount, selectedChat -> {
                if (selectedChat != null) {
                    String title = selectedChat.title;
                    if (!TextUtils.isEmpty(selectedChat.username)) {
                        title += " (@" + selectedChat.username + ")";
                    }
                    bindSub.setText("Привязан: " + title);
                    bindBtnHolder[0].setText("Изменить");
                    BulletinFactory.of(SoundGramStyleSettingsFragment.this)
                            .createSimpleBulletin(R.raw.contact_check, "Канал успешно привязан")
                            .show();
                }
            });
        });
        bindRow.addView(bindBtnHolder[0], LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        bindCard.addView(bindRow);
        content.addView(bindCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // 3. Public Library MD3 Card
        content.addView(createSectionHeader(context, "ПРОФИЛЬ И ПУБЛИЧНОСТЬ"));
        FrameLayout pubCard = createMd3Card(context);
        LinearLayout pubRow = new LinearLayout(context);
        pubRow.setOrientation(LinearLayout.HORIZONTAL);
        pubRow.setGravity(Gravity.CENTER_VERTICAL);
        pubRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        LinearLayout pubTextCol = new LinearLayout(context);
        pubTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView pubTitle = new TextView(context);
        pubTitle.setText(getString(R.string.sg_public_library_title));
        pubTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        pubTitle.setTypeface(AndroidUtilities.bold());
        pubTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        pubTextCol.addView(pubTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView pubSub = new TextView(context);
        pubSub.setText(getString(R.string.sg_public_library_subtitle));
        pubSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        pubSub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        pubTextCol.addView(pubSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
        pubRow.addView(pubTextCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 12, 0));

        Switch pubSwitch = new Switch(context);
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
            if (!TextUtils.isEmpty(username)) {
                boolean enable = pubSwitch.isChecked();
                if (enable) {
                    pubSwitch.setChecked(true);
                    Bulletin.ProgressTwoLineAnimatedTitleLottieLayout layout = new Bulletin.ProgressTwoLineAnimatedTitleLottieLayout(context, getResourceProvider());
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
        content.addView(pubCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // 3. Playback & Crossfade MD3 Card
        content.addView(createSectionHeader(context, "ВОСПРОИЗВЕДЕНИЕ И ПЕРЕХОДЫ"));
        FrameLayout cfCard = createMd3Card(context);
        LinearLayout cfCol = new LinearLayout(context);
        cfCol.setOrientation(LinearLayout.VERTICAL);
        cfCol.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        TextView cfTitle = new TextView(context);
        cfTitle.setText("Кроссфейд треков");
        cfTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        cfTitle.setTypeface(AndroidUtilities.bold());
        cfTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        cfCol.addView(cfTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        int currentCfSec = SoundGramConfig.crossfadeDurationSec;
        TextView cfSub = new TextView(context);
        cfSub.setText(currentCfSec == 0 ? "Выключен" : ("Плавный переход: " + currentCfSec + " сек."));
        cfSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        cfSub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        cfCol.addView(cfSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 10));

        SeekBar cfSeek = new SeekBar(context);
        cfSeek.setMax(12);
        cfSeek.setProgress(currentCfSec);
        cfSeek.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
        cfSeek.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));
        cfSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser) {
                    SoundGramConfig.setCrossfadeDurationSec(progress);
                    cfSub.setText(progress == 0 ? "Выключен" : ("Плавный переход: " + progress + " сек."));
                }
            }
        });
        cfCol.addView(cfSeek, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        cfCard.addView(cfCol);
        content.addView(cfCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // 4. Wave Visualizer MD3 Card
        content.addView(createSectionHeader(context, "ВИЗУАЛИЗАТОР «МОЯ ВОЛНА»"));
        FrameLayout waveCard = createMd3Card(context);
        LinearLayout waveCol = new LinearLayout(context);
        waveCol.setOrientation(LinearLayout.VERTICAL);
        waveCol.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        TextView raysTitle = new TextView(context);
        raysTitle.setText(getString(R.string.sg_sphere_rays_title));
        raysTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        raysTitle.setTypeface(AndroidUtilities.bold());
        raysTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        waveCol.addView(raysTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView raysSub = new TextView(context);
        raysSub.setText(getString(R.string.sg_sphere_rays_subtitle));
        raysSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        raysSub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        waveCol.addView(raysSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 10));

        SeekBar raysSeek = new SeekBar(context);
        raysSeek.setMax(200);
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
        waveCol.addView(raysSeek, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        TextView sizeTitle = new TextView(context);
        sizeTitle.setText(getString(R.string.sg_sphere_size_title));
        sizeTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        sizeTitle.setTypeface(AndroidUtilities.bold());
        sizeTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        waveCol.addView(sizeTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        int initialSize = prefs().getInt(KEY_WAVE_SPHERE_SIZE, 100);
        TextView sizeSub = new TextView(context);
        sizeSub.setText(String.format(getString(R.string.sg_sphere_size_subtitle), initialSize));
        sizeSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        sizeSub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        waveCol.addView(sizeSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 10));

        SeekBar sizeSeek = new SeekBar(context);
        sizeSeek.setMax(100);
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
        waveCol.addView(sizeSeek, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        waveCard.addView(waveCol);
        content.addView(waveCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // 5. DSP Equalizer MD3 Card
        content.addView(createSectionHeader(context, "ЗВУК И ЭФФЕКТЫ"));
        FrameLayout eqCard = createMd3Card(context);
        LinearLayout eqRow = new LinearLayout(context);
        eqRow.setOrientation(LinearLayout.HORIZONTAL);
        eqRow.setGravity(Gravity.CENTER_VERTICAL);
        eqRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        LinearLayout eqTextCol = new LinearLayout(context);
        eqTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView eqTitle = new TextView(context);
        eqTitle.setText("DSP Эквалайзер и эффекты");
        eqTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        eqTitle.setTypeface(AndroidUtilities.bold());
        eqTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        eqTextCol.addView(eqTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView eqSub = new TextView(context);
        eqSub.setText("10-полосный эквалайзер, усиление басов, реверберация, пресеты");
        eqSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        eqSub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        eqTextCol.addView(eqSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
        eqRow.addView(eqTextCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 12, 0));

        TextView eqBtn = createMd3Button(context, "Открыть", accent, () -> {
            new SoundGramEQAlert(getParentActivity(), getResourceProvider()).show();
        });
        eqRow.addView(eqBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        eqCard.addView(eqRow);
        content.addView(eqCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // 6. Music Import MD3 Card
        content.addView(createSectionHeader(context, "ИМПОРТ МУЗЫКИ"));
        FrameLayout importCard = createMd3Card(context);
        LinearLayout importRow = new LinearLayout(context);
        importRow.setOrientation(LinearLayout.HORIZONTAL);
        importRow.setGravity(Gravity.CENTER_VERTICAL);
        importRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        LinearLayout importTextCol = new LinearLayout(context);
        importTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView importTitle = new TextView(context);
        importTitle.setText("Импорт треков");
        importTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        importTitle.setTypeface(AndroidUtilities.bold());
        importTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        importTextCol.addView(importTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView importSub = new TextView(context);
        importSub.setText("Spotify, VK, Яндекс Музыка, YouTube, текстовые списки");
        importSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        importSub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        importTextCol.addView(importSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
        importRow.addView(importTextCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 12, 0));

        TextView importBtn = createMd3Button(context, "Импорт", accent, () -> showImportBottomSheet(context));
        importRow.addView(importBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        importCard.addView(importRow);
        importCard.setOnClickListener(v -> showImportBottomSheet(context));
        content.addView(importCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // 7. Storage Cache MD3 Card
        content.addView(createSectionHeader(context, "ХРАНИЛИЩЕ И КЭШ"));
        FrameLayout cacheCard = createMd3Card(context);
        LinearLayout cacheRow = new LinearLayout(context);
        cacheRow.setOrientation(LinearLayout.HORIZONTAL);
        cacheRow.setGravity(Gravity.CENTER_VERTICAL);
        cacheRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        LinearLayout cacheTextCol = new LinearLayout(context);
        cacheTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView cacheTitle = new TextView(context);
        cacheTitle.setText("Кэш аудиозаписей");
        cacheTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        cacheTitle.setTypeface(AndroidUtilities.bold());
        cacheTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        cacheTextCol.addView(cacheTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView cacheSub = new TextView(context);
        cacheSub.setText("Локальные аудиофайлы и кэш обложек");
        cacheSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        cacheSub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        cacheTextCol.addView(cacheSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
        cacheRow.addView(cacheTextCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 12, 0));

        TextView cacheBtn = createMd3Button(context, "Очистить", Theme.getColor(Theme.key_text_RedRegular), () -> {
            try {
                File cacheDir = ApplicationLoader.applicationContext.getCacheDir();
                if (cacheDir != null) {
                    File musicCache = new File(cacheDir, "music");
                    if (musicCache.exists() && musicCache.isDirectory()) {
                        File[] files = musicCache.listFiles();
                        if (files != null) {
                            for (File f : files) f.delete();
                        }
                    }
                }
            } catch (Throwable ignored) {}
            BulletinFactory.of(SoundGramStyleSettingsFragment.this)
                    .createSimpleBulletin(R.raw.ic_save_to_music, "Кэш музыки очищен")
                    .show();
        });
        cacheRow.addView(cacheBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        cacheCard.addView(cacheRow);
        content.addView(cacheCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        scrollView.addView(content);
        fragmentView = scrollView;
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (tracksCountView != null && listenTimeView != null) {
            SharedPreferences sp = SoundGramStatsManager.getStatsPrefs(currentAccount);
            int tt = sp.getInt("total_tracks", 0);
            long ts = sp.getLong("total_seconds", 0L);
            long h = ts / 3600, m = (ts % 3600) / 60;
            tracksCountView.setText(String.valueOf(tt));
            listenTimeView.setText(h + " ч " + m + " мин");
        }
    }

    private void notifyStyleChanged() {
        SoundGramStyleManager.dispatchStyleChanged();
        for (int i = 0; i < 4; i++) {
            NotificationCenter.getInstance(i).postNotificationName(NOTIFICATION_STYLE_CHANGED);
        }
    }

    private TextView createSectionHeader(Context context, String title) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        tv.setTypeface(AndroidUtilities.bold());
        tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        tv.setPadding(AndroidUtilities.dp(6), 0, 0, AndroidUtilities.dp(8));
        return tv;
    }

    private FrameLayout createMd3Card(Context context) {
        FrameLayout card = new FrameLayout(context);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setCornerRadius(AndroidUtilities.dp(20));
        card.setBackground(bg);
        if (Build.VERSION.SDK_INT >= 21) {
            card.setElevation(AndroidUtilities.dp(2));
        }
        return card;
    }

    private TextView createMd3Button(Context context, String text, int color, Runnable onClick) {
        TextView btn = new TextView(context);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        btn.setTypeface(AndroidUtilities.bold());
        btn.setTextColor(Color.WHITE);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(AndroidUtilities.dp(16));
        btn.setBackground(bg);
        btn.setOnClickListener(v -> {
            if (onClick != null) onClick.run();
        });
        return btn;
    }

    private View makeMd3StatPill(Context ctx, String icon, String value, String label, int accent, TextView[] outHolder) {
        LinearLayout pill = new LinearLayout(ctx);
        pill.setOrientation(LinearLayout.VERTICAL);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));

        GradientDrawable bg = new GradientDrawable();
        int surfaceColor = Theme.getColor(Theme.key_windowBackgroundGray);
        bg.setColor(surfaceColor);
        bg.setCornerRadius(AndroidUtilities.dp(16));
        pill.setBackground(bg);

        TextView iconTv = new TextView(ctx);
        iconTv.setText(icon);
        iconTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        iconTv.setGravity(Gravity.CENTER);
        pill.addView(iconTv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView valTv = new TextView(ctx);
        valTv.setText(value);
        valTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        valTv.setTypeface(AndroidUtilities.bold());
        valTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        valTv.setGravity(Gravity.CENTER);
        pill.addView(valTv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

        TextView lblTv = new TextView(ctx);
        lblTv.setText(label);
        lblTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        lblTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        lblTv.setGravity(Gravity.CENTER);
        pill.addView(lblTv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (outHolder != null) outHolder[0] = valTv;
        return pill;
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
        bg.setCornerRadii(new float[]{AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24), 0, 0, 0, 0});
        container.setBackground(bg);

        TextView titleTv = new TextView(activityCtx);
        titleTv.setText("Импорт музыки");
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        titleTv.setTypeface(AndroidUtilities.bold());
        titleTv.setTextColor(textColor);
        container.addView(titleTv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        TextView hintTv = new TextView(activityCtx);
        hintTv.setText("Вставьте ссылку на плейлист или трек (YouTube, Spotify, VK, Яндекс Музыка) или список треков текстом (Исполнитель - Трек):");
        hintTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        hintTv.setTextColor(grayColor);
        hintTv.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        container.addView(hintTv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        LinearLayout inputLayout = new LinearLayout(activityCtx);
        inputLayout.setOrientation(LinearLayout.VERTICAL);

        EditText inputEt = new EditText(activityCtx);
        inputEt.setHint("https://music.youtube.com/playlist?list=...\nhttps://open.spotify.com/playlist/...\nили:\nLinkin Park - Numb\nQueen - Bohemian Rhapsody");
        inputEt.setHintTextColor(Theme.getColor(Theme.key_chat_messagePanelHint));
        inputEt.setTextColor(textColor);
        inputEt.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        inputEt.setMinLines(3);
        inputEt.setMaxLines(7);
        inputEt.setGravity(Gravity.TOP | Gravity.START);
        inputEt.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));

        GradientDrawable etBg = new GradientDrawable();
        etBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        etBg.setCornerRadius(AndroidUtilities.dp(16));
        etBg.setStroke(AndroidUtilities.dp(1), ColorUtils.setAlphaComponent(accent, 40));
        inputEt.setBackground(etBg);
        inputLayout.addView(inputEt, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

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

        container.addView(inputLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView actionBtn = new TextView(activityCtx);
        actionBtn.setText("Импортировать");
        actionBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        actionBtn.setTypeface(AndroidUtilities.bold());
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
}
