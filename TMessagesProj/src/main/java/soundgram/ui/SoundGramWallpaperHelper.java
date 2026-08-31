package soundgram.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import soundgram.music.SoundGramConfig;

public class SoundGramWallpaperHelper {

    private static final int TAG_WALLPAPER_VIEW = 0x7f099999;
    private static Bitmap sCachedChatListBitmap;
    private static String sCachedChatListBitmapPath;

    public static boolean hasChatListWallpaper() {
        return (!TextUtils.isEmpty(SoundGramConfig.chatListWallVideoPath) && new File(SoundGramConfig.chatListWallVideoPath).exists())
                || (!TextUtils.isEmpty(SoundGramConfig.chatListWallPath) && new File(SoundGramConfig.chatListWallPath).exists());
    }

    public static boolean hasChatVideoWallpaper() {
        return !TextUtils.isEmpty(SoundGramConfig.chatVideoPath) && new File(SoundGramConfig.chatVideoPath).exists();
    }

    public static void applyChatListWallpaper(ViewGroup container) {
        if (container == null) return;
        Context context = container.getContext();

        View existing = container.findViewWithTag(TAG_WALLPAPER_VIEW);
        if (existing != null) {
            container.removeView(existing);
        }

        if (!TextUtils.isEmpty(SoundGramConfig.chatListWallVideoPath)) {
            File videoFile = new File(SoundGramConfig.chatListWallVideoPath);
            if (videoFile.exists()) {
                VideoWallpaperView videoView = new VideoWallpaperView(context);
                videoView.setTag(TAG_WALLPAPER_VIEW);
                videoView.setDimming(SoundGramConfig.chatListWallDimming);
                videoView.setVideoPath(SoundGramConfig.chatListWallVideoPath);
                container.addView(videoView, 0, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
                return;
            }
        }

        if (!TextUtils.isEmpty(SoundGramConfig.chatListWallPath)) {
            File imageFile = new File(SoundGramConfig.chatListWallPath);
            if (imageFile.exists()) {
                FrameLayout frame = new FrameLayout(context);
                frame.setTag(TAG_WALLPAPER_VIEW);

                ImageView iv = new ImageView(context);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);

                try {
                    if (!imageFile.getAbsolutePath().equals(sCachedChatListBitmapPath) || sCachedChatListBitmap == null || sCachedChatListBitmap.isRecycled()) {
                        sCachedChatListBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                        sCachedChatListBitmapPath = imageFile.getAbsolutePath();
                    }
                    if (sCachedChatListBitmap != null) {
                        iv.setImageBitmap(sCachedChatListBitmap);
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                }

                frame.addView(iv, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

                View dim = new View(context);
                dim.setBackgroundColor(0xFF000000);
                dim.setAlpha(Math.max(0f, Math.min(1f, SoundGramConfig.chatListWallDimming)));
                frame.addView(dim, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

                container.addView(frame, 0, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            }
        }
    }

    public static void applyChatVideoWallpaper(ViewGroup container) {
        if (container == null) return;
        Context context = container.getContext();

        View existing = container.findViewWithTag(TAG_WALLPAPER_VIEW);
        if (existing != null) {
            container.removeView(existing);
        }

        if (!TextUtils.isEmpty(SoundGramConfig.chatVideoPath)) {
            File videoFile = new File(SoundGramConfig.chatVideoPath);
            if (videoFile.exists()) {
                VideoWallpaperView videoView = new VideoWallpaperView(context);
                videoView.setTag(TAG_WALLPAPER_VIEW);
                videoView.setVideoPath(SoundGramConfig.chatVideoPath);
                container.addView(videoView, 0, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            }
        }
    }

    public static void onResume(ViewGroup container) {
        if (container == null) return;
        View view = container.findViewWithTag(TAG_WALLPAPER_VIEW);
        if (view instanceof VideoWallpaperView) {
            ((VideoWallpaperView) view).resume();
        }
    }

    public static void onPause(ViewGroup container) {
        if (container == null) return;
        View view = container.findViewWithTag(TAG_WALLPAPER_VIEW);
        if (view instanceof VideoWallpaperView) {
            ((VideoWallpaperView) view).pause();
        }
    }

    public static String saveFileToInternalStorage(Uri uri, String targetFileName) {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            File dest = new File(ctx.getFilesDir(), targetFileName);
            InputStream in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) return null;
            FileOutputStream out = new FileOutputStream(dest);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            return dest.getAbsolutePath();
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }
}
