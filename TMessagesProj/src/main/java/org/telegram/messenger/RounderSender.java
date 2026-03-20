
package org.telegram.messenger;

import android.media.MediaMetadataRetriever;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.PhotoViewer;

public class RounderSender {

    public static void sendRoundVideo(String path, VideoEditedInfo info, long dialogId, BaseFragment parentFragment) {
        if (parentFragment == null || path == null) return;

        // 1. Проверка на GIF (вместо Python split)
        if (path.toLowerCase().endsWith(".gif")) {
            showSimpleAlert(parentFragment, "Ошибка", "GIF-файлы нельзя отправлять кружком.");
            return;
        }

        int currentAccount = parentFragment.getCurrentAccount();

        try {
            // 2. Если пользователь не заходил в редактор (info == null), собираем данные файла
            if (info == null) {
                info = new VideoEditedInfo();
                info.originalPath = path;
                
                MediaMetadataRetriever mr = new MediaMetadataRetriever();
                mr.setDataSource(path);
                int w = Integer.parseInt(mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                int h = Integer.parseInt(mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                int r = Integer.parseInt(mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
                long durMs = Long.parseLong(mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                mr.release();

                boolean swapped = (r == 90 || r == 270);
                info.originalWidth = swapped ? h : w;
                info.originalHeight = swapped ? w : h;
                info.originalDuration = durMs * 1000L;
                info.rotationValue = r;
            }

            // 3. Считаем итоговые параметры (с учетом кропа из редактора)
            int w = info.resultWidth > 0 ? info.resultWidth : info.originalWidth;
            int h = info.resultHeight > 0 ? info.resultHeight : info.originalHeight;
            long duration = info.estimatedDuration > 0 ? info.estimatedDuration : info.originalDuration;
            long durationMs = duration / 1000L; 

            // 4. Проверки
            boolean isSquare = (w == h);
            boolean isTooLarge = (w > 1081 || h > 1081);
            boolean isTooLong = (durationMs > 60500); 

            if (!isSquare || isTooLarge || isTooLong) {
                showErrorDialog(parentFragment, !isSquare, isTooLarge, isTooLong);
                return;
            }

            // 5. Настройка флагов как в плагине
            info.roundVideo = true; 
            // info.muted = true; // Раскомментируй, если хочешь без звука

            final VideoEditedInfo finalInfo = info;
            AndroidUtilities.runOnUIThread(() -> {
                SendMessagesHelper.getInstance(currentAccount).prepareSendingVideo(
                        AccountInstance.getInstance(currentAccount),
                        path,
                        finalInfo,
                        null, null,
                        dialogId,
                        null, null, null, null, null,
                        0, null,
                        true, // isRoundVideo
                        0, 0, false, false, null, null, 0, 0L, 0L
                );

                if (PhotoViewer.hasInstance()) {
                    PhotoViewer.getInstance().closePhoto(false, false);
                }
                try {
                    parentFragment.dismissCurrentDialog();
                } catch (Exception ignored) {}
            });

        } catch (Exception e) {
            FileLog.e("Rounder Error", e);
        }
    }


private static void showSimpleAlert(BaseFragment fragment, String title, String text) {
        if (fragment == null || fragment.getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle(title);
        builder.setMessage(text);
        builder.setPositiveButton("OK", null);
        fragment.showDialog(builder.create());
    }

    private static void showErrorDialog(BaseFragment fragment, boolean ratioError, boolean sizeError, boolean timeError) {
        if (fragment == null || fragment.getParentActivity() == null) return;

        StringBuilder message = new StringBuilder("Видео не соответствует требованиям:\n\n");
        if (ratioError) message.append("• Должно быть квадратным (1:1).\n");
        if (sizeError)  message.append("• Разрешение не более 640x640.\n");
        if (timeError)  message.append("• Длительность не более 60 секунд.");

        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
        builder.setTitle("Ошибка");
        builder.setMessage(message.toString());
        builder.setPositiveButton("OK", null);
        fragment.showDialog(builder.create());
    }
}