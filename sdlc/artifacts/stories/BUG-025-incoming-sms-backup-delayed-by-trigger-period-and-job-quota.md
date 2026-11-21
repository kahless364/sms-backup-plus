---
type: bug
status: ready
artifact_type: bug
severity: medium
priority: medium
complexity: high
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
  - BUG-023
  - BUG-021
  - BUG-024
  - U-069
  - U-068
  - U-014
  - U-017
  - CR-015
  - REQ-MODERNIZATION-016
  - BUG-026
  - U-073
platforms: []
tags:
  - workmanager
  - scheduler
  - content-trigger
  - incoming-backup
  - standby-bucket
  - job-quota
  - inv-4
  - product-owner-decision
id: BUG-025
title: A second incoming SMS within fifteen minutes does not start a backup on time, and the scheduler's quota can hold every incoming backup until the app is opened
domain: build
origin: code-review
updated_at: '2026-11-21T14:32:17.111Z'
---

# BUG-025: A second incoming SMS within fifteen minutes does not start a backup on time, and the scheduler's quota can hold every incoming backup until the app is opened

## Description

The app promises to back up an incoming text message a set time after it arrives. The setting is "Incoming schedule", default 1 minute, with the help text "When to backup incoming SMS (after arrival)" (`app/src/main/res/values/strings.xml:169-170`). On 2026-11-21 the orchestrator measured on an Android 16 emulator that two test messages sent six minutes apart were backed up about 18 and 12 minutes after they arrived, and only when the app was opened (`sdlc/artifacts/build/research/2026-11-21-incoming-backup-delay-measurement.md`, called "the record" below). Two separate mechanisms were seen. They are different defects with different causes; they add up on a device, so they are written down together.

**Mechanism 1: the content trigger fires at most once per fifteen-minute period.** Stage 1 of the incoming backup (CNTR-MODERNIZATION-004 INV-4) is a content-URI trigger over the SMS provider. The code registers it as a `PeriodicWorkRequest` with a fifteen-minute interval and the SMS URI as its only constraint. WorkManager runs a periodic request that way: the first run is immediate; after each run the next run is scheduled one interval later, as a scheduler job that carries that interval as a minimum latency. After the trigger has fired once, the next SMS is not seen until the period ends. The record shows the job with "Minimum latency: +14m59s993ms" after SMS A, and no trigger line for SMS B in the 9.5 minutes that were watched.

**Mechanism 2: the scheduler's quota.** The platform's job scheduler (JobScheduler) holds jobs back when the app has no quota in its current app standby bucket. In the record the app was in bucket 50. All three of its jobs (the trigger job once its latency had elapsed, the delayed `INCOMING` backup, and a second delayed job that has a connectivity requirement) were held with `WITHIN_QUOTA` unsatisfied for a quarter of an hour, and ran within a second of the app being opened (bucket then 10). This applies to any job-based path, including the regular backup and including the first SMS: SMS A's own delayed backup was held (it waited 17 min 50 s). Mechanism 2 does not depend on mechanism 1.

U-069 (the fix for BUG-023) makes the trigger worker call `scheduleIncoming()`. That changes what the trigger enqueues, not how often it fires, and not the quota. It changes neither mechanism.

This bug records the facts, the options and the decisions only the owner can take. It does not choose the fix (the Solution Architect writes the approach next), and it does not decide what delay is acceptable (product owner).

### Facts confirmed against the code, and corrections to the report that raised this

Source files were read from commit `0d490997` (`git show`), not from the working tree. Line numbers are those of that commit; they are cited so a claim can be checked, not as stable addresses. The legacy files are read at `d095fe02^`, the parent of the commit (U-017) that deleted them.

1. **How the trigger is registered: confirmed.** `WorkManagerScheduler.scheduleContentTrigger()` (`app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt:218-250`):
   - Constraints: `addContentUriTrigger(Consts.SMS_PROVIDER, true)` (descendants included), plus the call-log URI when call-log backup and call-log-after-call are both on (`:224-230`). No network constraint, no charging or battery constraint.
   - Request: `PeriodicWorkRequest.Builder(BackupTriggerWorker::class.java, PERIODIC_MIN_INTERVAL_SECS, SECONDS)` (`:232-239`). The interval is 15 minutes: `PERIODIC_MIN_INTERVAL_SECS = TimeUnit.MINUTES.toSeconds(15)` (`:116`), documented at `:112-115` as the WorkManager minimum. No flex interval is given (the three-argument constructor), no backoff, tag `CONTENT_TRIGGER` (`:238`).
   - Enqueue: `enqueueUniquePeriodicWork("CONTENT_TRIGGER", ExistingPeriodicWorkPolicy.UPDATE, request)` (`:241-246`).
   - Gate: returns null without enqueuing when the secret store reports the encryption-degraded state (`:219-222`, U-054).
2. **What `BackupTriggerWorker` does: confirmed.** `doWork` (`app/src/main/java/com/zegoggles/smssync/worker/BackupTriggerWorker.kt:53-74`) logs the trigger line, enqueues unique work `INCOMING` with `REPLACE` and `setInitialDelay(incomingTimeoutSecs)`, and returns `Result.success()`. It never re-registers anything, so the next fire is scheduled by WorkManager from the periodic request alone. U-069 replaces the enqueue with `scheduler.scheduleIncoming()`; the registration is untouched (BUG-023 AC-11).
3. **Why the period limits firing: read in the library sources, with one version caveat.** The app uses `androidx.work:work-runtime-ktx:2.9.1` (`app/build.gradle:155`). The 2.12.0 sources are in the local Gradle module cache (S4 of DES-MODERNIZATION-014 moves the app to 2.12.0); the 2.9.1 sources were not opened, so the logic is read for 2.12.0 and not shown for 2.9.1.
   - `WorkSpec.calculateNextRunTime` (`WorkSpec.kt`, companion function from line 411): for periodic work with `periodCount == 0` the next run is `lastEnqueueTime + initialDelay` (immediate when there is no initial delay); otherwise `lastEnqueueTime + intervalDuration`. With no flex, the interval is the full 15 minutes. The code comment there says the first run of a periodic request is immediate in the scheduler.
   - `SystemJobInfoConverter.java:97-114`: the job gets `setMinimumLatency(nextRunTime - now)` when that is above zero. Content URIs are added with `addTriggerContentUri` at `:116-123`. The converter code read (`:75-147`) calls no periodic setter on the job; each period is one scheduler job that WorkManager re-creates after a run.
   - Reading of the record: the first SMS found a fresh, never-run period (zero latency) and the trigger fired 11 s later (07:25:32 to 07:25:43). After that run the next job carried the full interval as latency ("+14m59s993ms"), which is the 900 s of the request. So the 15-minute floor is `PERIODIC_MIN_INTERVAL_SECS` working as written.
