package soundgram.icons;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import soundgram.icons.IconPackManager;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class IconPackPreviewBottomSheet extends BottomSheet {

    private IconPackManager.IconPackInfo packInfo;
    private Runnable onInstallCallback;
    private boolean enableAfterInstall = true;

    public IconPackPreviewBottomSheet(Context context, IconPackManager.IconPackInfo packInfo, Runnable onInstallCallback) {
        super(context, false);
        this.packInfo = packInfo;
        this.onInstallCallback = onInstallCallback;

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(24), AndroidUtilities.dp(20), AndroidUtilities.dp(20));

        // Circular Dial Icon sample previews using IconDialView
        IconDialView dialView = new IconDialView(context);
        dialView.setIcons(loadSampleBitmaps());
        container.addView(dialView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        TextView titleView = new TextView(context);
        titleView.setText(packInfo.packName != null ? packInfo.packName : packInfo.packId);
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        container.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 6));

        TextView subtitleView = new TextView(context);
        String version = packInfo.version != null ? packInfo.version : "1.0";
        String author = packInfo.author != null ? packInfo.author.trim() : "Unknown";

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
                    ds.setColor(Theme.getColor(Theme.key_chat_messageLinkIn));
                    ds.setUnderlineText(true);
                }
            }, start, ssb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            subtitleView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        }
        subtitleView.setText(ssb);
        subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitleView.setGravity(Gravity.CENTER_HORIZONTAL);
        container.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 24));

        TextView installButton = new TextView(context);
        int iconCount = getIconCount();
        boolean isInstalled = packInfo.path != null && packInfo.path.exists() && packInfo.path.isDirectory();
        boolean isActive = false;
        if (isInstalled) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("iconpacks", Context.MODE_PRIVATE);
            String activePackId = prefs.getString("active_pack", null);
            isActive = packInfo.packId.equals(activePackId);
        }

        if (isInstalled) {
            if (isActive) {
                installButton.setText("Применено");
                installButton.setEnabled(false);
                installButton.setAlpha(0.5f);
            } else {
                installButton.setText("Применить набор");
            }
        } else {
            if (iconCount > 0) {
                installButton.setText("Установить набор (" + iconCount + " иконок)");
            } else {
                installButton.setText("Установить набор");
            }
        }
        installButton.setGravity(Gravity.CENTER);
        installButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        installButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        installButton.setTypeface(AndroidUtilities.bold());
        installButton.setBackgroundDrawable(Theme.AdaptiveRipple.filledRect(Theme.getColor(Theme.key_featuredStickers_addButton), 8f));
        installButton.setOnClickListener(v -> installPack());
        container.addView(installButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 0, 0, 0, isInstalled ? 0 : 16));

        if (!isInstalled) {
            CheckBoxCell checkBoxCell = new CheckBoxCell(context, 1);
            checkBoxCell.setBackgroundDrawable(Theme.getSelectorDrawable(false));
            checkBoxCell.setText("Включить после установки", "", enableAfterInstall, false);
            checkBoxCell.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), 0);
            checkBoxCell.setOnClickListener(v -> {
                enableAfterInstall = !enableAfterInstall;
                checkBoxCell.setChecked(enableAfterInstall, true);
            });
            container.addView(checkBoxCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        }

        setCustomView(container);
    }

    private ArrayList<Bitmap> loadSampleBitmaps() {
        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        if (packInfo.path != null && packInfo.path.exists() && packInfo.path.isDirectory()) {
            File[] files = packInfo.path.listFiles();
            if (files != null) {
                int count = 0;
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith(".png") && !f.getName().equals("preview.png")) {
                        try {
                            Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                            if (bmp != null) {
                                bitmaps.add(bmp);
                                count++;
                                if (count >= 7) break;
                            }
                        } catch (Exception ignore) {}
                    }
                }
            }
        } else {
            File zipFile = new File(IconPackManager.getIconPacksDir(), "cached_import.zip");
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
        }
        return bitmaps;
    }

    private int getIconCount() {
        File metadataFile = null;
        if (packInfo.path != null) {
            metadataFile = new File(packInfo.path, "metadata.json");
        } else {
            File zipFile = new File(IconPackManager.getIconPacksDir(), "cached_import.zip");
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
        }

        if (metadataFile != null && metadataFile.exists()) {
            try {
                String jsonStr = new String(Files.readAllBytes(metadataFile.toPath()), "UTF-8");
                org.json.JSONObject json = new org.json.JSONObject(jsonStr);
                org.json.JSONObject icons = json.optJSONObject("icons");
                if (icons != null) {
                    return icons.length();
                }
            } catch (Exception ignore) {}
        }
        return 0;
    }

    private void installPack() {
        if (onInstallCallback != null) {
            onInstallCallback.run();
        }
        if (enableAfterInstall) {
            // icon reset
            IconPackManager.setActivePack(packInfo.packId);
        }
        dismiss();
    }
}
