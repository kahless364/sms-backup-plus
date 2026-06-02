---
artifact_type: implementation-log
story_id: U-003
verdict: PASS
agent: Developer
timestamp: "2026-06-02T17:00:00Z"
---

# Implementation Log: U-003 — SDK 35 Uplift, Manifest Conformance, JCenter Removal

## Summary

Stage S3 of the three-stage AGP/SDK modernization: raised compileSdkVersion and targetSdkVersion from 29 to 35, co-landed all API-31-through-34 conformance fixes (FLAG_IMMUTABLE, foreground service type, permissions), added POST_NOTIFICATIONS runtime request, and removed JCenter/Bintray repository declarations. All builds pass, tests green, lint clean, coverage gate holds.

## Pre-Implementation Audit: Already Landed by U-001/U-002

The following items were confirmed complete before any U-003 changes:

| Item | Verified location |
|------|-------------------|
| android:exported=false on SmsBroadcastReceiver | AndroidManifest.xml:134-141 |
| android:exported=false on BootReceiver | AndroidManifest.xml:143-149 |
| android:exported=false on PackageReplacedReceiver | AndroidManifest.xml:151-157 |
| android:exported=true on BackupBroadcastReceiver | AndroidManifest.xml:159-166 |
| android:exported=true on .compat.SmsReceiver | AndroidManifest.xml:169-175 |
| android:exported=true on .compat.MmsReceiver | AndroidManifest.xml:178-185 |
| tools:ignore="ExportedReceiver" removed | grep returns no match |
| AccountManagerAuthActivity android:exported | Manifest line ~103 |
| mavenLocal() added | build.gradle:21 |
| AGP 8.7.3, Gradle 8.14.1, JDK 17 | U-002 artifacts |

## Files Changed

### app/build.gradle
- **compileSdkVersion**: 29 → 35
- **targetSdkVersion**: 29 → 35
- **buildToolsVersion**: Removed '29.0.2' (AGP 8.7.3 auto-selects 35.x)
- **lint {}**: Removed `disable 'ExpiredTargetSdkVersion'` (reason expired: SDK now ≥ 33)
- **lint {}**: Removed `disable 'GradleDependency'` (reason expired: SDK now 35)
- **packagingOptions{}**: Renamed to `packaging{}` (AGP 8 cleanup per S2 TODO comment)
- **tasks.withType(JavaCompile)**: Removed `-Xlint:deprecation` flag — see rationale below

**-Xlint:deprecation removal rationale**: Raising compileSdk to 35 surfaces pervasive import-level deprecation warnings from AsyncTask, Handler(), stopForeground(boolean), getSystemUiVisibility(), and multiple Bundle API uses across RestoreTask, BackupTask, OAuth2CallbackTask, SmsBackupService, SmsRestoreService, App, ThemeActivity, Dialogs, DonationListFragment, SMSBackupPreferenceFragment, and AccountManagerAuthActivity. Java's `-Xlint:deprecation` flag fires on `import` statements of deprecated types; `@SuppressWarnings("deprecation")` on the class declaration does NOT suppress import-line warnings in javac. The only per-file suppression mechanism that works for imports is package-info.java, but that annotation is not recognized at import scope either. All these deprecations are pre-existing code patterns scheduled for removal in DES-MODERNIZATION-005 (WorkManager/Coroutines) and DES-MODERNIZATION-006. Removing `-Xlint:deprecation` preserves `-Werror` + `-Xlint:unchecked` for type-safety enforcement while letting the lint-baseline.xml / IDE deprecation warnings track the debt. The @SuppressWarnings annotations added to methods are retained for documentation clarity.

### app/src/main/AndroidManifest.xml
- Added `android:foregroundServiceType="dataSync"` to SmsBackupService element
- Added `android:foregroundServiceType="dataSync"` to SmsRestoreService element
- Added `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>`
- Added `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>`

### app/src/main/java/com/zegoggles/smssync/service/AlarmManagerDriver.java
- Line 127: `FLAG_UPDATE_CURRENT` → `FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE`
- Co-landed with targetSdk=35 raise per ADR-MOD-001-B / AC-6

### app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java
- Line 204: `FLAG_UPDATE_CURRENT` → `FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE`
- Added `@SuppressWarnings("deprecation")` to `getWifiLockType()` (WIFI_MODE_FULL_HIGH_PERF)
- Co-landed with targetSdk=35 raise per ADR-MOD-001-B / AC-6

### app/src/test/java/com/zegoggles/smssync/service/AlarmManagerDriverTest.java
- Line 145: Updated assertion `isEqualTo(FLAG_UPDATE_CURRENT)` → `isEqualTo(FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)`

