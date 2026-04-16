package org.telegram.ui;

import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.messenger.LocaleController.getStringResId;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.telegram.ui.SoundGramLyricsDialog;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.ChatActivityInterface;
import org.telegram.ui.Components.ChatAvatarContainer;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.AudioPlayerAlert;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.widget.LinearLayout;
import android.util.TypedValue;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.MusicWebSearcher;

public class ChannelMusicFragment extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, ChatActivityInterface {

    public interface PlaylistUpdateListener {
        void onPlaylistUpdated();
    }
    private ImageView playWaveBtn;
    private WaveAnimatedWidget waveWidget; // Добавь в начало класса

    // Добавьте или замените эти переменные в классе:
    private SavedAlbumsAdapter savedAlbumsAdapter;
    public static ArrayList<MessageObject> savedAlbumMessages = new ArrayList<>();
    private RecyclerListView albumsRecyclerView;
    private AlbumAdapter albumsAdapter; // Адаптер для альбомов добавим ниже
    private LinearLayout tabStrip;
    private androidx.viewpager.widget.ViewPager viewPager;
    private LinearLayout libraryListHeader;
    private RecyclerListView artistRecyclerView;
    private ArtistAdapter artistAdapter;
    private SwipeRefreshLayout swipeRefreshLayout; // ИСПРАВЛЕНИЕ: Сделать переменную глобальной
    private RecyclerListView listView;
    private MusicAdapter adapter;

    private RecyclerListView playlistsListView;
    private PlaylistsHorizontalAdapter playlistsAdapter;

    private LinearLayout headerContainer;

    private EditText searchField;
    private ImageView searchBackBtn;
    private ImageView searchIconBtn;
    private ImageView refreshBtn;
    private ImageView statsBtn;
    private TextView headerTitle;
    private LinearLayout standardHeaderLayout;
    private LinearLayout searchHeaderLayout;

    public static ArrayList<MessageObject> audioMessages = new ArrayList<>();
    private ArrayList<MessageObject> searchResultMessages = new ArrayList<>();
    private ArrayList<MusicWebSearcher.WebTrack> webResultTracks = new ArrayList<>();
    private ArrayList<File> playlistFiles = new ArrayList<>();
    private long musicChannelId = 0;
    private long musicChannelHash = 0;
    private boolean isSearching = false;
    private Timer searchTimer;
    private long lastTrackedId = 0;

    private static final String CHANNEL_ID_FILE_NAME = "music_channel_id.txt";

    public static void safeNotifyAdapter(RecyclerView.Adapter adapt, RecyclerListView lv) {
        if (adapt == null) return;
        if (lv != null && lv.isComputingLayout()) {
            lv.post(adapt::notifyDataSetChanged);
        } else {
            adapt.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.didReceiveNewMessages);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidStart);

