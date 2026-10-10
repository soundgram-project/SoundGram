package soundgram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;

/**
 * RadioWaveProgressView:
 * 60 vertical bars, gap 3 px, radius 2 dp.
 * Played part is accent (#C8F169).
 * Current playing bar is pure white (#FFFFFF) and 8 px taller.
 * Remaining bars are line-strong (#3A3A3A).
 * Tap and drag to seek.
 */
public class RadioWaveProgressView extends View {

    public interface OnSeekListener {
        void onSeek(float progress, boolean isFinal);
    }

    private static final int BARS_COUNT = 60;
    private final float[] amplitudes = new float[BARS_COUNT];
    private float progress = 0.0f;
    private boolean isDragging = false;
    private OnSeekListener seekListener;

    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint currentBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint unplayedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();

    public RadioWaveProgressView(Context context) {
        super(context);
        init();
    }

    private void init() {
        accentPaint.setColor(SoundGramDesignTokens.getAccentColor());
        accentPaint.setStyle(Paint.Style.FILL);

        currentBarPaint.setColor(SoundGramDesignTokens.getTextColor());
        currentBarPaint.setStyle(Paint.Style.FILL);

        unplayedPaint.setColor(SoundGramDesignTokens.getPlaylistCardColor());
        unplayedPaint.setStyle(Paint.Style.FILL);

        // Generate smooth default musical wave pattern
        generateDefaultAmplitudes();
    }

    public void updateColors() {
        accentPaint.setColor(SoundGramDesignTokens.getAccentColor());
        currentBarPaint.setColor(SoundGramDesignTokens.getTextColor());
        unplayedPaint.setColor(SoundGramDesignTokens.getPlaylistCardColor());
        invalidate();
    }

    private void generateDefaultAmplitudes() {
        for (int i = 0; i < BARS_COUNT; i++) {
            double angle = (i / (double) BARS_COUNT) * Math.PI * 4;
            double wave1 = Math.sin(angle);
            double wave2 = Math.cos(angle * 1.7 + 0.5);
            double combined = (Math.abs(wave1 * 0.6 + wave2 * 0.4) + 0.25);
            amplitudes[i] = (float) Math.min(1.0, Math.max(0.18, combined));
        }
    }

    public void setWaveformAmplitudes(byte[] waveData) {
        if (waveData == null || waveData.length == 0) {
            generateDefaultAmplitudes();
            invalidate();
            return;
        }
        for (int i = 0; i < BARS_COUNT; i++) {
            int srcIdx = (int) ((i / (float) BARS_COUNT) * waveData.length);
            srcIdx = Math.min(srcIdx, waveData.length - 1);
            int val = waveData[srcIdx] & 0xFF;
            amplitudes[i] = Math.max(0.15f, Math.min(1.0f, val / 31.0f));
        }
        invalidate();
    }

    public void setProgress(float p) {
        if (!isDragging) {
            this.progress = Math.max(0f, Math.min(1f, p));
            invalidate();
        }
    }

    public float getProgress() {
        return progress;
    }

    public void setOnSeekListener(OnSeekListener listener) {
        this.seekListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float w = getWidth();
        if (w <= 0) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = true;
                progress = Math.max(0f, Math.min(1f, x / w));
                if (seekListener != null) {
                    seekListener.onSeek(progress, false);
                }
                invalidate();
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            case MotionEvent.ACTION_MOVE:
                progress = Math.max(0f, Math.min(1f, x / w));
                if (seekListener != null) {
                    seekListener.onSeek(progress, false);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                progress = Math.max(0f, Math.min(1f, x / w));
                if (seekListener != null) {
                    seekListener.onSeek(progress, true);
                }
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        accentPaint.setColor(SoundGramDesignTokens.getAccentColor());
        currentBarPaint.setColor(SoundGramDesignTokens.getTextColor());
        unplayedPaint.setColor(SoundGramDesignTokens.getLineStrongColor());

        float gap = AndroidUtilities.dp(3);
        float totalGaps = gap * (BARS_COUNT - 1);
        float barWidth = Math.max(1f, (w - totalGaps) / BARS_COUNT);
        float cornerRadius = AndroidUtilities.dp(2);
        float extraHeightCurrent = AndroidUtilities.dp(8);

        int currentIndex = Math.min(BARS_COUNT - 1, (int) (progress * BARS_COUNT));

        float centerY = h / 2f;
        float maxHalfHeight = (h - extraHeightCurrent) / 2f;

        for (int i = 0; i < BARS_COUNT; i++) {
            float left = i * (barWidth + gap);
            float right = left + barWidth;

            float amp = amplitudes[i];
            float halfH = Math.max(AndroidUtilities.dp(3), amp * maxHalfHeight);

            boolean isCurrent = (i == currentIndex);
            boolean isPlayed = (i < currentIndex);

            if (isCurrent) {
                halfH += extraHeightCurrent / 2f;
            }

            float top = centerY - halfH;
            float bottom = centerY + halfH;

            barRect.set(left, top, right, bottom);

            Paint paintToUse;
            if (isCurrent) {
                paintToUse = currentBarPaint;
            } else if (isPlayed) {
                paintToUse = accentPaint;
            } else {
                paintToUse = unplayedPaint;
            }

            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, paintToUse);
        }
    }
}
