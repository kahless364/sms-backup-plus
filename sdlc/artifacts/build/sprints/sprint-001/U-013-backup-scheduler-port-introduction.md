---
type: story
status: planned
sprint: "000001"
artifact_type: user-story
priority: high
complexity: medium
parallel_eligible: false
iteration: 2
requirements:
  - REQ-MODERNIZATION-005
design_docs:
  - DES-MODERNIZATION-005
integration_contracts:
  - CNTR-MODERNIZATION-004
  - CNTR-MODERNIZATION-005
dependencies:
  - U-006
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-013
title: Introduce BackupScheduler port and LegacyScheduler adapter (branch-by-abstraction seam)
pipeline: ''
domain: modernization
requirement_source: authored
---

# U-013: Introduce BackupScheduler port and LegacyScheduler adapter (branch-by-abstraction seam)

## Story

As a developer executing the WorkManager migration (MU-005),
I want a `BackupScheduler` port that defines the full scheduling contract in core, backed by a `LegacyScheduler` adapter that wraps the current `BackupJobs` machinery — with every call site (broadcast receiver, boot receiver, SMS receiver, `SmsJobService`, `SmsBackupService`, and `App`) already routing through the port —
so that the subsequent WorkManager implementation (U-014) can be introduced by adding a new adapter and flipping the binding in one place, with zero call-site changes and with the public `com.zegoggles.smssync.BACKUP` broadcast contract preserved byte-for-byte throughout.

## Acceptance Criteria

**AC-1 — `BackupScheduler` port exists in core with the full nine-operation surface**

Given the core module as produced by this story,
when a developer inspects the `BackupScheduler` interface (or Kotlin `interface`/abstract class) at `core/src/main/java/com/zegoggles/smssync/core/scheduler/BackupScheduler.kt` (exact package path to be finalized at implementation time but must live in core with no `com.firebase.*`, `androidx.work.*`, or `android.app.AlarmManager` types in any method signature),
then all nine operations are present with the signatures below, and no Android scheduler-framework type appears in any parameter or return type:

- `scheduleIncoming(): ScheduledJob?` — one-off delayed by `incomingTimeoutSecs`; network constraint per `isWifiOnly()`
- `scheduleRegular(): ScheduledJob?` — recurring backup; network constraint per `isWifiOnly()`; exponential backoff (initial 30s, cap 300s)
- `scheduleContentTrigger(): ScheduledJob?` — recurring content-URI observer over the SMS provider (plus call-log URI when `isCallLogBackupAfterCallEnabled()`); API-level branching lives inside the adapter, not in the port signature
- `scheduleBootup(): ScheduledJob?` — if `!isAutoBackupEnabled()` calls `cancelAll()` and returns null; otherwise schedules a regular backup with the 60-second boot delay
- `scheduleImmediate(): ScheduledJob?` — one-off with no network constraint; fires for the `BROADCAST_INTENT` path
- `scheduleRestore(config: RestoreConfig): ScheduledJob?` — one-off restore job (new capability; no scheduling analog exists today)
- `cancelAll()` — equivalent to `cancelRegular()` + cancel of the content-trigger job
- `cancelRegular()` — cancels the periodic backup job only
- `cancel(jobKind: JobKind)` — single-flight cancellation by job kind
- `observe(jobKind: JobKind): Flow<SchedulerState>` — observable job state + progress; must not block

**AC-2 — `LegacyScheduler` adapter wraps the existing `BackupJobs` machinery without modifying it**

Given `LegacyScheduler` at `app/src/main/java/com/zegoggles/smssync/scheduler/LegacyScheduler.kt` (package path to be finalized),
when a developer reads the implementation,
then:
- `scheduleIncoming()` delegates to `backupJobs.scheduleIncoming()`
- `scheduleRegular()` delegates to `backupJobs.scheduleRegular()`
- `scheduleContentTrigger()` delegates to `backupJobs.scheduleContentTriggerJob()`
- `scheduleBootup()` delegates to `backupJobs.scheduleBootup()`
- `scheduleImmediate()` delegates to `backupJobs.scheduleImmediate()`
- `scheduleRestore(config)` returns null (no legacy analog) with a log message noting the stub
- `cancelAll()` delegates to `backupJobs.cancelAll()`
- `cancelRegular()` delegates to `backupJobs.cancelRegular()`
- `cancel(jobKind)` delegates to the tag-based cancel on `BackupJobs` using the same tag strings currently used by `BackupJobs` (`backupType.name()`, `"contentTrigger"`)
- `observe(jobKind)` returns `MutableStateFlow(SchedulerState.Unknown)` (unimplemented in the legacy path; no behavioral regression because the legacy path had no observable state)
- the underlying `BackupJobs` instance is **not modified** — `BackupJobs.java` has zero diffs introduced by this story; the adapter holds a `BackupJobs` reference and calls its existing public methods

