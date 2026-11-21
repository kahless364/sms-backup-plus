---
type: bug
status: ready
artifact_type: bug
severity: low
priority: medium
complexity: low
parallel_eligible: true
iteration: 1
requirements:
  - REQ-MODERNIZATION-005
design_docs:
  - DES-MODERNIZATION-005
integration_contracts:
  - CNTR-MODERNIZATION-004
dependencies: []
related_items:
  - BUG-025
  - BUG-023
  - U-069
  - U-073
  - U-068
  - U-014
  - U-017
  - CNTR-MODERNIZATION-004
  - DES-MODERNIZATION-005
platforms: []
tags:
  - workmanager
  - scheduler
  - regular-backup
  - settings
id: BUG-026
title: A regular schedule of "Never" still schedules a backup every fifteen minutes
domain: build
origin: code-review
updated_at: '2026-11-21T14:32:17.134Z'
---

# BUG-026: A regular schedule of "Never" still schedules a backup every fifteen minutes

## Description

The auto-backup settings offer a "Regular schedule" whose last choice is "Never". With auto backup on and "Never" chosen, the app should run no regular backup; only incoming messages should start backups. Instead, the scheduler turns "Never" into a periodic backup every fifteen minutes. That is more often than the most frequent choice the setting offers (30 minutes).

Found by the Solution Architect while writing BUG-025's approach. The Requirements Analyst first noticed it there (BUG-025, Notes, "Observations outside this bug"). Every statement below was checked against commit `0d490997` (`git show`, not the working tree) and, for the legacy code, against `d095fe02^`, the parent of the commit that deleted it. Line numbers are those commits'.

**The chain, confirmed:**

1. **The setting offers "Never", with the value −1.**
   - The preference `auto_backup_schedule` (`app/src/main/res/xml/preferences.xml:187-193`) uses `@array/auto_backup_schedule_entries` and `@array/auto_backup_schedule_values`, default `7200`.
   - Entries (`app/src/main/res/values/arrays.xml:74-81`): 30 mins, 1 h, 2 h, 6 h, 24 h, `@string/never`.
   - Values (`:83-90`): `1800`, `3600`, `7200`, `21600`, `86400`, `-1`.
   - `@string/never` is "Never" (`app/src/main/res/values/strings.xml:256`). The setting's label and summary are "Regular schedule" and "When to backup SMS" (`:166-167`).
   - The incoming schedule has its own array, also ending in "Never" = `-1` (`arrays.xml:92-106`).
2. **The preference returns −1.** `Preferences.getRegularTimeoutSecs()` is `getStringAsInt(REGULAR_TIMEOUT_SECONDS, Defaults.REGULAR_TIMEOUT_SECONDS)` (`app/src/main/java/com/zegoggles/smssync/preferences/Preferences.java:204-205`). `getStringAsInt` returns `Integer.valueOf("-1")`, that is −1 (`:185-194`). The default `2 * 60 * 60` (`Defaults.java:19`) applies only when nothing is stored.
3. **One caller, no guard on the value.** A search of `app/src` for `scheduleRegular`, `regularTimeoutSecs`, `RegularTimeoutSecs` and `REGULAR_TIMEOUT` finds one production caller: `App.rescheduleJobs()` (`app/src/main/java/com/zegoggles/smssync/App.java:373-385`).
   - It calls `scheduler.cancelAll()` (`:374`).
   - Then, if auto backup is on (`:376`), it calls `scheduler.scheduleRegular()` (`:377`) with no check of the regular value.
   - The only value check in the method is for the incoming schedule, before the content trigger is registered (`:381-382`).
   - The other matches are the settings screens, which only display the value (`MainSettings.java:131-144` shows "Regular schedule: Never"; `AutoBackupSettings.java:21` lists the key for change events), the port declaration (`scheduler/BackupScheduler.java:68`) and the tests.
