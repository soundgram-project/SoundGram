package soundgram.wsproxy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Checks whether Telegram DCs are reachable.
 * Monitors network changes and VPN state to auto-enable/disable proxy.
 */
public class ProxyAutoDetector implements NotificationCenter.NotificationCenterDelegate {

    private static final String TAG = "ProxyAutoDetect";

    private static final String TG_HOST    = "149.154.167.51";
    private static final int    TG_PORT    = 443;
    private static final int    TG_TIMEOUT = 3000;

    private static final String GOOGLE_HOST    = "8.8.8.8";
    private static final int    GOOGLE_PORT    = 53;
    private static final int    GOOGLE_TIMEOUT = 2000;

    private static final int PORT_WAIT_TIMEOUT_MS = 8_000;
    private static final int PORT_WAIT_INTERVAL_MS = 300;

    private static ConnectivityManager connectivityManager;
    private static ConnectivityManager.NetworkCallback networkCallback;
    private static volatile boolean isInitialized = false;
    private static volatile boolean vpnActive = false;
    private static volatile int lastNetworkTransport = 0;
    private static Context sContext;
    private static final ProxyAutoDetector sDelegate = new ProxyAutoDetector();
    private static volatile long lastConnectionCheckTime = 0;

    public static void init(Context context) {
        if (isInitialized) return;
        isInitialized = true;
        sContext = context.getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            registerNetworkCallback(sContext);
        }

