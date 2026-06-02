---
type: story
status: planned
sprint: "000001"
artifact_type: user-story
priority: critical
complexity: high
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-001
design_docs:
  - DES-MODERNIZATION-001
integration_contracts:
  - CNTR-MODERNIZATION-005
dependencies:
  - U-002
  - U-005
change_records: []
platforms:
  - android
tags:
  - sdk35
  - manifest-conformance
  - jcenter-removal
  - gate-g0
gate_additions:
  - G0
id: U-003
title: 'Stage S3: raise targetSdk to 35, co-land all API-31-through-34 conformance fixes, and remove JCenter'
pipeline: modernization
domain: modernization
---

# U-003: Stage S3 — SDK 35 Uplift, Manifest Conformance, and JCenter Removal

## Story

As a maintainer of SMS Backup+,
I want to raise `compileSdkVersion` and `targetSdkVersion` from 29 to 35, co-land all required Android API-31-through-34 behavioral conformance fixes in the same commit, and remove the defunct JCenter repository declarations,
So that the app produces a release-signed Android App Bundle (AAB) at `targetSdk 35` that passes Google Play's pre-launch policy review (Gate G0), unblocking all further modernization work from reaching users.

## Acceptance Criteria

- [ ] **AC-1 — SDK declarations raised to 35.**
  `app/build.gradle` declares `compileSdkVersion 35` and `targetSdkVersion 35`.
  Verified by: `grep "compileSdkVersion\|targetSdkVersion" app/build.gradle` returns only lines containing the value `35`; no line contains `29`.

- [ ] **AC-2 — FLAG_IMMUTABLE added at AlarmManagerDriver.java:127.**
  The `PendingIntent.getService(...)` call at `app/src/main/java/com/zegoggles/smssync/service/AlarmManagerDriver.java` (currently line 127) passes `FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE` as its flags argument.
  Verified by: `grep -n "FLAG_IMMUTABLE" app/src/main/java/com/zegoggles/smssync/service/AlarmManagerDriver.java` returns a match at the relevant line.

- [ ] **AC-3 — FLAG_IMMUTABLE added at ServiceBase.java:204.**
  The `PendingIntent.getActivity(...)` call at `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java` (currently line 204 token) passes `FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE` as its flags argument.
  Verified by: `grep -n "FLAG_IMMUTABLE" app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java` returns a match at the relevant line.

- [ ] **AC-4 — No remaining PendingIntent construction site lacks a mutability flag.**
  Every `PendingIntent.get*` call in `app/src/main/java/` carries either `FLAG_IMMUTABLE` or `FLAG_MUTABLE`.
  Verified by: `grep -rn "PendingIntent\.get" app/src/main/java/` followed by inspection confirming each site carries a mutability flag; currently exactly two production sites exist (AlarmManagerDriver and ServiceBase — grep-confirmed by DES-MODERNIZATION-001 §PendingIntent) and both are covered by AC-2 and AC-3.

- [ ] **AC-5 — AlarmManagerDriverTest:97 flag assertion updated to the ORed value.**
  `app/src/test/java/com/zegoggles/smssync/service/AlarmManagerDriverTest.java` line 97 asserts `getFlags()` equals `FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE`, not the prior `FLAG_UPDATE_CURRENT` alone.
  Verified by: `grep -n "FLAG_IMMUTABLE\|FLAG_UPDATE_CURRENT" app/src/test/java/com/zegoggles/smssync/service/AlarmManagerDriverTest.java` shows the combined flag at the assertion site.

- [ ] **AC-6 — FLAG_IMMUTABLE and targetSdk 35 co-land in the same commit.**
  No intermediate commit in the PR exists where `targetSdkVersion` is `35` and either `AlarmManagerDriver.java:127` or `ServiceBase.java:204` lacks `FLAG_IMMUTABLE`.
  Verified by: PR commit history; the commit setting `targetSdkVersion 35` is identical to, or preceded within the same PR by, the commit adding `FLAG_IMMUTABLE` at both sites, with no green-build intermediate where one change is absent and the other present.

- [ ] **AC-7 — SmsBroadcastReceiver declared exported="false".**
  `app/src/main/AndroidManifest.xml` line 131 `<receiver>` element for `SmsBroadcastReceiver` carries `android:exported="false"`.
  Verified by: static inspection of `AndroidManifest.xml` at the `SmsBroadcastReceiver` element.

