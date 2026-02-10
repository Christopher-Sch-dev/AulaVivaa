# OPTIMIZACIONES AGRESIVAS (R8)
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# KEEP Compose (crítico)
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }

# KEEP Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# KEEP Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# KEEP Supabase
-keep class io.github.jan.supabase.** { *; }

# KEEP Gemini AI
-keep class com.google.ai.** { *; }

# OPTIMIZACIÓN: Elimina logs en release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# KEEP Markwon (Markdown)
-dontwarn io.noties.markwon.**
-dontwarn com.caverock.androidsvg.**
-dontwarn pl.droidsonroids.gif.**

# KEEP PDFBox
-dontwarn com.tom_roush.pdfbox.**
-dontwarn com.gemalto.jp2.**
-dontwarn org.apache.commons.logging.**

# OPTIMIZACIÓN: Inline functions
-allowaccessmodification
-mergeinterfacesaggressively

# KEEP DTOs para Gson (CRÍTICO para login/register y Gemini API)
-keep class cl.duocuc.aulaviva.data.remote.** { *; }
-keepclassmembers class cl.duocuc.aulaviva.data.remote.** { *; }

# KEEP Gson serialization
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

