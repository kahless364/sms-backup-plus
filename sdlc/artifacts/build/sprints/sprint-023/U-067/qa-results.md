---
artifact_type: qa-results
story_id: "U-067"
verdict: "PASS"
agent: "QA Analyst"
timestamp: "2026-11-21"
ac_total: 16
ac_passed: 16
ac_failed: 0
tests_run: 753
tests_passed: 751
---

# QA Validation: U-067

Story: `sdlc/artifacts/build/sprints/sprint-023/U-067-worker-cancellation-checks-use-the-running-coroutine.md` (AC-1 to AC-12, IC-1 to IC-4; the binding rulings DP-1 in, DP-2 Route S and P-1). Change under test: commit `c12a2788` (parent `d73017de`). The story starts at `c9770c30`; `git diff --stat c9770c30..d73017de -- app k9mail-vendored build.gradle gradle settings.gradle` prints nothing, so the commits between are documents only and `c9770c30` is the code base. My filtered evidence is in `sdlc/artifacts/build/sprints/sprint-023/U-067/evidence/qa/`: `qa-gate.txt`, `qa-seven-cases.txt`, `qa-kotlinc.txt`, `qa-mutations.txt`, `qa-checks.txt`. These were written through `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools/sanitize.py` or contain command output with no machine details. Raw logs are kept outside the repository. Line numbers are at `c12a2788`. BW = `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt`, RW = `.../service/RestoreWorker.kt`, BLT = `app/src/test/java/com/zegoggles/smssync/service/BackupWorkerLoopCancellationTest.kt`, RIT = `.../service/RestoreWorkerIntegrationTest.kt`, RCT = `.../service/RestoreWorkerCheckpointTest.kt`.

## Summary

I re-ran the full gate myself: exit 0; 753 tests, 0 failures, 0 errors, 2 skipped; JaCoCo passed; `assembleRelease` passed. I read the production diff and each of the seven new tests, and traced each cancellation test pass by pass through the real loop to find where the check throws and what has been written by then. I forced a real Kotlin compile: no worker warning, against 3 at baseline. I ran three mutations of my own: one "old binding restored" (site 2) and two "check deleted" (sites 1 and 3). Each failing set equals the plan's row exactly, and each file was restored to its committed blob. Every criterion is met. Two are met with deviations that were stated or ruled before the build, so the verdict is PASS.

## Verdict: PASS

## Gate and counts (run by QA, 2026-11-21)

| Item | Result | Evidence |
|---|---|---|
| Command | `./gradlew -I <U-063 junit-xml.init.gradle> --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease`. I deleted `app/build/test-results` first. The shell had no `MSYS_NO_PATHCONV`. The run was in the foreground with a 600000 ms timeout; the tool moved it to the background and I waited for the completion notice without polling. No other Gradle ran meanwhile | `evidence/qa/qa-gate.txt` |
| Daemon JVM | `java.version=25.0.5.1` (JDK 25, as `gradle/gradle-daemon-jvm.properties` declares) | same |
| Result | `BUILD SUCCESSFUL in 3m 54s`, `exit=0`. `testDebugUnitTest`, `jacocoTestCoverageVerification` and `assembleRelease` executed; `assembleDebug` was UP-TO-DATE, because the tree is the same as the developer's I8 build | same |
| JUnit totals (79 files) | `tests=753 failures=0 errors=0 skipped=2`, so 751 passed | same |
| Skipped | `AppTest#shouldGetVersionName` (`AppTest.java:13`) and `AuthPreferencesTest#u012_robolectricSmoke_encryptedPrefsSecretStore_migrateFromPlaintext` (`AuthPreferencesTest.java:1079`): the two `@Ignore`d tests, which predate the story. The start skipped count was also 2 (`evidence/I0-gates-junit-totals.txt`) | `qa-gate.txt`; `qa-checks.txt` |
| Authoritative count | `git grep -h "@Test" <commit> -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' \| grep -c "@Test"` = **746** at `c9770c30` and **753** at `c12a2788` | `qa-checks.txt` |
| Delta | +7. The diff of `app/src/test` adds exactly seven `@Test` lines and removes none. The seven are named under AC-11. The only removed lines in `app/src/test` are the four Javadoc lines of `BackupWorkerTest.kt:27-30` | `qa-checks.txt` |
| Gradle total against the count | Equal (753 = 753). JUnit reports an `@Ignore`d method as a skipped test case, so the two ignored tests are inside both the 753 grep count (their methods carry `@Test`) and the Gradle 753, as 751 run and passed plus 2 skipped. There is no difference to explain | `qa-gate.txt` |
| The seven worker classes | 67 cases, 67 passed: `BackupWorkerTest` 9, `BackupWorkerIntegrationTest` 8, `BackupWorkerWatermarkTest` 6, `BackupWorkerLoopCancellationTest` 2, `RestoreWorkerTest` 13, `RestoreWorkerIntegrationTest` 11, `RestoreWorkerCheckpointTest` 18. The list is identical to the developer's `evidence/I8-seven-cases.txt` (`diff` exit 0) | `qa-seven-cases.txt` |

