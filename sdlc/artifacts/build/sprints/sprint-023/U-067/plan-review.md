---
artifact_type: gate-result
story_id: "U-067"
gate: plan-review
gate_id: "plan-review"
checkpoint: plan-gate
verdict: PASS
iteration: 1
agent: "Solution Architect (separate instance, plan-review)"
timestamp: "2026-11-21T10:14:20Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-023/U-067/plan.md"
base_commit: "4095db286d52dc4a361abc03de078edfce857b9c"
blockers: 0
observations: 7
---

# Gate Report: gate-result — U-067

## Gate: plan-review @ plan-gate

## Verdict: PASS

## Summary

I read the story, the source bug and the plan in full. I checked the plan against the code itself and did not take its line numbers or quotations on trust.

**Base.** HEAD is `0d5755c4`. `git diff --stat 4095db28..HEAD -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat` is empty, so the code is the plan's base. Plan row D1 ("a later HEAD that changes nothing in those paths") covers this case. The HEAD blobs and the working-tree hashes of `BackupWorker.kt` and `RestoreWorker.kt` equal `BW_BLOB` and `RW_BLOB`. The authoritative count at HEAD is 746. The per-class `@Test` counts are 9, 8, 6, 13, 9 and 15, which sum to 60, as the plan's L18 states.

**Edits applied outside the repository.** I applied E1 to E6, T2a, T2b, T3a, T3b and T4 by script to copies of the five files in my session scratch folder, taking the code blocks verbatim from the plan. The script matched each `old_string` with the file's own CRLF endings.
- Every `old_string` occurs exactly once.
- The production diff is exactly the six lines AC-8 (b) names: `BackupWorker.kt` 57, 305 (which becomes 305-306) and 331 (which becomes 332); `RestoreWorker.kt` 66, 311 and 399.
- No `RestoreWorker.kt` line moves.

**Compile check, by hand.** No kotlinc is on PATH, so I read the patched test files and T1 against the real sources for types, imports, fixtures and signatures, without compiling. I found no error; the details are in the table below.

**Mechanism.** I read the compiled 2.9.1, 1.7.3 and stdlib 2.3.21 classes from the local Gradle cache with the JDK 25 `javap`, read-only. The claims hold (see "Mechanism claims").

**Behaviour.** I traced each new test, pass by pass, through the fixed code, the code at HEAD and each of the six mutants. Every row of the plan's 2.4 and I6 tables re-derives exactly.

**Verdict.** No finding would make the developer's run fail, give wrong evidence or leave a criterion unproven. Seven observations follow. The two worth putting into the developer brief are O-1 (the list of other work in the checkout is out of date, and a concurrent planner could trip I8's records check) and O-2 (the recovery command restores LF endings into a CRLF file).

## Findings

### Blockers

None.

### What I checked and found sound

**1. Edits and compile-in-principle (scratch copies, outside the repository).**

