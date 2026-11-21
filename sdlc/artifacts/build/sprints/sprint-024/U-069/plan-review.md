---
artifact_type: gate-result
story_id: "U-069"
gate: plan-review
gate_id: "plan-review"
checkpoint: plan-gate
verdict: FAIL
iteration: 1
agent: "Solution Architect (separate instance, plan-review)"
timestamp: "2026-11-21T12:42:25Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-024/U-069/plan.md"
base_commit: "4300ffa98370b77dcc4dd8d12142079aada8313a"
blockers: 3
observations: 7
---

# Gate Report: gate-result — U-069

## Gate: plan-review @ plan-gate

## Verdict: FAIL

## Summary

I reviewed iteration 1 of the plan (1,659 lines, untracked in the working tree). I did not take its line numbers, blobs or counts on trust. I re-derived them on scratch copies outside the repository, and never read the shared files from the working tree.

**Base and simulation.**
- I took the base files with `git show 4300ffa9:<path>` and applied U-068's planned scheduler edits E5.12 to E5.16 (U-068 plan iteration 2, 3.6) by script. Each old text occurs once, and the result is 481 lines with blob `0194308c…`, the same as L20.
- I then applied W1 to W3 to the base worker and got blob `c47a0452…` (76 lines, `--numstat` `25 28`, one hunk `@@ -17,58 +17,55 @@`). I built the test file from 3.2: green blob `6e4437bd…` and red blob `5a57298c…`, 452 lines each, with 15 `@Test` lines. All of these match L19.
- I extracted the plan's `oldtext_check.py` and ran it on LF and CRLF scratch trees (post-U-068 scheduler, HEAD test file and `app/build.gradle`). Both `start` and `fixed` give `mismatches=0`.

**U-068's committed increments compared with the simulation.** HEAD is now `d93bb508`, a document commit. The code commits since `4300ffa9` are `U-068 S1-I1` to `S1-I4`. The committed text agrees with the simulation:
- root `build.gradle:30` is `reports.junitXml.required = true`;
- `app/build.gradle` differs from the base only at `:24`, `:153` and `:160`, so Rule 15 is still at `:575-588` (`:579`, `:586`);
- `WorkManagerSchedulerTest.kt` has 25 `@Test` lines, with `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` at `:380`, the two `ac5_scheduleIncoming_*` tests at `:457` and `:470`, `se002_scheduleIncoming_*` at `:517` and the AC-12 (c) comment at `:548`.

The scheduler is still the base blob `0ae361ec…`, because U-068 I5 is not yet committed. The worker is `c37bbbff…`.

**Result.** The production change, the harness, the red run, the mutation table and the IC-4 commands hold up; details are below.

There are three blockers:
- **B-1:** the plan's JaCoCo premise is wrong. The Hilt and Dagger classes generated for the worker report no lines, so the expected floor is 1.00, not 0.20 to 0.55. A build-file comment written in advance would then be false.
- **B-2:** the IC-3 outcome rules call two cases a pass that the story does not allow.
- **B-3:** one final check is a `git grep` on an untracked file, so it can never find anything.

## Findings

### Blockers

**B-1: the generated worker classes contribute no lines to JaCoCo, so the floor expectation, the D19 band and the E-G1/E-G2 texts rest on a false premise.**

- **Where:**
  - `plan.md:82` (L17: "the KSP-generated `_Factory` and `_AssistedFactory_Impl` … are counted");
  - `:94` (N6: "about 0.35 … about 13 uncovered generated lines");
  - `:201` (2.3 row A1: "Generated classes in the `worker` package lower its measured ratio");
  - `:874` (E-G1 new text: "the Hilt-generated classes of the worker are counted and are not run by unit tests");
  - `:881-882` (E-G2 expected class names);
  - `:1167-1170` (I4 step 3: "`F` between `0.20` and `0.55`");
  - `:1376` (D19);
  - `:1481` (R7).
