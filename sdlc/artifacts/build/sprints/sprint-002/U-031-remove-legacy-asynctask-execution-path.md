---
type: story
status: done
artifact_type: user-story
priority: medium
complexity: high
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-013
design_docs:
  - DES-MODERNIZATION-012
integration_contracts:
  - CNTR-MODERNIZATION-004
dependencies:
  - U-030
change_records:
  - CR-001
platforms: []
tags:
  - modernization
  - asynctask-removal
  - workmanager
  - foreground-service
gate_additions: []
id: U-031
title: Migrate manual backup/restore dispatch to WorkManager and delete legacy AsyncTask execution path
pipeline: ''
domain: modernization
alignment_audit: passed
sprint: '000002'
---

# U-031: Migrate manual backup/restore dispatch to WorkManager and delete legacy AsyncTask execution path

## Story

As a maintainer,
I want manual backup/restore dispatch migrated to WorkManager and the legacy AsyncTask BackupTask/RestoreTask deleted,
So that there is a single CoroutineWorker execution path with no duplicated logic.

## Acceptance Criteria

> **Ordering is binding.** AC-1 (Step 2a bridge) MUST pass before AC-2 through AC-7 (dispatch migration + deletion) are implemented or merged. The verification commands below are written to be run in order.

- [ ] **AC-1 — Worker-to-foreground signalling bridge built (BLOCKING PREREQUISITE — Step 2a).**
  Given `SmsBackupService.backup()` enqueues a `BackupWorker` via `getScheduler().scheduleManual(backupType)`,
  When the worker emits progress or reaches a terminal `WorkInfo.State` (`SUCCEEDED`, `FAILED`, `CANCELLED`),
  Then `SmsBackupService.backupStateChanged(BackupState)` is invoked by a `WorkManager.getWorkInfoByIdLiveData(workId)` (or `getWorkInfosForUniqueWorkLiveData(uniqueName)`) observer registered in `backup()` immediately after enqueue — mapping the worker's `PROGRESS_KEY_STATE` progress data and terminal `WorkInfo.State` into a `BackupState` and calling the existing `backupStateChanged()` driver, which performs `startForeground`, `stopForeground(true)`, and `stopSelf()` exactly as today — and the observer is torn down on `stopSelf()`.
  The equivalent observer MUST be built in `SmsRestoreService.handleIntent()` driving `restoreStateChanged(RestoreState)`.
  *Rationale:* `backupStateChanged()`/`restoreStateChanged()` are today driven **only** by a direct call from `BackupTask.java:273`/`RestoreTask.java:328`; workers emit only `setProgress(workDataOf(...))` (`BackupWorker.kt:292/298/337`; `RestoreWorker.kt:218/222/307/392`) and never call `emitState()`; and neither service has a `syncStateRepository` collector (grep-confirmed: no `emitState` caller in any worker file). Deleting the AsyncTask without this bridge silently removes the only caller of `startForeground`/`stopForeground`/`stopSelf`. This AC has no existing artifact to "verify" — the bridge must be implemented. **AC-1 must pass before any deletion AC is begun** (DES-MODERNIZATION-012 §Sequencing Step 2a).

- [ ] **AC-2 — MainActivity manual dispatch migrated onto the scheduler port.**
  Given `MainActivity.java:406` calls `startService(new Intent(this, SmsBackupService.class).setAction(backupType.name()))` and `:411` calls `startService(new Intent(this, SmsRestoreService.class))` (these call sites are NOT changed — they are out of scope per REQ-MODERNIZATION-013 §Constraints),
  When `SmsBackupService.backup(BackupType)` receives the intent (carrying `MANUAL` or `SKIP` as the action),
  Then `backup()` calls `getScheduler().scheduleManual(backupType)` instead of `getBackupTask().execute(getBackupConfig(..., getMailTransport()))`, where `backupType` is one of the two members `BackupType.MANUAL` or `BackupType.SKIP` of the **single shared** `com.zegoggles.smssync.service.BackupType` enum (`BackupType.java:6-12`), and all pre-flight checks (permissions, credentials, enabled-types, the `MailException` catch for invalid URI, and `moveToState()` error reporting) are preserved verbatim at the call site.
  And when `SmsRestoreService.handleIntent()` receives a restore intent,
  Then it calls `getScheduler().scheduleRestore(new RestoreSchedulerConfig(RestoreWorker.RESTORE_WORK_NAME, RestoreWorker.RESTORE_WORK_NAME))` instead of `getRestoreTask().execute(config)`, with the `canWriteToSmsProvider()` guard and `SmsProviderNotWritableException` path preserved verbatim.
  `SKIP` routes to `BackupWorker.executeSkip()` (`BackupWorker.kt:161-162`) via the tag mechanism (AC-3); `MANUAL` reaches the normal IMAP path. There is **no** separate engine-level enum — `MANUAL` and `SKIP` are members of this same shared `service.BackupType` enum.

