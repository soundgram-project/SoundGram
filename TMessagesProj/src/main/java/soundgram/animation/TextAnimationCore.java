package soundgram.animation;

import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.method.PasswordTransformationMethod;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.ReplacementSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import org.json.JSONObject;

public class TextAnimationCore {
    private static final Object LOCK = new Object();
    private static TextAnimationCore instance;

    public static class AnimHiddenSpan extends ForegroundColorSpan {
        public AnimHiddenSpan() {
            super(0);
        }
    }

    public boolean isEnabled() {
        return this.initialized && this.enabled;
    }

    public void cleanupSpans(View view) {
        removeHiddenSpans(view, this.states.get(view));
    }

    public void cleanupAllSpans() {
        for (Map.Entry<View, AnimationState> entry : new ArrayList<>(this.states.entrySet())) {
            restoreSystemCursor(entry.getKey(), entry.getValue());
            restoreSystemSelectionHandles((EditText) entry.getKey(), entry.getValue());
            removeHiddenSpans(entry.getKey(), entry.getValue());
        }
    }
    private Class<?> chatActivityEditTextCaptionClass;
    private Class<?> editTextBoldCursorClass;
    private Class<?> editTextCaptionClass;
    private Class<?> editTextEffectsClass;
    private Field fieldResourcesProvider;
    private Integer keyChatMessagePanelCursor;
    private Method textViewGetVerticalOffsetMethod;
    private Class<?> themeClass;
    private Method themeGetColorMethod;
    private Method themeGetColorWithProviderMethod;
    private boolean initialized = false;
    private boolean enabled = true;
    private int duration = 300;
    private boolean blurEnabled = true;
    private int blurDuration = 300;
    private int blurRadius = 10;
    private int blurTextDelay = 20;
    private boolean slideEnabled = true;
    private int slideDist = 20;
    private boolean scaleEnabled = false;
    private float scaleStart = 0.0f;
    private boolean rotateEnabled = false;
    private int rotateAngle = -15;
    private boolean deleteAnimEnabled = true;
    private int particleCount = 5;
    private float particleSpeed = 1.0f;
    private float particleSize = 1.0f;
    private float particleSpread = 1.0f;
    private int particleStyle = 0;
    private boolean cursorEnabled = true;
    private int cursorSpeed = 25;
    private int cursorWidth = 5;
    private boolean liquidCursorEnabled = false;
    private int liquidScaleFactor = 15;
    private int selectionCursorEffect = 0;
    private int selectionLiquidStretch = 60;
    private int selectionLiquidSide = 50;
    private boolean ignoreSpaces = true;
    private boolean animateAllLines = false;
    private boolean debugMode = false;
    private final WeakHashMap<View, AnimationState> states = new WeakHashMap<>();

    public static void start() {
        getInstance().startInternal();
    }

    public static void updateSettings(String str) {
        getInstance().updateSettingsInternal(str);
    }

    public static void unload() {
        synchronized (LOCK) {
            TextAnimationCore textAnimationCore = instance;
            if (textAnimationCore != null) {
                textAnimationCore.cleanup();
                instance = null;
            }
        }
    }

