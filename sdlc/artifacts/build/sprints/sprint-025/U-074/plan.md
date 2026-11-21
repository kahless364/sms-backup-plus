---
artifact_type: plan
story_id: "U-074"
gate_id: "planning"
checkpoint: "planning"
verdict: "NEEDS_INFO"
agent: "Solution Architect"
timestamp: "2026-11-21T19:38:07Z"
iteration: 1
base_commit: "2f58f927f503f016a87d7565110bcf636bbe5e36"   # PLAN_HEAD (documents only since fd70325a); code read at fd70325a, at U-070's seven session-1 blobs and with U-075's three edits applied in scratch; the start commit is read at I0 (the commit that closes U-075)
contracts_verified:
  - CNTR-MODERNIZATION-004
  - CNTR-MODERNIZATION-006
  - CNTR-MODERNIZATION-005
risks_identified: 14
---

# Implementation Plan: U-074 — A Cancelled Backup or Restore Ends as Cancelled, Not as an Error Line and a Failure Result; the Cancel Collector Stops Warning on Every Normal Finish (BUG-024)


**DRAFT, incomplete (stopped 2026-11-21 at the owner's request). Not reviewed, not to be built from.**

Resume notes:
- **Complete (written and self-tested):** sections 0 to 8 and 10 to 13; every embedded text (3.1 helper, 3.2 to 3.4 production edits, 3.5 and 3.6 tests, 3.7 tools `oldtext_check.py`, `failset.py`, `failsets.txt`); the mutation table 4.8 with derived complete failing sets. The embedded blocks in this file are the authoritative copies (the scratch folder may not survive); every blob value is computed from this file's own text.
- **Partial:** section 9 (optional device check, PO-2) is an outline whose `adb` lines were not self-tested; section 14 holds the self-test results observed before the stop, without a final second extraction pass; the plan-text extractor of 14.9 picked up three of the four greps (the false-failure grep was tested directly) and is to be fixed; the Verdict is not given.
- **Not started:** the plan gates (plan review, contract verification, depth check); re-checking the plan against the real start commit once U-070 and U-075 are committed (I0's `oldtext_check.py start` does that).
- **Facts established, with sources:**
  - Simulated start (fd70325a + U-070's session-1 blobs + U-075's plan section 3 edits, each U-075 old text found once): `BackupWorker.kt` `3f2f0c7678fab5a7ddf5d36581f2df32cd19a643` (653 lines), `RestoreWorker.kt` `9be01b15210508393e750136ff127cc7eed3a968` (791), `WorkerForeground.kt` `0782f85f1b3815790ac21c87b454fe1f972a34c1` (65): equal to U-075's predicted blobs. Unchanged since `fd70325a`: `WorkManagerCancelCollector.kt` `681f1dda48361223dad26cdb7a5d178840d18be5`, `RestoreWorkerIntegrationTest.kt` `a6da69a5c8ce9c29af3f65281d88906f5e60ca9f`, `RestoreWorkerCheckpointTest.kt` `c4da4724688e3e36451c477fa75a50b08744afb2`. U-070's fixture text (its plan 4.1, first fenced block) hashes to `e00ce4bb75d20e9b36d0c8c0b8abc03e668b91b0`.
  - After this story's edits (simulated): `BackupWorker.kt` `523c77a1…`, `RestoreWorker.kt` `1a6a6209…`, collector `a3b5a601…`, the two restore test classes `6d210473…` and `913d33e1…` (section 3).
  - Catch sites at start (`CATCH_RE`, 4.0): handled `BackupWorker.kt:172`, `RestoreWorker.kt:198`, `:272`, `:348`, `:433`, `WorkManagerCancelCollector.kt:53`; left on purpose `WorkerForeground.kt:47`; the twelve other broad catches of 2.3 are not on a suspending run path (19 lines in all; 17 at `fd70325a`).
  - Library readings: WorkManager 2.9.1 bytecode (`javap`) of `CoroutineWorker` (job field, launch, cancel listener), `CoroutineWorker$startWork$1` (`future.set` / `setException`), `ListenableWorker.isStopped` (`mStopReason != -256`); WorkManager 2.12.0 sources: `WorkerWrapper.launch` (`:98-115`, `WorkerStoppedException` → Stopped, other `CancellationException` → Failed "self-cancelled"), `runWorker` (`:374-385`), `interrupt` (`:419-421`), `awaitWithin`, `ToContinuation`, `WorkerStoppedException` (`:652-690`), `CoroutineWorker.startWork` (`launchFuture(params.workerContext + Job())`), `launchFuture` in `ListenableFuture.kt` (`setCancelled` on a `CancellationException`); Robolectric 4.15.1 `ShadowLog` (`ConcurrentLinkedQueue` storage, `@Resetter`). Details in L8 to L12.
  - `RestoreCheckpointStore.NO_CHECKPOINT = 0` (L14); the helper's line 50 is unreachable, so JaCoCo is expected to show `covered=8 missed=1` (L18).
- **Open questions:** SC-1 to SC-5 (8.2) for the orchestrator; N1 to N3 (section 1) are answered only by the developer's runs.

Story: `sdlc/artifacts/build/sprints/sprint-025/U-074-cancelled-run-is-not-reported-as-a-failure.md` (binding).
Source bug: `sdlc/artifacts/stories/BUG-024-cancelled-backup-or-restore-reported-as-failure.md` (Suggested Approach
sections 0 to 8, rulings 1 to 6). Decision appendix (`sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`):
Record 18 (no external `diff` or `cmp`; stop on a vanished tool or a killed process), Records 21 to 23 (ruling 60:
U-075 before U-074; ruling 73: self-test before return; ruling 75: U-074 before U-076 to U-078). Form:
`sdlc/artifacts/build/sprints/sprint-024/U-075/plan.md` (iteration 2) and `sdlc/artifacts/build/sprints/sprint-024/U-069/plan.md`.

**Planning state.** Planned at `PLAN_HEAD` `2f58f927` on `sdlc/modernization-plan`; `git diff --stat fd70325a 2f58f927 -- app`
is empty (seven document commits). Story U-070 is in its second build session in this checkout and story U-075 (planned,
gates passed) is built after it; neither is committed. The working tree under `app/` and `app/build` was **not read**.
The start text was derived in a scratch folder outside the repository: (1) `git show fd70325a:<path>` for every file
neither story changes (`WorkManagerCancelCollector.kt` blob `681f1dda…`, `RestoreWorkerIntegrationTest.kt` `a6da69a5…`,
`RestoreWorkerCheckpointTest.kt` `c4da4724…`, `BackupWorkerLoopCancellationTest.kt`); (2) U-070's session-1 blobs by
`git cat-file blob` (`BackupWorker.kt` `ba667c43…`, `RestoreWorker.kt` `dc84c4f5…`, `BackupItemsFetcher.java`
`6af6f455…`, `DataTypePreferences.java` `38de9961…`, `SmsBackupWindow.java` `c48dcc5b…`, `LateVisibleSmsCursor.java`
`f8e920a6…`, `BackupQueryBuilder.java` `43a23c4b…`) and U-070's fixture file from its plan section 4.1 (first fenced
block, blob `e00ce4bb…`); (3) U-075's edits E-B1, E-B2, E-R1 and its new `service/WorkerForeground.kt`, taken from
U-075's plan section 3 by line range: each U-075 old text occurred exactly once, and the results are **`BackupWorker.kt`
`3f2f0c7678fab5a7ddf5d36581f2df32cd19a643` (653 lines), `RestoreWorker.kt` `9be01b15210508393e750136ff127cc7eed3a968`
(791 lines), `WorkerForeground.kt` `0782f85f1b3815790ac21c87b454fe1f972a34c1` (65 lines)**, the three blobs U-075's plan
predicts (its L20 and 3.3), which confirms the simulation. Line numbers below are at that simulated start unless marked
`fd70325a`. No Gradle, build, test, device or network was run.

**The start commit is not fixed by this plan.** `<start>` is `HEAD` at I0: the commit that closes U-075 (which follows
U-070). The start count `C0` is recorded there with its commit; **expected** 839 (771 at `fd70325a`, plus U-070's 50,
plus U-075's 18) if nothing else lands. Every later figure is "start + N".

**Size.** Three tracked production files gain one statement each per catch (`BackupWorker.kt` 1, `RestoreWorker.kt` 4,
`WorkManagerCancelCollector.kt` 1: six added lines, no removed line) and one production file is new
(`service/WorkerCancellation.kt`, 50 lines, one `internal suspend` function). Tests: two new classes
(`BackupWorkerCancelledRunTest`, 7 tests; `WorkManagerCancelCollectorTest`, 2), one new helper (`WorkerLogs.kt`), and
added lines only in `RestoreWorkerIntegrationTest.kt` (8 tests) and `RestoreWorkerCheckpointTest.kt` (2): **19 tests**
(the story's 18 plus one control, B0). **20 mutation runs**: M-C1 to M-C9 (the story's M-C1 to M-C8 and this plan's M-C9),
U-067's six and U-070's five. **25 Gradle runs**: two full gates (I0, I8) and 23 targeted runs, none of which includes
U-070's slow `SmsBackupRecordSizeTest`. **Two developer sessions** (4.10). The developer commits nothing.

**Compile check.** The helper, the three non-trivial mutants of it (M-C6, M-C7, M-C9), the collector with its edit, and
all four test files (the two new classes, `WorkerLogs.kt`, and both restore classes with their edits applied) were
compiled in the scratch folder with the cached `kotlin-compiler-embeddable` 2.3.21 (the build's Kotlin), without Gradle
and without network, against the SDK's `android-35/android.jar`, the cached `work-runtime` and `work-testing` 2.9.1
classes, `kotlin-stdlib` 2.3.21, `kotlinx-coroutines-core-jvm` 1.7.3, Guava 33.0.0, JUnit 4.13.2, Hamcrest 1.3, Truth
1.4.4, Mockito 5.18.0, Robolectric 4.15.1 (`robolectric`, `junit`, `sandbox`, `shadows-framework`, `shadowapi`,
`annotations`), `preference` 1.1.0, and **stubs** of the app classes they name, written from the signatures read at
`fd70325a` and from U-070's fixture text. The two workers were compiled as **mirror stubs** that carry the five catch
blocks with this plan's statements, not the real files (which need some forty more app classes). All exit 0; the only
warnings are the existing "no cast needed" lines of the two restore classes. The first real compile is the developer's
I1 (tests) and I3 (production) run (row D12).

## 0. Binding Inputs

| Source | What binds this plan |
|--------|----------------------|
| Story AC-1 to AC-17, IC-1 to IC-3, start conditions, binding constraints, "Not in this story" | As written. Where this plan is stricter (the control B0, the injection M-C9, the complete failing sets of M-C7 and of the re-runs, every test asserting the absence of warnings), section 8.2 says so and raises it |
| BUG-024 Suggested Approach, rulings 1 to 6 | The job's state decides; `rethrowIfCancelled` first in five worker catches and the collector's; `logCancel` at the two `doWork` sites only; tests through `doWork()`, no seam; SC-1 out, SC-2 in; no contract change in this story |
| Ruling 60 (Record 21), U-075 IC-2 and hand-over item 2 | U-075's `catch (e: IllegalStateException)` in `WorkerForeground.kt` is found by the catch search and is **left on purpose**; a check proves the cancellation still propagates through it at the start commit (2.3, I2) |
| Ruling 73 (Record 23) | Section 14: every command and tool that can run without Gradle, a device or `app/` was run on scratch input |
| Record 18 | No external `diff` or `cmp`; comparisons by `git diff --no-index`, `sha256sum` or `git hash-object`; modest shell activity; stop on a vanished tool or a killed process |
| REQ-MODERNIZATION-015 AC-13 as amended by CR-016 | The count rule (AC-16, 5.3) |
| CNTR-MODERNIZATION-004 v4 (CR-017) | Read in full for cancellation, failure, retry and stop statements (8.1); not edited |

## 1. Assumption Ledger

`verified` = read or run read-only in this session, with where. `needs_check` = a developer step with a decision row.
`plausible, not verified` = not read.

| # | Claim | Status | Evidence |
|---|-------|--------|----------|
| L1 | Start `BackupWorker.kt` (3f2f0c76): `doWork()` `:118-176`; U-075's helper call `:127` inside `if (!backupType.isBackground())` `:126-128`, **before** `return try {` `:150`; `MailException` catch `:168-171`; broad catch `:172-175` (`Log.e(TAG, "BackupWorker: unrecoverable error", e)` `:173`, `Result.failure(... e.message ?: "unknown")` `:174`); `fetchAndBackupItems` typed catches `:276-289` and `finally { cursors?.close() }` `:290-292`; `backupCursors` `:317-396`, `checkSettings` `:332` outside its `try`, loop check `:343`, `finally { transport.closeFolders() }` `:392-395`; `TAG` `:561`, `KEY_FAILURE_REASON` `:575` | verified | scratch simulation (Planning state); `fd70325a` `:164-171` are the same catches |
| L2 | Start `RestoreWorker.kt` (9be01b15): helper call `:148` before `return try {` `:165`; `doWork` broad catch `:198-201`; `executeRestore` `try` `:231-258`, catches `XOAuth2FailedException` `:260`, `RequiresLoginException` `:264`, `MailException` `:267` (`return Result.retry()` `:271`), `IllegalStateException` `:272-274`, `finally { transport.closeFolders() }` `:275-278`; `runImapRestoreLoop` `try` `:308-347`, loop check `:311`, catch `:348-351`; `executeRestoreWithValues` loop check `:399`, catch `:433-436` (no `finally`, no transport); `handleAuthError` catches `MailException`, `TokenRefreshException` `:669-676`. Identical to `fd70325a` at these lines (U-070 edits only `:461-464`; U-075 replaces five lines with five) | verified | simulation; `git show fd70325a` |
| L3 | `WorkManagerCancelCollector.kt` (681f1dda, unchanged since `fd70325a`): `collect` launches on `CoroutineScope(SupervisorJob() + Dispatchers.IO)` `:44-45`; waits on `repository.events.filterIsInstance<SyncEvent.Cancel>().first()` `:48-50`; `catch (e: Exception)` `:53` logs `"WorkManagerCancelCollector: error collecting cancel for $uniqueWorkName"` at warning with the throwable `:54`; `TAG` is `private val TAG = "SMSBackup+"` `:34` | verified | `git show fd70325a` |
| L4 | U-075's helper (0782f85f) `WorkerForeground.kt:47` `} catch (e: IllegalStateException) {`, first statement `:48` `if (e is CancellationException) throw e` (`java.util.concurrent.CancellationException`), continues only on `ForegroundServiceStartNotAllowedException` from API 31 `:49-52`, else `throw e` `:53` | verified | U-075 plan 3.3, extracted and hashed |
| L5 | U-075's tests `BackupWorkerForegroundTest.foreground_cancelled_endsCancelled` (F12) and `RestoreWorkerForegroundTest.restore_foreground_cancelled_endsCancelled` (R5) cancel the job and throw a `CancellationException` from the foreground request of a MANUAL backup and of a restore, and assert a thrown `CancellationException`, no `Result`, a cancelled job and no `create()`. They are at `<start>` and pass after U-075 (its I3). They are this story's proof that a cancellation still propagates through the helper at the start commit (2.3) | verified (plan text); needs_check (run) | U-075 plan 3.4 `:951-982`, `:1167-1196`; I2, I3 |
| L6 | Every broad catch in `app/src/main` at `fd70325a` (`git grep -n -E` of 4.0's `CATCH_RE`): 17 lines; at `<start>` two more (`WorkerForeground.kt:47`, U-075; `BackupItemsFetcher.java:99` `catch (RuntimeException e)`, U-070, closes its window cursor and rethrows) and `BackupWorker.kt` at `:172` instead of `:168`: **19 lines** (2.3) | verified | `git grep` at `fd70325a`; `grep` on the simulated start files (14.8) |
| L7 | Exception hierarchy: `kotlinx.coroutines.CancellationException` is a typealias of `java.util.concurrent.CancellationException`, which extends `IllegalStateException`; `MailException`, `RequiresLoginException`, `TokenRefreshException` extend `Exception`; `XOAuth2FailedException` extends `MailException` | verified | `git grep` of the four declarations at `fd70325a`; BUG-024 Technical Context |
| L8 | WorkManager **2.9.1** `CoroutineWorker`: a `CompletableJob` field; `startWork` launches `doWork()` in `coroutineContext + job`; `startWork$1` calls `doWork`, then `future.set(result)`; its catch calls `future.setException`; a listener cancels `job` once `future.isCancelled()`. `ListenableWorker.isStopped()` is `mStopReason != -256` | verified | `javap -c -p` on the cached `work-runtime-2.9.1.aar` classes (`CoroutineWorker`, `$startWork$1`, `ListenableWorker`) |
| L9 | WorkManager **2.9.1** `WorkerWrapper.interrupt` resolves the wrapper (CANCELLED kept, or ENQUEUED for a system stop) before it cancels the worker's future; the result of a stopped worker is never applied; a `CancellationException` thrown while not stopped is recorded FAILED | read by BUG-024's architect (its L4 to L7), not re-read here | BUG-024 Suggested Approach section 1 |
| L10 | WorkManager **2.12.0** (`work-runtime-2.12.0-sources.jar`, read): `WorkerWrapper.launch` runs `withContext(workerJob) { runWorker() }`; a `WorkerStoppedException` gives `Resolution.Stopped` (`:108-109`); **any other `CancellationException` gives `Resolution.Failed` ("worker was self-cancelled, which we treat as failure", `:110-112`)**; `runWorker` logs `"$workDescription was cancelled"` at info and rethrows (`:376-378`); `interrupt` = `workerJob.cancel(WorkerStoppedException(stopReason))` (`:419-421`); `awaitWithin`'s cancellation handler calls `worker.stop(reason)` and then `cancel(false)` on the worker's future (`:652-672`); `ToContinuation` cancels the continuation with no cause when the worker's future is cancelled (`:680-683`); `WorkerStoppedException(reason) : CancellationException()` (`:674`). `CoroutineWorker.startWork` = `launchFuture(params.workerContext + Job()) { doWork() }` (`CoroutineWorker.kt:62-66`); `launchFuture` turns a `CancellationException` from the block into `completer.setCancelled()` and any other throwable into `setException` (`ListenableFuture.kt:39-48`); `onStopped()` only calls `super` | verified | extracted from the cached sources jar into scratch and read |
| L11 | Coroutines: `CoroutineContext.isActive` and `CoroutineContext.ensureActive()` exist in 1.7.3 (used by U-067 in both workers) and 1.11.0; `ensureActive()` throws the job's `CancellationException` when the job is not active | verified (compile); BUG-024 L14 for the bodies | scratch compile against 1.7.3 |
| L12 | Robolectric 4.15.1 `ShadowLog`: `logs` and `logsByTag` are `ConcurrentLinkedQueue`s (entries written on `Dispatchers.IO` are kept), `getLogsForTag`, `clear()`, a `@Resetter`; `LogItem` has `type`, `tag`, `msg`, `throwable` | verified | `javap -p -v` on `shadows-framework-4.15.1.jar` |
| L13 | The project pins tests at SDK 29 (`robolectric.properties:8`, U-075 L14); the SDK 29 `android-all-instrumented` i7 jar is in the local Maven cache (U-075 L13). This story's tests need no class above API 29 and run at `@Config(sdk = [29])`, like the neighbouring classes; the SDK 34/35 jars U-075 needs are not needed here | verified | U-075 plan L13, L14; the test text names no class above API 29 |
| L14 | `RestoreCheckpointStore.NO_CHECKPOINT = 0` (`RestoreCheckpointStore.kt:64`): a checkpoint written at index 0 reads as `NO_CHECKPOINT`. R3, R5 and R8 assert 0 with that note | verified | `git show fd70325a` |
| L15 | U-070's fixtures as planned (its plan 4.1): `LateSmsHarness(context)` with `sms`, `watermark()`; `ProviderDouble.add(date, address, body, …)`; `RecordingTransport` records `"appendMessages"` and then calls `beforeAppend` **before** recording the message, records `"closeFolders"`, `count(call)`, `appended`; `SmsFixture.D`. The names and these orders as **built** are checked by text at I0 (`oldtext_check.py start`, the `FX-*` ids) | verified (plan text); needs_check (as built) | U-070 plan `:1517-1974`; row D4 |
| L16 | `DataTypePreferences.setMaxSyncedDateAfterAppend` (U-070, `38de9961` `:106-131`) writes `max(stored, confirmed)` for SMS, so the watermark after a cancelled backup is the last confirmed date and stays `-1` when nothing was confirmed | verified | `git cat-file blob` |
| L17 | `jacoco_classes.py` (U-069) prints `CLASS <name> covered=<c> missed=<m>`; `jacoco_pkgs.py` (U-063) prints dotted package names; `junit_cases.py` (U-063) prints `<class>#<name> PASS\|FAIL\|ERROR\|SKIP [type]`, then `CLASS` and totals lines | verified | the tools' text at `HEAD` |
| L18 | The scratch line table of `WorkerCancellationKt` has lines 41 to 45 and 47 to 50 (and 52 from the inlined `currentCoroutineContext`, beyond the file's 50 lines, which JaCoCo's Kotlin inline filter drops). Line 50 holds only the `return Unit` after `context.ensureActive()`, which cannot run (`ensureActive()` always throws there): **expected `covered=8 missed=1`** | verified (bytecode); needs_check (JaCoCo) | `javap -c -l` on the scratch class; I8; row D16 |
| L19 | JaCoCo Rule 1 `com.zegoggles.smssync.service*`, LINE, `minimum = 0.70` (`app/build.gradle:358-365` at `fd70325a`); `toolVersion = "0.8.14"` (`:212`) | verified | file read |
| L20 | No `withTimeout`, `isStopped`, `async(`, `supervisorScope` or `coroutineScope {` in `app/src/main` at `fd70325a`: no `TimeoutCancellationException` or child-scope cancellation can reach the catches from app code | verified | `git grep` (exit 1, no line) |
| N1 | The test files compile on Gradle's real classpath with the real app classes and U-070's fixtures as built | needs_check | I1, I2; row D12 |
| N2 | A four-row MANUAL backup through `doWork()` under the harness writes no `Log.WARN` and no `Log.ERROR` entry with tag `SMSBackup+` (B0); the cancelled-run assertions of "no warning" rest on it | needs_check | I2 (B0 green at start); row D11 (c) |
| N3 | `MutableSharedFlow.subscriptionCount` becomes 1 once the collector is suspended in `first()` on 1.7.3, so K1 cancels a collector that is waiting (the cancel is not taken before the coroutine started) | plausible, not verified (1.7.3 source not read; DES-MODERNIZATION-014 `:1107` records that 1.8.0 guarantees registration before the first suspension) | K1 red at start (I2) shows it; row D11 (d) |
| P1 | With `-ea`, coroutines stack-trace recovery may copy an exception resumed across a suspension. Every exception these tests compare is thrown and caught without a resumption, and the tests compare class and message, never identity | plausible, not verified | the tests do not depend on it |

## 2. Design (decided)

### 2.0 Principles applied

Book chapters are cited as commonly given; they were not re-read in this session.

- **Cancellation is control flow, not an error** (kotlinx.coroutines guide, "Cancellation and timeouts"; Goetz et al.,
  *Java Concurrency in Practice*, ch. 7 "Cancellation and Shutdown"; Elizarov, "Structured concurrency"). A
  `CancellationException` that a broad catch swallows breaks the coroutine's contract with its parent and turns a
  requested stop into a reported failure.
- **Decide on the authority, not on the symptom.** The authority for "this run is cancelled" is the state of the job
  running it (the thing `cancel` and WorkManager act on), not the type of whatever was thrown (Nygard, *Release It!*,
  2nd ed., ch. 4 on misleading failure signals; Bloch, *Effective Java*, 3rd ed., Item 77 "Don't ignore exceptions").
  The type is a symptom: a `CancellationException` can be thrown by a callee with the job active, and a real exception
  can surface after the job was cancelled.
- **Exceptions carry their level** (Nygard, ch. 10 "Transparency"; the logging levels convention of Android's `Log`): a
  cancel is information, a real exception during a cancel is a warning with its cause, a failure is an error. A false
  error line is an operational defect because device checks and the owner read error lines as failures.
- **Characterisation first, failing first, then mutation analysis** (Feathers, *Working Effectively with Legacy Code*,
  ch. 13; Beck, *TDD by Example*; DeMillo, Lipton and Sayward, 1978). Each assertion message names what it checks
  (Meszaros, *xUnit Test Patterns*, "Assertion Roulette").

### 2.1 The fix

One new file, `service/WorkerCancellation.kt` (3.1), holds `internal suspend fun rethrowIfCancelled(e: Exception, tag:
String, site: String, logCancel: Boolean = false)`, the text of BUG-024 Suggested Approach section 2 with a KDoc that
names the collector. Lines of that file:

| Line | Text | Why |
|------|------|-----|
| 41 | `internal suspend fun rethrowIfCancelled(...)` | `internal`: the workers, the collector and the tests of the module only. `suspend` so it can read the caller's coroutine context; every caller is suspending code (two `doWork`s, three `suspend` loop functions, the collector's `launch` block) (IC-2). Same package, so no import changes in any caller |
| 42 | `val context = currentCoroutineContext()` | The context of the coroutine that runs the catch: under WorkManager the job of `CoroutineWorker.startWork`'s launch (L8, L10), under the harness the test's child job |
| 43 | `if (context.isActive) return` | **The rule.** Job active: the catch handles `e` exactly as today, even a `CancellationException` (AC-5, case 2). `isActive` is `false` only while the job is cancelling: inside the job's own body it cannot be completed |
| 44-46 | `if (e !is CancellationException) Log.w(tag, "$site: exception while the run was being cancelled; the run ends cancelled", e)` | A real exception during a cancel is part of the cancel (AC-4); it is written once, at warning, with its throwable, by the innermost site; outer sites then see a `CancellationException` and write nothing |
| 47 | `if (logCancel) Log.i(tag, "$site: run cancelled")` | One information line per cancelled run, from `doWork` only (AC-7) |
| 48 | `if (e is CancellationException) throw e` | Rethrown as it is |
| 49 | `context.ensureActive()` | For a real exception: throws the job's own `CancellationException` (L11) |
| 50 | `}` | The `return Unit` here is unreachable by design (L18) |

**The six statements** (3.2 to 3.4), each the first statement of an existing catch; nothing else in any catch, `finally`,
typed catch or signature changes, and each catch keeps its expression form (the two `doWork` catches are still the value
branch of `return try { ... }` whose last expression is `Result.failure(...)`; the three loop-function catches still end
in `return Result.failure(...)`):

| Edit | Site (start line) | Statement |
|------|-------------------|-----------|
| E-B1 | `BackupWorker.doWork`, `catch (e: Exception)` `:172` | `rethrowIfCancelled(e, TAG, "BackupWorker", logCancel = true)` |
| E-R1 | `RestoreWorker.doWork`, `catch (e: Exception)` `:198` | `rethrowIfCancelled(e, TAG, "RestoreWorker", logCancel = true)` |
| E-R2 | `RestoreWorker.executeRestore`, `catch (e: IllegalStateException)` `:272` | `rethrowIfCancelled(e, TAG, "RestoreWorker.executeRestore")` |
| E-R3 | `RestoreWorker.runImapRestoreLoop`, `catch (e: IllegalStateException)` `:348` | `rethrowIfCancelled(e, TAG, "RestoreWorker.runImapRestoreLoop")` |
| E-R4 | `RestoreWorker.executeRestoreWithValues`, `catch (e: IllegalStateException)` `:433` | `rethrowIfCancelled(e, TAG, "RestoreWorker.executeRestoreWithValues")` |
| E-K1 | `WorkManagerCancelCollector.collect`, `catch (e: Exception)` `:53` | `rethrowIfCancelled(e, TAG, "WorkManagerCancelCollector")` |

**Cleanup is unchanged.** A throw from a catch still runs the `finally` that follows it: `executeRestore`'s
`closeFolders` (`:275-278`), `backupCursors`' `closeFolders` (`:392-395`, the exception passes through it before it reaches
`doWork`), `fetchAndBackupItems`' `cursors?.close()` (`:290-292`). The restore checkpoint is cleared only on the success
paths (`:340`, `:425`); the backup watermark is written only after a confirmed append (L16). AC-10's cursor clause: U-070's
provider double returns a fresh `MatrixCursor` per query and keeps no reference, so `isClosed` is **not observed**; the
closing is read from `fetchAndBackupItems`' `finally` (`:290-292`) and the exception path through it is unchanged
(recorded in the implementation log, as U-067 ruling P-1 did).

### 2.2 What "cancelled" means for a `CoroutineWorker`, and what the library does with the run (item 1)

- **Cancelled = the job of the coroutine running the code is cancelling.** In production that job is cancelled by
  WorkManager for a user cancel, a REPLACE and a system stop: on 2.9.1 through the worker's future being cancelled and
  the listener cancelling the job (L8, L9); on 2.12.0 through `awaitWithin` calling `worker.stop(reason)` and then
  cancelling the worker's future, whose cancellation listener cancels the job (L10). The next suspension point that
  checks (in practice `setProgress`) or the loop's own check (U-067) then throws a `JobCancellationException`, a
  `CancellationException`.
- **`isStopped` is not used.** It is `ListenableWorker`'s stop reason (L8), set by `stop()`. Under the unit harness
  nobody calls `stop()`, so a rule on `isStopped` could not be tested at all; it is not the coroutine's own state, and a
  cancel of the coroutine from its scope would not set it. In production both are set on every stop path, so the choice
  changes nothing there.
- **Subclasses.** `JobCancellationException` (the job's), `TimeoutCancellationException` (`withTimeout`: not used in
  `app/src/main`, L20; if it were, the job would be active and the run a failure, which is correct for a timeout), and
  2.12.0's `WorkerStoppedException` (the cause of the **wrapper's** job, L10; the worker's own job is cancelled without
  it). The rule never decides on the subclass.
- **What WorkManager does with the run.** The worker's report has no effect on the recorded state of a stopped run, in
  either version:

| Case | 2.9.1 (L9, read by BUG-024) | 2.12.0 (L10, read here) | The worker after this story |
|------|-----------------------------|--------------------------|-----------------------------|
| User cancel (`cancelUniqueWork`) | CANCELLED set first; the wrapper resolves itself; the worker's throw lands on a cancelled future | CANCELLED set first; the wrapper's continuation ends with `WorkerStoppedException` → `Stopped`; `resetWorkerStatus` keeps the finished state | throws; `I SMSBackup+: BackupWorker: run cancelled` |
| REPLACE | as a cancel, then the old record is deleted | as a cancel; state null after the delete | the same |
| System stop (constraints, quota, timeout) | ENQUEUED (rescheduled) by the wrapper | `Stopped` → ENQUEUED with the stop reason | the same |
| `CancellationException` thrown while the job is **active** (case 2) | `future.setException` → `ExecutionException` → FAILED | `launchFuture` → `setCancelled` → `ToContinuation` cancels without a cause → **`Resolution.Failed` ("self-cancelled")** → FAILED | returns `Result.failure`, as today → FAILED on both |

  **What 2.12.0 does differently** (the reason the story is placed before U-073): in 2.9.1 a stopped worker's outcome is
  dropped because its future is already cancelled; in 2.12.0 a `CancellationException` that leaves the worker and is not a
  `WorkerStoppedException` is a failure, and the worker's coroutine is a separate `Job()` (L10). A worker that rethrows only
  when its own job is cancelled meets 2.12.0's `Stopped` branch for every stop (the wrapper's continuation is cancelled
  with the `WorkerStoppedException` before the worker's future is, BUG-024 section 1), and never reaches the
  "self-cancelled" branch, which only case 2 reaches, where this story returns a failure exactly as the library would
  record it.
- **What the status screen and the notification show after a cancel: unchanged.** `MainViewModel` maps
  `WorkInfo.State.CANCELLED` to `CANCELED_BACKUP`/`CANCELED_RESTORE` and `FAILED` to `ERROR` (`MainViewModel.kt:249-253`,
  `:289-293` at `fd70325a`), and `StatusPreference` shows the "canceled" label; the state comes from WorkManager, which
  this story does not change. The foreground notification of a manual run ends when WorkManager's foreground service stops
  with the work (U-075 L10, read there); automatic runs show none since U-075. Neither is edited.
- **The cancel collector (SC-2).** `MainViewModel` cancels the collector's job on every normal finish (`:209-212`,
  `:233-236`), on every new run (`:141`, `:177`, `:323`, `:331`) and in `onCleared` (`:344-345`); `first()` then throws a
  `JobCancellationException` into the broad catch, which today writes the warning. After E-K1 the coroutine ends
  cancelled, which is normal completion for a `launch` in a `SupervisorJob` scope: no handler, no line. A real error with
  the job active (K2) is still written at warning with its throwable. `MainViewModel` is not changed (IC-3).

### 2.3 Every catch that can receive a `CancellationException` (IC-1)

A coroutine `CancellationException` can originate only in suspending code (a suspension point or a cancellation check) or
be thrown by a callee. The search (`CATCH_RE` of 4.0 over `app/src/main`, L6) gives 19 lines at `<start>`:

| Line at `<start>` | Catch | Status |
|-------------------|-------|--------|
| `service/BackupWorker.kt:172` | `catch (e: Exception)`, `doWork` | **Handled**, E-B1 (site 1) |
| `service/RestoreWorker.kt:198` | `catch (e: Exception)`, `doWork` | **Handled**, E-R1 (site 2) |
| `service/RestoreWorker.kt:272` | `catch (e: IllegalStateException)`, `executeRestore` | **Handled**, E-R2 (site 3) |
| `service/RestoreWorker.kt:348` | `catch (e: IllegalStateException)`, `runImapRestoreLoop` | **Handled**, E-R3 (site 4) |
| `service/RestoreWorker.kt:433` | `catch (e: IllegalStateException)`, `executeRestoreWithValues` | **Handled**, E-R4 (site 5) |
| `service/WorkManagerCancelCollector.kt:53` | `catch (e: Exception)`, `collect`'s launch block | **Handled**, E-K1 (site 6, SC-2) |
| `service/WorkerForeground.kt:47` | `catch (e: IllegalStateException)`, U-075's `setForegroundOrContinue` | **Left on purpose.** Its first statement rethrows any `CancellationException` (L4), so a cancellation while the foreground request is suspended, or a cancelled updater, propagates; it absorbs only `ForegroundServiceStartNotAllowedException` from API 31 and rethrows everything else. Its two callers sit before the workers' `try` blocks (`:127`, `:148`), so nothing it lets through can reach sites 1 or 2. Proof at the start commit: U-075's F12 and R5 (L5) run at I2 (before any production edit) and at I3, and `oldtext_check.py` checks the rethrow and the two call positions (`FG-CATCH`, `FG-OUTSIDE-B`, `FG-OUTSIDE-R`) at I0 and after the fix |
| `service/BackupItemsFetcher.java:99` | `catch (RuntimeException e)` (U-070) | Not on a suspending path: closes the window cursor and rethrows `e` |
| `service/BackupCursors.java:96` | `catch (Exception e)` around `cursor.close()` | Not suspending; called from `fetchAndBackupItems`' `finally` |
| `mail/transport/K9MailTransport.java:450`, `mail/BackupImapStore.java:95` | `catch (Exception e)` around `folder.close()` in `closeFolders()` | Not suspending; blocking transport code that cannot receive a coroutine cancellation |
| `preferences/EncryptedPrefsSecretStore.java:145`, `:253` | `catch (Exception e)`, `catch (RuntimeException e)` | Not suspending; secret store reads (CNTR-MODERNIZATION-003) |
| `App.java:235`, `activity/MainViewModel.kt:262`, `activity/auth/AccountManagerAuthActivity.java:130`, `activity/fragments/PinCertificateEnrollmentFlow.java:352`, `auth/PeopleApiContactsAdapter.java:79`, `:135` | various | Not on a worker's run path or the cancel path |

The typed catches of both workers (`MailException`, `XOAuth2FailedException`, `RequiresLoginException`,
`SecurityException`, and `MailException`/`TokenRefreshException` in the two `handleAuthError` functions) are **left on
purpose** (AC-9, BUG-024 section 2): none of these types is a `CancellationException`; a typed exception thrown while the
job is cancelled still maps as today, which no library version applies to a stopped run. The other suspending files of
`app/src/main` (`RestoreCheckpointStore.kt`, `RestoreInsertInterceptor.kt`, `SharedPreferencesCheckpointStore.kt`,
`state/FlowSyncStateRepository.kt`, `state/SyncStateRepository.kt`) have no `catch` (`git grep -n "catch ("` exit 1 at
`fd70325a`).

### 2.4 What the log shows after this story (item 3)

Until this story a run the system stops writes `E SMSBackup+: BackupWorker: unrecoverable error` with the job's
`CancellationException` (U-075 plan 2.6 item 3; its section 9 reads that pair as "stopped by the system"). After it, at
tag `SMSBackup+`:

| Run | Error | Warning | Info |
|-----|-------|---------|------|
| Backup stopped by the system, cancelled by the user, or replaced (REPLACE) | none | none | `BackupWorker: run cancelled` once |
| Restore, the same | none | none | `RestoreWorker: run cancelled` once |
| A real exception thrown while the job is cancelled | none | `<site>: exception while the run was being cancelled; the run ends cancelled` once, with the throwable, from the innermost site (`BackupWorker`, `RestoreWorker.runImapRestoreLoop`, `RestoreWorker.executeRestore` or `RestoreWorker`) | the `run cancelled` line once |
| A manual run that finishes normally, then the collector's job is cancelled | none | **none** (no more `WorkManagerCancelCollector: error collecting cancel for MANUAL`) | none from the collector |
| A user cancel | none | none | the collector's existing `D SMSBackup+: WorkManagerCancelCollector: Cancel received, cancelling <name>` (`:51`, unchanged), then the worker's `run cancelled` |
| Not cancelled (a real failure, or case 2) | exactly as today (`BackupWorker: unrecoverable error`, `RestoreWorker: unrecoverable error`, `RestoreWorker: IllegalStateException (possible memory)`, `RestoreWorker.executeRestoreWithValues: IllegalStateException`) | as today | as today |

WorkManager's own lines are not changed: for a stopped or cancelled run `I WM-WorkerWrapper: … was cancelled` (2.9.1 per
BUG-024 L6; 2.12.0 L10) and **no** `Worker result … for Work [ id=… ]` line for that run (as before; U-075 14.7). The
app's WorkManager configuration logs from INFO (U-075 L29), so the debug line "Work interrupted for" never appears.

**The greps for later device checks** (U-071 IC-4, U-073, U-076; `logcat -v threadtime` files, LF or CRLF; self-tested in
14.9 on constructed lines and on the committed log of 2026-11-21):
- a cancelled or stopped run: `grep -cE ' I SMSBackup\+: (BackupWorker|RestoreWorker): run cancelled[[:space:]]*$' <log>`;
- the old false-failure signature (0 expected for any cancel or stop after this story; above 0 with a following
  `CancellationException` line would mean case 2): `grep -cE ' E SMSBackup\+: (BackupWorker: unrecoverable error|RestoreWorker: unrecoverable error|RestoreWorker: IllegalStateException \(possible memory\)|RestoreWorker\.executeRestoreWithValues: IllegalStateException)[[:space:]]*$' <log>`;
- a real exception during a cancel: `grep -cE ' W SMSBackup\+: [A-Za-z.]+: exception while the run was being cancelled; the run ends cancelled[[:space:]]*$' <log>`;
- the collector's warning (0 expected after a normal finish): `grep -cF 'WorkManagerCancelCollector: error collecting cancel for ' <log>`.

**Consequence for U-075's tool.** U-075's `ac12_run.py` classifies a stop by `unrecoverable-cancel` (an
`unrecoverable error` line followed by a `CancellationException` line). After this story a stopped run of a fixed build
reads `result=none unrecoverable=0`, which that tool reports as neither pass, fail nor "stopped". U-075's own AC-12 runs
before this story starts and is not affected; later checks must read a stop by the first grep above and the absence of a
`Worker result` line. Scope Concern SC-2 (8.2) and hand-over item 4.

### 2.5 Alternatives considered (not chosen)

| Option | Why not |
|--------|---------|
| `catch (e: CancellationException) { throw e }` before each broad catch (rule on the type) | Case 2 would end by throwing: U-070's `ac9_cancelledDuringTheAppend_…` (B12, `runBlocking`) would fail, and the library records FAILED either way; a real exception during a cancel would still write the false error line (BUG-024 section 2). M-C6 is this alternative and is detected |
| Decide on `isStopped` | Not settable under the harness; not the coroutine's own state (2.2) |
| Return a `Result` for a cancelled run | No cancelled `Result` exists; `failure` contradicts CNTR-004 `:328`, `retry` contradicts `:331` ("terminal") |
| `currentCoroutineContext().ensureActive()` alone as the first statement | Rethrows the job's exception instead of `e`, writes no warning for a real exception (AC-4) and no info line (AC-7) |
| Restructure into one outer cancellation handler per worker | Changes more than one statement per catch (AC-17); moves code around `finally` blocks |
| Handle only the two `doWork` catches | The inner catches would still log "possible memory" at error for the loop's cancellation (R1, R3, C1) |
| A log seam instead of `ShadowLog` | A production change for tests; `ShadowLog` is on the classpath (L12) |

### 2.6 Tests (item 4)

- **SDK level 29** for every new test, the pinned level (L13). No level above it is needed; nothing is downloaded.
- **Harness** (U-067's): the worker function runs in a child started `CoroutineStart.LAZY` inside `runBlocking` and is
  joined; the child records what it returns or throws (`try { result = … } catch (t: Throwable) { thrown = t; throw t }`).
  The job is cancelled from inside a transport, interceptor or factory callback; a callback that throws cancels first.
  "Ends cancelled" = a `CancellationException` thrown, no `Result`, the job cancelled.
- **Backup** (`BackupWorkerCancelledRunTest`, new): four SMS rows at `SmsFixture.D + i * 1000` in U-070's
  `ProviderDouble` through `LateSmsHarness`, U-070's `RecordingTransport` with `beforeAppend`, and the worker built as
  `LateSmsHarness.backup` builds it (tag MANUAL, real `MessageConverter`), but run in the lazy child (the harness's
  `backup()` returns from `runBlocking` and offers no job to cancel). U-070's fixture file is not edited (AC-12). The
  MANUAL tag makes U-075's helper call the builder's default `TestForegroundUpdater`, which completes at once.
- **Restore** (R1 to R8 in `RestoreWorkerIntegrationTest`, C1 and C2 in `RestoreWorkerCheckpointTest`): added inside
  the existing classes to reuse their private fixtures (`cancellableImapRun`, `buildWorkerWithTransport`,
  `FailingRestoreTransport`, `CancellingInterceptor`, `buildWorker`, `createSmsItems`, `fakeProvider`,
  `checkpointStore`); one private helper is added for a worker with a given transport factory (R4, R6). Each new test
  calls `ShadowLog.clear()` first (the classes' `@Before` is not edited).
- **Collector** (`WorkManagerCancelCollectorTest`, new): a Mockito mock of `SyncStateRepository`. K1: `events` is a
  `MutableSharedFlow` that never emits; the test waits until the flow has a subscriber (`subscriptionCount`, so the
  cancel hits a waiting collector, N3), cancels the returned job and joins it. K2: reading `events` throws
  `IllegalStateException` inside the collector's `try` (the same catch as a failing collection; a hand-written
  `SharedFlow` is avoided because implementing `SharedFlow` outside the library is not stable for inheritance;
  recalled, not verified). Both join before they read `ShadowLog` (the collector runs on `Dispatchers.IO`).
- **Logs** through one helper, `WorkerLogs.kt` (`workerLogs(priority)` over `ShadowLog.getLogsForTag("SMSBackup+")`).
  "No error line" is "no `Log.ERROR` entry with the tag at all"; every cancelled-run test also asserts the exact number
  of warnings (0, or 1 with its throwable), and every characterisation test asserts no warning and no `run cancelled`
  line, so M-C9 can detect them.
- **B0 (this plan's control)**: the same four-row backup without a cancel passes and writes no warning or error line. It
  is green at the start and makes the strict "no warning" assertions of B1 and B2 trustworthy (N2): if a normal run
  wrote a warning, I2 shows it before any production edit (row D11 (c)).

### 2.7 Edge cases (behaviour after the fix)

1. **A refusal racing a cancellation (U-075's edge case 5, decided here: no change).** If the foreground request is
   refused after the job was cancelled, the helper absorbs the refusal (one warning) and returns `false`; the run reaches
   the first cooperative point (`setProgress` or the loop check), which throws, and site 1 or 2 writes `run cancelled`.
   Result: one refusal warning plus one info line, no error, the run ends cancelled. The blocking work done before that
   point (cursor queries, and for a backup the IMAP login if `setProgress` does not suspend) is wasted but changes no
   data. `WorkerForeground.kt` is not in this story's files (AC-17).
2. A cancel while `setForeground` is suspended: the helper rethrows before any `try` (L4, L5).
3. A typed exception (`MailException`, `RequiresLoginException`, …) while the job is cancelled: mapped as today (retry or
   a failure reason) and ignored by the library for a stopped run (2.3).
4. A cancel during the XOAuth2 retry path: `handleAuthError` calls `backupCursors`/`executeRestore` inside a `try` that
   catches only `MailException` and `TokenRefreshException`; the cancellation passes through to site 1 (backup) or sites
   3 and 2 (restore).
5. `executeRestoreWithValues` (test-facing, not called from `doWork`) ends cancelled with no line at all (AC-3).
6. The thread refresh after a cancelled IMAP restore stays skipped (SC-1 of the bug, out).
7. Two warnings are never written for one exception: the innermost site writes it and throws the job's
   `CancellationException`, which the outer sites rethrow silently (R8 asserts exactly one).

### 2.8 Interactions (exact)

- **U-071** (next story): its plan reads U-074's implementation log to classify the later cases of
  `BackupWorkerLoopCancellationTest`, `RestoreWorkerIntegrationTest` and `RestoreWorkerCheckpointTest` by the loop they
  drive (U-071 plan I0 step 13, row D64). The table is in section 10 item 3 and is copied into the implementation log.
  E-B1 moves U-071's text anchor M-1 (`                currentCoroutineContext().ensureActive()`) from `:343` to `:344`; it
  stays unique (checked by `oldtext_check.py`, `U067-S1`).
- **U-073** (WorkManager 2.12.0): the 19 tests are the guard its increment W re-runs; hand-over H-2 of the story stands.
- **U-075**: its helper and tests are read, not edited (2.3). Its `ac12_run.py` no longer sees a stop of a fixed build
  as "stopped" (2.4; SC-2).
- **U-070**: its fixtures are reused unedited; its `BackupWorker.kt`/`RestoreWorker.kt` mutations are re-run (I7). Its
  remaining rows (in `SmsBackupWindow`, `BackupItemsFetcher`, `DataTypePreferences`, `BackupQueryBuilder`) are **not**
  re-run: this story changes none of those files and no line those rows mutate; their detection rests on U-070's NEW5
  tests, which this story does not edit, and the I4 and I8 runs show those tests still pass.
- **U-067**: its seven tests pass unedited; its six mutations are re-run (I6). The new statements sit inside the catches,
  not on the three check lines.
- **Known stale comment** (SC-3 of the story, not edited; AC-11 forbids editing the test): `RestoreWorkerCheckpointTest.kt`
  `:454-458` ("the loop's IllegalStateException catch turns it into a Result").

## 3. Exact Changes

Paths: `$S` = `app/src/main/java/com/zegoggles/smssync/service`, `$TS` = `app/src/test/java/com/zegoggles/smssync/service`.
Every Edit is made with the Edit tool, matched by text: the `old_string` is the first fenced block after the marker line
`<!-- edit: <id> old -->`, the `new_string` the first after `<!-- edit: <id> new -->` (without the final newline). Each
`old_string` occurs exactly once at `<start>` (I0 `oldtext_check.py start`) in text neither U-070 nor U-075 changes.
Tracked files are CRLF in the working tree (`core.autocrlf true`); the Edit tool keeps them; `git hash-object` gives the
LF blob. New files are written with the Write tool; each one's text is the first fenced block after its marker line
`<!-- file: <path> -->`, LF, with a final newline.

### 3.1 New file `$S/WorkerCancellation.kt` (I3 step 1; blob `48a8074c22b40bd3d27df27cefae9ee2d994ba8e`, 50 lines)

<!-- file: app/src/main/java/com/zegoggles/smssync/service/WorkerCancellation.kt -->
```kotlin
/*
 * Copyright (c) 2024 SMS Backup+
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zegoggles.smssync.service

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive

/**
 * U-074 (BUG-024): the first statement of each broad catch on a worker's run path and of the
 * cancel collector's catch.
 *
 * The state of the job running the caller decides, not the type of [e]. If that job is
 * cancelled (the user's cancel, a REPLACE of the unique work, a stop by the system), the run
 * ends cancelled, whatever was thrown: this function throws, so the caller's failure handling
 * (error line, Result.failure) does not run. A CancellationException is rethrown as it is; any
 * other exception is first written once at warning level with [tag], naming [site], so it is not
 * lost, and the job's own CancellationException is thrown. With [logCancel] (the two doWork
 * sites only) one information line "<site>: run cancelled" is written.
 * CNTR-MODERNIZATION-004, Error Handling: "Cancellation is terminal, not failure."
 *
 * If the job is active, this returns and the caller handles [e] as before, even when [e] is a
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

### 3.2 `$S/BackupWorker.kt` (I3 step 2; one Edit; start `:172-173`)

<!-- edit: E-B1 old -->
```
        } catch (e: Exception) {
            Log.e(TAG, "BackupWorker: unrecoverable error", e)
```
<!-- edit: E-B1 new -->
```
        } catch (e: Exception) {
            rethrowIfCancelled(e, TAG, "BackupWorker", logCancel = true)
            Log.e(TAG, "BackupWorker: unrecoverable error", e)
```

### 3.3 `$S/RestoreWorker.kt` (I3 step 2; four Edits; start `:198-199`, `:271-273`, `:347-349`, `:433-434`)

E-R2 and E-R3 carry the line before the catch, because the two "possible memory" catches have the same three lines.

<!-- edit: E-R1 old -->
```
        } catch (e: Exception) {
            Log.e(TAG, "RestoreWorker: unrecoverable error", e)
```
<!-- edit: E-R1 new -->
```
        } catch (e: Exception) {
            rethrowIfCancelled(e, TAG, "RestoreWorker", logCancel = true)
            Log.e(TAG, "RestoreWorker: unrecoverable error", e)
```
<!-- edit: E-R2 old -->
```
            return Result.retry()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "RestoreWorker: IllegalStateException (possible memory)", e)
```
<!-- edit: E-R2 new -->
```
            return Result.retry()
        } catch (e: IllegalStateException) {
            rethrowIfCancelled(e, TAG, "RestoreWorker.executeRestore")
            Log.e(TAG, "RestoreWorker: IllegalStateException (possible memory)", e)
```
<!-- edit: E-R3 old -->
```
            ))
        } catch (e: IllegalStateException) {
            Log.e(TAG, "RestoreWorker: IllegalStateException (possible memory)", e)
```
<!-- edit: E-R3 new -->
```
            ))
        } catch (e: IllegalStateException) {
            rethrowIfCancelled(e, TAG, "RestoreWorker.runImapRestoreLoop")
            Log.e(TAG, "RestoreWorker: IllegalStateException (possible memory)", e)
```
<!-- edit: E-R4 old -->
```
        } catch (e: IllegalStateException) {
            Log.e(TAG, "RestoreWorker.executeRestoreWithValues: IllegalStateException", e)
```
<!-- edit: E-R4 new -->
```
        } catch (e: IllegalStateException) {
            rethrowIfCancelled(e, TAG, "RestoreWorker.executeRestoreWithValues")
            Log.e(TAG, "RestoreWorker.executeRestoreWithValues: IllegalStateException", e)
```

### 3.4 `$S/WorkManagerCancelCollector.kt` (I3 step 2; one Edit; start `:53-54`)

<!-- edit: E-K1 old -->
```
            } catch (e: Exception) {
                Log.w(TAG, "WorkManagerCancelCollector: error collecting cancel for $uniqueWorkName", e)
```
<!-- edit: E-K1 new -->
```
            } catch (e: Exception) {
                rethrowIfCancelled(e, TAG, "WorkManagerCancelCollector")
                Log.w(TAG, "WorkManagerCancelCollector: error collecting cancel for $uniqueWorkName", e)
```

Expected after I3 (simulated): `BackupWorker.kt` `523c77a11577ca976ac4c7f2f0896372c883092d` (654 lines; `--numstat` `1 0`;
hunk `@@ -172,0 +173 @@`); `RestoreWorker.kt` `1a6a6209d81609f4c470fa9055cf8f351da2415d` (795; `4 0`; hunks
`@@ -198,0 +199 @@`, `@@ -272,0 +274 @@`, `@@ -348,0 +351 @@`, `@@ -433,0 +437 @@`); `WorkManagerCancelCollector.kt`
`a3b5a601aed426ff719c2f7b52c0473dda53ff29` (59; `1 0`; `@@ -53,0 +54 @@`).

### 3.5 New test files (I1 step 1, Write tool)

**T1** `$TS/BackupWorkerCancelledRunTest.kt` — blob `3e9d4701343cf0f1dfbb25b2cab7eaa66b32a7bb` (261 lines, 7 lines
containing `@Test`).

<!-- file: app/src/test/java/com/zegoggles/smssync/service/BackupWorkerCancelledRunTest.kt -->
```kotlin
package com.zegoggles.smssync.service

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertWithMessage
import com.zegoggles.smssync.contacts.ContactAccessor
import com.zegoggles.smssync.di.MailTransportFactory
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.preferences.AuthPreferences
import com.zegoggles.smssync.preferences.Preferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * U-074 (BUG-024): what BackupWorker.doWork reports when the job running it is cancelled, and
 * that a genuine failure is reported exactly as before. The worker is built as U-070's
 * LateSmsHarness.backup builds it (real MessageConverter over U-070's SMS provider double, U-070's
 * RecordingTransport) and run as U-067's tests run a worker: in a lazily started child coroutine
 * that is joined; the job is cancelled from inside the transport, and a transport that throws
 * cancels the job first, as a suspension point fails on a cancelled job in production.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class BackupWorkerCancelledRunTest {

    private lateinit var context: Context
    private lateinit var harness: LateSmsHarness

    /** What one run of doWork() did: the Result it returned, or what it threw, and its job. */
    private class Run {
        var result: ListenableWorker.Result? = null
        var thrown: Throwable? = null
        lateinit var job: Job
    }

    @Before
    fun setUp() {
        @Suppress("DEPRECATION")
        context = RuntimeEnvironment.application
        harness = LateSmsHarness(context)
        for (i in 0 until ROWS) {
            harness.sms.add(date = SmsFixture.D + i * STEP, address = "+1555074000$i", body = "u074 row $i")
        }
        ShadowLog.clear()
    }

    /** Control: no cancellation; every row is appended and nothing is written at warning or error level. */
    @Test
    fun doWork_notCancelled_backsUpEveryRow_noWarningOrErrorLine_control() = runBlocking<Unit> {
        val run = Run()
        val transport = RecordingTransport()

        runDoWork(run, worker(transport))

        assertWithMessage("the job is cancelled").that(run.job.isCancelled).isFalse()
        assertWithMessage("doWork() throws").that(run.thrown).isNull()
        assertWithMessage("Result").that(run.result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        assertWithMessage("backed_up in the output").that(
            (run.result as ListenableWorker.Result.Success).outputData.getInt(BackupWorker.PROGRESS_KEY_BACKED_UP, -1))
            .isEqualTo(ROWS)
        assertWithMessage("appended messages").that(transport.appended).hasSize(ROWS)
        assertWithMessage("SMS watermark").that(harness.watermark()).isEqualTo(SmsFixture.D + (ROWS - 1) * STEP)
        assertWithMessage("closeFolders calls").that(transport.count("closeFolders")).isEqualTo(1)
        assertWithMessage("entries at Log.ERROR with tag SMSBackup+").that(workerLogs(Log.ERROR)).isEmpty()
        assertWithMessage("entries at Log.WARN with tag SMSBackup+").that(workerLogs(Log.WARN)).isEmpty()
        assertWithMessage("\"run cancelled\" lines").that(runCancelledLines()).isEmpty()
    }

    /** B1 (AC-1): cancelled during the second append; the check at the top of the next pass ends the run. */
    @Test
    fun doWork_cancelledDuringSecondAppend_endsCancelledWithoutErrorLine() = runBlocking<Unit> {
        val run = Run()
        var appends = 0
        val transport = RecordingTransport().apply { beforeAppend = { if (++appends == 2) run.job.cancel() } }

        runDoWork(run, worker(transport))

        assertEndsCancelled(run)
        assertWithMessage("entries at Log.WARN with tag SMSBackup+").that(workerLogs(Log.WARN)).isEmpty()
        assertWithMessage("appendMessages calls").that(transport.count("appendMessages")).isEqualTo(2)
        assertWithMessage("appended messages").that(transport.appended).hasSize(2)
        assertWithMessage("SMS watermark (the date the second append confirmed)").that(harness.watermark())
            .isEqualTo(SmsFixture.D + STEP)
        assertWithMessage("closeFolders calls").that(transport.count("closeFolders")).isEqualTo(1)
    }

    /** B2 (AC-6): the first append cancels the job and throws a CancellationException. */
    @Test
    fun doWork_cancellationThrownByAppend_endsCancelled() = runBlocking<Unit> {
        val run = Run()
        val transport = RecordingTransport().apply {
            beforeAppend = {
                run.job.cancel()
                throw CancellationException("test")
            }
        }

        runDoWork(run, worker(transport))

        assertEndsCancelled(run)
        assertWithMessage("entries at Log.WARN with tag SMSBackup+").that(workerLogs(Log.WARN)).isEmpty()
        assertWithMessage("appendMessages calls").that(transport.count("appendMessages")).isEqualTo(1)
        assertWithMessage("appended messages").that(transport.appended).isEmpty()
        assertWithMessage("SMS watermark (unchanged)").that(harness.watermark())
            .isEqualTo(DataType.Defaults.MAX_SYNCED_DATE)
        assertWithMessage("closeFolders calls").that(transport.count("closeFolders")).isEqualTo(1)
    }

    /** B3 (AC-8 (ii)): a real IllegalStateException with the job active is a failure, as before. */
    @Test
    fun doWork_illegalStateFromAppend_notCancelled_unrecoverableFailure() = runBlocking<Unit> {
        val run = Run()
        val boom = IllegalStateException("boom")
        val transport = RecordingTransport().apply { beforeAppend = { throw boom } }

        runDoWork(run, worker(transport))

        assertFailureAsBefore(run, "boom", boom)
        assertWithMessage("closeFolders calls").that(transport.count("closeFolders")).isEqualTo(1)
    }

    /** B4 (AC-8 (ii)): an exception without a message gives the reason "unknown", as before. */
    @Test
    fun doWork_exceptionWithoutMessage_notCancelled_reasonUnknown() = runBlocking<Unit> {
        val run = Run()
        val io = java.io.IOException()
        val transport = RecordingTransport().apply { beforeAppend = { throw io } }

        runDoWork(run, worker(transport))

        assertFailureAsBefore(run, "unknown", io)
    }

    /** B5 (AC-5): a CancellationException thrown while the job is active is a failure, as before. */
    @Test
    fun doWork_cancellationExceptionWhileActive_isAFailureAsToday() = runBlocking<Unit> {
        val run = Run()
        val x = CancellationException("x")
        val transport = RecordingTransport().apply { beforeAppend = { throw x } }

        runDoWork(run, worker(transport))

        assertFailureAsBefore(run, "x", x)
    }

    /** B6 (AC-4): a real exception thrown while the job is cancelled is part of the cancel: one warning. */
    @Test
    fun doWork_realExceptionWhileCancelled_endsCancelledWithOneWarning() = runBlocking<Unit> {
        val run = Run()
        var appends = 0
        val transport = RecordingTransport().apply {
            beforeAppend = {
                if (++appends == 2) {
                    run.job.cancel()
                    throw IllegalStateException("late")
                }
            }
        }

        runDoWork(run, worker(transport))

        assertEndsCancelled(run)
        val warnings = workerLogs(Log.WARN)
        assertWithMessage("entries at Log.WARN with tag SMSBackup+").that(warnings.map { it.msg }).containsExactly(
            "BackupWorker: exception while the run was being cancelled; the run ends cancelled")
        assertWithMessage("class of the warning's throwable").that(warnings.single().throwable?.javaClass)
            .isEqualTo(IllegalStateException::class.java)
        assertWithMessage("message of the warning's throwable").that(warnings.single().throwable?.message).isEqualTo("late")
        assertWithMessage("appended messages").that(transport.appended).hasSize(1)
        assertWithMessage("SMS watermark (the date the first append confirmed)").that(harness.watermark())
            .isEqualTo(SmsFixture.D)
        assertWithMessage("closeFolders calls").that(transport.count("closeFolders")).isEqualTo(1)
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    /** U-067's harness: doWork() in a lazily started child of this scope, joined; the outcome is recorded. */
    private suspend fun CoroutineScope.runDoWork(run: Run, worker: BackupWorker) {
        run.job = launch(start = CoroutineStart.LAZY) {
            try {
                run.result = worker.doWork()
            } catch (t: Throwable) {
                run.thrown = t
                throw t
            }
        }
        run.job.join()
    }

    /** The worker LateSmsHarness.backup builds (tag MANUAL), with [transport]. */
    private fun worker(transport: RecordingTransport): BackupWorker {
        val factory = object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String,
                                      workerParameters: WorkerParameters): ListenableWorker? {
                if (workerClassName != BackupWorker::class.java.name) return null
                val prefs = Preferences(appContext)
                val authPrefs = AuthPreferences(appContext)
                return BackupWorker(appContext, workerParameters, prefs, authPrefs, MailTransportFactory { transport },
                    ContactAccessor(), WorkerEngineFactory(appContext, prefs, authPrefs))
            }
        }
        return TestListenableWorkerBuilder<BackupWorker>(context)
            .setWorkerFactory(factory)
            .setTags(listOf(BackupType.MANUAL.name))
            .build()
    }

    private fun runCancelledLines(): List<ShadowLog.LogItem> =
        workerLogs(Log.INFO).filter { it.msg == "BackupWorker: run cancelled" }

    /** "Ends cancelled": a CancellationException, no Result, a cancelled job; no error line; one info line. */
    private fun assertEndsCancelled(run: Run) {
        assertWithMessage("the job is cancelled").that(run.job.isCancelled).isTrue()
        assertWithMessage("doWork() ends by throwing a CancellationException").that(run.thrown)
            .isInstanceOf(CancellationException::class.java)
        assertWithMessage("no Result is returned").that(run.result).isNull()
        assertWithMessage("entries at Log.ERROR with tag SMSBackup+").that(workerLogs(Log.ERROR)).isEmpty()
        assertWithMessage("\"BackupWorker: run cancelled\" at Log.INFO").that(runCancelledLines()).hasSize(1)
    }

    /** A genuine failure, exactly as before this story: the Result, the reason and the one error line. */
    private fun assertFailureAsBefore(run: Run, reason: String, cause: Throwable) {
        assertWithMessage("the job is cancelled").that(run.job.isCancelled).isFalse()
        assertWithMessage("doWork() throws").that(run.thrown).isNull()
        assertWithMessage("Result").that(run.result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
        assertWithMessage("failure reason").that(
            (run.result as ListenableWorker.Result.Failure).outputData.getString(BackupWorker.KEY_FAILURE_REASON))
            .isEqualTo(reason)
        val errors = workerLogs(Log.ERROR)
        assertWithMessage("entries at Log.ERROR with tag SMSBackup+").that(errors.map { it.msg })
            .containsExactly("BackupWorker: unrecoverable error")
        assertWithMessage("class of the error line's throwable").that(errors.single().throwable?.javaClass)
            .isEqualTo(cause.javaClass)
        assertWithMessage("message of the error line's throwable").that(errors.single().throwable?.message)
            .isEqualTo(cause.message)
        assertWithMessage("entries at Log.WARN with tag SMSBackup+").that(workerLogs(Log.WARN)).isEmpty()
        assertWithMessage("\"run cancelled\" lines").that(runCancelledLines()).isEmpty()
    }

    private companion object {
        const val ROWS = 4
        const val STEP = 1000L
    }
}
```

**T2** `$TS/WorkManagerCancelCollectorTest.kt` — blob `28229a7dacbf1a77e940acd7d00725d4fc9fc16d` (81 lines, 2 `@Test`).

<!-- file: app/src/test/java/com/zegoggles/smssync/service/WorkManagerCancelCollectorTest.kt -->
```kotlin
package com.zegoggles.smssync.service

import android.content.Context
import android.util.Log
import com.google.common.truth.Truth.assertWithMessage
import com.zegoggles.smssync.service.state.SyncEvent
import com.zegoggles.smssync.service.state.SyncStateRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * U-074 (BUG-024, SC-2): MainViewModel cancels the job WorkManagerCancelCollector.collect returns
 * when a run finishes, when a new run starts and in onCleared. That cancel is not an error and
 * writes no warning; a real error in the collector, with its job active, is still logged. The
 * collector runs on Dispatchers.IO, so each test joins the returned job before it reads the log.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class WorkManagerCancelCollectorTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        @Suppress("DEPRECATION")
        context = RuntimeEnvironment.application
        ShadowLog.clear()
    }

    /** K1 (AC-14): the collector's own job is cancelled while it waits for a Cancel event. */
    @Test
    fun collect_jobCancelledWhileWaiting_logsNothing() = runBlocking<Unit> {
        val events = MutableSharedFlow<SyncEvent>()
        val repository = Mockito.mock(SyncStateRepository::class.java)
        Mockito.`when`(repository.events).thenReturn(events)

        val job = WorkManagerCancelCollector.collect(context, repository, "MANUAL")
        // The collector is waiting in first() once it has subscribed; only then is the cancel a cancel of a waiting collector.
        withTimeout(WAIT_MS) { events.subscriptionCount.first { it > 0 } }
        job.cancel()
        job.join()

        assertWithMessage("the collector's job is cancelled").that(job.isCancelled).isTrue()
        assertWithMessage("entries at Log.WARN with tag SMSBackup+").that(workerLogs(Log.WARN)).isEmpty()
        assertWithMessage("entries at Log.ERROR with tag SMSBackup+").that(workerLogs(Log.ERROR)).isEmpty()
    }

    /** K2 (AC-14): a real error while the job is active is still a warning with its throwable. */
    @Test
    fun collect_eventsFlowFails_logsWarning() = runBlocking<Unit> {
        val failure = IllegalStateException("u074 events unavailable")
        val repository = Mockito.mock(SyncStateRepository::class.java)
        Mockito.`when`(repository.events).thenThrow(failure)

        val job = WorkManagerCancelCollector.collect(context, repository, "MANUAL")
        withTimeout(WAIT_MS) { job.join() }

        assertWithMessage("the collector's job is cancelled").that(job.isCancelled).isFalse()
        val warnings = workerLogs(Log.WARN)
        assertWithMessage("entries at Log.WARN with tag SMSBackup+").that(warnings.map { it.msg }).containsExactly(
            "WorkManagerCancelCollector: error collecting cancel for MANUAL")
        assertWithMessage("class of the warning's throwable").that(warnings.single().throwable?.javaClass)
            .isEqualTo(IllegalStateException::class.java)
        assertWithMessage("message of the warning's throwable").that(warnings.single().throwable?.message)
            .isEqualTo("u074 events unavailable")
    }

    private companion object {
        const val WAIT_MS = 10_000L
    }
}
```

**T3** `$TS/WorkerLogs.kt` — blob `ffdaca2b15f056bbe1b426147298f6e94a6275ec` (11 lines, no `@Test`).

<!-- file: app/src/test/java/com/zegoggles/smssync/service/WorkerLogs.kt -->
```kotlin
package com.zegoggles.smssync.service

