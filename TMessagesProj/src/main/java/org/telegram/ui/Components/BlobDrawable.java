package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.RectF;
import android.graphics.BlurMaskFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;

import org.telegram.messenger.LiteMode;
import org.telegram.messenger.SharedConfig;

import java.util.Random;

public class BlobDrawable {

    public static float MAX_SPEED = 8.2f;
    public static float MIN_SPEED = 0.8f;
    public static float AMPLITUDE_SPEED = 0.33f;

    public static float SCALE_BIG = 0.807f;
    public static float SCALE_SMALL = 0.704f;

    public static float SCALE_BIG_MIN = 0.878f;
    public static float SCALE_SMALL_MIN = 0.926f;

    public static float FORM_BIG_MAX = 0.6f;
    public static float FORM_SMALL_MAX = 0.6f;

    public static float GLOBAL_SCALE = 1f;

    public static float FORM_BUTTON_MAX = 0f;

    public static float GRADIENT_SPEED_MIN = 0.5f;
    public static float GRADIENT_SPEED_MAX = 0.01f;

    public static float LIGHT_GRADIENT_SIZE = 0.5f;

    public float minRadius;
    public float maxRadius;

    private Path path = new Path();
    public Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    protected float[] radius;
    protected float[] angle;
    protected float[] radiusNext;
    protected float[] angleNext;
    protected float[] progress;
    protected float[] speed;


    private float[] pointStart = new float[4];
    private float[] pointEnd = new float[4];

    protected final Random random = new Random();

    protected final float N;
    private final float L;
    public float cubicBezierK = 1f;

    private final Matrix m = new Matrix();

    protected final int liteFlag;

    private static class Ray {
        float angle;
        float currentLength;
        float targetLength;
        float speed;
        float phase;
        int color;
    }
    private Ray[] rays;
    private final static int NUM_RAYS = 20;

