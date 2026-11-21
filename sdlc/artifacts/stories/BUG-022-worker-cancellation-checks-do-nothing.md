---
type: bug
status: ready
artifact_type: bug
severity: low
priority: medium
complexity: low
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items:
  - DES-MODERNIZATION-014
  - REQ-MODERNIZATION-016
  - U-060
  - U-015
  - CNTR-MODERNIZATION-004
  - DES-MODERNIZATION-005
platforms: []
tags:
  - coroutines
  - workmanager
  - cancellation
id: BUG-022
title: Backup and restore workers' cancellation checks are bound to a dispatcher and never throw
domain: build
origin: code-review
updated_at: '2026-11-21T08:18:32.327Z'
---

# BUG-022: Backup and restore workers' cancellation checks are bound to a dispatcher and never throw

## Description

The backup worker and the restore worker each have a loop that is meant to stop when the work is cancelled. At the top of each pass the loop calls `coroutineContext.ensureActive()`. There are three such calls:

| # | File and line | Enclosing function | Loop |
|---|---------------|--------------------|------|
| 1 | `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt:331` | `private suspend fun backupCursors` (`:305`) | `while (cursors.hasNext())` (`:330`) |
| 2 | `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt:311` | `private suspend fun runImapRestoreLoop` (`:289`) | `while (currentRestoredItem < itemsToRestoreCount)` (`:310`) |
| 3 | `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt:399` | `internal suspend fun executeRestoreWithValues` (`:378`) | same shape (`:398`); the test-facing path |

Each file imports `kotlin.coroutines.coroutineContext` (`BackupWorker.kt:57`, `RestoreWorker.kt:66`), which is the property that would give the running coroutine's context, and `kotlinx.coroutines.ensureActive` (`BackupWorker.kt:54`, `RestoreWorker.kt:63`). That is clearly the intent. But each call is inside a member function of a class that extends `androidx.work.CoroutineWorker` (`BackupWorker.kt:116`, `RestoreWorker.kt:136`), not inside a `withContext { }` block and not inside a top-level function. In all three places an unqualified `coroutineContext` therefore finds the worker's own member property first. The compiler says which property it found: `'val coroutineContext: CoroutineDispatcher' is deprecated. use withContext(...) inside doWork() instead.` (see Evidence). That property is a dispatcher, a thread-pool setting. It holds no job, so there is nothing for `ensureActive()` to check, and it never throws.

The result is that three checks that look like cancellation points are not. Cancellation can still reach the workers, but only at some other suspension point (see Actual Behavior).

Two further facts:

- **It is old.** The checks were written in story U-015 (sprint 001) and passed code review as working (`sdlc/artifacts/build/sprints/sprint-001/U-015/review-code.md:26`, "[PASS] Cooperative cancellation via coroutineContext.ensureActive()"). The same three warnings appear in the build log from before U-060 (`sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/baseline-gates.log:88-90`), so U-060 did not cause them. It only recorded them as debt.
- **Nothing tests it.** The worker tests assert WorkManager's own state after `cancelUniqueWork`, not that the loop stops (see Existing Tests).

Found by design DES-MODERNIZATION-014 (finding F-1, `sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-toolchain-runtime-libraries-platform-level-design.md:903-917`). That design decided not to fix it there, because a fix changes cancellation behaviour and REQ-MODERNIZATION-016 is an upgrade that must not (`:913-914`).

## Steps to Reproduce

This bug has a compile-time observation and a behavioural one. Neither was run for this write-up (Gradle was not run, by instruction). The first is read from saved logs. The second is a described test, not an observed result.

**A. Compile-time observation (from saved logs, no new run needed).**

1. Open `sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/final-gates.log`, lines 108-110.
2. Observe three Kotlin warnings, one per call site:
   - `BackupWorker.kt:331:17 'val coroutineContext: CoroutineDispatcher' is deprecated. use withContext(...) inside doWork() instead.`
   - `RestoreWorker.kt:311:21` (same text)
   - `RestoreWorker.kt:399:21` (same text)
3. To see it fresh, compile the app module and read the Kotlin warnings. Run Gradle in a shell without `MSYS_NO_PATHCONV` set.
4. Cross-check: the minified-class mapping from the same story lists the field behind the name as `kotlinx.coroutines.CoroutineDispatcher coroutineContext` (`sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/after-mapping.txt:47342`), in contrast to the `kotlin.coroutines.CoroutineContext coroutineContext` entries (`:30382`, `:30595`, `:109566`), which are other classes' fields.

**B. Behavioural reproduction at unit level (described, not run).**

The existing unit set-up can show the restore loop at call site 3, because that function is `internal` and already called directly by tests (`app/src/test/java/com/zegoggles/smssync/service/RestoreWorkerCheckpointTest.kt:352`, `:375`).

1. Build a restore worker as `RestoreWorkerCheckpointTest.buildWorker()` does (`:104`) and make five or more SMS `ContentValues` with its `createSmsItems(n)`.
2. Inside `runBlocking`, start a child coroutine with `start = CoroutineStart.UNDISPATCHED`. In its first statement cancel its own job (`coroutineContext[Job]!!.cancel()` written against the test's own scope, which is not the worker's property). Then call `executeRestoreWithValues(items, 0, preferences, context, workName)`.
3. Nothing suspends between the cancel and the first `ensureActive()`. A check that observes the real job therefore throws before the first insert. Expected: zero SMS rows inserted (`countSmsRows()` equals 0) and no checkpoint written (`checkpointStore.read(workName)` equals `NO_CHECKPOINT`).
4. With the bug, the check does nothing and the first item is inserted before any later suspension point can notice. Expected today: `countSmsRows()` is at least 1.
5. Why this form: it does not depend on what `setProgress` does under the test harness, which is not known (see Evidence, "Not verified"). The assertion is on rows and checkpoint, not on a `CancellationException`, because the loop sits inside `catch (e: IllegalStateException)` (`RestoreWorker.kt:433`), and `CancellationException` extends `IllegalStateException`. A working check would therefore come back as `Result.failure("illegal_state")`, not as a thrown exception. This is a Scope Concern (SC-1), not something this bug changes.

