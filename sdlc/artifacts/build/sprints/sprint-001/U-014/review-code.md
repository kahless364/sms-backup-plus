---
artifact_type: review-code
story_id: "U-014"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-03"
blockers: 0
warnings: 2
---

# Code Review: U-014 — WorkManagerScheduler Adapter

## Overall Verdict: PASS

The implementation correctly introduces `WorkManagerScheduler` as the second `BackupScheduler`
adapter alongside `LegacyScheduler`, preserving all four invariants and both integration
contracts (CNTR-MODERNIZATION-004, CNTR-MODERNIZATION-005). No blockers found.

## Checklist

### Interface Compliance

- [x] `WorkManagerScheduler` implements every method of `BackupScheduler` (9 methods)
- [x] No Android scheduler types in the port signature (WorkManager types are in the adapter only)
- [x] `ScheduledJob` return type used correctly (no Firebase or WorkManager types cross the port)
- [x] `RestoreSchedulerConfig` parameter type used correctly in `scheduleRestore`

### INV-1 — REPLACE semantics (single-flight)

- [x] One-off work: `ExistingWorkPolicy.REPLACE` for INCOMING and BROADCAST_INTENT
- [x] Periodic work: `ExistingPeriodicWorkPolicy.UPDATE` for REGULAR (equivalent to setReplaceCurrent=true)
- [x] CONTENT_TRIGGER: `ExistingPeriodicWorkPolicy.UPDATE`
- [x] Stable unique names: `BackupType.name()` for REGULAR/INCOMING/BROADCAST_INTENT; `CONTENT_TRIGGER_UNIQUE_NAME = "CONTENT_TRIGGER"` for content trigger
- [x] Test: 3 INV-1 test methods pass

### INV-2 — Network constraints

- [x] BROADCAST_INTENT → `Constraints.NONE` (not CONNECTED — critical for offline Tasker triggers)
- [x] Regular/Incoming with `isWifiOnly=true` → `NetworkType.UNMETERED`
- [x] Regular/Incoming with `isWifiOnly=false` → `NetworkType.CONNECTED`
- [x] `requiresCharging` never set (no charging constraint anywhere)
- [x] Test: 4 INV-2 test methods pass

### INV-3 — Backoff fidelity

- [x] `BackoffPolicy.EXPONENTIAL` used on all non-immediate job types
- [x] Initial delay = 30s (`BACKOFF_INITIAL_SECS = 30L`)
- [x] 300s cap documented and deferred to U-015 worker layer (correct — WorkManager has no cap param)
- [x] `BACKOFF_MAX_SECS = 300L` constant present for U-015 reference
- [x] Test: 3 INV-3 tests pass (2 constant assertions + 1 acceptance test)

### INV-4 — Content-URI trigger

- [x] API 24+ path: `addContentUriTrigger(SMS_PROVIDER, true)` with `isTriggeredForDescendants=true`
- [x] Call-log URI added conditionally when `isCallLogBackupAfterCallEnabled() && isBackupEnabled(CALLLOG)`
- [x] Pre-24 path: returns null; broadcast fallback (SmsBroadcastReceiver) unchanged
- [x] Two-stage debounce: `BackupTriggerWorker` enqueues delayed `BackupWorker` (not direct backup)
- [x] `BackupTriggerWorker` uses `ExistingWorkPolicy.REPLACE` for follow-up enqueue
- [x] Test: 4 INV-4 tests pass (3 API-29 + 1 pre-24 via @Config(sdk=[23]))

### CNTR-MODERNIZATION-005 — Broadcast contract

- [x] `BackupBroadcastReceiver.BACKUP_ACTION = "com.zegoggles.smssync.BACKUP"` unchanged
- [x] `android:exported="true"` in `AndroidManifest.xml:168`
- [x] `<action android:name="com.zegoggles.smssync.BACKUP"/>` unchanged
- [x] `isAllow3rdPartyIntegration()` gate preserved
- [x] `getScheduler(context).scheduleImmediate()` called on matching broadcast

### Production Binding

- [x] `LegacyScheduler` remains the production binding in `App.onCreate` (no flip)
- [x] `WorkManagerScheduler` not yet connected to production DI (U-017)
- [x] `CompositeScheduler` in `debug` source set only (absent from release builds)

### Code Quality

- [x] Kotlin files follow project conventions (Apache 2.0 header, package, imports)
- [x] No hardcoded credentials or secrets
- [x] Meaningful log messages with TAG = "SMSBackup+"
- [x] Split-responsibility (INV-3 cap) documented in class KDoc and BackupWorker
- [x] U-024 ordering constraint documented in WorkManagerScheduler KDoc and BackupWorker KDoc
- [x] `BOOT_BACKUP_DELAY_SECS = 60L` matches `BackupJobs.java:56`
- [x] `PERIODIC_MIN_INTERVAL_SECS = 15 minutes` handles WorkManager minimum interval constraint

## Warnings (Non-blocking)

### Warning 1: Deprecated `RuntimeEnvironment.application` in test

`WorkManagerSchedulerTest.kt:54` uses `RuntimeEnvironment.application` which is deprecated
in newer Robolectric. The project uses `RuntimeEnvironment.application` consistently across all
tests (matches existing pattern in BackupJobsTest, LegacySchedulerTest, etc.). Not a blocker;
migration to `ApplicationProvider.getApplicationContext()` deferred to a future cleanup.

### Warning 2: PeriodicWorkRequest minimum interval fallback

`scheduleRegular()` silently uses 15 minutes as a minimum if `regularTimeoutSecs < 900`.
This behavior is not logged at a visible level. Consider logging a warning if the fallback
is applied; however, the legacy scheduler had no such minimum (Firebase JobDispatcher allowed
sub-15-minute periods). This is a behavioral difference that should be noted in U-017 documentation.

## Files Reviewed

| File | Status |
|------|--------|
| `app/build.gradle` | OK — WorkManager 2.9.1 added correctly |
| `app/src/main/.../scheduler/WorkManagerScheduler.kt` | OK |
| `app/src/main/.../worker/BackupWorker.kt` | OK — stub with clear U-015 note |
| `app/src/main/.../worker/BackupTriggerWorker.kt` | OK — two-stage debounce correct |
| `app/src/debug/.../scheduler/CompositeScheduler.kt` | OK — debug-only, correct delegation |
| `app/src/test/.../service/WorkManagerSchedulerTest.kt` | OK — 20 tests, all pass |
