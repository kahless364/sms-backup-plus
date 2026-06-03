# Vendored firebase-jobdispatcher 0.8.6 (patched)

## Why this exists

`com.firebase:firebase-jobdispatcher:0.8.6` is the deprecated, archived Google library
SMS Backup+ uses for background scheduling (`BackupJobs` → `GooglePlayDriver`). Two
problems make it un-consumable as-is on a modern toolchain:

1. **No longer published.** The artifact is gone from Maven Central and JCenter (JCenter
   shut down Feb 2022). The scijava mirror that briefly hosted it is unreliable. So the
   AAR must be built from source (`googlearchive/firebase-jobdispatcher-android`, tag
   `v0.8.6`) and published to the local Maven repo (`~/.m2`). `mavenLocal()` in the root
   `build.gradle` resolves it. (Established in story U-001.)

2. **Crashes at targetSdk 31+.** `GooglePlayDriver`'s constructor calls
   `PendingIntent.getBroadcast(context, 0, new Intent(), 0)` with **no mutability flag**.
   Android 12 (API 31)+ throws `IllegalArgumentException` for any PendingIntent created
   without `FLAG_IMMUTABLE`/`FLAG_MUTABLE`. Because `App.onCreate()` constructs
   `BackupJobs` (which builds `GooglePlayDriver` on the default scheduler path), the app
   **crashes on every launch** once targetSdk is raised to 35 (story U-003). Discovered
   by on-device emulator testing (the Gradle build does not catch it).

## The patch

`google-play-driver-flag-immutable.patch` adds a `Build.VERSION`-guarded `FLAG_IMMUTABLE`
to the `GooglePlayDriver` identity-token PendingIntent. The token is never mutated, so
immutable is correct. Guarded by `SDK_INT >= 31` (integer literal, because the library
compiles against compileSdk 29 where `Build.VERSION_CODES.S` is not a named constant).

## How to (re)build the patched artifact

```bash
# 1. Clone the archived source at the 0.8.6 tag
git clone https://github.com/googlearchive/firebase-jobdispatcher-android.git \
  /tmp/firebase-jobdispatcher-src
cd /tmp/firebase-jobdispatcher-src
git checkout v0.8.6   # or the 0.8.6 release commit

# 2. Apply the patch (from the repo root: vendor/firebase-jobdispatcher/)
git apply /path/to/sms-backup-plus/vendor/firebase-jobdispatcher/google-play-driver-flag-immutable.patch

# 3. Build + publish to local Maven (requires JDK 17, Android SDK)
export JAVA_HOME=<jdk-17>
export ANDROID_HOME=<android-sdk>
./gradlew :jobdispatcher:publishToMavenLocal
# -> publishes com.firebase:firebase-jobdispatcher:0.8.6 (patched) to ~/.m2
```

After this, the app build resolves the patched AAR via `mavenLocal()`.

## Lifespan

**This whole arrangement is temporary.** Story U-014/U-017 (the WorkManager migration,
MU-005) removes firebase-jobdispatcher entirely. When that lands:
- delete this `vendor/firebase-jobdispatcher/` directory,
- remove `mavenLocal()` from the root `build.gradle` (if nothing else needs it),
- remove the `com.firebase:firebase-jobdispatcher` dependency from `app/build.gradle`.

## Caveat for CI / fresh clones

A fresh checkout (or CI runner) has neither the local source nor the `~/.m2` artifact.
Until the WorkManager migration removes the dependency, CI must run the rebuild recipe
above as a prerequisite step, or the build will fail to resolve
`com.firebase:firebase-jobdispatcher:0.8.6`. This is a known, tracked limitation.
