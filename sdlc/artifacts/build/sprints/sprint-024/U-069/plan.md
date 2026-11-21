---
artifact_type: plan
story_id: "U-069"
gate_id: "planning"
checkpoint: "planning"
verdict: "PASS"
agent: "Solution Architect"
timestamp: "2026-11-21T13:38:22Z"
iteration: 3
base_commit: "4300ffa98370b77dcc4dd8d12142079aada8313a"   # iteration-1 planning HEAD (PLAN_HEAD); iteration 2 re-read at b097d602; the start commit is read at I0 (after U-068 closes)
contracts_verified:
  - CNTR-MODERNIZATION-004
risks_identified: 14
---

# Implementation Plan: U-069 — The Content-Trigger Worker Enqueues the Incoming Backup Through `scheduleIncoming()` (BUG-023, Route A)

Story: `sdlc/artifacts/build/sprints/sprint-024/U-069-content-trigger-backup-goes-through-the-scheduler.md`.
Source bug and binding rulings: `sdlc/artifacts/stories/BUG-023-content-trigger-backup-skips-scheduling-checks.md`
(Suggested Approach, rulings 1 to 7) and the story's "Orchestrator rulings before planning" (Record 16: SC-1 to SC-7).
Models for structure and conventions: `sdlc/artifacts/build/sprints/sprint-023/U-067/plan.md` (with Amendment A1) and
`sdlc/artifacts/build/sprints/sprint-024/U-068/plan.md` (iteration 2). The six iteration-1 blockers of U-068
(`U-068/plan-review.md`) are designed out from the start: every evidence and hand-off list equals what the steps write
(section 4.9 and I7 step 3); every expected count of lines, hunks or hits was derived in this session by running the
command read-only on the real text or on a simulated copy of the post-U-068 text (section 1, L19 and L20); every
end-of-story check is scoped to this story's own paths, because the orchestrator commits documents during the run
(I6 step 4, rows D29 and D30); pristine copies keep their bytes (`cp`, raw SHA-256 next to `git hash-object`); the
mutation protocol's preconditions are restated per entry and hold at every step that uses it (4.8).

**Planning state.** Planned at HEAD `4300ffa9` on `sdlc/modernization-plan`. Gradle, builds, tests and devices were not
run. Story U-068 (minSdk 24) is being built in this checkout now; this story starts after it closes. Every text this
plan edits or mutates was read at `4300ffa9` with `git show`, and the post-U-068 text of the shared files was produced
in a scratch copy by applying U-068's own edits (U-068 plan 3.4 T1 to T3, 3.6 E5.12 to E5.16). Each U-068 old text
occurred exactly once. No step depends on the working tree as it is today.

**The start commit is not fixed by this plan.** `<start>` is `HEAD` at I0, the commit that closes U-068 (status `done`).
The start count `C0`, the Gradle totals and the coverage figures are measured there. No expectation depends on a fixed
test count: the authoritative count was 753 at `4300ffa9`, and U-068 adds 3 before this story starts.

**Size.** 3 tracked files change: `BackupTriggerWorker.kt` (the fix, 25 lines added, 28 deleted, one hunk), `app/build.gradle`
(2 lines in JaCoCo Rule 15, P-1) and one new test file, `BackupTriggerWorkerTest.kt` (15 tests). No other file under
`app/src/main`, no build or dependency file, no contract, design, change record or the scheduler test file changes.
There are **12 mutation runs**: M-T1 to M-T9 as the story lists them, M-T10, the Hilt-annotation detector, M-T11, a
compile-only control, and M-T12, a hard-coded 60 s delay (iteration 2, contract-verification O-3). There are **18
Gradle runs**: I0 2, I2 1, I3 1, I4 1, I5 12 and I6 1. **The developer commits nothing.** The orchestrator commits
after the reviews (section 7, item 1). No step runs in parallel with another: one working tree, foreground Gradle only,
and the mutation protocol edits that tree in place.

## Iteration 2 (after the plan gates, 2026-11-21)

Iteration 1's gates: plan-review **FAIL** (B-1 to B-3, O-1 to O-7), contract-verification PASS (O-1 to O-4),
expert-depth-check PASS with two recommendations (`plan-review.md`, `contract-verification.md`, `expert-depth-check.md`
in this workspace, read in full). Rulings: Record 18, rulings 45 (two build-file lines and fifteen tests, accepted) and
46 (the floor), and the product owner's decision on the endpoint-security event (no external `diff` or `cmp`). Every
change is made in place; the table says where. State of U-068 (updated in iteration 3): the whole story is committed as
seven increments, `533e9f51` (S1-I1), `2aad7d70`, `853483e2`, `c8e98a7f`, `9aa84266` (S1-I5), `7c5e6073` and
`0d490997` (S1-I7). At `0d490997` the worker is still `c37bbbff…`, and the committed scheduler is `0194308c…`, the blob
of L20's simulation.