4. **When the trigger is registered and re-registered (a correction to the report: not at app start).** `scheduleContentTrigger()` has one production caller, `App.rescheduleJobs()` (`app/src/main/java/com/zegoggles/smssync/App.java:373-385`): `scheduler.cancelAll()` first (`:374`), then, when auto backup is on, `scheduleRegular()` (`:377`) and, when the incoming delay is above 0, `scheduleContentTrigger()` (`:381-382`). `rescheduleJobs()` runs only when `SyncEvent.AutoBackupSettingsChanged` is collected (`App.java:279-293`). The emitters found by search are: the preference-change listener (`activity/fragments/SMSBackupPreferenceFragment.java:31-56`), used for the auto-backup switch (`MainSettings.java:71`) and for the incoming schedule, regular schedule and Wi-Fi-only settings (`AutoBackupSettings.java:19-24`); the call-log permission grant (`AdvancedSettings.java:201-207`); and `PackageReplacedReceiver` (`receiver/PackageReplacedReceiver.java:20-29`, on `MY_PACKAGE_REPLACED`). A search for emitters of the event found no emission on an ordinary app start, and `BootReceiver` calls only `scheduleBootup()` (`receiver/BootReceiver.java:33`), which enqueues the one-time `REGULAR` backup after 60 s and does not touch the trigger (`WorkManagerScheduler.kt:262-286`). The record's "cancelAll, scheduleRegular and scheduleContentTrigger at app start" therefore fits the `adb install -r` of the U-068 build at 07:42:47, which sends `MY_PACKAGE_REPLACED`, rather than the app being started at 07:42:50. The log itself is outside the repository and was not read, so this is a reading of the code against the record, not an observation.
   - Consequence that matters for the design and for testing: because `cancelAll()` always precedes the registration, `UPDATE` never updates an existing request here; every registration creates a new request with period count 0, so the first SMS after any registration fires at once and the 15-minute limit applies from the second. Changing the Wi-Fi-only setting, the incoming or regular schedule, or switching auto backup off and on, resets the period. After a reboot WorkManager restores its own jobs (the converter's own comment at `SystemJobInfoConverter.java:125-126` says the jobs are rescheduled on boot); the period is not reset by `BootReceiver`.
   - The reset is also a practical hazard for the Android 17 measurement of story S5: its Run R reads the trigger worker's line (BUG-023, Existing Behavior to Preserve). CR-015 already said that how soon the trigger fires again "is not verified, so Run R should not assume it fires immediately" (`sdlc/artifacts/build/change-records/CR-015-cntr-004-content-trigger-pre-api-24-fallback-removed.md:667-668`). The record now answers it: not within the same fifteen-minute period.
5. **The legacy trigger, and whether the limit is a regression: partly verified, see "Was this a regression" below.**
6. **The other incoming path: confirmed as BUG-023 states it, nothing new read.** `SmsBroadcastReceiver` is declared `android:exported="false"` with `android:permission="android.permission.BROADCAST_SMS"` (`app/src/main/AndroidManifest.xml:132-139`); `RECEIVE_SMS` is declared (`:17`) and BUG-023's Suggested Approach established that it is never requested at run time. The platform side of that finding is cited from memory in BUG-023 and was not re-fetched here. See "A working receiver path" below.
7. **The regular backup: confirmed, with a side observation.** `scheduleRegular()` is a `PeriodicWorkRequest` of `BackupWorker` with the network constraint of INV-2 and EXPONENTIAL 30 s backoff (`WorkManagerScheduler.kt:170-202`; interval `maxOf(regularTimeoutSecs, 900)` at `:180`). Default 2 hours: `Defaults.REGULAR_TIMEOUT_SECONDS = 2 * 60 * 60` (`app/src/main/java/com/zegoggles/smssync/preferences/Defaults.java:19`) and `android:defaultValue="7200"` (`app/src/main/res/xml/preferences.xml:188-193`). Options: 30 min, 1 h, 2 h, 6 h, 24 h, never (`app/src/main/res/values/arrays.xml:74-90`). It is a scheduler job like the trigger, so it has the same standby and quota exposure (the platform text is cited under Mechanism 2); that is read from the code and the platform text, not observed on a device. The side observation is under Notes, "Observations outside this bug" (the "never" value).

### Was this a regression from the legacy app

**The mechanism changed: verified. A change in what the owner experienced: not verified.**

What the legacy code did (read at `d095fe02^`):

- `BackupJobs.createContentUriTriggerJob()` (`app/src/main/java/com/zegoggles/smssync/service/BackupJobs.java:168-175`) built the trigger as a Firebase JobDispatcher job with `Trigger.contentUriTrigger(observedUris())`, `setRecurring(true)`, `setLifetime(FOREVER)`, tag `contentTrigger`, replace-current, and the `INCOMING` constraints and retry strategy (network `ON_ANY_NETWORK` or `ON_UNMETERED_NETWORK`; exponential 30/300 s; `:186-206`).
- `SmsJobService.onStartJob` (`app/src/main/java/com/zegoggles/smssync/service/SmsJobService.java:65-92`) handled a content-triggered run by calling `getBackupJobs().scheduleIncoming()` and returning (`:72-77`). The legacy delayed follow-up was `Trigger.executionWindow(inSeconds, inSeconds)` (`BackupJobs.java:162`).
- The dispatcher's driver was `GooglePlayDriver` or, with the old-scheduler setting, `AlarmManagerDriver` (`BackupJobs.java:68-71`).

So the old trigger was a recurring content-URI job run by the dispatcher's driver, not a one-shot job that the app re-registered by hand. Whether every SMS fired it, and with what minimum gap, depended on the dispatcher's driver (the Google Play driver ran in the Play services process), and that code is not in this repository. **Not verified:** that the legacy trigger fired for a second SMS inside fifteen minutes. No measurement of the legacy app exists in the artifacts searched.

What the migration changed (verified): U-014 chose a `PeriodicWorkRequest` with `PERIODIC_MIN_INTERVAL_SECS` (the commit that introduced the constant is `55e4e4e7`, "U-014: WorkManagerScheduler adapter"). The design documents call the trigger "recurring content-URI observer" and "`PeriodicWorkRequest` + content-URI triggers" (DES-MODERNIZATION-005 `:89`, `:436`; CNTR-MODERNIZATION-004 `:66`, example `:369-380` with `recurring = true`) and say the WorkManager triggers "reproduce this two-stage behavior" (DES-MODERNIZATION-005 `:133-138`). A search of DES-MODERNIZATION-005, CNTR-MODERNIZATION-004 and REQ-MODERNIZATION-005 for the fifteen-minute floor, "one-shot" and re-registration found nothing; neither document says that a periodic request limits how often the trigger can fire. U-014's plan and review do not mention it (`sdlc/artifacts/build/sprints/sprint-001/U-014/plan.md:42`, `review-code.md:32`, which checks only that the policy is `UPDATE`). So the fifteen-minute limit was not a considered design decision, and the design text does not describe it. Whether it is a regression for the owner depends on the unverified legacy behaviour above. Do not describe it in the story as a regression from the legacy app; describe it as a limit the design did not state.

### What the requirements, contract and user-facing text promise

| Source | Text | Is "about a minute after arrival" promised? |
|---|---|---|
| Setting label and help | "Incoming schedule" and "When to backup incoming SMS (after arrival)" (`app/src/main/res/values/strings.xml:169-170`); entries "1 min", "3 mins", "30 mins", "1 h", "Never" (`arrays.xml:92-108`; strings `_1min` `:258`, `_3mins` `:259`); default 60 s (`Defaults.java:15`, `preferences.xml:196-201`) | Yes, as the setting's meaning: the owner picks the delay after arrival. No text mentions a limit or a dependency on the system |
| README FAQ | "Incoming backup schedule is used for incoming messages. 3 minutes here means that any incoming SMS will trigger a backup after 3 minutes. ... You should set the incoming schedule to a low value if you want to make sure that incoming SMS show up in Gmail shortly after arrival." (`README.md:296-302`) | Yes, in the plainest form: "any incoming SMS will trigger a backup after 3 minutes". It says "any" |
| CNTR-MODERNIZATION-004 INV-4 | An SMS or call-log content change MUST trigger backup via content-URI triggers; the content change enqueues a delayed incoming backup, delay = `getIncomingTimeoutSecs()`, and not a backup on the raw change (`:243`, Validation Rule 5 `:277`). The two-stage debounce | Yes in part: "MUST trigger backup" for "an SMS content change", delay = `incomingTimeoutSecs`. It does not say how soon after the change the trigger must fire, or that every change must fire it |
| DES-MODERNIZATION-005 | "WorkManager content-URI triggers reproduce this two-stage behavior" (`:133-138`) | The word "reproduce" is the only commitment; nothing about latency or frequency |
| REQ-MODERNIZATION-005 | AC-3 (d) "content-URI trigger" among four invariants that are "preserved and test-verified"; the requirement is to preserve "all current scheduling behavior" (`:30`, `:53-58`) | By "preserving all current scheduling behavior" the requirement covers it only if the legacy behaviour was prompt per SMS, which is not verified above |
| Tests | `inv4_contentTrigger_*` assert the registration and the constraints, not a firing sequence (DES-MODERNIZATION-005 `:379`) | No test fires the trigger twice |

So "a backup about a minute after an SMS arrives" is a stated expectation in the setting's help text and, for "any incoming SMS", in the README FAQ. It is not stated in the requirement or contract as a latency bound. The requirement and contract promise the debounce structure and the delay value, not a firing guarantee.

### The platform side of mechanism 2 (what was read, what was not)

Read locally in the SDK directory (the Android 36 platform sources, read-only; nothing run):

- App standby buckets (`android/app/usage/UsageStatsManager.java`): `STANDBY_BUCKET_ACTIVE = 10` "The app was used very recently, currently in use or likely to be used very soon ... Buckets > ACTIVE will most likely be restricted in some way. For instance, jobs and alarms may be deferred" (`:127-133`); `WORKING_SET = 20` (`:135-140`), `FREQUENT = 30` (`:142-149`), `RARE = 40` "will have more restrictions, including network restrictions, except during certain short periods (at a minimum, once a day) when they are allowed to execute jobs" (`:151-158`), `RESTRICTED = 45` (`:160-168`, "not enabled in R"), and **`STANDBY_BUCKET_NEVER = 50` "The app has never been used." (`:170-175`, hidden system API)**. So bucket 50 in the record means the platform regards the app as never used, which is the lowest, a fresh-install condition on an emulator that had just been started. It is not a statement about where a phone the owner uses will be. The getter's text (`:738-742`): apps in buckets at or below ACTIVE have no standby restrictions.
- `JobScheduler.PENDING_JOB_REASON_QUOTA = 14` "The app has consumed all of its current quota" (`android/app/job/JobScheduler.java:231-236`) and `PENDING_JOB_REASON_APP_STANDBY = 2` "The current standby bucket prevents the job from running" (`:158-162`). The record's `WITHIN_QUOTA` constraint is the first of these. The numbers (how much quota each bucket gets, in what window) are not in the public sources read; **not verified**.
- Expedited jobs (`android/app/job/JobInfo.java:1990-2043`): "Bypass Doze, app standby, and battery saver network restrictions", "stricter quotas", "If an app has run out of available expedited quota, any pending expedited jobs will run as regular jobs", "Expedited jobs may only set network, storage-not-low, and persistence constraints", and "Be exempt from delay to optimize job execution".

Read in the WorkManager 2.12.0 sources (local Gradle module cache; 2.9.1 not read):

- `WorkRequest.Builder.setExpedited(OutOfQuotaPolicy)`: policies `RUN_AS_NON_EXPEDITED_WORK_REQUEST` and `DROP_WORK_REQUEST` (`OutOfQuotaPolicy.kt`). `build()` rejects an expedited request with a content-URI trigger, battery-not-low, charging or device-idle constraint ("Expedited jobs only support network and storage constraints") **and rejects an initial delay above 0 ("Expedited jobs cannot be delayed")** (`WorkRequest.kt:301-305`). The converter sets the job expedited only on API 31 and above, and only when the work is not a retry and not delayed (`SystemJobInfoConverter.java:132-138`).
- `CoroutineWorker.getForegroundInfo()` throws `IllegalStateException("Not implemented")` unless overridden; the doc says to override it when the request is expedited (`CoroutineWorker.kt:91-93`, and the text at `:88-89`). `CoroutineWorker.setForeground` "will throw an IllegalStateException if the process is subject to foreground service restrictions. Consider using setExpedited and getForegroundInfo instead" (`:108-111`).

Read in the app (commit `0d490997`):

- `BackupWorker.doWork` calls `setForeground(createBackupForegroundInfo())` as its first statement (`app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt:123`), with `FOREGROUND_SERVICE_TYPE_DATA_SYNC` on Android 10 and above (`:517-538`, `:535`). The manifest overrides WorkManager's `SystemForegroundService` to declare `foregroundServiceType="dataSync"` (`AndroidManifest.xml:127-129`) and holds `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_DATA_SYNC` (`:65`, `:67`). This is a foreground notification once the job is already running; it does not start the job earlier and does not give the job quota. No expedited request and no `getForegroundInfo` override exist in `app/src` (search of `setExpedited`, `OutOfQuotaPolicy`, `getForegroundInfo`: no match).
- `BackupTriggerWorker` has content-URI triggers on its request, so it cannot itself be expedited (the `build()` check above).

Not read, so **not verified**: the platform's published tables for how long each bucket may defer a job and how much quota each bucket gets; whether an app that is exempt from battery optimisation is placed in a better bucket; which events start a foreground service from the background without restriction on Android 12 and newer (the exemption list); whether a running scheduler job counts as one; what the Android 17 documentation says about any of this.

### What the owner actually experiences

Known:
- On one Android 16 emulator, with a never-used standby bucket, a debug build and two messages, the incoming backups waited 18 and 12 minutes and ran when the app was opened (the record).
- The code registers the trigger as 15-minute periodic work, so a second message inside a period cannot start the trigger before the period ends (Fact 3, read from the library sources, not observed on a device past the 9.5 minutes).
- The regular backup, every 2 hours by default, is the safety net (Fact 7). It is a scheduler job with the same standby and quota exposure, so it is not shown to be more prompt than the incoming path in a low bucket. Nothing is lost: the next backup that does run takes everything not yet backed up. **Correction by the Solution Architect:** after a reboot the periodic regular backup is deleted by the boot backup's `REPLACE` and is not registered again until a settings change or a package replace. This is read from the code and the library, not observed (Suggested Approach, "Safety-net correction"; BUG-026, Observations). Until that is fixed, "the safety net" holds only between reboots.

Not known:
- Which bucket the owner's phones are in. A phone used daily, with the app opened at least sometimes, sits in a higher bucket than a never-used emulator (the SDK text above: bucket follows use). But this app is a background tool that the owner may open rarely, so a higher bucket than 50 is expected and not shown. The owner's own devices have not been measured.
- Whether, with quota available, the second SMS would have fired the trigger at the end of the period and been backed up one delay later (the record says the period ended at about 07:40:43 with quota the unmet condition, and does not show it). This is the expected behaviour from the library logic, not observed.
- Whether the legacy app was prompt (above).
- How the delay behaves on Android 17 (S5 of DES-MODERNIZATION-014) or at targetSdk 37.

## Steps to Reproduce

The device steps are the orchestrator's procedure in the record, on the Android 16 emulator, with a debug build and auto backup on, mail account set, "Incoming schedule" 1 minute. Nothing was run for this artifact (no emulator, no Gradle).

**(a) Mechanism 1, isolated from mechanism 2.**
1. Put the app in an active standby bucket (read the bucket with `am get-standby-bucket` before the run and after, and record both; the record read it that way). The command that sets the bucket for the isolating run is the orchestrator's to confirm: not verified here.
2. Make any change that re-registers the trigger (for example, change the incoming schedule to 3 mins and back to 1 min), so the period starts fresh. Wait 1 minute.
3. Send test SMS A. Observe the trigger line within seconds ("BackupTriggerWorker.doWork: content-URI trigger fired ... initialDelay=60s") and a backup starting one delay later.
4. Six minutes after A, send test SMS B. Observe on today's code: no trigger line for B until the 15-minute period since A's trigger has ended. Expected on today's code from the library logic: the trigger fires when the period ends (about 15 minutes after A's fire) and B's backup starts one delay after that, so about 16 minutes after A, about 10 minutes after B. Not shown yet; the record's run could not show it because of quota.

