---
story_id: U-069
phase: code-review
reviewer: code-reviewer
commit: d7a69974
verdict: PASS
---

## Code Review: U-069

### Summary
The change is small and correct. `BackupTriggerWorker` becomes a `@HiltWorker` that calls `BackupScheduler.scheduleIncoming()`, so the content-trigger path now gets the U-054 gate, the auto-backup and delay rule, the network constraint, the 30 s backoff, and unique work INCOMING with REPLACE. The class name and package are unchanged. The tests exercise the real `WorkManagerScheduler`, and the claimed detections are plausible from the test code. The only other change is one JaCoCo floor. I found no Critical or High findings.

Verdict: PASS

### Findings

#### Critical
None.

#### High
None.

#### Medium
None.

#### Low

- **L-1: The reflection helper depends on WorkManager internals, and the comment pins versions that the next raise will make stale.** `BackupTriggerWorkerTest.kt`, `captureActiveIncomingRequest()` and `workSpecOf()` (read at the commit; the comment block above `captureActiveIncomingRequest` and the helper at the end of the file).
  - The helper reaches `WorkManagerImpl.getWorkDatabase()`, `WorkDatabase.workSpecDao()`, `WorkSpecDao.getWorkSpec(String)`, and the `WorkSpec` public fields `backoffPolicy`, `backoffDelayDuration` and `input`.
  - These are `@RestrictTo(LIBRARY_GROUP)` internals. The project's `work-testing` is 2.9.1. U-071/U-073 may raise it, and the comment asserts that 2.12.0 has the same members, which will rot.
  - It does not pass vacuously, which is what matters:
    - Every reflective step throws (`ClassNotFoundException`, `NoSuchMethodException`, `NoSuchFieldException`, or `InvocationTargetException` on a throwing `invoke`).
    - A null spec fails `assertWithMessage(...).that(spec).isNotNull()`.
    - The ac3 tests also pin the values (`UNMETERED`/`CONNECTED`, `EXPONENTIAL`, 30000 ms, 60000 ms, tag), so "equal but both wrong" cannot pass.
  - The cost of a break is a red test class, not a silent pass.
  - Risk: a WorkManager raise may turn the 2 ac3 cases red for a reason unrelated to the behaviour. A reflection failure surfaces as a reflection exception with no hint at the cause.
  - Suggested fix (optional): wrap the reflection in one `try`/`catch (ReflectiveOperationException)` that rethrows `AssertionError("WorkManager internals moved (WorkSpec/WorkSpecDao); update captureActiveIncomingRequest")`. Soften the "2.9.1 and 2.12.0" claim to "checked at 2.9.1".
  - Reflecting through the public declaring types is sound. The Room dependency is `runtimeOnly` for `work-runtime`, so it cannot be named at compile time.

- **L-2: The log line says "enqueueing delayed incoming backup" even when the scheduler then declines.** `BackupTriggerWorker.kt` lines 63-64.
  - The byte-for-byte line is deliberately preserved. The tests assert it, and CR-015, BUG-021 AC-12 and DES-014 Run R read it.
  - A degraded or auto-backup-off run therefore logs "enqueueing" followed by the scheduler's "blocked" or "skipped" line.
  - No change is needed now, because the following line explains it. If the string contract is ever relaxed, change it to "trigger fired; requesting delayed incoming backup".

#### Info

- **I-1: `preferences.incomingTimeoutSecs` is read twice (worker, for the log; scheduler, to decide).** `BackupTriggerWorker.kt` line 61.
  - A preference change between the two reads could make the logged delay differ from the real one. This is benign and documented in the code comment.
  - The injected `Preferences` exists only for this log line. That cost is accepted, because the log string is a contract.

- **I-2: The worker does not await the `Operation` returned by `enqueueUniqueWork`.** It returns `Result.success()` immediately after `scheduleIncoming()`.
  - The enqueue runs on WorkManager's task executor, and the worker never blocks on it.
  - The old code behaved the same way.
  - There is no deadlock or threading hazard. The `Worker.doWork()` background thread calling a non-blocking enqueue is the normal pattern.
  - `Result.success()` on a declined enqueue is right:
    - A decline is a rule working.
    - Returning `retry()` would back off and re-run the trigger.
    - Returning `failure()` would mark the periodic trigger failed.
    - The scheduler logs the reason.

- **I-3: Pre-existing semantic, unchanged by this story.** REPLACE on INCOMING cancels an INCOMING `BackupWorker` that is currently running if the trigger fires again. This is INV-1 (single-flight, `setReplaceCurrent(true)`), identical to the old worker and to every other `scheduleIncoming()` caller. It is not a finding against U-069. It is related to BUG-025 and BUG-029 territory.

- **I-4: AC-7 (`ac7_debounce_...`) passes on the old code** (it was PASS in the red run, `I2-red-cases.txt`). That is correct. The old worker also delayed, and the case guards the debounce against regression rather than detecting BUG-023. Delay length is additionally asserted by ac3 and ac9, which failed red.

### Judgement answers

**1. Worker correctness**
- Every rule is inherited because the worker calls the single builder, `WorkManagerScheduler.scheduleIncoming()` (`scheduler/WorkManagerScheduler.kt`):
  - the `isEncryptionDegraded()` gate;
  - `isAutoBackupEnabled` and `timeoutSecs <= 0` return null;
  - `networkConstraints(INCOMING)`;
  - `EXPONENTIAL` 30 s backoff;
  - `setInitialDelay(timeoutSecs)`;
  - tag INCOMING;
  - `enqueueUniqueWork(INCOMING, REPLACE, ...)`.
