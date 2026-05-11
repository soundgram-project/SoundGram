#include <jni.h>
#include <dlfcn.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>

#define LOG_TAG "TgWsProxyJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static void* go_handle = NULL;

// Типы Go-функций
typedef int  (*fn_StartProxy)(const char*, int, const char*, const char*, int);
typedef int  (*fn_StopProxy)(void);
typedef void (*fn_SetPoolSize)(int);
typedef void (*fn_SetCfProxyCacheDir)(const char*);
typedef void (*fn_SetCfProxyConfig)(int, int, const char*);
typedef void (*fn_SetFakeTls)(int, const char*);
typedef void (*fn_SetSpoofSni)(int, const char*);
typedef char*(*fn_GetSecretWithPrefix)(void);
typedef char*(*fn_GetStats)(void);
typedef void (*fn_FreeString)(char*);

// Указатели на Go-функции
static fn_StartProxy         pStartProxy = NULL;
static fn_StopProxy          pStopProxy = NULL;
static fn_SetPoolSize        pSetPoolSize = NULL;
static fn_SetCfProxyCacheDir pSetCfProxyCacheDir = NULL;
static fn_SetCfProxyConfig   pSetCfProxyConfig = NULL;
static fn_SetFakeTls         pSetFakeTls = NULL;
static fn_SetSpoofSni        pSetSpoofSni = NULL;
static fn_GetSecretWithPrefix pGetSecretWithPrefix = NULL;
static fn_GetStats           pGetStats = NULL;
static fn_FreeString         pFreeString = NULL;

__attribute__((constructor))
void init_bridge() {
    // Загружаем Go .so (уже загружен через System.loadLibrary, но dlopen нужен для dlsym)
    go_handle = dlopen("libtgwsproxy.so", RTLD_NOW | RTLD_GLOBAL);
    if (!go_handle) {
        LOGE("dlopen libtgwsproxy.so failed: %s", dlerror());
        return;
    }

    pStartProxy         = (fn_StartProxy)        dlsym(go_handle, "StartProxy");
    pStopProxy          = (fn_StopProxy)         dlsym(go_handle, "StopProxy");
    pSetPoolSize        = (fn_SetPoolSize)       dlsym(go_handle, "SetPoolSize");
    pSetCfProxyCacheDir = (fn_SetCfProxyCacheDir)dlsym(go_handle, "SetCfProxyCacheDir");
    pSetCfProxyConfig   = (fn_SetCfProxyConfig)  dlsym(go_handle, "SetCfProxyConfig");
    pSetFakeTls         = (fn_SetFakeTls)        dlsym(go_handle, "SetFakeTls");
    pSetSpoofSni        = (fn_SetSpoofSni)       dlsym(go_handle, "SetSpoofSni");
    pGetSecretWithPrefix= (fn_GetSecretWithPrefix)dlsym(go_handle, "GetSecretWithPrefix");
    pGetStats           = (fn_GetStats)          dlsym(go_handle, "GetStats");
    pFreeString         = (fn_FreeString)        dlsym(go_handle, "FreeString");

    if (!pStartProxy) LOGE("dlsym StartProxy failed: %s", dlerror());
}

// --- JNI реализации ---

JNIEXPORT jint JNICALL
Java_tw_nekomimi_nekogram_wsproxy_NativeProxy_StartProxy(
    JNIEnv* env, jclass cls, jstring host, jint port, jstring dcIps, jstring secret, jint verbose) {
    
    if (!pStartProxy) return -1;
    
    const char* cHost = (*env)->GetStringUTFChars(env, host, NULL);
    const char* cDcIps = dcIps ? (*env)->GetStringUTFChars(env, dcIps, NULL) : "";
    const char* cSecret = secret ? (*env)->GetStringUTFChars(env, secret, NULL) : "";
    
    int result = pStartProxy(cHost, port, cDcIps, cSecret, verbose);
    
    (*env)->ReleaseStringUTFChars(env, host, cHost);
    if (dcIps) (*env)->ReleaseStringUTFChars(env, dcIps, cDcIps);
    if (secret) (*env)->ReleaseStringUTFChars(env, secret, cSecret);
    
    return result;
}

