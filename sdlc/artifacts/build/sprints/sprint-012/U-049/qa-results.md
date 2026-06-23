---
artifact_type: qa-results
story_id: "U-049"
verdict: "PASS"
agent: "QA Analyst"
timestamp: "2026-06-23"
ac_total: 5
ac_passed: 5
ac_failed: 0
tests_run: 642
tests_passed: 642
---

# QA Validation: U-049

## Verdict: PASS

Independently verified service retirement against the actual tree. The three service
files and `MainViewModelFactory` are physically deleted (confirmed by `ls` → "No such
file or directory"); the `<service>` entries for them are gone from the manifest; all
remaining `SmsBackupService`/`SmsRestoreService`/`ServiceBase` occurrences are KDoc
comments, not imports or live code. Foreground notification is re-homed into both workers
via `setForeground` as the first `doWork()` statement; WorkInfo observation moved to
`MainViewModel` in `viewModelScope` with `onCleared()` cleanup (BUG-005 preserved);
`MainActivity` dispatches via the injected scheduler with no Service hop;
CNTR-MODERNIZATION-005 broadcast path and the BUG-009 Q+ SMS-role round-trip are intact.

## Acceptance Criteria Results

> Every PASS cites a file:line reference.

| AC | Status | Evidence (file:line references required) |
|----|--------|------------------------------------------|
| AC-1 | PASS | `ls` confirms `SmsBackupService.java`, `SmsRestoreService.java`, `ServiceBase.java`, and `MainViewModelFactory.kt` → "No such file or directory". `AndroidManifest.xml` has no `<service>` for them (only unrelated `.compat.HeadlessSmsSendService` at line 206, an SMS-role component, untouched). `git grep "import.*Sms(Backup\|Restore)Service\|import.*ServiceBase" app/src/main` → zero. All residual name hits are comments (e.g. `MainViewModel.kt:45`, `BackupWorker.kt:119`). |
| AC-2 | PASS | `BackupWorker.kt:120` `setForeground(createBackupForegroundInfo())` is the first statement of `doWork()`; `RestoreWorker.kt:147` `setForeground(createRestoreForegroundInfo())` likewise. Same channel `App.CHANNEL_ID` and content as the legacy services (`BackupWorker.kt:469,498`, `RestoreWorker.kt:683,712`). |
| AC-3 | PASS | Observation re-homed to `MainViewModel.observeBackupWork()` (`MainViewModel.kt:138-155`) and `observeRestoreWork()` (`162-179`) via `WorkManager.getWorkInfosForUniqueWorkFlow(...)` inside `viewModelScope.launch`. BUG-005 fix preserved: `onCleared()` (`MainViewModel.kt:280-286`) cancels `backupObserverJob`, `restoreObserverJob`, `backupCancelCollectorJob`, `restoreCancelCollectorJob`; viewModelScope auto-cancels on Activity destroy. |
| AC-4 | PASS | `MainActivity.startBackup()` calls `viewModel.startBackup(backupType)` (`MainActivity.java:414`); `startRestore()` routes through `SmsDefaultRoleHelper` → `viewModel.startRestore()` (`MainActivity.java:421-425`). `MainViewModel.startBackup/startRestore` call the injected `BackupScheduler.scheduleManual/scheduleRestore` (`MainViewModel.kt:97,119`). `git grep startService` in `activity/` → comment references only, no live calls. |
| AC-5 | PASS (build-green verified by user; on-device broadcast test DEFERRED) | Integrated tree: `assembleDebug` + `testDebugUnitTest` (642) + jacoco all PASS. CNTR-MODERNIZATION-005: `BackupBroadcastReceiver.backupRequested()` → `getScheduler(context).scheduleImmediate()` (`BackupBroadcastReceiver.java:54`), no Service intermediary, no `android:permission` added; receiver untouched. The `adb am broadcast` on-device check (Verification Step 5) is DEFERRED per user instruction — not failed. |

## Integration Path Verification

| Component | Entry Point | Call Path | Verified (yes/no) |
|-----------|-------------|-----------|--------------------|
| Backup dispatch | MainActivity button | `MainActivity.startBackup()` (`:414`) → `MainViewModel.startBackup()` (`:92`) → `BackupScheduler.scheduleManual()` (`:97`) → WorkManager → `BackupWorker.doWork()` | yes |
| Restore dispatch | MainActivity button | `MainActivity.startRestore()` (`:421`) → `SmsDefaultRoleHelper.startRestore()` → `viewModel.startRestore()` (`:114`) → `scheduleRestore()` (`:119`) → `RestoreWorker` | yes |
| Broadcast backup (CNTR-MODERNIZATION-005) | `com.zegoggles.smssync.BACKUP` | `BackupBroadcastReceiver.onReceive` (`:42`) → `backupRequested` (`:50`) → `getScheduler(context).scheduleImmediate()` (`:54`) → WorkManager | yes |
| Foreground notification | doWork() start | `BackupWorker.doWork():120` / `RestoreWorker.doWork():147` `setForeground(...)` → WorkManager SystemForegroundService | yes |
| Cancel routing | SyncEvent.Cancel | `MainViewModel.startBackup/RestoreCancelCollector` (`:262,270`) → `WorkManagerCancelCollector.collect()` → `WorkManager.cancelUniqueWork()` | yes |

## Behavioral Contract Verification

| Contract | Clause | Implementation (file:line) | Match (yes/no) |
|----------|--------|----------------------------|----------------|
| CNTR-MODERNIZATION-005 | Action string frozen; receiver dispatches to scheduler not service; no permission added; 3rd-party guard intact | `BackupBroadcastReceiver.java:39,51,54`; no `android:permission` in manifest receiver entry | yes |
| BUG-005 observer-leak | Observer deregistered on owning-lifecycle destroy | `MainViewModel.onCleared():280-286` cancels all jobs; viewModelScope auto-cancel | yes |
| BUG-009/U-041 Q+ role round-trip | Q+ always proceeds to role request regardless of legacy default-package value | `SmsDefaultRoleHelper.startRestore():77-83` (Q+ branch) → `requestDefaultSmsPackageChange():111-117` uses `RoleManager.createRequestRoleIntent(ROLE_SMS)` + `startActivityForResult` | yes |
| Foreground notification parity | Same channel + notification ID as ServiceBase | `BackupWorker` ID matches `SmsBackupService.BACKUP_ID` (`:498`); `RestoreWorker` ID matches `SmsRestoreService.RESTORE_ID` (`:712`); channel `App.CHANNEL_ID` | yes |
| WorkInfo → state mapping parity | Mirror service mapWorkInfoToBackup/RestoreState | `MainViewModel.kt:185-208` (backup), `:225-245` (restore) — SUCCEEDED/FAILED/CANCELLED/RUNNING all handled | yes |

## Capabilities Inventory Verification

| Capability | Status in Log | Verified in Code | File:Line | Result |
|------------|---------------|------------------|-----------|--------|
| `handleIntent` → dispatch | RETAINED | yes | `MainViewModel.startBackup/startRestore` (`:92,114`) | PASS |
| foreground notification | RETAINED | yes | `BackupWorker.createBackupForegroundInfo()` (`:469`), `RestoreWorker` (`:683`) | PASS |
| WorkInfo observer bridge | RETAINED | yes | `MainViewModel.observeBackupWork/observeRestoreWork` (`:138,162`) | PASS |
| cancel collector | RETAINED | yes | `MainViewModel.kt:262,270` → `WorkManagerCancelCollector` | PASS |
| BUG-005 cleanup | RETAINED | yes | `MainViewModel.onCleared():280-286` | PASS |
| mapWorkInfoTo*State | RETAINED | yes | `MainViewModel.kt:185,225` | PASS |
| restore clearCache | RETAINED | yes | `RestoreWorker.clearAppCache()` (`:630`) | PASS |
| `acquireLocks/releaseLocks` | INTENTIONALLY REMOVED | N/A | WorkManager manages CoroutineWorker wakelocks; documented `BackupWorker.kt:174` | Valid justification: YES |
| `scheduleNextBackup` | INTENTIONALLY REMOVED | N/A | WorkManager periodic work is self-sustaining | Valid justification: YES |
| `handleErrorState` notification | INTENTIONALLY REMOVED | N/A | error states emitted via repository (FAILED WorkInfo → ERROR state) | Valid justification: YES |
| ServiceBase `getMailTransport`/`getPreferences` | INTENTIONALLY REMOVED | N/A | workers use injected `MailTransportFactory`/`Preferences` | Valid justification: YES |

## Requirement Scope Coverage

| Requirement | Scope Item | Covered (yes/no) | Evidence (file:line) |
|-------------|------------|-------------------|----------------------|
| assessment AR-002 | Delete 3 service classes + manifest entries (~1,200 lines) | yes | files deleted; manifest cleaned |
| assessment AR-002 | Foreground via setForeground in workers | yes | `BackupWorker.kt:120`, `RestoreWorker.kt:147` |
| assessment AR-002 | MainActivity dispatches via scheduler, no Service | yes | `MainActivity.java:414,421` |
| Preserve clause | Scheduler cancel-in-flight still works | yes | `WorkManagerCancelCollector` → `cancelUniqueWork`; `WorkManagerScheduler.cancel():375` |

## Test Results

Authoritative `@Test` count = 642 (confirmed). 35 tests for the deleted `SmsBackupServiceTest`/`SmsRestoreServiceTest` were removed with the classes they tested (explained net regression). `MainActivityRestoreTest` updated: BUG-009 role-request test intact (`:75`), service-start assertion replaced with restore-path assertion (`:175`, "No service start expected (U-049)").

## Regression Results

`MainViewModelTest` updated for the extended constructor (Context + BackupScheduler). No production capability dropped without inventory tracking. `BackupBroadcastReceiver` untouched per story instruction.

## Phase Completion Report
---
story_id: "U-049"
phase: "validation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-012/U-049/qa-results.md"
story_status: "validated"
current_build_phase: "validation"
ac_passed: 5
ac_total: 5
errors: []
---