**(b) Mechanism 2.** As the record: bucket 50 (a never-used app), then SMS A, then (optionally) SMS B. Read the scheduler dump (`dumpsys jobscheduler`, the record's method) for the app's jobs: `WITHIN_QUOTA` unsatisfied after their delay has elapsed; the backup runs when the app is opened. Observed once.

**(c) Unit level (where the harness allows).** A Robolectric test with `WorkManagerTestInitHelper` (as `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt:42-81`): register the trigger, drive the test driver through "constraints met" for the trigger, then through "next period" and "constraints met" again, and read whether the trigger runs again and when. Whether the test driver models the content-URI re-arm and the minimum latency of a periodic request is not verified; the Solution Architect states what the harness can and cannot show, and the device measurement carries what it cannot.

## Expected Behavior

An incoming message is backed up about the configured "Incoming schedule" delay after it arrives, whether it is the first message or a later one, as the setting's help text and the README FAQ say. Where the system cannot allow that (the scheduler withholds quota from the app), the app says so to the owner, or does something defined about it, and the regular backup remains the safety net. The contract and design say what the code does.

## Actual Behavior

The first message after the trigger is registered fires it within seconds. A later message within the 15-minute period does not fire it until the period has ended. Independently, in a low standby bucket every job of the app, including the delayed incoming backup, waits until the app is opened or quota returns. No text in the app, the README, the contract or the design says either of these.

## Evidence

The record is `sdlc/artifacts/build/research/2026-11-21-incoming-backup-delay-measurement.md`. One emulator, two messages, one run; raw logs are kept outside the repository. Its stated limits are kept: it does not show behaviour on a physical device or in another bucket, nor whether the second message would have fired the trigger at the end of the period had quota been available, nor how long the quota hold lasts without opening the app.

| Time (device clock) | Event | What it shows |
|---|---|---|
| 07:25:32 | SMS A sent | |
| 07:25:43 | Trigger line, `initialDelay=60s` | The first SMS fires the trigger in 11 s |
| 07:29:20 | Dump: trigger job "Minimum latency: +14m59s993ms", `TIMING_DELAY, FLEXIBILITY, CONTENT_TRIGGER`, may run again in +11m22s; delayed `INCOMING` job, delay elapsed 3m15s earlier, `WITHIN_QUOTA` unsatisfied; a delayed job with a connectivity requirement, `CONNECTIVITY` and `WITHIN_QUOTA` unsatisfied; bucket 50 | Mechanism 1 (latency of about the 900 s interval) and mechanism 2 (quota) together |
| 07:31:26 | SMS B sent | |
| to 07:41:00 | No trigger line (9.5 minutes watched) | Consistent with mechanism 1; the period ends at about 07:40:43 |
| 07:41:09 | Dump: trigger job's latency elapsed 26 s earlier, content condition met, `WITHIN_QUOTA` unsatisfied; the two delayed jobs waiting 14.5 and 15 minutes after their delay elapsed | Mechanism 2: quota is the unmet condition, so mechanism 1 cannot be judged past this point |
| 07:42:47 to 07:42:50 | Debug build of U-068 installed over the app, app started; bucket afterwards 10; `BackupWorker` starts `type=INCOMING`, 2 messages | The jobs ran within a second of the app being opened |
| 07:43:22 | `backup complete, backedUp=2` | SMS A 17 min 50 s and SMS B 11 min 56 s after arrival |

Two things the table does not settle: (1) the trigger job's content condition was already met at 07:41:09, which fits the expectation that a message that arrived inside the period is picked up when the period ends; (2) no log line from the run says why the jobs became eligible at the moment the app was opened (bucket moved from 50 to 10 at the same time as the install-over and the start). Reading that the bucket change caused it is the record's reading and is consistent with the dump; the bucket was not changed on its own while watching.

### A working receiver path (how it would and would not help)

`SmsBroadcastReceiver` is meant to be a second incoming path (CNTR-MODERNIZATION-004 `:245`: incoming SMS only; when delivered, and auto backup is on, login set and first backup run, it calls `scheduleIncoming()`, `SmsBroadcastReceiver.java:44-83`). BUG-023's Suggested Approach established that it is not delivered the system broadcast (not exported; `RECEIVE_SMS` never requested) and records product-owner item PO-C (BUG-023 item PO-3: remove or restore it; `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md:2353`).

- **Mechanism 1: a working receiver would avoid it for incoming SMS.** The receiver would run on every `SMS_RECEIVED` broadcast, with no 15-minute period: each message calls `scheduleIncoming()`, which replaces the pending `INCOMING` work (INV-1), so the debounce is the unique-work `REPLACE`. It sees incoming SMS only; sent messages and call-log changes are seen only by the content trigger (CNTR-MODERNIZATION-004 `:245`), so the trigger's limit would remain for those.
- **Mechanism 2: it would not avoid it.** The receiver enqueues the same delayed `INCOMING` job, which is subject to the same quota. Any job-based path has mechanism 2. Only a path that does not wait for the job scheduler (for example, work started by the receiver itself or a foreground service) would be different, and whether the platform allows that from a receiver on Android 12 and newer was not verified.
- **Cost.** A new run-time permission prompt for `RECEIVE_SMS`, and an exported receiver (BUG-023 PO-3).

## Acceptance Criteria

The criteria state behaviour and say how each is checked; they do not fix the mechanism. A criterion that depends on a product-owner choice says so and gives the criterion for each option (PO-1, PO-2 and PO-3 below). The tolerance values are proposals for the Solution Architect to confirm or replace; the story states the final values before the criteria are used. "Configured delay" means `incomingTimeoutSecs`.

- [ ] AC-1 (mechanism 1): With the app in an active standby bucket, a second incoming SMS that arrives inside fifteen minutes of the first starts its backup within the configured delay plus a tolerance of 60 seconds after it arrives. Checked by (a) a unit test of the registration and firing sequence where the harness allows it (AC-8 states what it can show), and (b) a repeat of today's emulator measurement: two SMS six minutes apart, the bucket read before and after and recorded, the trigger line times and the `BackupWorker ... type=INCOMING` start times recorded for both messages. The tolerance of 60 s is confirmed by the Solution Architect for the active bucket (the first message fired in 11 s in the record, which fits a platform content-update delay of about 10 s; the tolerance also covers the scheduler's ordinary slack). It does not apply in a low bucket (AC-4).
- [ ] AC-2 (first SMS kept): The first message after the trigger is registered still fires it and still starts its backup one configured delay later, within the same tolerance. Checked by the same measurement (SMS A of the repeat) and by a unit test that registers the trigger and fires it once.
- [ ] AC-3 (debounce kept, INV-4): Nothing is backed up on the raw content change. Two messages inside one configured delay leave exactly one non-terminal `INCOMING` work, replaced by the later one (INV-1), and the backup runs once. Checked by BUG-023's tests AC-7 and AC-8 (they stay green) and by the repeat measurement with two messages 30 seconds apart, which shows one `type=INCOMING` run.
- [ ] AC-4 (quota, depends on PO-1 and PO-2): What the app does when the scheduler withholds quota is defined. Per option: **Option S (state it):** the owner is told in the app where the delay cannot be avoided (AC-5) and no code path is added for it; checked by AC-5 and by the repeat measurement in bucket 50 showing the backup runs when the app is opened. **Option X (expedited incoming backup):** with the app in bucket 50, an incoming backup starts within the configured delay plus 60 seconds of arrival without the app being opened, or, if the out-of-quota policy is run-as-non-expedited, the behaviour in that case is the one AC-5 states; checked by the repeat measurement in bucket 50 and by a unit test of the request that is built (the policy, and how the debounce delay is kept; see Suggested Approach, ruling 2, which finds no place for expedited work in this path). **Option R (receiver restored):** the criteria of PO-3 below apply, and AC-4 is the same as for Option S for the quota part. A choice that is not recorded here is not accepted by the story.
- [ ] AC-5 (told to the owner): Wherever the delay after arrival cannot be guaranteed, the owner-facing text says so: the help text of "Incoming schedule" (`strings.xml:170`, and the translations that exist in `values-*`), and the README FAQ (`README.md:296-302`, which now says "any incoming SMS will trigger a backup after 3 minutes"). The text states the real condition in words the owner can check (for example, that the system may hold the backup until the app is opened, and that the regular backup is the safety net). Checked by reading the strings and the README against the behaviour measured in AC-1 and AC-4; no text may claim a delay the measurement does not show. The wording is the owner's to approve (PO-1).
- [ ] AC-6 (contract and design say what the code does): CNTR-MODERNIZATION-004 INV-4 (`:243`, the example at `:369-380`) and DES-MODERNIZATION-005 (`:89`, `:133-138`, `:320`, `:436`, `:495`) state how the trigger fires again after a run and the latency bound that follows, by a change record, in the same change as the code. No statement in the contract, design, requirement note or code comment is false after the change (`WorkManagerScheduler.kt:57-62` and `:204-213` comments included). Checked by reading those places against the code and the measurement.
- [ ] AC-7 (regular backup unchanged): Nothing about the regular backup changes: `scheduleRegular()` (`WorkManagerScheduler.kt:170-202`), its interval, constraints, backoff, unique name and policy are unchanged, and `scheduleBootup`, `scheduleImmediate`, `scheduleManual`, `scheduleRestore`, `cancelAll` and `cancel` are unchanged unless the chosen design requires a change that the story names and the contract records. Checked by `git diff` over those files and the existing `WorkManagerSchedulerTest` run.
- [ ] AC-8 (test honesty): The story states what a unit test can show of the platform's content-trigger re-arm and the periodic latency, and what only the device measurement can. Where a behaviour above (AC-1) cannot be shown by a unit test, the criterion is carried by the device measurement, recorded as evidence in the story with the raw log lines of the run, the bucket values and the times. A unit test that cannot fail for the defect is not counted.
- [ ] AC-9 (registration reset is covered): After any re-registration (a settings change, a package replace), the first message fires at once and a second inside fifteen minutes follows AC-1. Checked by the repeat measurement run once after a settings change and once after an `adb install -r`.
- [ ] AC-10 (conditional on PO-3, Option R): If the receiver is restored, an ordinary SMS produces the receiver's `onReceive` line and a `scheduleIncoming` line on the emulator with `RECEIVE_SMS` granted; the receiver is exported only as far as the platform needs; the permission prompt exists and its refusal leaves the content trigger working; and mechanism 1 is shown not to apply to a second incoming SMS inside fifteen minutes. If it is removed instead, the manifest, `SmsBroadcastReceiver.java`, `SmsBroadcastReceiverTest`, CNTR-MODERNIZATION-004 `:243`, `:245`, `:387-389` and DES-MODERNIZATION-005 `:321` are changed by a change record and no text claims a receiver path.
- [ ] AC-11 (gate): `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` exits 0 with 0 failures, the skipped count not higher, and the authoritative test count (`git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`) taken before and after, the difference explained by the new tests. No per-package coverage floor is lowered.
- [ ] AC-12 (the trigger cannot disarm itself; added by the Solution Architect): After any trigger run, exactly one pending trigger exists while auto backup is on, the incoming delay is above 0 and the encryption is not degraded. This holds whatever the follow-up enqueue does, including throwing. No trigger is re-armed when one of those conditions is false. The re-arm never leaves a request waiting behind a periodic request. Checked by the unit tests T3 to T7 of the Suggested Approach, ruling 3, each shown to fail by its recorded mutation (AC-8), and on the device by the dump after the upgrade install of the acceptance repeat.

### Integration Criteria (if fix modifies component boundaries)

- [ ] IC-1: Every place that registers or re-registers the trigger is listed and handled: `WorkManagerScheduler.kt:218-250`, `App.java:373-385`, and, if the trigger becomes a one-time request that is enqueued again after each run, the place that does it (`BackupTriggerWorker.kt:53-74` or the scheduler), with a search of `app/src/main` for `CONTENT_TRIGGER`, `enqueueUniquePeriodicWork`, `scheduleContentTrigger` attached.
- [ ] IC-2: If the type of the trigger request changes (periodic to one-time), the work already persisted on devices under the unique name `CONTENT_TRIGGER` is handled: after an upgrade install over the existing install (data kept), the old periodic work is replaced or cancelled and exactly one trigger is registered. Checked on the emulator with `adb install -r` and the scheduler dump. The worker class name and package `com.zegoggles.smssync.worker.BackupTriggerWorker` do not change (BUG-023 Suggested Approach, "Persisted work").
- [ ] IC-3: If the port `BackupScheduler` changes (a new operation or a changed signature), that is a contract change under CNTR-MODERNIZATION-004 and needs its change record first; every caller is updated and compile-checked.