- [ ] **AC-8 — BootReceiver declared exported="false".**
  `app/src/main/AndroidManifest.xml` line 139 `<receiver>` element for `BootReceiver` carries `android:exported="false"`.
  Verified by: static inspection of `AndroidManifest.xml` at the `BootReceiver` element.

- [ ] **AC-9 — PackageReplacedReceiver declared exported="false".**
  `app/src/main/AndroidManifest.xml` line 145 `<receiver>` element for `PackageReplacedReceiver` carries `android:exported="false"`.
  Verified by: static inspection of `AndroidManifest.xml` at the `PackageReplacedReceiver` element.

- [ ] **AC-10 — BackupBroadcastReceiver declared exported="true" with tools:ignore removed.**
  `app/src/main/AndroidManifest.xml` line 151 `<receiver>` element for `BackupBroadcastReceiver` carries `android:exported="true"` and the previously present `tools:ignore="ExportedReceiver"` attribute is removed.
  Verified by: (a) static inspection confirms `android:exported="true"` on the element; (b) `grep "ExportedReceiver" app/src/main/AndroidManifest.xml` returns no output.

- [ ] **AC-11 — .compat.SmsReceiver declared exported="true".**
  `app/src/main/AndroidManifest.xml` line 159 `<receiver>` element for `.compat.SmsReceiver` carries `android:exported="true"`.
  Verified by: static inspection of `AndroidManifest.xml` at the `.compat.SmsReceiver` element.

- [ ] **AC-12 — .compat.MmsReceiver declared exported="true".**
  `app/src/main/AndroidManifest.xml` line 167 `<receiver>` element for `.compat.MmsReceiver` carries `android:exported="true"`.
  Verified by: static inspection of `AndroidManifest.xml` at the `.compat.MmsReceiver` element.

- [ ] **AC-13 — No ExportedReceiver lint suppression remains anywhere in the manifest.**
  `grep "ExportedReceiver" app/src/main/AndroidManifest.xml` returns no output.
  Verified by: command output showing zero matches.

- [ ] **AC-14 — Lint reports zero ExportedReceiver violations for all six receivers.**
  `./gradlew lint` CI report contains zero violations of the `ExportedReceiver` lint category for any of the six intent-filtered `<receiver>` elements.
  Verified by: CI lint report artifact.

- [ ] **AC-15 — ADB BACKUP broadcast reaches BackupBroadcastReceiver on API 31+ (CNTR-005 contract preserved).**
  On a test device or emulator running API 31 or higher with `3rd party integration` enabled in Advanced Settings, executing `adb shell am broadcast -a com.zegoggles.smssync.BACKUP com.zegoggles.smssync` results in logcat output confirming `BackupBroadcastReceiver.onReceive` was called and the backup was enqueued (log line `"backup requested via broadcast intent"`).
  Verified by: logcat capture showing the receipt log line after the ADB broadcast command; the action string `com.zegoggles.smssync.BACKUP`, the target package `com.zegoggles.smssync`, and the absence of any `android:permission` on the receiver are all confirmed unchanged.

- [ ] **AC-16 — SmsBackupService foregroundServiceType set to dataSync.**
  The `<service>` element for `SmsBackupService` in `app/src/main/AndroidManifest.xml` (currently line 123) includes `android:foregroundServiceType="dataSync"`.
  Verified by: static inspection of the `SmsBackupService` `<service>` element in `AndroidManifest.xml`.

- [ ] **AC-17 — SmsRestoreService foregroundServiceType set to dataSync.**
  The `<service>` element for `SmsRestoreService` in `app/src/main/AndroidManifest.xml` (currently line 124) includes `android:foregroundServiceType="dataSync"`.
  Verified by: static inspection of the `SmsRestoreService` `<service>` element in `AndroidManifest.xml`.

- [ ] **AC-18 — FOREGROUND_SERVICE_DATA_SYNC permission declared.**
  `app/src/main/AndroidManifest.xml` contains `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>`.
  Verified by: `grep "FOREGROUND_SERVICE_DATA_SYNC" app/src/main/AndroidManifest.xml` returns a match.

- [ ] **AC-19 — POST_NOTIFICATIONS permission declared.**
  `app/src/main/AndroidManifest.xml` contains `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>`.
  Verified by: `grep "POST_NOTIFICATIONS" app/src/main/AndroidManifest.xml` returns a match.

- [ ] **AC-20 — POST_NOTIFICATIONS runtime request fires on API 33+ before first notification.**
  On an AVD running API 33 (Android 13) or higher, initiating a backup from the app UI causes a system `POST_NOTIFICATIONS` permission dialog to appear before any backup-progress notification is posted (or, if the permission has already been granted, the permission check passes silently without crash).
  Verified by: instrumentation test or documented manual smoke on an API 33+ AVD confirming the dialog appears on first backup initiation with the permission not yet granted.

