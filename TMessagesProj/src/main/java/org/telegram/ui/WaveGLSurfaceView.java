package org.telegram.ui;

import android.content.Context;
import android.media.audiofx.Visualizer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaController;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class WaveGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private static final String VERTEX_SHADER = "" +
            "attribute vec4 aPosition;\n" +
            "void main() {\n" +
            "  gl_Position = aPosition;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER = "" +
            "precision highp float;\n" +
            "\n" +
            "uniform vec2 vScreenSize;\n" +
            "uniform float vTime;\n" +
            "uniform float vScale;\n" +
            "\n" +
            "uniform vec3 vColorBackground;\n" +
            "\n" +
            "uniform vec3 vColor[6];\n" +
            "uniform vec3 vRotation[3];\n" +
            "\n" +
            "uniform float vAudio[3];\n" +
            "uniform float vReact[3];\n" +
            "uniform float vRaysCount;\n" +
            "\n" +
            "uniform vec2 vInteractionPoint;\n" +
            "uniform float vInteraction;\n" +
            "\n" +
            "#define CIRCLE_WIDTH_BASE 0.8\n" +
            "#define CIRCLE_WIDTH_STEP 0.2\n" +
            "\n" +
            "#define SPARK_STRENGTH_BASE 1.0\n" +
            "#define SPARK_STRENGTH_STEP 0.3\n" +
            "\n" +
            "#define CIRCLE_RADIUS_BASE 0.95\n" +
            "#define CIRCLE_RADIUS_STEP 0.15\n" +
            "\n" +
            "#define CIRCLE_OFFSET_BASE 0.0\n" +
            "#define CIRCLE_OFFSET_STEP 1.57\n" +
            "\n" +
            "vec4 permute(vec4 x){return mod(((x*34.0)+1.0)*x, 289.0);}\n" +
            "vec4 taylorInvSqrt(vec4 r){return 1.79284291400159 - 0.85373472095314 * r;}\n" +
            "\n" +
            "float snoise3(vec3 v) {\n" +
            "  const vec2 C = vec2(0.1666667, 0.3333333); // vec2(1.0/6.0, 1.0/3.0)\n" +
            "  const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);\n" +
            "\n" +
            "  // First corner\n" +
            "  vec3 i = floor(v + dot(v, C.yyy));\n" +
            "  vec3 x0 = v - i + dot(i, C.xxx);\n" +
            "\n" +
            "  // Other corners\n" +
            "  vec3 g = step(x0.yzx, x0.xyz);\n" +
            "  vec3 l = 1.0 - g;\n" +
            "  vec3 i1 = min(g.xyz, l.zxy);\n" +
            "  vec3 i2 = max(g.xyz, l.zxy);\n" +
            "\n" +
            "  // x0 = x0 - 0. + 0.0 * C\n" +
            "  vec3 x1 = x0 - i1 + 1.0 * C.xxx;\n" +
            "  vec3 x2 = x0 - i2 + 2.0 * C.xxx;\n" +
            "  vec3 x3 = x0 - 1. + 3.0 * C.xxx;\n" +
            "\n" +
            "  // Permutations\n" +
            "  i = mod(i, 289.0);\n" +
            "  vec4 p = permute( permute( permute(\n" +
            "             i.z + vec4(0.0, i1.z, i2.z, 1.0 ))\n" +
            "           + i.y + vec4(0.0, i1.y, i2.y, 1.0 ))\n" +
            "           + i.x + vec4(0.0, i1.x, i2.x, 1.0 ));\n" +
            "\n" +
            "  // Gradients ( N*N points uniformly over a square, mapped onto an octahedron.)\n" +
            "  // The ring size 17*17 = 289 is close to a multiple of 49 (49*6 = 294)\n" +
            "  float n_ = 0.142857142857; // 1.0/7.0\n" +
            "  vec3 ns = n_ * D.wyz - D.xzx;\n" +
            "\n" +
            "  vec4 j = p - 49.0 * floor(p * ns.z *ns.z); //  mod(p,N*N), N=7\n" +
            "\n" +
            "  vec4 x_ = floor(j * ns.z);\n" +
            "  vec4 y_ = floor(j - 7.0 * x_); // mod(j,N)\n" +
            "\n" +
            "  vec4 x = x_ *ns.x + ns.yyyy;\n" +
            "  vec4 y = y_ *ns.x + ns.yyyy;\n" +
            "  vec4 h = 1.0 - abs(x) - abs(y);\n" +
            "\n" +
            "  vec4 b0 = vec4( x.xy, y.xy );\n" +
            "  vec4 b1 = vec4( x.zw, y.zw );\n" +
            "\n" +
            "  vec4 s0 = floor(b0)*2.0 + 1.0;\n" +
            "  vec4 s1 = floor(b1)*2.0 + 1.0;\n" +
            "  vec4 sh = -step(h, vec4(0.0));\n" +
            "\n" +
            "  vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy;\n" +
            "  vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww;\n" +
            "\n" +
            "  vec3 p0 = vec3(a0.xy,h.x);\n" +
            "  vec3 p1 = vec3(a0.zw,h.y);\n" +
            "  vec3 p2 = vec3(a1.xy,h.z);\n" +
            "  vec3 p3 = vec3(a1.zw,h.w);\n" +
            "\n" +
            "  //Normalise gradients\n" +
            "  vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));\n" +
            "  p0 *= norm.x;\n" +
            "  p1 *= norm.y;\n" +
            "  p2 *= norm.z;\n" +
            "  p3 *= norm.w;\n" +
            "\n" +
            "  // Mix final noise value\n" +
            "  vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);\n" +
            "  m = m * m;\n" +
            "  return 42.0 * dot(m*m, vec4(dot(p0,x0), dot(p1,x1), dot(p2,x2), dot(p3,x3)));\n" +
            "}\n" +
            "\n" +
            "float tri(in float x){return abs(fract(x)-.5);}\n" +
            "vec3 tri3(in vec3 p){return vec3( tri(p.z+tri(p.y*20.)), tri(p.z+tri(p.x*1.)), tri(p.y+tri(p.x*1.)));}\n" +
            "\n" +
            "float triNoise3D(in vec3 p, in float spd)\n" +
            "{\n" +
            "  float z=0.4;\n" +
            "  float rz = 0.1;\n" +
            "  vec3 bp = p;\n" +
            "  for (float i=0.; i<=4.; i++ )\n" +
            "  {\n" +
            "    vec3 dg = tri3(bp*0.01);\n" +
            "    p += (dg+vTime*.1*spd);\n" +
            "\n" +
            "    bp *= 4.;\n" +
            "    z *= 0.9;\n" +
            "    p *= 1.6;\n" +
            "\n" +
            "    rz+= (tri(p.z+tri(0.6*p.x+0.1*tri(p.y))))/z;\n" +
            "  }\n" +
            "  return smoothstep(0.0, 8., rz + sin(rz + sin(z) * 2.8) * 2.2);\n" +
            "}\n" +
            "\n" +
            "vec2 rotate(vec2 p, float a) {\n" +
            "  float s = sin(a);\n" +
            "  float c = cos(a);\n" +
            "  return vec2(p.x * c - p.y * s, p.x * s + p.y * c);\n" +
            "}\n" +
            "\n" +
            "float light(float intensity, float attenuation, float dist) {\n" +
            "  return intensity / (1.0 + dist + dist * attenuation);\n" +
            "}\n" +
            "\n" +
            "vec4 makeNoiseBlob2(vec2 uv, vec3 color1, vec3 color2, float strength, float offset) {\n" +
            "  float len = length(uv);\n" +
            "  float v0, v1, cl;\n" +
            "  float r0, d0, n0;\n" +
            "  float r, d;\n" +
            "\n" +
            "  n0 = snoise3( vec3(uv * 1.2 + offset, vTime * 0.5 + offset) ) * 0.5 + 0.5;\n" +
            "  r0 = mix(0.0, 1.0, n0);\n" +
            "  d0 = distance(uv, r0 / len * uv);\n" +
            "  v0 = smoothstep(r0 + 0.1 + (sin(vTime + offset) + 1.0), r0, len);\n" +
            "\n" +
            "  v1 = light(0.15 * (1.0 + 1.5 * (-sin(vTime * 2. + offset * 0.5) * 0.5)) + 0.3 * strength, 10.0 , d0);\n" +
            "\n" +
            "  vec3 col = mix(color1, color2, uv.y * 2.);\n" +
            "  col = col + v1;\n" +
            "  col.rgb = clamp(col.rgb, 0.0, 1.0);\n" +
            "  return vec4(col, v0);\n" +
            "}\n" +
            "\n" +
            "vec4 makeBlob(vec2 uv,\n" +
            "              float blob,\n" +
            "              vec3 color1,\n" +
            "              vec3 color2,\n" +
            "              float width,\n" +
            "              float baseReaction,\n" +
            "              float likeReaction,\n" +
            "              float audioStrength,\n" +
            "              float offset,\n" +
            "              vec2 noiseOffset) {\n" +
            "  float len = length(uv);\n" +
            "\n" +
            "  float outerRadius = blob + width * 0.5 + baseReaction * (1.0 + max(likeReaction, audioStrength * 0.6) * 50. * baseReaction);\n" +
            "\n" +
            "  float strength = max(likeReaction, audioStrength);\n" +
            "\n" +
            "  vec4 noise = makeNoiseBlob2(uv * (1.0 - likeReaction * 0.5) + noiseOffset, color1, color2, strength, offset);\n" +
            "  noise.a = mix(0.0, noise.a, smoothstep(outerRadius, 0.5, len));\n" +
            "  noise.rgb += 0.6 * likeReaction * (1.0 - smoothstep(0.2, outerRadius * 0.8, len));\n" +
            "\n" +
            "  return noise;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "  vec2 uv = gl_FragCoord.xy / vScreenSize.xy;\n" +
            "\n" +
            "  uv = uv * 2.0 - 1.0;\n" +
            "  uv.y *= vScreenSize.y / min(vScreenSize.x, vScreenSize.y) / vScale;\n" +
            "  uv.x *= vScreenSize.x / min(vScreenSize.x, vScreenSize.y) / vScale;\n" +
            "\n" +
            "  vec2 ruv = uv * 2.0;\n" +
            "  float pr = length(ruv);\n" +
            "  float pa = atan(ruv.y, ruv.x);\n" +
            "\n" +
            "  float idx = (pa/3.1415) / 2.0;   // 0 to 1\n" +
            "\n" +
            "  vec2 ruv1 = rotate(uv * 2.0, 3.1415);\n" +
            "  float pa1 = atan(ruv1.y, ruv1.x);\n" +
            "  float idx1 = (pa1/3.1415) / 2.0;   // 0 to 1\n" +
            "  float idx21 = (pa1/3.1415 + 1.0) / 2.0 * 3.1415; // 0 to PI\n" +
            "\n" +
            "  float spark = triNoise3D(vec3(idx * vRaysCount, 0.0, 0.0), 0.1);\n" +
            "  spark = mix(spark, triNoise3D(vec3(idx1 * vRaysCount, 0.0, idx1 * vRaysCount), 0.1), smoothstep(0.9, 1.0, sin(idx21)));\n" +
            "  spark = spark * 0.2 + pow(spark, 10.);\n" +
            "  spark = smoothstep(0.0, spark, 0.3) * spark;\n" +
            "\n" +
            "  vec4 color = vec4(vColorBackground, 1.0); // Opaque background using theme color\n" +
            "  vec4 blobColor;\n" +
            "  float floatIndex;\n" +
            "  float radius;\n" +
            "\n" +
            "  float n0 = snoise3(vec3(uv * 1.2, vTime * 0.5));\n" +
            "\n" +
            "  for (int i = 0; i < 3; i++) {\n" +
            "    floatIndex = float(i);\n" +
            "    radius = CIRCLE_RADIUS_BASE - CIRCLE_RADIUS_STEP * floatIndex;\n" +
            "    blobColor = makeBlob(uv,\n" +
            "                         mix(radius, radius + 0.3, n0),\n" +
            "                         vColor[i],\n" +
            "                         vColor[i+3],\n" +
            "                         CIRCLE_WIDTH_BASE - CIRCLE_WIDTH_STEP * floatIndex,\n" +
            "                         (SPARK_STRENGTH_BASE - SPARK_STRENGTH_STEP * floatIndex) * spark,\n" +
            "                         vReact[i],\n" +
            "                         vAudio[i],\n" +
            "                         CIRCLE_OFFSET_BASE + CIRCLE_OFFSET_STEP * floatIndex,\n" +
            "                         rotate(vRotation[i].xy, vTime * vRotation[i].z));\n" +
            "\n" +
            "    color.rgb = mix(color.rgb, blobColor.rgb, blobColor.a); \n" +
            "    color.a = max(blobColor.a, color.a);\n" +
            "  }\n" +
            "\n" +
            "  // Premultiply alpha for Android SurfaceFlinger\n" +
            "  gl_FragColor = vec4(color.rgb * color.a, color.a);\n" +
            "}\n";

    private FloatBuffer vertexBuffer;
    private int programId;
    
    // Uniform Locations
    private int uScreenSize;
    private int uTime;
    private int uScale;
    private int uColorBackground;
    private int uColor;
    private int uRotation;
    private int uAudio;
    private int uReact;
    private int uRaysCount;

    private int width, height;

    private float dynamicTime = 0f;
    private long lastFrameTime;

    // Visualizer Audio
    private Visualizer visualizer;
    private static final int SPECTRUM_BANDS = 48;
    private float[] currentAudio = new float[3];
    private float[] targetAudio = new float[3];

    // Like Animation React
    private float[] vReactData = new float[3];
    private long[] reactTimers = new long[3];
    private boolean isLiking = false;

    // Fixed colors and rotation
    private float[] vColorData = {
            255/255f, 50/255f,  150/255f,  // Layer 0: Pink
            255/255f, 200/255f, 0/255f,    // Layer 0: Yellow
            255/255f, 0/255f,   200/255f,  // Layer 1: Deep Pink
            255/255f, 255/255f, 0/255f,    // Layer 1: Bright Yellow
            255/255f, 100/255f, 200/255f,  // Layer 2: Light Pink
            255/255f, 150/255f, 0/255f     // Layer 2: Orange-Yellow
    };
    
    private float[] vRotationData = new float[]{
            0f, 0f, 0.2f,
            0f, 0f, 0.15f,
            0f, 0f, 0.1f
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        org.telegram.ui.Components.AudioVisualizerDrawable.setGlobalListener((playing, animate, waveform) -> {
            if (waveform != null && waveform.length >= 7) {
                float low = waveform[0] + waveform[1];
                float mid = waveform[2] + waveform[3];
                float high = waveform[4] + waveform[5];

                // Многократно повышаем чувствительность!
                targetAudio[0] = Math.min(1.0f, low * 1.5f); // Bass
                targetAudio[1] = Math.min(1.0f, mid * 1.5f); // Mid
                targetAudio[2] = Math.min(1.0f, high * 1.5f); // High
            } else {
                targetAudio[0] = targetAudio[1] = targetAudio[2] = 0f;
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        org.telegram.ui.Components.AudioVisualizerDrawable.setGlobalListener(null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        if (w > 0 && h > 0) {
            // Оптимизация GPU: уменьшаем разрешение рендера в 3 раза
            // Hardware Scaler сделает картинку гладкой, а нагрузка на GPU упадет в 9 раз
            getHolder().setFixedSize(w / 3, h / 3);
        }
    }

    public WaveGLSurfaceView(Context context) {
        super(context);
        
        setEGLContextClientVersion(2);
        
        // ZOrderMediaOverlay guarantees it's drawn behind the UI elements but above the window background
        setZOrderMediaOverlay(true);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(android.graphics.PixelFormat.TRANSLUCENT);

        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Quad vertices
        float[] coords = {
                -1.0f,  1.0f,
                -1.0f, -1.0f,
                 1.0f,  1.0f,
                 1.0f, -1.0f
        };
        vertexBuffer = ByteBuffer.allocateDirect(coords.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(coords).position(0);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            FileLog.e("SoundGram GL: Shader compilation failed: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // Transparent clear
        
        // Enable blending
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        programId = GLES20.glCreateProgram();
        GLES20.glAttachShader(programId, vertexShader);
        GLES20.glAttachShader(programId, fragmentShader);
        GLES20.glLinkProgram(programId);

        uScreenSize = GLES20.glGetUniformLocation(programId, "vScreenSize");
        uTime = GLES20.glGetUniformLocation(programId, "vTime");
        uScale = GLES20.glGetUniformLocation(programId, "vScale");
        uColorBackground = GLES20.glGetUniformLocation(programId, "vColorBackground");
        uColor = GLES20.glGetUniformLocation(programId, "vColor");
        uRotation = GLES20.glGetUniformLocation(programId, "vRotation");
        uAudio = GLES20.glGetUniformLocation(programId, "vAudio");
        uReact = GLES20.glGetUniformLocation(programId, "vReact");
        uRaysCount = GLES20.glGetUniformLocation(programId, "vRaysCount");
        
        lastFrameTime = SystemClock.elapsedRealtime();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now = SystemClock.elapsedRealtime();
        float dt = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;
        
        boolean isPlaying = MediaController.getInstance().getPlayingMessageObject() != null
                && !MediaController.getInstance().isMessagePaused();

        if (!isPlaying) {
            for (int i = 0; i < 3; i++) targetAudio[i] = 0f;
        }

        // Audio Smoothing
        float decay = 0.02f;
        float totalEnergy = 0f;
        for(int i = 0; i < 3; i++) {
            float target = targetAudio[i];
            if (target > currentAudio[i]) {
                currentAudio[i] = target;
            } else {
                currentAudio[i] = currentAudio[i] - Math.min(decay, currentAudio[i] - target);
            }
            if (!isPlaying) currentAudio[i] *= 0.95f;
            totalEnergy += currentAudio[i];
        }
        // Ограничиваем увеличение скорости до 1.3x
        dynamicTime += dt * (1.0f + Math.min(0.3f, totalEnergy * 0.3f));

        // Like Animation Cascade
        for (int i = 0; i < 3; i++) {
            if (isLiking) {
                long elapsed = now - reactTimers[i];
                if (elapsed >= 0 && elapsed < 850) {
                    vReactData[i] = 0.7f;
                } else if (elapsed >= 850) {
                    vReactData[i] = Math.max(0f, vReactData[i] - 0.05f);
                }
            }
        }
        if (isLiking && (now - reactTimers[2] > 1500)) {
            isLiking = false;
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(programId);

        GLES20.glUniform2f(uScreenSize, width, height);
        GLES20.glUniform1f(uTime, dynamicTime);
        GLES20.glUniform1f(uScale, 0.7f * SoundGramStyleSettingsFragment.getWaveSphereSizeMultiplier());
        
        int bgColor = org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_windowBackgroundWhite);
        GLES20.glUniform3f(uColorBackground, android.graphics.Color.red(bgColor)/255f, android.graphics.Color.green(bgColor)/255f, android.graphics.Color.blue(bgColor)/255f);

        GLES20.glUniform3fv(uColor, 6, vColorData, 0);
        GLES20.glUniform3fv(uRotation, 3, vRotationData, 0);
        GLES20.glUniform1fv(uAudio, 3, currentAudio, 0);
        GLES20.glUniform1fv(uReact, 3, vReactData, 0);
        GLES20.glUniform1f(uRaysCount, SoundGramStyleSettingsFragment.getWaveRaysMultiplier());

        int positionHandle = GLES20.glGetAttribLocation(programId, "aPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    public void linkToAudioSession(int sessionId) {
        // Мы больше не используем Visualizer, так как получаем данные напрямую из Telegram!
    }

    private void processFft(byte[] fft) {
        // Мы больше не используем FFT, так как Telegram дает готовый waveform
    }

    public void playLikeAnimation() {
        isLiking = true;
        long now = SystemClock.elapsedRealtime();
        reactTimers[0] = now;
        reactTimers[1] = now + 100;
        reactTimers[2] = now + 150;
    }

    public void startAnimation() {
        // GLSurfaceView updates continuously when resumed
    }

    public void stopAnimation() {
        releaseVisualizer();
        for (int i = 0; i < 3; i++) targetAudio[i] = 0f;
    }

    public void releaseVisualizer() {
        if (visualizer != null) {
            try { visualizer.setEnabled(false); visualizer.release(); } catch (Exception e) {}
            visualizer = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseVisualizer();
        for (int i = 0; i < 3; i++) targetAudio[i] = 0f;
    }

    @Override
    public void onResume() {
        super.onResume();
        lastFrameTime = SystemClock.elapsedRealtime();
    }
}
