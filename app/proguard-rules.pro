# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# GMA Next-Gen SDK - keep ad SDK classes referenced via reflection
-keep class com.google.android.libraries.ads.mobile.sdk.** { *; }
-dontwarn com.google.android.libraries.ads.mobile.sdk.**

# WorkManager
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.background.systemjob.SystemJobService { *; }
-keep class androidx.work.impl.foreground.SystemForegroundService { *; }
-keep class androidx.work.impl.background.systemalarm.SystemAlarmService { *; }
-keep class androidx.work.impl.workers.DiagnosticsWorker { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
