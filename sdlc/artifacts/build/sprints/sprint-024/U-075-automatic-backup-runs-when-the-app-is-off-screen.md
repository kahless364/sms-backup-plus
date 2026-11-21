---
type: story
status: in-progress
sprint: '024'
artifact_type: user-story
priority: high
complexity: medium
parallel_eligible: false
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies:
  - U-070
change_records: []
related_items:
  - BUG-029
  - BUG-016
  - BUG-024
  - BUG-025
  - BUG-026
  - BUG-027
  - U-049
  - U-067
  - U-069
  - U-070
  - U-071
  - U-072
  - U-073
  - U-074
  - REQ-MODERNIZATION-005
  - REQ-MODERNIZATION-013
  - REQ-MODERNIZATION-015
  - CR-016
  - DES-MODERNIZATION-005
  - CNTR-MODERNIZATION-004
platforms: []
tags:
  - workmanager
  - foreground-service
  - android-12
  - background-start-restriction
  - automatic-backup
  - backup
  - restore
  - bug-fix
  - device-check
gate_additions: []
id: U-075
title: Run automatic backups as ordinary background work so that they succeed when the app is off screen, keep the foreground notification only for backups and restores started from the screen, and let a refused foreground start continue instead of failing the run
pipeline: ''
domain: modernization
requirement_source: bug:BUG-029
updated_at: '2026-11-21T16:28:11.148Z'
---

# U-075: Run Automatic Backups as Ordinary Background Work So That They Succeed When the App Is Off Screen, Keep the Foreground Notification Only for Backups and Restores Started From the Screen, and Let a Refused Foreground Start Continue Instead of Failing the Run

## Story
As the owner of this private, unpublished SMS Backup+ build (its only user, on phones with Android 11 or later, who relies on automatic backup of new messages and on the regular schedule without opening the app),
I want every automatic backup (incoming, regular, after a restart, or started by a third-party broadcast) to back up whether or not the app is on screen, while a backup or restore I start from the screen still shows its progress notification,
So that on Android 12 and later new messages reach the backup without my opening the app, and the fixes of the scheduler stories that follow (BUG-025, BUG-026, BUG-027) are not cancelled out by every off-screen run failing at its first statement.

Source: bug BUG-029 (`sdlc/artifacts/stories/BUG-029-background-backup-fails-at-foreground-start.md`, status ready, severity high, priority high), observed on the Android 16 emulator on 2026-11-21 (`sdlc/artifacts/build/research/2026-11-21-background-backup-and-boot-observation.md`, Run 1, Run 2, "What it shows" 1-2). The approach is the bug's recommended option (c) (Suggested Approach, "Options" and "Exactly what is caught"), ruled by the orchestrator as ruling 60 (decision appendix Record 21, `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`). There is no REQ-* source; the story's source is the bug. The rulings are binding and are not re-decided here; the criteria below carry them.

**What the fix is, in one paragraph.** `BackupWorker.doWork` infers the backup type first and asks WorkManager for the foreground only for a run the user started from the screen (`MANUAL`, `SKIP`). `RestoreWorker.doWork` keeps asking for every restore (a restore is always user-started). Both requests go through one new internal helper that catches only the system's refusal (`android.app.ForegroundServiceStartNotAllowedException`, API 31 and later), writes one warning without message content or an address, and lets the run continue as ordinary background work; it rethrows a cancellation and every other exception. Automatic backups never ask, so they are never refused and show no progress notification, as before the scheduler migration (story U-049). Nothing else in the workers, the scheduler, the manifest or the build changes.

**Product owner item PO-K (held, decision appendix Record 21).** Ruling 60 builds the recommended answer "no": automatic backups show no progress notification. If the product owner answers PO-K "yes" before this story's build starts, the story is returned to the orchestrator for revision (the bug's option (a)); the developer does not change the criteria on its own. If the answer comes after the build, it is a new story.

**Not in this story** (each ruled or recorded; none is an acceptance criterion):
- The regular schedule lost after a restart (BUG-027) and the "Never" schedule (BUG-026): the scheduler-fix story.
- The delay of incoming backups (BUG-025): its text-only story.
- Removing the SMS receiver.
- The cancellation reporting of the workers' broad catches (BUG-024, story U-074). This story leaves those catches as they are. U-074's search for catch sites (its IC-1) will find the new helper's catch; it is listed there as left on purpose (IC-2 below).
- Any prompt to exempt the app from battery optimisation (product owner item PO-L; the fix does not need one).
- Any change to scheduling (delays, constraints, expedited work, tags, unique names, policies) or to the manifest's `SystemForegroundService` declaration. The bug shows neither is required (bug Impact Analysis rows 7 and 3; Suggested Approach option (b), rejected).
- Any contract or design edit (Integration Contract References below).

## Acceptance Criteria

