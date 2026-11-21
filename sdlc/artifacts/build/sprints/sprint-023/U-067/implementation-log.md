---
artifact_type: implementation-log
story_id: "U-067"
gate_id: "implementation"
checkpoint: "implementation"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-11-21T11:10:00Z"
iteration: 1
base_commit: "c9770c303690a95b6200e82c73316ba8ce2bb4e8"
files_changed: 5
files_created: 1
tests_added: 7
tests_passing: 753
---

# Implementation Log: U-067 - Make the workers' three cancellation checks observe the running job

Plan: `sdlc/artifacts/build/sprints/sprint-023/U-067/plan.md` (iteration 1, Amendment A1), followed step by step. Evidence is under `sdlc/artifacts/build/sprints/sprint-023/U-067/evidence/`. Raw logs and the pristine copies of the fixed production files were kept in a work folder outside the repository. Nothing was committed, staged or pushed. No emulator or device was used.

`START` is `c9770c303690a95b6200e82c73316ba8ce2bb4e8` (the HEAD at I0; documents only since the plan's base `4095db28`: `git diff --stat 4095db28 HEAD -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat` printed nothing, so row D1's "descendant" clause applied). HEAD moved to `fb707d7e` (documents) during the run; `git diff --name-only START HEAD -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat` is empty (`evidence/I8-checks.txt`, `no-code-commit-end`).

## Result in one paragraph

The three checks now read `currentCoroutineContext().ensureActive()` (`BackupWorker.kt:332`, `RestoreWorker.kt:311`, `:399`); each file's `import kotlin.coroutines.coroutineContext` was replaced in place by `import kotlinx.coroutines.currentCoroutineContext`; `backupCursors` is `internal` with `@VisibleForTesting(otherwise = PRIVATE)` (Route S). Seven tests were added (four cancellation tests, three controls) and the one false Javadoc in `BackupWorkerTest.kt` was corrected (comment only). The red run failed exactly the four cancellation tests; the green run passed all 67 cases of the seven worker classes; all six mutations were detected with the expected failing sets; the forced Kotlin compile shows the three `coroutineContext` warnings at baseline and none after. Final gate: `exit=0`, `files=79 tests=753 failures=0 errors=0 skipped=2`, JaCoCo passes, no floor edited.

## Contract Adherence

The story lists no `integration_contracts` (`integration_contracts: []`; plan section 8, contract-verification O-4). `CNTR-MODERNIZATION-004` rule 7 (`:279`) is read, not edited and not produced: the fix makes its cooperative-cancellation mechanism real (`BackupWorker.kt:332`, `RestoreWorker.kt:311`, `:399`). No contract, requirement, design or change record was edited.

## Capabilities Inventory (existing code that was changed)

| Capability of the old code | Status | Where |
|---|---|---|
| The check at the top of the backup pass, called once per pass | RETAINED, same position, now observing the running job | `BackupWorker.kt:332` |
| The check at the top of the IMAP-restore pass and of the test-facing restore pass | RETAINED, same positions, now observing the running job | `RestoreWorker.kt:311`, `:399` |
| `backupCursors` behaviour: LOGIN/CALC progress, `checkSettings`, per-pass convert, append and watermark step, calendar sync, `setProgress`, `STATE_FINISHED`, `closeFolders` in the `finally` | RETAINED unchanged (only the modifier and one annotation line changed) | `BackupWorker.kt:305-306` and body |
| Both production callers of `backupCursors` (`fetchAndBackupItems`, auth-retry in `handleAuthError`) | RETAINED, same class, compile unchanged (full gate) | `BackupWorker.kt:255` and `:462` area (call text untouched; the two hunks below `:305` shift only by one line) |
| `kotlinx.coroutines.ensureActive` import | RETAINED | `BackupWorker.kt:54`, `RestoreWorker.kt:63` |
| Caught types and returned results (`Result.failure` on the broad catches) | RETAINED unchanged (SC-1 out of scope) | no hunk touches a catch (I4, I8 hunks) |
| `finally` cleanup: `closeFolders()` and `cursors?.close()` | RETAINED unchanged | `BackupWorker.kt:280`, `:378`; `RestoreWorker.kt:277` |
| Import `kotlin.coroutines.coroutineContext` (unused once the calls changed) | INTENTIONALLY REMOVED (replaced in place; AC-8 (c)) | `BackupWorker.kt:57`, `RestoreWorker.kt:66` |
| The private visibility of `backupCursors` | INTENTIONALLY REMOVED (DP-2 Route S; AC-8 (b)) | `BackupWorker.kt:306` |
| False Javadoc of `BackupWorkerTest.kt` (AC-5 verified "at the WorkManager level", loops "unwind") | INTENTIONALLY REMOVED, replaced (DP-1 ruled in; AC-9) | `BackupWorkerTest.kt:27-30` |

