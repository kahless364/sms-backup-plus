---
artifact_type: review-code
story_id: "U-067"
verdict: "PASS"
agent: "Code Reviewer"
timestamp: "2026-11-21"
blockers: 0
warnings: 0
---

# Code Review: U-067 (the workers' cancellation checks observe the running job; BUG-022)

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | Met. No contract, requirement or design file is edited by the code change. The behaviour restored is the one CNTR-MODERNIZATION-004 rule 7 already states. |
| Test coverage | Met. Seven tests added (746 to 753), none deleted, none ignored. The four cancellation tests fail without the fix and pass with it for the stated reason (traced below); the three controls pass in both states. |
| Code quality | Good. Production diff is exactly the three call lines, the two import lines and the visibility change of `backupCursors`. Findings are Low or Info only. |

## Verdict: PASS

The production change is correct against AC-7, AC-8, IC-1 to IC-3 and the three orchestrator rulings (DP-1 in, DP-2 Route S, P-1). No finding must be fixed in this cycle.

## What I read and ran

- Read from commit `c12a2788` (not from the working tree): the production diff; whole `BackupWorker.kt` lines 120-190 and 236-440; whole `RestoreWorker.kt` lines 78-90 and 160-560; the whole of `BackupWorkerLoopCancellationTest.kt`; the diffs of `BackupWorkerTest.kt`, `RestoreWorkerCheckpointTest.kt`, `RestoreWorkerIntegrationTest.kt`; `RestoreWorkerCheckpointTest.kt` `buildWorker`/`countSmsRows`; the story file; `mutation-log.md`; the evidence files named below; the Runtime Claims section of `implementation-log.md`.
- Ran (read-only git and shell reads only): `git show --stat`, `git show -- app/src/main`, `git grep` for `coroutineContext`, the authoritative `@Test` count at `HEAD~1` and `HEAD` (746 and 753). No Gradle, no edits.
- Plan, plan-review, contract-verification and expert-depth-check were consulted for the mutation table and the harness assumptions, not re-read end to end.

## Correctness of the production change

1. **The three call sites.** `BackupWorker.kt:332` (inside `backupCursors`, top of the `while (cursors.hasNext())` pass), `RestoreWorker.kt:311` (`runImapRestoreLoop`) and `RestoreWorker.kt:399` (`executeRestoreWithValues`) now read `currentCoroutineContext().ensureActive()`. `git grep -n coroutineContext` over `app/src/main` at the commit returns only the three new calls and, in `WorkManagerCancelCollector.kt:23`, a comment that does not contain the old name in the sense of IC-3 (the case-sensitive grep restricted to the two workers returns nothing: `I8-checks.txt`, `ic3-old-name-exit=1`).
2. **Imports.** `kotlin.coroutines.coroutineContext` replaced in place by `kotlinx.coroutines.currentCoroutineContext` at `BackupWorker.kt:57` and `RestoreWorker.kt:66`; `kotlinx.coroutines.ensureActive` kept (`:54`, `:63`). No line number moved in `RestoreWorker.kt` (numstat 3/3); in `BackupWorker.kt` the only shift is the one added annotation line at `:305` (lines below `:305` move by one, `:331` to `:332`), as AC-8 (c) allows for Route S.
3. **Visibility and annotation form.** `BackupWorker.kt:305-306`: `@androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)` then `internal suspend fun backupCursors(`. Same fully-qualified form and same `otherwise = PRIVATE` as `appendBatchAndUpdateWatermark` (U-053). The return type and parameters are unchanged. Callers: `BackupWorker.kt:255` (`fetchAndBackupItems`) and `:463` (the XOAuth2 retry in `handleAuthError`), both inside the class and unchanged; the test is the third. IC-1 holds: the Hilt/WorkManager constructors are not touched.
4. **Nothing else.** `git show --stat -- app/src/main` lists the two worker files only (7 insertions, 6 deletions). No file under `k9mail-vendored`, no build file, no `StatusPreference.java`, `WorkManagerCancelCollector.kt` or `MainViewModel.kt` (IC-2). No catch, result, watermark or checkpoint logic changed (AC-8 (d)).
5. **Comments that describe the checks** (`BackupWorker.kt:72`, `:286`, `:330`, `RestoreWorker.kt:84`, `WorkManagerCancelCollector.kt:23`) are now true as written: each says the loop stops by `ensureActive()` at the next suspension or pass, and the check now does throw. The comment at `:330` ("ensureActive() replaces isCancelled() polling") is accurate. The comment at `WorkManagerCancelCollector.kt:23` says "at the next suspension point"; the check actually acts at the top of the next pass (a pass is not interrupted); this imprecision pre-dates the story, the story rules these comments out of scope, and it is not misleading enough to act on (see O-4).
6. **Javadoc (AC-9, DP-1).** `BackupWorkerTest.kt` now says `ic2_workManager_cancelsBackupWorkerRequest` asserts WorkManager's state only (CANCELLED after `cancelUniqueWork()`) and does not run the loop, and names `BackupWorkerLoopCancellationTest.backupCursors_cancelledAfterSecondAppend_stopsAtNextPass`. That is true: the named test exists in the commit at that name, and `ic2_workManager_cancelsBackupWorkerRequest` is in the green case list. The diff of the file is the comment only (4 additions, 4 deletions in the comment block).

