# Menjaga seluruh package proyek agar tidak terhapus oleh R8
-keep class com.bilangannumerasiapp.** { *; }
-keep class com.ujiannet.id.** { *; }
-keep class com.game.sos.** { *; }

# Menjaga JavaScript Interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# Menjaga AdMob & Play Integrity
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
-keep class com.pairip.** { *; }
-keepclassmembers class com.pairip.** { *; }
-dontwarn com.pairip.**