What cannot be shown at unit level with the present set-up:

- **Call sites 1 and 2.** `backupCursors` and `runImapRestoreLoop` are `private`. Reaching call site 1 through `doWork()` needs seeded SMS provider rows, at least two batches, and a recording transport, along the lines of `BackupWorkerIntegrationTest.buildWorkerWithTransport` (`BackupWorkerIntegrationTest.kt:86`). `doWork()` also calls `setForeground` first (`BackupWorker.kt:123`), so a pre-cancelled job may stop there for a different reason. A test of call site 1 that discriminates probably needs the Solution Architect to open a seam (as U-053 did for `appendBatchAndUpdateWatermark`). That is a design choice for the fix, not decided here.
- **WorkManager's real stop path.** `cancelUniqueWork` delivers a stop to the worker through WorkManager's own machinery. The existing tests use `TestListenableWorkerBuilder`, where it is not shown that the stop reaches the worker's job.

## Expected Behavior

When the work is cancelled, each loop stops at its next check: no further batch (backup) or item (restore) is started after the check sees the cancellation. This is what the code comments, the test Javadoc and the design documents already say it does (see Supporting Documentation).

## Actual Behavior

The three checks never throw. The loops continue past them. What actually ends the work after a cancellation is whatever other suspension point the loop reaches:

- **Backup loop** (`BackupWorker.kt:330-367`). Per pass: `converter.convertMessages` (blocking), `appendBatchAndUpdateWatermark` (a plain non-suspend function, `:404`, doing blocking IMAP calls), `calendarSyncer.syncCalendar` (blocking), then `setProgress` (`:361`), which suspends.
- **Restore loop** (`RestoreWorker.kt:310-330`). Per pass: `importMessage` (fetch from IMAP and insert), then `setProgress` (`:321`).
- **Restore test-facing loop** (`:398-415`). Per pass: `insertSmsValues`, then `setProgress` (`:406`).
- There is no `withContext`, no `runInterruptible` and no other `ensureActive` in the two worker files beyond these three. The only suspension point in a pass is `setProgress`.

So after a cancellation the loop finishes the pass it is in (it must, whatever the fix, because the IMAP calls are blocking and cannot be interrupted) and then relies on `setProgress` to notice. Design DES-MODERNIZATION-014 states this and bounds it: "Cancellation still lands at the next real suspension point" (`:908`). In effect: at most one more batch or item beyond the one in flight, to be confirmed by the fix's test (see Severity). Not verified: whether `setProgress` always throws promptly on a cancelled job. The library's await on an already-completed future may return without checking the job. If so, the bound would not hold and the loop would run on. The sources are not in the repository (see Evidence, "Not verified").

User-facing path, as read from the code:

1. The user presses the sync or restore button while a run is active. `StatusPreference.java:279-288` (backup) and `:305-313` (restore) set the button to "canceling" and emit `SyncEvent.Cancel(USER)`.
2. `WorkManagerCancelCollector.collect` (`app/src/main/java/com/zegoggles/smssync/service/WorkManagerCancelCollector.kt:43-57`), started from `MainViewModel.kt:324` (backup) and `:332` (restore), takes the first Cancel event and calls `WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)` (`:52`).
3. WorkManager is expected to stop the running worker and cancel its coroutine job. That last step is in the WorkManager library and was not read (assumed).
4. The collector's own comment names the intended mechanism: "The worker's cooperative ensureActive() handles the interrupt at the next suspension point" (`:23`).

## Evidence

All paths are workspace-relative.

| Evidence | What it shows |
|----------|---------------|
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt:54`, `:57`, `:116`, `:305`, `:330-331` | Imports of `ensureActive` and the top-level `coroutineContext`; class extends `CoroutineWorker`; call is in a private suspend member, directly in the loop, no `withContext` |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt:63`, `:66`, `:136`, `:289`, `:310-311`, `:378`, `:398-399` | Same two imports; same class shape; two call sites, both directly in loops of suspend members |
| `sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/final-gates.log:108-110` | The three warnings, one per call site, naming `val coroutineContext: CoroutineDispatcher` |
| `sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/baseline-gates.log:88-90`, `qa/qa-gates-run1.log:164-166`, and 11 more saved logs from the same story | Same three warnings before U-060 and in every later run: not caused by U-060 |
| `sdlc/artifacts/build/sprints/sprint-017/U-060/implementation-log.md:242-246` | U-060 recorded the new JDK 25 and Kotlin 2.3 warnings as debt (section 1e) |
| `app/build.gradle:155`, `:118`, `:117` | WorkManager `work-runtime-ktx:2.9.1`, kotlinx-coroutines `1.7.3`, kotlin-stdlib `2.3.21` |
| `app/build.gradle:65-67` | `warningsAsErrors true` is inside the `lint { }` block only; `app/build.gradle:203` `-Werror` applies to javac only; no `allWarningsAsErrors` for Kotlin anywhere in the Gradle files (searched `*.gradle`, `*.kts`, `*.properties`). Kotlin warnings do not fail the build today |
| `sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-...md:166` (A7), `:903-917` (F-1 and decisions), `:1558` (risk 10) | The finding, its bound, the "not fixed here" decision, and the stop rule for the WorkManager increment |

