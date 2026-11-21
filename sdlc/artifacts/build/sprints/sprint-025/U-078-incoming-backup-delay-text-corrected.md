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
integration_contracts: []
dependencies:
  - U-075
  - U-077
change_records: []
related_items:
  - BUG-025
  - BUG-029
  - CR-017
  - U-070
  - U-075
  - U-076
  - U-077
  - U-073
  - CR-016
  - REQ-MODERNIZATION-015
  - CNTR-MODERNIZATION-004
platforms: []
tags:
  - text-only
  - help-text
  - readme
  - translations
  - incoming-backup
  - content-trigger
  - standby-bucket
  - product-owner-decision
  - bug-fix
gate_additions: []
id: U-078
title: Correct the Incoming schedule help text and the README so that they no longer promise a backup after the configured delay, admit a wait of up to about 15 minutes or longer when the app is rarely opened, and remove the 24 translations of the changed string so that they fall back to the corrected English (text only, no code change)
pipeline: ''
domain: modernization
requirement_source: bug:BUG-025
updated_at: '2026-11-21T18:10:57.986Z'
---

# U-078: Correct the Incoming Schedule Help Text and the README So That They No Longer Promise a Backup After the Configured Delay, Admit a Wait of Up to About 15 Minutes or Longer When the App Is Rarely Opened, and Let the Translations Fall Back to the Corrected English

## Story
As the owner of this private, unpublished SMS Backup+ build (its only user, who reads the setting's help text and the README to know when a new message will be in the backup),
I want the "Incoming schedule" help text and the README to say what the app actually does: it waits the chosen time, the backup can start up to about 15 minutes later, or longer if I rarely open the app, and opening the app lets a held backup run,
So that I do not rely on a promise ("any incoming SMS will trigger a backup after 3 minutes") the app cannot keep, and I know what to do when I want a backup at once.

Source: bug BUG-025 (`sdlc/artifacts/stories/BUG-025-incoming-sms-backup-delayed-by-trigger-period-and-job-quota.md`, status ready, severity medium, priority medium). The product owner decided on 2026-11-21: "Only correct the text" (decision appendix `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`, Record 20; ruling 54). By the dated note under the bug's Acceptance Criteria, the criteria on the trigger (AC-1, the re-firing part of AC-9, AC-12, and the parts of AC-6 and AC-8 about a changed trigger) are withdrawn; AC-2, AC-3 and AC-7 hold because no code changes; **AC-4 (Option S: state it), AC-5 (told to the owner) and AC-11 (the gate) are this story's.** The facts are the bug's Description (mechanism 1: the content trigger fires at most once per 15-minute period; mechanism 2: the job scheduler's quota by app standby bucket), the measurement record `sdlc/artifacts/build/research/2026-11-21-incoming-backup-delay-measurement.md` and the device observation `sdlc/artifacts/build/research/2026-11-21-background-backup-and-boot-observation.md` (finding 4). The rulings are binding and are not re-decided here. The bug closes with this story (ruling 54).

