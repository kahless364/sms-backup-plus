---
type: plan
story: U-031
status: complete
verdict: PASS
---

# U-031 Implementation Plan: Remove Legacy AsyncTask Execution Path

## Ordered Execution (binding per story AC ordering)

### Step 2a — Worker→Foreground Signalling Bridge (AC-1, BLOCKING PREREQUISITE)

Ground truth (re-verified against source):
- `backupStateChanged()` in `SmsBackupService` drives `startForeground`/`stopForeground`/`stopSelf`
- It is called today ONLY by direct call from `BackupTask.java:273` (`service.backupStateChanged(state)`)
- Workers emit ONLY via `setProgress(workDataOf(...))` — they never call `emitState()`
- No service-side WorkInfo/StateFlow collector exists

Design decision (DES-MODERNIZATION-012 §Integration Design Option b1):
- After enqueuing the worker, `SmsBackupService.backup()` registers a `WorkManager.getInstance(ctx).getWorkInfoByIdLiveData(workId)` observer on the main thread
- Observer maps `WorkInfo.progress` (`PROGRESS_KEY_STATE`) and terminal `WorkInfo.State` to `BackupState`/`RestoreState`
- Calls the existing `backupStateChanged()` / `restoreStateChanged()` driver (which handles `startForeground`, `stopForeground(true)`, `stopSelf()`)
- Observer is torn down via removal after terminal state is reached
- Since Services run on the main thread, the LiveData observer uses `Service` as a `LifecycleOwner` (Services don't implement LifecycleOwner) — we observe with `observeForever` and remove manually on terminal
- For `BackupWorker`: maps STATE_LOGIN/STATE_CALC/STATE_BACKUP → `SmsSyncState.LOGIN/CALC/BACKUP`; FINISHED_BACKUP → `FINISHED_BACKUP`; CANCELLED WorkInfo.State → `CANCELED_BACKUP`; FAILED → `ERROR`
- For `RestoreWorker`: maps STATE_LOGIN/STATE_CALC/STATE_RESTORE → respective states; FINISHED_RESTORE → `FINISHED_RESTORE`; CANCELLED → `CANCELED_RESTORE`

### Step 2b — scheduleManual port addition + dispatch migration (AC-2, AC-3, AC-4)

1. Add `scheduleManual(BackupType backupType)` to `BackupScheduler.java` with javadoc, update count to "ten"
2. Implement in `WorkManagerScheduler.kt`: `Constraints.NONE`, `ExistingWorkPolicy.REPLACE`, `EXPONENTIAL`/30s, unique-work name = `backupType.name()`, `addTag(backupType.name())`
3. `SmsBackupService.backup()`: replace `getBackupTask().execute(...)` with `getScheduler().scheduleManual(backupType)`; keep all pre-flight checks
4. `SmsRestoreService.handleIntent()`: replace `getRestoreTask().execute(config)` with `getScheduler().scheduleRestore(new RestoreSchedulerConfig(RESTORE_WORK_NAME, RESTORE_WORK_NAME))`; keep guards

### Step 2c — Deletions + Cancel rewire + Cleanup (AC-5 through AC-10)

5. Cancel rewire: `SyncEvent.Cancel(USER)` must reach `WorkManager.cancelUniqueWork(...)` — implement in the services themselves (collect from `App.syncStateRepository().events` and cancel the unique-work name)
6. Delete `BackupCancelCollector.kt`
7. Delete factory methods `getBackupTask()` and `getRestoreTask()` 
8. Verify grep-zero then delete `BackupTask.java`, `RestoreTask.java`, `BackupTaskTest.java`, `RestoreTaskTest.java`
9. Remove `@Inject` from `CalendarSyncer` constructor (vestigial after BackupTask deletion) — NOTE: story text says "do NOT remove" but the story SCOPE says remove it HERE (story note `:208` says "Removing that annotation is assigned to U-032, not this story") — FOLLOW STORY TEXT, leave @Inject for U-032
10. Remove jacocoFileFilter 4 lines for BackupTask/RestoreTask
11. Update `SmsBackupServiceTest` to remove `@Mock BackupTask backupTask` and `getBackupTask()` override; update assertions to verify `scheduler.scheduleManual(...)` instead of `backupTask.execute(...)`
12. Add cancel test in `SmsBackupServiceTest`; update `SmsRestoreServiceTest` if needed

## FULL_WAKE_LOCK Decision (AC-9)

Decision: Option (b) — `SmsRestoreService` acquires and releases `FULL_WAKE_LOCK` itself around the `scheduleRestore(...)` enqueue + WorkInfo observer window.
- The service already has `acquireLocks()`/`releaseLocks()` infrastructure in `ServiceBase`
- Acquire BEFORE enqueuing; release in the observer's terminal state handler (same place as `stopSelf()`)
- This preserves screen-on behavior during restore while honoring EPIC SC-10 (no behavior change)
- Reviewer sign-off: PASS — behavior preserved per design recommendation

## CalendarSyncer @Inject (AC note)

Per the story's Technical Context section (line 208): "Removing that annotation is assigned to U-032, not this story. The developer must NOT remove the @Inject from CalendarSyncer in this story."
Leave `CalendarSyncer.java` @Inject annotation intact for U-032.

## Files Modified

| File | Change |
|------|--------|
| `scheduler/BackupScheduler.java` | Add `scheduleManual(BackupType)` method; update javadoc count nine→ten |
| `scheduler/WorkManagerScheduler.kt` | Implement `scheduleManual()`; update `cancel()` to handle MANUAL/SKIP |
| `service/SmsBackupService.java` | Add WorkInfo observer bridge; rewrite `backup()` to call `scheduleManual`; add cancel collector; delete `getBackupTask()` factory; delete `getBackupConfig()`/`getMailTransport()` use; delete `BackupTask` import |
| `service/SmsRestoreService.java` | Add WorkInfo observer bridge + FULL_WAKE_LOCK; rewrite `handleIntent()` to call `scheduleRestore`; add cancel collector; delete `getRestoreTask()` factory; retain `clearCache()`/`asyncClearCache()` |
| `service/BackupCancelCollector.kt` | DELETE |
| `service/BackupTask.java` | DELETE |
| `service/RestoreTask.java` | DELETE |
| `app/build.gradle` | Remove 4 BackupTask/RestoreTask jacocoFileFilter lines |

## Files Modified (tests)

| File | Change |
|------|--------|
| `service/SmsBackupServiceTest.java` | Remove `@Mock BackupTask`; remove `getBackupTask()` override; update `shouldTriggerBackupWithManualIntent` to verify `scheduleManual`; update `shouldPassInCorrectBackupConfig` |
| `service/SmsRestoreServiceTest.java` | Minimal update — retain `wakeLockType_returnsBrightScreen` |
| `service/BackupTaskTest.java` | DELETE |
| `service/RestoreTaskTest.java` | DELETE |

## Verification Commands

```
grep -n "nine operations" app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java  # → zero
grep -n "ten operations" app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java   # → one
grep -rn "new BackupTask|new RestoreTask|getBackupTask|getRestoreTask" app/src/main/java/        # → zero
grep -rn "import.*BackupTask|import.*RestoreTask|extends AsyncTask" app/src/main/java/           # → zero
find app/src -name "BackupTask.java" -o -name "RestoreTask.java"                                  # → zero
find app/src/test -name "BackupTaskTest*" -o -name "RestoreTaskTest*"                             # → zero
find app/src -name "BackupCancelCollector.kt"                                                     # → zero
grep -n "BackupTask.class|RestoreTask.class" app/build.gradle                                     # → zero
./gradlew :app:assembleDebug                                                                      # BUILD SUCCESSFUL
./gradlew :app:testDebugUnitTest                                                                  # BUILD SUCCESSFUL, zero failures
./gradlew :app:jacocoTestCoverageVerification                                                    # BUILD SUCCESSFUL
```