**Existing tests (searched `app/src/test/**/*Worker*` for cancel, stop, `isStopped`, `ensureActive`):**

- `BackupWorkerTest.kt:27-31` (Javadoc) says the loop's `ensureActive()` calls "unwind the coroutine at the next suspension point". That claim is untrue, per this bug. The only test code on cancellation is `ic2_workManager_cancelsBackupWorkerRequest` (`:223-236`), which enqueues, calls `cancelUniqueWork` and asserts `WorkInfo.State.CANCELLED`. It does not run the loop.
- `RestoreWorkerTest.kt:235-247` is the same for restore.
- `RestoreWorkerCheckpointTest.kt:369` (`ac7_checkpointRetainedOnInterruption_notClearedOnCrash`) interrupts with a simulated crash, not a cancellation.
- No test mentions `isStopped`, `onStopped`, `ensureActive` in code, or cancels a job running a loop. The search found `ensureActive` only in comments and Javadoc.
- No `androidTest` source mentions cancel. U-015 named an "AC-5 cancellation test" among instrumented tests (`sdlc/artifacts/build/sprints/sprint-001/U-015-coroutineworker-backup-restore-rewrite.md:128`). None exists in the repository today.

**Verified from the repository, and what was not:**

- Verified: the three sites, the imports, the enclosing functions, no `withContext` in the workers, the warning text, WorkManager 2.9.1, coroutines 1.7.3.
- Not verified (library sources are not in the repository, and no Gradle run was allowed): that the member `CoroutineWorker.coroutineContext` in 2.9.1 is typed `CoroutineDispatcher` with default `Dispatchers.Default`. The warning text confirms the type and the deprecation. The default and the fact that a dispatcher contains no `Job` are standard library behaviour, taken as assumed. That `kotlinx.coroutines`' `CoroutineContext.ensureActive()` returns silently when the context has no `Job` is also assumed (the design records it too, `:166`). That `setProgress` throws promptly for a cancelled job in production, or under the test harness, is not verified. A compile-time check would show the first point (the warning, which exists). A unit test run on the unfixed code, as in Steps to Reproduce B, would show the rest.

## Severity and Priority

**Severity: low (confirmed).** Reasons:

- The bound. Every pass of every loop calls `setProgress`, a suspension point (`BackupWorker.kt:361`, `RestoreWorker.kt:321`, `:406`). The IMAP calls inside a pass are blocking and uninterruptible either way, so even a working check cannot stop a pass already running. The practical difference a fix makes is, at most, one more batch (backup) or one more item (restore) after the cancellation. The batch size was not measured here. This bound depends on `setProgress` taking effect on the cancelled job, which is not verified (see Evidence).
- Can an append reach the mail server after the user cancels? Yes, and also after a fix: the batch already in flight completes because the calls are blocking. Without the fix, one further batch can be converted, appended and have its watermark advanced. That is not data loss. The watermark moves only after a confirmed append (BUG-010 gating, `BackupWorker.kt:292-300`, `:404-420`), so the next run starts after what was sent. For restore, the extra item is inserted behind the existing duplicate guards and its checkpoint is written (`RestoreWorker.kt:361-366`).
- Not a fault the owner can see. No crash, no lost data, no duplicate. The visible difference is that the work may take one pass longer to stop than the "canceling" button implies.
- The app is private and unpublished, for its owner's own use (project notes).

**Priority: medium.** Not for the harm, which is small, but for timing:

1. The design's stop rule for the WorkManager increment uses the deprecation warning at these three lines as its tripwire (`DES-MODERNIZATION-014...:915-917`). A warning is a weak sentinel: the fix itself removes it. A test that fails when the checks stop observing the real job is a better one, and it needs this bug fixed first.
2. Whether the bug can become a build failure after an upgrade is not known. As far as the repository shows, a deprecation warning does not fail any Kotlin build here (see Evidence). Two cases are open, both not verified: (a) if a later Kotlin, coroutines or WorkManager release raises the member's deprecation to an error, the three lines stop compiling, and the bug becomes a build failure inside the runtime-library story; (b) if a later WorkManager removes the member, the unqualified name falls back to the imported top-level `coroutineContext`, the code compiles, and the checks begin to work with no code change. In that case cancellation behaviour shifts silently inside an upgrade that must show no regression. The design's stop rule exists for exactly that (`:915-917`). It names WorkManager 2.12.0; its D10 text as read today says only that coroutines 1.11.0 deprecates the dispatcher as a context key and says nothing about Kotlin 2.4 (see SC-4).

## What the upcoming upgrades do to these lines

