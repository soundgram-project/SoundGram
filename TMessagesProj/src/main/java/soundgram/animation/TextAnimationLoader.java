package soundgram.animation;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

public class TextAnimationLoader {

    private static boolean loaded = false;

    public static boolean isLoaded() {
        return loaded;
    }

    public static SharedPreferences getPreferences() {
        return ApplicationLoader.applicationContext.getSharedPreferences("soundgram_textanim", Context.MODE_PRIVATE);
    }

    public static void loadFromAssets() {
        try {
            soundgram.animation.TextAnimationCore.start();
            loaded = true;
            updateSettings();
        } catch (Exception e) {
            FileLog.e("Failed to start native TextAnimationCore", e);
        }
    }

    public static void updateSettings() {
        if (!loaded) return;
        try {
            SharedPreferences p = getPreferences();
            org.json.JSONObject config = new org.json.JSONObject();
            config.put("enabled", p.getBoolean("text_anim_enabled", true));
            config.put("duration", String.valueOf(p.getInt("text_anim_duration", 300)));
            config.put("blur_enabled", p.getBoolean("text_anim_blur_enabled", true));
            config.put("blur_duration", String.valueOf(p.getInt("text_anim_blur_duration", 300)));
            config.put("blur_radius", String.valueOf(p.getInt("text_anim_blur_radius", 10)));
            config.put("blur_text_delay", String.valueOf(p.getInt("text_anim_blur_text_delay", 20)));
            
            config.put("slide_enabled", p.getBoolean("text_anim_slide_enabled", true));
            config.put("slide_dist", String.valueOf(p.getInt("text_anim_slide_dist", 20)));
            config.put("scale_enabled", p.getBoolean("text_anim_scale_enabled", false));
            config.put("scale_start", String.valueOf(p.getFloat("text_anim_scale_start", 0.0f)));
            config.put("rotate_enabled", p.getBoolean("text_anim_rotate_enabled", false));
            config.put("rotate_angle", String.valueOf(p.getInt("text_anim_rotate_angle", -15)));
            
            config.put("delete_anim_enabled", p.getBoolean("text_anim_delete_anim_enabled", true));
            
            config.put("particle_style", p.getInt("text_anim_particle_style", 0));
            config.put("particle_count", String.valueOf(p.getInt("text_anim_particle_count", 5)));
            config.put("particle_speed", String.valueOf(p.getInt("text_anim_particle_speed", 50)));
            config.put("particle_spread", String.valueOf(p.getInt("text_anim_particle_spread", 50)));
            config.put("particle_size", String.valueOf(p.getInt("text_anim_particle_size", 50)));
            
            config.put("cursor_enabled", p.getBoolean("text_anim_cursor_enabled", true));
            config.put("cursor_speed", String.valueOf(p.getInt("text_anim_cursor_speed", 25)));
            config.put("cursor_width", String.valueOf(p.getInt("text_anim_cursor_width", 5)));
            
            config.put("liquid_cursor_enabled", p.getBoolean("text_anim_liquid_cursor_enabled", false));
            config.put("liquid_scale_factor", String.valueOf(p.getInt("text_anim_liquid_scale_factor", 15)));
            
            config.put("selection_cursor_effect", p.getInt("text_anim_selection_cursor_effect", 0));
            config.put("selection_liquid_stretch", String.valueOf(p.getInt("text_anim_selection_liquid_stretch", 60)));
            config.put("selection_liquid_side", String.valueOf(p.getInt("text_anim_selection_liquid_side", 50)));
            
            config.put("ignore_spaces", p.getBoolean("text_anim_ignore_spaces", true));
            config.put("animate_all_lines", p.getBoolean("text_anim_animate_all_lines", false));
            config.put("debug_mode", p.getBoolean("text_anim_debug_mode", false));

            soundgram.animation.TextAnimationCore.updateSettings(config.toString());
        } catch (Exception e) {
            FileLog.e("Failed to update TextAnimationCore settings natively", e);
        }
    }

    public static void unload() {
        if (loaded) {
            try {
                soundgram.animation.TextAnimationCore.unload();
            } catch (Exception e) {
                FileLog.e("Failed to unload TextAnimationCore natively", e);
            }
            loaded = false;
        }
    }
}