- [ ] **AC-3 — `scheduleManual(BackupType)` added to `BackupScheduler` port; unique-work names are distinct from `BROADCAST_INTENT`.**
  Given the `BackupScheduler` port (`BackupScheduler.java`) currently has nine operations and `scheduleImmediate()` hard-codes `BackupType.BROADCAST_INTENT` as its tag and unique-work name (`WorkManagerScheduler.kt:274,278`),
  When `scheduleManual(BackupType backupType)` is called with `BackupType.MANUAL` or `BackupType.SKIP`,
  Then the new port operation is declared in `BackupScheduler.java` with the `scheduleManual(BackupType backupType)` signature and its javadoc; `BackupScheduler.java`'s "all nine operations below" javadoc count is updated to "all ten operations below" (`BackupScheduler.java:36-37`); `WorkManagerScheduler.kt` implements `scheduleManual` to enqueue a `OneTimeWorkRequest` for `BackupWorker` with `Constraints.NONE` (no network constraint — identical to `scheduleImmediate`, CNTR-MODERNIZATION-004 INV-2), `ExistingWorkPolicy.REPLACE` (INV-1), `EXPONENTIAL`/30s initial/300s cap backoff (INV-3), no initial delay, unique-work name `backupType.name()` (yielding `"MANUAL"` or `"SKIP"`), and `addTag(backupType.name())` so that `BackupWorker.inferBackupType()` (`BackupWorker.kt:430-436`) resolves `BackupType.fromName(tag)` to `MANUAL` or `SKIP`; and the unique-work name `"MANUAL"`/`"SKIP"` is **distinct** from `"BROADCAST_INTENT"`, so a manual run and an automation-broadcast run via `scheduleImmediate()` do not REPLACE each other in the WorkManager queue (CNTR-MODERNIZATION-004 Validation Rule 8).
  `scheduleImmediate()` is NOT changed — its `BROADCAST_INTENT` tag, unique-work name, and all callers remain byte-for-byte identical.
  Verification: grep `"nine operations"` in `BackupScheduler.java` returns zero results; grep `"ten operations"` returns one result; grep `scheduleManual` in `WorkManagerScheduler.kt` returns at least one implementation line; `./gradlew :app:kaptDebugKotlin` produces zero errors.

- [ ] **AC-4 — `BackupScheduler.java` javadoc operation count updated from nine to ten.**
  Given `BackupScheduler.java:36-37` currently reads "all nine operations below … are binding clauses" (verified; CR-001 Evidence E6),
  When `scheduleManual(BackupType)` is added to the interface in this story,
  Then the javadoc at that line is updated to "all ten operations below" before the merge of this story.
  Verification: `grep -n "nine operations" app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java` returns zero output lines.

- [ ] **AC-5 — Cancel path rewired; `BackupCancelCollector.kt` deleted.**
  Given `StatusPreference.java` emits `SyncEvent.Cancel(USER)` at `:279-283` (backup cancel) and `:304-308` (restore cancel), consumed today **only** by `BackupCancelCollector.kt:29-31`/`:40-42` which calls `task.onCancelRequested()`,
  When `BackupCancelCollector.kt` is deleted (its only consumers are the deleted tasks),
  Then the `SyncEvent.Cancel(USER)` event is rewired to reach `WorkManager.cancelUniqueWork(uniqueName)` via the injected `BackupScheduler`, targeting the unique-work names enqueued by the dispatch migration (`BackupType.MANUAL.name()` for backup, `RestoreWorker.RESTORE_WORK_NAME` for restore); the workers' cooperative `ensureActive()` checks handle the interrupt at the next suspension point; and the AC-1 `WorkInfo` observer drives the resulting `stopForeground(true)`/`stopSelf()` on the `CANCELLED` terminal state.
  A cancel regression test is required: trigger a manual backup run, emit `Cancel(USER)`, assert that the `WorkInfo` state reaches `CANCELLED` and the service calls `stopForeground`/`stopSelf`.
  Verification: `find app/src -name "BackupCancelCollector.kt"` returns zero lines; grep `BackupCancelCollector` over `app/src/main/java/` returns zero results.

