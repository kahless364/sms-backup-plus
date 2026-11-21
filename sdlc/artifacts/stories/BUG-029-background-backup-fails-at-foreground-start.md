---
type: bug
status: ready
artifact_type: bug
severity: high
priority: high
complexity: medium
parallel_eligible: true
iteration: 1
requirements:
  - REQ-MODERNIZATION-005
  - REQ-MODERNIZATION-013
  - REQ-MODERNIZATION-015
design_docs:
  - DES-MODERNIZATION-005
integration_contracts:
  - CNTR-MODERNIZATION-004
dependencies: []
related_items:
  - BUG-016
  - BUG-024
  - BUG-025
  - BUG-026
  - BUG-027
  - U-003
  - U-049
  - U-069
  - U-070
  - U-071
  - U-073
  - U-074
  - CNTR-MODERNIZATION-004
  - DES-MODERNIZATION-005
  - REQ-MODERNIZATION-005
  - REQ-MODERNIZATION-013
  - REQ-MODERNIZATION-015
platforms: []
tags:
  - workmanager
  - foreground-service
  - android-12
  - background-start-restriction
  - backup
  - restore
  - automatic-backup
  - device-observed
  - product-owner-decision
id: BUG-029
title: Every automatic backup that starts while the app is not on screen fails at once on Android 12 and later, because the worker's first statement asks for a foreground service and the system refuses
domain: build
origin: qa
updated_at: '2026-11-21T16:14:43.452Z'
---

# BUG-029: Every automatic backup that starts while the app is not on screen fails at once on Android 12 and later, because the worker's first statement asks for a foreground service and the system refuses

## Description

On Android 12 and later, an automatic backup (incoming, regular, after a restart, or started by a third-party broadcast) that starts while the app is not on screen does no work. The first statement of `BackupWorker.doWork` asks WorkManager to move the worker into a foreground service. The system refuses that start for an app in the background. The refusal escapes `doWork`, and WorkManager records the run as failed. No message is read and nothing is uploaded. A backup that starts while the app is on screen succeeds. So automatic backup works only when the owner opens the app.

Seen on the Android 16 emulator on 2026-11-21 (observation record `sdlc/artifacts/build/research/2026-11-21-background-backup-and-boot-observation.md`, "Run 1", "Run 2", "What it shows" item 1). BUG-025 had named this as a risk read from the library, not examined (BUG-025, Notes, "Observations outside this bug", "`setForeground` can throw under foreground-service restrictions").

