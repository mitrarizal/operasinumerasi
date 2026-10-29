# ==============================================================================
# JAVASCRIPT INTERFACE (BRIDGE WEBVIEW & NATIVE)
# ==============================================================================
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# ==============================================================================
# MENJAGA CLASS APPLICATION & PACKAGE UTAMA (MENCEGAH ClassNotFoundException)
# ==============================================================================
-keep class com.bilangannumerasiapp.MyApplication { *; }
-keep class com.bilangannumerasiapp.** { *; }
-keep class com.ujiannet.id.** { *; }

# ==============================================================================
# MENJAGA GOOGLE MOBILE ADS (ADMOB)
# ==============================================================================
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# ==============================================================================
# MENJAGA PROTEKSI LISENSI & PLAY INTEGRITY GOOGLE PLAY (PAIRIP)
# ==============================================================================
-keep class com.pairip.** { *; }
-keepclassmembers class com.pairip.** { *; }
-dontwarn com.pairip.**