- **Evidence.** The current `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml` was built today from the same generators. It lists these classes as empty `<class …/>` elements with no counter at all:
  - `BackupWorker_Factory`, `BackupWorker_AssistedFactory_Impl`, `BackupWorker_AssistedFactory`;
  - `RestoreWorker_Factory`, `RestoreWorker_AssistedFactory_Impl`, `RestoreWorker_AssistedFactory`, `RestoreWorker_HiltModule`.

  The compiled `BackupWorker_Factory.class` does have line tables (3 `LineNumberTable` attributes) and carries `dagger.internal.DaggerGenerated`. JaCoCo 0.8.14 drops any class whose CLASS- or RUNTIME-retained annotation has "Generated" in its simple name. The `_AssistedFactory` and `_HiltModule` types are interfaces without code. `BackupTriggerWorker$Companion` also has no counter today, because its private constructor is filtered.

  So after the fix the `worker` package holds six classes: `BackupTriggerWorker`, `$Companion`, `_AssistedFactory`, `_AssistedFactory_Impl`, `_Factory` and `_HiltModule`. Only `BackupTriggerWorker` has lines. The 15 tests run the constructor and every line of `doWork()`, and I4 step 3 itself expects `ci>0` on every line. **Expected: `covered = total`, ratio 1.0000, `F = 1.00`.**
- **Effect.**
  - The plan's main path lands in D19's "outside 0.20 to 0.55" branch.
  - E-G1 then writes into `app/build.gradle` a sentence that implies the generated classes weigh on the floor. Ruling P-1 exists precisely to keep false sentences out of Rule 15.
  - E-G2's "classes counted" would list six names, four of which contribute nothing.
  - The story's AC-11 sentence "the measured ratio will be below 100%" is also wrong (see O-6).
- **Fix:**
  1. **L17, N6, 2.3 row A1, R7.** State that the generated classes sit in the class roots but report 0 lines (the `@DaggerGenerated` filter; interfaces have no code). Expected `F = 1.00` with `c = t`, the lines of `BackupTriggerWorker.kt` only.
  2. **I4 step 3.** Expected: six `CLASS` lines; `BackupTriggerWorker` `covered=t missed=0`; the other five `covered=0 missed=0`; `PACKAGE … floor=1.00`.
  3. **D19.** "`F` below 1.00: record each `ci=0` line of `I4-worker-lines.txt` and continue (stop below 0.05). A generated class with a non-zero total: record it and use the alternative E-G1 text."
  4. **E-G1.** `new_string`: `        // BackupTriggerWorker has unit tests since U-069 (BackupTriggerWorkerTest); its Hilt-generated classes report no lines to JaCoCo (@DaggerGenerated filter; the others are interfaces).` Alternative text when D19 applies: `        // BackupTriggerWorker has unit tests since U-069 (BackupTriggerWorkerTest).`
  5. **E-G2.** `<names>` are the `CLASS` names whose `covered + missed > 0`, so the expected value is `BackupTriggerWorker`. The comment form becomes `minimum = <F> // MEASURED <P>% (<c>/<t> lines, <YYYY-MM-DD>, U-069); lines from: <names>`.
  6. Hand the 1.00 floor to the orchestrator for U-070, which edits this worker: any uncovered line it adds fails Rule 15.

**B-2: the IC-3 outcome rules grant "Pass" where the story requires the `BackupWorker` lines.**

- **Where:** `plan.md:1538-1541`, the "Pass, not conclusive for the run" bullet: "Also when the run says 'nothing to backup', or the backup does not start within the wait (for example Wi-Fi only on and no unmetered network on the AVD)".
- **Story:** `U-069-…md:112`, IC-3 Results: "the `BackupWorker` lines are required only when the scheduler line is `enqueued INCOMING`". The "not conclusive for the run" label is given only to the `skipped` and `blocked` outcomes.
- **Effect.** An enqueued backup that never starts, or finds nothing to back up, would be recorded as an IC-3 pass, so the criterion would be left unproven.
- **Fix.** Delete that sentence from the pass bullet. Add a bullet: "**Not conclusive: enqueued, run not observed** — the scheduler line is `enqueued INCOMING` and, within the wait, either `BackupWorker: nothing to backup` appears or no `starting backup (` line appears (say which, with the `network=` value). This is not a pass: IC-3 stays open and the orchestrator reports it to the owner. No setting is changed to force a run."