    private static final String AGSL = 
        "uniform vec2 u_resolution;\n" +
        "uniform float u_time;\n" +
        "uniform float u_amplitude;\n" +
        "vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }\n" +
        "vec2 mod289(vec2 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }\n" +
        "vec3 permute(vec3 x) { return mod289(((x*34.0)+1.0)*x); }\n" +
        "float snoise(vec2 v) {\n" +
        "  const vec4 C = vec4(0.211324865405187, 0.366025403784439, -0.577350269189626, 0.024390243902439);\n" +
        "  vec2 i  = floor(v + dot(v, C.yy));\n" +
        "  vec2 x0 = v - i + dot(i, C.xx);\n" +
        "  vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);\n" +
        "  vec4 x12 = x0.xyxy + C.xxzz;\n" +
        "  x12.xy -= i1;\n" +
        "  i = mod289(i);\n" +
        "  vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0)) + i.x + vec3(0.0, i1.x, 1.0));\n" +
        "  vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy), dot(x12.zw,x12.zw)), 0.0);\n" +
        "  m = m*m; m = m*m;\n" +
        "  vec3 x = 2.0 * fract(p * C.www) - 1.0;\n" +
        "  vec3 h = abs(x) - 0.5;\n" +
        "  vec3 ox = floor(x + 0.5);\n" +
        "  vec3 a0 = x - ox;\n" +
        "  m *= 1.79284291400159 - 0.85373472095314 * (a0*a0 + h*h);\n" +
        "  vec3 g;\n" +
        "  g.x  = a0.x  * x0.x  + h.x  * x0.y;\n" +
        "  g.yz = a0.yz * x12.xz + h.yz * x12.yw;\n" +
        "  return 130.0 * dot(m, g);\n" +
        "}\n" +
        "float fbm(vec2 p, float t) {\n" +
        "  p += vec2(sin(t * 0.5), cos(t * 0.3));\n" +
        "  float f = 0.0;\n" +
        "  f += 0.5000 * snoise(p); p = p * 2.02 + vec2(t * 0.2, -t * 0.1);\n" +
        "  f += 0.2500 * snoise(p); p = p * 2.03 - vec2(-t * 0.1, t * 0.2);\n" +
        "  f += 0.1250 * snoise(p);\n" +
        "  return f;\n" +
        "}\n" +
        "vec4 main(vec2 fragCoord) {\n" +
        "  vec2 uv = fragCoord.xy / u_resolution.xy;\n" +
        "  vec2 pos = uv * 2.0 - 1.0;\n" +
        "  pos.x *= u_resolution.x / u_resolution.y;\n" +
        "  float baseRadius = 0.4 + u_amplitude * 0.2;\n" +
        "  float angle = atan(pos.y, pos.x);\n" +
        "  float dist = length(pos);\n" +
        "  float noiseRadius = dist + fbm(pos * 2.0, u_time) * 0.2 + fbm(pos * 4.0, u_time * 1.5) * (0.05 + u_amplitude * 0.1);\n" +
        "  float core = 1.0 - smoothstep(baseRadius - 0.1, baseRadius + 0.1, noiseRadius);\n" +
        "  float aura = 1.0 - smoothstep(0.0, baseRadius + 0.5, noiseRadius);\n" +
        "  aura = pow(aura, 1.8);\n" +
        "  vec3 colCore = vec3(1.0, 0.9, 0.0);\n" +
        "  vec3 colMid = vec3(1.0, 0.55, 0.0);\n" +
        "  vec3 colEdge = vec3(0.78, 0.0, 1.0);\n" +
        "  float plasmaNoise = fbm(pos * 5.0, u_time * 2.0);\n" +
        "  float plasma = sin(plasmaNoise * 15.0 - u_time * 8.0) * 0.5 + 0.5;\n" +
        "  vec3 plasmaColor = vec3(1.0, 1.0, 0.6) * plasma * pow(core, 2.5) * (0.5 + u_amplitude * 0.5);\n" +
        "  float rayNoise = snoise(vec2(angle * 5.0, u_time * 0.5));\n" +
        "  float rayNoise2 = snoise(vec2(angle * 9.0, u_time * 0.8));\n" +
        "  float rays = smoothstep(0.3, 1.0, rayNoise * 0.5 + rayNoise2 * 0.5);\n" +
        "  rays *= smoothstep(baseRadius + 0.6, baseRadius + 0.1, dist);\n" +
        "  rays *= smoothstep(0.0, 0.2, dist);\n" +
        "  vec3 finalColor = vec3(0.0);\n" +
        "  finalColor += colEdge * aura * 0.9;\n" +
        "  finalColor += colMid * core * 0.9;\n" +
        "  finalColor += colCore * pow(core, 2.0);\n" +
        "  finalColor += plasmaColor;\n" +
        "  finalColor += vec3(1.0, 0.5, 0.9) * rays * (0.4 + u_amplitude * 0.8);\n" +
        "  float alpha = clamp(max(max(finalColor.r, finalColor.g), finalColor.b), 0.0, 1.0);\n" +
        "  return vec4(finalColor * alpha, alpha);\n" +
        "}\n";

