---
type: story
status: planned
sprint: '025'
artifact_type: user-story
priority: medium
complexity: low
parallel_eligible: false
iteration: 1
requirements: []
design_docs: []
integration_contracts:
  - CNTR-MODERNIZATION-004
dependencies:
  - U-075
  - U-076
change_records: []
related_items:
  - CR-017
  - BUG-023
  - BUG-025
  - U-069
  - U-073
  - U-078
  - U-068
  - CR-016
  - REQ-MODERNIZATION-005
  - REQ-MODERNIZATION-015
  - DES-MODERNIZATION-005
  - CNTR-MODERNIZATION-004
  - CNTR-MODERNIZATION-006
platforms: []
tags:
  - sms-receiver
  - dead-code-removal
  - manifest
  - content-trigger
  - incoming-backup
  - cntr-004
  - product-owner-decision
  - device-check
gate_additions: []
id: U-077
title: Remove the SMS-received receiver that never gets the system broadcast (the class, its manifest entry, its enable/disable in App, its test and its javadoc mention), keep RECEIVE_SMS and the default-SMS receiver, so that the content trigger is the one incoming path in the code as in the contract
pipeline: ''
domain: modernization
requirement_source: cr:CR-017
updated_at: '2026-11-21T18:10:57.277Z'
---

# U-077: Remove the SMS-Received Receiver That Never Gets the System Broadcast, Keep RECEIVE_SMS and the Default-SMS Receiver, So That the Content Trigger Is the One Incoming Path in the Code as in the Contract

## Story
As the owner of this private, unpublished SMS Backup+ build (its only user, on phones with Android 11 or later),
I want the receiver for `SMS_RECEIVED`, which the system never delivers to it, deleted together with everything that exists only for it, while the permission and the receiver that a restore needs stay,
So that the code says what the app does (incoming messages reach the backup through the content trigger alone), no reader or later story restores a dead path on the belief that it works, and the scheduler contract's "Code not yet at v4" item K-2 can be deleted.

Source: change record CR-017 (`sdlc/artifacts/build/change-records/CR-017-cntr-004-scheduler-never-boot-example-and-receiver.md`, status implemented, with Amendments 1 and 2), "Part 4" (ruling: not a break), "What the story that removes the SMS-received receiver must cite" (items 1 to 6), the orchestrator's rulings on its scope concerns (rulings 3 and 4), Amendment 1 finding S-6 (edits C-30, D-27), close-out edit X-2. The decision is the product owner's of 2026-11-21, "Remove it (Recommended)" (decision appendix `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`, Record 20, ruling 56). The facts of non-delivery are BUG-023's (`sdlc/artifacts/stories/BUG-023-content-trigger-backup-skips-scheduling-checks.md`, Suggested Approach, "Severity, and the SMS-received receiver", and PO-3). The rulings are binding and are not re-decided here.

**What the change is, in one paragraph.** Delete `receiver/SmsBroadcastReceiver.java` and its test `SmsBroadcastReceiverTest.java`. Delete its `<receiver>` element from `AndroidManifest.xml`. In `App.java` delete its import and its line in `setBroadcastReceiversEnabled`, which then toggles `BootReceiver` only, and drop its name from the comment above the call. Drop its name from the port's javadoc list of call sites in `BackupScheduler.java`. Nothing else changes: `RECEIVE_SMS` stays declared, `compat/SmsReceiver` (the `SMS_DELIVER` receiver used while the app holds the SMS role during a restore) stays, and the content trigger, the scheduler and the other three receivers are untouched.

**Not in this story** (each ruled or recorded; none is an acceptance criterion):
- Any permission change. `RECEIVE_SMS` stays (CR-017 ruling 3); `RECEIVE_MMS` and every other permission stay. Confirming the `SMS_DELIVER` grant during a restore is story U-073's device check (ruling 3).
- The content-trigger period and the delay of incoming backups (BUG-025): story U-078 changes only the text.
- The scheduler fixes for "Never" and the boot path (BUG-026, BUG-027): story U-076.
- Statements in CNTR-MODERNIZATION-004 that are false for reasons outside CR-017, and CNTR-MODERNIZATION-006 line 44 (it names this receiver among receivers that publish events): the later housekeeping record (CR-017 rulings 7 and 8).
- The close-out edit X-2 (deleting item K-2 from the contract): the orchestrator's, when this story closes. This story edits no file under `sdlc/artifacts/design` or `sdlc/artifacts/requirements`.
- Renaming `setBroadcastReceiversEnabled` or rewriting the other lines of the comment above its call (see Scope Concerns, SC-2).

## Acceptance Criteria

Terms. `<start>` is the commit this story starts from (it contains story U-076 as done). `<end>` is the story's last commit. "The receiver" is `com.zegoggles.smssync.receiver.SmsBroadcastReceiver`. Line numbers are as read at commit `74b31759`; story U-076 runs first and may move lines in `BackupScheduler.java` (its javadoc) and elsewhere, so every edit is located by its text, not by its line number. Every command below is a read-only `git` command unless it says otherwise; its expected output is stated.