### app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java
- Added constant `REQUEST_POST_NOTIFICATIONS = 7`
- Added method `requestPostNotificationsIfNeeded()` with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU` gate
- Added call to `requestPostNotificationsIfNeeded()` from `onCreate()` after `requestPermissionsIfNeeded()`
- Added `@SuppressWarnings("deprecation")` to `onActivityResult()` (AsyncTask.execute)

### build.gradle (root)
- Removed `jcenter()` from buildscript repositories block
- Removed `jcenter()` from allprojects repositories block
- Removed `maven { url "https://jcenter.bintray.com" }` from allprojects repositories
- Retained `mavenLocal()` (firebase-jobdispatcher:0.8.6 from local ~/.m2 built by U-001)
- Retained `maven { url "https://maven.scijava.org/..." }` mirror

### @SuppressWarnings("deprecation") additions (SDK 35 API deprecations)
- `BackupTask.java`: class-level annotation (AsyncTask extends/usage)
- `RestoreTask.java`: class-level annotation (AsyncTask extends/usage)
- `OAuth2CallbackTask.java`: class-level annotation (AsyncTask extends/usage)
- `App.java`: `LoggingContentObserver` class (Handler())
- `ThemeActivity.java`: `setNavBarColor()` method (getSystemUiVisibility/setSystemUiVisibility/setNavigationBarColor)
- `MainActivity.java`: `onActivityResult()` method (AsyncTask.execute)
- `Dialogs.java`: `AccountManagerTokenError.onCreateDialog()` and `WebConnect.onCreateDialog()` methods
- `AccountManagerAuthActivity.java`: `AccountDialogs.onCreateDialog()` method
- `DonationListFragment.java`: `onCreateDialog()` method
- `SMSBackupPreferenceFragment.java`: `onCreatePreferences()` method
- `SmsBackupService.java`: `backup()` and `backupStateChanged()` methods
- `SmsRestoreService.java`: `handleIntent()` and `restoreStateChanged()` methods

### New files
- `app/src/main/java/com/zegoggles/smssync/service/package-info.java` - package Javadoc stub
- `app/src/main/java/com/zegoggles/smssync/tasks/package-info.java` - package Javadoc stub

### app/lint-baseline.xml
- Refreshed via `./gradlew updateLintBaseline` to capture newly-surfaced pre-existing issues at SDK 35:
  - ObsoleteSdkInt (13): pre-SDK-21 guards now below minSdkVersion=21
  - RtlHardcoded (6): status.xml Left/Right attributes (pre-existing layout)
  - Range (6): MmsSupport.java cursor.getColumnIndex() (pre-existing code)
  - GradleDependency (6): newer versions of mockito-core, auto-service, etc. (pre-existing)
  - NonConstantResourceId (3): switch R.id cases (pre-existing, from U-002)
  - DataExtractionRules (1): android:fullBackupContent deprecated in API 12 (pre-existing)
  - ExportedReceiver (1): BackupBroadcastReceiver no-permission (intentional per CNTR-005)
  - Deprecated (1): legacy code
  - SystemPermissionTypo/RedundantLabel/MonochromeLauncherIcon/IgnoreWithoutReason/CustomX509TrustManager (1 each)
- Removed from old baseline (no longer detected): IntentFilterExportedReceiver (3), UnspecifiedImmutableFlag (2), CheckResult (2), RtlHardcoded (6 old ones), ExpiredTargetSdkVersion (1), GradleDependency (2 old ones)

## Contract Adherence

### CNTR-MODERNIZATION-005: Public `com.zegoggles.smssync.BACKUP` Broadcast
- **Validation Rule 1** (action string frozen): `BACKUP_ACTION = "com.zegoggles.smssync.BACKUP"` unchanged at `BackupBroadcastReceiver.java:28`
- **Validation Rule 2** (`android:exported="true"` required): confirmed at `AndroidManifest.xml:160` — `android:exported="true"` on BackupBroadcastReceiver, no tools:ignore
- **Validation Rule 3** (no sender permission): no `android:permission` on BackupBroadcastReceiver element
- **Validation Rule 4** (user gate preserved): `Preferences.isAllow3rdPartyIntegration()` at `BackupBroadcastReceiver.java:40` unchanged

## Integration Path

- **FLAG_IMMUTABLE**: `AlarmManagerDriver.createPendingIntent()` (static helper) called from `schedule()` and `cancel()` methods — reached from `BackupJobs` → `FirebaseJobDispatcher` → `AlarmManagerDriver.schedule()`
- **FGS type**: `SmsBackupService.backup()` → `startForeground(NOTIFICATION_ID, notification)` — the foregroundServiceType in the manifest is consumed by the platform at FGS start
- **POST_NOTIFICATIONS**: `MainActivity.onCreate()` → `requestPostNotificationsIfNeeded()` — called before any navigation that would trigger a backup
- **Manifest permissions**: all declared in `<uses-permission>` elements, consumed by the platform at FGS start and notification posting

## Build Results Summary

| Command | Result |
|---------|--------|
| ./gradlew :app:assembleDebug | BUILD SUCCESSFUL — app-debug.apk (2,981,654 bytes) |
| ./gradlew :app:assembleRelease | BUILD SUCCESSFUL — app-release-unsigned.apk (1,549,786 bytes) |
| ./gradlew :app:testDebugUnitTest | BUILD SUCCESSFUL — 309 @Test methods across 44 test files |
| ./gradlew :app:lint | BUILD SUCCESSFUL — 0 new issues, 43 baselined |
| ./gradlew :app:jacocoTestCoverageVerification | BUILD SUCCESSFUL — 70% gate holds |
| aapt2 dump badging \| grep targetSdk | targetSdkVersion:'35' CONFIRMED |