**AC-3 — All seven call sites are migrated from `new BackupJobs(context)` to the injected `BackupScheduler` port**

Given the full production source tree after this story,
when a developer runs `grep -rn "new BackupJobs" app/src/main/java/`,
then the command produces zero lines of output — confirming that no call site constructs `BackupJobs` directly.

The seven call sites verified in current source, each of which must be migrated, are:

| Call site | File (verified) | Current call | Port call |
|-----------|-----------------|--------------|-----------|
| CS-1 | `receiver/BackupBroadcastReceiver.java:42` | `new BackupJobs(context).scheduleImmediate()` | `scheduler.scheduleImmediate()` |
| CS-2 | `receiver/BootReceiver.java:29` (`getBackupJobs`) | `new BackupJobs(context)` | inject `BackupScheduler` |
| CS-3 | `receiver/SmsBroadcastReceiver.java:86` (`getBackupJobs`) | `new BackupJobs(context)` | inject `BackupScheduler` |
| CS-4 | `service/SmsJobService.java:147` (`getBackupJobs`) | `new BackupJobs(this)` | inject `BackupScheduler` |
| CS-5 | `service/SmsBackupService.java:308` (`getBackupJobs`) | `new BackupJobs(this)` | inject `BackupScheduler` |
| CS-6 | `App.java:78` | `new BackupJobs(this)` | inject `BackupScheduler` |
| CS-7 | `service/SmsBackupService.java:277` | `getBackupJobs().scheduleRegular()` | `scheduler.scheduleRegular()` |

Injection mechanism (Hilt or manual constructor injection) is at the implementer's discretion provided the DI approach is consistent with the project's Hilt migration plan (DES-MODERNIZATION-008); the choice must be documented in Implementation Notes.

**AC-4 — Content-trigger two-stage debounce behavior is preserved through the port**

Given `SmsJobService.onStartJob` currently calls `getBackupJobs().scheduleIncoming()` (not a direct backup) when triggered by the content-URI job (`wasTriggeredByContentUri` — verified at `SmsJobService.java:72-77`),
when the call site is migrated to use the `BackupScheduler` port (AC-3, CS-4),
then `SmsJobService.onStartJob` calls `scheduler.scheduleIncoming()` under the same `wasTriggeredByContentUri` branch condition, and `LegacyScheduler.scheduleIncoming()` continues to delegate to `backupJobs.scheduleIncoming()` — preserving the delayed-follow-up two-stage debounce without modification.

A unit test named `contentTrigger_schedulesIncomingFollowUp_notDirectBackup` in `SmsJobServiceTest` (new or extended) confirms that when `onStartJob` is called with a job tagged `"contentTrigger"`, the injected `BackupScheduler` receives a `scheduleIncoming()` call and receives no other schedule call during that invocation.

**AC-5 — `com.zegoggles.smssync.BACKUP` broadcast contract is preserved byte-for-byte**

Given `BackupBroadcastReceiver` before and after the call-site migration (CS-1),
when a developer diffs `BackupBroadcastReceiver.java` between the base commit and this story's commit,
then:
- the class name `BackupBroadcastReceiver` is unchanged
- the constant `BACKUP_ACTION = "com.zegoggles.smssync.BACKUP"` is unchanged
- the `onReceive` method signature and the `BACKUP_ACTION.equals(intent.getAction())` guard are unchanged
- the `isAllow3rdPartyIntegration()` guard in `backupRequested()` is unchanged
- the only diff inside `backupRequested()` is the replacement of `new BackupJobs(context).scheduleImmediate()` with `scheduler.scheduleImmediate()` (where `scheduler` is the injected `BackupScheduler`)
- `AndroidManifest.xml` line 153 (`<action android:name="com.zegoggles.smssync.BACKUP"/>`) is unchanged

