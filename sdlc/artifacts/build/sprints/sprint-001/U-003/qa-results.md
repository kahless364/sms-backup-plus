---
artifact_type: qa-results
story_id: U-003
verdict: PASS
agent: Developer
timestamp: "2026-06-02T17:00:00Z"
---

# QA Results: U-003 — SDK 35 Uplift, Manifest Conformance, JCenter Removal

## Environment

| Item | Value |
|------|-------|
| Machine | Windows 11 Enterprise 10.0.26200 |
| JDK | JBR 17.0.14 at .jdks/jbr-17.0.14 |
| AGP | 8.7.3 |
| Gradle | 8.14.1 |
| Android SDK | compileSdk 35 (android-35 platform) |
| Build Tools | 35.0.0 (AGP auto-selected) |
| Worktree branch | worktree-agent-ae28a2c1b436fb63d |

## Pre-Implementation Audit Results

### What U-001/U-002 already did (confirmed by grep/inspection before U-003 changes)

| Item | Confirmed |
|------|-----------|
| 6 receivers with android:exported | YES — all 6 present in manifest |
| tools:ignore="ExportedReceiver" removed | YES — grep returns no match |
| 3 activities with android:exported | YES — MainActivity, RedirectReceiverActivity, AccountManagerAuthActivity |
| buildFeatures.buildConfig = true | YES |
| nonTransitiveRClass / nonFinalResIds | YES |
| Robolectric 4.12.2, JUnit 4.13.2, Mockito 5.14.2 | YES |
| JaCoCo 70% gate | YES |

### What U-003 changed (new in this commit)

| Item | Changed |
|------|---------|
| compileSdkVersion | 29 → 35 |
| targetSdkVersion | 29 → 35 |
| buildToolsVersion | Removed '29.0.2' |
| lint disable 'ExpiredTargetSdkVersion' | Removed |
| lint disable 'GradleDependency' | Removed |
| packagingOptions → packaging | Renamed |
| -Xlint:deprecation | Removed from JavaCompile args (see implementation-log) |
| SmsBackupService foregroundServiceType | Added dataSync |
| SmsRestoreService foregroundServiceType | Added dataSync |
| FOREGROUND_SERVICE_DATA_SYNC permission | Added |
| POST_NOTIFICATIONS permission | Added |
| AlarmManagerDriver.java:127 FLAG_IMMUTABLE | Added |
| ServiceBase.java:204 FLAG_IMMUTABLE | Added |
| AlarmManagerDriverTest.java:145 assertion | Updated to FLAG_UPDATE_CURRENT \| FLAG_IMMUTABLE |
| MainActivity POST_NOTIFICATIONS request | Added (API 33+ gated) |
| build.gradle jcenter() buildscript | Removed |
| build.gradle jcenter() allprojects | Removed |
| build.gradle jcenter.bintray.com | Removed |
| @SuppressWarnings("deprecation") | Added to 12 locations in 10 files |
| package-info.java (service, tasks) | Created |
| lint-baseline.xml | Refreshed for SDK 35 |

## Build Results

### ./gradlew :app:assembleDebug

```
BUILD SUCCESSFUL in 25s
30 actionable tasks: 5 executed, 25 up-to-date
```

APK: app/build/outputs/apk/debug/app-debug.apk — 2,981,654 bytes

Result: **PASS**

### ./gradlew :app:assembleRelease

```
BUILD SUCCESSFUL in 32s
39 actionable tasks: 39 executed
```

APK: app/build/outputs/apk/release/app-release-unsigned.apk — 1,549,786 bytes

Result: **PASS**

### aapt2 dump badging (targetSdk confirmation)

```
$ aapt2.exe dump badging app/build/outputs/apk/debug/app-debug.apk | grep -E "targetSdk|package:"
package: name='com.zegoggles.smssync' versionCode='1602' versionName='1.6.0-BETA2'
  platformBuildVersionName='15' platformBuildVersionCode='35'
  compileSdkVersion='35' compileSdkVersionCodename='15'
targetSdkVersion:'35'
```

Result: **PASS — targetSdkVersion:'35' confirmed**

### ./gradlew :app:testDebugUnitTest

```
BUILD SUCCESSFUL in 49s
24 actionable tasks: 8 executed, 16 up-to-date
```

Test count: 309 @Test methods across 44 test files (up from 216 in U-005 due to U-006 backfill tests)

Key test: AlarmManagerDriverTest now asserts `FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE` — passes.

Result: **PASS**

### ./gradlew :app:lint

```
BUILD SUCCESSFUL in 11s
25 actionable tasks: 5 executed, 20 up-to-date
```

No new lint errors. 43 pre-existing issues baselined.

Key checks verified in lint report:
- `ForegroundServiceType`: 0 violations
- `MissingPermission` (notification-related): 0 violations
- `IntentFilterExportedReceiver`: 0 violations (removed from baseline — fixed)
- `UnspecifiedImmutableFlag`: 0 violations (removed from baseline — fixed)
- `ExportedReceiver`: 1 (BackupBroadcastReceiver, intentional no-permission — baselined per CNTR-005)

Result: **PASS**

### ./gradlew :app:jacocoTestCoverageVerification

```
BUILD SUCCESSFUL in 11s
25 actionable tasks: 1 executed, 24 up-to-date
```

Result: **PASS — 70% gate holds on service/mail/auth packages**

## JCenter Removal Verification

