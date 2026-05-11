package tw.nekomimi.nekogram.wsproxy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TgWsProxyService extends Service {

    private static final String TAG = "TgWsProxy";
    private static final int NOTIFICATION_ID = 101;
    private static final String CHANNEL_ID = "sg_proxy";

    public static final String ACTION_START = "sg.proxy.START";
    public static final String ACTION_STOP  = "sg.proxy.STOP";

    public static final String EXTRA_PORT       = "port";
    public static final String EXTRA_SECRET     = "secret";
    public static final String EXTRA_POOL_SIZE  = "pool";
    public static final String EXTRA_CF_ENABLED = "cf";
    public static final String EXTRA_CF_DOMAIN  = "cf_domain";
    public static final String EXTRA_SPOOF_SNI  = "spoof";
    public static final String EXTRA_SPOOF_DOMAIN = "spoof_domain";

    public static volatile boolean isRunning = false;

    private PowerManager.WakeLock wakeLock;
    private ExecutorService executor;
    private final AtomicBoolean stopInProgress = new AtomicBoolean(false);

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            tryAutoStart();
            return START_STICKY;
        }

        switch (intent.getAction()) {
            case ACTION_START:
                int port = intent.getIntExtra(EXTRA_PORT, 1443);
                String secret = intent.getStringExtra(EXTRA_SECRET);
                int pool = intent.getIntExtra(EXTRA_POOL_SIZE, 4);
                boolean cf = intent.getBooleanExtra(EXTRA_CF_ENABLED, true);
                String cfDomain = intent.getStringExtra(EXTRA_CF_DOMAIN);
                boolean spoof = intent.getBooleanExtra(EXTRA_SPOOF_SNI, false);
                String spoofDomain = intent.getStringExtra(EXTRA_SPOOF_DOMAIN);

                startProxy(port, secret != null ? secret : "", pool, cf,
                    cfDomain != null ? cfDomain : "", spoof,
                    spoofDomain != null ? spoofDomain : "vk.com");
                break;

            case ACTION_STOP:
                stopProxy();
                break;
        }
        return START_STICKY;
    }

    private void tryAutoStart() {
        SharedPreferences prefs = getSharedPreferences("wsproxy", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("wsproxy_enabled", false)) return;
        if (isRunning) return;

        int port = prefs.getInt("wsproxy_port", 1443);
        String secret = prefs.getString("wsproxy_secret", "");
        if (secret.isEmpty()) return;

        int pool = prefs.getInt("wsproxy_pool", 4);
        boolean cf = prefs.getBoolean("wsproxy_cf", true);
        String cfDomain = prefs.getString("wsproxy_cf_domain", "");
        boolean spoof = prefs.getBoolean("wsproxy_spoof", false);
        String spoofDomain = prefs.getString("wsproxy_spoof_domain", "vk.com");

        startProxy(port, secret, pool, cf, cfDomain, spoof, spoofDomain);
    }

    private void startProxy(int port, String secret, int poolSize, boolean cfEnabled,
            String cfDomain, boolean spoofSni, String spoofDomain) {

        if (isRunning || stopInProgress.get()) return;

        // Минимальное "невидимое" уведомление (требуется для foreground service)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "SG", NotificationManager.IMPORTANCE_NONE);
            ch.setSound(null, null);
            ch.enableVibration(false);
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(android.R.color.transparent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, notification);
        }

        acquireWakeLock();
        isRunning = true;
        stopInProgress.set(false);

        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                NativeProxy.setPoolSize(poolSize);
                NativeProxy.setCfProxyCacheDir(getCacheDir().getAbsolutePath());
                NativeProxy.setCfProxyConfig(cfEnabled, true, cfDomain);
                NativeProxy.setSpoofSni(spoofSni, spoofDomain);

                int result = NativeProxy.startProxy("127.0.0.1", port, "", secret, 0);
                if (result != 0) {
                    Log.e(TAG, "Start error: " + result);
                    stopProxy();
                    return;
                }

                for (int i = 0; i < 10; i++) {
                    Thread.sleep(500);
                    if (isPortOpen("127.0.0.1", port, 1000)) break;
                }

            } catch (Exception e) {
                Log.e(TAG, "Error", e);
                stopProxy();
            }
        });
    }

    private void stopProxy() {
        if (!isRunning && !stopInProgress.get()) return;
        stopInProgress.set(true);
        isRunning = false;

        if (executor != null) {
            executor.execute(() -> {
                try { NativeProxy.stopProxy(); } catch (Exception ignored) {}
            });
            executor.shutdown();
            executor = null;
        }

        releaseWakeLock();
        stopForeground(true);
        stopSelf();
        stopInProgress.set(false);
    }

    private boolean isPortOpen(String host, int port, int timeout) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (Exception e) { return false; }
    }

    private void acquireWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SG:Proxy");
            wakeLock.acquire(10 * 60 * 1000L);
        } catch (Exception e) { Log.w(TAG, "WakeLock", e); }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = null;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        stopProxy();
        super.onDestroy();
    }
}
