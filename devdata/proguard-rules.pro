# devdata — KYC Test Toolkit
# ProGuard / R8 rules for release builds

# ── Kotlin ─────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }

# ── Kotlin Coroutines ───────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ── kotlinx.serialization ───────────────────────────────────────────────────
# Keep serializer companion objects and all @Serializable classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.** { *; }

-keep,includedescriptorclasses class com.vadimtoptunov.**$$serializer { *; }
-keepclassmembers class com.vadimtoptunov.** {
    *** Companion;
}
-keepclasseswithmembers class com.vadimtoptunov.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Room ────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.**

# ── Jetpack Compose ─────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Android Keystore / security ─────────────────────────────────────────────
-keep class java.security.** { *; }
-keep class javax.security.** { *; }
-dontwarn java.security.**

# ── HCE / NFC ────────────────────────────────────────────────────────────────
-keep class com.vadimtoptunov.devdata.hce.** { *; }

# ── General Android ──────────────────────────────────────────────────────────
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
