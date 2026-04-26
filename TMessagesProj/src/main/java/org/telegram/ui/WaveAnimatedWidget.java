package org.telegram.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.media.audiofx.Visualizer;
import android.view.View;
import android.view.animation.LinearInterpolator;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;
import org.telegram.ui.ActionBar.Theme;

import java.util.Random;

/**
 * WaveAnimatedWidget — полностью переработанный.
 *
 * Что нового:
 *  • Жидкий стеклянный blob с аурой и хроматическим свечением
 *  • Плавающие частицы (звёзды/пыль) вокруг blob'а
 *  • Три слоя: внешнее свечение → inner glow → blob-тело с градиентом
 *  • Встроенный «дыхательный» idle-пульс
 *  • Реакция на FFT через Visualizer (с smart-fallback)
 *  • Прозрачный фон (рисуем поверх стеклянной карточки)
 */
public class WaveAnimatedWidget extends View {

    // ─── Paint-объекты ────────────────────────────────────────────────────────
    private final Paint outerGlowPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerGlowPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blobPaint       = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shimmerPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint       = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ─── Blob / FFT ────────────────────────────────────────────────────────────
    private final Path blobPath = new Path();
    private static final int SPECTRUM_BANDS = 48;
    private final float[] currentMagnitudes = new float[SPECTRUM_BANDS];
    private final float[] targetMagnitudes  = new float[SPECTRUM_BANDS];

    private float rotationAngle  = 0f;
    private float colorShift     = 0f;
    private float idleBreathing  = 0f;
    private float targetBass     = 0f;
    private float currentBass    = 0f;
    private float organismRadius = 100f;

    // ─── Частицы ──────────────────────────────────────────────────────────────
    private static final int PARTICLE_COUNT = 55;  // больше звёзд для всего экрана
    private final float[] pX       = new float[PARTICLE_COUNT];
    private final float[] pY       = new float[PARTICLE_COUNT];
    private final float[] pVX      = new float[PARTICLE_COUNT];
    private final float[] pVY      = new float[PARTICLE_COUNT];
    private final float[] pRadius  = new float[PARTICLE_COUNT];
    private final float[] pAlpha   = new float[PARTICLE_COUNT];
    private final float[] pAlphaV  = new float[PARTICLE_COUNT]; // alpha velocity
    private final float[] pPhase   = new float[PARTICLE_COUNT]; // orbit phase
    private final float[] pOrbit   = new float[PARTICLE_COUNT]; // orbit radius
    private final float[] pOrbitV  = new float[PARTICLE_COUNT]; // orbit speed
    private final int[]   pColor   = new int[PARTICLE_COUNT];
    private boolean particlesInit  = false;

    // ─── Пульсирующие кольца ──────────────────────────────────────────────────
    private static final int RING_COUNT = 3;
    private final float[] ringRadius = new float[RING_COUNT];
    private final float[] ringAlpha  = new float[RING_COUNT];
    private final float[] ringSpeed  = new float[RING_COUNT];

    // ─── Shimmer-блик (ярко-белый луч сверху-слева) ───────────────────────────
    private float shimmerAngle = -40f;

    // ─── Visualizer ───────────────────────────────────────────────────────────
    private Visualizer visualizer;

    // ─── ValueAnimator для постоянного invalidate() ───────────────────────────
    private ValueAnimator ticker;

    private final Random rng = new Random();

    // ─── Цветовая палитра (меняется со временем) ──────────────────────────────
    private final float[] hsvBuf = new float[3];

