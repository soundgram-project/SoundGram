package soundgram.music;

import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.LocaleController;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.messenger.LocaleController.getStringResId;
import static soundgram.music.MusicWebSearcher.loadArtistTracksDeezer;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import org.telegram.messenger.FileLoader;
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
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.VideoEditedInfo;
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
import org.telegram.messenger.audioinfo.AudioInfo;
import org.telegram.ui.Components.RadialProgress2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
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
import org.telegram.messenger.ChatObject;
import org.telegram.ui.Cells.ProfileSearchCell;
import java.util.concurrent.ConcurrentHashMap;
import soundgram.music.MusicWebSearcher;
import soundgram.ui.SoundGramStyleManager;
import soundgram.ui.SoundGramStyleSettingsFragment;
import soundgram.ui.WaveGLSurfaceView;
import org.telegram.ui.MainTabsActivity;
import org.telegram.ui.LaunchActivity;
public class ChannelMusicFragment extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, ChatActivityInterface, MainTabsActivity.TabFragmentDelegate {

    private org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode iBlur3SourceGlass;
    private org.telegram.ui.Components.blur3.DownscaleScrollableNoiseSuppressor scrollableViewNoiseSuppressor;
    private org.telegram.ui.Components.blur3.ViewGroupPartRenderer libraryBlurCapture;
    private org.telegram.ui.Components.blur3.ViewGroupPartRenderer albumsBlurCapture;
    private boolean iBlur3Invalidated;
    private int navigationBarHeight;

    private final ArrayList<android.graphics.RectF> iBlur3Positions = new ArrayList<>();
    private final android.graphics.RectF iBlur3PositionActionBar = new android.graphics.RectF();
    private final android.graphics.RectF iBlur3PositionMainTabs = new android.graphics.RectF(); {
        iBlur3Positions.add(iBlur3PositionActionBar);
        iBlur3Positions.add(iBlur3PositionMainTabs);
    }

    // iBlur3Capture is set in createView after the list views are created
    private org.telegram.ui.Components.blur3.capture.IBlur3Capture iBlur3Capture;