- The old worker had no constraints and no backoff, and ignored the gate and the auto-backup flag. That was BUG-023.
- The class is still `com.zegoggles.smssync.worker.BackupTriggerWorker`. Persisted CONTENT_TRIGGER work looks it up by that name. `ic1` asserts the name.
- Reachability:
  - `App.java` implements `Configuration.Provider` and has a field-injected `HiltWorkerFactory` (lines 90, 132). Auto-initialisation is disabled in the manifest.
  - `BackupWorker` and `RestoreWorker` are already `@HiltWorker`, so `hilt-work` 1.2.0 and the compiler are in place.
  - A third `@HiltWorker` needs nothing else. The generated `_HiltModule` installs itself in `SingletonComponent` and is picked up by Hilt's aggregating processor.
  - `BackupScheduler` is bound by `SchedulerModule` (`@Binds`, `@Singleton`). `Preferences` is already injected into `BackupWorker`. There is no cycle: `WorkManagerScheduler` references the worker only by `Class`.
  - `scheduleContentTrigger()` builds `PeriodicWorkRequest.Builder(BackupTriggerWorker::class.java...)`. Because the worker is created by the factory, persisted rows resolve through the map key (class name).
- Whether the factory actually builds the worker on a device is IC-3 (orchestrator). The unit test (`ic1`) verifies the generated classes, the map key, and the single assisted constructor with its parameter order.

**2. Tests**
- The 15 cases run the real `WorkManagerScheduler` in the WorkManager test harness and assert the enqueued request. The worker is built with the test's scheduler through a `WorkerFactory`.
- Red run: 11 fail, 4 pass on the old behaviour (`evidence/I2-red-cases.txt`). The 4 passing cases are ac7 and ac8a/b/c. Their passing is plausible, because the old code also used REPLACE on INCOMING and delayed.
- Each detection matches a rule the old worker lacked:
  - ac1/ac2/ac3: no network constraint or backoff;
  - ac4/ac5/ac6: no gate, flag or delay rule, so work was enqueued;
  - ac9: the scheduler's log lines are absent;
  - ic1: no generated Hilt classes.
- Each test registers the trigger with happy-path stubs and then flips one stub, so the rules are shown at run time. Success is asserted in every declined case.
- Mutation runs M-T1 to M-T12 are in the evidence (11 detections; M-T11 is compile-only). I judged them plausible from the test code, not by re-running them.
- The reflection risk is covered under L-1.

**3. JaCoCo floor**
- `app/build.gradle`: the `worker*` rule goes from 0.00 to 0.90, and a comment line is replaced. Nothing else is touched. The diff is 2 lines of floor plus the comment, and the stat shows 4 changed lines in the file.
- The numbers are consistent with the evidence: 10/10 lines (`I6-worker-lines.txt`).
  - At 10 counted lines, 0.95 leaves no spare line (it needs 9.5, so 10).
  - 0.90 leaves one spare line (9).
  - So 0.90 is the highest 0.05 step with a spare line, as ruling 47 says.
- The comment is accurate: the Hilt-generated classes report 0/0 lines, and the companion is empty. It is a single long line, which is cosmetic only.

**4. Quality**
- Naming and KDoc are clear and accurate. The class doc names scheduleIncoming and the INV/U-054 rules, and notes that success is returned when the scheduler declines. Imports in both files are used (checked by reading).
- The log line carries no message content or address.
- The tests follow the `WorkManagerSchedulerTest` harness.
- Only trivial issues are L-2 and I-1.

**5. Scope**
- `git show --stat d7a69974 -- app` lists exactly `app/build.gradle`, `BackupTriggerWorker.kt` and `BackupTriggerWorkerTest.kt`. `SmsBroadcastReceiver` is untouched (ruling 56). No other floor is changed.
- BUG-025, BUG-026, BUG-027, BUG-029 and the CNTR-004 example lines were not raised.

### Patterns Verified
- [x] Follows existing code patterns (`@HiltWorker`/`@AssistedInject` as `BackupWorker`)
- [x] Error handling is appropriate (decline is success; scheduler logs the reason)
- [x] Tests cover new functionality
- [x] No hardcoded values that should be configurable
- [x] No unnecessary complexity

### Integration Verified
- [x] New code is reachable from production entry points (`scheduleContentTrigger()` -> persisted CONTENT_TRIGGER work -> `HiltWorkerFactory` in `App` -> worker -> `scheduleIncoming()`)
- [x] Registries/dispatch maps updated (generated `_HiltModule` map key; no manual registry)
- [x] Function signatures match at all call sites (the only constructor callers are generated code and the test factory; the scheduler uses `::class.java`)
- [x] No dead code introduced
- [x] Integration path documented in implementation-log.md

### Regression Check
- [x] Replacement of a single method body; the old inline `OneTimeWorkRequest` build is intentionally removed. Its only function (INCOMING enqueue with REPLACE and delay) is retained inside `scheduleIncoming()`. No capability lost.

### Contract Verification
- [x] Consumers identified: `WorkManagerScheduler.scheduleContentTrigger()` (by class), persisted work rows, and log readers (CR-015, BUG-021 AC-12, DES-014 Run R). The log line is unchanged byte for byte.
- [x] No field or event renames.

### Runtime Claims Verified
- [x] The inventory is present in the implementation log (the claim is the 15 tests plus the mutation runs; the probe is real WorkManager test-harness runs).
- [x] The probe design is adequate: the real scheduler, the real WorkManager harness, the red run on the old behaviour, and mutation falsifiability.
- [x] No escape-hatch entries.
- [x] No probe wrote into `../amp`. I found no sign of it in the log or the evidence names.
- [x] UNVERIFIED (escape-hatch) claims: None. The on-device behaviour (the factory builds the worker; the trigger path on an emulator) is IC-3, run by the orchestrator and not claimed here.