    public WaveAnimatedWidget(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_SOFTWARE, null); // нужен для BlurMaskFilter
        init();
    }

    private void init() {
        blobPaint.setStyle(Paint.Style.FILL);

        outerGlowPaint.setStyle(Paint.Style.FILL);
        innerGlowPaint.setStyle(Paint.Style.FILL);

        shimmerPaint.setStyle(Paint.Style.FILL);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(1.5f);
        ringPaint.setAntiAlias(true);

        particlePaint.setStyle(Paint.Style.FILL);

        // Инициализируем кольца
        for (int i = 0; i < RING_COUNT; i++) {
            ringRadius[i] = i * 0.3f; // разнесём по фазе
            ringAlpha[i]  = 1f - i * 0.3f;
            ringSpeed[i]  = 0.6f + i * 0.25f;
        }
    }

    // ─── Подключение к AudioSession ───────────────────────────────────────────
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
                @Override public void onWaveFormDataCapture(Visualizer v, byte[] wf, int sr) {}
                @Override public void onFftDataCapture(Visualizer v, byte[] fft, int sr) {
                    processFft(fft);
                }
            }, Visualizer.getMaxCaptureRate() / 2, false, true);
            visualizer.setEnabled(true);
        } catch (Exception e) {
            FileLog.e("SoundGram WaveWidget: Visualizer failed: " + e.getMessage());
            visualizer = null;
        }
    }

    private void processFft(byte[] fft) {
        if (fft == null || fft.length < 2) return;
        int dataSize = Math.min(fft.length / 2, SPECTRUM_BANDS);
        float bassLevel = 0f;
        for (int i = 0; i < SPECTRUM_BANDS; i++) {
            if (i < dataSize) {
                float mag = (float) Math.hypot(fft[2 * i], fft[2 * i + 1]);
                targetMagnitudes[i] = Math.min(mag * 1.1f, organismRadius * 0.28f);
                if (i < 5) bassLevel += mag;
            } else {
                targetMagnitudes[i] = 0f;
            }
        }
        targetBass = bassLevel / 5f;
    }

    // ─── Запуск/остановка ─────────────────────────────────────────────────────
    public void startAnimation() {
        if (ticker == null || !ticker.isRunning()) {
            ticker = ValueAnimator.ofFloat(0f, 1f);
            ticker.setDuration(16_000);
            ticker.setRepeatCount(ValueAnimator.INFINITE);
            ticker.setInterpolator(new LinearInterpolator());
            ticker.addUpdateListener(a -> invalidate());
            ticker.start();
        }
    }

    public void stopAnimation() {
        releaseVisualizer();
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        // Сброс FFT
        targetBass = 0f;
        for (int i = 0; i < SPECTRUM_BANDS; i++) targetMagnitudes[i] = 0f;
    }

    public void releaseVisualizer() {
        if (visualizer != null) {
            try { visualizer.setEnabled(false); visualizer.release(); } catch (Exception e) { FileLog.e(e); }
            visualizer = null;
        }
    }

    // ─── Инициализация частиц (после первого layout) ─────────────────────────
    private void initParticles(float cx, float cy, float baseR) {
        int w = getWidth(), h = getHeight();
        float maxDist = (float) Math.sqrt(cx * cx + cy * cy) * 1.1f; // до углов экрана
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            // Первые 15 частиц — ближе к шару (орбитальные), остальные — по всему экрану
            if (i < 15) {
                pOrbit[i] = baseR * (1.4f + rng.nextFloat() * 1.2f);
            } else {
                // Случайно по всему экрану
                pOrbit[i] = baseR * 1.8f + rng.nextFloat() * (maxDist - baseR * 1.8f);
            }
            pOrbitV[i] = (rng.nextBoolean() ? 1 : -1) * (0.1f + rng.nextFloat() * 0.5f);
            pPhase[i]  = rng.nextFloat() * (float)(2 * Math.PI);
            pRadius[i] = 1.0f + rng.nextFloat() * (i < 15 ? 4.0f : 2.5f);
            pAlpha[i]  = rng.nextFloat();
            pAlphaV[i] = 0.005f + rng.nextFloat() * 0.018f;

            pX[i] = cx + pOrbit[i] * (float) Math.cos(pPhase[i]);
            pY[i] = cy + pOrbit[i] * (float) Math.sin(pPhase[i]);
            pVX[i] = 0f; pVY[i] = 0f;

            // Цвет — разнообразная палитра: синий, фиолетовый, розовый, белый
            int h2 = 180 + rng.nextInt(180); // 180–360
            float sat = i % 7 == 0 ? 0.0f : (0.3f + rng.nextFloat() * 0.6f); // часть — белые
            pColor[i] = Color.HSVToColor(new float[]{h2, sat, 1f});
        }
        particlesInit = true;
    }

    // ─── onDraw ───────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float cx = w / 2f;
        float cy = h / 2f;
        float baseRadius = Math.min(w, h) * 0.38f; // увеличенный шар

        if (!particlesInit) initParticles(cx, cy, baseRadius);

        // ── Шаг времени ───────────────────────────────────────────────────────
        rotationAngle += 1.2f;
        colorShift    += 0.0018f;
        idleBreathing += 0.045f;
        shimmerAngle  += 0.4f;

        if (rotationAngle >= 360f) rotationAngle -= 360f;
        if (colorShift   >  1f)   colorShift    -= 1f;

        // ── Состояние плеера ──────────────────────────────────────────────────
        boolean isPlaying = MediaController.getInstance().getPlayingMessageObject() != null
                && !MediaController.getInstance().isMessagePaused();

        if (!isPlaying) {
            targetBass = 0f;
            for (int i = 0; i < SPECTRUM_BANDS; i++) targetMagnitudes[i] = 0f;
        }

        // Smart-fallback: если visualizer null, но музыка идёт
        if ((visualizer == null) && isPlaying) {
            targetBass = 8f + rng.nextFloat() * 10f;
            for (int i = 0; i < SPECTRUM_BANDS; i++) {
                targetMagnitudes[i] = rng.nextFloat() * baseRadius * 0.12f;
            }
        }

        // ── Интерполяция bass ─────────────────────────────────────────────────
        float bassAlpha = targetBass > currentBass ? 0.38f : 0.08f;
        currentBass += (targetBass - currentBass) * bassAlpha;
        if (currentBass > baseRadius * 0.22f) currentBass = baseRadius * 0.22f;

        float breathingOffset = (float) Math.sin(idleBreathing) * (baseRadius * 0.06f);
        this.organismRadius = baseRadius + currentBass + breathingOffset;

        // ── Цвета ─────────────────────────────────────────────────────────────
        float hue1 = (colorShift * 360f + 260f) % 360f; // фиолетовый
        float hue2 = (colorShift * 360f + 200f) % 360f; // голубой
        float hue3 = (colorShift * 360f + 300f) % 360f; // розовый

        hsvBuf[0] = hue1; hsvBuf[1] = 0.75f; hsvBuf[2] = 1f;
        int col1 = Color.HSVToColor(hsvBuf);
        hsvBuf[0] = hue2; hsvBuf[1] = 0.65f; hsvBuf[2] = 0.95f;
        int col2 = Color.HSVToColor(hsvBuf);
        hsvBuf[0] = hue3; hsvBuf[1] = 0.80f; hsvBuf[2] = 1f;
        int col3 = Color.HSVToColor(hsvBuf);

        // ── 1. Внешнее динамическое свечение (пульсирует с музыкой) ─────────
        float bassBoost = 1f + (currentBass / (baseRadius * 0.22f)) * 0.6f; // 1.0–1.6x от баса
        float outerR = organismRadius * (2.0f + 0.5f * bassBoost);
        // Слой 1: самое внешнее — широкая аура
        outerGlowPaint.setShader(new RadialGradient(cx, cy, outerR,
                new int[]{
                        Color.argb((int)(70 * bassBoost),  Color.red(col1), Color.green(col1), Color.blue(col1)),
                        Color.argb((int)(35 * bassBoost),  Color.red(col2), Color.green(col2), Color.blue(col2)),
                        Color.argb(12, Color.red(col3), Color.green(col3), Color.blue(col3)),
                        Color.TRANSPARENT
                },
                new float[]{0f, 0.35f, 0.65f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, outerR, outerGlowPaint);
        // Слой 2: средняя аура (смещённый цвет для хроматического эффекта)
        float midR = organismRadius * (1.65f + 0.25f * bassBoost);
        innerGlowPaint.setShader(new RadialGradient(cx, cy, midR,
                new int[]{
                        Color.argb((int)(160 * Math.min(1.2f, bassBoost)), Color.red(col1), Color.green(col1), Color.blue(col1)),
                        Color.argb((int)(80  * bassBoost), Color.red(col3), Color.green(col3), Color.blue(col3)),
                        Color.TRANSPARENT
                },
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, midR, innerGlowPaint);

        // ── 2. Пульсирующие кольца ────────────────────────────────────────────
        for (int i = 0; i < RING_COUNT; i++) {
            ringRadius[i] += ringSpeed[i];
            ringAlpha[i]  -= 0.005f * ringSpeed[i];

            float maxR = organismRadius * 2.8f;
            if (ringRadius[i] > maxR || ringAlpha[i] <= 0f) {
                // Перезапускаем кольцо
                ringRadius[i] = organismRadius * 1.05f;
                ringAlpha[i]  = 0.35f + rng.nextFloat() * 0.2f;
                ringSpeed[i]  = 0.55f + rng.nextFloat() * 0.4f;
            }

            int ringColor = (i % 2 == 0) ? col1 : col2;
            ringPaint.setColor(Color.argb(
                    (int)(ringAlpha[i] * 255),
                    Color.red(ringColor), Color.green(ringColor), Color.blue(ringColor)));
            canvas.drawCircle(cx, cy, ringRadius[i], ringPaint);
        }

        // ── 3. Blob-тело ──────────────────────────────────────────────────────
        blobPaint.setShader(new LinearGradient(
                cx - organismRadius, cy - organismRadius,
                cx + organismRadius, cy + organismRadius,
                new int[]{col1, col2, col3},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.MIRROR));

        // Интерполяция FFT-деформаций
        blobPath.reset();
        int pts = SPECTRUM_BANDS;
        float[] px = new float[pts];
        float[] py = new float[pts];

        for (int i = 0; i < pts; i++) {
            float tgt = targetMagnitudes[i];
            float cur = currentMagnitudes[i];
            currentMagnitudes[i] = cur + (tgt - cur) * (tgt > cur ? 0.45f : 0.12f);

            // Дополнительная деформация от idle-дыхания (органичная форма)
            float idleDeform = (float) Math.sin(idleBreathing * 0.7f + i * 0.4f) * baseRadius * 0.03f;
            float surfaceDeform = currentMagnitudes[i] * 0.65f + idleDeform;
            float r = this.organismRadius + surfaceDeform;

            float angle = (float)(i * 2 * Math.PI / pts) + (float) Math.toRadians(rotationAngle);
            px[i] = cx + r * (float) Math.cos(angle);
            py[i] = cy + r * (float) Math.sin(angle);
        }

        blobPath.moveTo((px[0] + px[pts - 1]) / 2f, (py[0] + py[pts - 1]) / 2f);
        for (int i = 0; i < pts; i++) {
            int next = (i + 1) % pts;
            blobPath.quadTo(px[i], py[i], (px[i] + px[next]) / 2f, (py[i] + py[next]) / 2f);
        }
        blobPath.close();
        canvas.drawPath(blobPath, blobPaint);

        // ── 4. Shimmer-блик (стеклянный отблеск) ─────────────────────────────
        // Вращаем полупрозрачный белый эллипс поверх blob
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(shimmerAngle);
        float shimW = organismRadius * 0.55f;
        float shimH = organismRadius * 0.22f;
        shimmerPaint.setShader(new LinearGradient(
                -shimW, -shimH, shimW, shimH,
                new int[]{Color.TRANSPARENT, Color.argb(55, 255, 255, 255), Color.TRANSPARENT},
                null, Shader.TileMode.CLAMP));
        canvas.drawOval(-shimW, -shimH, shimW, shimH, shimmerPaint);
        canvas.restore();

        // ── 5. Частицы-звёзды ─────────────────────────────────────────────────
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            // Орбитальное движение (частица вращается вокруг blob)
            pPhase[i] += Math.toRadians(pOrbitV[i]);

            // Радиус орбиты немного пульсирует с музыкой
            float dynamicOrbit = pOrbit[i] + currentBass * 0.5f;
            pX[i] = cx + dynamicOrbit * (float) Math.cos(pPhase[i]);
            pY[i] = cy + dynamicOrbit * (float) Math.sin(pPhase[i]);

            // Мерцание
            pAlpha[i] += pAlphaV[i];
            if (pAlpha[i] > 1f) { pAlpha[i] = 1f; pAlphaV[i] = -pAlphaV[i]; }
            if (pAlpha[i] < 0f) { pAlpha[i] = 0f; pAlphaV[i] = -pAlphaV[i]; }

            int pc = pColor[i];
            particlePaint.setColor(Color.argb(
                    (int)(pAlpha[i] * 200),
                    Color.red(pc), Color.green(pc), Color.blue(pc)));

            // Звёздочка: центральный кружок + крест
            float pr = pRadius[i];
            canvas.drawCircle(pX[i], pY[i], pr, particlePaint);

            // Лучи у более крупных частиц
            if (pr > 3f) {
                particlePaint.setColor(Color.argb(
                        (int)(pAlpha[i] * 100),
                        Color.red(pc), Color.green(pc), Color.blue(pc)));
                float ray = pr * 2.2f;
                canvas.drawRect(pX[i] - ray, pY[i] - 0.7f, pX[i] + ray, pY[i] + 0.7f, particlePaint);
                canvas.drawRect(pX[i] - 0.7f, pY[i] - ray, pX[i] + 0.7f, pY[i] + ray, particlePaint);
            }
        }
    }
}
