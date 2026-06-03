---
type: story
status: done
sprint: "000001"
artifact_type: user-story
priority: high
complexity: high
parallel_eligible: false
iteration: 2
requirements:
  - REQ-MODERNIZATION-005
design_docs:
  - DES-MODERNIZATION-005
integration_contracts:
  - CNTR-MODERNIZATION-004
dependencies:
  - U-014
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-015
title: 'CoroutineWorker rewrite: extract backup/restore use-cases to core and replace BackupTask/RestoreTask with BackupWorker/RestoreWorker'
pipeline: ''
domain: modernization
---

# U-015: CoroutineWorker rewrite: extract backup/restore use-cases to core and replace BackupTask/RestoreTask with BackupWorker/RestoreWorker

## Story

As a maintainer of SMS Backup+,
I want the backup and restore execution logic extracted from `BackupTask`/`RestoreTask` into suspending use-case functions in the core module, with `BackupWorker` and `RestoreWorker` (`@HiltWorker CoroutineWorker`) serving as thin adapters that call those functions and report progress through WorkManager `setProgress` and an observable `SchedulerState` flow,
so that the `AsyncTask` dependency that was removed in API 33 is eliminated, the `SmsJobService` lifecycle bypass is deleted, structured coroutine cancellation replaces `cancel(true)` + `isCancelled()` polling, and the Otto progress bus is no longer used by the execution path.

## Acceptance Criteria

- [ ] AC-1: **[Core use-case extraction — BackupUseCase]** Given the backup loop currently inside `BackupTask.backupCursors()` (verified at `app/src/main/java/com/zegoggles/smssync/service/BackupTask.java`, the `while (!isCancelled() && cursors.hasNext())` loop at line 267), when this story is complete, then an equivalent suspending function — `BackupUseCase.execute(config: BackupConfig): BackupResult` — lives in the core module and contains the backup loop rewritten as a suspending loop (replacing `isCancelled()` with a coroutine cancellation check via `ensureActive()` or equivalent cooperative suspension). The function carries no `AsyncTask`, `Otto`, or `SmsBackupService` import. All collaborators (`BackupItemsFetcher`, `MessageConverter`, `CalendarSyncer`, `AuthPreferences`, `Preferences`, `TokenRefresher`) are constructor-injected, not pulled from a `Service` reference (replacing the `SmsBackupService` field access verified at `BackupTask.java:52-88`).

- [ ] AC-2: **[Core use-case extraction — RestoreUseCase]** Given the restore loop currently inside `RestoreTask.restore()` (verified at `app/src/main/java/com/zegoggles/smssync/service/RestoreTask.java`, the `for (; currentRestoredItem < itemsToRestoreCount && !isCancelled(); currentRestoredItem++)` loop at line 119), when this story is complete, then an equivalent suspending function — `RestoreUseCase.execute(config: RestoreConfig): RestoreResult` — lives in the core module and contains the restore loop rewritten as a suspending loop with cooperative cancellation. The function carries no `AsyncTask`, `Otto`, or `SmsRestoreService` import. The `smsExists()` dedup guard (keyed on `date + address + type`, verified at `RestoreTask.java:308-327`) and the `callLogExists()` dedup guard (keyed on `date + number + duration + type`, verified at `RestoreTask.java:288-306`) are preserved verbatim in the rewritten function and gate every provider insert exactly as they do today (`RestoreTask.java:259` for SMS, `:280` for call log).

- [ ] AC-3: **[BackupWorker — thin CoroutineWorker adapter]** Given the core `BackupUseCase`, when `BackupWorker` (located in the app module, annotated `@HiltWorker`, extending `CoroutineWorker`) is invoked by WorkManager, then: (a) `doWork()` calls `BackupUseCase.execute(config)` and maps the result to `Result.success()`, `Result.failure()`, or `Result.retry()` (retryable errors — network, transient IMAP — map to `retry()`; unrecoverable errors map to `failure()` with a `Data` payload containing the failure reason); (b) progress is emitted via `setProgress(workDataOf(...))` at each loop iteration, mirroring the `publishProgress(BackupState(...))` cadence from `BackupTask.java:292`, using the `BackupProgress` payload type owned by DES-007; (c) the `BackupWorker` class contains no backup loop, no content resolver access, no direct IMAP interaction, and no reference to `SmsBackupService`.

