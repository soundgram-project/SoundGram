package soundgram.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.ChatMessageSharedResources;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.ChatActionCell;
import org.telegram.ui.Components.ReactionsContainerLayout;
import org.telegram.ui.Components.CubicBezierInterpolator;

import java.lang.reflect.Field;
import java.util.ArrayList;



public class IosLikeSelectionHelper {

    public static View currentMessageCell = null;
    public static MotionEvent currentEvent = null;

    // Active popup animation state
    public static ActionBarPopupWindow activePopup = null;
    public static ChatActivity activeChatActivity = null;
    public static View activeOriginalMessageCell = null;
    public static View activeNewCell = null;
    public static ReactionsContainerLayout activeReactionsView = null;
    public static ViewGroup activeInnerContainer = null;
    public static ArrayList<Object[]> activeBottomMenuViews = null;
    public static Integer activeMessageTranslation = null;
    public static ArrayList<View> activeOriginalGroupCells = null;
    private static AnimatorSet outAnimation = null;

    private static long cellStopwatchStart = 0;
    private static long cellStopwatchDuration = 0;

    private static final int RADIUS = 250;

    private static boolean isIOSSelectionEnabled() {
        return org.telegram.messenger.ApplicationLoader.applicationContext.getSharedPreferences("soundgram_config", android.content.Context.MODE_PRIVATE).getBoolean("ios_like_selection", true);
    }

