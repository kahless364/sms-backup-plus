---
type: story
status: done
sprint: '000001'
artifact_type: user-story
priority: high
complexity: high
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-005
design_docs:
  - DES-MODERNIZATION-005
integration_contracts:
  - CNTR-MODERNIZATION-004
  - CNTR-MODERNIZATION-005
dependencies:
  - U-013
  - U-003
change_records: []
platforms:
  - android
tags:
  - workmanager
  - scheduler
  - branch-by-abstraction
  - gate-g3
gate_additions:
  - G3
id: U-014
title: 'WorkManagerScheduler: production BackupScheduler adapter with REPLACE semantics, EXPONENTIAL 30s/300s backoff, per-type network constraints, and content-URI trigger'
pipeline: modernization
domain: modernization
updated_at: '2026-06-03T07:06:55.868Z'
resolution: done
---

# U-014: WorkManagerScheduler — Production BackupScheduler Adapter

## Story

As a maintainer of SMS Backup+,
I want to implement `WorkManagerScheduler` as the production `BackupScheduler` adapter that maps every scheduling operation to WorkManager `WorkRequest`s with REPLACE enqueue semantics, a single EXPONENTIAL backoff strategy (30s initial, 300s cap), per-type network constraints (UNMETERED/CONNECTED/NONE), and a content-URI trigger with a pre-API-24 broadcast fallback,
so that the four legacy background-execution mechanisms (Firebase JobDispatcher, `AlarmManagerDriver`, `SmsJobService` lifecycle bypass, and `AsyncTask`) are retired behind a single binding flip while all current scheduling behaviors and the public `com.zegoggles.smssync.BACKUP` broadcast contract are preserved without regression.

## Acceptance Criteria

- [ ] AC-1: **[REPLACE semantics — INV-1 single-flight]** Given `WorkManagerScheduler` is the bound `BackupScheduler` implementation, when the same job kind (e.g., `REGULAR`) is enqueued twice in succession via the port without an intervening `cancel`, then `WorkManager.getWorkInfosForUniqueWork(uniqueName)` returns exactly one non-terminal `WorkInfo` item — confirming `ExistingWorkPolicy.REPLACE` (one-off work) and `enqueueUniquePeriodicWork(name, UPDATE, ...)` (periodic work) implement the `setReplaceCurrent(true)` invariant from `BackupJobs.java:188`. The test runs under `WorkManagerTestInitHelper.initializeTestWorkManager` with a `SynchronousExecutor`.

- [ ] AC-2: **[Backoff fidelity — INV-3 single EXPONENTIAL strategy]** Given any non-`BROADCAST_INTENT` backup type enqueued via the port, when the produced `WorkSpec` is inspected, then `workSpec.backoffPolicy == BackoffPolicy.EXPONENTIAL` and `workSpec.backoffDelayDuration == 30_000L` (30 seconds in milliseconds). Additionally, an instrumented or unit test asserts that after simulated retry scheduling the effective computed delay does not exceed 300 000ms (300 seconds), reproducing the `newRetryStrategy(RETRY_POLICY_EXPONENTIAL, 30, 300)` single-strategy semantics from `BackupJobs.java:205`. There is exactly one backoff configuration applied — the story must not introduce separate initial-vs-cap values for backup and restore; restore has no current backoff value and inherits the same 30s/300s strategy.

- [ ] AC-3: **[Network constraint fidelity — INV-2 per-type, no charging constraint]** Given `WorkManagerScheduler` mapping the `isWifiOnly()` preference and the `BROADCAST_INTENT` immediate path, when the produced `WorkSpec.constraints` for each case is inspected, then:
  (a) `scheduleRegular()` / `scheduleIncoming()` with `isWifiOnly() = true` produces `requiredNetworkType == NetworkType.UNMETERED`;
  (b) `scheduleRegular()` / `scheduleIncoming()` with `isWifiOnly() = false` produces `requiredNetworkType == NetworkType.CONNECTED`;
  (c) `scheduleImmediate()` (`BROADCAST_INTENT`) produces `Constraints.NONE` — specifically `requiredNetworkType == NetworkType.NOT_REQUIRED` — matching `BackupJobs.java:197` where `BROADCAST_INTENT` returns `new int[0]` (no constraint), NOT `CONNECTED`;
  (d) `requiresCharging` is `false` in all cases, asserting that the migration does not silently add a charging constraint not present in the legacy `jobConstraints()` at `BackupJobs.java:195-201`.