- [ ] AC-4: **[RestoreWorker — thin CoroutineWorker adapter]** Given the core `RestoreUseCase`, when `RestoreWorker` (app module, `@HiltWorker`, `CoroutineWorker`) is invoked by WorkManager, then: (a) `doWork()` calls `RestoreUseCase.execute(config)` and maps the result to `Result.success()`, `Result.failure()`, or `Result.retry()`; (b) progress is emitted via `setProgress` at each message restore, mirroring the `publishProgress(RestoreState(RESTORE, currentRestoredItem, itemsToRestoreCount, ...))` cadence from `RestoreTask.java:123`; (c) `RestoreWorker` carries no restore loop, no provider access, and no reference to `SmsRestoreService`.

- [ ] AC-5: **[Structured cancellation — no isCancelled() polling]** Given a running `BackupWorker` or `RestoreWorker`, when `BackupScheduler.cancel(jobKind)` is called (which calls `WorkManager.cancelUniqueWork(name)`, cancelling the coroutine scope), then: (a) the coroutine is cancelled cooperatively — the loop in the use-case unwinds at the next `ensureActive()` or suspension point without needing `isCancelled()` polling; (b) the worker emits `SchedulerState.Cancelled` via the `observe(jobKind)` flow; (c) the existing `AsyncTask.cancel(true)` + `isCancelled()` pattern (verified at `BackupTask.java:117,234,267` and `RestoreTask.java:78,119,206`) is removed and replaced by this cooperative cancellation path. Verified in a WorkManager instrumented test: enqueue a backup or restore worker, cancel it mid-run, and assert the work reaches `WorkInfo.State.CANCELLED` with no further `setProgress` emissions after the cancellation point.

- [ ] AC-6: **[Otto progress bus removed from the execution path]** Given `BackupTask.java` currently imports `com.squareup.otto.Subscribe` (line 12) and calls `App.register(this)` / `App.post(state)` / `App.unregister(this)` (verified at lines 110, 255, 244), and `RestoreTask.java` does the same (verified at lines 18, 74, 213), when this story is complete, then neither `BackupWorker`, `RestoreWorker`, `BackupUseCase`, nor `RestoreUseCase` contains any import of `com.squareup.otto`, any call to `App.register`, `App.post`, or `App.unregister`, or any `@Subscribe`-annotated method. Progress is emitted exclusively via `setProgress` + `SchedulerState` flow (per CNTR-MODERNIZATION-004 §SchedulerState). Note: `App.post(BackupState)` call sites in `SmsBackupService` and `SmsJobService` that are not owned by this story are left untouched until DES-007 (Otto→StateFlow) lands; this story removes only the `BackupTask`/`RestoreTask` Otto coupling.

- [ ] AC-7: **[SmsJobService lifecycle bypass deleted]** Given `SmsJobService.onStartJob()` currently instantiates `SmsBackupService` directly (verified at `SmsJobService.java:82-84`: `new SmsBackupService(); service.attachBaseContext(this); service.handleIntent(...)`) to dodge the API-26 background-start restriction, when this story is complete, then `SmsJobService.java` is deleted from `app/src/main/java/com/zegoggles/smssync/service/`. No other class instantiates `SmsBackupService` outside of its normal Android lifecycle. A CI grep gate asserts zero matches for the class name `SmsJobService` in any Java or Kotlin source file under `app/src/main/`.

- [ ] AC-8: **[AsyncTask import eliminated]** Given `BackupTask.java` line 5 carries `import android.os.AsyncTask` and declares `class BackupTask extends AsyncTask<BackupConfig, BackupState, BackupState>`, and `RestoreTask.java` line 8 carries `import android.os.AsyncTask` and declares `class RestoreTask extends AsyncTask<RestoreConfig, RestoreState, RestoreState>`, when this story is complete, then both `BackupTask.java` and `RestoreTask.java` are deleted. A CI grep gate asserts zero matches for `import android.os.AsyncTask` or `extends AsyncTask` in any file under `app/src/main/java/com/zegoggles/smssync/service/`.