        AndroidUtilities.runOnUIThread(() -> {
            for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
                NotificationCenter.getInstance(i).addObserver(sDelegate, NotificationCenter.didUpdateConnectionState);
            }
        });

        // Initial check on app startup
        new Thread(() -> {
            checkAndAutoEnable(sContext);
        }, "proxy-auto-detect-init").start();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didUpdateConnectionState) {
            int state = ConnectionsManager.getInstance(account).getConnectionState();
            if (state == ConnectionsManager.ConnectionStateConnecting || state == ConnectionsManager.ConnectionStateWaitingForNetwork) {
                long now = System.currentTimeMillis();
                if (now - lastConnectionCheckTime < 10000) {
                    return;
                }
                lastConnectionCheckTime = now;
                if (sContext != null && !isVpnActive() && !TgWsProxyService.isRunning) {
                    new Thread(() -> {
                        if (isVpnActive()) return;
                        SharedPreferences prefs = sContext.getSharedPreferences("tgwsproxy", Context.MODE_PRIVATE);
                        if (prefs.getBoolean("enabled", false) || TgWsProxyService.isRunning) return;
                        if (!prefs.getBoolean("auto_detect", true)) return;
                        String secret = getOrCreateSecret(prefs);

                        if (tcpPing(TG_HOST, TG_PORT, TG_TIMEOUT)) {
                            return; // TG reachable
                        }
                        if (tcpPing(GOOGLE_HOST, GOOGLE_PORT, GOOGLE_TIMEOUT) || tcpPing("google.com", 443, GOOGLE_TIMEOUT)) {
                            Log.i(TAG, "TG connecting/waiting and Google reachable -> auto-enabling bypass");
                            prefs.edit().putBoolean("enabled", true).apply();
                            startProxyService(sContext, prefs);
                        }
                    }, "proxy-auto-detect-connection").start();
                }
            }
        }
    }

    private static void registerNetworkCallback(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(@NonNull Network n)  { evaluateNetwork(context); }
            @Override public void onLost(@NonNull Network n)       { evaluateNetwork(context); }
            @Override public void onCapabilitiesChanged(@NonNull Network n, @NonNull NetworkCapabilities c) {
                evaluateNetwork(context);
            }
        };

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            } else {
                connectivityManager.registerNetworkCallback(request, networkCallback);
            }
        } catch (Exception e) {
            Log.w(TAG, "registerNetworkCallback failed", e);
        }
    }

    private static void evaluateNetwork(Context context) {
        if (connectivityManager == null) return;

        boolean hasVpn  = isVpnActive();
        boolean hasWifi = false;
        boolean hasCell = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network active = connectivityManager.getActiveNetwork();
            if (active != null) {
                NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(active);
                if (caps != null) {
                    hasWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                    hasCell = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
                }
            }
        } else {
            Network[] networks = connectivityManager.getAllNetworks();
            for (Network network : networks) {
                NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
                if (caps != null
                        && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    hasWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                    hasCell = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
                    break;
                }
            }
        }

        SharedPreferences prefs = context.getSharedPreferences("tgwsproxy", Context.MODE_PRIVATE);

        if (hasVpn) {
            vpnActive = true;
            boolean suspendOnVpn = prefs.getBoolean("suspend_on_vpn", false);
            if (suspendOnVpn && (prefs.getBoolean("enabled", false) || TgWsProxyService.isRunning)) {
                Log.i(TAG, "VPN detected and suspend_on_vpn enabled, disabling proxy");
                prefs.edit().putBoolean("enabled", false).commit();

                Intent stopIntent = new Intent(context, TgWsProxyService.class);
                stopIntent.setAction(TgWsProxyService.ACTION_STOP);
                context.startService(stopIntent);

                disableTelegramProxy();
                sendStateChangedBroadcast(context);
            }
        } else {
            vpnActive = false;
        }

        // Just a normal network switch (e.g., Wi-Fi <-> Cellular <-> VPN)
        int currentTransport = hasVpn ? 3 : (hasWifi ? 1 : (hasCell ? 2 : 0));
        if (currentTransport != 0 && lastNetworkTransport != 0 && currentTransport != lastNetworkTransport) {
            Log.i(TAG, "Network changed → auto detecting or resetting proxy");
            lastNetworkTransport = currentTransport;

            new Thread(() -> {
                if (!hasVpn) {
                    if (TgWsProxyService.isRunning) {
                        NativeProxy.resetForNetworkChange();
                        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                        // Fast restart via intent
                        startProxyService(context, prefs);
                    } else {
                        checkAndAutoEnable(context);
                    }
                }
            }, "proxy-net-change").start();
        } else if (currentTransport != 0) {
            lastNetworkTransport = currentTransport;
        }
    }

    private static boolean isVpnActive() {
        if (connectivityManager == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network active = connectivityManager.getActiveNetwork();
            if (active == null) return false;
            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(active);
            return caps != null && !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (Network network : connectivityManager.getAllNetworks()) {
                NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
                if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void checkAndAutoEnable(Context ctx) {
        if (vpnActive) {
            Log.i(TAG, "VPN active, skipping proxy auto-enable");
            return;
        }
        SharedPreferences prefs = ctx.getSharedPreferences("tgwsproxy", Context.MODE_PRIVATE);

        if (prefs.getBoolean("enabled", false) || TgWsProxyService.isRunning) {
            return; // Already enabled manually
        }
        if (!prefs.getBoolean("auto_detect", true)) {
            return;
        }
        String secret = getOrCreateSecret(prefs);

        Log.i(TAG, "Pinging Telegram...");
        if (tcpPing(TG_HOST, TG_PORT, TG_TIMEOUT)) {
            Log.i(TAG, "Telegram reachable — proxy not needed");
            return;
        }

        Log.i(TAG, "Telegram unreachable — pinging Google...");
        if (!tcpPing(GOOGLE_HOST, GOOGLE_PORT, GOOGLE_TIMEOUT) && !tcpPing("google.com", 443, GOOGLE_TIMEOUT)) {
            Log.i(TAG, "No internet — skip");
            return;
        }

        Log.i(TAG, "Telegram blocked, internet OK — enabling proxy");
        prefs.edit().putBoolean("enabled", true).apply();
        startProxyService(ctx, prefs);
    }

    public static String getOrCreateSecret(SharedPreferences prefs) {
        String secret = prefs.getString("secret", "");
        if (secret == null || secret.isEmpty()) {
            byte[] b = new byte[16];
            new java.security.SecureRandom().nextBytes(b);
            StringBuilder sb = new StringBuilder();
            for (byte v : b) sb.append(String.format("%02x", v));
            secret = sb.toString();
            prefs.edit().putString("secret", secret).apply();
        }
        return secret;
    }

    public static void startProxyService(Context ctx, SharedPreferences prefs) {
        int port = prefs.getInt("port", 1443);
        String secret = getOrCreateSecret(prefs);
        
        Intent intent = new Intent(ctx, TgWsProxyService.class);
        intent.setAction(TgWsProxyService.ACTION_START);
        intent.putExtra(TgWsProxyService.EXTRA_PORT,        port);
        intent.putExtra(TgWsProxyService.EXTRA_SECRET,      secret);
        intent.putExtra(TgWsProxyService.EXTRA_POOL_SIZE,   prefs.getInt("pool_size",       4));
        intent.putExtra(TgWsProxyService.EXTRA_RECV_BUF,    prefs.getInt("buffer",         256));
        intent.putExtra(TgWsProxyService.EXTRA_SEND_BUF,    prefs.getInt("buffer",         256));
        intent.putExtra(TgWsProxyService.EXTRA_CF_ENABLED,  prefs.getBoolean("cf_enabled", true));
        intent.putExtra(TgWsProxyService.EXTRA_CF_DOMAIN,   prefs.getString("cf_domain",   ""));
        intent.putExtra(TgWsProxyService.EXTRA_FAKE_TLS,    prefs.getBoolean("fake_tls",   false));
        intent.putExtra(TgWsProxyService.EXTRA_FAKE_DOMAIN, prefs.getString("fake_domain", "google.com"));
        intent.putExtra(TgWsProxyService.EXTRA_VERBOSE,     0);
        intent.putExtra(TgWsProxyService.EXTRA_DC_IPS,      prefs.getString("dc_ips",      ""));

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent);
            } else {
                ctx.startService(intent);
            }
        } catch (Throwable e) {
            Log.w(TAG, "Failed to start TgWsProxyService foreground service: " + e.getMessage());
            try {
                ctx.startService(intent);
            } catch (Throwable ignored) {}
        }

        waitForPortAndApply(ctx, port, secret);
    }

    private static void waitForPortAndApply(Context ctx, int port, String secret) {
        long deadline = System.currentTimeMillis() + PORT_WAIT_TIMEOUT_MS;
        boolean opened = false;

        while (System.currentTimeMillis() < deadline) {
            try { Thread.sleep(PORT_WAIT_INTERVAL_MS); } catch (InterruptedException e) { return; }
            if (tcpPing("127.0.0.1", port, 800)) {
                opened = true;
                break;
            }
        }

        if (!opened) {
            Log.w(TAG, "Proxy port didn't open in time — skipping applyTelegramProxySettings");
            return;
        }

        String proxySecret = NativeProxy.getSecretWithPrefix();
        if (proxySecret == null || proxySecret.isEmpty()) {
            proxySecret = "dd" + secret;
        }
        final String finalSecret = proxySecret;
        final int finalPort = port;

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                org.telegram.messenger.SharedConfig.ProxyInfo existing = null;
                for (org.telegram.messenger.SharedConfig.ProxyInfo info : org.telegram.messenger.SharedConfig.proxyList) {
                    if ("127.0.0.1".equals(info.address) && info.port == finalPort) {
                        existing = info;
                        break;
                    }
                }

                org.telegram.messenger.SharedConfig.ProxyInfo proxyInfo;
                if (existing != null) {
                    existing.secret = finalSecret;
                    proxyInfo = existing;
                } else {
                    proxyInfo = new org.telegram.messenger.SharedConfig.ProxyInfo("127.0.0.1", finalPort, "", "", finalSecret);
                    org.telegram.messenger.SharedConfig.addProxy(proxyInfo);
                }

                org.telegram.messenger.SharedConfig.currentProxy = proxyInfo;
                org.telegram.messenger.SharedConfig.saveProxyList();

                org.telegram.messenger.MessagesController.getGlobalMainSettings()
                        .edit()
                        .putBoolean("proxy_enabled", true)
                        .putString("proxy_ip",     "127.0.0.1")
                        .putString("proxy_user",   "")
                        .putString("proxy_pass",   "")
                        .putInt("proxy_port",       finalPort)
                        .putString("proxy_secret",  finalSecret)
                        .commit();

                org.telegram.tgnet.ConnectionsManager.setProxySettings(true, "127.0.0.1", finalPort, "", "", finalSecret);
                org.telegram.messenger.NotificationCenter.getGlobalInstance().postNotificationName(org.telegram.messenger.NotificationCenter.proxySettingsChanged);

            } catch (Exception e) {
                Log.e(TAG, "Failed to apply Telegram proxy settings", e);
            }
        });
    }

    private static void disableTelegramProxy() {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                org.telegram.messenger.MessagesController.getGlobalMainSettings()
                        .edit().putBoolean("proxy_enabled", false).commit();
                org.telegram.tgnet.ConnectionsManager.setProxySettings(
                        false, "", 0, "", "", "");
                org.telegram.messenger.NotificationCenter.getGlobalInstance()
                        .postNotificationName(
                                org.telegram.messenger.NotificationCenter.proxySettingsChanged);
            } catch (Exception e) {
                Log.e(TAG, "Failed to disable Telegram proxy settings", e);
            }
        });
    }

    private static void sendStateChangedBroadcast(Context context) {
        Intent intent = new Intent(TgWsProxyService.ACTION_STATE_CHANGED);
        intent.setPackage(context.getPackageName());
        intent.putExtra("running", TgWsProxyService.isRunning);
        intent.putExtra("port", TgWsProxyService.currentPort);
        context.sendBroadcast(intent);
    }

    public static boolean tcpPing(String host, int port, int timeoutMs) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