4. **The scheduler clamps instead of declining.** `WorkManagerScheduler.scheduleRegular()` (`app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt:170-202`) checks the encryption-degraded state (`:171-174`) and auto backup (`:175-178`). It then computes `val intervalSecs = maxOf(preferences.regularTimeoutSecs.toLong(), PERIODIC_MIN_INTERVAL_SECS)` (`:180`), which for −1 is 900. It enqueues a `PeriodicWorkRequest` of `BackupWorker` every 900 s with the network constraint, exponential 30 s backoff and tag `REGULAR`, under the unique name `REGULAR` with `UPDATE` (`:182-197`). It logs "enqueued REGULAR with interval=900s" (`:199-200`).
   - The KDoc says "minimum 15 minutes per WM API" and "Returns null if autoBackupEnabled is false" (`:163-164`). The constant's comment says the floor "is used when getRegularTimeoutSecs() returns a value below 15 minutes" (`:112-115`).
   - Neither mentions a disabled value. The comment describes exactly what happens to −1, but it reads as written for a positive value that is too small.
5. **Removing `maxOf` alone would not fix it.** WorkManager clamps any periodic interval below 15 minutes to 15 minutes, with the warning "Interval duration lesser than minimum allowed value; Changed to 900000". Read in 2.12.0 at `WorkSpec.kt:203-216` and found as the same string in the 2.9.1 bytecode of `WorkSpec`. A non-positive value must therefore be handled by an explicit early return.
6. **The incoming schedule's "Never" is handled.** `scheduleIncoming()` returns `null` when `timeoutSecs <= 0` (`WorkManagerScheduler.kt:138-143`), and `App.rescheduleJobs()` registers the content trigger only when `getIncomingTimeoutSecs() > 0` (`App.java:381-382`). Only the regular path lacks the check.
7. **The legacy app scheduled nothing for "Never".**
   - `BackupJobs.scheduleRegular()` was `schedule(preferences.getRegularTimeoutSecs(), REGULAR, false)` (`app/src/main/java/com/zegoggles/smssync/service/BackupJobs.java:78-80` at `d095fe02^`).
   - `schedule()` created a job only if `force || (preferences.isAutoBackupEnabled() && inSeconds > 0)` (`:132`). Otherwise it logged and returned `null` (`:141-144`).
   - The legacy `App.rescheduleJobs()` also cancelled everything first (`App.java:195-205` at `d095fe02^`).
   - So "Never" meant no regular job, and the old scheduler logged "no next sync" when `scheduleRegular()` returned `null` (`SmsBackupService.java:276-284` at `d095fe02^`).
   - **Not verified:** that the legacy app was run with "Never" and observed. This is read from the code only.
8. **No test pins the clamp, and none tests "Never".**
   - `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt` (25 `@Test` at `0d490997`) is the only test file that sets `regularTimeoutSecs`. It always stubs 3600 (`:73`, `:100`).
   - Its `scheduleRegular()` tests check single flight (`:98-112`), the network type (`:162-`, `:181-`), the absence of a charging constraint (`:219-`), backoff (`:282-`) and the degraded gate (`:533-`).
   - None uses a value of 0 or below, or below 900, and none reads the interval. The clamp is therefore not pinned as intended behaviour by any test.
