# Proguard rules for GamerX AI

# Keep Gemini API classes
-keep class com.google.ai.client.generativeai.** { *; }

# Keep Room entities
-keep class com.gamerx.ai.data.db.entities.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Compose
-dontwarn androidx.compose.**

# General
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