Every code statement below was checked against commit `33706a13` (`git show`, not the working tree, which has another agent's uncommitted changes in `app/`). Line numbers are that commit's. Each statement is marked:
- **observed**: from the record or the sanitized app log `sdlc/artifacts/build/research/evidence/2026-11-21-background-backup-app-log.txt` (cited as "log :N");
- **read**: code, the WorkManager 2.9.1 bytecode (`javap` on the AAR in the local Gradle cache), the WorkManager 2.12.0 sources (sources JAR in the local Gradle cache), or the Android 16 (API 36) SDK sources;
- **not verified**.

The app uses WorkManager 2.9.1 (`app/build.gradle:155-156`), targetSdk 35 and minSdk 24 (`app/build.gradle:24-25`).

**The chain, confirmed:**

1. **The worker asks for the foreground first, outside any `try`.**
   - `BackupWorker.doWork()` begins at `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt:118`. Its first statement is `setForeground(createBackupForegroundInfo())` (`:123`) (read).
   - The `try` that maps exceptions to results starts at `:146`. It covers the transport and the backup (`:146-171`), not `:123`. So nothing in the worker catches an exception from `:123` (read).
   - `createBackupForegroundInfo()` (`:516-539`) builds the ongoing "Backup" notification on `App.CHANNEL_ID`, id `BACKUP_NOTIFICATION_ID = 1` (`:545`), with type `FOREGROUND_SERVICE_TYPE_DATA_SYNC` on API 29 and later (`:534-535`) (read).
   - The call is made for **every** backup type. The type is inferred from the tags only afterwards (`:142`, `:501-507`) (read).
   - `RestoreWorker.doWork()` has the same first statement, `setForeground(createRestoreForegroundInfo())` (`app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt:148`). It is also before its `try` (`:165`) (read).
   - The manifest overrides WorkManager's `SystemForegroundService` with `foregroundServiceType="dataSync"` (`app/src/main/AndroidManifest.xml:126-129`, the BUG-016 fix). It declares `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` and `POST_NOTIFICATIONS` (`:65`, `:67`, `:69`) (read). So the refusal is not a missing type or permission.
2. **What `setForeground` does (WorkManager 2.9.1, read).**
   - `CoroutineWorker.setForeground` calls `setForegroundAsync(info)`. That goes to `WorkerParameters.getForegroundUpdater().setForegroundAsync(...)` (`ListenableWorker.setForegroundAsync`). The suspend function then waits for the returned future.
   - In production the updater is `WorkForegroundUpdater`. `WorkerWrapper` builds it for every run. Its task (`WorkForegroundUpdater$1.run`) calls `ForegroundProcessor.startForeground(id, info)`, which is `Processor.startForeground`. It then posts the notification through `startService`. Any `Throwable` goes into `SettableFuture.setException` (exception table `0-109 → 112`, type `Throwable`).
   - `Processor.startForeground` (source lines 195-209):
     - logs "Moving WorkSpec (…) to the foreground" (log :10, :41);
     - moves the `WorkerWrapper` from `mEnqueuedWorkMap` into `mForegroundWorkMap`;
     - acquires the `ProcessorForegroundLck` wake lock;
     - **then** calls `ContextCompat.startForegroundService(…)` (line 206; the stack in log :14 and :45 shows `Processor.java:206`).
   - 2.12.0 is the same (`androidx/work/impl/Processor.java:192-208`).
3. **The system refuses the start (observed).**
   - `ContextImpl.startForegroundService` throws `android.app.ForegroundServiceStartNotAllowedException: startForegroundService() not allowed due to mAllowStartForeground false: service com.zegoggles.smssync/androidx.work.impl.foreground.SystemForegroundService` (log :20, :51).
   - Platform, read: `Service.startForeground` javadoc, "Beginning with SDK Version S, apps targeting SDK Version S or higher are not allowed to start foreground services from the background", and `@throws ForegroundServiceStartNotAllowedException` "if the service is restricted from becoming foreground service due to background restriction" (SDK sources `android/app/Service.java:723-727`, `:748-751`).
   - Hierarchy, read: `ForegroundServiceStartNotAllowedException extends ServiceStartNotAllowedException` (`ForegroundServiceStartNotAllowedException.java:26-27`), which `extends IllegalStateException` (`ServiceStartNotAllowedException.java:24`).
   - The type errors of Android 14 are **siblings**, not subclasses: `InvalidForegroundServiceTypeException` and `MissingForegroundServiceTypeException` extend `ForegroundServiceTypeException extends ServiceStartNotAllowedException` (`ForegroundServiceTypeException.java:24`; `InvalidForegroundServiceTypeException.java:25-26`; `MissingForegroundServiceTypeException.java:25-26`). This matters for what the fix may catch (Suggested Approach, "Exactly what is caught").
4. **How the refusal reaches the worker (2.9.1, read).**
   - The exception is set on the future (item 2). `setForeground` unwraps it in both branches.
     - If the future is already done, it calls `get()`, catches `ExecutionException` and throws its cause (`CoroutineWorker.setForeground`, bytecode offsets 25-52).
     - Otherwise the listener `ListenableFutureKt$await$2$1.run` catches the throwable of `get()`, takes `getCause()`, and resumes the coroutine with that cause. It cancels the continuation only when the throwable is a `java.util.concurrent.CancellationException`.
   - So `doWork` sees `ForegroundServiceStartNotAllowedException` itself, thrown at `BackupWorker.kt:123`.
   - It escapes `doWork` (item 1). `CoroutineWorker$startWork$1` sets it on the worker's result future (`setException`).
5. **WorkManager records a failure, not a retry (2.9.1, read; observed).**
   - `WorkerWrapper$2.run` calls `get()` on the worker's result future. The `ExecutionException` is logged as "… failed because it threw an exception/error" (log :21-22, :52-53).
   - `mResult` keeps its initial value `Result.failure()` (constructor, offsets 5-8). `onWorkFinished` → `handleResult(Failure)` logs "Worker result FAILURE for …" (log :23, :54).
   - For one-time work it calls `setFailedAndResolve()`, which is final. For periodic work it calls `resetPeriodicAndResolve()`, so the next period is scheduled (`handleResult`, offsets 109-159).
   - The worker's own retry rules (`Result.retry()` on `MailException`, `BackupWorker.kt:164-167`) are never reached.
   - 2.12.0 also gives `Resolution.Failed` for any non-cancellation throwable (`androidx/work/impl/WorkerWrapper.kt:379-385`).
   - Consequences:
     - an incoming, boot or broadcast backup that fails this way is not retried; the next one waits for the next trigger;
     - a periodic regular backup fails again at every period that starts off screen. That is read, not observed: no periodic regular backup existed after the restart (record, "What it does not show"; BUG-027).
6. **Which runs were refused and which were allowed (observed).**

   | Time | Work | App state | Result |
   |---|---|---|---|
   | 09:27:45 | INCOMING (left over from before the restart) | not opened; under a second after `BOOT_COMPLETED` (log :2, :4-8) | entered the foreground, SUCCESS |
   | 09:28:57 | REGULAR (the boot backup), 72 s after boot | not opened | refused, FAILURE (log :10-23) |
   | 09:46:36 | REGULAR (the boot backup, held by quota until then) | on screen (opened 09:46:34 to 09:46:38) | entered the foreground, backed up 1 SMS, SUCCESS (log :29-40) |
   | 09:47:36 | INCOMING, 58 s after Home | not on screen | refused, FAILURE (log :41-54) |

   - `BackupWorker` logged nothing for the refused runs: "starting backup" (`:144`) comes after `:123` (log :10-24 and :41-55 have no `SMSBackup+` line).
   - The device was not exempt from battery optimisation (record, "Set-up").
   - Why the 09:27:45 start was allowed: the record does not say. The platform's exemption list was not read (Not verified 2).
7. **Who is affected.**
   - Every automatic backup type runs `BackupWorker` with its tag and no foreground handling of its own: `scheduleIncoming` (`WorkManagerScheduler.kt:133-153`, initial delay `:148`), `scheduleRegular` (periodic, `:170-197`), `scheduleBootup` (60 s delay, `:262-281`) and `scheduleImmediate` (`BROADCAST_INTENT`, third-party automation, `:303-316`).
   - `scheduleManual` (`MANUAL`/`SKIP`, `:341-357`) and `scheduleRestore` (`:380-392`) are started from the screen. They are affected only when the run starts after the user has left, for example a retry 30 s or more later (`EXPONENTIAL` 30 s backoff, CNTR-MODERNIZATION-004 INV-3).
   - On Android 11 and earlier the restriction does not exist (Service.java `:723-727`, "Beginning with SDK Version S").
   - The owner's phones run Android 11 or later (orchestrator). Every one on Android 12 or later is affected. Which those are is not known here (Not verified 10).
8. **Nothing is lost; everything is delayed.**
   - A refused run stops before it reads the provider. The watermark advances only after a confirmed append (`appendBatchAndUpdateWatermark`, `BackupWorker.kt:405-425`).
   - So the next run that is allowed backs up everything above the watermark, like the 09:46:36 run (log :34-38). Today that is the next time the owner opens the app while a backup starts.

## Steps to Reproduce

The procedure of run 2 in the record. It was observed once on 2026-11-21 with the debug build of story U-068. The orchestrator runs it on the emulator `Pixel_9_Pro` (Android 16, API 36) only, never `Pixel_10_Pro`. Use `MSYS_NO_PATHCONV=1` only for `adb` commands with a `/sdcard` path, never for Gradle.

1. **Set-up.**
   - Debug build installed (`com.zegoggles.smssync`).
   - Mail account configured; auto backup on; first backup done.
   - Incoming delay 1 minute; regular schedule at its default.
   - No battery-optimisation exemption: `adb -s <serial> shell dumpsys deviceidle whitelist` has no line for the package.
2. **Cold start** the emulator without saving a snapshot (`-no-snapshot-save`). Wait until `adb -s <serial> shell getprop sys.boot_completed` prints `1`. Do not open the app.
3. **Run-1 variant (optional, the boot backup).** Wait about two minutes with network, then read `adb -s <serial> logcat -d | grep -E "SMSBackup\+|WM-WorkerWrapper|WM-Processor"`.
   - Expected on today's code: "enqueued bootup REGULAR with 60s delay", then about 60 s later "Moving WorkSpec (…) to the foreground", the `ForegroundServiceStartNotAllowedException` and "Worker result FAILURE … REGULAR".
   - In run 2 the boot job was instead held by job quota (standby bucket 50, BUG-025). `adb -s <serial> shell am get-standby-bucket com.zegoggles.smssync` and `dumpsys jobscheduler` show why it has not run.
4. **Open the app for about three seconds, then press Home.** This moves the app out of bucket 50 (record, 09:46:34 to 09:46:38). A boot backup held until then may start during these seconds and succeed. That is expected.
5. **Send one test SMS from the emulator console**: `adb -s <serial> emu sms send 5550100 "BUG-029 test"`. Any sender number and text without personal data will do. Or send it before step 4, as in run 2.
6. **Wait at least 90 s with the app not on screen.** The content trigger enqueues an incoming backup with the 1-minute delay (log :32). It starts while the app is in the background (run 2: 58 s after Home).
7. **Observe:** `adb -s <serial> logcat -d | grep -E "SMSBackup\+|WM-WorkerWrapper|WM-Processor|WM-SystemFgDispatcher"` shows, for the work tagged `INCOMING`:
   - "Moving WorkSpec (…) to the foreground";
   - `ForegroundServiceStartNotAllowedException … mAllowStartForeground false`;
   - "Worker result FAILURE";
   - **no** "BackupWorker.doWork: starting backup, type=INCOMING" line.
   The SMS is not in the mail account. The WorkManager diagnostics (`adb -s <serial> shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS -p com.zegoggles.smssync`, BUG-027 Steps B.2) list the work as failed.
8. **Work-around check:** open the app and trigger a backup (or wait for the next trigger while on screen). The SMS is backed up.

## Expected Behavior

An automatic backup backs up whether or not the app is on screen. When it cannot, or should not, run as a foreground service, it runs as ordinary background work. It ends with the result its own work gives: success, the existing `MailException` retry, or the existing failure reasons. The refusal of a foreground start is never the reason a backup fails. A backup or restore the user starts from the screen still shows the "Backup" or restore notification as today. Scheduling is unchanged.

## Actual Behavior

On Android 12 and later, a backup worker that starts while the app is in the background fails at its first statement:
- WorkManager logs `ForegroundServiceStartNotAllowedException` and "Worker result FAILURE";
- the worker logs nothing;
- a one-time backup is not retried, and a periodic one fails again at its next period if the app is again off screen.

The observed runs: the boot backup at 09:28:57 and the incoming backup at 09:47:36. New messages wait until the owner opens the app at a moment when a backup starts.

## Evidence

| Evidence | What it shows |
|---|---|
| `sdlc/artifacts/build/research/2026-11-21-background-backup-and-boot-observation.md` (Set-up, Run 1, Run 2, "What it shows" 1-2, "What it does not show") | The device, build and timing; refused in the background, allowed on screen or right after boot; the failure is final for that run |
| `sdlc/artifacts/build/research/evidence/2026-11-21-background-backup-app-log.txt` :10-24, :41-55 | The two refusals: `Processor.java:206`, `WorkForegroundUpdater.java:100`, the exception text, "failed because it threw", "Worker result FAILURE" |
| same log :4-9, :29-40 | Two runs that entered the foreground and succeeded (one under a second after boot, one on screen) |
| `service/BackupWorker.kt:118-172` (`:123`, `:142`, `:146`), `:516-539`; `service/RestoreWorker.kt:148`, `:165` | First statement `setForeground`, outside the `try`, for every type |
| `AndroidManifest.xml:65-69`, `:126-129` | Type and permissions are declared; the refusal is the background restriction |
| WorkManager 2.9.1 bytecode (read): `CoroutineWorker.setForeground`, `ListenableFutureKt$await$2$1`, `ListenableWorker.setForegroundAsync`, `WorkForegroundUpdater$1.run`, `Processor.startForeground` / `stopWork`, `WorkerWrapper` (`mResult`, `$2.run`, `handleResult`, `tryCheckForInterruptionAndResolve`), `WorkForegroundRunnable`, `WorkRequest$Builder`, `PeriodicWorkRequest$Builder`, `SystemJobInfoConverter`, `SystemJobService.onStopJob` | The exception path, the result, and what expedited work allows |
| WorkManager 2.12.0 sources (read): `CoroutineWorker.kt:105-117`, `impl/Processor.java:192-208`, `:240-244`, `impl/WorkerWrapper.kt:376-386`, `impl/foreground/SystemForegroundService.java:189-204` | Same behaviour in the version U-073 moves to |
| SDK sources, API 36 (read): `Service.java:723-727`, `:748-760`, `:1203-1216`; `ForegroundServiceStartNotAllowedException.java:26-31`; `ServiceStartNotAllowedException.java:24`; `ForegroundServiceTypeException.java:24`; `content/pm/ServiceInfo.java:140-173`; `job/JobScheduler.java:90-96`; `job/JobInfo.java:281-286`; `job/JobParameters.java:205-213` | The restriction from S, the exception hierarchy, the dataSync 6-hour limit from Android 15, job execution limits |
| `git log -S` on `BackupWorker.kt` / `RestoreWorker.kt`; U-049 story `:36`, `:49-50`, `:68`; U-049 `qa-results.md:35`; `SmsBackupService.java:216-219` (master) and `:386-389` (`4b966da2^`) | When it entered; legacy promoted only manual backups |

## Acceptance Criteria

The unit criteria are checked in Robolectric with `TestListenableWorkerBuilder`, as in `BackupWorkerTest.kt:33-58`.
- **Foreground updater.** The builder has a public `setForegroundUpdater(ForegroundUpdater)`. Its default is `TestForegroundUpdater`, which completes the future at once (both read in the work-testing 2.9.1 bytecode). A test makes `setForeground` fail by passing a `ForegroundUpdater` whose `setForegroundAsync` throws `ForegroundServiceStartNotAllowedException("test")` (public constructor, `ForegroundServiceStartNotAllowedException.java:31`, API 36 sources). `ListenableWorker.setForegroundAsync` calls the updater directly (read), so the exception reaches `doWork` unwrapped.
- **Failed-future variant.** A second variant returns an already-failed future, which covers the `isDone` branch of item 4. It needs a `ListenableFuture` helper on the test classpath; whether one is there is not verified (Not verified 8). If none is, the throwing updater alone suffices: both branches throw the cause (item 4).
- **SDK level.** Tests of the refusal run at `@Config(sdk = [35])` or another level of 31 or above, because the exception class exists from API 31 and the fix guards on `SDK_INT`. The project pins `sdk=29` globally (`app/src/test/resources/robolectric.properties:8`) and per class. Whether Robolectric 4.15.1 resolves sdk 35 offline is Not verified 7.

"Start" is the commit the fix story starts from. "Automatic types" are `INCOMING`, `REGULAR` and `BROADCAST_INTENT`. "User-started types" are `MANUAL` and `SKIP` (`BackupType.isBackground()`, `service/BackupType.java:37-39`). Test names are working names.

- [ ] AC-1 (a backup whose foreground start is refused still runs and backs up): Given a `MANUAL` run, a throwing foreground updater, one SMS row served by story U-070's provider double and a recording transport (U-070's fixtures as built, not edited), when `doWork()` runs, then it returns `Result.Success`, the transport saw one append, and the SMS watermark equals the confirmed date. Test `BackupWorkerForegroundTest.manual_foregroundRefused_continuesAndBacksUp`, red at Start: today `doWork` throws.
- [ ] AC-2 (the refusal is logged once, at warning, with no personal data): In AC-1's run, `ShadowLog` holds:
  - exactly one `Log.WARN` entry with tag `SMSBackup+`, with the fixed text "BackupWorker: foreground start not allowed; continuing without the foreground" (the plan fixes the text) and the exception's class name;
  - no `Log.ERROR` entry with tag `SMSBackup+`;
  - no entry that contains the row's body, its address, the account name or a folder name.

  The throwable may be attached: its message names only the package and WorkManager's service (log :20). The level survives in release builds: no `-assumenosideeffects` rule for `android.util.Log` exists (search of `app/` at `33706a13`). The same holds for the restore (AC-7) with the prefix "RestoreWorker:". Checked by AC-1's test and AC-7's test.