- [ ] **AC-21 — POST_NOTIFICATIONS runtime request is suppressed on API 32 and below.**
  On an AVD running API 32 (Android 12L) or lower, initiating a backup does not trigger a `POST_NOTIFICATIONS` permission request and does not throw `RuntimeException` or crash.
  Verified by: instrumentation test or documented manual smoke on an API 32 AVD confirming no `POST_NOTIFICATIONS` dialog appears and no `RuntimeException` is logged.

- [ ] **AC-22 — Lint reports zero ForegroundServiceType and MissingPermission violations.**
  `./gradlew lint` CI report contains zero violations of `ForegroundServiceType` or `MissingPermission` (notification-posting) categories.
  Verified by: CI lint report artifact.

- [ ] **AC-23 — jcenter() removed from all Gradle files.**
  The string `jcenter` does not appear anywhere in `build.gradle`, `app/build.gradle`, `settings.gradle`, or any file under `gradle/`.
  Verified by: `grep -ri "jcenter" build.gradle app/build.gradle settings.gradle gradle/` returns no output. The three verified occurrences — `build.gradle:4` (buildscript block), `build.gradle:21` (allprojects block), and `build.gradle:25` (literal `https://jcenter.bintray.com` URL) — are all removed.

- [ ] **AC-24 — jcenter.bintray.com URL removed from all Gradle and properties files.**
  The string `jcenter.bintray.com` does not appear in any `*.gradle` or `*.properties` file.
  Verified by: `grep -r "jcenter.bintray.com" .` returns no output.

- [ ] **AC-25 — scijava mirror retained; firebase-jobdispatcher resolves cleanly.**
  The `https://maven.scijava.org/...` repository entry in `build.gradle` (the mirror hosting `com.firebase:firebase-jobdispatcher:0.8.6`) is NOT removed by this story. After jcenter removal, `./gradlew dependencies --refresh-dependencies` on a fully cleared Gradle cache completes with no "Could not resolve" errors for `firebase-jobdispatcher` or any other dependency.
  Verified by: CI build log showing clean dependency resolution with `--refresh-dependencies` on a cleared cache; `firebase-jobdispatcher` artifact is confirmed resolved from the scijava mirror, not from a jcenter endpoint.

- [ ] **AC-26 — assembleRelease exits 0 on a clean checkout after all changes.**
  `./gradlew assembleRelease` exits 0 on a clean checkout with a cleared Gradle cache, using only `google()`, `mavenCentral()`, `https://jitpack.io`, and the scijava mirror as repository sources.
  Verified by: CI build log showing exit code 0 and no "Could not resolve" or "Manifest merger failed" errors.

- [ ] **AC-27 — API 31+ Robolectric scheduling test passes without IllegalArgumentException.**
  The Robolectric test covering the `AlarmManagerDriver` backup-scheduling path, running against an emulated API 31+ SDK level (authored or updated under U-005 / REQ-MODERNIZATION-001 §AC-14), completes without throwing `IllegalArgumentException` and without any assertion failure.
  Verified by: `./gradlew test` output showing the scheduling test passing under the API 31+ SDK level.

- [ ] **AC-28 — bundleRelease produces a signed AAB with aapt2-confirmed targetSdkVersion 35 (Gate G0 prerequisite).**
  `./gradlew bundleRelease` produces a non-empty signed AAB file. Running `aapt2 dump badging <path-to-release.aab> | grep targetSdkVersion` outputs `targetSdkVersion:'35'`.
  Verified by: (a) presence and non-zero file size of the AAB artifact; (b) `aapt2 dump badging` command output showing `targetSdkVersion:'35'`.

- [ ] **AC-29 — Signed AAB submitted to Play internal track passes pre-launch review (Gate G0).**
  The signed AAB produced by AC-28 is submitted to the Google Play Console internal testing track. The submission is accepted without any policy-rejection email or console error on the dimensions of `targetSdkVersion`, `android:exported`, foreground-service type, or `PendingIntent` mutability.
  Verified by: Google Play Console submission status showing no policy violation on any of these dimensions. This is the terminal gate signal for Gate G0 / Milestone M0.

### Integration Criteria

No new runtime components are introduced by this story. The single new code path — the SDK-gated `POST_NOTIFICATIONS` runtime request — attaches to the existing user-initiated backup/restore entry point in `MainActivity` and the existing notification path in `ServiceBase`. No new component, module, package, or public interface is created.