JNIEXPORT jint JNICALL
Java_tw_nekomimi_nekogram_wsproxy_NativeProxy_StopProxy(JNIEnv* env, jclass cls) {
    if (!pStopProxy) return -1;
    return pStopProxy();
}

JNIEXPORT void JNICALL
Java_tw_nekomimi_nekogram_wsproxy_NativeProxy_SetPoolSize(JNIEnv* env, jclass cls, jint size) {
    if (pSetPoolSize) pSetPoolSize(size);
}

JNIEXPORT void JNICALL
Java_tw_nekomimi_nekogram_wsproxy_NativeProxy_SetCfProxyCacheDir(
    JNIEnv* env, jclass cls, jstring cacheDir) {
    
    if (!pSetCfProxyCacheDir) return;
    const char* cDir = cacheDir ? (*env)->GetStringUTFChars(env, cacheDir, NULL) : "";
    pSetCfProxyCacheDir(cDir);
    if (cacheDir) (*env)->ReleaseStringUTFChars(env, cacheDir, cDir);
}

JNIEXPORT void JNICALL
Java_tw_nekomimi_nekogram_wsproxy_NativeProxy_SetCfProxyConfig(
    JNIEnv* env, jclass cls, jint enabled, jint priority, jstring userDomain) {
    
    if (!pSetCfProxyConfig) return;
    const char* cDomain = userDomain ? (*env)->GetStringUTFChars(env, userDomain, NULL) : "";
    pSetCfProxyConfig(enabled, priority, cDomain);
    if (userDomain) (*env)->ReleaseStringUTFChars(env, userDomain, cDomain);
}

JNIEXPORT void JNICALL
Java_tw_nekomimi_nekogram_wsproxy_NativeProxy_SetFakeTls(
    JNIEnv* env, jclass cls, jint enabled, jstring domain) {
    
    if (!pSetFakeTls) return;
    const char* cDomain = domain ? (*env)->GetStringUTFChars(env, domain, NULL) : "";
    pSetFakeTls(enabled, cDomain);
    if (domain) (*env)->ReleaseStringUTFChars(env, domain, cDomain);
}

JNIEXPORT void JNICALL
Java_tw_nekomimi_nekogram_wsproxy_NativeProxy_SetSpoofSni(
    JNIEnv* env, jclass cls, jint enabled, jstring domain) {
    
    if (!pSetSpoofSni) return;
    const char* cDomain = domain ? (*env)->GetStringUTFChars(env, domain, NULL) : "";
    pSetSpoofSni(enabled, cDomain);
    if (domain) (*env)->ReleaseStringUTFChars(env, domain, cDomain);
}

JNIEXPORT jstring JNICALL
Java_tw_nekomimi_nekogram_wsproxy_NativeProxy_GetSecretWithPrefix(JNIEnv* env, jclass cls) {
    if (!pGetSecretWithPrefix) return NULL;
    char* result = pGetSecretWithPrefix();
    if (!result) return NULL;
    jstring jstr = (*env)->NewStringUTF(env, result);
    if (pFreeString) pFreeString(result);
    return jstr;
}

JNIEXPORT jstring JNICALL
Java_tw_nekomimi_nekogram_wsproxy_NativeProxy_GetStats(JNIEnv* env, jclass cls) {
    if (!pGetStats) return NULL;
    char* result = pGetStats();
    if (!result) return NULL;
    jstring jstr = (*env)->NewStringUTF(env, result);
    if (pFreeString) pFreeString(result);
    return jstr;
}

JNIEXPORT void JNICALL
Java_tw_nekomimi_nekogram_wsproxy_NativeProxy_FreeString(
    JNIEnv* env, jclass cls, jstring str) {
    // Не используется — освобождение в GetSecretWithPrefix/GetStats
}