## Do the four cancellation tests fail without the fix, for the stated reason? (control-flow trace)

Common mechanism. Each test runs the real loop in a child coroutine that the test holds (`launch(start = CoroutineStart.LAZY)`, then `workJob.join()`, which starts the lazy child on the `runBlocking` event loop on the single test thread). The cancelling callback calls `workJob.cancel()` from inside the loop's own blocking call. That cancels the child's job. The child body is not interrupted by the call (nothing in the callback suspends); it continues to the end of the pass. The next suspension or check that looks at the job is the only place cancellation takes effect. Under the unit-test harness `setProgress` is a completed future and not a cancellation point (story AC terms; implementation-log Assumption Ledger), so the loop's own `ensureActive()` is the only stop point. With the old binding, `coroutineContext` resolves to the worker's inherited dispatcher member (no job), so the check cannot throw and the loop runs to the end. That is exactly the red run: `expected: 2 but was: 4`, `expected: 2 but was: 4`, `expected: 3 but was: 5`, `expected: 0 but was: 5` (`I3-red-message.txt`; `I3-red.txt`: "67 tests completed, 4 failed"; the three controls and the 60 older cases pass).

- **Site 1, `backupCursors_cancelledAfterSecondAppend_stopsAtNextPass`.** Fixture: four rows, one row per `BackupCursors.next()`, converter mock echoes the date of the current row, `StubMailTransport` wrapped so that append number 2 runs `workJob.cancel()` before it returns. Pass 1: check passes, convert (1), append (1), watermark = date 1, `setProgress`. Pass 2: check passes (job not yet cancelled), convert (2), append (2) cancels the job and returns the confirmed date, `appendBatchAndUpdateWatermark` then sets the watermark to `ROW_DATES[1]` (`BackupWorker.kt:424`; no suspension between the callback and that write), `setProgress` returns. Top of pass 3: `hasNext()` is true, the check throws `CancellationException` before `cursors.next()`, so the cursor stays on row 2 (`position == 1`), no third convert or append, watermark stays `ROW_DATES[1]`. The `finally` runs `transport.closeFolders()` once (`:377`). Asserted: `appends == 2`, `converts == 2`, `cursor.position == 1`, watermark `== ROW_DATES[1]`, `closes == 1`, `workJob.isCancelled`. Unfixed: all four rows are processed (appends 4); the first assertion that fails is `isCancelled`-then-`appends` as logged (`expected 2 but was 4`). The `completedEveryRow` assertion cannot fail independently (the cancelled run never sets `run.result`, so it is null and the boolean is false whenever the earlier assertions hold); that is by design (the test does not assert the `Result` type) and is harmless. The cursor-close half of AC-1 (c) is not observable through `backupCursors` (it happens in `fetchAndBackupItems` at `:279-281`); this is the orchestrator's ruling P-1. The test asserts the cursor position instead, which is stronger evidence that the loop stopped before consuming the next row.
- **Site 2, `doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass`.** Fixture: four handles from the first `getMessages` call (the call-log call returns none), `importMessageBody` returns item `i` of `createSmsItems(4)`, and on call 2 it cancels the job before returning. `doWork()` runs `setForeground` and the pre-loop `setProgress` calls before the loop; those happen before the cancel, so they are unaffected. Pass 1: import (1), insert row 1, checkpoint 0. Pass 2: import (2) cancels, returns, `insertSmsValues` inserts row 2 and writes checkpoint 1 (`:471-475`; the code is non-suspending after the callback except `insertInterceptor.afterInsert`, which is the no-op), `setProgress`. Top of pass 3: the check throws; no import 3. Asserted: `importCalls == 2`, `rowCount == 2`, checkpoint `== 1` (so `checkpointStore.clear` at `:340` was not reached), `closeCalls == 1` (outer `finally` at `:277`), `isCancelled`. Unfixed: four imports, four rows, checkpoint cleared (`NO_CHECKPOINT`).
- **Site 3, `executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass`.** Five distinct items, interceptor `afterInsert(2)` cancels the job. Pass 3: insert row 3 (index 2), checkpoint 2, then `afterInsert(2)` cancels; `setProgress` returns; top of pass 4: the check throws. Asserted: three rows, interceptor saw exactly `0, 1, 2` in order, checkpoint `== 2` (not cleared), `isCancelled`. Unfixed: five rows, checkpoint cleared. Because the cancel comes mid-loop, a single check placed before the loop would pass `...cancelledBeforeFirstPass...` but fail this test; the pair therefore pins the check to the top of each pass.
- **Site 3, `executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing`.** `launch(start = UNDISPATCHED)` runs the body at once; its first statement cancels its own job (`currentCoroutineContext()[Job]!!.cancel()`), and the call follows with no suspension between them. `checkpointStore.read` at the top of the function is not a suspension. The first pass's check throws before any insert. Asserted: `isCancelled`, zero rows, checkpoint `== NO_CHECKPOINT`. Unfixed: all five rows inserted (no suspension point looks at the job), checkpoint cleared. The test asserts rows and checkpoint, not the thrown exception, because the loop's `catch (e: IllegalStateException)` at `:433` swallows it (see "What a cancelled run does" below).

