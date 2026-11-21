---
type: bug
status: ready
artifact_type: bug
severity: medium
priority: high
complexity: medium
parallel_eligible: true
iteration: 1
requirements:
  - REQ-MODERNIZATION-005
design_docs:
  - DES-MODERNIZATION-005
  - DES-MODERNIZATION-014
integration_contracts:
  - CNTR-MODERNIZATION-004
dependencies: []
related_items:
  - CR-015
  - BUG-021
  - BUG-022
  - REQ-MODERNIZATION-016
  - U-014
  - U-015
  - U-054
  - U-065
platforms: []
tags:
  - workmanager
  - scheduler
  - content-trigger
  - incoming-backup
  - inv-2
  - inv-4
  - test-coverage
id: BUG-023
title: The incoming-SMS backup started by the content trigger is enqueued without the network, encryption-degraded and auto-backup rules that scheduleIncoming() applies, and the worker that does it has no test
domain: build
origin: code-review
updated_at: '2026-11-21T09:40:09.113Z'
---

# BUG-023: The incoming-SMS backup started by the content trigger is enqueued without the network, encryption-degraded and auto-backup rules that scheduleIncoming() applies, and the worker that does it has no test

## Description

An incoming text message normally starts a backup in two stages (CNTR-MODERNIZATION-004 INV-4). Stage 1: a content-URI trigger over the SMS provider fires a small worker, `BackupTriggerWorker`. Stage 2: that worker enqueues a delayed one-off `BackupWorker` as unique work `INCOMING` (the delay, `incomingTimeoutSecs`, is the debounce), replacing any pending one.

The contract, the worker's own class comment, the parent design and a test comment all say stage 2 goes through the scheduler's `scheduleIncoming()`. It does not. The worker builds the request itself. `scheduleIncoming()` does four things before and while it enqueues that the worker's copy does not do:

1. It gives the request a network constraint: unmetered when "Wi-Fi only" is set, any connection otherwise.
2. It gives the request an explicit exponential backoff of 30 seconds.
3. It enqueues nothing when the secret store reports the encryption-degraded state (story U-054, finding SE-002).
4. It enqueues nothing when auto backup is off or the incoming delay is 0 or less.

The two requests share the unique-work name, the tag, the delay and the replace policy, so they look alike in a log. They differ in behaviour: on the trigger path the backup is not held back by the network setting, and is not stopped by the degraded or auto-backup state.

`BackupWorker` does not repeat any of these checks itself (see Evidence, "What BackupWorker re-checks"), so the missing rules are not made up for later. Separately, no unit test references `BackupTriggerWorker`, so nothing in the suite would notice a change to it. DES-MODERNIZATION-005 names such a test as the mitigation for the debounce risk.

The bug was found by the Solution Architect's review of change record CR-015 (code-review origin). CR-015 deliberately leaves the contract sentence as it is and sends this divergence to a bug (CR-015 Scope Concern 2, `sdlc/artifacts/build/change-records/CR-015-cntr-004-content-trigger-pre-api-24-fallback-removed.md`).

### Facts confirmed against the code, and two corrections to the report that raised this

Read at the working tree of the branch `sdlc/modernization-plan` (HEAD `9e983665` plus uncommitted edits). Line numbers move; they are cited so the claim can be checked, not as stable addresses.

- Confirmed: `BackupTriggerWorker.doWork` (`app/src/main/java/com/zegoggles/smssync/worker/BackupTriggerWorker.kt:53-74`) builds a `OneTimeWorkRequest` for `BackupWorker` with `setInitialDelay(incomingTimeoutSecs, SECONDS)` and `addTag("INCOMING")` (`:61-64`) and enqueues it as unique work `INCOMING` with `REPLACE` (`:66-71`). Its class comment says stage 2 is "via scheduleIncoming()" (`:39`).
- Confirmed: `WorkManagerScheduler.scheduleIncoming()` is at `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt:134-159`.
- Correction 1 (path): the test file with the comment is `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt` (package `service`, not `scheduler`); the comment is at `:537`.
- Correction 2 (claim about auto backup): switching auto backup off does cancel the content trigger. The settings-changed event runs `rescheduleJobs()`, which calls `cancelAll()` first (`app/src/main/java/com/zegoggles/smssync/App.java:279-293`, `:373-385`), and `cancelAll()` cancels unique work `REGULAR` and `CONTENT_TRIGGER` (`WorkManagerScheduler.kt:410-415`). So the auto-backup gap is narrower than "the periodic trigger is still registered": it is an already-running or already-fired trigger, and the pending `INCOMING` work, which `cancelAll()` does not cancel on either path (see Impact of each missing check).

## Steps to Reproduce

No emulator is needed for (a) to (c); they are unit-level and none has been run for this artifact (Gradle was not run, by instruction). They describe the tests the fix must add (AC-1 to AC-8), and what they show on today's code is read from the code, not observed.

Setup for (a) to (c): Robolectric test with `WorkManagerTestInitHelper`, as `WorkManagerSchedulerTest` does (`WorkManagerSchedulerTest.kt:42-81`); `Preferences` and `SecretStore` as mocks or real objects over the test context. Run `BackupTriggerWorker.doWork` directly, for example with `TestListenableWorkerBuilder` or `TestWorkerBuilder` (one of the two appears in six existing test files for the other workers, for example `app/src/test/java/com/zegoggles/smssync/service/BackupWorkerTest.kt`; which one, and whether it suits a plain `Worker`, was not read).

**(a) Network setting.**
1. Set Wi-Fi only on (`isWifiOnly` true) and auto backup on, incoming delay 60.
2. Run the trigger worker once.
3. Read the work info of unique work `INCOMING` (`getWorkInfosForUniqueWork`).
4. Observe on today's code: `constraints.requiredNetworkType` is `NOT_REQUIRED`. `scheduleIncoming()` in the same state gives `UNMETERED` (existing test pattern: `WorkManagerSchedulerTest.kt:172`, `:191`).

**(b) Encryption-degraded state.**
1. Make `isEncryptionDegraded()` return true; auto backup on, delay 60.
2. Run the trigger worker.
3. Observe on today's code: unique work `INCOMING` is enqueued. `scheduleIncoming()` in the same state enqueues nothing and returns null (`WorkManagerScheduler.kt:135-138`).

**(c) Auto backup off at run time.**
1. Make `isAutoBackupEnabled` false (or the incoming delay 0), then run the trigger worker.
2. Observe on today's code: unique work `INCOMING` is enqueued (with a delay of 0 seconds if the delay is 0). `scheduleIncoming()` enqueues nothing (`:140-144`).

**(d) Device check, optional and not a gate.** A debug build, auto backup on, Wi-Fi only on, the device on mobile data only: one ordinary SMS, then read the log for the `INCOMING` run. Not run; see Not verified.

## Expected Behavior

The follow-up backup that the content trigger starts is subject to the same rules as the one `scheduleIncoming()` starts: the network setting decides which networks it may run on, and it is not enqueued at all when the app is in the encryption-degraded state or auto backup is off. The two-stage debounce is unchanged: nothing is backed up at the moment of the raw change; the delayed unique work replaces a pending one. The contract text and the code say the same thing. `BackupTriggerWorker` has tests.

## Actual Behavior

The follow-up request has no network constraint, no explicit backoff, and is enqueued whatever the degraded and auto-backup state is. The contract sentence and the code disagree. No test covers the worker.

## Evidence

### The two requests, side by side

| Property | `scheduleIncoming()` (`WorkManagerScheduler.kt`) | `BackupTriggerWorker.doWork` (`BackupTriggerWorker.kt`) |
|---|---|---|
| Encryption-degraded gate | Returns null, enqueues nothing (`:135-138`) | None |
| Auto-backup-off gate | Returns null (`:140-144`) | None |
| Incoming delay 0 or less | Returns null (`:140`, `:144`) | Not checked; `setInitialDelay(incomingTimeoutSecs)` as read (`:55`, `:62`) |
| Network constraint | `networkConstraints(INCOMING)`: `UNMETERED` if `isWifiOnly`, else `CONNECTED` (`:147`, helper `:471-481`) | None set (`:61-64`); WorkManager's default, not required |
| Backoff | `EXPONENTIAL`, 30 s (`:148`, constant `:103`) | Not set; the library default applies. The Solution Architect read it as `EXPONENTIAL`, 30 000 ms in the 2.12.0 sources (`WorkSpec.kt:62`, `:65`; `WorkRequest.kt:384`). For 2.9.1 the constant name was seen but not its value. So this row differs in code, not, as far as read, in effect |
| Initial delay | `incomingTimeoutSecs` (`:149`) | `incomingTimeoutSecs` (`:55`, `:62`) |
| Tag | `INCOMING` (`:150`) | `INCOMING` (`:63`) |
| Unique name and policy | `INCOMING`, `REPLACE` (`:154`) | `INCOMING`, `REPLACE` (`:67-70`) |
| Input data | None | None |
| Where the preferences come from | Injected `Preferences` and `SecretStore` (`:80-84`) | `Preferences(applicationContext)` built in `doWork` (`:54`); no `SecretStore` is available to it (plain `Worker`, `:48-51`; noted before as W-6, `sdlc/artifacts/build/reviews/consolidated-2026-06/code-review.md:222`) |
| Result | Returns a `ScheduledJob` or null | Returns `Result.success()` (`:73`) |

The two requests carry no input data. `BackupWorker` finds its backup type from the tag (`inferBackupType`, `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt:500-506`), which is `INCOMING` for both, so the type does not differ.

### What BackupWorker re-checks

