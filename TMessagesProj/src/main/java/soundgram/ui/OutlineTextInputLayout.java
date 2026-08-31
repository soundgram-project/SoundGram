package soundgram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

public class OutlineTextInputLayout extends FrameLayout {

    private EditTextBoldCursor editText;
    private TextView labelView;
    private Paint borderPaint;
    private RectF rect = new RectF();
    private boolean isFocused;

    public OutlineTextInputLayout(Context context) {
        super(context);
        setWillNotDraw(false);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);

        editText = new EditTextBoldCursor(context);
        editText.setBackgroundDrawable(null);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        editText.setCursorWidth(AndroidUtilities.dp(2));
        addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        labelView = new TextView(context);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        labelView.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
        addView(labelView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 12, 16, 12, 0));

        labelView.setPivotX(0);
        labelView.setPivotY(0);

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            isFocused = hasFocus;
            updateLabel(true);
            invalidate();
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateLabel(true);
            }
        });

        updateLabel(false);
    }

    public EditTextBoldCursor getEditText() {
        return editText;
    }

    public void setLabel(CharSequence text) {
        labelView.setText(text);
        updateLabel(false);
    }

    public void setText(CharSequence text) {
        editText.setText(text);
        updateLabel(false);
    }

    private void updateLabel(boolean animate) {
        boolean hasText = editText.getText().length() > 0;
        boolean active = isFocused || hasText;

        int textColor;
        if (isFocused) {
            textColor = Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated);
        } else {
            textColor = Theme.getColor(Theme.key_windowBackgroundWhiteHintText);
        }
        labelView.setTextColor(textColor);
        editText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated));

        float targetY = active ? -AndroidUtilities.dp(10) : AndroidUtilities.dp(16);
        float targetScale = active ? 0.8f : 1.0f;

        if (animate) {
            labelView.animate()
                    .translationY(targetY)
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .setDuration(150)
                    .start();
        } else {
            labelView.setTranslationY(targetY);
            labelView.setScaleX(targetScale);
            labelView.setScaleY(targetScale);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int bgColor = Theme.getColor(Theme.key_dialogBackground);
        if (bgColor == 0) {
            bgColor = Theme.getColor(Theme.key_windowBackgroundWhite);
        }
        labelView.setBackgroundColor(bgColor);

        int strokeColor = isFocused ? Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated) : Theme.getColor(Theme.key_windowBackgroundWhiteHintText);
        int strokeWidth = isFocused ? AndroidUtilities.dp(2) : AndroidUtilities.dp(1);

        borderPaint.setColor(strokeColor);
        borderPaint.setStrokeWidth(strokeWidth);

        float padding = strokeWidth / 2f;
        rect.set(padding, padding, getWidth() - padding, getHeight() - padding);
        canvas.drawRoundRect(rect, AndroidUtilities.dp(8), AndroidUtilities.dp(8), borderPaint);
    }
}