No assertion depends on timing, a dispatcher, a delay or the `Result` type. There is no `Thread.sleep`, no `delay`, no `withTimeout`; the only ordering is the single-threaded event loop of `runBlocking`. The `Result` is read only in the control tests, where no cancellation happens.

## Would the three controls catch a fix that stops too early?

Each control runs the same fixture and harness with the cancelling callback disabled (`cancelOnAppend = 0`, `cancelOnImportCall = 0`, `cancelAtIndex = -1`) and asserts the full outcome: backup: four appends, four converts, `position == 3`, watermark `== ROW_DATES.last()`, `closes == 1`, `Result.Success` with `backedUp == 4` and `STATE_FINISHED`; IMAP restore: four imports, four rows, checkpoint cleared, `closes == 1`, `Result.Success`; test-facing restore: five rows, interceptor saw `0..4`, checkpoint cleared, `Result.Success`. A change that made a check throw spuriously (a hard-coded throw, a check on the wrong job, a check on a stale flag) fails these. A fix that stops one pass early is caught by the exact counts of the cancellation tests (2, 2, 3 and three rows). A fix that stops one pass late is caught by the same counts. The controls also prove the fixtures are not what makes the cancellation tests fail.

## Mutation log, six rows against the test code (re-derived by reading)

| Row | Mutation | Failing set I derive | Log says | Agree |
|-----|----------|----------------------|----------|-------|
| M-1a | `BackupWorker.kt` site 1 old binding | Site 1 test only: loop runs to the end, `appends` 4 | 1 fail: site-1 test, `expected 2 but was 4` | yes |
| M-1b | site 1 `if (false) ...` | Same; the site 2 and 3 tests do not reach `backupCursors` | same | yes |
| M-2a | `RestoreWorker.kt:311` old binding | Site 2 test only: `importCalls` 4. The site 3 tests call `executeRestoreWithValues`, which does not use `runImapRestoreLoop` | 1 fail: site-2 test, `expected 2 but was 4` | yes |
| M-2b | `:311` deleted | Same | same | yes |
| M-3a | `RestoreWorker.kt:399` old binding | The mid-loop test (rows 5, expected 3) and the pre-cancel test (rows 5, expected 0). The IMAP and backup tests do not reach site 3 | 2 fails with `3 vs 5` and `0 vs 5` | yes |
| M-3b | `:399` deleted | Same | same | yes |

