---
type: story
status: done
sprint: '000001'
artifact_type: user-story
priority: high
complexity: medium
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
  - U-015
  - U-016
change_records: []
platforms: []
tags:
  - gate-G3
  - scheduler-cutover
  - firebase-removal
gate_additions:
  - G3
id: U-017
title: Flip production binding LegacyScheduler→WorkManagerScheduler; delete BackupJobs, AlarmManagerDriver, SmsJobService, and firebase-jobdispatcher; declare Gate G3
pipeline: ''
domain: modernization
requirement_source: authored
updated_at: '2026-06-03T20:52:52.046Z'
resolution: done
---

# U-017: Flip production binding LegacyScheduler→WorkManagerScheduler; delete BackupJobs, AlarmManagerDriver, SmsJobService, and firebase-jobdispatcher; declare Gate G3

## Story

As a maintainer of SMS Backup+ responsible for long-term supply-chain integrity and API-level safety,
I want to perform the single binding flip that replaces `LegacyScheduler` with `WorkManagerScheduler` as the production `BackupScheduler` implementation, and physically delete the four obsolete artifacts (`BackupJobs.java`, `AlarmManagerDriver.java`, `SmsJobService.java`, the `firebase-jobdispatcher` dependency, and the `maven.scijava.org` repository entry) after confirming zero divergence in the parallel run,
so that the shipping binary contains no reference to Firebase JobDispatcher, no `AsyncTask`-based worker, and no `AlarmManagerDriver` fallback — retiring all four dead scheduling mechanisms at once and eliminating the supply-chain risk of the non-Google scijava mirror.

## Acceptance Criteria

**AC-1 — Parallel-run divergence check passes before the flip: no out-of-allow-list divergence logged**

Given a debug build with `CompositeScheduler` active (bound by U-014 / U-015, targeting the instrumented device or emulator running API 24+),
when each of the seven scheduling entry points is exercised in sequence — `scheduleIncoming()`, `scheduleRegular()`, `scheduleContentTrigger()`, `scheduleBootup()` (auto-backup enabled), `scheduleBootup()` (auto-backup disabled, expect `cancelAll()`), `scheduleImmediate()`, and `scheduleRestore(config)` —
then:
- the `CompositeScheduler` divergence log contains zero entries outside the documented allow-list (the allow-list is empty; the source-mapping in DES-MODERNIZATION-005 §Integration Design is exact);
- for each entry point, the `WorkManagerScheduler`-produced `WorkSpec` constraints, backoff policy, initial-backoff duration, existing-work policy, unique-work name, and content-URI trigger URIs match the `LegacyScheduler`-produced values field-for-field;
- specifically: `scheduleImmediate()` produces `NetworkType.NOT_REQUIRED` on both sides (no network constraint — per `BROADCAST_INTENT → new int[0]` at `BackupJobs.java:197`);
- `scheduleRegular()` and `scheduleIncoming()` with `isWifiOnly()=true` produce `NetworkType.UNMETERED` on both sides; with `isWifiOnly()=false` produce `NetworkType.CONNECTED` on both sides;
- no backoff strategy uses a charging constraint on either side (matching verified `jobConstraints`);
- backoff is `BackoffPolicy.EXPONENTIAL`, initial-delay 30 s, effective cap 300 s on both sides (single `(30, 300)` strategy — not two separate strategies);
- `setReplaceCurrent(true)` equivalent is `ExistingWorkPolicy.REPLACE` on the WorkManager side;
- and the divergence check result is recorded as a named verification artifact in this story's Notes before the flip commit is made.

**AC-2 — Production Hilt binding is flipped: `WorkManagerScheduler` is the sole release-build `BackupScheduler` implementation**