- [ ] **AC-6 — Static call-graph confirmed zero live AsyncTask construction, then `BackupTask.java` and `RestoreTask.java` deleted.**
  Given the pre-deletion prerequisite: the dispatch migration (AC-2) and the factory deletion (AC-7) must both be complete before this AC is acted on,
  When a grep verification is run across `app/src/main/java/`:
  — `grep -rn "new BackupTask\|new RestoreTask\|getBackupTask\|getRestoreTask" app/src/main/java/` returns zero output lines, AND
  — `grep -rn "\.execute(" app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java` returns zero results for AsyncTask `.execute(` calls,
  Then `BackupTask.java` (`app/src/main/java/com/zegoggles/smssync/service/BackupTask.java`), `RestoreTask.java` (`app/src/main/java/com/zegoggles/smssync/service/RestoreTask.java`), `BackupTaskTest.java`, and `RestoreTaskTest.java` are deleted from the repository.
  Post-deletion verification:
  — `find app/src -name "BackupTask.java" -o -name "RestoreTask.java"` returns zero lines,
  — `find app/src/test -name "BackupTaskTest*" -o -name "RestoreTaskTest*"` returns zero lines,
  — `grep -rn "import.*BackupTask\|import.*RestoreTask\|extends AsyncTask" app/src/main/java/` returns zero results.

- [ ] **AC-7 — `getBackupTask()` and `getRestoreTask()` factory methods deleted.**
  Given `SmsBackupService.backup()` now calls `scheduleManual(backupType)` (AC-2) and no longer invokes `getBackupTask()`, and `SmsRestoreService.handleIntent()` no longer invokes `getRestoreTask()`,
  When the factory methods are deleted from `SmsBackupService.java` (`:202-240`) and `SmsRestoreService.java` (`:129-143`),
  Then `grep -rn "getBackupTask\|getRestoreTask" app/src/main/java/` returns zero output lines.
  Note: this deletion is assigned to THIS story (U-031); U-032/REQ-014 AC-5/AC-6 are verification-only gates — they do NOT perform the deletion. Leaving the factory bodies in place after the callers and the constructed classes are deleted would not compile (`new BackupTask(...)` with `BackupTask.java` absent).

- [ ] **AC-8 — `SmsBackupService` and `SmsRestoreService` retained as live foreground-service shells.**
  Given `MainActivity.java:406/411` still calls `startService(...)` targeting these services (out of scope to change), and both services are declared in `AndroidManifest.xml` with `android:foregroundServiceType="dataSync"`,
  When this story completes,
  Then `SmsBackupService.java` and `SmsRestoreService.java` are present in `app/src/main/java/com/zegoggles/smssync/service/` and in `AndroidManifest.xml` with `android:foregroundServiceType="dataSync"` unchanged.
  `SmsRestoreService.clearCache()` and its `asyncClearCache()` call in `onCreate()` are retained unchanged — the pre-restore temp-file purge runs in service `onCreate` **before** the worker is enqueued, and `RestoreWorker.clearAppCache()` is left untouched (no worker behavior change per EPIC-MODERNIZATION-005 SC-10).
  Verification: `find app/src/main/java -name "SmsBackupService.java" -o -name "SmsRestoreService.java"` returns two lines; `grep -n "foregroundServiceType" app/src/main/AndroidManifest.xml` returns at least two matches of `"dataSync"`; `grep -n "asyncClearCache\|clearCache" app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java` returns at least two matches.

- [ ] **AC-9 — Restore `FULL_WAKE_LOCK` behavior decision documented and implemented.**
  Given `SmsRestoreService.wakeLockType()` returns `FULL_WAKE_LOCK` on API 19+ (`:203-212`) to keep the screen on during restore, while WorkManager holds only a `PARTIAL_WAKE_LOCK` for a running `CoroutineWorker` (documented at `BackupWorker.kt:150-153`),
  When the story implementation log records the behavior decision,
  Then the implementation log contains an explicit decision statement choosing one of:
  (a) Accept the screen-on behavior change for worker-driven restore (UX change, justified), OR
  (b) Have `SmsRestoreService` acquire and release `FULL_WAKE_LOCK` itself around the `scheduleRestore(...)` enqueue and the `WorkInfo` observer window (recommended by DES-MODERNIZATION-012 to honor EPIC SC-10).
  The decision references WorkManager's built-in PARTIAL wakelock semantics for `CoroutineWorker` and includes reviewer sign-off.
  The existing test `SmsRestoreServiceTest.wakeLockType_returnsBrightScreen` is retained; if option (a) is chosen, the test is updated to assert the new behavior and the change is documented in the log.