`BackupWorker.doWork` (`BackupWorker.kt:118-172`) sets the foreground notification (`:123`), applies the backoff cap of 300 s using the run attempt and the 30 s base (`:128-137`), infers the type (`:142`), builds the transport and runs the backup (`:150-163`). It does not read `isWifiOnly`, `isAutoBackupEnabled` or `isEncryptionDegraded`: a search of `BackupWorker.kt` for those names finds nothing. Across `app/src/main`, `isWifiOnly` is read only by the scheduler (`WorkManagerScheduler.kt:476`, `:486`), the preference getter (`Preferences.java:173`) and a settings screen (`activity/fragments/MainSettings.java:146`); `isEncryptionDegraded` is read only by the scheduler and by `MainViewModel.kt:106`, `:130`, `:166` (the manual backup and restore starts). So for an `INCOMING` run, the scheduler's gates are the only gates.

### Impact of each missing check

| Missing on the trigger path | What it does on the trigger path | Established or not |
|---|---|---|
| Network constraint | The backup runs when the delay ends on whatever network the device has, metered included, although "Wi-Fi only" is set. With no connection, the transport is expected to fail and the run to retry (`BackupWorker.kt:164-167`, `:272-275` return `Result.retry()` on `MailException`) | Absence of the constraint and of any other Wi-Fi check: established. That an offline connect raises `MailException`: not read |
| Encryption-degraded gate | An `INCOMING` backup is started on a device whose credentials are in plaintext. U-054's stated reason for the gate: a degraded device "must not enqueue automatic backup work" (`WorkManagerScheduler.kt:130-133`, `:305-309`) | That the work is enqueued: established. What `BackupWorker` then does with plaintext credentials: not read |
| Auto-backup gate | Narrow, see Correction 2: switching auto backup off cancels the trigger registration but not a pending `INCOMING` work. The gap is a trigger that fires or is running when auto backup is switched off, or any path that changes auto backup without emitting the settings-changed event | The cancellation: established (`App.java:373-385`, `WorkManagerScheduler.kt:411-415`). Whether every path emits the event: not read |
| Delay 0 or less | `rescheduleJobs()` registers the trigger only when the delay is above 0 (`App.java:381-383`), so a trigger worker normally sees a positive delay. If the delay is changed to 0 without the trigger being re-registered, a raw content change would enqueue an `INCOMING` backup with no delay, which is the debounce lost | That the registration check exists: established. Whether changing the delay re-registers: not read |
| Backoff | If the library default differs from exponential 30 s, a retry runs on a different schedule; the 300 s cap in `BackupWorker.kt:128-137` assumes the 30 s base | Not read |

### Where the sentences are

- Contract `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md:384-385`: "On trigger, the worker does NOT back up directly — it calls scheduler.scheduleIncoming(), enqueuing a delayed one-off (delay = getIncomingTimeoutSecs()). INV-4 two-stage debounce." CR-015 cites this as `:378`; the line moved while CR-015 was being applied. The contract's Validation Rule 5 (`:277`) is narrower and the code meets it as written: the worker "MUST enqueue the delayed incoming-backup (delay = `getIncomingTimeoutSecs()`)". Rule 2 (`:274`) says INV-1 to INV-4 "hold for every enqueue path", and the trigger's enqueue path does not meet INV-2 (network rule per type).
- Worker class comment `BackupTriggerWorker.kt:39`. Scheduler class comment `WorkManagerScheduler.kt:58-63` and method comment `:212-213` say only that the worker "enqueues a delayed [BackupWorker]", which is true.
- Test comment `WorkManagerSchedulerTest.kt:537-538`: "The content-URI trigger worker would ultimately call scheduleIncoming(), but the gate must fire before any work is enqueued." The test (`:540-551`) only checks that `scheduleContentTrigger()` registers nothing when degraded.
- DES-MODERNIZATION-005 (`sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md`): `:133-138` says the legacy job service "does NOT back up directly — it calls `scheduleIncoming()` to enqueue a delayed follow-up", and that for WorkManager "the content-trigger work enqueues the delayed incoming-backup work; the design must keep the debounce". The second sentence does not say the call is `scheduleIncoming()`; the first describes the legacy code. `:434` lists `scheduleIncoming()` as "network per `isWifiOnly()`". The Risks row at `:495` (line numbers corrected by the Solution Architect; this file had `:442` and `:504`, and CR-015 gave `:503`) names the mitigation "INV-4 test asserts the debounce delay"; no such test exists (below).
- Design DES-MODERNIZATION-014 does not mention `BackupTriggerWorker` in the lines read here; CR-015 Scope Concern 2 (`:712-719`) says this bug is to be fixed before story S4 so that the REQ-MODERNIZATION-016 AC-8 baseline includes the worker.

### No test references the worker

A search of `app/src/test` for `BackupTriggerWorker` (and for "Stage 2" and "via scheduleIncoming") finds no reference other than the comment at `WorkManagerSchedulerTest.kt:537` (which says "content-URI trigger worker", not the class name). The JaCoCo floor for its package is `0.00`, with the comment "MEASURED 0% — BackupTriggerWorker untested; TODO raise after dedicated tests" (`app/build.gradle:575-587`). The existing INV-3 tests do not read the backoff either: they check a constant and that the work was enqueued (`WorkManagerSchedulerTest.kt:259-290`). A search for `initialDelay` in the scheduler test finds only comment lines (`:387-394`), so no test asserts the `INCOMING` delay length for either path.

### Observation, with its limits: the SMS_RECEIVED receiver is not delivered (a separate defect, not fixed by this bug)

Correction by the Solution Architect (2026-11-21). The analyst's heading said "may never run (not a second defect)". The Suggested Approach (section "Severity, and the SMS-received receiver") establishes from the code that the receiver is not delivered `SMS_RECEIVED`. The causes are the manifest's `exported="false"`, added in U-001 (commit `50f3b34f`), and a `RECEIVE_SMS` permission that is never requested at runtime. That is a defect of its own, recorded there for a separate change record. A second emulator run on 2026-11-21 (`sdlc/artifacts/build/sprints/sprint-022/U-066/evidence/IC1-emulator.txt:27`) was a debuggable build: 0 `onReceive(` lines and 0 `scheduleIncoming` lines, while the trigger line appeared and its `INCOMING` run followed 60 s later. The analyst's text below is kept as written for the record.

CR-015 (sections Evidence 3 and Scope Concerns 1) records one run on an Android 16 emulator on 2026-11-20 (story U-065's emulator check): an injected SMS produced the trigger worker's log line ("content-URI trigger fired; enqueueing delayed incoming backup with initialDelay=60s") and no line from the receiver. Facts read here:

- `SmsBroadcastReceiver` is declared `android:exported="false"` with `android:permission="android.permission.BROADCAST_SMS"` and an intent filter for `android.provider.Telephony.SMS_RECEIVED` (`app/src/main/AndroidManifest.xml:146-153`); the app holds `RECEIVE_SMS` (`:17`). Whether the platform delivers that broadcast to a receiver declared this way is not established.
- What the receiver logs (`app/src/main/java/com/zegoggles/smssync/receiver/SmsBroadcastReceiver.java`): `onReceive(` only through `Log.v` guarded by `LOCAL_LOGV` (`:49`), which is true in debug builds only (per CR-015, `App.java:92`); when the message is accepted, nothing of its own; when not set up, "Received SMS but not set up to back up." (`:62`) or the `autoBackup=...` line (`:81`). The accepted path's sign of delivery is the scheduler's `WorkManagerScheduler.scheduleIncoming: enqueued INCOMING` line (`WorkManagerScheduler.kt:156`).
- So the missing line proves little unless the run was a debug build (CR-015 says the build type and whether the scheduler line appeared are not known).

**Conditional weight.** If the receiver is in fact never delivered the broadcast, the content trigger is the only incoming path, and this bug's defects apply to every automatic incoming backup, not to a second path beside a healthy one. That raises its weight; it does not change the acceptance criteria. If the receiver is delivered, the two paths end in the same unique work `INCOMING` with `REPLACE`, so the receiver's (checked) enqueue and the trigger's (unchecked) enqueue replace each other, and which one's request ends up running depends on order (see AC-8).

What would confirm it: a debug build on a device or emulator, auto backup on, login set and first backup done, one ordinary SMS; filter the log (tag `SMSBackup+`) for the receiver's `onReceive(` line and for the scheduler's `scheduleIncoming: enqueued INCOMING` line. Both present: the receiver is delivered. Only the trigger worker's line: the receiver is not delivered (for that build, that Android level, that message). Neither is a claim about other levels. This check is Verification Step 9 and is not an acceptance gate. (Solution Architect: after this fix the trigger path also logs `scheduleIncoming: enqueued INCOMING`, so on the fixed build that line no longer shows that the receiver was delivered. Only the receiver's own lines, `onReceive(`, "Not set up to back up." or "Received SMS but not set up to back up.", do.)

## Acceptance Criteria

**Product owner's decision on the SMS receiver, 2026-11-21 (added by the orchestrator; decision appendix, Record 20, and ruling 56).** Item PO-3 of this bug's Suggested Approach was put to the product owner, who answered: "Remove it (Recommended)". The receiver is removed through a change record and its own small story, placed after story U-072. This bug's fix story (U-069) does not touch the receiver.


The criteria state behaviour, not mechanism. The contract already fixes what the trigger does: it does not back up on the raw change and it enqueues the delayed incoming backup (CNTR-MODERNIZATION-004 Rule 5, INV-4). "Same as `scheduleIncoming()`" below is a requirement on the resulting work, as read from the work manager, not on which function is called. Every test named is a unit test under Robolectric with `WorkManagerTestInitHelper`, in the style of `WorkManagerSchedulerTest.kt`, with the trigger worker run through `TestListenableWorkerBuilder` or an equivalent; the Solution Architect fixes the harness. In every case the preferences and secret-store state are set after `scheduleContentTrigger()` has registered the trigger and before the trigger worker runs, because the rules must hold at run time, not only at registration.

