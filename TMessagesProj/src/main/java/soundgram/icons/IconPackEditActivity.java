package soundgram.icons;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import soundgram.icons.IconPackManager;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import soundgram.ui.OutlineTextInputLayout;
import org.telegram.ui.Components.RecyclerListView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class IconPackEditActivity extends BaseFragment {

    public static class DrawableInfo {
        public String name;
        public int resId;

        public DrawableInfo(String name, int resId) {
            this.name = name;
            this.resId = resId;
        }
    }

    private IconPackManager.IconPackInfo info;
    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private ArrayList<DrawableInfo> allDrawables = new ArrayList<>();
    private ArrayList<DrawableInfo> searchDrawables = new ArrayList<>();
    private HashMap<String, String> sessionCustomIcons = new HashMap<>();

    private boolean isSearchActive = false;

    private EditText nameEditText;
    private EditText authorEditText;
    private EditText versionEditText;

    private OutlineTextInputLayout nameInput;
    private OutlineTextInputLayout authorInput;
    private OutlineTextInputLayout versionInput;

    private android.widget.ImageView floatingButton;

    private String currentEditingResourceName;
    private IconEditBottomSheet activeEditSheet;

    public IconPackEditActivity(IconPackManager.IconPackInfo info) {
        super();
        this.info = info;
    }

    private boolean isRu() {
        return LocaleController.getInstance().getCurrentLocale().getLanguage().equals("ru");
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        // Scan R.drawable for customizable drawables
        java.lang.reflect.Field[] fields = org.telegram.messenger.R.drawable.class.getFields();
        for (java.lang.reflect.Field field : fields) {
            String name = field.getName();
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && field.getType() == int.class) {
                String lower = name.toLowerCase();
                if (lower.startsWith("abc_") || lower.startsWith("androidx_") || lower.startsWith("design_") || lower.startsWith("material_") || lower.startsWith("mtrl_") || lower.startsWith("notification_") || lower.startsWith("common_") || lower.startsWith("ic_launcher") || lower.startsWith("play_services_")) {
                    continue;
                }
                try {
                    int resId = field.getInt(null);
                    allDrawables.add(new DrawableInfo(name, resId));
                } catch (Exception ignore) {}
            }
        }
        allDrawables.sort((o1, o2) -> o1.name.compareTo(o2.name));

        // Load existing customizations
        File targetDir = info.path;
        File metadataFile = new File(targetDir, "metadata.json");
        if (metadataFile.exists()) {
            try {
                String jsonStr = new String(java.nio.file.Files.readAllBytes(metadataFile.toPath()), "UTF-8");
                JSONObject json = new JSONObject(jsonStr);
                JSONObject icons = json.optJSONObject("icons");
                if (icons != null) {
                    java.util.Iterator<String> keys = icons.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        String fileName = icons.getString(key);
                        File imgFile = new File(targetDir, fileName);
                        if (imgFile.exists()) {
                            sessionCustomIcons.put(key, imgFile.getAbsolutePath());
                        }
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }

        return true;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(isRu() ? "Настройка набора" : "Edit Icon Pack");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        org.telegram.ui.ActionBar.ActionBarMenuItem searchItem = actionBar.createMenu().addItem(1, R.drawable.ic_ab_search_solar).setIsSearchField(true);
        searchItem.setActionBarMenuItemSearchListener(new org.telegram.ui.ActionBar.ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {
                isSearchActive = true;
                searchDrawables.clear();
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onSearchCollapse() {
                isSearchActive = false;
                searchDrawables.clear();
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onTextChanged(android.widget.EditText editText) {
                String query = editText.getText().toString().trim().toLowerCase();
                searchDrawables.clear();
                if (!query.isEmpty()) {
                    for (DrawableInfo info : allDrawables) {
                        if (info.name.toLowerCase().contains(query)) {
                            searchDrawables.add(info);
                        }
                    }
                } else {
                    searchDrawables.addAll(allDrawables);
                }
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(listAdapter = new ListAdapter(context));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (isSearchActive) {
                DrawableInfo dInfo = searchDrawables.get(position);
                showEditIconBottomSheet(dInfo);
            } else {
                if (position >= 2) {
                    DrawableInfo dInfo = allDrawables.get(position - 2);
                    showEditIconBottomSheet(dInfo);
                }
            }
        });

        floatingButton = new android.widget.ImageView(context);
        floatingButton.setScaleType(android.widget.ImageView.ScaleType.CENTER);
        android.graphics.drawable.Drawable drawable = org.telegram.ui.ActionBar.Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(56), Theme.getColor(Theme.key_chats_actionBackground), Theme.getColor(Theme.key_chats_actionPressedBackground));
        if (android.os.Build.VERSION.SDK_INT < 21) {
            android.graphics.drawable.Drawable shadowDrawable = context.getResources().getDrawable(R.drawable.floating_shadow).mutate();
            shadowDrawable.setColorFilter(new android.graphics.PorterDuffColorFilter(0xff000000, android.graphics.PorterDuff.Mode.MULTIPLY));
            org.telegram.ui.Components.CombinedDrawable combinedDrawable = new org.telegram.ui.Components.CombinedDrawable(shadowDrawable, drawable, 0, 0);
            combinedDrawable.setIconSize(AndroidUtilities.dp(56), AndroidUtilities.dp(56));
            drawable = combinedDrawable;
        }
        floatingButton.setBackgroundDrawable(drawable);
        floatingButton.setColorFilter(new android.graphics.PorterDuffColorFilter(Theme.getColor(Theme.key_chats_actionIcon), android.graphics.PorterDuff.Mode.MULTIPLY));
        floatingButton.setImageResource(R.drawable.ic_ab_done);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            android.animation.StateListAnimator animator = new android.animation.StateListAnimator();
            animator.addState(new int[]{android.R.attr.state_pressed}, android.animation.ObjectAnimator.ofFloat(floatingButton, "translationZ", AndroidUtilities.dp(2), AndroidUtilities.dp(4)).setDuration(200));
            animator.addState(new int[]{}, android.animation.ObjectAnimator.ofFloat(floatingButton, "translationZ", AndroidUtilities.dp(4), AndroidUtilities.dp(2)).setDuration(200));
            floatingButton.setStateListAnimator(animator);
            floatingButton.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @android.annotation.SuppressLint("NewApi")
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setOval(0, 0, AndroidUtilities.dp(56), AndroidUtilities.dp(56));
                }
            });
        }
        frameLayout.addView(floatingButton, LayoutHelper.createFrame(android.os.Build.VERSION.SDK_INT >= 21 ? 56 : 60, android.os.Build.VERSION.SDK_INT >= 21 ? 56 : 60, (LocaleController.isRTL ? android.view.Gravity.LEFT : android.view.Gravity.RIGHT) | android.view.Gravity.BOTTOM, LocaleController.isRTL ? 14 : 0, 0, LocaleController.isRTL ? 0 : 14, 14));
        floatingButton.setOnClickListener(v -> savePack());

        return fragmentView;
    }

    private void showEditIconBottomSheet(DrawableInfo dInfo) {
        IconEditBottomSheet sheet = new IconEditBottomSheet(getParentActivity(), dInfo.name, dInfo.resId);
        showDialog(sheet);
    }

    private void savePack() {
        String name = nameEditText != null ? nameEditText.getText().toString().trim() : info.packName;
        String author = authorEditText != null ? authorEditText.getText().toString().trim() : info.author;
        String version = versionEditText != null ? versionEditText.getText().toString().trim() : info.version;

        if (name.isEmpty()) {
            Toast.makeText(getParentActivity(), isRu() ? "Введите название" : "Enter pack name", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File targetDir = info.path;
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            File metadataFile = new File(targetDir, "metadata.json");
            JSONObject json = new JSONObject();
            JSONObject iconsJson = new JSONObject();

            if (metadataFile.exists()) {
                try {
                    String jsonStr = new String(java.nio.file.Files.readAllBytes(metadataFile.toPath()), "UTF-8");
                    JSONObject oldJson = new JSONObject(jsonStr);
                    JSONObject oldIcons = oldJson.optJSONObject("icons");
                    if (oldIcons != null) {
                        java.util.Iterator<String> keys = oldIcons.keys();
                        while (keys.hasNext()) {
                            String k = keys.next();
                            iconsJson.put(k, oldIcons.getString(k));
                        }
                    }
                } catch (Exception ignore) {}
            }

            json.put("packId", info.packId);
            json.put("packName", name);
            json.put("author", author);
            json.put("version", version);

            for (java.util.Map.Entry<String, String> entry : sessionCustomIcons.entrySet()) {
                String resourceName = entry.getKey();
                String tempPath = entry.getValue();
                if (tempPath == null) {
                    iconsJson.remove(resourceName);
                    File oldFile = new File(targetDir, resourceName + ".png");
                    if (oldFile.exists()) {
                        oldFile.delete();
                    }
                } else {
                    File destFile = new File(targetDir, resourceName + ".png");
                    if (!tempPath.equals(destFile.getAbsolutePath())) {
                        Bitmap bitmap = BitmapFactory.decodeFile(tempPath);
                        if (bitmap != null) {
                            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                            }
                        }
                    }
                    iconsJson.put(resourceName, resourceName + ".png");
                }
            }

            json.put("icons", iconsJson);

            try (FileOutputStream fos = new FileOutputStream(metadataFile)) {
                fos.write(json.toString(4).getBytes("UTF-8"));
            }

            File previewFile = new File(targetDir, "preview.png");
            soundgram.icons.IconDialView dialView = new soundgram.icons.IconDialView(getParentActivity());
            ArrayList<Bitmap> sampleBitmaps = new ArrayList<>();
            for (String p : sessionCustomIcons.values()) {
                if (p != null) {
                    Bitmap bmp = BitmapFactory.decodeFile(p);
                    if (bmp != null) {
                        sampleBitmaps.add(bmp);
                        if (sampleBitmaps.size() >= 7) break;
                    }
                }
            }
            if (sampleBitmaps.size() < 7) {
                File[] files = targetDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && f.getName().endsWith(".png") && !f.getName().equals("preview.png")) {
                            if (!sessionCustomIcons.containsValue(f.getAbsolutePath())) {
                                Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                                if (bmp != null) {
                                    sampleBitmaps.add(bmp);
                                    if (sampleBitmaps.size() >= 7) break;
                                }
                            }
                        }
                    }
                }
            }

            if (!sampleBitmaps.isEmpty()) {
                dialView.setIcons(sampleBitmaps);
                int size = AndroidUtilities.dp(200);
                dialView.measure(View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY));
                dialView.layout(0, 0, size, size);
                Bitmap previewBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(previewBmp);
                dialView.draw(canvas);
                try (FileOutputStream fos = new FileOutputStream(previewFile)) {
                    previewBmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                }
                previewBmp.recycle();
                for (Bitmap b : sampleBitmaps) {
                    b.recycle();
                }
            }

            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("iconpacks", Context.MODE_PRIVATE);
            String activePackId = prefs.getString("active_pack", null);
            if (info.packId.equals(activePackId)) {
                IconPackManager.setActivePack(activePackId);
            }

            Toast.makeText(getParentActivity(), isRu() ? "Набор сохранен" : "Pack saved", Toast.LENGTH_SHORT).show();
            finishFragment();
        } catch (Exception e) {
            FileLog.e(e);
            Toast.makeText(getParentActivity(), isRu() ? "Ошибка сохранения" : "Save error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == 200 && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            try {
                Uri uri = data.getData();
                InputStream is = ApplicationLoader.applicationContext.getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                if (bitmap != null) {
                    int targetWidth = 96;
                    int targetHeight = 96;
                    try {
                        int resId = 0;
                        for (DrawableInfo dInfo : allDrawables) {
                            if (dInfo.name.equals(currentEditingResourceName)) {
                                resId = dInfo.resId;
                                break;
                            }
                        }
                        if (resId != 0) {
                            android.graphics.drawable.Drawable originalDrawable = ApplicationLoader.applicationContext.getResources().getDrawable(resId);
                            if (originalDrawable != null) {
                                int w = originalDrawable.getIntrinsicWidth();
                                int h = originalDrawable.getIntrinsicHeight();
                                if (w > 0 && h > 0) {
                                    targetWidth = w;
                                    targetHeight = h;
                                }
                            }
                        }
                    } catch (Exception ignore) {}

                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
                    
                    File tempDir = new File(ApplicationLoader.applicationContext.getCacheDir(), "edit_icons_temp");
                    tempDir.mkdirs();
                    File tempFile = new File(tempDir, currentEditingResourceName + "_temp.png");
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    }
                    if (scaledBitmap != bitmap) {
                        scaledBitmap.recycle();
                    }
                    bitmap.recycle();
                    
                    sessionCustomIcons.put(currentEditingResourceName, tempFile.getAbsolutePath());
                    
                    if (activeEditSheet != null) {
                        activeEditSheet.updateCustomView();
                    }
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                }
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    private class HeaderView extends LinearLayout {
        public HeaderView(Context context) {
            super(context);
            setOrientation(VERTICAL);
            setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16));
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

            nameInput = new OutlineTextInputLayout(context);
            nameInput.setLabel(isRu() ? "Название набора" : "Pack Name");
            nameInput.getEditText().setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            nameInput.getEditText().setSingleLine(true);
            nameInput.setText(info.packName);
            addView(nameInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56, 0, 0, 0, 16));

            authorInput = new OutlineTextInputLayout(context);
            authorInput.setLabel(isRu() ? "Имя автора" : "Author Name");
            authorInput.getEditText().setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            authorInput.getEditText().setSingleLine(true);
            authorInput.setText(info.author);
            addView(authorInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56, 0, 0, 0, 16));

            versionInput = new OutlineTextInputLayout(context);
            versionInput.setLabel(isRu() ? "Версия" : "Version");
            versionInput.getEditText().setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            versionInput.getEditText().setSingleLine(true);
            versionInput.setText(info.version);
            addView(versionInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56));
            
            nameEditText = nameInput.getEditText();
            authorEditText = authorInput.getEditText();
            versionEditText = versionInput.getEditText();
        }
    }

    private class IconEditBottomSheet extends BottomSheet {
        private String resourceName;
        private int resId;
        private BackupImageView originalView;
        private BackupImageView customView;
        
        public IconEditBottomSheet(Context context, String resourceName, int resId) {
            super(context, true);
            this.resourceName = resourceName;
            this.resId = resId;
            
            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20));
            
            TextView titleView = new TextView(context);
            titleView.setText(resourceName);
            titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            titleView.setTypeface(AndroidUtilities.bold());
            container.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));
            
            LinearLayout iconsLayout = new LinearLayout(context);
            iconsLayout.setOrientation(LinearLayout.HORIZONTAL);
            iconsLayout.setGravity(Gravity.CENTER);
            
            LinearLayout origLayout = new LinearLayout(context);
            origLayout.setOrientation(LinearLayout.VERTICAL);
            origLayout.setGravity(Gravity.CENTER);
            
            TextView origTitle = new TextView(context);
            origTitle.setText(isRu() ? "Оригинал" : "Original");
            origTitle.setTextColor(Theme.getColor(Theme.key_dialogTextGray));
            origTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            origLayout.addView(origTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
            
            originalView = new BackupImageView(context);
            originalView.setRoundRadius(AndroidUtilities.dp(8));
            originalView.setImageResource(resId);
            originalView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), PorterDuff.Mode.MULTIPLY));
            origLayout.addView(originalView, LayoutHelper.createLinear(64, 64));
            
            iconsLayout.addView(origLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
            
            LinearLayout custLayout = new LinearLayout(context);
            custLayout.setOrientation(LinearLayout.VERTICAL);
            custLayout.setGravity(Gravity.CENTER);
            
            TextView custTitle = new TextView(context);
            custTitle.setText(isRu() ? "Своя" : "Custom");
            custTitle.setTextColor(Theme.getColor(Theme.key_dialogTextGray));
            custTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            custLayout.addView(custTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));
            
            customView = new BackupImageView(context);
            customView.setRoundRadius(AndroidUtilities.dp(8));
            updateCustomView();
            
            customView.setOnClickListener(v -> {
                try {
                    currentEditingResourceName = resourceName;
                    activeEditSheet = IconEditBottomSheet.this;
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent, 200);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
            custLayout.addView(customView, LayoutHelper.createLinear(64, 64));
            
            iconsLayout.addView(custLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
            
            container.addView(iconsLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 24));
            
            TextView doneButton = new TextView(context);
            doneButton.setText(isRu() ? "Готово" : "Done");
            doneButton.setGravity(Gravity.CENTER);
            doneButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
            doneButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            doneButton.setTypeface(AndroidUtilities.bold());
            doneButton.setBackgroundDrawable(Theme.AdaptiveRipple.filledRect(Theme.getColor(Theme.key_featuredStickers_addButton), 8f));
            doneButton.setOnClickListener(v -> dismiss());
            container.addView(doneButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
            
            setCustomView(container);
        }
        
        public void updateCustomView() {
            String path = sessionCustomIcons.get(resourceName);
            if (path != null) {
                customView.setImage(path, "64_64", null);
            } else {
                customView.setImageResource(R.drawable.msg_theme);
            }
        }
    }

    private class DrawableCell extends FrameLayout {
        private TextView nameTextView;
        private BackupImageView originalIconView;
        private BackupImageView customIconView;
        private boolean needDivider;

        public DrawableCell(Context context) {
            super(context);
            
            originalIconView = new BackupImageView(context);
            originalIconView.setRoundRadius(AndroidUtilities.dp(8));
            originalIconView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), PorterDuff.Mode.MULTIPLY));
            addView(originalIconView, LayoutHelper.createFrame(40, 40, (LocaleController.isRTL ? android.view.Gravity.RIGHT : android.view.Gravity.LEFT) | android.view.Gravity.CENTER_VERTICAL, LocaleController.isRTL ? 0 : 21, 0, LocaleController.isRTL ? 21 : 0, 0));

            nameTextView = new TextView(context);
            nameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            nameTextView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 16);
            nameTextView.setLines(1);
            nameTextView.setMaxLines(1);
            nameTextView.setSingleLine(true);
            nameTextView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            nameTextView.setGravity((LocaleController.isRTL ? android.view.Gravity.RIGHT : android.view.Gravity.LEFT) | android.view.Gravity.CENTER_VERTICAL);
            addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? android.view.Gravity.RIGHT : android.view.Gravity.LEFT) | android.view.Gravity.CENTER_VERTICAL, LocaleController.isRTL ? 76 : 76, 0, LocaleController.isRTL ? 76 : 76, 0));

            customIconView = new BackupImageView(context);
            customIconView.setRoundRadius(AndroidUtilities.dp(8));
            addView(customIconView, LayoutHelper.createFrame(40, 40, (LocaleController.isRTL ? android.view.Gravity.LEFT : android.view.Gravity.RIGHT) | android.view.Gravity.CENTER_VERTICAL, LocaleController.isRTL ? 21 : 0, 0, LocaleController.isRTL ? 0 : 21, 0));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(56) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
        }

        public void setDrawable(DrawableInfo drawableInfo, String customPath, boolean divider) {
            nameTextView.setText(drawableInfo.name);
            originalIconView.setImageResource(drawableInfo.resId);
            
            if (customPath != null) {
                customIconView.setImage(customPath, "40_40", null);
                customIconView.setVisibility(VISIBLE);
            } else {
                customIconView.setVisibility(GONE);
            }
            
            needDivider = divider;
            setWillNotDraw(!divider);
            requestLayout();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (needDivider) {
                canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(76), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(76) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
            }
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            if (isSearchActive) return true;
            return position >= 2;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new HeaderView(mContext);
                    break;
                case 1:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                default:
                    view = new DrawableCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0:
                    // HeaderView input fields bind is self-contained in constructor
                    break;
                case 1:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    headerCell.setText(isRu() ? "Иконки" : "Icons");
                    break;
                case 2:
                    DrawableCell cell = (DrawableCell) holder.itemView;
                    if (isSearchActive) {
                        DrawableInfo dInfo = searchDrawables.get(position);
                        boolean isLast = position == searchDrawables.size() - 1;
                        cell.setDrawable(dInfo, sessionCustomIcons.get(dInfo.name), !isLast);
                    } else {
                        DrawableInfo dInfo = allDrawables.get(position - 2);
                        boolean isLast = position == allDrawables.size() + 2 - 1;
                        cell.setDrawable(dInfo, sessionCustomIcons.get(dInfo.name), !isLast);
                    }
                    break;
            }
        }

        @Override
        public int getItemCount() {
            if (isSearchActive) {
                return searchDrawables.size();
            }
            return allDrawables.size() + 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (isSearchActive) {
                return 2;
            }
            if (position == 0) return 0;
            if (position == 1) return 1;
            return 2;
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{DrawableCell.class, HeaderCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        return themeDescriptions;
    }
}
