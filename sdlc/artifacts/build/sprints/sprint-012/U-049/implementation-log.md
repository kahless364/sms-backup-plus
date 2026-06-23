---
artifact_type: implementation-log
story_id: "U-049"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-23"
files_changed: 13
files_created: 0
tests_added: 0
tests_passing: 638
---

# Implementation Log: U-049 — Retire Legacy Backup/Restore Service Dual-Dispatch Layer

## Summary

Deleted ~1,200 lines of transitional glue (SmsBackupService, SmsRestoreService, ServiceBase)
and their AndroidManifest `<service>` entries. Foreground notification re-homed into
BackupWorker/RestoreWorker via `setForeground(ForegroundInfo(...))`. WorkInfo observation
and state emission re-homed into MainViewModel using `getWorkInfosForUniqueWorkFlow()` and
viewModelScope. MainActivity now calls `viewModel.startBackup()/startRestore()` directly —
no Service intermediary.

**Net change: -1,995 lines, +406 lines (net -1,589 lines)**

## Files Modified

| File | Change |
|------|--------|
| `app/src/main/AndroidManifest.xml` | Removed `<service android:name=".service.SmsBackupService">` and `<service android:name=".service.SmsRestoreService">` entries; added explanatory comment |
| `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` | Removed `import SmsBackupService`, `import SmsRestoreService`. `startBackup()` now calls `viewModel.startBackup(backupType)`. `startRestore()` replaces all `startService(SmsRestoreService.class)` calls with `viewModel.startRestore()`. Updated comment in `requestPostNotificationsIfNeeded()`. |
| `app/src/main/java/com/zegoggles/smssync/activity/MainViewModel.kt` | Major extension: added `Context` and `BackupScheduler` constructor params; added `startBackup()`, `startRestore()`, `observeBackupWork()`, `observeRestoreWork()`, `mapWorkInfoToBackupState()`, `mapWorkInfoToRestoreState()`, cancel collector wiring, and `onCleared()` for BUG-005 fix. |
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` | Added `setForeground(createBackupForegroundInfo())` at start of `doWork()`. Added `createBackupForegroundInfo()` private method. Added `BACKUP_NOTIFICATION_ID = 1` companion constant. Added imports: `ForegroundInfo`, `NotificationCompat`, `App`, `R`, `MainActivity`, `PendingIntent`. |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt` | Added `setForeground(createRestoreForegroundInfo())` at start of `doWork()`. Added `createRestoreForegroundInfo()` private method. Added `RESTORE_NOTIFICATION_ID = 2` companion constant. Added imports: `ForegroundInfo`, `NotificationCompat`, `App`, `R`, `MainActivity`, `PendingIntent`. |
| `app/src/test/java/com/zegoggles/smssync/activity/MainActivityRestoreTest.java` | Updated `bug009_remediation_onActivityResult_resultOk_qPlus_roleHeld_startsRestoreService` test to `bug009_remediation_onActivityResult_resultOk_qPlus_roleHeld_entersRestorePath` (service no longer started; test verifies guard still fires). Removed `SmsRestoreService` import. Added `ShadowActivity` import back (still needed for other test). Updated RESULT_CANCELED test comment. |
| `app/src/test/java/com/zegoggles/smssync/activity/MainViewModelTest.kt` | Added `@RunWith(RobolectricTestRunner.class)`, `@Mock BackupScheduler`, `MockitoAnnotations.openMocks()`. Updated `MainViewModel(...)` constructor call to pass `RuntimeEnvironment.getApplication(), repository, mockScheduler`. |

## Files Deleted

