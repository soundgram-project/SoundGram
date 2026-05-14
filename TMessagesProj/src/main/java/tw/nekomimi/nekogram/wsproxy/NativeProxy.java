package tw.nekomimi.nekogram.wsproxy;

import android.util.Log;

public class NativeProxy {

    static {
        try {
            // Load Go .so first (contains StartProxy, SetPoolSize, etc.)
            System.loadLibrary("tgwsproxy");
            // Then JNI bridge (uses dlopen to find symbols from Go .so)
            System.loadLibrary("tgwsproxy_jni");
            Log.i("NativeProxy", "All libraries loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e("NativeProxy", "Failed to load libraries", e);
        }
    }

    public static native int StartProxy(String host, int port, String dcIps, String secret, int verbose);
    public static native int StopProxy();
    public static native void SetPoolSize(int size);
    public static native void SetRecvBuffer(int size);
    public static native void SetSendBuffer(int size);
    public static native void SetCfProxyCacheDir(String cacheDir);
    public static native void SetCfProxyConfig(int enabled, int priority, String userDomain);
    public static native void SetFakeTls(int enabled, String domain);
    public static native String GetSecretWithPrefix();
    public static native String GetStats();
    public static native void FreeString(String p);

    public static int startProxy(String host, int port, String dcIps, String secret, int verbose) {
        return StartProxy(host, port, dcIps, secret, verbose);
    }

    public static int stopProxy() {
        return StopProxy();
    }

    public static void setPoolSize(int size) {
        SetPoolSize(size);
    }

    public static void setRecvBuffer(int size) {
        SetRecvBuffer(size);
    }

    public static void setSendBuffer(int size) {
        SetSendBuffer(size);
    }

    public static void setCfProxyCacheDir(String cacheDir) {
        SetCfProxyCacheDir(cacheDir);
    }

    public static void setCfProxyConfig(boolean enabled, boolean priority, String userDomain) {
        SetCfProxyConfig(enabled ? 1 : 0, priority ? 1 : 0, userDomain != null ? userDomain : "");
    }

    public static void setFakeTls(boolean enabled, String domain) {
        SetFakeTls(enabled ? 1 : 0, domain != null ? domain : "");
    }

    public static String getSecretWithPrefix() {
        return GetSecretWithPrefix();
    }

    public static String getStats() {
        return GetStats();
    }
}