- [ ] **IC-1**: The `POST_NOTIFICATIONS` runtime request code path at the backup initiation entry point is version-gated: it is only reached when `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU` (API 33). The same idiom used at `ServiceBase.java:208` (`Build.VERSION.SDK_INT >= ...`) is used here. Verified by static inspection of the call site.
- [ ] **IC-2**: `BackupBroadcastReceiver` in `AndroidManifest.xml:151` carries `android:exported="true"` and no `tools:ignore="ExportedReceiver"`, satisfying the manifest precondition in CNTR-MODERNIZATION-005 §Validation Rule 2. Verified by static inspection.
- [ ] **IC-3**: The `AlarmManagerDriverTest` flag assertion at line 97 is updated in the same commit as the `FLAG_IMMUTABLE` OR, so the test suite remains green immediately after the S3 commit lands. Verified by CI test run in the S3 commit.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/build.gradle` | `compileSdkVersion 29`, `targetSdkVersion 29` (lines 10, 15); `buildToolsVersion "29.0.2"` (line 11) | Raise to `compileSdkVersion 35`, `targetSdkVersion 35`, `buildToolsVersion` paired with SDK 35 |
| `app/src/main/AndroidManifest.xml` | Six intent-filtered `<receiver>` elements lack `android:exported`; `BackupBroadcastReceiver` has `tools:ignore="ExportedReceiver"`; `SmsBackupService`/`SmsRestoreService` lack `foregroundServiceType`; `FOREGROUND_SERVICE_DATA_SYNC` and `POST_NOTIFICATIONS` permissions absent | Add explicit `android:exported` to all six receivers (3x false, 3x true); remove `tools:ignore` from `BackupBroadcastReceiver`; add `foregroundServiceType="dataSync"` to both services; add both new permissions |
| `app/src/main/java/com/zegoggles/smssync/service/AlarmManagerDriver.java` | `PendingIntent.getService(..., FLAG_UPDATE_CURRENT)` at line 127 — no mutability flag | OR in `PendingIntent.FLAG_IMMUTABLE`: `FLAG_UPDATE_CURRENT \| PendingIntent.FLAG_IMMUTABLE` |
| `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java` | `PendingIntent.getActivity(..., FLAG_UPDATE_CURRENT)` at line 204 token — no mutability flag | OR in `PendingIntent.FLAG_IMMUTABLE`: `FLAG_UPDATE_CURRENT \| PendingIntent.FLAG_IMMUTABLE` |
| `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` (or the backup-initiation entry point) | No `POST_NOTIFICATIONS` runtime request before starting backup | Add `ActivityCompat.requestPermissions(..., POST_NOTIFICATIONS)` call, version-gated to `SDK_INT >= Build.VERSION_CODES.TIRAMISU` (API 33) |
| `app/src/test/java/com/zegoggles/smssync/service/AlarmManagerDriverTest.java` | Line 97 asserts `assertThat(shadowPendingIntent.getFlags()).isEqualTo(FLAG_UPDATE_CURRENT)` | Update assertion to `isEqualTo(FLAG_UPDATE_CURRENT \| PendingIntent.FLAG_IMMUTABLE)` |
| `build.gradle` | `jcenter()` at lines 4 and 21; `https://jcenter.bintray.com` at line 25 | Remove all three JCenter/Bintray references; retain `https://maven.scijava.org/...` mirror |

## Existing Behavior to Preserve

