/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import org.telegram.ui.SoundGramStyleManager;
import org.telegram.ui.SoundGramStyleSettingsFragment;
import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.core.graphics.ColorUtils;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.C;
import com.google.android.gms.cast.framework.CastContext;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.FileRefController;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.audioinfo.AudioInfo;
import org.telegram.messenger.chromecast.ChromecastController;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChannelMusicFragment;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSlider;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Adapters.FiltersView;
import org.telegram.ui.CastSync;
import org.telegram.ui.Cells.AudioPlayerCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.ChooseQualityLayout;
import org.telegram.ui.Components.Forum.ForumUtilities;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.MusicWebSearcher;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.telegram.ui.SoundGramEQAlert;
import xyz.nextalone.nagram.NaConfig;
import androidx.recyclerview.widget.LinearSmoothScroller;


public class AudioPlayerAlert extends BottomSheet implements NotificationCenter.NotificationCenterDelegate, DownloadController.FileDownloadProgressListener {
    public static final int PLAY_ONCE = 3;

    private TextView forwardButton;
    private TextView backwardButton;

    private ActionBar actionBar;
    private View navBarFiller;
    private View actionBarShadow;
    private View playerShadow;
    private boolean searchWas;
    private boolean searching;
    // Источник воспроизведения — ставится снаружи ДО открытия плеера
    public static String currentSource      = "untitled";
    public static String currentSourceLabel = "";          // доп. текст (название альбома, плейлиста и т.д.)

    public static final String SOURCE_LIBRARY  = "library";
    public static final String SOURCE_WAVE     = "wave";
    public static final String SOURCE_ALBUM    = "album";
    public static final String SOURCE_PLAYLIST = "playlist";
    public static final String SOURCE_ARTIST   = "artist";
    public static final String SOURCE_SEARCH   = "search";
    public static final String SOURCE_UNTITLED = "untitled";

    // View, которую будем обновлять при смене трека
    private TextView sourceChipTextView;
    private RecyclerListView listView;
    private LinearLayoutManager layoutManager;
    private ListAdapter listAdapter;
    private LinearLayout emptyView;
    private ImageView emptyImageView;
    private TextView emptyTitleTextView;
    private TextView emptySubtitleTextView;
    private FrameLayout lyricsContainer;
    private RecyclerListView lyricsListView;
    private LyricsAdapter lyricsAdapter;
    private ArrayList<LrcLine> lyricsLines = new ArrayList<>();
    private boolean isLyricsVisible = false;
    private ValueAnimator lyricsAnimator;
    private int currentLyricIndex = -1;
    private LinearSmoothScroller smoothScroller; // Для плавного центрального скролла

    private View borderView; // <--- ДОБАВЬ ЭТУ СТРОКУ


    private FrameLayout playerLayout;
    private ButtonWithCounterView saveToProfileButton;
    private LinkSpanDrawable.LinksTextView unsaveFromProfileTextView;
    private ItemTouchHelper itemTouchHelper;
    private CoverContainer coverContainer;
    private ClippingTextViewSwitcher titleTextView;
    private RLottieImageView prevButton;
    private RLottieImageView nextButton;
    private ClippingTextViewSwitcher authorTextView;
    private ActionBarMenuItem optionsButton;
    private ChooseQualityLayout.QualityIcon optionsIcon;
    private ActionBarMenuSubItem castItem;
    private CastMediaRouteButton castItemButton;
    private LineProgressView progressView;
    private SeekBarView seekBarView;
    private SimpleTextView timeTextView;
    private ActionBarMenuItem playbackSpeedButton;
    private SpeedIconDrawable speedIcon;
    private ActionBarMenuSlider.SpeedSlider speedSlider;
    private boolean slidingSpeed;
    private ActionBarMenuSubItem[] speedItems = new ActionBarMenuSubItem[6];
    private TextView durationTextView;
    private ActionBarMenuItem repeatButton;
    private ActionBarMenuSubItem playOnceItem;
    private ActionBarMenuSubItem repeatSongItem;
    private ActionBarMenuSubItem repeatListItem;
    private ActionBarMenuSubItem shuffleListItem;
    private ActionBarMenuSubItem reverseOrderItem;
    private ImageView playButton;
    private PlayPauseDrawable playPauseDrawable;
    private FrameLayout blurredView;
    private BackupImageView bigAlbumConver;
    private ActionBarMenuItem searchItem;
    private boolean blurredAnimationInProgress;
    private View[] buttons = new View[7];
    private SpringAnimation seekBarBufferSpring;

    private boolean draggingSeekBar;

    private long lastBufferedPositionCheck;
    private boolean currentAudioFinishedLoading;

    private boolean scrollToSong = true;

    private int searchOpenPosition = -1;
    private int searchOpenOffset;

    private boolean padWithItem;


    public static class LrcLine {
        public long timeMs;
        public String text;
        public LrcLine(long timeMs, String text) {
            this.timeMs = timeMs;
            this.text = text;
        }
    }


    private MessagesController.SavedMusicList savedMusicList;
    private boolean isMyList() {
        return savedMusicList != null && savedMusicList.dialogId == UserConfig.getInstance(currentAccount).getClientUserId();
    }

    private ArrayList<MessageObject> playlist;
    private MessageObject lastMessageObject;
    private boolean noforwards;

    private int scrollOffsetY = Integer.MAX_VALUE;
    private int topBeforeSwitch;

    private boolean inFullSize;

    private String currentFile;

    private AnimatorSet actionBarAnimation;

    private int lastTime;
    private int lastDuration;

    private int TAG;

    private LaunchActivity parentActivity;
    int rewindingState;
    float rewindingProgress = -1;

    int rewindingForwardPressedCount;
    long lastRewindingTime;
    long lastUpdateRewindingPlayerTime;

    private boolean wasLight;

    private final static float[] speeds = new float[] {
            .5f, 1f, 1.2f, 1.5f, 1.7f, 2f
    };
    private String getSourceDisplayName() {
        switch (currentSource) {
            case SOURCE_LIBRARY:  return "Библиотека";
            case SOURCE_WAVE:     return "Моя волна";
            case SOURCE_SEARCH:   return "Поиск";
            case SOURCE_ALBUM:
                return TextUtils.isEmpty(currentSourceLabel) ? "Альбом"   : currentSourceLabel;
            case SOURCE_PLAYLIST:
                return TextUtils.isEmpty(currentSourceLabel) ? "Плейлист" : currentSourceLabel;
            case SOURCE_ARTIST:
                return TextUtils.isEmpty(currentSourceLabel) ? "Артист"   : currentSourceLabel;
            default:              return "Untitled";
        }
    }



    private final Runnable forwardSeek = new Runnable() {
        @Override
        public void run() {
            long duration = MediaController.getInstance().getDuration();
            if (duration == 0 || duration == C.TIME_UNSET) {
                lastRewindingTime = System.currentTimeMillis();
                return;
            }
            float currentProgress = rewindingProgress;

            long t = System.currentTimeMillis();
            long dt = t - lastRewindingTime;
            lastRewindingTime = t;
            long updateDt = t - lastUpdateRewindingPlayerTime;
            if (rewindingForwardPressedCount == 1) {
                dt = dt * 3 - dt;
            } else if (rewindingForwardPressedCount == 2) {
                dt = dt * 6 - dt;
            } else {
                dt = dt * 12 - dt;
            }
            long currentTime = (long) (duration * currentProgress + dt);
            currentProgress = currentTime / (float) duration;
            if (currentProgress < 0) {
                currentProgress = 0;
            }
            rewindingProgress = currentProgress;
            MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
            if (messageObject != null && (messageObject.isMusic() || messageObject.isVoice())) {
                if (!MediaController.getInstance().isMessagePaused()) {
                    MediaController.getInstance().getPlayingMessageObject().audioProgress = rewindingProgress;
                }
                updateProgress(messageObject);
            }
            if (rewindingState == 1 && rewindingForwardPressedCount > 0 && MediaController.getInstance().isMessagePaused()) {
                if (updateDt > 200 || rewindingProgress == 0) {
                    lastUpdateRewindingPlayerTime = t;
                    MediaController.getInstance().seekToProgress(MediaController.getInstance().getPlayingMessageObject(), currentProgress);
                }
                if (rewindingForwardPressedCount > 0 && rewindingProgress > 0) {
                    AndroidUtilities.runOnUIThread(forwardSeek, 16);
                }
            }
        }
    };
    private void showPlayerOptionsMenu() {
        if (getContext() == null) return;

        BottomSheet.Builder builder = new BottomSheet.Builder(getContext());

        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFF1C1C1E);
            float r = AndroidUtilities.dp(20);
            bg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
            container.setBackground(bg);
        }
        container.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(24));

        // Drag-handle
        View handle = new View(getContext());
        {
            GradientDrawable hBg = new GradientDrawable();
            hBg.setColor(0xFF48484A);
            hBg.setCornerRadius(AndroidUtilities.dp(2));
            handle.setBackground(hBg);
        }
        FrameLayout hw = new FrameLayout(getContext());
        hw.addView(handle, new FrameLayout.LayoutParams(
                AndroidUtilities.dp(40), AndroidUtilities.dp(4), Gravity.CENTER));
        container.addView(hw,
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 24, 0, 0, 0, 8));

        MessageObject mo = MediaController.getInstance().getPlayingMessageObject();

        // 1. Эквалайзер
        container.addView(buildMenuRow(getContext(), "Эквалайзер", R.drawable.msg_customize, () -> {
            builder.getDismissRunnable().run();
            if (parentActivity != null) new SoundGramEQAlert(parentActivity).show();
        }));

        container.addView(buildMenuDivider(getContext()));

        // 2. В библиотеку — только если трека ещё нет
        boolean inLib = false;
        if (mo != null) {
            for (MessageObject m : org.telegram.ui.ChannelMusicFragment.audioMessages) {
                if (m.getId() == mo.getId()) { inLib = true; break; }
            }
        }
        if (!inLib) {
            container.addView(buildMenuRow(getContext(), "В библиотеку", R.drawable.msg_download, () -> {
                builder.getDismissRunnable().run();
                onSubItemClick(5);
            }));
            container.addView(buildMenuDivider(getContext()));
        }

        // 3. Поделиться
        container.addView(buildMenuRow(getContext(), "Поделиться", R.drawable.msg_shareout, () -> {
            builder.getDismissRunnable().run();
            onSubItemClick(1);
        }));

        container.addView(buildMenuDivider(getContext()));

        // 4. Добавить в плейлист
        container.addView(buildMenuRow(getContext(), "Добавить в плейлист", R.drawable.msg_add, () -> {
            builder.getDismissRunnable().run();
            onSubItemClick(100);
        }));

        builder.setCustomView(container);
        showDialog(builder.create());
    }

    // Вспомогательные методы для строк меню
    private View buildMenuRow(Context ctx, String title, int iconRes, Runnable onClick) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16),
                AndroidUtilities.dp(20), AndroidUtilities.dp(16));
        row.setBackground(Theme.createSelectorDrawable(0x18FFFFFF, 0));
        row.setOnClickListener(v -> onClick.run());

        ImageView icon = new ImageView(ctx);
        icon.setImageResource(iconRes);
        icon.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN);
        icon.setScaleType(ImageView.ScaleType.CENTER);
        row.addView(icon,
                LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL, 0, 0, 18, 0));

        TextView tv = new TextView(ctx);
        tv.setText(title);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        tv.setTextColor(0xFFFFFFFF);
        row.addView(tv,
                LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        return row;
    }

    private View buildMenuDivider(Context ctx) {
        View v = new View(ctx);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(1));
        lp.setMargins(AndroidUtilities.dp(20), 0, AndroidUtilities.dp(20), 0);
        v.setLayoutParams(lp);
        v.setBackgroundColor(0x18FFFFFF);
        return v;
    }
    public AudioPlayerAlert(final Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context, true, resourcesProvider);
        fixNavigationBar();

        try {
            shadowDrawable = context.getResources().getDrawable(R.drawable.sheet_shadow_round).mutate();
            shadowDrawable.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_dialogBackground), PorterDuff.Mode.MULTIPLY));
        } catch (Exception e) {
            shadowDrawable = new android.graphics.drawable.ColorDrawable(getThemedColor(Theme.key_dialogBackground));
        }

        MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
        if (messageObject != null) {
            currentAccount = messageObject.currentAccount;
        } else {
            currentAccount = UserConfig.selectedAccount;
        }

        parentActivity = (LaunchActivity) context;

        TAG = DownloadController.getInstance(currentAccount).generateObserverTag();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidStart);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingProgressDidChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoaded);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoadProgressChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.musicDidLoad);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.moreMusicDidLoad);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.musicIdsLoaded);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.messagePlayingSpeedChanged);

        containerView = new FrameLayout(context) {
            private RectF rect = new RectF();
            private boolean ignoreLayout = false;
            private int lastMeasturedHeight;
            private int lastMeasturedWidth;

            @Override
            public boolean onTouchEvent(MotionEvent e) {
                return !isDismissed() && super.onTouchEvent(e);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int totalHeight = MeasureSpec.getSize(heightMeasureSpec);
                int w = MeasureSpec.getSize(widthMeasureSpec);

                if (totalHeight != lastMeasturedHeight || w != lastMeasturedWidth) {
                    if (blurredView != null && blurredView.getTag() != null) {
                        showAlbumCover(false, false);
                    }
                    lastMeasturedWidth = w;
                    lastMeasturedHeight = totalHeight;
                }

                ignoreLayout = true;

                if (playerLayout != null) {
                    playerLayout.setVisibility(searchWas || keyboardVisible ? INVISIBLE : VISIBLE);
                    if (playerShadow != null) playerShadow.setVisibility(GONE); // Полностью отключаем тень

                    // Измеряем плеер как WRAP_CONTENT, чтобы он занял ровно свой объем
                    playerLayout.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(totalHeight, MeasureSpec.AT_MOST));

                    // listView скрыт — padding не нужен
                }

                if (listView != null && actionBarShadow != null) {
                    LayoutParams layoutParams = (LayoutParams) listView.getLayoutParams();
                    layoutParams.topMargin = ActionBar.getCurrentActionBarHeight() + AndroidUtilities.statusBarHeight;
                    layoutParams = (LayoutParams) actionBarShadow.getLayoutParams();
                    layoutParams.topMargin = ActionBar.getCurrentActionBarHeight() + AndroidUtilities.statusBarHeight;
                }

                if (blurredView != null) {
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) blurredView.getLayoutParams();
                    lp.topMargin = -getPaddingTop();
                }

                ignoreLayout = false;

                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(totalHeight, MeasureSpec.EXACTLY));
                inFullSize = true;
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                updateLayout();
                updateEmptyViewPosition();
            }

            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                    if (scrollOffsetY != 0 && actionBar != null && actionBar.getAlpha() == 0.0f) {
                        boolean tapOnPlayer = false;
                        if (playerLayout != null && playerLayout.getVisibility() == VISIBLE) {
                            float py = playerLayout.getTranslationY();
                            if (ev.getY() >= py && ev.getY() <= py + playerLayout.getMeasuredHeight()) {
                                tapOnPlayer = true;
                            }
                        }
                        if (!tapOnPlayer && ev.getY() < scrollOffsetY + dp(12)) {
                            dismiss();
                            return true;
                        }
                    }
                }
                return super.onInterceptTouchEvent(ev);
            }

            @Override
            public void requestLayout() {
                if (ignoreLayout) return;
                super.requestLayout();
            }

            @Override
            protected void onDraw(Canvas canvas) {
                if (listView == null || listView.getVisibility() != View.VISIBLE) return;
                int offset = dp(13);
                int top = scrollOffsetY - backgroundPaddingTop - offset;
                top += listView.getTranslationY();
                int y = top + dp(20);
                int height = getMeasuredHeight() + dp(15) + backgroundPaddingTop;
                float rad = 1.0f;

                if (top + backgroundPaddingTop < ActionBar.getCurrentActionBarHeight()) {
                    float toMove = offset + dp(11 - 7);
                    float moveProgress = Math.min(1.0f, (ActionBar.getCurrentActionBarHeight() - top - backgroundPaddingTop) / toMove);
                    float availableToMove = ActionBar.getCurrentActionBarHeight() - toMove;
                    top -= (int) (availableToMove * moveProgress);
                    y -= (int) (availableToMove * moveProgress);
                    height += (int) (availableToMove * moveProgress);
                    rad = 1.0f - moveProgress;
                }
                if (Build.VERSION.SDK_INT >= 21) {
                    top += AndroidUtilities.statusBarHeight;
                    y += AndroidUtilities.statusBarHeight;
                }
                if (shadowDrawable != null) {
                    shadowDrawable.setBounds(0, top, getMeasuredWidth(), height);
                    shadowDrawable.draw(canvas);
                }
                if (rad != 1.0f) {
                    Theme.dialogs_onlineCirclePaint.setColor(getThemedColor(Theme.key_dialogBackground));
                    rect.set(backgroundPaddingLeft, backgroundPaddingTop + top, getMeasuredWidth() - backgroundPaddingLeft, backgroundPaddingTop + top + dp(24));
                    canvas.drawRoundRect(rect, dp(12) * rad, dp(12) * rad, Theme.dialogs_onlineCirclePaint);
                }
                if (rad != 0) {
                    Theme.dialogs_onlineCirclePaint.setColor(getThemedColor(Theme.key_sheet_scrollUp));
                    Theme.dialogs_onlineCirclePaint.setAlpha((int) (Color.alpha(getThemedColor(Theme.key_sheet_scrollUp)) * rad));
                    rect.set((getMeasuredWidth() - dp(36)) / 2, y, (getMeasuredWidth() + dp(36)) / 2, y + dp(4));
                    canvas.drawRoundRect(rect, dp(2), dp(2), Theme.dialogs_onlineCirclePaint);
                }
            }
        };
        containerView.setWillNotDraw(false);
        containerView.setPadding(0, 0, 0, 0);
        containerView.setBackgroundColor(0x00000000);


        actionBar = new ActionBar(context, resourcesProvider) {
            @Override public void setAlpha(float alpha) { super.setAlpha(alpha); containerView.invalidate(); }
        };
        actionBar.setBackgroundColor(getThemedColor(Theme.key_dialogBackground));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setItemsColor(getThemedColor(Theme.key_player_actionBarTitle), false);
        actionBar.setTitleColor(getThemedColor(Theme.key_player_actionBarTitle));
        actionBar.setOccupyStatusBar(true);
        actionBar.setAlpha(0.0f);

        ActionBarMenu menu = actionBar.createMenu();
        searchItem = menu.addItem(0, R.drawable.outline_header_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override public void onSearchCollapse() { if (searching) { searchWas = false; searching = false; setAllowNestedScroll(true); if (listAdapter != null) listAdapter.search(null); } }
            @Override public void onSearchExpand() { searching = true; setAllowNestedScroll(false); if (listAdapter != null) listAdapter.notifyDataSetChanged(); }
            @Override public void onTextChanged(EditText editText) { if (listAdapter != null) { if (editText.length() > 0) listAdapter.search(editText.getText().toString()); else { searchWas = false; listAdapter.search(null); } } }
        });
        searchItem.setContentDescription("Search");
        EditTextBoldCursor editText = searchItem.getSearchField();
        editText.setHint("Search");
        editText.setTextColor(getThemedColor(Theme.key_player_actionBarTitle));
        editText.setCursorColor(getThemedColor(Theme.key_player_actionBarTitle));

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) dismiss(); else onSubItemClick(id); }
        });

        actionBarShadow = new View(context);
        actionBarShadow.setAlpha(0.0f);
        actionBarShadow.setBackgroundResource(R.drawable.header_shadow);

        playerShadow = new View(context);
        playerShadow.setBackgroundColor(getThemedColor(Theme.key_dialogShadowLine));

        playerLayout = new FrameLayout(context) {
            @Override
            public void setTranslationY(float translationY) {
                // ... твой текущий код setTranslationY ...
                if (containerView != null && getMeasuredHeight() > 0) {
                    float targetY = scrollOffsetY - getMeasuredHeight();
                    if (targetY > 0) targetY = 0;
                    super.setTranslationY(targetY);
                } else {
                    super.setTranslationY(translationY);
                }
            }

            // ДОБАВИТЬ ЭТО:
            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                // Сначала даем шанс кнопкам внутри плеера обработать нажатие
                boolean handled = super.dispatchTouchEvent(ev);

                // Если мы свайпаем по пустому месту плеера, передаем событие в список
                if (!handled && listView != null) {
                    MotionEvent e = MotionEvent.obtain(ev);
                    // Корректируем Y-координату, чтобы список правильно понял жест
                    e.offsetLocation(0, getTranslationY() - listView.getTranslationY());
                    handled = listView.onTouchEvent(e);
                    e.recycle();
                }
                return handled;
            }
        }; int playerAccent = Theme.getColor(Theme.key_featuredStickers_addButton);
        android.graphics.drawable.GradientDrawable playerGlass =
                SoundGramStyleManager.buildGlassDrawable(
                        SoundGramStyleSettingsFragment.KEY_GLASS_PLAYER,
                        SoundGramStyleSettingsFragment.KEY_ALPHA_PLAYER,
                        playerAccent, 28);
        playerLayout.setBackground(playerGlass);
        if (Build.VERSION.SDK_INT >= 21) playerLayout.setElevation(AndroidUtilities.dp(12));


        LinearLayout playerUiContainer = new LinearLayout(context);
        playerUiContainer.setOrientation(LinearLayout.VERTICAL);
        playerUiContainer.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        int navBarPad = AndroidUtilities.navigationBarHeight > 0 ? AndroidUtilities.navigationBarHeight : 0;
        int statusBarH = AndroidUtilities.statusBarHeight > 0 ? AndroidUtilities.statusBarHeight : 0;
        playerUiContainer.setPadding(0, statusBarH + AndroidUtilities.dp(12), 0, navBarPad + AndroidUtilities.dp(8));
        playerLayout.addView(playerUiContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP));

        coverContainer = new CoverContainer(context) {
            @Override
            protected void onImageUpdated(ImageReceiver imageReceiver) {
                android.graphics.drawable.GradientDrawable base = new android.graphics.drawable.GradientDrawable();
                base.setCornerRadii(new float[]{
                        AndroidUtilities.dp(28), AndroidUtilities.dp(28),
                        AndroidUtilities.dp(28), AndroidUtilities.dp(28),
                        0, 0, 0, 0
                });
                base.setStroke(AndroidUtilities.dp(1), 0x33FFFFFF);

                if (imageReceiver != null) {
                    android.graphics.Bitmap b = imageReceiver.getBitmap();
                    if (b != null && !b.isRecycled()) {
                        try {
                            int w = b.getWidth(), h = b.getHeight();
                            // Усредняем 4 угла → доминирующий цвет обложки
                            int avg = androidx.core.graphics.ColorUtils.blendARGB(
                                    androidx.core.graphics.ColorUtils.blendARGB(b.getPixel(w/4, h/4), b.getPixel(w*3/4, h/4), 0.5f),
                                    androidx.core.graphics.ColorUtils.blendARGB(b.getPixel(w/4, h*3/4), b.getPixel(w*3/4, h*3/4), 0.5f),
                                    0.5f);
                            // Тёмная версия цвета для жидкого стекла
                            int dark = androidx.core.graphics.ColorUtils.blendARGB(0xFF050508, avg, 0.35f);
                            int darker = androidx.core.graphics.ColorUtils.blendARGB(0xFF020205, avg, 0.20f);
                            base.setColors(new int[]{
                                    androidx.core.graphics.ColorUtils.setAlphaComponent(dark,   220),
                                    androidx.core.graphics.ColorUtils.setAlphaComponent(darker, 235)
                            });
                            base.setGradientType(android.graphics.drawable.GradientDrawable.LINEAR_GRADIENT);
                        } catch (Exception e) {
                            base.setColor(0xE0101018);
                        }
                    } else {
                        base.setColor(0xE0101018);
                    }
                } else {
                    base.setColor(0xE0101018);
                }
                playerLayout.setBackground(base);
            }
        };

        borderView = new View(context);
        {
            android.graphics.drawable.GradientDrawable borderDrawable = new android.graphics.drawable.GradientDrawable();
            borderDrawable.setColor(Color.TRANSPARENT);
            // Двойная рамка: внешняя белая полупрозрачная + внутренний блик сверху
            borderDrawable.setStroke(AndroidUtilities.dp(1), 0x50FFFFFF);
            borderDrawable.setCornerRadius(AndroidUtilities.dp(20));
            borderView.setBackground(borderDrawable);
            // RenderEffect blur (Android 12+) для настоящего frosted glass эффекта
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                borderView.setRenderEffect(
                        android.graphics.RenderEffect.createBlurEffect(18f, 18f,
                                android.graphics.Shader.TileMode.CLAMP));
            }
        }
        android.graphics.drawable.GradientDrawable borderDrawable = new android.graphics.drawable.GradientDrawable();
        borderDrawable.setColor(Color.TRANSPARENT);
        borderDrawable.setStroke(dp(1), 0x44FFFFFF);
        borderDrawable.setCornerRadius(dp(20)); // совпадает с cornerRadius обложки
        borderView.setBackground(borderDrawable);

        // ── TopBar: [● Источник]  ·············  [···] ─────────────────────
        FrameLayout playerTopBar = new FrameLayout(context);

        // Chip источника (● Playlist name)
        LinearLayout sourceChipContainer = new LinearLayout(context);
        sourceChipContainer.setOrientation(LinearLayout.HORIZONTAL);
        sourceChipContainer.setGravity(Gravity.CENTER_VERTICAL);
        sourceChipContainer.setPadding(
                AndroidUtilities.dp(10), AndroidUtilities.dp(6),
                AndroidUtilities.dp(14), AndroidUtilities.dp(6));
        {
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_dialogBackground), 200));
            chipBg.setCornerRadius(AndroidUtilities.dp(20));
            chipBg.setStroke(AndroidUtilities.dp(1), 0x22FFFFFF);
            sourceChipContainer.setBackground(chipBg);
        }
        View redDot = new View(context);
        {
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(0xFFFF3B3B);
            redDot.setBackground(dotBg);
        }
        sourceChipContainer.addView(redDot,
                LayoutHelper.createLinear(8, 8, Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

        sourceChipTextView = new TextView(context);
        sourceChipTextView.setText(getSourceDisplayName());
        sourceChipTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        sourceChipTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        sourceChipTextView.setTextColor(0xFFFFFFFF);
        sourceChipTextView.setSingleLine(true);
        sourceChipTextView.setEllipsize(TextUtils.TruncateAt.END);
        sourceChipContainer.addView(sourceChipTextView,
                LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        playerTopBar.addView(sourceChipContainer,
                LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.LEFT));

        // Кнопка ··· (правый верхний угол) — круглая тёмная
        ImageView topOptionsBtn = new ImageView(context);
        topOptionsBtn.setScaleType(ImageView.ScaleType.CENTER);
        topOptionsBtn.setImageResource(R.drawable.ic_ab_other);
        topOptionsBtn.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN);
        if (Build.VERSION.SDK_INT >= 21)
            topOptionsBtn.setBackgroundDrawable(Theme.createSelectorDrawable(0x33FFFFFF, 1, dp(20)));
        topOptionsBtn.setOnClickListener(v -> showPlayerOptionsMenu());
        playerTopBar.addView(topOptionsBtn,
                LayoutHelper.createFrame(40, 40, Gravity.CENTER_VERTICAL | Gravity.RIGHT));

        // topBar — первый элемент
        playerUiContainer.addView(playerTopBar,
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44,
                        Gravity.CENTER_HORIZONTAL, 20, 0, 20, 14));

        // ── Обложка ───────────────────────────────────────────────────────────
        // Обложка — большая, занимает большую часть экрана, со скруглёнными углами
        int screenWidthDp = (int) (AndroidUtilities.displaySize.x / AndroidUtilities.density);
        int coverSizeDp = Math.min(screenWidthDp - 48, 340); // почти на весь экран, отступ 24dp с каждой стороны
        FrameLayout coverFrame = new FrameLayout(context);
        coverFrame.addView(coverContainer, LayoutHelper.createFrame(coverSizeDp, coverSizeDp, Gravity.CENTER));
        coverFrame.addView(borderView, LayoutHelper.createFrame(coverSizeDp, coverSizeDp, Gravity.CENTER));
        playerUiContainer.addView(coverFrame, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT, coverSizeDp + 8,
                Gravity.CENTER_HORIZONTAL, 24, 0, 24, 0));

        // --- СОЗДАНИЕ КОНТЕЙНЕРА С ТЕКСТАМИ (ИСПРАВЛЕНО) ---
        lyricsContainer = new FrameLayout(context);
        lyricsContainer.setVisibility(View.GONE);
        lyricsContainer.setAlpha(0f);

        lyricsListView = new RecyclerListView(context);
        // ВАЖНО: Теперь LayoutManager и Adapter инициализируются сразу, зависаний не будет!
        lyricsListView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        lyricsAdapter = new LyricsAdapter(context);
        lyricsListView.setAdapter(lyricsAdapter);
        lyricsListView.setClipToPadding(false);
        lyricsListView.setPadding(0, AndroidUtilities.dp(350), 0, AndroidUtilities.dp(160));
        lyricsListView.setItemAnimator(null);
        lyricsContainer.addView(lyricsListView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        View lyricsTouchBlocker = new View(context) {
            @Override public boolean onTouchEvent(MotionEvent event) { return true; } // Проглатываем все касания
        };
        lyricsTouchBlocker.setBackgroundColor(Color.TRANSPARENT);
        lyricsContainer.addView(lyricsTouchBlocker, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        // Кладем контейнер текстов поверх всего плеера с отступом снизу, чтобы не перекрыть кнопки
        playerLayout.addView(lyricsContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP, 0, 0, 0, AndroidUtilities.dp(100)));

        // ── Название + автор + кнопка лирики ─────────────────────────────────
        FrameLayout textAndButtonsContainer = new FrameLayout(context);

        // Кнопка лирики (иконка сообщения) — только справа, как на скрине
        ImageView lyricsButton = new ImageView(context);
        lyricsButton.setImageResource(R.drawable.msg_message);
        lyricsButton.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.MULTIPLY);
        lyricsButton.setScaleType(ImageView.ScaleType.CENTER);
        if (Build.VERSION.SDK_INT >= 21) lyricsButton.setBackgroundDrawable(Theme.createSelectorDrawable(0x33FFFFFF, 1, dp(24)));
        lyricsButton.setOnClickListener(view -> toggleLyrics());
        textAndButtonsContainer.addView(lyricsButton, LayoutHelper.createFrame(48, 48, Gravity.CENTER_VERTICAL | Gravity.RIGHT, 0, 0, 4, 0));

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);

        titleTextView = new ClippingTextViewSwitcher(context) {
            @Override protected TextView createTextView() {
                final TextView textView = new MarqueeTextView(context);
                textView.setTextColor(Theme.getColor(Theme.key_player_actionBarTitle));
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
                textView.setTypeface(AndroidUtilities.bold());
                textView.setMarqueeRepeatLimit(-1);
                textView.setSingleLine(true);
                textView.setSelected(true);
                textView.setFocusable(true);
                textView.setFocusableInTouchMode(true);
                textView.setHorizontallyScrolling(true);
                textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                return textView;
            }
        };
        textLayout.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        authorTextView = new ClippingTextViewSwitcher(context) {
            @Override protected TextView createTextView() {
                final TextView textView = new MarqueeTextView(context);
                textView.setTextColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_player_actionBarTitle), 180));
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                textView.setMarqueeRepeatLimit(-1);
                textView.setSingleLine(true);
                textView.setSelected(true);
                textView.setFocusable(true);
                textView.setFocusableInTouchMode(true);
                textView.setHorizontallyScrolling(true);
                textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                return textView;
            }
        };
        textLayout.addView(authorTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));

