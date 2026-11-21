---
type: story
status: planned
sprint: '025'
artifact_type: user-story
priority: high
complexity: medium
parallel_eligible: false
iteration: 1
requirements: []
design_docs: []
integration_contracts:
  - CNTR-MODERNIZATION-004
dependencies:
  - U-072
  - U-075
change_records: []
related_items:
  - BUG-026
  - BUG-027
  - CR-017
  - CR-016
  - BUG-025
  - BUG-029
  - U-014
  - U-017
  - U-054
  - U-069
  - U-071
  - U-072
  - U-073
  - U-074
  - U-075
  - U-077
  - U-078
  - CNTR-MODERNIZATION-004
  - DES-MODERNIZATION-005
  - REQ-MODERNIZATION-005
  - REQ-MODERNIZATION-015
  - REQ-MODERNIZATION-016
platforms: []
tags:
  - workmanager
  - scheduler
  - regular-backup
  - boot
  - unique-work
  - inv-1
  - never
  - bug-fix
  - device-check
  - cntr-004-v4
gate_additions: []
id: U-076
title: Keep the regular backup registered across a phone restart, re-register it where an earlier build left a one-time entry in its place, and make the regular schedule "Never" schedule nothing
pipeline: ''
domain: modernization
requirement_source: bug:BUG-027
updated_at: '2026-11-21T18:10:56.665Z'
---

# U-076: Keep the Regular Backup Registered Across a Phone Restart, Re-register It Where an Earlier Build Left a One-Time Entry in Its Place, and Make the Regular Schedule "Never" Schedule Nothing

## Story
As the owner of this private, unpublished SMS Backup+ build (its only user, with auto backup on and the regular schedule at its default of two hours, restarting the phone from time to time without opening the app afterwards),
I want a restart to leave my regular backup schedule exactly as it was, a phone that has already lost it to get it back on its own, and the regular schedule "Never" to mean that no regular backup runs,
So that the "every 2 hours" the settings show is what the app does after every restart, and "Never" stops the regular backup instead of turning it into one every fifteen minutes.

Sources: two bugs, fixed in one story by the orchestrator's ruling 55 (decision appendix `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`, Record 20):
- **BUG-027** (`sdlc/artifacts/stories/BUG-027-regular-backup-removed-by-the-boot-backup.md`, status ready, severity medium, priority high), confirmed twice on the Android 16 emulator on 2026-11-21 (its Evidence, Record 21 ruling 62; `sdlc/artifacts/build/research/2026-11-21-background-backup-and-boot-observation.md`, Run 1). The mechanism is its Description items 1 to 6; the legacy comparison item 7.
- **BUG-026** (`sdlc/artifacts/stories/BUG-026-regular-schedule-never-still-schedules-a-backup.md`, status ready, severity low, priority medium). The mechanism is its Description items 1 to 6.

The contract the story is built against is **CNTR-MODERNIZATION-004 v4**, written by change record **CR-017** (`sdlc/artifacts/build/change-records/CR-017-cntr-004-scheduler-never-boot-example-and-receiver.md`, status implemented, with Amendments 1 and 2; one record for all scheduler changes by ruling 67, Record 22). The contract states the target and lists this story's gap as item K-1 of "Code not yet at v4". Closing this story is what lets the orchestrator apply close-out edit X-1, which deletes K-1. The rulings are binding and are not re-decided here. The criteria below carry them.

**What the fix is, in one paragraph.** `WorkManagerScheduler` registers the regular backup through one private path used by both `scheduleRegular()` and `scheduleBootup()`. That path builds today's periodic `REGULAR` request unchanged and adds the 60 s boot delay as the initial delay when called from the boot path. It enqueues the request with `ExistingPeriodicWorkPolicy.UPDATE`. If the returned operation fails (the expected cause is a one-time entry under `REGULAR` that an earlier build left), it logs a warning, re-checks the gates and re-registers the same request with `ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE`. The boot path no longer builds a one-time request. A non-positive regular interval ("Never" is −1; 0 too) registers nothing, cancels `REGULAR` and returns `null`, on both methods. The encryption-degraded check stays first and changes nothing. Comments and the port's javadoc are corrected. This is the shape of BUG-027 Suggested Approach ("Fix", steps 1 to 4) and BUG-026 Suggested Approach ("Fix"). The plan may refine the shape, but not the criteria.

**The product owner's questions PO-1 and PO-2 (BUG-027 Notes) are answered "no" for this story** (ruling 68, Record 22; Record 20 `:2479`; CR-017 Scope Concern 2 ruling):
- **PO-1 "no":** with the regular schedule at "Never", a restart registers nothing and cancels a regular backup that an earlier build registered (AC-9, AC-10, device step (iii)).
- **PO-2 "no":** with a positive interval, a restart registers the periodic regular backup, or keeps the one already registered, and starts no separate boot backup (AC-1, AC-2, AC-4, device step (i)).

The questions stay listed for the owner. **If the owner answers "yes" before this story's build starts,** the story goes back to the orchestrator for revision; the developer does not change criteria on its own. The contract sentences that change then are listed in CR-017, Amendment 1, section "S-7: the completed 'If the owner later answers yes' paragraph". They are not restated here. The story's criteria that would change are:
- **PO-1 "yes":** AC-9, the `scheduleBootup()` half of AC-10, the restart part of device step (iii), and IC-2's "no new unique name".
- **PO-2 "yes":** AC-2, AC-4 (its "no other unique name holds a `REGULAR`-tagged item", by Amendment 2, DS-1), AC-5's "starts no separate boot backup", device step (i)'s "no one-time boot entry", and IC-2. A guard criterion is added for the overlap guard that the planner would design.

If the answer comes after the build, it is a new story with its own change record, contract v5.

**Not in this story.** Each item below is ruled or recorded elsewhere, and none is an acceptance criterion.
- **The SMS-received receiver** (`SmsBroadcastReceiver`, its manifest entry, its toggle in `App`, its test, and its name in the port javadoc at `BackupScheduler.java:32-33`) is removed by story U-077 (ruling 56). This story leaves those lines alone (AC-15).
- **The help text and README on the incoming delay** belong to story U-078 (BUG-025, ruling 54: text only).
- **BUG-025's content-trigger period** stays unchanged: no change to `scheduleContentTrigger()` or to the 15-minute trigger period.
- **The cancellation reporting of the workers** is story U-074 (BUG-024).
- **The foreground change of the backup worker** is story U-075 (BUG-029). This story depends on it (Technical Context, placement) and edits no worker.
- **Any library raise** is story U-073, including WorkManager 2.9.1 to 2.12.0. This story is built on 2.9.1.
- **Product owner item PO-D**, "when auto backup is switched off, should a backup that is already waiting be cancelled" (decision appendix `:2354`), is out of scope. **Checked against the v4 text: v4 does not require it.**
  - Rule 4 requires `cancelAll()` at boot when auto backup is off. The code already does that (`WorkManagerScheduler.kt:267-271` at commit `74b31759`), and this story keeps it.
  - `cancelAll()` stays "cancelRegular() + cancel content-trigger" (contract `scheduleRegular`/`cancelAll` block, `:106-107`). It does not cancel a pending `INCOMING`.
  - No v4 clause requires `scheduleRegular()` to cancel anything when auto backup is off. Rule 4a's cancel applies only to a non-positive interval.
  - PO-D stays held for the owner.
- **The README.** Its sentences about the regular schedule and "Run at startup" (`README.md:238`, `:304-305` at `74b31759`) become true with this story. They are read by QA and not edited (AC-16 (h); CR-017 Evidence E-3, last table).
- **The contract housekeeping** of CR-017 Scope Concerns 7 and 8 (ruled: a separate record).
- **Failed operations of the other six enqueue calls.** Logging them is a possible hardening story (BUG-027 Notes, "Observations outside this bug").
- **A cross-run guard between backup types** (same Notes).

## Acceptance Criteria

