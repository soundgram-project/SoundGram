package soundgram.icons;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

import java.util.List;

public class IconDialView extends ViewGroup {
    private int centerSize;
    private int surroundSize;
    private int radius;
    private float scale;

    public IconDialView(Context context) {
        this(context, 1.0f);
    }

    public IconDialView(Context context, float scale) {
        super(context);
        this.scale = scale;
        this.centerSize = (int) (AndroidUtilities.dp(48) * scale);
        this.surroundSize = (int) (AndroidUtilities.dp(40) * scale);
        this.radius = (int) (AndroidUtilities.dp(64) * scale);
    }

    public void setIcons(List<Bitmap> bitmaps) {
        removeAllViews();
        if (bitmaps == null || bitmaps.isEmpty()) return;

        // Center icon (index 0)
        ImageView centerView = createIconView(getContext(), bitmaps.get(0), centerSize);
        addView(centerView);

        // Surrounding icons (index 1 to 6)
        for (int i = 1; i < bitmaps.size(); i++) {
            ImageView surroundView = createIconView(getContext(), bitmaps.get(i), surroundSize);
            addView(surroundView);
        }
    }

    private ImageView createIconView(Context context, Bitmap bmp, int size) {
        ImageView img = new ImageView(context);
        img.setImageBitmap(bmp);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int padding = (int) (AndroidUtilities.dp(6) * scale);
        img.setPadding(padding, padding, padding, padding);
        img.setBackground(Theme.createSimpleSelectorCircleDrawable(size, 0x0f000000, 0x1f000000));
        return img;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = (int) (AndroidUtilities.dp(200) * scale);
        setMeasuredDimension(size, size);

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int childSize = (i == 0) ? centerSize : surroundSize;
            child.measure(MeasureSpec.makeMeasureSpec(childSize, MeasureSpec.EXACTLY),
                          MeasureSpec.makeMeasureSpec(childSize, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        if (count == 0) return;

        int cx = getMeasuredWidth() / 2;
        int cy = getMeasuredHeight() / 2;

        // Center child
        View centerChild = getChildAt(0);
        centerChild.layout(cx - centerChild.getMeasuredWidth() / 2,
                           cy - centerChild.getMeasuredHeight() / 2,
                           cx + centerChild.getMeasuredWidth() / 2,
                           cy + centerChild.getMeasuredHeight() / 2);

        // Surrounding children
        int surroundCount = count - 1;
        for (int i = 0; i < surroundCount; i++) {
            View child = getChildAt(i + 1);
            double angle = -Math.PI / 2 + (i * 2 * Math.PI / surroundCount);
            int x = (int) (cx + radius * Math.cos(angle));
            int y = (int) (cy + radius * Math.sin(angle));
            child.layout(x - child.getMeasuredWidth() / 2,
                         y - child.getMeasuredHeight() / 2,
                         x + child.getMeasuredWidth() / 2,
                         y + child.getMeasuredHeight() / 2);
        }
    }
}