- [ ] **AC-10 — `jacocoFileFilter` exclusions for deleted classes removed; 70% gate still passes.**
  Given `app/build.gradle:229-232` excludes four glob patterns for the deleted classes,
  When those four lines are removed after `BackupTask.java` and `RestoreTask.java` are deleted,
  Then `grep -n "BackupTask.class\|BackupTask\$\*.class\|RestoreTask.class\|RestoreTask\$\*.class" app/build.gradle` returns zero output lines,
  And `./gradlew :app:jacocoTestCoverageVerification` completes with `BUILD SUCCESSFUL`, confirming the 70% line-coverage gate holds for `com.zegoggles.smssync.service*` with the four exclusions removed (the deleted classes contribute zero lines — covered or uncovered — so the ratio is not lowered by removing their exclusions).
  If `service*` coverage falls below 70% after removing the exclusions (R-1 risk: `BackupTaskTest`/`RestoreTaskTest` transitively covered other `service.*` classes), backfill characterization tests on surviving `service.*` classes (`BackupConfig`, `BackupCursors`, `BulkFetcher`, or equivalent) before merge.

- [ ] **AC-11 — Full test suite passes; assembleDebug clean.**
  Given all deletion and migration changes are applied,
  When the build is run,
  Then `./gradlew :app:testDebugUnitTest` completes with zero test failures,
  And `./gradlew :app:assembleDebug` completes successfully with zero compile errors, confirming no production or test source retains a reference to any deleted class.

- [ ] **AC-12 — On-device smoke: manual and scheduled backup/restore run via WorkManager with no regression.**
  Given the app is installed on a device or emulator with a test account configured,
  When a manual backup is triggered from the `MainActivity` "Backup Now" button,
  Then a `BackupWorker` job is enqueued via `WorkManagerScheduler` and a log line containing `WorkManagerScheduler` and a `BackupWorker` UUID appears in logcat within 5 seconds of the trigger; the backup completes without crash; the foreground notification appears during execution and is dismissed on completion.
  When a manual restore is triggered from the app UI,
  Then a `RestoreWorker` job is enqueued and confirmed in logcat by the same mechanism; restore completes without crash.
  When a scheduled (`REGULAR`) backup fires via the WorkManager periodic work request,
  Then `BackupState(FINISHED_BACKUP, ...)` state emission is visible in the app log or logcat, confirming the scheduled path is unaffected by this story.

### Integration Criteria

