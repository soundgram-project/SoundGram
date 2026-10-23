-keep class androidx.sharetarget.** { *; }

-dontwarn com.google.android.gms.internal.vision.**
-dontwarn com.google.android.gms.vision.**
-dontwarn com.google.android.gms.common.**

-keep class com.google.android.gms.vision.face.** { *; }
-keep class com.google.android.gms.vision.clearcut.** { *; }
-keep class com.google.android.gms.internal.vision.** { *; }

-keep class com.google.firebase.remoteconfig.** { *; }
-keep class com.google.android.gms.internal.config.** { *; }

-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * { @androidx.annotation.Keep *; }

-keep class org.webrtc.* { *; }
-keep class org.webrtc.audio.* { *; }
-keep class org.webrtc.voiceengine.* { *; }
-keep class org.telegram.messenger.* { *; }
-keep class org.telegram.messenger.camera.* { *; }
-keep class org.telegram.messenger.secretmedia.* { *; }
-keep class org.telegram.messenger.support.* { *; }
-keep class org.telegram.messenger.time.* { *; }
-keep class org.telegram.messenger.video.* { *; }
-keep class org.telegram.messenger.voip.* { *; }
-keep class org.telegram.SQLite.** { *; }
-keep class org.telegram.tgnet.ConnectionsManager { *; }
-keep class org.telegram.tgnet.NativeByteBuffer { *; }
-keep class org.telegram.tgnet.RequestTimeDelegate { *; }
-keep class org.telegram.tgnet.RequestDelegate { *; }
-keep class org.telegram.tgnet.** { *; }
# Plugin compatibility: Python plugins (Chaquopy) import and Pine-hook ARBITRARY org.telegram classes by
# their ORIGINAL names (e.g. `from org.telegram.tgnet.tl import TL_account`, hook `ChatActivityEnterView`).
# Keep the whole tree un-shrunk and un-renamed so practically every plugin's imports/hooks resolve.
-keep class org.telegram.** { *; }
-keep class org.telegram.ui.Stories.recorder.FfmpegAudioWaveformLoader { *; }
-keep class androidx.mediarouter.app.MediaRouteButton { *; }
-keepclassmembers class ** {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class org.telegram.utils.code.highlight.PrismaHighlighter {
    private static native <methods>;
}

# https://developers.google.com/ml-kit/known-issues#android_issues
-keep class com.google.mlkit.nl.languageid.internal.LanguageIdentificationJni { *; }
-keep class soundgram.wsproxy.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Huawei Services
-keep class com.huawei.hianalytics.**{ *; }
-keep class com.huawei.updatesdk.**{ *; }
-keep class com.huawei.hms.**{ *; }

# Don't warn about checkerframework and Kotlin annotations
-dontwarn org.checkerframework.**
-dontwarn javax.annotation.**

-keep class io.nano.tex.** {*;}

-keep class org.telegram.tgnet.** { *; }

# JLatexMath: macro/atom classes are loaded reflectively by Class.forName
-keep class org.scilab.forge.jlatexmath.** { *; }
-keep class ru.noties.jlatexmath.** { *; }
-dontwarn org.scilab.forge.jlatexmath.**

# Plugin reflection robustness: Chaquopy/Pine resolve nested classes (TLRPC$TL_message), read generic
# signatures and annotations at runtime. Preserve the attributes reflection needs so any plugin importing a
# nested TL type / reading annotations resolves correctly.
-keepattributes Signature,InnerClasses,EnclosingMethod,Exceptions,*Annotation*,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# NG: StringConcatFactory absent on Android <26; Kotlin 1.9 + Java 17 target
# emits invokeDynamic for string templates. Desugaring covers runtime; -dontwarn
# silences R8 missing-class check.
-dontwarn java.lang.invoke.StringConcatFactory

# NG: Jackson 2.x's Java7SupportImpl references java.beans.* classes that the
# Android runtime does not ship. The class is reflectively loaded behind a
# Java-version probe, so the references are dead on Android; silence R8.
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient

# Room Database & DAOs
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.Entity { *; }
-keep @androidx.room.Entity class * { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# AyuGram, NekoGram, NimarkoGram, SoundGram
-keep class com.radolyn.ayugram.** { *; }
-keep interface com.radolyn.ayugram.** { *; }
-keep class tw.nekomimi.nekogram.** { *; }
-keep interface tw.nekomimi.nekogram.** { *; }
-keep class app.nimarkogram.** { *; }
-keep interface app.nimarkogram.** { *; }
-keep class soundgram.** { *; }
-keep interface soundgram.** { *; }

# Use -keep to explicitly keep any other classes shrinking would remove
#-dontoptimize
#-dontobfuscate


