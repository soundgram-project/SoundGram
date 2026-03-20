
package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SoundGramLyricsAlert extends BottomSheet {

    private ArrayList<LrcLine> lyricsLines = new ArrayList<>();
    private LinearLayout lyricsContainer;
    private Runnable updateRunnable;
    private boolean isUpdating = false;

    public static class LrcLine implements Comparable<LrcLine> {
        public long timeMs;
        public String text;
        public TextView textView;
        public LrcLine(long timeMs, String text) { this.timeMs = timeMs; this.text = text; }
        @Override public int compareTo(LrcLine o) { return Long.compare(this.timeMs, o.timeMs); }
    }

    public SoundGramLyricsAlert(Context context, MessageObject track) {
        super(context, false);

        ScrollView scroll = new ScrollView(context);
        lyricsContainer = new LinearLayout(context);
        lyricsContainer.setOrientation(LinearLayout.VERTICAL);
        lyricsContainer.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground));
        lyricsContainer.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24));
        scroll.addView(lyricsContainer);

        TextView title = new TextView(context);
        title.setText("Загрузка текста...");
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        lyricsContainer.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 24));

        setCustomView(scroll);
        fetchLyrics(track.getMusicTitle(), track.getMusicAuthor());

        // Караоке апдейтер
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isUpdating) return;
                MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
                if (playing != null && !lyricsLines.isEmpty()) {
                    long currentMs = (long) (playing.audioProgress * playing.getDuration() * 1000);
                    updateHighlight(currentMs);
                }
                AndroidUtilities.runOnUIThread(this, 300);
            }
        };
    }

    private void updateHighlight(long currentMs) {
        int activeIndex = -1;
        for (int i = lyricsLines.size() - 1; i >= 0; i--) {
            if (currentMs >= lyricsLines.get(i).timeMs) {
                activeIndex = i;
                break;
            }
        }
        for (int i = 0; i < lyricsLines.size(); i++) {
            TextView tv = lyricsLines.get(i).textView;
            if (tv != null) {
                if (i == activeIndex) {
                    tv.setTextColor(0xFF1DB954); // Зеленый Spotify
                    tv.setTextSize(20);
                    tv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                } else {
                    tv.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
                    tv.setTextSize(18);
                    tv.setTypeface(null);
                }
            }
        }
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
                    
                    // Парсим JSON Array (берем первый результат)
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
            lyricsContainer.removeAllViews();
            for (LrcLine line : lyricsLines) {
                TextView tv = new TextView(getContext());
                tv.setText(line.text);
                tv.setTextSize(18);
                tv.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
                line.textView = tv;
                lyricsContainer.addView(tv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            }
            isUpdating = true;
            AndroidUtilities.runOnUIThread(updateRunnable);
        });
    }

    private void showNoLyrics() {
        lyricsContainer.removeAllViews();
        TextView title = new TextView(getContext());
        title.setText("Синхронный текст не найден 😔");
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        lyricsContainer.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    @Override
    public void dismiss() {
        super.dismiss();
        isUpdating = false;
        AndroidUtilities.cancelRunOnUIThread(updateRunnable);
    }
}