- [ ] **IC-1:** `scheduleManual(BackupType)` is declared in `BackupScheduler.java` (the core port interface) and implemented in `WorkManagerScheduler.kt`; the method signature is `scheduleManual(BackupType backupType)` or the Kotlin equivalent `scheduleManual(backupType: BackupType)`.
- [ ] **IC-2:** `SmsBackupService.backup(BackupType)` calls `getScheduler().scheduleManual(backupType)` as its engine-dispatch call (replacing `getBackupTask().execute(...)`); `SmsRestoreService.handleIntent()` calls `getScheduler().scheduleRestore(...)` (replacing `getRestoreTask().execute(...)`). Both services are reachable from `MainActivity.startBackup()`/`startRestore()` via `startService(...)` exactly as before.
- [ ] **IC-3:** All call sites that previously referenced `BackupTask` or `RestoreTask` (including `SmsBackupServiceTest.java`'s `@Mock BackupTask` and `getBackupTask()` override, which is removed in coordination with this story) compile cleanly after deletion; no import, cast, or method reference to the deleted classes survives in `app/src/main/java/` or `app/src/test/java/`.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `service/SmsBackupService.java` | `backup(BackupType)` at `:130-159` calls `getBackupTask().execute(getBackupConfig(..., getMailTransport()))` | Add `WorkInfo` LiveData observer (AC-1 bridge); rewrite `backup()` to call `getScheduler().scheduleManual(backupType)` (AC-2); delete `getBackupTask()` factory at `:202-240` (AC-7) |
| `service/SmsRestoreService.java` | `handleIntent()` at `:87-121` calls `getRestoreTask().execute(config)` | Add `WorkInfo` LiveData observer (AC-1 bridge); rewrite `handleIntent()` to call `getScheduler().scheduleRestore(...)` (AC-2); delete `getRestoreTask()` factory at `:129-143` (AC-7); `clearCache()`/`asyncClearCache()` at `:152-174` UNCHANGED |
| `scheduler/BackupScheduler.java` | Nine operations; javadoc at `:36` reads "all nine operations below" | Add `scheduleManual(BackupType backupType)` method declaration + javadoc; update operation count to "ten" (AC-3, AC-4) |
| `scheduler/WorkManagerScheduler.kt` | `scheduleImmediate()` at `:270-282` hard-codes `BROADCAST_INTENT` tag | Implement `scheduleManual(BackupType)` per CNTR-MODERNIZATION-004 Validation Rule 8: `Constraints.NONE`, `ExistingWorkPolicy.REPLACE`, `EXPONENTIAL`/30s/300s, unique-work name = `backupType.name()`, `addTag(backupType.name())` (AC-3) |
| `service/BackupTask.java` | AsyncTask-based backup engine; FQN k-9 catch at `:306-310` (sealed by U-030 as Step 1); `Lazy<CalendarSyncer>` consumer | DELETED after AC-1/AC-2/AC-6 prerequisites satisfied (AC-6) |
| `service/RestoreTask.java` | AsyncTask-based restore engine; `FULL_WAKE_LOCK` comment at `:100-101` | DELETED (AC-6) |
| `service/BackupCancelCollector.kt` | Consumes `SyncEvent.Cancel(USER)` and calls `task.onCancelRequested()` for both tasks | DELETED; cancel rewired to `BackupScheduler.cancel()` / `WorkManager.cancelUniqueWork()` (AC-5) |
| `app/build.gradle` | `jacocoFileFilter` at `:229-232` excludes four `BackupTask`/`RestoreTask` globs | Remove those four exclusion lines after deletion (AC-10) |
| `activity/StatusPreference.java` | Cancel emitter at `:279-283`/`:304-308` targets `SyncEvent.Cancel(USER)` → `BackupCancelCollector` | Rewired to reach `WorkManager.cancelUniqueWork` via scheduler (AC-5); `StatusPreference.java` emit mechanism preserved |
| `service/BackupTaskTest.java` | Tests `BackupTask` (mocks `convertMessages` at `:117-237`) | DELETED (AC-6); pre-deletion confirm `jacocoTestCoverageVerification` still passes (AC-10) |
| `service/RestoreTaskTest.java` | Tests `RestoreTask` | DELETED (AC-6) |

## Existing Behavior to Preserve

- `SmsBackupService.backup()` pre-flight checks (permissions, credentials, enabled-types, `MailException` catch for invalid URI, `moveToState()` error reporting) — preserved verbatim in the rewritten method body.
- `SmsRestoreService.handleIntent()` `canWriteToSmsProvider()` guard and `SmsProviderNotWritableException` path — preserved verbatim.
- `SmsRestoreService.clearCache()` / `asyncClearCache()` in `onCreate()` — retained unchanged; the pre-restore temp-file purge remains a service `onCreate` responsibility and runs before the worker enqueue.
- Manual foreground-service promotion: `SmsBackupService.backupStateChanged()` at `:259-280` calls `startForeground(BACKUP_ID, notification)` and `stopForeground(true)` + `stopSelf()` on terminal state — preserved via the AC-1 `WorkInfo` observer bridge.
- Manual restore teardown: `SmsRestoreService.restoreStateChanged()` at `:178-195` drives `stopForeground(true)` + `stopSelf()` — preserved via the AC-1 `WorkInfo` observer bridge.
- `SKIP` backup early-return path: `BackupWorker.executeSkip()` at `:161-162` (marks max-synced date without IMAP) — preserved because `scheduleManual(BackupType.SKIP)` tags the worker with `"SKIP"` and `inferBackupType()` resolves it correctly (no BROADCAST_INTENT collapse).
- `MANUAL` vs `BROADCAST_INTENT` notification/foreground semantics: `MANUAL` has `isBackground() == false` (`BackupType.java:38`) driving `shouldNotifyUser` and `notifyAboutBackup` in `SmsBackupService` — preserved because the worker is tagged `"MANUAL"`, not `"BROADCAST_INTENT"`.
- Scheduled (REGULAR/INCOMING/BROADCAST_INTENT) backup and restore paths — not changed; `scheduleImmediate()`, `scheduleRegular()`, `scheduleIncoming()`, and `scheduleRestore()` call sites are unaffected.
- `SmsRestoreService.wakeLockType()` FULL_WAKE_LOCK behavior — explicit documented decision required (AC-9; recommended: service acquires the lock itself around the enqueue+observe window).
- `SmsBackupServiceTest` and `SmsRestoreServiceTest` non-BackupTask/non-RestoreTask test assertions (state machine, scheduling, error notification) — retained or replaced by equivalent assertions against `scheduleManual`/`scheduleRestore` instead of `backupTask.execute()`.
- The 70% JaCoCo line-coverage gate for `com.zegoggles.smssync.service*` — maintained; backfill characterization tests if needed after exclusion removal (AC-10 R-1 mitigation).

## Verification Steps

1. **AC-1 (WorkInfo bridge):** Trigger a manual backup from the app UI (or via a Robolectric test that calls `SmsBackupService.backup(BackupType.MANUAL)` with a mock `WorkManager`). Assert that `backupStateChanged()` is called with a `RUNNING` state (verifying `startForeground` is triggered) and subsequently with a `FINISHED` terminal state (verifying `stopForeground(true)` and `stopSelf()` are called). Confirm the `WorkInfo` observer is unregistered after `stopSelf()`.

2. **AC-2 (dispatch migration):** Run `grep -rn "getBackupTask\(\)\|getRestoreTask\(\)" app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java` — must return zero results. Read `SmsBackupService.backup()` body: assert it contains a call matching `scheduleManual(` and does not contain `execute(`. Read `SmsRestoreService.handleIntent()` body: assert it contains `scheduleRestore(` and does not contain `execute(`.

3. **AC-3 (scheduleManual port operation):** Run `grep -n "scheduleManual" app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java` — must return at least one match (the declaration). Run `grep -n "scheduleManual" app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt` — must return at least one match (the implementation). In a unit test: call `WorkManagerScheduler.scheduleManual(BackupType.SKIP)`, query `WorkManager.getWorkInfosForUniqueWork("SKIP")`, assert `WorkInfo` exists with tag `"SKIP"` and constraints `Constraints.NONE`. Repeat for `BackupType.MANUAL`. Assert that `WorkManager.getWorkInfosForUniqueWork("BROADCAST_INTENT")` is unaffected (no replacement).

4. **AC-4 (javadoc count):** Run `grep -n "nine operations" app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java` — must return zero results. Run `grep -n "ten operations" app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java` — must return one result.

5. **AC-5 (cancel rewire):** In a unit test: configure a `SmsBackupService` with a mock `BackupScheduler`; trigger `backup(BackupType.MANUAL)`; emit `SyncEvent.Cancel(USER)` through `StatusPreference`'s cancel path; assert `scheduler.cancel(BackupType.MANUAL)` (or `WorkManager.cancelUniqueWork("MANUAL")`) is called exactly once. Run `find app/src -name "BackupCancelCollector.kt"` — must return zero lines.

6. **AC-6 (grep-zero then delete):** Before deletion: run `grep -rn "new BackupTask\|new RestoreTask\|getBackupTask\|getRestoreTask" app/src/main/java/` — must return zero lines. After deletion: `find app/src -name "BackupTask.java" -o -name "RestoreTask.java"` → zero; `find app/src/test -name "BackupTaskTest*" -o -name "RestoreTaskTest*"` → zero; `grep -rn "import.*BackupTask\|import.*RestoreTask\|extends AsyncTask" app/src/main/java/` → zero.

7. **AC-7 (factory deletion):** Run `grep -rn "getBackupTask\|getRestoreTask" app/src/main/java/` → zero results.

8. **AC-8 (services retained):** Run `find app/src/main/java -name "SmsBackupService.java" -o -name "SmsRestoreService.java"` — must return two lines. Run `grep -n "foregroundServiceType" app/src/main/AndroidManifest.xml` — must return two matches both containing `"dataSync"`. Run `grep -n "asyncClearCache\|clearCache" app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java` — must return at least two matches confirming the methods are present.

9. **AC-9 (wakelock decision):** Read the story implementation log. Confirm it contains the word "FULL_WAKE_LOCK" and one of "accept" or "service acquires" and the word "PARTIAL". If option (a) was chosen, confirm `SmsRestoreServiceTest.wakeLockType_returnsBrightScreen` is updated to assert the new behavior and the change is noted.

10. **AC-10 (jacoco exclusions):** Run `grep -n "BackupTask.class\|BackupTask\$\*.class\|RestoreTask.class\|RestoreTask\$\*.class" app/build.gradle` — must return zero results. Run `./gradlew :app:jacocoTestCoverageVerification` — must complete with `BUILD SUCCESSFUL`.

11. **AC-11 (build + test):** Run `./gradlew :app:testDebugUnitTest` — must complete with zero test failures. Run `./gradlew :app:assembleDebug` — must complete successfully.

12. **AC-12 (on-device smoke):** Install the debug APK on a device or emulator with a test account. Tap "Backup Now"; inspect logcat within 5 seconds for a line containing `WorkManagerScheduler` and a UUID associated with a `BackupWorker` job. Observe the foreground notification appears and is dismissed on completion. Trigger a restore; confirm a `RestoreWorker` UUID appears in logcat. Confirm a `REGULAR` periodic backup fires and logs `BackupState(FINISHED_BACKUP)`.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Java/Kotlin) | All changes — WorkInfo bridge, dispatch migration, scheduleManual implementation, deletions, cancel rewire, jacocoFileFilter, smoke | Developer |

