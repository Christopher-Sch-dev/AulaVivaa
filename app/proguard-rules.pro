# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ============================================
# RETROFIT & OKHTTP
# ============================================
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# ============================================
# ROOM DATABASE
# ============================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# ============================================
# JETPACK COMPOSE
# ============================================
-keep class androidx.compose.** { *; }
-keep class kotlin.coroutines.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ============================================
# KOTLIN SERIALIZATION
# ============================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class cl.duocuc.aulaviva.data.remote.dto.**$$serializer { *; }
-keepclassmembers class cl.duocuc.aulaviva.data.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class cl.duocuc.aulaviva.data.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================
# GSON (Para Retrofit)
# ============================================
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class cl.duocuc.aulaviva.data.remote.dto.** { *; }
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# ============================================
# SUPABASE & JWT
# ============================================
-keep class io.github.jan.supabase.** { *; }
-keep class cl.duocuc.aulaviva.data.remote.** { *; }
-dontwarn io.github.jan.supabase.**

# ============================================
# GEMINI AI
# ============================================
-keep class com.google.ai.client.generativeai.** { *; }
-keep class cl.duocuc.aulaviva.data.remote.Gemini** { *; }

# ============================================
# PRESERVAR PARA DEBUGGING
# ============================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================
# NUESTRAS CLASES DE DOMINIO
# ============================================
-keep class cl.duocuc.aulaviva.data.model.** { *; }
-keep class cl.duocuc.aulaviva.domain.** { *; }
