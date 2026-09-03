package soundgram.icons;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.AccountInstance;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadioButton;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.TopicsFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import app.nimarkogram.messenger.NimarkoConfig;
import app.nimarkogram.messenger.icons.icon_replaces.BaseIconReplace;
import app.nimarkogram.messenger.icons.icon_replaces.LiquidGlassFullReplace;
import app.nimarkogram.messenger.icons.icon_replaces.NoIconReplace;
import app.nimarkogram.messenger.icons.icon_replaces.PlumpyFullReplace;
import app.nimarkogram.messenger.icons.icon_replaces.SolarIconReplace;

public class IconPacksActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private ArrayList<IconPackManager.IconPackInfo> installedPacks = new ArrayList<>();

    private int builtinHeaderRow;
    private int defaultPackRow;
    private int solarPackRow;
    private int liquidGlassPackRow;
    private int plumpyPackRow;

    private int installedHeaderRow;
    private int installedStartRow;
    private int installedEndRow;
    private int rowCount;

    private ImageView floatingButton;

    private static final BaseIconReplace REPLACE_DEFAULT = new NoIconReplace();
    private static final BaseIconReplace REPLACE_SOLAR = new SolarIconReplace();
    private static final BaseIconReplace REPLACE_LIQUID = new LiquidGlassFullReplace();
    private static final BaseIconReplace REPLACE_PLUMPY = new PlumpyFullReplace();

    private static final int[] SAMPLE_RES = {
            R.drawable.msg_settings, R.drawable.msg_calls, R.drawable.msg_folders,
            R.drawable.msg_camera, R.drawable.msg_contacts, R.drawable.msg_archive
    };

    private boolean isRu() {
        return LocaleController.getInstance().getCurrentLocale().getLanguage().equals("ru");
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateRows();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void updateRows() {
        installedPacks = IconPackManager.getInstalledPacks();
        rowCount = 0;
        builtinHeaderRow = rowCount++;
        defaultPackRow = rowCount++;
        solarPackRow = rowCount++;
        liquidGlassPackRow = rowCount++;
        plumpyPackRow = rowCount++;

        if (!installedPacks.isEmpty()) {
            installedHeaderRow = rowCount++;
            installedStartRow = rowCount;
            rowCount += installedPacks.size();
            installedEndRow = rowCount;
        } else {
            installedHeaderRow = -1;
            installedStartRow = -1;
            installedEndRow = -1;
        }
    }

    private static final int create_item = 2;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(isRu() ? "Наборы иконок" : "Icon Packs");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == create_item) {
                    showCreatePackDialog();
                }
            }
        });

        org.telegram.ui.ActionBar.ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(create_item, R.drawable.ic_ab_other);

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(listAdapter = new ListAdapter(context));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            boolean changed = false;
            if (position == defaultPackRow) {
                NimarkoConfig.setIconReplacement(NimarkoConfig.ICON_REPLACE_NONE);
                IconPackManager.setActivePack(null);
                changed = true;
            } else if (position == solarPackRow) {
                NimarkoConfig.setIconReplacement(NimarkoConfig.ICON_REPLACE_SOLAR);
                IconPackManager.setActivePack(null);
                changed = true;
            } else if (position == liquidGlassPackRow) {
                NimarkoConfig.setIconReplacement(NimarkoConfig.ICON_REPLACE_LIQUID_GLASS);
                IconPackManager.setActivePack(null);
                changed = true;
            } else if (position == plumpyPackRow) {
                NimarkoConfig.setIconReplacement(NimarkoConfig.ICON_REPLACE_PLUMPY);
                IconPackManager.setActivePack(null);
                changed = true;
            } else if (position >= installedStartRow && position < installedEndRow) {
                IconPackManager.IconPackInfo info = installedPacks.get(position - installedStartRow);
                IconPackPreviewBottomSheet sheet = new IconPackPreviewBottomSheet(getParentActivity(), info, () -> {
                    NimarkoConfig.setIconReplacement(NimarkoConfig.ICON_REPLACE_NONE);
                    IconPackManager.setActivePack(info.packId);
                    updateRows();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                });
                showDialog(sheet);
            }
            if (changed) {
                updateRows();
                listAdapter.notifyDataSetChanged();
                if (org.telegram.ui.LaunchActivity.instance != null) {
                    org.telegram.ui.LaunchActivity.instance.reloadResources();
                }
                Theme.reloadAllResources(ApplicationLoader.applicationContext);
                AndroidUtilities.runOnUIThread(() -> {
                    org.telegram.messenger.NotificationCenter.getGlobalInstance().postNotificationName(
                            org.telegram.messenger.NotificationCenter.needSetDayNightTheme,
                            Theme.getActiveTheme(),
                            Theme.isCurrentThemeNight(),
                            null,
                            -1
                    );
                });
            }
        });

        listView.setOnItemLongClickListener((view, position) -> {
            if (position >= installedStartRow && position < installedEndRow) {
                IconPackManager.IconPackInfo info = installedPacks.get(position - installedStartRow);
                showPackOptions(info);
                return true;
            }
            return false;
        });

        floatingButton = new ImageView(context);
        floatingButton.setScaleType(ImageView.ScaleType.CENTER);
        Drawable drawable = Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(56), Theme.getColor(Theme.key_chats_actionBackground), Theme.getColor(Theme.key_chats_actionPressedBackground));
        if (Build.VERSION.SDK_INT < 21) {
            Drawable shadowDrawable = context.getResources().getDrawable(R.drawable.floating_shadow).mutate();
            shadowDrawable.setColorFilter(new android.graphics.PorterDuffColorFilter(0xff000000, android.graphics.PorterDuff.Mode.MULTIPLY));
            org.telegram.ui.Components.CombinedDrawable combinedDrawable = new org.telegram.ui.Components.CombinedDrawable(shadowDrawable, drawable, 0, 0);
            combinedDrawable.setIconSize(AndroidUtilities.dp(56), AndroidUtilities.dp(56));
            drawable = combinedDrawable;
        }
        floatingButton.setBackgroundDrawable(drawable);
        floatingButton.setColorFilter(new android.graphics.PorterDuffColorFilter(Theme.getColor(Theme.key_chats_actionIcon), android.graphics.PorterDuff.Mode.MULTIPLY));
        floatingButton.setImageResource(R.drawable.ic_ab_other);
        if (Build.VERSION.SDK_INT >= 21) {
            android.animation.StateListAnimator animator = new android.animation.StateListAnimator();
            animator.addState(new int[]{android.R.attr.state_pressed}, android.animation.ObjectAnimator.ofFloat(floatingButton, "translationZ", AndroidUtilities.dp(2), AndroidUtilities.dp(4)).setDuration(200));
            animator.addState(new int[]{}, android.animation.ObjectAnimator.ofFloat(floatingButton, "translationZ", AndroidUtilities.dp(4), AndroidUtilities.dp(2)).setDuration(200));
            floatingButton.setStateListAnimator(animator);
            floatingButton.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setOval(0, 0, AndroidUtilities.dp(56), AndroidUtilities.dp(56));
                }
            });
        }
        frameLayout.addView(floatingButton, LayoutHelper.createFrame(Build.VERSION.SDK_INT >= 21 ? 56 : 60, Build.VERSION.SDK_INT >= 21 ? 56 : 60, (LocaleController.isRTL ? android.view.Gravity.LEFT : android.view.Gravity.RIGHT) | android.view.Gravity.BOTTOM, LocaleController.isRTL ? 14 : 0, 0, LocaleController.isRTL ? 0 : 14, 14));
        floatingButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setItems(new CharSequence[]{
                isRu() ? "Создать новый набор" : "Create new pack",
                isRu() ? "Импортировать иконпак" : "Import icon pack"
            }, (dialogInterface, i) -> {
                if (i == 0) {
                    showCreatePackDialog();
                } else if (i == 1) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(intent, 100);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
            });
            showDialog(builder.create());
        });

        return fragmentView;
    }

    private void showCreatePackDialog() {
        CreateIconPackBottomSheet sheet = new CreateIconPackBottomSheet(getParentActivity(), (info) -> {
            updateRows();
            listAdapter.notifyDataSetChanged();
            presentFragment(new IconPackEditActivity(info));
        });
        showDialog(sheet);
    }

    private void showPackOptions(IconPackManager.IconPackInfo info) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setItems(new CharSequence[]{
            isRu() ? "Изменить" : "Edit",
            isRu() ? "Поделиться" : "Share",
            isRu() ? "Удалить" : "Delete"
        }, new int[]{R.drawable.msg_edit, R.drawable.msg_share, R.drawable.msg_delete}, (dialogInterface, i) -> {
            if (i == 0) {
                presentFragment(new IconPackEditActivity(info));
            } else if (i == 1) {
                sharePack(info);
            } else if (i == 2) {
                IconPackManager.deleteDirectory(info.path);
                if (info.packId.equals(IconPackManager.getInstance().getActivePackId())) {
                    IconPackManager.setActivePack(null);
                }
                updateRows();
                listAdapter.notifyDataSetChanged();
            }
        });
        showDialog(builder.create());
    }

    private void sharePack(IconPackManager.IconPackInfo info) {
        if (info == null || info.path == null || !info.path.exists()) return;
        try {
            File cacheDir = new File(ApplicationLoader.applicationContext.getCacheDir(), "shared_packs");
            cacheDir.mkdirs();
            File zipFile = new File(cacheDir, info.packName + ".icons");
            if (zipFile.exists()) {
                zipFile.delete();
            }

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                File[] files = info.path.listFiles();
                if (files != null) {
                    byte[] buffer = new byte[4096];
                    for (File f : files) {
                        if (f.isFile()) {
                            zos.putNextEntry(new ZipEntry(f.getName()));
                            try (FileInputStream fis = new FileInputStream(f)) {
                                int len;
                                while ((len = fis.read(buffer)) > 0) {
                                    zos.write(buffer, 0, len);
                                }
                            }
                            zos.closeEntry();
                        }
                    }
                }
            }

            Bundle args = new Bundle();
            args.putBoolean("onlySelect", true);
            args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_FORWARD);
            args.putString("selectAlertString", isRu() ? "Поделиться набором иконок" : "Share icon pack");
            args.putString("selectAlertStringGroup", isRu() ? "Поделиться набором иконок в группе" : "Share icon pack in group");
            DialogsActivity fragment = new DialogsActivity(args);
            fragment.setDelegate(new DialogsActivity.DialogsActivityDelegate() {
                @Override
                public boolean didSelectDialogs(DialogsActivity fragment, ArrayList<MessagesStorage.TopicKey> dids, CharSequence message, boolean param, boolean notify, int scheduleDate, int scheduleRepeatPeriod, TopicsFragment topicsFragment) {
                    if (dids != null && !dids.isEmpty()) {
                        for (MessagesStorage.TopicKey topicKey : dids) {
                            SendMessagesHelper.prepareSendingDocument(
                                AccountInstance.getInstance(currentAccount),
                                zipFile.getAbsolutePath(),
                                zipFile.getAbsolutePath(),
                                null,
                                null,
                                "application/zip",
                                topicKey.dialogId,
                                null,
                                null,
                                null,
                                null,
                                null,
                                notify,
                                scheduleDate,
                                null,
                                null,
                                false
                            );
                            if (!TextUtils.isEmpty(message)) {
                                SendMessagesHelper.prepareSendingText(AccountInstance.getInstance(currentAccount), message, topicKey.dialogId, notify, scheduleDate, scheduleRepeatPeriod, 0);
                            }
                        }
                        fragment.finishFragment();
                        return true;
                    }
                    return false;
                }
            });
            presentFragment(fragment);
        } catch (Exception e) {
            FileLog.e(e);
            Toast.makeText(getParentActivity(), isRu() ? "Ошибка экспорта набора" : "Pack export error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100 && resultCode == android.app.Activity.RESULT_OK && data != null) {
            try {
                Uri uri = data.getData();
                if (uri != null) {
                    InputStream is = ApplicationLoader.applicationContext.getContentResolver().openInputStream(uri);
                    File tempFile = new File(IconPackManager.getIconPacksDir(), "temp.zip");
                    FileOutputStream fos = new FileOutputStream(tempFile);
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                    fos.close();
                    is.close();

                    IconPackManager.IconPackInfo info = IconPackManager.parseZipMetadata(tempFile);
                    if (info != null) {
                        File cachedZip = new File(IconPackManager.getIconPacksDir(), "cached_import.zip");
                        tempFile.renameTo(cachedZip);
                        
                        IconPackPreviewBottomSheet previewSheet = new IconPackPreviewBottomSheet(getParentActivity(), info, () -> {
                            if (IconPackManager.installIconPack(cachedZip)) {
                                IconPackManager.setActivePack(info.packId);
                                Toast.makeText(ApplicationLoader.applicationContext, isRu() ? "Набор иконок установлен и применен" : "Icon pack installed and applied", Toast.LENGTH_SHORT).show();
                                updateRows();
                                if (listAdapter != null) {
                                    listAdapter.notifyDataSetChanged();
                                }
                            } else {
                                Toast.makeText(ApplicationLoader.applicationContext, isRu() ? "Ошибка установки" : "Installation error", Toast.LENGTH_SHORT).show();
                            }
                            cachedZip.delete();
                        });
                        previewSheet.setOnDismissListener(dialog -> {
                            if (cachedZip.exists()) {
                                cachedZip.delete();
                            }
                        });
                        showDialog(previewSheet);
                    } else {
                        Toast.makeText(ApplicationLoader.applicationContext, isRu() ? "Неверный формат набора" : "Invalid pack format", Toast.LENGTH_SHORT).show();
                        tempFile.delete();
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    private class IconPackCell extends FrameLayout {
        private TextView nameTextView;
        private TextView valueTextView;
        private BackupImageView imageView;
        private IconDialView dialView;
        private ImageView checkImageView;
        private RadioButton radioButton;
        public ImageView optionsButton;
        private boolean needDivider;

        public IconPackCell(Context context) {
            super(context);
            
            imageView = new BackupImageView(context);
            imageView.setRoundRadius(AndroidUtilities.dp(8));
            imageView.setVisibility(GONE);
            addView(imageView, LayoutHelper.createFrame(40, 40, (LocaleController.isRTL ? android.view.Gravity.RIGHT : android.view.Gravity.LEFT) | android.view.Gravity.CENTER_VERTICAL, LocaleController.isRTL ? 0 : 21, 0, LocaleController.isRTL ? 21 : 0, 0));

            dialView = new IconDialView(context, 0.25f);
            addView(dialView, LayoutHelper.createFrame(50, 50, (LocaleController.isRTL ? android.view.Gravity.RIGHT : android.view.Gravity.LEFT) | android.view.Gravity.CENTER_VERTICAL, LocaleController.isRTL ? 0 : 16, 0, LocaleController.isRTL ? 16 : 0, 0));

            checkImageView = new ImageView(context);
            checkImageView.setImageResource(R.drawable.round_check2);
            checkImageView.setColorFilter(new android.graphics.PorterDuffColorFilter(Theme.getColor(Theme.key_featuredStickers_addButton), android.graphics.PorterDuff.Mode.MULTIPLY));
            addView(checkImageView, LayoutHelper.createFrame(20, 20, (LocaleController.isRTL ? android.view.Gravity.RIGHT : android.view.Gravity.LEFT) | android.view.Gravity.TOP, LocaleController.isRTL ? 0 : 46, 37, LocaleController.isRTL ? 46 : 0, 0));

            nameTextView = new TextView(context);
            nameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            nameTextView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 16);
            nameTextView.setLines(1);
            nameTextView.setMaxLines(1);
            nameTextView.setSingleLine(true);
            nameTextView.setEllipsize(TextUtils.TruncateAt.END);
            nameTextView.setGravity((LocaleController.isRTL ? android.view.Gravity.RIGHT : android.view.Gravity.LEFT) | android.view.Gravity.CENTER_VERTICAL);
            addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? android.view.Gravity.RIGHT : android.view.Gravity.LEFT) | android.view.Gravity.TOP, LocaleController.isRTL ? 64 : 76, 10, LocaleController.isRTL ? 76 : 64, 0));

            valueTextView = new TextView(context);
            valueTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            valueTextView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 13);
            valueTextView.setLines(1);
            valueTextView.setMaxLines(1);
            valueTextView.setSingleLine(true);
            valueTextView.setEllipsize(TextUtils.TruncateAt.END);
            valueTextView.setGravity((LocaleController.isRTL ? android.view.Gravity.RIGHT : android.view.Gravity.LEFT) | android.view.Gravity.CENTER_VERTICAL);
            addView(valueTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? android.view.Gravity.RIGHT : android.view.Gravity.LEFT) | android.view.Gravity.TOP, LocaleController.isRTL ? 64 : 76, 33, LocaleController.isRTL ? 76 : 64, 0));

            radioButton = new RadioButton(context);
            radioButton.setSize(AndroidUtilities.dp(20));
            radioButton.setColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_radioBackgroundChecked));
            addView(radioButton, LayoutHelper.createFrame(22, 22, (LocaleController.isRTL ? android.view.Gravity.LEFT : android.view.Gravity.RIGHT) | android.view.Gravity.CENTER_VERTICAL, LocaleController.isRTL ? 22 : 0, 0, LocaleController.isRTL ? 0 : 22, 0));

            optionsButton = new ImageView(context);
            optionsButton.setScaleType(ImageView.ScaleType.CENTER);
            optionsButton.setImageResource(R.drawable.ic_ab_other);
            optionsButton.setColorFilter(new android.graphics.PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), android.graphics.PorterDuff.Mode.MULTIPLY));
            optionsButton.setBackgroundDrawable(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
            addView(optionsButton, LayoutHelper.createFrame(40, 40, (LocaleController.isRTL ? android.view.Gravity.LEFT : android.view.Gravity.RIGHT) | android.view.Gravity.CENTER_VERTICAL, LocaleController.isRTL ? 12 : 0, 0, LocaleController.isRTL ? 0 : 12, 0));
        }

        private ArrayList<Bitmap> getSampleBitmapsForPack(String pathString, BaseIconReplace replacement) {
            ArrayList<Bitmap> bitmaps = new ArrayList<>();
            if (pathString != null) {
                File dir = new File(pathString);
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        int count = 0;
                        for (File f : files) {
                            if (f.isFile() && f.getName().endsWith(".png") && !f.getName().equals("preview.png")) {
                                try {
                                    Bitmap bmp = android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath());
                                    if (bmp != null) {
                                        bitmaps.add(bmp);
                                        count++;
                                        if (count >= 7) break;
                                    }
                                } catch (Exception ignore) {}
                            }
                        }
                    }
                }
            } else if (replacement != null) {
                for (int stockId : SAMPLE_RES) {
                    try {
                        int resId = replacement.wrap(stockId);
                        Drawable drawable = getContext().getResources().getDrawable(resId);
                        Bitmap bitmap = null;
                        if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
                            bitmap = ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
                        } else if (drawable != null) {
                            int w = Math.max(1, drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : AndroidUtilities.dp(24));
                            int h = Math.max(1, drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : AndroidUtilities.dp(24));
                            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bitmap);
                            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                            drawable.draw(canvas);
                        }
                        if (bitmap != null) {
                            bitmaps.add(bitmap);
                        }
                    } catch (Exception ignore) {}
                }
            }
            return bitmaps;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
        }

        public void setBuiltinPack(String name, String author, BaseIconReplace replacement, boolean checked, boolean divider) {
            nameTextView.setText(name);
            valueTextView.setText(author);
            if (checked) {
                nameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
            } else {
                nameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            }
            
            ArrayList<Bitmap> bitmaps = getSampleBitmapsForPack(null, replacement);
            dialView.setIcons(bitmaps);
            dialView.setVisibility(VISIBLE);
            
            radioButton.setVisibility(VISIBLE);
            radioButton.setChecked(checked, false);
            optionsButton.setVisibility(GONE);
            checkImageView.setVisibility(GONE);
            
            needDivider = divider;
            setWillNotDraw(!divider);
            requestLayout();
        }

        public void setCustomPack(String name, String author, String iconPath, boolean checked, boolean divider) {
            nameTextView.setText(name);
            valueTextView.setText(author);
            if (checked) {
                nameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
            } else {
                nameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            }
            
            String folderPath = null;
            if (iconPath != null) {
                int idx = iconPath.lastIndexOf('/');
                if (idx != -1) {
                    folderPath = iconPath.substring(0, idx);
                }
            }
            
            ArrayList<Bitmap> bitmaps = getSampleBitmapsForPack(folderPath, null);
            dialView.setIcons(bitmaps);
            dialView.setVisibility(VISIBLE);
            
            radioButton.setVisibility(GONE);
            optionsButton.setVisibility(VISIBLE);
            checkImageView.setVisibility(checked ? VISIBLE : GONE);
            
            needDivider = divider;
            setWillNotDraw(!divider);
            requestLayout();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (needDivider && Theme.dividerPaint != null) {
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
            return position == defaultPackRow || position == solarPackRow || position == liquidGlassPackRow || position == plumpyPackRow || (position >= installedStartRow && position < installedEndRow);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1:
                default:
                    view = new IconPackCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == builtinHeaderRow) {
                        headerCell.setText(isRu() ? "Базовые наборы" : "Built-in Packs");
                    } else if (position == installedHeaderRow) {
                        headerCell.setText(isRu() ? "Включённые наборы" : "Installed Packs");
                    }
                    break;
                }
                case 1: {
                    IconPackCell cell = (IconPackCell) holder.itemView;
                    String activePackId = IconPackManager.getInstance().getActivePackId();
                    int currentReplace = NimarkoConfig.iconReplacement;

                    if (position == defaultPackRow) {
                        cell.setBuiltinPack(LocaleController.getString(R.string.NM_IconPack_DefaultTitle), LocaleController.getString(R.string.NM_IconPack_DefaultSubtitle), REPLACE_DEFAULT, activePackId == null && currentReplace == NimarkoConfig.ICON_REPLACE_NONE, true);
                    } else if (position == solarPackRow) {
                        cell.setBuiltinPack(LocaleController.getString(R.string.NM_IconPack_SolarTitle), LocaleController.getString(R.string.NM_IconPack_SolarSubtitle), REPLACE_SOLAR, activePackId == null && currentReplace == NimarkoConfig.ICON_REPLACE_SOLAR, true);
                    } else if (position == liquidGlassPackRow) {
                        cell.setBuiltinPack(LocaleController.getString(R.string.NM_IconPack_LiquidTitle), LocaleController.getString(R.string.NM_IconPack_LiquidSubtitle), REPLACE_LIQUID, activePackId == null && currentReplace == NimarkoConfig.ICON_REPLACE_LIQUID_GLASS, true);
                    } else if (position == plumpyPackRow) {
                        cell.setBuiltinPack(LocaleController.getString(R.string.NM_IconPack_PlumpyTitle), LocaleController.getString(R.string.NM_IconPack_PlumpySubtitle), REPLACE_PLUMPY, activePackId == null && currentReplace == NimarkoConfig.ICON_REPLACE_PLUMPY, false);
                    } else if (position >= installedStartRow && position < installedEndRow) {
                        IconPackManager.IconPackInfo info = installedPacks.get(position - installedStartRow);
                        boolean isLast = position == installedEndRow - 1;
                        cell.setCustomPack(info.packName, "by " + info.author, info.path + "/preview.png", info.packId.equals(activePackId), !isLast);
                        cell.optionsButton.setOnClickListener(v -> showPackOptions(info));
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == builtinHeaderRow || position == installedHeaderRow) {
                return 0;
            }
            return 1;
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{IconPackCell.class, HeaderCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        return themeDescriptions;
    }
}
