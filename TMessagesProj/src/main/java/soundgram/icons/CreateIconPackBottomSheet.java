package soundgram.icons;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import soundgram.icons.IconPackManager;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import soundgram.ui.OutlineTextInputLayout;

import java.io.File;
import java.io.FileOutputStream;

public class CreateIconPackBottomSheet extends BottomSheet {

    private OutlineTextInputLayout nameLayout;
    private OutlineTextInputLayout authorLayout;
    private OutlineTextInputLayout versionLayout;
    private IconPackManager.IconPackInfo editInfo;
    private org.telegram.messenger.Utilities.Callback<IconPackManager.IconPackInfo> onCreatedCallback;

    public CreateIconPackBottomSheet(Context context, org.telegram.messenger.Utilities.Callback<IconPackManager.IconPackInfo> onCreatedCallback) {
        this(context, null, onCreatedCallback);
    }

    public CreateIconPackBottomSheet(Context context, IconPackManager.IconPackInfo editInfo, org.telegram.messenger.Utilities.Callback<IconPackManager.IconPackInfo> onCreatedCallback) {
        super(context, true);
        this.editInfo = editInfo;
        this.onCreatedCallback = onCreatedCallback;

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20));

        TextView titleView = new TextView(context);
        titleView.setText(editInfo != null ? "Редактировать набор" : "Новый набор иконок");
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        container.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        nameLayout = new OutlineTextInputLayout(context);
        nameLayout.setLabel("Название набора");
        nameLayout.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nameLayout.getEditText().setSingleLine(true);
        if (editInfo != null) nameLayout.setText(editInfo.packName);
        container.addView(nameLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56, 0, 0, 0, 16));

        authorLayout = new OutlineTextInputLayout(context);
        authorLayout.setLabel("Имя автора");
        authorLayout.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        authorLayout.getEditText().setSingleLine(true);
        if (editInfo != null) authorLayout.setText(editInfo.author);
        container.addView(authorLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56, 0, 0, 0, 16));

        versionLayout = new OutlineTextInputLayout(context);
        versionLayout.setLabel("Версия");
        versionLayout.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        versionLayout.getEditText().setSingleLine(true);
        versionLayout.setText(editInfo != null ? editInfo.version : "1.0");
        container.addView(versionLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56, 0, 0, 0, 24));

        TextView saveButton = new TextView(context);
        saveButton.setText(editInfo != null ? "Сохранить" : "Создать");
        saveButton.setGravity(Gravity.CENTER);
        saveButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        saveButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        saveButton.setTypeface(AndroidUtilities.bold());
        saveButton.setBackgroundDrawable(Theme.AdaptiveRipple.filledRect(Theme.getColor(Theme.key_featuredStickers_addButton), 8f));
        saveButton.setOnClickListener(v -> saveOrCreatePack());
        container.addView(saveButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));

        setCustomView(container);
    }

    private void saveOrCreatePack() {
        String name = nameLayout.getEditText().getText().toString().trim();
        String author = authorLayout.getEditText().getText().toString().trim();
        String version = versionLayout.getEditText().getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Введите название", Toast.LENGTH_SHORT).show();
            return;
        }
        if (author.isEmpty()) author = "Unknown";
        if (version.isEmpty()) version = "1.0";

        try {
            String packId = editInfo != null ? editInfo.packId : "pack_" + System.currentTimeMillis();
            File targetDir = editInfo != null ? editInfo.path : new File(IconPackManager.getIconPacksDir(), packId);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            File metadataFile = new File(targetDir, "metadata.json");
            org.json.JSONObject json;
            if (editInfo != null && metadataFile.exists()) {
                try {
                    String jsonStr = new String(java.nio.file.Files.readAllBytes(metadataFile.toPath()), "UTF-8");
                    json = new org.json.JSONObject(jsonStr);
                } catch (Exception e) {
                    json = new org.json.JSONObject();
                }
            } else {
                json = new org.json.JSONObject();
                json.put("icons", new org.json.JSONObject());
            }

            json.put("packId", packId);
            json.put("packName", name);
            json.put("author", author);
            json.put("version", version);

            try (FileOutputStream fos = new FileOutputStream(metadataFile)) {
                fos.write(json.toString(4).getBytes("UTF-8"));
            }

            Toast.makeText(ApplicationLoader.applicationContext, editInfo != null ? "Набор изменен" : "Создано: " + name, Toast.LENGTH_SHORT).show();
            if (onCreatedCallback != null) {
                IconPackManager.IconPackInfo newInfo = new IconPackManager.IconPackInfo();
                newInfo.packId = packId;
                newInfo.packName = name;
                newInfo.author = author;
                newInfo.version = version;
                newInfo.path = targetDir;
                onCreatedCallback.run(newInfo);
            }
            dismiss();
        } catch (Exception e) {
            FileLog.e(e);
            Toast.makeText(ApplicationLoader.applicationContext, "Ошибка при сохранении", Toast.LENGTH_SHORT).show();
        }
    }
}