- [ ] AC-3 (automatic backups do not ask for the foreground; recommended option, see Notes PO-1): For each automatic type (`INCOMING`, `REGULAR`, `BROADCAST_INTENT` as the tag), `doWork()` never calls the foreground updater (a counting updater records 0 calls), and the run reaches the backup path. The test factory's transport throws `MailException`, so the result is `Result.retry()` as today. Test `automaticTypes_doNotRequestForeground`, red at Start (1 call today). If the owner answers PO-1 "yes", this criterion is replaced by: automatic types ask once, and AC-1 and AC-2 hold for each automatic type.
- [ ] AC-4 (a backup that can enter the foreground still does): For `MANUAL` and `SKIP`, `doWork()` calls the updater exactly once, before any provider query or transport call. The `ForegroundInfo` it passes has notification id `BACKUP_NOTIFICATION_ID` (1), the channel `App.CHANNEL_ID` and, on API 29 and later, type `FOREGROUND_SERVICE_TYPE_DATA_SYNC`. Today's `createBackupForegroundInfo()` is unchanged. Test `userStartedTypes_requestForegroundOnce`, green at Start and after.
- [ ] AC-5 (nothing else is swallowed):
  - (a) An updater that throws `IllegalStateException("x")`, makes `doWork()` throw that exception, as today.
  - (b) An updater that throws `InvalidForegroundServiceTypeException("x")` makes `doWork()` throw it (a BUG-016-type manifest regression must stay loud).
  - (c) A `SecurityException` from the updater propagates.
  - (d) An updater that cancels the running job and then throws `CancellationException("test")` makes `doWork()` end by throwing a `CancellationException`, with no warning line and no transport or provider call.

  Tests `foreground_otherIllegalState_propagates`, `foreground_typeException_propagates`, `foreground_securityException_propagates`, `foreground_cancelled_endsCancelled`. All green at Start: today everything propagates.