- **Public BACKUP broadcast contract (CNTR-MODERNIZATION-005 — HARD-PRESERVE).** The action string `com.zegoggles.smssync.BACKUP`, the target package `com.zegoggles.smssync`, the `android:exported="true"` state of `BackupBroadcastReceiver`, the absence of any `android:permission` on the receiver, the user-gate behavior (`third_party_integration` preference, default false), and the post-condition (an immediate backup is enqueued when the gate is open) MUST all survive unchanged. Third-party callers (Tasker, MacroDroid, ADB) must continue to trigger backups via this broadcast.
- **SMS delivery path.** `.compat.SmsReceiver` (SMS_DELIVER filter, `BROADCAST_SMS` permission) and `.compat.MmsReceiver` (WAP_PUSH_DELIVER filter, `BROADCAST_WAP_PUSH` permission) must remain reachable by the platform; both are set `exported="true"`.
- **Boot auto-backup trigger.** `BootReceiver` continues to receive `BOOT_COMPLETED` and trigger the scheduled backup after reboot. Setting `exported="false"` is correct because `BOOT_COMPLETED` is a system broadcast; the platform still delivers it to `exported="false"` receivers. Verify the receiver continues to fire after emulator reboot.
- **SMS_RECEIVED implicit broadcast.** `SmsBroadcastReceiver` continues to receive the `SMS_RECEIVED` broadcast from the platform despite `exported="false"` (system broadcasts are exempt from the exported restriction for delivery). Verify the receiver is not accidentally removed from the manifest.
- **Foreground service continuity.** Both `SmsBackupService` and `SmsRestoreService` must continue to start as foreground services and post progress notifications without `ForegroundServiceStartNotAllowedException` or `SecurityException`. The `foregroundServiceType="dataSync"` addition enables this at API 34; the `FOREGROUND_SERVICE_DATA_SYNC` permission satisfies the manifest requirement.
- **scijava mirror and firebase-jobdispatcher resolution.** The `https://maven.scijava.org/...` mirror in `build.gradle` must NOT be removed; `firebase-jobdispatcher:0.8.6` depends on it exclusively (verified DES-MODERNIZATION-001 §JCenter Removal). Its removal is owned by MU-005.
- **warningsAsErrors / -Werror policy.** `warningsAsErrors true` (app/build.gradle:41) and `-Werror -Xlint:deprecation` (app/build.gradle:75) must remain. The `lint-baseline.xml` (committed at S1 by U-001) governs the pre-existing warning set; this story may only remove entries from it, not add new suppressions.
- **AlarmManagerDriver interim status.** `AlarmManagerDriver.java` remains in the codebase with the `FLAG_IMMUTABLE` fix in place. It is deleted in full by U-017 (WorkManager migration, MU-005). Do not delete it here.

## Verification Steps

1. **AC-1 — SDK declarations.**
   Run `grep "compileSdkVersion\|targetSdkVersion" app/build.gradle`.
   Expected: every line in the output contains the value `35`; no line contains `29`.

2. **AC-2 — AlarmManagerDriver FLAG_IMMUTABLE.**
   Run `grep -n "FLAG_IMMUTABLE" app/src/main/java/com/zegoggles/smssync/service/AlarmManagerDriver.java`.
   Expected: one match at the `PendingIntent.getService(...)` construction site (currently line 127); the match shows `FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE` (or the reverse OR order).

3. **AC-3 — ServiceBase FLAG_IMMUTABLE.**
   Run `grep -n "FLAG_IMMUTABLE" app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java`.
   Expected: one match at the `PendingIntent.getActivity(...)` construction site (currently line 201–205 region); the match shows `FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE`.

4. **AC-4 — No bare PendingIntent construction.**
   Run `grep -rn "PendingIntent\.get" app/src/main/java/`.
   Expected: exactly two lines returned (one in `AlarmManagerDriver.java`, one in `ServiceBase.java`); both lines show a mutability flag (`FLAG_IMMUTABLE` or `FLAG_MUTABLE`) in their flags argument.

5. **AC-5 — AlarmManagerDriverTest assertion.**
   Run `grep -n "FLAG_IMMUTABLE\|FLAG_UPDATE_CURRENT" app/src/test/java/com/zegoggles/smssync/service/AlarmManagerDriverTest.java`.
   Expected: the assertion at line 97 shows the combined value `FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE`; the old single-token `FLAG_UPDATE_CURRENT` assertion no longer appears alone at that line.

6. **AC-6 — Co-land atomicity.**
   Inspect the PR commit history.
   Expected: no commit exists in the PR where `targetSdkVersion 35` is present in `app/build.gradle` and any of the following are absent: `FLAG_IMMUTABLE` in `AlarmManagerDriver.java:127`, `FLAG_IMMUTABLE` in `ServiceBase.java:204`, updated assertion in `AlarmManagerDriverTest.java:97`.

7. **AC-7 through AC-12 — Six-receiver exported values.**
   Open `app/src/main/AndroidManifest.xml`. Inspect each `<receiver>` element in order:
   - Line 131 `SmsBroadcastReceiver`: confirm `android:exported="false"`.
   - Line 139 `BootReceiver`: confirm `android:exported="false"`.
   - Line 145 `PackageReplacedReceiver`: confirm `android:exported="false"`.
   - Line 151 `BackupBroadcastReceiver`: confirm `android:exported="true"` and confirm `tools:ignore` attribute is absent from this element.
   - Line 159 `.compat.SmsReceiver`: confirm `android:exported="true"`.
   - Line 167 `.compat.MmsReceiver`: confirm `android:exported="true"`.