| Item | Checked against | Result |
|------|-----------------|--------|
| E1 to E6 | the files at HEAD | Each `old_string` is unique. I4's predicted hunks are right: `@@ -57 +57 @@`, `@@ -305 +305,2 @@` and `@@ -331 +332 @@` for BW, and `@@ -66 +66 @@`, `@@ -311 +311 @@` and `@@ -399 +399 @@` for RW. The predicted numstat `4 3` and `3 3` is also right |
| E2 exposure | `WorkerEngineFactory.kt:104-118` (an `internal` function already returns the package-private `CalendarSyncer`, and its KDoc says `public` would not compile) | An `internal` parameter of that type compiles by the same rule. The two callers are inside the class (`BackupWorker.kt:255`, `:462`) |
| T1 `BackupConfig(...)`, `BackupCursors()`, `add(...)` | `BackupConfig.java:147-153` (package-private; non-null transport; non-empty types), `BackupCursors.java:48`, `:51` | The argument order and types match. `ContactGroup(long)` is public (`ContactGroup.java:14`) and `BackupType.MANUAL` exists |
| T1 `backupCursors(...)` call | `BackupWorker.kt:305-312` | Six arguments in order. `null` for `CalendarSyncer?` |
| T1 transport | `StubMailTransport` (`BackupWorkerWatermarkTest.kt:355-396`: public; `appendMessagesBehavior` runs before the return; the return falls back to `result.getMaxDate()`) | `MailTransport` is a Java interface with no default methods, so `object : MailTransport by stub` delegates all eight members. `checkSettings` and `openFolder` (`createFolderHandle()`) are safe |
| T1 converter | `Mockito.mock(Class, Answer)`; `ConversionResult.add` reads `Headers.DATE` (`public static final`, `Headers.java:14`); `MAX_SYNCED_DATE = -1` (`DataType.java:87`) | Each row's result has `maxDate = ROW_DATES[i]`, which is greater than -1. The mock answers by the cursor's current position (0 to 3) |
| T1 helpers and keys | `TestableBackupWorkerFactoryWithTransport(MailTransport)` (`BackupWorker.kt:609`); `PROGRESS_KEY_BACKED_UP`, `PROGRESS_KEY_STATE`, `STATE_FINISHED` (companion consts) | All resolve. Every import is used |
| T2 | `RestoreWorkerIntegrationTest.kt`: fields `checkpointStore`, `fakeProvider` (`:66-67`); `buildWorkerWithTransport` (`:381`); `createSmsItems` (`:412`); `ImapRestoringTransport` constructor and lambda types (`:482-486`); `MailTransportTestFactories.createHandle(String): MailMessageHandle` (public static); `MessageImportResult(String, DataType, ContentValues)` (public, `failed = false`); `RestoreWorker.RESTORE_WORK_NAME` | All resolve. `MailTransport`, `ListenableWorker`, `DataType`, `MessageImportResult` and `MailTransportTestFactories` are already imported. `checkpointStore.read` is `suspend` and is called inside `runBlocking` |
| T3 | `RestoreWorkerCheckpointTest.kt`: `buildWorker(store, interceptor)` (`:99-107`); `countSmsRows` (`:91`); `preferences`, `context`, `checkpointStore` fields; `RestoreInsertInterceptor.afterInsert` is `suspend` (`RestoreInsertInterceptor.kt:39`); `RestoreCheckpointStore.NO_CHECKPOINT` (`RestoreCheckpointStore.kt:64`) | All resolve. The nested `private class CancellingInterceptor` overrides correctly |
| Names | `git grep` under `app/` for `BackupWorkerLoopCancellationTest`, `CancellableImapRun`, `CancellingInterceptor`, `class BackupRun` | No hit |
| `@Test` text | the four new blocks | The lines that contain `@Test` are exactly the annotation lines: 2, 2 and 3, and none in T4. So the count goes from start to start + 7. No new block contains `@Ignore` or `Assume` |
| T4 | `BackupWorkerTest.kt:27-30`; `ic2_workManager_cancelsBackupWorkerRequest` exists (`:223`) | Four lines become four lines, comment only, giving one `@@ -27,4 +27,4 @@` hunk. AC-9's content requirements are met |

**2. Mechanism claims.**

| Claim | Status | Evidence |
|-------|--------|----------|
| Bare `coroutineContext` inside the workers resolves to the inherited `CoroutineWorker` member, a dispatcher | **verified** | `javap -p androidx.work.CoroutineWorker` (2.9.1) shows `private final CoroutineDispatcher coroutineContext`, `public CoroutineDispatcher getCoroutineContext()` and the `getCoroutineContext$annotations()` deprecation holder. The real job is a separate field, `CompletableJob job`. The compiler's own warnings name that member at exactly the three sites (`sprint-017/U-060/evidence/final-gates.log:108-110`: "'val coroutineContext: CoroutineDispatcher' is deprecated"). So the member beats the explicit import, as Kotlin's resolution order requires |
| `ensureActive()` on a context without a `Job` returns silently | **verified** | `JobKt__JobKt.ensureActive(CoroutineContext)` (1.7.3) is `get(Job.Key)`, then a null check, then `return`. `CoroutineDispatcher.get` delegates to `ContinuationInterceptor.DefaultImpls.get` (stdlib 2.3.21). For a key that is not an `AbstractCoroutineContextKey`, that returns the element only for the `ContinuationInterceptor` key and otherwise returns `null`. So `dispatcher[Job]` is null |
| `setProgress` and `setForeground` do not check cancellation under the test harness | **verified** | `CoroutineWorker.setProgress` and `setForeground` (2.9.1) call `isDone()` and, when it is true, return `get()`. Only the not-done branch builds a `CancellableContinuationImpl`. `ListenableWorker.setProgressAsync` passes straight to `WorkerParameters.getProgressUpdater().updateProgress`. `TestProgressUpdater.updateProgress` and `TestForegroundUpdater.setForegroundAsync` (work-testing 2.9.1) return a `SettableFuture` already `set(...)`. `TestListenableWorkerBuilder` constructs both |
| `currentCoroutineContext()` exists in 1.7.3 and is not deprecated | **verified** | `CoroutineScopeKt.currentCoroutineContext(Continuation)` is present, and `javap -v` shows no deprecation attribute on it. D20 still guards the I7 compile |
| No other suspension point in the loops that would stop the unfixed code | **verified** | `grep` finds no `withContext`, `yield`, `delay`, `await` or `Dispatchers` in either worker. `InMemoryCheckpointStore` (`RestoreWorkerCheckpointTest.kt:487-500`) and `RestoreInsertInterceptor.NoOp` never suspend |
| `:app:compileDebugKotlin --rerun --no-build-cache` makes the Kotlin compiler recompile the module and print its warnings | **plausible, not verified** (no Gradle allowed) | `--rerun` gives the task non-incremental input changes, so the Kotlin Gradle plugin rebuilds in full. The plan proves the method at I0 step 3 (`worker-coroutineContext=3`, `task-skipped=0`) before relying on it at I7, and D2 stops on failure. It cannot give false evidence |