| File | Reason |
|------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java` | AC-1: transitional dual-dispatch layer retired |
| `app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java` | AC-1: transitional dual-dispatch layer retired |
| `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java` | AC-1: base class of deleted services; no remaining subclasses |
| `app/src/main/java/com/zegoggles/smssync/activity/MainViewModelFactory.kt` | Dead code — U-024 already migrated to @HiltViewModel; constructor signature changed made it compile-fail; was never called in production (MainActivity already used Hilt factory) |
| `app/src/test/java/com/zegoggles/smssync/service/SmsBackupServiceTest.java` | Tests for deleted class |
| `app/src/test/java/com/zegoggles/smssync/service/SmsRestoreServiceTest.java` | Tests for deleted class |

## How Foreground / Progress Now Works

1. **Foreground notification**: `BackupWorker.doWork()` and `RestoreWorker.doWork()` each call `setForeground(ForegroundInfo(...))` as the first statement. WorkManager's `SystemForegroundService` (declared in WorkManager's own manifest) manages the foreground promotion. Notification channel (`App.CHANNEL_ID`), notification IDs (BACKUP=1, RESTORE=2), and content match the legacy service notifications.

2. **Progress observation**: `MainViewModel.observeBackupWork()` / `observeRestoreWork()` use `WorkManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName)` within `viewModelScope.launch {}`. Each `WorkInfo` update is mapped to `BackupState`/`RestoreState` and emitted to `SyncStateRepository.emitState()`. `MainActivityFlowHelper` collects from the repository and routes to `MainActivity.onBackupStateChanged()` / `onRestoreStateChanged()` — unchanged from before.

3. **Cancellation**: `WorkManagerCancelCollector.collect()` is started for each backup/restore work item. Cancel events from `SyncEvent.Cancel` are routed to `WorkManager.cancelUniqueWork()`. This is the same pattern used in the deleted services.

## How CNTR-MODERNIZATION-005 is Preserved

`BackupBroadcastReceiver` was NOT touched. It still calls `scheduler.scheduleImmediate()` directly via the injected `BackupScheduler` (unchanged from its current state). No service intermediary was ever involved in the CNTR-MODERNIZATION-005 path. AC-5 satisfied.

## How BUG-005 is Preserved

The original BUG-005 fix ensured `LiveData.removeObserver()` was called via cached `workInfoLiveData` reference in `SmsBackupService.onDestroy()` / `SmsRestoreService.onDestroy()`.

In the new architecture, the observation is done via `viewModelScope.launch { flow.collect {} }`. The `viewModelScope` coroutine is **automatically cancelled** when `ViewModel.onCleared()` is called (which happens when the Activity is destroyed). Cancelling the scope cancels all `collect {}` coroutines, which terminates the flow collection — equivalent to `removeObserver()`. Additionally, `MainViewModel.onCleared()` explicitly cancels `backupObserverJob`, `restoreObserverJob`, `backupCancelCollectorJob`, `restoreCancelCollectorJob` for defensive cleanup. No observer leak is possible.

## AC Verification

| AC | Status | Evidence |
|----|--------|---------|
| AC-1: Service files deleted, no `<service>` entries, no imports in production code | PASS | Files deleted; manifest entries removed; `grep -r "import.*SmsBackupService\|import.*SmsRestoreService\|import.*ServiceBase" app/src/main/` = zero results |
| AC-2: BackupWorker and RestoreWorker call `setForeground(ForegroundInfo(...))` | PASS | `BackupWorker.kt:105`, `RestoreWorker.kt:133` |
| AC-3: WorkInfo observer re-homed; BUG-005 observer-leak fix preserved | PASS | `MainViewModel.kt` `observeBackupWork()`/`observeRestoreWork()` in viewModelScope; `onCleared()` cancels all jobs |
| AC-4: `MainActivity` calls `viewModel.startBackup()/startRestore()` directly | PASS | `MainActivity.java:420` (`viewModel.startBackup(backupType)`), `MainActivity.java:432` (`viewModel.startRestore()`) |
| AC-5: Build green, CNTR-MODERNIZATION-005 path intact | PASS | `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` all PASS; `BackupBroadcastReceiver` untouched |

## Capabilities Inventory (U-049 Retirement)

**SmsBackupService capabilities:**
- `handleIntent()` → dispatch to scheduler: RETAINED (now in `MainViewModel.startBackup()`)
- `backup()` → permission/credential checks: INTENTIONALLY REMOVED (workers handle these; UI errors are surfaced via WorkInfo FAILED state)
- `notifyAboutBackup()` → foreground notification: RETAINED (now in `BackupWorker.createBackupForegroundInfo()`)
- `registerBackupWorkInfoObserver()` → WorkInfo bridge: RETAINED (now in `MainViewModel.observeBackupWork()`)
- `registerCancelCollector()` → cancel routing: RETAINED (now in `MainViewModel.startBackupCancelCollector()`)
- `tearDownObserverAndCollector()` → BUG-005 cleanup: RETAINED (now via `viewModelScope` lifecycle and `onCleared()`)
- `scheduleNextBackup()` → schedule next regular: INTENTIONALLY REMOVED (WorkManager periodic work is self-sustaining; `BackupWorker` does not call `scheduleRegular()` — this was service-layer glue that the worker doesn't need)
- `backupStateChanged()` → foreground/stop driver: RETAINED in part (state emission now goes via repository → MainActivityFlowHelper → onBackupStateChanged())
- `handleErrorState()` → error notification dispatch: INTENTIONALLY REMOVED (error states are emitted via repository; error notifications were service-bound and don't need re-implementation at the worker level for the foreground case)
- `mapWorkInfoToBackupState()` / `mapProgressStateToSmsSyncState()`: RETAINED (now in `MainViewModel`)

**SmsRestoreService capabilities:**
- `handleIntent()` → dispatch to scheduler: RETAINED (now in `MainViewModel.startRestore()`)
- `registerRestoreWorkInfoObserver()` → WorkInfo bridge: RETAINED (now in `MainViewModel.observeRestoreWork()`)
- `registerCancelCollector()` → cancel routing: RETAINED (now in `MainViewModel.startRestoreCancelCollector()`)
- `tearDownObserverAndCollector()` → BUG-005 cleanup: RETAINED (viewModelScope + onCleared)
- `restoreStateChanged()` → foreground/stop driver: RETAINED in part (state emission via repository)
- `acquireLocks()` / `releaseLocks()` → FULL_WAKE_LOCK: INTENTIONALLY REMOVED (WorkManager manages wakelocks internally for CoroutineWorker; documented in BackupWorker.kt:165)
- `clearCache()` → temp file cleanup: RETAINED (already in RestoreWorker.clearAppCache())
- `mapWorkInfoToRestoreState()` / `mapProgressStateToSmsSyncState()`: RETAINED (now in `MainViewModel`)

**ServiceBase capabilities:**
- `getPreferences()` / `getAuthPreferences()`: INTENTIONALLY REMOVED (workers use @AssistedInject-supplied Preferences/AuthPreferences directly)
- `getMailTransport()`: INTENTIONALLY REMOVED (workers use MailTransportFactory from DI)
- `acquireLocks()` / `releaseLocks()`: INTENTIONALLY REMOVED (WorkManager handles wakelocks)
- `createNotification()` / `getPendingIntent()`: RETAINED (BackupWorker.createBackupForegroundInfo() / RestoreWorker.createRestoreForegroundInfo() replicate this)
- `appLog()` / `appLogDebug()`: INTENTIONALLY REMOVED (workers use android.util.Log directly; AppLog is a service-lifecycle concern)

## Build & Coverage Results

```
./gradlew :app:assembleDebug          → BUILD SUCCESSFUL
./gradlew :app:testDebugUnitTest      → BUILD SUCCESSFUL
./gradlew :app:jacocoTestCoverageVerification → BUILD SUCCESSFUL (all packages ≥ 70% LINE)
```

Authoritative @Test count (post-commit): **638**
(Previous: 673; removed 35 tests from SmsBackupServiceTest + SmsRestoreServiceTest for deleted classes)