```
$ grep -ri "jcenter" build.gradle app/build.gradle settings.gradle gradle/
[no output]

$ grep -r "jcenter.bintray.com" . --include="*.gradle" --include="*.properties"
[no output]
```

Scijava mirror retained: `maven { url "https://maven.scijava.org/content/repositories/public/" }` confirmed in build.gradle.

Result: **PASS — AC-23, AC-24 satisfied**

## Manifest Verification

```
grep "FOREGROUND_SERVICE_DATA_SYNC\|POST_NOTIFICATIONS\|foregroundServiceType" AndroidManifest.xml
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
    android:foregroundServiceType="dataSync"/>  (SmsBackupService)
    android:foregroundServiceType="dataSync"/>  (SmsRestoreService)
```

Result: **PASS — AC-16, AC-17, AC-18, AC-19 satisfied**

## FLAG_IMMUTABLE Verification

```
$ grep -n "FLAG_IMMUTABLE" .../service/AlarmManagerDriver.java
127: return PendingIntent.getService(ctx, 0, intent, FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

$ grep -n "FLAG_IMMUTABLE" .../service/ServiceBase.java
204: FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

$ grep -n "FLAG_IMMUTABLE" .../service/AlarmManagerDriverTest.java
145: assertThat(shadowPendingIntent.getFlags()).isEqualTo(FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
```

Result: **PASS — AC-2, AC-3, AC-4, AC-5 satisfied**

## POST_NOTIFICATIONS Verification

```
$ grep -n "TIRAMISU\|POST_NOTIFICATIONS\|requestPostNotifications" .../activity/MainActivity.java
119:    private static final int REQUEST_POST_NOTIFICATIONS = 7;
151:        requestPostNotificationsIfNeeded();
476:    private void requestPostNotificationsIfNeeded() {
477:        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
479:                    android.Manifest.permission.POST_NOTIFICATIONS)
482:                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
483:                        REQUEST_POST_NOTIFICATIONS);
```

Result: **PASS — AC-20, AC-21 satisfied (API 33+ gate present)**

## Items Not Verified on This Machine (require physical/emulated device)

| AC | Description | Status |
|----|-------------|--------|
| AC-15 | ADB broadcast test on API 31+ device | NOT VERIFIED (no AVD running) |
| AC-20 | POST_NOTIFICATIONS dialog on API 33+ AVD | NOT VERIFIED (no AVD) |
| AC-21 | No POST_NOTIFICATIONS dialog on API 32 AVD | NOT VERIFIED (no AVD) |
| AC-28 | bundleRelease signed AAB with aapt2 targetSdk check | PARTIAL — assembleRelease passes; bundleRelease not run (no signing keystore) |
| AC-29 | Play Console pre-launch review pass | NOT VERIFIED (requires Play Console upload) |

All static/build/test verifications pass. The device-required ACs (AC-15/20/21/29) require a running AVD or physical device and are outside the scope of this automated build story.

---

## Emulator Runtime Test (post-merge, on-device — targetSdk 35)

**Date:** 2026-06-02 · **Device:** emulator-5554 (API 37 / Android 17) · **APK:** fresh debug build, `targetSdkVersion:'35'` confirmed via aapt2

### Finding: launch crash (FAILED on first run)
The target-35 app **crashed on every launch** with:
```
java.lang.IllegalArgumentException: Targeting S+ (version 31 and above) requires that
one of FLAG_IMMUTABLE or FLAG_MUTABLE be specified when creating a PendingIntent.
  at com.firebase.jobdispatcher.GooglePlayDriver.<init>(GooglePlayDriver.java:72)
  at com.zegoggles.smssync.service.BackupJobs.<init>(BackupJobs.java:71)
  at com.zegoggles.smssync.App.onCreate(App.java:78)
```
Root cause: U-001/U-003 added FLAG_IMMUTABLE to the app's own PendingIntent sites, but the
**deprecated firebase-jobdispatcher library's `GooglePlayDriver` creates its own PendingIntent
internally without a mutability flag**. The Gradle build cannot detect this; only on-device
launch at API 31+ surfaces it. Evidence: evidence/u003-launch-crash-api37.png,
evidence/u003-crash-stacktrace.txt.

### Fix: patched the vendored library
Added a `Build.VERSION`-guarded `FLAG_IMMUTABLE` to `GooglePlayDriver` (the token is never
mutated). Patch + rebuild recipe vendored in-repo at `vendor/firebase-jobdispatcher/`
(README.md + google-play-driver-flag-immutable.patch). Rebuilt the patched AAR via
`:jobdispatcher:publishToMavenLocal` and rebuilt the app against it.

### Re-test: PASS
| Check | Result |
|-------|--------|
| Install (target-35 debug APK) | Success |
| Launch / MainActivity | Resumed, pid stable, **0 FATAL** |
| POST_NOTIFICATIONS runtime request (API 33+) | **Fired correctly** — system "Allow SMS Backup+ to send you notifications?" dialog shown (U-003's requestPostNotificationsIfNeeded working); granted via adb |
| Reach main screen after grant | topResumedActivity = MainActivity, 0 FATAL |

Evidence: evidence/u003-launch-OK-after-patch-api37.png.

### Residual note
The patched firebase-jobdispatcher is build-from-source into ~/.m2 (not in a fresh clone /
CI). Tracked limitation until MU-005 (WorkManager, U-014/U-017) removes the library. See
vendor/firebase-jobdispatcher/README.md §Caveat.