    public static TextAnimationCore getInstance() {
        TextAnimationCore textAnimationCore;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new TextAnimationCore();
            }
            textAnimationCore = instance;
        }
        return textAnimationCore;
    }

    private void startInternal() {
        if (this.initialized) {
            return;
        }
        try {
            this.editTextEffectsClass = loadClassSafely("org.telegram.ui.Components.EditTextEffects");
            this.editTextBoldCursorClass = loadClassSafely("org.telegram.ui.Components.EditTextBoldCursor");
            this.editTextCaptionClass = loadClassSafely("org.telegram.ui.Components.EditTextCaption");
            this.chatActivityEditTextCaptionClass = loadClassSafely("org.telegram.ui.Components.ChatActivityEnterView$ChatActivityEditTextCaption");
            findThemeMembers();
            this.initialized = true;
            log("TextAnimationCore started natively");
        } catch (Throwable th) {
            log("Start error: " + th.getMessage());
        }
    }

    private void updateSettingsInternal(String str) {
        try {
            if (str == null) {
                str = "{}";
            }
            JSONObject jSONObject = new JSONObject(str);
            boolean wasEnabled = this.enabled;
            this.enabled = optBoolean(jSONObject, "enabled", true);
            if (wasEnabled && !this.enabled) {
                cleanupAllSpans();
            }
            this.duration = Math.max(1, optInt(jSONObject, "duration", 300));
            this.blurEnabled = optBoolean(jSONObject, "blur_enabled", true);
            this.blurDuration = Math.max(1, optInt(jSONObject, "blur_duration", 300));
            this.blurRadius = Math.max(0, optInt(jSONObject, "blur_radius", 10));
            this.blurTextDelay = Math.max(0, Math.min(99, optInt(jSONObject, "blur_text_delay", 20)));
            this.slideEnabled = optBoolean(jSONObject, "slide_enabled", true);
            this.slideDist = optInt(jSONObject, "slide_dist", 20);
            this.scaleEnabled = optBoolean(jSONObject, "scale_enabled", false);
            this.scaleStart = Math.max(0.0f, Math.min(2.0f, optFloat(jSONObject, "scale_start", 0.0f)));
            this.rotateEnabled = optBoolean(jSONObject, "rotate_enabled", false);
            this.rotateAngle = optInt(jSONObject, "rotate_angle", -15);
            this.deleteAnimEnabled = optBoolean(jSONObject, "delete_anim_enabled", true);
            this.particleCount = Math.max(0, Math.min(80, optInt(jSONObject, "particle_count", 5)));
            this.particleSpeed = Math.max(0.1f, optFloat(jSONObject, "particle_speed", 50.0f) / 50.0f);
            this.particleSize = Math.max(0.1f, optFloat(jSONObject, "particle_size", 50.0f) / 50.0f);
            this.particleSpread = Math.max(0.0f, Math.min(2.0f, optFloat(jSONObject, "particle_spread", 50.0f) / 50.0f));
            this.particleStyle = Math.max(0, Math.min(4, optInt(jSONObject, "particle_style", 0)));
            this.cursorEnabled = optBoolean(jSONObject, "cursor_enabled", true);
            this.cursorSpeed = Math.max(1, Math.min(100, optInt(jSONObject, "cursor_speed", 25)));
            this.cursorWidth = Math.max(1, Math.min(12, optInt(jSONObject, "cursor_width", 5)));
            this.liquidCursorEnabled = optBoolean(jSONObject, "liquid_cursor_enabled", false);
            this.liquidScaleFactor = Math.max(0, Math.min(80, optInt(jSONObject, "liquid_scale_factor", 15)));
            this.selectionCursorEffect = Math.max(0, Math.min(1, optInt(jSONObject, "selection_cursor_effect", 0)));
            this.selectionLiquidStretch = Math.max(0, Math.min(240, optInt(jSONObject, "selection_liquid_stretch", 60)));
            this.selectionLiquidSide = Math.max(0, Math.min(240, optInt(jSONObject, "selection_liquid_side", 50)));
            this.ignoreSpaces = optBoolean(jSONObject, "ignore_spaces", true);
            this.animateAllLines = optBoolean(jSONObject, "animate_all_lines", false);
            this.debugMode = optBoolean(jSONObject, "debug_mode", false);
            invalidateTrackedViews();
        } catch (Throwable th) {
            log("Settings error: " + th.getMessage());
        }
    }

    private boolean optBoolean(JSONObject jSONObject, String str, boolean z) {
        Object opt = jSONObject.opt(str);
        if (opt instanceof Boolean) {
            return ((Boolean) opt).booleanValue();
        }
        if (opt instanceof String) {
            return Boolean.parseBoolean((String) opt);
        }
        return jSONObject.optBoolean(str, z);
    }

    private int optInt(JSONObject jSONObject, String str, int i) {
        Object opt = jSONObject.opt(str);
        if (opt instanceof Number) {
            return ((Number) opt).intValue();
        }
        if (opt instanceof String) {
            try {
                return Integer.parseInt(((String) opt).trim());
            } catch (Exception e) {
            }
        }
        return i;
    }

    private float optFloat(JSONObject jSONObject, String str, float f) {
        Object opt = jSONObject.opt(str);
        if (opt instanceof Number) {
            return ((Number) opt).floatValue();
        }
        if (opt instanceof String) {
            try {
                return Float.parseFloat(((String) opt).trim());
            } catch (Exception e) {
            }
        }
        return f;
    }

    private void cleanup() {
        Iterator it2 = new ArrayList(this.states.entrySet()).iterator();
        while (it2.hasNext()) {
            Map.Entry entry = (Map.Entry) it2.next();
            restoreSystemCursor((View) entry.getKey(), (AnimationState) entry.getValue());
            removeHiddenSpans((View) entry.getKey(), (AnimationState) entry.getValue());
        }
        this.states.clear();
        this.initialized = false;
        log("TextAnimationCore unloaded natively");
    }

    private Class<?> loadClassSafely(String str) {
        try {
            return Class.forName(str);
        } catch (Throwable th) {
            return null;
        }
    }

    private Field findFieldInHierarchy(Class<?> cls, String str) {
        while (cls != null) {
            try {
                Field declaredField = cls.getDeclaredField(str);
                declaredField.setAccessible(true);
                return declaredField;
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            } catch (Throwable th) {
                return null;
            }
        }
        return null;
    }

    private Method findMethodInHierarchy(Class<?> cls, String str, Class<?>... clsArr) {
        while (cls != null) {
            try {
                Method declaredMethod = cls.getDeclaredMethod(str, clsArr);
                declaredMethod.setAccessible(true);
                return declaredMethod;
            } catch (NoSuchMethodException e) {
                cls = cls.getSuperclass();
            } catch (Throwable th) {
                return null;
            }
        }
        return null;
    }

    private void findThemeMembers() {
        Field findFieldInHierarchy = findFieldInHierarchy(this.chatActivityEditTextCaptionClass, "resourcesProvider");
        this.fieldResourcesProvider = findFieldInHierarchy;
        if (findFieldInHierarchy == null) {
            this.fieldResourcesProvider = findFieldInHierarchy(this.editTextCaptionClass, "resourcesProvider");
        }
        try {
            Method declaredMethod = TextView.class.getDeclaredMethod("getVerticalOffset", Boolean.TYPE);
            this.textViewGetVerticalOffsetMethod = declaredMethod;
            declaredMethod.setAccessible(true);
        } catch (Throwable th) {
        }
        try {
            Class<?> cls = Class.forName("org.telegram.ui.ActionBar.Theme");
            this.themeClass = cls;
            this.keyChatMessagePanelCursor = (Integer) cls.getField("key_chat_messagePanelCursor").get(null);
            this.themeGetColorMethod = this.themeClass.getMethod("getColor", Integer.TYPE);
            this.themeGetColorWithProviderMethod = this.themeClass.getMethod("getColor", Integer.TYPE, Class.forName("org.telegram.ui.ActionBar.Theme$ResourcesProvider"));
        } catch (Throwable th2) {
            log("Theme lookup error: " + th2.getMessage());
        }
    }

    private boolean isTelegramStyledEditText(View view) {
        Class<?> cls;
        Class<?> cls2;
        return view != null && (((cls = this.editTextEffectsClass) != null && cls.isInstance(view)) || ((cls2 = this.editTextBoldCursorClass) != null && cls2.isInstance(view)));
    }

    private boolean shouldHandleHookedView(View view, boolean z) {
        if (view instanceof EditText) {
            return (z && isTelegramStyledEditText(view)) ? false : true;
        }
        return false;
    }

    private AnimationState getState(View view) {
        AnimationState animationState = this.states.get(view);
        if (animationState == null) {
            AnimationState animationState2 = new AnimationState();
            this.states.put(view, animationState2);
            return animationState2;
        }
        return animationState;
    }

    private boolean isSensitiveInput(EditText editText) {
        return editText != null && (editText.getTransformationMethod() instanceof PasswordTransformationMethod);
    }

    private int getLastLineStart(EditText editText) {
        try {
            Layout layout = editText.getLayout();
            if (layout != null && layout.getLineCount() > 0) {
                return layout.getLineStart(layout.getLineCount() - 1);
            }
            return 0;
        } catch (Throwable th) {
            return 0;
        }
    }

    public static void beforeDraw(View view, Canvas canvas) {
        TextAnimationCore core = getInstance();
        if (!core.initialized || !core.enabled) {
            core.cleanupSpans(view);
            return;
        }

        org.telegram.ui.Components.EditTextBoldCursor boldCursor =
            (view instanceof org.telegram.ui.Components.EditTextBoldCursor) ?
            (org.telegram.ui.Components.EditTextBoldCursor) view : null;

        int savedIgnore = 0;
        if (boldCursor != null) {
            savedIgnore = boldCursor.ignoreTopCount;
            boldCursor.ignoreTopCount += 5;
        }

        try {
            AnimationState state = core.getState(view);
            state.drawingDepth++;
            try {
                if (core.cursorEnabled) {
                    core.hideSystemCursor((EditText) view, state);
                    core.hideSystemSelectionHandles((EditText) view, state);
                    if (view instanceof org.telegram.ui.Components.EditTextBoldCursor) {
                        ((org.telegram.ui.Components.EditTextBoldCursor) view).setAllowDrawCursor(false);
                    }
                } else {
                    core.restoreSystemCursor(view, state);
                    core.restoreSystemSelectionHandles((EditText) view, state);
                    if (view instanceof org.telegram.ui.Components.EditTextBoldCursor) {
                        ((org.telegram.ui.Components.EditTextBoldCursor) view).setAllowDrawCursor(true);
                    }
                }
                EditText editText = (EditText) view;
                Editable text = editText.getText();
                if (text == null) {
                    return;
                }
                String charSequence = text.toString();
                int length = charSequence.length();
                int lastLineStart = core.getLastLineStart(editText);
                if (core.isSensitiveInput(editText)) {
                    core.resetAnimationState(editText, state, charSequence, length, lastLineStart);
                    return;
                }
                if (!core.animateAllLines && lastLineStart != state.prevLastLineStart) {
                    core.resetAnimationState(editText, state, charSequence, length, lastLineStart);
                    return;
                }
                int min = Math.min(length, state.prevLen);
                int i = 0;
                while (i < min && charSequence.charAt(i) == state.prevText.charAt(i)) {
                    i++;
                }
                int i2 = 0;
                while (i2 < length - i && i2 < state.prevLen - i && charSequence.charAt((length - 1) - i2) == state.prevText.charAt((state.prevLen - 1) - i2)) {
                    i2++;
                }
                int i3 = state.prevLen - i2;
                int i4 = length - i2;
                int i5 = i3 - i;
                int i6 = i4 - i;
                boolean z;
                if (i5 <= 0 && i6 <= 0) {
                    z = false;
                } else {
                    z = true;
                    if (i5 > 0 && core.deleteAnimEnabled) {
                        core.spawnDeleteParticles(editText, state, i, state.prevText.substring(i, i3));
                    }
                    if (!state.charStartTimes.isEmpty()) {
                        core.shiftExistingAnimations(state, length, state.prevLen, i, i2, i6 - i5);
                    }
                    if (!core.animateAllLines && !state.charStartTimes.isEmpty()) {
                        Iterator<Integer> it = state.charStartTimes.keySet().iterator();
                        while (it.hasNext()) {
                            if (it.next() < lastLineStart) {
                                it.remove();
                            }
                        }
                    }
                    if (i6 > 0) {
                        core.addInsertedAnimations(editText, state, charSequence, i, i4, lastLineStart);
                        core.triggerSpaceCursorJump(editText, state, charSequence, i, i4);
                        core.triggerEnterCursorDive(editText, state, charSequence, i, i4);
                    } else if (i5 > 0) {
                        core.triggerBackspaceCursorPulse(editText, state);
                    }
                    if (state.charStartTimes.isEmpty() && state.particles.isEmpty()) {
                        state.hasAnimatingChars = false;
                        core.removeHiddenSpans(editText, state);
                    } else {
                        state.hasAnimatingChars = true;
                        core.updateHiddenSpans(editText, state);
                        core.startAnimationLoop(editText, state);
                    }
                }
                state.prevText = charSequence;
                state.prevLen = length;
                state.prevLastLineStart = lastLineStart;
            } catch (Throwable th) {
                if (core.debugMode) {
                    core.log("before draw error: " + th.getMessage());
                }
            }
        } finally {
            if (boldCursor != null) {
                boldCursor.ignoreTopCount = savedIgnore;
            }
        }
    }

    public static void afterDraw(View view, Canvas canvas) {
        TextAnimationCore core = getInstance();
        if (!core.initialized || !core.enabled) {
            return;
        }
        AnimationState state = core.getState(view);
        state.drawingDepth = Math.max(0, state.drawingDepth - 1);
        if (state.drawingDepth > 0) {
            return;
        }
        try {
            EditText editText = (EditText) view;
            if (state.hasAnimatingChars) {
                core.drawAnimatedChars(editText, canvas, state);
            }
            if (!state.particles.isEmpty()) {
                core.drawParticles(canvas, state);
            }
            if (core.cursorEnabled) {
                core.drawCursor(editText, canvas, state);
            }
        } catch (Throwable th) {
            if (core.debugMode) {
                core.log("after draw error: " + th.getMessage());
            }
        }
    }

    private void resetAnimationState(EditText editText, AnimationState animationState, String str, int i, int i2) {
        animationState.charStartTimes.clear();
        animationState.particles.clear();
        animationState.lastParticleUpdateTime = 0L;
        animationState.cursorMotion = 0.0f;
        animationState.spaceJumpStartTime = 0L;
        animationState.enterDiveStartTime = 0L;
        animationState.backspacePulseStartTime = 0L;
        animationState.selectionStretchStartTime = 0L;
        animationState.selectionStretchBoost = 0.0f;
        animationState.lastSelectionOffset = -1;
        animationState.lastSelectionStart = -1;
        animationState.lastSelectionEnd = -1;
        animationState.selectionEffectStartTime = 0L;
        animationState.selectionStartX = -1.0f;
        animationState.selectionStartY = -1.0f;
        animationState.selectionEndX = -1.0f;
        animationState.selectionEndY = -1.0f;
        animationState.selectionStartSnap = 0.0f;
        animationState.selectionEndSnap = 0.0f;
        animationState.hasAnimatingChars = false;
        animationState.animationRunning = false;
        removeHiddenSpans(editText, animationState);
        animationState.prevText = str;
        animationState.prevLen = i;
        animationState.prevLastLineStart = i2;
    }

    private void shiftExistingAnimations(AnimationState animationState, int i, int i2, int i3, int i4, int i5) {
        Iterator<Map.Entry<Integer, CharAnimData>> it = animationState.charStartTimes.entrySet().iterator();
        HashMap<Integer, CharAnimData> hashMap = null;
        while (it.hasNext()) {
            Map.Entry<Integer, CharAnimData> next = it.next();
            int intValue = next.getKey();
            if (intValue >= i3) {
                if (intValue >= i2 - i4 && i5 != 0) {
                    int i6 = intValue + i5;
                    it.remove();
                    if (i6 >= 0 && i6 < i) {
                        if (hashMap == null) {
                            hashMap = new HashMap<>();
                        }
                        hashMap.put(i6, next.getValue());
                    }
                } else {
                    it.remove();
                }
            }
        }
        if (hashMap != null) {
            animationState.charStartTimes.putAll(hashMap);
        }
    }

    private void addInsertedAnimations(EditText editText, AnimationState animationState, String str, int i, int i2, int i3) {
        Editable text = editText.getText();
        Spannable editable = text instanceof Spannable ? (Spannable) text : null;
        long currentTimeMillis = System.currentTimeMillis();
        while (i < i2) {
            int codePointAt = str.codePointAt(i);
            int charCount = Character.charCount(codePointAt);
            int i4 = i + charCount;
            String substring = str.substring(i, i4);
            if ((this.animateAllLines || i >= i3) && ((!this.ignoreSpaces || !substring.trim().isEmpty()) && !isEmojiLike(editable, i, i4, codePointAt))) {
                animationState.charStartTimes.put(i, new CharAnimData(currentTimeMillis, substring, charCount));
            }
            i = i4;
        }
    }

    private void triggerSpaceCursorJump(EditText editText, AnimationState animationState, String str, int i, int i2) {
        if (!this.cursorEnabled || !this.liquidCursorEnabled || editText == null || str == null) {
            return;
        }
        try {
            Layout layout = editText.getLayout();
            if (layout == null) {
                return;
            }
            int max = Math.max(0, i);
            int min = Math.min(str.length(), Math.max(i, i2));
            while (true) {
                if (max >= min) {
                    max = -1;
                    break;
                }
                int codePointAt = str.codePointAt(max);
                if (codePointAt == 32) {
                    break;
                } else {
                    max += Character.charCount(codePointAt);
                }
            }
            if (max >= 0) {
                int max2 = Math.max(0, Math.min(max, editText.getText().length()));
                int lineForOffset = layout.getLineForOffset(max2);
                float paddingLeft = (editText.getPaddingLeft() + layout.getPrimaryHorizontal(max2)) - editText.getScrollX();
                float extendedPaddingTop = getDrawingExtendedPaddingTop(editText) + getVerticalOffset(editText) + layout.getLineTop(lineForOffset);
                float extendedPaddingTop2 = getDrawingExtendedPaddingTop(editText) + getVerticalOffset(editText) + layout.getLineBottom(lineForOffset);
                float lineSpacingExtra = editText.getLineSpacingExtra();
                if (lineSpacingExtra != 0 && lineForOffset < layout.getLineCount() - 1) {
                    extendedPaddingTop2 -= lineSpacingExtra;
                }
                float max3 = Math.max(1.0f, extendedPaddingTop2 - extendedPaddingTop);
                animationState.spaceJumpX = paddingLeft;
                animationState.spaceJumpY = extendedPaddingTop2 - (max3 * 0.08f);
                animationState.spaceJumpStartTime = System.currentTimeMillis();
                editText.invalidate();
                startCursorHeartbeat(editText);
            }
        } catch (Throwable th) {
            if (this.debugMode) {
                log("space cursor jump error: " + th.getMessage());
            }
        }
    }

    private void triggerEnterCursorDive(EditText editText, AnimationState animationState, String str, int i, int i2) {
        if (!this.cursorEnabled || !this.liquidCursorEnabled || editText == null || str == null) {
            return;
        }
        try {
            int max = Math.max(0, i);
            int min = Math.min(str.length(), Math.max(i, i2));
            while (max < min) {
                int codePointAt = str.codePointAt(max);
                if (codePointAt == 10) {
                    animationState.enterDiveStartTime = System.currentTimeMillis();
                    editText.invalidate();
                    startCursorHeartbeat(editText);
                    return;
                }
                max += Character.charCount(codePointAt);
            }
        } catch (Throwable th) {
            if (this.debugMode) {
                log("enter cursor dive error: " + th.getMessage());
            }
        }
    }

    private void triggerBackspaceCursorPulse(EditText editText, AnimationState animationState) {
        if (!this.cursorEnabled || !this.liquidCursorEnabled || editText == null) {
            return;
        }
        animationState.backspacePulseStartTime = System.currentTimeMillis();
        editText.invalidate();
        startCursorHeartbeat(editText);
    }

    private boolean isEmojiLike(Spannable spannable, int i, int i2, int i3) {
        if (Character.charCount(i3) > 1 || i3 == 8205 || i3 == 65039 || i3 == 8419) {
            return true;
        }
        if ((i3 >= 9728 && i3 <= 10175) || (i3 >= 126976 && i3 <= 129791)) {
            return true;
        }
        if (spannable != null) {
            try {
                Object[] spans = spannable.getSpans(i, i2, ReplacementSpan.class);
                if (spans != null) {
                    if (spans.length > 0) {
                        return true;
                    }
                }
                return false;
            } catch (Throwable th) {
            }
        }
        return false;
    }

    private void spawnDeleteParticles(EditText editText, AnimationState animationState, int i, String str) {
        if (str == null || str.length() == 0 || str.length() > 15 || this.particleCount <= 0) {
            return;
        }
        int i2 = 0;
        while (i2 < str.length()) {
            int charCount = Character.charCount(str.codePointAt(i2)) + i2;
            spawnDeleteParticlesAt(editText, animationState, i2 + i, str.substring(i2, charCount));
            i2 = charCount;
        }
    }

    private void spawnDeleteParticlesAt(EditText editText, AnimationState animationState, int i, String str) {
        try {
            Layout layout = editText.getLayout();
            TextPaint paint = editText.getPaint();
            if (layout != null && paint != null) {
                int i2 = 0;
                int max = Math.max(0, Math.min(i, Math.max(0, editText.getText().length())));
                int lineForOffset = layout.getLineForOffset(max);
                float paddingLeft = (editText.getPaddingLeft() + layout.getPrimaryHorizontal(max)) - editText.getScrollX();
                float extendedPaddingTop = getDrawingExtendedPaddingTop(editText) + getVerticalOffset(editText) + layout.getLineBaseline(lineForOffset);
                float max2 = Math.max(1.0f, paint.measureText(str));
                int color = paint.getColor();
                float textSize = paint.getTextSize() * this.particleSpread * 0.1f;
                while (true) {
                    if (i2 < this.particleCount) {
                        float f = i2;
                        animationState.particles.add(new Particle((((0.5f + f) * max2) / Math.max(1, this.particleCount)) + paddingLeft + ((((i2 % 2 == 0 ? -1.0f : 1.0f) * textSize) * f) / Math.max(1, this.particleCount)), extendedPaddingTop - (paint.getTextSize() / 3.0f), color, paint.getTextSize(), this.particleSpeed, this.particleSize, this.particleStyle, str, this.particleSpread));
                        i2++;
                    } else {
                        return;
                    }
                }
            }
        } catch (Throwable th) {
            if (this.debugMode) {
                log("particle spawn error: " + th.getMessage());
            }
        }
    }

    private void updateHiddenSpans(EditText editText, AnimationState animationState) {
        try {
            Editable text = editText.getText();
            if (!(text instanceof Spannable)) {
                return;
            }
            Spannable editable = (Spannable) text;
            removeHiddenSpans(editText, animationState);
            if (animationState.hiddenSpans == null) {
                animationState.hiddenSpans = new HashMap<>();
            }
            int length = text.length();
            int lastLineStart = getLastLineStart(editText);
            for (Map.Entry<Integer, CharAnimData> entry : animationState.charStartTimes.entrySet()) {
                int intValue = entry.getKey();
                CharAnimData value = entry.getValue();
                if (intValue >= 0 && intValue < length) {
                    if (!this.animateAllLines && intValue < lastLineStart) {
                        continue;
                    }
                    int min = Math.min(length, value.length + intValue);
                    AnimHiddenSpan animHiddenSpan = new AnimHiddenSpan();
                    editable.setSpan(animHiddenSpan, intValue, min, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    animationState.hiddenSpans.put(intValue, animHiddenSpan);
                }
            }
        } catch (Throwable th) {
        }
    }

    private void removeHiddenSpans(View view, AnimationState animationState) {
        try {
            if (view instanceof EditText) {
                Editable text = ((EditText) view).getText();
                if (text instanceof Spannable) {
                    Spannable editable = (Spannable) text;
                    AnimHiddenSpan[] animSpans = editable.getSpans(0, editable.length(), AnimHiddenSpan.class);
                    if (animSpans != null && animSpans.length > 0) {
                        for (AnimHiddenSpan s : animSpans) {
                            editable.removeSpan(s);
                        }
                    }
                    ForegroundColorSpan[] fSpans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
                    if (fSpans != null && fSpans.length > 0) {
                        for (ForegroundColorSpan s : fSpans) {
                            if (s.getForegroundColor() == 0) {
                                editable.removeSpan(s);
                            }
                        }
                    }
                }
            }
            if (animationState != null && animationState.hiddenSpans != null) {
                animationState.hiddenSpans.clear();
            }
        } catch (Throwable th) {
        }
    }

    private void startAnimationLoop(final EditText editText, final AnimationState animationState) {
        if (animationState.animationRunning) {
            return;
        }
        animationState.animationRunning = true;
        editText.postOnAnimationDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (editText.getWindowToken() == null) {
                        animationState.animationRunning = false;
                        animationState.hasAnimatingChars = false;
                        TextAnimationCore.this.removeHiddenSpans(editText, animationState);
                        return;
                    }
                    long currentTimeMillis = System.currentTimeMillis();
                    int max = Math.max(TextAnimationCore.this.duration, TextAnimationCore.this.blurDuration);
                    Iterator<Map.Entry<Integer, CharAnimData>> it = animationState.charStartTimes.entrySet().iterator();
                    while (it.hasNext()) {
                        if (currentTimeMillis - it.next().getValue().startTime >= max) {
                            it.remove();
                        }
                    }
                    TextAnimationCore.this.updateHiddenSpans(editText, animationState);
                    editText.invalidate();
                    if (animationState.charStartTimes.isEmpty() && animationState.particles.isEmpty()) {
                        animationState.animationRunning = false;
                        animationState.hasAnimatingChars = false;
                        TextAnimationCore.this.removeHiddenSpans(editText, animationState);
                        return;
                    }
                    editText.postOnAnimationDelayed(this, 16L);
                } catch (Throwable th) {
                    animationState.animationRunning = false;
                    animationState.hasAnimatingChars = false;
                    TextAnimationCore.this.removeHiddenSpans(editText, animationState);
                }
            }
        }, 16L);
    }

    private void drawAnimatedChars(EditText editText, Canvas canvas, AnimationState animationState) {
        try {
            TextPaint paint = editText.getPaint();
            Layout layout = editText.getLayout();
            if (paint != null && layout != null) {
                long currentTimeMillis = System.currentTimeMillis();
                int length = editText.getText().length();
                int lastLineStart = getLastLineStart(editText);
                int paddingLeft = editText.getPaddingLeft();
                int scrollX = editText.getScrollX();
                float extendedPaddingTop = getDrawingExtendedPaddingTop(editText) + getVerticalOffset(editText);
                int alpha = paint.getAlpha();
                float textSize = paint.getTextSize();
                Iterator it = new ArrayList(animationState.charStartTimes.entrySet()).iterator();
                while (it.hasNext()) {
                    Map.Entry entry = (Map.Entry) it.next();
                    int intValue = ((Integer) entry.getKey()).intValue();
                    CharAnimData charAnimData = (CharAnimData) entry.getValue();
                    if (intValue >= 0 && intValue < length) {
                        if (this.animateAllLines || intValue >= lastLineStart) {
                            int lineForOffset = layout.getLineForOffset(intValue);
                            float primaryHorizontal = (paddingLeft + layout.getPrimaryHorizontal(intValue)) - scrollX;
                            float lineBaseline = extendedPaddingTop + layout.getLineBaseline(lineForOffset);
                            float f = (float) (currentTimeMillis - charAnimData.startTime);
                            float easeOutQuint = easeOutQuint(Math.min(1.0f, f / Math.max(1, this.duration)));
                            float easeOutQuint2 = easeOutQuint(Math.min(1.0f, f / Math.max(1, this.blurDuration)));
                            float f2 = lineBaseline + (this.slideEnabled ? (-dp(editText, this.slideDist)) * (1.0f - easeOutQuint) : 0.0f);
                            canvas.save();
                            if (this.scaleEnabled || this.rotateEnabled) {
                                float measureText = (paint.measureText(charAnimData.text) / 2.0f) + primaryHorizontal;
                                float f3 = f2 - (textSize / 3.0f);
                                canvas.translate(measureText, f3);
                                if (this.scaleEnabled) {
                                    float f4 = this.scaleStart;
                                    float f5 = f4 + ((1.0f - f4) * easeOutQuint);
                                    canvas.scale(f5, f5);
                                }
                                if (this.rotateEnabled) {
                                    canvas.rotate(this.rotateAngle * (1.0f - easeOutQuint));
                                }
                                canvas.translate(-measureText, -f3);
                            }
                            drawAnimatedText(canvas, charAnimData.text, primaryHorizontal, f2, paint, animationState, easeOutQuint2, alpha);
                            canvas.restore();
                        }
                    }
                }
                paint.setAlpha(alpha);
            }
        } catch (Throwable th) {
            if (this.debugMode) {
                log("draw chars error: " + th.getMessage());
            }
        }
    }

    private void drawAnimatedText(Canvas canvas, String str, float f, float f2, Paint paint, AnimationState animationState, float f3, int i) {
        if (animationState.sharedPaint == null) {
            animationState.sharedPaint = new Paint(paint);
        } else {
            animationState.sharedPaint.set(paint);
        }
        float min = Math.min(0.99f, Math.max(0.0f, this.blurTextDelay / 100.0f));
        float f4 = f3 > min ? (f3 - min) / (1.0f - min) : 0.0f;
        if (this.blurEnabled && this.blurRadius > 0 && f3 < 1.0f) {
            float f5 = 1.0f - f3;
            int i2 = (int) (i * f5);
            if (i2 > 4) {
                animationState.sharedPaint.setAlpha(i2);
                animationState.sharedPaint.setMaskFilter(new BlurMaskFilter(Math.max(0.1f, this.blurRadius * f5), BlurMaskFilter.Blur.NORMAL));
                canvas.drawText(str, f, f2, animationState.sharedPaint);
                animationState.sharedPaint.setMaskFilter(null);
            }
        }
        int i3 = (int) (f4 * i);
        int drawAlpha = Math.max(i3, (int) (i * Math.max(0.15f, f3)));
        if (drawAlpha > 0 || !this.blurEnabled || this.blurRadius <= 0) {
            animationState.sharedPaint.setAlpha(drawAlpha);
            animationState.sharedPaint.setMaskFilter(null);
            canvas.drawText(str, f, f2, animationState.sharedPaint);
        }
    }

    private void drawParticles(Canvas canvas, AnimationState animationState) {
        if (animationState.particlePaint == null) {
            animationState.particlePaint = new Paint(1);
        }
        if (animationState.particleGlowPaint == null) {
            animationState.particleGlowPaint = new Paint(1);
        }
        if (animationState.particleStrokePaint == null) {
            animationState.particleStrokePaint = new Paint(1);
            animationState.particleStrokePaint.setStyle(Paint.Style.STROKE);
            animationState.particleStrokePaint.setStrokeCap(Paint.Cap.ROUND);
        }
        if (animationState.particleTextPaint == null) {
            animationState.particleTextPaint = new Paint(1);
            animationState.particleTextPaint.setTextAlign(Paint.Align.CENTER);
        }
        long currentTimeMillis = System.currentTimeMillis();
        long max = animationState.lastParticleUpdateTime == 0 ? 16L : Math.max(8L, Math.min(48L, currentTimeMillis - animationState.lastParticleUpdateTime));
        animationState.lastParticleUpdateTime = currentTimeMillis;
        Iterator<Particle> it = animationState.particles.iterator();
        while (it.hasNext()) {
            Particle next = it.next();
            int max2 = Math.max(0, Math.min(255, (int) (next.life * 255.0f)));
            int i = next.color;
            animationState.particlePaint.setARGB(max2, (i >> 16) & 255, 255 & (i >> 8), i & 255);
            drawParticleShape(canvas, animationState, next, next.size * ((next.life * 0.65f) + 0.35f), max2, i);
            if (!next.update(max)) {
                it.remove();
            }
        }
        if (animationState.particles.isEmpty()) {
            animationState.lastParticleUpdateTime = 0L;
        }
    }

    private void drawParticleShape(Canvas canvas, AnimationState animationState, Particle particle, float f, int i, int i2) {
        if (particle.style == 1) {
            drawSparkParticle(canvas, animationState, particle, f, i, i2);
            return;
        }
        if (particle.style == 2) {
            drawSnowParticle(canvas, animationState, particle, f, i, i2);
            return;
        }
        if (particle.style == 3) {
            drawSakuraParticle(canvas, animationState, particle, f, i, i2);
        } else if (particle.style == 4) {
            drawTextParticle(canvas, animationState, particle, f, i, i2);
        } else {
            canvas.drawCircle(particle.x, particle.y, f / 2.0f, animationState.particlePaint);
        }
    }

    private void setPaintColor(Paint paint, int i, int i2) {
        paint.setARGB(i2, (i >> 16) & 255, (i >> 8) & 255, i & 255);
    }

    private void drawSparkParticle(Canvas canvas, AnimationState animationState, Particle particle, float f, int i, int i2) {
        Paint paint = animationState.particleStrokePaint;
        setPaintColor(paint, i2, i);
        paint.setStrokeWidth(Math.max(1.0f, 0.28f * f));
        float max = Math.max(3.0f, f * particle.stretch);
        double atan2 = Math.atan2(particle.vy, particle.vx);
        float cos = ((float) Math.cos(atan2)) * max * 0.5f;
        float sin = ((float) Math.sin(atan2)) * max * 0.5f;
        canvas.drawLine(particle.x - cos, particle.y - sin, cos + particle.x, particle.y + sin, paint);
    }

    private void drawSnowParticle(Canvas canvas, AnimationState animationState, Particle particle, float f, int i, int i2) {
        Paint paint = animationState.particleStrokePaint;
        setPaintColor(paint, lightenColor(i2, 0.55f), Math.max(32, i));
        paint.setStrokeWidth(Math.max(1.0f, 0.12f * f));
        canvas.save();
        canvas.rotate(particle.rotation, particle.x, particle.y);
        float max = Math.max(2.0f, f * 0.5f);
        for (int i3 = 0; i3 < 3; i3++) {
            canvas.rotate(60.0f, particle.x, particle.y);
            canvas.drawLine(particle.x - max, particle.y, particle.x + max, particle.y, paint);
            float f2 = 0.35f * max;
            float f3 = max * 0.38f;
            canvas.drawLine(particle.x + f3, particle.y, (particle.x + f3) - f2, particle.y - f2, paint);
            canvas.drawLine(particle.x + f3, particle.y, (particle.x + f3) - f2, particle.y + f2, paint);
        }
        canvas.restore();
    }

    private void drawSakuraParticle(Canvas canvas, AnimationState animationState, Particle particle, float f, int i, int i2) {
        Paint paint = animationState.particlePaint;
        Paint paint2 = animationState.particleGlowPaint;
        int tintSakura = tintSakura(i2);
        Path path = new Path();
        float max = Math.max(2.0f, 0.62f * f);
        float max2 = Math.max(3.0f, particle.stretch * f);
        float f2 = -max2;
        float f3 = f2 * 0.5f;
        path.moveTo(0.0f, f3);
        float f4 = f2 * 0.58f;
        float f5 = max2 * 0.02f;
        float f6 = max2 * 0.42f;
        path.cubicTo(max * 0.95f, f4, max * 1.03f, f5, max * 0.22f, f6);
        float f7 = max2 * 0.49f;
        float f8 = max2 * 0.52f;
        path.cubicTo(max * 0.1f, f7, max * 0.04f, f8, 0.0f, max2 * 0.56f);
        float f9 = -max;
        path.cubicTo(f9 * 0.04f, f8, f9 * 0.1f, f7, f9 * 0.22f, f6);
        path.cubicTo(f9 * 1.03f, f5, f9 * 0.95f, f4, 0.0f, f3);
        canvas.save();
        canvas.translate(particle.x, particle.y);
        canvas.rotate(particle.rotation);
        setPaintColor(paint2, lightenColor(tintSakura, 0.35f), Math.min(110, Math.max(18, i / 2)));
        paint2.setMaskFilter(new BlurMaskFilter(Math.max(1.0f, 0.36f * f), BlurMaskFilter.Blur.NORMAL));
        canvas.drawPath(path, paint2);
        paint2.setMaskFilter(null);
        setPaintColor(paint, tintSakura, Math.max(24, i));
        canvas.drawPath(path, paint);
        Paint paint3 = animationState.particleStrokePaint;
        setPaintColor(paint3, lightenColor(tintSakura, 0.28f), Math.min(150, Math.max(28, (i * 3) / 5)));
        paint3.setStrokeWidth(Math.max(1.0f, 0.08f * f));
        canvas.drawLine(0.0f, f2 * 0.28f, 0.0f, max2 * 0.38f, paint3);
        canvas.restore();
    }

    private void drawTextParticle(Canvas canvas, AnimationState animationState, Particle particle, float f, int i, int i2) {
        Paint paint = animationState.particleTextPaint;
        setPaintColor(paint, i2, i);
        paint.setTextSize(Math.max(6.0f, f));
        canvas.save();
        canvas.rotate(particle.rotation, particle.x, particle.y);
        canvas.drawText(particle.text, particle.x, particle.y + (f * 0.35f), paint);
        canvas.restore();
    }

    private int lightenColor(int color, float f) {
        int red = (color >> 16) & 255;
        int green = (color >> 8) & 255;
        int blue = color & 255;
        return (blue + ((int) ((255 - blue) * f))) | ((red + ((int) ((255 - red) * f))) << 16) | ((green + ((int) ((255 - green) * f))) << 8);
    }

    private int tintSakura(int i) {
        return Math.min(235, (int) (((i & 255) * 0.6f) + 108.0f)) | (Math.min(255, (int) ((((i >> 16) & 255) * 0.55f) + 145.0f)) << 16) | (Math.min(230, (int) ((((i >> 8) & 255) * 0.45f) + 82.0f)) << 8);
    }

    private void hideSystemCursor(EditText editText, AnimationState animationState) {
        try {
            if (editText instanceof org.telegram.ui.Components.EditTextBoldCursor) {
                ((org.telegram.ui.Components.EditTextBoldCursor) editText).setAllowDrawCursor(false);
            }
            startCursorHeartbeat(editText);
        } catch (Throwable th) {
        }
    }

    private void restoreSystemCursor(View view, AnimationState animationState) {
        try {
            if (view instanceof org.telegram.ui.Components.EditTextBoldCursor) {
                ((org.telegram.ui.Components.EditTextBoldCursor) view).setAllowDrawCursor(true);
            }
        } catch (Throwable th) {
        }
    }

    private void hideSystemSelectionHandles(EditText editText, AnimationState state) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                android.graphics.drawable.Drawable left = editText.getTextSelectHandleLeft();
                if (left != null && !(left instanceof TransparentDrawable)) {
                    editText.setTextSelectHandleLeft(new TransparentDrawable(left));
                }
                android.graphics.drawable.Drawable right = editText.getTextSelectHandleRight();
                if (right != null && !(right instanceof TransparentDrawable)) {
                    editText.setTextSelectHandleRight(new TransparentDrawable(right));
                }
            } else {
                Field editorField = findFieldInHierarchy(TextView.class, "mEditor");
                if (editorField != null) {
                    Object editor = editorField.get(editText);
                    if (editor != null) {
                        Field leftField = findFieldInHierarchy(editor.getClass(), "mSelectHandleLeft");
                        Field rightField = findFieldInHierarchy(editor.getClass(), "mSelectHandleRight");
                        if (leftField != null) {
                            Object left = leftField.get(editor);
                            if (left instanceof android.graphics.drawable.Drawable && !(left instanceof TransparentDrawable)) {
                                leftField.set(editor, new TransparentDrawable((android.graphics.drawable.Drawable) left));
                            }
                        }
                        if (rightField != null) {
                            Object right = rightField.get(editor);
                            if (right instanceof android.graphics.drawable.Drawable && !(right instanceof TransparentDrawable)) {
                                rightField.set(editor, new TransparentDrawable((android.graphics.drawable.Drawable) right));
                            }
                        }
                    }
                }
            }
        } catch (Throwable th) {
        }
    }

    private void restoreSystemSelectionHandles(EditText editText, AnimationState state) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                android.graphics.drawable.Drawable left = editText.getTextSelectHandleLeft();
                if (left instanceof TransparentDrawable) {
                    editText.setTextSelectHandleLeft(((TransparentDrawable) left).mDelegate);
                }
                android.graphics.drawable.Drawable right = editText.getTextSelectHandleRight();
                if (right instanceof TransparentDrawable) {
                    editText.setTextSelectHandleRight(((TransparentDrawable) right).mDelegate);
                }
            } else {
                Field editorField = findFieldInHierarchy(TextView.class, "mEditor");
                if (editorField != null) {
                    Object editor = editorField.get(editText);
                    if (editor != null) {
                        Field leftField = findFieldInHierarchy(editor.getClass(), "mSelectHandleLeft");
                        Field rightField = findFieldInHierarchy(editor.getClass(), "mSelectHandleRight");
                        if (leftField != null) {
                            Object left = leftField.get(editor);
                            if (left instanceof TransparentDrawable) {
                                leftField.set(editor, ((TransparentDrawable) left).mDelegate);
                            }
                        }
                        if (rightField != null) {
                            Object right = rightField.get(editor);
                            if (right instanceof TransparentDrawable) {
                                rightField.set(editor, ((TransparentDrawable) right).mDelegate);
                            }
                        }
                    }
                }
            }
        } catch (Throwable th) {
        }
    }

    private boolean getCursorMetrics(EditText editText, Layout layout, int i, float[] fArr) {
        try {
            int max = Math.max(0, Math.min(i, editText.getText().length()));
            int lineForOffset = layout.getLineForOffset(max);
            float paddingLeft = (editText.getPaddingLeft() + layout.getPrimaryHorizontal(max)) - editText.getScrollX();
            float extendedPaddingTop = getDrawingExtendedPaddingTop(editText) + getVerticalOffset(editText) + layout.getLineTop(lineForOffset);
            float extendedPaddingTop2 = getDrawingExtendedPaddingTop(editText) + getVerticalOffset(editText) + layout.getLineBottom(lineForOffset);
            float lineSpacingExtra = editText.getLineSpacingExtra();
            if (lineSpacingExtra != 0 && lineForOffset < layout.getLineCount() - 1) {
                extendedPaddingTop2 -= lineSpacingExtra;
            }
            fArr[0] = paddingLeft;
            fArr[1] = extendedPaddingTop;
            fArr[2] = extendedPaddingTop2;
            fArr[3] = (extendedPaddingTop + extendedPaddingTop2) / 2.0f;
            fArr[4] = Math.max(1.0f, extendedPaddingTop2 - extendedPaddingTop);
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    private void drawSelectionCursorEffects(EditText editText, Canvas canvas, AnimationState animationState, Layout layout, int i, int i2, int i3, float f, long j) {
        int max = Math.max(0, Math.min(i, i2));
        int min = Math.min(Math.max(0, Math.max(i, i2)), editText.getText().length());
        if (max == min) {
            return;
        }
        boolean z = animationState.lastSelectionStart >= 0 && animationState.lastSelectionStart != max;
        boolean z2 = animationState.lastSelectionEnd >= 0 && animationState.lastSelectionEnd != min;
        if (animationState.lastSelectionStart != max || animationState.lastSelectionEnd != min) {
            animationState.selectionStartSnap = z ? -1.0f : z2 ? 0.35f : 0.0f;
            animationState.selectionEndSnap = z2 ? 1.0f : z ? -0.35f : 0.0f;
            animationState.lastSelectionStart = max;
            animationState.lastSelectionEnd = min;
            animationState.selectionEffectStartTime = j;
        }
        float[] fArr = new float[5];
        float[] fArr2 = new float[5];
        if (!getCursorMetrics(editText, layout, max, fArr) || !getCursorMetrics(editText, layout, min, fArr2)) {
            return;
        }
        float min2 = animationState.selectionEffectStartTime == 0 ? 1.0f : Math.min(1.0f, ((float) (j - animationState.selectionEffectStartTime)) / 360.0f);
        double d = min2;
        float sin = (float) Math.sin(d * 3.141592653589793d);
        if (min2 >= 1.0f) {
            animationState.selectionStartSnap = 0.0f;
            animationState.selectionEndSnap = 0.0f;
        }
        float f2 = this.selectionLiquidStretch / 100.0f;
        float f3 = this.selectionLiquidSide / 100.0f;
        float min3 = Math.min(1.15f, (0.35f * f2) + (0.22f * f3)) + 0.88f;
        float max2 = Math.max(0.12f, Math.min(0.42f, 0.3f - (f2 * 0.055f)));
        if (animationState.selectionStartX < 0.0f || animationState.selectionStartY < 0.0f) {
            animationState.selectionStartX = fArr[0];
            animationState.selectionStartY = fArr[3];
        } else {
            animationState.selectionStartX += (fArr[0] - animationState.selectionStartX) * max2;
            animationState.selectionStartY += (fArr[3] - animationState.selectionStartY) * max2;
        }
        if (animationState.selectionEndX < 0.0f || animationState.selectionEndY < 0.0f) {
            animationState.selectionEndX = fArr2[0];
            animationState.selectionEndY = fArr2[3];
        } else {
            animationState.selectionEndX += (fArr2[0] - animationState.selectionEndX) * max2;
            animationState.selectionEndY += (fArr2[3] - animationState.selectionEndY) * max2;
        }
        float f4 = fArr2[0] - fArr[0];
        float f5 = fArr2[3] - fArr[3];
        float max3 = Math.max(0.0f, 1.0f - Math.min(1.0f, ((float) Math.sqrt((f4 * f4) + (f5 * f5))) / Math.max(dp(editText, 18.0f), Math.max(fArr[4], fArr2[4]) * ((f3 * 0.45f) + 1.2f)))) * min3;
        float f6 = fArr[0] - animationState.selectionStartX;
        float f7 = fArr2[0] - animationState.selectionEndX;
        float abs = Math.abs(f6);
        float abs2 = Math.abs(f7);
        drawSelectionHandle(canvas, animationState, fArr, animationState.selectionStartX, animationState.selectionStartY, i3, f, sin, max3, animationState.selectionStartSnap, -1.0f, f6, abs);
        drawSelectionHandle(canvas, animationState, fArr2, animationState.selectionEndX, animationState.selectionEndY, i3, f, sin, max3, animationState.selectionEndSnap, 1.0f, f7, abs2);
    }

    private void drawSelectionHandle(Canvas canvas, AnimationState animationState, float[] fArr, float f, float f2, int i, float f3, float f4, float f5, float f6, float f7, float f8, float f9) {
        if (animationState.cursorPaint == null) {
            animationState.cursorPaint = new Paint(1);
        }
        if (animationState.cursorRect == null) {
            animationState.cursorRect = new RectF();
        }
        float f10 = fArr[4];
        float f11 = (fArr[1] + fArr[2]) / 2.0f;
        float f12 = this.selectionLiquidStretch / 100.0f;
        float f13 = this.selectionLiquidSide / 100.0f;
        float min = Math.min(1.0f, Math.abs(f8) / Math.max(f3, f10 * 0.22f));
        float max = Math.max(1.0f, ((Math.min(1.0f, f9 / Math.max(f3, f10 * 0.25f)) * ((0.46f * f12) + 0.34f)) + 1.08f + (f4 * 0.18f) + (((0.28f * f13) + 0.18f) * f5)) * f3);
        float min2 = Math.min(((0.78f * f12) + 0.34f + (0.34f * f13)) * f10, (((0.72f * f12) + 0.38f) * f9) + (f3 * f4 * ((f12 * 1.25f) + 0.55f)));
        float f14 = f + (f7 * max * ((f5 * ((f13 * 0.3f) + 0.18f)) + 0.14f + (min * f13 * 0.22f))) + (f4 * f6 * f3 * ((0.65f * f13) + 1.25f)) + (f13 * f8 * 0.16f);
        float f15 = (f2 - f11) * 0.24f;
        float f16 = fArr[1] + ((0.1f - (0.03f * f4)) * f10) + f15;
        float f17 = (fArr[2] - (f10 * (0.1f - (0.018f * f4)))) + f15;
        float f18 = ((min * 0.3f) + 0.25f) * min2;
        float f19 = max / 2.0f;
        float f20 = (f14 - f19) - (f7 < 0.0f ? min2 : f18);
        float f21 = f14 + f19;
        if (f7 <= 0.0f) {
            min2 = f18;
        }
        animationState.cursorRect.set(f20, f16, f21 + min2, f17);
        animationState.cursorPaint.setColor(i);
        animationState.cursorPaint.setAlpha(255);
        canvas.drawRoundRect(animationState.cursorRect, max, max, animationState.cursorPaint);
    }

    private void drawCursor(EditText editText, Canvas canvas, AnimationState animationState) {
        try {
            if (editText.isFocused() && editText.getSelectionStart() >= 0 && editText.getLayout() != null) {
                Layout layout = editText.getLayout();
                boolean z = false;
                int max = Math.max(0, Math.min(editText.getSelectionStart(), editText.getText().length()));
                int max2 = Math.max(0, Math.min(editText.getSelectionEnd(), editText.getText().length()));
                if (this.selectionCursorEffect > 0 && max != max2) {
                    z = true;
                }
                int i = z ? max2 : max;
                int lineForOffset = layout.getLineForOffset(i);
                float paddingLeft = (editText.getPaddingLeft() + layout.getPrimaryHorizontal(i)) - editText.getScrollX();
                float extendedPaddingTop = getDrawingExtendedPaddingTop(editText) + getVerticalOffset(editText) + layout.getLineTop(lineForOffset);
                float extendedPaddingTop2 = getDrawingExtendedPaddingTop(editText) + getVerticalOffset(editText) + layout.getLineBottom(lineForOffset);
                float lineSpacingExtra = editText.getLineSpacingExtra();
                if (lineSpacingExtra != 0 && lineForOffset < layout.getLineCount() - 1) {
                    extendedPaddingTop2 -= lineSpacingExtra;
                }
                float f13 = (extendedPaddingTop + extendedPaddingTop2) / 2.0f;
                long currentTimeMillis = System.currentTimeMillis();
                
                float currentTargetX = paddingLeft;
                float currentTargetY = f13;
                if (Math.abs(currentTargetX - animationState.lastCursorX) > 0.1f || Math.abs(currentTargetY - animationState.lastCursorY) > 0.1f) {
                    animationState.lastCursorX = currentTargetX;
                    animationState.lastCursorY = currentTargetY;
                    animationState.lastCursorMoveTime = currentTimeMillis;
                }
                
                int cursorAlpha = 255;
                if (currentTimeMillis - animationState.lastCursorMoveTime > 500) {
                    long elapsed = currentTimeMillis - animationState.lastCursorMoveTime;
                    if ((elapsed / 500) % 2 != 0) {
                        cursorAlpha = 0;
                    }
                }

                float min;
                if (animationState.lastCursorDrawTime == 0) {
                    min = 16.0f;
                } else {
                    min = Math.min(50.0f, (float) (currentTimeMillis - animationState.lastCursorDrawTime));
                }
                animationState.lastCursorDrawTime = currentTimeMillis;
                if (this.liquidCursorEnabled) {
                    if (animationState.lastSelectionOffset >= 0 && animationState.lastSelectionOffset != i && Math.abs(i - animationState.lastSelectionOffset) > 2) {
                        animationState.selectionStretchBoost = Math.min(1.0f, Math.abs(i - animationState.lastSelectionOffset) / 18.0f);
                        animationState.selectionStretchStartTime = currentTimeMillis;
                    }
                    animationState.lastSelectionOffset = i;
                }
                float f14 = min / 16.0f;
                float min2 = Math.min(1.0f, Math.max(0.05f, this.cursorSpeed / 100.0f) * f14);
                if (animationState.cursorX >= 0.0f && animationState.cursorY >= 0.0f) {
                    animationState.cursorX += (paddingLeft - animationState.cursorX) * min2;
                    animationState.cursorY += (f13 - animationState.cursorY) * min2;
                    float f15 = paddingLeft - animationState.cursorX;
                    float f16 = f13 - animationState.cursorY;
                    animationState.cursorMotion += (((float) Math.sqrt((f15 * f15) + (f16 * f16))) - animationState.cursorMotion) * Math.min(1.0f, f14 * 0.32f);
                    if (animationState.cursorPaint == null) {
                        animationState.cursorPaint = new Paint(1);
                    }
                    if (animationState.cursorRect == null) {
                        animationState.cursorRect = new RectF();
                    }
                    int themeColorForView = getThemeColorForView(editText, this.keyChatMessagePanelCursor, editText.getPaint().getColor());
                    animationState.cursorPaint.setColor(themeColorForView);
                    animationState.cursorPaint.setAlpha(cursorAlpha);
                    float max3 = Math.max(1.0f, dp(editText, this.cursorWidth));
                    if (z) {
                        drawSelectionCursorEffects(editText, canvas, animationState, layout, max, max2, themeColorForView, max3, currentTimeMillis);
                        editText.invalidate();
                        return;
                    }
                    animationState.lastSelectionStart = -1;
                    animationState.lastSelectionEnd = -1;
                    animationState.selectionEffectStartTime = 0L;
                    animationState.selectionStartX = -1.0f;
                    animationState.selectionStartY = -1.0f;
                    animationState.selectionEndX = -1.0f;
                    animationState.selectionEndY = -1.0f;
                    animationState.selectionStartSnap = 0.0f;
                    animationState.selectionEndSnap = 0.0f;
                    float f17 = animationState.cursorX;
                    float f18 = animationState.cursorY;
                    float max4 = Math.max(1.0f, extendedPaddingTop2 - extendedPaddingTop);
                    float f2 = f17;
                    float f3 = 0.0f;
                    if (this.liquidCursorEnabled && animationState.spaceJumpStartTime > 0) {
                        float min3 = Math.min(1.0f, ((float) (currentTimeMillis - animationState.spaceJumpStartTime)) / 260.0f);
                        double d = min3;
                        float sin = (float) Math.sin(d * 3.141592653589793d);
                        f2 = f17 + ((animationState.spaceJumpX - f17) * sin);
                        f18 += (animationState.spaceJumpY - f18) * sin;
                        if (min3 >= 1.0f) {
                            animationState.spaceJumpStartTime = 0L;
                            animationState.spaceJumpX = -1.0f;
                            animationState.spaceJumpY = -1.0f;
                            f3 = 0.0f;
                        } else {
                            f3 = sin;
                        }
                    }
                    float f5 = f2;
                    float f6 = 0.0f;
                    if (this.liquidCursorEnabled && animationState.enterDiveStartTime > 0) {
                        float min4 = Math.min(1.0f, ((float) (currentTimeMillis - animationState.enterDiveStartTime)) / 340.0f);
                        f5 = f2;
                        double d2 = min4;
                        f6 = (float) Math.sin(d2 * 3.141592653589793d);
                        f18 += max4 * 0.28f * f6;
                        if (min4 >= 1.0f) {
                            animationState.enterDiveStartTime = 0L;
                            f6 = 0.0f;
                        }
                    }
                    float f7 = paddingLeft;
                    float f8 = extendedPaddingTop2;
                    float f9 = 0.0f;
                    if (this.liquidCursorEnabled && animationState.backspacePulseStartTime > 0) {
                        float min5 = Math.min(1.0f, ((float) (currentTimeMillis - animationState.backspacePulseStartTime)) / 190.0f);
                        f8 = extendedPaddingTop2;
                        double d3 = min5;
                        f9 = (float) Math.sin(d3 * 3.141592653589793d);
                        if (min5 >= 1.0f) {
                            animationState.backspacePulseStartTime = 0L;
                            f9 = 0.0f;
                        }
                    }
                    float f10 = 0.0f;
                    if (this.liquidCursorEnabled && animationState.selectionStretchStartTime > 0) {
                        float min6 = Math.min(1.0f, ((float) (currentTimeMillis - animationState.selectionStretchStartTime)) / 420.0f);
                        double d4 = min6;
                        f10 = ((float) Math.sin(d4 * 3.141592653589793d)) * animationState.selectionStretchBoost;
                        if (min6 >= 1.0f) {
                            animationState.selectionStretchStartTime = 0L;
                            animationState.selectionStretchBoost = 0.0f;
                            f10 = 0.0f;
                        }
                    }
                    float min7 = this.liquidCursorEnabled ? Math.min(1.0f, animationState.cursorMotion / Math.max(max3, dp(editText, Math.max(1, this.liquidScaleFactor)))) : 0.0f;
                    float dp = ((0.7f * f10) + 1.0f) * dp(editText, this.liquidScaleFactor);
                    float f11 = 0.0f;
                    if (this.liquidCursorEnabled) {
                        f11 = Math.min(dp, (animationState.cursorMotion * ((f10 * 0.28f) + 0.46f)) + (1.4f * max3 * f3) + (1.8f * max3 * f10));
                    }
                    float f12 = 1.0f;
                    float max5 = Math.max(0.55f * max3, ((((((0.22f * min7) + 1.0f) + (0.28f * f3)) + (f6 * 0.1f)) + (f10 * 0.14f)) - (0.34f * f9)) * max3);
                    if (f15 > 0.0f) {
                        f12 = -1.0f;
                    }
                    float f19 = max5 / 2.0f;
                    float f20 = (f5 - f19) - (f12 >= 0.0f ? f11 : 0.0f);
                    float f21 = f19 + f5;
                    if (f12 >= 0.0f) {
                        f11 = 0.0f;
                    }
                    float f22 = f21 + f11;
                    float max6 = Math.max(0.0f, ((min7 * 0.18f) + 0.08f + (0.1f * f3) + (f6 * 0.08f) + (f9 * 0.14f)) * max4);
                    float f23 = (f18 - f13) * 0.12f;
                    float f24 = extendedPaddingTop + max6 + f23;
                    float f25 = (f8 - max6) + f23;
                    if (f3 > 0.0f) {
                        float max7 = Math.max(max3 * 2.7f, max4 * 0.36f);
                        float f26 = max7 / 2.0f;
                        f20 += ((f5 - f26) - f20) * f3;
                        f22 += ((f5 + f26) - f22) * f3;
                        f24 += ((f18 - max7) - f24) * f3;
                        f25 += (f18 - f25) * f3;
                        max5 += (f26 - max5) * f3;
                    }
                    animationState.cursorRect.set(f20, f24, f22, f25);
                    canvas.drawRoundRect(animationState.cursorRect, max5, max5, animationState.cursorPaint);
                    if (Math.abs(f7 - animationState.cursorX) > 0.5f || Math.abs(f13 - animationState.cursorY) > 0.5f || animationState.cursorMotion > 0.2f || animationState.spaceJumpStartTime > 0 || animationState.enterDiveStartTime > 0 || animationState.backspacePulseStartTime > 0 || animationState.selectionStretchStartTime > 0) {
                        editText.invalidate();
                    }
                    return;
                }
                animationState.cursorX = paddingLeft;
                animationState.cursorY = f13;
                if (animationState.cursorPaint == null) {
                    animationState.cursorPaint = new Paint(1);
                }
                if (animationState.cursorRect == null) {
                    animationState.cursorRect = new RectF();
                }
                int themeColorForView2 = getThemeColorForView(editText, this.keyChatMessagePanelCursor, editText.getPaint().getColor());
                animationState.cursorPaint.setColor(themeColorForView2);
                animationState.cursorPaint.setAlpha(cursorAlpha);
                float max32 = Math.max(1.0f, dp(editText, this.cursorWidth));
                animationState.cursorRect.set(paddingLeft - (max32 / 2.0f), extendedPaddingTop, paddingLeft + (max32 / 2.0f), extendedPaddingTop2);
                canvas.drawRect(animationState.cursorRect, animationState.cursorPaint);
            }
        } catch (Throwable th) {
            if (this.debugMode) {
                log("cursor draw error: " + th.getMessage());
            }
        }
    }

    private void startCursorHeartbeat(final View view) {
        if (!this.cursorEnabled || view == null) {
            return;
        }
        final AnimationState state = getState(view);
        if (state.heartbeatRunning) {
            return;
        }
        state.heartbeatRunning = true;
        view.postOnAnimationDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (TextAnimationCore.this.cursorEnabled && view.isFocused() && view.getWindowToken() != null && view.isShown()) {
                        view.invalidate();
                        view.postOnAnimationDelayed(this, 16L);
                        return;
                    }
                    state.heartbeatRunning = false;
                } catch (Throwable th) {
                    state.heartbeatRunning = false;
                }
            }
        }, 16L);
    }

    private void invalidateTrackedViews() {
        Iterator it = new ArrayList(this.states.keySet()).iterator();
        while (it.hasNext()) {
            View view = (View) it.next();
            if (view != null) {
                try {
                    view.invalidate();
                } catch (Throwable th) {
                }
            }
        }
    }

    private float getVerticalOffset(TextView textView) {
        try {
            Method method = this.textViewGetVerticalOffsetMethod;
            if (method != null) {
                Object invoke = method.invoke(textView, Boolean.TRUE);
                if (invoke instanceof Integer) {
                    return ((Integer) invoke).floatValue();
                }
            }
        } catch (Throwable th) {
        }
        return 0.0f;
    }

    private float getDrawingExtendedPaddingTop(EditText editText) {
        if (editText instanceof org.telegram.ui.Components.EditTextBoldCursor) {
            return 0.0f;
        }
        return editText.getExtendedPaddingTop();
    }

    private int getThemeColorForView(View view, Integer num, int i) {
        Object obj;
        Method method;
        if (num == null || this.themeClass == null) {
            return i;
        }
        try {
            Field field = this.fieldResourcesProvider;
            if (field != null && view != null) {
                obj = field.get(view);
            } else {
                obj = null;
            }
            if (obj != null && (method = this.themeGetColorWithProviderMethod) != null) {
                Object invoke = method.invoke(null, num, obj);
                if (invoke instanceof Integer) {
                    return (Integer) invoke;
                }
            }
            Method method2 = this.themeGetColorMethod;
            if (method2 != null) {
                Object invoke2 = method2.invoke(null, num);
                if (invoke2 instanceof Integer) {
                    return (Integer) invoke2;
                }
            }
        } catch (Throwable th) {
        }
        return i;
    }

    private float dp(View view, float f) {
        if (view == null) {
            return f;
        }
        return f * view.getResources().getDisplayMetrics().density;
    }

    private float easeOutQuint(float f) {
        float max = 1.0f - Math.max(0.0f, Math.min(1.0f, f));
        return 1.0f - ((((max * max) * max) * max) * max);
    }

    private void log(String str) {
        try {
            Log.d("TextAnimationCore", String.valueOf(str));
        } catch (Throwable th) {
        }
    }

    public static void onFocusChanged(View view, boolean focused) {
        TextAnimationCore core = getInstance();
        if (!core.initialized || !core.enabled) {
            return;
        }
        AnimationState state = core.getState(view);
        if (!focused) {
            state.cursorX = -1.0f;
            state.cursorY = -1.0f;
            state.cursorMotion = 0.0f;
            state.spaceJumpStartTime = 0L;
            state.enterDiveStartTime = 0L;
            state.backspacePulseStartTime = 0L;
            state.selectionStretchStartTime = 0L;
            state.selectionStretchBoost = 0.0f;
            state.lastSelectionOffset = -1;
            state.lastSelectionStart = -1;
            state.lastSelectionEnd = -1;
            state.selectionEffectStartTime = 0L;
            state.selectionStartX = -1.0f;
            state.selectionStartY = -1.0f;
            state.selectionEndX = -1.0f;
            state.selectionEndY = -1.0f;
            state.selectionStartSnap = 0.0f;
            state.selectionEndSnap = 0.0f;
        }
        view.invalidate();
        core.startCursorHeartbeat(view);
    }

    public static void onTouchEvent(View view, MotionEvent event) {
        TextAnimationCore core = getInstance();
        if (!core.initialized || !core.enabled) {
            return;
        }
        view.invalidate();
        core.startCursorHeartbeat(view);
    }

    public static class TransparentDrawable extends android.graphics.drawable.Drawable {
        public final android.graphics.drawable.Drawable mDelegate;

        public TransparentDrawable(android.graphics.drawable.Drawable delegate) {
            this.mDelegate = delegate;
        }

        @Override
        public void draw(android.graphics.Canvas canvas) {
        }

        @Override
        public void setAlpha(int alpha) {
            if (mDelegate != null) {
                mDelegate.setAlpha(alpha);
            }
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            if (mDelegate != null) {
                mDelegate.setColorFilter(colorFilter);
            }
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSPARENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return mDelegate != null ? mDelegate.getIntrinsicWidth() : 0;
        }

        @Override
        public int getIntrinsicHeight() {
            return mDelegate != null ? mDelegate.getIntrinsicHeight() : 0;
        }
    }
}