- [ ] AC-6 (no change to scheduling): `git diff --name-only <start> -- app/src/main/java/com/zegoggles/smssync/scheduler app/src/main/java/com/zegoggles/smssync/worker app/src/main/AndroidManifest.xml app/src/main/java/com/zegoggles/smssync/receiver` is empty. `WorkManagerSchedulerTest.kt` is unedited. No request gains or loses `setExpedited`, a delay, a constraint, a tag or a unique name. Checked by `git diff`.
- [ ] AC-7 (restore gets the same handling): `RestoreWorker.doWork()` still asks for the foreground first. With a throwing updater it continues and runs the restore. With today's test transport factory, the result the transport gives is `Result.retry()` on its `MailException`, `RestoreWorker.kt:194-197`; the plan may use the existing `TestableRestoreWorkerFactory` fixtures instead. It writes AC-2's warning once (prefix "RestoreWorker:"), and AC-5 (a), (b) and (d) hold for it. Tests in a new `RestoreWorkerForegroundTest`. The refusal test is red at Start.
- [ ] AC-8 (what the user sees):
  - Automatic backups show no notification while they run, as in the legacy app (Root Cause Analysis).
  - A manual backup or restore started on screen shows today's ongoing notification.
  - A manual backup or restore whose foreground start is refused shows no notification and runs. The status screen ignores progress of background types unless it is an error (`activity/StatusPreference.java:230-234`). A manual run's progress reaches it through `setProgress`, which does not depend on the foreground. The plan confirms this by reading `MainViewModel`.

  Checked by AC-3, AC-4 and the device criterion AC-10 (screenshots).
- [ ] AC-9 (gates, counts and failure injections):
  - **Gate.** `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease` exits 0 at Start and at the end. Run it on JDK 25 as declared by `gradle/gradle-daemon-jvm.properties`, in a shell without `MSYS_NO_PATHCONV`, with a timeout above three minutes. The end run has 0 failures, a skipped count not higher than at Start, and no JaCoCo floor lowered.
  - **Counts.** The authoritative count (`git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`) is recorded at Start with its commit id. It is recorded again at the end with the story's last commit in place of `HEAD`, or before the commit with the working-tree form and `--untracked`, per REQ-MODERNIZATION-015 AC-13 as amended by CR-016. Tests from other changes are listed and not counted. The difference equals the new tests, each named. No fixed count is stated.
  - **Failure injections.** Each is applied alone and recorded with the U-074 AC-15 protocol (`sha256sum` before and after, restore from the saved copy, never present at a gate or commit):
    - M-F1, delete the catch: AC-1 and AC-7 fail;
    - M-F2, swallow every `IllegalStateException`: AC-5 (a), (b) and (d) fail;
    - M-F3, ask for the foreground for every type: AC-3 fails;
    - M-F4, delete the warning line: AC-2 fails.
- [ ] AC-10 (device, run by the orchestrator on `Pixel_9_Pro`, Android 16; never `Pixel_10_Pro`): Install the fixed debug build over today's build, with data kept. Then:
  - **(a) Incoming, off screen.** Steps to Reproduce 2 and 4 to 7: open the app for about three seconds, Home, send the test SMS, wait until the delayed incoming backup has started with the app off screen (its "starting backup, type=INCOMING" timestamp is after the Home press). The log shows:
    - no `ForegroundServiceStartNotAllowedException` and no "Moving WorkSpec … to the foreground" for that work;
    - "BackupWorker: sending 1 SMS message(s) to server.", "watermark advanced for SMS", "backup complete, backedUp=1";
    - "Worker result SUCCESS for Work [ … INCOMING ]".

    The test SMS is in the mail account's SMS folder, checked by the orchestrator. No backup notification appears: a screenshot of the notification shade taken while the run is logged, `adb -s <serial> exec-out screencap -p > out.png`.
  - **(b) Boot backup, never opened.** After a cold start without opening the app, if the boot backup is not held by quota (step 3), it ends "Worker result SUCCESS … REGULAR". If it is held, record that and do not count it as a failure of this criterion (BUG-025).
  - **(c) Manual, on screen.** Press the backup button. The log shows "Moving WorkSpec (…) to the foreground" and "Started foreground service", a screenshot shows the "Backup" notification, and the run ends SUCCESS.
  - **(d) Optional, the catch on a device.** Airplane mode on, press the backup button, Home at once. The retry about 30 s later starts off screen and logs AC-2's warning once. Its result is the transport's (expected `Result.retry()` "MailException — retrying"; whether the failed connection maps to `MailException` is not verified). Airplane mode off: a later retry backs up and ends SUCCESS.

  Evidence: the sanitized log (U-063 sanitizer) and the screenshots in the story's evidence folder.

### Integration Criteria (if fix modifies component boundaries)
- [ ] IC-1: No signature changes. `BackupWorker` and `RestoreWorker` constructors, `TestableBackupWorkerFactory`, `TestableRestoreWorkerFactory`, U-070's `TestableBackupWorkerFactoryWithTransport` (as built), `createBackupForegroundInfo()` and `createRestoreForegroundInfo()` are unchanged. The only new production symbol is one internal helper (Suggested Approach). Checked by `:app:assembleDebug` and the diff.
- [ ] IC-2: The new `catch` is listed for story U-074. U-074's IC-1 searches the worker files and the code they call for `catch (e: IllegalStateException)`. The new catch rethrows a `CancellationException` as its first act, and the plan of the later story names it as "left on purpose". Checked by reading the helper; recorded in Implementation Notes.

## Impact Analysis

Searches run at `33706a13` (`git grep`, `git show`):
- in `app/src`: `setForeground(`, `ForegroundInfo(`, `isBackground()`, `setExpedited`, `setInitialDelay`, `assumenosideeffects`, `sdk = [`;
- in the contract and DES-MODERNIZATION-005: `foreground`, `notification`, `expedit`, `Result.failure`, `Failed`;
- in `sdlc/artifacts/requirements`: `foreground`;
- U-049's story and QA files;
- `git log -S` on the two `setForeground` lines and on `targetSdkVersion 3`.

| # | Layer | Affected? | Files and lines | Action needed |
|---|-------|-----------|-----------------|---------------|
| 1 | Requirements | No change | REQ-MODERNIZATION-005 `:30` ("preserving all current scheduling behavior"), `:41`; REQ-MODERNIZATION-013 `:155-170` (AC-8: a documented decision whether foreground promotion "for manual backups" is preserved via `setForeground()`) | The fix story records the revised decision in its plan, which is what AC-8 asks for |
| 2 | Design and contracts | Small clarification (non-breaking) | DES-MODERNIZATION-005 `:54`, `:129-131` ("where a user-visible notification is required it uses `setForeground` / `getForegroundInfo`"), Risks `:487-496`; CNTR-MODERNIZATION-004 `:280` ("the manual notification/foreground semantics"), `:284`, `:287` | See Suggested Approach, "Contract and design" |
| 3 | Stories and sprints | Order and references | U-070 (in progress; edits other lines of both worker files); U-074 (planned; five catch sites and its IC-1 search); U-071, U-073 (start state; WorkManager 2.12.0); the BUG-026/BUG-027 scheduler story (its device AC-9 needs a background run to succeed); BUG-025 (its fix makes background incoming runs more frequent) | Suggested Approach, "Where it goes" |
| 4 | Application code | Yes (root) | `service/BackupWorker.kt:118-145`; `service/RestoreWorker.kt:143-165`; one new helper file | The fix |
| 5 | Vendored library | No | None | None |
| 6 | Tests | Yes (new only) | New `BackupWorkerForegroundTest.kt`, `RestoreWorkerForegroundTest.kt`; existing tests unedited (they run on `TestForegroundUpdater`, read) | AC-1 to AC-5, AC-7, AC-9 |
| 7 | Build and manifest | No | `AndroidManifest.xml:126-129` stays (still needed for manual and restore) | None |