        checkOrCreateMusicChannel();
        refreshPlaylistFiles();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (waveWidget != null) {
            waveWidget.stopAnimation();
        }
        isWaitingForUpload = false;
        AndroidUtilities.cancelRunOnUIThread(pollMessagesRunnable);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.didReceiveNewMessages);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidStart);

    }


    @Override
    public boolean onBackPressed(boolean force) {
        if (isSearching) {
            closeSearch();
            return false;
        }
        if (fragmentView != null) {
            AndroidUtilities.hideKeyboard(fragmentView);
        }
        return super.onBackPressed(force);
    }

    @Override
    public long getDialogId() { return getTargetChannelId(); }
    @Override public long getTopicId() { return 0; }
    @Override public SizeNotifierFrameLayout getContentView() { return (SizeNotifierFrameLayout) fragmentView; }
    @Override public ChatAvatarContainer getAvatarContainer() { return null; }
    @Override public TLRPC.Chat getCurrentChat() { return MessagesController.getInstance(currentAccount).getChat(getTargetChannelId()); }
    @Override public TLRPC.User getCurrentUser() { return null; }

    public int countBeforeUpload = 0;
    public boolean isWaitingForUpload = false;
    public final Runnable pollMessagesRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isWaitingForUpload) return;
            if (audioMessages.size() > countBeforeUpload) {
                isWaitingForUpload = false;
                return;
            }
            loadMusicFromChannel();
            AndroidUtilities.runOnUIThread(pollMessagesRunnable, 2000);
        }
    };

    @Override
    public View createView(Context context) {
        actionBar.setAddToContainer(false);
        SizeNotifierFrameLayout content = new SizeNotifierFrameLayout(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                canvas.drawColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            }
        };
        fragmentView = content;

        LinearLayout mainColumn = new LinearLayout(context);
        mainColumn.setOrientation(LinearLayout.VERTICAL);

        headerContainer = new LinearLayout(context);
        headerContainer.setOrientation(LinearLayout.VERTICAL);
        headerContainer.setClickable(true);
        headerContainer.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        headerContainer.setPadding(0, AndroidUtilities.statusBarHeight, 0, 0);

        artistRecyclerView = new RecyclerListView(context);
        artistRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        artistAdapter = new ArtistAdapter(context);
        artistRecyclerView.setAdapter(artistAdapter);
        artistRecyclerView.setVisibility(View.GONE);

        albumsRecyclerView = new RecyclerListView(context);
        albumsRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        albumsAdapter = new AlbumAdapter(context);
        albumsRecyclerView.setAdapter(albumsAdapter);
        albumsRecyclerView.setVisibility(View.GONE);

        createSearchMenu(context, headerContainer);

        headerContainer.addView(artistRecyclerView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        headerContainer.addView(albumsRecyclerView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        mainColumn.addView(headerContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        tabStrip = new LinearLayout(context);
        tabStrip.setOrientation(LinearLayout.HORIZONTAL);
        tabStrip.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));

        TextView waveTabBtn = new TextView(context);
        waveTabBtn.setText(getString(R.string.mywave));
        waveTabBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        waveTabBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        waveTabBtn.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        waveTabBtn.setPadding(0, 0, AndroidUtilities.dp(24), 0);

        TextView libraryTabBtn = new TextView(context);
        libraryTabBtn.setText(getString(R.string.lib));
        libraryTabBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        libraryTabBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        libraryTabBtn.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        libraryTabBtn.setPadding(0, 0, AndroidUtilities.dp(24), 0);

        TextView albumsTabBtn = new TextView(context);
        albumsTabBtn.setText("Альбомы");
        albumsTabBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        albumsTabBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        albumsTabBtn.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));

        tabStrip.addView(waveTabBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        tabStrip.addView(libraryTabBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        tabStrip.addView(albumsTabBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        mainColumn.addView(tabStrip, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));


        // --- 1. VIEWPAGER С ИДЕАЛЬНЫМИ СВАЙПАМИ (ДЛЯ ГЛАВНОГО МЕНЮ ТГ) ---
        viewPager = new androidx.viewpager.widget.ViewPager(context) {
            private float initialX, initialY;

            @Override
            public boolean onInterceptTouchEvent(android.view.MotionEvent ev) {
                if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    initialX = ev.getX();
                    initialY = ev.getY();
                    getParent().requestDisallowInterceptTouchEvent(true);
                } else if (ev.getAction() == android.view.MotionEvent.ACTION_MOVE) {
                    float dx = ev.getX() - initialX;
                    float dy = ev.getY() - initialY;

                    if (Math.abs(dx) > Math.abs(dy)) { // Горизонтальный свайп
                        if (dx > 0 && getCurrentItem() == 0) {
                            // Мы на левой вкладке (Моя Волна) и свайпаем вправо -> отдаем жест Чатам
                            getParent().requestDisallowInterceptTouchEvent(false);
                            return false;
                        } else if (dx < 0 && getCurrentItem() == getAdapter().getCount() - 1) {
                            // Мы на правой вкладке (Альбомы) и свайпаем влево -> отдаем жест Настройкам
                            getParent().requestDisallowInterceptTouchEvent(false);
                            return false;
                        } else {
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }
                    }
                }
                try { return super.onInterceptTouchEvent(ev); } catch (Exception e) { return false; }
            }

            @Override
            public boolean onTouchEvent(android.view.MotionEvent ev) {
                try { return super.onTouchEvent(ev); } catch (Exception e) { return false; }
            }
        };
        viewPager.setOffscreenPageLimit(3);


        // --- 2. ПОЛНОЭКРАННЫЙ ДИЗАЙН "МОЯ ВОЛНА" ---
        FrameLayout waveContainer = new FrameLayout(context);
        waveContainer.setClipChildren(false);

        // СЛОЙ 1: АНИМАЦИЯ (Самый задний фон)
        waveWidget = new WaveAnimatedWidget(context);
        waveContainer.addView(waveWidget, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // СЛОЙ 2: КОНТЕЙНЕР ДЛЯ UI (Чтобы интерфейс гарантированно был ПОВЕРХ шара)
        FrameLayout uiLayer = new FrameLayout(context);

        // Кнопка настроек жанров (Стеклянная)
        TextView waveSettingsBtn = new TextView(context);
        waveSettingsBtn.setText(SmartRadioEngine.currentConfig.query);
        waveSettingsBtn.setTextSize(15);
        waveSettingsBtn.setTextColor(0xFFFFFFFF);
        waveSettingsBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        waveSettingsBtn.setGravity(Gravity.CENTER);
        waveSettingsBtn.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(20), 0x33FFFFFF));
        waveSettingsBtn.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(10), AndroidUtilities.dp(24), AndroidUtilities.dp(10));

        uiLayer.addView(waveSettingsBtn, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 40, 0, 0));
        // --- ТУМБЛЕР ИИ ---
        TextView aiToggleBtn = new TextView(context);
        aiToggleBtn.setText(SmartRadioEngine.useNeuralNetwork ? "✨ Нейросеть: ВКЛ" : "✨ Нейросеть: ВЫКЛ");
        aiToggleBtn.setTextSize(13);
        aiToggleBtn.setTextColor(SmartRadioEngine.useNeuralNetwork ? 0xFF1DB954 : 0xFFAAAAAA);
        aiToggleBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        aiToggleBtn.setGravity(Gravity.CENTER);
        aiToggleBtn.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), 0x33000000));
        aiToggleBtn.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(6), AndroidUtilities.dp(16), AndroidUtilities.dp(6));
        aiToggleBtn.setOnClickListener(v -> {
            SmartRadioEngine.useNeuralNetwork = !SmartRadioEngine.useNeuralNetwork;
            aiToggleBtn.setText(SmartRadioEngine.useNeuralNetwork ? "✨ Нейросеть: ВКЛ" : "✨ Нейросеть: ВЫКЛ");
            aiToggleBtn.setTextColor(SmartRadioEngine.useNeuralNetwork ? 0xFF1DB954 : 0xFFAAAAAA);
            Toast.makeText(context, SmartRadioEngine.useNeuralNetwork ? "Умный подбор включен" : "Обычный подбор включен", Toast.LENGTH_SHORT).show();
        });
        uiLayer.addView(aiToggleBtn, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 90, 0, 0));
        // --- КОНЕЦ ТУМБЛЕРА ---
        // Кнопка Play и заголовок
        playWaveBtn = new ImageView(context);
        playWaveBtn.setScaleType(ImageView.ScaleType.CENTER);
        boolean isInitialPlaying = !MediaController.getInstance().isMessagePaused() && MediaController.getInstance().isMyWave;
        playWaveBtn.setVisibility(isInitialPlaying ? View.GONE : View.VISIBLE);

        TextView waveTitle = new TextView(context);
        waveTitle.setText("");
        waveTitle.setTextSize(42);
        waveTitle.setTextColor(0xFFFFFFFF);
        waveTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        waveTitle.setShadowLayer(8, 0, 4, 0x88000000);
        waveTitle.setGravity(Gravity.CENTER);

        LinearLayout centerGroup = new LinearLayout(context);
        centerGroup.setOrientation(LinearLayout.VERTICAL);
        centerGroup.setGravity(Gravity.CENTER);
        centerGroup.addView(playWaveBtn, LayoutHelper.createLinear(80, 80, Gravity.CENTER, 0, 0, 0, 20));
        centerGroup.addView(waveTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        uiLayer.addView(centerGroup, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        // Добавляем UI слой поверх анимации
        waveContainer.addView(uiLayer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Обработчик кнопки настроек
        waveSettingsBtn.setOnClickListener(v -> showWaveSettingsSheet(context, waveSettingsBtn));

        // Запуск анимации с привязкой к сессии ТГ
        waveWidget.startAnimation();
        int tgSessionId = MediaController.getInstance().getAudioSessionId();
        waveWidget.linkToAudioSession(tgSessionId);

        // 1. НАЖАТИЕ НА ТЕКСТ "МОЯ ВОЛНА" - ЖЕСТКАЯ ГЕНЕРАЦИЯ С НУЛЯ
        waveTitle.setOnClickListener(v -> {
            Toast.makeText(context, "Создаем новую волну...", Toast.LENGTH_SHORT).show();
            MediaController.getInstance().cleanupPlayer(true, true);
            MediaController.getInstance().myWaveBasePlaylist.clear();
            MediaController.getInstance().isMyWave = true;

            SmartRadioEngine.generateWaveFromSettings(currentAccount, new SmartRadioEngine.WaveCallback() {
                @Override
                public void onTracksFound(ArrayList<MessageObject> tracks) {
                    AndroidUtilities.runOnUIThread(() -> {
                        MediaController.getInstance().setPlaylist(tracks, tracks.get(0), MediaController.WAVE_CONTEXT_ID);
                        MediaController.getInstance().playMessage(tracks.get(0));
                    });
                }

                @Override
                public void onMoreTracksAdded(ArrayList<MessageObject> tracks) {
                    AndroidUtilities.runOnUIThread(() -> {
                        ArrayList<MessageObject> currentPlaylist = MediaController.getInstance().getPlaylist();
                        if (currentPlaylist != null && !currentPlaylist.isEmpty()) {
                            currentPlaylist.addAll(tracks);
                        } else {
                            MediaController.getInstance().setPlaylist(tracks, tracks.get(0), MediaController.WAVE_CONTEXT_ID);
                        }
                    });
                }

                @Override
                public void onError() {
                    Toast.makeText(context, "Не удалось сгенерировать волну", Toast.LENGTH_SHORT).show();
                }
            });

        });

        // 2. НАЖАТИЕ НА КНОПКУ PLAY/ШАР - ТОЛЬКО ГЕНЕРАЦИЯ НОВОЙ ВОЛНЫ
        playWaveBtn.setOnClickListener(v -> waveTitle.callOnClick());
        centerGroup.setOnClickListener(v -> waveTitle.callOnClick());

        FrameLayout libraryContainer = new FrameLayout(context);
        swipeRefreshLayout = new SwipeRefreshLayout(context);
        swipeRefreshLayout.setColorSchemeColors(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadMusicFromChannel();
            AndroidUtilities.runOnUIThread(() -> swipeRefreshLayout.setRefreshing(false), 1500);
        });

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setBackgroundColor(Color.TRANSPARENT);
        adapter = new MusicAdapter(context, null);
        listView.setAdapter(adapter);
        listView.setVerticalScrollBarEnabled(false);
        listView.setGlowColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        this.libraryListHeader = new LinearLayout(context);
        this.libraryListHeader.setOrientation(LinearLayout.VERTICAL);
        createHorizontalPlaylists(context, this.libraryListHeader);

        libraryContainer.addView(this.libraryListHeader, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        FrameLayout.LayoutParams swipeParams = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT);
        swipeParams.topMargin = AndroidUtilities.dp(130);
        swipeRefreshLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        libraryContainer.addView(swipeRefreshLayout, swipeParams);

        FrameLayout albumsTabContainer = new FrameLayout(context);
        RecyclerListView savedAlbumsListView = new RecyclerListView(context);
        savedAlbumsListView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(context, 2));
        savedAlbumsAdapter = new SavedAlbumsAdapter(context);
        savedAlbumsListView.setAdapter(savedAlbumsAdapter);
        savedAlbumsListView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(80));
        savedAlbumsListView.setClipToPadding(false);
        albumsTabContainer.addView(savedAlbumsListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        viewPager.setAdapter(new androidx.viewpager.widget.PagerAdapter() {
            @Override public int getCount() { return 3; }
            @Override public boolean isViewFromObject(View view, Object object) { return view == object; }
            @Override public Object instantiateItem(ViewGroup container, int position) {
                View view = position == 0 ? waveContainer : (position == 1 ? libraryContainer : albumsTabContainer);
                container.addView(view);
                return view;
            }
            @Override public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }
        });

        viewPager.addOnPageChangeListener(new androidx.viewpager.widget.ViewPager.OnPageChangeListener() {
            @Override public void onPageScrolled(int pos, float off, int posPx) {}
            @Override public void onPageScrollStateChanged(int state) {}
            @Override public void onPageSelected(int position) {
                waveTabBtn.setTextColor(Theme.getColor(position == 0 ? Theme.key_windowBackgroundWhiteBlackText : Theme.key_windowBackgroundWhiteGrayText));
                libraryTabBtn.setTextColor(Theme.getColor(position == 1 ? Theme.key_windowBackgroundWhiteBlackText : Theme.key_windowBackgroundWhiteGrayText));
                albumsTabBtn.setTextColor(Theme.getColor(position == 2 ? Theme.key_windowBackgroundWhiteBlackText : Theme.key_windowBackgroundWhiteGrayText));
            }
        });

        waveTabBtn.setOnClickListener(v -> viewPager.setCurrentItem(0, true));
        libraryTabBtn.setOnClickListener(v -> viewPager.setCurrentItem(1, true));
        albumsTabBtn.setOnClickListener(v -> viewPager.setCurrentItem(2, true));

        viewPager.setCurrentItem(1, false);
        waveTabBtn.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        libraryTabBtn.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        albumsTabBtn.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));

        mainColumn.addView(viewPager, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1.0f));
        content.addView(mainColumn, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        MiniPlayerView miniPlayerView = new MiniPlayerView(context, this);
        int sysNavBar = AndroidUtilities.navigationBarHeight > 0 ? AndroidUtilities.navigationBarHeight : 0;
        int tgBottomMenu = AndroidUtilities.dp(60);

        FrameLayout.LayoutParams playerParams = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 64, Gravity.BOTTOM);
        playerParams.setMargins(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), sysNavBar + tgBottomMenu + AndroidUtilities.dp(8));
        content.addView(miniPlayerView, playerParams);

        listView.setPadding(0, 0, 0, AndroidUtilities.dp(40));

        listView.setOnItemClickListener((view, position) -> {
            if (adapter.getItemViewType(position) == 2) return;
            if (isSearching) {
                if (position < webResultTracks.size()) {
                    ArrayList<MessageObject> queue = new ArrayList<>();
                    for (MusicWebSearcher.WebTrack wt : webResultTracks) {
                        int durSec = parseDuration(wt.duration);
                        MessageObject fakeMsg = createWebMessageObject(currentAccount, wt.title, wt.artist, wt.downloadUrl, durSec, -(long)wt.downloadUrl.hashCode(), wt.coverUrl);
                        queue.add(fakeMsg);
                    }
                    MessageObject streamMsg = queue.get(position);
                    playContext(streamMsg, queue, -Math.abs((long) "search_context".hashCode()));
                } else {
                    int localPos = position - webResultTracks.size();
                    if (localPos >= 0 && localPos < searchResultMessages.size()) {
                        playContext(searchResultMessages.get(localPos), searchResultMessages, -Math.abs((long) ("search_context".hashCode())));
                    }
                }
            } else {
                if (position >= 0 && position < audioMessages.size()) {
                    playContext(audioMessages.get(position), audioMessages, getTargetChannelId());
                }
            }
        });

        return fragmentView;
    }

    private void showWaveSettingsSheet(Context context, TextView updateTextTarget) {
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        int bgColor = Theme.getColor(Theme.key_dialogBackground);
        int glassColor = android.graphics.Color.argb(240, android.graphics.Color.red(bgColor), android.graphics.Color.green(bgColor), android.graphics.Color.blue(bgColor));
        gd.setColor(glassColor);
        float radius = AndroidUtilities.dp(16);
        gd.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        container.setBackground(gd);
        container.setPadding(0, AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16));

        TextView title = new TextView(context);
        title.setText(getString(R.string.wave_settings)); // "Настройка Моей волны"
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setGravity(Gravity.CENTER);
        container.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 24));

        String[] genres = {
                "Любой жанр", "Поп", "Хип-Хоп", "Рок", "Инди", "Фонк",
                "Гиперпоп", "Пост-панк", "Эмо-рэп", "Метал", "Альтернатива",
                "К-поп", "Джаз", "Блюз", "Классика", "Электроника", "Дабстеп",
                "Панк-рок", "Синтвейв", "Ло-фай", "Эмбиент", "Фолк"
        };

        TextView genreTitle = new TextView(context);
        genreTitle.setText("Жанр");
        genreTitle.setTextSize(14);
        genreTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        container.addView(genreTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16, 0, 16, 8));

        RecyclerListView genreListView = new RecyclerListView(context);
        genreListView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(context, 3));
        ChipsAdapter genreAdapter = new ChipsAdapter(context, genres, SmartRadioEngine.currentConfig.query);
        genreListView.setAdapter(genreAdapter);
        genreListView.setClipToPadding(false);
        genreListView.setPadding(AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12), AndroidUtilities.dp(16));

        ScrollView genreScroll = new ScrollView(context);
        genreScroll.addView(genreListView);
        container.addView(genreScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 300, 0, 0, 0, 16));

        TextView applyBtn = new TextView(context);
        applyBtn.setText("Применить");
        applyBtn.setGravity(Gravity.CENTER);
        applyBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        applyBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        applyBtn.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), 0xFF8A2BE2));
        applyBtn.setTextColor(0xFFFFFFFF);

        applyBtn.setOnClickListener(v -> {
            SmartRadioEngine.currentConfig.query = genreAdapter.getSelectedItem();
            updateTextTarget.setText(SmartRadioEngine.currentConfig.query);
            builder.getDismissRunnable().run();
        });

        container.addView(applyBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 16, 0, 16, 8));

        builder.setCustomView(container);
        showDialog(builder.create());
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didReceiveNewMessages) {
            if ((Long) args[0] == getTargetChannelId()) {
                ArrayList<MessageObject> newMsgs = (ArrayList<MessageObject>) args[1];
                for (MessageObject msg : newMsgs) {
                    if (msg.messageOwner != null && msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) {
                        String[] p = msg.messageOwner.message.split("\\|\\|\\|");
                        if(p.length >= 5) audioMessages.add(0, createWebMessageObject(currentAccount, p[1], p[2], p[3], parseDuration(p[4]), msg.getId(), ""));
                    } else if (msg.isMusic()) {
                        audioMessages.add(0, msg);
                    }
                }
                if (isSearching) filterMusic(searchField.getText().toString());
                else safeNotifyAdapter(adapter, listView);
            }
        } else if (id == NotificationCenter.messagePlayingDidStart) {
            MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
            if (playing != null && playing.getId() != lastTrackedId) {
                lastTrackedId = playing.getId();
            }
            safeNotifyAdapter(adapter, listView);
        }
        else if (id == NotificationCenter.messagePlayingDidReset || id == NotificationCenter.messagePlayingPlayStateChanged) {
            safeNotifyAdapter(adapter, listView);
        }
    }

    private void openSearch() {
        isSearching = true;
        standardHeaderLayout.setVisibility(View.GONE);
        searchHeaderLayout.setVisibility(View.VISIBLE);
        searchField.requestFocus();
        AndroidUtilities.showKeyboard(searchField);

        if (viewPager != null) viewPager.setCurrentItem(1, false);
        if (tabStrip != null) tabStrip.setVisibility(View.GONE);
        if (libraryListHeader != null) libraryListHeader.setVisibility(View.GONE);
        if (playlistsListView != null) playlistsListView.setVisibility(View.GONE); // Скрываем плейлисты

        // ИСПРАВЛЕНИЕ: Убираем отступ у swipeRefreshLayout в поиске
        if (swipeRefreshLayout != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) swipeRefreshLayout.getLayoutParams();
            lp.topMargin = AndroidUtilities.dp(0); // Огромный отступ 130dp заменяется на 0dp
            swipeRefreshLayout.setLayoutParams(lp);
        }
    }


    private void closeSearch() {
        isSearching = false;
        searchField.setText("");

        if (searchField.isFocused()) {
            searchField.clearFocus();
        }
        AndroidUtilities.hideKeyboard(searchField);
        if (fragmentView != null) {
            AndroidUtilities.hideKeyboard(fragmentView);
        }

        searchHeaderLayout.setVisibility(View.GONE);
        standardHeaderLayout.setVisibility(View.VISIBLE);

        if (tabStrip != null) tabStrip.setVisibility(View.VISIBLE);
        if (libraryListHeader != null) libraryListHeader.setVisibility(View.VISIBLE);
        if (playlistsListView != null) playlistsListView.setVisibility(View.VISIBLE); // Возвращаем плейлисты

        artistRecyclerView.setVisibility(View.GONE);
        albumsRecyclerView.setVisibility(View.GONE);
        // ИСПРАВЛЕНИЕ: Обнуляем высоту и маргины
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LayoutHelper.MATCH_PARENT, 0);
        p.setMargins(0, 0, 0, 0); // Обнуляем маргины
        artistRecyclerView.setLayoutParams(p);
        albumsRecyclerView.setLayoutParams(p);

        // ИСПРАВЛЕНИЕ: Возвращаем отступ у swipeRefreshLayout при закрытии поиска
        if (swipeRefreshLayout != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) swipeRefreshLayout.getLayoutParams();
            lp.topMargin = AndroidUtilities.dp(130); // Старый маргин возвращается
            swipeRefreshLayout.setLayoutParams(lp);
        }

        if (webResultTracks != null) webResultTracks.clear();
        if (searchResultMessages != null) searchResultMessages.clear();
        safeNotifyAdapter(adapter, listView);
    }


    public static void saveAlbumToLibrary(String title, String url, String cover) {
        File file = new File(ApplicationLoader.applicationContext.getExternalFilesDir(null), "saved_albums.txt");
        try {
            boolean exists = false;
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new java.io.FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains(url)) { exists = true; break; }
                }
                br.close();
            }
            if (!exists) {
                BufferedWriter bw = new BufferedWriter(new java.io.FileWriter(file, true));
                bw.write(title + "|||" + url + "|||" + cover);
                bw.newLine();
                bw.close();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }

    }

    public static ArrayList<MusicWebSearcher.WebAlbum> getSavedAlbums() {
        ArrayList<MusicWebSearcher.WebAlbum> list = new ArrayList<>();
        File file = new File(ApplicationLoader.applicationContext.getExternalFilesDir(null), "saved_albums.txt");
        if (!file.exists()) return list;
        try (BufferedReader br = new BufferedReader(new java.io.FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|\\|\\|");
                if (p.length >= 3) {
                    list.add(new MusicWebSearcher.WebAlbum(p[0], "", p[1], p[2]));
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    private void showStatisticsBottomSheet() {
        if (getParentActivity() == null) return;

        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        LinearLayout container = new LinearLayout(getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        int bgColor = Theme.getColor(Theme.key_dialogBackground);
        int glassColor = Color.argb(230, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor));
        gd.setColor(glassColor);

        float radius = AndroidUtilities.dp(16);
        gd.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        container.setBackground(gd);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(32));

        TextView title = new TextView(getParentActivity());
        title.setText(getString(R.string.stat));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setGravity(Gravity.CENTER);
        container.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("soundgram_stats", Context.MODE_PRIVATE);
        int totalTracks = prefs.getInt("total_tracks", 0);
        long totalSeconds = prefs.getLong("total_seconds", 0L);

        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder timeFormatted = new StringBuilder();
        if (days > 0) timeFormatted.append(days).append(getString(R.string.day));
        if (hours > 0 || days > 0) timeFormatted.append(hours).append(getString(R.string.hour));
        if (minutes > 0 || hours > 0 || days > 0) timeFormatted.append(minutes).append(getString(R.string.min));
        timeFormatted.append(seconds).append(getString(R.string.sec));

        TextView statsText = new TextView(getParentActivity());
        statsText.setText(getString(R.string.trc) + totalTracks + "\n"+getString(R.string.tm) + timeFormatted.toString());
        statsText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        statsText.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        statsText.setGravity(Gravity.CENTER);
        statsText.setLineSpacing(AndroidUtilities.dp(8), 1.0f);
        container.addView(statsText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setCustomView(container);
        showDialog(builder.create());
    }

    public static int parseDuration(String durationStr) {
        try {
            if (TextUtils.isEmpty(durationStr)) return 0;
            String[] parts = durationStr.split(":");
            if (parts.length == 2) {
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            } else if (parts.length == 3) {
                return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]);
            }
        } catch (Exception e) {}
        return 0;
    }

    // Хелпер создания фейкового MessageObject для стриминга (без скачивания)
    public static MessageObject createWebMessageObject(int currentAccount, String title, String artist, String url, int duration, long messageId, String coverUrl) {
        TLRPC.TL_message webMsg = new TLRPC.TL_message();
        webMsg.id = (int) messageId;
        String safeCover = TextUtils.isEmpty(coverUrl) ? "" : coverUrl;
        webMsg.message = "SG_STREAM|||" + title + "|||" + artist + "|||" + url + "|||" + duration + "|||" + messageId + "|||" + safeCover;
        webMsg.date = (int) (System.currentTimeMillis() / 1000);
        webMsg.dialog_id = 0;

        webMsg.peer_id = new TLRPC.TL_peerChannel();
        webMsg.peer_id.channel_id = 0;

        webMsg.media = new TLRPC.TL_messageMediaDocument();
        webMsg.media.document = new TLRPC.TL_document();
        webMsg.media.document.id = messageId;
        webMsg.media.document.dc_id = 0;
        webMsg.media.document.mime_type = "audio/mp3";
        webMsg.media.document.size = 0;

        TLRPC.TL_documentAttributeAudio attr = new TLRPC.TL_documentAttributeAudio();
        attr.title = title;
        attr.performer = artist;
        attr.duration = duration;
        webMsg.media.document.attributes.add(attr);

        MessageObject msgObj = new MessageObject(currentAccount, webMsg, false, false);
        msgObj.messageOwner.attachPath = url;

        return msgObj;
    }

    public static void setAudioCover(BackupImageView coverImage, MessageObject msg, String filter, int radius) {
        coverImage.setRoundRadius(AndroidUtilities.dp(radius));
        android.graphics.drawable.Drawable placeholder = Theme.createRoundRectDrawable(AndroidUtilities.dp(radius), Theme.getColor(Theme.key_graySection));

        if (msg == null) {
            coverImage.setImageDrawable(placeholder);
            return;
        }

        // ПАРСИМ КАСТОМНУЮ ОБЛОЖКУ
        if (msg.messageOwner != null && msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) {
            String[] parts = msg.messageOwner.message.split("\\|\\|\\|");
            if (parts.length >= 7 && !TextUtils.isEmpty(parts[6])) {
                coverImage.setImage(parts[6], filter, placeholder);
                return;
            }
        }

        TLRPC.Document document = msg.getDocument();
        TLRPC.PhotoSize thumb = null;

        if (msg.photoThumbs != null && !msg.photoThumbs.isEmpty()) {
            thumb = org.telegram.messenger.FileLoader.getClosestPhotoSizeWithSize(msg.photoThumbs, 320);
        }

        if (thumb == null && document != null && document.thumbs != null && !document.thumbs.isEmpty()) {
            thumb = org.telegram.messenger.FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 320);
        }

        if (thumb != null && document != null) {
            coverImage.setImage(org.telegram.messenger.ImageLocation.getForDocument(thumb, document), filter, placeholder, msg);
            return;
        }

        String artworkUrl = msg.getArtworkUrl(true);
        if (!TextUtils.isEmpty(artworkUrl)) {
            coverImage.setImage(artworkUrl, filter, placeholder);
            return;
        }

        if (document != null) {
            org.telegram.messenger.ImageLocation loc = org.telegram.messenger.ImageLocation.getForDocument(document);
            if (loc != null) {
                coverImage.setImage(loc, filter, placeholder, msg);
                return;
            }
        }

        coverImage.setImageDrawable(placeholder);
    }


    private void saveMessagesToCache(ArrayList<MessageObject> messages) {
        if (messages == null || messages.isEmpty()) return;
        try {
            File dir = ApplicationLoader.applicationContext.getExternalFilesDir(null);
            File file = new File(dir, "music_cache_v3.dat");
            SerializedData data = new SerializedData(messages.size() * 1024);
            data.writeInt32(messages.size());
            for (MessageObject msg : messages) msg.messageOwner.serializeToStream(data);
            try (java.io.FileOutputStream stream = new java.io.FileOutputStream(file)) {
                stream.write(data.toByteArray());
            }
            data.cleanup();
        } catch (Exception e) { FileLog.e(e); }
    }


    // 1. Метод сохранения альбома напрямую в канал
    public void saveAlbumToChannel(String title, String url, String coverUrl) {
        long targetId = getTargetChannelId();
        if (targetId == 0) return;
        String text = "SG_ALBUM|||" + title + "|||" + url + "|||" + coverUrl;
        SendMessagesHelper.getInstance(currentAccount).sendMessage(text, -targetId, null, null, null, true, null, null, null, true, 0, 0, null, false);
        Toast.makeText(getParentActivity(), "Альбом сохранен в библиотеку", Toast.LENGTH_SHORT).show();

        countBeforeUpload = audioMessages.size();
        isWaitingForUpload = true;
        AndroidUtilities.runOnUIThread(pollMessagesRunnable, 1500);
    }

    // 2. Метод загрузки кэша (парсинг SG_ALBUM)
    private void loadMessagesFromCache() {
        try {
            File dir = ApplicationLoader.applicationContext.getExternalFilesDir(null);
            File file = new File(dir, "music_cache_v3.dat");
            if (!file.exists()) return;

            byte[] fileData = new byte[(int) file.length()];
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) { fis.read(fileData); }

            SerializedData data = new SerializedData(fileData);
            int count = data.readInt32(false);
            ArrayList<MessageObject> cachedTracks = new ArrayList<>();
            ArrayList<MessageObject> cachedAlbums = new ArrayList<>();
            long selfUserId = UserConfig.getInstance(currentAccount).getClientUserId();
            for (int i = 0; i < count; i++) {
                int constructor = data.readInt32(false);
                TLRPC.Message message = TLRPC.Message.TLdeserialize(data, constructor, false);
                if (message != null) {
                    message.readAttachPath(data, selfUserId);
                    if (message.message != null) {
                        if (message.message.startsWith("SG_STREAM|||")) {
                            String[] parts = message.message.split("\\|\\|\\|");
                            if (parts.length >= 5) {
                                cachedTracks.add(createWebMessageObject(currentAccount, parts[1], parts[2], parts[3], parseDuration(parts[4]), Long.parseLong(parts[5]), parts.length > 6 ? parts[6] : ""));
                            }
                        } else if (message.message.startsWith("SG_ALBUM|||")) {
                            cachedAlbums.add(new MessageObject(currentAccount, message, false, false));
                        } else {
                            // Обычный трек (даже с припиской) добавляется!
                            MessageObject obj = new MessageObject(currentAccount, message, false, false);
                            if (obj.isMusic()) cachedTracks.add(obj);
                        }
                    } else {
                        MessageObject obj = new MessageObject(currentAccount, message, false, false);
                        if (obj.isMusic()) cachedTracks.add(obj);
                    }
                }


            }
            data.cleanup();
            AndroidUtilities.runOnUIThread(() -> {
                audioMessages.clear();
                audioMessages.addAll(cachedTracks);
                savedAlbumMessages.clear();
                savedAlbumMessages.addAll(cachedAlbums);
                safeNotifyAdapter(adapter, listView);
                safeNotifyAdapter(playlistsAdapter, playlistsListView);
                safeNotifyAdapter(savedAlbumsAdapter, null);
                if (audioMessages.isEmpty() && ApplicationLoader.isNetworkOnline()) loadMusicFromChannel();
            });
        } catch (Exception e) { FileLog.e(e); }
    }

    // 3. Метод загрузки из канала онлайн (парсинг SG_ALBUM)
    private void loadMusicFromChannel() {
        long targetId = getTargetChannelId();
        if (targetId == 0) return;
        loadMessagesFromCache();
        if (!org.telegram.messenger.ApplicationLoader.isNetworkOnline()) return;

        TLRPC.TL_messages_search searchReq = new TLRPC.TL_messages_search();
        TLRPC.TL_inputPeerChannel inputPeer = new TLRPC.TL_inputPeerChannel();
        inputPeer.channel_id = targetId; inputPeer.access_hash = musicChannelHash;
        searchReq.peer = inputPeer;
        searchReq.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        searchReq.q = ""; searchReq.limit = 3000;

        org.telegram.tgnet.ConnectionsManager.getInstance(currentAccount).sendRequest(searchReq, (response, error) -> {
            if (response instanceof TLRPC.messages_Messages) {
                TLRPC.messages_Messages messages = (TLRPC.messages_Messages) response;
                ArrayList<MessageObject> loadedTracks = new ArrayList<>();
                ArrayList<MessageObject> loadedAlbums = new ArrayList<>();
                for (TLRPC.Message msg : messages.messages) {
                    if (msg.message != null) {
                        if (msg.message.startsWith("SG_STREAM|||")) {
                            String[] parts = msg.message.split("\\|\\|\\|");
                            if (parts.length >= 5) loadedTracks.add(createWebMessageObject(currentAccount, parts[1], parts[2], parts[3], parseDuration(parts[4]), msg.id, ""));
                        } else if (msg.message.startsWith("SG_ALBUM|||")) {
                            loadedAlbums.add(new MessageObject(currentAccount, msg, false, false));
                        } else {
                            MessageObject messageObject = new MessageObject(currentAccount, msg, false, false);
                            if (messageObject.isMusic()) loadedTracks.add(messageObject);
                        }
                    } else {
                        MessageObject messageObject = new MessageObject(currentAccount, msg, false, false);
                        if (messageObject.isMusic()) loadedTracks.add(messageObject);
                    }
                }



                // Сохраняем в кэш ОБА списка, слив их вместе
                ArrayList<MessageObject> combinedToCache = new ArrayList<>(loadedTracks);
                combinedToCache.addAll(loadedAlbums);
                saveMessagesToCache(combinedToCache);

                AndroidUtilities.runOnUIThread(() -> {
                    audioMessages.clear();
                    audioMessages.addAll(loadedTracks);
                    savedAlbumMessages.clear();
                    savedAlbumMessages.addAll(loadedAlbums);
                    safeNotifyAdapter(adapter, listView);
                    safeNotifyAdapter(playlistsAdapter, playlistsListView);
                    safeNotifyAdapter(savedAlbumsAdapter, null);
                });
            }
        });
    }

    // 4. Адаптер для 3-й вкладки (Альбомы из канала)
    private class SavedAlbumsAdapter extends RecyclerListView.SelectionAdapter {
        private Context mContext;
        public SavedAlbumsAdapter(Context context) { this.mContext = context; }

        @Override public boolean isEnabled(RecyclerView.ViewHolder holder) { return true; }
        @Override public int getItemCount() { return savedAlbumMessages.size(); }

        @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout layout = new LinearLayout(mContext);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            layout.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            layout.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(16));

            BackupImageView cover = new BackupImageView(mContext);
            cover.setRoundRadius(AndroidUtilities.dp(12));
            layout.addView(cover, LayoutHelper.createLinear(140, 140, Gravity.CENTER, 0, 5, 0, 5));

            TextView titleText = new TextView(mContext);
            titleText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            titleText.setGravity(Gravity.CENTER);
            titleText.setMaxLines(2);
            titleText.setEllipsize(android.text.TextUtils.TruncateAt.END);
            layout.addView(titleText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 4, 4, 4, 0));

            return new RecyclerListView.Holder(layout);
        }

        @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            LinearLayout layout = (LinearLayout) holder.itemView;
            BackupImageView cover = (BackupImageView) layout.getChildAt(0);
            TextView titleText = (TextView) layout.getChildAt(1);

            MessageObject msgObj = savedAlbumMessages.get(position);
            String[] parts = msgObj.messageOwner.message.split("\\|\\|\\|");

            String title = parts.length > 1 ? parts[1] : "Unknown";
            String url = parts.length > 2 ? parts[2] : "";
            String coverUrl = parts.length > 3 ? parts[3] : "";

            titleText.setText(title);
            cover.setImage(coverUrl, "200_200", null);

            layout.setOnClickListener(v -> {
                presentFragment(new AlbumProfileFragment(title, url, coverUrl));
            });
        }
    }


    private void createSearchMenu(Context context, LinearLayout container) {
        FrameLayout topBar = new FrameLayout(context);

        standardHeaderLayout = new LinearLayout(context);
        standardHeaderLayout.setOrientation(LinearLayout.HORIZONTAL);
        standardHeaderLayout.setGravity(Gravity.CENTER_VERTICAL);

        statsBtn = new ImageView(context);
        statsBtn.setImageResource(R.drawable.msg_info);
        statsBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.SRC_IN);
        statsBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
        statsBtn.setScaleType(ImageView.ScaleType.CENTER);
        statsBtn.setOnClickListener(v -> showStatisticsBottomSheet());

        headerTitle = new TextView(context);
        headerTitle.setText("Music");
        headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        headerTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        headerTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        headerTitle.setGravity(Gravity.CENTER);

        searchIconBtn = new ImageView(context);
        searchIconBtn.setImageResource(R.drawable.ic_ab_search);
        searchIconBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.SRC_IN);
        searchIconBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
        searchIconBtn.setScaleType(ImageView.ScaleType.CENTER);
        searchIconBtn.setOnClickListener(v -> openSearch());

        refreshBtn = new ImageView(context);
        refreshBtn.setImageResource(R.drawable.msg_download);
        refreshBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.SRC_IN);
        refreshBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
        refreshBtn.setScaleType(ImageView.ScaleType.CENTER);
        refreshBtn.setOnClickListener(v -> showImportDialog());

        standardHeaderLayout.addView(statsBtn, LayoutHelper.createLinear(48, 48));
        standardHeaderLayout.addView(headerTitle, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 40, 0, 0, 0));
        standardHeaderLayout.addView(searchIconBtn, LayoutHelper.createLinear(48, 48));
        standardHeaderLayout.addView(refreshBtn, LayoutHelper.createLinear(48, 48));

        searchHeaderLayout = new LinearLayout(context);
        searchHeaderLayout.setOrientation(LinearLayout.HORIZONTAL);
        searchHeaderLayout.setGravity(Gravity.CENTER_VERTICAL);
        searchHeaderLayout.setVisibility(View.GONE);
        searchHeaderLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        searchBackBtn = new ImageView(context);
        searchBackBtn.setImageResource(R.drawable.ic_ab_back);
        searchBackBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.SRC_IN);
        searchBackBtn.setScaleType(ImageView.ScaleType.CENTER);
        searchBackBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
        searchBackBtn.setOnClickListener(v -> closeSearch());

        // Уменьшенная строка поиска
        FrameLayout searchFieldContainer = new FrameLayout(context);
        searchFieldContainer.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), 0x1A000000));
        searchFieldContainer.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(4));

        ImageView searchInnerIcon = new ImageView(context);
        searchInnerIcon.setImageResource(R.drawable.ic_ab_search);
        searchInnerIcon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.MULTIPLY));
        searchFieldContainer.addView(searchInnerIcon, LayoutHelper.createFrame(20, 20, Gravity.LEFT | Gravity.CENTER_VERTICAL));

        searchField = new EditText(context);
        searchField.setHint(getString(R.string.searchtrc));
        searchField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        searchField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        searchField.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        searchField.setSingleLine(true);
        searchField.setBackgroundDrawable(null);
        searchField.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchTimer != null) searchTimer.cancel();
                searchTimer = new Timer();
                searchTimer.schedule(new TimerTask() {
                    @Override public void run() {
                        AndroidUtilities.runOnUIThread(() -> filterMusic(s.toString()));
                    }
                }, 400); // 400 миллисекунд задержки (идеально для живого поиска)
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Прячем клавиатуру при нажатии Enter
        searchField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                AndroidUtilities.hideKeyboard(searchField);
                return true;
            }
            return false;
        });


        searchFieldContainer.addView(searchField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 28, 0, 0, 0));

        searchHeaderLayout.addView(searchBackBtn, LayoutHelper.createLinear(48, 48));
        searchHeaderLayout.addView(searchFieldContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 0, 0, 16, 0));

        // Высота уменьшена до 48
        topBar.addView(standardHeaderLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48));
        topBar.addView(searchHeaderLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48));
        container.addView(topBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
    }

    private void saveChannelIdToFile(long channelId) {
        if (channelId == 0) return;
        File dir = ApplicationLoader.applicationContext.getExternalFilesDir(null);
        if (dir == null) return;
        File file = new File(dir, CHANNEL_ID_FILE_NAME);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(String.valueOf(channelId));
            musicChannelId = channelId;
        } catch (IOException e) { e.printStackTrace(); }
    }

    private long getChannelIdFromFile() {
        File dir = ApplicationLoader.applicationContext.getExternalFilesDir(null);
        if (dir == null) return 0;
        File file = new File(dir, CHANNEL_ID_FILE_NAME);
        if (!file.exists()) return 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null && !TextUtils.isEmpty(line.trim())) return Long.parseLong(line.trim());
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    private long getTargetChannelId() {
        if (musicChannelId != 0) return musicChannelId;
        return musicChannelId = getChannelIdFromFile();
    }

    private void saveWebTrackToLibrary(MusicWebSearcher.WebTrack track) {
        long targetId = getTargetChannelId();
        if (targetId == 0) return;
        String text = "SG_STREAM|||" + track.title + "|||" + track.artist + "|||" + track.downloadUrl + "|||" + parseDuration(track.duration);
        SendMessagesHelper.getInstance(currentAccount).sendMessage(text, -targetId, null, null, null, true, null, null, null, true, 0, 0, null, false);
        Toast.makeText(getParentActivity(), getString(R.string.trconl), Toast.LENGTH_SHORT).show();

        countBeforeUpload = audioMessages.size();
        isWaitingForUpload = true;
        AndroidUtilities.runOnUIThread(pollMessagesRunnable, 1500);
    }

    private void downloadAndUploadTrack(MusicWebSearcher.WebTrack track) {
        long targetIdFromFile = getChannelIdFromFile();
        if (targetIdFromFile == 0) {
            checkOrCreateMusicChannel();
            return;
        }
        musicChannelId = targetIdFromFile;
        File cacheDir = ApplicationLoader.applicationContext.getExternalCacheDir();
        AlertDialog progressDialog = new AlertDialog.Builder(getParentActivity()).setMessage(getString(R.string.dwld)+ "0%").create();
        progressDialog.show();

        MusicWebSearcher.downloadTrack(track.downloadUrl, cacheDir, new MusicWebSearcher.DownloadCallback() {
            @Override public void onProgress(int percent) {
                AndroidUtilities.runOnUIThread(() -> progressDialog.setMessage(getString(R.string.dwld) + percent + "%"));
            }
            @Override public void onResult(File file) {
                progressDialog.dismiss();
                AndroidUtilities.runOnUIThread(() -> {
                    long rawId = getChannelIdFromFile();
                    if (rawId == 0) return;
                    long dialogId = -rawId;
                    countBeforeUpload = audioMessages.size();
                    isWaitingForUpload = true;
                    AndroidUtilities.runOnUIThread(pollMessagesRunnable, 5000);
                    ArrayList<String> paths = new ArrayList<>();
                    paths.add(file.getAbsolutePath());
                    SendMessagesHelper.prepareSendingDocuments(AccountInstance.getInstance(currentAccount), paths, new ArrayList<>(paths), null, null, "audio/mp3", dialogId, null, null, null, null, null, true, 0, null, null, 0, 0, false, 0);
                });
            }
            @Override public void onFailed() {
                AndroidUtilities.runOnUIThread(() -> Toast.makeText(getParentActivity(), "Failed to download track", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void deleteTrackFromChannel(MessageObject msg) {
        if (msg == null) return;
        long rawId = getChannelIdFromFile();
        if (rawId == 0) return;
        MessagesController.getInstance(currentAccount).deleteMessages(new ArrayList<>(Collections.singletonList(msg.getId())), null, null, -rawId, 0, true, 0);
        audioMessages.remove(msg);
        if (isSearching) searchResultMessages.remove(msg);
        safeNotifyAdapter(adapter, listView);
        Toast.makeText(getParentActivity(), getString(R.string.dl), Toast.LENGTH_SHORT).show();
    }

    private void createHorizontalPlaylists(Context context, LinearLayout container) {
        playlistsListView = new RecyclerListView(context);
        playlistsListView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        playlistsAdapter = new PlaylistsHorizontalAdapter(context);
        playlistsListView.setAdapter(playlistsAdapter);
        playlistsListView.setOnItemClickListener((view, position) -> {
            if (position == 0) {
                showCreatePlaylistDialog();
            } else {
                File file = playlistFiles.get(position - 1);
                presentFragment(new PlaylistDetailFragment(file, getTargetChannelId(), this::refreshPlaylistFiles));
            }
        });

        container.addView(playlistsListView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 130, 16, 8, 16, 12));
    }

    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString(R.string.nwpl));
        LinearLayout container = new LinearLayout(getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(6), AndroidUtilities.dp(24), 0);
        final EditText input = new EditText(getParentActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint(getString(R.string.enttit));
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        input.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        input.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(4), Theme.getColor(Theme.key_windowBackgroundGray)));
        input.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
        container.addView(input, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setView(container);
        builder.setPositiveButton(getString(R.string.create), (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!TextUtils.isEmpty(name)) {
                File file = new File(getPlaylistsDir(), name + ".txt");
                try { if (file.createNewFile()) refreshPlaylistFiles(); } catch (IOException e) { e.printStackTrace(); }
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private static File getPlaylistsDir() {
        File dir = ApplicationLoader.applicationContext.getExternalFilesDir("Playlists");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private void refreshPlaylistFiles() {
        playlistFiles.clear();
        File dir = getPlaylistsDir();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files != null) Collections.addAll(playlistFiles, files);
        safeNotifyAdapter(playlistsAdapter, playlistsListView);
    }

    public void checkOrCreateMusicChannel() {
        final String requiredTitle = UserConfig.getInstance(currentAccount).getClientUserId() + "SoundGram";
        for (TLRPC.Dialog dialog : MessagesController.getInstance(currentAccount).getAllDialogs()) {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialog.id);
            if (chat != null && requiredTitle.equals(chat.title)) {
                musicChannelId = chat.id;
                musicChannelHash = chat.access_hash;
                saveChannelIdToFile(musicChannelId);
                loadMusicFromChannel();
                return;
            }
        }
        long savedId = getChannelIdFromFile();
        if (savedId != 0) {
            musicChannelId = savedId;
            loadMusicFromChannel();
            return;
        }
        createMusicChannel(requiredTitle);
    }

    private void createMusicChannel(String title) {
        TLRPC.TL_channels_createChannel req = new TLRPC.TL_channels_createChannel();
        req.title = title; req.about = "Storage"; req.broadcast = true; req.megagroup = false;
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (error == null && response instanceof TLRPC.Updates) {
                TLRPC.Updates updates = (TLRPC.Updates) response;
                MessagesController.getInstance(currentAccount).processUpdates(updates, false);
                for (TLRPC.Chat chat : updates.chats) {
                    if (chat instanceof TLRPC.TL_channel) {
                        MessagesController.getNotificationsSettings(currentAccount).edit().putLong("music_channel_id", chat.id).apply();
                        musicChannelId = chat.id;
                        musicChannelHash = ((TLRPC.TL_channel) chat).access_hash;
                        AndroidUtilities.runOnUIThread(() -> {
                            saveChannelIdToFile(musicChannelId);
                            loadMusicFromChannel();
                        });
                        break;
                    }
                }
            }
        });
    }



    public void playContext(MessageObject track, ArrayList<MessageObject> queue, long contextId) {
        MediaController mediaController = MediaController.getInstance();
        MessageObject currentPlaying = mediaController.getPlayingMessageObject();
        if (currentPlaying != null && currentPlaying.getId() == track.getId()) {
            if (mediaController.isMessagePaused()) mediaController.playMessage(track);
            else mediaController.pauseMessage(track);
            return;
        }

        for (MessageObject m : queue) {
            m.audioProgress = 0.0f;
            m.audioProgressSec = 0;
        }

        mediaController.setPlaylist(new ArrayList<>(queue), track, contextId);
        mediaController.seekToProgress(track, 0);
        mediaController.playMessage(track);
    }




    public static class AddTracksFragment extends BaseFragment {
        private File playlistFile;
        private PlaylistUpdateListener listener;
        private ArrayList<MessageObject> allTracks;
        private HashSet<Integer> playlistIds = new HashSet<>();
        private RecyclerListView listView;
        private AddTracksAdapter adapter;

        public AddTracksFragment(File file, PlaylistUpdateListener listener) {
            this.playlistFile = file;
            this.listener = listener;
        }

        @Override
        public boolean onFragmentCreate() {
            super.onFragmentCreate();
            allTracks = ChannelMusicFragment.audioMessages;
            if (playlistFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(playlistFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        try {
                            if (line.startsWith("SG_STREAM|||")) {
                                String[] p = line.split("\\|\\|\\|");
                                if (p.length >= 4) playlistIds.add(p[3].hashCode());
                            } else {
                                playlistIds.add(Integer.parseInt(line.trim()));
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (IOException e) { e.printStackTrace(); }
            }
            return true;
        }

        @Override
        public void onFragmentDestroy() {
            super.onFragmentDestroy();

            ArrayList<String> existingOrder = new ArrayList<>();
            if (playlistFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(playlistFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        existingOrder.add(line.trim());
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }

            ArrayList<String> finalOrder = new ArrayList<>();

            for (MessageObject msg : allTracks) {
                int idToCheck = (msg.messageOwner != null && msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) ? msg.messageOwner.message.split("\\|\\|\\|")[3].hashCode() : msg.getId();

                if (playlistIds.contains(idToCheck)) {
                    String identifier = (msg.messageOwner != null && msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) ? msg.messageOwner.message : String.valueOf(msg.getId());
                    if (!existingOrder.contains(identifier)) {
                        finalOrder.add(identifier);
                    }
                }
            }

            for (String idStr : existingOrder) {
                int idToCheck = idStr.startsWith("SG_STREAM|||") ? idStr.split("\\|\\|\\|")[3].hashCode() : Integer.parseInt(idStr);
                if (playlistIds.contains(idToCheck)) {
                    finalOrder.add(idStr);
                }
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(playlistFile, false))) {
                for (String identifier : finalOrder) {
                    bw.write(identifier);
                    bw.newLine();
                }
            } catch (IOException e) { e.printStackTrace(); }

            if (listener != null) listener.onPlaylistUpdated();
        }

        @Override
        public View createView(Context context) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.setTitle(getString(R.string.addtrc));
            actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
                @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
            });

            SizeNotifierFrameLayout content = new SizeNotifierFrameLayout(context) {
                @Override protected void onDraw(Canvas canvas) {
                    canvas.drawColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                }
            };
            fragmentView = content;

            listView = new RecyclerListView(context);
            listView.setLayoutManager(new LinearLayoutManager(context));
            adapter = new AddTracksAdapter(context);
            listView.setAdapter(adapter);

            listView.setOnItemClickListener((view, position) -> {
                MessageObject msg = allTracks.get(position);
                int msgId = (msg.messageOwner != null && msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) ? msg.messageOwner.message.split("\\|\\|\\|")[3].hashCode() : msg.getId();

                if (playlistIds.contains(msgId)) playlistIds.remove(msgId);
                else playlistIds.add(msgId);
                adapter.notifyItemChanged(position);
            });

            content.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            return fragmentView;
        }

        private class AddTracksAdapter extends RecyclerListView.SelectionAdapter {
            private Context mContext;
            public AddTracksAdapter(Context context) { mContext = context; }

            @Override public boolean isEnabled(RecyclerView.ViewHolder holder) { return true; }
            @Override public int getItemCount() { return allTracks.size(); }

            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                SpotifyTrackCell cell = new SpotifyTrackCell(mContext);
                return new RecyclerListView.Holder(cell);
            }

            @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                SpotifyTrackCell cell = (SpotifyTrackCell) holder.itemView;
                MessageObject msg = allTracks.get(position);
                cell.setMessageObject(msg);

                int msgId = (msg.messageOwner != null && msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) ? msg.messageOwner.message.split("\\|\\|\\|")[3].hashCode() : msg.getId();
                boolean isAdded = playlistIds.contains(msgId);

                if (isAdded) {
                    cell.optionsButton.setImageResource(R.drawable.ic_ab_done);
                    cell.optionsButton.setColorFilter(0xFF1DB954, PorterDuff.Mode.SRC_IN);
                } else {
                    cell.optionsButton.setImageResource(R.drawable.msg_add);
                    cell.optionsButton.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.SRC_IN);
                }
                cell.optionsButton.setClickable(false);
            }
        }
    }

    public class PlaylistDetailFragment extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, ChatActivityInterface {
        private boolean isEditingMode = false;
        private File playlistFile;
        private long channelId;
        private ArrayList<MessageObject> playlistMessages = new ArrayList<>();
        private RecyclerListView listView;
        private PlaylistMusicAdapter adapter;
        private PlaylistUpdateListener listener;
        private long playlistUniqueId;
        private String currentPlaylistName;
        private final int PICK_IMAGE_REQUEST = 101;

        public PlaylistDetailFragment(File file, long channelId, PlaylistUpdateListener listener) {
            this.playlistFile = file;
            this.currentPlaylistName = file.getName().replace(".txt", "");
            this.channelId = channelId;
            this.listener = listener;
            this.playlistUniqueId = -(long) Math.abs(file.getName().hashCode());
        }

        @Override public long getDialogId() { return channelId; }
        @Override public long getTopicId() { return 0; }
        @Override public SizeNotifierFrameLayout getContentView() { return (SizeNotifierFrameLayout) fragmentView; }
        @Override public ChatAvatarContainer getAvatarContainer() { return null; }
        @Override public TLRPC.Chat getCurrentChat() { return MessagesController.getInstance(currentAccount).getChat(channelId); }
        @Override public TLRPC.User getCurrentUser() { return null; }

        @Override
        public boolean onFragmentCreate() {
            super.onFragmentCreate();
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.didReceiveNewMessages);
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidReset);
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidStart);
            loadPlaylistData();
            return true;
        }

        @Override
        public void onFragmentDestroy() {
            super.onFragmentDestroy();
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.didReceiveNewMessages);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidReset);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidStart);
        }

        @Override
        public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
            if (requestCode == PICK_IMAGE_REQUEST && resultCode == android.app.Activity.RESULT_OK && data != null) {
                try {
                    java.io.InputStream is = ApplicationLoader.applicationContext.getContentResolver().openInputStream(data.getData());
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
                    int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
                    android.graphics.Bitmap cropped = android.media.ThumbnailUtils.extractThumbnail(bitmap, size, size);

                    File coverFile = new File(getPlaylistsDir(), currentPlaylistName + "_cover.jpg");
                    java.io.FileOutputStream out = new java.io.FileOutputStream(coverFile);
                    cropped.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out);
                    out.close();

                    if (listener != null) listener.onPlaylistUpdated();
                    safeNotifyAdapter(adapter, listView);
                } catch (Exception e) {}
            }
        }

        @Override
        public View createView(Context context) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.setTitle("");
            actionBar.setOccupyStatusBar(true);

            ActionBarMenu menu = actionBar.createMenu();
            menu.addItem(1, R.drawable.msg_add);
            ActionBarMenuItem editItem = menu.addItem(2, R.drawable.msg_edit);
            menu.addItem(3, R.drawable.msg_delete);

            actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
            actionBar.setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle));
            actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false);
            actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false);
            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) finishFragment();
                    else if (id == 1) {
                        presentFragment(new AddTracksFragment(playlistFile, () -> loadPlaylistData()));
                    }
                    else if (id == 3) deletePlaylist();
                    else if (id == 4) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        getParentActivity().startActivityForResult(intent, PICK_IMAGE_REQUEST);
                    }
                    else if (id == 2) {
                        isEditingMode = !isEditingMode;
                        if (isEditingMode) {
                            editItem.setIcon(R.drawable.ic_ab_done);
                            Toast.makeText(context, getString(R.string.holdtrc), Toast.LENGTH_SHORT).show();
                            menu.addItem(4, R.drawable.msg_gallery_locked1);
                        } else {
                            editItem.setIcon(R.drawable.msg_edit);
                            savePlaylistOrder();
                            menu.setItemVisibility(4, 0);
                        }
                    }
                }
            });

            SizeNotifierFrameLayout content = new SizeNotifierFrameLayout(context) {
                @Override protected void onDraw(Canvas canvas) {
                    canvas.drawColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                }
            };
            fragmentView = content;

            listView = new RecyclerListView(context);
            listView.setLayoutManager(new LinearLayoutManager(context));
            adapter = new PlaylistMusicAdapter(context);
            listView.setAdapter(adapter);
            listView.setPadding(0, 0, 0, AndroidUtilities.dp(16));
            listView.setOnItemClickListener((view, position) -> {
                if (adapter.getItemViewType(position) == 2) return;
                if (position > 0 && position <= playlistMessages.size()) {
                    playStrictInPlaylist(playlistMessages.get(position - 1));
                }
            });

            content.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            MiniPlayerView miniPlayerView = new MiniPlayerView(context, this);
            content.addView(miniPlayerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 64, Gravity.BOTTOM));

            ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                @Override
                public boolean isLongPressDragEnabled() {
                    return isEditingMode;
                }

                @Override
                public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                    if (viewHolder.getItemViewType() == 2 || target.getItemViewType() == 2) return false;
                    int fromPos = viewHolder.getAdapterPosition() - 1;
                    int toPos = target.getAdapterPosition() - 1;
                    if (fromPos < 0 || toPos < 0 || fromPos >= playlistMessages.size() || toPos >= playlistMessages.size()) return false;

                    java.util.Collections.swap(playlistMessages, fromPos, toPos);
                    adapter.notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                    return true;
                }

                @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) { }

                @Override
                public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
                    savePlaylistOrder();
                }
            });

            touchHelper.attachToRecyclerView(listView);

            return fragmentView;
        }

        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == NotificationCenter.messagePlayingDidStart || id == NotificationCenter.messagePlayingPlayStateChanged || id == NotificationCenter.messagePlayingDidReset) {
                safeNotifyAdapter(adapter, listView);
            }
        }

        private void loadPlaylistData() {
            playlistMessages.clear();
            if (playlistFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(playlistFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("SG_STREAM|||")) {
                            String[] parts = line.split("\\|\\|\\|");
                            if (parts.length >= 5) {
                                int dur = 0; try { dur = Integer.parseInt(parts[4]); } catch (Exception e){}
                                playlistMessages.add(createWebMessageObject(currentAccount, parts[1], parts[2], parts[3], dur, parts[3].hashCode(), ""));
                            }
                        } else {
                            try {
                                int id = Integer.parseInt(line);
                                for (MessageObject msg : ChannelMusicFragment.audioMessages) {
                                    if (msg.getId() == id) { playlistMessages.add(msg); break; }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (IOException e) { e.printStackTrace(); }
            }
            safeNotifyAdapter(adapter, listView);
            if (listener != null) listener.onPlaylistUpdated();
        }

        private void playStrictInPlaylist(MessageObject messageObject) {
            MediaController mediaController = MediaController.getInstance();
            MessageObject currentPlaying = mediaController.getPlayingMessageObject();
            if (currentPlaying != null && currentPlaying.getId() == messageObject.getId()) {
                if (mediaController.isMessagePaused()) mediaController.playMessage(messageObject);
                else mediaController.pauseMessage(messageObject);
                return;
            }

            for (MessageObject m : playlistMessages) {
                m.audioProgress = 0.0f;
                m.audioProgressSec = 0;
            }

            mediaController.setPlaylist(new ArrayList<>(playlistMessages), messageObject, playlistUniqueId);
            mediaController.seekToProgress(messageObject, 0);
            mediaController.playMessage(messageObject);
        }

        private void deletePlaylist() {
            if (playlistFile.delete()) {
                File coverFile = new File(getPlaylistsDir(), currentPlaylistName + "_cover.jpg");
                if (coverFile.exists()) coverFile.delete();
                if (listener != null) listener.onPlaylistUpdated();
                finishFragment();
            }
        }

        private void showPlaylistTrackOptionsBottomSheet(BaseFragment fragment, MessageObject msg, Runnable onDeleteCallback) {
            Context context = fragment.getParentActivity();
            if (context == null) return;
            BottomSheet.Builder builder = new BottomSheet.Builder(context);

            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);

            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(Theme.getColor(Theme.key_dialogBackground));
            gd.setCornerRadii(new float[]{AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), 0, 0, 0, 0});
            container.setBackground(gd);
            container.setPadding(0, AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16));

            LinearLayout header = new LinearLayout(context);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), AndroidUtilities.dp(16));

            BackupImageView cover = new BackupImageView(context);
            cover.setRoundRadius(AndroidUtilities.dp(4));
            setAudioCover(cover, msg, "50_50", 4);

            LinearLayout textContainer = new LinearLayout(context);
            textContainer.setOrientation(LinearLayout.VERTICAL);
            textContainer.setPadding(AndroidUtilities.dp(16), 0, 0, 0);

            TextView title = new TextView(context);
            title.setText(msg.getMusicTitle());
            title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            title.setTextSize(16);
            title.setSingleLine(true);
            title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));

            TextView artist = new TextView(context);
            artist.setText(msg.getMusicAuthor());
            artist.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
            artist.setTextSize(14);
            artist.setSingleLine(true);

            textContainer.addView(title);
            textContainer.addView(artist);
            header.addView(cover, LayoutHelper.createLinear(48, 48));
            header.addView(textContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            container.addView(header);

            View divider = new View(context);
            divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
            container.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 0, 0, 8));

            LinearLayout deleteRow = new LinearLayout(context);
            deleteRow.setOrientation(LinearLayout.HORIZONTAL);
            deleteRow.setGravity(Gravity.CENTER_VERTICAL);
            deleteRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
            deleteRow.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 0));
            deleteRow.setOnClickListener(v -> {
                builder.getDismissRunnable().run();
                if (onDeleteCallback != null) onDeleteCallback.run();
            });

            ImageView deleteIcon = new ImageView(context);
            deleteIcon.setImageResource(R.drawable.msg_delete);
            deleteIcon.setColorFilter(Theme.getColor(Theme.key_dialogIcon), PorterDuff.Mode.SRC_IN);

            TextView deleteText = new TextView(context);
            deleteText.setText(getString(R.string.dlpl));
            deleteText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            deleteText.setTextSize(16);

            deleteRow.addView(deleteIcon, LayoutHelper.createLinear(24, 24));
            deleteRow.addView(deleteText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 16, 0, 0, 0));
            container.addView(deleteRow);

            builder.setCustomView(container);
            fragment.showDialog(builder.create());
        }

        public void savePlaylistOrder() {
            if (playlistFile == null) return;
            try (java.io.FileWriter fw = new java.io.FileWriter(playlistFile, false)) {
                for (MessageObject msg : playlistMessages) {
                    if (msg.messageOwner != null && msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) {
                        fw.write(msg.messageOwner.message + "\n");
                    } else {
                        fw.write(msg.getId() + "\n");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private class PlaylistMusicAdapter extends RecyclerListView.SelectionAdapter {
            private Context mContext;
            public PlaylistMusicAdapter(Context context) { mContext = context; }

            @Override public boolean isEnabled(RecyclerView.ViewHolder holder) { return holder.getItemViewType() == 1; }

            @Override public int getItemCount() { return playlistMessages.size() + 2; }

            @Override public int getItemViewType(int position) {
                if (position == 0) return 0;
                if (position == playlistMessages.size() + 1) return 2;
                return 1;
            }

            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                if (viewType == 2) {
                    View phantom = new View(mContext);
                    phantom.setLayoutParams(new RecyclerView.LayoutParams(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(70)));
                    return new RecyclerListView.Holder(phantom);
                }
                if (viewType == 0) {
                    FrameLayout headerContainer = new FrameLayout(mContext);
                    headerContainer.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                    BackupImageView bgCover = new BackupImageView(mContext);
                    headerContainer.addView(bgCover, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

                    View gradientView = new View(mContext);
                    int bgColor = Theme.getColor(Theme.key_windowBackgroundWhite);
                    int transparentBg = (bgColor & 0x00FFFFFF) | 0x44000000;
                    android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
                            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{transparentBg, bgColor});
                    gradientView.setBackground(gd);
                    headerContainer.addView(gradientView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

                    LinearLayout content = new LinearLayout(mContext);
                    content.setOrientation(LinearLayout.VERTICAL);
                    content.setGravity(Gravity.CENTER_HORIZONTAL);
                    content.setPadding(0, AndroidUtilities.dp(60), 0, AndroidUtilities.dp(24));

                    BackupImageView centerCover = new BackupImageView(mContext);
                    centerCover.setRoundRadius(AndroidUtilities.dp(8));
                    content.addView(centerCover, LayoutHelper.createLinear(200, 200, Gravity.CENTER_HORIZONTAL));

                    TextView title = new TextView(mContext);
                    title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
                    title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    title.setGravity(Gravity.CENTER);
                    content.addView(title, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 20, 0, 0));

                    TextView metaText = new TextView(mContext);
                    metaText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                    metaText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                    metaText.setGravity(Gravity.CENTER);
                    content.addView(metaText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 6, 0, 0));

                    LinearLayout buttonsLayout = new LinearLayout(mContext);
                    buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
                    buttonsLayout.setGravity(Gravity.CENTER);

                    TextView playButton = new TextView(mContext);
                    playButton.setText(getString(R.string.hear));
                    playButton.setTextColor(0xFFFFFFFF);
                    playButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                    playButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    playButton.setGravity(Gravity.CENTER);
                    playButton.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(24), 0xFF1DB954));
                    playButton.setPadding(AndroidUtilities.dp(40), 0, AndroidUtilities.dp(40), 0);
                    buttonsLayout.addView(playButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48, 0, 0, 16, 0));

                    ImageView shuffleButton = new ImageView(mContext);
                    shuffleButton.setImageResource(R.drawable.msg_retry);
                    shuffleButton.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), PorterDuff.Mode.SRC_IN);
                    shuffleButton.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(24), Theme.getColor(Theme.key_listSelector)));
                    shuffleButton.setScaleType(ImageView.ScaleType.CENTER);
                    buttonsLayout.addView(shuffleButton, LayoutHelper.createLinear(48, 48));

                    TextView waveButton = new TextView(mContext);
                    waveButton.setText(getString(R.string.mywave));
                    waveButton.setTextColor(0xFFFFFFFF);
                    waveButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                    waveButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    waveButton.setGravity(Gravity.CENTER);
                    waveButton.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(24), 0xFF8A2BE2));
                    waveButton.setPadding(AndroidUtilities.dp(20), 0, AndroidUtilities.dp(20), 0);
                    buttonsLayout.addView(waveButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48, 16, 0, 0, 0));

                    waveButton.setOnClickListener(v -> {
                        if (playlistMessages.isEmpty()) return;
                        Toast.makeText(mContext, getString(R.string.wvc), Toast.LENGTH_SHORT).show();

                        ArrayList<MessageObject> originalList = new ArrayList<>(playlistMessages);

                        // ИСПРАВЛЕНО ЗДЕСЬ (Заменен метод на актуальный)
                        SmartRadioEngine.generateWaveByPlaylist(originalList, currentAccount, new SmartRadioEngine.WaveCallback() {
                            @Override
                            public void onTracksFound(ArrayList<MessageObject> waveQueue) {
                                MediaController.getInstance().myWaveBasePlaylist.clear();
                                MediaController.getInstance().myWaveBasePlaylist.addAll(waveQueue);
                                playContext(waveQueue.get(0), MediaController.getInstance().myWaveBasePlaylist, MediaController.WAVE_CONTEXT_ID);
                            }

                            @Override
                            public void onMoreTracksAdded(ArrayList<MessageObject> tracks) {}

                            @Override
                            public void onError() {
                                Toast.makeText(mContext, getString(R.string.er), Toast.LENGTH_SHORT).show();
                            }
                        });
                    });

                    playButton.setOnClickListener(v -> {
                        if (!playlistMessages.isEmpty()) {
                            playStrictInPlaylist(playlistMessages.get(0));
                        }
                    });

                    shuffleButton.setOnClickListener(v -> {
                        if (!playlistMessages.isEmpty()) {
                            ArrayList<MessageObject> shuffled = new ArrayList<>(playlistMessages);
                            Collections.shuffle(shuffled);
                            MediaController mediaController = MediaController.getInstance();
                            mediaController.setPlaylist(shuffled, shuffled.get(0), playlistUniqueId);
                            mediaController.seekToProgress(shuffled.get(0), 0);
                            mediaController.playMessage(shuffled.get(0));
                        }
                    });

                    content.addView(buttonsLayout, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48, Gravity.CENTER_HORIZONTAL, 0, 20, 0, 0));
                    headerContainer.addView(content, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

                    return new RecyclerListView.Holder(headerContainer);
                }
                SpotifyTrackCell cell = new SpotifyTrackCell(mContext);
                return new RecyclerListView.Holder(cell);
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                if (holder.getItemViewType() == 2) return;

                if (holder.getItemViewType() == 0) {
                    FrameLayout headerContainer = (FrameLayout) holder.itemView;
                    BackupImageView bgCover = (BackupImageView) headerContainer.getChildAt(0);
                    LinearLayout content = (LinearLayout) headerContainer.getChildAt(2);
                    BackupImageView centerCover = (BackupImageView) content.getChildAt(0);
                    TextView title = (TextView) content.getChildAt(1);
                    TextView metaText = (TextView) content.getChildAt(2);

                    title.setText(currentPlaylistName);

                    int totalSeconds = 0;
                    for (MessageObject m : playlistMessages) {
                        totalSeconds += m.getDuration();
                    }
                    int minutes = totalSeconds / 60;
                    metaText.setText(playlistMessages.size() + getString(R.string.trck) + minutes + getString(R.string.min));

                    MessageObject coverMsg = null;
                    for (MessageObject m : playlistMessages) {
                        if (hasValidIcon(m)) {
                            coverMsg = m;
                            break;
                        }
                    }

                    setPlaylistCover(bgCover, currentPlaylistName, coverMsg, 0);
                    setPlaylistCover(centerCover, currentPlaylistName, coverMsg, 8);

                } else {
                    SpotifyTrackCell myCell = (SpotifyTrackCell) holder.itemView;
                    MessageObject msg = playlistMessages.get(position - 1);

                    myCell.setMessageObject(msg);

                    myCell.optionsButton.setOnClickListener(v -> {
                        showPlaylistTrackOptionsBottomSheet(PlaylistDetailFragment.this, msg, () -> {
                            playlistMessages.remove(msg);
                            savePlaylistOrder();
                            safeNotifyAdapter(this, listView);
                        });
                    });
                }
            }
        }
    }



    private class PlaylistsHorizontalAdapter extends RecyclerListView.SelectionAdapter {
        private Context mContext;
        public PlaylistsHorizontalAdapter(Context context) { mContext = context; }
        @Override public boolean isEnabled(RecyclerView.ViewHolder holder) { return true; }
        @Override public int getItemCount() { return playlistFiles.size() + 1; }
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout container = new LinearLayout(mContext);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setGravity(Gravity.CENTER_HORIZONTAL);
            container.setLayoutParams(new RecyclerView.LayoutParams(AndroidUtilities.dp(100), ViewGroup.LayoutParams.WRAP_CONTENT));
            ((RecyclerView.LayoutParams)container.getLayoutParams()).rightMargin = AndroidUtilities.dp(12);

            FrameLayout coverFrame = new FrameLayout(mContext);

            BackupImageView coverImage = new BackupImageView(mContext);
            coverImage.setRoundRadius(AndroidUtilities.dp(4));
            coverFrame.addView(coverImage, LayoutHelper.createFrame(100, 100));

            ImageView plusIcon = new ImageView(mContext);
            plusIcon.setImageResource(R.drawable.msg_add);
            plusIcon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.SRC_IN);
            plusIcon.setScaleType(ImageView.ScaleType.CENTER);
            plusIcon.setVisibility(View.GONE);
            coverFrame.addView(plusIcon, LayoutHelper.createFrame(40, 40, Gravity.CENTER));

            container.addView(coverFrame, LayoutHelper.createLinear(100, 100));

            TextView nameText = new TextView(mContext);
            nameText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            nameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            nameText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            nameText.setSingleLine(true);
            nameText.setEllipsize(TextUtils.TruncateAt.END);
            nameText.setGravity(Gravity.CENTER_HORIZONTAL);
            container.addView(nameText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 6, 0, 0));

            return new RecyclerListView.Holder(container);
        }
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            LinearLayout container = (LinearLayout) holder.itemView;
            FrameLayout frame = (FrameLayout) container.getChildAt(0);
            BackupImageView coverImage = (BackupImageView) frame.getChildAt(0);
            ImageView plusIcon = (ImageView) frame.getChildAt(1);
            TextView nameText = (TextView) container.getChildAt(1);

            if (position == 0) {
                coverImage.setImageDrawable(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_dialogBackground)));
                plusIcon.setVisibility(View.VISIBLE);
                nameText.setText(getString(R.string.create));
            } else {
                plusIcon.setVisibility(View.GONE);
                File pFile = playlistFiles.get(position - 1);
                String pName = pFile.getName().replace(".txt", "");
                nameText.setText(pName);

                MessageObject coverMsg = null;
                try (BufferedReader br = new BufferedReader(new FileReader(pFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("SG_STREAM|||")) continue; // пропускаем для обложки
                        int id = Integer.parseInt(line);
                        for (MessageObject msg : audioMessages) {
                            if (msg.getId() == id && hasValidIcon(msg)) {
                                coverMsg = msg;
                                break;
                            }
                        }
                        if (coverMsg != null) break;
                    }
                } catch (Exception e) {}

                setPlaylistCover(coverImage, pName, coverMsg, 8);
            }
        }
    }

    public static class SpotifyTrackCell extends FrameLayout {
        public BackupImageView coverImage;
        public TextView titleTextView;
        public TextView artistTextView;
        public ImageView optionsButton;
        public ImageView playingIndicator;
        private MessageObject currentMessageObject;

        public SpotifyTrackCell(Context context) {
            super(context);
            setLayoutParams(new RecyclerView.LayoutParams(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(64)));
            setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 0));

            FrameLayout coverFrame = new FrameLayout(context);
            coverImage = new BackupImageView(context);
            coverImage.setRoundRadius(AndroidUtilities.dp(4));
            coverFrame.addView(coverImage, LayoutHelper.createFrame(48, 48));

            playingIndicator = new ImageView(context);
            playingIndicator.setScaleType(ImageView.ScaleType.CENTER);
            playingIndicator.setColorFilter(0xFF1DB954, PorterDuff.Mode.SRC_IN);
            playingIndicator.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(4), 0x99000000));
            playingIndicator.setVisibility(View.GONE);
            coverFrame.addView(playingIndicator, LayoutHelper.createFrame(48, 48));

            addView(coverFrame, LayoutHelper.createFrame(48, 48, Gravity.LEFT | Gravity.CENTER_VERTICAL, 16, 0, 0, 0));

            LinearLayout textLayout = new LinearLayout(context);
            textLayout.setOrientation(LinearLayout.VERTICAL);

            titleTextView = new TextView(context);
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            titleTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            titleTextView.setSingleLine(true);
            titleTextView.setEllipsize(TextUtils.TruncateAt.END);

            artistTextView = new TextView(context);
            artistTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            artistTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            artistTextView.setSingleLine(true);
            artistTextView.setEllipsize(TextUtils.TruncateAt.END);

            textLayout.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            textLayout.addView(artistTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 76, 0, 48, 0));

            optionsButton = new ImageView(context);
            optionsButton.setImageResource(R.drawable.ic_ab_other);
            optionsButton.setScaleType(ImageView.ScaleType.CENTER);
            optionsButton.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.SRC_IN);
            optionsButton.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
            addView(optionsButton, LayoutHelper.createFrame(48, 48, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 4, 0));
        }

        public void setMessageObject(MessageObject msg) {
            this.currentMessageObject = msg;
            if (msg == null) return;
            titleTextView.setText(msg.getMusicTitle());
            artistTextView.setText(msg.getMusicAuthor());

            MessageObject playingMsg = MediaController.getInstance().getPlayingMessageObject();
            boolean isPlaying = playingMsg != null && playingMsg.getId() == msg.getId() && msg.getId() != 0;

            if (isPlaying) {
                titleTextView.setTextColor(0xFF1DB954);
                playingIndicator.setVisibility(View.VISIBLE);
            } else {
                titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                playingIndicator.setVisibility(View.GONE);
            }

            setAudioCover(coverImage, msg, "50_50", 4);
        }
    }

    private class MusicAdapter extends RecyclerListView.SelectionAdapter {
        private Context mContext;
        private ArrayList<MessageObject> customList;
        private static final int TYPE_WEB_TRACK = 0;
        private static final int TYPE_LOCAL_TRACK = 1;

        public MusicAdapter(Context context, ArrayList<MessageObject> list) {
            mContext = context;
            this.customList = list;
        }

        @Override public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() != 2;
        }

        @Override public int getItemCount() {
            int baseSize = customList != null ? customList.size() : (isSearching ? webResultTracks.size() + searchResultMessages.size() : audioMessages.size());
            return baseSize + 1;
        }

        @Override public int getItemViewType(int position) {
            int normalSize = customList != null ? customList.size() : (isSearching ? webResultTracks.size() + searchResultMessages.size() : audioMessages.size());
            if (position == normalSize) return 2;

            if (customList != null) return TYPE_LOCAL_TRACK;
            return (isSearching && position < webResultTracks.size()) ? TYPE_WEB_TRACK : TYPE_LOCAL_TRACK;
        }

        @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == 2) {
                View phantom = new View(mContext);
                phantom.setLayoutParams(new RecyclerView.LayoutParams(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(70)));
                return new RecyclerListView.Holder(phantom);
            }
            return new RecyclerListView.Holder(new SpotifyTrackCell(mContext));
        }

        @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == 2) return;

            SpotifyTrackCell myCell = (SpotifyTrackCell) holder.itemView;

            if (getItemViewType(position) == TYPE_WEB_TRACK) {
                myCell.optionsButton.setVisibility(View.VISIBLE); // Теперь кнопка видна
                MusicWebSearcher.WebTrack track = webResultTracks.get(position);
                int durSec = parseDuration(track.duration);
                MessageObject fakeObj = createWebMessageObject(currentAccount, track.title, track.artist, track.downloadUrl, durSec, -(long)track.downloadUrl.hashCode(), "");
                myCell.setMessageObject(fakeObj);

                myCell.optionsButton.setOnClickListener(v -> {
                    showWebTrackOptionsBottomSheet(ChannelMusicFragment.this, track);
                });
            } else {
                myCell.optionsButton.setVisibility(View.VISIBLE);
                int idx = (isSearching && customList == null) ? position - webResultTracks.size() : position;
                ArrayList<MessageObject> list = customList != null ? customList : (isSearching ? searchResultMessages : audioMessages);
                MessageObject msg = list.get(idx);
                myCell.setMessageObject(msg);

                myCell.optionsButton.setOnClickListener(v -> {
                    showTrackOptionsBottomSheet(ChannelMusicFragment.this, msg, false, () -> deleteTrackFromChannel(msg));
                });
            }
        }
    }

    private void showWebTrackOptionsBottomSheet(BaseFragment fragment, MusicWebSearcher.WebTrack track) {
        Context context = fragment.getParentActivity();
        if (context == null) return;
        BottomSheet.Builder builder = new BottomSheet.Builder(context);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Theme.getColor(Theme.key_dialogBackground));
        gd.setCornerRadii(new float[]{AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), 0, 0, 0, 0});
        container.setBackground(gd);
        container.setPadding(0, AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16));

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        BackupImageView cover = new BackupImageView(context);
        cover.setRoundRadius(AndroidUtilities.dp(4));
        setAudioCover(cover, null, "50_50", 4);

        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setPadding(AndroidUtilities.dp(16), 0, 0, 0);

        TextView title = new TextView(context);
        title.setText(track.title);
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setTextSize(16);
        title.setSingleLine(true);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));

        TextView artist = new TextView(context);
        artist.setText(track.artist);
        artist.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        artist.setTextSize(14);
        artist.setSingleLine(true);

        textContainer.addView(title);
        textContainer.addView(artist);
        header.addView(cover, LayoutHelper.createLinear(48, 48));
        header.addView(textContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        container.addView(header);

        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        container.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 0, 0, 8));

        container.addView(createMenuOption(context, getString(R.string.addlb), R.drawable.msg_add, () -> {
            builder.getDismissRunnable().run();
            saveWebTrackToLibrary(track);
        }));

        container.addView(createMenuOption(context, getString(R.string.dwch), R.drawable.msg_download, () -> {
            builder.getDismissRunnable().run();
            downloadAndUploadTrack(track);
        }));

        builder.setCustomView(container);
        fragment.showDialog(builder.create());
    }

    private void showTrackOptionsBottomSheet(BaseFragment fragment, MessageObject msg, boolean isInsidePlaylist, Runnable onDeleteCallback) {
        Context context = fragment.getParentActivity();
        if (context == null) return;
        BottomSheet.Builder builder = new BottomSheet.Builder(context);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Theme.getColor(Theme.key_dialogBackground));
        gd.setCornerRadii(new float[]{AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), 0, 0, 0, 0});
        container.setBackground(gd);
        container.setPadding(0, AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16));

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        BackupImageView cover = new BackupImageView(context);
        cover.setRoundRadius(AndroidUtilities.dp(4));
        setAudioCover(cover, msg, "50_50", 4);

        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setPadding(AndroidUtilities.dp(16), 0, 0, 0);

        TextView title = new TextView(context);
        title.setText(msg.getMusicTitle());
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setTextSize(16);
        title.setSingleLine(true);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));

        TextView artist = new TextView(context);
        artist.setText(msg.getMusicAuthor());
        artist.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        artist.setTextSize(14);
        artist.setSingleLine(true);

        textContainer.addView(title);
        textContainer.addView(artist);
        header.addView(cover, LayoutHelper.createLinear(48, 48));
        header.addView(textContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        container.addView(header);

        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        container.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 0, 0, 8));

        if (!isInsidePlaylist) {
            container.addView(createMenuOption(context, getString(R.string.addpl), R.drawable.msg_add, () -> {
                builder.getDismissRunnable().run();
                showSelectPlaylistBottomSheet(fragment, msg);
            }));
        }

        String deleteText = isInsidePlaylist ? getString(R.string.dlpl) : getString(R.string.dll);
        container.addView(createMenuOption(context, deleteText, R.drawable.msg_delete, () -> {
            builder.getDismissRunnable().run();
            if (onDeleteCallback != null) onDeleteCallback.run();
        }));

        builder.setCustomView(container);
        fragment.showDialog(builder.create());
    }

    private void showSelectPlaylistBottomSheet(BaseFragment fragment, MessageObject msg) {
        Context context = fragment.getParentActivity();
        if (context == null || playlistFiles.isEmpty()) return;

        BottomSheet.Builder builder = new BottomSheet.Builder(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Theme.getColor(Theme.key_dialogBackground));
        gd.setCornerRadii(new float[]{AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), 0, 0, 0, 0});
        container.setBackground(gd);

        TextView title = new TextView(context);
        title.setText(getString(R.string.addpl));
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setTextSize(18);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16));
        container.addView(title);

        for (File pFile : playlistFiles) {
            String name = pFile.getName().replace(".txt", "");
            container.addView(createMenuOption(context, name, R.drawable.ic_ab_other, () -> {
                ArrayList<String> existingLines = new ArrayList<>();
                if (pFile.exists()) {
                    try (BufferedReader br = new BufferedReader(new FileReader(pFile))) {
                        String line;
                        while ((line = br.readLine()) != null) existingLines.add(line);
                    } catch (IOException e) { e.printStackTrace(); }
                }

                try (BufferedWriter bw = new BufferedWriter(new FileWriter(pFile, false))) {
                    if (msg.messageOwner != null && msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) {
                        bw.write(msg.messageOwner.message);
                    } else {
                        bw.write(String.valueOf(msg.getId()));
                    }
                    bw.newLine();
                    for (String oldLine : existingLines) {
                        bw.write(oldLine);
                        bw.newLine();
                    }
                    Toast.makeText(context, getString(R.string.addin) + name, Toast.LENGTH_SHORT).show();
                    safeNotifyAdapter(playlistsAdapter, playlistsListView);
                } catch (IOException e) { e.printStackTrace(); }
                builder.getDismissRunnable().run();
            }));
        }
        builder.setCustomView(container);
        fragment.showDialog(builder.create());
    }


    private View createMenuOption(Context context, String text, int iconRes, Runnable onClick) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 0));
        row.setOnClickListener(v -> onClick.run());

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Theme.getColor(Theme.key_dialogIcon), PorterDuff.Mode.SRC_IN);

        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        textView.setTextSize(16);

        row.addView(icon, LayoutHelper.createLinear(24, 24));
        row.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 16, 0, 0, 0));
        return row;
    }



    private void showImportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString(R.string.imm));
        LinearLayout container = new LinearLayout(getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(6), AndroidUtilities.dp(24), 0);
        final EditText input = new EditText(getParentActivity());
        input.setHint(getString(R.string.chus));
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        input.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        input.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(4), Theme.getColor(Theme.key_windowBackgroundGray)));
        input.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
        container.addView(input, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setView(container);
        builder.setPositiveButton(getString(R.string.src), (dialog, which) -> {
            String query = input.getText().toString().replace("https://t.me/", "").replace("@", "").trim();
            if (!TextUtils.isEmpty(query)) searchChannelAndFetchMusic(query);
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void searchChannelAndFetchMusic(String username) {
        TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
        req.username = username;
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (response instanceof TLRPC.TL_contacts_resolvedPeer) {
                TLRPC.TL_contacts_resolvedPeer peer = (TLRPC.TL_contacts_resolvedPeer) response;
                if (!peer.chats.isEmpty()) {
                    fetchMusicFromResolvedChannel(peer.chats.get(0).id, ((TLRPC.TL_channel) peer.chats.get(0)).access_hash);
                } else {
                    AndroidUtilities.runOnUIThread(() -> Toast.makeText(getParentActivity(), "Channel not found", Toast.LENGTH_SHORT).show());
                    checkOrCreateMusicChannel();
                }
            }
        });
    }

    private void fetchMusicFromResolvedChannel(long sourceChannelId, long accessHash) {
        TLRPC.TL_messages_search searchReq = new TLRPC.TL_messages_search();
        TLRPC.TL_inputPeerChannel inputPeer = new TLRPC.TL_inputPeerChannel();
        inputPeer.channel_id = sourceChannelId; inputPeer.access_hash = accessHash;
        searchReq.peer = inputPeer; searchReq.filter = new TLRPC.TL_inputMessagesFilterMusic();
        searchReq.q = ""; searchReq.limit = 100;

        ConnectionsManager.getInstance(currentAccount).sendRequest(searchReq, (response, error) -> {
            if (response instanceof TLRPC.messages_Messages) {
                TLRPC.messages_Messages messages = (TLRPC.messages_Messages) response;
                ArrayList<MessageObject> importedTracks = new ArrayList<>();
                for (TLRPC.Message msg : messages.messages) {
                    MessageObject messageObject = new MessageObject(currentAccount, msg, false, false);
                    if (messageObject.isMusic()) importedTracks.add(messageObject);
                }
                AndroidUtilities.runOnUIThread(() -> showImportSelectionSheet(importedTracks));
            } else AndroidUtilities.runOnUIThread(() -> Toast.makeText(getParentActivity(), "No tracks found", Toast.LENGTH_SHORT).show());
        });
    }

    private void showImportSelectionSheet(ArrayList<MessageObject> availableTracks) {
        if (availableTracks.isEmpty()) return;
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        builder.setTitle(getString(R.string.ims), true);
        LinearLayout container = new LinearLayout(getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Theme.getColor(Theme.key_dialogBackground));
        gd.setCornerRadii(new float[]{AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), 0, 0, 0, 0});
        container.setBackground(gd);

        HashSet<Integer> selectedIds = new HashSet<>();
        TextView importButton = new TextView(getParentActivity());
        importButton.setText(getString(R.string.imports));
        importButton.setGravity(Gravity.CENTER);
        importButton.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(6), Theme.getColor(Theme.key_featuredStickers_addButton)));
        importButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));

        RecyclerListView sheetList = new RecyclerListView(getParentActivity());
        sheetList.setLayoutManager(new LinearLayoutManager(getParentActivity()));
        sheetList.setAdapter(new RecyclerListView.SelectionAdapter() {
            @Override public boolean isEnabled(RecyclerView.ViewHolder holder) { return true; }
            @Override public int getItemCount() { return availableTracks.size(); }
            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new RecyclerListView.Holder(new CheckBoxCell(getParentActivity(), 1, null));
            }
            @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                MessageObject msg = availableTracks.get(position);
                ((CheckBoxCell) holder.itemView).setText(msg.getMusicTitle() + " - " + msg.getMusicAuthor(), "", selectedIds.contains(msg.getId()), true);
            }
        });
        sheetList.setOnItemClickListener((view, position) -> {
            MessageObject msg = availableTracks.get(position);
            if (selectedIds.contains(msg.getId())) selectedIds.remove(msg.getId());
            else selectedIds.add(msg.getId());
            ((CheckBoxCell) view).setChecked(selectedIds.contains(msg.getId()), true);
            importButton.setText(selectedIds.isEmpty() ? getString(R.string.imports) : getString(R.string.im) + selectedIds.size() + ")");
        });
        importButton.setOnClickListener(v -> {
            if (!selectedIds.isEmpty()) {
                ArrayList<MessageObject> messagesToForward = new ArrayList<>();
                for (MessageObject msg : availableTracks) if (selectedIds.contains(msg.getId())) messagesToForward.add(msg);
                long rawId = getChannelIdFromFile();
                if (rawId != 0) {
                    SendMessagesHelper.getInstance(currentAccount).sendMessage(messagesToForward, -rawId, false, true, false, 0, 0);
                    countBeforeUpload = audioMessages.size();
                    isWaitingForUpload = true;
                    AndroidUtilities.runOnUIThread(pollMessagesRunnable, 1000);
                    builder.getDismissRunnable().run();
                }
            }
        });
        container.addView(sheetList, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 300, 1.0f));
        container.addView(importButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 16, 16, 16, 16));
        builder.setCustomView(container);
        showDialog(builder.create());
    }

    public static void setPlaylistCover(BackupImageView coverImage, String playlistName, MessageObject defaultMsg, int radius) {
        File coverFile = new File(getPlaylistsDir(), playlistName + "_cover.jpg");
        if (coverFile.exists()) {
            coverImage.setRoundRadius(AndroidUtilities.dp(radius));
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeFile(coverFile.getAbsolutePath());
            if (bmp != null) {
                coverImage.setImageBitmap(bmp);
                return;
            }
        }
        setAudioCover(coverImage, defaultMsg, "100_100_b", radius);
    }


    private boolean hasValidIcon(MessageObject m) {
        if (m == null) return false;

        if (m.photoThumbs != null) {
            for (TLRPC.PhotoSize p : m.photoThumbs) {
                if (!(p instanceof TLRPC.TL_photoSizeEmpty)) return true;
            }
        }

        if (m.getDocument() != null && m.getDocument().thumbs != null) {
            for (TLRPC.PhotoSize p : m.getDocument().thumbs) {
                if (!(p instanceof TLRPC.TL_photoSizeEmpty)) return true;
            }
        }

        return !TextUtils.isEmpty(m.getArtworkUrl(true));
    }

    public class MiniPlayerView extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {
        private TextView titleTextView;
        private TextView artistTextView;
        private BackupImageView coverImage;
        private ImageView playPauseBtn;
        private Context parentContext;

        public MiniPlayerView(Context context, BaseFragment fragment) {
            super(context);
            this.parentContext = context;

            // Плавающий дизайн
            setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_dialogBackground)));
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                setElevation(AndroidUtilities.dp(4));
            }
            setClickable(true);


            coverImage = new BackupImageView(context);
            coverImage.setRoundRadius(AndroidUtilities.dp(4));
            addView(coverImage, LayoutHelper.createFrame(48, 48, Gravity.LEFT | Gravity.CENTER_VERTICAL, 16, 0, 0, 0));

            LinearLayout textLayout = new LinearLayout(context);
            textLayout.setOrientation(LinearLayout.VERTICAL);

            titleTextView = new TextView(context);
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            titleTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            titleTextView.setSingleLine(true);
            titleTextView.setEllipsize(TextUtils.TruncateAt.END);

            artistTextView = new TextView(context);
            artistTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            artistTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            artistTextView.setSingleLine(true);
            artistTextView.setEllipsize(TextUtils.TruncateAt.END);

            textLayout.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            textLayout.addView(artistTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 76, 0, 144, 0));

            ImageView prevBtn = new ImageView(context);
            prevBtn.setImageResource(R.drawable.baseline_skip_previous_24_white);
            prevBtn.setScaleType(ImageView.ScaleType.CENTER);
            prevBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), PorterDuff.Mode.SRC_IN);
            prevBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
            prevBtn.setOnClickListener(v -> MediaController.getInstance().playPreviousMessage());
            addView(prevBtn, LayoutHelper.createFrame(48, 48, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 96, 0));

            playPauseBtn = new ImageView(context);
            playPauseBtn.setScaleType(ImageView.ScaleType.CENTER);
            playPauseBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), PorterDuff.Mode.SRC_IN);
            playPauseBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
            playPauseBtn.setOnClickListener(v -> {
                MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
                if (playing != null) {
                    if (MediaController.getInstance().isMessagePaused()) MediaController.getInstance().playMessage(playing);
                    else MediaController.getInstance().pauseMessage(playing);
                }
            });
            addView(playPauseBtn, LayoutHelper.createFrame(48, 48, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 48, 0));

            ImageView nextBtn = new ImageView(context);
            nextBtn.setImageResource(R.drawable.baseline_skip_next_24_white);
            nextBtn.setScaleType(ImageView.ScaleType.CENTER);
            nextBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), PorterDuff.Mode.SRC_IN);
            nextBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
            nextBtn.setOnClickListener(v -> MediaController.getInstance().playNextMessage());
            addView(nextBtn, LayoutHelper.createFrame(48, 48, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

            setOnClickListener(v -> {
                try {
                    // ВЫЗОВ ДИАЛОГА ЧЕРЕЗ АКТИВНЫЙ ФРАГМЕНТ
                    if (fragment.getParentActivity() != null) {
                        AudioPlayerAlert alert = new AudioPlayerAlert(fragment.getParentActivity(), fragment.getResourceProvider());
                        fragment.showDialog(alert);
                    }
                } catch (Exception e) {
                    FileLog.e("AudioPlayerAlert err: " + e);
                }
            });
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidReset);
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidStart);
            updateView();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidReset);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidStart);
        }

        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == NotificationCenter.messagePlayingDidReset || id == NotificationCenter.messagePlayingPlayStateChanged || id == NotificationCenter.messagePlayingDidStart) {
                updateView();
            }
        }

        private void updateView() {
            MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
            if (playing == null || (!playing.isMusic() && playing.messageOwner.attachPath == null)) {

                setVisibility(View.GONE);
                return;
            }
            setVisibility(View.VISIBLE);
            titleTextView.setText(playing.getMusicTitle());
            artistTextView.setText(playing.getMusicAuthor());
            setAudioCover(coverImage, playing, "50_50", 4);

            if (MediaController.getInstance().isMessagePaused()) {
                playPauseBtn.setImageResource(R.drawable.ic_play);
            } else {
                playPauseBtn.setImageResource(R.drawable.ic_pause);
            }
        }
    }
    private void filterMusic(String query) {
        searchResultMessages.clear();
        webResultTracks.clear();

        // Отрезаем пробелы в начале и в конце (" track " -> "track")
        String trimmedQuery = query.trim();

        // Ищем только если введено хотя бы 2 символа (иначе Hitmos выдает популярный мусор)
        if (!TextUtils.isEmpty(trimmedQuery) && trimmedQuery.length() >= 2) {
            if (tabStrip != null) tabStrip.setVisibility(View.GONE);
            if (libraryListHeader != null) libraryListHeader.setVisibility(View.GONE);
            if (viewPager != null && viewPager.getCurrentItem() != 1) viewPager.setCurrentItem(1, false);

            String lower = trimmedQuery.toLowerCase();
            for (MessageObject msg : audioMessages) {
                if (msg.getMusicTitle().toLowerCase().contains(lower) || msg.getMusicAuthor().toLowerCase().contains(lower)) {
                    searchResultMessages.add(msg);
                }
            }

            MusicWebSearcher.search(trimmedQuery, (tracks, artists, albums) -> {
                webResultTracks.clear();
                if (tracks != null) webResultTracks.addAll(tracks);

                ArrayList<MusicWebSearcher.WebArtist> validArtists = new ArrayList<>();
                if (artists != null) {
                    for (MusicWebSearcher.WebArtist a : artists) {
                        // Жесткий фильтр: имя не пустое, не Unknown и длиннее 1 символа
                        if (a.name != null && !a.name.trim().isEmpty() && !a.name.equalsIgnoreCase("Unknown") && a.name.length() > 1) {
                            validArtists.add(a);
                        }
                    }
                }

                ArrayList<MusicWebSearcher.WebAlbum> validAlbums = new ArrayList<>();
                if (albums != null) {
                    for (MusicWebSearcher.WebAlbum a : albums) {
                        // Жесткий фильтр для альбомов
                        if (a.title != null && !a.title.trim().isEmpty() && !a.title.equalsIgnoreCase("Unknown") && a.title.length() > 1) {
                            validAlbums.add(a);
                        }
                    }
                }

                if (artistAdapter != null) artistAdapter.setArtists(validArtists);
                if (albumsAdapter != null) albumsAdapter.setAlbums(validAlbums);

                LinearLayout.LayoutParams artistLP = new LinearLayout.LayoutParams(LayoutHelper.MATCH_PARENT, validArtists.isEmpty() ? 0 : LayoutHelper.WRAP_CONTENT);
                artistLP.setMargins(0, 0, 0, 0); // Обнуляем маргины
                artistRecyclerView.setLayoutParams(artistLP);

                LinearLayout.LayoutParams albumsLP = new LinearLayout.LayoutParams(LayoutHelper.MATCH_PARENT, validAlbums.isEmpty() ? 0 : LayoutHelper.WRAP_CONTENT);
                albumsLP.setMargins(0, 0, 0, 0); // Обнуляем маргины
                albumsRecyclerView.setLayoutParams(albumsLP);

                artistRecyclerView.setVisibility(validArtists.isEmpty() ? View.GONE : View.VISIBLE);
                albumsRecyclerView.setVisibility(validAlbums.isEmpty() ? View.GONE : View.VISIBLE);

                safeNotifyAdapter(adapter, listView);
            });

        } else {
            // Если запрос пустой или 1 символ — скрываем всю выдачу и возвращаем табы
            if (tabStrip != null) tabStrip.setVisibility(View.VISIBLE);
            if (libraryListHeader != null) libraryListHeader.setVisibility(View.VISIBLE);

            artistRecyclerView.setVisibility(View.GONE);
            albumsRecyclerView.setVisibility(View.GONE);

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LayoutHelper.MATCH_PARENT, 0);
            p.setMargins(0, 0, 0, 0);
            artistRecyclerView.setLayoutParams(p);
            albumsRecyclerView.setLayoutParams(p);

            if (artistAdapter != null) artistAdapter.setArtists(new ArrayList<>());
            if (albumsAdapter != null) albumsAdapter.setAlbums(new ArrayList<>());

            safeNotifyAdapter(adapter, listView);
        }
    }

    public class ArtistProfileFragment extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
        private String artistName;
        private String artistUrl;
        private String bannerUrl;

        private LinearLayout contentLayout;
        private LinearLayout albumsContainer;
        private LinearLayout tracksContainer;
        private TextView albumsTitle;
        private HorizontalScrollView albumsScroll;
        private ArrayList<MessageObject> queue = new ArrayList<>();
        private BackupImageView bannerView;

        public ArtistProfileFragment(String name, String url, String banner) {
            this.artistName = name;
            this.artistUrl = url;
            this.bannerUrl = banner;
        }

        @Override
        public boolean onFragmentCreate() {
            super.onFragmentCreate();
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidStart);
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
            return true;
        }

        @Override
        public void onFragmentDestroy() {
            super.onFragmentDestroy();
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidStart);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        }

        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == NotificationCenter.messagePlayingDidStart || id == NotificationCenter.messagePlayingPlayStateChanged) {
                updatePlayingTrackColor();
            }
        }

        private void updatePlayingTrackColor() {
            if (tracksContainer == null || queue.isEmpty()) return;
            MessageObject playing = MediaController.getInstance().getPlayingMessageObject();

            for (int i = 0; i < tracksContainer.getChildCount(); i++) {
                View v = tracksContainer.getChildAt(i);
                if (v instanceof LinearLayout) {
                    LinearLayout trackRow = (LinearLayout) v;
                    LinearLayout textContainer = (LinearLayout) trackRow.getChildAt(2);
                    TextView titleTxt = (TextView) textContainer.getChildAt(0);

                    if (i < queue.size()) {
                        MessageObject rowMsg = queue.get(i);
                        if (playing != null && playing.getId() == rowMsg.getId() && playing.getId() != 0) {
                            titleTxt.setTextColor(0xFF1DB954);
                        } else {
                            titleTxt.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                        }
                    }
                }
            }
        }

        @Override
        public View createView(Context context) {
            actionBar.setBackButtonImage(org.telegram.messenger.R.drawable.ic_ab_back);
            actionBar.setAllowOverlayTitle(false);
            actionBar.setTitle("");
            actionBar.setBackgroundColor(Color.TRANSPARENT);
            actionBar.setItemsColor(0xFFFFFFFF, false);
            actionBar.setActionBarMenuOnItemClick(new org.telegram.ui.ActionBar.ActionBar.ActionBarMenuOnItemClick() {
                @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
            });

            FrameLayout rootFrame = new FrameLayout(context) {
                @Override protected void onDraw(Canvas canvas) {
                    canvas.drawColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                }
            };
            fragmentView = rootFrame;

            ScrollView scrollView = new ScrollView(context);
            scrollView.setVerticalScrollBarEnabled(false);
            scrollView.setClipToPadding(false);
            scrollView.setPadding(0, 0, 0, AndroidUtilities.dp(70));
            rootFrame.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            contentLayout = new LinearLayout(context);
            contentLayout.setOrientation(LinearLayout.VERTICAL);
            scrollView.addView(contentLayout, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

            FrameLayout headerFrame = new FrameLayout(context);
            bannerView = new BackupImageView(context);
            bannerView.setImage(bannerUrl, "600_600", null);
            headerFrame.addView(bannerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 320));

            MusicWebSearcher.fetchArtistPictureFromDeezer(artistName, url -> {
                if (url != null) bannerView.setImage(url, "600_600", null);
            });

            View gradientView = new View(context);
            int bgColor = Theme.getColor(Theme.key_windowBackgroundWhite);
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, new int[]{(bgColor & 0x00FFFFFF), bgColor});
            gradientView.setBackground(gd);
            headerFrame.addView(gradientView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 320));

            TextView nameText = new TextView(context);
            nameText.setText(artistName);
            nameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 42);
            nameText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            nameText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            nameText.setShadowLayer(4, 0, 2, 0x88000000);
            headerFrame.addView(nameText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 16, 0, 16, 16));

            contentLayout.addView(headerFrame, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 320));

            FrameLayout actionsFrame = new FrameLayout(context);
            ImageView playButton = new ImageView(context);
            playButton.setImageResource(org.telegram.messenger.R.drawable.ic_play);
            playButton.setColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.SRC_IN);
            playButton.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(28), 0xFF1DB954));
            playButton.setScaleType(ImageView.ScaleType.CENTER);
            playButton.setOnClickListener(v -> {
                if (!queue.isEmpty()) {
                    MediaController.getInstance().setPlaylist(queue, queue.get(0), -(long)"artist_context".hashCode());
                    MediaController.getInstance().seekToProgress(queue.get(0), 0);
                    MediaController.getInstance().playMessage(queue.get(0));
                } else Toast.makeText(context, "Треки загружаются...", Toast.LENGTH_SHORT).show();
            });
            actionsFrame.addView(playButton, LayoutHelper.createFrame(56, 56, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 16, 16, 16));
            contentLayout.addView(actionsFrame, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            albumsTitle = new TextView(context);
            albumsTitle.setText("Популярные релизы");
            albumsTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            albumsTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            albumsTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            albumsTitle.setVisibility(View.GONE);
            contentLayout.addView(albumsTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 16, 8, 16, 12));

            albumsScroll = new HorizontalScrollView(context);
            albumsScroll.setHorizontalScrollBarEnabled(false);
            albumsScroll.setClipToPadding(false);
            albumsScroll.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), 0);
            albumsScroll.setVisibility(View.GONE);

            albumsContainer = new LinearLayout(context);
            albumsContainer.setOrientation(LinearLayout.HORIZONTAL);
            albumsScroll.addView(albumsContainer, LayoutHelper.createScroll(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER_VERTICAL));
            contentLayout.addView(albumsScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 180, 0, 0, 0, 16));

            TextView tracksTitle = new TextView(context);
            tracksTitle.setText("Популярные треки");
            tracksTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            tracksTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            tracksTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            contentLayout.addView(tracksTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 16, 16, 16, 8));

            // --- ШИРОКАЯ КНОПКА ВОЛНЫ ---
            TextView waveBtn = ChannelMusicFragment.createYandexWaveButton(context, "🔮 Моя Волна по исполнителю", v -> {
                Toast.makeText(context, "Генерация волны...", Toast.LENGTH_SHORT).show();
                SmartRadioEngine.generateWaveByArtist(artistName, currentAccount, new SmartRadioEngine.WaveCallback() {
                    @Override
                    public void onTracksFound(ArrayList<MessageObject> tracks) {
                        MediaController.getInstance().setPlaylist(tracks, tracks.get(0), 0);
                        MediaController.getInstance().playMessage(tracks.get(0));
                    }
                    @Override public void onMoreTracksAdded(ArrayList<MessageObject> tracks) {
                        MediaController.getInstance().addTracksToPlaylist(tracks);
                    }
                    @Override public void onError() { Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show(); }
                });
            });
            contentLayout.addView(waveBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16, 8, 16, 16));

            tracksContainer = new LinearLayout(context);
            tracksContainer.setOrientation(LinearLayout.VERTICAL);
            contentLayout.addView(tracksContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            ChannelMusicFragment.MiniPlayerView miniPlayerView = new ChannelMusicFragment.MiniPlayerView(context, this);
            rootFrame.addView(miniPlayerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 64, Gravity.BOTTOM));

            loadData(context);
            return fragmentView;
        }

        private void loadData(Context context) {
            // Загружаем альбомы ТОЛЬКО из Deezer (чтобы не было чужих)
            MusicWebSearcher.loadArtistAlbumsViaDeezer(artistName, new MusicWebSearcher.ArtistProfileCallback() {
                @Override
                public void onResult(ArrayList<MusicWebSearcher.WebAlbum> albums, ArrayList<MusicWebSearcher.WebTrack> dummy) {
                    if (albums.isEmpty()) return;

                    albumsTitle.setVisibility(View.VISIBLE);
                    albumsScroll.setVisibility(View.VISIBLE);
                    albumsContainer.removeAllViews();

                    for (MusicWebSearcher.WebAlbum album : albums) {
                        LinearLayout albumCell = new LinearLayout(context);
                        albumCell.setOrientation(LinearLayout.VERTICAL);
                        albumCell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 0));
                        albumCell.setPadding(0, 0, AndroidUtilities.dp(16), 0);

                        BackupImageView cover = new BackupImageView(context);
                        cover.setRoundRadius(AndroidUtilities.dp(8));
                        cover.setImage(album.coverUrl, "150_150", null);
                        albumCell.addView(cover, LayoutHelper.createLinear(130, 130, 0, 0, 0, 8));

                        TextView title = new TextView(context);
                        title.setText(album.title);
                        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                        title.setMaxLines(2);
                        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
                        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                        albumCell.addView(title, LayoutHelper.createLinear(130, LayoutHelper.WRAP_CONTENT));

                        albumCell.setOnClickListener(v -> presentFragment(new AlbumProfileFragment(album.title, album.albumUrl, album.coverUrl)));
                        albumsContainer.addView(albumCell);
                    }
                }
                @Override public void onFailed() {}
            });

            // Если ссылка есть (перешли из поиска) — грузим треки сразу
            if (artistUrl != null && !artistUrl.isEmpty()) {
                fetchTracksFromHitmos(artistUrl, context);
            } else {
                // Если ссылки нет (перешли из плеера) — ищем ее в фоне
                MusicWebSearcher.search(artistName, (tracks, artists, albums) -> {
                    String hitmosUrl = null;
                    if (artists != null && !artists.isEmpty()) {
                        for (MusicWebSearcher.WebArtist a : artists) {
                            if (a.name.equalsIgnoreCase(artistName)) { hitmosUrl = a.artistUrl; break; }
                        }
                        if (hitmosUrl == null) hitmosUrl = artists.get(0).artistUrl;
                    }
                    if (hitmosUrl != null) {
                        artistUrl = hitmosUrl;
                        AndroidUtilities.runOnUIThread(() -> fetchTracksFromHitmos(artistUrl, context));
                    } else {
                        AndroidUtilities.runOnUIThread(() -> Toast.makeText(context, "Треки не найдены", Toast.LENGTH_SHORT).show());
                    }
                });
            }
        }

        private void fetchTracksFromHitmos(String targetUrl, Context context) {
            MusicWebSearcher.loadArtistProfile(targetUrl, new MusicWebSearcher.ArtistProfileCallback() {
                @Override
                public void onResult(ArrayList<MusicWebSearcher.WebAlbum> ignoredAlbums, ArrayList<MusicWebSearcher.WebTrack> tracks) {
                    queue.clear();
                    int trackIndex = 1;
                    for (MusicWebSearcher.WebTrack track : tracks) {
                        int durSec = ChannelMusicFragment.parseDuration(track.duration);
                        MessageObject fakeMsg = ChannelMusicFragment.createWebMessageObject(currentAccount, track.title, track.artist, track.downloadUrl, durSec, -(long)track.downloadUrl.hashCode(), "");
                        queue.add(fakeMsg);

                        LinearLayout trackRow = new LinearLayout(context);
                        trackRow.setOrientation(LinearLayout.HORIZONTAL);
                        trackRow.setGravity(Gravity.CENTER_VERTICAL);
                        trackRow.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 0));
                        trackRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));

                        TextView numberTxt = new TextView(context);
                        numberTxt.setText(String.valueOf(trackIndex++));
                        numberTxt.setTextSize(15);
                        numberTxt.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                        numberTxt.setGravity(Gravity.CENTER);
                        trackRow.addView(numberTxt, LayoutHelper.createLinear(24, LayoutHelper.WRAP_CONTENT, 0, 0, 12, 0));

                        BackupImageView trackCover = new BackupImageView(context);
                        trackCover.setRoundRadius(AndroidUtilities.dp(4));
                        String useCover = track.coverUrl != null && !track.coverUrl.isEmpty() ? track.coverUrl : bannerUrl;
                        trackCover.setImage(useCover, "50_50", null);
                        trackRow.addView(trackCover, LayoutHelper.createLinear(48, 48, 0, 0, 12, 0));

                        LinearLayout textContainer = new LinearLayout(context);
                        textContainer.setOrientation(LinearLayout.VERTICAL);

                        TextView titleTxt = new TextView(context);
                        titleTxt.setText(track.title);
                        titleTxt.setTextSize(16);
                        titleTxt.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                        titleTxt.setSingleLine(true);
                        titleTxt.setEllipsize(android.text.TextUtils.TruncateAt.END);

                        TextView artistTxt = new TextView(context);
                        artistTxt.setText(track.artist);
                        artistTxt.setTextSize(14);
                        artistTxt.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                        artistTxt.setSingleLine(true);

                        textContainer.addView(titleTxt, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                        textContainer.addView(artistTxt, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
                        trackRow.addView(textContainer, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

                        ImageView optionsIcon = new ImageView(context);
                        optionsIcon.setImageResource(org.telegram.messenger.R.drawable.ic_ab_other);
                        optionsIcon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon));
                        optionsIcon.setScaleType(ImageView.ScaleType.CENTER);
                        optionsIcon.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
                        trackRow.addView(optionsIcon, LayoutHelper.createLinear(40, 40));
                        optionsIcon.setOnClickListener(v -> showWebTrackOptionsBottomSheet(ArtistProfileFragment.this, track));

                        trackRow.setOnClickListener(v -> {
                            MediaController.getInstance().setPlaylist(queue, fakeMsg, -(long)"artist_context".hashCode());
                            MediaController.getInstance().seekToProgress(fakeMsg, 0);
                            MediaController.getInstance().playMessage(fakeMsg);
                            updatePlayingTrackColor();
                        });
                        tracksContainer.addView(trackRow);
                    }
                    updatePlayingTrackColor();
                }
                @Override public void onFailed() {}
            });
        }

    }

    public class AlbumProfileFragment extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
        private String title;
        private String albumUrl;
        private String coverUrl;
        private ArrayList<MessageObject> playlistMessages = new ArrayList<>();
        private RecyclerListView listView;
        private AlbumMusicAdapter adapter;
        private long albumUniqueId;

        public AlbumProfileFragment(String title, String albumUrl, String coverUrl) {
            this.title = title;
            this.albumUrl = albumUrl;
            this.coverUrl = coverUrl;
            this.albumUniqueId = -(long) Math.abs(albumUrl.hashCode());
        }

        private MessageObject getSavedAlbumMessage() {
            for (MessageObject msg : savedAlbumMessages) {
                if (msg.messageOwner != null && msg.messageOwner.message != null && msg.messageOwner.message.contains(albumUrl)) {
                    return msg;
                }
            }
            return null;
        }

        @Override
        public boolean onFragmentCreate() {
            super.onFragmentCreate();
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidReset);
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidStart);
            loadAlbumData();
            return true;
        }

        @Override
        public void onFragmentDestroy() {
            super.onFragmentDestroy();
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidReset);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidStart);
        }

        @Override
        public View createView(Context context) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.setTitle("");
            actionBar.setOccupyStatusBar(true);

            ActionBarMenu menu = actionBar.createMenu();
            boolean isSaved = getSavedAlbumMessage() != null;
            if (isSaved) {
                menu.addItem(1, R.drawable.msg_delete);
            } else {
                menu.addItem(1, R.drawable.msg_add);
            }
            menu.addItem(5, R.drawable.msg_download);

            actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) finishFragment();
                    else if (id == 1) {
                        MessageObject savedMsg = getSavedAlbumMessage();
                        if (savedMsg != null) {
                            deleteTrackFromChannel(savedMsg);
                            savedAlbumMessages.remove(savedMsg);
                            safeNotifyAdapter(savedAlbumsAdapter, null);
                            finishFragment();
                            Toast.makeText(getParentActivity(), "Альбом удален", Toast.LENGTH_SHORT).show();
                        } else {
                            saveAlbumToChannel(title, albumUrl, coverUrl);
                            menu.getItem(1).setIcon(R.drawable.msg_delete);
                        }
                    } else if (id == 5) {
                        Toast.makeText(getParentActivity(), "Скачивание альбома в фоне началось...", Toast.LENGTH_LONG).show();
                        File cacheDir = ApplicationLoader.applicationContext.getExternalCacheDir();
                        long rawId = getChannelIdFromFile();
                        if (rawId == 0) return;

                        new Thread(() -> {
                            for (MessageObject m : playlistMessages) {
                                if (m.messageOwner.message != null && m.messageOwner.message.startsWith("SG_STREAM|||")) {
                                    String[] parts = m.messageOwner.message.split("\\|\\|\\|");
                                    if (parts.length >= 4) {
                                        File downloadedFile = MusicWebSearcher.downloadTrackSync(parts[3], cacheDir);
                                        if (downloadedFile != null) {
                                            ArrayList<String> paths = new ArrayList<>();
                                            paths.add(downloadedFile.getAbsolutePath());
                                            AndroidUtilities.runOnUIThread(() -> {
                                                countBeforeUpload = audioMessages.size();
                                                isWaitingForUpload = true;
                                                AndroidUtilities.runOnUIThread(pollMessagesRunnable, 5000);
                                                SendMessagesHelper.prepareSendingDocuments(AccountInstance.getInstance(currentAccount), paths, new ArrayList<>(paths), null, null, "audio/mp3", -rawId, null, null, null, null, null, true, 0, null, null, 0, 0, false, 0);
                                            });
                                        }
                                        try { Thread.sleep(2000); } catch (Exception e){}
                                    }
                                }
                            }

                            AndroidUtilities.runOnUIThread(() -> {
                                SendMessagesHelper.getInstance(currentAccount).sendMessage("Ссылка на альбом: " + albumUrl, -rawId, null, null, null, true, null, null, null, true, 0, 0, null, false);
                                Toast.makeText(getParentActivity(), "Все треки альбома отправлены на загрузку", Toast.LENGTH_SHORT).show();
                            });
                        }).start();
                    }
                }
            });

            FrameLayout content = new FrameLayout(context) {
                @Override protected void onDraw(Canvas canvas) {
                    canvas.drawColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                }
            };
            fragmentView = content;

            listView = new RecyclerListView(context);
            listView.setLayoutManager(new LinearLayoutManager(context));
            adapter = new AlbumMusicAdapter(context);
            listView.setAdapter(adapter);
            listView.setPadding(0, 0, 0, AndroidUtilities.dp(70));
            listView.setClipToPadding(false);
            listView.setOnItemClickListener((view, position) -> {
                if (adapter.getItemViewType(position) == 2) return;
                if (position > 0 && position <= playlistMessages.size()) {
                    playStrictInPlaylist(playlistMessages.get(position - 1));
                }
            });

            content.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            ChannelMusicFragment.MiniPlayerView miniPlayerView = new ChannelMusicFragment.MiniPlayerView(context, this);
            content.addView(miniPlayerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 64, Gravity.BOTTOM));

            return fragmentView;
        }

        private void loadAlbumData() {
            if (albumUrl != null && albumUrl.startsWith("SEARCH_ALBUM:")) {
                String[] parts = albumUrl.replace("SEARCH_ALBUM:", "").split("\\|\\|\\|", 2);
                String aTitle = parts[0];
                String aArtist = parts.length > 1 ? parts[1] : "";

                playlistMessages.clear();
                MusicWebSearcher.loadAlbumWithDeezerFallback(aTitle, aArtist, new MusicWebSearcher.ArtistProfileCallback() {
                    @Override
                    public void onResult(ArrayList<MusicWebSearcher.WebAlbum> albums, ArrayList<MusicWebSearcher.WebTrack> tracks) {
                        safeNotifyAdapter(adapter, listView);
                    }
                    @Override
                    public void onTrackLoaded(MusicWebSearcher.WebTrack wt) {
                        playlistMessages.add(createWebMessageObject(currentAccount, wt.title, wt.artist, wt.downloadUrl, parseDuration(wt.duration), -(long)wt.downloadUrl.hashCode(), wt.coverUrl));
                        safeNotifyAdapter(adapter, listView);
                    }
                    @Override
                    public void onFailed() {
                        Toast.makeText(getParentActivity(), "Альбом не найден", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                fetchRealAlbum(albumUrl);
            }
        }

        private void fetchRealAlbum(String realUrl) {
            playlistMessages.clear();
            MusicWebSearcher.loadArtistProfile(realUrl, new MusicWebSearcher.ArtistProfileCallback() {
                @Override public void onResult(ArrayList<MusicWebSearcher.WebAlbum> albums, ArrayList<MusicWebSearcher.WebTrack> tracks) {
                    safeNotifyAdapter(adapter, listView);
                }
                @Override
                public void onTrackLoaded(MusicWebSearcher.WebTrack wt) {
                    playlistMessages.add(createWebMessageObject(currentAccount, wt.title, wt.artist, wt.downloadUrl, parseDuration(wt.duration), -(long)wt.downloadUrl.hashCode(), wt.coverUrl));
                    safeNotifyAdapter(adapter, listView);
                }
                @Override public void onFailed() { Toast.makeText(getParentActivity(), "Ошибка загрузки", Toast.LENGTH_SHORT).show(); }
            });
        }

        private void playStrictInPlaylist(MessageObject messageObject) {
            MediaController mediaController = MediaController.getInstance();
            MessageObject currentPlaying = mediaController.getPlayingMessageObject();
            if (currentPlaying != null && currentPlaying.getId() == messageObject.getId()) {
                if (mediaController.isMessagePaused()) mediaController.playMessage(messageObject);
                else mediaController.pauseMessage(messageObject);
                return;
            }
            for (MessageObject m : playlistMessages) { m.audioProgress = 0.0f; m.audioProgressSec = 0; }
            mediaController.setPlaylist(new ArrayList<>(playlistMessages), messageObject, albumUniqueId);
            mediaController.seekToProgress(messageObject, 0);
            mediaController.playMessage(messageObject);
        }

        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == NotificationCenter.messagePlayingDidStart || id == NotificationCenter.messagePlayingPlayStateChanged || id == NotificationCenter.messagePlayingDidReset) {
                safeNotifyAdapter(adapter, listView);
            }
        }

        private class AlbumMusicAdapter extends RecyclerListView.SelectionAdapter {
            private Context mContext;
            public AlbumMusicAdapter(Context context) { mContext = context; }
            @Override public boolean isEnabled(RecyclerView.ViewHolder holder) { return holder.getItemViewType() == 1; }
            @Override public int getItemCount() { return playlistMessages.size() + 2; }
            @Override public int getItemViewType(int position) {
                if (position == 0) return 0;
                if (position == playlistMessages.size() + 1) return 2;
                return 1;
            }
            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                if (viewType == 2) {
                    View phantom = new View(mContext);
                    phantom.setLayoutParams(new RecyclerView.LayoutParams(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(70)));
                    return new RecyclerListView.Holder(phantom);
                }
                if (viewType == 0) {
                    FrameLayout headerContainer = new FrameLayout(mContext);
                    headerContainer.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                    BackupImageView bgCover = new BackupImageView(mContext);
                    bgCover.setImage(coverUrl, "100_100_b", null);
                    headerContainer.addView(bgCover, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

                    View gradientView = new View(mContext);
                    int bgColor = Theme.getColor(Theme.key_windowBackgroundWhite);
                    int transparentBg = (bgColor & 0x00FFFFFF) | 0x88000000;
                    android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
                            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{transparentBg, bgColor});
                    gradientView.setBackground(gd);
                    headerContainer.addView(gradientView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

                    LinearLayout content = new LinearLayout(mContext);
                    content.setOrientation(LinearLayout.VERTICAL);
                    content.setGravity(Gravity.CENTER_HORIZONTAL);
                    content.setPadding(0, AndroidUtilities.dp(60), 0, AndroidUtilities.dp(24));

                    BackupImageView centerCover = new BackupImageView(mContext);
                    centerCover.setRoundRadius(AndroidUtilities.dp(8));
                    centerCover.setImage(coverUrl, "200_200", null);
                    content.addView(centerCover, LayoutHelper.createLinear(200, 200, Gravity.CENTER_HORIZONTAL));

                    TextView albumTitleText = new TextView(mContext);
                    albumTitleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
                    albumTitleText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    albumTitleText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    albumTitleText.setGravity(Gravity.CENTER);
                    albumTitleText.setText(title);
                    content.addView(albumTitleText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 20, 0, 0));

                    LinearLayout buttonsLayout = new LinearLayout(mContext);
                    buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
                    buttonsLayout.setGravity(Gravity.CENTER);

                    TextView playButton = new TextView(mContext);
                    playButton.setText("Слушать");
                    playButton.setTextColor(0xFFFFFFFF);
                    playButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                    playButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    playButton.setGravity(Gravity.CENTER);
                    playButton.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(24), 0xFF1DB954));
                    playButton.setPadding(AndroidUtilities.dp(40), 0, AndroidUtilities.dp(40), 0);
                    buttonsLayout.addView(playButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48, 0, 0, 0, 0));

                    playButton.setOnClickListener(v -> {
                        if (!playlistMessages.isEmpty()) playStrictInPlaylist(playlistMessages.get(0));
                    });

                    content.addView(buttonsLayout, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48, Gravity.CENTER_HORIZONTAL, 0, 20, 0, 0));

                    // --- ШИРОКАЯ КНОПКА ВОЛНЫ ДЛЯ АЛЬБОМОВ ---
                    TextView waveBtn = new TextView(mContext);
                    waveBtn.setText("🔮 Моя Волна по альбому");
                    waveBtn.setTextColor(0xFFFFFFFF);
                    waveBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                    waveBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    waveBtn.setGravity(Gravity.CENTER);
                    android.graphics.drawable.GradientDrawable gdBtn = new android.graphics.drawable.GradientDrawable(
                            android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[]{0xFF8A2BE2, 0xFF4B0082});
                    gdBtn.setCornerRadius(AndroidUtilities.dp(12));
                    waveBtn.setBackground(gdBtn);
                    waveBtn.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14));
                    waveBtn.setOnClickListener(v -> {
                        Toast.makeText(mContext, "Генерация волны...", Toast.LENGTH_SHORT).show();
                        String aArtist = "";
                        if (albumUrl != null && albumUrl.startsWith("SEARCH_ALBUM:")) {
                            String[] parts = albumUrl.replace("SEARCH_ALBUM:", "").split("\\|\\|\\|", 2);
                            aArtist = parts.length > 1 ? parts[1] : "";
                        }
                        SmartRadioEngine.generateWaveByAlbum(aArtist, title, currentAccount, new SmartRadioEngine.WaveCallback() {
                            @Override
                            public void onTracksFound(ArrayList<MessageObject> tracks) {
                                MediaController.getInstance().setPlaylist(tracks, tracks.get(0), 0);
                                MediaController.getInstance().playMessage(tracks.get(0));
                            }
                            @Override public void onMoreTracksAdded(ArrayList<MessageObject> tracks) {}
                            @Override public void onError() { Toast.makeText(mContext, "Ошибка", Toast.LENGTH_SHORT).show(); }
                        });
                    });
                    content.addView(waveBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 24, 20, 24, 0));
                    // --- КОНЕЦ КНОПКИ ---

                    headerContainer.addView(content, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

                    return new RecyclerListView.Holder(headerContainer);
                }
                SpotifyTrackCell cell = new SpotifyTrackCell(mContext);
                return new RecyclerListView.Holder(cell);
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                if (holder.getItemViewType() == 2 || holder.getItemViewType() == 0) return;
                SpotifyTrackCell myCell = (SpotifyTrackCell) holder.itemView;
                MessageObject msg = playlistMessages.get(position - 1);
                myCell.setMessageObject(msg);

                myCell.optionsButton.setVisibility(View.VISIBLE);
                myCell.optionsButton.setOnClickListener(v -> {
                    if (msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) {
                        String[] parts = msg.messageOwner.message.split("\\|\\|\\|");
                        if (parts.length >= 5) {
                            String durStr = String.valueOf(parseDuration(parts[4]) / 60) + ":" + String.format("%02d", parseDuration(parts[4]) % 60);
                            String cUrl = parts.length >= 6 ? parts[5] : "";
                            MusicWebSearcher.WebTrack track = new MusicWebSearcher.WebTrack(parts[1], parts[2], parts[3], durStr, cUrl);
                            showWebTrackOptionsBottomSheet(AlbumProfileFragment.this, track);
                            return;
                        }
                    }
                    showTrackOptionsBottomSheet(AlbumProfileFragment.this, msg, true, () -> {
                        playlistMessages.remove(msg);
                        safeNotifyAdapter(this, listView);
                    });
                });
            }
        }
    }


