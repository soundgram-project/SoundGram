package org.telegram.messenger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.telegram.ui.LaunchActivity;

public class SoundGramPrivacy {
    public static Notification buildForegroundNotification(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm.getNotificationChannel("soundgram_bg") == null) {
                NotificationChannel ch = new NotificationChannel("soundgram_bg", "Фоновый перехват", NotificationManager.IMPORTANCE_MIN);
                ch.setShowBadge(false);
                ch.enableLights(false);
                ch.enableVibration(false);
                nm.createNotificationChannel(ch);
            }
        }

        Intent intent = new Intent(context, LaunchActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent, flags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                new Notification.Builder(context, "soundgram_bg") : new Notification.Builder(context);

        return builder.setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("SoundGram Privacy")
                .setContentText("Перехват сообщений активен")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }
}