8. **AC-13 — No ExportedReceiver suppression.**
   Run `grep "ExportedReceiver" app/src/main/AndroidManifest.xml`.
   Expected: no output (zero matches).

9. **AC-14 — Lint ExportedReceiver.**
   Run `./gradlew lint` and open the HTML/XML lint report in `app/build/reports/lint-results*.xml`.
   Expected: zero issues of `id="ExportedReceiver"` referencing any of the six receiver class names.

10. **AC-15 — BACKUP broadcast contract (CNTR-005).**
    On an AVD or device running API 31+:
    a. Install the debug or release build.
    b. Open the app, navigate to Settings > Advanced Settings, enable "3rd party integration".
    c. In a terminal run: `adb shell am broadcast -a com.zegoggles.smssync.BACKUP com.zegoggles.smssync`
    d. Run: `adb logcat -s SmsSyncApplication | grep -i "backup requested"`
    Expected: logcat shows a line containing `"backup requested via broadcast intent"` (not the "...but ignored" variant). The broadcast is delivered (adb output shows `Broadcast completed: result=0`).
    Contract invariants confirmed: action string is `com.zegoggles.smssync.BACKUP`; package is `com.zegoggles.smssync`; no permission required on the sender side.

11. **AC-16 and AC-17 — Foreground service types.**
    In `app/src/main/AndroidManifest.xml`, locate the `<service>` elements for `SmsBackupService` and `SmsRestoreService` (currently lines 123–124 region).
    Expected: both elements carry the attribute `android:foregroundServiceType="dataSync"`.

12. **AC-18 — FOREGROUND_SERVICE_DATA_SYNC permission.**
    Run `grep "FOREGROUND_SERVICE_DATA_SYNC" app/src/main/AndroidManifest.xml`.
    Expected: one match showing `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>`.

13. **AC-19 — POST_NOTIFICATIONS permission.**
    Run `grep "POST_NOTIFICATIONS" app/src/main/AndroidManifest.xml`.
    Expected: one match showing `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>`.

14. **AC-20 — POST_NOTIFICATIONS runtime request on API 33+.**
    On an AVD running API 33 (Android 13) with `POST_NOTIFICATIONS` permission not yet granted:
    a. Install the build.
    b. Tap the backup button in the main activity.
    Expected: an Android permission dialog appears asking for notification access before any backup-progress notification is displayed. Grant the permission and confirm backup proceeds normally. Alternatively, run the relevant instrumentation test targeting API 33+ that verifies `ActivityCompat.requestPermissions` is called with `POST_NOTIFICATIONS` before the notification is posted.

15. **AC-21 — POST_NOTIFICATIONS suppressed on API 32.**
    On an AVD running API 32 (Android 12L):
    a. Install the build.
    b. Tap the backup button.
    Expected: no `POST_NOTIFICATIONS` dialog appears; backup proceeds and posts a progress notification without crash. No `RuntimeException` in logcat. Alternatively, run the relevant instrumentation test targeting API 32 confirming `requestPermissions` is NOT called for `POST_NOTIFICATIONS`.

16. **AC-22 — Lint ForegroundServiceType / MissingPermission.**
    Run `./gradlew lint` and inspect `app/build/reports/lint-results*.xml`.
    Expected: zero issues with `id="ForegroundServiceType"` or `id="MissingPermission"` related to notification posting.

17. **AC-23 — jcenter removed.**
    Run `grep -ri "jcenter" build.gradle app/build.gradle settings.gradle gradle/`.
    Expected: no output.

18. **AC-24 — jcenter.bintray.com removed.**
    Run `grep -r "jcenter.bintray.com" .`.
    Expected: no output.

19. **AC-25 — scijava mirror retained; firebase-jobdispatcher resolves.**
    Confirm `build.gradle` still contains the `maven { url "https://maven.scijava.org/..." }` entry.
    Then run: `./gradlew dependencies --refresh-dependencies` with a cleared Gradle cache (`rm -rf ~/.gradle/caches`).
    Expected: command completes with exit code 0 and no "Could not resolve" errors; `com.firebase:firebase-jobdispatcher:0.8.6` resolution is logged as resolved from the scijava mirror, not from any jcenter endpoint.

20. **AC-26 — assembleRelease clean build.**
    On a clean checkout, clear the Gradle cache (`rm -rf ~/.gradle/caches`) and run `./gradlew assembleRelease`.
    Expected: exit code 0; no "Could not resolve", "Manifest merger failed", or "Execution failed" errors in the build output.