- [ ] AC-1: With Wi-Fi only on, after the trigger worker runs, unique work `INCOMING` exists and its required network type is `UNMETERED`. Checked by a unit test that reads the constraints of the work info (same reading as `WorkManagerSchedulerTest.kt:172`). The test fails on HEAD with `NOT_REQUIRED`.
- [ ] AC-2: With Wi-Fi only off, after the trigger worker runs, the required network type of `INCOMING` is `CONNECTED` (not `NOT_REQUIRED`). Checked as AC-1. The test fails on HEAD.
- [ ] AC-3: Apart from where it came from, the request the trigger path leaves in the work manager equals the one `scheduleIncoming()` leaves, for each of Wi-Fi only on and off: same unique name, same tag set, same initial delay (`incomingTimeoutSecs`), same constraints, same backoff policy and delay, same (empty) input data. Checked by a test that runs each path in turn against the same preferences and compares the work info fields the library exposes. The existing INV-3 tests do not read backoff (`WorkManagerSchedulerTest.kt:259-290`), and the work info may not expose it in the library version in use; if it cannot be read, the story states how backoff equality is shown instead (for example by one shared definition of the request that both paths use, shown by the test that changes the definition and fails both). Backoff equality is not claimed as verified until that is stated.
- [ ] AC-4: With the encryption-degraded state reported by the secret store, after the trigger worker runs, no `INCOMING` work is enqueued (the unique work has no entry, or none newer than before the run), and the worker returns the same result type it returns today on success. Checked by a unit test. Fails on HEAD (work is enqueued).
- [ ] AC-5: With auto backup off, after the trigger worker runs, no `INCOMING` work is enqueued. Checked by a unit test. Fails on HEAD.
- [ ] AC-6: With an incoming delay of 0 or less, after the trigger worker runs, no `INCOMING` work is enqueued (no zero-delay backup on a raw change). Checked by a unit test for delay 0 and for a negative delay. Fails on HEAD.
- [ ] AC-7: The debounce is kept (INV-4, CNTR-MODERNIZATION-004 Rule 5). After the trigger worker runs with the allowed state, `BackupWorker` has not run; unique work `INCOMING` is in the enqueued state with an initial delay equal to `incomingTimeoutSecs`; it is not running or succeeded. Checked by a unit test that does not advance the test clock. This is the "INV-4 test asserts the debounce delay" that DES-MODERNIZATION-005 `:495` names and no test provides today; it also asserts the delay length, which no test does for either path.
- [ ] AC-8: Single flight. Running the trigger worker twice leaves exactly one non-terminal `INCOMING` work, and the first is cancelled and the second carries a fresh delay. Running the trigger worker and then `scheduleIncoming()` (and the reverse order) also leaves exactly one non-terminal `INCOMING` work (the contract's own statement at `:388-389`: the receiver's follow-up "collapses with the content trigger's follow-up"). Checked by a unit test per order. Passes today for the trigger-only case; the point is that the fix does not break it.
- [ ] AC-9: `BackupTriggerWorker` has direct unit tests covering AC-1 to AC-8, and each fails when the corresponding behaviour is removed (the story records the one-line change it made to see each fail, as the scheduler tests do under "Failure injection"). The JaCoCo line floor for `com.zegoggles.smssync.worker*` in `app/build.gradle` is raised from `0.00` to a value the story states and the measured coverage supports, and the "untested" comment and TODO there (`:579`, `:586`) are replaced. No other per-package floor is lowered. Checked by reading `app/build.gradle` and by `./gradlew :app:jacocoTestCoverageVerification` exiting 0.
- [ ] AC-10: The contract and the code agree, by one of two routes that the Solution Architect chooses and the story names. Route A: the code is changed so that the sentence at CNTR-MODERNIZATION-004 `:384-385` ("it calls scheduler.scheduleIncoming()") is true, and the contract is left unchanged. Route B: a change record changes that sentence (and the contract's Rule 5 if needed) to state the trigger worker's obligations directly: the network rule of INV-2, the degraded gate, the auto-backup gate, the delay rule; and the code meets that text. Under either route, the four places that carry the "via scheduleIncoming()" wording are consistent with the code in the same change: the contract (`:384-385`, and `:372-376` if Route B touches it), the worker comment (`BackupTriggerWorker.kt:39`), the test comment (`WorkManagerSchedulerTest.kt:537-538`), and DES-MODERNIZATION-005 (`:133-138` and the note at `:495` once the test exists). Checked by reading those places against the code and the test of AC-3; no statement in any of them may be false. **Route chosen: A** (Suggested Approach, ruling 1). Under Route A the contract sentence at `:384-385` is left unchanged. Separately, the content-trigger example at `:372-376` is corrected by its own change record (ruling 2), and that record exists and is applied in the same change (added by the Solution Architect; this tightens the AC and does not weaken it).
- [ ] AC-11: Nothing else in the scheduler changes behaviour. The existing tests in `WorkManagerSchedulerTest.kt` pass without edits other than the comment at `:537-538`; the registration of the content trigger (`WorkManagerScheduler.kt:219-258`: the SMS URI, the call-log URI when enabled, the 15-minute periodic request, `UPDATE`) is unchanged; `scheduleRegular`, `scheduleBootup`, `scheduleImmediate`, `scheduleManual`, `scheduleRestore` and `cancelAll` are unchanged. `BackupWorker.kt` is not changed by this fix. Checked by `git diff` over those files and the existing test run.
- [ ] AC-12: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` exits 0 with 0 failures, the skipped count not higher than before, and the authoritative test count (`git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`) taken before and after, the difference explained by the new tests.

### Integration Criteria (if fix modifies component boundaries)

- [ ] IC-1: Every place that builds or enqueues `INCOMING` work is listed in the fix and handled: `WorkManagerScheduler.kt:134-159` and `BackupTriggerWorker.kt:61-71` (a search of `app/src/main` for `BackupType.INCOMING` and `enqueueUniqueWork` is attached). The receiver's call (`SmsBroadcastReceiver.java:60`) is checked, not changed.
- [ ] IC-2: If the way `BackupTriggerWorker` obtains its preferences or the secret store changes (for example the worker becomes injected, as `BackupWorker` is), every place that constructs or registers it is updated in the same change and compile-checked (`WorkManagerScheduler.kt:241`; the release-build keep behaviour of the worker class name, which U-060's contract verification tracks, `sdlc/artifacts/build/sprints/sprint-017/U-060/contract-verification.md:105`; no `.pro` file under `app` names the class; the Solution Architect established that the class name is kept by the WorkManager library's own consumer rule `-keep class * extends androidx.work.Worker` in `work-runtime-2.9.1.aar` `proguard.txt`), and the worker is still created in the unit test harness and in a release build. The worker's class name and package do not change, because work already persisted on the device names the class. If the port of `BackupScheduler` changes, that is a contract change under CNTR-MODERNIZATION-004 and needs its change record first.
- [ ] IC-3 (added by the Solution Architect): the production worker factory builds the changed worker. On the emulator, with a debug build of the fix installed over the existing install (`adb install -r`, app data kept, so the persisted `CONTENT_TRIGGER` work is the old one), auto backup on, one ordinary SMS injected. The device log shows the trigger worker's line, followed by a `WorkManagerScheduler.scheduleIncoming:` line (`enqueued INCOMING`, or `blocked`/`skipped` with the stated reason). There is no `NoSuchMethodException` or "Could not create Worker" line for `BackupTriggerWorker`. Unit tests cannot show this: Robolectric runs without Hilt and no `@HiltAndroidTest` exists. A failure here would silently stop every automatic incoming backup. Run by the orchestrator under the product owner's standing device instructions; the log lines are recorded in the story's evidence.

## Impact Analysis

Searches run for this artifact: `BackupTriggerWorker` (all of `app`, `sdlc/artifacts/design`, `sdlc/artifacts/build`), `scheduleIncoming`, `scheduleContentTrigger`, `cancelAll`, `isWifiOnly`, `isEncryptionDegraded`, `isAutoBackupEnabled`, `EncryptionDegradedException`, `AutoBackupSettingsChanged`, `RECEIVE_SMS`. Counts are from these searches, not exhaustive greps of every layer.

| # | Layer | Affected? | Files and lines | Action needed |
|---|-------|-----------|-----------------|---------------|
| 1 | Requirements (`sdlc/artifacts/requirements`) | Check only | `modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md:57` (AC-3(d), content-URI trigger) and the CR-015 note at `:87`. REQ-MODERNIZATION-016 AC-8 baseline (named by CR-015 Scope Concern 2, `:716-717`; that AC was not re-read here) | None expected. The requirement states that the trigger is preserved; it does not state the follow-up's rules. If the story plan finds the network or degraded rule is a product promise not yet written, it raises a requirement change, not this bug |
| 2 | Design and contracts (`sdlc/artifacts/design`) | Yes, one sentence by Route A or B | `contracts/CNTR-MODERNIZATION-004-backup-scheduler.md:384-385` (the sentence), `:277` (Rule 5), `:274` (Rule 2), `:372-376` (see Scope Concern 1); `modernization/DES-MODERNIZATION-005-...md:133-138`, `:434`, `:495` | AC-10. CR-015 changes this contract at other lines and leaves `:384-385` on purpose; this bug owns it |
| 3 | Stories, sprints, change records (`sdlc/artifacts/build`, `sdlc/artifacts/stories`) | Yes | CR-015 (Scope Concern 2 raises this bug; its S1 does not absorb it, `:718-719`, `:353-356`); DES-MODERNIZATION-014 story S4 (WorkManager 2.9.1 to 2.12.0, `DES-MODERNIZATION-014-...md:379`, `:1126`); BUG-021 and BUG-022 (below) | Sequence per Technical Context |
| 4 | Application code (`app/src/main`) | Yes (root) | `worker/BackupTriggerWorker.kt:39`, `:53-74`; `scheduler/WorkManagerScheduler.kt:134-159`, `:219-258`, `:471-481`; read-only: `service/BackupWorker.kt:118-172`, `:500-506`; `App.java:279-293`, `:373-385`; `receiver/SmsBroadcastReceiver.java:47-84` | AC-1 to AC-8, AC-10, IC-1, IC-2 |
| 5 | Vendored library (`k9mail-vendored`) | No | None | None |
| 6 | Tests (`app/src/test`) | Yes | `service/WorkManagerSchedulerTest.kt:537-538` (comment), `:42-81` (harness); new test class for the trigger worker; `app/build.gradle:575-587` (floor) | AC-9, AC-11, AC-12 |
| 7 | Documentation (`README.md`, `sdlc/artifacts/engagement`) | Not examined | No search of `README.md` for Wi-Fi only or incoming backup wording was run for this artifact | Not verified; the Solution Architect or QA confirms whether any owner-facing text describes incoming backup and Wi-Fi only |

**Cascade analysis** (three layers affected: design/contract, code, tests).

- Root layer: application code. The trigger worker builds its own request and so does not inherit the scheduler's rules. The contract and DES-MODERNIZATION-005 describe the intended behaviour (a call to the scheduler); the code was written differently in story U-014 (`sdlc/artifacts/build/sprints/sprint-001/U-014/review-code.md:57-58` records the debounce as reviewed OK; it does not mention the rules) and kept in U-017 (`sprint-001/U-017/implementation-log.md:54`, `:67`, "RETAINED").
- Propagation: code to the contract sentence and three comments (AC-10); code to tests (AC-1 to AC-9); no propagation to `BackupWorker`, the vendored library, or the mail transport.
- Fix order: (1) tests written first and shown failing on HEAD for AC-1, AC-2, AC-4, AC-5, AC-6; (2) the fix; (3) AC-7, AC-8 and AC-3 added (these pass or can be shown to pass on both sides); (4) the Route A or B text change (AC-10); (5) floor raised (AC-9).

**Interaction with other work.**

- **BUG-021** (late-visible SMS never backed up, `sdlc/artifacts/stories/BUG-021-delayed-otp-sms-skipped-by-backup-watermark.md`). Both concern the incoming-backup path, but they touch different code. BUG-021 changes what a backup selects and how the watermark moves (`BackupQueryBuilder`, `BackupWorker.kt`, `DataTypePreferences`, restore); this bug changes how the incoming backup is enqueued (`BackupTriggerWorker.kt`, possibly `WorkManagerScheduler.kt`). They do not edit the same lines. The interaction is in behaviour: (1) BUG-021's likely-case argument rests on an incoming message starting a debounced backup within minutes (BUG-021 Description, "the likely case"). After this fix, with Wi-Fi only on, that backup waits for an unmetered network, so a later message is backed up inside a one-time-code message's three-hour delay less often than today; this can only reduce BUG-021's trigger condition on such a device, and does not remove the bug, which is independent of the cause of the late row. (2) BUG-021's Not known item 2 (whether a trigger fires when a withheld message is released) is answered by the same trigger worker, so a release that fires the trigger now goes through these rules. (3) BUG-021 AC-12's device check and its D14 Run R read this worker's log line; the fix must keep that line (see Existing Behavior to Preserve). (4) Both are scheduled before S4 (DES-MODERNIZATION-014 `:510-520` for BUG-021; the orchestrator's ruling for this bug), and neither blocks the other in code; the order between them is the Solution Architect's to set.
- **BUG-022** (cancellation checks in `BackupWorker` and `RestoreWorker`): unrelated code, except that both are fixed before the WorkManager upgrade so that its baseline is stable. No shared lines.
- **CR-015** applies to CNTR-MODERNIZATION-004 and removes the pre-API-24 clauses. It leaves `:384-385` as written, and says the "via scheduleIncoming()" comments go to this bug (`CR-015...md:718-719`, `:353`). The Route A or B choice here is the follow-up to that ruling.
- **U-054 (SE-002)**: this bug is a gap in U-054's gate: it was applied to `scheduleIncoming()`, `scheduleRegular()`, `scheduleContentTrigger()`, `scheduleBootup()`, `scheduleImmediate()`, `scheduleManual()` and `scheduleRestore()` (all seven have it in `WorkManagerScheduler.kt`), but not to the one place that enqueues work outside the scheduler.

## Suggested Approach

Written by the Solution Architect on 2026-11-21. Documents only; Gradle and the emulator were not run. Read in full this session: this bug, `BackupTriggerWorker.kt`, `WorkManagerScheduler.kt`, `SmsBroadcastReceiver.java`, `App.java`, `AppPermission.java`, `AndroidManifest.xml`, `BackupWorker.kt:80-280` and `:560-620`, `WorkManagerSchedulerTest.kt:42-81`, `:245-294`, `:384-411`, `:530-551`, CNTR-MODERNIZATION-004 `:90-409`, DES-MODERNIZATION-005 `:130-139`, `:316-325` and the cited rows, the U-066 emulator record (`sdlc/artifacts/build/sprints/sprint-022/U-066/evidence/IC1-emulator.txt:27`), the CR-015 applied-text check (F-5, F-6), the legacy `SmsJobService.java` and `BackupJobs.java` at the parent of commit `d095fe02` (U-017, which deleted them), and the WorkManager artifacts in the local Gradle cache (2.9.1 class files, 2.12.0 sources). Scope verified via grep: `INCOMING` work is enqueued in exactly two places in `app/src/main` (`WorkManagerScheduler.kt:153-154`, `BackupTriggerWorker.kt:66-71`); `scheduleIncoming()` has one production caller today (`SmsBroadcastReceiver.java:60`).

### Rulings at a glance

| # | Question | Ruling |
|---|----------|--------|
| 1 | Route A or B | **Route A.** `BackupTriggerWorker` becomes a `@HiltWorker` with the `BackupScheduler` port injected and calls `scheduler.scheduleIncoming()`; its private request builder is deleted |
| 2 | Network constraint and backoff on the trigger registration | **No.** The contract example at CNTR-MODERNIZATION-004 `:372-376` is a legacy carry-over and is wrong for WorkManager; a change record corrects it. The registration code is unchanged (AC-11) |
| 3 | Receiver's login and first-backup conditions on the trigger path | **Not applied by this bug.** The legacy trigger path never had them and the contract does not state them. Product owner item PO-2 |
| 4 | Cancel a pending `INCOMING` when auto backup is switched off | **Not in this bug** (same on both paths, contract-defined `cancelAll()`). Product owner item PO-1, recommended yes as a separate small change |
| 5 | Testability | All of AC-1 to AC-8 checkable in Robolectric. Constraints and initial delay from public `WorkInfo` in 2.9.1 and 2.12.0; backoff and input data only from the internal `WorkSpec`. Harness and floor below |
| 6 | Order | U-066 closes, then **BUG-022, BUG-023, BUG-021**, then S4. Three separate stories |
| 7 | Contract and design follow-ups | Route A makes CNTR-004 `:384-385`, Rule 2 for this path, Rule 5 and DES-005 `:133-138`, `:320`, `:434`, `:495` true with no text change. One change record: the example at CNTR-004 `:372-376` |
| - | Severity, and the SMS-received receiver | **Severity medium, priority high (both kept).** The receiver is not delivered `SMS_RECEIVED` because of the app's own manifest. That is a separate defect, a regression from U-001. It is recorded for its own change record and is not fixed here |

### 1. Route A, and the shape of the fix

**Why A, not B.**

1. The sentence states the intended behaviour and is the legacy behaviour. Legacy `SmsJobService.onStartJob` called `getBackupJobs().scheduleIncoming()` for a content-triggered run (`SmsJobService.java:72-77` at `d095fe02^`). Legacy `scheduleIncoming()` went through `createBuilder()`, which applied `jobConstraints()` (Wi-Fi only gives `ON_UNMETERED_NETWORK`) and the 30/300 exponential retry strategy, and it scheduled nothing when auto backup was off (`BackupJobs.java` at `d095fe02^`, `schedule(...)` and `createBuilder`). So the WorkManager port lost four rules that the legacy code had. That is a regression introduced in U-014, not a design choice.
2. Route B would put the rules for `INCOMING` in writing twice, once for the scheduler and once for the worker, and keep two builders in code. Two builders drifting apart is this defect. Route B fixes the text and keeps the cause.
3. Under Route A the U-054 gate (SE-002), INV-2 and INV-3 apply to the trigger path because the trigger path now runs that code, with no second copy to keep in step. Any later change to `scheduleIncoming()`, such as S4's WorkManager 2.12.0 move, covers both paths.

**How the plain `Worker` reaches the scheduler. Options weighed:**

| Option | Verdict | Reason |
|---|---|---|
| A1. `@HiltWorker` + `@AssistedInject`, inject the `BackupScheduler` port (and `Preferences` for the log line) | **Chosen** | This is the pattern `BackupWorker` (`BackupWorker.kt:105-116`) and `RestoreWorker` (`RestoreWorker.kt:118-119`) already use. `HiltWorkerFactory` is the only production factory (`App.java:160-166`), and `androidx.hilt:hilt-work`/`hilt-compiler` 1.2.0 are already applied (`app/build.gradle:184-185`). The test can pass a real `WorkManagerScheduler` built over mocks. The port, not the concrete class, is injected, so CNTR-004 Rule 1 and the port surface are untouched |
| A2. `App.getScheduler(applicationContext).scheduleIncoming()` | Rejected | One line, but it uses the legacy static bridge (`App.java:141-146`; the receivers' "will be replaced" comments). The field is null under Robolectric without Hilt (`App.java:226-244`), so the worker cannot be unit-tested without adding a seam, and AC-9 needs that test |
| A3. Hilt `@EntryPoint` + `EntryPointAccessors` | Rejected | Has the same unit-test problem as A2, and would be a new pattern (grep finds no `@EntryPoint` in `app/src/main`) |
| A4. A shared request-builder function that both call | Rejected | The worker would still need `SecretStore` and `Preferences` to apply the gates. It would either copy the gates or leave two gate sites, so there would still be two code paths |
| A5. Convert to `CoroutineWorker` | Rejected as unnecessary | `@HiltWorker` works with any `ListenableWorker`. `scheduleIncoming()` is synchronous, and a `Worker` already runs `doWork()` off the main thread |

**Target shape** (illustrative; the developer owns the exact text):

```kotlin
@HiltWorker
class BackupTriggerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scheduler: BackupScheduler,
    private val preferences: Preferences
) : Worker(context, params) {
    override fun doWork(): Result {
        Log.d(TAG, "BackupTriggerWorker.doWork: content-URI trigger fired; " +
                "enqueueing delayed incoming backup with initialDelay=${preferences.incomingTimeoutSecs}s")
        // INV-4 stage 2: the scheduler applies INV-1..INV-3, the SE-002 gate and the auto-backup/delay rule.
        scheduler.scheduleIncoming()
        return Result.success()
    }
}
```

- **Log line kept byte for byte**, as Existing Behavior to Preserve requires. If the scheduler declines, its own `blocked` or `skipped` line follows (`WorkManagerScheduler.kt:136`, `:141-142`), so the log stays truthful. The `OneTimeWorkRequest`, `WorkManager`, `TimeUnit`, `BackupType` and `BackupWorker` imports go.
- **Result stays `Result.success()`** whether or not `scheduleIncoming()` returns null (AC-4). A declined enqueue is a rule working as intended, not a failure of the trigger run. The scheduler's log already gives the reason. This also removes Not verified 5.
- **Persisted work.** Devices that already have `CONTENT_TRIGGER` work store the worker class name `com.zegoggles.smssync.worker.BackupTriggerWorker` in their `WorkSpec`. The class name and package **must not change**. With the name unchanged, the first trigger after the upgrade is resolved by `HiltWorkerFactory` through the generated assisted factory, and nothing has to be registered again.
- **Release build (closes the IC-2 open point).** The `proguard.txt` in `work-runtime-2.9.1.aar` has `-keep class * extends androidx.work.Worker` and keeps the public constructors of every `ListenableWorker` subclass. So the class name is stable under R8 whatever `app/proguard-rules.pro` says, and it matches hilt-work's string map key. `BackupWorker` relies on the same mechanism. No `.pro` change.
- **The failure mode to guard.** If the annotation or the KSP step is missing, `HiltWorkerFactory` returns null. WorkManager then falls back to reflection and finds no `(Context, WorkerParameters)` constructor, so the trigger fails on every SMS. Because the receiver is not delivered (see Severity below), that would silently stop **every** automatic incoming backup. Unit tests cannot see this: no `@HiltAndroidTest` exists under `app/src/test`, and Robolectric runs without Hilt. Hence the added **IC-3** (device check) below.
- **Unaffected.** `WorkManagerSchedulerTest` registers `CONTENT_TRIGGER` but never runs it; its `TestableBackupWorkerFactory` returns null for other classes (`BackupWorker.kt:576-596`). The class literal `BackupTriggerWorker::class.java` in `WorkManagerScheduler.kt:241` is a compile-time reference only. The scheduler does not inject the worker, so there is no DI cycle. `BackupScheduler.java`, `WorkManagerScheduler.kt` code and `BackupWorker.kt` are unchanged (AC-11). The receiver is checked, not changed (IC-1).

### 2. The trigger registration carries no network constraint or backoff; the contract example is wrong

The example at CNTR-MODERNIZATION-004 `:372-376` (`networkType = CONNECTED or UNMETERED`, `backoff = EXPONENTIAL 30s`, `existingPolicy = REPLACE`) copies the legacy content-trigger job. That job was built by `createBuilder(INCOMING)`, which attached the `INCOMING` network constraint and retry strategy to the observer job itself (`BackupJobs.createContentUriTriggerJob` at `d095fe02^`). Under WorkManager that is the wrong place:

- Stage 1 does no network I/O. INV-2 is a rule for backup work, and after Route A the stage-2 `INCOMING` request carries it. The net effect, no backup until a permitted network is up, is the same as legacy.
- A network constraint on stage 1 would hold the trigger, and so the start of the debounce, until the network matches. That adds a dependency, changes the moment the delay starts, and changes the registration, which AC-11 forbids.
- A backoff on a worker that always returns success never applies.
- The code uses `ExistingPeriodicWorkPolicy.UPDATE` (`WorkManagerScheduler.kt:252`), which INV-1 allows for periodic work ("REPLACE/UPDATE", `:240`).

Ruling: the code is right and the example is wrong. A change record (the next free number; CR-016 is the latest in `sdlc/artifacts/build/change-records/` today) amends only the example. It removes `networkType` and `backoff` from the `CONTENT_TRIGGER` block and sets `existingPolicy = UPDATE`, with a one-line note that INV-2 and INV-3 are carried by the `INCOMING` follow-up. This is non-breaking under the contract's own policy (`:266-267`: no invariant changes, the example is restated). It closes Scope Concern 1.

### 3. Login and first-backup conditions: not applied by this bug

These are the receiver's own checks (`SmsBroadcastReceiver.java:66-83`), not `scheduleIncoming()`'s. The legacy trigger path never applied them: `onStartJob` called `scheduleIncoming()` directly, and that checked auto backup only. The contract does not state them for the trigger, and the receiver has not been delivered since 2026-06-02 (see Severity), so they have not been in force on the owner's devices since then. Adding them now would change behaviour. Before the first backup, an SMS would no longer start an automatic first (full) backup, and that is something the user would notice. It is product owner item PO-2. What `BackupWorker` does with no login set (`RequiresLoginException` gives `Result.failure`, `BackupWorker.kt:268-271`) was not traced end to end.

### 4. A pending `INCOMING` survives switching auto backup off

Confirmed (`WorkManagerScheduler.kt:410-415`). It is the same in legacy, where `cancelAll()` was `cancelRegular()` plus the content trigger, and in the contract (`:94-95`). It is the same on both paths, so it is not this bug, and AC-11 keeps `cancelAll()` unchanged. If the owner wants it (PO-1), the smallest correct change is in `App.rescheduleJobs()` (`App.java:373-385`): call the existing port operation `scheduler.cancel(BackupType.INCOMING)` (`WorkManagerScheduler.kt:424-440`) **only when auto backup is off**. It should not go in `cancelAll()`, because `rescheduleJobs()` calls `cancelAll()` on every settings change, and that would drop a pending incoming backup each time an unrelated setting changes. The window affected is at most one `incomingTimeoutSecs`.

### 5. Testability

**What the library exposes** (read from the cached artifacts, not from a build):

| Field | 2.9.1 public `WorkInfo` | 2.12.0 public `WorkInfo` | Internal `WorkSpec` (both) |
|---|---|---|---|
| Constraints (`requiredNetworkType`) | `getConstraints()` | `constraints` (`WorkInfo.kt:65`) | yes |
| Initial delay | `getInitialDelayMillis()` | `initialDelayMillis` (`:68`) | yes |
| Tags, state, id | yes | yes | n/a |
| Backoff policy and delay | **no** | **no** | `backoffPolicy`, `backoffDelayDuration` |
| Input data | **no** | **no** | `input` |

- The 2.9.1 column is from the method names in `androidx/work/WorkInfo.class`. The 2.12.0 column is from `work-runtime-2.12.0-sources.jar`.
- The internal `WorkSpec` is reached as `WorkManagerImpl.getInstance(context).workDatabase.workSpecDao().getWorkSpec(workInfo.id.toString())`. `getWorkDatabase` and `getWorkSpec(String)` exist in both versions (2.9.1 class files; 2.12.0 `WorkSpecDao.kt:64`). It is `@RestrictTo`: put `@SuppressLint("RestrictedApi")` on the helper. Lint `warningsAsErrors` (`app/build.gradle:65-67`) does not check test sources unless `checkTestSources` is set, and it is not set there.
- **The library default backoff is `EXPONENTIAL`, 30 000 ms.** Source: 2.12.0 `WorkSpec.kt:62`, `:65`; `WorkRequest.kt:384` `DEFAULT_BACKOFF_DELAY_MILLIS = 30000L`. In 2.9.1 the constant is present but its value was not read from the bytecode. So the "Backoff" row of the Evidence table is a difference in code, not in effect, and a backoff assertion passes on HEAD too. Backoff equality is therefore shown two ways: **by construction**, since there is one builder after Route A, and by the AC-3 `WorkSpec` comparison, which guards against a second builder coming back. AC-3's "if it cannot be read" alternative is not needed: it can be read on both versions. S4 re-runs the same tests on 2.12.0, and the `WorkSpec` accessors it uses exist there.

**Harness.**

- New file `app/src/test/java/com/zegoggles/smssync/worker/BackupTriggerWorkerTest.kt`, using `@RunWith(RobolectricTestRunner::class)` and `@Config(sdk = [29])` as `WorkManagerSchedulerTest` does.
- `setUp` copies `WorkManagerSchedulerTest.kt:53-81`: `WorkManagerTestInitHelper.initializeTestWorkManager` with `TestableBackupWorkerFactory`, the same `Preferences` / `DataTypePreferences` / `SecretStore` mock stubs, and a real `WorkManagerScheduler(context, preferences, secretStore)`.
- The worker is built by `TestListenableWorkerBuilder<BackupTriggerWorker>(context).setWorkerFactory(...)`, as `BackupWorkerTest.kt:57-60` does. The factory is a small one that returns `BackupTriggerWorker(appContext, params, scheduler, preferences)` and is defined in the test. Then call `doWork()` directly (a `Worker`'s `doWork()` is synchronous).
- Per the AC preamble, every test first calls `scheduler.scheduleContentTrigger()` with the happy-path stubs, then changes the stubs, then runs the worker.
- No `TestDriver` is used: the test clock is not advanced and constraints are not marked met, so the `INCOMING` work stays `ENQUEUED`.

| AC | How it is checked | Fails on HEAD? |
|---|---|---|
| AC-1, AC-2 | `getWorkInfosForUniqueWork("INCOMING").get().single().constraints.requiredNetworkType` is `UNMETERED` / `CONNECTED` | Yes (`NOT_REQUIRED`) |
| AC-3 | Run the trigger path, capture the `WorkInfo` and `WorkSpec`; `cancelUniqueWork("INCOMING")`; run `scheduleIncoming()`, capture again. Compare tags (they include the worker class name tag WorkManager adds itself; it is the same class on both paths), `initialDelayMillis`, `constraints`, `backoffPolicy`, `backoffDelayDuration`, `input`. Once each for Wi-Fi only on and off | Yes (constraints differ) |
| AC-4 | Stub `isEncryptionDegraded()` true after registration; after `doWork()`, `getWorkInfosForUniqueWork("INCOMING")` is empty and the result is `Result.success()` | Yes |
| AC-5, AC-6 | The same, with `isAutoBackupEnabled` false; with `incomingTimeoutSecs` 0 and -1 | Yes |
| AC-7 | `state == ENQUEUED`, `initialDelayMillis == incomingTimeoutSecs * 1000`, and no `BackupWorker` has run (state not `RUNNING`/`SUCCEEDED`) | It passes on HEAD for state and delay; it is the missing INV-4 test |
| AC-8 | Count the non-finished entries for `INCOMING` after each order (trigger twice; trigger then `scheduleIncoming()`; the reverse). For "the first is cancelled", capture the first id and assert it is `CANCELLED` or absent and that the surviving id differs. A "fresh delay" cannot be told apart by duration without advancing the clock: the new id is the evidence that a new request with its own delay replaced the old one | Passes on HEAD; regression guard |

**Failure injection for AC-9**, recorded in the story:

| Change | Tests that fail |
|---|---|
| Restore the private builder without `setConstraints` | AC-1, AC-2, AC-3 |
| Drop the degraded check in `scheduleIncoming()` | AC-4 (and the existing `se002_*` scheduler test) |
| Drop the auto-backup check | AC-5 |
| Drop the `timeoutSecs <= 0` check | AC-6 |
| Pass 0 as the delay | AC-7 |
| `ExistingWorkPolicy.KEEP` | AC-8 |

**JaCoCo.** The `worker*` package floor is `0.00` (`app/build.gradle:575-587`). After A1, the package also holds the hilt-work generated classes for the worker (an assisted factory and its implementation, a `_Factory`, a Hilt module; the exact names were not seen because nothing was built). `jacocoFileFilter` (`app/build.gradle:232-245`) does not exclude generated classes, and the Robolectric tests do not run them. So the measured ratio will be below 100%. Set the floor to the measured value rounded down to the next 0.05, state the number and the classes counted in the comment, and do **not** add a generated-class exclusion in this story. An exclusion would change the measured ratio of every package with Hilt classes, which would confuse AC-9's "no other floor lowered" check.

### 6. Order against BUG-021 and BUG-022

Order: **U-066 closes, then BUG-022, then BUG-023, then BUG-021, then S4.** Each is its own story.

- BUG-022 goes first, as ruled (DES-MODERNIZATION-014 `:509-516`). U-066's AC-11(a) freeze on `app/src/main` applies to BUG-023 as well.
- **BUG-023 before BUG-021**:
  - BUG-021's device criterion (AC-12) and DES-014's D14 Run R observe the incoming path. Measuring after BUG-023 means they run on the final incoming path, with its network rule, and need no repeat.
  - BUG-023 is small and is checked in unit tests, apart from IC-3's one-SMS check. BUG-021 partly waits on a measurement.
  - Moving BUG-023 earlier does not delay BUG-021's build, because the files do not overlap.
- **Files.**
  - BUG-021 and BUG-022 both edit `service/BackupWorker.kt`; BUG-023 does not touch it (AC-11). BUG-021 cites `WorkManagerScheduler.kt:233` read-only.
  - The only file all three may share is `app/build.gradle`'s JaCoCo block, in different rules, which is a trivial merge.
  - Against DES-014 story S1: both touch `WorkManagerSchedulerTest.kt` (S1 removes the pre-24 test; BUG-023 edits the comment at `:537-538`) and the `WorkManagerScheduler.kt` KDoc. Either order works, but not concurrently.
- **Why not share a story.** The risks differ: BUG-023 is DI wiring for the only incoming path, BUG-021 is watermark semantics, BUG-022 is cancellation. Merging them would mix three review lenses and three failure-injection sets in one diff. `parallel_eligible: true` stays correct for the code, but the sequence above is the recommendation.

### 7. Contract and design follow-ups

**Become true with the fix, no text change:**

- CNTR-004:
  - `:384-385` (the sentence).
  - The INV-4 realization `:243`.
  - Rule 2 `:274` for this enqueue path: INV-2 and INV-3 now hold.
  - Rule 5 `:277`.
  - The INV-4 note `:245`: "INV-1 collapses the two at enqueue".
- DES-005:
  - `:133-138` (`:135` describes legacy; `:137` stays true).
  - `:320`.
  - The port table `:434`.
  - The Risks row `:495` ("INV-4 test asserts the debounce delay"), which becomes true once AC-7's test exists; citing the test name there is optional.

So AC-10's DES-005 item needs no edit under Route A beyond checking it.

**Code and test comments (in the fix):**

- `BackupTriggerWorker.kt:34-35` and `:39`: say the worker calls `BackupScheduler.scheduleIncoming()`; drop the stale U-015 paragraph `:44-46`.
- `WorkManagerSchedulerTest.kt:537-538`: "would ultimately call" becomes true; present tense is optional.

**Needs a change record:**

- The CNTR-004 example `:372-376` (ruling 2). It is the only contract text change.
- The CR-015 check F-5's optional "known divergence" pointer is **not** needed if this fix is the next scheduler change. Add it only if BUG-023 is approved and then deferred.

**For other owners, recorded here and not ACs of this bug:**

- (a) DES-MODERNIZATION-014 D14 Run R (`:1415-1419`, `:2247`) reads the `SmsBroadcastReceiver` lines. Those lines will not appear (see Severity). After BUG-023 the trigger path also logs `WorkManagerScheduler.scheduleIncoming: enqueued INCOMING`, so that line no longer identifies the receiver either. Run R should read the trigger line.
- (b) `sdlc/artifacts/build/sprints/sprint-017/U-060/contract-verification.md:49` says the trigger worker is built by reflection. That becomes historical after A1, and the C-2 check of any later R8 story should list `BackupTriggerWorker` among the `HiltWorkerFactory` keys. The closed record is not edited.
- (c) DES-MODERNIZATION-008 (Hilt) was not read in this session. If it lists the `@HiltWorker` workers, the story adds `BackupTriggerWorker` there.

**Stale references fixed in this bug** (CR-015 check, F-5 side note; confirmed by grep of DES-005 today): the Risks row is `:495`, not `:504`; the port table row is `:434`, not `:442`. Every occurrence in this file is updated.

### Severity, and the SMS-received receiver

**The receiver is not delivered `SMS_RECEIVED`, because of the app's own manifest. That makes it a defect of its own, not a platform rule for non-default SMS apps.**

The manifest blocks delivery in two independent ways. Either one is enough:

1. **`android:exported="false"`** (`AndroidManifest.xml:148`). Commit `50f3b34f` (U-001, 2026-06-02) added it: "Add android:exported to all 6 intent-filtered receivers ... SmsBroadcastReceiver=false". Before that, the receiver had an intent filter and no `exported` attribute, so it was exported by default at that target level. The platform's manifest reference says a non-exported receiver receives only messages from the same application or the same user ID (recalled; not fetched this session). The telephony stack sends `SMS_RECEIVED` from the phone process, not from this app's user ID, so the broadcast is filtered before it reaches the receiver. `BootReceiver` and `PackageReplacedReceiver` are also non-exported, but their broadcasts come from the system server, which is allowed. That is why only the SMS receiver went dead.
2. **`RECEIVE_SMS` is declared (`:17`) but never requested at runtime.** The only runtime requests are for `READ_SMS`, `READ_CALL_LOG`, `READ_CONTACTS` (`AppPermission.java:17-19`), `POST_NOTIFICATIONS`, `GET_ACCOUNTS` and `WRITE_CALENDAR`; grep finds no request for `RECEIVE_SMS`. Since Android 8 a dangerous permission is granted only if it is requested, even when another permission in its group is held (recalled platform behaviour change, not fetched). The broadcast requires its receivers to hold `RECEIVE_SMS`. A grant through the default-SMS role during a restore (`SmsDefaultRoleHelper`, not read) could be an exception; it was not established.

Non-default SMS apps do receive `SMS_RECEIVED` when they are exported and hold the permission; only `SMS_DELIVER` is reserved for the default app. So the platform allows what the app declared away.

The 2026-11-21 emulator record fits this reading: a debuggable build, verbose lines present, 0 `onReceive(` lines, and the trigger's `INCOMING` run 60 s later. It does not rule out a third cause on its own: `App.setBroadcastReceiversEnabled` disables the component when auto backup is off (`App.java:258`, `:358-361`), and the record does not state the auto-backup setting. The code reading does not depend on the emulator. Not established: whether `RECEIVE_SMS` is granted on that emulator, which `adb shell dumpsys package com.zegoggles.smssync` would show (orchestrator).

**Remedy: not in this bug.** The receiver has been dead code on every Android 11+ device since U-001. CNTR-004 `:245` says removing it "would be a separate, recorded change". Record it as its own item, a change record that either removes the receiver or restores it; see PO-3. If it is removed, the change record updates CNTR-004 `:243`, `:245`, `:387-389` and DES-005 `:321`. AC-8's mixed-order cases stay valid as port behaviour either way.

**Severity stays medium; priority stays high.** This replaces the analyst's conditional rule in Technical Context.

- The receiver finding settles the first half of that rule: the content trigger is the only automatic incoming path on the owner's Android 11+ devices. So the missing rules apply to every automatic incoming backup.
- The finding widens reach, not consequence. The consequence class is unchanged: no message is lost or duplicated, a setting is not honoured, and a rarely-entered security gate is bypassed.
- The second half of the rule, whether the owner uses Wi-Fi only, is unknown. Its answer changes the urgency, which priority high already reflects, not the class.
- Rating this high would put a setting that is not honoured above BUG-021's silent loss of messages, which is rated medium. That would invert the scale this project has used.
- Priority high stands on the orchestrator's before-S4 ruling and on reach.

### For the product owner (not decided here)

| Item | Question | Recommendation |
|---|---|---|
| PO-1 | When you switch auto backup off, should an incoming backup already waiting (up to one incoming delay away) be cancelled? Today it still runs, as in the original app | **Yes**, as a separate small change in `App.rescheduleJobs()` using `cancel(INCOMING)` when auto backup is off (ruling 4) |
| PO-2 | Should an SMS start an automatic backup before login is set or before the first backup has been run? The original app's content trigger did; the dead receiver did not | **Keep as is** (no extra gate). The first-backup case only matters on a fresh install or reset. If you prefer the first full backup to start only by hand, it is a one-condition change on the trigger path, raised as its own item |
| PO-3 | The SMS-received receiver has not worked since June. Remove it, or restore it? Restoring it means a new permission prompt (`RECEIVE_SMS`) and an exported receiver | **Remove** through a change record. The content trigger already starts every incoming backup and also sees sent messages and call-log changes; removal changes nothing you can observe today |
| PO-4 (information, not a decision) | Is "Wi-Fi only" on, on your devices? | After this fix, with it on, incoming backups wait for Wi-Fi, as your setting says. That follows from the setting and the contract (INV-2), so it is not a decision; the answer only tells how much this fix changes in practice |

### Not verified by the Solution Architect

- The platform documentation for non-exported receivers and for the Android 8 permission-group change is cited from memory. No page was fetched in this session.
- Whether `RECEIVE_SMS` is held on the emulator or on the owner's devices.
- The auto-backup setting during the 2026-11-21 emulator run.
- The names of the hilt-work generated classes and their effect on the `worker*` ratio (nothing was built).
- The 2.9.1 value of `DEFAULT_BACKOFF_DELAY_MILLIS` (the constant name was seen, not the value).
- The DES-MODERNIZATION-008 worker list.
- What `BackupWorker` does with no login set, or with credentials in plaintext.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/worker/BackupTriggerWorker.kt` (`:39`, `:53-74`) | Builds and enqueues its own `INCOMING` request: delay and tag only, no network constraint, no backoff, no degraded or auto-backup check; class comment says it goes through `scheduleIncoming()` | Route A (Suggested Approach, ruling 1): `@HiltWorker` with an `@AssistedInject` constructor; the `BackupScheduler` port and `Preferences` are injected; `doWork()` logs the unchanged line, calls `scheduler.scheduleIncoming()` and returns `Result.success()`; the private builder and its imports are removed; the class comment is corrected. The class name and package are unchanged |
| `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt` (`:134-159`, `:471-481`) | `scheduleIncoming()` applies the four rules; `networkConstraints` is private | No code change under Route A (it becomes the only builder of `INCOMING` work). Comments only if S1 has not already reworded the INV-4 KDoc (`:58-63`) |
| `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` (`:384-385`) | Says the trigger "calls scheduler.scheduleIncoming()" | Route A: `:384-385` left as is. The content-trigger example at `:372-376` (network, backoff, `REPLACE`) is corrected by a new change record (ruling 2; AC-10) |
| `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` (`:133-138`, `:495`) | Describes the call; names the debounce test as the mitigation | Made consistent with the code and the new test (AC-10) |
| `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt` (`:537-538`) | Comment says the trigger worker "would ultimately call scheduleIncoming()" | Comment made true (AC-10) |
| `app/src/test/java/com/zegoggles/smssync/worker/BackupTriggerWorkerTest.kt` (new; the directory does not exist yet) | No test for the worker | New tests (AC-1 to AC-9); harness in the Suggested Approach, ruling 5 |
| `app/build.gradle` (`:575-587`) | Package floor `0.00`, "TODO raise after dedicated tests" | Floor raised (AC-9) |

## Existing Behavior to Preserve

- The two-stage debounce (CNTR-MODERNIZATION-004 INV-4): no backup on the raw content change; the delayed unique work `INCOMING` replaces a pending one (`REPLACE`, INV-1); the delay is `incomingTimeoutSecs`.
- The trigger worker's log line "BackupTriggerWorker.doWork: content-URI trigger fired; enqueueing delayed incoming backup with initialDelay=...s" (`BackupTriggerWorker.kt:57-58`): CR-015's emulator record and DES-MODERNIZATION-014 D14 Run R read it. If the fix changes its text, those artifacts are updated in the same change.
- The unique work name `INCOMING`, the tag `INCOMING` and the backup type `BackupWorker` infers from it (`BackupWorker.kt:500-506`).
- The content trigger's registration (SMS URI; call-log URI when call-log backup and call-log-after-call are on; 15-minute periodic request; `UPDATE`; no encryption-degraded registration, `WorkManagerScheduler.kt:219-258`). The trigger worker is also fired by a call-log change when that URI is registered; the follow-up it enqueues is the same `INCOMING` work as for an SMS (not changed here).
- The receiver path: `SmsBroadcastReceiver` still calls `scheduleIncoming()` after its own checks (auto backup on, login set, not first backup; `SmsBroadcastReceiver.java:66-83`) and `SmsBroadcastReceiverTest` stays green.
- `scheduleRegular`, `scheduleBootup`, `scheduleImmediate`, `scheduleManual`, `scheduleRestore`, `cancelAll` and `cancel` (AC-11).
- `BackupWorker`'s own behaviour: foreground notification, backoff cap, retry on `MailException`, watermark rules (BUG-010, BUG-021).
- The result the trigger worker returns on its normal path (`Result.success()`, `BackupTriggerWorker.kt:73`). Why this matters for a periodic worker is not verified (see Not verified).

## Verification Steps

For QA, per acceptance criterion. Run Gradle in a shell without `MSYS_NO_PATHCONV`. Device steps are run by the orchestrator or QA, never a subagent with a background shell.

1. AC-1, AC-2: run the new network tests on HEAD and confirm they fail with `NOT_REQUIRED`; run on the fix and confirm `UNMETERED` and `CONNECTED`. Confirm the preference is changed after `scheduleContentTrigger()` and before the worker runs.
2. AC-3: read the comparison test; confirm it covers Wi-Fi only on and off and lists each field compared; read how backoff equality is shown and that the story says so.
3. AC-4, AC-5, AC-6: run on HEAD and confirm each fails with an `INCOMING` work present; run on the fix and confirm none.
4. AC-7: run the test; confirm the test clock is not advanced, the state is enqueued, and the delay equals `incomingTimeoutSecs`.
5. AC-8: run the three single-flight tests (trigger twice; trigger then scheduler; scheduler then trigger); confirm exactly one non-terminal work item each.
6. AC-9: list the new test methods against AC-1 to AC-8; read the recorded failure injections; read `app/build.gradle:575-590` and confirm the floor is above `0.00` and the old comment is gone.
7. AC-10: open the four places and the code side by side; for Route B confirm the change record exists and CNTR-MODERNIZATION-004 carries it.
8. AC-11, AC-12: `git diff` over the files named; run `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`; compare the authoritative test count.
9. Observation check, not an acceptance gate (Evidence, "Observation, with its limits"): debug build, auto backup on, login set, first backup done, one ordinary SMS; filter the log (tag `SMSBackup+`) for `onReceive(` and `scheduleIncoming: enqueued INCOMING`. Record which lines appear, the build type and the Android level. Only the owner enters any password. On a build that contains this fix, `scheduleIncoming: enqueued INCOMING` comes from the trigger path too, so only the receiver's own lines show that the receiver was delivered. Also record the output of `adb shell dumpsys package com.zegoggles.smssync` for `RECEIVE_SMS` (granted or not) and the auto-backup setting.
10. IC-3 (added by the Solution Architect): with the fixed debug build installed over the existing install (data kept), one SMS. Confirm the trigger line is followed by a `WorkManagerScheduler.scheduleIncoming:` line, and that no "Could not create Worker" or `NoSuchMethodException` line names `BackupTriggerWorker`.

## Root Cause Analysis
<!-- Filled during planning phase -->

Intake note (not the planning-phase analysis). Root layer: application code. The trigger worker was written in story U-014 as a stub that enqueues the delayed backup itself (`sdlc/artifacts/build/sprints/sprint-001/U-014/plan.md:40`) and was retained through U-015 and U-017. The review checked that it debounces and uses `REPLACE` (`U-014/review-code.md:57-58`); the network, degraded and auto-backup rules were not on that checklist. The later gate (U-054, SE-002) was added to the scheduler's operations and not to the worker, which is not part of the scheduler. The contract's wording ("calls scheduleIncoming()") was written from the legacy job service's behaviour (DES-MODERNIZATION-005 `:133-135`) and was never compared with the worker. That a worker with no test could drift from it unnoticed follows from the 0% floor.

## Technical Context

- **Severity: medium.** No message is lost or duplicated by this defect. What goes wrong: (1) an automatic backup runs on a metered network although the owner chose Wi-Fi only, which sends message content over mobile data and uses the owner's data allowance, against a setting the owner made on purpose; (2) an automatic backup is started on a device in the encryption-degraded state, which U-054 decided must not happen (what the worker then does with the plaintext credentials is not read, so the worst case is not claimed); (3) the debounce could be lost in the delay-0 case, which needs a configuration change that is not shown to happen. Against a higher rating: the app is private and unpublished; the owner's devices run Android 11 or newer; the degraded state is rare by design and the auto-backup gap is narrow (Correction 2); and nothing is shown to have gone wrong on a device. It rises to high if the observation in Evidence shows the receiver is never delivered the broadcast (every automatic incoming backup is then unchecked) and the owner uses Wi-Fi only; it falls to low if QA shows the library default and `BackupWorker` together already hold back a metered-network run (not expected from the code read). **Solution Architect ruling (2026-11-21), which replaces this conditional rule: medium, kept.** The receiver is established as not delivered, so the trigger is the only automatic incoming path and this bug's reach is every automatic incoming backup. The consequence class is unchanged, though: no loss or duplication, a setting not honoured, a rare gate bypassed. Whether Wi-Fi only is on is unknown (PO-4). A high rating would place this above BUG-021's silent message loss, which is rated medium. The library default does not hold back a metered run: the default sets backoff only, not constraints (Suggested Approach, ruling 5). Reasons in the Suggested Approach, "Severity".
- **How earlier bugs were rated.** BUG-018 medium/medium, BUG-019 medium/medium, BUG-020 low/medium, BUG-021 medium/high, BUG-022 low/medium (as in the file headers). This one is a setting that is not honoured, with a security-motivated gate bypassed, so not below BUG-022; not above BUG-021, which is silent data loss.
- **Priority: high.** The orchestrator ruled (recorded here, not re-decided) that this fix comes before story S4 of DES-MODERNIZATION-014, which raises WorkManager to 2.12.0 and touches the scheduler, so that the REQ-MODERNIZATION-016 AC-8 baseline includes the worker (CR-015 Scope Concern 2). The fix is its own small story. The priority reflects that ordering, not the severity.
- **Complexity: medium.** The change itself is small. The cost is the test harness for a plain `Worker` that builds its own `Preferences` and has no `SecretStore` (how it gets one is the Solution Architect's choice, and may change how the worker is constructed), and the contract route decision.
- **Sequencing (orchestrator rulings, recorded).** Before S4. Its own small story. Interacts with BUG-021 (above) and CR-015 (above). The order against BUG-021 and BUG-022 is not ruled; DES-MODERNIZATION-014 `:505-520` places BUG-022 before S2 and BUG-021 between BUG-022 and S4. *Solution Architect ruling:* after U-066 closes, the order is BUG-022, then BUG-023, then BUG-021, then S4, each its own story (Suggested Approach, ruling 6).
- **Product-owner context.** The app is private, for the owner's own use; no store, rollout or other-user exposure. The owner's devices run Android 11 or newer. The mail server is Gmail.
- **Library version.** `androidx.work:work-runtime-ktx:2.9.1` and `work-testing:2.9.1` today (`app/build.gradle:155-156`); S4 raises both to 2.12.0 (DES-MODERNIZATION-014 `:379`). Any test written here runs on 2.9.1 first and on 2.12.0 after S4; the exposed work-info fields may differ between them (not checked).

## Supporting Documentation

- `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` (`:274` Rule 2, `:277` Rule 5, `:372-376`, `:384-389`)
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` (`:133-138`, `:434`, `:495`)
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-toolchain-runtime-libraries-platform-level-design.md` (story S4; `:505-520` ordering of BUG-021 and BUG-022)
- `sdlc/artifacts/build/change-records/CR-015-cntr-004-content-trigger-pre-api-24-fallback-removed.md` (Evidence 3 and 4; Scope Concerns 1 and 2; Suggested Approach, "What the code shows that the record did not have")
- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md` (AC-3(d), `:57`; CR-015 note, `:87`)
- `sdlc/artifacts/stories/BUG-021-delayed-otp-sms-skipped-by-backup-watermark.md`, `sdlc/artifacts/stories/BUG-022-worker-cancellation-checks-do-nothing.md` (structure, rating precedents, ordering)
- `sdlc/artifacts/build/sprints/sprint-001/U-014/review-code.md`, `sdlc/artifacts/build/sprints/sprint-001/U-017/implementation-log.md` (origin of the worker)

## Implementation Notes
<!-- Added by agents during build -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes

### Scope Concerns

Raised, not acted on; none changes a ruling recorded above.

1. **The contract's example for the trigger's own registration also differs from the code.** CNTR-MODERNIZATION-004 `:372-376` shows the `CONTENT_TRIGGER` periodic work with `networkType = CONNECTED` (or `UNMETERED` per `isWifiOnly()`) and backoff exponential 30 s. `scheduleContentTrigger()` (`WorkManagerScheduler.kt:232-247`) sets only the content-URI triggers, with no network constraint and no backoff. Whether the trigger registration should carry a network constraint is not obvious: a constraint there would delay stage 1, and so the debounce start, until the network is available. This is not in the acceptance criteria. It is raised for the Solution Architect to decide under Route A or B (a change record can correct the example).
2. **The receiver path has checks the trigger path does not.** `SmsBroadcastReceiver` also requires login information and that the first backup has run (`SmsBroadcastReceiver.java:66-83`), and these are the receiver's own, not `scheduleIncoming()`'s. The trigger worker has neither. The contract does not state them for the trigger. Whether an `INCOMING` backup that fires before login or before the first backup does anything harmful (`BackupWorker` returns a failure for failed authentication, `BackupWorker.kt:268-271`; not read in full) is not established. Not made an acceptance criterion because the contract sentence does not cover it.
3. **A pending `INCOMING` work survives switching auto backup off.** `cancelAll()` cancels `REGULAR` and `CONTENT_TRIGGER` only (`WorkManagerScheduler.kt:410-415`), so an `INCOMING` work enqueued by either path before the switch still runs when its delay ends. This is the same on both paths, so it is not part of this bug and not an acceptance criterion; the auto-backup rule of AC-5 covers only the moment the trigger worker runs. Raised in case the owner expects a pending incoming backup to stop.
4. **The call-log trigger shares the same worker and the same follow-up.** A call-log change (registered when call-log backup and call-log-after-call are on) enqueues the same `INCOMING` work, so the rules this fix adds apply to it too. Nothing else is claimed.
5. **The comments in the scheduler about the pre-API-24 path are story S1's, not this bug's** (CR-015 Scope Concern 3). This bug's AC-10 touches only the "via scheduleIncoming()" wording.
6. **U-060's contract verification calls the trigger worker a "plain `CoroutineWorker`"** (`sprint-017/U-060/contract-verification.md:49`). The code extends `Worker` (`BackupTriggerWorker.kt:51`). No effect on this bug; noted so the story does not repeat it.

### Not verified

Everything below could not be established from the code and artifacts read, and is not assumed.

1. Whether the platform delivers `SMS_RECEIVED` to the receiver as declared (`exported="false"` with `BROADCAST_SMS`, `AndroidManifest.xml:146-153`), on any Android level. The emulator run of 2026-11-20 does not decide it (CR-015). Whether the run was a debug build, and whether the scheduler's `enqueued INCOMING` line appeared, is not known; the raw log is outside the repository. *Solution Architect update:* largely closed. The code shows two independent causes of non-delivery (Suggested Approach, "Severity"), and the 2026-11-21 debuggable-build run found 0 `onReceive(` and 0 `scheduleIncoming` lines. Still open: whether `RECEIVE_SMS` is granted on the emulator or on the owner's devices; the auto-backup setting during that run; and the platform documentation, which is cited from memory.
2. The WorkManager library default for backoff when none is set (believed exponential, 30 s) and whether it equals what `scheduleIncoming()` sets. Not read; the 2.9.1 sources were not opened. *Solution Architect update:* 2.12.0 sources give `EXPONENTIAL`, 30 000 ms, equal to `scheduleIncoming()`'s. 2.9.1's value was not read from the bytecode. Moot after Route A, because there is one builder.
3. Whether the work info in 2.9.1 (and in 2.12.0 after S4) exposes the backoff policy and delay, the initial delay and the input data, so that AC-3 can be shown from the public API. Whether `TestListenableWorkerBuilder` or `TestWorkerBuilder` runs a plain `Worker` that calls `WorkManager.getInstance(...)` inside the existing Robolectric harness was not tried. No test was written or run. *Solution Architect update:* the public `WorkInfo` exposes constraints, initial delay, tags and state in both 2.9.1 and 2.12.0, but neither backoff nor input data. The internal `WorkSpec`, reachable in both versions, exposes all of them (Suggested Approach, ruling 5). `TestListenableWorkerBuilder` with `setWorkerFactory` is the harness, as `BackupWorkerTest.kt:57-60` uses it. Still not run.
4. What `BackupWorker` does on a device in the encryption-degraded state (it does not check it, but whether the credential read then fails or proceeds with plaintext credentials was not read), and what it does with no network connection (that the connect raises `MailException` and so retries was not read).
5. Whether a `Result.failure()` or a thrown error from a periodic trigger worker stops its recurrence. Relevant to AC-4 to AC-6 ("returns the same result type as today"): the criteria keep the current `success` result so the question does not arise, but the better choice was not examined. *Solution Architect ruling:* `success` is the better choice. A declined enqueue is the rule working, not a failure of the trigger run (Suggested Approach, ruling 1).
6. Whether every path that changes auto backup or the incoming delay emits the settings-changed event that re-registers the jobs (`AdvancedSettings.java:203-205` and `PackageReplacedReceiver.java:22-27` emit it; other writers were not searched), and so how often a trigger worker sees a delay of 0 or less in practice. Whether `setInitialDelay` with a negative value is accepted by the library was not checked.
7. Whether the content-trigger periodic work is re-armed after each run so that a second SMS fires it again, and how soon (the 15-minute periodic interval, `WorkManagerScheduler.kt:117`, `:240-247`). Not examined; it affects how the debounce behaves but not the criteria here.
8. Whether any owner-facing text (`README.md`) describes incoming backup and Wi-Fi only (layer 7 of the Impact Analysis).
9. The review checklist of U-014: that it did not check the network, degraded or auto-backup rules is read only from the two lines cited (`review-code.md:57-58`), not from the whole file.
10. REQ-MODERNIZATION-016 AC-8 was not re-read; its baseline role is as CR-015 states it.
11. Whether the content of the INV-2 table in the contract (long lines at `:108`, `:241-245`) says anything further about the trigger's follow-up: those lines were not read in full.
12. That the Android 17 behaviour in BUG-021 (a trigger at release) reaches this worker: BUG-021 itself marks it unmeasured.
