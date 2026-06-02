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
# U-004: R8 keep rules for reflection surfaces (interim — removed by later MUs)
# ---------------------------------------------------------------------------

# Otto event bus: preserve all methods annotated with @Subscribe or @Produce.
# Otto dispatches events by reflecting on these methods at runtime. R8 would
# otherwise rename or strip them, silently breaking all event delivery.
# 12 @Subscribe/@Produce import sites across 11 files in the current codebase.
# These keep rules are INTERIM and must be removed when Otto is replaced by
# StateFlow in MU-006.
-keep @com.squareup.otto.Subscribe class * { *; }
-keepclassmembers class * {
    @com.squareup.otto.Subscribe <methods>;
    @com.squareup.otto.Produce <methods>;
}

# firebase-jobdispatcher: SmsJobService is resolved reflectively via the
# ACTION_EXECUTE intent filter. Without this rule R8 strips or renames the
# class, breaking job dispatch.
# INTERIM: remove when firebase-jobdispatcher is replaced by WorkManager in MU-005.
-keep public class com.zegoggles.smssync.service.SmsJobService { *; }

# AuthMode and DataType enums: Preferences.getDefaultType() uses valueOf() paths
# that R8 treats as dead code without a keep rule.
-keepclassmembers enum com.zegoggles.smssync.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