public static class ChipCell extends FrameLayout {
        private TextView textView;

        public ChipCell(Context context) {
            super(context);
            // MATCH_PARENT по ширине, чтобы занимать всю колонку
            setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));

            textView = new TextView(context);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            textView.setGravity(Gravity.CENTER);
            textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));

            // Ограничение в 1 строку, если не влезает — ставится троеточие
            textView.setSingleLine(true);
            textView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            textView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));

            addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        }

        public void setChip(String text, boolean isSelected) {
            textView.setText(text);
            if (isSelected) {
                textView.setTextColor(0xFFFFFFFF);
                textView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), 0xFF8A2BE2));
            } else {
                textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                textView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_windowBackgroundGray)));
            }
        }
    }

    private class ChipsAdapter extends RecyclerListView.SelectionAdapter {
        private Context mContext;
        private String[] items;
        private int selectedIndex;

        public ChipsAdapter(Context context, String[] items, String currentSelection) {
            this.mContext = context;
            this.items = items;

            this.selectedIndex = 0;
            for (int i = 0; i < items.length; i++) {
                if (items[i].equals(currentSelection)) {
                    this.selectedIndex = i;
                    break;
                }
            }
        }

        public String getSelectedItem() {
            return items[selectedIndex];
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) { return true; }

        @Override
        public int getItemCount() { return items.length; }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RecyclerListView.Holder(new ChipCell(mContext));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ChipCell cell = (ChipCell) holder.itemView;
            cell.setChip(items[position], position == selectedIndex);

            cell.setOnClickListener(v -> {
                int oldIndex = selectedIndex;
                selectedIndex = position;
                notifyItemChanged(oldIndex);
                notifyItemChanged(selectedIndex);
            });
        }
    }
    private class AlbumAdapter extends RecyclerListView.SelectionAdapter {
        private Context mContext;
        private ArrayList<MusicWebSearcher.WebAlbum> albums = new ArrayList<>();

        public AlbumAdapter(Context context) {
            this.mContext = context;
        }

        public void setAlbums(ArrayList<MusicWebSearcher.WebAlbum> newAlbums) {
            this.albums.clear();
            if (newAlbums != null) {
                this.albums.addAll(newAlbums);
            }
            notifyDataSetChanged();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) { return true; }

        @Override
        public int getItemCount() { return albums.size(); }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout layout = new LinearLayout(mContext);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            // ИСПРАВЛЕНИЕ: Высота WRAP_CONTENT убирает черную дыру
            layout.setLayoutParams(new RecyclerView.LayoutParams(AndroidUtilities.dp(100), ViewGroup.LayoutParams.WRAP_CONTENT));
            layout.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));

            BackupImageView cover = new BackupImageView(mContext);
            cover.setRoundRadius(AndroidUtilities.dp(8));
            layout.addView(cover, LayoutHelper.createLinear(70, 70, Gravity.CENTER, 0, 5, 0, 5));

            TextView titleText = new TextView(mContext);
            titleText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            titleText.setGravity(Gravity.CENTER);
            titleText.setMaxLines(1);
            titleText.setEllipsize(TextUtils.TruncateAt.END);
            layout.addView(titleText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 4, 0, 4, 0));

            return new RecyclerListView.Holder(layout);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            LinearLayout layout = (LinearLayout) holder.itemView;
            BackupImageView cover = (BackupImageView) layout.getChildAt(0);
            TextView titleText = (TextView) layout.getChildAt(1);

            MusicWebSearcher.WebAlbum album = albums.get(position);
            titleText.setText(album.title);
            cover.setImage(album.coverUrl, "100_100", null);

            layout.setOnClickListener(v -> {
                presentFragment(new AlbumProfileFragment(album.title, album.albumUrl, album.coverUrl));
            });

        }
    }


    private class ArtistAdapter extends RecyclerListView.SelectionAdapter {
        private Context mContext;
        private ArrayList<MusicWebSearcher.WebArtist> artists = new ArrayList<>();

        public ArtistAdapter(Context context) {
            this.mContext = context;
        }

        public void setArtists(ArrayList<MusicWebSearcher.WebArtist> newArtists) {
            this.artists.clear();
            if (newArtists != null) {
                this.artists.addAll(newArtists);
            }
            notifyDataSetChanged();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) { return true; }

        @Override
        public int getItemCount() { return artists.size(); }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout layout = new LinearLayout(mContext);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            layout.setLayoutParams(new RecyclerView.LayoutParams(AndroidUtilities.dp(80), ViewGroup.LayoutParams.WRAP_CONTENT));
            layout.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));

            BackupImageView avatar = new BackupImageView(mContext);
            avatar.setRoundRadius(AndroidUtilities.dp(30)); // Идеальный круг
            layout.addView(avatar, LayoutHelper.createLinear(60, 60, Gravity.CENTER, 0, 5, 0, 5));

            TextView nameText = new TextView(mContext);
            nameText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            nameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            nameText.setGravity(Gravity.CENTER);
            nameText.setMaxLines(1);
            nameText.setEllipsize(TextUtils.TruncateAt.END);
            layout.addView(nameText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 4, 0, 4, 0));

            return new RecyclerListView.Holder(layout);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            LinearLayout layout = (LinearLayout) holder.itemView;
            BackupImageView avatar = (BackupImageView) layout.getChildAt(0);
            TextView nameText = (TextView) layout.getChildAt(1);

            MusicWebSearcher.WebArtist artist = artists.get(position);
            nameText.setText(artist.name);

            // Принудительно грузим с Deezer с высоким разрешением
            MusicWebSearcher.fetchArtistPictureFromDeezer(artist.name, url -> {
                if (url != null) {
                    artist.imageUrl = url;
                    avatar.setImage(url, "150_150", null);
                } else if (!TextUtils.isEmpty(artist.imageUrl)) {
                    avatar.setImage(artist.imageUrl, "150_150", null);
                }
            });

            layout.setOnClickListener(v -> {
                presentFragment(new ArtistProfileFragment(artist.name, artist.artistUrl, artist.imageUrl));
            });


        }
    }
    public static TextView createYandexWaveButton(Context context, String text, View.OnClickListener onClickListener) {
        TextView waveBtn = new TextView(context);
        waveBtn.setText(text);
        waveBtn.setTextColor(0xFFFFFFFF);
        waveBtn.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 16);
        waveBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        waveBtn.setGravity(android.view.Gravity.CENTER);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF8A2BE2, 0xFF4B0082});
        gd.setCornerRadius(AndroidUtilities.dp(12));
        waveBtn.setBackground(gd);
        waveBtn.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14));
        waveBtn.setOnClickListener(onClickListener);
        return waveBtn;
    }

}
