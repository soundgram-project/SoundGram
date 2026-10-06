package soundgram.ui;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;

/**
 * CollapsibleSectionHeaderView:
 * Block section header with collapse/expand toggle.
 * Left: monospace title («плейлисты · 8» or «альбомы · 3»).
 * Right: accent monospace «свернуть ↑» or «развернуть ↓».
 * Entire row is clickable.
 */
public class CollapsibleSectionHeaderView extends LinearLayout {

    public interface OnToggleListener {
        void onToggle(boolean isCollapsed);
    }

    private final TextView titleTextView;
    private final TextView toggleTextView;
    private boolean isCollapsed = false;
    private OnToggleListener listener;

    public CollapsibleSectionHeaderView(Context context, String titleText, boolean defaultCollapsed) {
        super(context);
        this.isCollapsed = defaultCollapsed;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFF1B1B1B);
        bg.setCornerRadius(AndroidUtilities.dp(12));
        bg.setStroke(AndroidUtilities.dp(1), 0xFF2E2E2E);
        setBackground(bg);

        titleTextView = new TextView(context);
        titleTextView.setText(titleText);
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        titleTextView.setTextColor(SoundGramDesignTokens.COLOR_TEXT_MUTED);
        titleTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmono.ttf"));
        titleTextView.setLetterSpacing(0.02f);

        toggleTextView = new TextView(context);
        toggleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        toggleTextView.setTextColor(SoundGramDesignTokens.COLOR_ACCENT);
        toggleTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmono.ttf"));
        toggleTextView.setLetterSpacing(0.02f);

        updateToggleText();

        addView(titleTextView, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        addView(toggleTextView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        setOnClickListener(v -> {
            isCollapsed = !isCollapsed;
            updateToggleText();
            if (listener != null) {
                listener.onToggle(isCollapsed);
            }
        });
    }

    private TextView actionButton;

    public void setActionButton(String text, Runnable onAction) {
        if (actionButton == null) {
            actionButton = new TextView(getContext());
            actionButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            actionButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmono.ttf") != null ? AndroidUtilities.getTypeface("fonts/rmono.ttf") : android.graphics.Typeface.MONOSPACE);
            actionButton.setTextColor(SoundGramDesignTokens.COLOR_ON_ACCENT);
            actionButton.setGravity(Gravity.CENTER);
            actionButton.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
            android.graphics.drawable.GradientDrawable actBg = new android.graphics.drawable.GradientDrawable();
            actBg.setColor(SoundGramDesignTokens.COLOR_ACCENT);
            actBg.setCornerRadius(AndroidUtilities.dp(6));
            actionButton.setBackground(actBg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.rightMargin = AndroidUtilities.dp(10);
            addView(actionButton, 1, lp);
        }
        actionButton.setText(text);
        actionButton.setOnClickListener(v -> {
            if (onAction != null) {
                onAction.run();
            }
        });
    }

    public void setTitle(String text) {
        titleTextView.setText(text);
    }

    public void setCollapsed(boolean collapsed) {
        this.isCollapsed = collapsed;
        updateToggleText();
    }

    public boolean isCollapsed() {
        return isCollapsed;
    }

    public void setOnToggleListener(OnToggleListener listener) {
        this.listener = listener;
    }

    private void updateToggleText() {
        toggleTextView.setText(isCollapsed ? "развернуть ∨" : "свернуть ∧");
    }
}
