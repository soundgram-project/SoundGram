package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.media.audiofx.Visualizer;
import android.view.View;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.ui.ActionBar.Theme;

public class WaveAnimatedWidget extends View {
    private Paint blobPaint;
    private Paint glowPaint;
    private Path blobPath;

    private float rotationAngle = 0f;
    private float colorShift = 0f;

    private Visualizer visualizer;
    private static final int SPECTRUM_BANDS = 32;
    private float[] currentMagnitudes = new float[SPECTRUM_BANDS];
    private float[] targetMagnitudes = new float[SPECTRUM_BANDS];
    private float organismRadius = 150f; // Начальное значение

    private float targetBass = 0f;
    private float currentBass = 0f;
    private float idleBreathing = 0f;

    public WaveAnimatedWidget(Context context) {
        super(context);
        init();
    }

    private void init() {
        blobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blobPaint.setStyle(Paint.Style.FILL);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.FILL);

        blobPath = new Path();
    }

    public void linkToAudioSession(int sessionId) {
        try {
            if (visualizer != null) {
                visualizer.setEnabled(false);
                visualizer.release();
                visualizer = null;
            }

            visualizer = new Visualizer(sessionId);
            visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer v, byte[] waveform, int samplingRate) {
                    // Нам нужна только частотка (FFT), форму волны пропускаем
                }

                @Override
                public void onFftDataCapture(Visualizer v, byte[] fft, int samplingRate) {
                    processFftData(fft);
                    invalidate(); // Перерисовываем кадр
                }
            }, Visualizer.getMaxCaptureRate() / 2, false, true);

            visualizer.setEnabled(true);
        } catch (Exception e) {
            FileLog.e("SoundGram: Visualizer init failed for session " + sessionId + ": " + e.getMessage());
            visualizer = null; // Будет работать смарт-фолбек в onDraw
        }
    }

    private void processFftData(byte[] fft) {
        if (fft == null || fft.length < 2) return;

        int dataSize = Math.min(fft.length / 2, SPECTRUM_BANDS);
        float currentBassLevel = 0f;

        for (int i = 0; i < SPECTRUM_BANDS; i++) {
            if (i < dataSize) {
                byte rfk = fft[2 * i];
                byte ifk = fft[2 * i + 1];
                float magnitude = (float) Math.hypot(rfk, ifk);

                // Снижен множитель до 1.2f, ограничение деформации до 30% от радиуса (было 2.5f и 80%)
                targetMagnitudes[i] = Math.min(magnitude * 1.2f, this.organismRadius * 0.3f);

                if (i < 4) {
                    currentBassLevel += magnitude;
                }
            } else {
                targetMagnitudes[i] = 0;
            }
        }
        targetBass = currentBassLevel / 4f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        canvas.drawColor(Theme.getColor(Theme.key_windowBackgroundGray));

        float cx = w / 2f;
        float cy = h / 2f;
        float baseRadius = Math.min(w, h) * 0.28f;

        rotationAngle += 1.5f;
        colorShift += 0.002f;
        idleBreathing += 0.05f;

        if (rotationAngle >= 360f) rotationAngle -= 360f;
        if (colorShift > 1f) colorShift -= 1f;

        int coreColor1 = Color.HSVToColor(new float[]{(colorShift * 360) % 360, 0.8f, 1f});
        int coreColor2 = Color.HSVToColor(new float[]{((colorShift + 0.15f) * 360) % 360, 0.9f, 0.9f});

        boolean isPlaying = MediaController.getInstance().getPlayingMessageObject() != null &&
                !MediaController.getInstance().isMessagePaused();

        if (!isPlaying) {
            targetBass = 0f;
            for (int i = 0; i < SPECTRUM_BANDS; i++) targetMagnitudes[i] = 0f;
        }

        if (targetBass > currentBass) {
            currentBass += (targetBass - currentBass) * 0.4f;
        } else {
            currentBass += (targetBass - currentBass) * 0.1f;
        }

        // Жесткое ограничение баса (максимум +25% к размеру, а не 150%)
        if (currentBass > baseRadius * 0.25f) currentBass = baseRadius * 0.25f;

        float breathingOffset = (float) Math.sin(idleBreathing) * (baseRadius * 0.05f);

        // ВАЖНО: Обновляем глобальную переменную класса, чтобы процесс FFT знал реальный размер
        this.organismRadius = baseRadius + currentBass + breathingOffset;

        float glowRadius = this.organismRadius * 1.7f;
        RadialGradient glowShader = new RadialGradient(
                cx, cy, glowRadius,
                new int[]{Color.argb(120, Color.red(coreColor1), Color.green(coreColor1), Color.blue(coreColor1)), Color.TRANSPARENT},
                new float[]{0.3f, 1f}, Shader.TileMode.CLAMP
        );
        glowPaint.setShader(glowShader);
        canvas.drawCircle(cx, cy, glowRadius, glowPaint);

        LinearGradient bodyShader = new LinearGradient(
                cx - this.organismRadius, cy - this.organismRadius,
                cx + this.organismRadius, cy + this.organismRadius,
                new int[]{coreColor1, coreColor2}, null, Shader.TileMode.MIRROR
        );
        blobPaint.setShader(bodyShader);

        blobPath.reset();
        int points = SPECTRUM_BANDS;
        float[] px = new float[points];
        float[] py = new float[points];

        // Смарт-фолбек: успокаиваем рандомные скачки, если музыка играет в обход визуализатора
        if (visualizer == null || !visualizer.getEnabled()) {
            if (isPlaying) {
                targetBass = (float) (Math.random() * 10 + 5);
                for (int i = 0; i < SPECTRUM_BANDS; i++) {
                    targetMagnitudes[i] = (float) (Math.random() * this.organismRadius * 0.15f);
                }
            } else {
                targetBass = 0;
                for (int i = 0; i < SPECTRUM_BANDS; i++) targetMagnitudes[i] = 0;
            }
        }

        for (int i = 0; i < points; i++) {
            if (targetMagnitudes[i] > currentMagnitudes[i]) {
                currentMagnitudes[i] += (targetMagnitudes[i] - currentMagnitudes[i]) * 0.5f;
            } else {
                currentMagnitudes[i] += (targetMagnitudes[i] - currentMagnitudes[i]) * 0.15f;
            }

            float angle = (float) (i * 2 * Math.PI / points) + (float) Math.toRadians(rotationAngle);
            // Снижена деформация поверхности от треков (было 1.8f)
            float surfaceDeform = currentMagnitudes[i] * 0.7f;
            float r = this.organismRadius + surfaceDeform;

            px[i] = cx + r * (float) Math.cos(angle);
            py[i] = cy + r * (float) Math.sin(angle);
        }

        blobPath.moveTo((px[0] + px[points - 1]) / 2f, (py[0] + py[points - 1]) / 2f);
        for (int i = 0; i < points; i++) {
            int next = (i + 1) % points;
            float midX = (px[i] + px[next]) / 2f;
            float midY = (py[i] + py[next]) / 2f;
            blobPath.quadTo(px[i], py[i], midX, midY);
        }
        blobPath.close();

        canvas.drawPath(blobPath, blobPaint);
        invalidate();
    }

    public void releaseVisualizer() {
        if (visualizer != null) {
            try {
                visualizer.setEnabled(false);
                visualizer.release();
            } catch (Exception e) {
                FileLog.e(e);
            }
            visualizer = null;
        }
        targetBass = 0f;
        for (int i = 0; i < SPECTRUM_BANDS; i++) targetMagnitudes[i] = 0;
    }

    public void startAnimation() {
        invalidate();
    }

    public void stopAnimation() {
        releaseVisualizer();
    }

}