No item is a REGRESSION.

## Runtime Claims

**Claim:** When the job of the coroutine running the backup loop, the IMAP-restore loop or the test-facing restore loop is cancelled in the middle of a pass, the loop stops at the top of the next pass (the pass in flight completes; no further append, import or insert happens).
**Class:** crash/interrupt resilience
**Probe design:** Unit tests under Robolectric, each running the real loop in a child coroutine it holds (`launch(start = LAZY)`, then `join()`), cancelling that child from inside the fixture callback (the transport's second append, the second `importMessageBody`, `afterInsert(2)`) with no time involved. Under the work-testing harness `setProgress` is a completed future (plan L6), so the loop's own check is the only stop point. Falsifiability: the red run (fix absent) fails all four cancellation tests with `expected 2 but was 4`, `expected 2 but was 4`, `expected 3 but was 5` and `expected 0 but was 5`; six mutants (old binding, check deleted) at the three sites are each detected, and the three controls (same harness, no cancel) pass in every run. Topology and failure-mode limit: this is the in-process coroutine path, not WorkManager's own stop path.
**Probe command + raw output:** `evidence/I3-red.txt`, `evidence/I3-red-cases.txt`, `evidence/I3-red-message.txt` (fix absent: 67 cases, 4 fail, 63 pass); `evidence/I5-green.txt`, `evidence/I5-green-cases.txt` (fix present: 67 cases, 0 fail); `evidence/mutation-log.md` with `evidence/mutations/M-*.txt`, `M-*-cases.txt`, `M-*-message.txt` (six mutants, each failing set as planned). The Gradle invocation form is `./gradlew -I <init script> --console=plain :app:testDebugUnitTest --tests <classes>`, run in the foreground.

**Claim:** After the fix, the Kotlin compiler no longer reports the deprecated `coroutineContext` binding for either worker.
**Class:** none of the five classes (build output claim), recorded here for completeness
**Probe design:** `:app:compileDebugKotlin --rerun --no-build-cache` really runs the compiler (`task-line=1 task-skipped=0`); the baseline run before any edit proves the method sees the three warnings.
**Probe command + raw output:** `evidence/I0-kotlinc.txt` and `I0-kotlinc-summary.txt` (`warnings=9`, `worker-coroutineContext=3`: `BackupWorker.kt:331:17`, `RestoreWorker.kt:311:21`, `:399:21`); `evidence/I7-kotlinc.txt` and `I7-kotlinc-summary.txt` (`warnings=6`, `worker-coroutineContext=0`, no line for either worker file, none naming `currentCoroutineContext`).

**Claim (not verified):** In production, WorkManager's own stop (its worker wrapper calling `onStopped()`) cancels the loop through the same job.
**Escape-hatch justification:** Needs a running WorkManager with a real executor and a real stop, that is a device or emulator run; IC-4 and the delegation exclude any device. No narrower probe exists in the unit harness: the tests call the worker directly, not through `startWork()`. The plan (L5b) read `startWork`, `onStopped` and the future listener from compiled 2.9.1 classes, but not the caller of `onStopped()`. This claim is explicitly UNVERIFIED here and is not reported as verified.

## Steps

All times are UTC clock readings (`date -u`). Decision rows used are named; "none" means no row fired.

### I0 - Baseline (2026-11-21T10:22:32Z to 10:54Z; evidence `I0-baseline.txt`, `I0-gates.txt`, `I0-gates-junit-totals.txt`, `I0-kotlinc*.txt`)
- Step 1: HEAD `c9770c30`, branch `sdlc/modernization-plan`; nothing dirty in `app`, `k9mail-vendored` or the build files; the HEAD blobs and the working-tree hashes of the two workers equal `BW_BLOB` `5c554ec3bf737383f5a8717fdfc518e5eac33993` and `RW_BLOB` `871ec7c764be5f9557fdc25ec0683cb835cfeac7`; `toolchainVersion=25`; new file absent.
- **The count at the start of this story: 746, taken at `c9770c30` (the documents-only descendant of `4095db28`).** `@Ignore` lines at HEAD: 6 (the authoritative-command pattern also counts comment mentions; the same command at the end gives 6 again).
- Step 2, gate G: `exit=0`, JDK `25.0.5.1`, `files=78 tests=746 failures=0 errors=0 skipped=2` (4m 1s). Start Gradle total 746, start skipped 2.
- Step 3, KC baseline: `task-line=1 task-skipped=0 warnings=9 worker-coroutineContext=3`; the three lines are `BackupWorker.kt:331:17`, `RestoreWorker.kt:311:21` and `:399:21`.
- Row D1: not triggered (a descendant that changes nothing in the story's paths; `START` recorded). Row D2: not triggered.

### I1 - Route S seam (E2; evidence `I1-seam.txt`)
Hunk `@@ -305 +305,2 @@`. File still fully CRLF (634 of 634 lines). Row D10: not triggered.

### I2 - Tests (evidence `I2-tests.txt`)
T1 written; T2a, T2b, T3a, T3b and T4 applied exactly as in the plan. `git diff --stat`: `BackupWorkerTest.kt` 8 changed lines (4 + 4, comment lines only; one hunk `@@ -27,4 +27,4 @@`; non-comment changed lines 0), `RestoreWorkerCheckpointTest.kt` +93, `RestoreWorkerIntegrationTest.kt` +91; removed lines in those two: 0. Working-tree count with `--untracked`: 753 (start + 7). `BackupWorkerLoopCancellationTest.kt`: 2 `@Test`, 0 `@Ignore` or `Assume`. Row D4: not triggered (the check passed first time).

### I3 - Red run (2026-11-21T10:29:38Z; evidence `I3-red.txt`, `I3-red-cases.txt`, `I3-red-message.txt`)
Fix absent, seam and tests present; the seam and all tests compiled first time (no D3, no D4). 67 cases, **4 failed**, 63 passed: `backupCursors_cancelledAfterSecondAppend_stopsAtNextPass` (expected 2, was 4), `doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass` (expected 2, was 4), `executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass` (expected 3, was 5), `executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing` (expected 0, was 5), each a Truth `ComparisonFailureWithFacts`. The three controls and the 60 existing cases passed. This equals plan 2.4 and I3's expectation exactly. Rows D5 to D9: none triggered.

### I4 - Production fix (E1, E3 to E6; evidence `I4-production.txt`)
Files changed: `BackupWorker.kt` then `RestoreWorker.kt`. Hunks: BW `@@ -57 +57 @@`, `@@ -305 +305,2 @@`, `@@ -331 +332 @@`; RW `@@ -66 +66 @@`, `@@ -311 +311 @@`, `@@ -399 +399 @@`. numstat `4 3` and `3 3`. `old-name-exit=1` (IC-3). `ensureActive()` at exactly the three call lines plus the five comments. Both files still fully CRLF (634/634, 787/787). Saved copies in the work folder; `BW_NEW` `eb535f5c9c384e639f5077ce8ed3bd0ad7af350d`, `RW_NEW` `ffa9c57e90761dc56a5967e06b303e2d9080f81b`, `PROD_DIFF` `97045c8341925a9d34cc0f4f4560269b3d237bd1`. Row D10: not triggered.

### I5 - Green run (2026-11-21T10:33:25Z; evidence `I5-green.txt`, `I5-green-cases.txt`)
`exit=0`, 67 cases, 67 pass, 0 fail, 0 error, 0 skip. Row D11: not triggered.

### I6 - Mutations (evidence `mutation-log.md`, `mutations/`)
Six entries, each with a write-ahead `APPLYING` line (clock time, file, hash before), one Edit, one NEW3 run (31 cases), restore by copy from the pristine file, the clean check (hashes equal `BW_NEW` and `RW_NEW`, diff hash equals `PROD_DIFF`, porcelain lists exactly the two worker files) and a `restored` line. Results against the plan rows:

| Id | Failing of 31 | Failing tests | Plan row | Verdict |
|----|---------------|---------------|----------|---------|
| M-1a | 1 | `backupCursors_cancelledAfterSecondAppend_stopsAtNextPass` (2 expected, 4 was) | matches | detected |
| M-1b | 1 | same | matches | detected |
| M-2a | 1 | `doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass` (2 expected, 4 was) | matches | detected |
| M-2b | 1 | same | matches | detected |
| M-3a | 2 | `executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass` (3 expected, 5 was); `executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing` (0 expected, 5 was) | matches | detected |
| M-3b | 2 | same two | matches | detected |

No mutation failed to compile (D12 not used); none was undetected (D13 not used); no run failed more or fewer tests than its row (D14 not used). The (a) mutants leave the now-unused import `kotlinx.coroutines.currentCoroutineContext` in place, which compiles. Every entry has its `restored` line.

### I7 - AC-7 compile (2026-11-21T10:53:02Z; evidence `I7-kotlinc.txt`, `I7-kotlinc-summary.txt`)
`task-line=1 task-skipped=0 warnings=6 worker-coroutineContext=0`. The lines counted for `BackupWorker.kt` and `RestoreWorker.kt`: none (0 lines). The six remaining warnings are in `MainViewModel.kt`, `SmsDefaultRoleHelper.kt` (two), `WorkManagerScheduler.kt`, `SharedPreferencesCheckpointStore.kt` and `WorkerEngineFactory.kt`, so the compiler really ran. Baseline 9 warnings (3 on the workers) against 6 now: the three worker warnings are gone and nothing was added. No warning names `currentCoroutineContext`. Rows D2, D20: not triggered.

### I8 - Final gate and checks (2026-11-21T10:54:33Z to 11:01:51Z; evidence `I8-gates.txt`, `I8-gates-junit-totals.txt`, `I8-seven-cases.txt`, `I8-checks.txt`)
- I6 clean check passed before the gate (hashes `BW_NEW`, `RW_NEW`, diff `PROD_DIFF`, porcelain two worker files).
- Gate G: `exit=0`, JDK `25.0.5.1`, `BUILD SUCCESSFUL in 7m 14s`, `files=79 tests=753 failures=0 errors=0 skipped=2`; JaCoCo passed; no floor, `limit` or `minimum` was edited.
- Determinism: `diff I5-green-cases.txt I8-seven-cases.txt` gave exit 0 (the same 67 cases and results).
- Checks (`I8-checks.txt`): working-tree count with `--untracked` 753; count at `HEAD` still 746 (nothing committed); status of `app k9mail-vendored` is the six expected lines (five ` M`, one `??`); name-only list is the two workers; the six hunks of I4; production diff hash `PROD_DIFF`; the three cancel-path files not in the diff (`ic2-end` empty); `ic3-old-name-exit=1`; three call lines plus five comments; empty build-file diff; `vm-exit=0`; `@Ignore` count 6 (equal to I0's 6); added `@Ignore` or `Assume` lines 0 and in the new file 0; `no-code-commit-end` empty; `git status --porcelain -- <workspace>` lists only `evidence/` (my files; the plan, the gate reports and the story are already committed).
- Outside the story's paths, noticed and recorded by path only (not a stop): HEAD moved from `c9770c30` to `fb707d7e` (documents; no path under `app`, `k9mail-vendored` or the build files); `sdlc/.skill-context.json` was untracked at the start.
- Row D15: not triggered.

### I9 - Log
This file, written after I8. P-1 is ruled (story Implementation Notes, "Ruling on the plan's scope concern P-1", 2026-11-21): the cursor-close half of AC-1 (c) is declared not observable through the seam (the close is in `fetchAndBackupItems`'s `finally`, `BackupWorker.kt:279-281`, outside `backupCursors`); the test asserts `closeFolders` exactly once; QA judges the cursor half by reading the code path (not edited; its body is non-suspending, plan L20 (b)).

### I10 - Hygiene scan
Last line below.

## Files

Modified (production): `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` (E1 import, E2 seam, E3 call), `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt` (E4 import, E5 and E6 calls).

Modified (tests): `app/src/test/java/com/zegoggles/smssync/service/RestoreWorkerIntegrationTest.kt` (three imports, two tests and the helper block; no existing line changed), `app/src/test/java/com/zegoggles/smssync/service/RestoreWorkerCheckpointTest.kt` (four imports, the interceptor class and three tests; no existing line changed), `app/src/test/java/com/zegoggles/smssync/service/BackupWorkerTest.kt` (Javadoc lines 27-30 only, AC-9).

Created: `app/src/test/java/com/zegoggles/smssync/service/BackupWorkerLoopCancellationTest.kt`; the evidence files under `sdlc/artifacts/build/sprints/sprint-023/U-067/evidence/` (named in each step); this log. The story's `## Implementation Notes` gained developer entries; its frontmatter is untouched.

`BackupWorkerWatermarkTest.kt` has no diff (the plan took the new-file branch of the story's Affected Code row; plan section 7 item 2).

## Tests

Added (7; the count at the start 746 to 753): in `BackupWorkerLoopCancellationTest`: `backupCursors_cancelledAfterSecondAppend_stopsAtNextPass` (AC-1), `backupCursors_notCancelled_appendsEveryRow` (AC-5 control); in `RestoreWorkerIntegrationTest`: `doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass` (AC-2), `doWork_imapRestore_notCancelled_restoresEveryItem` (AC-5 control); in `RestoreWorkerCheckpointTest`: `executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass` (AC-3), `executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing` (AC-4), `executeRestoreWithValues_notCancelled_insertsEveryItem` (AC-5 control).

None deleted, none edited, none `@Ignore`d, none guarded by `Assume`. No existing assertion, test or helper was touched (the two edited test classes received insertions only; `BackupWorkerTest.kt` received a comment change only).

Counts, relative to "the count at the start of this story" (746 at `c9770c30`): the authoritative command in the working-tree `--untracked` form gives **753** = 746 + 7 (the CR-016 form; nothing is committed, so the same command at `HEAD` still gives 746). The Gradle total for the final gate is **753** (`tests=753`), equal. The 67 cases of the seven worker classes are 60 existing plus 7 new. Skipped: 2 at the start, 2 at the end.

## Criterion-to-evidence table

| Criterion | Result | Evidence |
|---|---|---|
| AC-1 | Met in the form ruled by P-1: 2 appends, 2 conversions, cursor at row 1, watermark equals the second confirmed date, `closeFolders` once, no Success with `backedUp` 4. The cursor-close half is declared not observable through the seam (QA reads the code path) | `I5-green-cases.txt`; red `I3-red-message.txt`; M-1a, M-1b |
| AC-2 | Met: 2 imports, 2 rows, checkpoint 1, `closeFolders` once | `I5-green-cases.txt`; M-2a, M-2b |
| AC-3 | Met: 3 rows, interceptor saw `[0, 1, 2]`, checkpoint 2 | `I5-green-cases.txt`; M-3a, M-3b |
| AC-4 | Met: 0 rows (the discriminator; `NO_CHECKPOINT` is 0, so the checkpoint line is a consistency check, plan 2.4) | `I5-green-cases.txt`; M-3a, M-3b |
| AC-5 | Met: three controls pass in the red run and in the green run | `I3-red-cases.txt`, `I5-green-cases.txt` |
| AC-6 | Met: six mutations, all detected, failing sets as planned, `restored` after each | `mutation-log.md`, `mutations/` |
| AC-7 | Met: 3 worker warnings at baseline, 0 after, with a compiler that really ran | `I0-kotlinc-summary.txt`, `I7-kotlinc-summary.txt` |
| AC-8 | Met: two files; six hunks; imports replaced in place; `ensureActive` import kept; no `RestoreWorker.kt` line moved (3 / 3 numstat) | `I4-production.txt`, `I8-checks.txt` |
| AC-9 | Met: one `@@ -27,4 +27,4 @@` hunk, comment lines only | `I2-tests.txt` |
| AC-10 | Met: the six named classes pass (plus the new one); nothing edited, deleted, ignored or guarded | `I8-seven-cases.txt`, `I2-tests.txt` |
| AC-11 | Met: gates before and after `exit=0`; failures 0; skipped 2 then 2; count 746 then 753 (+7, none removed); Gradle total 753; no floor edited | `I0-gates*.txt`, `I8-gates*.txt`, `I8-checks.txt` |
| AC-12 | Met: empty build-file diff, `vm-exit=0`, no added `@Ignore` | `I8-checks.txt` |
| IC-1 | Met: no constructor or WorkManager/Hilt signature changed; both callers of `backupCursors` (`:255`, `:462`, both in the class) compile (full gate); the test call site compiles | `I4-production.txt`, `I8-gates.txt` |
| IC-2 | Met: none of the three cancel-path files in the diff | `I8-checks.txt` (`ic2-end` empty) |
| IC-3 | Met: `ic3-old-name-exit=1`; three call lines `currentCoroutineContext().ensureActive()` plus five comments | `I4-production.txt`, `I8-checks.txt` |
| IC-4 | No device step | n/a |

## Integration path

No new production component. The changed code is reached from `BackupWorker.doWork` (`fetchAndBackupItems` calls `backupCursors` at `:255`, the auth-retry at `:462`) and from `RestoreWorker.doWork` (`executeRestore` to `runImapRestoreLoop`; `executeRestoreWithValues` is the test-facing path), all registered through Hilt/WorkManager as before. The new test file is discovered by the unit-test task (it ran in the final gate: 79 files).

## Final state of the tree

`git status --short -- app k9mail-vendored` at the end:

```
 M app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt
 M app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt
 M app/src/test/java/com/zegoggles/smssync/service/BackupWorkerTest.kt
 M app/src/test/java/com/zegoggles/smssync/service/RestoreWorkerCheckpointTest.kt
 M app/src/test/java/com/zegoggles/smssync/service/RestoreWorkerIntegrationTest.kt
?? app/src/test/java/com/zegoggles/smssync/service/BackupWorkerLoopCancellationTest.kt
```

No mutation present: `git hash-object` of the two workers equals `BW_NEW` and `RW_NEW`, and the production diff hash equals `PROD_DIFF`.

## Deviations

1. Restore by copy from the pristine files during mutations (plan I6, as declared).
2. The mutation-log entries and this log's evidence files were appended with shell `echo` calls (log text only); every source and test edit used the Edit or Write tools. The plan's "no heredoc" rule concerns source and test files.
3. Two parallel tool calls in one step (the M-1b write-ahead entry and its Edit): the write-ahead entry's recorded "hash before" equals `BW_NEW`, so the clean check held; the mutated line was confirmed by `grep` before the run. All later entries sequenced the write-ahead entry before the Edit.
4. While the first gate ran in the background I issued one trivial `echo` command by mistake (no Gradle, no file effect); afterwards I waited using file reads only.
5. The I2 plan expectation "hunk `@@ -305 +305,2 @@`" appears in `git diff` with trailing context text (`class BackupWorker @AssistedInject constructor(`); the hunk numbers are as planned.

## Items for the orchestrator

1. The orchestrator commits. Before the commit the HEAD count stays 746; after it, the `HEAD` count equals this log's 753.
2. Plan section 7 items stand: the follow-up bug for the swallowed `CancellationException` (item 4), re-running the six mutations at S2-I5, S4-C and S4-W (item 5), and re-citing contract lines by function name (item 7).
3. QA reads the cursor-close half of AC-1 (c) by code path (`BackupWorker.kt:279-281`, not edited), per ruling P-1.
4. The story's Affected Code row "`BackupWorkerWatermarkTest.kt` or a new test file" is stale; `BackupWorkerWatermarkTest.kt` has no diff (new file taken).
5. `@Ignore` lines by the authoritative pattern: 6 at the start and at the end (the pattern also counts comment mentions); the two `@Ignore`d tests quoted in the story and the skipped count 2 are unchanged.
6. The final gate also rebuilt both APKs from the final tree; no mutation is present and no entry of the mutation log lacks `restored`.

## Hygiene scan

Step I10, result in `evidence/I10-scan.txt`.
