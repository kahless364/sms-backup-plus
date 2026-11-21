---
type: bug
status: ready
artifact_type: bug
severity: low
priority: medium
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs:
  - DES-MODERNIZATION-014
  - DES-MODERNIZATION-005
integration_contracts:
  - CNTR-MODERNIZATION-004
dependencies: []
related_items:
  - U-067
  - BUG-022
  - BUG-021
  - U-070
  - BUG-023
  - U-069
  - BUG-018
  - U-015
  - U-031
  - U-065
  - REQ-MODERNIZATION-016
platforms: []
tags:
  - coroutines
  - workmanager
  - cancellation
  - error-handling
  - logging
  - restore
  - backup
id: BUG-024
title: A cancelled or stopped backup or restore is logged as an error and returned as Result.failure, because the workers' broad catches swallow the CancellationException
domain: build
origin: code-review
updated_at: '2026-11-21T11:47:30.915Z'
---

# BUG-024: A cancelled or stopped backup or restore is logged as an error and returned as Result.failure, because the workers' broad catches swallow the CancellationException

## Description

When a backup or restore run is cancelled (or stopped by WorkManager), the worker's loop stops by throwing a `CancellationException`. That exception is then caught by a broad catch in the same worker and handled as if it were a failure of the run: it is logged at error level and the worker returns `Result.failure(...)` with a reason. Three places do this:

| Path | Where the cancellation is caught | What is logged (priority, text) | What is returned |
|---|---|---|---|
| Backup (the loop `BackupWorker.backupCursors`) | `catch (e: Exception)` in `doWork` (`app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt:168-171`), after passing the four typed catches in `fetchAndBackupItems` (`:265-278`), none of which match | Error, "BackupWorker: unrecoverable error" (`:169`) | `Result.failure` with `KEY_FAILURE_REASON` = the exception's message, or "unknown" if it has none (`:170`) |
| IMAP restore (`RestoreWorker.runImapRestoreLoop`) | `catch (e: IllegalStateException)` inside the loop function (`app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt:348-351`); the same catch in `executeRestore` (`:272-274`) is a second net that is not reached from the loop | Error, "RestoreWorker: IllegalStateException (possible memory)" (`:349`) | `Result.failure` with reason "illegal_state" (`:350`) |
| Test-facing restore loop (`RestoreWorker.executeRestoreWithValues`) | `catch (e: IllegalStateException)` (`:433-435`) | Error, "RestoreWorker.executeRestoreWithValues: IllegalStateException" (`:434`) | `Result.failure` with reason "illegal_state" (`:435`) |

`kotlinx.coroutines.CancellationException` is `java.util.concurrent.CancellationException`, which extends `IllegalStateException`; that is why the restore catches, which were written for a memory problem, take it. A cancelled run is not an error and not a failure. The Kotlin rule is that a `CancellationException` is rethrown, not swallowed (structured concurrency; detekt names the pattern `SwallowedException`; citations in `sdlc/artifacts/build/sprints/sprint-023/U-067/plan.md`, section 2.0, "Anti-patterns named").

The same exception can reach these catches from places other than the loops' checks: a cancelled `setProgress` inside the same `try` blocks (`BackupWorker.kt:319`, `:325`, `:362`; `RestoreWorker.kt:232`, `:236`, `:321`, `:406`) lands in the same catches. The `setForeground` call at the start of each `doWork` is outside every `try` (`BackupWorker.kt:123`, `RestoreWorker.kt:148`) and is not affected.

### What this bug is, and what it is not

- **Since U-067 it is regular, not new.** Before story U-067 (commit `c12a2788`, BUG-022) the loops' own checks never threw (they read a job-less context), so a cancelled run reached these catches only through another suspension point, in practice `setProgress`. BUG-022's Suggested Approach says that in production `setProgress` usually suspends and so usually throws on a cancelled job, and that under the unit-test harness it never does (`sdlc/artifacts/stories/BUG-022-worker-cancellation-checks-do-nothing.md`, Suggested Approach, option 1 and the WorkManager facts above it; not re-read here). So the path existed and was taken "usually" in production; U-067 makes it certain at the top of every pass and makes it reachable in tests. U-067 did not add a catch.
- **It was left out of U-067 on purpose.** U-067's Scope Concern on cancelled runs returning `Result.failure`, ruled by the orchestrator (Record 15, rows 17 and 18): "not in this story. The new tests do not assert the `Result` type" (`sdlc/artifacts/build/sprints/sprint-023/U-067-worker-cancellation-checks-use-the-running-coroutine.md`, Scope Concerns and Implementation Notes). U-067's code review (I-4) and security review (W-1, W-2) found the behaviour independently and classified it "defer-justified". This bug is that deferred item.
- **A second, smaller observation is recorded as a related note, not as part of this bug's criteria** (a cancelled IMAP restore skips the thread refresh; see Scope Concerns SC-1).
- **A third, separate site with the same anti-pattern** is `WorkManagerCancelCollector` (see Scope Concerns SC-2). It is the line the 2026-11-21 emulator run logged. It is not a worker path and is not caused by U-067.

## Steps to Reproduce

None of these has been run for this artifact (Gradle and the device were not used, by instruction). (a) to (c) are unit-level and describe the tests the fix must add; what they show on today's code is read from the code. (d) is a device check, optional and not a gate.

Harness for (a) to (c): the one U-067's tests use (Robolectric, `@Config(sdk = [29])`, a child coroutine started `CoroutineStart.LAZY` and joined, cancelled from inside a transport or interceptor callback so that the pass in flight completes and the check at the top of the next pass throws). Files read at HEAD: `app/src/test/java/com/zegoggles/smssync/service/BackupWorkerLoopCancellationTest.kt`, `RestoreWorkerIntegrationTest.kt:370-457`, `RestoreWorkerCheckpointTest.kt:413-500`.

**(a) IMAP restore.**
1. Build a worker with `RestoreWorkerIntegrationTest.buildWorkerWithTransport` and the fixture `cancellableImapRun(itemCount = 4, cancelOnImportCall = 2)`.
2. Run `worker.doWork()` in the lazy child coroutine; join it.
3. Observe on today's code: the loop stops after two imports (U-067's assertions), and `run.result` holds `Result.Failure` with `KEY_FAILURE_REASON` "illegal_state"; the log (Robolectric `ShadowLog`, tag `SMSBackup+`) holds an error-priority "RestoreWorker: IllegalStateException (possible memory)" entry. U-067's test does not assert either; it asserts the effects only (`RestoreWorkerIntegrationTest.kt:372-377`).

**(b) Test-facing restore loop.** As (a) with `RestoreWorkerCheckpointTest` (`CancellingInterceptor(cancelAtIndex = 2)`, `worker.executeRestoreWithValues(...)`). Today the function returns `Result.Failure("illegal_state")` and logs the `executeRestoreWithValues` error line. U-067's test discards the return value (`:441-444`).

**(c) Backup.**
1. `backupCursors` is `internal` since U-067 and the catch is outside it, so a test through that seam cannot see the catch (`BackupWorkerLoopCancellationTest.kt:45`, the class comment "Not asserted: which Result a cancelled run returns").
2. The test must go through `doWork()`: `BackupWorkerIntegrationTest.buildWorkerWithTransport` (`BackupWorkerIntegrationTest.kt:86-88`) builds such a worker, and `doWork_noData_returnsSuccess` shows `doWork()` is callable under the harness. A cancelled run needs at least two rows reaching `backupCursors`, so the fixture needs either rows in a fake SMS provider (as `fakeProvider` does for restore) or a seam; that choice is the Solution Architect's (see Open Questions).
3. Observe on today's code (by reading): `Result.Failure` with the exception's message as the reason, and the error line "BackupWorker: unrecoverable error".

**(d) Device check (optional).** A debug build; start a manual backup with enough messages to run for a few passes; press the sync button to cancel (`StatusPreference.java:279-288` emits `SyncEvent.Cancel(USER)`; `WorkManagerCancelCollector` calls `cancelUniqueWork`). Read the log (tag `SMSBackup+`) for "BackupWorker: unrecoverable error". Same for restore ("RestoreWorker: IllegalStateException (possible memory)"). Not run.

## Expected Behavior

A run that is cancelled by the user, replaced by a newer enqueue of the same unique work, or stopped by the system is not logged as an error and is not reported by the worker as a failure with an error reason. It ends as a cancelled run, which is what CNTR-MODERNIZATION-004 says ("Cancellation is terminal, not failure. A structured-cancelled job emits `SchedulerState.Cancelled`, distinct from `Failed`", Error Handling, last bullet, `:287`). Cleanup (`closeFolders`, cursors) still runs. A genuine failure (a real `IllegalStateException`, an IO or other exception from the transport, converter or provider) is still caught, logged and reported exactly as today.

## Actual Behavior

A cancelled or stopped run is logged at error level as an "unrecoverable error" (backup) or a "possible memory" `IllegalStateException` (restore), and the worker returns `Result.failure` with a reason ("illegal_state", or the exception message). The effects of that on what the user and the app see are in Evidence, "What the result does".

## Evidence

### Facts confirmed against the code, read at `HEAD` `c12a2788` (via `git show HEAD:<path>` for the two worker files)

- Confirmed: the checks that throw are `currentCoroutineContext().ensureActive()` at `BackupWorker.kt:332`, `RestoreWorker.kt:311` and `:399` (U-067).
- Confirmed: backup. `fetchAndBackupItems` (`BackupWorker.kt:226-282`) catches `XOAuth2FailedException` (`:265`), `RequiresLoginException` (`:268`), `MailException` (`:272`), `SecurityException` (`:276`). `MailException` and `RequiresLoginException` extend `Exception` (`mail/transport/MailException.java:36`, `service/exception/RequiresLoginException.java:5`), `XOAuth2FailedException` extends `MailException` (`mail/transport/XOAuth2FailedException.java:28`). None is a `CancellationException`, so it passes through `finally { cursors?.close() }` (`:279-281`) to `doWork`'s `catch (e: Exception)` (`:168`). `doWork` catches `MailException` first (`:164`, returns `Result.retry()`), which a cancellation also does not match.
- Confirmed: backup cleanup. `backupCursors` closes the transport's folders in a `finally` (`:376-379`); `fetchAndBackupItems` closes the cursors in a `finally` (`:279-281`). Both run on a cancellation.
- Confirmed: IMAP restore. The check at `RestoreWorker.kt:311` is inside the `try` of `runImapRestoreLoop` (`:308-351`), so the cancellation is caught at `:348` and the function returns `Result.failure("illegal_state")`. The `executeRestore` catches (`:260-274`) are reached only by cancellations from `setProgress` at `:232` and `:236`, `checkSettings`, `openFolder` or `getMessages`, which are inside that `try` but outside the loop. `executeRestore`'s `finally { transport.closeFolders() }` (`:275-278`) runs on every path.
- Confirmed: test-facing restore. `executeRestoreWithValues` (`:378-438`), catch at `:433`. No `finally`; there is no transport.
- Confirmed: `doWork` of the restore worker also has `catch (e: Exception)` (`:198-200`, "unrecoverable error", `Result.failure(message)`); a cancellation that is not caught earlier would end there. Which exits reach it was not traced.
- Correction to the report that raised this: the report says the `executeRestore` catch "around `:272` is not reached". Confirmed for the loop path; it is reached for cancellations at the other suspension points listed above.

### What the result does (user-visible effect, traced)

