/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Components;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.BasePermissionsActivity;
import org.telegram.ui.PhotoAlbumPickerActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class WallpaperUpdater {

    private String currentPicturePath;
    private File picturePath = null;
    private Activity parentActivity;
    private BaseFragment parentFragment;
    private WallpaperUpdaterDelegate delegate;
    private File currentWallpaperPath;
    public boolean currentWallpaperIsVideo;
    public String currentWallpaperVideoPath;

    public interface WallpaperUpdaterDelegate {
        void didSelectWallpaper(File file, Bitmap bitmap, boolean gallery);
        void needOpenColorPicker();
    }

    public WallpaperUpdater(Activity activity, BaseFragment fragment, WallpaperUpdaterDelegate wallpaperUpdaterDelegate) {
        parentActivity = activity;
        parentFragment = fragment;
        delegate = wallpaperUpdaterDelegate;
    }

    public void showAlert(final boolean fromTheme) {
        BottomSheet.Builder builder = new BottomSheet.Builder(parentActivity);
        builder.setTitle(LocaleController.getString(R.string.ChoosePhoto), true);

        CharSequence[] items;
        int[] icons;
        if (fromTheme) {
            items = new CharSequence[]{LocaleController.getString(R.string.ChooseTakePhoto), LocaleController.getString(R.string.SelectFromGallery), LocaleController.getString(R.string.SelectColor), LocaleController.getString(R.string.Default)};
            icons = null;
        } else {
            items = new CharSequence[]{LocaleController.getString(R.string.ChooseTakePhoto), LocaleController.getString(R.string.SelectFromGallery)};
            icons = new int[]{R.drawable.msg_camera, R.drawable.msg_photos};
        }

        builder.setItems(items, icons, (dialogInterface, i) -> {
            try {
                if (i == 0) {
                    try {
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        File image = AndroidUtilities.generatePicturePath();
                        if (image != null) {
                            if (Build.VERSION.SDK_INT >= 24) {
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(parentActivity, ApplicationLoader.getApplicationId() + ".provider", image));
                                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } else {
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image));
                            }
                            currentPicturePath = image.getAbsolutePath();
                        }
                        parentActivity.startActivityForResult(takePictureIntent, 10);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                } else if (i == 1) {
                    openGallery();
                } else if (fromTheme) {
                    if (i == 2) {
                        delegate.needOpenColorPicker();
                    } else if (i == 3) {
                        delegate.didSelectWallpaper(null, null, false);
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
        builder.show();
    }

    public void openGallery() {
        if (parentFragment != null) {
            final Activity activity = parentFragment.getParentActivity();
            if (activity != null) {
                if (Build.VERSION.SDK_INT >= 33) {
                    if (activity.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                        activity.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                        activity.requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO}, BasePermissionsActivity.REQUEST_CODE_EXTERNAL_STORAGE);
                        return;
                    }
                } else if (Build.VERSION.SDK_INT >= 23) {
                    if (activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, BasePermissionsActivity.REQUEST_CODE_EXTERNAL_STORAGE);
                        return;
                    }
                }
            }
            PhotoAlbumPickerActivity fragment = new PhotoAlbumPickerActivity(PhotoAlbumPickerActivity.SELECT_TYPE_WALLPAPER, false, false, null);
            fragment.setAllowSearchImages(false);
            fragment.setDelegate(new PhotoAlbumPickerActivity.PhotoAlbumPickerActivityDelegate() {
                @Override
                public void didSelectPhotos(ArrayList<SendMessagesHelper.SendingMediaInfo> photos, boolean notify, int scheduleDate) {
                    WallpaperUpdater.this.didSelectPhotos(photos);
                }

                @Override
                public void startPhotoSelectActivity() {
                    try {
                        Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
                        photoPickerIntent.setType("*/*");
                        photoPickerIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
                        parentActivity.startActivityForResult(photoPickerIntent, 11);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
            });
            parentFragment.presentFragment(fragment);
        } else {
            try {
                Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
                photoPickerIntent.setType("*/*");
                photoPickerIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
                parentActivity.startActivityForResult(photoPickerIntent, 11);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    private void didSelectPhotos(ArrayList<SendMessagesHelper.SendingMediaInfo> photos) {
        try {
            if (!photos.isEmpty()) {
                SendMessagesHelper.SendingMediaInfo info = photos.get(0);
                if (info.path != null) {
                    boolean isVideo = info.isVideo;
                    if (!isVideo) {
                        String lower = info.path.toLowerCase();
                        if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".3gp") || lower.endsWith(".avi")) {
                            isVideo = true;
                        }
                    }
                    if (isVideo) {
                        currentWallpaperIsVideo = true;
                        currentWallpaperVideoPath = info.path;
                        Bitmap bitmap = null;
                        android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                        try {
                            retriever.setDataSource(info.path);
                            bitmap = retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                        } catch (Exception e) {
                            FileLog.e(e);
                        } finally {
                            try {
                                retriever.release();
                            } catch (Exception ignored) {}
                        }
                        if (bitmap == null) {
                            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                        }
                        currentWallpaperPath = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), Utilities.random.nextInt() + ".jpg");
                        FileOutputStream stream = new FileOutputStream(currentWallpaperPath);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 87, stream);
                        stream.close();
                        delegate.didSelectWallpaper(currentWallpaperPath, bitmap, true);
                    } else {
                        currentWallpaperIsVideo = false;
                        currentWallpaperVideoPath = null;
                        currentWallpaperPath = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), Utilities.random.nextInt() + ".jpg");
                        Point screenSize = AndroidUtilities.getRealScreenSize();
                        Bitmap bitmap = ImageLoader.loadBitmap(info.path, null, screenSize.x, screenSize.y, true);
                        FileOutputStream stream = new FileOutputStream(currentWallpaperPath);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 87, stream);
                        stream.close();
                        delegate.didSelectWallpaper(currentWallpaperPath, bitmap, true);
                    }
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public void cleanup() {
        /*if (currentWallpaperPath != null) {
            currentWallpaperPath.delete();
        }*/
    }

    public String getCurrentPicturePath() {
        return currentPicturePath;
    }

    public void setCurrentPicturePath(String value) {
        currentPicturePath = value;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 10) {
                AndroidUtilities.addMediaToGallery(currentPicturePath);
                FileOutputStream stream = null;
                try {
                    currentWallpaperPath = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), Utilities.random.nextInt() + ".jpg");
                    Point screenSize = AndroidUtilities.getRealScreenSize();
                    Bitmap bitmap = ImageLoader.loadBitmap(currentPicturePath, null, screenSize.x, screenSize.y, true);
                    stream = new FileOutputStream(currentWallpaperPath);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 87, stream);
                    delegate.didSelectWallpaper(currentWallpaperPath, bitmap, false);
                } catch (Exception e) {
                    FileLog.e(e);
                } finally {
                    try {
                        if (stream != null) {
                            stream.close();
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
                currentPicturePath = null;
            } else if (requestCode == 11) {
                if (data == null || data.getData() == null) {
                    return;
                }
                try {
                    Uri uri = data.getData();
                    boolean isVideo = false;
                    String type = parentActivity.getContentResolver().getType(uri);
                    if (type == null) {
                        android.database.Cursor cursor = null;
                        try {
                            cursor = parentActivity.getContentResolver().query(uri, new String[]{android.provider.MediaStore.MediaColumns.MIME_TYPE}, null, null, null);
                            if (cursor != null && cursor.moveToFirst()) {
                                type = cursor.getString(0);
                            }
                        } catch (Exception ignored) {
                        } finally {
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                    }
                    if (type != null && type.startsWith("video/")) {
                        isVideo = true;
                    } else {
                        String path = AndroidUtilities.getPath(uri);
                        if (path != null) {
                            String lower = path.toLowerCase();
                            if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".3gp") || lower.endsWith(".avi")) {
                                isVideo = true;
                            }
                        }
                    }
                    if (isVideo) {
                        currentWallpaperIsVideo = true;
                        File videoFile = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), Utilities.random.nextInt() + ".mp4");
                        try (java.io.InputStream is = parentActivity.getContentResolver().openInputStream(uri);
                             FileOutputStream fos = new FileOutputStream(videoFile)) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, read);
                            }
                        }
                        currentWallpaperVideoPath = videoFile.getAbsolutePath();

                        Bitmap bitmap = null;
                        android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                        try {
                            retriever.setDataSource(parentActivity, uri);
                            bitmap = retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                        } catch (Exception e) {
                            FileLog.e(e);
                        } finally {
                            try {
                                retriever.release();
                            } catch (Exception ignored) {}
                        }
                        if (bitmap == null) {
                            try {
                                retriever = new android.media.MediaMetadataRetriever();
                                retriever.setDataSource(currentWallpaperVideoPath);
                                bitmap = retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                            } catch (Exception e) {
                                FileLog.e(e);
                            } finally {
                                try {
                                    retriever.release();
                                } catch (Exception ignored) {}
                            }
                        }
                        if (bitmap == null) {
                            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                        }
                        currentWallpaperPath = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), Utilities.random.nextInt() + ".jpg");
                        FileOutputStream stream = new FileOutputStream(currentWallpaperPath);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 87, stream);
                        stream.close();
                        delegate.didSelectWallpaper(currentWallpaperPath, bitmap, false);
                    } else {
                        currentWallpaperIsVideo = false;
                        currentWallpaperVideoPath = null;
                        currentWallpaperPath = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), Utilities.random.nextInt() + ".jpg");
                        Point screenSize = AndroidUtilities.getRealScreenSize();
                        Bitmap bitmap = ImageLoader.loadBitmap(null, uri, screenSize.x, screenSize.y, true);
                        FileOutputStream stream = new FileOutputStream(currentWallpaperPath);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 87, stream);
                        stream.close();
                        delegate.didSelectWallpaper(currentWallpaperPath, bitmap, false);
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        }
    }
}