An ADB verification (described in Verification Steps) confirms that after the migration a backup work item is enqueued when the broadcast is sent.

**AC-6 — `App.rescheduleJobs()` routes through the port for all three scheduling calls**

Given `App.java` after migration,
when a developer reads `App.rescheduleJobs()` (currently at `App.java:195-204`),
then all three calls — `backupJobs.cancelAll()`, `backupJobs.scheduleRegular()`, and `backupJobs.scheduleContentTriggerJob()` — are replaced with `scheduler.cancelAll()`, `scheduler.scheduleRegular()`, and `scheduler.scheduleContentTrigger()` respectively, and the `backupJobs` field is removed from `App`.

**AC-7 — `LegacyScheduler` produces behaviorally identical scheduling output to `BackupJobs` for all five schedulable job types**

Given a Robolectric unit test class `LegacySchedulerTest` exercising `LegacyScheduler` with a real `BackupJobs` instance backed by a mock `Preferences`,
when each of the five schedule operations is called under the same preference stubs used in the existing `BackupJobsTest`:

- `scheduleIncoming()` with `getIncomingTimeoutSecs() > 0`
- `scheduleRegular()` with `isAutoBackupEnabled() = true`
- `scheduleContentTrigger()`
- `scheduleBootup()` with `isAutoBackupEnabled() = true`
- `scheduleImmediate()`

then the `Job` objects produced by `LegacyScheduler` are equal to those produced by a direct call to `BackupJobs` with the same preference stubs — verifying that the adapter is a pure delegation layer with no behavioral change.

**AC-8 — `LegacyScheduler.cancelAll()` and `cancelRegular()` delegate and do not throw**

Given `LegacyScheduler` with a `BackupJobs` backed by a `FirebaseJobDispatcher` that always returns `CANCEL_RESULT_SUCCESS`,
when `cancelAll()` is called,
then `BackupJobs.cancelRegular()` and `BackupJobs.cancelContentUriTrigger()` are both invoked (verified via Mockito `verify`) and no exception propagates to the caller.

When `cancelRegular()` is called alone, only `BackupJobs.cancelRegular()` is invoked.

**AC-9 — Existing `BackupJobsTest` suite continues to pass without modification**

Given the test class `service/BackupJobsTest.java` as it exists after U-006 merges (all AC-4 through AC-7 tests from U-006 green),
when this story's changes are merged,
then `./gradlew :app:testDebugUnitTest --tests "*.BackupJobsTest"` exits with `BUILD SUCCESSFUL` and zero test failures — confirming that `BackupJobs.java` is unmodified and the characterization tests remain valid.

**AC-10 — No `com.firebase.jobdispatcher` import appears in `BackupScheduler` port or any call-site class**

Given all files modified or created by this story,
when a developer runs `grep -rn "com.firebase.jobdispatcher" <each modified file>`,
then zero matches are found in: `BackupScheduler.kt`, `LegacyScheduler.kt`, `BackupBroadcastReceiver.java`, `BootReceiver.java`, `SmsBroadcastReceiver.java`, `SmsJobService.java`, `SmsBackupService.java`, `App.java`. Firebase imports must remain confined to `BackupJobs.java` and `AlarmManagerDriver.java`, which are unchanged by this story.

### Integration Criteria

