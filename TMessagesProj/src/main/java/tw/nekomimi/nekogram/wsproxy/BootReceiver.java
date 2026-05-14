package tw.nekomimi.nekogram.wsproxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        SharedPreferences prefs = context.getSharedPreferences("tgwsproxy", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("auto_start", true)) return;  // default true per screenshot
        if (!prefs.getBoolean("enabled", false)) return;

        String secret = prefs.getString("secret", "");
        if (secret.isEmpty()) return;

        Intent serviceIntent = new Intent(context, TgWsProxyService.class);
        serviceIntent.setAction(TgWsProxyService.ACTION_START);
        serviceIntent.putExtra(TgWsProxyService.EXTRA_PORT,        prefs.getInt("port",        1443));
        serviceIntent.putExtra(TgWsProxyService.EXTRA_SECRET,      secret);
        serviceIntent.putExtra(TgWsProxyService.EXTRA_POOL_SIZE,   prefs.getInt("pool_size",   6));
        serviceIntent.putExtra(TgWsProxyService.EXTRA_RECV_BUF,    prefs.getInt("buffer",      256));
        serviceIntent.putExtra(TgWsProxyService.EXTRA_SEND_BUF,    prefs.getInt("buffer",      256));
        serviceIntent.putExtra(TgWsProxyService.EXTRA_CF_ENABLED,  prefs.getBoolean("cf_enabled",  true));
        serviceIntent.putExtra(TgWsProxyService.EXTRA_CF_DOMAIN,   prefs.getString("cf_domain",    ""));
        serviceIntent.putExtra(TgWsProxyService.EXTRA_FAKE_TLS,    prefs.getBoolean("fake_tls",    true));
        serviceIntent.putExtra(TgWsProxyService.EXTRA_FAKE_DOMAIN, prefs.getString("fake_domain",  "google.com"));
        serviceIntent.putExtra(TgWsProxyService.EXTRA_VERBOSE,     prefs.getInt("verbose",         0));
        serviceIntent.putExtra(TgWsProxyService.EXTRA_DC_IPS,      prefs.getString("dc_ips",       ""));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
