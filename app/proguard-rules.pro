# ─────────────────────────────────────────────────────────────────────────────
# Città driver app — R8 / ProGuard keep rules (Slice 6, WU 6b)
#
# Internal-only release build: minify + resource shrink on, no signing config, no
# Firebase mapping upload. These rules cover the reflective / generated-code paths
# that R8 cannot see through: Retrofit + OkHttp, Gson model (de)serialization,
# Hilt/Dagger components, coroutines internals, and Firebase/Crashlytics.
# ─────────────────────────────────────────────────────────────────────────────

# Keep line numbers / source file for readable release stack traces, then rename
# the source file attribute so the original file names are not leaked.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─── Retrofit / OkHttp / Okio ────────────────────────────────────────────────
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**

# Retrofit relies on generic signatures and method annotations at runtime.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Keep the Retrofit service interface and its annotations intact.
-keep interface com.citta.driver.data.api.CittaApi { *; }

# Retrofit 2.x: keep generic type information on Continuation for suspend funs.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ─── Gson ────────────────────────────────────────────────────────────────────
-keepattributes *Annotation*
-dontwarn com.google.gson.**
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# All wire DTOs and serialized domain models are populated by Gson reflection.
-keep class com.citta.driver.data.api.** { *; }
-keep class com.citta.driver.domain.** { *; }

# Enum (de)serialization (Gson + kotlinx): keep values()/valueOf().
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ─── Kotlin metadata / reflection ────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }

# ─── Kotlin coroutines ───────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ─── Hilt / Dagger ───────────────────────────────────────────────────────────
-dontwarn dagger.hilt.**
-dontwarn javax.inject.**
-keep,allowobfuscation @interface dagger.hilt.android.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# ─── Firebase / Crashlytics ──────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-keepattributes *Annotation*,Signature,Exceptions

# ─── AndroidX / WorkManager / Compose (defensive) ────────────────────────────
-dontwarn androidx.**
-keep class androidx.hilt.work.** { *; }
