package soundgram.animation;

import java.util.Random;

public class Particle {
    private static final Random RANDOM = new Random();
    public static final int STYLE_DOTS = 0;
    public static final int STYLE_SAKURA = 3;
    public static final int STYLE_SNOW = 2;
    public static final int STYLE_SPARKS = 1;
    public static final int STYLE_TEXT = 4;
    public float angularVelocity;
    public int color;
    public float decay;
    public float drag;
    public float gravity;
    public float life;
    public float rotation;
    public float size;
    public float stretch;
    public int style;
    public String text;
    public float vx;
    public float vy;
    public float wind;
    public float wobble;
    public float wobblePhase;
    public float x;
    public float y;

    public Particle(float f, float f2, int i, float f3, float f4, float f5, int i2, String str, float f6) {
        this.life = 1.0f;
        this.x = f;
        this.y = f2;
        this.color = i;
        this.style = i2;
        this.text = (str == null || str.length() == 0) ? "*" : str;
        Random random = RANDOM;
        this.rotation = random.nextFloat() * 360.0f;
        this.wobblePhase = random.nextFloat() * 6.2831855f;
        configure(f3, Math.max(0.1f, f4), Math.max(0.1f, f5), Math.max(0.0f, f6));
    }

    public Particle(float f, float f2, int i, float f3, float f4, float f5, int i2, String str) {
        this(f, f2, i, f3, f4, f5, i2, str, 1.0f);
    }

    public Particle(float f, float f2, int i, float f3, float f4, float f5) {
        this(f, f2, i, f3, f4, f5, 0, null, 1.0f);
    }

    private void configure(float f, float f2, float f3, float f4) {
        float max = Math.max(1.0f, f);
        float f5 = (0.95f * f4) + 0.35f;
        float f6 = (0.7f * f4) + 0.55f;
        int i = this.style;
        if (i == 1) {
            Random random = RANDOM;
            float nextFloat = random.nextFloat() * 6.2831855f;
            float nextFloat2 = ((random.nextFloat() * 3.7f) + 2.2f) * f2;
            double d = nextFloat;
            this.vx = ((float) Math.cos(d)) * nextFloat2 * f5;
            this.vy = ((((float) Math.sin(d)) * nextFloat2) * f6) - (f2 * 1.4f);
            this.size = Math.max(1.0f, ((max / 10.0f) + ((random.nextFloat() * max) / 9.0f)) * f3);
            this.decay = (random.nextFloat() * 0.032f) + 0.024f;
            this.gravity = 0.045f;
            this.drag = 0.018f;
            this.wind = (random.nextFloat() - 0.5f) * 0.018f * f2;
            this.angularVelocity = (random.nextFloat() - 0.5f) * 24.0f;
            this.stretch = (random.nextFloat() * 1.6f) + 1.8f;
            return;
        }
        if (i == 2) {
            Random random2 = RANDOM;
            this.vx = (random2.nextFloat() - 0.5f) * 1.2f * f2 * f5;
            this.vy = ((random2.nextFloat() * 1.4f) - 0.7f) * f2 * f6;
            this.size = Math.max(2.0f, ((max / 9.0f) + ((random2.nextFloat() * max) / 8.0f)) * f3);
            this.decay = (random2.nextFloat() * 0.016f) + 0.01f;
            this.gravity = 0.018f;
            this.drag = 0.01f;
            this.wind = ((random2.nextFloat() * 0.018f) + 0.008f) * f2;
            this.wobble = (random2.nextFloat() * 0.9f) + 0.35f;
            this.angularVelocity = (random2.nextFloat() - 0.5f) * 7.0f;
            this.stretch = 1.0f;
            return;
        }
        if (i == 3) {
            Random random3 = RANDOM;
            float nextFloat3 = (((random3.nextFloat() - 0.38f) * 1.85f) * f4) - 1.5707964f;
            float nextFloat4 = ((random3.nextFloat() * 2.6f) + 1.4f) * f2;
            double d2 = nextFloat3;
            this.vx = (((float) Math.cos(d2)) * nextFloat4 * f5) + (f2 * 0.45f);
            this.vy = (((float) Math.sin(d2)) * nextFloat4 * f6) + (0.25f * f2);
            this.size = Math.max(2.4f, ((max / 6.8f) + ((random3.nextFloat() * max) / 6.5f)) * f3);
            this.decay = (random3.nextFloat() * 0.012f) + 0.007f;
            this.gravity = 0.025f;
            this.drag = 0.01f;
            this.wind = ((random3.nextFloat() * 0.03f) + 0.018f) * f2 * ((f4 * 0.45f) + 0.55f);
            this.wobble = ((random3.nextFloat() * 1.35f) + 0.75f) * ((0.35f * f4) + 0.65f);
            this.angularVelocity = (random3.nextFloat() - 0.5f) * 18.0f;
            this.stretch = (random3.nextFloat() * 0.62f) + 1.6f;
            return;
        }
        if (i != 4) {
            Random random4 = RANDOM;
            this.vx = (random4.nextFloat() - 0.5f) * 5.0f * f2 * f5;
            this.vy = (((-random4.nextFloat()) * 3.4f) - 0.8f) * f2 * f6;
            this.size = Math.max(1.0f, (((random4.nextFloat() * max) / 6.0f) + (max / 12.0f)) * f3);
            this.decay = (random4.nextFloat() * 0.035f) + 0.018f;
            this.gravity = 0.08f;
            this.drag = 0.008f;
            this.wind = 0.0f;
            this.angularVelocity = (random4.nextFloat() - 0.5f) * 10.0f;
            this.stretch = 1.0f;
            return;
        }
        Random random5 = RANDOM;
        float nextFloat5 = ((random5.nextFloat() - 0.5f) * 2.2f) - 1.5707964f;
        float nextFloat6 = ((random5.nextFloat() * 2.4f) + 1.7f) * f2;
        double d3 = nextFloat5;
        this.vx = ((float) Math.cos(d3)) * nextFloat6 * f5;
        this.vy = ((float) Math.sin(d3)) * nextFloat6 * f6;
        this.size = Math.max(4.0f, max * ((random5.nextFloat() * 0.2f) + 0.45f) * f3);
        this.decay = (random5.nextFloat() * 0.02f) + 0.018f;
        this.gravity = 0.055f;
        this.drag = 0.016f;
        this.wind = (random5.nextFloat() - 0.5f) * 0.014f * f2;
        this.angularVelocity = (random5.nextFloat() - 0.5f) * 16.0f;
        this.stretch = 1.0f;
    }

    public boolean update(long j) {
        float max = Math.max(0.25f, Math.min(3.0f, ((float) j) / 16.0f));
        float f = this.wobblePhase + (0.08f * max);
        this.wobblePhase = f;
        this.vx += this.wind * max;
        this.x += (this.vx + (this.wobble == 0.0f ? 0.0f : ((float) Math.sin(f)) * this.wobble)) * max;
        float f2 = this.y;
        float f3 = this.vy;
        this.y = f2 + (f3 * max);
        this.vy = f3 + (this.gravity * max);
        this.vx *= Math.max(0.0f, 1.0f - (this.drag * max));
        this.vy *= Math.max(0.0f, 1.0f - ((this.drag * 0.45f) * max));
        this.rotation += this.angularVelocity * max;
        float f4 = this.life - (this.decay * max);
        this.life = f4;
        return f4 > 0.0f;
    }
}