**Terms.**
- **Lines.** Every line reference in this story is "at commit `74b31759`", read with `git show 74b31759:<path>`. `git diff --stat fd70325a 74b31759 -- app/` is empty (checked when this story was written), so CR-017's and DES-MODERNIZATION-005's code lines (cited at `fd70325a`) hold at `74b31759`.
- **The old scheduler.** "The scheduler code at commit `fd70325a`" in DES-MODERNIZATION-005 rows D-23 and D-24 is the same file as `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt` at `74b31759`. That file is called **the old scheduler** below.
- **Start.** Start is the commit that closes this story's predecessor in sprint 025. Today that is U-072. If a sprint revision puts another story between U-072 and this one, Start is that story's closing commit. The plan re-anchors every line at Start (obligation P-5).
- **Red / green at Start.** A test that proves changed behaviour is written first and run against the unchanged production code at Start. It must fail for the reason the criterion names (an assertion, not a compile or fixture error), and the plan records the output. Start condition S-3 makes the production scheduler at Start equal to the old scheduler, so **red at Start means red on the old scheduler** (D-23, D-24: "fails on the scheduler code at `fd70325a`, and the story records that run").
- **Guard.** A test of behaviour that does not change passes at Start by design. Guards are labelled as such and are not gate evidence for the change.
- **Entry** is an item of `WorkManager.getWorkInfosForUniqueWork("REGULAR").get()`.
  - **Periodic** means `WorkInfo.getPeriodicityInfo() != null`, and **one-time** means it is `null`.
  - **Non-terminal** means `ENQUEUED`, `RUNNING` or `BLOCKED`.
  - The accessors `getId`, `getGeneration`, `getPeriodicityInfo`, `getInitialDelayMillis` and `getConstraints` exist in 2.9.1 (BUG-027 Suggested Approach, "Tests", read in bytecode).
- **Seeded entries** are created inside the test directly through `WorkManager`, not through the scheduler, so the tests do not depend on the old code (BUG-027 Suggested Approach, "Tests"; D-23 (3)).
  - **(S1) The one-time seed** is today's boot request: a one-time `BackupWorker` with the regular network constraint, `EXPONENTIAL` 30 s backoff, a 60 s initial delay and tag `REGULAR`, enqueued by `enqueueUniqueWork("REGULAR", ExistingWorkPolicy.REPLACE, …)`. That is what `WorkManagerScheduler.kt:273-281` enqueues. Its states are `ENQUEUED` as enqueued, `CANCELLED` after `cancelUniqueWork("REGULAR")`, and `SUCCEEDED` only if the plan can drive it there (obligation P-3).
  - **(S2) The "Never" seed** is a periodic `BackupWorker` with a 900 s interval and tag `REGULAR`, enqueued under `REGULAR` with `UPDATE`. That is what `:180-197` registers for −1.
- **Harness.** The existing harness of `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt:42-81`:
  - Robolectric at `sdk = [29]` (`:43`);
  - `WorkManagerTestInitHelper.initializeTestWorkManager` with a `Configuration` whose worker executor is `Executors.newSingleThreadExecutor()` and whose factory is `BackupWorker.TestableBackupWorkerFactory()` (`:62-67`);
  - mocked `Preferences`, `DataTypePreferences` and `SecretStore`, with default stubs at `:70-78`: auto backup on, incoming 60, regular 3600, Wi-Fi only off, call-log trigger off, not degraded.

  New tests are added as new methods to that file or as a new test class in the same package with the same set-up (the plan decides). No existing method is edited. Test names below are working names; the plan fixes them.
- **The log prefixes** below are binding. The plan fixes the rest of each text. Every line is written with tag `SMSBackup+`.
  - **The "Never" line**, at `Log.d`: `WorkManagerScheduler.scheduleRegular: skipped (regular schedule is Never` and `WorkManagerScheduler.scheduleBootup: skipped (regular schedule is Never`.
  - **The boot line**, at `Log.d`: `WorkManagerScheduler.scheduleBootup: registered REGULAR`.
  - **The fallback warning**, at `Log.w`: it contains `UPDATE failed` and `CANCEL_AND_REENQUEUE`.

### A. The boot path keeps the regular backup (contract Rule 4; INV-1 realization; DES-MODERNIZATION-005 INV-1 row, D-9 as amended by D-23)

- [ ] AC-1 (a restart keeps a registered regular backup as it is; D-23 (1), BUG-027 AC-1 and AC-5):
  - Given: auto backup on, not degraded, `regularTimeoutSecs` 3600, and `scheduler.scheduleRegular()` called, which leaves one periodic `ENQUEUED` entry. Its `id` and `generation` are recorded.
  - When: `scheduler.scheduleBootup()` is called.
  - Then: there is exactly one entry. It is periodic and `ENQUEUED`, its `periodicityInfo.repeatIntervalMillis` is 3 600 000, its `id` is the recorded one, and its `generation` is the recorded one plus exactly 1. That shows an in-place `UPDATE`, not a cancel and re-enqueue.
  - Checked by test `boot_afterRegisteredRegular_keepsSamePeriodicEntry`. **Red at Start:** today the entry is one-time with a new `id`, because `REPLACE` deletes the periodic work (`WorkManagerScheduler.kt:280-281`).
  - Whether `UPDATE` raises `generation` by exactly one when the request is unchanged except for the initial delay is settled by obligation P-1 (a) before the build.
- [ ] AC-2 (a restart with nothing registered registers the regular backup; D-23 (2), BUG-027 AC-2):
  - Given: no entry, for each of `isWifiOnly` `true` and `false`.
  - When: `scheduler.scheduleBootup()` is called.
  - Then: there is exactly one entry. It is periodic and `ENQUEUED`, with `repeatIntervalMillis` 3 600 000, `initialDelayMillis` 60 000, and the tag `REGULAR`. Its `constraints.requiredNetworkType` is `UNMETERED` when Wi-Fi only is on and `CONNECTED` otherwise (INV-2). It has no charging constraint, and its backoff is `EXPONENTIAL` 30 s (INV-3, read from the `WorkSpec` as `inv3_backoffPolicy_exponential_regularJobEnqueued`, `:283`, does).
  - Checked by tests `boot_nothingRegistered_registersPeriodic_wifiOnly` and `…_anyNetwork`. **Red at Start:** today the entry is one-time.
  - This is the self-healing case: a phone already affected by BUG-027 gets its regular backup back at its next restart.
- [ ] AC-3 (a one-time entry left by an earlier build does not block registration; D-23 (3), BUG-027 AC-3, and BUG-027 Suggested Approach, "Upgrade"):
  - Given: the one-time seed (S1) under `REGULAR`, in each of these states:
    - (a) `ENQUEUED`;
    - (b) `CANCELLED`, after `scheduler.cancelAll()`, as `App.rescheduleJobs()` does (`App.java:373-374`);
    - (c) `SUCCEEDED`, if obligation P-3 finds the harness can drive it there. Otherwise (c) is recorded "not tested: <reason>" in Implementation Notes and is not reported as passed.
  - When: separately for each state, `scheduler.scheduleRegular()` is called, and in a fresh test with the same seed, `scheduler.scheduleBootup()`.
  - Then: there is exactly one entry, periodic and `ENQUEUED`, with the configured interval. No one-time entry remains under `REGULAR`. The fallback warning is written exactly once (`ShadowLog`).
  - Checked by tests `regular_overOneTimeSeed_<state>_registersPeriodic` and `boot_overOneTimeSeed_<state>_registersPeriodic`. **Red at Start:** today `scheduleRegular()`'s `UPDATE` fails silently on the one-time entry, which stays (BUG-027 Description item 5), and `scheduleBootup()` leaves a one-time entry.
  - These tests carry K-1's third clause ("the next `scheduleRegular()` then fails silently on that entry").
- [ ] AC-4 (single flight, one work kind per name; D-23 (4), BUG-027 AC-4):
  - Given: the default stubs, with `regularTimeoutSecs` 3600.
  - When: each of these sequences runs in its own test:
    - `scheduleRegular()`, `scheduleBootup()`;
    - `scheduleBootup()`, `scheduleRegular()`;
    - `scheduleRegular()`, `scheduleBootup()`, `cancelAll()`, `scheduleRegular()`.
  - Then:
    - `WorkManager.getWorkInfosByTag("REGULAR").get()` holds exactly one non-terminal item;
    - that item is periodic, and its `id` is the `id` of the one non-terminal entry under `REGULAR`;
    - no non-terminal item that carries the tag `REGULAR` exists under any other unique name: every `REGULAR`-tagged `id` is found among the entries of `REGULAR`.
  - Checked by tests `inv1_regularAndBoot_sequence1..3_oneNonTerminalPeriodic`. **Red at Start:**
    - sequence 1 leaves a one-time item;
    - sequence 2 leaves the one-time boot item, because `UPDATE` fails;
    - sequence 3 leaves no non-terminal item, only the cancelled one-time item.

    This is why D-23 requires "periodic" and not just "one".