**Cascade.** The code is the root: U-049 put a foreground request first, for every type, with no handling of a refusal. The design line `:129-131` is correct but does not say what "required" means or what happens on a refusal. Fix order: code and tests in one story; the design clarification in the next change record (below). It does not block the code.

## Suggested Approach

Written by the Solution Architect on 2026-11-21. Advisory until story planning.

### What the library and platform allow (the facts the options rest on)

| Fact | Source |
|---|---|
| L1. A refused start surfaces in `doWork` as `ForegroundServiceStartNotAllowedException`, unwrapped, at `setForeground` | 2.9.1 bytecode, Description item 4 |
| L2. Uncaught, the run is FAILED (one-time) or skipped to the next period (periodic) | 2.9.1 `WorkerWrapper.handleResult`; 2.12.0 `WorkerWrapper.kt:379-385` |
| L3. Before the start is attempted, `Processor.startForeground` has already moved the work into its foreground map and acquired the `ProcessorForegroundLck` wake lock. After a refusal both remain until the work ends. The lock is released when the foreground map is empty (`Processor.stopForegroundService`, which catches "Unable to stop foreground service") | 2.9.1 bytecode; 2.12.0 `Processor.java:192-208` |
| L4. `Processor.stopWork`, which the JobScheduler stop calls (`SystemJobService.onStopJob` → `WorkLauncher.stopWorkWithReason`), **ignores** work in the foreground map ("Ignored stopWork. WorkerWrapper … is in foreground"). `onStopJob` then returns `true` (reschedule) unless the app cancelled the work | 2.9.1 bytecode; 2.12.0 `Processor.java:240-244` |
| L5. A worker that is not in the foreground runs as an ordinary JobScheduler job. From Android 12, jobs "will still be stopped after 10 minutes if the system is busy or needs the resources, but if not, jobs may continue running longer than 10 minutes". Default-priority jobs have "a general execution time of 10 minutes". A stop reason `STOP_REASON_TIMEOUT` exists | SDK `JobScheduler.java:90-96`, `JobInfo.java:281-286`, `JobParameters.java:205-213` |
| L6. When the system stops a worker that is not in the foreground map, the worker is interrupted, its result is ignored (`onWorkFinished` runs `tryCheckForInterruptionAndResolve` before `handleResult`), and the work is rescheduled | 2.9.1 bytecode; U-074 Technical Context, "Library facts" |
| L7. Expedited one-time work "cannot be delayed" and periodic work "cannot be expedited". Both are thrown by the builders | 2.9.1 `WorkRequest$Builder.build`, `PeriodicWorkRequest$Builder` |
| L8. On API 31+, expedited work is a JobScheduler expedited job (`JobInfo.Builder.setExpedited`). Below 31, `WorkForegroundRunnable` calls `getForegroundInfoAsync()` and promotes it | 2.9.1 `SystemJobInfoConverter`, `WorkForegroundRunnable.run` |
| L9. `dataSync` foreground services have a 6-hour limit from Android 15, counted over a rolling 24-hour window, after which `onTimeout` is called | SDK `ServiceInfo.java:146-155`, `Service.java:1203-1216` |
| L10. The backup checkpoints per append: the watermark is written after each confirmed append (`BackupWorker.kt:405-425`). A stopped or killed run loses nothing that was confirmed, and the next run starts above the watermark | Code |

### Options

**(a) Catch the refusal at `setForeground` and continue as ordinary background work (all types).**
- *What it allows.* Every run that is refused continues and backs up. A run that is allowed is promoted as today.
- *The catch (L3, L4).* After a refusal, WorkManager still treats the work as foreground. A JobScheduler stop (time limit when busy, quota, lost network constraint) is then ignored. The worker keeps running in a process that the system no longer counts as running a job. JobScheduler reschedules the job anyway. What the platform then does with that process (cached-app freezing, a kill) is not verified (Not verified 5).
- *If the process is killed.* Nothing confirmed is lost (L10). How 2.9.1 recovers a `RUNNING` work spec after a process death was not read (Not verified 5).
- *U-074.* A refused run that is later stopped is not interrupted at all (L4), so U-074's cancellation path is not reached for it.
- *Other costs.* A brief foreground notification for automatic backups when allowed, and its use of the 6-hour budget (L9) for automatic runs. Background incremental runs are short (log :29-40, 4 s for one SMS), so in practice the effect of L4 is small. It is still a state the library does not expect.

**(b) Expedited work.**
- *Not possible for the automatic paths.* The incoming backup has its configured delay (`:148`), the boot backup 60 s (`:276`), and the regular backup is periodic (`:182`). Expedited cannot be delayed or periodic (L7).
- *Removing the incoming delay to allow it is ruled out.* The delay is the content-trigger debounce the design requires (DES-MODERNIZATION-005, Risks, "Content-URI debounce regresses"; CNTR-MODERNIZATION-004 INV-4).
- *Where it could apply.* Only to `BROADCAST_INTENT`, `MANUAL` and `SKIP`, which are undelayed (`:309-316`, `:350-357`). It would bring expedited-job quota.
- *Unknowns.* It would need `getForegroundInfo()` overridden for API < 31 (L8). Whether an expedited job on 31+ may start a foreground service from the background is not verified (Not verified 6).
- Rejected.

**(c) Recommended: ask for the foreground only for runs the user started, as the legacy app did, and catch the refusal there (a narrow form of (a)).**
- *The change.* `doWork` infers the type first. It asks for the foreground only when `!backupType.isBackground()` (`MANUAL`, `SKIP`; `BackupType.java:37-39`). The request goes through one helper that catches exactly the refusal (below), logs it once and continues. `RestoreWorker` always asks (a restore is always user-started) through the same helper.
- *What it allows.* Automatic backups never enter the foreground map. They run as ordinary jobs: the system's stops are honoured (L6). A stopped run is rescheduled and continues above the watermark (L10). U-074's cancellation path applies to them as designed. They do not use the 6-hour budget (L9).
- *User-started runs.* They keep today's notification when they start on screen, which is the usual case. A refused one continues with L3/L4's quirk. That is rare (a retry after the user left) and is the same trade as (a).
- *Legacy.* It is what the legacy app did. `SmsBackupService` promoted only `MANUAL` (`if (state.backupType == MANUAL) notifyAboutBackup(state)`, `SmsBackupService.java:216-219` on `master`, `:386-389` at `4b966da2^`). Automatic backups never ran as a foreground service. `SKIP` is promoted here because it is user-started and today's code promotes it. It is short (`executeSkip`, `BackupWorker.kt:198-218`).
- *Design and contract.* It matches DES-MODERNIZATION-005 `:129-131` ("where a user-visible notification is required") and CNTR-MODERNIZATION-004 `:280` ("the manual notification/foreground semantics").
- *Why not (a) alone.* (a) leaves every refused automatic run in a state the library does not expect (L4). (c) removes the refusal from the common path altogether instead of absorbing it. It also gives identical behaviour for automatic backups on Android 11 and on Android 16. The code size is the same. (a) is the right choice only if the owner wants the notification for automatic backups whenever the system allows it (Notes, PO-1).