## Technical Context

- **Sequencing — story 2 of 3:** This story executes after U-030 (REQ-MODERNIZATION-012, ACL seal) and before U-032 (REQ-MODERNIZATION-014, Hilt completion). U-030 must be merged first: it seals `ServiceBase.getMailTransport()`'s FQN catch (`ServiceBase.java:171-176`) and ensures `K9MailTransport`'s constructor throws only `MailException` before this story touches `SmsBackupService`. U-032 depends on this story having merged so that `BackupTask` is absent before `@AndroidEntryPoint` is applied (the `CalendarSyncer` `@Inject` ctor with unbound `long calendarId` at `CalendarSyncer.java:32-41` compiles today only because `BackupTask` is hand-constructed — the moment `BackupTask` is deleted, the vestigial `@Inject` annotation is an inert dead annotation, not a live graph path, preventing `[Dagger/MissingBinding]` when U-032 adds `@AndroidEntryPoint`).

- **CalendarSyncer prerequisite for U-032:** `CalendarSyncer.java:32-41` has an `@Inject` constructor whose second parameter is a raw `long calendarId` with no Hilt binding anywhere in `di/` (grep-confirmed: no `@Provides`/`@Binds` for `calendarId` or `long` exists in `PreferencesModule.kt` or any other module file; there is no `EngineModule`). This compiles today only because `BackupTask` (the only `@Inject`-mediated consumer of `CalendarSyncer` via `Lazy<CalendarSyncer>`) is hand-constructed in `SmsBackupService.getBackupTask()` (the `Lazy` is a hand-written lambda at `SmsBackupService.java:230-237` calling `new CalendarSyncer(...)` directly, bypassing Dagger). Deleting `BackupTask` in this story removes `CalendarSyncer`'s only `@Inject`-mediated consumer; the `@Inject` annotation then becomes vestigial. **Removing that annotation is assigned to U-032, not this story.** The developer must NOT remove the `@Inject` from `CalendarSyncer` in this story.