**3. Each test bites, deterministically.** All harness work runs on `runBlocking`'s single-thread event loop, with no `delay`, no sleep and no second thread. The child is cancelled by `workJob.cancel()` on a job the test holds, from inside a blocking fixture callback. `LAZY` plus `join()` makes sure the `lateinit` is assigned before the body runs. AC-4 uses `UNDISPATCHED` with a self-cancel and no suspension before the first check. In every case the parent is not cancelled, so `join()` and `runBlocking` return normally. What each case does:
- **AC-1, fixed code.** The second append cancels. Its watermark step (`ROW_DATES[1]`) and its `setProgress` complete. The check at the top of pass 3 throws, the `finally` runs `closeFolders` once, and the exception ends the child as cancelled, so `run.result` stays null.
- **AC-1, unfixed code.** All four passes run. Every assertion before `appends` holds (`isCancelled` is true, because `cancel()` was called), so the test fails with `appends` expected 2, was 4.
- **AC-2, fixed code.** The check throws in pass 3. The exception is caught at `RestoreWorker.kt:348` and `executeRestore`'s `finally` (`:277`) runs. Result: two rows, checkpoint 1, close count 1.
- **AC-2, unfixed code.** Four imports and four rows; the checkpoint is cleared at `:340`.
- **AC-3, fixed code.** After `afterInsert(2)`, the check at the top of pass 4 throws and is caught at `:433`. Result: three rows, `[0, 1, 2]`, checkpoint 2.
- **AC-3, unfixed code.** Five rows; the checkpoint is cleared.
- **AC-4, fixed code.** Zero rows.
- **AC-4, unfixed code.** Five rows.
- **Controls.** All three pass on both forms.

**AC-6 rows, re-derived from the test code.** Each test enters exactly one loop:
- test 1: `backupCursors`;
- tests 3 and 4: `runImapRestoreLoop`, through `doWork` and then `executeRestore`;
- tests 5 to 7: `executeRestoreWithValues`.

Mutation (a) puts the bare name back. It compiles through the member, because the kept `ensureActive` import applies to a `CoroutineContext` receiver. Mutation (b) is `if (false) …`, which compiles. Both make the site a no-op.

Result for each pair: M-1a/b fail test 1 only. M-2a/b fail test 3 only. M-3a/b fail tests 5 and 6 only. Every other NEW3 case passes (2 + 11 + 18 = 31 cases: 30, 30 and 29 must pass), and no existing case in NEW3 cancels a loop. The table at `plan.md:812-819` is right in every cell. Each new `old_string` (the fixed call at site 1, and the three-line forms with their context lines at sites 2 and 3) is unique in the file after the fix.

**4. Criterion coverage.**
- AC-1 to AC-5 are tests 1 to 7: red at I3 and green at I5. The cursor-close half of AC-1 (c) is declared not observable, as the orchestrator ruled (story `:208`).
- AC-6 is I6.
- AC-7 is I0 step 3 against I7.
- AC-8 is the I4 and I8 hunks, numstat, name list and diff hash. The list "limited to" equals E1 to E6 exactly: two imports replaced in place, three call lines, and the E2 seam. The `kotlinx.coroutines.ensureActive` imports (`:54`, `:63`) are untouched.
- AC-9 is T4 plus the I2 `bwt-non-comment-end` check.
- AC-10 is I3, I5 and I8. I2's removed-line count of 0 for `RIT` and `RCT` proves that no existing assertion was edited.
- AC-11 is the I0 and I8 gates (with `assembleRelease`), JDK 25 proven by `U063-DAEMON-JVM` and `toolchainVersion=25`, and the skipped count. The count rule is applied right: start count with its commit at I0, then start + 7 in the working-tree `--untracked` form at I2 and I8, with the Gradle total checked as `<n>+7`.
- AC-12 is I8's build-diff, `vm-exit` and the `@Ignore`/`Assume` checks.
- IC-1 to IC-4 are covered by I4, I8, the gate and section 9.

The I8 determinism diff is valid. The only test classes whose names contain `BackupWorker` or `RestoreWorker` are the seven classes (`git ls-files app/src/test`), and `junit_cases.py` prints no timings.

**5. Decision rows.** Every probe has a row. Wherever no acceptable alternative exists, the row says stop and report: D1, D2, D3, D5, D9, D13, D15, D19 and D20. No row allows a criterion, a test or an assertion to be dropped: D4 keeps every assertion, D7 stops if a control could pass only by changing an AC's numbers, and D12 never counts an invalid mutant.

