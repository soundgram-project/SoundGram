#!/bin/bash

# === 1. Укажите путь к вашему NDK ===
export ANDROID_NDK_HOME="$HOME/Android/Sdk/ndk/27.2.12479018"

# === 2. Правильный путь к toolchain ===
export TOOLCHAIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin"

if [ ! -d "$TOOLCHAIN" ]; then
    echo "❌ Папка toolchain не найдена: $TOOLCHAIN"
    echo "Ищем альтернативы..."
    find "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt" -maxdepth 2 -name "*-clang" 2>/dev/null | head -5
    exit 1
fi

echo "✅ Toolchain: $TOOLCHAIN"

# === 3. Проверим наличие компиляторов ===
for compiler in aarch64-linux-android21-clang armv7a-linux-androideabi21-clang x86_64-linux-android21-clang i686-linux-android21-clang; do
    if [ -f "$TOOLCHAIN/$compiler" ]; then
        echo "  ✅ $compiler"
    else
        echo "  ❌ $compiler не найден"
    fi
done

# === 4. Путь к выходным .so ===
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR" || exit 1

CURRENT_DIR="$SCRIPT_DIR"
while [ "$CURRENT_DIR" != "/" ]; do
    if [ "$(basename "$CURRENT_DIR")" = "TMessagesProj" ]; then
        TM_PROJ_DIR="$CURRENT_DIR"
        break
    elif [ -d "$CURRENT_DIR/TMessagesProj" ]; then
        TM_PROJ_DIR="$CURRENT_DIR/TMessagesProj"
        break
    fi
    CURRENT_DIR="$(dirname "$CURRENT_DIR")"
done

if [ -z "$TM_PROJ_DIR" ]; then
    echo "❌ TMessagesProj directory not found!"
    exit 1
fi

OUT_BASE="$TM_PROJ_DIR/jni"
JNI_LIBS="$TM_PROJ_DIR/src/main/jniLibs"
mkdir -p "$OUT_BASE/arm64-v8a"
mkdir -p "$OUT_BASE/armeabi-v7a"
mkdir -p "$JNI_LIBS/arm64-v8a"
mkdir -p "$JNI_LIBS/armeabi-v7a"

export CGO_ENABLED=1
export GOOS=android

# === 5. ARM64 ===
echo ""
echo "🔨 Сборка arm64-v8a..."
export CC="$TOOLCHAIN/aarch64-linux-android21-clang"
export CXX="$TOOLCHAIN/aarch64-linux-android21-clang++"
export GOARCH=arm64
go build -trimpath -ldflags="-s -w -extldflags '-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384'" -buildmode=c-shared -o "$OUT_BASE/arm64-v8a/libtgwsproxy.so" tg-ws-proxy.go

# === 6. ARMv7 ===
echo "🔨 Сборка armeabi-v7a..."
export CC="$TOOLCHAIN/armv7a-linux-androideabi21-clang"
export CXX="$TOOLCHAIN/armv7a-linux-androideabi21-clang++"
export GOARCH=arm
export GOARM=7
go build -trimpath -ldflags="-s -w -extldflags '-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384'" -buildmode=c-shared -o "$OUT_BASE/armeabi-v7a/libtgwsproxy.so" tg-ws-proxy.go

echo ""
echo "✅ Готово!"
cp -f "$OUT_BASE/arm64-v8a/libtgwsproxy.so" "$JNI_LIBS/arm64-v8a/"
cp -f "$OUT_BASE/armeabi-v7a/libtgwsproxy.so" "$JNI_LIBS/armeabi-v7a/"
ls -lh "$OUT_BASE"/*/
ls -lh "$JNI_LIBS"/*/
