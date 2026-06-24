---
type: bug
status: done
artifact_type: bug
severity: high
priority: high
complexity: low
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts:
  - CNTR-MODERNIZATION-005
dependencies: []
related_items:
  - U-049
platforms: [android]
tags: [foreground-service, workmanager, targetsdk-35, regression]
id: BUG-016
title: WorkManager SystemForegroundService missing foregroundServiceType — backup/restore worker crash-loops on targetSdk 34+
domain: build
origin: on-device-validation
resolved_by: U-049
sprint: '000016'
---

# BUG-016: WorkManager SystemForegroundService missing foregroundServiceType — backup/restore worker crash-loops on targetSdk 34+

## Description
U-049 (sprint-012) retired the app's own `SmsBackupService`/`SmsRestoreService` and moved the
foreground notification into `BackupWorker`/`RestoreWorker` via
`setForeground(ForegroundInfo(id, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC))`. On API 31+
the actual `Service.startForeground(notification, type)` call is made by WorkManager's **own**
`androidx.work.impl.foreground.SystemForegroundService`, not by any app service. On targetSdk 34+
the runtime `foregroundServiceType` passed to `startForeground` **must be a subset of the
`android:foregroundServiceType` declared on the `<service>` manifest element**. The WorkManager
library declares `SystemForegroundService` with **no** `foregroundServiceType` (0x0), so passing
`dataSync` (0x1) throws `IllegalArgumentException: foregroundServiceType 0x00000001 is not a subset
of foregroundServiceType attribute 0x00000000 in service element of manifest file`. The process
crashes; ActivityManager restarts the foreground service, which crashes again — a crash-loop until
"crashed too many times, killing!". The backup/restore never runs.

The deleted app services *did* declare the type in the manifest; the U-049 manifest comment
incorrectly assumed WorkManager "handles the `android:foregroundServiceType="dataSync"` permission
automatically." It does not — the app must override the library's service element.

This was invisible to JVM unit tests and the GreenMail IMAP integration tests (neither exercises
the real Android foreground-service manifest-subset check). It only reproduces on-device on
API 34+. Found while driving on-device validation of the post-migration build (HEAD e29d00b2)
via the CNTR-MODERNIZATION-005 broadcast trigger path.

## Steps to Reproduce
1. On an API 34+ device/emulator (targetSdk 35 build), configure an account and enable
   `third_party_integration`.
2. Trigger a backup so the worker goes to the foreground:
   `adb shell am broadcast -a com.zegoggles.smssync.BACKUP -p com.zegoggles.smssync`
   (the in-app BACKUP button takes the same `setForeground` path).
3. Observe the worker start then immediately crash.

## Expected Behavior
`BackupWorker`/`RestoreWorker` enter the foreground with a `dataSync`-typed notification and proceed
to read messages and connect to IMAP. No crash.

## Actual Behavior
```
D SMSBackup+: BackupWorker.doWork: starting backup, type=BROADCAST_INTENT, attempt=0
I WM-SystemFgDispatcher: Started foreground service ... SystemForegroundService
E AndroidRuntime: java.lang.IllegalArgumentException: foregroundServiceType 0x00000001 is not a
    subset of foregroundServiceType attribute 0x00000000 in service element of manifest file
    at androidx.work.impl.foreground.SystemForegroundService$Api31Impl.startForeground(SystemForegroundService.java:193)
W ActivityManager: Process com.zegoggles.smssync has crashed too many times, killing!
```

## Evidence
- On-device logcat (emulator-5554, API 35, build e29d00b2), 2026-06-24: crash-loop captured at the
  `SystemForegroundService.startForeground` call; worker never reaches `fetchAndBackupItems`.
- Merged manifest before fix:
  `app/build/intermediates/merged_manifest/debug/.../AndroidManifest.xml` —
  `<service android:name="androidx.work.impl.foreground.SystemForegroundService" ... />` with **no**
  `android:foregroundServiceType`.
- `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt:494` /
  `RestoreWorker.kt:708` — `ForegroundInfo(id, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)`
  (U-049 fix; correct, but ineffective without the manifest type on the service that actually calls
  `startForeground`).
- `app/src/main/AndroidManifest.xml:131-135` (pre-fix comment) — incorrect "WorkManager ... handles
  ... automatically" assumption.

## Fix
Override WorkManager's service element in `app/src/main/AndroidManifest.xml` to declare the type:
```xml
<service
    android:name="androidx.work.impl.foreground.SystemForegroundService"
    android:foregroundServiceType="dataSync"
    tools:replace="android:foregroundServiceType" />
```
(`FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_DATA_SYNC` permissions were already present from U-049.)

## Acceptance Criteria
- [x] AC-1: Merged manifest's `SystemForegroundService` element declares
  `android:foregroundServiceType="dataSync"`.
- [x] AC-2: On API 34+, triggering a backup no longer throws `IllegalArgumentException` at
  `startForeground`; the worker proceeds past the foreground handoff into message fetch + IMAP
  connect. (Validated on-device: worker read 3 SMS and reached the IMAP `LOGIN` stage.)
- [x] AC-3: Build green (assembleDebug, testDebugUnitTest, jacoco ≥70%).
- [ ] AC-4: A successful live append to the IMAP `SMS` folder — blocked only by the test Gmail
  account's app-password being rejected at `LOGIN` (real-account credential matter, not code);
  the entire code path up to and including the real TLS handshake to `imap.gmail.com:993` is
  validated.

## Root Cause Analysis
U-049 migrated the foreground service ownership from app-declared services to the WorkManager
library service but did not migrate the manifest `foregroundServiceType` declaration along with it.
The U-049 code review caught the related `ForegroundInfo` 2-arg→3-arg defect but neither the review
nor QA caught the manifest-side half, because the failure mode is only observable at runtime on a
real API 34+ Android foreground-service start — not reachable from JVM/Robolectric unit tests or the
GreenMail IMAP integration tests. Process insight for the retro: foreground-service-type correctness
on targetSdk 34+ requires on-device (or instrumented) validation as an explicit gate when foreground
ownership changes.
