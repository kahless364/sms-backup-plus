---
artifact_type: plan
story_id: "U-067"
gate_id: "planning"
checkpoint: "planning"
verdict: "PASS"
agent: "Solution Architect"
timestamp: "2026-11-21T09:58:20Z"
iteration: 1
base_commit: "4095db286d52dc4a361abc03de078edfce857b9c"
contracts_verified:
  - CNTR-MODERNIZATION-004
risks_identified: 9
---

# Implementation Plan: U-067 — Make the Workers' Three Cancellation Checks Observe the Running Job, and Prove Each With a Mid-Loop Cancellation Test

Story: `sdlc/artifacts/build/sprints/sprint-023/U-067-worker-cancellation-checks-use-the-running-coroutine.md`.
Source bug: `sdlc/artifacts/stories/BUG-022-worker-cancellation-checks-do-nothing.md`. Model for structure and tools:
`sdlc/artifacts/build/sprints/sprint-022/U-066/plan.md` (sections 1, 4, 5, 10).

**Starting state, verified at planning time.** HEAD was `4095db28` on `sdlc/modernization-plan`.
`git status --porcelain -- app k9mail-vendored` printed nothing, and the authoritative count at HEAD was **746**.

A1: at the amendment, HEAD is `0d5755c4`. It changed documents only since `4095db28`:
`git diff --stat 4095db28 0d5755c4 -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat`
prints nothing. Row D1's "descendant" clause therefore applies, and `START` is whatever HEAD is at I0.

The other work in the checkout is left alone:
- `sdlc/.skill-delegation-audit.log` and `sdlc/.skill-context.json`;
- everything under `sdlc/artifacts/build/sprints/sprint-024/`: the committed stories U-068 to U-070, and the U-068
  planning workspace, where a planner and its plan-gate reports write during this story's build.

The orchestrator may commit documents, never code, while the developer runs.

**Size.**
- Production: two files, six changed lines and one added line (edits E1 to E6).
- Tests: one new test file and three edited test files (T1 to T4). Seven tests are added, so the count goes
  from the start count to start + 7 (753 if the start is 746). No existing test is edited, renamed or deleted.
- Runs: six mutation runs and 12 Gradle invocations in all (A1, plan-review O-4).

**Binding rulings** (story Implementation Notes, Record 15 rows 17 and 18):
- DP-1 is ruled in: AC-9 applies.
- DP-2 is **Route S**: `backupCursors` becomes `internal` with `@VisibleForTesting(otherwise = PRIVATE)`. The
  "record why not tested" fallback is not available.
- Scope Concern 3 is recorded for the runtime-library story.
- The `Result.failure` follow-up is not part of this story.
- A1: P-1 is ruled (story Implementation Notes, "Ruling on the plan's scope concern P-1", 2026-11-21). The
  cursor-close half of AC-1 (c) is declared not observable through the seam, and QA judges it by reading the code
  path. The two corrections (two callers; Javadoc `:27-30`) are accepted.

## Amendment A1 (after the plan gates, 2026-11-21)

The plan gates of iteration 1 were:
- plan-review **PASS** (O-1 to O-7);
- contract-verification **PASS** (O-1 to O-4);
- expert-depth-check **FAIL**, at warning level only (criteria 1 to 4 "satisfied with a recommendation", plus an
  edge-case review).

The reports are `plan-review.md`, `contract-verification.md` and `expert-depth-check.md` in this workspace. No
rework iteration was required, and `iteration: 1` stays. This amendment takes up every finding. Each change is
made in place, where the developer reads it; the table only says where to look. The new library facts (L5b, L7,
L20) were read in this session with `javap` from the local Gradle cache, and the new source facts with `git grep`.

**No production edit, no test text and no mutation row changed.** What changed:
- the mutation **recovery** command (O-2);
- the I8 records check (O-1);
- rows D6 and D10;
- counts and text.