1. **The worker reports to nobody directly.** Neither worker holds a `SyncStateRepository` (constructor parameters, `BackupWorker.kt:106-116`, `RestoreWorker.kt:120-135`) and neither emits a state event. `KEY_FAILURE_REASON` is read by no production class outside the workers (search of `app/src/main` for `KEY_FAILURE_REASON` and `failure_reason`: hits only in `BackupWorker.kt` and `RestoreWorker.kt`). So the "reason" is never shown.
2. **The UI is driven by `WorkInfo.State`, not by the `Result`.** Only a manual backup or restore started from the main screen is observed: `MainViewModel.observeBackupWork` and `observeRestoreWork` (`MainViewModel.kt:198-239`, started at `:146` and `:184`) map `WorkInfo.State` to `BackupState`/`RestoreState`: `FAILED` to `ERROR` with a generic `MailException("BackupWorker failed")` / `("RestoreWorker failed")`, `CANCELLED` to `CANCELED_BACKUP` / `CANCELED_RESTORE` (`:249-253`, `:289-293`); `StatusPreference` then shows the "canceled" label and details (`StatusPreference.java:213-217` restore, `:251-255` backup). No production code observes the automatic runs (`INCOMING`, `REGULAR`, `BROADCAST_INTENT`): a search of `app/src/main` for `WorkInfo` finds no observer outside `MainViewModel`, and `WorkManagerScheduler.observe` is a stub (next point).
3. **The contract's `SchedulerState.Cancelled` is not emitted by any production code.** `WorkManagerScheduler.observe` returns a constant `SchedulerState.Enqueued` and says no production caller uses it (`WorkManagerScheduler.kt:447-458`). The app-level realization of "emits Cancelled, distinct from Failed" is `WorkInfo.State.CANCELLED` mapped to `CANCELED_*` in `MainViewModel`. The contract itself says `SchedulerState` is mapped from `WorkInfo.State` (`CNTR-MODERNIZATION-004-backup-scheduler.md:228`, "Maps the WorkInfo lifecycle ENQUEUED -> RUNNING -> SUCCEEDED/FAILED/CANCELLED").
4. **WorkManager 2.9.1 discards the result of a stopped worker. Read in the 2.9.1 bytecode (local Gradle cache, `javap`), not run.** `WorkerWrapper.interrupt(int)` stores the stop reason, then cancels the result future and calls `ListenableWorker.stop`. The future listener (`WorkerWrapper$2.run`) calls `WorkerWrapper.onWorkFinished()` for each of its exits (a result, a `CancellationException` from the future, an `ExecutionException`). `onWorkFinished()` starts with `tryCheckForInterruptionAndResolve()`; when the wrapper was interrupted that method reads the work's state, calls `resolve(!state.isFinished)` and returns true, and `onWorkFinished()` returns without calling `handleResult`. So the `Result` a stopped worker returns is not stored and does not change the work's state. For a user cancel or a REPLACE the state is already finished (`CancelWorkRunnable` calls `WorkSpecDao.setCancelledState` and `Processor.stopAndCancelWork`; which happens first was not read), so the work stays `CANCELLED` and is not rescheduled. For a system stop (constraints lost, timeout) the state is not finished, so the work is rescheduled by the library whatever the worker returned. U-067's security review (I-7) and BUG-022's Suggested Approach state the same; U-067's contract verification marked it "library behaviour, not verified" (O-1); this reading supports it for 2.9.1 only.
5. **Net effect today, 2.9.1:** for a user cancel, a REPLACE and a system stop, the UI state and the scheduling outcome are right whatever the worker returns. What is wrong is the worker's own report: an error-level log line with a misleading cause ("possible memory", "unrecoverable"), and a `Result.failure` that nobody reads and the library drops. There is no data effect: cleanup runs, the watermark and checkpoint are unchanged by this path (U-067 security review I-1, I-2).
6. **Why the log line still matters.** Device checks use that line as the failure signature. U-065's emulator check treated a count of `BackupWorker: unrecoverable error` above 0 as a failure (`sdlc/artifacts/build/sprints/sprint-021/U-065/evidence/IC1-emulator.txt:23`, plan L18); BUG-021's and the runtime-library story's device checks read the same logs. After U-067 a cancelled run produces that line deterministically, so a check that cancels, or a run that a new enqueue replaces, shows a false failure.
7. **REPLACE makes it routine, not only a user action.** Every one-off enqueue uses `ExistingWorkPolicy.REPLACE` on its unique name (`WorkManagerScheduler.kt:154` INCOMING, `:289` REGULAR, `:324` BROADCAST_INTENT, `:365` MANUAL/SKIP, `:400` RESTORE; the trigger worker's own enqueue of INCOMING is described in BUG-023). A new incoming SMS during a running INCOMING backup replaces and so stops it. That REPLACE reaches the same stop path was not read in the bytecode (U-067 security review I-4 states it); it is the documented REPLACE semantics.

### What the contract says, and whether today's behaviour breaches it

Clauses (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md`):

- Error Handling, `:284`: "Worker failure -> retry under INV-3. A failed `doWork()` returns `Result.retry()` ... Non-retryable failures return `Result.failure()` and surface as `SchedulerState.Failed(cause)`."
- Error Handling, `:287`: "Cancellation is terminal, not failure. A structured-cancelled job emits `SchedulerState.Cancelled`, distinct from `Failed`."
- Validation Rule 7, `:279`: "`cancel` is structured. It MUST cooperatively cancel the running coroutine (unwinding at the next suspension point) and clear any scheduled retry".

Reading:

- **The contract leaves the worker's `Result` open for a cancelled run.** `Cancelled` is a `SchedulerState` mapped from `WorkInfo.State.CANCELLED` (`:226`, `:228`); a worker's `Result` has no cancelled value (success, failure, retry). The clause fixes what the job emits (state `Cancelled`, not `Failed`) and that cancellation is "terminal, not failure". It does not say what the worker returns or throws. Which mechanism meets it (rethrow, or return a result) is the Solution Architect's.
- **User-requested cancel.** As an emitted state the clause is met today (`CANCELLED` is set by WorkManager and the `Result` is dropped, Evidence point 4; not run on a device). As the worker's own report it is not: the worker logs an error and returns `Result.failure`, which is "failure", and `:284` reserves `Result.failure()` for non-retryable failures, which a cancel is not.
- **Replacement of unique work (REPLACE).** Same as the user cancel for the replaced run. There is no observer for automatic runs, so the effect is the log line.
- **Stop by the system (constraints, timeout).** The `:287` clause is about a structured-cancelled job; a system stop is not a `cancel` call. Under 2.9.1 the library reschedules and drops the result (Evidence point 4), so the work is not turned into a failure. Reporting it as an error in the log is still wrong. If a later library version handled the result of a stopped worker, a `Result.failure` would turn a system stop into a permanent failure, which would breach `:284` (a stop is not a non-retryable failure); see Not verified, item 3.
- **Conclusion.** Today's behaviour does not breach `:287` as an observable state on any of the three paths, on the strength of 2.9.1 library behaviour that is read, not run, and not guaranteed by the contract. It breaches the clause's meaning ("terminal, not failure") in what the worker itself reports, on all three paths. The state guarantee rests on a library detail, not on the worker, which is the reason the runtime-library story (S4, 2.12.0) makes this bug more than cosmetic.

### The emulator line "WorkManagerCancelCollector: error collecting cancel for MANUAL ... JobCancellationException"

Reported by the orchestrator from the 2026-11-21 emulator run after a normal backup; the log is not in the repository and was not re-read here. Read in the code: `WorkManagerCancelCollector.collect` launches a coroutine that waits on `repository.events.filterIsInstance<SyncEvent.Cancel>().first()` and wraps it in `catch (e: Exception)`, which logs "error collecting cancel for $uniqueWorkName" at warning level (`WorkManagerCancelCollector.kt:43-57`; the `Log.w` is at `:54`). `MainViewModel` cancels that coroutine when the work reaches a finished state (`MainViewModel.kt:209-212`, `:233-236`: `backupCancelCollectorJob?.cancel()`). The cancelled coroutine's `first()` throws `JobCancellationException`, which the broad catch logs. So the line is **a separate site of the same anti-pattern**, caused by the collector's own job being cancelled on a normal finish, not by a worker cancellation and not by U-067 (the collector is from story U-031). Its effect is one warning line on every manual run that ends. It is not part of this bug's acceptance criteria (SC-2).

## Severity and Priority

- **Severity: low (the scaffold's value is confirmed).** No data effect; cleanup runs; the UI and the scheduling outcome are right on 2.9.1 because the library drops the result (Evidence point 4); the worker returns a result that nobody reads. The visible effect is a misleading error-level log line, and the false failure signature in device checks (Evidence point 6). Not lowered further because the correct state rests on library behaviour that the next library raise may change (Not verified, item 3), and because "error" for a normal cancel misleads the owner when reading logs.
- **Priority: medium (the scaffold's value is confirmed).** It does not block a user. It should land before the runtime-library story S4 of DES-MODERNIZATION-014 (orchestrator ruling, recorded; the orchestrator places it), because S4 re-runs U-067's mutations and raises coroutines and WorkManager, and a worker that does not depend on the library dropping its result is the safer baseline for that.
- **Complexity: medium (the scaffold's value is kept).** The production change is small (three catches, possibly one in `WorkManagerCancelCollector`). The cost is in the tests: the backup catch is outside the seam U-067 opened, so reaching it needs a `doWork()`-level fixture with rows, or a seam; and the log assertion needs `ShadowLog`, which no test in the repository uses yet (search of `app/src/test` for `ShadowLog`).

## Sequencing (recorded; the orchestrator places it)

- Own small story, before S4 of DES-MODERNIZATION-014. U-067's four cancellation tests assert effects, not the `Result` type, so this fix does not edit them (U-067 plan section 7, item 4).
- **Not at the same time as story U-070 (the BUG-021 fix).** Both edit `BackupWorker.kt` and `RestoreWorker.kt`, in different lines but adjacent hunks. This bug edits `BackupWorker.kt:164-171` (`doWork` catches), possibly `:265-281` (`fetchAndBackupItems` catches) and `RestoreWorker.kt:194-201`, `:260-278`, `:348-351`, `:433-435`. BUG-021 edits `BackupWorker.kt:198-218` (SKIP), `:257-261` (first-backup sentinel, directly above the catches at `:265`), `:416-423` (`appendBatchAndUpdateWatermark`) and possibly the loop and selection, and `RestoreWorker.kt:464-468` (watermark update in restore), per `sdlc/artifacts/stories/BUG-021-delayed-otp-sms-skipped-by-backup-watermark.md`, Affected Code. Built together they conflict in the same files and share one test baseline (both extend `BackupWorkerIntegrationTest.kt` and `RestoreWorkerIntegrationTest.kt`), so build one after the other. Which is first is not decided here.
- BUG-023 (story U-069) does not touch these two files.

## Acceptance Criteria
The criteria state behaviour, not mechanism. The contract fixes one thing: a cancelled job is "terminal, not failure" and is distinct from `Failed` (CNTR-MODERNIZATION-004 `:287`); it leaves open what the worker returns or throws for a cancelled run (see Evidence, "What the contract says"). So no criterion says "rethrow" or "return X"; each says what must not be reported and what must still be true. "Cancelled" in AC-1 to AC-5 means: the job of the coroutine running the worker is cancelled, by the harness U-067's tests use (a lazily started child coroutine, cancelled from inside a transport or interceptor callback; `BackupWorkerLoopCancellationTest.kt`, `RestoreWorkerIntegrationTest.kt:370-457`, `RestoreWorkerCheckpointTest.kt:413-500`). "Not logged as an error" is checked with Robolectric's `ShadowLog` (tag `SMSBackup+`, error priority); "not reported as a failure" is checked on the value the worker function returns, when it returns one. Every test below is a new unit test; the Solution Architect fixes the fixtures.

- [ ] AC-1: Backup. When a backup run is cancelled during the loop, no error-priority log entry "BackupWorker: unrecoverable error" is written, and the run does not return `Result.Failure` (with any reason). Checked by a test that runs `doWork()` (not only `backupCursors`, because the catch is in `doWork`, `BackupWorker.kt:168`) with at least two rows and cancels during the second append, as U-067's test 1 does. Fails on HEAD.
- [ ] AC-2: IMAP restore. When an IMAP restore is cancelled during the second import (U-067's fixture `cancellableImapRun(itemCount = 4, cancelOnImportCall = 2)`), no error-priority entry "RestoreWorker: IllegalStateException (possible memory)" is written and `doWork()` does not return `Result.Failure` (in particular not reason "illegal_state"). Fails on HEAD.
- [ ] AC-3: Test-facing restore loop. When `executeRestoreWithValues` is cancelled after the third insert (U-067's `CancellingInterceptor(cancelAtIndex = 2)`), no error-priority entry from `executeRestoreWithValues` is written and the function does not return `Result.Failure`. Fails on HEAD.
- [ ] AC-4: What a cancelled run reports. In AC-1 to AC-3 the run ends as cancelled and not as a completed run: the job is cancelled (`isCancelled` true), and whatever the run returns, if it returns, is not `Result.Failure`, not `Result.Retry` (a cancel is terminal, `:287`, not a retryable failure, `:284`) and not a `Result.Success` that reports the run as finished (U-067's test 1 already asserts the last for backup, `BackupWorkerLoopCancellationTest.kt:103-105`). The contract leaves the worker's `Result` open for a cancelled run; the mechanism that meets this is the Solution Architect's, and the story names it. Checked by the same three tests. Where the observable state matters, the story also states whether it was shown with `WorkManagerTestInitHelper` (work enqueued, `cancelUniqueWork`, state `CANCELLED`, as `ic2_workManager_cancelsBackupWorkerRequest` does) or not shown at unit level.
- [ ] AC-5: Other cancellation points. A `CancellationException` that reaches the catches from a call other than the loop check (a transport call, `setProgress`) is treated as AC-1 to AC-4 require, in all four catch sites that can receive it: `BackupWorker.kt:168`, `RestoreWorker.kt:198`, `:272`, `:348`, `:433`. Checked by tests whose transport (or interceptor) throws a `CancellationException` from `checkSettings`, `openFolder` or `getMessages` (restore, reaching `:272`), from `appendMessages` (backup) and from `importMessageBody` (restore, reaching `:348`). Whether the test throws it with or without cancelling the job first is the Solution Architect's; the story states which and why.
- [ ] AC-6: Genuine failures are unchanged. With the job not cancelled: (i) a real `IllegalStateException` raised inside the IMAP restore loop, and inside `executeRestoreWithValues`, is still logged at error level with the same text and returns `Result.Failure` with reason "illegal_state"; (ii) a real `IllegalStateException` or an IO exception raised inside the backup loop is still logged "BackupWorker: unrecoverable error" at error level and returns `Result.Failure` with reason equal to its message ("unknown" if it has none); (iii) a real exception raised in `RestoreWorker.doWork` outside the loops still returns `Result.Failure` (`:198-200`). Checked by new tests. **No existing test pins (i) to (iii):** a search of `app/src/test` for `illegal_state`, `unrecoverable` and `KEY_FAILURE_REASON` finds only the backoff-cap tests (`BackupWorkerTest.kt:103`, `BackupWorkerIntegrationTest.kt:186`, `RestoreWorkerTest.kt:86`, `:105`, `RestoreWorkerIntegrationTest.kt:119`) and the auth reason (`RestoreWorkerIntegrationTest.kt:157`). So these tests are written first and shown green on HEAD before the fix, then green after; that is the proof that "exactly as today" holds.
- [ ] AC-7: The typed mappings are unchanged, and the existing tests that pin them pass without edits: `MailException` to `Result.retry()` (`BackupWorkerIntegrationTest.doWork_mailExceptionFromNoEnabledTypes_returnsRetry`, `doWork_mailExceptionFromTransportFactory_returnsRetry`; `RestoreWorkerIntegrationTest.doWork_mailExceptionFromTransport_returnsRetry`), `RequiresLoginException` to `Result.Failure("auth_failed")` (`RestoreWorkerIntegrationTest.doWork_requiresLoginException_returnsFailure`), the backoff cap to `Result.Failure("backoff_cap_exceeded")` (`BackupWorkerTest.inv3_backoffCap_atAttempt4_returnsFailure`, `BackupWorkerIntegrationTest.doWork_backoffCapExceeded_returnsFailureWithReason`, `RestoreWorkerTest.inv3_backoffCap_atAttempt4_returnsFailure`, `RestoreWorkerIntegrationTest.doWork_backoffCapExceeded_returnsFailureWithReason`), a failed import to `Success` (`RestoreWorkerIntegrationTest.doWork_withImapMessagesFailedImport_coversRestoreLoop`). Checked by running them and by `git diff` showing no edit to them.
- [ ] AC-8: Cleanup still runs on a cancelled run. `transport.closeFolders()` is called exactly once on each of the three paths (the counters U-067's tests already use: `run.closes`, `run.closeCalls`), the restore checkpoint is left where it was (not cleared), and the backup watermark is the date the last confirmed append returned (U-067's assertions). The closing of the backup cursors (`BackupWorker.kt:279-281`) is asserted if the `doWork()` fixture of AC-1 can observe it, and otherwise stated as read from the code and not observed (as U-067 ruling P-1 did).
- [ ] AC-9: U-067's seven tests pass unedited (`git diff` shows no edit to them): `BackupWorkerLoopCancellationTest` (2), `RestoreWorkerIntegrationTest` `doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass` and `doWork_imapRestore_notCancelled_restoresEveryItem`, `RestoreWorkerCheckpointTest` `executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass`, `executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing` and `executeRestoreWithValues_notCancelled_insertsEveryItem`. U-067's six mutations (U-067 plan section I6: M-1a, M-1b on `BackupWorker.kt:332`; M-2a, M-2b on `RestoreWorker.kt:311`; M-3a, M-3b on `RestoreWorker.kt:399`) are re-run per that protocol after the fix and are each still detected by the same tests as in the I6 table. Checked by the test run and by the mutation log kept with the story's evidence.
- [ ] AC-10: A run that is not cancelled behaves as today in every respect: the same results, reasons, log lines and cleanup. Checked by U-067's three control tests (`backupCursors_notCancelled_appendsEveryRow`, `doWork_imapRestore_notCancelled_restoresEveryItem`, `executeRestoreWithValues_notCancelled_insertsEveryItem`) and by AC-6 and AC-7.
- [ ] AC-11: Each new test of AC-1 to AC-5 fails on HEAD and passes after the fix (the story records the failing output), and fails again when the broad catch is restored in the form it has now (the story records that one-line change for each of the three catch sites). The AC-6 tests pass on both sides.
- [ ] AC-12: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` exits 0 with 0 failures and the skipped count not higher than before; the authoritative test count (`git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`) is taken before and after and the difference is explained by the new tests. No per-package JaCoCo floor is lowered.
- [ ] AC-13 (added by the Solution Architect): One positive line for a cancelled run. In the runs of AC-1 and AC-2, exactly one info-priority entry is written with tag `SMSBackup+` and text "BackupWorker: run cancelled" (backup) or "RestoreWorker: run cancelled" (IMAP restore). In AC-3's run (`executeRestoreWithValues`, not reached from `doWork`) no such entry is written. Checked by tests B1, R1 and C1 of the Suggested Approach §3.
- [ ] AC-14 (added by the Solution Architect): A `CancellationException` thrown while the job is **not** cancelled is handled as today. Raised from the backup append it gives `Result.Failure` with its message as the reason and the error line "BackupWorker: unrecoverable error". Raised from the IMAP import it gives `Result.Failure("illegal_state")` and the "possible memory" error line. WorkManager 2.9.1 and 2.12.0 both record such a run as FAILED (Suggested Approach §1). Checked by characterization tests B5 and R7, green on HEAD and after. Any test of U-070 that throws a `CancellationException` through the workers with the job active (U-070 plan, `ac9_cancelledDuringTheAppend_...`) passes unedited.
- [ ] AC-15 (added by the Solution Architect): A real exception (not a `CancellationException`) thrown while the job is already cancelled ends the run cancelled. The worker function throws a `CancellationException` and returns no `Result`. No error-priority entry is written. Exactly one warning-priority entry carries the original exception as its throwable. Checked by tests B6 (backup) and R8 (IMAP restore), red on HEAD.
- [ ] AC-16 (added by the Solution Architect; SC-2 ruled in): Cancelling the job returned by `WorkManagerCancelCollector.collect`, as `MainViewModel` does when a run finishes, when a new run starts and in `onCleared`, writes no "error collecting cancel" warning. A real error in the collector, with its job active, is still logged at warning with its throwable. Checked by tests K1 (red on HEAD) and K2 (green on HEAD) in a new `WorkManagerCancelCollectorTest`.

### Integration Criteria (if fix modifies component boundaries)

- [ ] IC-1: Every catch that can receive a `CancellationException` in the two workers is listed in the fix and handled or left on purpose: `BackupWorker.kt:168`, `RestoreWorker.kt:198`, `:272`, `:348`, `:433` (a search of `app/src/main` for `catch (e: Exception)`, `catch (e: IllegalStateException)`, `catch (e: Throwable)` and `runCatching` over the two worker files and the classes they call on the run path is attached; at HEAD the other catches in the two files name `MailException`, `XOAuth2FailedException`, `RequiresLoginException`, `SecurityException` and `TokenRefreshException`, none of which is a `CancellationException`). Solution Architect: the five sites are handled by `rethrowIfCancelled` (Suggested Approach §2). The typed catches are left on purpose: a typed exception thrown while the job is cancelled maps as today (AC-7), and the library ignores what a stopped worker returns (§1). The collector's catch (`WorkManagerCancelCollector.kt:53`) is the sixth site, ruled in under SC-2.
- [ ] IC-2: No signature, constructor, Hilt injection or `internal` seam changes (`BackupWorker` and `RestoreWorker` constructors, `backupCursors`, `executeRestoreWithValues`, `TestableBackupWorkerFactoryWithTransport`), so no caller needs an update. If a seam is added to reach the backup catch (AC-1), every caller and test double is updated in the same change and compile-checked. The user-facing cancel path (`StatusPreference.java:279-288` and `:305-313`, `WorkManagerCancelCollector.kt`, `MainViewModel.kt:146-147`, `:184-185`) is unchanged unless SC-2 is ruled in.

## Impact Analysis

Searches run for this artifact: `CancellationException`, `ensureActive`, `catch (` over the two worker files at HEAD, `KEY_FAILURE_REASON`, `failure_reason`, `WorkInfo` and `observe` over `app/src/main`, `SchedulerState`, `ShadowLog` and `illegal_state`/`unrecoverable` over `app/src/test`, `detekt` over the build files (no hit: no detekt configuration is in the repository), and the cancellation, `Cancelled`, `Result.failure` and non-retryable clauses of CNTR-MODERNIZATION-004. The two worker files were read with `git show HEAD:<path>`, not from disk. Counts are from these searches, not exhaustive greps of every layer.

| # | Layer | Affected? | Files and lines | Action needed |
|---|-------|-----------|-----------------|---------------|
| 1 | Requirements (`sdlc/artifacts/requirements`) | No change expected | REQ-MODERNIZATION-016 AC-8 (scheduler-contract tests pass unmodified or are listed) is the reason the timing matters; not re-read here. No requirement states the cancel result | None. If the plan finds a product statement about what a cancelled run shows, it raises a requirement change, not this bug |
| 2 | Design and contracts (`sdlc/artifacts/design`) | Check only | `contracts/CNTR-MODERNIZATION-004-backup-scheduler.md:226` (`Cancelled`), `:228` (mapped from `WorkInfo.State`), `:279` (rule 7), `:284`, `:287`; DES-MODERNIZATION-014 F-1 and its restated stop rule (`:1104-1138` per U-067's story) | The contract text is not changed: the clause is already right and the worker is brought to it. The Solution Architect decides whether `:287` should say what the worker returns for a cancelled run (it leaves that open), and whether `observe()` being a stub (`WorkManagerScheduler.kt:456-458`) needs a note; neither is required by the criteria |
| 3 | Stories, sprints, change records (`sdlc/artifacts/build`, `sdlc/artifacts/stories`) | Yes | U-067 (the story that made the path regular; its tests and six mutations must survive, AC-9); BUG-022; U-070 (BUG-021 fix, shares the two files); DES-MODERNIZATION-014 story S4 (raises WorkManager and coroutines; re-runs U-067's mutations) | Sequence per Sequencing |
| 4 | Application code (`app/src/main`) | Yes (root) | `service/BackupWorker.kt:164-171`, possibly `:265-281`; `service/RestoreWorker.kt:194-201`, `:260-278`, `:348-351`, `:433-435`; optionally `service/WorkManagerCancelCollector.kt:43-57` (SC-2) | AC-1 to AC-8, IC-1, IC-2 |
| 5 | Vendored library (`k9mail-vendored`) | No | None | None |
| 6 | Tests (`app/src/test`) | Yes | New tests in `service/BackupWorkerIntegrationTest.kt` (or a new class) for the backup `doWork()` path, `service/RestoreWorkerIntegrationTest.kt`, `service/RestoreWorkerCheckpointTest.kt`; U-067's tests untouched | AC-1 to AC-11 |
| 7 | Documentation (`README.md`, `sdlc/artifacts/engagement`) | Not examined | No search of the README for cancel wording was run | Not verified |

**Cascade analysis** (two layers: code and tests; the contract is the reference, not a change).

- Root layer: application code. Three broad catches take a `CancellationException` that belongs to the coroutine machinery.
- Propagation: code to tests (the first tests of the worker-level result of a cancelled run); no propagation to the scheduler, the UI or the library.
- Fix order: (1) the genuine-failure characterization tests (AC-6) written and shown green on HEAD; (2) the cancellation tests (AC-1 to AC-5) written and shown red on HEAD; (3) the fix; (4) all green, U-067's tests unedited (AC-9), mutations re-run, gate (AC-12).

**Interaction with other work.**

- **U-067 / BUG-022.** This bug starts where U-067 stopped. U-067's tests cancel the job and assert effects; they stay as they are. Its six mutations must still be detected (AC-9).
- **U-070 / BUG-021.** Same two files, adjacent hunks (see Sequencing). Not built at the same time.
- **S4 of DES-MODERNIZATION-014 (WorkManager 2.9.1 to 2.12.0, coroutines).** It re-runs U-067's mutations and these tests. The result-handling code of WorkManager changed between the two versions (Not verified, item 3); the tests of this bug are the guard for that.
- **BUG-018** (restore reports failure after success because `updateAllThreadsIfAnySmsRestored` throws "Unknown URL" on Android 16). Same function as SC-1 and the same family of "an exception in a worker is reported as the run's failure". Different cause. Relevant to SC-1: a refresh added to the cancel path would inherit that function's behaviour.

## Suggested Approach

Written by the Solution Architect, 2026-11-21. Code read at `HEAD` `2b927b21`. The commits after `c12a2788` are documents only: `git diff --stat c12a2788 HEAD -- app/` is empty. The two worker files were read with `git show HEAD:<path>`, not from disk. The tests were read the same way: `BackupWorkerLoopCancellationTest.kt`, `RestoreWorkerIntegrationTest.kt`, `RestoreWorkerCheckpointTest.kt`, `BackupWorkerIntegrationTest.kt`, `BackupWorkerTest.kt:223-237`. Also read: `WorkManagerCancelCollector.kt`, `activity/MainViewModel.kt:140-345`, `scheduler/WorkManagerScheduler.kt:446-458` and the CNTR-MODERNIZATION-004 clauses `:217-232` and `:279-287`. From the U-070 plan (`sdlc/artifacts/build/sprints/sprint-024/U-070/plan.md`), its edits P6 and P7 (`:1140-1209`) and its fixtures and its AC-9 tests (`:1329-1716`, `:2038-2067`). Library classes were read from the local Gradle cache, unpacked into a scratch folder outside the repository. Nothing was run: no Gradle, no device.

### 0. Rulings in brief

1. **The job's state decides, not the exception's type.** The first statement of each broad catch is `rethrowIfCancelled(...)` (§2). If the coroutine job running the worker is cancelled, the run ends by throwing: a `CancellationException` is rethrown as it is, and any other exception is logged once at warning level and then the job's `CancellationException` is thrown. If the job is active, the call returns and the catch handles the exception exactly as today, even when it is a `CancellationException`. Nothing returns a `Result` for a cancelled run. This holds on WorkManager 2.9.1 and 2.12.0 and on coroutines 1.7.3 and 1.11.0 (§1).
2. **Five call sites plus SC-2.** One new internal function, `service/WorkerCancellation.kt`, is called at `BackupWorker.kt:168` and at `RestoreWorker.kt:198`, `:272`, `:348` and `:433` (all five checked at HEAD). SC-2 adds one more catch clause in `WorkManagerCancelCollector.kt:53`. A cancelled run writes one info line at the `doWork` level and no error line. Cleanup is unchanged.
3. **Tests go through `doWork()`; no new seam.** The backup test reuses U-070's provider double and recording transport, which reach `backupCursors` through `doWork()` with the real converter (§3). Logs are asserted with Robolectric `ShadowLog`, which is on the test classpath. "Ends cancelled" is shown by the thrown exception, the cancelled job and the absent `Result`. `WorkInfo.State` cannot be shown under `TestListenableWorkerBuilder`. 18 new tests: 10 are red on HEAD and 8 are characterization tests, green on HEAD.
4. **SC-1 is out** (an item for the product owner, filed with BUG-018). **SC-2 is in** (same rule, one clause, two tests).
5. **Contract: no change is needed for this fix.** A non-breaking follow-up change record (v4) is named for the orchestrator: after this fix, before U-073 is planned (§5).
6. **Order: after U-070, in other words after sprint 024, and before U-071.** The hard limit is before U-073 (S4). This order needs no re-planning of U-070 (§6).

### 1. Rethrow or return: what the libraries do with each

**Library facts (each marked read or not read).** "Read" means read in this session: Kotlin sources from the `-sources.jar`, Java bytecode with `javap -c` from JDK 25.

| # | Fact | Source | Status |
|---|---|---|---|
| L1 | 2.9.1 `CoroutineWorker` lives in `work-runtime-2.9.1.aar`. `work-runtime-ktx-2.9.1.aar` holds no classes: its `classes.jar` has one version file. *Correction to Not verified item 2: there are no ktx classes to decompile.* | jar listings | read |
| L2 | 2.9.1 `CoroutineWorker`: `startWork$1` calls `doWork()`, then `future.set(result)`. Its catch of `Throwable` (exception table `36-53`, `60-82` to `85`) calls `future.setException(t)`. A listener added in the constructor runs on the serial task executor and calls `job.cancel()` once `future.isCancelled()`. `onStopped()` calls `future.cancel(false)`. | `CoroutineWorker.class`, `CoroutineWorker$startWork$1.class` | read (bytecode) |
| L3 | 2.9.1 `ListenableWorker.stop(int)` sets `mStopReason`, then calls `onStopped()`. | `ListenableWorker.class` | read |
| L4 | 2.9.1 `WorkerWrapper.interrupt(int)`, in order: `mInterrupted = reason`; `tryCheckForInterruptionAndResolve()`; `mWorkerResultFuture.cancel(true)`; if that future is cancelled, `mWorker.stop(reason)`. The wrapper is resolved **inside `interrupt`**, before the worker's future is cancelled. *Addition to Evidence point 4: the analyst placed the resolution in `onWorkFinished`. It already happens in `interrupt`, and `onWorkFinished` later returns without calling `handleResult`.* | `WorkerWrapper.class` | read |
| L5 | 2.9.1 `tryCheckForInterruptionAndResolve`: if the wrapper was interrupted, a null state gives `resolve(false)`; otherwise it calls `resolve(!state.isFinished())`. `resolve(true)` sets the state to `ENQUEUED`, sets the stop reason and calls `markWorkSpecScheduled(id, -1)`, so the work is rescheduled. | `WorkerWrapper.class` | read |
| L6 | 2.9.1 `WorkerWrapper$2.run` (the listener on the result future): a `CancellationException` from `get()` gives `Logger.info("<work> was cancelled")`; an `ExecutionException` gives `Logger.error("<work> failed because it threw an exception/error")`; `onWorkFinished()` runs on every exit. `mResult` is set to `Result.failure()` in the constructor. `AbstractFuture.getDoneValue` turns a `Failure` into an `ExecutionException`. So a `CancellationException` thrown from `doWork` while the worker is not stopped is recorded as FAILED. | `WorkerWrapper$2.class`, `WorkerWrapper.class`, `AbstractFuture.class` | read |
| L7 | 2.9.1 `CancelWorkRunnable.cancel` calls `iterativelyCancelWorkAndDependents` (state CANCELLED) **before** `Processor.stopAndCancelWork`. For REPLACE, `EnqueueRunnable` runs `CancelWorkRunnable.forName(...).run()` and then `WorkSpecDao.delete`. *Settles the order left open in Not verified item 2.* | `CancelWorkRunnable.class`, `EnqueueRunnable.class` | read |
| L8 | 2.9.1 `Processor.stopWork`, `stopAndCancelWork` → `interrupt`; which system callbacks call `stopWork` (`SystemJobService.onStopJob`, constraint tracking, timeouts) | the analyst's reading | not re-read |
| L9 | 2.12.0 `CoroutineWorker.startWork` = `launchFuture(context + Job()) { doWork() }` (`CoroutineWorker.kt:55-68`). `onStopped()` only calls `super`, so it does not cancel the future (`:124-126`). `launchFuture`: the future's cancellation listener cancels the job (`ListenableFuture.kt:39`); a `CancellationException` from the block calls `completer.setCancelled()`, any other throwable calls `setException` (`:41-48`). | `work-runtime-2.12.0-sources.jar` | read |
| L10 | 2.12.0 `WorkerWrapper.interrupt` = `workerJob.cancel(WorkerStoppedException(stopReason))` (`WorkerWrapper.kt:419-421`). The worker's future is awaited by `awaitWithin`, whose cancellation handler calls `worker.stop(reason)` first and then `cancel(false)` on the worker's future (`:662-670`). That `cancel` cancels the worker's coroutine job (L9). **So the stop reason is set before the worker's job is cancelled, and the wrapper's own continuation is already cancelled with the `WorkerStoppedException`**, because this is that continuation's handler. | same | read |
| L11 | 2.12.0 `launch`: `WorkerStoppedException` gives `Resolution.Stopped(reason)` (`:108-109`); any other `CancellationException` gives `Resolution.Failed`, with the comment "worker was self-cancelled, which we treat as failure" (`:110-112`); `runWorker` logs at info "`<work>` was cancelled" and rethrows (`:376-378`). `Stopped` calls `resetWorkerStatus`: a finished state or a null one changes nothing; any other state becomes `ENQUEUED` with the stop reason, which reschedules the work (`:423-451`). `Failed` calls `onWorkFailed`, which gives FAILED (`:408-416`). `ToContinuation` cancels the wrapper's continuation **without a cause** when the worker's future is found cancelled (`:680-683`). | same | read |
| L12 | 2.12.0 `CancelWorkRunnable.cancel`: `setCancelledState` (`CancelWorkRunnable.kt:35`) comes before `processor.stopAndCancelWork(..., STOP_REASON_CANCELLED_BY_APP)` (`:39`). REPLACE runs `forNameInline` (`EnqueueRunnable.kt:240`) and then deletes the records (`:247-249`); this is why `onWorkFinished` expects a null state "with a REPLACE" (`WorkerWrapper.kt:392-397`). `Processor.interrupt` calls `wrapper.interrupt(stopReason)` (`Processor.java:438-441`). | same | read |
| L13 | Coroutines 1.11.0: a child cancelled by its parent takes the parent's root cause when that cause is a `CancellationException` (`JobSupport.kt:736-746`). A continuation's cancellation cause is `parent.getCancellationException()`, which returns that root cause (`CancellableContinuationImpl.kt:266-267`, `:692-700`; `JobSupport.kt:412-419`). So the `WorkerStoppedException` reaches `awaitWithin` unchanged. Stack-trace recovery copies an exception only in debug mode (`StackTraceRecovery.kt:57`), and never one with extra fields such as `reason` (`ExceptionsConstructor.kt:53-54`). | `kotlinx-coroutines-core-jvm-1.11.0-sources.jar` | read |
| L14 | Coroutines 1.11.0: `CoroutineContext.isActive` = `get(Job)?.isActive ?: true` (`Job.kt:555-556`); `CoroutineContext.ensureActive()` throws the job's `getCancellationException()` when the job is not active (`:584-586`, `:602-604`). Coroutines 1.7.3: `JobKt` has the same `isActive(CoroutineContext)`, `ensureActive(Job)` and `ensureActive(CoroutineContext)`, and `ensureActive(Job)` throws `getCancellationException()` when `!isActive`. | 1.11.0 sources; 1.7.3 `javap` | read (1.7.3: those three methods only) |
| L15 | Coroutines 1.11.0 `runBlocking` rethrows the body's exception, a `CancellationException` included (`jvmMain/Builders.kt:68`, `throw it.cause`) | 1.11.0 sources | read; 1.7.3 not read |
| L16 | `ShadowLog` (Robolectric `shadows-framework-4.15.1`) has `getLogsForTag(String)`, `getLogs()`, `clear()` and `reset()`; each `LogItem` has `type` (the priority), `tag`, `msg` and `throwable` | `javap` | read; whether `reset()` is a `@Resetter` was not checked, so the tests call `clear()` in `@Before` |

**Trace of the three cancellations.** The worker's exit is the same in every row, because in both versions the wrapper decides the outcome of a stopped worker without asking the worker.

| Cancellation | 2.9.1 | 2.12.0 | Rethrow (this ruling) | Return `Result.failure` (today) |
|---|---|---|---|---|
| **User cancel** (`cancelUniqueWork`, by id) | CANCELLED is set first (L7); `interrupt` sees a finished state and calls `resolve(false)` (L4, L5). **CANCELLED, not rescheduled.** WM logs at debug "Work interrupted for ..." and at info "... was cancelled" (L6) | CANCELLED is set first (L12); `interrupt` cancels with a `WorkerStoppedException`; the wrapper's continuation resumes with it and gives `Stopped`; `resetWorkerStatus` sees a finished state and changes nothing (L10, L11). **CANCELLED, not rescheduled.** WM logs at info "... was cancelled" | Ignored: in 2.9.1 the future is already cancelled, so `setException` does nothing (L2); in 2.12.0 `setCancelled` on a cancelled completer does nothing (L9) | Ignored the same way (`set` does nothing) |
| **REPLACE** (every one-off enqueue: `WorkManagerScheduler.kt:154`, `:289`, `:324`, `:365`, `:400`) | Same as a user cancel, then the old record is deleted (L7). **CANCELLED, then deleted; the new request runs** | Same; the state is null after the delete, so `resetWorkerStatus` changes nothing (L11, L12) | Ignored | Ignored |
| **System stop** (constraints lost, quota, timeout, idle) | State RUNNING; `resolve(true)` gives **ENQUEUED: rescheduled by the library** (L5). The path from the system callback to `stopWork` is the analyst's reading (L8) | State RUNNING; `Stopped` gives `resetWorkerStatus`, then **ENQUEUED: rescheduled** (L11) | Ignored | Ignored |
| *Case 2: a `CancellationException` thrown while the job is active (no stop)* | Throwing it gives an `ExecutionException`, then `Logger.error`, then FAILED (L6) | Throwing it: `setCancelled`, then `ToContinuation` cancels the continuation with no cause, which gives `Failed` ("self-cancelled"), then **FAILED** (L9, L11) | Not done: with the job active the catch handles it as today and returns `Result.failure`, which gives **FAILED** on both versions | FAILED on both |

**Can the 2.12.0 "self-cancelled, treat as failure" branch be reached by a rethrow in any of the three cases? No.** It is reached only when the wrapper's continuation ends with a `CancellationException` that is not a `WorkerStoppedException`. That happens only through `ToContinuation` seeing the worker's future cancelled, or through the `isDone` fast path of `awaitWithin` (`:652`), while the wrapper's `workerJob` is not cancelled. In all three cases the worker's future and job are cancelled only from inside the cancellation handler of the wrapper's continuation, after `worker.stop(reason)` (L10). By then the continuation is cancelled with the `WorkerStoppedException` and cannot be resumed with any other value. The order is causal, so no thread race can change it. The branch is reached only in case 2, where the job is active. In case 2 this ruling returns a failure, as today, and gets FAILED, which is what the library would give anyway.

**Why rethrow, keyed on the job's state:**
(a) It is the structured-concurrency rule, and `ensureActive` in a catch is the coroutine-aware form of it. The kotlinx.coroutines guidance is cited in U-067 plan §2.0, not re-read here.
(b) `:284` keeps `Result.failure()` for non-retryable failures, and a cancel is not one.
(c) The worker's report matches the state the library records on both versions: cancelled or rescheduled after a stop, FAILED for case 2.
(d) **U-070's planned test `ac9_cancelledDuringTheAppend_nothingSaved_rowsUploadedByTheNextBackup` (U-070 plan `:2064-2067`) keeps passing.** It throws a `CancellationException` from the transport with the job active, through `runBlocking { worker.doWork() }` (`:1710`). A rethrow keyed on the type would make `runBlocking` rethrow it (L15) and fail that test. Keyed on the job's state, it is case 2 and returns `Result.failure`, as it does today.
(e) Returning a `Result` instead would need an invented value: there is no cancelled `Result`, `failure` is wrong (`:284`) and `retry` is wrong (`:287` "terminal").

**What still rests on reading alone, and the test for S4.** The rows above were read, not run. The test that turns them into evidence belongs in U-073 (S4), increment W, written first on 2.9.1 and re-run on 2.12.0; the orchestrator adds it to U-073, which is `ready`. It is a WorkManager-level test with the test WorkManager on a real executor (not `SynchronousExecutor`). A `BackupWorker` is built through `TestableBackupWorkerFactoryWithTransport` with a transport whose `appendMessages` blocks on a latch.
- (i) While it is blocked, call `cancelUniqueWork`, release the latch, wait for a finished `WorkInfo`: it must be `CANCELLED`, with no error-priority `SMSBackup+` line.
- (ii) While it is blocked, enqueue the same unique name with REPLACE: the old id's `WorkInfo` must be gone or `CANCELLED`, the new request must run, and there must be no error line.
- The system stop needs `Processor.stopWork`, which is a restricted API. It stays a reading here (L5, L11) plus S4's device check.
- First step of that test: a probe of whether `setForeground` runs under the real wrapper in Robolectric (not verified). If it cannot, S4 records the test as not feasible, and its device check carries the cancel case.

The fix is safe whatever that test shows. If a later WorkManager applied a stopped worker's outcome, a rethrow would be read as cancelled or stopped, and a returned `Result.failure` would be read as FAILED. Rethrowing is the safe choice either way.

### 2. The exact shape of the fix

**New file `app/src/main/java/com/zegoggles/smssync/service/WorkerCancellation.kt`.** One internal function in the workers' package. The two workers already import `currentCoroutineContext` and `ensureActive` (U-067), and they need no new import.

```kotlin
package com.zegoggles.smssync.service

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive

/**
 * BUG-024: the first statement of each broad catch on a worker's run path.
 *
 * If the job running the code is cancelled (the user's cancel, a REPLACE of the unique work,
 * a stop by the system), the run ends cancelled, whatever was thrown: this function throws and
 * the caller's failure handling (error log, Result.failure) does not run. A CancellationException
 * is rethrown as it is; any other exception is first logged at warning level, so it is not lost,
 * and the job's CancellationException is thrown. CNTR-MODERNIZATION-004, Error Handling:
 * "Cancellation is terminal, not failure."
 *
 * If the job is active, this returns, and the caller handles [e] as before, even when [e] is a
 * CancellationException: WorkManager (2.9.1 and 2.12.0) records a worker that ends with a
 * CancellationException while it was not stopped as FAILED, so the worker's report agrees.
 */
internal suspend fun rethrowIfCancelled(e: Exception, tag: String, site: String, logCancel: Boolean = false) {
    val context = currentCoroutineContext()
    if (context.isActive) return
    if (e !is CancellationException) {
        Log.w(tag, "$site: exception while the run was being cancelled; the run ends cancelled", e)
    }
    if (logCancel) Log.i(tag, "$site: run cancelled")
    if (e is CancellationException) throw e
    context.ensureActive()
}
```

**The five sites.** At each site one statement is inserted as the first line of the existing catch. Nothing else in the catch changes, and no catch is reordered or removed.

| Site (HEAD) | Inserted first statement | Why this `logCancel` |
|---|---|---|
| `BackupWorker.kt:168` (`doWork`, `catch (e: Exception)`) | `rethrowIfCancelled(e, TAG, "BackupWorker", logCancel = true)` | Outermost: every cancelled backup ends here, after `fetchAndBackupItems`' typed catches (`:265-278`) and its `finally` (`:279-281`) |
| `RestoreWorker.kt:198` (`doWork`, `catch (e: Exception)`) | `rethrowIfCancelled(e, TAG, "RestoreWorker", logCancel = true)` | Outermost: once the inner sites rethrow, every cancelled restore ends here |
| `RestoreWorker.kt:272` (`executeRestore`, `catch (e: IllegalStateException)`) | `rethrowIfCancelled(e, TAG, "RestoreWorker.executeRestore")` | Inner: without it, the exception rethrown by `:348` would be logged here as "possible memory" |
| `RestoreWorker.kt:348` (`runImapRestoreLoop`, `catch (e: IllegalStateException)`) | `rethrowIfCancelled(e, TAG, "RestoreWorker.runImapRestoreLoop")` | Inner: this is where the loop check `:311` lands |
| `RestoreWorker.kt:433` (`executeRestoreWithValues`, `catch (e: IllegalStateException)`) | `rethrowIfCancelled(e, TAG, "RestoreWorker.executeRestoreWithValues")` | Test-facing; not called from `doWork`, so no info line |

Each catch keeps its expression form. At `:168` and `:198` the catch is the value branch of `return try { ... }`, so its last expression is still the `Result.failure(...)`. At `:272`, `:348` and `:433` it still ends in `return Result.failure(...)`. Kotlin allows a suspending call in a catch block.

**Why `rethrowIfCancelled` and not `catch (e: CancellationException) { throw e }`.** The type form is the textbook form and it is smaller. Here it is wrong in two ways.
(1) Case 2 (a `CancellationException` thrown with the job active): the type form ends the run by throwing, and `runBlocking` in U-070's test rethrows it (§1 (d)). The library records FAILED in any case, so the type form gains nothing.
(2) Analyst's question 4 (a real exception while the job is cancelled): the type form logs it as "unrecoverable error" or "possible memory" at error level and returns `Result.failure`. The library then drops that result and records CANCELLED or ENQUEUED, so the device checks see the very false-failure line this bug removes. The job-state form agrees with the recorded state and keeps the exception at warning level.

**Ruling on question 4.** If a real `IllegalStateException` (or any exception caught by a broad catch) is thrown while the job is already cancelled, it is part of the cancel. The run ends cancelled; the exception is logged once, at warning, with its throwable, at the innermost site that catches it; no error line is written and no `Result` is returned. Only one warning line is written: the inner site logs it and throws the job's `CancellationException`, and the outer sites see a `CancellationException` and rethrow it without logging. A new criterion, AC-15, pins this.

**The typed catches are left as they are, on purpose.** These are `MailException` to retry at `BackupWorker.kt:164`, `:272` and `RestoreWorker.kt:194`, `:267`; `RequiresLoginException`, `SecurityException` and `XOAuth2FailedException` at `BackupWorker.kt:265-278` and `RestoreWorker.kt:260-266`; and `MailException` and `TokenRefreshException` in the two `handleAuthError` functions. A typed exception thrown while the job is cancelled still maps as today. It is not a cancellation; AC-7 pins those mappings; and both library versions ignore what a stopped worker returns (§1). IC-1 records this.

**What is logged, at which level** (tag `SMSBackup+`):

| Run | Error | Warning | Info |
|---|---|---|---|
| Cancelled backup or IMAP restore (cancel, REPLACE, stop) | none | none | one line: "BackupWorker: run cancelled" or "RestoreWorker: run cancelled" |
| Cancelled `executeRestoreWithValues` | none | none | none |
| A real exception while the job is cancelled | none | one line, with the throwable, from the innermost site | one line at the `doWork` level |
| Not cancelled: a genuine failure, or case 2 | exactly as today ("unrecoverable error", "IllegalStateException (possible memory)", "executeRestoreWithValues: IllegalStateException") | as today | as today |

WorkManager's own lines (tag `WM-WorkerWrapper`: "was cancelled" at info in both versions; "Work interrupted" at debug in 2.9.1) are not changed. WorkManager's minimum logging level is not configured in `app/src/main` (search for `setMinimumLoggingLevel`), so the library default applies; that default was not read.

**Cleanup is unchanged.** The change adds a statement inside existing catches and moves no `finally`.
- `backupCursors` still closes the transport's folders in its `finally` (`BackupWorker.kt:376-379`).
- `fetchAndBackupItems` still closes the cursors in its `finally` (`:279-281`); the `CancellationException` passes through it exactly as today.
- `executeRestore` still calls `transport.closeFolders()` in its `finally` (`RestoreWorker.kt:275-278`). A throw from its catch runs that `finally`.
- The restore checkpoint is cleared only on the success paths (`:340`, `:425`), which a cancelled run does not reach.
- The backup watermark is written only after a confirmed append (`:423`); the pass in flight completes and the next check throws.
- `updateAllThreadsIfAnySmsRestored` is skipped on a cancelled run, today and after the fix (SC-1).

**SC-2, ruled in.** In `WorkManagerCancelCollector.kt`, the first statement of the catch at `:53` becomes `rethrowIfCancelled(e, TAG, "WorkManagerCancelCollector")`. There is no info line, because the collector's job is cancelled on every normal finish (`MainViewModel.kt:211`, `:235`), on every new run (`:141`, `:177`, `:323`, `:331`) and in `onCleared` (`:344-345`). The coroutine then completes as cancelled, which is normal completion for `launch` in a `SupervisorJob` scope: no handler is called and nothing is logged. A real error in the collector, with its job active, is still logged at warning, as today. `MainViewModel` is not changed.

### 3. Testability

**Harness (U-067's, reused).** Robolectric with `@Config(sdk = [29])`. The worker function runs in a child started with `launch(start = CoroutineStart.LAZY)` and joined. The job is cancelled from inside a transport or interceptor callback, so the pass in flight completes. Each new cancellation test records the throwable: `try { run.result = worker.doWork() } catch (t: Throwable) { run.thrown = t; throw t }`. It then asserts that `run.thrown` is a `CancellationException`, that `run.result` is `null` (the run did not return) and that `workJob.isCancelled` is true. For a cancellation test whose transport throws, the callback cancels the job and then throws, e.g. `{ workJob.cancel(); throw CancellationException("test") }`. This is how a `setProgress` or other suspension point fails on a cancelled job in production. Under the harness `setProgress` never throws (U-067 plan, L5 to L7).

**Logs.** `ShadowLog` is on the test classpath (L16) and unused so far. No test in `app/src/test` observes logs today (search for `ShadowLog`). One small test helper, `app/src/test/java/com/zegoggles/smssync/service/WorkerLogs.kt`, provides `internal fun workerLogs(priority: Int) = ShadowLog.getLogsForTag(BackupWorker.TAG).filter { it.type == priority }`. Each new class calls `ShadowLog.clear()` in `@Before`. Absence is asserted as "no entry at `Log.ERROR` whose `msg` is the failure text". To be stricter, there must be no `Log.ERROR` entry with tag `SMSBackup+` at all during the run. The info line is asserted as exactly one entry with that `msg` at `Log.INFO`.

**The backup's `doWork`-level catch.** The test goes through `doWork()`, and no further seam is justified. The catch is in `doWork`, and the constructor already injects everything a test needs.
- **After U-070 (recommended order, §6):** reuse U-070's test fixtures in package `service`: `ProviderDouble`, `registerProvider`, `RecordingTransport` (with `beforeAppend`) and the worker factory shape of `LateSmsHarness.backup` (U-070 plan `:1662-1711`). Build the worker the same way, run it in the lazy child, and cancel in `beforeAppend` on the second append. The real `MessageConverter` runs over the double's rows. U-070's own probe N1 checks that this works, and U-070's tests depend on it.
- **If the order is reversed:** register a minimal SMS provider that returns N rows for any query, and give the worker a `WorkerEngineFactory` test double whose `createMessageConverter` returns U-067's `dateEchoConverter`. `WorkerEngineFactory` is a final Kotlin class (`WorkerEngineFactory.kt:60`), so that needs Mockito 5.18.0's inline mock maker; this was not run.
- **Cursor closing:** observable through `doWork()` only if the provider double hands out a cursor whose `isClosed` the test can read. U-070's double records queries, not cursors. AC-8's cursor clause is therefore stated as read from `BackupWorker.kt:279-281` and not observed, as U-067 ruling P-1 did, unless the plan finds otherwise.

**What a test can and cannot show about `WorkInfo.State`.** Under `TestListenableWorkerBuilder` there is no `WorkSpec` and no `WorkerWrapper`. The tests show the worker's own end: a thrown `CancellationException`, no `Result`, a cancelled job, the log lines and the cleanup counters. They cannot show `CANCELLED` versus `FAILED`, rescheduling, or WorkManager's own log lines. Those rest on §1 (read) and on S4's test. AC-4's "where the observable state matters" is answered as "not shown at unit level". `ic2_workManager_cancelsBackupWorkerRequest` (`BackupWorkerTest.kt:223-237`) cancels a request delayed by 60 s that never ran, so it shows nothing about a running worker.

**The tests.** 18 new tests. The restore tests go in the existing classes, to reuse their private fixtures: `cancellableImapRun`, `buildWorkerWithTransport`, `ImapRestoringTransport`, `CancellingInterceptor`, `buildWorker`, `createSmsItems`. Adding tests there does not edit U-067's seven tests (AC-9). The new classes are `BackupWorkerCancelledRunTest.kt` and `WorkManagerCancelCollectorTest.kt`. "Red" and "green" mean the result on HEAD; the characterization tests are written first and shown green on HEAD before any production edit.

| # | Test (class) | Fixture and what it asserts | AC | HEAD |
|---|---|---|---|---|
| B1 | `doWork_cancelledDuringSecondAppend_endsCancelledWithoutErrorLine` (BackupWorkerCancelledRunTest) | 4 SMS rows; cancel in `beforeAppend` on append 2. Asserts: thrown `CancellationException`, `result == null`, job cancelled; no error entry, one info "BackupWorker: run cancelled"; 2 appends, watermark = row 2's confirmed date, `closeFolders` once | AC-1, AC-4, AC-8, AC-13 | red |
| B2 | `doWork_cancellationThrownByAppend_endsCancelled` | cancel the job and throw a `CancellationException` from append 1. Asserts: as B1, watermark unchanged | AC-5 | red |
| B3 | `doWork_illegalStateFromAppend_notCancelled_unrecoverableFailure` | `IllegalStateException("boom")` from append 1, job active. Asserts: `Failure`, reason "boom", one error "BackupWorker: unrecoverable error" with that throwable | AC-6 (ii) | green |
| B4 | `doWork_exceptionWithoutMessage_notCancelled_reasonUnknown` | `java.io.IOException()` from append 1. Asserts: `Failure`, reason "unknown", error line | AC-6 (ii) | green |
| B5 | `doWork_cancellationExceptionWhileActive_isAFailureAsToday` | `CancellationException("x")` from append 1, job **not** cancelled. Asserts: `Failure`, reason "x", error line | AC-14 | green |
| B6 | `doWork_realExceptionWhileCancelled_endsCancelledWithOneWarning` | `{ workJob.cancel(); throw IllegalStateException("late") }` from append 2. Asserts: thrown `CancellationException`, no error entry, exactly one warning entry whose throwable is that exception | AC-15 | red |
| R1 | `doWork_imapRestore_cancelledDuringSecondImport_endsCancelledWithoutErrorLine` (RestoreWorkerIntegrationTest) | `cancellableImapRun(itemCount = 4, cancelOnImportCall = 2)`. Asserts: thrown `CancellationException`, `run.result == null`; no "possible memory" or "unrecoverable" error entry; one info "RestoreWorker: run cancelled"; rows 2, checkpoint 1, `closeCalls` 1 | AC-2, AC-4, AC-8, AC-13 | red |
| R2 | `doWork_imapRestore_cancellationFromCheckSettings_endsCancelled` | the transport's `checkSettings` cancels and throws; the exception reaches `:272`. Asserts: as R1, 0 rows, `closeCalls` 1. `openFolder` and `getMessages` are in the same `try` (`RestoreWorker.kt:231-258`) and need no separate test | AC-5 | red |
| R3 | `doWork_imapRestore_cancellationFromImport_endsCancelled` | the import on call 2 cancels and throws, so it reaches `:348` without the loop check | AC-5 | red |
| R4 | `doWork_imapRestore_cancellationFromTransportFactory_endsCancelled` | the `MailTransportFactory` lambda cancels and throws; the exception reaches `:198` directly | AC-5 | red |
| R5 | `doWork_imapRestore_illegalStateFromImport_notCancelled_illegalState` | `IllegalStateException` from import 2, job active. Asserts: `Failure("illegal_state")`, one "IllegalStateException (possible memory)" error | AC-6 (i) | green |
| R6 | `doWork_exceptionFromTransportFactory_notCancelled_unrecoverableFailure` | `IllegalArgumentException("no transport")` from the factory. Asserts: `Failure("no transport")`, "RestoreWorker: unrecoverable error" | AC-6 (iii) | green |
| R7 | `doWork_imapRestore_cancellationExceptionWhileActive_isIllegalStateAsToday` | `CancellationException` from import 2, job not cancelled. Asserts: `Failure("illegal_state")`, error line | AC-14 | green |
| R8 | `doWork_imapRestore_realExceptionWhileCancelled_endsCancelledWithOneWarning` | import 2 cancels and throws `IllegalStateException`. Asserts: thrown `CancellationException`, one warning from "RestoreWorker.runImapRestoreLoop", no error | AC-15 | red |
| C1 | `executeRestoreWithValues_cancelledAfterThirdInsert_endsCancelledWithoutErrorLine` (RestoreWorkerCheckpointTest) | `CancellingInterceptor(cancelAtIndex = 2)`. Asserts: thrown `CancellationException`, no return value, no `executeRestoreWithValues` error, no info line; rows 3, checkpoint 2 | AC-3, AC-4 | red |
| C2 | `executeRestoreWithValues_illegalStateFromInterceptor_notCancelled_illegalState` | an interceptor that throws `IllegalStateException` at index 1. Asserts: `Failure("illegal_state")`, error line | AC-6 (i) | green |
| K1 | `collect_jobCancelledWhileWaiting_logsNothing` (WorkManagerCancelCollectorTest) | a repository whose `events` never emits (`MutableSharedFlow`); cancel the returned `Job`, join. Asserts: cancelled, no "error collecting cancel" warning | AC-16 | red |
| K2 | `collect_eventsFlowFails_logsWarning` | `events` whose `collect` throws `IllegalStateException`. Asserts: one warning with that throwable | AC-16 | green |

The repository for K1 and K2 is a Mockito mock of the `SyncStateRepository` interface (`SyncStateRepository.kt:17`) with `events` stubbed. The collector runs on `Dispatchers.IO`, so each test joins the returned job before it reads `ShadowLog`.

**Mutations** (protocol as U-067 I6: pristine copies, hash check, `git status --porcelain` after each restore). Each is one change to one line.

| Id | Mutation | Must turn red |
|---|---|---|
| M-1 | delete the inserted call at `BackupWorker.kt:168` (the catch as it is now) | B1, B2, B6 |
| M-2 | the same at `RestoreWorker.kt:198` | R1, R2, R3, R4, R8 (every cancelled restore now ends there) |
| M-3 | the same at `:272` | R1, R2, R3, R8 |
| M-4 | the same at `:348` | R1, R3, R8 |
| M-5 | the same at `:433` | C1 |
| M-6 | in `rethrowIfCancelled`, replace `if (context.isActive) return` with `if (e !is CancellationException) return` (the type rule) | B5, R7 (case 2), B6, R8 (case 3) |
| M-7 | `logCancel` ignored (delete the `Log.i` line) | B1, R1 |
| M-8 | delete the inserted call in `WorkManagerCancelCollector.kt` | K1 |

M-1 to M-5 are AC-11's "restore the broad catch in the form it has now", one per site. U-067's six mutations (M-1a to M-3b) are re-run unchanged (AC-9). The new call is inside the catches, not on U-067's check lines, so each is still caught by the same U-067 tests.

**Count.** 18 new `@Test`s, all additions. The authoritative count from CLAUDE.md is taken before and after.

### 4. Scope Concerns

- **SC-1: out.** The fix does not change whether the thread refresh runs on a cancelled IMAP restore: it was skipped before and is skipped after. Whether a cancelled restore should refresh the conversations is a behaviour the user can see, and no contract or approved requirement settles it, so it goes to the product owner (list below). It belongs with BUG-018 (the same function, `updateAllThreadsIfAnySmsRestored`, which throws "Unknown URL" on Android 16): a refresh added to the cancel path would take on that failure.
- **SC-2: in.** It is the same rule and one inserted statement, and it removes a false-alarm line that the device checks now read. Its fix and tests are in §2 and §3 (K1, K2, M-8) and its criterion is AC-16. IC-2's clause "unchanged unless SC-2 is ruled in" applies: `WorkManagerCancelCollector.kt` changes, `MainViewModel.kt` does not.

### 5. Contract (CNTR-MODERNIZATION-004)

**No change is needed for this fix to conform.**
- `:287` already says what must hold: "Cancellation is terminal, not failure". After the fix the worker no longer contradicts it: no `Result.failure`, no error line.
- `:284` reserves `Result.failure()` for non-retryable failures, and the fix stops returning it for a cancel.
- `:279` (Rule 7) is met by U-067's checks together with the library cancelling the job.

**Follow-up for the orchestrator** (not in the fix story): a non-breaking change record (v4, no signature, `SchedulerState` variant or invariant changed). Raise it after this fix lands and before U-073 (S4) is planned, so that S4's contract verification checks it. It does two things:
1. It adds to Error Handling, after `:287`: "A worker whose job is cancelled ends by rethrowing the `CancellationException`; it returns no `Result`. WorkManager records `CANCELLED` for a cancel or a REPLACE and re-enqueues the work after a system stop; neither surfaces as `Failed`."
2. It records, as a known gap, that `WorkManagerScheduler.observe` is a stub that returns `SchedulerState.Enqueued` (`scheduler/WorkManagerScheduler.kt:456-458`). So `:285` ("Failures surface to consumers via `observe(jobKind)` emitting `SchedulerState.Failed`") and `:287` ("emits `SchedulerState.Cancelled`") are met today only by `MainViewModel`'s mapping of `WorkInfo.State` (`MainViewModel.kt:198-239`), and only for manual runs.

Whether to implement `observe` or reword the two clauses is that record's decision.

### 6. Order and files

**Recommendation: after U-070 (the end of sprint 024), as the first story of the next sprint, before U-071.** The hard limit is before U-073 (S4), whose preconditions already require BUG-022 and BUG-021 first (DES-MODERNIZATION-014 `:616`).

- **Building BUG-024 before U-070 would force U-070 to be re-planned.** U-070's plan is PASS and was written against the code after U-067. Its baseline step checks that the history of the two workers since `c9770c30` holds only U-067's commits (U-070 plan `:2987`, `:3006`). Its mutation protocol keeps pristine copies and hashes of `BackupWorker.kt` and `RestoreWorker.kt` (`:3092-3123`), and its line citations are taken at that state. A BUG-024 commit in between breaks all three.
- **Building BUG-024 after U-070 costs nothing extra,** because BUG-024 is not planned yet, and U-070's edits move none of BUG-024's five sites. U-070's backup edits are below `:198` (P6a at the SKIP `try` `:206-211`, P6b at `:356-357`, P6c at `:423`) and its restore edit is at `:461` (P7), all after `:168`, `:198`, `:272`, `:348` and `:433`.
- **Bonus:** BUG-024 can then reuse U-070's provider double and recording transport for the backup `doWork()` test.
- **Between U-070 and U-071:** U-071 raises the toolchain and the test tooling. Writing BUG-024's tests on the current Robolectric 4.15.1, coroutines 1.7.3 and WorkManager 2.9.1 keeps the "red on HEAD" proof free of tooling changes, and S2 to S4 then re-run 18 more tests as guards.

**What BUG-024's plan must re-read at U-070's closing commit:**
1. both worker files (re-derive every line number, expected unchanged);
2. U-070's test fixtures (`LateVisibleSmsFixtures.kt`, `ProviderDouble`, `RecordingTransport`, the `LateSmsHarness` factory) as built, not as planned;
3. U-070's tests that throw through the workers' catches, in particular `ac9_cancelledDuringTheAppend_...`. It must pass unedited: it is case 2, and B5 pins the same rule. Add U-070's tests to AC-9's "pass unedited" list;
4. U-070's mutation table, so that BUG-024's mutations M-1 to M-8 do not overlap U-070's protected lines;
5. the U-067 test comment `RestoreWorkerCheckpointTest.kt:454-458` ("the loop's IllegalStateException catch turns it into a Result"). It becomes stale after this fix, but AC-9 forbids editing that test, so the comment is listed in the implementation log as a known stale comment, to be corrected by a later change that may edit it.

U-069 does not touch the two worker files and is not affected.

### 7. For the product owner

- **PO-1 (SC-1): should a cancelled restore refresh the conversation threads?** Today, and after this fix, it does not: rows restored by a cancelled run can sit outside their conversation until a later run refreshes. *Recommendation: no refresh on cancel. Record the question with BUG-018, whose fix owns the refresh function and its Android 16 failure.*
- **PO-2: the device check (d).** It is optional and not a gate. *Recommendation: run it once on the Android 16 emulator after the fix. It needs a debug build and a manual backup cancelled with the sync button. Expected: no "unrecoverable error" or "possible memory" line, one "run cancelled" info line, no "error collecting cancel" warning, and the "canceled" label as today.*

### 8. What the Solution Architect could not verify

1. Nothing was run. The library behaviour in §1 was read in sources or bytecode, and the tests' red and green results are predictions.
2. 2.9.1 `Processor.stopWork` and `stopAndCancelWork` were not re-read, nor which system callbacks call them in either version (L8).
3. 1.7.3 `runBlocking` and `JobSupport` were not read; only `isActive` and `ensureActive` were (L14, L15).
4. Whether `ShadowLog.reset()` is a `@Resetter`; the tests call `clear()` either way (L16).
5. Whether the real `MessageConverter` converts U-070's provider rows under Robolectric (U-070's own probe N1), and whether Mockito can mock `WorkerEngineFactory` (only needed in the reversed order).
6. Whether a `BackupWorker` runs under the test WorkManager with the real wrapper in Robolectric (`setForeground` through the real foreground updater). That is the first step of S4's test (§1).
7. The kotlinx.coroutines guidance on `ensureActive` in catch blocks was not re-read in this session (U-067 plan §2.0 cites it).
8. WorkManager's default minimum logging level was not read (§2).

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` (`:164-171`, `:265-281`) | `doWork`'s `catch (e: Exception)` logs "unrecoverable error" and returns `Result.failure(message)` for a `CancellationException` | A cancellation is not logged or reported as a failure (AC-1, AC-4, AC-5). Mechanism: the Solution Architect's |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt` (`:348-351`, `:272-274`, `:198-200`, `:433-435`) | The `IllegalStateException` and `Exception` catches log an error and return `Result.failure("illegal_state")` or the message for a `CancellationException`; `updateAllThreadsIfAnySmsRestored` is skipped (SC-1) | As above (AC-2, AC-3, AC-4, AC-5) |
| `app/src/main/java/com/zegoggles/smssync/service/WorkerCancellation.kt` (new, Solution Architect) | Does not exist | One internal function, `rethrowIfCancelled`, called first in each broad catch (Suggested Approach §2) |
| `app/src/main/java/com/zegoggles/smssync/service/WorkManagerCancelCollector.kt` (`:53-55`) | Logs a warning for its own cancellation on every finished, replaced or cleared manual run | SC-2 ruled in: `rethrowIfCancelled` first in the catch (AC-16) |
| `app/src/test/java/com/zegoggles/smssync/service/BackupWorkerCancelledRunTest.kt`, `WorkManagerCancelCollectorTest.kt`, `WorkerLogs.kt` (new, Solution Architect) | Do not exist | Tests B1 to B6, K1 and K2, and the `ShadowLog` helper (Suggested Approach §3) |
| `app/src/test/java/com/zegoggles/smssync/service/BackupWorkerIntegrationTest.kt`, `RestoreWorkerIntegrationTest.kt`, `RestoreWorkerCheckpointTest.kt` (new tests only) | No test asserts the result or the log of a cancelled run, or of a genuine `illegal_state` / "unrecoverable" failure | New tests (AC-1 to AC-6) |

## Existing Behavior to Preserve

- A genuine failure is caught, logged at error level with today's text, and returned as today: `Result.failure("illegal_state")` for a real `IllegalStateException` in the restore loops, `Result.failure(message or "unknown")` for a real exception in the backup loop and in `RestoreWorker.doWork` (AC-6).
- `MailException` to `Result.retry()`; `XOAuth2FailedException` to the token-refresh retry; `RequiresLoginException` to `"auth_failed"`; `SecurityException` to `"missing_permission"`; the 300-second backoff cap to `"backoff_cap_exceeded"` (AC-7).
- Cleanup on a cancelled run: `closeFolders` once, cursors closed, restore checkpoint not cleared, backup watermark equal to the last confirmed append (AC-8; U-067 security review I-1, I-2).
- The cancellation checks themselves and the "stops at the next pass" behaviour (U-067's seven tests, AC-9).
- The path from the sync button to `cancelUniqueWork` and to the `CANCELED_*` state (IC-2).
- A run that is not cancelled behaves exactly as today (AC-10).
- The `Log.d`/`Log.i`/`Log.w` lines of the workers that the device checks of other stories read ("starting backup", "backup complete", "nothing to backup", "starting restore"); only the error line for a cancelled run goes.

## Verification Steps

1. AC-1 to AC-3: run the new cancellation tests on HEAD (red: each fails on the log assertion or on the `Result.Failure` assertion), then on the fix (green). Read the failing output into the story's evidence.
2. AC-4: read the story's statement of the mechanism and of how the cancelled state was shown (unit state `CANCELLED`, or "not shown at unit level"); confirm it does not contradict CNTR-MODERNIZATION-004 `:284` and `:287`.
3. AC-5: run the tests that throw a `CancellationException` from a transport call; confirm each of `BackupWorker.kt:168`, `RestoreWorker.kt:198`, `:272`, `:348`, `:433` is covered by at least one test (the story lists which test reaches which site).
4. AC-6: run the characterization tests on HEAD before the fix and record that they are green; run them after.
5. AC-7 and AC-10: run the named existing tests; `git diff HEAD -- app/src/test` shows only added tests.
6. AC-8: read the `closeFolders` counts in the new tests; for the backup cursors, read `BackupWorker.kt:279-281` and the story's statement.
7. AC-9: `git diff` shows no edit to U-067's seven tests; the mutation log shows M-1a to M-3b each detected by the same tests as the I6 table, with the restore protocol (`restored` line, hash and porcelain checks) as in U-067.
8. AC-11: for each of the three catch sites, restore the old broad catch in one line, run the new tests, see them fail, restore.
9. AC-12: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` exit 0; authoritative test count before and after.
10. Optional, the owner's call and not a gate: the device check (d) above on a debug build; the log should hold no "unrecoverable error" or "possible memory" line after a cancel, and the screen should show the "canceled" label as today.

## Root Cause Analysis

The workers were written (story U-015) with broad catches that turn any exception of the run into a `Result`: `catch (e: Exception)` in `BackupWorker.doWork`, and `catch (e: IllegalStateException)` in the restore functions for what the comment calls "possible memory" (a legacy of `RestoreTask`). Coroutine cancellation is carried by a `CancellationException`, a subclass of `IllegalStateException`, so it is caught by both. The catches never needed to tell it apart while the loops' checks did not throw (BUG-022: they read a job-less context). After U-067 the checks throw on cancel, and the catches take the exception on every cancelled run. The catches were not wrong when written; they predate a working cancellation check. It was not caught earlier because no test cancels a running worker and asserts its result or log (the only cancellation test before U-067 asserted WorkManager's own state, `BackupWorkerTest.kt:223`), and because WorkManager 2.9.1 drops the result (Evidence point 4), which hid the cost.

## Technical Context

- Exception hierarchy at HEAD: `kotlinx.coroutines.CancellationException` is `java.util.concurrent.CancellationException`, which extends `IllegalStateException`. The typed catches of both workers (`MailException`, `XOAuth2FailedException`, `RequiresLoginException`, `SecurityException`) do not match it (`MailException.java:36`, `XOAuth2FailedException.java:28`, `RequiresLoginException.java:5`).
- WorkManager 2.9.1 (read in the bytecode, not run): `Processor.stopWork` and `stopAndCancelWork` call `WorkerWrapper.interrupt`; `onWorkFinished` returns before `handleResult` for an interrupted wrapper. WorkManager 2.12.0 (read in `work-runtime-2.12.0-sources.jar`, not run): `interrupt` cancels the wrapper's job with a `WorkerStoppedException`; `launch` maps `WorkerStoppedException` to `Resolution.Stopped` and any other `CancellationException` to `Resolution.Failed` with the comment "worker was self-cancelled, which we treat as failure" (`WorkerWrapper.kt:100-114`); `onWorkFinished` handles a result only when the stored state is `RUNNING` (`:389-408`).
- Harness facts from U-067: under work-testing 2.9.1 `setProgress` and `setForeground` return completed futures without a job check, so under the unit harness the loop's own check is the only place a loop stops (U-067 plan, L5 to L7). The tests call the worker's functions directly, not through `startWork()`, so they do not exercise WorkManager's stop.
- `kotlinx.coroutines.CancellationException` rule: rethrow it. The kotlinx.coroutines guide sections and the detekt rule names are in the U-067 plan, section 2.0. No detekt configuration exists in this repository (search of the build files), so the rule is a convention here, not a check.
- Tag and constants: log tag `SMSBackup+` (`BackupWorker.kt:542`); `KEY_FAILURE_REASON = "failure_reason"` (`:556`).
- No test in the repository uses `ShadowLog`; Robolectric provides it. The Solution Architect decides whether to use it or a small log seam.

## Supporting Documentation

- `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` `:226`, `:228`, `:279`, `:284`, `:287`.
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-toolchain-runtime-libraries-platform-level-design.md` (F-1, the restated stop rule, story S4).
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` (cooperative cancellation, `:215-218` per BUG-022).
- `sdlc/artifacts/stories/BUG-022-worker-cancellation-checks-do-nothing.md` (Scope Concern SC-1, Suggested Approach option 1).
- `sdlc/artifacts/build/sprints/sprint-023/U-067-worker-cancellation-checks-use-the-running-coroutine.md` and `sdlc/artifacts/build/sprints/sprint-023/U-067/plan.md` (section 2.0 principles, section 7 item 4 "Follow-up bug (SC-1)", I6 mutations), `review-code.md` (I-4), `review-security.md` (W-1, W-2, W-3, I-7).
- `sdlc/artifacts/stories/BUG-021-delayed-otp-sms-skipped-by-backup-watermark.md` (Affected Code; shares the two files), `sdlc/artifacts/stories/BUG-018-restore-reports-failure-after-success.md`, `sdlc/artifacts/stories/BUG-023-content-trigger-backup-skips-scheduling-checks.md`.
- `sdlc/artifacts/build/sprints/sprint-021/U-065/evidence/IC1-emulator.txt` (the "unrecoverable error" count used as a failure signature).

## Implementation Notes

None yet. Added by agents during build.

## Review Findings

None yet. Origin: U-067's code review (finding I-4) and security review (findings W-1, W-2, W-3), 2026-11-21, all classified "defer-justified" and assigned to this bug by the orchestrator's ruling on U-067's Scope Concern.

## Scope Concerns

Raised, not decided. Neither is part of the acceptance criteria above; the Solution Architect and the orchestrator rule.

**SC-1. A cancelled IMAP restore skips the thread refresh (the security review's second observation). Decision: its own concern, recorded here as a related note, not part of this bug's criteria.**

- Fact: `updateAllThreadsIfAnySmsRestored` (`RestoreWorker.kt:623-629`) runs only after the loop completes (`:331`, `:416`) or in the `MailException` retry path (`:270`). A cancelled run returns from the catch before it. Rows inserted by that run can sit outside their conversation until a later run inserts something and refreshes; display only, no row lost or duplicated (U-067 security review W-2).
- Correction to the security review's wording: it says that before U-067 a cancelled run "always reached" the refresh. That holds under the unit harness. In production a cancellation at `setProgress` inside the loop's `try` already went to the same catch and skipped the refresh (U-067 plan section 7 item 4; BUG-022 SC-1), so the skip is not new, only more regular.
- Why it is not the same bug: the criteria here are about what a cancelled run reports, and the fix for them is to stop treating cancellation as an error. Running the refresh on cancel is a different change: it adds a provider call to a path that is meant to stop promptly, it needs a decision (refresh on cancel, or leave to the next run), and it would inherit the behaviour of a function that throws "Unknown URL" on Android 16 (BUG-018). Nothing in AC-1 to AC-12 requires or forbids the refresh on a cancelled run. If the Solution Architect's mechanism changes whether it runs, the story says so.
- Recommendation: leave to a later small change if the owner wants it, or drop; low value.
- **Solution Architect ruling: out.** The fix does not change whether the refresh runs. Whether it should run is item PO-1 for the product owner, to be recorded with BUG-018 (Suggested Approach §4, §7).

**SC-2. `WorkManagerCancelCollector` has the same anti-pattern and logs a warning on every finished manual run.**

- Fact: see Evidence, "The emulator line". Separate cause (its own job cancelled on a normal finish), separate file, not a worker path, older than U-067, effect one warning line.
- Not in the criteria because the orchestrator's brief scoped this bug to the workers' reporting of a cancelled run. If ruled in, the criterion would be: after the observer finishes a manual run, no "error collecting cancel" warning is logged and a real error in the collector is still logged; a unit test of `collect` with a repository whose `events` never emits, cancelling the returned `Job`. It is one file and one test.
- Recommendation: rule it in, because it is the same rule, one line, and it removes the second false-alarm line that device checks now show.
- **Solution Architect ruling: in.** AC-16, tests K1 and K2, mutation M-8 (Suggested Approach §2, §3). The same warning also follows the collector cancels at `MainViewModel.kt:141`, `:177`, `:323`, `:331` and `:344-345`, not only at `:211` and `:235`; the one catch covers them all.

## Not verified

1. **Nothing here was run.** No Gradle, no device, no test. The failing behaviour in Steps to Reproduce (a) to (c) is read from the code and from U-067's reviews, not observed. The result values and log lines for a cancelled run on HEAD have not been captured by a test (U-067's tests discard them).
2. **The 2.9.1 result-dropping (Evidence point 4) was read in bytecode only.** Not run; not read: the order of `setCancelledState` and `stopAndCancelWork` in `CancelWorkRunnable`, the path from a REPLACE enqueue to the stop (the bytecode of `EnqueueRunnable`), and what `CoroutineWorker.startWork` of 2.9.1 does when `doWork` throws a `CancellationException` rather than returns (the `work-runtime-ktx` classes were not decompiled). The `onWorkFinished` reading covers all three exits of the future in `WorkerWrapper$2.run`, so a throw from a stopped worker is dropped too, but how the throw becomes one of those exits was not read. *Solution Architect, partly settled by reading (Suggested Approach §1, L1 to L7):* in 2.9.1 `CoroutineWorker` lives in `work-runtime`, and the ktx artifact holds no classes. A throw becomes `future.setException`, which is ignored when the future is already cancelled. `interrupt` resolves the wrapper itself, before it cancels the future. `CancelWorkRunnable` sets CANCELLED before `stopAndCancelWork`. A REPLACE cancels and then deletes the record. Still not re-read: `Processor.stopWork` and the system stop callbacks.
3. **WorkManager 2.12.0 (S4).** Read in the sources: a `CancellationException` that leaves the worker and is not a `WorkerStoppedException` is treated as a failure (`Resolution.Failed`, `WorkerWrapper.kt:109-111`). Whether a worker that rethrows a user cancel is seen by the wrapper as stopped (job cancelled with `WorkerStoppedException`, so `Resolution.Stopped`) or as self-cancelled (`Resolution.Failed`) was not determined; it depends on which exception the wrapper's `withContext` rethrows, which was not traced. Whether a returned `Result.failure` from a stopped worker is applied in 2.12.0 (the `RUNNING` branch of `onWorkFinished`, system stop) was not determined either. This is the main reason the tests must be re-run by S4. *Solution Architect, settled by reading (Suggested Approach §1, L9 to L13):* for a user cancel, a REPLACE and a system stop, the wrapper's continuation is cancelled with the `WorkerStoppedException` before the worker's future and job are cancelled, and `worker.stop(reason)` runs first. So the result is `Resolution.Stopped`, whatever the worker throws or returns. The "self-cancelled" branch is reached only by a `CancellationException` thrown while the job is active. A returned `Result` of a stopped worker is not applied: its future is already cancelled. Not run; the test for S4 is named in §1.
4. **Whether `setProgress` throws promptly on a cancelled job in production** is BUG-022's Suggested Approach reading, not re-read here; it bears on "usually" in the statement that the path existed before U-067.
5. **Whether the cursors' closing can be observed through a `doWork()`-level backup test** is not known (U-067 ruling P-1 says it cannot through the seam).
6. **The `JobCancellationException` line** from the emulator run was reported by the orchestrator; the log is not in the repository and was not read. The code path that produces it was read (`MainViewModel.kt:209-212`, `WorkManagerCancelCollector.kt:43-57`).
7. **README and engagement documents** were not searched for cancel or failure wording (Impact Analysis layer 7).
8. **What an `IllegalStateException` thrown while the job is cancelled, but which is not itself a `CancellationException`, should do** is not decided by the contract or here (Open Questions, 4). *Solution Architect: decided. It is part of the cancel, logged once at warning with its throwable (AC-15; Suggested Approach §2).*

## Open Questions for the Solution Architect

*All six are answered in the Suggested Approach: 1 in §1 and §2; 2 and 3 in §3; 4 in §2 (AC-15); 5 in §4; 6 in §5.*

1. **Mechanism.** Rethrow the `CancellationException` ahead of each broad catch (the Kotlin convention), or catch it and return a result? Under WorkManager 2.9.1 both are dropped for a stopped worker; under 2.12.0 they may not be (Not verified, item 3). Which does the 2.12.0 code support for user cancel, REPLACE and system stop, and does the answer hold for S4?
2. **Backup test path.** `backupCursors` is a seam but the catch is in `doWork`. A fake SMS provider with at least two rows through `doWork()`, or a second seam? Can the cursors' closing be observed?
3. **Log assertion.** `ShadowLog` (unused so far in the repository) or a log seam?
4. **A real exception while the job is cancelled.** If a real `IllegalStateException` (not a `CancellationException`) is thrown while the job is already cancelled, is it logged as a failure (as today) or treated as part of the cancel? AC-6 covers only the non-cancelled case.
5. **SC-1 and SC-2.** Rule each in or out.
6. **Contract wording.** Should CNTR-MODERNIZATION-004 `:287` state what the worker returns or throws for a cancelled run (it is open), and should `WorkManagerScheduler.observe()` being a stub (`:456-458`) be recorded? Neither is required by the criteria.

## Notes

- Origin: code review (U-067, 2026-11-21). The Suggested Approach is the Solution Architect's to write.
- Sequencing is the orchestrator's ruling, recorded above, not re-decided here.
