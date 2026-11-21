# Observation: automatic backups that start in the background fail on Android 16; the boot backup removes the regular schedule

Made by the orchestrator on 2026-11-21 on the Android 16 emulator (AVD `Pixel_9_Pro`, API 36), under ruling 55 of the
decision appendix (Record 20: "The orchestrator runs the device procedure in the bug on the Android 16 emulator to
observe it before the fix is planned"). The app's log lines are in
`sdlc/artifacts/build/research/evidence/2026-11-21-background-backup-app-log.txt` (the app's own process only, passed
through the sanitizer of story U-063). Raw device logs are kept outside the repository.

## Set-up

| Item | Value |
|------|-------|
| Device | Emulator `Pixel_9_Pro`, Android 16 (API 36). Not `Pixel_10_Pro` |
| App | `com.zegoggles.smssync`, debug build of story U-068 (versionCode 1602, version name 1.6.0-BETA2, minSdk 24, targetSdk 35), installed earlier the same day |
| Account | Mail account configured, auto backup on, incoming delay 1 minute, regular schedule at its default |
| Battery optimisation | The app is not on the device's battery-optimisation exemption list (`dumpsys deviceidle whitelist` has no line for the package) |
| WorkManager | 2.9.1 |

Nothing on the device was changed for the runs: no setting, no permission, no standby-bucket command. The emulator was
cold-started (`-no-snapshot-save`), one test SMS was sent through the emulator console in run 2, and the app was opened
once in run 2 as described.

## Run 1 (cold boot at about 09:27; the app was not opened)

| Time | What happened | Source |
|------|---------------|--------|
| 09:27:44 | `BootReceiver` received `BOOT_COMPLETED`; log lines `bootup` and `WorkManagerScheduler.scheduleBootup: enqueued bootup REGULAR with 60s delay` | app log |
| 09:27:45 | A backup of type INCOMING left over from before the restart (attempt 1) ran, entered the foreground, found nothing to back up and succeeded. It started less than a second after the boot broadcast | app log |
| after boot | Scheduler dump for the app: two jobs only, the content trigger and one one-time job with a 60 s minimum latency and a network requirement. **No periodic job** | `dumpsys jobscheduler` |
| 09:28:57 | The boot backup (tags `BackupWorker`, `REGULAR`) started. WorkManager tried to move it to the foreground. The system refused: `android.app.ForegroundServiceStartNotAllowedException: startForegroundService() not allowed due to mAllowStartForeground false`. **Worker result FAILURE.** `BackupWorker.doWork` logged nothing: the failure is at its first statement | app log |
| afterwards | WorkManager's own diagnostics: recently completed work `REGULAR FAILED`; enqueued work: the content trigger only. **No regular backup is scheduled** | WorkManager diagnostics dump |

## Run 2 (cold boot at about 09:41)

| Time | What happened | Source |
|------|---------------|--------|
| 09:42:08 | `bootup`; `scheduleBootup: enqueued bootup REGULAR with 60s delay` | app log |
| 09:44:14 | Scheduler dump: the boot job's delay had elapsed, its network requirement was not yet met. The app's standby bucket was 50 (never used) | `dumpsys jobscheduler`, `am get-standby-bucket` |
| 09:44:27 | One test SMS sent to the emulator | emulator console |
| 09:46:08 | Scheduler dump: both jobs held for lack of job quota (`WITHIN_QUOTA` unsatisfied). The content trigger had not fired, 101 s after the SMS | `dumpsys jobscheduler` |
| 09:46:34 to 09:46:38 | The app was opened for about three seconds, then the Home key was pressed | adb |
| 09:46:36 | With the app on screen: the boot backup (REGULAR) started, entered the foreground, backed up the one SMS and succeeded at 09:46:40. The content trigger fired in the same second and enqueued an incoming backup with a 60 s delay | app log |
| 09:47:36 | The incoming backup started, 58 s after the app left the screen. WorkManager tried to move it to the foreground. The system refused with the same exception. **Worker result FAILURE** (tags `BackupWorker`, `INCOMING`) | app log |

## What it shows

1. **A backup that starts while the app is not on screen fails at once on Android 16.** Both worker types that ran in
   the background (REGULAR in run 1, INCOMING in run 2) failed at `setForeground`, the first statement of
   `BackupWorker.doWork` (`app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt:123`), before any message
   was read. The two backups that succeeded on this day's runs started either within a second of the boot broadcast or
   while the app was on screen (run 2 at 09:46:36; the measurement of the same morning at 07:42:50, recorded in
   `2026-11-21-incoming-backup-delay-measurement.md`, also started in the second the app was opened).
2. **The failure is final for that run.** WorkManager records FAILURE, not a retry. For a one-time backup nothing runs
   it again until the next trigger or the next time the schedule is registered.
3. **BUG-027 is confirmed on a device.** After the restart the only regular work is the one-time boot backup; there is
   no periodic regular backup in the scheduler or in WorkManager, before or after the boot backup has run.
4. **With the app in standby bucket 50, nothing ran at all until the app was opened** (BUG-025's second mechanism, seen
   again).

## What it does not show

- Android 12 to 15, or a physical device. The restriction is documented by the platform from Android 12 for apps that
  target it; only Android 16 was run.
- An app that is exempt from battery optimisation, or that holds another exemption the platform grants. Neither was
  set up.
- A periodic regular backup that starts in the background: none existed after the restart (finding 3). It calls the
  same first statement, so the same failure is expected, but it was not observed.
- Why the INCOMING backup in run 1 carried the same work id as one that had succeeded at 07:43 the same day. A device
  state that was not fully written when the emulator was last shut down would explain it; it was not examined.
- Android 11 and earlier, where the restriction does not exist.

## Consequence

Finding 1 is raised as a bug of its own (it is not BUG-025, whose subject is delay, and not BUG-027, whose subject is
the lost schedule). It bears on every automatic backup on a phone with Android 12 or later. BUG-027 gets a dated note
with finding 3.