21. **AC-27 — API 31+ Robolectric scheduling test.**
    Run `./gradlew test` (or the targeted test task for `AlarmManagerDriverTest` and the API-31+ scheduling test authored under U-005).
    Expected: all tests pass; the `AlarmManagerDriverTest` flag-assertion test passes with the updated expected value; no `IllegalArgumentException` is thrown by the scheduling path at the API-31+ SDK level.

22. **AC-28 — bundleRelease and aapt2 targetSdk check.**
    Run `./gradlew bundleRelease`.
    Expected: a non-empty AAB file is produced at `app/build/outputs/bundle/release/app-release.aab`.
    Then run: `aapt2 dump badging app/build/outputs/bundle/release/app-release.aab | grep targetSdkVersion`.
    Expected output line: `targetSdkVersion:'35'`.

23. **AC-29 — Play pre-launch review (Gate G0).**
    Upload the signed AAB from AC-28 to Google Play Console (internal testing track).
    Expected: the submission is accepted by the Play Console; no policy-rejection email or console error is received on the dimensions of `targetSdkVersion`, `android:exported`, foreground-service type, or `PendingIntent` mutability. The Play Console status shows the release as accepted for internal testing. This is the single terminal gate signal for Gate G0 / Milestone M0.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android | All changes in this story: SDK version raise, all six receiver manifest edits, FGS type declarations, permission declarations, FLAG_IMMUTABLE additions, POST_NOTIFICATIONS runtime request, AlarmManagerDriverTest update, JCenter removal | Developer |

## Technical Notes

**Co-requisite with U-005.** This story (U-003) and U-005 (Robolectric uplift to 4.12.x, mockito-core 5.x, junit 4.13.2) are a single atomic build sweep. They MUST land in the same PR and the same CI run. `app/build.gradle:66` pins Robolectric 4.3.1, which cannot compile or run against `compileSdkVersion 35`. The moment AC-1 sets `compileSdkVersion 35`, the existing test suite stops compiling unless U-005's Robolectric upgrade is simultaneously present. Neither story ships independently. AC-27 (scheduling test passing) is only achievable when both stories are merged.

**AlarmManagerDriver interim status.** `AlarmManagerDriver.java` is slated for full deletion by U-017 (WorkManager migration, MU-005). The `FLAG_IMMUTABLE` fix at line 127 (AC-2) is an explicit interim measure. It must remain in place and keep its test (AC-27) green until U-017 removes the file. Do not attempt to remove or refactor this class as part of this story.

**Six-receiver scope (audit-corrected).** REQ-MODERNIZATION-001 and the original design draft named only four receivers. The manifest contains six intent-filtered receivers; omitting the two `.compat.*` receivers would produce a manifest-merger error under AGP 7+ (already in place from U-001/U-002). All six must carry explicit `android:exported` or this story fails AC-26 (`assembleRelease` exit 0). This scope correction is formally acknowledged in DES-MODERNIZATION-001 §Manifest Conformance §Scope correction.

**BackupBroadcastReceiver export value is non-negotiable.** Setting `android:exported="false"` on `BackupBroadcastReceiver` compiles cleanly and passes lint but silently deletes the public `com.zegoggles.smssync.BACKUP` API that Tasker users and automation scripts depend on (CNTR-MODERNIZATION-005). The value MUST be `true`. AC-10 and AC-15 together form the two-part verification: static inspection confirms the attribute, and the ADB broadcast test confirms the contract is live.

**FLAG_IMMUTABLE timing constraint.** The fix is a no-op at `targetSdk 29` (no reason to pre-land it in an earlier stage). At `targetSdk 35` on an API 31+ device, the absence of either `FLAG_IMMUTABLE` or `FLAG_MUTABLE` throws `IllegalArgumentException` at `PendingIntent` construction time on the scheduling path — a hard crash, not a warning. AC-6 enforces that no intermediate commit state exposes this condition.

**POST_NOTIFICATIONS version gate.** Use `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU` (API 33) as the guard. This is identical to the existing idiom at `ServiceBase.java:208`. The call site for the runtime request is the user-initiated backup/restore entry point in `MainActivity`, before `SmsBackupService` posts its first progress notification via `ServiceBase.createNotification` / `getNotifier` (verified at `ServiceBase.java:174,187`). Do not request the permission from inside the service; request it from the activity before starting the service.

