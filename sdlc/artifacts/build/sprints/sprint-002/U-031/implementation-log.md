---
artifact_type: implementation-log
story_id: "U-031"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-04"
files_changed: 7
files_created: 2
tests_added: 18
tests_passing: true
---

# Implementation Log: U-031 — Remove Legacy AsyncTask Execution Path

## Summary

U-031 removes the legacy `AsyncTask`-based execution path from `SmsBackupService` and `SmsRestoreService`, replacing it with WorkManager dispatch via `scheduleManual()` and `scheduleRestore()`. A worker→foreground signalling bridge is built (DES-MODERNIZATION-012 §Integration Design Option b1) that observes WorkInfo LiveData and drives the existing `backupStateChanged()`/`restoreStateChanged()` foreground/stop drivers. Cancel is rewired through a new `WorkManagerCancelCollector`. All legacy `AsyncTask` classes and tests are deleted. The 70% JaCoCo LINE coverage gate passes.

## Contract Adherence

**CNTR-MODERNIZATION-004 v2 — BackupScheduler port (Validation Rule 8)**

- `BackupScheduler.java` javadoc updated "nine operations" → "ten operations" (file:line 29, verified grep-zero for "nine operations")
- `scheduleManual(BackupType backupType)` added to `BackupScheduler.java` with CNTR-004 v2 Validation Rule 8 javadoc (file:line 106–128)
- `WorkManagerScheduler.scheduleManual()` uses `addTag(backupType.name)`, `ExistingWorkPolicy.REPLACE`, `Constraints.NONE`, `BackoffPolicy.EXPONENTIAL`/30s/300s, unique-work name = `backupType.name` (scheduler/WorkManagerScheduler.kt:299–315)
- Only `BackupType.MANUAL` or `BackupType.SKIP` accepted; `require()` guard for contract violation (scheduler/WorkManagerScheduler.kt:301–303)
- `SmsBackupService.backup()` calls `getScheduler().scheduleManual(backupType)` (service/SmsBackupService.java:169)
- `cancel()` extended to handle `BackupType.MANUAL` and `BackupType.SKIP` (scheduler/WorkManagerScheduler.kt)

## CalendarSyncer @Inject

Per story line 208: CalendarSyncer `@Inject` annotation NOT removed in this story. Assigned to U-032. Verified: `CalendarSyncer.java` unchanged.

## FULL_WAKE_LOCK Decision (AC-9 Option b)

`SmsRestoreService` acquires `FULL_WAKE_LOCK` via `acquireLocks()` before `scheduleRestore()` enqueue (service/SmsRestoreService.java:131) and releases via `releaseLocks()` in `restoreStateChanged()` terminal branch (service/SmsRestoreService.java:346). This preserves screen-on behavior during restore while honoring EPIC SC-10 (no behavior change to the user).

## Files Modified

| File | Change | Reason |
|------|--------|--------|
| `app/src/main/java/.../scheduler/BackupScheduler.java` | Added `scheduleManual(BackupType)` javadoc + method declaration; updated count nine→ten | CNTR-MODERNIZATION-004 v2 Validation Rule 8 |
| `app/src/main/java/.../scheduler/WorkManagerScheduler.kt` | Added `scheduleManual()` implementation; updated `cancel()` for MANUAL/SKIP | BackupScheduler port implementation |
| `app/src/main/java/.../service/SmsBackupService.java` | Full rewrite: WorkInfo observer bridge, scheduleManual dispatch, cancel collector, deleted getBackupTask(), removed MailException catch from backup() | AC-1, AC-2, AC-3, AC-5, AC-7 |
| `app/src/main/java/.../service/SmsRestoreService.java` | Full rewrite: WorkInfo observer bridge, scheduleRestore dispatch, FULL_WAKE_LOCK, cancel collector, deleted getRestoreTask(), canWriteToSmsProvider() protected, added getScheduler() | AC-1, AC-2, AC-4, AC-5, AC-9 |
| `app/build.gradle` | Removed 4 jacocoFileFilter lines for BackupTask/RestoreTask (lines 229–232) | AC-10 — classes deleted |
| `app/src/test/java/.../service/SmsBackupServiceTest.java` | Removed @Mock BackupTask; removed getBackupTask() override; updated assertions to verify scheduler.scheduleManual(); added SKIP type test; fixed shouldCheckForValidStore/shouldNotifyUserAboutErrorInManualMode | AC tests — BackupTask deleted |
| `app/src/test/java/.../service/SmsRestoreServiceTest.java` | Major expansion: anonymous subclass pattern, handleIntent tests, state transition tests, WorkManagerTestInitHelper | AC tests — RestoreTask deleted, coverage gate |

## Files Created

| File | Reason |
|------|--------|
| `app/src/main/java/.../service/WorkManagerCancelCollector.kt` | Replaces BackupCancelCollector.kt; routes SyncEvent.Cancel to WorkManager.cancelUniqueWork() (R-4 mitigation, AC-5) |
| `sdlc/artifacts/build/sprints/sprint-002/U-031/plan.md` | Implementation plan (SDLC artifact) |

## Files Deleted

| File | Reason |
|------|--------|
| `app/src/main/java/.../service/BackupCancelCollector.kt` | Replaced by WorkManagerCancelCollector.kt; called task.onCancelRequested() — no longer valid |
| `app/src/main/java/.../service/BackupTask.java` | Legacy AsyncTask execution class — all invocations removed (AC-6, AC-7) |
| `app/src/main/java/.../service/RestoreTask.java` | Legacy AsyncTask execution class — all invocations removed (AC-6, AC-7) |
| `app/src/test/java/.../service/BackupTaskTest.java` | Tests for deleted class |
| `app/src/test/java/.../service/RestoreTaskTest.java` | Tests for deleted class |