**B-3: the "no `@Ignore`, no `Assume`" check on the new test file is vacuous.**

- **Where:** `plan.md:1201`, I6 step 4: `git grep -n "@Ignore\|Assume" -- $TST; echo "exit=$?"` → `exit=1`.
- **What.** `$TST` is untracked until the orchestrator commits it, and `git grep` without `--untracked` searches tracked files only. The command prints nothing and exits 1 whatever the file contains. `I6-final-checks.txt` would therefore record a proof that checked nothing.
- **Fix.** `grep -nE '@Ignore|Assume' $TST; echo "exit=$?"` → no line, `exit=1`. Alternatively `git grep --untracked -n -E '@Ignore|Assume' -- $TST`, the form the plan already uses for IC-2 (a) and AC-14.

### Checks re-derived (no finding)

**1. Production edits** (`BackupTriggerWorker.kt`). I applied W1 to W3 to the scratch copy and read the result in full.
- **Annotations.** `@HiltWorker` and `class BackupTriggerWorker @AssistedInject constructor(@Assisted context: Context, @Assisted params: WorkerParameters, private val scheduler: BackupScheduler, private val preferences: Preferences) : Worker(context, params)`. This is `BackupWorker.kt:105-116`'s form, and the imports match `BackupWorker.kt:51-53`.
- **androidx.hilt 1.2.0 accepts a plain `Worker`.** I read `hilt-compiler-1.2.0.jar` with `javap -v`. `WorkerStep` checks `ClassNames.LISTENABLE_WORKER`, and its messages include "@HiltWorker annotated class should contain exactly one @AssistedInject annotated constructor." and the Context-before-WorkerParameters rule. `WorkerElement` names `\u0001_AssistedFactory` and `\u0001_HiltModule`, and the generator writes `bind`.
- **What `HiltWorkerFactory` needs** (hilt-work 1.2.0). Its constructor is package-private over `Map<String, Provider<WorkerAssistedFactory<? extends ListenableWorker>>>`, and `WorkerAssistedFactory.create(Context, WorkerParameters)` builds the worker.
- **Generated module form.** The existing `RestoreWorker_HiltModule.java` has `@Binds @IntoMap @StringKey("com.zegoggles.smssync.service.RestoreWorker") … bind(RestoreWorker_AssistedFactory)`. This is the form test `ic1_…` reflects on.
- **Annotation retention** (`javap -v`): `IntoMap`, `StringKey`, `AssistedFactory`, `AssistedInject` and `Assisted` are RUNTIME; `HiltWorker` is CLASS.
- **Hilt bindings.** `BackupScheduler` is bound by `@Binds @Singleton` (`SchedulerModule.kt`). `Preferences` is provided by `@Provides @Singleton` (`PreferencesModule.kt`). There is no cycle: `WorkManagerScheduler` injects only `Context`, `Preferences` and `SecretStore`.
- **Factory configuration.** `App.java` returns `Configuration.Builder().setWorkerFactory(hiltWorkerFactory)`, and its map today has `newMapBuilder(2)` with two `put` lines.
- **`doWork()`.** The log line is byte-identical (two source lines), followed by `scheduler.scheduleIncoming()` and an unconditional `return Result.success()`.
- **Nothing else changes.** Package line and class name are unchanged. Of the two `^[-+]` diff lines that match the header pattern, both are the `class` header.