Given the repository is on the cutover commit (the binding-flip change),
when a developer inspects the Hilt module that binds `BackupScheduler` in the `app` module (release + debug non-composite flavors),
then:
- the release-build binding resolves `BackupScheduler` to `WorkManagerScheduler`, not to `LegacyScheduler` or `CompositeScheduler`;
- `CompositeScheduler` and `LegacyScheduler` remain present only in the `debug` variant source set and carry no `@Provides` / `@Binds` annotation reachable from the release build graph;
- the Hilt binding is confirmed by running `./gradlew :app:hiltJavaCompileRelease` (or the project's equivalent release Hilt compilation task) with `BUILD SUCCESSFUL` and no unresolved binding errors;
- and no call site in `app/src/main/` constructs `BackupJobs`, `AlarmManagerDriver`, or `SmsJobService` directly — every scheduling call is routed through the injected `BackupScheduler` port.

**AC-3 — `BackupJobs.java` is deleted from the source tree**

Given the cutover commit is applied,
when a developer runs `find app/src -name "BackupJobs.java"` from the repository root,
then the command produces zero lines of output, confirming the file is not present under any source set (main, debug, test, androidTest).

**AC-4 — `AlarmManagerDriver.java` is deleted from the source tree**

Given the cutover commit is applied,
when a developer runs `find app/src -name "AlarmManagerDriver.java"` from the repository root,
then the command produces zero lines of output, confirming the file is not present under any source set.

**AC-5 — `SmsJobService.java` is deleted from the source tree**

Given the cutover commit is applied,
when a developer runs `find app/src -name "SmsJobService.java"` from the repository root,
then the command produces zero lines of output, confirming the file is not present under any source set (including `AndroidManifest.xml` — the `<service>` element for `SmsJobService` is also removed).

**AC-6 — Gate G3 grep gate: zero matches for all four forbidden patterns**

Given the cutover commit is applied and `./gradlew assemble` completes successfully,
when a developer runs each of the following four grep commands from the repository root:

```
grep -rn "com.firebase.jobdispatcher"       app/src/main/java/
grep -rn "AlarmManagerDriver"               app/src/main/java/
grep -rn "SmsJobService"                    app/src/main/java/
grep -rn "extends AsyncTask"                app/src/main/java/com/zegoggles/smssync/service/
```

then every command exits with status 0 and produces zero lines of output — confirming that no production-source reference to `com.firebase.jobdispatcher`, `AlarmManagerDriver`, `SmsJobService`, or `extends AsyncTask` (in the service package) survives the cutover; and the four command outputs are recorded verbatim in this story's Notes as the Gate G3 evidence artifact.

**AC-7 — `firebase-jobdispatcher` dependency and `maven.scijava.org` repository entry are removed from `build.gradle`**

Given the cutover commit is applied,
when a developer reads `build.gradle` (root-level) and `app/build.gradle`,
then:
- neither file contains `firebase-jobdispatcher` in any `implementation`, `api`, `compile`, or `classpath` dependency declaration;
- neither file contains `maven.scijava.org` or `scijava` in any `repositories` block;
- `androidx.work:work-runtime-ktx:2.9.x` (or higher, matching the version required by REQ-MODERNIZATION-001) is declared as an `implementation` dependency in `app/build.gradle`;
- and `./gradlew dependencies --configuration releaseRuntimeClasspath | grep firebase` produces zero lines, confirming `firebase-jobdispatcher` is not pulled in transitively.

**AC-8 — `com.zegoggles.smssync.BACKUP` public broadcast contract is preserved: ADB broadcast test passes**

Given the cutover build is installed on a device or emulator (`adb install -r app/build/outputs/apk/debug/app-debug.apk`),
when a developer executes:

```
adb shell am broadcast -a com.zegoggles.smssync.BACKUP
```

then:
- the command completes without a `SecurityException` or `ActivityManager` error;
- querying `WorkManager.getWorkInfosForUniqueWork("BROADCAST_INTENT")` (or the equivalent unique-work name used by `scheduleImmediate()` in `WorkManagerScheduler`) from the test harness shows at least one work item in `ENQUEUED` or `RUNNING` state within 3 seconds of the broadcast;
- the `AndroidManifest.xml` `<receiver>` entry for the BACKUP broadcast still declares `android:exported="true"` and the `<intent-filter>` `<action android:name="com.zegoggles.smssync.BACKUP"/>` unchanged;
- and no change has been made to the receiver class name, the action string, or the manifest filter — confirming byte-for-byte preservation of the external contract (REQ-MODERNIZATION-005 AC-4; DES-MODERNIZATION-005 §"Public broadcast contract preservation").

**AC-9 — Full CI suite is green post-cutover**

Given all ACs above are satisfied,
when `./gradlew test connectedAndroidTest lint assembleRelease` is run from the repository root,
then:
- all unit tests pass, including the four WorkManager-invariant tests from U-014 (INV-1 through INV-4) and the resumable-restore fault-injection test from U-016;
- `assembleRelease` completes with `BUILD SUCCESSFUL` — confirming the release build compiles without `firebase-jobdispatcher` or `scijava`;
- lint reports zero warnings referencing `AlarmManagerDriver`, `SmsJobService`, `BackupJobs`, or `firebase.jobdispatcher`;
- no test previously passing has been broken;
- and the build artifact's DEX does not contain `com.firebase.jobdispatcher` or `AlarmManagerDriver` class references (verifiable with `./gradlew :app:assembleRelease` followed by `dexdump` or `apkanalyzer dex packages --defined-only`).

**AC-10 — Gate G3 declared: four mechanisms retired; supply-chain risk eliminated**

Given AC-1 through AC-9 are all green and their verification records are documented in this story's Notes,
when a developer or reviewer examines the repository state at the commit that closes this story,
then the following milestone declaration is recorded in this story's Notes and in the team's milestone log:

> **Gate G3 PASSED.**
> Confirmed: (a) parallel-run divergence check showed zero out-of-allow-list divergences before flip (AC-1); (b) production binding is `WorkManagerScheduler` — release build graph resolves correctly (AC-2); (c) `BackupJobs.java` deleted (AC-3); (d) `AlarmManagerDriver.java` deleted (AC-4); (e) `SmsJobService.java` deleted (AC-5); (f) grep-zero gate: zero matches for `com.firebase.jobdispatcher`, `AlarmManagerDriver`, `SmsJobService`, `extends AsyncTask` in service package (AC-6); (g) `firebase-jobdispatcher` and `maven.scijava.org` removed from build (AC-7); (h) ADB BACKUP broadcast test passes against installed build (AC-8); (i) CI green, release build assembles, DEX contains no forbidden class references (AC-9). All four dead scheduling mechanisms retired. Supply-chain risk of the `maven.scijava.org` mirror eliminated.

This declaration must be present before this story is moved to `status: done`.

### Integration Criteria

- [ ] IC-1: The Hilt module binding `BackupScheduler` → `WorkManagerScheduler` is present in `app/src/main/` (not in a debug-only variant source set) and is confirmed by successful Hilt release compilation (`./gradlew :app:hiltJavaCompileRelease`)
- [ ] IC-2: `WorkManagerScheduler.scheduleImmediate()` is reachable from the BACKUP broadcast receiver in production source — trace: `BroadcastReceiver.onReceive()` → injected `BackupScheduler` → `scheduleImmediate()` → `WorkManager.enqueueUniqueWork()`
- [ ] IC-3: All former call sites of `new BackupJobs(context)` in boot receiver, broadcast receiver, and settings UI have been migrated to the injected `BackupScheduler` port — grep `new BackupJobs` in `app/src/main/java/` must produce zero matches
- [ ] IC-4: `SmsJobService` `<service>` element is removed from `app/src/main/AndroidManifest.xml` — grep `SmsJobService` in `app/src/main/AndroidManifest.xml` must produce zero matches

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/BackupJobs.java` | Primary scheduler: constructs `FirebaseJobDispatcher`, maps backup types to `Job` objects via `createBuilder`; contains the `isUseOldScheduler()` ternary (lines 68-71) selecting `AlarmManagerDriver` vs `GooglePlayDriver`; `setReplaceCurrent(true)` on every job (line 188); `newRetryStrategy(EXPONENTIAL, 30, 300)` (line 205) | **Deleted** — entire file removed; all scheduling behavior is already subsumed by `WorkManagerScheduler` (landed in U-014) |
| `app/src/main/java/com/zegoggles/smssync/service/AlarmManagerDriver.java` | Implements `com.firebase.jobdispatcher.Driver`; selected by `isUseOldScheduler()` as an `AlarmManager`-backed fallback; reads `getWindowStart()` only (line 134-135) — the execution window is degenerate | **Deleted** — WorkManager internally owns `AlarmManager`+`BroadcastReceiver` for API 14-22; no hand-written fallback driver required (ADR-005-A) |
| `app/src/main/java/com/zegoggles/smssync/service/SmsJobService.java` | Extends `com.firebase.jobdispatcher.JobService`; `onStartJob()` at lines 82-84 manually instantiates `SmsBackupService` and calls `attachBaseContext(this)` to bypass the API-26 background-start ban; `backupStateChanged()` at lines 110-126 bridges Otto events | **Deleted** — `CoroutineWorker.doWork()` (in `BackupWorker`/`RestoreWorker`, landed in U-015) is the execution context; there is no `JobService` lifecycle bridge |
| `build.gradle` (root) | `maven.scijava.org` repository entry at lines 26-27 (verified): serves `firebase-jobdispatcher:0.8.6` | `repositories` block for `maven.scijava.org` removed |
| `app/build.gradle` | `implementation 'com.firebase:firebase-jobdispatcher:0.8.6'` dependency declared | Dependency declaration removed; `androidx.work:work-runtime-ktx:2.9.x+` retained (added in U-014) |
| Hilt binding module (app module, release flavor) | Binds `BackupScheduler` → `LegacyScheduler` (wrapping `BackupJobs`) prior to this story; `CompositeScheduler` in debug via debug variant binding | Binding flipped: release binding → `WorkManagerScheduler`; debug binding → `CompositeScheduler` (no change to debug variant) |
| `app/src/main/AndroidManifest.xml` | `<service android:name=".service.SmsJobService" ...>` element present with Firebase JobDispatcher `<intent-filter>` | `<service>` element for `SmsJobService` removed |
| Boot receiver, BACKUP broadcast receiver, settings UI (call sites of `new BackupJobs(context)`) | Construct `BackupJobs` directly | Migrated to inject and call `BackupScheduler` port; `new BackupJobs(...)` call sites removed |

## Existing Behavior to Preserve

- `com.zegoggles.smssync.BACKUP` intent action string, receiver class name, `android:exported="true"`, and `<intent-filter>` declaration in `AndroidManifest.xml` are byte-for-byte unchanged — third-party automation (Tasker, etc.) must continue to trigger an immediate backup without modification.
- All four scheduling invariants remain satisfied by `WorkManagerScheduler` (already verified by U-014 invariant tests): INV-1 single-flight REPLACE semantics; INV-2 per-type network constraints including the no-constraint rule for immediate/BROADCAST_INTENT; INV-3 exponential backoff, initial 30 s, cap 300 s; INV-4 content-URI trigger (API 24+) with broadcast fallback (API 14-23).
- The `scheduleContentTrigger()` two-stage debounce is preserved: the content-URI-triggered worker enqueues a delayed `INCOMING` work item with `initialDelay = getIncomingTimeoutSecs()` rather than backing up on the raw content change — matching today's `SmsJobService.onStartJob` (lines 72-77) behavior.
- Boot backup delay of 60 s (`BOOT_BACKUP_DELAY`, `BackupJobs.java:56,92`) is preserved in `WorkManagerScheduler.scheduleBootup()` via `OneTimeWorkRequest(initialDelay = 60, SECONDS)`.
- `scheduleBootup()` cancels all work when `!isAutoBackupEnabled()`, matching the current `BackupJobs.scheduleBootup()` branch.
- Durable restore checkpoint behavior (landed in U-016) is unaffected — this story does not modify `RestoreWorker` or checkpoint storage.

## Verification Steps

**AC-1 — Parallel-run divergence check:**
1. Install the debug build (with `CompositeScheduler` active from U-014/U-015) on a device or emulator running API 24+.
2. Using the WorkManager test harness or a debug instrumented test, invoke each entry point in sequence: `scheduleIncoming()`, `scheduleRegular()` (with `isWifiOnly()` both true and false), `scheduleContentTrigger()`, `scheduleBootup()` (auto-backup on), `scheduleBootup()` (auto-backup off), `scheduleImmediate()`, `scheduleRestore(config)`.
3. After each invocation, capture the `CompositeScheduler` divergence log via Logcat tag (e.g., `CompositeScheduler`).
4. Assert zero log entries outside the allow-list (expected: none).
5. Record the full divergence log (confirming zero entries) in this story's Notes before proceeding to the flip commit.

**AC-2 — Binding flip confirmed:**
1. After the flip commit, run `./gradlew :app:hiltJavaCompileRelease` and confirm `BUILD SUCCESSFUL`.
2. Inspect the Hilt-generated component source in `app/build/generated/hilt/component_sources/` for the release variant; confirm `WorkManagerScheduler` appears as the bound `BackupScheduler` implementation.
3. Run `grep -rn "LegacyScheduler" app/src/main/java/` — confirm zero matches (LegacyScheduler is debug-only).
4. Run `grep -rn "new BackupJobs" app/src/main/java/` — confirm zero matches.

**AC-3 through AC-5 — File deletions:**
1. Run `find app/src -name "BackupJobs.java"` — confirm zero lines.
2. Run `find app/src -name "AlarmManagerDriver.java"` — confirm zero lines.
3. Run `find app/src -name "SmsJobService.java"` — confirm zero lines.
4. Open `app/src/main/AndroidManifest.xml` and confirm no `SmsJobService` `<service>` element is present.

**AC-6 — Gate G3 grep gate (four commands, all must produce zero output):**
```
grep -rn "com.firebase.jobdispatcher"    app/src/main/java/
grep -rn "AlarmManagerDriver"            app/src/main/java/
grep -rn "SmsJobService"                 app/src/main/java/
grep -rn "extends AsyncTask"             app/src/main/java/com/zegoggles/smssync/service/
```
Record each command and its complete (zero-line) output verbatim in this story's Notes.

**AC-7 — Build file cleanup:**
1. Open `build.gradle` and confirm no `maven.scijava.org` entry under any `repositories` block.
2. Open `app/build.gradle` and confirm no `firebase-jobdispatcher` dependency line.
3. Confirm `work-runtime-ktx` is present in `app/build.gradle`.
4. Run `./gradlew :app:assembleRelease` — confirm `BUILD SUCCESSFUL`.
5. Run `./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -i firebase` — confirm zero output.

**AC-8 — ADB BACKUP broadcast test:**
1. Install the release build: `adb install -r app/build/outputs/apk/release/app-release-unsigned.apk` (or the debug equivalent if the release APK is not yet signed).
2. Run: `adb shell am broadcast -a com.zegoggles.smssync.BACKUP`
3. Observe Logcat or use the WorkManager status query (`adb shell dumpsys jobscheduler` is not applicable; use `WorkManager.getInstance(context).getWorkInfosForUniqueWork("BROADCAST_INTENT").get()` from an instrumented test) to confirm a work item is enqueued within 3 seconds.
4. Open `app/src/main/AndroidManifest.xml` and confirm the BACKUP receiver `<action>` string and `android:exported="true"` are unchanged.
5. Record the `am broadcast` output and the work-info query result in this story's Notes.

**AC-9 — CI green:**
1. Run `./gradlew test connectedAndroidTest lint assembleRelease` from the repository root.
2. Confirm `BUILD SUCCESSFUL` and zero test failures.
3. Confirm the four WorkManager invariant tests (INV-1 through INV-4, from U-014) are present and green.
4. Confirm the resumable-restore fault-injection test (from U-016) is present and green.
5. Confirm no lint errors reference the deleted classes or the removed dependency.
6. Optional: run `apkanalyzer dex packages --defined-only app/build/outputs/apk/release/app-release-unsigned.apk | grep firebase` — confirm zero lines.

**AC-10 — Gate G3 declared:**
1. With AC-1 through AC-9 all confirmed green and their outputs recorded in this story's Notes, compose the Gate G3 declaration from AC-10.
2. Append the declaration to this story's Notes.
3. Record the Git commit SHA at which the declaration applies.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (all) | Parallel-run divergence check; binding flip commit; deletion of three Java files; `build.gradle` cleanup; `AndroidManifest.xml` `<service>` removal; migration of `BackupJobs` call sites to injected port; Gate G3 grep gate; ADB broadcast test; CI run; Gate G3 declaration | Developer |

## Technical Context

- This is the final cutover step in MU-005 ("THE SPINE"). The implementation work — the `BackupScheduler` port (U-013), `WorkManagerScheduler` adapter (U-014), `CoroutineWorker` rewrite (U-015), and durable restore checkpoint (U-016) — is complete before this story begins. This story performs the flip and the cleanup only.
- **The parallel-run check (AC-1) is a hard pre-condition for the flip commit.** The `CompositeScheduler` must log zero divergence across all entry points before the binding-flip commit is authored. The divergence record is evidence that justifies the flip; without it the cutover is speculative.
- **"Four mechanisms, one flip" property:** because every call site in production already routes through the injected `BackupScheduler` port (established by U-013/U-014), the entire cutover is a single Hilt module change plus three file deletions. No call-site rewrite is needed in this story beyond migrating any remaining `new BackupJobs(context)` construction sites discovered during implementation.
- **Open verification item from DES-MODERNIZATION-005:** the exact set of `BackupJobs` call sites outside `SmsJobService` (boot receiver, broadcast receiver, settings) is noted as "scope assumed — not yet grep-verified" in the design. The developer must grep `new BackupJobs` across `app/src/main/java/` before the flip and confirm each site is already migrated (or migrate it in this story). IC-3 above encodes this obligation.
- **Backoff cap fidelity:** Firebase's `newRetryStrategy(RETRY_POLICY_EXPONENTIAL, 30, 300)` is a single strategy where 30 is initial-backoff seconds and 300 is maximum-backoff seconds. WorkManager expresses initial backoff as `setBackoffCriteria(EXPONENTIAL, 30, SECONDS)` and internally caps at a much larger value (~5 h); the 300 s ceiling must be re-enforced at the worker (in `BackupWorker.doWork()`, landed in U-015). INV-3 in the U-014 test suite verifies this. This story does not need to re-implement the cap — it only needs to confirm the test remains green (AC-9).
- **No charging constraint anywhere:** the current `jobConstraints` emits network type only (`BackupJobs.java:195-201`). `WorkManagerScheduler` must likewise never set `requiresCharging`. The parallel-run check (AC-1) will surface any accidental constraint introduction.
- **scijava removal safety check:** before removing the `maven.scijava.org` entry, grep the full dependency tree for any other artifact using that mirror (`./gradlew :app:dependencies | grep scijava`). If any other artifact resolves through scijava, coordinate its migration separately. In this codebase, `firebase-jobdispatcher:0.8.6` is the only known consumer (per DES-MODERNIZATION-005 §ADR-005-A).
- **`SmsJobService` manifest removal:** the `<service>` element for `SmsJobService` in `AndroidManifest.xml` carries a Firebase JobDispatcher `<intent-filter>` that must also be removed. Leaving the manifest entry while deleting the class would cause a build error; this is caught by AC-9.
- **Rollback path:** because the port surface is binding-identical, reverting the flip requires only a single Hilt module revert (no call-site changes). The three deleted files must be restored from Git history. Document the rollback procedure in the Notes if the team requires it.
- **Relevant source coordinates (pre-cutover, for deletion reference):**
  - `app/src/main/java/com/zegoggles/smssync/service/BackupJobs.java` — `FirebaseJobDispatcher`; `Driver` strategy at lines 66-72; retry at line 205; constraint mapping at lines 195-201
  - `app/src/main/java/com/zegoggles/smssync/service/AlarmManagerDriver.java` — `implements com.firebase.jobdispatcher.Driver`; `getWindowStart()` at lines 134-135
  - `app/src/main/java/com/zegoggles/smssync/service/SmsJobService.java` — `extends com.firebase.jobdispatcher.JobService`; lifecycle bypass at lines 82-84; Otto bridge at lines 110-126
  - `build.gradle` lines 26-27: `maven { url "https://maven.scijava.org/content/repositories/public" }` (verified per DES-MODERNIZATION-005 §Context)
  - `app/build.gradle`: `implementation 'com.firebase:firebase-jobdispatcher:0.8.6'` (verified)

## Supporting Documentation

- REQ-MODERNIZATION-005 §"Acceptance Criteria" — AC-1 (single port), AC-4 (broadcast), AC-5 (removals); this story directly satisfies all three
- DES-MODERNIZATION-005 §"ADR-005-A" — rationale for deleting `AlarmManagerDriver` and the scijava mirror; WorkManager internal scheduler subsumption
- DES-MODERNIZATION-005 §"Branch-by-abstraction parallel-run" — `CompositeScheduler` divergence check that gates the flip
- DES-MODERNIZATION-005 §"Dependency gates" — Gate G3 grep gate definition; hard predecessors (DES-001, DES-003/G2, REQ-001)
- DES-MODERNIZATION-005 §"Removal verification (AC-5, Gate G3)" — the four grep patterns and build-file checks
- DES-MODERNIZATION-005 §"Public broadcast contract — ADB test (AC-4)" — ADB broadcast test procedure
- DES-MODERNIZATION-005 §"Integration Design — Honesty notes" — the three corrected misreadings that affect INV-2 and INV-3 verification
- DES-MODERNIZATION-005 §"Parallel-run divergence check (pre-cutover confidence)" — entry points to exercise and assertion method

## Integration Contract References

- CNTR-MODERNIZATION-004 — `BackupScheduler` port operations contract (core → app): this story confirms the release binding satisfies the port surface; IC-1 and IC-2 verify the binding is wired
- CNTR-MODERNIZATION-005 — `com.zegoggles.smssync.BACKUP` broadcast contract (external event): AC-8 directly verifies the broadcast action, receiver, exported flag, and intent-filter are byte-for-byte unchanged; IC-4 verifies no manifest drift

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

This story is the production cutover and cleanup step for MU-005 (WorkManager Scheduler Spine). No new scheduling logic is written here — all implementation is in U-013 through U-016. The developer's deliverables are: (1) parallel-run divergence record (pre-flip evidence); (2) three Java file deletions plus manifest `<service>` removal; (3) `build.gradle` / `app/build.gradle` cleanup; (4) Hilt binding flip commit; (5) migration of any remaining `new BackupJobs(context)` call sites; (6) Gate G3 grep-gate outputs recorded verbatim; (7) ADB broadcast test result recorded; (8) CI green confirmation; (9) Gate G3 declaration.

Gate G3 declaration will be recorded here upon completion:

> [Gate G3 declaration — to be recorded by developer upon AC-1 through AC-9 confirmation, including Git commit SHA]