- [ ] AC-5 (a restart neither cancels nor resets a registered regular backup, and starts no separate boot backup; contract Rule 4):
  - The boot path enqueues with `UPDATE` first. The `CANCEL_AND_REENQUEUE` fallback is reached only from the failure of that operation, never as the first call.
  - No code path in `scheduleBootup()` builds a `OneTimeWorkRequest` or calls `enqueueUniqueWork`.
  - No unique name other than `REGULAR` is enqueued by `scheduleBootup()`.
  - Checked by:
    - AC-1 (same `id`, `generation` + 1);
    - AC-4 (no other unique name);
    - mutation M-3 (AC-20);
    - reading the diff of `scheduleBootup()` and the private registration path: `git grep -n "OneTimeWorkRequest" <end> -- app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt` lists no line inside `scheduleBootup()` or that path.
  - A running regular backup cannot be held `RUNNING` by this harness. That it is not cancelled rests on `UPDATE` not cancelling a running worker (BUG-027 Description item 5, read in 2.12.0 at `WorkerUpdater.kt:58`, `:105`), which obligation P-1 (a) confirms for 2.9.1. It is recorded so in Implementation Notes and not claimed as tested.
- [ ] AC-6 (the fallback re-checks the gates before it enqueues; BUG-027 Suggested Approach, "Risks"):
  - Given: the one-time seed (S1) `ENQUEUED`, so that `UPDATE` fails.
  - When: one of the following becomes true by the time the fallback runs, while it was false when `scheduleRegular()` passed its gates:
    - (a) auto backup is off;
    - (b) encryption is degraded;
    - (c) `regularTimeoutSecs` is −1.
  - Then: the fallback enqueues nothing. No periodic entry exists under `REGULAR`, and nothing else is cancelled by the fallback.
  - How the test makes the gate change between the two reads (for example consecutive Mockito stubs) is the plan's choice. If the plan shows the harness runs the listener before any second read can differ, the case is proved by reading the code plus mutation M-7, and recorded so.
  - Checked by test(s) `fallback_gateChangedBeforeFallback_enqueuesNothing` (or the recorded reading). **Red at Start** does not apply: the old scheduler has no fallback. The test is red on the build with M-7 applied.
- [ ] AC-7 (the library facts the fix rests on are established before the build, not assumed):
  - The plan records obligations P-1 to P-4 (Technical Context) with their sources, from WorkManager **2.9.1**, the version in use until story U-073. The bug and CR-017 mark each as "not verified" or "read in 2.12.0 only" (BUG-027 Not verified 3, 5, 7, 8, 10; CR-017 Not verified, second and sixth bullets).
  - Each obligation has a stop condition. When one is met, the developer stops before the first production edit and reports. The story does not continue on an assumption.
  - Checked by the plan's recorded findings, copied to Implementation Notes.

### B. The regular schedule "Never" (contract Rule 4a; DES-MODERNIZATION-005 "Never" row, D-11 as amended by D-24)

- [ ] AC-8 ("Never" registers nothing and removes an existing regular backup; D-24 (1), BUG-026 AC-1 and AC-2):
  - Given: auto backup on and not degraded.
  - Then each of these holds:
    - (a) with `regularTimeoutSecs` −1, `scheduleRegular()` returns `null` and leaves no non-terminal entry;
    - (b) the same holds with 0;
    - (c) after `scheduleRegular()` with 3600, a second `scheduleRegular()` with −1 returns `null` and leaves no non-terminal entry, with no `cancelAll()` in between;
    - (d) the "Never" line is written in (a) to (c).
  - Checked by tests `never_minusOne_registersNothing`, `never_zero_registersNothing` and `never_afterRegistered_cancelsRegular`. **Red at Start:** today −1 and 0 give a 900 s periodic entry and a non-null job (`WorkManagerScheduler.kt:180`, `:201`).
- [ ] AC-9 (a restart under "Never" registers nothing; D-24 (2), BUG-027 AC-11, PO-1 "no"):
  - Then each of these holds:
    - (a) with `regularTimeoutSecs` −1, `scheduleBootup()` returns `null` and leaves no non-terminal entry;
    - (b) the same holds with 0;
    - (c) after `scheduleRegular()` with 3600, `scheduleBootup()` with −1 returns `null` and leaves no non-terminal entry;
    - (d) in (a) to (c), `getWorkInfosByTag("REGULAR")` holds no non-terminal item under any unique name, so no separate boot backup is started;
    - (e) the boot's "Never" line is written.
  - Checked by tests `boot_never_minusOne_registersNothing`, `boot_never_zero_registersNothing` and `boot_never_afterRegistered_cancelsRegular`. **Red at Start:** today the boot path enqueues a one-time entry whatever the interval.
- [ ] AC-10 (the upgrade case: a 15-minute regular backup an earlier build registered under "Never" is removed; D-24 (3), contract Rule 4 as amended by C-25, CR-017 Amendment 1 S-1):
  - Given: the "Never" seed (S2) under `REGULAR` and `regularTimeoutSecs` −1.
  - When: separately, `scheduleBootup()` is called (the next restart) and `scheduleRegular()` is called (the next settings event).
  - Then: each leaves no non-terminal entry and returns `null`.
  - Checked by tests `upgrade_neverSeed_boot_cancels` and `upgrade_neverSeed_regular_cancels`. **Red at Start:** today the boot path replaces the seed with a one-time entry, and `scheduleRegular()` updates it to 900 s.
- [ ] AC-11 (guards: positive intervals are unchanged; D-24 "Guards of unchanged behaviour", BUG-026 AC-3 and AC-4). These tests are **guards. They pass at Start by design and are not gate evidence.**
  - With `regularTimeoutSecs` 60, 899 and 900, `scheduleRegular()` registers a periodic entry with `repeatIntervalMillis` 900 000.
  - Each offered value, 1800, 3600, 7200, 21600 and 86400 (`app/src/main/res/values/arrays.xml:83-90`), gives its own value in milliseconds.
  - The tests pin the behaviour, not the mechanism. WorkManager clamps below 15 minutes too (BUG-026 Description item 5), so they still pass if the app's `maxOf` is removed. The test's comment says so.
  - Checked by tests `regular_belowMinimum_raisedTo900s` (60, 899, 900) and `regular_offeredValues_useOwnInterval` (parameterised or five small tests; the plan decides), green at Start and after.
- [ ] AC-12 (guard: the incoming path is unaffected by "Never"; BUG-026 AC-5):
  - With `regularTimeoutSecs` −1 and `incomingTimeoutSecs` 60, `scheduleContentTrigger()` registers `CONTENT_TRIGGER`, and `scheduleIncoming()` enqueues `INCOMING` with `initialDelayMillis` 60 000.
  - Checked by test `never_incomingPathUnchanged`. **Guard:** green at Start and after.

### C. Unchanged behaviour (guards) and the existing tests

- [ ] AC-13 (guards: the gates and the content trigger; BUG-027 AC-6 and AC-7; contract Rules 4 and 4a, "The encryption-degraded check comes first and changes nothing"). All of the following are **guards**, green at Start and after:
  - (a) after `scheduleRegular()` with 3600, a degraded `scheduleBootup()` returns `null` and leaves the entry with the same `id`, `state` and `generation`;
  - (b) the same with `regularTimeoutSecs` −1 at the time of the degraded call. The degraded check runs before the "Never" rule, so nothing is cancelled;
  - (c) after `scheduleContentTrigger()`, `scheduleBootup()` with 3600 leaves the `CONTENT_TRIGGER` entry with the same `id` and `state`;
  - (d) auto backup off: `scheduleBootup()` returns `null` and leaves no non-terminal entry under `REGULAR` or `CONTENT_TRIGGER`.
  - Checked by tests `boot_degraded_afterRegistered_leavesItUnchanged`, `boot_degraded_never_cancelsNothing`, `boot_leavesContentTriggerUnchanged` and `boot_autoBackupOff_cancelsAll`.
