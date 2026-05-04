# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
#
# Repackage and obfuscation dictionaries.
-repackageclasses 'x'
-classobfuscationdictionary proguard/obfuscation-dict.txt
-obfuscationdictionary proguard/obfuscation-dict.txt
-packageobfuscationdictionary proguard/obfuscation-dict.txt

# Hide source file metadata.
-renamesourcefileattribute SourceFile

# Keep WebView JavaScript bridges stable in release. The JavaScript side calls
# annotated method names directly, so R8 may shrink internals but must not rename
# the exposed bridge methods.
-keepclassmembers class ** {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep native registration and JNI data carriers stable. The native library uses
# RegisterNatives plus fixed JVM names/field names for these internal bridge
# classes; obfuscating them can silently disable native paths in release.
-keep class com.example.coblaxexamlock.nativebridge.NativeBridgeBindings { *; }
-keep class com.example.coblaxexamlock.ClipboardNormalizedItemInput { *; }
-keep class com.example.coblaxexamlock.NativeClipboardSnapshotCore { *; }

# Manifest entry points are rewritten by R8; avoid keeping activity internals readable.

# Strip logs (avoid matching java.lang.Object methods).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static boolean isLoggable(...);
}

# Jetpack Security (EncryptedSharedPreferences).
-keep class androidx.security.crypto.** { *; }

# R8 missing annotation classes (safe to ignore at runtime).
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn javax.annotation.concurrent.GuardedBy
-dontwarn javax.annotation.Nullable

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