Each entry has a write-ahead line with a clock time, the mutated line confirmed before the run (for b-rows and M-2a/M-3a; M-1a states the diff against HEAD was empty at the site), a single targeted run of 31 cases (2 + 11 + 18, which I recounted from `I5-green-cases.txt`), a `restored` line with hash, diff-hash and porcelain checks, and the closing summary table. The restored hashes equal `BW_NEW` and `RW_NEW`, which are the blob ids of the committed production files (`eb535f5c`, `ffa9c57e`) shown in the same log and in the commit diff header. The (a) mutation leaves the new import unused, which compiles; the log says so. No mutation was invalid; none went undetected; AC-6 is met.

## Consistency of the numbers

- 746 to 753: `git grep -h "@Test"` count is 746 at `HEAD~1` and 753 at `HEAD` (I ran both). Seven tests added, named in the log and in the commit, none removed.
- Gradle totals: `I0-gates-junit-totals.txt` `tests=746 failures=0 errors=0 skipped=2`; `I8-gates-junit-totals.txt` `files=79 tests=753 failures=0 errors=0 skipped=2`. Skipped did not rise.
- 67 cases in seven classes: `I5-green-cases.txt` and `I8-seven-cases.txt` each list 67 `PASS` rows over seven `CLASS` lines (counted); `I3-red.txt` says "67 tests completed, 4 failed".
- Warnings 9 to 6: `I0-kotlinc-summary.txt` `warnings=9`, `worker-coroutineContext=3`, `task-line=1`, `task-skipped=0`; `I7-kotlinc-summary.txt` `warnings=6`, `worker-coroutineContext=0`. The six remaining warnings listed in `I7-kotlinc.txt` are in `MainViewModel.kt`, `SmsDefaultRoleHelper.kt` (two), `WorkManagerScheduler.kt`, `SharedPreferencesCheckpointStore.kt` and `WorkerEngineFactory.kt`; none names a worker file or `coroutineContext`. The compile really ran (the task was not skipped), so AC-7 is met.
- AC-12 and the build files: the production diff is the two worker files only; `I8-checks.txt` shows no build-file or `@Ignore` change.

## Test hygiene

- Fixtures reused unedited: the integration test adds `cancellableImapRun` and wraps `ImapRestoringTransport` by delegation instead of editing it; the checkpoint test adds `CancellingInterceptor` beside the existing interceptors; `createSmsItems`, `buildWorker`, `countSmsRows` and `buildWorkerWithTransport` are used as they are. The diff of the two existing test files is additions only (`@@` hunks with no removed lines apart from the import block additions); no existing assertion edited.
- No sleeps, no timeouts, no threads. No coroutine is left running: every child is joined; the lazy child is started by `join()`. A cancelled child completes before `join()` returns. No `GlobalScope`.
- Shared state: each class re-creates its fixtures in `setUp`; the checkpoint tests use unique work names per test (`test_restore_u067_*`); the backup test resets the SMS watermark in `setUp`; Robolectric resets the preference files and content resolver per test. The mocks are created per test.
- Resource closing: the backup tests call `cursors.close()` as the last statement, so a failing assertion earlier leaves a `MatrixCursor` unclosed (L-1).
- The new test file follows the package, runner and `@Config(sdk = [29])` conventions of the sibling worker tests.

## What a cancelled run does after the check throws (facts for the follow-up bug, SC-1)

The check throws a `kotlinx.coroutines.CancellationException`, which is `java.util.concurrent.CancellationException`, a subclass of `IllegalStateException`, and is not a `MailException`, `XOAuth2FailedException`, `RequiresLoginException` or `SecurityException`. The throw happens inside the loop's `try` in all three loops, before any new work of the pass.