- [ ] AC-14 (the existing tests pass with no edit; CR-017, "What the story … must cite" item 3). These tests pass at the end, and `git diff <start> -- app/src/test` shows only added lines in `WorkManagerSchedulerTest.kt` (new methods, new imports) and new files:
  - the three existing boot tests: `ac5_scheduleBootup_withAutoBackupEnabled_isEnqueued` (`WorkManagerSchedulerTest.kt:428`), `ac5_scheduleBootup_autoBackupDisabled_returnsNull` (`:445`) and `se002_scheduleBootup_encryptionDegraded_returnsNullNoWork` (`:569`);
  - every existing `scheduleRegular()` test (`:99`, `:163`, `:182`, `:220`, `:283`, `:534`);
  - the `inv4_*` tests (`:308`, `:332`, `:356`, `:380`), which are the INV-4 evidence of the DES-MODERNIZATION-005 INV-4 row;
  - `BootReceiverTest.shouldScheduleBootupBackupAfterBootup` (`receiver/BootReceiverTest.java:33`);
  - `BackupTriggerWorkerTest` (all methods);
  - every other existing test.

  Checked by the gate (AC-18) and that `git diff`.

### D. Scope, comments and contract conformance

- [ ] AC-15 (only the scheduler and the port's javadoc change; BUG-027 AC-7, BUG-026 AC-5, CR-017 citation item 6):
  - `git diff --name-only <start> -- app/src/main` lists exactly:
    - `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt`
    - `app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java`
  - In `WorkManagerScheduler.kt`, every changed line is in one of these places:
    - `scheduleRegular()` and its KDoc;
    - `scheduleBootup()` and its KDoc;
    - the new private registration path (and its fallback listener);
    - the companion object's `PERIODIC_MIN_INTERVAL_SECS` comment (`:112-115`);
    - the class comment's INV-1 and unique-name lines (`:40-42`, `:67-69`);
    - the imports.
  - These are unchanged: `scheduleIncoming`, `scheduleContentTrigger`, `scheduleImmediate`, `scheduleManual`, `scheduleRestore`, `cancelAll`, `cancelRegular`, `cancel`, the observation methods and `networkConstraints`.
  - In `BackupScheduler.java`, every changed line is a comment line (javadoc) inside the places AC-16 names. The "All call sites" sentence (`:32-33`) is not touched; it is story U-077's.
  - `git diff <start> --` each of these files is empty:
    - `App.java`, `receiver/BootReceiver.java`, `receiver/PackageReplacedReceiver.java`, `receiver/SmsBroadcastReceiver.java` (if it still exists at Start), `worker/BackupTriggerWorker.kt`, `service/BackupWorker.kt`, `AndroidManifest.xml`;
    - `build.gradle`, `app/build.gradle`, `settings.gradle`, `gradle/`;
    - `sdlc/artifacts/design`, `sdlc/artifacts/requirements`.
  - Checked by those commands at the end, with their output in the evidence.
- [ ] AC-16 (the code's comments and the port's javadoc say what v4 says; BUG-026 AC-6, BUG-027 AC-8, CR-017 citation item 5 as amended by Amendment 1, Scope Concern 12 ruling). These edits are comment-only. Each place below says the stated thing at the end:
  - (a) `WorkManagerScheduler.kt:112-115` (`PERIODIC_MIN_INTERVAL_SECS`): the 15-minute floor applies to positive values. A non-positive value means "Never" and registers nothing.
  - (b) `:160-169` (`scheduleRegular()` KDoc):
    - `UPDATE`, with `CANCEL_AND_REENQUEUE` only when `UPDATE` fails;
    - a non-positive interval registers nothing, cancels `REGULAR` and returns `null`;
    - a positive interval below 15 minutes is raised to 15 minutes;
    - `null` when auto backup is off or encryption is degraded.
  - (c) `:252-261` (`scheduleBootup()` KDoc):
    - it registers the regular periodic backup as `scheduleRegular()` does, with the 60 s boot delay as the first-run delay of a new registration;
    - `UPDATE` keeps an existing registration;
    - it never enqueues a one-time `REGULAR` work and starts no separate boot backup;
    - "Never" registers nothing;
    - the legacy source is `BackupJobs.java:86-97`, both branches.
  - (d) The class comment's INV-1 lines (`:40-42`): `REPLACE` for one-off work; `UPDATE` for periodic work; `REGULAR`'s fallback; the boot path never enqueues a one-off under `REGULAR`.
  - (e) The class comment's unique-name lines (`:67-69`): the boot path has no unique name of its own; it registers the `REGULAR` periodic work.
  - (f) `BackupScheduler.java`:
    - `:36`, "Contract: CNTR-MODERNIZATION-004 v2", names v4;
    - `:58-68` (`scheduleRegular()` javadoc) states the "Never" rule, `UPDATE`, and `null` for "Never";
    - `:70-72` (`scheduleContentTrigger()` javadoc) says the call-log URI is added "when call-log backup is enabled and `isCallLogBackupAfterCallEnabled()`" (CR-017 Amendment 1, M-2's code hand-over; the code's condition is `WorkManagerScheduler.kt:227-228`);
    - `:83-92` (`scheduleBootup()` javadoc) says what (c) says, in port terms (no WorkManager type).
  - (g) No sentence in these places still says the boot path schedules a one-off, or that `scheduleRegular()` uses `REPLACE`.
  - (h) QA reads `README.md:238` ("Run at startup - used to enable automatic backups after reboot") and `:304-305` ("2 hours here means that the device will try to backup all messages every 2 hours") against the fixed behaviour and records that both are true. Neither file is edited.
  - Checked by reading each place against the code and against the contract's `scheduleRegular()` and `scheduleBootup()` blocks.
- [ ] AC-17 (every clause of K-1 is met, so that X-1 can be applied). The plan's conformance table and QA's record map each item to the criterion that proves it. The story is not closed while any row is unmet. Item K-1 of the contract's "Code not yet at v4" names three code facts and lists the contract texts they affect. Each is met as follows:

  | K-1 item (contract "Code not yet at v4") | Met by |
  |---|---|
  | "`scheduleRegular()` raises a non-positive interval to 15 minutes and registers a periodic backup (BUG-026)" | AC-8, AC-10 |
  | "`scheduleBootup()` enqueues a one-time `BackupWorker` under `REGULAR` with `REPLACE`, which deletes the periodic regular backup at every restart" | AC-1, AC-2, AC-4, AC-5; device step (i) |
  | "the next `scheduleRegular()` then fails silently on that entry (BUG-027)" | AC-3; device step (ii) |
  | Affected: the `scheduleRegular()` block | AC-3, AC-8, AC-11; AC-16 (b), (f) |
  | Affected: the `scheduleBootup()` block | AC-1, AC-2, AC-3, AC-5, AC-9, AC-13; AC-16 (c), (f) |
  | Affected: the boot-path bullet of the `BackupType` notes ("No `BOOT` unique name is used") | AC-4, AC-9 (d); IC-2 |
  | Affected: the boot comment of `initialDelaySecs` (60 s as the first-run delay of a new `REGULAR` registration) | AC-2 (60 000 ms on a new registration); AC-1 (an existing one keeps its schedule) |
  | Affected: the `REGULAR` fallback in the `ExistingPolicy` note | AC-3, AC-5, AC-6 |
  | Affected: the `REGULAR` row of the unique-name table (periodic, interval `max(…, 15 min)`, network per Wi-Fi only, `EXPONENTIAL` 30 s, first-run delay 60 s at a new boot registration, `UPDATE` with fallback, "nothing is registered, and an existing entry is cancelled, when `getRegularTimeoutSecs() <= 0`") | AC-2, AC-3, AC-8, AC-9, AC-10, AC-11 |
  | Affected: the INV-1 realization (`REGULAR` uses `UPDATE`; fallback only on failure; one work kind per name) | AC-1, AC-3, AC-4, AC-5 |
  | Affected: Rule 4 (as amended by C-25) | AC-1 to AC-6, AC-9, AC-10, AC-13 |
  | Affected: Rule 4a | AC-8 to AC-11, AC-13 (b) |
  | Affected: the regular example (`UPDATE`, fallback, "a value <= 0 registers nothing") | AC-2, AC-3, AC-8 |

  The contract and DES-MODERNIZATION-005 test obligations for this story map one to one:

  | Obligation | Criterion |
  |---|---|
  | D-23 (1): same `id`, interval 3 600 000 ms, `generation` + 1 | AC-1 |
  | D-23 (2): nothing registered; 60 000 ms; regular network for both Wi-Fi-only values; tag `REGULAR` | AC-2 |
  | D-23 (3): one-time seed `ENQUEUED` and `CANCELLED`, both methods (`SUCCEEDED` if the harness can drive it) | AC-3 |
  | D-23 (4): the three sequences; exactly one non-terminal, periodic, under `REGULAR`; no other name | AC-4 |
  | D-23: "each of (1) to (4) fails on the scheduler code at commit `fd70325a`, and the story records that run" | Red at Start for AC-1 to AC-4 (Terms; Start condition S-3) |
  | D-24 (1): −1 and 0; after 3600 without `cancelAll()` | AC-8 |
  | D-24 (2): `scheduleBootup()` with −1 or 0 | AC-9 |
  | D-24 (3): the upgrade case | AC-10 |
  | D-24: (1) to (3) fail at `fd70325a`; the story records the run | Red at Start for AC-8 to AC-10 |
  | D-24: interval tests are guards, not gate evidence | AC-11 |
  | Citation item 3: listed tests kept green with no edit | AC-14 |
  | INV-4 row: `inv4_*` and `BackupTriggerWorkerTest` remain the INV-4 evidence | AC-14; AC-12 and AC-13 (c) as guards |
  | Breaking-change policy: the boot-path break "MUST be gated by the INV-1..4 tests" | AC-1 to AC-4 in the gate (AC-18); mutations (AC-20) |

  Checked by QA against the contract text at Start. When every row is met, the orchestrator applies X-1 (CR-017, "Close-out edits"). Applying X-1 is the orchestrator's step, not the developer's.

### E. Gate, count and failure injections

- [ ] AC-18 (gate, coverage):
  - The gate runs on JDK 25 as declared by `gradle/gradle-daemon-jvm.properties`, in a shell without `MSYS_NO_PATHCONV`. The command is `./gradlew --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease :app:jacocoTestReport`.
  - It runs at Start (before the first edit) and at the end (no failure injection present). Both exit 0 with 0 test failures, and the end run's skipped count is not higher than Start's.
  - No JaCoCo floor is lowered (`git diff <start> -- app/build.gradle` is empty), and no test is `@Ignore`d.
  - The scheduler package's LINE ratio is read from the end report against its floor (Rule 12, `com.zegoggles.smssync.scheduler*`, 0.60, measured 62.2 % at its last measurement, `app/build.gradle:538-545`). A fall below the floor is a stop and a report; the floor is not changed.
  - A full gate takes about ten minutes since story U-070 and is moved to the background by the harness. The plan handles that as the plans of U-071 and U-075 do:
    - every Gradle call writes its own `exit=` line into its log;
    - the developer waits for the completion notice, never polls and never starts a second Gradle;
    - a log without `exit=` is a cut-off run and a stop.
- [ ] AC-19 (test counts; REQ-MODERNIZATION-015 AC-13 as amended by CR-016 Amendment 1):
  - The count is `git grep -h "@Test" <commit> -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`.
  - It is recorded at Start with the Start commit's id, and at the end with the story's last commit in place of `<commit>`. Before the commit, it may be taken with the working-tree form plus `--untracked`, stated as such.
  - Tests that come from changes other than this story's are listed by name and not counted. The difference equals this story's new tests, each listed by name.
  - No fixed baseline number is stated in this story.
- [ ] AC-20 (each new test detects what it names):
  - Each failure injection is applied alone and recorded in `mutation-log.md` in the story's evidence folder with the protocol of U-074 AC-15:
    - each changed production file is saved and its `sha256sum` recorded first;
    - a write-ahead line;
    - one targeted test run;
    - a restore from the saved copy, with the hash shown equal to the saved one;
    - `git status --porcelain -- app/src/main` lists only this story's files.
  - A mutation is never present at a gate, a review or a commit. A mutation that does not compile is logged invalid, with its compiling replacement.

  | Id | Injection (one change) | Tests that must fail (at least) |
  |----|------------------------|---------------------------------|
  | M-1 | The boot path enqueues today's one-time `REPLACE` request again (BUG-027 AC-10 (i)) | AC-1, AC-2, AC-4 |
  | M-2 | The fallback is removed (the failed `UPDATE` is ignored again) (BUG-027 AC-10 (ii)) | AC-3 |
  | M-3 | The boot path's first call uses `CANCEL_AND_REENQUEUE` instead of `UPDATE` (BUG-027 AC-10 (iii)) | AC-1 (its `id` assertion) |
  | M-4 | The "Never" early return is removed from the shared path (BUG-026 AC-7 (i)) | AC-8, AC-9, AC-10 |
  | M-5 | `<= 0` becomes `< 0` (BUG-026 AC-7 (ii)) | the 0 tests of AC-8 and AC-9 |
  | M-6 | The "Never" branch returns `null` without cancelling `REGULAR` | AC-8 (c), AC-9 (c), AC-10 |
  | M-7 | The fallback skips its gate re-check | AC-6 (if tested; otherwise recorded as the reading's evidence) |

  - Before running, the plan derives the complete failing set for each row. A row is met when the observed set equals the derived set.
  - The guards (AC-11 to AC-13) are expected to stay green under M-1 to M-7, except where the derived set names them.
  - Checked by `mutation-log.md`.

### F. On the device (run by the orchestrator only)

- [ ] AC-21 (device, emulator `Pixel_9_Pro`, Android 16; no other device is started or touched).
  - **Set-up and rules:**
    - The orchestrator installs debug builds over the current one with data kept (`adb install -r`).
    - Nothing but the app's own state is changed: its settings, its installed build, opening and leaving the app, restarting the emulator. No device setting, permission, battery-optimisation exemption or standby-bucket command is used, and no step needs a system setting.
    - The regular schedule "Never" is an app setting. Choosing it through the app's settings screen and putting the previous value back afterwards is allowed on this emulator by Record 20 ("Allow all of them", PO-G). Step (iii) does this and says so.
    - Auto backup is on with a mail account configured, as left by the earlier device checks.
  - **What the orchestrator reads:**
    - `adb -s <serial> shell dumpsys jobscheduler` for the app's jobs. The orchestrator records the lines that show whether each job is periodic or one-time; their exact wording is not fixed by this story.
    - WorkManager's diagnostics: `adb -s <serial> shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS -p com.zegoggles.smssync`, then `adb -s <serial> logcat -d` around "Enqueued work", for the unique name and class of each entry.
    - Optionally, for the definitive periodic/one-time distinction, WorkManager's database copied out with `run-as` and queried **outside the repository** as BUG-027 Steps to Reproduce B step 3 shows (`interval_duration > 0` means periodic).

    Whether the 2.9.1 diagnostics output shows periodicity is not verified. Periodicity is read from the scheduler dump or the database query.
  - **Steps, in this order:**
    - **(i) A cold restart keeps the regular backup.**
      - With the fixed debug build installed and the regular schedule at its default, open the app once, then leave it.
      - Cold-start the emulator without saving a snapshot (`-no-snapshot-save`), wait until `getprop sys.boot_completed` prints `1`, and do not open the app.
      - Met when the app's log shows `bootup` (`BootReceiver.java:32`) and the boot line (Terms), and, from at least 2 minutes after `BOOT_COMPLETED` (longer than the 60 s boot delay), all of the following hold:
        - the scheduler dump shows a periodic job for the regular backup;
        - the diagnostics list one `BackupWorker` entry with unique name `REGULAR` and the `CONTENT_TRIGGER` entry;
        - no one-time job with a 60 s minimum latency is held for the boot. An `INCOMING` one-off that was pending before the restart is allowed and named if seen (CR-017 Scope Concern 10).
    - **(ii) A phone that an earlier build left without a regular backup gets it back.**
      - Install the debug build of Start, which has the old scheduler, and cold-restart as in (i). Confirm the BUG-027 state: only a one-time `REGULAR` entry and no periodic job, as in the observation's Run 1.
      - Without opening the app, install the fixed debug build with `adb install -r`. That delivers `MY_PACKAGE_REPLACED`, so `App.rescheduleJobs()` runs (`PackageReplacedReceiver.java:20-29`, `App.java:373-385`).
      - Met when the log shows the fallback warning, or a plain registration if the one-time entry had already been pruned, and the scheduler dump and diagnostics then show one periodic `REGULAR` entry and no one-time `REGULAR` entry.
      - If the package-replaced event does not reach `rescheduleJobs()` (`tryEmitEvent` returned `false`, `PackageReplacedReceiver.java:26-28`), the orchestrator records that line and cold-restarts once more. The fixed boot path must then register the periodic entry (AC-3's boot case).
    - **(iii) "Never" schedules nothing, and a restart under "Never" registers nothing.** This step changes an app setting and restores it (Record 20).
      - Open the app. In the auto-backup settings, choose "Never" for the regular schedule and note the value it replaced.
      - Met when the log shows the "Never" line of `scheduleRegular()` and the scheduler dump shows no periodic job for the regular backup (the content trigger stays).
      - Then cold-restart as in (i) without opening the app. Met when the log shows the boot's "Never" line and no `REGULAR` entry is non-terminal.
      - Open the app and set the regular schedule back to the noted value. Met when the log shows a registration with that interval and the dump shows the periodic job again.
      - The step is not complete until the setting is restored. If it cannot be restored, the orchestrator stops and tells the owner.
    - **(iv) Optional: a new registration's first run starts off screen and succeeds** (it shows that U-075 and this story work together). After (ii), the new registration's first run starts with the app off screen. It is met when that run, the work tagged `REGULAR`, logs "BackupWorker.doWork: starting backup, type=REGULAR" and ends "Worker result SUCCESS" with no foreground refusal.
      - If it does not start within 15 minutes and the scheduler dump shows it held for quota or standby, it is recorded "not reached: held by the system's job quota (BUG-025)" with that line.
      - That is neither a pass nor a failure.
  - **Recording:**
    - A step that cannot be run is recorded "not run" with the reason and is never reported as passed.
    - Evidence is the app's log lines passed through the sanitizer of story U-063, and the relevant scheduler-dump and diagnostics lines, sanitized, in the story's evidence folder.
    - **No screenshot and no UI dump goes into the repository**, because the owner's address is on screen (U-075 ruling SC-1). Raw device logs and any copied database stay outside the repository.
    - The owner's e-mail address, machine names and profile paths never go into the evidence.

### Integration Criteria

The fix creates no new component; it changes two methods of the one production adapter. The scaffold's registry criteria are restated for that.

- [ ] IC-1 (no signature changes; callers unchanged; BUG-026 IC-1, BUG-027 IC-1):
  - The port keeps its signatures: `@Nullable ScheduledJob scheduleRegular()` (`BackupScheduler.java:67-68`) and `@Nullable ScheduledJob scheduleBootup()` (`:91-92`).
  - Their production callers, `BootReceiver.java:33` and `App.java:377`, are unchanged and compile.
  - Checked by `:app:assembleDebug`, AC-15, and the output of `git grep -n -e "scheduleBootup" -e "scheduleRegular" <end> -- app/src/main`, which shows no new caller.
- [ ] IC-2 (persisted work keeps its identity; BUG-027 IC-2):
  - These stay unchanged: the unique name `REGULAR`, the worker class `BackupWorker`, and the tag `REGULAR` (from which `BackupWorker.inferBackupType()` infers the type, `BackupWorker.kt:501-507`).
  - An installed build's entries under `REGULAR` are taken over: periodic ones by AC-1 and AC-10, one-time ones in any state by AC-3.
  - No new unique name is introduced; `BOOT` is not used (contract unique-name table, "No `BOOT` unique name is used"). `git grep -n '"BOOT"' <end> -- app/src/main` prints nothing.

## Affected Code

Lines are at commit `74b31759`. **Locate every site by function name and statement text, not by line number.** The plan re-derives each line at Start (P-5).

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt` `scheduleRegular()` (`:170-202`) | Degraded gate (`:171-174`), auto-backup gate (`:175-178`), then `maxOf(regularTimeoutSecs, 900)` (`:180`), so −1 and 0 become 900 s. Periodic request (`:182-190`), `enqueueUniquePeriodicWork(REGULAR, UPDATE)` (`:192-197`) with the returned `Operation` ignored; the log line is printed whatever happens (`:199-200`) | Gates unchanged and first. A non-positive interval cancels `REGULAR`, writes the "Never" line and returns `null`. Otherwise it calls the shared registration path with no initial delay |
| same file, `scheduleBootup()` (`:262-286`) | Degraded gate (`:263-266`); auto backup off leads to `cancelAll()` and `null` (`:267-271`); otherwise a **one-time** `BackupWorker` with a 60 s delay and tag `REGULAR` (`:273-278`), `enqueueUniqueWork(REGULAR, ExistingWorkPolicy.REPLACE)` (`:280-281`), which deletes the periodic work | Both gates unchanged and first. Then the same "Never" rule. Then the shared registration path with `BOOT_BACKUP_DELAY_SECS` (`:96`) as the initial delay. No one-time request |
| same file, new private registration path | Does not exist | Builds today's periodic request (`:182-190`), adds the initial delay when it is positive, and enqueues with `UPDATE`. On a failed operation it logs the warning, re-checks the three gates and re-enqueues the same request with `CANCEL_AND_REENQUEUE`. The listener holds only the application context (`:80`) |
| same file, comments (`:40-42`, `:67-69`, `:112-115`, `:160-169`, `:252-261`) | INV-1 text without the fallback; unique names "per `BackupType`"; the floor "used when getRegularTimeoutSecs() returns a value below 15 minutes"; KDocs without "Never" and with "schedules regular backup with BOOT_BACKUP_DELAY" | As AC-16 (a) to (e) |
| `app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java` (`:36`, `:58-68`, `:70-72`, `:83-92`), javadoc only | "Contract: CNTR-MODERNIZATION-004 v2"; `scheduleRegular` "REPLACE/UPDATE … null if auto-backup is disabled"; the call-log URI "when `isCallLogBackupAfterCallEnabled()`"; `scheduleBootup` "schedules a regular backup with the 60-second boot delay" | As AC-16 (f). Comment lines only. `:32-33` is left for U-077 |
| `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt` (or a new test class beside it) | 25 `@Test` at `74b31759`. No periodic-then-boot test, no one-time seed, no "Never" test, no interval assertion (BUG-027 Description item 9; BUG-026 Description item 8) | New tests AC-1 to AC-13. No existing method edited |

Read, not changed: `App.java` (`:373-385`), `receiver/BootReceiver.java` (`:24`, `:31-34`), `receiver/PackageReplacedReceiver.java` (`:20-29`), `service/BackupWorker.kt` (`:501-507`, and its first statement as U-075 leaves it), `AndroidManifest.xml` (`:141-155`), CNTR-MODERNIZATION-004 v4, DES-MODERNIZATION-005, the two bugs, CR-017.

## Existing Behavior to Preserve

- **The encryption-degraded gate** stays first on both methods: `null`, nothing enqueued, nothing cancelled (U-054; contract Rules 4 and 4a). This holds under "Never" too.
- **Auto backup off.** `scheduleBootup()` calls `cancelAll()` and returns `null` (Rule 4, first sentence). `scheduleRegular()` returns `null` without cancelling (unchanged; PO-D is not this story's).
- **The periodic request for a positive interval** keeps:
  - the interval `max(value, 900 s)`;
  - network `UNMETERED` when Wi-Fi only is on, else `CONNECTED` (INV-2);
  - backoff `EXPONENTIAL` 30 s (INV-3);
  - no charging constraint;
  - the tag and unique name `REGULAR`;
  - `UPDATE` as the primary policy (INV-1).
- **The content trigger** (`CONTENT_TRIGGER`, `UPDATE`, 15 min) is unchanged, and so are:
  - the incoming backup (`INCOMING`, `REPLACE`, delay `getIncomingTimeoutSecs()`);
  - the immediate, manual, skip and restore paths;
  - `cancel()`, `cancelAll()` and `cancelRegular()`;
  - `App.rescheduleJobs()`, `BootReceiver`, `PackageReplacedReceiver` and the manifest.
- **The settings keys, values and summaries** (`auto_backup_schedule`: 1800, 3600, 7200, 21600, 86400, −1; default 7200).
- **The log line that device checks grep for**: `bootup` (`BootReceiver.java:32`). The scheduler's own boot and regular log texts change (Terms), and nothing parses them (BUG-026 Suggested Approach, "Risks").

## Verification Steps

Run Gradle in a shell without `MSYS_NO_PATHCONV`, on JDK 25. Do not call `diff` or `cmp`; compare with `git diff --no-index` or `sha256sum`. `<start>` is the Start commit and `<end>` the story's last commit.

1. **Start conditions.** Check S-1 to S-4 (Technical Context); record `<start>`.
2. **Library facts.** Settle P-1 to P-4 and record each finding with its source. If a stop condition is met, stop and report before any edit.
3. **Gate and count at Start.** Run AC-18's gate once at Start, and take AC-19's count with the Start commit id.
4. **Characterization first.** Write AC-11, AC-12 and AC-13's tests and run them against the unchanged code. All are green; record the output.
5. **Failing first.** Write AC-1 to AC-4 and AC-8 to AC-10. Run them against the unchanged code (Start = the old scheduler, S-3). Each fails for the reason its criterion states; record the output. A test that passes here is a defect of the test and is fixed before the production edit.
6. **Fix.** Make the production change and the comment edits (AC-16). Write AC-6's test(s). All new tests are green, and the "Never", boot and fallback lines are read against Terms.
7. **Existing tests.** They pass with no edit (AC-14). `git diff <start> -- app/src/test` shows added methods, imports or files only.
8. **AC-15.** Run the `git diff --name-only` and `git diff` commands, and read the two production diffs against the allowed places.
9. **AC-20.** Run M-1 to M-7 one at a time by the protocol, and compare the observed failing sets with the derived ones.
10. **AC-18 and AC-19.** Run the gate at the end and take the count at the end, listing other stories' tests separately.
11. **IC-1 and IC-2.** Run the two `git grep` searches.
12. **AC-16 and AC-17.** QA reads each comment place, the README lines and the conformance tables against the contract text at Start.
13. **AC-21.** The orchestrator runs steps (i) to (iii) in order, optionally (iv), and collects the sanitized lines. The developer runs no device step.
14. **Close-out.** When the story is closed, the orchestrator applies X-1. That is not a developer step.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Kotlin, WorkManager 2.9.1, Robolectric, Gradle) | `WorkManagerScheduler.kt` (two methods, the registration path, comments), `BackupScheduler.java` javadoc, the new tests, the failure injections, the gates, obligations P-1 to P-5 | Developer |
| Device check | AC-21 on `Pixel_9_Pro` only | Orchestrator |
| Contract close-out | X-1 (deletes K-1) when the story closes | Orchestrator |

## Technical Context

### Placement and start conditions (each with a stop rule)

- **S-1. Placement.**
  - This story is in sprint 025 after story U-072 and before story U-073 (ruling 55; CR-017 "Order relative to the code").
  - It is also after U-075 (ruling 60) and U-074, both of which come earlier in the sprint order (U-075 is sprint 024, wave 4; U-074 is sprint 025, wave 1).
  - The Sprint Manager assigns its wave when sprint 025 is revised (sprint-025 plan, "Expected, not planned").
  - It must not run concurrently with story U-077. Both edit `BackupScheduler.java`, at different lines (CR-017, "Order between the two stories"). Whichever is second re-reads its start state.
  - Stop rule: if `git log` at Start does not show U-072, U-075 and U-074 closed, stop and report.
- **S-2. Dependencies.**
  - **U-072** is listed for order: it is this story's predecessor in the sprint, and its closing commit is Start.
  - **U-075** is listed because it changes `BackupWorker.kt`'s first statement (`setForeground(createBackupForegroundInfo())`, `BackupWorker.kt:123`) so that a backup that starts off screen no longer fails (BUG-029). Without it, every regular backup this story keeps or registers fails when it starts with the app off screen. Device step (iv), and the value of the whole fix, would then mean nothing (CR-017 citation item 8). Until U-075 closes, ruling 61 applies to device steps.
- **S-3. The scheduler file at Start equals the old scheduler.** `git diff --stat 74b31759 <start> -- app/src/main/java/com/zegoggles/smssync/scheduler/` is empty. None of U-070, U-075, U-074, U-071 or U-072 is planned to touch it.
  - Stop rule: if it is not empty, stop and report. The red-at-Start runs would then not be runs on the code D-23 and D-24 name, and the plan must instead run the new tests against the old scheduler file put in place by `git show 74b31759:<path>` (hash recorded, restored by `sha256sum`). The orchestrator decides which.
- **S-4. Library version.** `app/build.gradle` at Start still declares `androidx.work:work-runtime-ktx:2.9.1` and `work-testing:2.9.1` (`:155-156` at `74b31759`). Story U-073, which raises it to 2.12.0, follows this story.
  - Stop rule: if the version at Start is not 2.9.1, stop and report. P-1 to P-4 would have to be re-read for that version.

### Plan obligations (the bugs' and CR-017's "not verified" points, made explicit)

Each obligation is settled from the **WorkManager 2.9.1** artifacts in the local Gradle cache: the bytecode with `javap`, and the sources jar if present. No network fetch. A short probe test may be used, run in the harness and deleted before the fix (not counted, not committed). Each finding is recorded with its source (class and method, or probe output).

- **P-1. What `enqueueUniquePeriodicWork(name, UPDATE, request)` does when it meets each kind of existing entry:**
  - **(a) An existing periodic entry that has not run yet**, for example the one AC-1 sets up, or one a boot call just registered with a 60 s initial delay.
    - Is the `id` kept? Is `generation` raised by exactly one?
    - Are `lastEnqueueTime` and `periodCount` kept, so that the registration is not reset?
    - Is the new request's initial delay applied to a registration with `periodCount == 0`? In other words, does a boot call over a never-run registration reset its first-run time, and does a later `scheduleRegular()` with no delay drop a pending 60 s delay?
    - Is a `RUNNING` worker left running?

    BUG-027 read the update path in 2.12.0 (`WorkerUpdater.kt:45-106`) and saw only the type-check message and the `copy` call in 2.9.1 (Not verified 3, 5). CR-017's C-3 claims only "UPDATE keeps an existing registration (its id and schedule; a running regular backup is not cancelled)" (CR-017 Not verified, second bullet).
    - **Stop condition:** if 2.9.1 does not keep the `id`, or resets `lastEnqueueTime` or `periodCount`, or cancels a running worker, Rule 4's "MUST NOT cancel or reset a registered or running regular backup" cannot be met with `UPDATE`. Stop and report to the orchestrator; the contract text would over-promise.

    If only the first-run time of a never-run registration moves by the boot delay, that is recorded. It is not a stop: the registration is not "registered and run" yet. The plan states the observed behaviour in Implementation Notes for the contract owner.
  - **(b) An existing ONE-TIME entry under the same unique name**, left by an earlier build.
    - Does 2.9.1 reject it in every state, with the type check before the cancelled case (BUG-027 Description item 5, read in 2.9.1 bytecode)?
    - Does the rejection surface only through the returned `Operation` (`getResult()` completing exceptionally), and not as an exception thrown from `enqueueUniquePeriodicWork`?
    - Does `ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE` exist in 2.9.1, and does it remove a one-time entry and insert the periodic one (BUG-027 Description item 3 and Suggested Approach step 3)?

    The fallback the bug proposes rests on these three points.
    - **Stop condition:** if `UPDATE` does not fail on a one-time entry, or fails in a way the fix cannot observe without blocking the caller's thread, or `CANCEL_AND_REENQUEUE` does not remove the one-time entry, stop and report. The shape of the fix (and C-2 and C-3's "if UPDATE fails … CANCEL_AND_REENQUEUE") would need the orchestrator's decision. `scheduleBootup()` runs on the boot receiver's main thread (BUG-027 Suggested Approach, "Options considered", last bullet), so a blocking query is not a silent substitute.
  - **(c) A cancelled or finished entry.**
    - A cancelled periodic entry: is it deleted and the new request enqueued (the cancelled case of `enqueueUniquelyNamedPeriodic`)?
    - A cancelled or succeeded one-time entry: is the type check reached first, so that it is (b)?
    - Is a pruned name treated as absent (`enqueueNew`)?
    - **Stop condition:** if the behaviour differs from BUG-027's reading in a way that changes an outcome AC-3, AC-8 (c) or AC-10 asserts, stop and report. Test expectations are not adjusted to fit.
- **P-2. When the fallback runs in the harness.**
  - On which executor does a listener on `operation.result` run in production, and in the harness? The harness sets the worker executor to a single thread (`WorkManagerSchedulerTest.kt:63-67`); the task executor is whatever `WorkManagerTestInitHelper` installs. BUG-027 Not verified 8 expects `SynchronousExecutor`, with the branch not fully traced.
  - Can a test observe the fallback's result deterministically, by completion of a future and not by sleeping?
  - **Stop condition:** if the only way to observe it is a timed wait, stop and report. No `Thread.sleep`-based test is written.
- **P-3. Can the harness drive the one-time seed (S1) to `SUCCEEDED`** with `WorkManagerTestInitHelper.getTestDriver(...)` and the harness's factory (BUG-027 Not verified 7)? If not, AC-3 (c) is recorded "not tested" with the reason. That is not a stop: the type check comes before the state check (P-1 (b)), so the `ENQUEUED` and `CANCELLED` cases show the defect.
- **P-4. The `WorkInfo` accessors in 2.9.1** that the tests use (`getId`, `getGeneration`, `getPeriodicityInfo().getRepeatIntervalMillis()`, `getInitialDelayMillis`, `getConstraints().getRequiredNetworkType()`) exist, and report for a test-enqueued entry what production does (BUG-026 Not verified 5). If an accessor is missing, the assertion is read from the `WorkSpec` through the test helper the existing `inv3_*` tests use (`:283`), and recorded so.
- **P-5. Re-anchor on Start.** Every line in this story is at `74b31759`. The plan re-derives each site at Start in the two production files, the test file, `App.java` and the receivers, and records the mapping where a line moved.

### Binding constraints for the plan

- **Comparison tool** (product owner's decision of 2026-11-21, decision appendix Record 18):
  - No agent calls the external programs `diff` or `cmp`. Comparisons use `git diff --no-index` or `sha256sum`.
  - Shell activity stays modest.
  - If a tool disappears or a process is killed without a visible reason, the agent stops and reports.
  - Nothing is restored from quarantine, and no exclusion is added by any agent.
- **No test edits, no new production seam, no dependency change, no `@Ignore`, no floor lowered, no device step by the developer.** The developer commits nothing unless the sprint's commit rule for this story says otherwise.
- **No blocking call** on the caller's thread to read the existing entry before choosing a policy (P-1 (b)).
- **Test-count rule:** AC-19. A planning figure for the number of new tests is not a criterion.

### Library and contract facts the approach rests on

These were read by the bugs' Solution Architect, not run here. In short:
- `REPLACE` deletes every entry under the name (BUG-027 Description item 3, 2.9.1 bytecode and 2.12.0 sources);
- `UPDATE` rejects a one-time entry in every state, and keeps a periodic one in place (item 5);
- WorkManager prunes finished work only after a day, and never prunes an `ENQUEUED` entry (item 6);
- WorkManager itself clamps a periodic interval below 15 minutes (BUG-026 item 5).

The contract ruling is a behavioural break of the boot path, non-breaking for consumers, gated by the INV-1 tests (CR-017, "The ruling this record makes", Parts 1 and 2; Scope Concern 1 ruling). Story U-073 then carries this story's tests unedited across WorkManager 2.12.0 (REQ-MODERNIZATION-016 AC-8; CR-017, "Interaction with U-073").

## Supporting Documentation

- **Bugs:**
  - BUG-027: Description items 1 to 9, Evidence (the dated device note), Steps to Reproduce A and B, Expected Behavior, AC-1 to AC-11, IC-1, IC-2, Suggested Approach ("Fix", "Why this shape", "Options considered and rejected", "Upgrade", "Risks"), Notes PO-1, PO-2, Not verified.
  - BUG-026: Description, AC-1 to AC-7, IC-1, IC-2, Suggested Approach, Not verified.
- **Change record CR-017:** "The ruling this record makes" (Parts 1 and 2); "What the story that fixes BUG-026 and BUG-027 must cite" with Amendment 1's "Changes to obligations elsewhere in this record"; the scope-concern rulings (concerns 2 and 12); Amendment 1 (S-1, S-4, S-5, S-7, and the `BackupScheduler.java:71-72` hand-over); "Order relative to the code"; close-out edit X-1.
- **Decision appendix** (`sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`):
  - Record 20: rulings 55 and 56, PO-G "Allow all of them", and the held BUG-027 questions at `:2479`;
  - Record 21: rulings 60, 61, 62;
  - Record 22: rulings 67, 68;
  - Record 18: the comparison tool.
- **Observation:** `sdlc/artifacts/build/research/2026-11-21-background-backup-and-boot-observation.md` (Run 1; "What it shows" item 3).
- **Requirements:**
  - REQ-MODERNIZATION-005 ("preserving all current scheduling behavior"; the CR-017 notes);
  - REQ-MODERNIZATION-015 AC-13 as amended by CR-016 Amendment 1 (the count rule);
  - REQ-MODERNIZATION-016 AC-8 (U-073 carries the scheduler tests unedited).
- **Design:** DES-MODERNIZATION-005, Design Validation, the INV-1 row (D-9 as amended by D-23) and the "Never" row (D-11 as amended by D-24); the `scheduleRegular()` and `scheduleBootup()` port rows; the lifetime mapping row; the value-object rows `existingPolicy`, `uniqueName`, `bootDelaySecs`; the risk row "A one-time entry under a periodic unique name blocks `UPDATE`"; the CR-017 note (D-19).
- **Model stories:** `sdlc/artifacts/build/sprints/sprint-024/U-075-automatic-backup-runs-when-the-app-is-off-screen.md` (structure, device rules, mutation protocol); `sdlc/artifacts/build/sprints/sprint-024/U-069-content-trigger-backup-goes-through-the-scheduler.md`; U-074 AC-15 (mutation protocol).

## Integration Contract References

**CNTR-MODERNIZATION-004 v4** (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md`). This story implements these sections, read in the contract file at Start:
- the Interface block: `scheduleRegular()` (C-2) and `scheduleBootup()` (C-3);
- the `BackupType` notes: the boot-path bullet (C-4);
- "Unique work names and policies (v4)": the `REGULAR` row and "No `BOOT` unique name is used" (C-5);
- `SchedulerConfig.initialDelaySecs`, the boot comment (C-7);
- the `ExistingPolicy` enum note: the `REGULAR` fallback (C-9);
- INV-1's realization (C-10);
- Validation Rule 4 as amended by C-25, and Rule 4a (C-17);
- the regular example (C-18);
- "Code not yet at v4", item K-1 as amended by C-26: **this story closes it** (X-1);
- the Breaking change policy: the boot-path behavioural break is gated by the INV-1 tests (AC-1 to AC-4).

INV-4's row and note are not changed by this story. Its evidence (`inv4_*`, `BackupTriggerWorkerTest`) is kept green unedited (AC-14). The SMS-received receiver half of v4 (K-2) is story U-077's.

**DES-MODERNIZATION-005** (`sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md`) names the tests this story adds:
- the INV-1 row (D-23), met by AC-1 to AC-4;
- the "Never" row (D-24), met by AC-8 to AC-11.

No contract or design file is edited by this story (AC-15). The contract is already at v4, ahead of the code by design (CR-017, option A).

## Implementation Notes
<!-- Added by agents during build -->

None yet. Record here:
- the Start commit, and the S-1 to S-4 results;
- the findings of P-1 to P-4 with their sources, and P-5's line mapping;
- the red-at-Start output of AC-1 to AC-4 and AC-8 to AC-10;
- AC-3 (c) tested or "not tested" with the reason;
- how AC-6 was proved;
- the counts (AC-19);
- the PO-1 and PO-2 answer used ("no", ruling 68; CR-017 citation item 4);
- the note that a `RUNNING` regular backup is not covered by a test (AC-5).

## Review Findings
<!-- Summarized from workspace artifacts -->

None yet.

## Notes

- **Origin.** BUG-027, raised by the Solution Architect while writing BUG-025's approach and confirmed on the emulator (Record 21, ruling 62), and BUG-026, from the same work. They are fixed together by ruling 55, against CR-017 (ruling 67), with the owner's two questions answered "no" by ruling 68. Written 2026-11-21.
- **Severity and priority.** BUG-027 is medium severity and high priority; BUG-026 is low and medium. The story takes the higher priority.
- **Contract close-out.** When this story closes, the orchestrator deletes K-1 (X-1). Once U-077 has also closed (X-2), the whole "Code not yet at v4" blockquote goes.
- **Hand-overs (not this story's work):**
  - **H-1 (story U-073):** the new boot and "Never" tests are among "every test covering CNTR-MODERNIZATION-004" that must pass unmodified across WorkManager 2.12.0 (REQ-MODERNIZATION-016 AC-8). P-1's findings for 2.9.1 should be re-read for 2.12.0 in U-073's plan; BUG-027 read the 2.12.0 sources already.
  - **H-2 (orchestrator):** if P-1 (a) shows that a boot call moves the first run of a never-run registration, the finding goes to the contract owner as a possible wording point on C-3 ("keeps an existing registration (its id and schedule)"). It is not a defect of this story.
  - **H-3 (housekeeping record, CR-017 Scope Concern 7):** none added by this story.
- **Product owner items.** PO-1 and PO-2 of BUG-027: built "no" (see the Story section for what a "yes" changes). PO-D: not required by v4 (see "Not in this story"). None blocks the build.