- **Step 2a (WorkInfo bridge) is the highest-effort task:** There is no existing worker-to-`backupStateChanged()`/`restoreStateChanged()` channel. `BackupTask.java:273` calls `service.backupStateChanged(state)` and `RestoreTask.java:328` calls `service.restoreStateChanged(state)` directly — these are the **only** callers today. Workers emit `setProgress(workDataOf(...))` only (`BackupWorker.kt:337`). Building the `WorkManager.getWorkInfoByIdLiveData(workId)` observer in `backup()` / `handleIntent()` that maps `WorkInfo.State`/`progressData` back into a `BackupState`/`RestoreState` and calls the existing `*StateChanged()` driver is the most complex task in this story and must be implemented before any deletion begins.

- **`scheduleImmediate()` is unchanged:** The existing `scheduleImmediate()` implementation at `WorkManagerScheduler.kt:270-282` hard-codes `BackupType.BROADCAST_INTENT.name` for both the tag (`addTag(BackupType.BROADCAST_INTENT.name)`, `:274`) and the unique-work name (`:278`). It must not be modified. The new `scheduleManual(BackupType)` is a separate method that uses `backupType.name()` for both the tag and the unique-work name, producing `"MANUAL"` or `"SKIP"` — distinct names that prevent WorkManager REPLACE-collision between manual and automation-broadcast runs (CNTR-MODERNIZATION-004 Validation Rule 8).

- **BackupType enum is a single shared type:** There is exactly one `BackupType` enum in the codebase — `com.zegoggles.smssync.service.BackupType` (`BackupType.java:6-12`), with six members: `BROADCAST_INTENT`, `INCOMING`, `REGULAR`, `UNKNOWN`, `MANUAL`, `SKIP`. It is imported by `WorkManagerScheduler.kt:26` and used by the engine layer (`BackupWorker`, `SmsBackupService`). There is no separate "engine-level" or "scheduler-level" enum. `MANUAL.isBackground() == false` (`BackupType.java:37-39`) is the property that drives manual notification/foreground behavior in the service.