// Обработчик нажатия на автора — переход в профиль артиста
        authorTextView.setOnClickListener(v -> {
            MessageObject playingMsg = MediaController.getInstance().getPlayingMessageObject();
            if (playingMsg == null) return;
            String fullAuthor = playingMsg.getMusicAuthor();
            String[] authors = fullAuthor.split("\\s*(,|&|\\bfeat\\.|\\bft\\.)\\s*");
            org.telegram.ui.ChannelMusicFragment dummyFragment = new org.telegram.ui.ChannelMusicFragment();
            if (authors.length > 1) {
                BottomSheet.Builder builder = new BottomSheet.Builder(getContext());
                LinearLayout container = new LinearLayout(getContext());
                container.setOrientation(LinearLayout.VERTICAL);
                container.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_dialogBackground)));
                container.setPadding(0, AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16));
                TextView sheetTitle = new TextView(getContext());
                sheetTitle.setText("Перейти к артисту");
                sheetTitle.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
                sheetTitle.setTextSize(18);
                sheetTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                sheetTitle.setGravity(Gravity.CENTER);
                sheetTitle.setPadding(0, 0, 0, AndroidUtilities.dp(16));
                container.addView(sheetTitle);
                for (String authorName : authors) {
                    String cleanName = authorName.trim();
                    if (cleanName.isEmpty()) continue;
                    LinearLayout row = new LinearLayout(getContext());
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(12));
                    row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 0));
                    ImageView icon = new ImageView(getContext());
                    icon.setImageResource(R.drawable.msg_allowspeak);
                    icon.setColorFilter(Theme.getColor(Theme.key_dialogIcon), PorterDuff.Mode.SRC_IN);
                    row.addView(icon, LayoutHelper.createLinear(24, 24));
                    TextView text = new TextView(getContext());
                    text.setText(cleanName);
                    text.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
                    text.setTextSize(16);
                    row.addView(text, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 16, 0, 0, 0));
                    row.setOnClickListener(v1 -> {
                        builder.getDismissRunnable().run();
                        parentActivity.presentFragment(dummyFragment.new ArtistProfileFragment(cleanName, "", ""));
                        dismiss();
                    });
                    container.addView(row);
                }
                builder.setCustomView(container);
                builder.show();
            } else {
                parentActivity.presentFragment(dummyFragment.new ArtistProfileFragment(fullAuthor, "", ""));
                dismiss();
            }
        });

        // Текст занимает всё слева, кнопка лирики — справа (48dp)
        textAndButtonsContainer.addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 4, 0, 56, 0));
        playerUiContainer.addView(textAndButtonsContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 20, 14, 20, 0));seekBarView = new SeekBarView(context, resourcesProvider) {
            @Override boolean onTouch(MotionEvent ev) { return rewindingState == 0 && super.onTouch(ev); }
        };
        seekBarView.setLineWidth(4);
        seekBarView.setSliderStyleOverride(SeekBarView.SLIDER_STYLE_DEFAULT);
        seekBarView.setDelegate(new SeekBarView.SeekBarViewDelegate() {
            @Override public void onSeekBarDrag(boolean stop, float progress) {
                if (stop) MediaController.getInstance().seekToProgress(MediaController.getInstance().getPlayingMessageObject(), progress);
                MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
                if (messageObject != null && (messageObject.isMusic() || messageObject.isVoice())) updateProgress(messageObject);
            }
            @Override public void onSeekBarPressed(boolean pressed) { draggingSeekBar = pressed; }
            @Override public CharSequence getContentDescription() { return ""; }
        });
        seekBarView.setReportChanges(true);

        seekBarBufferSpring = new SpringAnimation(new FloatValueHolder(0)).setSpring(new SpringForce().setStiffness(750f).setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)).addUpdateListener((animation, value, velocity) -> {
            if (seekBarView != null) seekBarView.setBufferedProgress(value / 1000f);
        });

        progressView = new LineProgressView(context);
        progressView.setVisibility(View.INVISIBLE);

        timeTextView = new SimpleTextView(context);
        timeTextView.setTextSize(12);
        timeTextView.setText("0:00");
        timeTextView.setTextColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_player_actionBarTitle), 170));

        durationTextView = new TextView(context);
        durationTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        durationTextView.setTextColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_player_actionBarTitle), 170));
        durationTextView.setGravity(Gravity.RIGHT);
        int accentColor = Theme.getColor(Theme.key_featuredStickers_addButton);
        int bgColor = Theme.getColor(Theme.key_dialogBackground);
        FrameLayout seekFrame = new FrameLayout(context);
        seekFrame.setBackground(SoundGramStyleManager.buildGlassDrawable(
                SoundGramStyleSettingsFragment.KEY_GLASS_PLAYER,
                SoundGramStyleSettingsFragment.KEY_ALPHA_PLAYER,
                SoundGramStyleSettingsFragment.KEY_COLOR_BG_PLAYER,
                SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_PLAYER,
                20f));
        SoundGramStyleManager.addStyleListener(seekFrame, () ->
                seekFrame.setBackground(SoundGramStyleManager.buildGlassDrawable(
                        SoundGramStyleSettingsFragment.KEY_GLASS_PLAYER,
                        SoundGramStyleSettingsFragment.KEY_ALPHA_PLAYER,
                        SoundGramStyleSettingsFragment.KEY_COLOR_BG_PLAYER,
                        SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_PLAYER,
                        20f)));
        seekFrame.addView(seekBarView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 40, Gravity.TOP | Gravity.LEFT, 16, 0, 16, 0));
        seekFrame.addView(progressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 2, Gravity.TOP | Gravity.LEFT, 21, 20, 21, 0));
        seekFrame.addView(timeTextView, LayoutHelper.createFrame(100, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 24, 25, 0, 0));
        seekFrame.addView(durationTextView, LayoutHelper.createFrame(100, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT, 0, 25, 24, 0));
        playerUiContainer.addView(seekFrame, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 50, Gravity.CENTER_HORIZONTAL, 20, 12, 20, 8));

        buttons = new View[7];
        final int iconColor = Theme.getColor(Theme.key_player_actionBarTitle);

        FrameLayout hiddenContainer = new FrameLayout(context);
        hiddenContainer.setVisibility(View.GONE);
        playerLayout.addView(hiddenContainer, LayoutHelper.createFrame(0, 0));

        playbackSpeedButton = new ActionBarMenuItem(context, null, 0, 0, false, resourcesProvider);
        hiddenContainer.addView(playbackSpeedButton);
        speedSlider = new ActionBarMenuSlider.SpeedSlider(getContext(), resourcesProvider);
        for (int i = 0; i < 6; i++) speedItems[i] = playbackSpeedButton.addSubItem(i, R.drawable.msg_speed_normal, "");

        buttons[5] = forwardButton = new TextView(context);
        buttons[1] = backwardButton = new TextView(context);
        hiddenContainer.addView(forwardButton);
        hiddenContainer.addView(backwardButton);

        FrameLayout mediaButtonsContainer = new FrameLayout(context);
        {
            mediaButtonsContainer.setBackground(SoundGramStyleManager.buildGlassDrawable(
                    SoundGramStyleSettingsFragment.KEY_GLASS_PLAYER,
                    SoundGramStyleSettingsFragment.KEY_ALPHA_PLAYER,
                    SoundGramStyleSettingsFragment.KEY_COLOR_BG_PLAYER,
                    SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_PLAYER,
                    40f));
            SoundGramStyleManager.addStyleListener(mediaButtonsContainer, () ->
                    mediaButtonsContainer.setBackground(SoundGramStyleManager.buildGlassDrawable(
                            SoundGramStyleSettingsFragment.KEY_GLASS_PLAYER,
                            SoundGramStyleSettingsFragment.KEY_ALPHA_PLAYER,
                            SoundGramStyleSettingsFragment.KEY_COLOR_BG_PLAYER,
                            SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_PLAYER,
                            40f)));
            if (Build.VERSION.SDK_INT >= 21) mediaButtonsContainer.setElevation(AndroidUtilities.dp(2));
        }

        buttons[3] = playButton = new ImageView(context);
        playButton.setScaleType(ImageView.ScaleType.CENTER);
        playButton.setImageDrawable(playPauseDrawable = new PlayPauseDrawable(36));
        playPauseDrawable.setPause(!MediaController.getInstance().isMessagePaused(), false);
        playButton.setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
        if (Build.VERSION.SDK_INT >= 21) playButton.setBackgroundDrawable(Theme.createSelectorDrawable(0x33FFFFFF, 1, dp(36)));
        playButton.setOnClickListener(v -> {
            if (MediaController.getInstance().isDownloadingCurrentMessage()) return;
            if (MediaController.getInstance().isMessagePaused()) MediaController.getInstance().playMessage(MediaController.getInstance().getPlayingMessageObject());
            else MediaController.getInstance().pauseMessage(MediaController.getInstance().getPlayingMessageObject());
        });
        mediaButtonsContainer.addView(playButton, LayoutHelper.createFrame(72, 72, Gravity.CENTER, 0, 0, 0, 0));

        buttons[2] = prevButton = new RLottieImageView(context) {
            float startX; float startY; int pressedCount = 0; long lastTime; long lastUpdateTime;
            private final Runnable pressedRunnable = new Runnable() {
                @Override public void run() {
                    pressedCount++;
                    if (pressedCount == 1) {
                        rewindingState = -1;
                        if (MediaController.getInstance().getPlayingMessageObject() != null) rewindingProgress = MediaController.getInstance().getPlayingMessageObject().audioProgress;
                        lastTime = System.currentTimeMillis();
                        AndroidUtilities.runOnUIThread(this, 2000); AndroidUtilities.runOnUIThread(backSeek);
                    } else if (pressedCount == 2) AndroidUtilities.runOnUIThread(this, 2000);
                }
            };
            private final Runnable backSeek = new Runnable() {
                @Override public void run() {
                    long duration = MediaController.getInstance().getDuration();
                    if (duration == 0 || duration == C.TIME_UNSET) { lastTime = System.currentTimeMillis(); return; }
                    float currentProgress = rewindingProgress;
                    long t = System.currentTimeMillis(); long dt = t - lastTime; lastTime = t; long updateDt = t - lastUpdateTime;
                    if (pressedCount == 1) dt *= 3; else if (pressedCount == 2) dt *= 6; else dt *= 12;
                    long currentTime = (long) (duration * currentProgress - dt);
                    currentProgress = currentTime / (float) duration;
                    if (currentProgress < 0) currentProgress = 0;
                    rewindingProgress = currentProgress;
                    MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
                    if (messageObject != null && (messageObject.isMusic() || messageObject.isVoice())) updateProgress(messageObject);
                    if (rewindingState == -1 && pressedCount > 0) {
                        if (updateDt > 200 || rewindingProgress == 0) {
                            lastUpdateTime = t;
                            if (rewindingProgress == 0) {
                                if (MediaController.getInstance().getPlayingMessageObject() != null) { MediaController.getInstance().seekToProgress(MediaController.getInstance().getPlayingMessageObject(), 0); MediaController.getInstance().pauseByRewind(); }
                            } else {
                                if (MediaController.getInstance().getPlayingMessageObject() != null) MediaController.getInstance().seekToProgress(MediaController.getInstance().getPlayingMessageObject(), currentProgress);
                            }
                        }
                        if (pressedCount > 0 && rewindingProgress > 0) AndroidUtilities.runOnUIThread(backSeek, 16);
                    }
                }
            };
            long startTime;
            @Override public boolean onTouchEvent(MotionEvent event) {
                if (seekBarView != null && (seekBarView.isDragging() || rewindingState == 1)) return false;
                float x = event.getRawX(); float y = event.getRawY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = x; startY = y; startTime = System.currentTimeMillis(); rewindingState = 0; AndroidUtilities.runOnUIThread(pressedRunnable, 300);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getBackground() != null) getBackground().setHotspot(startX, startY);
                        setPressed(true); break;
                    case MotionEvent.ACTION_MOVE:
                        float dx = x - startX; float dy = y - startY;
                        if ((dx * dx + dy * dy) > 100 && rewindingState == 0) { AndroidUtilities.cancelRunOnUIThread(pressedRunnable); setPressed(false); } break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        AndroidUtilities.cancelRunOnUIThread(pressedRunnable); AndroidUtilities.cancelRunOnUIThread(backSeek);
                        if (rewindingState == 0 && event.getAction() == MotionEvent.ACTION_UP && (System.currentTimeMillis() - startTime < 300)) {
                            MediaController.getInstance().playPreviousMessage(); prevButton.setProgress(0f); prevButton.playAnimation();
                        }
                        if (pressedCount > 0) { lastUpdateTime = 0; backSeek.run(); MediaController.getInstance().resumeByRewind(); }
                        rewindingProgress = -1; setPressed(false); rewindingState = 0; pressedCount = 0; break;
                }
                return true;
            }
        };
        prevButton.setScaleType(ImageView.ScaleType.CENTER);
        prevButton.setAnimation(R.raw.player_prev, 24, 24);
        prevButton.setLayerColor("Triangle 3.**", iconColor);
        prevButton.setLayerColor("Triangle 4.**", iconColor);
        prevButton.setLayerColor("Rectangle 4.**", iconColor);
        if (Build.VERSION.SDK_INT >= 21) prevButton.setBackgroundDrawable(Theme.createSelectorDrawable(0x33FFFFFF, 1, dp(24)));
        mediaButtonsContainer.addView(prevButton, LayoutHelper.createFrame(64, 64, Gravity.CENTER, -90, 0, 0, 0));

        buttons[4] = nextButton = new RLottieImageView(context) {
            float startX; float startY; boolean pressed;
            private final Runnable pressedRunnable = new Runnable() {
                @Override public void run() {
                    if (MediaController.getInstance().getPlayingMessageObject() == null) return;
                    rewindingForwardPressedCount++;
                    if (rewindingForwardPressedCount == 1) {
                        pressed = true; rewindingState = 1;
                        if (MediaController.getInstance().isMessagePaused()) startForwardRewindingSeek();
                        else if (rewindingState == 1) { AndroidUtilities.cancelRunOnUIThread(forwardSeek); lastUpdateRewindingPlayerTime = 0; }
                        MediaController.getInstance().setPlaybackSpeed(true, 4); AndroidUtilities.runOnUIThread(this, 2000);
                    } else if (rewindingForwardPressedCount == 2) { MediaController.getInstance().setPlaybackSpeed(true, 7); AndroidUtilities.runOnUIThread(this, 2000); }
                    else MediaController.getInstance().setPlaybackSpeed(true, 13);
                }
            };
            @Override public boolean onTouchEvent(MotionEvent event) {
                if (seekBarView != null && (seekBarView.isDragging() || rewindingState == -1)) return false;
                float x = event.getRawX(); float y = event.getRawY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        pressed = false; startX = x; startY = y; AndroidUtilities.runOnUIThread(pressedRunnable, 300);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getBackground() != null) getBackground().setHotspot(startX, startY);
                        setPressed(true); break;
                    case MotionEvent.ACTION_MOVE:
                        float dx = x - startX; float dy = y - startY;
                        if ((dx * dx + dy * dy) > 100 && !pressed) { AndroidUtilities.cancelRunOnUIThread(pressedRunnable); setPressed(false); } break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        if (!pressed && event.getAction() == MotionEvent.ACTION_UP && isPressed()) { MediaController.getInstance().playNextMessage(); nextButton.setProgress(0f); nextButton.playAnimation(); }
                        AndroidUtilities.cancelRunOnUIThread(pressedRunnable);
                        if (rewindingForwardPressedCount > 0) {
                            MediaController.getInstance().setPlaybackSpeed(true, 1f);
                            if (MediaController.getInstance().isMessagePaused()) { lastUpdateRewindingPlayerTime = 0; forwardSeek.run(); }
                        }
                        rewindingState = 0; setPressed(false); rewindingForwardPressedCount = 0; rewindingProgress = -1; break;
                }
                return true;
            }
        };
        nextButton.setScaleType(ImageView.ScaleType.CENTER);
        nextButton.setAnimation(R.raw.player_prev, 24, 24);
        nextButton.setLayerColor("Triangle 3.**", iconColor);
        nextButton.setLayerColor("Triangle 4.**", iconColor);
        nextButton.setLayerColor("Rectangle 4.**", iconColor);
        nextButton.setRotation(180f);
        if (Build.VERSION.SDK_INT >= 21) nextButton.setBackgroundDrawable(Theme.createSelectorDrawable(0x33FFFFFF, 1, dp(24)));
        mediaButtonsContainer.addView(nextButton, LayoutHelper.createFrame(64, 64, Gravity.CENTER, 90, 0, 0, 0));

        buttons[0] = repeatButton = new ActionBarMenuItem(context, null, 0, 0, false, resourcesProvider);
        repeatButton.setLongClickEnabled(false);
        repeatButton.setShowSubmenuByMove(false);
        repeatButton.setAdditionalYOffset(-dp(166 + 48));
        if (Build.VERSION.SDK_INT >= 21) repeatButton.setBackgroundDrawable(Theme.createSelectorDrawable(0x33FFFFFF, 1, dp(24)));
        repeatButton.setOnClickListener(v -> { updateSubMenu(); repeatButton.toggleSubMenu(); });
        playOnceItem = repeatButton.addSubItem(5, R.drawable.photo_timer, LocaleController.getString(R.string.PlayOnce));
        repeatSongItem = repeatButton.addSubItem(3, R.drawable.player_new_repeatone, LocaleController.getString(R.string.RepeatSong));
        repeatListItem = repeatButton.addSubItem(4, R.drawable.player_new_repeatall, LocaleController.getString(R.string.RepeatList));
        shuffleListItem = repeatButton.addSubItem(2, R.drawable.player_new_shuffle, LocaleController.getString(R.string.ShuffleList));
        reverseOrderItem = repeatButton.addSubItem(1, R.drawable.player_new_order, LocaleController.getString(R.string.ReverseOrder));
        repeatButton.setShowedFromBottom(true);
        repeatButton.setDelegate(id -> {
            if (id == 1 || id == 2) {
                boolean oldReversed = SharedConfig.playOrderReversed;
                if (SharedConfig.playOrderReversed && id == 1 || SharedConfig.shuffleMusic && id == 2) MediaController.getInstance().setPlaybackOrderType(0);
                else MediaController.getInstance().setPlaybackOrderType(id);
                if (listAdapter != null) listAdapter.notifyDataSetChanged();
                if (oldReversed != SharedConfig.playOrderReversed) { if (listView != null) listView.stopScroll(); scrollToCurrentSong(false); }
            } else {
                if (id == 4) { if (SharedConfig.repeatMode == 1) SharedConfig.setRepeatMode(0); else SharedConfig.setRepeatMode(1); }
                else if (id == 5) { if (SharedConfig.repeatMode == PLAY_ONCE) SharedConfig.setRepeatMode(0); else SharedConfig.setRepeatMode(PLAY_ONCE); }
                else { if (SharedConfig.repeatMode == 2) SharedConfig.setRepeatMode(0); else SharedConfig.setRepeatMode(2); }
            }
            updateRepeatButton();
        });

        buttons[6] = optionsButton = new ActionBarMenuItem(context, null, 0, iconColor, false, resourcesProvider);
        optionsButton.setIcon(optionsIcon = new ChooseQualityLayout.QualityIcon(context, R.drawable.ic_ab_other, resourcesProvider));
        optionsButton.setLongClickEnabled(false);
        optionsButton.setShowSubmenuByMove(false);
        optionsButton.setSubMenuOpenSide(1);
        optionsButton.setAdditionalYOffset(-dp(157 + 40));
        if (Build.VERSION.SDK_INT >= 21) optionsButton.setBackgroundDrawable(Theme.createSelectorDrawable(0x33FFFFFF, 1, dp(24)));
        optionsButton.addSubItem(1, R.drawable.msg_shareout, "Поделиться");

