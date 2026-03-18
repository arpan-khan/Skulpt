# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Room entities
-keep class com.skulpt.app.data.model.** { *; }

# Keep Gson models
-keep class com.skulpt.app.util.ExportData { *; }
-keep class com.skulpt.app.util.ExportDay { *; }
-keep class com.skulpt.app.util.ExportExercise { *; }
-keepattributes Signature
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Keep Navigation Safe Args
-keep class com.skulpt.app.ui.** { *; }

# Keep Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# General
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-dontwarn org.slf4j.**
