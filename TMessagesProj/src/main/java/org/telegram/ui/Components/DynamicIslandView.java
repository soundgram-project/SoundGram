package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.audioinfo.AudioInfo;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

public class DynamicIslandView extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

    private final int currentAccount = UserConfig.selectedAccount;
    private final Theme.ResourcesProvider resourcesProvider;
    private final Runnable onCollapse;
    private final Runnable onOpenFullPlayer;

    private BackupImageView coverImageView;
    private TextView titleTextView;
    private TextView artistTextView;
    private SeekBarView seekBarView;
    private TextView currentTimeTextView;
    private TextView durationTextView;
    private ImageView playButton;
    private PlayPauseDrawable playPauseDrawable;
    private ImageView prevButton;
    private ImageView nextButton;
    private ImageView collapseButton;

    private boolean isDraggingSeek = false;
    private GradientDrawable cardBg;

    public DynamicIslandView(@NonNull Context context, Theme.ResourcesProvider resourcesProvider, Runnable onCollapse, Runnable onOpenFullPlayer) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        this.onCollapse = onCollapse;
        this.onOpenFullPlayer = onOpenFullPlayer;
        init(context);
    }

    private void init(Context context) {
        cardBg = new GradientDrawable();
        cardBg.setCornerRadius(AndroidUtilities.dp(20));
        updateCardColors();
        setBackground(cardBg);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(AndroidUtilities.dp(3));
            setClipToOutline(true);
        }

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
        addView(content, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // 1. Header row: [Cover] [Title + Artist] [Collapse]
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        coverImageView = new BackupImageView(context);
        coverImageView.setRoundRadius(AndroidUtilities.dp(8));
        header.addView(coverImageView, LayoutHelper.createLinear(32, 32, Gravity.CENTER_VERTICAL));

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setGravity(Gravity.CENTER_VERTICAL);

        titleTextView = new TextView(context);
        titleTextView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        titleTextView.setTypeface(AndroidUtilities.bold());
        titleTextView.setSingleLine(true);
        titleTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        titleTextView.setSelected(true);
        titleTextView.setHorizontallyScrolling(true);
        textLayout.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        artistTextView = new TextView(context);
        artistTextView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        artistTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        artistTextView.setSingleLine(true);
        artistTextView.setEllipsize(TextUtils.TruncateAt.END);
        textLayout.addView(artistTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 1, 0, 0));

        header.addView(textLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, Gravity.CENTER_VERTICAL, 10, 0, 8, 0));

        View.OnClickListener openPlayerListener = v -> {
            if (onOpenFullPlayer != null) {
                onOpenFullPlayer.run();
            }
        };
        setOnClickListener(openPlayerListener);
        content.setOnClickListener(openPlayerListener);
        header.setOnClickListener(openPlayerListener);
        coverImageView.setOnClickListener(openPlayerListener);
        textLayout.setOnClickListener(openPlayerListener);

        collapseButton = new ImageView(context);
        collapseButton.setImageResource(R.drawable.baseline_keyboard_arrow_down_24);
        collapseButton.setScaleType(ImageView.ScaleType.CENTER);
        collapseButton.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_inappPlayerClose), PorterDuff.Mode.SRC_IN));
        collapseButton.setBackground(Theme.createSelectorDrawable(0x20FFFFFF, 1, AndroidUtilities.dp(13)));
        collapseButton.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            if (onCollapse != null) {
                onCollapse.run();
            }
        });
        header.addView(collapseButton, LayoutHelper.createLinear(26, 26, Gravity.CENTER_VERTICAL));

        content.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 32));

        // 2. Squiggly Wavy Seekbar with Time Labels
        LinearLayout timeLayout = new LinearLayout(context);
        timeLayout.setOrientation(LinearLayout.HORIZONTAL);
        timeLayout.setGravity(Gravity.CENTER_VERTICAL);

        int titleColor = getThemedColor(Theme.key_dialogTextBlack);
        int accentColor = getThemedColor(Theme.key_featuredStickers_addButton);

        currentTimeTextView = new TextView(context);
        currentTimeTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        currentTimeTextView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        currentTimeTextView.setText("0:00");
        timeLayout.addView(currentTimeTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        seekBarView = new SeekBarView(context);
        seekBarView.setWavy(true);
        seekBarView.setReportChanges(true);
        seekBarView.setColors(ColorUtils.setAlphaComponent(titleColor, 40), accentColor);
        seekBarView.setDelegate(new SeekBarView.SeekBarViewDelegate() {
            @Override
            public void onSeekBarDrag(boolean stop, float progress) {
                isDraggingSeek = !stop;
                MessageObject msg = MediaController.getInstance().getPlayingMessageObject();
                if (stop && msg != null) {
                    MediaController.getInstance().seekToProgress(msg, progress);
                }
            }
        });
        timeLayout.addView(seekBarView, LayoutHelper.createLinear(0, 20, 1.0f, Gravity.CENTER_VERTICAL, 6, 0, 6, 0));

        durationTextView = new TextView(context);
        durationTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        durationTextView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        durationTextView.setText("0:00");
        timeLayout.addView(durationTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        content.addView(timeLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 20, 0, 4, 0, 0));

        // 3. Playback Controls: [Prev] [Play/Pause] [Next]
        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        prevButton = createControlButton(context, R.drawable.round_skip_previous_36, titleColor, 28, () -> {
            MediaController.getInstance().playPreviousMessage();
        });
        controls.addView(prevButton, LayoutHelper.createLinear(28, 28, Gravity.CENTER, 0, 0, 20, 0));

        playButton = new ImageView(context);
        playButton.setScaleType(ImageView.ScaleType.CENTER);
        playPauseDrawable = new PlayPauseDrawable(16);
        playPauseDrawable.setPause(!MediaController.getInstance().isMessagePaused(), false);
        playButton.setImageDrawable(playPauseDrawable);
        int iconColor = AndroidUtilities.computePerceivedBrightness(accentColor) > 0.72f ? 0xFF000000 : 0xFFFFFFFF;
        playButton.setColorFilter(new PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN));
        playButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(17), accentColor, Theme.blendOver(accentColor, 0x25000000)));
        attachScaleFeedback(playButton, 0.88f);
        playButton.setOnClickListener(v -> {
            MessageObject msg = MediaController.getInstance().getPlayingMessageObject();
            if (msg == null) return;
            if (MediaController.getInstance().isMessagePaused()) {
                MediaController.getInstance().playMessage(msg);
            } else {
                MediaController.getInstance().pauseMessage(msg);
            }
        });
        controls.addView(playButton, LayoutHelper.createLinear(34, 34, Gravity.CENTER, 0, 0, 20, 0));

        nextButton = createControlButton(context, R.drawable.round_skip_next_36, titleColor, 28, () -> {
            MediaController.getInstance().playNextMessage();
        });
        controls.addView(nextButton, LayoutHelper.createLinear(28, 28, Gravity.CENTER, 0, 0, 0, 0));

        content.addView(controls, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 34, 0, 2, 0, 0));

        setOnLongClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            if (onCollapse != null) {
                onCollapse.run();
            }
            return true;
        });

        updateTrackData();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width <= 0) {
            width = AndroidUtilities.displaySize.x;
        }
        int constrainedWidth = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        super.onMeasure(constrainedWidth, heightMeasureSpec);
    }

    private void updateCardColors() {
        if (cardBg == null) return;
        int bgCol = getThemedColor(Theme.key_inappPlayerBackground);
        if (bgCol == 0) {
            bgCol = Theme.isCurrentThemeDark() ? 0xF01C1C1E : 0xF8F2F2F7;
        } else {
            bgCol = 0xFF000000 | bgCol;
        }
        int strokeCol = Theme.isCurrentThemeDark() ? 0x22FFFFFF : 0x18000000;
        cardBg.setColor(bgCol);
        cardBg.setStroke(AndroidUtilities.dp(1), strokeCol);
    }

    private ImageView createControlButton(Context context, int iconResId, int tintColor, int sizeDp, Runnable onClick) {
        ImageView btn = new ImageView(context);
        btn.setImageResource(iconResId);
        btn.setScaleType(ImageView.ScaleType.CENTER);
        btn.setColorFilter(new PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            btn.setBackground(Theme.createSelectorDrawable(ColorUtils.setAlphaComponent(tintColor, 35), 1, AndroidUtilities.dp(sizeDp / 2f)));
        }
        attachScaleFeedback(btn, 0.84f);
        btn.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            if (onClick != null) {
                onClick.run();
            }
        });
        return btn;
    }

    private void attachScaleFeedback(View view, float scaleFactor) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(scaleFactor).scaleY(scaleFactor).setDuration(120).setInterpolator(CubicBezierInterpolator.EASE_OUT).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(220).setInterpolator(new android.view.animation.OvershootInterpolator(1.8f)).start();
                    break;
            }
            return false;
        });
    }

    public void updateTrackData() {
        MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
        if (messageObject == null) {
            return;
        }

        String title = messageObject.getMusicTitle();
        String author = messageObject.getMusicAuthor();
        if (TextUtils.isEmpty(title)) {
            title = LocaleController.getString(R.string.AudioUnknownTitle);
        }
        if (TextUtils.isEmpty(author)) {
            author = LocaleController.getString(R.string.AudioUnknownArtist);
        }

        titleTextView.setText(title);
        artistTextView.setText(author);

        // Load artwork
        AudioInfo audioInfo = MediaController.getInstance().getAudioInfo();
        if (audioInfo != null && audioInfo.getCover() != null) {
            coverImageView.setImageBitmap(audioInfo.getCover());
        } else {
            boolean setCustom = false;
            if (messageObject.messageOwner != null
                    && messageObject.messageOwner.message != null
                    && messageObject.messageOwner.message.startsWith("SG_STREAM|||")) {
                String[] sgParts = messageObject.messageOwner.message.split("\\|\\|\\|");
                if (sgParts.length >= 7 && !TextUtils.isEmpty(sgParts[6])) {
                    coverImageView.setImage(ImageLocation.getForPath(sgParts[6]), null, Theme.createRoundRectDrawable(AndroidUtilities.dp(8), Theme.getColor(Theme.key_graySection)), messageObject);
                    setCustom = true;
                }
            }
            if (!setCustom) {
                String artworkUrl = messageObject.getArtworkUrl(false);
                if (!TextUtils.isEmpty(artworkUrl)) {
                    coverImageView.setImage(ImageLocation.getForPath(artworkUrl), null, (ImageLocation) null, null, null, 0, 1, messageObject);
                } else {
                    TLRPC.Document document = messageObject.getDocument();
                    if (document != null && document.thumbs != null && !document.thumbs.isEmpty()) {
                        coverImageView.setImage(ImageLocation.getForDocument(document.thumbs.get(0), document), null, (ImageLocation) null, null, null, 0, 1, messageObject);
                    } else {
                        coverImageView.setImageDrawable(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), ColorUtils.setAlphaComponent(getThemedColor(Theme.key_dialogTextBlack), 40)));
                    }
                }
            }
        }

        updateProgress();
        if (playPauseDrawable != null) {
            playPauseDrawable.setPause(!MediaController.getInstance().isMessagePaused(), false);
        }
    }

    public void updateProgress() {
        MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
        if (messageObject == null) return;

        float progress = messageObject.audioProgress;
        long duration = (long) (messageObject.getDuration() * 1000);
        if (duration <= 0) {
            duration = MediaController.getInstance().getDuration();
        }

        if (!isDraggingSeek && seekBarView != null) {
            seekBarView.setProgress(progress, false);
        }

        long currentMs = (long) (duration * progress);
        currentTimeTextView.setText(AndroidUtilities.formatShortDuration((int) (currentMs / 1000)));
        durationTextView.setText(AndroidUtilities.formatShortDuration((int) (duration / 1000)));
    }

    public void updatePlayPause(boolean isPlaying) {
        if (playPauseDrawable != null) {
            playPauseDrawable.setPause(isPlaying, true);
        }
    }

    public void updateColors() {
        updateCardColors();
        int titleColor = getThemedColor(Theme.key_dialogTextBlack);
        int subtitleColor = getThemedColor(Theme.key_dialogTextGray2);
        int accentColor = getThemedColor(Theme.key_featuredStickers_addButton);

        if (titleTextView != null) {
            titleTextView.setTextColor(titleColor);
        }
        if (artistTextView != null) {
            artistTextView.setTextColor(subtitleColor);
        }
        if (currentTimeTextView != null) {
            currentTimeTextView.setTextColor(subtitleColor);
        }
        if (durationTextView != null) {
            durationTextView.setTextColor(subtitleColor);
        }
        if (collapseButton != null) {
            collapseButton.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_inappPlayerClose), PorterDuff.Mode.SRC_IN));
        }
        if (prevButton != null) {
            prevButton.setColorFilter(new PorterDuffColorFilter(titleColor, PorterDuff.Mode.SRC_IN));
        }
        if (nextButton != null) {
            nextButton.setColorFilter(new PorterDuffColorFilter(titleColor, PorterDuff.Mode.SRC_IN));
        }
        if (playButton != null) {
            int iconColor = AndroidUtilities.computePerceivedBrightness(accentColor) > 0.72f ? 0xFF000000 : 0xFFFFFFFF;
            playButton.setColorFilter(new PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN));
            playButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(17), accentColor, Theme.blendOver(accentColor, 0x25000000)));
        }
        if (seekBarView != null) {
            seekBarView.setColors(ColorUtils.setAlphaComponent(titleColor, 40), accentColor);
        }
    }

    private int getThemedColor(int key) {
        return Theme.getColor(key, resourcesProvider);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingProgressDidChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidReset);
        updateTrackData();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingProgressDidChanged);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidReset);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.messagePlayingPlayStateChanged) {
            if (playPauseDrawable != null) {
                playPauseDrawable.setPause(!MediaController.getInstance().isMessagePaused(), true);
            }
        } else if (id == NotificationCenter.messagePlayingProgressDidChanged) {
            updateProgress();
        } else if (id == NotificationCenter.messagePlayingDidReset) {
            if (MediaController.getInstance().getPlayingMessageObject() != null) {
                updateTrackData();
            }
        }
    }
}
