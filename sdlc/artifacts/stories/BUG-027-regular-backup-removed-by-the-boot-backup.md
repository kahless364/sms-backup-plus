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
integration_contracts:
  - CNTR-MODERNIZATION-004
dependencies: []
related_items:
  - BUG-025
  - BUG-026
  - U-014
  - U-017
  - U-054
  - U-069
  - U-071
  - U-072
  - U-073
  - CNTR-MODERNIZATION-004
  - DES-MODERNIZATION-005
  - REQ-MODERNIZATION-005
platforms: []
tags:
  - workmanager
  - scheduler
  - regular-backup
  - boot
  - unique-work
  - inv-1
  - product-owner-decision
id: BUG-027
title: After a phone restart the boot backup deletes the regular backup schedule, and it does not come back on its own
domain: build
origin: code-review
updated_at: '2026-11-21T15:11:34.701Z'
---

# BUG-027: After a phone restart the boot backup deletes the regular backup schedule, and it does not come back on its own

## Description

After every restart of the phone, the app's regular backup (default every two hours: `app/src/main/res/xml/preferences.xml:193`, `app/src/main/java/com/zegoggles/smssync/preferences/Defaults.java:19`) stops being scheduled. The boot backup is enqueued under the same WorkManager unique name as the regular periodic backup, with a policy that deletes whatever is already under that name. After the boot backup has run once, there is no regular backup. A settings change or an app update re-registers it only after WorkManager has pruned the finished boot entry, at least a day later. Until then, re-registration fails silently. From the restart on, only incoming-message backups run, and those carry the delays of BUG-025.

Found by the Solution Architect while writing BUG-025's approach ("Safety-net correction") and recorded in BUG-026, Notes. Every statement below was checked against commit `0d490997` (`git show`, not the working tree). Legacy code was checked against `d095fe02^`, the parent of the commit that deleted it (U-017). Line numbers are those commits'. Library facts are marked **read** (2.9.1 bytecode via `javap`, or 2.12.0 sources, both from the local Gradle cache) or **not read**. The app uses WorkManager 2.9.1 (`app/build.gradle:155-156`).

**The chain, confirmed:**

1. **Who calls `scheduleBootup()`, and when.**
   - There is one production caller, `BootReceiver.bootup()` (`app/src/main/java/com/zegoggles/smssync/receiver/BootReceiver.java:31-34`). It runs on `ACTION_BOOT_COMPLETED` only (`:24`). A search of `app/src` for `scheduleBootup` finds no other caller. The other matches are the port (`scheduler/BackupScheduler.java:92`), the adapter and the tests.
   - Manifest: `RECEIVE_BOOT_COMPLETED` (`app/src/main/AndroidManifest.xml:62`). Receiver `.receiver.BootReceiver`, `enabled="true"`, `exported="false"`, action `BOOT_COMPLETED` (`:141-147`). It has no `directBootAware` attribute.
   - **Conditions.** The receiver component is enabled only while auto backup is on. `App.onCreate()` calls `setBroadcastReceiversEnabled(preferences.isAutoBackupEnabled())` (`App.java:258`), and so does every settings-changed event (`:289`). That method toggles `BootReceiver` and `SmsBroadcastReceiver` (`:358-361`). The receiver checks nothing else: unlike the SMS receiver (CNTR-MODERNIZATION-004 `:245`: "login information is set and the first backup has run"), it does not check for a configured account or a completed first backup. `scheduleBootup()` itself checks only the encryption-degraded state (`WorkManagerScheduler.kt:263-266`) and auto backup (`:267-271`; off means `cancelAll()` and `null`, reachable only if the component state and the preference disagree).
   - **Delivery (platform behaviour; not read in this session).** `BOOT_COMPLETED` is exempt from the Android 8 implicit-broadcast limits and is delivered to manifest receivers on every version the owner uses (Android 11 and later). Because the app is not direct-boot aware, it arrives after the first unlock. It is not delivered to an app in the stopped state (never launched since install, or force-stopped). It is **not** sent on an app update; `MY_PACKAGE_REPLACED` is (`PackageReplacedReceiver`, manifest `:149-155`).
2. **What the boot job is.** `scheduleBootup()` builds a **one-time** `BackupWorker` request with the regular network constraint, exponential 30 s backoff, a 60 s initial delay and the tag `REGULAR` (`WorkManagerScheduler.kt:273-278`). It enqueues it with `enqueueUniqueWork("REGULAR", ExistingWorkPolicy.REPLACE, request)` (`:280-281`) and logs "enqueued bootup REGULAR with 60s delay" (`:283-284`). When it runs, `BackupWorker` infers the type from the tag (`BackupWorker.kt:501-507`), so it is a backup of type `REGULAR` (`:142-144`). It backs up every enabled data type above the watermark (`:151`, `:489`), like any other backup.
3. **`REPLACE` deletes the periodic regular backup.** `scheduleRegular()` registers the periodic work under the **same** unique name: `enqueueUniquePeriodicWork("REGULAR", ExistingPeriodicWorkPolicy.UPDATE, …)` (`:192-197`).
   - **2.9.1, read (bytecode):** for a policy other than `KEEP`/`APPEND`/`APPEND_OR_REPLACE`, `EnqueueRunnable.enqueueWorkWithPrerequisites` calls `CancelWorkRunnable.forName(name, …)` and then `WorkSpecDao.delete(id)` for every entry under the name, whatever its type or state. It then inserts the new request.
   - **2.12.0, read (sources):** the same, at `androidx/work/impl/utils/EnqueueRunnable.kt:235-250` ("Cancel all of these workers" … "delete all the database records"). `KEEP` keeps only an `ENQUEUED` or `RUNNING` entry (`:224-233`).
   - So at every boot, the periodic `REGULAR` `WorkSpec` is cancelled and deleted. The content trigger (`CONTENT_TRIGGER`) is not touched.
4. **Nothing re-registers it after the boot job.**
   - `BackupWorker` has no scheduler call (the only scheduler references are the backoff constants, `BackupWorker.kt:130-133`).
   - `App.onCreate()` does **not** call `rescheduleJobs()` on a process start. It only installs a collector that calls it on `SyncEvent.AutoBackupSettingsChanged` (`App.java:281-293`). BUG-025's analyst found the same.
   - `rescheduleJobs()` (`App.java:373-385`) is the only production caller of `scheduleRegular()` (`:377`). It runs only when one of these emits the event:
     - the auto-backup switch (`activity/fragments/MainSettings.java:71`);
     - incoming schedule, regular schedule, Wi-Fi only and "use old scheduler" (`AutoBackupSettings.java:19-24`);
     - SMS or MMS backup enabled (`AdvancedSettings.java:172`);
     - call log "after call" (`AdvancedSettings.java:293`);
     - the call-log permission grant (`AdvancedSettings.java:201-205`);
     - a package replace (`receiver/PackageReplacedReceiver.java:20-29`; `tryEmitEvent` can return `false`, `:24-28`).

     All listeners go through `SMSBackupPreferenceFragment.addPreferenceListener` (`:31-56`).
