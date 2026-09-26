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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# Keep Firestore data models and app classes to prevent crash on release build
-keep class com.huyarev.cinevia.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil
-dontwarn coil.**

# AWS S3 / Cloudflare
-keep class com.amazonaws.** { *; }
-dontwarn com.amazonaws.**

# Keep annotations and signatures for Gson/Retrofit
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses, EnclosingMethod

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Keep Kotlin metadata for Retrofit suspend functions
-keep class kotlin.Metadata { *; }

# Explicitly keep Retrofit interface
-keep interface com.huyarev.cinevia.network.TmdbApi { *; }