- [ ] IC-1: `LegacyScheduler` is registered as the `BackupScheduler` binding in the Hilt application module (or equivalent DI configuration) before the first call site that depends on it; the binding location is documented in Implementation Notes with the file and method name.
- [ ] IC-2: Every call site listed in AC-3 (CS-1 through CS-7) resolves `BackupScheduler` through the DI graph at runtime — not via `new LegacyScheduler(...)` inline — so that the production binding can be swapped in U-014 by changing a single Hilt module entry.
- [ ] IC-3: `SmsJobService.shouldRun()` and `backupStateChanged()` (which call `getBackupJobs().cancelRegular()` and depend on `JobParameters`) are migrated to call the injected `scheduler.cancelRegular()` and use `scheduler.observe(jobKind)` or an equivalent port-mediated signal, so that `SmsJobService` holds no direct reference to `BackupJobs` after this story.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/receiver/BackupBroadcastReceiver.java` | Constructs `new BackupJobs(context)` at line 42 and calls `scheduleImmediate()` | Inject `BackupScheduler`; replace direct construction with `scheduler.scheduleImmediate()` (CS-1); action string, receiver name, and manifest filter unchanged |
| `app/src/main/java/com/zegoggles/smssync/receiver/BootReceiver.java` | `getBackupJobs(context)` factory method returns `new BackupJobs(context)` at line 29; calls `scheduleBootup()` at line 25 | Inject `BackupScheduler`; remove `getBackupJobs` factory; call `scheduler.scheduleBootup()` (CS-2) |
| `app/src/main/java/com/zegoggles/smssync/receiver/SmsBroadcastReceiver.java` | `getBackupJobs(context)` factory method returns `new BackupJobs(context)` at line 86; calls `scheduleIncoming()` at line 52 | Inject `BackupScheduler`; remove `getBackupJobs` factory; call `scheduler.scheduleIncoming()` (CS-3) |
| `app/src/main/java/com/zegoggles/smssync/service/SmsJobService.java` | `getBackupJobs()` factory at line 147 returns `new BackupJobs(this)`; calls `scheduleIncoming()` (line 76), `cancelRegular()` (line 138) | Inject `BackupScheduler`; remove `getBackupJobs` factory; replace both calls through port (CS-4, IC-3) |
| `app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java` | `getBackupJobs()` factory at line 308 returns `new BackupJobs(this)`; calls `scheduleRegular()` at line 277 | Inject `BackupScheduler`; remove `getBackupJobs` factory; call `scheduler.scheduleRegular()` (CS-5, CS-7) |
| `app/src/main/java/com/zegoggles/smssync/App.java` | Constructs `new BackupJobs(this)` at line 78; holds `backupJobs` field; `rescheduleJobs()` calls `cancelAll()`, `scheduleRegular()`, `scheduleContentTriggerJob()` | Inject `BackupScheduler`; remove `backupJobs` field; update `rescheduleJobs()` to use port (CS-6, AC-6) |
| `app/src/main/java/com/zegoggles/smssync/service/BackupJobs.java` | Primary scheduler class (four mechanisms, `Driver` Strategy at lines 66-72) | **No changes** — this file is frozen in this story; it is wrapped by `LegacyScheduler`, not modified |
| `app/src/main/java/com/zegoggles/smssync/service/AlarmManagerDriver.java` | `AlarmManager`-based `Driver` implementation | **No changes** — retained as-is until U-017 |
| `core/src/main/java/com/zegoggles/smssync/core/scheduler/BackupScheduler.kt` | Does not exist | **Created** — the hexagonal port interface with nine operations and no Android scheduler types in its signature |
| `app/src/main/java/com/zegoggles/smssync/scheduler/LegacyScheduler.kt` | Does not exist | **Created** — `BackupScheduler` implementation that delegates all operations to `BackupJobs`; `scheduleRestore` returns null with a log stub; `observe` returns `MutableStateFlow(SchedulerState.Unknown)` |
| DI module (new or extended) | Does not exist / not updated | **Created/updated** — binds `BackupScheduler` to `LegacyScheduler` as a singleton for the application lifetime |

## Existing Behavior to Preserve

- All current scheduling behavior produced by `BackupJobs` — retry strategy (initial 30s, cap 300s, exponential), network constraints (`ON_UNMETERED_NETWORK` for wifi-only, `ON_ANY_NETWORK` otherwise, `new int[0]` for `BROADCAST_INTENT`), recurring vs. one-off job types, `setReplaceCurrent(true)` on every job, tag strings (`backupType.name()`, `"contentTrigger"`, `BOOT_BACKUP_DELAY = 60`) — must pass through `LegacyScheduler` unchanged.
- The `BOOT_BACKUP_DELAY = 60` constant in `BackupJobs.java:56` is used only inside `BackupJobs.scheduleBootup()`. The `LegacyScheduler` delegate calls `backupJobs.scheduleBootup()` directly, so the delay is preserved without the port needing to know the value.
- The `isUseOldScheduler()` `Driver`-selection ternary at `BackupJobs.java:68-71` is preserved as-is for the duration of this story; it is removed in U-017 (legacy scheduler removal).
- The `getBackupJobs()` protected factory methods in `BootReceiver`, `SmsBroadcastReceiver`, and `SmsBackupService` exist to allow test subclassing (verified pattern in current tests). These factory methods are removed only if the DI injection approach makes them redundant; if any test class overrides them, the developer must migrate those test overrides to inject a mock `BackupScheduler` instead and document the change in Implementation Notes.
- `SmsJobService.backupStateChanged()` subscriber on the Otto bus (`@Subscribe`) must remain functional — the Otto wiring is not changed by this story (that is owned by DES-007 / a later story). Only the `cancelRegular()` call inside `shouldRun()` is rerouted through the port.
- `App.gcmAvailable` check and `setBroadcastReceiversEnabled` logic adjacent to the `backupJobs` field initialization are unchanged; only the `backupJobs` field and its usages are affected.

## Verification Steps

**AC-1 (port interface surface):**
1. Open `BackupScheduler.kt` (or `.java`) in core.
2. Confirm the file contains no `import com.firebase.*`, `import androidx.work.*`, or `import android.app.AlarmManager` statements.
3. Confirm all nine method signatures are present using the names in AC-1.
4. Confirm the return types are module-defined value types (`ScheduledJob?`, `Flow<SchedulerState>`) with no Android scheduler framework types.

**AC-2 (LegacyScheduler delegation):**
1. Open `LegacyScheduler.kt`.
2. For each of the eight delegating operations, confirm the body is a single delegation call to the corresponding `BackupJobs` method.
3. Confirm `scheduleRestore(config)` returns null and contains a log statement (not silent).
4. Confirm `observe(jobKind)` returns a `MutableStateFlow` initialized to `SchedulerState.Unknown` (or the equivalent sentinel value).
5. Run `grep -n "BackupJobs" LegacyScheduler.kt` and confirm the only references are the constructor parameter/field and the delegation calls — no static `new BackupJobs(...)` construction inside the adapter.

**AC-3 (call-site grep-zero):**
1. From the repository root, run: `grep -rn "new BackupJobs" app/src/main/java/`
2. Confirm the command produces zero lines of output.
3. For each of the seven call sites, open the file and confirm the method that previously called `new BackupJobs(...)` now calls the injected `scheduler` instance.

**AC-4 (content-trigger debounce preserved):**
1. Run `./gradlew :app:testDebugUnitTest --tests "*.SmsJobServiceTest.contentTrigger_schedulesIncomingFollowUp_notDirectBackup"`.
2. Confirm the test passes.
3. Open `SmsJobService.java` (or Kotlin equivalent) and confirm the `wasTriggeredByContentUri` branch still calls `scheduler.scheduleIncoming()` and does not call `scheduler.scheduleRegular()` or any other schedule operation.

**AC-5 (BACKUP broadcast contract):**
1. Run `git diff HEAD~1 -- app/src/main/java/com/zegoggles/smssync/receiver/BackupBroadcastReceiver.java` and confirm that `BACKUP_ACTION = "com.zegoggles.smssync.BACKUP"` and the `onReceive` guard are unchanged, and the only diff inside `backupRequested()` is the `new BackupJobs(context).scheduleImmediate()` → `scheduler.scheduleImmediate()` substitution.
2. Run `grep -n "com.zegoggles.smssync.BACKUP" app/src/main/AndroidManifest.xml` and confirm line 153 is unchanged.
3. ADB broadcast test: install the debug build on a device or emulator running API 21+. Run `adb shell am broadcast -a com.zegoggles.smssync.BACKUP`. Confirm that a backup job is enqueued via the scheduler (for `LegacyScheduler` this means `BackupJobs.scheduleImmediate()` is invoked, which can be confirmed via logcat tag `SMS_BACKUP+` showing the job scheduled).

**AC-6 (App.rescheduleJobs migration):**
1. Open `App.java` and locate `rescheduleJobs()`.
2. Confirm `backupJobs` field is absent from the class.
3. Confirm `rescheduleJobs()` calls `scheduler.cancelAll()`, `scheduler.scheduleRegular()`, and `scheduler.scheduleContentTrigger()` (in the same conditional structure as the original three calls).

**AC-7 (LegacyScheduler behavioral equivalence):**
1. Run `./gradlew :app:testDebugUnitTest --tests "*.LegacySchedulerTest"`.
2. Confirm all five schedule-operation equivalence tests pass.
3. Confirm the test class verifies that the `Job` tag, constraints, retry strategy, and recurrence flag match between `LegacyScheduler` output and direct `BackupJobs` output for each type.

**AC-8 (cancel delegation):**
1. Run `./gradlew :app:testDebugUnitTest --tests "*.LegacySchedulerTest"` (cancel tests within the same class).
2. Confirm `cancelAll()` test passes with both `cancelRegular()` and `cancelContentUriTrigger()` verified via Mockito.
3. Confirm `cancelRegular()` test passes with only `cancelRegular()` verified.

**AC-9 (BackupJobsTest unmodified):**
1. Run `./gradlew :app:testDebugUnitTest --tests "*.BackupJobsTest"`.
2. Confirm `BUILD SUCCESSFUL` with zero test failures.
3. Run `git diff HEAD~1 -- app/src/test/java/com/zegoggles/smssync/service/BackupJobsTest.java` and confirm no lines changed.

**AC-10 (no firebase imports in migrated files):**
1. For each of the eight non-`BackupJobs` files listed in AC-10, run `grep -n "com.firebase.jobdispatcher" <file>`.
2. Confirm zero matches in each.
3. Run `grep -rn "com.firebase.jobdispatcher" app/src/main/java/com/zegoggles/smssync/receiver/` to confirm zero matches in the entire receiver package.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android / JVM | Define `BackupScheduler` port in core; implement `LegacyScheduler` adapter; migrate all seven call sites; write `LegacySchedulerTest` equivalence and cancel tests; write `SmsJobService` content-trigger debounce test | Developer |

## Technical Notes

**The existing `Driver` Strategy is already half the port (CAND-001).** `BackupJobs.java:66-72` already selects between `AlarmManagerDriver` and `GooglePlayDriver` via a `preferences.isUseOldScheduler()` ternary — this is the Strategy pattern that DES-MODERNIZATION-005 identifies as CAND-001. That ternary is the internal driver selection; the `BackupScheduler` port is the external scheduling contract. This story makes the port explicit and routes all callers through it. The `Driver` strategy inside `BackupJobs` is not touched — it is the LegacyScheduler's internal detail until U-017.

**All seven call sites are verified from production source.** The grep `new BackupJobs` over `app/src/main/java` returns exactly eight lines: the `BackupJobs.java` constructor definitions (2 lines, not call sites) and six construction points in `BackupBroadcastReceiver.java:42`, `BootReceiver.java:29`, `SmsBroadcastReceiver.java:86`, `SmsJobService.java:147`, `SmsBackupService.java:308`, and `App.java:78`. The seventh call site (CS-7, `SmsBackupService.java:277`) calls `getBackupJobs().scheduleRegular()` where `getBackupJobs()` delegates to the construction at line 308 — it is not a separate `new BackupJobs` expression but must be migrated as part of CS-5.

**`BackupJobs.java` must not be modified.** This is the branch-by-abstraction seam: the LegacyScheduler wraps existing behavior; U-014 introduces a parallel `WorkManagerScheduler`; U-017 flips the binding and removes `BackupJobs`. Any modification to `BackupJobs.java` in this story invalidates the characterization tests from U-006 (AC-9) and breaks the strangler-fig sequencing.

**`scheduleRestore` stub in `LegacyScheduler`.** There is no restore scheduling analog in `BackupJobs` today. The stub exists so the port surface is complete and U-014/U-016 can implement it without changing the port. The stub must return null, not throw — callers that invoke `scheduleRestore` before U-016 ships must not crash.

**`observe()` stub in `LegacyScheduler`.** Firebase JobDispatcher offers no observable state API. Returning `MutableStateFlow(SchedulerState.Unknown)` is a no-op semantically and is safe for current UI consumers (none currently observe scheduler state — the Otto bus is used instead per `SmsJobService.java:110-126`). The `observe()` implementation becomes live in U-014/U-019.

**`scheduleContentTrigger()` port name vs. `scheduleContentTriggerJob()` in `BackupJobs`.** The port uses the shorter name `scheduleContentTrigger()` (no `Job` suffix) per DES-MODERNIZATION-005 §BackupScheduler port. The `LegacyScheduler` adapter calls `backupJobs.scheduleContentTriggerJob()` internally — the name discrepancy is entirely inside the adapter.

**`App.rescheduleJobs()` note on `isUseOldScheduler()` guard.** `App.java:201` checks `!preferences.isUseOldScheduler()` before calling `scheduleContentTriggerJob()`. The migrated call to `scheduler.scheduleContentTrigger()` must preserve this guard — it must not be dropped.

**DI injection for BroadcastReceivers.** Standard Hilt injection via `@AndroidEntryPoint` on `BroadcastReceiver` subclasses is the recommended approach consistent with DES-008. If Hilt `@AndroidEntryPoint` cannot be applied to all receivers in this story's sprint (e.g., AGP version constraint), the developer may use `EntryPoints.get(context, ...)` as a fallback for this story only, with a note that the clean `@AndroidEntryPoint` form will be completed in U-014 or the Hilt migration story (U-NNN per DES-008). The choice must be documented in Implementation Notes and must not leave any call site using `new LegacyScheduler(...)` inline.

**Gate G2 dependency.** This story depends on U-006 (Gate G2 green). The characterization tests in U-006 (AC-4 through AC-7 in that story) pin the `BackupJobs` retry strategy and constraint values against which the `LegacySchedulerTest` equivalence assertion (AC-7 this story) is also anchored. U-013 may not be sprint-planned until U-006 is `status: done` and Gate G2 is declared.

**`cancelContentUriTrigger()` is private in `BackupJobs`.** The private method `BackupJobs.cancelContentUriTrigger()` (line 112) is called by `cancelAll()`. Since `LegacyScheduler.cancelAll()` delegates to `backupJobs.cancelAll()`, the private method is exercised transitively — the adapter does not need a separate `cancel("contentTrigger")` call.  The AC-8 Mockito verify of `cancelContentUriTrigger()` must be done on a spy of `BackupJobs` or by verifying that the `FirebaseJobDispatcher.cancel("contentTrigger")` invocation occurs, not by calling the private method directly.

## Supporting Documentation

- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md` — requirement this story partially satisfies (AC-1: port exists; AC-1 first half: all call sites use the port; AC-4: broadcast contract preserved)
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` — §BackupScheduler port (CAND-001 realized): full nine-operation surface and rationale; §LegacyScheduler adapter: delegation semantics; §Branch-by-abstraction parallel-run: confirms this story is the seam step, not the cutover; §Integration Design / BackupScheduler port (operations): per-operation semantics table; §Constraint + backoff configuration: value object fields
- `sdlc/artifacts/stories/U-006-chartest-migrate-backupjobs-coverage.md` — Gate G2 dependency; AC-4 through AC-7 in U-006 pin the `BackupJobs` retry and constraint values that `LegacySchedulerTest` (AC-7 this story) re-asserts through the adapter

## Integration Contract References

- CNTR-MODERNIZATION-004 — `BackupScheduler` port (core → app) service contract: nine operations, constraint/backoff value object, unique names per `BackupType`, `SchedulerState` observation. This story creates the port and the first adapter; the contract governs the operation signatures and behavioral invariants that both the `LegacyScheduler` and the future `WorkManagerScheduler` must satisfy.
- CNTR-MODERNIZATION-005 — `com.zegoggles.smssync.BACKUP` broadcast external event contract: action string `com.zegoggles.smssync.BACKUP`, receiver class `BackupBroadcastReceiver`, manifest intent-filter at `AndroidManifest.xml:153`, `isAllow3rdPartyIntegration()` guard, and downstream effect `scheduler.scheduleImmediate()`. AC-5 of this story is the per-story verification that the external contract is preserved.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Draft story written against verified source (`BackupJobs.java:62-207` full Driver Strategy + all seven public schedule/cancel methods; `BackupBroadcastReceiver.java:42` CS-1; `BootReceiver.java:29` CS-2; `SmsBroadcastReceiver.java:86` CS-3; `SmsJobService.java:147` CS-4; `SmsBackupService.java:308` CS-5; `App.java:78` CS-6; `SmsBackupService.java:277` CS-7; `AndroidManifest.xml:153` BACKUP action; `SmsJobService.java:72-77` content-trigger two-stage debounce). NOT finalized. Load-bearing open item: the DI injection approach for `BroadcastReceiver` call sites (CS-1/CS-2/CS-3) must be chosen and recorded in Implementation Notes before sprint-planning — if Hilt `@AndroidEntryPoint` on receivers is not yet available, the `EntryPoints.get` fallback is acceptable for this story only with a tracked follow-up.