| Finding | Change, and where |
|---------|-------------------|
| plan-review O-1 | "Starting state" lists the current other work, sprint 024 among it. Constraint C12 is replaced: it now names `sdlc/artifacts/build/sprints/sprint-024/**` and says that document commits by the orchestrator may happen during the run. In I8 step 4, the records check is replaced by checks scoped to this story's paths (`app`, `k9mail-vendored`, the build files, `$W`), plus a "no code commit since `START`" check. A change outside those paths is recorded and is not a D15 stop. D15 says the same |
| plan-review O-2 | The I6 recovery note now restores with the checkout filters: `git cat-file --filters --path=<path> <blob> > <path>`. This writes the file with its working-tree (CRLF) endings, like the normal restore by `cp` from the pristine copy, so both paths give the same bytes and the same hash |
| plan-review O-3 | D10 now covers "I1 or I4" (an E2 hunk other than `@@ -305 +305,2 @@`). D6 now says "fixture and harness only; every assertion of 3.2 kept, as in D4" |
| plan-review O-4 | 12 Gradle invocations, in "Size" and in 4.0 |
| plan-review O-5 | AC-4: `NO_CHECKPOINT` is 0 (`RestoreCheckpointStore.kt:64`), so a checkpoint at index 0 reads the same as none. The **row count** is the discriminator. "No checkpoint is written" follows from it, because a checkpoint is written only after an insert (`RestoreWorker.kt:461-472`). Stated in 2.4, in the test table 3.3, and in the criterion map. The test text is unchanged |
| plan-review O-6 | P-1 and the two corrections are ruled: "Binding rulings", 2.3, section 7 items 1 and 2, the verdict. The developer cites the ruling in I9 |
| plan-review O-7 | The story's Affected Code row "`BackupWorkerWatermarkTest.kt` or a new test file" is stale; the plan takes the new-file branch (T1), and `BackupWorkerWatermarkTest.kt` gets no diff. Noted in section 7 item 2 for the orchestrator and the reviewers; no plan step changes |
| contract-verification O-1 | Section 7 item 4 (the follow-up bug) cites CNTR-MODERNIZATION-004 `:284` and `:287` by line. It states that WorkManager discarding the `Result` of an already-cancelled run is a library behaviour, not a contract guarantee. It is partly read here (L5b: `onStopped()` cancels the future), and it is not verified that the later `set(...)` is discarded by WorkManager's own worker wrapper |
| contract-verification O-2 | New section 7 item 7. CNTR-MODERNIZATION-004 (`:87`, `:131`, `:149`, `:280`) and CNTR-MODERNIZATION-007 cite `BackupWorker.kt` lines that were already stale, and E2 shifts the lines below `:305` by one more. No contract text becomes false. Recommend re-citing by name at the next contract amendment |
| contract-verification O-3, O-4 | Notes in section 8: rule 7's "next suspension point" is met in the stricter form "top of the next pass", with no conflict (O-3); `integration_contracts: []` is correct (O-4) |
| expert-depth C1 | New 2.0 "Principles applied": structured concurrency and cooperative cancellation (kotlinx.coroutines guide sections named); mutation testing (DeMillo, Lipton and Sayward 1978), with the classes of mutant; Test Spy and Test Stub (Meszaros ch. 23); Decorator by Kotlin delegation (GoF); the seam (Feathers ch. 4, already cited) |
| expert-depth C2 | 2.0 names the broad catches as the swallowed-`CancellationException` anti-pattern (kotlinx.coroutines guide; detekt `exceptions` rule set, `SwallowedException` and `TooGenericExceptionCaught`), kept on purpose (SC-1). It also names the shadowing hazard. The sixth comment `RestoreWorker.kt:369` is verified ("AC-7: checkpoint NOT cleared on CANCELLED (cooperative cancellation unwinds before clear)") and added to the untouched list in 3.1. 2.0 says why it stays untouched |
| expert-depth C3 | 2.1 table gains three rows: the import alias, `yield()` (both rejected, with reasons), and the gain and cost of widening `backupCursors`. 2.2 gains the `runTest` against `runBlocking` decision |
| expert-depth C4 | New ledger row L5b, from `javap -c` of `CoroutineWorker.startWork`, `onStopped` and the constructor's future listener (2.9.1). 2.2's claim now points at L5b. Who calls `onStopped` in production (WorkManager's worker wrapper) was not read and is marked so. The parent-child rule is cited (2.0). L7 now verifies `currentCoroutineContext` in 1.11.0 as well (present, not deprecated; the single deprecated member of that class is `isActive`), so the Portability row's "1.7.3 and 1.11.0" is now backed |
| expert-depth edge cases | New ledger row L20:<br>• no `NonCancellable`, `withContext`, `yield(`, `delay(` or `runCatching` in either worker (`git grep` exit 1);<br>• the two cleanup `finally` bodies (`BackupWorker.kt:280`, `:377`; also `RestoreWorker.kt:277`) are non-suspending Java calls, so they run on cancellation;<br>• the auth retry (`:462`) is entered only from `catch (e: XOAuth2FailedException)` (`:265-267`), so a cancellation cannot trigger it;<br>• in production the loop usually stops one step earlier, at the in-pass `setProgress`.<br>The last is also stated in 2.4. Section 8's risk table now has a likelihood and an impact for each risk, with grounds |

## 1. Assumption Ledger

`verified` means read or run in this planning session, at the location given. `needs_check` means a developer
step, with a decision row for each outcome (section 5). The library facts that the story and the bug list as
"not verified" are settled here. I read them with the JDK 25 `javap` from the compiled classes in the local
Gradle cache (work-runtime 2.9.1, work-testing 2.9.1, kotlinx-coroutines-core-jvm 1.7.3), extracted to my
session scratch folder. Nothing was written to the repository.

| # | Claim | Status | Evidence |
|---|-------|--------|----------|
| L1 | HEAD is `4095db286d52dc4a361abc03de078edfce857b9c`, `app` and `k9mail-vendored` are clean, and the count is 746 (CLAUDE.md command) | verified | `git rev-parse HEAD`; `git status --porcelain -- app k9mail-vendored` (empty); count command |
| L2 | The HEAD blob equals the working-tree hash for each file: `BackupWorker.kt` `5c554ec3bf737383f5a8717fdfc518e5eac33993`, `RestoreWorker.kt` `871ec7c764be5f9557fdc25ec0683cb835cfeac7`, `BackupWorkerTest.kt` `9890bb0e7c45be9c4e09ad595751ac39967bbc02`, `RestoreWorkerIntegrationTest.kt` `ae994499448b99f5cd65e453cc8e6b0325f98b7f`, `RestoreWorkerCheckpointTest.kt` `536db5b77288ddfc0bb966492afcddbae873760b`. The index is LF and the working tree CRLF (`git ls-files --eol`) | verified | `git rev-parse HEAD:<p>`, `git hash-object <p>` |
| L3 | Scope, verified by grep. `coroutineContext` appears in `app/src/main` exactly five times: `BackupWorker.kt:57` (import) and `:331` (call); `RestoreWorker.kt:66` (import), `:311` and `:399` (calls). No other use in either file, so both imports are unused once the calls change, and each is replaced in place. `kotlinx.coroutines.ensureActive` is imported at `BackupWorker.kt:54` and `RestoreWorker.kt:63`, and stays. The other `ensureActive()` hits are comments only: `BackupWorker.kt:72`, `:286`, `:329`; `RestoreWorker.kt:84`; `WorkManagerCancelCollector.kt:23` | verified | `git grep -n "coroutineContext" -- app/src/main`; `git grep -n "ensureActive" -- app/src/main` |
| L4 | `backupCursors` is `private suspend` at `BackupWorker.kt:305`. It has **two** production callers, both inside the class: `:255` (`fetchAndBackupItems`) and `:462` (`handleAuthError` retry). The story's IC-1 says "single production caller (`:255`)", which undercounts (section 7). No test calls it today | verified | `git grep -n "backupCursors(" -- app/src` |
| L5 | **Why the old check never throws (mechanism).** In work-runtime 2.9.1, `CoroutineWorker` declares `private final kotlinx.coroutines.CoroutineDispatcher coroutineContext`, a public open getter `getCoroutineContext(): CoroutineDispatcher`, and the deprecation-annotation holder `getCoroutineContext$annotations()`. Its real job is the separate field `private final CompletableJob job`. Inside a member function of a subclass, the unqualified name `coroutineContext` resolves to the inherited member of the implicit receiver `this`, ahead of the explicitly imported top-level `kotlin.coroutines.coroutineContext`. The compiler's own warning names that member: "'val coroutineContext: CoroutineDispatcher' is deprecated" (`sprint-017/U-060/evidence/final-gates.log:108-110`). In coroutines 1.7.3, `CoroutineContext.ensureActive()` compiles to `get(Job.Key)`, then returns if the result is null, otherwise calls `Job.ensureActive()`. A dispatcher is a single context element whose key is `ContinuationInterceptor`, so `dispatcher[Job]` is null and the call returns. All three checks are no-ops | verified | `javap -p` on `androidx.work.CoroutineWorker`; `javap -c` on `kotlinx.coroutines.JobKt__JobKt.ensureActive(CoroutineContext)` |
| L5b | **What WorkManager's stop does to the worker's job (A1, expert-depth C4).** In work-runtime 2.9.1, `CoroutineWorker.startWork()` builds `CoroutineScope(getCoroutineContext().plus(job))` and `launch`es the `startWork$1` body, which runs `doWork()`, in it. So `doWork` runs in a coroutine whose parent is the worker's `job`. `onStopped()` calls `future.cancel(false)`. The constructor adds a listener to `future` (`SettableFuture.addListener`), and the listener `_init_$lambda$0` calls `job.cancel()` when `future.isCancelled()`. So a stop cancels the `job` that is the parent of the coroutine running `doWork`. Cancelling the test's child coroutine that calls `doWork` (or the loop function) has the same effect on the code under test: the context `currentCoroutineContext()` reads holds a cancelled job. **Not read:** which WorkManager class calls `onStopped()` in production (the worker wrapper's stop path), and whether a `Result` set after that cancel is discarded. These affect only section 7 item 4, not the tests | verified (the `CoroutineWorker` part); not verified (the caller and the discard) | `javap -c -p androidx.work.CoroutineWorker`: `startWork`, `onStopped`, `<init>`, `_init_$lambda$0` |
| L6 | **The harness property the tests rely on.** `CoroutineWorker.setProgress` (2.9.1, inlined `await`) calls `isDone()` on the future, and if it is done returns `get()` without touching the job. Only the not-done branch builds a `CancellableContinuationImpl`. `TestProgressUpdater.updateProgress` (work-testing 2.9.1) returns a `SettableFuture` already `set(null)`. `TestForegroundUpdater` does the same, and `setForeground` has the same fast path. `TestListenableWorkerBuilder` builds both updaters. So under the harness neither call is a cancellation point. The loops' own checks are the only stop points, and on the unfixed code a mid-loop cancellation runs the loop to the end | verified | `javap -c` on `CoroutineWorker.setProgress` / `setForeground`, `TestProgressUpdater`, `TestForegroundUpdater`, `TestListenableWorkerBuilder` |
| L7 | `currentCoroutineContext()` exists in coroutines 1.7.3 as a top-level suspend function in package `kotlinx.coroutines` (class `CoroutineScopeKt`). Inside a suspend function it returns the context of the running coroutine. The worker's member cannot capture it, because the names differ. It carries no deprecation attribute: in 1.7.3 per `javap -v` (plan-review, "Mechanism claims"), and A1 in 1.11.0 per `javap -v`, where the only deprecated member of `CoroutineScopeKt` is `isActive`. In 1.11.0 `CoroutineContext.ensureActive()` compiles to the same `get(Job.Key)` and null check as in 1.7.3. The I7 compile still guards against a warning | verified (1.7.3 and 1.11.0) | `javap -p` and `javap -v` on `CoroutineScopeKt`; `javap -c` on `JobKt__JobKt` in both jars; row D20 |
| L8 | `BackupCursors()` and `add(DataType, Cursor)` are package-private (`BackupCursors.java:48`, `:51`). One pass of the loop is one row: `next()` calls `moveToNext()` or `moveToFirst()` once (`:73-86`). `hasNext()` is false at the last row (`:32-34`, `:69-71`). `BackupConfig`'s constructor is package-private and rejects a null transport and empty types (`BackupConfig.java:26-35`). `backupCursors` does not read `groupToBackup` or `maxItemsPerSync` | verified | files read |
| L9 | `cursors.close()` is called in `fetchAndBackupItems`'s `finally` (`BackupWorker.kt:279-281`), **not** in `backupCursors`. So Route S cannot observe the cursor-closing half of AC-1 (c). This is a declared narrowing (section 2.3, Scope Concern P-1). `closeFolders()` is in `backupCursors`'s own `finally` (`:375-378`) and is asserted | verified | file read |
| L10 | `MessageConverter` is a public, non-final Java class. `convertMessages(Cursor, DataType)` is public (`MessageConverter.java:140`). It is already mocked under Robolectric in `K9MailTransportPortMethodsTest.java:167` (and four more sites), with `mockito-core:5.18.0` (`app/build.gradle:163`; inline mock maker by default in Mockito 5). The new test therefore mocks the converter and does not depend on which cursor columns the real converter needs (the story's DP-2 "not verified" item) | verified | `git grep` on `mock(MessageConverter.class)` |
| L11 | `ConversionResult.add` sets `maxDate` from the message's `X-smssync-date` header through `Headers.get(msg, DATE)`, which returns `getHeader(...)[0]` (`ConversionResult.java:19-33`, `Headers.java:30-37`). `StubMailTransport.appendMessages` runs `appendMessagesBehavior` first and then, with no other option set, returns `result.getMaxDate()` (`BackupWorkerWatermarkTest.kt:369-374`). `appendBatchAndUpdateWatermark` writes exactly that value (`BackupWorker.kt:416-422`). The mock `ImapMessage` header pattern is the one at `BackupWorkerWatermarkTest.kt:328-334` | verified | files read |
| L12 | An `internal` function whose parameter type is the package-private Java `CalendarSyncer` compiles. The same exposure, of the return type, already compiles at `WorkerEngineFactory.kt:118`; parameters follow the same exposure rule | verified by analogy; needs_check | I3 compiles E2; row D3 |
| L13 | The restore fixture: SMS and call-log restore are both on by default (`DataType.java:95`, `:97`; `DataTypePreferences.java:37-40`), so `getMessages` is called twice, SMS first (`RestoreWorker.kt:241-252`). `maxItemsPerRestore` defaults to -1 in code (`Defaults.java:24`) or 500 from the XML default (`defaults.xml:6`); either way `itemsToRestoreCount` equals the handle count, 4 (`RestoreWorker.kt:254-255`). With no `KEY_UNIQUE_WORK_NAME` the checkpoint key is `RESTORE_WORK_NAME` = `"RESTORE"` (`:229`, `:739`). `RestoreWorkerIntegrationTest.setUp` registers `FakeSmsContentProvider` for `sms`, initialises the test WorkManager and sets the IMAP user (`:69-99`). `buildWorkerWithTransport` passes the class's `checkpointStore` field (`:381-410`). `ImapRestoringTransport` is a file-private final class whose `closeFolders` does nothing and is not counted (`:482-518`). It is counted by Kotlin interface delegation, so the helper is not edited | verified | files read |
| L14 | `RestoreInsertInterceptor.afterInsert` is `suspend` and runs after the insert and after `checkpointStore.write` (`RestoreInsertInterceptor.kt:39`; `RestoreWorker.kt:461-475`). `InMemoryCheckpointStore` never suspends (`RestoreWorkerCheckpointTest.kt:487-500`), so neither it nor the interceptor is a cancellation point | verified | files read |
| L15 | Catches. `kotlinx.coroutines.CancellationException` is `java.util.concurrent.CancellationException`, which extends `IllegalStateException`. So a thrown check is caught at `RestoreWorker.kt:348` and `:433` and returns `Result.failure("illegal_state")`. In the backup path it passes `BackupWorker.kt:265-278` and leaves `backupCursors`. In the backup test it ends the test's child coroutine as cancelled; in production `doWork` (`:168`) catches it | verified | JDK API; files read |
| L16 | Toolchain: Gradle 9.5.1, which supports the built-in `--rerun` task option; "Daemon JVM: Compatible with Java 25 … (from gradle/gradle-daemon-jvm.properties)" | verified | `./gradlew --version` (the only Gradle call made in planning) |
| L17 | JUnit XML is off in the build. The read-only init script `sprint-020/U-063/evidence/tools/junit-xml.init.gradle` turns it on and prints `U063-DAEMON-JVM: java.version=…`. `sanitize.py <in> <out> [--grep RE]` replaces repository, home, user and host and refuses e-mail-like tokens. `junit_totals.py <dir>` and `junit_cases.py <dir> [filters]` exist. `compiled-pipeline.json` of this workspace is excluded locally (`.git/info/exclude:40`) | verified | tool headers read; `git check-ignore -v` |
| L18 | Test classes at HEAD (`@Test` lines): `BackupWorkerTest` 9, `BackupWorkerIntegrationTest` 8, `BackupWorkerWatermarkTest` 6, `RestoreWorkerTest` 13, `RestoreWorkerIntegrationTest` 9, `RestoreWorkerCheckpointTest` 15, which is 60, none `@Ignore`d. After this story the seven classes hold 67 | verified | `grep -c` |
| L19 | Names used by the new code are free in the package: `git grep` finds `BackupWorkerLoopCancellationTest`, `CancellableImapRun`, `CancellingInterceptor` and `BackupRun` nowhere under `app/` (Kotlin private top-level classes of one package share the JVM namespace, so this matters) | verified | `git grep -n` (the only hit for a similar name, `BackupWorkerCancellationTest`, is in the U-015 story document, which is why that name is not used) |
| L20 | **Edge cases (A1).** (a) Neither worker file contains `NonCancellable`, `withContext`, `yield(`, `delay(` or `runCatching`. So no other cancellation point exists on the loop paths, and nothing would let the unfixed code stop under the harness (D5). (b) The cleanup `finally` bodies are non-suspending Java calls: `cursors?.close()` (`BackupWorker.kt:280`), `transport.closeFolders()` (`BackupWorker.kt:377`, `RestoreWorker.kt:277`). A suspending call in a `finally` of a cancelled coroutine would throw at once unless wrapped in `withContext(NonCancellable)`; these are not suspending calls, so they run. (c) The auth retry, the second caller of `backupCursors` (`:462`), is entered only from `catch (e: XOAuth2FailedException)` (`:265-267`). A `CancellationException` is not one, so a cancellation never re-enters the loop; a cancel during the retry's own loop is stopped by the same check. (d) In production `setProgress` usually suspends (its future completes on a task thread; bug, Suggested Approach), so after a cancel the loop usually stops at the in-pass `setProgress` (`BackupWorker.kt:361`; `RestoreWorker.kt:321`, `:406`), one step before the check. That point comes after the pass's watermark or checkpoint write, so the persisted state is the one the tests assert either way | verified (a) to (c); (d) the code order is verified, and the production suspension is the bug's library reading | `git grep -n -E "NonCancellable\|withContext\|yield\(\|delay\(\|runCatching"` on both files (exit 1); files read |
| N1 | The cancellation harness behaves as designed (2.2): `launch(start = LAZY)`, the fixture cancelling the held job, `join()`, and `runBlocking` itself not failing | needs_check | I3 red run and I5 green run; rows D5 to D9 |
| N2 | `:app:compileDebugKotlin --rerun --no-build-cache` really invokes the compiler and prints every warning of the module, including the three warnings at HEAD | needs_check | I0 step 3; row D2 |

## 2. Design (decided)

### 2.0 Principles applied (A1, expert-depth C1 and C2)

The kotlinx.coroutines guide sections below are cited by their published titles. I did not re-read them in this
session; the library behaviour each one describes is read from bytecode in L5 to L7.

- **Cooperative cancellation.** The guide "Cancellation and timeouts", section "Cancellation is cooperative": a
  coroutine stops only at a check (`ensureActive`, `isActive`, `yield`) or at a cancellable suspension. The bug is
  a check that never observes the job (L5); the fix makes it observe the job.
- **Structured concurrency.** The guide "Coroutine exceptions handling", section "Cancellation and exceptions": a
  child cancelled with a `CancellationException` does not cancel its parent. The test harness (2.2) relies on
  this, cancelling a child job while `runBlocking` completes normally. WorkManager relies on the same rule:
  `doWork` runs in a child of the worker's `job` (L5b).
- **Mutation testing.** DeMillo, Lipton and Sayward, "Hints on Test Data Selection: Help for the Practicing
  Programmer", *IEEE Computer* 11(4), 1978. The six I6 entries are mutants of two classes:
  - (a) a *binding* mutant specific to this bug, which reverts to the shadowed name;
  - (b) a *statement-deletion* mutant.

  A surviving (b) would mean the tests cannot observe the check at all, which is risk R1.
- **Test doubles** (Meszaros, *xUnit Test Patterns*, ch. 23):
  - the counting transports (`countingTransport`, `cancellableImapRun`) and `CancellingInterceptor` are **Test
    Spies**, recording calls that the test asserts;
  - the mocked `MessageConverter` is a configurable **Test Stub**;
  - the `object : MailTransport by …` wrappers are **Decorators** (Gamma et al., *Design Patterns*, "Decorator")
    built with Kotlin interface delegation, so the existing helpers stay unedited.
- **The seam.** Feathers, *Working Effectively with Legacy Code*, ch. 4, "The Seam Model" (2.1).

**Anti-patterns named.**
- **Shadowing by an implicit-receiver member.** An inherited member beats an explicitly imported top-level
  declaration of the same name (L5). The deprecation warning was the only sign, and it was read as a deprecation
  notice rather than as a binding change.
- **Swallowed `CancellationException`** at the broad catches: `catch (e: Exception)` (`BackupWorker.kt:168`) and
  `catch (e: IllegalStateException)` (`RestoreWorker.kt:348`, `:433`). The kotlinx.coroutines guide, "Cancellation
  and timeouts", asks for `CancellationException` to be rethrown. Static-analysis catalogues list the pattern as
  detekt's `exceptions` rule set, `SwallowedException` and `TooGenericExceptionCaught`; it is also why `runCatching`
  is unsafe in coroutines.
  - It is **kept on purpose** in this story (SC-1; section 7 item 4).
  - It does not affect what the tests assert. The job state is set by `cancel()` whatever the catch returns, and
    the tests assert effects, not the `Result`.
- **Comments that overstate the checks.** Six comments (3.1) become true after the fix and stay untouched.

### 2.1 The fix, and why this form

At the three sites, `coroutineContext.ensureActive()` becomes `currentCoroutineContext().ensureActive()`, and in
each file the import line `import kotlin.coroutines.coroutineContext` is replaced, in place, by
`import kotlinx.coroutines.currentCoroutineContext`. The options considered:

| Option | Gained | Gives way | Decision |
|--------|--------|-----------|----------|
| `currentCoroutineContext().ensureActive()` (ruled; bug option 1) | Reads the running coroutine's context through a name that no member of `CoroutineWorker` can shadow. kotlinx-coroutines provides it for exactly this clash with a receiver's `coroutineContext`. It does not depend on the WorkManager member existing, being deprecated, or being removed in a later release | One more import name | **Taken** |
| Fully qualified `kotlin.coroutines.coroutineContext.ensureActive()` | Same behaviour | An "optimise imports" or a later edit can shorten it back to the bare name, and the bug returns with no warning once the member is gone | Not taken (bug, option 1b) |
| Poll `isStopped` | — | The pattern that CNTR-MODERNIZATION-004 rule 7 (`:279`) forbids ("not merely poll an `isCancelled()` flag"), and untestable because `stop()` is library-restricted | Not taken |
| A1: import alias (`import kotlin.coroutines.coroutineContext as runningContext`) | Same behaviour; no new API name | An alias is as easy to remove in an import clean-up as a qualifier is to shorten. It also hides why it exists, and the bare name would compile again through the member | Not taken |
| A1: `yield()` at the top of the pass | It checks cancellation | A dispatch on every pass. Under `runBlocking` it lets other coroutines interleave, which loses the determinism of 2.2. In production it adds a thread hop per message for no gain over a check | Not taken |

**Gain and cost of the Route S seam (A1, expert-depth C3).**
- **Gained:** a direct, deterministic test of site 1. There is no fake content provider, and no coupling to
  `BackupQueryBuilder`'s query shape (Route P).
- **Cost:** `backupCursors` is reachable from the whole app module, not only its class. Its two production callers
  (`:255`, `:462`) are in the class, and a third caller elsewhere in the module would compile.
- **Containment:** `@VisibleForTesting(otherwise = PRIVATE)`, which lint reports for a non-test caller outside the
  class, and Kotlin's module scope. This is the same cost U-053 accepted for `appendBatchAndUpdateWatermark` (R9).

The import is replaced rather than deleted and added. That keeps every line of `RestoreWorker.kt` in place, and
every line of `BackupWorker.kt` above `backupCursors` (AC-8 (c)).

**Route S seam (E2).** `private suspend fun backupCursors(` becomes two lines:
`@androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)` and
`internal suspend fun backupCursors(`. This is U-053's pattern (`BackupWorker.kt:403-404`): a fully qualified
annotation, so no import is added and AC-8 (b) holds. In Feathers' terms (*Working Effectively with Legacy Code*,
ch. 4, "The Seam Model") it is the cheapest enabling point that reaches the real loop.

The added line moves the call to `:332`, below `backupCursors`, which AC-8 (c) allows. The alternative, putting
the annotation on the signature line, would keep `:331`. It is not taken, because it departs from the U-053
pattern that DP-2 names, and because DES-MODERNIZATION-014's restated stop rule is free of line numbers
(`:1130-1132`).

Both callers (`:255`, `:462`) are members of the same class, so the `otherwise = PRIVATE` lint contract holds.

### 2.2 How a test cancels "the work's job" deterministically

The tests call `doWork()`, `backupCursors()` or `executeRestoreWithValues()` directly, not through
`startWork()`. So `currentCoroutineContext()` inside the worker is the context of the test coroutine that makes
the call. Cancelling that coroutine's job has the same effect on the code under test as WorkManager's stop. A1:
L5b shows that `startWork` runs `doWork` in a child of the worker's `job`, and that `onStopped()` cancels the
future, whose listener cancels `job`. Which class calls `onStopped()` in production was not read (L5b).

**Harness choice: `runBlocking`, not `runTest` (A1, expert-depth C3).** `runTest` (kotlinx-coroutines-test 1.7.3)
is on the test classpath (`app/build.gradle:150`) and is used in four test classes, for example
`MainViewModelTest.kt`. It is not taken, for three reasons:
- nothing here needs virtual time or a test dispatcher;
- its dispatch timeout and its leaked-coroutine checks would add failure modes unrelated to the check under test;
- the worker test classes the new tests sit beside use `runBlocking`, so one style per class is kept.

Form used for AC-1 to AC-3 and the AC-5 controls:
```kotlin
runBlocking<Unit> {
    lateinit var workJob: Job
    // fixture captures { workJob.cancel() } and calls it from inside the blocking callback
    workJob = launch(start = CoroutineStart.LAZY) { /* call the worker */ }
    workJob.join()   // starts the child on runBlocking's single-thread event loop and waits
    ...assertions...
}
```
- **Why a child job and not `runBlocking`'s own.** A cancelled `runBlocking` coroutine rethrows at the end. A
  child cancelled with a `CancellationException` does not cancel its parent, and `join()` returns normally.
- **Why `LAZY`.** The `lateinit` job must be assigned before the fixture can call it. With `LAZY` the body
  starts only at `join()`, after the assignment. With `UNDISPATCHED` the body would run inside `launch(...)`,
  before the assignment, and fail with `UninitializedPropertyAccessException`.
- **No time.** No `delay`, no sleep and no other thread; `runBlocking`'s event loop runs the whole child.
- **The tests' own cancelling code** names a job the test holds. It never touches the worker's `coroutineContext`
  property (Verification Step 2).

Form used for AC-4 (cancelled before the first pass). A `LAZY` job cancelled before it starts never runs its
body, which would make the test pass on the unfixed code for the wrong reason. So AC-4 uses
`launch(start = CoroutineStart.UNDISPATCHED) { currentCoroutineContext()[Job]!!.cancel(); worker.executeRestoreWithValues(...) }`.
That is the bug's Steps to Reproduce B. Nothing between the cancel and the first check suspends
(L14: `checkpointStore.read` does not suspend).

**What is asserted and what is not.**
- Asserted: calls on the transport and the converter, rows, checkpoint, watermark, `closeFolders` count, cursor
  position, and `job.isCancelled`. `isCancelled` is a fixture-sanity line that is true on fixed and unfixed code
  alike, placed first so a test never passes because the cancel was not delivered.
- Not asserted: the `Result` type of a cancelled run (bug SC-1). AC-1 (d) is checked in its own words, "not a
  `Result.Success` with `backedUp` equal to the row count", which holds whether the run returns nothing, a
  failure, or anything else.
- The controls do assert `Result.Success`, as AC-5 requires.

### 2.3 Declared narrowing (Route S consequence)

AC-1 (c) asks that "the cursors were closed" (`BackupWorker.kt:279-281`). That `close()` lies in
`fetchAndBackupItems`, outside the seam (L9). A Route S test therefore cannot observe it. The test asserts
`closeFolders` exactly once (the seam's own `finally`) and asserts nothing about cursor closing.

The cursor close is unchanged code in a Kotlin `finally`, which runs on any throwable, including
`CancellationException`. The test closes its own cursors in its last line, as test hygiene, not as evidence.
A1: **ruled** (story Implementation Notes, "Ruling on the plan's scope concern P-1", 2026-11-21). The cursor-close
half of AC-1 (c) is declared not observable through the seam, and QA judges it by reading the code path: the
`finally` at `BackupWorker.kt:279-281` is not edited, and its body is non-suspending (L20 (b)).

### 2.4 Observable behaviour (from the code; what each test sees)

| Test | Fixed code | Unfixed code (red run, and mutations (a) and (b)) |
|------|-----------|-----------------------------------------------------|
| AC-1 `backupCursors_cancelledAfterSecondAppend_stopsAtNextPass` (4 rows, cancel in the 2nd append) | converts 2, appends 2, cursor position 1, watermark `ROW_DATES[1]`, closeFolders 1, no completed Success | converts 4, appends 4, position 3, watermark `ROW_DATES[3]`, Success with backedUp 4. **First failing assertion:** `appends` expected 2, was 4 |
| AC-2 `doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass` (4 handles, cancel in the 2nd import) | imports 2, rows 2, checkpoint 1, closeFolders 1 | imports 4, rows 4, checkpoint cleared. **First failing:** `importCalls` expected 2, was 4 |
| AC-3 `executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass` (5 items, cancel in `afterInsert(2)`) | rows 3, interceptor saw `[0, 1, 2]`, checkpoint 2 | rows 5, checkpoint cleared. **First failing:** rows expected 3, was 5 |
| AC-4 `executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing` (5 items, self-cancel first) | rows 0, checkpoint `NO_CHECKPOINT` | rows 5. **First failing:** rows expected 0, was 5 |
| AC-5 `backupCursors_notCancelled_appendsEveryRow` | converts 4, appends 4, position 3, watermark `ROW_DATES[3]`, closeFolders 1, Success backedUp 4 with `STATE_FINISHED` | same (passes) |
| AC-5 `doWork_imapRestore_notCancelled_restoresEveryItem` | imports 4, rows 4, checkpoint `NO_CHECKPOINT`, closeFolders 1, Success | same (passes) |
| AC-5 `executeRestoreWithValues_notCancelled_insertsEveryItem` | rows 5, interceptor `[0..4]`, checkpoint `NO_CHECKPOINT`, Success | same (passes) |

**AC-4's checkpoint assertion (A1, plan-review O-5).** `NO_CHECKPOINT` is 0 (`RestoreCheckpointStore.kt:64`), so
a checkpoint written at index 0 reads the same as no checkpoint, and the unfixed code's cleared checkpoint also
reads 0. The **row count** (0 against 5) is the discriminator. "No checkpoint is written" follows from it,
because a checkpoint is written only after an insert (`RestoreWorker.kt:461-472`). The checkpoint line stays as
a consistency check; QA reads AC-4's "no checkpoint is written" as carried by the rows assertion together with
that ordering.

**Harness against production (A1, expert-depth edge cases).** Under the harness the loop stops at the check at
the top of the next pass. In production the in-pass `setProgress` usually suspends and is then the first
cancellable point, so the loop usually stops one step earlier, at the end of the pass in flight (L20 (d)). The
state the tests assert is the same either way, because the watermark write (`BackupWorker.kt:346`, before
`:361`) and the checkpoint write (`RestoreWorker.kt:472`, before `:321` and `:406`) precede that `setProgress`.
The check is what makes the stop certain when `setProgress` takes its fast path.

The controls use the same `launch(LAZY)`/`join` harness as the cancellation tests. That shows the harness alone
does not stop a loop, and that each cancellation test fails only because of its cancellation. This is the
AC-5 purpose, and it means the third control is not a copy of the existing `ac6_checkpointClearedOnSucceeded`.

## 3. Exact Edits

### 3.1 Production (Edit tool; working tree is CRLF, keep it)

| Id | File | `old_string` → `new_string` |
|----|------|------------------------------|
| E1 | `BackupWorker.kt:57` | `import kotlin.coroutines.coroutineContext` → `import kotlinx.coroutines.currentCoroutineContext` |
| E2 | `BackupWorker.kt:305` | `    private suspend fun backupCursors(` → two lines: `    @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)` and `    internal suspend fun backupCursors(` |
| E3 | `BackupWorker.kt:331` (`:332` after E2) | `                coroutineContext.ensureActive()` (16 spaces) → `                currentCoroutineContext().ensureActive()` |
| E4 | `RestoreWorker.kt:66` | as E1 |
| E5 | `RestoreWorker.kt:311` | the three lines `                    coroutineContext.ensureActive()` / (blank) / `                    val item = msgs[currentRestoredItem]` → the same three lines with `coroutineContext.ensureActive()` replaced by `currentCoroutineContext().ensureActive()` (20 spaces; the context line makes the match unique) |
| E6 | `RestoreWorker.kt:399` | as E5 with `                    val values = items[currentRestoredItem]` as the context line |

Nothing else under `app/src/main` or `k9mail-vendored` changes. No comment is edited. Six comments overstate the
checks today, and all become true after the fix (story, Out of scope):
- `BackupWorker.kt:72`, `:286` and `:329`;
- `RestoreWorker.kt:84`;
- `WorkManagerCancelCollector.kt:23`;
- A1: `RestoreWorker.kt:369`, verified, which reads "AC-7: checkpoint NOT cleared on CANCELLED (cooperative
  cancellation unwinds before clear)". It holds today only by the `setProgress` route; after the fix the loop's
  own check also makes it true. AC-8 (b) allows no other line in that file, so it stays untouched.

### 3.2 Tests

All test text below is final. Constraint C10 applies to the whole of it: no test text may contain the string
`@Test` other than the annotations themselves, because the authoritative count would grow by one for each.

**T1. New file `app/src/test/java/com/zegoggles/smssync/service/BackupWorkerLoopCancellationTest.kt`** (Write tool):

```kotlin
package com.zegoggles.smssync.service

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.fsck.k9.mail.store.imap.ImapMessage
import com.google.common.truth.Truth.assertThat
import com.zegoggles.smssync.contacts.ContactGroup
import com.zegoggles.smssync.mail.ConversionResult
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.mail.Headers
import com.zegoggles.smssync.mail.MessageConverter
import com.zegoggles.smssync.mail.transport.MailTransport
import com.zegoggles.smssync.preferences.Preferences
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.stubbing.Answer
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.EnumSet

/**
 * U-067 (BUG-022): the backup loop's cancellation check in [BackupWorker.backupCursors]
 * (site 1) observes the job of the coroutine that runs the loop.
 *
 * Under the unit-test harness setProgress is never a cancellation point (work-testing's
 * progress updater returns a completed future, and WorkManager 2.9.1 returns a completed
 * future without looking at the job), so the loop's own check is the only place the loop can
 * stop. Each test runs backupCursors in a child coroutine that it holds (launch LAZY, then
 * join), so cancelling that child is what WorkManager's stop does to the worker's job.
 * The cancelling test cancels the child from inside the transport's second append; the pass
 * in flight completes, and the check at the top of the next pass stops the loop.
 *
 * Not asserted: which Result a cancelled run returns (BUG-022 SC-1). Not observable here:
 * the closing of the cursors, which is done by the caller fetchAndBackupItems, outside the seam.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class BackupWorkerLoopCancellationTest {

    private lateinit var context: Context
    private lateinit var preferences: Preferences

    companion object {
        /** One SMS row per pass; each row's date is the date its conversion result carries. */
        private val ROW_DATES = listOf(1_700_000_101_000L, 1_700_000_102_000L, 1_700_000_103_000L, 1_700_000_104_000L)
    }

    /** Counters of one backup run. */
    private class BackupRun {
        var converts = 0
        var appends = 0
        var closes = 0
        var result: ListenableWorker.Result? = null
    }

    @Before
    fun setUp() {
        @Suppress("DEPRECATION")
        context = RuntimeEnvironment.application
        preferences = Preferences(context)
        preferences.dataTypePreferences.setMaxSyncedDate(DataType.SMS, DataType.Defaults.MAX_SYNCED_DATE)
    }

    /**
     * AC-1: four rows; the job is cancelled during the second append. Exactly two conversions
     * and two appends happen, the cursor stays on the second row, the watermark is the date the
     * second append confirmed, closeFolders runs once, and no completed run is reported.
     */
    @Test
    fun backupCursors_cancelledAfterSecondAppend_stopsAtNextPass() = runBlocking<Unit> {
        val run = BackupRun()
        lateinit var workJob: Job
        val transport = countingTransport(run, cancelOnAppend = 2) { workJob.cancel() }
        val cursor = smsCursor()
        val cursors = BackupCursors().apply { add(DataType.SMS, cursor) }
        val worker = buildWorker(transport)
        val converter = dateEchoConverter(run)

        workJob = launch(start = CoroutineStart.LAZY) {
            run.result = worker.backupCursors(cursors, backupConfig(transport), preferences, converter, null, cursors.count())
        }
        workJob.join()

        assertThat(workJob.isCancelled).isTrue()
        assertThat(run.appends).isEqualTo(2)
        assertThat(run.converts).isEqualTo(2)
        assertThat(cursor.position).isEqualTo(1)
        assertThat(preferences.dataTypePreferences.getMaxSyncedDate(DataType.SMS)).isEqualTo(ROW_DATES[1])
        assertThat(run.closes).isEqualTo(1)
        val result = run.result
        val completedEveryRow = result is ListenableWorker.Result.Success &&
            result.outputData.getInt(BackupWorker.PROGRESS_KEY_BACKED_UP, -1) == ROW_DATES.size
        assertThat(completedEveryRow).isFalse()
        cursors.close()
    }

    /**
     * AC-5 control: the same fixture and harness with no cancellation backs up every row,
     * leaves the watermark at the last confirmed date and reports a finished run.
     */
    @Test
    fun backupCursors_notCancelled_appendsEveryRow() = runBlocking<Unit> {
        val run = BackupRun()
        lateinit var workJob: Job
        val transport = countingTransport(run, cancelOnAppend = 0) { workJob.cancel() }
        val cursor = smsCursor()
        val cursors = BackupCursors().apply { add(DataType.SMS, cursor) }
        val worker = buildWorker(transport)
        val converter = dateEchoConverter(run)

        workJob = launch(start = CoroutineStart.LAZY) {
            run.result = worker.backupCursors(cursors, backupConfig(transport), preferences, converter, null, cursors.count())
        }
        workJob.join()

        assertThat(workJob.isCancelled).isFalse()
        assertThat(run.appends).isEqualTo(ROW_DATES.size)
        assertThat(run.converts).isEqualTo(ROW_DATES.size)
        assertThat(cursor.position).isEqualTo(ROW_DATES.size - 1)
        assertThat(preferences.dataTypePreferences.getMaxSyncedDate(DataType.SMS)).isEqualTo(ROW_DATES.last())
        assertThat(run.closes).isEqualTo(1)
        val result = run.result
        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        val data = (result as ListenableWorker.Result.Success).outputData
        assertThat(data.getInt(BackupWorker.PROGRESS_KEY_BACKED_UP, -1)).isEqualTo(ROW_DATES.size)
        assertThat(data.getString(BackupWorker.PROGRESS_KEY_STATE)).isEqualTo(BackupWorker.STATE_FINISHED)
        cursors.close()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildWorker(transport: MailTransport): BackupWorker =
        TestListenableWorkerBuilder<BackupWorker>(context)
            .setWorkerFactory(BackupWorker.TestableBackupWorkerFactoryWithTransport(transport))
            .build() as BackupWorker

    private fun backupConfig(transport: MailTransport): BackupConfig =
        BackupConfig(transport, 0, ROW_DATES.size, ContactGroup(-1L), BackupType.MANUAL, EnumSet.of(DataType.SMS), false)

    /** An SMS cursor with one row per entry of [ROW_DATES]. */
    private fun smsCursor(): MatrixCursor =
        MatrixCursor(arrayOf("_id", "date")).apply {
            ROW_DATES.forEachIndexed { i, date -> addRow(arrayOf<Any>(i + 1L, date)) }
        }

    /**
     * [StubMailTransport] (confirmed date = the result's max date), counting appends and
     * closeFolders; on append number [cancelOnAppend] (1-based; 0 = never) [onCancel] runs
     * before the append returns, so the pass in flight completes.
     */
    private fun countingTransport(run: BackupRun, cancelOnAppend: Int, onCancel: () -> Unit): MailTransport {
        val stub = StubMailTransport(appendMessagesBehavior = {
            run.appends++
            if (run.appends == cancelOnAppend) onCancel()
        })
        return object : MailTransport by stub {
            override fun closeFolders() {
                run.closes++
                stub.closeFolders()
            }
        }
    }

    /**
     * A converter that returns, for the cursor's current row, a one-message result dated
     * ROW_DATES[row]. Mocked, so the test does not depend on the real converter's columns.
     */
    private fun dateEchoConverter(run: BackupRun): MessageConverter {
        val results = ROW_DATES.map { conversionResult(it) }
        return mock(MessageConverter::class.java, Answer<Any?> { invocation ->
            if (invocation.method.name == "convertMessages") {
                run.converts++
                results[invocation.getArgument<Cursor>(0).position]
            } else {
                Mockito.RETURNS_DEFAULTS.answer(invocation)
            }
        })
    }

    private fun conversionResult(date: Long): ConversionResult {
        val result = ConversionResult(DataType.SMS)
        val message = mock(ImapMessage::class.java)
        `when`(message.getHeader(Headers.DATE)).thenReturn(arrayOf(date.toString()))
        result.add(message, emptyMap())
        return result
    }
}
```

**T2. `RestoreWorkerIntegrationTest.kt`** (Edit tool, two insertions; no existing line changes).
- **T2a** `old_string` `import kotlinx.coroutines.runBlocking` → that line followed by
  `import kotlinx.coroutines.CoroutineStart`, `import kotlinx.coroutines.Job` and `import kotlinx.coroutines.launch`.
- **T2b** `old_string`: the three lines `    // =========================================================================`,
  `    // Helpers`, `    // =========================================================================` (`:366-368`; this
  combination is unique). `new_string`: the block below, then a blank line, then the same three lines.

```kotlin
    // =========================================================================
    // U-067 (BUG-022): the IMAP restore loop's cancellation check (site 2)
    // =========================================================================

    /**
     * U-067 AC-2: four SMS handles; the job of the coroutine running doWork() is cancelled
     * during the second importMessageBody. The pass in flight completes (insert, checkpoint),
     * and the check at the top of the next pass stops the loop: two imports, two rows,
     * checkpoint 1 (not cleared), closeFolders once. The Result is not asserted (BUG-022 SC-1).
     */
    @Test
    fun doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass() = runBlocking<Unit> {
        lateinit var workJob: Job
        val run = cancellableImapRun(itemCount = 4, cancelOnImportCall = 2) { workJob.cancel() }
        val worker = buildWorkerWithTransport(run.transport)

        workJob = launch(start = CoroutineStart.LAZY) { run.result = worker.doWork() }
        workJob.join()

        assertThat(workJob.isCancelled).isTrue()
        assertThat(run.importCalls).isEqualTo(2)
        assertThat(fakeProvider.rowCount()).isEqualTo(2)
        assertThat(checkpointStore.read(RestoreWorker.RESTORE_WORK_NAME)).isEqualTo(1)
        assertThat(run.closeCalls).isEqualTo(1)
    }

    /**
     * U-067 AC-5 control: the same fixture and harness with no cancellation restores every
     * item, clears the checkpoint and succeeds.
     */
    @Test
    fun doWork_imapRestore_notCancelled_restoresEveryItem() = runBlocking<Unit> {
        lateinit var workJob: Job
        val run = cancellableImapRun(itemCount = 4, cancelOnImportCall = 0) { workJob.cancel() }
        val worker = buildWorkerWithTransport(run.transport)

        workJob = launch(start = CoroutineStart.LAZY) { run.result = worker.doWork() }
        workJob.join()

        assertThat(workJob.isCancelled).isFalse()
        assertThat(run.importCalls).isEqualTo(4)
        assertThat(fakeProvider.rowCount()).isEqualTo(4)
        assertThat(checkpointStore.read(RestoreWorker.RESTORE_WORK_NAME)).isEqualTo(RestoreCheckpointStore.NO_CHECKPOINT)
        assertThat(run.closeCalls).isEqualTo(1)
        assertThat(run.result).isInstanceOf(ListenableWorker.Result.Success::class.java)
    }

    /** Counters and the transport of one U-067 IMAP restore run. */
    private class CancellableImapRun {
        var getMessagesCalls = 0
        var importCalls = 0
        var closeCalls = 0
        var result: ListenableWorker.Result? = null
        lateinit var transport: MailTransport
    }

    /**
     * U-067: [itemCount] SMS handles from the SMS folder (the call-log folder returns none);
     * import call i returns the SMS item i of [createSmsItems]; on import call number
     * [cancelOnImportCall] (1-based; 0 = never) [onCancel] runs before the result is returned.
     * closeFolders is counted by delegation, so [ImapRestoringTransport] is unchanged.
     */
    private fun cancellableImapRun(itemCount: Int, cancelOnImportCall: Int, onCancel: () -> Unit): CancellableImapRun {
        val run = CancellableImapRun()
        val items = createSmsItems(itemCount)
        val handles = (0 until itemCount).map { MailTransportTestFactories.createHandle("u067-$it") }
        val imap = ImapRestoringTransport(
            folderHandleForAll = MailTransportTestFactories.createFolderHandle(),
            getMessagesImpl = { _, _, _, _ ->
                run.getMessagesCalls++
                if (run.getMessagesCalls == 1) handles.toMutableList() else mutableListOf()
            },
            importMessageBodyImpl = { _, h, _ ->
                val index = run.importCalls
                run.importCalls++
                if (run.importCalls == cancelOnImportCall) onCancel()
                MessageImportResult(h?.uid ?: "unknown", DataType.SMS, items[index])
            }
        )
        run.transport = object : MailTransport by imap {
            override fun closeFolders() {
                run.closeCalls++
                imap.closeFolders()
            }
        }
        return run
    }
```

**T3. `RestoreWorkerCheckpointTest.kt`** (Edit tool, two insertions; no existing line changes).
- **T3a** `old_string` `import kotlinx.coroutines.runBlocking` → that line followed by
  `import kotlinx.coroutines.CoroutineStart`, `import kotlinx.coroutines.Job`,
  `import kotlinx.coroutines.currentCoroutineContext` and `import kotlinx.coroutines.launch`.
- **T3b** `old_string`: the two lines `    // -----------------------------------------------------------------------` and
  `    // InMemoryCheckpointStore unit tests` (`:408-409`; unique). `new_string`: the block below, then a blank
  line, then the same two lines.

```kotlin
    // -----------------------------------------------------------------------
    // U-067 (BUG-022): the test-facing restore loop's cancellation check (site 3)
    // -----------------------------------------------------------------------

    /** U-067: records each afterInsert index; at [cancelAtIndex] (-1 = never) runs [onCancel]. */
    private class CancellingInterceptor(
        private val cancelAtIndex: Int,
        private val onCancel: () -> Unit
    ) : RestoreInsertInterceptor {
        val seen = mutableListOf<Int>()
        override suspend fun afterInsert(insertIndex: Int) {
            seen.add(insertIndex)
            if (insertIndex == cancelAtIndex) onCancel()
        }
    }

    /**
     * U-067 AC-3: five items; the job is cancelled in afterInsert(2). The pass in flight
     * completes, and the check at the top of the next pass stops the loop: three rows
     * (indexes 0 to 2), checkpoint 2, not cleared. Stricter than a pre-cancel test: a single
     * check placed before the loop would pass AC-4 but not this test.
     */
    @Test
    fun executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass() = runBlocking<Unit> {
        val items = createSmsItems(5)
        val workName = "test_restore_u067_cancel_mid_loop"
        lateinit var workJob: Job
        val interceptor = CancellingInterceptor(cancelAtIndex = 2) { workJob.cancel() }
        val worker = buildWorker(interceptor = interceptor)

        workJob = launch(start = CoroutineStart.LAZY) {
            worker.executeRestoreWithValues(items, 0, preferences, context, workName)
        }
        workJob.join()

        assertThat(workJob.isCancelled).isTrue()
        assertThat(countSmsRows()).isEqualTo(3)
        assertThat(interceptor.seen).containsExactly(0, 1, 2).inOrder()
        assertThat(checkpointStore.read(workName)).isEqualTo(2)
    }

    /**
     * U-067 AC-4: the child cancels its own job in its first statement and then calls the
     * function, with no suspension in between (UNDISPATCHED: the body runs at once). Nothing
     * is inserted and no checkpoint is written. Rows and checkpoint are asserted, not a thrown
     * CancellationException: the loop's IllegalStateException catch turns it into a Result.
     */
    @Test
    fun executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing() = runBlocking<Unit> {
        val items = createSmsItems(5)
        val workName = "test_restore_u067_cancel_before_loop"
        val worker = buildWorker()

        val child = launch(start = CoroutineStart.UNDISPATCHED) {
            currentCoroutineContext()[Job]!!.cancel()
            worker.executeRestoreWithValues(items, 0, preferences, context, workName)
        }
        child.join()

        assertThat(child.isCancelled).isTrue()
        assertThat(countSmsRows()).isEqualTo(0)
        assertThat(checkpointStore.read(workName)).isEqualTo(RestoreCheckpointStore.NO_CHECKPOINT)
    }

    /**
     * U-067 AC-5 control: the AC-3 fixture and harness with no cancellation inserts every
     * item, clears the checkpoint and succeeds.
     */
    @Test
    fun executeRestoreWithValues_notCancelled_insertsEveryItem() = runBlocking<Unit> {
        val items = createSmsItems(5)
        val workName = "test_restore_u067_not_cancelled"
        lateinit var workJob: Job
        val interceptor = CancellingInterceptor(cancelAtIndex = -1) { workJob.cancel() }
        val worker = buildWorker(interceptor = interceptor)
        var result: ListenableWorker.Result? = null

        workJob = launch(start = CoroutineStart.LAZY) {
            result = worker.executeRestoreWithValues(items, 0, preferences, context, workName)
        }
        workJob.join()

        assertThat(workJob.isCancelled).isFalse()
        assertThat(countSmsRows()).isEqualTo(5)
        assertThat(interceptor.seen).containsExactly(0, 1, 2, 3, 4).inOrder()
        assertThat(checkpointStore.read(workName)).isEqualTo(RestoreCheckpointStore.NO_CHECKPOINT)
        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
    }
```

**T4 (AC-9). `BackupWorkerTest.kt:27-30`** (comment only; `:31` is the closing ` */` and stays). `old_string`
is the four lines:
```
 * AC-5 (structured cancellation via ensureActive) is verified at the WorkManager
 * level: the coroutine scope is cancelled by WorkManager's cooperative cancellation
 * when cancelUniqueWork() is called. The ensureActive() calls inside the loop unwind
 * the coroutine at the next suspension point.
```
`new_string` is these four lines, so no line moves:
```
 * AC-5 (cancellation): ic2_workManager_cancelsBackupWorkerRequest asserts WorkManager's
 * state only (CANCELLED after cancelUniqueWork()); it does not run the backup loop. That
 * the loop stops at its next pass when its job is cancelled is shown by U-067's
 * BackupWorkerLoopCancellationTest.backupCursors_cancelledAfterSecondAppend_stopsAtNextPass.
```

### 3.3 Test table (one row per new test)

| # | Test (class) | AC | Asserts | Bitten by |
|---|--------------|----|---------|-----------|
| 1 | `backupCursors_cancelledAfterSecondAppend_stopsAtNextPass` (`BackupWorkerLoopCancellationTest`) | AC-1 (a), (b), (c) closeFolders half, (d) | 2 appends, 2 conversions, cursor at row 1, watermark `ROW_DATES[1]`, closeFolders 1, no Success with backedUp 4 | M-1a, M-1b; red run |
| 2 | `backupCursors_notCancelled_appendsEveryRow` (same) | AC-5 | 4 appends, 4 conversions, row 3, watermark last, closeFolders 1, Success backedUp 4, `STATE_FINISHED` | none (control; passes before and after) |
| 3 | `doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass` (`RestoreWorkerIntegrationTest`) | AC-2 (a) to (d) | 2 imports, 2 rows, checkpoint 1, closeFolders 1 | M-2a, M-2b; red run |
| 4 | `doWork_imapRestore_notCancelled_restoresEveryItem` (same) | AC-5 | 4 imports, 4 rows, `NO_CHECKPOINT`, closeFolders 1, Success | none (control) |
| 5 | `executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass` (`RestoreWorkerCheckpointTest`) | AC-3 | 3 rows, interceptor `[0, 1, 2]`, checkpoint 2 | M-3a, M-3b; red run |
| 6 | `executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing` (same) | AC-4 | 0 rows (the discriminator), `NO_CHECKPOINT` (a consistency check; 2.4, A1) | M-3a, M-3b; red run |
| 7 | `executeRestoreWithValues_notCancelled_insertsEveryItem` (same) | AC-5 | 5 rows, interceptor `[0..4]`, `NO_CHECKPOINT`, Success | none (control) |

## 4. Increments

### 4.0 Environment and conventions (every step)

```bash
# From the repository root. Never set MSYS_NO_PATHCONV for Gradle.
unset MSYS_NO_PATHCONV
W=sdlc/artifacts/build/sprints/sprint-023/U-067; E=$W/evidence; M=$E/mutations
T=sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools      # read-only
INIT=$T/junit-xml.init.gradle
R=app/build/test-results/testDebugUnitTest
BW=app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt
RW=app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt
TS=app/src/test/java/com/zegoggles/smssync/service
BLT=$TS/BackupWorkerLoopCancellationTest.kt; RIT=$TS/RestoreWorkerIntegrationTest.kt
RCT=$TS/RestoreWorkerCheckpointTest.kt; BWT=$TS/BackupWorkerTest.kt
BW_BLOB=5c554ec3bf737383f5a8717fdfc518e5eac33993; RW_BLOB=871ec7c764be5f9557fdc25ec0683cb835cfeac7
CP=app/src/main/java/com/zegoggles/smssync   # IC-2 files below
WORK=<a u067-work folder inside the developer's own session scratch folder>   # raw logs, pristine copies
```
`START` is the full id that `git rev-parse HEAD` prints at I0, recorded in the implementation log. It is expected
to be `4095db28…`. Shell state does not persist between calls, so each call sets what it needs. `WORK` is the
only absolute path, and it is never written into a repository file.

**Class sets** (`--tests`, package `com.zegoggles.smssync.service.`):
- **NEW3** = `BackupWorkerLoopCancellationTest`, `RestoreWorkerIntegrationTest`, `RestoreWorkerCheckpointTest`
  (31 cases at the end).
- **SEVEN** = NEW3 plus `BackupWorkerTest`, `BackupWorkerIntegrationTest`, `BackupWorkerWatermarkTest`,
  `RestoreWorkerTest` (67 cases).

**TR(name, classes…)**: one Gradle call, in the foreground, with a 600000 ms tool timeout. The results folder is
deleted first and the init script is always passed:
```bash
rm -rf $R; ./gradlew -I "$INIT" --console=plain :app:testDebugUnitTest --tests "com.zegoggles.smssync.service.<C1>" [--tests "…<C2>" ...] > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> $WORK/logs/<name>.log
python -I -X utf8 $T/sanitize.py $WORK/logs/<name>.log $E/<name>.txt --grep '^(BUILD |FAILURE|U063-DAEMON-JVM|exit=)|tests completed|FAILED$'
python -I -X utf8 $T/junit_cases.py $R BackupWorker RestoreWorker > $WORK/logs/<name>-cases.log; python -I -X utf8 $T/sanitize.py $WORK/logs/<name>-cases.log <dest>-cases.txt
```
For a failure message: `grep -o 'message="[^"]*"' $R/TEST-<fqcn>.xml > $WORK/logs/<name>-msg.log`, then
sanitise it into `<dest>-message.txt`.

**KC(name)**, the AC-7 compile: one Gradle call, foreground, 600000 ms:
```bash
./gradlew -I "$INIT" --console=plain :app:compileDebugKotlin --rerun --no-build-cache > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> $WORK/logs/<name>.log
python -I -X utf8 $T/sanitize.py $WORK/logs/<name>.log $E/<name>.txt --grep '^(w|e): |> Task :app:compileDebugKotlin|^BUILD |U063-DAEMON-JVM|exit='
{ echo "task-line=$(grep -c '^> Task :app:compileDebugKotlin$' $E/<name>.txt)"; echo "task-skipped=$(grep -cE '^> Task :app:compileDebugKotlin (UP-TO-DATE|FROM-CACHE|NO-SOURCE|SKIPPED)' $E/<name>.txt)"; echo "warnings=$(grep -c '^w: ' $E/<name>.txt)"; echo "worker-coroutineContext=$(grep -E '^w: .*(BackupWorker|RestoreWorker)\.kt' $E/<name>.txt | grep -c 'coroutineContext')"; } > $E/<name>-summary.txt
```
`--rerun` is Gradle's built-in task option. It forces this one task to execute non-incrementally, so the Kotlin
compiler recompiles and reports the whole module. `--no-build-cache` stops a cached output from standing in for
the run.

**Gate G**:
```bash
rm -rf $R; ./gradlew -I "$INIT" --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> $WORK/logs/<name>.log
```
It is sanitised with `--grep '^(BUILD |FAILURE|> Task :app:(assemble|test|jacoco)|U063-DAEMON-JVM|exit=)|tests completed|FAILED$'`,
followed by `python -I -X utf8 $T/junit_totals.py $R > $E/<name>-junit-totals.txt`.

**Timestamps** come from `date -u +%Y-%m-%dT%H:%M:%SZ`, run at that moment.

**Expected Gradle invocations: 12** (A1; 1 + 1 + 1 + 1 + 6 + 1 + 1). I0 gate (1), I0 KC (1), I3 red (1), I5 green (1), I6 mutations (6), I7 KC
(1), I8 gate (1). Rows D2, D6, D8 and D11 may add one logged repeat each.

### I0 — Baseline (no edit before this step passes)

1. `mkdir -p "$WORK/logs" "$WORK/pristine" "$M"`. Then one call into `$WORK/logs/I0-baseline.log`, sanitised to
   `$E/I0-baseline.txt`:
   ```bash
   date -u +%Y-%m-%dT%H:%M:%SZ
   git rev-parse HEAD; git rev-parse --abbrev-ref HEAD
   git status --porcelain -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat; echo "porcelain-end"
   git rev-parse HEAD:$BW HEAD:$RW; git hash-object $BW $RW
   git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"
   git grep -h "@Ignore" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Ignore"
   grep -v '^#' gradle/gradle-daemon-jvm.properties
   test -e $BLT; echo "new-file-exists-exit=$?"
   ```
   Expected:
   - HEAD `4095db28…`, or a descendant that changes nothing under `app`, `k9mail-vendored` or the build files.
   - Nothing before `porcelain-end`.
   - The two HEAD blobs and the two working-tree hashes equal `BW_BLOB` and `RW_BLOB`.
   - The count. **Record it as "the count at the start of this story", with the commit id.** It is 746 at
     `4095db28`; the plan does not hard-code it.
   - The `@Ignore` line count, recorded.
   - `toolchainVersion=25` and `new-file-exists-exit=1`.

   **Stop:** row D1.
2. Gate G (`I0-gates`). Expected: `exit=0`; `U063-DAEMON-JVM: java.version=25…`;
   `tests=<n> failures=0 errors=0 skipped=<s>`. Record `<s>` as the start skipped count (2 expected) and `<n>` as
   the start Gradle total. **Stop:** D1.
3. KC(`I0-kotlinc`), the AC-7 baseline that proves the method can see the warnings. Expected:
   `task-line=1 task-skipped=0`, `warnings` ≥ 4, `worker-coroutineContext=3`. The three lines are
   `BackupWorker.kt:331:17`, `RestoreWorker.kt:311:21` and `RestoreWorker.kt:399:21`, as in
   `sprint-017/U-060/evidence/final-gates.log:108-110`. **Row:** D2.
4. Create `$W/implementation-log.md`: a header, then the I0 entry with the clock time, `START`, the start count
   with its commit, the start skipped count and the Gradle total.

### I1 — Route S seam (E2 only)

Apply E2. Write `git diff -U0 HEAD -- $BW | grep '^@@'` to `$E/I1-seam.txt`. Expected: exactly
`@@ -305 +305,2 @@`. There is no Gradle run here; I3 compiles the seam.

### I2 — Tests (T1 to T4; no other production edit yet)

Write T1 and apply T2a, T2b, T3a, T3b and T4. One call writes `$E/I2-tests.txt`:
```bash
git diff --stat HEAD -- $TS
git diff HEAD -- $RIT $RCT | grep -c '^-[^-]'; echo "rit-rct-removed-lines-end"          # 0
git diff -U0 HEAD -- $BWT | grep '^@@'                                                     # @@ -27,4 +27,4 @@
git diff -U0 HEAD -- $BWT | grep -E '^[-+]' | grep -vE '^(\+\+\+|---) ' | grep -vcE '^[-+] \* '; echo "bwt-non-comment-end"   # 0
git grep --untracked -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"   # start + 7
grep -c "@Test" $BLT; grep -c "@Ignore\|Assume" $BLT; echo "blt-end"                       # 2, then 0
```
**Stop:** D4. A count other than start + 7 means some "@Test" text is in a comment: fix the text.

### I3 — Red run (fix absent, seam and tests present)

TR(`I3-red`, SEVEN); cases go to `$E/I3-red-cases.txt`.

Expected: 67 cases.
- Exactly four fail, each with an `AssertionError`: tests 1, 3, 5 and 6 of 3.3, with the first failing assertion
  as in 2.4.
- Every other case passes: the three controls and all 60 existing cases.
- This run is also Verification Step 3 (the old binding at all three sites at once).

**Rows:** D3 to D9.

### I4 — Production fix (E1, E3, E4, E5, E6)

Apply the five edits. Then one call writes `$E/I4-production.txt`:
```bash
git diff --name-only HEAD -- app/src/main k9mail-vendored
git diff -U0 HEAD -- $BW | grep '^@@'; git diff -U0 HEAD -- $RW | grep '^@@'
git diff --numstat HEAD -- $BW $RW
git grep -n "coroutineContext" -- $BW $RW; echo "old-name-exit=$?"
git grep -n "ensureActive()" -- app/src/main
git hash-object $BW $RW
git diff HEAD -- app/src/main k9mail-vendored | git hash-object --stdin
```
Expected:
- The two files, `$BW` then `$RW`.
- BW hunks `@@ -57 +57 @@`, `@@ -305 +305,2 @@`, `@@ -331 +332 @@`; RW hunks `@@ -66 +66 @@`, `@@ -311 +311 @@`,
  `@@ -399 +399 @@`.
- numstat `4 3` for BW and `3 3` for RW.
- `old-name-exit=1` (IC-3).
- `ensureActive()` at exactly three call lines, each `currentCoroutineContext().ensureActive()`
  (`BackupWorker.kt:332`, `RestoreWorker.kt:311`, `:399`), plus the comments `BackupWorker.kt:72`, `:286`, `:329`,
  `RestoreWorker.kt:84` and `WorkManagerCancelCollector.kt:23`.

Then:
- `cp $BW $RW "$WORK/pristine/"`;
- `git hash-object -w $BW $RW`, recording `BW_NEW` and `RW_NEW`;
- record the diff hash as `PROD_DIFF`.

**Stop:** D10.

### I5 — Green run

TR(`I5-green`, SEVEN), with cases in `$E/I5-green-cases.txt`. Expected: `exit=0`, 67 cases, all pass.
**Row:** D11.

### I6 — Mutations (AC-6; write-ahead and restore protocol)

`$M/../mutation-log.md` (that is, `$E/mutation-log.md`) starts with this header:
- the title;
- "Protocol: U-067 plan section I6 (U-065 AC-7 / U-066 AC-9 form: write-ahead entry with clock time, one Edit,
  one targeted run, restore by copying the pristine file, hash and porcelain check, `restored` line).
  Repository-relative paths.";
- `START`, `BW_BLOB`, `RW_BLOB`, `BW_NEW`, `RW_NEW`, `PROD_DIFF`;
- the recovery note: "If a session is lost while an entry says `APPLYING` and has no `restored` line, nothing else
  runs (no gate, no review, no commit) until the file's hash has been compared with `BW_NEW` or `RW_NEW` and, if
  it differs, the file has been restored from its pristine copy. If that copy is lost:
  `git cat-file --filters --path=<path> <BW_NEW|RW_NEW> > <path>`. This applies the checkout conversion, so the
  file gets its working-tree CRLF endings, byte for byte the same as the normal restore by `cp` from the pristine
  copy, and `git hash-object` equals the saved blob. `git checkout` is never used, because it would discard the
  fix."
  A1, plan-review O-2: with `core.autocrlf=true` in this checkout, the filtered form was checked read-only at
  planning. `git cat-file --filters --path=$BW $BW_BLOB` is byte-identical to the working-tree file (`cmp` exit
  0). The unfiltered `git cat-file blob` would write LF endings and is not used.

**Clean check** (before and after each entry):
- `git hash-object $BW $RW` equals `BW_NEW` and `RW_NEW`;
- `git diff HEAD -- app/src/main k9mail-vendored | git hash-object --stdin` equals `PROD_DIFF`;
- `git status --porcelain -- app/src/main` lists exactly ` M $BW` and ` M $RW`.

**Each entry, one at a time:**
1. Clean check.
2. Write-ahead entry `### <id> APPLYING <clock>`: the file, its hash before, the edit, the class set (NEW3) and
   the expected result.
3. One Edit. Read the lines first and copy `old_string` from the Read output.
4. One TR(`<id>`, NEW3), with cases written to `$M/<id>-cases.txt` and the failure message to
   `$M/<id>-message.txt`.
5. Restore at once by `cp "$WORK/pristine/<file>" <path>`, whatever the result.
6. Clean check again, then append `run_exit`, `cases`, `failed`, `passed`,
   `expected: matches | does not match`, `verdict`, and `restored: <clock> hash-ok diff-hash-ok porcelain-ok`.

No gate, review or commit runs while an entry lacks its `restored` line.

| Id | File | Exact edit (`old_string` → `new_string`) | Must fail (NEW3, 31 cases) | Must pass |
|----|------|------------------------------------------|----------------------------|-----------|
| M-1a | BW | `                currentCoroutineContext().ensureActive()` → `                coroutineContext.ensureActive()` (old binding; compiles through the inherited member, no import needed) | test 1 — "`appends` expected 2, was 4" (A) | the other 30, including tests 3, 5, 6 and the controls 2, 4, 7 |
| M-1b | BW | the same `old_string` → `                if (false) currentCoroutineContext().ensureActive()` | test 1 (A) | the other 30 |
| M-2a | RW | the E5 three lines (fixed form) → the same with `coroutineContext.ensureActive()` | test 3 — "`importCalls` expected 2, was 4" (A) | the other 30, including 1, 5, 6, controls |
| M-2b | RW | the E5 three lines (fixed form) → the same with `if (false) currentCoroutineContext().ensureActive()` | test 3 (A) | the other 30 |
| M-3a | RW | the E6 three lines (fixed form) → the same with `coroutineContext.ensureActive()` | tests 5 ("rows expected 3, was 5") and 6 ("expected 0, was 5") (A) | the other 29, including 1, 3, controls |
| M-3b | RW | the E6 three lines (fixed form) → the same with `if (false) currentCoroutineContext().ensureActive()` | tests 5 and 6 (A) | the other 29 |

"(A)" means an `AssertionError` (Truth). Each row was derived from the test code in 3.2 and the production code at
HEAD, by the passes worked through in 2.4. No mutation reaches another site's test, because each test goes
through exactly one loop:
- test 1 reaches `backupCursors` only;
- tests 3 and 4 reach `runImapRestoreLoop` only (`doWork` → `executeRestore`);
- tests 5 to 7 reach `executeRestoreWithValues` only.

The existing classes cancel no loop, so they are not run per mutation; the final gate runs them unmutated. The
order is M-1a, M-1b, M-2a, M-2b, M-3a, M-3b. At the end, add a summary table under the header (id, site, failing
of 31, verdict). **Rows:** D12 to D14.

### I7 — AC-7 compile

Before it: the I6 clean check, and every entry has its `restored` line. Then KC(`I7-kotlinc`). Expected:
`task-line=1 task-skipped=0`, `warnings` ≥ 1 (the other files' warnings, for example `SmsDefaultRoleHelper.kt`,
show the compiler really ran), `worker-coroutineContext=0`. Also `grep -cE '^w: .*(BackupWorker|RestoreWorker)\.kt' $E/I7-kotlinc.txt`
is recorded with each line it counts. **Rows:** D2, D20.

### I8 — Final gate, counts and file checks

1. The I6 clean check passes.
2. Gate G (`I8-gates`). Expected: `exit=0`, `tests=<n>+7 failures=0 errors=0 skipped=<s>` (skipped not above the
   start). JaCoCo passes and no floor is edited. **Row:** D15.
3. `python -I -X utf8 $T/junit_cases.py $R BackupWorker RestoreWorker > $WORK/logs/I8-cases.log`. Sanitise it into
   `$E/I8-seven-cases.txt`, then `diff $E/I5-green-cases.txt $E/I8-seven-cases.txt; echo "determinism-diff-exit=$?"`.
   Expected 0. The class filter of the two runs is the same, so this is the determinism check, with no extra run.
4. `$E/I8-checks.txt`:
   ```bash
   git grep --untracked -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"   # start + 7 (working tree, --untracked: CR-016 form)
   git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"         # start count, until the commit
   git status --porcelain -- app k9mail-vendored                         # the six lines below
   git diff --name-only HEAD -- app/src/main k9mail-vendored             # $BW, $RW (AC-8 a)
   git diff -U0 HEAD -- $BW $RW | grep '^@@'                             # the six hunks of I4 (AC-8 b)
   git diff HEAD -- app/src/main k9mail-vendored | git hash-object --stdin   # PROD_DIFF
   git diff --name-only HEAD -- $CP/activity/StatusPreference.java $CP/service/WorkManagerCancelCollector.kt $CP/activity/MainViewModel.kt; echo "ic2-end"   # nothing (IC-2)
   git grep -n "coroutineContext" -- $BW $RW; echo "ic3-old-name-exit=$?"     # 1 (IC-3)
   git grep -n "ensureActive()" -- app/src/main                              # 3 call lines + 5 comments (IC-3)
   git diff --stat HEAD -- build.gradle app/build.gradle k9mail-vendored/build.gradle settings.gradle gradle; echo "build-diff-end"   # nothing (AC-12)
   git diff --quiet HEAD -- gradle/verification-metadata.xml; echo "vm-exit=$?"   # 0
   git grep --untracked -h "@Ignore" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Ignore"   # equals I0's @Ignore count
   git diff HEAD -- app/src/test | grep -cE '^\+.*(@Ignore|Assume)'; grep -cE '@Ignore|Assume' $BLT; echo "ignore-end"   # 0, 0
   git diff --name-only $START HEAD -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat; echo "no-code-commit-end"   # nothing (A1: only document commits since START)
   git status --porcelain -- $W; echo "workspace-end"             # only files the developer wrote (A1)
   ```
   **A1 (plan-review O-1): scope of these checks.** They cover this story's own paths only: `app`,
   `k9mail-vendored`, the build files and the workspace `$W`. Changes elsewhere are not this story's and are never a
   D15 stop. This includes `sdlc/artifacts/build/sprints/sprint-024/**`, where a U-068 planner and its plan-gate
   reports write during this build, and document commits the orchestrator may make while the developer runs.
   - A changed or new path outside the story's paths that the developer notices is recorded in the implementation
     log with its path only.
   - In `$W`, a file the developer did not write (for example a gate report added by the orchestrator) is likewise
     recorded and not a stop.
   - `no-code-commit-end` must be preceded by nothing. A code commit by someone else during the run is D15.
   The expected `app k9mail-vendored` status, in `git status` order:
   - ` M app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt`
   - ` M app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt`
   - ` M app/src/test/java/com/zegoggles/smssync/service/BackupWorkerTest.kt`
   - ` M app/src/test/java/com/zegoggles/smssync/service/RestoreWorkerCheckpointTest.kt`
   - ` M app/src/test/java/com/zegoggles/smssync/service/RestoreWorkerIntegrationTest.kt`
   - `?? app/src/test/java/com/zegoggles/smssync/service/BackupWorkerLoopCancellationTest.kt`

   The story's `<start>..HEAD` forms apply after the orchestrator's commit. Before it, the working-tree forms above
   are the same checks, and the implementation log says so (REQ-MODERNIZATION-015 AC-13 as amended by CR-016).
   **Row:** D15.

### I9 — Implementation log and hand-off

For each step, log the clock time, a command summary (repository-relative paths; `$WORK` named only as "the work
folder"), the result line from the evidence file, and any decision row used. Close with:
- **Counts**, relative to "the count at the start of this story":
  - the seven tests added, listed by name (3.3);
  - none deleted, none edited;
  - the Gradle total checked against the count.
- **Declared items:**
  - the AC-1 (c) narrowing (2.3);
  - IC-1's second caller (`:462`);
  - the restore by copy during mutations;
  - the working-tree count form (CR-016).
- **"Runtime Claims"**, listing only what evidence files show.
- **Last line:** "Hygiene scan: step I10, result in `evidence/I10-scan.txt`".

The developer edits neither the story nor any record. Section 7 lists the items for the orchestrator.

### I10 — Evidence-hygiene scan (last step, after every file is final)

```bash
P='[a-z0-9._%+-]+@[a-z0-9-]+\.[a-z]{2,}|Users[/\\]|App''Data'
L=$WORK/logs
git diff HEAD -- app/src k9mail-vendored > $L/I10-code.diff; cat $BLT >> $L/I10-code.diff
{
  grep -rlEi --exclude=compiled-pipeline.json "$P" $W $L/I10-code.diff > $L/I10-pattern-files.log
  echo "pattern-scan exit=$? files=$(wc -l < $L/I10-pattern-files.log)"
  for label in user-name host-name; do
    case $label in user-name) v="${USERNAME:-$USER}" ;; host-name) v="${COMPUTERNAME:-$(hostname)}" ;; esac
    if [ ${#v} -lt 3 ]; then echo "$label-scan value-unavailable"; continue; fi
    grep -rliF --exclude=compiled-pipeline.json -e "$v" $W $L/I10-code.diff > $L/I10-$label-files.log
    echo "$label-scan exit=$? files=$(wc -l < $L/I10-$label-files.log)"
  done
} > $L/I10-scan.log 2>&1
```
Expected: every scan reports `exit=1 files=0`. Sanitise `$L/I10-scan.log` into `$E/I10-scan.txt`.

The e-mail part of `P` is `sanitize.py`'s own form, which needs a character before `@`. U-066's form did not, and
it would flag the annotation `@androidx.annotation…` of E2 in the code diff and in this plan, so the plan uses the
sanitiser's form. **Row:** D17.

## 5. Decision Table

| Row | Observation | What the developer does |
|-----|-------------|-------------------------|
| D1 | I0: HEAD is not `4095db28` or a descendant that leaves `app`, `k9mail-vendored` and the build files unchanged (A1: `0d5755c4` is such a descendant); a path is dirty; a blob differs; the toolchain is not 25; the new file already exists; or the gate fails | Stop and report, with the sanitised evidence (nothing has been edited). A later HEAD that changes nothing in those paths: record its id as `START` and continue |
| D2 | KC: `task-skipped=1` or `task-line=0`; or at I0 `worker-coroutineContext` is not 3 although the task ran | Repeat once with `--rerun-tasks` in place of `--rerun` (logged, same summary). If still not as expected: stop and report "AC-7 method cannot be shown"; do not edit anything to make it so |
| D3 | I3: compile error at E2 (an exposure error naming `CalendarSyncer` or another type) | Stop and report. No other production visibility may change (AC-8) |
| D4 | I2 or I3: a new test file does not compile (for example access to `BackupConfig`, `BackupCursors()` or `add`, or a Mockito overload) | Correct the **test** text only, keeping every assertion of 3.2, and log the change. If the only cure is a production change, stop and report |
| D5 | I3: a cancellation test (1, 3, 5 or 6) **passes** on the unfixed code | Stop and report: the harness property L6 does not hold in this build, so the test cannot tell the fix apart. Neither dropping the test nor weakening it is acceptable |
| D6 | I3: a cancellation test fails at `isCancelled` (false), or with an exception escaping the test (`ERROR`), e.g. `UninitializedPropertyAccessException` | A fixture defect: check that the job is assigned before `join()` and that `LAZY` is used (2.2). Correct the test text (fixture and harness only; every assertion of 3.2 kept, as in D4), log it, and repeat I3 once |
| D7 | I3: a control (2, 4 or 7) fails | A fixture defect: diagnose from its message (e.g. the handle count, or which folder is called first), correct the fixture only and repeat I3 once. If the control cannot pass on the unfixed code without changing an AC's numbers: stop and report |
| D8 | I3: the backup tests error with "WorkManager is not initialized" | Add to the new file's `setUp` the test WorkManager initialisation of `RestoreWorkerIntegrationTest.kt:88-98`, with `BackupWorker.TestableBackupWorkerFactory()` as the worker factory (test-only). Log it and repeat I3 once |
| D9 | I3: an existing test fails | Stop and report: E2 or a test edit changed existing behaviour |
| D10 | I1 or I4: a hunk, a numstat line or a grep differs from the expected result (at I1: anything other than `@@ -305 +305,2 @@`) | Correct the edit to the exact text of 3.1 and repeat that step's checks. Never add another production change |
| D11 | I5: a test fails | A new test: diagnose against 2.4. If it is the fixture: D6 or D7 (one logged repeat). If the fix is wrong (e.g. an edit missed): D10. An existing test: stop and report |
| D12 | I6: a mutation does not compile | Log it as `invalid` with the compiler line, and use its compiling replacement. For (a): `                (this as androidx.work.CoroutineWorker).coroutineContext.ensureActive()` (indentation as the site). For (b): the call line replaced by the same indentation followed by `// currentCoroutineContext().ensureActive()`. Run the replacement as `<id>'`; an invalid entry never counts as a detection |
| D13 | I6: a mutation is not detected (its must-fail test passes) | Restore, log a **finding** "AC-6 unmet for <id>", finish the remaining entries, then stop and report before I7 |
| D14 | I6: more tests fail than the row names, or one of the must-pass tests fails | Restore, log `expected: does not match`, finish the remaining entries, and report it in the hand-off as a deviation for the reviewers. It is a detection if the must-fail test failed |
| D15 | I8: gate not 0; failures or errors; skipped above the start; Gradle total not `<n>+7`; working-tree count not start + 7; a code commit since `START`; any other check of the story's own paths not as expected | Stop and report with the evidence. No floor, limit or minimum edit; no `@Ignore`. A1: a change outside the story's paths (sprint 024, other documents, the orchestrator's document commits) is recorded with its path and is **not** a stop |
| D17 | I10: any scan has `exit=0` | Re-sanitise the named file or correct the log text, then repeat the scan. A hit in a code file: stop and report |
| D18 | The tool moves a Gradle run to the background by itself | Wait for its completion notice; never poll; start nothing else meanwhile |
| D19 | "bash.exe … not found", or a process killed without a visible reason | Stop and report (endpoint security). Do not work around it |
| D20 | I7: `worker-coroutineContext` > 0, or a new warning names `currentCoroutineContext` | Stop and report: the fix is incomplete, or L7's "not deprecated" does not hold |

## 6. Criterion-to-Step Map

| Criterion | Where shown |
|-----------|-------------|
| AC-1, AC-2, AC-3, AC-4 | Tests 1, 3, 5 and 6 (3.2). Red at I3, green at I5. AC-1 (c) cursor half: declared narrowing 2.3, ruled (QA judges it by reading). AC-4: the row count is the discriminator; "no checkpoint" follows from the insert-then-checkpoint order (2.4, A1) |
| AC-5 | Tests 2, 4 and 7, passing at I3 and I5; AC-10 |
| AC-6 | I6 `mutation-log.md`; the combined form at I3 |
| AC-7 | I0 step 3 (3 warnings) against I7 (0), with a compiler that really ran |
| AC-8 | I4 and I8 checks (two files; six hunks; imports in place; `ensureActive` import kept); no change to caught types or values (E1 to E6 touch only the call expression) |
| AC-9 | T4; I2 `bwt-non-comment-end` 0 and a single `@@ -27,4 +27,4 @@` hunk |
| AC-10 | I3, I5 and I8 (SEVEN and the full gate); I2 shows no `-` line in RIT and RCT and only comment lines in BWT |
| AC-11 | I0 and I8 gates; counts by the authoritative command (start at I0 with its commit; end in the `--untracked` form before the commit) |
| AC-12 | I8 build-diff, `vm-exit`, `@Ignore` and `Assume` checks |
| IC-1 | Constructors not edited (I4 hunks); both callers `:255`, `:462` compile (gate); test call site in T1 |
| IC-2 | I8 `ic2-end` |
| IC-3 | I4 and I8 greps |
| IC-4 | No device step. Every criterion is shown at unit level, the bug's device check is optional, and a subagent never runs an emulator or the AVD `Pixel_10_Pro` |

## 7. Items for the Orchestrator

1. **Scope Concern P-1: ruled** (A1; story Implementation Notes, "Ruling on the plan's scope concern P-1",
   2026-11-21). AC-1 (c)'s "the cursors were closed" is outside the Route S seam (`BackupWorker.kt:279-281`). The
   plan asserts `closeFolders` once and declares the cursor half not observable through the seam (2.3), and QA
   judges that half by reading the code path. The developer cites the ruling in I9.
2. **Story corrections: accepted** (the same ruling).
   - IC-1 has two production callers, `:255` and `:462`, both in the class.
   - The AC-9 Javadoc is `:27-30`.
   - A1, plan-review O-7: the story's Affected Code row "`BackupWorkerWatermarkTest.kt` or a new test file" no
     longer applies. The plan takes the new-file branch (T1), and `BackupWorkerWatermarkTest.kt` gets no diff. This
     is noted for the reviewers, who should not expect one; the story text is the orchestrator's to update, if
     wanted.
3. **Line citations.** After E2, site 1 is `BackupWorker.kt:332` (it was `:331`). No `RestoreWorker.kt` line
   moves. The restated stop rule of DES-MODERNIZATION-014 (`:1130-1132`) has no line numbers, so it stays valid.
   U-060's logs are history.
4. **Follow-up bug (SC-1, recommended after this story, low severity).** A cancelled run now ends through the
   broad catches:
   - backup: `BackupWorker.kt:168` logs "unrecoverable error" at error level and returns `Result.failure(<message>)`;
   - restore: `RestoreWorker.kt:348` and `:433` log "IllegalStateException (possible memory)" and return
     `Result.failure("illegal_state")`, skipping `updateAllThreadsIfAnySmsRestored`.

   This is the same path a `setProgress` cancellation already takes in production. The new tests do not assert
   the `Result` type, so that bug's fix (rethrowing `CancellationException` ahead of each broad catch) does not
   have to edit them.

   A1: the pattern is the **swallowed `CancellationException`** anti-pattern (2.0: kotlinx.coroutines guide,
   "Cancellation and timeouts"; detekt `exceptions` rule set, `SwallowedException`). The follow-up bug should:
   - write its criteria against CNTR-MODERNIZATION-004 `:284` ("Non-retryable failures return `Result.failure()`
     and surface as `SchedulerState.Failed(cause)`") and `:287` ("Cancellation is terminal, not failure": a
     structured-cancelled job emits `SchedulerState.Cancelled`, distinct from `Failed`);
   - record that "the work stays CANCELLED although the worker returns `Result.failure`" rests on library
     behaviour, not on a contract guarantee (contract-verification O-1).

   Of that behaviour, this session read that `onStopped()` cancels the result future and that the future's
   listener cancels the job (L5b). It did not read whether WorkManager's worker wrapper discards a result set
   after that. Until the bug lands, the visible residue is the error-level log line.
5. **For the Kotlin, coroutines and WorkManager increments (S2-I5, S4-C, S4-W).** Re-run this story's six
   mutations (I6 table), not only the tests (Scope Concern 3, ruled). The tests can fail only because, under
   work-testing 2.9.1, `setProgress` and `setForeground` return completed futures without a job check (L6). A
   later work-testing or WorkManager release that suspends there would let the unfixed code pass too, and only
   mutation (b) would show it.
6. **Commit.** The developer commits nothing. After the orchestrator's commit, the HEAD count equals the
   implementation log's end count.
7. **Contract line citations (A1, contract-verification O-2).**
   - CNTR-MODERNIZATION-004 cites `BackupWorker.kt:161-162` and `:430-436` (at its `:87`, `:131`, `:149` and
     `:280`).
   - CNTR-MODERNIZATION-007 cites `BackupWorker.kt:138`, `:310` and `:34`.
   - These were already stale at the base commit: `executeSkip` is called at `:187-188` and `inferBackupType` is at
     `:500`. E2 shifts every line below `:305` by one more.
   - No contract text becomes false.
   - Recommendation for the next contract amendment: cite by function name, not by line. No contract is edited by
     this story.

## 8. Contracts, Standards, Risks, Out of Scope

**CNTR-MODERNIZATION-004** (`approved`), rule 7 (`:279`: "`cancel` is structured. It MUST cooperatively cancel
the running coroutine (unwinding at the next suspension point) … not merely poll an `isCancelled()` flag") and
"Cancellation is terminal, not failure" (`:287`). The contract is read, not edited and not produced.
- The fix makes rule 7's mechanism real.
- Option 2 (polling) would violate rule 7 and is rejected (2.1).
- `:287` concerns the scheduler state that consumers observe. WorkManager keeps the work CANCELLED because
  `onStopped()` cancels the result future first (bug, Suggested Approach). The `Result.failure` returned inside
  the worker is item 4 of section 7 and does not contradict the contract at the port.
- No new component boundary: the story's `integration_contracts: []` is right. The seam is `internal`, inside
  one module (A1, contract-verification O-4: confirmed by that gate).
- A1, contract-verification O-3: rule 7 says "unwinding at the next suspension point". After the fix the loops
  also stop at the top of the next pass. In production `setProgress` may still be the first point (L20 (d)), and
  either way no point lies inside a pass, which is blocking IMAP work. The tests show the stricter, pass-boundary
  form under a harness where `setProgress` cannot cancel. There is no conflict with the rule and no contract
  change.

**Requirements and standards.**
- The story has `requirements: []` and `design_docs: []`.
- REQ-MODERNIZATION-015 AC-13 (as amended by CR-016, read at `:165-176`) governs the counts (I0, I8).
- REQ-MODERNIZATION-016 AC-8 (worker tests unmodified across the runtime upgrade) is why these tests are written
  so that a later story need not edit them.
- No REQ of `type: standard` bears on this change (U-066 plan section 12 found none, and this story touches no
  requirement).

**Risks**

A1 (expert-depth edge cases): each risk has a likelihood and an impact, with one-line grounds.

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|------------|--------|------------|
| R1 | Harness property L6 changes with a later work-testing release, making the tests vacuous | Medium, at S4-W: 2.12.0 replaces the `setProgress` await (bug, Suggested Approach); not at S2-I5 or S4-C, where WorkManager is unchanged | High: a regression of the check would pass silently | Section 7 item 5 (re-run the mutations); D5 here |
| R2 | `lateinit` job read before assignment | Low: the form is fixed in the text (`LAZY` + `join()`) | Low: an immediate `ERROR`, caught at I3 | 2.2; D6 |
| R3 | A JUnit 4 test method returning non-`Unit` | Low: every new test is written `= runBlocking<Unit> { … }` | Low: an initialisation error at I3, not wrong evidence | Text as written; D4 |
| R4 | The authoritative count inflated by "@Test" text in comments | Low: plan-review counted the annotation lines exactly | Medium: a wrong count in AC-11's evidence | C10; I2 check |
| R5 | JVM class-name clash of helper classes in the package | Low: names checked free (L19) | Low: a compile error at I3 | Nested or private names |
| R6 | Mutation left in place | Low: one edit per entry, restored at once | High: a gate, review or commit on mutated code | Write-ahead log, restore by copy, hash, diff-hash and porcelain checks, recovery note (I6) |
| R7 | The CRLF working tree is altered by an edit or a recovery | Low: Edit keeps the endings; recovery uses `--filters` (A1) | Low: whole-file diff noise; hashes would still match | `cp` restore; filtered `cat-file` checked byte-identical (I6) |
| R8 | The hygiene pattern flags the E2 annotation | Low: the sanitiser's form is used | Low: a false stop at I10 | I10 pattern |
| R9 | `backupCursors` visibility misused by a later production caller outside the class | Low: lint reports it | Medium: the seam becomes an API by accident | `@VisibleForTesting(otherwise = PRIVATE)`, as for U-053 (2.1, gain and cost) |

**Out of scope** (the story's list): SC-1 rethrow; pre-loop cancellability (SC-3); the IMAP dispatcher (SC-5);
the five comments that become true; BUG-021, BUG-023; S4; any user interface.

## 9. IC-4 — Device

No emulator or device step is part of this story. Every criterion is shown at unit level. The bug's Verification
Step 8 is optional and the owner's call. If the owner wants one, the orchestrator runs it, never a subagent and
never on the AVD `Pixel_10_Pro`, and it adds nothing to the criteria.

## 10. Constraints for the Developer (binding)

1. Run Gradle in the foreground, with the tool timeout set to 600000 ms on every call, and **never with
   `MSYS_NO_PATHCONV=1` set** (it breaks the wrapper's classpath). Scope that variable to adb-only shells; this
   story has none.
2. If the tool moves a run to the background by itself, wait for its completion notice. Never poll, and start no
   other command meanwhile.
3. No background or detached shells, no `&`, no `sleep` or polling loops, no helper agents for shell work, no
   heredocs for editing (Write and Edit tools only).
4. On "bash.exe … not found", or a process killed without a visible reason: stop and report (endpoint security).
   Do not work around it.
5. Raw logs and pristine copies stay in the work folder outside the repository. Only sanitised evidence enters
   `$W`. Never write an e-mail address, host name, user name or user-profile path into any repository file.
6. Take timestamps from `date -u +%Y-%m-%dT%H:%M:%SZ`; never type them.
7. Commit nothing. The orchestrator commits, with the product owner's permission.
8. Edit production code only as E1 to E6 say, plus the logged mutations, each restored and proven by blob hash,
   diff hash and porcelain.
9. No dependency, Gradle, build or verification-metadata change. No JaCoCo floor, `limit` or `minimum` edit. No
   `@Ignore`, no `Assume`. No edit to an existing assertion, test or helper (T2 and T3 insert only; T4 is comment
   only).
10. No test text may contain the string "@Test" except the annotations themselves.
11. No new file under `app/` other than T1.
12. Do not touch the other work in the checkout (A1, plan-review O-1). That is:
    - `sdlc/.skill-delegation-audit.log` and `sdlc/.skill-context.json`;
    - `sdlc/artifacts/build/sprints/sprint-024/**` (sprint 024 and the U-068 planning workspace, where a planner
      and its plan-gate reports write during this build);
    - any design, requirement, contract or change record;
    - the story file.

    The orchestrator may commit documents (never code) while you run. Changes outside this story's paths (`app`,
    `k9mail-vendored`, the build files, `$W`) are recorded if noticed and are never a reason to stop (I8, D15).
13. No emulator, no device, no `adb`.

## 11. Quality Attribute Coverage (ISO/IEC 25010)

| Characteristic | Finding |
|----------------|---------|
| Functional Suitability | Correctness: the three checks become real stop points (L5 shows why they were not). Each site is shown by its own test and its own two mutants; completeness is shown by the controls |
| Performance Efficiency | `currentCoroutineContext()` is an inline read of the continuation's context, with no allocation and no I/O. Unchanged per-pass cost. A cancelled backup now stops one pass earlier, so there is one fewer IMAP APPEND and one fewer watermark step |
| Compatibility | Co-existence and interoperability: no WorkManager, Hilt or constructor signature change (IC-1). The fix no longer depends on the deprecated member, so a later WorkManager that removes or raises it cannot change behaviour silently |
| Usability | Not applicable to this story's scope (no UI). The "canceling" button state now matches the loop's real stop one pass sooner |
| Reliability | Fault tolerance and recoverability are kept: `finally` cleanup runs (closeFolders asserted); the watermark advances only on a confirmed append (BUG-010; AC-1 (b)); the restore checkpoint is kept on cancellation (AC-2 (c), AC-3) for resumption |
| Security | Not applicable to this story's scope. No data, credential, trust or permission path changes; `internal` is module-scoped and annotated `otherwise = PRIVATE` |
| Maintainability | Testability: Route S adds one documented seam, the U-053 pattern. Modifiability: a name that cannot be shadowed replaces one that was. The mutations make the tests' discriminating power auditable at each later upgrade |
| Portability | Not applicable beyond the unit level. The fix is pure Kotlin/kotlinx-coroutines API, present and not deprecated in 1.7.3 and 1.11.0, with the same `ensureActive` bytecode (L7, verified in A1) |

## 12. Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| U-067 story | `sdlc/artifacts/build/sprints/sprint-023/U-067-worker-cancellation-checks-use-the-running-coroutine.md` | Read in full: DPs and rulings, AC-1 to AC-12, IC-1 to IC-4, tables, Technical Context, Scope Concerns |
| BUG-022 | `sdlc/artifacts/stories/BUG-022-worker-cancellation-checks-do-nothing.md` | Read in full; Suggested Approach |
| U-066 plan | `sdlc/artifacts/build/sprints/sprint-022/U-066/plan.md` | Model: sections 0, 1, 4 to 7, 10 to 12 and the verdict read; tool invocations |
| CNTR-MODERNIZATION-004 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` | Status `approved`; rule 7 `:279`; `:287` |
| REQ-MODERNIZATION-015 | `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-015-toolchain-and-test-tooling-latest-stable-minsdk-23.md` | AC-13 as amended (`:165-176`) |
| U-060 evidence | `sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/final-gates.log` | `:100-112`: the three warnings and the warning format |
| U-063 tools | `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools/` | `junit-xml.init.gradle`, `sanitize.py`, `junit_totals.py`, `junit_cases.py` headers |
| Workers | `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt`, `RestoreWorker.kt` | Read in full |
| Collaborators | `…/service/BackupCursors.java`, `BackupConfig.java`, `WorkerEngineFactory.kt`, `RestoreInsertInterceptor.kt`, `BackupType.java`; `…/mail/MessageConverter.java` (`:54-158`), `ConversionResult.java`, `Headers.java`, `…/mail/transport/MailTransport.java` (signatures), `MessageImportResult.java` (`:45-60`); `…/preferences/DataTypePreferences.java`, `Defaults.java`, `Preferences.java`; `…/mail/DataType.java` | Seams, defaults, fixture facts |
| Tests | `app/src/test/java/com/zegoggles/smssync/service/BackupWorkerWatermarkTest.kt`, `RestoreWorkerIntegrationTest.kt`, `RestoreWorkerCheckpointTest.kt` (read in full); `BackupWorkerTest.kt` (`:1-45`, `:222-238`); `…/mail/transport/MailTransportTestFactories.java`; `…/mail/transport/K9MailTransportPortMethodsTest.java` and `…/mail/MessageConverterTest.java` (mock usage) | Fixtures reused; counts |
| Build | `app/build.gradle:162-164`, `gradle/gradle-daemon-jvm.properties`, `./gradlew --version` | Mockito version, toolchain |
| Libraries (compiled) | work-runtime 2.9.1, work-testing 2.9.1, kotlinx-coroutines-core-jvm 1.7.3 and (A1) 1.11.0 from the local Gradle cache, read with `javap` in the planner's scratch folder | L5, L5b, L6, L7 |
| Plan-gate reports (A1) | `sdlc/artifacts/build/sprints/sprint-023/U-067/plan-review.md`, `contract-verification.md`, `expert-depth-check.md` | Read in full; Amendment A1 |
| Contract lines (A1) | CNTR-MODERNIZATION-004 `:284`, `:287` | Quoted in section 7 item 4 |
| `RestoreWorker.kt:369`, `RestoreCheckpointStore.kt:64` (A1) | `app/src/main/java/com/zegoggles/smssync/service/` | The sixth comment; `NO_CHECKPOINT = 0` |
| Plan template, standards | plugin cache `templates/build-artifacts/build-plan.md`, `knowledge/standards/expert-exceeding-depth.md` (headings and anti-patterns) | Frontmatter and sections |
| REQ and DES corpus | `sdlc/artifacts/requirements/modernization/`, `sdlc/artifacts/design/modernization/` | Not re-read in full. The story lists none; DES-MODERNIZATION-014's relevant lines are cited by the story and were not changed by this story |

## Verdict

**PASS.** The story's facts hold against the code at `4095db28` (and at `0d5755c4`, which changed no code), with
two corrections:
- the second caller of `backupCursors` (`:462`);
- the cursor close lying outside the Route S seam (declared narrowing, Scope Concern P-1; both accepted by the
  orchestrator's ruling).

Amendment A1 takes up every finding of the three plan gates. It changes no production edit, no test text and no
mutation row. It changes the mutation recovery command, the I8 scope of the records checks, rows D1, D6, D10 and
D15, the run count (12), and adds the grounding and edge-case text.

Every library fact the story and the bug left "not verified" is now verified from the compiled classes:
- the member's type: L5;
- `ensureActive` on a context with no job: L5;
- the `setProgress` and `setForeground` fast paths, and the test updaters: L6;
- `currentCoroutineContext` in 1.7.3: L7.

The fix is the ruled one, at exactly the three sites, with the imports changed in place and the Route S seam in
U-053's form. Each of the four cancellation tests:
- cancels a child job the test holds, from inside the fixture, with no time involved;
- asserts effects rather than the `Result` type;
- is matched by a control that uses the same harness.

The six mutations are derived per site, with the tests that must fail and the tests that must pass. AC-7 is shown
by a compile that is forced to run and is first proven, at I0, to see the three current warnings. Every probe has
a decision row, and "stop and report" is the outcome wherever no acceptable alternative exists.

## Phase Completion Report
---
story_id: "U-067"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-023/U-067/plan.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
