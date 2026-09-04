package tw.nekomimi.nekogram.ui.icons;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import xyz.nextalone.nagram.NaConfig;

@SuppressLint("UseCompatLoadingForDrawables")
public class IconsResources extends soundgram.icons.IconPackResources {

    public static final int ICON_REPLACE_SOLAR = 1;

    public IconsResources(Resources resources) {
        super(resources);
    }

    private Drawable getCustomDrawable(int id, @Nullable Theme theme, int density, boolean useDensity) {
        try {
            String name = getResourceEntryName(id);
            if (name != null) {
                Drawable custom = soundgram.icons.IconPackManager.getInstance().getDrawable(name);
                if (custom != null) {
                    return custom;
                }
            }
        } catch (Exception ignore) {}

        int convertedId = getConversion(id);
        if (convertedId != id) {
            try {
                String convertedName = getResourceEntryName(convertedId);
                if (convertedName != null) {
                    Drawable custom = soundgram.icons.IconPackManager.getInstance().getDrawable(convertedName);
                    if (custom != null) {
                        return custom;
                    }
                }
            } catch (Exception ignore) {}
        }

        try {
            if (useDensity) {
                Drawable d = super.getDrawableForDensity(convertedId, density, theme);
                if (d != null) return d;
            } else {
                Drawable d = super.getDrawable(convertedId, theme);
                if (d != null) return d;
            }
        } catch (Exception ignore) {}

        try {
            if (useDensity) {
                return super.getDrawableForDensity(id, density, theme);
            } else {
                return super.getDrawable(id, theme);
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Drawable getDrawable(int id) throws NotFoundException {
        Drawable d = getCustomDrawable(id, null, 0, false);
        if (d != null) {
            return d;
        }
        throw new NotFoundException();
    }

    @Override
    public Drawable getDrawable(int id, @Nullable Theme theme) throws NotFoundException {
        Drawable d = getCustomDrawable(id, theme, 0, false);
        if (d != null) {
            return d;
        }
        throw new NotFoundException();
    }

    @Nullable
    @Override
    public Drawable getDrawableForDensity(int id, int density, @Nullable Theme theme) {
        return getCustomDrawable(id, theme, density, true);
    }

    @Nullable
    @Override
    public Drawable getDrawableForDensity(int id, int density) throws NotFoundException {
        Drawable d = getCustomDrawable(id, null, density, true);
        if (d != null) {
            return d;
        }
        throw new NotFoundException();
    }

    private int getConversion(int icon) {
        return getConversion(icon, -1);
    }

    private int getConversion(int icon, int forcedIconsType) {
        int consideredIconsType = forcedIconsType == -1 ? NaConfig.INSTANCE.getIconReplacements().Int() : forcedIconsType;

        if (consideredIconsType == ICON_REPLACE_SOLAR) {
            return SolarIcons.Companion.getConversion(icon);
        }

        return icon;
    }
}
