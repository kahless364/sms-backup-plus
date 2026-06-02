# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/Caskroom/android-sdk/3859397/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# U-002: R8 full-mode keep rules (AGP 8 default; interim until later MUs)
# ---------------------------------------------------------------------------

# Otto event bus: preserve all @Subscribe and @Produce annotated methods.
# Otto dispatches events by reflecting on these methods at runtime. R8 full-mode
# would otherwise inline, rename, or strip them, silently breaking all event delivery.
# INTERIM: remove when Otto is replaced by StateFlow in MU-006.
-keepclassmembers class * {
    @com.squareup.otto.Subscribe <methods>;
    @com.squareup.otto.Produce <methods>;
}

# firebase-jobdispatcher: SmsJobService is resolved reflectively via the
# ACTION_EXECUTE intent filter. Without this rule R8 strips or renames the class.
# INTERIM: remove when firebase-jobdispatcher is replaced by WorkManager in MU-005.
-keep public class com.zegoggles.smssync.service.SmsJobService { *; }
-keep class com.firebase.jobdispatcher.** { *; }

# Preserve enum valueOf() and values() methods used by Preferences reflection paths.
-keepclassmembers enum com.zegoggles.smssync.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