    private Object runtimeShader;
    private Paint shaderPaint;

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * factor);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    private void initRays() {
        rays = new Ray[NUM_RAYS];
        for (int i = 0; i < NUM_RAYS; i++) {
            rays[i] = new Ray();
            rays[i].angle = (float) (i * 2.0 * Math.PI / NUM_RAYS);
            float direction = (i % 2 == 0) ? 1.0f : -1.0f;
            rays[i].speed = direction * (float) (0.05 + 0.08 * random.nextFloat());
            rays[i].phase = random.nextFloat() * 100f;
            if (i % 3 == 0) {
                rays[i].color = 0xFFFFF9C4;
            } else if (i % 3 == 1) {
                rays[i].color = 0xFFE0277D;
            } else {
                rays[i].color = 0xFF8A2BE2;
            }
            rays[i].currentLength = 0;
            rays[i].targetLength = 0;
        }
    }

    public BlobDrawable(int n) {
        this(n, LiteMode.FLAG_CALLS_ANIMATIONS);
    }

    public BlobDrawable(int n, int liteFlag) {
        N = n;
        L = (float) ((4.0 / 3.0) * Math.tan(Math.PI / (2 * N)));
        radius = new float[n];
        angle = new float[n];

        radiusNext = new float[n];
        angleNext = new float[n];
        progress = new float[n];
        speed = new float[n];

        for (int i = 0; i < N; i++) {
            generateBlob(radius, angle, i);
            generateBlob(radiusNext, angleNext, i);
            progress[i] = 0;
        }

        this.liteFlag = liteFlag;
        initRays();

        if (Build.VERSION.SDK_INT >= 33) {
            try {
                runtimeShader = new android.graphics.RuntimeShader(AGSL);
                shaderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                shaderPaint.setShader((android.graphics.RuntimeShader) runtimeShader);
            } catch (Throwable e) {
                runtimeShader = null;
            }
        }
    }

    protected void generateBlob(float[] radius, float[] angle, int i) {
        float angleDif = 360f / N * 0.05f;
        float radDif = maxRadius - minRadius;
        radius[i] = minRadius + Math.abs(((random.nextInt() % 100f) / 100f)) * radDif;
        angle[i] = 360f / N * i + ((random.nextInt() % 100f) / 100f) * angleDif;
        speed[i] = (float) (0.017 + 0.003 * (Math.abs(random.nextInt() % 100f) / 100f));
    }

    public void update(float amplitude, float speedScale) {
        if (!LiteMode.isEnabled(liteFlag)) {
            return;
        }
        if (rays == null) {
            initRays();
        }
        for (int i = 0; i < N; i++) {
            progress[i] += (speed[i] * MIN_SPEED) + amplitude * speed[i] * MAX_SPEED * speedScale;
            if (progress[i] >= 1f) {
                progress[i] = 0;
                radius[i] = radiusNext[i];
                angle[i] = angleNext[i];
                generateBlob(radiusNext, angleNext, i);
            }
        }
        float dt = 0.016f;
        for (int i = 0; i < NUM_RAYS; i++) {
            Ray ray = rays[i];
            ray.phase += dt * (1.8f + amplitude * 4.5f);
            ray.angle += ray.speed * dt * (0.8f + amplitude * 1.5f);
            
            float baseLength = minRadius + (maxRadius - minRadius) * 0.35f;
            float maxLength = maxRadius * (1.35f + 0.75f * amplitude);
            float pulse = (float) Math.sin(ray.phase);
            ray.targetLength = baseLength + (maxLength - baseLength) * (0.5f + 0.5f * pulse);
            
            ray.currentLength += (ray.targetLength - ray.currentLength) * 0.12f;
        }
    }

    public void draw(float cX, float cY, Canvas canvas, Paint paint) {
        if (!LiteMode.isEnabled(liteFlag)) {
            return;
        }
        
        if (rays == null) {
            initRays();
        }

        int parentAlpha = paint.getAlpha();
        float alphaScale = parentAlpha / 255.0f;
        
        if (alphaScale <= 0.01f) {
            return;
        }
        
        float time = System.currentTimeMillis() * 0.0012f;

        if (Build.VERSION.SDK_INT >= 33 && runtimeShader != null && shaderPaint != null) {
            android.graphics.RuntimeShader rs = (android.graphics.RuntimeShader) runtimeShader;
            float size = maxRadius * 2.8f; 
            rs.setFloatUniform("u_resolution", size, size);
            rs.setFloatUniform("u_time", time);
            rs.setFloatUniform("u_amplitude", amplitude);
            
            shaderPaint.setAlpha(parentAlpha);
            canvas.save();
            canvas.translate(cX - size / 2f, cY - size / 2f);
            canvas.drawRect(0, 0, size, size, shaderPaint);
            canvas.restore();
            return;
        }
        
        path.reset();

        for (int i = 0; i < N; i++) {
            float progress = this.progress[i];
            int nextIndex = i + 1 < N ? i + 1 : 0;
            float progressNext = this.progress[nextIndex];
            float r1 = radius[i] * (1f - progress) + radiusNext[i] * progress;
            float r2 = radius[nextIndex] * (1f - progressNext) + radiusNext[nextIndex] * progressNext;
            float angle1 = angle[i] * (1f - progress) + angleNext[i] * progress;
            float angle2 = angle[nextIndex] * (1f - progressNext) + angleNext[nextIndex] * progressNext;

            float l = L * (Math.min(r1, r2) + (Math.max(r1, r2) - Math.min(r1, r2)) / 2f) * cubicBezierK;
            m.reset();
            m.setRotate(angle1, cX, cY);

            pointStart[0] = cX;
            pointStart[1] = cY - r1;
            pointStart[2] = cX + l;
            pointStart[3] = cY - r1;

            m.mapPoints(pointStart);

            pointEnd[0] = cX;
            pointEnd[1] = cY - r2;
            pointEnd[2] = cX - l;
            pointEnd[3] = cY - r2;

            m.reset();
            m.setRotate(angle2, cX, cY);

            m.mapPoints(pointEnd);

            if (i == 0) {
                path.moveTo(pointStart[0], pointStart[1]);
            }

            path.cubicTo(
                    pointStart[2], pointStart[3],
                    pointEnd[2], pointEnd[3],
                    pointEnd[0], pointEnd[1]
            );
        }

        float scaleBoost = 1.28f + 0.12f * amplitude;
        
        float dx1 = (float) Math.sin(time * 0.8f) * maxRadius * 0.06f;
        float dy1 = (float) Math.cos(time * 0.7f) * maxRadius * 0.06f;
        
        float dx2 = (float) Math.sin(time * 1.2f) * maxRadius * 0.03f;
        float dy2 = (float) Math.cos(time * 1.1f) * maxRadius * 0.03f;
        
        float dx3 = (float) Math.sin(time * 1.5f) * maxRadius * 0.015f;
        float dy3 = (float) Math.cos(time * 1.4f) * maxRadius * 0.015f;

        Paint layerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        layerPaint.setStyle(Paint.Style.FILL);
        
        layerPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));

        canvas.save();
        canvas.translate(dx1, dy1);
        canvas.scale(1.18f * scaleBoost, 1.18f * scaleBoost, cX, cY);
        
        float blurRadius = maxRadius * 0.18f;
        if (blurRadius > 0.5f) {
            layerPaint.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL));
        }
        
        RadialGradient grad1 = new RadialGradient(
            cX, cY,
            maxRadius * 1.5f * scaleBoost,
            new int[]{
                adjustAlpha(0xFFC700FF, 0.45f * alphaScale), 
                adjustAlpha(0xFF7928CA, 0.20f * alphaScale), 
                adjustAlpha(0xFF000000, 0.0f)
            },
            new float[]{0.0f, 0.65f, 1.0f},
            Shader.TileMode.CLAMP
        );
        layerPaint.setShader(grad1);
        canvas.drawPath(path, layerPaint);
        canvas.restore();
        
        canvas.save();
        canvas.translate(dx2, dy2);
        canvas.scale(1.0f * scaleBoost, 1.0f * scaleBoost, cX, cY);
        
        layerPaint.setMaskFilter(null);
        RadialGradient grad2 = new RadialGradient(
            cX, cY,
            maxRadius * 1.2f * scaleBoost,
            new int[]{
                adjustAlpha(0xFFFF8C00, 0.85f * alphaScale), 
                adjustAlpha(0xFFC700FF, 0.4f * alphaScale),  
                adjustAlpha(0xFF000000, 0.0f)
            },
            new float[]{0.0f, 0.55f, 1.0f},
            Shader.TileMode.CLAMP
        );
        layerPaint.setShader(grad2);
        canvas.drawPath(path, layerPaint);
        canvas.restore();

        canvas.save();
        canvas.translate(dx3, dy3);
        canvas.scale(0.82f * scaleBoost, 0.82f * scaleBoost, cX, cY);
        
        RadialGradient grad3 = new RadialGradient(
            cX, cY,
            maxRadius * 0.85f * scaleBoost,
            new int[]{
                adjustAlpha(0xFFFFE600, 1.0f * alphaScale), 
                adjustAlpha(0xFFFF8C00, 0.85f * alphaScale), 
                adjustAlpha(0xFF000000, 0.0f)
            },
            new float[]{0.0f, 0.45f, 1.0f},
            Shader.TileMode.CLAMP
        );
        layerPaint.setShader(grad3);
        canvas.drawPath(path, layerPaint);
        
        canvas.save();
        canvas.scale(0.85f, 0.85f, cX, cY);
        layerPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        RadialGradient plasmaGrad = new RadialGradient(
            cX, cY,
            maxRadius * 0.6f * scaleBoost,
            new int[]{
                adjustAlpha(0xFFFFFFFF, 0.7f * alphaScale), 
                adjustAlpha(0xFFFFE600, 0.3f * alphaScale), 
                adjustAlpha(0xFF000000, 0.0f)
            },
            new float[]{0.0f, 0.6f, 1.0f},
            Shader.TileMode.CLAMP
        );
        layerPaint.setShader(plasmaGrad);
        canvas.drawPath(path, layerPaint);
        canvas.restore();
        
        canvas.restore();

        if (rays != null && alphaScale > 0.05f) {
            Paint rayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            rayPaint.setStyle(Paint.Style.FILL);
            rayPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN)); 
            Path rayPath = new Path();
            
            float blurSoft = maxRadius * 0.08f * scaleBoost;
            
            for (int i = 0; i < NUM_RAYS; i++) {
                Ray ray = rays[i];
                if (ray.currentLength <= 0) continue;
                
                float angle = ray.angle;
                float dirX = (float) Math.cos(angle);
                float dirY = (float) Math.sin(angle);
                float perpX = -dirY;
                float perpY = dirX;
                
                float startX = cX + dx3;
                float startY = cY + dy3;
                
                float R_start = maxRadius * 0.15f * scaleBoost;
                float R_end = ray.currentLength * scaleBoost;
                
                float W_base = maxRadius * 0.03f * scaleBoost;
                float W_tip = maxRadius * 0.28f * scaleBoost; 
                
                float p1X = startX + R_start * dirX - W_base * perpX;
                float p1Y = startY + R_start * dirY - W_base * perpY;
                
                float p2X = startX + R_start * dirX + W_base * perpX;
                float p2Y = startY + R_start * dirY + W_base * perpY;
                
                float p3X = startX + R_end * dirX + W_tip * perpX;
                float p3Y = startY + R_end * dirY + W_tip * perpY;
                
                float p4X = startX + R_end * dirX - W_tip * perpX;
                float p4Y = startY + R_end * dirY - W_tip * perpY;
                
                rayPath.reset();
                rayPath.moveTo(p1X, p1Y);
                rayPath.lineTo(p2X, p2Y);
                rayPath.lineTo(p3X, p3Y);
                rayPath.lineTo(p4X, p4Y);
                rayPath.close();
                
                int startColor = adjustAlpha(0xFFFFE600, 0.45f * alphaScale); 
                int endColor = adjustAlpha(0xFFC700FF, 0.0f); 
                
                LinearGradient rayGrad = new LinearGradient(
                    startX, startY, startX + R_end * dirX, startY + R_end * dirY,
                    startColor, endColor,
                    Shader.TileMode.CLAMP
                );
                
                rayPaint.setShader(rayGrad);
                if (blurSoft > 0.5f) {
                    rayPaint.setMaskFilter(new BlurMaskFilter(blurSoft, BlurMaskFilter.Blur.NORMAL));
                } else {
                    rayPaint.setMaskFilter(null);
                }
                canvas.drawPath(rayPath, rayPaint);
            }
        }
    }

    public void generateBlob() {
        for (int i = 0; i < N; i++) {
            generateBlob(radius, angle, i);
            generateBlob(radiusNext, angleNext, i);
            progress[i] = 0;
        }
    }

    private float animateToAmplitude;
    public float amplitude;
    private float animateAmplitudeDiff;

    private final static float ANIMATION_SPEED_WAVE_HUGE = 0.65f;
    private final static float ANIMATION_SPEED_WAVE_SMALL = 0.45f;
    private final static float animationSpeed = 1f - ANIMATION_SPEED_WAVE_HUGE;
    private final static float animationSpeedTiny = 1f - ANIMATION_SPEED_WAVE_SMALL;

    public void setValue(float value) {
        amplitude = value;
    }

    public void setValue(float value, boolean isBig) {
        animateToAmplitude = value;
        if (!LiteMode.isEnabled(liteFlag)) {
            return;
        }
        if (isBig) {
            if (animateToAmplitude > amplitude) {
                animateAmplitudeDiff = (animateToAmplitude - amplitude) / (100f + 300f * animationSpeed);
            } else {
                animateAmplitudeDiff = (animateToAmplitude - amplitude) / (100 + 500f * animationSpeed);
            }
        } else {
            if (animateToAmplitude > amplitude) {
                animateAmplitudeDiff = (animateToAmplitude - amplitude) / (100f + 400f * animationSpeedTiny);
            } else {
                animateAmplitudeDiff = (animateToAmplitude - amplitude) / (100f + 500f * animationSpeedTiny);
            }
        }
    }

    public void updateAmplitude(long dt) {
        if (animateToAmplitude != amplitude) {
            amplitude += animateAmplitudeDiff * dt;
            if (animateAmplitudeDiff > 0) {
                if (amplitude > animateToAmplitude) {
                    amplitude = animateToAmplitude;
                }
            } else {
                if (amplitude < animateToAmplitude) {
                    amplitude = animateToAmplitude;
                }
            }
        }
    }
}