- **R-1 coverage risk:** Deleting `BackupTaskTest.java` and `RestoreTaskTest.java` removes tests that may transitively cover other `service.*` classes (`BackupConfig`, `BackupCursors`, `BulkFetcher`). The 70% JaCoCo gate for `com.zegoggles.smssync.service*` must be verified after deletion and exclusion removal (AC-10). If the gate dips below 70%, backfill characterization tests on the surviving classes (the same remedy used in U-006 and U-017).

- **`SmsBackupServiceTest` edit coordination with U-032:** `SmsBackupServiceTest.java` currently declares `@Mock BackupTask backupTask` and overrides `getBackupTask()` (per DES-MODERNIZATION-012 §Sequencing per-shared-file table). In this story: the `getBackupTask()` override and `@Mock BackupTask` field must be removed when the factory is deleted (AC-7). The anonymous-subclass migration (`new SmsBackupService() {` replacement) is owned by U-032. Leave the test in a compiling state at the end of this story — the `@Mock BackupTask` and `getBackupTask()` override go here; the broader anonymous-subclass shape stays for U-032.

- **Estimation guidance — HIGH complexity:** This is a multi-step story with strong internal ordering: (Step 2a) WorkInfo bridge build, (Step 2b) dispatch migration + `scheduleManual` port addition, (Step 2c) deletions + cancel rewire + jacocoFileFilter + smoke. Each step has non-trivial verification. Expect this to be the largest story in the three-story sequence.

## Supporting Documentation

- REQ-MODERNIZATION-013 — Remove the legacy AsyncTask backup/restore execution path (all ACs; especially §Confirm non-invocation, §Scope SmsBackupService conditionally, §Preserve foreground-service and wakelock semantics)
- DES-MODERNIZATION-012 §Component Design > REQ-013 — manual dispatch migration decision (Option a); foreground/wakelock decision (Option b1); factory deletion ownership; jacocoFileFilter
- DES-MODERNIZATION-012 §Integration Design — before/after dispatch flow; WorkInfo observer as hard prerequisite; R-2/R-3/R-4 hazards; `clearCache()` retention decision
- DES-MODERNIZATION-012 §Sequencing & Dependencies — ordered Step 1 (U-030) → Step 2a/2b/2c (this story) → Step 3 (U-032); CalendarSyncer precondition
- DES-MODERNIZATION-012 §Risks — R-1 (coverage gate), R-2 (foreground bridge), R-3 (FULL_WAKE_LOCK), R-4 (cancel path)

## Integration Contract References

- CNTR-MODERNIZATION-004 §Contract Definition > `scheduleManual(backupType: BackupType)` — port operation signature, constraint spec (Constraints.NONE, REPLACE, EXPONENTIAL/30s/300s, no delay, unique-work name = backupType.name(), addTag(backupType.name()))
- CNTR-MODERNIZATION-004 §Validation Rule 8 — `scheduleManual` type-carrying invariant; must not collapse MANUAL/SKIP onto BROADCAST_INTENT; unique-work names distinct
- CNTR-MODERNIZATION-004 §Enum BackupType — single shared six-member enum; MANUAL/SKIP are members, not a separate type
- CNTR-MODERNIZATION-004 §Changelog v2 — "nine → ten" operation count; CR-001 forward-authoring guidance

## Implementation Notes
<!-- Added by agents during build -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes

- U-030 (REQ-MODERNIZATION-012) must merge before this story begins. U-030 deletes the FQN k-9 catch in `ServiceBase.getMailTransport()` and seals the `K9MailTransport` constructor; if this story runs first, `ServiceBase.java:171-176` still has the FQN catch and `SmsBackupService` (which calls `getMailTransport()`) would retain a compile coupling.
- The `BackupTask.java` FQN k-9 catch at `:306-310` (a REQ-MODERNIZATION-012 concern) is moot for this story: U-030 seals it in the U-030 pass; this story then deletes `BackupTask.java` entirely. No double-edit is needed.
- `BackupWorker.kt` and `RestoreWorker.kt` jacocoFileFilter exclusions at `app/build.gradle:219-222` are NOT removed by this story — worker coverage is deferred to instrumented tests (REQ-MODERNIZATION-013 Notes; out of scope).
- The `getMailTransport()` seam in `ServiceBase` (used to construct the `MailTransport` passed to the task) becomes unused for backup once `SmsBackupService.backup()` delegates to the scheduler (the worker builds its own transport via `MailTransportFactory`). The design retains `getMailTransport()` to avoid widening scope — do NOT remove it in this story. It is flagged as now-dead for a future cleanup story.
- `LegacyScheduler` and `CompositeScheduler` (debug-only adapters) — if present at implementation time, they must also implement `scheduleManual(BackupType)`. The developer must confirm their presence via grep at the start of implementation.