    public static void onTouchEvent(View cell, MotionEvent event) {
        if (isIOSSelectionEnabled()) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                currentMessageCell = cell;
                currentEvent = MotionEvent.obtain(event);
            }
        }
    }

    private static ChatMessageCell cloneChatMessageCell(ChatMessageCell original) {
        Context context = original.getContext();
        ChatMessageSharedResources sharedResources = null;
        try {
            Field f = ChatMessageCell.class.getDeclaredField("sharedResources");
            f.setAccessible(true);
            sharedResources = (ChatMessageSharedResources) f.get(original);
        } catch (Exception ignore) {}

        ChatMessageCell fakeCell = new ChatMessageCell(
            context,
            original.currentAccount,
            false,
            sharedResources,
            original.getResourcesProvider()
        );
        original.copyParamsTo(fakeCell);

        MessageObject mobj = original.getMessageObject();
        if (mobj != null) {
            if (mobj.type == MessageObject.TYPE_POLL) {
                if (original.pollButtons != null && !original.pollButtons.isEmpty()) {
                    fakeCell.firstVisiblePollButton = 0;
                    fakeCell.lastVisiblePollButton = original.pollButtons.size() - 1;
                }
            }
            fakeCell.setMessageObject(
                mobj,
                original.getCurrentMessagesGroup(),
                original.pinnedBottom,
                original.pinnedTop,
                original.firstInChat
            );
        }
        fakeCell.setDelegate(original.getDelegate());
        fakeCell.setTag("ios_like_selection");
        return fakeCell;
    }

    public static boolean hookShowAtLocation(ActionBarPopupWindow popup, View parent, int gravity, int x, int y) {
        if (!isIOSSelectionEnabled() || currentMessageCell == null) {
            return false;
        }

        BaseFragment lastFragment = LaunchActivity.getLastFragment();
        if (!(lastFragment instanceof ChatActivity)) {
            return false;
        }
        ChatActivity chatActivity = (ChatActivity) lastFragment;

        View oldContent = popup.getContentView();
        if (!(oldContent instanceof ViewGroup)) {
            return false;
        }
        ViewGroup oldContentView = (ViewGroup) oldContent;

        // Check if there is a ReactionsContainerLayout in the children to confirm it's a message menu
        boolean hasReactions = false;
        ArrayList<View> children = new ArrayList<>();
        for (int i = 0; i < oldContentView.getChildCount(); i++) {
            View child = oldContentView.getChildAt(i);
            children.add(child);
            if (child instanceof ReactionsContainerLayout) {
                hasReactions = true;
            } else if (child instanceof ViewGroup) {
                ViewGroup vgChild = (ViewGroup) child;
                for (int j = 0; j < vgChild.getChildCount(); j++) {
                    if (vgChild.getChildAt(j) instanceof ReactionsContainerLayout) {
                        hasReactions = true;
                        break;
                    }
                }
            }
        }

        if (!hasReactions) {
            return false;
        }

        // Clean up old content views so they can be re-parented
        oldContentView.removeAllViews();

        Context context = currentMessageCell.getContext();
        int displayWidth = context.getResources().getDisplayMetrics().widthPixels;
        int displayHeight = context.getResources().getDisplayMetrics().heightPixels;

        // 1. Create wrapper (FrameLayout filling screen)
        FrameLayout newContent = new FrameLayout(context);
        newContent.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        newContent.setClickable(true);
        newContent.setClipChildren(false);

        // 2. Create ScrollView
        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrollView.setFillViewport(true);
        scrollView.setClipChildren(false);
        scrollView.setClipToPadding(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.setVerticalScrollBarEnabled(false);

        // 3. Create Inner Container (LinearLayout vertical)
        LinearLayout innerContainer = new LinearLayout(context);
        innerContainer.setOrientation(LinearLayout.VERTICAL);
        innerContainer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Extract reactionsLayout
        ReactionsContainerLayout reactionsView = null;
        if (!children.isEmpty() && children.get(0) instanceof ReactionsContainerLayout) {
            reactionsView = (ReactionsContainerLayout) children.remove(0);
        }

        // 4. Visually clone the cell
        View newCell = null;
        int messageHeight = 0;
        int groupWidth = 0;

        activeOriginalGroupCells = new ArrayList<>();
        if (currentMessageCell instanceof ChatMessageCell) {
            ChatMessageCell original = (ChatMessageCell) currentMessageCell;
            MessageObject.GroupedMessages group = original.getCurrentMessagesGroup();

            if (group != null) {
                ViewGroup parentView = (ViewGroup) original.getParent();
                if (parentView != null) {
                    for (int i2 = 0; i2 < parentView.getChildCount(); i2++) {
                        View child = parentView.getChildAt(i2);
                        if (child instanceof ChatMessageCell) {
                            ChatMessageCell cell = (ChatMessageCell) child;
                            if (cell.getCurrentMessagesGroup() == group) {
                                activeOriginalGroupCells.add(cell);
                            }
                        }
                    }
                }
            }

            if (activeOriginalGroupCells.size() > 1) {
                int minX = Integer.MAX_VALUE;
                int minY = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int maxY = Integer.MIN_VALUE;
                for (View cell : activeOriginalGroupCells) {
                    int[] loc = new int[2];
                    cell.getLocationOnScreen(loc);
                    minX = Math.min(minX, loc[0]);
                    minY = Math.min(minY, loc[1]);
                    maxX = Math.max(maxX, loc[0] + cell.getMeasuredWidth());
                    maxY = Math.max(maxY, loc[1] + cell.getMeasuredHeight());
                }

                groupWidth = maxX - minX;
                messageHeight = maxY - minY;

                FrameLayout groupWrapper = new FrameLayout(context);
                groupWrapper.setClipChildren(false);
                groupWrapper.setClipToPadding(false);

                for (View cell : activeOriginalGroupCells) {
                    int[] loc = new int[2];
                    cell.getLocationOnScreen(loc);
                    int relX = loc[0] - minX;
                    int relY = loc[1] - minY;

                    ChatMessageCell fakeCell = cloneChatMessageCell((ChatMessageCell) cell);
                    fakeCell.measure(
                        View.MeasureSpec.makeMeasureSpec(cell.getMeasuredWidth(), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(cell.getMeasuredHeight(), View.MeasureSpec.EXACTLY)
                    );
                    fakeCell.layout(0, 0, cell.getMeasuredWidth(), cell.getMeasuredHeight());

                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(cell.getMeasuredWidth(), cell.getMeasuredHeight());
                    lp.leftMargin = relX;
                    lp.topMargin = relY;
                    groupWrapper.addView(fakeCell, lp);
                }

                newCell = groupWrapper;
            } else {
                activeOriginalGroupCells.clear();
                ChatMessageCell fakeCell = cloneChatMessageCell(original);
                newCell = fakeCell;
                messageHeight = original.getMeasuredHeight();
                groupWidth = original.getMeasuredWidth();
            }
        } else if (currentMessageCell instanceof ChatActionCell) {
            ChatActionCell original = (ChatActionCell) currentMessageCell;
            ChatActionCell fakeCell = new ChatActionCell(context);
            fakeCell.setMessageObject(original.getMessageObject(), true);
            fakeCell.setTag("ios_like_selection");
            newCell = fakeCell;
            messageHeight = original.getMeasuredHeight();
            groupWidth = original.getMeasuredWidth();
        }

        Boolean isOut = null;
        Boolean isChannel = null;
        if (currentMessageCell instanceof ChatMessageCell) {
            MessageObject mobj = ((ChatMessageCell) currentMessageCell).getMessageObject();
            if (mobj != null) {
                isOut = mobj.isOutOwner();
                isChannel = mobj.isFromChannel();
            }
        }

        int gravityAlignment = Gravity.CENTER;
        if (newCell != null) {
            if (currentMessageCell instanceof ChatMessageCell) {
                gravityAlignment = (isOut != null && isOut && (isChannel == null || !isChannel)) ? Gravity.RIGHT : Gravity.LEFT;
            }
        }

        // Measure fake cell
        if (newCell != null) {
            newCell.measure(
                View.MeasureSpec.makeMeasureSpec(groupWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(messageHeight, View.MeasureSpec.EXACTLY)
            );
            newCell.layout(0, 0, groupWidth, messageHeight);
        }

        // Extract and process bottom menu views
        ActionBarPopupWindow.ActionBarPopupWindowLayout popupActionsMenu = null;
        View popupActionsGroupedMenu = null;
        ArrayList<Object[]> bottomMenuViews = new ArrayList<>();

        int wSpec = View.MeasureSpec.makeMeasureSpec(displayWidth, View.MeasureSpec.AT_MOST);
        int hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

        for (View child : children) {
            if (child instanceof ActionBarPopupWindow.ActionBarPopupWindowLayout) {
                popupActionsMenu = (ActionBarPopupWindow.ActionBarPopupWindowLayout) child;
                popupActionsMenu.swipeBackGravityRight = (gravityAlignment == Gravity.RIGHT);
                
                org.telegram.ui.Components.PopupSwipeBackLayout swipeBack = popupActionsMenu.getSwipeBack();
                if (swipeBack != null) {
                    swipeBack.setOnTouchListener((v, e) -> {
                        int action = e.getAction();
                        if (action == MotionEvent.ACTION_DOWN) {
                            scrollView.requestDisallowInterceptTouchEvent(true);
                        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                            scrollView.requestDisallowInterceptTouchEvent(false);
                        }
                        return false;
                    });
                }
            } else if (child instanceof FrameLayout && ((FrameLayout) child).getChildCount() > 0) {
                popupActionsGroupedMenu = child;
            }

            if (newCell != null) {
                LinearLayout.LayoutParams childLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                childLp.gravity = gravityAlignment;
                child.setLayoutParams(childLp);
            }

            innerContainer.addView(child);
            child.measure(wSpec, hSpec);
        }

        int bottomMenuHeight = 0;
        for (View child : children) {
            String tag = null;
            if (child == popupActionsMenu) {
                tag = "actions";
            } else if (child == popupActionsGroupedMenu) {
                tag = "grouped";
            }
            int measuredH = child.getMeasuredHeight();
            bottomMenuViews.add(new Object[]{child, measuredH, tag});
            bottomMenuHeight += measuredH;
        }

        // Menu width calculations
        if (popupActionsGroupedMenu != null && popupActionsMenu != null) {
            int menuWidth = 0;
            org.telegram.ui.Components.PopupSwipeBackLayout swipeBack = popupActionsMenu.getSwipeBack();
            if (swipeBack != null && swipeBack.getChildCount() > 0) {
                View backgroundView = swipeBack.getChildAt(0);
                if (backgroundView != null) {
                    backgroundView.measure(wSpec, hSpec);
                    menuWidth = backgroundView.getMeasuredWidth();
                }
            }

            if (menuWidth <= 0) {
                popupActionsMenu.measure(wSpec, hSpec);
                menuWidth = popupActionsMenu.getMeasuredWidth();
            }

            menuWidth = Math.min(Math.max(menuWidth, AndroidUtilities.dp(200)), displayWidth - AndroidUtilities.dp(32));

            ViewGroup.LayoutParams menuLp = popupActionsMenu.getLayoutParams();
            if (menuLp != null) {
                menuLp.width = menuWidth;
                popupActionsMenu.setLayoutParams(menuLp);
            }

            ViewGroup.LayoutParams groupedLp = popupActionsGroupedMenu.getLayoutParams();
            if (groupedLp instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) groupedLp;
                llp.width = menuWidth;
                llp.topMargin = AndroidUtilities.dp(-8);
                popupActionsGroupedMenu.setLayoutParams(llp);
            } else if (groupedLp != null) {
                groupedLp.width = menuWidth;
                popupActionsGroupedMenu.setLayoutParams(groupedLp);
            }

            int exactWSpec = View.MeasureSpec.makeMeasureSpec(menuWidth, View.MeasureSpec.EXACTLY);
            popupActionsGroupedMenu.measure(exactWSpec, hSpec);
        }

        if (popupActionsMenu != null) {
            int color = popupActionsMenu.getBackgroundColor();
            int alpha = (color >> 24) & 0xFF;
            alpha = (int) (255 / 1.3);
            color = (alpha << 24) | (color & 0x00FFFFFF);
            popupActionsMenu.setBackgroundColor(color);
        }

        int reactionsHeight = 0;
        if (reactionsView != null) {
            reactionsView.measure(wSpec, hSpec);
            reactionsHeight = reactionsView.getMeasuredHeight() + AndroidUtilities.dp(8);
        }

        int originalMsgX = 0;
        int originalMsgY = 0;
        if (newCell != null) {
            if (activeOriginalGroupCells != null && activeOriginalGroupCells.size() > 1) {
                int minX = Integer.MAX_VALUE;
                int minY = Integer.MAX_VALUE;
                for (View cell : activeOriginalGroupCells) {
                    int[] loc = new int[2];
                    cell.getLocationOnScreen(loc);
                    minX = Math.min(minX, loc[0]);
                    minY = Math.min(minY, loc[1]);
                }
                originalMsgX = minX;
                originalMsgY = minY;
            } else {
                int[] loc = new int[2];
                currentMessageCell.getLocationOnScreen(loc);
                originalMsgX = loc[0];
                originalMsgY = loc[1];
            }
        } else {
            originalMsgY = y + reactionsHeight;
            originalMsgX = 0;
            innerContainer.setPadding(x, 0, 0, 0);
        }

        int topLimit = AndroidUtilities.statusBarHeight + AndroidUtilities.dp(24);
        int bottomLimit = AndroidUtilities.navigationBarHeight + AndroidUtilities.dp(12);

        int paddingTop = (displayHeight - messageHeight) / 2 - reactionsHeight;
        if (paddingTop < topLimit) {
            paddingTop = topLimit;
        }
        int menuHeight = reactionsHeight + bottomMenuHeight + messageHeight;

        if (reactionsView != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = gravityAlignment;
            lp.bottomMargin = AndroidUtilities.dp(8);
            innerContainer.addView(reactionsView, 0, lp);
        }

        if (paddingTop + menuHeight > displayHeight - bottomLimit) {
            scrollView.setPadding(0, 0, 0, bottomLimit);
        }

        View spacer = new View(context);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, paddingTop));
        spacer.setOnClickListener(v -> popup.dismiss());
        innerContainer.addView(spacer, 0);

        if (newCell != null) {
            LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cellLp.gravity = gravityAlignment;
            // Place it after the spacer (and reactionsView if present)
            int index = reactionsView != null ? 2 : 1;
            innerContainer.addView(newCell, index, cellLp);
        }

        scrollView.addView(innerContainer);
        newContent.addView(scrollView);

        boolean isMessage = currentMessageCell instanceof ChatMessageCell;
        if (isMessage && (gravityAlignment == Gravity.LEFT || gravityAlignment == Gravity.RIGHT) &&
                (popupActionsMenu != null || reactionsView != null || popupActionsGroupedMenu != null)) {
            ChatMessageCell chatMessageCell = (ChatMessageCell) currentMessageCell;
            int avatarWidth = chatMessageCell.needDrawAvatar() ? AndroidUtilities.dp(48) : 0;
            int pad = (gravityAlignment == Gravity.LEFT) ? (org.telegram.messenger.SharedConfig.bubbleRadius / 2) : AndroidUtilities.dp(8);
            int mar = (gravityAlignment == Gravity.LEFT) ?
                    (originalMsgX + avatarWidth + pad) :
                    (originalMsgX + chatMessageCell.getWidth() - chatMessageCell.getBoundsRight());

            int menuTranslation = 0;
            if (popupActionsMenu != null) {
                if (gravityAlignment == Gravity.LEFT) {
                    int width = popupActionsMenu.getMeasuredWidth();
                    if (mar + width + pad > displayWidth || mar < pad) {
                        menuTranslation = pad;
                    } else {
                        menuTranslation = mar;
                    }
                } else if (gravityAlignment == Gravity.RIGHT) {
                    menuTranslation = Math.min(displayWidth - pad, mar);
                }
            }

            View[] viewsToTranslate = new View[]{popupActionsMenu, reactionsView, popupActionsGroupedMenu};
            for (View view : viewsToTranslate) {
                if (view == null) {
                    continue;
                }

                int translation = 0;
                if (view == reactionsView) {
                    if (gravityAlignment == Gravity.LEFT) {
                        int width = view.getMeasuredWidth();
                        if (mar + width + pad > displayWidth || mar < pad) {
                            translation = pad;
                        } else {
                            translation = mar;
                        }
                    } else if (gravityAlignment == Gravity.RIGHT) {
                        translation = Math.min(displayWidth - pad, mar);
                    }

                    if (isOut != null) {
                        reactionsView.setMirrorX(isOut);

                        int messageLeft = originalMsgX + chatMessageCell.getBackgroundDrawableLeft();
                        int messageRight = originalMsgX + chatMessageCell.getBackgroundDrawableRight();

                        int messageTail = AndroidUtilities.dp(org.telegram.messenger.SharedConfig.bubbleRadius);
                        float bubbleMargin = (reactionsView.radius + AndroidUtilities.dp(8)) / 2f;
                        float minX = bubbleMargin;
                        float maxX = reactionsView.getMeasuredWidth() - bubbleMargin;

                        float bubbleOffset = 0;
                        if (isOut) {
                            float bubbleX = messageLeft + messageTail;
                            float layoutLeft = displayWidth - reactionsView.getMeasuredWidth();
                            float reactionsLeft = layoutLeft + translation;
                            bubbleOffset = bubbleX - reactionsLeft - reactionsView.bigCircleOffset;
                            bubbleOffset = Math.max(minX - reactionsView.bigCircleOffset, Math.min(bubbleOffset, maxX - reactionsView.bigCircleOffset));
                        } else {
                            float bubbleX = messageRight - messageTail;
                            float reactionsLeft = translation;
                            bubbleOffset = bubbleX - reactionsLeft - reactionsView.getMeasuredWidth() + reactionsView.bigCircleOffset;
                            bubbleOffset = Math.max(minX - reactionsView.getMeasuredWidth() + reactionsView.bigCircleOffset, Math.min(bubbleOffset, maxX - reactionsView.getMeasuredWidth() + reactionsView.bigCircleOffset));
                        }

                        reactionsView.setBubbleOffset(bubbleOffset);
                    }
                } else {
                    translation = menuTranslation;
                }

                if (translation != 0) {
                    view.setTranslationX(translation);
                }
            }
        }

        // Store active state for out animation
        activePopup = popup;
        activeChatActivity = chatActivity;
        activeOriginalMessageCell = currentMessageCell;
        activeNewCell = newCell;
        activeReactionsView = reactionsView;
        activeInnerContainer = innerContainer;
        activeBottomMenuViews = bottomMenuViews;

        int scrollTargetY = 0;
        if (paddingTop == topLimit) {
            scrollTargetY = (topLimit + reactionsHeight + messageHeight / 2) - displayHeight / 2;
        }
        final int finalScrollTargetY = scrollTargetY;

        // Calculate translation of cloned cell
        if (newCell != null) {
            int[] cellLoc = new int[2];
            final View finalNewCell = newCell;
            final int finalOriginalMsgY = originalMsgY;
            newCell.post(() -> {
                if (finalScrollTargetY > 0) {
                    scrollView.scrollTo(0, finalScrollTargetY);
                }
                finalNewCell.getLocationOnScreen(cellLoc);
                activeMessageTranslation = finalOriginalMsgY - cellLoc[1];
                // Animate entry
                AnimatorSet set = animate(false, popup, activeMessageTranslation, activeBottomMenuViews, activeChatActivity, activeOriginalMessageCell, activeReactionsView, activeNewCell, activeInnerContainer);
                if (set != null) {
                    set.start();
                }
            });
        } else {
            // Animate entry immediately
            AnimatorSet set = animate(false, popup, null, activeBottomMenuViews, activeChatActivity, activeOriginalMessageCell, activeReactionsView, activeNewCell, activeInnerContainer);
            if (set != null) {
                set.start();
            }
        }

        popup.setContentView(newContent);
        popup.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        popup.setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
        popup.setAnimationStyle(0); // Disable default animation
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        return true;
    }

    public static boolean hookDismiss(ActionBarPopupWindow popup) {
        if (!isIOSSelectionEnabled() || currentMessageCell == null) {
            return false;
        }

        if (popup != activePopup) {
            return false;
        }

        if (outAnimation != null && outAnimation.isRunning()) {
            return true;
        }

        View contentView = popup.getContentView();
        if (contentView != null) {
            contentView.setClickable(false);
        }
        if (activeInnerContainer != null) {
            activeInnerContainer.setClickable(false);
        }

        outAnimation = animate(true, popup, activeMessageTranslation, activeBottomMenuViews, activeChatActivity, activeOriginalMessageCell, activeReactionsView, activeNewCell, activeInnerContainer);
        if (outAnimation != null) {
            outAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    outAnimation = null;
                    clearActivePopupState();
                    popup.dismiss(false);
                }
            });
            outAnimation.start();
            return true;
        }

        clearActivePopupState();
        return false;
    }

    private static void clearActivePopupState() {
        activePopup = null;
        activeChatActivity = null;
        activeOriginalMessageCell = null;
        activeNewCell = null;
        activeReactionsView = null;
        activeInnerContainer = null;
        activeBottomMenuViews = null;
        activeMessageTranslation = null;
        activeOriginalGroupCells = null;
    }

    private static void applyBlur(View view, float radius) {
        if (Build.VERSION.SDK_INT >= 31) {
            if (radius <= 0) {
                view.setRenderEffect(null);
            } else {
                view.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(radius, radius, android.graphics.Shader.TileMode.MIRROR));
            }
        }
    }

    private static void altBlur(boolean out, ActionBarPopupWindow popup, ChatActivity chatActivity, int duration, ArrayList<Animator> animations, View newCell) {
        Drawable drawable = popup.getBackground();
        if (!out && (drawable == null || !(drawable instanceof BitmapDrawable)) && chatActivity != null && chatActivity.getFragmentView() != null) {
            View origCell = activeOriginalMessageCell;
            int oldVisibility = View.VISIBLE;
            ArrayList<Integer> oldVisibilities = new ArrayList<>();
            if (activeOriginalGroupCells != null && activeOriginalGroupCells.size() > 1) {
                for (View cell : activeOriginalGroupCells) {
                    oldVisibilities.add(cell.getVisibility());
                    cell.setVisibility(View.INVISIBLE);
                }
            } else if (origCell != null) {
                oldVisibility = origCell.getVisibility();
                origCell.setVisibility(View.INVISIBLE);
            }
            
            Bitmap bitmap = AndroidUtilities.makeBlurBitmap(chatActivity.getFragmentView(), 8f, 15);
            
            if (activeOriginalGroupCells != null && activeOriginalGroupCells.size() > 1) {
                for (int idx = 0; idx < activeOriginalGroupCells.size(); idx++) {
                    activeOriginalGroupCells.get(idx).setVisibility(oldVisibilities.get(idx));
                }
            } else if (origCell != null) {
                origCell.setVisibility(oldVisibility);
            }

            if (bitmap != null) {
                BitmapDrawable bitmapDrawable = new BitmapDrawable(chatActivity.getFragmentView().getResources(), bitmap);
                bitmapDrawable.mutate();
                bitmapDrawable.setColorFilter(new android.graphics.PorterDuffColorFilter(Color.parseColor("#50000000"), PorterDuff.Mode.SRC_ATOP));
                popup.setBackgroundDrawable(bitmapDrawable);
                
                int displayWidth = chatActivity.getFragmentView().getResources().getDisplayMetrics().widthPixels;
                setCellVisibility(true, activeOriginalMessageCell, displayWidth);
                if (newCell != null) {
                    newCell.post(() -> setCellVisibility(false, activeOriginalMessageCell, displayWidth));
                }
                drawable = bitmapDrawable;
            }
        } else if (out && drawable != null) {
            AndroidUtilities.runOnUIThread(() -> popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)), duration);
        }
        if (drawable != null) {
            animations.add(ObjectAnimator.ofInt(drawable, "alpha", out ? 255 : 0, out ? 0 : 255));
        }
    }

    private static void setCellVisibility(boolean visible, View originalCell, int displayWidth) {
        if (activeOriginalGroupCells != null && activeOriginalGroupCells.size() > 1) {
            for (View cell : activeOriginalGroupCells) {
                setSingleCellVisibility(visible, cell, displayWidth);
            }
        } else {
            setSingleCellVisibility(visible, originalCell, displayWidth);
        }
    }

    private static void setSingleCellVisibility(boolean visible, View cell, int displayWidth) {
        if (cell == null) return;
        if (visible) {
            cell.setVisibility(View.VISIBLE);
            if (cell instanceof ChatMessageCell) {
                ((ChatMessageCell) cell).updateTranslation();
            } else {
                cell.setTranslationX(0);
            }
        } else {
            cell.setVisibility(View.INVISIBLE);
        }
    }

    private static void addFadeAnimations(View view, float scale, boolean out, ArrayList<Animator> animations) {
        float startScale = out ? 1f : scale;
        float endScale = out ? scale : 1f;
        float startAlpha = out ? 1f : 0f;
        float endAlpha = out ? 0f : 1f;

        view.setPivotX(view.getMeasuredWidth() / 2f);
        if (view instanceof ReactionsContainerLayout) {
            view.setPivotY(view.getMeasuredHeight());
        }

        view.setScaleX(startScale);
        view.setScaleY(startScale);
        view.setAlpha(startAlpha);

        animations.add(ObjectAnimator.ofFloat(view, View.SCALE_X, startScale, endScale));
        animations.add(ObjectAnimator.ofFloat(view, View.SCALE_Y, startScale, endScale));
        animations.add(ObjectAnimator.ofFloat(view, View.ALPHA, startAlpha, endAlpha));
    }

    private static void addMessageScaleAnimations(View view, boolean out, ArrayList<Animator> animations) {
        float startScale = out ? 1.03f : 1.0f;
        float endScale = out ? 1.0f : 1.03f;

        view.setPivotX(view.getMeasuredWidth() / 2f);
        view.setPivotY(view.getMeasuredHeight() / 2f);

        view.setScaleX(startScale);
        view.setScaleY(startScale);

        animations.add(ObjectAnimator.ofFloat(view, View.SCALE_X, startScale, endScale));
        animations.add(ObjectAnimator.ofFloat(view, View.SCALE_Y, startScale, endScale));
    }

    public static AnimatorSet animate(
            boolean out,
            ActionBarPopupWindow popup,
            Integer messageTranslation,
            ArrayList<Object[]> bottomMenuViews,
            ChatActivity chatActivity,
            View originalMessageCell,
            ReactionsContainerLayout reactionsView,
            View newCell,
            ViewGroup innerContainer
    ) {
        if (chatActivity == null) return null;
        View contentView = chatActivity.getFragmentView();
        if (contentView == null) return null;

        int displayWidth = contentView.getResources().getDisplayMetrics().widthPixels;
        int duration = 280;
        boolean alt = Build.VERSION.SDK_INT < 31;

        ArrayList<Animator> animations = new ArrayList<>();

        if (!out) {
            setCellVisibility(false, originalMessageCell, displayWidth);
        }

        float radiusDp = AndroidUtilities.dp(15);
        if (!alt) {
            ValueAnimator blurAnim = ValueAnimator.ofFloat(out ? radiusDp : 0f, out ? 0f : radiusDp);
            blurAnim.addUpdateListener(animation -> applyBlur(contentView, (Float) animation.getAnimatedValue()));
            animations.add(blurAnim);

            ColorDrawable dimDrawable = new ColorDrawable(Color.BLACK);
            dimDrawable.setAlpha(0);
            popup.setBackgroundDrawable(dimDrawable);
            
            ValueAnimator dimAnim = ValueAnimator.ofInt(out ? 127 : 0, out ? 0 : 127);
            dimAnim.addUpdateListener(animation -> dimDrawable.setAlpha((Integer) animation.getAnimatedValue()));
            animations.add(dimAnim);
        } else {
            altBlur(out, popup, chatActivity, duration, animations, newCell);
        }

        if (messageTranslation != null) {
            float startY = out ? (innerContainer != null ? innerContainer.getTranslationY() : 0) : messageTranslation;
            float endY = 0;
            if (out) {
                if (originalMessageCell != null && newCell != null) {
                    int[] origLoc = new int[2];
                    originalMessageCell.getLocationOnScreen(origLoc);
                    int[] newLoc = new int[2];
                    newCell.getLocationOnScreen(newLoc);
                    endY = origLoc[1] - newLoc[1] + (innerContainer != null ? innerContainer.getTranslationY() : 0);
                } else {
                    endY = messageTranslation;
                }
            } else {
                endY = 0;
            }
            View target = innerContainer != null ? innerContainer : popup.getContentView();
            animations.add(ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, startY, endY));
        }

        if (newCell != null) {
            addMessageScaleAnimations(newCell, out, animations);
        }

        if (bottomMenuViews != null) {
            float pivotY = 0;
            for (Object[] item : bottomMenuViews) {
                View view = (View) item[0];
                int height = (Integer) item[1];
                String tag = (String) item[2];
                view.setPivotY(pivotY / 2f);
                pivotY -= height;
                addFadeAnimations(view, "grouped".equals(tag) ? 0.7f : 0.8f, out, animations);
            }
        }

        if (reactionsView != null) {
            addFadeAnimations(reactionsView, 0.8f, out, animations);
        }

        if (!out) {
            cellStopwatchStart = System.currentTimeMillis();
            if (newCell != null) {
                newCell.post(() -> {
                    cellStopwatchDuration = System.currentTimeMillis() - cellStopwatchStart;
                });
            }
        } else {
            long delay = Math.max(0, duration - cellStopwatchDuration - 50);
            AndroidUtilities.runOnUIThread(() -> {
                setCellVisibility(true, originalMessageCell, displayWidth);
            }, delay);
        }

        AnimatorSet set = new AnimatorSet();
        set.playTogether(animations);
        set.setDuration(duration);
        set.setInterpolator(out ? CubicBezierInterpolator.DEFAULT : CubicBezierInterpolator.EASE_OUT_QUINT);

        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (out) {
                    setCellVisibility(true, originalMessageCell, displayWidth);
                    if (originalMessageCell instanceof ChatMessageCell) {
                        org.telegram.ui.Components.RecyclerListView recycler = chatActivity.getChatListView();
                        if (recycler != null) {
                            int position = recycler.getChildAdapterPosition(originalMessageCell);
                            if (position >= 0) {
                                try {
                                    Field adapterField = ChatActivity.class.getDeclaredField("chatAdapter");
                                    adapterField.setAccessible(true);
                                    androidx.recyclerview.widget.RecyclerView.Adapter adapter = (androidx.recyclerview.widget.RecyclerView.Adapter) adapterField.get(chatActivity);
                                    if (adapter != null) {
                                        adapter.notifyItemChanged(position);
                                    }
                                } catch (Exception ignore) {}
                            }
                        }
                    }
                }
            }
        });

        return set;
    }
}
