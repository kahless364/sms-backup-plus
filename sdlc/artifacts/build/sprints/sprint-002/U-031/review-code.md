---
artifact_type: code-review
story_id: "U-031"
verdict: "PASS"
reviewer: "Developer (self-review)"
timestamp: "2026-06-04"
---

# Code Review: U-031 — Remove Legacy AsyncTask Execution Path

## Summary

Self-review of the U-031 implementation covering: WorkInfo bridge pattern, scheduleManual dispatch, cancel rewire, deletion of legacy classes, and test updates.

## Review Findings

### PASS — Worker→Foreground Bridge (AC-1)

- `registerBackupWorkInfoObserver()` and `registerRestoreWorkInfoObserver()` use `observeForever()` with manual cleanup on terminal state — correct pattern for Service (not a LifecycleOwner)
- Observer removed both in `onChanged()` on terminal state AND in `onDestroy()` via `tearDownObserverAndCollector()` — no leak
- `mapWorkInfoToBackupState()` and `mapWorkInfoToRestoreState()` are package-visible for testability — good
- Bridge maps all 5 state constants from each worker: LOGIN, CALC, BACKUP/RESTORE, FINISHED, CANCELED — complete

### PASS — scheduleManual Dispatch (AC-2, AC-3)

- `BackupScheduler.scheduleManual()` port method added with CNTR-MODERNIZATION-004 v2 compliant javadoc
- `WorkManagerScheduler.scheduleManual()` enforces MANUAL/SKIP-only via `require()` — contract violation caught early
- Unique-work name = `backupType.name` ("MANUAL" or "SKIP") — distinct from "BROADCAST_INTENT" (no cross-collision)
- `addTag(backupType.name)` present — required for `BackupWorker.inferBackupType()` to resolve correctly
- Pre-flight checks (permissions, credentials, enabled types) retained verbatim in `backup()`

### PASS — Restore Dispatch (AC-4)

- `scheduleRestore(new RestoreSchedulerConfig(RESTORE_WORK_NAME, RESTORE_WORK_NAME))` — correct per AC-4
- `canWriteToSmsProvider()` guard preserved verbatim
- `SmsProviderNotWritableException` path preserved

### PASS — Cancel Rewire (AC-5)

- `WorkManagerCancelCollector` collects `SyncEvent.Cancel` from `SyncStateRepository.events` (SharedFlow) via `filterIsInstance<SyncEvent.Cancel>().first()` — collects first cancel and stops
- Calls `WorkManager.cancelUniqueWork(uniqueWorkName)` — worker's `ensureActive()` cooperative cancellation fires at next suspension point
- Cancel collector `Job` held as field; cancelled in `tearDownObserverAndCollector()` — no coroutine leak
- `BackupCancelCollector.kt` deleted — the old path that called `task.onCancelRequested()` is gone

### PASS — Deletions (AC-6, AC-7)

- `BackupTask.java`, `RestoreTask.java`, `BackupCancelCollector.kt` deleted
- `BackupTaskTest.java`, `RestoreTaskTest.java` deleted with their source classes
- `getBackupTask()` factory deleted from `SmsBackupService`
- `getRestoreTask()` factory deleted from `SmsRestoreService`
- All grep-zero checks verified

### PASS — FULL_WAKE_LOCK (AC-9)

- `acquireLocks()` called before `scheduleRestore()` in `handleIntent()`
- `releaseLocks()` called in `restoreStateChanged()` terminal branch — paired with acquire
- Edge case: if `scheduleRestore()` returns null, `releaseLocks()` is called before `postError()` — no lock leak

### PASS — CalendarSyncer @Inject (story constraint)

- `CalendarSyncer.java` NOT modified — `@Inject` annotation retained for U-032

### PASS — Worker Code Unchanged (EPIC SC-10)

- `BackupWorker.kt` and `RestoreWorker.kt` NOT modified — behavior unchanged
- Service-side bridge observes worker output but doesn't change worker logic

### MINOR — canWriteToSmsProvider() visibility change

- Changed from `private` to `protected` to enable test subclass override. This is a minimal test seam that doesn't expose internal logic inappropriately. Mirrors the `SmsBackupService.getBackupTask()` pattern already in the codebase.

### MINOR — Stale `getMailTransport()` import

- `SmsRestoreService` no longer calls `getMailTransport()` in `handleIntent()`. However, the method is defined in `ServiceBase` and is still used by `SmsBackupService` (via the backup worker path). The import/reference in `ServiceBase` is retained intentionally per story scope. No action needed in this story.

### MINOR — Test count note

- `shouldCheckForValidStore` test behavior correctly updated: the IMAP URI is no longer validated in the service (it's validated inside `BackupWorker`). The test now verifies `scheduleManual(MANUAL)` is called, which is the correct assertion for the new dispatch path.

## No Issues Blocking

All critical requirements met. Implementation consistent with DES-MODERNIZATION-012 §Integration Design Option b1.