- [ ] AC-9: **[SchedulerState flow observable — ENQUEUED→RUNNING→SUCCEEDED/FAILED/CANCELLED]** Given `BackupScheduler.observe(jobKind)` (defined in CNTR-MODERNIZATION-004 §observe), when a backup or restore job transitions through WorkManager states, then the flow emits `SchedulerState.Enqueued`, `SchedulerState.Running(progress)` (at each `setProgress` call from the worker), and the appropriate terminal state (`Succeeded`, `Failed(cause)`, or `Cancelled`). Verified in a WorkManager instrumented test using `WorkManagerTestInitHelper.initializeTestWorkManager` + `SynchronousExecutor`: enqueue a backup, let it complete, and assert the collected flow emissions match the expected `Enqueued → Running(…) → Succeeded` sequence without any `App.post(BackupState)` / `@Subscribe` involvement.

- [ ] AC-10: **[Existing backup behaviors preserved: skip, auth retry, calendar sync]** Given the rewritten `BackupUseCase`, when it executes, then the following behaviors verified in `BackupTask` are preserved without regression: (a) the `SKIP` backup type path (verified at `BackupTask.java:125-127`, `skip()` method at `:207-218`) still marks the max-synced date for each type and returns a `FINISHED_BACKUP` equivalent without connecting to IMAP; (b) the XOAuth2 token-refresh retry path (verified at `BackupTask.java:183-205`, `handleAuthError()`) retries at most once after a 400-status auth failure; (c) when calendar sync is enabled (`preferences.isCallLogCalendarSyncEnabled()`) and CALLLOG items are backed up, `CalendarSyncer.syncCalendar(result)` is called once per CALLLOG batch (verified at `BackupTask.java:282-284`); (d) `setMaxSyncedDate` is called for each type after its messages are appended (verified at `BackupTask.java:285`).

- [ ] AC-11: **[Existing restore behaviors preserved: dedup guards and auth retry]** Given the rewritten `RestoreUseCase`, when it executes, then: (a) the `smsExists(values)` guard gates every SMS insert — the `!smsExists(values)` condition at `RestoreTask.java:259` is reproduced exactly, with the same `date + address + type` key; (b) the `callLogExists(values)` guard gates every call-log insert — the `!callLogExists(values)` condition at `:280` is reproduced exactly, with the same `date + number + duration + type` key; (c) the XOAuth2 token-refresh retry path at `RestoreTask.java:157-177` (at most one retry per restore run, passing `currentRestoredItem` as the resume offset) is preserved; (d) both `!config.restoreSms && !config.restoreCallLog` early-exit (`:85-86`) and thread-update-after-restore (`:129`) behaviors are preserved.

- [ ] AC-12: **[Unit test coverage — BackupUseCase and RestoreUseCase]** Given both use-cases live in the core module (pure Kotlin, no Android framework types), when the core module unit tests run (JVM, no emulator), then: (a) `BackupUseCaseTest` covers the happy-path backup loop (items > 0, messages appended, `FINISHED_BACKUP` result), the skip path (AC-10a), and the cancellation path (AC-5 — `cancel()` called on the coroutine scope mid-loop, `CANCELED_BACKUP` result); (b) `RestoreUseCaseTest` covers the happy-path restore loop, the `smsExists()` dedup bypass (a duplicate message does not produce a second insert), and the cancellation path; (c) all new tests pass under `./gradlew :core:test` with no emulator or instrumented test runner.

### Integration Criteria

