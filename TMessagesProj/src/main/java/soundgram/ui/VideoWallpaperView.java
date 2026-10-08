package soundgram.ui;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.FileLog;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;

public class VideoWallpaperView extends FrameLayout implements TextureView.SurfaceTextureListener {

    private TextureView textureView;
    private View dimView;
    private MediaPlayer mediaPlayer;
    private Surface surface;
    private String currentVideoPath;
    private float currentDimming = 0f;
    private int videoWidth;
    private int videoHeight;
    private boolean isPrepared;
    private boolean isPaused;

    public VideoWallpaperView(Context context) {
        super(context);

        textureView = new TextureView(context);
        textureView.setSurfaceTextureListener(this);
        addView(textureView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        dimView = new View(context);
        dimView.setBackgroundColor(0xFF000000);
        dimView.setAlpha(0f);
        addView(dimView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }

    public void setDimming(float dimming) {
        currentDimming = Math.max(0f, Math.min(1f, dimming));
        if (dimView != null) {
            dimView.setAlpha(currentDimming);
        }
    }

    public void setVideoPath(String path) {
        if (TextUtils.equals(currentVideoPath, path)) {
            return;
        }
        currentVideoPath = path;
        playVideo();
    }

    private void playVideo() {
        releasePlayer();
        if (TextUtils.isEmpty(currentVideoPath) || surface == null) {
            return;
        }
        File file = new File(currentVideoPath);
        if (!file.exists()) {
            return;
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setSurface(surface);
            mediaPlayer.setDataSource(getContext(), Uri.fromFile(file));
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(0f, 0f);
            mediaPlayer.setOnVideoSizeChangedListener((mp, width, height) -> {
                videoWidth = width;
                videoHeight = height;
                updateTextureMatrix();
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                try {
                    mp.seekTo(0);
                    mp.start();
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            });
            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                if (!isPaused) {
                    try {
                        mp.start();
                    } catch (Throwable e) {
                        FileLog.e(e);
                    }
                }
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                releasePlayer();
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (Throwable e) {
            FileLog.e(e);
            releasePlayer();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (surface == null && textureView != null && textureView.isAvailable()) {
            SurfaceTexture st = textureView.getSurfaceTexture();
            if (st != null) {
                surface = new Surface(st);
            }
        }
        if (surface != null && !TextUtils.isEmpty(currentVideoPath) && mediaPlayer == null) {
            playVideo();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        pause();
    }

    public void pause() {
        isPaused = true;
        if (mediaPlayer != null && isPrepared) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
    }

    public void resume() {
        isPaused = false;
        if (surface == null && textureView != null && textureView.isAvailable()) {
            SurfaceTexture st = textureView.getSurfaceTexture();
            if (st != null) {
                surface = new Surface(st);
            }
        }
        if (mediaPlayer != null && isPrepared) {
            try {
                if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        } else if (surface != null && !TextUtils.isEmpty(currentVideoPath)) {
            playVideo();
        }
    }

    public void releasePlayer() {
        isPrepared = false;
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (Throwable ignored) {}
            try {
                mediaPlayer.release();
            } catch (Throwable ignored) {}
            mediaPlayer = null;
        }
    }

    private void updateTextureMatrix() {
        if (videoWidth == 0 || videoHeight == 0 || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        float scaleX = 1.0f;
        float scaleY = 1.0f;

        float viewRatio = (float) viewWidth / (float) viewHeight;
        float videoRatio = (float) videoWidth / (float) videoHeight;

        if (viewRatio > videoRatio) {
            scaleY = (viewRatio / videoRatio);
        } else {
            scaleX = (videoRatio / viewRatio);
        }

        Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY, viewWidth / 2.0f, viewHeight / 2.0f);
        textureView.setTransform(matrix);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            updateTextureMatrix();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        surface = new Surface(surfaceTexture);
        playVideo();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        updateTextureMatrix();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        releasePlayer();
        if (surface != null) {
            surface.release();
            surface = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }
}