// Проверяем, есть ли трек в библиотеке
        boolean isInLibrary = false;
        if (messageObject != null) {
            for (MessageObject libMsg : org.telegram.ui.ChannelMusicFragment.audioMessages) {
                if (libMsg.getId() == messageObject.getId()
                        || (libMsg.messageOwner.message != null
                        && messageObject.messageOwner.message != null
                        && libMsg.messageOwner.message.equals(messageObject.messageOwner.message))) {
                    isInLibrary = true;
                    break;
                }
            }
        }
        if (!isInLibrary) {
            optionsButton.addSubItem(5, R.drawable.msg_download, "В библиотеку");
        }

        optionsButton.addSubItem(100, R.drawable.msg_add, "Добавить в плейлист");
        optionsButton.addSubItem(7, R.drawable.msg_saved, "Добавить в профиль");
// id=4 (ShowInChat) — УБРАН
        optionsButton.setSubItemShown(7, false);
        optionsButton.setShowedFromBottom(true);
        optionsButton.setOnClickListener(v -> optionsButton.toggleSubMenu());
        optionsButton.setDelegate(this::onSubItemClick);
        FrameLayout bottomControls = new FrameLayout(context);

        // Центр: prev | play | next
        bottomControls.addView(mediaButtonsContainer,
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 80, Gravity.CENTER));

        // Слева: зацикливание (loop one)
        final ImageView loopOneBtn = new ImageView(context);
        loopOneBtn.setScaleType(ImageView.ScaleType.CENTER);
        loopOneBtn.setImageResource(R.drawable.player_new_repeatone);
        loopOneBtn.setColorFilter(
                SharedConfig.repeatMode == 2
                        ? Theme.getColor(Theme.key_featuredStickers_addButton)
                        : 0x80FFFFFF,
                PorterDuff.Mode.SRC_IN);
        if (Build.VERSION.SDK_INT >= 21)
            loopOneBtn.setBackgroundDrawable(Theme.createSelectorDrawable(0x33FFFFFF, 1, dp(24)));
        loopOneBtn.setOnClickListener(v -> {
            if (SharedConfig.repeatMode == 2) SharedConfig.setRepeatMode(0);
            else SharedConfig.setRepeatMode(2);
            loopOneBtn.setColorFilter(
                    SharedConfig.repeatMode == 2
                            ? Theme.getColor(Theme.key_featuredStickers_addButton)
                            : 0x80FFFFFF,
                    PorterDuff.Mode.SRC_IN);
        });
        bottomControls.addView(loopOneBtn,
                LayoutHelper.createFrame(48, 48, Gravity.CENTER_VERTICAL | Gravity.LEFT, 8, 0, 0, 0));

        // Справа: перемешивание (shuffle)
        final ImageView shuffleBtn = new ImageView(context);
        shuffleBtn.setScaleType(ImageView.ScaleType.CENTER);
        shuffleBtn.setImageResource(R.drawable.player_new_shuffle);
        shuffleBtn.setColorFilter(
                SharedConfig.shuffleMusic
                        ? Theme.getColor(Theme.key_featuredStickers_addButton)
                        : 0x80FFFFFF,
                PorterDuff.Mode.SRC_IN);
        if (Build.VERSION.SDK_INT >= 21)
            shuffleBtn.setBackgroundDrawable(Theme.createSelectorDrawable(0x33FFFFFF, 1, dp(24)));
        shuffleBtn.setOnClickListener(v -> {
            MediaController.getInstance().setPlaybackOrderType(SharedConfig.shuffleMusic ? 0 : 2);
            shuffleBtn.setColorFilter(
                    SharedConfig.shuffleMusic
                            ? Theme.getColor(Theme.key_featuredStickers_addButton)
                            : 0x80FFFFFF,
                    PorterDuff.Mode.SRC_IN);
        });
        bottomControls.addView(shuffleBtn,
                LayoutHelper.createFrame(48, 48, Gravity.CENTER_VERTICAL | Gravity.RIGHT, 0, 0, 8, 0));

        // Кнопки repeat/shuffle — прижаты к низу экрана
        int navBarH2 = AndroidUtilities.navigationBarHeight > 0 ? AndroidUtilities.navigationBarHeight : 0;
        playerLayout.addView(bottomControls,
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 80,
                        Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL,
                        8, 0, 8, navBarH2 > 0 ? (int)(navBarH2 / AndroidUtilities.density) + 8 : 16));


        emptyView = new LinearLayout(context);
        emptyView.setOrientation(LinearLayout.VERTICAL);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        containerView.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        emptyView.setOnTouchListener((v, event) -> true);

        emptyImageView = new ImageView(context);
        emptyImageView.setImageResource(R.drawable.music_empty);
        emptyImageView.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_dialogEmptyImage), PorterDuff.Mode.MULTIPLY));
        emptyView.addView(emptyImageView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        emptyTitleTextView = new TextView(context);
        emptyTitleTextView.setTextColor(getThemedColor(Theme.key_dialogEmptyText));
        emptyTitleTextView.setGravity(Gravity.CENTER);
        emptyTitleTextView.setText(LocaleController.getString(R.string.NoAudioFound));
        emptyTitleTextView.setTypeface(AndroidUtilities.bold());
        emptyTitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        emptyTitleTextView.setPadding(dp(40), 0, dp(40), 0);
        emptyView.addView(emptyTitleTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 11, 0, 0));

        emptySubtitleTextView = new TextView(context);
        emptySubtitleTextView.setTextColor(getThemedColor(Theme.key_dialogEmptyText));
        emptySubtitleTextView.setGravity(Gravity.CENTER);
        emptySubtitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        emptySubtitleTextView.setPadding(dp(40), 0, dp(40), 0);
        emptyView.addView(emptySubtitleTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 6, 0, 0));

        listView = new RecyclerListView(context) {
            boolean ignoreLayout;
            @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
                super.onLayout(changed, l, t, r, b);
                if (searchOpenPosition != -1 && actionBar != null && !actionBar.isSearchFieldVisible()) {
                    ignoreLayout = true;
                    if (layoutManager != null) layoutManager.scrollToPositionWithOffset(searchOpenPosition, searchOpenOffset - listView.getPaddingTop());
                    super.onLayout(false, l, t, r, b);
                    ignoreLayout = false;
                    searchOpenPosition = -1;
                } else if (scrollToSong) {
                    scrollToSong = false;
                    ignoreLayout = true;
                    if (scrollToCurrentSong(true)) super.onLayout(false, l, t, r, b);
                    ignoreLayout = false;
                }
            }
            @Override protected boolean allowSelectChildAtPosition(float x, float y) {
                return playerLayout != null && y > playerLayout.getTranslationY() + playerLayout.getMeasuredHeight();
            }
            @Override public void requestLayout() { if (!ignoreLayout) super.requestLayout(); }
        };
        listView.setClipToPadding(false);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        listView.setHorizontalScrollBarEnabled(false);
        listView.setVerticalScrollBarEnabled(false);
        containerView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setVisibility(View.GONE); // Список треков скрыт — плеер на весь экран
        listView.setAdapter(listAdapter = new ListAdapter(context));
        listView.setGlowColor(getThemedColor(Theme.key_dialogScrollGlow));
        listView.setOnItemClickListener((view, position) -> { if (view instanceof AudioPlayerCell) ((AudioPlayerCell) view).didPressedButton(); });
        listView.setOnItemLongClickListener((view, position) -> {
            if (view instanceof AudioPlayerCell && !isMyList()) { showOptions((AudioPlayerCell) view, ((AudioPlayerCell) view).getMessageObject()); return true; }
            return false;
        });

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) AndroidUtilities.hideKeyboard(getCurrentFocus());
            }
            @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                updateLayout(); // Родная логика Telegram идеально позиционирует WRAP_CONTENT плеер
                updateEmptyViewPosition();

                if (!searchWas) {
                    int firstVisibleItem = layoutManager != null ? layoutManager.findFirstVisibleItemPosition() : 0;
                    if (padWithItem) firstVisibleItem = Math.max(0, firstVisibleItem - 1);
                    int visibleItemCount = firstVisibleItem == RecyclerView.NO_POSITION ? 0 : Math.abs((layoutManager != null ? layoutManager.findLastVisibleItemPosition() : 0) - firstVisibleItem) + 1;
                    int totalItemCount = recyclerView.getAdapter() != null ? recyclerView.getAdapter().getItemCount() : 0;
                    if (SharedConfig.playOrderReversed) { if (firstVisibleItem < 10) MediaController.getInstance().loadMoreMusic(); }
                    else { if (firstVisibleItem + visibleItemCount > totalItemCount - 10) MediaController.getInstance().loadMoreMusic(); }
                }
            }
        });

        unsaveFromProfileTextView = new LinkSpanDrawable.LinksTextView(context, resourcesProvider);
        unsaveFromProfileTextView.setVisibility(View.GONE);
        playerLayout.addView(unsaveFromProfileTextView, LayoutHelper.createFrame(0, 0));

        saveToProfileButton = new ButtonWithCounterView(context, resourcesProvider);
        saveToProfileButton.setVisibility(View.GONE);
        playerLayout.addView(saveToProfileButton, LayoutHelper.createFrame(0, 0));

        savedMusicList = MediaController.getInstance().currentSavedMusicList;

        padWithItem = false;

        playlist = MediaController.getInstance().getPlaylist();
        if (listAdapter != null) { listAdapter.setup(); listAdapter.notifyDataSetChanged(); }

        actionBar.setTitle(LocaleController.getString(R.string.AttachMusic));
        if (savedMusicList != null) {
            if (savedMusicList.dialogId == UserConfig.getInstance(currentAccount).getClientUserId()) actionBar.setTitle(getString(R.string.ProfilePlaylistTitleMine));
            else actionBar.setTitle(formatString(R.string.ProfilePlaylistTitle, DialogObject.getShortName(savedMusicList.dialogId)));
        } else if (messageObject != null && !MediaController.getInstance().currentPlaylistIsGlobalSearch()) {
            long did = messageObject.getDialogId();
            if (DialogObject.isEncryptedDialog(did)) {
                TLRPC.EncryptedChat encryptedChat = MessagesController.getInstance(currentAccount).getEncryptedChat(DialogObject.getEncryptedChatId(did));
                if (encryptedChat != null) {
                    TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(encryptedChat.user_id);
                    if (user != null) actionBar.setTitle(ContactsController.formatName(user.first_name, user.last_name));
                }
            } else if (did == UserConfig.getInstance(currentAccount).getClientUserId()) {
                if (messageObject.getSavedDialogId() == UserObject.ANONYMOUS) actionBar.setTitle(LocaleController.getString(R.string.AnonymousForward));
                else actionBar.setTitle(LocaleController.getString(R.string.SavedMessages));
            } else if (DialogObject.isUserDialog(did)) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(did);
                if (user != null) actionBar.setTitle(ContactsController.formatName(user.first_name, user.last_name));
            } else {
                TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-did);
                if (chat != null) actionBar.setTitle(chat.title);
            }
        }

        if (isMyList()) {
            itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
                @Override public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    if (viewHolder.getItemViewType() != 0) return 0;
                    return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
                }
                @Override public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    int fromPosition = viewHolder.getAdapterPosition(); int toPosition = target.getAdapterPosition();
                    if (padWithItem) {
                        if (fromPosition <= 0 || toPosition <= 0) return false;
                        savedMusicList.move(fromPosition - 1, toPosition - 1);
                    } else savedMusicList.move(fromPosition, toPosition);
                    if (playlist != null) { playlist.clear(); playlist.addAll(savedMusicList.list); }
                    if (listAdapter != null) listAdapter.notifyItemMoved(fromPosition, toPosition);
                    return true;
                }
                @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
                @Override public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                    if (viewHolder != null && listView != null) listView.hideSelector(false);
                    if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                        if (listView != null) listView.cancelClickRunnables(false);
                        if (viewHolder != null) viewHolder.itemView.setPressed(true);
                    }
                    super.onSelectedChanged(viewHolder, actionState);
                }
                @Override public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder); viewHolder.itemView.setPressed(false);
                }
            });
            if (listView != null) itemTouchHelper.attachToRecyclerView(listView);
        }

        View navBarFiller = new View(context) {
            @Override protected void onDraw(Canvas canvas) {
                // Берём цвет фона самого playerLayout
                if (playerLayout != null && playerLayout.getBackground() instanceof android.graphics.drawable.ColorDrawable) {
                    canvas.drawColor(((android.graphics.drawable.ColorDrawable) playerLayout.getBackground()).getColor());
                } else {
                    canvas.drawColor(0xFF111111);
                }
            }
        };
        containerView.addView(playerLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        actionBarShadow.setVisibility(View.GONE); // скрываем — список треков убран
        containerView.addView(actionBarShadow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 3));
        actionBar.setVisibility(View.GONE); // скрываем — список треков убран
        containerView.addView(actionBar);
        blurredView = new FrameLayout(context) {
            @Override public boolean onTouchEvent(MotionEvent event) {
                if (blurredView != null && blurredView.getTag() != null) showAlbumCover(false, true);
                return true;
            }
        };
        blurredView.setAlpha(0.0f);
        blurredView.setVisibility(View.INVISIBLE);
        getContainer().addView(blurredView);

        updateTitle(false);
        try { updateRepeatButton(); } catch (Exception ignored) {}
        try { updateEmptyView(); } catch (Exception ignored) {}
    }

    @Override
    public int getContainerViewHeight() {
        if (containerView == null) return 0;
        return containerView.getMeasuredHeight();
    }

    private void startForwardRewindingSeek() {
        if (rewindingState == 1) {
            lastRewindingTime = System.currentTimeMillis();
            rewindingProgress = MediaController.getInstance().getPlayingMessageObject().audioProgress;
            AndroidUtilities.cancelRunOnUIThread(forwardSeek);
            AndroidUtilities.runOnUIThread(forwardSeek);
        }
    }

    private void updateEmptyViewPosition() {
        if (emptyView.getVisibility() != View.VISIBLE) {
            return;
        }
        int h = playerLayout.getVisibility() == View.VISIBLE ? dp(150) : -dp(30);
        emptyView.setTranslationY((emptyView.getMeasuredHeight() - containerView.getMeasuredHeight() - h) / 2);
    }

    private void updateEmptyView() {
        emptyView.setVisibility(searching && listAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        updateEmptyViewPosition();
    }

    private boolean scrollToCurrentSong(boolean search) {
        if (layoutManager == null) return false;
        // ИСПРАВЛЕНИЕ: Принудительный скролл на 0 (самый верх, чтобы был виден плеер на весь экран)
        layoutManager.scrollToPositionWithOffset(0, 0);
        return true;
    }


    @Override
    public boolean onCustomMeasure(View view, int width, int height) {
        boolean isPortrait = width < height;
        if (view == blurredView) {
            blurredView.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            return true;
        }
        return false;
    }

    @Override
    protected boolean onCustomLayout(View view, int left, int top, int right, int bottom) {
        int width = (right - left);
        int height = (bottom - top);
        boolean isPortrait = width < height;
        if (view == blurredView) {
            blurredView.layout(left, 0, left + width, height);
            return true;
        }
        return false;
    }

    private void setMenuItemChecked(ActionBarMenuSubItem item, boolean checked) {
        if (checked) {
            item.setTextColor(getThemedColor(Theme.key_player_buttonActive));
            item.setIconColor(getThemedColor(Theme.key_player_buttonActive));
        } else {
            item.setTextColor(getThemedColor(Theme.key_actionBarDefaultSubmenuItem));
            item.setIconColor(getThemedColor(Theme.key_actionBarDefaultSubmenuItem));
        }
    }

    private HintView speedHintView;
    private long lastPlaybackClick;

    private void checkSpeedHint() {
        final long now = System.currentTimeMillis();
        if (now - lastPlaybackClick > 300) {
            int hintValue = MessagesController.getGlobalNotificationsSettings().getInt("speedhint", 0);
            hintValue++;
            if (hintValue > 2) {
                hintValue = -10;
            }
            MessagesController.getGlobalNotificationsSettings().edit().putInt("speedhint", hintValue).apply();
            if (hintValue >= 0) {
                showSpeedHint();
            }
        }
        lastPlaybackClick = now;
    }

    private void showSpeedHint() {
        if (containerView != null) {
            speedHintView = new HintView(getContext(), 5, false) {
                @Override
                public void setVisibility(int visibility) {
                    super.setVisibility(visibility);
                    if (visibility != View.VISIBLE) {
                        try {
                            ((ViewGroup) getParent()).removeView(this);
                        } catch (Exception e) {}
                    }
                }
            };
            speedHintView.setExtraTranslationY(dp(6));
            speedHintView.setText(LocaleController.getString("SpeedHint"));
            playerLayout.addView(speedHintView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, 0, 6, 0));
            speedHintView.showForView(playbackSpeedButton, true);
        }
    }

    private void updateSubMenu() {
        setMenuItemChecked(shuffleListItem, SharedConfig.shuffleMusic);
        setMenuItemChecked(reverseOrderItem, SharedConfig.playOrderReversed);
        setMenuItemChecked(repeatListItem, SharedConfig.repeatMode == 1);
        setMenuItemChecked(repeatSongItem, SharedConfig.repeatMode == 2);
        setMenuItemChecked(playOnceItem, SharedConfig.repeatMode == PLAY_ONCE);
    }

    private boolean equals(float a, float b) {
        return Math.abs(a - b) < 0.05f;
    }

    private void updatePlaybackButton(boolean animated) {
        if (playbackSpeedButton == null) {
            return;
        }
        float currentPlaybackSpeed = MediaController.getInstance().getPlaybackSpeed(true);
        speedIcon.setValue(currentPlaybackSpeed, animated);
        speedSlider.setSpeed(currentPlaybackSpeed, animated);
        updateColors();

        boolean isFinal = !slidingSpeed;
        slidingSpeed = false;

        for (int a = 0; a < speedItems.length; a++) {
            if (isFinal && equals(currentPlaybackSpeed, speeds[a])) {
                speedItems[a].setColors(getThemedColor(Theme.key_featuredStickers_addButtonPressed), getThemedColor(Theme.key_featuredStickers_addButtonPressed));
            } else {
                speedItems[a].setColors(getThemedColor(Theme.key_actionBarDefaultSubmenuItem), getThemedColor(Theme.key_actionBarDefaultSubmenuItem));
            }
        }
    }

    public void updateColors() {
        if (playbackSpeedButton != null) {
            float currentPlaybackSpeed = MediaController.getInstance().getPlaybackSpeed(true);
            final int color = getThemedColor(!equals(currentPlaybackSpeed, 1.0f) ? Theme.key_featuredStickers_addButtonPressed : Theme.key_inappPlayerClose);
            if (speedIcon != null) {
                speedIcon.setColor(color);
            }
            if (Build.VERSION.SDK_INT >= 21) {
                playbackSpeedButton.setBackground(Theme.createSelectorDrawable(color & 0x19ffffff, 1, dp(14)));
            }
        }
        if (castItem != null) {
            castItem.setEnabledByColor(castItemButton != null && castItemButton.isConnected(), getThemedColor(Theme.key_actionBarDefaultSubmenuItem), getThemedColor(Theme.key_actionBarDefaultSubmenuItemIcon), getThemedColor(Theme.key_featuredStickers_addButton));
            castItem.setSelectorColor(castItemButton != null && castItemButton.isConnected() ? Theme.multAlpha(getThemedColor(Theme.key_featuredStickers_addButton), .10f) : getThemedColor(Theme.key_listSelector));
        }
        if (forwardButton != null) {
            forwardButton.setTextColor(getThemedColor(Theme.key_player_button));
        }
        if (backwardButton != null) {
            backwardButton.setTextColor(getThemedColor(Theme.key_player_button));
        }
    }

    private void onSubItemClick(int id) {
        final MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
        if (messageObject == null || parentActivity == null) return;

        if (id == 1) { // Поделиться
            boolean isSGStream = messageObject.messageOwner != null
                    && messageObject.messageOwner.message != null
                    && messageObject.messageOwner.message.startsWith("SG_STREAM|||");

            // Проверяем локальный файл
            File cacheFile = null;
            String attachPath = messageObject.messageOwner.attachPath;
            if (attachPath != null && !attachPath.isEmpty() && !attachPath.startsWith("http")) {
                cacheFile = new File(attachPath);
                if (!cacheFile.exists()) cacheFile = null;
            }
            if (cacheFile == null && !isSGStream) {
                cacheFile = FileLoader.getInstance(currentAccount).getPathToMessage(messageObject.messageOwner);
                if (cacheFile != null && !cacheFile.exists()) cacheFile = null;
            }

            if (cacheFile != null) {
                share(messageObject);
                dismiss();
                return;
            }

            // Файла нет — нужно скачать
            AlertDialog progressDialog = new AlertDialog(getContext(), 3);
            progressDialog.setMessage("Скачивание трека...");
            progressDialog.show();

            if (isSGStream) {
                // SG_STREAM: скачиваем через MusicWebSearcher
                String[] parts = messageObject.messageOwner.message.split("\\|\\|\\|");
                if (parts.length >= 4) {
                    String downloadUrl = parts[3];
                    long msgId = messageObject.getId();
                    File destFile = new File(
                            org.telegram.ui.ChannelMusicFragment.getMusicStorageDir(),
                            (msgId > 0 ? msgId : Math.abs((long) downloadUrl.hashCode())) + ".mp3");
                    new Thread(() -> {
                        boolean ok = MusicWebSearcher.downloadTrackToExactFileSync(downloadUrl, destFile);
                        AndroidUtilities.runOnUIThread(() -> {
                            progressDialog.dismiss();
                            if (ok) {
                                messageObject.messageOwner.attachPath = destFile.getAbsolutePath();
                                share(messageObject);
                            } else {
                                Toast.makeText(getContext(), "Ошибка скачивания", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();
                } else {
                    progressDialog.dismiss();
                }
            } else {
                // Обычный TG трек: через FileLoader
                int observerTag = DownloadController.getInstance(currentAccount).generateObserverTag();
                DownloadController.getInstance(currentAccount).addLoadingFileObserver(
                        messageObject.getFileName(), new DownloadController.FileDownloadProgressListener() {
                            @Override public void onFailedDownload(String fn, boolean canceled) {
                                AndroidUtilities.runOnUIThread(progressDialog::dismiss);
                                DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
                            }
                            @Override public void onSuccessDownload(String fn) {
                                AndroidUtilities.runOnUIThread(() -> { progressDialog.dismiss(); share(messageObject); });
                                DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
                            }
                            @Override public void onProgressDownload(String fn, long dl, long tot) {}
                            @Override public void onProgressUpload(String fn, long ul, long tot, boolean enc) {}
                            @Override public int getObserverTag() { return observerTag; }
                        });
                FileLoader.getInstance(currentAccount).loadFile(messageObject.getDocument(), messageObject, 1, 0);
            }
            dismiss();

        } else if (id == 5) { // В библиотеку
            // ИСПРАВЛЕНИЕ: читаем ID из SharedPreferences, потом из файла
            long targetId = MessagesController.getNotificationsSettings(currentAccount).getLong("music_channel_id", 0);
            if (targetId == 0) {
                try {
                    File dir = ApplicationLoader.applicationContext.getExternalFilesDir(null);
                    if (dir != null) {
                        File f = new File(dir, "music_channel_id.txt");
                        if (f.exists()) {
                            java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(f));
                            String line = r.readLine();
                            r.close();
                            if (line != null && !line.trim().isEmpty()) {
                                targetId = Long.parseLong(line.trim());
                                // Кэшируем в SharedPreferences
                                MessagesController.getNotificationsSettings(currentAccount).edit()
                                        .putLong("music_channel_id", targetId).apply();
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (targetId == 0) {
                Toast.makeText(getContext(), "Откройте раздел Музыка хотя бы раз", Toast.LENGTH_LONG).show();
                return;
            }
            boolean isSGStream = messageObject.messageOwner != null
                    && messageObject.messageOwner.message != null
                    && messageObject.messageOwner.message.startsWith("SG_STREAM|||");
            if (isSGStream) {
                SendMessagesHelper.getInstance(currentAccount).sendMessage(
                        messageObject.messageOwner.message, -targetId,
                        null, null, null, true, null, null, null, true, 0, 0, null, false);
            } else {
                ArrayList<MessageObject> fmessages = new ArrayList<>();
                fmessages.add(messageObject);
                SendMessagesHelper.getInstance(currentAccount).sendMessage(fmessages, -targetId, false, false, true, 0, 0);
            }
            Toast.makeText(getContext(), "Сохранено в библиотеку", Toast.LENGTH_SHORT).show();
            dismiss();

        } else if (id == 100) { // В плейлист
            showPlaylistSelectorFromDialog(messageObject);
        } else if (id == 7) { // В профиль
            saveToProfile(messageObject, true, () ->
                    Toast.makeText(getContext(), "Трек добавлен в профиль!", Toast.LENGTH_SHORT).show(), false);
            dismiss();
        }
    }
    private void showPlaylistSelectorFromDialog(MessageObject msg) {
        File dir = org.telegram.ui.ChannelMusicFragment.getPlaylistsDir();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));

        if (files == null || files.length == 0) {
            Toast.makeText(getContext(), "Плейлисты не найдены. Сначала создайте их в разделе Музыка.", Toast.LENGTH_LONG).show();
            return;
        }

        BottomSheet.Builder builder = new BottomSheet.Builder(getContext());
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Theme.getColor(Theme.key_dialogBackground));
        gd.setCornerRadii(new float[]{AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), 0, 0, 0, 0});
        container.setBackground(gd);
        container.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(16));

        TextView titleView = new TextView(getContext());
        titleView.setText("Добавить в плейлист");
        titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 18);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16));
        container.addView(titleView);

        View div = new View(getContext());
        div.setBackgroundColor(Theme.getColor(Theme.key_divider));
        container.addView(div, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 0, 0, 8));

        for (File pFile : files) {
            String pName = pFile.getName().replace(".txt", "");

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(14), AndroidUtilities.dp(20), AndroidUtilities.dp(14));
            row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 0));

            ImageView icon = new ImageView(getContext());
            icon.setImageResource(R.drawable.msg_add);
            icon.setColorFilter(Theme.getColor(Theme.key_dialogIcon), android.graphics.PorterDuff.Mode.SRC_IN);
            row.addView(icon, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL));

            TextView nameView = new TextView(getContext());
            nameView.setText(pName);
            nameView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            nameView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 16);
            nameView.setPadding(AndroidUtilities.dp(16), 0, 0, 0);
            row.addView(nameView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

            row.setOnClickListener(v -> {
                ArrayList<String> existingLines = new ArrayList<>();
                if (pFile.exists()) {
                    try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(pFile))) {
                        String line;
                        while ((line = br.readLine()) != null) existingLines.add(line);
                    } catch (Exception e) { e.printStackTrace(); }
                }

                try (java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.FileWriter(pFile, false))) {
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
                    Toast.makeText(getContext(), "Добавлено в \"" + pName + "\"", Toast.LENGTH_SHORT).show();
                } catch (Exception e) { e.printStackTrace(); }
                builder.getDismissRunnable().run();
            });
            container.addView(row);
        }

        builder.setCustomView(container);
        builder.create().show();
    }

    private void showAlbumCover(boolean show, boolean animated) {
        if (show) {
            if (blurredView.getVisibility() == View.VISIBLE || blurredAnimationInProgress) {
                return;
            }
            blurredView.setTag(1);
            bigAlbumConver.setImageBitmap(coverContainer.getImageReceiver().getBitmap());
            blurredAnimationInProgress = true;
            BaseFragment fragment = parentActivity.getActionBarLayout().getFragmentStack().get(parentActivity.getActionBarLayout().getFragmentStack().size() - 1);
            View fragmentView = fragment.getFragmentView();
            if (fragmentView != null) {
                int w = (int) (fragmentView.getMeasuredWidth() / 6.0f);
                int h = (int) (fragmentView.getMeasuredHeight() / 6.0f);
                Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.scale(1.0f / 6.0f, 1.0f / 6.0f);
                fragmentView.draw(canvas);
                canvas.translate(containerView.getLeft() - getLeftInset(), 0);
                containerView.draw(canvas);
                Utilities.stackBlurBitmap(bitmap, Math.max(7, Math.max(w, h) / 180));
                blurredView.setBackground(new BitmapDrawable(bitmap));
            }
            blurredView.setVisibility(View.VISIBLE);
            blurredView.animate().alpha(1.0f).setDuration(180).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    blurredAnimationInProgress = false;
                }
            }).start();
            bigAlbumConver.animate().scaleX(1f).scaleY(1f).setDuration(180).start();
        } else {
            if (blurredView.getVisibility() != View.VISIBLE) {
                return;
            }
            blurredView.setTag(null);
            if (animated) {
                blurredAnimationInProgress = true;
                blurredView.animate().alpha(0.0f).setDuration(180).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        blurredView.setVisibility(View.INVISIBLE);
                        bigAlbumConver.setImageBitmap(null);
                        blurredAnimationInProgress = false;
                    }
                }).start();
                bigAlbumConver.animate().scaleX(0.9f).scaleY(0.9f).setDuration(180).start();
            } else {
                blurredView.setAlpha(0.0f);
                blurredView.setVisibility(View.INVISIBLE);
                bigAlbumConver.setImageBitmap(null);
                bigAlbumConver.setScaleX(0.9f);
                bigAlbumConver.setScaleY(0.9f);
            }
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.messagePlayingDidStart || id == NotificationCenter.messagePlayingPlayStateChanged || id == NotificationCenter.messagePlayingDidReset) {
            updateTitle(id == NotificationCenter.messagePlayingDidReset && (Boolean) args[1]);
            if (id == NotificationCenter.messagePlayingDidReset || id == NotificationCenter.messagePlayingPlayStateChanged) {
                int count = listView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = listView.getChildAt(a);
                    if (view instanceof AudioPlayerCell) {
                        AudioPlayerCell cell = (AudioPlayerCell) view;
                        MessageObject messageObject = cell.getMessageObject();
                        if (messageObject != null && (messageObject.isVoice() || messageObject.isMusic())) {
                            cell.updateButtonState(false, true);
                        }
                    }
                }
                if (id == NotificationCenter.messagePlayingPlayStateChanged) {
                    if (MediaController.getInstance().getPlayingMessageObject() != null) {
                        if (MediaController.getInstance().isMessagePaused()) {
                            startForwardRewindingSeek();
                        } else if (rewindingState == 1 && rewindingProgress != -1f) {
                            AndroidUtilities.cancelRunOnUIThread(forwardSeek);
                            lastUpdateRewindingPlayerTime = 0;
                            forwardSeek.run();
                            rewindingProgress = -1f;
                        }
                    }
                }
            } else {
                MessageObject messageObject = (MessageObject) args[0];
                if (messageObject.eventId != 0) {
                    return;
                }
                int count = listView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View view = listView.getChildAt(a);
                    if (view instanceof AudioPlayerCell) {
                        AudioPlayerCell cell = (AudioPlayerCell) view;
                        MessageObject messageObject1 = cell.getMessageObject();
                        if (messageObject1 != null && (messageObject1.isVoice() || messageObject1.isMusic())) {
                            cell.updateButtonState(false, true);
                        }
                    }
                }
            }
            if (optionsIcon != null) {
                optionsIcon.setCasting(CastSync.isActive(), true);
            }
        } else if (id == NotificationCenter.messagePlayingProgressDidChanged) {
            MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
            if (messageObject != null && (messageObject.isMusic() || messageObject.isVoice())) {
                updateProgress(messageObject);
            }
        } else if (id == NotificationCenter.messagePlayingSpeedChanged) {
            updatePlaybackButton(true);
        } else if (id == NotificationCenter.musicDidLoad) {
            savedMusicList = MediaController.getInstance().currentSavedMusicList;
            playlist = MediaController.getInstance().getPlaylist();
            listAdapter.notifyDataSetChanged();
        } else if (id == NotificationCenter.moreMusicDidLoad) {
            savedMusicList = MediaController.getInstance().currentSavedMusicList;
            playlist = MediaController.getInstance().getPlaylist();
            listAdapter.notifyDataSetChanged();
            if (SharedConfig.playOrderReversed) {
                listView.stopScroll();
                int addedCount = (Integer) args[0];
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                int position = layoutManager.findLastVisibleItemPosition();
                if (position != RecyclerView.NO_POSITION) {
                    View firstVisView = layoutManager.findViewByPosition(position);
                    int offset = firstVisView == null ? 0 : firstVisView.getTop();
                    layoutManager.scrollToPositionWithOffset(position + addedCount, offset);
                }
            }
        } else if (id == NotificationCenter.fileLoaded) {
            String name = (String) args[0];
            if (name.equals(currentFile)) {
                updateTitle(false);
                currentAudioFinishedLoading = true;
            }
        } else if (id == NotificationCenter.fileLoadProgressChanged) {
            String name = (String) args[0];
            if (name.equals(currentFile)) {
                MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
                if (messageObject == null) {
                    return;
                }
                Long loadedSize = (Long) args[1];
                Long totalSize = (Long) args[2];
                float bufferedProgress;
                if (currentAudioFinishedLoading) {
                    bufferedProgress = 1.0f;
                } else {
                    long newTime = SystemClock.elapsedRealtime();
                    if (Math.abs(newTime - lastBufferedPositionCheck) >= 500) {
                        bufferedProgress = MediaController.getInstance().isStreamingCurrentAudio() ? FileLoader.getInstance(currentAccount).getBufferedProgressFromPosition(messageObject.audioProgress, currentFile) : 1.0f;
                        lastBufferedPositionCheck = newTime;
                    } else {
                        bufferedProgress = -1;
                    }
                }
                if (bufferedProgress != -1) {
                    seekBarBufferSpring.getSpring().setFinalPosition(bufferedProgress * 1000);
                    seekBarBufferSpring.start();
                }
            }
        } else if (id == NotificationCenter.musicIdsLoaded) {
            updateTitle(false);
        }
    }

    @Override
    protected boolean canDismissWithSwipe() {
        return false;
    }

    private void updateLayout() {
        if (listView == null || playerLayout == null) return;

        if (listView.getChildCount() <= 0) {
            listView.setTopGlowOffset(scrollOffsetY = listView.getPaddingTop());
            containerView.invalidate();
            return;
        }

        View child = listView.getChildAt(0);
        RecyclerListView.Holder holder = (RecyclerListView.Holder) listView.findContainingViewHolder(child);
        int top = child instanceof AudioPlayerCell ? child.getTop() : child.getBottom();
        int newOffset = dp(7);
        if (top >= dp(7) && holder != null && holder.getAdapterPosition() == 0) {
            newOffset = top;
        }

        boolean show = newOffset <= dp(12);
        if (show && actionBar.getTag() == null || !show && actionBar.getTag() != null) {
            actionBar.setTag(show ? 1 : null);
            if (actionBarAnimation != null) {
                actionBarAnimation.cancel();
                actionBarAnimation = null;
            }
            actionBarAnimation = new AnimatorSet();
            actionBarAnimation.setDuration(180);
            actionBarAnimation.playTogether(
                    ObjectAnimator.ofFloat(actionBar, View.ALPHA, show ? 1.0f : 0.0f),
                    ObjectAnimator.ofFloat(actionBarShadow, View.ALPHA, show ? 1.0f : 0.0f));
            actionBarAnimation.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationCancel(Animator animation) { actionBarAnimation = null; }
            });
            actionBarAnimation.start();
        }

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) listView.getLayoutParams();
        newOffset += layoutParams.topMargin - AndroidUtilities.statusBarHeight - dp(11);
        if (scrollOffsetY != newOffset) {
            listView.setTopGlowOffset((scrollOffsetY = newOffset) - layoutParams.topMargin - AndroidUtilities.statusBarHeight);
            containerView.invalidate();
        }

        // ИСПРАВЛЕНИЕ: Сшиваем плеер со списком
        // Плеер выталкивается вверх при скролле списка
        float yOffset = scrollOffsetY - playerLayout.getMeasuredHeight();
        if (yOffset > 0) yOffset = 0; // Не даем плееру оторваться вниз при оттяжке
        playerLayout.setTranslationY(yOffset);
        if (playerShadow != null) {
            playerShadow.setTranslationY(yOffset + playerLayout.getMeasuredHeight());
        }

        int offset = dp(13);
        top = scrollOffsetY - backgroundPaddingTop - offset;
        float rad = 1.0f;
        if (top + backgroundPaddingTop < ActionBar.getCurrentActionBarHeight()) {
            float toMove = offset + dp(11 - 7);
            float moveProgress = Math.min(1.0f, (ActionBar.getCurrentActionBarHeight() - top - backgroundPaddingTop) / toMove);
            rad = 1.0f - moveProgress;
        }

        boolean light = rad <= 0.5f && ColorUtils.calculateLuminance(getThemedColor(Theme.key_dialogBackground)) > 0.7f;
        if (light != wasLight) {
            AndroidUtilities.setLightStatusBar(getWindow(), wasLight = light);
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidStart);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingProgressDidChanged);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoaded);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoadProgressChanged);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.musicDidLoad);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.moreMusicDidLoad);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.musicIdsLoaded);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.messagePlayingSpeedChanged);
        DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
    }

    @Override
    public void onBackPressed() {
        if (actionBar != null && actionBar.isSearchFieldVisible()) {
            actionBar.closeSearchField();
            return;
        }
        if (blurredView.getTag() != null) {
            showAlbumCover(false, true);
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onFailedDownload(String fileName, boolean canceled) {

    }

    @Override
    public void onSuccessDownload(String fileName) {

    }

    @Override
    public void onProgressDownload(String fileName, long downloadedSize, long totalSize) {
        progressView.setProgress(Math.min(1f, downloadedSize / (float) totalSize), true);
    }

    @Override
    public void onProgressUpload(String fileName, long uploadedSize, long totalSize, boolean isEncrypted) {

    }

    @Override
    public int getObserverTag() {
        return TAG;
    }

    private void updateRepeatButton() {
        int mode = SharedConfig.repeatMode;
        if (mode == 0 || mode == 1) {
            if (SharedConfig.shuffleMusic) {
                if (mode == 0) {
                    repeatButton.setIcon(R.drawable.player_new_shuffle);
                } else {
                    repeatButton.setIcon(R.drawable.player_new_repeat_shuffle);
                }
            } else if (SharedConfig.playOrderReversed) {
                if (mode == 0) {
                    repeatButton.setIcon(R.drawable.player_new_order);
                } else {
                    repeatButton.setIcon(R.drawable.player_new_repeat_reverse);
                }
            } else {
                repeatButton.setIcon(R.drawable.player_new_repeatall);
            }
            if (mode == 0 && !SharedConfig.shuffleMusic && !SharedConfig.playOrderReversed) {
                repeatButton.setTag(Theme.key_player_button);
                repeatButton.setIconColor(getThemedColor(Theme.key_player_button));
                Theme.setSelectorDrawableColor(repeatButton.getBackground(), getThemedColor(Theme.key_listSelector), true);
                repeatButton.setContentDescription(LocaleController.getString(R.string.AccDescrRepeatOff));
            } else {
                repeatButton.setTag(Theme.key_player_buttonActive);
                repeatButton.setIconColor(getThemedColor(Theme.key_player_buttonActive));
                Theme.setSelectorDrawableColor(repeatButton.getBackground(), getThemedColor(Theme.key_player_buttonActive) & 0x19ffffff, true);
                if (mode == 0) {
                    if (SharedConfig.shuffleMusic) {
                        repeatButton.setContentDescription(LocaleController.getString(R.string.ShuffleList));
                    } else {
                        repeatButton.setContentDescription(LocaleController.getString(R.string.ReverseOrder));
                    }
                } else {
                    repeatButton.setContentDescription(LocaleController.getString(R.string.AccDescrRepeatList));
                }
            }
        } else if (mode == 2) {
            repeatButton.setIcon(R.drawable.player_new_repeatone);
            repeatButton.setTag(Theme.key_player_buttonActive);
            repeatButton.setIconColor(getThemedColor(Theme.key_player_buttonActive));
            Theme.setSelectorDrawableColor(repeatButton.getBackground(), getThemedColor(Theme.key_player_buttonActive) & 0x19ffffff, true);
            repeatButton.setContentDescription(LocaleController.getString(R.string.AccDescrRepeatOne));
        } else if (mode == PLAY_ONCE) {
            repeatButton.setIcon(R.drawable.photo_timer);
            repeatButton.setTag(Theme.key_player_buttonActive);
            repeatButton.setIconColor(getThemedColor(Theme.key_player_buttonActive));
            Theme.setSelectorDrawableColor(repeatButton.getBackground(), getThemedColor(Theme.key_player_buttonActive) & 0x19ffffff, true);
            repeatButton.setContentDescription(LocaleController.getString(R.string.PlayOnce));
        }
    }

    private void updateProgress(MessageObject messageObject) {
        updateProgress(messageObject, false);
    }

    private void updateProgress(MessageObject messageObject, boolean animated) {
        if (seekBarView != null) {
            int newTime;
            if (seekBarView.isDragging()) {
                newTime = (int) (messageObject.getDuration() * seekBarView.getProgress());
            } else {
                boolean updateRewinding = rewindingProgress >= 0 && (rewindingState == -1 || (rewindingState == 1 && MediaController.getInstance().isMessagePaused()));
                if (updateRewinding) {
                    seekBarView.setProgress(rewindingProgress, animated);
                } else {
                    seekBarView.setProgress(messageObject.audioProgress, animated);
                }

                float bufferedProgress;
                if (currentAudioFinishedLoading) {
                    bufferedProgress = 1.0f;
                } else {
                    long time = SystemClock.elapsedRealtime();
                    if (Math.abs(time - lastBufferedPositionCheck) >= 500) {
                        bufferedProgress = MediaController.getInstance().isStreamingCurrentAudio() ? FileLoader.getInstance(currentAccount).getBufferedProgressFromPosition(messageObject.audioProgress, currentFile) : 1.0f;
                        lastBufferedPositionCheck = time;
                    } else {
                        bufferedProgress = -1;
                    }
                }
                if (bufferedProgress != -1) {
                    seekBarBufferSpring.getSpring().setFinalPosition(bufferedProgress * 1000);
                    seekBarBufferSpring.start();
                }
                if (updateRewinding) {
                    newTime = (int) (messageObject.getDuration() * seekBarView.getProgress());
                    messageObject.audioProgressSec = newTime;
                } else {
                    newTime = messageObject.audioProgressSec;
                }
            }
            if (lastTime != newTime) {
                lastTime = newTime;
                timeTextView.setText(AndroidUtilities.formatShortDuration(newTime));
            }
            seekBarView.updateTimestamps(messageObject, null);
        }

        MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
        if (isLyricsVisible && !lyricsLines.isEmpty() && playing != null) {
            // ИСПРАВЛЕНИЕ: Берем точное время в миллисекундах из плеера для идеальной синхронизации
            long currentMs = MediaController.getInstance().getProgressMs(playing);
            if (currentMs < 0) {
                if (playing.audioProgressSec > 0) currentMs = playing.audioProgressSec * 1000L;
                else currentMs = (long) (playing.audioProgress * playing.getDuration() * 1000L);
            }
            updateHighlight(currentMs);
        }
    }

    private void smoothScrollToCenter(int position) {
        if (lyricsListView.getLayoutManager() == null) return;
        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
            @Override
            public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
                // Идеально центрируем элемент
                return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
            }
            @Override
            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                // Плавная скорость как в Яндекс.Музыке
                return 150f / displayMetrics.densityDpi;
            }
        };
        smoothScroller.setTargetPosition(position);
        lyricsListView.getLayoutManager().startSmoothScroll(smoothScroller);
    }


    private void updateHighlight(long currentMs) {

        int newActiveIndex = -1;
        for (int i = lyricsLines.size() - 1; i >= 0; i--) {
            if (currentMs >= lyricsLines.get(i).timeMs) {
                newActiveIndex = i;
                break;
            }
        }
        if (newActiveIndex != currentLyricIndex) {
            currentLyricIndex = newActiveIndex;
            lyricsAdapter.notifyDataSetChanged();
            if (currentLyricIndex != -1) {
                smoothScrollToCenter(currentLyricIndex);
            }
        }
    }

    private void checkIfMusicDownloaded(MessageObject messageObject) {
        File cacheFile = null;
        if (messageObject.messageOwner.attachPath != null && messageObject.messageOwner.attachPath.length() > 0) {
            cacheFile = new File(messageObject.messageOwner.attachPath);
            if (!cacheFile.exists()) {
                cacheFile = null;
            }
        }
        if (cacheFile == null) {
            cacheFile = FileLoader.getInstance(currentAccount).getPathToMessage(messageObject.messageOwner);
        }

        // ИСПРАВЛЕНИЕ: Разрешаем проигрывать и контроллировать стримы напрямую
        boolean isDirectStream = messageObject.messageOwner.attachPath != null &&
                (messageObject.messageOwner.attachPath.startsWith("http://") ||
                        messageObject.messageOwner.attachPath.startsWith("https://"));

        boolean canStream = SharedConfig.streamMedia && ((int) messageObject.getDialogId() != 0 || isDirectStream) && messageObject.isMusic();

        if (!cacheFile.exists() && !canStream && !isDirectStream) {
            String fileName = messageObject.getFileName();
            DownloadController.getInstance(currentAccount).addLoadingFileObserver(fileName, this);
            Float progress = ImageLoader.getInstance().getFileProgress(fileName);
            progressView.setProgress(progress != null ? progress : 0, false);
            progressView.setVisibility(View.VISIBLE);
            seekBarView.setVisibility(View.INVISIBLE);
            playButton.setEnabled(false);
        } else {
            DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
            progressView.setVisibility(View.INVISIBLE);
            seekBarView.setVisibility(View.VISIBLE);
            playButton.setEnabled(true);
        }
    }

    private void updateTitle(boolean shutdown) {
        MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
        if (messageObject == null && shutdown || messageObject != null && !(messageObject.isMusic() || messageObject.isVoice())) {
            dismiss();
        } else {
            if (messageObject == null) {
                lastMessageObject = null;
                return;
            }
            final boolean sameMessageObject = messageObject == lastMessageObject;
            lastMessageObject = messageObject;
            if (messageObject.eventId != 0 || messageObject.getId() <= -2000000000) {
                optionsButton.setVisibility(View.INVISIBLE);
            } else {
                optionsButton.setVisibility(View.VISIBLE);
            }
            final long dialogId = messageObject.getDialogId();
            final long docId = messageObject.getDocument() != null ? messageObject.getDocument().id : 0L;
            final boolean noforwards = (
                    dialogId < 0 && MessagesController.getInstance(currentAccount).isPeerNoForwards(dialogId) ||
                            MessagesController.getInstance(currentAccount).isPeerNoForwards(messageObject.getDialogId()) ||
                            messageObject.messageOwner.noforwards
            );

            // ИСПРАВЛЕНИЕ: Удален код, который плющил высоту плеера обратно до стандартных Telegram размеров
            if (noforwards != this.noforwards) {
                this.noforwards = noforwards;
            }

            if (noforwards) {
                optionsButton.hideSubItem(1);
                optionsButton.hideSubItem(100);
                optionsButton.setAdditionalYOffset(-dp(16 + 48 * 3));
            } else {
                optionsButton.showSubItem(1);
                optionsButton.showSubItem(100);
                optionsButton.showSubItem(2);
                optionsButton.showSubItem(5);
                optionsButton.setAdditionalYOffset(-dp(157 + 40 + 48));
            }
            optionsButton.setSubItemShown(4, messageObject.getId() > 0);
            optionsButton.setSubItemShown(7, isMyList());

            checkIfMusicDownloaded(messageObject);
            updateProgress(messageObject, !sameMessageObject);
            updateCover(messageObject, !sameMessageObject);

            if (MediaController.getInstance().isMessagePaused()) {
                playPauseDrawable.setPause(false);
                playButton.setContentDescription(LocaleController.getString(R.string.AccActionPlay));
            } else {
                playPauseDrawable.setPause(true);
                playButton.setContentDescription(LocaleController.getString(R.string.AccActionPause));
            }
            String title = messageObject.getMusicTitle();
            String author = messageObject.getMusicAuthor();
            titleTextView.setText(title);
            authorTextView.setText(author);
            // ФИКС ТЕКСТА: Запрашиваем новый текст, если панель открыта
            if (isLyricsVisible && messageObject != null) {
                fetchLyrics(messageObject);
            }
            // Обновляем chip источника при каждой смене трека
            if (sourceChipTextView != null) {
                sourceChipTextView.setText(getSourceDisplayName());
            }

            final MessagesController.SavedMusicIds musicIds = MessagesController.getInstance(currentAccount).getSavedMusicIds();
            saveToProfileButton.setLoading(musicIds.loading);
            setVisibleInProfile(musicIds.ids.contains(docId));

            int duration = lastDuration = (int) messageObject.getDuration();

            if (durationTextView != null) {
                durationTextView.setText(duration != 0 ? AndroidUtilities.formatShortDuration(duration) : "-:--");
            }

            if (duration > 60 * 10) {
                playbackSpeedButton.setVisibility(View.VISIBLE);
            } else {
                playbackSpeedButton.setVisibility(View.GONE);
            }

            if (!sameMessageObject) {
                preloadNeighboringThumbs();
            }
        }
    }


    private void updateCover(MessageObject messageObject, boolean animated) {
        final BackupImageView imageView = animated ? coverContainer.getNextImageView() : coverContainer.getImageView();
        final AudioInfo audioInfo = MediaController.getInstance().getAudioInfo();
        if (animated) {
            coverContainer.switchImageViews();
        }
        if (audioInfo != null && audioInfo.getCover() != null) {
            imageView.setImageBitmap(audioInfo.getCover());
            currentFile = null;
            currentAudioFinishedLoading = true;
        } else {
            TLRPC.Document document = messageObject.getDocument();
            currentFile = FileLoader.getAttachFileName(document);
            currentAudioFinishedLoading = false;
            String artworkUrl = messageObject.getArtworkUrl(false);
            final ImageLocation thumbImageLocation = getArtworkThumbImageLocation(messageObject);
            if (!TextUtils.isEmpty(artworkUrl)) {
                imageView.setImage(ImageLocation.getForPath(artworkUrl), null, thumbImageLocation, null, null, 0, 1, messageObject);
            } else if (thumbImageLocation != null) {
                imageView.setImage(null, null, thumbImageLocation, null, null, 0, 1, messageObject);
            } else {
                imageView.setImageDrawable(null);
            }
            imageView.invalidate();
        }
    }

    private ImageLocation getArtworkThumbImageLocation(MessageObject messageObject) {
        final TLRPC.Document document = messageObject.getDocument();
        TLRPC.PhotoSize thumb = document != null ? FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 360) : null;
        if (!(thumb instanceof TLRPC.TL_photoSize) && !(thumb instanceof TLRPC.TL_photoSizeProgressive)) {
            thumb = null;
        }
        if (thumb != null) {
            return ImageLocation.getForDocument(thumb, document);
        }
        final String smallArtworkUrl = messageObject.getArtworkUrl(true);
        if (smallArtworkUrl != null) {
            return ImageLocation.getForPath(smallArtworkUrl);
        }
        return null;
    }

    private void preloadNeighboringThumbs() {
        final MediaController mediaController = MediaController.getInstance();
        final List<MessageObject> playlist = mediaController.getPlaylist();
        if (playlist.size() <= 1) {
            return;
        }

        final List<MessageObject> neighboringItems = new ArrayList<>();
        final int playingIndex = mediaController.getPlayingMessageObjectNum();

        int nextIndex = playingIndex + 1;
        int prevIndex = playingIndex - 1;
        if (nextIndex >= playlist.size()) {
            nextIndex = 0;
        }
        if (nextIndex <= -1) {
            nextIndex = playlist.size() - 1;
        }
        if (prevIndex <= -1) {
            prevIndex = playlist.size() - 1;
        }
        if (prevIndex >= playlist.size()) {
            prevIndex = 0;
        }

        neighboringItems.add(playlist.get(nextIndex));
        if (nextIndex != prevIndex) {
            neighboringItems.add(playlist.get(prevIndex));
        }

        for (int i = 0, N = neighboringItems.size(); i < N; i++) {
            final MessageObject messageObject = neighboringItems.get(i);
            final ImageLocation thumbImageLocation = getArtworkThumbImageLocation(messageObject);
            if (thumbImageLocation != null) {
                if (thumbImageLocation.path != null) {
                    ImageLoader.getInstance().preloadArtwork(thumbImageLocation.path);
                } else {
                    FileLoader.getInstance(currentAccount).loadFile(thumbImageLocation, messageObject, null, FileLoader.PRIORITY_LOW, 1);
                }
            }
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context context;
        private ArrayList<MessageObject> searchResult = new ArrayList<>();
        private Runnable searchRunnable;

        public ListAdapter(Context context) {
            this.context = context;
        }

        private boolean listViewIsVisible;
        public void setup() {
            // Список треков всегда скрыт — плеер на весь экран
            listViewIsVisible = false;
            listView.setVisibility(View.GONE);
            listView.setTranslationY(AndroidUtilities.displaySize.y);
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            // Список треков всегда скрыт
            listViewIsVisible = false;
            listView.setVisibility(View.GONE);
            if (playlist.size() > 1) {
                playerLayout.setBackground(SoundGramStyleManager.buildGlassDrawable(
                        SoundGramStyleSettingsFragment.KEY_GLASS_PLAYER,
                        SoundGramStyleSettingsFragment.KEY_ALPHA_PLAYER,
                        SoundGramStyleSettingsFragment.KEY_COLOR_BG_PLAYER,
                        SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_PLAYER,
                        28f));
                playerShadow.setVisibility(View.VISIBLE);
                listView.setPadding(0, listView.getPaddingTop(), 0, dp(179 + 52));
            } else {
                playerLayout.setBackground(SoundGramStyleManager.buildGlassDrawable(
                        SoundGramStyleSettingsFragment.KEY_GLASS_PLAYER,
                        SoundGramStyleSettingsFragment.KEY_ALPHA_PLAYER,
                        SoundGramStyleSettingsFragment.KEY_COLOR_BG_PLAYER,
                        SoundGramStyleSettingsFragment.KEY_COLOR_STROKE_PLAYER,
                        28f));
                playerShadow.setVisibility(View.VISIBLE);
                listView.setPadding(0, listView.getPaddingTop(), 0, 0);
            }
            updateEmptyView();
        }

        @Override
        public int getItemCount() {
            if (searchWas) {
                return searchResult.size();
            }
            return playlist.size() > 1 ? playlist.size() : 0;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == 1) {
                View paddingView = new View(context) {
                    @Override
                    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                        super.onMeasure(
                                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                                MeasureSpec.makeMeasureSpec(dp(300), MeasureSpec.EXACTLY)
                        );
                    }
                };
                return new RecyclerListView.Holder(paddingView);
            } else {
                View view = new AudioPlayerCell(context, MediaController.getInstance().currentPlaylistIsGlobalSearch() ? AudioPlayerCell.VIEW_TYPE_GLOBAL_SEARCH : AudioPlayerCell.VIEW_TYPE_DEFAULT, resourcesProvider);
                return new RecyclerListView.Holder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (padWithItem) {
                if (position == 0) {
                    final View paddingView = holder.itemView;
                    return;
                }
                position--;
            }
            final AudioPlayerCell cell = (AudioPlayerCell) holder.itemView;
            final MessageObject messageObject;
            final boolean needDivider;
            final View.OnTouchListener onReorderTouch;
            if (searchWas) {
                messageObject = searchResult.get(position);
                needDivider = position + 1 < searchResult.size();
            } else if (savedMusicList != null ? !SharedConfig.playOrderReversed : SharedConfig.playOrderReversed) {
                messageObject = playlist.get(position);
                needDivider = position + 1 < playlist.size();
            } else {
                messageObject = playlist.get(playlist.size() - position - 1);
                needDivider = (playlist.size() - position - 2) >= 0;
            }
            if (isMyList()) {
                onReorderTouch = (v, e) -> {
                    if (e.getAction() == MotionEvent.ACTION_DOWN) {
                        itemTouchHelper.startDrag(listView.getChildViewHolder(cell));
                    }
                    return false;
                };
            } else {
                onReorderTouch = null;
            }
            cell.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground, resourcesProvider));
            cell.setMessageObject(messageObject, isMyList(), isMyList() || noforwards || messageObject.getId() <= 0 ? null : btn -> showOptions(cell, messageObject), needDivider, onReorderTouch);
        }

        @Override
        public int getItemViewType(int i) {
            if (padWithItem && i == 0) {
                return 1;
            }
            return 0;
        }

        public void search(final String query) {
            if (searchRunnable != null) {
                Utilities.searchQueue.cancelRunnable(searchRunnable);
                searchRunnable = null;
            }
            if (query == null) {
                searchResult.clear();
                notifyDataSetChanged();
            } else {
                Utilities.searchQueue.postRunnable(searchRunnable = () -> {
                    searchRunnable = null;
                    processSearch(query);
                }, 300);
            }
        }

        private void processSearch(final String query) {
            AndroidUtilities.runOnUIThread(() -> {
                final ArrayList<MessageObject> copy = new ArrayList<>(playlist);
                Utilities.searchQueue.postRunnable(() -> {
                    String search1 = query.trim().toLowerCase();
                    if (search1.length() == 0) {
                        updateSearchResults(new ArrayList<>(), query);
                        return;
                    }
                    String search2 = LocaleController.getInstance().getTranslitString(search1);
                    if (search1.equals(search2) || search2.length() == 0) {
                        search2 = null;
                    }
                    String[] search = new String[1 + (search2 != null ? 1 : 0)];
                    search[0] = search1;
                    if (search2 != null) {
                        search[1] = search2;
                    }

                    ArrayList<MessageObject> resultArray = new ArrayList<>();

                    for (int a = 0; a < copy.size(); a++) {
                        MessageObject messageObject = copy.get(a);
                        for (int b = 0; b < search.length; b++) {
                            String q = search[b];
                            String name = messageObject.getDocumentName();
                            if (name == null || name.length() == 0) {
                                continue;
                            }
                            name = name.toLowerCase();
                            if (name.contains(q)) {
                                resultArray.add(messageObject);
                                break;
                            }
                            TLRPC.Document document;
                            if (messageObject.type == MessageObject.TYPE_TEXT) {
                                document = messageObject.messageOwner.media.webpage.document;
                            } else {
                                document = messageObject.messageOwner.media.document;
                            }
                            boolean ok = false;
                            for (int c = 0; c < document.attributes.size(); c++) {
                                TLRPC.DocumentAttribute attribute = document.attributes.get(c);
                                if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                                    if (attribute.performer != null) {
                                        ok = attribute.performer.toLowerCase().contains(q);
                                    }
                                    if (!ok && attribute.title != null) {
                                        ok = attribute.title.toLowerCase().contains(q);
                                    }
                                    break;
                                }
                            }
                            if (ok) {
                                resultArray.add(messageObject);
                                break;
                            }
                        }
                    }

                    updateSearchResults(resultArray, query);
                });
            });
        }

        private void updateSearchResults(final ArrayList<MessageObject> documents, String query) {
            AndroidUtilities.runOnUIThread(() -> {
                if (!searching) {
                    return;
                }
                searchWas = true;
                searchResult = documents;
                notifyDataSetChanged();
                layoutManager.scrollToPosition(0);
                emptySubtitleTextView.setText(AndroidUtilities.replaceTags(LocaleController.formatString("NoAudioFoundPlayerInfo", R.string.NoAudioFoundPlayerInfo, query)));
            });
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        ThemeDescription.ThemeDescriptionDelegate delegate = () -> {
            EditTextBoldCursor editText = searchItem.getSearchField();
            editText.setCursorColor(getThemedColor(Theme.key_player_actionBarTitle));

            repeatButton.setIconColor(getThemedColor((Integer) repeatButton.getTag()));
            Theme.setSelectorDrawableColor(repeatButton.getBackground(), getThemedColor(Theme.key_listSelector), true);

            optionsButton.setIconColor(getThemedColor(Theme.key_player_button));
            Theme.setSelectorDrawableColor(optionsButton.getBackground(), getThemedColor(Theme.key_listSelector), true);

            if (forwardButton != null) {
                forwardButton.setTextColor(getThemedColor(Theme.key_player_button));
                if (Build.VERSION.SDK_INT >= 21) {
                    forwardButton.setBackgroundDrawable(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(24)));
                }
            }
            if (backwardButton != null) {
                backwardButton.setTextColor(getThemedColor(Theme.key_player_button));
                if (Build.VERSION.SDK_INT >= 21) {
                    backwardButton.setBackgroundDrawable(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(24)));
                }
            }

            progressView.setBackgroundColor(getThemedColor(Theme.key_player_progressBackground));
            progressView.setProgressColor(getThemedColor(Theme.key_player_progress));

            updateSubMenu();
            repeatButton.redrawPopup(getThemedColor(Theme.key_actionBarDefaultSubmenuBackground));

            optionsButton.setPopupItemsColor(getThemedColor(Theme.key_actionBarDefaultSubmenuItem), false);
            optionsButton.setPopupItemsColor(getThemedColor(Theme.key_actionBarDefaultSubmenuItem), true);
            optionsButton.redrawPopup(getThemedColor(Theme.key_actionBarDefaultSubmenuBackground));
        };

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_dialogBackground));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, delegate, Theme.key_player_actionBarTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_player_actionBarTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBTITLECOLOR, null, null, null, null, Theme.key_player_actionBarTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_player_actionBarSelector));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCH, null, null, null, null, Theme.key_player_actionBarTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCHPLACEHOLDER, null, null, null, null, Theme.key_player_time));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{AudioPlayerCell.class}, null, null, null, Theme.key_chat_inLoader));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{AudioPlayerCell.class}, null, null, null, Theme.key_chat_outLoader));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{AudioPlayerCell.class}, null, null, null, Theme.key_chat_inLoaderSelected));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{AudioPlayerCell.class}, null, null, null, Theme.key_chat_inMediaIcon));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{AudioPlayerCell.class}, null, null, null, Theme.key_chat_inMediaIconSelected));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{AudioPlayerCell.class}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{AudioPlayerCell.class}, null, null, null, Theme.key_chat_inAudioSelectedProgress));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{AudioPlayerCell.class}, null, null, null, Theme.key_chat_inAudioProgress));

        themeDescriptions.add(new ThemeDescription(containerView, 0, null, null, new Drawable[]{shadowDrawable}, null, Theme.key_dialogBackground));

        themeDescriptions.add(new ThemeDescription(progressView, 0, null, null, null, null, Theme.key_player_progressBackground));
        themeDescriptions.add(new ThemeDescription(progressView, 0, null, null, null, null, Theme.key_player_progress));
        themeDescriptions.add(new ThemeDescription(seekBarView, 0, null, null, null, null, Theme.key_player_progressBackground));
        themeDescriptions.add(new ThemeDescription(seekBarView, 0, null, null, null, null, Theme.key_player_progressCachedBackground));
        themeDescriptions.add(new ThemeDescription(seekBarView, ThemeDescription.FLAG_PROGRESSBAR, null, null, null, null, Theme.key_player_progress));

        themeDescriptions.add(new ThemeDescription(playbackSpeedButton, ThemeDescription.FLAG_CHECKTAG | ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_inappPlayerPlayPause));
        themeDescriptions.add(new ThemeDescription(playbackSpeedButton, ThemeDescription.FLAG_CHECKTAG | ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_inappPlayerClose));

        themeDescriptions.add(new ThemeDescription(repeatButton, 0, null, null, null, delegate, Theme.key_player_button));
        themeDescriptions.add(new ThemeDescription(repeatButton, 0, null, null, null, delegate, Theme.key_player_buttonActive));
        themeDescriptions.add(new ThemeDescription(repeatButton, 0, null, null, null, delegate, Theme.key_listSelector));
        themeDescriptions.add(new ThemeDescription(repeatButton, 0, null, null, null, delegate, Theme.key_actionBarDefaultSubmenuItem));
        themeDescriptions.add(new ThemeDescription(repeatButton, 0, null, null, null, delegate, Theme.key_actionBarDefaultSubmenuBackground));
        themeDescriptions.add(new ThemeDescription(optionsButton, 0, null, null, null, delegate, Theme.key_player_button));
        themeDescriptions.add(new ThemeDescription(optionsButton, 0, null, null, null, delegate, Theme.key_listSelector));
        themeDescriptions.add(new ThemeDescription(optionsButton, 0, null, null, null, delegate, Theme.key_actionBarDefaultSubmenuItem));
        themeDescriptions.add(new ThemeDescription(optionsButton, 0, null, null, null, delegate, Theme.key_actionBarDefaultSubmenuBackground));

        themeDescriptions.add(new ThemeDescription(prevButton, 0, null, new RLottieDrawable[]{prevButton.getAnimatedDrawable()}, "Triangle 3", Theme.key_player_button));
        themeDescriptions.add(new ThemeDescription(prevButton, 0, null, new RLottieDrawable[]{prevButton.getAnimatedDrawable()}, "Triangle 4", Theme.key_player_button));
        themeDescriptions.add(new ThemeDescription(prevButton, 0, null, new RLottieDrawable[]{prevButton.getAnimatedDrawable()}, "Rectangle 4", Theme.key_player_button));
        themeDescriptions.add(new ThemeDescription(prevButton, ThemeDescription.FLAG_IMAGECOLOR | ThemeDescription.FLAG_USEBACKGROUNDDRAWABLE, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(playButton, ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_player_button));
        themeDescriptions.add(new ThemeDescription(playButton, ThemeDescription.FLAG_IMAGECOLOR | ThemeDescription.FLAG_USEBACKGROUNDDRAWABLE, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(nextButton, 0, null, new RLottieDrawable[]{nextButton.getAnimatedDrawable()}, "Triangle 3", Theme.key_player_button));
        themeDescriptions.add(new ThemeDescription(nextButton, 0, null, new RLottieDrawable[]{nextButton.getAnimatedDrawable()}, "Triangle 4", Theme.key_player_button));
        themeDescriptions.add(new ThemeDescription(nextButton, 0, null, new RLottieDrawable[]{nextButton.getAnimatedDrawable()}, "Rectangle 4", Theme.key_player_button));
        themeDescriptions.add(new ThemeDescription(nextButton, ThemeDescription.FLAG_IMAGECOLOR | ThemeDescription.FLAG_USEBACKGROUNDDRAWABLE, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(forwardButton, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, delegate, Theme.key_player_button));
        themeDescriptions.add(new ThemeDescription(forwardButton, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, delegate, Theme.key_listSelector));
        themeDescriptions.add(new ThemeDescription(backwardButton, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, delegate, Theme.key_player_button));
        themeDescriptions.add(new ThemeDescription(backwardButton, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, delegate, Theme.key_listSelector));


        themeDescriptions.add(new ThemeDescription(playerLayout, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_player_background));

        themeDescriptions.add(new ThemeDescription(playerShadow, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_dialogShadowLine));

        themeDescriptions.add(new ThemeDescription(emptyImageView, ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_dialogEmptyImage));
        themeDescriptions.add(new ThemeDescription(emptyTitleTextView, ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_dialogEmptyText));
        themeDescriptions.add(new ThemeDescription(emptySubtitleTextView, ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_dialogEmptyText));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_dialogScrollGlow));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(progressView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_emptyListPlaceholder));
        themeDescriptions.add(new ThemeDescription(progressView, ThemeDescription.FLAG_PROGRESSBAR, null, null, null, null, Theme.key_progressCircle));

        themeDescriptions.add(new ThemeDescription(durationTextView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_player_time));
        themeDescriptions.add(new ThemeDescription(timeTextView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_player_time));
        themeDescriptions.add(new ThemeDescription(titleTextView.getTextView(), ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_player_actionBarTitle));
        themeDescriptions.add(new ThemeDescription(titleTextView.getNextTextView(), ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_player_actionBarTitle));
        themeDescriptions.add(new ThemeDescription(authorTextView.getTextView(), ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_player_time));
        themeDescriptions.add(new ThemeDescription(authorTextView.getNextTextView(), ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_player_time));

        themeDescriptions.add(new ThemeDescription(containerView, 0, null, null, null, null, Theme.key_sheet_scrollUp));

        return themeDescriptions;
    }

    private void saveToProfile(MessageObject messageObject, boolean save, Runnable done, boolean triedFileRef) {
        final TLRPC.Document document = messageObject.getDocument();
        if (document == null) {
            return;
        }
        final long documentId = document.id;
        final TLRPC.TL_account_saveMusic req = new TLRPC.TL_account_saveMusic();
        req.unsave = !save;
        req.id = new TLRPC.TL_inputDocument();
        req.id.id = documentId;
        req.id.access_hash = document.access_hash;
        req.id.file_reference = document.file_reference;
        if (req.id.file_reference == null) {
            req.id.file_reference = new byte[0];
        }
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> {
            if (err != null && FileRefController.isFileRefError(err.text)) {
                if (triedFileRef || messageObject.getId() < 0) {
                    AndroidUtilities.runOnUIThread(() -> {
                        BulletinFactory.of((FrameLayout) containerView, resourcesProvider)
                                .showForError(err);
                    });
                    return;
                }
                if (messageObject.getDialogId() >= 0) {
                    final int msg_id = messageObject.getId();
                    final TLRPC.TL_messages_getMessages fileRefReq = new TLRPC.TL_messages_getMessages();
                    fileRefReq.id.add(msg_id);
                    ConnectionsManager.getInstance(currentAccount).sendRequest(fileRefReq, (res1, err1) -> {
                        if (res1 instanceof TLRPC.messages_Messages) {
                            final TLRPC.messages_Messages r = (TLRPC.messages_Messages) res1;
                            TLRPC.Message message = null;
                            for (int i = 0; i < r.messages.size(); ++i) {
                                if (r.messages.get(i).id == msg_id) {
                                    message = r.messages.get(i);
                                    break;
                                }
                            }
                            if (message != null) {
                                saveToProfile(new MessageObject(currentAccount, message, false, true), save, done, true);
                            } else {
                                AndroidUtilities.runOnUIThread(() -> {
                                    BulletinFactory.of((FrameLayout) containerView, resourcesProvider)
                                            .createErrorBulletin(LocaleController.formatString(R.string.UnknownErrorCode, "CLIENT_MESSAGE_NOT_FOUND"))
                                            .show();
                                });
                            }
                        } else if (err1 != null) {
                            AndroidUtilities.runOnUIThread(() -> {
                                BulletinFactory.of((FrameLayout) containerView, resourcesProvider)
                                        .showForError(err1);
                            });
                        }
                    });
                } else {
                    final int msg_id = messageObject.getId();
                    final TLRPC.TL_channels_getMessages fileRefReq = new TLRPC.TL_channels_getMessages();
                    fileRefReq.channel = MessagesController.getInstance(currentAccount).getInputChannel(-messageObject.getDialogId());
                    fileRefReq.id.add(msg_id);
                    ConnectionsManager.getInstance(currentAccount).sendRequest(fileRefReq, (res1, err1) -> {
                        if (res1 instanceof TLRPC.messages_Messages) {
                            final TLRPC.messages_Messages r = (TLRPC.messages_Messages) res1;
                            TLRPC.Message message = null;
                            for (int i = 0; i < r.messages.size(); ++i) {
                                if (r.messages.get(i).id == msg_id) {
                                    message = r.messages.get(i);
                                    break;
                                }
                            }
                            if (message != null) {
                                saveToProfile(new MessageObject(currentAccount, message, false, true), save, done, true);
                            } else {
                                AndroidUtilities.runOnUIThread(() -> {
                                    BulletinFactory.of((FrameLayout) containerView, resourcesProvider)
                                            .createErrorBulletin(LocaleController.formatString(R.string.UnknownErrorCode, "CLIENT_MESSAGE_NOT_FOUND"))
                                            .show();
                                });
                            }
                        } else if (err1 != null) {
                            AndroidUtilities.runOnUIThread(() -> {
                                BulletinFactory.of((FrameLayout) containerView, resourcesProvider)
                                        .showForError(err1);
                            });
                        }
                    });
                }
                return;
            } else if (err != null) {
                AndroidUtilities.runOnUIThread(() -> {
                    BulletinFactory.of((FrameLayout) containerView, resourcesProvider)
                            .showForError(err);
                });
            }

            AndroidUtilities.runOnUIThread(() -> {
                MessagesController.getInstance(currentAccount)
                        .getSavedMusicIds()
                        .update(documentId, save);
                final long selfId = UserConfig.getInstance(currentAccount).getClientUserId();
                final TLRPC.UserFull userInfo = MessagesController.getInstance(currentAccount).getUserFull(selfId);
                if (userInfo != null) {
                    if (save) {
                        userInfo.flags2 |= TLObject.FLAG_21;
                        userInfo.saved_music = document;
                    } else if (userInfo.saved_music != null && userInfo.saved_music.id == documentId) {
                        userInfo.flags2 &=~ TLObject.FLAG_21;
                        userInfo.saved_music = null;
                    }
                    MessagesStorage.getInstance(currentAccount).updateUserInfo(userInfo, true);
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.profileMusicUpdated, selfId);
                }
                if (done != null) {
                    done.run();
                }
            });
        });
    }

    private void showOptions(AudioPlayerCell cell, MessageObject messageObject) {
        final ItemOptions o = ItemOptions.makeOptions(container, resourcesProvider, cell, true);

        if (isMyList()) {
            o.addIf(!noforwards, R.drawable.msg_forward, getString(R.string.Forward), () -> {
                o.dismiss();
                forward(messageObject);
            });
            o.addIf(!noforwards, R.drawable.msg_shareout, getString(R.string.ShareFile), () -> {
                o.dismiss();
                share(messageObject);
            });
            o.add(R.drawable.msg_delete, getString(R.string.Delete), true, () -> {
                saveToProfile(messageObject, false, () -> {
                    savedMusicList.remove(messageObject);
                    playlist.remove(messageObject);
                    listAdapter.notifyDataSetChanged();
                    o.dismiss();

                    setVisibleInProfile(false);
                    BulletinFactory.of((FrameLayout) containerView, resourcesProvider)
                            .createSimpleBulletin(R.raw.ic_delete, getString(R.string.AudioSaveToMyProfileUnsaved))
                            .show();
                }, false);
            });
        } else {
            final MessagesController.SavedMusicIds musicIds = MessagesController.getInstance(currentAccount).getSavedMusicIds();
            final TLRPC.Document document = messageObject.getDocument();
            final long documentId = document != null ? document.id : 0;

            final ItemOptions o2 = o.makeSwipeback();
            o2.add(R.drawable.ic_ab_back, getString(R.string.Back), o::closeSwipeback);
            o2.addGap();
            o2.addIf(!musicIds.ids.contains(documentId), R.drawable.left_status_profile, getString(R.string.AudioSaveToMyProfile), () -> {
                saveToProfile(messageObject, true, () -> {
                    setVisibleInProfile(true);
                    BulletinFactory.of((FrameLayout) containerView, resourcesProvider)
                            .createSimpleBulletin(R.raw.saved_messages, getString(R.string.AudioSaveToMyProfileSaved))
                            .show();
                    o.dismiss();
                }, false);
            });
            o2.add(R.drawable.msg_saved, getString(R.string.AudioSaveToSavedMessages), () -> {
                forward(messageObject, UserConfig.getInstance(currentAccount).getClientUserId());
                o.dismiss();

                BulletinFactory.of((FrameLayout) containerView, resourcesProvider)
                        .createSimpleBulletin(R.raw.saved_messages, getString(R.string.AudioSaveToSavedMessagesSaved))
                        .show();
            });
            o2.add(R.drawable.menu_download_round, getString(R.string.AudioSaveToMusicFolder), () -> {
                saveToMusic(messageObject);
                o.dismiss();
            });
            o2.addGap();
            o2.addText(getString(R.string.AudioSaveToInfo), 12, dp(200));

            o.addIf(!noforwards, R.drawable.msg_stories_save, getString(R.string.AudioSaveTo), () -> o.openSwipeback(o2));
            if (!noforwards && o.getLast() != null)
                o.getLast().setRightIcon(R.drawable.msg_arrowright);

            o.addGap();
            o.addIf(!noforwards, R.drawable.msg_forward, getString(R.string.Forward), () -> {
                o.dismiss();
                forward(messageObject);
            });
            o.addIf(!noforwards, R.drawable.msg_share, getString(R.string.ShareFile), () -> {
                o.dismiss();
                share(messageObject);
            });
            o.addIf(messageObject.getId() > 0, R.drawable.msg_view_file, getString(R.string.ShowInChat), () -> {
                if (UserConfig.selectedAccount != currentAccount) {
                    parentActivity.switchToAccount(currentAccount, true);
                }

                Bundle args = new Bundle();
                long did = messageObject.getDialogId();
                if (DialogObject.isEncryptedDialog(did)) {
                    args.putInt("enc_id", DialogObject.getEncryptedChatId(did));
                } else if (DialogObject.isUserDialog(did)) {
                    args.putLong("user_id", did);
                } else {
                    TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-did);
                    if (chat != null && chat.migrated_to != null) {
                        args.putLong("migrated_to", did);
                        did = -chat.migrated_to.channel_id;
                    }
                    args.putLong("chat_id", -did);
                }
                args.putInt("message_id", messageObject.getId());
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.closeChats);
                parentActivity.presentFragment(new ChatActivity(args), false, false);
                dismiss();
            });
        }

        o.setGravity(LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT);
        o.show();
    }

    private void setVisibleInProfile(boolean visible) {
        if (isMyList() || noforwards) {
            saveToProfileButton.setVisibility(View.GONE);
            unsaveFromProfileTextView.setVisibility(View.GONE);
            return;
        }
        saveToProfileButton.setVisibility(View.VISIBLE);
        unsaveFromProfileTextView.setVisibility(View.VISIBLE);
        saveToProfileButton.animate()
                .alpha(visible ? 0.0f : 1.0f)
                .scaleX(visible ? 0.8f : 1.0f)
                .scaleY(visible ? 0.8f : 1.0f)
                .setDuration(420)
                .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                .withEndAction(() -> {
                    saveToProfileButton.setVisibility(visible ? View.GONE : View.VISIBLE);
                })
                .start();
        unsaveFromProfileTextView.animate()
                .alpha(!visible ? 0.0f : 1.0f)
                .scaleX(!visible ? 0.8f : 1.0f)
                .scaleY(!visible ? 0.8f : 1.0f)
                .setDuration(420)
                .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                .withEndAction(() -> {
                    unsaveFromProfileTextView.setVisibility(visible ? View.VISIBLE : View.GONE);
                })
                .start();
    }

    private void saveToMusic(MessageObject messageObject) {
        if (Build.VERSION.SDK_INT >= 23 && (Build.VERSION.SDK_INT <= 28 || BuildVars.NO_SCOPED_STORAGE) && parentActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            parentActivity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
            return;
        }
        String fileName = FileLoader.getDocumentFileName(messageObject.getDocument());
        if (TextUtils.isEmpty(fileName)) {
            fileName = messageObject.getFileName();
        }
        String path = messageObject.messageOwner.attachPath;
        if (path != null && path.length() > 0) {
            File temp = new File(path);
            if (!temp.exists()) {
                path = null;
            }
        }
        if (path == null || path.length() == 0) {
            path = FileLoader.getInstance(currentAccount).getPathToMessage(messageObject.messageOwner).toString();
        }
        MediaController.saveFile(path, parentActivity, 3, fileName, messageObject.getDocument() != null ? messageObject.getDocument().mime_type : "", uri -> BulletinFactory.of((FrameLayout) containerView, resourcesProvider).createDownloadBulletin(BulletinFactory.FileType.AUDIO).show());
    }

    private void share(MessageObject messageObject) {
        try {
            File f = null;
            boolean isVideo = false;

            if (!TextUtils.isEmpty(messageObject.messageOwner.attachPath)) {
                f = new File(messageObject.messageOwner.attachPath);
                if (!f.exists()) {
                    f = null;
                }
            }
            if (f == null) {
                f = FileLoader.getInstance(currentAccount).getPathToMessage(messageObject.messageOwner);
            }

            if (f.exists()) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(messageObject.getMimeType());
                if (Build.VERSION.SDK_INT >= 24) {
                    try {
                        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(ApplicationLoader.applicationContext, ApplicationLoader.getApplicationId() + ".provider", f));
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignore) {
                        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
                    }
                } else {
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
                }

                parentActivity.startActivityForResult(Intent.createChooser(intent, LocaleController.getString(R.string.ShareFile)), 500);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
                builder.setTitle(LocaleController.getString(R.string.AppName));
                builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
                builder.setMessage(LocaleController.getString(R.string.PleaseDownload));
                builder.show();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void forward(MessageObject messageObject, long dialogId) {
        if (UserConfig.selectedAccount != currentAccount) {
            parentActivity.switchToAccount(currentAccount, true);
        }
        final ArrayList<MessageObject> fmessages;
        final TLRPC.TL_document document;
        if (messageObject.getId() < 0) {
            fmessages = null;
            if (!(messageObject.getDocument() instanceof TLRPC.TL_document)) {
                return;
            }
            document = (TLRPC.TL_document) messageObject.getDocument();
        } else {
            fmessages = new ArrayList<>();
            fmessages.add(messageObject);
            document = null;
        }
        if (fmessages != null) {
            SendMessagesHelper.getInstance(currentAccount).sendMessage(fmessages, dialogId, false, false, !NaConfig.INSTANCE.getSilentMessageByDefault().Bool(), 0, 0);
        } else {
            SendMessagesHelper.getInstance(currentAccount).sendMessage(SendMessagesHelper.SendMessageParams.of(document, null, messageObject.messageOwner.attachPath, dialogId, null, null, null, null, null, null, !NaConfig.INSTANCE.getSilentMessageByDefault().Bool(), 0, 0, 0, savedMusicList, null, false, false));
        }
        final BaseFragment lastFragment = LaunchActivity.getLastFragment();
        if (lastFragment != null) {
            BulletinFactory.of(lastFragment)
                    .createSimpleBulletin(
                            R.raw.forward,
                            dialogId == UserConfig.getInstance(currentAccount).getClientUserId() ?
                                    LocaleController.getString(R.string.FwdMessageToSavedMessages) :
                                    dialogId > 0 ?
                                            LocaleController.formatString(R.string.FwdMessageToUser, DialogObject.getShortName(dialogId)) :
                                            LocaleController.formatString(R.string.FwdMessageToGroup, DialogObject.getShortName(dialogId))
                    )
                    .show();
        }
    }

    private void forward(MessageObject messageObject) {
        if (UserConfig.selectedAccount != currentAccount) {
            parentActivity.switchToAccount(currentAccount, true);
        }
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_FORWARD);
        args.putBoolean("canSelectTopics", true);
        DialogsActivity fragment = new DialogsActivity(args);
        final ArrayList<MessageObject> fmessages;
        final TLRPC.TL_document document;
        if (messageObject.getId() < 0) {
            fmessages = null;
            if (!(messageObject.getDocument() instanceof TLRPC.TL_document)) {
                return;
            }
            document = (TLRPC.TL_document) messageObject.getDocument();
        } else {
            fmessages = new ArrayList<>();
            fmessages.add(messageObject);
            document = null;
        }
        fragment.setDelegate((fragment1, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
            if (dids.size() > 1 || dids.get(0).dialogId == UserConfig.getInstance(currentAccount).getClientUserId() || message != null || fmessages == null) {
                for (int a = 0; a < dids.size(); a++) {
                    long did = dids.get(a).dialogId;
                    if (message != null) {
                        SendMessagesHelper.getInstance(currentAccount).sendMessage(SendMessagesHelper.SendMessageParams.of(message.toString(), did, null, null, null, true, null, null, null, !NaConfig.INSTANCE.getSilentMessageByDefault().Bool(), 0, 0, null, false));
                    }
                    if (fmessages != null) {
                        SendMessagesHelper.getInstance(currentAccount).sendMessage(fmessages, did, false, false, !NaConfig.INSTANCE.getSilentMessageByDefault().Bool(), 0, 0);
                    } else {
                        SendMessagesHelper.getInstance(currentAccount).sendMessage(SendMessagesHelper.SendMessageParams.of(document, null, messageObject.messageOwner.attachPath, did, null, null, null, null, null, null, notify && !NaConfig.INSTANCE.getSilentMessageByDefault().Bool(), scheduleDate, 0, 0, savedMusicList, null, false, false));
                    }
                }
                fragment1.finishFragment();
                final BaseFragment lastFragment = LaunchActivity.getLastFragment();
                if (lastFragment != null) {
                    BulletinFactory.of(lastFragment)
                            .createSimpleBulletin(
                                    R.raw.forward,
                                    dids.size() == 1 && dids.get(0).dialogId == UserConfig.getInstance(currentAccount).getClientUserId() ?
                                            LocaleController.getString(R.string.FwdMessageToSavedMessages) :
                                            dids.size() == 1 && dids.get(0).dialogId > 0 ?
                                                    LocaleController.formatString(R.string.FwdMessageToUser, DialogObject.getShortName(dids.get(0).dialogId)) :
                                                    dids.size() == 1 && dids.get(0).dialogId < 0 ?
                                                            LocaleController.formatString(R.string.FwdMessageToGroup, DialogObject.getShortName(dids.get(0).dialogId)) :
                                                            LocaleController.formatPluralStringComma("FwdMessageToManyChats", dids.size())
                            )
                            .show();
                }
            } else {
                MessagesStorage.TopicKey topicKey = dids.get(0);
                long did = topicKey.dialogId;
                Bundle args1 = new Bundle();
                args1.putBoolean("scrollToTopOnResume", true);
                if (DialogObject.isEncryptedDialog(did)) {
                    args1.putInt("enc_id", DialogObject.getEncryptedChatId(did));
                } else if (DialogObject.isUserDialog(did)) {
                    args1.putLong("user_id", did);
                } else {
                    args1.putLong("chat_id", -did);
                }
                ChatActivity chatActivity = new ChatActivity(args1);
                if (topicKey.topicId != 0) {
                    ForumUtilities.applyTopic(chatActivity, topicKey);
                }
                if (parentActivity.presentFragment(chatActivity, true, false)) {
                    chatActivity.showFieldPanelForForward(true, fmessages);
                    if (topicKey.topicId != 0) {
                        fragment1.removeSelfFromStack();
                    }
                } else {
                    fragment1.finishFragment();
                }
            }
            return true;
        });
        parentActivity.presentFragment(fragment);
        dismiss();
    }

    private static abstract class CoverContainer extends FrameLayout {

        private final BackupImageView[] imageViews = new BackupImageView[2];

        private int activeIndex;
        private AnimatorSet animatorSet;

        public CoverContainer(@NonNull Context context) {
            super(context);
            for (int i = 0; i < 2; i++) {
                imageViews[i] = new BackupImageView(context);
                final int index = i;
                imageViews[i].getImageReceiver().setDelegate((imageReceiver, set, thumb, memCache) -> {
                    if (index == activeIndex) {
                        onImageUpdated(imageReceiver);
                    }
                });
                imageViews[i].setRoundRadius(dp(20)); // скруглённые грани обложки
                if (i == 1) {
                    imageViews[i].setVisibility(GONE);
                }
                addView(imageViews[i], LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            }
        }

        public final void switchImageViews() {
            if (animatorSet != null) {
                animatorSet.cancel();
            }
            animatorSet = new AnimatorSet();
            activeIndex = activeIndex == 0 ? 1 : 0;

            final BackupImageView prevImageView = imageViews[activeIndex == 0 ? 1 : 0];
            final BackupImageView currImageView = imageViews[activeIndex];

            final boolean hasBitmapImage = prevImageView.getImageReceiver().hasBitmapImage();

            currImageView.setAlpha(hasBitmapImage ? 1f : 0f);
            currImageView.setScaleX(0.8f);
            currImageView.setScaleY(0.8f);
            currImageView.setVisibility(VISIBLE);

            if (hasBitmapImage) {
                prevImageView.bringToFront();
            } else {
                prevImageView.setVisibility(GONE);
                prevImageView.setImageDrawable(null);
            }

            final ValueAnimator expandAnimator = ValueAnimator.ofFloat(0.8f, 1f);
            expandAnimator.setDuration(125);
            expandAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT);
            expandAnimator.addUpdateListener(a -> {
                float animatedValue = (float) a.getAnimatedValue();
                currImageView.setScaleX(animatedValue);
                currImageView.setScaleY(animatedValue);
                if (!hasBitmapImage) {
                    currImageView.setAlpha(a.getAnimatedFraction());
                }
            });

            if (hasBitmapImage) {
                final ValueAnimator collapseAnimator = ValueAnimator.ofFloat(prevImageView.getScaleX(), 0.8f);
                collapseAnimator.setDuration(125);
                collapseAnimator.setInterpolator(CubicBezierInterpolator.EASE_IN);
                collapseAnimator.addUpdateListener(a -> {
                    float animatedValue = (float) a.getAnimatedValue();
                    prevImageView.setScaleX(animatedValue);
                    prevImageView.setScaleY(animatedValue);
                    final float fraction = a.getAnimatedFraction();
                    if (fraction > 0.25f && !currImageView.getImageReceiver().hasBitmapImage()) {
                        prevImageView.setAlpha(1f - (fraction - 0.25f) * (1f / 0.75f));
                    }
                });
                collapseAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        prevImageView.setVisibility(GONE);
                        prevImageView.setImageDrawable(null);
                        prevImageView.setAlpha(1f);
                    }
                });

                animatorSet.playSequentially(collapseAnimator, expandAnimator);
            } else {
                animatorSet.play(expandAnimator);
            }

            animatorSet.start();
        }

        public final BackupImageView getImageView() {
            return imageViews[activeIndex];
        }

        public final BackupImageView getNextImageView() {
            return imageViews[activeIndex == 0 ? 1 : 0];
        }

        public final ImageReceiver getImageReceiver() {
            return getImageView().getImageReceiver();
        }

        protected abstract void onImageUpdated(ImageReceiver imageReceiver);
    }

    public abstract static class ClippingTextViewSwitcher extends FrameLayout {

        private final TextView[] textViews = new TextView[2];
        private final float[] clipProgress = new float[]{0f, 0.75f};
        private final int gradientSize = dp(24);

        private final Matrix gradientMatrix;
        private final Paint gradientPaint;
        private final Paint erasePaint;

        private int activeIndex;
        private AnimatorSet animatorSet;
        private LinearGradient gradientShader;
        private int stableOffest = -1;
        private final RectF rectF = new RectF();
        private int rightPadding;

        public ClippingTextViewSwitcher(@NonNull Context context) {
            super(context);
            for (int i = 0; i < 2; i++) {
                textViews[i] = createTextView();
                if (i == 1) {
                    textViews[i].setAlpha(0f);
                    textViews[i].setVisibility(GONE);
                }
                addView(textViews[i], LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));
            }
            gradientMatrix = new Matrix();
            gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            gradientPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            erasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            erasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            gradientShader = new LinearGradient(gradientSize, 0, 0, 0, 0, 0xFF000000, Shader.TileMode.CLAMP);
            gradientPaint.setShader(gradientShader);
        }

        private boolean isCenter;

        public void setIsCenter() {
            isCenter = true;
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            if (isCenter) {
                for (int a = 0; a < textViews.length; a++) {
                    View v = textViews[a];
                    if (v == null) continue;
                    if (v.getMeasuredWidth() < getMeasuredWidth()) {
                        int l = (getMeasuredWidth() - v.getMeasuredWidth()) / 2;
                        v.layout(l, 0, l + v.getMeasuredWidth(), v.getMeasuredHeight());
                    }
                }
            }
        }

        @Override
        protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
            final int index = child == textViews[0] ? 0 : 1;
            final boolean result;
            boolean hasStableRect = false;

            if (isCenter) {
                stableOffest = -1;
            }

            if (stableOffest > 0) {
                for (TextView tv : textViews) {
                    if (tv instanceof MarqueeTextView && ((MarqueeTextView) tv).isNeedMarquee()) {
                        stableOffest = -1;
                        break;
                    }
                }
            }

            if (stableOffest > 0 && textViews[activeIndex].getAlpha() != 1f && textViews[activeIndex].getLayout() != null) {
                float x1 = textViews[activeIndex].getLayout().getPrimaryHorizontal(0);
                float x2 = textViews[activeIndex].getLayout().getPrimaryHorizontal(stableOffest);
                hasStableRect = true;
                if (x1 == x2) {
                    hasStableRect = false;
                } else if (x2 > x1) {
                    rectF.set(x1, 0, x2, getMeasuredHeight());
                } else {
                    rectF.set(x2, 0, x1, getMeasuredHeight());
                }

                if (hasStableRect && index == activeIndex) {
                    canvas.save();
                    canvas.clipRect(rectF);
                    textViews[0].draw(canvas);
                    canvas.restore();
                }
            }
            if (clipProgress[index] > 0f || hasStableRect) {
                final int width = Math.min(child.getWidth(), getWidth()); // - rightPadding;
                final int height = Math.min(child.getHeight(), getHeight());
                final int saveCount = canvas.saveLayer(0, 0, width, height, null, Canvas.ALL_SAVE_FLAG);
                result = super.drawChild(canvas, child, drawingTime);
                final float gradientStart = width * (1f - clipProgress[index]);
                final float gradientEnd = gradientStart + gradientSize;
                gradientMatrix.setTranslate(gradientStart, 0);
                gradientShader.setLocalMatrix(gradientMatrix);
                canvas.drawRect(gradientStart, 0, gradientEnd, height, gradientPaint);
                if (width > gradientEnd) {
                    canvas.drawRect(gradientEnd, 0, width, height, erasePaint);
                }
                if (hasStableRect) {
                    canvas.drawRect(rectF, erasePaint);
                }
                canvas.restoreToCount(saveCount);
            } else {
                result = super.drawChild(canvas, child, drawingTime);
            }
            return result;
        }

        public void setText(CharSequence text) {
            setText(text, true);
        }

        public void setText(CharSequence text, boolean animated) {
            final CharSequence currentText = textViews[activeIndex].getText();

            if (TextUtils.isEmpty(currentText) || !animated) {
                textViews[activeIndex].setText(text);
                return;
            } else if (TextUtils.equals(text, currentText)) {
                return;
            }

            stableOffest = 0;
            int n = Math.min(text.length(), currentText.length());
            for (int i = 0; i < n; i++) {
                if (text.charAt(i) != currentText.charAt(i)) {
                    break;
                }
                stableOffest++;
            }
            if (stableOffest <= 3) {
                stableOffest = -1;
            }

            final int index = activeIndex == 0 ? 1 : 0;
            final int prevIndex = activeIndex;
            activeIndex = index;

            if (animatorSet != null) {
                animatorSet.cancel();
            }
            animatorSet = new AnimatorSet();
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    textViews[prevIndex].setVisibility(GONE);
                }
            });

            textViews[index].setText(text);
            textViews[index].bringToFront();
            textViews[index].setVisibility(VISIBLE);

            final int duration = 300;

            final ValueAnimator collapseAnimator = ValueAnimator.ofFloat(clipProgress[prevIndex], 0.75f);
            collapseAnimator.setDuration(duration / 3 * 2); // 0.66
            collapseAnimator.addUpdateListener(a -> {
                clipProgress[prevIndex] = (float) a.getAnimatedValue();
                invalidate();
            });

            final ValueAnimator expandAnimator = ValueAnimator.ofFloat(clipProgress[index], 0f);
            expandAnimator.setStartDelay(duration / 3); // 0.33
            expandAnimator.setDuration(duration / 3 * 2); // 0.66
            expandAnimator.addUpdateListener(a -> {
                clipProgress[index] = (float) a.getAnimatedValue();
                invalidate();
            });

            final ObjectAnimator fadeOutAnimator = ObjectAnimator.ofFloat(textViews[prevIndex], View.ALPHA, 0f);
            fadeOutAnimator.setStartDelay(duration / 4); // 0.25
            fadeOutAnimator.setDuration(duration / 2); // 0.5

            final ObjectAnimator fadeInAnimator = ObjectAnimator.ofFloat(textViews[index], View.ALPHA, 1f);
            fadeInAnimator.setStartDelay(duration / 4); // 0.25
            fadeInAnimator.setDuration(duration / 2); // 0.5

            animatorSet.playTogether(collapseAnimator, expandAnimator, fadeOutAnimator, fadeInAnimator);
            animatorSet.start();
        }

        public TextView getTextView() {
            return textViews[activeIndex];
        }

        public TextView getNextTextView() {
            return textViews[activeIndex == 0 ? 1 : 0];
        }

        public int getCustomPaddingRight() {
            return rightPadding;
        }

        public void setCustomPaddingRight(int padding) {
            rightPadding = padding;
            for (TextView tv : textViews) {
                if (tv instanceof MarqueeTextView) {
                    ((MarqueeTextView) tv).setCustomPaddingRight(padding);
                }
            }
            invalidate();
        }

        protected abstract TextView createTextView();
    }


    private ValueAnimator rightPaddingAnimator;

    private void setCustomPaddingRight(int padding, boolean animated) {
        if (rightPaddingAnimator != null) {
            rightPaddingAnimator.cancel();
            rightPaddingAnimator = null;
        }
        if (titleTextView.getCustomPaddingRight() == padding) {
            return;
        }

        if (!animated) {
            titleTextView.setCustomPaddingRight(padding);
            authorTextView.setCustomPaddingRight(padding);
            return;
        }

        rightPaddingAnimator = ValueAnimator.ofInt(titleTextView.getCustomPaddingRight(), padding);
        if (padding == 0) {
            rightPaddingAnimator.setStartDelay(200L);
            rightPaddingAnimator.setDuration(100L);
        } else {
            rightPaddingAnimator.setDuration(200L);
        }
        rightPaddingAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        rightPaddingAnimator.addUpdateListener(animation -> {
            titleTextView.setCustomPaddingRight((int) animation.getAnimatedValue());
            authorTextView.setCustomPaddingRight((int) animation.getAnimatedValue());
        });
        rightPaddingAnimator.start();
    }
    private void toggleLyrics() {
        if (lyricsAnimator != null) lyricsAnimator.cancel();
        isLyricsVisible = !isLyricsVisible;

        if (isLyricsVisible) {
            lyricsContainer.setVisibility(View.VISIBLE);
            MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
            if (playing != null) fetchLyrics(playing);
        }

        lyricsAnimator = ValueAnimator.ofFloat(isLyricsVisible ? 0f : 1f, isLyricsVisible ? 1f : 0f);
        lyricsAnimator.setDuration(250);
        lyricsAnimator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            lyricsContainer.setAlpha(alpha);

            // Плавно скрываем обложку и рамку
            if (coverContainer != null) coverContainer.setAlpha(1f - alpha);
            if (borderView != null) borderView.setAlpha(1f - alpha);
        });
        lyricsAnimator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (!isLyricsVisible) {
                    lyricsContainer.setVisibility(View.GONE);
                    if (coverContainer != null) {
                        coverContainer.setVisibility(View.VISIBLE);
                        coverContainer.setAlpha(1f);
                    }
                    if (borderView != null) {
                        borderView.setVisibility(View.VISIBLE);
                        borderView.setAlpha(1f);
                    }
                } else {
                    // Полностью прячем, чтобы не перекрывало клики (как ты просил, никаких иконок под текстом)
                    if (coverContainer != null) coverContainer.setVisibility(View.INVISIBLE);
                    if (borderView != null) borderView.setVisibility(View.INVISIBLE);
                }
            }
        });
        lyricsAnimator.start();
    }


    private static final java.util.HashMap<String, ArrayList<LrcLine>> lyricsInMemoryCache = new java.util.HashMap<>();

    private void fetchLyrics(MessageObject msg) {
        lyricsLines.clear();
        lyricsLines.add(new LrcLine(0, "Поиск текста..."));
        if (lyricsAdapter != null) lyricsAdapter.notifyDataSetChanged();
        currentLyricIndex = -1;

        String title = msg.getMusicTitle();
        String artist = msg.getMusicAuthor();

        // Очистка имени для поиска
        String cleanArtist = artist.replaceAll("(?i)(feat\\.|ft\\.|&|,).*$", "").trim();
        String cleanTitle  = title.replaceAll("(?i)(\\(.*?\\)|\\[.*?\\]|-.*?(remix|edit|mix)).*$", "").trim();
        String cacheKey = (cleanArtist + "|" + cleanTitle).toLowerCase();

        // 1. In-memory cache
        if (lyricsInMemoryCache.containsKey(cacheKey)) {
            ArrayList<LrcLine> cached = lyricsInMemoryCache.get(cacheKey);
            AndroidUtilities.runOnUIThread(() -> {
                lyricsLines.clear();
                lyricsLines.addAll(cached);
                if (lyricsAdapter != null) lyricsAdapter.notifyDataSetChanged();
            });
            return;
        }

        new Thread(() -> {
            try {
                // 2. Файловый кэш
                File textDir = new File(ApplicationLoader.applicationContext.getExternalFilesDir(null), "Texts");
                if (!textDir.exists()) textDir.mkdirs();
                File lyricsFile = new File(textDir, cleanArtist + " - " + cleanTitle + ".lrc");

                if (lyricsFile.exists()) {
                    StringBuilder sb = new StringBuilder();
                    java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(lyricsFile));
                    String fileLine;
                    while ((fileLine = br.readLine()) != null) sb.append(fileLine).append("\n");
                    br.close();
                    String content = sb.toString();
                    if (!content.trim().isEmpty() && !content.contains("NOT_FOUND")) {
                        ArrayList<LrcLine> parsed = new ArrayList<>();
                        parseLrcIntoList(content, parsed);
                        if (!parsed.isEmpty()) {
                            lyricsInMemoryCache.put(cacheKey, parsed);
                            AndroidUtilities.runOnUIThread(() -> {
                                lyricsLines.clear();
                                lyricsLines.addAll(parsed);
                                if (lyricsAdapter != null) lyricsAdapter.notifyDataSetChanged();
                            });
                            return;
                        }
                    }
                }

                // 3. Сетевой запрос — метод пользователя как база
                String query = cleanTitle + " " + cleanArtist;
                java.net.URL url = new java.net.URL("https://lrclib.net/api/search?q="
                        + java.net.URLEncoder.encode(query, "UTF-8"));
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "SoundGram-Android/1.0");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                if (conn.getResponseCode() != 200) {
                    saveLyricsNotFound(lyricsFile);
                    showLyricsError();
                    return;
                }

                java.io.InputStream is = conn.getInputStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                is.close();

                if (TextUtils.isEmpty(response) || !response.startsWith("[")) {
                    saveLyricsNotFound(lyricsFile);
                    showLyricsError();
                    return;
                }

                org.json.JSONArray array = new org.json.JSONArray(response);
                String foundSynced = "";
                String foundPlain  = "";

                // Точное совпадение по title
                for (int i = 0; i < array.length(); i++) {
                    org.json.JSONObject obj = array.getJSONObject(i);
                    String resultTitle = obj.optString("trackName", "").toLowerCase();
                    String synced = obj.optString("syncedLyrics", "");
                    String plain  = obj.optString("plainLyrics", "");

                    boolean titleMatch = resultTitle.contains(cleanTitle.toLowerCase())
                            || cleanTitle.toLowerCase().contains(resultTitle);

                    if (titleMatch && !TextUtils.isEmpty(synced) && !synced.equals("null")) {
                        foundSynced = synced;
                        foundPlain  = plain;
                        break;
                    }
                }

                // Фолбэк — первый с текстом
                if (TextUtils.isEmpty(foundSynced)) {
                    for (int i = 0; i < array.length(); i++) {
                        String synced = array.getJSONObject(i).optString("syncedLyrics", "");
                        String plain  = array.getJSONObject(i).optString("plainLyrics", "");
                        if (!TextUtils.isEmpty(synced) && !synced.equals("null")) {
                            foundSynced = synced;
                            foundPlain  = plain;
                            break;
                        }
                        if (!TextUtils.isEmpty(plain) && !plain.equals("null") && TextUtils.isEmpty(foundPlain)) {
                            foundPlain = plain;
                        }
                    }
                }

                String toUse = !TextUtils.isEmpty(foundSynced) ? foundSynced : foundPlain;

                if (TextUtils.isEmpty(toUse) || toUse.equals("null")) {
                    saveLyricsNotFound(lyricsFile);
                    showLyricsError();
                    return;
                }

                // Сохраняем в файл
                try (java.io.FileWriter fw = new java.io.FileWriter(lyricsFile)) {
                    fw.write(toUse);
                } catch (Exception ignored) {}

                // Парсим и кладём в память
                ArrayList<LrcLine> result = new ArrayList<>();
                if (!TextUtils.isEmpty(foundSynced)) {
                    parseLrcIntoList(foundSynced, result);
                }
                if (result.isEmpty() && !TextUtils.isEmpty(foundPlain)) {
                    for (String l : foundPlain.split("\n")) {
                        if (!l.trim().isEmpty()) result.add(new LrcLine(-1, l.trim()));
                    }
                }

                if (!result.isEmpty()) {
                    lyricsInMemoryCache.put(cacheKey, result);
                    AndroidUtilities.runOnUIThread(() -> {
                        lyricsLines.clear();
                        lyricsLines.addAll(result);
                        if (lyricsAdapter != null) lyricsAdapter.notifyDataSetChanged();
                    });
                } else {
                    saveLyricsNotFound(lyricsFile);
                    showLyricsError();
                }

            } catch (Exception e) {
                FileLog.e(e);
                showLyricsError();
            }
        }).start();
    }

    private void saveLyricsNotFound(File f) {
        try (java.io.FileWriter fw = new java.io.FileWriter(f)) { fw.write("NOT_FOUND"); } catch (Exception ignored) {}
    }

    private void showLyricsError() {
        AndroidUtilities.runOnUIThread(() -> {
            lyricsLines.clear();
            lyricsLines.add(new LrcLine(0, "Текст не найден"));
            if (lyricsAdapter != null) lyricsAdapter.notifyDataSetChanged();
        });
    }

    // Вынесенный метод парсинга LRC в ArrayList (заменяет parseLrc)
    private void parseLrcIntoList(String lrcContent, ArrayList<LrcLine> out) {
        String[] lines = lrcContent.split("\n");
        for (String line : lines) {
            line = line.trim();
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("\\[(\\d{2,}):(\\d{2})(?:\\.(\\d{1,3}))?\\](.*)")
                    .matcher(line);
            while (matcher.find()) {
                try {
                    int min = Integer.parseInt(matcher.group(1));
                    int sec = Integer.parseInt(matcher.group(2));
                    String msStr = matcher.group(3);
                    int ms = 0;
                    if (msStr != null) {
                        ms = Integer.parseInt(msStr);
                        if (msStr.length() == 1) ms *= 100;
                        else if (msStr.length() == 2) ms *= 10;
                    }
                    long timeMs = min * 60000L + sec * 1000L + ms;
                    String text = matcher.group(4).trim();
                    if (!TextUtils.isEmpty(text)) out.add(new LrcLine(timeMs, text));
                } catch (Exception ignored) {}
            }
        }
        java.util.Collections.sort(out, (a, b) -> Long.compare(a.timeMs, b.timeMs));
    }
    private class LyricsAdapter extends RecyclerListView.SelectionAdapter {
        private Context mContext;

        public LyricsAdapter(Context context) {
            mContext = context;
        }

        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(getContext());
            textView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            textView.setGravity(Gravity.CENTER);
            textView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
            return new RecyclerListView.Holder(textView);
        }
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            TextView textView = (TextView) holder.itemView;
            textView.setText(lyricsLines.get(position).text);

            int activeIndex = currentLyricIndex != -1 ? currentLyricIndex : 0;
            int distance = Math.abs(position - activeIndex);

            if (distance == 0) {
                // АКТИВНАЯ СТРОКА (которая сейчас играет)
                textView.setAlpha(1.0f);
                textView.setScaleX(1.05f);
                textView.setScaleY(1.05f);
                textView.setTextColor(0xFFFFFFFF);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
                textView.setShadowLayer(25f, 0f, 0f, 0xCCFFFFFF);
            } else if (distance == 1) {
                // СОСЕДНИЕ СТРОКИ (1 предыдущая, 1 следующая)
                textView.setAlpha(0.45f);
                textView.setScaleX(1.0f);
                textView.setScaleY(1.0f);
                textView.setTextColor(0xFFFFFFFF);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
                textView.setShadowLayer(0f, 0f, 0f, 0);
            } else if (distance == 2) {
                // ВТОРЫЕ СОСЕДНИЕ (2 предыдущая, 2 следующая)
                textView.setAlpha(0.15f);
                textView.setScaleX(0.95f); // Чуть уменьшаем для красивого эффекта глубины
                textView.setScaleY(0.95f);
                textView.setTextColor(0xFFFFFFFF);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
                textView.setShadowLayer(0f, 0f, 0f, 0);
            } else {
                // ВСЕ ОСТАЛЬНЫЕ СТРОКИ (полностью скрыты)
                textView.setAlpha(0.0f);
                textView.setShadowLayer(0f, 0f, 0f, 0);
            }

            // Отключаем клики, чтобы текст был просто визуалом
            textView.setOnClickListener(null);
            textView.setClickable(false);
        }

        @Override
        public int getItemCount() {
            return lyricsLines.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) { return false; }
    }
}