- [ ] IC-1: `BackupWorker` is registered in `app/src/main/AndroidManifest.xml` as a `<provider>` or via the WorkManager `Configuration` (whichever pattern WorkManager 2.9.x requires for `@HiltWorker`) — confirm the exact registration path at implementation time; the `HiltWorkerFactory` must be the active `WorkerFactory` in the app's WorkManager `Configuration.Provider`.
- [ ] IC-2: `BackupWorker` and `RestoreWorker` are reachable from the `WorkManagerScheduler` adapter (U-014): `WorkManagerScheduler.scheduleImmediate()` and `scheduleRegular()` enqueue `BackupWorker`; `scheduleRestore(config)` enqueues `RestoreWorker`. Verify with an instrumented test that a `scheduleImmediate()` call results in a `WorkInfo` whose `workerClass` is `BackupWorker`.
- [ ] IC-3: All call sites that previously constructed `new BackupTask(service)` or `new RestoreTask(service, ...)` (located in `SmsBackupService` and `SmsRestoreService` respectively) are replaced. After this story, those service classes either delegate to the `BackupScheduler` port or are themselves scheduled and no longer act as direct task constructors. The compiler must report zero references to `BackupTask` or `RestoreTask` after deletion.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/BackupTask.java` | `AsyncTask<BackupConfig, BackupState, BackupState>` — runs the IMAP backup loop off the main thread; emits progress via `App.post(BackupState)` (Otto); cancellation via `AsyncTask.cancel(true)` + `isCancelled()` polling | Deleted; replaced by `BackupUseCase` (core, suspending) + `BackupWorker` (app, `@HiltWorker CoroutineWorker`) |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreTask.java` | `AsyncTask<RestoreConfig, RestoreState, RestoreState>` — runs the IMAP restore loop off the main thread; emits progress via `App.post(RestoreState)` (Otto); cancellation via `AsyncTask.cancel(true)` + `isCancelled()` polling; contains `smsExists()` and `callLogExists()` dedup guards | Deleted; replaced by `RestoreUseCase` (core, suspending) + `RestoreWorker` (app, `@HiltWorker CoroutineWorker`); dedup guards carried verbatim into `RestoreUseCase` |
| `app/src/main/java/com/zegoggles/smssync/service/SmsJobService.java` | `com.firebase.jobdispatcher.JobService`; `onStartJob` manually instantiates `SmsBackupService` (lines 82-84) to bypass API-26 background-start restriction; `onStopJob` posts `CancelEvent` to Otto bus; subscribes to `BackupState` via `@Subscribe backupStateChanged` to call `jobFinished` | Deleted entirely; `CoroutineWorker.doWork()` is the execution context — there is no `JobService` that hand-starts a `Service` |
| `core/src/main/java/.../service/BackupUseCase.kt` | Does not exist | Created: suspending backup use-case function extracted from `BackupTask.backupCursors()` + `fetchAndBackupItems()`; no Android scheduler types; fully unit-testable off-device |
| `core/src/main/java/.../service/RestoreUseCase.kt` | Does not exist | Created: suspending restore use-case function extracted from `RestoreTask.restore()`; dedup guards and auth-retry logic preserved; no `AsyncTask` or Otto |
| `app/src/main/java/.../service/BackupWorker.kt` | Does not exist | Created: `@HiltWorker CoroutineWorker`; thin adapter that calls `BackupUseCase.execute()`, emits `setProgress`, maps result to `Result.*` |
| `app/src/main/java/.../service/RestoreWorker.kt` | Does not exist | Created: `@HiltWorker CoroutineWorker`; thin adapter that calls `RestoreUseCase.execute()`, emits `setProgress`, maps result to `Result.*` |
| `app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java` | Constructs `new BackupTask(this)` and executes it via `execute(config)` | `BackupTask` construction removed; service either delegates to `BackupScheduler` or is removed from the direct-execution path (scope to be confirmed at implementation time via grep of `BackupTask` construction sites) |
| `app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java` | Constructs `new RestoreTask(this, converter, resolver, tokenRefresher)` and executes it | `RestoreTask` construction removed; analogous refactoring to `SmsBackupService` |

## Existing Behavior to Preserve

- The backup loop's `while (!isCancelled() && cursors.hasNext())` per-type iteration order and per-type `setMaxSyncedDate` update (verified at `BackupTask.java:267,285`) must be reproduced identically in `BackupUseCase`.
- The `SKIP` backup type early-return path (verified at `BackupTask.java:125-127`) must produce a `FINISHED_BACKUP`-equivalent result without any IMAP store interaction.
- The XOAuth2 token-refresh retry: one retry maximum, a new IMAP store object is constructed for the retry (verified at `BackupTask.java:192`: `config.retryWithStore(service.getBackupImapStore())`).
- `CalendarSyncer.syncCalendar(result)` called per CALLLOG batch when calendar sync is enabled.
- The restore `currentRestoredItem` cursor advancement: the index starts at `config.currentRestoredItem` (not 0) — verified at `RestoreTask.java:100,119` — supporting resume from a checkpoint offset.
- `smsExists()` dedup key: `date + address + type` (exactly three fields, verified at `RestoreTask.java:308-327`).
- `callLogExists()` dedup key: `date + number + duration + type` (exactly four fields, verified at `RestoreTask.java:288-306`).
- SMS restore limited to `MESSAGE_TYPE_INBOX` and `MESSAGE_TYPE_SENT` to avoid re-sending (verified at `RestoreTask.java:256-258`).
- Thread-update after any SMS is restored (`updateAllThreadsIfAnySmsRestored()`, verified at `RestoreTask.java:129`).
- The restore XOAuth2 retry passes `currentRestoredItem` as the resume point into `config.retryWithStore(currentRestoredItem, ...)` (verified at `RestoreTask.java:165`).
- The content-trigger → `scheduleIncoming()` two-stage debounce behavior that was in `SmsJobService.onStartJob(:72-77)` must already be handled by U-014's `WorkManagerScheduler`; this story only deletes `SmsJobService` — it does not re-implement that path.