**2. The 15 tests.**
- **Accessors in WorkManager 2.9.1** (`javap`), all present: `WorkInfo.getId/getState/getTags/getRunAttemptCount/getConstraints/getInitialDelayMillis`, `State.isFinished()`, `WorkManagerImpl.getInstance(Context)`, `getWorkDatabase()`, `WorkDatabase.workSpecDao()`, `WorkSpecDao.getWorkSpec(String)`, the public fields `WorkSpec.input/initialDelay/backoffPolicy/backoffDelayDuration`, `getWorkInfoById(UUID)` returning `ListenableFuture<WorkInfo>`, `cancelUniqueWork`, and `Operation.getResult()`. `DEFAULT_BACKOFF_DELAY_MILLIS = 30000`.
- **The same names in the 2.12.0 sources:** `WorkSpecDao.kt:64` `getWorkSpec(id: String): WorkSpec?`, `WorkManagerImpl.java:168` and `:276`, `WorkDatabase.kt:88`, the `@JvmField` fields at `WorkSpec.kt:50`, `:52`, `:62` and `:65`, `WorkInfo.kt:35-68` and `:202`, and `WorkManager.kt:515` `ListenableFuture<WorkInfo?>`.
- **work-testing 2.9.1.** `TestListenableWorkerBuilderKt.TestListenableWorkerBuilder(Context, Data, List, int, List, List)` with `$default` exists, and so do `setWorkerFactory` and `build()`. `build()` calls `createWorkerWithDefaultFallback` and rejects a null worker. `TestScheduler` gates on constraints and on the initial delay.
- **Robolectric 4.15.1 `ShadowLog`:** `getLogsForTag(String)` returns `ImmutableList<LogItem>`; `LogItem` has the public final fields `type`, `tag` and `msg`; the class carries `@Resetter`.
- **Harness.** The harness is `WorkManagerSchedulerTest.setUp`'s: the single-thread executor, `TestableBackupWorkerFactory`, the same stubs, and a real `WorkManagerScheduler`. The degraded state is set through the mocked `SecretStore`.
- **Constants and tags.** `BACKOFF_INITIAL_SECS` is a public `const val … = 30L`. The scheduler's `TAG` is `"SMSBackup+"`, so its `blocked` and `enqueued INCOMING` lines are in `getLogsForTag(TAG)`.
- **Red run on the unchanged worker.** The old worker reads real preferences, which default to 60 (`Defaults.java:15`), and sets no constraints. Expected **11 fail, 4 pass**, the same as 5.2:

  | Tests | Result | Why |
  |-------|--------|-----|
  | `ac1`, `ac2` | fail | `NOT_REQUIRED` |
  | both `ac3` | fail | the constraints differ |
  | `ac4`, `ac5`, both `ac6` | fail | the work is enqueued |
  | `ac9_allowed` | fail | the logged value is 60, not 75 |
  | `ac9_degraded` | fail | the trigger line is found but no `blocked` line follows |
  | `ic1` | fail | no generated class exists |
  | `ac7`, `ac8a`, `ac8b`, `ac8c` | pass | `ENQUEUED`, 60 000 ms, `REPLACE` |
- **Determinism.** The clock is never advanced and no constraint is met, so `INCOMING` stays `ENQUEUED`. In 2.9.1 `REPLACE` deletes the old records and `KEEP` returns early only for `ENQUEUED`/`RUNNING` entries, so `assertReplaced` handles both outcomes. A negative initial delay does not throw in 2.9.1: `setInitialDelay` checks only for overflow.