## Impact Analysis

Searches run for this artifact: `scheduleContentTrigger`, `rescheduleJobs`, `cancelAll`, `PERIODIC_MIN_INTERVAL_SECS`, `AutoBackupSettingsChanged`, `setExpedited`, `OutOfQuotaPolicy`, `getForegroundInfo`, `setForeground`, `SMS_RECEIVED`, `ui_auto_backup_incoming_schedule`, and the legacy `BackupJobs` and `SmsJobService`. Counts are from these searches, not exhaustive greps of every layer.

| # | Layer | Affected? | Files and lines | Action needed |
|---|-------|-----------|-----------------|---------------|
| 1 | Requirements (`sdlc/artifacts/requirements`) | Check only | `modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md:30`, `:53-58`, `:87` (AC-3 (d); "preserving all current scheduling behavior") | None expected for mechanism 1 (the requirement does not state a latency). If the owner sets a delay expectation (PO-1), that is a requirement change, not this bug |
| 2 | Design and contracts (`sdlc/artifacts/design`) | Yes | `contracts/CNTR-MODERNIZATION-004-backup-scheduler.md:66`, `:243`, `:245`, `:277`, `:369-380`; `modernization/DES-MODERNIZATION-005-...md:89`, `:133-138`, `:320`, `:436`, `:495`; `DES-MODERNIZATION-014-...md` (S4 library move, S5 measurement) | AC-6; a change record. Whichever option is chosen, INV-4 gets a sentence on how the trigger fires again and the bound |
| 3 | Stories, sprints, change records | Yes | BUG-023 / U-069 (the trigger worker, not the period); BUG-021; S4 and S5 of DES-MODERNIZATION-014; CR-015 `:667-668` | Sequencing is the orchestrator's |
| 4 | Application code (`app/src/main`) | Yes | `scheduler/WorkManagerScheduler.kt:218-250` (registration); `worker/BackupTriggerWorker.kt:53-74`; `App.java:373-385`; `service/BackupWorker.kt:123`, `:517-538`; `AndroidManifest.xml:127-139`; `receiver/SmsBroadcastReceiver.java`; `res/values/strings.xml:169-170` | Depends on the option chosen |
| 5 | Vendored library (`k9mail-vendored`) | No | None | None |
| 6 | Tests (`app/src/test`) | Yes | `service/WorkManagerSchedulerTest.kt:42-81` (harness); `inv4_contentTrigger_*` (registration only); `SmsBroadcastReceiverTest` | AC-1, AC-3, AC-8; new tests |
| 7 | Documentation | Yes | `README.md:296-302`; translated strings in `app/src/main/res/values-*/strings.xml` (`ui_auto_backup_incoming_schedule_desc` exists in 25 locale folders, 26 files with the default; corrected by the Solution Architect from "at least in `values-ca`") | AC-5 |

**Interaction with other work.**
- **BUG-023 / U-069**: BUG-023 changes what the trigger enqueues; this bug changes how often the trigger can fire and what the scheduler allows. They do not edit the same lines unless the trigger worker is re-enqueueing itself (Option 1 below), in which case BUG-025 builds on the U-069 worker; the order is the Solution Architect's and the orchestrator's. U-069's AC for single flight (AC-8 of BUG-023) stays valid and is reused by AC-3 here.
- **BUG-021**: its likely case rests on an incoming message starting a debounced backup within minutes (BUG-021, Description); mechanism 1 and 2 widen the time before that backup. BUG-021's own D14 Run R reads the trigger line: see Fact 4.
- **S4 and S5 of DES-MODERNIZATION-014**: S4 moves WorkManager to 2.12.0, where the logic of Fact 3 was read; the tests written here run on 2.9.1 first and 2.12.0 after. S5's Android 17 measurement needs the trigger to fire; it should account for the period (re-register to reset it, Fact 4) and should not wait for this bug unless the owner wants the trigger changed first.
- **PO-C / BUG-023 PO-3** (the SMS receiver): the decision interacts with the options below.
- **BUG-024** (cancelled runs reported as failure): unrelated code. Listed because it was the sibling found on the same day.

## Suggested Approach

Written by the Solution Architect on 2026-11-21. Advisory until story planning, which may refine it. Application code was read with `git show 0d490997:<path>` and the legacy code with `git show d095fe02^:<path>`; line numbers are those commits'. Nothing was run: no Gradle, no emulator, no test.

**Summary of the rulings.**
1. Mechanism 1: replace the periodic trigger with a **one-time content-triggered request that the trigger worker re-arms after each run**, with `APPEND_OR_REPLACE` for the re-arm, `REPLACE` for every registration from outside the worker, and a guard. Contract: a change record, CNTR-MODERNIZATION-004 v4, breaking for the behavioural contract under the contract's own policy, no consumer migration.
2. Mechanism 2: **no code that tries to beat the quota.** Expedited work does not fit anywhere in this path, and neither does a foreground service. Battery-optimisation exemption is not requested. The app's text states the condition (Option S of AC-4).
3. Testability: the WorkManager test driver can show the re-arm, the policy, the migration and the guard, and it can tell today's periodic trigger from the fix. Only a device run can show the platform's content observation, its delays and the quota.
4. Sequencing: BUG-026's fix, then this fix, as two small stories after U-072 and before U-073 (details in ruling 4).
5. The six questions in the analyst's options table are answered one by one in ruling 5. Three items go to the owner (list at the end).

### Library and platform facts used (read / not read)

