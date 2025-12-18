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

# OPTIMIZACIÓN: Inline functions
-allowaccessmodification
-mergeinterfacesaggressively
