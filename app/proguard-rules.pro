# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/moo/Development/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

-dontwarn org.joda.convert.FromString
-dontwarn org.joda.convert.ToString

# WorkManager Worker keep rules
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# App Startup initializers
-keep class * extends androidx.startup.Initializer {
    public <init>();
}

# Preserve annotations and signatures required for Jackson reflection and Room
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, RuntimeVisibleParameterAnnotations

# Jackson JSON deserialization models
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.* *;
}
-keep class com.fasterxml.jackson.core.type.TypeReference { *; }
-keep class * extends com.fasterxml.jackson.core.type.TypeReference
-keep class li.doerf.hacked.remote.hibp.BreachedAccount { *; }
-keep class li.doerf.hacked.remote.pwnedpasswords.** { *; }

# Room Database keep rules
-keep class * extends androidx.room.RoomDatabase
-keep class li.doerf.hacked.db.entities.** { *; }
-keep class li.doerf.hacked.db.daos.** { *; }

# Keep all workers in remote package
-keep class li.doerf.hacked.remote.**Worker { *; }

# Lifecycle and CompositionLocal keep rules
-keep class androidx.lifecycle.ViewTreeLifecycleOwner { *; }
-keep class androidx.lifecycle.compose.LocalLifecycleOwner** { *; }