**Terms.**
- **Start** is the commit that closes story U-070 (U-070's last commit; story U-069 is committed before it). Line references in this story are "at commit `b096514b` (before U-070)", read with `git show b096514b:<path>`. `git diff --stat 33706a13 b096514b -- app/` is empty, so the bug's line numbers (read at `33706a13`) hold at `b096514b`. U-070 edits other lines of both worker files; the plan re-anchors every line on Start (Technical Context, obligation P-3).
- **Automatic types**: `INCOMING`, `REGULAR`, `BROADCAST_INTENT`, and a run with no recognised type tag, which `inferBackupType()` resolves to `BROADCAST_INTENT` (`BackupWorker.kt:501-507` at `b096514b`). **User-started types**: `MANUAL` and `SKIP`, exactly those for which `BackupType.isBackground()` is false (`service/BackupType.java:37-39` at `b096514b`).
- **The refusal**: `android.app.ForegroundServiceStartNotAllowedException`.
- **Throwing updater**: a `androidx.work.ForegroundUpdater` passed to `TestListenableWorkerBuilder.setForegroundUpdater(...)` whose `setForegroundAsync` throws the named exception (bug Acceptance Criteria, "Foreground updater"). **Counting updater**: one that records each call and its `ForegroundInfo`, notes the call's position relative to the transport factory's `create()` and the provider queries, and returns a completed future. Every other test runs on the builder's default `TestForegroundUpdater`, which completes at once.
- **The refusal level**: the Robolectric SDK level (31 or higher) the plan establishes under obligation P-1. Every test that constructs the refusal or an Android 14 type exception runs at `@Config(sdk = [<refusal level>])`; the type exceptions need 34 or higher.
- **The warning**: one entry at `Log.WARN` with tag `SMSBackup+`, whose text starts with the fixed prefix `BackupWorker: foreground start not allowed; continuing without the foreground` (or `RestoreWorker: ...` for a restore) and contains the exception's simple class name. The plan fixes the final text; the prefix is binding.
- **Fixtures**: U-070's provider double and recording transport as built (in U-070's plan, `ProviderDouble`, `registerProvider`, `RecordingTransport`, `LateSmsHarness` in `LateVisibleSmsFixtures.kt`), reused without editing that file; any helper this story needs goes into its own new test files. Log assertions use Robolectric's `ShadowLog`, cleared in `@Before`.
- "Red at Start" and "green at Start" are the results of the test written first and run against the unchanged production code at Start; the plan records the output. Test names are working names; the plan fixes them.

### A. Behaviour of the workers (unit tests)