## Capabilities Inventory (Replacement Story)

The following capabilities from the original AsyncTask execution path are accounted for:

### BackupTask.java
- RETAINED: BackupConfig construction — moved into BackupWorker (pre-existing, unchanged)
- RETAINED: IMAP backup execution — BackupWorker (pre-existing, unchanged)
- RETAINED: Progress reporting → backupStateChanged() — REPLACED by WorkInfo observer bridge in SmsBackupService.registerBackupWorkInfoObserver() (SmsBackupService.java:209–242)
- RETAINED: Login state → backupStateChanged(LOGIN) — mapped from STATE_LOGIN in mapProgressStateToSmsSyncState() (SmsBackupService.java:282)
- RETAINED: Calc state → backupStateChanged(CALC) — mapped from STATE_CALC (SmsBackupService.java:283)
- RETAINED: Backup state → backupStateChanged(BACKUP) — mapped from STATE_BACKUP (SmsBackupService.java:284)
- RETAINED: Finished state — mapped from SUCCEEDED WorkInfo.State (SmsBackupService.java:255–256)
- RETAINED: Error state — mapped from FAILED WorkInfo.State (SmsBackupService.java:257–260)
- RETAINED: Cancel state — mapped from CANCELLED WorkInfo.State (SmsBackupService.java:260–261)
- INTENTIONALLY REMOVED: BackupCancelCollector calling task.onCancelRequested() — REPLACED by WorkManagerCancelCollector routing to WorkManager.cancelUniqueWork() (AC-5)

### RestoreTask.java
- RETAINED: RestoreConfig construction — RestoreWorker builds its own config from inputData (pre-existing)
- RETAINED: IMAP restore execution — RestoreWorker (pre-existing, unchanged)
- RETAINED: Progress → restoreStateChanged() — REPLACED by WorkInfo observer bridge in SmsRestoreService.registerRestoreWorkInfoObserver() (SmsRestoreService.java:175–213)
- RETAINED: LOGIN/CALC/RESTORE/FINISHED/CANCELED state mapping — mapProgressStateToSmsSyncState() (SmsRestoreService.java:245–252)
- RETAINED: Terminal state handling — SUCCEEDED→FINISHED_RESTORE, FAILED→ERROR, CANCELLED→CANCELED_RESTORE (SmsRestoreService.java:215–231)
- INTENTIONALLY REMOVED: RestoreTask.getMailTransport() call in SmsRestoreService.handleIntent() — RestoreWorker builds its own transport internally (AC-2)

## Integration Path

- New code in `SmsBackupService.backup()` is called from `handleIntent()` which is called from Android Service dispatch
- `registerBackupWorkInfoObserver()` registers a `WorkManager.getWorkInfosForUniqueWorkLiveData()` observer that drives `backupStateChanged()`
- `WorkManagerCancelCollector.collect()` is called from both services and connects `App.syncStateRepository().events` to `WorkManager.cancelUniqueWork()`
- `scheduleManual()` in `WorkManagerScheduler` is called from `SmsBackupService.backup()` which dispatches `BackupWorker`
- `scheduleRestore()` in `WorkManagerScheduler` is called from `SmsRestoreService.handleIntent()` which dispatches `RestoreWorker`

## Test Results

- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL
- `./gradlew :app:testDebugUnitTest` — BUILD SUCCESSFUL, zero failures
- `./gradlew :app:jacocoTestCoverageVerification` — BUILD SUCCESSFUL (service* ≥70% LINE)
- New tests added: 18 (SmsBackupServiceTest: +7, SmsRestoreServiceTest: +11)
- Deleted tests: BackupTaskTest.java (estimated 20+ tests), RestoreTaskTest.java (estimated 8+ tests) — deleted with their source classes

## Verification Grep-Zero Checks

All verified zero:
- `grep -rn "new BackupTask|new RestoreTask|getBackupTask|getRestoreTask" app/src/main/java/` — zero functional invocations (only comments)
- `grep -rn "import.*BackupTask|import.*RestoreTask" app/src/main/java/` — zero
- `find app/src -name "BackupCancelCollector.kt"` — zero
- `find app/src -name "BackupTask.java" -o -name "RestoreTask.java"` — zero
- `find app/src/test -name "BackupTaskTest*" -o -name "RestoreTaskTest*"` — zero
- `grep -n "BackupTask.class|RestoreTask.class" app/build.gradle` — zero
- `grep -n "nine operations" .../BackupScheduler.java` — zero

## Regression Results

No regressions. All 581+ tests pass. Coverage gate passes at ≥70% across service*, mail*, auth* packages.

## Notes

- `getMailTransport()` method in `ServiceBase` — NOT removed per story scope (only `SmsBackupService.getBackupConfig()` was removed; the factory seam `getMailTransport()` is still used by the `K9MailTransport` adapter created internally for non-service code paths)
- `SmsBackupService.getMailTransport()` method in `ServiceBase` retained — `SmsRestoreService.handleIntent()` no longer calls it, but the method is part of `ServiceBase` infrastructure
- `canWriteToSmsProvider()` in `SmsRestoreService` changed from `private` to `protected` to allow test subclasses to override without PackageManager initialization
- `getScheduler()` added to `SmsRestoreService` (mirrors `SmsBackupService.getScheduler()`) — will be replaced by Hilt `@Inject` in U-032
- WorkManager re-initialization warnings in test output from `WorkManagerTestInitHelper.initializeTestWorkManager()` called per-test-class `@Before` are benign — Robolectric resets context between test classes
- `extends AsyncTask` in `PinCertificateEnrollmentFlow.java` and `OAuth2CallbackTask.java` are out of scope for this story and are NOT removed
