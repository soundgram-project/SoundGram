package soundgram.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import org.telegram.messenger.AndroidUtilities;

/**
 * RadioGenreScaleView:
 * Analog radio tuning tape for genres ordered by energy:
 * эмбиент, ло-фай, классика, джаз, блюз, инди, синтвейв, поп, к-поп, электроника,
 * хип-хоп, рэп, эмо-рэп, гиперпоп, рок, альтернатива, пост-панк, фонк, металл.
 * Fixed accent needle (#C8F169) in center, smooth sliding tape with divisions underneath,
 * snapping to nearest genre with haptic feedback.
 */
public class RadioGenreScaleView extends View {

    public interface OnGenreSelectedListener {
        void onGenreSelected(String genre, int index);
    }

    public static final String[] GENRES = {
            "эмбиент", "ло-фай", "классика", "джаз", "блюз",
            "инди", "синтвейв", "поп", "к-поп", "электроника",
            "хип-хоп", "рэп", "эмо-рэп", "гиперпоп", "рок",
            "альтернатива", "пост-панк", "фонк", "металл"
    };

    private static final int SUB_DIVISIONS = 4; // 4 small ticks between major genre ticks
    private float scrollOffset = 0f; // in pixels
    private float slotWidth = 0f;    // distance between genres

    private final Paint majorTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minorTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float lastTouchX;
    private VelocityTracker velocityTracker;
    private ValueAnimator snapAnimator;
    private int lastSnappedIndex = 5; // default "инди"
    private OnGenreSelectedListener listener;

    public RadioGenreScaleView(Context context) {
        super(context);
        init();
    }

    private void init() {
        slotWidth = AndroidUtilities.dp(84);

        majorTickPaint.setColor(SoundGramDesignTokens.COLOR_LINE_STRONG);
        majorTickPaint.setStrokeWidth(AndroidUtilities.dp(1.5f));

        minorTickPaint.setColor(SoundGramDesignTokens.COLOR_LINE);
        minorTickPaint.setStrokeWidth(AndroidUtilities.dp(1.0f));

        textPaint.setColor(SoundGramDesignTokens.COLOR_TEXT_MUTED);
        textPaint.setTextSize(AndroidUtilities.dp(11));
        textPaint.setTypeface(AndroidUtilities.getTypeface("fonts/rmono.ttf"));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setLetterSpacing(0.02f);

        needlePaint.setColor(SoundGramDesignTokens.COLOR_ACCENT);
        needlePaint.setStyle(Paint.Style.FILL);

        scrollOffset = lastSnappedIndex * slotWidth;
    }

    public void setOnGenreSelectedListener(OnGenreSelectedListener listener) {
        this.listener = listener;
    }

    public void setSelectedGenre(String genre) {
        if (genre == null) return;
        String g = genre.toLowerCase().trim();
        for (int i = 0; i < GENRES.length; i++) {
            if (GENRES[i].equalsIgnoreCase(g)) {
                snapToIndex(i, true);
                return;
            }
        }
    }

    public String getSelectedGenre() {
        int idx = Math.max(0, Math.min(GENRES.length - 1, Math.round(scrollOffset / slotWidth)));
        return GENRES[idx];
    }

    private void snapToIndex(int index, boolean animate) {
        if (index < 0 || index >= GENRES.length) return;
        float targetOffset = index * slotWidth;
        if (snapAnimator != null) {
            snapAnimator.cancel();
        }
        if (animate) {
            snapAnimator = ValueAnimator.ofFloat(scrollOffset, targetOffset);
            snapAnimator.setDuration(220);
            snapAnimator.setInterpolator(new DecelerateInterpolator());
            snapAnimator.addUpdateListener(animation -> {
                scrollOffset = (float) animation.getAnimatedValue();
                invalidate();
            });
            snapAnimator.start();
        } else {
            scrollOffset = targetOffset;
            invalidate();
        }
        if (lastSnappedIndex != index) {
            lastSnappedIndex = index;
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            if (listener != null) {
                listener.onGenreSelected(GENRES[index], index);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);

        float x = event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (snapAnimator != null) snapAnimator.cancel();
                lastTouchX = x;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = lastTouchX - x;
                scrollOffset += dx;
                float maxOffset = (GENRES.length - 1) * slotWidth;
                scrollOffset = Math.max(-AndroidUtilities.dp(40), Math.min(maxOffset + AndroidUtilities.dp(40), scrollOffset));
                lastTouchX = x;

                int curIdx = Math.round(scrollOffset / slotWidth);
                if (curIdx >= 0 && curIdx < GENRES.length && curIdx != lastSnappedIndex) {
                    lastSnappedIndex = curIdx;
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    if (listener != null) {
                        listener.onGenreSelected(GENRES[curIdx], curIdx);
                    }
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                velocityTracker.computeCurrentVelocity(1000);
                float vx = velocityTracker.getXVelocity();
                velocityTracker.recycle();
                velocityTracker = null;

                float projectedOffset = scrollOffset - (vx * 0.15f);
                int targetIdx = Math.max(0, Math.min(GENRES.length - 1, Math.round(projectedOffset / slotWidth)));
                snapToIndex(targetIdx, true);
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

        float centerX = w / 2f;
        float lineY = AndroidUtilities.dp(16);
        float majorTickH = AndroidUtilities.dp(16);
        float minorTickH = AndroidUtilities.dp(8);

        // Draw scale divisions
        for (int i = 0; i < GENRES.length; i++) {
            float genreX = centerX + (i * slotWidth - scrollOffset);

            // Draw minor ticks before next genre
            if (i < GENRES.length - 1) {
                float subStep = slotWidth / (SUB_DIVISIONS + 1);
                for (int s = 1; s <= SUB_DIVISIONS; s++) {
                    float subX = genreX + s * subStep;
                    if (subX >= 0 && subX <= w) {
                        canvas.drawLine(subX, lineY, subX, lineY + minorTickH, minorTickPaint);
                    }
                }
            }

            // Draw major genre tick
            if (genreX >= -slotWidth && genreX <= w + slotWidth) {
                canvas.drawLine(genreX, lineY, genreX, lineY + majorTickH, majorTickPaint);

                // Highlight genre text if close to center
                float dist = Math.abs(genreX - centerX);
                float alpha = Math.max(0.35f, 1f - (dist / (slotWidth * 1.5f)));
                boolean isCenter = dist < (slotWidth * 0.5f);

                textPaint.setColor(isCenter ? SoundGramDesignTokens.COLOR_TEXT : SoundGramDesignTokens.COLOR_TEXT_MUTED);
                textPaint.setAlpha((int) (alpha * 255));
                canvas.drawText(GENRES[i], genreX, lineY + majorTickH + AndroidUtilities.dp(16), textPaint);
            }
        }

        // Draw fixed center accent needle (2x18 px indicator)
        float needleW = AndroidUtilities.dp(2.5f);
        float needleH = AndroidUtilities.dp(20);
        canvas.drawRect(centerX - needleW / 2f, lineY - AndroidUtilities.dp(2), centerX + needleW / 2f, lineY + needleH, needlePaint);
    }
}