- **Backup (`BackupWorker.kt:332`).** The `finally` in `backupCursors` runs `transport.closeFolders()` (`:377`-ish, the `finally` after the loop). The exception leaves `backupCursors` and passes the catches in `fetchAndBackupItems` (`:265` `XOAuth2FailedException`, `:268` `RequiresLoginException`, `:272` `MailException`, `:276` `SecurityException`), none of which match; the `finally` at `:280` runs `cursors?.close()`. It reaches `doWork` and is caught by `catch (e: Exception)` at `BackupWorker.kt:168`: `Log.e(TAG, "BackupWorker: unrecoverable error", e)` at `:169` and `Result.failure(workDataOf(KEY_FAILURE_REASON to (e.message ?: "unknown")))`, so the failure reason is the exception's message (normally "Job was cancelled"). On the XOAuth2 retry path (`backupCursors` called at `:463` from `handleAuthError`, itself running inside a catch clause of the same `try`), the exception is thrown from within a catch block, so the sibling catches do not see it either; it propagates to `doWork:168` the same way.
- **IMAP restore (`RestoreWorker.kt:311`).** Caught by the inner `catch (e: IllegalStateException)` in `runImapRestoreLoop` at `:348`: `Log.e(TAG, "RestoreWorker: IllegalStateException (possible memory)", e)` at `:349` and `return Result.failure(workDataOf(KEY_FAILURE_REASON to "illegal_state"))`. The outer catch at `:272` is not reached because the inner one returns first. `executeRestore`'s `finally` at `:277` runs `transport.closeFolders()`. `updateAllThreadsIfAnySmsRestored` (`:331`) and `checkpointStore.clear` (`:340`) are skipped; the checkpoint stays at the last confirmed insert.
- **Test-facing restore (`RestoreWorker.kt:399`).** Caught at `:433`: `Log.e(TAG, "RestoreWorker.executeRestoreWithValues: IllegalStateException", e)` at `:434` and `Result.failure(... "illegal_state")`. No `closeFolders` in that loop. The function returns normally, which is why the tests assert on the child's `isCancelled` flag (a job whose body returned while cancelling reports cancelled) and not on a thrown exception.
- **What a bug should say.** In both restore loops a cancelled run returns `Result.failure("illegal_state")` and logs a misleading "possible memory" error line; in the backup loop it returns `Result.failure(<message>)` and logs "unrecoverable error". Whether WorkManager discards that result for a cancelled worker is the Solution Architect's reading and was not re-verified by this story or this review. The new tests assert none of it, so a follow-up that rethrows `CancellationException` ahead of the broad catches does not need to edit them (the pre-cancel test would still pass: the rethrown exception ends the child as cancelled with no rows and no checkpoint, and the tests do not read the result of a cancelled call).

## Findings

### Blockers

None.

### Warnings

None.

### Observations

- **L-1 (Low). `BackupWorkerLoopCancellationTest.kt` (`cursors.close()` as the last statement of both tests).** Not in a `finally`; when an assertion fails the `MatrixCursor` is not closed. It is a heap object with no OS resource, so no leak of consequence and no cross-test effect. Must fix in this cycle: no. — Classification: defer-justified — a `MatrixCursor` holds no native resource and the pattern matches sibling tests.
- **L-2 (Low). `BackupWorkerLoopCancellationTest.kt`, the `completedEveryRow` assertion of the cancellation test.** It evaluates `run.result`, which is always null for a cancelled run (the child never assigns it), so it cannot fail on its own; the preceding assertions already fail on the unfixed code. It states AC-1 (d) literally and costs nothing. Must fix in this cycle: no. — Classification: defer-justified — AC-1 (d) asks for exactly this statement and the story forbids asserting the `Result` type.
- **I-1 (Info). `BackupWorker.kt:305-306`.** The KDoc above `backupCursors` still describes the loop but says nothing about the new visibility; the sibling `appendBatchAndUpdateWatermark` has a "Visibility: internal so it is accessible from tests" paragraph. The annotation itself documents the intent (`@VisibleForTesting`). No action. — Classification: defer-justified — the story limits the production diff to the visibility keyword and the annotation (AC-8 (b)); a KDoc edit would exceed it.
- **I-2 (Info). The cursor-close half of AC-1 (c) is not covered by a test** (ruling P-1). The `finally` that closes the cursors (`BackupWorker.kt:279-281`) is not edited by this story, and the exception does leave `backupCursors` through it (traced above), so the read-only check the ruling asks of QA passes. — Classification: defer-justified — orchestrator ruling P-1 accepts this and the code path is unchanged.
- **I-3 (Info). `WorkManagerCancelCollector.kt:23` and `BackupWorker.kt:72` say the check acts "at the next suspension point".** The check acts at the top of the next pass, after the in-flight pass finishes. The comments are now true in spirit; their wording is slightly loose and is the story's declared out-of-scope text. — Classification: defer-justified — the story names these comments as untouched, and the new Javadoc in `BackupWorkerTest.kt` states the pass-level behaviour precisely.
- **I-4 (Info). The observed behaviour of a cancelled run (a failure result and an error-level log line in both restore loops and the backup loop) is the follow-up bug the story defers (SC-1).** Facts with `file:line` are in the section above. — Classification: defer-justified — the story and the orchestrator rule it out of this change and assign it to a separate bug.