- [ ] AC-4: **[Content-URI trigger with pre-24 fallback — INV-4]** Given `WorkManagerScheduler.scheduleContentTrigger()` is called:
  (a) On API 24+: the produced `WorkSpec.constraints.contentUriTriggers` includes the SMS provider URI with `triggerForDescendants = true`, and also includes the call-log provider URI when `isCallLogBackupAfterCallEnabled()` is `true`, matching the `FLAG_NOTIFY_FOR_DESCENDANTS` semantics verified at `BackupJobs.java:179,181`. The triggered worker enqueues a delayed `OneTimeWorkRequest` with `initialDelay = getIncomingTimeoutSecs()` rather than executing the backup directly, preserving the two-stage debounce behavior from `SmsJobService.onStartJob:76`;
  (b) On API 14–23: `scheduleContentTrigger()` does not use `addContentUriTrigger` (unavailable below 24) and instead the SMS-received broadcast path calls `scheduler.scheduleIncoming()`, producing a `OneTimeWorkRequest` with `initialDelay = getIncomingTimeoutSecs()` seconds. The `SDK_INT >= 24` branch lives entirely inside `WorkManagerScheduler`; the port surface and callers are version-agnostic.

- [ ] AC-5: **[Degenerate-window timing — initialDelay, no flex window]** Given `scheduleIncoming()` with a positive `incomingTimeoutSecs` value, when the produced `OneTimeWorkRequest` is inspected, then `workSpec.initialDelay == incomingTimeoutSecs * 1_000L` milliseconds. There is no flex window. The `scheduleBootup()` path uses `initialDelay = 60_000L` (60 seconds = `BOOT_BACKUP_DELAY`). This confirms the degenerate-window mapping (`executionWindow(inSeconds, inSeconds)` → `setInitialDelay(inSeconds)`) from `BackupJobs.java:162`.

- [ ] AC-6: **[Public broadcast contract preserved]** Given `adb shell am broadcast -a com.zegoggles.smssync.BACKUP` is sent to the installed debug build, when the broadcast receiver handles the intent, then it resolves the injected `BackupScheduler` and calls `scheduler.scheduleImmediate()` — the action string `com.zegoggles.smssync.BACKUP`, the receiver class name, the `<intent-filter>` in `AndroidManifest.xml`, and the exported attribute are all unchanged from the legacy implementation. An instrumented test asserts that after the broadcast, `WorkManager.getWorkInfosForUniqueWork(BROADCAST_INTENT_UNIQUE_NAME)` contains an enqueued work item with `Constraints.NONE`. Third-party automation (e.g., Tasker) is unaffected.

- [ ] AC-7: **[CompositeScheduler debug-only parallel-run binding]** Given a debug build variant, when `CompositeScheduler` is the bound `BackupScheduler`, then every `scheduleXxx()` / `cancelXxx()` call is delegated to both `LegacyScheduler` (wrapping `BackupJobs`) and `WorkManagerScheduler` in sequence, divergences in produced constraints, backoff values, or trigger configuration are logged at `DEBUG` level, and the `CompositeScheduler` class, its Hilt module binding, and `LegacyScheduler` are absent from the release build variant (confirmed by `BuildConfig.DEBUG` guard or `debugImplementation` scoping). This binding is the pre-cutover confidence gate; the production flip replaces `LegacyScheduler` + `CompositeScheduler` with `WorkManagerScheduler` alone.

- [ ] AC-8: **[Four INV tests pass on the WorkManager test harness]** Given the WorkManager test harness initialized via `WorkManagerTestInitHelper.initializeTestWorkManager(context, config)` with a `SynchronousExecutor`, when the four invariant tests (INV-1 through INV-4, as specified in DES-MODERNIZATION-005 Design Validation) are executed as JVM unit tests or Robolectric tests, then all four pass with no `TestDriver` timing manipulation required for the scheduling assertions. Tests must be placed in `app/src/test/java/...service/WorkManagerSchedulerTest.java` (or the Kotlin equivalent). Each invariant is a separate `@Test` method with an independent failure message.

### Integration Criteria