**jcenter removal scope.** Remove exactly three lines/blocks from `build.gradle`: `jcenter()` in the buildscript repositories block (line 4), `jcenter()` in the allprojects repositories block (line 21), and `maven { url "https://jcenter.bintray.com" }` (line 25). Do NOT remove the `maven { url "https://maven.scijava.org/..." }` entry at lines 26–27; `firebase-jobdispatcher:0.8.6` resolves only from that mirror and will break the build if removed (AC-25). The scijava mirror is removed by U-017 when firebase-jobdispatcher itself is replaced.

**warningsAsErrors baseline.** The `lint-baseline.xml` committed at S1 (U-001) governs the pre-existing warning set under `warningsAsErrors true` (app/build.gradle:41) and `-Werror -Xlint:deprecation` (app/build.gradle:75). For this story, only remove entries from the baseline, never add new suppressions. If raising `targetSdk` surfaces new deprecation warnings (e.g., `getNetworkInfo`, `getAllNetworks`), the pre-existing suppressions at `ServiceBase.java:215,225` should already cover them; confirm that no new `-Werror` failure blocks the build. Full empty-baseline restoration is a requirement closure condition for REQ-MODERNIZATION-001.

**Estimation.** High complexity. The story touches six files across build configuration, manifest, three Java source files, and one test file, with hard timing constraints (atomic commit), a co-requisite story (U-005), and a terminal gate check (Play Console submission). Allow at least two developer-days for the S3 commit, CI green, AVD smoke, and Gate G0 submission.

## Supporting Documentation

- REQ-MODERNIZATION-001 §SDK and Build Toolchain Uplift (AC-1)
- REQ-MODERNIZATION-001 §JCenter Repository Declarations Removed (AC-7/8/9 of the REQ, which map to AC-23/24/25 here)
- REQ-MODERNIZATION-001 §FLAG_IMMUTABLE Co-Landed on Both PendingIntent Sites (AC-10/11/12/13/14 of the REQ)
- REQ-MODERNIZATION-001 §android:exported Declared on All Six Manifest Receivers (AC-15..20 of the REQ)
- REQ-MODERNIZATION-001 §FOREGROUND_SERVICE_DATA_SYNC Type and POST_NOTIFICATIONS Permission (AC-21..27 of the REQ)
- REQ-MODERNIZATION-001 §Signed AAB at targetSdk 35 Passes Play Pre-Launch Review (AC-28/29/30 of the REQ)
- DES-MODERNIZATION-001 §Staged Toolchain Uplift (Stage S3 definition)
- DES-MODERNIZATION-001 §API-31+ PendingIntent Mutability (FLAG_IMMUTABLE co-land, AlarmManagerDriverTest:97 coupling)
- DES-MODERNIZATION-001 §Manifest Conformance (six-receiver table with export rationale, FGS type, POST_NOTIFICATIONS)
- DES-MODERNIZATION-001 §JCenter / Bintray Repository Removal (scijava caveat)
- DES-MODERNIZATION-001 §Robolectric Co-Requisite Coupling (U-005 dependency)
- DES-MODERNIZATION-001 §Risks (over-removal of scijava, AlarmManagerDriverTest coupling, API 33+ gate)

## Integration Contract References

- CNTR-MODERNIZATION-005 §Contract Definition — action string `com.zegoggles.smssync.BACKUP`, `android:exported="true"`, no `android:permission`, user gate (AC-10 and AC-15 jointly satisfy Validation Rule 2)
- CNTR-MODERNIZATION-005 §Validation Rules — rules 1 (action string frozen), 2 (exported=true required), 3 (no sender permission), 4 (user gate preserved); all four rules verified by AC-10, AC-13, AC-15
- CNTR-MODERNIZATION-005 §Versioning §Breaking change policy — setting exported=false is explicitly listed as a prohibited breaking change; AC-10 (`exported="true"`) and AC-15 (live ADB test) together form the two-part enforcement

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

One-line: Stage S3 — raise compile/targetSdk 29 to 35 with co-landed FLAG_IMMUTABLE on both PendingIntent sites and updated AlarmManagerDriverTest:97; set explicit android:exported on all six intent-filtered receivers (SmsBroadcastReceiver/BootReceiver/PackageReplacedReceiver=false, BackupBroadcastReceiver/compat.SmsReceiver/compat.MmsReceiver=true) with tools:ignore removed; add FOREGROUND_SERVICE_DATA_SYNC type and POST_NOTIFICATIONS runtime request (API 33+ gated); remove jcenter()/bintray (retain scijava mirror); produce signed AAB verified by aapt2 and submitted to Play internal track (Gate G0) — co-requisite with U-005 in the same PR.
