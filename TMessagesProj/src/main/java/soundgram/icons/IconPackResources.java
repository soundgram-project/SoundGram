package soundgram.icons;

import android.content.res.AssetFileDescriptor;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Movie;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;

public class IconPackResources extends Resources {

    private final Resources baseResources;
    public static Resources apiBaseResources;

    public IconPackResources(Resources baseResources) {
        super(baseResources.getAssets(), baseResources.getDisplayMetrics(), baseResources.getConfiguration());
        this.baseResources = baseResources;
        apiBaseResources = baseResources;
    }

    @Override
    public Drawable getDrawable(int id, @Nullable Theme theme) throws NotFoundException {
        try {
            String name = getResourceEntryName(id);
            if (name != null) {
                Drawable custom = IconPackManager.getInstance().getDrawable(name);
                if (custom != null) {
                    return custom;
                }
            }
        } catch (Exception ignore) {
        }
        return baseResources.getDrawable(id, theme);
    }

    @Override
    public Drawable getDrawable(int id) throws NotFoundException {
        try {
            String name = getResourceEntryName(id);
            if (name != null) {
                Drawable custom = IconPackManager.getInstance().getDrawable(name);
                if (custom != null) {
                    return custom;
                }
            }
        } catch (Exception ignore) {
        }
        return baseResources.getDrawable(id);
    }

    @Override
    public Drawable getDrawableForDensity(int id, int density, @Nullable Theme theme) {
        try {
            String name = getResourceEntryName(id);
            if (name != null) {
                Drawable custom = IconPackManager.getInstance().getDrawable(name);
                if (custom != null) {
                    return custom;
                }
            }
        } catch (Exception ignore) {
        }
        return baseResources.getDrawableForDensity(id, density, theme);
    }

    @Override
    public Drawable getDrawableForDensity(int id, int density) throws NotFoundException {
        try {
            String name = getResourceEntryName(id);
            if (name != null) {
                Drawable custom = IconPackManager.getInstance().getDrawable(name);
                if (custom != null) {
                    return custom;
                }
            }
        } catch (Exception ignore) {
        }
        return baseResources.getDrawableForDensity(id, density);
    }
}