## Acceptance Criteria Results

**Harness, checked once for all four cancellation tests.** Each test runs the real loop in a child coroutine created with `launch(start = CoroutineStart.LAZY)` and assigned to a `lateinit var workJob` before `join()` (BLT:91-94, RIT:385-386, RCT:442-445). The fixture's callback calls `workJob.cancel()`: a job the test holds, never the worker's `coroutineContext` property (Verification Step 2). `currentCoroutineContext()` inside the worker therefore reads the child's context, whose `Job` is `workJob`. Each cancellation test first asserts `workJob.isCancelled` is true, so a test cannot pass because the cancel was never delivered. No test asserts the `Result` type of a cancelled run (bug SC-1).

| AC | Status | Evidence (file:line references required) |
|----|--------|------------------------------------------|
| AC-1 backup loop stops at the next pass (site 1) | **Satisfied with deviation** | **Test** BLT:82 `backupCursors_cancelledAfterSecondAppend_stopsAtNextPass`. It uses 4 rows (BLT:57) and one SMS cursor in a hand-built `BackupCursors` (BLT:87). The transport counts appends and cancels on append 2 inside `appendMessagesBehavior` (BLT:165-169). **Trace** (BW:306-380): LOGIN and CALC `setProgress` take the harness fast path. Pass 1: the check at BW:332 is active; convert 1, append 1, watermark `ROW_DATES[0]`. Pass 2: active; `next()` moves to position 1; convert 2; append 2 cancels `workJob` and then returns the confirmed date; BW:423 sets the watermark to `ROW_DATES[1]`; `setProgress` (BW:362) does not check. Pass 3: `hasNext()` is true, and BW:332 throws `CancellationException`. The `finally` at BW:376-379 runs `closeFolders()`, and the exception leaves `backupCursors`, so `run.result` is never set. **Asserts:** (a) `appends == 2` (BLT:97) and `converts == 2` (BLT:98), so no third append; cursor position 1 (BLT:99). (b) The watermark equals `ROW_DATES[1]` (BLT:100); it starts at `MAX_SYNCED_DATE` (BLT:73), so it is neither the start value nor a later one. (c) `closes == 1` (BLT:101). (d) Not a `Success` with `backedUp == 4` (BLT:102-105). **Bites:** the developer's red run gave "expected 2, was 4" (`evidence/I3-red-message.txt`); so did my Q-M1b. **Deviation (ruled, P-1):** the cursor-close half of (c) cannot be observed through the seam. I judged it by reading the code path, as the ruling requires. `cursors?.close()` is in the `finally` of `fetchAndBackupItems` (BW:279-281). That `finally` encloses the call at BW:255. The story's diff has no hunk there (hunks only at BW:57, :305, :331). The body is a non-suspending Java call, so it runs on a cancelled coroutine. The `CancellationException` (an `IllegalStateException`) is caught by none of the catches at BW:265-278 (`XOAuth2FailedException` extends `MailException`, which extends `Exception`, `MailException.java:36`; `RequiresLoginException` extends `Exception`; `SecurityException` is a sibling), so it passes through that `finally`, which runs once. The cursor half holds by code path |
| AC-2 IMAP restore loop stops at the next pass (site 2) | **Satisfied** | **Test** RIT:380 `doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass`, through the real production entry point `doWork()`, using the existing `buildWorkerWithTransport`, `ImapRestoringTransport` and `createSmsItems`. No helper was edited: the diff of RIT has insertions only (`qa-checks.txt`, removed lines). The 4 handles come from the SMS folder only; the call-log folder returns none. `importMessageBodyImpl` cancels on call 2 and then returns a valid SMS result (`cancellableImapRun`). **Trace** (RW:289-353): pass 1, the check at RW:311 is active; import 1, insert, checkpoint 0 (RW:472). Pass 2: active; import 2 cancels; insert, checkpoint 1 (RW:472); `setProgress` (RW:321) takes the fast path. Pass 3: RW:311 throws. The exception is caught at RW:348, so RW:331 `updateAllThreadsIfAnySmsRestored` and RW:340 `checkpointStore.clear` are not reached. The outer `finally` RW:275-278 runs `closeFolders()` once. **Asserts:** (a) `importCalls == 2` (RIT:389); (b) the fake provider holds 2 rows (RIT:390); (c) the checkpoint is 1 (RIT:391), which also shows it was not cleared, because a clear would read `NO_CHECKPOINT` = 0 (`RestoreCheckpointStore.kt:64`); (d) `closeCalls == 1` (RIT:392). **Bites:** red "expected 2, was 4"; the developer's M-2a and M-2b; my Q-M2a |
| AC-3 test-facing restore loop stops at the next pass (site 3) | **Satisfied** | **Test** RCT:435 `executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass`. It uses 5 items, and `CancellingInterceptor` cancels in `afterInsert(2)` (RCT:417-426, the existing `RestoreInsertInterceptor` hook). **Trace** (RW:378-438): indexes 0, 1 and 2 are each inserted and checkpointed (RW:461, :472), then pass through the interceptor (RW:475); index 2 cancels. `setProgress` RW:406 takes the fast path. Pass 4: RW:399 throws, it is caught at RW:433, and RW:425 `clear` is not reached. **Asserts:** 3 rows (RCT:448); the interceptor saw `[0, 1, 2]` in order (RCT:449); the checkpoint is 2, so not cleared (RCT:450). A single check before the loop would not stop this test, so this form also catches a check moved out of the loop. **Bites:** red "expected 3, was 5"; M-3a, M-3b, and my Q-M3b |
| AC-4 cancelled before the first pass (site 3) | **Satisfied with deviation** | **Test** RCT:460 `executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing`. `launch(start = UNDISPATCHED)` runs the body at once: its first statement is `currentCoroutineContext()[Job]!!.cancel()` (RCT:466), followed directly by the call (RCT:467). `checkpointStore.read` (RW:388) does not suspend, so nothing suspends between the cancel and the first check at RW:399. That check throws on pass 1, and RW:433 catches it. **Asserts:** `child.isCancelled` (RCT:471); 0 rows (RCT:472); the checkpoint reads `NO_CHECKPOINT` (RCT:473); no `CancellationException` is asserted. **Bites:** red "expected 0, was 5"; M-3a, M-3b, Q-M3b. **Deviation (stated in plan 2.4 under A1, plan-review O-5):** `NO_CHECKPOINT` is 0 (`RestoreCheckpointStore.kt:64`), so on its own RCT:473 cannot tell "never written" from "written at index 0" or "cleared". The discriminator is the row count. "No checkpoint is written" follows from it: the only checkpoint writes on this path are at RW:472, inside `if (uri != null)` after the insert at RW:461. With 0 rows inserted, no write happened. I accept this. The criterion's effect is proved, by a weaker assertion plus code ordering |
| AC-5 a run that is not cancelled is unchanged | **Satisfied** | The three controls use the same fixtures and the same `LAZY`/`join` harness, with the cancel trigger disabled. BLT:114 `backupCursors_notCancelled_appendsEveryRow`: not cancelled (BLT:128); 4 appends and 4 conversions (:129-130); position 3 (:131); watermark `ROW_DATES.last()` (:132); `closes == 1` (:133); `Success` with `backedUp == 4` and `STATE_FINISHED` (:135-138). RIT:400 `doWork_imapRestore_notCancelled_restoresEveryItem`: 4 imports, 4 rows, `NO_CHECKPOINT`, `closeCalls == 1`, `Success` (:408-413). RCT:481 `executeRestoreWithValues_notCancelled_insertsEveryItem`: 5 rows, interceptor `[0..4]`, `NO_CHECKPOINT`, `Success` (:494-498). All three passed in the developer's red run (fix absent; `evidence/I3-red-cases.txt`), in the green run, in my gate and in each of my three mutation runs (`qa-mutations.txt`). So the harness alone does not stop a loop. The AC-10 half is covered below |
| AC-6 each test bites its own site (mutation evidence) | **Satisfied** | `evidence/mutation-log.md` has six entries, M-1a to M-3b. Each has a write-ahead `APPLYING` line with a UTC time, the file and its hash before (`BW_NEW` `eb535f5c`, `RW_NEW` `ffa9c57e`), the edit, a NEW3 run of 31 cases, the failing set, `expected: matches`, and a `restored` line with hash-ok, diff-hash-ok and porcelain-ok. All six were detected with exactly the plan's failing set (plan I6 table): M-1a and M-1b fail only the AC-1 test; M-2a and M-2b only the AC-2 test; M-3a and M-3b exactly the AC-3 and AC-4 tests. None was invalid. The three controls passed in every run. The committed blobs at `c12a2788` equal the saved post-fix blobs: `git rev-parse c12a2788:<BW>` = `eb535f5c...`, `<RW>` = `ffa9c57e...` (`qa-mutations.txt`). So no mutation was committed. **My own re-application** (see "QA mutations"): Q-M1b (site 1, check deleted), Q-M2a (site 2, old binding) and Q-M3b (site 3, check deleted). Each gives exactly the plan's failing set, and each was restored |
| AC-7 deprecated binding gone from the build output | **Satisfied** | I ran `./gradlew -I <init> --console=plain :app:compileDebugKotlin --rerun --no-build-cache` (the plan's KC form). The output has the line `> Task :app:compileDebugKotlin` with no UP-TO-DATE or FROM-CACHE suffix, so the compiler really ran. `BUILD SUCCESSFUL`, `exit=0`. It printed 6 `w:` warnings, all in other files (`MainViewModel.kt:55`, `SmsDefaultRoleHelper.kt:116` and `:121`, `WorkManagerScheduler.kt:81`, `SharedPreferencesCheckpointStore.kt:49`, `WorkerEngineFactory.kt:61`). There are 0 lines for `BackupWorker.kt` or `RestoreWorker.kt`, and 0 lines anywhere contain `coroutineContext` (`qa-kotlinc.txt`). The baseline has the three warnings at `BackupWorker.kt:331:17`, `RestoreWorker.kt:311:21` and `:399:21` ("'val coroutineContext: CoroutineDispatcher' is deprecated"), both in the developer's `evidence/I0-kotlinc.txt` (9 warnings) and in `sprint-017/U-060/evidence/final-gates.log:108-110`, which I read. So 9 → 6: exactly the three worker warnings are gone, and nothing was added |
| AC-8 nothing else changes in production | **Satisfied** | (a) `git diff --name-only c9770c30 c12a2788 -- app/src/main k9mail-vendored` lists exactly BW and RW. (b) The hunks are BW `@@ -57 +57 @@` (import), `@@ -305 +305,2 @@` (annotation plus `private`→`internal`, the Route S exception) and `@@ -331 +332 @@` (call); RW `@@ -66 +66 @@`, `@@ -311 +311 @@` and `@@ -399 +399 @@`. numstat is `4 3` and `3 3` (`qa-checks.txt`). I read every hunk (`git show c12a2788 -- app/src/main`). (c) `import kotlin.coroutines.coroutineContext` was replaced in place by `import kotlinx.coroutines.currentCoroutineContext` (BW:57, RW:66). `import kotlinx.coroutines.ensureActive` is kept (BW:54, RW:63). No line of RW moved, and no line of BW above `backupCursors` moved. (d) No hunk touches conversion, append, insert, watermark, checkpoint, return or catch code. The annotation is fully qualified, so no import was added (the U-053 pattern, BW:404) |
| AC-9 the false Javadoc corrected (DP-1 in) | **Satisfied** | `BackupWorkerTest.kt` has one hunk, `@@ -24,10 +24,10 @@`, which replaces lines 27-30 four for four. Every changed line starts with ` * `, and the closing ` */` at :31 is unchanged (I read the diff). The new text says `ic2_workManager_cancelsBackupWorkerRequest` "asserts WorkManager's state only (CANCELLED after cancelUniqueWork()); it does not run the backup loop". It names `BackupWorkerLoopCancellationTest.backupCursors_cancelledAfterSecondAppend_stopsAtNextPass` as the test that shows the loop stopping. The phrase "verified at the WorkManager level" is gone. The ruling corrected the line range to `:27-30` (P-1 corrections) |
| AC-10 existing behaviour intact | **Satisfied** | In my gate, all six named classes pass: `BackupWorkerTest` 9/9, `BackupWorkerIntegrationTest` 8/8, `BackupWorkerWatermarkTest` 6/6, `RestoreWorkerTest` 13/13, `RestoreWorkerIntegrationTest` 9 existing + 2 new, `RestoreWorkerCheckpointTest` 15 existing + 3 new (`qa-seven-cases.txt`). I checked each existing test file hunk by hunk. RIT has two hunks, `@@ -27,6 +27,9 @@` (three import lines added after `runBlocking`) and `@@ -363,6 +366,94 @@` (a block inserted before the "Helpers" banner); both add lines only. RCT has two hunks, `@@ -14,6 +14,10 @@` (four imports) and `@@ -405,6 +409,95 @@` (a block before "InMemoryCheckpointStore unit tests"); both add lines only. `BackupWorkerTest.kt` has the comment hunk only. `BackupWorkerIntegrationTest`, `BackupWorkerWatermarkTest` and `RestoreWorkerTest` have no diff. Across `app/src/test`, the only removed lines are the four Javadoc lines. No assertion was edited or deleted. 0 added lines carry `@Ignore` or `Assume` |
| AC-11 gates and counts | **Satisfied** | **Start:** `evidence/I0-gates.txt` and `I0-gates-junit-totals.txt` (`exit=0`, `tests=746 failures=0 errors=0 skipped=2`). The count at the start of this story is 746 at `c9770c30`; I confirmed it myself. **End:** the developer's I8 (`tests=753`, exit 0) and my own run (same figures, `qa-gate.txt`). Both runs used JDK 25, a shell without `MSYS_NO_PATHCONV`, and timeouts above three minutes. Failures 0; skipped 2 → 2. The count at `c12a2788` is 753 = 746 + 7 − 0. The seven added tests: `backupCursors_cancelledAfterSecondAppend_stopsAtNextPass`, `backupCursors_notCancelled_appendsEveryRow` (BLT); `doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass`, `doWork_imapRestore_notCancelled_restoresEveryItem` (RIT); `executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass`, `executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing`, `executeRestoreWithValues_notCancelled_insertsEveryItem` (RCT). The Gradle total of 753 agrees. JaCoCo passed, and `app/build.gradle` has no diff, so no floor, `limit` or `minimum` was edited |
| AC-12 no dependency or build change | **Satisfied** | `git diff --stat c9770c30 c12a2788 -- build.gradle app/build.gradle k9mail-vendored/build.gradle settings.gradle gradle` is empty, and so is the diff of `gradle/verification-metadata.xml` (`vm-exit=0`). The diff of `app/src/test` adds 0 lines matching `@Ignore` or `Assume`. The `@Ignore` line count by the authoritative pattern is 6 at the start and 6 at the end (the pattern also counts comment mentions). The annotated tests are the same two (`qa-checks.txt`) |
| IC-1 signatures and call sites | **Satisfied** | The constructors BW:106-116 and RW:119-136 are unchanged; I read them at `c12a2788`, and no hunk falls in them. `backupCursors` has two production callers, both inside the class: BW:255 (`fetchAndBackupItems`) and BW:463 (the auth retry in `handleAuthError`). The plan and the P-1 ruling corrected the story's "single caller". There are two test call sites, BLT:92 and BLT:124. All compile in my gate. `@VisibleForTesting(otherwise = PRIVATE)` (BW:305) keeps the lint contract, because both production callers are in the class |
| IC-2 user-facing cancel path unchanged | **Satisfied** | `git diff --name-only c9770c30 c12a2788` lists none of `StatusPreference.java`, `WorkManagerCancelCollector.kt` or `MainViewModel.kt` (`qa-checks.txt`, `ic2-end` with nothing before it). `ic2_workManager_cancelsBackupWorkerRequest` and `ic2_workManager_cancelsRestoreWorkerRequest` pass in my gate |
| IC-3 every site accounted for | **Satisfied** | `git grep -n "coroutineContext" c12a2788 -- <BW> <RW>` exits 1 with no output. `git grep -n "ensureActive()" c12a2788 -- app/src/main` gives exactly three call lines, BW:332, RW:311 and RW:399, each `currentCoroutineContext().ensureActive()`, plus the comments BW:72, :286, :330, RW:84 and `WorkManagerCancelCollector.kt:23` (`qa-checks.txt`) |
| IC-4 no emulator check | **Not applicable** | The story requires none, and every criterion is shown at unit level. The bug's Verification Step 8 is optional. No emulator or device was touched in this validation |

## Requirement Scope Coverage

The story has no REQ-\* source (`requirements: []`). Its source is BUG-022. The contract side, CNTR-MODERNIZATION-004 rule 7, is read and not produced. The test-count rule is REQ-MODERNIZATION-015 AC-13 as amended by CR-016.

### BUG-022 acceptance criteria (for the bug's close-out)

| Bug criterion | Satisfied by U-067? | Evidence |
|---|---|---|
| AC-1 each check observes the running job and throws when it is cancelled | **Yes** | BW:332, RW:311, RW:399 read `currentCoroutineContext()`. The four cancellation tests pass with the fix and fail without it (red run; mutations (a)) |
| AC-2 a unit test per site that fails unfixed and passes fixed; for the restore sites a pre-cancel test asserting no insert and no checkpoint | **Yes, with deviation** | Site 1: BLT:82 through the Route S seam, so the bug's "record why" fallback was not used. Site 2: RIT:380 cancels mid-loop. Site 3: RCT:435 cancels mid-loop, and RCT:460 is the pre-cancel test of Steps to Reproduce B. **Deviation:** site 2 has no pre-cancel test. Its mid-loop test is the story's stricter form (a check placed only before the loop would not pass it), and the story's AC-2 asks for exactly that form. The no-checkpoint assertion of RCT:460 relies on the row count (see AC-4) |
| AC-3 the tests also fail when a check is removed or mis-bound, by a recorded mutation per site | **Yes** | `evidence/mutation-log.md`: six mutations, all detected. My Q-M1b, Q-M2a and Q-M3b reproduce three of them |
| AC-4 no deprecation warning on the three lines | **Yes** | `qa-kotlinc.txt`: 0 worker warnings and no `coroutineContext` line, against 3 at baseline |
| AC-5 cancellation at least as prompt as today | **Yes** (by code reading plus tests) | Nothing inside a pass changed order, and `setProgress` (BW:362, RW:321, :406) is still at the same place, so every stop point of the old code still exists. The fix adds a stop point that actually throws at the top of each pass. So for any position at cancellation, the number of passes started afterwards can only stay the same or fall, and it falls by at most one (the pass the old code started before its next `setProgress`). Under the harness, the old code never stopped and the new code stops at the next pass (four tests) |
| AC-6 nothing else in the two workers changes | **Yes**, with the IC-1 seam | Six hunks only (AC-8). The `private`→`internal` change plus the annotation is the seam the bug's IC-1 permits. The Javadoc change is in a test file, under SC-2 / DP-1 |
| AC-7 existing worker tests pass unedited; the gate exits 0; skipped not higher | **Yes** | AC-10 and the gate above: 753/0/0/2 |
| IC-1 a seam, if opened, has every caller and test site updated; no WorkManager or Hilt signature changes | **Yes** | IC-1 above |
| IC-2 the cancel path is unchanged | **Yes** | IC-2 above |

BUG-022's criteria are all met by this story, so the bug can be closed. Its SC-1 (a cancelled run returns `Result.failure` and logs at error level) is out of scope and stays open as the separate follow-up the story names.

| Requirement | Scope Item | Covered (yes/no) | Evidence (file:line) |
|-------------|------------|-------------------|----------------------|
| REQ-MODERNIZATION-015 AC-13 as amended by CR-016 | Start and end counts by the authoritative command with commit ids; added tests named; the Gradle total checked against the count | yes | 746 at `c9770c30`, 753 at `c12a2788`; seven named; Gradle 753 (`qa-checks.txt`, `qa-gate.txt`) |
| DES-MODERNIZATION-014 F-1 stop rule (`:1130-1132`) | The cancellation tests exist and pass, and no Kotlin warning names `coroutineContext` in either worker | yes | The four tests in `qa-seven-cases.txt`; `qa-kotlinc.txt` |
| CNTR-MODERNIZATION-004 rule 7 (`:279`) | Cancel cooperatively cancels the running coroutine at the next suspension point | yes (the stricter form "top of the next pass") | BW:332, RW:311, :399; AC-1 to AC-4 tests |

## Integration Path Verification

| Component | Entry Point | Call Path | Verified (yes/no) |
|-----------|-------------|-----------|--------------------|
| Site 1 check (BW:332) | WorkManager → `BackupWorker.doWork` (Hilt `@HiltWorker`, BW:105-106) | `doWork` → `executeBackup` (BW:163) → `fetchAndBackupItems` (BW:190) → `backupCursors` (BW:255; also BW:463 on the auth retry) → loop BW:331 → check BW:332 | yes (code read; the test enters at `backupCursors` through the Route S seam, BLT:92) |
| Site 2 check (RW:311) | WorkManager → `RestoreWorker.doWork` | `doWork` → `executeRestore` → `runImapRestoreLoop` (RW:257) → loop RW:310 → check RW:311 | yes (RIT:380 enters at the production `doWork()` itself) |
| Site 3 check (RW:399) | Test-facing `internal executeRestoreWithValues` (RW:378) | direct → loop RW:398 → check RW:399 | yes (RCT:435, :460; not a production path, by design) |
| `backupCursors` seam (BW:305-306) | Test only, `otherwise = PRIVATE` | BLT:92, :124 | yes |
| New test class | `:app:testDebugUnitTest` discovery | 79 result files in my gate, including `BackupWorkerLoopCancellationTest` (2 cases) | yes |

## Behavioral Contract Verification

| Contract | Clause | Implementation (file:line) | Match (yes/no) |
|----------|--------|----------------------------|----------------|
| CNTR-MODERNIZATION-004 rule 7 | `cancel` cooperatively cancels the running coroutine | BW:332, RW:311, :399 now observe the running job (AC-1 to AC-4) | yes |
| CNTR-MODERNIZATION-004 `:287` | Cancellation is terminal, not failure | Not changed by this story: a cancelled run still returns `Result.failure` from the broad catches (BW:168, RW:348, :433), as before by the `setProgress` route. This is bug SC-1, out of scope | unchanged (disclosed) |
| Watermark gating (BUG-010) | The watermark only after a confirmed append | BW:417-423 unchanged; AC-1 asserts the watermark equals the second confirmed date | yes |
| Restore write ordering | insert → checkpoint → interceptor; checkpoint kept when the run does not succeed | RW:461, :472, :475 unchanged; AC-2 and AC-3 assert the checkpoint is retained | yes |
| `finally` cleanup | `closeFolders()` and `cursors?.close()` run on cancellation | BW:376-379 (asserted once, BLT:101), RW:275-278 (RIT:392), BW:279-281 (code path, P-1) | yes |
| Worker signatures | Constructors seen by WorkManager and Hilt | BW:106-116, RW:119-136: no hunk | yes |

## Capabilities Inventory Verification

| Capability | Status in Log | Verified in Code | File:Line | Result |
|------------|--------------|-----------------|-----------|--------|
| The check at the top of each backup pass | RETAINED (now observing the job) | yes | BW:332 | PASS |
| The checks at the top of both restore passes | RETAINED | yes | RW:311, :399 | PASS |
| `backupCursors` body (progress, `checkSettings`, convert, append, watermark, calendar sync, `STATE_FINISHED`, `closeFolders`) | RETAINED | yes; the only hunks are the signature lines and the call line | BW:306-380 | PASS |
| Both production callers of `backupCursors` | RETAINED | yes | BW:255, :463 | PASS |
| `kotlinx.coroutines.ensureActive` import | RETAINED | yes | BW:54, RW:63 | PASS |
| Catches and results | RETAINED | yes, no hunk | BW:164-171, :265-281; RW:260-278, :348-351, :433-436 | PASS |
| `finally` cleanup | RETAINED | yes | BW:279-281, :376-379; RW:275-278 | PASS |
| Import `kotlin.coroutines.coroutineContext` | INTENTIONALLY REMOVED | N/A | N/A | Valid: AC-8 (c), replaced in place |
| `private` visibility of `backupCursors` | INTENTIONALLY REMOVED | N/A | N/A | Valid: DP-2 Route S, AC-8 (b) |
| False Javadoc in `BackupWorkerTest.kt` | INTENTIONALLY REPLACED | yes | `BackupWorkerTest.kt:27-30` | Valid: DP-1, AC-9 |

## Test Results

- Full gate: exit 0, `tests=753 failures=0 errors=0 skipped=2` (751 passed), JaCoCo passed, `assembleRelease` passed (`evidence/qa/qa-gate.txt`).
- The seven worker classes: 67 of 67 passed, identical to the developer's I8 list (`evidence/qa/qa-seven-cases.txt`).

### QA mutations (`evidence/qa/qa-mutations.txt`)

Each mutation was one single-line `sed` substitution on the working-tree file, using the plan's literal text. It was followed by one targeted run of the plan's NEW3 set (`--tests` `BackupWorkerLoopCancellationTest`, `RestoreWorkerIntegrationTest`, `RestoreWorkerCheckpointTest`; 31 cases, results folder deleted first), then `git checkout -- <file>`. Before and after each one, `git status --short -- app k9mail-vendored` was empty, and the file's `git hash-object` equalled the committed blob.

| Id | Plan row | Mutation | Result | Tests that failed | Restore |
|----|----------|----------|--------|-------------------|---------|
| Q-M1b | M-1b (site 1, check deleted) | BW:332 `currentCoroutineContext().ensureActive()` → `if (false) currentCoroutineContext().ensureActive()` | **Detected**: exit 1, 1 of 31 failed, 30 passed | `backupCursors_cancelledAfterSecondAppend_stopsAtNextPass`: "expected: 2 but was: 4" (the `appends` line). Equals the plan row | `git checkout`; hash `eb535f5c` = committed; status empty |
| Q-M2a | M-2a (site 2, old binding restored) | RW:311 → `coroutineContext.ensureActive()` (compiles through the inherited member) | **Detected**: exit 1, 1 of 31 failed, 30 passed | `doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass`: "expected: 2 but was: 4". Equals the plan row | `git checkout`; hash `ffa9c57e` = committed; status empty |
| Q-M3b | M-3b (site 3, check deleted) | RW:399 → `if (false) currentCoroutineContext().ensureActive()` | **Detected**: exit 1, 2 of 31 failed, 29 passed | `executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass` ("expected: 3 but was: 5") and `executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing` ("expected: 0 but was: 5"). Equals the plan row | `git checkout`; hash `ffa9c57e` = committed; status empty |

After the last restore, `git diff --quiet HEAD -- app k9mail-vendored` exits 0. Nothing was staged or committed. The three controls passed in all three runs. The compiled classes under `app/build` are from the Q-M3b run until the next build, and Gradle recompiles them because the restored source differs from that run's input.

## Regression Results

- Every other test class passes in the full gate (0 failures, 0 errors over 79 files).
- Build files and dependency verification: empty diff.
- Hygiene: I scanned the content of all 47 files of `c12a2788` for e-mail addresses, user-profile or application-data paths, the user name and the host name. User name: 0 files; host name: 0 files. The one e-mail-like hit is the synthetic IMAP user of `RestoreWorkerIntegrationTest.setUp` (RIT:84) in the reserved `.local` test domain. It was already present at `c9770c30` (`:81`) and is not in the story's diff. My own evidence files scan clean on all four patterns.

## Deviations (all stated before or during the build, and accepted)

1. **AC-1 (c), cursor-close half:** not observable through the Route S seam (ruling P-1). Judged by code path: BW:279-281 is not edited, its body does not suspend, and no catch at BW:265-278 intercepts a `CancellationException`.
2. **AC-4, "no checkpoint is written":** `NO_CHECKPOINT` is 0, so the checkpoint assertion does not tell the cases apart. The 0-row assertion carries the criterion, together with the write ordering at RW:461-472 (plan 2.4, A1).
3. **BUG-022 AC-2, site 2:** covered by a mid-loop test, not a pre-cancel test, as the story specifies.
4. **Developer protocol deviations** (`implementation-log.md` "Deviations" 1 to 5: restore by copy, log appends by `echo`, one parallel write-ahead, one stray `echo`, hunk trailing context). None affects a result: the committed blobs equal `BW_NEW` and `RW_NEW`.

## Issues Found

- **No defects.**
- **UNVERIFIED (disclosed) runtime claim.** The implementation log's `## Runtime Claims` has one escape-hatch entry: "in production, WorkManager's own stop (its worker wrapper calling `onStopped()`) cancels the loop through the same job". The justification names the missing infrastructure (a running WorkManager with a real executor and stop, that is, a device run excluded by IC-4) and why the unit harness cannot reach it (the tests call the worker directly, not through `startWork()`). It is specific, so I accept it. It is **not** reported as verified, and no criterion depends on it: the story defines "the work's job" as the job of the test's coroutine (AC terms). The plan's L5b bytecode reading (`startWork` launches `doWork` in a child of `job`; `onStopped` → `future.cancel` → listener → `job.cancel()`) makes the claim plausible. The other two runtime claims carry embedded evidence (red, green and mutation runs; the KC baseline and after), and I reproduced both.
- **O-1 (observation).** The cancelled runs still log "unrecoverable error" (backup, BW:169) or "IllegalStateException (possible memory)" (restore, RW:349, :434) at error level, and return `Result.failure`. This was true before the fix too, by the `setProgress` route, and is bug SC-1, out of scope.

## Recommendations (not required by any criterion)

1. Raise the SC-1 follow-up bug: rethrow `CancellationException` ahead of the broad catches (BW:168, RW:272, :348, :433). The new tests were written not to need editing when that happens.
2. In S2-I5, S4-C and S4-W, re-run the six mutations, not only the tests (story Scope Concern 3, ruled). A work-testing version whose `setProgress` checks the job would make the unmutated tests pass for the wrong reason, and only mutation (b) would show it.
3. If the production stop path should ever be measured without a device: a narrower probe may exist through `TestListenableWorkerBuilder.build().startWork()` followed by cancelling the returned future, which per L5b cancels `job`. It was not tried here and is not required.
4. Before any device install from this checkout, run `assembleDebug`. It will recompile after my mutation runs.

## Phase Completion Report
---
story_id: "U-067"
phase: "validation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-023/U-067/qa-results.md"
story_status: "done"
current_build_phase: "validation"
ac_passed: 16
ac_total: 16
errors: []
---