5. **Even then, `UPDATE` fails while the boot entry exists.**
   - **2.9.1, read (bytecode, `WorkerUpdater.enqueueUniquelyNamedPeriodic`):** `WorkManagerImpl.enqueueUniquePeriodicWork` sends `UPDATE` to this function. On the serial task executor, it checks in this order:
     - more than one entry under the name: fail, "Can't apply UPDATE policy to the chains of work.";
     - no entry: `enqueueNew` (a continuation with `ExistingWorkPolicy.KEEP`);
     - `WorkSpec` missing: fail;
     - `!isPeriodic`: fail, "Can't update OneTimeWorker to Periodic Worker. Update operation must preserve worker's type.";
     - state `CANCELLED`: delete it, then `enqueueNew`;
     - otherwise, update in place.
   - **2.12.0, read:** the same order at `androidx/work/impl/WorkerUpdater.kt:141-166` (type check `:156-160`, before the cancelled case `:162-166`). `updateWorkImpl` repeats the type check (`:51-57`). It keeps `lastEnqueueTime` and `periodCount` (`:69-80`), and returns `APPLIED_FOR_NEXT_RUN` without cancelling a running worker (`:58`, `:105`).
   - So `UPDATE` fails on a one-time entry under `REGULAR` in **every** state: `ENQUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED` and `CANCELLED`. When the name is absent, it registers. `rescheduleJobs()`'s `cancelAll()` (`:374`) does not help, because cancelling a one-time entry leaves it one-time. The failure is set on the returned `Operation`, which `scheduleRegular()` ignores (`WorkManagerScheduler.kt:192-197`). Nothing is thrown, and the log line "enqueued REGULAR with interval=…" (`:199-200`) is printed anyway.
6. **How long the window lasts.** WorkManager prunes finished work when its database is opened.
   - **2.9.1, read:** `CleanupCallback.onOpen` runs `DELETE FROM workspec WHERE state IN (2, 3, 5) AND (last_enqueue_time + minimum_retention_duration) < now − PRUNE_THRESHOLD_MILLIS`, provided no unfinished work depends on it. `WorkDatabaseKt.PRUNE_THRESHOLD_MILLIS = TimeUnit.DAYS.toMillis(1)`. States 2, 3 and 5 are `SUCCEEDED`, `FAILED` and `CANCELLED`.
   - **2.12.0, read:** the same, at `androidx/work/impl/WorkDatabase.kt:170-200`. `last_enqueue_time` is set at enqueue (`utils/EnqueueRunnable.kt:270`). The unique-name row cascades on delete (`model/WorkName.kt:25-38`).
   - The app sets no `keepResultsForAtLeast` and never calls `pruneWork()` (search of `app/src/main`), so `minimum_retention_duration` is 0.
   - **Window:** a finished or cancelled boot entry stops blocking `UPDATE` at the first opening of WorkManager's database at least 24 hours after the boot. That opening is a process start of the app. A boot entry that never finishes (still `ENQUEUED`, for example Wi-Fi only with no Wi-Fi) is never pruned until something cancels it.
   - So the regular backup returns only at the first `rescheduleJobs()` after that prune. If the phone is restarted again first, the cycle starts over.
7. **The legacy app did not lose the regular schedule at boot, though it used the same tag.**
   - `BackupJobs.scheduleBootup()` (`app/src/main/java/com/zegoggles/smssync/service/BackupJobs.java:86-97` at `d095fe02^`) had three branches:
     - auto backup off: `cancelAll()`;
     - the old scheduler (AlarmManager driver): `schedule(BOOT_BACKUP_DELAY, REGULAR, false)` (`:91-92`, `BOOT_BACKUP_DELAY = 60` at `:56`);
     - otherwise, the default Google Play driver: **nothing** ("everything else should be persistent by GCM", `:93-95`).
   - The boot job had the **same** tag `REGULAR` (`setTag(backupType.name())`, `:190`) and replace-current (`:188`). But it **was** the regular job: recurring, because `REGULAR.isRecurring()` (`BackupType.java:41`), with lifetime `FOREVER` (`BackupJobs.java:162-165`).
   - After every `REGULAR` run on the old scheduler, `SmsBackupService.scheduleNextBackup()` called `scheduleRegular()` again with the configured interval (`SmsBackupService.java:223`, `:275-287` at `d095fe02^`).
   - So on both legacy paths the regular schedule survived a reboot. The default path left the persisted job alone. The old-scheduler path restarted the regular job with a first run 60 s after boot, and it re-armed itself at the configured interval. The old scheduler was needed for that, because alarms do not survive a reboot.
   - **Correction to the finding as raised:** the legacy boot job was **not** under a separate tag. The schedule survived because the boot job was the recurring regular job and re-armed itself, not because the names differed.
   - The migration took the old-scheduler branch alone, made it unconditional and one-time, and kept the name. It dropped the re-arm, because a WorkManager periodic does not need one, and the default path's "do nothing".