## Patterns Verified

- [x] Follows existing code patterns (seam by `internal` plus `@VisibleForTesting(otherwise = PRIVATE)` as in U-053; tests in the sibling worker test classes with the same runner, config and helpers)
- [x] Error handling is appropriate (no catch changed; a thrown check is handled by the same code as a `setProgress` cancellation)
- [x] Tests cover new functionality (four cancellation tests, three controls; each site proven by its own test and its own mutation pair)
- [x] No hardcoded values that should be configurable (row dates and counts are fixture constants)
- [x] No unnecessary complexity

## Integration Verified

- [x] New code is reachable from production entry points: `WorkManager` -> `BackupWorker.doWork` -> `executeBackup` -> `fetchAndBackupItems` -> `backupCursors` (`:255`) -> check (`:332`); `RestoreWorker.doWork` -> `executeRestore` -> `runImapRestoreLoop` -> check (`:311`); `executeRestoreWithValues` (check at `:399`) is the test-facing path as before and is not called by `doWork`
- [x] Registries/dispatch maps: none introduced or needed
- [x] Function signatures match at all call sites (`backupCursors` keeps its signature; callers at `:255`, `:463` and the test compile; the full gate built)
- [x] No dead code introduced (no new production function; the `internal` widening exists for the new test)
- [x] Integration path documented in implementation-log.md

## Regression Check (replacement/rewrite)

Not a replacement or rewrite; no file was deleted or replaced.

- [x] Capabilities Inventory: not required (no deleted or replaced file)
- [x] Nothing was removed; the diff deletes only the three old call lines and the two old import lines

## Contract Verification

- [x] Consumers of the modified interface identified: `backupCursors` consumers are the two in-class callers and the new test; the three checks have no consumers beyond their loops
- [x] No field, event or data-shape change; visibility only
- [x] No contract mismatch; `CNTR-MODERNIZATION-004` rule 7 ("MUST cooperatively cancel the running coroutine") is now made true by the check and is not edited

## Runtime Claims Verified

- [x] `## Runtime Claims` inventory read in the implementation log: two entries (the loop-stop claim, class crash/interrupt resilience; and the compiler-warning claim recorded as a build-output claim)
- [x] Every declared claim has an embedded probe with raw output: the loop-stop claim cites `I3-red*.txt`, `I5-green*.txt` and the six mutation runs; the warning claim cites `I0-kotlinc*` and `I7-kotlinc*`
- [x] The probe design is adequate per the live-verification checklist: failure-mode fidelity and falsifiability are shown (red run on the unfixed code, six mutants detected, controls pass); precondition fidelity is stated (the harness's `setProgress` property is the one the loop-stop claim relies on, listed in the Assumption Ledger); topology fidelity is limited and declared (in-process coroutine path, not WorkManager's own stop path), which is the scope of the claim as the story states it (IC-4: no device check is a criterion)
- [x] No escape-hatch justification is used
- [x] Inventory completeness audited against the story's runtime-behaviour criteria (AC-1 to AC-4 are the loop-stop claim; AC-7 is the build-output claim); no runtime-behaviour AC lacks an entry. Read-only; no probe was initiated by this review
- [x] No probe wrote into the sibling plugin-source repository (the probes are unit tests and Gradle runs inside this repository; the mutation protocol restores the two worker files from saved copies)
- [x] UNVERIFIED (escape-hatch) claims: None. Disclosed, not claims of this story: WorkManager's production stop path and the harness property that `setProgress` is never a cancellation point are the Solution Architect's reading of compiled library classes (story Notes) and were not re-verified by this review; this story's tests rely on the second one and the mutation evidence (the unfixed code passes nothing it should fail) supports it empirically for the versions in the build.

## Phase Completion Report
---
story_id: "U-067"
phase: "code-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-023/U-067/review-code.md"
---