**3. Mutation table.**
- **Re-derived sets.** I re-derived every failing set and every must-pass set, and all eleven rows match 4.8:

  | Mutant | Failing | Passing |
  |--------|---------|---------|
  | M-T1 | 10 | `ac7`, `ac8a`, `ac8b`, `ac8c`, `ic1` |
  | M-T7 | 10 | `ac4`, `ac5`, both `ac6`, `ic1` |
  | M-T8 | 5 | the other 10 |
  | M-T9 | 2 | the other 13 |
  | M-T10 | `ic1` | the other 14 |
  | M-T2 | `ac4`, `ac9_degraded` + `se002_scheduleIncoming_*` | 13 + 24 |
  | M-T3 | `ac5` + `ac5_…autoBackupDisabled…` | 14 + 24 |
  | M-T4 | both `ac6` + `ac5_…zeroTimeout…` | 13 + 24 |
  | M-T5 | `ac7`, both `ac3` | 12 + 25 |
  | M-T6 | `ac8a`, `ac8b`, `ac8c` | 12 + 25 |

  M-T11 is a compile failure from the hilt-compiler message.
- **Must-pass details.** Under M-T6, both `ac3` pass because the cancelled entry is finished, and `inv1_replace_incoming_…` passes because it checks only `hasSize(1)`. No scheduler test reads the initial delay.
- **Edits and protocol.** Each counted mutant is one Edit, and each old text occurs once in the fixed text (`oldtext_check.py fixed`). The "does not compile" row restores by `cp` whatever the result (step 5), and its preconditions hold at every entry.

**4. Criteria procedures.**
- **AC-10, AC-13, IC-2 greps.** I re-ran each against HEAD, translated to post-U-068 line numbers, and against the simulated post-story files:
  - (a) gives 4 lines, then none;
  - (b) gives one line, at `:153` post-U-068;
  - (c) gives 8 lines, then 6, with `:421` post-U-068;
  - (d) gives 7 lines;
  - IC-2 (a) at the end gives **15** lines: 5 in the scheduler, the worker's `:52` and `:63`, and the test's `:63`, `:302`, `:319`, `:340`, `:341`, `:363`, `:447`, `:449`;
  - (e) gives 4 lines, with the worker's line moving to `:41`.
- **AC-11 floor procedure.** The XML is read at I4, before E-G1 and E-G2; no exclusion is added; `⌊20c/t⌋/20` cannot exceed the ratio. The expected `app/build.gradle` diff is `2 2`, four `^[-+][^-+]` lines and two `minimum =` lines. Only `:579` and `:586` contain the searched phrases today. See B-1 for the values.
- **AC-12.** All four sites are handled.
- **AC-13 (d).** The expected `1 file changed, 2 insertions(+), 2 deletions(-)` is right under P-1.
- **AC-14.** `C0 + 15` uses the `--untracked` form together with the `app/src/test` porcelain check (D28). The count at HEAD is 755 today; no step fixes it.

**5. IC-4 and IC-3 tooling.**
- **Tools on this machine.** `build-tools/36.0.0` in the SDK folder has `dexdump.exe` and `aapt2.exe`. There is no `cmdline-tools` folder. `platform-tools` has `adb.exe`. `"$BT/dexdump"` runs without the `.exe` suffix from Git Bash; I ran its usage text. `unzip`, `timeout`, `sha256sum` and `awk` are on PATH, and `sdk_dir.py` prints the folder with forward slashes.
- **Release outputs.** The release output is `app-release-unsigned.apk`, because there is no keystore (`app/build.gradle:55`). The existing `mapping.txt` is LF, R8 9.0.32, and contains exactly one line `com.zegoggles.smssync.worker.BackupTriggerWorker -> com.zegoggles.smssync.worker.BackupTriggerWorker:`. So the plan's anchored count of `1` is derivable.
- **Keep rules.** `work-runtime-2.9.1.aar`'s `proguard.txt` has `-keep class * extends androidx.work.Worker`. `hilt-work-1.2.0.aar` has `-keepnames @androidx.hilt.work.HiltWorker class * extends androidx.work.ListenableWorker`.
- **IC-3 safety rules.** These are complete: only `Pixel_9_Pro`, never `Pixel_10_Pro`, `install -r` only, never `-d` or uninstall, one console SMS, no setting changed, a password prompt means "not run", and no adb argument needs `MSYS_NO_PATHCONV`. Only the outcome labels are wrong (B-2).