## Verification Steps

1. **AC-1/AC-2 (use-case extraction compiles):** Run `./gradlew :core:compileKotlin`. Must succeed with zero errors. Confirm `BackupUseCase.kt` and `RestoreUseCase.kt` exist in the core module and contain no `import android.os.AsyncTask`, no `import com.squareup.otto`, and no `SmsBackupService`/`SmsRestoreService` reference (`grep -r "AsyncTask\|com.squareup.otto\|SmsBackupService\|SmsRestoreService" core/src/main/` must return no matches in these files).

2. **AC-3/AC-4 (worker thin-adapter compile and structure):** Run `./gradlew :app:compileDebugKotlin`. Confirm `BackupWorker.kt` and `RestoreWorker.kt` exist in the app module, are annotated `@HiltWorker`, extend `CoroutineWorker`, and their `doWork()` bodies contain no content-resolver access, no `BackupImapStore` construction, and no direct IMAP method calls (`grep -n "imapStore\|ContentResolver\|resolver\." app/src/main/java/.../BackupWorker.kt` must return no matches within `doWork()`).

3. **AC-5 (structured cancellation — instrumented test):** Run `./gradlew :app:connectedDebugAndroidTest --tests "*.BackupWorkerCancellationTest"`. The test enqueues a `BackupWorker` via `WorkManagerTestInitHelper`, lets it begin executing, calls `WorkManager.cancelUniqueWork(...)`, then asserts `getWorkInfoById(...).get().state == WorkInfo.State.CANCELLED`. Also assert that the final `setProgress` value observed before cancellation contains a partial (non-terminal) `currentItem` count, and that no progress emissions appear after the `CANCELLED` state.

4. **AC-6 (Otto removed from execution path — grep gate):** Run `grep -rn "com.squareup.otto\|App\.post\|App\.register\|App\.unregister\|@Subscribe" app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt core/src/main/java/`. Must return zero matches.

5. **AC-7 (SmsJobService deleted — grep gate):** Run `grep -rn "SmsJobService" app/src/main/`. Must return zero matches. Also confirm `SmsJobService.java` no longer exists: `ls app/src/main/java/com/zegoggles/smssync/service/SmsJobService.java` must return "No such file".

6. **AC-8 (AsyncTask eliminated — grep gate):** Run `grep -rn "import android.os.AsyncTask\|extends AsyncTask" app/src/main/java/com/zegoggles/smssync/service/`. Must return zero matches. Confirm `BackupTask.java` and `RestoreTask.java` no longer exist.

7. **AC-9 (SchedulerState flow — WorkManager test):** Run `./gradlew :app:connectedDebugAndroidTest --tests "*.SchedulerStateFlowTest"`. Test uses `WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration, SynchronousExecutor())`, enqueues a backup via `scheduler.scheduleImmediate()`, calls `WorkManagerTestInitHelper.getTestDriver().setAllConstraintsMet(workId)` to advance execution, collects `scheduler.observe(BROADCAST_INTENT).take(3).toList()`, and asserts the emission sequence is `[Enqueued, Running(...), Succeeded]`. Assert no `App.post(...)` was called during the test by verifying the Otto bus has zero subscribers (or by confirming the Otto bus is not initialized in the test scope).

8. **AC-10 (backup behavior regression — unit tests):** Run `./gradlew :core:test --tests "*.BackupUseCaseTest"`. All tests must pass. To verify the SKIP path specifically, temporarily remove the SKIP branch from `BackupUseCase` and confirm `BackupUseCaseTest.skipBackupType_doesNotConnectToImap` fails.

9. **AC-11 (restore dedup regression — unit tests):** Run `./gradlew :core:test --tests "*.RestoreUseCaseTest"`. All tests must pass. To verify the `smsExists` guard specifically, temporarily remove the `!smsExists(values)` guard from `RestoreUseCase` and confirm `RestoreUseCaseTest.duplicateSms_isNotInsertedTwice` fails. Restore the guard.