- [ ] AC-1 (a user-started backup whose foreground start is refused still runs and backs up): Given a `MANUAL` run, a throwing updater that throws the refusal, one SMS row served by the provider double, and the recording transport, when `doWork()` runs at the refusal level, then it returns `Result.Success`, the transport saw exactly one append, and the SMS watermark equals the date that append confirmed. Given a `SKIP` run with the same throwing updater, `doWork()` returns the same `Result` and leaves the same SMS watermark as the same fixture with the default updater (the control is run in the same test class). Checked by tests `BackupWorkerForegroundTest.manual_foregroundRefused_continuesAndBacksUp` and `skip_foregroundRefused_sameOutcomeAsAllowed`. Both red at Start: today `doWork` throws the refusal at `BackupWorker.kt:123` (`b096514b`), outside the `try` that starts at `:146`.
- [ ] AC-2 (the refusal is logged once, at warning, with no personal data): In each run of AC-1, `ShadowLog` holds exactly one warning (Terms), whose throwable, if attached, is the refusal; no entry at `Log.ERROR` with tag `SMSBackup+`; and no entry under any tag whose text contains the row's body, the row's address, the configured account name or a folder name. The line "BackupWorker.doWork: starting backup, type=MANUAL, attempt=0" (today's text, `BackupWorker.kt:144` at `b096514b`) is still written. Checked by the assertions of AC-1's tests and of AC-6's refusal test (prefix `RestoreWorker:`). Red at Start. The level survives in release builds: no `-assumenosideeffects` rule exists under `app/` (`git grep -n assumenosideeffects b096514b -- app` prints nothing; the plan repeats the search at Start).
- [ ] AC-3 (automatic backups do not ask for the foreground and are therefore never refused): For each automatic type (`INCOMING`, `REGULAR`, `BROADCAST_INTENT` as the tag, and a run with no type tag), with a transport factory whose transport throws `MailException` (the fixture `BackupWorkerIntegrationTest.doWork_mailExceptionFromTransportFactory_returnsRetry` uses at Start; the plan names it), when `doWork()` runs:
  - (a) with a counting updater, the updater records 0 calls, the line "BackupWorker.doWork: starting backup, type=<type>, attempt=0" is written, and the result is `Result.retry()`, as today's mapping gives (`BackupWorker.kt:164-167` at `b096514b`);
  - (b) with a throwing updater that throws the refusal, the result is the same `Result.retry()`, and no warning is written.

  Checked by tests `BackupWorkerForegroundTest.automaticTypes_doNotRequestForeground` and `automaticTypes_foregroundRefused_runAsToday` (parameterised over the four cases or one test per case; the plan decides). Both red at Start: today the updater records 1 call, and with the throwing updater `doWork` throws.
- [ ] AC-4 (a user-started backup that can enter the foreground still does, with today's notification): For `MANUAL` and `SKIP`, with a counting updater, `doWork()` calls the updater exactly once, before the transport factory's `create()` and before any provider query. The `ForegroundInfo` it passes has notification id `BackupWorker.BACKUP_NOTIFICATION_ID` (1), a notification on channel `App.CHANNEL_ID`, and, at API 29 or higher, foreground-service type `ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC` (`BackupWorker.kt:516-539`, `:545` at `b096514b`). `git diff <start> -- app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` shows no changed line inside `createBackupForegroundInfo()`. Checked by test `BackupWorkerForegroundTest.userStartedTypes_requestForegroundOnce`. Green at Start and after.
- [ ] AC-5 (nothing but the refusal is swallowed): For a `MANUAL` run:
  - (a) an updater that throws `IllegalStateException("x")` (the type of the library's own "Calls to setForegroundAsync() must complete..." error) makes `doWork()` throw that exception;
  - (b) an updater that throws `android.app.InvalidForegroundServiceTypeException("x")`, and one that throws `android.app.MissingForegroundServiceTypeException("x")`, make `doWork()` throw that exception (a manifest regression like BUG-016 stays loud; their parent `ServiceStartNotAllowedException` is not caught);
  - (c) an updater that throws `SecurityException("x")` makes `doWork()` throw it;
  - (d) an updater that cancels the job running `doWork()` and then throws `CancellationException("test")` (story U-067's child-coroutine harness) makes `doWork()` end by throwing a `CancellationException`, with no warning written, no transport created and no provider query made.

  In (a) to (c) no warning is written and no transport is created. Checked by tests `foreground_otherIllegalState_propagates`, `foreground_typeException_propagates`, `foreground_securityException_propagates`, `foreground_cancelled_endsCancelled`. All green at Start (today everything propagates) and after.
- [ ] AC-6 (a restore gets the same handling and still asks for the foreground every time): `RestoreWorker.doWork()` calls the updater exactly once, before the transport factory's `create()` (counting updater; the `ForegroundInfo` is today's `createRestoreForegroundInfo()`, `RestoreWorker.kt:690` at `b096514b`, unchanged, id `RESTORE_NOTIFICATION_ID` 2, `:718`). With a throwing updater that throws the refusal it continues: the line "RestoreWorker.doWork: starting restore, attempt=0" is written, the result is the one the transport gives (with today's test transport factory, `Result.retry()` on its `MailException`, `RestoreWorker.kt:194-197` at `b096514b`; the plan may use the existing `TestableRestoreWorkerFactory` fixtures and names the expected result), and the warning with prefix `RestoreWorker:` is written exactly once with no `Log.ERROR` entry. AC-5 (a), (b) and (d) hold for the restore. Checked by tests in a new `RestoreWorkerForegroundTest` (working names `restore_requestsForegroundOnce`, `restore_foregroundRefused_continues`, `restore_foreground_otherIllegalState_propagates`, `restore_foreground_typeException_propagates`, `restore_foreground_cancelled_endsCancelled`). The refusal test is red at Start (today `doWork` throws at `RestoreWorker.kt:148`, outside the `try` at `:165`); the others are green at Start and after.
- [ ] AC-7 (what a refused user-started run does, settled by the plan, not assumed): The plan records, before the build, what happens to a `MANUAL` backup or a restore whose foreground request is refused, from what the bug read in WorkManager 2.9.1 (Suggested Approach, facts L3 and L4) and from reading `MainViewModel.kt` and `StatusPreference.java` at Start:
  - the run continues and its result is applied (AC-1, AC-6);
  - no progress notification is shown for it;
  - whether its progress still reaches the status screen through `setProgress` (expected yes: the status screen suppresses only background types, `StatusPreference.java:234` at `b096514b`, and `MANUAL` is not one);
  - that WorkManager keeps the work in its foreground map, so a system stop of the job is ignored until the work ends (L4).

  **Stop condition:** if the reading shows that a refused run's result is not applied, or that its work can stay `RUNNING` after `doWork` returns, the developer stops and reports before the build; the story does not continue on an assumption. Checked by the plan's recorded reading (copied to Implementation Notes) and by device step (v) when it is run.

### B. Scope, gates and proof

- [ ] AC-8 (only the two workers and the new helper change; no scheduling, manifest or build change): `git diff --name-only <start> -- app/src/main` lists exactly `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt`, `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt` and the new `app/src/main/java/com/zegoggles/smssync/service/WorkerForeground.kt`. In `BackupWorker.kt` every changed line is inside `doWork()`; in `RestoreWorker.kt` the only changed lines are the foreground call at the top of `doWork()` and the comment above it (the U-049 comments at `BackupWorker.kt:119-122` and `RestoreWorker.kt:144-147` at `b096514b` state a wrong reason, bug Root Cause Analysis, and may be rewritten). The type is inferred before the foreground request; the backoff check, the "starting backup" line and the `try` follow in today's order. `git diff --name-only <start> -- build.gradle app/build.gradle k9mail-vendored/build.gradle settings.gradle gradle sdlc/artifacts/design sdlc/artifacts/build/change-records` is empty. `git diff <start> -- app/src/test` shows added files only; `WorkManagerSchedulerTest.kt` and every existing test, U-067's and U-070's included, pass unedited. Checked by those commands, run at the end, with their output in the evidence.
- [ ] AC-9 (gate, coverage): On JDK 25 as declared by `gradle/gradle-daemon-jvm.properties`, in a shell without `MSYS_NO_PATHCONV`, with an explicit timeout above three minutes on every Gradle call, `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease` runs at Start (before the first edit) and at the end (no failure injection present). Both exit 0 with 0 test failures, and the end run's skipped count is not higher than Start's. No JaCoCo floor is lowered and no test is `@Ignore`d. A package whose measured LINE coverage falls because of this story's lines (in particular `com.zegoggles.smssync.service`, which holds the helper) is a stop: the developer reports it; the floor is not changed.
- [ ] AC-10 (test counts, REQ-MODERNIZATION-015 AC-13 as amended by CR-016 Amendment 1): The authoritative count `git grep -h "@Test" <commit> -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"` is recorded at Start with the Start commit's id, and at the end with the story's last commit in place of `<commit>` (or, before the commit, with the working-tree form and `--untracked`, stated as such). Tests that come from changes other than this story's are listed by name and not counted. The difference equals this story's new tests, each listed by name. No fixed count is stated in this story.
- [ ] AC-11 (each new test detects what it names): Each failure injection is applied alone and recorded in `mutation-log.md` in the story's evidence folder with the U-074 AC-15 protocol (each changed production file saved and its `sha256sum` recorded first; a write-ahead line; one targeted test run; restore from the saved copy; the hash shown equal to the saved one; `git status --porcelain -- app/src/main` listing only this story's files; a mutation is never present at a gate, a review or a commit; a mutation that does not compile is logged invalid with its compiling replacement):

  | Id | Injection (one change) | Tests that must fail |
  |----|------------------------|----------------------|
  | M-F1a | In `BackupWorker.doWork`, call `setForeground(...)` directly in place of the helper | AC-1, AC-2 tests |
  | M-F1b | The same in `RestoreWorker.doWork` | AC-6 refusal test |
  | M-F2 | In the helper, continue on every `IllegalStateException` (drop the refusal test) | AC-5 (a), (b), (d); AC-6's (a), (b), (d) counterparts |
  | M-F3 | In the helper, catch `ServiceStartNotAllowedException` (the parent) in place of the refusal | AC-5 (b) and its restore counterpart |
  | M-F4 | In `BackupWorker.doWork`, ask for the foreground for every type | AC-3 (a) |
  | M-F5 | In `BackupWorker.doWork`, ask for no type | AC-4 |
  | M-F6 | Delete the `Log.w` line of the helper | AC-2 tests |

  The plan derives the complete failing set per row before running it, and a row is met when the observed set equals the derived set. In addition, U-070's mutations whose code lies in `BackupWorker.kt` (M-L11, M-L20, M-L22 in U-070's plan table) are re-run after the fix and each is still detected by exactly the failing set U-070 recorded. Checked by `mutation-log.md`.

### C. On the device (run by the orchestrator only)

- [ ] AC-12 (device, emulator `Pixel_9_Pro`, Android 16, API 36; `Pixel_10_Pro` is never started or touched): The orchestrator installs the story's debug build over the current one with data kept. Nothing but the app's own state is changed (its settings, a test SMS, opening and leaving the app): no device setting, permission, battery-optimisation exemption or standby-bucket command. Set-up as the bug's Steps to Reproduce step 1 (mail account configured, auto backup on, incoming delay 1 minute, regular schedule at its default, `dumpsys deviceidle whitelist` without a line for the package). The orchestrator reads `adb -s <serial> logcat -d` filtered to `SMSBackup\+|WM-WorkerWrapper|WM-Processor|WM-SystemFgDispatcher|ForegroundServiceStartNotAllowedException`, for the app's own process only. Steps, in this order:
  - **(iii) Manual, on screen.** With the app on screen, press the backup button. Met when the log shows "Moving WorkSpec (...) to the foreground" for that work, a screenshot of the notification shade (`adb -s <serial> exec-out screencap -p > <file>`) taken while the run is logged shows the "Backup" notification, and the log shows "Worker result SUCCESS" for the work tagged `MANUAL`.
  - **(i) Incoming, off screen.** The bug's Steps to Reproduce steps 4 to 7: open the app for about three seconds, press Home, send one test SMS from the emulator console (`adb -s <serial> emu sms send 5550100 "U-075 test"`; no personal data in number or text), and wait at least 90 seconds with the app off screen. Met when, for the work tagged `INCOMING` whose "BackupWorker.doWork: starting backup, type=INCOMING" timestamp is later than the Home press: there is no "Moving WorkSpec (...) to the foreground" line for it; the log shows "backup complete, backedUp=1" (or the run's equivalent line at Start, which the plan names) and "Worker result SUCCESS" for it; the test SMS is in the mail account's SMS folder (checked by the orchestrator); and a screenshot of the notification shade taken while the run is logged shows no "Backup" notification. If the incoming backup does not start within 15 minutes of the SMS and `dumpsys jobscheduler` shows it held by job quota (BUG-025), (i) is recorded "not run: held by the system's job quota" with that line, and repeated once after opening the app for about three seconds again.
  - **(ii) Boot backup, never opened.** Cold-start the emulator without saving a snapshot (`-no-snapshot-save`), wait until `getprop sys.boot_completed` prints `1`, and do not open the app. Observe for 15 minutes from `BOOT_COMPLETED`. Met when the boot backup (work tagged `REGULAR`, enqueued with "enqueued bootup REGULAR with 60s delay") starts in that window and ends "Worker result SUCCESS" with no refusal for it. **If it does not start in that window** and `dumpsys jobscheduler` shows the job held for quota or standby (`WITHIN_QUOTA` unsatisfied, as in the observation's Run 2), (ii) is recorded "not reached: held by the system's job quota (BUG-025)" with that line as evidence. That is neither a pass nor a failure of this criterion: it is not reported as passed, and the orchestrator states in the QA record that the boot case rests on (i) and AC-3. A refusal or a "Worker result FAILURE" for the boot backup is a failure.
  - **(iv) No refusal off screen.** Across the whole of (i) and (ii), the log has no line containing `ForegroundServiceStartNotAllowedException` from the app's process.
  - **(v) Optional, the catch on a device.** Airplane mode on, press the backup button, press Home at once. The retry about 30 seconds later starts off screen; it logs the warning once and ends with the transport's result (expected "MailException — retrying"; whether the failed connection maps to `MailException` is not verified, bug Not verified 11). Airplane mode off: a later retry ends SUCCESS. Not required; it informs AC-7.

  A step that cannot be run is recorded "not run" with the reason and is never reported as passed. Evidence: the log lines passed through the sanitizer of story U-063 and the screenshots, in the story's evidence folder. The owner's e-mail address, machine names and profile paths never go into the evidence; raw device logs stay outside the repository.

### Integration Criteria

The fix creates one new internal function and calls it from two places; the scaffold's registry criteria are restated for that.

- [ ] IC-1 (one helper, two call sites, no signature change): The helper is one `internal suspend` function in the new file `app/src/main/java/com/zegoggles/smssync/service/WorkerForeground.kt`, package `com.zegoggles.smssync.service` (working shape from the bug, Suggested Approach, "Exactly what is caught": `CoroutineWorker.setForegroundOrContinue(info: ForegroundInfo, tag: String, site: String): Boolean`). `git grep -n "setForeground(" <end> -- app/src/main` shows the call inside the helper only, and `git grep -n "setForegroundOrContinue" <end> -- app/src/main` shows the definition and exactly two calls (`BackupWorker.doWork`, `RestoreWorker.doWork`). The constructors of `BackupWorker` and `RestoreWorker`, `TestableBackupWorkerFactory`, `TestableRestoreWorkerFactory`, `TestableBackupWorkerFactoryWithTransport`, U-070's fixtures, `createBackupForegroundInfo()` and `createRestoreForegroundInfo()` are unchanged, so no caller or test double needs an update. Checked by `:app:assembleDebug` and by those searches.
- [ ] IC-2 (the new catch is handed to story U-074): The helper's `catch (e: IllegalStateException)` rethrows a `CancellationException` as its first act and continues only on the refusal. It is recorded in this story's Implementation Notes as the one new catch on the worker run path, for U-074's IC-1 search, to be listed there as "left on purpose". This story does not edit U-074; the orchestrator carries the line into U-074's plan. Checked by reading the helper and the Implementation Notes entry.
- [ ] IC-3 (reached from production): For `MANUAL` and `SKIP` the helper is reached from `WorkManagerScheduler.scheduleManual` through WorkManager to `BackupWorker.doWork`; for a restore from `scheduleRestore` to `RestoreWorker.doWork`. Checked by AC-4, AC-6 and device step (iii).

## Affected Code

Lines are at commit `b096514b` (before U-070). U-070 edits other lines of both worker files (below `doWork`'s catch in `BackupWorker.kt`; the SMS import in `RestoreWorker.kt`). **Locate every site by function name and statement text, not by line number**; the plan re-derives each line at Start.

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` (`doWork`, `:118-145`) | First statement `setForeground(createBackupForegroundInfo())` (`:123`), for every type, outside the `try` (`:146-171`); the type is inferred only at `:142`. A refusal escapes `doWork` and WorkManager records FAILURE (bug Description items 1-5) | `val backupType = inferBackupType()` first; then `if (!backupType.isBackground())` the helper call with `createBackupForegroundInfo()`; then the backoff check (`:125-137`), the "starting backup" line (`:144`, text unchanged) and the `try`, as today. Both catches unchanged |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt` (`doWork`, `:143-148`) | First statement `setForeground(createRestoreForegroundInfo())` (`:148`), outside the `try` (`:165`) | The helper call with `createRestoreForegroundInfo()`, always. Nothing else, in particular neither catch at `:194-201` |
| `app/src/main/java/com/zegoggles/smssync/service/WorkerForeground.kt` (new) | Does not exist | The helper (IC-1): `try { setForeground(info); return true }`; on `IllegalStateException`: rethrow if it is a `CancellationException`; if `SDK_INT >= S` and it is the refusal, write the warning and return `false`; otherwise rethrow |
| `app/src/test/java/com/zegoggles/smssync/service/BackupWorkerForegroundTest.kt` (new) | No test makes `setForeground` fail; every test runs on `TestForegroundUpdater` | AC-1 to AC-5 |
| `app/src/test/java/com/zegoggles/smssync/service/RestoreWorkerForegroundTest.kt` (new) | Same | AC-6 |

Read, not changed: `WorkManagerScheduler.kt`, `AndroidManifest.xml` (`:126-129`, the `dataSync` override, still needed for manual backups and restores), `MainViewModel.kt` and `StatusPreference.java` (AC-7), U-070's fixtures and tests, U-067's harness, CNTR-MODERNIZATION-004, DES-MODERNIZATION-005.

## Existing Behavior to Preserve

- A manual backup, a Skip and a restore started on screen show today's ongoing notification: same channel, id, text, content intent and `dataSync` type (`BackupWorker.kt:516-539`; `RestoreWorker.kt:690` onward, at `b096514b`). The manifest's `SystemForegroundService` override (`AndroidManifest.xml:126-129`) stays.
- The 300-second backoff cap and its failure reason "backoff_cap_exceeded" (`BackupWorker.kt:125-137`; `RestoreWorker.kt:150-158`).
- Every typed mapping of both workers: `MailException` to `Result.retry()`; `XOAuth2FailedException` to the token refresh; `RequiresLoginException` to "auth_failed"; `SecurityException` to "missing_permission"; the broad catch to `Result.failure(message or "unknown")`. The cancellation behaviour of U-067, and of U-074 when it lands.
- The watermark rule: advance only after a confirmed append, per append (BUG-010; `appendBatchAndUpdateWatermark`, `BackupWorker.kt:405-425`), and U-070's late-visible-SMS behaviour.
- The log lines device checks read: "BackupWorker.doWork: starting backup, type=..., attempt=...", "starting backup (N messages)", "backup complete, backedUp=N", "nothing to backup", "RestoreWorker.doWork: starting restore, attempt=...".
- Scheduling in full: types, delays, constraints, backoff, tags, unique names, policies (AC-8).
- The status screen's suppression of background-type progress except errors (`StatusPreference.java:234`).
- Android 11 and earlier: user-started runs enter the foreground as today; automatic runs no longer do (the same behaviour on every Android version).

## Verification Steps

Run Gradle in a shell without `MSYS_NO_PATHCONV`, on JDK 25 (`./gradlew --version` shows the Daemon JVM line citing `gradle/gradle-daemon-jvm.properties`), with an explicit timeout above three minutes. Do not call `diff` or `cmp`; compare with `git diff --no-index` or `sha256sum`. `<start>` is the Start commit.

1. **Start conditions.** `git log` shows U-069 and U-070 committed and done; record `<start>`. Re-read both worker files and U-070's fixtures as built (Technical Context, obligation P-3). Settle obligations P-1 and P-2 and record the result in the plan. Run the gate once and record the count with the commit id (AC-9, AC-10). If a start condition fails, stop and report.
2. **Characterization first.** Write AC-4's test, AC-5's tests and AC-6's non-refusal tests. Run them against the unchanged production code; all are green. Record the output.
3. **Failing first.** Write AC-1, AC-2, AC-3 and AC-6's refusal test. Each fails for the stated reason (today `doWork` throws the refusal; today 1 updater call for automatic types), not on a compile or fixture error. Record the output.
4. **Fix.** Add `WorkerForeground.kt` and the two call sites. All new tests are green. Read the warning line against AC-2 (prefix, level, class name, no personal data).
5. **Unchanged behaviour.** Existing tests pass unedited; `git diff <start> -- app/src/test` shows added files only (AC-8).
6. **AC-8.** Run the two `git diff --name-only` commands; the first lists the three files, the second is empty. Read the `BackupWorker.kt` diff: every changed line in `doWork`.
7. **AC-11.** Run M-F1a to M-F6 one at a time by the protocol, then U-070's M-L11, M-L20 and M-L22; compare observed and derived failing sets; read `mutation-log.md` for the write-ahead, failing-test and `restored` lines and the `sha256sum` equality.
8. **AC-9, AC-10.** Gate at the end; the count at the end as AC-10 says; no floor changed (`git diff <start> -- app/build.gradle` empty).
9. **IC-1 to IC-3.** Run the two `git grep` searches of IC-1; record the helper's catch for U-074 in Implementation Notes.
10. **AC-12.** The orchestrator's device run, steps (iii), (i), (ii), (iv) in that order, optionally (v), with the sanitized log and the screenshots. The developer does not run the device steps.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Kotlin, coroutines, WorkManager 2.9.1, Robolectric 4.15.1, Gradle) | `WorkerForeground.kt`, the two call sites, the two new test classes, the failure injections, the gates, obligations P-1 to P-3 | Developer |
| Device check | AC-12 on `Pixel_9_Pro` only | Orchestrator |
| Design text (optional, non-breaking) | The clarification of DES-MODERNIZATION-005 `:129-131` the bug proposes; not this story's work | Orchestrator, Artifact Librarian |

## Technical Context

### Start conditions (each with a stop rule)

1. **U-070 is done and committed** (this story's dependency; U-070 depends on U-069, so U-069 is committed too). Both stories edit `BackupWorker.kt`; U-070's plan is line-anchored and hashed. Stop rule: if `git log` does not show U-070 closed at Start, stop and report. The working tree when this story was written held another agent's uncommitted changes under `app/` (U-069's build); nothing in this story relies on them.
2. **Gate green at Start.** Stop rule: if the gate fails at `<start>`, stop and report.
3. **Placement (ruling 60).** This story is built directly after U-070 and before U-074, the toolchain stories (U-071, U-072, U-073), the scheduler-fix story for BUG-026 and BUG-027, and the text-only story for BUG-025. The Sprint Manager places it when sprint 024 is revised.

### Plan obligations (the bug's "not verified" points, made explicit)

- **P-1. Can the tests run at SDK 31 or higher, offline?** The project pins `sdk=29` globally (`app/src/test/resources/robolectric.properties:8` at `b096514b`) and per class (at `b096514b`: `sdk = [29]` 10 times, `sdk = Build.VERSION_CODES.Q` 7 times, `sdk = Build.VERSION_CODES.N` twice, `sdk = [24]` once; no test runs above 29). The refusal class exists from API 31, the type exceptions from API 34, and the helper guards on `SDK_INT`. Robolectric is 4.15.1 (`app/build.gradle:160`). The bug saw `android-all-instrumented` jars for Android 15 and 16 in the local Maven cache but did not check that 4.15.1 resolves to them (bug Not verified 7). The plan establishes, **offline, from what is already in the local caches** and without a network fetch, a level of 34 or higher at which a test class runs, by reading Robolectric 4.15.1's SDK table in the cached jar and confirming the matching `android-all-instrumented` jar is present, then by one probe test class run at that level. It also confirms that U-070's fixtures and U-067's harness work at that level (they are written at 29). **If no level of 34 or higher works offline:** a level of 31 to 33 carries AC-1, AC-2, AC-3 (b), AC-5 (a), (c), (d) and AC-6, and AC-5 (b) is recorded as "proved by reading the helper and by M-F3 not runnable", with a Scope Concern to the orchestrator. **If no level of 31 or higher works offline:** the developer stops and reports before the build. The tests then cannot construct the refusal, the refusal criteria (AC-1, AC-2, AC-6's refusal test, M-F1, M-F2, M-F6) and the helper's coverage could rest only on the device step, and the coverage floor of the `service` package might fall (AC-9). The orchestrator decides (for example, under the precedent of ruling 37, whether a download by the test framework is allowed). No production seam is added to make the refusal testable at SDK 29.
- **P-2. A refused manual backup or restore.** As AC-7: the plan records what 2.9.1 does with it (bug Suggested Approach L3, L4), what the user sees, and whether `setProgress` still reaches the status screen. Stop condition as AC-7. The bug's Not verified 5 (process freezing or a kill while a refused run goes on after a system stop) stays not verified and is named in Implementation Notes; it is the same trade the bug accepts for a rare case (a retry that starts after the user left).
- **P-3. Re-anchor on Start.** Every line number in this story is at `b096514b`. The plan re-derives each site in both worker files at Start, the names of U-070's fixtures as built, and U-070's mutation table rows M-L11, M-L20, M-L22. If a name differs, the story is read through the built name and the plan records the mapping.

### Binding constraints for the plan

- **Comparison tool (product owner's decision of 2026-11-21, decision appendix Record 18):** no agent calls the external programs `diff` or `cmp`; comparisons use `git diff --no-index` or `sha256sum`. Shell activity stays modest. If a tool disappears or a process is killed without a visible reason, the agent stops and reports. Nothing is restored from quarantine and no exclusion is added by any agent.
- **What is caught (ruling 60; bug Suggested Approach, "Exactly what is caught"):** only `ForegroundServiceStartNotAllowedException`, only when `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` (the class does not exist on API 24-30). Never swallowed: `CancellationException` (checked first: `java.util.concurrent.CancellationException` is an `IllegalStateException`), `InvalidForegroundServiceTypeException` and `MissingForegroundServiceTypeException` (siblings of the refusal under `ServiceStartNotAllowedException`, so the parent is not caught), the library's own `IllegalStateException`, `SecurityException`, every `Error`.
- **An equivalent mutation, known in advance.** With the shape above, deleting the cancellation rethrow is not detectable: a `CancellationException` is not the refusal, so it reaches the final `throw e` anyway. The first-line rethrow is kept on purpose (it keeps the rule visible and survives a later reordering), and it is not in AC-11's table; M-F2 covers the risk it guards against.
- **No test edits, no new seam, no dependency change, no `@Ignore`, no floor lowered, no device step by the developer.** The developer commits nothing.
- **Test-count rule:** AC-10. A planning figure for the number of new tests is not a criterion.

### Library and platform facts the approach rests on

Read by the Solution Architect in the bug, not run here: bug Description items 2-5 and Suggested Approach facts L1-L10 (WorkManager 2.9.1 bytecode and 2.12.0 sources; Android 16 SDK sources). In short: the refusal surfaces in `doWork` unwrapped at `setForeground` (L1); uncaught, the run is FAILED, or skipped to the next period for periodic work (L2); a work whose start was refused stays in WorkManager's foreground map, so a system stop is ignored until it ends (L3, L4); work that never asks runs as an ordinary job whose stops are honoured and rescheduled (L5, L6); expedited work cannot be delayed or periodic (L7); the watermark makes any stopped run lossless (L10). WorkManager 2.12.0 (story U-073) behaves the same at L1-L4, so U-073 carries this story's tests unedited.

### Known stale comment

`BackupWorkerIntegrationTest.kt:40` ("Foreground notification setup (always runs in doWork)") and `:76` at `b096514b` become partly stale for automatic types. AC-8 forbids editing existing tests, so the comment is listed in Implementation Notes as known and stale.

## Supporting Documentation

- Bug: `sdlc/artifacts/stories/BUG-029-background-backup-fails-at-foreground-start.md` (Description items 1-8, Steps to Reproduce, Acceptance Criteria, Suggested Approach "Options", "Exactly what is caught", "Interactions", "Where it goes", Verification Steps, Root Cause Analysis, Notes PO-1, PO-2, Not verified).
- Observation: `sdlc/artifacts/build/research/2026-11-21-background-backup-and-boot-observation.md`; sanitized log `sdlc/artifacts/build/research/evidence/2026-11-21-background-backup-app-log.txt`.
- Decision appendix, `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`: Record 21 (rulings 60, 61; held items PO-K, PO-L), Record 18 (comparison tool).
- Requirements: REQ-MODERNIZATION-015 AC-13 as amended by CR-016 Amendment 1 (count rule); REQ-MODERNIZATION-013 AC-8 (a documented decision whether foreground promotion for manual backups is kept through `setForeground()`; this story's plan records the decision: kept for `MANUAL`, `SKIP` and restores, dropped for automatic types); REQ-MODERNIZATION-005 ("preserving all current scheduling behavior", met by AC-8).
- Stories: U-070 (`sdlc/artifacts/build/sprints/sprint-024/U-070-late-visible-sms-are-backed-up.md`, its plan's fixtures and mutation table); U-074 (`sdlc/artifacts/build/sprints/sprint-025/U-074-cancelled-run-is-not-reported-as-a-failure.md`, IC-1, AC-15 protocol); U-067 (the child-coroutine cancellation harness); U-049 (`sdlc/artifacts/build/sprints/sprint-012/U-049-retire-the-legacy-backuprestore-service-dual-dispatch-layer.md`, where the defect entered).

## Integration Contract References

- **CNTR-MODERNIZATION-004** (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md`). Searched at `b096514b` for "foreground" and "notification": the one hit is Validation Rule 8 at `:280`, which speaks of "the manual notification/foreground semantics" of `scheduleManual`. It does not state that automatic backups run in the foreground or show a notification; this story keeps the manual semantics. `:284` (non-retryable failures return `Result.failure()`) is better met: a refusal is no longer a failure.
- **DES-MODERNIZATION-005** (`sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md`). Searched at `b096514b`: `:54` ("owns worker lifecycle plus foreground promotion") and `:129-131` ("where a user-visible notification is required it uses `setForeground` / `getForegroundInfo`"). Neither states that automatic backups run in the foreground or show a notification; both hold under this fix.
- **So no change record is required before the build.** The bug proposes an optional, non-breaking clarification (DES-MODERNIZATION-005 `:129-131` and a Risks row at `:487-496`; optionally a sentence after CNTR-MODERNIZATION-004 `:287`, folded into U-074's hand-over H-1 if that is not yet written). It is the orchestrator's to raise after this story or with it; it does not gate the code. This story edits no contract or design file (AC-8).

## Implementation Notes
<!-- Added by agents during build -->

None yet. Record here: the Start commit and counts; the outcome of P-1 (the refusal level and how it was established offline), P-2 (the reading of a refused manual run or restore) and P-3 (the mapping of names and lines); the failing output at Start for the red tests; the helper's catch for U-074's IC-1 (IC-2); the stale comment in `BackupWorkerIntegrationTest.kt`; the bug's Not verified 5 as still not verified.

## Review Findings
<!-- Summarized from workspace artifacts -->

None yet.

## Notes

- Origin: BUG-029, raised by the orchestrator's device observation under ruling 55 (Record 21), approach by the Solution Architect, ruled as ruling 60. Written 2026-11-21.
- **Ruling 61 stays in force until this story is built and its device criterion has run:** device steps of other stories that need a backup to run keep the app on screen from before the backup starts until it ends, and a check whose subject is a start off screen is recorded "blocked by BUG-029".
- **Hand-over items (not this story's work):**
  - **H-1 (orchestrator):** carry the helper's catch into U-074's plan as "left on purpose" under its IC-1 (IC-2).
  - **H-2 (orchestrator, optional):** the non-breaking design clarification of Integration Contract References.
  - **H-3 (story U-073):** a line in its notes that WorkManager ignores scheduler stops for work it moved to the foreground even when the start was refused; after this fix that matters only for a refused manual backup or restore (bug Notes, "Observations outside this bug").
- **Product owner items:** PO-K (notification for automatic backups; the fix builds "no"; see the Story section for what a "yes" does) and PO-L (battery-optimisation prompt; "no"; nothing here asks for an exemption). Neither blocks the build.