    public ChannelMusicFragment() {
        super();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            scrollableViewNoiseSuppressor = new org.telegram.ui.Components.blur3.DownscaleScrollableNoiseSuppressor();
            iBlur3SourceGlass = new org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode(null);
        } else {
            scrollableViewNoiseSuppressor = null;
            iBlur3SourceGlass = null;
        }
    }

    @Override
    public boolean canParentTabsSlide(android.view.MotionEvent ev, boolean forward) {
        if (viewPager == null || viewPager.getAdapter() == null) {
            return true;
        }
        int currentItem = viewPager.getCurrentItem();
        int count = viewPager.getAdapter().getCount();
        if (forward) {
            return currentItem == count - 1;
        } else {
            return currentItem == 0;
        }
    }

    @Override
    public org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode getGlassSource() {
        return iBlur3SourceGlass;
    }

    private void blur3_InvalidateBlur() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S
                || scrollableViewNoiseSuppressor == null
                || fragmentView == null
                || iBlur3Capture == null) {
            return;
        }

        final int additionalList = org.telegram.messenger.AndroidUtilities.dp(48);
        final int mainTabBottom = fragmentView.getMeasuredHeight() - navigationBarHeight
                - org.telegram.messenger.AndroidUtilities.dp(app.nimarkogram.messenger.utils.ui.MainTabsHelper.getMainTabsMargin());
        final int mainTabTop = mainTabBottom
                - org.telegram.messenger.AndroidUtilities.dp(app.nimarkogram.messenger.utils.ui.MainTabsHelper.getMainTabsHeight());

        iBlur3PositionActionBar.set(0, -additionalList, fragmentView.getMeasuredWidth(),
                (actionBar != null ? actionBar.getMeasuredHeight() : 0) + additionalList);
        iBlur3PositionMainTabs.set(0, mainTabTop, fragmentView.getMeasuredWidth(), mainTabBottom);
        iBlur3PositionMainTabs.inset(0,
                org.telegram.messenger.LiteMode.isEnabled(org.telegram.messenger.LiteMode.FLAG_LIQUID_GLASS)
                        ? 0 : -org.telegram.messenger.AndroidUtilities.dp(48));

        scrollableViewNoiseSuppressor.setupRenderNodes(iBlur3Positions, 2);
        scrollableViewNoiseSuppressor.invalidateResultRenderNodes(
                iBlur3Capture, fragmentView.getMeasuredWidth(), fragmentView.getMeasuredHeight());
    }

    public interface PlaylistUpdateListener {
        void onPlaylistUpdated();
    }
    private ImageView playWaveBtn;
    private WaveGLSurfaceView waveWidget; // Добавь в начало класса
    // Глобальные списки (вставить в начало класса рядом с audioMessages)
    private static final android.util.SparseArray<ArrayList<MessageObject>> accountAudioMessages = new android.util.SparseArray<>();
    private static final android.util.SparseArray<ArrayList<MessageObject>> accountSavedAlbumMessages = new android.util.SparseArray<>();
    private static final android.util.SparseArray<ArrayList<MessageObject>> accountPlaylistMessagesList = new android.util.SparseArray<>();

    public static synchronized ArrayList<MessageObject> getAudioMessages(int account) {
        ArrayList<MessageObject> list = accountAudioMessages.get(account);
        if (list == null) {
            list = new ArrayList<>();
            accountAudioMessages.put(account, list);
        }
        return list;
    }

    public static synchronized ArrayList<MessageObject> getSavedAlbumMessages(int account) {
        ArrayList<MessageObject> list = accountSavedAlbumMessages.get(account);
        if (list == null) {
            list = new ArrayList<>();
            accountSavedAlbumMessages.put(account, list);
        }
        return list;
    }

    public static synchronized ArrayList<MessageObject> getPlaylistMessagesList(int account) {
        ArrayList<MessageObject> list = accountPlaylistMessagesList.get(account);
        if (list == null) {
            list = new ArrayList<>();
            accountPlaylistMessagesList.put(account, list);
        }
        return list;
    }

    // Глобальные списки для текущего экземпляра фрагмента
    public ArrayList<MessageObject> playlistMessagesList = new ArrayList<>();
    public static MessageObject globalStatsMessage = null;
    public static final int TRACK_SPACING_DP        = 6;   // между ячейками
    public static final int TRACK_HORIZONTAL_DP     = 12;  // боковой отступ
    public static final int TRACK_ARTIST_SPACING_DP = 0;   // в профиле артиста — без отступов

    // Вспомогательный метод для проверки, является ли сообщение плейлистом
    public static boolean isPlaylistMessage(MessageObject msg) {
        if (msg == null || msg.messageOwner == null || msg.messageOwner.message == null) return false;
        return msg.messageOwner.message.contains("\nicon=");
    }

    // Добавьте или замените эти переменные в классе:
    private SavedAlbumsAdapter savedAlbumsAdapter;
    public ArrayList<MessageObject> savedAlbumMessages = new ArrayList<>();
    private RecyclerListView albumsRecyclerView;
    private AlbumAdapter albumsAdapter; // Адаптер для альбомов добавим ниже
    private LinearLayout tabStrip;
    private androidx.viewpager.widget.ViewPager viewPager;
    private TextView albumsTabBtnRef;
    private int tabAccentColor, tabInactiveColor;
    private LinearLayout libraryListHeader;
    private RecyclerListView artistRecyclerView;
    private ArtistAdapter artistAdapter;
    private SwipeRefreshLayout swipeRefreshLayout; // ИСПРАВЛЕНИЕ: Сделать переменную глобальной
    private RecyclerListView listView;
    private RecyclerListView savedAlbumsListView; // Albums grid page
    private MusicAdapter adapter;

    private RecyclerListView playlistsListView;
    private PlaylistsHorizontalAdapter playlistsAdapter;
    private FrameLayout playlistsContainer;
    private TextView emptyPlaylistsText;

    private LinearLayout headerContainer;

    private EditText searchField;
    private ImageView searchBackBtn;
    private ImageView searchIconBtn;
    private ImageView refreshBtn;
    private ImageView statsBtn;
    private TextView headerTitle;
    private LinearLayout standardHeaderLayout;
    private LinearLayout searchHeaderLayout;

    public ArrayList<MessageObject> audioMessages = new ArrayList<>();
    private ArrayList<MessageObject> searchResultMessages = new ArrayList<>();
    private ArrayList<MusicWebSearcher.WebTrack> webResultTracks = new ArrayList<>();
    private ArrayList<File> playlistFiles = new ArrayList<>();
    private long musicChannelId = 0;
    private long musicChannelHash = 0;
    private boolean isSearching = false;
    private Timer searchTimer;
    private String currentSearchQuery = "";

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        audioMessages = getAudioMessages(currentAccount);
        savedAlbumMessages = getSavedAlbumMessages(currentAccount);
        playlistMessagesList = getPlaylistMessagesList(currentAccount);

        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.didReceiveNewMessages);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidStart);

        checkOrCreateMusicChannel();
        refreshPlaylistFiles();

        // НОВЫЙ ВЫЗОВ: Проверяем наличие обновлений волны с GitHub
        SmartRadioEngine.checkAndUpdateWaveSeeds(getParentActivity());

        return true;
    }

    public static long getValidUserId(int account) {
        UserConfig userConfig = UserConfig.getInstance(account);
        if (!userConfig.isConfigLoaded()) {
            userConfig.loadConfig();
        }
        return userConfig.getClientUserId();
    }

    private int channelSearchRetryCount = 0;

    public void checkOrCreateMusicChannel() {
        long userId = getValidUserId(currentAccount);
        final String targetChannelTitle = "id" + userId + "soundgram";
        final String userSoundGramTitle = userId != 0 ? ("id" + userId + "soundgram") : "soundgram";

        // 1. Проверяем сохранённый ID канала для этого аккаунта
        long savedId = getChannelIdFromFile();
        if (savedId != 0) {
            musicChannelId = savedId;
            loadMessagesFromCache();
            loadMusicFromChannel();
            return;
        }

        // 2. Проверяем глобальный legacy ключ ТОЛЬКО для главного аккаунта (account 0)
        if (currentAccount == 0) {
            long legacyId = MessagesController.getNotificationsSettings(currentAccount).getLong("music_channel_id", 0);
            if (legacyId != 0) {
                musicChannelId = legacyId;
                saveChannelIdToFile(musicChannelId);
                MessagesController.getNotificationsSettings(currentAccount).edit().remove("music_channel_id").apply();
                loadMessagesFromCache();
                loadMusicFromChannel();
                return;
            }
        }

        // 3. Ищем существующий канал/группу пользователя в диалогах и кэше чатов
        TLRPC.Chat found = findSoundGramChannelInMemory(targetChannelTitle, userSoundGramTitle);
        if (found != null) {
            musicChannelId = found.id;
            musicChannelHash = found.access_hash;
            saveChannelIdToFile(musicChannelId, musicChannelHash);
            loadMessagesFromCache();
            loadMusicFromChannel();
            return;
        }

        // 4. Диалоги могут быть ещё не загружены (особенно на вторичных аккаунтах).
        //    Запрашиваем загрузку диалогов из БД/сети и повторяем попытку
        MessagesController controller = MessagesController.getInstance(currentAccount);
        if (!controller.dialogsLoaded) {
            controller.loadDialogs(0, 0, 100, true);
        }

        if (channelSearchRetryCount < 5) {
            channelSearchRetryCount++;
            AndroidUtilities.runOnUIThread(this::checkOrCreateMusicChannel, 1000);
            return;
        }

        // 5. Канал не привязан и не найден после всех попыток — показываем диалог выбора
        channelSearchRetryCount = 0;
        loadMessagesFromCache();
        showSetupChannelPromptDialog();
    }

    private boolean isSoundGramChannelTitle(String title, String target, String userTitle) {
        if (title == null) return false;
        String t = title.trim();
        if (target.equalsIgnoreCase(t) || userTitle.equalsIgnoreCase(t)) return true;
        if (t.equalsIgnoreCase("id" + getValidUserId(currentAccount) + "soundgram")) return true;
        if ("SoundGram Music".equalsIgnoreCase(t) || "SoundGram".equalsIgnoreCase(t)) return true;
        if ("SoundGram Music (0)".equalsIgnoreCase(t)) return true;
        // Дополнительная проверка: любой "SoundGram Music (число)" канал где мы creator
        if (t.toLowerCase().startsWith("soundgram music (") && t.endsWith(")")) return true;
        return false;
    }

    private TLRPC.Chat findSoundGramChannelInMemory(String targetTitle, String userTitle) {
        long userId = getValidUserId(currentAccount);
        String requiredUsername = "id" + userId + "soundgram";

        // Сначала ищем в диалогах
        ArrayList<TLRPC.Dialog> allDialogs = MessagesController.getInstance(currentAccount).getAllDialogs();
        if (allDialogs != null) {
            for (TLRPC.Dialog dialog : allDialogs) {
                if (dialog.id < 0) {
                    TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialog.id);
                    if (chat != null && isSoundGramAdmin(chat)) {
                        if (!TextUtils.isEmpty(chat.username) && chat.username.equalsIgnoreCase(requiredUsername)) {
                            return chat;
                        }
                        if (isSoundGramChannelTitle(chat.title, targetTitle, userTitle)) {
                            return chat;
                        }
                    }
                }
            }
        }

        // Затем в кэше чатов
        java.util.concurrent.ConcurrentHashMap<Long, TLRPC.Chat> chats = MessagesController.getInstance(currentAccount).getChats();
        if (chats != null) {
            for (TLRPC.Chat chat : chats.values()) {
                if (chat != null && isSoundGramAdmin(chat)) {
                    if (!TextUtils.isEmpty(chat.username) && chat.username.equalsIgnoreCase(requiredUsername)) {
                        return chat;
                    }
                    if (isSoundGramChannelTitle(chat.title, targetTitle, userTitle)) {
                        return chat;
                    }
                }
            }
        }

        return null;
    }

    private boolean isSoundGramAdmin(TLRPC.Chat chat) {
        return chat.creator || (chat.admin_rights != null && (chat.admin_rights.post_messages || chat.admin_rights.edit_messages));
    }
    private int currentSearchPage = 1;
    private boolean isSearchLoadingMore = false;
    private boolean hasMoreSearchResults = true;
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
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        SoundGramStatsManager.stopPeriodicSync();
        if (waveWidget != null) {
            waveWidget.releaseVisualizer();
        }
        isWaitingForUpload = false;
        AndroidUtilities.cancelRunOnUIThread(pollMessagesRunnable);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.didReceiveNewMessages);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidStart);

    }

    private void setSphereVisible(boolean visible) {
        if (waveWidget == null) return;
        if (visible) {
            waveWidget.setVisibility(View.VISIBLE);
            waveWidget.animate().alpha(1f).setDuration(150).setListener(null).start();
        } else {
            waveWidget.animate().alpha(0f).setDuration(150).setListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    waveWidget.setVisibility(View.GONE);
                }
            }).start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (waveWidget != null) {
            waveWidget.onPause();
            setSphereVisible(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkOrCreateMusicChannel();
        if (waveWidget != null) {
            if (viewPager != null && viewPager.getCurrentItem() == 0) {
                // Only resume GL renderer when the Wave tab is visible
                waveWidget.onResume();
                setSphereVisible(true);
            } else {
                // Keep the renderer paused on other tabs to avoid background GPU drain
                waveWidget.onPause();
            }
        }
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        super.onTransitionAnimationEnd(isOpen, backward);
        if (isOpen && waveWidget != null && viewPager != null && viewPager.getCurrentItem() == 0) {
            setSphereVisible(true);
        } else if (!isOpen && waveWidget != null) {
            setSphereVisible(false);
        }
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
    private int pollAttempts = 0;
    private static final int MAX_POLL_ATTEMPTS = 10; // максимум 10 попыток × 2сек = 20сек таймаут
    public final Runnable pollMessagesRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isWaitingForUpload) return;
            pollAttempts++;
            // Останавливаемся если трек/альбом появился ИЛИ вышел таймаут
            if (audioMessages.size() + savedAlbumMessages.size() > countBeforeUpload
                    || pollAttempts >= MAX_POLL_ATTEMPTS) {
                isWaitingForUpload = false;
                pollAttempts = 0;
                return;
            }
            // ИСПРАВЛЕНИЕ: Лёгкая проверка последних 10 сообщений вместо полной загрузки 3000.
            // Полный loadMusicFromChannel() с limit=3000 на UI-потоке вызывал ANR.
            pollCheckRecentMessages();
            AndroidUtilities.runOnUIThread(pollMessagesRunnable, 2000);
        }
    };

    /** Лёгкая проверка: запрашиваем только последние 10 сообщений канала.
     *  Если среди них появился новый альбом/трек — делаем полный reload. */
    private void pollCheckRecentMessages() {
        long targetId = getTargetChannelId();
        if (targetId == 0) return;
        TLRPC.TL_messages_search searchReq = new TLRPC.TL_messages_search();
        TLRPC.TL_inputPeerChannel inputPeer = new TLRPC.TL_inputPeerChannel();
        inputPeer.channel_id = targetId;
        inputPeer.access_hash = musicChannelHash;
        searchReq.peer = inputPeer;
        searchReq.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        searchReq.q = "";
        searchReq.limit = 10; // только последние 10 — не блокирует UI
        org.telegram.tgnet.ConnectionsManager.getInstance(currentAccount).sendRequest(searchReq, (response, error) -> {
            if (error != null || !(response instanceof TLRPC.messages_Messages)) return;
            TLRPC.messages_Messages messages = (TLRPC.messages_Messages) response;
            int newAlbums = 0, newTracks = 0;
            for (TLRPC.Message msg : messages.messages) {
                if (msg.message != null) {
                    if (msg.message.startsWith("SG_ALBUM|||")) newAlbums++;
                    else if (msg.message.startsWith("SG_STREAM|||") || msg.message.startsWith("SG_STATS|||")) newTracks++;
                } else {
                    MessageObject mo = new MessageObject(currentAccount, msg, false, false);
                    if (mo.isMusic()) newTracks++;
                }
            }
            final int finalNewAlbums = newAlbums;
            final int totalNew = newAlbums + newTracks;
            AndroidUtilities.runOnUIThread(() -> {
                if (totalNew > countBeforeUpload || (finalNewAlbums > 0 && savedAlbumMessages.size() == 0)
                        || totalNew != audioMessages.size() + savedAlbumMessages.size()) {
                    isWaitingForUpload = false;
                    pollAttempts = 0;
                    loadMusicFromChannel(); // полный reload только один раз когда данные пришли
                }
            });
        });
    }

    public void saveAlbumToChannel(String title, String url, String coverUrl) {
        long targetId = getTargetChannelId();
        if (targetId == 0) return;
        String text = "SG_ALBUM|||" + title + "|||" + url + "|||" + coverUrl;
        SendMessagesHelper.getInstance(currentAccount).sendMessage(SendMessagesHelper.SendMessageParams.of(text, -targetId, null, null, null, true, null, null, null, true, 0, 0, null, false));
        Toast.makeText(getParentActivity(), getString(R.string.sg_album_saved_to_library), Toast.LENGTH_SHORT).show();

        countBeforeUpload = audioMessages.size() + savedAlbumMessages.size();
        isWaitingForUpload = true;
        pollAttempts = 0;
        AndroidUtilities.runOnUIThread(pollMessagesRunnable, 1500);
    }

    private TextView createYmTab(Context context, String text) {
        TextView tab = new TextView(context);
        tab.setText(text);
        tab.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        tab.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        tab.setGravity(Gravity.CENTER);
        tab.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(7), AndroidUtilities.dp(14), AndroidUtilities.dp(7));
        tab.setSingleLine(true);
        tab.setHasTransientState(true);
        return tab;
    }

    private void applyTabActive(TextView tab, int accentColor) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(accentColor);
        bg.setCornerRadius(AndroidUtilities.dp(22));
        tab.setBackground(bg);
        tab.setTextColor(Theme.getColor(Theme.key_chats_actionIcon));
    }

    private void applyTabInactive(TextView tab, int inactiveColor) {
        tab.setBackground(null);
        tab.setTextColor(inactiveColor);
    }

    // ── Динамическое управление вкладкой Альбомы ──────────────────────────────
    private void refreshAlbumsTabVisibility(TextView albumsTabBtn, int accentColor, int inactiveColor) {
        if (tabStrip == null || albumsTabBtn == null || viewPager == null) return;
        boolean hasAlbums = !savedAlbumMessages.isEmpty();
        boolean tabVisible = tabStrip.indexOfChild(albumsTabBtn) >= 0;
        if (hasAlbums && !tabVisible) {
            // Добавляем вкладку с анимацией
            tabStrip.addView(albumsTabBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            albumsTabBtn.setAlpha(0f);
            albumsTabBtn.animate().alpha(1f).setDuration(200).start();
            if (viewPager.getAdapter() != null) viewPager.getAdapter().notifyDataSetChanged();
        } else if (!hasAlbums && tabVisible) {
            // Удаляем вкладку с анимацией, если мы не на ней — переключаемся
            if (viewPager.getCurrentItem() == 2) viewPager.setCurrentItem(1, true);
            albumsTabBtn.animate().alpha(0f).setDuration(200)
                    .withEndAction(() -> tabStrip.removeView(albumsTabBtn)).start();
            if (viewPager.getAdapter() != null) viewPager.getAdapter().notifyDataSetChanged();
        }
    }

    private void animateTabSwitch(TextView tab, boolean isActive, int accentColor, int inactiveColor, int activeTextColor) {
        if (isActive) {
            applyTabActive(tab, accentColor);
            tab.animate().scaleX(1.05f).scaleY(1.05f).setDuration(180)
                    .setInterpolator(new OvershootInterpolator(2f))
                    .withEndAction(() -> tab.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                    .start();
        } else {
            applyTabInactive(tab, inactiveColor);
            tab.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
        }
    }

    // ── Spring-отдача при клике на кнопки ──────────────────────────────────────
    private void applySpringClick(View view) {
        view.animate().scaleX(0.88f).scaleY(0.88f).setDuration(80).withEndAction(() ->
                view.animate().scaleX(1f).scaleY(1f)
                        .setDuration(300)
                        .setInterpolator(new OvershootInterpolator(3f))
                        .start()
        ).start();
    }

    // ── Затемнение/осветление цвета ────────────────────────────────────────────
    private int adjustColorBrightness(int color, float factor) {
        int r = Math.min(255, (int)(Color.red(color) * factor));
        int g = Math.min(255, (int)(Color.green(color) * factor));
        int b = Math.min(255, (int)(Color.blue(color) * factor));
        return Color.argb(Color.alpha(color), r, g, b);
    }
    private FrameLayout createWavePage(Context context) {

        // ── Корень ────────────────────────────────────────────────────────────
        FrameLayout root = new FrameLayout(context);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // ── Фон не задаем, чтобы WaveGLSurfaceView (MediaOverlay) был виден сквозь него ──

        // ── WaveGLSurfaceView теперь добавляется в content (на весь экран), чтобы лучи заходили за заголовок

        // ── Центральный блок поверх шара ───────────────────────────────
        LinearLayout centerBlock = new LinearLayout(context);
        centerBlock.setOrientation(LinearLayout.VERTICAL);
        centerBlock.setGravity(Gravity.CENTER);

        // -- Верхний ряд: [Play/Pause] Моя волна
        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER);
        
        playWaveBtn = new ImageView(context);
        playWaveBtn.setImageResource(R.drawable.ic_pause);
        playWaveBtn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        playWaveBtn.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN);
        ImageView playBtn = playWaveBtn;
        
        TextView waveTitle = new TextView(context);
        waveTitle.setText(getString(R.string.mywave));
        waveTitle.setTextColor(0xFFFFFFFF);
        waveTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
        waveTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        
        titleRow.addView(playBtn, LayoutHelper.createLinear(36, 36));
        titleRow.addView(waveTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 8, 0, 0, 0));

        // Инициализация состояния кнопки Play
        boolean isPlayingWave = AudioPlayerAlert.currentSource == AudioPlayerAlert.SOURCE_WAVE &&
                MediaController.getInstance().getPlayingMessageObject() != null &&
                !MediaController.getInstance().isMessagePaused();
        playBtn.setImageResource(isPlayingWave ? R.drawable.ic_pause : R.drawable.ic_play);

        titleRow.setOnClickListener(v -> {
            applySpringClick(v);
            MessageObject playingMsg = MediaController.getInstance().getPlayingMessageObject();
            if (AudioPlayerAlert.currentSource == AudioPlayerAlert.SOURCE_WAVE && playingMsg != null) {
                if (MediaController.getInstance().isMessagePaused()) {
                    MediaController.getInstance().playMessage(playingMsg);
                    playBtn.setImageResource(R.drawable.ic_pause);
                } else {
                    MediaController.getInstance().pauseMessage(playingMsg);
                    playBtn.setImageResource(R.drawable.ic_play);
                }
            } else {
                playBtn.setImageResource(R.drawable.ic_pause);
                waveWidget.startAnimation();
                AudioPlayerAlert.currentSource = AudioPlayerAlert.SOURCE_WAVE;
                AudioPlayerAlert.currentSourceLabel = "";
                try {
                    org.telegram.ui.Components.BulletinFactory.global().createSimpleBulletin(org.telegram.ui.LauncherIconController.getActiveIconDrawable(context), getString(R.string.sg_wave_generating)).show();
                } catch (Throwable e) {
                    android.widget.Toast.makeText(context, getString(R.string.sg_wave_generating), android.widget.Toast.LENGTH_SHORT).show();
                }
                SmartRadioEngine.stopWaveSession();
                SmartRadioEngine.generateWaveFromSettings(currentAccount, new SmartRadioEngine.WaveCallback() {
                    @Override public void onTracksFound(ArrayList<MessageObject> tracks) {
                        if (!tracks.isEmpty()) {
                            MediaController.getInstance().setPlaylist(tracks, tracks.get(0), MediaController.WAVE_CONTEXT_ID);
                            MediaController.getInstance().playMessage(tracks.get(0));
                        }
                    }
                    @Override public void onMoreTracksAdded(ArrayList<MessageObject> tracks) {
                        MediaController.getInstance().addTracksToPlaylist(tracks);
                    }
                    @Override public void onError() {
                    }
                });
            }
        });

        // -- Кнопка Настроить
        TextView genreDropBtn = new TextView(context);
        genreDropBtn.setGravity(Gravity.CENTER);
        genreDropBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        genreDropBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        genreDropBtn.setTextColor(0xFFFFFFFF);
        if (!android.text.TextUtils.isEmpty(SmartRadioEngine.currentConfig.query)) {
            genreDropBtn.setText("✦  " + getLocalizedGenre(SmartRadioEngine.currentConfig.query) + "  ▾");
        } else {
            genreDropBtn.setText("≡  " + getString(R.string.sg_configure));
        }
        
        {
            GradientDrawable gBg = new GradientDrawable();
            gBg.setColor(0x22FFFFFF); // Полупрозрачное стекло
            gBg.setCornerRadius(AndroidUtilities.dp(24));
            genreDropBtn.setBackground(gBg);
        }
        genreDropBtn.setPadding(
                AndroidUtilities.dp(20), AndroidUtilities.dp(10),
                AndroidUtilities.dp(20), AndroidUtilities.dp(10));
        genreDropBtn.setOnClickListener(v -> {
            applySpringClick(v);
            showWaveGenreSheet(context, genreDropBtn);
        });

        centerBlock.addView(titleRow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        root.addView(centerBlock, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER,
                0, 0, 0, 75)); // Компенсация сдвига ViewPager, чтобы попасть ровно в центр экрана

        root.addView(genreDropBtn, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL,
                0, 0, 0, 165)); // Под сферой (внизу экрана)

        return root;
    }

    @Override
    public View createView(Context context) {
        actionBar.setAddToContainer(false);
        actionBar.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        actionBar.setItemsBackgroundColor(android.graphics.Color.TRANSPARENT, false);

        // Корневой контейнер с фоном в стиле Яндекс Музыки
        SizeNotifierFrameLayout content = new SizeNotifierFrameLayout(context) {
            private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(Canvas canvas) {
                int bgColor = Theme.getColor(Theme.key_windowBackgroundWhite);
                bgPaint.setColor(bgColor);
                
                // Плавно скрываем фон приложения, чтобы показать WaveGLSurfaceView (MediaOverlay) под ним
                if (waveWidget != null && waveWidget.getVisibility() == View.VISIBLE) {
                    float waveAlpha = waveWidget.getAlpha();
                    bgPaint.setAlpha(Math.max(0, 255 - (int)(waveAlpha * 255)));
                } else {
                    bgPaint.setAlpha(255);
                }
                
                canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);
                
                if (waveWidget != null && waveWidget.getVisibility() == View.VISIBLE && waveWidget.getAlpha() > 0f && waveWidget.getAlpha() < 1f) {
                    invalidate();
                }
            }
            @Override
            protected void dispatchDraw(Canvas canvas) {
                if (android.os.Build.VERSION.SDK_INT >= 31 && scrollableViewNoiseSuppressor != null) {
                    blur3_InvalidateBlur();
                    final int width = getMeasuredWidth();
                    final int height = getMeasuredHeight();
                    if (iBlur3SourceGlass != null && !iBlur3SourceGlass.inRecording()) {
                        final Canvas c = iBlur3SourceGlass.beginRecording(width, height);
                        c.drawColor(getThemedColor(Theme.key_windowBackgroundWhite));
                        if (org.telegram.messenger.SharedConfig.chatBlurEnabled()) {
                            scrollableViewNoiseSuppressor.draw(c, org.telegram.ui.Components.blur3.DownscaleScrollableNoiseSuppressor.DRAW_GLASS);
                        }
                        iBlur3SourceGlass.endRecording();
                    }
                    iBlur3Invalidated = false;
                }
                super.dispatchDraw(canvas);
            }
        };

        // Register Window Insets listener to track navigation bar height
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            navigationBarHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom;
            return androidx.core.view.WindowInsetsCompat.CONSUMED;
        });

        // ── Инициализация WaveGLSurfaceView на ВЕСЬ ЭКРАН ──
        waveWidget = new WaveGLSurfaceView(context);
        waveWidget.setVisibility(View.GONE);
        // Immediately pause the GL renderer — it starts automatically on GLSurfaceView resume;
        // we will resume it only when the wave tab is actually visible to save GPU/battery.
        waveWidget.onPause();
        content.addView(waveWidget, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        fragmentView = content;

        LinearLayout mainColumn = new LinearLayout(context);
        mainColumn.setOrientation(LinearLayout.VERTICAL);

        // ── HEADER CONTAINER ──────────────────────────────────────────────────
        headerContainer = new LinearLayout(context);
        headerContainer.setOrientation(LinearLayout.VERTICAL);
        headerContainer.setClickable(true);
        headerContainer.setPadding(0, AndroidUtilities.statusBarHeight, 0, 0);

        // Стеклянный фон хедера убран по просьбе
        headerContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        artistRecyclerView = new RecyclerListView(context);
        artistRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        artistAdapter = new ArtistAdapter(context);
        artistRecyclerView.setAdapter(artistAdapter);
        artistRecyclerView.setVisibility(View.GONE);
        artistRecyclerView.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN || event.getAction() == android.view.MotionEvent.ACTION_MOVE) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            }
            return false;
        });

        albumsRecyclerView = new RecyclerListView(context);
        albumsRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        albumsAdapter = new AlbumAdapter(context);
        albumsRecyclerView.setAdapter(albumsAdapter);
        albumsRecyclerView.setVisibility(View.GONE);
        albumsRecyclerView.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN || event.getAction() == android.view.MotionEvent.ACTION_MOVE) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            }
            return false;
        });

        createSearchMenu(context, headerContainer);

        headerContainer.addView(artistRecyclerView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        headerContainer.addView(albumsRecyclerView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        mainColumn.addView(headerContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // ── TAB STRIP (парящая стеклянная панель) ──────────────────────────
        tabStrip = new LinearLayout(context);
        tabStrip.setOrientation(LinearLayout.HORIZONTAL);
        tabStrip.setGravity(Gravity.CENTER);
        tabStrip.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
        tabStrip.setBackground(SoundGramStyleManager.buildGlassDrawable(
                SoundGramStyleSettingsFragment.KEY_GLASS_PLAYLISTS,
                SoundGramStyleSettingsFragment.KEY_ALPHA_PLAYLISTS,
                SoundGramStyleSettingsFragment.KEY_COLOR_BG_PLAYLISTS,
                SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_PLAYLISTS,
                26f));
        if (android.os.Build.VERSION.SDK_INT >= 21)
            tabStrip.setElevation(AndroidUtilities.dp(6));

        int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);
        int inactiveColor = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);
        int activeTextColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);

        TextView waveTabBtn = createYmTab(context, getString(R.string.mywave));
        TextView libraryTabBtn = createYmTab(context, getString(R.string.lib));
        TextView albumsTabBtn = createYmTab(context, getString(R.string.sg_albums_tab));

        // Изначально активна вкладка Библиотека
        applyTabActive(libraryTabBtn, accentColor);
        applyTabInactive(waveTabBtn, inactiveColor);
        applyTabInactive(albumsTabBtn, inactiveColor);

        // Сохраняем ссылки для динамического управления
        albumsTabBtnRef = albumsTabBtn;
        tabAccentColor = accentColor;
        tabInactiveColor = inactiveColor;

        tabStrip.addView(waveTabBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
        tabStrip.addView(libraryTabBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
        // Вкладка альбомов добавляется только когда есть хоть 1 альбом
        if (!savedAlbumMessages.isEmpty()) {
            tabStrip.addView(albumsTabBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        }
        mainColumn.addView(tabStrip, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 16, 6, 16, 10));

        // ── VIEW PAGER ────────────────────────────────────────────────────────
        viewPager = new androidx.viewpager.widget.ViewPager(context) {
            private float initialX, initialY;
            private boolean isDisallowed = false;

            @Override
            public boolean onInterceptTouchEvent(android.view.MotionEvent ev) {
                if (isSearching) return false;
                int action = ev.getAction() & android.view.MotionEvent.ACTION_MASK;
                if (action == android.view.MotionEvent.ACTION_DOWN) {
                    initialX = ev.getX();
                    initialY = ev.getY();
                    isDisallowed = false;
                    super.onInterceptTouchEvent(ev);
                    return false;
                } else if (action == android.view.MotionEvent.ACTION_MOVE) {
                    float dx = ev.getX() - initialX;
                    float dy = ev.getY() - initialY;
                    if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > AndroidUtilities.dp(8)) {
                        int current = getCurrentItem();
                        int max = getAdapter() != null ? getAdapter().getCount() - 1 : 0;
                        if ((dx > 0 && current == 0) || (dx < 0 && current == max)) {
                            if (isDisallowed) {
                                getParent().requestDisallowInterceptTouchEvent(false);
                                isDisallowed = false;
                            }
                            return false;
                        } else {
                            if (!isDisallowed) {
                                getParent().requestDisallowInterceptTouchEvent(true);
                                isDisallowed = true;
                            }
                            return super.onInterceptTouchEvent(ev);
                        }
                    }
                } else if (action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_CANCEL) {
                    if (isDisallowed) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                        isDisallowed = false;
                    }
                }
                return super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean canScrollHorizontally(int direction) {
                if (isSearching) return false;
                int current = getCurrentItem();
                int max = getAdapter() != null ? getAdapter().getCount() - 1 : 0;
                if (current == max) {
                    return false;
                }
                if (current == 0 && direction < 0) {
                    return false;
                }
                return super.canScrollHorizontally(direction);
            }

            @Override
            public boolean onTouchEvent(android.view.MotionEvent ev) {
                if (isSearching) return false;
                int action = ev.getAction() & android.view.MotionEvent.ACTION_MASK;
                if (action == android.view.MotionEvent.ACTION_MOVE) {
                    float dx = ev.getX() - initialX;
                    float dy = ev.getY() - initialY;
                    if (Math.abs(dx) > Math.abs(dy)) {
                        int current = getCurrentItem();
                        int max = getAdapter() != null ? getAdapter().getCount() - 1 : 0;
                        if ((dx > 0 && current == 0) || (dx < 0 && current == max)) {
                            if (isDisallowed) {
                                getParent().requestDisallowInterceptTouchEvent(false);
                                isDisallowed = false;
                            }
                            return false;
                        }
                    }
                }
                return super.onTouchEvent(ev);
            }
        };
        viewPager.setOffscreenPageLimit(2);

        FrameLayout waveContainer = createWavePage(context);

        // ── PAGE 1: БИБЛИОТЕКА ────────────────────────────────────────────────
        LinearLayout libraryContainer = new LinearLayout(context);
        libraryContainer.setOrientation(LinearLayout.VERTICAL);

        // Заголовок библиотеки
        libraryListHeader = new LinearLayout(context);
        libraryListHeader.setOrientation(LinearLayout.VERTICAL);
        libraryListHeader.setPadding(0, 0, 0, 0);
        createHorizontalPlaylists(context, libraryListHeader);

        // Track list
        swipeRefreshLayout = new SwipeRefreshLayout(context);
        swipeRefreshLayout.setColorSchemeColors(accentColor);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadMusicFromChannel();
            swipeRefreshLayout.setRefreshing(false);
        });

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new MusicAdapter(context, null);
        listView.setAdapter(adapter);

        listView.addItemDecoration(new TrackItemDecoration(6, 12, false));
        listView.setClipToPadding(false);

        listView.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(androidx.recyclerview.widget.RecyclerView rv, int dx, int dy) {
                if (isSearching && !isSearchLoadingMore && hasMoreSearchResults && !TextUtils.isEmpty(currentSearchQuery)) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) listView.getLayoutManager();
                    if (layoutManager != null) {
                        int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                        int totalItemCount = adapter != null ? adapter.getItemCount() : 0;
                        if (totalItemCount > 0 && lastVisibleItem >= totalItemCount - 4) {
                            loadNextSearchPage();
                        }
                    }
                }
            }
        });

        swipeRefreshLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        libraryContainer.addView(libraryListHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        libraryContainer.addView(swipeRefreshLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1.0f));


            // ── PAGE 2: АЛЬБОМЫ ───────────────────────────────────────────────────
        FrameLayout albumsTabContainer = new FrameLayout(context);
        savedAlbumsListView = new RecyclerListView(context);
        savedAlbumsListView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(context, 2));
        savedAlbumsAdapter = new SavedAlbumsAdapter(context);
        savedAlbumsListView.setAdapter(savedAlbumsAdapter);
        savedAlbumsListView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(80));
        savedAlbumsListView.setClipToPadding(false);
        albumsTabContainer.addView(savedAlbumsListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // ── PAGER ADAPTER (динамический — 2 или 3 страницы) ──────────────────
        final FrameLayout finalWaveContainer = waveContainer;
        final LinearLayout finalLibraryContainer = libraryContainer;
        final FrameLayout finalAlbumsContainer = albumsTabContainer;
        viewPager.setAdapter(new androidx.viewpager.widget.PagerAdapter() {
            @Override public int getCount() { return savedAlbumMessages.isEmpty() ? 2 : 3; }
            @Override public boolean isViewFromObject(View view, Object object) { return view == object; }
            @Override public Object instantiateItem(ViewGroup container, int position) {
                View view = position == 0 ? finalWaveContainer : (position == 1 ? finalLibraryContainer : finalAlbumsContainer);
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
                if (isSearching) return;
                animateTabSwitch(waveTabBtn, position == 0, accentColor, inactiveColor, activeTextColor);
                animateTabSwitch(libraryTabBtn, position == 1, accentColor, inactiveColor, activeTextColor);
                animateTabSwitch(albumsTabBtn, position == 2, accentColor, inactiveColor, activeTextColor);
                if (position == 0 && waveWidget != null) {
                    waveWidget.onResume(); // resume GL renderer only on the Wave tab
                    setSphereVisible(true);
                    waveWidget.startAnimation();
                } else if (waveWidget != null) {
                    waveWidget.onPause(); // stop GL renderer on non-Wave tabs to save GPU/battery
                    setSphereVisible(false);
                    waveWidget.stopAnimation();
                }
            }
        });

        waveTabBtn.setOnClickListener(v -> { applySpringClick(v); SoundGramTTS.speak(v, waveTabBtn.getText()); viewPager.setCurrentItem(0, true); });
        libraryTabBtn.setOnClickListener(v -> { applySpringClick(v); SoundGramTTS.speak(v, libraryTabBtn.getText()); viewPager.setCurrentItem(1, true); });
        albumsTabBtn.setOnClickListener(v -> { applySpringClick(v); SoundGramTTS.speak(v, albumsTabBtn.getText()); viewPager.setCurrentItem(2, true); });

        viewPager.setCurrentItem(1, false);

        mainColumn.addView(viewPager, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1.0f));
        content.addView(mainColumn, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // ── Blur capture setup for liquid glass bottom tab bar ─────────────────
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                && scrollableViewNoiseSuppressor != null) {
            // Library tab (listView is the main RecyclerView on the Library page)
            final SizeNotifierFrameLayout finalContent = content;
            libraryBlurCapture = new org.telegram.ui.Components.blur3.ViewGroupPartRenderer(
                    listView, finalContent, listView::drawChild);
            listView.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(androidx.recyclerview.widget.RecyclerView rv, int dx, int dy) {
                    if (scrollableViewNoiseSuppressor != null) {
                        scrollableViewNoiseSuppressor.onScrolled(dx, dy);
                        blur3_InvalidateBlur();
                    }
                }
            });

            // Albums tab (savedAlbumsListView is the grid RecyclerView on the Albums page)
            albumsBlurCapture = new org.telegram.ui.Components.blur3.ViewGroupPartRenderer(
                    savedAlbumsListView, finalContent, savedAlbumsListView::drawChild);
            savedAlbumsListView.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(androidx.recyclerview.widget.RecyclerView rv, int dx, int dy) {
                    if (scrollableViewNoiseSuppressor != null) {
                        scrollableViewNoiseSuppressor.onScrolled(dx, dy);
                        blur3_InvalidateBlur();
                    }
                }
            });

            // Composite dispatcher — selects the correct capture based on the active tab
            iBlur3Capture = (canvas, position) -> {
                if (viewPager == null) return;
                int currentItem = viewPager.getCurrentItem();
                if (currentItem == 1) {
                    libraryBlurCapture.capture(canvas, position);
                } else if (currentItem == 2 && albumsBlurCapture != null) {
                    albumsBlurCapture.capture(canvas, position);
                }
            };
        }

        // ── MINI PLAYER ───────────────────────────────────────────────────────
        MiniPlayerView miniPlayerView = new MiniPlayerView(context, this);
        int bottomTabsOffset = app.nimarkogram.messenger.NimarkoConfig.showMainTabs ? 88 : 0;
        content.addView(miniPlayerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 64, Gravity.BOTTOM, 8, 0, 8, bottomTabsOffset + 8));
        SoundGramStyleManager.applyGlass(
                miniPlayerView,
                SoundGramStyleSettingsFragment.KEY_GLASS_MINI,
                SoundGramStyleSettingsFragment.KEY_ALPHA_MINI,
                SoundGramStyleSettingsFragment.KEY_COLOR_BG_MINI,
                SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_MINI,
                22f,
                () -> miniPlayerView.invalidate()
        );

        int listBottomPad = bottomTabsOffset + 88;
        listView.setPadding(0, 0, 0, AndroidUtilities.dp(listBottomPad));
        savedAlbumsListView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(listBottomPad));

        listView.setOnItemClickListener((view, position) -> {
            if (view != null && view.getContentDescription() != null) {
                SoundGramTTS.speak(view, view.getContentDescription());
            }
            // При запуске из библиотеки (listView.setOnItemClickListener):
            AudioPlayerAlert.currentSource = AudioPlayerAlert.SOURCE_LIBRARY;
            AudioPlayerAlert.currentSourceLabel = "";
            if (adapter.getItemViewType(position) == 2) return;
            if (isSearching) {
                if (position < webResultTracks.size()) {
                    MusicWebSearcher.WebTrack clickedTrack = webResultTracks.get(position);
                    if (TextUtils.isEmpty(clickedTrack.downloadUrl)) {
                        Toast.makeText(getParentActivity(), getString(R.string.sg_track_loading), Toast.LENGTH_SHORT).show();
                        new Thread(() -> {
                            MusicWebSearcher.WebTrack fresh = MusicWebSearcher.resolveHitmosSync(clickedTrack.title, clickedTrack.artist);
                            if (fresh != null && !TextUtils.isEmpty(fresh.downloadUrl)) {
                                clickedTrack.downloadUrl = fresh.downloadUrl;
                            }
                            AndroidUtilities.runOnUIThread(() -> {
                                if (TextUtils.isEmpty(clickedTrack.downloadUrl)) {
                                    Toast.makeText(getParentActivity(), getString(R.string.sg_download_error), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                ArrayList<MessageObject> queue = new ArrayList<>();
                                MessageObject targetMsg = null;
                                for (MusicWebSearcher.WebTrack wt : webResultTracks) {
                                    if (TextUtils.isEmpty(wt.downloadUrl)) continue;
                                    int durSec = parseDuration(wt.duration);
                                    MessageObject fakeMsg = createWebMessageObject(currentAccount, wt.title, wt.artist, wt.downloadUrl, durSec, -(long)wt.downloadUrl.hashCode(), wt.coverUrl);
                                    queue.add(fakeMsg);
                                    if (wt == clickedTrack) {
                                        targetMsg = fakeMsg;
                                    }
                                }
                                if (targetMsg != null) {
                                    playContext(targetMsg, queue, -Math.abs((long) "search_context".hashCode()));
                                }
                            });
                        }).start();
                        return;
                    }

                    ArrayList<MessageObject> queue = new ArrayList<>();
                    for (MusicWebSearcher.WebTrack wt : webResultTracks) {
                        if (TextUtils.isEmpty(wt.downloadUrl)) continue;
                        int durSec = parseDuration(wt.duration);
                        MessageObject fakeMsg = createWebMessageObject(currentAccount, wt.title, wt.artist, wt.downloadUrl, durSec, -(long)wt.downloadUrl.hashCode(), wt.coverUrl);
                        queue.add(fakeMsg);
                    }
                    MessageObject streamMsg = position < queue.size() ? queue.get(position) : (!queue.isEmpty() ? queue.get(0) : null);
                    if (streamMsg != null) {
                        playContext(streamMsg, queue, -Math.abs((long) "search_context".hashCode()));
                    }
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

        // ── Корневой контейнер ────────────────────────────────────────────────
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        // Жидкое стекло: полупрозрачный фон + скруглённые углы сверху
        int bgColor = Theme.getColor(Theme.key_dialogBackground);
        GradientDrawable sheetBg = new GradientDrawable();
        sheetBg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(bgColor, 242));
        float radius = AndroidUtilities.dp(28);
        sheetBg.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        sheetBg.setStroke(AndroidUtilities.dp(1),
                androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF, 40));
        container.setBackground(sheetBg);
        container.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(12));

        // ── Ручка drag ────────────────────────────────────────────────────────
        View handle = new View(context);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF, 55));
        handleBg.setCornerRadius(AndroidUtilities.dp(3));
        handle.setBackground(handleBg);
        FrameLayout handleWrapper = new FrameLayout(context);
        handleWrapper.addView(handle, new FrameLayout.LayoutParams(
                AndroidUtilities.dp(40), AndroidUtilities.dp(4), Gravity.CENTER));
        container.addView(handleWrapper, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 24, 0, 0, 0, 16));

        // ── Заголовок листа ───────────────────────────────────────────────────
        int accent = Theme.getColor(Theme.key_featuredStickers_addButton);

        TextView sheetTitle = new TextView(context);
        sheetTitle.setText(getString(R.string.sg_wave_settings_title));
        sheetTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        sheetTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        sheetTitle.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        sheetTitle.setGravity(Gravity.CENTER);
        container.addView(sheetTitle, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                20, 0, 20, 20));

        // ── Лейбл «ЖАНР» ─────────────────────────────────────────────────────
        TextView genreLabel = new TextView(context);
        genreLabel.setText(getString(R.string.sg_wave_genre_label));
        genreLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        genreLabel.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        genreLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        genreLabel.setLetterSpacing(0.10f);
        container.addView(genreLabel, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                20, 0, 20, 10));

        // ── Сетка жанров 3×N ─────────────────────────────────────────────────
        String[] genres = {
                "Любой жанр", "Поп", "Хип-Хоп", "Рэп", "Рок", "Инди",
                "Фонк", "Гиперпоп", "Пост-панк", "Эмо-рэп", "Металл",
                "Альтернатива", "К-поп", "Джаз", "Блюз", "Классика",
                "Электроника", "Синтвейв", "Ло-фай", "Эмбиент"
        };

        RecyclerListView genreListView = new RecyclerListView(context);
        genreListView.setLayoutManager(
                new androidx.recyclerview.widget.GridLayoutManager(context, 3));
        ChipsAdapter genreAdapter = new ChipsAdapter(context, genres,
                SmartRadioEngine.currentConfig.query);
        genreListView.setAdapter(genreAdapter);
        genreListView.setHasFixedSize(true);
        genreListView.setClipToPadding(false);
        genreListView.setPadding(AndroidUtilities.dp(12), 0,
                AndroidUtilities.dp(12), AndroidUtilities.dp(8));

        ScrollView genreScroll = new ScrollView(context);
        genreScroll.addView(genreListView);
        container.addView(genreScroll, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 340, 0, 0, 0, 14));

        // ── Кнопка «Применить» — градиентный фиолетовый ─────────────────────
        TextView applyBtn = new TextView(context);
        applyBtn.setText(getString(R.string.sg_wave_apply));
        applyBtn.setGravity(Gravity.CENTER);
        applyBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        applyBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        applyBtn.setTextColor(Theme.getColor(Theme.key_chats_actionIcon));

        GradientDrawable applyBg = new GradientDrawable();
        applyBg.setColor(Theme.getColor(Theme.key_chats_actionBackground));
        applyBg.setCornerRadius(AndroidUtilities.dp(16));
        applyBg.setStroke(AndroidUtilities.dp(1),
                androidx.core.graphics.ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), 55));
        applyBtn.setBackground(applyBg);
        if (Build.VERSION.SDK_INT >= 21) applyBtn.setElevation(AndroidUtilities.dp(4));

        applyBtn.setOnClickListener(v -> {
            applySpringClick(v);
            SmartRadioEngine.currentConfig.query = genreAdapter.getSelectedItem();
            if (updateTextTarget != null)
                updateTextTarget.setText("✦  " + SmartRadioEngine.currentConfig.query);
            builder.getDismissRunnable().run();
        });
        container.addView(applyBtn, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 52, 20, 0, 20, 0));

        builder.setCustomView(container);
        showDialog(builder.create());
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didReceiveNewMessages) {
            long dialogId = (Long) args[0];
            if (dialogId == getTargetChannelId() || dialogId == -getTargetChannelId()) {
                ArrayList<MessageObject> newMsgs = (ArrayList<MessageObject>) args[1];
                for (MessageObject msg : newMsgs) {
                    if (msg != null && msg.messageOwner != null && msg.messageOwner.message != null) {
                        String text = msg.messageOwner.message;
                        if (text.contains("\nicon=")) {
                            String[] lines = text.split("\n");
                            if (lines.length > 0) {
                                String title = lines[0].trim();
                                PlaylistSyncManager.updatePlaylistMsgId(currentAccount, title, msg.getId());
                            }
                        } else if (text.startsWith("SG_STREAM|||")) {
                            String[] p = text.split("\\|\\|\\|");
                            if (p.length >= 5) audioMessages.add(0, createWebMessageObject(currentAccount, p[1], p[2], p[3], parseDuration(p[4]), msg.getId(), p.length >= 7 ? p[6] : ""));
                        } else if (msg.isMusic()) {
                            audioMessages.add(0, msg);
                        }
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
            updateWavePlayButton();
        }
        else if (id == NotificationCenter.messagePlayingDidReset || id == NotificationCenter.messagePlayingPlayStateChanged) {
            safeNotifyAdapter(adapter, listView);
            updateWavePlayButton();
        }
    }

    private void updateWavePlayButton() {
        if (playWaveBtn == null) return;
        boolean isPlayingWave = AudioPlayerAlert.currentSource == AudioPlayerAlert.SOURCE_WAVE
                && MediaController.getInstance().getPlayingMessageObject() != null
                && !MediaController.getInstance().isMessagePaused();
        playWaveBtn.setImageResource(isPlayingWave ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void openSearch() {
        isSearching = true;
        // Анимация смены хедеров
        standardHeaderLayout.animate().alpha(0f).scaleX(0.95f).setDuration(150).withEndAction(() -> {
            standardHeaderLayout.setVisibility(View.GONE);
            searchHeaderLayout.setVisibility(View.VISIBLE);
            searchHeaderLayout.setAlpha(0f);
            searchHeaderLayout.setTranslationX(AndroidUtilities.dp(20));
            searchHeaderLayout.animate().alpha(1f).translationX(0f).setDuration(200)
                    .setInterpolator(new OvershootInterpolator(1.5f)).start();
        }).start();

        searchField.requestFocus();
        AndroidUtilities.showKeyboard(searchField);

        if (viewPager != null) viewPager.setCurrentItem(1, false);
        if (tabStrip != null) {
            tabStrip.animate().alpha(0f).translationY(-AndroidUtilities.dp(8)).setDuration(150)
                    .withEndAction(() -> tabStrip.setVisibility(View.GONE)).start();
        }
        if (libraryListHeader != null) {
            libraryListHeader.animate().alpha(0f).setDuration(120)
                    .withEndAction(() -> libraryListHeader.setVisibility(View.GONE)).start();
        }
        if (playlistsListView != null) playlistsListView.setVisibility(View.GONE);

        if (swipeRefreshLayout != null && swipeRefreshLayout.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) swipeRefreshLayout.getLayoutParams();
            lp.topMargin = 0;
            swipeRefreshLayout.setLayoutParams(lp);
        }
    }

    private void closeSearch() {
        isSearching = false;
        searchField.setText("");
        if (searchField.isFocused()) searchField.clearFocus();
        AndroidUtilities.hideKeyboard(searchField);
        if (fragmentView != null) AndroidUtilities.hideKeyboard(fragmentView);

        searchHeaderLayout.animate().alpha(0f).translationX(AndroidUtilities.dp(16)).setDuration(150)
                .withEndAction(() -> {
                    searchHeaderLayout.setVisibility(View.GONE);
                    standardHeaderLayout.setVisibility(View.VISIBLE);
                    standardHeaderLayout.setAlpha(0f);
                    standardHeaderLayout.setScaleX(0.95f);
                    standardHeaderLayout.animate().alpha(1f).scaleX(1f).setDuration(200)
                            .setInterpolator(new OvershootInterpolator(1.5f)).start();
                }).start();

        if (tabStrip != null) {
            tabStrip.setVisibility(View.VISIBLE);
            tabStrip.setAlpha(0f);
            tabStrip.setTranslationY(-AndroidUtilities.dp(8));
            tabStrip.animate().alpha(1f).translationY(0f).setDuration(220)
                    .setInterpolator(new OvershootInterpolator(1.5f)).start();
        }
        if (libraryListHeader != null) {
            libraryListHeader.setVisibility(View.VISIBLE);
            libraryListHeader.animate().alpha(1f).setDuration(180).start();
        }
        if (playlistsListView != null) playlistsListView.setVisibility(View.VISIBLE);

        artistRecyclerView.setVisibility(View.GONE);
        albumsRecyclerView.setVisibility(View.GONE);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LayoutHelper.MATCH_PARENT, 0);
        artistRecyclerView.setLayoutParams(p);
        albumsRecyclerView.setLayoutParams(p);

        if (swipeRefreshLayout != null && swipeRefreshLayout.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) swipeRefreshLayout.getLayoutParams();
            lp.topMargin = 0;
            swipeRefreshLayout.setLayoutParams(lp);
        }

        if (webResultTracks != null) webResultTracks.clear();
        if (searchResultMessages != null) searchResultMessages.clear();
        safeNotifyAdapter(adapter, listView);
    }
    public static void saveAlbumToLibrary(String title, String url, String cover) {
        File file = new File(getUserStorageDir(UserConfig.selectedAccount), "saved_albums.txt");
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
        File file = new File(getUserStorageDir(UserConfig.selectedAccount), "saved_albums.txt");
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
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        // Жидкое стекло: полупрозрачный диалоговый фон + скруглённые верхние углы
        int bgColor = Theme.getColor(Theme.key_dialogBackground);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(bgColor, 230));
        float r = AndroidUtilities.dp(24);
        gd.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        gd.setStroke(AndroidUtilities.dp(1),
                androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF, 35));
        container.setBackground(gd);
        if (Build.VERSION.SDK_INT >= 21) container.setElevation(AndroidUtilities.dp(8));
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(20),
                AndroidUtilities.dp(24), AndroidUtilities.dp(36));

        // Drag handle
        View handle = new View(getParentActivity());
        android.graphics.drawable.GradientDrawable handleBg = new android.graphics.drawable.GradientDrawable();
        handleBg.setColor(
                androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF, 65));
        handleBg.setCornerRadius(AndroidUtilities.dp(2));
        handle.setBackground(handleBg);
        FrameLayout handleWrapper = new FrameLayout(getParentActivity());
        handleWrapper.addView(handle, new FrameLayout.LayoutParams(
                AndroidUtilities.dp(36), AndroidUtilities.dp(4), Gravity.CENTER_HORIZONTAL));
        container.addView(handleWrapper,
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 20, 0, 0, 0, 20));

        // Иконка — фиолетовый круг с инфо-иконкой
        int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
        FrameLayout iconCircle = new FrameLayout(getParentActivity());
        android.graphics.drawable.GradientDrawable iconBg = new android.graphics.drawable.GradientDrawable();
        iconBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        iconBg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 40));
        iconBg.setStroke(AndroidUtilities.dp(2),
                androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 100));
        iconCircle.setBackground(iconBg);
        android.widget.ImageView iconView = new android.widget.ImageView(getParentActivity());
        iconView.setImageResource(R.drawable.msg_info);
        iconView.setColorFilter(accent, PorterDuff.Mode.SRC_IN);
        iconView.setScaleType(android.widget.ImageView.ScaleType.CENTER);
        iconCircle.addView(iconView,
                new FrameLayout.LayoutParams(AndroidUtilities.dp(28), AndroidUtilities.dp(28),
                        Gravity.CENTER));
        container.addView(iconCircle,
                LayoutHelper.createLinear(56, 56, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        // Заголовок
        TextView titleView = new TextView(getParentActivity());
        titleView.setText(getString(R.string.stat));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setGravity(Gravity.CENTER);
        container.addView(titleView,
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                        0, 0, 0, 20));

        // Данные статистики
        SharedPreferences prefs = SoundGramStatsManager.getStatsPrefs(currentAccount);
        int totalTracks = prefs.getInt("total_tracks", 0);
        long totalSeconds = prefs.getLong("total_seconds", 0L);
        long days = totalSeconds / 86400, hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60, seconds = totalSeconds % 60;
        StringBuilder timeFormatted = new StringBuilder();
        if (days > 0) timeFormatted.append(days).append(getString(R.string.day));
        if (hours > 0 || days > 0) timeFormatted.append(hours).append(getString(R.string.hour));
        if (minutes > 0 || hours > 0 || days > 0) timeFormatted.append(minutes).append(getString(R.string.min));
        timeFormatted.append(seconds).append(getString(R.string.sec));

        // Карточки статистики — стеклянные плитки
        LinearLayout cardsRow = new LinearLayout(getParentActivity());
        cardsRow.setOrientation(LinearLayout.HORIZONTAL);
        cardsRow.setGravity(Gravity.CENTER);

        cardsRow.addView(makeStatCard(getParentActivity(), "🎵", String.valueOf(totalTracks),
                getString(R.string.trc)), LayoutHelper.createLinear(0, 120, 1.0f, 0, 0, 8, 0));
        cardsRow.addView(makeStatCard(getParentActivity(), "⏱", timeFormatted.toString(),
                getString(R.string.tm)), LayoutHelper.createLinear(0, 120, 1.0f));

        container.addView(cardsRow,
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setCustomView(container);
        showDialog(builder.create());
    }

    // helper — стеклянная карточка для статистики
    private View makeStatCard(Context context, String emoji, String value, String label) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(14),
                AndroidUtilities.dp(8), AndroidUtilities.dp(14));

        int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 25));
        bg.setCornerRadius(AndroidUtilities.dp(18));
        bg.setStroke(AndroidUtilities.dp(1),
                androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 70));
        card.setBackground(bg);
        if (Build.VERSION.SDK_INT >= 21) card.setElevation(AndroidUtilities.dp(3));

        TextView emojiTv = new TextView(context);
        emojiTv.setText(emoji);
        emojiTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        emojiTv.setGravity(Gravity.CENTER);
        card.addView(emojiTv, LayoutHelper.createLinear(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 4));

        TextView valueTv = new TextView(context);
        valueTv.setText(value);
        valueTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        valueTv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        valueTv.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        valueTv.setGravity(Gravity.CENTER);
        valueTv.setMaxLines(2);
        valueTv.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(valueTv, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 2));

        TextView labelTv = new TextView(context);
        labelTv.setText(label);
        labelTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        labelTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        labelTv.setGravity(Gravity.CENTER);
        card.addView(labelTv, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        return card;
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

    public static MessageObject createWebMessageObject(int currentAccount, String title, String artist,
                                                       String url, int duration, long messageId, String coverUrl) {
        TLRPC.TL_message webMsg = new TLRPC.TL_message();
        webMsg.id = (int) messageId;
        String safeCover = android.text.TextUtils.isEmpty(coverUrl) ? "" : coverUrl;

        long trackHashId = Math.abs((long) url.hashCode());
        webMsg.message = "SG_STREAM|||" + title + "|||" + artist + "|||" + url + "|||"
                + duration + "|||" + trackHashId + "|||" + safeCover;
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

        // 1. Новый формат: Исполнитель - Название.mp3
        File fileByName = getLocalTrackFile(artist, title);
        if (fileByName.exists() && fileByName.length() > 1024) {
            msgObj.messageOwner.attachPath = fileByName.getAbsolutePath();
            return msgObj;
        }

        // 2. Старый формат: messageId.mp3
        if (messageId > 0) {
            File fileById = new File(getMusicStorageDir(), messageId + ".mp3");
            if (fileById.exists() && fileById.length() > 1024) {
                // Переименовываем в новый формат
                if (fileById.renameTo(fileByName)) {
                    msgObj.messageOwner.attachPath = fileByName.getAbsolutePath();
                } else {
                    msgObj.messageOwner.attachPath = fileById.getAbsolutePath();
                }
                return msgObj;
            }
        }

        // 3. Старый формат: hashId.mp3
        File fileByHash = new File(getMusicStorageDir(), trackHashId + ".mp3");
        if (fileByHash.exists() && fileByHash.length() > 1024) {
            if (fileByHash.renameTo(fileByName)) {
                msgObj.messageOwner.attachPath = fileByName.getAbsolutePath();
            } else {
                msgObj.messageOwner.attachPath = fileByHash.getAbsolutePath();
            }
            return msgObj;
        }

        // Файла нет — сохраняем исходный URL; финальный редирект резолвится асинхронно при воспроизведении
        msgObj.messageOwner.attachPath = url;
        return msgObj;
    }

    private void syncLocalLibraryDownloads() {
        if (!org.telegram.messenger.ApplicationLoader.isNetworkOnline()) return;
        ArrayList<MessageObject> snapshot = new ArrayList<>(audioMessages);

        new Thread(() -> {
            for (MessageObject msg : snapshot) {
                if (msg == null || msg.messageOwner == null) continue;

                // --- Нативные TG-аудио треки (не SG_STREAM) ---
                if (msg.messageOwner.message == null || msg.messageOwner.message.isEmpty()) {
                    if (msg.isMusic() && msg.getDocument() != null) {
                        File tgFile = FileLoader.getInstance(currentAccount).getPathToMessage(msg.messageOwner);
                        if (tgFile == null || !tgFile.exists() || tgFile.length() < 1024) {
                            // Скачиваем через TG FileLoader
                            AndroidUtilities.runOnUIThread(() ->
                                    FileLoader.getInstance(currentAccount).loadFile(
                                            msg.getDocument(), msg, FileLoader.PRIORITY_LOW, 1));
                        } else {
                            msg.messageOwner.attachPath = tgFile.getAbsolutePath();
                        }
                    }
                    continue;
                }

                if (!msg.messageOwner.message.startsWith("SG_STREAM|||")) continue;

                String[] parts = msg.messageOwner.message.split("\\|\\|\\|");
                if (parts.length < 4) continue;

                String trackTitle = parts.length > 1 ? parts[1] : "Unknown";
                String trackArtist = parts.length > 2 ? parts[2] : "Unknown";
                String downloadUrl = parts[3];

                // Целевой файл — новый формат имени
                File targetFile = getLocalTrackFile(trackArtist, trackTitle);

                if (targetFile.exists() && targetFile.length() > 1024) {
                    // Файл уже есть — обновляем attachPath
                    if (!targetFile.getAbsolutePath().equals(msg.messageOwner.attachPath)) {
                        msg.messageOwner.attachPath = targetFile.getAbsolutePath();
                    }
                    continue;
                }

                // Проверяем старые форматы
                long msgId = msg.getId();
                long hashId = 0;
                try { hashId = Long.parseLong(parts.length > 5 ? parts[5] : "0"); } catch (Exception ignored) {}
                if (hashId == 0) hashId = Math.abs((long) downloadUrl.hashCode());

                File oldById = msgId > 0 ? new File(getMusicStorageDir(), msgId + ".mp3") : null;
                File oldByHash = new File(getMusicStorageDir(), hashId + ".mp3");

                if (oldById != null && oldById.exists() && oldById.length() > 1024) {
                    oldById.renameTo(targetFile);
                    msg.messageOwner.attachPath = targetFile.exists()
                            ? targetFile.getAbsolutePath() : oldById.getAbsolutePath();
                    continue;
                }
                if (oldByHash.exists() && oldByHash.length() > 1024) {
                    oldByHash.renameTo(targetFile);
                    msg.messageOwner.attachPath = targetFile.exists()
                            ? targetFile.getAbsolutePath() : oldByHash.getAbsolutePath();
                    continue;
                }

                // Скачиваем в фоне
                FileLog.d("SoundGram: auto-downloading: " + trackArtist + " - " + trackTitle);
                boolean ok = MusicWebSearcher.downloadTrackToExactFileSync(downloadUrl, targetFile);
                if (ok) {
                    msg.messageOwner.attachPath = targetFile.getAbsolutePath();
                    FileLog.d("SoundGram: downloaded OK → " + targetFile.getName());
                } else {
                    msg.messageOwner.attachPath = downloadUrl;
                    FileLog.d("SoundGram: download failed, will stream original url: " + downloadUrl);
                }

                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            }
        }).start();
    }
    private void saveWebTrackToLibrary(MusicWebSearcher.WebTrack track) {
        if (track == null) return;
        if (TextUtils.isEmpty(track.downloadUrl)) {
            Toast.makeText(getParentActivity(), getString(R.string.sg_track_loading), Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(track.title) || track.title.equals("Unknown")) {
            Toast.makeText(getParentActivity(), getString(R.string.sg_track_title_unknown), Toast.LENGTH_SHORT).show();
            return;
        }

        long targetId = getTargetChannelId();
        if (targetId == 0) {
            Toast.makeText(getParentActivity(), getString(R.string.sg_library_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        String safeTitle  = track.title  != null ? track.title  : "Unknown";
        String safeArtist = track.artist != null ? track.artist : "Unknown";
        String safeDur    = track.duration != null ? track.duration : "0:00";
        String safeCover  = track.coverUrl != null ? track.coverUrl : "";

        final String dlUrl    = track.downloadUrl;
        final String dlArtist = safeArtist;
        final String dlTitle  = safeTitle;
        final long   fTargetId = targetId;
        final String fSafeDur  = safeDur;
        String tempCover = safeCover;

        new Thread(() -> {
            String fCover = tempCover;
            File destFile = getLocalTrackFile(dlArtist, dlTitle);
            boolean downloaded = destFile.exists() && destFile.length() > 1024;
            if (!downloaded) {
                downloaded = MusicWebSearcher.downloadTrackToExactFileSync(dlUrl, destFile);
            }
            if (!downloaded) {
                MusicWebSearcher.WebTrack freshTrack = MusicWebSearcher.resolveHitmosSync(dlTitle, dlArtist);
                if (freshTrack != null && !TextUtils.isEmpty(freshTrack.downloadUrl)) {
                    downloaded = MusicWebSearcher.downloadTrackToExactFileSync(freshTrack.downloadUrl, destFile);
                    if (TextUtils.isEmpty(fCover) && !TextUtils.isEmpty(freshTrack.coverUrl)) {
                        fCover = freshTrack.coverUrl;
                    }
                }
            }

            final boolean ok = downloaded && destFile.exists() && destFile.length() > 1024;
            final File finalFile = ok ? destFile : null;

            File coverFile = null;
            if (ok && finalFile != null && !TextUtils.isEmpty(fCover)) {
                try {
                    File cacheDir = new File(ApplicationLoader.applicationContext.getCacheDir(), "audio_covers");
                    if (!cacheDir.exists()) cacheDir.mkdirs();
                    File cFile = new File(cacheDir, Math.abs(finalFile.getAbsolutePath().hashCode()) + ".jpg");
                    if (!cFile.exists() || cFile.length() == 0) {
                        MusicWebSearcher.downloadTrackToExactFileSync(fCover, cFile);
                    }
                    if (cFile.exists() && cFile.length() > 0) {
                        coverFile = cFile;
                        localCoverCache.put(finalFile.getAbsolutePath(), cFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }

            final String thumbPath = coverFile != null ? coverFile.getAbsolutePath() : null;

            AndroidUtilities.runOnUIThread(() -> {
                if (ok && finalFile != null) {
                    sendLocalAudioFileToChannel(finalFile, dlTitle, dlArtist, fTargetId, currentAccount, thumbPath);
                    Toast.makeText(getParentActivity(), getString(R.string.sg_added_to_library), Toast.LENGTH_SHORT).show();
                    countBeforeUpload = audioMessages.size() + savedAlbumMessages.size();
                    isWaitingForUpload = true;
                    pollAttempts = 0;
                    AndroidUtilities.runOnUIThread(pollMessagesRunnable, 1500);
                } else {
                    Toast.makeText(getParentActivity(), getString(R.string.sg_download_error), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }


    /**
     * Отправляет локальный mp3-файл в канал-библиотеку как аудио-документ через TG.
     * Создаём TLRPC.TL_document с нулевым id (новый файл) и attachPath — TG сам загрузит файл.
     */
    public static void sendLocalAudioFileToChannel(File file, String title, String artist, long channelId, int account, String thumbPath) {
        if (file == null || !file.exists() || file.length() < 1024) return;
        try {
            File thumbFile = (thumbPath != null && new File(thumbPath).exists()) ? new File(thumbPath) : null;
            MusicWebSearcher.embedId3v2Cover(file, thumbFile, title, artist);

            long rawChannelId = Math.abs(channelId);
            long targetPeer = -rawChannelId;

            MessagesController mc = MessagesController.getInstance(account);
            TLRPC.Chat chat = mc.getChat(rawChannelId);
            long accessHash = getChannelHashFromFile(account);
            if (chat == null || !ChatObject.isChannel(chat)) {
                chat = AccountInstance.getInstance(account).getMessagesStorage().getChatSync(rawChannelId);
            }
            if (chat == null || !ChatObject.isChannel(chat)) {
                TLRPC.TL_channel newChannel = new TLRPC.TL_channel();
                newChannel.id = rawChannelId;
                newChannel.access_hash = accessHash;
                long uid = getValidUserId(account);
                newChannel.title = "id" + uid + "soundgram";
                newChannel.broadcast = true;
                chat = newChannel;
                mc.putChat(chat, true);
                ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                chats.add(chat);
                AccountInstance.getInstance(account).getMessagesStorage().putUsersAndChats(null, chats, true, true);
            } else if (chat.access_hash == 0 && accessHash != 0) {
                chat.access_hash = accessHash;
                mc.putChat(chat, true);
            }

            SendMessagesHelper.prepareSendingDocument(
                    AccountInstance.getInstance(account),
                    file.getAbsolutePath(),
                    file.getAbsolutePath(),
                    null,
                    null,
                    "audio/mpeg",
                    targetPeer,
                    null, null, null, null, null,
                    true, 0, null, null, false
            );
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void sendLocalAudioFileToChannel(File file, String title, String artist, long channelId, int account) {
        sendLocalAudioFileToChannel(file, title, artist, channelId, account, null);
    }

    /** Экземплярный wrapper для вызова внутри фрагмента */
    private void sendLocalAudioFileToChannel(File file, String title, String artist, long channelId) {
        sendLocalAudioFileToChannel(file, title, artist, channelId, currentAccount, null);
    }
    private static final java.util.concurrent.ConcurrentHashMap<String, String> localCoverCache = new java.util.concurrent.ConcurrentHashMap<>();

    public static byte[] extractEmbeddedCoverDirect(String path) {
        if (TextUtils.isEmpty(path)) return null;
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(path, "r")) {
            byte[] header = new byte[10];
            if (raf.read(header) < 10) return null;

            // ID3v2 Tag (MP3)
            if (header[0] == 'I' && header[1] == 'D' && header[2] == '3') {
                int tagSize = ((header[6] & 0x7F) << 21) | ((header[7] & 0x7F) << 14) | ((header[8] & 0x7F) << 7) | (header[9] & 0x7F);
                if (tagSize > 0 && tagSize < 10 * 1024 * 1024) {
                    byte[] tagData = new byte[tagSize];
                    raf.readFully(tagData);
                    int pos = 0;
                    while (pos < tagData.length - 10) {
                        if ((tagData[pos] == 'A' && tagData[pos+1] == 'P' && tagData[pos+2] == 'I' && tagData[pos+3] == 'C') ||
                            (tagData[pos] == 'P' && tagData[pos+1] == 'I' && tagData[pos+2] == 'C')) {
                            boolean isV22 = (tagData[pos] == 'P');
                            int frameHeaderSize = isV22 ? 6 : 10;
                            int frameSize = isV22 ?
                                (((tagData[pos+3] & 0xFF) << 16) | ((tagData[pos+4] & 0xFF) << 8) | (tagData[pos+5] & 0xFF)) :
                                (((tagData[pos+4] & 0xFF) << 24) | ((tagData[pos+5] & 0xFF) << 16) | ((tagData[pos+6] & 0xFF) << 8) | (tagData[pos+7] & 0xFF));
                            
                            int dataStart = pos + frameHeaderSize;
                            if (dataStart + frameSize <= tagData.length) {
                                for (int i = dataStart; i < dataStart + frameSize - 4; i++) {
                                    if ((tagData[i] == (byte)0xFF && tagData[i+1] == (byte)0xD8 && tagData[i+2] == (byte)0xFF) ||
                                        (tagData[i] == (byte)0x89 && tagData[i+1] == 'P' && tagData[i+2] == 'N' && tagData[i+3] == 'G')) {
                                        int imageLen = dataStart + frameSize - i;
                                        if (imageLen > 500) {
                                            byte[] img = new byte[imageLen];
                                            System.arraycopy(tagData, i, img, 0, imageLen);
                                            return img;
                                        }
                                    }
                                }
                            }
                        }
                        pos++;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static String getLocalAudioCoverPath(MessageObject msg) {
        if (msg == null) return null;
        File file = null;

        // 1. Прямой путь из attachPath
        if (msg.messageOwner != null && !TextUtils.isEmpty(msg.messageOwner.attachPath)) {
            File f = new File(msg.messageOwner.attachPath);
            if (f.exists() && f.length() > 1024) file = f;
        }

        // 2. Локальные файлы SoundGram по названию
        if (file == null) {
            String title = msg.getMusicTitle();
            String artist = msg.getMusicAuthor();
            if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(artist)) {
                File f = getLocalTrackFile(artist, title);
                if (f.exists() && f.length() > 1024) file = f;
            }
        }

        // 3. Файл из кеша Telegram (getPathToMessage и getPathToAttach)
        if (file == null && msg.messageOwner != null) {
            int acc = msg.currentAccount;
            try {
                File f1 = org.telegram.messenger.FileLoader.getInstance(acc).getPathToMessage(msg.messageOwner);
                if (f1 != null && f1.exists() && f1.length() > 1024) {
                    file = f1;
                } else if (msg.getDocument() != null) {
                    File f2 = org.telegram.messenger.FileLoader.getInstance(acc).getPathToAttach(msg.getDocument(), true);
                    if (f2 != null && f2.exists() && f2.length() > 1024) {
                        file = f2;
                    }
                }
            } catch (Exception ignored) {}
        }

        if (file == null) return null;

        String path = file.getAbsolutePath();
        if (localCoverCache.containsKey(path)) {
            String cached = localCoverCache.get(path);
            return TextUtils.isEmpty(cached) ? null : cached;
        }

        try {
            File cacheDir = new File(ApplicationLoader.applicationContext.getCacheDir(), "audio_covers");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File coverFile = new File(cacheDir, Math.abs(path.hashCode()) + ".jpg");
            if (coverFile.exists() && coverFile.length() > 0) {
                localCoverCache.put(path, coverFile.getAbsolutePath());
                return coverFile.getAbsolutePath();
            }

            byte[] art = null;
            try {
                android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                retriever.setDataSource(path);
                art = retriever.getEmbeddedPicture();
                retriever.release();
            } catch (Exception ignored) {}

            if (art == null || art.length == 0) {
                art = extractEmbeddedCoverDirect(path);
            }

            if (art != null && art.length > 0) {
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(coverFile)) {
                    fos.write(art);
                }
                localCoverCache.put(path, coverFile.getAbsolutePath());
                return coverFile.getAbsolutePath();
            }
        } catch (Exception ignore) {}

        localCoverCache.put(path, "");
        return null;
    }

    public static void setAudioCover(BackupImageView coverImage, MessageObject msg, String filter, int radius) {
        if (coverImage == null) return;
        coverImage.setRoundRadius(AndroidUtilities.dp(radius));
        android.graphics.drawable.Drawable placeholder = Theme.createRoundRectDrawable(AndroidUtilities.dp(radius), Theme.getColor(Theme.key_graySection));

        if (msg == null) {
            coverImage.setImageDrawable(placeholder);
            return;
        }

        // 1. Проверяем готовый bitmap обложки из воспроизводящего трека в MediaController
        MessageObject playingMsg = MediaController.getInstance().getPlayingMessageObject();
        if (playingMsg != null && (playingMsg == msg || (playingMsg.getId() != 0 && playingMsg.getId() == msg.getId()))) {
            AudioInfo audioInfo = MediaController.getInstance().getAudioInfo();
            if (audioInfo != null && audioInfo.getCover() != null) {
                coverImage.setImageBitmap(audioInfo.getCover());
                return;
            }
        }

        // 2. Проверяем вшитый audioCover в объекте MessageObject
        if (msg.audioCover != null) {
            coverImage.setImageBitmap(msg.audioCover);
            return;
        }

        // 3. Поддержка кастомных обложек для веб-треков (SG_STREAM)
        if (msg.messageOwner != null && msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) {
            String[] parts = msg.messageOwner.message.split("\\|\\|\\|");
            if (parts.length >= 7 && !TextUtils.isEmpty(parts[6])) {
                coverImage.setImage(parts[6], filter, placeholder);
                return;
            }
        }

        // 4. Локальная обложка из встроенных ID3 тегов MP3 файла
        String localCoverPath = getLocalAudioCoverPath(msg);
        if (localCoverPath != null) {
            coverImage.setImage(org.telegram.messenger.ImageLocation.getForPath(localCoverPath), filter, placeholder, msg);
            return;
        }

        // 5. Миниатюра из Telegram document.thumbs
        TLRPC.Document document = msg.getDocument();
        TLRPC.PhotoSize thumb = document != null ? org.telegram.messenger.FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 360) : null;
        if (!(thumb instanceof TLRPC.TL_photoSize) && !(thumb instanceof TLRPC.TL_photoSizeProgressive)) {
            thumb = null;
        }

        // 6. artworkUrl (iTunes / athumb)
        String artworkUrl = msg.getArtworkUrl(true);

        if (thumb != null && document != null) {
            coverImage.setImage(org.telegram.messenger.ImageLocation.getForDocument(thumb, document), filter, placeholder, msg);
        } else if (artworkUrl != null && !artworkUrl.isEmpty()) {
            coverImage.setImage(org.telegram.messenger.ImageLocation.getForPath(artworkUrl), filter, placeholder, msg);
        } else {
            coverImage.setImageDrawable(placeholder);
        }
    }

    public static void setAudioCover(RadialProgress2 radialProgress, MessageObject messageObject) {
        if (radialProgress == null) return;
        if (messageObject == null) {
            radialProgress.setImageOverlay(null, null, null);
            return;
        }

        // 1. Проверяем обложку у текущего играющего трека
        MessageObject playingMsg = MediaController.getInstance().getPlayingMessageObject();
        if (playingMsg != null && (playingMsg == messageObject || (playingMsg.getId() != 0 && playingMsg.getId() == messageObject.getId()))) {
            AudioInfo audioInfo = MediaController.getInstance().getAudioInfo();
            if (audioInfo != null && audioInfo.getCover() != null) {
                radialProgress.setImageOverlay(audioInfo.getCover());
                return;
            }
        }

        // 2. Вшитый audioCover
        if (messageObject.audioCover != null) {
            radialProgress.setImageOverlay(messageObject.audioCover);
            return;
        }

        // 3. Обложка для SG_STREAM трека из веб-поиска
        if (messageObject.messageOwner != null && messageObject.messageOwner.message != null && messageObject.messageOwner.message.startsWith("SG_STREAM|||")) {
            String[] parts = messageObject.messageOwner.message.split("\\|\\|\\|");
            if (parts.length >= 7 && !TextUtils.isEmpty(parts[6])) {
                radialProgress.setImageOverlay(parts[6]);
                return;
            }
        }

        // 4. Встроенный ID3 кавер из локального MP3 файла
        String localCoverPath = getLocalAudioCoverPath(messageObject);
        if (!TextUtils.isEmpty(localCoverPath)) {
            radialProgress.setImageOverlay(localCoverPath);
            return;
        }

        // 5. Миниатюра из document.thumbs
        TLRPC.Document document = messageObject.getDocument();
        TLRPC.PhotoSize thumb = document != null ? FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 360) : null;
        if (thumb instanceof TLRPC.TL_photoSize || thumb instanceof TLRPC.TL_photoSizeProgressive) {
            radialProgress.setImageOverlay(thumb, document, messageObject);
            return;
        }

        // 6. Remote artworkUrl
        String artworkUrl = messageObject.getArtworkUrl(true);
        if (!TextUtils.isEmpty(artworkUrl)) {
            radialProgress.setImageOverlay(artworkUrl);
            return;
        }

        radialProgress.setImageOverlay(null, null, null);
    }

    private void saveMessagesToCache(ArrayList<MessageObject> messages) {
        if (messages == null || messages.isEmpty()) return;
        try {
            File dir = getUserStorageDir(currentAccount);
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


    private void loadMessagesFromCache() {
        try {
            File dir = getUserStorageDir(currentAccount);
            if (dir == null) return;
            File file = new File(dir, "music_cache_v3.dat");
            if (!file.exists()) return;

            byte[] fileData = new byte[(int) file.length()];
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                fis.read(fileData);
            }

            SerializedData data = new SerializedData(fileData);
            int count = data.readInt32(false);

            ArrayList<MessageObject> cachedTracks = new ArrayList<>();
            ArrayList<MessageObject> cachedAlbums = new ArrayList<>();
            ArrayList<MessageObject> cachedPlaylists = new ArrayList<>();
            long selfUserId = getValidUserId(currentAccount);

            for (int i = 0; i < count; i++) {
                int constructor = data.readInt32(false);
                TLRPC.Message message = TLRPC.Message.TLdeserialize(data, constructor, false);
                if (message == null) continue;
                message.readAttachPath(data, selfUserId);

                if (message.message != null) {
                    if (message.message.startsWith("SG_STREAM|||")) {
                        String[] parts = message.message.split("\\|\\|\\|");
                        if (parts.length >= 5) {
                            String cover = parts.length > 6 ? parts[6] : "";
                            cachedTracks.add(createWebMessageObject(
                                    currentAccount, parts[1], parts[2], parts[3],
                                    parseDuration(parts[4]), message.id, cover));
                        }
                    } else if (message.message.startsWith("SG_ALBUM|||")) {
                        cachedAlbums.add(new MessageObject(currentAccount, message, false, false));
                    } else if (message.message.startsWith("SG_STATS|||")) {
                        globalStatsMessage = new MessageObject(currentAccount, message, false, false);
                    } else if (PlaylistSyncManager.isPlaylistMessage(message.message)) {
                        cachedPlaylists.add(new MessageObject(currentAccount, message, false, false));
                    } else {
                        MessageObject obj = new MessageObject(currentAccount, message, false, false);
                        if (obj.isMusic()) cachedTracks.add(obj);
                    }
                } else {
                    MessageObject obj = new MessageObject(currentAccount, message, false, false);
                    if (obj.isMusic()) cachedTracks.add(obj);
                }
            }
            data.cleanup();

            AndroidUtilities.runOnUIThread(() -> {
                audioMessages.clear();
                audioMessages.addAll(cachedTracks);
                savedAlbumMessages.clear();
                savedAlbumMessages.addAll(cachedAlbums);
                playlistMessagesList.clear();
                playlistMessagesList.addAll(cachedPlaylists);
                PlaylistSyncManager.syncOnChannelLoaded(currentAccount, getTargetChannelId(), cachedPlaylists);
                refreshPlaylistFiles();
                if (!org.telegram.messenger.ApplicationLoader.isNetworkOnline()) {
                    // Оффлайн: показываем только скачанные треки из кэша,
                    // затем подмешиваем локальные файлы, которых нет в кэше
                    filterOfflineTracks();
                }
                safeNotifyAdapter(adapter, listView);
                safeNotifyAdapter(playlistsAdapter, playlistsListView);
                safeNotifyAdapter(savedAlbumsAdapter, null);
                refreshAlbumsTabVisibility(albumsTabBtnRef, tabAccentColor, tabInactiveColor);

                // Запускаем скачивание недостающих файлов (для оффлайн работы)
                syncLocalLibraryDownloads();

                // Подмешиваем локальные файлы, которых нет в кэше/канале
                mergeLocalOnlyTracks();
            });
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
    private void loadMusicFromChannel() {
        long targetId = getTargetChannelId();
        if (targetId == 0) return;
        SoundGramStatsManager.startPeriodicSync(currentAccount, targetId);

        if (!org.telegram.messenger.ApplicationLoader.isNetworkOnline()) {
            loadMessagesFromCache();
            return;
        }

        fetchChannelHistoryPaginated(targetId, musicChannelHash, 0, new ArrayList<>(), 0);
    }

    private void fetchChannelHistoryPaginated(long channelId, long accessHash, int offsetId, ArrayList<TLRPC.Message> accumulatedMessages, int totalFetched) {
        TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
        TLRPC.InputPeer inputPeer = MessagesController.getInstance(currentAccount).getInputPeer(-channelId);
        if (inputPeer == null || inputPeer instanceof TLRPC.TL_inputPeerEmpty || (inputPeer.access_hash == 0 && accessHash != 0)) {
            TLRPC.TL_inputPeerChannel inputPeerChannel = new TLRPC.TL_inputPeerChannel();
            inputPeerChannel.channel_id = channelId;
            inputPeerChannel.access_hash = accessHash != 0 ? accessHash : getChannelHashFromFile(currentAccount);
            inputPeer = inputPeerChannel;
        }
        req.peer = inputPeer;
        req.offset_id = offsetId;
        req.offset_date = 0;
        req.add_offset = 0;
        req.limit = 100;
        req.max_id = 0;
        req.min_id = 0;
        req.hash = 0;

        org.telegram.tgnet.ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (error != null || !(response instanceof TLRPC.messages_Messages)) {
                if (accumulatedMessages.isEmpty()) {
                    AndroidUtilities.runOnUIThread(this::loadMessagesFromCache);
                    return;
                }
                processLoadedChannelMessages(accumulatedMessages);
                return;
            }

            TLRPC.messages_Messages messagesRes = (TLRPC.messages_Messages) response;
            if (messagesRes.messages != null && !messagesRes.messages.isEmpty()) {
                accumulatedMessages.addAll(messagesRes.messages);
                int lastId = messagesRes.messages.get(messagesRes.messages.size() - 1).id;
                int newTotal = totalFetched + messagesRes.messages.size();
                if (messagesRes.messages.size() == 100 && newTotal < 3000) {
                    fetchChannelHistoryPaginated(channelId, accessHash, lastId, accumulatedMessages, newTotal);
                    return;
                }
            }
            processLoadedChannelMessages(accumulatedMessages);
        });
    }

    private void processLoadedChannelMessages(ArrayList<TLRPC.Message> allMessages) {
        ArrayList<MessageObject> loadedTracks = new ArrayList<>();
        ArrayList<MessageObject> loadedAlbums = new ArrayList<>();
        ArrayList<MessageObject> loadedPlaylists = new ArrayList<>();
        HashSet<String> seenTrackKeys = new HashSet<>();

        for (TLRPC.Message msg : allMessages) {
            if (msg.message != null) {
                if (msg.message.startsWith("SG_STREAM|||")) {
                    String[] parts = msg.message.split("\\|\\|\\|");
                    if (parts.length >= 5) {
                        String dedupeKey = parts[3];
                        if (seenTrackKeys.contains(dedupeKey)) continue;
                        seenTrackKeys.add(dedupeKey);

                        String cover = parts.length > 6 ? parts[6] : "";
                        loadedTracks.add(createWebMessageObject(
                                currentAccount, parts[1], parts[2], parts[3],
                                parseDuration(parts[4]), msg.id, cover));
                    }
                } else if (msg.message.startsWith("SG_ALBUM|||")) {
                    loadedAlbums.add(new MessageObject(currentAccount, msg, false, false));
                } else if (msg.message.startsWith("SG_STATS|||")) {
                    globalStatsMessage = new MessageObject(currentAccount, msg, false, false);
                } else if (PlaylistSyncManager.isPlaylistMessage(msg.message)) {
                    loadedPlaylists.add(new MessageObject(currentAccount, msg, false, false));
                } else {
                    MessageObject mo = new MessageObject(currentAccount, msg, false, false);
                    if (mo.isMusic()) {
                        String k = trackKey(mo.getMusicAuthor(), mo.getMusicTitle());
                        if (!seenTrackKeys.contains(k)) {
                            seenTrackKeys.add(k);
                            loadedTracks.add(mo);
                        }
                    }
                }
            } else {
                MessageObject mo = new MessageObject(currentAccount, msg, false, false);
                if (mo.isMusic()) {
                    String k = trackKey(mo.getMusicAuthor(), mo.getMusicTitle());
                    if (!seenTrackKeys.contains(k)) {
                        seenTrackKeys.add(k);
                        loadedTracks.add(mo);
                    }
                }
            }
        }

        ArrayList<MessageObject> combined = new ArrayList<>(loadedTracks);
        combined.addAll(loadedAlbums);
        combined.addAll(loadedPlaylists);
        if (globalStatsMessage != null) combined.add(globalStatsMessage);

        final ArrayList<MessageObject> combinedSnapshot = new ArrayList<>(combined);
        new Thread(() -> saveMessagesToCache(combinedSnapshot)).start();

        AndroidUtilities.runOnUIThread(() -> {
            audioMessages.clear();
            audioMessages.addAll(loadedTracks);
            savedAlbumMessages.clear();
            savedAlbumMessages.addAll(loadedAlbums);
            playlistMessagesList.clear();
            playlistMessagesList.addAll(loadedPlaylists);

            PlaylistSyncManager.syncOnChannelLoaded(currentAccount, getTargetChannelId(), loadedPlaylists);
            refreshPlaylistFiles();

            safeNotifyAdapter(adapter, listView);
            safeNotifyAdapter(playlistsAdapter, playlistsListView);
            safeNotifyAdapter(savedAlbumsAdapter, null);
            refreshAlbumsTabVisibility(albumsTabBtnRef, tabAccentColor, tabInactiveColor);

            syncLocalLibraryDownloads();
            mergeLocalOnlyTracks();
        });
    }

    /**
     * Возвращает нормализованный ключ "исполнитель|||название" для дедупликации.
     * Регистр и пробелы приводятся к единому виду.
     */
    private static String trackKey(String artist, String title) {
        return artist.trim().toLowerCase() + "|||" + title.trim().toLowerCase();
    }

    /**
     * Собирает множество ключей из уже загруженного списка audioMessages.
     * Для SG_STREAM треков ключ берётся из текста сообщения,
     * для нативных аудио — из атрибутов документа.
     */
    private static HashSet<String> buildLoadedKeySet(ArrayList<MessageObject> messages) {
        HashSet<String> keys = new HashSet<>();
        for (MessageObject msg : messages) {
            if (msg == null || msg.messageOwner == null) continue;
            if (msg.messageOwner.message != null
                    && msg.messageOwner.message.startsWith("SG_STREAM|||")) {
                String[] p = msg.messageOwner.message.split("\\|\\|\\|");
                if (p.length >= 3) keys.add(trackKey(p[2], p[1]));
            } else if (msg.messageOwner.media != null
                    && msg.messageOwner.media.document != null) {
                for (TLRPC.DocumentAttribute a : msg.messageOwner.media.document.attributes) {
                    if (a instanceof TLRPC.TL_documentAttributeAudio) {
                        String t  = a.title     != null ? a.title     : "";
                        String ar = a.performer != null ? a.performer : "";
                        keys.add(trackKey(ar, t));
                        break;
                    }
                }
            }
        }
        return keys;
    }

    /**
     * Сканирует папку Music и добавляет в audioMessages все .mp3-файлы,
     * которых там нет — ни по каналу, ни по кэшу.
     *
     * Дедупликация по ключу "Исполнитель|||Название" (case-insensitive),
     * поэтому трек, загруженный из канала И лежащий на диске, считается одним.
     *
     * Вызывается и при успешной загрузке с канала, и при оффлайн-режиме,
     * поэтому локальная коллекция всегда полная, без дублей.
     *
     * Работает в фоновом потоке; результат применяется на UI-потоке.
     */
    private void mergeLocalOnlyTracks() {
        // Снимаем снапшот текущего списка до входа в фон
        final ArrayList<MessageObject> snapshot = new ArrayList<>(audioMessages);

        new Thread(() -> {
            File musicDir = getMusicStorageDir();
            File[] files = musicDir.listFiles(
                    (d, name) -> name.endsWith(".mp3") && !name.startsWith("."));
            if (files == null || files.length == 0) return;

            // Сортировка: сначала новые
            java.util.Arrays.sort(files,
                    (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

            HashSet<String> loadedKeys = buildLoadedKeySet(snapshot);
            ArrayList<MessageObject> localOnly = new ArrayList<>();

            for (File f : files) {
                if (f.length() < 1024) continue; // битый/пустой файл

                String rawName = f.getName().replace(".mp3", "");
                String artist = getString(R.string.sg_unknown_artist);
                String title  = rawName;
                int dashIdx = rawName.indexOf(" - ");
                if (dashIdx > 0) {
                    artist = rawName.substring(0, dashIdx).trim();
                    title  = rawName.substring(dashIdx + 3).trim();
                }

                // Если трек уже есть в канале/кэше — пропускаем
                if (loadedKeys.contains(trackKey(artist, title))) continue;
                // Трек был явно удалён пользователем — не возвращать через локальный скан.
                // Страховка на случай если файл ещё не успел удалиться с диска (гонка потоков).
                if (org.telegram.ui.Components.AudioPlayerAlert.deletedTrackKeys
                        .contains(trackKey(artist, title))) continue;

                TLRPC.TL_message localMsg = new TLRPC.TL_message();
                localMsg.id      = (int) (f.lastModified() % Integer.MAX_VALUE);
                localMsg.message = "";
                localMsg.date    = (int) (f.lastModified() / 1000);
                localMsg.peer_id = new TLRPC.TL_peerChannel();
                localMsg.media   = new TLRPC.TL_messageMediaDocument();
                localMsg.media.document = new TLRPC.TL_document();
                localMsg.media.document.mime_type = "audio/mp3";
                localMsg.media.document.size      = f.length();

                TLRPC.TL_documentAttributeAudio attr = new TLRPC.TL_documentAttributeAudio();
                attr.title     = title;
                attr.performer = artist;
                attr.duration  = 0;
                localMsg.media.document.attributes.add(attr);

                MessageObject msgObj = new MessageObject(currentAccount, localMsg, false, false);
                msgObj.messageOwner.attachPath = f.getAbsolutePath();
                localOnly.add(msgObj);
            }

            if (!localOnly.isEmpty()) {
                AndroidUtilities.runOnUIThread(() -> {
                    audioMessages.addAll(localOnly);
                    safeNotifyAdapter(adapter, listView);
                });
            }
        }).start();
    }

    /** @deprecated Заменён на {@link #mergeLocalOnlyTracks()}. Оставлен для совместимости. */
    @Deprecated
    private void loadLocalFilesAsFallback() {
        new Thread(() -> {
            File musicDir = getMusicStorageDir();
            File[] files = musicDir.listFiles((d, name) -> name.endsWith(".mp3") && !name.startsWith("."));
            if (files == null || files.length == 0) return;

            // СОРТИРОВКА: Сначала новые файлы
            java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

            ArrayList<MessageObject> localTracks = new ArrayList<>();

            for (File f : files) {
                boolean alreadyLoaded = false;
                for (MessageObject existing : audioMessages) {
                    if (existing.messageOwner != null && f.getAbsolutePath().equals(existing.messageOwner.attachPath)) {
                        alreadyLoaded = true;
                        break;
                    }
                }
                if (alreadyLoaded) continue;

                String rawName = f.getName().replace(".mp3", "");
                String artist = "Unknown";
                String title = rawName;
                int dashIdx = rawName.indexOf(" - ");
                if (dashIdx > 0) {
                    artist = rawName.substring(0, dashIdx).trim();
                    title  = rawName.substring(dashIdx + 3).trim();
                }

                TLRPC.TL_message localMsg = new TLRPC.TL_message();
                localMsg.id = (int) (f.lastModified() % Integer.MAX_VALUE);
                localMsg.message = "";
                localMsg.date = (int) (f.lastModified() / 1000);
                localMsg.peer_id = new TLRPC.TL_peerChannel();
                localMsg.media = new TLRPC.TL_messageMediaDocument();
                localMsg.media.document = new TLRPC.TL_document();
                localMsg.media.document.mime_type = "audio/mp3";
                localMsg.media.document.size = f.length();

                TLRPC.TL_documentAttributeAudio attr = new TLRPC.TL_documentAttributeAudio();
                attr.title = title;
                attr.performer = artist;
                attr.duration = 0;
                localMsg.media.document.attributes.add(attr);

                MessageObject msgObj = new MessageObject(currentAccount, localMsg, false, false);
                msgObj.messageOwner.attachPath = f.getAbsolutePath();
                localTracks.add(msgObj);
            }

            if (!localTracks.isEmpty()) {
                AndroidUtilities.runOnUIThread(() -> {
                    audioMessages.addAll(localTracks);
                    safeNotifyAdapter(adapter, listView);
                });
            }
        }).start();
    }
    public static android.graphics.drawable.GradientDrawable createLiquidGlassDrawable(int bgColor, int accentColor, int radiusDp) {
        android.graphics.drawable.GradientDrawable glassBg = new android.graphics.drawable.GradientDrawable();
        glassBg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(bgColor, 210));
        glassBg.setCornerRadius(AndroidUtilities.dp(radiusDp));
        glassBg.setStroke(AndroidUtilities.dp(1), androidx.core.graphics.ColorUtils.setAlphaComponent(accentColor, 60));
        return glassBg;
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
            layout.setGravity(Gravity.CENTER_HORIZONTAL);
            layout.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            layout.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(16));

            FrameLayout coverFrame = new FrameLayout(mContext);
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                coverFrame.setElevation(AndroidUtilities.dp(6));
            }

            BackupImageView cover = new BackupImageView(mContext);
            cover.setRoundRadius(AndroidUtilities.dp(16));
            coverFrame.addView(cover, LayoutHelper.createFrame(140, 140));

            View borderView = new View(mContext);
            GradientDrawable borderBg = new GradientDrawable();
            borderBg.setColor(Color.TRANSPARENT);
            borderBg.setCornerRadius(AndroidUtilities.dp(16));
            borderBg.setStroke(AndroidUtilities.dp(1.5f), 0x4DFFFFFF);
            borderView.setBackground(borderBg);
            coverFrame.addView(borderView, LayoutHelper.createFrame(140, 140));

            layout.addView(coverFrame, LayoutHelper.createLinear(140, 140, Gravity.CENTER_HORIZONTAL, 0, 5, 0, 8));

            TextView titleText = new TextView(mContext);
            titleText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            titleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            titleText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            titleText.setGravity(Gravity.CENTER);
            titleText.setMaxLines(2);
            titleText.setEllipsize(android.text.TextUtils.TruncateAt.END);
            layout.addView(titleText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 4, 0, 4, 0));

            return new RecyclerListView.Holder(layout);
        }

        @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            LinearLayout layout = (LinearLayout) holder.itemView;
            FrameLayout coverFrame = (FrameLayout) layout.getChildAt(0);
            BackupImageView cover = (BackupImageView) coverFrame.getChildAt(0);
            TextView titleText = (TextView) layout.getChildAt(1);

            MessageObject msgObj = savedAlbumMessages.get(position);
            String[] parts = msgObj.messageOwner.message.split("\\|\\|\\|");

            String title = parts.length > 1 ? parts[1] : "Unknown";
            String url = parts.length > 2 ? parts[2] : "";
            String coverUrl = parts.length > 3 ? parts[3] : "";
            String artist = parts.length > 4 ? parts[4] : "";

            titleText.setText(title);
            cover.setImage(coverUrl, "200_200", null);

            layout.setContentDescription(LocaleController.formatString("AccDescrMusicAlbum", R.string.AccDescrMusicAlbum, title, artist));
            layout.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

            layout.setOnClickListener(v -> {
                applySpringClick(layout);
                presentFragment(new AlbumProfileFragment(title, artist, url, coverUrl));
            });
        }
    }


    private void createSearchMenu(Context context, LinearLayout container) {
        int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);
        int bgColor = Theme.getColor(Theme.key_windowBackgroundWhite);

        FrameLayout topBar = new FrameLayout(context);

        // ── Стандартный хедер ─────────────────────────────────────────────────
        standardHeaderLayout = new LinearLayout(context);
        standardHeaderLayout.setOrientation(LinearLayout.HORIZONTAL);
        standardHeaderLayout.setGravity(Gravity.CENTER_VERTICAL);
        standardHeaderLayout.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);

        statsBtn = new ImageView(context);
        statsBtn.setImageResource(R.drawable.msg_info);
        statsBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.SRC_IN);
        statsBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
        statsBtn.setScaleType(ImageView.ScaleType.CENTER);
        statsBtn.setContentDescription(LocaleController.getString(R.string.AccDescrMusicSettings));
        statsBtn.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        statsBtn.setOnClickListener(v -> {
            applySpringClick(v);
            SoundGramTTS.speak(v, statsBtn.getContentDescription());
            setSphereVisible(false);
            // Показываем ПОЛНЫЙ ЭКРАН (не боттом-шит!)
            presentFragment(new SoundGramStyleSettingsFragment());
        });
        headerTitle = new TextView(context);
        headerTitle.setText("SoundGram");
        headerTitle.setContentDescription("SoundGram");
        headerTitle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        headerTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        headerTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        headerTitle.setGravity(Gravity.CENTER);
        headerTitle.setLetterSpacing(-0.02f);

        searchIconBtn = new ImageView(context);
        searchIconBtn.setImageResource(R.drawable.msg_search);
        searchIconBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.SRC_IN);
        searchIconBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
        searchIconBtn.setScaleType(ImageView.ScaleType.CENTER);
        searchIconBtn.setContentDescription(LocaleController.getString(R.string.Search));
        searchIconBtn.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        searchIconBtn.setOnClickListener(v -> { applySpringClick(v); SoundGramTTS.speak(v, searchIconBtn.getContentDescription()); openSearch(); });

        refreshBtn = new ImageView(context);
        refreshBtn.setImageResource(R.drawable.msg_download);
        refreshBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.SRC_IN);
        refreshBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
        refreshBtn.setScaleType(ImageView.ScaleType.CENTER);
        refreshBtn.setContentDescription(getString(R.string.ImportMusicTrack));
        refreshBtn.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        refreshBtn.setOnClickListener(v -> { applySpringClick(v); SoundGramTTS.speak(v, refreshBtn.getContentDescription()); showImportDialog(); });

        standardHeaderLayout.addView(statsBtn,
                LayoutHelper.createLinear(48, 48));
        standardHeaderLayout.addView(headerTitle,
                LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 8, 0, 0, 0));
        standardHeaderLayout.addView(searchIconBtn,
                LayoutHelper.createLinear(48, 48));

        // ── Поисковый хедер — стеклянный контейнер ───────────────────────────
        searchHeaderLayout = new LinearLayout(context);
        searchHeaderLayout.setOrientation(LinearLayout.HORIZONTAL);
        searchHeaderLayout.setGravity(Gravity.CENTER_VERTICAL);
        searchHeaderLayout.setVisibility(View.GONE);
        searchHeaderLayout.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(12), 0);

        // Кнопка назад с анимацией
        searchBackBtn = new ImageView(context);
        searchBackBtn.setImageResource(R.drawable.ic_ab_back);
        searchBackBtn.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
        searchBackBtn.setScaleType(ImageView.ScaleType.CENTER);
        searchBackBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
        searchBackBtn.setContentDescription(LocaleController.getString(R.string.Back));
        searchBackBtn.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        searchBackBtn.setOnClickListener(v -> { applySpringClick(v); SoundGramTTS.speak(v, searchBackBtn.getContentDescription()); closeSearch(); });

        // Стеклянный контейнер для строки поиска
        FrameLayout searchFieldContainer = new FrameLayout(context);
        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(
                Theme.getColor(Theme.key_windowBackgroundGray), 180));
        searchBg.setCornerRadius(AndroidUtilities.dp(20));
        searchBg.setStroke(AndroidUtilities.dp(1),
                androidx.core.graphics.ColorUtils.setAlphaComponent(accentColor, 50));
        searchFieldContainer.setBackground(searchBg);
        searchFieldContainer.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));

        ImageView searchInnerIcon = new ImageView(context);
        searchInnerIcon.setImageResource(R.drawable.msg_search);
        searchInnerIcon.setColorFilter(new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN));
        searchFieldContainer.addView(searchInnerIcon, LayoutHelper.createFrame(18, 18, Gravity.LEFT | Gravity.CENTER_VERTICAL));

        searchField = new EditText(context);
        searchField.setHint(getString(R.string.searchtrc));
        searchField.setContentDescription(getString(R.string.searchtrc));
        searchField.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        searchField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
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
                }, 200);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        searchField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                AndroidUtilities.hideKeyboard(searchField);
                return true;
            }
            return false;
        });

        searchFieldContainer.addView(searchField, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                Gravity.LEFT | Gravity.CENTER_VERTICAL, 26, 0, 0, 0));

        searchHeaderLayout.addView(searchBackBtn, LayoutHelper.createLinear(48, 48));
        searchHeaderLayout.addView(searchFieldContainer, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        topBar.addView(standardHeaderLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 52));
        topBar.addView(searchHeaderLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 52));
        container.addView(topBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52));
    }

    private String getAccountChannelIdFileName() {
        long userId = getValidUserId(currentAccount);
        return userId != 0 ? "music_channel_id_" + userId + ".txt" : "music_channel_id_acc" + currentAccount + ".txt";
    }

    private String getAccountChannelIdPrefKey() {
        long userId = getValidUserId(currentAccount);
        return userId != 0 ? "music_channel_id_" + userId : "music_channel_id_acc" + currentAccount;
    }

    private void saveChannelIdToFile(long channelId) {
        saveChannelIdToFile(channelId, 0);
    }

    private void saveChannelIdToFile(long channelId, long accessHash) {
        if (channelId == 0) return;
        musicChannelId = channelId;
        if (accessHash != 0) {
            musicChannelHash = accessHash;
        }
        long userId = getValidUserId(currentAccount);

        SharedPreferences.Editor editor = MessagesController.getNotificationsSettings(currentAccount).edit();
        if (userId != 0) {
            editor.putLong("music_channel_id_" + userId, channelId);
            if (accessHash != 0) editor.putLong("music_channel_hash_" + userId, accessHash);
        }
        editor.putLong("music_channel_id_acc" + currentAccount, channelId);
        if (accessHash != 0) editor.putLong("music_channel_hash_acc" + currentAccount, accessHash);
        editor.apply();

        File dir = getUserStorageDir(currentAccount);
        if (dir == null) return;
        File file = new File(dir, getAccountChannelIdFileName());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(channelId + (accessHash != 0 ? ":" + accessHash : ""));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static long getChannelHashFromFile(int account) {
        long userId = getValidUserId(account);
        if (userId != 0) {
            long prefHash = MessagesController.getNotificationsSettings(account).getLong("music_channel_hash_" + userId, 0);
            if (prefHash != 0) return prefHash;
        }
        long prefAccHash = MessagesController.getNotificationsSettings(account).getLong("music_channel_hash_acc" + account, 0);
        if (prefAccHash != 0) return prefAccHash;

        File dir = getUserStorageDir(account);
        if (dir != null) {
            File[] candidateFiles = userId != 0 ? new File[]{
                    new File(dir, "music_channel_id_" + userId + ".txt"),
                    new File(dir, "music_channel_id_acc" + account + ".txt"),
                    new File(dir, "music_channel_id_0.txt"),
                    new File(dir, "music_channel_id.txt")
            } : new File[]{
                    new File(dir, "music_channel_id_acc" + account + ".txt"),
                    new File(dir, "music_channel_id_0.txt"),
                    new File(dir, "music_channel_id.txt")
            };

            for (File file : candidateFiles) {
                if (file != null && file.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line = reader.readLine();
                        if (line != null && !TextUtils.isEmpty(line.trim())) {
                            String trimmed = line.trim();
                            if (trimmed.contains(":")) {
                                String[] parts = trimmed.split(":");
                                if (parts.length > 1) {
                                    long fHash = Long.parseLong(parts[1].trim());
                                    if (fHash != 0) {
                                        SharedPreferences.Editor editor = MessagesController.getNotificationsSettings(account).edit();
                                        if (userId != 0) {
                                            editor.putLong("music_channel_hash_" + userId, fHash);
                                        }
                                        editor.putLong("music_channel_hash_acc" + account, fHash);
                                        editor.apply();
                                        return fHash;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        }
        return 0;
    }

    public static long getChannelIdFromFile(int account) {
        long userId = getValidUserId(account);
        if (userId != 0) {
            long prefId = MessagesController.getNotificationsSettings(account).getLong("music_channel_id_" + userId, 0);
            if (prefId != 0) return prefId;
        }

        long prefAccId = MessagesController.getNotificationsSettings(account).getLong("music_channel_id_acc" + account, 0);
        if (prefAccId != 0) return prefAccId;

        if (account == 0) {
            long legacyId = MessagesController.getNotificationsSettings(account).getLong("music_channel_id", 0);
            if (legacyId != 0) return legacyId;
        }

        File dir = getUserStorageDir(account);
        if (dir != null) {
            File[] candidateFiles = userId != 0 ? new File[]{
                    new File(dir, "music_channel_id_" + userId + ".txt"),
                    new File(dir, "music_channel_id_acc" + account + ".txt"),
                    new File(dir, "music_channel_id_0.txt"),
                    new File(dir, "music_channel_id.txt")
            } : new File[]{
                    new File(dir, "music_channel_id_acc" + account + ".txt"),
                    new File(dir, "music_channel_id_0.txt"),
                    new File(dir, "music_channel_id.txt")
            };

            for (File file : candidateFiles) {
                if (file != null && file.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line = reader.readLine();
                        if (line != null && !TextUtils.isEmpty(line.trim())) {
                            String trimmed = line.trim();
                            String idStr = trimmed.contains(":") ? trimmed.split(":")[0].trim() : trimmed;
                            long id = Long.parseLong(idStr);
                            long fHash = 0;
                            if (trimmed.contains(":")) {
                                String[] parts = trimmed.split(":");
                                if (parts.length > 1) {
                                    try {
                                        fHash = Long.parseLong(parts[1].trim());
                                    } catch (Exception ignored) {}
                                }
                            }
                            if (id != 0) {
                                SharedPreferences.Editor editor = MessagesController.getNotificationsSettings(account).edit();
                                if (userId != 0) {
                                    editor.putLong("music_channel_id_" + userId, id);
                                    if (fHash != 0) editor.putLong("music_channel_hash_" + userId, fHash);
                                }
                                editor.putLong("music_channel_id_acc" + account, id);
                                if (fHash != 0) editor.putLong("music_channel_hash_acc" + account, fHash);
                                editor.apply();
                                return id;
                            }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        }
        return 0;
    }

    private long getChannelIdFromFile() {
        return getChannelIdFromFile(currentAccount);
    }

    private long getTargetChannelId() {
        if (musicChannelId != 0) return musicChannelId;
        return musicChannelId = getChannelIdFromFile();
    }

    private void deleteTrackFromChannel(MessageObject msg) {
        if (msg == null) return;
        long rawId = getChannelIdFromFile();
        if (rawId == 0) return;

        MessagesController.getInstance(currentAccount).deleteMessages(
                new ArrayList<>(Collections.singletonList(msg.getId())),
                null, null, -rawId, 0, true, 0);

        if (msg.messageOwner != null) {
            if (msg.messageOwner.attachPath != null) {
                File f = new File(msg.messageOwner.attachPath);
                if (f.exists() && f.getAbsolutePath().startsWith(getMusicStorageDir().getAbsolutePath())) {
                    f.delete();
                }
            }

            if (msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) {
                String[] parts = msg.messageOwner.message.split("\\|\\|\\|");
                if (parts.length >= 3) {
                    File byName = getLocalTrackFile(parts[2], parts[1]);
                    if (byName.exists()) byName.delete();
                }
                if (parts.length >= 4) {
                    long hashId = Math.abs((long) parts[3].hashCode());
                    new File(getMusicStorageDir(), hashId + ".mp3").delete();
                }
            }

            new File(getMusicStorageDir(), msg.getId() + ".mp3").delete();
        }

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
        playlistsListView.setClipToPadding(false);
        playlistsListView.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(4));
        playlistsListView.setOnItemClickListener((view, position) -> {
            applySpringClick(view);
            if (view != null && view.getContentDescription() != null) {
                SoundGramTTS.speak(view, view.getContentDescription());
            }
            if (position == 0) {
                showCreatePlaylistDialog();
            } else {
                File file = playlistFiles.get(position - 1);
                presentFragment(new PlaylistDetailFragment(file, getTargetChannelId(), this::refreshPlaylistFiles));
            }
        });

        playlistsContainer = new FrameLayout(context);
        playlistsContainer.addView(playlistsListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 168));

        emptyPlaylistsText = new TextView(context);
        emptyPlaylistsText.setText(getString(R.string.MusicNoPlaylists));
        emptyPlaylistsText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        emptyPlaylistsText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        emptyPlaylistsText.setGravity(Gravity.CENTER);
        emptyPlaylistsText.setVisibility(View.GONE);
        playlistsContainer.addView(emptyPlaylistsText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        container.addView(playlistsContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 4));

        refreshPlaylistFiles();
    }

    private void showCreatePlaylistDialog() {
        if (getTargetChannelId() == 0) {
            showSetupChannelPromptDialog();
            return;
        }
        Activity activity = getParentActivity();
        if (activity == null) activity = LaunchActivity.instance;
        if (activity == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(getString(R.string.nwpl));
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(6), AndroidUtilities.dp(24), 0);
        final EditText input = new EditText(activity);
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
                PlaylistSyncManager.PlaylistData data = new PlaylistSyncManager.PlaylistData();
                data.title = name;
                PlaylistSyncManager.writePlaylistFile(file, data);
                PlaylistSyncManager.syncPlaylistToChannel(currentAccount, getTargetChannelId(), file);
                refreshPlaylistFiles();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    public static File getPlaylistsDir(int account) {
        return PlaylistSyncManager.getPlaylistsDir(account);
    }

    public static File getPlaylistsDir() {
        return PlaylistSyncManager.getPlaylistsDir();
    }

    private void refreshPlaylistFiles() {
        playlistFiles.clear();
        File dir = PlaylistSyncManager.getPlaylistsDir(currentAccount);
        File[] files = dir != null ? dir.listFiles((d, name) -> name.endsWith(".txt")) : null;
        if (files != null) {
            for (File f : files) {
                PlaylistSyncManager.PlaylistData data = PlaylistSyncManager.readPlaylistFile(f);
                if (!data.isDeleted) {
                    playlistFiles.add(f);
                }
            }
        }
        safeNotifyAdapter(playlistsAdapter, playlistsListView);
    }

    private boolean setupPromptShowing = false;
    private int setupPromptRetryCount = 0;

    private void showSetupChannelPromptDialog() {
        if (setupPromptShowing) return;
        Activity activity = getParentActivity();
        if (activity == null) {
            activity = LaunchActivity.instance;
        }
        if (activity == null || activity.isFinishing()) {
            if (setupPromptRetryCount < 10) {
                setupPromptRetryCount++;
                AndroidUtilities.runOnUIThread(this::showSetupChannelPromptDialog, 500);
            }
            return;
        }
        setupPromptRetryCount = 0;
        setupPromptShowing = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(getString(R.string.SoundGram));
        builder.setMessage(getString(R.string.MusicStorageNeeded));
        builder.setPositiveButton(getString(R.string.MusicStorageCreateAuto), (dialog, which) -> {
            setupPromptShowing = false;
            long userId = getValidUserId(currentAccount);
            String title = "id" + userId + "soundgram";
            createMusicChannel(title);
        });
        builder.setNegativeButton(getString(R.string.MusicStorageLinkOwn), (dialog, which) -> {
            setupPromptShowing = false;
            showAdminChannelPicker();
        });
        builder.setOnDismissListener(dialog -> setupPromptShowing = false);
        builder.show();
    }

    private void showAdminChannelPicker() {
        Activity activity = getParentActivity();
        if (activity == null) activity = LaunchActivity.instance;
        if (activity == null) return;
        final Activity finalAct = activity;

        ArrayList<TLRPC.Chat> adminChats = new ArrayList<>();
        HashSet<Long> addedIds = new HashSet<>();

        ArrayList<TLRPC.Dialog> allDialogs = MessagesController.getInstance(currentAccount).getAllDialogs();
        if (allDialogs != null) {
            for (TLRPC.Dialog dialog : allDialogs) {
                if (dialog.id < 0) {
                    long chatId = -dialog.id;
                    TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(chatId);
                    if (chat != null && !addedIds.contains(chat.id)) {
                        boolean isAdmin = chat.creator || (chat.admin_rights != null && (chat.admin_rights.post_messages || chat.admin_rights.edit_messages || chat.admin_rights.change_info));
                        boolean isPublic = !TextUtils.isEmpty(chat.username);
                        if (isAdmin && isPublic && (ChatObject.isChannel(chat) || ChatObject.isMegagroup(chat))) {
                            adminChats.add(chat);
                            addedIds.add(chat.id);
                        }
                    }
                }
            }
        }

        ConcurrentHashMap<Long, TLRPC.Chat> chatsMap = MessagesController.getInstance(currentAccount).getChats();
        if (chatsMap != null) {
            for (TLRPC.Chat chat : chatsMap.values()) {
                if (chat != null && !addedIds.contains(chat.id)) {
                    boolean isAdmin = chat.creator || (chat.admin_rights != null && (chat.admin_rights.post_messages || chat.admin_rights.edit_messages || chat.admin_rights.change_info));
                    boolean isPublic = !TextUtils.isEmpty(chat.username);
                    if (isAdmin && isPublic && (ChatObject.isChannel(chat) || ChatObject.isMegagroup(chat))) {
                        adminChats.add(chat);
                        addedIds.add(chat.id);
                    }
                }
            }
        }

        long userId = getValidUserId(currentAccount);
        if (adminChats.isEmpty()) {
            AlertDialog.Builder emptyBuilder = new AlertDialog.Builder(finalAct);
            emptyBuilder.setTitle(getString(R.string.SoundGram));
            emptyBuilder.setMessage("Для хранения музыки требуется публичный канал или группа с публичной ссылкой (@username), в котором у вас есть права администратора.\n\nПодходящих публичных каналов не найдено. Создать персональный канал id" + userId + "soundgram автоматически?");
            emptyBuilder.setPositiveButton(getString(R.string.MusicStorageCreateAuto), (d, w) -> {
                createMusicChannel("id" + userId + "soundgram");
            });
            emptyBuilder.setNegativeButton(getString(R.string.cancel), null);
            emptyBuilder.show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(finalAct);
        builder.setTitle(getString(R.string.MusicStorageSelect));
        builder.setMessage("Выберите публичный канал или группу (@username) для хранения вашей музыки:");

        RecyclerListView listView = new RecyclerListView(finalAct);
        listView.setLayoutManager(new LinearLayoutManager(finalAct, LinearLayoutManager.VERTICAL, false));

        AlertDialog[] dialogRef = new AlertDialog[1];

        listView.setAdapter(new RecyclerListView.SelectionAdapter() {
            @Override
            public boolean isEnabled(RecyclerView.ViewHolder holder) {
                return true;
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                ProfileSearchCell cell = new ProfileSearchCell(finalAct);
                cell.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return new RecyclerListView.Holder(cell);
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                ProfileSearchCell cell = (ProfileSearchCell) holder.itemView;
                TLRPC.Chat chat = adminChats.get(position);
                String subtext = ChatObject.isChannelAndNotMegaGroup(chat) ? getString(R.string.MusicStorageChannel) : getString(R.string.MusicStorageGroup);
                if (!TextUtils.isEmpty(chat.username)) {
                    subtext += " • @" + chat.username;
                }
                if (chat.participants_count > 0) {
                    subtext += " • " + LocaleController.formatPluralString("Members", chat.participants_count);
                }
                cell.setData(chat, null, chat.title, subtext, false, false);
                cell.useSeparator = position != adminChats.size() - 1;
            }

            @Override
            public int getItemCount() {
                return adminChats.size();
            }
        });

        listView.setOnItemClickListener((view, position) -> {
            if (position >= 0 && position < adminChats.size()) {
                TLRPC.Chat selectedChat = adminChats.get(position);
                musicChannelId = selectedChat.id;
                musicChannelHash = selectedChat.access_hash;
                saveChannelIdToFile(musicChannelId, musicChannelHash);
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                Toast.makeText(finalAct, LocaleController.formatString("MusicStorageLinked", R.string.MusicStorageLinked, selectedChat.title), Toast.LENGTH_SHORT).show();
                loadMessagesFromCache();
                loadMusicFromChannel();
            }
        });

        builder.setView(listView);
        builder.setNegativeButton(getString(R.string.cancel), null);
        dialogRef[0] = builder.create();
        dialogRef[0].show();
    }

    private void createMusicChannel(String title) {
        Activity activity = getParentActivity();
        if (activity == null) activity = LaunchActivity.instance;
        final Activity finalAct = activity;

        long userId = getValidUserId(currentAccount);
        String channelName = "id" + userId + "soundgram";

        TLRPC.TL_channels_createChannel req = new TLRPC.TL_channels_createChannel();
        req.title = channelName; req.about = "SoundGram Storage"; req.broadcast = true; req.megagroup = false;
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (error == null && response instanceof TLRPC.Updates) {
                TLRPC.Updates updates = (TLRPC.Updates) response;
                MessagesController.getInstance(currentAccount).processUpdates(updates, false);
                for (TLRPC.Chat chat : updates.chats) {
                    if (chat != null) {
                        musicChannelId = chat.id;
                        musicChannelHash = chat.access_hash;
                        AndroidUtilities.runOnUIThread(() -> {
                            saveChannelIdToFile(musicChannelId, musicChannelHash);
                            loadMusicFromChannel();
                            if (finalAct != null) {
                                Toast.makeText(finalAct, getString(R.string.MusicStorageCreated), Toast.LENGTH_SHORT).show();
                            }
                        });

                        TLRPC.TL_channels_updateUsername updateUsernameReq = new TLRPC.TL_channels_updateUsername();
                        TLRPC.TL_inputChannel inputChannel = new TLRPC.TL_inputChannel();
                        inputChannel.channel_id = chat.id;
                        inputChannel.access_hash = chat.access_hash;
                        updateUsernameReq.channel = inputChannel;
                        updateUsernameReq.username = channelName;
                        ConnectionsManager.getInstance(currentAccount).sendRequest(updateUsernameReq, (uResp, uErr) -> {
                            if (uErr == null && uResp instanceof TLRPC.Updates) {
                                MessagesController.getInstance(currentAccount).processUpdates((TLRPC.Updates) uResp, false);
                            }
                        });
                        break;
                    }
                }
            } else {
                AndroidUtilities.runOnUIThread(() -> {
                    if (finalAct != null) {
                        Toast.makeText(finalAct, LocaleController.formatString("MusicStorageError", R.string.MusicStorageError, (error != null ? error.text : "unknown error")), Toast.LENGTH_LONG).show();
                    }
                });
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

        // Для SG_STREAM — проверяем наличие локального файла
        if (track.messageOwner != null && track.messageOwner.message != null
                && track.messageOwner.message.startsWith("SG_STREAM|||")) {

            String[] parts = track.messageOwner.message.split("\\|\\|\\|");
            if (parts.length >= 4) {
                String trackTitle  = parts.length > 1 ? parts[1] : "";
                String trackArtist = parts.length > 2 ? parts[2] : "";
                String streamUrl   = parts[3];

                File byName = getLocalTrackFile(trackArtist, trackTitle);
                if (byName.exists() && byName.length() > 1024) {
                    track.messageOwner.attachPath = byName.getAbsolutePath();
                } else {
                    long msgId = track.getId();
                    File byId = msgId > 0 ? new File(getMusicStorageDir(), msgId + ".mp3") : null;
                    if (byId != null && byId.exists() && byId.length() > 1024) {
                        track.messageOwner.attachPath = byId.getAbsolutePath();
                    } else {
                        long hashId = Math.abs((long) streamUrl.hashCode());
                        File byHash = new File(getMusicStorageDir(), hashId + ".mp3");
                        if (byHash.exists() && byHash.length() > 1024) {
                            track.messageOwner.attachPath = byHash.getAbsolutePath();
                        } else if (!org.telegram.messenger.ApplicationLoader.isNetworkOnline()) {
                            if (getParentActivity() != null) {
                                Toast.makeText(getParentActivity(), getString(R.string.sg_no_connection_track_not_downloaded), Toast.LENGTH_SHORT).show();
                            }
                            return;
                        } else {
                            final MessageObject finalTrack = track;
                            final ArrayList<MessageObject> finalQueue = queue;
                            final long finalContextId = contextId;
                            final String originalUrl = streamUrl;

                            // ИСПРАВЛЕНИЕ VPN: Передаём оригинальный URL напрямую в ExoPlayer.
                            // ExoPlayer с setAllowCrossProtocolRedirects(true) и правильными заголовками
                            // (Referer, User-Agent) сам пройдёт все редиректы через VPN TUN-интерфейс
                            // в момент воспроизведения — это надёжнее чем резолв заранее,
                            // который привязывался к IP/сессии VPN и устаревал.
                            String streamingUrl = originalUrl;

                            AndroidUtilities.runOnUIThread(() -> {
                                finalTrack.messageOwner.attachPath = streamingUrl;
                                for (MessageObject m : finalQueue) { m.audioProgress = 0.0f; m.audioProgressSec = 0; }
                                mediaController.setPlaylist(new ArrayList<>(finalQueue), finalTrack, finalContextId);
                                mediaController.seekToProgress(finalTrack, 0);
                                mediaController.playMessage(finalTrack);
                            });
                            return;
                        }
                    }
                }
            }
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
            allTracks = ChannelMusicFragment.getAudioMessages(currentAccount);
            if (playlistFile != null && playlistFile.exists()) {
                PlaylistSyncManager.PlaylistData pData = PlaylistSyncManager.readPlaylistFile(playlistFile);
                for (String line : pData.trackLines) {
                    try {
                        if (line.startsWith("SG_STREAM|||")) {
                            String[] p = line.split("\\|\\|\\|");
                            if (p.length >= 4) playlistIds.add(p[3].hashCode());
                        } else {
                            playlistIds.add(Integer.parseInt(line.trim()));
                        }
                    } catch (Exception ignored) {}
                }
            }
            return true;
        }

        @Override
        public void onFragmentDestroy() {
            super.onFragmentDestroy();

            PlaylistSyncManager.PlaylistData pData = PlaylistSyncManager.readPlaylistFile(playlistFile);
            ArrayList<String> existingOrder = new ArrayList<>(pData.trackLines);
            HashSet<String> existingSet = new HashSet<>(existingOrder);

            // ── Находим новые добавленные треки ──
            ArrayList<String> newTracks = new ArrayList<>();
            for (MessageObject msg : allTracks) {
                if (msg.messageOwner == null) continue;

                String identifier;
                int idToCheck;
                if (msg.messageOwner.message != null
                        && msg.messageOwner.message.startsWith("SG_STREAM|||")) {
                    identifier = msg.messageOwner.message;
                    String[] p = identifier.split("\\|\\|\\|");
                    if (p.length < 4) continue;
                    idToCheck = p[3].hashCode();
                } else {
                    identifier = String.valueOf(msg.getId());
                    idToCheck = msg.getId();
                }

                if (playlistIds.contains(idToCheck) && !existingSet.contains(identifier)) {
                    newTracks.add(identifier);
                }
            }

            // ── Строим финальный список: СНАЧАЛА НОВЫЕ ТРЕКИ, ЗАТЕМ СТАРЫЕ ──
            java.util.LinkedHashSet<String> finalSet = new java.util.LinkedHashSet<>();

            // 1. Новые треки добавляем в НАЧАЛО плейлиста
            for (String newTrack : newTracks) {
                finalSet.add(newTrack);
            }

            // 2. Ранее сохранённые треки (если не сняты с отметки) сохраняют свой прежний порядок
            for (String idStr : existingOrder) {
                int idToCheck;
                try {
                    idToCheck = idStr.startsWith("SG_STREAM|||")
                            ? idStr.split("\\|\\|\\|")[3].hashCode()
                            : Integer.parseInt(idStr.trim());
                } catch (Exception e) { continue; }

                if (playlistIds.contains(idToCheck)) {
                    finalSet.add(idStr);
                }
            }

            pData.trackLines.clear();
            pData.trackLines.addAll(finalSet);
            PlaylistSyncManager.writePlaylistFile(playlistFile, pData);

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

            SizeNotifierFrameLayout content = new SizeNotifierFrameLayout(context);
            content.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            fragmentView = content;

            listView = new RecyclerListView(context);
            listView.setLayoutManager(new LinearLayoutManager(context));
            adapter = new AddTracksAdapter(context);
            listView.setAdapter(adapter);

            listView.addItemDecoration(new TrackItemDecoration(6, 12, false));
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
                    cell.optionsButton.setContentDescription(getString(R.string.AccDescrMusicAdded));
                    cell.optionsButton.setColorFilter(Theme.getColor(Theme.key_chats_actionBackground), PorterDuff.Mode.SRC_IN);
                } else {
                    cell.optionsButton.setImageResource(R.drawable.msg_add);
                    cell.optionsButton.setContentDescription(getString(R.string.AccDescrMusicAdd));
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
        private boolean hasPendingEdits = false;

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
            hasOwnBackground = true;
            loadPlaylistData();
            return true;
        }

        @Override
        public void onFragmentDestroy() {
            super.onFragmentDestroy();
            if (hasPendingEdits) {
                savePlaylistOrder();
            }
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

                    PlaylistSyncManager.PlaylistData pData = PlaylistSyncManager.readPlaylistFile(playlistFile);
                    PlaylistSyncManager.writePlaylistFile(playlistFile, pData);

                    hasPendingEdits = true;
                    if (listener != null) listener.onPlaylistUpdated();
                    safeNotifyAdapter(adapter, listView);
                } catch (Exception e) {}
            }
        }

        @Override
        public boolean hasOwnBackground() {
            return true;
        }

        @Override
        public View createView(Context context) {
            hasOwnBackground = true;
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.setTitle("");
            actionBar.setOccupyStatusBar(true);

            ActionBarMenu menu = actionBar.createMenu();
            ActionBarMenuItem addBtn = menu.addItem(1, R.drawable.msg_add);
            addBtn.setContentDescription(getString(R.string.addtrc));
            ActionBarMenuItem editItem = menu.addItem(2, R.drawable.msg_edit);
            editItem.setContentDescription(getString(R.string.edit));
            ActionBarMenuItem iconItem = menu.addItem(4, R.drawable.msg_gallery_locked1);
            iconItem.setVisibility(View.GONE);
            ActionBarMenuItem deleteBtn = menu.addItem(3, R.drawable.msg_delete);
            deleteBtn.setContentDescription(getString(R.string.Delete));

            actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
            actionBar.setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle));
            actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false);
            actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false);
            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) finishFragment();
                    else if (id == 1) {
                        presentFragment(new AddTracksFragment(playlistFile, () -> {
                            hasPendingEdits = true;
                            loadPlaylistData();
                        }));
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
                            hasPendingEdits = true;
                            editItem.setIcon(R.drawable.ic_ab_done);
                            Toast.makeText(context, getString(R.string.holdtrc), Toast.LENGTH_SHORT).show();
                            menu.setItemVisibility(4, View.VISIBLE);
                        } else {
                            editItem.setIcon(R.drawable.msg_edit);
                            savePlaylistOrder();
                            menu.setItemVisibility(4, View.GONE);
                        }
                    }
                }
            });

            SizeNotifierFrameLayout content = new SizeNotifierFrameLayout(context);
            content.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            fragmentView = content;

            listView = new RecyclerListView(context);
            listView.setLayoutManager(new LinearLayoutManager(context));
            adapter = new PlaylistMusicAdapter(context);
            listView.setAdapter(adapter);
            listView.addItemDecoration(new TrackItemDecoration(6, 12, true));
            listView.setPadding(0, 0, 0, AndroidUtilities.dp(88));
            listView.setClipToPadding(false);
            listView.setOnItemClickListener((view, position) -> {
                // При запуске из плейлиста (PlaylistDetailFragment):
                AudioPlayerAlert.currentSource = AudioPlayerAlert.SOURCE_PLAYLIST;
                AudioPlayerAlert.currentSourceLabel = currentPlaylistName;
                if (adapter.getItemViewType(position) == 2) return;
                if (position > 0 && position <= playlistMessages.size()) {
                    playStrictInPlaylist(playlistMessages.get(position - 1));
                }
            });

            content.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            MiniPlayerView miniPlayerView = new MiniPlayerView(context, this);
            content.addView(miniPlayerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 64, Gravity.BOTTOM, 8, 0, 8, 8));
            SoundGramStyleManager.applyGlass(
                    miniPlayerView,
                    SoundGramStyleSettingsFragment.KEY_GLASS_MINI,
                    SoundGramStyleSettingsFragment.KEY_ALPHA_MINI,
                    SoundGramStyleSettingsFragment.KEY_COLOR_BG_MINI,
                    SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_MINI,
                    22f,
                    () -> miniPlayerView.invalidate()
            );
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
                    hasPendingEdits = true;
                    return true;
                }

                @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) { }

                @Override
                public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
                    hasPendingEdits = true;
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
            HashSet<String> seenKeys = new HashSet<>();

            if (playlistFile != null && playlistFile.exists()) {
                PlaylistSyncManager.PlaylistData pData = PlaylistSyncManager.readPlaylistFile(playlistFile);
                if (pData.isDeleted) return;

                for (String line : pData.trackLines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    if (line.startsWith("SG_STREAM|||")) {
                        String[] parts = line.split("\\|\\|\\|");
                        if (parts.length >= 5) {
                            String dedupeKey = parts[3];
                            if (seenKeys.contains(dedupeKey)) continue;
                            seenKeys.add(dedupeKey);

                            int dur = 0;
                            try { dur = Integer.parseInt(parts[4]); } catch (Exception e){}
                            playlistMessages.add(createWebMessageObject(
                                    currentAccount, parts[1], parts[2], parts[3],
                                    dur, parts[3].hashCode(), parts.length > 6 ? parts[6] : ""));
                        }
                    } else {
                        try {
                            int id = Integer.parseInt(line);
                            String dedupeKey = "id:" + id;
                            if (seenKeys.contains(dedupeKey)) continue;
                            seenKeys.add(dedupeKey);

                            for (MessageObject msg : ChannelMusicFragment.getAudioMessages(currentAccount)) {
                                if (msg.getId() == id) {
                                    playlistMessages.add(msg);
                                    break;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
            safeNotifyAdapter(adapter, listView);
            if (listener != null) listener.onPlaylistUpdated();
        }

        private void playStrictInPlaylist(MessageObject messageObject) {
            if (hasPendingEdits) {
                savePlaylistOrder();
            }
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
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(getString(R.string.AppName));
            builder.setMessage(getString(R.string.sg_delete_playlist_confirm));
            builder.setPositiveButton(getString(R.string.sg_delete_playlist), (dialog, which) -> {
                PlaylistSyncManager.PlaylistData data = PlaylistSyncManager.readPlaylistFile(playlistFile);
                PlaylistSyncManager.deletePlaylistFromChannel(currentAccount, getTargetChannelId(), playlistFile, data.msgId, data.iconId);
                if (listener != null) listener.onPlaylistUpdated();
                finishFragment();
            });
            builder.setNegativeButton(getString(R.string.Cancel), null);
            builder.show();
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
            PlaylistSyncManager.PlaylistData data = PlaylistSyncManager.readPlaylistFile(playlistFile);
            data.trackLines.clear();
            for (MessageObject msg : playlistMessages) {
                if (msg.messageOwner != null && msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) {
                    data.trackLines.add(msg.messageOwner.message);
                } else {
                    data.trackLines.add(String.valueOf(msg.getId()));
                }
            }
            data.pendingSync = false;
            PlaylistSyncManager.writePlaylistFile(playlistFile, data);
            PlaylistSyncManager.syncPlaylistToChannel(currentAccount, getTargetChannelId(), playlistFile);
            hasPendingEdits = false;
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
                    int bgColor    = Theme.getColor(Theme.key_windowBackgroundWhite);
                    int textColor  = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
                    int grayColor  = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);
                    int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);
                    FrameLayout headerRoot = new FrameLayout(mContext);
                    headerRoot.setLayoutParams(new RecyclerView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));

                    // ① Фоновый блюр (обложка плейлиста на всю ширину)
                    BackupImageView bgCover = new BackupImageView(mContext);
                    headerRoot.addView(bgCover,
                            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 450));

                    // ② Тёмный градиент от верха к bgColor снизу
                    View deepFade = new View(mContext);
                    GradientDrawable fadeDraw = new GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{0xE6000000, 0x99000000,
                                    androidx.core.graphics.ColorUtils
                                            .setAlphaComponent(bgColor, 240),
                                    bgColor});
                    deepFade.setBackground(fadeDraw);
                    headerRoot.addView(deepFade,
                            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 450));

                    // ③ Центральный контент
                    LinearLayout contentCol = new LinearLayout(mContext);
                    contentCol.setOrientation(LinearLayout.VERTICAL);
                    contentCol.setGravity(Gravity.CENTER_HORIZONTAL);
                    contentCol.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(64),
                            AndroidUtilities.dp(24), AndroidUtilities.dp(24));

                    // Обложка плейлиста с тенью + стеклянный бордер
                    FrameLayout artWrapper = new FrameLayout(mContext);
                    if (android.os.Build.VERSION.SDK_INT >= 21)
                        artWrapper.setElevation(AndroidUtilities.dp(24));

                    BackupImageView centerCover = new BackupImageView(mContext);
                    centerCover.setRoundRadius(AndroidUtilities.dp(20));
                    artWrapper.addView(centerCover,
                            LayoutHelper.createFrame(210, 210));

                    View artBorder = new View(mContext);
                    GradientDrawable artBorderBg = new GradientDrawable();
                    artBorderBg.setColor(Color.TRANSPARENT);
                    artBorderBg.setCornerRadius(AndroidUtilities.dp(20));
                    artBorderBg.setStroke(AndroidUtilities.dp(1.5f), 0x55FFFFFF);
                    artBorder.setBackground(artBorderBg);
                    artWrapper.addView(artBorder,
                            LayoutHelper.createFrame(210, 210));

                    contentCol.addView(artWrapper,
                            LayoutHelper.createLinear(210, 210, Gravity.CENTER_HORIZONTAL));

                    // Название плейлиста
                    TextView titleTv = new TextView(mContext);
                    titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 28);
                    titleTv.setTextColor(0xFFFFFFFF);
                    titleTv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    titleTv.setGravity(Gravity.CENTER);
                    titleTv.setShadowLayer(AndroidUtilities.dp(8), 0,
                            AndroidUtilities.dp(3), 0x99000000);
                    titleTv.setLetterSpacing(-0.015f);
                    contentCol.addView(titleTv,
                            LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                                    LayoutHelper.WRAP_CONTENT,
                                    Gravity.CENTER_HORIZONTAL, 0, 18, 0, 0));

                    // Мета-текст (X треков · N мин)
                    TextView metaTv = new TextView(mContext);
                    metaTv.setTextColor(0xB3FFFFFF);
                    metaTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                    metaTv.setGravity(Gravity.CENTER);
                    metaTv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    contentCol.addView(metaTv,
                            LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                                    LayoutHelper.WRAP_CONTENT,
                                    Gravity.CENTER_HORIZONTAL, 0, 6, 0, 0));

                    // ── Панель кнопок (Liquid Glass карточка) ──────────────
                    FrameLayout btnPanel = new FrameLayout(mContext);
                    btnPanel.setBackground(SoundGramStyleManager.buildGlassDrawable(
                            SoundGramStyleSettingsFragment.KEY_GLASS_PLAYLISTS,
                            SoundGramStyleSettingsFragment.KEY_ALPHA_PLAYLISTS,
                            SoundGramStyleSettingsFragment.KEY_COLOR_BG_PLAYLISTS,
                            SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_PLAYLISTS,
                            28f));
                    if (android.os.Build.VERSION.SDK_INT >= 21)
                        btnPanel.setElevation(AndroidUtilities.dp(8));

                    LinearLayout btnsRow = new LinearLayout(mContext);
                    btnsRow.setOrientation(LinearLayout.HORIZONTAL);
                    btnsRow.setGravity(Gravity.CENTER_VERTICAL);
                    btnsRow.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8),
                            AndroidUtilities.dp(8), AndroidUtilities.dp(8));

                    // Кнопка Play
                    TextView playButton = new TextView(mContext);
                    playButton.setText(getString(R.string.hear));
                    playButton.setTextColor(Theme.getColor(Theme.key_chats_actionIcon));
                    playButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                    playButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    playButton.setGravity(Gravity.CENTER);
                    playButton.setContentDescription(getString(R.string.AccDescrMusicPlayPlaylist));
                    playButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                    GradientDrawable playBg = new GradientDrawable();
                    playBg.setColor(Theme.getColor(Theme.key_chats_actionBackground));
                    playBg.setCornerRadius(AndroidUtilities.dp(24));
                    playButton.setBackground(playBg);
                    playButton.setPadding(AndroidUtilities.dp(20), 0, AndroidUtilities.dp(20), 0);
                    if (android.os.Build.VERSION.SDK_INT >= 21)
                        playButton.setElevation(AndroidUtilities.dp(6));
                    playButton.setOnClickListener(v -> {
                        AudioPlayerAlert.currentSource = AudioPlayerAlert.SOURCE_PLAYLIST;
                        AudioPlayerAlert.currentSourceLabel = currentPlaylistName;
                        applySpringClick(playButton);
                        SoundGramTTS.speak(v, playButton.getContentDescription());
                        if (!playlistMessages.isEmpty())
                            playStrictInPlaylist(playlistMessages.get(0));
                    });
                    btnsRow.addView(playButton,
                            LayoutHelper.createLinear(0, 48, 1.0f, 0, 0, 6, 0));

                    // Кнопка Shuffle
                    ImageView shuffleButton = new ImageView(mContext);
                    shuffleButton.setImageResource(R.drawable.msg_retry);
                    shuffleButton.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN);
                    shuffleButton.setScaleType(ImageView.ScaleType.CENTER);
                    shuffleButton.setContentDescription(getString(R.string.AccDescrMusicShuffle));
                    shuffleButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                    shuffleButton.setBackground(SoundGramStyleManager.buildGlassDrawable(
                            SoundGramStyleSettingsFragment.KEY_GLASS_PLAYLISTS,
                            SoundGramStyleSettingsFragment.KEY_ALPHA_PLAYLISTS,
                            SoundGramStyleSettingsFragment.KEY_COLOR_BG_PLAYLISTS,
                            SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_PLAYLISTS,
                            24f));
                    shuffleButton.setOnClickListener(v -> {
                        AudioPlayerAlert.currentSource = AudioPlayerAlert.SOURCE_PLAYLIST;
                        AudioPlayerAlert.currentSourceLabel = currentPlaylistName;
                        applySpringClick(shuffleButton);
                        SoundGramTTS.speak(v, shuffleButton.getContentDescription());
                        if (!playlistMessages.isEmpty()) {
                            ArrayList<MessageObject> shuffled = new ArrayList<>(playlistMessages);
                            Collections.shuffle(shuffled);
                            MediaController mc = MediaController.getInstance();
                            mc.setPlaylist(shuffled, shuffled.get(0), playlistUniqueId);
                            mc.seekToProgress(shuffled.get(0), 0);
                            mc.playMessage(shuffled.get(0));
                        }
                    });
                    btnsRow.addView(shuffleButton,
                            LayoutHelper.createLinear(48, 48, 0, 0, 6, 0));

                    // Кнопка Wave
                    TextView waveButton = new TextView(mContext);
                    waveButton.setText(getString(R.string.mywave));
                    waveButton.setTextColor(0xFFFFFFFF);
                    waveButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                    waveButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    waveButton.setGravity(Gravity.CENTER);
                    waveButton.setSingleLine(true);
                    waveButton.setEllipsize(TextUtils.TruncateAt.END);
                    waveButton.setContentDescription(getString(R.string.AccDescrMusicMyWave));
                    waveButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                    waveButton.setBackground(SoundGramStyleManager.buildGlassDrawable(
                            SoundGramStyleSettingsFragment.KEY_GLASS_PLAYLISTS,
                            SoundGramStyleSettingsFragment.KEY_ALPHA_PLAYLISTS,
                            SoundGramStyleSettingsFragment.KEY_COLOR_BG_PLAYLISTS,
                            SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_PLAYLISTS,
                            24f));
                    waveButton.setPadding(AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12), 0);
                    if (android.os.Build.VERSION.SDK_INT >= 21)
                        waveButton.setElevation(AndroidUtilities.dp(4));
                    waveButton.setOnClickListener(v -> {
                        AudioPlayerAlert.currentSource = AudioPlayerAlert.SOURCE_WAVE;
                        AudioPlayerAlert.currentSourceLabel = "";
                        applySpringClick(waveButton);
                        SoundGramTTS.speak(v, waveButton.getContentDescription());
                        if (playlistMessages.isEmpty()) return;
                        Toast.makeText(mContext, getString(R.string.wvc), Toast.LENGTH_SHORT).show();
                        ArrayList<MessageObject> originalList = new ArrayList<>(playlistMessages);
                        SmartRadioEngine.stopWaveSession();
                        SmartRadioEngine.generateWaveByPlaylist(originalList, currentAccount,
                                new SmartRadioEngine.WaveCallback() {
                                    @Override public void onTracksFound(ArrayList<MessageObject> waveQueue) {
                                        MediaController.getInstance().myWaveBasePlaylist.clear();
                                        MediaController.getInstance().myWaveBasePlaylist.addAll(waveQueue);
                                        playContext(waveQueue.get(0),
                                                MediaController.getInstance().myWaveBasePlaylist,
                                                MediaController.WAVE_CONTEXT_ID);
                                    }
                                    @Override public void onMoreTracksAdded(ArrayList<MessageObject> t) {
                                        MediaController.getInstance().addTracksToPlaylist(t);
                                    }
                                    @Override public void onError() {
                                        Toast.makeText(mContext, getString(R.string.er),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    });
                    btnsRow.addView(waveButton,
                            LayoutHelper.createLinear(0, 48, 1.0f));

                    btnPanel.addView(btnsRow,
                            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                                    LayoutHelper.WRAP_CONTENT));
                    contentCol.addView(btnPanel,
                            LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                                    LayoutHelper.WRAP_CONTENT,
                                    Gravity.CENTER_HORIZONTAL, 0, 20, 0, 0));

                    headerRoot.addView(contentCol,
                            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                                    LayoutHelper.WRAP_CONTENT));

                    return new RecyclerListView.Holder(headerRoot);
                }

                SpotifyTrackCell cell = new SpotifyTrackCell(mContext);
                return new RecyclerListView.Holder(cell);
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                if (holder.getItemViewType() == 2) return;

                if (holder.getItemViewType() == 0) {
                    FrameLayout headerRoot  = (FrameLayout) holder.itemView;
                    // child 0 = bgCover, 1 = deepFade, 2 = contentCol
                    BackupImageView bgCover = (BackupImageView) headerRoot.getChildAt(0);
                    LinearLayout contentCol = (LinearLayout)  headerRoot.getChildAt(2);
                    // contentCol: 0=artWrapper, 1=titleTv, 2=metaTv, 3=btnPanel
                    FrameLayout artWrapper  = (FrameLayout)   contentCol.getChildAt(0);
                    BackupImageView centerCover = (BackupImageView) artWrapper.getChildAt(0);
                    TextView titleTv  = (TextView) contentCol.getChildAt(1);
                    TextView metaTv   = (TextView) contentCol.getChildAt(2);

                    titleTv.setText(currentPlaylistName);

                    int totalSecs = 0;
                    for (MessageObject m : playlistMessages) totalSecs += m.getDuration();
                    int mins = totalSecs / 60;
                    metaTv.setText(playlistMessages.size() + getString(R.string.trck)
                            + " · " + mins + getString(R.string.min));

                    MessageObject coverMsg = null;
                    for (MessageObject m : playlistMessages) {
                        if (hasValidIcon(m)) { coverMsg = m; break; }
                    }
                    setPlaylistCover(bgCover,     currentPlaylistName, coverMsg, 0);
                    setPlaylistCover(centerCover, currentPlaylistName, coverMsg, 16);
                    return; // дальше не продолжаем
                }

                else {
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
            container.setLayoutParams(new RecyclerView.LayoutParams(AndroidUtilities.dp(116), ViewGroup.LayoutParams.WRAP_CONTENT));
            ((RecyclerView.LayoutParams) container.getLayoutParams()).rightMargin = AndroidUtilities.dp(12);

            // ① Арт-ворк / Обложка с тенью и стеклянным ободком
            FrameLayout coverFrame = new FrameLayout(mContext);
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                coverFrame.setElevation(AndroidUtilities.dp(6));
            }

            BackupImageView coverImage = new BackupImageView(mContext);
            coverImage.setRoundRadius(AndroidUtilities.dp(16));
            coverFrame.addView(coverImage, LayoutHelper.createFrame(108, 108));

            // Стеклянный бордер
            View borderView = new View(mContext);
            GradientDrawable borderBg = new GradientDrawable();
            borderBg.setColor(Color.TRANSPARENT);
            borderBg.setCornerRadius(AndroidUtilities.dp(16));
            borderBg.setStroke(AndroidUtilities.dp(1), 0x33FFFFFF);
            borderView.setBackground(borderBg);
            coverFrame.addView(borderView, LayoutHelper.createFrame(108, 108));

            // Иконка плюса в круглом glowing-контейнере для создания
            FrameLayout createIconWrapper = new FrameLayout(mContext);
            GradientDrawable circleBg = new GradientDrawable();
            circleBg.setShape(GradientDrawable.OVAL);
            circleBg.setColor(Theme.getColor(Theme.key_chats_actionBackground));
            createIconWrapper.setBackground(circleBg);
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                createIconWrapper.setElevation(AndroidUtilities.dp(4));
            }

            ImageView plusIcon = new ImageView(mContext);
            plusIcon.setImageResource(R.drawable.msg_add);
            plusIcon.setColorFilter(Theme.getColor(Theme.key_chats_actionIcon), PorterDuff.Mode.SRC_IN);
            plusIcon.setScaleType(ImageView.ScaleType.CENTER);
            createIconWrapper.addView(plusIcon, LayoutHelper.createFrame(24, 24, Gravity.CENTER));

            createIconWrapper.setVisibility(View.GONE);
            coverFrame.addView(createIconWrapper, LayoutHelper.createFrame(44, 44, Gravity.CENTER));

            container.addView(coverFrame, LayoutHelper.createLinear(108, 108, Gravity.CENTER_HORIZONTAL));

            // ② Название плейлиста
            TextView nameText = new TextView(mContext);
            nameText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            nameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            nameText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            nameText.setSingleLine(true);
            nameText.setEllipsize(TextUtils.TruncateAt.END);
            nameText.setGravity(Gravity.CENTER_HORIZONTAL);
            container.addView(nameText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

            // ③ Мета-информация (Количество треков)
            TextView countText = new TextView(mContext);
            countText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            countText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            countText.setSingleLine(true);
            countText.setEllipsize(TextUtils.TruncateAt.END);
            countText.setGravity(Gravity.CENTER_HORIZONTAL);
            container.addView(countText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            return new RecyclerListView.Holder(container);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            LinearLayout container = (LinearLayout) holder.itemView;
            FrameLayout frame = (FrameLayout) container.getChildAt(0);
            BackupImageView coverImage = (BackupImageView) frame.getChildAt(0);
            View borderView = frame.getChildAt(1);
            FrameLayout createIconWrapper = (FrameLayout) frame.getChildAt(2);
            TextView nameText = (TextView) container.getChildAt(1);
            TextView countText = (TextView) container.getChildAt(2);

            if (position == 0) {
                // Карточка «Создать плейлист»
                int accent = Theme.getColor(Theme.key_chats_actionBackground);
                GradientDrawable createCardBg = new GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        new int[]{androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 50), androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 20)});
                createCardBg.setCornerRadius(AndroidUtilities.dp(16));
                coverImage.setImageDrawable(createCardBg);

                GradientDrawable createBorder = new GradientDrawable();
                createBorder.setColor(Color.TRANSPARENT);
                createBorder.setCornerRadius(AndroidUtilities.dp(16));
                createBorder.setStroke(AndroidUtilities.dp(1.5f), androidx.core.graphics.ColorUtils.setAlphaComponent(accent, 85));
                borderView.setBackground(createBorder);

                createIconWrapper.setVisibility(View.VISIBLE);
                nameText.setText(getString(R.string.create));
                nameText.setTextColor(accent);
                countText.setText(getString(R.string.MusicNewPlaylist));
                container.setContentDescription(LocaleController.getString(R.string.AccDescrMusicCreatePlaylist));
                container.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            } else {
                createIconWrapper.setVisibility(View.GONE);

                GradientDrawable defaultBorder = new GradientDrawable();
                defaultBorder.setColor(Color.TRANSPARENT);
                defaultBorder.setCornerRadius(AndroidUtilities.dp(16));
                defaultBorder.setStroke(AndroidUtilities.dp(1), 0x33FFFFFF);
                borderView.setBackground(defaultBorder);

                File pFile = playlistFiles.get(position - 1);
                PlaylistSyncManager.PlaylistData pData = PlaylistSyncManager.readPlaylistFile(pFile);
                String pName = !android.text.TextUtils.isEmpty(pData.title) ? pData.title : pFile.getName().replace(".txt", "");
                nameText.setText(pName);
                nameText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));

                MessageObject coverMsg = null;
                int trackCount = pData.trackLines != null ? pData.trackLines.size() : 0;
                container.setContentDescription(LocaleController.formatString("AccDescrMusicPlaylist", R.string.AccDescrMusicPlaylist, pName, trackCount));
                container.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                if (pData.trackLines != null) {
                    for (String line : pData.trackLines) {
                        if (coverMsg != null) break;
                        if (!line.startsWith("SG_STREAM|||")) {
                            try {
                                int id = Integer.parseInt(line);
                                for (MessageObject msg : audioMessages) {
                                    if (msg.getId() == id && hasValidIcon(msg)) {
                                        coverMsg = msg;
                                        break;
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }

                countText.setText(trackCount + " " + getString(R.string.trck));
                setPlaylistCover(coverImage, pName, coverMsg, 16);
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
        public boolean isArtistProfile = false;

        public SpotifyTrackCell(Context context) {
            super(context);
            setHasTransientState(true);

            int itemHeight = isArtistProfile ? 64 : 70;
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    AndroidUtilities.dp(itemHeight));
            lp.topMargin = AndroidUtilities.dp(3);
            lp.bottomMargin = AndroidUtilities.dp(3);
            lp.leftMargin = AndroidUtilities.dp(8);
            lp.rightMargin = AndroidUtilities.dp(8);
            setLayoutParams(lp);

            setBackground(SoundGramStyleManager.buildGlassDrawable(
                    SoundGramStyleSettingsFragment.KEY_GLASS_TRACKS,
                    SoundGramStyleSettingsFragment.KEY_ALPHA_TRACKS,
                    SoundGramStyleSettingsFragment.KEY_COLOR_BG_TRACKS,
                    SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_TRACKS,
                    16f));

            FrameLayout coverFrame = new FrameLayout(context);
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                coverFrame.setElevation(AndroidUtilities.dp(3));
            }

            coverImage = new BackupImageView(context);
            coverImage.setRoundRadius(AndroidUtilities.dp(12));
            coverFrame.addView(coverImage, LayoutHelper.createFrame(52, 52));

            playingIndicator = new ImageView(context);
            playingIndicator.setScaleType(ImageView.ScaleType.CENTER);
            GradientDrawable indicatorBg = new GradientDrawable();
            indicatorBg.setColor(0x55000000);
            indicatorBg.setCornerRadius(AndroidUtilities.dp(12));
            playingIndicator.setBackground(indicatorBg);
            playingIndicator.setVisibility(View.GONE);
            coverFrame.addView(playingIndicator, LayoutHelper.createFrame(52, 52));

            addView(coverFrame, LayoutHelper.createFrame(52, 52, Gravity.LEFT | Gravity.CENTER_VERTICAL, 10, 0, 0, 0));

            LinearLayout textLayout = new LinearLayout(context);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            textLayout.setGravity(Gravity.CENTER_VERTICAL);

            titleTextView = new TextView(context);
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            titleTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            titleTextView.setSingleLine(true);
            titleTextView.setEllipsize(TextUtils.TruncateAt.END);
            titleTextView.setHasTransientState(true);

            artistTextView = new TextView(context);
            artistTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            artistTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            artistTextView.setSingleLine(true);
            artistTextView.setEllipsize(TextUtils.TruncateAt.END);

            textLayout.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            textLayout.addView(artistTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 74, 0, 48, 0));

            optionsButton = new ImageView(context);
            optionsButton.setImageResource(R.drawable.ic_ab_other);
            optionsButton.setScaleType(ImageView.ScaleType.CENTER);
            optionsButton.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.SRC_IN);
            optionsButton.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
            addView(optionsButton, LayoutHelper.createFrame(40, 40, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 6, 0));

            setOnTouchListener((v, ev) -> {
                if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(80).start();
                } else if (ev.getAction() == android.view.MotionEvent.ACTION_UP || ev.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(200).setInterpolator(new OvershootInterpolator(2f)).start();
                }
                return false;
            });
        }

        public void setMessageObject(MessageObject msg) {
            this.currentMessageObject = msg;
            if (msg == null) return;
            titleTextView.setText(msg.getMusicTitle());

            int duration = (int) msg.getDuration();
            String durStr = duration > 0 ? String.format("%d:%02d", duration / 60, duration % 60) : "";
            String author = msg.getMusicAuthor();
            if (!TextUtils.isEmpty(author) && !TextUtils.isEmpty(durStr)) {
                artistTextView.setText(author + " • " + durStr);
            } else if (!TextUtils.isEmpty(author)) {
                artistTextView.setText(author);
            } else {
                artistTextView.setText(durStr);
            }

            MessageObject playingMsg = MediaController.getInstance().getPlayingMessageObject();
            boolean isPlaying = playingMsg != null && playingMsg.getId() == msg.getId() && msg.getId() != 0;

            if (isPlaying) {
                titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
                playingIndicator.setVisibility(View.VISIBLE);
                ValueAnimator pulseAnim = ValueAnimator.ofFloat(1f, 1.04f, 1f);
                pulseAnim.setDuration(1200);
                pulseAnim.setRepeatCount(ValueAnimator.INFINITE);
                pulseAnim.setRepeatMode(ValueAnimator.RESTART);
                pulseAnim.addUpdateListener(anim -> {
                    float val = (float) anim.getAnimatedValue();
                    coverImage.setScaleX(val);
                    coverImage.setScaleY(val);
                });
                coverImage.setTag(pulseAnim);
                pulseAnim.start();
            } else {
                titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                playingIndicator.setVisibility(View.GONE);
                Object tag = coverImage.getTag();
                if (tag instanceof ValueAnimator) ((ValueAnimator) tag).cancel();
                coverImage.setScaleX(1f);
                coverImage.setScaleY(1f);
            }
            setAudioCover(coverImage, msg, "50_50", 12);

            String title = msg.getMusicTitle();
            if (TextUtils.isEmpty(title)) title = "";
            String accText;
            if (isPlaying) {
                accText = LocaleController.formatString("AccDescrMusicTrackPlaying", R.string.AccDescrMusicTrackPlaying, title, !TextUtils.isEmpty(author) ? author : "");
            } else {
                accText = LocaleController.formatString("AccDescrMusicTrack", R.string.AccDescrMusicTrack, title, !TextUtils.isEmpty(author) ? author : "", !TextUtils.isEmpty(durStr) ? durStr : "");
            }
            setContentDescription(accText);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            optionsButton.setContentDescription(LocaleController.formatString("AccDescrMusicOptions", R.string.AccDescrMusicOptions, title));
            coverImage.setContentDescription(title);
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
                MessageObject fakeObj = createWebMessageObject(currentAccount, track.title, track.artist, track.downloadUrl, durSec, -(long)track.downloadUrl.hashCode(), track.coverUrl);
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

    private void showTrackOptionsBottomSheet(BaseFragment fragment, MessageObject msg, boolean isInsidePlaylist, Runnable onDeleteCallback) {
        Context context = fragment.getParentActivity();
        if (context == null) return;
        BottomSheet.Builder builder = new BottomSheet.Builder(context);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        int bgColor   = Theme.getColor(Theme.key_dialogBackground);
        int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);

        // ЖИДКОЕ СТЕКЛО ДЛЯ МЕНЮ
        container.setBackground(createLiquidGlassDrawable(bgColor, accentColor, 20));
        container.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(16));

        View handle = new View(context);
        GradientDrawable hBg = new GradientDrawable();
        hBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        hBg.setCornerRadius(AndroidUtilities.dp(2));
        handle.setBackground(hBg);
        FrameLayout hw = new FrameLayout(context);
        hw.addView(handle, new FrameLayout.LayoutParams(AndroidUtilities.dp(36), AndroidUtilities.dp(4), Gravity.CENTER_HORIZONTAL));
        container.addView(hw, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 20, 0, 4, 0, 8));

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(4), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        BackupImageView cover = new BackupImageView(context);
        cover.setRoundRadius(AndroidUtilities.dp(10));
        setAudioCover(cover, msg, "50_50", 10);

        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setPadding(AndroidUtilities.dp(14), 0, 0, 0);

        TextView title = new TextView(context);
        title.setText(msg.getMusicTitle());
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        title.setSingleLine(true);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));

        TextView artist = new TextView(context);
        artist.setText(msg.getMusicAuthor());
        artist.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        artist.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        artist.setSingleLine(true);

        textContainer.addView(title);
        textContainer.addView(artist);
        header.addView(cover, LayoutHelper.createLinear(52, 52));
        header.addView(textContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        container.addView(header);

        View divider = new View(context);
        divider.setBackgroundColor(androidx.core.graphics.ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_divider), 80));
        container.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 0, 0, 4));

        if (!isInsidePlaylist) {
            container.addView(createGlassMenuOption(context, getString(R.string.addpl), R.drawable.msg_add, () -> {
                builder.getDismissRunnable().run();
                showSelectPlaylistBottomSheet(fragment, msg);
            }));
        }

        String deleteText = isInsidePlaylist ? getString(R.string.dlpl) : getString(R.string.dll);
        container.addView(createGlassMenuOption(context, deleteText, R.drawable.msg_delete, () -> {
            builder.getDismissRunnable().run();
            if (onDeleteCallback != null) onDeleteCallback.run();
        }));

        builder.setCustomView(container);
        fragment.showDialog(builder.create());
    }

    private void addTelegramTrackToLibrary(MessageObject msg) {
        long targetId = getChannelIdFromFile();
        if (targetId == 0) {
            Toast.makeText(getParentActivity(), getString(R.string.sg_open_music_first), Toast.LENGTH_LONG).show();
            return;
        }
        ArrayList<MessageObject> fmessages = new ArrayList<>();
        fmessages.add(msg);
        SendMessagesHelper.getInstance(currentAccount).sendMessage(fmessages, -targetId, false, false, true, 0, 0);
        Toast.makeText(getParentActivity(), getString(R.string.sg_saved_to_library), Toast.LENGTH_SHORT).show();
    }


    // Стеклянная строка меню шторки
    private View createGlassMenuOption(Context context, String text, int iconRes, Runnable onClick) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(14), AndroidUtilities.dp(20), AndroidUtilities.dp(14));
        row.setBackground(Theme.createSelectorDrawable(
                androidx.core.graphics.ColorUtils.setAlphaComponent(
                        Theme.getColor(Theme.key_listSelector), 50), 0));
        row.setOnClickListener(v -> { applySpringClick(v); onClick.run(); });
        row.setHasTransientState(true);

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Theme.getColor(Theme.key_featuredStickers_addButton), PorterDuff.Mode.SRC_IN);

        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);

        row.addView(icon, LayoutHelper.createLinear(22, 22));
        row.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 18, 0, 0, 0));
        return row;
    }

    // Обновлённая шторка для веб-треков
    private void showWebTrackOptionsBottomSheet(BaseFragment fragment, MusicWebSearcher.WebTrack track) {
        Context context = fragment.getParentActivity();
        if (context == null) return;
        final BottomSheet[] dialogRef = {null};
        BottomSheet.Builder builder = new BottomSheet.Builder(context);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int bgColor = Theme.getColor(Theme.key_dialogBackground);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(bgColor, 245));
        float r = AndroidUtilities.dp(20);
        gd.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        gd.setStroke(AndroidUtilities.dp(1),
                androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF, 30));
        container.setBackground(gd);
        container.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(16));

        // Ручка
        View handle = new View(context);
        GradientDrawable hBg = new GradientDrawable();
        hBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        hBg.setCornerRadius(AndroidUtilities.dp(2));
        handle.setBackground(hBg);
        FrameLayout hw = new FrameLayout(context);
        hw.addView(handle, new FrameLayout.LayoutParams(AndroidUtilities.dp(36), AndroidUtilities.dp(4), Gravity.CENTER_HORIZONTAL));
        container.addView(hw, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 20, 0, 4, 0, 4));

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        BackupImageView cover = new BackupImageView(context);
        cover.setRoundRadius(AndroidUtilities.dp(10));
        cover.setImage(track.coverUrl, "50_50", null);

        LinearLayout textContainer = new LinearLayout(context);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setPadding(AndroidUtilities.dp(14), 0, 0, 0);

        TextView title = new TextView(context);
        title.setText(track.title);
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        title.setSingleLine(true);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));

        TextView artist = new TextView(context);
        artist.setText(track.artist);
        artist.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        artist.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        artist.setSingleLine(true);

        textContainer.addView(title);
        textContainer.addView(artist);
        header.addView(cover, LayoutHelper.createLinear(50, 50));
        header.addView(textContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        container.addView(header);

        View divider = new View(context);
        divider.setBackgroundColor(androidx.core.graphics.ColorUtils.setAlphaComponent(
                Theme.getColor(Theme.key_divider), 80));
        container.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 0, 0, 4));

        container.addView(createGlassMenuOption(context, getString(R.string.addlb), R.drawable.msg_add, () -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            saveWebTrackToLibrary(track);
        }));

        builder.setCustomView(container);
        BottomSheet dialog = builder.create();
        dialogRef[0] = dialog;
        fragment.showDialog(dialog);
    }

    public void showSelectPlaylistBottomSheet(@NonNull BaseFragment fragment, MessageObject msg) {
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
            PlaylistSyncManager.PlaylistData pData = PlaylistSyncManager.readPlaylistFile(pFile);
            String name = !android.text.TextUtils.isEmpty(pData.title) ? pData.title : pFile.getName().replace(".txt", "");
            container.addView(createMenuOption(context, name, R.drawable.ic_ab_other, () -> {
                String trackLine;
                if (msg.messageOwner != null && msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) {
                    trackLine = msg.messageOwner.message;
                } else {
                    trackLine = String.valueOf(msg.getId());
                }
                if (!pData.trackLines.contains(trackLine)) {
                    pData.trackLines.add(0, trackLine);
                    PlaylistSyncManager.writePlaylistFile(pFile, pData);
                    PlaylistSyncManager.syncPlaylistToChannel(currentAccount, getTargetChannelId(), pFile);
                }
                Toast.makeText(context, getString(R.string.addin) + name, Toast.LENGTH_SHORT).show();
                safeNotifyAdapter(playlistsAdapter, playlistsListView);
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
                    AndroidUtilities.runOnUIThread(() -> Toast.makeText(getParentActivity(), getString(R.string.sg_channel_not_found), Toast.LENGTH_SHORT).show());
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
            } else AndroidUtilities.runOnUIThread(() -> Toast.makeText(getParentActivity(), getString(R.string.sg_no_tracks_found), Toast.LENGTH_SHORT).show());
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
            setHasTransientState(true);
            init(context, fragment);
        }

        private float swipeStartX;
        private float swipeStartY;
        private boolean isSwipingHorizontal;
        private int touchSlop;
        private boolean startDisallowed;

        @Override
        public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            super.requestDisallowInterceptTouchEvent(disallowIntercept);
            startDisallowed = disallowIntercept;
        }

        @Override
        public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
            if (touchSlop == 0) {
                touchSlop = android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop();
            }
            int action = ev.getActionMasked();
            float absX = ev.getRawX();
            float absY = ev.getRawY();

            if (action == android.view.MotionEvent.ACTION_DOWN) {
                swipeStartX = absX;
                swipeStartY = absY;
                isSwipingHorizontal = false;
                startDisallowed = false;
            } else if (action == android.view.MotionEvent.ACTION_MOVE && !isSwipingHorizontal && !startDisallowed) {
                float dx = absX - swipeStartX;
                float dy = absY - swipeStartY;
                if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                    isSwipingHorizontal = true;
                    android.view.MotionEvent cancel = android.view.MotionEvent.obtain(ev);
                    cancel.setAction(android.view.MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
            }

            if (isSwipingHorizontal) {
                float dx = absX - swipeStartX;
                if (action == android.view.MotionEvent.ACTION_MOVE) {
                    setTranslationX(dx);
                    setAlpha(1.0f - Math.min(1f, Math.abs(dx) / (float) getMeasuredWidth()));
                } else if (action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_CANCEL) {
                    if (Math.abs(dx) > getMeasuredWidth() / 3f) {
                        MediaController.getInstance().cleanupPlayer(true, true);
                        postDelayed(() -> {
                            setTranslationX(0);
                            setAlpha(1.0f);
                        }, 300);
                    } else {
                        animate().translationX(0).alpha(1.0f).setDuration(200).start();
                    }
                    isSwipingHorizontal = false;
                }
                return true;
            }

            boolean handled = super.dispatchTouchEvent(ev);
            if (action == android.view.MotionEvent.ACTION_DOWN && !handled) {
                handled = true;
            }
            return handled;
        }

        private void init(Context context, BaseFragment fragment) {


            int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);
            int bgColor = Theme.getColor(Theme.key_dialogBackground);

            int playerHeight = app.nimarkogram.messenger.utils.ui.MainTabsHelper.getMainTabsHeight();
            
            org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode blurSource = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && fragment.fragmentView != null) {
                blurSource = new org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode(null);
                blurSource.setupRenderer(new org.telegram.ui.Components.blur3.RenderNodeWithHash.Renderer() {
                    @Override
                    public void renderNodeCalculateHash(org.telegram.ui.Components.blur3.capture.IBlur3Hash hash) {}
                    @Override
                    public void renderNodeUpdateDisplayList(android.graphics.Canvas canvas) {
                        if (fragment.fragmentView != null) {
                            int oldVisibility = getVisibility();
                            setVisibility(INVISIBLE);
                            fragment.fragmentView.draw(canvas);
                            setVisibility(oldVisibility);
                        }
                    }
                });
            }

            org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory iBlur3FactoryGlass = new org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory(blurSource != null ? blurSource : new org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceColor());
            if (fragment.fragmentView != null) {
                org.telegram.ui.Components.chat.ViewPositionWatcher viewPositionWatcher = new org.telegram.ui.Components.chat.ViewPositionWatcher(fragment.fragmentView);
                iBlur3FactoryGlass.setSourceRootView(viewPositionWatcher, (android.view.ViewGroup) fragment.fragmentView);
            }
            iBlur3FactoryGlass.setLiquidGlassEffectAllowed(org.telegram.messenger.LiteMode.isEnabled(org.telegram.messenger.LiteMode.FLAG_LIQUID_GLASS));
            
            org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable tabsViewBackground = iBlur3FactoryGlass.create(this, org.telegram.ui.Components.blur3.drawable.color.impl.BlurredBackgroundProviderImpl.mainTabs(fragment.getResourceProvider()));
            tabsViewBackground.setRadius(AndroidUtilities.dp(playerHeight / 2f));
            setBackground(tabsViewBackground);

            if (Build.VERSION.SDK_INT >= 21) {
                setElevation(AndroidUtilities.dp(8));
            }
            setClickable(true);
            setVisibility(View.GONE);
            coverImage = new BackupImageView(context);
            coverImage.setRoundRadius(AndroidUtilities.dp(10));
            addView(coverImage, LayoutHelper.createFrame(40, 40, Gravity.LEFT | Gravity.CENTER_VERTICAL, 12, 0, 0, 0));

            LinearLayout textLayout = new LinearLayout(context);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            textLayout.setGravity(Gravity.CENTER_VERTICAL);

            titleTextView = new TextView(context);
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            titleTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            titleTextView.setSingleLine(true);
            titleTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            titleTextView.setMarqueeRepeatLimit(-1);
            titleTextView.setSelected(true);

            artistTextView = new TextView(context);
            artistTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            artistTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            artistTextView.setSingleLine(true);
            artistTextView.setEllipsize(TextUtils.TruncateAt.END);

            textLayout.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            textLayout.addView(artistTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
            addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 64, 0, 148, 0));

            ImageView prevBtn = new ImageView(context);
            prevBtn.setImageResource(R.drawable.baseline_skip_previous_24_white);
            prevBtn.setScaleType(ImageView.ScaleType.CENTER);
            prevBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), PorterDuff.Mode.SRC_IN);
            prevBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
            prevBtn.setHasTransientState(true);
            prevBtn.setOnClickListener(v -> {
                applyPlayerButtonSpring(v);
                SoundGramTTS.speak(v, prevBtn.getContentDescription());
                MediaController.getInstance().playPreviousMessage();
            });
            prevBtn.setContentDescription(LocaleController.getString(R.string.AccDescrPrevious));
            prevBtn.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            addView(prevBtn, LayoutHelper.createFrame(44, 44, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 100, 0));

            playPauseBtn = new ImageView(context);
            playPauseBtn.setScaleType(ImageView.ScaleType.FIT_CENTER);
            playPauseBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
            playPauseBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), PorterDuff.Mode.SRC_IN);
            playPauseBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
            playPauseBtn.setHasTransientState(true);
            playPauseBtn.setOnClickListener(v -> {
                applyPlayerButtonSpring(v);
                SoundGramTTS.speak(v, playPauseBtn.getContentDescription());
                MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
                if (playing != null) {
                    if (MediaController.getInstance().isMessagePaused()) MediaController.getInstance().playMessage(playing);
                    else MediaController.getInstance().pauseMessage(playing);
                }
            });
            addView(playPauseBtn, LayoutHelper.createFrame(44, 44, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 54, 0));

            ImageView nextBtn = new ImageView(context);
            nextBtn.setImageResource(R.drawable.baseline_skip_next_24_white);
            nextBtn.setScaleType(ImageView.ScaleType.CENTER);
            nextBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), PorterDuff.Mode.SRC_IN);
            nextBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
            nextBtn.setHasTransientState(true);
            nextBtn.setContentDescription(LocaleController.getString(R.string.AccDescrNext));
            nextBtn.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            nextBtn.setOnClickListener(v -> {
                applyPlayerButtonSpring(v);
                SoundGramTTS.speak(v, nextBtn.getContentDescription());
                MediaController.getInstance().playNextMessage();
            });
            addView(nextBtn, LayoutHelper.createFrame(44, 44, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

            // ПРОГРЕСС-БАР УДАЛЁН

            setOnClickListener(v -> {
                SoundGramTTS.speak(v, getContentDescription());
                try {
                    if (fragment.getParentActivity() != null) {
                        AudioPlayerAlert alert = new AudioPlayerAlert(fragment.getParentActivity(), fragment.getResourceProvider());
                        fragment.showDialog(alert);
                    }
                } catch (Exception e) {
                    FileLog.e("AudioPlayerAlert err: " + e);
                }
            });

            setTranslationY(AndroidUtilities.dp(80));
        }

        private void applyPlayerButtonSpring(View v) {
            v.animate().scaleX(0.80f).scaleY(0.80f).setDuration(70).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(300)
                            .setInterpolator(new OvershootInterpolator(4f)).start()
            ).start();
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
            if (id == NotificationCenter.messagePlayingDidReset
                    || id == NotificationCenter.messagePlayingPlayStateChanged
                    || id == NotificationCenter.messagePlayingDidStart) {
                updateView();
            }
        }

        private void updateView() {
            MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
            if (playing == null || (!playing.isMusic() && playing.messageOwner.attachPath == null)) {
                if (getVisibility() == View.VISIBLE) {
                    animate().translationY(AndroidUtilities.dp(80)).alpha(0f).setDuration(220)
                            .withEndAction(() -> setVisibility(View.GONE)).start();
                }
                return;
            }
            if (getVisibility() != View.VISIBLE) {
                setVisibility(View.VISIBLE);
                animate().translationY(0).alpha(1f).setDuration(280)
                        .setInterpolator(new OvershootInterpolator(1.5f)).start();
            }
            titleTextView.setText(playing.getMusicTitle());
            artistTextView.setText(playing.getMusicAuthor());
            setAudioCover(coverImage, playing, "50_50", 12);

            boolean isPaused = MediaController.getInstance().isMessagePaused();
            playPauseBtn.setImageResource(isPaused ? R.drawable.ic_play : R.drawable.ic_pause);
            playPauseBtn.setContentDescription(LocaleController.getString(isPaused ? R.string.AccActionPlay : R.string.AccActionPause));
            playPauseBtn.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            coverImage.setContentDescription(playing.getMusicTitle());
            setContentDescription(LocaleController.formatString("AccDescrMusicMiniPlayer", R.string.AccDescrMusicMiniPlayer, playing.getMusicTitle(), playing.getMusicAuthor()));
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    private void loadNextSearchPage() {
        if (isSearchLoadingMore || !hasMoreSearchResults || TextUtils.isEmpty(currentSearchQuery)) return;
        isSearchLoadingMore = true;
        final int nextPage = currentSearchPage + 1;
        final String searchForQuery = currentSearchQuery;

        MusicWebSearcher.search(searchForQuery, nextPage, (tracks, artists, albums) -> {
            if (!searchForQuery.equals(currentSearchQuery) || !isSearching) {
                isSearchLoadingMore = false;
                return;
            }
            if (tracks != null && !tracks.isEmpty()) {
                currentSearchPage = nextPage;
                int startPos = webResultTracks.size();
                ArrayList<MusicWebSearcher.WebTrack> uniqueNewTracks = new ArrayList<>();
                for (MusicWebSearcher.WebTrack t : tracks) {
                    boolean exists = false;
                    for (MusicWebSearcher.WebTrack existing : webResultTracks) {
                        if (existing.downloadUrl.equals(t.downloadUrl) ||
                            (existing.title.equalsIgnoreCase(t.title) && existing.artist.equalsIgnoreCase(t.artist))) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        webResultTracks.add(t);
                        uniqueNewTracks.add(t);
                    }
                }
                if (uniqueNewTracks.isEmpty()) {
                    hasMoreSearchResults = false;
                } else if (tracks.size() < 3) {
                    hasMoreSearchResults = false;
                }
                if (adapter != null && !uniqueNewTracks.isEmpty()) {
                    adapter.notifyItemRangeInserted(startPos, uniqueNewTracks.size());
                }
            } else {
                hasMoreSearchResults = false;
            }
            isSearchLoadingMore = false;
        });
    }

    private void filterMusic(String query) {
        String queryNoTrailing = query != null ? query.replaceAll("\\s+$", "") : "";

        if (!TextUtils.isEmpty(queryNoTrailing)) {
            if (tabStrip != null) tabStrip.setVisibility(View.GONE);
            if (libraryListHeader != null) libraryListHeader.setVisibility(View.GONE);
            if (viewPager != null && viewPager.getCurrentItem() != 1) viewPager.setCurrentItem(1, false);

            if (queryNoTrailing.equals(currentSearchQuery) && !webResultTracks.isEmpty()) {
                return;
            }

            currentSearchQuery = queryNoTrailing;
            currentSearchPage = 1;
            hasMoreSearchResults = true;
            isSearchLoadingMore = false;

            searchResultMessages.clear();
            webResultTracks.clear();

            String lower = queryNoTrailing.toLowerCase();
            for (MessageObject msg : audioMessages) {
                if (msg.getMusicTitle().toLowerCase().contains(lower) || msg.getMusicAuthor().toLowerCase().contains(lower)) {
                    searchResultMessages.add(msg);
                }
            }

            MusicWebSearcher.search(queryNoTrailing, 1, (tracks, artists, albums) -> {
                if (!queryNoTrailing.equals(currentSearchQuery)) return;

                webResultTracks.clear();
                if (tracks != null) {
                    webResultTracks.addAll(tracks);
                    if (tracks.size() < 3) hasMoreSearchResults = false;
                } else {
                    hasMoreSearchResults = false;
                }

                ArrayList<MusicWebSearcher.WebArtist> validArtists = new ArrayList<>();
                if (artists != null) {
                    for (MusicWebSearcher.WebArtist a : artists) {
                        if (a.name != null && !a.name.trim().isEmpty() && !a.name.equalsIgnoreCase("Unknown")) {
                            validArtists.add(a);
                        }
                    }
                }

                ArrayList<MusicWebSearcher.WebAlbum> validAlbums = new ArrayList<>();
                if (albums != null) {
                    for (MusicWebSearcher.WebAlbum a : albums) {
                        if (a.title != null && !a.title.trim().isEmpty() && !a.title.equalsIgnoreCase("Unknown")) {
                            validAlbums.add(a);
                        }
                    }
                }

                if (artistAdapter != null) artistAdapter.setArtists(validArtists);
                if (albumsAdapter != null) albumsAdapter.setAlbums(validAlbums);

                LinearLayout.LayoutParams artistLP = new LinearLayout.LayoutParams(LayoutHelper.MATCH_PARENT, validArtists.isEmpty() ? 0 : LayoutHelper.WRAP_CONTENT);
                artistLP.setMargins(0, 0, 0, 0);
                artistRecyclerView.setLayoutParams(artistLP);

                LinearLayout.LayoutParams albumsLP = new LinearLayout.LayoutParams(LayoutHelper.MATCH_PARENT, validAlbums.isEmpty() ? 0 : LayoutHelper.WRAP_CONTENT);
                albumsLP.setMargins(0, 0, 0, 0);
                albumsRecyclerView.setLayoutParams(albumsLP);

                artistRecyclerView.setVisibility(validArtists.isEmpty() ? View.GONE : View.VISIBLE);
                albumsRecyclerView.setVisibility(validAlbums.isEmpty() ? View.GONE : View.VISIBLE);

                safeNotifyAdapter(adapter, listView);
            });

        } else {
            currentSearchQuery = "";
            currentSearchPage = 1;
            hasMoreSearchResults = true;
            isSearchLoadingMore = false;

            searchResultMessages.clear();
            webResultTracks.clear();

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
    private void filterOfflineTracks() {
        ArrayList<MessageObject> offlineList = new ArrayList<>();
        for (MessageObject msg : audioMessages) {
            // Если это трек из SoundGram по ссылке (SG_STREAM)
            if (msg.messageOwner != null && msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) {
                String[] parts = msg.messageOwner.message.split("\\|\\|\\|");
                if (parts.length >= 4) {
                    File f = getLocalTrackFile(parts[2], parts[1]);
                    // Оставляем только если файл скачан
                    if (f.exists() && f.length() > 1024) {
                        offlineList.add(msg);
                    }
                }
            } else if (msg.isMusic()) {
                // Если это нативный трек отправленный ФАЙЛОМ (mp3/flac) — оставляем его всегда
                offlineList.add(msg);
            }
        }
        audioMessages.clear();
        audioMessages.addAll(offlineList);
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
            hasOwnBackground = true;
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
                if (v instanceof FrameLayout) {
                    FrameLayout trackCard = (FrameLayout) v;
                    if (trackCard.getChildCount() > 0 && trackCard.getChildAt(0) instanceof LinearLayout) {
                        LinearLayout trackRow = (LinearLayout) trackCard.getChildAt(0);
                        if (trackRow.getChildCount() >= 3 && trackRow.getChildAt(2) instanceof LinearLayout) {
                            LinearLayout textContainer = (LinearLayout) trackRow.getChildAt(2);
                            TextView titleTxt = (TextView) textContainer.getChildAt(0);

                            if (i < queue.size()) {
                                MessageObject rowMsg = queue.get(i);
                                if (playing != null && playing.getId() == rowMsg.getId() && playing.getId() != 0) {
                                    titleTxt.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
                                } else {
                                    titleTxt.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public boolean hasOwnBackground() {
            return true;
        }

        @Override
        public ActionBar createActionBar(Context context) {
            ActionBar bar = super.createActionBar(context);
            if (bar != null) {
                bar.setBackButtonImage(org.telegram.messenger.R.drawable.ic_ab_back);
                bar.setAllowOverlayTitle(false);
                bar.setTitle("");
                bar.setBackgroundColor(Color.TRANSPARENT);
                bar.setItemsColor(0xFFFFFFFF, false);
                bar.setAddToContainer(false);
                bar.setCastShadows(false);
                bar.setClipContent(true);
                bar.setOccupyStatusBar(true);
                bar.setActionBarMenuOnItemClick(new org.telegram.ui.ActionBar.ActionBar.ActionBarMenuOnItemClick() {
                    @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
                });
            }
            return bar;
        }

        @Override
        public View createView(Context context) {
            hasOwnBackground = true;
            if (actionBar != null) {
                actionBar.setBackButtonImage(org.telegram.messenger.R.drawable.ic_ab_back);
                actionBar.setAllowOverlayTitle(false);
                actionBar.setTitle("");
                actionBar.setBackgroundColor(Color.TRANSPARENT);
                actionBar.setItemsColor(0xFFFFFFFF, false);
                actionBar.setAddToContainer(false);
                actionBar.setCastShadows(false);
                actionBar.setClipContent(true);
                actionBar.setOccupyStatusBar(true);
                actionBar.setActionBarMenuOnItemClick(new org.telegram.ui.ActionBar.ActionBar.ActionBarMenuOnItemClick() {
                    @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
                });
            }

            int accentColor  = Theme.getColor(Theme.key_featuredStickers_addButton);
            int bgColor      = Theme.getColor(Theme.key_windowBackgroundWhite);
            int textColor    = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
            int grayColor    = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);

            // ── Корневой FrameLayout ─────────────────────────────────────────
            FrameLayout rootFrame = new FrameLayout(context);
            rootFrame.setBackgroundColor(bgColor);
            fragmentView = rootFrame;

            // ── ScrollView ───────────────────────────────────────────────────
            ScrollView scrollView = new ScrollView(context);
            scrollView.setVerticalScrollBarEnabled(false);
            scrollView.setClipToPadding(false);
            scrollView.setPadding(0, 0, 0, AndroidUtilities.dp(80));
            rootFrame.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            if (actionBar != null) {
                rootFrame.addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            }

            contentLayout = new LinearLayout(context);
            contentLayout.setOrientation(LinearLayout.VERTICAL);
            scrollView.addView(contentLayout, LayoutHelper.createScroll(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

            // ── ❶ HERO (баннер исполнителя с Параллаксом) ───────────────────
            FrameLayout heroFrame = new FrameLayout(context);

            // Фотография исполнителя — полный экран с параллакс-смещением
            bannerView = new BackupImageView(context);
            bannerView.setBackgroundColor(0xFF161616);
            if (!TextUtils.isEmpty(bannerUrl)) bannerView.setImage(bannerUrl, "600_600", null);
            heroFrame.addView(bannerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 420));

            // Загружаем с Deezer в высоком разрешении
            MusicWebSearcher.fetchArtistPictureFromDeezer(artistName, url -> {
                if (url != null) bannerView.setImage(url, "600_600", null);
            });

            // Параллакс-эффект при прокрутке
            scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
                int scrollY = scrollView.getScrollY();
                if (scrollY >= 0) {
                    bannerView.setTranslationY(scrollY * 0.45f);
                } else {
                    bannerView.setTranslationY(0);
                    float scale = 1.0f + Math.abs(scrollY) / 400.0f;
                    bannerView.setScaleX(scale);
                    bannerView.setScaleY(scale);
                }
            });

            // Слой 1: Затемнение сверху (для читаемости ActionBar)
            View topScrim = new View(context);
            GradientDrawable topGrad = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xD0000000, 0x00000000});
            topScrim.setBackground(topGrad);
            heroFrame.addView(topScrim, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 120));

            // Слой 2: Плавное скрытие к фону снизу
            View bottomFade = new View(context);
            GradientDrawable bottomGrad = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0x00000000,
                            0x99000000,
                            androidx.core.graphics.ColorUtils.setAlphaComponent(bgColor, 240),
                            bgColor});
            bottomFade.setBackground(bottomGrad);
            heroFrame.addView(bottomFade, LayoutHelper.createFrame(
                    LayoutHelper.MATCH_PARENT, 220, Gravity.BOTTOM));

            // Контейнер с текстовой информацией артиста
            LinearLayout infoCol = new LinearLayout(context);
            infoCol.setOrientation(LinearLayout.VERTICAL);
            infoCol.setPadding(AndroidUtilities.dp(20), 0, AndroidUtilities.dp(20), AndroidUtilities.dp(24));

            // Бейдж верификации «✓ ИСПОЛНИТЕЛЬ»
            TextView verifiedBadge = new TextView(context);
            verifiedBadge.setText(getString(R.string.MusicVerifiedArtist));
            verifiedBadge.setTextColor(0xFFFFFFFF);
            verifiedBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            verifiedBadge.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            verifiedBadge.setLetterSpacing(0.04f);

            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setColor(0x35FFFFFF);
            badgeBg.setCornerRadius(AndroidUtilities.dp(12));
            badgeBg.setStroke(AndroidUtilities.dp(1), 0x55FFFFFF);
            verifiedBadge.setBackground(badgeBg);
            verifiedBadge.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));

            infoCol.addView(verifiedBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

            // Имя исполнителя
            TextView nameText = new TextView(context);
            nameText.setText(artistName);
            nameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 36);
            nameText.setTextColor(0xFFFFFFFF);
            nameText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            nameText.setShadowLayer(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(3), 0xBB000000);
            nameText.setLetterSpacing(-0.02f);
            infoCol.addView(nameText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            heroFrame.addView(infoCol, LayoutHelper.createFrame(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));

            contentLayout.addView(heroFrame, LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, 420));

            // ── ❷ ПАНЕЛЬ ДЕЙСТВИЙ (Liquid Glass карточка) ───────────────────
            FrameLayout actionsCard = new FrameLayout(context);
            actionsCard.setBackground(SoundGramStyleManager.buildGlassDrawable(
                    SoundGramStyleSettingsFragment.KEY_GLASS_ARTIST,
                    SoundGramStyleSettingsFragment.KEY_ALPHA_ARTIST,
                    SoundGramStyleSettingsFragment.KEY_COLOR_BG_ARTIST,
                    SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_ARTIST,
                    28f));
            if (android.os.Build.VERSION.SDK_INT >= 21)
                actionsCard.setElevation(AndroidUtilities.dp(8));

            LinearLayout actionsRow = new LinearLayout(context);
            actionsRow.setOrientation(LinearLayout.HORIZONTAL);
            actionsRow.setGravity(Gravity.CENTER_VERTICAL);
            actionsRow.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10),
                    AndroidUtilities.dp(10), AndroidUtilities.dp(10));

            // Кнопка «СЛУШАТЬ» (Play All)
            TextView playAllBtn = new TextView(context);
            playAllBtn.setText(getString(R.string.hear));
            playAllBtn.setTextColor(Theme.getColor(Theme.key_chats_actionIcon));
            playAllBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            playAllBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            playAllBtn.setGravity(Gravity.CENTER);
            GradientDrawable playBg = new GradientDrawable();
            playBg.setColor(Theme.getColor(Theme.key_chats_actionBackground));
            playBg.setCornerRadius(AndroidUtilities.dp(24));
            playAllBtn.setBackground(playBg);
            if (android.os.Build.VERSION.SDK_INT >= 21)
                playAllBtn.setElevation(AndroidUtilities.dp(6));
            playAllBtn.setOnClickListener(v -> {
                AudioPlayerAlert.currentSource = AudioPlayerAlert.SOURCE_ARTIST;
                AudioPlayerAlert.currentSourceLabel = artistName;
                applySpringClick(v);
                if (!queue.isEmpty()) {
                    MediaController.getInstance().setPlaylist(queue, queue.get(0),
                            -(long) "artist_context".hashCode());
                    MediaController.getInstance().seekToProgress(queue.get(0), 0);
                    MediaController.getInstance().playMessage(queue.get(0));
                } else Toast.makeText(context, getString(R.string.sg_tracks_loading), Toast.LENGTH_SHORT).show();
            });
            actionsRow.addView(playAllBtn, LayoutHelper.createLinear(0, 48, 1.0f, 0, 0, 6, 0));

            // Кнопка Shuffle (стеклянная)
            ImageView shuffleBtn = new ImageView(context);
            shuffleBtn.setImageResource(org.telegram.messenger.R.drawable.msg_retry);
            shuffleBtn.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN);
            shuffleBtn.setScaleType(ImageView.ScaleType.CENTER);
            shuffleBtn.setBackground(SoundGramStyleManager.buildGlassDrawable(
                    SoundGramStyleSettingsFragment.KEY_GLASS_ARTIST,
                    SoundGramStyleSettingsFragment.KEY_ALPHA_ARTIST,
                    SoundGramStyleSettingsFragment.KEY_COLOR_BG_ARTIST,
                    SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_ARTIST,
                    24f));
            shuffleBtn.setOnClickListener(v -> {
                AudioPlayerAlert.currentSource = AudioPlayerAlert.SOURCE_ARTIST;
                AudioPlayerAlert.currentSourceLabel = artistName;
                applySpringClick(v);
                if (!queue.isEmpty()) {
                    ArrayList<MessageObject> shuffled = new ArrayList<>(queue);
                    Collections.shuffle(shuffled);
                    MediaController.getInstance().setPlaylist(shuffled, shuffled.get(0),
                            -(long) "artist_shuffle".hashCode());
                    MediaController.getInstance().seekToProgress(shuffled.get(0), 0);
                    MediaController.getInstance().playMessage(shuffled.get(0));
                }
            });
            actionsRow.addView(shuffleBtn, LayoutHelper.createLinear(48, 48, 0, 0, 6, 0));

            // Кнопка «Моя Волна»
            TextView waveBtn = new TextView(context);
            waveBtn.setText(getString(R.string.mywave));
            waveBtn.setTextColor(0xFFFFFFFF);
            waveBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            waveBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            waveBtn.setGravity(Gravity.CENTER);
            waveBtn.setSingleLine(true);
            waveBtn.setEllipsize(TextUtils.TruncateAt.END);
            waveBtn.setBackground(SoundGramStyleManager.buildGlassDrawable(
                    SoundGramStyleSettingsFragment.KEY_GLASS_ARTIST,
                    SoundGramStyleSettingsFragment.KEY_ALPHA_ARTIST,
                    SoundGramStyleSettingsFragment.KEY_COLOR_BG_ARTIST,
                    SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_ARTIST,
                    24f));
            waveBtn.setPadding(AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12), 0);
            if (android.os.Build.VERSION.SDK_INT >= 21)
                waveBtn.setElevation(AndroidUtilities.dp(4));
            waveBtn.setOnClickListener(v -> {
                AudioPlayerAlert.currentSource = AudioPlayerAlert.SOURCE_WAVE;
                AudioPlayerAlert.currentSourceLabel = "";
                applySpringClick(v);
                Toast.makeText(context, getString(R.string.sg_wave_generating), Toast.LENGTH_SHORT).show();
                new Thread(() -> {
                    ArrayList<String> genres = MusicWebSearcher.getArtistGenresSync(artistName);
                    String genreQuery = genres.isEmpty() ? artistName : genres.get(0);
                    AndroidUtilities.runOnUIThread(() -> {
                        String savedQuery = SmartRadioEngine.currentConfig.query;
                        SmartRadioEngine.currentConfig.query = genreQuery;
                        SmartRadioEngine.stopWaveSession();
                        SmartRadioEngine.generateWaveByArtist(artistName, currentAccount,
                                new SmartRadioEngine.WaveCallback() {
                                    @Override public void onTracksFound(ArrayList<MessageObject> tracks) {
                                        SmartRadioEngine.currentConfig.query = savedQuery;
                                        MediaController.getInstance().setPlaylist(tracks, tracks.get(0), MediaController.WAVE_CONTEXT_ID);
                                        MediaController.getInstance().playMessage(tracks.get(0));
                                    }
                                    @Override public void onMoreTracksAdded(ArrayList<MessageObject> t) {
                                        MediaController.getInstance().addTracksToPlaylist(t);
                                    }
                                    @Override public void onError() {
                                        SmartRadioEngine.currentConfig.query = savedQuery;
                                        Toast.makeText(context, getString(R.string.sg_wave_error), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    });
                }).start();
            });
            actionsRow.addView(waveBtn, LayoutHelper.createLinear(0, 48, 1.0f));

            actionsCard.addView(actionsRow, LayoutHelper.createFrame(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            contentLayout.addView(actionsCard,
                    LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                            LayoutHelper.WRAP_CONTENT, 16, -12, 16, 20));

            // ── ❸ БЛОК АЛЬБОМОВ (заголовок + горизонтальный скролл) ─────────
            albumsTitle = new TextView(context);
            albumsTitle.setText(getString(R.string.sg_popular_releases));
            albumsTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            albumsTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            albumsTitle.setTextColor(textColor);
            albumsTitle.setLetterSpacing(-0.01f);
            albumsTitle.setVisibility(View.GONE);
            contentLayout.addView(albumsTitle,
                    LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT,
                            LayoutHelper.WRAP_CONTENT, 20, 0, 20, 12));

            albumsScroll = new HorizontalScrollView(context);
            albumsScroll.setHorizontalScrollBarEnabled(false);
            albumsScroll.setClipToPadding(false);
            albumsScroll.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), 0);
            albumsScroll.setVisibility(View.GONE);

            albumsContainer = new LinearLayout(context);
            albumsContainer.setOrientation(LinearLayout.HORIZONTAL);
            albumsScroll.addView(albumsContainer,
                    LayoutHelper.createScroll(LayoutHelper.WRAP_CONTENT,
                            LayoutHelper.MATCH_PARENT, Gravity.CENTER_VERTICAL));
            contentLayout.addView(albumsScroll,
                    LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 220, 0, 0, 0, 20));

            // ── ❹ ЗАГОЛОВОК «Популярные треки» ─────────────────────────────
            TextView tracksTitle = new TextView(context);
            tracksTitle.setText(getString(R.string.sg_popular_tracks));
            tracksTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            tracksTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            tracksTitle.setTextColor(textColor);
            tracksTitle.setLetterSpacing(-0.01f);
            contentLayout.addView(tracksTitle,
                    LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT,
                            LayoutHelper.WRAP_CONTENT, 20, 0, 20, 12));

            // ── ❺ КОНТЕЙНЕР ТРЕКОВ ──────────────────────────────────────────
            tracksContainer = new LinearLayout(context);
            tracksContainer.setOrientation(LinearLayout.VERTICAL);
            tracksContainer.setClipToPadding(false);
            contentLayout.addView(tracksContainer,
                    LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                            LayoutHelper.WRAP_CONTENT, 12, 0, 12, 0));

            // ── ❻ MiniPlayer (поверх всего) ─────────────────────────────────
            ChannelMusicFragment.MiniPlayerView miniPlayerView =
                    new ChannelMusicFragment.MiniPlayerView(context, this);
            rootFrame.addView(miniPlayerView,
                    LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 64, Gravity.BOTTOM));
            SoundGramStyleManager.applyGlass(
                    miniPlayerView,
                    SoundGramStyleSettingsFragment.KEY_GLASS_MINI,
                    SoundGramStyleSettingsFragment.KEY_ALPHA_MINI,
                    SoundGramStyleSettingsFragment.KEY_COLOR_BG_MINI,
                    SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_MINI,
                    22f,
                    () -> miniPlayerView.invalidate()
            );
            loadData(context);
            return fragmentView;
        }


        private void loadData(Context context) {
            int bgColor   = Theme.getColor(Theme.key_windowBackgroundWhite);
            int textColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
            int grayColor = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);

            MusicWebSearcher.loadArtistAlbumsViaDeezer(artistName,
                    new MusicWebSearcher.ArtistProfileCallback() {
                        @Override
                        public void onResult(ArrayList<MusicWebSearcher.WebAlbum> albums,
                                             ArrayList<MusicWebSearcher.WebTrack> dummy) {
                            if (albums.isEmpty()) return;
                            albumsTitle.setVisibility(View.VISIBLE);
                            albumsScroll.setVisibility(View.VISIBLE);
                            albumsContainer.removeAllViews();

                            for (MusicWebSearcher.WebAlbum album : albums) {
                                // Обёртка-карточка (Liquid Glass)
                                LinearLayout albumCard = new LinearLayout(context);
                                albumCard.setOrientation(LinearLayout.VERTICAL);
                                albumCard.setGravity(Gravity.CENTER_HORIZONTAL);
                                albumCard.setPadding(0, 0, AndroidUtilities.dp(14), 0);
                                albumCard.setClipToPadding(false);

                                // Обложка с тенью
                                FrameLayout coverWrapper = new FrameLayout(context);
                                if (android.os.Build.VERSION.SDK_INT >= 21)
                                    coverWrapper.setElevation(AndroidUtilities.dp(6));

                                BackupImageView cover = new BackupImageView(context);
                                cover.setRoundRadius(AndroidUtilities.dp(16));
                                cover.setImage(album.coverUrl, "200_200", null);

                                // Стеклянная рамка вокруг обложки
                                GradientDrawable coverBorder = new GradientDrawable();
                                coverBorder.setColor(Color.TRANSPARENT);
                                coverBorder.setCornerRadius(AndroidUtilities.dp(16));
                                coverBorder.setStroke(AndroidUtilities.dp(1.5f),
                                        androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF, 75));

                                FrameLayout coverFrame = new FrameLayout(context);
                                coverFrame.addView(cover,
                                        LayoutHelper.createFrame(150, 150));
                                View borderOverlay = new View(context);
                                borderOverlay.setBackground(coverBorder);
                                coverFrame.addView(borderOverlay,
                                        LayoutHelper.createFrame(150, 150));
                                coverWrapper.addView(coverFrame,
                                        LayoutHelper.createFrame(150, 150));
                                albumCard.addView(coverWrapper,
                                        LayoutHelper.createLinear(150, 150, 0, 0, 0, 8));

                                // Название альбома
                                TextView titleTv = new TextView(context);
                                titleTv.setText(album.title);
                                titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                                titleTv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                                titleTv.setMaxLines(2);
                                titleTv.setEllipsize(TextUtils.TruncateAt.END);
                                titleTv.setTextColor(textColor);
                                titleTv.setGravity(Gravity.CENTER_HORIZONTAL);
                                albumCard.addView(titleTv,
                                        LayoutHelper.createLinear(150, LayoutHelper.WRAP_CONTENT));

                                albumCard.setOnClickListener(v -> {
                                    applySpringClick(albumCard);
                                    presentFragment(new AlbumProfileFragment(
                                            album.title, album.albumUrl, album.coverUrl));
                                });
                                albumsContainer.addView(albumCard);
                            }
                        }
                        @Override public void onFailed() {}
                    });

            if (artistUrl != null && !artistUrl.isEmpty()) {
                fetchTracksFromHitmos(artistUrl, context);
            } else {
                MusicWebSearcher.search(artistName, (tracks, artists, albums) -> {
                    String hitmosUrl = null;
                    if (artists != null && !artists.isEmpty()) {
                        for (MusicWebSearcher.WebArtist a : artists) {
                            if (a.name.equalsIgnoreCase(artistName)) {
                                hitmosUrl = a.artistUrl; break;
                            }
                        }
                        if (hitmosUrl == null) hitmosUrl = artists.get(0).artistUrl;
                    }
                    if (hitmosUrl != null) {
                        artistUrl = hitmosUrl;
                        AndroidUtilities.runOnUIThread(
                                () -> fetchTracksFromHitmos(artistUrl, context));
                    } else {
                        AndroidUtilities.runOnUIThread(() ->
                                Toast.makeText(context, getString(R.string.sg_tracks_not_found),
                                        Toast.LENGTH_SHORT).show());
                    }
                });
            }
        }


        private MusicWebSearcher.ArtistProfileCallback createFallbackCallback(MusicWebSearcher.ArtistProfileCallback original, String targetUrl, String artistName) {
            return new MusicWebSearcher.ArtistProfileCallback() {
                @Override
                public void onTrackLoaded(MusicWebSearcher.WebTrack track) { original.onTrackLoaded(track); }
                @Override
                public void onResult(ArrayList<MusicWebSearcher.WebAlbum> albums, ArrayList<MusicWebSearcher.WebTrack> tracks) {
                    if (tracks.isEmpty()) onFailed();
                    else original.onResult(albums, tracks);
                }
                @Override
                public void onFailed() {
                    if (targetUrl != null && targetUrl.startsWith("DEEZER_ARTIST:")) {
                        long deezerArtistId = Long.parseLong(targetUrl.replace("DEEZER_ARTIST:", "").trim());
                        MusicWebSearcher.loadArtistTracksDeezer(deezerArtistId, artistName, original);
                    } else {
                        original.onFailed();
                    }
                }
            };
        }

        private void fetchTracksFromHitmos(String targetUrl, Context context) {
            queue.clear();
            tracksContainer.removeAllViews();

            int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);
            int textColor   = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
            int grayColor   = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);

            MusicWebSearcher.ArtistProfileCallback callback = new MusicWebSearcher.ArtistProfileCallback() {
                int trackIndex = 1;

                @Override
                public void onTrackLoaded(MusicWebSearcher.WebTrack track) {
                    if (track == null || TextUtils.isEmpty(track.downloadUrl)) return;

                    int durSec = ChannelMusicFragment.parseDuration(track.duration);
                    MessageObject fakeMsg = ChannelMusicFragment.createWebMessageObject(
                            currentAccount, track.title, track.artist, track.downloadUrl,
                            durSec, -(long) track.downloadUrl.hashCode(), track.coverUrl);
                    queue.add(fakeMsg);

                    FrameLayout trackCard = new FrameLayout(context);
                    trackCard.setBackground(SoundGramStyleManager.buildGlassDrawable(
                            SoundGramStyleSettingsFragment.KEY_GLASS_TRACKS,
                            SoundGramStyleSettingsFragment.KEY_ALPHA_TRACKS,
                            SoundGramStyleSettingsFragment.KEY_COLOR_BG_TRACKS,
                            SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_TRACKS,
                            14f));
                    if (android.os.Build.VERSION.SDK_INT >= 21)
                        trackCard.setElevation(AndroidUtilities.dp(2));

                    LinearLayout trackRow = new LinearLayout(context);
                    trackRow.setOrientation(LinearLayout.HORIZONTAL);
                    trackRow.setGravity(Gravity.CENTER_VERTICAL);
                    trackRow.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10),
                            AndroidUtilities.dp(8), AndroidUtilities.dp(10));
                    trackRow.setBackground(Theme.createSelectorDrawable(
                            androidx.core.graphics.ColorUtils.setAlphaComponent(
                                    Theme.getColor(Theme.key_listSelector), 60), 0));

                    TextView numberTxt = new TextView(context);
                    numberTxt.setText(String.format("%02d", trackIndex++));
                    numberTxt.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                    numberTxt.setTextColor(androidx.core.graphics.ColorUtils.setAlphaComponent(grayColor, 180));
                    numberTxt.setGravity(Gravity.CENTER);
                    numberTxt.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    trackRow.addView(numberTxt, LayoutHelper.createLinear(28, LayoutHelper.WRAP_CONTENT, 0, 0, 10, 0));

                    BackupImageView trackCover = new BackupImageView(context);
                    trackCover.setRoundRadius(AndroidUtilities.dp(8));
                    String useCover = !TextUtils.isEmpty(track.coverUrl) ? track.coverUrl : bannerUrl;
                    trackCover.setImage(useCover, "60_60", null);
                    trackRow.addView(trackCover, LayoutHelper.createLinear(48, 48, 0, 0, 12, 0));

                    LinearLayout textBlock = new LinearLayout(context);
                    textBlock.setOrientation(LinearLayout.VERTICAL);
                    textBlock.setGravity(Gravity.CENTER_VERTICAL);

                    TextView titleTxt = new TextView(context);
                    titleTxt.setText(track.title);
                    titleTxt.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                    titleTxt.setTextColor(textColor);
                    titleTxt.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    titleTxt.setSingleLine(true);
                    titleTxt.setEllipsize(TextUtils.TruncateAt.END);

                    TextView artistTxt = new TextView(context);
                    artistTxt.setText(track.artist);
                    artistTxt.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                    artistTxt.setTextColor(androidx.core.graphics.ColorUtils.setAlphaComponent(grayColor, 200));
                    artistTxt.setSingleLine(true);
                    artistTxt.setEllipsize(TextUtils.TruncateAt.END);

                    textBlock.addView(titleTxt, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                    textBlock.addView(artistTxt, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 3, 0, 0));
                    trackRow.addView(textBlock, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

                    ImageView optionsIcon = new ImageView(context);
                    optionsIcon.setImageResource(org.telegram.messenger.R.drawable.ic_ab_other);
                    optionsIcon.setColorFilter(
                            androidx.core.graphics.ColorUtils.setAlphaComponent(grayColor, 180),
                            PorterDuff.Mode.SRC_IN);
                    optionsIcon.setScaleType(ImageView.ScaleType.CENTER);
                    optionsIcon.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
                    trackRow.addView(optionsIcon, LayoutHelper.createLinear(40, 44));
                    optionsIcon.setOnClickListener(v ->
                            showWebTrackOptionsBottomSheet(ArtistProfileFragment.this, track));

                    trackCard.addView(trackRow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

                    trackCard.setOnClickListener(v -> {
                        AudioPlayerAlert.currentSource = AudioPlayerAlert.SOURCE_ARTIST;
                        AudioPlayerAlert.currentSourceLabel = artistName;
                        applySpringClick(trackCard);
                        MediaController.getInstance().setPlaylist(
                                new ArrayList<>(queue), fakeMsg,
                                -(long)"artist_context".hashCode());
                        MediaController.getInstance().seekToProgress(fakeMsg, 0);
                        MediaController.getInstance().playMessage(fakeMsg);
                        updatePlayingTrackColor();
                    });

                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
                    lp.setMargins(0, 0, 0, AndroidUtilities.dp(6));
                    tracksContainer.addView(trackCard, lp);
                }

                @Override
                public void onResult(ArrayList<MusicWebSearcher.WebAlbum> ignoredAlbums,
                                     ArrayList<MusicWebSearcher.WebTrack> tracks) {
                    updatePlayingTrackColor();
                }

                @Override
                public void onFailed() {
                    AndroidUtilities.runOnUIThread(() ->
                            Toast.makeText(context, getString(R.string.sg_tracks_not_found), Toast.LENGTH_SHORT).show());
                }
            };

            MusicWebSearcher.loadArtistProfile(targetUrl, artistName, callback);
        }

    }

    public class AlbumProfileFragment extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
        private String title;
        private String artist;
        private String albumUrl;
        private String coverUrl;
        private ArrayList<MessageObject> playlistMessages = new ArrayList<>();
        private RecyclerListView listView;
        private AlbumMusicAdapter adapter;
        private long albumUniqueId;

        public AlbumProfileFragment(String title, String albumUrl, String coverUrl) {
            this(title, "", albumUrl, coverUrl);
        }

        public AlbumProfileFragment(String title, String artist, String albumUrl, String coverUrl) {
            this.title = title;
            this.artist = artist;
            this.albumUrl = albumUrl;
            this.coverUrl = coverUrl;
            this.albumUniqueId = -(long) Math.abs(albumUrl.hashCode());
        }

        private MessageObject getSavedAlbumMessage() {
            if (albumUrl == null) return null; // ИСПРАВЛЕНИЕ: Защита от NPE
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
            hasOwnBackground = true;
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
        public boolean hasOwnBackground() {
            return true;
        }

        private void addLoadedTrack(MessageObject msgObj) {
            AndroidUtilities.runOnUIThread(() -> {
                int pos = playlistMessages.size();
                playlistMessages.add(msgObj);
                if (adapter != null) {
                    adapter.notifyItemInserted(pos + 1);
                }
            });
        }

        @Override
        public ActionBar createActionBar(Context context) {
            ActionBar bar = super.createActionBar(context);
            if (bar != null) {
                bar.setBackButtonImage(R.drawable.ic_ab_back);
                bar.setAllowOverlayTitle(false);
                bar.setTitle("");
                bar.setBackgroundColor(Color.TRANSPARENT);
                bar.setItemsColor(0xFFFFFFFF, false);
                bar.setAddToContainer(false);
                bar.setCastShadows(false);
                bar.setClipContent(true);
                bar.setOccupyStatusBar(true);

                ActionBarMenu menu = bar.createMenu();
                boolean isSaved = getSavedAlbumMessage() != null;
                if (isSaved) {
                    menu.addItem(1, R.drawable.msg_delete);
                } else {
                    menu.addItem(1, R.drawable.msg_add);
                }

                bar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
                    @Override
                    public void onItemClick(int id) {
                        if (id == -1) finishFragment();
                        else if (id == 1) {
                            MessageObject savedMsg = getSavedAlbumMessage();
                            if (savedMsg != null) {
                                deleteTrackFromChannel(savedMsg);
                                savedAlbumMessages.remove(savedMsg);
                                safeNotifyAdapter(savedAlbumsAdapter, null);
                                refreshAlbumsTabVisibility(albumsTabBtnRef, tabAccentColor, tabInactiveColor);
                                finishFragment();
                                Toast.makeText(getParentActivity(), getString(R.string.sg_album_deleted), Toast.LENGTH_SHORT).show();
                            } else {
                                AndroidUtilities.runOnUIThread(() -> {
                                    saveAlbumToChannel(title, albumUrl, coverUrl);
                                    if (menu != null && menu.getItem(1) != null) {
                                        menu.getItem(1).setIcon(R.drawable.msg_delete);
                                    }
                                    Toast.makeText(getParentActivity(), getString(R.string.sg_album_saved), Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    }
                });
            }
            return bar;
        }

        @Override
        public View createView(Context context) {
            hasOwnBackground = true;

            FrameLayout content = new FrameLayout(context);
            content.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            fragmentView = content;

            listView = new RecyclerListView(context);
            listView.setLayoutManager(new LinearLayoutManager(context));
            adapter = new AlbumMusicAdapter(context);
            listView.setAdapter(adapter);
            listView.addItemDecoration(new TrackItemDecoration(6, 12, true));
            listView.setPadding(0, 0, 0, AndroidUtilities.dp(88));
            listView.setClipToPadding(false);
            listView.setOnItemClickListener((view, position) -> {
                // При запуске трека из альбома (AlbumProfileFragment.playStrictInPlaylist):
                AudioPlayerAlert.currentSource = AudioPlayerAlert.SOURCE_ALBUM;
                AudioPlayerAlert.currentSourceLabel = title; // поле title класса AlbumProfileFragment
                if (adapter.getItemViewType(position) == 2) return;
                if (position > 0 && position <= playlistMessages.size()) {
                    playStrictInPlaylist(playlistMessages.get(position - 1));
                }
            });

            content.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            if (actionBar != null) {
                content.addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            }

            ChannelMusicFragment.MiniPlayerView miniPlayerView = new ChannelMusicFragment.MiniPlayerView(context, this);
            content.addView(miniPlayerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 64, Gravity.BOTTOM, 8, 0, 8, 8));
            SoundGramStyleManager.applyGlass(
                    miniPlayerView,
                    SoundGramStyleSettingsFragment.KEY_GLASS_MINI,
                    SoundGramStyleSettingsFragment.KEY_ALPHA_MINI,
                    SoundGramStyleSettingsFragment.KEY_COLOR_BG_MINI,
                    SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_MINI,
                    22f,
                    () -> miniPlayerView.invalidate()
            );

            return fragmentView;
        }

        private void loadAlbumData() {
            if (albumUrl != null && albumUrl.startsWith("SEARCH_ALBUM:")) {
                String[] parts = albumUrl.replace("SEARCH_ALBUM:", "").split("::::", 2);
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
                        int currentTrackIndex = playlistMessages.size() + 1;
                        MessageObject msgObj = createWebMessageObject(
                                currentAccount, wt.title, wt.artist, wt.downloadUrl,
                                parseDuration(wt.duration), -(long) wt.downloadUrl.hashCode(), wt.coverUrl);

                        // Ищем локальный файл альбома: новый формат имени
                        File localByName = getLocalAlbumTrackFile(title, wt.artist, wt.title);
                        if (localByName.exists() && localByName.length() > 1024) {
                            msgObj.messageOwner.attachPath = localByName.getAbsolutePath();
                        } else {
                            // Старый формат: номер трека
                            File localByNum = getLocalAlbumTrackFile(title, currentTrackIndex);
                            if (localByNum.exists() && localByNum.length() > 1024) {
                                localByNum.renameTo(localByName);
                                msgObj.messageOwner.attachPath = localByName.exists()
                                        ? localByName.getAbsolutePath() : localByNum.getAbsolutePath();
                            }
                        }

                        addLoadedTrack(msgObj);
                    }
                    @Override
                    public void onFailed() {
                        Toast.makeText(getParentActivity(), getString(R.string.sg_album_not_found), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                fetchRealAlbum(albumUrl);
            }
        }


        private void fetchRealAlbum(String realUrl) {
            playlistMessages.clear();
            String searchArtist = artist;
            String searchTitle = title;
            if (TextUtils.isEmpty(searchArtist) && title != null && title.contains(" - ")) {
                String[] tParts = title.split(" - ", 2);
                searchArtist = tParts[0].trim();
                searchTitle = tParts[1].trim();
            }
            final String fArtist = searchArtist;
            final String fTitle = searchTitle;
            MusicWebSearcher.loadArtistProfile(realUrl, new MusicWebSearcher.ArtistProfileCallback() {
                @Override public void onResult(ArrayList<MusicWebSearcher.WebAlbum> albums, ArrayList<MusicWebSearcher.WebTrack> tracks) {
                    if ((tracks == null || tracks.isEmpty()) && playlistMessages.isEmpty()) {
                        MusicWebSearcher.loadAlbumWithDeezerFallback(fTitle, fArtist, new MusicWebSearcher.ArtistProfileCallback() {
                            @Override public void onResult(ArrayList<MusicWebSearcher.WebAlbum> a, ArrayList<MusicWebSearcher.WebTrack> t) { safeNotifyAdapter(adapter, listView); }
                            @Override public void onTrackLoaded(MusicWebSearcher.WebTrack wt) {
                                MessageObject msgObj = createWebMessageObject(currentAccount, wt.title, wt.artist, wt.downloadUrl, parseDuration(wt.duration), -(long) wt.downloadUrl.hashCode(), wt.coverUrl);
                                addLoadedTrack(msgObj);
                            }
                            @Override public void onFailed() { Toast.makeText(getParentActivity(), getString(R.string.sg_album_not_found), Toast.LENGTH_SHORT).show(); }
                        });
                        return;
                    }
                    safeNotifyAdapter(adapter, listView);
                }
                @Override
                public void onTrackLoaded(MusicWebSearcher.WebTrack wt) {
                    int currentTrackIndex = playlistMessages.size() + 1;
                    MessageObject msgObj = createWebMessageObject(currentAccount, wt.title, wt.artist, wt.downloadUrl, parseDuration(wt.duration), -(long)wt.downloadUrl.hashCode(), wt.coverUrl);

                    File localAlbumFile = getLocalAlbumTrackFile(title, currentTrackIndex);
                    if (localAlbumFile.exists()) {
                        msgObj.messageOwner.attachPath = localAlbumFile.getAbsolutePath();
                    }

                    addLoadedTrack(msgObj);
                }

                @Override public void onFailed() {
                    MusicWebSearcher.loadAlbumWithDeezerFallback(fTitle, fArtist, new MusicWebSearcher.ArtistProfileCallback() {
                        @Override public void onResult(ArrayList<MusicWebSearcher.WebAlbum> a, ArrayList<MusicWebSearcher.WebTrack> t) { safeNotifyAdapter(adapter, listView); }
                        @Override public void onTrackLoaded(MusicWebSearcher.WebTrack wt) {
                            MessageObject msgObj = createWebMessageObject(currentAccount, wt.title, wt.artist, wt.downloadUrl, parseDuration(wt.duration), -(long) wt.downloadUrl.hashCode(), wt.coverUrl);
                            addLoadedTrack(msgObj);
                        }
                        @Override public void onFailed() { Toast.makeText(getParentActivity(), getString(R.string.sg_album_not_found), Toast.LENGTH_SHORT).show(); }
                    });
                }
            });
        }

        private void playStrictInPlaylist(MessageObject messageObject) {
            if (messageObject != null && messageObject.messageOwner != null && (messageObject.messageOwner.attachPath == null || messageObject.messageOwner.attachPath.isEmpty())) {
                Toast.makeText(getParentActivity(), getString(R.string.sg_track_loading), Toast.LENGTH_SHORT).show();
                new Thread(() -> {
                    String tTitle = messageObject.getMusicTitle() != null ? messageObject.getMusicTitle() : "";
                    String tArtist = messageObject.getMusicAuthor() != null ? messageObject.getMusicAuthor() : "";
                    MusicWebSearcher.WebTrack fresh = MusicWebSearcher.resolveHitmosSync(tTitle, tArtist);
                    if (fresh != null && !TextUtils.isEmpty(fresh.downloadUrl)) {
                        messageObject.messageOwner.attachPath = fresh.downloadUrl;
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        if (messageObject.messageOwner.attachPath == null || messageObject.messageOwner.attachPath.isEmpty()) {
                            Toast.makeText(getParentActivity(), getString(R.string.sg_download_error), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        playStrictInPlaylistInternal(messageObject);
                    });
                }).start();
                return;
            }
            playStrictInPlaylistInternal(messageObject);
        }

        private void playStrictInPlaylistInternal(MessageObject messageObject) {
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
                    int bgColor    = Theme.getColor(Theme.key_windowBackgroundWhite);
                    int textColor  = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
                    int grayColor  = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);
                    int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);

                    // ── Корневой контейнер заголовка ────────────────────────
                    FrameLayout headerRoot = new FrameLayout(mContext);
                    headerRoot.setLayoutParams(new RecyclerView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));

                    // ① Блюр-фон: размытая обложка на всю ширину
                    BackupImageView bgCover = new BackupImageView(mContext);
                    bgCover.setImage(coverUrl, "600_600_b", null);
                    headerRoot.addView(bgCover,
                            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 450));

                    // ② Тёмный градиент поверх фона (снизу тянется к bgColor)
                    View deepFade = new View(mContext);
                    GradientDrawable fadeDraw = new GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{0xE6000000,
                                    0x99000000,
                                    androidx.core.graphics.ColorUtils
                                            .setAlphaComponent(bgColor, 240),
                                    bgColor});
                    deepFade.setBackground(fadeDraw);
                    headerRoot.addView(deepFade,
                            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 450));

                    // Слой затемнения сверху для читаемости кнопок ActionBar
                    View topScrim = new View(mContext);
                    GradientDrawable topGrad = new GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{0xCC000000, 0x00000000});
                    topScrim.setBackground(topGrad);
                    headerRoot.addView(topScrim, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 120));

                    // ③ Центральный контент (обложка + текст + кнопки)
                    LinearLayout contentCol = new LinearLayout(mContext);
                    contentCol.setOrientation(LinearLayout.VERTICAL);
                    contentCol.setGravity(Gravity.CENTER_HORIZONTAL);
                    contentCol.setPadding(AndroidUtilities.dp(24), AndroidUtilities.statusBarHeight + AndroidUtilities.dp(56),
                            AndroidUtilities.dp(24), AndroidUtilities.dp(24));

                    // Обложка альбома — с тенью и стеклянной рамкой
                    FrameLayout artWrapper = new FrameLayout(mContext);
                    if (android.os.Build.VERSION.SDK_INT >= 21)
                        artWrapper.setElevation(AndroidUtilities.dp(20));

                    BackupImageView centerCover = new BackupImageView(mContext);
                    centerCover.setRoundRadius(AndroidUtilities.dp(20));
                    centerCover.setImage(coverUrl, "300_300", null);
                    artWrapper.addView(centerCover,
                            LayoutHelper.createFrame(210, 210));

                    // Стеклянный ободок поверх обложки
                    View artBorder = new View(mContext);
                    GradientDrawable artBorderBg = new GradientDrawable();
                    artBorderBg.setColor(Color.TRANSPARENT);
                    artBorderBg.setCornerRadius(AndroidUtilities.dp(20));
                    artBorderBg.setStroke(AndroidUtilities.dp(1.5f), 0x66FFFFFF);
                    artBorder.setBackground(artBorderBg);
                    artWrapper.addView(artBorder,
                            LayoutHelper.createFrame(210, 210));

                    contentCol.addView(artWrapper,
                            LayoutHelper.createLinear(210, 210, Gravity.CENTER_HORIZONTAL));

                    // Название альбома
                    TextView albumTitleTv = new TextView(mContext);
                    albumTitleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 26);
                    albumTitleTv.setTextColor(0xFFFFFFFF);
                    albumTitleTv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    albumTitleTv.setGravity(Gravity.CENTER);
                    albumTitleTv.setShadowLayer(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(3), 0xAA000000);
                    albumTitleTv.setLetterSpacing(-0.01f);
                    albumTitleTv.setText(title);
                    contentCol.addView(albumTitleTv,
                            LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                                    LayoutHelper.WRAP_CONTENT,
                                    Gravity.CENTER_HORIZONTAL, 0, 18, 0, 0));

                    // Подзаголовок (Тип релиза: Альбом)
                    TextView subTitleTv = new TextView(mContext);
                    subTitleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                    subTitleTv.setTextColor(0xCCFFFFFF);
                    subTitleTv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    subTitleTv.setGravity(Gravity.CENTER);
                    subTitleTv.setText(getString(R.string.MusicAlbum));
                    contentCol.addView(subTitleTv,
                            LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                                    LayoutHelper.WRAP_CONTENT,
                                    Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));

                    // ── Кнопки действий ────────────────────────────────────
                    // Стеклянная панель под кнопки
                    FrameLayout btnPanel = new FrameLayout(mContext);
                    btnPanel.setBackground(SoundGramStyleManager.buildGlassDrawable(
                            SoundGramStyleSettingsFragment.KEY_GLASS_ALBUMS,
                            SoundGramStyleSettingsFragment.KEY_ALPHA_ALBUMS,
                            SoundGramStyleSettingsFragment.KEY_COLOR_BG_ALBUMS,
                            SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_ALBUMS,
                            28f));
                    if (android.os.Build.VERSION.SDK_INT >= 21)
                        btnPanel.setElevation(AndroidUtilities.dp(8));

                    LinearLayout btnsRow = new LinearLayout(mContext);
                    btnsRow.setOrientation(LinearLayout.HORIZONTAL);
                    btnsRow.setGravity(Gravity.CENTER_VERTICAL);
                    btnsRow.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10),
                            AndroidUtilities.dp(10), AndroidUtilities.dp(10));

                    // Кнопка «СЛУШАТЬ» (зелёный градиент)
                    TextView playBtn = new TextView(mContext);
                    playBtn.setText(getString(R.string.hear));
                    playBtn.setTextColor(Theme.getColor(Theme.key_chats_actionIcon));
                    playBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                    playBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    playBtn.setGravity(Gravity.CENTER);
                    GradientDrawable playBtnBg = new GradientDrawable();
                    playBtnBg.setColor(Theme.getColor(Theme.key_chats_actionBackground));
                    playBtnBg.setCornerRadius(AndroidUtilities.dp(24));
                    playBtn.setBackground(playBtnBg);
                    if (android.os.Build.VERSION.SDK_INT >= 21)
                        playBtn.setElevation(AndroidUtilities.dp(6));
                    playBtn.setOnClickListener(v -> {
                        AudioPlayerAlert.currentSource = AudioPlayerAlert.SOURCE_ALBUM;
                        AudioPlayerAlert.currentSourceLabel = title;
                        applySpringClick(playBtn);
                        if (!playlistMessages.isEmpty())
                            playStrictInPlaylist(playlistMessages.get(0));
                    });
                    btnsRow.addView(playBtn, LayoutHelper.createLinear(0, 48, 1.0f, 0, 0, 6, 0));

                    // Кнопка Shuffle (стеклянная)
                    ImageView shuffleBtn = new ImageView(mContext);
                    shuffleBtn.setImageResource(org.telegram.messenger.R.drawable.msg_retry);
                    shuffleBtn.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN);
                    shuffleBtn.setScaleType(ImageView.ScaleType.CENTER);
                    shuffleBtn.setBackground(SoundGramStyleManager.buildGlassDrawable(
                            SoundGramStyleSettingsFragment.KEY_GLASS_ALBUMS,
                            SoundGramStyleSettingsFragment.KEY_ALPHA_ALBUMS,
                            SoundGramStyleSettingsFragment.KEY_COLOR_BG_ALBUMS,
                            SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_ALBUMS,
                            24f));
                    shuffleBtn.setOnClickListener(v -> {
                        AudioPlayerAlert.currentSource = AudioPlayerAlert.SOURCE_ALBUM;
                        AudioPlayerAlert.currentSourceLabel = title;
                        applySpringClick(v);
                        if (!playlistMessages.isEmpty()) {
                            ArrayList<MessageObject> shuffled = new ArrayList<>(playlistMessages);
                            Collections.shuffle(shuffled);
                            MediaController.getInstance().setPlaylist(shuffled, shuffled.get(0), albumUniqueId);
                            MediaController.getInstance().seekToProgress(shuffled.get(0), 0);
                            MediaController.getInstance().playMessage(shuffled.get(0));
                        }
                    });
                    btnsRow.addView(shuffleBtn, LayoutHelper.createLinear(48, 48, 0, 0, 6, 0));

                    // Кнопка «Моя Волна»
                    TextView waveBtn = new TextView(mContext);
                    waveBtn.setText(getString(R.string.mywave));
                    waveBtn.setTextColor(0xFFFFFFFF);
                    waveBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                    waveBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    waveBtn.setGravity(Gravity.CENTER);
                    waveBtn.setSingleLine(true);
                    waveBtn.setEllipsize(TextUtils.TruncateAt.END);
                    waveBtn.setBackground(SoundGramStyleManager.buildGlassDrawable(
                            SoundGramStyleSettingsFragment.KEY_GLASS_ALBUMS,
                            SoundGramStyleSettingsFragment.KEY_ALPHA_ALBUMS,
                            SoundGramStyleSettingsFragment.KEY_COLOR_BG_ALBUMS,
                            SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_ALBUMS,
                            24f));
                    waveBtn.setPadding(AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12), 0);
                    if (android.os.Build.VERSION.SDK_INT >= 21)
                        waveBtn.setElevation(AndroidUtilities.dp(4));

                    waveBtn.setOnClickListener(v -> {
                        AudioPlayerAlert.currentSource = AudioPlayerAlert.SOURCE_WAVE;
                        applySpringClick(waveBtn);
                        Toast.makeText(mContext, getString(R.string.sg_wave_generating), Toast.LENGTH_SHORT).show();
                        String aArtist = artist != null ? artist : "";
                        if (aArtist.isEmpty() && albumUrl != null && albumUrl.startsWith("SEARCH_ALBUM:")) {
                            String[] parts = albumUrl.replace("SEARCH_ALBUM:", "").split("::::", 2);
                            aArtist = parts.length > 1 ? parts[1] : "";
                        }
                        if (aArtist.isEmpty() && !playlistMessages.isEmpty()) {
                            aArtist = playlistMessages.get(0).getMusicAuthor();
                        }
                        if (aArtist.isEmpty() && title != null && title.contains(" - ")) {
                            String[] tParts = title.split(" - ", 2);
                            aArtist = tParts[0].trim();
                        }
                        final String finalArtist = aArtist;
                        AudioPlayerAlert.currentSourceLabel = (!finalArtist.isEmpty() ? finalArtist + " - " : "") + title;
                        ArrayList<MessageObject> currentTracks = new ArrayList<>(playlistMessages);
                        SmartRadioEngine.stopWaveSession();
                        SmartRadioEngine.generateWaveByAlbum(finalArtist, title, currentTracks,
                                currentAccount, new SmartRadioEngine.WaveCallback() {
                                    @Override public void onTracksFound(ArrayList<MessageObject> tracks) {
                                        if (!tracks.isEmpty()) {
                                            MediaController.getInstance().setPlaylist(
                                                    tracks, tracks.get(0), MediaController.WAVE_CONTEXT_ID);
                                            MediaController.getInstance().playMessage(tracks.get(0));
                                        }
                                    }
                                    @Override public void onMoreTracksAdded(ArrayList<MessageObject> t) {
                                        MediaController.getInstance().addTracksToPlaylist(t);
                                    }
                                    @Override public void onError() {
                                        Toast.makeText(mContext, getString(R.string.sg_wave_error), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    });
                    btnsRow.addView(waveBtn, LayoutHelper.createLinear(0, 48, 1.0f));

                    btnPanel.addView(btnsRow,
                            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                                    LayoutHelper.WRAP_CONTENT));
                    contentCol.addView(btnPanel,
                            LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                                    LayoutHelper.WRAP_CONTENT,
                                    Gravity.CENTER_HORIZONTAL, 0, 20, 0, 0));

                    headerRoot.addView(contentCol,
                            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                                    LayoutHelper.WRAP_CONTENT));

                    return new RecyclerListView.Holder(headerRoot);
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
                    if (msg.messageOwner == null) return; // NPE guard

                    if (msg.messageOwner.message != null && msg.messageOwner.message.startsWith("SG_STREAM|||")) {
                        String[] parts = msg.messageOwner.message.split("\\|\\|\\|");
                        if (parts.length >= 4 && !android.text.TextUtils.isEmpty(parts[3])) {
                            // downloadUrl есть — трек загружен
                            String durStr = parts.length >= 5
                                    ? (parseDuration(parts[4]) / 60) + ":" + String.format("%02d", parseDuration(parts[4]) % 60)
                                    : "0:00";
                            String cUrl = parts.length >= 7 ? parts[6] : (parts.length >= 6 ? parts[5] : "");
                            MusicWebSearcher.WebTrack track = new MusicWebSearcher.WebTrack(
                                    parts[1], parts[2], parts[3], durStr, cUrl);
                            showWebTrackOptionsBottomSheet(AlbumProfileFragment.this, track);
                            return;
                        } else {
                            // Трек ещё не загружен
                            Toast.makeText(mContext, getString(R.string.sg_track_loading), Toast.LENGTH_SHORT).show();
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
            setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    AndroidUtilities.dp(48)));
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
            setContentDescription(text + (isSelected ? ", " + LocaleController.getString(R.string.AccDescrMusicGenreSelected) : ""));
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            if (isSelected) {
                textView.setTextColor(Theme.getColor(Theme.key_chats_actionIcon));
                textView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_chats_actionBackground)));
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
            TextView chip = new TextView(mContext);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine(true);
            chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
            chip.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
            chip.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    AndroidUtilities.dp(40)));
            return new RecyclerListView.Holder(chip);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            TextView chip = (TextView) holder.itemView;
            String genre = items[position];
            chip.setText(getLocalizedGenre(genre));
            boolean isSelected = (position == selectedIndex);
            chip.setContentDescription(LocaleController.formatString(isSelected ? "AccDescrMusicGenreSelected" : "AccDescrMusicGenre", isSelected ? R.string.AccDescrMusicGenreSelected : R.string.AccDescrMusicGenre, getLocalizedGenre(genre)));
            chip.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

            int accent = Theme.getColor(Theme.key_featuredStickers_addButton);
            GradientDrawable bg = new GradientDrawable();

            if (isSelected) {
                // Активный — сплошной акцент + белая обводка
                bg.setColor(accent);
                bg.setCornerRadius(AndroidUtilities.dp(20));
                bg.setStroke(AndroidUtilities.dp(1),
                        androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF, 80));
                chip.setTextColor(0xFFFFFFFF);
                chip.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            } else {
                // Неактивный — жидкое стекло: полупрозрачный фон + граница
                int bgColor = Theme.getColor(Theme.key_windowBackgroundGray);
                bg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(bgColor, 120));
                bg.setCornerRadius(AndroidUtilities.dp(20));
                bg.setStroke(AndroidUtilities.dp(1),
                        androidx.core.graphics.ColorUtils.setAlphaComponent(
                                Theme.getColor(Theme.key_windowBackgroundWhiteGrayText), 80));
                chip.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                chip.setTypeface(AndroidUtilities.getTypeface("fonts/rregular.ttf"));
            }
            chip.setBackground(bg);

            chip.setOnClickListener(v -> {
                SoundGramTTS.speak(v, chip.getContentDescription());
                selectedIndex = position;
                notifyDataSetChanged();
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

            layout.setContentDescription(LocaleController.formatString("AccDescrMusicAlbum", R.string.AccDescrMusicAlbum, album.title, album.artist != null ? album.artist : ""));
            layout.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

            layout.setOnClickListener(v -> {
                SoundGramTTS.speak(v, layout.getContentDescription());
                presentFragment(new AlbumProfileFragment(album.title, album.artist != null ? album.artist : "", album.albumUrl, album.coverUrl));
            });

        }
    }

    private TextView makeInlineGenreChip(Context context, String text, boolean active, int accent) {
        TextView chip = new TextView(context);
        chip.setText(text);
        chip.setContentDescription(text + (active ? ", " + LocaleController.getString(R.string.AccDescrMusicGenreSelected) : ""));
        chip.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        chip.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setPadding(
                AndroidUtilities.dp(14), AndroidUtilities.dp(8),
                AndroidUtilities.dp(14), AndroidUtilities.dp(8));
        applyChipState(chip, active, accent);
        return chip;
    }
    private void applyWaveModeCardStyle(LinearLayout card, boolean active, int accentColor) {
        GradientDrawable bg = new GradientDrawable();
        if (active) {
            bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{
                            androidx.core.graphics.ColorUtils.setAlphaComponent(accentColor, 160),
                            androidx.core.graphics.ColorUtils.setAlphaComponent(0xFF4A148C, 200)
                    });
            bg.setCornerRadius(AndroidUtilities.dp(20));
            bg.setStroke(AndroidUtilities.dp(1),
                    androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF, 80));
        } else {
            int bgColor = Theme.getColor(Theme.key_dialogBackground);
            bg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(bgColor, 90));
            bg.setCornerRadius(AndroidUtilities.dp(20));
            bg.setStroke(AndroidUtilities.dp(1),
                    androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF, 30));
        }
        card.setBackground(bg);
        if (Build.VERSION.SDK_INT >= 21)
            card.setElevation(active ? AndroidUtilities.dp(8) : AndroidUtilities.dp(2));
    }
    private void showWaveGenreSheet(Context context, TextView updateTarget) {
        // Строим BottomSheet вручную — без RecyclerView, без ChipsAdapter
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        {
            int bg = Theme.getColor(Theme.key_dialogBackground);
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(bg, 245));
            float r = AndroidUtilities.dp(28);
            gd.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
            gd.setStroke(AndroidUtilities.dp(1),
                    androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF, 40));
            container.setBackground(gd);
        }
        container.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(32));

        // Drag handle
        {
            View h = new View(context);
            GradientDrawable hBg = new GradientDrawable();
            hBg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF, 55));
            hBg.setCornerRadius(AndroidUtilities.dp(3));
            h.setBackground(hBg);
            FrameLayout hw = new FrameLayout(context);
            hw.addView(h, new FrameLayout.LayoutParams(
                    AndroidUtilities.dp(40), AndroidUtilities.dp(4), Gravity.CENTER));
            container.addView(hw, LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, 28, 0, 0, 0, 12));
        }

        // Заголовок
        TextView title = new TextView(context);
        title.setText(getString(R.string.sg_wave_genre_title));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setGravity(Gravity.CENTER);
        container.addView(title, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 20, 0, 20, 20));

        // ── Жанры в виде FlexWrap через несколько LinearLayout-строк ─────────
        String[] genres = {
                "Любой жанр", "Поп", "Хип-Хоп", "Рэп", "Рок", "Инди",
                "Фонк", "Гиперпоп", "Пост-панк", "Эмо-рэп", "Металл",
                "Альтернатива", "К-поп", "Джаз", "Блюз", "Классика",
                "Электроника", "Синтвейв", "Ло-фай", "Эмбиент"
        };

        final String[] selectedGenre = {SmartRadioEngine.currentConfig.query};
        final int accent = Theme.getColor(Theme.key_featuredStickers_addButton);

        // Скроллящийся контейнер с жанрами
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // WrapLayout — LinearLayout per row, строки добавляем сами
        // Чипы по 3 в строке (просто GridLayout через LinearLayout)
        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), AndroidUtilities.dp(8));

        // Список всех чипов для обновления состояния
        ArrayList<TextView> allChips = new ArrayList<>();

        int cols = 3;
        for (int row = 0; row < (genres.length + cols - 1) / cols; row++) {
            LinearLayout rowLayout = new LinearLayout(context);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER_VERTICAL);
            rowLayout.setWeightSum(cols);

            for (int col = 0; col < cols; col++) {
                int idx = row * cols + col;
                if (idx >= genres.length) {
                    // Пустое место — заполнитель
                    View spacer = new View(context);
                    rowLayout.addView(spacer, LayoutHelper.createLinear(
                            0, 40, 1f, 4, 4, 4, 4));
                    continue;
                }

                String genre = genres[idx];
                boolean isSelected = genre.equals(selectedGenre[0]);

                TextView chip = new TextView(context);
                chip.setText(getLocalizedGenre(genre));
                chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                chip.setGravity(Gravity.CENTER);
                chip.setSingleLine(true);
                chip.setEllipsize(TextUtils.TruncateAt.END);
                chip.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(10),
                        AndroidUtilities.dp(4), AndroidUtilities.dp(10));
                applyWaveChipStyle(chip, isSelected, accent);
                allChips.add(chip);

                chip.setOnClickListener(v -> {
                    selectedGenre[0] = genre;
                    SmartRadioEngine.currentConfig.query = genre;
                    // Обновляем стиль всех чипов
                    for (int i = 0; i < allChips.size(); i++) {
                        applyWaveChipStyle(allChips.get(i), genres[i].equals(genre), accent);
                    }
                    // Обновляем текст кнопки
                    if (updateTarget != null) {
                        updateTarget.setText("✦  " + getLocalizedGenre(genre) + "  ▾");
                    }
                });

                rowLayout.addView(chip, LayoutHelper.createLinear(
                        0, 40, 1f, 4, 4, 4, 4));
            }
            grid.addView(rowLayout, LayoutHelper.createLinear(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        scroll.addView(grid, LayoutHelper.createScroll(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));
        container.addView(scroll, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 320, 0, 0, 0, 16));

        // Кнопка «Применить»
        TextView applyBtn = new TextView(context);
        applyBtn.setText(getString(R.string.sg_wave_apply));
        applyBtn.setGravity(Gravity.CENTER);
        applyBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        applyBtn.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        applyBtn.setTextColor(Theme.getColor(Theme.key_chats_actionIcon));
        {
            GradientDrawable aBg = new GradientDrawable();
            aBg.setColor(Theme.getColor(Theme.key_chats_actionBackground));
            aBg.setCornerRadius(AndroidUtilities.dp(16));
            applyBtn.setBackground(aBg);
        }
        applyBtn.setOnClickListener(v -> {
            applySpringClick(v);
            builder.getDismissRunnable().run();
        });
        container.addView(applyBtn, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, 52, 20, 0, 20, 0));

        builder.setCustomView(container);
        showDialog(builder.create());
    }

    // Стилизация одного чипа жанра
    private void applyWaveChipStyle(TextView chip, boolean selected, int accent) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(10));
        chip.setContentDescription(chip.getText() + (selected ? ", " + LocaleController.getString(R.string.AccDescrMusicGenreSelected) : ""));
        chip.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        if (selected) {
            bg.setColor(accent);
            bg.setStroke(0, 0);
            chip.setTextColor(0xFFFFFFFF);
            chip.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        } else {
            int bgColor = Theme.getColor(Theme.key_windowBackgroundGray);
            bg.setColor(bgColor);
            bg.setStroke(AndroidUtilities.dp(1),
                    androidx.core.graphics.ColorUtils.setAlphaComponent(
                            Theme.getColor(Theme.key_windowBackgroundWhiteGrayText), 80));
            chip.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            chip.setTypeface(AndroidUtilities.getTypeface("fonts/rregular.ttf"));
        }
        chip.setBackground(bg);
    }

    private void applyChipState(TextView chip, boolean active, int accent) {
        GradientDrawable bg = new GradientDrawable();
        if (active) {
            bg.setColor(accent);
            bg.setCornerRadius(AndroidUtilities.dp(20));
            bg.setStroke(0, 0);
            chip.setTextColor(0xFFFFFFFF);
        } else {
            bg.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF, 20));
            bg.setCornerRadius(AndroidUtilities.dp(20));
            bg.setStroke(AndroidUtilities.dp(1),
                    androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF, 60));
            chip.setTextColor(androidx.core.graphics.ColorUtils.setAlphaComponent(0xFFFFFFFF, 200));
        }
        chip.setBackground(bg);
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
            layout.setContentDescription(LocaleController.formatString("AccDescrMusicArtist", R.string.AccDescrMusicArtist, artist.name));
            layout.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

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
                SoundGramTTS.speak(v, layout.getContentDescription());
                presentFragment(new ArtistProfileFragment(artist.name, artist.artistUrl, artist.imageUrl));
            });


        }
    }
    private int findMessageIdByAlbum(String title) {
        if (savedAlbumMessages == null || title == null) return 0; // ИСПРАВЛЕНИЕ: Защита
        for (int a = 0; a < savedAlbumMessages.size(); a++) {
            MessageObject obj = savedAlbumMessages.get(a);
            if (obj.messageOwner != null && obj.messageOwner.message != null && obj.messageOwner.message.contains(title)) {
                return obj.getId();
            }
        }
        return 0;
    }


    private void startAlbumFilesDownload(String albumTitle, ArrayList<MessageObject> tracks) {
        if (tracks == null || tracks.isEmpty()) return;
        Toast.makeText(getParentActivity(), String.format(getString(R.string.sg_album_downloading), albumTitle), Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            for (MessageObject mo : tracks) {
                String downloadUrl = null;
                String trackTitle  = "Track";
                String trackArtist = "Unknown";

                if (mo.messageOwner != null && mo.messageOwner.message != null
                        && mo.messageOwner.message.startsWith("SG_STREAM|||")) {
                    String[] parts = mo.messageOwner.message.split("\\|\\|\\|");
                    if (parts.length >= 4) {
                        trackTitle  = parts.length > 1 ? parts[1] : trackTitle;
                        trackArtist = parts.length > 2 ? parts[2] : trackArtist;
                        downloadUrl = parts[3];
                    }
                } else if (mo.isMusic()) {
                    trackTitle  = mo.getMusicTitle();
                    trackArtist = mo.getMusicAuthor();
                    FileLoader.getInstance(currentAccount).loadFile(mo.getDocument(), mo, 1, 0);
                    continue;
                }

                if (downloadUrl == null) continue;

                // Сохраняем в /Music/<albumTitle>/Исполнитель - Название.mp3
                File destFile = getLocalAlbumTrackFile(albumTitle, trackArtist, trackTitle);
                if (!destFile.exists() || destFile.length() < 1024) {
                    boolean ok = MusicWebSearcher.downloadTrackToExactFileSync(downloadUrl, destFile);
                    if (ok) mo.messageOwner.attachPath = destFile.getAbsolutePath();
                } else {
                    mo.messageOwner.attachPath = destFile.getAbsolutePath();
                }

                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
            AndroidUtilities.runOnUIThread(() ->
                    Toast.makeText(getParentActivity(), String.format(getString(R.string.sg_album_downloaded), albumTitle), Toast.LENGTH_SHORT).show());
        }).start();
    }

    public static File getUserStorageDir(int account) {
        long userId = getValidUserId(account);
        File baseDir = org.telegram.messenger.ApplicationLoader.applicationContext.getExternalFilesDir(null);
        if (baseDir == null) return null;
        File userDir = userId != 0 ? new File(baseDir, "user_" + userId) : new File(baseDir, "account_" + account);
        if (!userDir.exists()) userDir.mkdirs();
        migrateLegacyFilesIfNeeded(baseDir, userDir, account);
        return userDir;
    }

    private static synchronized void migrateLegacyFilesIfNeeded(File baseDir, File userDir, int account) {
        if (baseDir == null || userDir == null || baseDir.equals(userDir)) return;
        long userId = getValidUserId(account);
        if (userId == 0) return;

        try {
            // 1. Music folder migration
            File oldMusic = new File(baseDir, "Music");
            File newMusic = new File(userDir, "Music");
            if (oldMusic.exists() && oldMusic.isDirectory()) {
                if (!newMusic.exists()) newMusic.mkdirs();
                File[] files = oldMusic.listFiles();
                if (files != null) {
                    for (File f : files) {
                        File dest = new File(newMusic, f.getName());
                        if (!dest.exists()) {
                            f.renameTo(dest);
                        }
                    }
                }
            }

            // 2. Playlists folder migration
            File oldPlaylists = new File(baseDir, "Playlists");
            File newPlaylists = new File(userDir, "Playlists");
            if (oldPlaylists.exists() && oldPlaylists.isDirectory()) {
                if (!newPlaylists.exists()) newPlaylists.mkdirs();
                File[] files = oldPlaylists.listFiles();
                if (files != null) {
                    for (File f : files) {
                        File dest = new File(newPlaylists, f.getName());
                        if (!dest.exists()) {
                            f.renameTo(dest);
                        }
                    }
                }
            }

            // 3. Cache file migration
            File oldCache = new File(baseDir, "music_cache_v3.dat");
            File newCache = new File(userDir, "music_cache_v3.dat");
            if (oldCache.exists() && !newCache.exists()) {
                oldCache.renameTo(newCache);
            }

            // 4. Saved albums migration
            File oldAlbums = new File(baseDir, "saved_albums.txt");
            File newAlbums = new File(userDir, "saved_albums.txt");
            if (oldAlbums.exists() && !newAlbums.exists()) {
                oldAlbums.renameTo(newAlbums);
            }

            // 5. Channel ID migration
            long globalPrefId = MessagesController.getNotificationsSettings(account).getLong("music_channel_id", 0);
            File oldChannelFile = new File(baseDir, "music_channel_id.txt");
            long fileId = 0;
            if (oldChannelFile.exists()) {
                try (BufferedReader r = new BufferedReader(new FileReader(oldChannelFile))) {
                    String l = r.readLine();
                    if (l != null && !l.trim().isEmpty()) {
                        fileId = Long.parseLong(l.trim());
                    }
                } catch (Exception ignored) {}
            }
            long legacyId = globalPrefId != 0 ? globalPrefId : fileId;
            if (legacyId != 0) {
                long currentAccountChannelId = MessagesController.getNotificationsSettings(account).getLong("music_channel_id_" + userId, 0);
                if (currentAccountChannelId == 0) {
                    MessagesController.getNotificationsSettings(account).edit()
                            .putLong("music_channel_id_" + userId, legacyId)
                            .apply();
                    File newChannelFile = new File(userDir, "music_channel_id_" + userId + ".txt");
                    if (!newChannelFile.exists()) {
                        try (FileWriter fw = new FileWriter(newChannelFile)) {
                            fw.write(String.valueOf(legacyId));
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static File getUserStorageDir() {
        return getUserStorageDir(UserConfig.selectedAccount);
    }

    public static File getMusicStorageDir(int account) {
        File dir = new File(getUserStorageDir(account), "Music");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File getMusicStorageDir() {
        return getMusicStorageDir(UserConfig.selectedAccount);
    }

    /**
     * Путь к треку: /Music/Исполнитель - Название.mp3
     */
    public static File getLocalTrackFile(String artist, String title) {
        String name = MusicWebSearcher.getSafeFileName(artist, title);
        return new File(getMusicStorageDir(), name);
    }

    /**
     * Старый метод по hashId — оставляем для обратной совместимости при миграции.
     */
    public static File getLocalTrackFile(long trackHashId) {
        return new File(getMusicStorageDir(), trackHashId + ".mp3");
    }

    /**
     * Путь к треку альбома: /Music/<НазваниеАльбома>/Исполнитель - Название.mp3
     */
    public static File getLocalAlbumTrackFile(String albumTitle, String artist, String trackTitle) {
        String safeDirName = albumTitle.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_").trim();
        File albumDir = new File(getMusicStorageDir(), safeDirName);
        if (!albumDir.exists()) albumDir.mkdirs();
        String name = MusicWebSearcher.getSafeFileName(artist, trackTitle);
        return new File(albumDir, name);
    }

    /**
     * Старый метод по номеру трека — обратная совместимость.
     */
    public static File getLocalAlbumTrackFile(String albumTitle, int trackNumber) {
        String safeDirName = albumTitle.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_").trim();
        File albumDir = new File(getMusicStorageDir(), safeDirName);
        if (!albumDir.exists()) albumDir.mkdirs();
        return new File(albumDir, trackNumber + ".mp3");
    }

    public static File getLocalAlbumDir(String albumTitle) {
        String safeDirName = albumTitle.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_").trim();
        File dir = new File(getMusicStorageDir(), safeDirName);
        if (!dir.exists()) dir.mkdirs();
        return dir;
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
    public static class TrackItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacingPx;
        private final int horizontalPx;
        private final boolean skipFirst;   // пропускать элемент с position==0 (header)

        /**
         * @param spacingDp    вертикальный отступ между ячейками (dp)
         * @param horizontalDp горизонтальный отступ (dp), 0 = без отступа
         * @param skipFirst    true → не добавлять отступ перед первым item
         */
        public TrackItemDecoration(int spacingDp, int horizontalDp, boolean skipFirst) {
            this.spacingPx   = AndroidUtilities.dp(spacingDp);
            this.horizontalPx = AndroidUtilities.dp(horizontalDp);
            this.skipFirst   = skipFirst;
        }

        @Override
        public void getItemOffsets(
                @NonNull android.graphics.Rect outRect,
                @NonNull View view,
                @NonNull RecyclerView parent,
                @NonNull RecyclerView.State state) {

            int position = parent.getChildAdapterPosition(view);
            if (position == RecyclerView.NO_ID) return;

            // Пропускаем header (position == 0) если skipFirst == true
            if (skipFirst && position == 0) {
                outRect.set(0, 0, 0, 0);
                return;
            }

            outRect.left   = horizontalPx;
            outRect.right  = horizontalPx;
            outRect.bottom = spacingPx;

            // Отступ сверху только у первого элемента (после header)
            if (position == (skipFirst ? 1 : 0)) {
                outRect.top = spacingPx;
            } else {
                outRect.top = 0;
            }
        }
    }

    public static String getLocalizedGenre(String genreKey) {
        if (genreKey == null) return "";
        String key;
        int resId;
        switch (genreKey) {
            case "Любой жанр": key = "sg_genre_any"; resId = R.string.sg_genre_any; break;
            case "Поп": key = "sg_genre_pop"; resId = R.string.sg_genre_pop; break;
            case "Хип-Хоп": key = "sg_genre_hiphop"; resId = R.string.sg_genre_hiphop; break;
            case "Рэп": key = "sg_genre_rap"; resId = R.string.sg_genre_rap; break;
            case "Рок": key = "sg_genre_rock"; resId = R.string.sg_genre_rock; break;
            case "Инди": key = "sg_genre_indie"; resId = R.string.sg_genre_indie; break;
            case "Фонк": key = "sg_genre_phonk"; resId = R.string.sg_genre_phonk; break;
            case "Гиперпоп": key = "sg_genre_hyperpop"; resId = R.string.sg_genre_hyperpop; break;
            case "Пост-панк": key = "sg_genre_postpunk"; resId = R.string.sg_genre_postpunk; break;
            case "Эмо-рэп": key = "sg_genre_emorap"; resId = R.string.sg_genre_emorap; break;
            case "Металл": key = "sg_genre_metal"; resId = R.string.sg_genre_metal; break;
            case "Альтернатива": key = "sg_genre_alternative"; resId = R.string.sg_genre_alternative; break;
            case "К-поп": key = "sg_genre_kpop"; resId = R.string.sg_genre_kpop; break;
            case "Джаз": key = "sg_genre_jazz"; resId = R.string.sg_genre_jazz; break;
            case "Блюз": key = "sg_genre_blues"; resId = R.string.sg_genre_blues; break;
            case "Классика": key = "sg_genre_classical"; resId = R.string.sg_genre_classical; break;
            case "Электроника": key = "sg_genre_electronic"; resId = R.string.sg_genre_electronic; break;
            case "Синтвейв": key = "sg_genre_synthwave"; resId = R.string.sg_genre_synthwave; break;
            case "Ло-фай": key = "sg_genre_lofi"; resId = R.string.sg_genre_lofi; break;
            case "Эмбиент": key = "sg_genre_ambient"; resId = R.string.sg_genre_ambient; break;
            default: return genreKey;
        }
        return org.telegram.messenger.LocaleController.getString(key, resId);
    }
}