10. **AC-12 (core unit tests run without emulator):** Run `./gradlew :core:test`. All tests must pass with exit code 0. The test run must not require an Android emulator or device (confirmed by the absence of `connectedAndroidTest` in the Gradle task used). Confirm total test count for `BackupUseCaseTest` is at least 3 (happy path, skip path, cancellation) and for `RestoreUseCaseTest` is at least 3 (happy path, dedup bypass, cancellation).

11. **IC-1 (HiltWorkerFactory wiring — end-to-end compile):** Run `./gradlew :app:assembleDebug`. Build must succeed. In the built APK's `AndroidManifest.xml`, confirm `androidx.work.impl.WorkManagerInitializer` is removed (default initializer disabled) and the app's `Application` class implements `Configuration.Provider` returning a `Configuration` with `HiltWorkerFactory` set as the `workerFactory`.

12. **IC-3 (no remaining BackupTask/RestoreTask references — compile gate):** Run `./gradlew :app:compileDebugKotlin`. Must succeed with zero errors. Run `grep -rn "BackupTask\|RestoreTask" app/src/main/`. Must return zero matches (the classes are deleted; any remaining reference would be a compile error or dead import).

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android / Kotlin (core module) | `BackupUseCase.kt`, `RestoreUseCase.kt` — suspending use-case extraction, dedup guard preservation, cancellation via `ensureActive()`, unit tests | Developer |
| Android / Kotlin (app module) | `BackupWorker.kt`, `RestoreWorker.kt` — `@HiltWorker CoroutineWorker` thin adapters, `setProgress` emission, `Result.*` mapping, `HiltWorkerFactory` wiring, `SmsJobService.java` deletion | Developer |
| Android / Instrumented tests | AC-5 cancellation test, AC-9 SchedulerState flow test, IC-1 end-to-end wiring verification | Developer |

## Technical Notes

**`@HiltWorker` wiring is owned by U-024, not this story.** `BackupWorker` and `RestoreWorker` are introduced here as plain `CoroutineWorker`s, constructed by a stub/manual `WorkerFactory` for the duration of this story (marked `// TODO U-024`). The full `@HiltWorker` + `@AssistedInject` constructors (`@Assisted Context`, `@Assisted WorkerParameters`, injected core collaborators), the `HiltWorkerFactory`, and the `Configuration.Provider` wiring with the default WorkManager initializer removed from the manifest are all delivered by **U-024** (which depends on this story plus U-023). U-015 therefore has **no dependency on U-022** — the worker exists and runs via the manual factory; Hilt wiring is layered on later. This removes the U-015→U-022→U-020→U-015 cycle that an earlier draft introduced. The manual stub factory MUST be replaced by U-024 before the Hilt epic is considered complete.

**SmsJobService bypass removal scope.** This story deletes `SmsJobService.java` entirely (AC-7). Two behaviors previously owned by `SmsJobService` must be confirmed as handled elsewhere before deletion: (1) the content-trigger → `scheduleIncoming()` debounce (currently at `SmsJobService.java:72-77`) — this must be reproduced in the WorkManager content-trigger worker implemented in U-014; confirm U-014 landed before deleting `SmsJobService`; (2) the `onStopJob` → `CancelEvent` → Otto cancellation path (`:106`) — this is superseded by WorkManager's cooperative coroutine cancellation (AC-5); no explicit `CancelEvent` posting is needed once the `CoroutineWorker` scope is cancelled by WorkManager.

**`SmsBackupService` / `SmsRestoreService` residual scope.** After `BackupTask` and `RestoreTask` are deleted, `SmsBackupService` and `SmsRestoreService` lose their primary execution logic. The scope of what remains in those service classes after this story (versus what is removed as part of a later cleanup story) is **not fully determined** and must be grep-confirmed at implementation planning time. Minimally, the `new BackupTask(this).execute(config)` and `new RestoreTask(this, ...).execute(config)` call sites are removed by IC-3. What remains (lock acquisition, notification management, foreground service promotion) may move to `BackupWorker`/`RestoreWorker` or to a later story. Document the decision in Implementation Notes.

**Otto bus partial removal.** This story removes Otto from `BackupTask` and `RestoreTask` only. `SmsBackupService`, `SmsRestoreService`, `SmsJobService` (now deleted), and the UI layer still use Otto until DES-007 (Otto→StateFlow) lands. The temporary state is: workers emit through `setProgress`+`SchedulerState` flow; the services may still subscribe to Otto for other purposes. Do not rip out the Otto bus here — that is DES-007's scope.

