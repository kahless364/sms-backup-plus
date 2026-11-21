Measurement: how soon an incoming SMS starts an automatic backup. Run by the orchestrator on 2026-11-21.
Device: Android 16 emulator (AVD Pixel_9_Pro, API 36), app com.zegoggles.smssync debug build of story U-066 (commit 1f8ebe3b; versionCode 1602, minSdk 21), mail account configured, auto backup on (the app's screen read 'Incoming schedule: 1 min' earlier the same day, from the orchestrator's run notes). The emulator had just been started; the app had not been opened since the start. Device clock times.

07:25:32  test SMS A sent through the emulator console.
07:25:43  app log: 'BackupTriggerWorker.doWork: content-URI trigger fired; enqueueing delayed incoming backup with initialDelay=60s' (11 s after the SMS).
07:29:20  scheduler dump (dumpsys jobscheduler) for the app:
          - the content-trigger job: 'Minimum latency: +14m59s993ms', required constraints TIMING_DELAY, FLEXIBILITY, CONTENT_TRIGGER; time until it may run again: +11m22s.
          - the delayed incoming backup job: required TIMING_DELAY, FLEXIBILITY; delay elapsed 3m15s earlier; 'Unsatisfied constraints: WITHIN_QUOTA'. It had not run.
          - a second delayed job with a connectivity requirement: unsatisfied CONNECTIVITY and WITHIN_QUOTA.
          app standby bucket (am get-standby-bucket): 50.
07:31:26  test SMS B sent.
07:31:26 to 07:41:00  no 'content-URI trigger fired' line (waited 9.5 minutes).
07:41:09  scheduler dump: the content-trigger job's delay had elapsed 26 s earlier and its content-URI condition was met, but WITHIN_QUOTA was unsatisfied; the two delayed jobs were still waiting, 14.5 and 15 minutes after their delay elapsed, each with WITHIN_QUOTA unsatisfied.
07:42:47  the debug build of story U-068 (minSdk 24) was installed over the app (adb install -r) and the app was started at 07:42:50 for that story's launch check. Standby bucket afterwards: 10.
07:42:50  app log: 'BackupWorker.doWork: starting backup, type=INCOMING'; 'starting backup (2 messages)'.
07:43:22  app log: 'backup complete, backedUp=2' (SMS A 17m50s and SMS B 11m56s after they arrived).

What it shows: (1) after the content trigger fires once, its job carries a minimum latency of about fifteen minutes, and a second SMS six minutes later did not fire it within 9.5 minutes; (2) with the app in standby bucket 50 the scheduler held all three of the app's jobs back for lack of quota, and the backup ran within a second of the app being opened.
What it does not show: the behaviour on a physical device or in another standby bucket; whether the second SMS would have fired the trigger at the end of the period if quota had been available (the period ended at about 07:40:43 and quota was the unmet condition then); how long the quota hold lasts without opening the app. One emulator, two messages, one run.
Raw logs are kept outside the repository.
