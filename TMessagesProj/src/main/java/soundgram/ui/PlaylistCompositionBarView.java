package soundgram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;

/**
 * PlaylistCompositionBarView:
 * 3-segment source composition bar (accent #C8F169 for library, #BDBDBD for chats, #5C5C5C for channels),
 * height 8 px, with monospace labels underneath: «библиотека 32 · чаты 14 · каналы 10».
 */
public class PlaylistCompositionBarView extends LinearLayout {

    private int libraryCount = 0;
    private int chatsCount = 0;
    private int channelsCount = 0;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF segRect = new RectF();
    private final TextView labelsTextView;

    public PlaylistCompositionBarView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setWillNotDraw(false);

        labelsTextView = new TextView(context);
        labelsTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        labelsTextView.setTextColor(SoundGramDesignTokens.COLOR_TEXT_MUTED);
        labelsTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmono.ttf"));
        labelsTextView.setGravity(Gravity.START);
        labelsTextView.setLetterSpacing(0.02f);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.topMargin = AndroidUtilities.dp(16); // space for the 8px bar on top
        addView(labelsTextView, lp);

        updateLabels();
    }

    public void setCounts(int library, int chats, int channels) {
        this.libraryCount = Math.max(0, library);
        this.chatsCount = Math.max(0, chats);
        this.channelsCount = Math.max(0, channels);
        updateLabels();
        invalidate();
    }

    private void updateLabels() {
        labelsTextView.setText("библиотека " + libraryCount + " · чаты " + chatsCount + " · каналы " + channelsCount);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        if (w <= 0) return;

        float barH = AndroidUtilities.dp(8);
        float radius = AndroidUtilities.dp(4);
        float top = AndroidUtilities.dp(2);
        float bottom = top + barH;

        int total = libraryCount + chatsCount + channelsCount;
        if (total == 0) {
            // Default placeholder segment
            barPaint.setColor(SoundGramDesignTokens.COLOR_LINE_STRONG);
            segRect.set(0, top, w, bottom);
            canvas.drawRoundRect(segRect, radius, radius, barPaint);
            return;
        }

        float libWidth = (libraryCount / (float) total) * w;
        float chatWidth = (chatsCount / (float) total) * w;
        float chanWidth = w - libWidth - chatWidth;

        // Clip the entire canvas or draw rounded background
        segRect.set(0, top, w, bottom);
        canvas.save();
        android.graphics.Path clipPath = new android.graphics.Path();
        clipPath.addRoundRect(segRect, radius, radius, android.graphics.Path.Direction.CW);
        canvas.clipPath(clipPath);

        // 1. Library segment (accent #C8F169)
        if (libWidth > 0) {
            barPaint.setColor(SoundGramDesignTokens.COLOR_ACCENT);
            canvas.drawRect(0, top, libWidth, bottom, barPaint);
        }

        // 2. Chats segment (#BDBDBD)
        if (chatWidth > 0) {
            barPaint.setColor(SoundGramDesignTokens.COLOR_TEXT_2);
            canvas.drawRect(libWidth, top, libWidth + chatWidth, bottom, barPaint);
        }

        // 3. Channels segment (#5C5C5C)
        if (chanWidth > 0) {
            barPaint.setColor(SoundGramDesignTokens.COLOR_TEXT_DIM);
            canvas.drawRect(libWidth + chatWidth, top, w, bottom, barPaint);
        }

        canvas.restore();
    }
}
