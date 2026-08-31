package soundgram.icons;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import soundgram.icons.IconPackManager;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import soundgram.icons.IconDialView;

public class IconPackBottomSheet extends BottomSheet {

    private File zipFile;
    private IconPackManager.IconPackInfo info;
    private boolean enableAfterInstall = true;

    public IconPackBottomSheet(Context context, File zipFile, Theme.ResourcesProvider resourcesProvider) {
        super(context, false, resourcesProvider);
        this.zipFile = zipFile;

        info = IconPackManager.parseZipMetadata(zipFile);
        if (info == null) {
            info = new IconPackManager.IconPackInfo();
            info.packName = "Invalid Icon Pack";
            info.author = "Unknown";
            info.version = "1.0";
        }

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(24), AndroidUtilities.dp(20), AndroidUtilities.dp(20));

        // Circular Dial Icon sample previews using IconDialView
        IconDialView dialView = new IconDialView(context);
        dialView.setIcons(loadSampleBitmaps());
        container.addView(dialView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        TextView titleView = new TextView(context);
        titleView.setText(info.packName != null ? info.packName : info.packId);
        titleView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        container.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 6));

        TextView subtitleView = new TextView(context);
        String version = info.version != null ? info.version : "1.0";
        String author = info.author != null ? info.author.trim() : "Unknown";

        // Let's create the clickable author text
        android.text.SpannableStringBuilder ssb = new android.text.SpannableStringBuilder();
        ssb.append("Версия ").append(version).append(" • Автор: ");
        int start = ssb.length();
        ssb.append(author);
        if (!author.equalsIgnoreCase("Unknown") && !author.isEmpty()) {
            final String finalAuthor = author;
            ssb.setSpan(new android.text.style.ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    String url;
                    if (finalAuthor.startsWith("http://") || finalAuthor.startsWith("https://")) {
                        url = finalAuthor;
                    } else {
                        String username = finalAuthor.startsWith("@") ? finalAuthor.substring(1) : finalAuthor;
                        url = "https://t.me/" + username;
                    }
                    org.telegram.messenger.browser.Browser.openUrl(context, url);
                    dismiss();
                }
                @Override
                public void updateDrawState(android.text.TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(getThemedColor(Theme.key_chat_messageLinkIn));
                    ds.setUnderlineText(true);
                }
            }, start, ssb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            subtitleView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        }
        subtitleView.setText(ssb);
        subtitleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitleView.setGravity(Gravity.CENTER_HORIZONTAL);
        container.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 24));

        TextView installButton = new TextView(context);
        int iconCount = getIconCount();
        if (iconCount > 0) {
            installButton.setText("Установить набор (" + iconCount + " иконок)");
        } else {
            installButton.setText("Установить набор");
        }
        installButton.setGravity(Gravity.CENTER);
        installButton.setTextColor(getThemedColor(Theme.key_featuredStickers_buttonText));
        installButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        installButton.setTypeface(AndroidUtilities.bold());
        installButton.setBackgroundDrawable(Theme.AdaptiveRipple.filledRect(getThemedColor(Theme.key_featuredStickers_addButton), 8f));
        installButton.setOnClickListener(v -> {
            if (info.packName.equals("Invalid Icon Pack")) {
                Toast.makeText(context, "Cannot apply invalid pack", Toast.LENGTH_SHORT).show();
                dismiss();
                return;
            }
            installPack();
        });
        container.addView(installButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 0, 0, 0, 16));

        CheckBoxCell checkBoxCell = new CheckBoxCell(context, 1);
        checkBoxCell.setBackgroundDrawable(Theme.getSelectorDrawable(false));
        checkBoxCell.setText("Включить после установки", "", enableAfterInstall, false);
        checkBoxCell.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), 0);
        checkBoxCell.setOnClickListener(v -> {
            enableAfterInstall = !enableAfterInstall;
            checkBoxCell.setChecked(enableAfterInstall, true);
        });
        container.addView(checkBoxCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

        setCustomView(container);
    }

    private ArrayList<Bitmap> loadSampleBitmaps() {
        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        if (zipFile.exists()) {
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
                ZipEntry entry;
                int count = 0;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory() && entry.getName().endsWith(".png") && !entry.getName().equals("preview.png")) {
                        Bitmap bmp = BitmapFactory.decodeStream(zis);
                        if (bmp != null) {
                            bitmaps.add(bmp);
                            count++;
                            if (count >= 7) break;
                        }
                    }
                }
            } catch (Exception ignore) {}
        }
        return bitmaps;
    }

    private int getIconCount() {
        if (zipFile.exists()) {
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals("metadata.json")) {
                        StringBuilder sb = new StringBuilder();
                        byte[] buffer = new byte[1024];
                        int read;
                        while ((read = zis.read(buffer)) != -1) {
                            sb.append(new String(buffer, 0, read, "UTF-8"));
                        }
                        org.json.JSONObject json = new org.json.JSONObject(sb.toString());
                        org.json.JSONObject icons = json.optJSONObject("icons");
                        if (icons != null) {
                            return icons.length();
                        }
                    }
                }
            } catch (Exception ignore) {}
        }
        return 0;
    }

    private void installPack() {
        boolean success = IconPackManager.installIconPack(zipFile);
        if (success) {
            if (enableAfterInstall) {
                // icon reset
                IconPackManager.setActivePack(info.packId);
            }
            dismiss();
            Toast.makeText(getContext(), "Icon Pack installed!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Failed to install Icon Pack", Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }
}