- [ ] AC-1 (the class is deleted): Given `<end>`, then `git ls-tree --name-only <end> app/src/main/java/com/zegoggles/smssync/receiver/` prints exactly three lines: `.../BackupBroadcastReceiver.java`, `.../BootReceiver.java`, `.../PackageReplacedReceiver.java`; and `git diff --diff-filter=D --name-only <start>..<end>` prints exactly `app/src/main/java/com/zegoggles/smssync/receiver/SmsBroadcastReceiver.java` and `app/src/test/java/com/zegoggles/smssync/receiver/SmsBroadcastReceiverTest.java`.
- [ ] AC-2 (its manifest entry is deleted, nothing else in the manifest): Given `<end>`, then (a) `git diff <start>..<end> -- app/src/main/AndroidManifest.xml` is one hunk of deleted lines only, no line added: the eight lines of the element `<receiver android:name=".receiver.SmsBroadcastReceiver" ...>` to its `</receiver>` (`AndroidManifest.xml:132-139` at `74b31759`) and at most one adjacent blank line; (b) `git grep -n -E "SmsBroadcastReceiver|SMS_RECEIVED" <end> -- app/src/main/AndroidManifest.xml` prints nothing; (c) `git grep -c "<receiver" <end> -- app/src/main/AndroidManifest.xml` is one less than the same count at `<start>` (6 at `74b31759`, so 5 if `<start>` has the same receivers); (d) `git grep -c "uses-permission" <end> -- app/src/main/AndroidManifest.xml` equals the count at `<start>` (20 at `74b31759`).
- [ ] AC-3 (its enable/disable in `App` is deleted; `BootReceiver`'s stays): Given `<end>`, then `git diff <start>..<end> -- app/src/main/java/com/zegoggles/smssync/App.java` shows exactly three changed lines and no other: the import `import com.zegoggles.smssync.receiver.SmsBroadcastReceiver;` deleted (`:46`); the line `enableOrDisableComponent(enabled, SmsBroadcastReceiver.class);` deleted (`:359`); and the comment line `// U-017: SmsBroadcastReceiver / BootReceiver enable/disable logic simplified:` (`:255`) replaced by `// U-017: BootReceiver enable/disable logic simplified:`. What remains in `setBroadcastReceiversEnabled(boolean enabled)` is the single statement `enableOrDisableComponent(enabled, BootReceiver.class);`; its two callers (`:258` at start, `:289` on every auto-backup settings event) and `enableOrDisableComponent` (`:363-371`) are unchanged.
- [ ] AC-4 (its mention in the port javadoc is deleted): Given `<end>`, then the javadoc sentence of `app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java` that begins `All call sites (` (`:32-33` at `74b31759`) no longer names `SmsBroadcastReceiver` and still names the other call sites it named at `<start>`; `git diff <start>..<end> -- app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java` touches only the lines of that sentence (re-wrapping it inside the comment is allowed), and no line outside the javadoc comment changes. Story U-076 edits other lines of the same javadoc first; this story reads the file as `<start>` has it.
- [ ] AC-5 (no reference remains): Given `<end>`, then `git grep -n "SmsBroadcastReceiver" <end> -- app README.md` prints nothing (at `74b31759` its hits are exactly the manifest `:132`, `App.java:46`, `:255`, `:359`, the class itself, `BackupScheduler.java:32` and `SmsBroadcastReceiverTest.java:19`, `:25`, `:30`, `:76`; `README.md` has none); and `git grep -n "SMS_RECEIVED" <end> -- app` prints nothing (at `74b31759` its hits are the manifest `:137`, the class `:45`, `:51` and the test `:47`, `:54`, `:61`, `:68`). If a story between `74b31759` and `<start>` added a hit, the plan lists it and removes it as a reference to the deleted class, or justifies it in Implementation Notes; the criterion is then "only the justified hits". No string resource, layout, `R.` reference, proguard rule, lint-baseline entry or document is used only by the receiver: the class uses no resource (its two messages are log literals), `app/proguard-rules.pro` and `app/lint-baseline.xml` do not name it (`git grep -n "SmsBroadcastReceiver" 74b31759 -- app/proguard-rules.pro app/lint-baseline.xml`: no hit), so nothing else is deleted.
- [ ] AC-6 (its tests go, no other test is edited): Given `<end>`, then (a) the four tests of `SmsBroadcastReceiverTest` are gone with the file (`shouldScheduleIncomingBackupAfterIncomingMessage`, `shouldNotScheduleIfAutoBackupIsDisabled`, `shouldNotScheduleIfLoginInformationIsNotSet`, `shouldNotScheduleIfFirstBackupHasNotBeenRun`); (b) `git diff --name-only <start>..<end> -- app/src/test` lists that deleted file and, only if AC-8 requires them, new test files or added test methods for the remaining receivers, and nothing else. At `74b31759` the search `git grep -n -E "SmsBroadcastReceiver|SMS_RECEIVED|receiver\.Sms" 74b31759 -- app/src/test` finds only `SmsBroadcastReceiverTest.java`; so these test files stay byte-identical: `receiver/BootReceiverTest.java`, `receiver/BackupBroadcastReceiverTest.java`, `compat/SmsReceiverTest.java`, `service/exception/ExceptionCoverageTest.java` (it uses the string `android.permission.RECEIVE_SMS` as data, `:31`), `worker/BackupTriggerWorkerTest.kt`, `service/WorkManagerSchedulerTest.kt` and `AppTest.java`. INV-4 evidence: `SmsBroadcastReceiverTest.shouldScheduleIncomingBackupAfterIncomingMessage` stops being INV-4 evidence (DES-MODERNIZATION-005 D-10); no test is added for INV-4, which `BackupTriggerWorkerTest` and the `inv4_*` tests carry (CR-017 item 5).
- [ ] AC-7 (what must not change): Given `<end>`, then each of these shows an empty diff `<start>..<end>` (`git diff --stat <start>..<end> -- <paths>` prints nothing): `app/src/main/java/com/zegoggles/smssync/compat` (`SmsReceiver.java` and the rest of the package), `app/src/main/java/com/zegoggles/smssync/receiver/BootReceiver.java`, `.../receiver/BackupBroadcastReceiver.java`, `.../receiver/PackageReplacedReceiver.java`, `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt`, `app/src/main/java/com/zegoggles/smssync/worker`, `app/src/main/java/com/zegoggles/smssync/activity/SmsDefaultRoleHelper.kt`, `app/src/main/res`, `app/proguard-rules.pro`, `app/lint.xml`, `gradle`, `settings.gradle`, `build.gradle`, `k9mail-vendored`. And `git diff --name-only <start>..<end> -- app/src/main` lists exactly four paths: the deleted class, `AndroidManifest.xml`, `App.java`, `BackupScheduler.java`. In the manifest, `<uses-permission android:name="android.permission.RECEIVE_SMS"/>` (`:17`) and the element `<receiver android:name=".compat.SmsReceiver" android:exported="true" android:permission="android.permission.BROADCAST_SMS">` with its `SMS_DELIVER` filter (`:167-173`) are unchanged (part of AC-2 (a): no line other than the receiver's is touched). `app/build.gradle` changes at most as AC-8 allows.
- [ ] AC-8 (the `receiver` package's coverage floor holds and is not lowered; CR-017 ruling 4): Given the JaCoCo XML report of `<start>` (`:app:jacocoTestReport`), the plan computes, before the first edit, the package's expected line ratio after the deletion as (covered lines of `com/zegoggles/smssync/receiver` − covered lines of `SmsBroadcastReceiver`) / (total lines of the package − total lines of `SmsBroadcastReceiver`), each figure read from the report's `<counter type="LINE">` of the package and of the class, and records the figures and the result. If the result is below the floor `minimum = 0.50` (`app/build.gradle`, Rule 11, `includes = ['com.zegoggles.smssync.receiver*']`, `:521-534` at `74b31759`; the comment records 29/58 at U-060, zero headroom), the story adds Robolectric unit tests for `BootReceiver`, `PackageReplacedReceiver` or `BackupBroadcastReceiver` (each asserting an observable outcome: the port call made or not made, the event emitted or not, through the existing `getScheduler` seam or `App.syncStateRepository()`) until the measured ratio at `<end>` is at or above 0.50; it does not lower the floor, add an exclusion, or change any other rule. At `<end>`, `./gradlew --console=plain :app:jacocoTestCoverageVerification` exits 0 and the measured ratio of the package is recorded. `git diff <start>..<end> -- app/build.gradle` is empty, or changes only the comment lines of Rule 11 to record the new measurement (the `minimum` value stays `0.50`). Every other package floor holds (the root package also loses one line of `App.java`).
- [ ] AC-9 (the merged manifests, static check; CR-017 Amendment 1, S-6): Given `./gradlew --console=plain :app:assembleDebug :app:assembleRelease` at `<end>`, when the merged manifest of each variant is read (the files under `app/build/intermediates/merged_manifests/` that story U-068 read; the exact path as produced, recorded), then in each: no line contains `SmsBroadcastReceiver` and no line contains `android.provider.Telephony.SMS_RECEIVED` (count 0 by `grep -c`); `<uses-permission android:name="android.permission.RECEIVE_SMS"` occurs once; the element for `com.zegoggles.smssync.compat.SmsReceiver` with `android.provider.Telephony.SMS_DELIVER` is present; and the receivers `BootReceiver`, `PackageReplacedReceiver`, `BackupBroadcastReceiver` are present. The same is read in the APKs by `apkanalyzer manifest print` (or `aapt2 dump xmltree --file AndroidManifest.xml`) and recorded. If a merged manifest at `<start>` already differs from the source in an element this criterion names, the difference is recorded and explained.
- [ ] AC-10 (the persisted enabled state after an upgrade; CR-017 "Not verified"): Given that the builds before this story call `setComponentEnabledSetting` for the receiver (`App.java:358-371`), so a device that ran them holds an explicit enabled or disabled state for a component the new build no longer declares, then the plan states, before the build, what the platform does with that persisted state on an upgrade that removes the component, labelled "read" (with the source and its lines) or "recalled, not read". The device check IC-3 records what the device shows (`dumpsys package`, before and after) and that the app starts and a restart is clean. A leftover name in the package's persisted component-state lists is recorded and is not a failure on its own; a crash, an `Unable to instantiate receiver` line or a `ClassNotFoundException` naming the receiver is.
- [ ] AC-11 (contract and design true; nothing edited there): Given `<end>`, then these texts are true against the code and are not edited: CNTR-MODERNIZATION-004 v4, the Consumers row (C-1, `:44` at `74b31759`), the INV-4 realization and note (C-11, C-12, C-30, `:283`: "the content trigger is the one incoming path"; "`scheduleIncoming()` stays a port operation; its production caller is the content-trigger worker"; "`RECEIVE_SMS` ... stays ... the story that removes the SMS-received receiver removes no permission"), the content-trigger example (C-19); DES-MODERNIZATION-005 D-7 with D-27 (`:323`) and D-10 (`:381`); DES-MODERNIZATION-014 E-1 to E-3. And item K-2 (`CNTR-MODERNIZATION-004...md:293`: "`SmsBroadcastReceiver`, its manifest entry and its enable/disable in `App` are still in the code") is false at `<end>`, shown by AC-1, AC-2, AC-3, so the orchestrator can apply X-2. Checked by `git diff --name-only <start>..<end> -- sdlc/artifacts/design sdlc/artifacts/requirements sdlc/artifacts/build/change-records` printing nothing, and by `git grep -n "scheduleIncoming()" <end> -- app/src/main` showing, outside comments, only the port declaration, `WorkManagerScheduler`'s implementation and the call in `BackupTriggerWorker.kt` (IC-2).
- [ ] AC-12 (gate, lint and counts): Given JDK 25 as declared by `gradle/gradle-daemon-jvm.properties`, in a shell without `MSYS_NO_PATHCONV`, when `./gradlew --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease :app:jacocoTestReport` runs before the first edit and again at `<end>`, then both runs exit 0 with 0 failures and the skipped count not higher than at the start. `./gradlew --console=plain :app:lintDebug` exits 0 at the start and at `<end>`, and `git diff <start>..<end> -- app/lint-baseline.xml` is empty or shows only deleted `<issue>` blocks (see Technical Context, "Lint baseline"). The authoritative count (`git grep -h "@Test" <commit> -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`) is recorded at `<start>` with the commit id, and at `<end>`; the end count equals **start count − R + A**, where R is the number of `@Test` in `SmsBroadcastReceiverTest.java` at `<start>` (4 at `74b31759`) and A is the number of tests AC-8 added (0 if none), each established by the plan from the files and each added test listed by name. No fixed count is stated here (REQ-MODERNIZATION-015 AC-13 as amended by CR-016).

### Integration Criteria

This story creates no component; it removes one. The scaffold's three criteria are restated for a removal.

- [ ] IC-1 (no registry names the removed component): Given `<end>`, the two places that registered the receiver with the platform no longer do: the manifest (AC-2) and `App.setBroadcastReceiversEnabled` (AC-3). No other registration exists: no code builds a `ComponentName` or `IntentFilter` for it (`git grep -n -E "registerReceiver|ComponentName" <end> -- app/src/main/java` is read and every hit accounted for; at `74b31759` `ComponentName` appears only in `enableOrDisableComponent`, which now receives `BootReceiver.class` only, and in the deleted class's unused import).
- [ ] IC-2 (the production path for incoming messages is the content trigger alone): Given `<end>`, an incoming SMS reaches the backup only through `WorkManagerScheduler.scheduleContentTrigger()` (content-URI trigger on the SMS provider) → `BackupTriggerWorker.doWork()` → `BackupScheduler.scheduleIncoming()` → unique work `INCOMING` → `BackupWorker`. `scheduleIncoming()` has one production caller, `BackupTriggerWorker` (`git grep -n "scheduleIncoming()" <end> -- app/src/main`, every hit accounted for: the port, the implementation, the worker's call, comments). IC-3 shows the path working on the device.
- [ ] IC-3 (device check; run by the orchestrator only, never by a subagent): Given the Android 16 emulator (AVD `Pixel_9_Pro`; `adb -s <serial> shell getprop ro.build.version.sdk` prints `36`), with the app and its mail account configured and auto backup on, and with no system setting changed, then:
  1. **Before**: `adb -s <serial> shell dumpsys package com.zegoggles.smssync` is saved (outside the repository); it shows the installed version and the receiver among the package's receivers (if the installed build does not have the receiver, the upgrade part of this check is "not conclusive: no build with the receiver installed").
  2. **Upgrade**: `adb -s <serial> install -r <the debug APK of <end>>` succeeds (`Success`), same application id and signing key, app data kept. If the install would need an uninstall or would lose the app's data, the check stops and is recorded "not run" with the reason.
  3. **Start**: the app is opened from the launcher and its main screen appears; the app's log (tag `SMSBackup+`, app process only) has no `FATAL EXCEPTION`, no `Unable to instantiate receiver` and no `ClassNotFoundException` or `ComponentInfo` line naming `SmsBroadcastReceiver`; in this debug build the verbose line `enableOrDisableComponent(<true|false>, BootReceiver)` appears and no `enableOrDisableComponent(` line names `SmsBroadcastReceiver`.
  4. **Incoming SMS** (story U-075 is done before this story starts, so the backup may start off screen): the app is opened once, then the Home key is pressed; one test SMS is sent through the emulator console with the app off screen; then the log shows `BackupTriggerWorker.doWork: content-URI trigger fired; enqueueing delayed incoming backup with initialDelay=<n>s`, then `WorkManagerScheduler.scheduleIncoming: enqueued INCOMING ...`, then, after the delay, `BackupWorker: starting backup (N messages)` with N of at least 1 and `BackupWorker: backup complete, backedUp=` with no `FATAL EXCEPTION` and no `unrecoverable error`. If U-075 were not done when the check runs, ruling 61 applies instead (the app stays on screen from before the SMS until the backup ends) and the evidence says so. If the trigger line does not appear within five minutes, or the work is held by the system (`dumpsys jobscheduler` shows `WITHIN_QUOTA` unsatisfied), the step is recorded "not conclusive" with the dump, not a pass.
  5. **After**: `dumpsys package com.zegoggles.smssync` shows no receiver named `SmsBroadcastReceiver` in the package's receiver list or in the receiver resolver table; whether the name still appears in the persisted component-state lists (`enabledComponents:` / `disabledComponents:`) is recorded (AC-10).
  6. **Restart**: `adb -s <serial> reboot`, wait until `getprop sys.boot_completed` prints `1`; the log shows `BootReceiver`'s `bootup` line (if auto backup is on) and no `FATAL EXCEPTION`, `Unable to instantiate receiver` or `ClassNotFoundException` from the app's process; the app opens again.
  Conditions (decision appendix Record 14 and Record 20): one test SMS through the emulator console is allowed; the AVD `Pixel_10_Pro` is never started or touched; only the owner enters a password (if the app asks for one, the check stops and is recorded "not run"); the app is never uninstalled; no system setting is changed. Evidence: the app's log lines passed through the sanitizer of story U-063 (no message content, no address), the `dumpsys` excerpts naming the package's receivers and component states, the build type, the Android level, the times, kept in the story's evidence folder; raw logs stay outside the repository. A step that cannot be run is recorded "not run" with the reason. What it does not prove: a release build on a device, Android 11 to 15, the owner's own phones.

## Affected Code

Lines as read at commit `74b31759` of branch `sdlc/modernization-plan` (2026-11-21), before story U-076. Locate by text after U-076.

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/receiver/SmsBroadcastReceiver.java` | Manifest receiver for `SMS_RECEIVED`; if auto backup on, login set and first backup done, calls `getScheduler(context).scheduleIncoming()` (`:44-111`, `:58-63`, `:66-84`). Never delivered: `exported="false"` and `RECEIVE_SMS` never requested at run time (BUG-023) | Deleted |
| `app/src/main/AndroidManifest.xml` (`:132-139`) | `<receiver android:name=".receiver.SmsBroadcastReceiver" android:enabled="true" android:exported="false" android:permission="android.permission.BROADCAST_SMS">` with an `SMS_RECEIVED` filter | The element deleted (AC-2). `:17` (`RECEIVE_SMS`) and `:167-173` (`compat.SmsReceiver`, `SMS_DELIVER`) unchanged |
| `app/src/main/java/com/zegoggles/smssync/App.java` (`:46`, `:255`, `:358-361`) | Imports the receiver; `setBroadcastReceiversEnabled(enabled)` toggles the receiver and `BootReceiver`, called at start (`:258`) and on every auto-backup settings event (`:289`) | Import and the receiver's toggle line deleted; the comment line names `BootReceiver` only (AC-3) |
| `app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java` (`:32-33`) | Javadoc: "All call sites (BackupBroadcastReceiver, BootReceiver, SmsBroadcastReceiver, SmsBackupService, App) are routed through this port." | The receiver's name dropped (AC-4). Story U-076 edits `:36` and `:58-92` of the same javadoc first |
| `app/src/test/java/com/zegoggles/smssync/receiver/SmsBroadcastReceiverTest.java` | 4 `@Test` (`:45`, `:51`, `:58`, `:65`) | Deleted |
| `app/src/test/java/com/zegoggles/smssync/receiver/` (new tests, only if AC-8 requires them) | `BootReceiverTest` 1 `@Test`, `BackupBroadcastReceiverTest` 2; `PackageReplacedReceiver` has none | Added tests only; the two existing files are not edited |
| `app/build.gradle` (Rule 11, `:521-534`) | Receiver package LINE floor 0.50, comment "U-060: 50.0% (29/58), zero headroom" | Nothing, or the comment lines only (AC-8); `minimum` stays 0.50 |

Read, not changed: `compat/SmsReceiver.java`, `activity/SmsDefaultRoleHelper.kt` (`:105-110`, `:128-131`), `worker/BackupTriggerWorker.kt`, `scheduler/WorkManagerScheduler.kt`, `app/lint-baseline.xml`, `app/proguard-rules.pro`.

## Existing Behavior to Preserve

- The content trigger and the two-stage debounce (CNTR-MODERNIZATION-004 INV-4): `scheduleContentTrigger()`, `BackupTriggerWorker` and `scheduleIncoming()` unchanged; `BackupTriggerWorkerTest` and the `inv4_*` tests pass unedited.
- The restore's default-SMS role: `compat/SmsReceiver` keeps receiving `SMS_DELIVER` while the app holds the role, so "Any incoming SMS messages during restore will get delivered as usual" (`strings.xml:346-347`) stays true. `RECEIVE_SMS` stays declared for it (CR-017 ruling 3).
- `BootReceiver`: still enabled when auto backup is on and disabled when it is off (`setBroadcastReceiversEnabled`), still calls `scheduleBootup()`; `BootReceiverTest` unedited.
- `BackupBroadcastReceiver` (the `com.zegoggles.smssync.BACKUP` intent, CNTR-MODERNIZATION-005) and `PackageReplacedReceiver` (`MY_PACKAGE_REPLACED` emits the settings event that re-registers the jobs): unchanged.
- The scheduler port surface: no operation, signature or `BackupType` value changes (CR-017 Part 4: not a break).
- Every other permission, component and attribute of the manifest.
- Every JaCoCo floor; no floor lowered.

## Verification Steps

For QA. Gradle in a shell without `MSYS_NO_PATHCONV`, JDK 25 (`./gradlew --version` shows the Daemon JVM line citing `gradle/gradle-daemon-jvm.properties`). The full gate takes about ten minutes and is run in the background by the harness: start it once, wait for its completion notice, read the log's exit line; never start a second Gradle while one runs. Device steps are the orchestrator's.

1. Start: confirm `<start>` contains U-076 as done (`git log`); record the authoritative count and the commit id; run the gate and `:app:lintDebug` once before the first edit; read the AC-8 figures from the JaCoCo XML report and the recorded calculation.
2. AC-1, AC-6 (a): `git ls-tree --name-only <end> app/src/main/java/com/zegoggles/smssync/receiver/` (three lines); `git diff --diff-filter=D --name-only <start>..<end>` (two lines).
3. AC-2: `git diff <start>..<end> -- app/src/main/AndroidManifest.xml` (one hunk, deletions only, the receiver's element); the three `git grep` counts of AC-2 (b) to (d).
4. AC-3, AC-4: `git diff <start>..<end> -- app/src/main/java/com/zegoggles/smssync/App.java app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java`; read `setBroadcastReceiversEnabled` at `<end>` (one statement, `BootReceiver.class`).
5. AC-5: `git grep -n "SmsBroadcastReceiver" <end> -- app README.md` and `git grep -n "SMS_RECEIVED" <end> -- app` (no output, or only the justified hits).
6. AC-6 (b), AC-7: `git diff --name-only <start>..<end> -- app/src/test`; `git diff --stat <start>..<end> -- <the AC-7 paths>` (no output); `git diff --name-only <start>..<end> -- app/src/main` (four paths).
7. AC-8: read the plan's calculation against the report; run `:app:jacocoTestCoverageVerification` (part of the gate); read `git diff <start>..<end> -- app/build.gradle`; if tests were added, read each for an observable assertion.
8. AC-9: open the debug and release merged manifests; run the `grep -c` checks; read the `apkanalyzer` (or `aapt2`) output.
9. AC-10: read the plan's statement and its label ("read" or "recalled"); compare with IC-3 steps 1 and 5.
10. AC-11: `git diff --name-only <start>..<end> -- sdlc/artifacts/design sdlc/artifacts/requirements sdlc/artifacts/build/change-records` (no output); read the named contract and design sentences against the code; `git grep -n "scheduleIncoming()" <end> -- app/src/main`.
11. AC-12: run the gate and `:app:lintDebug` at `<end>`; read `git diff <start>..<end> -- app/lint-baseline.xml`; compute start − R + A against the end count; read the list of added tests.
12. IC-1, IC-2: the two `git grep` readings; every hit accounted for.
13. IC-3 (orchestrator): `adb devices` lists the `Pixel_9_Pro` serial and not `Pixel_10_Pro`; run steps 1 to 6 (set `MSYS_NO_PATHCONV=1` for adb commands with Unix paths only, never for Gradle); record each result or "not run"/"not conclusive" with its reason.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Java, manifest, Robolectric, Gradle) | The deletions and edits of AC-1 to AC-5, the coverage calculation and any tests of AC-8, the lint-baseline reading, the merged-manifest check (AC-9), the gate and the counts | Developer |
| Device check | IC-3 on the AVD `Pixel_9_Pro` under Records 14 and 20 | Orchestrator (never a subagent) |
| Contract | Close-out edit X-2 (delete item K-2 from CNTR-MODERNIZATION-004) when this story closes | Orchestrator |

## Technical Context

### Facts verified at `74b31759` (read with `git show` and `git grep`, 2026-11-21)

- **The receiver** (`receiver/SmsBroadcastReceiver.java`): `onReceive` handles `android.provider.Telephony.SMS_RECEIVED` only (`:45`, `:51`); `shouldSchedule` requires auto backup on, login set and the first backup done (`:66-84`); then `getScheduler(context).scheduleIncoming()` (`:60`); messages "Received SMS but not set up to back up." and "Not set up to back up. ..." are log literals; it uses no resource. Unused imports (`ComponentName`, `PackageManager`, `DateFormat`, three static imports) go with the file.
- **Why it is never delivered** (BUG-023, "Severity"): `android:exported="false"` since U-001 (commit `50f3b34f`, 2026-06-02), and `RECEIVE_SMS` declared (`AndroidManifest.xml:17`) but never requested at run time; a debuggable emulator build showed 0 `onReceive(` lines. The platform rules are recalled in BUG-023, not fetched.
- **Manifest receivers at `74b31759`**: six `<receiver>` elements: `.receiver.SmsBroadcastReceiver` `:132`, `.receiver.BootReceiver` `:141`, `.receiver.PackageReplacedReceiver` `:149`, `.receiver.BackupBroadcastReceiver` `:157`, `.compat.SmsReceiver` `:167` (`SMS_DELIVER`, `exported="true"`, `BROADCAST_SMS`), `.compat.MmsReceiver` `:176`. Twenty `uses-permission` lines.
- **`App`**: `LOCAL_LOGV = DEBUG` (`:92`), so the verbose `enableOrDisableComponent(<enabled>, <SimpleName>)` line (`:365`) is written in debug builds only, which IC-3 step 3 reads. `setComponentEnabledSetting(..., ENABLED|DISABLED, DONT_KILL_APP)` (`:367-370`) persists an explicit component state on the device; that is the state AC-10 is about.
- **`RECEIVE_SMS`, who needs it** (CR-017 Evidence E-1, ruling 3): no Java or Kotlin file under `app/src/main` names it; the `SMS_DELIVER` receiver needs it while the app holds the SMS role during a restore (`SmsDefaultRoleHelper.kt:105-110`, `:128-131`); the platform rule that `SMS_DELIVER` requires the receiving app to hold `RECEIVE_SMS` is recalled, not read (CR-017 "Not verified").
- **Tests referencing the receiver**: only `SmsBroadcastReceiverTest.java` (`git grep` of `app/src/test` for `SmsBroadcastReceiver`, `SMS_RECEIVED`, `receiver.Sms`). The test count at `74b31759` is 771; this story does not rely on that figure.
- **Coverage**: Rule 11 of `app/build.gradle` (`:521-534`), LINE ≥ 0.50, comment "U-060 re-measured: exactly 50.0% (29/58) = ZERO HEADROOM". `SmsBroadcastReceiver` is covered by its four tests; the three remaining classes are covered by three tests between them (`BootReceiverTest` 1, `BackupBroadcastReceiverTest` 2; `PackageReplacedReceiver` none). Whether the deletion breaches the floor was not computed (no build was run for this story); because the deleted class is among the better-covered ones, a breach is plausible, so the plan computes it first (AC-8).
- **Lint baseline**: `app/lint-baseline.xml` has 26 `<issue>` entries (`grep -c '^ *<issue *$'`); none names the receiver. Four are in `src/main/AndroidManifest.xml`. Baseline entries are matched by issue id, file and message, not by line: their recorded lines are already stale (the `ExportedReceiver` entry for `BackupBroadcastReceiver` records line 167, where `74b31759` has `.compat.SmsReceiver`; `BackupBroadcastReceiver` is at `:157`). So the deletion is not expected to make an entry stale. The plan treats the baseline as story U-068 did (its AC-10): if `:app:lintDebug` reports a baseline entry as fixed, the baseline is regenerated on a scratch copy of the tree outside the repository, compared with `git diff --no-index`, and only deletions are applied to the real file; no entry is added, no message edited.
- **Persisted state of a removed component** (AC-10): not verified by CR-017 ("Expected to be dropped by the package manager on upgrade; harmless either way; not checked"). The plan states the platform's behaviour with its label; IC-3 shows it on one device.

### Start conditions

- **Order**: sprint 025, after story U-076 (the fix for BUG-026 and BUG-027; it edits the scheduler and the port's javadoc, `BackupScheduler.java:36`, `:58-92`) and before story U-073 (runtime libraries), whose "existing tests unedited" baseline then excludes `SmsBroadcastReceiverTest` (CR-017, "Interaction with U-073"). Story U-075 (sprint 024) is done before sprint 025 starts; IC-3 step 4 relies on it. Story U-078 follows this one.
- **Gate at start**: green at `<start>`; if not, stop and report.

### Implementation constraints

- **Comparison tools** (decision appendix Record 18): no external `diff` or `cmp`. Compare files with `git diff --no-index` or by `sha256sum`. Keep shell activity modest. If a tool disappears, or a process is killed without a visible reason, stop and report; restore nothing from quarantine and add no exclusion.
- **Gradle**: the full gate takes about ten minutes (since story U-070) and the harness moves it to the background. The plan writes each Gradle run to a log file with its exit code, waits for the completion notice rather than polling, and runs one Gradle at a time. Never with `MSYS_NO_PATHCONV=1`.
- **Read code at a commit** (`git show <commit>:<path>`) when the working tree under `app/` may hold another story's uncommitted work.
- No e-mail address, host name, user name or profile path in any committed file or evidence.

## Supporting Documentation

- Change record: CR-017, "Part 4", "What the story that removes the SMS-received receiver must cite" (items 1 to 6, read with Amendment 1: C-30, D-27, the merged-manifest check), "Orchestrator rulings on the scope concerns" (rulings 3, 4, 7, 8), "Close-out edits" (X-2), "Not verified".
- Bug: BUG-023 (Suggested Approach, "Severity, and the SMS-received receiver"; PO-3; the dated product-owner note).
- Decision appendix: Record 20 (the owner's answer and ruling 56), Record 18 (the comparison-tool rule), Records 14 and 20 (device conditions), Record 21 ruling 61 (on-screen rule until BUG-029 is fixed).
- Requirements: REQ-MODERNIZATION-005 (CR-017 note R-1b to R-1d; no criterion changed); REQ-MODERNIZATION-015 AC-13 as amended by CR-016 (count rule).
- Design: DES-MODERNIZATION-005 D-7, D-10, D-27; DES-MODERNIZATION-014 E-1 to E-3.
- Model for the lint-baseline treatment: story U-068 (`sdlc/artifacts/build/sprints/sprint-024/U-068-minsdk-24-floor.md`, AC-10).

## Integration Contract References

- `CNTR-MODERNIZATION-004#backup-scheduler` v4 (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md`): the Consumers row (C-1); INV-4 realization and note (C-11, C-12, C-30: the content trigger is the one incoming path; `RECEIVE_SMS` stays); the content-trigger example (C-19); "Code not yet at v4" item K-2, deleted by X-2 at this story's close. Ruling: not a break (Part 4). Built against, not edited.
- `CNTR-MODERNIZATION-006` line 44 names this receiver among event publishers; false today and after this story; the later housekeeping record's (CR-017 ruling 8). Not edited.

## Implementation Notes
<!-- Added by agents during build -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes

### Scope Concerns (raised, not acted on; the story is written to the decisions as approved)

- **SC-1: ruling 56's wording against CR-017 ruling 3.** Ruling 56 says the story deletes "the receive permission if nothing else needs it". CR-017 found that something else needs it (the `SMS_DELIVER` receiver during a restore) and ruled that it stays. The two agree; the story keeps `RECEIVE_SMS` and records the reason (AC-7, Existing Behavior). The underlying platform rule is recalled, not read; U-073's device check reads the grant during a restore.
- **SC-2: the comment above the `setBroadcastReceiversEnabled` call.** Its third line, "Receivers remain enabled (WorkManager does not need manual component toggling).", is not true of `BootReceiver`, which is disabled when auto backup is off. CR-017 asks only that the receiver's name be dropped (item 2), so AC-3 changes only that line. Correcting the third line, or renaming the method now that it toggles one receiver, is left for a later change.
- **SC-3: the port javadoc names `SmsBackupService`**, which is not a class under `app/src` at `74b31759` (outside this story). AC-4 removes only the receiver's name.
- **SC-4: the rulings table of CR-017 cites "decision appendix, Record 23"**, but the decision appendix ends at Record 22 at `74b31759`. The rulings are binding as written in CR-017; the record number is the orchestrator's to correct.
- **SC-5: placement.** CR-017 and ruling 56 place the story "after U-072, before U-073"; this story is placed after U-076 and before U-073, which satisfies both. CR-017 says the fix story keeps `App.java` unchanged (its item 6); if story U-076 does change `App.java`, this story's plan re-reads `App.java` at `<start>` (AC-3 locates by text).