**(d) Considered, not chosen.**
- *Asking for a battery-optimisation exemption.* That is an owner decision (PO-2). Option (c) does not need it.
- *An ordinary, non-foreground progress notification for automatic or refused runs.* That is a user-visible addition; the legacy app had none. Not part of this bug; it can follow PO-1.
- *Deciding by process importance (`ActivityManager.getMyMemoryState`) whether to ask.* This is a guess at the platform's rule (which has exemptions, Not verified 2). It would skip the foreground in cases the platform allows. (c) decides by who started the run, which the app knows exactly.

### Exactly what is caught

One internal helper, for example `internal suspend fun CoroutineWorker.setForegroundOrContinue(info: ForegroundInfo, tag: String, site: String): Boolean` in a new file `app/src/main/java/com/zegoggles/smssync/service/WorkerForeground.kt`:

```kotlin
try {
    setForeground(info)
    return true
} catch (e: IllegalStateException) {
    if (e is CancellationException) throw e                       // kotlinx = java.util.concurrent; an IllegalStateException
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        e is ForegroundServiceStartNotAllowedException) {
        Log.w(tag, "$site: foreground start not allowed; continuing without the foreground (${e.javaClass.simpleName})")
        return false
    }
    throw e
}
```

- **Caught and continued:** `android.app.ForegroundServiceStartNotAllowedException` only, on API 31 and later (Description item 3). The `SDK_INT` guard keeps the `is` check off API 24-30, where the class does not exist.
- **Never swallowed:**
  - `CancellationException`, including a cancellation while `setForeground` is suspended or a cancelled future (Description item 4). It is checked first because `java.util.concurrent.CancellationException` is an `IllegalStateException` (U-074 Technical Context, "Hierarchy").
  - `InvalidForegroundServiceTypeException` and `MissingForegroundServiceTypeException`: siblings, a manifest regression like BUG-016. Catching their parent `ServiceStartNotAllowedException` would hide them, so the parent is not caught.
  - The library's own `IllegalStateException` "Calls to setForegroundAsync() must complete before a ListenableWorker signals completion…" (`WorkForegroundUpdater$1`).
  - `SecurityException` (a missing permission).
  - Every `Error`.
- `Log.w` is written once per run, because the helper is called once per run.

`BackupWorker.doWork` then reads:
1. `val backupType = inferBackupType()`, moved up from `:142`;
2. `if (!backupType.isBackground()) setForegroundOrContinue(createBackupForegroundInfo(), TAG, "BackupWorker")`;
3. the backoff check (`:125-137`), as today;
4. "starting backup" (`:144`), text unchanged: device checks read it;
5. the `try` (`:146-171`), as today.

`RestoreWorker.doWork` replaces `:148` with the helper call. Nothing else changes, in particular neither catch at `:168-171` / `:198-200` (U-074's sites).

### Interactions

- **BUG-024 / U-074** (`sdlc/artifacts/build/sprints/sprint-025/U-074-cancelled-run-is-not-reported-as-a-failure.md`).
  - The helper rethrows cancellation, so a cancelled run is never turned into a warning and a continue.
  - Under (c), automatic runs are stoppable (L6). A system stop then reaches the `ensureActive` checks and U-074's `rethrowIfCancelled` path, which ends the run as cancelled. WorkManager ignores the result and reschedules (L6).
  - U-074's tests go through `doWork` with U-070's harness tagged `MANUAL` or `SKIP` (U-074 Technical Context, start condition 3). They still call `setForeground`, which `TestForegroundUpdater` completes, so they are unaffected.
  - U-074's IC-1 catch search will find the helper's catch; see IC-2.
  - U-074's AC-17 measures from its own Start. If this fix lands first, it is part of that Start.
  - U-074's hand-over H-2 (a WorkManager-level test with a probe "whether `setForeground` runs under the real wrapper in Robolectric") becomes simpler: an automatic-type worker no longer calls it.
- **U-070** (`sdlc/artifacts/build/sprints/sprint-024/U-070-late-visible-sms-are-backed-up.md`).
  - U-070 edits `BackupWorker.kt` below `doWork`'s catch and `RestoreWorker.kt`'s SMS import (U-074 Affected Code, introduction). This fix edits `doWork`'s first lines only.
  - The fix's tests reuse U-070's provider double and recording transport as built, without editing `LateVisibleSmsFixtures.kt`.
  - U-070's harness tags `MANUAL`/`SKIP`, so its tests and mutations are unaffected; the plan re-runs U-070's `BackupWorker.kt` mutations M-L11, M-L20 and M-L22 to show it.
- **BUG-026 / BUG-027 scheduler story.** No shared file (that story edits `WorkManagerScheduler.kt` and its test only, BUG-027 AC-7). Its device criterion AC-9 ("After the next regular or incoming run, it is still periodic") needs a background run to start; today that run fails at once (log :10-23). The fix should land before that device check. Once both have landed, a restart neither deletes the regular backup nor makes it fail off screen.
- **BUG-025.** Its fix makes incoming backups start sooner and more often, nearly always off screen. Without this fix every one of them would fail. This fix should land first.
- **U-071, U-073.** 2.12.0 behaves the same at every point this fix relies on: L1 to L4 were read in 2.12.0 (`CoroutineWorker.kt:105-117`, `Processor.java:192-208`, `:240-244`, `WorkerWrapper.kt:376-386`). U-073 should carry the new tests unedited, as it does the scheduler tests.
- **Contract and design.**
  - No contract statement is broken by the fix. CNTR-MODERNIZATION-004 `:280` speaks of "the manual notification/foreground semantics", which (c) keeps. `:284` (non-retryable failures return `Result.failure()`) is satisfied better: a refusal is no longer a failure. `:287` is unaffected.
  - A non-breaking change record should make two statements true in text:
    - DES-MODERNIZATION-005 `:129-131`: "`setForeground` is used for backups the user starts and for restores; automatic backups run without the foreground. Promotion is best effort: a refused foreground start is logged once and the run continues as ordinary background work." `:54` ("owns worker lifecycle plus foreground promotion") stays as it is.
    - A Risks row (`:487-496`): "Android 12+ refuses foreground starts from the background (targetSdk ≥ 31) | High | Promote only user-started work; catch `ForegroundServiceStartNotAllowedException` only".
  - Optionally, CNTR-MODERNIZATION-004 Error Handling after `:287`: "A refused foreground promotion is not a failure."
  - **Version.** Next free version, approved after the code story or with it; it does not gate the code. If U-074's hand-over record H-1 (a non-breaking CNTR addition after `:287`) is not yet written, fold this sentence into that record rather than adding a version.
- **RestoreWorker.**
  - It gets the helper, and it keeps asking for the foreground. A restore is long, user-started and was a foreground service in the legacy app (REQ-MODERNIZATION-013 `:155-164`). The foreground protects it from the job time limit (L5).
  - It needs the catch because its retries (`Result.retry()` on `MailException`, `RestoreWorker.kt:194-197`, 30 s backoff) can start after the user left. Without the catch such a retry fails at once.
  - A refused restore continues as background work. Its durable checkpoint (U-016) lets a stopped run resume.

### Where it goes

Directly after story U-070 and before U-074 and the toolchain stories (U-071, U-072, U-073), as the orchestrator ruled provisionally. Reasons:
- **U-070.** Both stories edit `BackupWorker.kt` (and `RestoreWorker.kt`). U-070's plan is line-anchored and hashed and is in progress, so this fix must not land before it or alongside it.
- **U-074.** It locates its sites by function name and the catch's text (U-074 Affected Code, introduction). This fix adds a catch in a new helper, not in U-074's five sites. U-074's plan then only lists the helper's catch under its IC-1. Putting this fix first also means U-074's cancellation tests run on the final shape of `doWork`.
- **The toolchain stories.** They move JDK, Gradle, AGP and libraries. Landing a high-severity behaviour fix first lets U-073 carry its tests across WorkManager 2.12.0, and the device criterion AC-10 runs on today's known-good toolchain.
- **Before the BUG-026/BUG-027 scheduler story and BUG-025's fix**, for the reasons under Interactions.
- **Cost.** U-071's plan re-derives its start state (test count, files). It must do so after U-074 anyway, so one more story before it changes the numbers, not the kind of work.
- **No reason against was found.** One caution: if U-069 or U-070 is still being built, this story must not start until both are committed. The working tree at the time of writing has uncommitted changes in `scheduler/WorkManagerScheduler.kt` and `worker/BackupTriggerWorker.kt` (U-069).

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` (`doWork`, `:118-145`) | `setForeground` first, for every type, outside the `try`; a refusal fails the run | Infer the type first. Ask for the foreground only for `MANUAL`/`SKIP`, through the helper. The rest of `doWork` and both catches are unchanged |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt` (`doWork`, `:148`) | Same first statement | Same helper call; always asks |
| `app/src/main/java/com/zegoggles/smssync/service/WorkerForeground.kt` (new) | Does not exist | `setForegroundOrContinue`: catches only `ForegroundServiceStartNotAllowedException` (API 31+), rethrows cancellation and everything else, one warning |
| `app/src/test/java/com/zegoggles/smssync/service/BackupWorkerForegroundTest.kt`, `RestoreWorkerForegroundTest.kt` (new) | No test makes `setForeground` fail; every test runs on `TestForegroundUpdater` | AC-1 to AC-5, AC-7 |
| `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` (`:129-131`, Risks `:487-496`); optionally CNTR-MODERNIZATION-004 after `:287` | Silent on refusals and on which runs are promoted | Non-breaking change record (Suggested Approach, "Contract and design") |

## Existing Behavior to Preserve

- A manual backup, a Skip and a restore started on screen show today's ongoing notification: same channel, id, text, content intent and `dataSync` type (`BackupWorker.kt:516-539`; `RestoreWorker.kt`'s `createRestoreForegroundInfo`). The `SystemForegroundService` override in the manifest (`:126-129`) stays.
- The 300 s backoff cap and its failure reason (`BackupWorker.kt:125-137`; `RestoreWorker.kt:150-158`).
- Every typed mapping: `MailException` to `Result.retry()`; `XOAuth2FailedException` to the token refresh; `RequiresLoginException` to "auth_failed"; `SecurityException` to "missing_permission"; the broad catch to `Result.failure(message)`. The cancellation behaviour U-067 built and U-074 will build.
- The watermark rule: advance only after a confirmed append, per append (BUG-010, `:405-425`).
- The log lines device checks read: "BackupWorker.doWork: starting backup, type=…, attempt=…", "starting backup (N messages)", "backup complete, backedUp=N", "nothing to backup", "RestoreWorker.doWork: starting restore, attempt=…".
- Scheduling in full (AC-6): types, delays, constraints, backoff, tags, unique names, policies.
- The status screen's suppression of background-type progress (`StatusPreference.java:230-234`).

