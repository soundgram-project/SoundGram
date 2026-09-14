package soundgram.music;

import android.text.TextUtils;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.SharedConfig;

import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public final class MusicNetworkHelper {

    private static final String TAG = "MusicNetworkHelper";
    public static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private static volatile boolean sInitialized = false;
    private static javax.net.ssl.SSLSocketFactory sTrustAllSslSocketFactory = null;

    static {
        init();
    }

    public static synchronized void init() {
        if (sInitialized) return;
        sInitialized = true;

        try {
            // Setup global authenticator for proxy credentials if configured
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    try {
                        if (SharedConfig.isProxyEnabled() && SharedConfig.currentProxy != null) {
                            SharedConfig.ProxyInfo pi = SharedConfig.currentProxy;
                            if (!TextUtils.isEmpty(pi.username) && !TextUtils.isEmpty(pi.address)) {
                                String host = getRequestingHost();
                                if (host == null || host.equalsIgnoreCase(pi.address) || getRequestorType() == RequestorType.PROXY) {
                                    return new PasswordAuthentication(pi.username,
                                            (pi.password != null ? pi.password : "").toCharArray());
                                }
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e(TAG + " proxy auth error: " + e);
                    }
                    return super.getPasswordAuthentication();
                }
            });
        } catch (Exception e) {
            FileLog.e(TAG + " init authenticator failed: " + e);
        }

        try {
            // Setup trust-all SSL context for CDN mirrors
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
            }}, new SecureRandom());
            sTrustAllSslSocketFactory = sc.getSocketFactory();
        } catch (Exception e) {
            FileLog.e(TAG + " init ssl failed: " + e);
        }

        installProxySelector();
    }

    /**
     * Installs a custom ProxySelector so that ExoPlayer and all default Java HttpURLConnection
     * calls respect Telegram's proxy when enabled, or use direct connection when disabled.
     */
    public static void installProxySelector() {
        try {
            ProxySelector.setDefault(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    if (uri != null) {
                        String host = uri.getHost();
                        if ("127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host)) {
                            return Collections.singletonList(Proxy.NO_PROXY);
                        }
                    }
                    Proxy active = getActiveProxy();
                    return Collections.singletonList(active);
                }

                @Override
                public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                    FileLog.w(TAG + " connect failed to " + uri + " via " + sa + ": " + ioe);
                }
            });
        } catch (Exception e) {
            FileLog.e(TAG + " installProxySelector failed: " + e);
        }
    }

    /**
     * Returns the active Proxy configured in Telegram or the local TgWsProxy bypass
     * if enabled, or Proxy.NO_PROXY if disabled.
     */
    public static Proxy getActiveProxy() {
        try {
            if (SharedConfig.isProxyEnabled() && SharedConfig.currentProxy != null) {
                SharedConfig.ProxyInfo pi = SharedConfig.currentProxy;
                if (!TextUtils.isEmpty(pi.address) && pi.port > 0) {
                    if ("127.0.0.1".equals(pi.address) || "localhost".equalsIgnoreCase(pi.address)) {
                        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(pi.address, pi.port));
                    }
                    if (TextUtils.isEmpty(pi.secret)) {
                        return new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(pi.address, pi.port));
                    }
                }
            }
            if (soundgram.wsproxy.TgWsProxyService.isRunning && soundgram.wsproxy.TgWsProxyService.currentPort > 0) {
                return new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", soundgram.wsproxy.TgWsProxyService.currentPort));
            }
            if (org.telegram.messenger.ApplicationLoader.applicationContext != null) {
                android.content.SharedPreferences prefs = org.telegram.messenger.ApplicationLoader.applicationContext
                        .getSharedPreferences("tgwsproxy", android.content.Context.MODE_PRIVATE);
                if (prefs.getBoolean("enabled", false)) {
                    int port = prefs.getInt("port", 1443);
                    if (port > 0) {
                        if (!soundgram.wsproxy.TgWsProxyService.isRunning) {
                            soundgram.wsproxy.ProxyAutoDetector.startProxyService(org.telegram.messenger.ApplicationLoader.applicationContext, prefs);
                        }
                        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", port));
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(TAG + " getActiveProxy error: " + e);
        }
        return Proxy.NO_PROXY;
    }

    /**
     * Checks if proxy is currently enabled and active.
     */
    public static boolean isProxyActive() {
        return getActiveProxy() != Proxy.NO_PROXY;
    }

    /**
     * Opens a HttpURLConnection strictly using the active proxy setting (or direct if disabled).
     */
    public static HttpURLConnection openConnection(URL url) throws IOException {
        Proxy proxy = getActiveProxy();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);

        if (conn instanceof HttpsURLConnection && sTrustAllSslSocketFactory != null) {
            try {
                ((HttpsURLConnection) conn).setSSLSocketFactory(sTrustAllSslSocketFactory);
                ((HttpsURLConnection) conn).setHostnameVerifier((hostname, session) -> true);
            } catch (Exception ignored) {}
        }

        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        return conn;
    }

    /**
     * Opens a HttpURLConnection for a given URL string.
     */
    public static HttpURLConnection openConnection(String urlStr) throws IOException {
        return openConnection(new URL(urlStr));
    }
}