**6. Developer constraints** (section 10) are complete against the brief:
- foreground Gradle with a 600000 ms timeout;
- never `MSYS_NO_PATHCONV=1` (and `unset` in 4.0);
- wait for a run the tool moves to the background, with no polling (C2, D18);
- no background or detached shells and no heredocs (C3);
- stop on "bash.exe not found" or an unexplained kill (C4, D19);
- commit nothing (C7);
- no build, dependency or metadata change, no floor edit, no `@Ignore` or `Assume` (C9);
- leave other work alone (C12, but see O-1).

Hygiene is also covered. Raw logs and the pristine copies stay in the work folder. The evidence passes through `sanitize.py`, which replaces repository, home, user and host and refuses e-mail-like tokens. I10's scan uses the sanitiser's own e-mail form, so the E2 annotation does not produce a false hit.

### Observations (non-blocking)

**O-1 (medium): the "other work in the checkout" list is out of date, and a concurrent planner could trip I8's records check.**
- **Where:** `plan.md:23-25` and constraint C12 (`plan.md:1053-1055`); the I8 line `git status --porcelain -- sdlc/artifacts/design sdlc/artifacts/requirements sdlc/artifacts/build/change-records; echo "records-end"   # nothing` (`plan.md:862`).
- **What:** the three U-068 to U-070 drafts were committed at `1c8303e2`. They now live under `sdlc/artifacts/build/sprints/sprint-024/`, and U-068 is being planned in this checkout (`sprint-024/U-068/`, commit `0d5755c4`). If that planner writes a record under one of those three paths during the build, D15 stops the developer for a change that is not this story's.
- **Fix:** in C12, replace the drafts entry with "`sdlc/artifacts/build/sprints/sprint-024/**` (sprint 024 and the U-068 planning workspace)". At I8, add: "a line there that this story did not write is recorded in the log with its path and is not a D15 stop. Only a file the developer wrote is."

**O-2 (low): the recovery command restores LF endings into a CRLF file.**
- **Where:** the I6 recovery note, "`git cat-file blob <BW_NEW|RW_NEW> > <path>`" (`plan.md:792`).
- **What:** `git hash-object -w` stores the normalised (LF) content. `cat-file blob` writes it back unconverted, so the blob-hash check still passes but the working-tree file silently turns LF. The normal path (`cp` from the pristine copy) is unaffected.
- **Fix:** use `git cat-file --filters --path=<path> <BW_NEW|RW_NEW> > <path>`, which applies the checkout conversion.

**O-3 (low): I1 has no decision row, and D6 does not restate D4's "keep every assertion".**
- **Where:** I1 (`plan.md:717-720`); D6 (`plan.md:926`); D10 (`plan.md:930`).
- **Fix:** in D10, change "I4:" to "I1 or I4:". In D6, after "Correct the test text", add "(fixture and harness only; every assertion of 3.2 kept, as in D4)".

**O-4 (low): the Gradle invocation count is 12, not 11.**
- **Where:** `plan.md:31` and `plan.md:680`. The itemised list sums to 1 + 1 + 1 + 1 + 6 + 1 + 1 = 12.
- **Fix:** write 12 in both places. This has no effect on any step.

**O-5 (info): AC-4's checkpoint assertion cannot bite on its own.**
- **Where:** test 6 (`plan.md:564`); `RestoreCheckpointStore.kt:64` (`NO_CHECKPOINT = 0`).
- **What:** a checkpoint written for index 0 reads the same as no checkpoint. On the unfixed code the checkpoint is cleared and also reads 0. So only `countSmsRows() == 0` distinguishes the fix. That is enough for AC-4, because a checkpoint is written only after an insert (`RestoreWorker.kt:461-472`), so zero rows means no write.
- **Fix:** none needed. QA should read AC-4's "no checkpoint is written" as carried by the rows assertion together with that ordering.

**O-6 (info): section 7 items 1 and 2 are now ruled.**
- **Where:** `plan.md:151` and `plan.md:961-966`.
- **What:** the orchestrator accepted P-1 and both corrections (story `:208`).
- **Fix:** in I9, the developer cites the ruling instead of "asks for a ruling". No plan change is required.

**O-7 (info): the test-class line of the Affected Code table no longer applies.**
- **Where:** story Affected Code row "`BackupWorkerWatermarkTest.kt` or a new test file".
- **What:** the plan takes the new-file branch (T1) and leaves `BackupWorkerWatermarkTest.kt` unedited, which is consistent with AC-10. The I8 status list (`plan.md:864-870`) correctly omits that file.
- **Fix:** none. Noted so that a reviewer does not expect a diff there.

## Phase Completion Report
---
story_id: "U-067"
phase: "plan-gate/plan-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-023/U-067/plan-review.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
