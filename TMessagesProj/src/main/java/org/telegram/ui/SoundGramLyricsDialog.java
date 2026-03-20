package org.telegram.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SoundGramLyricsDialog extends Dialog {

    private ArrayList<LrcLine> lyricsLines = new ArrayList<>();
    private RecyclerListView listView;
    private LyricsAdapter adapter;
    private LinearLayoutManager layoutManager;

    private SeekBar progressBar;

    private Runnable updateRunnable;
    private Runnable resumeAutoScrollRunnable;

    private boolean isUpdating = false;
    private boolean isManualScrolling = false;
    private int currentActiveIndex = -1;
    private MessageObject playingTrack;

    public static class LrcLine implements Comparable<LrcLine> {
        public long timeMs;
        public String text;
        public LrcLine(long timeMs, String text) { this.timeMs = timeMs; this.text = text; }
        @Override public int compareTo(LrcLine o) { return Long.compare(this.timeMs, o.timeMs); }
    }

    public SoundGramLyricsDialog(Context context, MessageObject track) {
        super(context);
        this.playingTrack = track;
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(24), Theme.getColor(Theme.key_dialogBackground)));
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        TextView titleText = new TextView(context);
        titleText.setText(track.getMusicTitle());
        titleText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleText.setTextSize(20);
        titleText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleText.setSingleLine(true);
        titleText.setEllipsize(TextUtils.TruncateAt.END);
        titleText.setGravity(Gravity.CENTER);
        root.addView(titleText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView artistText = new TextView(context);
        artistText.setText(track.getMusicAuthor());
        artistText.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        artistText.setTextSize(16);
        artistText.setSingleLine(true);
        artistText.setEllipsize(TextUtils.TruncateAt.END);
        artistText.setGravity(Gravity.CENTER);
        root.addView(artistText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        listView = new RecyclerListView(context);
        listView.setSelectorDrawableColor(0);
        listView.setListSelectorColor(Color.TRANSPARENT);
        listView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // ВАЖНО: Отключаем FadingEdge, именно он создавал грязный эффект тени в центре
        listView.setVerticalFadingEdgeEnabled(false);
        listView.setClipToPadding(false);

        layoutManager = new LinearLayoutManager(context);
        listView.setLayoutManager(layoutManager);
        adapter = new LyricsAdapter();
        listView.setAdapter(adapter);

        listView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int height = listView.getMeasuredHeight();
                if (height > 0) {
                    int verticalPadding = (height / 2) - AndroidUtilities.dp(30);
                    listView.setPadding(0, verticalPadding, 0, verticalPadding);
                    listView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });

        root.addView(listView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 0, 1.0f, 0, 0, 0, 16));

        resumeAutoScrollRunnable = () -> {
            isManualScrolling = false;
            adapter.notifyDataSetChanged();
            if (currentActiveIndex != -1) smoothScrollToCenter(currentActiveIndex);
        };

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    isManualScrolling = true;
                    AndroidUtilities.cancelRunOnUIThread(resumeAutoScrollRunnable);
                    adapter.notifyDataSetChanged();
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    AndroidUtilities.runOnUIThread(resumeAutoScrollRunnable, 3000);
                }
            }
        });

        listView.setOnItemClickListener((view, position) -> {
            if (position >= 0 && position < lyricsLines.size()) {
                LrcLine line = lyricsLines.get(position);
                float progress = (float) (line.timeMs / (playingTrack.getDuration() * 1000f));
                MediaController.getInstance().seekToProgress(playingTrack, progress);

                isManualScrolling = false;
                AndroidUtilities.cancelRunOnUIThread(resumeAutoScrollRunnable);
                adapter.notifyDataSetChanged();
            }
        });

        progressBar = new SeekBar(context);
        progressBar.setMax(1000);
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                if (fromUser) MediaController.getInstance().seekToProgress(playingTrack, p / 1000f);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {} @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        root.addView(progressBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setContentView(root);
        fetchLyrics(track.getMusicTitle(), track.getMusicAuthor());

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isUpdating) return;
                MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
                if (playing != null) {
                    if (playingTrack == null || playing.getId() != playingTrack.getId()) {
                        playingTrack = playing;
                        titleText.setText(playingTrack.getMusicTitle());
                        artistText.setText(playingTrack.getMusicAuthor());
                        lyricsLines.clear();
                        adapter.notifyDataSetChanged();
                        fetchLyrics(playingTrack.getMusicTitle(), playingTrack.getMusicAuthor());
                    }

                    progressBar.setProgress((int) (playing.audioProgress * 1000));

                    if (!lyricsLines.isEmpty()) {
                        long currentMs = (long) (playing.audioProgress * playing.getDuration() * 1000);
                        updateHighlight(currentMs);
                    }
                }
                AndroidUtilities.runOnUIThread(this, 100);
            }
        };
    }

    private void smoothScrollToCenter(int position) {
        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
            @Override
            public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
                return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
            }
            @Override
            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                return 100f / displayMetrics.densityDpi;
            }
        };
        smoothScroller.setTargetPosition(position);
        layoutManager.startSmoothScroll(smoothScroller);
    }

    private void updateHighlight(long currentMs) {
        int newActiveIndex = -1;
        for (int i = lyricsLines.size() - 1; i >= 0; i--) {
            if (currentMs >= lyricsLines.get(i).timeMs) {
                newActiveIndex = i;
                break;
            }
        }
        if (newActiveIndex != currentActiveIndex) {
            currentActiveIndex = newActiveIndex;
            adapter.notifyDataSetChanged();
            if (!isManualScrolling && currentActiveIndex != -1) {
                smoothScrollToCenter(currentActiveIndex);
            }
        }
    }

    private class LyricsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override public int getItemCount() { return lyricsLines.size(); }

        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(getContext());
            tv.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
            return new RecyclerListView.Holder(tv);
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TextView tv = (TextView) holder.itemView;
            tv.setText(lyricsLines.get(position).text);

            if (isManualScrolling) {
                tv.setTextColor(0xFFFFFFFF);
                tv.setAlpha(0.6f);
                tv.setTextSize(18);
                tv.setTypeface(null);
            } else {
                if (position == currentActiveIndex) {
                    // АКТИВНАЯ СТРОКА: Яркая, крупная (как в Yandex Music)
                    tv.setTextColor(0xFFFFFFFF);
                    tv.setTextSize(22);
                    tv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    tv.setAlpha(1.0f);
                } else {
                    // ОСТАЛЬНЫЕ СТРОКИ: Затемненные, без расчетов дистанции
                    tv.setTextColor(0xFFFFFFFF);
                    tv.setTextSize(18);
                    tv.setTypeface(null);
                    tv.setAlpha(0.35f);
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.90);
            int height = (int) ((metrics.heightPixels * 0.85) / 1.3);
            window.setLayout(width, height);
            window.setGravity(Gravity.CENTER);
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        isUpdating = false;
        AndroidUtilities.cancelRunOnUIThread(updateRunnable);
        AndroidUtilities.cancelRunOnUIThread(resumeAutoScrollRunnable);
    }

    private void fetchLyrics(String title, String artist) {
        new Thread(() -> {
            try {
                String query = title + " " + artist;
                URL url = new URL("https://lrclib.net/api/search?q=" + URLEncoder.encode(query, "UTF-8"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "SoundGram-Android");

                if (conn.getResponseCode() == 200) {
                    InputStream is = conn.getInputStream();
                    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                    String response = s.hasNext() ? s.next() : "";

                    String syncedLyrics = "";
                    if (response.startsWith("[")) {
                        org.json.JSONArray array = new org.json.JSONArray(response);
                        if (array.length() > 0) {
                            syncedLyrics = array.getJSONObject(0).optString("syncedLyrics", "");
                        }
                    }

                    if (!TextUtils.isEmpty(syncedLyrics)) {
                        parseLrc(syncedLyrics);
                    } else {
                        AndroidUtilities.runOnUIThread(() -> showNoLyrics());
                    }
                } else {
                    AndroidUtilities.runOnUIThread(() -> showNoLyrics());
                }
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> showNoLyrics());
            }
        }).start();
    }

    private void parseLrc(String lrc) {
        Pattern pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)");
        String[] lines = lrc.split("\n");
        for (String line : lines) {
            Matcher m = pattern.matcher(line);
            if (m.find()) {
                long min = Long.parseLong(m.group(1));
                long sec = Long.parseLong(m.group(2));
                long mil = Long.parseLong(m.group(3));
                if (m.group(3).length() == 2) mil *= 10;
                long timeMs = (min * 60 * 1000) + (sec * 1000) + mil;
                String text = m.group(4).trim();
                if (!TextUtils.isEmpty(text)) {
                    lyricsLines.add(new LrcLine(timeMs, text));
                }
            }
        }
        Collections.sort(lyricsLines);

        AndroidUtilities.runOnUIThread(() -> {
            adapter.notifyDataSetChanged();
            isUpdating = true;
            AndroidUtilities.runOnUIThread(updateRunnable);
        });
    }

    private void showNoLyrics() {
        lyricsLines.clear();
        lyricsLines.add(new LrcLine(0, "Синхронный текст не найден 😔"));
        AndroidUtilities.runOnUIThread(() -> adapter.notifyDataSetChanged());
    }
}