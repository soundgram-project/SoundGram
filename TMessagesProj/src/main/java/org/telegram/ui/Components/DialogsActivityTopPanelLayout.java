package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;

import me.vkryl.android.animator.ListAnimator;

public class DialogsActivityTopPanelLayout extends AnimatedLinearLayout {
    public DialogsActivityTopPanelLayout(@NonNull Context context) {
        super(context);

        setOrientation(LinearLayout.VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);
        updateColors();
    }

    BlurredBackgroundDrawable backgroundDrawable;

    public void setBlurredBackground(BlurredBackgroundDrawable background) {
        backgroundDrawable = background;
    }

    private float extraHeight;

    public void setExtraHeight(float extraHeight) {
        if (this.extraHeight != extraHeight) {
            this.extraHeight = extraHeight;
            checkBoundsAndClipping();
            invalidate();
        }
    }

    public float getExtraHeight() {
        return extraHeight;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        checkBoundsAndClipping();
    }

    @Override
    protected void onItemsChanged() {
        super.onItemsChanged();
        checkBoundsAndClipping();
        invalidate();
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        checkBoundsAndClipping();
        invalidate();
    }

    private boolean isIslandTouchTarget;

    public boolean isIslandTouch(MotionEvent ev) {
        if (isIslandTouchTarget) {
            return true;
        }
        if (callFragmentContextView == null || callFragmentContextView.getVisibility() != VISIBLE) {
            return false;
        }
        if (!callFragmentContextView.isPlayerExpanded() && callFragmentContextView.getExpandProgress() <= 0f) {
            return false;
        }
        float localX = ev.getX() - getX();
        float localY = ev.getY() - getY();

        float targetX = 0;
        float targetY = 0;
        View v = callFragmentContextView;
        while (v != null && v != this) {
            targetX += v.getX();
            targetY += v.getY();
            if (v.getParent() instanceof View) {
                v = (View) v.getParent();
            } else {
                break;
            }
        }
        float fx = localX - targetX;
        float fy = localY - targetY;
        float islandHeight = dp(116);
        return fx >= 0 && fx <= callFragmentContextView.getWidth() && fy >= 0 && fy <= islandHeight;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (callFragmentContextView != null && callFragmentContextView.getVisibility() == VISIBLE && (callFragmentContextView.isPlayerExpanded() || callFragmentContextView.getExpandProgress() > 0f)) {
            float targetX = 0;
            float targetY = 0;
            View v = callFragmentContextView;
            while (v != null && v != this) {
                targetX += v.getX();
                targetY += v.getY();
                if (v.getParent() instanceof View) {
                    v = (View) v.getParent();
                } else {
                    break;
                }
            }
            float x = ev.getX() - targetX;
            float y = ev.getY() - targetY;
            int action = ev.getActionMasked();
            float islandHeight = dp(116);
            if (action == MotionEvent.ACTION_DOWN) {
                isIslandTouchTarget = (x >= 0 && x <= callFragmentContextView.getWidth() && y >= 0 && y <= islandHeight);
            }
            if (isIslandTouchTarget) {
                MotionEvent event = MotionEvent.obtain(ev);
                event.setLocation(x, y);
                boolean handled = callFragmentContextView.dispatchTouchEvent(event);
                event.recycle();
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    isIslandTouchTarget = false;
                }
                return true;
            }
        } else {
            isIslandTouchTarget = false;
        }
        return super.dispatchTouchEvent(ev)
            || ev.getAction() == MotionEvent.ACTION_DOWN && backgroundDrawable != null && backgroundDrawable.getBounds().contains((int) ev.getX(), (int) ev.getY());
    }

    private final Path clipPath = new Path();
    private final RectF clipRectF = new RectF();

    private void checkBoundsAndClipping() {
        final float bgHeight = Math.max(0, getMetadata().getTotalHeight() + extraHeight);
        final float bgAlpha = getMetadata().getTotalVisibility();

        clipRectF.set(getPaddingLeft(), getPaddingTop(), getMeasuredWidth() - getPaddingRight(), getPaddingTop() + bgHeight);

        final float r = Math.min(dp(defaultRadiusDp), Math.min(clipRectF.width(), clipRectF.height()) / 2f);
        clipPath.rewind();
        clipPath.addRoundRect(clipRectF, r, r, Path.Direction.CW);

        if (backgroundDrawable != null) {
            backgroundDrawable.setAlpha((int) (bgAlpha * 255));
            backgroundDrawable.setBounds(dp(4), dp(14), getMeasuredWidth() - dp(4), getPaddingTop() + getPaddingBottom() + (int) bgHeight - dp(14));
            backgroundDrawable.setRadius(Math.min(dp(defaultRadiusDp), bgHeight / 2));
        }
    }

    private int defaultRadiusDp = 24;

    public void setDefaultRadiusDp(int defaultRadius) {
        this.defaultRadiusDp = defaultRadius;
    }

    public void updateColors() {
        if (backgroundDrawable != null) {
            backgroundDrawable.updateColors();
        }
        invalidate();
    }

    private FragmentContextView callFragmentContextView;

    public void setCallFragmentContextView(FragmentContextView fragmentContextView) {
        callFragmentContextView = fragmentContextView;
        callFragmentContextView.getCapsuleBlobDrawable().setCallback(this);
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return super.verifyDrawable(who) || callFragmentContextView != null && callFragmentContextView.getCapsuleBlobDrawable() == who;
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (getMetadata().getTotalVisibility() == 0) return;
        checkBoundsAndClipping();

        if (backgroundDrawable != null) {
            backgroundDrawable.draw(canvas);
        }

        View callDrawnView = null;
        if (callFragmentContextView != null) {
            final int style = callFragmentContextView.getCurrentStyle();
            if (style == FragmentContextView.STYLE_ACTIVE_GROUP_CALL || style == FragmentContextView.STYLE_CONNECTING_GROUP_CALL) {
                for (int a = 0, N = getEntriesCount(); a < N; a++) {
                    final ListAnimator.Entry<AnimatedLinearLayout.Holder> entry = getEntry(a);
                    final float top = getPaddingTop() + entry.getRectF().top;
                    final View view = entry.item.view;
                    final float alpha = entry.getVisibility();
                    if (alpha <= 0 || !isCallView(view)) {
                        continue;
                    }

                    final CapsuleBlobDrawable capsuleBlobDrawable = callFragmentContextView.getCapsuleBlobDrawable();
                    final int p = capsuleBlobDrawable.getRequiredInset();
                    final int h = dp(36) + p * 2;
                    capsuleBlobDrawable.setBounds(getPaddingLeft() - p, -p, getMeasuredWidth() - getPaddingRight() + p, -p + h);
                    capsuleBlobDrawable.setAlpha((int) (255 * alpha));
                    canvas.save();
                    canvas.translate(0, top);
                    capsuleBlobDrawable.draw(canvas);
                    canvas.restore();

                    callDrawnView = view;
                }
            } else if (callFragmentContextView.isPlayerExpanded() || callFragmentContextView.getExpandProgress() > 0) {
                for (int a = 0, N = getEntriesCount(); a < N; a++) {
                    final ListAnimator.Entry<AnimatedLinearLayout.Holder> entry = getEntry(a);
                    final View view = entry.item.view;
                    if (isCallView(view)) {
                        callDrawnView = view;
                        break;
                    }
                }
            }
        }

        canvas.save();
        canvas.clipPath(clipPath);
        for (int a = 0, N = getEntriesCount(); a < N; a++) {
            final ListAnimator.Entry<AnimatedLinearLayout.Holder> entry = getEntry(a);
            final float top = getPaddingTop() + entry.getRectF().top;
            final View view = entry.item.view;

            final float position = entry.getPosition();
            final float alpha = entry.getVisibility() * Math.min(1, position);

            if (alpha <= 0) {
                continue;
            }

            if (callDrawnView == view) {
                continue;
            }

            final int wasAlpha = Theme.dividerPaint.getAlpha();
            Theme.dividerPaint.setAlpha((int) (wasAlpha * alpha));
            final float offsetL = getPaddingLeft() + dp(16) * (1f - alpha);
            final float offsetR = getPaddingRight() + dp(16) * (1f - alpha);
            canvas.drawLine(offsetL, top, getWidth() - offsetR, top, Theme.dividerPaint);
            Theme.dividerPaint.setAlpha(wasAlpha);
        }

        exceptCall = callDrawnView != null;
        onlyCall = false;
        super.dispatchDraw(canvas);
        canvas.restore();

        if (callDrawnView != null) {
            onlyCall = true;
            exceptCall = false;
            super.dispatchDraw(canvas);
        }
    }



    private boolean exceptCall;
    private boolean onlyCall;

    @Override
    protected boolean drawChild(@NonNull Canvas canvas, View child, long drawingTime) {
        final boolean isCallView = isCallView(child);
        if (isCallView && exceptCall || !isCallView && onlyCall) {
            return false;
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    private boolean isCallView(View view) {
        return callFragmentContextView != null && (callFragmentContextView == view || callFragmentContextView.getParent() == view);
    }
}