As far as the cited sources show (the design's D10 table, `DES-MODERNIZATION-014...:899`, `:901`, and the release notes it cites, which were not re-read for this bug):

| Increment | What is stated | Effect on these lines |
|-----------|----------------|-----------------------|
| C: coroutines 1.7.3 to 1.11.0 | 1.11.0 "deprecated `CoroutineDispatcher` as a context key" | Not verified. The deprecation concerns the dispatcher used as a key, which is not what is done here. Whether the member property or the `ensureActive()` call gets a new or changed warning is not stated |
| W: WorkManager 2.9.1 to 2.12.0 | 2.10.0 adds `Configuration.workerCoroutineContext`; nothing is stated about the `CoroutineWorker.coroutineContext` member | Not verified: whether the member stays, stays deprecated, is raised to an error, or is removed. The design's stop rule: if the warning at the three lines disappears or changes, stop and report (`:915-917`) |
| Kotlin 2.4 (and 2.3.x) | Not found in D10 as read | Not verified |

## Acceptance Criteria

The criteria say what must be true after the fix. They do not say how to write it.

- [ ] AC-1: Each of the three cancellation checks observes the job of the work that is actually running. After the fix, cancelling that job makes the check throw, in the backup loop and in both restore loops.
- [ ] AC-2: A unit test fails on the unfixed code and passes on the fixed code, for each of the three call sites, or, where a call site cannot be reached at unit level, the story records why and what covers it instead. For the restore sites, the test cancels before the first pass and asserts that no item is inserted and no checkpoint is written (Steps to Reproduce B). It asserts on those effects, not on a thrown `CancellationException` (see SC-1).
- [ ] AC-3: The test of AC-2 also fails if a check is removed or is mis-bound again, shown by a recorded mutation for each site (delete the check; restore the old binding). A test that passes in either state does not satisfy this criterion.
- [ ] AC-4: No compiler deprecation warning remains on the three lines. After the change, compile the app module and search the Kotlin warnings for `coroutineContext`: none.
- [ ] AC-5: Cancellation is at least as prompt as today. In every position the loop could be in when cancelled, it stops no later than before the fix: the number of batches (backup) or items (restore) started after the cancellation is never greater than before. It can be lower, by up to one.
- [ ] AC-6: Nothing else in the two workers changes: no change to what is appended, converted, inserted, recorded as watermark or checkpoint, returned as a result, or caught, other than the three checks. (Comment and Javadoc corrections are a separate decision: SC-2.)
- [ ] AC-7: Existing worker behaviour is intact: the existing tests of `BackupWorkerTest`, `BackupWorkerIntegrationTest`, `BackupWorkerWatermarkTest`, `RestoreWorkerTest` and `RestoreWorkerCheckpointTest` pass with no edit to their assertions. `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` exits 0 with 0 failures, and the skipped count is not higher than before the change (2 at the design's HEAD).

### Integration Criteria (if fix modifies component boundaries)
- [ ] IC-1: If a seam is opened so that `backupCursors` or `runImapRestoreLoop` can be tested (for example by changing visibility, as `appendBatchAndUpdateWatermark` was in U-053), every caller and test call site is updated in the same change, and no production signature seen by WorkManager or Hilt (`BackupWorker.kt:106-116`, `RestoreWorker.kt` constructor) changes.
- [ ] IC-2: The user-facing cancel path (`StatusPreference.java:279-288`, `:305-313`; `WorkManagerCancelCollector.kt:43-57`; `MainViewModel.kt:324`, `:332`) is unchanged.

## Impact Analysis

Searches run: `ensureActive` (52 occurrences in 25 files across `*.md`, `*.kt`, `*.java`), `coroutineContext`, `isStopped`, `onStopped`, `withContext`, `runInterruptible`, `cancelUniqueWork`, `SyncEvent.Cancel`, `cancel`/`stop` in the named contracts and requirements. Layers follow the set used in BUG-020; the project's configured layer list was not re-read.

| # | Layer | Affected? | Files and lines | Action needed |
|---|-------|-----------|-----------------|---------------|
| 1 | Requirements (`sdlc/artifacts/requirements`) | No change required | `modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md` and `REQ-MODERNIZATION-013-...md`: a case-insensitive search for `cancel` and `stop` finds nothing in either. `REQ-MODERNIZATION-016-...md:99` (AC-8) requires the scheduler-contract tests to pass unmodified or be listed | None. No requirement states the stop rule in words, so none is violated. The rule is in the contract and the design (below). REQ-016 AC-8 is the reason timing matters |
| 2 | Design and contracts (`sdlc/artifacts/design`) | Wording only | `contracts/CNTR-MODERNIZATION-004-backup-scheduler.md:273` (rule 7): "`cancel` is structured. It MUST cooperatively cancel the running coroutine (unwinding at the next suspension point) and clear any scheduled retry — not merely poll an `isCancelled()` flag." The next suspension point is still honoured today via `setProgress`, so the rule is met, but not by the mechanism the build records claim. `CNTR-MODERNIZATION-006-...md:298` (rule 7) concerns `Cancel.Origin`, not the worker. `modernization/DES-MODERNIZATION-005-...md:215-218` ("cancellation cooperatively cancels the coroutine, which unwinds the suspending loop at the next suspension point") and `:450`. `modernization/DES-MODERNIZATION-014-...md:70-74`, `:166`, `:260-263`, `:903-917`, `:1519-1521`, `:1558` | None required. DES-014 F-1 and its stop rule need a pointer to this bug and, after the fix, a revision of the stop rule (see Timing) |
| 3 | Stories, sprints, change records | Yes (references) | `sdlc/artifacts/build/sprints/sprint-001/U-015/implementation-log.md:44`, `:62`, `:113` and `review-code.md:26` record the checks as working (history; leave as is). `U-016`, `U-031`, `U-042`, `U-050`, `U-053` records mention `ensureActive` in passing. `sdlc/artifacts/stories/BUG-018-...md` mentions it once | Link from the fix story. No rework of finished stories |
| 4 | Application code (`app/src/main`) | Yes (root) | The three call sites above. Comments that describe the checks as working: `BackupWorker.kt:72`, `:286`, `:329`; `RestoreWorker.kt:84`; `WorkManagerCancelCollector.kt:23` | The fix. Comment wording: SC-2 |
| 5 | Vendored library (`k9mail-vendored`) | No | No match | None |
| 6 | Tests (`app/src/test`) | Yes | New tests per AC-2 and AC-3. `service/BackupWorkerTest.kt:27-31` (Javadoc claims the checks unwind the coroutine: wrong today); `:223-236`; `service/RestoreWorkerTest.kt:235-247`; `service/RestoreWorkerCheckpointTest.kt:104`, `:352`, `:375` (the seam) | Add tests; the old tests stay unedited (AC-7). Javadoc: SC-2 |
| 7 | Documentation (`README.md`, `sdlc/artifacts/engagement`) | Not checked in detail | One match in `sdlc/analysis/20260623-post-migration-assessment/outputs/sms-backup-plus/architecture.md` (analysis output, historical). README and engagement artifacts were not searched for this bug | None expected |

**Cascade analysis** (root layer: application code; four layers touched: code, tests, design wording, build records).

- Root: application code. The contract and the design say what is wanted; the code does not do it. No upstream statement is wrong.
- Propagation: code to tests (a new test is required, since the old ones assert only WorkManager state); code to comments and Javadoc that overstate what the checks do (SC-2); code to the DES-014 stop rule (its tripwire changes once the warning is gone). No propagation to requirements, vendored code or the contract text.
- Fix order: (1) the code and its tests together; (2) the DES-014 stop rule restated in terms of the new tests (Solution Architect); (3) comment wording if in scope.
- Affected in-progress work: sprint-022 is the current sprint folder (`sdlc/artifacts/build/sprints/sprint-022/`, untracked at HEAD `b9fd172f`); its content was not read, so whether a story touches `BackupWorker.kt` or `RestoreWorker.kt` is not known. The runtime-library story is not started.

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->

Advisory, from the Solution Architect's intake pass on 2026-11-21. Library facts below were read from the compiled classes of the named library versions (read-only, no Gradle run). Story planning may revise all of it.

**What the library classes settle.**

- WorkManager 2.9.1, `CoroutineWorker`: the member `coroutineContext` is a `CoroutineDispatcher` field set to `Dispatchers.Default`, marked `@Deprecated("use withContext(...) inside doWork() instead.")` at the default level (warning). The worker's real `Job` is a separate private field. `startWork()` launches `doWork()` in a scope made of dispatcher plus that job. `onStopped()` cancels the result future, and a listener on that future cancels the job.
- kotlinx-coroutines 1.7.3 (and 1.11.0): `CoroutineContext.ensureActive()` reads `this[Job]` and does nothing if there is none. A dispatcher holds no job, so the three checks are no-ops. This confirms the bug's two "assumed" items.
- `setProgress` in WorkManager 2.9.1 has a fast path. If the progress future is already complete, it returns that value **without checking cancellation**. It only checks cancellation when it actually suspends. The test helper `TestProgressUpdater` (work-testing 2.9.1) always returns a completed future. So **under the unit-test harness `setProgress` is never a cancellation point**. In production, `WorkProgressUpdater` completes the future on a task thread, so it usually suspends, but that is a race, not a guarantee. The bound in the bug's Severity section ("at most one more pass") therefore holds in production most of the time, not always.
- WorkManager 2.12.0 keeps the member, typed `CoroutineDispatcher`, with the same deprecation message at warning level. It now runs `doWork` on `WorkerParameters.workerContext`, and its `setProgress` uses the `androidx.concurrent` `await` in place of the 2.9.1 code. Whether that `await` also has a fast path was not checked. So the warning is not expected to change in increment W. The `setProgress` cancellation behaviour, which is today's only working stop point, may change there.
- Kotlin stdlib 2.3.21 and 2.4.21: the top-level `kotlin.coroutines.coroutineContext` has no `kotlin.Deprecated` annotation in either version. `currentCoroutineContext()` exists in coroutines 1.7.3 and 1.11.0 and is not deprecated in either.

**One correction to the bug's picture of the backup loop.** One pass of `backupCursors` handles **one message**, not a batch. `BackupCursors.next()` moves one row (`BackupCursors.java:73-86`), and `MessageConverter.convertMessages` turns that one row into at most one message (`MessageConverter.java:140-154`). So after a cancellation the backup overrun is one message, one IMAP APPEND and one watermark step. It is not one batch of one data type, which is how both the bug and DES-MODERNIZATION-014 D10 ("once per batch of one data type") describe it.

**Fix options.**

| Option | What it means | Catches (SC-1) | Comments (SC-2) | Contract CNTR-MODERNIZATION-004 rule 7 | Verdict |
|---|---|---|---|---|---|
| 1. Read the real context: `currentCoroutineContext().ensureActive()` | Reads the context of the running coroutine. The name cannot be captured by the worker's member, and kotlinx-coroutines provides it for exactly this kind of name clash | A working check throws `CancellationException`, which extends `IllegalStateException`. Restore: caught at `RestoreWorker.kt:348` and `:433` and returned as `Result.failure("illegal_state")`. Backup: passes the `BackupWorker.kt:265-279` catches (none matches) and is caught at `:168` as `Result.failure(<message>)`. `finally` cleanup runs in both. In production the result is dropped: the job is only cancelled after `onStopped()` has already cancelled the result future, so the later `future.set(...)` has no effect and the work stays CANCELLED. `setProgress` cancellations already go through these same catches today, so nothing new is swallowed. What remains is a misleading error-level log line | They become true. Only the `BackupWorkerTest.kt:27-31` Javadoc stays wrong, because it says the behaviour is verified "at the WorkManager level" | Met by the intended mechanism | **Recommended** |
| 1b. Same, fully qualified: `kotlin.coroutines.coroutineContext.ensureActive()` | Same behaviour | Same | Same | Same | Acceptable. But an "optimise imports" or a later edit can shorten it back to the bare name and bring the bug back silently. Option 1 cannot be shortened that way |
| 2. Poll the worker's `isStopped` | The flag is set in `stop()`, slightly before the job is cancelled | Depends on what the loop does when it sees the flag: `break` changes results; throwing reproduces option 1 | Comments and contract wording would need rewriting | Rule 7 says "not merely poll an `isCancelled()` flag". This is that pattern | Not advised. It is also hard to test: `ListenableWorker.stop(int)` is restricted to the library group (`@RestrictTo(LIBRARY_GROUP)`), and the tests call `doWork()` directly, so nothing sets the flag |
| 3. Delete the checks and rely on `setProgress` | Removes the warnings and the dead code | Unchanged | Five comments and the Javadoc must be rewritten to say "stops at `setProgress`" | Met only through a call with a fast path that skips the check (see above). It is never a stop point under tests, and its behaviour may change in WorkManager 2.12.0 | Not advised. It turns a race into the design |

**SC-1 (rethrowing `CancellationException`).** I agree with the bug: keep it out of this fix. Since the result is dropped in production, the harm is a misleading log line ("unrecoverable error", or "IllegalStateException (possible memory)"). Restore also skips `updateAllThreadsIfAnySmsRestored` on that path, which `setProgress` cancellations already do today. Adding `catch (e: CancellationException) { throw e }` ahead of each broad catch is a change to the catches (AC-6) and to every caller's error path. It deserves its own low-severity bug, scheduled after this one so its tests can reuse this story's cancellation tests.

**Testability (smallest seam per site).** The tests call `doWork()` or `executeRestoreWithValues` directly inside `runBlocking`. So a fixed check observes the test's own job, and since `setProgress` never throws under the harness, the check is the only stop point. That makes the tests sharp: unfixed code runs to completion (all rows inserted, checkpoint cleared, `Result.success`); fixed code stops.

- Site 3 (`RestoreWorker.kt:399`): no new seam needed. Better than the bug's "cancel before the first pass": cancel the test's job from inside the existing `RestoreInsertInterceptor.afterInsert(k)` hook. A fixed loop then stops with exactly k+1 rows and the checkpoint at k. A pre-cancel test alone would also pass if a single check sat before the loop.
- Site 2 (`RestoreWorker.kt:311`): reachable without a seam. `RestoreWorkerIntegrationTest` already builds a worker with a fake transport (`buildWorkerWithTransport`, `:381`; `ImapRestoringTransport`, `:482`) and registers a fake SMS provider (`:86`). Its `importMessageBody` lambda can cancel the job after item k. The bug's "only one restore site has a seam" undercounts.
- Site 1 (`BackupWorker.kt:331`): no existing test puts rows through `backupCursors`. `doWork_noData_returnsSuccess` only covers the empty provider. Smallest seam: widen `backupCursors` to `internal` with `@VisibleForTesting(otherwise = PRIVATE)`, as U-053 did for `appendBatchAndUpdateWatermark`. Then drive it with a `BackupCursors` built from in-memory cursors (constructor and `add` are package-private, in the same package as the tests) and a transport whose `appendMessages` cancels after k messages. A `MessageConverter` must also be supplied: its testing constructor is package-private in `mail`, so the planner should check whether `WorkerEngineFactory` or a test double is the easier route. The alternative without a seam is a fake provider that answers `BackupQueryBuilder`'s queries. It needs no production change, but it ties the test to the query shape and costs more. This is IC-1's case and changes no WorkManager or Hilt signature.

**Timing.** I agree with option A and with the orchestrator's placement, with one correction to the bug: the fix must land **before S2, not merely before S4**. The design as it reads today puts the F-1 stop rule on three increments: S2-I5 (Kotlin 2.4.21), S4-C and S4-W (D10, and the S2 table row S2-I5). The bug's SC-4 ("D10 says nothing about Kotlin 2.4") is out of date. Scheduling the fix with S1 or directly after it meets this. I would make it a separate story directly after S1 (or before it; the two are independent) rather than an increment inside S1, because S1's proof rests on "manifest and minSdk only" and a behaviour change would blur it. If it does go inside S1, keep it as its own increment and its own commit.

- Nothing in the fix depends on a version that S2 or S4 changes. `currentCoroutineContext()` and `ensureActive()` behave the same in coroutines 1.7.3 and 1.11.0, and the fix no longer reads the WorkManager member at all.
- Restated stop rule (for the design owner, D10): "In S2-I5, S4-C and S4-W, the cancellation tests added by BUG-022 pass unmodified, and the Kotlin compiler prints no warning naming `coroutineContext` in `BackupWorker.kt` or `RestoreWorker.kt`." The first part checks the behaviour. The second catches the old binding coming back. Line numbers should not be part of the rule, since they will move.
- A small courtesy for the plan: change the import line in place, from `kotlin.coroutines.coroutineContext` to `kotlinx.coroutines.currentCoroutineContext`. Then no line numbers shift in the two files, and the line citations in DES-MODERNIZATION-014 and in sprint-022's U-066 plan (which cites `BackupWorker.kt` log lines) stay valid.

**Shape and planning risks.**

- One story: one defect, three sites, one mechanism. Include the `BackupWorkerTest.kt:27-31` Javadoc correction, the only comment that stays false after the fix. Leave the other SC-2 comments alone: the fix makes them true. Leave SC-1 to its own bug, and SC-3 out of scope.
- SC-5 (blocking IMAP work on the default dispatcher) is context, not scope; I agree. It explains why no check can stop a pass already in flight. It is also why WorkManager 2.12.0's `workerCoroutineContext` is a separate design topic.
- Risk: the fix changes the result of cancelled tests from success to `Result.failure(...)`. No existing test cancels a running loop, so AC-7 is not threatened. New tests should assert on rows, checkpoint and transport calls, as the bug says, not on the result type, so that the later SC-1 bug does not have to edit them.
- Risk: a mutation that removes a check (AC-3) must actually make a test fail. Because `setProgress` never throws under the harness, removing a check does make the tests fail. This harness property is what AC-3 relies on, and the plan should record it. Re-check it in increment W, where work-testing 2.12.0 may complete progress futures differently (not checked).
- Not checked here: WorkManager's `WorkerWrapper` handling of a stopped worker beyond `CoroutineWorker` itself, and the 2.12.0 `await` fast path. Neither changes the recommendation.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` (`:331`) | Check bound to the deprecated dispatcher property; never throws | Check observes the running job (AC-1) |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt` (`:311`, `:399`) | Same | Same |
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` (`:57`, imports) and `RestoreWorker.kt` (`:66`) | Import of the top-level `coroutineContext` that the calls were meant to use | Whatever the fix needs; the Solution Architect decides |
| `app/src/test/java/com/zegoggles/smssync/service/` (new test file or additions) | No test stops a loop by cancelling its job | Tests per AC-2 and AC-3 |
| `app/src/test/java/com/zegoggles/smssync/service/BackupWorkerTest.kt` (`:27-31`) | Javadoc says the loop unwinds on cancel | SC-2 |

## Existing Behavior to Preserve

- Watermark gating: `setMaxSyncedDate` only after a confirmed append (BUG-010; `BackupWorker.kt:292-300`, `:404-420`).
- Restore write ordering: insert, then checkpoint, then interceptor; checkpoint retained, not cleared, when the run does not succeed (`RestoreWorker.kt:361-366`; `RestoreWorkerCheckpointTest.kt:369`).
- Results and retry rules in `doWork` and the helper functions: `Result.retry()` on `MailException`, `Result.failure` with the same reasons, the 300-second backoff cap (`BackupWorker.kt:125-137`, `RestoreWorker.kt:150-158`).
- `setProgress` emitted once per pass with the same keys (`BackupWorker.kt:361-366`, `RestoreWorker.kt:321-325`, `:406-410`).
- The cleanup in `finally` blocks: `transport.closeFolders()` (`BackupWorker.kt:377`, `RestoreWorker.kt:277`) and `cursors?.close()` (`BackupWorker.kt:280`), which must still run when a check throws.
- The cancel path from the button to `cancelUniqueWork` (IC-2), and the WorkInfo state reaching CANCELLED.
- A run that is never cancelled behaves as today in every respect.

## Verification Steps

For QA, per acceptance criterion. Run Gradle in a shell without `MSYS_NO_PATHCONV`.

1. AC-1 and AC-2: run the new tests on the fixed code and confirm they pass. Check out the unfixed worker files (keep the new tests) and confirm each test fails. Record both runs.
2. AC-3: for each call site, delete the check, run the tests, confirm a named test fails; restore the old binding (the dispatcher property), run again, confirm a named test fails. Record the results beside the story's evidence.
3. AC-4: compile the app module and search the output for `coroutineContext`: zero Kotlin warnings. Compare with `sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/final-gates.log:108-110`.
4. AC-5: compare the new tests and the old behaviour: a cancellation raised at each point of a pass (before conversion or fetch, after the append or insert, during `setProgress`) leaves no more started batches or items than before.
5. AC-6: `git diff` of the two worker files shows only the three checks (and imports they need).
6. AC-7: run `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`; exit 0, 0 failures, skipped not higher. Authoritative test count: `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`; explain any change by the new tests.
7. IC-1 and IC-2: `git diff` shows no constructor or injection change; the cancel path files are not in the diff.
8. Device check (optional, owner's call): start a large backup, press the button to cancel, read the log for the loop's last pass. Not required by the criteria.

## Root Cause Analysis
<!-- Filled during planning phase -->

Preliminary, from reading the code (to be confirmed by the Solution Architect).

- **Mechanism.** Kotlin resolves an unqualified name to a member of the implicit receiver before it considers an imported top-level declaration of the same name. Inside a class that extends `CoroutineWorker`, `coroutineContext` therefore means the inherited member, not the import. The compiler warning is the only sign, and nothing treated the warning as a defect.
- **Why it was not caught.** The code review of U-015 passed the checks on reading (`review-code.md:26`). The test Javadoc states the behaviour as verified at WorkManager level (`BackupWorkerTest.kt:27-31`) while the tests assert only WorkManager state. The warnings were recorded as debt in U-060 without anyone connecting them to the loop's purpose. The U-015 story named a cancellation test among instrumented tests (`U-015-coroutineworker-backup-restore-rewrite.md:128`); none exists.
- **Why it is masked.** Every pass ends in `setProgress`, a suspension point at which cancellation can take effect, so the app appears to stop when cancelled.

## Technical Context

- Build: Kotlin 2.3.21 (`app/build.gradle:2`, `:117`), WorkManager `work-runtime-ktx` 2.9.1 (`:155`), kotlinx-coroutines 1.7.3 (`:118`). Kotlin warnings do not fail the build (see Evidence).
- Threading: the loops run inside `doWork()`, on the worker's own dispatcher; the IMAP and content-provider calls in a pass are blocking and are not interruptible. So even a working check acts only between passes.
- Test set-up: Robolectric at API 29, `TestListenableWorkerBuilder`, the `WorkManagerTestInitHelper` test initialiser (`BackupWorkerIntegrationTest.kt:76-90`), and the fault-injection seam `RestoreInsertInterceptor` in `RestoreWorkerCheckpointTest`.
- Related design rules: runtime-library order R, A, P, C, L, W; coroutines before WorkManager (`DES-MODERNIZATION-014...:890-892`).

## Decision Point: when to fix, relative to the runtime-library story

The question: fix this bug as its own small story before the runtime-library story (S4), inside S4's coroutines or WorkManager increment, or after S4.

| Option | For | Against |
|--------|-----|---------|
| **A. Own small story, before S4 (recommended default)** | The behaviour change is isolated and attributable; S4 can then claim "no regression" without an exception for cancellation. The new tests replace the warning as the tripwire, so the stop rule in increment W (`DES-MODERNIZATION-014...:915-917`) can be restated as "the cancellation tests pass unmodified after each increment" (this needs the Solution Architect to revise D10). Works against the known WorkManager 2.9.1 and coroutines 1.7.3, so the story needs no knowledge of the later releases. Small and low risk | Delays S4 by one small story. The deprecation warning, the stop rule's current tripwire, disappears before S4 starts, so the design's stop rule must be restated first |
| B. Inside S4's coroutines (C) or WorkManager (W) increment | One fewer story | Mixes a deliberate behaviour change into an upgrade whose whole proof is "no regression"; REQ-016 AC-8 wants worker tests unmodified; if something moves, the cause is ambiguous. Same objection as BUG-020's "fix inside the move". The fix would also be written against the library that is changing under it |
| C. After S4 | S4 stays pure; the stop rule's warning tripwire remains useful for one more release | The tripwire only reports that the binding changed, not whether cancellation works. If a later release raises the deprecation to an error (not verified), this becomes a build failure inside S4 and is fixed under pressure, as in B. Cancellation behaviour may shift silently inside S4 if the member is removed |

**Recommended default: A.** Decided by the product owner on the advice of the Solution Architect. The recommendation rests on three facts from the repository: the bound is small (the loops stop anyway at `setProgress`), so there is no urgency to hurry it into S4; S4 is built on the claim that nothing changes in behaviour; and the design's own tripwire is the warning this bug removes. If the owner prefers C, the stop rule in W must stay and the S4 evidence must record the three warnings before and after each of C and W.

## Scope Concerns

Recorded for the Solution Architect and the orchestrator. None changes this bug's scope as written.

- **SC-1: the loops swallow `CancellationException`.** `CancellationException` extends `IllegalStateException`. `RestoreWorker.kt:272`, `:348` and `:433` catch `IllegalStateException` and return `Result.failure("illegal_state")`. `BackupWorker.kt:168` and `RestoreWorker.kt:198` catch `Exception` and return `Result.failure(...)`. So when a check starts to work, its cancellation is caught and turned into a failure result in the restore loops, as `setProgress`'s cancellation already is today. Whether WorkManager ignores the returned result of a cancelled worker is assumed, not verified. This bug does not change the catches (AC-6). A separate bug or the Solution Architect's decision is needed on whether a cancelled run should return a failure result at all. The test design in this bug avoids depending on it.
- **SC-2: comments and Javadoc say the checks work.** `BackupWorker.kt:72`, `:286`, `:329`; `RestoreWorker.kt:84`; `WorkManagerCancelCollector.kt:23`; `BackupWorkerTest.kt:27-31`. After the fix they become true. Before it they are wrong. Whether to touch them in the fix story is for the owner. AC-6 allows it only if decided.
- **SC-3: the pre-loop work is not cancellable.** Restore's fetch of message headers (`RestoreWorker.kt:241-252`) and `transport.checkSettings()` (`BackupWorker.kt:320`, `RestoreWorker.kt:234`) run before the loop with no cancellation point other than the `setProgress` calls beside them. A cancel during a long fetch is noticed late. Out of scope. Not measured.
- **SC-4: the brief and the design differ on Kotlin 2.4.** The intake text says D10 says Kotlin 2.4 or WorkManager 2.12.0 may change how the name resolves. D10 as it stands today (`DES-MODERNIZATION-014...:885-917`) names coroutines 1.11.0's deprecation and the WorkManager stop rule, and not Kotlin 2.4. I did not find a Kotlin 2.4 statement and so recorded it as not verified.
- **SC-5: blocking IMAP work runs on the default worker dispatcher.** The workers do blocking network and provider calls without `withContext` and without an I/O dispatcher. WorkManager 2.10.0 adds `Configuration.workerCoroutineContext` (design `:901`), which may bear on this. Not examined; not part of this bug.
- **SC-6: the backup loop has no test seam.** `backupCursors` is private. AC-2 allows recording that call site 1 cannot be shown at unit level, but a seam is the better way. This is a design choice with IC-1, left to the Solution Architect.
- **SC-7: U-015's cancellation test was never built.** The story listed an "AC-5 cancellation test" (`U-015-...md:128`). It does not exist, and the build records report AC-5 as met. This is a record discrepancy, not a code defect; the fix story's tests close the gap.

## Supporting Documentation

- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-toolchain-runtime-libraries-platform-level-design.md` (F-1 `:903-917`; A7 `:166`; D10 order `:890-892`; risk 10 `:1558`)
- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-016-runtime-libraries-latest-stable.md` (AC-8 `:99`)
- `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` (rule 7 `:273`; "Cancellation is terminal, not failure" `:281`)
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` (`:215-218`, `:450`)
- `sdlc/artifacts/build/sprints/sprint-001/U-015/review-code.md` (`:26`), `U-015/implementation-log.md` (`:44`, `:62`, `:113`)
- `sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/final-gates.log` (`:108-110`), `baseline-gates.log` (`:88-90`), `implementation-log.md` (section 1e)
- `sdlc/artifacts/stories/BUG-020-pinned-factory-no-tls-hardening.md` (model for shape and for the timing argument)

## Implementation Notes
<!-- Added by agents during build -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes

- Raised by the design DES-MODERNIZATION-014 (FD-VAL-006, MEDIUM there) and filed on 2026-11-21 at HEAD `b9fd172f`. This write-up rated it low in severity and medium in priority; both are for the product owner to confirm.
- Not run: Gradle, the tests described above, the device check. Every statement about library behaviour is marked verified, assumed or not verified in Evidence.
