# Keep source file and line numbers for readable stack traces in Play Console (with mapping.txt)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Parcelables
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep data models
-keep class com.VTDStadio.Hotroin.PrintHistoryItem { *; }
-keep class com.VTDStadio.Hotroin.SupportPushItem { *; }

# Firebase & Google Play Services
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.mlkit.**

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Guava / ListenableFuture
-dontwarn com.google.common.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.lang.model.element.Modifier

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