**Backoff cap at the worker (INV-3).** WorkManager's internal backoff ceiling is `MAX_BACKOFF_MILLIS` (~5h), which exceeds the 300s cap inherited from `BackupJobs.java:205` (`newRetryStrategy(EXPONENTIAL, 30, 300)`). The 300s cap must be re-imposed at the worker level: if `doWork()` returns `Result.retry()` and the elapsed backoff duration (readable from `WorkerParameters.getRunAttemptCount()` combined with the exponential formula) would exceed 300s, the worker caps the effective retry by returning `Result.failure()` instead (or by scheduling a new `OneTimeWorkRequest` with a 300s cap via the `BackupScheduler` port). This is load-bearing (CNTR-MODERNIZATION-004 §INV-3); implement and verify with a unit test that constructs the worker at attempt count N and asserts `Result.failure()` is returned once the calculated backoff exceeds 300s.

**Dedup guard transplant fidelity.** The `smsExists()` and `callLogExists()` methods in `RestoreTask.java` query the content resolver with hard-coded projection and selection strings. When transplanting to `RestoreUseCase`, the exact column names (`Telephony.TextBasedSmsColumns.DATE`, `.ADDRESS`, `.TYPE`; `CallLog.Calls.DATE`, `.NUMBER`, `.DURATION`, `.TYPE`) and the `date = ? AND address = ? AND type = ?` / `date = ? AND number = ? AND duration = ? AND type = ?` SQL fragments must be preserved byte-for-byte. Any deviation silently changes dedup semantics and could cause duplicate messages or missed restores.

**`@AssistedInject` constructor shape.** Per DES-MODERNIZATION-005 §Hilt worker construction, `BackupWorker`'s constructor replaces the hand-construction at `BackupTask.java:61-88` (which pulls `authPreferences`, `fetcher`, `converter`, `calendarSyncer`, `tokenRefresher` from a `SmsBackupService` reference). The worker constructor takes `@Assisted Context` and `@Assisted WorkerParameters` (required by WorkManager) plus the injected collaborators. `RestoreWorker` similarly replaces `RestoreTask.java:61-70`. Map each collaborator to its Hilt-provided type; document any that require new `@Provides` or `@Binds` in a Hilt module in Implementation Notes.

## Supporting Documentation

- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md` — AC-2 (CoroutineWorker, no lifecycle bypass), AC-5 (removals)
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` — ADR-005-B (CoroutineWorker rewrite decision); §Components (BackupWorker, RestoreWorker, use-cases); §Hilt worker construction; §Design Validation (observability validation, removal verification Gate G3)
- `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` — §SchedulerState (observe flow shape); §Validation Rules rule 7 (structured cancellation); §Error Handling (retry/failure mapping)

## Integration Contract References

- CNTR-MODERNIZATION-004 §observe — `observe(jobKind): Flow<SchedulerState>` — defines the `SchedulerState` variants (`Enqueued`, `Running(progress)`, `Succeeded`, `Failed`, `Cancelled`) and the `BackupProgress` payload type that `setProgress` must emit; this story's AC-3(b), AC-4(b), and AC-9 are verified against this section.
- CNTR-MODERNIZATION-004 §Validation Rules rule 7 — structured cancellation clause: `cancel` MUST cooperatively cancel the running coroutine at the next suspension point and clear any scheduled retry; verified by AC-5.
- CNTR-MODERNIZATION-004 §INV-3 — backoff fidelity clause: effective retry delay capped at 300s; the worker-side cap described in Technical Notes implements this clause.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Draft content written and verified against live source (`BackupTask.java:5,12,50,110,117,234,267,282-285,292` — AsyncTask, Otto, isCancelled() polling, loop, calendarSyncer, setMaxSyncedDate; `RestoreTask.java:8,18,48,74,78,100,119,123,165,259,280,288-327` — AsyncTask, Otto, loop, currentRestoredItem, dedup guards; `SmsJobService.java:72-84,106` — content-trigger follow-up, lifecycle bypass, onStopJob CancelEvent). NOT finalized; the load-bearing open item for review is the `SmsBackupService`/`SmsRestoreService` residual scope decision — the implementer must grep-confirm every `BackupTask` and `RestoreTask` construction site across the full source tree and document which service-class responsibilities migrate to the workers vs. remain in a later cleanup story before this story is marked ready.