- [ ] IC-1: `WorkManagerScheduler` is registered as the production `BackupScheduler` binding in a Hilt `@Module` in the app module (e.g., `app/src/main/java/.../di/SchedulerModule.kt`), replacing the `BackupJobs`-based construction at the `@Singleton` scope. The debug variant overrides this binding with `CompositeScheduler` via a `debugImplementation` Hilt module.
- [ ] IC-2: The broadcast receiver for `com.zegoggles.smssync.BACKUP` is wired to call `scheduler.scheduleImmediate()` on the injected `BackupScheduler` port rather than constructing `new BackupJobs(context)`. The receiver must be confirmed in `AndroidManifest.xml` and the receiver source before the story is closed.
- [ ] IC-3: All call sites that previously constructed `new BackupJobs(context)` or called methods directly on `BackupJobs` are migrated to the injected `BackupScheduler` port. The full set of call sites must be grep-confirmed at implementation time (scope assumed from DES-MODERNIZATION-005 — not pre-verified across the full tree) and each migration recorded in Implementation Notes.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/BackupJobs.java` | Owns all scheduling via Firebase `FirebaseJobDispatcher` and `GooglePlayDriver` / `AlarmManagerDriver` selection at lines 66–72 | Retained as-is during parallel-run; wrapped by `LegacyScheduler` for debug `CompositeScheduler`; deleted at binding-flip cutover (U-017) |
| `app/src/main/java/com/zegoggles/smssync/service/SmsJobService.java` | Extends `com.firebase.jobdispatcher.JobService`; `onStartJob` at lines 82–84 manually instantiates `SmsBackupService` to bypass the API-26 background-start restriction | Retained during parallel-run; deleted at cutover (U-017); `WorkManagerScheduler` eliminates the bypass via `CoroutineWorker.doWork()` |
| `app/src/main/java/com/zegoggles/smssync/service/AlarmManagerDriver.java` | Implements `com.firebase.jobdispatcher.Driver`; selected when `Preferences.isUseOldScheduler()` is true (line 44) | Retained during parallel-run; deleted at cutover (U-017); subsumed by WorkManager's internal scheduler selection |
| `app/src/main/java/com/zegoggles/smssync/[broadcast receiver source]` | Constructs `new BackupJobs(context).scheduleImmediate()` on receiving `com.zegoggles.smssync.BACKUP` | Migrated to inject and call `BackupScheduler.scheduleImmediate()` (IC-2); action string and manifest filter preserved byte-for-byte |
| `app/src/main/java/com/zegoggles/smssync/di/SchedulerModule.kt` (new) | Does not exist | Created; provides `WorkManagerScheduler` as `@Singleton BackupScheduler` binding in release; overridden in debug to bind `CompositeScheduler` |
| `app/src/main/java/com/zegoggles/smssync/service/WorkManagerScheduler.kt` (new) | Does not exist | Created; sole production implementation of `BackupScheduler` port (defined in U-013); maps all port operations to `WorkRequest`s per the DES-MODERNIZATION-005 contract-mapping table |
| `app/src/debug/java/com/zegoggles/smssync/service/CompositeScheduler.kt` (new) | Does not exist | Created in debug source set; delegates to both `LegacyScheduler` and `WorkManagerScheduler`; logs divergence |
| `app/src/debug/java/com/zegoggles/smssync/service/LegacyScheduler.kt` (new) | Does not exist | Created in debug source set; wraps `BackupJobs` for parallel-run |
| `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.java` (new) | Does not exist | Created; four INV test methods (INV-1 through INV-4) plus supplementary AC coverage |
| `app/build.gradle` | References `firebase-jobdispatcher` dependency and `maven.scijava.org` repository (lines 26–27) | `androidx.work:work-runtime-ktx:2.9.x` added; `firebase-jobdispatcher` and scijava mirror NOT yet removed (that is Gate G3, owned by U-017) |

## Existing Behavior to Preserve

- The `com.zegoggles.smssync.BACKUP` action string, the receiver class, and its `<intent-filter>` in `AndroidManifest.xml` must be preserved byte-for-byte; any drift breaks third-party automation.
- The two-stage incoming-SMS debounce: content-URI trigger fires → worker enqueues a delayed incoming-backup work item with delay = `getIncomingTimeoutSecs()`. The worker must NOT back up directly on the raw content-URI change. This mirrors `SmsJobService.onStartJob:76`.
- The `isCallLogBackupAfterCallEnabled()` condition that gates the second URI in the content-trigger constraint must be respected; adding the call-log URI unconditionally changes user-visible behavior.
- The bootup scheduling branch: `scheduleBootup()` must call `cancelAll()` when `!isAutoBackupEnabled()` and schedule regular backup otherwise, with `initialDelay = 60s` (`BOOT_BACKUP_DELAY = BackupJobs.java:56`).
- The `isWifiOnly()` → `UNMETERED` / `CONNECTED` branch must function identically to the legacy `ON_UNMETERED_NETWORK` / `ON_ANY_NETWORK` selection; users who configured wifi-only must not silently receive metered-network backups.
- `scheduleImmediate()` must carry NO network constraint (`Constraints.NONE`), not `CONNECTED`. Changing this silently breaks backups triggered by the public broadcast on devices with no active network at trigger time.
- `requiresCharging` must remain `false`; no charging constraint existed in the legacy scheduler.

## Verification Steps

1. **AC-1 (INV-1 REPLACE / single-flight):** Run `./gradlew :app:testDebugUnitTest --tests "*.WorkManagerSchedulerTest.enqueue_sameName_twice_yieldsOneWorkItem"`. The test must pass. To confirm the assertion is load-bearing, temporarily change `ExistingWorkPolicy.REPLACE` to `ExistingWorkPolicy.KEEP` in `WorkManagerScheduler` and rerun — the test must fail with "expected exactly 1 non-terminal item, found 2" or similar.

2. **AC-2 (INV-3 backoff 30s initial / 300s cap):** Run `./gradlew :app:testDebugUnitTest --tests "*.WorkManagerSchedulerTest.backoffPolicy_isExponential_30s_initialDelay"` and `"*.WorkManagerSchedulerTest.backoffPolicy_capIs300s"`. Both must pass. Temporarily change the initial delay constant from 30 to 31 seconds in `WorkManagerScheduler` — the first test must fail. Restore and change the cap constant from 300 to 301 — the second must fail. Restore originals.

3. **AC-3 (INV-2 network constraints):** Run `./gradlew :app:testDebugUnitTest --tests "*.WorkManagerSchedulerTest.constraint_wifiOnly_isUnmetered"`, `"*.WorkManagerSchedulerTest.constraint_anyNetwork_isConnected"`, `"*.WorkManagerSchedulerTest.constraint_immediate_isNone"`, and `"*.WorkManagerSchedulerTest.constraint_noCharging_inAllCases"`. All must pass. For the immediate-path test, confirm `NetworkType.NOT_REQUIRED` (not `CONNECTED`) is asserted. For the charging test, confirm `requiresCharging == false` is asserted across all three network-type cases.

4. **AC-4 (INV-4 content-URI trigger and pre-24 fallback):** On an API 24+ test device or emulator, run the instrumented test `./gradlew :app:connectedDebugAndroidTest --tests "*.WorkManagerSchedulerTest.contentTrigger_api24plus_smsUriPresent"`. Assert `workSpec.constraints.contentUriTriggers` contains the SMS provider URI with `triggerForDescendants = true`. With `isCallLogBackupAfterCallEnabled() = true`, assert the call-log URI is also present. On API 23 or below (or via Robolectric with `@Config(sdk = 23)`), run `"*.WorkManagerSchedulerTest.contentTrigger_priorApi24_fallsBackToBroadcastPath"` — assert that the triggered SMS-received broadcast path invokes `scheduler.scheduleIncoming()` and produces a `OneTimeWorkRequest` with `initialDelay > 0`.

5. **AC-5 (degenerate-window initialDelay):** Run `./gradlew :app:testDebugUnitTest --tests "*.WorkManagerSchedulerTest.scheduleIncoming_initialDelay_matchesTimeoutSecs"` and `"*.WorkManagerSchedulerTest.scheduleBootup_initialDelay_is60s"`. Both must pass. Temporarily set `incomingTimeoutSecs` to 0 — the first test must detect either zero delay or a sentinel edge case. Restore.

6. **AC-6 (public broadcast contract):** On a device or emulator with the debug build installed, run:
   `adb shell am broadcast -a com.zegoggles.smssync.BACKUP`
   Then query WorkManager state via the app's debug UI or adb shell:
   `adb shell dumpsys jobscheduler | grep smssync`
   or run the instrumented test `./gradlew :app:connectedDebugAndroidTest --tests "*.BroadcastContractTest.backup_broadcast_enqueuesiImmedateWork"`. Assert a work item for the BROADCAST_INTENT unique name is enqueued. Confirm that the action string in `AndroidManifest.xml` is still `com.zegoggles.smssync.BACKUP` (unchanged) via `grep -n "BACKUP" app/src/main/AndroidManifest.xml`.

7. **AC-7 (CompositeScheduler debug-only):** Build a release APK with `./gradlew :app:assembleRelease`. Confirm `CompositeScheduler`, `LegacyScheduler`, and any `firebase-jobdispatcher` imports do NOT appear in the release dex by running `./gradlew :app:assembleRelease` and inspecting with `dexdump` or ProGuard mapping for those class names — they must be absent. Build a debug APK and confirm `CompositeScheduler` is present and callable.

8. **AC-8 (four INV tests pass on test harness):** Run `./gradlew :app:testDebugUnitTest --tests "*.WorkManagerSchedulerTest"`. All four INV test methods (named `inv1_*`, `inv2_*`, `inv3_*`, `inv4_*` or equivalent) must pass. Confirm each is a separate `@Test` method with an independent failure message by temporarily breaking each invariant in isolation and verifying only the corresponding test fails.

9. **IC-3 (no remaining direct BackupJobs construction in non-debug source):** After implementing, run:
   `grep -rn "new BackupJobs\|BackupJobs(" app/src/main/ --include="*.java" --include="*.kt"`
   The result must contain zero matches outside the `LegacyScheduler` wrapper (which is itself debug-only). Any remaining direct call site is a defect.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (app module) | `WorkManagerScheduler.kt`, `LegacyScheduler.kt` (debug), `CompositeScheduler.kt` (debug), `SchedulerModule.kt` (Hilt), broadcast receiver migration, `WorkManagerSchedulerTest` (four INV tests) | Developer |

## Technical Notes

**Single backoff strategy — not two values.** `BackupJobs.java:205` calls `firebaseJobDispatcher.newRetryStrategy(RETRY_POLICY_EXPONENTIAL, 30, 300)` where the Firebase signature is `(policy, initialBackoffSeconds, maximumBackoffSeconds)`. This is one strategy: EXPONENTIAL growth starting at 30s capped at 300s. AC-2 must assert `backoffDelayDuration == 30_000ms` (the WorkManager `setBackoffCriteria` initial value) and that the effective delay does not exceed 300s. WorkManager's own internal cap is `MAX_BACKOFF_MILLIS` (approximately 5 hours), which is much larger than 300s; the 300s ceiling must be re-imposed at the worker layer (owned by U-015, the `BackupWorker`/`RestoreWorker` story) rather than by WorkManager itself. The story must document this split responsibility in Implementation Notes to prevent the 300s cap from being lost in U-015.

**Immediate backup carries NO network constraint.** `BackupJobs.java:197` returns `new int[0]` for `BROADCAST_INTENT` — an empty constraint array. The WorkManager mapping for `scheduleImmediate` must use `Constraints.NONE` (equivalently, `NetworkType.NOT_REQUIRED`). Using `NetworkType.CONNECTED` would silently change behavior: a Tasker-triggered backup on a device temporarily without connectivity would be deferred rather than attempted. INV-2 must encode this as a positive assertion (`NOT_REQUIRED`), not merely absence of `UNMETERED`.

**Content-URI trigger: two-stage debounce, not direct backup.** Today `SmsJobService.onStartJob` (line 72–77), when triggered by the `contentTrigger` job, calls `scheduleIncoming()` to enqueue a delayed follow-up — it does NOT back up directly. The `WorkManagerScheduler.scheduleContentTrigger()` design must preserve this: the content-trigger `PeriodicWorkRequest`'s worker enqueues a `OneTimeWorkRequest(initialDelay = incomingTimeoutSecs)` rather than executing backup logic. If U-015 (the `BackupWorker` story) has not landed, a stub `BackupTriggerWorker` that only enqueues the follow-up may be needed as an interim. The dependency on U-015 for the full execution path must be explicit in Implementation Notes.

**Degenerate execution window.** `BackupJobs.java:162` calls `Trigger.executionWindow(inSeconds, inSeconds)` — start equals end. This is a degenerate window with no flex. The WorkManager mapping is `setInitialDelay(inSeconds, SECONDS)` with no `PeriodicWorkRequest` flex window. Do not add a flex window; it would change semantics. The `AlarmManagerDriver.java:134-135` path confirms only `getWindowStart()` is ever read, never the end.

**`isUseOldScheduler()` branch — deleted, not ported.** The `BackupJobs.java:68-71` ternary that selects `AlarmManagerDriver` vs `GooglePlayDriver` is explicitly NOT ported. WorkManager selects `JobScheduler` (API 23+) or its own `AlarmManager`+`BroadcastReceiver` implementation internally. The `isUseOldScheduler()` preference loses its function; it should be left as a no-op during this story and removed in U-017 or U-018.

**Hilt worker factory.** `WorkManagerScheduler` constructs `WorkRequest`s; it does not construct workers directly. However, `BackupWorker` and `RestoreWorker` (U-015, U-016) require `@HiltWorker` / `@AssistedInject` construction and a `HiltWorkerFactory`. `WorkManagerScheduler` must reference the correct worker class names. The `Configuration.Provider` setup (custom WorkManager initialization, default initializer removed from manifest) is owned by U-024 (Hilt worker factory wire story). U-014 must not break the default initializer path before U-024 lands; the story must document this ordering constraint in Implementation Notes.

**Unique work names.** The unique name per `BackupType` must be stable across app restarts (WorkManager persists work by unique name). Use the `BackupType` enum name as the base (e.g., `"REGULAR"`, `"INCOMING"`, `"BROADCAST_INTENT"`) plus a `"contentTrigger"` tag for content-URI work, matching `BackupJobs.java:57` (`CONTENT_TRIGGER_TAG`) and `setTag(backupType.name())` at `BackupJobs.java:190`. Do not generate names dynamically; a name change between app versions silently abandons scheduled work.

**Broadcast receiver call site — verification gap.** DES-MODERNIZATION-005 flags that the exact receiver class and its manifest `<intent-filter>` for `com.zegoggles.smssync.BACKUP` must be grep-confirmed at implementation time. The implementer must run `grep -rn "BACKUP" app/src/main/AndroidManifest.xml app/src/main/java/` to locate the receiver class and confirm it before migrating the receiver body (IC-2). This is a verification gap from the design and must be resolved and documented in Implementation Notes before the story is submitted for review.

**WorkManager version gate.** `androidx.work:work-runtime-ktx` 2.9.x+ requires the AGP/SDK bump from U-003 (REQ-MODERNIZATION-001). U-014 is blocked until U-003 is merged. The dependency is declared in the story's `dependencies` field.

**Parallel-run divergence logging.** `CompositeScheduler` must log divergences at `DEBUG` level using a structured format that includes: job kind, the legacy constraint/backoff value, the WorkManager constraint/backoff value, and whether they match. Expected divergences (none, per the exact source mapping) must be documented in a companion comment. Any unexpected divergence logged during QA testing of the debug build is a blocker for the binding flip.

## Supporting Documentation

- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md` — AC-3 (four scheduling invariants), AC-4 (public broadcast), AC-5 (legacy removals)
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` — Integration Design (JobDispatcher→WorkManager contract-mapping table); Design Validation (INV-1 through INV-4 test specifications); Incoming-SMS trigger section; Public broadcast contract section; ADR-005-A (WorkManager subsumes AlarmManager); ADR-005-B (CoroutineWorker rewrite); Hilt worker construction section

## Integration Contract References

- `CNTR-MODERNIZATION-004` — `BackupScheduler` port (core → app): operations table, constraint/backoff value object, unique-name convention, cancellation semantics, and `observe` flow. This story produces the sole production implementation of that contract.
- `CNTR-MODERNIZATION-005` — `com.zegoggles.smssync.BACKUP` broadcast external event contract: action string, receiver class, manifest filter, and exported semantics. This story preserves and is bound by that contract at IC-2.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

One-line note: Draft content written against verified source (`BackupJobs.java:57,68-71,88,162,163,171,177-184,188,190,197-205`; `SmsJobService.java:72-77,82-84,147`; `AlarmManagerDriver.java:44,134-135`; `app/build.gradle:8,14,26-27`) — NOT finalized; the load-bearing open item for review is the broadcast receiver call-site verification gap (DES-MODERNIZATION-005 flags this as "scope assumed, not grep-confirmed") — the implementer must grep-confirm the receiver class and manifest filter before IC-2 can be closed, and must document the result in Implementation Notes.