## Verification Steps

Run Gradle in a shell without `MSYS_NO_PATHCONV`. Compare files with `git diff --no-index` or `sha256sum`, never `diff` or `cmp`.

1. **Start.** `git log` shows U-070 done (and U-069). Run the gate and record the count with the commit id (AC-9).
2. **Characterization first.** Write AC-4 and AC-5's tests. Run them against unchanged code; all are green. Record the output.
3. **Failing first.** Write AC-1, AC-2, AC-3 and AC-7's refusal tests. Each fails for the stated reason (today `doWork` throws `ForegroundServiceStartNotAllowedException`; today 1 call for automatic types), not on a compile or fixture error. Record the output.
4. **Fix.** Add the helper and the two call sites. All new tests are green. Read the warning line against AC-2 (text, level, no personal data).
5. **Unchanged behaviour.** Existing tests pass unedited (`git diff <start> -- app/src/test` shows added files only). U-070's `BackupWorker.kt` mutations M-L11, M-L20 and M-L22 are still detected by their recorded sets.
6. **AC-6.** Run the `git diff --name-only` of AC-6; it is empty.
7. **AC-9.** Gate at the end, the count at the end, and M-F1 to M-F4 by the protocol.
8. **AC-10.** The orchestrator's device run (a) to (c), optionally (d), with the sanitized log and screenshots.
9. **IC-1, IC-2.** Read the diff for signatures. Record the helper's catch for U-074 in Implementation Notes.

## Root Cause Analysis

- **When it entered.**
  - `git log -S "setForeground(createBackupForegroundInfo())"` and the restore equivalent both find one commit, `4b966da2` (2026-06-23, "U-049: retire legacy backup/restore Service dual-dispatch layer (AR-002)").
  - Its story's AC-2 asks both workers to "call `setForeground(ForegroundInfo(...))` at the start of execution with the same notification channel and content that `ServiceBase` previously provided" (U-049 story `:36`, `:49-50`). Its Technical Context says it "must be called early in `doWork()` to avoid ANR on Android 12+ (foreground-service start restriction)" (`:68`). That is a misreading: the Android 12 restriction forbids the start from the background, and calling early does not help.
  - The precondition is targetSdk 35, set by U-003 (`e86ef3f9`, 2026-06-02). Before it the app targeted 29 (`e86ef3f9^:app/build.gradle:17`), where the restriction does not apply (Service.java `:723-727`).
- **What changed against the legacy app.**
  - The legacy service promoted only manual backups (`SmsBackupService.java:216-219` on `master`; `:386-389` at `4b966da2^`).
  - Before U-049 the worker path did not call `setForeground` at all (U-049 story `:49`, "Does not call `setForeground`").
  - U-049 promoted every type, not only `MANUAL`, against REQ-MODERNIZATION-013's framing of the decision as one "for manual backups" (`:162`). It also left the call outside the `try`.
- **Why it was not caught.**
  - Every unit test builds the worker with `TestListenableWorkerBuilder`. Its default `TestForegroundUpdater` completes at once (read), so no test could see a refusal.
  - U-049's QA checked the call by reading ("first statement of `doWork()`", `qa-results.md:35`).
  - Later device checks ran backups with the app on screen. The 07:42:50 measurement of the same morning also started in the second the app was opened (record, "What it shows" 1).
  - BUG-025 named the risk from the library documentation (2.12.0 `CoroutineWorker.kt:108-111`: "Calling `setForeground` will throw an `IllegalStateException` if the process is subject to foreground service restrictions") but did not examine it.

## Technical Context

