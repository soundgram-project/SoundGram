package soundgram.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;

/**
 * RadioSectionStripView:
 * Three text tabs in one line («моя волна», «библиотека», «альбомы»), 14 px,
 * active #EEEEEE / 500, inactive #8A8A8A, no background and no borders.
 * Below the tabs is an analog radio dial with 61 tick marks (every 10th is higher),
 * with a sliding accent arrow 2x12 px (#C8F169) moving to the center of the active tab (200 ms ease-out).
 */
public class RadioSectionStripView extends FrameLayout {

    public interface OnTabSelectedListener {
        void onTabSelected(int index);
    }

    private final TextView[] tabViews = new TextView[2];
    private final String[] tabTitles = {"Моя волна", "Библиотека"};

    private int selectedTab = 1; // Default: Библиотека
    private float arrowPositionX = -1f;
    private ValueAnimator arrowAnimator;
    private OnTabSelectedListener listener;

    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrowPath = new Path();

    private static final int NUM_TICKS = 61;

    public RadioSectionStripView(Context context) {
        super(context);
        setWillNotDraw(false);

        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(AndroidUtilities.dp(1));

        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setColor(SoundGramDesignTokens.COLOR_ACCENT);

        LinearLayout tabsRow = new LinearLayout(context);
        tabsRow.setOrientation(LinearLayout.HORIZONTAL);
        tabsRow.setGravity(Gravity.CENTER_VERTICAL);
        tabsRow.setWeightSum(2f);

        for (int i = 0; i < 2; i++) {
            final int index = i;
            TextView tv = new TextView(context);
            tv.setText(tabTitles[i]);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            tv.setGravity(Gravity.CENTER);
            tv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            tv.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(6));
            tv.setOnClickListener(v -> setSelectedTab(index, true));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
            tabsRow.addView(tv, lp);
            tabViews[i] = tv;
        }

        addView(tabsRow, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        updateTabStyles();
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.listener = listener;
    }

    public int getSelectedTab() {
        return selectedTab;
    }

    public void setSelectedTab(int index, boolean animate) {
        if (index < 0 || index >= 2) return;
        selectedTab = index;
        updateTabStyles();

        float targetX = getTabCenterX(index);
        if (targetX <= 0) {
            post(() -> setSelectedTab(index, false));
            return;
        }

        if (arrowAnimator != null) {
            arrowAnimator.cancel();
        }

        if (animate && arrowPositionX > 0) {
            arrowAnimator = ValueAnimator.ofFloat(arrowPositionX, targetX);
            arrowAnimator.setDuration(200);
            arrowAnimator.setInterpolator(new DecelerateInterpolator());
            arrowAnimator.addUpdateListener(animation -> {
                arrowPositionX = (float) animation.getAnimatedValue();
                invalidate();
            });
            arrowAnimator.start();
        } else {
            arrowPositionX = targetX;
            invalidate();
        }

        if (listener != null) {
            listener.onTabSelected(index);
        }
    }

    public void setPageOffset(int position, float offset) {
        if (position < 0 || position > 1) return;
        if (position == 1) {
            float x = getTabCenterX(1);
            if (x > 0) {
                arrowPositionX = x;
                if (selectedTab != 1) {
                    selectedTab = 1;
                    updateTabStyles();
                }
                invalidate();
            }
            return;
        }
        float x1 = getTabCenterX(0);
        float x2 = getTabCenterX(1);
        if (x1 > 0 && x2 > 0) {
            arrowPositionX = x1 + (x2 - x1) * offset;
            int newSelected = offset >= 0.5f ? 1 : 0;
            if (newSelected != selectedTab) {
                selectedTab = newSelected;
                updateTabStyles();
            }
            invalidate();
        }
    }

    private float getTabCenterX(int index) {
        if (index < 0 || index >= 2 || tabViews[index] == null) return -1f;
        TextView tv = tabViews[index];
        return tv.getLeft() + tv.getWidth() / 2f;
    }

    private void updateTabStyles() {
        for (int i = 0; i < 2; i++) {
            if (tabViews[i] == null) continue;
            boolean active = (i == selectedTab);
            tabViews[i].setTextColor(active ? SoundGramDesignTokens.COLOR_TEXT : SoundGramDesignTokens.COLOR_TEXT_MUTED);
            tabViews[i].setAlpha(active ? 1.0f : 0.75f);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (arrowPositionX <= 0) {
            arrowPositionX = getTabCenterX(selectedTab);
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Dial scale baseline Y (at the bottom)
        float lineY = h - AndroidUtilities.dp(14);
        float paddingX = AndroidUtilities.dp(18);
        float usableWidth = w - paddingX * 2;
        float step = usableWidth / (NUM_TICKS - 1);

        // Draw 61 tick marks
        for (int i = 0; i < NUM_TICKS; i++) {
            float x = paddingX + i * step;
            boolean isMajor = (i % 10 == 0);
            float tickHeight = isMajor ? AndroidUtilities.dp(7) : AndroidUtilities.dp(3.5f);
            tickPaint.setColor(isMajor ? SoundGramDesignTokens.COLOR_LINE_STRONG : SoundGramDesignTokens.COLOR_LINE);
            canvas.drawLine(x, lineY, x, lineY + tickHeight, tickPaint);
        }

        // Draw sliding accent arrow (2x12 px indicator pointing down/up)
        if (arrowPositionX <= 0) {
            arrowPositionX = getTabCenterX(selectedTab);
        }
        if (arrowPositionX > 0) {
            float arrowW = AndroidUtilities.dp(2);
            float arrowH = AndroidUtilities.dp(12);
            float arrowTop = lineY - AndroidUtilities.dp(1);
            // Draw rectangle needle
            canvas.drawRect(
                    arrowPositionX - arrowW / 2f,
                    arrowTop,
                    arrowPositionX + arrowW / 2f,
                    arrowTop + arrowH,
                    arrowPaint
            );
        }
    }
}