| Finding | Change, and where |
|---------|-------------------|
| plan-review **B-1** (generated classes report no lines) | **Verified** from the current `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`: `BackupWorker_Factory`, `_AssistedFactory_Impl`, `_AssistedFactory`, `_HiltModule` and the four `RestoreWorker_*` classes are `<class>` elements with no counter; `BackupWorker_Factory.class` has 4 `LineNumberTable` attributes and carries `dagger.internal.DaggerGenerated` (CLASS retention, dagger 2.59.2); JaCoCo 0.8.14's `AnnotationGeneratedFilter` matches the string `Generated`; `BackupTriggerWorker$Companion` has no counter today. New L24. Changed: L17, N6, 2.3 row A1, 2.6, 3.3 (E-G1 and E-G2 texts), 3.5 (`jacoco_classes.py` prints the ruled floor), I4 step 3 (six classes, ratio 1.0000, floor 0.95), D19 (new band), R7, section 7 items 5 and 8. **Floor per ruling 46:** measured 1.00 → `0.95`; the note says what is measured and why the floor is 0.95, and does not say generated classes count. The floor covers `com.zegoggles.smssync.worker*`, which holds only `BackupTriggerWorker` and its generated classes; no later story in this sprint or in U-071 to U-073 edits that package (U-070 edits `service`, `preferences` and `mail` and lists `BackupTriggerWorker.kt` as not touched), so the reviewer's "binds U-070" is corrected in item 5 |
| plan-review **B-2** (IC-3 pass rules) | Section 9 step 9: "nothing to backup" and "no run within the wait" after `enqueued INCOMING` are now **"Not conclusive: enqueued, run not observed"**, not a pass; the reviewer's text is used |
| plan-review **B-3** (`git grep` on an untracked file) | I6 step 4 uses `grep -nE '@Ignore|Assume' $TST`. Every other check re-read for the same mistake: the AC-12 (e) site grep was widened to `git grep --untracked` (superseded in iteration 3, B-4: it is now restricted to the AC-12 files, and the new test file is checked separately); IC-2 (a) and AC-14 already used `--untracked`; AC-10 (a) to (c) search `app/src/main`, where every file is tracked; `oldtext_check.py fixed`, `git hash-object $TST` and `grep -c "@Test" $TST` read the file directly; no other check reads an untracked path through `git grep` |
| plan-review O-1 (hunk header suffix) | I3 step 2 expects the full line `@@ -17,58 +17,55 @@ package com.zegoggles.smssync.worker` (re-derived with `git diff --no-index` on the scratch copies) |
| plan-review O-2 (`WorkManager\.`) | I3 step 2 uses `\bWorkManager\b`; re-run on the scratch copies: 0 lines in the fixed worker, 13 in the old one |
| plan-review O-3 (`compiled-pipeline.json`) | I7 step 3: listed as the orchestrator's, recorded, not a stop |
| plan-review O-4 (`T` reused in section 9) | Section 9 uses `DEVT` for the device time (`T0` is already the Gradle total) |
| plan-review O-5 (rulings made) | P-1 and P-2 marked ruled (Record 18 ruling 45) in 2.3, 8.2, the Verdict and the report; D24 (b) deleted |
| plan-review O-6 (close-out corrections) | Section 7 item 8: the story's AC-11 sentence "the measured ratio will be below 100%" is corrected by the orchestrator at close-out; item 5: the floor (0.95) and which package it covers |
| plan-review O-7 (stale generated class under M-T10) | D22 (a): for M-T10, first list `app/build/generated/ksp/debug/java/com/zegoggles/smssync/worker/` and record whether `BackupTriggerWorker_AssistedFactory.java` is still there |
| contract-verification O-1 (`integration_contracts: []`) | 8.1 and section 7 item 11: the field stays empty because the Artifact Librarian has no method to add a contract to a story after scaffolding; the contract-verification gate checked CNTR-MODERNIZATION-004 regardless (PASS) |
| contract-verification O-2 (receiver stood in for by a direct call) | Stated in 2.2 (AC-8 row); no change needed: `SmsBroadcastReceiver.java:60` makes exactly that call and stays covered by `SmsBroadcastReceiverTest` |
| contract-verification O-3 (Hilt-worker statements; `ac7`'s 60 equals the default) | Section 7 item 7 names DES-MODERNIZATION-008 `:113` and `:204-206`, DES-MODERNIZATION-005 `:261-262` and `:357`, and the trigger worker's type (`Worker`). `ac9_allowedState_…` (75 s) now also asserts `initialDelayMillis` 75 000 on the trigger path; new mutation **M-T12** (a hard-coded 60 s delay in `scheduleIncoming()`) is killed by that assertion only. Test blobs re-derived (L19) |
| contract-verification O-4 (wording) | L20 and 2.1: `:60` says "enqueues a delayed [BackupWorker]" and `:211-212` "enqueues a delayed incoming backup"; 3.4: the registration has "no network constraint and no backoff" (it has the content-URI trigger constraints), and `REPLACE` in the example is not a divergence under INV-1, so the change record needs to correct only the network constraint and the backoff (section 7 item 2) |
| expert-depth criterion 4 | 2.0: Assertion Roulette and Fragile Test cited to Meszaros ch. 16 ("Behavior Smells"; Test Logic in Production stays ch. 15); Ports and Adapters (Cockburn, 2005) named; INV-4 named as a trailing-edge debounce; L12 cites the documented unique-work contract (Android Developers, "Managing work", "Unique Work"; `ExistingPeriodicWorkPolicy.UPDATE` in the API reference) |
| expert-depth criterion 8 | 2.4 items 11 to 13 added (running backup and BUG-024; process death; the periodic trigger, from `SystemJobInfoConverter` read with `javap` in 2.9.1, new L25); section 11 Reliability row extended |
| Product owner's decision (Record 18): no external `diff` or `cmp` | The plan had no `diff` or `cmp` call (checked by `grep`); every comparison is `git diff`, `git diff --no-index` or a SHA-256 or blob comparison. Section 10 gains constraints 13 and 14 (no `diff`/`cmp`; modest shell activity; stop on a vanished tool or killed process). No expected value depended on `diff` output |

Totals after iteration 2: **15 tests** (unchanged; one assertion block added to `ac9_allowedState_…`), **12 mutation
runs** (11 counted detections, M-T11 compile-only), **18 Gradle runs**.

## Iteration 3 (after the iteration-2 plan gates, 2026-11-21)

Iteration 2's gates: plan-review **FAIL** on one new blocker (B-4) with three notes; contract-verification PASS;
expert-depth-check PASS with one recommendation (both reports read). **Every final check was run, not reasoned:** I built
a scratch git repository outside the checkout from `git archive 0d490997` (U-068's last increment), committed it as
`<start>`, applied this plan's edits there (W1 to W3, the new test file, E-G1 and E-G2 with a sample floor comment), and
ran each command of I0 step 5, I3 step 2 and I6 step 4 in it. The outputs are listed in L27; every expected value in
those steps now equals a printed output.

| Finding | Change, and where |
|---------|-------------------|
| plan-review **B-4** (AC-12 (e) prints 5 lines with `--untracked`) | Confirmed by running it: the `--untracked` form prints 5 lines, the fifth `BackupTriggerWorkerTest.kt:402`, the assertion label "request via the trigger worker, against the request via scheduleIncoming()". Fixed by restricting the check to the files AC-12 is about: `git grep -n -E '<pattern>' -- app/src/main app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt sdlc/artifacts/design` (all tracked), which prints exactly the 4 sites; plus a separate `grep -nE '<pattern>' $TST` that prints exactly 1 line, `402:`, recorded as an assertion label, not an AC-12 statement. Same restriction in I0 step 5 (e) (4 lines at `<start>`, run). I6 step 4 and I0 step 5 |
| plan-review note 1 (IC-3 cases after `enqueued INCOMING`) | Section 9 step 9: a backup that starts but does not complete within the wait is "Not conclusive: enqueued, run not completed"; `BackupWorker: unrecoverable error` from the app's process is a **Fail** |
| plan-review note 2 (stale preamble) | Iteration 2 preamble: U-068 is committed as seven increments ending `0d490997` |
| plan-review note 3 (floor slack) | Stands as written (ruling 46); no change |
| expert-depth recommendation (edge case 13 in plain words) | 2.4 item 13 rewritten: the user effect (the first SMS after a quiet spell is backed up about a minute after it arrives; later SMS in the same fifteen-minute period wait until it ends, up to about sixteen minutes with the defaults, and are backed up together; the SMS receiver is not delivered, so this trigger is the only automatic path); the period anchor "fifteen minutes after the last run" marked assumed, not read; the sentence on changes during the trigger run marked "plausible, not verified". New hand-over item 13: an existing behaviour, not changed by this story; the orchestrator measures it on the emulator and raises it as its own bug |
| Own re-check (this is the last automatic iteration) | All final checks run in the scratch repository (L27). No other expected value changed. One fact read anew: the authoritative count at `0d490997` is 756, so the end count would be 771 if nothing lands between (not used by any step; `C0` is still taken at I0) |

Totals unchanged: **15 tests**, **12 mutation runs** (11 counted), **18 Gradle runs**.

## 0. Binding Inputs

| Source | What binds this plan |
|--------|----------------------|
| Story, "Orchestrator rulings before planning" (Record 16) | SC-1: the change record for the contract example at CNTR-004 `:372-376` is the orchestrator's and is not a close condition; at close the story states its number or "not yet raised"; no contract or design file is edited. SC-2: U-067 and U-068 stay in `dependencies`; U-069 runs after U-068. SC-3: the release build is checked by its mapping and class listing (IC-4); the device check (IC-3) runs the debug build. SC-4 to SC-7: information only; the receiver is not touched (Record 15 row 16) |
| BUG-023 Suggested Approach | Ruling 1: Route A, option A1 (`@HiltWorker`, `@AssistedInject`, the port injected, the private builder deleted, name and package unchanged, `Result.success()` always, the log line byte for byte). Ruling 2: no network constraint or backoff on the registration. Rulings 3, 4: PO-2 and PO-1 are not applied. Ruling 5: harness and `WorkSpec` access. Ruling 6: order. Ruling 7: the follow-up sites |
| Story AC-1 to AC-14, IC-1 to IC-4 | As written. Where this plan is stricter (AC-3 value checks, AC-8 (b) and (c) id checks, the second AC-9 test, test `ic1_…`), it says so in 2.3 and 8.2 |
| CNTR-MODERNIZATION-004 v3 | INV-1 to INV-4 (`:240-243`), the INV-4 note (`:245`), Breaking change policy (`:266-267`), Validation Rules 1, 2 and 5 (`:273`, `:274`, `:277`), the example sentence (`:384-385`) |
| U-068 plan (iteration 2) | Its edits to the shared files (L20). Its hand-over item 6: the scheduler sentences that AC-13 (b) relies on stay byte-identical; the test `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` stays unchanged |
| REQ-MODERNIZATION-015 AC-13 (amended by CR-016) | The count rule: start count by the authoritative command at `<start>` with its commit id; end count with the working-tree `--untracked` form before the orchestrator's commit; each added test listed by name; at most 2 skipped |

## 1. Assumption Ledger

`verified` = read or run read-only in this session, with where. `needs_check` = a developer step with a decision row
for each outcome. `plausible, not verified` = not read; named so that a later story checks it. Line numbers are at
`4300ffa9` unless marked "post-U-068" (from the simulated copy, L20).

| # | Claim | Status | Evidence |
|---|-------|--------|----------|
| L1 | Planning blobs at `4300ffa9`: `BackupTriggerWorker.kt` `c37bbbff2176213bc174610b9e9dfc772b1c5d3d` (79 lines); `WorkManagerScheduler.kt` `0ae361ec16c09a3632eef1aff7c6d184344cf4c7` (489 lines); `app/build.gradle` `0ab6ef75326d581f1820ba7e991bc388f2c0fbb2` (594 lines); `WorkManagerSchedulerTest.kt` `ff26b56faec805abafa1c6345857509391268771`; `BackupWorker.kt` `eb535f5c…`; `BackupScheduler.java` `58629d6e…`; `SmsBroadcastReceiver.java` `b67a8dce…`; `AndroidManifest.xml` `1077c3c1…` | verified | `git rev-parse 4300ffa9:<path>` |
| L2 | Line endings: worker, scheduler and the scheduler test are LF in the index and CRLF in the working tree (`core.autocrlf true`); `app/build.gradle` is LF in both. `git hash-object` applies the clean filter, so a working-tree hash equals the index blob. The Edit tool keeps a file's line endings. Python opened with the default `newline` reads CRLF and LF alike | verified | `git ls-files --eol`; `git config core.autocrlf`; the start-mode text check run on CRLF copies (L19) |
| L3 | The worker today: plain `Worker` with a `(Context, WorkerParameters)` constructor (`:48-51`); `Preferences(applicationContext)` (`:54`); the log line (`:57-58`); its own `OneTimeWorkRequest` with delay and tag only (`:61-64`); `enqueueUniqueWork(INCOMING, REPLACE)` (`:66-71`); `Result.success()` (`:73`); `TAG = "SMSBackup+"` (`:77`). U-068 does not edit the file (U-068 plan 8.1 item 5) | verified | file read in full |
| L4 | `scheduleIncoming()` (post-U-068 `:133-158`): the degraded gate and its `blocked` warning (`:134-137`); `timeoutSecs` and the `!isAutoBackupEnabled \|\| timeoutSecs <= 0` gate with its `skipped` line (`:138-143`); the request with `networkConstraints(INCOMING)`, exponential 30 s backoff, the delay and the tag (`:145-150`); `enqueueUniqueWork(INCOMING, REPLACE)` (`:152-153`); the `enqueued INCOMING` line (`:155-156`). U-068 moves every line after `:20` up by one and does not touch this method | verified | scheduler read in full at `4300ffa9`; simulated post-U-068 copy (L20) |
| L5 | The Hilt worker form: `BackupWorker.kt:105-116` (`@HiltWorker`, `class BackupWorker @AssistedInject constructor(@Assisted context: Context, @Assisted params: WorkerParameters, …)`), imports `androidx.hilt.work.HiltWorker`, `dagger.assisted.Assisted`, `dagger.assisted.AssistedInject` (`:51-53`). `HiltWorkerFactory` is the only production factory (`App.java:132` field, `:160-166` configuration, `:232-237` initialise) | verified | files read |
| L6 | androidx.hilt compiler 1.2.0 accepts any `ListenableWorker` subclass: `WorkerStep` checks `ClassNames.LISTENABLE_WORKER` with `XType.isAssignableFrom`; its messages include "@HiltWorker annotated class should contain exactly one @AssistedInject annotated constructor." and "The 'Context' parameter must be declared before the 'WorkerParameters' …". `WorkerElement` names the generated types `<Name>_AssistedFactory` and `<Name>_HiltModule`; `WorkerGenerator` writes a method `bind` | verified | `javap -c -p -constants` and `-v` on `hilt-compiler-1.2.0.jar` (local Gradle cache) |
| L7 | What is generated for a Hilt worker, from the existing build output for `RestoreWorker`: `_HiltModule` (`@Module @InstallIn(SingletonComponent)` interface, `@Binds @IntoMap @StringKey("<fully qualified name>") bind(<Name>_AssistedFactory)`), `_AssistedFactory` (`@AssistedFactory` interface extending `WorkerAssistedFactory<<Name>>`), `_AssistedFactory_Impl` and `_Factory` (Dagger), all under `app/build/generated/ksp/debug/java/<package>/`, plus `hilt_aggregated_deps/_<package>_<Name>_HiltModule.java`. The Hilt component `app/build/generated/hilt/component_sources/debug/com/zegoggles/smssync/DaggerApp_HiltComponents_SingletonC.java` builds the factory map with `MapBuilder…newMapBuilder(2)` and one `mapBuilder.put("com.zegoggles.smssync.service.BackupWorker", …)` line per worker (`:593-595`) | verified (existing build output, read-only) | listing and reading of `app/build/generated/` |
| L8 | `HiltWorkerFactory.createWorker` does `Map.get(className)`, `Provider.get()`, `WorkerAssistedFactory.create(context, params)`; its constructor is package-private. Retention: `HiltWorker` CLASS; `StringKey`, `IntoMap`, `AssistedInject`, `AssistedFactory`, `Assisted` RUNTIME | verified | `javap` on `hilt-work-1.2.0.aar`, `hilt-common-1.2.0.jar`, `dagger-2.59.2.jar` |
| L9 | Release keep rules: `work-runtime-2.9.1.aar` `proguard.txt` has `-keep class * extends androidx.work.Worker` and keeps the public constructors of every `ListenableWorker` subclass; `hilt-work-1.2.0.aar` `proguard.txt` has `-keepnames @androidx.hilt.work.HiltWorker class * extends androidx.work.ListenableWorker`. `app/proguard-rules.pro` names no worker (`-dontoptimize` at `:57`). This closes the story's "not verified" item 5 (first half) | verified | both `proguard.txt` files read; `grep -n -i worker app/proguard-rules.pro` (no line) |
| L10 | WorkManager 2.9.1 API used by the tests: `WorkInfo.getId/getState/getTags/getRunAttemptCount/getConstraints/getInitialDelayMillis`; `WorkManager.getWorkInfosForUniqueWork(String)`, `getWorkInfoById(UUID)` (`ListenableFuture<WorkInfo>`), `cancelUniqueWork(String)` → `Operation.getResult()`; `WorkManagerImpl.getInstance(Context)`, `getWorkDatabase()`; `WorkDatabase.workSpecDao()`; `WorkSpecDao.getWorkSpec(String)`; public `WorkSpec` fields `backoffPolicy`, `backoffDelayDuration`, `input`, `initialDelay`; `Constraints.equals`, `Data.equals`, `WorkInfo.State.isFinished()`. `WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS = 30000` (closes the story's "not verified" item 4) | verified | `javap -p` and `-constants` on `work-runtime-2.9.1.aar` classes |
| L11 | The same accessors in 2.12.0: `WorkInfo.kt:35-39` (`id`, `state`, `tags`), `:65` `constraints`, `:68` `initialDelayMillis`; `WorkManagerImpl.java:168` `getInstance(Context)`, `:276` `getWorkDatabase()`; `WorkDatabase.kt:88` `workSpecDao()`; `WorkSpecDao.kt:64` `getWorkSpec(id: String): WorkSpec?`; `WorkSpec.kt:50`, `:52`, `:55`, `:62`, `:65` (`@JvmField` `input`, `initialDelay`, `constraints`, `backoffPolicy`, `backoffDelayDuration`). The test is written so both compile: `spec!!` and an explicit `WorkInfo?` for `getWorkInfoById` | verified | `work-runtime-2.12.0-sources.jar` (local Gradle cache) |
| L12 | `REPLACE` deletes the replaced work's records, and `KEEP` returns early while an `ENQUEUED` or `RUNNING` entry exists, in 2.12.0 (`EnqueueRunnable.kt:224-249`) and in 2.9.1 (`EnqueueRunnable.class`: `ExistingWorkPolicy.KEEP` test, `CancelWorkRunnable.forName`, two `WorkSpecDao.delete` calls). So after a replacement the first id is absent; AC-8 accepts "absent or `CANCELLED`". `REPLACE` cancels unfinished work, running work included (`CancelWorkRunnable.forName` on the existing chain; 2.4 item 11). The documented contract says the same: Android Developers, "Managing work", section "Unique Work" (`ExistingWorkPolicy` `REPLACE`, `KEEP`, `APPEND`, `APPEND_OR_REPLACE`), and `ExistingPeriodicWorkPolicy.UPDATE` in the WorkManager API reference for the registration (cited as the vendor names them; not fetched in this session) | verified (code); documentation cited | 2.12.0 sources; 2.9.1 `javap -c` |
| L13 | work-testing 2.9.1: `TestListenableWorkerBuilder.setWorkerFactory`, `build()`, the reified Kotlin factory function; `initializeTestWorkManager(Context, Configuration)` uses `LEGACY_OVERRIDE_WITH_SYNCHRONOUS_EXECUTORS`; `TestScheduler` runs a work only when its constraints are met (if it has any) and its initial delay is met (`isRunnableInternalState` reads `hasConstraints`, `getConstraintsMet`, `getInitialDelayMet`). The existing `ac5_scheduleIncoming_withTimeout_isEnqueued` shows `INCOMING` stays unrun in this harness. work-testing 2.12.0 is not in the cache | verified (2.9.1); 2.12.0 plausible, not verified | `javap` on `work-testing-2.9.1.aar`; cache listing |
| L14 | Robolectric 4.15.1: `ShadowLog.getLogsForTag(String)` returns `ImmutableList<ShadowLog.LogItem>`; `LogItem` has public final `type`, `tag`, `msg`, `throwable`; the class carries `@Resetter` (`reset()`). `robolectric.properties` holds only `sdk=29`; no runner setting and no test uses `ShadowLog` today. Guava is on the test compile classpath (Truth 1.4.4's POM, compile scope); `shadows-framework` is an API dependency of `robolectric` | verified | `javap` on `shadows-framework-4.15.1.jar`; `robolectric.properties`; `git grep ShadowLog` (none); Truth POM; `robolectric-4.15.1.module` |
| L15 | `Defaults.INCOMING_TIMEOUT_SECONDS = 60` (`Defaults.java:15`), so the unchanged worker, which reads the real preferences, logs `initialDelay=60s` and delays 60 000 ms under Robolectric | verified | file read |
| L16 | Bindings: `SchedulerModule.kt:28-38` binds `BackupScheduler` to `WorkManagerScheduler` as `@Singleton`; `PreferencesModule.kt:40-43` provides `Preferences` as `@Singleton`; `WorkManagerScheduler` (`:79-83` post-U-068) injects `Context`, `Preferences`, `SecretStore` and not the worker (its only reference is the class literal, post-U-068 `:233`), so there is no cycle | verified | files read |
| L17 | JaCoCo: class roots are the unit-test ASM dir, the javac dir and the Kotlin dir (`app/build.gradle:217-221`); `jacocoFileFilter` (`:232-245`) excludes only `R`, `BuildConfig`, `Manifest*`, `*Test*`, `android/**`. The KSP-generated `_Factory` and `_AssistedFactory_Impl` are in the javac root but **report no lines**: they carry `@DaggerGenerated`, which JaCoCo's generated-annotation filter removes (L24); `_AssistedFactory` and `_HiltModule` are interfaces without code. So after the fix only `BackupTriggerWorker` contributes lines to `worker`, and the expected ratio is 1.00 (iteration 2, plan-review B-1). The Hilt component (`generated/hilt/component_sources`) is not in a root. `worker` was 0/16 lines in 2 classes (`BackupTriggerWorker` with 16 lines, `$Companion` with no counter) at U-061 I9, U-063 and in today's report. No rule names `hilt_aggregated_deps`. `jacocoTestCoverageVerification` writes no report, so the XML needs `:app:jacocoTestReport` | verified | `app/build.gradle` read; `sprint-018/U-061/evidence/I9-jacoco-packages.txt:21`; `sprint-017/U-060/evidence/qa/qa-ac9-class-counts.txt:23`; `sprint-020/U-063/evidence/FIX-jacoco-packages.txt` |
| L18 | Tools: build-tools 36.0.0 has `dexdump.exe` and `aapt2.exe`; no `cmdline-tools` (no `apkanalyzer`); `platform-tools/adb.exe`; GNU `timeout` 8.32 on PATH; `javap.exe` in the JDK 25 `bin`. U-068's helper tools (`sdk_dir.py`, `jacoco_lines.py`) are in `sdlc/artifacts/build/sprints/sprint-024/U-068/evidence/tools/` (uncommitted today; U-068 commits them in its I1) | verified | listings; `dexdump.exe` usage text |
| L19 | Expected text facts, derived by running the plan's own text check (`oldtext_check.py`, 3.5) on a scratch tree made of the `4300ffa9` worker and `app/build.gradle`, the simulated post-U-068 scheduler and scheduler test (LF and CRLF copies): every edited or mutated text occurs exactly once; the pre-24 branch and the pre-24 test name occur 0 times. After W1 to W3 the worker's blob is `c47a0452d040f959df134fc8a71fda5be6b6180f` (76 lines; `--numstat` `25 28`; one hunk `@@ -17,58 +17,55 @@`; package line unchanged); the new test file's blob is `85401cb1f2a1133fa285aaf1bbefd41b0c1bbd93` (456 lines, 15 lines `    @Test`), and `306dee99fc19dc1810d72abeed33a683cbb7edd1` in its red form (one line differs). (Iteration 2: the hunk line is `@@ -17,58 +17,55 @@ package com.zegoggles.smssync.worker` with git's function-context suffix; the test blobs changed with the 75 s delay assertion in `ac9_allowedState_…`; iteration 1's `6e4437bd…` and `5a57298c…` no longer apply) | verified (simulation) | scratch copies; `git hash-object`; `git diff --no-index --numstat` |
| L20 | Post-U-068 text of the shared files, by applying U-068's edits to the `4300ffa9` blobs: scheduler E5.12 to E5.16 (each once), 481 lines, simulated blob `0194308cfcf9ff34effaf08c7990c087b0bedcbd`; the AC-13 (b) sentences, byte-identical: `:60` "[BackupTriggerWorker] enqueues a delayed [BackupWorker] rather than backing up directly." and `:211-212` "the triggered [BackupTriggerWorker] enqueues a delayed / incoming backup rather than backing up directly." (iteration 2 wording, contract-verification O-4); scheduler test T1 to T3 (each once): 25 `@Test`, `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` `:380`, `ac5_scheduleIncoming_autoBackupDisabled_returnsNull` `:457`, `ac5_scheduleIncoming_zeroTimeout_returnsNull` `:470`, `se002_scheduleIncoming_encryptionDegraded_returnsNullNoWork` `:517`, the comment "would ultimately call scheduleIncoming()" `:548`. `app/build.gradle`: U-068 edits `:24`, `:153`, `:160` in place, so Rule 15 stays at `:575-588` (`:579` note, `:586` floor). Root `build.gradle:30` becomes `reports.junitXml.required = true` | verified (simulation) | U-068 plan 3.1, 3.4, 3.5, 3.6 applied to scratch copies |
| L21 | AC-10, IC-2 and AC-12 greps, derived on the simulated post-story texts: see I0 step 5 and I6 step 4 for each expected output | verified (simulation) | `grep -n` on the scratch tree; the `4300ffa9` forms by `git grep` |
| L22 | WorkManager's factory failure texts: "Could not instantiate " (`WorkerFactory`) and "Could not create Worker " (`WorkerWrapper`). `BackupWorker`'s lines: "BackupWorker: starting backup (%d messages)" (`:314`), "BackupWorker: backup complete, backedUp=" (`:370`), "BackupWorker: unrecoverable error" (`:169`) | verified | `javap -v` constant pools; `git grep` |
| L23 | Planning-time facts not used by any step: count 753 at `4300ffa9`; `@Ignore` on code lines 2 (`AppTest.java:13`, `AuthPreferencesTest.java:1079`); `SmsBroadcastReceiverTest` 4 tests with a mocked port | verified | the authoritative command; `git grep` |
| L24 | (Iteration 2.) In today's `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml` the classes `BackupWorker_Factory`, `BackupWorker_AssistedFactory_Impl`, `BackupWorker_AssistedFactory`, `BackupWorker_HiltModule` and the four `RestoreWorker_*` classes have no counter; `BackupTriggerWorker$Companion` has none; `BackupTriggerWorker` has `LINE missed=16 covered=0`. `BackupWorker_Factory.class` and `_AssistedFactory_Impl.class` (javac root) carry `dagger.internal.DaggerGenerated`, whose retention is CLASS (`dagger-2.59.2.jar`); `BackupWorker_Factory.class` has 4 `LineNumberTable` attributes, so the lines exist and are filtered, not absent. JaCoCo 0.8.14's `AnnotationGeneratedFilter` holds the constant `Generated` | verified | the XML read with Python, read-only; `javap -v` on the class files and on `dagger.internal.DaggerGenerated`; `javap -c -constants` on `org.jacoco.core-0.8.14.jar` |
| L25 | (Iteration 2.) WorkManager 2.9.1 `SystemJobInfoConverter.convert`: a periodic `WorkSpec` becomes a one-shot `JobInfo` (no `setPeriodic` call) with `setMinimumLatency(max(calculateNextRunTime() − now, 0))` and, when the constraints have content-URI triggers, `addTriggerContentUri` per URI plus `setTriggerContentUpdateDelay` and `setTriggerContentMaxDelay`. So after a run the next `CONTENT_TRIGGER` job waits at least until its next period (15 minutes after the last run) and for a content change | verified | `javap -c -p` on `work-runtime-2.9.1.aar`'s class |
| L26 | (Iteration 2.) Record 18: ruling 45 accepts two build-file lines and fifteen tests; ruling 46 sets the floor to the measured ratio rounded down to 0.05, and to 0.95 when the measurement is 1.00; the product owner's decision continues the builds with Git's built-in comparison (no external `diff` or `cmp`). No later story in sprint 024 or U-071 to U-073 edits `app/src/main/java/com/zegoggles/smssync/worker/` (U-070's story lists `BackupTriggerWorker.kt` under "Not touched", `:169`; its Affected Code is in `service`, `preferences`, `mail`) | verified | `thunderbird-mail-libraries-feasibility-analysis.md` Record 18; `git grep` of the stories at `b097d602` |
| L27 | (Iteration 3.) Outputs of the final checks, run in a scratch git repository built from `git archive 0d490997` (committed as `<start>`) with this plan's edits applied: `oldtext_check.py start` mismatches=0 before the edits, `fixed` mismatches=0 after; blobs `c47a0452…` (worker) and `85401cb1…` (test); worker `--numstat` `25 28`, one hunk `@@ -17,58 +17,55 @@ package com.zegoggles.smssync.worker`, header-line count 2; AC-10 (a) no line, exit 1; AC-10 (b) one line `WorkManagerScheduler.kt:153`; AC-10 (c) 6 lines `:146`, `:149`, `:153`, `:156`, `:157`, `:421`; the import grep no line, exit 1 (13 lines on the old worker); porcelain ` M app/build.gradle`, ` M …/BackupTriggerWorker.kt`, `?? …/BackupTriggerWorkerTest.kt`; IC-2 (a) 15 lines (scheduler `:29`, `:60`, `:62`, `:211`, `:233`; worker `:52`, `:63`; test `:63`, `:306`, `:323`, `:344`, `:345`, `:367`, `:451`, `:453`); AC-12 (e) restricted 4 lines (worker `:41`, scheduler test `:548`, CNTR-004 `:384`, DES-005 `:135`), on the new test file 1 line (`:402`), and 5 lines in the iteration-2 `--untracked` form (B-4 confirmed); AC-13 (a) only the worker; (b)(c) exit 0; (d) ` app/build.gradle | 4 ++--`, ` 1 file changed, 2 insertions(+), 2 deletions(-)`; (e)(f) exit 0; `@Ignore`/`Assume` no line, exit 1; AC-12 design and change-record checks empty; no code commit after `<start>`; count 756 at `<start>`, 771 in the working tree (= +15), `app/src/test` porcelain only the new file, 15 `@Test` in it; `app/build.gradle` `--numstat` `2 2`, four changed lines, 2 `minimum =` lines, stale phrases no line, exit 1. The I0 step 5 greps at `<start>`: (a) 4 lines `:20`, `:35`, `:61`, `:67`; (b) 1 line `:153`; (c) 8 lines; (d) 7 lines; (e) restricted 4 lines with the worker at `:39` | verified (run) | the scratch repository; one read-only script |
| N1 | The test file compiles against WorkManager 2.9.1, Truth 1.4.4, Robolectric 4.15.1 and Mockito 5.18.0 | needs_check | I2; row D13 |
| N2 | `ShadowLog` records the worker's `Log.d` with tag `SMSBackup+` in this Robolectric set-up | needs_check | I2 (the degraded AC-9 test must fail only at its scheduler-line assertion); row D14 (a) |
| N3 | `WorkManagerImpl.getInstance(context).workDatabase.workSpecDao().getWorkSpec(id)` returns the spec in the harness | needs_check | I2 (AC-3 must fail only at its equality assertion); row D14 (b) |
| N4 | The harness leaves `INCOMING` work `ENQUEUED` (no constraint met, delay not met) | needs_check | I2 (AC-7 passes on the unchanged worker); row D14 (c) |
| N5 | KSP generates the four worker classes and the aggregated-deps file, and both Hilt components (debug, release) get the map entry `mapBuilder.put("com.zegoggles.smssync.worker.BackupTriggerWorker", …)` with `newMapBuilder(3)` | needs_check | I4 step 2, I6 step 3; rows D17, D18 |
| N6 | The measured `worker` ratio: expected `covered = total`, ratio 1.0000 (only `BackupTriggerWorker.kt` reports lines, L17, L24; every line runs in the tests), so the ruled floor is 0.95 (L26). Iteration 1's "about 0.35" rested on counting the generated classes and is withdrawn | needs_check | I4 step 3; row D19 |
| N7 | M-T10 (`@HiltWorker` removed) compiles: Dagger accepts an `@AssistedInject` constructor with no `@AssistedFactory` | needs_check | I5; row D22 |
| N8 | M-T11 (`@AssistedInject` removed, `@HiltWorker` kept) fails the KSP step | needs_check | I5; row D23 |
| N9 | WorkManager accepts `setInitialDelay(-1, SECONDS)` under M-T4 (if it throws, the negative-delay test errors instead of failing; both count as detection) | needs_check | I5 M-T4; row D22 |
| N10 | R8 keeps `com.zegoggles.smssync.worker.BackupTriggerWorker` self-mapped and in the release dex; the dotted map-key string is in the release dex | needs_check | I6 step 5; rows D26, D27 |
| P1 | work-testing 2.12.0 keeps `TestListenableWorkerBuilder`'s API (U-073); androidx.hilt 1.4.0 keeps the generated names and `bind` (U-071); Robolectric 4.18 keeps `ShadowLog.getLogsForTag` and `LogItem` (U-071) | plausible, not verified | not in the cache; hand-over 7 items 3 and 4 |

## 2. Design (decided)

### 2.0 Principles applied

Book chapters are cited as commonly given; I did not re-read them in this session.

- **One source of truth for a rule: "The Evils of Duplication"** (Hunt and Thomas, *The Pragmatic Programmer*, ch. 2,
  DRY) and the smell **Duplicated Code** (Fowler, *Refactoring*, 2nd ed., ch. 3). BUG-023 is two builders of the same
  `INCOMING` request that drifted apart: one applies the network, degraded, auto-backup and delay rules, the other does
  not. The fix deletes the second builder, and AC-3's equality test is the fitness function that keeps it deleted.
- **Dependency Inversion** (Martin, *Agile Software Development: Principles, Patterns, and Practices*, ch. 11) and
  **Constructor Injection** (Seemann and van Deursen, *Dependency Injection: Principles, Practices, and Patterns*,
  ch. 4). The worker depends on the `BackupScheduler` port, not on `WorkManagerScheduler`; CNTR-004 Rule 1 and the port
  surface stay untouched. The rejected option A2 (`App.getScheduler(…)`) is the **Service Locator** anti-pattern of the
  same book (ch. 5): a hidden dependency that is null under Robolectric, so the worker could not be tested without a
  new seam, which would be **Test Logic in Production** (Meszaros, *xUnit Test Patterns*, ch. 15, "Code Smells").
  The port and its WorkManager adapter are the **Ports and Adapters** style (Cockburn, "Hexagonal Architecture (Ports
  and Adapters)", 2005): `BackupScheduler` is the driven port, `WorkManagerScheduler` its adapter, and after this story
  both incoming triggers (the SMS receiver and the content-URI worker) are primary actors that reach the incoming rule
  only through the port.
- **A trailing-edge debounce** (INV-4). Stage 1 is the content-URI change; stage 2 is a delayed one-time request whose
  unique name and `REPLACE` policy coalesce a burst of changes into one backup that runs `incomingTimeoutSecs` after the
  last of them (the documented unique-work contract, L12). AC-7 asserts the delay, AC-8 the coalescing; 2.4 item 13 says
  how the periodic trigger's own period adds a rate limit in front of it.
- **A sociable unit test through the real collaborator** (Fowler, bliki "UnitTest", solitary versus sociable;
  Freeman and Pryce, *Growing Object-Oriented Software, Guided by Tests*, ch. 8, "Only Mock Types That You Own").
  The tests run the real `WorkManagerScheduler` over the WorkManager test harness and assert the request that lands in
  WorkManager, not a mock interaction. A mock `verify(scheduler).scheduleIncoming()` would pass even if a second builder
  came back beside the call; only the state-based assertion of AC-1 to AC-3 sees the request itself.
- **Published versus public interface** (Fowler, "Public versus Published Interfaces", *IEEE Software*, 2002). The worker's
  fully qualified class name is published: devices store it in their `WorkSpec`, and `HiltWorkerFactory` looks it up as a
  string key. The class name and package therefore do not change, and test `ic1_…` pins both the name and the key.
- **Logs as an operational interface** (Nygard, *Release It!*, 2nd ed., ch. 10, "Transparency"). The trigger line is read
  by CR-015's emulator record, BUG-021's AC-12 and DES-014's Run R, so it is kept byte for byte and asserted at debug
  level; the scheduler's own `blocked` or `skipped` line after it keeps the log truthful when the rule declines.
- **Test-first evidence and mutation analysis** (Beck, *Test-Driven Development: By Example*, part I, "red, green";
  DeMillo, Lipton and Sayward, "Hints on Test Data Selection", *IEEE Computer*, 1978). The new tests run first against
  the unchanged worker (I2), and each rule they pin has a mutant they must kill (I5). Assertion messages name what each
  assertion checks, against the **Assertion Roulette** smell (Meszaros, ch. 16, "Behavior Smells"), so a red result
  shows which rule failed.
- **Coverage floor as a ratchet** (Ford, Parsons and Kua, *Building Evolutionary Architectures*, ch. 2, fitness
  functions). The `worker` floor is raised on measurement, with no new exclusion: the measured ratio rounded down to
  0.05, and 0.95 when the measurement is 1.00 (Record 18 ruling 46), so a line-counting difference in a later JaCoCo
  version cannot fail a gate that no story may lower.

### 2.1 The fix and why this form

**Production form** (exact text in 3.1). `@HiltWorker` on the class and
`@AssistedInject constructor(@Assisted context: Context, @Assisted params: WorkerParameters, private val scheduler: BackupScheduler, private val preferences: Preferences) : Worker(context, params)`,
the form of `BackupWorker.kt:105-116` (L5). `Worker` stays (option A5 rejected): androidx.hilt accepts any
`ListenableWorker` subclass (L6), `scheduleIncoming()` is synchronous, and a `Worker` already runs `doWork()` on
WorkManager's executor. `doWork()`:
1. reads `preferences.incomingTimeoutSecs` once, for the log line only. The worker makes no decision on any value, so
   every rule is decided inside `scheduleIncoming()`, which reads its inputs itself. That is how this plan reads "reads
   nothing itself that `scheduleIncoming()` already reads": the line must print `<n>`, so this one read stays;
2. logs the unchanged line (two source lines byte-identical to `:57-58`; an `Int` prints like the old `Long`, story
   correction 1);
3. calls `scheduler.scheduleIncoming()`;
4. returns `Result.success()` whatever the call returns (ruling 1). An exception from the call is not caught: WorkManager
   records the failed run as it did when the old enqueue threw. The declined cases are not exceptions.

**What KSP and Hilt generate, and where** (L6, L7; checked at I4 step 2 and I6 step 3, N5):
- androidx.hilt compiler 1.2.0, into `app/build/generated/ksp/debug/java/com/zegoggles/smssync/worker/`:
  `BackupTriggerWorker_AssistedFactory.java` and `BackupTriggerWorker_HiltModule.java` (binding the factory under
  `@StringKey("com.zegoggles.smssync.worker.BackupTriggerWorker")`), plus
  `app/build/generated/ksp/debug/java/hilt_aggregated_deps/_com_zegoggles_smssync_worker_BackupTriggerWorker_HiltModule.java`;
- Dagger 2.59.2, in the same folder: `BackupTriggerWorker_Factory.java` and `BackupTriggerWorker_AssistedFactory_Impl.java`;
- the Hilt component, in `app/build/generated/hilt/component_sources/{debug,release}/com/zegoggles/smssync/DaggerApp_HiltComponents_SingletonC.java`:
  a third `mapBuilder.put("com.zegoggles.smssync.worker.BackupTriggerWorker", …)` in the map that `HiltWorkerFactory`
  receives.

**Runtime path after the fix.** A content change runs `CONTENT_TRIGGER` → WorkManager calls
`HiltWorkerFactory.createWorker(context, "com.zegoggles.smssync.worker.BackupTriggerWorker", params)` → map lookup →
the generated assisted factory → `BackupTriggerWorker(context, params, <the singleton port>, <the singleton Preferences>)`
→ `doWork()` → `scheduleIncoming()` (degraded gate, auto-backup and delay gate, the one builder, `REPLACE`) →
`Result.success()`. The work persisted on a device names the same class, so the first trigger after the upgrade
resolves through the same lookup with nothing registered again (IC-2 (b)).

**Nothing else under `app/src/main` changes** (AC-13): the scheduler, `BackupWorker.kt`, the port, the receiver and the
manifest show empty diffs (I6 step 4). The scheduler comments `:60` ("enqueues a delayed [BackupWorker]") and
`:211-212` ("enqueues a delayed / incoming backup") (post-U-068) stay true: the worker enqueues it through the port.

### 2.2 Test design

New class `app/src/test/java/com/zegoggles/smssync/worker/BackupTriggerWorkerTest.kt` (the folder is new; U-073's
AC-13 needs this path, hand-over item 3). The harness copies `WorkManagerSchedulerTest.setUp` (`:53-81`): Robolectric at
SDK 29, `WorkManagerTestInitHelper` with a single-thread executor and `BackupWorker.TestableBackupWorkerFactory()`
(which returns null for other classes), mocked `Preferences`, `DataTypePreferences` and `SecretStore` with the same
happy-path stubs, and a real `WorkManagerScheduler(context, preferences, secretStore)`. The worker is built by
`TestListenableWorkerBuilder<BackupTriggerWorker>(context).setWorkerFactory(TriggerWorkerFactory())`; the test factory
returns `BackupTriggerWorker(appContext, workerParameters, scheduler, preferences)`, as `HiltWorkerFactory` does with the
injected objects. `doWork()` is called directly. Every test first calls `registerContentTrigger()` (asserts a non-null
job) with the allowed state, then changes the one stub it is about, then runs the worker. No `TestDriver` is used.

| AC | Test(s) | How it is read | Stricter than the story? |
|----|---------|----------------|--------------------------|
| AC-1, AC-2 | `ac1_wifiOnlyOn_…`, `ac2_wifiOnlyOff_…` | `getWorkInfosForUniqueWork("INCOMING").get()`: exactly one entry; `constraints.requiredNetworkType` `UNMETERED` / `CONNECTED`; result success | No |
| AC-3 | `ac3_wifiOnlyOn_…`, `ac3_wifiOnlyOff_…` | `captureActiveIncomingRequest()` after the worker; `cancelUniqueWork("INCOMING").result.get()`; `scheduleIncoming()`; capture again; the two `IncomingRequest` values (tags, `initialDelayMillis`, `constraints` from `WorkInfo`; `backoffPolicy`, `backoffDelayDuration`, `input` from the `WorkSpec`, `@SuppressLint("RestrictedApi")`) are equal | Yes: the equal pair is also checked against the contract values (network per Wi-Fi only, `EXPONENTIAL`, 30 000 ms, 60 000 ms, tag `INCOMING`), so "equal but both wrong" cannot pass (P-4) |
| AC-4 to AC-6 | `ac4_…`, `ac5_…`, `ac6_incomingDelayZero_…`, `ac6_incomingDelayNegative_…` | no `INCOMING` entry; result `Result.success()` (these are AC-9's three declined cases) | No |
| AC-7 | `ac7_debounce_…` | one entry, state `ENQUEUED`, `runAttemptCount` 0, `initialDelayMillis` 60 000 | Adds the run-attempt check. The story fixes 60, which equals the default (L15); a hard-coded 60 is caught by `ac9_allowedState_…` (75 s, M-T12), not by `ac7` |
| AC-8 | `ac8a_…`, `ac8b_…`, `ac8c_…` | after the second request: exactly one non-finished entry; its id differs from the first; the first is absent or `CANCELLED` (L12). The receiver path is stood in for by a direct `scheduler.scheduleIncoming()` call, the exact call `SmsBroadcastReceiver.java:60` makes; the receiver itself stays covered by `SmsBroadcastReceiverTest` | Yes for (b) and (c): the story asks only "exactly one"; with `KEEP` (M-T6) that still holds, so the id check is what lets (b) and (c) see M-T6 (P-3) |
| AC-9 | `ac9_allowedState_…` and `ac9_encryptionDegraded_…` | `ShadowLog.getLogsForTag("SMSBackup+")`: the trigger line exactly once, at `Log.DEBUG`, byte for byte; the scheduler's `enqueued INCOMING` line (allowed) or `blocked` line (degraded) after it; result success. The allowed test also asserts one `INCOMING` entry with `initialDelayMillis` 75 000 (iteration 2) | Yes: the allowed test sets `incomingTimeoutSecs` 75 after registration, so `<n>` and the delay are shown to come from the injected preferences at run time, not the default; the degraded test pins the SC-4 statement that the scheduler's reason follows the line (P-5) |
| IC-1, IC-2 (b) | `ic1_hiltWiring_assistedFactoryIsBoundUnderThePersistedClassName` | reflection on the generated classes (RUNTIME retention, L8): `_AssistedFactory` exists, extends `WorkerAssistedFactory`, carries `@AssistedFactory`; `_HiltModule.bind(_AssistedFactory)` carries `@IntoMap` and `@StringKey("com.zegoggles.smssync.worker.BackupTriggerWorker")`; the worker's class name is that string; exactly one `@AssistedInject` constructor with `(Context, WorkerParameters, BackupScheduler, Preferences)` in order | New (P-2). It is the unit-level detector the story's SC-6 says does not exist (section 2.5) |

Counts: 15 tests, all in the new class. Red run (I2, unchanged worker): 11 fail, 4 pass (5.2).

### 2.3 Decisions and trade-offs

| Decision | Gained | Gives way | Not chosen | Why |
|----------|--------|-----------|------------|-----|
| A1: `@HiltWorker`, port injected (ruled) | The pattern the code base already uses for both workers; the worker is testable with any port; one builder | Four generated classes join the `worker` package (they report no lines, L24, so the ratio is the worker's own); the wiring is only fully proven on a device (IC-3) | A2 static bridge; A3 `@EntryPoint`; A4 shared builder; A5 `CoroutineWorker` | Ruled (bug ruling 1). A2 is a Service Locator that is null under Robolectric; A3 is a new pattern with the same test problem; A4 leaves two gate sites; A5 adds nothing |
| Inject `Preferences` for the log value | `<n>` comes from the same singleton the scheduler reads; the tests control it | One read that the scheduler repeats (the two reads can differ only if the setting changes between them, which the log then shows truthfully) | (a) keep `Preferences(applicationContext)`; (b) drop `<n>` from the line | (a) reads real shared preferences that the test cannot stub, the per-call construction U-022 removed (ARCH-002); (b) breaks AC-9 and the three readers of the line |
| `Result.success()` always (ruled) | A declined enqueue is the rule working; no retry storm; the periodic trigger is never marked failed | A caller cannot tell "declined" from "enqueued" by the result; the log line tells it | Failure or retry on null | Ruled (bug ruling 1); M-T8 pins it |
| Red run before the fix, with a two-argument factory line | Evidence that 11 of the 15 tests see the defect, and probes N1 to N4 before production code moves | One extra edit (T-F) and one Gradle run | Writing the tests after the fix; a reflective factory that adapts to either constructor | The first gives no red evidence (Verification Step 2); the second puts logic in the test that itself needs testing |
| Backoff and input read from the internal `WorkSpec` | AC-3 compares all six fields the story names | The test depends on an internal API (a **Fragile Test** risk, Meszaros ch. 16): one helper, verified in 2.9.1 and 2.12.0 (L10, L11) | Public `WorkInfo` only | `WorkInfo` has neither field in either version; the story requires them |
| Test `ic1_…` added (P-2, ruled: Record 18 ruling 45) | A unit-level failure when `@HiltWorker` is removed; the persisted name and the map key pinned | Couples one test to androidx.hilt's generated names (`_AssistedFactory`, `_HiltModule`, `bind`), stable since hilt-work 1.0 and read in 1.2.0 (L6); U-071 moves to 1.4.0 (P1) | Leave it to IC-3 | IC-3 runs once on one emulator; a unit test runs in every gate. Reflection, not direct references, so a missing class fails the test instead of the compile, which keeps it a valid mutation detector |
| `WorkManagerSchedulerTest.kt:548` not edited | The scheduler test file shows an empty diff (simplest proof of AC-13 (e); U-073 AC-13 unaffected) | The comment stays in the conditional ("would ultimately call") | Present tense | AC-12 (c): the sentence is true after the fix and the tense is optional |
| Rule 15: `:586` floor line **and** the `:579` note (P-1, ruled: Record 18 ruling 45) | No false sentence is left in the rule ("BackupTriggerWorker has no unit tests; floor stays at 0%") | `app/build.gradle` shows 2 changed lines, not 1 | One line only | The story allows "this rule's `minimum` value and its comment"; leaving `:579` would put a false statement in the build file |
| Floor 0.95 under a measured 1.00 (Record 18 ruling 46) | A later JaCoCo version that counts a line differently (U-071 raises JaCoCo) cannot fail a gate that no story may lower | One uncovered line in the worker could go unnoticed by the floor (the 15 tests and the mutants still see it) | 1.00 | Ruled |

### 2.4 Edge cases (behaviour after the fix)

1. **Encryption degraded after registration:** each trigger logs the trigger line and the scheduler's `blocked` line;
   no `INCOMING` work; result success (AC-4, second AC-9 test). `CONTENT_TRIGGER` stays registered until the next
   `rescheduleJobs()`; that is unchanged.
2. **Auto backup switched off:** the trigger path now declines (AC-5). A pending `INCOMING` enqueued before the switch
   still runs (PO-1, unchanged on both paths, `cancelAll()` untouched).
3. **Delay 0 or negative:** declined, no zero-delay backup (AC-6); the line prints `initialDelay=0s` or `-1s` and the
   `skipped` line follows.
4. **Wi-Fi only on:** the backup now waits for an unmetered network, as on the receiver path. On mobile data alone an
   incoming backup is delayed until Wi-Fi; that is the setting working (story "So that").
5. **Two triggers inside the delay, or trigger and receiver:** one `INCOMING` survives and the newest request's delay
   applies (`REPLACE`; AC-8 (a) to (c)).
6. **Call-log trigger:** the same worker, the same follow-up and the same rules (Existing Behavior to Preserve).
7. **The first trigger after the upgrade:** the persisted `CONTENT_TRIGGER` names the same class; `HiltWorkerFactory`
   resolves it (IC-2 (b), IC-3). If the factory could not, WorkManager would fall back to reflection, find no
   `(Context, WorkerParameters)` constructor and log "Could not instantiate …"; IC-3 searches for that.
8. **A device below minSdk 24:** cannot install the build at all (U-068); not a case here.
9. **`scheduleIncoming()` throws** (for example, WorkManager not initialised): the exception propagates as before; not
   caught, not one of AC-9's cases.
10. **Release build:** the class name survives R8 under two keep rules (L9); IC-4 checks the mapping and the dex.
11. **A trigger while the incoming backup is running:** `REPLACE` cancels unfinished work, running included (L12:
    `EnqueueRunnable` → `CancelWorkRunnable.forName`), so the running `BackupWorker` is stopped and a new delayed request
    replaces it. Since story U-067 the stopped backup really stops at its next pass, and until BUG-024 is fixed it is
    logged and reported as a failure (`sdlc/artifacts/stories/BUG-024-cancelled-backup-or-restore-reported-as-failure.md`).
    This is unchanged by this story: the old worker and the receiver both used `REPLACE`. AC-8 covers the `ENQUEUED` case
    only; the `RUNNING` case is recorded for BUG-024.
12. **Process death inside `doWork()`:** if the process dies before `enqueueUniqueWork` commits, no `INCOMING` work
    exists. The `CONTENT_TRIGGER` run is rescheduled at WorkManager's next initialisation and its job again carries the
    content-URI triggers, so it may wait for the next content change; the message is then backed up by the next trigger or
    by the regular backup. Unchanged: the old worker had the same window.
13. **The periodic content trigger:** `CONTENT_TRIGGER` is a `PeriodicWorkRequest` with content-URI triggers at the
    15-minute minimum interval, registered with `UPDATE`. WorkManager 2.9.1 turns it into a one-shot `JobInfo` with
    `setMinimumLatency` up to the next period plus the content-URI triggers (L25, read with `javap`), so stage 1 can fire
    at most once per period. The period's anchor, "fifteen minutes after the last run", is **assumed, not read**: it is
    how `WorkSpec.calculateNextRunTime()` is commonly described, and that method's body was not read in this session. A
    change that lands inside the latency window is expected to run the trigger when the window ends (JobScheduler's
    content-trigger behaviour; plausible, not verified). Changes that land while the trigger worker itself runs are seen
    by the next scheduled job (**plausible, not verified**).
    **What the owner sees, in plain words.** The SMS-received receiver is not delivered on Android 11 and newer (BUG-023,
    Record 15 row 16), so this trigger is the only automatic path for incoming messages. The first SMS after a quiet
    spell is backed up about a minute after it arrives (the 60-second default delay). Later SMS that arrive within the
    same fifteen-minute period wait until the period ends, up to about sixteen minutes with the defaults (the rest of the
    period plus the 60 seconds), and are then backed up together. This behaviour existed before this story and is not
    changed by it; it is recorded for DES-005 and handed to the orchestrator (section 7 items 7 and 13).

### 2.5 Which check detects which failure

| Failure | Unit tests | Build | IC-4 (release mapping, dex) | IC-3 (device) |
|---------|------------|-------|------------------------------|---------------|
| A second builder of `INCOMING` comes back (M-T1) | AC-1 to AC-4, AC-5, AC-6, AC-9 tests | — | — | — |
| A rule dropped in `scheduleIncoming()` (M-T2 to M-T6) | the AC test of that rule, plus the existing scheduler test where one exists | — | — | — |
| The delay hard-coded to the default 60 s (M-T12) | `ac9_allowedState_…` (75 s) | — | — | — |
| The worker stops calling the scheduler (M-T7) | AC-1 to AC-3, AC-7, AC-8, AC-9 | — | — | yes (no scheduler line) |
| **`@HiltWorker` removed (M-T10)** | **`ic1_…`** (generated classes missing) | compiles (N7) | no: both keep rules still keep the name | yes: "Could not instantiate …" on the first trigger |
| **`@AssistedInject` removed (M-T11)** | not reached | **the KSP step fails** (hilt-compiler's message, L6) | — | — |
| Class renamed or moved | `ic1_…` (name pinned) | — | yes (self-mapping absent) | yes |
| R8 renames or removes the class | — | — | **yes** | not run on a release build (SC-3) |
| The component does not include the module | — | — | — | **yes**; I4 step 2 and I6 step 3 also read the generated component for the map entry |

So, made explicit for SC-6: removing `@HiltWorker` is detected by test `ic1_…` (and by IC-3); removing `@AssistedInject`
is detected by the build's KSP step; IC-4 detects neither, because the worker's name is kept by WorkManager's own rule
either way, and it is the detector only for a release-build rename or removal.

### 2.6 What each comparison can and cannot show

| Check | Can show | Cannot show |
|-------|----------|-------------|
| Red run (I2) | That 11 tests see the defect on the unchanged worker; probes N1 to N4 | Anything about the fixed code |
| Green run and gates (I3, I4, I6) | The rules hold at run time on the trigger path; INV-1 to INV-4 for its request; the scheduler tests unchanged | WorkManager's real scheduler and constraint trackers (the harness's `TestScheduler` replaces them) |
| Mutations (I5) | That each test is load-bearing for the rule it names | Faults outside the 12 mutants |
| Generated sources (I4, I6) | That KSP and Hilt wired the worker into the component's map, debug and release | That the map is used at run time on a device |
| The `worker` coverage ratio (I4, I6) | That every line of `BackupTriggerWorker.kt` runs in the tests | Anything about the generated classes: JaCoCo filters them (L24) |
| IC-4 | The class name survives R8 in the release APK; the key string survives | That the release build runs on a device |
| IC-3 | The production factory builds the changed worker on API 36 and the trigger reaches the scheduler | Release builds, Android 11-era devices, the owner's phones |

## 3. Exact Changes

Every edit uses the Edit tool on a file read first; copy `old_string` from the Read output (the worker is CRLF in the
working tree; the Edit tool keeps it). Paths: `S=app/src/main/java/com/zegoggles/smssync`,
`TS=app/src/test/java/com/zegoggles/smssync`. Before any edit, `oldtext_check.py start` (I0 step 2) has shown that each
`old_string` below occurs exactly once; if the Edit tool still reports "not found" or "not unique", row D4 applies.

### 3.1 `$S/worker/BackupTriggerWorker.kt` (three edits, I3 step 1)

**W1 (imports, `:18-27`).** `old_string`:
```kotlin
import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Worker
import com.zegoggles.smssync.preferences.Preferences
import com.zegoggles.smssync.service.BackupType
import com.zegoggles.smssync.service.BackupWorker
import java.util.concurrent.TimeUnit
```
`new_string`:
```kotlin
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import androidx.work.Worker
import com.zegoggles.smssync.preferences.Preferences
import com.zegoggles.smssync.scheduler.BackupScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
```

**W2 (class comment and header, `:29-51`).** `old_string` (the `—` is U+2014 and the `§` is U+00A7, as in the file):
```kotlin
/**
 * Content-URI trigger worker — implements the INV-4 two-stage debounce.
 *
 * This worker is enqueued by the CONTENT_TRIGGER [androidx.work.PeriodicWorkRequest]
 * (set up by [com.zegoggles.smssync.scheduler.WorkManagerScheduler.scheduleContentTrigger]).
 * It does NOT perform a backup directly; instead it enqueues a delayed
 * [OneTimeWorkRequest] for [BackupWorker] with [initialDelay = getIncomingTimeoutSecs()].
 *
 * This preserves the two-stage debounce (INV-4):
 * - Stage 1: content-URI change fires this trigger worker
 * - Stage 2: this worker enqueues the delayed incoming backup via scheduleIncoming()
 *
 * The worker does NOT back up on the raw content change (INV-4, DES-MODERNIZATION-005
 * §Incoming-SMS trigger).
 *
 * U-015 dependency: once BackupWorker is a CoroutineWorker with full execution logic,
 * this trigger worker's follow-up enqueue will invoke the real backup. No changes to this
 * worker are required by U-015.
 */
class BackupTriggerWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
```
`new_string`:
```kotlin
/**
 * Content-URI trigger worker — implements the INV-4 two-stage debounce.
 *
 * This worker is enqueued by the CONTENT_TRIGGER [androidx.work.PeriodicWorkRequest]
 * (set up by [com.zegoggles.smssync.scheduler.WorkManagerScheduler.scheduleContentTrigger]).
 * It does NOT perform a backup directly; instead it calls [BackupScheduler.scheduleIncoming],
 * the one builder of the delayed incoming backup (unique work INCOMING, REPLACE). This path
 * therefore gets every rule of that call: the encryption-degraded gate (U-054), the
 * auto-backup and delay rule, the network constraint per Wi-Fi only (INV-2), the exponential
 * 30 s backoff (INV-3) and the delay of getIncomingTimeoutSecs() (INV-4).
 *
 * This preserves the two-stage debounce (INV-4):
 * - Stage 1: content-URI change fires this trigger worker
 * - Stage 2: this worker enqueues the delayed incoming backup via scheduleIncoming()
 *
 * The worker does NOT back up on the raw content change (INV-4, DES-MODERNIZATION-005
 * §Incoming-SMS trigger). It returns success whether or not the scheduler enqueued work:
 * a declined enqueue is a rule working, and the scheduler logs the reason.
 *
 * U-069 (BUG-023): created by HiltWorkerFactory through the assisted factory generated for
 * [HiltWorker], like the backup and restore workers. The class name and package must not
 * change: CONTENT_TRIGGER work persisted on a device stores this class name.
 */
@HiltWorker
class BackupTriggerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scheduler: BackupScheduler,
    private val preferences: Preferences
) : Worker(context, params) {
```
The Stage 2 sentence (old `:39`, new `:41`) is kept byte for byte and is now true (AC-12 (b)). The new comment contains
none of `OneTimeWorkRequest`, `enqueueUniqueWork`, `WorkManager.`, `TimeUnit`, `BackupType`, `BackupWorker`, so the
AC-10 greps are exact (I3 step 2).

**W3 (`doWork()`, `:53-74`).** `old_string`:
```kotlin
    override fun doWork(): Result {
        val prefs = Preferences(applicationContext)
        val incomingTimeoutSecs = prefs.incomingTimeoutSecs.toLong()

        Log.d(TAG, "BackupTriggerWorker.doWork: content-URI trigger fired; " +
                "enqueueing delayed incoming backup with initialDelay=${incomingTimeoutSecs}s")

        // INV-4: two-stage debounce — enqueue delayed incoming backup; do NOT back up directly.
        val incomingWork = OneTimeWorkRequest.Builder(BackupWorker::class.java)
            .setInitialDelay(incomingTimeoutSecs, TimeUnit.SECONDS)
            .addTag(BackupType.INCOMING.name)
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                BackupType.INCOMING.name,
                androidx.work.ExistingWorkPolicy.REPLACE,
                incomingWork
            )

        return Result.success()
    }
```
`new_string`:
```kotlin
    override fun doWork(): Result {
        // Read for the log line only; scheduleIncoming() reads the preferences itself and decides.
        val incomingTimeoutSecs = preferences.incomingTimeoutSecs

        Log.d(TAG, "BackupTriggerWorker.doWork: content-URI trigger fired; " +
                "enqueueing delayed incoming backup with initialDelay=${incomingTimeoutSecs}s")

        // INV-4 stage 2: the one builder of INCOMING work applies INV-1 to INV-3, the U-054 gate
        // and the auto-backup and delay rule. A null result (declined) is not a failure of this run.
        scheduler.scheduleIncoming()

        return Result.success()
    }
```
The companion object (`TAG`) and the closing brace stay. Result: 76 lines, blob `c47a0452d040f959df134fc8a71fda5be6b6180f`
(L19). New line numbers: `@HiltWorker` `:51`, header `:52`, the log line `:63-64`, the call `:68`, `return` `:70`.

### 3.2 New file `$TS/worker/BackupTriggerWorkerTest.kt` (Write tool, I1 step 1; one edit in I3 step 1)

Written in I1 in its **red form**: identical to the text below except the one factory line
`                BackupTriggerWorker(appContext, workerParameters, scheduler, preferences)` is written as
`                BackupTriggerWorker(appContext, workerParameters)` (16 leading spaces in both), so that it compiles
against the unchanged worker. Red-form blob `306dee99fc19dc1810d72abeed33a683cbb7edd1`.
**T-F** (I3 step 1, Edit): that one line → the four-argument line. Final blob `85401cb1f2a1133fa285aaf1bbefd41b0c1bbd93`
(456 lines; iteration 2 adds the four-line delay assertion at the end of `ac9_allowedState_…`).
The text contains the string `@Test` only on the 15 annotation lines.

```kotlin
package com.zegoggles.smssync.worker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.hilt.work.WorkerAssistedFactory
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.WorkManagerImpl
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.preferences.DataTypePreferences
import com.zegoggles.smssync.preferences.Preferences
import com.zegoggles.smssync.preferences.SecretStore
import com.zegoggles.smssync.scheduler.BackupScheduler
import com.zegoggles.smssync.scheduler.WorkManagerScheduler
import com.zegoggles.smssync.service.BackupType
import com.zegoggles.smssync.service.BackupWorker
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.util.UUID
import java.util.concurrent.Executors

/**
 * U-069 (BUG-023): the content-trigger worker starts the incoming backup through
 * [WorkManagerScheduler.scheduleIncoming], so the trigger path obeys the rules of every other
 * incoming backup (CNTR-MODERNIZATION-004 INV-1 to INV-4, the U-054 encryption-degraded gate, the
 * auto-backup and delay rule) and keeps the two-stage debounce (INV-4).
 *
 * Harness as in WorkManagerSchedulerTest: WorkManagerTestInitHelper with a single-thread executor,
 * mocked preferences and secret store, a real WorkManagerScheduler. The worker is built with
 * TestListenableWorkerBuilder and a factory that passes that scheduler and those preferences, as
 * HiltWorkerFactory passes the injected port in production; doWork() is called directly. Each test
 * registers the content trigger with the happy-path stubs first, then changes the stub it is about,
 * then runs the worker, so the rules are shown to hold at run time. The clock is not advanced and no
 * constraint is marked met, so INCOMING work stays ENQUEUED.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class BackupTriggerWorkerTest {

    private lateinit var context: Context
    private lateinit var scheduler: WorkManagerScheduler

    @Mock private lateinit var preferences: Preferences
    @Mock private lateinit var dataTypePreferences: DataTypePreferences
    @Mock private lateinit var secretStore: SecretStore

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = RuntimeEnvironment.getApplication()

        // The harness of WorkManagerSchedulerTest.setUp. BackupWorker would be created by its test
        // factory if WorkManager ran it; it does not run here.
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .setWorkerFactory(BackupWorker.TestableBackupWorkerFactory())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        // The allowed state; each test changes the stub it is about after the trigger is registered.
        `when`(preferences.dataTypePreferences).thenReturn(dataTypePreferences)
        `when`(preferences.isAutoBackupEnabled).thenReturn(true)
        `when`(preferences.incomingTimeoutSecs).thenReturn(60)
        `when`(preferences.regularTimeoutSecs).thenReturn(3600)
        `when`(preferences.isWifiOnly).thenReturn(false)
        `when`(preferences.isCallLogBackupAfterCallEnabled()).thenReturn(false)
        `when`(dataTypePreferences.isBackupEnabled(DataType.CALLLOG)).thenReturn(false)
        `when`(secretStore.isEncryptionDegraded()).thenReturn(false)

        scheduler = WorkManagerScheduler(context, preferences, secretStore)
    }

    // -----------------------------------------------------------------------
    // AC-1, AC-2: INV-2 on the trigger path
    // -----------------------------------------------------------------------

    @Test
    fun ac1_wifiOnlyOn_incomingWorkRequiresUnmeteredNetwork() {
        registerContentTrigger()
        `when`(preferences.isWifiOnly).thenReturn(true)

        val result = runTriggerWorker()

        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
        val work = incomingWork()
        assertWithMessage("INCOMING work entries").that(work).hasSize(1)
        assertWithMessage("required network type").that(work[0].constraints.requiredNetworkType)
            .isEqualTo(NetworkType.UNMETERED)
    }

    @Test
    fun ac2_wifiOnlyOff_incomingWorkRequiresConnectedNetwork() {
        registerContentTrigger()
        `when`(preferences.isWifiOnly).thenReturn(false)

        val result = runTriggerWorker()

        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
        val work = incomingWork()
        assertWithMessage("INCOMING work entries").that(work).hasSize(1)
        assertWithMessage("required network type").that(work[0].constraints.requiredNetworkType)
            .isEqualTo(NetworkType.CONNECTED)
    }

    // -----------------------------------------------------------------------
    // AC-3: the request the trigger path leaves equals the one scheduleIncoming() leaves
    // -----------------------------------------------------------------------

    @Test
    fun ac3_wifiOnlyOn_triggerRequestEqualsScheduleIncomingRequest() {
        assertTriggerRequestEqualsScheduleIncomingRequest(wifiOnly = true, expectedNetwork = NetworkType.UNMETERED)
    }

    @Test
    fun ac3_wifiOnlyOff_triggerRequestEqualsScheduleIncomingRequest() {
        assertTriggerRequestEqualsScheduleIncomingRequest(wifiOnly = false, expectedNetwork = NetworkType.CONNECTED)
    }

    // -----------------------------------------------------------------------
    // AC-4 to AC-6: the scheduler declines; doWork() still returns success (AC-9)
    // -----------------------------------------------------------------------

    @Test
    fun ac4_encryptionDegradedAtRunTime_noIncomingWork_success() {
        registerContentTrigger()
        `when`(secretStore.isEncryptionDegraded()).thenReturn(true)

        val result = runTriggerWorker()

        assertWithMessage("INCOMING work entries").that(incomingWork()).isEmpty()
        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun ac5_autoBackupOffAtRunTime_noIncomingWork_success() {
        registerContentTrigger()
        `when`(preferences.isAutoBackupEnabled).thenReturn(false)

        val result = runTriggerWorker()

        assertWithMessage("INCOMING work entries").that(incomingWork()).isEmpty()
        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun ac6_incomingDelayZero_noIncomingWork_success() {
        registerContentTrigger()
        `when`(preferences.incomingTimeoutSecs).thenReturn(0)

        val result = runTriggerWorker()

        assertWithMessage("INCOMING work entries").that(incomingWork()).isEmpty()
        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun ac6_incomingDelayNegative_noIncomingWork_success() {
        registerContentTrigger()
        `when`(preferences.incomingTimeoutSecs).thenReturn(-1)

        val result = runTriggerWorker()

        assertWithMessage("INCOMING work entries").that(incomingWork()).isEmpty()
        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
    }

    // -----------------------------------------------------------------------
    // AC-7: the debounce is kept (INV-4); the delay length is asserted
    // -----------------------------------------------------------------------

    @Test
    fun ac7_debounce_incomingWorkWaitsEnqueuedForTheIncomingDelay() {
        registerContentTrigger()
        `when`(preferences.incomingTimeoutSecs).thenReturn(60)

        val result = runTriggerWorker()

        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
        val work = incomingWork()
        assertWithMessage("INCOMING work entries").that(work).hasSize(1)
        assertWithMessage("state (BackupWorker has not run)").that(work[0].state).isEqualTo(WorkInfo.State.ENQUEUED)
        assertWithMessage("run attempts").that(work[0].runAttemptCount).isEqualTo(0)
        assertWithMessage("initial delay ms").that(work[0].initialDelayMillis).isEqualTo(60L * 1000L)
    }

    // -----------------------------------------------------------------------
    // AC-8: single flight (INV-1); the new id shows that a new request replaced the old one
    // -----------------------------------------------------------------------

    @Test
    fun ac8a_triggerTwice_oneActiveIncomingWork_secondReplacesFirst() {
        registerContentTrigger()
        runTriggerWorker()
        val first = singleActiveIncomingId()

        runTriggerWorker()

        assertReplaced(first)
    }

    @Test
    fun ac8b_triggerThenScheduleIncoming_oneActiveIncomingWork_secondReplacesFirst() {
        registerContentTrigger()
        runTriggerWorker()
        val first = singleActiveIncomingId()

        assertWithMessage("scheduleIncoming() job").that(scheduler.scheduleIncoming()).isNotNull()

        assertReplaced(first)
    }

    @Test
    fun ac8c_scheduleIncomingThenTrigger_oneActiveIncomingWork_secondReplacesFirst() {
        registerContentTrigger()
        assertWithMessage("scheduleIncoming() job").that(scheduler.scheduleIncoming()).isNotNull()
        val first = singleActiveIncomingId()

        runTriggerWorker()

        assertReplaced(first)
    }

    // -----------------------------------------------------------------------
    // AC-9: the worker's own contract (log line byte for byte, success in every case)
    // -----------------------------------------------------------------------

    @Test
    fun ac9_allowedState_logsTheTriggerLineAtDebugBeforeTheSchedulerEnqueues() {
        registerContentTrigger()
        `when`(preferences.incomingTimeoutSecs).thenReturn(75)

        val result = runTriggerWorker()

        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
        val logs = ShadowLog.getLogsForTag(TAG)
        val trigger = assertOneTriggerLine(logs, 75)
        val enqueued = logs.indexOfFirst {
            (it.msg ?: "").startsWith("WorkManagerScheduler.scheduleIncoming: enqueued INCOMING")
        }
        assertWithMessage("index of the scheduler's enqueued line, after the trigger line")
            .that(enqueued).isGreaterThan(trigger)
        val work = incomingWork()
        assertWithMessage("INCOMING work entries").that(work).hasSize(1)
        assertWithMessage("initial delay ms (the current setting, not the default)").that(work[0].initialDelayMillis)
            .isEqualTo(75L * 1000L)
    }

    @Test
    fun ac9_encryptionDegraded_logsTheTriggerLineThenTheSchedulersBlockedLine_success() {
        registerContentTrigger()
        `when`(secretStore.isEncryptionDegraded()).thenReturn(true)

        val result = runTriggerWorker()

        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
        val logs = ShadowLog.getLogsForTag(TAG)
        val trigger = assertOneTriggerLine(logs, 60)
        val blocked = logs.indexOfFirst {
            (it.msg ?: "").startsWith("WorkManagerScheduler.scheduleIncoming: blocked")
        }
        assertWithMessage("index of the scheduler's blocked line, after the trigger line")
            .that(blocked).isGreaterThan(trigger)
    }

    // -----------------------------------------------------------------------
    // IC-1, IC-2 (b): the production wiring, read from the classes generated for the worker
    // -----------------------------------------------------------------------

    /**
     * HiltWorkerFactory finds a worker's assisted factory in a map keyed by the class name that
     * WorkManager stores with the work. Hilt does not run under Robolectric here, so this reads the
     * classes the androidx.hilt compiler generates for the annotated worker: the assisted factory
     * exists, it is bound into that map under the persisted class name, and the worker has exactly one
     * assisted-inject constructor with the two assisted parameters first. Without the HiltWorker
     * annotation the generated classes are missing and this fails. That the app's component installs
     * the module and the factory builds the worker on a device is IC-3's.
     */
    @Test
    fun ic1_hiltWiring_assistedFactoryIsBoundUnderThePersistedClassName() {
        assertWithMessage("worker class name").that(BackupTriggerWorker::class.java.name)
            .isEqualTo(PERSISTED_CLASS_NAME)

        val assistedFactory = generatedClass("_AssistedFactory")
        val hiltModule = generatedClass("_HiltModule")
        assertWithMessage("generated ${PERSISTED_CLASS_NAME}_AssistedFactory").that(assistedFactory).isNotNull()
        assertWithMessage("generated ${PERSISTED_CLASS_NAME}_HiltModule").that(hiltModule).isNotNull()
        assertWithMessage("assisted factory type")
            .that(WorkerAssistedFactory::class.java.isAssignableFrom(assistedFactory!!)).isTrue()
        assertWithMessage("assisted factory annotation")
            .that(assistedFactory.isAnnotationPresent(AssistedFactory::class.java)).isTrue()

        val bind = hiltModule!!.getDeclaredMethod("bind", assistedFactory)
        assertWithMessage("map binding").that(bind.isAnnotationPresent(IntoMap::class.java)).isTrue()
        assertWithMessage("map key").that(bind.getAnnotation(StringKey::class.java)?.value)
            .isEqualTo(PERSISTED_CLASS_NAME)

        val assistedConstructors = BackupTriggerWorker::class.java.declaredConstructors
            .filter { it.isAnnotationPresent(AssistedInject::class.java) }
        assertWithMessage("assisted-inject constructors").that(assistedConstructors).hasSize(1)
        assertWithMessage("constructor parameters").that(assistedConstructors[0].parameterTypes.toList())
            .containsExactly(
                Context::class.java, WorkerParameters::class.java,
                BackupScheduler::class.java, Preferences::class.java
            ).inOrder()
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Builds the trigger worker with the test's port and preferences, as HiltWorkerFactory does with the injected ones. */
    private inner class TriggerWorkerFactory : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? =
            if (workerClassName == BackupTriggerWorker::class.java.name) {
                BackupTriggerWorker(appContext, workerParameters, scheduler, preferences)
            } else {
                null
            }
    }

    /** Fields of one INCOMING request: public WorkInfo fields, plus backoff and input data from the WorkSpec. */
    private data class IncomingRequest(
        val tags: Set<String>,
        val initialDelayMillis: Long,
        val constraints: Constraints,
        val backoffPolicy: BackoffPolicy,
        val backoffDelayDuration: Long,
        val input: Data
    )

    private fun registerContentTrigger() {
        assertWithMessage("content trigger registered with the allowed state")
            .that(scheduler.scheduleContentTrigger()).isNotNull()
    }

    private fun runTriggerWorker(): ListenableWorker.Result =
        TestListenableWorkerBuilder<BackupTriggerWorker>(context)
            .setWorkerFactory(TriggerWorkerFactory())
            .build()
            .doWork()

    private fun incomingWork(): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(INCOMING).get()

    private fun activeIncomingWork(): List<WorkInfo> = incomingWork().filter { !it.state.isFinished }

    private fun singleActiveIncomingId(): UUID {
        val active = activeIncomingWork()
        assertWithMessage("active INCOMING work entries").that(active).hasSize(1)
        return active[0].id
    }

    private fun assertReplaced(first: UUID) {
        val survivor = singleActiveIncomingId()
        assertWithMessage("id of the surviving INCOMING work (the second request)").that(survivor).isNotEqualTo(first)
        val old: WorkInfo? = WorkManager.getInstance(context).getWorkInfoById(first).get()
        if (old != null) {
            assertWithMessage("state of the first INCOMING work").that(old.state).isEqualTo(WorkInfo.State.CANCELLED)
        }
    }

    private fun assertTriggerRequestEqualsScheduleIncomingRequest(wifiOnly: Boolean, expectedNetwork: NetworkType) {
        registerContentTrigger()
        `when`(preferences.isWifiOnly).thenReturn(wifiOnly)

        assertWithMessage("doWork() result").that(runTriggerWorker()).isEqualTo(ListenableWorker.Result.success())
        val viaTrigger = captureActiveIncomingRequest()
        WorkManager.getInstance(context).cancelUniqueWork(INCOMING).result.get()
        assertWithMessage("scheduleIncoming() job").that(scheduler.scheduleIncoming()).isNotNull()
        val viaScheduler = captureActiveIncomingRequest()

        assertWithMessage("request via the trigger worker, against the request via scheduleIncoming()")
            .that(viaTrigger).isEqualTo(viaScheduler)
        // The equal pair is also tied to the contract's values, so "equal but both wrong" cannot pass.
        assertWithMessage("required network type (INV-2)").that(viaTrigger.constraints.requiredNetworkType)
            .isEqualTo(expectedNetwork)
        assertWithMessage("backoff policy (INV-3)").that(viaTrigger.backoffPolicy).isEqualTo(BackoffPolicy.EXPONENTIAL)
        assertWithMessage("backoff delay ms (INV-3)").that(viaTrigger.backoffDelayDuration)
            .isEqualTo(WorkManagerScheduler.BACKOFF_INITIAL_SECS * 1000L)
        assertWithMessage("initial delay ms (INV-4)").that(viaTrigger.initialDelayMillis).isEqualTo(60L * 1000L)
        assertWithMessage("tags").that(viaTrigger.tags).contains(INCOMING)
    }

    // WorkInfo exposes neither backoff nor input data (WorkManager 2.9.1 and 2.12.0); the WorkSpec does,
    // through the same accessors in both. It is library-internal (@RestrictTo), hence the suppression.
    @SuppressLint("RestrictedApi")
    private fun captureActiveIncomingRequest(): IncomingRequest {
        val active = activeIncomingWork()
        assertWithMessage("active INCOMING work entries").that(active).hasSize(1)
        val info = active[0]
        val spec = WorkManagerImpl.getInstance(context).workDatabase.workSpecDao().getWorkSpec(info.id.toString())
        assertWithMessage("WorkSpec of the active INCOMING work").that(spec).isNotNull()
        return IncomingRequest(
            tags = info.tags,
            initialDelayMillis = info.initialDelayMillis,
            constraints = info.constraints,
            backoffPolicy = spec!!.backoffPolicy,
            backoffDelayDuration = spec.backoffDelayDuration,
            input = spec.input
        )
    }

    /** Asserts the trigger line once, at debug, byte for byte as CR-015, BUG-021 AC-12 and DES-014 Run R read it. */
    private fun assertOneTriggerLine(logs: List<ShadowLog.LogItem>, incomingTimeoutSecs: Int): Int {
        val expected = TRIGGER_LINE_PREFIX + incomingTimeoutSecs + "s"
        val indices = logs.indices.filter { logs[it].msg == expected }
        assertWithMessage("count of the trigger line \"$expected\"").that(indices).hasSize(1)
        assertWithMessage("level of the trigger line").that(logs[indices[0]].type).isEqualTo(Log.DEBUG)
        return indices[0]
    }

    private fun generatedClass(suffix: String): Class<*>? =
        try {
            Class.forName(PERSISTED_CLASS_NAME + suffix)
        } catch (e: ClassNotFoundException) {
            null
        }

    companion object {
        private const val TAG = "SMSBackup+"
        private const val PERSISTED_CLASS_NAME = "com.zegoggles.smssync.worker.BackupTriggerWorker"
        private const val TRIGGER_LINE_PREFIX =
            "BackupTriggerWorker.doWork: content-URI trigger fired; enqueueing delayed incoming backup with initialDelay="
        private val INCOMING = BackupType.INCOMING.name
    }
}
```

Notes. `spec!!` and `val old: WorkInfo?` compile against 2.9.1 (platform types; at most a warning) and 2.12.0
(`WorkSpec?`, `WorkInfo?`), L11. No Kotlin warning is an error in this build (no `allWarningsAsErrors`; `-Werror` is
javac-only and not for test tasks, `app/build.gradle:190-205`). Lint does not check test sources here (no
`checkTestSources`), so `@SuppressLint` is documentation. The test's own classes are not measured by JaCoCo.

### 3.3 `app/build.gradle`, JaCoCo Rule 15 (two edits, I6 step 1; P-1, ruled)

Iteration 2 (plan-review B-1, Record 18 ruling 46). The texts no longer claim that the generated classes count.

**E-G1 (`:579`).** `old_string`: `        // BackupTriggerWorker has no unit tests; floor stays at 0%.` → `new_string` (main path):
`        // BackupTriggerWorker has unit tests since U-069 (BackupTriggerWorkerTest); its Hilt-generated classes report no lines to JaCoCo (@DaggerGenerated filter; the others are interfaces).`
Alternative `new_string` when row D19 (b) applies (a generated class reports lines):
`        // BackupTriggerWorker has unit tests since U-069 (BackupTriggerWorkerTest).`

**E-G2 (`:586`).** `old_string`: `                minimum = 0.00 // MEASURED 0% — BackupTriggerWorker untested; TODO raise after dedicated tests`
→ `new_string`, main path (measured ratio 1.00, I4 step 3):
`                minimum = 0.95 // MEASURED <P>% (<c>/<t> lines, <YYYY-MM-DD>, U-069); floor one 0.05 step below the measurement, so a JaCoCo line-counting change cannot fail it`
(expected `<P>` = `100.0`)
and, if the measured ratio is below 1.00 (row D19 (a)):
`                minimum = <F> // MEASURED <P>% (<c>/<t> lines, <YYYY-MM-DD>, U-069); floor is the measurement rounded down to 0.05`.
`<F>`, `<P>`, `<c>`, `<t>` are the `ruled_floor`, `percent`, `covered` and `total` values of the `PACKAGE` line of
`I4-worker-classes.txt` (or of `I6-worker-classes.txt` under row D25); `<YYYY-MM-DD>` is today's UTC date from
`date -u +%F`. The classes whose lines were measured (`CLASS` lines with `covered + missed > 0`; expected:
`BackupTriggerWorker` only) are named in the implementation log, not in the build file. No `includes`, `excludes`,
`limit`, `counter` or other `minimum` line is touched; no exclusion is added (AC-11). Both lines are inside Rule 15
(`:575-588`); U-068 does not move them (L20). The rule covers the package `com.zegoggles.smssync.worker*` only: after
this story it holds `BackupTriggerWorker` and its generated classes, and no later story in sprint 024 or U-071 to U-073
adds code there (L26).

### 3.4 Not changed, and the AC-12 dispositions

| Site | Disposition | Why |
|------|-------------|-----|
| CNTR-004 `:384-385` ("it calls scheduler.scheduleIncoming()") | True after the fix; not edited | Route A |
| CNTR-004 `:243` (INV-4 realization), `:245` (INV-4 note: "INV-1 collapses the two at enqueue"), `:274` (Rule 2), `:277` (Rule 5) | True for this path after the fix; not edited | Route A; AC-3 and AC-8 show it |
| CNTR-004 `:372-376` (the content-trigger example with a network constraint, backoff, `REPLACE`) | **Known divergence, kept**: the code registers with the content-URI trigger constraints only, no network constraint and no backoff, with `UPDATE`. `REPLACE` in the example is not itself a divergence (INV-1 allows REPLACE or UPDATE for periodic work), so only the network constraint and the backoff need correcting | Bug ruling 2, Record 15 row 15, SC-1 ruling: the later change record's (hand-over item 2) |
| DES-005 `:133-138` (`:135` "it calls `scheduleIncoming()`": the legacy job service), `:320`, `:434`, `:495` ("INV-4 test asserts the debounce delay") | True as written; not edited. `:495` becomes true with `ac7_debounce_…` | AC-12 (d) |
| Worker comment (old `:29-47`) | Rewritten by W2: "calls [BackupScheduler.scheduleIncoming]"; Stage 2 sentence kept; U-015 paragraph dropped | AC-12 (b) |
| `WorkManagerSchedulerTest.kt:548` (post-U-068) "would ultimately call scheduleIncoming()" | True; not edited (2.3) | AC-12 (c) |
| `WorkManagerScheduler.kt`, `BackupWorker.kt`, `BackupScheduler.java`, `SmsBroadcastReceiver.java`, `AndroidManifest.xml`, `app/proguard-rules.pro`, `gradle/verification-metadata.xml`, every other build file | Not edited | AC-13, IC-2 (c), (d), IC-4 |

### 3.5 Helper tools (Write tool, I0 step 1, into `$TL` = `sdlc/artifacts/build/sprints/sprint-024/U-069/evidence/tools/`)

`oldtext_check.py` (tested in this session on LF and CRLF scratch copies, L19):
```python
"""U-069: check that every text this story's edits and mutations replace occurs exactly once.
usage (from the repository root): python -I -X utf8 oldtext_check.py start|fixed
start: before any edit (the worker as at <start>, the scheduler as U-068 leaves it, app/build.gradle, the
       scheduler test). fixed: after W1-W3 and T-F (the worker's mutation texts, the test factory line).
Files are read with universal newlines, so CRLF and LF working trees give the same result. Exit 1 if any count
is not 1."""
import sys
WRK = 'app/src/main/java/com/zegoggles/smssync/worker/BackupTriggerWorker.kt'
SCH = 'app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt'
BLD = 'app/build.gradle'
SXT = 'app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt'
TST = 'app/src/test/java/com/zegoggles/smssync/worker/BackupTriggerWorkerTest.kt'
START = [
    ('W1', WRK, 'import androidx.work.OneTimeWorkRequest\nimport androidx.work.WorkManager\nimport androidx.work.WorkerParameters\n'),
    ('W2', WRK, 'class BackupTriggerWorker(\n    context: Context,\n    params: WorkerParameters\n) : Worker(context, params) {\n'),
    ('W2-doc', WRK, ' * U-015 dependency: once BackupWorker is a CoroutineWorker with full execution logic,\n'),
    ('W3', WRK, '        val prefs = Preferences(applicationContext)\n        val incomingTimeoutSecs = prefs.incomingTimeoutSecs.toLong()\n'),
    ('LOG', WRK, '        Log.d(TAG, "BackupTriggerWorker.doWork: content-URI trigger fired; " +\n'
                 '                "enqueueing delayed incoming backup with initialDelay=${incomingTimeoutSecs}s")\n'),
    ('M-T2', SCH, '        if (secretStore.isEncryptionDegraded()) {\n'
                  '            Log.w(TAG, "WorkManagerScheduler.scheduleIncoming: blocked — encryption degraded, credentials in plaintext")\n'),
    ('M-T3/M-T4', SCH, '        if (!preferences.isAutoBackupEnabled || timeoutSecs <= 0) {\n'),
    ('M-T5/M-T12', SCH, '            .setInitialDelay(timeoutSecs.toLong(), TimeUnit.SECONDS)\n'),
    ('M-T6', SCH, '            .enqueueUniqueWork(BackupType.INCOMING.name, ExistingWorkPolicy.REPLACE, request)\n'),
    ('AC13b-1', SCH, ' *   [BackupTriggerWorker] enqueues a delayed [BackupWorker] rather than backing up directly.\n'),
    ('AC13b-2', SCH, '     * INV-4: two-stage debounce — the triggered [BackupTriggerWorker] enqueues a delayed\n'
                     '     * incoming backup rather than backing up directly.\n'),
    ('NO-PRE24', SCH, 'override fun scheduleContentTrigger(): ScheduledJob? {\n'),
    ('G1', BLD, '        // BackupTriggerWorker has no unit tests; floor stays at 0%.\n'),
    ('G2', BLD, '                minimum = 0.00 // MEASURED 0% — BackupTriggerWorker untested; TODO raise after dedicated tests\n'),
    ('AC12c', SXT, '     * The content-URI trigger worker would ultimately call scheduleIncoming(), but the gate\n'),
    ('T-se002-incoming', SXT, '    fun se002_scheduleIncoming_encryptionDegraded_returnsNullNoWork() {\n'),
    ('T-autoBackupOff', SXT, '    fun ac5_scheduleIncoming_autoBackupDisabled_returnsNull() {\n'),
    ('T-zeroTimeout', SXT, '    fun ac5_scheduleIncoming_zeroTimeout_returnsNull() {\n'),
    ('T-atMinSdk24', SXT, '    fun inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger() {\n'),
]
ABSENT_AT_START = [
    ('pre24-branch', SCH, 'Build.VERSION.SDK_INT < Build.VERSION_CODES.N'),
    ('pre24-test', SXT, 'fun inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast'),
]
FIXED = [
    ('M-T1/M-T7', WRK, '        scheduler.scheduleIncoming()\n'),
    ('M-T8', WRK, '        scheduler.scheduleIncoming()\n\n        return Result.success()\n'),
    ('M-T9', WRK, '"enqueueing delayed incoming backup with initialDelay=${incomingTimeoutSecs}s")'),
    ('M-T10', WRK, '@HiltWorker\nclass BackupTriggerWorker @AssistedInject constructor(\n'),
    ('M-T11', WRK, 'class BackupTriggerWorker @AssistedInject constructor(\n'),
    ('T-F', TST, '                BackupTriggerWorker(appContext, workerParameters, scheduler, preferences)\n'),
]
mode = sys.argv[1]
checks = START if mode == 'start' else FIXED
bad = 0
for cid, path, text in checks:
    n = open(path, encoding='utf-8').read().count(text)
    bad += n != 1
    print(f'{cid} {path} occurrences={n}')
if mode == 'start':
    for cid, path, text in ABSENT_AT_START:
        n = open(path, encoding='utf-8').read().count(text)
        bad += n != 0
        print(f'{cid} {path} occurrences={n} (expected 0)')
print(f'RESULT mode={mode} mismatches={bad}')
sys.exit(1 if bad else 0)
```

`jacoco_classes.py`:
```python
"""U-069: per-class LINE counters of one JaCoCo package, the package total, and the floor rounded down to 0.05.
usage: python -I -X utf8 jacoco_classes.py <jacocoTestReport.xml> <package/path>"""
import sys
import xml.etree.ElementTree as ET
xml, pkg = sys.argv[1:3]
root = ET.parse(xml).getroot()
p = next((p for p in root.iter('package') if p.get('name') == pkg), None)
if p is None:
    raise SystemExit(f'not found: {pkg}')

def line(el):
    c = next((c for c in el.findall('counter') if c.get('type') == 'LINE'), None)
    return (int(c.get('covered')), int(c.get('missed'))) if c is not None else (0, 0)

for c in sorted(p.findall('class'), key=lambda c: c.get('name')):
    cov, mis = line(c)
    print(f"CLASS {c.get('name').replace('/', '.')} covered={cov} missed={mis}")
cov, mis = line(p)
tot = cov + mis
if tot == 0:
    raise SystemExit('no lines')
h = (cov * 20 // tot) * 5   # hundredths, rounded down to a multiple of 5
r = 95 if h == 100 else h   # Record 18 ruling 46: one step lower when the measurement is 1.00
print(f"PACKAGE {pkg.replace('/', '.')} covered={cov} total={tot} ratio={cov / tot:.4f} "
      f"percent={100.0 * cov / tot:.1f} floor={h // 100}.{h % 100:02d} ruled_floor={r // 100}.{r % 100:02d}")
```
The floor is integer arithmetic (`⌊20c/t⌋ / 20`), so it is never above the measured ratio, and JaCoCo's `COVEREDRATIO`
check (`ratio < minimum` fails) passes at any rounding of the ratio. `ruled_floor` is the value written into the build
file (0.95 under a measured 1.00).

## 4. Implementation Steps

### 4.0 Environment and conventions (every step)

```bash
# From the repository root. Never set MSYS_NO_PATHCONV for Gradle.
unset MSYS_NO_PATHCONV
W=sdlc/artifacts/build/sprints/sprint-024/U-069; E=$W/evidence; M=$E/mutations; TL=$E/tools
T=sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools      # read-only: sanitize.py, junit_totals.py, junit_cases.py, jacoco_pkgs.py, junit-xml.init.gradle
U68T=sdlc/artifacts/build/sprints/sprint-024/U-068/evidence/tools   # read-only: sdk_dir.py, jacoco_lines.py
INIT=$T/junit-xml.init.gradle
R=app/build/test-results/testDebugUnitTest
JX=app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
S=app/src/main/java/com/zegoggles/smssync; TS=app/src/test/java/com/zegoggles/smssync
WRK=$S/worker/BackupTriggerWorker.kt; SCH=$S/scheduler/WorkManagerScheduler.kt
TST=$TS/worker/BackupTriggerWorkerTest.kt; SXT=$TS/service/WorkManagerSchedulerTest.kt
BTW=com.zegoggles.smssync.worker.BackupTriggerWorkerTest; WMS=com.zegoggles.smssync.service.WorkManagerSchedulerTest
SBR=com.zegoggles.smssync.receiver.SmsBroadcastReceiverTest
PLAN_HEAD=4300ffa98370b77dcc4dd8d12142079aada8313a
SDK="$(python -I -X utf8 $U68T/sdk_dir.py)"; BT="$SDK/build-tools/36.0.0"
JDKBIN="$(ls -d "/c/Program Files/Java/"jdk-25* | tail -1)/bin"
WORK=<a u069-work folder inside the developer's own session scratch folder>   # raw logs, pristine copies, dex extracts
START=<the commit recorded in I0 step 2>
XI=<empty, or "-I $INIT", per row D6>
```
Shell state does not persist; each call sets what it needs. `WORK`, `SDK` and `JDKBIN` are never written into a
repository file (evidence names them "the work folder", "the SDK", "the JDK").

**Gradle calls.** Foreground, tool timeout 600000 ms on every call, `--console=plain`, output to the work folder, exit
appended:
- **G0** (I0): `rm -rf $R; ./gradlew $XI --console=plain clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification :app:assembleRelease > $WORK/logs/I0-gates.log 2>&1; echo "exit=$?" >> $WORK/logs/I0-gates.log`
- **G-M** (I4): the same without `clean`, log `I4-gates.log`. **G-F** (I6): the same with `clean`, log `I6-gates.log`.
- **TR(name, classes)**: `rm -rf $R; ./gradlew $XI --console=plain :app:testDebugUnitTest --tests "<fqcn>" [--tests "<fqcn>" …] > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> $WORK/logs/<name>.log`

`:app:jacocoTestReport` is added to the story's gate command only to produce the XML; it is finalized by the same
verification task. The story's four gate tasks are all in G0, G-M and G-F.

**Filters for `sanitize.py`** (`python -I -X utf8 $T/sanitize.py <in> <out> --grep '<regex>'`):
- gate filter **G**: `'^(BUILD |FAILURE|> Task :app:(assemble|test|jacoco|lintVital|ksp)|U063-DAEMON-JVM|exit=)|tests completed|FAILED$|^e: |error:'`
- build-error filter **BE**: `'^(BUILD |FAILURE|exit=)|HiltWorker|AssistedInject|Assisted|^e: |error:'`

**Evidence after a gate, procedure EV-T(k)** (k = 0, 4, 6):
```bash
python -I -X utf8 $T/sanitize.py $WORK/logs/I<k>-gates.log $E/I<k>-gates.txt --grep '<G>'
python -I -X utf8 $T/junit_totals.py $R > $E/I<k>-junit-totals.txt
python -I -X utf8 $T/junit_cases.py $R BackupTriggerWorkerTest WorkManagerSchedulerTest SmsBroadcastReceiverTest > $E/I<k>-cases.txt
python -I -X utf8 $T/jacoco_pkgs.py $JX > $E/I<k>-jacoco-packages.txt
python -I -X utf8 $TL/jacoco_classes.py $JX com/zegoggles/smssync/worker > $E/I<k>-worker-classes.txt
```
At I4 and I6 also:
`python -I -X utf8 $U68T/jacoco_lines.py $JX com/zegoggles/smssync/worker BackupTriggerWorker.kt $WRK > $E/I<k>-worker-lines.txt`.

**Generated-code record, procedure EV-G(k)** (k = 4, 6; after a gate that exited 0):
```bash
{ ls app/build/generated/ksp/debug/java/com/zegoggles/smssync/worker/
  ls app/build/generated/ksp/debug/java/hilt_aggregated_deps/ | grep -c '^_com_zegoggles_smssync_worker_BackupTriggerWorker_HiltModule\.java$'
  for v in debug release; do C=app/build/generated/hilt/component_sources/$v/com/zegoggles/smssync/DaggerApp_HiltComponents_SingletonC.java
    echo "$v put=$(grep -c 'mapBuilder.put("com.zegoggles.smssync.worker.BackupTriggerWorker"' $C) size3=$(grep -c 'newMapBuilder(3)' $C)"; done
} > $WORK/logs/I<k>-generated.log 2>&1
python -I -X utf8 $T/sanitize.py $WORK/logs/I<k>-generated.log $E/I<k>-generated.txt
```
Expected: the four files `BackupTriggerWorker_AssistedFactory.java`, `BackupTriggerWorker_AssistedFactory_Impl.java`,
`BackupTriggerWorker_Factory.java`, `BackupTriggerWorker_HiltModule.java`; `1`; `debug put=1 size3=1`;
`release put=1 size3=1` (N5). Rows D17, D18.

**Timestamps** come from `date -u +%Y-%m-%dT%H:%M:%SZ`, run at that moment.

### I0 — Start state and baseline (no tracked file changes)

1. Create `$WORK/logs`, `$WORK/pristine`, `$E`, `$M`, `$TL`; write `oldtext_check.py` and `jacoco_classes.py` (3.5)
   into `$TL` with the Write tool.
2. **Start state** (one call, `$WORK/logs/I0-start.log`, sanitised to `$E/I0-start.txt`):
   ```bash
   date -u +%Y-%m-%dT%H:%M:%SZ; git rev-parse HEAD; git rev-parse --abbrev-ref HEAD
   git merge-base --is-ancestor $PLAN_HEAD HEAD; echo "plan-head-ancestor-exit=$?"
   git log --oneline $PLAN_HEAD..HEAD
   git log --oneline --grep='^U-068 S1-I' $PLAN_HEAD..HEAD | wc -l
   grep -n '^status' sdlc/artifacts/build/sprints/sprint-024/U-068-minsdk-24-floor.md sdlc/artifacts/build/sprints/sprint-023/U-067-*.md
   git status --porcelain --untracked-files=all -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat; echo "porcelain-end"
   grep -n 'reports.junitXml.required\|reports.html.required' build.gradle
   grep -n 'minSdkVersion' app/build.gradle
   for p in $WRK $SCH app/build.gradle $SXT $S/service/BackupWorker.kt $S/scheduler/BackupScheduler.java $S/receiver/SmsBroadcastReceiver.java app/src/main/AndroidManifest.xml; do echo "$(git rev-parse HEAD:$p) $(git hash-object $p) $p"; done
   python -I -X utf8 $TL/oldtext_check.py start; echo "oldtext-exit=$?"
   ls $TS/worker 2>&1 | head -1
   ls $U68T
   git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"
   git grep -nE '^\s*@Ignore(\s|\(|$)' HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt'
   grep -v '^#' gradle/gradle-daemon-jvm.properties
   "$JDKBIN/jps.exe" -l | grep -iE 'gradle'; echo "jps-gradle-end"
   ```
   Expected: `plan-head-ancestor-exit=0`; the commits after `$PLAN_HEAD` are U-068's seven increment commits (`U-068 S1-I1`
   to `S1-I7`, possibly fix commits) and orchestrator document commits; both stories `status: done`; nothing before
   `porcelain-end`; `reports.junitXml.required = true` at `:30` and `reports.html.required = false` at `:29`;
   `minSdkVersion 24`; for each path the two hashes equal each other, the worker's is `c37bbbff…` (L1), the scheduler's is
   expected to be `0194308c…` (L20; rows D4, D5); `RESULT mode=start mismatches=0` and `oldtext-exit=0`; `ls` of the
   test folder reports "No such file or directory"; `$U68T` lists `sdk_dir.py` and `jacoco_lines.py`; the count `C0` as
   printed (no expected number); the two `@Ignore` lines; `toolchainVersion=25`; no Gradle JVM before `jps-gradle-end`.
   Record `START=$(git rev-parse HEAD)` and `C0` at the top of the log. **Rows:** D1 to D8.
3. `./gradlew --version > $WORK/logs/I0-gradle-version.log 2>&1` sanitised to `$E/I0-gradle-version.txt` (expected the
   Daemon JVM line citing `gradle/gradle-daemon-jvm.properties`, Java 25).
4. **Gate G0**, then EV-T(0). Expected: `exit=0`; `I0-junit-totals.txt` `failures=0 errors=0`, `tests=T0`, `skipped=S0`
   (recorded; `S0` is "the start" for AC-14); `WorkManagerSchedulerTest` 25 PASS, `SmsBroadcastReceiverTest` 4 PASS;
   `I0-worker-classes.txt`: classes `BackupTriggerWorker` and `BackupTriggerWorker$Companion`, `covered=0 total=16`
   (L17). **Rows:** D9 to D11.
5. **Greps at `<start>`** into `$WORK/logs/I0-greps.log`, sanitised to `$E/I0-greps.txt`, each command echoed before its
   output:
   - (a) `git grep -n -E "enqueueUniqueWork|OneTimeWorkRequest" -- app/src/main/java/com/zegoggles/smssync/worker` →
     4 lines, `BackupTriggerWorker.kt:20`, `:35`, `:61`, `:67`;
   - (b) `git grep -n "enqueueUniqueWork(BackupType.INCOMING" -- app/src/main` → 1 line,
     `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt:153:            .enqueueUniqueWork(BackupType.INCOMING.name, ExistingWorkPolicy.REPLACE, request)`
     (the worker's multi-line call does not match this pattern, which is why (a) and (c) are also run);
   - (c) `git grep -n -E "BackupType\.INCOMING" -- app/src/main` → 8 lines: `WorkManagerScheduler.kt:146`, `:149`, `:153`,
     `:156`, `:157`, `:421` and `BackupTriggerWorker.kt:63`, `:68`;
   - (d) `git grep -n "BackupTriggerWorker" -- app/src` → 7 lines: `WorkManagerScheduler.kt:29`, `:60`, `:62`, `:211`,
     `:233` and `BackupTriggerWorker.kt:48`, `:57`;
   - (e) ``git grep -n -E 'via scheduleIncoming\(\)|it calls (scheduler\.)?`?scheduleIncoming\(\)|would ultimately call scheduleIncoming\(\)' -- app/src/main app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt sdlc/artifacts/design``
     (restricted to the files AC-12 is about, iteration 3) → 4 lines: `BackupTriggerWorker.kt:39`, `WorkManagerSchedulerTest.kt:548`,
     `CNTR-MODERNIZATION-004-backup-scheduler.md:384`, `DES-MODERNIZATION-005-workmanager-scheduler-design.md:135`.
   Line numbers are post-U-068 (L20); a different number with the same text is recorded, not a stop; a different set of
   files or texts is row D12.
6. Create `$W/implementation-log.md`: header (story, plan path, `<start>`, `C0`, `T0`, `S0`, the `@Ignore` lines), SC-1 to
   SC-7 with the rulings, the I0 results by reference to the evidence files, and the note that U-067's and U-068's tests
   are inside `C0`. `git status --porcelain --untracked-files=all -- app` is still empty.

### I1 — The new test file in its red form (no production change)

1. Write `$TST` with the Write tool: the text of 3.2 with the factory line in its two-argument form.
2. Checks into `$WORK/logs/I1-checks.log` (kept for `I2-red-checks.txt`): `git hash-object $TST` →
   `306dee99fc19dc1810d72abeed33a683cbb7edd1`; `grep -c "@Test" $TST` → `15`;
   `git status --porcelain --untracked-files=all -- app` → exactly `?? app/src/test/java/com/zegoggles/smssync/worker/BackupTriggerWorkerTest.kt`;
   `git diff --quiet HEAD -- app/src/main; echo "main-clean-exit=$?"` → 0. **Row:** D12b.

### I2 — Red run (the defect is visible; probes N1 to N4)

1. TR(`I2-red`, `$BTW`). Expected `exit=1` (tests fail), the class compiled (N1; row D13).
2. `python -I -X utf8 $T/junit_cases.py $R BackupTriggerWorkerTest > $E/I2-red-cases.txt`;
   `grep -o 'message="[^"]*"' $R/TEST-$BTW.xml > $WORK/logs/I2-red-msg.log; python -I -X utf8 $T/sanitize.py $WORK/logs/I2-red-msg.log $E/I2-red-messages.txt`.
   Expected: the table of 5.2, `cases=15 pass=4 fail=11 error=0`. **Row:** D14.
3. `$E/I2-red-checks.txt` (sanitised from the I1 log plus): `git diff --quiet HEAD -- app/src/main` exit 0 (the worker
   was unchanged during the run); the I1 checks.
4. Log entry: the red results by name, with the failing assertion's message label for each, and the sentence "the test
   class was compiled against the unchanged worker by writing the factory line in its two-argument form; one Edit (T-F)
   switches it in I3" (Verification Step 2's compile-order note).

### I3 — The fix, and the green run

1. Edits W1, W2, W3 on `$WRK` (3.1); Edit T-F on `$TST` (3.2).
2. Checks into `$WORK/logs/I3-checks.log`, sanitised to `$E/I3-checks.txt`:
   - `git hash-object $WRK` → `c47a0452d040f959df134fc8a71fda5be6b6180f`; `git hash-object $TST` →
     `85401cb1f2a1133fa285aaf1bbefd41b0c1bbd93` (row D15);
   - `git diff --numstat -- $WRK` → `25	28	app/src/main/java/com/zegoggles/smssync/worker/BackupTriggerWorker.kt`;
     `git diff -- $WRK | grep -E '^@@'` → exactly one line, `@@ -17,58 +17,55 @@ package com.zegoggles.smssync.worker`
     (git's function-context suffix included; re-derived with `git diff --no-index` on the scratch copies); `git diff -- $WRK | grep -cE '^[-+](package |.*class BackupTriggerWorker)'`
     → `2` (only the old and new `class` header lines; the package line is untouched, IC-2 (b));
   - `python -I -X utf8 $TL/oldtext_check.py fixed` → `RESULT mode=fixed mismatches=0`;
   - AC-10 (a): `git grep -n -E "enqueueUniqueWork|OneTimeWorkRequest" -- app/src/main/java/com/zegoggles/smssync/worker; echo "exit=$?"` →
     no line, `exit=1`; AC-10 (b) as I0 5 (b), the same one line; AC-10 (c): 6 lines, all `WorkManagerScheduler.kt`
     (`:146`, `:149`, `:153`, `:156`, `:157`, `:421`);
   - `grep -nE "OneTimeWorkRequest|\bWorkManager\b|TimeUnit|BackupType|BackupWorker\b|enqueueUniqueWork" $WRK; echo "exit=$?"`
     → no line, `exit=1` (AC-10's "no longer imports or uses"; `\bWorkManager\b` also matches the old import line and
     does not match `WorkManagerScheduler`; on the old worker the same command prints 13 lines);
   - `git status --porcelain --untracked-files=all -- app` → exactly ` M app/src/main/java/com/zegoggles/smssync/worker/BackupTriggerWorker.kt`
     and `?? app/src/test/java/com/zegoggles/smssync/worker/BackupTriggerWorkerTest.kt`.
3. TR(`I3-green`, `$BTW`, `$WMS`, `$SBR`) → `exit=0`;
   `python -I -X utf8 $T/junit_cases.py $R BackupTriggerWorkerTest WorkManagerSchedulerTest SmsBroadcastReceiverTest > $E/I3-green-cases.txt`.
   Expected: 15, 25 and 4 cases, all PASS. **Row:** D16.
4. `cp $WRK $WORK/pristine/BackupTriggerWorker.kt`; record in the log `HW=$(git hash-object $WRK)` (= `c47a0452…`) and
   `SW=$(sha256sum $WRK | cut -d' ' -f1)` (raw bytes, CRLF kept). These are the worker's restore references for I5.

### I4 — Measurement gate (floor unchanged)

1. **Gate G-M**, then EV-T(4) with `I4-worker-lines.txt`. Expected: `exit=0`; `tests=T0+15`, `failures=0`, `errors=0`,
   `skipped ≤ S0`; the three classes all PASS; every floor holds (`worker` still `0.00`). **Rows:** D17, D20.
2. EV-G(4). **Rows:** D17, D18.
3. Read `I4-worker-classes.txt` (iteration 2, plan-review B-1). Expected: six `CLASS` lines,
   `com.zegoggles.smssync.worker.BackupTriggerWorker covered=t missed=0` and `BackupTriggerWorker$Companion`,
   `BackupTriggerWorker_AssistedFactory`, `BackupTriggerWorker_AssistedFactory_Impl`, `BackupTriggerWorker_Factory`,
   `BackupTriggerWorker_HiltModule` each `covered=0 missed=0`; the `PACKAGE` line `covered=t total=t ratio=1.0000
   percent=100.0 floor=1.00 ruled_floor=0.95` (L17, L24, N6). Every line of `BackupTriggerWorker.kt` in
   `I4-worker-lines.txt` has `ci>0`. Record `c`, `t`, `P`, `F` (= `ruled_floor`) and the classes with lines in the log.
   **Row:** D19.

### I5 — Failure injections M-T1 to M-T12 (protocol 4.8)

In the order M-T1, M-T7, M-T8, M-T9, M-T10, M-T11 (worker), then M-T2, M-T3, M-T4, M-T5, M-T6, M-T12 (scheduler).
Before the first scheduler entry: `cp $SCH $WORK/pristine/WorkManagerScheduler.kt` and record
`HS=$(git rev-parse HEAD:$SCH)` and `SS=$(sha256sum $SCH | cut -d' ' -f1)`. Twelve runs. After the last entry: the 4.8
(1) check once more and `git diff --quiet HEAD -- $SCH` exit 0.

### I6 — The floor, the final gate, IC-4 and the final checks

1. Edits E-G1 and E-G2 (3.3) with the main-path texts (or the D19 alternatives) and the values from I4 step 3.
2. Checks: `git diff --numstat -- app/build.gradle` → `2	2	app/build.gradle`; `git diff -U0 -- app/build.gradle | grep -E '^[-+][^-+]'`
   → exactly the old and new `:579` and `:586` lines (4 lines); `git diff -U0 -- app/build.gradle | grep -cE '^[-+] *minimum ='`
   → `2`; `grep -n "BackupTriggerWorker untested\|TODO raise after dedicated tests\|has no unit tests" app/build.gradle; echo "exit=$?"`
   → no line, `exit=1`. **Row:** D24.
3. **Gate G-F** (with `clean`), then EV-T(6) with `I6-worker-lines.txt`, and EV-G(6). Expected: `exit=0`; `tests=T0+15`,
   `failures=0`, `errors=0`, `skipped ≤ S0`; the `worker` `PACKAGE` line equal to I4's (same `c`, `t`, `F`); every floor
   holds. **Rows:** D18, D25.
4. **Final checks** into `$WORK/logs/I6-final.log`, sanitised to `$E/I6-final-checks.txt`, each command echoed:
   - AC-10 (a), (b), (c) as I3 step 2;
   - IC-2 (a): `git grep --untracked -n "BackupTriggerWorker" -- app/src` → 15 lines: `WorkManagerScheduler.kt:29`,
     `:60`, `:62`, `:211`, `:233`; `BackupTriggerWorker.kt:52`, `:63`; `BackupTriggerWorkerTest.kt:63`, `:306`, `:323`,
     `:344`, `:345`, `:367`, `:451`, `:453` (iteration 2 line numbers). Every hit is accounted for in the log: the
     scheduler's import, two comments, the KDoc and the class literal; the declaration and the log text; the test's class,
     references and constants;
   - AC-12 (e) grep exactly as I0 5 (e), restricted to `app/src/main`, `$SXT` and `sdlc/artifacts/design` (all tracked;
     iteration 3, B-4) → 4 lines: `BackupTriggerWorker.kt:41`, `WorkManagerSchedulerTest.kt:548`,
     `CNTR-MODERNIZATION-004-backup-scheduler.md:384`, `DES-MODERNIZATION-005-workmanager-scheduler-design.md:135`. Then
     the new test file separately, with plain `grep` because it is untracked:
     ``grep -nE 'via scheduleIncoming\(\)|it calls (scheduler\.)?`?scheduleIncoming\(\)|would ultimately call scheduleIncoming\(\)' $TST``
     → exactly 1 line, `402:        assertWithMessage("request via the trigger worker, against the request via scheduleIncoming()")`,
     the AC-3 assertion label; the log records it as a label, not an AC-12 statement (both outputs run, L27);
   - AC-13 (a): `git diff --name-only $START -- app/src/main` → only `$WRK`; (b), (c):
     `git diff --quiet $START -- $SCH $S/service/BackupWorker.kt $S/scheduler/BackupScheduler.java $S/receiver/SmsBroadcastReceiver.java app/src/main/AndroidManifest.xml; echo "exit=$?"` → `exit=0`;
     (d): `git diff --stat $START -- build.gradle app/build.gradle k9mail-vendored/build.gradle settings.gradle gradle` →
     ` app/build.gradle | 4 ++--` and ` 1 file changed, 2 insertions(+), 2 deletions(-)`; (e), (f):
     `git diff --quiet $START -- $SXT $TS/receiver/SmsBroadcastReceiverTest.java; echo "exit=$?"` → `exit=0`, and the
     I6 cases file shows 25 and 4 PASS; `grep -nE '@Ignore|Assume' $TST; echo "exit=$?"` → no line, `exit=1` (plain
     `grep`, because `$TST` is untracked until the orchestrator's commit and `git grep` without `--untracked` would search
     nothing; plan-review B-3);
   - AC-12: `git diff --name-only $START HEAD -- sdlc/artifacts/design sdlc/artifacts/build/change-records` and
     `git status --porcelain -- sdlc/artifacts/design sdlc/artifacts/build/change-records`; expected nothing; any line
     is attributed (row D29). The developer edits no file there;
   - scope of commits: `git log --oneline $START..HEAD -- app k9mail-vendored build.gradle settings.gradle gradle; echo "code-commits-end"`
     → nothing before the marker (row D30); `git log --oneline $START..HEAD` recorded (document commits by others);
   - AC-14 counts: `git grep --untracked -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`
     → `C0 + 15` (the working-tree form, said so); `git status --porcelain --untracked-files=all -- app/src/test` →
     only the new test file (row D28);
   - porcelain of the story's paths: `git status --porcelain --untracked-files=all -- app` → the two lines of I3 step 2
     plus ` M app/build.gradle`.
5. **IC-4** into `$WORK/logs/I6-ic4.log`, sanitised to `$E/I6-ic4-release.txt`:
   ```bash
   ls app/build/outputs/apk/release/
   APK=app/build/outputs/apk/release/app-release-unsigned.apk; MAP=app/build/outputs/mapping/release/mapping.txt
   ls -l $APK $MAP | awk '{print $5, $NF}'
   grep -nE '^com\.zegoggles\.smssync\.worker\.' $MAP
   grep -c '^com\.zegoggles\.smssync\.worker\.BackupTriggerWorker -> com\.zegoggles\.smssync\.worker\.BackupTriggerWorker:$' $MAP
   rm -rf "$WORK/dex"; mkdir -p "$WORK/dex"; unzip -q -o $APK 'classes*.dex' -d "$WORK/dex"
   for d in "$WORK"/dex/classes*.dex; do "$BT/dexdump" "$d" 2>/dev/null | grep "Class descriptor"; done | sed -E "s/.*'L(.*);'.*/\1/" | LC_ALL=C sort -u > "$WORK/release-dex-classes.txt"
   wc -l < "$WORK/release-dex-classes.txt"
   grep -E '^com/zegoggles/smssync/worker/' "$WORK/release-dex-classes.txt"
   grep -c '^com/zegoggles/smssync/worker/BackupTriggerWorker$' "$WORK/release-dex-classes.txt"
   for d in "$WORK"/dex/classes*.dex; do echo "$(basename $d) key-string=$(grep -a -c -F 'com.zegoggles.smssync.worker.BackupTriggerWorker' "$d")"; done
   ```
   Expected: the APK name as listed (row D26 if it differs); the self-mapping count `1`; the dex listing count `1`, and
   the `worker` lines as found (the generated classes' release names are recorded as found: they may be renamed);
   `key-string` at least `1` in total across the dex files (the map key, a string literal kept under `-dontoptimize`;
   row D27). **Rows:** D26, D27.
6. Log entry: the floor, the measured figures, IC-4's results, the generated names, and whether the class is
   self-mapped.

### I7 — Implementation log, hygiene scan, hand-off

1. Complete `$W/implementation-log.md`: each increment's result by reference to its evidence; the 15 added tests by
   name (5.1); `C0`, `C0 + 15`, `T0`, `T0 + 15`, `S0` and the end skipped count; the mutation summary (11 detections,
   M-T11 invalid by design, with its build message); the classes whose lines the `worker` floor measures (I4 step 3); AC-12's statement "the change record for the CNTR-004 `:372-376`
   example: not yet raised" (or its number if the orchestrator has raised it by then, SC-1); the IC-4 result; the
   hand-over items of section 7 that concern the developer's findings.
2. **Hygiene scan** into `$E/I7-scan.txt`:
   `grep -rnIE 'Users[/\\]|App''Data|[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+\.[A-Za-z]{2,}' $W/implementation-log.md $E | grep -v '^Binary' > $WORK/logs/I7-scan.log; echo "scan-lines=$(wc -l < $WORK/logs/I7-scan.log)" > $E/I7-scan.txt`
   → `scan-lines=0` (the pattern is split as `App''Data` so this line does not match itself in the log). **Row:** D31.
3. `ls -- <every path of the 4.9 list except IC3-emulator.txt> > /dev/null; echo "files-exist-exit=$?"` → 0;
   `git status --porcelain --untracked-files=all -- app $W` → the three `app` lines of I6 step 4 and `?? ` lines for
   exactly the `$W` files of 4.9, plus, if they show, `plan.md`, the plan-gate files and `$W/compiled-pipeline.json`,
   which are the orchestrator's: recorded, not a stop (plan-review O-3).
4. Hand-off message: the 4.9 file list; the debug APK path `app/build/outputs/apk/debug/app-debug.apk` with its size and
   SHA-256 (`stat -c %s`, `sha256sum`) for IC-3; the counts; the floor; anything recorded under a decision row.

### 4.8 Mutation protocol (write-ahead; restore proven by hash and raw bytes)

`$E/mutation-log.md` header: "Protocol: U-069 plan 4.8 (write-ahead entry, one Edit, one targeted run, restore by
copying the pristine file, blob, raw-byte and porcelain checks, `restored` line). The production change is uncommitted
while mutations run. Repository-relative paths."; `<start>`; `HW`, `SW` (worker, fixed text) and `HS`, `SS` (scheduler,
= HEAD). **Recovery note** (in the header): "If a session is lost while an entry says `APPLYING` without a `restored`
line, nothing else runs until: the worker is restored **only** by `cp` from its pristine copy (never `git restore`, which
would remove the fix) or, if the pristine copy is lost, by `git restore --source=HEAD --worktree -- <worker>` followed by
W1 to W3 and the blob check `c47a0452…`; the scheduler is restored by `cp` from its pristine copy or by
`git restore --source=HEAD --worktree -- <scheduler>` (it equals HEAD)."

For each entry, one at a time:
1. **Preconditions:** `git status --porcelain -- app/src/main` is exactly ` M $WRK`; `git hash-object $WRK` = `HW`;
   `git hash-object $SCH` = `HS`; for a scheduler entry its pristine copy exists. Otherwise stop (row D21).
2. **Write-ahead** line `### <id> APPLYING <date -u>` with the file, the exact edit, the class(es) run and the expected
   failing and passing cases.
3. **One Edit** as the table says.
4. **TR(`<id>`, classes).** Then `python -I -X utf8 $T/junit_cases.py $R BackupTriggerWorkerTest WorkManagerSchedulerTest > $M/<id>-cases.txt`
   and `for c in $BTW $WMS; do [ -f $R/TEST-$c.xml ] && grep -o 'message="[^"]*"' $R/TEST-$c.xml; done > $WORK/logs/<id>-msg.log; python -I -X utf8 $T/sanitize.py $WORK/logs/<id>-msg.log $M/<id>-message.txt`.
   For M-T11 instead: `python -I -X utf8 $T/sanitize.py $WORK/logs/M-T11.log $M/M-T11-build.txt --grep '<BE>'`.
5. **Restore** by `cp $WORK/pristine/<file> <file>`, whatever the result.
6. **Re-check** (1), plus `sha256sum <file>` = `SW` or `SS`; append `run_exit`, the failing and passing cases,
   `expected: matches | does not match`, `verdict: detected | undetected | invalid (does not compile)`, and
   `restored: <date -u> blob-ok raw-ok main-clean`.

No gate, review or commit runs while an entry lacks `restored`. Failure types: **A** = an `AssertionError` (Truth's);
**X** = an exception thrown by the code under test (counts as a failing test; type recorded).

| Id | File | Exact edit (`old_string` → `new_string`) | Classes run | Expected failing (every other case of the classes run passes) |
|----|------|-------------------------------------------|-------------|------------------------------------------------------------------|
| M-T1 | worker | `        scheduler.scheduleIncoming()` → `        androidx.work.WorkManager.getInstance(applicationContext).enqueueUniqueWork("INCOMING", androidx.work.ExistingWorkPolicy.REPLACE, androidx.work.OneTimeWorkRequest.Builder(com.zegoggles.smssync.service.BackupWorker::class.java).setInitialDelay(incomingTimeoutSecs.toLong(), java.util.concurrent.TimeUnit.SECONDS).addTag("INCOMING").build())` (a private builder with delay and tag only, as at the old `:61-71`) | BTW | `ac1`, `ac2` ("required network type", NOT_REQUIRED), both `ac3` (the equality), `ac4`, `ac5`, both `ac6` ("INCOMING work entries"), both `ac9` (no scheduler line after the trigger line): 10 A. Passing: `ac7`, `ac8a`, `ac8b`, `ac8c`, `ic1` |
| M-T7 | worker | `        scheduler.scheduleIncoming()` → `        // M-T7 removed: scheduler.scheduleIncoming()` | BTW | `ac1`, `ac2`, both `ac3`, `ac7` ("INCOMING work entries" or "active INCOMING work entries"), `ac8a`, `ac8b` (no first work), `ac8c` (same id survives), both `ac9`: 10 A. Passing: `ac4`, `ac5`, both `ac6`, `ic1` |
| M-T8 | worker | the three lines `        scheduler.scheduleIncoming()`, blank, `        return Result.success()` → `        val job = scheduler.scheduleIncoming()`, blank, `        return if (job == null) Result.failure() else Result.success()` | BTW | `ac4`, `ac5`, both `ac6`, `ac9_encryptionDegraded` ("doWork() result"): 5 A (AC-9's declined cases). Passing: the other 10 |
| M-T9 | worker | `"enqueueing delayed incoming backup with initialDelay=${incomingTimeoutSecs}s")` → `"scheduling delayed incoming backup with initialDelay=${incomingTimeoutSecs}s")` | BTW | both `ac9` ("count of the trigger line"): 2 A. Passing: the other 13 |
| M-T10 | worker | `@HiltWorker` + `class BackupTriggerWorker @AssistedInject constructor(` (two lines) → `class BackupTriggerWorker @AssistedInject constructor(` | BTW | `ic1` ("generated …_AssistedFactory"): 1 A. Passing: the other 14. If it does not compile (N7), row D22 (c) |
| M-T11 | worker | `class BackupTriggerWorker @AssistedInject constructor(` → `class BackupTriggerWorker constructor(` (`@HiltWorker` kept) | BTW | Build fails at the KSP step with hilt-compiler's "@HiltWorker annotated class should contain exactly one @AssistedInject annotated constructor." (or Dagger's message on the `@Assisted` parameters): logged **invalid (does not compile)**, not counted; it records that the build is the detector. If it compiles, row D23 |
| M-T2 | scheduler | the two lines `        if (secretStore.isEncryptionDegraded()) {` and `            Log.w(TAG, "WorkManagerScheduler.scheduleIncoming: blocked — encryption degraded, credentials in plaintext")` → the same with the first line `        if (false && secretStore.isEncryptionDegraded()) {` | BTW, WMS | BTW `ac4`, `ac9_encryptionDegraded`; WMS `se002_scheduleIncoming_encryptionDegraded_returnsNullNoWork`: 3 A. Passing: 13 + 24 |
| M-T3 | scheduler | `        if (!preferences.isAutoBackupEnabled \|\| timeoutSecs <= 0) {` → `        if (timeoutSecs <= 0) {` | BTW, WMS | BTW `ac5`; WMS `ac5_scheduleIncoming_autoBackupDisabled_returnsNull`: 2 A. Passing: 14 + 24 |
| M-T4 | scheduler | the same line → `        if (!preferences.isAutoBackupEnabled) {` | BTW, WMS | BTW both `ac6` (A; the negative case may be X, N9); WMS `ac5_scheduleIncoming_zeroTimeout_returnsNull` (A): 3. Passing: 13 + 24 |
| M-T5 | scheduler | `            .setInitialDelay(timeoutSecs.toLong(), TimeUnit.SECONDS)` → `            .setInitialDelay(0L, TimeUnit.SECONDS)` | BTW, WMS | BTW `ac7` ("initial delay ms"), both `ac3` ("initial delay ms (INV-4)"), `ac9_allowedState` ("initial delay ms (the current setting, not the default)", iteration 2): 4 A. Passing: 11 + 25 |
| M-T6 | scheduler | `            .enqueueUniqueWork(BackupType.INCOMING.name, ExistingWorkPolicy.REPLACE, request)` → the same with `ExistingWorkPolicy.KEEP` | BTW, WMS | BTW `ac8a`, `ac8b`, `ac8c` ("id of the surviving INCOMING work"): 3 A. Passing: 12 + 25 (both `ac3` pass: the cancelled entry does not block `KEEP`, L12; WMS `inv1_replace_incoming_…` passes, it counts entries only) |
| M-T12 | scheduler | `            .setInitialDelay(timeoutSecs.toLong(), TimeUnit.SECONDS)` → `            .setInitialDelay(60L, TimeUnit.SECONDS)` (the delay hard-coded to the default; iteration 2, contract-verification O-3) | BTW, WMS | BTW `ac9_allowedState` ("initial delay ms (the current setting, not the default)"): 1 A. Passing: 14 + 25 (`ac7` and both `ac3` use 60 and pass, which is why this mutant exists; no scheduler test reads the delay) |

`\|\|` is Kotlin's `||`, escaped for this table. Each counted mutant is one Edit; M-T8's and M-T10's `old_string` span
two or three lines and are still one Edit. Every expected-failing list is a superset of the story's table for that id
(the story's M-T1 lists AC-1 to AC-3; M-T7 AC-1, AC-2, AC-3, AC-7, AC-8; M-T8 the three declined cases, which are the
`ac4`, `ac5`, `ac6` result assertions and the degraded AC-9 test here). 11 counted detections (M-T1 to M-T10, M-T12).
Iteration 2 re-derived every row against the new `ac9_allowedState_…`: only M-T5 gains a failing case (the delay is 0);
under M-T1, M-T7 and M-T9 that test already fails earlier, and under M-T2, M-T3, M-T4, M-T6, M-T8 and M-T10 it passes
(the delay is 75 000).

### 4.9 Files for the orchestrator's commit (exact; the developer commits nothing)

`$W` = `sdlc/artifacts/build/sprints/sprint-024/U-069`.
- Code: `app/src/main/java/com/zegoggles/smssync/worker/BackupTriggerWorker.kt`; `app/src/test/java/com/zegoggles/smssync/worker/BackupTriggerWorkerTest.kt`; `app/build.gradle`.
- Log: `$W/implementation-log.md`.
- Tools: `$W/evidence/tools/{oldtext_check.py,jacoco_classes.py}`.
- I0: `$W/evidence/I0-{start.txt,gradle-version.txt,gates.txt,junit-totals.txt,cases.txt,jacoco-packages.txt,worker-classes.txt,greps.txt}`.
- I2, I3: `$W/evidence/I2-{red-cases.txt,red-messages.txt,red-checks.txt}`; `$W/evidence/I3-{checks.txt,green-cases.txt}`.
- I4: `$W/evidence/I4-{gates.txt,junit-totals.txt,cases.txt,jacoco-packages.txt,worker-classes.txt,worker-lines.txt,generated.txt}`.
- I5: `$W/evidence/mutation-log.md`; `$W/evidence/mutations/M-T{1,2,3,4,5,6,7,8,9,10,12}-{cases,message}.txt` (22 files); `$W/evidence/mutations/M-T11-build.txt`.
- I6: `$W/evidence/I6-{gates.txt,junit-totals.txt,cases.txt,jacoco-packages.txt,worker-classes.txt,worker-lines.txt,generated.txt,final-checks.txt,ic4-release.txt}`.
- I7: `$W/evidence/I7-scan.txt`.
- IC-3 (orchestrator, section 9): `$W/evidence/IC3-emulator.txt`.

That is 3 code files and 57 workspace files (1 log, 2 tools, and 8 + 3 + 2 + 7 + 24 + 9 + 1 = 54 evidence files, the
mutation log counted in the 24), plus the IC-3 record. Never staged by anyone for this story: the story file, the sprint plan, other
stories' workspaces, `compiled-pipeline.json`, `sdlc/.skill-context.json`, `sdlc/.skill-delegation-audit.log`.

## 5. Tests and Counts

### 5.1 The 15 added tests (class `com.zegoggles.smssync.worker.BackupTriggerWorkerTest`)

| # | Test | Criterion | Red on the unchanged worker | Mutants that kill it |
|---|------|-----------|-----------------------------|----------------------|
| 1 | `ac1_wifiOnlyOn_incomingWorkRequiresUnmeteredNetwork` | AC-1 | FAIL, "required network type" (NOT_REQUIRED) | M-T1, M-T7 |
| 2 | `ac2_wifiOnlyOff_incomingWorkRequiresConnectedNetwork` | AC-2 | FAIL, same | M-T1, M-T7 |
| 3 | `ac3_wifiOnlyOn_triggerRequestEqualsScheduleIncomingRequest` | AC-3 | FAIL, the equality (constraints) | M-T1, M-T5, M-T7 |
| 4 | `ac3_wifiOnlyOff_triggerRequestEqualsScheduleIncomingRequest` | AC-3 | FAIL, the equality | M-T1, M-T5, M-T7 |
| 5 | `ac4_encryptionDegradedAtRunTime_noIncomingWork_success` | AC-4, AC-9 | FAIL, "INCOMING work entries" | M-T1, M-T2, M-T8 |
| 6 | `ac5_autoBackupOffAtRunTime_noIncomingWork_success` | AC-5, AC-9 | FAIL, same | M-T1, M-T3, M-T8 |
| 7 | `ac6_incomingDelayZero_noIncomingWork_success` | AC-6, AC-9 | FAIL, same | M-T1, M-T4, M-T8 |
| 8 | `ac6_incomingDelayNegative_noIncomingWork_success` | AC-6, AC-9 | FAIL, same | M-T1, M-T4, M-T8 |
| 9 | `ac7_debounce_incomingWorkWaitsEnqueuedForTheIncomingDelay` | AC-7 | PASS | M-T5, M-T7 |
| 10 | `ac8a_triggerTwice_oneActiveIncomingWork_secondReplacesFirst` | AC-8 (a) | PASS | M-T6, M-T7 |
| 11 | `ac8b_triggerThenScheduleIncoming_oneActiveIncomingWork_secondReplacesFirst` | AC-8 (b) | PASS | M-T6, M-T7 |
| 12 | `ac8c_scheduleIncomingThenTrigger_oneActiveIncomingWork_secondReplacesFirst` | AC-8 (c) | PASS | M-T6, M-T7 |
| 13 | `ac9_allowedState_logsTheTriggerLineAtDebugBeforeTheSchedulerEnqueues` | AC-9 | FAIL, "count of the trigger line" (the old worker logs the real preference, 60, not 75) | M-T1, M-T5, M-T7, M-T9, M-T12 |
| 14 | `ac9_encryptionDegraded_logsTheTriggerLineThenTheSchedulersBlockedLine_success` | AC-9 | FAIL, "index of the scheduler's blocked line" (the trigger line is found: probe N2) | M-T1, M-T2, M-T7, M-T8, M-T9 |
| 15 | `ic1_hiltWiring_assistedFactoryIsBoundUnderThePersistedClassName` | IC-1, IC-2 (b) | FAIL, "generated …_AssistedFactory" | M-T10 |

### 5.2 Red-run expectation (I2)

`cases=15 pass=4 fail=11 error=0`: tests 9 to 12 pass; tests 1 to 8, 13, 14 and 15 fail at the assertion named in 5.1.
Each failure's message must carry that assertion's label; the label is how probes N2 (test 14) and N3 (tests 3, 4)
are read (row D14).

### 5.3 Counts (REQ-015 AC-13 as amended by CR-016; AC-14)

- Start: `C0` = the authoritative command at `<start>` (I0 step 2), with the commit id. It includes U-067's and U-068's
  tests, which land before `<start>`; they are not this story's.
- Added by this story: the 15 tests of 5.1, by name. Deleted: none. Changed: none (`WorkManagerSchedulerTest.kt` and
  `SmsBroadcastReceiverTest.java` show empty diffs).
- End: `C0 + 15`, by the working-tree form (`git grep --untracked`, I6 step 4), said so; after the orchestrator's commit,
  the same command with that commit in place of `HEAD` (hand-over item 1).
- Gradle: `tests = T0 + 15` in `I6-junit-totals.txt`; failures 0; errors 0; skipped not above `S0` (and not above 2,
  REQ-015 AC-13). Any difference is recorded and explained (row D28).

## 6. Decision Table

| Row | Observation | What the developer does |
|-----|-------------|-------------------------|
| D1 | HEAD is not a descendant of `$PLAN_HEAD` | Stop and report; nothing has been edited |
| D2 | U-068 or U-067 is not `done`, or U-068's increment commits are missing | Stop before any edit (start conditions, Record 15 row 15) |
| D3 | Porcelain shows a change under the checked paths at I0 | Stop; never stash, restore or commit someone else's change |
| D4 | `oldtext_check.py start` reports a count other than 1 (or a pre-24 text present); or the Edit tool reports an `old_string` not found or not unique | Stop and report "plan text no longer matches <file>" with the tool output and `git log --oneline $PLAN_HEAD..HEAD -- <file>`; nothing is edited by guesswork |
| D5 | The worker's blob is not `c37bbbff…`; or the scheduler's is not `0194308c…` | Worker: stop and report (no story before this one edits it). Scheduler: if `oldtext_check` passes, record the blob and continue (U-068's KDoc wording may differ by a character; this story edits nothing there); otherwise D4 |
| D6 | `build.gradle` shows `reports.junitXml.required = true` | `XI` is empty: totals come from the build's own JUnit XML. If it shows `false`: `XI="-I $INIT"` (the read-only init script) and the log says why; D2 has then very likely already applied |
| D7 | `minSdkVersion` is not 24 | Stop (U-068 has not landed) |
| D8 | `jps` lists a Gradle JVM | Stop and ask the orchestrator; another build may still run in this checkout |
| D9 | G0 fails, or failures or errors are above 0 | Stop and report (story: the gate is green at `<start>`) |
| D10 | `S0` above 2 | Record and report as pre-existing; AC-14 compares with `S0` |
| D11 | The I0 `worker` package is not 2 classes and 0/16 lines | Record and explain from `I0-worker-classes.txt`; not a stop (the baseline is measured) |
| D12 | An I0 grep's set of files or texts differs from I0 step 5 | Record and explain each difference; a hit that shows a second `INCOMING` builder or another reference to the worker: stop and report |
| D12b | The red-form blob is not `306dee99…`, or the `@Test` count is not 15 | Compare the file with 3.2 and correct it until the blob matches; never run with a variant |
| D13 | The test class does not compile (I2) | Stop and report the compiler message; the test text is the plan's and is not changed by the developer |
| D14 | The red run differs from 5.2 | (a) Test 14 fails at "count of the trigger line": `ShadowLog.getLogsForTag` does not capture. Fallback F-LOG: in both AC-9 tests replace `ShadowLog.getLogsForTag(TAG)` by `ShadowLog.getLogs().filter { it.tag == TAG }` (two Edits; the blobs of 3.2 then no longer apply and the new ones are recorded); run I2 again once. If it still fails there, stop and report. (b) Test 3 or 4 fails at "WorkSpec of the active INCOMING work": stop and report (no public API reads backoff or input). (c) Test 9 fails: stop and report (the harness runs work or the default delay differs; AC-7 would not be reliable). (d) Test 10, 11 or 12 fails: stop and report (REPLACE semantics differ from L12). (e) A test listed as FAIL passes: stop and report (it does not detect the defect). (f) An error (exception) instead of a failure: record its type; if it comes from the harness (for example WorkManager not initialised), stop and report |
| D15 | The fixed worker's blob is not `c47a0452…`, or the final test blob is not `85401cb1…` (unless F-LOG applied) | Re-read W1 to W3 and T-F against the file and correct until the blob matches |
| D16 | The green run has a failure | Stop and report; never change an assertion to fit |
| D17 | KSP or `assembleDebug` fails at I4 | Stop and report the message (the production factory would not resolve) |
| D18 | EV-G: the four generated files, the aggregated-deps file, or `put=1 size3=1` in either component, not as expected | Missing entry or file: stop and report (the worker would not be built by `HiltWorkerFactory`). Only `size3` differs (for example a different map size because the component counts differently): record the lines and explain; not a stop if `put=1` in both |
| D19 | The measured `worker` ratio (iteration 2, plan-review B-1; ruling 46) | Expected 1.00, `ruled_floor` 0.95: the main-path E-G1 and E-G2 texts. (a) Ratio below 1.00: record each `ci=0` line of `I4-worker-lines.txt` and continue with `<F>` = `ruled_floor` and E-G2's second form; below 0.05: stop and report (the worker is not covered). (b) A generated class with `covered + missed > 0`: record it and use E-G1's alternative text |
| D20 | A floor other than `worker` fails at I4 | Stop and report |
| D21 | A mutation precondition (4.8 (1)) fails | Stop; apply the recovery note; never run a mutation on a dirty tree |
| D22 | A mutation result | (a) An expected-failing case passes: undetected; record it, stop and report (AC-11 unmet). For M-T10 only, first list `app/build/generated/ksp/debug/java/com/zegoggles/smssync/worker/` and record whether `BackupTriggerWorker_AssistedFactory.java` is still there (a stale generated class, not a real escape; plan-review O-7), then stop and report either way. (b) Extra failing cases: record and explain each from the mutant; not a stop if explained. (c) A counted mutant (M-T1 to M-T10) does not compile: log it invalid with the compiler message; M-T10: record "detected by the build" (a valid detector, not counted as a unit detection); M-T1 to M-T9 and M-T12: stop and report (no replacement is improvised) |
| D23 | M-T11 compiles | Read its cases: `ic1` must fail at "assisted-inject constructors" (size 0); record it as a unit detection and say the build did not detect it |
| D24 | The `app/build.gradle` diff is not the four lines of I6 step 2 | `git restore --source=HEAD --worktree -- app/build.gradle` (it equals HEAD before E-G1 and E-G2) and redo them. (Iteration 1's branch (b), "one line", is deleted: ruling 45 accepted two lines) |
| D25 | G-F fails on the `worker` floor, or the I6 ratio differs from I4's | Record both; set `<F>` from the `ruled_floor` of `I6-worker-classes.txt`, redo E-G2's value and comment (and E-G1 by D19 (b) if it now applies), run G-F once more; log it. Any other failure: stop and report |
| D26 | The release APK is not `app-release-unsigned.apk`, or the self-mapping count or the dex class count is not 1 | Another APK name: use the one listed and record it. The class renamed or missing: stop and report (persisted work would fail to resolve on a release build) |
| D27 | `key-string` is 0 in every dex | Record "the map-key string was not found as a literal"; not a stop (I6's generated component and IC-3 cover the key); the hand-off says so |
| D28 | The end count is not `C0 + 15`, or the Gradle total is not `T0 + 15`, or skipped is above `S0` | Record and explain; a test added by someone else's change after `<start>`: list it with its commit id (or "uncommitted"), not as this story's |
| D29 | AC-12 check lists a file under `sdlc/artifacts/design` or `sdlc/artifacts/build/change-records` | A commit by the orchestrator (for example the follow-up change record): record its id and subject; not a stop. An uncommitted change there: record its path only; never touch it |
| D30 | A commit in `$START..HEAD` touches `app`, `k9mail-vendored` or a build file | Stop and report (code changed under the story) |
| D31 | The hygiene scan has a line | Fix the file through `sanitize.py`, re-scan; never hand off with a hit |
| D32 | "bash.exe … not found", or a process killed without a visible reason | Stop and report (endpoint security); do not work around it |
| D33 | The tool moves a Gradle run to the background | Wait for its completion notice; never poll; start nothing else meanwhile |

## 7. Items for the Orchestrator (hand-over)

1. **Commit after the reviews** (the standing rule; the developer commits nothing): the 4.9 list with explicit paths;
   suggested subject `U-069: content-trigger worker enqueues the incoming backup through scheduleIncoming() (BUG-023)`.
   Then run the authoritative count with that commit in place of `HEAD` (AC-14 end form). U-070's I0 checks that a
   commit since `c9770c30` touches `worker/BackupTriggerWorker.kt` (`u069-end`), so U-070 needs this commit.
2. **The change record for the contract example** (CNTR-004 `:372-376`; bug ruling 2, Record 15 row 15, SC-1): not
   raised by this story. CR-016 is the latest record in `sdlc/artifacts/build/change-records/` today, so the next free
   number is CR-017 if nothing else takes it first. The story closes with its number or "not yet raised". Wording for
   that record (contract-verification O-4): the registration has the content-URI trigger constraints and "no network
   constraint and no backoff"; the example's `REPLACE` is not a divergence under INV-1 (periodic work may use REPLACE or
   UPDATE), so the record needs to correct only the network constraint and the backoff.
3. **For U-073 (runtime libraries, increment W, WorkManager 2.12.0):** the new test is
   `app/src/test/java/com/zegoggles/smssync/worker/BackupTriggerWorkerTest.kt`; U-073's AC-13 diff is taken over the
   folder `app/src/test/java/com/zegoggles/smssync/worker/`. It is written to compile and pass unchanged on 2.12.0: every
   `WorkManager`, `WorkInfo`, `WorkManagerImpl`, `WorkDatabase`, `WorkSpecDao` and `WorkSpec` member it uses was read in
   the 2.12.0 sources (L11), and the nullable forms compile on both versions. Not verified: work-testing 2.12.0's
   `TestListenableWorkerBuilder` (not in the cache, P1). Recommended, as U-067 recommended for its own: re-run this
   story's mutation table (M-T1 to M-T10, M-T12) at W, because a later WorkManager could change `REPLACE`/`KEEP` handling or
   the default backoff that the tests rely on.
4. **For U-071 (toolchain: androidx.hilt 1.4.0, Hilt 2.60.1, Robolectric 4.18):** test `ic1_…` reads androidx.hilt's
   generated names `<Name>_AssistedFactory`, `<Name>_HiltModule` and the method `bind` (read in 1.2.0, L6); the AC-9
   tests use `ShadowLog.getLogsForTag` and `LogItem.msg`/`type` (read in 4.15.1, L14). If either changes, U-071's gate
   shows it in that test; the fix is one name. Coverage: JaCoCo filters the generated classes today (`@DaggerGenerated`,
   L24), so the `worker` floor measures `BackupTriggerWorker.kt` only; U-071 also raises JaCoCo, and the 0.95 floor under
   a measured 1.00 (ruling 46) is there so that a line-counting change does not fail the gate.
5. **For U-070 (BUG-021) and DES-014 D14 Run R:** after this story the trigger path also logs
   `WorkManagerScheduler.scheduleIncoming: enqueued INCOMING`, so that line no longer identifies the receiver; Run R and
   BUG-021's AC-12 should read the trigger line `BackupTriggerWorker.doWork: content-URI trigger fired; …`. With Wi-Fi
   only on, the incoming backup now waits for an unmetered network, which may change the timing those checks observe.
   **The `worker` floor** (iteration 2, plan-review B-1 and O-6, ruling 46): Rule 15 covers only
   `com.zegoggles.smssync.worker*`, which after this story holds `BackupTriggerWorker` and its generated classes; its
   floor becomes 0.95 under a measured 1.00. The reviewer's remark that it "binds U-070, which edits this worker" does not
   hold: U-070 edits `BackupWorker.kt` and `RestoreWorker.kt` in `service` (and files in `preferences` and `mail`) and
   lists `BackupTriggerWorker.kt` as not touched (L26). No later story in sprint 024 or U-071 to U-073 adds code to the
   `worker` package. Any future story that does must cover its own new lines to keep 0.95.
6. **The SMS-received receiver** is left as it is (Record 15 row 16; PO-3 open): `SmsBroadcastReceiver.java` and its
   manifest entry show empty diffs; `SmsBroadcastReceiverTest` passes unchanged.
7. **Design text for the orchestrator** (no edit here; contract-verification O-3): the two-worker statements are
   DES-MODERNIZATION-008 `:113` (the `@HiltWorker` table row) and `:204-206` ("`@HiltWorker` for the CoroutineWorkers"),
   and DES-MODERNIZATION-005 `:261-262` (component table) and `:357` ("`BackupWorker` and `RestoreWorker` are annotated
   `@HiltWorker`"). None is false (none claims "only"), but a later amendment should list all three workers and name the
   trigger worker's type: a plain `Worker`, not a `CoroutineWorker`. For DES-005 also: the periodic content trigger fires
   at most once per 15-minute period (2.4 item 13, L25), so the effective debounce after a burst can be up to the rest of
   the period plus `incomingTimeoutSecs`. `sprint-017/U-060/contract-verification.md:49`
   ("created by the default WorkerFactory fallback through reflection") is historical; a later R8 story's C-2 check should
   list `BackupTriggerWorker` among the `HiltWorkerFactory` keys.
8. **Corrections to the story's text** (for the close-out, not edits by the developer): AC-11's "Not detectable by any
   unit test" becomes "detected by `ic1_…` (`@HiltWorker`) and by the build (`@AssistedInject`)" (2.5); the story's
   "not verified" items 2 (harness and `ShadowLog`), 4 (2.9.1 default backoff: 30 000 ms, L10) and 5 (keep rule, L9)
   are closed by this plan or by I2. **AC-11's sentence "the measured ratio will be below 100%"** (story `:101`) is false:
   the generated classes report no lines (L24) and the ratio is expected to be 1.00; the orchestrator corrects it at
   close-out, with the floor rule of ruling 46 (plan-review O-6).
9. **Product-owner items** PO-1, PO-2, PO-3 stay open (bug ruling 3, 4; Record 15 row 16).
10. **IC-3** is the orchestrator's (section 9), after the developer's hand-off and before or after the reviews, never
    with a mutation present.
11. **`integration_contracts`** (contract-verification O-1): the story's field stays empty because the Artifact
    Librarian has no method to add a contract to a story after scaffolding. The contract-verification gate checked
    CNTR-MODERNIZATION-004 regardless (iteration 1, PASS), and the story cites it in `related_items` and its text.
12. **BUG-024:** a trigger during a running incoming backup stops that backup through `REPLACE`, and since U-067 the stop
    is real and reported as a failure (2.4 item 11); BUG-024's fix should treat this path as one of its cases.
13. **The periodic trigger's rate limit** (2.4 item 13; iteration 3, depth-check recommendation): later SMS within one
    fifteen-minute period wait until it ends (up to about sixteen minutes with the defaults) and are backed up together.
    This is a behaviour that existed before this story and is not changed by it. The orchestrator is measuring it on the
    emulator and raises it as its own bug; this story's criteria do not depend on it.

## 8. Contracts Verified, Scope Concerns, Risks, Out of Scope

### 8.1 Contracts (approved; read, not edited)

**CNTR-MODERNIZATION-004 v3** (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md`,
`status: approved`). The story crosses one boundary, the `BackupScheduler` port, as a consumer; it changes no port
signature, `BackupType` value, `SchedulerState` variant or invariant, so under the Breaking change policy (`:266-267`)
nothing is breaking and no amendment is needed. Point by point:
- Rule 1 (`:273`): no `androidx.work` type crosses the port; the worker depends on the port (IC-1).
- INV-1 (`:240`): `REPLACE` single flight on the trigger path; AC-8 (a) to (c), M-T6.
- INV-2 (`:241`): network per Wi-Fi only on the trigger path; AC-1, AC-2, AC-3, M-T1, M-T7.
- INV-3 (`:242`): exponential 30 s on the trigger path; AC-3 (value and equality). The 300 s cap stays in `BackupWorker`.
- INV-4 (`:243`, `:245`) and Rule 5 (`:277`): the debounce; AC-7, M-T5; the trigger worker enqueues the delayed
  incoming backup, it does not back up.
- Rule 2 (`:274`): INV-1 to INV-4 hold for this enqueue path, asserted by the DES-005 harness
  (`WorkManagerTestInitHelper`, `getWorkInfosForUniqueWork`, `WorkSpec.constraints`/`backoffPolicy`/`backoffDelayDuration`).
- U-054 (SE-002) degraded gate: now on the trigger path; AC-4, M-T2.
- The sentence `:384-385` becomes true. The example `:372-376` stays divergent by ruling (8.2 is not needed for it: SC-1).
No new component boundary; no missing or draft CNTR. **Contract gate: PASS.** The story's `integration_contracts` field
stays empty: the Artifact Librarian has no method to add a contract after scaffolding. The contract-verification gate
checked CNTR-MODERNIZATION-004 regardless (iteration 1, PASS; hand-over item 11), and the story cites it in
`related_items` and its text, so the gate's substance (an approved contract, no deviation) is met.

### 8.2 Scope Concerns of this plan

- **P-1, ruled (Record 18 ruling 45): Rule 15 changes on two lines, not one** (the floor line and the note at `:579`,
  which is false after this story). E-G1 always applies.
- **P-2, ruled (Record 18 ruling 45): test `ic1_…` is added**, the unit-level detector for a removed `@HiltWorker`; the
  story adds fifteen tests.
- **Floor, ruled (Record 18 ruling 46):** measured ratio rounded down to 0.05, and 0.95 when the measurement is 1.00.
- **P-3, P-4, P-5 (information): stricter than the story's wording.** AC-8 (b) and (c) also assert the new id (without
  it M-T6 is not detected by them); AC-3 also asserts the contract values; AC-9 has a second test (degraded) and uses
  75 s in the first, where it also asserts the 75 000 ms delay (M-T12). None changes a criterion's meaning.
- **P-6 (information): `WorkManagerSchedulerTest.kt` is not edited** (AC-12 (c) is true as written).

The story's own Scope Concerns are ruled (section 0) and applied.

### 8.3 Risks

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|------------|--------|-----------|
| R1 | The Hilt factory does not build the worker on a device, so every automatic incoming backup stops silently (the receiver is not delivered) | Low (L6, L7; same pattern as two workers) | High | `ic1_…`; EV-G (component map entry, debug and release); IC-3 on the emulator |
| R2 | R8 renames or drops the class in release | Very low (two keep rules, L9) | High | IC-4 (mapping and dex), row D26 |
| R3 | `ShadowLog` does not capture under this set-up | Low (L14) | Medium (AC-9 unprovable) | Red-run probe (test 14), fallback F-LOG, else stop (D14 (a)) |
| R4 | The internal `WorkSpec` accessor fails in the harness | Low (L10) | Medium (AC-3) | Red-run probe (tests 3, 4), stop (D14 (b)) |
| R5 | The harness runs `INCOMING` work, so AC-7's state check is not reliable | Low (L13) | Medium | Red-run probe (test 9), stop (D14 (c)) |
| R6 | A mutation left in the tree reaches a gate, review or commit | Low | High | 4.8: write-ahead, preconditions, `restored` line, blob and raw-byte checks; the developer commits nothing |
| R7 | The measured `worker` ratio differs from the expected 1.00 (a worker line not run, or a generator or JaCoCo change that makes a generated class report lines) | Low (L17, L24) | Low | The floor is measured, not assumed; no exclusion; 0.95 under 1.00 (ruling 46); D19 (a) and (b) |
| R8 | U-071's androidx.hilt 1.4.0 renames generated types, breaking `ic1_…` | Low | Low | One-name fix; hand-over item 4 |
| R9 | U-073's work-testing 2.12.0 changes the builder API | Low | Low | Hand-over item 3; the rest of the API is verified in 2.12.0 sources |
| R10 | The owner notices incoming backups waiting on mobile data (Wi-Fi only on) | Medium | Low (intended) | The story's purpose; `CHANGES` is not edited by this story; hand-over item 5 |
| R11 | Another process commits code during the story | Low | Medium | Scoped checks, D30 |
| R12 | Plan text drift because U-068's final text differs from its plan | Low | Medium | `oldtext_check.py start` before any edit, D4, D5 |
| R13 | IC-3 cannot be conclusive (auto backup off on the AVD, no trigger, password prompt) | Medium (SC-5) | Low | Section 9 outcomes ("not conclusive", "not run"); no setting changed |
| R14 | The double read of `incomingTimeoutSecs` prints a value the scheduler then does not use (a setting changed in between) | Very low | Very low | The scheduler's own line states the delay it used |

### 8.4 Out of scope (each ruled or recorded)

The registration's constraint and backoff (ruling 2); cancelling a pending `INCOMING` on auto-backup off (PO-1); login
and first-backup checks on the trigger path (PO-2); the SMS receiver (PO-3); any `BackupWorker`, port, scheduler,
manifest, ProGuard or dependency change; the contract and design texts; `CHANGES`, `README.md`.

## 9. IC-3 — Emulator Check (for the orchestrator; the developer does not run it)

Preconditions: the developer's hand-off names `app/build/outputs/apk/debug/app-debug.apk` with its size and SHA-256 (built
by G-F from the final tree), and no mutation entry lacks `restored`. Run in the foreground; raw output goes to the
orchestrator's work folder outside the repository. `ADB="$SDK/platform-tools/adb"`. `MSYS_NO_PATHCONV=1` is needed only
for an adb command with a device path; no command below has one; never set it for Gradle. Conditions (Record 14,
instructions 1 to 4): one test SMS through the emulator console is allowed and the evidence says so; the AVD
`Pixel_10_Pro` is never started or touched; nobody but the owner handles a password; no system or app setting is
changed; the app is never uninstalled; if the install would lose data, the check stops as "not run".

1. `"$ADB" devices -l` first; it addresses no device.
   - No device listed: ask the owner to start the `Pixel_9_Pro` emulator. Never start or touch `Pixel_10_Pro`.
   - More than one device: stop, ask the owner which serial is `Pixel_9_Pro`, send nothing to any device until answered.
   - Exactly one emulator: `"$ADB" -s <serial> emu avd name`; if its first line is not `Pixel_9_Pro`, stop and ask.
   - Exactly one physical device, or anything else: send nothing, stop and ask the owner.
   Every later command uses only that serial, with `-s`.
2. `"$ADB" -s <serial> shell getprop ro.build.version.sdk` → `36`. Then
   `"$ADB" -s <serial> shell dumpsys package com.zegoggles.smssync | grep -E "versionCode|lastUpdateTime"`.
3. `sha256sum app/build/outputs/apk/debug/app-debug.apk` equals the hand-off; then
   `"$ADB" -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk` → `Success`. On any failure (signing
   mismatch, `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, downgrade): stop, report "not run". Never uninstall, never `-d`, never
   clear data. Repeat the `dumpsys` line.
4. `DEVT=$("$ADB" -s <serial> shell "date '+%m-%d %H:%M:%S.000'" | tr -d '\r')` (device time; `T` stays the U-063 tools
   folder that step 10's `sanitize.py` needs, plan-review O-4); check that `DEVT` is not empty. Send one SMS:
   `"$ADB" -s <serial> emu sms send 5551234 "U-069 IC-3 test message"`; record that it was sent and when.
5. Wait for the trigger line (tool timeout 600000):
   `timeout 300 "$ADB" -s <serial> logcat -T "$DEVT" -m 1 -e 'BackupTriggerWorker\.doWork: content-URI trigger fired'; echo "exit=$?"`.
   `exit=124` means no trigger line within five minutes: write the log (step 8) and report **"not conclusive: the
   trigger did not fire"**.
6. Wait for the scheduler's line:
   `timeout 120 "$ADB" -s <serial> logcat -T "$DEVT" -m 1 -e 'WorkManagerScheduler\.scheduleIncoming: (enqueued INCOMING|blocked|skipped)'; echo "exit=$?"`.
7. Only if that line is `enqueued INCOMING with delay=<d>s, network=<n>`: wait for the run,
   `timeout $((d + 240)) "$ADB" -s <serial> logcat -T "$DEVT" -m 1 -e 'BackupWorker: (backup complete, backedUp=|unrecoverable error|nothing to backup)'; echo "exit=$?"`.
8. `"$ADB" -s <serial> logcat -d -v threadtime -T "$DEVT" > <work>/ic3-logcat.txt` and `"$ADB" -s <serial> shell pidof com.zegoggles.smssync`.
   Search the file for, in this order: the trigger line (tag `SMSBackup+`, debug); a `WorkManagerScheduler.scheduleIncoming:`
   line after it; lines naming `BackupTriggerWorker` with `Could not instantiate`, `Could not create Worker` or
   `NoSuchMethodException` (WorkManager's factory failure texts, L22); when enqueued: `BackupWorker: starting backup (`
   (read N) and `BackupWorker: backup complete, backedUp=`; `FATAL EXCEPTION` followed by `Process: com.zegoggles.smssync`;
   `BackupWorker: unrecoverable error` with the app's pid.
9. **Outcomes.**
   - **Pass:** the trigger line, then the scheduler line, no factory error; and, if the scheduler line is
     `enqueued INCOMING`, the start line with N ≥ 1 and the completion line, with zero `FATAL EXCEPTION` and zero
     `unrecoverable error` from the app's process.
   - **Pass, not conclusive for the run:** trigger line, scheduler line `skipped` or `blocked` (auto backup off, delay
     ≤ 0, or degraded on that AVD), no factory error. It proves the production factory built the worker and the rule
     fired; it did not exercise the backup run. This label applies to `skipped` and `blocked` only (story IC-3 Results).
   - **Not conclusive: enqueued, run not observed** (iteration 2, plan-review B-2): the scheduler line is
     `enqueued INCOMING` and, within the wait, either `BackupWorker: nothing to backup` appears or no `starting backup (`
     line appears (say which, with the `network=` value). This is not a pass: IC-3 stays open and the orchestrator
     reports it to the owner. No setting is changed to force a run.
   - **Not conclusive: enqueued, run not completed** (iteration 3, plan-review note 1): the scheduler line is
     `enqueued INCOMING`, a `BackupWorker: starting backup (` line appears, and no `backup complete, backedUp=` line
     appears within the wait (step 7 ends with `exit=124`), with no error line from the app's process. Not a pass; IC-3
     stays open and the orchestrator reports it with the lines seen.
   - **Not conclusive: the trigger did not fire** (step 5). **Not run:** install refused, a password prompt, a
     second-device ambiguity the owner has not resolved.
   - **Fail:** a factory error naming `BackupTriggerWorker`; or the trigger line without any scheduler line within the
     wait; or, after `enqueued INCOMING`, a `BackupWorker: unrecoverable error` line from the app's process (iteration 3,
     plan-review note 1); or an app crash (`FATAL EXCEPTION` with `Process: com.zegoggles.smssync`).
10. Record `sdlc/artifacts/build/sprints/sprint-024/U-069/evidence/IC3-emulator.txt` through `sanitize.py` (it refuses
    e-mail-like text): clock time (UTC), AVD name, SDK level, build type (debug), the APK SHA-256 against the hand-off,
    install result, versionCode and lastUpdateTime before and after, that one test SMS was sent and when, the matching
    lines (trigger, scheduler, run), the Wi-Fi-only and auto-backup state **as the log shows them** (`network=UNMETERED`
    means Wi-Fi only on, `CONNECTED` off; `skipped (autoBackup=false, …)` means auto backup off; read, not changed), the
    error counts, and the outcome. Never copy raw logcat into the repository.

**What it proves:** on Android 16 (API 36), the production `HiltWorkerFactory` builds the changed worker for work that
names `com.zegoggles.smssync.worker.BackupTriggerWorker` (after `install -r`, with the app's data kept), and the trigger
reaches `scheduleIncoming()`; when enqueued, the delayed incoming backup still runs. **What it does not prove:** a release
build on a device (SC-3; IC-4 covers the mapping), Android 11-era devices, the owner's phones, the receiver path.

## 10. Constraints for the Developer (binding)

1. Run Gradle in the foreground with the tool timeout set to 600000 ms on every call, and **never with
   `MSYS_NO_PATHCONV=1` set** (it breaks the wrapper's classpath). This story has no adb step for the developer.
2. If the tool moves a run to the background by itself, wait for its completion notice. Never poll; start no other
   command meanwhile.
3. No background or detached shells, no `&`, no `sleep` or polling loops, no helper agents for shell work, no heredocs
   for editing (Write and Edit tools only).
4. On "bash.exe … not found", or a process killed without a visible reason: stop and report (endpoint security). Do not
   work around it.
5. Raw logs, pristine copies and dex extracts stay in the work folder outside the repository. Only sanitised evidence
   enters `$W`. Never write an e-mail address, host name, user name or user-profile path into any repository file.
6. Take timestamps from `date -u +%Y-%m-%dT%H:%M:%SZ`; never type them.
7. **Commit nothing.** The orchestrator commits after the reviews. Never stage, stash, reset, amend, rebase or push.
8. Edit production code only as W1 to W3 say, plus the logged mutations, each restored and proven by blob, raw bytes and
   porcelain. Edit the test file only as 3.2 and T-F say (and F-LOG only under row D14 (a)).
9. No dependency, Gradle, build, ProGuard or verification-metadata change. **The only build-file change is Rule 15's
   floor line and its note (E-G2, E-G1; P-1, ruled).** No new exclusion. No `@Ignore`, no `Assume`. No edit to
   `WorkManagerSchedulerTest.kt` or any other existing test.
10. No new file under `app/` other than `BackupTriggerWorkerTest.kt`. No test text contains `@Test` except the 15
    annotations.
11. Do not touch the other work in the checkout: `sdlc/.skill-context.json`, `sdlc/.skill-delegation-audit.log`, other
    stories' workspaces and files (the U-068 and U-070 workspaces, BUG-024), the story file, the sprint plan, any design,
    requirement, contract or change record, and this workspace's plan and plan-gate files. The orchestrator may commit
    documents while you run; such commits are recorded (I6 step 4) and are never a reason to stop (row D29); a code
    commit by anyone is (row D30).
12. No emulator, no device, no `adb`.
13. **Do not call the external `diff` or `cmp`** (the product owner's decision of 2026-11-21, Record 18, after the
    endpoint-security tool quarantined Git's `diff.exe`). Compare with `git diff` (between commits, against the index or
    the working tree), `git diff --no-index` (`--quiet`: exit 0 equal, 1 different; `--stat` or `--numstat` for counts;
    plain output where the differences are read), or by comparing `sha256sum` values or `git hash-object` blobs for
    byte or content identity. Every comparison in this plan already has that form.
14. Keep shell activity modest: batch comparisons in one call, and no loop that starts hundreds of processes (the
    plan's loops run over at most a few dex files or eight paths). If a tool disappears, or a process is killed without
    a visible reason, stop and report; do not restore, replace or work around the tool.

## 11. Quality Attribute Coverage (ISO/IEC 25010)

| Characteristic | Finding |
|----------------|---------|
| Functional Suitability | Correctness and completeness: the trigger path now applies all four rules it lacked (network, degraded, auto backup, delay ≤ 0) and keeps the debounce; each rule has a test and a mutant (5.1, 4.8). Appropriateness: one builder serves both incoming paths |
| Performance Efficiency | Time behaviour: one extra preference read and a singleton lookup per trigger; the enqueue cost is unchanged. Resource use: with Wi-Fi only, backups wait for unmetered networks, which saves metered data by design |
| Compatibility | Co-existence and interoperability: the persisted `CONTENT_TRIGGER` work resolves through the unchanged class name and the factory's string key (IC-2 (b), `ic1_…`, IC-3); no port, contract or WorkManager API change; WorkManager 2.12.0 accessors verified (L11) |
| Usability | Not applicable to this story's scope (no UI). The visible effect is that automatic incoming backups now honour the Wi-Fi-only and auto-backup settings the user chose |
| Reliability | Fault tolerance: a declined enqueue returns success, so the periodic trigger is never failed or retried; `REPLACE` keeps single flight across both paths (AC-8). Recoverability: the backoff of the follow-up is the contract's 30 s exponential (AC-3). `REPLACE` also stops a running incoming backup (2.4 item 11; BUG-024), unchanged from before; a process death inside `doWork()` leaves the message to the next trigger or the regular backup (2.4 item 12) |
| Security | The U-054 (SE-002) degraded gate now covers the trigger path, so no automatic backup is enqueued while credentials are in plaintext (AC-4, M-T2). No credential, permission or exported-component change |
| Maintainability | Modifiability: a later change to `scheduleIncoming()` (BUG-021, WorkManager 2.12.0) covers both paths. Testability: the worker gets its first 15 tests, the AC-3 equality guards against a second builder, and assertion labels make failures readable. Analysability: the class comment states what the worker does |
| Portability | Installability and adaptability: unchanged on every supported level (minSdk 24 to targetSdk 35); the release build keeps the class name under two consumer keep rules (L9, IC-4) |

## 12. Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| U-069 story | `sdlc/artifacts/build/sprints/sprint-024/U-069-content-trigger-backup-goes-through-the-scheduler.md` | Read in full: AC-1 to AC-14, IC-1 to IC-4, M-T1 to M-T9, Affected Code, Technical Context, Scope Concerns, Not verified, the rulings |
| BUG-023 | `sdlc/artifacts/stories/BUG-023-content-trigger-backup-skips-scheduling-checks.md` | Suggested Approach (rulings 1 to 7) read in full; headings and Scope Concerns |
| CNTR-MODERNIZATION-004 v3 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` | Status; `:236-280`, `:365-390` |
| DES-MODERNIZATION-005 | `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` | `:133-138`, `:320-321`, `:434`, `:495` |
| DES-MODERNIZATION-008 | `sdlc/artifacts/design/modernization/DES-MODERNIZATION-008-hilt-di-design.md` | `@HiltWorker` lines by grep (`:113`) |
| REQ-MODERNIZATION-015 | `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-015-*.md` | AC-13 (`:165-180`), the count rule |
| U-068 plan, plan-review | `sdlc/artifacts/build/sprints/sprint-024/U-068/plan.md`, `plan-review.md` | Sections 0 to 4.9, 5, 6 (rows), 7, 8.1; the edits applied to scratch copies (L20); the blocker list |
| U-067 plan | `sdlc/artifacts/build/sprints/sprint-023/U-067/plan.md` | Structure, Amendment A1, sections 9 to 12, Verdict |
| U-066 plan | `sdlc/artifacts/build/sprints/sprint-022/U-066/plan.md` | Section 9 (emulator procedure) |
| U-066 IC-1 record | `sdlc/artifacts/build/sprints/sprint-022/U-066/evidence/IC1-emulator.txt` | Evidence format and log lines |
| U-070 plan | `sdlc/artifacts/build/sprints/sprint-024/U-070/plan.md` (HEAD) | Its references to U-069 (`u069-end`, Run R) |
| U-071, U-073 stories | `sdlc/artifacts/stories/U-071-*.md`, `U-073-*.md` | Hilt and WorkManager versions; U-073 AC-13's folder rule |
| Source at `4300ffa9` | `app/src/main/java/com/zegoggles/smssync/worker/BackupTriggerWorker.kt` (full), `scheduler/WorkManagerScheduler.kt` (full), `service/BackupWorker.kt` (`:16-60`, `:95-135`, `:576-634`), `scheduler/BackupScheduler.java` (`:40-60`), `di/SchedulerModule.kt`, `di/PreferencesModule.kt`, `App.java` (`:125-170`, `:225-240`, `:370-385`), `preferences/Preferences.java` (grep), `preferences/Defaults.java` (grep), `receiver/SmsBroadcastReceiver.java` (`:55-62`) | The code being changed and its neighbours |
| Tests at `4300ffa9` | `service/WorkManagerSchedulerTest.kt` (`:1-130`, `:130-494`, `:495-591`), `service/BackupWorkerTest.kt` (`:1-75`), `receiver/SmsBroadcastReceiverTest.java` (grep), `src/test/resources/robolectric.properties` | Harness, names, counts |
| Build files at `4300ffa9` | `app/build.gradle` (`:1-125`, `:140-330`, rules list, `:540-594`), root `build.gradle` (`:20-53`), `gradle.properties`, `app/proguard-rules.pro` (grep) | JaCoCo rules and filters, compiler options, keep rules |
| Generated build output (read-only) | `app/build/generated/ksp/debug/java/com/zegoggles/smssync/service/RestoreWorker_*.java`, `hilt_aggregated_deps/`, `app/build/generated/hilt/component_sources/debug/com/zegoggles/smssync/DaggerApp_HiltComponents_SingletonC.java` (`:588-615`); `app/build/intermediates/` class-root listings | What Hilt generates and where (L7) |
| Library artifacts (local Gradle cache, `javap` and sources) | `work-runtime` 2.9.1 (aar, `proguard.txt`), 2.12.0 (sources jar); `work-testing` 2.9.1; `hilt-work` 1.2.0 (aar, `proguard.txt`); `hilt-compiler` 1.2.0; `hilt-common` 1.2.0; `dagger` 2.59.2; `shadows-framework` 4.15.1; `robolectric` 4.15.1 module; `truth` 1.4.4 POM | L6 to L14 |
| Evidence tools and records | `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools/` (`sanitize.py`, `junit_totals.py`, `junit_cases.py`, `jacoco_pkgs.py`, `junit-xml.init.gradle`), `U-063/evidence/FIX-jacoco-packages.txt`, `sprint-018/U-061/evidence/I9-jacoco-packages.txt`, `sprint-017/U-060/evidence/{kept-after.txt,qa/qa-ac9-class-counts.txt}`, U-068's `evidence/tools/` listing | Tools reused; baseline figures |
| SDK listing | the SDK folder (`build-tools`, `platform-tools`, no `cmdline-tools`) | L18 |
| Standards | plugin cache `knowledge/standards/expert-exceeding-depth.md`, `truth-and-accuracy.md` (1.7.2); `templates/build-artifacts/build-plan.md`, `knowledge/standards/phase-completion-report.md` (1.9.0) | Depth criteria; template; report format |
| REQ and DES corpus | `sdlc/artifacts/requirements/modernization/`, `sdlc/artifacts/design/modernization/` | Not re-read in full; the story lists no REQ or DES; the cited lines above were read. No REQ with `type: standard` exists (`git grep`) |
| Iteration 2: plan-gate reports | `sdlc/artifacts/build/sprints/sprint-024/U-069/plan-review.md`, `contract-verification.md`, `expert-depth-check.md` | Read in full; the Iteration 2 table |
| Iteration 2: rulings | `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`, Record 18 (rulings 45, 46; the endpoint-security decision) | L26 |
| Iteration 2: coverage data | `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml` (read-only, Python); `app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/zegoggles/smssync/service/BackupWorker_{Factory,AssistedFactory_Impl}.class` (`javap -v`); `dagger.internal.DaggerGenerated` (`dagger-2.59.2.jar`); `AnnotationGeneratedFilter` (`org.jacoco.core-0.8.14.jar`) | L24, B-1 |
| Iteration 2: WorkManager periodic jobs | `androidx.work.impl.background.systemjob.SystemJobInfoConverter` (`work-runtime-2.9.1.aar`, `javap -c -p`) | L25, 2.4 item 13 |
| Iteration 2: BUG-024, U-070 story | `sdlc/artifacts/stories/BUG-024-cancelled-backup-or-restore-reported-as-failure.md` (title, status); `sdlc/artifacts/build/sprints/sprint-024/U-070-late-visible-sms-are-backed-up.md` (Affected Code, "Not touched" `:169`, `:323`) | 2.4 item 11; L26 |
| Iteration 2: git state | `git log` at `b097d602` (U-068 S1-I1 to S1-I4 committed); `git show` of the worker and scheduler blobs | Iteration 2 preamble |

## Verdict

**PASS.** The story can be built as written. Its facts hold against the code at `4300ffa9`, and against the post-U-068
text of the shared files, produced by applying U-068's own edits.

Every "not verified" item the tests depend on is now verified from the cached artifacts, or is a red-run probe with a
decision row and a fallback that still proves the criterion, or "stop and report":
- `ShadowLog`: L14, probe N2;
- the `WorkSpec` accessors on both WorkManager versions: L10, L11, probe N3;
- the 2.9.1 default backoff: 30 000 ms, L10;
- the two keep rules: L9;
- what Hilt generates for a plain `Worker`: L6, L7, N5.

The fix is the ruled Route A1 in `BackupWorker`'s form, with the log line byte for byte. Every edit and mutation is given
as exact text, checked once by a tool before the first edit, and tied to expected blobs.

The 15 tests cover AC-1 to AC-9 and IC-1. 11 of them must fail on the unchanged worker. The 11 counted mutants each name
the tests that must fail and those that must pass. The two Hilt-annotation failure modes have named detectors: test
`ic1_…` for `@HiltWorker`, the KSP step for `@AssistedInject`. IC-4 is explicitly the detector for neither.

**Iteration 2** takes up every gate finding (the Iteration 2 table). The JaCoCo premise is corrected from today's
coverage data: the generated classes report no lines, the `worker` ratio is expected to be 1.00, and the floor is 0.95
under ruling 46. IC-3 no longer calls an unobserved run a pass. The vacuous `git grep` check is replaced, and every
other final check was re-read for the same mistake. A hard-coded default delay is now caught (M-T12). No external `diff`
or `cmp` is called anywhere. P-1 and P-2 are ruled (Record 18 ruling 45); nothing is open for the orchestrator before the
build.

**Iteration 3** fixes B-4 by restricting the AC-12 (e) grep to the files AC-12 is about and checking the new test file
on its own. It labels the last two IC-3 cases and states the periodic trigger's user effect. Every expected value of
the final checks (I0 step 5, I3 step 2, I6 step 4) was confirmed by running the command in a scratch repository built
from `0d490997` with this plan's edits applied (L27).

## Phase Completion Report
---
story_id: "U-069"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-024/U-069/plan.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
notes: "Iteration 3: B-4 fixed (AC-12 (e) restricted to the AC-12 files: 4 lines; the new test file checked separately: 1 line, :402, an assertion label); IC-3 last two cases labelled; edge case 13 in plain words plus hand-over item 13; every final check run in a scratch repository built from 0d490997 (L27). Iteration 2: B-1 to B-3, plan-review O-1 to O-7, contract-verification O-1 to O-4 and both depth recommendations taken up, none declined. 15 tests, 12 mutation runs (11 counted, M-T11 compile-only), 18 Gradle runs; the developer commits nothing; no external diff or cmp. Worker floor 0.95 under an expected measured 1.00 (ruling 46). P-1 and P-2 ruled (ruling 45); nothing open."
---