**6. Procedure quality.**
- **Evidence lists.** Every file in the 4.9 list is written by a named step: 8 + 3 + 2 + 7 + 22 + 9 + 1 = 52 evidence files, plus the log and 2 tools.
- **Gradle runs.** The tally of 17 is right: I0 2, I2 1, I3 1, I4 1, I5 11, I6 1.
- **Scoping.** The end checks are scoped (D29, D30), and U-068's commits before `<start>` are only recorded.
- **Copies and restores.** Pristine copies use `cp`, with the raw SHA-256 recorded next to the blob.
- **Developer constraints.** All of them are present: foreground Gradle with 600000 ms, `unset MSYS_NO_PATHCONV`, waiting for a backgrounded run, stopping on an unexplained kill, committing nothing, no emulator, and two build-file lines.

### Observations (non-blocking)

**O-1 (low): the hunk-header expectation omits git's function-context suffix.**
- **Where:** `plan.md:1146`, I3 step 2.
- **What:** `git diff -- $WRK | grep -E '^@@'` prints `@@ -17,58 +17,55 @@ package com.zegoggles.smssync.worker`, not `@@ -17,58 +17,55 @@` only.
- **Fix:** `grep -oE '^@@ [-+0-9, ]+ @@'`, or state the full line.

**O-2 (low): the "no longer imports" grep misses `import androidx.work.WorkManager`.**
- **Where:** `plan.md:1152`.
- **What:** `WorkManager\.` needs a dot after the name, so it does not match the import line. The blob check `c47a0452…` covers this.
- **Fix:** use `\bWorkManager\b`. It does not match `WorkManagerScheduler` in the KDoc.

**O-3 (low): I7 step 3's expected untracked list omits `compiled-pipeline.json`.**
- **Where:** `plan.md:1244-1245`.
- **What:** `$W/compiled-pipeline.json` is untracked today and would show as `??`, but no row says what to do with it.
- **Fix:** add it to the "plus" list, as "the orchestrator's, recorded, not a stop".

**O-4 (low): section 9 reuses `T`.**
- **Where:** `plan.md:1518`.
- **What:** `T=$(… date …)` reuses the name that 4.0 (`:1003`) gives the U-063 tools folder, which step 10's `sanitize.py` needs.
- **Fix:** rename it to `T0`.

**O-5 (info): the plan still asks for rulings that Record 18 has made.**
- **Where:** `plan.md:208`, `:1381` (D24 (b)), `:1456-1463`, `:1645-1647`.
- **What:** P-1 ("two lines") and P-2 ("fifteen tests") are decided.
- **Fix:** mark them ruled, and delete D24 (b) so that no branch can drop the `:579` edit.

**O-6 (info): close-out corrections for the orchestrator.**
- **Story AC-11's sentence.** "the measured ratio will be below 100%" (`U-069-…md:101`) is false (B-1). Add it to hand-over item 8.
- **The 1.00 floor.** This floor binds U-070 and any later edit of the worker; add it to hand-over item 5.

**O-7 (low): M-T10 can only be undetected through stale build output.**
- **Where:** `plan.md:1283`, M-T10.
- **What:** it relies on incremental KSP and javac deleting `_AssistedFactory` once `@HiltWorker` is gone. If `ic1` passed under M-T10, D22 (a) would stop the story without telling a real escape from a stale class.
- **Fix:** add to D22 (a) for M-T10 only: "first check that `app/build/generated/ksp/debug/java/com/zegoggles/smssync/worker/` has no `BackupTriggerWorker_AssistedFactory.java`, and record the listing".

## Phase Completion Report
---
story_id: "U-069"
phase: "plan-gate/plan-review"
verdict: "FAIL"
artifact_path: "sdlc/artifacts/build/sprints/sprint-024/U-069/plan-review.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
