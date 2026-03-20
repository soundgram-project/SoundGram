package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class ChannelMusicFragment extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, ChatActivityInterface {

    public interface PlaylistUpdateListener {
        void onPlaylistUpdated();
    }

    private SwipeRefreshLayout swipeRefreshLayout;
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
                Toast.makeText(getParentActivity(), "Список обновлен!", Toast.LENGTH_SHORT).show();
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
            @Override protected void onDraw(Canvas canvas) {
                canvas.drawColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            }
        };
        fragmentView = content;

        // Обновление стягиванием
        swipeRefreshLayout = new SwipeRefreshLayout(context);
        swipeRefreshLayout.setColorSchemeColors(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadMusicFromChannel();
            AndroidUtilities.runOnUIThread(() -> swipeRefreshLayout.setRefreshing(false), 1500);
        });

        // 1. СПИСОК
        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setBackgroundColor(Color.TRANSPARENT);
        adapter = new MusicAdapter(context, null);
        listView.setAdapter(adapter);
        listView.setVerticalScrollBarEnabled(false);
        listView.setGlowColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        listView.setClipToPadding(false);

        swipeRefreshLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        content.addView(swipeRefreshLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // 2. ШАПКА
        headerContainer = new LinearLayout(context);
        headerContainer.setOrientation(LinearLayout.VERTICAL);
        headerContainer.setClickable(true);
        headerContainer.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        headerContainer.setPadding(0, AndroidUtilities.statusBarHeight, 0, 0);

        createSearchMenu(context, headerContainer);
        createHorizontalPlaylists(context, headerContainer);

        content.addView(headerContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // 3. МИНИ-ПЛЕЕР
        MiniPlayerView miniPlayerView = new MiniPlayerView(context, getParentActivity());
        miniPlayerView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        View shadow = new View(context);
        shadow.setBackgroundColor(0x1A000000);
        miniPlayerView.addView(shadow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1, Gravity.TOP));

        FrameLayout.LayoutParams playerParams = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 64, Gravity.BOTTOM);
        int sysNavBar = AndroidUtilities.navigationBarHeight > 0 ? AndroidUtilities.navigationBarHeight : 0;
        int tgBottomMenu = AndroidUtilities.dp(60);
        playerParams.bottomMargin = sysNavBar + tgBottomMenu;
        content.addView(miniPlayerView, playerParams);

        // 4. ДИНАМИЧЕСКИЕ ОТСТУПЫ
        headerContainer.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                headerContainer.getViewTreeObserver().removeOnPreDrawListener(this);
                int headerHeight = headerContainer.getMeasuredHeight();
                int bottomPadding = AndroidUtilities.dp(64) + playerParams.bottomMargin;
                listView.setPadding(0, headerHeight, 0, bottomPadding);
                swipeRefreshLayout.setProgressViewOffset(false, headerHeight, headerHeight + AndroidUtilities.dp(32));
                return true;
            }
        });

        listView.setOnItemClickListener((view, position) -> {
            if (adapter.getItemViewType(position) == 2) return;
            if (isSearching) {
                if (position < webResultTracks.size()) {
                    downloadAndUploadTrack(webResultTracks.get(position));
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

    private void saveListeningStat(MessageObject msg) {
        if (msg == null) return;
        try {
            File file = new File(ApplicationLoader.applicationContext.getExternalFilesDir(null), "soundgram_stats.txt");
            long totalSeconds = 0;
            int totalTracks = 0;
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line = br.readLine();
                if (line != null && line.contains(",")) {
                    String[] parts = line.split(",");
                    totalTracks = Integer.parseInt(parts[0]);
                    totalSeconds = Long.parseLong(parts[1]);
                }
                br.close();
            }
            totalTracks++;
            totalSeconds += msg.getDuration();
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(totalTracks + "," + totalSeconds);
            bw.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showStatisticsBottomSheet() {
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        LinearLayout container = new LinearLayout(getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Theme.getColor(Theme.key_dialogBackground));
        gd.setCornerRadii(new float[]{AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), 0, 0, 0, 0});
        container.setBackground(gd);
        container.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24));

        TextView title = new TextView(getParentActivity());
        title.setText("Статистика SoundGram");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setGravity(Gravity.CENTER);
        container.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        File file = new File(ApplicationLoader.applicationContext.getExternalFilesDir(null), "soundgram_stats.txt");
        int totalTracks = 0;
        long totalSeconds = 0;
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = br.readLine();
                if (line != null && line.contains(",")) {
                    String[] parts = line.split(",");
                    totalTracks = Integer.parseInt(parts[0]);
                    totalSeconds = Long.parseLong(parts[1]);
                }
            } catch (Exception e) {}
        }

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        TextView statsText = new TextView(getParentActivity());
        statsText.setText("Всего прослушано треков: " + totalTracks + "\nОбщее время: " + hours + " ч " + minutes + " мин");
        statsText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        statsText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        statsText.setGravity(Gravity.CENTER);
        statsText.setLineSpacing(AndroidUtilities.dp(6), 1.0f);
        container.addView(statsText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        builder.setCustomView(container);
        showDialog(builder.create());
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

    private void loadMessagesFromCache() {
        try {
            File dir = ApplicationLoader.applicationContext.getExternalFilesDir(null);
            File file = new File(dir, "music_cache_v3.dat");
            if (!file.exists()) return;

            byte[] fileData = new byte[(int) file.length()];
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) { fis.read(fileData); }

            SerializedData data = new SerializedData(fileData);
            int count = data.readInt32(false);
            ArrayList<MessageObject> cached = new ArrayList<>();
            long selfUserId = UserConfig.getInstance(currentAccount).getClientUserId();
            for (int i = 0; i < count; i++) {
                int constructor = data.readInt32(false);
                TLRPC.Message message = TLRPC.Message.TLdeserialize(data, constructor, false);
                if (message != null) {
                    message.readAttachPath(data, selfUserId);
                    cached.add(new MessageObject(currentAccount, message, false, false));
                }
            }
            data.cleanup();
            AndroidUtilities.runOnUIThread(() -> {
                audioMessages.clear();
                audioMessages.addAll(cached);
                safeNotifyAdapter(adapter, listView);
                safeNotifyAdapter(playlistsAdapter, playlistsListView); // Авто-подтягивание обложек
                if (audioMessages.isEmpty() && ApplicationLoader.isNetworkOnline()) loadMusicFromChannel();
            });
        } catch (Exception e) { FileLog.e(e); }
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
        standardHeaderLayout.addView(headerTitle, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 0, 0, 0, 0));
        standardHeaderLayout.addView(searchIconBtn, LayoutHelper.createLinear(48, 48));
        standardHeaderLayout.addView(refreshBtn, LayoutHelper.createLinear(48, 48));

        searchHeaderLayout = new LinearLayout(context);
        searchHeaderLayout.setOrientation(LinearLayout.HORIZONTAL);
        searchHeaderLayout.setGravity(Gravity.CENTER_VERTICAL);
        searchHeaderLayout.setVisibility(View.GONE);
        searchHeaderLayout.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground));

        searchBackBtn = new ImageView(context);
        searchBackBtn.setImageResource(R.drawable.ic_ab_back);
        searchBackBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.SRC_IN);
        searchBackBtn.setScaleType(ImageView.ScaleType.CENTER);
        searchBackBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
        searchBackBtn.setOnClickListener(v -> closeSearch());

        searchField = new EditText(context);
        searchField.setHint("Search track...");
        searchField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        searchField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        searchField.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        searchField.setSingleLine(true);
        searchField.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchField.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), 0);
        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchTimer != null) searchTimer.cancel();
                searchTimer = new Timer();
                searchTimer.schedule(new TimerTask() {
                    @Override public void run() { AndroidUtilities.runOnUIThread(() -> filterMusic(s.toString())); }
                }, 300);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        searchHeaderLayout.addView(searchBackBtn, LayoutHelper.createLinear(48, 48));
        searchHeaderLayout.addView(searchField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        topBar.addView(standardHeaderLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 50));
        topBar.addView(searchHeaderLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 50));
        container.addView(topBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50));
    }

    private void openSearch() {
        isSearching = true;
        standardHeaderLayout.setVisibility(View.GONE);
        searchHeaderLayout.setVisibility(View.VISIBLE);
        searchField.requestFocus();
        AndroidUtilities.showKeyboard(searchField);
    }

    private void closeSearch() {
        isSearching = false;
        searchField.setText("");
        if (searchField.isFocused()) {
            searchField.clearFocus();
            AndroidUtilities.hideKeyboard(searchField);
        }
        searchHeaderLayout.setVisibility(View.GONE);
        standardHeaderLayout.setVisibility(View.VISIBLE);
        if (webResultTracks != null) webResultTracks.clear();
        if (searchResultMessages != null) searchResultMessages.clear();
        safeNotifyAdapter(adapter, listView);
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

    private void downloadAndUploadTrack(MusicWebSearcher.WebTrack track) {
        long targetIdFromFile = getChannelIdFromFile();
        if (targetIdFromFile == 0) {
            checkOrCreateMusicChannel();
            return;
        }
        musicChannelId = targetIdFromFile;
        File cacheDir = ApplicationLoader.applicationContext.getExternalCacheDir();
        AlertDialog progressDialog = new AlertDialog.Builder(getParentActivity()).setMessage("Загрузка: 0%").create();
        progressDialog.show();

        MusicWebSearcher.downloadTrack(track.downloadUrl, cacheDir, new MusicWebSearcher.DownloadCallback() {
            @Override public void onProgress(int percent) {
                AndroidUtilities.runOnUIThread(() -> progressDialog.setMessage("Загрузка: " + percent + "%"));
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
        Toast.makeText(getParentActivity(), "Удалено", Toast.LENGTH_SHORT).show();
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
        builder.setTitle("New Playlist");
        LinearLayout container = new LinearLayout(getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(6), AndroidUtilities.dp(24), 0);
        final EditText input = new EditText(getParentActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint("Enter title");
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        input.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        input.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(4), Theme.getColor(Theme.key_windowBackgroundGray)));
        input.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
        container.addView(input, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setView(container);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!TextUtils.isEmpty(name)) {
                File file = new File(getPlaylistsDir(), name + ".txt");
                try { if (file.createNewFile()) refreshPlaylistFiles(); } catch (IOException e) { e.printStackTrace(); }
            }
        });
        builder.setNegativeButton("Cancel", null);
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

    private void loadMusicFromChannel() {
        long targetId = getTargetChannelId();
        if (targetId == 0) return;
        loadMessagesFromCache();
        if (!org.telegram.messenger.ApplicationLoader.isNetworkOnline()) return;

        TLRPC.TL_messages_search searchReq = new TLRPC.TL_messages_search();
        TLRPC.TL_inputPeerChannel inputPeer = new TLRPC.TL_inputPeerChannel();
        inputPeer.channel_id = targetId; inputPeer.access_hash = musicChannelHash;
        searchReq.peer = inputPeer; searchReq.filter = new TLRPC.TL_inputMessagesFilterMusic();
        searchReq.q = ""; searchReq.limit = 3000;

        org.telegram.tgnet.ConnectionsManager.getInstance(currentAccount).sendRequest(searchReq, (response, error) -> {
            if (response instanceof TLRPC.messages_Messages) {
                TLRPC.messages_Messages messages = (TLRPC.messages_Messages) response;
                ArrayList<MessageObject> loaded = new ArrayList<>();
                for (TLRPC.Message msg : messages.messages) {
                    MessageObject messageObject = new MessageObject(currentAccount, msg, false, false);
                    if (messageObject.isMusic()) loaded.add(messageObject);
                }
                saveMessagesToCache(loaded);
                AndroidUtilities.runOnUIThread(() -> {
                    audioMessages.clear();
                    audioMessages.addAll(loaded);
                    safeNotifyAdapter(adapter, listView);
                    safeNotifyAdapter(playlistsAdapter, playlistsListView);
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

        for (MessageObject m : queue) {
            m.audioProgress = 0.0f;
            m.audioProgressSec = 0;
        }

        mediaController.setPlaylist(new ArrayList<>(queue), track, contextId);
        mediaController.seekToProgress(track, 0);
        mediaController.playMessage(track);
    }

    private void filterMusic(String query) {
        searchResultMessages.clear();
        webResultTracks.clear();
        if (!TextUtils.isEmpty(query)) {
            String lower = query.toLowerCase();
            for (MessageObject msg : audioMessages) {
                if (msg.getMusicTitle().toLowerCase().contains(lower) || msg.getMusicAuthor().toLowerCase().contains(lower)) {
                    searchResultMessages.add(msg);
                }
            }
            MusicWebSearcher.search(query, results -> {
                webResultTracks.clear();
                webResultTracks.addAll(results);
                safeNotifyAdapter(adapter, listView);
            });
        }
        safeNotifyAdapter(adapter, listView);
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
                        try { playlistIds.add(Integer.parseInt(line.trim())); } catch (Exception ignored) {}
                    }
                } catch (IOException e) { e.printStackTrace(); }
            }
            return true;
        }

        @Override
        public void onFragmentDestroy() {
            super.onFragmentDestroy();

            ArrayList<Integer> existingOrder = new ArrayList<>();
            if (playlistFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(playlistFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        existingOrder.add(Integer.parseInt(line.trim()));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }

            ArrayList<Integer> finalOrder = new ArrayList<>();

            for (MessageObject msg : allTracks) {
                if (playlistIds.contains(msg.getId()) && !existingOrder.contains(msg.getId())) {
                    finalOrder.add(msg.getId());
                }
            }

            for (Integer id : existingOrder) {
                if (playlistIds.contains(id)) {
                    finalOrder.add(id);
                }
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(playlistFile, false))) {
                for (Integer id : finalOrder) {
                    bw.write(String.valueOf(id));
                    bw.newLine();
                }
            } catch (IOException e) { e.printStackTrace(); }

            if (listener != null) listener.onPlaylistUpdated();
        }

        @Override
        public View createView(Context context) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.setTitle("Добавить треки");
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
                if (playlistIds.contains(msg.getId())) playlistIds.remove(msg.getId());
                else playlistIds.add(msg.getId());
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

                boolean isAdded = playlistIds.contains(msg.getId());
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

                    Toast.makeText(getParentActivity(), "Обложка обновлена", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(context, "Удерживайте трек для сортировки", Toast.LENGTH_SHORT).show();
                            menu.addItem(4, R.drawable.msg_gallery_locked1); // Кастомная иконка
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

            MiniPlayerView miniPlayerView = new MiniPlayerView(context, getParentActivity());
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
            ArrayList<Integer> orderIds = new ArrayList<>();
            if (playlistFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(playlistFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        try { orderIds.add(Integer.parseInt(line.trim())); } catch (Exception ignored) {}
                    }
                } catch (IOException e) { e.printStackTrace(); }
            }
            for (Integer id : orderIds) {
                for (MessageObject msg : ChannelMusicFragment.audioMessages) {
                    if (msg.getId() == id) {
                        playlistMessages.add(msg);
                        break;
                    }
                }
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

        private void showPlaylistTrackOptionsBottomSheet(Context context, MessageObject msg, Runnable onDeleteCallback) {
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
            deleteText.setText("Удалить из плейлиста");
            deleteText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            deleteText.setTextSize(16);

            deleteRow.addView(deleteIcon, LayoutHelper.createLinear(24, 24));
            deleteRow.addView(deleteText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 16, 0, 0, 0));
            container.addView(deleteRow);

            builder.setCustomView(container);
            showDialog(builder.create());
        }

        private void savePlaylistOrder() {
            if (playlistFile == null) return;
            try (java.io.FileWriter fw = new java.io.FileWriter(playlistFile, false)) {
                for (MessageObject msg : playlistMessages) {
                    fw.write(msg.getId() + "\n");
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
                    playButton.setText("Слушать");
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
                    metaText.setText(playlistMessages.size() + " треков • " + minutes + " мин");

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
                        showPlaylistTrackOptionsBottomSheet(getParentActivity(), msg, () -> {
                            playlistMessages.remove(msg);
                            savePlaylistOrder();
                            safeNotifyAdapter(this, listView);
                        });
                    });
                }
            }
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didReceiveNewMessages) {
            if ((Long) args[0] == getTargetChannelId()) {
                ArrayList<MessageObject> newMsgs = (ArrayList<MessageObject>) args[1];
                for (MessageObject msg : newMsgs) if (msg.isMusic()) audioMessages.add(0, msg);
                if (isSearching) filterMusic(searchField.getText().toString());
                else safeNotifyAdapter(adapter, listView);
            }
        } else if (id == NotificationCenter.messagePlayingDidStart) {
            MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
            if (playing != null && playing.getId() != lastTrackedId) {
                lastTrackedId = playing.getId();
                saveListeningStat(playing);
            }
            safeNotifyAdapter(adapter, listView);
        } else if (id == NotificationCenter.messagePlayingDidReset || id == NotificationCenter.messagePlayingPlayStateChanged) {
            safeNotifyAdapter(adapter, listView);
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
                nameText.setText("Создать");
            } else {
                plusIcon.setVisibility(View.GONE);
                File pFile = playlistFiles.get(position - 1);
                String pName = pFile.getName().replace(".txt", "");
                nameText.setText(pName);

                MessageObject coverMsg = null;
                try (BufferedReader br = new BufferedReader(new FileReader(pFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        int id = Integer.parseInt(line.trim());
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
            boolean isPlaying = playingMsg != null && playingMsg.getId() == msg.getId();

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
                myCell.optionsButton.setVisibility(View.GONE);
                MusicWebSearcher.WebTrack track = webResultTracks.get(position);
                TLRPC.TL_message webMsg = new TLRPC.TL_message();
                webMsg.media = new TLRPC.TL_messageMediaDocument();
                webMsg.media.document = new TLRPC.TL_document();
                TLRPC.TL_documentAttributeAudio attr = new TLRPC.TL_documentAttributeAudio();
                attr.title = track.title;
                attr.performer = track.artist;
                webMsg.media.document.attributes.add(attr);
                myCell.setMessageObject(new MessageObject(currentAccount, webMsg, false, false));
            } else {
                myCell.optionsButton.setVisibility(View.VISIBLE);
                int idx = (isSearching && customList == null) ? position - webResultTracks.size() : position;
                ArrayList<MessageObject> list = customList != null ? customList : (isSearching ? searchResultMessages : audioMessages);
                MessageObject msg = list.get(idx);
                myCell.setMessageObject(msg);

                myCell.optionsButton.setOnClickListener(v -> {
                    showTrackOptionsBottomSheet(mContext, msg, false, () -> deleteTrackFromChannel(msg));
                });
            }
        }
    }

    private void showTrackOptionsBottomSheet(Context context, MessageObject msg, boolean isInsidePlaylist, Runnable onDeleteCallback) {
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
            container.addView(createMenuOption(context, "Добавить в плейлист", R.drawable.msg_add, () -> {
                builder.getDismissRunnable().run();
                showSelectPlaylistBottomSheet(context, msg);
            }));
        }

        String deleteText = isInsidePlaylist ? "Удалить из плейлиста" : "Удалить из библиотеки";
        container.addView(createMenuOption(context, deleteText, R.drawable.msg_delete, () -> {
            builder.getDismissRunnable().run();
            if (onDeleteCallback != null) onDeleteCallback.run();
        }));

        builder.setCustomView(container);
        showDialog(builder.create());
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

    private void showSelectPlaylistBottomSheet(Context context, MessageObject msg) {
        if (playlistFiles.isEmpty()) {
            Toast.makeText(context, "Сначала создайте плейлист!", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheet.Builder builder = new BottomSheet.Builder(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Theme.getColor(Theme.key_dialogBackground));
        gd.setCornerRadii(new float[]{AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), 0, 0, 0, 0});
        container.setBackground(gd);

        TextView title = new TextView(context);
        title.setText("Добавить в плейлист");
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
                    bw.write(String.valueOf(msg.getId()));
                    bw.newLine();
                    for (String oldLine : existingLines) {
                        bw.write(oldLine);
                        bw.newLine();
                    }
                    Toast.makeText(context, "Добавлено в " + name, Toast.LENGTH_SHORT).show();
                    safeNotifyAdapter(playlistsAdapter, playlistsListView);
                } catch (IOException e) { e.printStackTrace(); }
                builder.getDismissRunnable().run();
            }));
        }
        builder.setCustomView(container);
        showDialog(builder.create());
    }

    private void showImportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Import Music from Channel");
        LinearLayout container = new LinearLayout(getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(6), AndroidUtilities.dp(24), 0);
        final EditText input = new EditText(getParentActivity());
        input.setHint("Enter channel @username");
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        input.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        input.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(4), Theme.getColor(Theme.key_windowBackgroundGray)));
        input.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
        container.addView(input, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setView(container);
        builder.setPositiveButton("Search", (dialog, which) -> {
            String query = input.getText().toString().replace("https://t.me/", "").replace("@", "").trim();
            if (!TextUtils.isEmpty(query)) searchChannelAndFetchMusic(query);
        });
        builder.setNegativeButton("Cancel", null);
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
        builder.setTitle("Import to SoundGram", true);
        LinearLayout container = new LinearLayout(getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Theme.getColor(Theme.key_dialogBackground));
        gd.setCornerRadii(new float[]{AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), 0, 0, 0, 0});
        container.setBackground(gd);

        HashSet<Integer> selectedIds = new HashSet<>();
        TextView importButton = new TextView(getParentActivity());
        importButton.setText("Import Tracks");
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
            importButton.setText(selectedIds.isEmpty() ? "Import Tracks" : "Import (" + selectedIds.size() + ")");
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

    public static void setAudioCover(BackupImageView coverImage, MessageObject msg, String filter, int radius) {
        coverImage.setRoundRadius(AndroidUtilities.dp(radius));
        android.graphics.drawable.Drawable placeholder = Theme.createRoundRectDrawable(AndroidUtilities.dp(radius), Theme.getColor(Theme.key_graySection));

        if (msg == null) {
            coverImage.setImageDrawable(placeholder);
            return;
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

        public MiniPlayerView(Context context, android.app.Activity parentActivity) {
            super(context);
            this.parentContext = context;
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            setClickable(true);

            View border = new View(context);
            border.setBackgroundColor(Theme.getColor(Theme.key_divider));
            addView(border, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1, Gravity.TOP));

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

            addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 76, 0, 240, 0));

            ImageView lyricsBtn = new ImageView(context);
            lyricsBtn.setImageResource(R.drawable.menu_quote_specific);
            lyricsBtn.setScaleType(ImageView.ScaleType.CENTER);
            lyricsBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), PorterDuff.Mode.SRC_IN);
            lyricsBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
            lyricsBtn.setOnClickListener(v -> {
                MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
                if (playing != null) new SoundGramLyricsDialog(parentActivity, playing).show();
            });
            addView(lyricsBtn, LayoutHelper.createFrame(48, 48, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 192, 0));

            ImageView eqBtn = new ImageView(context);
            eqBtn.setImageResource(R.drawable.files_music);
            eqBtn.setScaleType(ImageView.ScaleType.CENTER);
            eqBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), PorterDuff.Mode.SRC_IN);
            eqBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
            eqBtn.setOnClickListener(v -> showDialog(new SoundGramEQAlert(parentActivity)));
            addView(eqBtn, LayoutHelper.createFrame(48, 48, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 144, 0));

            ImageView prevBtn = new ImageView(context);
            prevBtn.setImageResource(R.drawable.cast_ic_expanded_controller_skip_previous);
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
            nextBtn.setImageResource(R.drawable.cast_ic_expanded_controller_skip_next);
            nextBtn.setScaleType(ImageView.ScaleType.CENTER);
            nextBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), PorterDuff.Mode.SRC_IN);
            nextBtn.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
            nextBtn.setOnClickListener(v -> MediaController.getInstance().playNextMessage());
            addView(nextBtn, LayoutHelper.createFrame(48, 48, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

            setOnClickListener(v -> {
                try {
                    if (parentActivity != null) {
                        AudioPlayerAlert alert = new AudioPlayerAlert(parentActivity, getResourceProvider());
                        showDialog(alert);
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
            if (playing == null || !playing.isMusic()) {
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
}