- **Severity: high** (the scaffold's value, confirmed).
  - Automatic backup is the app's main background function. On Android 12 and later it fails at every start that happens off screen, which for automatic backups is nearly every start. The failure is silent: no app log line, no notification, nothing on the status screen, which hides background-type states other than errors (`StatusPreference.java:230-234`).
  - The work-around is to open the app.
  - Not **critical**: no data is lost or corrupted (Description item 8), nothing crashes, and Android 11 and earlier are not affected.
  - On the project's scale (BUG-027 medium, a lost schedule with incoming backups still running), a near-total loss of automatic backup on current Android versions is above medium.
- **Priority: high** (raised from the scaffold's medium).
  - It cancels the value of BUG-025's and BUG-027's fixes: incoming and regular backups that start on time still fail off screen.
  - It affects the owner on every phone with Android 12 or later, at default settings.
  - The fix is small, needs no owner decision to start (PO-1 and PO-2 have safe defaults), and has a clear place in the order.
- **Complexity: medium** (kept).
  - The code is a helper and two call sites. The care is in the exception boundary: cancellation and the type exceptions must pass.
  - The tests need an SDK level of 31 or above under Robolectric (Not verified 7).
  - It also needs a device run and a small change record.
- **Origin: `qa`** (kept). Found by the orchestrator's device observation under ruling 55 (record, first paragraph).
- **Library versions.** WorkManager 2.9.1 in the app; 2.12.0 planned by U-073. L1 to L4 were read in both; L5 to L8 in 2.9.1 bytecode and the API 36 SDK sources.

## Supporting Documentation

- `sdlc/artifacts/build/research/2026-11-21-background-backup-and-boot-observation.md`; `sdlc/artifacts/build/research/evidence/2026-11-21-background-backup-app-log.txt`
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` (`:39`, `:54`, `:121`, `:129-131`, `:487-496`)
- `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` (`:280`, `:284`, `:285`, `:287`; INV-3, INV-4)
- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md` (`:30`, `:41`); `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-013-remove-legacy-asynctask-execution-path.md` (`:155-170`, AC-8); REQ-MODERNIZATION-015 AC-13 as amended by CR-016 (count rule)
- `sdlc/artifacts/build/sprints/sprint-012/U-049-retire-the-legacy-backuprestore-service-dual-dispatch-layer.md` (`:36`, `:49-50`, `:68`); `sdlc/artifacts/build/sprints/sprint-012/U-049/qa-results.md` (`:35`)
- `sdlc/artifacts/stories/BUG-025-incoming-sms-backup-delayed-by-trigger-period-and-job-quota.md` (Notes, "Observations outside this bug"); `sdlc/artifacts/stories/BUG-027-regular-backup-removed-by-the-boot-backup.md` (AC-9; Steps B.2 diagnostics)
- `sdlc/artifacts/build/sprints/sprint-024/U-070-late-visible-sms-are-backed-up.md`; `sdlc/artifacts/build/sprints/sprint-025/U-074-cancelled-run-is-not-reported-as-a-failure.md` (Affected Code, IC-1, AC-15 protocol, Technical Context "Library facts")

## Implementation Notes
<!-- Added by agents during build -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes

The number BUG-028 was used up by a failed scaffold call and is not used.

### For the product owner (decide; recommendation given)

- **PO-1: Should automatic backups (new messages, the regular schedule, after a restart, a third-party trigger) show the "Backup" notification while they run?**
  - The legacy app never did: only manual backups showed it.
  - Since U-049 the app tries for every backup. The system allows it only when the app is on screen, and when it does not, the backup fails (this bug).
  - **Recommendation: no.** Automatic backups run silently, as before U-049, and are not subject to the refusal at all (option (c)). Manual backups and restores keep the notification. The fix uses this until you decide.
  - If **yes**, the fix becomes option (a): automatic backups try for the notification and, when refused, run without it. They are then in a state the library does not expect (Suggested Approach, L4).
- **PO-2: Should the app ever ask you to exempt it from battery optimisation?**
  - Today it does not, and the emulator was not exempt.
  - **Recommendation: no, not for this bug.** Option (c) does not need it.
  - Whether an exemption would lift BUG-025's job-quota delays is not verified; that is BUG-025's question, not this one's.
  - The fix does not ask, and changes nothing about the exemption, until decided.

### Observations outside this bug (raised, not part of the acceptance criteria)

- **WorkManager ignores scheduler stops for work it moved to the foreground, even when the start was refused** (L3, L4; 2.9.1 bytecode and 2.12.0 `Processor.java:192-208`, `:240-244`). After the fix this matters only for a refused manual backup or restore. Worth a line in U-073's notes.
- **A second refusal point exists inside the service in 2.12.0.** `SystemForegroundService.Api31Impl.startForeground` catches `ForegroundServiceStartNotAllowedException` and `SecurityException` and only logs "Unable to start foreground service" (`SystemForegroundService.java:189-204`). In that case the worker is not told. Not reachable in the observed runs, where the refusal came earlier, at `startForegroundService`.
- **The log shows "Stopping foreground service" after each refused start** (log :24, :55). The dispatcher saw a stop although the start was refused. Not examined.
- **`BackupWorker.doWork`'s broad catch turns a `CancellationException` into `Result.failure`** (`:168-171`). That is BUG-024, fixed by U-074; it is unchanged here.
- **The U-049 story's sentence "must be called early in `doWork()` to avoid ANR on Android 12+ (foreground-service start restriction)"** (`:68`) is wrong (Root Cause Analysis). Later stories should not repeat it.
- **Android 15's 6-hour `dataSync` budget** (L9) applies to long manual backups and restores. What WorkManager 2.9.1 does on `onTimeout` was not read.

### Not verified

1. Nothing was run for this artifact: no Gradle, no unit test, no device. The fix's behaviour is described, not observed. The bug itself was observed once, on one emulator.
2. The platform's rule for when a background foreground-service start is allowed (visible activity, a grace period after leaving the screen, the window after `BOOT_COMPLETED`, battery-optimisation exemption, other exemptions). Only the javadoc sentence (`Service.java:723-727`) was read. Why the 09:27:45 start was allowed and the 09:47:36 start, 58 s after Home, was refused is observed, not explained.
3. Android 15's rule for apps targeting 35 that `BOOT_COMPLETED` receivers may not start `dataSync` foreground services. Not read. WorkManager starts the service from a job, not from the receiver, so it is probably not involved.
4. Whether starting a `dataSync` foreground service after the 6-hour budget is used up throws `ForegroundServiceStartNotAllowedException` (`ServiceInfo.java:146-155` describes the limit and `onTimeout`, not the next start).
5. Under option (a) or a refused manual run: what the platform does with a process whose job was stopped while the worker goes on (freezing, a kill), what happens when JobScheduler restarts the job while the old wrapper still runs (`Processor.startWork` logs "is already enqueued for processing"; the rest was not traced), and how 2.9.1 recovers a `RUNNING` work spec after a process death.
6. Whether an expedited job on API 31+ may start a foreground service from the background, and what the default `CoroutineWorker.getForegroundInfo` does in 2.9.1.
7. That Robolectric 4.15.1 runs `@Config(sdk = [35])` offline. The local Maven cache holds `android-all-instrumented` jars `15-robolectric-12650502-i7` and `16-robolectric-13921718-i7`; that 4.15.1 resolves to them was not checked. Also that `ForegroundServiceStartNotAllowedException` and the type exceptions are present in those jars: they were read in the API 36 SDK sources only.
8. Whether a `ListenableFuture` helper that can build a failed future (`androidx.concurrent.futures`, Guava) is on the test classpath.
9. Whether a denied `POST_NOTIFICATIONS` hides the foreground notification of a manual backup; it does not affect this fix.
10. Which of the owner's phones run Android 12 or later.
11. Whether a manual backup in airplane mode ends in `MailException` (AC-10 (d)).
12. The repeated work id of the INCOMING backup in run 1 (record, "What it does not show"). Not examined.