**What the change is, in one paragraph.** Three text sites change and nothing else: (1) the English summary string `ui_auto_backup_incoming_schedule_desc` in `app/src/main/res/values/strings.xml`, shown under "Incoming schedule" in the auto-backup settings; (2) the README FAQ answer "What's the difference between regular and incoming backup schedule?", its incoming paragraph; (3) the 24 translated copies of the same string in `app/src/main/res/values-*/strings.xml`, which are deleted so that every locale shows the corrected English text (ruling 54, the Solution Architect's proposal; kept after checking, see Technical Context, "Translations"). No Java, Kotlin, manifest or build file changes.

**Not in this story** (each ruled or recorded; none is an acceptance criterion):
- Any change to the content trigger, its 15-minute period or the incoming delay. The bug's Suggested Approach (the one-time re-armed trigger) stays on file for a later decision (ruling 54).
- Notifications, and any prompt to exempt the app from battery optimisation (product owner items PO-K and PO-L; BUG-025 Suggested Approach, ruling 2). The README's existing sentence suggesting the owner change "Battery optimization" by hand (`README.md:256-258` at `74b31759`) is not edited.
- The "Regular schedule" paragraph of the same README answer and the "Run at startup" line (`README.md:304-305`, `:238`): true once story U-076 is done (CR-017 hand-over list), not edited here.
- Any contract or design sentence. CR-017 ruled that no contract text bounds how soon an incoming SMS is backed up and that what the app promises belongs to this story (CR-017 Scope Concern 9, ruling 9); no sentence is added to CNTR-MODERNIZATION-004.
- The string `ui_backup_calllog_after_call_desc` ("Automatically backup after incoming/outgoing call (uses Incoming schedule).", `strings.xml:144`): it promises no time, and "uses Incoming schedule" stays true. Not edited.
- `CHANGES` (`:21`, "Optionally trigger a backup after incoming/outgoing calls"): release history, not edited.

## Acceptance Criteria

Terms. `<start>` is the commit this story starts from (it contains stories U-075, U-076 and U-077 as done). `<end>` is the story's last commit. "The string" is `ui_auto_backup_incoming_schedule_desc`. Line numbers are as read at commit `74b31759`; `README.md` gains lines from story U-070 before `<start>` (its plan section 3.9, P9a and P9b, both above the FAQ answer edited here), so every edit is located by its text.

- [ ] AC-1 (the English help text; BUG-025 AC-5): Given `<end>`, then `app/src/main/res/values/strings.xml` defines the string exactly once, and its value is, byte for byte:
  `Wait after an SMS arrives before backing up. The backup can start up to about 15 minutes later than this, or longer, for example if you rarely open the app. Open the app to let a held backup run, or tap Backup to back up now.`
  Present text at `74b31759` (`:170`): `When to backup incoming SMS (after arrival)`. The new value contains no apostrophe, no double quote, no `%`, `&` or `<`, and does not start with `@` or `?`, so it needs no XML or Android string escaping. `git diff <start>..<end> -- app/src/main/res/values/strings.xml` changes that one line and no other; the label `ui_auto_backup_incoming_schedule_label` ("Incoming schedule"), the entries and values of `auto_backup_incoming_schedule_*` (`arrays.xml:92-108`) and `preferences.xml:195-201` are unchanged.
- [ ] AC-2 (the README FAQ; BUG-025 AC-5): Given `<end>`, then in `README.md`, under the heading `#### <a id="faq-backup-schedule">What's the difference between regular and incoming backup schedule?</a>`, the first paragraph (at `74b31759` `:298-302`) is replaced by exactly these two paragraphs (line breaks inside a paragraph may differ; the words, punctuation and paragraph split may not):
  > Incoming backup schedule is used for incoming messages. 3 minutes here means that SMS Backup+ waits about 3 minutes after an incoming SMS before it starts a backup. It is a full backup (including any sent messages). Set the incoming schedule to a low value if you want new messages backed up soon after they arrive.
  >
  > The backup does not always start on time. SMS Backup+ notices new messages at most about once every 15 minutes, so a backup can start up to about 15 minutes later than the incoming schedule. If you rarely open SMS Backup+, Android can hold the app's background work for longer. Opening the app lets a held backup run; tap Backup to back up at once. With "Require Wifi" on, a backup also waits for a Wi-Fi connection.

  Present text at `74b31759`: "Incoming backup schedule is used for incoming messages. 3 minutes here means that any incoming SMS will trigger a backup after 3 minutes. It is a full backup (including any sent messages). You should set the incoming schedule to a low value if you want to make sure that incoming SMS show up in Gmail shortly after arrival." The heading, the table-of-contents line (`:177`), the "Regular schedule" paragraph and the battery-life paragraph after it are unchanged. `git diff <start>..<end> -- README.md` is one hunk inside that answer. The sentences sit in that answer, below U-070's two README additions (the Skip paragraph and the paragraph after the Reset answer); they neither repeat nor contradict them (AC-6).
- [ ] AC-3 (every other promising text site was searched and is listed): Given `74b31759`, the search `git grep -n -i -E "trigger a backup|after arrival|shortly after|incoming schedule" 74b31759 -- . ':!sdlc' ':!.sdlc' ':!app/src/main/res/values-*'` finds exactly: `CHANGES:21`, `README.md:299`, `:300`, `:302`, `strings.xml:144`, `:169`, `:170`; and `git grep -n -i "incoming" 74b31759 -- README.md DEVELOPMENT.md BUGS.md metadata` finds no other sentence about when an incoming message is backed up. So the sites that change are AC-1, AC-2 and AC-4 and no other; the rest are listed under "Not in this story" with the reason. CR-017's hand-over list names for this subject exactly `README.md:298-302` and `strings.xml:170` (CR-017 Evidence E-3, the BUG-025 rows; Impact Analysis row 8), which AC-1 and AC-2 cover. The plan re-runs both searches at `<start>` and records the output; a new hit added by an earlier story is listed and either edited under this story's wording rule (AC-5) or justified.
- [ ] AC-4 (translations; ruling 54): Given `<end>`, then none of the 24 translated `strings.xml` files defines the string: `git grep -n "ui_auto_backup_incoming_schedule_desc" <end> -- app/src/main/res` prints exactly two lines, `app/src/main/res/values/strings.xml:<n>` and `app/src/main/res/xml/preferences.xml:<n>` (the `android:summary` reference). At `74b31759` the string is in these 24 files, one line each: `values-ca`, `-cs`, `-da`, `-de`, `-es`, `-fr`, `-gl`, `-gr`, `-hu`, `-it`, `-ko`, `-nb-rNO`, `-nl`, `-pl`, `-pt-rPT`, `-ru`, `-sk`, `-sq`, `-sr`, `-sv`, `-tr`, `-uk`, `-zh-rCN`, `-zh-rTW` (every `values-*` folder of the app). `git diff --stat <start>..<end> -- 'app/src/main/res/values-*'` lists those 24 files with `1 -` each and no insertion; each file keeps its line endings and encoding, and no other line of it changes (`git diff --numstat` shows `0	1` per file). The translated label `ui_auto_backup_incoming_schedule_label` stays in each locale.
- [ ] AC-5 (the new text is true against the code and the measurement, and promises nothing the app cannot keep; BUG-025 AC-4 Option S and AC-5): Given `<end>`, each claim of AC-1 and AC-2 is checked against its source, cited at `74b31759` (the plan re-reads each at `<start>` and records the lines):
  (a) "waits about N minutes ... before it starts a backup" / "Wait after an SMS arrives before backing up": `scheduleIncoming()` enqueues `INCOMING` with `setInitialDelay(preferences.incomingTimeoutSecs)` (`WorkManagerScheduler.kt:133`, `:138`, `:148`), started by the trigger worker (`BackupTriggerWorker.kt:59-68`); options 1 min, 3 mins, 30 mins, 1 h, Never (`arrays.xml:92-108`), default 60 s (`Defaults.java:15`).
  (b) "notices new messages at most about once every 15 minutes" / "up to about 15 minutes later": the content trigger is a `PeriodicWorkRequest` of `BackupTriggerWorker` with `PERIODIC_MIN_INTERVAL_SECS` = 15 minutes (`WorkManagerScheduler.kt:116`, `:232-236`) and only content-URI constraints (`:224-230`); after a run the next run carries the full interval as minimum latency (BUG-025 Fact 3, read in the WorkManager 2.12.0 sources; the record's "Minimum latency: +14m59s993ms"). The first message after a registration fires within seconds (record: 11 s), so "up to" is the bound of mechanism 1 and "about" covers the platform's content delay.
  (c) "Android can hold the app's background work for longer" / "or longer, for example if you rarely open the app": the record (bucket 50: all three jobs held with `WITHIN_QUOTA` unsatisfied for about 15 minutes, backed up 17 min 50 s and 11 min 56 s after arrival) and the observation's finding 4 (in bucket 50 nothing ran until the app was opened). The text names rarely opening the app as an example, not as the only cause, and gives no upper bound for it (the record does not show how long a hold lasts).
  (d) "Opening the app lets a held backup run": both records show the held jobs running within a second of the app being opened (record 07:42:50; observation Run 2, 09:46:36). After story U-075 a backup that then starts off screen runs as ordinary background work instead of failing (U-075; observation finding 1 is the defect it fixes), which is why this story depends on U-075. The text does not claim that opening the app makes the trigger fire before its period ends.
  (e) "tap Backup to back up at once/now": the main screen's button is labelled "Backup" (`strings.xml:11`, `ui_sync_button_label_idle`); it calls `scheduleManual` (`MainViewModel.kt:143`), which enqueues with `Constraints.NONE` and no delay (`WorkManagerScheduler.kt:341-359`).
  (f) "With "Require Wifi" on, a backup also waits for a Wi-Fi connection": `networkConstraints` gives `UNMETERED` when `isWifiOnly` for every type but `BROADCAST_INTENT` (`WorkManagerScheduler.kt:463-468`); the setting's label is "Require Wifi" (`strings.xml:172`).
  (g) Not claimed, on purpose: that the regular schedule catches up (it is a scheduler job with the same quota exposure, and "Never" schedules none; BUG-025 "What the owner actually experiences"; CR-017 Rule 4a); that a progress notification appears (U-075: automatic backups show none); that a battery-optimisation exemption helps (not verified, BUG-025 ruling 2); that each new message restarts the wait (true only when the trigger fires again). The Solution Architect's draft wording in BUG-025 (Suggested Approach, ruling 2, "What the fix does") was not used as written, because its "the regular schedule then catches up" and "The regular schedule is the fallback" are claims of kind (g), and its README sentence omits mechanism 1.
  The check is recorded in Implementation Notes as a table: claim, source lines at `<start>`, true or not. A claim found untrue at `<start>` stops the story for the orchestrator; the developer does not reword the text on its own (AC-1, AC-2 are the approved wording; see Notes, SC-1).
- [ ] AC-6 (consistent with stories U-070 and U-075): Given `<end>`, then (a) U-070's README additions (its plan `sdlc/artifacts/build/sprints/sprint-024/U-070/plan.md` section 3.9: P9a after "uninstalled SMS Backup and this initial backup won't ever be backed up to Gmail." in the Skip paragraph, at about `:82-87` of `74b31759`; P9b after the Reset answer, at about `:274-275`) are present and byte-identical at `<start>` and `<end>` (`git diff <start>..<end> -- README.md` touches no line of them); they say when a late-visible message is backed up ("when Android shows it, provided that happens within 6 hours"), this story's paragraph says when a backup starts; neither restates the other; (b) nothing in the new text says or implies that the app must stay on screen for an automatic backup to succeed, or that a notification shows it (U-075).
- [ ] AC-7 (no code, manifest or build change; BUG-025 AC-2, AC-3, AC-7 hold): Given `<end>`, then `git diff --stat <start>..<end> -- app/src/main/java app/src/main/AndroidManifest.xml app/build.gradle` prints nothing, and `git diff --name-only <start>..<end>` lists exactly `README.md`, `app/src/main/res/values/strings.xml` and the 24 files of AC-4, plus this story's own `sdlc/` workspace files. `app/lint-baseline.xml`, `app/lint.xml`, `app/src/main/res/xml/preferences.xml`, `app/src/main/res/values/arrays.xml`, `gradle/`, `build.gradle`, `settings.gradle` are unchanged.
- [ ] AC-8 (resources compile, lint passes with no new baseline entry; BUG-025 AC-11): Given JDK 25 as declared by `gradle/gradle-daemon-jvm.properties`, in a shell without `MSYS_NO_PATHCONV`, when `./gradlew --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease :app:jacocoTestReport` runs before the first edit and at `<end>`, both exit 0 with 0 failures and the skipped count not higher than at the start; and `./gradlew --console=plain :app:lintDebug` exits 0 at the start and at `<end>` with `git diff <start>..<end> -- app/lint-baseline.xml` empty. `MissingTranslation` is set to `ignore` in `app/lint.xml` (`:8` at `74b31759`), so the deletions of AC-4 raise no lint issue; if lint reports any new issue, the story stops and reports (no baseline entry is added, no lint rule is changed).
- [ ] AC-9 (test count unchanged): Given `<start>` and `<end>`, the authoritative count (`git grep -h "@Test" <commit> -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`) is equal at both, recorded with the commit ids. No test pins the old text or the string's name: `git grep -n -E "ui_auto_backup_incoming_schedule|after arrival|When to backup incoming" 74b31759 -- app/src/test app/src/androidTest` prints nothing (re-run at `<start>`; a hit there is a stop for the orchestrator, because a test edit is outside "text only").
- [ ] AC-10 (seen on the device; run by the orchestrator only): Given the Android 16 emulator (AVD `Pixel_9_Pro`; `adb -s <serial> shell getprop ro.build.version.sdk` prints `36`), when the debug APK of `<end>` is installed over the existing install (`adb -s <serial> install -r <apk>`, `Success`, app data kept) and the orchestrator opens the app's settings, then "Auto backup settings", then a UI dump is taken (`adb -s <serial> shell uiautomator dump <device path>`, pulled to a location outside the repository), then the dump has a node whose `text` is "Incoming schedule" and a node whose `text` equals the AC-1 value byte for byte. The device locale is read (`adb -s <serial> shell getprop persist.sys.locale`, read only) and recorded; because no translation remains, the English text is expected in any locale. Recorded in the story's evidence as text: the two node texts, the locale, the build type, the Android level, the date; the dump file itself stays outside the repository. Conditions: no system setting changed; the AVD `Pixel_10_Pro` never started or touched; only the owner enters a password (if asked, the step is recorded "not run"); the app is never uninstalled; if the install would lose the app's data, the step is recorded "not run" with the reason. No SMS is sent for this check. A screenshot (`adb -s <serial> exec-out screencap -p`) is optional and, if taken, kept outside the repository.

### Integration Criteria

Not applicable: the story creates no component, changes no call site and no registration. The one resource reference, `android:summary="@string/ui_auto_backup_incoming_schedule_desc"` (`preferences.xml:198`), still resolves (AC-4, AC-8, AC-10).

## Affected Code

Lines as read at commit `74b31759` (2026-11-21).

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/res/values/strings.xml` (`:170`) | `ui_auto_backup_incoming_schedule_desc` = "When to backup incoming SMS (after arrival)", the summary of the "Incoming schedule" list preference (`preferences.xml:195-201`, static summary; no summary provider in `AutoBackupSettings.java`) | The value of AC-1 |
| `README.md` (`:298-302`) | "3 minutes here means that any incoming SMS will trigger a backup after 3 minutes. ... make sure that incoming SMS show up in Gmail shortly after arrival." | The two paragraphs of AC-2 |
| `app/src/main/res/values-*/strings.xml` (24 files) | Each translates the old promise (for example `values-de`: "Wieviele Minuten nach Eintreffen der SMS gesichert werden soll.") | The string's line deleted in each (AC-4) |

Read, not changed: `app/src/main/res/xml/preferences.xml`, `app/src/main/res/values/arrays.xml`, `app/lint.xml`, `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt`, `app/src/main/java/com/zegoggles/smssync/worker/BackupTriggerWorker.kt`, `app/src/main/java/com/zegoggles/smssync/activity/fragments/AutoBackupSettings.java`.

## Existing Behavior to Preserve

- Every line of code, the manifest and the build: the content trigger, its period, the incoming delay, the debounce (BUG-025 AC-2, AC-3, AC-7 hold because nothing changes).
- The setting's key, label, entries and values (`auto_backup_incoming_schedule`: 60, 180, 1800, 3600, -1) and its default (60 s).
- Every other string in every locale, including the translated label "Incoming schedule".
- U-070's README additions and every other README answer.
- Every test and every JaCoCo floor; the lint baseline.

## Verification Steps

For QA. Gradle in a shell without `MSYS_NO_PATHCONV`, JDK 25. The full gate takes about ten minutes and the harness runs it in the background: start it once, wait for its completion notice, read the log's exit line; one Gradle at a time. The device step is the orchestrator's.

1. Start: confirm `<start>` contains U-075, U-076 and U-077 as done; record the authoritative count and commit id; run the gate and `:app:lintDebug` before the first edit; re-run the two searches of AC-3 and the test search of AC-9 and keep their output.
2. AC-1: `git show <end>:app/src/main/res/values/strings.xml | grep -n "ui_auto_backup_incoming_schedule_desc"` (one line, the exact value); `git diff <start>..<end> -- app/src/main/res/values/strings.xml` (one line changed).
3. AC-2, AC-6 (a): `git diff <start>..<end> -- README.md` (one hunk, inside the faq-backup-schedule answer); compare the words with AC-2; confirm U-070's two additions are untouched.
4. AC-3: read the plan's search output against the list.
5. AC-4: `git grep -n "ui_auto_backup_incoming_schedule_desc" <end> -- app/src/main/res` (two lines); `git diff --numstat <start>..<end> -- 'app/src/main/res/values-*'` (24 lines, `0	1` each).
6. AC-5: read the claim table in Implementation Notes against the cited lines and the two research records.
7. AC-7: `git diff --stat <start>..<end> -- app/src/main/java app/src/main/AndroidManifest.xml app/build.gradle` (no output); `git diff --name-only <start>..<end>`.
8. AC-8, AC-9: the gate and `:app:lintDebug` at `<end>`; `git diff <start>..<end> -- app/lint-baseline.xml` (no output); the counts at both commits (equal).
9. AC-10 (orchestrator): `adb devices` shows the `Pixel_9_Pro` serial and not `Pixel_10_Pro`; install, open the settings, dump, read the two node texts; record as text (set `MSYS_NO_PATHCONV=1` for adb commands with Unix paths only, never for Gradle).

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android resources and README | AC-1, AC-2, AC-4 edits; the searches of AC-3 and AC-9; the claim table of AC-5; the gate, lint and counts | Developer |
| Device check | AC-10 on the AVD `Pixel_9_Pro` under Records 14 and 20 | Orchestrator (never a subagent) |

## Technical Context

### Facts verified at `74b31759` (read with `git show` and `git grep`, 2026-11-21)

- **Where the text is shown.** `preferences.xml:195-201`: `ListPreference` `auto_backup_incoming_schedule`, `android:summary="@string/ui_auto_backup_incoming_schedule_desc"`. `AutoBackupSettings.java` sets no summary provider for it (only the old-scheduler preference's summary, `:31`), so the summary is the string itself, which AC-10 reads.
- **Translations.** The string is defined in 24 translated files, one line each, and in the default file: 25 definitions in 25 `strings.xml` files, plus the reference in `preferences.xml` (`git grep -n ui_auto_backup_incoming_schedule_desc 74b31759 -- app/src/main/res`: 26 lines). The app has 24 `values-*` folders and every one carries it. BUG-025 (Impact Analysis row 7; Suggested Approach, ruling 2) counts "25 locale folders, 26 files with the default"; the count at `74b31759` is 24 and 25 (see Notes, SC-2).
- **Why deleting the translations is kept, not judged harmful.** (1) Every translation states the old promise in its own words (for example "after arrival", "how many minutes after the SMS arrives"); kept, they would leave the false promise in 24 languages, which AC-5 of the bug forbids. (2) Correct translations of a caveat cannot be checked for a private app with one user. (3) Android falls back to the default `values/strings.xml` for a string a locale lacks, so the setting shows the English text, not an error. (4) Lint's `MissingTranslation` is `ignore` in `app/lint.xml:8`, so the build and lint are unaffected. The cost is one English line in an otherwise translated settings screen for a user with a non-English locale; for this app's only user that is acceptable, and a later translation can add the string back.
- **The two mechanisms the text admits** (BUG-025 Description): mechanism 1, the trigger's 15-minute period (`WorkManagerScheduler.kt:116`, `:232-236`); mechanism 2, the job quota by app standby bucket (bucket 50 in the record, "never used"; the platform's quota tables were not read, BUG-025 "Not verified"). The measured waits were 17 min 50 s and 11 min 56 s in bucket 50, released by opening the app.
- **U-075's effect on the text.** Before U-075, a backup that started off screen failed at its first statement on Android 12 and later (observation, finding 1), so "opening the app lets a held backup run" would have been true only while the app stayed on screen. After U-075 an automatic backup runs as ordinary background work. The text describes behaviour after U-075; hence the dependency.
- **U-070's README text** (its plan section 3.9): P9a extends the Skip paragraph (near the top of the README, `:82-87` at `74b31759`); P9b adds a paragraph after the Reset answer (`:274-275`). Both are above the faq-backup-schedule answer (`:296-302`), which this story edits; after U-070 the FAQ answer is a few lines lower. The edits do not overlap.
- **No contract text promises a latency** (CR-017 Evidence E-3, "Notes to carry"; Scope Concern 9 and its ruling): no contract or design edit follows from this story.

### Start conditions

- **Order**: sprint 025, after story U-077 and before story U-073. Depends on story U-075 (the text describes behaviour after it). U-077 is a sequencing dependency (the sprint places it first; the two stories share no file). Story U-076 is done before U-077, so the README's "Regular schedule" paragraph, not edited here, is true at `<start>` (CR-017 hand-over list).
- **Gate at start**: green at `<start>`; if not, stop and report.

### Implementation constraints

- **Comparison tools** (decision appendix Record 18): no external `diff` or `cmp`. Compare files with `git diff --no-index` or by `sha256sum`. Keep shell activity modest. If a tool disappears, or a process is killed without a visible reason, stop and report; restore nothing from quarantine and add no exclusion.
- **Editing the 24 files**: delete the one line by its exact text; keep each file's line endings and encoding (several are non-Latin scripts); check each with `git diff --numstat` (AC-4). Do not reformat, re-sort or re-indent any file.
- **Gradle**: the full gate takes about ten minutes and the harness moves it to the background; the plan writes each run to a log file with its exit code, waits for the completion notice rather than polling, and runs one Gradle at a time. Never with `MSYS_NO_PATHCONV=1`.
- No e-mail address, host name, user name or profile path in any committed file or evidence.

## Supporting Documentation

- Bug: BUG-025 (Description and "Facts confirmed"; "What the requirements, contract and user-facing text promise"; the dated owner-decision note and AC-4, AC-5, AC-11; Suggested Approach, ruling 2, "What the fix does").
- Research: `sdlc/artifacts/build/research/2026-11-21-incoming-backup-delay-measurement.md`; `sdlc/artifacts/build/research/2026-11-21-background-backup-and-boot-observation.md` (finding 4; finding 1 for U-075).
- Decision appendix: Record 20 (the owner's answer "Only correct the text"; ruling 54), Record 18 (comparison-tool rule), Records 14 and 20 (device conditions).
- Change record: CR-017 (Evidence E-3, the BUG-025 rows; Impact Analysis row 8; Scope Concern 9 and its ruling).
- Stories: U-075 (automatic backups off screen), U-070 (plan section 3.9, the README additions).
- Requirements: REQ-MODERNIZATION-015 AC-13 as amended by CR-016 (count rule).

## Integration Contract References

None. CNTR-MODERNIZATION-004 INV-4 states the requested delay and no latency bound; it stays true and is not edited (CR-017 ruling 9).

## Implementation Notes
<!-- Added by agents during build -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes

### Scope Concerns (raised, not acted on; the story is written to the decisions as approved)

- **SC-1: the exact wording is proposed, not owner-approved.** BUG-025 AC-5 says "The wording is the owner's to approve (PO-1)". The owner decided "Only correct the text" and its content (a wait of up to about a quarter of an hour, or longer if the app is rarely opened), not the sentences. AC-1 and AC-2 give the sentences this story builds to. The orchestrator may show them to the owner before the build; a change by the owner is an amendment of AC-1 or AC-2 by the orchestrator, not an edit by the developer.
- **SC-2: count of translations.** BUG-025 says the string "exists in 25 locale folders, 26 files with the default"; at `74b31759` it is 24 translated files and the default (25 `strings.xml` files; 26 lines with the `preferences.xml` reference). The story uses the count read at `74b31759` and requires the plan to re-count at `<start>`.
- **SC-3: the battery-optimisation sentence in the README** (`README.md:256-258`: try setting "Battery optimization" to "Don't optimize") makes no promise, but whether an exemption lifts the job quota is not verified (BUG-025 ruling 2). It is left as it is (out of scope by the brief: no battery-optimisation prompts); a later text pass may qualify it.
- **SC-4: "15 minutes" for "a quarter of an hour".** The owner's words are "up to about a quarter of an hour"; the text uses "about 15 minutes" because the setting's own entries are in minutes. Same meaning.
