---
artifact_type: plan
story_id: "U-076"
gate_id: "planning"
checkpoint: "planning"
verdict: "NEEDS_INFO"
agent: "Solution Architect"
timestamp: "2026-11-21T22:40:00Z"
iteration: 1
base_commit: "2f58f927f503f016a87d7565110bcf636bbe5e36"   # PLAN_HEAD; app/ equals fd70325a there (git diff --stat fd70325a 2f58f927 -- app is empty); the start commit is read at I0 (the commit that closes U-072)
contracts_verified:
  - CNTR-MODERNIZATION-004
risks_identified: 12
---

# Implementation Plan: U-076 — The Regular Backup Survives a Restart, a One-Time Entry Left Under `REGULAR` Is Replaced, and "Never" Schedules Nothing (BUG-027, BUG-026; Contract v4, CR-017)

**DRAFT, incomplete (stopped 2026-11-21 at the owner's request). Not reviewed, not to be built from.**

**Resume notes**

- **Complete (drafted in full; not yet reviewed end to end):** sections 0, 1 (the assumption ledger with every WorkManager reading), 2 (design, P-1 to P-4, alternatives, edge cases, toolchain dependencies), 3 (exact changes; the embedded files E1 to E6 are final drafts and were tested, see 14), 4 (steps I0 to I7, mutation protocol and table, files, sessions), 5, 6, 7, 8 (conformance table, scope concerns SC-1 to SC-6, risks), 9 (the device section), 10, 11, 12, 13.
- **Partial:** section 14 (self-test): the rows run are listed. `u076_sets.py` (E4), `junit_cases.py` on constructed XML, `sanitize.py` samples and the hygiene scan of this file are **not yet run**. The Verdict section was drafted as if the plan were complete; the verdict of this draft is NEEDS_INFO.
- **Not started:** a final read-through of the whole plan against the story's AC-1 to AC-21 wording; a second, independent derivation of the failing sets (they were derived once, in sections 4.8 and 5.1).
- **Facts established, with their sources (do not re-derive).** All are in section 1, rows L1 to L20, read with `javap -c -p` from the cached `work-runtime-2.9.1.aar` and `work-testing-2.9.1.aar` (no sources jar is cached). P-1 in short:
  - *`UPDATE` against a periodic entry that has not run yet* (`WorkerUpdater.enqueueUniquelyNamedPeriodic`, then `WorkerUpdater.updateWorkImpl`): the id stays the same. `state`, `runAttemptCount`, `lastEnqueueTime`, `periodCount` and `nextScheduleTimeOverride` are kept, and `generation` goes up by 1 (`copy$default` mask `4447229`). There is no `Processor` stop call, so a running worker is not stopped. The stored initial delay is the new request's. A never-run entry's first run therefore becomes `lastEnqueueTime` plus the new delay (`WorkSpec$Companion.calculateNextRunTime`: `periodCount == 0` gives `lastEnqueueTime + initialDelay`). This is recorded as H-2, not a stop.
  - *`UPDATE` against a one-time entry*: `enqueueUniquelyNamedPeriodic` checks `isPeriodic` **before** it checks for `CANCELLED`. It therefore fails in every state (ENQUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED) with `UnsupportedOperationException` "Can't update OneTimeWorker to Periodic Worker. Update operation must preserve worker's type." The other branches: more than one entry fails with "chains of work"; no entry goes to `enqueueNew`; a cancelled periodic entry is deleted and then `enqueueNew` runs.
  - *Delivery*: the failure comes only through the returned `Operation` (`OperationImpl.markState(FAILURE)` sets the exception on `getResult()`). `enqueueUniquePeriodicWork` throws nothing.
  - *Fallback*: `WorkManagerImpl.createWorkContinuationForUniquePeriodicWork` maps `CANCEL_AND_REENQUEUE` to `ExistingWorkPolicy.REPLACE`. `EnqueueRunnable` then cancels by name, deletes every entry under the name whatever its type or state, and inserts the new spec.
  - *`cancelUniqueWork`*: `CancelWorkRunnable.cancel` calls `Processor.stopAndCancelWork`, so a running worker is stopped. Rule 4a needs this; Rule 4 forbids it only for a positive interval.
  - *Harness (P-2)*: `WorkManagerTestInitHelper.initializeTestWorkManager(context, config)` uses `LEGACY_OVERRIDE_WITH_SYNCHRONOUS_EXECUTORS`. That gives a synchronous task executor and a `SynchronousSerialExecutor` (re-entrant drain), and keeps the worker executor. `AbstractFuture.addListener` runs the listener at once when the future is already complete. So the fallback has run when the scheduler call returns, and no timed wait is needed.
  - *Device*: `SystemJobInfoConverter` never calls `setPeriodic`, and the diagnostics worker prints no periodicity column. Periodicity on the device must therefore be read from WorkManager's database (`androidx.work.workdb`, `WorkSpec.interval_duration`), copied together with its `-wal` file (SC-2).
  - *Code*: the scheduler blob `0194308c…` is the same at `fd70325a`, `74b31759` and `2f58f927`. The repository blobs are LF and the working tree is CRLF (`core.autocrlf=true`). The test count at `2f58f927` is 771. U-069's evidence gives scheduler 137/185 and receiver 39/58 at `fd70325a`.
- **Design alternatives compared** (section 2.2): A (UPDATE, then a listener with a gate re-check, then CANCEL_AND_REENQUEUE) is chosen. B (query the existing entry first) is rejected because it needs a blocking read on the boot receiver's main thread. C (always CANCEL_AND_REENQUEUE at boot) is rejected by Rule 4. D (a new unique name) is rejected by IC-2. No contract sentence needs to change for A.
- **Open questions and scope concerns** (section 8.2):
  - SC-1: U-071's possible `@param:ApplicationContext` edit against start condition S-3.
  - SC-2: the device's periodicity evidence must come from the database.
  - SC-3: the SUCCEEDED seed uses a test-local plain Worker.
  - SC-4: the fallback can stop a running one-time entry once, on upgrade (the Rule 4 sentence is quoted).
  - SC-5: where the plan is stricter than the story.
  - SC-6: two corrections to the story text.
  - Also: H-2 for the contract owner (section 10 item 3), and the residual race R4.
- **Built in scratch and kept here as fenced blocks:**
  - E1: the test class.
  - E2: the probe.
  - E3: the edit and injection tool, with all edit and injection texts.
  - E4: the derived-set checker (untested).
  - E5: the database reader.
  - E6: the UI finder.

  Their blobs are in their marker lines. The fixed production blobs are `3d4fe7608e41db561a557ead7b965704f6200478` (scheduler) and `bbb2945f5680ff5cb62c26b649907053cc46913a` (port).


Story: `sdlc/artifacts/build/sprints/sprint-025/U-076-regular-backup-survives-a-restart-and-never-schedules-nothing.md`
(binding: AC-1 to AC-21, IC-1, IC-2, P-1 to P-5, S-1 to S-4, hand-overs H-1 to H-3). Bugs:
`sdlc/artifacts/stories/BUG-027-regular-backup-removed-by-the-boot-backup.md`,
`sdlc/artifacts/stories/BUG-026-regular-schedule-never-still-schedules-a-backup.md`. Contract:
`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` at v4; design
`sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md`; change record
`sdlc/artifacts/build/change-records/CR-017-cntr-004-scheduler-never-boot-example-and-receiver.md`. Rulings: Records 18 and
20 to 23 of the decision appendix (55, 56, 60 to 62, 67, 68, 69 to 76; PO-G "Allow all of them"). Form:
`sdlc/artifacts/build/sprints/sprint-024/U-075/plan.md` (iteration 2) and `sdlc/artifacts/build/sprints/sprint-024/U-069/plan.md`.

**Planning state.** Planned at `PLAN_HEAD` `2f58f927` on `sdlc/modernization-plan`. `git diff --stat fd70325a 2f58f927 --
app/` is empty and the scheduler file has the same blob at `fd70325a`, `74b31759` and `2f58f927`
(`0194308cfcf9ff34effaf08c7990c087b0bedcbd`), so every code fact below is read at `fd70325a` with `git show` and cited
"`:n`" at that commit. The working tree under `app/` and `app/build` was not read (U-070's build session is running there).
No Gradle, build, test, device or network was run.

**The start commit is not fixed by this plan.** `<start>` is `HEAD` at I0: the commit that closes U-072 (sprint 025 order
U-074, U-071, U-072, U-076). `C0` (the authoritative count), the Gradle totals and the coverage figures are measured
there; every expected total is "start + N".

**Size.** Two tracked production files change: `scheduler/WorkManagerScheduler.kt` (`--numstat` `117 44`: two methods
rewritten, one private registration path and its listener added, five comment places, three imports) and
`scheduler/BackupScheduler.java` (`21 8`, javadoc only). One test file is new: `WorkManagerSchedulerRegularBootTest.kt`,
**30 tests** (23 that prove changed behaviour, 7 guards). No build, manifest, receiver, worker, contract or design file
changes. A throwaway probe class (5 tests) runs once at I2 and is deleted before the fix. **8 counted failure
injections** (the story's M-1 to M-7 and this plan's M-8). **12 Gradle runs**: two full gates (I0, I6) and ten targeted
runs (I2 1, I3 1, I5 8) whose class lists leave out `SmsBackupRecordSizeTest`. **One developer session.** The developer
commits nothing.

**How the edits are carried.** Every production edit and every injection is data in one tool, `u076_edits.py`
(embedded file E3), that checks each old text occurs exactly once (the anchor check of I0), applies the edits keeping
each file's line endings, checks the result, and applies one injection at a time. The plan text of the tool is the
single source: its blobs and the fixed files' blobs were derived from this plan file (section 14.1).

**Compile check.** The fixed scheduler, each of the eight injected schedulers, the new test class and the probe were
compiled in a scratch folder with the cached `kotlin-compiler-embeddable` **2.4.21** (the Kotlin that U-071 brings, its
plan 2.1 row 3), without Gradle and without network: against the SDK's `android-37.0/android.jar`, the cached
`work-runtime-2.9.1` and `work-testing-2.9.1` classes, `listenablefuture` 1.0, `kotlin-stdlib` 2.4.21, Mockito 5.24.0,
JUnit 4.13.2, Truth 1.4.4, Guava 33.0.0, Robolectric **4.16.1** (the newest cached; Start has 4.18.1) and **stubs** of the
app classes the scheduler names, written from the signatures read at `fd70325a` (`Preferences.java:115-205`,
`SecretStore.java:18`, `:85`, `DataTypePreferences.java:26`, `Consts.java:33-34`, `BackupType.java:7-12`). All exit 0; the
test class gives one deprecation warning (`RuntimeEnvironment.application`, the form `WorkManagerSchedulerTest.kt:56`
uses). The test class and the probe also compile against the **unchanged** scheduler (the red run's configuration). The
stubs are mine, not Gradle's: the first real compile is the developer's I2 run (row D10).

## 0. Binding Inputs

| Source | What binds this plan |
|--------|----------------------|
| Story AC-1 to AC-21, IC-1, IC-2, P-1 to P-5, S-1 to S-4, binding constraints | As written. Where this plan is stricter (AC-6's tests are written at I1 and run red at Start; AC-1's device evidence by WorkManager's database; M-8; the hunk check of AC-15), section 8.2 says so |
| CNTR-MODERNIZATION-004 v4 | The `scheduleRegular()` and `scheduleBootup()` blocks (`:63-71`, `:76-83`), the boot bullet of the `BackupType` notes (`:162-163`), the `REGULAR` row and "No `BOOT` unique name is used" (`:174`, `:181`), `initialDelaySecs` (`:200-203`), the `ExistingPolicy` note (`:221-224`), INV-1's realization (`:278`), K-1 (`:292`), the Changelog v4 (`:297`), the Breaking change policy (`:309`), Rules 4 and 4a (`:319-320`), the regular example (`:397-411`) |
| DES-MODERNIZATION-005 | The INV-1 row (D-23, `:378`), the "Never" row (D-24, `:382`), the port rows (`:438`, `:440`), `bootDelaySecs` (`:460`), the risk row (`:500`) |
| CR-017 | "What the story that fixes BUG-026 and BUG-027 must cite" (`:480-508`) with Amendment 1's changes (`:1565-1574`); close-out edit X-1 (`:1203`); "Order between the two stories" (`:1710-1717`) |
| Ruling 68 (Record 22) | PO-1 and PO-2 are built "no" |
| Rulings 55, 75 (Records 20, 23) | One story for both bugs; sprint 025 order U-076, U-077, U-078 after U-072, before U-073 |
| Ruling 73 (Record 23) | Section 14: every command and tool that can run without Gradle, a device or the working tree under `app/` was run here |
| Record 18 | No external `diff` or `cmp`; `git diff --no-index` or `sha256sum`; modest shell activity; stop on a vanished tool or a killed process |
| Record 20, PO-G | Device step (iii) may toggle the regular schedule on `Pixel_9_Pro` and must put it back |
| REQ-MODERNIZATION-015 AC-13 as amended by CR-016 | The count rule (AC-19) |
| REQ-MODERNIZATION-016 AC-8 | U-073 carries these tests unedited across WorkManager 2.12.0 (hand-over H-1, section 10) |

## 1. Assumption Ledger

`read` = read in this session, with where (WorkManager facts: `javap -c -p` of the classes in the cached
`work-runtime-2.9.1.aar` and `work-testing-2.9.1.aar`; no sources jar is cached). `needs_check` = a developer step with a
decision row. "2.9.1" is the version at Start (S-4).

| # | Assumption | Status | Evidence / where checked |
|---|------------|--------|--------------------------|
| L1 | `WorkManagerImpl.enqueueUniquePeriodicWork` sends `UPDATE` to `WorkerUpdater.enqueueUniquelyNamedPeriodic`; every other policy goes to `createWorkContinuationForUniquePeriodicWork`, which maps `KEEP` to `ExistingWorkPolicy.KEEP` and everything else (`CANCEL_AND_REENQUEUE`, deprecated `REPLACE`) to `ExistingWorkPolicy.REPLACE` | read | `WorkManagerImpl` bytecode (`getstatic UPDATE`, `if_acmpne`; `KEEP`→`KEEP`, else `REPLACE`) |
| L2 | `enqueueUniquelyNamedPeriodic`, on the serial task executor, in this order: more than one entry → fail "Can't apply UPDATE policy to the chains of work."; none → `enqueueNew`; `WorkSpec` missing → fail `IllegalStateException`; **not periodic → fail "Can't update OneTimeWorker to Periodic Worker. Update operation must preserve worker's type."** (before any state test); `CANCELLED` → delete, then `enqueueNew`; otherwise `updateWorkImpl` with the new spec copied onto the old id, then `SUCCESS`, or `FAILURE(throwable)` on any throwable | read | `WorkerUpdater` bytecode, offsets 45 to 356 of the lambda |
| L3 | `updateWorkImpl`: a finished entry → `NOT_APPLIED`; a type change → throws `UnsupportedOperationException`; if `Processor.isEnqueued(id)` is false the schedulers' jobs are cancelled and rescheduled, if true nothing is stopped (`APPLIED_FOR_NEXT_RUN`); the stored spec is the new request's with **`state`, `runAttemptCount`, `lastEnqueueTime`, `periodCount`, `nextScheduleTimeOverride` (and its generation) kept and `generation` + 1** (`copy$default` mask `4447229` = `0x43DBFD`: exactly parameters 1, 10, 13, 18, 19, 20, 21 passed); the tags are replaced. No `Processor.stop*` call | read | `WorkerUpdater.updateWorkImpl` and its transaction lambda |
| L4 | The failure reaches the caller only through the returned `Operation`: `OperationImpl.markState(FAILURE)` sets the exception on `getResult()`'s future (and posts the state); `enqueueUniquePeriodicWork` itself throws nothing for it, because the check runs inside the executor task | read | `OperationImpl.markState`; L2 |
| L5 | `EnqueueRunnable` with `ExistingWorkPolicy.REPLACE`: `CancelWorkRunnable.forName(name, …, false).run()`, then `WorkSpecDao.delete(id)` for **every** entry under the name, whatever its type or state, then inserts the new spec | read | `EnqueueRunnable.enqueueWorkWithPrerequisites`, offsets 513 to 641 |
| L6 | `cancelUniqueWork` → `CancelWorkRunnable.cancel` → `iterativelyCancelWorkAndDependents` and `Processor.stopAndCancelWork(id, …)`: **a running worker is stopped** | read | `CancelWorkRunnable` bytecode |
| L7 | Next run of a periodic spec: `periodCount == 0` → `lastEnqueueTime + initialDelay`; otherwise `lastEnqueueTime + interval` (flex aside); an override wins when set | read | `WorkSpec$Companion.calculateNextRunTime` |
| L8 | `WorkManagerTestInitHelper.initializeTestWorkManager(context, config)` uses `ExecutorsMode.LEGACY_OVERRIDE_WITH_SYNCHRONOUS_EXECUTORS`; with no task executor configured (the harness sets only `setExecutor`, `WorkManagerSchedulerTest.kt:62-67`) it installs `SynchronousExecutor` as the task executor and `SynchronousSerialExecutor` as the serial executor; the worker executor (the harness's single thread) is kept | read | `WorkManagerTestInitHelper` (tableswitch case 1 = `LEGACY_…`, from `WorkManagerTestInitHelper$1`), `TestWorkManagerImplKt` |
| L9 | `SynchronousSerialExecutor.execute` queues the task and, unless a drain is already running, drains the queue on the calling thread: a task submitted from inside a task runs after it, still inside the outer call | read | its bytecode (`isRunning`, `removeFirstOrNull`) |
| L10 | The test task executor's main-thread executor is the same `SynchronousExecutor` (runs inline) | read | `TestWorkManagerImplKt$createTestWorkManagerImpl$taskExecutor$1` |
| L11 | `AbstractFuture.addListener` on a completed future runs the listener at once on the given executor | read | `androidx.work.impl.utils.futures.AbstractFuture.addListener` (`TOMBSTONE` branch → `executeListener`) |
| L12 | `TestScheduler`: a spec with constraints is not run until `setAllConstraintsMet`; one with an initial delay not until `setInitialDelayMet`; so a `BackupWorker` with a network constraint never runs in the harness unless driven | read | `TestScheduler.isRunnableInternalState`, `schedule`; `TestDriver` |
| L13 | A plain `Worker` runs `doWork()` on `getBackgroundExecutor()` (the configuration's executor) | read | `Worker.startWork` |
| L14 | `WorkInfo` (2.9.1) has `getId`, `getState`, `getTags`, `getGeneration`, `getConstraints`, `getInitialDelayMillis`, `getPeriodicityInfo` (`getRepeatIntervalMillis`), `getNextScheduleTimeMillis`, `getStopReason`; **no backoff accessor** | read | `javap -p androidx.work.WorkInfo`, `WorkInfo$PeriodicityInfo` |
| L15 | WorkManager's JobScheduler jobs are never periodic: `SystemJobInfoConverter` calls `setMinimumLatency` and never `setPeriodic`; periodicity travels only as the extra `EXTRA_IS_PERIODIC` | read | `SystemJobInfoConverter` (the `JobInfo$Builder` methods it calls, listed) |
| L16 | The diagnostics worker logs at INFO with tag `WM-DiagnosticsWrkr` the columns "Id, Class Name, Job Id (or Alarm Id), State, Unique Name, Tags" under "Recently completed work", "Running work", "Enqueued work": **no periodicity column** | read | `DiagnosticsWorker`, `DiagnosticsWorkerKt` strings |
| L17 | WorkManager's database is `androidx.work.workdb`; tables `WorkSpec` (`interval_duration`, `initial_delay`, `period_count`, `generation`, `state` …), `WorkName`, `WorkTag` | read | `WorkDatabasePathHelperKt`; `WorkDatabase_Impl$1` `CREATE TABLE` strings (used verbatim in self-test 14.9) |
| L18 | The app's WorkManager configuration logs from INFO (no `setMinimumLoggingLevel`), so the diagnostics lines (INFO) reach logcat and WorkManager's DEBUG lines do not | read in U-075's plan (L29, `App.java:162-165`); not re-read | — |
| L19 | `BackupWorker` is a `CoroutineWorker` (`BackupWorker.kt:116`); `TestableBackupWorkerFactory` builds it with a transport that throws (`:591`) and returns `null` for any other class (`:596`), so WorkManager's default factory builds other workers by reflection | read | `BackupWorker.kt` at `fd70325a` |
| L20 | Kotlin compiler warnings are not errors in `:app` (`kotlin { compilerOptions { jvmTarget } }` only, `app/build.gradle:104-109`; `-Werror` is javac's, `:203`; lint's `warningsAsErrors` `:67` does not run in a unit-test task) | read | `app/build.gradle` at `fd70325a` |
| N1 | The test class and the probe compile on Gradle's unit-test classpath at Start (Kotlin 2.4.21, Robolectric 4.18.1, Mockito 5.24.0, Truth 1.4.5) | needs_check | I2 (row D10); compiled here against 4.16.1 and Truth 1.4.4 |
| N2 | The probe's runtime readings confirm L2 to L4, L7 to L13 in the harness | needs_check | I2 (rows D11 to D14) |
| N3 | The reflective `WorkSpec` read works in the harness | needs_check | I2: AC-2's two tests must fail only at their "periodic" assertion (they reach the reflective read only after the fix); I3 (row D10). It is `BackupTriggerWorkerTest`'s helper, green since U-069 |
| N4 | `ShadowLog.getLogsForTag(TAG)` returns the scheduler's `Log.d`/`Log.w` entries with `type`, `msg` | needs_check | I3 green; `BackupTriggerWorkerTest.kt:259`, `:280` use the same API at `fd70325a` |
| N5 | JaCoCo's LINE counting at Start (0.8.16, U-071) | needs_check | I0 and I6 under the same version; floors read, never changed |

## 2. Design (decided)

### 2.0 Principles applied

- **One registration path.** Both port methods reach the regular backup through `registerRegular(caller, initialDelaySecs)`; the "Never" rule, the request, the policy and the fallback live there once, so each injection is one edit and cannot diverge between the two methods.
- **Gates first and unchanged.** The degraded check and the auto-backup check stay the first two statements of both methods, with today's texts (`:171-178`, `:263-271`).
- **No blocking, no new seam.** The `Operation`'s result is read by a listener on a direct executor; nothing waits on it (story constraint "No blocking call"). No new constructor parameter, interface, executor injection or dependency.
- **Contract text as the specification.** Each sentence of v4 that this story makes true is mapped to a line of code and a test (section 8.1); where the code cannot make a sentence literally true, that is a scope concern with the sentence (8.2).

### 2.1 The fix

**`scheduleRegular()`** (`:170-202` → fixed `:185-195`): degraded gate; auto-backup gate (returns `null`, cancels nothing:
PO-D is not this story's); then `return registerRegular("scheduleRegular", 0L)`.

**`scheduleBootup()`** (`:262-286` → fixed `:263-274`): degraded gate; auto backup off → `cancelAll()`, `null` (Rule 4,
first sentence); then `return registerRegular("scheduleBootup", BOOT_BACKUP_DELAY_SECS)`. No `OneTimeWorkRequest`, no
`enqueueUniqueWork` (AC-5).

**`registerRegular(caller, initialDelaySecs)`** (new, fixed `:285-322`):
1. reads `regularTimeoutSecs` **once**; `<= 0` → `Log.d` "WorkManagerScheduler.<caller>: skipped (regular schedule is
   Never, regularTimeoutSecs=<v>); cancelling REGULAR", `cancelRegular()`, `null` (Rule 4a; both methods);
2. `intervalSecs = maxOf(value, 900)` (unchanged rule, `:180`);
3. today's periodic request unchanged (`:182-190`): `BackupWorker`, `networkConstraints(REGULAR)`, `EXPONENTIAL` 30 s,
   tag `REGULAR`; `setInitialDelay(initialDelaySecs)` only when it is positive;
4. `enqueueUniquePeriodicWork("REGULAR", UPDATE, request).result`;
5. `Log.d` "WorkManagerScheduler.<caller>: registered REGULAR (UPDATE) with interval=<n>s, firstRunDelay=<d>s,
   network=<…>" (the boot line of the story's Terms for `caller = scheduleBootup`);
6. `result.addListener({ onRegularUpdateResult(caller, result, request) }, Executor { it.run() })`;
7. returns `ScheduledJob("REGULAR", "WorkManagerScheduler:REGULAR @ <n>s")` (today's text for `scheduleRegular`, `:201`).

**`onRegularUpdateResult(caller, result, request)`** (new, fixed `:333-359`): `result.get()` on the completed future (no
wait); success → return. Failure (`ExecutionException` cause, or any other exception from `get`): reads the three gates
**once each, unconditionally** (degraded, auto backup, interval); any closed → one `Log.w` "…: UPDATE failed on REGULAR
(<cause class>); not re-registered, a gate closed (degraded=…, autoBackup=…, regularTimeoutSecs=…)" and return; all open
→ one `Log.w` "…: UPDATE failed on REGULAR (<cause class>: <message>); re-registering with CANCEL_AND_REENQUEUE" (the
fallback warning of Terms) and `enqueueUniquePeriodicWork("REGULAR", CANCEL_AND_REENQUEUE, request)` with the same
request (L1, L5: deletes every entry under `REGULAR`, inserts the periodic one). Its own result is not listened to: no
loop. The listener holds `context` (the application context, `:80`), `preferences`, `secretStore`, the request and a
string: nothing of an Activity.

**Comments** (AC-16): the class comment's INV-1 lines (`:40-42`) and unique-name lines (`:67-69`), the
`PERIODIC_MIN_INTERVAL_SECS` comment (`:112-115`), both KDocs, and in `BackupScheduler.java` `:36` (v2 → v4), the
`scheduleRegular()` javadoc (`:58-66`), the call-log condition (`:71-72`) and the `scheduleBootup()` javadoc (`:83-90`).
`BackupScheduler.java:32-33` is U-077's and is not touched. The exact texts are E3's `W1` to `W6`, `B1` to `B4`.

The log texts change: "enqueued REGULAR with interval=" (`:199`) and "enqueued bootup REGULAR with 60s delay" (`:283`)
are gone. Nothing in `app/src` parses them (BUG-026 Suggested Approach, "Risks"); `bootup` (`BootReceiver.java:32`) is
unchanged. Section 10 item 4 lists the documents that grep the old texts.

### 2.2 P-2: how the fallback is observed, and the alternatives against the contract

**Production.** The direct executor runs the listener on the thread that completes the result: WorkManager's serial
task thread when the `UPDATE` task finishes after `addListener`, or the caller's thread at `addListener` when it finished
before (L11). Either way nothing blocks: `get()` runs only on a completed future. The `CANCEL_AND_REENQUEUE` enqueue from
inside the serial task thread only posts a task behind the current one (it never waits), so there is no deadlock.

**Harness (P-2 answered).** The harness's task executor is synchronous and its serial executor drains on the calling
thread (L8, L9). So inside `scheduleRegular()`/`scheduleBootup()`: the `UPDATE` task runs and completes the result before
`enqueueUniquePeriodicWork` returns; `addListener` finds it complete and runs the listener at once (L11); the fallback's
enqueue runs synchronously too. **When the scheduler call returns, the fallback has run and its effect is in the
database.** The tests read state right after the call: no sleep, no timed wait, no latch. The probe (E2, I2) checks
`isDone` right after the call and that a direct-executor listener ran before `addListener` returned (stop condition of
P-2 if not, row D12).

**AC-6 (the gate re-check) is therefore testable, not only readable.** The first pass reads each gate once and the
fallback reads each gate once more (2.1), all on the test thread, in a fixed order; Mockito's consecutive stubs
(`thenReturn(open, closed)`) close one gate between the two reads. Three tests, one per gate (5.1, T13 to T15).

**Alternatives** (the contract's v4 text fixes `UPDATE` first, `CANCEL_AND_REENQUEUE` only when `UPDATE` fails: `:65-67`,
`:174`, `:221-224`, `:278`, Rule 4 `:319`):

| Option | What it does | Against v4 | Testable | Decision |
|--------|--------------|------------|----------|----------|
| A. `UPDATE`, listener on the `Operation`, fallback `CANCEL_AND_REENQUEUE` with a gate re-check | 2.1 | As written | Yes, deterministically (above) | **Chosen** |
| B. Read `getWorkInfosForUniqueWork("REGULAR")` first and pick the policy | One extra query | The policy text holds, but the read must block (`.get()`) on the boot receiver's main thread, or become asynchronous and be option A with a different trigger | Yes | Rejected: the story forbids a blocking read (P-1 (b), "No blocking call"); an asynchronous read adds a second race window (the state may change between read and enqueue) |
| C. Always `CANCEL_AND_REENQUEUE` at boot | Periodic REPLACE every boot | Contradicts Rule 4 "MUST NOT cancel or reset a registered or running regular backup" and the `scheduleBootup()` block's "UPDATE keeps an existing registration" | Yes | Rejected (also cancels a running regular backup, L5, L6) |
| D. A new unique name for the periodic work, one-off migration | Leaves `REGULAR` to old entries | Contradicts IC-2 ("No new unique name"), the `REGULAR` row and "No `BOOT` unique name is used" | Yes | Rejected |

No contract sentence has to change for option A (but see SC-2 and SC-4 in 8.2 for two sentences the code makes true
only in a qualified sense).

### 2.3 P-1, P-3, P-4: the library facts, with the stop conditions

| Point | Finding (2.9.1) | Source | Stop condition (story) | Met? | Runtime confirmation |
|-------|-----------------|--------|------------------------|------|----------------------|
| P-1 (a) id kept | Yes: the new spec is copied onto the existing id | L2 | id not kept → stop | No | probe `p1a` (STOP line) |
| P-1 (a) generation | + 1 exactly, also when only the initial delay differs | L3 | — (AC-1 rests on it) | — | probe `p1a`; AC-1 |
| P-1 (a) `lastEnqueueTime`, `periodCount` | Kept | L3 | reset → stop | No | probe `p1a` (STOP lines) |
| P-1 (a) first run of a never-run registration | **Moves:** next run = original `lastEnqueueTime` + the new initial delay (boot: + 60 s; a later `scheduleRegular()`: + 0, so a pending 60 s boot delay is dropped). A registration that has run (`periodCount` ≥ 1) keeps its next run (`lastEnqueueTime` + interval) | L3, L7 | Recorded, not a stop | — | probe `p1a` (H-2 lines); handed over as H-2 (section 10 item 3) |
| P-1 (a) running worker | Not stopped by `UPDATE` (no stop call; `APPLIED_FOR_NEXT_RUN`) | L3 | cancelled → stop | No | not testable in the harness (AC-5 note) |
| P-1 (b) one-time entry | `UPDATE` fails in **every** state (type check before the state check) | L2 | does not fail → stop | No | probe `p1b` ×2, `p3` |
| P-1 (b) delivery | Only through `Operation.getResult()` failing with `ExecutionException` (cause `UnsupportedOperationException`); nothing thrown | L4 | not observable without blocking → stop | No | probe `p1b` (STOP P-2 lines) |
| P-1 (b) `CANCEL_AND_REENQUEUE` | Exists (compiles; L1); maps to `ExistingWorkPolicy.REPLACE`, which deletes the one-time entry and inserts the periodic one | L1, L5 | does not remove → stop | No | probe `p1b`, `p3` |
| P-1 (c) cancelled periodic | Deleted, then the new request is enqueued (new id) | L2 | outcome differs from BUG-027 → stop | No | probe `p1c` |
| P-1 (c) absent name | `enqueueNew` registers | L2 | same | No | probe `p1c` |
| `cancelUniqueWork` on a running regular backup | **Stops it** (Processor.stopAndCancelWork) | L6 | — | — | Rule 4 limits "MUST NOT cancel … running" to a positive interval; Rule 4a requires the cancel under "Never". No conflict; the stopped run ends as U-074's "run cancelled" |
| P-3 SUCCEEDED seed | Reachable with a plain `Worker` (L12, L13, L19): `TestDriver` meets the delay and constraints, the run is handed to the harness's single worker thread, and a no-op task submitted to that thread afterwards returns only when the run and its state write (synchronous serial executor, L9, L10) are done: a completion barrier, not a timed wait. `BackupWorker` itself cannot be driven (a `CoroutineWorker`, and its test transport throws) | L9, L10, L12, L13, L19 | not a stop | — | probe `p3`; if it fails, row D12 removes the two (c) tests and records "not tested" |
| P-4 accessors | All five exist; backoff is not on `WorkInfo`, so INV-3 is read from the `WorkSpec` by U-069's reflective helper | L14 | — | — | AC-2's tests |

**The probe** (E2, `U076LibraryProbeTest`, 5 tests, run once with the red run at I2, deleted at I3 step 0, never counted
or committed) calls WorkManager directly with the harness's set-up. Each assertion message starts with the stop
condition it guards (`STOP P-1 (a)`, `STOP P-1 (b)`, `STOP P-1 (c)`, `STOP P-2`), with `H-2 (recorded, not a stop)`, or with
`P-3`. Every `STOP` assertion of `p1a` precedes its `H-2` assertions, so an `H-2` failure means every stop condition
passed. It prints its readings (`P-1a …`, `P-1b cause …`, `P-1c …`, `P-3 …`) into the XML's system-out for the record.

**A correction to the story's text.** AC-2 says INV-3 is "read from the `WorkSpec` as
`inv3_backoffPolicy_exponential_regularJobEnqueued`, `:283`, does", and P-4 names "the test helper the existing `inv3_*`
tests use (`:283`)". That test reads only the state (`WorkManagerSchedulerTest.kt:283-293`); no `inv3_*` test reads the
`WorkSpec`. The reflective helper is `BackupTriggerWorkerTest`'s (U-069 Amendment A4). The plan uses it; no criterion
changes (section 10 item 8).

### 2.4 Edge cases (behaviour after the fix)

1. **Settings change** (`App.rescheduleJobs()`, `App.java:373-385`): `cancelAll()` then `scheduleRegular()`. The periodic entry is `CANCELLED` first, so `UPDATE` deletes it and registers a new one (L2): a settings change restarts the schedule, as today. Unchanged behaviour.
2. **Package replaced over a BUG-027 phone**: `cancelAll()` cancels the one-time entry, `UPDATE` fails on it (type), the fallback replaces it (BUG-027 Suggested Approach, "Upgrade"; AC-3 (b); device (ii)).
3. **Boot over a registered regular backup**: in place, same id, generation + 1; a registration that has run keeps its next run; a never-run one gets `lastEnqueueTime` + 60 s (H-2), in practice already past at boot, so it runs when constraints and quota allow.
4. **Boot with nothing registered** (a phone hit by BUG-027 whose boot entry was pruned): `enqueueNew`, first run 60 s after boot (AC-2).
5. **"Never" with a running regular backup**: cancelled and stopped (L6); Rule 4a requires the cancel. After U-074 the stopped run logs `BackupWorker: run cancelled` at INFO.
6. **"Never" while auto backup is off**: `scheduleRegular()` returns `null` at the auto-backup gate and cancels nothing (unchanged; PO-D); `scheduleBootup()` with auto off cancels all (unchanged).
7. **Degraded with "Never"**: the degraded gate returns first; nothing is cancelled (AC-13 (b)).
8. **The fallback over a running one-time entry**: `CANCEL_AND_REENQUEUE` stops it (L5, L6). Only reachable on the first registration after upgrading from a build with the old boot path; see SC-4.
9. **Race between the fallback and a later cancel** (production only): the listener reads the gates on the serial task thread and posts the `CANCEL_AND_REENQUEUE` task; a `cancelAll()` that the main thread posts between that read and that post runs first, and the fallback then registers. The window is between two statements of one method and exists only while a one-time entry sits under `REGULAR` (once per upgraded phone). The re-check closes the case BUG-027 "Risks" names (a gate closed before the fallback runs); this narrower one stays open. Risk R4.
10. **Other `Operation` failures**: "chains of work" (impossible with this code) and a missing `WorkSpec` also take the fallback, which repairs both; a failure of the fallback's own enqueue is not listened to (no loop) and is visible only in WorkManager's logs. A database error in `UPDATE` likewise takes the fallback once.
11. **Thread of the listener in production**: WorkManager's serial task thread or the caller's thread. It reads `SharedPreferences` through `Preferences` and `SecretStore.isEncryptionDegraded()`; both are read today from worker threads (`BackupWorker` reads `Preferences` off the main thread), so no new threading rule. `EncryptedPrefsSecretStore.isEncryptionDegraded()` (`:317`) was not read for thread safety: R6.
12. **Never-run boot registration followed within 60 s by a settings event**: the settings path cancels and re-registers (item 1), so the boot delay is dropped; not a regression (today the settings event did the same).

### 2.5 Toolchain dependencies (U-071, U-072 change these before Start)

| Dependency | Why the plan needs it | What the developer checks at I0 | If it differs |
|------------|-----------------------|---------------------------------|---------------|
| Kotlin language and compiler (2.4.21 expected) | The production edit uses a Java SAM constructor (`Executor { it.run() }`), a `try`-expression whose `try` branch ends in `return`, `Future<*>` | The `kotlin-stdlib` line of `app/build.gradle`; the I2 compile (row D10) | A compile error in the production edit stops at I3 (row D10); compiled here with 2.4.21 |
| Robolectric (4.18.1 expected) | `RuntimeEnvironment.application`, `ShadowLog.getLogsForTag`, `LogItem.type`/`msg`, `@Config(sdk = [29])` | The `robolectric` line; G0 green (WorkManagerSchedulerTest and BackupTriggerWorkerTest use the same APIs) | D10 |
| Mockito (5.24.0 expected) | Consecutive stubs `thenReturn(a, b)` on mocks of a Java class and a Java interface with a default method (`SecretStore.isEncryptionDegraded`, `:85`) | The `mockito-core` line | D10 |
| Truth (1.4.5 expected) | `assertWithMessage`, `containsAtLeastElementsIn`; failure types `AssertionErrorWithFacts` / `ComparisonFailureWithFacts` (`u076_sets.py` accepts both and JUnit's) | The `truth` line | A new failure type name shows as `FAIL-WRONG-REASON` in `u076_sets.py`: row D15 |
| JUnit XML location `app/build/test-results/testDebugUnitTest` and its `message` attribute form (`<class>: <message>` with `&#10;`) | `u076_sets.py` reads it | G0 writes XML there (`junit_totals.py` exit 0) | Row D7: point `$R` at the folder AGP writes and record it |
| JaCoCo counting (0.8.16) | The scheduler package's LINE ratio | I0 and I6 measured under the same version | Floors are never changed (row D16) |
| AGP debug APK path | The device hand-off | `command ls -1 app/build/outputs/apk/debug/` at I0 step 5 | Use the one `.apk` listed and record its name |
| WorkManager 2.9.1 (S-4) | Every L-row | `app/build.gradle:155-156` text | Stop (S-4) |

## 3. Exact Changes

### 3.1 Production edits (carried by E3; applied at I3 step 2)

`$S` = `app/src/main/java/com/zegoggles/smssync`. The developer does not type these edits: `python -I -X utf8
$TL/u076_edits.py . apply` applies them after checking every old text once. Old texts are matched with line endings
normalised to LF; each file is written back with the line endings it had (the working tree of this checkout is CRLF
under `core.autocrlf=true`; the repository blobs are LF: 481 LF and 0 CR in the scheduler blob, read by byte count).

| Id | File | Place (`fd70325a`) | Change |
|----|------|--------------------|--------|
| W1 | `$S/scheduler/WorkManagerScheduler.kt` | class comment INV-1, `:40-42` | REPLACE one-off; UPDATE periodic; REGULAR's fallback; the boot path never enqueues a one-off under REGULAR (AC-16 (d)) |
| W2 | same | class comment unique names, `:67-69` | the boot path has no unique name of its own; manual/skip named (AC-16 (e)) |
| W3 | same | `PERIODIC_MIN_INTERVAL_SECS` comment, `:112-115` | the floor applies to positive values; non-positive = "Never" (AC-16 (a)) |
| W4 | same | `scheduleRegular()` KDoc and body, `:160-202` | 2.1 (AC-16 (b)) |
| W5 | same | `scheduleBootup()` KDoc and body, `:252-286`, plus the new `registerRegular` and `onRegularUpdateResult` after it | 2.1 (AC-16 (c)) |
| W6 | same | imports, `:31-32` | `ExecutionException`, `Executor`, `Future` |
| B1 | `$S/scheduler/BackupScheduler.java` | `:36` | "v2" → "v4" |
| B2 | same | `:58-66` | `scheduleRegular()` javadoc: UPDATE, the fallback, "Never", `null` (AC-16 (f)) |
| B3 | same | `:71-72` | "when call-log backup is enabled and `isCallLogBackupAfterCallEnabled()`" (CR-017 Amendment 1, M-2's hand-over; code `:227-228`) |
| B4 | same | `:83-90` | `scheduleBootup()` javadoc as AC-16 (c), in port terms |

Result (from the plan's own E3 applied to the `fd70325a` blobs, section 14.2): `WorkManagerScheduler.kt` 554 lines,
blob **`3d4fe7608e41db561a557ead7b965704f6200478`**; `BackupScheduler.java` 197 lines, blob
**`bbb2945f5680ff5cb62c26b649907053cc46913a`** (`git hash-object <path>`, which normalises the CRLF working copy to the LF
blob; tested in a scratch repository with `core.autocrlf=true`, 14.3). `--numstat` `117 44` and `21 8`. Every hunk lies in
an allowed place of AC-15 (old ranges 30, 41-42, 67-69, 114, 161-201, 253-285; port 36, 62-65, 72, 84-89); the `hunks` mode
of E3 checks it, and that every changed port line is a comment line.

Fixed-file sites for the reviewers: `scheduleIncoming` `:142`, `scheduleRegular` `:185`, `scheduleBootup` `:263`,
`registerRegular` `:285` (the "Never" block `:287-292`, `UPDATE` `:312`, `addListener` `:320`), `onRegularUpdateResult`
`:333` (`CANCEL_AND_REENQUEUE` `:356`), `scheduleImmediate` `:376`. `OneTimeWorkRequest` occurs at `:154`, `:382`, `:423`,
`:458` only (incoming, immediate, manual, restore): none in `scheduleBootup()` or the registration path (AC-5).

### 3.2 New test file (I1 step 1, Write tool)

**Embedded file E1** — `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerRegularBootTest.kt` (blob
`eac2af9f27e3db2c3914915e584253a398356303`, 557 lines, 30 `@Test`, LF):

```kotlin
package com.zegoggles.smssync.service

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertWithMessage
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.preferences.DataTypePreferences
import com.zegoggles.smssync.preferences.Preferences
import com.zegoggles.smssync.preferences.SecretStore
import com.zegoggles.smssync.scheduler.ScheduledJob
import com.zegoggles.smssync.scheduler.WorkManagerScheduler
import org.junit.After
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * U-076 (BUG-026, BUG-027; CNTR-MODERNIZATION-004 v4, Rules 4 and 4a; DES-MODERNIZATION-005, the INV-1 row
 * (D-23) and the "Never" row (D-24)): the regular backup's one registration path, the boot path, the fallback
 * over a one-time entry under REGULAR, and the regular schedule "Never".
 *
 * The harness is WorkManagerSchedulerTest's (Robolectric at SDK 29; WorkManagerTestInitHelper with a
 * single-thread worker executor and BackupWorker.TestableBackupWorkerFactory; the same default stubs). The one
 * addition is a handle on that worker executor, used only by the SUCCEEDED seed of AC-3 (c) as a completion
 * barrier. WorkManager 2.9.1's test helper replaces the default task executor with a synchronous one, so an
 * enqueue and the listener on its result have both run when the scheduler call returns: no test waits on time.
 *
 * Entries are read with WorkInfo's public accessors (id, generation, periodicityInfo, initialDelayMillis,
 * constraints, tags, state). The backoff is not on WorkInfo; it is read from the internal WorkSpec by
 * reflection, as BackupTriggerWorkerTest does (Room is not on the unit-test compile classpath).
 *
 * Seeded entries are made here, directly through WorkManager, so that no test depends on the scheduler code it
 * judges: (S1) today's boot request, a one-time BackupWorker under REGULAR with REPLACE; (S2) the 900 s periodic
 * that the old scheduleRegular() registered for "Never".
 *
 * Tests marked "Guard" pin behaviour that does not change; they pass before the fix by design and are not gate
 * evidence for the change.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class WorkManagerSchedulerRegularBootTest {

    private lateinit var context: Context
    private lateinit var scheduler: WorkManagerScheduler
    private lateinit var workerExecutor: ExecutorService

    @Mock private lateinit var preferences: Preferences
    @Mock private lateinit var dataTypePreferences: DataTypePreferences
    @Mock private lateinit var secretStore: SecretStore

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = RuntimeEnvironment.application

        workerExecutor = Executors.newSingleThreadExecutor()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(workerExecutor)
            .setWorkerFactory(BackupWorker.TestableBackupWorkerFactory())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

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

    @After
    fun tearDown() {
        workerExecutor.shutdownNow()
    }

    // -----------------------------------------------------------------------
    // AC-1, AC-2: the boot path keeps or registers the periodic regular backup (D-23 (1), (2))
    // -----------------------------------------------------------------------

    @Test
    fun boot_afterRegisteredRegular_keepsSamePeriodicEntry() {
        scheduler.scheduleRegular()
        val registered = assertOnePeriodicEnqueued("after scheduleRegular()", 3_600_000L)

        scheduler.scheduleBootup()

        val entry = assertOnePeriodicEnqueued("after scheduleBootup()", 3_600_000L)
        assertWithMessage("id (an in-place UPDATE keeps it)").that(entry.id).isEqualTo(registered.id)
        assertWithMessage("generation (an in-place UPDATE raises it by exactly one)")
            .that(entry.generation).isEqualTo(registered.generation + 1)
    }

    @Test
    fun boot_nothingRegistered_registersPeriodic_wifiOnly() = assertBootRegistersNew(true, NetworkType.UNMETERED)

    @Test
    fun boot_nothingRegistered_registersPeriodic_anyNetwork() = assertBootRegistersNew(false, NetworkType.CONNECTED)

    // -----------------------------------------------------------------------
    // AC-3: a one-time entry left under REGULAR does not block the registration (D-23 (3))
    // -----------------------------------------------------------------------

    @Test
    fun regular_overOneTimeSeed_enqueued_registersPeriodic() =
        assertRegistersOverSeed(WorkInfo.State.ENQUEUED) { scheduler.scheduleRegular() }

    @Test
    fun regular_overOneTimeSeed_cancelled_registersPeriodic() =
        assertRegistersOverSeed(WorkInfo.State.CANCELLED) { scheduler.scheduleRegular() }

    @Test
    fun regular_overOneTimeSeed_succeeded_registersPeriodic() =
        assertRegistersOverSeed(WorkInfo.State.SUCCEEDED) { scheduler.scheduleRegular() }

    @Test
    fun boot_overOneTimeSeed_enqueued_registersPeriodic() =
        assertRegistersOverSeed(WorkInfo.State.ENQUEUED) { scheduler.scheduleBootup() }

    @Test
    fun boot_overOneTimeSeed_cancelled_registersPeriodic() =
        assertRegistersOverSeed(WorkInfo.State.CANCELLED) { scheduler.scheduleBootup() }

    @Test
    fun boot_overOneTimeSeed_succeeded_registersPeriodic() =
        assertRegistersOverSeed(WorkInfo.State.SUCCEEDED) { scheduler.scheduleBootup() }

    // -----------------------------------------------------------------------
    // AC-4: one non-terminal REGULAR-tagged item, periodic, under REGULAR (D-23 (4))
    // -----------------------------------------------------------------------

    @Test
    fun inv1_regularAndBoot_sequence1_oneNonTerminalPeriodic() {
        scheduler.scheduleRegular()
        scheduler.scheduleBootup()
        assertOneNonTerminalRegularWork()
    }

    @Test
    fun inv1_regularAndBoot_sequence2_oneNonTerminalPeriodic() {
        scheduler.scheduleBootup()
        scheduler.scheduleRegular()
        assertOneNonTerminalRegularWork()
    }

    @Test
    fun inv1_regularAndBoot_sequence3_oneNonTerminalPeriodic() {
        scheduler.scheduleRegular()
        scheduler.scheduleBootup()
        scheduler.cancelAll()
        scheduler.scheduleRegular()
        assertOneNonTerminalRegularWork()
    }

    // -----------------------------------------------------------------------
    // AC-6: the fallback re-reads the gates before it enqueues (BUG-027 Suggested Approach, "Risks")
    // Each gate is open at the first read (scheduleRegular) and closed at the second (the fallback).
    // -----------------------------------------------------------------------

    @Test
    fun fallback_autoBackupOffBeforeFallback_enqueuesNothing() =
        assertFallbackSkipped("autoBackup=false") { `when`(preferences.isAutoBackupEnabled).thenReturn(true, false) }

    @Test
    fun fallback_degradedBeforeFallback_enqueuesNothing() =
        assertFallbackSkipped("degraded=true") { `when`(secretStore.isEncryptionDegraded()).thenReturn(false, true) }

    @Test
    fun fallback_neverBeforeFallback_enqueuesNothing() =
        assertFallbackSkipped("regularTimeoutSecs=-1") { `when`(preferences.regularTimeoutSecs).thenReturn(3600, -1) }

    // -----------------------------------------------------------------------
    // AC-8, AC-9, AC-10: the regular schedule "Never" (D-24 (1), (2), (3))
    // -----------------------------------------------------------------------

    @Test
    fun never_minusOne_registersNothing() = assertNever(-1, false, NEVER_REGULAR) { scheduler.scheduleRegular() }

    @Test
    fun never_zero_registersNothing() = assertNever(0, false, NEVER_REGULAR) { scheduler.scheduleRegular() }

    @Test
    fun never_afterRegistered_cancelsRegular() = assertNever(-1, true, NEVER_REGULAR) { scheduler.scheduleRegular() }

    @Test
    fun boot_never_minusOne_registersNothing() = assertNever(-1, false, NEVER_BOOT) { scheduler.scheduleBootup() }

    @Test
    fun boot_never_zero_registersNothing() = assertNever(0, false, NEVER_BOOT) { scheduler.scheduleBootup() }

    @Test
    fun boot_never_afterRegistered_cancelsRegular() = assertNever(-1, true, NEVER_BOOT) { scheduler.scheduleBootup() }

    @Test
    fun upgrade_neverSeed_boot_cancels() = assertNeverSeedCancelled { scheduler.scheduleBootup() }

    @Test
    fun upgrade_neverSeed_regular_cancels() = assertNeverSeedCancelled { scheduler.scheduleRegular() }

    // -----------------------------------------------------------------------
    // AC-11, AC-12: guards of unchanged behaviour (D-24 "Guards"; BUG-026 AC-3 to AC-5)
    // -----------------------------------------------------------------------

    /**
     * Guard. It pins the behaviour, not the mechanism: WorkManager itself raises a periodic interval below
     * 15 minutes to 15 minutes (BUG-026 Description item 5), so this still passes if the app's maxOf is removed.
     */
    @Test
    fun regular_belowMinimum_raisedTo900s() {
        for (value in listOf(60, 899, 900)) {
            `when`(preferences.regularTimeoutSecs).thenReturn(value)
            scheduler.scheduleRegular()
            assertOnePeriodicEnqueued("regularTimeoutSecs=$value", 900_000L)
        }
    }

    /** Guard: each offered value of auto_backup_schedule (arrays.xml) is used as given. */
    @Test
    fun regular_offeredValues_useOwnInterval() {
        for (value in listOf(1800, 3600, 7200, 21600, 86400)) {
            `when`(preferences.regularTimeoutSecs).thenReturn(value)
            scheduler.scheduleRegular()
            assertOnePeriodicEnqueued("regularTimeoutSecs=$value", value * 1000L)
        }
    }

    /** Guard: "Never" does not touch the incoming path (BUG-026 AC-5). */
    @Test
    fun never_incomingPathUnchanged() {
        `when`(preferences.regularTimeoutSecs).thenReturn(-1)
        `when`(preferences.incomingTimeoutSecs).thenReturn(60)

        assertWithMessage("scheduleContentTrigger()").that(scheduler.scheduleContentTrigger()).isNotNull()
        val trigger = contentTriggerEntries()
        assertWithMessage("CONTENT_TRIGGER entries").that(trigger).hasSize(1)
        assertWithMessage("CONTENT_TRIGGER state").that(trigger[0].state).isEqualTo(WorkInfo.State.ENQUEUED)

        assertWithMessage("scheduleIncoming()").that(scheduler.scheduleIncoming()).isNotNull()
        val incoming = workManager().getWorkInfosForUniqueWork("INCOMING").get()
        assertWithMessage("INCOMING entries").that(incoming).hasSize(1)
        assertWithMessage("INCOMING initialDelayMillis").that(incoming[0].initialDelayMillis).isEqualTo(60_000L)
    }

    // -----------------------------------------------------------------------
    // AC-13: guards of the gates and the content trigger (BUG-027 AC-6, AC-7; Rules 4 and 4a)
    // -----------------------------------------------------------------------

    /** Guard: the degraded check comes first and changes nothing. */
    @Test
    fun boot_degraded_afterRegistered_leavesItUnchanged() {
        scheduler.scheduleRegular()
        val before = assertOnePeriodicEnqueued("after scheduleRegular()", 3_600_000L)
        `when`(secretStore.isEncryptionDegraded()).thenReturn(true)

        assertWithMessage("scheduleBootup() while degraded").that(scheduler.scheduleBootup()).isNull()

        assertSameEntry(before)
    }

    /** Guard: the degraded check runs before the "Never" rule, so nothing is cancelled. */
    @Test
    fun boot_degraded_never_cancelsNothing() {
        scheduler.scheduleRegular()
        val before = assertOnePeriodicEnqueued("after scheduleRegular()", 3_600_000L)
        `when`(preferences.regularTimeoutSecs).thenReturn(-1)
        `when`(secretStore.isEncryptionDegraded()).thenReturn(true)

        assertWithMessage("scheduleBootup() while degraded, \"Never\"").that(scheduler.scheduleBootup()).isNull()

        assertSameEntry(before)
    }

    /** Guard: the boot path does not touch CONTENT_TRIGGER. */
    @Test
    fun boot_leavesContentTriggerUnchanged() {
        scheduler.scheduleContentTrigger()
        val before = contentTriggerEntries()
        assertWithMessage("CONTENT_TRIGGER entries before").that(before).hasSize(1)

        scheduler.scheduleBootup()

        val after = contentTriggerEntries()
        assertWithMessage("CONTENT_TRIGGER entries after").that(after).hasSize(1)
        assertWithMessage("CONTENT_TRIGGER id").that(after[0].id).isEqualTo(before[0].id)
        assertWithMessage("CONTENT_TRIGGER state").that(after[0].state).isEqualTo(before[0].state)
    }

    /** Guard: auto backup off at boot cancels all (Rule 4, first sentence). */
    @Test
    fun boot_autoBackupOff_cancelsAll() {
        scheduler.scheduleRegular()
        scheduler.scheduleContentTrigger()
        `when`(preferences.isAutoBackupEnabled).thenReturn(false)

        assertWithMessage("scheduleBootup() with auto backup off").that(scheduler.scheduleBootup()).isNull()

        assertWithMessage("non-terminal entries under REGULAR").that(nonTerminalRegularEntries()).isEmpty()
        assertWithMessage("non-terminal entries under CONTENT_TRIGGER")
            .that(contentTriggerEntries().filter { isNonTerminal(it) }).isEmpty()
    }

    // -----------------------------------------------------------------------
    // Shared assertions
    // -----------------------------------------------------------------------

    private fun assertBootRegistersNew(wifiOnly: Boolean, network: NetworkType) {
        `when`(preferences.isWifiOnly).thenReturn(wifiOnly)
        assertWithMessage("precondition: nothing under REGULAR").that(regularEntries()).isEmpty()
        val mark = logMark()

        val job = scheduler.scheduleBootup()

        val entry = assertOnePeriodicEnqueued("after scheduleBootup()", 3_600_000L)
        assertWithMessage("returned job").that(job).isNotNull()
        assertWithMessage("initialDelayMillis (the boot delay of a new registration)")
            .that(entry.initialDelayMillis).isEqualTo(60_000L)
        assertWithMessage("tags").that(entry.tags).contains(REGULAR)
        assertWithMessage("requiredNetworkType (INV-2)").that(entry.constraints.requiredNetworkType).isEqualTo(network)
        assertWithMessage("requiresCharging (INV-2)").that(entry.constraints.requiresCharging()).isFalse()
        val spec = workSpecOf(entry.id)
        assertWithMessage("WorkSpec of the entry").that(spec).isNotNull()
        val specClass = Class.forName("androidx.work.impl.model.WorkSpec")
        assertWithMessage("backoff policy (INV-3)").that(specClass.getField("backoffPolicy").get(spec))
            .isEqualTo(BackoffPolicy.EXPONENTIAL)
        assertWithMessage("backoff delay ms (INV-3)").that(specClass.getField("backoffDelayDuration").getLong(spec))
            .isEqualTo(WorkManagerScheduler.BACKOFF_INITIAL_SECS * 1000L)
        val logs = logsSince(mark)
        assertWithMessage("boot lines").that(lines(logs, Log.DEBUG, BOOT_LINE)).hasSize(1)
        assertWithMessage("fallback warnings").that(fallbackWarnings(logs)).isEmpty()
    }

    private fun assertRegistersOverSeed(state: WorkInfo.State, call: () -> Any?) {
        val seed = seedOneTimeIn(state)
        val mark = logMark()

        call()

        val entry = assertOnePeriodicEnqueued("after the call", 3_600_000L)
        assertWithMessage("the seed is gone (a new id)").that(entry.id).isNotEqualTo(seed)
        assertWithMessage("one-time entries left under REGULAR")
            .that(regularEntries().filter { it.periodicityInfo == null }).isEmpty()
        assertWithMessage("fallback warnings").that(fallbackWarnings(logsSince(mark))).hasSize(1)
    }

    private fun assertOneNonTerminalRegularWork() {
        val tagged = workManager().getWorkInfosByTag(REGULAR).get()
        val active = tagged.filter { isNonTerminal(it) }
        assertWithMessage("non-terminal REGULAR-tagged items").that(active).hasSize(1)
        assertWithMessage("that item is periodic").that(active[0].periodicityInfo).isNotNull()
        val underName = nonTerminalRegularEntries()
        assertWithMessage("non-terminal entries under REGULAR").that(underName).hasSize(1)
        assertWithMessage("the tagged item is the entry under REGULAR").that(active[0].id).isEqualTo(underName[0].id)
        assertWithMessage("every REGULAR-tagged id is an entry of REGULAR (no other unique name holds one)")
            .that(regularEntries().map { it.id }).containsAtLeastElementsIn(tagged.map { it.id })
    }

    private fun assertFallbackSkipped(closedGate: String, stubGateChange: () -> Unit) {
        scheduler.scheduleContentTrigger()
        val trigger = contentTriggerEntries()
        assertWithMessage("precondition: CONTENT_TRIGGER registered").that(trigger).hasSize(1)
        val seed = seedOneTimeIn(WorkInfo.State.ENQUEUED)
        stubGateChange()
        val mark = logMark()

        scheduler.scheduleRegular()

        val entries = regularEntries()
        assertWithMessage("entries under REGULAR").that(entries).hasSize(1)
        assertWithMessage("periodic entries under REGULAR").that(entries.filter { it.periodicityInfo != null }).isEmpty()
        assertWithMessage("the seed is left as it was (id)").that(entries[0].id).isEqualTo(seed)
        assertWithMessage("the seed is left as it was (state)").that(entries[0].state).isEqualTo(WorkInfo.State.ENQUEUED)
        val after = contentTriggerEntries()
        assertWithMessage("CONTENT_TRIGGER entries").that(after).hasSize(1)
        assertWithMessage("CONTENT_TRIGGER id").that(after[0].id).isEqualTo(trigger[0].id)
        assertWithMessage("CONTENT_TRIGGER state").that(after[0].state).isEqualTo(trigger[0].state)
        val logs = logsSince(mark)
        assertWithMessage("fallback warnings (re-registering)").that(fallbackWarnings(logs)).isEmpty()
        val skipped = lines(logs, Log.WARN, SKIP_LINE).filter { (it.msg ?: "").contains("not re-registered") }
        assertWithMessage("the fallback ran and did not re-register (one line)").that(skipped).hasSize(1)
        assertWithMessage("the closed gate named in that line").that(skipped[0].msg).contains(closedGate)
    }

    private fun assertNever(value: Int, registerFirst: Boolean, neverLine: String, call: () -> ScheduledJob?) {
        if (registerFirst) {
            scheduler.scheduleRegular()
            assertOnePeriodicEnqueued("after scheduleRegular() with 3600", 3_600_000L)
        }
        `when`(preferences.regularTimeoutSecs).thenReturn(value)
        val mark = logMark()

        val job = call()

        assertWithMessage("returned job (regularTimeoutSecs=$value)").that(job).isNull()
        assertWithMessage("non-terminal entries under REGULAR").that(nonTerminalRegularEntries()).isEmpty()
        assertWithMessage("non-terminal REGULAR-tagged items under any name")
            .that(workManager().getWorkInfosByTag(REGULAR).get().filter { isNonTerminal(it) }).isEmpty()
        assertWithMessage("\"Never\" lines starting \"$neverLine\"").that(lines(logsSince(mark), Log.DEBUG, neverLine))
            .hasSize(1)
    }

    private fun assertNeverSeedCancelled(call: () -> ScheduledJob?) {
        val request = PeriodicWorkRequest.Builder(BackupWorker::class.java, 900L, TimeUnit.SECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .addTag(REGULAR)
            .build()
        workManager().enqueueUniquePeriodicWork(REGULAR, ExistingPeriodicWorkPolicy.UPDATE, request).result.get()
        assertOnePeriodicEnqueued("precondition: the \"Never\" seed (S2)", 900_000L)
        `when`(preferences.regularTimeoutSecs).thenReturn(-1)

        val job = call()

        assertWithMessage("returned job").that(job).isNull()
        assertWithMessage("non-terminal entries under REGULAR").that(nonTerminalRegularEntries()).isEmpty()
    }

    private fun assertOnePeriodicEnqueued(label: String, intervalMillis: Long): WorkInfo {
        val entries = regularEntries()
        assertWithMessage("$label: entries under REGULAR").that(entries).hasSize(1)
        val entry = entries[0]
        assertWithMessage("$label: the entry is periodic").that(entry.periodicityInfo).isNotNull()
        assertWithMessage("$label: state").that(entry.state).isEqualTo(WorkInfo.State.ENQUEUED)
        assertWithMessage("$label: repeatIntervalMillis").that(entry.periodicityInfo!!.repeatIntervalMillis)
            .isEqualTo(intervalMillis)
        return entry
    }

    private fun assertSameEntry(before: WorkInfo) {
        val entries = regularEntries()
        assertWithMessage("entries under REGULAR").that(entries).hasSize(1)
        assertWithMessage("id").that(entries[0].id).isEqualTo(before.id)
        assertWithMessage("state").that(entries[0].state).isEqualTo(before.state)
        assertWithMessage("generation").that(entries[0].generation).isEqualTo(before.generation)
    }

    // -----------------------------------------------------------------------
    // Seeds, reads and logs
    // -----------------------------------------------------------------------

    /**
     * (S1) today's boot request (WorkManagerScheduler.kt:273-281 before the fix), enqueued directly: a one-time
     * worker with the regular network constraint, EXPONENTIAL 30 s, a 60 s initial delay and tag REGULAR, under
     * REGULAR with REPLACE. ENQUEUED as enqueued; CANCELLED after cancelAll(), as App.rescheduleJobs() does;
     * SUCCEEDED by running it (P-3). The SUCCEEDED seed uses SucceedingSeedWorker in place of BackupWorker, a
     * coroutine worker that this harness cannot drive to SUCCEEDED; WorkManager's UPDATE checks only the type
     * (one-time or periodic) and the state of the entry, never its worker class.
     */
    private fun seedOneTimeIn(state: WorkInfo.State): UUID {
        val workerClass: Class<out ListenableWorker> =
            if (state == WorkInfo.State.SUCCEEDED) SucceedingSeedWorker::class.java else BackupWorker::class.java
        val request = OneTimeWorkRequest.Builder(workerClass)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .setInitialDelay(60L, TimeUnit.SECONDS)
            .addTag(REGULAR)
            .build()
        workManager().enqueueUniqueWork(REGULAR, ExistingWorkPolicy.REPLACE, request).result.get()
        when (state) {
            WorkInfo.State.ENQUEUED -> Unit
            WorkInfo.State.CANCELLED -> scheduler.cancelAll()
            WorkInfo.State.SUCCEEDED -> {
                val driver = WorkManagerTestInitHelper.getTestDriver(context)
                assertWithMessage("precondition: the test driver").that(driver).isNotNull()
                driver!!.setInitialDelayMet(request.id)
                driver.setAllConstraintsMet(request.id)
                // Completion barrier: the seed's doWork() was handed to the single worker thread before this task.
                workerExecutor.submit(Runnable {}).get()
            }
            else -> throw IllegalArgumentException("not a seed state: $state")
        }
        val seeded = regularEntries()
        assertWithMessage("precondition: one seeded entry under REGULAR").that(seeded).hasSize(1)
        assertWithMessage("precondition: the seed is one-time").that(seeded[0].periodicityInfo).isNull()
        assertWithMessage("precondition: the seed's state").that(seeded[0].state).isEqualTo(state)
        return request.id
    }

    private fun workManager(): WorkManager = WorkManager.getInstance(context)

    private fun regularEntries(): List<WorkInfo> = workManager().getWorkInfosForUniqueWork(REGULAR).get()

    private fun contentTriggerEntries(): List<WorkInfo> =
        workManager().getWorkInfosForUniqueWork(WorkManagerScheduler.CONTENT_TRIGGER_UNIQUE_NAME).get()

    private fun isNonTerminal(info: WorkInfo): Boolean =
        info.state == WorkInfo.State.ENQUEUED || info.state == WorkInfo.State.RUNNING ||
            info.state == WorkInfo.State.BLOCKED

    private fun nonTerminalRegularEntries(): List<WorkInfo> = regularEntries().filter { isNonTerminal(it) }

    /** WorkManagerImpl.getWorkDatabase().workSpecDao().getWorkSpec(id), by reflection (BackupTriggerWorkerTest's form). */
    private fun workSpecOf(id: UUID): Any? {
        val database = Class.forName("androidx.work.impl.WorkManagerImpl").getMethod("getWorkDatabase").invoke(workManager())
        val dao = Class.forName("androidx.work.impl.WorkDatabase").getMethod("workSpecDao").invoke(database)
        return Class.forName("androidx.work.impl.model.WorkSpecDao").getMethod("getWorkSpec", String::class.java)
            .invoke(dao, id.toString())
    }

    private fun logMark(): Int = ShadowLog.getLogsForTag(TAG).size

    private fun logsSince(mark: Int): List<ShadowLog.LogItem> = ShadowLog.getLogsForTag(TAG).drop(mark)

    private fun lines(logs: List<ShadowLog.LogItem>, level: Int, prefix: String): List<ShadowLog.LogItem> =
        logs.filter { it.type == level && (it.msg ?: "").startsWith(prefix) }

    /** The fallback warning (Terms): at Log.w, containing "UPDATE failed" and "CANCEL_AND_REENQUEUE". */
    private fun fallbackWarnings(logs: List<ShadowLog.LogItem>): List<ShadowLog.LogItem> = logs.filter {
        it.type == Log.WARN && (it.msg ?: "").contains("UPDATE failed") && (it.msg ?: "").contains("CANCEL_AND_REENQUEUE")
    }

    private companion object {
        const val TAG = "SMSBackup+"
        /** The persisted unique name and tag (IC-2), written out so that a rename fails here. */
        const val REGULAR = "REGULAR"
        const val NEVER_REGULAR = "WorkManagerScheduler.scheduleRegular: skipped (regular schedule is Never"
        const val NEVER_BOOT = "WorkManagerScheduler.scheduleBootup: skipped (regular schedule is Never"
        const val BOOT_LINE = "WorkManagerScheduler.scheduleBootup: registered REGULAR"
        const val SKIP_LINE = "WorkManagerScheduler.scheduleRegular: UPDATE failed on REGULAR"
    }
}

/**
 * The SUCCEEDED seed's worker (AC-3 (c), P-3). TestableBackupWorkerFactory returns null for it, so WorkManager
 * builds it by reflection; doWork() runs on the harness's single worker thread and succeeds.
 */
class SucceedingSeedWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result = Result.success()
}
```

Row D12 (P-3 not reached): the two methods `regular_overOneTimeSeed_succeeded_registersPeriodic` and
`boot_overOneTimeSeed_succeeded_registersPeriodic` are deleted, each with its `@Test` line and the blank line after it
(two Edits, `old_string` = the four lines from `    @Test` through the blank line). The file then has 28 `@Test` and
blob `176d4dc8f75e1532f464aec8f74e979403c6f5ca` (derived from the plan text, 14.1). `SucceedingSeedWorker` and the `SUCCEEDED` branch of
`seedOneTimeIn` stay (unused; no warning is an error, L20).

### 3.3 The throwaway probe (I1 step 1; deleted at I3 step 0)

**Embedded file E2** — `app/src/test/java/com/zegoggles/smssync/service/U076LibraryProbeTest.kt` (blob `d14411868143829af0c4c51b506fbdf6f65be290`,
208 lines, 5 `@Test`, LF):

```kotlin
package com.zegoggles.smssync.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * U-076 throwaway probe (plan 2.3; P-1 to P-3). Runs once at I2 in the harness of WorkManagerSchedulerTest,
 * calls WorkManager 2.9.1 directly (never the scheduler), and is deleted before the fix. Not counted, never
 * committed. Each assertion message names the stop condition it guards.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class U076LibraryProbeTest {

    private lateinit var context: Context
    private lateinit var workerExecutor: ExecutorService

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application
        workerExecutor = Executors.newSingleThreadExecutor()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(workerExecutor)
            .setWorkerFactory(BackupWorker.TestableBackupWorkerFactory())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @After
    fun tearDown() {
        workerExecutor.shutdownNow()
    }

    /** P-1 (a): UPDATE over a never-run periodic entry keeps id, lastEnqueueTime and periodCount; generation + 1. */
    @Test
    fun p1a_updateOverNeverRunPeriodic_keepsTheRegistration() {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, periodic(0L)).result.get()
        val first = entries().single()
        val spec0 = spec(first.id)
        val enqueue0 = specLong(spec0, "lastEnqueueTime")
        val periods0 = specPeriodCount(spec0)

        wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, periodic(60L)).result.get()
        val boot = entries().single()
        val spec1 = spec(boot.id)
        println("P-1a first: id=${first.id} gen=${first.generation} lastEnqueue=$enqueue0 periodCount=$periods0 next=${first.nextScheduleTimeMillis} delay=${first.initialDelayMillis}")
        println("P-1a boot:  id=${boot.id} gen=${boot.generation} lastEnqueue=${specLong(spec1, "lastEnqueueTime")} periodCount=${specPeriodCount(spec1)} next=${boot.nextScheduleTimeMillis} delay=${boot.initialDelayMillis}")
        assertWithMessage("STOP P-1 (a): UPDATE changed the id").that(boot.id).isEqualTo(first.id)
        assertWithMessage("P-1 (a): generation + 1 (AC-1)").that(boot.generation).isEqualTo(first.generation + 1)
        assertWithMessage("STOP P-1 (a): UPDATE reset lastEnqueueTime").that(specLong(spec1, "lastEnqueueTime")).isEqualTo(enqueue0)
        assertWithMessage("STOP P-1 (a): UPDATE reset periodCount").that(specPeriodCount(spec1)).isEqualTo(periods0)
        assertWithMessage("H-2 (recorded, not a stop): the new initial delay is applied").that(boot.initialDelayMillis).isEqualTo(60_000L)
        assertWithMessage("H-2 (recorded, not a stop): first run = lastEnqueueTime + 60 s").that(boot.nextScheduleTimeMillis).isEqualTo(enqueue0 + 60_000L)

        wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, periodic(0L)).result.get()
        val again = entries().single()
        println("P-1a again: id=${again.id} gen=${again.generation} next=${again.nextScheduleTimeMillis} delay=${again.initialDelayMillis}")
        assertWithMessage("H-2 (recorded, not a stop): a later UPDATE without delay drops it").that(again.nextScheduleTimeMillis).isEqualTo(enqueue0)
    }

    /** P-1 (b) and P-2, ENQUEUED seed. */
    @Test
    fun p1b_updateOverOneTimeEnqueued_failsOnTheOperationOnly_thenCancelAndReenqueueReplacesIt() =
        assertUpdateFailsThenReplaced(seedOneTime())

    /** P-1 (b) and P-2, CANCELLED seed. */
    @Test
    fun p1b_updateOverOneTimeCancelled_failsOnTheOperationOnly_thenCancelAndReenqueueReplacesIt() {
        val seed = seedOneTime()
        WorkManager.getInstance(context).cancelUniqueWork(NAME).result.get()
        assertWithMessage("precondition: CANCELLED").that(entries().single().state).isEqualTo(WorkInfo.State.CANCELLED)
        assertUpdateFailsThenReplaced(seed)
    }

    /** P-3 and P-1 (b), SUCCEEDED seed (a plain Worker; BackupWorker is a coroutine worker). */
    @Test
    fun p3_oneTimeSeedReachesSucceeded_thenUpdateFailsAndCancelAndReenqueueReplacesIt() {
        val request = oneTime(SucceedingSeedWorker::class.java)
        WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request).result.get()
        val driver = WorkManagerTestInitHelper.getTestDriver(context)!!
        driver.setInitialDelayMet(request.id)
        driver.setAllConstraintsMet(request.id)
        workerExecutor.submit(Runnable {}).get()
        val state = entries().single().state
        println("P-3 seed state after the barrier: $state")
        assertWithMessage("P-3 (not a stop; AC-3 (c) is then recorded not tested): SUCCEEDED").that(state)
            .isEqualTo(WorkInfo.State.SUCCEEDED)
        assertUpdateFailsThenReplaced(request.id)
    }

    /** P-1 (c): UPDATE over a cancelled periodic deletes it and enqueues the new request; over nothing, registers. */
    @Test
    fun p1c_updateOverCancelledPeriodicOrNothing_registersNew() {
        val wm = WorkManager.getInstance(context)
        val absent = periodic(0L)
        wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, absent).result.get()
        assertWithMessage("STOP P-1 (c): nothing registered over an absent name").that(entries().single().id).isEqualTo(absent.id)
        wm.cancelUniqueWork(NAME).result.get()
        val next = periodic(0L)
        wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, next).result.get()
        val after = entries()
        println("P-1c after cancel + UPDATE: ${after.map { "${it.id} ${it.state} periodic=${it.periodicityInfo != null}" }}")
        assertWithMessage("STOP P-1 (c): entries after UPDATE over a cancelled periodic").that(after).hasSize(1)
        assertWithMessage("STOP P-1 (c): the new request is enqueued").that(after[0].id).isEqualTo(next.id)
        assertWithMessage("STOP P-1 (c): state").that(after[0].state).isEqualTo(WorkInfo.State.ENQUEUED)
        assertWithMessage("STOP P-1 (c): the cancelled entry is deleted").that(wm.getWorkInfoById(absent.id).get()).isNull()
    }

    private fun assertUpdateFailsThenReplaced(seed: UUID) {
        val wm = WorkManager.getInstance(context)
        val operation = try {
            wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, periodic(0L))
        } catch (e: Exception) {
            throw AssertionError("STOP P-1 (b): enqueueUniquePeriodicWork threw instead of failing the Operation", e)
        }
        val result = operation.result
        assertWithMessage("STOP P-2: the result is not complete when the call returns (the harness)").that(result.isDone).isTrue()
        var listenerRan = false
        result.addListener({ listenerRan = true }, Executor { it.run() })
        assertWithMessage("STOP P-2: a direct-executor listener did not run before addListener returned").that(listenerRan).isTrue()
        val cause = try {
            result.get()
            null
        } catch (e: ExecutionException) {
            e.cause
        }
        println("P-1b cause: ${cause?.javaClass?.name}: ${cause?.message}")
        assertWithMessage("STOP P-1 (b): UPDATE did not fail on a one-time entry").that(cause).isNotNull()
        assertWithMessage("P-1 (b): the cause").that(cause).isInstanceOf(UnsupportedOperationException::class.java)
        assertWithMessage("P-1 (b): the one-time entry is untouched by the failed UPDATE").that(entries().single().id).isEqualTo(seed)
        val periodicRequest = periodic(0L)
        wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, periodicRequest).result.get()
        val after = entries()
        assertWithMessage("STOP P-1 (b): entries after CANCEL_AND_REENQUEUE").that(after).hasSize(1)
        assertWithMessage("STOP P-1 (b): the periodic request replaced the seed").that(after[0].id).isEqualTo(periodicRequest.id)
        assertWithMessage("STOP P-1 (b): periodic").that(after[0].periodicityInfo).isNotNull()
        assertWithMessage("STOP P-1 (b): ENQUEUED").that(after[0].state).isEqualTo(WorkInfo.State.ENQUEUED)
    }

    private fun entries(): List<WorkInfo> = WorkManager.getInstance(context).getWorkInfosForUniqueWork(NAME).get()

    private fun network(): Constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    private fun periodic(delaySecs: Long): PeriodicWorkRequest {
        val builder = PeriodicWorkRequest.Builder(BackupWorker::class.java, 3600L, TimeUnit.SECONDS)
            .setConstraints(network())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .addTag(NAME)
        if (delaySecs > 0) builder.setInitialDelay(delaySecs, TimeUnit.SECONDS)
        return builder.build()
    }

    private fun oneTime(worker: Class<out androidx.work.ListenableWorker>): OneTimeWorkRequest =
        OneTimeWorkRequest.Builder(worker)
            .setConstraints(network())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .setInitialDelay(60L, TimeUnit.SECONDS)
            .addTag(NAME)
            .build()

    private fun seedOneTime(): UUID {
        val request = oneTime(BackupWorker::class.java)
        WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request).result.get()
        return request.id
    }

    private fun spec(id: UUID): Any {
        val database = Class.forName("androidx.work.impl.WorkManagerImpl").getMethod("getWorkDatabase")
            .invoke(WorkManager.getInstance(context))
        val dao = Class.forName("androidx.work.impl.WorkDatabase").getMethod("workSpecDao").invoke(database)
        return Class.forName("androidx.work.impl.model.WorkSpecDao").getMethod("getWorkSpec", String::class.java)
            .invoke(dao, id.toString())!!
    }

    private fun specLong(spec: Any, field: String): Long = spec.javaClass.getField(field).getLong(spec)

    private fun specPeriodCount(spec: Any): Int = spec.javaClass.getMethod("getPeriodCount").invoke(spec) as Int

    private companion object {
        const val NAME = "REGULAR"
    }
}
```

### 3.4 Tools (I0 step 1, Write tool, into `$TL` = `sdlc/artifacts/build/sprints/sprint-025/U-076/evidence/tools/`)

All are Python 3 standard library only, run as `python -I -X utf8 <tool> …`, and print no message content, address or
path of this machine. E3 contains six em dashes (`—`, U+2014) inside four Kotlin and two Java old/new texts: write them as
the character, exactly as printed (the scheduler's own log texts contain it, `:172`, `:264`).

**Embedded file E3** — `u076_edits.py` (developer: anchors, the fix, the hunk check, the injections; blob
`5f418cdd2b986d0d9054e836d7f45ba19df93a0d`, 551 lines):

```python
#!/usr/bin/env python3
"""U-076 edit and injection tool (plan sections 3.1 to 3.3 and 4.8).

Usage (from the repository root, ROOT = .):
  python -I -X utf8 u076_edits.py ROOT start      every edit's old text occurs exactly once in its file
  python -I -X utf8 u076_edits.py ROOT apply      start check, then apply every edit; each file keeps its line endings
  python -I -X utf8 u076_edits.py ROOT fixed      every new text occurs exactly once; no old text is left
  python -I -X utf8 u076_edits.py ROOT mutants    every injection's old text occurs exactly once in the fixed scheduler
  python -I -X utf8 u076_edits.py ROOT mutate ID  apply one injection (M-1 to M-8) to the fixed scheduler
  python -I -X utf8 u076_edits.py ROOT hunks DIFF  AC-15: every hunk of a `git diff -U0 <start>` file lies in an allowed
                                                 place (old line ranges at Start), and every changed line of the
                                                 port is a comment line (ROOT is not read)
Texts are compared with line endings normalised to LF. One line per check, then a RESULT line.
Exit 0 = every check passed; 1 = a mismatch (nothing written); 2 = usage, a missing file or mixed line endings.
"""
import hashlib
import os
import sys

WMS = "app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt"
BS = "app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java"

W1_OLD = """ * - **INV-1 single-flight**: [ExistingWorkPolicy.REPLACE] for one-off work;
 *   [ExistingPeriodicWorkPolicy.UPDATE] for periodic. Source: `setReplaceCurrent(true)` on
 *   every job in `BackupJobs.java:188`.
"""
W1_NEW = """ * - **INV-1 single-flight**: [ExistingWorkPolicy.REPLACE] for one-off work;
 *   [ExistingPeriodicWorkPolicy.UPDATE] for periodic work (REGULAR, CONTENT_TRIGGER). REGULAR
 *   falls back to [ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE] only when its UPDATE fails
 *   (a one-time entry left under REGULAR by an earlier build). One work kind per unique name:
 *   the boot path registers the REGULAR periodic work and never enqueues a one-off under
 *   REGULAR (CNTR-MODERNIZATION-004 v4). Source: `setReplaceCurrent(true)` on every job in
 *   `BackupJobs.java:188`.
"""

W2_OLD = """ * **Unique work names**: [BackupType.name] for regular/incoming/immediate, plus
 * [CONTENT_TRIGGER_UNIQUE_NAME] for the content-trigger periodic work. Names must be
 * stable across app restarts because WorkManager persists work by unique name.
"""
W2_NEW = """ * **Unique work names**: [BackupType.name] for regular/incoming/immediate/manual/skip, plus
 * [CONTENT_TRIGGER_UNIQUE_NAME] for the content-trigger periodic work. The boot path has no
 * unique name of its own: it registers the REGULAR periodic work (no BOOT name is used).
 * Names must be stable across app restarts because WorkManager persists work by unique name.
"""

W3_OLD = """        /**
         * Minimum interval for PeriodicWorkRequest is 15 minutes per WorkManager API.
         * This fallback is used when getRegularTimeoutSecs() returns a value below 15 minutes.
         */
"""
W3_NEW = """        /**
         * Minimum interval for PeriodicWorkRequest is 15 minutes per WorkManager API.
         * A positive getRegularTimeoutSecs() below 15 minutes is raised to it. A non-positive
         * value means "Never" and registers nothing (CNTR-MODERNIZATION-004 v4, Rule 4a).
         */
"""

W4_OLD = """    /**
     * Schedules a recurring backup.
     * INV-1: UPDATE on stable unique name; INV-2: UNMETERED or CONNECTED; INV-3: EXPONENTIAL/30s.
     * Interval = [Preferences.getRegularTimeoutSecs] seconds (minimum 15 minutes per WM API).
     * Returns null if autoBackupEnabled is false.
     *
     * U-054 (SE-002): returns null without enqueuing any work when
     * [SecretStore.isEncryptionDegraded] returns true. A degraded device must not enqueue
     * automatic backup work that would fail when credentials cannot be decrypted.
     */
    override fun scheduleRegular(): ScheduledJob? {
        if (secretStore.isEncryptionDegraded()) {
            Log.w(TAG, "WorkManagerScheduler.scheduleRegular: blocked — encryption degraded, credentials in plaintext")
            return null
        }
        if (!preferences.isAutoBackupEnabled) {
            Log.d(TAG, "WorkManagerScheduler.scheduleRegular: skipped (autoBackup disabled)")
            return null
        }

        val intervalSecs = maxOf(preferences.regularTimeoutSecs.toLong(), PERIODIC_MIN_INTERVAL_SECS)

        val request = PeriodicWorkRequest.Builder(
            BackupWorker::class.java,
            intervalSecs,
            TimeUnit.SECONDS
        )
            .setConstraints(networkConstraints(BackupType.REGULAR))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_INITIAL_SECS, TimeUnit.SECONDS)
            .addTag(BackupType.REGULAR.name)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                BackupType.REGULAR.name,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )

        Log.d(TAG, "WorkManagerScheduler.scheduleRegular: enqueued REGULAR with " +
                "interval=${intervalSecs}s, network=${constraintDescription(BackupType.REGULAR)}")
        return ScheduledJob(BackupType.REGULAR.name, "WorkManagerScheduler:REGULAR @ ${intervalSecs}s")
    }
"""
W4_NEW = """    /**
     * Registers the recurring regular backup (unique name and tag REGULAR, a periodic BackupWorker).
     * INV-1: [ExistingPeriodicWorkPolicy.UPDATE] on the stable unique name; only when that UPDATE
     * fails (a one-time entry under REGULAR left by an earlier build) is the same request
     * re-registered with [ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE].
     * INV-2: UNMETERED or CONNECTED; INV-3: EXPONENTIAL/30s.
     * Interval = [Preferences.getRegularTimeoutSecs] seconds; a positive value below 15 minutes is
     * raised to 15 minutes (the WorkManager minimum). A non-positive value ("Never" is -1; 0 too)
     * registers nothing, cancels REGULAR and returns null (CNTR-MODERNIZATION-004 v4, Rule 4a;
     * the legacy `inSeconds > 0` rule, BackupJobs.java:132).
     * Returns null if autoBackupEnabled is false (nothing is cancelled then).
     *
     * U-054 (SE-002): returns null without enqueuing any work when
     * [SecretStore.isEncryptionDegraded] returns true. A degraded device must not enqueue
     * automatic backup work that would fail when credentials cannot be decrypted.
     */
    override fun scheduleRegular(): ScheduledJob? {
        if (secretStore.isEncryptionDegraded()) {
            Log.w(TAG, "WorkManagerScheduler.scheduleRegular: blocked — encryption degraded, credentials in plaintext")
            return null
        }
        if (!preferences.isAutoBackupEnabled) {
            Log.d(TAG, "WorkManagerScheduler.scheduleRegular: skipped (autoBackup disabled)")
            return null
        }
        return registerRegular("scheduleRegular", 0L)
    }
"""

W5_OLD = """    /**
     * Schedules (or cancels) jobs on device boot.
     * If !isAutoBackupEnabled: cancels all; returns null.
     * Otherwise: schedules regular backup with BOOT_BACKUP_DELAY (60s) initial delay.
     * Source: BackupJobs.java:86-97; BOOT_BACKUP_DELAY = 60 (line 56).
     *
     * U-054 (SE-002): returns null without enqueuing any work when
     * [SecretStore.isEncryptionDegraded] returns true. A degraded device must not enqueue
     * automatic backup work that would fail when credentials cannot be decrypted.
     */
    override fun scheduleBootup(): ScheduledJob? {
        if (secretStore.isEncryptionDegraded()) {
            Log.w(TAG, "WorkManagerScheduler.scheduleBootup: blocked — encryption degraded, credentials in plaintext")
            return null
        }
        if (!preferences.isAutoBackupEnabled) {
            Log.d(TAG, "WorkManagerScheduler.scheduleBootup: autoBackup disabled, cancelling all")
            cancelAll()
            return null
        }

        val request = OneTimeWorkRequest.Builder(BackupWorker::class.java)
            .setConstraints(networkConstraints(BackupType.REGULAR))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_INITIAL_SECS, TimeUnit.SECONDS)
            .setInitialDelay(BOOT_BACKUP_DELAY_SECS, TimeUnit.SECONDS)
            .addTag(BackupType.REGULAR.name)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(BackupType.REGULAR.name, ExistingWorkPolicy.REPLACE, request)

        Log.d(TAG, "WorkManagerScheduler.scheduleBootup: enqueued bootup REGULAR with " +
                "${BOOT_BACKUP_DELAY_SECS}s delay")
        return ScheduledJob(BackupType.REGULAR.name, "WorkManagerScheduler:bootup @ ${BOOT_BACKUP_DELAY_SECS}s")
    }
"""
W5_NEW = """    /**
     * Keeps the regular backup registered on device boot (CNTR-MODERNIZATION-004 v4, Rule 4).
     * If !isAutoBackupEnabled: cancels all; returns null.
     * Otherwise registers the regular periodic backup exactly as [scheduleRegular] does (the same
     * REGULAR request, UPDATE, the CANCEL_AND_REENQUEUE fallback, the same "Never" rule), with
     * BOOT_BACKUP_DELAY (60s) as the first-run delay of a new registration. UPDATE keeps an
     * existing registration (its id; its schedule once it has run) and does not cancel a running
     * regular backup. It never enqueues a one-time REGULAR work and starts no separate boot
     * backup. "Never" (a non-positive interval) registers nothing, cancels REGULAR and returns
     * null.
     * Source: BackupJobs.java:86-97, both branches: the old-scheduler branch restarted the
     * recurring regular job 60s after boot, the default branch kept the persisted job;
     * BOOT_BACKUP_DELAY = 60 (line 56).
     *
     * U-054 (SE-002): returns null without enqueuing any work when
     * [SecretStore.isEncryptionDegraded] returns true. A degraded device must not enqueue
     * automatic backup work that would fail when credentials cannot be decrypted.
     */
    override fun scheduleBootup(): ScheduledJob? {
        if (secretStore.isEncryptionDegraded()) {
            Log.w(TAG, "WorkManagerScheduler.scheduleBootup: blocked — encryption degraded, credentials in plaintext")
            return null
        }
        if (!preferences.isAutoBackupEnabled) {
            Log.d(TAG, "WorkManagerScheduler.scheduleBootup: autoBackup disabled, cancelling all")
            cancelAll()
            return null
        }
        return registerRegular("scheduleBootup", BOOT_BACKUP_DELAY_SECS)
    }

    /**
     * The one registration path of the regular backup, shared by [scheduleRegular] (no delay) and
     * [scheduleBootup] ([BOOT_BACKUP_DELAY_SECS]); the caller's gates (encryption degraded, auto
     * backup) have run before. A non-positive interval ("Never") registers nothing, cancels
     * REGULAR and returns null (Rule 4a). Otherwise it builds the periodic REGULAR request and
     * enqueues it with UPDATE without waiting for the result: [onRegularUpdateResult] reads the
     * result when it completes, so the caller's thread (the boot receiver's main thread) never
     * blocks.
     */
    private fun registerRegular(caller: String, initialDelaySecs: Long): ScheduledJob? {
        val regularSecs = preferences.regularTimeoutSecs
        if (regularSecs <= 0) {
            Log.d(TAG, "WorkManagerScheduler.$caller: skipped (regular schedule is Never, " +
                    "regularTimeoutSecs=$regularSecs); cancelling REGULAR")
            cancelRegular()
            return null
        }

        val intervalSecs = maxOf(regularSecs.toLong(), PERIODIC_MIN_INTERVAL_SECS)

        val builder = PeriodicWorkRequest.Builder(
            BackupWorker::class.java,
            intervalSecs,
            TimeUnit.SECONDS
        )
            .setConstraints(networkConstraints(BackupType.REGULAR))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_INITIAL_SECS, TimeUnit.SECONDS)
            .addTag(BackupType.REGULAR.name)
        if (initialDelaySecs > 0) {
            builder.setInitialDelay(initialDelaySecs, TimeUnit.SECONDS)
        }
        val request = builder.build()

        val result = WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                BackupType.REGULAR.name,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            .result

        Log.d(TAG, "WorkManagerScheduler.$caller: registered REGULAR (UPDATE) with " +
                "interval=${intervalSecs}s, firstRunDelay=${initialDelaySecs}s, " +
                "network=${constraintDescription(BackupType.REGULAR)}")
        result.addListener({ onRegularUpdateResult(caller, result, request) }, Executor { it.run() })
        return ScheduledJob(BackupType.REGULAR.name, "WorkManagerScheduler:REGULAR @ ${intervalSecs}s")
    }

    /**
     * Listener on the result of the REGULAR UPDATE. It runs on the thread that completes the
     * result, or at once when the result is already complete; the result is complete when it
     * runs, so get() does not block. On success it does nothing. On failure (the expected cause:
     * a one-time entry under REGULAR left by an earlier build, whose work type UPDATE may not
     * change) it reads the three gates again. If they still allow the registration, it logs one
     * warning and re-registers the same request with CANCEL_AND_REENQUEUE, which deletes every
     * entry under REGULAR and inserts the periodic one; otherwise it logs that it did not.
     */
    private fun onRegularUpdateResult(caller: String, result: Future<*>, request: PeriodicWorkRequest) {
        val failure: Throwable = try {
            result.get()
            return
        } catch (e: ExecutionException) {
            e.cause ?: e
        } catch (e: Exception) {
            e
        }
        val degraded = secretStore.isEncryptionDegraded()
        val autoBackup = preferences.isAutoBackupEnabled
        val regularSecs = preferences.regularTimeoutSecs
        if (degraded || !autoBackup || regularSecs <= 0) {
            Log.w(TAG, "WorkManagerScheduler.$caller: UPDATE failed on REGULAR " +
                    "(${failure.javaClass.simpleName}); not re-registered, a gate closed " +
                    "(degraded=$degraded, autoBackup=$autoBackup, regularTimeoutSecs=$regularSecs)")
            return
        }
        Log.w(TAG, "WorkManagerScheduler.$caller: UPDATE failed on REGULAR " +
                "(${failure.javaClass.simpleName}: ${failure.message}); re-registering with CANCEL_AND_REENQUEUE")
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                BackupType.REGULAR.name,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                request
            )
    }
"""

W6_OLD = """import java.util.concurrent.TimeUnit
import javax.inject.Inject
"""
W6_NEW = """import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.inject.Inject
"""

B1_OLD = """ * <strong>Contract: CNTR-MODERNIZATION-004 v2</strong> — all ten operations below,
"""
B1_NEW = """ * <strong>Contract: CNTR-MODERNIZATION-004 v4</strong> — all ten operations below,
"""

B2_OLD = """    /**
     * Schedules a recurring automatic backup.
     * <p>
     * Network constraint per {@code isWifiOnly()}; exponential backoff (30 s initial,
     * 300 s cap); REPLACE/UPDATE on a stable unique name. Returns {@code null} if
     * auto-backup is disabled.
     *
     * @return the enqueued job, or {@code null} if not scheduled
     */
"""
B2_NEW = """    /**
     * Schedules a recurring automatic backup.
     * <p>
     * Network constraint per {@code isWifiOnly()}; exponential backoff (30 s initial,
     * 300 s cap); UPDATE on a stable unique name, re-registered (cancel and re-enqueue) only
     * when that update fails. Interval = {@code getRegularTimeoutSecs()}; a positive value
     * below 15 minutes is raised to 15 minutes. A non-positive value ("Never") registers
     * nothing, cancels an existing regular backup and returns {@code null}
     * (CNTR-MODERNIZATION-004 v4, Rule 4a). Returns {@code null} if auto-backup is disabled.
     *
     * @return the enqueued job, or {@code null} if not scheduled ("Never", auto-backup
     *         disabled, or encryption degraded)
     */
"""

B3_OLD = """     * Schedules a recurring content-URI observer over the SMS provider (and the
     * call-log URI when {@code isCallLogBackupAfterCallEnabled()}).
"""
B3_NEW = """     * Schedules a recurring content-URI observer over the SMS provider (and the
     * call-log URI when call-log backup is enabled and
     * {@code isCallLogBackupAfterCallEnabled()}).
"""

B4_OLD = """    /**
     * Schedules (or cancels) jobs on device boot.
     * <p>
     * If {@code !isAutoBackupEnabled()}: calls {@link #cancelAll()} and returns
     * {@code null}. Otherwise schedules a regular backup with the 60-second boot delay.
     *
     * @return the enqueued job, or {@code null}
     */
"""
B4_NEW = """    /**
     * Keeps the regular backup registered on device boot.
     * <p>
     * If {@code !isAutoBackupEnabled()}: calls {@link #cancelAll()} and returns
     * {@code null}. Otherwise registers the regular recurring backup exactly as
     * {@link #scheduleRegular()} does, with the 60-second boot delay as the first-run delay of
     * a new registration. An existing registration is kept (its schedule once it has run), and
     * a running regular backup is not cancelled. It never schedules a one-off regular backup and
     * starts no separate boot backup. A regular schedule of "Never" (a non-positive interval)
     * registers nothing, cancels an existing regular backup and returns {@code null}
     * (CNTR-MODERNIZATION-004 v4, Rule 4). Legacy source: {@code BackupJobs.java:86-97}, both
     * branches.
     *
     * @return the registered job, or {@code null} ("Never", auto-backup disabled, or
     *         encryption degraded)
     */
"""

EDITS = [
    ("W1", WMS, W1_OLD, W1_NEW), ("W2", WMS, W2_OLD, W2_NEW), ("W3", WMS, W3_OLD, W3_NEW),
    ("W4", WMS, W4_OLD, W4_NEW), ("W5", WMS, W5_OLD, W5_NEW), ("W6", WMS, W6_OLD, W6_NEW),
    ("B1", BS, B1_OLD, B1_NEW), ("B2", BS, B2_OLD, B2_NEW), ("B3", BS, B3_OLD, B3_NEW),
    ("B4", BS, B4_OLD, B4_NEW),
]

M1_NEW = """        val request = OneTimeWorkRequest.Builder(BackupWorker::class.java)
            .setConstraints(networkConstraints(BackupType.REGULAR))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_INITIAL_SECS, TimeUnit.SECONDS)
            .setInitialDelay(BOOT_BACKUP_DELAY_SECS, TimeUnit.SECONDS)
            .addTag(BackupType.REGULAR.name)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(BackupType.REGULAR.name, ExistingWorkPolicy.REPLACE, request)

        Log.d(TAG, "WorkManagerScheduler.scheduleBootup: enqueued bootup REGULAR with " +
                "${BOOT_BACKUP_DELAY_SECS}s delay")
        return ScheduledJob(BackupType.REGULAR.name, "WorkManagerScheduler:bootup @ ${BOOT_BACKUP_DELAY_SECS}s")
"""
BOOT_CALL = """        return registerRegular("scheduleBootup", BOOT_BACKUP_DELAY_SECS)
"""
NEVER_BLOCK = """        if (regularSecs <= 0) {
            Log.d(TAG, "WorkManagerScheduler.$caller: skipped (regular schedule is Never, " +
                    "regularTimeoutSecs=$regularSecs); cancelling REGULAR")
            cancelRegular()
            return null
        }
"""
MUTATIONS = [
    ("M-1", BOOT_CALL, M1_NEW),
    ("M-2", """        result.addListener({ onRegularUpdateResult(caller, result, request) }, Executor { it.run() })
""", """        // M-2: the failed UPDATE is ignored again (no fallback listener)
"""),
    ("M-3", """                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            .result
""", """                if (initialDelaySecs > 0) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            .result
"""),
    ("M-4", NEVER_BLOCK, """        // M-4: no "Never" early return
"""),
    ("M-5", """        if (regularSecs <= 0) {
            Log.d(""", """        if (regularSecs < 0) {
            Log.d("""),
    ("M-6", """            cancelRegular()
            return null
        }

        val intervalSecs""", """            return null
        }

        val intervalSecs"""),
    ("M-7", """        if (degraded || !autoBackup || regularSecs <= 0) {
""", """        if (false) {
"""),
    ("M-8", BOOT_CALL, """        return registerRegular("scheduleBootup", 0L)
"""),
]


ALLOWED = {
    WMS: [(18, 32), (40, 42), (67, 69), (112, 115), (160, 202), (252, 286)],
    BS: [(36, 36), (58, 68), (70, 72), (83, 92)],
}


def hunks(diff_path):
    with open(diff_path, "rb") as f:
        lines = f.read().decode("utf-8").replace("\r\n", "\n").split("\n")
    bad, count, rel = 0, 0, None
    for line in lines:
        if line.startswith("diff --git "):
            rel = line.split(" b/", 1)[1] if " b/" in line else None
            if rel not in ALLOWED:
                print("OUTSIDE-FILE %s" % rel)
                bad += 1
            continue
        if line.startswith("@@ ") and rel in ALLOWED:
            old = line.split(" ")[1][1:]
            start, n = (old.split(",") + ["1"])[:2]
            start, n = int(start), int(n)
            lo, hi = (start, start + n - 1) if n else (start, start)
            ok = any((r0 <= lo and hi <= r1) if n else (r0 - 1 <= lo <= r1) for r0, r1 in ALLOWED[rel])
            count += 1
            print("%s %s old=%d,%d" % ("ok" if ok else "OUTSIDE", rel.rsplit("/", 1)[1], start, n))
            bad += 0 if ok else 1
            continue
        if rel == BS and line[:1] in "+-" and not line.startswith(("+++", "---")):
            body = line[1:].strip()
            if not (body.startswith("*") or body.startswith("/**")):
                print("NOT-A-COMMENT %s" % line[:100])
                bad += 1
    print("RESULT mode=hunks hunks=%d mismatches=%d" % (count, bad))
    return 0 if bad == 0 and count > 0 else 1


def load(root, rel):
    path = os.path.join(root, rel)
    if not os.path.isfile(path):
        print("missing " + rel)
        sys.exit(2)
    with open(path, "rb") as f:
        text = f.read().decode("utf-8")
    crlf = text.count("\r\n")
    if crlf and crlf != text.count("\n"):
        print("mixed-line-endings " + rel)
        sys.exit(2)
    return text.replace("\r\n", "\n"), crlf > 0


def save(root, rel, text, crlf):
    data = (text.replace("\n", "\r\n") if crlf else text).encode("utf-8")
    with open(os.path.join(root, rel), "wb") as f:
        f.write(data)
    print("wrote %s sha256=%s lines=%d eol=%s" % (rel, hashlib.sha256(data).hexdigest(), text.count("\n"),
                                                  "crlf" if crlf else "lf"))


def check(root, mode):
    files = {}
    bad = 0
    for eid, rel, old, new in EDITS:
        if rel not in files:
            files[rel] = load(root, rel)
        text = files[rel][0]
        if mode == "start":
            n = text.count(old)
            ok = n == 1
            print("%s %s old=%d" % ("ok" if ok else "MISMATCH", eid, n))
        else:
            n_new = text.count(new)
            n_old = 0 if old in new else text.count(old)
            ok = n_new == 1 and n_old == 0
            print("%s %s new=%d old=%d" % ("ok" if ok else "MISMATCH", eid, n_new, n_old))
        bad += 0 if ok else 1
    return files, bad


def main(argv):
    if len(argv) < 3:
        print(__doc__)
        return 2
    root, mode = argv[1], argv[2]
    if mode == "hunks":
        if len(argv) < 4 or not os.path.isfile(argv[3]):
            print("missing diff file")
            return 2
        return hunks(argv[3])
    if mode in ("start", "fixed"):
        _, bad = check(root, mode)
        print("RESULT mode=%s mismatches=%d" % (mode, bad))
        return 0 if bad == 0 else 1
    if mode == "apply":
        files, bad = check(root, "start")
        if bad:
            print("RESULT mode=apply mismatches=%d written=0" % bad)
            return 1
        for eid, rel, old, new in EDITS:
            text, crlf = files[rel]
            files[rel] = (text.replace(old, new, 1), crlf)
        for rel in (WMS, BS):
            save(root, rel, files[rel][0], files[rel][1])
        print("RESULT mode=apply mismatches=0 written=2")
        return 0
    if mode in ("mutants", "mutate"):
        text, crlf = load(root, WMS)
        wanted = argv[3] if mode == "mutate" and len(argv) > 3 else None
        if mode == "mutate" and wanted not in [m[0] for m in MUTATIONS]:
            print("unknown injection id")
            return 2
        bad = 0
        for mid, old, new in MUTATIONS:
            if wanted and mid != wanted:
                continue
            n = text.count(old)
            ok = n == 1
            print("%s %s old=%d" % ("ok" if ok else "MISMATCH", mid, n))
            bad += 0 if ok else 1
            if mode == "mutate" and ok:
                save(root, WMS, text.replace(old, new, 1), crlf)
        print("RESULT mode=%s mismatches=%d" % (mode if not wanted else "mutate " + wanted, bad))
        return 0 if bad == 0 else 1
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
```

**Embedded file E4** — `u076_sets.py` (developer: the derived outcome of every targeted run; blob `006b0061d7f8f1063e90e882e37985072835744f`,
229 lines):

```python
#!/usr/bin/env python3
"""U-076: compare one targeted run's JUnit XML with the plan's derived outcome (plan 5.2 and 4.8).

usage: python -I -X utf8 u076_sets.py <results-dir> <RED|GREEN|M-1|...|M-8> [--no-succeeded]
Reads TEST-*.xml of WorkManagerSchedulerRegularBootTest (NEW), WorkManagerSchedulerTest (OLD) and, for RED,
U076LibraryProbeTest (PROBE). A case fails when its XML has <failure> or <error>. For each expected failure
the type must be an assertion (Truth or JUnit) and the first line of its message must contain the plan's
label. Every OLD case must pass. A probe p1a failure at an "H-2 (recorded" line is printed, not counted
(every STOP line of p1a comes before it). --no-succeeded: the two AC-3 (c) tests were removed (row D12): in RED they
are expected to fail at their precondition and probe p3 at its P-3 line; elsewhere they are absent.
Prints one line per failing or unexpected case and a RESULT line.
Exit 0 = matches; 1 = does not match; 2 = usage, or no XML for a class the run needs.
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET

PKG = "com.zegoggles.smssync.service."
NEW, OLD, PROBE = PKG + "WorkManagerSchedulerRegularBootTest", PKG + "WorkManagerSchedulerTest", PKG + "U076LibraryProbeTest"
SUCC = ("regular_overOneTimeSeed_succeeded_registersPeriodic", "boot_overOneTimeSeed_succeeded_registersPeriodic")
ALL_NEW = [
    "boot_afterRegisteredRegular_keepsSamePeriodicEntry",
    "boot_nothingRegistered_registersPeriodic_wifiOnly", "boot_nothingRegistered_registersPeriodic_anyNetwork",
    "regular_overOneTimeSeed_enqueued_registersPeriodic", "regular_overOneTimeSeed_cancelled_registersPeriodic",
    "regular_overOneTimeSeed_succeeded_registersPeriodic", "boot_overOneTimeSeed_enqueued_registersPeriodic",
    "boot_overOneTimeSeed_cancelled_registersPeriodic", "boot_overOneTimeSeed_succeeded_registersPeriodic",
    "inv1_regularAndBoot_sequence1_oneNonTerminalPeriodic", "inv1_regularAndBoot_sequence2_oneNonTerminalPeriodic",
    "inv1_regularAndBoot_sequence3_oneNonTerminalPeriodic",
    "fallback_autoBackupOffBeforeFallback_enqueuesNothing", "fallback_degradedBeforeFallback_enqueuesNothing",
    "fallback_neverBeforeFallback_enqueuesNothing",
    "never_minusOne_registersNothing", "never_zero_registersNothing", "never_afterRegistered_cancelsRegular",
    "boot_never_minusOne_registersNothing", "boot_never_zero_registersNothing", "boot_never_afterRegistered_cancelsRegular",
    "upgrade_neverSeed_boot_cancels", "upgrade_neverSeed_regular_cancels",
    "regular_belowMinimum_raisedTo900s", "regular_offeredValues_useOwnInterval", "never_incomingPathUnchanged",
    "boot_degraded_afterRegistered_leavesItUnchanged", "boot_degraded_never_cancelsNothing",
    "boot_leavesContentTriggerUnchanged", "boot_autoBackupOff_cancelsAll",
]
PERIODIC_BOOT = "after scheduleBootup(): the entry is periodic"
PERIODIC_CALL = "after the call: the entry is periodic"
JOB = "returned job"
SKIPPED = "the fallback ran and did not re-register (one line)"
NONTERM = "non-terminal entries under REGULAR"
SETS = {
    "RED": {
        "boot_afterRegisteredRegular_keepsSamePeriodicEntry": PERIODIC_BOOT,
        "boot_nothingRegistered_registersPeriodic_wifiOnly": PERIODIC_BOOT,
        "boot_nothingRegistered_registersPeriodic_anyNetwork": PERIODIC_BOOT,
        "regular_overOneTimeSeed_enqueued_registersPeriodic": PERIODIC_CALL,
        "regular_overOneTimeSeed_cancelled_registersPeriodic": PERIODIC_CALL,
        "regular_overOneTimeSeed_succeeded_registersPeriodic": PERIODIC_CALL,
        "boot_overOneTimeSeed_enqueued_registersPeriodic": PERIODIC_CALL,
        "boot_overOneTimeSeed_cancelled_registersPeriodic": PERIODIC_CALL,
        "boot_overOneTimeSeed_succeeded_registersPeriodic": PERIODIC_CALL,
        "inv1_regularAndBoot_sequence1_oneNonTerminalPeriodic": "that item is periodic",
        "inv1_regularAndBoot_sequence2_oneNonTerminalPeriodic": "that item is periodic",
        "inv1_regularAndBoot_sequence3_oneNonTerminalPeriodic": "non-terminal REGULAR-tagged items",
        "fallback_autoBackupOffBeforeFallback_enqueuesNothing": SKIPPED,
        "fallback_degradedBeforeFallback_enqueuesNothing": SKIPPED,
        "fallback_neverBeforeFallback_enqueuesNothing": SKIPPED,
        "never_minusOne_registersNothing": JOB, "never_zero_registersNothing": JOB,
        "never_afterRegistered_cancelsRegular": JOB, "boot_never_minusOne_registersNothing": JOB,
        "boot_never_zero_registersNothing": JOB, "boot_never_afterRegistered_cancelsRegular": JOB,
        "upgrade_neverSeed_boot_cancels": JOB, "upgrade_neverSeed_regular_cancels": JOB,
    },
    "GREEN": {},
    "M-1": {
        "boot_afterRegisteredRegular_keepsSamePeriodicEntry": PERIODIC_BOOT,
        "boot_nothingRegistered_registersPeriodic_wifiOnly": PERIODIC_BOOT,
        "boot_nothingRegistered_registersPeriodic_anyNetwork": PERIODIC_BOOT,
        "boot_overOneTimeSeed_enqueued_registersPeriodic": PERIODIC_CALL,
        "boot_overOneTimeSeed_cancelled_registersPeriodic": PERIODIC_CALL,
        "boot_overOneTimeSeed_succeeded_registersPeriodic": PERIODIC_CALL,
        "inv1_regularAndBoot_sequence1_oneNonTerminalPeriodic": "that item is periodic",
        "boot_never_minusOne_registersNothing": JOB, "boot_never_zero_registersNothing": JOB,
        "boot_never_afterRegistered_cancelsRegular": JOB, "upgrade_neverSeed_boot_cancels": JOB,
    },
    "M-2": {
        "regular_overOneTimeSeed_enqueued_registersPeriodic": PERIODIC_CALL,
        "regular_overOneTimeSeed_cancelled_registersPeriodic": PERIODIC_CALL,
        "regular_overOneTimeSeed_succeeded_registersPeriodic": PERIODIC_CALL,
        "boot_overOneTimeSeed_enqueued_registersPeriodic": PERIODIC_CALL,
        "boot_overOneTimeSeed_cancelled_registersPeriodic": PERIODIC_CALL,
        "boot_overOneTimeSeed_succeeded_registersPeriodic": PERIODIC_CALL,
        "fallback_autoBackupOffBeforeFallback_enqueuesNothing": SKIPPED,
        "fallback_degradedBeforeFallback_enqueuesNothing": SKIPPED,
        "fallback_neverBeforeFallback_enqueuesNothing": SKIPPED,
    },
    "M-3": {
        "boot_afterRegisteredRegular_keepsSamePeriodicEntry": "id (an in-place UPDATE keeps it)",
        "boot_overOneTimeSeed_enqueued_registersPeriodic": "fallback warnings",
        "boot_overOneTimeSeed_cancelled_registersPeriodic": "fallback warnings",
        "boot_overOneTimeSeed_succeeded_registersPeriodic": "fallback warnings",
    },
    "M-4": {
        "never_minusOne_registersNothing": JOB, "never_zero_registersNothing": JOB,
        "never_afterRegistered_cancelsRegular": JOB, "boot_never_minusOne_registersNothing": JOB,
        "boot_never_zero_registersNothing": JOB, "boot_never_afterRegistered_cancelsRegular": JOB,
        "upgrade_neverSeed_boot_cancels": JOB, "upgrade_neverSeed_regular_cancels": JOB,
    },
    "M-5": {"never_zero_registersNothing": JOB, "boot_never_zero_registersNothing": JOB},
    "M-6": {
        "never_afterRegistered_cancelsRegular": NONTERM, "boot_never_afterRegistered_cancelsRegular": NONTERM,
        "upgrade_neverSeed_boot_cancels": NONTERM, "upgrade_neverSeed_regular_cancels": NONTERM,
    },
    "M-7": {
        "fallback_autoBackupOffBeforeFallback_enqueuesNothing": "periodic entries under REGULAR",
        "fallback_degradedBeforeFallback_enqueuesNothing": "periodic entries under REGULAR",
        "fallback_neverBeforeFallback_enqueuesNothing": "periodic entries under REGULAR",
    },
    "M-8": {
        "boot_nothingRegistered_registersPeriodic_wifiOnly": "initialDelayMillis (the boot delay of a new registration)",
        "boot_nothingRegistered_registersPeriodic_anyNetwork": "initialDelayMillis (the boot delay of a new registration)",
    },
}
PROBES = [
    "p1a_updateOverNeverRunPeriodic_keepsTheRegistration",
    "p1b_updateOverOneTimeEnqueued_failsOnTheOperationOnly_thenCancelAndReenqueueReplacesIt",
    "p1b_updateOverOneTimeCancelled_failsOnTheOperationOnly_thenCancelAndReenqueueReplacesIt",
    "p3_oneTimeSeedReachesSucceeded_thenUpdateFailsAndCancelAndReenqueueReplacesIt",
    "p1c_updateOverCancelledPeriodicOrNothing_registersNew",
]
ASSERTION_TYPES = ("AssertionErrorWithFacts", "ComparisonFailureWithFacts", "AssertionError", "ComparisonFailure")


def read_cases(results):
    cases = {}
    for path in sorted(glob.glob(os.path.join(results, "TEST-*.xml"))):
        root = ET.parse(path).getroot()
        for tc in root.iter("testcase"):
            cls, name = tc.get("classname") or "", tc.get("name") or ""
            status, kind, first = "PASS", "", ""
            for tag, label in (("failure", "FAIL"), ("error", "ERROR"), ("skipped", "SKIP")):
                el = tc.find(tag)
                if el is not None:
                    status, kind = label, (el.get("type") or "")
                    message = el.get("message") or ""
                    first = message.split("\n", 1)[0]
                    break
            cases[(cls, name)] = (status, kind, first)
    return cases


def main(argv):
    args = [a for a in argv[1:] if a != "--no-succeeded"]
    no_succ = "--no-succeeded" in argv
    if len(args) != 2 or args[1] not in SETS:
        print(__doc__)
        return 2
    results, sid = args
    cases = read_cases(results)
    classes = {c for c, _ in cases}
    need = [NEW, OLD] + ([PROBE] if sid == "RED" else [])
    missing_class = [c for c in need if c not in classes]
    if missing_class:
        print("RESULT id=%s missing-class=%s verdict=does-not-match" % (sid, ",".join(c.rsplit(".", 1)[1] for c in missing_class)))
        return 2
    expected = dict(SETS[sid])
    names = list(ALL_NEW)
    if no_succ:
        if sid == "RED":
            for n in SUCC:
                expected[n] = "precondition: the seed's state"
        else:
            names = [n for n in names if n not in SUCC]
            for n in SUCC:
                expected.pop(n, None)
    bad = 0
    observed_fail = 0
    for n in names:
        status, kind, first = cases.get((NEW, n), ("ABSENT", "", ""))
        if status == "ABSENT":
            print("ABSENT %s" % n)
            bad += 1
            continue
        failed = status in ("FAIL", "ERROR")
        observed_fail += 1 if failed else 0
        if status == "SKIP":
            print("SKIP %s" % n)
            bad += 1
        elif n in expected:
            if not failed:
                print("MISSING-FAIL %s" % n)
                bad += 1
            else:
                type_ok = kind.rsplit(".", 1)[-1] in ASSERTION_TYPES
                label_ok = expected[n] in first
                ok = type_ok and label_ok
                print("%s %s type=%s label=%s" % ("FAIL-AS-EXPECTED" if ok else "FAIL-WRONG-REASON", n,
                                                   kind.rsplit(".", 1)[-1], "ok" if label_ok else "other"))
                bad += 0 if ok else 1
        elif failed:
            print("EXTRA-FAIL %s type=%s" % (n, kind.rsplit(".", 1)[-1]))
            bad += 1
    extra_new = sorted(name for (c, name) in cases if c == NEW and name not in names)
    for n in extra_new:
        print("UNKNOWN-CASE %s" % n)
        bad += 1
    old = [(name, v[0]) for (c, name), v in cases.items() if c == OLD]
    old_bad = [name for name, s in old if s != "PASS"]
    for n in old_bad:
        print("OLD-NOT-PASS %s" % n)
    bad += len(old_bad)
    probe_txt = "-"
    if sid == "RED":
        probe_bad = 0
        for p in PROBES:
            status, kind, first = cases.get((PROBE, p), ("ABSENT", "", ""))
            want_fail = no_succ and p.startswith("p3_")
            if want_fail:
                ok = status in ("FAIL", "ERROR") and "P-3" in first
            else:
                ok = status == "PASS"
            if not ok and p.startswith("p1a_") and status == "FAIL" and "H-2 (recorded" in first:
                print("PROBE-H2-RECORDED %s %s" % (p, first[:160]))
                continue
            if not ok:
                print("PROBE-%s %s %s" % ("NOT-AS-EXPECTED", p, first[:160]))
                probe_bad += 1
        bad += probe_bad
        probe_txt = "%d/%d" % (len(PROBES) - probe_bad, len(PROBES))
    print("RESULT id=%s new-cases=%d expected-fail=%d observed-fail=%d old=%d/%d probe=%s mismatches=%d verdict=%s" % (
        sid + (" --no-succeeded" if no_succ else ""), len(names), len(expected), observed_fail,
        len(old) - len(old_bad), len(old), probe_txt, bad, "matches" if bad == 0 else "does-not-match"))
    return 0 if bad == 0 else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
```

**Embedded file E5** — `workdb_rows.py` (orchestrator, AC-21: WorkManager's database copy; blob `48e0e5fd843122aeb6998b3f6d212a7d8900df88`,
83 lines):

```python
#!/usr/bin/env python3
"""U-076 AC-21: read WorkManager's database, copied off the emulator into a folder outside the repository.

usage: python -I -X utf8 workdb_rows.py <folder>
The folder holds androidx.work.workdb and, when the device had them, androidx.work.workdb-wal and -shm.
Prints one ENTRY line per WorkSpec under the unique names REGULAR, CONTENT_TRIGGER and INCOMING, one TAGGED
line per WorkSpec tagged REGULAR, then one SUMMARY line. Ids are cut to 8 characters; no other column of the
database is printed. Periodic means interval_duration > 0 (WorkManager 2.9.1 schema, read in its bytecode).
Exit 0 = read; 1 = no SQLite database in the folder, or a query failed.
"""
import os
import sqlite3
import sys

STATES = {0: "ENQUEUED", 1: "RUNNING", 2: "SUCCEEDED", 3: "FAILED", 4: "BLOCKED", 5: "CANCELLED"}
NON_TERMINAL = ("ENQUEUED", "RUNNING", "BLOCKED")


def main(argv):
    if len(argv) != 2:
        print(__doc__)
        return 1
    path = os.path.join(argv[1], "androidx.work.workdb")
    try:
        with open(path, "rb") as f:
            header = f.read(16)
    except OSError:
        header = b""
    if header != b"SQLite format 3\x00":
        print("db=unreadable")
        return 1
    try:
        con = sqlite3.connect(path)
        rows = con.execute(
            "SELECT n.name, s.id, s.state, s.worker_class_name, s.interval_duration, s.initial_delay, "
            "s.period_count, s.generation FROM workname n JOIN workspec s ON s.id = n.work_spec_id "
            "WHERE n.name IN ('REGULAR', 'CONTENT_TRIGGER', 'INCOMING') ORDER BY n.name, s.id").fetchall()
        tagged = con.execute(
            "SELECT s.id, s.state, s.interval_duration, "
            "(SELECT group_concat(w.name) FROM workname w WHERE w.work_spec_id = s.id) "
            "FROM worktag t JOIN workspec s ON s.id = t.work_spec_id WHERE t.tag = 'REGULAR' ORDER BY s.id").fetchall()
        con.close()
    except sqlite3.Error as e:
        print("db=unreadable query-error=%s" % type(e).__name__)
        return 1
    counts = {"regular": 0, "periodic": 0, "onetime": 0, "nonterminal": 0, "nonterminal-periodic": 0,
              "trigger-nonterminal": 0, "incoming-nonterminal": 0}
    for name, sid, state, worker, interval, delay, periods, generation in rows:
        st = STATES.get(state, str(state))
        periodic = interval > 0
        print("ENTRY name=%s id8=%s state=%s periodic=%s interval_ms=%d initial_delay_ms=%d period_count=%d "
              "generation=%d worker=%s" % (name, sid[:8], st, "yes" if periodic else "no", interval, delay, periods,
                                           generation, worker.rsplit(".", 1)[-1]))
        if name == "REGULAR":
            counts["regular"] += 1
            counts["periodic" if periodic else "onetime"] += 1
            if st in NON_TERMINAL:
                counts["nonterminal"] += 1
                counts["nonterminal-periodic"] += 1 if periodic else 0
        elif name == "CONTENT_TRIGGER" and st in NON_TERMINAL:
            counts["trigger-nonterminal"] += 1
        elif name == "INCOMING" and st in NON_TERMINAL:
            counts["incoming-nonterminal"] += 1
    tagged_nt, elsewhere = 0, 0
    for sid, state, interval, names in tagged:
        st = STATES.get(state, str(state))
        names = names or ""
        print("TAGGED id8=%s state=%s periodic=%s names=%s" % (sid[:8], st, "yes" if interval > 0 else "no", names or "-"))
        if st in NON_TERMINAL:
            tagged_nt += 1
            if "REGULAR" not in names.split(","):
                elsewhere += 1
    print("SUMMARY regular-entries=%d regular-periodic=%d regular-onetime=%d regular-nonterminal=%d "
          "regular-nonterminal-periodic=%d tagged-regular-nonterminal=%d tagged-regular-elsewhere=%d "
          "content-trigger-nonterminal=%d incoming-nonterminal=%d" % (
              counts["regular"], counts["periodic"], counts["onetime"], counts["nonterminal"],
              counts["nonterminal-periodic"], tagged_nt, elsewhere, counts["trigger-nonterminal"],
              counts["incoming-nonterminal"]))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
```

**Embedded file E6** — `ui_find.py` (orchestrator, AC-21 (iii): one node of a UI dump kept outside the repository; blob
`6df2733a04ea605979afcebca78fa6c8554e1b2f`, 48 lines):

```python
#!/usr/bin/env python3
"""U-076 AC-21 (iii): find one node in a uiautomator dump kept outside the repository; print nothing else of it.

usage: python -I -X utf8 ui_find.py <dump.xml> text <label>   centre "X Y" of the one node whose text equals
                                                              <label> (case and surrounding space ignored)
       python -I -X utf8 ui_find.py <dump.xml> checked        "checked=<text>" of the one checked node
Exit 0 = exactly one node; 1 = none or more than one ("found=<n>"), or the dump is unreadable ("xml=unreadable").
"""
import re
import sys
import xml.etree.ElementTree as ET

BOUNDS = re.compile(r"^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$")


def main(argv):
    if len(argv) < 3 or argv[2] not in ("text", "checked") or (argv[2] == "text" and len(argv) != 4):
        print(__doc__)
        return 1
    try:
        root = ET.parse(argv[1]).getroot()
    except (ET.ParseError, OSError):
        print("xml=unreadable")
        return 1
    nodes = list(root.iter("node"))
    if argv[2] == "text":
        want = argv[3].strip().lower()
        hits = [n for n in nodes if (n.get("text") or "").strip().lower() == want]
        if len(hits) != 1:
            print("found=%d" % len(hits))
            return 1
        m = BOUNDS.match(hits[0].get("bounds") or "")
        if not m:
            print("bounds=unreadable")
            return 1
        x1, y1, x2, y2 = (int(g) for g in m.groups())
        print("%d %d" % ((x1 + x2) // 2, (y1 + y2) // 2))
        return 0
    hits = [n for n in nodes if n.get("checked") == "true" and (n.get("text") or "").strip()]
    if len(hits) != 1:
        print("found=%d" % len(hits))
        return 1
    print("checked=%s" % hits[0].get("text").strip())
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
```

### 3.5 Not changed

`scheduleIncoming`, `scheduleContentTrigger`, `scheduleImmediate`, `scheduleManual`, `scheduleRestore`, `cancelAll`,
`cancelRegular`, `cancel`, `observe`, `networkConstraints`, `constraintDescription`, the companion's constants and every
other line outside 3.1's places; `BackupScheduler.java:32-33` (U-077's) and every signature (IC-1); `App.java`,
`receiver/*`, `worker/BackupTriggerWorker.kt`, `service/BackupWorker.kt`, `AndroidManifest.xml`; `build.gradle`,
`app/build.gradle` (no floor, no dependency), `settings.gradle`, `gradle/`; every existing test file; every contract,
design, requirement and change record.

## 4. Implementation Steps

### 4.0 Environment and conventions (every step)

```bash
# From the repository root. Never set MSYS_NO_PATHCONV for Gradle (CLAUDE.md, "Local Build & Device Notes").
unset MSYS_NO_PATHCONV
W=sdlc/artifacts/build/sprints/sprint-025/U-076; E=$W/evidence; M=$E/mutations; TL=$E/tools
T=sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools      # read-only: sanitize.py, junit_totals.py, junit_cases.py, jacoco_pkgs.py
U69T=sdlc/artifacts/build/sprints/sprint-024/U-069/evidence/tools   # read-only: jacoco_classes.py
R=app/build/test-results/testDebugUnitTest
JX=app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
S=app/src/main/java/com/zegoggles/smssync; TS=app/src/test/java/com/zegoggles/smssync
WMS=$S/scheduler/WorkManagerScheduler.kt; BS=$S/scheduler/BackupScheduler.java
NEWF=$TS/service/WorkManagerSchedulerRegularBootTest.kt; PRF=$TS/service/U076LibraryProbeTest.kt
NEW=com.zegoggles.smssync.service.WorkManagerSchedulerRegularBootTest
OLD=com.zegoggles.smssync.service.WorkManagerSchedulerTest
PRB=com.zegoggles.smssync.service.U076LibraryProbeTest
BRT=com.zegoggles.smssync.receiver.BootReceiverTest
BTWT=com.zegoggles.smssync.worker.BackupTriggerWorkerTest
PLAN_HEAD=2f58f927f503f016a87d7565110bcf636bbe5e36
WORK=<a u076-work folder inside the developer's own session scratch folder>   # raw logs, pristine copies, the Start APK
START=<the commit recorded in I0 step 2>
```
Shell state does not persist; each call sets what it needs. `WORK` is never written into a repository file (evidence
says "the work folder"). **Pipelines:** `$?` after `a | b` is `b`'s; every exit code the plan records is taken from a
command that writes to a file, never from a pipeline (self-test 14.4 found two such false readings).

**Gradle calls.** Foreground, tool timeout 600000 ms, output to the work folder, the exit code appended by the same
command line. A full gate takes about ten minutes since U-070 and the harness usually moves it to the background: wait
for the completion notice, never poll, start no other command meanwhile, then read the log; **a log whose last line is
not `exit=<n>` is a cut-off run: stop (row D9).** Never two Gradle processes at once.
- **G0** (I0) and **G-F** (I6), the only full gates (AC-18's command exactly):
  `rm -rf $R; ./gradlew --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease :app:jacocoTestReport > $WORK/logs/I<k>-gates.log 2>&1; echo "exit=$?" >> $WORK/logs/I<k>-gates.log`
- **TR(name, classes…)**: `rm -rf $R; ./gradlew --console=plain :app:testDebugUnitTest --tests "<fqcn>" [--tests "<fqcn>" …] > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> $WORK/logs/<name>.log`
  (no class list ever names `SmsBackupRecordSizeTest`; a failing test makes `exit=1`, which is expected in I2 and I5).

**Filters for `sanitize.py`** (`python -I -X utf8 $T/sanitize.py <in> <out> --grep '<regex>'`): gate filter **G** =
`'^(BUILD |FAILURE|> Task :app:(assemble|test|jacoco|lintVital|compile)|exit=)|tests completed|FAILED$|^e: |error:'`.

**EV(k)** (after a full gate, k = 0, 6):
```bash
python -I -X utf8 $T/sanitize.py $WORK/logs/I<k>-gates.log $E/I<k>-gates.txt --grep '<G>'
python -I -X utf8 $T/junit_totals.py $R > $E/I<k>-junit-totals.txt; echo "totals-exit=$?"
python -I -X utf8 $T/jacoco_pkgs.py $JX > $E/I<k>-jacoco-packages.txt; echo "jacoco-exit=$?"
python -I -X utf8 $U69T/jacoco_classes.py $JX com/zegoggles/smssync/scheduler > $E/I<k>-scheduler-classes.txt
grep -E '^com\.zegoggles\.smssync\.(scheduler|receiver) ' $E/I<k>-jacoco-packages.txt
```
**CASES(name)** (after a targeted run): `python -I -X utf8 $T/junit_cases.py $R RegularBoot WorkManagerSchedulerTest U076LibraryProbe BootReceiver BackupTriggerWorker > $E/<name>-cases.txt; echo "cases-exit=$?"`.

**Timestamps** from `date -u +%Y-%m-%dT%H:%M:%SZ`, run at that moment.

### I0 — Start state and baseline (no tracked file changes)

1. Create `$WORK/logs`, `$WORK/pristine`, `$E`, `$M`, `$TL`. Write E3 to E6 into `$TL` with the Write tool. Check
   `git hash-object $TL/u076_edits.py $TL/u076_sets.py $TL/workdb_rows.py $TL/ui_find.py` →
   `5f418cdd2b986d0d9054e836d7f45ba19df93a0d`, `006b0061d7f8f1063e90e882e37985072835744f`, `48e0e5fd843122aeb6998b3f6d212a7d8900df88`, `6df2733a04ea605979afcebca78fa6c8554e1b2f` (row D2).
2. **Start state** (one call into `$WORK/logs/I0-start.log`, sanitised to `$E/I0-start.txt`), each command echoed first:
   ```bash
   date -u +%Y-%m-%dT%H:%M:%SZ; git rev-parse HEAD; git rev-parse --abbrev-ref HEAD
   git merge-base --is-ancestor $PLAN_HEAD HEAD; echo "plan-head-ancestor-exit=$?"
   git log --oneline $PLAN_HEAD..HEAD -- app
   grep -n '^status' sdlc/artifacts/build/sprints/sprint-024/U-070-*.md sdlc/artifacts/build/sprints/sprint-024/U-075-*.md sdlc/artifacts/build/sprints/sprint-025/U-074-*.md sdlc/artifacts/build/sprints/sprint-025/U-071-*.md sdlc/artifacts/build/sprints/sprint-025/U-072-*.md sdlc/artifacts/build/sprints/sprint-025/U-077-*.md
   git status --porcelain --untracked-files=all -- app build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat; echo "porcelain-end"
   git diff --stat 74b31759 HEAD -- app/src/main/java/com/zegoggles/smssync/scheduler/; echo "s3-end"
   for p in $WMS $BS; do echo "$(git rev-parse HEAD:$p) $(git hash-object $p) $p"; done
   python -I -X utf8 $TL/u076_edits.py . start > $WORK/logs/I0-anchors.txt; echo "anchors-exit=$?"; cat $WORK/logs/I0-anchors.txt
   grep -n -E "work-runtime-ktx|work-testing|kotlin-stdlib|robolectric:|mockito-core|google.truth|toolVersion" app/build.gradle
   git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"
   git grep -nE '^\s*@Ignore(\s|\(|$)' HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt'
   git grep -n -e "scheduleBootup" -e "scheduleRegular" HEAD -- app/src/main
   git grep -n '"BOOT"' HEAD -- app/src/main > $WORK/logs/I0-boot-literal.txt; echo "boot-literal-exit=$?"
   git grep -n -E "SucceedingSeedWorker|WorkManagerSchedulerRegularBootTest|U076LibraryProbeTest" HEAD -- app > $WORK/logs/I0-names.txt; echo "names-exit=$?"
   grep -n -A8 -E "smssync\.(scheduler|receiver)\*'\]" app/build.gradle
   grep -v '^#' gradle/gradle-daemon-jvm.properties
   git status --porcelain --untracked-files=all -- sdlc/artifacts/design sdlc/artifacts/requirements sdlc/artifacts/build/change-records | cut -c4- | while read -r p; do sha256sum "$p"; done > $WORK/logs/others-I0.txt; cat $WORK/logs/others-I0.txt
   ```
   Expected: `plan-head-ancestor-exit=0`; the `-- app` log shows the closing commits of U-070, U-075, U-074, U-071, U-072
   and nothing else (S-1); the five stories `status: done`, U-077 not `done` (row D1); nothing before `porcelain-end`;
   **nothing before `s3-end` (S-3)**; both blob pairs equal and equal to `0194308c…` and `17522557…` (S-3; row D3);
   `anchors-exit=0` with `RESULT mode=start mismatches=0` (P-5: every anchor of W1 to W6 and B1 to B4 matches once at
   Start; row D4); `work-runtime-ktx:2.9.1` and `work-testing:2.9.1` (S-4; row D5); the Kotlin, Robolectric, Mockito,
   Truth and JaCoCo versions recorded (2.4.21, 4.18.1, 5.24.0, 1.4.5, 0.8.16 expected from U-071's plan 2.1; another
   value is recorded and checked against 2.5, row D6); `C0` with the commit id (771 at `fd70325a`; U-070, U-075, U-074
   add theirs; no expected number, AC-19); the `@Ignore` lines (two at `fd70325a`: `AppTest.java:13`,
   `AuthPreferencesTest.java:1079`); callers `App.java:377`, `BootReceiver.java:33` plus the port and the adapter (IC-1
   baseline); `boot-literal-exit=1` (IC-2 baseline); `names-exit=1` (the new names are free); the two rules with
   `minimum = 0.50` (receiver) and `0.60` (scheduler) (`app/build.gradle:528-548` at `fd70325a`); `toolchainVersion=25`;
   `others-I0.txt` one line per uncommitted document of someone else, or none. Record `START=$(git rev-parse HEAD)` and
   `C0` at the top of the log. **Rows:** D1 to D6.
3. `./gradlew --version > $WORK/logs/I0-gradle-version.log 2>&1`, sanitised to `$E/I0-gradle-version.txt` (the Daemon JVM
   line cites `gradle/gradle-daemon-jvm.properties`, Java 25).
4. **Gate G0** (expect the background, 4.0), then EV(0). Expected `exit=0` as the log's last line, `failures=0 errors=0`;
   `T0`, `S0` recorded; the scheduler and receiver lines recorded as `C_sched/T_sched`, `C_recv/T_recv` (no expected
   numbers: 137/185 and 39/58 at `fd70325a`, U-069 evidence `I6-jacoco-packages.txt`; nothing before Start changes these
   packages). **Rows:** D7, D8, D9.
5. **The Start APK for device step (ii):** `command ls -1 app/build/outputs/apk/debug/`, then
   `cp app/build/outputs/apk/debug/app-debug.apk $WORK/start-app-debug.apk; sha256sum $WORK/start-app-debug.apk | cut -c1-64`
   → `START_APK_SHA` (G-F overwrites the build folder later). Recorded in the log by hash only.
6. Create `$W/implementation-log.md`: story, plan path, `START`, `C0`, `T0`, `S0`, the coverage lines, the `@Ignore`
   lines, the toolchain versions, the I0 results by reference to the evidence files, P-5's mapping ("every anchor at its
   `fd70325a` text"), and PO-1/PO-2 "no" (ruling 68; CR-017 citation item 4).

### I1 — The test file and the probe (no production change)

1. Write E1 to `$NEWF` and E2 to `$PRF` with the Write tool.
2. Checks into `$WORK/logs/I1-checks.log`, sanitised to `$E/I1-checks.txt`: `git hash-object $NEWF $PRF` →
   `eac2af9f27e3db2c3914915e584253a398356303`, `d14411868143829af0c4c51b506fbdf6f65be290`; `grep -c "@Test" $NEWF $PRF` → 30, 5; `git status --porcelain --untracked-files=all -- app`
   → exactly `?? $NEWF` and `?? $PRF`; `git diff --quiet HEAD -- app; echo "tracked-clean-exit=$?"` → 0. **Rows:** D2, D17.

### I2 — Probe, characterisation and red run on the unchanged scheduler (one targeted run)

1. TR(`I2-red`, `$NEW` `$OLD` `$PRB`), then CASES(`I2-red`) and
   `python -I -X utf8 $TL/u076_sets.py $R RED > $E/I2-red-sets.txt; echo "sets-exit=$?"`.
2. `for x in $R/TEST-*.xml; do grep -o 'message="[^"]*"' "$x"; done > $WORK/logs/I2-msg.log; python -I -X utf8 $T/sanitize.py $WORK/logs/I2-msg.log $E/I2-red-messages.txt`;
   `grep -h -E '^P-(1a|1b|1c|3) ' $R/TEST-$PRB.xml > $WORK/logs/I2-probe.log; python -I -X utf8 $T/sanitize.py $WORK/logs/I2-probe.log $E/I2-probe-lines.txt`.
3. **Expected** (`exit=1` in the log, because the red tests fail): `RESULT id=RED new-cases=30 expected-fail=23
   observed-fail=23 old=<n>/<n> probe=5/5 mismatches=0 verdict=matches`, `sets-exit=0`. That is: the 23 tests of changed
   behaviour fail for the reasons of table 5.2 (each at its named assertion, an assertion type, not a compile or fixture
   error), the 7 guards pass (story Verification steps 4 and 5, from one run), every `WorkManagerSchedulerTest` case
   passes, and the five probe tests pass. The probe lines give `P-1a … gen=0 … periodCount=0`, `P-1a boot: … gen=1 …
   delay=60000`, `P-1b cause: java.lang.UnsupportedOperationException: Can't update OneTimeWorker to Periodic Worker. …`
   (twice, plus once from `p3`), `P-1c …`, `P-3 seed state after the barrier: SUCCEEDED`.
4. Decisions (rows): a probe line `STOP …` failed → **stop before any production edit** and report (D11); `P-3` failed →
   D12; `H-2` failed → recorded, not a stop (D13); a compile error → D10; any other mismatch → D14.
5. Record in the implementation log: the red output (by reference), P-1 to P-4 with their sources (section 2.3 and the
   probe lines), AC-3 (c) tested or "not tested: <reason>", and the note that a `RUNNING` regular backup is not covered
   by a test (AC-5).

### I3 — The fix and the green run

0. `rm $PRF`; `git status --porcelain --untracked-files=all -- app` → exactly `?? $NEWF`.
1. Only under row D12: the two Edits of 3.2; `git hash-object $NEWF` → `176d4dc8f75e1532f464aec8f74e979403c6f5ca`, `grep -c "@Test" $NEWF` → 28.
2. Into `$WORK/logs/I3-checks.log`, sanitised to `$E/I3-checks.txt`:
   ```bash
   python -I -X utf8 $TL/u076_edits.py . apply; echo "apply-exit=$?"
   python -I -X utf8 $TL/u076_edits.py . fixed; echo "fixed-exit=$?"
   git hash-object $WMS $BS
   git diff -U0 $START -- app/src/main > $WORK/logs/I3-diff.txt; python -I -X utf8 $TL/u076_edits.py . hunks $WORK/logs/I3-diff.txt > $WORK/logs/I3-hunks.txt; echo "hunks-exit=$?"; cat $WORK/logs/I3-hunks.txt
   git diff --numstat $START -- app/src/main
   python -I -X utf8 $TL/u076_edits.py . mutants > $WORK/logs/I3-mutants.txt; echo "mutants-exit=$?"; cat $WORK/logs/I3-mutants.txt
   git status --porcelain --untracked-files=all -- app
   ```
   Expected: `apply-exit=0` (two `wrote` lines, `eol=crlf` in this checkout or `lf`; either is fine); `fixed-exit=0`; the
   blobs `3d4fe760…`, `bbb2945f…`; `hunks-exit=0`, `RESULT mode=hunks hunks=21 mismatches=0`; numstat `21 8` (port) and
   `117 44` (scheduler); `mutants-exit=0` (every injection anchor once in the fixed file); porcelain ` M $BS`, ` M $WMS`,
   `?? $NEWF`. **Rows:** D2, D4, D17.
3. TR(`I3-green`, `$NEW` `$OLD` `$BRT` `$BTWT`), CASES(`I3-green`),
   `python -I -X utf8 $TL/u076_sets.py $R GREEN > $E/I3-green-sets.txt; echo "sets-exit=$?"` (add `--no-succeeded` under
   D12). Expected `exit=0` in the log; `verdict=matches`; in the cases file every `BootReceiverTest` and
   `BackupTriggerWorkerTest` case `PASS`. **Rows:** D10, D14.

### I4 — Pristine copies (no Gradle)

`cp $WMS $BS $WORK/pristine/`; `sha256sum $WMS $BS | cut -c1-64` → `SW`, `SB`; `git hash-object $WMS $BS` → `HW`, `HB`
(= 3.1's blobs); `git diff $START -- app/src/main | git hash-object --stdin` → `PROD_DIFF`. Into `$E/I4-pristine.txt`
(hashes only). Row D17.

### I5 — Failure injections (protocol 4.8; table 4.8)

M-1 to M-8, one at a time. Row D18.

### I6 — Final gate and the final checks

0. Clean check of 4.8; `grep -cE '^### [^ ]+ APPLYING ' $E/mutation-log.md` equals `grep -cE '^(- )?restored: ' $E/mutation-log.md`
   (both 8). Row D17.
1. **Gate G-F**, then EV(6). Expected `exit=0`; `failures=0 errors=0`; `tests` = `T0` + 30 (+ 28 under D12); `skipped`
   not above `S0`; the scheduler line at or above its floor 0.60 (expected to rise from `C_sched/T_sched`: the removed
   lines are covered today and every added line except the `catch (e: Exception)` branch of `onRegularUpdateResult` is
   run by the new tests); the receiver line equal to `C_recv/T_recv` (no receiver code changes). A scheduler ratio below
   0.60 is row D16 (stop, the floor is not changed). **Rows:** D7, D8, D9, D16.
2. **Final checks** (one call into `$WORK/logs/I6-final.log`, sanitised to `$E/I6-final-checks.txt`):
   ```bash
   git diff --name-only $START -- app/src/main
   git status --porcelain --untracked-files=all -- app
   git diff -U0 $START -- app/src/main > $WORK/logs/I6-diff.txt; python -I -X utf8 $TL/u076_edits.py . hunks $WORK/logs/I6-diff.txt > $WORK/logs/I6-hunks.txt; echo "hunks-exit=$?"
   git diff --quiet $START -- $S/App.java $S/receiver $S/worker/BackupTriggerWorker.kt $S/service/BackupWorker.kt app/src/main/AndroidManifest.xml build.gradle app/build.gradle settings.gradle gradle sdlc/artifacts/design sdlc/artifacts/requirements; echo "must-be-empty-exit=$?"
   git diff --quiet $START -- app/src/test; echo "tracked-tests-unchanged-exit=$?"
   for p in $WMS $BS; do echo "$(git hash-object $p) $(sha256sum $p | cut -c1-64) $p"; done
   grep -n "OneTimeWorkRequest" $WMS
   git grep -n -e "scheduleBootup" -e "scheduleRegular" -- app/src/main
   git grep -n '"BOOT"' -- app/src/main > $WORK/logs/I6-boot-literal.txt; echo "boot-literal-exit=$?"
   git grep -nE '^\s*@Ignore(\s|\(|$)' -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt'
   git grep -h --untracked "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"
   git status --porcelain --untracked-files=all -- sdlc/artifacts/design sdlc/artifacts/requirements sdlc/artifacts/build/change-records | cut -c4- | while read -r p; do sha256sum "$p"; done > $WORK/logs/others-I6.txt
   grep -vxF -f $WORK/logs/others-I0.txt $WORK/logs/others-I6.txt; echo "docs-new-or-changed-exit=$?"
   sha256sum app/build/outputs/apk/debug/app-debug.apk | cut -c1-64
   ```
   Expected: `--name-only` exactly `$BS` and `$WMS` (AC-15); porcelain ` M $BS`, ` M $WMS`, `?? $NEWF`; `hunks-exit=0`;
   `must-be-empty-exit=0` (AC-15; a diff in a document folder by the orchestrator in between is recorded with its commit,
   row D19); `tracked-tests-unchanged-exit=0` (AC-14: no existing test edited; the only test change is the new file);
   the blobs equal `HW`, `HB` and the SHA-256 values `SW`, `SB`; `OneTimeWorkRequest` at `:154`, `:382`, `:423`, `:458` only
   (AC-5); the callers unchanged (IC-1); `boot-literal-exit=1` (IC-2); the `@Ignore` lines as at I0; the count `C0` + 30
   (+ 28 under D12), stated "working tree with `--untracked`" (AC-19); `docs-new-or-changed-exit=1`; the fixed debug APK's
   SHA-256 → `FIXED_APK_SHA`. **Rows:** D16, D17, D19.

### I7 — Implementation log, hygiene, hand-off

1. Implementation log: I1 to I6 results by reference; the counts (`C0`, end count, the 30 new test names, other stories'
   tests listed by name if any landed between I0 and I6); the mutation summary; H-2's reading; the hand-off block
   (`FIXED_APK_SHA`, `START_APK_SHA`, the work-folder location of the Start APK described as "the developer's session
   scratch folder", the device section to run).
2. Hygiene: `python -I -X utf8 $T/sanitize.py <each evidence file> $WORK/logs/scan.tmp` over every file in `$E` and
   `$M` (exit 0 each; it refuses profile paths and e-mail-like text), and
   `git grep -n -i -E "@[a-z0-9.-]+\.[a-z]{2,}" -- $W` → nothing.
3. Story Implementation Notes: the items its section lists.

### 4.8 Mutation protocol (write-ahead; restore proven by hash) and table

`$E/mutation-log.md` header: "Protocol: U-076 plan 4.8 (U-074 AC-15 form: the production change uncommitted; each
changed production file saved and its `sha256sum` recorded first (I4); a write-ahead entry; one injection by
`u076_edits.py mutate`; one targeted run; restore by copying the saved file; the hash shown equal to the saved one;
`git status --porcelain -- app/src/main` listing only this story's files; never present at a gate, a review or a
commit)." Then `START`, `SW`, `SB`, `HW`, `HB`, `PROD_DIFF`. **Recovery note** (in the header): "If a session is lost while
an entry says `APPLYING` without a `restored` line, nothing else runs until `$WMS` is restored by `cp` from its pristine
copy, or, if that copy is lost, by `python -I -X utf8 $TL/u076_edits.py . apply` on a tree where `git checkout` restored
the Start text of both files, followed by the blob check `HW`, `HB` (`git hash-object`; the SHA-256 may then differ only
by line endings: record both). Never `git checkout` while the fix is in the tree without that re-apply."

**Clean check** (before and after each entry): `sha256sum $WMS $BS | cut -c1-64` = `SW SB`;
`git diff $START -- app/src/main | git hash-object --stdin` = `PROD_DIFF`; `git status --porcelain --untracked-files=all
-- app` = ` M $BS`, ` M $WMS`, `?? $NEWF` exactly.

**Each entry:**
1. Clean check (row D17 on failure).
2. Write-ahead `### <id> APPLYING <date -u>`: file, its `sha256sum`, the injection (the table's text), the classes run,
   the expected failing set copied from the table and "every other case passes".
3. `python -I -X utf8 $TL/u076_edits.py . mutate <id> > $WORK/logs/<id>-apply.txt; echo "mutate-exit=$?"` → 0 and
   `RESULT mode=mutate <id> mismatches=0`.
4. TR(`<id>`, `$NEW` `$OLD`), CASES into `$M/<id>-cases.txt`, and
   `python -I -X utf8 $TL/u076_sets.py $R <id> > $M/<id>-sets.txt; echo "sets-exit=$?"` (`--no-succeeded` under D12).
5. Restore at once, whatever the result: `cp $WORK/pristine/WorkManagerScheduler.kt $WMS`.
6. Clean check; append `run_exit`, the `RESULT` line of `u076_sets.py`, `verdict: detected | undetected | invalid (does
   not compile)`, and `restored: <date -u> sha256-ok diff-hash-ok porcelain-ok`.

A run's log with `exit=0` means nothing failed: `undetected`, row D18. A compile error (`e:` lines): `invalid`, row D18.

| Id | Injection (the exact texts are E3's `MUTATIONS`) | Complete failing set in `$NEW` (every other `$NEW` case and every `$OLD` case passes) | n | Under D12 |
|----|----------------------------------------------------|-------------------------------------------------------------------------------------------|---|-----------|
| M-1 | `scheduleBootup()`'s `return registerRegular("scheduleBootup", BOOT_BACKUP_DELAY_SECS)` → today's one-time `REPLACE` block (`:273-285`, verbatim) | T1, T2, T3 ("after scheduleBootup(): the entry is periodic"); T7, T8, T9 ("after the call: the entry is periodic"); T10 ("that item is periodic"); T19, T20, T21, T22 ("returned job") | 11 | 10 |
| M-2 | the `result.addListener(…)` line → a comment (the failed `UPDATE` is ignored again) | T4 to T9 ("after the call: the entry is periodic"); T13, T14, T15 ("the fallback ran and did not re-register") | 9 | 7 |
| M-3 | the registration path's `ExistingPeriodicWorkPolicy.UPDATE,` → `if (initialDelaySecs > 0) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.UPDATE,` (the boot path's first call) | T1 ("id (an in-place UPDATE keeps it)"); T7, T8, T9 ("fallback warnings": no `UPDATE` failure, so no warning) | 4 | 3 |
| M-4 | the "Never" block of `registerRegular` (six lines) → a comment | T16 to T21, T22, T23 ("returned job") | 8 | 8 |
| M-5 | `if (regularSecs <= 0) {` (the "Never" block) → `if (regularSecs < 0) {` | T17, T20 ("returned job") | 2 | 2 |
| M-6 | the "Never" block's `cancelRegular()` line removed | T18, T21, T22, T23 ("non-terminal entries under REGULAR") | 4 | 4 |
| M-7 | the fallback's `if (degraded \|\| !autoBackup \|\| regularSecs <= 0) {` → `if (false) {` | T13, T14, T15 ("periodic entries under REGULAR") | 3 | 3 |
| M-8 | `scheduleBootup()`'s `BOOT_BACKUP_DELAY_SECS` argument → `0L` (this plan's: a new boot registration loses its first-run delay; K-1's `initialDelaySecs` comment) | T2, T3 ("initialDelayMillis (the boot delay of a new registration)") | 2 | 2 |

`\|\|` is Kotlin's `||`, escaped for this table. Test ids T1 to T30: table 5.1. The label in quotes is the first line of
the failure message `u076_sets.py` requires. Every set was derived from the test text of E1 and the fixed text of 3.1
(the derivation per test is in 5.1's "Detected by" column); every injected file compiles (scratch compile, 14.5). The
guards (T24 to T30) pass under every injection; so does every `WorkManagerSchedulerTest` case (their boot test asserts
only "non-empty, not FAILED or CANCELLED", `:428-438`, which a one-time, a replaced or an updated entry all satisfy).
**Counted detections: M-1 to M-8 (8).** No injection needs a compiling replacement (all eight compile, L20).

### 4.9 Files for the orchestrator's commit (exact; the developer commits nothing)

- Code: `$WMS`, `$BS`, `$NEWF`.
- Workspace (`$W`): `implementation-log.md`; `evidence/tools/{u076_edits.py,u076_sets.py,workdb_rows.py,ui_find.py}`;
  `evidence/I0-{start.txt,gradle-version.txt,gates.txt,junit-totals.txt,jacoco-packages.txt,scheduler-classes.txt}`;
  `evidence/I1-checks.txt`; `evidence/I2-{red-cases.txt,red-sets.txt,red-messages.txt,probe-lines.txt}`;
  `evidence/I3-{checks.txt,green-cases.txt,green-sets.txt}`; `evidence/I4-pristine.txt`; `evidence/mutation-log.md`;
  `evidence/mutations/M-{1..8}-{cases,sets}.txt`; `evidence/I6-{gates.txt,junit-totals.txt,jacoco-packages.txt,scheduler-classes.txt,final-checks.txt}`;
  the AC-21 files of section 9 (the orchestrator's).
- Never staged: the probe (deleted), the story file, the sprint plan, other stories' workspaces, `compiled-pipeline.json`,
  `sdlc/.skill-context.json`, any contract, design or change record.

### 4.10 Sessions and hand-over

**One session**: 12 Gradle runs (two full gates of about ten minutes, likely in the background, and ten targeted runs
of `$NEW` with one to four other classes). Safe stopping points, never while a Gradle run is in flight: after I0; after
I2 (red evidence frozen, no production edit); after I4 (fix in place, pristine copies and hashes stored); between two I5
entries (each with its `restored` line); before I6. **Resume checks** (no Gradle): `git rev-parse HEAD` and
`git log --oneline $START..HEAD` (a commit by anyone is row D19); `git status --porcelain --untracked-files=all -- app`
as the stopping point leaves it; after I4: `sha256sum $WMS $BS` = `SW SB` and `PROD_DIFF` unchanged; no `APPLYING`
entry without `restored`; rebuild `$WORK/pristine/` from the working files only when the hashes equal `SW SB`. The
hand-over block in the implementation log gives: session end time, last finished step, next step, `START`, `C0`, `T0`,
`S0`, `SW SB`, `HW HB`, `PROD_DIFF`.

## 5. Tests and Counts

### 5.1 The 30 added tests (`WorkManagerSchedulerRegularBootTest`)

"Red" = fails on the unchanged scheduler at Start (= the old scheduler, S-3), at the named assertion. "Guard" = passes at
Start by design; not gate evidence for the change. "Detected by" = the injections whose derived set contains it.

| T | Test | AC (obligation) | At Start | Detected by |
|---|------|-----------------|----------|-------------|
| T1 | `boot_afterRegisteredRegular_keepsSamePeriodicEntry` | AC-1 (D-23 (1)) | Red: the entry is one-time (`REPLACE`, `:280-281`) | M-1, M-3 |
| T2 | `boot_nothingRegistered_registersPeriodic_wifiOnly` | AC-2 (D-23 (2)), INV-2 `UNMETERED`, INV-3 from the `WorkSpec` | Red: one-time | M-1, M-8 |
| T3 | `boot_nothingRegistered_registersPeriodic_anyNetwork` | AC-2, INV-2 `CONNECTED` | Red: one-time | M-1, M-8 |
| T4 | `regular_overOneTimeSeed_enqueued_registersPeriodic` | AC-3 (a), `scheduleRegular()` (D-23 (3)) | Red: `UPDATE` fails silently, the seed stays | M-2 |
| T5 | `regular_overOneTimeSeed_cancelled_registersPeriodic` | AC-3 (b) | Red: same | M-2 |
| T6 | `regular_overOneTimeSeed_succeeded_registersPeriodic` | AC-3 (c) (P-3) | Red: same | M-2 |
| T7 | `boot_overOneTimeSeed_enqueued_registersPeriodic` | AC-3 (a), `scheduleBootup()` | Red: a new one-time entry | M-1, M-2, M-3 |
| T8 | `boot_overOneTimeSeed_cancelled_registersPeriodic` | AC-3 (b) | Red: same | M-1, M-2, M-3 |
| T9 | `boot_overOneTimeSeed_succeeded_registersPeriodic` | AC-3 (c) | Red: same | M-1, M-2, M-3 |
| T10 | `inv1_regularAndBoot_sequence1_oneNonTerminalPeriodic` | AC-4 (D-23 (4)) | Red: the one non-terminal item is one-time | M-1 |
| T11 | `inv1_regularAndBoot_sequence2_oneNonTerminalPeriodic` | AC-4 | Red: the boot's one-time item stays (`UPDATE` fails) | — (see below) |
| T12 | `inv1_regularAndBoot_sequence3_oneNonTerminalPeriodic` | AC-4 | Red: no non-terminal item | — (see below) |
| T13 | `fallback_autoBackupOffBeforeFallback_enqueuesNothing` | AC-6 (a) | Red (stricter than the story): no fallback runs, so its "not re-registered" line is missing | M-2, M-7 |
| T14 | `fallback_degradedBeforeFallback_enqueuesNothing` | AC-6 (b) | Red: same | M-2, M-7 |
| T15 | `fallback_neverBeforeFallback_enqueuesNothing` | AC-6 (c) | Red: same | M-2, M-7 |
| T16 | `never_minusOne_registersNothing` | AC-8 (a), (d) (D-24 (1)) | Red: 900 s periodic, non-null job (`:180`, `:201`) | M-4 |
| T17 | `never_zero_registersNothing` | AC-8 (b), (d) | Red: same | M-4, M-5 |
| T18 | `never_afterRegistered_cancelsRegular` | AC-8 (c), (d) | Red: updated to 900 s | M-4, M-6 |
| T19 | `boot_never_minusOne_registersNothing` | AC-9 (a), (d), (e) (D-24 (2)) | Red: one-time boot entry | M-1, M-4 |
| T20 | `boot_never_zero_registersNothing` | AC-9 (b), (d), (e) | Red: same | M-1, M-4, M-5 |
| T21 | `boot_never_afterRegistered_cancelsRegular` | AC-9 (c), (d), (e) | Red: same | M-1, M-4, M-6 |
| T22 | `upgrade_neverSeed_boot_cancels` | AC-10 (D-24 (3)) | Red: replaced by a one-time entry | M-1, M-4, M-6 |
| T23 | `upgrade_neverSeed_regular_cancels` | AC-10 | Red: updated to 900 s | M-4, M-6 |
| T24 | `regular_belowMinimum_raisedTo900s` | AC-11 guard (60, 899, 900) | Guard | — |
| T25 | `regular_offeredValues_useOwnInterval` | AC-11 guard (1800 … 86400) | Guard | — |
| T26 | `never_incomingPathUnchanged` | AC-12 guard | Guard | — |
| T27 | `boot_degraded_afterRegistered_leavesItUnchanged` | AC-13 (a) guard | Guard | — |
| T28 | `boot_degraded_never_cancelsNothing` | AC-13 (b) guard | Guard | — |
| T29 | `boot_leavesContentTriggerUnchanged` | AC-13 (c) guard | Guard | — |
| T30 | `boot_autoBackupOff_cancelsAll` | AC-13 (d) guard | Guard | — |

**T11 and T12 under the injections.** With the boot path broken back to one-time (M-1), the fallback in
`scheduleRegular()` heals both sequences (sequence 2's last call, and sequence 3's last call after `cancelAll()`), so
they pass; with the fallback removed (M-2), no one-time entry is ever made in them. They are the evidence that the
fix's two halves together give one periodic item; their red run at Start is their detection (D-23: "each of (1) to (4)
fails on the scheduler code at `fd70325a`"). AC-20 requires each mutation to fail the tests it names and each new test
to be detected by at least one logged mutation where the code allows (ruling 40's reading, Record 18); T11, T12 and the
guards are the exceptions, stated.

**What each test asserts beyond the story's minimum** (so that an injection cannot pass by accident): AC-2's tests also
assert exactly one boot line and no fallback warning; AC-3's tests that the seed's id is gone and no one-time entry
remains; AC-6's tests that the seed and `CONTENT_TRIGGER` keep their id and state and that the skip line names the
closed gate; AC-8 and AC-9 that no `REGULAR`-tagged item is non-terminal under any name and that exactly one "Never"
line of the right method was written.

### 5.2 Red-run expectation (I2) and the green run (I3)

I2: 23 red, each at the first assertion that the old scheduler breaks (the labels of E4's `RED` set: T1 to T3 "after
scheduleBootup(): the entry is periodic"; T4 to T9 "after the call: the entry is periodic"; T10, T11 "that item is
periodic"; T12 "non-terminal REGULAR-tagged items"; T13 to T15 "the fallback ran and did not re-register (one line)"; T16
to T23 "returned job"); 7 guards green; 5 probe tests green; every `WorkManagerSchedulerTest` case green. I3: all 30 (or
28) green, and every case of `WorkManagerSchedulerTest` (25 at `fd70325a`), `BootReceiverTest`, `BackupTriggerWorkerTest`.

### 5.3 Counts (REQ-MODERNIZATION-015 AC-13 as amended by CR-016; AC-19)

`C0` at Start with its commit id; the end count `C0 + 30` (`C0 + 28` under D12), first with the working-tree form
(`git grep -h --untracked …`, stated so), then by the orchestrator with the story's commit. The difference is this
story's 30 tests, each listed by name (5.1). If anything else adds or removes a test between I0 and I6 (row D19), it is
listed by name and not counted. The probe's 5 tests are never in a count (deleted at I3, never committed).

## 6. Criterion-to-Step Map

| Criterion | Where it is met | Evidence |
|-----------|-----------------|----------|
| AC-1 | W5 (`UPDATE` in place); T1 | I2 red, I3 green, M-1, M-3 |
| AC-2 | W5 (`setInitialDelay(60)` for a new registration); T2, T3 | I2, I3, M-1, M-8 |
| AC-3 | W5 fallback; T4 to T9 | I2, I3, M-1, M-2, M-3; (c) by P-3 or "not tested" (D12) |
| AC-4 | W4 + W5; T10 to T12 | I2, I3, M-1 |
| AC-5 | W5; `grep -n OneTimeWorkRequest` (I6); T1; M-3; the `RUNNING` note (I2 step 5) | I6 final checks |
| AC-6 | W5 gate re-check; T13 to T15 | I3, M-2, M-7 (red at Start too) |
| AC-7 | Section 2.3; probe E2 | I2 probe lines; implementation log |
| AC-8 to AC-10 | W5's "Never" block; T16 to T23 | I2, I3, M-4, M-5, M-6 |
| AC-11 to AC-13 | Guards T24 to T30 | I2 green, I3 green, green under all injections |
| AC-14 | No existing test edited (`tracked-tests-unchanged-exit=0`); G-F | I6 |
| AC-15 | E3 `hunks`; `--name-only`; `must-be-empty-exit=0` | I3, I6 |
| AC-16 | W1 to W5, B1 to B4 (texts in E3) | QA reads the places (section 8.1 lists them) |
| AC-17 | Section 8.1 | QA |
| AC-18 | G0, G-F | I0, I6 |
| AC-19 | 5.3 | I0, I6, orchestrator |
| AC-20 | 4.8 | `mutation-log.md` |
| AC-21 | Section 9 (orchestrator) | `AC21-*` files |
| IC-1 | No signature change; callers grep | I0, I6 |
| IC-2 | `REGULAR` name, class, tag kept (the tests write `"REGULAR"` out, E1 `REGULAR` constant); `"BOOT"` grep empty | I6; T4 to T9 for one-time takeover; T1, T22, T23 for periodic |

## 7. Decision Table (stop routes)

| Row | Condition | Action |
|-----|-----------|--------|
| D1 | A predecessor (U-070, U-075, U-074, U-071, U-072) not `done`, or U-077 already `done` | Stop (S-1) |
| D2 | A tool's or test file's blob differs from the plan | Compare with the plan text and correct until equal; never run a variant |
| D3 | `s3-end` preceded by output (the scheduler at Start differs from `74b31759`) | Stop (S-3), with one pre-ruled exception asked for in SC-1: if the only difference is U-071's forced edit `@ApplicationContext` → `@param:ApplicationContext` on the constructor line (`:80`; U-071 plan, row D58), record it and continue. Anything else: stop; the orchestrator decides (story S-3) |
| D4 | `anchors-exit` not 0 (an anchor occurs 0 or 2+ times) | Stop: a re-anchoring amendment from the planner (P-5) |
| D5 | WorkManager not 2.9.1 | Stop (S-4) |
| D6 | A toolchain version other than 2.5 expects | Record; continue only if I2 compiles and G0 is green; else D10 |
| D7 | G0 or G-F not `exit=0`, failures or errors above 0, or no JUnit XML under `$R` | Stop; at I0 that is the predecessor's state. If the XML is elsewhere: point `$R` at it, record, continue |
| D8 | A coverage line missing in `jacoco_pkgs.py` output | Stop and report (the report path or format moved) |
| D9 | A Gradle log without a final `exit=` line | A cut-off run: stop. Inside an I5 entry: restore and clean check first, then stop |
| D10 | A compile error (`e:` lines) in I2 or I3 | Stop; report the error verbatim (the scratch compile used stubs and Robolectric 4.16.1) |
| D11 | A probe assertion starting `STOP` failed | Stop before any production edit; report the line (a contract sentence over-promises, P-1/P-2 stop conditions) |
| D12 | Probe `p3` failed at its `P-3` line, and T6, T9 failed at "precondition: the seed's state" | Not a stop: `u076_sets.py … RED --no-succeeded` must then match; at I3 step 1 delete T6 and T9 (3.2); AC-3 (c) "not tested: <the P-3 line>"; every later `u076_sets.py` call with `--no-succeeded` |
| D13 | Probe `p1a` failed at an `H-2` line | Record the reading for H-2; not a stop |
| D14 | `u076_sets.py` `does-not-match` in I2 or I3 for another reason | Stop; report its lines (a test defect is fixed only by a plan amendment) |
| D15 | A failure whose type is neither Truth's nor JUnit's assertion type | Stop: the red reason is not an assertion (story Terms) |
| D16 | The scheduler package below 0.60, or any floor or `@Ignore` changed | Stop; nothing is lowered |
| D17 | A clean check, porcelain or hash check fails | Stop; in I5 restore first |
| D18 | An injection undetected, invalid, or its observed set differs from the table | Record; stop and report (the table is the derived set; no adjustment) |
| D19 | A commit by anyone between I0 and I6, or a document changed by someone else | Record with its subject; a commit touching `app/` stops |

## 8. Contracts, Standards, Risks, Scope Concerns

### 8.1 Contract conformance (AC-17) and the close-out

Every row of K-1 (`:292`) and of "Affected here", mapped to the code that makes it true and the test or step that shows it:

| K-1 item | Code at the end (fixed lines) | Shown by |
|----------|-------------------------------|----------|
| "`scheduleRegular()` raises a non-positive interval to 15 minutes and registers a periodic backup (BUG-026)" — no longer true | `registerRegular` "Never" block `:287-292` | T16 to T18, T23; M-4, M-5, M-6; device (iii) |
| "`scheduleBootup()` enqueues a one-time `BackupWorker` under `REGULAR` with `REPLACE` …" — no longer true | `scheduleBootup` `:263-274` calls `registerRegular`; no `OneTimeWorkRequest` there | T1 to T3, T10; AC-5 grep; M-1, M-3, M-8; device (i) |
| "the next `scheduleRegular()` then fails silently on that entry (BUG-027)" — no longer true | `onRegularUpdateResult` `:333-359` | T4 to T9, T13 to T15; M-2, M-7; device (ii) |
| Affected: `scheduleRegular()` block (`:63-71`) | W4 + `registerRegular` | T4 to T6, T16 to T18, T24, T25; AC-16 (b), (f) |
| Affected: `scheduleBootup()` block (`:76-83`) | W5 | T1 to T3, T7 to T9, T19 to T22, T27 to T30; AC-16 (c), (f); "a running regular backup is not cancelled": L3 (read), not testable (AC-5 note) |
| Affected: boot bullet of the `BackupType` notes (`:162-163`) | No new unique name; boot uses `REGULAR` | T10 to T12 ("no other unique name holds one"), T19 to T21; IC-2 grep |
| Affected: `initialDelaySecs` boot comment (`:200-203`) | `setInitialDelay` only when positive (`:304-306`) | T2, T3 (60 000 ms); M-8; T1 (an existing one is updated in place) |
| Affected: `ExistingPolicy` note (`:221-224`) | `UPDATE` `:312`; `CANCEL_AND_REENQUEUE` `:356` only in the failure listener | T4 to T9, T13 to T15; M-2, M-3 |
| Affected: the `REGULAR` row (`:174`) | interval `maxOf`, network, backoff, tag, first-run delay, policy, "Never" cancel | T2, T3 (network, backoff, delay, tag), T16 to T25 |
| Affected: INV-1 realization (`:278`) | `UPDATE`; fallback only on failure; one work kind per name | T1, T4 to T12; M-1, M-2, M-3 |
| Affected: Rule 4 (`:319`) | 2.1 | T1 to T13, T19 to T22, T27 to T30 |
| Affected: Rule 4a (`:320`) | the "Never" block; the degraded gate first | T16 to T23, T24, T25, T28 |
| Affected: the regular example (`:397-411`) | `UPDATE`, fallback, "a value <= 0 registers nothing" | T2, T4, T16 |

Obligations D-23 (1) to (4) → T1, T2/T3, T4 to T9, T10 to T12; D-24 (1) to (3) → T16 to T18, T19 to T21, T22/T23; "each
fails on the scheduler code at `fd70325a`, and the story records that run" → I2 (Start's scheduler is that blob, S-3);
D-24's guards → T24, T25; citation item 3 → AC-14 (I6); INV-4 row → `inv4_*` and `BackupTriggerWorkerTest` green in I3 and
G-F, T26 and T29 as guards; the breaking-change policy ("MUST be gated by the INV-1..4 tests") → T1 to T12 in every
gate, M-1 to M-3.

**Close-out edit X-1** (CR-017 `:1203`): when the story is closed, the orchestrator deletes the line starting
`> - **K-1, until the story that fixes BUG-026 and BUG-027:**` from the contract's "Code not yet at v4" blockquote
(`:292`). Not a developer step. The blockquote stays (K-2 remains until U-077, X-2).

### 8.2 Scope Concerns (for the orchestrator; each with a recommended ruling)

- **SC-1 (S-3 and U-071's conditional edit).** U-071's plan may force `@ApplicationContext` → `@param:ApplicationContext`
  on `WorkManagerScheduler.kt:80` (its Risk 9, row D58). That would make S-3's `git diff --stat 74b31759 <start> -- scheduler/`
  non-empty, and the story then stops. The edit changes the annotation's use-site target only: the red runs on such a
  Start are runs on the old scheduler in every behaviour D-23 and D-24 name, and none of this plan's anchors include
  that line. **Recommended ruling:** if that one line is the only difference, S-3 is met; the developer records it
  (row D3). Any other difference stops as the story says.
- **SC-2 (device evidence of periodicity; AC-21 wording).** WorkManager 2.9.1 never registers a periodic JobScheduler
  job: every job, periodic work included, is a one-off with a minimum latency (L15). Its diagnostics have no periodicity
  column (L16). So AC-21 step (i)'s "the scheduler dump shows a periodic job for the regular backup" and step (iii)'s
  "the dump shows the periodic job again" cannot be met as worded, and the observation's reading "one one-time job with a
  60 s minimum latency … no periodic job" (`2026-11-21-background-backup-and-boot-observation.md`, Run 1) rests on the
  diagnostics, not on the dump (its conclusion stands: the diagnostics listed no `REGULAR` enqueued work). **Recommended
  ruling:** periodicity (and AC-1's "same id, generation + 1") is read from WorkManager's database copied off the
  emulator into the orchestrator's work folder (`run-as`, the debug build; the story's "optional, definitive" path, made
  the deciding read); the scheduler dump and the diagnostics are recorded but decide nothing (section 9). The
  orchestrator may add a dated note to the observation record.
- **SC-3 (the SUCCEEDED seed's worker class).** The story defines seed S1 as a one-time `BackupWorker`. It cannot reach
  `SUCCEEDED` in this harness (L19). T6 and T9 use the same request with a test-local plain `Worker`
  (`SucceedingSeedWorker`). `UPDATE`'s check reads only the entry's type and state, never its worker class (L2), and
  `REPLACE` deletes whatever is there (L5). **Recommended ruling:** accept; if refused, delete T6 and T9 as row D12 does and
  record AC-3 (c) "not tested: the seed's worker class".
- **SC-4 (the fallback can stop a running one-time entry).** Rule 4 (`:319`): "With a positive regular interval it MUST
  NOT cancel or reset a registered or running regular backup." The fallback's `CANCEL_AND_REENQUEUE` stops a one-time
  `REGULAR` entry that is `RUNNING` when the fixed build first registers (L5, L6): possible once per upgraded phone, if
  the old build's boot backup is running at that moment. The contract itself prescribes that fallback, and the entry is
  the earlier build's one-off, not the registered periodic backup; the run ends as U-074's "run cancelled" and its
  messages are taken by the next run (above the watermark). **Recommended ruling:** accept with no text change now; the
  housekeeping record of CR-017 Scope Concern 7 may add "(the one-off an earlier build left under `REGULAR` is replaced)".
- **SC-5 (stricter than the story, for the record).** AC-6's three tests are written at I1 and run red at Start (not only
  after the fix), because each asserts that the fallback ran and skipped; this makes them non-vacuous on the old code.
  M-8 is added. The device evidence of AC-1 (same id, generation + 1 across a restart) is added through SC-2's database
  read. **Recommended ruling:** accept.
- **SC-6 (story text corrections, no criterion changes).** (a) AC-2 and P-4 name `inv3_backoffPolicy_exponential_regularJobEnqueued`
  (`:283`) as a `WorkSpec` reader; it is not (2.3). (b) Device step (iii)'s "a registration with that interval and the
  dump shows the periodic job again": the registration line is `scheduleRegular: registered REGULAR (UPDATE) with
  interval=<n>s` after the fix (2.1). **Recommended ruling:** QA reads the criteria with these corrections; no amendment
  needed.

### 8.3 Risks

| # | Risk | Likelihood / impact | Mitigation |
|---|------|---------------------|------------|
| R1 | The probe shows a 2.9.1 behaviour different from the bytecode reading | Low / high | Stop before the fix (D11); the reading is L1 to L13 |
| R2 | A Robolectric 4.18.1 or Truth 1.4.5 difference breaks the compile or a label | Low / low | D10, D15; WorkManagerSchedulerTest and BackupTriggerWorkerTest use the same APIs and pass at Start |
| R3 | The SUCCEEDED barrier does not hold (the run finishes on another thread) | Low / low | D12: AC-3 (c) "not tested"; the type check comes first (L2), so (a) and (b) show the defect |
| R4 | Production race between the fallback and a `cancelAll()` posted between its gate read and its enqueue (2.4 item 9) | Very low / medium (a periodic backup after auto backup was switched off, until the next settings event or boot) | Re-check closes the named case; the window is two statements; only while a one-time entry exists (once per upgraded phone). Recorded, not closable without a blocking read |
| R5 | H-2: a never-run registration's first run moves at boot | Certain (read) / low | Recorded for the contract owner (section 10 item 3); after a restart the moved time is normally already past |
| R6 | `EncryptedPrefsSecretStore.isEncryptionDegraded()` called from WorkManager's serial thread | Low / low | It is already read off the main thread elsewhere (`BackupWorker`); not re-read here (not verified) |
| R7 | The device's quota or standby bucket holds jobs, so (iv) is not reached | High / none | (iv) is optional; "not reached" with the quota line |
| R8 | Copying the database misses the WAL file, so the read shows too little | Medium / high (a false "Never" pass) | Every `androidx.work.workdb*` file listed by `run-as … ls -l databases` is copied; a main file whose tables are only in the WAL makes `workdb_rows.py` exit 1 (14.9) |
| R9 | U-073 (2.12.0) changes the test helper's default executor mode or the internal names read by reflection | Medium / medium | Hand-over H-1 (section 10 item 2) names each dependency |
| R10 | The diagnostics worker is held by quota on a cold boot | High / none | Diagnostics decide nothing (SC-2) |
| R11 | A tool's text is mistyped when written from the plan | Low / medium | Blob checks (D2); the blobs come from the plan text (14.1) |
| R12 | A full gate is cut off by the harness | Low / medium | D9 |

### 8.4 Out of scope (each ruled or recorded)

The SMS-received receiver and `BackupScheduler.java:32-33` (U-077); the incoming-delay text (U-078); the content
trigger's period (BUG-025); the workers' cancellation reporting (U-074); the foreground change (U-075); any library
raise (U-073); PO-D; the README (read by QA, AC-16 (h)); logging the other six enqueue results; a cross-run guard.

## 9. AC-21 — Emulator Check (for the orchestrator; the developer runs no device step)

**Preconditions.** The developer's hand-off gives `FIXED_APK_SHA` (the debug APK at `app/build/outputs/apk/debug/app-debug.apk`
after G-F) and `START_APK_SHA` with the Start APK's place in the developer's session scratch folder; no mutation entry
lacks `restored`. The orchestrator copies the Start APK into its work folder `OW` (outside the repository) and checks
both SHA-256 values. Only the AVD `Pixel_9_Pro` is used; `Pixel_10_Pro` and any physical device are never touched.
Nothing but the app's own state changes: installing it with `install -r`, opening and leaving it, its regular schedule
setting in step (iii) (allowed and put back: Record 20, PO-G), cold starts of the emulator. No device setting,
permission, battery exemption or standby-bucket command; `dumpsys`, `getprop`, `wm size`, `run-as … ls/cat` and
`am get-standby-bucket` are read-only. The app is never uninstalled, force-stopped or cleared. Auto backup is on with an
account (left so by earlier checks; read from the log lines, not from the screen).

**Evidence rules.** No screenshot and no UI dump enters the repository, and no outcome rests on one (U-075 ruling
SC-1): UI dumps go to `OW` only, are read by `ui_find.py` (which prints coordinates or one label, nothing else) and are
deleted at the end. Raw logs, dumps and database copies stay in `OW`. Evidence in the repository: the sanitised lines
listed under "Record". An unobserved outcome is never a pass: a step that cannot be run is "not run: <reason>".

**Shell rules.** `ADB="$SDK/platform-tools/adb"` with `SDK="$(python -I -X utf8 sdlc/artifacts/build/sprints/sprint-024/U-068/evidence/tools/sdk_dir.py)"`;
`TL=sdlc/artifacts/build/sprints/sprint-025/U-076/evidence/tools`; `T=sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools`.
**`MSYS_NO_PATHCONV=1` only on the three `adb` commands with a `/sdcard/…` path, on that command only, never on Gradle**
(without it Git Bash rewrites `/sdcard/x` into a Windows path; `databases/…` is relative and passes unchanged: 14.10).
**No `timeout` above 540 s**; every call a foreground tool call with timeout 600000 ms; a wait on the device is
`"$ADB" -s $SER shell sleep <n>` with n ≤ 540. Device time: `"$ADB" -s $SER shell "date '+%m-%d %H:%M:%S.000'" | tr -d '\r'`
(written `$(dt)` below, typed out in each call). Python output is CRLF on this machine: values are read with
`tr -d '\r'` before `read` (14.8: `read -r X Y` keeps a CR on `Y` otherwise).

**Procedures used below.**
- **SER.** `"$ADB" devices | grep -E '^emulator-[0-9]+[[:space:]]+device$' | cut -f1 | while read -r s; do echo "$s $("$ADB" -s "$s" emu avd name | head -1 | tr -d '\r')"; done`;
  `SER` is the one serial named `Pixel_9_Pro` (none: ask the owner to start it; `Pixel_10_Pro` or two candidates: send
  nothing, ask). Then `"$ADB" -s $SER shell getprop ro.build.version.sdk` → `36`.
- **COLD.** `"$ADB" -s $SER emu kill`; start `"$SDK/emulator/emulator" -avd Pixel_9_Pro -no-snapshot-load -no-snapshot-save`
  with the tool's background mode (never `&`); select `SER` again; `timeout 540 "$ADB" -s $SER wait-for-device shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'; echo "exit=$?"`
  (124: repeat once, then "not run: no boot"); `BOOT_T=$(dt)`. Do not open the app, do not touch the screen.
- **DB(tag).** `mkdir -p "$OW/db-<tag>"; "$ADB" -s $SER shell run-as com.zegoggles.smssync ls -l databases > "$OW/db-<tag>/ls.txt"; for f in $(tr -d '\r' < "$OW/db-<tag>/ls.txt" | grep -oE 'androidx\.work\.workdb(-wal|-shm)?$'); do "$ADB" -s $SER exec-out run-as com.zegoggles.smssync cat "databases/$f" > "$OW/db-<tag>/$f"; done; python -I -X utf8 $TL/workdb_rows.py "$OW/db-<tag>" > "$OW/db-<tag>.txt"; echo "exit=$?"`.
  Exit 1 (`db=unreadable`): repeat once; then "not run: database not readable". The `SUMMARY` line is the reading.
- **JOBS(tag).** `"$ADB" -s $SER shell dumpsys jobscheduler > "$OW/jobs-<tag>.txt"; grep -c "com.zegoggles.smssync" "$OW/jobs-<tag>.txt"`
  (recorded with the app's job lines; decides nothing, SC-2).
- **DIAG(tag).** `D0=$(dt); "$ADB" -s $SER shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS -p com.zegoggles.smssync; timeout 90 "$ADB" -s $SER logcat -T "$D0" -m 1 -e 'Enqueued work' > /dev/null; echo "exit=$?"; "$ADB" -s $SER logcat -d -T "$D0" -s WM-DiagnosticsWrkr > "$OW/diag-<tag>.txt"`
  (124: "diagnostics not run (held)"; recorded only).
- **LOG(tag, since).** `"$ADB" -s $SER logcat -d -v threadtime -T "<since>" > "$OW/log-<tag>.txt"` (or without `-T` for
  "since boot"). The counts, each a separate `grep -c` on that file:
  `BOOTUP` = `' I SMSBackup\+: bootup'`;
  `BOOT` = `' D SMSBackup\+: WorkManagerScheduler\.scheduleBootup: registered REGULAR \(UPDATE\) with interval=[0-9]+s, firstRunDelay=60s'`;
  `FALLBACK` = `' W SMSBackup\+: WorkManagerScheduler\.(scheduleRegular|scheduleBootup): UPDATE failed on REGULAR .*re-registering with CANCEL_AND_REENQUEUE'`;
  `NEVER_R` = `' D SMSBackup\+: WorkManagerScheduler\.scheduleRegular: skipped \(regular schedule is Never'`;
  `NEVER_B` = `' D SMSBackup\+: WorkManagerScheduler\.scheduleBootup: skipped \(regular schedule is Never'`;
  `OLDBOOT` = `'scheduleBootup: enqueued bootup REGULAR with 60s delay'` (the Start build's text);
  `PRLOST` = `'tryEmitEvent\(AutoBackupSettingsChanged\) returned false'`;
  `REG` = the last match of `grep -oE 'WorkManagerScheduler\.scheduleRegular: registered REGULAR \(UPDATE\) with interval=[0-9]+s'`.
- **UI(label).** `MSYS_NO_PATHCONV=1 "$ADB" -s $SER shell uiautomator dump /sdcard/u076-ui.xml`;
  `MSYS_NO_PATHCONV=1 "$ADB" -s $SER exec-out cat /sdcard/u076-ui.xml > "$OW/ui.xml"`;
  `MSYS_NO_PATHCONV=1 "$ADB" -s $SER shell rm /sdcard/u076-ui.xml`;
  `python -I -X utf8 $TL/ui_find.py "$OW/ui.xml" text "<label>" > "$OW/ui.txt"; echo "exit=$?"; read -r X Y < <(tr -d '\r' < "$OW/ui.txt")`;
  exit 0: `"$ADB" -s $SER shell input tap $X $Y; "$ADB" -s $SER shell sleep 2`. `found=0` once: one swipe up
  (`SZ=$("$ADB" -s $SER shell wm size | tr -d '\r' | grep -oE '[0-9]+x[0-9]+' | tail -1); W=${SZ%x*}; H=${SZ#*x}; "$ADB" -s $SER shell input swipe $((W/2)) $((H*8/10)) $((W/2)) $((H*3/10))`)
  and repeat; still not found or `found=2`: "(iii) not run: <label> not found" (no coordinate is guessed).

**(i) A cold restart keeps the regular backup (AC-1, AC-5 on the device; K-1 second clause).**
1. SER. `sha256sum app/build/outputs/apk/debug/app-debug.apk | cut -c1-64` = `FIXED_APK_SHA`; `D1=$(dt)`;
   `"$ADB" -s $SER install -r app/build/outputs/apk/debug/app-debug.apk` → `Success` (else stop, "not run").
2. `timeout 120 "$ADB" -s $SER logcat -T "$D1" -m 1 -e 'WorkManagerScheduler\.scheduleRegular: (registered REGULAR|UPDATE failed on REGULAR|skipped)' > /dev/null; echo "exit=$?"`
   (the package-replaced path); then `"$ADB" -s $SER shell am start -n com.zegoggles.smssync/.activity.MainActivity`,
   `sleep 3`, `input keyevent KEYCODE_HOME`. LOG(i-install, `$D1`); DB(i-before).
3. COLD. `timeout 120 "$ADB" -s $SER logcat -m 1 -e 'WorkManagerScheduler\.scheduleBootup: registered REGULAR' > /dev/null; echo "exit=$?"`;
   `"$ADB" -s $SER shell sleep 150` (more than two minutes after `BOOT_COMPLETED`); LOG(i-boot, since boot); DB(i-after);
   JOBS(i); DIAG(i).
- **Pass:** `BOOTUP` ≥ 1 and `BOOT` = 1 in `log-i-boot`, with the interval equal to the setting (7200 s at the default);
  `FALLBACK` = 0 there; `db-i-after` `SUMMARY` has `regular-entries=1 regular-periodic=1 regular-onetime=0
  regular-nonterminal-periodic=1 tagged-regular-elsewhere=0`, and its `REGULAR` `ENTRY` has the `id8` of `db-i-before`'s
  and `generation` one higher (an in-place `UPDATE`; a run between the two reads changes neither). An `INCOMING` entry
  that was pending is allowed and named (CR-017 Scope Concern 10).
- **Fail:** a one-time `REGULAR` entry, no `REGULAR` entry, a different `id8`, or `OLDBOOT` above 0.
- **Not run:** `db-i-before` has no periodic `REGULAR` entry: "(i) not run: no registration before the restart", with the
  `SUMMARY` line (creating one through the settings screen is step (iii)'s action and is not done here).

**(ii) A phone the earlier build left without a regular backup gets it back (K-1 third clause; AC-3 on the device).**
1. `sha256sum "$OW/start-app-debug.apk" | cut -c1-64` = `START_APK_SHA`; `"$ADB" -s $SER install -r "$OW/start-app-debug.apk"`
   → `Success`. COLD. `timeout 120 "$ADB" -s $SER logcat -m 1 -e 'scheduleBootup: enqueued bootup REGULAR with 60s delay' > /dev/null; echo "exit=$?"`;
   DB(ii-broken). Expected: `regular-onetime=1 regular-periodic=0` (the BUG-027 state; observation Run 1). Otherwise
   "(ii) not run: the earlier build's state was not produced", with the `SUMMARY` line.
2. Without opening the app: `D2=$(dt)`; `"$ADB" -s $SER install -r app/build/outputs/apk/debug/app-debug.apk` → `Success`;
   `timeout 120 "$ADB" -s $SER logcat -T "$D2" -m 1 -e 'WorkManagerScheduler\.scheduleRegular: (registered REGULAR|UPDATE failed on REGULAR)' > /dev/null; echo "exit=$?"`;
   `"$ADB" -s $SER shell sleep 5`; LOG(ii-heal, `$D2`); DB(ii-healed).
- **Pass:** `FALLBACK` = 1 in `log-ii-heal` (or 0 with a `REG` line when `db-ii-broken` showed the one-time entry already
  gone), and `db-ii-healed` has `regular-entries=1 regular-periodic=1 regular-onetime=0 regular-nonterminal-periodic=1
  tagged-regular-elsewhere=0`.
- **Package-replaced event lost:** the wait ended 124 and `PRLOST` ≥ 1 (or no `now installed version` line): record it;
  COLD once more; LOG(ii-boot, since boot); DB(ii-boot). Pass then: `FALLBACK` = 1 for `scheduleBootup` and the same
  `SUMMARY` (AC-3's boot case).
- **Fail:** a one-time `REGULAR` entry remains, or no periodic one.

**(iv) Optional, run right after (ii): the new registration's first run starts off screen and succeeds.**
`timeout 450 "$ADB" -s $SER logcat -T "$D2" -m 1 -e 'BackupWorker\.doWork: starting backup, type=REGULAR' > "$OW/iv-start.txt"; echo "exit=$?"`
(a second 450 s wait anchored at the same `$D2` only after 124); then
`timeout 300 "$ADB" -s $SER logcat -T "$D2" -m 1 -e 'Worker result [A-Z]+ for Work .*REGULAR' > "$OW/iv-result.txt"; echo "exit=$?"`;
LOG(iv, `$D2`); `grep -cE ' W SMSBackup\+: BackupWorker: foreground start not allowed' "$OW/log-iv.txt"` and
`grep -cE ' E WM-WorkerWrapper: .*ForegroundServiceStartNotAllowedException' "$OW/log-iv.txt"` (U-075 plan section 9:
the first is a refused-and-continued request, which an automatic run never makes after U-075; the second is BUG-029's
defect). **Pass:** a start line, `Worker result SUCCESS … REGULAR`, both refusal counts 0. **Stopped by the system**
(neither pass nor fail): `I SMSBackup+: BackupWorker: run cancelled` (U-074's line) and no result line; repeat once.
**Not reached:** no start within the two waits and JOBS(iv) shows the app's job with `WITHIN_QUOTA` unsatisfied: "not
reached: held by the system's job quota (BUG-025)" with that line. **Fail:** a result other than SUCCESS, or a refusal
count above 0.

**(iii) "Never" schedules nothing, and a restart under "Never" registers nothing (AC-8, AC-9; PO-1 "no").** This step
changes an app setting and restores it (Record 20).
1. `"$ADB" -s $SER shell am start -n com.zegoggles.smssync/.activity.MainActivity`; `sleep 3`. UI("Auto backup settings");
   UI("Regular schedule"); then a dump without a tap: `python -I -X utf8 $TL/ui_find.py "$OW/ui.xml" checked > "$OW/ui.txt"; echo "exit=$?"`
   → `checked=<label>`: `NOTED=<label>` (expected `2 h`; map: `30 mins` 1800, `1 h` 3600, `2 h` 7200, `6 h` 21600, `24 h`
   86400). `D3=$(dt)`; UI("Never"); `"$ADB" -s $SER shell sleep 3`; LOG(iii-never, `$D3`); DB(iii-never).
   - **Pass (a):** `NEVER_R` = 1; `db-iii-never` has `regular-nonterminal=0 tagged-regular-nonterminal=0
     content-trigger-nonterminal=1` (the settings path cancels all and registers the trigger again, `App.java:373-385`).
2. `input keyevent KEYCODE_HOME`; COLD; `timeout 120 "$ADB" -s $SER logcat -m 1 -e 'scheduleBootup: skipped \(regular schedule is Never' > /dev/null; echo "exit=$?"`;
   LOG(iii-boot, since boot); DB(iii-boot).
   - **Pass (b):** `BOOTUP` ≥ 1, `NEVER_B` = 1, `BOOT` = 0; `db-iii-boot` has `regular-nonterminal=0 tagged-regular-nonterminal=0`.
3. **Restore.** Open the app as in 1; UI("Auto backup settings"); UI("Regular schedule"); `D4=$(dt)`; UI("$NOTED");
   `sleep 3`; LOG(iii-restore, `$D4`); DB(iii-restored).
   - **Pass (c):** `REG` ends `interval=<the noted value>s`; `db-iii-restored` has `regular-nonterminal-periodic=1` with
     `interval_ms` = the noted value × 1000.
   - The step is not complete until the setting is restored. If it cannot be restored, stop and tell the owner.
4. `rm -f "$OW/ui.xml" "$OW/ui.txt"`.

**Record.** `$E/AC21-emulator.txt`, written in `OW` first and copied through `python -I -X utf8 $T/sanitize.py <in> <out>`
(it refuses e-mail-like text and profile paths): UTC time; AVD name and SDK level; the two APK SHA-256 values; each
install result; `BOOT_T` of each cold start; each step's counts and `SUMMARY` lines; the `ENTRY` lines of `REGULAR`;
`NOTED`; the job counts and whether diagnostics ran; each step's outcome. The database readings `$E/AC21-db-<tag>.txt`
are the `workdb_rows.py` outputs through `sanitize.py` (ids cut to 8 characters by the tool). The log extract
`$E/AC21-log.txt` is `python -I -X utf8 $T/sanitize.py "$OW/log-<tag>.txt" <out> --grep 'SMSBackup\+|WM-WorkerWrapper|WM-DiagnosticsWrkr'`
for each tag, concatenated by the orchestrator in `OW` first. Nothing raw enters the repository.

**What it proves:** on Android 16, a restart keeps the registered regular backup in place (same id), a phone in the
BUG-027 state is healed by the package-replaced path (or the next boot), "Never" registers nothing in both paths, and the
setting returns. **What it does not prove:** a restart while a regular backup is running; Android 11 to 15; the owner's
phones; the first run's timing after boot (quota and standby govern it, R7); the race of 2.4 item 9.

## 10. Items for the Orchestrator (hand-over)

1. **Commit after the reviews** (the developer commits nothing): the 4.9 list. Then with the commit: `git diff --name-only
   <start> <commit> -- app/src/main` (two files), the IC-1 and IC-2 greps with `<commit>`, and the count with the commit
   (AC-19).
2. **H-1 (U-073, WorkManager 2.12.0).** These tests depend on library internals and test-helper behaviour that U-073's
   plan must re-read for 2.12.0: (a) the reflective read in T2/T3 (`WorkManagerImpl.getWorkDatabase()`,
   `WorkDatabase.workSpecDao()`, `WorkSpecDao.getWorkSpec(String)`, `WorkSpec.backoffPolicy`, `backoffDelayDuration`: the
   same names U-069's helper uses; U-069's plan says they are public in 2.12.0); (b) `initializeTestWorkManager(context,
   config)` defaulting to the synchronous task executor (L8): if 2.12.0 changes the default, T4 to T9 and T13 to T15 would
   see the fallback after the call returns; (c) the `TestDriver` and default-factory path of T6/T9 (L12, L13, L19); (d)
   `WorkInfo.getGeneration`; (e) the type check before the state check and `CANCEL_AND_REENQUEUE`'s mapping (L1, L2, L5;
   BUG-027 read them in 2.12.0); (f) P-1's facts (H-1 of the story). A failure of these tests across the raise is a
   library-behaviour question first. The probe text (E2) can be reused as U-073's own throwaway probe.
3. **H-2 (contract owner).** P-1 (a): `UPDATE` keeps the id, `lastEnqueueTime`, `periodCount`, `runAttemptCount` and the
   state, and raises the generation by one (L3); but the stored initial delay is the new request's, so a registration that
   **has not run yet** gets its first run at `lastEnqueueTime` + the new delay (L7): the boot path moves it to + 60 s, a
   later `scheduleRegular()` back to + 0. The `scheduleBootup()` block (`:80-82`) says "UPDATE keeps an existing
   registration (its id and schedule; …)". True for a registration that has run; for a never-run one the first-run time
   is recomputed. Possible wording: "(its id, and its schedule once it has run; …)". Not a defect of this story
   (story H-2); the probe line `P-1a boot:` gives the numbers.
4. **Log texts.** The texts "enqueued REGULAR with interval=" and "enqueued bootup REGULAR with 60s delay" no longer
   exist after this story. They are grepped by BUG-027 Steps to Reproduce B (steps 1, 5, 8), BUG-026 Steps to Reproduce,
   U-075's plan section 9 step (ii).3 (run before this story), and possibly U-073's device steps. Later device checks
   use `scheduleBootup: registered REGULAR (UPDATE)` and `scheduleRegular: registered REGULAR (UPDATE)`.
5. **U-077.** Both stories edit `BackupScheduler.java`. This story edits only `:36` and the javadoc lines of
   `scheduleRegular()` (`:58-66`), `scheduleContentTrigger()` (`:71-72`) and `scheduleBootup()` (`:83-90`); after it the file
   has 197 lines and **`:32-33` ("All call sites (BackupBroadcastReceiver, BootReceiver, SmsBroadcastReceiver,
   SmsBackupService, App) are routed through this port.") is unchanged at the same lines**, so U-077's AC-4 anchor holds.
   U-077's planner re-reads the file at its own start (CR-017, "Order between the two stories"). U-077 must not touch
   `:36` or the javadoc blocks this story wrote.
6. **SC-1 to SC-6** (8.2): rulings asked for before the build; SC-1 and SC-2 change what the developer and the orchestrator
   do (rows D3; section 9).
7. **X-1** at the close (8.1). Then the contract's "Code not yet at v4" keeps only K-2.
8. **Story text corrections** for QA (SC-6).
9. **Observation record** (SC-2): a dated note may say that the scheduler dump cannot show periodicity in WorkManager 2.9.1.

## 11. Constraints for the Developer (binding)

- Change only `$WMS`, `$BS` (both through E3) and add `$NEWF`; the probe exists only between I1 and I3 step 0.
- No edit of an existing test; no `@Ignore`; no floor, dependency or build-file change; no device step; no commit.
- No `diff` or `cmp`; comparisons by `git diff --no-index`, `git hash-object` or `sha256sum`. Modest shell activity. A
  vanished tool or a process killed without a visible reason: stop and report; restore nothing from quarantine, add no
  exclusion.
- Never `MSYS_NO_PATHCONV` in a Gradle shell; never two Gradle processes; never poll a background run.
- A stop row stops; nothing is adjusted to make a check pass.
- Evidence: through `sanitize.py`; no e-mail address, host name, user name or profile path.

## 12. Quality Attribute Coverage (ISO/IEC 25010)

| Characteristic | Finding |
|----------------|---------|
| Functional Suitability | Correctness: the regular schedule the settings show survives restarts (AC-1, AC-2), heals an affected phone (AC-3), and "Never" means none (AC-8 to AC-10). Completeness: every K-1 clause and "Affected here" item mapped (8.1). Appropriateness: the boot path translates both legacy branches (`BackupJobs.java:86-97`) |
| Performance Efficiency | Time behaviour: no added I/O on the caller's thread (no blocking read); one listener per registration. Resource use: no extra boot backup a minute after every restart (PO-2 "no"); no 15-minute backups under "Never" (BUG-026's battery and data cost removed) |
| Compatibility | Co-existence with persisted work of earlier builds: periodic entries updated in place, one-time entries replaced once (IC-2). Interoperability: the unique name, tag and worker class unchanged; no new name |
| Usability | The schedule shown is the schedule run; "Never" stops regular runs; incoming backups unchanged (AC-12). No new UI |
| Reliability | Fault tolerance: a failed `UPDATE` no longer fails silently (one warning, a repair). Recoverability: self-healing at the next settings event or boot. Residual: R4 (narrow race), SC-4 (a one-time run stopped once on upgrade). Maturity: 30 tests, 8 injections |
| Security | No new permission, component or exported surface; the encryption-degraded gate stays first on both paths (U-054); log lines carry no message content or account data; the device evidence is sanitised and database copies stay outside the repository |
| Maintainability | Modularity: one registration path; Analysability: four fixed log prefixes; Testability: deterministic tests without timed waits (2.2); Modifiability: each rule is one edit (the injections show it) |
| Portability | Adaptability: WorkManager behaviour pinned for 2.9.1 with the 2.12.0 dependencies listed (H-1); no API-level branch; Robolectric SDK 29 as the harness |

## 13. Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| U-076 story | `sdlc/artifacts/build/sprints/sprint-025/U-076-regular-backup-survives-a-restart-and-never-schedules-nothing.md` | Read in full |
| BUG-027 | `sdlc/artifacts/stories/BUG-027-regular-backup-removed-by-the-boot-backup.md` | Description, Steps, Evidence, Suggested Approach, Notes, Not verified |
| BUG-026 | `sdlc/artifacts/stories/BUG-026-regular-schedule-never-still-schedules-a-backup.md` | Description items, AC-3, Suggested Approach (grep and sections) |
| Observation | `sdlc/artifacts/build/research/2026-11-21-background-backup-and-boot-observation.md`; its app log `sdlc/artifacts/build/research/evidence/2026-11-21-background-backup-app-log.txt` | Read in full; real log formats |
| CNTR-MODERNIZATION-004 v4 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` | `:1-125`, `:155-230`, `:270-462` |
| DES-MODERNIZATION-005 | `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` | `:376-383`, `:438-440`, `:460`, `:500` |
| CR-017 | `sdlc/artifacts/build/change-records/CR-017-cntr-004-scheduler-never-boot-example-and-receiver.md` | `:480-541`, `:1199-1207`, `:1565-1580`, `:1680-1720` |
| Decision appendix | `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` | Records 18, 20, 21, 22, 23 (`:2409-2555`) |
| U-075 plan | `sdlc/artifacts/build/sprints/sprint-024/U-075/plan.md` | Form; `:1-125`, `:1524-1660`, `:1784-1876`, `:2052-2420` |
| U-069 plan | `sdlc/artifacts/build/sprints/sprint-024/U-069/plan.md` | Amendment A4 and the reflective helper (`:103-130`, `:960-1000`) |
| U-071 plan | `sdlc/artifacts/build/sprints/sprint-025/U-071/plan.md` | Section 2.1 (`:340-407`); the Risk 9 forced edits (`:872-892`) |
| U-070 plan | `sdlc/artifacts/build/sprints/sprint-024/U-070/plan.md` | File list (section 3 headings); scheduler mentions (grep) |
| U-074, U-072, U-077 stories; sprint-025 plan | `sdlc/artifacts/build/sprints/sprint-025/` | U-074's "run cancelled" line and file scope; U-072's Affected Code; U-077's javadoc lines; the order |
| Source at `fd70325a` (via `git show HEAD:` / `git grep HEAD`, equal blobs) | `scheduler/WorkManagerScheduler.kt` (full), `scheduler/BackupScheduler.java` (`:20-110`), the other four `scheduler/*.java` (full, for the compile), `App.java` (`:270-300`, `:355-392`), `receiver/BootReceiver.java` (`:20-40`), `receiver/PackageReplacedReceiver.java` (`:15-32`), `service/BackupWorker.kt` (grep, `:577-640`), `preferences/Preferences.java`, `SecretStore.java`, `DataTypePreferences.java`, `Consts.java`, `BackupType.java` (signatures), `res/values/arrays.xml` (`:70-95`), `res/xml/preferences.xml` (`:170-200`), `res/values/strings.xml` (grep) | The code changed and its neighbours |
| Tests at `fd70325a` | `service/WorkManagerSchedulerTest.kt` (full), `worker/BackupTriggerWorkerTest.kt` (`:1-40`, `:250-295`); grep of callers in `app/src/test` | Harness, `ShadowLog` use |
| Build files at `fd70325a` | `app/build.gradle` (`:60-72`, `:100-114`, `:116-164`, `:190-212`, `:520-548`), `gradle/gradle-daemon-jvm.properties` | Versions, floors, compiler flags |
| U-069 evidence | `sdlc/artifacts/build/sprints/sprint-024/U-069/evidence/I6-jacoco-packages.txt`, `I6-cases.txt`, `I2-red-messages.txt` | Real coverage lines, case lines, JUnit message format |
| U-063, U-068, U-069 tools | `sanitize.py`, `junit_totals.py`, `junit_cases.py`, `jacoco_pkgs.py`, `sdk_dir.py`, `jacoco_classes.py` | Usage lines and code read |
| Library artifacts (local Gradle cache; `javap -c -p`) | `work-runtime-2.9.1.aar` (`WorkManagerImpl`, `WorkerUpdater`, `OperationImpl`, `EnqueueRunnable`, `CancelWorkRunnable`, `Processor`, `WorkSpec$Companion`, `WorkInfo`, `AbstractFuture`, `Configuration`, `Worker`, `SystemJobInfoConverter`, `DiagnosticsWorker(Kt)`, `WorkDatabase_Impl$1`, `WorkDatabasePathHelper(Kt)`), `work-testing-2.9.1.aar` (`WorkManagerTestInitHelper` and `$1`, `$ExecutorsMode`, `TestWorkManagerImplKt` and its task executor, `SynchronousSerialExecutor`, `SynchronousExecutor`, `TestScheduler`, `InternalWorkState`, `TestDriver`) | L1 to L17 |
| Scratch compile | `kotlin-compiler-embeddable` 2.4.21 and its runtime jars; `android-37.0/android.jar`; Mockito 5.24.0, JUnit 4.13.2, Hamcrest 1.3, Truth 1.4.4, Guava 33.0.0, Robolectric 4.16.1 (and its `junit` module), `listenablefuture` 1.0, `lifecycle-livedata-core` 2.6.2, `lifecycle-common` 2.6.2, `annotation-jvm` 1.8.1, `javax.inject` 1 | Compile check |
| Template and standard | plugin cache `templates/build-artifacts/build-plan.md`; U-075's Phase Completion Report form | Frontmatter, sections, report |

## 14. Self-Test of the Plan's Commands (ruling 73)

Run on 2026-11-21 in this session's scratch folder, outside the repository, with no Gradle, no device and no read of
the working tree under `app/`. **Every tool and test file tested is the copy extracted from this plan file** (14.1).
Inputs: the `fd70325a` blobs (`git cat-file blob HEAD:<path>`, equal at `fd70325a`), a scratch git repository with
`core.autocrlf=true` whose working tree is CRLF like the developer's, constructed samples in the formats read (JUnit XML
with the real `message` form of U-069's evidence; a SQLite database built from WorkManager 2.9.1's own `CREATE TABLE`
strings; uiautomator XML; threadtime log lines in the form of the real observation log, with CRLF). Facts of this
machine found while testing: Python writes CRLF to a redirected stdout; `$?` after a pipeline is the last command's; the
Write tool turns `\uXXXX` escapes into the characters; `ls` is aliased to `ls -F`, so listings use `command ls -1`.

**Partial (stopped before completion).** Rows run so far, all on copies extracted from this plan file unless stated:

| # | Command as it stands in the plan | Input | Expected | Observed |
|---|----------------------------------|-------|----------|----------|
| 14.1 | Blob of every embedded file: first fenced block after its marker line, LF, SHA-1 of `blob <n>\0<data>` and `git hash-object --no-filters` | this plan file | E1 to E6 and E1V as declared in their marker lines | All seven equal; the two hash methods agree |
| 14.2 | `python -I -X utf8 $TL/u076_edits.py . start`, `apply`, `fixed`, `mutants` | the `fd70325a` blobs (LF) | `mismatches=0` each; blobs `3d4fe760…`, `bbb2945f…` | As expected; a second `apply` on the fixed tree refuses (`mismatches=9 written=0`, exit 1) |
| 14.3 | The same, plus `git hash-object $WMS $BS`, `hunks`, `--numstat`, porcelain, `grep -n OneTimeWorkRequest`, `git grep -n '"BOOT"'` | scratch clone with `core.autocrlf=true` (CRLF working tree, 481 CR) | `eol=crlf`; the LF blobs of 14.2; `hunks=21 mismatches=0`; `21 8`, `117 44`; ` M` twice; lines 154 382 423 458; exit 1 | As expected |
| 14.4 | `mutate M-1` to `M-8`, restore by `cp`, `sha256sum` cut to 64 characters | the CRLF clone | each changes the file; each restore equals the saved SHA-256 | As expected; unknown id, bad mode, missing root exit 2; `hunks` on an empty diff exit 1; on a diff with an out-of-place hunk and a code line in the port exit 1 with 3 findings |
| 14.4b | `$?` after a pipeline | the tool piped into `tail -1`, then `echo $?` | — | gives `tail`'s 0, not the tool's: every exit in the plan is taken from a command writing to a file |
| 14.5 | Scratch compile (Kotlin 2.4.21, stubs; the compiled originals are byte-equal to E1 to E3) | fixed scheduler; M-1 to M-8; E1 with the fixed scheduler; E1 and E2 with the Start scheduler | exit 0 | exit 0 for all; one deprecation warning (`RuntimeEnvironment.application`) |
| 14.6 | `grep -cE '^### [^ ]+ APPLYING '` and `grep -cE '^(- )?restored: '` | constructed log with prose that mentions both words | 3 and 2 | 3 and 2 |
| 14.7 | `workdb_rows.py <folder>` (scratch original, same text as E5) | SQLite databases built from 2.9.1's own `CREATE TABLE` strings: healthy, the BUG-027 state, a REGULAR-tagged item under another name; a main file without its WAL; an empty folder; a text file | the three SUMMARY lines; `db=unreadable` exit 1 for the last three | As expected (the main file alone has no tables: refused, not a false zero) |
| 14.8 | `ui_find.py` (scratch original, same text as E6); `read -r X Y` | constructed dumps (a label, a checked item, an address node, two matches, broken XML, empty, missing) | coordinates or `checked=2 h`; `found=n` or `xml=unreadable` exit 1; the address never printed | As expected; reading the output file directly leaves a CR on `Y`, hence `read -r X Y < <(tr -d '\r' < file)` |
| 14.9 | Git Bash path conversion (python.exe printing its arguments) | `databases/androidx.work.workdb`, `/sdcard/x.xml`, `-T "11-21 09:46:38.000"`, the regexes | only the `/sdcard/` path rewritten without `MSYS_NO_PATHCONV=1` | As expected |
| 14.10 | `timeout 2 sleep 5 && capture; echo exit=$?` | — | `exit=124`, capture skipped | As expected |
| 14.11 | The LOG greps of section 9; `wm size` and `run-as ls -l` parsing | constructed threadtime lines with CRLF; `wm size` with an override line; `ls -l`; an empty file | each count 1; `REG` 3600s; the swipe from the last size; the three database names; none from the empty file | As expected |
| 14.12 | I0's git commands at `PLAN_HEAD` | the repository (read only) | count 771; `s3` empty; blobs `0194308c…` and `17522557…`; two `@Ignore` lines; callers `App.java:377`, `BootReceiver.java:33` | As expected |

Not yet run: `u076_sets.py` (E4) on constructed JUnit XML (written, not tested), `junit_cases.py` on constructed XML, `sanitize.py` on samples, the hygiene scan of this file. These must be run before the plan goes to its gates.

Not testable here, and why: every Gradle step (the real compile on Gradle's classpath, the tests' run-time behaviour,
JaCoCo's numbers), every `adb` and emulator step against a device, and the real formats of `dumpsys jobscheduler`,
`run-as … ls -l` and `uiautomator dump` on `Pixel_9_Pro` (constructed samples only; their stop routes are the "not run"
outcomes of section 9). A later finding in those that can only cause a false stop is handled by the stop-and-amend
route (ruling 65).

## Verdict

**NEEDS_INFO (draft).** The text below was drafted before the stop; it states the intended verdict once section 14 is complete and the scope concerns are ruled, and is not a verdict of this draft.

- **The fix is the contract's shape:** one registration path for both port methods; `UPDATE` first; on a failed
  `Operation`, a gate re-check and `CANCEL_AND_REENQUEUE` with the same request; "Never" registers nothing and cancels;
  the gates first and unchanged; the boot path never builds a one-time request.
- **P-1 is settled from the 2.9.1 bytecode** with no stop condition met: `UPDATE` keeps the id, `lastEnqueueTime`,
  `periodCount` and a running worker and raises the generation by one; it fails on a one-time entry in every state,
  only through the `Operation`; `CANCEL_AND_REENQUEUE` (the periodic `REPLACE`) deletes any entry and inserts the
  periodic one. `cancelUniqueWork` stops a running worker, which Rule 4a requires under "Never". One finding goes to the
  contract owner (H-2: a never-run registration's first run moves).
- **P-2 is settled:** in the harness the operation completes and the direct-executor listener runs inside the scheduler
  call (synchronous task and serial executors, read in work-testing 2.9.1), so the tests read the result right after the
  call; no timed wait exists anywhere. AC-6 is a real test, not a reading.
- **P-3:** reachable with a plain worker and a completion barrier on the harness's single worker thread (SC-3); the
  fallback route is defined (D12). **P-4:** all accessors exist; INV-3 through U-069's reflective helper.
- 30 tests (23 red at Start, 7 guards), 8 injections with complete derived failing sets and failure labels, checked by a
  tool; 12 Gradle runs (two full gates), one session; a probe that turns every stop condition into a runtime check
  before the first production edit.
- Device evidence of periodicity comes from WorkManager's database, because 2.9.1 schedules no periodic job (SC-2).

Asked of the orchestrator before the build: rulings on SC-1 to SC-6 (recommendations given; none blocks the plan's
default path). Not verified: the real compile on Gradle's classpath with Robolectric 4.18.1 and Truth 1.4.5 (N1), the
probe's runtime readings (N2), JaCoCo's numbers (N5), the device formats of section 9, R6, and the platform's job quota.

## Phase Completion Report
---
story_id: "U-076"
phase: "planning"
verdict: "NEEDS_INFO"
artifact_path: "sdlc/artifacts/build/sprints/sprint-025/U-076/plan.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
notes: "draft saved at the owner's stop; planning to be resumed"
---