8. **What the contract and design say.**
   - CNTR-MODERNIZATION-004 (v3, approved):
     - `scheduleBootup()`: "if !isAutoBackupEnabled() -> cancelAll(); else schedule regular. boot delay = BOOT_BACKUP_DELAY = 60s. (BackupJobs.java:86, :56,:92)" (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md:69-71`). It cites only the old-scheduler line `:92`, not the default branch `:93-95`.
     - Validation Rule 4: "else schedule regular with the 60s boot delay" (`:276`).
     - Unique names are given "per kind: REGULAR / INCOMING / BROADCAST_INTENT / MANUAL / SKIP + the CONTENT_TRIGGER_TAG tag + a restore unique name" (`:144-147`), and `SchedulerConfig.uniqueName` is "per BackupType" (`:168`). No unique name is assigned to the boot job, and nothing addresses a collision with the periodic work.
     - INV-1: "at most one non-terminal work item per unique name"; one-off `REPLACE`, periodic `REPLACE/UPDATE` (`:240`).
     - Error Handling: a non-terminal periodic request "is re-executed by WorkManager after process death/reboot" (`:286`).
     - Breaking-change policy: "different `ExistingPolicy`" is a behavioural break (`:266`).
   - DES-MODERNIZATION-005:
     - It lists `OneTimeWorkRequest` for "incoming, bootup, immediate, restore" (`sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md:121`).
     - `scheduleBootup()` is "per-branch, mirrors `BackupJobs.scheduleBootup`" (`:437`). The code does not mirror the branches: it has no "persisted, do nothing" branch.
     - "WorkManager work survives reboot by default; one-off `UNTIL_NEXT_BOOT` semantics are emulated by enqueueing at boot" (`:287`).
     - The unique name is per `BackupType` (`:455`), and the boot delay 60 (`:457`).
   - **So the one-time shape is specified by DES `:121`. The name `REGULAR` is the adapter's reading of "unique name per `BackupType`"** (also the class comment, `WorkManagerScheduler.kt:67-69`). No document says the boot job may replace the periodic one, and INV-1's intent (single flight, not deletion of a different job kind) does not require it.
9. **What the tests assert.** `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt` has 25 `@Test` at `0d490997`.
   - `ac5_scheduleBootup_withAutoBackupEnabled_isEnqueued` (`:427-438`) calls only `scheduleBootup()`. It asserts that the `REGULAR` unique work is non-empty and not `FAILED` or `CANCELLED`. It **pins the name `REGULAR`** for the boot work, but not its type or policy.
   - `ac5_scheduleBootup_autoBackupDisabled_returnsNull` (`:444-451`) and `se002_scheduleBootup_encryptionDegraded_returnsNullNoWork` (`:568-579`) test the gates.
   - `BootReceiverTest` (`app/src/test/java/com/zegoggles/smssync/receiver/BootReceiverTest.java:32-36`) verifies only that the receiver calls `scheduleBootup()`.
   - **No test registers the periodic work and then the boot work**, and none looks at what remains. None calls `scheduleRegular()` while a one-time entry exists under `REGULAR`. Nothing pins `REPLACE` or the one-time type of the boot work as intended behaviour.

**Who is affected.** Every user with auto backup on, after every restart that delivers `BOOT_COMPLETED`, including the owner. The default regular interval is two hours.

## Steps to Reproduce

Nothing was run for this artifact (no Gradle, no emulator). Both procedures are described, not observed.

**A. Unit level (the existing harness, `WorkManagerSchedulerTest.kt:42-81`: Robolectric, `WorkManagerTestInitHelper` with the default task executor replaced by `SynchronousExecutor`, read in the work-testing 2.9.1 bytecode).**
1. `scheduler.scheduleRegular()` (stub 3600). `getWorkInfosForUniqueWork("REGULAR")` holds one `ENQUEUED` entry with a non-null `periodicityInfo` (`WorkInfo.getPeriodicityInfo()`, read in 2.9.1). Record its `id`.
2. `scheduler.scheduleBootup()`.
3. Observe on today's code: one entry under `REGULAR`, with a **different** `id`, `periodicityInfo == null` and `initialDelayMillis == 60000` (`WorkInfo.getInitialDelayMillis()`, read in 2.9.1). The periodic entry is gone.
4. `scheduler.cancelAll()`, then `scheduler.scheduleRegular()` (what `rescheduleJobs()` does). Observe: the `Operation` returned inside `scheduleRegular()` fails. The test cannot see it, so it asserts the outcome instead. The entry under `REGULAR` is still the one-time entry (now `CANCELLED`), and there is no periodic entry.

**B. Device (debug build, application id `com.zegoggles.smssync`, `app/build.gradle:23`; auto backup on, account set, first backup done). Run by the orchestrator on the emulator.** Run `adb` with `MSYS_NO_PATHCONV=1` only where a `/sdcard` path is used; never with Gradle.
1. Register the regular backup: in Auto backup settings, change "Regular schedule" (for example 2 h, then 1 h, then 2 h). Log: `adb logcat -d | grep "WorkManagerScheduler.scheduleRegular"` shows "enqueued REGULAR with interval=…".
2. Before the restart, record WorkManager's view:
   - `adb shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS -p com.zegoggles.smssync`. The `DiagnosticsReceiver` is in the 2.9.1 manifest, guarded by `android.permission.DUMP`, which the shell holds (read). Its worker prints "Recently completed work", "Running work" and "Enqueued work" (read in the bytecode).
   - Then `adb logcat -d | grep -A40 "WM-Diagnostics"`. The tag prefix is not verified, so search for "Enqueued work" if it does not match.
   - Expected: a `BackupWorker` entry with unique name `REGULAR`, and the `BackupTriggerWorker` entry `CONTENT_TRIGGER`.
   - Also: `adb shell dumpsys jobscheduler | grep -B2 -A12 "com.zegoggles.smssync"`, to count the app's `SystemJobService` jobs. It does not show unique names.
3. Optional, definitive (debuggable build): copy WorkManager's database with `adb exec-out run-as com.zegoggles.smssync cat databases/androidx.work.workdb > workdb`, and the same for `-wal` and `-shm`. Read it with any SQLite reader outside the repository: `SELECT n.name, s.id, s.state, s.interval_duration, s.initial_delay, s.last_enqueue_time FROM workname n JOIN workspec s ON s.id = n.work_spec_id WHERE n.name IN ('REGULAR','CONTENT_TRIGGER');`. `interval_duration > 0` means periodic.
4. Restart: `adb reboot`, `adb wait-for-device`, then poll `adb shell getprop sys.boot_completed` until it prints `1`. Unlock the screen if a lock is set.
5. `adb logcat -d | grep -E "bootup|WorkManagerScheduler.scheduleBootup"` shows "bootup" (`BootReceiver.java:32`) and "enqueued bootup REGULAR with 60s delay".
6. Repeat step 2 or 3. Expected on today's code: under `REGULAR` only a one-time entry (`interval_duration = 0`, `initial_delay = 60000`); no periodic entry. `CONTENT_TRIGGER` is unchanged.
7. Wait two minutes with network. Logcat shows "BackupWorker.doWork: starting backup, type=REGULAR" (`BackupWorker.kt:144`). Afterwards the `REGULAR` entry is `SUCCEEDED`, and there is still no periodic entry.
8. Change "Regular schedule" again. Logcat prints "enqueued REGULAR with interval=…", but step 2 or 3 still shows no periodic `REGULAR`. This is the silent `UPDATE` failure. Whether WorkManager logs the failure is not verified.

## Expected Behavior

A restart never removes the regular backup. After every boot with auto backup on, exactly one periodic regular backup is registered under `REGULAR`, at the configured interval and with the regular constraints. If none was registered, for example on a phone already affected by this bug, the boot registers it, with its first run about 60 s after boot. A regular backup that is already registered keeps its schedule and is not cancelled if it is running. `scheduleRegular()` registers the periodic backup whatever single entry is under `REGULAR`, including a one-time entry left by an installed build. At most one `REGULAR`-type backup is pending or running at any time (INV-1). The incoming path is untouched.

## Actual Behavior

At every boot, the periodic regular backup is deleted and replaced by one one-time backup 60 s later. After that backup runs, no regular backup exists. Settings changes and app updates fail silently to restore it until WorkManager prunes the finished entry, at the first app start at least 24 hours after the boot. Only then does the next such event restore it. If the owner restarts the phone, say, every night, the regular backup never comes back.

Meanwhile, content-triggered and SMS-receiver backups (`INCOMING`) still run. Each backs up every enabled type above the watermark, so nothing is lost or duplicated, only delayed:
- an SMS-free stretch delays the backup of MMS (`content://mms` is not among the trigger's URIs: `WorkManagerScheduler.kt:224-230`, `Consts.java:29`, `:33`; whether the provider also notifies `content://sms` on an MMS insert is not verified);
- likewise call-log entries when "after call" is off;
- and anything else, while BUG-025's quota holds incoming backups.

## Evidence

**Observed on a device, 2026-11-21 (added by the orchestrator; decision appendix, Record 21, ruling 62).** The restart procedure was run twice on the Android 16 emulator `Pixel_9_Pro` (debug build of story U-068, auto backup on, regular schedule at its default, no setting changed). After each restart the app logged `bootup` and `WorkManagerScheduler.scheduleBootup: enqueued bootup REGULAR with 60s delay`; the system's scheduler then held two jobs for the app, the content trigger and one one-time job, and no periodic job; after the boot backup had ended, WorkManager's diagnostics listed the content trigger as the only enqueued work. Record: `sdlc/artifacts/build/research/2026-11-21-background-backup-and-boot-observation.md`. The same runs showed that the boot backup itself fails when it starts off screen; that is BUG-029 and is fixed before this bug's story.


| Evidence | What it shows |
|---|---|
| `receiver/BootReceiver.java:24`, `:31-34`; `AndroidManifest.xml:62`, `:141-147` | Boot path; the only caller |
| `App.java:258`, `:289`, `:358-361` | The boot receiver is enabled only while auto backup is on |
| `scheduler/WorkManagerScheduler.kt:262-286` (`:273-281`) | One-time, tag and name `REGULAR`, `ExistingWorkPolicy.REPLACE`, 60 s |
| `scheduler/WorkManagerScheduler.kt:170-202` (`:192-197`) | The periodic regular backup under the same name, `UPDATE`, result ignored |
| `App.java:281-293`, `:373-385`; the emitters in Description item 4 | Re-registration only on settings or package-replace events |
| `service/BackupWorker.kt:130-133`, `:142-151`, `:501-507` | No reschedule after a run; the boot run is type `REGULAR` |
| WorkManager 2.9.1 bytecode (read): `EnqueueRunnable`, `WorkerUpdater.enqueueUniquelyNamedPeriodic`, `WorkManagerImpl.enqueueUniquePeriodicWork`, `CleanupCallback`, `WorkDatabaseKt.PRUNE_THRESHOLD_MILLIS`, `ExistingPeriodicWorkPolicy` (has `CANCEL_AND_REENQUEUE`), `WorkInfo` accessors, manifest receivers | `REPLACE` deletes; `UPDATE` fails on a one-time entry in any state; prune after one day |
| WorkManager 2.12.0 sources (read): `utils/EnqueueRunnable.kt:224-250`, `:270`; `WorkerUpdater.kt:45-106`, `:125-176`; `WorkDatabase.kt:170-200`; `model/WorkName.kt:25-38` | Same behaviour in the version U-073 moves to |
| `BackupJobs.java:56`, `:86-97`, `:160-166`, `:186-193`; `BackupType.java:41`; `SmsBackupService.java:206-223`, `:275-287` (all `d095fe02^`) | Legacy: same tag, but the boot job was the recurring regular job and re-armed itself; the default path did nothing |
| CNTR-MODERNIZATION-004 `:69-71`, `:144-147`, `:168`, `:240`, `:266`, `:276`, `:286`; DES-MODERNIZATION-005 `:121`, `:287`, `:437`, `:455`, `:457` | One-time boot shape is in the design; the name is an adapter choice; no document allows the deletion |
| `WorkManagerSchedulerTest.kt:427-451`, `:568-579`; `BootReceiverTest.java:32-36` | No test of periodic-then-boot; the boot test pins only the name |

## Acceptance Criteria

Each criterion is checked by unit tests in `WorkManagerSchedulerTest.kt` with the existing harness (`:42-81`) unless it says otherwise. Each new test must fail on today's code or on a recorded mutation (AC-10). "Entry" means an item of `getWorkInfosForUniqueWork("REGULAR").get()`. "Periodic" means `periodicityInfo != null`.

- [ ] AC-1 (a boot keeps the regular backup): After `scheduleRegular()` (3600), `scheduleBootup()` leaves exactly one entry, periodic, `ENQUEUED`, with a repeat interval of 3 600 000 ms and the **same `id`** as before the boot call. Checked by a test that fails today (the entry is one-time with a new id).
- [ ] AC-2 (a boot with nothing registered registers it): With no entry, `scheduleBootup()` leaves exactly one periodic `ENQUEUED` entry with the configured interval, `initialDelayMillis == 60000`, the regular network constraint (INV-2: `UNMETERED` when Wi-Fi only, else `CONNECTED`) and the tag `REGULAR`. Checked by a test (two stubs of `isWifiOnly`).
- [ ] AC-3 (an installed build's one-time entry does not block registration): Set up today's boot request in the test: a one-time `BackupWorker` with tag `REGULAR`, via `enqueueUniqueWork("REGULAR", REPLACE, …)`. Then, for each of these states: (a) `ENQUEUED`; (b) `CANCELLED` (after `scheduler.cancelAll()`, as `rescheduleJobs()` does); (c) `SUCCEEDED`, if the planner can drive it with `TestDriver` and a worker the harness factory builds, otherwise recorded as not tested; each of `scheduleRegular()` and `scheduleBootup()` leaves exactly one periodic `ENQUEUED` entry and no one-time entry. Checked by tests that fail today.
- [ ] AC-4 (single flight, INV-1): After any sequence of `scheduleRegular()`, `scheduleBootup()`, and `cancelAll()` followed by `scheduleRegular()` (three sequences in the test), `getWorkInfosByTag("REGULAR")` holds at most one non-terminal item. No unique name other than `REGULAR` holds a `REGULAR`-tagged item. Checked by a test.
- [ ] AC-5 (a boot does not cancel or reset a registered regular backup): The boot path updates in place. AC-1's unchanged `id` and a `generation` raised by exactly one show this was an `UPDATE`, not a cancel-and-re-enqueue. The fallback to re-enqueue happens only when the `UPDATE` operation fails. Checked by AC-1's test (asserting `generation` too) and by reading the code.
- [ ] AC-6 (gates unchanged): Encryption degraded: `scheduleBootup()` and `scheduleRegular()` return `null` and neither enqueue nor cancel anything; after a registered periodic, a degraded `scheduleBootup()` leaves it as it was. Auto backup off: `scheduleBootup()` calls `cancelAll()` and returns `null`. The three existing boot tests (`:427-451`, `:568-579`) and every existing `scheduleRegular()` test pass **with no edit**. Checked by one new test (degraded after registered) and the existing tests.
- [ ] AC-7 (nothing about the incoming path changes): `scheduleBootup()` leaves the `CONTENT_TRIGGER` entry with the same `id` and state. `scheduleIncoming()`, `scheduleContentTrigger()`, `scheduleImmediate()`, `scheduleManual()`, `scheduleRestore()`, `cancel()`, `cancelAll()` and `cancelRegular()` show no diff. `BootReceiver.java`, `App.java`, `PackageReplacedReceiver.java`, `SmsBroadcastReceiver.java`, `worker/BackupTriggerWorker.kt`, `BackupWorker.kt` and `AndroidManifest.xml` show empty diffs. Checked by one test and `git diff`.
- [ ] AC-8 (contract, design and code agree): Done by a change record (Suggested Approach, "Contract"). CNTR-MODERNIZATION-004 (`:69-71`, `:144-147`, `:240`, `:276`) and DES-MODERNIZATION-005 (`:121`, `:287`, `:437`) then state three things. The boot path registers the regular periodic backup under `REGULAR` with `UPDATE` and the 60 s boot delay as the initial delay of a new registration. It never enqueues a one-time `REGULAR` work. `scheduleRegular()` and the boot path fall back to `CANCEL_AND_REENQUEUE` when `UPDATE` fails. The KDoc of `scheduleBootup()` (`WorkManagerScheduler.kt:252-261`) and the class comment's INV-1 lines (`:40-42`, `:67-69`) say the same. Checked by reading those places against the code.
- [ ] AC-9 (device): The orchestrator runs Steps to Reproduce B on the emulator with the fixed debug build. After the reboot (step 6), `REGULAR` is one periodic entry. After the next regular or incoming run, it is still periodic. A settings change leaves it periodic. An install over a build that has a one-time `REGULAR` entry (step 7 state of today's build, then `adb install -r` of the fix) ends with one periodic entry. The diagnostics output or the database query is attached as evidence.
- [ ] AC-10 (gate and mutations): `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` exits 0 with 0 failures, the skipped count not higher, and no per-package floor lowered. The authoritative test count (`git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`) is taken before and after, and the difference is explained by the new tests. Three mutations are recorded:
  - (i) restore today's one-time `REPLACE` in the boot path: AC-1 and AC-4 fail;
  - (ii) remove the fallback: AC-3 fails;
  - (iii) use `CANCEL_AND_REENQUEUE` instead of `UPDATE` in the boot path: AC-1's `id` assertion fails.
- [ ] AC-11 (PO-1, "Never"): the criterion follows the owner's answer.
  - Recommended answer, "no boot backup with Never": with `regularTimeoutSecs` of −1 or 0, `scheduleBootup()` leaves no non-terminal entry under `REGULAR` and returns `null`.
  - Other answer: one one-time backup about 60 s after boot under its own unique name (`BOOT`), and no `REGULAR` entry.

  Checked by a test. This holds only if BUG-026's rule is in the same story; otherwise AC-11 moves to BUG-026.

### Integration Criteria (if fix modifies component boundaries)
- [ ] IC-1: The port keeps its signatures (`scheduler/BackupScheduler.java:68`, `:92`). Its callers, `BootReceiver.java:33` and `App.java:377`, are unchanged and compile. A search of `app/src` for `scheduleBootup` and `scheduleRegular` is attached and shows no new caller.
- [ ] IC-2: Persisted work keeps its identity. The unique name `REGULAR`, the worker class `BackupWorker` and the tag `REGULAR` are unchanged. An installed build's entries under `REGULAR`, periodic or one-time in any state, are taken over by AC-1 and AC-3. No new unique name is introduced unless PO-1 or PO-2 is answered "yes".

## Impact Analysis

Searches run (on `0d490997`, `git grep`): `scheduleBootup`, `scheduleRegular`, `rescheduleJobs`, `AutoBackupSettingsChanged`, `addPreferenceListener(`, `BOOT_COMPLETED`, `pruneWork`, `keepResultsForAtLeast`, `getWorkInfosForUniqueWork` in `app/src`. In the contract and DES-MODERNIZATION-005: `boot`, `ootup`, `unique name`, `REGULAR`, `persist`. In the sprint stories: `scheduleBootup`.

| # | Layer | Affected? | Files and lines | Action needed |
|---|-------|-----------|-----------------|---------------|
| 1 | Requirements | No change | `modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md:30` ("preserving all current scheduling behavior") | None. The fix restores the legacy behaviour (the regular schedule survives a reboot) |
| 2 | Design and contracts | Yes | CNTR-MODERNIZATION-004 INV-1 `:240`, `:69-71`, `:144-147`, `:276`; DES-MODERNIZATION-005 `:121`, `:287`, `:437` | AC-8, by change record |
| 3 | Stories, sprints, change records | References | BUG-025 (Description and AC-4 Option S rely on the regular backup as safety net; its "v4" number); BUG-026 (same function family; its Notes raised this); U-069 AC-13 (`sdlc/artifacts/build/sprints/sprint-024/U-069-content-trigger-backup-goes-through-the-scheduler.md:103`, `:138`: `scheduleBootup` unchanged); U-073 (existing scheduler tests unedited across WorkManager 2.12.0) | Sequencing, below |
| 4 | Application code | Yes (root) | `scheduler/WorkManagerScheduler.kt:252-286`, `:170-202`, comments `:40-42`, `:67-69` | The fix |
| 5 | Vendored library | No | None | None |
| 6 | Tests | Yes | `service/WorkManagerSchedulerTest.kt` (new tests only) | AC-1 to AC-7, AC-10, AC-11 |
| 7 | Documentation | No | README describes the regular schedule only as an interval (BUG-026, Impact row 7) | None |

**Cascade.** The root is the code: a one-time `REPLACE` under the periodic work's name. The design's `:121` (boot is a one-time request) and `:437` ("mirrors" the legacy branches, which the code does not) are the source of the shape. The contract is silent on the collision. Fix order: change record, then code and tests in one story.

## Suggested Approach

Written by the Solution Architect on 2026-11-21. Advisory until story planning.

**Fix (in `WorkManagerScheduler.kt` only).** Give the regular periodic registration one private helper, `enqueueRegular(initialDelaySecs: Long)`, used by both `scheduleRegular()` (delay 0) and `scheduleBootup()` (delay `BOOT_BACKUP_DELAY_SECS`).

1. `scheduleBootup()` keeps its two gates unchanged (degraded: `null`; auto backup off: `cancelAll()`, `null`). If BUG-026 rides along, it also gets BUG-026's `regularTimeoutSecs <= 0` rule (PO-1). It then returns `enqueueRegular(60)`. It **no longer builds a `OneTimeWorkRequest`**.
2. `enqueueRegular(d)` builds today's `scheduleRegular()` request unchanged: `PeriodicWorkRequest` of `BackupWorker`, the interval as today, `networkConstraints(REGULAR)`, `EXPONENTIAL` 30 s, tag `REGULAR`. It adds `setInitialDelay(d, SECONDS)` when `d > 0`, and calls `enqueueUniquePeriodicWork("REGULAR", ExistingPeriodicWorkPolicy.UPDATE, request)`.
3. **Robustness against a one-time entry under the name.** On the returned `Operation`, add a listener on `operation.result`, with a direct executor and no blocking. If the future fails (any `ExecutionException`; the expected cause is the `UnsupportedOperationException` of Description item 5), it logs a warning and enqueues a freshly built identical request with `ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE`. That policy is present in 2.9.1 (read), and is the non-deprecated name for periodic `REPLACE` in 2.12.0 (`ExistingPeriodicWorkPolicy.kt:29-38`, read). It deletes every entry under the name, one-time or not, and inserts the periodic request (Description item 3).
4. Log lines: "scheduleBootup: registered REGULAR (boot delay 60s, interval=…)" and "…: UPDATE failed (…); re-registered REGULAR with CANCEL_AND_REENQUEUE". The KDoc of `scheduleBootup()` and the class comment are corrected (AC-8).

**Why this shape.**
- **It is the legacy behaviour translated, not a new one.** The legacy old-scheduler path restarted the recurring regular job with a first run 60 s after boot (Description item 7). A periodic `REGULAR` request with a 60 s initial delay is that, in WorkManager terms. The default path relied on persistence, which WorkManager gives (contract `:286`, DES `:287`). `UPDATE` keeps a persisted registration as it is.
- **Single flight by construction.** There is only ever one `WorkSpec` under `REGULAR`. WorkManager does not run one `WorkSpec` twice at once, and `UPDATE` does not cancel a running worker (`WorkerUpdater.kt:58`, `:105`, 2.12.0 read). No second `REGULAR`-type work exists under any other name (AC-4).
- **Self-healing.** Phones already affected, with no periodic entry, get one at the next boot (absent name: `enqueueNew`).
- **No existing test changes.** The boot work stays under `REGULAR`, so `:427-438` and `:568-579` still hold (AC-6), which matters for U-073's "unedited" rule.

**What changes for the user.** With a registered regular backup, a restart no longer adds an extra backup 60 s after boot. `UPDATE` keeps `lastEnqueueTime` and `periodCount` (`WorkerUpdater.kt:69-80`), so the initial delay of the update has no effect after the first period. The periodic backup runs at its own due time, as soon as constraints allow if it fell due while the phone was off. WorkManager computes this; read only as far as the preserved fields, the run-time formula not read. The extra run after boot happens only when the registration is new. Whether the owner wants an unconditional backup after each boot is PO-2.

**Options considered and rejected.**
- *Boot backup under its own unique name (for example `BOOT`, `REPLACE`).* The periodic work survives. But a second `REGULAR`-type `BackupWorker` can run next to the periodic one: when the periodic is overdue at boot, it starts as soon as WorkManager reschedules, and the boot run follows 60 s later. There is no cross-run guard in `BackupWorker` (search for mutex, lock or running guards found none), so the two could upload the same items twice. It also breaks `:427-438` (empty `REGULAR`), and it does not heal an affected phone, because nothing re-registers the periodic work at boot. Rejected unless PO-2 is "yes"; even then, only together with step 3 and a guard against overlap.
- *Boot path calls `scheduleRegular()` and also enqueues a separate one-time backup.* The same overlap. Rejected.
- *Periodic `REPLACE`/`CANCEL_AND_REENQUEUE` at boot.* This is robust against one-time entries, but it cancels a running regular backup if `BOOT_COMPLETED` is processed after WorkManager has started the overdue run. That reports a cancelled run (BUG-024's area) and resets the period at every boot. It is used only as the fallback, when `UPDATE` has already failed, which means a one-time entry from an installed build is in the way.
- *Switch `scheduleRegular()` to `REPLACE` outright.* For its only caller this changes nothing, because `rescheduleJobs()` cancels first. But it conflicts with BUG-026 AC-4 ("`UPDATE` … as before"), and it would cancel a running backup for any future caller. Rejected in favour of the fallback.
- *Rely on pruning, or call `pruneWork()`.* Pruning takes at least 24 h and never removes an `ENQUEUED` entry. `pruneWork()` removes finished work of **all** names, including the `MANUAL`/`SKIP` and restore results that `MainViewModel` observes (`activity/MainViewModel.kt:146`, `:184`, `:198-201`). Rejected.
- *Blocking query of the existing entry before choosing the policy.* `scheduleBootup()` runs on the receiver's main thread. Rejected for the non-blocking listener.

**Upgrade (installed build's database).** An installed build can leave four states under `REGULAR`. The upgrade's `MY_PACKAGE_REPLACED` runs `rescheduleJobs()`: `cancelAll()`, then `scheduleRegular()`.
- A periodic entry (no reboot since the last registration): updated in place.
- A pending one-time boot entry: cancelled by `cancelAll()`, then the type check fails, then the fallback.
- A finished one-time boot entry: the type check fails, then the fallback.
- Nothing (pruned): `enqueueNew`.

All four end with one periodic entry. If the package-replace event is lost (`tryEmitEvent` returns `false`), the next boot or settings change heals it by the same path. Tested as AC-3; on the device as AC-9.

**Contract (change record).** It amends CNTR-MODERNIZATION-004:
- `scheduleBootup()` text `:69-71`: "registers the regular periodic backup under `REGULAR` (`UPDATE`); the 60 s boot delay is the initial delay of a new registration; never a one-time `REGULAR` work";
- Validation Rule 4 `:276`, with the same meaning;
- the unique-name note `:144-147`: the boot path shares `REGULAR` with the periodic backup because it is that backup;
- INV-1's realization `:240`: periodic `UPDATE`, with `CANCEL_AND_REENQUEUE` as the fallback when `UPDATE` fails.

It amends DES-MODERNIZATION-005 `:121` (bootup is no longer a `OneTimeWorkRequest`), `:287` and `:437`. Add a risk row: "a one-time entry under a periodic name blocks `UPDATE`".
- **Ruling: a behavioural break** under the contract's own policy (`:266`: "different `ExistingPolicy`"). The boot path changes from one-off `REPLACE` to periodic `UPDATE`, and the meaning of Validation Rule 4 changes. It is **not breaking for consumers**: no signature, `BackupType` value or `SchedulerState` variant changes.
- **Version step.** Next free version, approved before the code. As sequenced below, it is written together with BUG-026's record and should **share one record** with it: same methods, same story, same test file. BUG-026's part is non-breaking and this part is behaviourally breaking, so the shared record is breaking. That record is then **v4**, and BUG-025's planned record, which its text calls "v4", becomes **v5** when written. Its number is "the next free version", and its substance does not change. The change-record id is the next free `CR-` number at the time (CR-016 is the highest today).

**Tests (all in `WorkManagerSchedulerTest.kt`, new methods only):** AC-1 to AC-7 and AC-11 as written above.
- Read `id`, `generation`, `periodicityInfo`, `initialDelayMillis` and `constraints` from `WorkInfo`. All five accessors exist in 2.9.1 (read); in 2.12.0 they are expected unchanged but were not re-read.
- AC-3's one-time set-up builds today's request inside the test, so the test does not depend on the old code.

**Where it goes.** It **rides with BUG-026's story**, as one story and one change record.
- Same methods: `scheduleRegular()`, `scheduleBootup()` and the shared helper.
- Same test file and harness. The two rules interact (PO-1: "Never" and the boot path). Their mutations and the gate are run once.
- BUG-026 needs no owner decision. This bug needs only PO-1 and PO-2, both of which have a safe default (the recommendations). The story may start on the defaults if the owner has not answered, and record that.
- **Place:** as already recommended for BUG-026, after U-072 and before the BUG-025 fix and U-073.
  - U-069 is being built now. It edits only `worker/BackupTriggerWorker.kt` and one JaCoCo line, and its AC-13 requires `scheduleBootup`, `scheduleRegular` and the test file to be left unedited (U-069 story `:103`, `:138`).
  - U-071 and U-072 edit no scheduler code. U-071's plan is fixed against a start state.
  - U-073 (WorkManager 2.12.0) must then carry these tests unedited, which guards the fix across the library move. The library behaviour it relies on was read in 2.12.0 too.
  - BUG-025's fix, which edits the same file and test file, follows, and can then honestly call the regular backup a safety net after a reboot.
- **Priority argument for earlier.** If the orchestrator wants it sooner, the slot right after U-069 also works, at the price BUG-026 names: U-071 re-reads its start state (test count, scheduler file). It must not run concurrently with U-069 (shared test file, U-069 AC-13 (e)).

**Risks.**
- The fallback runs asynchronously after the failed `UPDATE`. A `scheduleRegular()` or `cancelAll()` queued between them executes first. A `cancelAll()` overtaken by the fallback would leave a periodic entry after auto backup was switched off. To close this, the fallback re-checks `isAutoBackupEnabled`, the degraded state and (with BUG-026) the interval before enqueuing. This is a test case for the planner.
- The listener must not capture an Activity context. The scheduler holds the application context (`WorkManagerScheduler.kt:80`).
- A failed `UPDATE` for another reason (a chain under the name, impossible with this code) also takes the fallback, which is the desired outcome.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt` (`:252-286`, `:170-202`, `:40-42`, `:67-69`) | Boot: one-time `REPLACE` under `REGULAR` deletes the periodic work; `scheduleRegular()`'s `UPDATE` fails silently on a one-time entry | Shared `enqueueRegular(delay)`: periodic `UPDATE`, boot delay as initial delay, `CANCEL_AND_REENQUEUE` fallback on failure; comments corrected |
| `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt` | No periodic-then-boot test; no one-time-then-regular test | New tests (AC-1 to AC-7, AC-11); no existing test edited |
| `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` (`:69-71`, `:144-147`, `:240`, `:276`); `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` (`:121`, `:287`, `:437`) | Boot is a one-time request; the collision is not addressed | AC-8, by change record (shared with BUG-026) |

## Existing Behavior to Preserve

- The encryption-degraded gate on both methods: `null`, nothing enqueued, nothing cancelled (U-054).
- Auto backup off at boot: `cancelAll()` and `null` (Validation Rule 4, first half).
- The periodic request: interval per `regularTimeoutSecs` (with BUG-026's rules if they ride along), the network per Wi-Fi only (INV-2), `EXPONENTIAL` 30 s (INV-3), no charging constraint, tag and unique name `REGULAR`, `UPDATE` as the primary policy (INV-1).
- The content trigger (`CONTENT_TRIGGER`), the incoming backup, the immediate, manual and restore paths, `cancel()`, `cancelAll()`, `cancelRegular()`, `App.rescheduleJobs()`, `BootReceiver` and the manifest.
- The settings keys, values and summaries.

## Verification Steps

For QA, per criterion. Run Gradle in a shell without `MSYS_NO_PATHCONV`.

1. AC-1 to AC-7, AC-11: run the new tests and the whole `WorkManagerSchedulerTest`. `git diff` of the test file shows added methods only.
2. The new tests fail on the unfixed code: check out the unfixed `WorkManagerScheduler.kt` with the new tests kept. AC-1, AC-3 and AC-4 must fail. Record the run.
3. AC-5: read the boot path (`UPDATE` first; fallback only in the failure listener) and AC-1's `id` and `generation` assertions.
4. AC-7: `git diff --stat` over the files named; `git diff` of `WorkManagerScheduler.kt` limited to `scheduleRegular`, `scheduleBootup`, the helper and the comments.
5. AC-8: read the change record and the amended contract and design text against the code.
6. AC-9: the orchestrator's device run (Steps to Reproduce B with the fix), with the diagnostics output or database query before and after the reboot, after a run, after a settings change, and after `adb install -r` over today's build in the step 7 state.
7. AC-10: the gate command, the authoritative count before and after, and the three recorded mutations.
8. IC-1, IC-2: the search output; `BackupScheduler.java`, `BootReceiver.java` and `App.java` absent from the diff.

## Root Cause Analysis

Preliminary, from the code:
- **Mechanism.** U-014 translated `scheduleBootup()` from the legacy old-scheduler branch alone (`BackupJobs.java:91-92`), following DES-MODERNIZATION-005 `:121` ("`OneTimeWorkRequest` (… bootup …)"). It kept the per-type unique name `REGULAR` (DES `:455`). In the legacy code that name and replace-current were harmless: the boot job was the recurring regular job, and the service re-armed it after every run (`SmsBackupService.java:275-287`). U-017 deleted both the re-arm and the default path's "do nothing" (`BackupJobs.java:93-95`). The translated boot job became a one-time `REPLACE` that deletes the periodic work it shares a name with.
- **Why it was not caught.** The contract cites only `:92` for `scheduleBootup` (`:71`). The design says the adapter "mirrors" the legacy branches (`:437`) when it does not. The only boot test checks that something is enqueued under `REGULAR` (`WorkManagerSchedulerTest.kt:427-438`), which today's code satisfies by deleting the periodic work. And the `UPDATE` failure is invisible: the `Operation` is ignored and the log line is printed anyway.

## Technical Context

- **Severity: medium** (the scaffold's value, confirmed).
  - For an app whose only purpose is backup, a periodic backup that silently disappears after every restart is a completeness risk, and the settings still show the schedule.
  - Against that: nothing is lost or duplicated. Incoming-triggered backups (content trigger and SMS receiver) still run, and each backs up every enabled type above the watermark (`BackupWorker.kt:151`, `:489`). So items are delayed until the next incoming backup, not dropped.
  - The delay can be long for items the trigger does not see: MMS without SMS (not verified), and call log with "after call" off. It is unbounded while BUG-025's quota holds incoming backups.
  - On the project's scale (BUG-021 and BUG-023 medium; BUG-020, BUG-022, BUG-024 and BUG-026 low), a silent loss of a scheduled function with no data loss is medium.
  - Not high, because no data is lost and the owner can restore the schedule; it is lost again at the next reboot.
- **Priority: high.**
  - It removes the safety net BUG-025's text and its Option S rely on, so BUG-025 should not be fixed or worded as if the regular backup survived a reboot.
  - It affects the owner after every restart, at default settings.
  - The fix is small, needs no owner decision to start (PO-1 and PO-2 have safe defaults), and rides with BUG-026's already-planned story.
- **Complexity: medium** (kept). One method pair and a helper with an asynchronous fallback; a behaviourally breaking change record; an upgrade case with four database states; a device check.
- **Origin: `code-review`** (kept). Found while writing BUG-025's approach, which came from the plan review of U-069.
- **Relation to BUG-025.** BUG-025 is incoming backups late or held. This bug removes the regular backup that would otherwise bound that lateness. Fixing this first makes BUG-025's "regular backup is the safety net" true after a reboot, within the regular interval and subject to the same quota.
- **Relation to BUG-026.** Same function family (`scheduleRegular()` / `scheduleBootup()`), same test file, same contract lines. BUG-026's Notes raised this bug and its PO-1 question.
- **Library.** WorkManager 2.9.1 in the app; 2.12.0 planned by U-073. Every library behaviour this fix relies on (`REPLACE` deletes, `UPDATE` type check and order, `UPDATE` keeps a running worker and the period, prune after one day, `CANCEL_AND_REENQUEUE`) was read in both, except where "Not verified" says otherwise.

## Supporting Documentation

- `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` (`:43`, `:69-71`, `:108`, `:144-147`, `:168`, `:240`, `:245`, `:251`, `:266`, `:276`, `:286`)
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` (`:88`, `:90`, `:121`, `:278`, `:287`, `:435`, `:437`, `:455`, `:457`)
- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md` (`:30`)
- `sdlc/artifacts/stories/BUG-025-incoming-sms-backup-delayed-by-trigger-period-and-job-quota.md` (Suggested Approach, "Safety-net correction"; contract v4 text)
- `sdlc/artifacts/stories/BUG-026-regular-schedule-never-still-schedules-a-backup.md` (Notes, "Observations outside this bug"; Suggested Approach, "Where it goes")
- `sdlc/artifacts/build/sprints/sprint-024/U-069-content-trigger-backup-goes-through-the-scheduler.md` (`:103`, `:138`)
- `sdlc/artifacts/build/sprints/sprint-025/U-073-runtime-libraries-latest-stable.md` (WorkManager 2.12.0, increment W)

## Implementation Notes
<!-- Added by agents during build -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes

### For the product owner (decide; recommendation given)

- **PO-1: With "Regular schedule: Never", should a restart still start one backup?** Today it does (one-time, 60 s after boot). The legacy old-scheduler path did one too, and the legacy default path none (`BackupJobs.java:91-95`). **Recommendation: no.** "Never" means no regular backups; incoming backups still run. With the fix as written and BUG-026's rule in the same story, the boot path registers nothing under "Never" (AC-11). If the answer is yes, the boot path enqueues one one-time backup under its own name `BOOT` only when the regular schedule is "Never". It cannot overlap a periodic `REGULAR`, because none exists then.
- **PO-2: Should every restart start a backup about a minute after boot, even when the regular backup is not due?** Today it does, but only by deleting the regular schedule. **Recommendation: no.**
  - The legacy default path did not do it.
  - The regular backup runs at its own due time, immediately after boot if it fell due while the phone was off.
  - Incoming backups pick up new messages.
  - A separate boot backup can overlap the regular one.

  If yes, the story adds a one-time `BOOT` backup and a guard so that it never runs while a `REGULAR` run is pending or running. That raises complexity, and the planner designs the guard.

### Observations outside this bug (raised, not part of the acceptance criteria)

- **Backups of different types can run at the same time.** `INCOMING`, `REGULAR`, `BROADCAST_INTENT` and `MANUAL` have different unique names, and `BackupWorker` has no cross-run guard. Two runs started close together could upload the same items. This exists today and is unchanged by this fix. Not observed; whether IMAP de-duplicates is not verified. Worth its own look before BUG-025 makes incoming runs more frequent.
- **The scheduler ignores every `Operation` it gets back.** All seven enqueue calls discard their results. This bug is one consequence. A general "log failed operations" rule could be a small hardening story.

### Not verified

1. Nothing was run: no Gradle, no unit test, no device. Steps to Reproduce A and B are described, not observed. In particular, the reboot effect is **read from code and library, not observed on a device**.
2. Platform delivery of `BOOT_COMPLETED` (implicit-broadcast exemption, after first unlock, not in the stopped state, not on update): platform behaviour, not read in this session.
3. In 2.9.1: the full `updateWorkImpl` body (only its type-check message and `copy` call were seen), the assignment of `last_enqueue_time` at enqueue, and the `WorkName` cascade. These were read in 2.12.0 only.
4. When WorkManager's Room database is first opened in a process. Expected at WorkManager initialisation (`App.java:232-237`) through its start-up work, not traced. This sets when the one-day prune runs.
5. How WorkManager computes the next run of an updated periodic work whose `periodCount > 0` (the initial delay is expected to be ignored), and how an overdue periodic job is run after a reboot.
6. Whether WorkManager logs a failed `UPDATE` operation, and the logcat tag of the diagnostics worker.
7. That `TestDriver` can drive a one-time entry to `SUCCEEDED` with the harness factory (AC-3 (c)). The `ENQUEUED` and `CANCELLED` cases suffice to show the defect, because the type check comes first.
8. That the `ListenableFuture` listener runs synchronously under the harness's `SynchronousExecutor` (expected; the work-testing 2.9.1 helper installs `SynchronousExecutor` as the task executor when none is configured: read in the bytecode, branch not fully traced).
9. Whether the MMS provider notifies `content://sms` on an MMS insert. This decides whether MMS-only arrivals start an incoming backup.
10. `WorkInfo` accessors (`getInitialDelayMillis`, `getGeneration`, `getPeriodicityInfo`) in 2.12.0. Read in 2.9.1 only.
11. The owner's restart habits and devices: how often the owner's phones restart, so how long the regular backup is absent in practice.