| Fact | WorkManager 2.9.1 (in use) | WorkManager 2.12.0 (U-073) |
|---|---|---|
| A `OneTimeWorkRequest` may carry content-URI triggers. `Constraints.Builder.addContentUriTrigger` is `@RequiresApi(24)`. The one-time builder's only check is idle mode plus backoff | **Read** (bytecode: `OneTimeWorkRequest$Builder` has only the message "Cannot set backoff criteria on an idle mode job"; `WorkRequest$Builder.build` checks content URIs only for expedited work) | **Read** (`Constraints.kt:457-459`; `OneTimeWorkRequest.kt:65`; `WorkRequest.kt:292-305`) |
| An expedited request cannot be delayed and cannot carry content URIs; periodic work cannot be expedited | **Read** (bytecode strings "Expedited jobs cannot be delayed", "Expedited jobs only support network and storage constraints") | **Read** (`WorkRequest.kt:301-305`; `PeriodicWorkRequest.kt:335`) |
| Next run time: periodic work runs at `lastEnqueueTime + initialDelay` when `periodCount == 0` and at `lastEnqueueTime + intervalDuration` after that; one-time work runs at `lastEnqueueTime + initialDelay` | **Read** (bytecode of `WorkSpec$Companion.calculateNextRunTime`: the same branches and argument order). This closes Not verified item 4 | **Read** (`WorkSpec.kt:411-456`, periodic branch `:437-453`) |
| The converter sets `setMinimumLatency` only for a positive offset (API 29 and above), adds `addTriggerContentUri` per trigger, passes the update delay and max delay through, and calls `setPersisted(false)`. It never calls `setPeriodic` | **Read** (bytecode: those calls, and no `setPeriodic`) | **Read** (`SystemJobInfoConverter.java:97-127`) |
| Unique-work policies. `KEEP` returns without enqueuing while any entry under the name is `ENQUEUED` or `RUNNING`. `REPLACE` cancels and deletes every entry under the name, the running one included. `APPEND_OR_REPLACE` makes the new work a child of every leaf without dependents, and starts a new chain if a prerequisite failed or was cancelled | **Read** (bytecode of `EnqueueRunnable`: the `KEEP` test on `ENQUEUED`/`RUNNING`, `APPEND_OR_REPLACE`, `hasDependents`) | **Read** (`EnqueueRunnable.kt:175-251`; `ExistingWorkPolicy.kt:23-54`) |
| A one-time work that succeeds moves its blocked children to `ENQUEUED`. One that fails fails all its dependents. A periodic work never reaches `SUCCEEDED`. Newly unblocked work is scheduled after the run | **Read** (bytecode: `setSucceededAndResolve` calls `getDependentWorkIds` and `hasCompletedAllPrerequisites`; `iterativelyFailWorkAndDependents` exists). The scheduling-after-run path was not traced in 2.9.1 | **Read** (`WorkerWrapper.kt:453-471`, `:510-520`, `:554-574`; `Schedulers.java:60-80`) |
| Each new `WorkSpec` gets its own JobScheduler job id | Not read | **Read** (`SystemJobScheduler.java:130-138`) |
| The in-process greedy scheduler ignores work that has content-URI triggers, so only JobScheduler runs the trigger | Not read | **Read** (`GreedyScheduler.java:168-171`) |
| Below API 31 an expedited work runs as a foreground service through `getForegroundInfoAsync()`. The `CoroutineWorker` default throws "Not implemented" | Partly read (bytecode string "Not implemented" in `CoroutineWorker.getForegroundInfo`; the below-31 path was not read) | **Read** (`WorkForeground.kt:38-42`; `CoroutineWorker.kt:91-92`) |
| `WorkInfo.getPeriodicityInfo()` and `getNextScheduleTimeMillis()` exist | **Read** (bytecode of `WorkInfo`) | Not re-read (U-069's plan L11 lists the 2.12.0 accessors) |
| WorkManager declares an exported diagnostics receiver, action `androidx.work.diagnostics.REQUEST_DIAGNOSTICS`, protected by `android.permission.DUMP` | **Read** (the 2.9.1 aar manifest) | Not read |
| Test driver (`work-testing`): `setAllConstraintsMet`, `setInitialDelayMet`, `setPeriodDelayMet`. A content-URI trigger is one ordinary constraint flag. Periodic work after its first run needs `setPeriodDelayMet` before it runs again | **Read** (bytecode of `TestScheduler`: `isConstraintsMet` reads one flag; `isRunnableInternalState` requires `periodDelayMet` or `isFirstPeriodicRun` for periodic work) | **Not read** (`work-testing` 2.12.0 is not in the local cache) |

Platform, from the Android 36 sources in the SDK directory, read only:
- **Read.** `android/app/job/JobInfo.java:1763-1803`. Content-URI jobs cannot be periodic or persisted. To keep observing, schedule "a new JobInfo using the same job ID" before finishing. Only then does "the system ... continue monitoring for content changes, and propagate any changes it sees over to the next job you schedule".
- **Read.** `JobInfo.java:1812-1832`. The update delay restarts on each further change; the max delay caps the total wait. The builder defaults for both are `-1` (`:1270-1271`).
- **Read.** `android/content/pm/ServiceInfo.java:141-167`. The `dataSync` foreground-service type "has time limit of 6 hours starting from" Android 15. When the limit is hit, `Service.onTimeout` is called.
- **Read.** `android/app/Service.java:724-746`. From Android 12, apps targeting it "are not allowed to start foreground services from the background". From Android 14 a valid type is required.
- **Read.** `android/os/PowerManager.java:2698-2699`. On the power allow-list "the system will not apply most power saving features to the app. Guardrails for extreme cases may still be applied". `android/provider/Settings.java:1654-1660`: "most applications should not use this ... run the risk of showing to the user as high power consumers".
- **Not read.** The JobScheduler service side is not in the SDK sources. That covers the content observer, the quota and flexibility controllers, the default for an update delay of `-1`, and the per-bucket quota numbers. The record's 11 s from SMS to trigger line is consistent with a default update delay of about 10 s. That is not verified.

Legacy (`d095fe02^`), read:
- The content trigger was a recurring Firebase JobDispatcher job (`BackupJobs.java:168-175`).
- `SmsJobService.onStartJob` called `scheduleIncoming()` and returned `false` (`SmsJobService.java:72-77`). The dispatcher's own comment, kept at `:58-60`, warns that rescheduling a job with the same tag during its run stops the running job.
- With the default Google Play driver, the SMS receiver was **disabled**. The legacy `App` enabled it only with the old-scheduler setting: `setBroadcastReceiversEnabled(preferences.isUseOldScheduler() && preferences.isAutoBackupEnabled())`, `App.java:112`. So the legacy app's promptness, if it was prompt, did not come from the receiver.
- How the Play-services driver re-armed a recurring content-URI job is not in the repository. My memory is that the dispatcher re-scheduled such a job after each run, with no interval. That is **not verified** and is not used below.

### Ruling 1: mechanism 1, the shape of the fix

**Options weighed.**

| Option | Mechanism 1 | Cost | Verdict |
|---|---|---|---|
| (a) One-time content-triggered request, re-armed by the trigger worker | Gone, for SMS **and** call log (sent messages too) | Contract v4 by change record. One port operation added. Migration of the persisted periodic work. A guard | **Recommended** |
| (b) Restore the SMS-received broadcast path | Gone for incoming SMS only. Sent messages and the call log keep the 15-minute limit | Receiver exported (with `android:permission="android.permission.BROADCAST_SMS"` kept). `RECEIVE_SMS` requested at run time, with a prompt. Refusal must leave the trigger working. Mechanism 2 unchanged | Not recommended. It fixes less, at the price of a permission prompt. It was not the legacy default path either (above) |
| (c) Keep the periodic trigger and state the bound | Kept: a later message within 15 minutes of a trigger run waits until the period ends, up to about 16 minutes with the defaults | Text only | Fallback only, if the owner rejects (a) under PO-C below |

**Concrete shape of (a).**

1. **One builder, two enqueues, both in the adapter.** A private `buildContentTriggerRequest()` in `WorkManagerScheduler` builds a `OneTimeWorkRequest` of `BackupTriggerWorker`. It keeps the present constraints: SMS URI with descendants, and the call-log URI under the same two preferences (`WorkManagerScheduler.kt:224-230`). It has tag `CONTENT_TRIGGER`, no initial delay, no network constraint and no backoff (as today, U-069 ruling 2). Both the update delay and the max delay are left unset (−1, the platform default):
   - Stage 1 has no delay worth tuning. The user-visible debounce is stage 2, the `incomingTimeoutSecs` delay.
   - An explicit short update delay would only bring forward a run that then waits a minute anyway.
   - These settings act before the job starts, so they do nothing for the gap discussed in item 4.
2. **Registration** (`scheduleContentTrigger()`, from `App.rescheduleJobs()`) uses `enqueueUniqueWork("CONTENT_TRIGGER", ExistingWorkPolicy.REPLACE, request)`. `REPLACE` deletes whatever is under the name, periodic or one-time, `RUNNING` or not (`EnqueueRunnable.kt:235-250`). So it is also the migration (item 5). Keep the encryption-degraded gate (`:219-222`).
3. **Re-arm**, from inside `BackupTriggerWorker.doWork()`, through a new port operation `rearmContentTrigger(): ScheduledJob?`. The adapter is the only place that builds the request, and U-069 routes the worker through the port for exactly that reason. The adapter:
   - returns `null` without enqueuing when the encryption is degraded, auto backup is off, or `incomingTimeoutSecs <= 0`. These are the conditions under which `App.rescheduleJobs()` does not register (`App.java:376-383`). Without this, a run that races a settings change could re-arm a trigger that the user just switched off: `APPEND_OR_REPLACE` over a cancelled entry starts a new chain (`EnqueueRunnable.kt:194-217`);
   - reads `getWorkInfosForUniqueWork("CONTENT_TRIGGER").get()` (allowed: the worker runs on WorkManager's background executor). If any entry is periodic (`periodicityInfo != null`), a legacy registration survived. It then calls the `REPLACE` registration instead, which ends the running periodic run; this happens once. If any entry is `ENQUEUED` or `BLOCKED`, a successor already exists, so it returns without enqueuing. Without this check, every abnormal stop that re-runs the parent would lengthen the chain by one;
   - otherwise calls `enqueueUniqueWork("CONTENT_TRIGGER", ExistingWorkPolicy.APPEND_OR_REPLACE, request)`.
4. **The exact policy, and why the other three are unsafe from inside `doWork()`.**
   - `KEEP` silently drops the re-arm, because the running worker is itself a `RUNNING` entry under the name (`EnqueueRunnable.kt:224-232`, and the same test in 2.9.1). The trigger would fire once and never again. This is the classic trap.
   - `REPLACE` cancels and deletes the running worker (`:235-250`). It does work if it is the last statement, but WorkManager then records the run as cancelled and stops a job that is still running. This is the hazard the platform text (`JobInfo.java:1781-1786`) and the legacy dispatcher comment (`SmsJobService.java:58-60`) both warn about.
   - `APPEND` inherits a failed or cancelled predecessor's state (`ExistingWorkPolicy.kt:40-42`).
   - `APPEND_OR_REPLACE` makes the successor a `BLOCKED` child of the running work. WorkManager moves it to `ENQUEUED` and schedules it when the parent succeeds (`WorkerWrapper.kt:554-574`, `Schedulers.java:60-80`).
   - So the worker must **always return `Result.success()`**. A failed parent fails its successor (`WorkerWrapper.kt:510-520`), and the trigger would be dead until the next registration. Today a periodic trigger survives a failure (`resetPeriodic`, `:465-468`); a one-time one does not.
   - This changes one point of U-069's ruling ("an exception from the call is not caught", U-069 plan section 2.1 item 4). The worker catches `Exception` around `scheduleIncoming()` and around the re-arm, logs each at error level under the same tag, and returns success. The log line of `BackupTriggerWorker.kt:57-58` stays byte for byte (Existing Behavior to Preserve).
   - INV-1's wording changes as a consequence (below). During a trigger run there are briefly two non-terminal entries under `CONTENT_TRIGGER`: the `RUNNING` parent and its one `BLOCKED` successor.
5. **A content change that lands between the start of a run and the moment the successor is scheduled is not seen by the trigger. It is not lost to the backup.**
   - The platform carries changes seen during a run over only to a job scheduled with **the same job id** (`JobInfo.java:1769-1780`). WorkManager gives the successor a new `WorkSpec` and so a new job id (`SystemJobScheduler.java:130-138`). So the trigger does not see a change in that gap.
   - The run has already enqueued (U-069: through `scheduleIncoming()`) an `INCOMING` backup at least 60 seconds later; the smallest positive value in `arrays.xml:100-105` is 60.
   - The backup takes every message not yet backed up, whatever woke it. A message that lands in the gap is therefore in that backup. The only effect is that it does not restart the debounce, so it is backed up sooner, not later.
   - The gap is the trigger run (milliseconds: one read and one enqueue) plus WorkManager's post-run scheduling. Before the run starts, the platform's update delay also merges bursts.
   - The update delay and max delay do not matter here, because they act before the job starts. **Not verified on a device:** the length of the gap, and how the platform treats the job instance while it runs.
6. **Upgrade of an installed build.**
   - The persisted periodic `CONTENT_TRIGGER` names the same worker class. U-069 IC-2 keeps the class name and package, so it keeps firing after the upgrade until something replaces it.
   - Three things replace it: (i) `PackageReplacedReceiver` emits `AutoBackupSettingsChanged` (`PackageReplacedReceiver.java:20-29`), so `rescheduleJobs()` cancels and registers with `REPLACE`; (ii) if that emit is lost (`tryEmitEvent` can return `false`, `:24-28`), the first run of the old periodic work reaches the re-arm guard, which sees a periodic entry and calls the `REPLACE` registration; (iii) any later settings change.
   - Do **not** use `APPEND_OR_REPLACE` while a periodic entry exists. A periodic parent never succeeds, so its child would stay `BLOCKED` for ever and a new child would join on each run. The guard exists to prevent that.
   - The unique name stays `CONTENT_TRIGGER` (Existing Behavior to Preserve). A rename would solve the migration too, but it would leave the old name alive until `cancelAll()` runs. Each fire of the old periodic work would then also arm the new chain, so two triggers would run.
7. **What changes for the user and the system.** Stage 1 fires on every content-change report, after the platform's update delay. Each run REPLACEs the pending `INCOMING`, so the backup starts one configured delay after the **last** change of a burst. This is the trailing-edge debounce INV-4 names and the legacy `setReplaceCurrent(true)` gave.
   - A conversation in which messages come faster than one per delay postpones the backup until a pause of one delay. The periodic trigger hid this by firing at most once per 15 minutes.
   - The trigger job runs once per change burst, not at most four times an hour. Each run is a database read plus an enqueue, with no network. Incoming backups also become more frequent on busy days (PO-C).
   - During a restore, `RestoreWorker.kt:461` and `:551` insert rows. The trigger then fires repeatedly, merged by the update and max delay, and replaces `INCOMING` each time. That is harmless; the same happened at most once per 15 minutes before.
8. **Comments that become false** and must change in the same story (AC-6):
   - `WorkManagerScheduler.kt:57-62`, `:67-69` ("content-trigger periodic work"), `:92`, `:112-115` (the constant's comment; the constant stays for `scheduleRegular`), `:204-217`;
   - the class comment of `BackupTriggerWorker` in U-069's form ("enqueued by the CONTENT_TRIGGER [androidx.work.PeriodicWorkRequest]");
   - the javadoc of `scheduleContentTrigger` in `BackupScheduler.java`.

**What it does to the contract (CNTR-MODERNIZATION-004).**
- **INV-1.** The realization for `CONTENT_TRIGGER` changes from `enqueueUniquePeriodicWork(UPDATE)` to `enqueueUniqueWork(REPLACE)` for registration plus `APPEND_OR_REPLACE` for the re-arm. The clause "at most one non-terminal work item per unique name" (`:240`) needs a stated exception: for `CONTENT_TRIGGER`, at most one pending (`ENQUEUED` or `BLOCKED`) trigger, plus the one running during its own re-arm. The contract's breaking-change policy names "different `ExistingPolicy`" as a behavioural break (`:266`).
- **INV-4.** The substance stays: content-URI triggers, the two-stage debounce, delay = `getIncomingTimeoutSecs()`. Add three things: stage 1 is a one-time request re-armed by the triggered worker; there is no minimum interval between fires; and a change seen during a trigger run is covered by the delayed backup that the run enqueued (item 5). Update the example at `:369-380` (`recurring = true` becomes one-time plus re-arm; `existingPolicy` REPLACE plus `APPEND_OR_REPLACE`). Update the port text for `scheduleContentTrigger()` at `:65-68` ("recurring content-URI observer").
- **Port.** Add `rearmContentTrigger(): Unit` ("re-registers the trigger from inside the triggered worker; declines under the registration's own conditions"). Its only consumer is `BackupTriggerWorker`. `BackupScheduler.java` gets the method. The only implementation is `WorkManagerScheduler`; the tests mock the interface (`MainViewModelTest.kt:43`), so nothing else breaks at compile time. The javadoc of `BackupScheduler.java:36` ("Contract: CNTR-MODERNIZATION-004 v2 — all ten operations below") becomes v4 and eleven. Its "v2" is already stale against v3.
- **Version step.** v4, **breaking for the behavioural contract** under the policy's own definition (a different `ExistingPolicy`; INV-1 wording). **Not breaking for consumers**: no existing signature, `BackupType` value or `SchedulerState` variant changes, and one operation is added, as v2 did. It needs a **new change record** (CNTR-MODERNIZATION-004 v4 and DES-MODERNIZATION-005), approved before the code, in the same story:
  - DES-MODERNIZATION-005 `:89`, `:133-138`, `:279` (the `setRecurring` row: the content trigger is no longer "`PeriodicWorkRequest` for recurring types"), `:320`, `:436`, and a risk row next to `:495` ("trigger silently disarmed": the guard and the success-always rule);
  - the INV-1 to INV-4 tests stay the gate (Validation Rule 2).
- **Alternative considered and rejected:** no new operation, with `scheduleContentTrigger()` choosing `APPEND_OR_REPLACE` when it finds a `RUNNING` entry. It hides the self-enqueue case behind state. A settings change that races a running trigger would append instead of resetting, so the hard reset that the migration relies on would no longer be guaranteed.

**Safety-net correction (affects Description, "What the owner actually experiences").** The regular backup is not a reliable safety net after a reboot. `BootReceiver` calls `scheduleBootup()` (`BootReceiver.java:31-34`), which enqueues a **one-time** work under the unique name `REGULAR` with `REPLACE` (`WorkManagerScheduler.kt:273-281`). `REPLACE` deletes the periodic `REGULAR` work (`EnqueueRunnable.kt:235-250`; the same in 2.9.1). After the boot backup has run, no periodic regular backup exists until a settings change or a package replace.

Even then, `scheduleRegular()` uses `UPDATE` (`:192-197`). `UPDATE` throws "Can't update OneTimeWorker to Periodic Worker" while the finished one-time entry is still in the database (`WorkerUpdater.kt:141-160` in 2.12.0, checked before the cancelled case; the same message in the 2.9.1 bytecode), and the operation fails without an exception at the caller.

This was read from code and library, not observed. It is outside this bug and is recorded in BUG-026, "Observations outside this bug", for its own bug. It bears on AC-4 Option S, whose text must not call the regular backup a fallback "after reboot" until that bug is fixed.

### Ruling 2: mechanism 2, the quota

**What cannot be fixed.** The platform decides when an app in a low standby bucket may run jobs.
- Bucket 50 means "never used" (`UsageStatsManager.java:170-175`). An app the owner never opens gets little or no job quota.
- A phone in daily use does not lift this app's bucket by itself. The bucket follows use **of this app**: launching it, and possibly interaction with its notifications (not verified).
- The analyst's PO-4 answer decides how much this matters in practice.

**Expedited work: where it could fit, and why it does not.**
- The delayed `INCOMING` backup cannot be expedited ("cannot be delayed", `WorkRequest.kt:305`). The trigger cannot be expedited (content URIs, `:301-304`).
- Two-step idea: a short delayed, non-expedited debounce step, then an expedited backup. In the low-bucket case the delayed step is itself a regular job, and the record shows precisely such a delayed job held by `WITHIN_QUOTA` for 15 minutes. The expedited backup would start only when the held step runs, which is no earlier than today. In a good bucket regular jobs are not held, so nothing is gained there either.
- Expedited from the trigger worker directly (backup at once, no delay) breaks INV-4's debounce. It also does not help, because the trigger is itself a regular job held by quota (the record, 07:41:09).
- Costs on top. On the owner's oldest device (Android 11, API 30) WorkManager runs expedited work as a foreground service through `getForegroundInfoAsync()` (`WorkForeground.kt:38-42`). `BackupWorker` has no override, and the default throws (`CoroutineWorker.kt:91-92`). Expedited quota is separate and "stricter". Out of quota, `RUN_AS_NON_EXPEDITED_WORK_REQUEST` makes it an ordinary job, the case already in hand, and `DROP_WORK_REQUEST` loses the backup.
- **Not worth it.**

**Foreground service.**
- `BackupWorker` already promotes itself once it runs (`BackupWorker.kt:123`, `dataSync`, `:517-538`; `AndroidManifest.xml:65`, `:67`, `:127-129`). That affects a running job only.
- Starting a foreground service earlier, from the background, is restricted from Android 12 for apps targeting it (`Service.java:724-731`). The app targets 35, so this applies on the owner's Android 12-and-newer phones. Which exemptions apply here is not verified.
- The `dataSync` limit of 6 hours from Android 15 (`ServiceInfo.java:147-148`) is far above a backup's run time. It is irrelevant to start latency, but a long first backup on Android 15 and newer must stay below it.
- Not a fix for quota.

**Battery-optimisation exemption.** Being allow-listed exempts the app from "most power saving features" (`PowerManager.java:2698-2699`). Whether that includes the job quota and the standby bucket is not stated in what was read (**not verified**). Asking for it requires the `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission and a system dialog. The platform itself says most apps should not, and that they risk "showing to the user as high power consumers" (`Settings.java:1656-1660`). Do not add it. If the owner wants it, the owner can allow-list the app in the system battery settings by hand; the README may say so (PO-A).

**What the fix does.** No new code path for mechanism 2 (AC-4, Option S). The app's text tells the truth (AC-5). Proposed wording, for the owner to approve (PO-A):
- `app/src/main/res/values/strings.xml:170`, `ui_auto_backup_incoming_schedule_desc`: "When to back up incoming SMS (after arrival). Android may hold this back while the app is rarely used; the regular schedule then catches up."
- README FAQ (`README.md:298-302`), in place of "3 minutes here means that any incoming SMS will trigger a backup after 3 minutes.": "3 minutes here means that a backup starts about 3 minutes after the last incoming SMS; each new message restarts the wait. Android decides when background work may run: if you rarely open SMS Backup+, Android can hold the backup until you open the app or until it allows background work again. The regular schedule is the fallback."
- The 25 translated `ui_auto_backup_incoming_schedule_desc` strings (26 files with the default): either translate the condition, or delete the translated entries so that those locales fall back to the English text. Either satisfies AC-5. Deleting is recommended for a private app, because machine-translated caveats in 25 languages cannot be checked. Lint's missing-translation check must then be read in the gate; it is not verified here whether the baseline flags it.

### Ruling 3: testability

**What a unit test can show** (Robolectric with `WorkManagerTestInitHelper` and the test driver, as in `WorkManagerSchedulerTest.kt:42-81`; the worker built by U-069's test factory with a real `WorkManagerScheduler`). Each test must fail on today's code or on a named mutation (AC-8):
- **T1, re-arm without a period (AC-1 at unit level).** Register, then `setAllConstraintsMet(triggerId)`. The worker runs; `INCOMING` is enqueued; under `CONTENT_TRIGGER` there are exactly one `SUCCEEDED` entry and one `ENQUEUED` entry with a **new** id, no `periodicityInfo`, and the same content-URI triggers. Then `setAllConstraintsMet(newId)`, with **no** `setPeriodDelayMet`: it runs again. On today's periodic code the second run does not happen, because the 2.9.1 `TestScheduler` requires `periodDelayMet` after a periodic work's first run (read in the bytecode). So this test discriminates.
- **T2, first change kept (AC-2).** A fresh registration runs on its first `setAllConstraintsMet`.
- **T3, re-arm is idempotent.** Call `rearmContentTrigger()` twice while a successor is `ENQUEUED`: still exactly one `ENQUEUED` or `BLOCKED` entry. Mutation: remove the guard; the test sees two.
- **T4, policy.** A worker whose re-arm uses `KEEP` leaves no successor. Use the mutation, not a production switch.
- **T5, survives an exception.** A port whose `scheduleIncoming()` throws: the worker returns success and a successor is `ENQUEUED`. Mutation: remove the catch; the successor is `FAILED`.
- **T6, declines.** Auto backup off, `incomingTimeoutSecs` of 0 and −1, and degraded: no successor, result success.
- **T7, migration (IC-2 at unit level).** Enqueue today's periodic request under `CONTENT_TRIGGER` (built in the test), then call `scheduleContentTrigger()`: one entry, not periodic. And: run the trigger worker while only the periodic entry exists. The guard replaces it, and no `BLOCKED` entry hangs behind a periodic parent.
- **T8, debounce kept (AC-3).** Two trigger runs leave one non-terminal `INCOMING`, the second (BUG-023's AC-7 and AC-8 tests stay green).

**What only a device shows:**
- the platform's content observation and its update delay;
- the gap of ruling 1 item 5;
- minimum latency on a real job;
- quota, standby buckets and the `FLEXIBILITY` constraint seen in the record;
- process death between a parent's success and the successor's scheduling (WorkManager re-schedules `ENQUEUED` work at its next start; not verified on a device);
- the real `HiltWorkerFactory` path (U-069 IC-3).

The test driver models none of these. A content-URI trigger is one flag (bytecode of `TestScheduler.isConstraintsMet`).

**Device procedure** (run by the orchestrator; commands are read-only knowledge and **not verified** against the Android 16 image unless stated):
- Bucket: `adb shell am get-standby-bucket com.zegoggles.smssync` (used in the record). `adb shell am set-standby-bucket com.zegoggles.smssync active` sets it; `working_set`, `frequent`, `rare` and `restricted` are the other names. Set it **after** the last launch of the app, because a launch moves it to active. Whether bucket 50 ("never") can be set by command is not known: reproduce it with a fresh emulator start and no launch, as the record did.
- Scheduler: `adb shell dumpsys jobscheduler` (used in the record). `adb shell cmd jobscheduler get-job-state com.zegoggles.smssync <job-id>`. `adb shell cmd jobscheduler reset-execution-quota -u 0 com.zegoggles.smssync` and `adb shell cmd jobscheduler reset-schedule-quota` reset quota between runs. `adb shell cmd jobscheduler run -f com.zegoggles.smssync <job-id>` forces a job and **must not** be used in an acceptance run, because it bypasses the constraints under test.
- Power: `adb shell dumpsys battery`, recorded. `adb shell cmd battery unplug` before a quota run and `adb shell cmd battery reset` after. Whether charging lifts quota is not verified, and an emulator usually reports AC power.
- WorkManager's view: `adb shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS -p com.zegoggles.smssync`. The receiver and its `DUMP` permission were read in the 2.9.1 manifest; the output tag was not read.
- **Acceptance repeat of the record (AC-1, AC-2, AC-3, AC-9):**
  1. Install the fixed build over the old one with `adb install -r` (this covers IC-2 and AC-9 at the same time).
  2. Dump: one `CONTENT_TRIGGER` job without "Minimum latency".
  3. Set the bucket to active; record the bucket and the battery state.
  4. Send SMS A; six minutes later SMS B; 30 seconds after B, SMS C.
  5. Record every trigger line, every `scheduleIncoming` enqueue line and every `BackupWorker ... type=INCOMING` start.
  6. Expected: the trigger line for each of A and B within about 15 s; one backup start within delay + 60 s after A and one within delay + 60 s after C, which merges B and C.
  7. Read the bucket again at the end. Repeat once after a settings change.
- **Quota run (AC-4, Option S), information only:** the same, in a low bucket. The expected result is the record's: held until the app is opened. Its log lines are quoted under AC-5's wording check.

### Ruling 4: sequencing

**Recommended order:** U-069, then U-070 (sprint 024), then U-074, U-071, U-072 (sprint 025 as planned), then **the BUG-026 fix**, then **the BUG-025 fix**, then U-073, then the targetSdk 37 story (S5).

Why this costs least re-planning:
- U-071 is planned at a fixed start state. Landing either fix before it would move its baseline: test count, coverage, the scheduler files. U-072 changes no app code.
- U-073 is not planned yet. It can plan against the fixed trigger. Its "scheduler tests pass unmodified" rule (REQ-MODERNIZATION-016 AC-8) then guards the new tests across the move to WorkManager 2.12.0. The semantics this fix relies on were read in both versions and are the same.
- S5's Run R then reads a trigger that can fire again.
- If the owner's answers (PO-A to PO-C) arrive earlier, the two stories can move up to directly after U-074, at the price of re-baselining U-071.

What each neighbour must know:
- **U-069.** No change to its scope. It must keep the class name, package, log line and the Hilt form; the BUG-025 fix builds on them. Its plan item 13 (the periodic limit) stays true for its own build. BUG-025 changes one of its rulings: the worker will catch exceptions (ruling 1, item 4). U-069's tests that mock the port may need the new operation stubbed. That is a test edit the BUG-025 story lists, not a defect of U-069.
- **U-070.** No shared lines. If its device check reads incoming timing, it records the standby bucket and re-registers first (Fact 4).
- **U-074.** No shared lines (`BackupWorker`, `RestoreWorker`, the cancel collector). The BUG-025 fix does not touch `BackupWorker.kt`.
- **U-071, U-072.** Nothing.
- **U-073.** Plan against the one-time trigger. Re-check in work-testing 2.12.0 that T1 still discriminates; that version's `TestScheduler` was not read. Re-read `EnqueueRunnable`, `WorkerWrapper` and the converter at 2.12.0, as above, for the stop rule.
- **S5 (DES-MODERNIZATION-014, Run R).** With the fix, every provider change fires the trigger. Without it, Run R must re-register just before T0 and expect at most one fire per 15 minutes. In either case Run R must set the bucket to active and record it, because mechanism 2 can hold the "trigger at release" line whatever the fix.
- **BUG-026** goes first because it is smaller and touches `scheduleRegular()`. This story's AC-7 keeps `scheduleRegular()` unchanged, so the two must not be merged without amending AC-7.

### Ruling 5: the analyst's open questions

The story has no "Open questions" section (AC-4 cited a "Q3" that did not exist; corrected to point here). The six questions are the six "What to check before choosing" cells of the options table below, answered in order.

1. **Option 1 (one-time trigger).** (a) A change in the gap is not seen by the trigger, but it is covered by the backup the run enqueued (ruling 1, item 5). (b) `APPEND_OR_REPLACE` with the guard; `KEEP` drops the re-arm and `REPLACE` cancels the running worker (item 4). (c) `REPLACE` registration, the package-replace path and the guard's self-heal (item 6). (d) Change record, v4, behaviourally breaking, consumer-compatible.
2. **Option 2 (receiver).** It needs the exported receiver and the `RECEIVE_SMS` prompt, covers SMS only and does not touch the quota. It was off in the legacy default configuration (`App.java:112` at `d095fe02^`). Not needed once Option 1 is built. Removal is BUG-023 PO-3's separate change (PO-B).
3. **Option 3 (expedited).** No place in this path (ruling 2). Rejected.
4. **Option 4 (foreground service).** Background start is restricted from Android 12 (`Service.java:724-731`), and the exemptions are not verified. The `dataSync` 6-hour limit concerns run time, not start. Rejected.
5. **Option 5 (state it).** Adopted for mechanism 2, with the wording above. It needs the owner's approval of the text (PO-A).
6. **Option 6 (battery exemption).** The platform text promises "most power saving features" off; its effect on quota and bucket is not verified. Not requested by the app. It may be mentioned as the owner's manual choice.

### For the product owner

| Item | Question | What each option means for you | Recommendation |
|---|---|---|---|
| PO-A (the analyst's PO-1) | What delay is acceptable, and what should the app promise? | **Fix and state:** a second message within 15 minutes no longer waits for the period. When Android holds the app back (you rarely open it), the backup waits for Android, and the text says so. **State only:** nothing changes in the code; the text says that a later message can wait up to about 16 minutes, and longer when Android holds the app back. **A number** ("within 5 minutes"): cannot be promised while Android can hold the app back for as long as it likes | **Fix and state**, with the wording in ruling 2 |
| PO-B (the analyst's PO-3, BUG-023 PO-3) | Restore the SMS-received path? | **Restore:** a new permission prompt to receive SMS, for a benefit the fix already gives (and only for incoming SMS). **Do not restore:** no prompt; the trigger covers SMS, sent messages and the call log | **Do not restore.** Decide its removal under BUG-023 PO-3 |
| PO-C (the analyst's PO-2) | Accept more frequent incoming backups for faster backup? | With the fix, a backup starts one delay after each pause in your messages. Today there are at most about four an hour. On a busy day: more backups, more mobile data and battery, still one backup per pause. If you prefer fewer, set "Incoming schedule" to 3 or 30 minutes. No new notification beyond the backup's own | **Accept**; use the incoming delay as the control |

The analyst's PO-4 (how often the app is opened) and PO-5 (Wi-Fi only) remain information requests.

### Options the Solution Architect should weigh (listed by the analyst, not designed)

| # | Option | Mechanism 1 | Mechanism 2 | What to check before choosing |
|---|---|---|---|---|
| 1 | A one-time content-triggered request, enqueued again after each run (instead of the periodic one) | Fixed: a one-time request has no interval; its first-run time is `lastEnqueueTime + initialDelay` (zero; `WorkSpec.kt` calculateNextRunTime, 2.12.0) | Not changed | (a) Whether a change that arrives between the start of a trigger run and its next enqueue is missed (platform behaviour for content-URI jobs; not verified). (b) The unique-work policy for an enqueue from inside the running worker under the same name: `REPLACE` would cancel the running worker; `KEEP` keeps the running one (library semantics read in the library docs, not here; not verified). (c) The persisted `CONTENT_TRIGGER` periodic work on existing installs (IC-2). (d) INV-1, the contract example at `:369-380` (`recurring = true`) and DES-MODERNIZATION-005 `:436` change by a change record |
| 2 | A working SMS-received path (PO-3) | Fixed for incoming SMS only; the trigger's limit remains for sent messages and call-log | Not changed (the receiver enqueues the same job) | Manifest export and permission requirement; the run-time prompt; BUG-023's platform notes are from memory and need to be checked |
| 3 | Expedited work for the incoming backup (`setExpedited`) | Not changed by itself | Intended to reduce it: expedited jobs "bypass ... app standby" and have their own quota; out of quota they run as regular jobs (policy `RUN_AS_NON_EXPEDITED_WORK_REQUEST`) or are dropped (`DROP_WORK_REQUEST`) | **The conflict with the debounce:** an expedited request cannot be delayed and cannot carry a content-URI trigger (`WorkRequest.kt:301-305`, 2.12.0), so `INCOMING` as designed (a delayed unique request, INV-4) cannot be expedited as it is; the debounce would have to be implemented another way (and the CNTR changed). Also: `BackupWorker` has no `getForegroundInfo` (the owner's oldest Android, 11, is below API 31, where expedited work runs as a foreground service and the coroutine worker's default throws); the expedited quota is "stricter"; the jobs "may be deferred if the system is under heavy load" |
| 4 | Foreground service started on an incoming message | Not changed | Avoids the job scheduler only if the service can be started from where it is started | The platform's rules for starting a foreground service from the background on Android 12 and newer, and the type `dataSync` limits on newer levels: not verified. The app already declares the `dataSync` type (`AndroidManifest.xml:127-129`) for backups that are running |
| 5 | Leave the regular backup as the safety net and state the expectation honestly | Not changed | Not changed | This is Option S of AC-4: the owner-facing text (AC-5), no new mechanism. Costs nothing in code; leaves the lateness as it is. Needs PO-1 |
| 6 | Ask for the app to be exempt from battery optimisation or otherwise raise its bucket | Not changed | Possibly reduced | Whether it changes the standby bucket at all: **not verified**. Listed so the question is asked |

Options 1 and 5 can be combined. Option 3 cannot keep the debounce as designed (above); that is the Solution Architect's to resolve, and the owner's to approve if the debounce changes.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt` (`:116`, `:218-250`) | Registers the trigger as a 15-minute periodic request, `UPDATE` | To be decided by the Solution Architect (Options 1 and 3) |
| `app/src/main/java/com/zegoggles/smssync/worker/BackupTriggerWorker.kt` (`:53-74`) | Enqueues the delayed `INCOMING` work and returns `success`; does not re-register | Possibly re-enqueues itself (Option 1); U-069 changes the same lines |
| `app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java` (`:36`, `:81`) | Declares `scheduleContentTrigger()`; javadoc says v2, ten operations | Adds `rearmContentTrigger()`; javadoc to v4, eleven operations (Suggested Approach, ruling 1; added by the Solution Architect) |
| `app/src/main/java/com/zegoggles/smssync/App.java` (`:279-293`, `:373-385`) | Cancels everything and re-registers on a settings-changed event; also on package replace | None expected; the period reset on re-registration is a fact to document |
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` (`:123`, `:517-538`) | Sets the foreground notification when the run starts | Only under Option 3 or 4 |
| `app/src/main/AndroidManifest.xml` (`:17`, `:127-139`) | Receiver not exported; `RECEIVE_SMS` declared | Only under PO-3 |
| `app/src/main/res/values/strings.xml` (`:169-170`) and `values-*` translations; `README.md` (`:296-302`) | State a delay after arrival, with no condition | AC-5 |
| `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md`, `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` | Describe a recurring observer with no firing bound | AC-6, by change record |
| `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt` and a new test file | Registration only | AC-1, AC-3, AC-8 |

## Existing Behavior to Preserve

- The two-stage debounce (CNTR-MODERNIZATION-004 INV-4): no backup on the raw content change; the delayed unique work `INCOMING` replaces a pending one (`REPLACE`, INV-1); the delay is `incomingTimeoutSecs`.
- The first message after a registration fires the trigger at once (Fact 3; AC-2).
- The trigger worker's log line "BackupTriggerWorker.doWork: content-URI trigger fired; enqueueing delayed incoming backup with initialDelay=...s" (`BackupTriggerWorker.kt:57-58`), which CR-015's emulator record, DES-MODERNIZATION-014 D14 Run R and the record here all read.
- The unique work names `INCOMING` and `CONTENT_TRIGGER`, and the backup type `BackupWorker` infers from the tag.
- The trigger's registration conditions: SMS URI; call-log URI when call-log backup and call-log-after-call are on; the encryption-degraded gate (`WorkManagerScheduler.kt:219-222`); registered only when auto backup is on and the incoming delay is above 0 (`App.java:376-383`).
- The regular backup: interval, constraints, backoff, unique name (AC-7). The user-visible settings keep their keys and values (`auto_backup_incoming_schedule` values 60, 180, 1800, 3600, -1).
- `BackupWorker`'s own behaviour: foreground notification, backoff cap, retry on `MailException`, watermark rules (BUG-010, BUG-021), and the INV-2 network rule on `INCOMING`.

## Verification Steps

For QA, per acceptance criterion. Run Gradle in a shell without `MSYS_NO_PATHCONV`. Device steps are run by the orchestrator or QA under the owner's standing device instructions; the raw logs stay outside the repository and the story's evidence quotes the lines.

1. AC-1, AC-2, AC-9: repeat the record's measurement as in Steps to Reproduce (a): bucket read before and after; re-register; SMS A; SMS B six minutes later; record every trigger line and `BackupWorker ... type=INCOMING` start time. Repeat once after a settings change and once after `adb install -r`. Compare with the tolerance the story states.
2. AC-3: run BUG-023's single-flight tests; send two SMS 30 seconds apart and confirm one `INCOMING` run.
3. AC-4, AC-5: for the option chosen, repeat the measurement in bucket 50 (a never-used app); confirm the behaviour stated; read the strings and the README FAQ against the measurement.
4. AC-6: open the contract, the design and the code comments side by side with the code; confirm the change record exists and is applied.
5. AC-7: `git diff` over the files named; run the existing scheduler tests.
6. AC-8: read the story's statement of what the harness can show; confirm each unit test fails when the behaviour is removed (the story records the one-line change used).
7. AC-10: only if the receiver is restored or removed; as stated.
8. AC-11 and IC-1 to IC-3: run the Gradle gate; compare the authoritative test counts; run the upgrade-install check if the trigger type changed.

## Root Cause Analysis
<!-- Filled during planning phase -->

Intake note (not the planning-phase analysis). Mechanism 1: the content trigger was written in U-014 as a `PeriodicWorkRequest` at the WorkManager minimum interval (`55e4e4e7`), because WorkManager's content-URI trigger is a constraint on a work request and the request chosen was the one that recurs. That a periodic request can fire a content trigger only once per interval was not stated in the design, the contract or the review checklist (Fact 3 and "Was this a regression" above); CR-015 noticed it was not verified (`:667-668`) and BUG-023 listed it under Not verified. Mechanism 2: the platform's rule, not the app's code; the app has no way to be exempt from it apart from the options listed. The code also gives the owner no sign of either: the settings text states a delay without a condition.

## Technical Context

- **Severity: medium (the scaffold's value, confirmed).** No message is lost or duplicated by either mechanism: a delayed message is backed up by the next backup that runs, and the regular backup (every 2 hours by default) is the safety net, subject to the same quota (Fact 7). What goes wrong is a promise not kept: the setting and the README say a backup comes a set time after arrival and it can take 12 to 18 minutes on the emulator and, in a low bucket, until the app is opened. Against a higher rating: nothing is lost; the evidence is one emulator in the lowest bucket, which a phone in daily use is not expected to be in; the owner has not reported a problem; the app is private and unpublished, the owner's devices run Android 11 or newer; and BUG-021 (silent loss of messages) is rated medium, so a delay cannot be rated above it without inverting the scale this project has used (BUG-018 medium, BUG-019 medium, BUG-020 low, BUG-021 medium, BUG-022 low, BUG-023 medium, BUG-024 low). Against a lower rating: mechanism 1 applies on any device and any bucket, to every second message inside fifteen minutes (a conversation is exactly that case), and the stated expectation is explicit ("any incoming SMS"). It rises to high if the owner says prompt backup of incoming messages matters (for example, as a record that must reach Gmail soon) or if a measurement on the owner's phone shows hours.
- **Priority: medium.** Sequencing belongs to the orchestrator. Reasons recorded for the placement: it is not changed by U-069; it bears on the product goal that the app works on current Android, on PO-C, and on S5 (whose Android 17 measurement needs the trigger to fire, and which can proceed by re-registering to reset the period); the safety net exists; the owner's decisions (PO-1 to PO-3) are needed before the Solution Architect can finish the approach. If the owner treats timely incoming backup as important, raise to high.
- **Complexity: high.** Two mechanisms with different owners (the app, the platform); a contract and design change (INV-4); possible interaction with the debounce design (Option 3); a platform measurement that a unit test cannot carry; owner decisions that change the shape of the fix.
- **Origin: `code-review`** (kept from the scaffold). Found on 2026-11-21 in a plan review of story U-069 and measured on a device by the orchestrator the same day.
- **Product-owner context.** The app is private, for the owner's own use; no store, rollout or other-user exposure; the owner's devices run Android 11 or newer; the mail server is Gmail. The compile SDK is 35, minSdk 24 and targetSdk 35 today (`app/build.gradle:21-25`), targetSdk 37 is the plan of S5.
- **Library version.** WorkManager 2.9.1 in the app; 2.12.0 sources read; S4 moves the app (BUG-023 Technical Context).

### For the product owner (not decided here)

| Item | Question | Options, and what each means for you |
|---|---|---|
| PO-1 | What delay after arrival is acceptable for an incoming message backup, and what should the app tell you? | **(a) Keep the promise of the setting (about the chosen delay, 1 minute by default) even when the system holds the app back.** This needs code that asks the system for priority (Option 3 or 4) and may need a change to how the delay is applied; the system may still refuse. **(b) Accept that a later message within 15 minutes waits for the next period, and say so.** Up to about 16 minutes for a second message, more in a low standby bucket; the Incoming schedule text and the README state it; nothing else changes. **(c) Set a number you can live with, for example "within 5 minutes", and have the Solution Architect find what meets it.** Either (a) or a one-time trigger (Option 1) is needed for that |
| PO-2 | Do you accept a visible notification and more battery use for faster backup of incoming messages? | The app already shows a foreground notification while a backup runs (`BackupWorker.kt:123`). Priority scheduling (Option 3 or 4) may start work sooner and more often. If you prefer fewer wake-ups and a lower battery use, choose (b) in PO-1 |
| PO-3 (the SMS receiver, "PO-C" of the feasibility analysis, BUG-023 PO-3) | Remove the receiver, or restore it? | **Remove:** nothing you can observe today changes; the receiver has not worked since June; the trigger remains the only path; the limit of mechanism 1 remains. **Restore:** you get a new permission prompt (`RECEIVE_SMS`) and an exported receiver; second messages inside 15 minutes would no longer wait for the period (mechanism 1 gone for incoming SMS); the quota hold (mechanism 2) would remain; sent messages and call-log changes would still depend on the trigger. A restored receiver is only needed if Option 1 is not chosen |
| PO-4 (information, not a decision) | How often do you open the app on your phones, and which Android version do they run? | A phone where the app is opened daily will sit in a better standby bucket than the emulator. This tells whether mechanism 2 matters in practice. If you never open it, say so: that is the case where the regular backup too may run late |
| PO-5 (information, not a decision) | Is "Wi-Fi only" on? | Combined with BUG-023, incoming backups then wait for Wi-Fi as well. Not a decision |

## Supporting Documentation

- The measurement record: `sdlc/artifacts/build/research/2026-11-21-incoming-backup-delay-measurement.md`
- `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` (INV-4 `:243`, `:245`; Rule 5 `:277`; example `:369-389`)
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` (`:89`, `:133-138`, `:320`, `:379`, `:436`, `:495`)
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-toolchain-runtime-libraries-platform-level-design.md` (S4 library move, S5 targetSdk 37 and its measurement)
- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md` (`:30`, `:53-58`, `:87`)
- `sdlc/artifacts/stories/BUG-023-content-trigger-backup-skips-scheduling-checks.md` (the trigger worker; the receiver finding; PO-3; Not verified item 7 asked this question)
- `sdlc/artifacts/build/change-records/CR-015-cntr-004-content-trigger-pre-api-24-fallback-removed.md` (`:667-668`)
- `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` (`:2343-2353`, PO-C)
- `sdlc/artifacts/build/sprints/sprint-001/U-014/plan.md`, `review-code.md` (origin of the periodic trigger)
- Platform and library sources read locally, read-only: the Android 36 platform sources in the SDK directory (`android/app/usage/UsageStatsManager.java`, `android/app/job/JobInfo.java`, `android/app/job/JobScheduler.java`); the WorkManager 2.12.0 sources jar in the local Gradle module cache (`WorkRequest.kt`, `WorkSpec.kt`, `OutOfQuotaPolicy.kt`, `CoroutineWorker.kt`, `SystemJobInfoConverter.java`)

## Implementation Notes
<!-- Added by agents during build -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes

### Scope Concerns

Raised, not acted on.

1. **The record's wording "at app start".** The code reading (Fact 4) finds re-registration on settings changes, on package replace and on the call-log permission grant, not on an ordinary start. The record may be read as an app-start registration; the install-over at 07:42:47 explains the lines. If a start-time registration exists that the search missed, the period reset would happen at every launch, which would matter for AC-9. The raw log would settle it.
2. **The README FAQ and the setting text are older than the migration.** They describe the legacy behaviour ("any incoming SMS will trigger a backup"). Whether they were ever true is the unverified legacy question above.
3. **The first message in a low bucket is also delayed (mechanism 2).** A fix to mechanism 1 alone leaves SMS A's 17 min 50 s in the record unchanged.
4. **The tolerance of 60 seconds in AC-1, AC-2 and AC-4 is the analyst's proposal**, from the first message's 11 s in the record. The owner's answer to PO-1 may replace it.

### Observations outside this bug (raised, not part of the acceptance criteria)

- **Regular schedule "Never".** The option "Never" has the value -1 (`arrays.xml:83-90`). `App.rescheduleJobs()` calls `scheduleRegular()` whenever auto backup is on (`App.java:376-377`), and `scheduleRegular()` uses `maxOf(regularTimeoutSecs, 900)` (`WorkManagerScheduler.kt:180`), which for -1 is 900 s, so a "never" setting would register a 15-minute backup. The legacy `schedule()` scheduled nothing for a non-positive value (`BackupJobs.java:132`). Read from the code only; not run, and `scheduleRegular` has a test that was not read. If correct, it is a separate defect (and bears on the "safety net" statement above); it is not covered here and AC-7 keeps `scheduleRegular` unchanged.
- **`setForeground` can throw under foreground-service restrictions.** The library says so (`CoroutineWorker.kt:108-111`), and `BackupWorker` calls it as the first statement of `doWork`, outside any `try` (`BackupWorker.kt:123`). Whether that can happen for a job started in the background on Android 12 and newer was not examined.

### Not verified

Everything below could not be established from the code, documents and sources read, and is not assumed.

1. Anything about a physical device or any standby bucket other than 50 and 10. The record is one emulator, two messages, one run.
2. Whether the legacy trigger (Firebase JobDispatcher, Play services driver) fired for a second SMS inside fifteen minutes. The dispatcher's code is not in the repository; no legacy measurement was found. So "regression from the legacy app" is not claimed.
3. That the second SMS would have fired the trigger at the end of the period and been backed up one delay later had quota been available (the record's stated limit). Expected from the library logic, not observed.
4. The library logic of Fact 3 for WorkManager 2.9.1 (the version in use). It was read in the 2.12.0 sources.
5. The platform's tables: how long each bucket may defer jobs, how much job quota each bucket gets and in what window, and the expedited-job quota. The public SDK sources read give the bucket meanings and the expedited text only. The platform's documentation pages were not fetched.
6. Whether an app exempt from battery optimisation gets a better bucket; which events allow starting a foreground service from the background on Android 12 and newer; the limits of the `dataSync` foreground service type on newer levels.
7. Whether a content change that arrives between the start of a trigger run and the next enqueue is missed, for a one-time trigger (Option 1).
8. Whether the WorkManager test driver models the content-URI re-arm and the periodic minimum latency, and the command that sets the standby bucket for the isolating run.
9. The raw log of the record (outside the repository): whether it shows a registration at app start, and what the library logged for the trigger job.
10. Whether the Android 36 platform source version equals what the Android 16 emulator runs, and what Android 17 does.
11. Where the owner's phones sit in the standby buckets, and how often the owner opens the app.
12. The "Never" regular schedule observation: read from the code, not run. (Now BUG-026, which confirms it from the code.)

Added by the Solution Architect:

13. Item 4 is now read for 2.9.1, in the bytecode (Suggested Approach, facts table). Item 7 is answered from the platform text and the library: such a change is not seen by the trigger but is covered by the pending backup. The gap's length on a device is still not verified. Item 8 is answered for the test driver of `work-testing` 2.9.1; 2.12.0 is not read. The device commands are listed and are not verified.
14. JobScheduler's service side: the default for an update delay of `-1`, the quota and flexibility controllers, whether charging lifts quota, and how the platform treats a content-URI job instance while it runs.
15. Whether the battery-optimisation allow-list lifts the job quota or the standby bucket. Which foreground-service start exemptions apply to this app on Android 12 and newer.
16. The legacy Firebase JobDispatcher's re-arm of recurring content-URI jobs (recalled, not read).
17. That `am set-standby-bucket`, the `cmd jobscheduler` subcommands named in ruling 3 and `cmd battery unplug` behave as described on the Android 16 emulator.
18. The reboot effect on the regular backup (Safety-net correction): read from the code and the library, not observed on a device.