9. **The contract and design are silent on "disabled".**
   - CNTR-MODERNIZATION-004 describes `scheduleRegular()` as "recurring backup; network per isWifiOnly(); EXPONENTIAL 30s→300s backoff; REPLACE/UPDATE on a stable unique name. (BackupJobs.java:78)" (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md:62-64`). It states no minimum interval and no disabled value. It says the port maps each method to a "verified public method of `BackupJobs.java`" (`:108`), and that the adapter is "a mechanical translation of the current `BackupJobs.java` behavior, not a reinterpretation of it" (Overview). By its own terms, the legacy `inSeconds > 0` guard is part of what it carries.
   - DES-MODERNIZATION-005 describes `scheduleRegular()` as "periodic / recurring, constraint-bound" (`:88`) and "recurring backup ... `enqueueUniquePeriodicWork(REGULAR, REPLACE/UPDATE)`" (`:435`). Its mapping table row "`Trigger.NOW` when `inSeconds <= 0` (`:162`) → `OneTimeWorkRequest` with no initial delay" (`:280`) misreads the legacy code for every call except `scheduleImmediate`. `Trigger.NOW` was reachable only with `force == true`, that is from `scheduleImmediate()` (`BackupJobs.java:99-100`). For a non-forced `inSeconds <= 0` the legacy code scheduled nothing (`:132`).
   - The 15-minute floor appears only in the code comment (`WorkManagerScheduler.kt:112-115`).

**Who is affected.** Anyone with auto backup on and "Regular schedule: Never". The default is 2 hours (`preferences.xml:193`, `Defaults.java:19`), so the owner is affected only if the owner chose "Never" on a phone. Which value the owner's phones use is not known.

## Steps to Reproduce

Nothing was run for this artifact (no Gradle, no emulator). Both procedures are described, not observed.

**A. Unit level (the existing harness, `WorkManagerSchedulerTest.kt:42-81`).**
1. Stub `preferences.regularTimeoutSecs` to return −1, with auto backup on (the harness default, `:71`).
2. Call `scheduler.scheduleRegular()`.
3. Observe on today's code: a non-null `ScheduledJob` with description "WorkManagerScheduler:REGULAR @ 900s" (`WorkManagerScheduler.kt:201`). `getWorkInfosForUniqueWork("REGULAR")` holds one `ENQUEUED` entry whose `periodicityInfo.repeatIntervalMillis` is 900 000 (`WorkInfo.getPeriodicityInfo()` exists in 2.9.1, read in the bytecode).

**B. Device (debug build, auto backup on, mail account set).**
1. Settings, Auto backup settings, "Regular schedule": choose "Never". The change emits `AutoBackupSettingsChanged`, so `rescheduleJobs()` runs.
2. Read the app log: "WorkManagerScheduler.scheduleRegular: enqueued REGULAR with interval=900s" (`:199-200`).
3. Read `adb shell dumpsys jobscheduler` for the app: a job for the `REGULAR` work. Or ask WorkManager: `adb shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS -p com.zegoggles.smssync`; the receiver was read in the 2.9.1 manifest, and this use is not verified.
4. Wait: a `BackupWorker ... type=REGULAR` run about every 15 minutes, while the app has quota (BUG-025, mechanism 2).

## Expected Behavior

"Regular schedule: Never" schedules no regular backup, and removes one that exists, as the legacy app did (`BackupJobs.java:132`). Incoming backups (content trigger and delayed `INCOMING`) work as configured. A positive value below fifteen minutes, which the settings do not offer and only a stale or hand-edited value could carry, is still raised to WorkManager's fifteen-minute minimum. Every offered positive value gives exactly that interval.

## Actual Behavior

"Never" (−1) gives a periodic backup every 15 minutes (900 s), more often than any offered choice. A value of 0 would do the same. Each run:
- starts `BackupWorker`, which posts its foreground notification first (`app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt:123`, `:517-538`);
- queries the providers;
- returns without a mail-server connection when there is nothing new ("nothing to backup", `:252-262`);
- otherwise uploads whatever is new.

So the user sees a backup notification up to about 96 times a day, the phone wakes for it, and new items reach the mailbox up to 15 minutes after they appear. Data the user has selected for backup but does not expect to be uploaded outside incoming backups, such as call-log entries when "after call" is off, is uploaded then. Nothing is lost or duplicated; the watermark rules are unchanged. The settings summary meanwhile says "Regular schedule: Never" (`MainSettings.java:138-140`).

## Evidence

| Evidence | What it shows |
|---|---|
| `app/src/main/res/values/arrays.xml:74-90`; `strings.xml:256`; `xml/preferences.xml:187-193` | The setting offers "Never" = −1; default 7200 |
| `preferences/Preferences.java:185-194`, `:204-205` | −1 is returned as −1 |
| `App.java:373-385` | The only caller; no check of the regular value; the incoming value is checked (`:381`) |
| `scheduler/WorkManagerScheduler.kt:112-116`, `:160-202` (`:180`) | The clamp; no non-positive branch; the log line prints the interval |
| `scheduler/WorkManagerScheduler.kt:133-143` | `scheduleIncoming()` declines `<= 0`: the contrast |
| WorkManager `WorkSpec.kt:203-216` (2.12.0, read); the same message in 2.9.1 (bytecode, read) | The library would clamp −1 to 15 minutes even without `maxOf` |
| `BackupJobs.java:78-80`, `:127-145` (`:132`), `:99-100`, `:162`; `App.java:195-205`; `SmsBackupService.java:276-284` (all at `d095fe02^`) | Legacy: "Never" scheduled nothing; `Trigger.NOW` only for forced (immediate) jobs |
| `WorkManagerSchedulerTest.kt:42-81`, `:73`, `:98-112` and the other `scheduleRegular` tests | Only 3600 is ever used; no interval assertion; nothing pins the clamp |
| CNTR-MODERNIZATION-004 `:62-64`, `:108`; DES-MODERNIZATION-005 `:88`, `:280`, `:435` | Silent on "disabled"; the DES mapping row misreads the legacy `inSeconds <= 0` case |

## Acceptance Criteria

Each criterion is checked by unit tests with the existing scheduler harness (`WorkManagerSchedulerTest.kt:42-81`: Robolectric, `WorkManagerTestInitHelper`, mocked `Preferences` and `SecretStore`) unless it says otherwise. Each new test must fail on today's code or on the recorded mutation (AC-7).

- [ ] AC-1 ("Never" schedules nothing): With auto backup on, the encryption not degraded and `regularTimeoutSecs` of −1, `scheduleRegular()` returns `null` and leaves no non-terminal entry under the unique name `REGULAR`. The same holds for 0: any non-positive value means "Never", as in the legacy `inSeconds > 0` rule. Checked by two tests, one for −1 and one for 0, that read `getWorkInfosForUniqueWork("REGULAR")`.
- [ ] AC-2 ("Never" removes an existing regular backup): After `scheduleRegular()` has registered a periodic backup with 3600, a call with −1 leaves no non-terminal entry under `REGULAR`, without relying on a prior `cancelAll()`. Checked by a test that calls `scheduleRegular()` twice with the two stubs.
- [ ] AC-3 (the minimum stays for positive values): A positive value below 900, for example 60 or 899, registers a periodic backup with a repeat interval of exactly 900 000 ms (`periodicityInfo.repeatIntervalMillis`), and 900 gives 900 000 ms. Checked by tests on the interval. This test pins the behaviour, not the mechanism: because WorkManager clamps too (Description, item 5), it still passes if the app's `maxOf` is removed. The story records this so that nobody reads it as a test of `maxOf`.
- [ ] AC-4 (other values unchanged): For each offered value, 1800, 3600, 7200, 21600 and 86400, the repeat interval equals the value in milliseconds. The tag `REGULAR`, the unique name `REGULAR`, the `UPDATE` policy, the network constraint per Wi-Fi only (INV-2), the exponential 30 s backoff (INV-3) and the absence of a charging constraint are as before. The existing `scheduleRegular()` tests pass with no edit to their assertions. Checked by one parameterised test or five small ones, plus the existing tests.
- [ ] AC-5 (incoming path unchanged): `scheduleIncoming()`, `scheduleContentTrigger()`, `scheduleBootup()`, `scheduleImmediate()`, `scheduleManual()`, `scheduleRestore()`, `cancelAll()`, `cancel()` and `App.rescheduleJobs()` show no diff. With `regularTimeoutSecs` of −1 and `incomingTimeoutSecs` of 60, `scheduleContentTrigger()` still registers `CONTENT_TRIGGER` and `scheduleIncoming()` still enqueues `INCOMING` with a 60 s delay. Checked by `git diff` over those methods and one test for the two calls.
- [ ] AC-6 (the contract and the code agree): CNTR-MODERNIZATION-004's `scheduleRegular()` text (`:62-64`) and DES-MODERNIZATION-005 (`:88`, `:435`, and the mapping row `:280`) state three things. A non-positive regular interval ("Never") schedules no regular backup and cancels an existing one. A positive interval below 15 minutes is raised to WorkManager's minimum. `Trigger.NOW` applied only to forced (immediate) jobs. The KDoc of `scheduleRegular()` (`WorkManagerScheduler.kt:160-169`) and the constant's comment (`:112-115`) say the same. Done by a change record (non-breaking: it restores the legacy rule the contract says it carries, and changes no signature or invariant). Checked by reading those places against the code.
- [ ] AC-7 (gate and mutations): `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` exits 0 with 0 failures, the skipped count is not higher, and no per-package coverage floor is lowered. The authoritative test count (`git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`) is taken before and after, and the difference is explained by the new tests. Two mutations are recorded: (i) remove the early return, and the AC-1 and AC-2 tests fail; (ii) change `<= 0` to `< 0`, and the 0 test fails.

### Integration Criteria (if fix modifies component boundaries)
- [ ] IC-1: The port `BackupScheduler.scheduleRegular()` keeps its signature (`scheduler/BackupScheduler.java:68`). Its only production caller, `App.java:377`, is unchanged and still compiles. A search of `app/src` for `scheduleRegular` is attached and shows no new caller.
- [ ] IC-2: No persisted work changes shape. The unique name `REGULAR`, the worker class `BackupWorker` and the tag are unchanged, so an installed build's existing `REGULAR` work is updated or cancelled by the same names as before.

## Impact Analysis

Searches run (on `0d490997`, `git grep`): `scheduleRegular`, `regularTimeoutSecs`, `RegularTimeoutSecs`, `REGULAR_TIMEOUT`, `PERIODIC_MIN_INTERVAL`, `auto_backup_schedule`, `intervalDuration` in `app/src`. Also `never`, `-1`, `disabled`, `minimum interval` and `900` in the contract, DES-MODERNIZATION-005 and REQ-MODERNIZATION-005.

| # | Layer | Affected? | Files and lines | Action needed |
|---|-------|-----------|-----------------|---------------|
| 1 | Requirements | No change | `modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md:30` ("preserving all current scheduling behavior") | None. The fix restores current (legacy) behaviour the requirement asks to preserve |
| 2 | Design and contracts | Yes (wording) | CNTR-MODERNIZATION-004 `:62-64`; DES-MODERNIZATION-005 `:88`, `:280`, `:435` | AC-6, by a change record |
| 3 | Stories, sprints, change records | References | BUG-025 (its AC-7 keeps `scheduleRegular()` unchanged; its approach cites this bug); U-073 (REQ-MODERNIZATION-016 AC-8, scheduler tests unmodified) | Sequencing, below |
| 4 | Application code | Yes (root) | `scheduler/WorkManagerScheduler.kt:112-115`, `:160-202` | The fix |
| 5 | Vendored library | No | None | None |
| 6 | Tests | Yes | `service/WorkManagerSchedulerTest.kt` (new tests; no existing assertion edited) | AC-1 to AC-5, AC-7 |
| 7 | Documentation | No | README FAQ "Regular schedule is used to perform backups in specific intervals" (`README.md:304-305`) does not mention "Never"; nothing false after the fix | None |

**Cascade.** The root is the code. The design's mapping row (`:280`) is a second, independent misreading of the same legacy rule. The contract is silent rather than wrong. The fix order is the change record, then the code and tests in one story.

## Suggested Approach

Written by the Solution Architect on 2026-11-21. Advisory until story planning.

**Fix (in `WorkManagerScheduler.scheduleRegular()`, nowhere else).** After the degraded and auto-backup checks, read `preferences.regularTimeoutSecs` once. If it is `<= 0`, call `cancelRegular()`, log "WorkManagerScheduler.scheduleRegular: skipped (regular schedule is Never, value=<n>)" and return `null`. Otherwise keep `maxOf(value, PERIODIC_MIN_INTERVAL_SECS)` and the rest of the method unchanged. Update the KDoc (`:160-169`) and the constant's comment (`:112-115`) to say: non-positive means no regular backup; the floor applies to positive values.

**Why there and not in `App.rescheduleJobs()`.**
- It mirrors `scheduleIncoming()`'s own `<= 0` rule (`:138-143`) and the legacy `schedule()` rule (`BackupJobs.java:132`), so every caller of the port is covered, now and later.
- It is testable with the existing harness. `App.rescheduleJobs()` is private and needs an application-level test.
- The early return must be explicit, because WorkManager would clamp −1 anyway (Description, item 5).
- `cancelRegular()` in the branch makes AC-2 hold for any caller, not only after `cancelAll()`.
- Keep the degraded check first, as today (U-054): a degraded device enqueues nothing and changes nothing.

**Why keep the clamp.** No offered value is below 30 minutes, so the floor only matters for a stale or hand-edited preference. Removing it changes nothing, because WorkManager clamps too. Keeping it makes the logged interval true ("interval=900s" rather than a value WorkManager silently changed). It is cheap and its meaning is now documented.

**Contract.** A small change record amends CNTR-MODERNIZATION-004 (`scheduleRegular()` text) and DES-MODERNIZATION-005 (`:88`, `:280`, `:435`). It is non-breaking: no signature, `BackupType`, `SchedulerState` or invariant changes. The rule it states is the legacy rule the contract already claims to translate. If BUG-025's change record (v4) is written at the same time, the two amendments can share one record. They must not share one story (below).

**Tests (all in `WorkManagerSchedulerTest.kt`, new methods only):** −1 and 0 give no entry (AC-1); 3600 then −1 gives no non-terminal entry (AC-2); 60, 899 and 900 give 900 000 ms (AC-3); the five offered values give their interval (AC-4); −1 regular with 60 incoming still registers the trigger and the incoming backup (AC-5). Read the interval with `WorkInfo.getPeriodicityInfo().getRepeatIntervalMillis()`. The accessor exists in 2.9.1 (bytecode, read). In 2.12.0 it is expected unchanged, but that was not re-read here.

**Where it goes.** As **its own small story**, not inside the BUG-025 fix:
- BUG-025's AC-7 keeps `scheduleRegular()` unchanged on purpose. BUG-025 also waits for owner decisions, while this fix needs none. And BUG-025 changes a behavioural invariant (v4), while this one restores a legacy rule.
- Stories that touch `scheduleRegular()` or its tests: U-068 is built and did not change `scheduleRegular()`. U-069's plan lists `scheduleRegular` among the methods it leaves unchanged (`sdlc/artifacts/build/sprints/sprint-024/U-069-content-trigger-backup-goes-through-the-scheduler.md:103`, `:138`). U-070, U-074, U-071 and U-072 do not edit the scheduler or its test file. U-073 must leave existing tests in that test folder unedited (U-073 story, AC-13).
- **Recommended place:** after U-072 and before the BUG-025 fix and U-073. U-071's plan is already written against a fixed start state, and this keeps it intact. U-073, not yet planned, then plans with these tests already present, so its "unmodified" rule guards them across the WorkManager 2.12.0 move.
- If the orchestrator prefers it earlier, any point after U-069 works, because U-069 edits the same test file. The price is that U-071 re-reads its start state (the test count and the scheduler file).

**Risks.**
- The new `cancelRegular()` call in the "Never" branch also removes a pending boot backup under `REGULAR` (`scheduleBootup()`, `WorkManagerScheduler.kt:273-281`) if `scheduleRegular()` is called while that is pending. Today `rescheduleJobs()` already does this through `cancelAll()`, so the behaviour for the only caller is unchanged.
- Log text is new and nothing parses it.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt` (`:112-115`, `:160-202`) | −1 or 0 becomes a 15-minute periodic backup | Non-positive value: cancel `REGULAR`, return `null`; KDoc and constant comment corrected |
| `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt` | No test below 3600; no interval assertion | New tests (AC-1 to AC-5) |
| `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` (`:62-64`), `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` (`:88`, `:280`, `:435`) | Silent on "Never"; the mapping row misreads `inSeconds <= 0` | AC-6, by change record |

## Existing Behavior to Preserve

- Every offered positive value gives exactly that interval; network per Wi-Fi only (INV-2); exponential 30 s backoff (INV-3); no charging constraint; unique name `REGULAR` with `UPDATE` (INV-1); tag `REGULAR`.
- The encryption-degraded gate returns `null` first and enqueues and cancels nothing (U-054, `:171-174`).
- Auto backup off: `null`, as today (`:175-178`).
- The incoming path, the content trigger, the boot backup, the immediate, manual and restore paths, and `App.rescheduleJobs()`.
- The settings keys and values (`auto_backup_schedule`: 1800, 3600, 7200, 21600, 86400, −1; default 7200) and the settings summary.

## Verification Steps

For QA, per criterion. Run Gradle in a shell without `MSYS_NO_PATHCONV`.

1. AC-1 to AC-5: run the new tests and the existing `WorkManagerSchedulerTest`. All pass, and no existing assertion is edited (`git diff` of the test file shows added methods only).
2. AC-1 and AC-2 fail on the unfixed code: check out the unfixed `WorkManagerScheduler.kt` with the new tests kept, and confirm the −1, 0 and 3600-then-−1 tests fail. Record the run.
3. AC-3: confirm the story records that the test pins the interval, not `maxOf`.
4. AC-5: `git diff` shows no change in the methods named, or in `App.java`.
5. AC-6: read the contract, the design and the two code comments against the code. The change record exists and is applied.
6. AC-7: the gate command; the authoritative test count before and after; the two recorded mutations.
7. IC-1: the `scheduleRegular` search output; `BackupScheduler.java` and `App.java` absent from the diff.
8. Device check (optional, owner's call): choose "Never" and read the log line "skipped (regular schedule is Never ...)". `dumpsys jobscheduler` shows no regular job. Choose "2 h" and read "interval=7200s".

## Root Cause Analysis

Preliminary, from the code:
- **Mechanism.** U-014 mapped the legacy `schedule(inSeconds, REGULAR, false)` to a `PeriodicWorkRequest` and added a 15-minute floor (`maxOf`) for WorkManager's minimum. The legacy guard `inSeconds > 0` (`BackupJobs.java:132`) sat in the shared `schedule()` helper, not in `scheduleRegular()` itself. It was carried into `scheduleIncoming()` (`:139`) but not into `scheduleRegular()`.
- **Why it was not caught.** DES-MODERNIZATION-005's mapping row read `inSeconds <= 0` as `Trigger.NOW` (`:280`), which is true only for the forced immediate job. The contract describes `scheduleRegular()` without a disabled case. The tests use only 3600.

## Technical Context

- **Severity: low** (the scaffold's medium, corrected).
  - Nothing is lost or duplicated, and the watermark rules are untouched. A run with nothing new ends before any mail-server connection (`BackupWorker.kt:252-262`). New items reach the mailbox sooner than the user asked, but they are items the user selected for backup.
  - The visible effect is a backup notification and a wake-up up to about 96 times a day, for users who chose "Never", against a summary that says "Never".
  - The owner is affected only if the owner chose "Never"; the default is 2 hours.
  - On the project's scale (BUG-020, BUG-022 and BUG-024 low; BUG-021 and BUG-023 medium), a setting that does more than asked, with no loss, sits with the low ones.
  - It rises to medium if the owner's phones are set to "Never".
- **Priority: medium.** The fix is small and needs no owner decision. It aligns the code with the contract's own claim. It should land before U-073, so that the runtime-library move guards the new tests.
- **Complexity: low** (the scaffold's medium, corrected). One method, two comments, new tests in an existing harness, one change record.
- **Origin: `code-review`** (kept). Found while writing BUG-025, which came from the plan review of U-069.
- **Library.** WorkManager 2.9.1 in the app (`app/build.gradle:155`). The clamp in `WorkSpec.setPeriodic` and `WorkInfo.getPeriodicityInfo()` were read in the 2.9.1 bytecode, and the clamp also in the 2.12.0 sources.

## Supporting Documentation

- `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` (`:62-64`, `:108`, Validation Rule 4 `:276`)
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` (`:88`, `:276-287` mapping table, `:435`)
- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md` (`:30`)
- `sdlc/artifacts/stories/BUG-025-incoming-sms-backup-delayed-by-trigger-period-and-job-quota.md` (Notes, "Observations outside this bug"; Suggested Approach, "Safety-net correction")
- `sdlc/artifacts/stories/BUG-022-worker-cancellation-checks-do-nothing.md` (model for shape)

## Implementation Notes
<!-- Added by agents during build -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes

### Observations outside this bug (raised, not part of the acceptance criteria)

- **After a reboot the periodic regular backup is deleted and may not come back.** Recommended for its own bug, provisionally medium severity. Read from the code and the library; **not observed on a device**.
  - `BootReceiver` calls `scheduleBootup()` (`receiver/BootReceiver.java:31-34`).
  - `scheduleBootup()` enqueues a **one-time** `BackupWorker` under the unique name `REGULAR` with `ExistingWorkPolicy.REPLACE` (`WorkManagerScheduler.kt:273-281`). `REPLACE` cancels and deletes every entry under the name, including the periodic `REGULAR` work (`EnqueueRunnable.kt:235-250` in 2.12.0; the same structure in the 2.9.1 bytecode). After the boot backup runs, no periodic regular backup exists.
  - It is registered again only through `App.rescheduleJobs()` (a settings change, the call-log permission grant, or a package replace).
  - Even then, `scheduleRegular()` uses `UPDATE`. While the finished one-time entry is still in WorkManager's database, `UPDATE` fails with "Can't update OneTimeWorker to Periodic Worker. Update operation must preserve worker's type." The type is checked before the cancelled case (`WorkerUpdater.kt:141-160` in 2.12.0; the same message in the 2.9.1 bytecode). The failure surfaces in the returned `Operation`, which `scheduleRegular()` ignores, not as an exception.
  - Legacy, for comparison: with the default driver, `scheduleBootup()` scheduled nothing, because the dispatcher kept its jobs (`BackupJobs.java:93-95` at `d095fe02^`).
  - The contract's Validation Rule 4 ("else schedule regular with the 60s boot delay") does not say the boot backup may replace the periodic one.
  - This also undermines the "regular backup is the safety net" statements in BUG-025. A fix touches `scheduleBootup()` and the same test file, and could share this bug's story if the orchestrator wishes; it needs its own criteria.
- **The boot backup runs even with "Never".** `scheduleBootup()` enqueues one backup 60 s after boot whatever the regular value is. Contract Validation Rule 4 requires this. The legacy old-scheduler path did the same (`BackupJobs.java:91-92`), and the legacy default path scheduled nothing. Left unchanged by this bug; whether "Never" should also skip the boot backup is an owner question for the reboot bug above.

### Not verified

1. Nothing was run: no Gradle, no unit test, no device. The reproduction steps are described.
2. That the legacy app, run with "Never", ran no regular backup. This is read from the code only.
3. Which regular schedule value the owner's phones use.
4. `WorkInfo.getPeriodicityInfo()` in WorkManager 2.12.0 (read for 2.9.1 only).
5. That `WorkManagerTestInitHelper` with the harness's executor reports `periodicityInfo` for an enqueued periodic work as in production. It is expected, not run.
6. The diagnostics broadcast's output on the Android 16 emulator.
7. The reboot observation, on a device. Also how soon WorkManager prunes the finished one-time `REGULAR` entry; that decides how long `UPDATE` keeps failing.
