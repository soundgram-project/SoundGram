package soundgram.animation;

import android.content.SharedPreferences;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import app.nimarkogram.messenger.preferences.BasePreferencesActivity;

public class TextAnimationSettingsActivity extends BasePreferencesActivity {

    private static final int ID_ENABLED             = 1;
    private static final int ID_IGNORE_SPACES       = 2;
    private static final int ID_ANIMATE_ALL_LINES   = 3;
    private static final int ID_DELETE_ANIM_ENABLED = 4;
    private static final int ID_BLUR_ENABLED        = 5;
    private static final int ID_SLIDE_ENABLED       = 6;
    private static final int ID_SCALE_ENABLED       = 7;
    private static final int ID_ROTATE_ENABLED      = 8;
    private static final int ID_CURSOR_ENABLED      = 9;
    private static final int ID_LIQUID_CURSOR       = 10;

    @Override
    public String getTitle() {
        return LocaleController.getString(R.string.sg_ta_title);
    }

    @Override
    public void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        SharedPreferences p = TextAnimationLoader.getPreferences();

        items.add(UItem.asHeader(LocaleController.getString(R.string.sg_ta_section_main)));
        items.add(UItem.asCheck(ID_ENABLED, LocaleController.getString(R.string.sg_ta_enabled))
                .setChecked(p.getBoolean("text_anim_enabled", true)));
        items.add(UItem.asCheck(ID_IGNORE_SPACES, LocaleController.getString(R.string.sg_ta_ignore_spaces))
                .setChecked(p.getBoolean("text_anim_ignore_spaces", true)));
        items.add(UItem.asCheck(ID_ANIMATE_ALL_LINES, LocaleController.getString(R.string.sg_ta_all_lines))
                .setChecked(p.getBoolean("text_anim_animate_all_lines", false)));
        items.add(UItem.asCheck(ID_DELETE_ANIM_ENABLED, LocaleController.getString(R.string.sg_ta_delete_anim))
                .setChecked(p.getBoolean("text_anim_delete_anim_enabled", true)));

        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(LocaleController.getString(R.string.sg_ta_section_effects)));
        items.add(UItem.asCheck(ID_BLUR_ENABLED, LocaleController.getString(R.string.sg_ta_blur))
                .setChecked(p.getBoolean("text_anim_blur_enabled", true)));
        items.add(UItem.asCheck(ID_SLIDE_ENABLED, LocaleController.getString(R.string.sg_ta_slide))
                .setChecked(p.getBoolean("text_anim_slide_enabled", true)));
        items.add(UItem.asCheck(ID_SCALE_ENABLED, LocaleController.getString(R.string.sg_ta_scale))
                .setChecked(p.getBoolean("text_anim_scale_enabled", false)));
        items.add(UItem.asCheck(ID_ROTATE_ENABLED, LocaleController.getString(R.string.sg_ta_rotate))
                .setChecked(p.getBoolean("text_anim_rotate_enabled", false)));

        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(LocaleController.getString(R.string.sg_ta_section_cursor)));
        items.add(UItem.asCheck(ID_CURSOR_ENABLED, LocaleController.getString(R.string.sg_ta_cursor_smooth))
                .setChecked(p.getBoolean("text_anim_cursor_enabled", true)));
        items.add(UItem.asCheck(ID_LIQUID_CURSOR, LocaleController.getString(R.string.sg_ta_cursor_liquid))
                .setChecked(p.getBoolean("text_anim_liquid_cursor_enabled", false)));
        items.add(UItem.asShadow(null));
    }

    @Override
    public void onClick(UItem item, View view, int position, float x, float y) {
        SharedPreferences p = TextAnimationLoader.getPreferences();
        int id = item.id;
        if (id == ID_ENABLED) {
            boolean v = !p.getBoolean("text_anim_enabled", true);
            p.edit().putBoolean("text_anim_enabled", v).apply();
            if (view instanceof org.telegram.ui.Cells.TextCheckCell) {
                ((org.telegram.ui.Cells.TextCheckCell) view).setChecked(v);
            }
        } else if (id == ID_IGNORE_SPACES) {
            boolean v = !p.getBoolean("text_anim_ignore_spaces", true);
            p.edit().putBoolean("text_anim_ignore_spaces", v).apply();
            if (view instanceof org.telegram.ui.Cells.TextCheckCell) {
                ((org.telegram.ui.Cells.TextCheckCell) view).setChecked(v);
            }
        } else if (id == ID_ANIMATE_ALL_LINES) {
            boolean v = !p.getBoolean("text_anim_animate_all_lines", false);
            p.edit().putBoolean("text_anim_animate_all_lines", v).apply();
            if (view instanceof org.telegram.ui.Cells.TextCheckCell) {
                ((org.telegram.ui.Cells.TextCheckCell) view).setChecked(v);
            }
        } else if (id == ID_DELETE_ANIM_ENABLED) {
            boolean v = !p.getBoolean("text_anim_delete_anim_enabled", true);
            p.edit().putBoolean("text_anim_delete_anim_enabled", v).apply();
            if (view instanceof org.telegram.ui.Cells.TextCheckCell) {
                ((org.telegram.ui.Cells.TextCheckCell) view).setChecked(v);
            }
        } else if (id == ID_BLUR_ENABLED) {
            boolean v = !p.getBoolean("text_anim_blur_enabled", true);
            p.edit().putBoolean("text_anim_blur_enabled", v).apply();
            if (view instanceof org.telegram.ui.Cells.TextCheckCell) {
                ((org.telegram.ui.Cells.TextCheckCell) view).setChecked(v);
            }
        } else if (id == ID_SLIDE_ENABLED) {
            boolean v = !p.getBoolean("text_anim_slide_enabled", true);
            p.edit().putBoolean("text_anim_slide_enabled", v).apply();
            if (view instanceof org.telegram.ui.Cells.TextCheckCell) {
                ((org.telegram.ui.Cells.TextCheckCell) view).setChecked(v);
            }
        } else if (id == ID_SCALE_ENABLED) {
            boolean v = !p.getBoolean("text_anim_scale_enabled", false);
            p.edit().putBoolean("text_anim_scale_enabled", v).apply();
            if (view instanceof org.telegram.ui.Cells.TextCheckCell) {
                ((org.telegram.ui.Cells.TextCheckCell) view).setChecked(v);
            }
        } else if (id == ID_ROTATE_ENABLED) {
            boolean v = !p.getBoolean("text_anim_rotate_enabled", false);
            p.edit().putBoolean("text_anim_rotate_enabled", v).apply();
            if (view instanceof org.telegram.ui.Cells.TextCheckCell) {
                ((org.telegram.ui.Cells.TextCheckCell) view).setChecked(v);
            }
        } else if (id == ID_CURSOR_ENABLED) {
            boolean v = !p.getBoolean("text_anim_cursor_enabled", true);
            p.edit().putBoolean("text_anim_cursor_enabled", v).apply();
            if (view instanceof org.telegram.ui.Cells.TextCheckCell) {
                ((org.telegram.ui.Cells.TextCheckCell) view).setChecked(v);
            }
        } else if (id == ID_LIQUID_CURSOR) {
            boolean v = !p.getBoolean("text_anim_liquid_cursor_enabled", false);
            p.edit().putBoolean("text_anim_liquid_cursor_enabled", v).apply();
            if (view instanceof org.telegram.ui.Cells.TextCheckCell) {
                ((org.telegram.ui.Cells.TextCheckCell) view).setChecked(v);
            }
        }
        TextAnimationLoader.updateSettings();
    }
}