import org.robolectric.shadows.ShadowLog

/**
 * U-074 (BUG-024): the entries written with the app's log tag (SMSBackup+, the tag of both
 * workers and of WorkManagerCancelCollector) at exactly [priority] (android.util.Log.ERROR,
 * WARN, INFO, ...), in the order they were written. The tests call ShadowLog.clear() first.
 */
internal fun workerLogs(priority: Int): List<ShadowLog.LogItem> =
    ShadowLog.getLogsForTag(BackupWorker.TAG).filter { it.type == priority }
```

### 3.6 Added lines in the two restore test classes (I1 step 1, Edit tool; added lines only)

**T-I1** (imports of `RestoreWorkerIntegrationTest.kt`):

<!-- edit: T-I1 old -->
```
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
```
<!-- edit: T-I1 new -->
```
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowLog
import android.util.Log
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
```

**T-I2** (R1 to R8 and their helpers, inserted after U-067's control test `doWork_imapRestore_notCancelled_restoresEveryItem`
and before the class `CancellableImapRun`). `old_string`:

<!-- edit: T-I2 old -->
```
    /** Counters and the transport of one U-067 IMAP restore run. */
    private class CancellableImapRun {
```
`new_string`: the block below, followed directly by the two lines of the `old_string` (the block ends with an empty
line).

<!-- block: T-I2 -->
```kotlin
    // =========================================================================
    // U-074 (BUG-024): what doWork() reports when the job is cancelled, and what a real failure
    // still reports. The same harness as U-067's tests; the outcome (Result or throwable) is recorded.
    // =========================================================================

    /** R1 (AC-2): the U-067 fixture, cancelled during import 2; the loop check ends the run. */
    @Test
    fun doWork_imapRestore_cancelledDuringSecondImport_endsCancelledWithoutErrorLine() = runBlocking<Unit> {
        ShadowLog.clear()
        val r = U074Run()
        val imap = cancellableImapRun(itemCount = 4, cancelOnImportCall = 2) { r.job.cancel() }

        runU074(r, buildWorkerWithTransport(imap.transport))

        assertU074EndsCancelled(r)
        assertWithMessage("entries at Log.WARN with tag SMSBackup+").that(workerLogs(Log.WARN)).isEmpty()
        assertWithMessage("importMessageBody calls").that(imap.importCalls).isEqualTo(2)
        assertWithMessage("rows inserted").that(fakeProvider.rowCount()).isEqualTo(2)
        assertWithMessage("checkpoint (not cleared)").that(checkpointStore.read(RestoreWorker.RESTORE_WORK_NAME)).isEqualTo(1)
        assertWithMessage("closeFolders calls").that(imap.closeCalls).isEqualTo(1)
    }

    /** R2 (AC-6): checkSettings cancels the job and throws; reaches executeRestore's catch. */
    @Test
    fun doWork_imapRestore_cancellationFromCheckSettings_endsCancelled() = runBlocking<Unit> {
        ShadowLog.clear()
        val r = U074Run()
        var closes = 0
        val failing = FailingRestoreTransport(checkSettingsBehavior = {
            r.job.cancel()
            throw CancellationException("test")
        })
        val transport = object : MailTransport by failing {
            override fun closeFolders() {
                closes++
            }
        }

        runU074(r, buildWorkerWithTransport(transport))

        assertU074EndsCancelled(r)
        assertWithMessage("entries at Log.WARN with tag SMSBackup+").that(workerLogs(Log.WARN)).isEmpty()
        assertWithMessage("rows inserted").that(fakeProvider.rowCount()).isEqualTo(0)
        assertWithMessage("checkpoint").that(checkpointStore.read(RestoreWorker.RESTORE_WORK_NAME))
            .isEqualTo(RestoreCheckpointStore.NO_CHECKPOINT)
        assertWithMessage("closeFolders calls").that(closes).isEqualTo(1)
    }

    /** R3 (AC-6): import 2 cancels the job and throws; reaches runImapRestoreLoop's catch without the loop check. */
    @Test
    fun doWork_imapRestore_cancellationFromImport_endsCancelled() = runBlocking<Unit> {
        ShadowLog.clear()
        val r = U074Run()
        val imap = cancellableImapRun(itemCount = 4, cancelOnImportCall = 2) {
            r.job.cancel()
            throw CancellationException("test")
        }

        runU074(r, buildWorkerWithTransport(imap.transport))

        assertU074EndsCancelled(r)
        assertWithMessage("entries at Log.WARN with tag SMSBackup+").that(workerLogs(Log.WARN)).isEmpty()
        assertWithMessage("importMessageBody calls").that(imap.importCalls).isEqualTo(2)
        assertWithMessage("rows inserted").that(fakeProvider.rowCount()).isEqualTo(1)
        assertWithMessage("checkpoint (index 0 written; NO_CHECKPOINT is also 0)")
            .that(checkpointStore.read(RestoreWorker.RESTORE_WORK_NAME)).isEqualTo(0)
        assertWithMessage("closeFolders calls").that(imap.closeCalls).isEqualTo(1)
    }

    /** R4 (AC-6): the transport factory cancels the job and throws; reaches doWork's catch directly. */
    @Test
    fun doWork_imapRestore_cancellationFromTransportFactory_endsCancelled() = runBlocking<Unit> {
        ShadowLog.clear()
        val r = U074Run()
        val factory = MailTransportFactory {
            r.job.cancel()
            throw CancellationException("test")
        }

        runU074(r, buildWorkerWithTransportFactory(factory))

        assertU074EndsCancelled(r)
        assertWithMessage("entries at Log.WARN with tag SMSBackup+").that(workerLogs(Log.WARN)).isEmpty()
        assertWithMessage("rows inserted").that(fakeProvider.rowCount()).isEqualTo(0)
    }

    /** R5 (AC-8 (i)): a real IllegalStateException from import 2 with the job active, as before. */
    @Test
    fun doWork_imapRestore_illegalStateFromImport_notCancelled_illegalState() = runBlocking<Unit> {
        ShadowLog.clear()
        val r = U074Run()
        val cause = IllegalStateException("u074 import")
        val imap = cancellableImapRun(itemCount = 4, cancelOnImportCall = 2) { throw cause }

        runU074(r, buildWorkerWithTransport(imap.transport))

        assertU074FailureAsBefore(r, "illegal_state", "RestoreWorker: IllegalStateException (possible memory)", cause)
        assertWithMessage("rows inserted").that(fakeProvider.rowCount()).isEqualTo(1)
        assertWithMessage("closeFolders calls").that(imap.closeCalls).isEqualTo(1)
    }

    /** R6 (AC-8 (iii)): a real exception from the transport factory with the job active, as before. */
    @Test
    fun doWork_exceptionFromTransportFactory_notCancelled_unrecoverableFailure() = runBlocking<Unit> {
        ShadowLog.clear()
        val r = U074Run()
        val cause = IllegalArgumentException("no transport")

        runU074(r, buildWorkerWithTransportFactory(MailTransportFactory { throw cause }))

        assertU074FailureAsBefore(r, "no transport", "RestoreWorker: unrecoverable error", cause)
    }

    /** R7 (AC-5): a CancellationException from import 2 while the job is active is handled as before. */
    @Test
    fun doWork_imapRestore_cancellationExceptionWhileActive_isIllegalStateAsToday() = runBlocking<Unit> {
        ShadowLog.clear()
        val r = U074Run()
        val cause = CancellationException("x")
        val imap = cancellableImapRun(itemCount = 4, cancelOnImportCall = 2) { throw cause }

        runU074(r, buildWorkerWithTransport(imap.transport))

        assertU074FailureAsBefore(r, "illegal_state", "RestoreWorker: IllegalStateException (possible memory)", cause)
    }

    /** R8 (AC-4): import 2 cancels the job and throws a real exception: one warning, from the innermost site. */
    @Test
    fun doWork_imapRestore_realExceptionWhileCancelled_endsCancelledWithOneWarning() = runBlocking<Unit> {
        ShadowLog.clear()
        val r = U074Run()
        val imap = cancellableImapRun(itemCount = 4, cancelOnImportCall = 2) {
            r.job.cancel()
            throw IllegalStateException("late")
        }

        runU074(r, buildWorkerWithTransport(imap.transport))

        assertU074EndsCancelled(r)
        val warnings = workerLogs(Log.WARN)
        assertWithMessage("entries at Log.WARN with tag SMSBackup+").that(warnings.map { it.msg }).containsExactly(
            "RestoreWorker.runImapRestoreLoop: exception while the run was being cancelled; the run ends cancelled")
        assertWithMessage("class of the warning's throwable").that(warnings.single().throwable?.javaClass)
            .isEqualTo(IllegalStateException::class.java)
        assertWithMessage("message of the warning's throwable").that(warnings.single().throwable?.message).isEqualTo("late")
        assertWithMessage("rows inserted").that(fakeProvider.rowCount()).isEqualTo(1)
        assertWithMessage("closeFolders calls").that(imap.closeCalls).isEqualTo(1)
    }

    /** U-074: what one run of doWork() did: the Result it returned, or what it threw, and its job. */
    private class U074Run {
        var result: ListenableWorker.Result? = null
        var thrown: Throwable? = null
        lateinit var job: Job
    }

    /** U-074: U-067's harness (a lazily started child of this scope, joined), recording the outcome. */
    private suspend fun CoroutineScope.runU074(r: U074Run, worker: RestoreWorker) {
        r.job = launch(start = CoroutineStart.LAZY) {
            try {
                r.result = worker.doWork()
            } catch (t: Throwable) {
                r.thrown = t
                throw t
            }
        }
        r.job.join()
    }

    /** U-074: as [buildWorkerWithTransport], with the transport factory itself given. */
    private fun buildWorkerWithTransportFactory(transportFactory: MailTransportFactory): RestoreWorker {
        val factory = object : androidx.work.WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): androidx.work.ListenableWorker? {
                return if (workerClassName == RestoreWorker::class.java.name) {
                    val prefs = Preferences(appContext)
                    val authPrefs = AuthPreferences(appContext)
                    RestoreWorker(
                        appContext,
                        workerParameters,
                        prefs,
                        authPrefs,
                        transportFactory,
                        checkpointStore,
                        RestoreInsertInterceptor.NoOp,
                        ContactAccessor(),
                        WorkerEngineFactory(appContext, prefs, authPrefs)
                    )
                } else null
            }
        }
        return TestListenableWorkerBuilder<RestoreWorker>(context)
            .setWorkerFactory(factory)
            .setRunAttemptCount(0)
            .build()
    }

    /** U-074 "ends cancelled": a CancellationException, no Result, a cancelled job; no error line; one info line. */
    private fun assertU074EndsCancelled(r: U074Run) {
        assertWithMessage("the job is cancelled").that(r.job.isCancelled).isTrue()
        assertWithMessage("doWork() ends by throwing a CancellationException").that(r.thrown)
            .isInstanceOf(CancellationException::class.java)
        assertWithMessage("no Result is returned").that(r.result).isNull()
        assertWithMessage("entries at Log.ERROR with tag SMSBackup+").that(workerLogs(Log.ERROR)).isEmpty()
        assertWithMessage("\"RestoreWorker: run cancelled\" at Log.INFO").that(
            workerLogs(Log.INFO).filter { it.msg == "RestoreWorker: run cancelled" }).hasSize(1)
    }

    /** U-074: a genuine failure, exactly as before this story: the Result, the reason and the one error line. */
    private fun assertU074FailureAsBefore(r: U074Run, reason: String, errorText: String, cause: Throwable) {
        assertWithMessage("the job is cancelled").that(r.job.isCancelled).isFalse()
        assertWithMessage("doWork() throws").that(r.thrown).isNull()
        assertWithMessage("Result").that(r.result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
        assertWithMessage("failure reason").that(
            (r.result as ListenableWorker.Result.Failure).outputData.getString(RestoreWorker.KEY_FAILURE_REASON))
            .isEqualTo(reason)
        val errors = workerLogs(Log.ERROR)
        assertWithMessage("entries at Log.ERROR with tag SMSBackup+").that(errors.map { it.msg }).containsExactly(errorText)
        assertWithMessage("class of the error line's throwable").that(errors.single().throwable?.javaClass)
            .isEqualTo(cause.javaClass)
        assertWithMessage("message of the error line's throwable").that(errors.single().throwable?.message)
            .isEqualTo(cause.message)
        assertWithMessage("entries at Log.WARN with tag SMSBackup+").that(workerLogs(Log.WARN)).isEmpty()
        assertWithMessage("\"run cancelled\" lines").that(
            workerLogs(Log.INFO).filter { it.msg?.endsWith(": run cancelled") == true }).isEmpty()
    }

```

**T-C1** (imports of `RestoreWorkerCheckpointTest.kt`):

<!-- edit: T-C1 old -->
```
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
```
<!-- edit: T-C1 new -->
```
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowLog
import android.util.Log
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CancellationException
```

**T-C2** (C1 and C2, inserted after U-067's control test `executeRestoreWithValues_notCancelled_insertsEveryItem` and
before the section "InMemoryCheckpointStore unit tests"). `old_string`:

<!-- edit: T-C2 old -->
```
    // -----------------------------------------------------------------------
    // InMemoryCheckpointStore unit tests
```
`new_string`: the block below, followed directly by the two lines of the `old_string`.

<!-- block: T-C2 -->
```kotlin
    // -----------------------------------------------------------------------
    // U-074 (BUG-024): what the test-facing restore loop reports when its job is cancelled,
    // and what a real failure still reports. The outcome (Result or throwable) is recorded.
    // -----------------------------------------------------------------------

    /**
     * C1 (AC-3): U-067's fixture, cancelled in afterInsert(2). The function ends by throwing a
     * CancellationException and returns no value; nothing is written at error, warning or info
     * level (it is not reached from doWork, so it writes no "run cancelled" line).
     */
    @Test
    fun executeRestoreWithValues_cancelledAfterThirdInsert_endsCancelledWithoutErrorLine() = runBlocking<Unit> {
        ShadowLog.clear()
        val items = createSmsItems(5)
        val workName = "test_restore_u074_cancel_mid_loop"
        lateinit var workJob: Job
        var result: ListenableWorker.Result? = null
        var thrown: Throwable? = null
        val interceptor = CancellingInterceptor(cancelAtIndex = 2) { workJob.cancel() }
        val worker = buildWorker(interceptor = interceptor)

        workJob = launch(start = CoroutineStart.LAZY) {
            try {
                result = worker.executeRestoreWithValues(items, 0, preferences, context, workName)
            } catch (t: Throwable) {
                thrown = t
                throw t
            }
        }
        workJob.join()

        assertWithMessage("the job is cancelled").that(workJob.isCancelled).isTrue()
        assertWithMessage("executeRestoreWithValues ends by throwing a CancellationException").that(thrown)
            .isInstanceOf(CancellationException::class.java)
        assertWithMessage("no Result is returned").that(result).isNull()
        assertWithMessage("entries at Log.ERROR with tag SMSBackup+").that(workerLogs(Log.ERROR)).isEmpty()
        assertWithMessage("entries at Log.WARN with tag SMSBackup+").that(workerLogs(Log.WARN)).isEmpty()
        assertWithMessage("entries at Log.INFO with tag SMSBackup+").that(workerLogs(Log.INFO)).isEmpty()
        assertWithMessage("rows inserted").that(countSmsRows()).isEqualTo(3)
        assertWithMessage("checkpoint (not cleared)").that(checkpointStore.read(workName)).isEqualTo(2)
    }

    /** C2 (AC-8 (i)): a real IllegalStateException from the interceptor at index 1, with the job active, as before. */
    @Test
    fun executeRestoreWithValues_illegalStateFromInterceptor_notCancelled_illegalState() = runBlocking<Unit> {
        ShadowLog.clear()
        val items = createSmsItems(5)
        val workName = "test_restore_u074_illegal_state"
        val cause = IllegalStateException("u074 interceptor")
        lateinit var workJob: Job
        var result: ListenableWorker.Result? = null
        var thrown: Throwable? = null
        val interceptor = CancellingInterceptor(cancelAtIndex = 1) { throw cause }
        val worker = buildWorker(interceptor = interceptor)

        workJob = launch(start = CoroutineStart.LAZY) {
            try {
                result = worker.executeRestoreWithValues(items, 0, preferences, context, workName)
            } catch (t: Throwable) {
                thrown = t
                throw t
            }
        }
        workJob.join()

        assertWithMessage("the job is cancelled").that(workJob.isCancelled).isFalse()
        assertWithMessage("executeRestoreWithValues throws").that(thrown).isNull()
        assertWithMessage("Result").that(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
        assertWithMessage("failure reason").that(
            (result as ListenableWorker.Result.Failure).outputData.getString(RestoreWorker.KEY_FAILURE_REASON))
            .isEqualTo("illegal_state")
        val errors = workerLogs(Log.ERROR)
        assertWithMessage("entries at Log.ERROR with tag SMSBackup+").that(errors.map { it.msg })
            .containsExactly("RestoreWorker.executeRestoreWithValues: IllegalStateException")
        assertWithMessage("class of the error line's throwable").that(errors.single().throwable?.javaClass)
            .isEqualTo(IllegalStateException::class.java)
        assertWithMessage("message of the error line's throwable").that(errors.single().throwable?.message)
            .isEqualTo("u074 interceptor")
        assertWithMessage("entries at Log.WARN with tag SMSBackup+").that(workerLogs(Log.WARN)).isEmpty()
        assertWithMessage("entries at Log.INFO with tag SMSBackup+").that(workerLogs(Log.INFO)).isEmpty()
        assertWithMessage("rows inserted").that(countSmsRows()).isEqualTo(2)
        assertWithMessage("checkpoint (not cleared)").that(checkpointStore.read(workName)).isEqualTo(1)
    }

```

Expected after I1 (simulated from `fd70325a`'s blobs): `RestoreWorkerIntegrationTest.kt`
`6d210473059a66f8909b6a3500c831c5b8507a9b` (844 lines, 19 `@Test`, `--numstat` `235 0`, hunks `@@ -39,0 +40,5 @@` and
`@@ -415,0 +421,230 @@`); `RestoreWorkerCheckpointTest.kt` `913d33e10056378b7e07048815111d0fba2591d9` (776 lines, 20
`@Test`, `88 0`, hunks `@@ -28,0 +29,4 @@` and `@@ -501,0 +506,84 @@`; git aligns the block's first comment line with
the anchor's, so the second hunk is shown one line lower than the insertion point; the text is the same).

### 3.7 Tools (Write tool, I0 step 1, into `$TL` = `sdlc/artifacts/build/sprints/sprint-025/U-074/evidence/tools/`)

`oldtext_check.py` — blob `1aac0476e7ea15791d78ec97866441eae8b4f975` (self-tests 14.4):

<!-- file: sdlc/artifacts/build/sprints/sprint-025/U-074/evidence/tools/oldtext_check.py -->
```python
"""U-074: check that every text this story's edits, mutations and tests rely on occurs as expected.
usage (from the repository root): python -I -X utf8 oldtext_check.py start|fixed
start: at <start>, before any edit (both workers, the collector and U-075's helper as built; U-070's fixtures;
the two restore test classes unedited). fixed: after E-B1, E-R1 to E-R4, E-K1, T-I1, T-I2, T-C1, T-C2 and the four
new files. Files are read with universal newlines, so CRLF and LF trees give the same counts. A missing file
counts as text that occurs 0 times. Prints one line per check and a RESULT line; exit 1 on any mismatch."""
import os
import sys
S = 'app/src/main/java/com/zegoggles/smssync/service/'
T = 'app/src/test/java/com/zegoggles/smssync/service/'
BW, RW, KC, WF, WC = (S + n for n in ('BackupWorker.kt', 'RestoreWorker.kt', 'WorkManagerCancelCollector.kt',
                                      'WorkerForeground.kt', 'WorkerCancellation.kt'))
FX, IT, CT = T + 'LateVisibleSmsFixtures.kt', T + 'RestoreWorkerIntegrationTest.kt', T + 'RestoreWorkerCheckpointTest.kt'
BT, KT, WL = T + 'BackupWorkerCancelledRunTest.kt', T + 'WorkManagerCancelCollectorTest.kt', T + 'WorkerLogs.kt'
E = '        } catch (e: Exception) {\n'
ISE = '        } catch (e: IllegalStateException) {\n'
LOG_B = '            Log.e(TAG, "BackupWorker: unrecoverable error", e)\n'
LOG_R = '            Log.e(TAG, "RestoreWorker: unrecoverable error", e)\n'
LOG_M = '            Log.e(TAG, "RestoreWorker: IllegalStateException (possible memory)", e)\n'
LOG_V = '            Log.e(TAG, "RestoreWorker.executeRestoreWithValues: IllegalStateException", e)\n'
K_CATCH = '            } catch (e: Exception) {\n'
K_LOG = '                Log.w(TAG, "WorkManagerCancelCollector: error collecting cancel for $uniqueWorkName", e)\n'
C_B = '            rethrowIfCancelled(e, TAG, "BackupWorker", logCancel = true)\n'
C_R = '            rethrowIfCancelled(e, TAG, "RestoreWorker", logCancel = true)\n'
C_X = '            rethrowIfCancelled(e, TAG, "RestoreWorker.executeRestore")\n'
C_L = '            rethrowIfCancelled(e, TAG, "RestoreWorker.runImapRestoreLoop")\n'
C_V = '            rethrowIfCancelled(e, TAG, "RestoreWorker.executeRestoreWithValues")\n'
C_K = '                rethrowIfCancelled(e, TAG, "WorkManagerCancelCollector")\n'
IMP = 'import org.robolectric.annotation.Config\nimport org.robolectric.shadows.ShadowContentResolver\n'
I2 = '    /** Counters and the transport of one U-067 IMAP restore run. */\n    private class CancellableImapRun {\n'
C2 = '    // -----------------------------------------------------------------------\n    // InMemoryCheckpointStore unit tests\n'
SHARED = [
    # U-067's three loop checks (mutations M-1a to M-3b re-run at I6)
    ('U067-S1', BW, '                currentCoroutineContext().ensureActive()\n', 1),
    ('U067-S2', RW, '                    currentCoroutineContext().ensureActive()\n\n                    val item = msgs[currentRestoredItem]\n', 1),
    ('U067-S3', RW, '                    currentCoroutineContext().ensureActive()\n\n                    val values = items[currentRestoredItem]\n', 1),
    # U-070's mutation anchors in the two workers (re-run at I7)
    ('M-L11', BW, '        val confirmedMaxDate: Long = transport.appendMessages(folder, result)\n', 1),
    ('M-L20', BW, '                    if (cursor.type == DataType.SMS) {\n', 1),
    ('M-L22', BW, '                if (type == DataType.SMS) {\n', 1),
    ('M-L16a', RW, '            preferences.dataTypePreferences.recordRestoredSms(values)\n', 1),
    ('M-L16b', RW, '            preferences.dataTypePreferences.recordRestoredSms(values)\n'
                   '            val uri = ctx.contentResolver.insert(Consts.SMS_PROVIDER, values)\n', 1),
    # U-075's helper: its catch is left on purpose; both calls lie before the workers' try blocks
    ('FG-CATCH', WF, '    } catch (e: IllegalStateException) {\n        if (e is CancellationException) throw e\n', 1),
    ('FG-OUTSIDE-B', BW, '            setForegroundOrContinue(createBackupForegroundInfo(), TAG, "BackupWorker")\n        }\n\n'
                         '        // INV-3 backoff cap: cap effective retry delay at 300s.\n', 1),
    ('FG-OUTSIDE-R', RW, '        setForegroundOrContinue(createRestoreForegroundInfo(), TAG, "RestoreWorker")\n\n'
                         '        // INV-3 backoff cap: cap effective retry delay at 300s (same logic as BackupWorker)\n', 1),
    # the log tag and the failure-reason key the tests read
    ('TAG-B', BW, '        internal const val TAG = "SMSBackup+"\n', 1),
    ('TAG-K', KC, '    private val TAG = "SMSBackup+"\n', 1),
    ('REASON-B', BW, '        const val KEY_FAILURE_REASON = "failure_reason"\n', 1),
    ('REASON-R', RW, '        const val KEY_FAILURE_REASON = "failure_reason"\n', 1),
    # U-070's fixtures as the new backup tests use them (the file is not edited)
    ('FX-harness', FX, 'internal class LateSmsHarness(val context: Context) {\n', 1),
    ('FX-sms', FX, '    val sms = ProviderDouble(DataType.SMS)\n', 1),
    ('FX-add', FX, '    fun add(date: Long, address: String?, body: String?, type: Int = SmsFixture.INBOX,\n', 1),
    ('FX-watermark', FX, '    fun watermark(): Long = dataTypePreferences.getMaxSyncedDate(DataType.SMS)\n', 1),
    ('FX-D', FX, '    const val D = 1_700_000_000_000L\n', 1),
    ('FX-transport', FX, 'internal class RecordingTransport : MailTransport {\n', 1),
    ('FX-appended', FX, '    val appended = mutableListOf<AppendedMessage>()\n', 1),
    ('FX-beforeAppend', FX, '    var beforeAppend: (() -> Unit)? = null\n', 1),
    ('FX-append-order', FX, '        calls.add("appendMessages")\n        beforeAppend?.invoke()\n        val r = result!!\n', 1),
    ('FX-close', FX, '    override fun closeFolders() {\n        calls.add("closeFolders")\n    }\n', 1),
    ('FX-count', FX, '    fun count(call: String) = calls.count { it == call }\n', 1),
    # the private fixtures of the two restore test classes that the added tests reuse
    ('IT-run', IT, '    private fun cancellableImapRun(itemCount: Int, cancelOnImportCall: Int, onCancel: () -> Unit): CancellableImapRun {\n', 1),
    ('IT-onCancel', IT, '                if (run.importCalls == cancelOnImportCall) onCancel()\n'
                        '                MessageImportResult(h?.uid ?: "unknown", DataType.SMS, items[index])\n', 1),
    ('IT-build', IT, '    private fun buildWorkerWithTransport(transport: MailTransport): RestoreWorker {\n', 1),
    ('IT-failing', IT, 'private open class FailingRestoreTransport(\n    private val checkSettingsBehavior: (() -> Unit)? = null\n'
                       ') : MailTransport {\n', 1),
    ('IT-fields', IT, '    private lateinit var checkpointStore: InMemoryCheckpointStore\n'
                      '    private lateinit var fakeProvider: FakeSmsContentProvider\n', 1),
    ('CT-interceptor', CT, '    private class CancellingInterceptor(\n        private val cancelAtIndex: Int,\n'
                           '        private val onCancel: () -> Unit\n    ) : RestoreInsertInterceptor {\n', 1),
    ('CT-onCancel', CT, '            if (insertIndex == cancelAtIndex) onCancel()\n', 1),
    ('CT-rows', CT, '    private fun countSmsRows(): Int = fakeProvider.rowCount()\n', 1),
    ('CT-items', CT, '    private fun createSmsItems(n: Int): List<ContentValues> {\n', 1),
    ('CT-build', CT, '        store: RestoreCheckpointStore = checkpointStore,\n'
                     '        interceptor: RestoreInsertInterceptor = RestoreInsertInterceptor.NoOp\n', 1),
]
START = SHARED + [
    ('E-B1', BW, E + LOG_B, 1),
    ('E-R1', RW, E + LOG_R, 1),
    ('E-R2', RW, '            return Result.retry()\n' + ISE + LOG_M, 1),
    ('E-R3', RW, '            ))\n' + ISE + LOG_M, 1),
    ('E-R4', RW, ISE + LOG_V, 1),
    ('E-K1', KC, K_CATCH + K_LOG, 1),
    ('T-I1', IT, IMP, 1),
    ('T-I2', IT, I2, 1),
    ('T-C1', CT, IMP, 1),
    ('T-C2', CT, C2, 1),
    ('NO-CALL-B', BW, 'rethrowIfCancelled', 0),
    ('NO-CALL-R', RW, 'rethrowIfCancelled', 0),
    ('NO-CALL-K', KC, 'rethrowIfCancelled', 0),
    ('NO-NEW-IT', IT, '_endsCancelled', 0),
    ('NO-NEW-CT', CT, '_endsCancelled', 0),
]
FIXED = SHARED + [
    ('E-B1/M-C1', BW, E + C_B + LOG_B, 1),
    ('E-R1/M-C2', RW, E + C_R + LOG_R, 1),
    ('E-R2/M-C3', RW, '            return Result.retry()\n' + ISE + C_X + LOG_M, 1),
    ('E-R3/M-C4', RW, '            ))\n' + ISE + C_L + LOG_M, 1),
    ('E-R4/M-C5', RW, ISE + C_V + LOG_V, 1),
    ('E-K1/M-C8', KC, K_CATCH + C_K + K_LOG, 1),
    ('CALLS-B', BW, 'rethrowIfCancelled(', 1),
    ('CALLS-R', RW, 'rethrowIfCancelled(', 4),
    ('CALLS-K', KC, 'rethrowIfCancelled(', 1),
    ('INFO-B', BW, 'logCancel = true', 1),
    ('INFO-R', RW, 'logCancel = true', 1),
    ('INFO-K', KC, 'logCancel', 0),
    ('DEF', WC, 'internal suspend fun rethrowIfCancelled(e: Exception, tag: String, site: String, '
                'logCancel: Boolean = false) {\n', 1),
    ('M-C6/M-C9', WC, '    if (context.isActive) return\n', 1),
    ('M-C7', WC, '    if (logCancel) Log.i(tag, "$site: run cancelled")\n', 1),
    ('T-I2-new', IT, '    fun doWork_imapRestore_realExceptionWhileCancelled_endsCancelledWithOneWarning()', 1),
    ('T-C2-new', CT, '    fun executeRestoreWithValues_illegalStateFromInterceptor_notCancelled_illegalState()', 1),
    ('T-I1-new', IT, IMP + 'import org.robolectric.shadows.ShadowLog\n', 1),
    ('T-C1-new', CT, IMP + 'import org.robolectric.shadows.ShadowLog\n', 1),
    ('NEW-BT', BT, 'class BackupWorkerCancelledRunTest {\n', 1),
    ('NEW-KT', KT, 'class WorkManagerCancelCollectorTest {\n', 1),
    ('NEW-WL', WL, 'internal fun workerLogs(priority: Int): List<ShadowLog.LogItem> =\n', 1),
]
mode = sys.argv[1] if len(sys.argv) > 1 else ''
if mode not in ('start', 'fixed'):
    print('usage: oldtext_check.py start|fixed')
    sys.exit(2)
checks = START if mode == 'start' else FIXED
bad = 0
cache = {}
for cid, path, text, want in checks:
    if path not in cache:
        cache[path] = open(path, encoding='utf-8').read() if os.path.exists(path) else ''
    n = cache[path].count(text)
    bad += n != want
    print(f'{cid} {path} occurrences={n} expected={want}' + ('' if n == want else ' MISMATCH'))
if mode == 'start':
    for path in (WC, BT, KT, WL):
        exists = os.path.exists(path)
        bad += exists
        print(f'NO-FILE {path} exists={exists} expected=False' + (' MISMATCH' if exists else ''))
print(f'RESULT mode={mode} mismatches={bad}')
sys.exit(1 if bad else 0)
```

`failset.py` — blob `5c9761bd29baa0319be496920cdbb384eb2dcc30` (self-tests 14.5):

<!-- file: sdlc/artifacts/build/sprints/sprint-025/U-074/evidence/tools/failset.py -->
```python
"""U-074: compare the failing cases of one targeted run with the plan's complete failing set for that run.
usage (from the repository root): python -I -X utf8 failset.py <cases.txt> <failsets.txt> <run-id>
<cases.txt> is U-063's junit_cases.py output for the run (CRLF or LF). <failsets.txt> holds sections: a line
'[<run-id>]' followed by one 'SimpleClassName#method' per line (an empty section is an empty set); blank lines and
lines starting with '#' are ignored. A case is failing when its status is FAIL or ERROR. Prints the failing cases,
then 'run=<id> cases=<n> failing=<k> expected=<m> matches=True|False', then 'missing=' and 'extra=' lines.
Exit 0 only when the sets are equal and the run has at least one case; exit 1 otherwise, and also when the
section is absent (an empty or unreadable run, or an unknown id, is never a match)."""
import sys

cases_path, sets_path, run_id = sys.argv[1:4]
cases, failing = 0, set()
for raw in open(cases_path, encoding='utf-8', errors='replace'):
    parts = raw.strip().split()
    if len(parts) < 2 or '#' not in parts[0]:
        continue
    cases += 1
    if parts[1] in ('FAIL', 'ERROR'):
        cls, _, method = parts[0].partition('#')
        failing.add(cls.rsplit('.', 1)[-1] + '#' + method)
sections, current = {}, None
for raw in open(sets_path, encoding='utf-8', errors='replace'):
    line = raw.strip()
    if not line or line.startswith('#'):
        continue
    if line.startswith('[') and line.endswith(']'):
        current = line[1:-1]
        sections.setdefault(current, set())
    elif current is not None:
        sections[current].add(line)
expected = sections.get(run_id)
for name in sorted(failing):
    print(f'failing {name}')
ok = expected is not None and cases > 0 and failing == expected
exp = expected if expected is not None else set()
print(f'run={run_id} cases={cases} failing={len(failing)} expected={len(exp) if expected is not None else "absent"} matches={ok}')
print('missing=' + ','.join(sorted(exp - failing)))
print('extra=' + ','.join(sorted(failing - exp)))
sys.exit(0 if ok else 1)
```

`failsets.txt` — blob `60b4259696daa500358af813cabd116525cfc28d` (the complete failing set of every targeted run; 23 sections; every
name checked against the test texts, 14.6):

<!-- file: sdlc/artifacts/build/sprints/sprint-025/U-074/evidence/tools/failsets.txt -->
```
# U-074 plan, section 4.8: the complete failing set of every targeted run (read by failset.py).
# Class sets: I2/I3 = CNEW + FG3 + LVSBT; I4 = REG; M-C*, M-1*, M-2*, M-3* = MUT; M-L* = U70 (plan 4.0).

[I2-red]
BackupWorkerCancelledRunTest#doWork_cancelledDuringSecondAppend_endsCancelledWithoutErrorLine
BackupWorkerCancelledRunTest#doWork_cancellationThrownByAppend_endsCancelled
BackupWorkerCancelledRunTest#doWork_realExceptionWhileCancelled_endsCancelledWithOneWarning
WorkManagerCancelCollectorTest#collect_jobCancelledWhileWaiting_logsNothing
RestoreWorkerIntegrationTest#doWork_imapRestore_cancelledDuringSecondImport_endsCancelledWithoutErrorLine
RestoreWorkerIntegrationTest#doWork_imapRestore_cancellationFromCheckSettings_endsCancelled
RestoreWorkerIntegrationTest#doWork_imapRestore_cancellationFromImport_endsCancelled
RestoreWorkerIntegrationTest#doWork_imapRestore_cancellationFromTransportFactory_endsCancelled
RestoreWorkerIntegrationTest#doWork_imapRestore_realExceptionWhileCancelled_endsCancelledWithOneWarning
RestoreWorkerCheckpointTest#executeRestoreWithValues_cancelledAfterThirdInsert_endsCancelledWithoutErrorLine

[I3-green]

[I4-regress]

[M-C1]
BackupWorkerCancelledRunTest#doWork_cancelledDuringSecondAppend_endsCancelledWithoutErrorLine
BackupWorkerCancelledRunTest#doWork_cancellationThrownByAppend_endsCancelled
BackupWorkerCancelledRunTest#doWork_realExceptionWhileCancelled_endsCancelledWithOneWarning

[M-C2]
RestoreWorkerIntegrationTest#doWork_imapRestore_cancelledDuringSecondImport_endsCancelledWithoutErrorLine
RestoreWorkerIntegrationTest#doWork_imapRestore_cancellationFromCheckSettings_endsCancelled
RestoreWorkerIntegrationTest#doWork_imapRestore_cancellationFromImport_endsCancelled
RestoreWorkerIntegrationTest#doWork_imapRestore_cancellationFromTransportFactory_endsCancelled
RestoreWorkerIntegrationTest#doWork_imapRestore_realExceptionWhileCancelled_endsCancelledWithOneWarning

[M-C3]
RestoreWorkerIntegrationTest#doWork_imapRestore_cancelledDuringSecondImport_endsCancelledWithoutErrorLine
RestoreWorkerIntegrationTest#doWork_imapRestore_cancellationFromCheckSettings_endsCancelled
RestoreWorkerIntegrationTest#doWork_imapRestore_cancellationFromImport_endsCancelled
RestoreWorkerIntegrationTest#doWork_imapRestore_realExceptionWhileCancelled_endsCancelledWithOneWarning

[M-C4]
RestoreWorkerIntegrationTest#doWork_imapRestore_cancelledDuringSecondImport_endsCancelledWithoutErrorLine
RestoreWorkerIntegrationTest#doWork_imapRestore_cancellationFromImport_endsCancelled
RestoreWorkerIntegrationTest#doWork_imapRestore_realExceptionWhileCancelled_endsCancelledWithOneWarning

[M-C5]
RestoreWorkerCheckpointTest#executeRestoreWithValues_cancelledAfterThirdInsert_endsCancelledWithoutErrorLine

[M-C6]
BackupWorkerCancelledRunTest#doWork_cancellationExceptionWhileActive_isAFailureAsToday
BackupWorkerCancelledRunTest#doWork_realExceptionWhileCancelled_endsCancelledWithOneWarning
RestoreWorkerIntegrationTest#doWork_imapRestore_cancellationExceptionWhileActive_isIllegalStateAsToday
RestoreWorkerIntegrationTest#doWork_imapRestore_realExceptionWhileCancelled_endsCancelledWithOneWarning
LateVisibleSmsBackupTest#ac9_cancelledDuringTheAppend_nothingSaved_rowsUploadedByTheNextBackup

[M-C7]
BackupWorkerCancelledRunTest#doWork_cancelledDuringSecondAppend_endsCancelledWithoutErrorLine
BackupWorkerCancelledRunTest#doWork_cancellationThrownByAppend_endsCancelled
BackupWorkerCancelledRunTest#doWork_realExceptionWhileCancelled_endsCancelledWithOneWarning
RestoreWorkerIntegrationTest#doWork_imapRestore_cancelledDuringSecondImport_endsCancelledWithoutErrorLine
RestoreWorkerIntegrationTest#doWork_imapRestore_cancellationFromCheckSettings_endsCancelled
RestoreWorkerIntegrationTest#doWork_imapRestore_cancellationFromImport_endsCancelled
RestoreWorkerIntegrationTest#doWork_imapRestore_cancellationFromTransportFactory_endsCancelled
RestoreWorkerIntegrationTest#doWork_imapRestore_realExceptionWhileCancelled_endsCancelledWithOneWarning

[M-C8]
WorkManagerCancelCollectorTest#collect_jobCancelledWhileWaiting_logsNothing

[M-C9]
BackupWorkerCancelledRunTest#doWork_illegalStateFromAppend_notCancelled_unrecoverableFailure
BackupWorkerCancelledRunTest#doWork_exceptionWithoutMessage_notCancelled_reasonUnknown
BackupWorkerCancelledRunTest#doWork_cancellationExceptionWhileActive_isAFailureAsToday
RestoreWorkerIntegrationTest#doWork_imapRestore_illegalStateFromImport_notCancelled_illegalState
RestoreWorkerIntegrationTest#doWork_exceptionFromTransportFactory_notCancelled_unrecoverableFailure
RestoreWorkerIntegrationTest#doWork_imapRestore_cancellationExceptionWhileActive_isIllegalStateAsToday
RestoreWorkerCheckpointTest#executeRestoreWithValues_illegalStateFromInterceptor_notCancelled_illegalState
WorkManagerCancelCollectorTest#collect_eventsFlowFails_logsWarning
LateVisibleSmsBackupTest#ac9_cancelledDuringTheAppend_nothingSaved_rowsUploadedByTheNextBackup

[M-1a]
BackupWorkerLoopCancellationTest#backupCursors_cancelledAfterSecondAppend_stopsAtNextPass
BackupWorkerCancelledRunTest#doWork_cancelledDuringSecondAppend_endsCancelledWithoutErrorLine

[M-1b]
BackupWorkerLoopCancellationTest#backupCursors_cancelledAfterSecondAppend_stopsAtNextPass
BackupWorkerCancelledRunTest#doWork_cancelledDuringSecondAppend_endsCancelledWithoutErrorLine

[M-2a]
RestoreWorkerIntegrationTest#doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass
RestoreWorkerIntegrationTest#doWork_imapRestore_cancelledDuringSecondImport_endsCancelledWithoutErrorLine

[M-2b]
RestoreWorkerIntegrationTest#doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass
RestoreWorkerIntegrationTest#doWork_imapRestore_cancelledDuringSecondImport_endsCancelledWithoutErrorLine

[M-3a]
RestoreWorkerCheckpointTest#executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass
RestoreWorkerCheckpointTest#executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing
RestoreWorkerCheckpointTest#executeRestoreWithValues_cancelledAfterThirdInsert_endsCancelledWithoutErrorLine

[M-3b]
RestoreWorkerCheckpointTest#executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass
RestoreWorkerCheckpointTest#executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing
RestoreWorkerCheckpointTest#executeRestoreWithValues_cancelledAfterThirdInsert_endsCancelledWithoutErrorLine

# U-070's rows: the planned sets of U-070's plan I8; the sets U-070 recorded govern (plan I0 step 3, row D20).
[M-L11]
LateVisibleSmsBackupTest#ac9_appendFails_nothingSaved_rowsUploadedByTheNextBackup
LateVisibleSmsBackupTest#ac9_cancelledDuringTheAppend_nothingSaved_rowsUploadedByTheNextBackup
LateVisibleSmsBackupTest#ac10_deathAfterTheServerConfirmed_onlyThatMessageTwice_oneCommitPerRowCarriesBoth

[M-L16a]
LateVisibleSmsSkipResetRestoreTest#ac14a_restoreFromTheServer_laterBackupUploadsNoneOfTheRestoredSms
LateVisibleSmsSkipResetRestoreTest#ac14b_deathAfterTheInsertBeforeTheWatermarkIsRaised_rowNotUploaded
LateVisibleSmsSkipResetRestoreTest#ac14c_lateRowBeforeARestoreThatRaisesTheWatermark_isUploaded

[M-L16b]
LateVisibleSmsSkipResetRestoreTest#ac14b_deathAfterTheInsertBeforeTheWatermarkIsRaised_rowNotUploaded

[M-L20]
LateVisibleSmsBackupTest#ac15_recordOnlyWritesKeepTheFirstRun_emptyFirstRunWritesTheSentinel
LateVisibleSmsBackupTest#ac21_rowsTheConverterRejects_recordedAsExamined_lateSmsNotStarved_noLogin

[M-L22]
LateVisibleSmsSkipResetRestoreTest#ac12_skipWithoutSms_leavesTheSmsWatermarkAndRecordUntouched
```

Reused read-only (committed): U-063 `sanitize.py`, `junit_totals.py`, `junit_cases.py`, `jacoco_pkgs.py`
(`sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools/`); U-069 `jacoco_classes.py`
(`sdlc/artifacts/build/sprints/sprint-024/U-069/evidence/tools/`).

### 3.8 Not changed

`WorkerForeground.kt`, `MainViewModel.kt`, `StatusPreference.java`, `WorkManagerScheduler.kt`, every typed catch,
every `finally`, both workers' constructors and test factories, `LateVisibleSmsFixtures.kt` and every other test file
not named above, every existing test (U-067's seven and U-070's included), `app/build.gradle`, `robolectric.properties`,
every contract, design and change record.

## 4. Implementation Steps

### 4.0 Environment and conventions (every step)

```bash
# From the repository root. Never set MSYS_NO_PATHCONV for Gradle.
unset MSYS_NO_PATHCONV
W=sdlc/artifacts/build/sprints/sprint-025/U-074; E=$W/evidence; M=$E/mutations; TL=$E/tools
T=sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools      # read-only: sanitize.py, junit_totals.py, junit_cases.py, jacoco_pkgs.py
U69T=sdlc/artifacts/build/sprints/sprint-024/U-069/evidence/tools   # read-only: jacoco_classes.py
R=app/build/test-results/testDebugUnitTest
JX=app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
S=app/src/main/java/com/zegoggles/smssync/service; TS=app/src/test/java/com/zegoggles/smssync/service
BW=$S/BackupWorker.kt; RW=$S/RestoreWorker.kt; KC=$S/WorkManagerCancelCollector.kt; WF=$S/WorkerForeground.kt; WC=$S/WorkerCancellation.kt
IT=$TS/RestoreWorkerIntegrationTest.kt; CT=$TS/RestoreWorkerCheckpointTest.kt; FX=$TS/LateVisibleSmsFixtures.kt
BT=$TS/BackupWorkerCancelledRunTest.kt; KT=$TS/WorkManagerCancelCollectorTest.kt; WL=$TS/WorkerLogs.kt
P=com.zegoggles.smssync.service
CNEW="--tests $P.BackupWorkerCancelledRunTest --tests $P.WorkManagerCancelCollectorTest --tests $P.RestoreWorkerIntegrationTest --tests $P.RestoreWorkerCheckpointTest"
FG3="--tests $P.BackupWorkerForegroundTest --tests $P.BackupWorkerForegroundApi24Test --tests $P.RestoreWorkerForegroundTest"
MUT="$CNEW --tests $P.BackupWorkerLoopCancellationTest --tests $P.LateVisibleSmsBackupTest"
NEW5="--tests $P.LateVisibleSmsRegressionTest --tests $P.LateVisibleSmsBackupTest --tests $P.LateVisibleSmsSkipResetRestoreTest --tests $P.BackupQueryBuilderWindowTest --tests com.zegoggles.smssync.preferences.SmsBackupWindowTest"
U70="$NEW5 $CNEW"
REG="--tests $P.BackupWorkerIntegrationTest --tests $P.BackupWorkerTest --tests $P.RestoreWorkerTest --tests $P.BackupWorkerLoopCancellationTest $NEW5"
CATCH_RE='catch \((e|t|ex|ignored|_)?: ?(Exception|Throwable|IllegalStateException|RuntimeException|CancellationException)\)|catch \((final )?(Exception|Throwable|IllegalStateException|RuntimeException|CancellationException) |runCatching'
PLAN_HEAD=2f58f927f503f016a87d7565110bcf636bbe5e36
WORK=<a u074-work folder inside the developer's own session scratch folder>   # raw logs, pristine copies
START=<the commit recorded in I0 step 2>
```
Shell state does not persist; each call sets what it needs. `WORK` is never written into a repository file (evidence
says "the work folder"). The class variables are used **unquoted** in the Gradle line so that each `--tests` and class
name is one argument (14.11).

**Gradle calls.** Tool timeout 600000 ms on every call; output to the work folder; the exit code appended by the same
command line. The two full gates take about 10 minutes each (U-070's `SmsBackupRecordSizeTest`), so the harness will move
them to the background: wait for the completion notice, never poll, start no other command meanwhile (above all no second
Gradle), then read the log; a log whose last line is not `exit=<n>` is a cut-off run (row D29). Targeted runs never
include `SmsBackupRecordSizeTest`.
- **G0** (I0) and **G-F** (I8), the only full gates, the story's command: `rm -rf $R; ./gradlew --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease :app:jacocoTestReport > $WORK/logs/I<k>-gates.log 2>&1; echo "exit=$?" >> $WORK/logs/I<k>-gates.log` (`rm -rf $R` removes the old XML results, so the test task runs again; the build cache is off, `gradle.properties` at `fd70325a`).
- **TR(name, classes)**: `rm -rf $R; ./gradlew --console=plain :app:testDebugUnitTest <classes> > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> $WORK/logs/<name>.log`, then
  **FS(name, id)**: `python -I -X utf8 $T/junit_cases.py $R > $E/<name>-cases.txt; python -I -X utf8 $TL/failset.py $E/<name>-cases.txt $TL/failsets.txt <id> > $E/<name>-failset.txt; echo "failset-exit=$?"`.
  For a mutation the two outputs go to `$M/` instead of `$E/`.

**Filter for `sanitize.py`** (`python -I -X utf8 $T/sanitize.py <in> <out> --grep '<regex>'`), gate filter **G**:
`'^(BUILD |FAILURE|> Task :app:(assemble|test|jacoco|lintVital|compile)|exit=)|tests completed|FAILED$|^e: |error:'`.

**EV(k)** (after a full gate; k = 0, 8):
```bash
python -I -X utf8 $T/sanitize.py $WORK/logs/I<k>-gates.log $E/I<k>-gates.txt --grep '<G>'
python -I -X utf8 $T/junit_totals.py $R > $E/I<k>-junit-totals.txt
python -I -X utf8 $T/jacoco_pkgs.py $JX > $E/I<k>-jacoco-packages.txt
python -I -X utf8 $U69T/jacoco_classes.py $JX com/zegoggles/smssync/service > $E/I<k>-service-classes.txt
```
`jacoco_pkgs.py` prints dotted names, so the line read is `com.zegoggles.smssync.service <covered>/<total> <percent>%`.

**Timestamps** from `date -u +%Y-%m-%dT%H:%M:%SZ`, run at that moment.

### I0 — Start state and baseline (no tracked file changes)

1. Create `$WORK/logs`, `$WORK/pristine`, `$E`, `$M`, `$TL`; write the three tools of 3.7 with the Write tool;
   `git hash-object $TL/oldtext_check.py $TL/failset.py $TL/failsets.txt` → `1aac0476e7ea15791d78ec97866441eae8b4f975`,
   `5c9761bd29baa0319be496920cdbb384eb2dcc30`, `60b4259696daa500358af813cabd116525cfc28d` (each derived from the plan text, 14.1; row D13).
2. **Start state** (one call into `$WORK/logs/I0-start.log`, sanitised to `$E/I0-start.txt`), each command echoed first:
   ```bash
   date -u +%Y-%m-%dT%H:%M:%SZ; git rev-parse HEAD; git rev-parse --abbrev-ref HEAD
   git merge-base --is-ancestor $PLAN_HEAD HEAD; echo "plan-head-ancestor-exit=$?"
   git log --oneline $PLAN_HEAD..HEAD
   git log --oneline $PLAN_HEAD..HEAD -- app
   grep -n '^status' sdlc/artifacts/build/sprints/sprint-023/U-067-*.md sdlc/artifacts/build/sprints/sprint-024/U-070-*.md sdlc/artifacts/build/sprints/sprint-024/U-075-*.md
   git status --porcelain --untracked-files=all -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat; echo "porcelain-end"
   for p in $BW $RW $KC $WF $IT $CT $FX; do echo "$(git rev-parse HEAD:$p) $(git hash-object $p) $p"; done
   python -I -X utf8 $TL/oldtext_check.py start; echo "oldtext-exit=$?"
   grep -n -F "                currentCoroutineContext().ensureActive()" $BW
   git grep -n -E "$CATCH_RE" HEAD -- app/src/main
   git grep -n -E "rethrowIfCancelled|WorkerCancellation" HEAD -- app; echo "names-exit=$?"
   git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"
   git grep -nE '^\s*@Ignore(\s|\(|$)' HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt'
   grep -v '^#' gradle/gradle-daemon-jvm.properties
   "$(command ls -d "/c/Program Files/Java/"jdk-25* | tail -1)/bin/jps.exe" -l | grep -iE 'gradle'; echo "jps-gradle-end"
   git status --porcelain --untracked-files=all -- sdlc/artifacts/design sdlc/artifacts/requirements sdlc/artifacts/build/change-records | cut -c4- | while read -r p; do sha256sum "$p"; done > $WORK/logs/others-I0.txt; cat $WORK/logs/others-I0.txt
   ```
   Expected: `plan-head-ancestor-exit=0`; the `-- app` log shows U-070's and U-075's code commits only; the three stories
   `status: done`; nothing before `porcelain-end`; for each file the two hashes are equal, and the blobs are
   **expected** `3f2f0c76…` (`$BW`), `9be01b15…` (`$RW`), `681f1dda…` (`$KC`), `0782f85f…` (`$WF`), `a6da69a5…` (`$IT`),
   `c4da4724…` (`$CT`), `e00ce4bb…` (`$FX`) (another value is row D5, not a stop when the text check passes);
   `RESULT mode=start mismatches=0`, `oldtext-exit=0` (row D4); the U-071 M-1 line at `:343` (recorded; after I3 `:344`);
   the 19 catch lines of 2.3 (the same texts at other line numbers are recorded; another broad catch on a run path is
   row D30); `names-exit=1` and no line; `C0` as printed with the commit id (**expected** 839; another value is recorded
   and explained, row D22); the `@Ignore` lines (two at `fd70325a`: `AppTest.java:13`, `AuthPreferencesTest.java:1079`);
   `toolchainVersion=25`; no Gradle JVM before `jps-gradle-end` (row D34); `others-I0.txt`: one `sha256sum` line per
   uncommitted path of someone else under the three document folders, or none (recorded, not a stop). Record
   `START=$(git rev-parse HEAD)` and `C0` at the top of the log. **Rows:** D1 to D5, D22, D23, D30, D34.
3. **U-070's recorded mutation sets** into `$WORK/logs/I0-u070.log`, sanitised to `$E/I0-u070-mutations.txt`:
   `for id in M-L11 M-L16 M-L20 M-L22; do awk -v id="$id" '$0 ~ "^### " id " " {on=1; print; next} /^#+ / {on=0} on' sdlc/artifacts/build/sprints/sprint-024/U-070/evidence/mutation-log.md; done`
   (whole entries up to the next heading; `M-L16` prints both `(a)` and `(b)`; 14.7). Translate U-070's ids (B10, S8, …)
   to test names through U-070's plan table 4.8 and compare with the `[M-L11]`, `[M-L16a]`, `[M-L16b]`, `[M-L20]`,
   `[M-L22]` sections of `failsets.txt` (planned from U-070's plan I8). **The recorded sets govern** (AC-12, "exactly the
   failing set"): if one differs, write `$TL/failsets-u070-recorded.txt` with the recorded sets under the same section
   names, record the difference, and use that file for those ids at I7 (row D20).
4. `./gradlew --version > $WORK/logs/I0-gradle-version.log 2>&1`, sanitised to `$E/I0-gradle-version.txt` (the Daemon JVM
   line cites `gradle/gradle-daemon-jvm.properties`, Java 25). **Row:** D34.
5. **Gate G0** (expect it in the background, 4.0), then EV(0). Expected `exit=0` as the log's last line; `failures=0
   errors=0`; `tests=T0`, `skipped=S0` (recorded); the `com.zegoggles.smssync.service` line of `I0-jacoco-packages.txt`
   recorded as `C_s/T_s` (no expected number; 674/822 at U-069's I6, U-070 and U-075 add lines). **Rows:** D7, D8, D29.
6. Create `$W/implementation-log.md`: story, plan path, `START`, `C0`, `T0`, `S0`, `C_s/T_s`, the `@Ignore` lines, the I0
   results by reference to the evidence files, the U-070 fixture mapping (as built = as planned, or the differences
   under row D4), the U-071 M-1 line number, and the note that U-069's, U-070's and U-075's tests are inside `C0`.

### I1 — The test files (no production change)

1. Write `$BT`, `$KT`, `$WL` (3.5) with the Write tool; apply T-I1, T-I2, T-C1, T-C2 (3.6) with the Edit tool (Read the
   lines first).
2. Checks into `$WORK/logs/I1-checks.log`, sanitised to `$E/I1-checks.txt`, each echoed:
   ```bash
   git hash-object $BT $KT $WL $IT $CT
   grep -c "@Test" $BT $KT $IT $CT
   git diff --numstat HEAD -- app/src/test
   git diff -U0 HEAD -- $IT | grep '^@@'; git diff -U0 HEAD -- $CT | grep '^@@'
   git status --porcelain --untracked-files=all -- app
   git diff --quiet HEAD -- app/src/main; echo "main-clean-exit=$?"
   ```
   Expected: `3e9d4701343cf0f1dfbb25b2cab7eaa66b32a7bb`, `28229a7dacbf1a77e940acd7d00725d4fc9fc16d`,
   `ffdaca2b15f056bbe1b426147298f6e94a6275ec`, and (with the start blobs of I0) `6d210473…` and `913d33e1…`; `@Test` counts 7, 2, 19,
   20; numstat exactly two lines `235	0	…RestoreWorkerIntegrationTest.kt` and `88	0	…RestoreWorkerCheckpointTest.kt`
   (no deleted line: AC-9, AC-11, AC-12); the four hunks of 3.6; porcelain exactly ` M $IT`, ` M $CT`, `?? $BT`,
   `?? $KT`, `?? $WL`; `main-clean-exit=0`. **Rows:** D13, D33, D35.

### I2 — Characterisation and red run on the unchanged production code

1. TR(`I2-red`, `$CNEW $FG3 --tests $P.LateVisibleSmsBackupTest`). Expected `exit=1`, the classes compiled (N1; row D12).
2. FS(`I2-red`, `I2-red`): expected `cases=92` (7 + 2 + 19 + 20 + 18 + 26), `failing=10`, `matches=True`,
   `failset-exit=0`. Then
   `for x in $R/TEST-*.xml; do grep -o 'message="[^"]*"' "$x"; done > $WORK/logs/I2-red-msg.log; python -I -X utf8 $T/sanitize.py $WORK/logs/I2-red-msg.log $E/I2-red-messages.txt`
   and `grep -c 'ends by throwing a CancellationException' $E/I2-red-messages.txt` → `9` (B1, B2, B6, R1 to R4, R8 and C1
   fail at that assertion, the cause of the defect: today the run returns a `Result`), and
   `grep -c 'entries at Log.WARN with tag SMSBackup+' $E/I2-red-messages.txt` → `1` (K1). The green half includes the
   eight characterisation tests and the control (B0, B3, B4, B5, R5, R6, R7, C2, K2), U-067's seven, U-070's B12
   (`ac9_cancelledDuringTheAppend_…`, AC-5) and U-075's F12 and R5 (the helper lets a cancellation through at the start
   commit, 2.3). **Rows:** D11, D12.
3. Append to the implementation log the red output (the ten failing tests with their first message line) for the story's
   "Red at Start", and the green list.

### I3 — The fix and the green run

1. Write `$WC` (3.1) with the Write tool.
2. Edits E-B1 (3.2), E-R1 to E-R4 (3.3), E-K1 (3.4) with the Edit tool (Read the lines first).
3. Checks into `$WORK/logs/I3-checks.log`, sanitised to `$E/I3-checks.txt`, each echoed:
   ```bash
   python -I -X utf8 $TL/oldtext_check.py fixed; echo "oldtext-exit=$?"
   git hash-object $WC $BW $RW $KC
   git diff --numstat HEAD -- app/src/main
   git diff -U0 HEAD -- $BW $RW $KC | grep '^@@'
   git status --porcelain --untracked-files=all -- app/src/main
   git grep -n --untracked "rethrowIfCancelled" -- app/src/main
   grep -n -F "                currentCoroutineContext().ensureActive()" $BW
   tr -cd '\r' < $WC | wc -c
   ```
   Expected: `RESULT mode=fixed mismatches=0`; `$WC` `48a8074c22b40bd3d27df27cefae9ee2d994ba8e`, and (with the start blobs of
   I0) `523c77a1…`, `1a6a6209…`, `a3b5a601…` (row D5 otherwise); numstat `1	0`, `4	0`, `1	0` for the three files and
   nothing else; the six hunks of 3.4 (each `,0 +<n> @@`: added lines only); porcelain exactly ` M $BW`, ` M $RW`,
   ` M $KC`, `?? $WC`; seven `rethrowIfCancelled` lines: `BackupWorker.kt:173`, `RestoreWorker.kt:199`, `:274`, `:351`,
   `:437`, `WorkManagerCancelCollector.kt:54`, `WorkerCancellation.kt:41`; the M-1 line at `:344`; the CR count of
   `$WC` recorded (0 expected; the recovery note uses it). **Rows:** D15, D30.
4. TR(`I3-green`, the classes of I2), then FS(`I3-green`, `I3-green`): expected `exit=0`, `cases=92 failing=0
   matches=True`. **Row:** D14.

### I4 — Regression run (AC-5, AC-9, AC-11, AC-12, AC-13)

1. TR(`I4-regress`, `$REG`), FS(`I4-regress`, `I4-regress`): expected `exit=0`, `failing=0 matches=True`; the case count
   recorded (BackupWorkerIntegrationTest 8, BackupWorkerTest 9, RestoreWorkerTest 13, BackupWorkerLoopCancellationTest 2
   at `fd70325a`, plus NEW5's 49 as built). This runs every test AC-9 names (the typed mappings), U-067's tests (with I3)
   and U-070's tests other than the size test, after the fix and before any mutation. **Row:** D14.
2. `git diff --numstat $START -- app/src/test` → exactly the two lines of I1 (no deleted line in any tracked test file;
   AC-9, AC-11, AC-12). **Row:** D33.

### I5 — Pristine copies, then this story's injections M-C1 to M-C9 (protocol and table 4.8)

1. Pristine copies and hashes into `$WORK/logs/I5-pristine.log`, sanitised to `$E/I5-pristine.txt`:
   ```bash
   cp $BW $RW $KC $WC $WORK/pristine/
   sha256sum $BW $RW $KC $WC $IT $CT $BT $KT $WL > $WORK/pristine.sha256; cat $WORK/pristine.sha256
   git hash-object -w $BW $RW $KC $WC
   git diff HEAD -- app/src/main | git hash-object --stdin
   ```
   Record the nine hashes, `HB HR HK HC` (written to the object store for recovery) and `PROD_DIFF` in the
   implementation log and the mutation log header.
2. The nine entries M-C1 to M-C9, in that order, each with TR(`<id>`, `$MUT`) and FS(`<id>`, `<id>`).

### I6 — U-067's six mutations (session 2)

The six entries M-1a, M-1b, M-2a, M-2b, M-3a, M-3b (4.8), each with TR(`<id>`, `$MUT`) and FS(`<id>`, `<id>`). Each
set contains the U-067 test(s) of U-067's log (AC-11) and the one new test that drives the same loop (B1, R1, C1); the
other U-067 tests and the other new tests pass.

### I7 — U-070's five rows (session 2)

The five entries M-L11, M-L16a, M-L16b, M-L20, M-L22 (4.8), each with TR(`<id>`, `$U70`) and FS(`<id>`, `<id>`)
(`failsets-u070-recorded.txt` in place of `failsets.txt` for an id that I0 step 3 found different). No new test fails
under them (derived: none of them changes what a cancelled or failing run returns or logs).

### I8 — Final gate and the final checks

0. **Clean check before the gate**: the 4.8 clean check and
   `grep -cE '^### [^ ]+ APPLYING ' $E/mutation-log.md` equal to `grep -cE '^(- )?restored: ' $E/mutation-log.md`
   (20 and 20; 14.13). **Rows:** D17, D32.
1. **Gate G-F** (expect it in the background, 4.0), then EV(8) and
   `python -I -X utf8 $T/junit_cases.py $R BackupWorkerCancelledRun WorkManagerCancelCollector RestoreWorkerIntegration RestoreWorkerCheckpoint > $E/I8-cases.txt`.
   Expected `exit=0` as the log's last line; `tests = T0 + 19`, failures 0, errors 0, skipped not above `S0`;
   `I8-cases.txt` `cases=48 pass=48`; in `I8-service-classes.txt`
   `CLASS com.zegoggles.smssync.service.WorkerCancellationKt covered=8 missed=1` (L18); the
   `com.zegoggles.smssync.service` ratio not below `C_s/T_s` (expected about +15 covered and +16 total lines: the helper's
   9 lines with 8 covered, the six statements, and collector lines first covered by K1 and K2); every floor passes.
   **Rows:** D16, D21, D29.
2. **Count (AC-16)**: `git grep --untracked -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`
   (working-tree form, stated as such) → `C0 + 19`. **Row:** D22.
3. **Final checks** into `$WORK/logs/I8-final.log`, sanitised to `$E/I8-final-checks.txt`, each echoed:
   ```bash
   python -I -X utf8 $TL/oldtext_check.py fixed; echo "oldtext-exit=$?"
   sha256sum -c --quiet $WORK/pristine.sha256; echo "sha-exit=$?"
   git status --porcelain --untracked-files=all -- app
   git diff --name-only $START -- app/src/main
   git diff --numstat $START -- app/src/test
   git diff --name-only $START -- build.gradle app/build.gradle k9mail-vendored/build.gradle settings.gradle gradle
   git log --oneline $START..HEAD -- app k9mail-vendored build.gradle settings.gradle gradle
   git status --porcelain --untracked-files=all -- sdlc/artifacts/design sdlc/artifacts/requirements sdlc/artifacts/build/change-records | cut -c4- | while read -r p; do sha256sum "$p"; done > $WORK/logs/others-I8.txt
   grep -vxF -f $WORK/logs/others-I0.txt $WORK/logs/others-I8.txt; echo "docs-new-or-changed-exit=$?"
   git log --format='%h %s' $START..HEAD -- sdlc/artifacts/design sdlc/artifacts/requirements sdlc/artifacts/build/change-records
   git grep -n --untracked "rethrowIfCancelled" -- app/src/main
   git grep -n -E "$CATCH_RE" -- app/src/main
   grep -nE '@Ignore|Assume' $BT $KT $WL; echo "ignore-exit=$?"
   grep -c "@Test" $BT $KT $IT $CT
   ```
   Expected: `mismatches=0`; `sha-exit=0` and no line (every file as at I5); porcelain exactly ` M $BW`, ` M $RW`,
   ` M $KC`, ` M $IT`, ` M $CT`, `?? $WC`, `?? $BT`, `?? $KT`, `?? $WL`; `--name-only` lists `$BW`, `$RW`, `$KC` (the new
   `$WC` is untracked until the orchestrator's commit; with the commit in place of the working tree it lists the four
   files of AC-17, hand-over item 1); the two test numstat lines of I1; the build-file list empty and no code commit after
   `$START` (row D23); `docs-new-or-changed-exit=1` with no line (U-075's rule: a path committed in between shows in the
   `git log` line with the orchestrator's subject); the seven `rethrowIfCancelled` lines of I3; the catch list of I0 with
   the three sites at their new numbers (`BackupWorker.kt:172` and `RestoreWorker.kt:198` unchanged, `:273`, `:350`,
   `:436`; `WorkManagerCancelCollector.kt:53`); `ignore-exit=1`; 7, 2, 19, 20. **Rows:** D23, D30, D31, D32, D33.

### I9 — Implementation log, hygiene scan, hand-off

1. Implementation log: every step's result by reference; the counts (5.3); the block "For the story's Implementation
   Notes" with: `START` and counts; U-070's fixture names as built; the IC-1 search output (I0, I8) with the statuses of
   2.3; the red output of I2; "which `WorkInfo.State` was not shown at unit level" (2.2; IC-3); AC-10's cursor statement
   (2.1); the stale comment (2.8); U-075's edge case 5 as decided (2.7 item 1); the hand-over items; the table of section
   10 item 3 for U-071.
2. **Hygiene scan** (last), `$WORK/logs/I9-scan.log`, sanitised to `$E/I9-scan.txt`:
   `grep -rnE '[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+\.[A-Za-z]{2,}|Users[/\\]|AppData' $W/implementation-log.md $E; echo "scan-exit=$?"`
   → `scan-exit=1`, no line. Row D27.
3. Hand-off: the 4.9 list and "no mutation entry lacks `restored`".

### 4.8 Mutation protocol (write-ahead; restore proven by hash) and table

`$E/mutation-log.md` header: "Protocol: U-074 plan 4.8 (story AC-15; U-067 plan I6 and U-069 AC-11 form: the production
change uncommitted; every changed production file saved and its `sha256sum` recorded first (I5 step 1); a write-ahead
entry; one Edit; one targeted run; restore by copying the saved file; the hashes shown equal to the saved ones;
`git status --porcelain -- app/src/main` listing only this story's files; a mutation is never present at a gate, a review
or a commit). Repository-relative paths."; `START`, the nine hashes, `HB HR HK HC`, `PROD_DIFF`. **Recovery note** (in the
header): "If a session is lost while an entry says `APPLYING` without a `restored` line, nothing else runs until the file
of that entry is restored only by `cp` from its pristine copy, or, if that copy is lost: `$BW`, `$RW`, `$KC` by
`git cat-file --filters --path=<path> <HB|HR|HK> > <path>`; `$WC` by `git cat-file blob <HC> > $WC` (LF; the `--filters`
form if I3 recorded a CR count above 0); then `sha256sum -c --quiet $WORK/pristine.sha256` exits 0. Never `git checkout` or
`git restore` (they would remove the fix)."

**Clean check** (before and after each entry): `sha256sum -c --quiet $WORK/pristine.sha256; echo "sha-exit=$?"` → 0, no
line; `git diff HEAD -- app/src/main | git hash-object --stdin` = `PROD_DIFF`; `git status --porcelain
--untracked-files=all -- app/src/main` = ` M $BW`, ` M $RW`, ` M $KC`, `?? $WC` exactly.

**Each entry:**
1. Clean check (row D17 on failure).
2. Write-ahead `### <id> APPLYING <date -u>`: file, its `sha256sum`, the exact edit, the class set, the section of
   `failsets.txt` it is compared with, and "every other case passes".
3. One Edit (Read the lines first).
4. TR(`<id>`, classes), FS(`<id>`, `<id>`) into `$M/`, and
   `for x in $R/TEST-*.xml; do grep -o 'message="[^"]*"' "$x"; done > $WORK/logs/<id>-msg.log; python -I -X utf8 $T/sanitize.py $WORK/logs/<id>-msg.log $M/<id>-message.txt`.
5. Restore at once: `cp $WORK/pristine/<file> <path>`, whatever the result.
6. Clean check; append `run_exit`, the `failset.py` line (`matches=…`, `missing=`, `extra=`), `verdict: detected |
   undetected | invalid (does not compile)`, and `restored: <date -u> sha256-ok diff-hash-ok porcelain-ok`.

The table gives each edit as `old` lines → `new` lines (` / ` separates lines; `\|` is `|`). The failing set is the
section of `failsets.txt` with the same id; "+" counts are complete over the classes run.

| Id | File | Exact edit | Classes | Complete failing set |
|----|------|------------|---------|----------------------|
| M-C1 | `$BW` | `            rethrowIfCancelled(e, TAG, "BackupWorker", logCancel = true)` / `            Log.e(TAG, "BackupWorker: unrecoverable error", e)` → `            Log.e(TAG, "BackupWorker: unrecoverable error", e)` | MUT | B1, B2, B6: 3 |
| M-C2 | `$RW` | `            rethrowIfCancelled(e, TAG, "RestoreWorker", logCancel = true)` / `            Log.e(TAG, "RestoreWorker: unrecoverable error", e)` → `            Log.e(TAG, "RestoreWorker: unrecoverable error", e)` | MUT | R1, R2, R3, R4, R8: 5 |
| M-C3 | `$RW` | `            rethrowIfCancelled(e, TAG, "RestoreWorker.executeRestore")` / `            Log.e(TAG, "RestoreWorker: IllegalStateException (possible memory)", e)` → `            Log.e(TAG, "RestoreWorker: IllegalStateException (possible memory)", e)` | MUT | R1, R2, R3, R8: 4 |
| M-C4 | `$RW` | `            rethrowIfCancelled(e, TAG, "RestoreWorker.runImapRestoreLoop")` / `            Log.e(TAG, "RestoreWorker: IllegalStateException (possible memory)", e)` → `            Log.e(TAG, "RestoreWorker: IllegalStateException (possible memory)", e)` | MUT | R1, R3, R8: 3 |
| M-C5 | `$RW` | `            rethrowIfCancelled(e, TAG, "RestoreWorker.executeRestoreWithValues")` / `            Log.e(TAG, "RestoreWorker.executeRestoreWithValues: IllegalStateException", e)` → `            Log.e(TAG, "RestoreWorker.executeRestoreWithValues: IllegalStateException", e)` | MUT | C1: 1 |
| M-C6 | `$WC` | `    if (context.isActive) return` → `    if (e !is CancellationException) return` (the rule keyed on the type) | MUT | B5, B6, R7, R8, U-070's `LateVisibleSmsBackupTest.ac9_cancelledDuringTheAppend_nothingSaved_rowsUploadedByTheNextBackup` (B12): 5 |
| M-C7 | `$WC` | `    if (logCancel) Log.i(tag, "$site: run cancelled")` / `    if (e is CancellationException) throw e` → `    if (e is CancellationException) throw e` | MUT | B1, B2, B6, R1, R2, R3, R4, R8 (every test that asserts the info line once): 8 |
| M-C8 | `$KC` | `                rethrowIfCancelled(e, TAG, "WorkManagerCancelCollector")` / `                Log.w(TAG, "WorkManagerCancelCollector: error collecting cancel for $uniqueWorkName", e)` → `                Log.w(TAG, "WorkManagerCancelCollector: error collecting cancel for $uniqueWorkName", e)` | MUT | K1: 1 |
| M-C9 | `$WC` | `    if (context.isActive) return` / `    if (e !is CancellationException) {` → `    if (e !is CancellationException) {` (this plan's addition: the guard for an active job removed) | MUT | B3, B4, B5, R5, R6, R7, C2, K2, U-070's B12: 9 |
| M-1a | `$BW` | `                currentCoroutineContext().ensureActive()` → `                coroutineContext.ensureActive()` (U-067's old binding) | MUT | U-067 `backupCursors_cancelledAfterSecondAppend_stopsAtNextPass`, B1: 2 |
| M-1b | `$BW` | `                currentCoroutineContext().ensureActive()` → `                if (false) currentCoroutineContext().ensureActive()` | MUT | the same 2 |
| M-2a | `$RW` | `                    currentCoroutineContext().ensureActive()` / (empty) / `                    val item = msgs[currentRestoredItem]` → `                    coroutineContext.ensureActive()` / (empty) / `                    val item = msgs[currentRestoredItem]` | MUT | U-067 `doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass`, R1: 2 |
| M-2b | `$RW` | the same three lines → `                    if (false) currentCoroutineContext().ensureActive()` / (empty) / `                    val item = msgs[currentRestoredItem]` | MUT | the same 2 |
| M-3a | `$RW` | `                    currentCoroutineContext().ensureActive()` / (empty) / `                    val values = items[currentRestoredItem]` → `                    coroutineContext.ensureActive()` / (empty) / `                    val values = items[currentRestoredItem]` | MUT | U-067 `executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass`, `executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing`, C1: 3 |
| M-3b | `$RW` | the same three lines → `                    if (false) currentCoroutineContext().ensureActive()` / (empty) / `                    val values = items[currentRestoredItem]` | MUT | the same 3 |
| M-L11 | `$BW` | `        val confirmedMaxDate: Long = transport.appendMessages(folder, result)` → `        dataTypePreferences.setMaxSyncedDateAfterAppend(type, DataType.Defaults.MAX_SYNCED_DATE, result.mapList)` / `        val confirmedMaxDate: Long = transport.appendMessages(folder, result)` | U70 | U-070's recorded set (planned: `ac9_appendFails_…`, `ac9_cancelledDuringTheAppend_…`, `ac10_deathAfterTheServerConfirmed_…`): 3 |
| M-L16a | `$RW` | `            preferences.dataTypePreferences.recordRestoredSms(values)` / `            val uri = ctx.contentResolver.insert(Consts.SMS_PROVIDER, values)` → `            val uri = ctx.contentResolver.insert(Consts.SMS_PROVIDER, values)` | U70 | U-070's recorded set (planned: `ac14a_…`, `ac14b_…`, `ac14c_…`): 3 |
| M-L16b | `$RW` | `            preferences.dataTypePreferences.recordRestoredSms(values)` / `            val uri = ctx.contentResolver.insert(Consts.SMS_PROVIDER, values)` → `            val uri = ctx.contentResolver.insert(Consts.SMS_PROVIDER, values)` / `            preferences.dataTypePreferences.recordRestoredSms(values)` | U70 | U-070's recorded set (planned: `ac14b_…`): 1 |
| M-L20 | `$BW` | `                    if (cursor.type == DataType.SMS) {` → `                    if (false) {` | U70 | U-070's recorded set (planned: `ac15_recordOnlyWritesKeepTheFirstRun_…`, `ac21_…`): 2 |
| M-L22 | `$BW` | `                if (type == DataType.SMS) {` → `                if (type == DataType.SMS \|\| type == DataType.MMS) {` | U70 | U-070's recorded set (planned: `ac12_skipWithoutSms_…`): 1 |

**How each set was derived** (from the test text of 3.5/3.6 and the production text of 3.1 to 3.4):
- M-C1 to M-C5 restore today's handling at one site. A test fails if its cancellation reaches that site: B1, B2 and B6
  end in site 1; R4 reaches site 2 directly, and R1, R2, R3, R8 reach site 2 after the inner sites rethrow; R2 is caught
  at site 3 (checkSettings is in `executeRestore`'s `try`), R1, R3, R8 at site 4 first and then site 3; C1 only site 5.
  Tests with an active job (B3 to B5, R5 to R7, C2) never pass the guard, so these edits do not touch them.
- M-C6: a `CancellationException` with the job active now ends the run by throwing (B5, R7, U-070's B12 through
  `runBlocking`); a real exception with the job cancelled is now handled as today (B6, R8). Every other case gives the same
  outcome under both rules.
- M-C7: every test that asserts exactly one `run cancelled` line (C1 asserts none and passes).
- M-C9: with the guard gone, an active job's real exception is first written as a warning (and, at `doWork`, an info
  line) before today's handling (B3, B4, R5, R6, C2, K2 assert no warning or exactly one), and an active
  `CancellationException` is rethrown (B5, R7, U-070's B12). U-070's `ac10_…` (a `SimulatedProcessDeath` through
  `doWork`'s catch with the job active) gets the extra lines but asserts no log, so it passes.
- M-1x, M-2x, M-3x: without the loop check the loop runs on after the cancel, so the run returns a `Result` (B1, R1, C1
  fail with U-067's tests); the tests whose callback throws (B2, B6, R3, R5, R7, R8, C2) end the run by the exception,
  not by the check, and pass.
- M-L rows: they change what is recorded or when, never what a cancelled or failing run returns or logs; the new tests'
  assertions on rows, watermarks and checkpoints are unaffected (L16: M-L11's extra early write keeps the watermark at
  `max(stored, -1)`).

**Counted detections:** M-C1 to M-C9 (9; AC-15 asks for M-C1 to M-C8). Every new test except the control B0 is in at
least one set. The mutants M-C6, M-C7 and M-C9 compile (scratch compile); M-C1 to M-C5 and M-C8 delete one statement.
If a counted mutant does not compile (row D18 (c)), its replacement is: M-C7 `    if (false && logCancel) Log.i(tag,
"$site: run cancelled")` in place of its first line; the others have none (stop).

### 4.9 Files for the orchestrator's commit (exact; the developer commits nothing)

`$W` = `sdlc/artifacts/build/sprints/sprint-025/U-074`.
- Code (9): `$S/BackupWorker.kt`, `$S/RestoreWorker.kt`, `$S/WorkManagerCancelCollector.kt`, `$S/WorkerCancellation.kt`,
  `$TS/BackupWorkerCancelledRunTest.kt`, `$TS/WorkManagerCancelCollectorTest.kt`, `$TS/WorkerLogs.kt`,
  `$TS/RestoreWorkerIntegrationTest.kt`, `$TS/RestoreWorkerCheckpointTest.kt`.
- Log: `$W/implementation-log.md`. Tools: `$W/evidence/tools/{oldtext_check.py,failset.py,failsets.txt}` (and
  `failsets-u070-recorded.txt` only under row D20).
- I0: `$W/evidence/I0-{start.txt,u070-mutations.txt,gradle-version.txt,gates.txt,junit-totals.txt,jacoco-packages.txt,service-classes.txt}`.
- I1: `I1-checks.txt`. I2: `I2-red-{cases,failset,messages}.txt`. I3: `I3-checks.txt`, `I3-green-{cases,failset}.txt`.
  I4: `I4-regress-{cases,failset}.txt`. I5: `I5-pristine.txt`.
- I5 to I7: `$W/evidence/mutation-log.md`; `$W/evidence/mutations/<id>-{cases,failset,message}.txt` for the 20 ids (60 files).
- I8: `I8-{gates,junit-totals,jacoco-packages,service-classes,cases,final-checks}.txt`. I9: `I9-scan.txt`.

9 code files and 89 workspace files (1 log, 3 tools, 7 + 1 + 3 + 3 + 2 + 1 + 61 + 6 + 1 = 85 evidence files with the
mutation log; one more under D20). Never staged for this story: the story file, the sprint plan, other stories'
workspaces, `compiled-pipeline.json`, `sdlc/.skill-context.json`, any design, contract or change record.

### 4.10 Sessions and hand-over

**Two sessions.** Session 1: I0 to I5 (G0 and 12 targeted runs). Session 2: I6 to I9 (11 targeted runs and G-F). Safe
stopping points, each followed by the resume checks (never while a Gradle run is in flight):
- after I0 (baseline frozen); after I2 (red evidence frozen; no production edit);
- after I4 (fix in place and green, regression run green; next step I5 step 1);
- between two mutation entries (each finished entry has its `restored` line); before I8 (every entry closed).

**Resume checks** (no Gradle): `git rev-parse HEAD` and `git log --oneline $START..HEAD` (a code commit by anyone is row
D23); `git status --porcelain --untracked-files=all -- app` as the stopping point leaves it; after I5:
`sha256sum -c --quiet $WORK/pristine.sha256` (rebuild `$WORK/pristine/` and `pristine.sha256` in the new session's work
folder from `HB HR HK HC` and the recorded hashes, as the recovery note says) and `PROD_DIFF` unchanged; `mutation-log.md`
has no `APPLYING` entry without `restored` (the two anchored counts of I8 step 0 equal). The hand-over block in the
implementation log gives: `Session <n> ends at <date -u>`, the last finished step, the next step, `START`, `C0`, `T0`,
`S0`, `C_s/T_s`, the nine hashes, `HB HR HK HC`, `PROD_DIFF`.

## 5. Tests and Counts

### 5.1 The 19 added tests

| # | Test (class) | Criterion | At start | Detected by |
|---|--------------|-----------|----------|-------------|
| B0 | `doWork_notCancelled_backsUpEveryRow_noWarningOrErrorLine_control` (BackupWorkerCancelledRunTest) | AC-13 (backup through `doWork`), N2 | PASS | none by design (a control) |
| B1 | `doWork_cancelledDuringSecondAppend_endsCancelledWithoutErrorLine` | AC-1, AC-7, AC-10 | FAIL, A "doWork() ends by throwing a CancellationException" | M-C1, M-C7, M-1a, M-1b |
| B2 | `doWork_cancellationThrownByAppend_endsCancelled` | AC-6 (site 1), AC-10 | FAIL, A (the same) | M-C1, M-C7 |
| B3 | `doWork_illegalStateFromAppend_notCancelled_unrecoverableFailure` | AC-8 (ii) | PASS | M-C9 |
| B4 | `doWork_exceptionWithoutMessage_notCancelled_reasonUnknown` | AC-8 (ii) | PASS | M-C9 |
| B5 | `doWork_cancellationExceptionWhileActive_isAFailureAsToday` | AC-5 | PASS | M-C6, M-C9 |
| B6 | `doWork_realExceptionWhileCancelled_endsCancelledWithOneWarning` | AC-4, AC-7 | FAIL, A (the same) | M-C1, M-C6, M-C7 |
| K1 | `collect_jobCancelledWhileWaiting_logsNothing` (WorkManagerCancelCollectorTest) | AC-14 | FAIL, A "entries at Log.WARN with tag SMSBackup+" | M-C8 |
| K2 | `collect_eventsFlowFails_logsWarning` | AC-14 | PASS | M-C9 |
| R1 | `doWork_imapRestore_cancelledDuringSecondImport_endsCancelledWithoutErrorLine` (RestoreWorkerIntegrationTest) | AC-2, AC-7, AC-10 | FAIL, A (the same) | M-C2, M-C3, M-C4, M-C7, M-2a, M-2b |
| R2 | `doWork_imapRestore_cancellationFromCheckSettings_endsCancelled` | AC-6 (site 3), AC-10 | FAIL, A | M-C2, M-C3, M-C7 |
| R3 | `doWork_imapRestore_cancellationFromImport_endsCancelled` | AC-6 (site 4) | FAIL, A | M-C2, M-C3, M-C4, M-C7 |
| R4 | `doWork_imapRestore_cancellationFromTransportFactory_endsCancelled` | AC-6 (site 2) | FAIL, A | M-C2, M-C7 |
| R5 | `doWork_imapRestore_illegalStateFromImport_notCancelled_illegalState` | AC-8 (i) | PASS | M-C9 |
| R6 | `doWork_exceptionFromTransportFactory_notCancelled_unrecoverableFailure` | AC-8 (iii) | PASS | M-C9 |
| R7 | `doWork_imapRestore_cancellationExceptionWhileActive_isIllegalStateAsToday` | AC-5 | PASS | M-C6, M-C9 |
| R8 | `doWork_imapRestore_realExceptionWhileCancelled_endsCancelledWithOneWarning` | AC-4, AC-7 | FAIL, A | M-C2, M-C3, M-C4, M-C6, M-C7 |
| C1 | `executeRestoreWithValues_cancelledAfterThirdInsert_endsCancelledWithoutErrorLine` (RestoreWorkerCheckpointTest) | AC-3, AC-7, AC-10 | FAIL, A "executeRestoreWithValues ends by throwing a CancellationException" | M-C5, M-3a, M-3b |
| C2 | `executeRestoreWithValues_illegalStateFromInterceptor_notCancelled_illegalState` | AC-8 (i) | PASS | M-C9 |

**Which site each test reaches** (AC-6): site 1 `BackupWorker.doWork`: B1, B2, B6; site 2 `RestoreWorker.doWork`: R1 to R4,
R8 (R4 directly); site 3 `executeRestore`: R1, R2, R3, R8; site 4 `runImapRestoreLoop`: R1, R3, R8; site 5
`executeRestoreWithValues`: C1; site 6 the collector: K1.

### 5.2 Red-run expectation (I2)

`cases=92 failing=10`: B1, B2, B6, R1, R2, R3, R4, R8, C1 at "… ends by throwing a CancellationException" (today the run
returns `Result.failure` and writes the error line), K1 at "entries at Log.WARN" (today the warning with the
`JobCancellationException`). The other 82 pass, among them B0, B3 to B5, R5 to R7, C2, K2, U-067's seven, U-070's B12 and
U-075's F12 and R5.

### 5.3 Counts (REQ-MODERNIZATION-015 AC-13 as amended by CR-016; AC-16)

- Start: `C0` by the authoritative command at `<start>` with its commit id (I0 step 2); expected 839. It contains U-069's,
  U-070's and U-075's tests (not this story's).
- Added: the 19 tests of 5.1, by name. Deleted: none. Changed: none (`--numstat` deletions 0 in both tracked test files).
- End: `C0 + 19` by the working-tree form with `--untracked` (I8 step 2), stated as such; after the orchestrator's commit
  the same command with that commit in place of `HEAD` (hand-over item 1).
- Gradle: `tests = T0 + 19`; failures 0; errors 0; skipped not above `S0`. A test added by another change after `<start>`
  is listed with its commit and not counted (row D22).

## 6. Criterion-to-Step Map

| Criterion | Where it is met | Evidence |
|-----------|-----------------|----------|
| AC-1 | B1; I2 red, I3 green; M-C1, M-C7, M-1a/b | `I2-red-*`, `I3-green-*`, mutation log |
| AC-2 | R1; M-C2, M-C3, M-C4, M-C7, M-2a/b | same |
| AC-3 | C1; M-C5, M-3a/b | same |
| AC-4 | B6, R8 (one warning, from `RestoreWorker.runImapRestoreLoop`); M-C6 | same |
| AC-5 | B5, R7; U-070's B12 in I2, I3, I4 unedited; M-C6, M-C9 | same; `I4-regress-*` |
| AC-6 | B2, R2, R3, R4 (5.1 site list) | same |
| AC-7 | the log assertions of B1, B2, B6, R1 to R4, R8, C1 and of the characterisation tests; M-C7 | same |
| AC-8 | B3, B4, R5, R6, C2 green at I2 and after; M-C9 | `I2-red-*`, `I3-green-*` |
| AC-9 | I4 (the named tests run); I1, I4, I8 numstat with 0 deletions | `I4-regress-*`, `I1-checks.txt`, `I8-final-checks.txt` |
| AC-10 | `closeFolders` and data assertions in B1, B2, B6, R1 to R3, R8, C1; cursor statement 2.1 | implementation log |
| AC-11 | U-067's seven in I2, I3, I4; I6 | mutation log |
| AC-12 | U-070's tests in I2, I3 (B12), I4 (NEW5); I7; fixture file unedited (I1 porcelain) | mutation log |
| AC-13 | U-067's three controls, B0, AC-5, AC-8, AC-9 | as above |
| AC-14 | K1, K2; M-C8 | as above |
| AC-15 | I5 to I7, table 4.8 | `mutation-log.md`, `mutations/` |
| AC-16 | G0, G-F; 5.3; build-file list empty | `I0-*`, `I8-*` |
| AC-17 | I3 and I8 checks (four files, six added lines); no step calls `diff` or `cmp` | `I3-checks.txt`, `I8-final-checks.txt` |
| IC-1 | 2.3; I0 and I8 catch search; `oldtext_check.py` `FG-*` | `I0-start.txt`, `I8-final-checks.txt` |
| IC-2 | 3.1 signature; I3 compile; `oldtext_check.py` `DEF`; no constructor or factory changed (`git diff` of I3) | `I3-checks.txt` |
| IC-3 | 2.2; `MainViewModel.kt`, `StatusPreference.java` not in the file list | implementation log |

## 7. Decision Table

| Row | Observation | What the developer does |
|-----|-------------|-------------------------|
| D1 | HEAD is not a descendant of `$PLAN_HEAD` | Stop and report; nothing has been edited |
| D2 | U-067, U-070 or U-075 is not `done`, or the `-- app` log lacks U-070's or U-075's code commit | Stop before any edit (start conditions 1, 2) |
| D3 | Porcelain shows a change under the checked paths at I0 | Stop; never stash, restore or commit someone else's change |
| D4 | `oldtext_check.py start` reports a mismatch (an anchor not once, a fixture name or order different, a name already present, a new file already there), or the Edit tool reports an `old_string` not found or not unique | Stop and report "plan text no longer matches <file>" with the tool output and `git log --oneline $PLAN_HEAD..HEAD -- <file>`. The developer never adapts an anchor, a test or a set: the SA writes a re-anchoring amendment from the start text |
| D5 | A start blob differs from the expected one while the text check passes | Record it (a story changed the file after its plan, for example in a review fix); the fixed blobs of I1 and I3 are then not predicted; the numstat, hunk and text checks still apply |
| D7 | G0 fails, or failures or errors above 0 | Stop and report (start condition 4) |
| D8 | `S0` above 2 | Record as pre-existing; AC-16 compares with `S0` |
| D11 | The red run differs from 5.2 | (a) A listed failing test passes: stop (it does not detect the defect). (b) A listed passing test fails: stop and report with the message. (c) B0 fails at a warning or error assertion: report the entry's text and class without copying any message content; the strict "no warning" assertions cannot be trusted; stop for an amendment. (d) K1 passes at start: the cancel was taken before the collector subscribed (N3); stop and report. (e) A failure at another assertion, or an exception of another type (an `AssertionError` from U-070's provider double, a `NoClassDefFoundError`): stop and report |
| D12 | A test file or the fix does not compile | Stop and report the compiler message (the files were compiled against stubs only); the developer does not change the text |
| D13 | A tool or test blob differs from the plan after the Write or Edit | Compare with the plan's text and correct until the blob matches; never run a variant |
| D14 | I3 or I4 has a failure | Stop and report; never change an assertion to fit |
| D15 | `oldtext_check.py fixed` mismatch, numstat or hunks differ, or the porcelain is not the four paths | Re-read the edits against the file and correct until they match; if the plan's text is the cause, stop and report |
| D16 | JaCoCo at I8 | (a) `service` below `C_s/T_s`, or any floor failing: stop and report (no floor is changed). (b) `WorkerCancellationKt` with `missed` above 1: stop and report (a path is not tested). (c) Other totals with `missed` 0 or 1: record (a different line table) |
| D17 | A clean check fails | Stop; apply the recovery note; never run a mutation on a dirty tree |
| D18 | A counted mutation's result | (a) An expected failing case passes: undetected; record, stop and report (AC-15 unmet). (b) Extra failing cases: record and explain each from the mutant; stop if one cannot be explained. (c) It does not compile: log `invalid (does not compile)` with the message; apply its listed replacement once (4.8), or stop if none is listed |
| D19 | A U-067 re-run (I6) fails a set other than its section | Stop and report (AC-11); U-071's rule (a later case may fail only under its own loop's mutation) is the same statement |
| D20 | U-070's recorded set (I0 step 3) differs from the planned section | Use the recorded set (`failsets-u070-recorded.txt`) and record the difference; at I7 the observed set must equal it, and none of this story's tests may fail; otherwise stop and report |
| D21 | G-F fails | Stop and report |
| D22 | End count not `C0 + 19`, Gradle tests not `T0 + 19`, or skipped above `S0` | Record and explain; a test added by another change after `<start>`: list it with its commit id, not as this story's |
| D23 | A build file listed, or a commit after `$START` touching `app`, `k9mail-vendored`, `build.gradle`, `settings.gradle` or `gradle`; or `docs-new-or-changed-exit` is 0 | Build file or code commit: stop and report. Documents: a path already uncommitted at I0 with the same hash at I8, or committed in between with the orchestrator's subject, is recorded and is not a stop; a path new or changed since I0 and still uncommitted: stop and report its path. The developer edits no document in these folders |
| D27 | The hygiene scan has a line | Fix the file through `sanitize.py`, re-scan; never hand off with a hit |
| D28 | "bash.exe … not found", or a process killed without a visible reason | Stop and report (endpoint security); do not work around it |
| D29 | The tool moves a Gradle run to the background (expected for G0 and G-F); or a Gradle log's last line is not `exit=<n>` | Background: wait for its completion notice; never poll; start nothing else meanwhile. A log without `exit=`: the run was cut off. Inside a mutation entry first finish protocol steps 5 and 6 (restore, clean check, `restored` line), then stop and report. Never rerun on a guess, never kill a daemon |
| D30 | A `rethrowIfCancelled` or catch grep differs from I0 step 2 or I3 step 3 | The same texts at other line numbers: record. Another broad catch on a worker's or the collector's run path, or an eighth `rethrowIfCancelled` line: stop and report |
| D31 | `ignore-exit` is not 1 | Stop (an `@Ignore` or `Assume` in a new test) |
| D32 | I8 step 0 or step 3: `sha-exit` not 0, `PROD_DIFF` differs, an `APPLYING` entry has no `restored` line, or the porcelain is not the nine paths | Stop. G-F (if it ran) is void. Apply the recovery note, record what was found, report; never hand off (AC-15: a mutation is never present at a gate) |
| D33 | A tracked test file shows a deleted line, or the `@Test` counts are not 7, 2, 19, 20 | Stop and report (AC-9, AC-11, AC-12); do not edit to fit |
| D34 | I0: `toolchainVersion` is not 25, `./gradlew --version` does not name the criteria file and Java 25, a Gradle JVM is listed before `jps-gradle-end`, or the `@Ignore` lines differ from the two of `fd70325a` | Stop before any Gradle call and report. A running Gradle JVM: start nothing. Other `@Ignore` lines: record them as pre-existing and report |
| D35 | I1: the porcelain is not the five paths, or `main-clean-exit` is not 0 | Stop and report; do not edit |

## 8. Contracts, Standards, Risks, Scope Concerns

### 8.1 Contracts and design (approved; read, not edited)

**CNTR-MODERNIZATION-004** (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md`, approved, **v4**
since CR-017, commit `74b31759`). Every statement on cancellation, failure, retries and stops, checked against the code
after this story:

| Line (HEAD) | Statement | After U-074 |
|-------------|-----------|-------------|
| `:110-112` | `cancel(jobKind)` "single-flight cancellation; structured-cancels the running coroutine and clears any scheduled retry" | True; unchanged (WorkManager's cancel; U-067's checks) |
| `:263-266` | `SchedulerState.Cancelled`; "Maps the WorkInfo lifecycle ENQUEUED -> RUNNING -> SUCCEEDED/FAILED/CANCELLED" | True as a mapping; `observe()` is still a stub (known gap, BUG-024 Evidence point 3, H-1) |
| `:323` | Rule 7 "`cancel` is structured. It MUST cooperatively cancel the running coroutine … and clear any scheduled retry" | True; unchanged |
| `:328` | "Worker failure → retry under INV-3 … Non-retryable failures return `Result.failure()` and surface as `SchedulerState.Failed(cause)`." | **Now true for cancels as well**: a cancelled run no longer returns `Result.failure()`. Case 2 (a `CancellationException` with the job active) still returns `failure`, which both library versions record as FAILED |
| `:329` | "No silent swallowing across the port. Failures surface to consumers via `observe(jobKind)` emitting `SchedulerState.Failed`" | Not made false by this story (it is false today because `observe()` is a stub, H-1 item 2). The collector's change silences only its own cancellation, which is not a failure; a real collector error is still logged (K2) |
| `:330` | "Process death is not an error … re-executed by WorkManager" | True; unchanged |
| `:331` | "**Cancellation is terminal, not failure.** A structured-cancelled job emits `SchedulerState.Cancelled`, distinct from `Failed`." | **Made true for the worker's own report** (no error line, no `Result.failure`). The sentence says nothing about a **system stop**, which is not a structured cancel and is not terminal (WorkManager re-enqueues it); it needs a new sentence (H-1), raised as SC-1 |

No sentence becomes false. One sentence needs to be added (H-1, pending): the story names it "CNTR-MODERNIZATION-004 v4",
but **v4 is now CR-017's**, and the line references in H-1 (`:284`, `:285`, `:287`) are `:328`, `:329`, `:331` at v4. SC-1.

**CNTR-MODERNIZATION-006** (`…/CNTR-MODERNIZATION-006-sync-state-repository.md`, approved): Rule 6 "No silent failure"
(`:294-297`: `tryEmitEvent` returns its boolean; `emitState` does not catch-and-suppress) and Rule 7 (`Cancel.Origin`,
`:298-301`) concern the repository's producers; the collector is a consumer of `events` and is unchanged in what it
consumes and does. Its Error Handling ("Errors are state, not exceptions across the boundary", `:305-308`) is not
touched: the workers publish no `SyncState` (they report through `WorkInfo`). **CNTR-MODERNIZATION-005** (broadcast) has
no statement on cancellation (search for "cancel": no line). No change needed in either.

**DES-MODERNIZATION-005** (approved): `:217-220` "Structured cancellation … cooperatively cancels the coroutine, which
unwinds the suspending loop at the next suspension point" and `:464-465` "Cancellation: structured — cancels the coroutine
and clears the scheduled retry": both true and now matched by the worker's report. **DES-MODERNIZATION-014** (approved):
F-1 and S4 (U-073) re-run these tests (hand-over H-2); no text changes.

**Standards.** No REQ with `type: standard` exists (U-069 plan 12, `git grep`; not re-run). REQ-MODERNIZATION-015 AC-13 as
amended by CR-016: 5.3.

### 8.2 Scope Concerns (for the orchestrator; each with a recommended ruling)

| # | Concern | Recommended ruling |
|---|---------|--------------------|
| SC-1 | The story's hand-over **H-1** asks for "CNTR-MODERNIZATION-004 v4" with references `:284`, `:285`, `:287`. Since CR-017 the contract is at v4, and those sentences are at `:328`, `:329`, `:331`. The sentence to add (after `:331`): "A worker whose job is cancelled ends by rethrowing the `CancellationException`; it returns no `Result`. WorkManager records `CANCELLED` for a cancel or a REPLACE and re-enqueues the work after a system stop; neither surfaces as `Failed`." plus the known gap that `observe()` is a stub (`:329` and `:331` are met today only through `MainViewModel`'s mapping, for manual runs) | Raise it as **v5** (non-breaking; no signature, `BackupType` or `SchedulerState` change) after this story closes and before U-073 is planned, with the references corrected; this story edits no contract (AC-17) |
| SC-2 | U-075's `ac12_run.py` (committed with U-075) recognises a system stop only by the old false-failure pair; after this story a stop of a fixed build reads `result=none unrecoverable=0`, which that tool classifies as nothing (2.4) | Do not edit U-075's evidence tool. Later device sections (U-071 IC-4, U-073, U-076) read a stop by the `run cancelled` grep of 2.4 and the absence of a `Worker result` line for the work; their planners are told (hand-over item 4) |
| SC-3 | The plan is stricter than the story: the control **B0** (19 tests, not 18); the injection **M-C9**; the complete failing set of **M-C7** has 8 tests, not the story's 2 (B1, R1); every test asserts the number of warnings; K2 makes **reading `events`** throw (inside the same `try`) instead of a flow whose collection throws (2.6) | Accept. The story's "18 tests" is a planning figure, not a criterion (Binding constraints); AC-15 says the plan derives the complete set and a row is met when observed equals derived |
| SC-4 | U-073's hand-over H-2 says "This story's 18-test set is the guard S4 re-runs" | Read it as the 19 tests of 5.1 |
| SC-5 | Under M-C6 U-070's B12 is expected to fail (the story asks the plan to confirm it) | Confirmed by derivation: B12 throws a `CancellationException` with the job active through `runBlocking`; under the type rule `doWork` rethrows it and `runBlocking` rethrows it into the test |

### 8.3 Risks

| # | Risk | Likelihood / impact | Mitigation |
|---|------|--------------------|------------|
| R1 | The tests do not compile on the real classpath (stubs used) | Low / medium | Scratch compile with the real library jars; D12 stops for an amendment |
| R2 | A normal run writes a warning, making "no warning" assertions fail after the fix | Low / medium | B0 at I2 shows it before any production edit; D11 (c) |
| R3 | K1 cancels before the collector subscribes, so it is green at start | Low / medium | The test waits for `subscriptionCount > 0`; D11 (d) |
| R4 | U-070 built its fixtures differently from its plan (names, the order of `beforeAppend` and recording) | Medium / medium | `oldtext_check.py start` (`FX-*`, `FX-append-order`); D4 amendment |
| R5 | A mutation's observed set differs from the derived set | Low / medium | `failset.py` compares as sets; D18 (b) requires an explanation per extra case |
| R6 | 25 Gradle runs take longer than planned; a session ends mid-way | Medium / low | Two sessions; stopping points; write-ahead log and recovery by blob |
| R7 | A full gate is cut off or overlapped by a second Gradle | High (moved) / medium | Two full gates only; `exit=` written by the command; wait for the notice; D29 |
| R8 | WorkManager 2.12.0 behaves otherwise at run time than its sources read | Low / medium | U-073 increment W (H-2) runs the state test on 2.12.0; this story's tests are its guard |
| R9 | The unreachable line 50 is counted differently by JaCoCo | Low / none | D16 (c) records; only `missed` above 1 stops |
| R10 | A later device check reads the old "stopped by the system" signature | Medium / low | 2.4 greps; SC-2; hand-over item 4 |
| R11 | The CRLF working tree and the Edit tool | Low / low | Blobs by `git hash-object` (LF); `oldtext_check.py` reads universal newlines (tested on CRLF trees, 14.4) |
| R12 | A real exception during a cancel is now a warning, not an error | By design / low | It keeps its throwable and the run's state is CANCELLED or ENQUEUED (2.2); AC-4 |
| R13 | Stack-trace recovery copies an exception | Low / none | Class and message compared, never identity (P1) |
| R14 | `ShadowLog` loses entries written on `Dispatchers.IO` | Very low / low | Concurrent queues (L12); the tests join before reading |

### 8.4 Out of scope (each ruled or recorded)

The thread refresh on a cancelled restore (bug SC-1; PO-1), any contract or design edit (H-1, SC-1), the WorkManager-level
state test (H-2, U-073), `MainViewModel.kt`, `StatusPreference.java`, `WorkManagerScheduler.kt`, `WorkerForeground.kt`,
the typed catches, dependencies and build files.

## 9. Optional Device Check (bug PO-2; for the orchestrator only; not a gate; only if the product owner allows it)

The story has **no device criterion** ("No device check is required"). If the product owner allows PO-2, the orchestrator
runs it on the Android 16 emulator, under these rules (U-075 section 9 and Record 14): **`Pixel_9_Pro` only**
(`Pixel_10_Pro` is never started or touched); no device setting changed; no screenshot and no UI dump in the repository
(dumps stay in the orchestrator's work folder, the record keeps extracted text only); `MSYS_NO_PATHCONV=1` only on the
single `adb` command that carries a device path, never in a Gradle shell; no wait above 540 s; an unobserved outcome is
recorded "not run: <reason>", never a pass. A manual backup uploads to the mail account configured on the emulator; the
product owner's allowance covers that.

1. Preconditions as U-075 section 9 (serial selected by `emu avd name` = `Pixel_9_Pro`; the debug APK of G-F installed;
   its SHA-256 recorded against the hand-off). `D0=$(dt)` as in U-075.
2. Enough SMS for a run of several seconds: `"$ADB" -s $SER emu sms send 15550740<nn> "u074 cancel <nn>"` for 40 numbers
   (test text only).
3. Open the app; find the sync button with U-075's `ui_bounds.py` on a dump kept in the work folder; tap it (Backup).
   `timeout 120 "$ADB" -s $SER logcat -m 1 -e 'BackupWorker: starting backup \(' > "$OW/c-start.txt"; echo "exit=$?"`
   (124: "not run: no start line"). Tap the same bounds again at once (the button cancels while a run is shown).
4. `timeout 120 "$ADB" -s $SER logcat -m 1 -e 'BackupWorker: (run cancelled|backup complete)' > "$OW/c-end.txt"; echo "exit=$?"`;
   then `"$ADB" -s $SER logcat -d -v threadtime -T "$D0" > "$OW/c-logcat.txt"` and the four greps of 2.4 on it.
   - **Pass:** `run cancelled` 1, false-failure 0, cancel warning 0, collector warning 0, and the status text "canceled"
     in a second dump (text extracted only).
   - **Not run: the backup finished before the cancel** (`backup complete` seen): recorded with the counts; repeat once.
   - **Fail:** a false-failure line, or a cancel warning without its explanation.
5. A normal manual backup to its end, then `grep -cF 'WorkManagerCancelCollector: error collecting cancel for '` over the
   log since its start: **pass** at 0 (AC-14 on a device).

Record `$E/PO2-emulator.txt` through `sanitize.py` (counts and outcomes; no message text). **What it proves:** on Android
16 a user cancel ends with one info line and no false failure line, and a normal finish writes no collector warning.
**What it does not prove:** a system stop or a REPLACE (no step provokes one; U-073's increment W and S4's device check),
WorkManager 2.12.0, other Android versions.

## 10. Items for the Orchestrator (hand-over)

1. **Commit after the reviews** (the developer commits nothing): the 4.9 list with explicit paths. Then re-run with the
   commit: `git diff --name-only <start> <commit> -- app/src/main` (the four files of AC-17), `git grep -n
   "rethrowIfCancelled" <commit> -- app/src/main` (seven lines), and the count with the commit (AC-16).
2. **SC-1 to SC-5** (8.2): rulings asked; none blocks the build (the plan follows the recommended rulings; a different
   ruling on SC-3 changes only the count and the failing sets of M-C7 and M-C9).
3. **For U-071** (its plan I0 step 13; copied into the implementation log): the later cases of its NEW3 classes and the
   loop each drives — R1: `runImapRestoreLoop` via `doWork` (site 2; fails under M-2a, M-2b); R3, R5, R7, R8: site 2
   (the callback throws; no U-067 mutation affects them); R2, R4, R6: none (before the loop); C1: `executeRestoreWithValues`
   (site 3; fails under M-3a, M-3b); C2: site 3 (throws; none). B1 (backup loop, site 1, M-1a, M-1b) is in
   `BackupWorkerCancelledRunTest`, outside U-071's NEW3. U-071's M-1 anchor moves from `:343` to `:344` (2.8).
4. **For U-071 IC-4, U-073 and U-076's device checks**: the four greps of 2.4; a system stop now shows
   `I SMSBackup+: BackupWorker: run cancelled` and no `Worker result` line; U-075's `ac12_run.py`'s "stopped by the system"
   outcome (`unrecoverable-cancel`) no longer occurs (SC-2).
5. **H-1** (contract, v5 per SC-1) and **H-2** (U-073 increment W) stand as the story gives them; H-3 (PO-1, PO-2) for
   the product owner.
6. **Close-out**: the stale comment (2.8); "which `WorkInfo.State`" statement (2.2); AC-10's cursor statement (2.1).

## 11. Constraints for the Developer (binding)

1. Gradle in the foreground, tool timeout 600000 ms on every call, never with `MSYS_NO_PATHCONV=1` set. No adb, no
   emulator, no device.
2. If the tool moves a run to the background, wait for its completion notice. Never poll; start nothing else meanwhile.
3. No background or detached shells, no `&`, no `sleep` or polling loops, no helper agents for shell work, no heredocs for
   editing (Write and Edit tools only).
4. On "bash.exe … not found", or a process killed without a visible reason: stop and report. Do not work around it;
   restore nothing from quarantine; add no exclusion.
5. **Do not call the external `diff` or `cmp`** (Record 18). Compare with `git diff`, `git diff --no-index`, `sha256sum`
   or `git hash-object`. Keep shell activity modest.
6. Raw logs and pristine copies stay in the work folder. Only sanitised evidence enters `$W`. Never write an e-mail
   address, host name, user name or user-profile path into a repository file.
7. Timestamps from `date -u +%Y-%m-%dT%H:%M:%SZ`.
8. **Commit nothing.** Never stage, stash, reset, amend, rebase or push.
9. Production edits only as 3.1 to 3.4, plus the logged mutations, each restored and proven by hash and porcelain. Test
   files only as 3.5 and 3.6. No edit to any existing test line, to U-070's fixtures, to U-075's files or to the story file.
10. No dependency, Gradle, build, ProGuard, manifest, `robolectric.properties` or verification-metadata change; no floor
    change; no `@Ignore`, no `Assume`.
11. No new file under `app/` other than the four of 4.9. No test text contains `@Test` except the 19 annotations.
12. Do not touch other work in the checkout: `sdlc/.skill-context.json`, other stories' workspaces (U-076's planning runs
    in parallel), the design and contract files, this workspace's plan and plan-gate files.

## 12. Quality Attribute Coverage (ISO/IEC 25010)

| Characteristic | Finding |
|----------------|---------|
| Functional Suitability | Correctness: a cancelled backup or restore ends cancelled with no false failure (AC-1 to AC-4, AC-6); genuine failures and typed mappings are unchanged (AC-5, AC-8, AC-9, AC-13). Completeness: all six catches that can receive a cancellation are handled or left on purpose (IC-1, 2.3); each is reached by a test (5.1) |
| Performance Efficiency | Time behaviour: one context read per caught exception, on the error path only; no new I/O. Resource use: unchanged; the collector's coroutine still completes on its cancel |
| Compatibility | Co-existence with WorkManager 2.9.1 and 2.12.0: the worker's report matches the state each records (2.2); the rule avoids 2.12.0's "self-cancelled = failure" branch for every stop. No signature, constructor, Hilt binding or seam change (IC-2) |
| Usability | The status screen and notification are unchanged (2.2). The owner's log now tells a cancel from a failure (2.4) |
| Reliability | Fault tolerance: a real exception during a cancel is kept (warning with throwable) rather than lost or misreported; cleanup (`closeFolders`, cursors, checkpoint, watermark) unchanged (2.1, AC-10). Recoverability: a stopped run is re-enqueued by WorkManager as before. Maturity: no longer depends on 2.9.1 dropping a stopped worker's result (the reason for the order before U-073) |
| Security | No new permission, component or data flow; the new lines carry no message content (site names and the throwable only, as the existing lines); the collector's change removes noise without hiding real errors (K2) |
| Maintainability | Modifiability: one 10-line helper, six one-line call sites (AC-17). Testability: 19 tests through `doWork()` and the collector's public function, no seam; every test but the control detected by an injection (4.8). Analysability: fixed texts and levels (2.4) with self-tested greps |
| Portability | No platform API involved; the tests run at the pinned SDK 29; nothing depends on an Android version |

## 13. Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| U-074 story | `sdlc/artifacts/build/sprints/sprint-025/U-074-cancelled-run-is-not-reported-as-a-failure.md` | Read in full |
| BUG-024 | `sdlc/artifacts/stories/BUG-024-cancelled-backup-or-restore-reported-as-failure.md` | Read in full (Suggested Approach sections 0 to 8) |
| Decision appendix | `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` | Records 18 and 21 to 23 |
| U-075 plan | `sdlc/artifacts/build/sprints/sprint-024/U-075/plan.md` | Form; 3.1 to 3.4 (applied in the simulation), 3.5 (`oldtext_check.py`, `ac12_run.py`), 4.0 to 4.10, 5, 7, 9, 10, 14 |
| U-069 plan | `sdlc/artifacts/build/sprints/sprint-024/U-069/plan.md` | Form; its evidence `I2-red-cases.txt`, `mutation-log.md` (self-tests) |
| U-070 plan and log | `sdlc/artifacts/build/sprints/sprint-024/U-070/plan.md` (4.1 fixtures in full, 4.3 `ac9`/`ac10`/`ac22`, 4.8 table, I8 rows M-L11, M-L16, M-L20, M-L22), `implementation-log.md` (grep) | Fixtures, B12, re-run rows |
| U-067 evidence | `sdlc/artifacts/build/sprints/sprint-023/U-067/evidence/mutation-log.md` | The six mutations and their detecting tests |
| U-071 plan | `sdlc/artifacts/build/sprints/sprint-025/U-071/plan.md` (grep: U-074, D64, I0 step 13, the M-1a to M-3b rows) | Interaction (2.8, hand-over item 3) |
| Sprint plan | `sdlc/artifacts/build/sprints/sprint-025/sprint-plan.md` (grep) | Order and dependencies |
| CNTR-MODERNIZATION-004 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` | Versioning, `:100-118`, `:255-275`, `:285-296`, `:321-335`; "cancel", "failure", "retry" search |
| CNTR-MODERNIZATION-006, -005 | `sdlc/artifacts/design/contracts/` | `:275-310`, `:340-365` of -006; "cancel" search of both |
| DES-MODERNIZATION-005, -014 | `sdlc/artifacts/design/modernization/` | Status; "cancel" search; -005 `:214-228`, `:462-466`; -014 `:1107` |
| Source at `fd70325a` | `service/WorkManagerCancelCollector.kt` (full), `service/RestoreCheckpointStore.kt`, `service/RestoreInsertInterceptor.kt`, `service/state/SyncStateRepository.kt`, `service/state/SyncEvent.kt` (grep), `service/BackupCursors.java` (`:25-110`), `mail/MessageConverter.java`, `mail/PersonLookup.java` (`:44-84`), `mail/DataType.java` (`:14-40`), `mail/transport/MailTransport.java`, `mail/transport/K9MailTransport.java` (`:444-453`), `mail/BackupImapStore.java` (`:88-98`), `preferences/EncryptedPrefsSecretStore.java` (`:140-147`), `App.java` (`:230-238`), `activity/MainViewModel.kt` (grep), `di/MailModule.kt` (grep), `app/build.gradle` (`:60-115`, rules), `gradle.properties` | The code changed and its neighbours |
| Start text (simulated) | `BackupWorker.kt` 3f2f0c76 and `RestoreWorker.kt` 9be01b15 (full), `WorkerForeground.kt` 0782f85f; U-070 blobs `6af6f455` (`BackupItemsFetcher.java`, `:40-225`), `38de9961` (`DataTypePreferences.java`, `setMaxSyncedDateAfterAppend`), and grep of the other five | Every site and line |
| Tests at `fd70325a` | `service/RestoreWorkerIntegrationTest.kt` (full), `service/RestoreWorkerCheckpointTest.kt` (`:1-120`, `:400-688`), `BackupWorkerLoopCancellationTest.kt`; `@Test` counts of six classes | Fixtures, anchors, counts |
| Library artifacts (local caches; `javap`, `unzip`) | `work-runtime-2.9.1.aar` (`CoroutineWorker`, `$startWork$1`, `ListenableWorker`), `work-runtime-2.12.0-sources.jar` (`WorkerWrapper.kt`, `CoroutineWorker.kt`, `ListenableFuture.kt`), `shadows-framework-4.15.1.jar` (`ShadowLog`), `robolectric-4.15.1.jar` | L8, L10, L12 |
| Scratch compile | `kotlin-compiler-embeddable` 2.3.21 and its runtime jars; the jars named under "Compile check" | 14.2 |
| Evidence tools | U-063 `sanitize.py`, `junit_cases.py`, `junit_totals.py`, `jacoco_pkgs.py`; U-069 `jacoco_classes.py` | Reused; text read |
| Real log | `sdlc/artifacts/build/research/evidence/2026-11-21-background-backup-app-log.txt` | Grep self-test (14.9) |
| Template, report format | plugin cache `templates/build-artifacts/build-plan.md`; U-075's Phase Completion Report | Frontmatter, report |

## 14. Self-Test of the Plan's Commands (ruling 73)

Run on 2026-11-21 in this session's scratch folder, outside the repository, with no Gradle, no device and no read of the
working tree under `app/`. **Every embedded file, edit text and mutation text tested is the copy extracted from this plan
file** (14.1), not a scratch original. "Constructed" inputs are written in the format of the real output; "real" inputs
are committed files. Python on this machine writes CRLF to a redirected stdout; `ls` is aliased to `ls -F` (the plan uses
`command ls` where a listing is read); each is handled where it matters.

| # | Command as it stands in the plan | Input | Expected | Observed |
|---|----------------------------------|-------|----------|----------|
| 14.1 | Blob of every embedded file: the first fenced block after its `<!-- file: … -->` marker, LF with a final newline, `git hash-object --no-filters` and SHA-1 of `blob <n>\0<data>` | this plan's text | the blobs printed in 3.1, 3.5, 3.7; equal to the drafts | all equal; both methods agree; no CR in any block |
| 14.2 | Scratch compile (Kotlin 2.3.21) of the extracted helper, the collector with E-K1, mirror stubs of the workers, the three new test files, both restore classes with T-I1, T-I2, T-C1, T-C2, and the mutants M-C6, M-C7, M-C9 | extracted texts and stubs | exit 0 | exit 0 for all; only the existing "no cast needed" warnings |
| 14.3 | Every Edit (old once, then new) on the simulated start | simulated start tree | each old text once; blobs and hunks of 3.4 and 3.6 | each once; `523c77a1…` 1/0 `@@ -172,0 +173 @@`; `1a6a6209…` 4/0 with four hunks; `a3b5a601…` 1/0; `6d210473…` 235/0, 19 `@Test`; `913d33e1…` 88/0, 20 `@Test` |
| 14.4 | `python -I -X utf8 $TL/oldtext_check.py start` / `fixed` | simulated start and fixed trees, tracked files CRLF | `mismatches=0` in each mode; the cross modes fail; no argument exit 2 | as expected (cross modes 21 and 15 mismatches) |
| 14.5 | `failset.py <cases> failsets.txt <id>` | constructed CRLF `junit_cases.py` outputs (match, missing, extra, all pass, empty, unknown id); real U-069 `I2-red-cases.txt` | exit 0, 1, 1, 0, 1, 1; real exit 0 | as expected |
| 14.6 | Every `Class#method` of `failsets.txt` resolves to `fun method(`; section sizes equal table 4.8 | extracted tests; fixed restore classes; `BackupWorkerLoopCancellationTest` at `fd70325a`; U-070's plan text | 23 sections, 73 names, 0 unresolved | as expected |
| 14.7 | I0 step 3 `awk` | constructed U-070-style log with `M-L16 (a)`, `(b)` and a decoy `M-L160`; real U-069 log | whole entries only | 3 entries printed, decoy not; real entry printed |
| 14.8 | `git grep -n -E "$CATCH_RE"` with the plan's assignment | `fd70325a`; simulated start and fixed files | 17 lines at `fd70325a`; 8 run-path lines at start; new numbers after the fix | 17; 8 (BW:172, RW:198/272/348/433, KC:53, WF:47, BIF:99); after: RW:273/350/436 |
| 14.9 | The four greps of 2.4 | constructed LF and CRLF logcat lines with decoys; committed log of 2026-11-21 | 2, 2, 2, 1; real log 0 each | as expected (the false-failure grep run directly; the plan-text extractor found 3 of 4 lines: to be fixed on resumption) |
| 14.10 | `git grep -h "@Test" … \| grep -c "@Test"` at `fd70325a`; `grep -c "@Test"` of the new texts; `@Ignore\|Assume` grep | repository; extracted texts | 771; 7, 2, 19, 20; no `@Test` text other than annotations; exit 1 | as expected |
| 14.11 | 4.0 class variables, used unquoted | the plan's 4.0 lines | each `--tests` and class one argument; no size test | CNEW 4, FG3 3, MUT 6, NEW5 5, U70 9, REG 9 `--tests`; size test absent |
| 14.12 | `sha256sum -c --quiet $WORK/pristine.sha256` | copies; one mutated, then restored | 0 and no line; 1 with `FAILED`; 0 after restore | as expected |
| 14.13 | `grep -cE '^### [^ ]+ APPLYING '` against `grep -cE '^(- )?restored: '` | constructed log of the 20 ids, then one open entry | 20 = 20; 21 against 20 | as expected |
| 14.14 | Plan hygiene: names, profile paths, e-mail tokens, CR, control characters | this plan | none | none; 7 tabs (the `--numstat` examples) |
| 14.15 | The 20 rows of table 4.8 on the simulated fixed tree | extracted table text | each old text once; restore by copy gives an equal hash | all once; all restore hashes equal |

Not testable here, and why: every Gradle step (compile on the real classpath, the test runs, the red/green split, JaCoCo's
line counts, the mutation runs), the emulator steps of section 9, and the working-tree state at `<start>` (the developer's
I0 measures it; a mismatch is a stop and an amendment, row D4).

## Verdict

Not given: this is a draft saved at the owner's stop (NEEDS_INFO). On resumption: re-run the section 14 extraction on this file, fix the 14.9 extractor, self-test section 9's commands, decide the verdict, then send the plan to its gates. All self-tests run so far passed; SC-1 to SC-5 await the orchestrator.

## Phase Completion Report
---
story_id: "U-074"
phase: "planning"
verdict: "NEEDS_INFO"
artifact_path: "sdlc/artifacts/build/sprints/sprint-025/U-074/plan.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
notes: "Draft saved at the owner's stop; planning to be resumed. Complete: sections 0-8, 10-13 and every embedded text (self-tested). Partial: sections 9 and 14. Not started: verdict and plan gates."
---
