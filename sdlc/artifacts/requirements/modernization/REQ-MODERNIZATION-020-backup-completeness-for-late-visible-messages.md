---
status: approved
type: functional
artifact_type: requirement
related_requirements:
  - REQ-MODERNIZATION-017
  - REQ-MODERNIZATION-019
related_stories: []
related_design_docs:
  - DES-MODERNIZATION-014
traces_to:
  - EPIC-MODERNIZATION-006
change_records:
  - CR-016
id: REQ-MODERNIZATION-020
title: Back up every SMS that is visible to the app exactly once, including a message that becomes visible after newer messages were backed up
domain: modernization
epic: EPIC-MODERNIZATION-006
priority: high
notes: |
  Correctness requirement under EPIC-MODERNIZATION-006. It is not one of the epic's five
  dependency-currency requirements. REQ-MODERNIZATION-017 (platform level 37) depends on it at
  targetSdk 37, unless the platform measurement owned by DES-MODERNIZATION-014 step S5-I1 shows the
  app picking the message up after release with a passing positive control (see Notes).
  Behaviour only: the mechanism belongs to the plan of the fix story for BUG-021, which is the
  defect record this requirement serves.
  Created under BUG-021 decision DP-4, whose recommended default applies because the product owner
  has not ruled otherwise (BUG-021, "Decision points for the product owner"; confirmed as Record 14
  row 10). The sequencing (fix first, then targetSdk 37, if the measurement confirms the gap) is
  Record 13 row 4. Both records are in the "Product owner decision" appendix of
  sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md.
  Priority is high: see Rationale.
updated_at: '2026-11-21T08:43:13.366Z'
---

# REQ-MODERNIZATION-020: Back up every SMS that is visible to the app exactly once, including a message that becomes visible after newer messages were backed up

## Description

The SMS backup must upload every SMS message that is visible to the app in the SMS provider at
the time of a backup, or becomes visible to the app later, to the mail server exactly once. This
includes a message that becomes visible after newer messages have already been backed up, so that
its date is at or below the date the backup has already reached. "Late" has a stated bound: the
guarantee holds for a message whose date is less than W below the SMS watermark when the message
becomes visible. W is 6 hours (21,600,000 ms). A message that becomes visible later than that is not
guaranteed to be backed up (AC-3, AC-5).

The fix must not cause a message to be stored on the mail server twice, and must not change how
Skip, Reset, restore, the first run, MMS and the call log behave, except as AC-14 (a message hidden
at Skip time) and AC-16 (no restored message uploaded after an interrupted restore) state. Where a
trade-off cannot be avoided, a duplicate is preferred to a lost message. This requirement accepts two
duplicates only, both bounded: the one that an interruption after a confirmed append can cause
(AC-10), and the one after Android's own app backup when the platform changed a row (AC-19).

Terms used below (they are the vocabulary of this requirement; the code calls the first one
`max_synced_date`):

- **Watermark**: the one stored number per data type that records how far the backup has got: the
  date of the newest message the backup has confirmed. This requirement concerns the SMS watermark.
- **Visible to the app**: returned to the app by a query of the SMS provider run by the app itself.
  A row that exists in the provider but is withheld from the app is not visible to it.
- **Late-visible message**: an SMS that becomes visible to the app when its date is at or below the
  SMS watermark.
- **Bookkeeping**: whatever the app keeps locally, besides the watermark, to know what it has
  already backed up. This requirement does not say what it is.
- **Backed up**: appended to the mail server's SMS folder and confirmed by the server.

## Context

### Why it is needed

No requirement states what the SMS backup must include. The only related text is the constraint in
REQ-MODERNIZATION-017 (and the same sentence in sibling requirements and in the epic) that backup
results are identical for the user before and after a change. That sentence compares two versions;
it does not say that every message is backed up. The gap was found by design DES-MODERNIZATION-014
(decision D14) and is recorded as BUG-021. This requirement is the rule that BUG-021's fix
implements and that the targetSdk 37 story cites. This requirement changes no acceptance criterion or constraint of REQ-MODERNIZATION-017; REQ-MODERNIZATION-017 carries a sequencing note that points here (CR-016).

### What the code does today (verified by reading it on 2026-11-21, at commit `b9fd172f`)

| # | Fact | Source |
|---|------|--------|
| 1 | The SMS query is `date > ? AND type <> ?`, with the stored SMS watermark as the first argument and the draft type as the second, plus a contact-group clause when the user chose a group. It is sorted by `date` with an optional `LIMIT`. There is no look-back. | `app/src/main/java/com/zegoggles/smssync/service/BackupQueryBuilder.java:103-116`, `:60-63`, `:149-163` |
| 2 | A test pins that exact selection text, its arguments and the sort order. A fix that changes the selection must change this test on purpose. | `app/src/test/java/com/zegoggles/smssync/service/BackupQueryBuilderTest.java:38-40`, `:51-53` |
| 3 | After each batch the backup appends to the server and then sets the watermark to the value the transport returns, with no comparison to the stored value. The value is the largest `X-smssync-date` header of the batch, and for SMS that header is the provider's `date` column. So a batch made only of older rows would lower the watermark. | `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt:416-422`; `app/src/main/java/com/zegoggles/smssync/mail/ConversionResult.java:19-32`; `app/src/main/java/com/zegoggles/smssync/mail/HeaderGenerator.java:67` |
| 4 | Nothing records which provider rows were uploaded. The watermark is the only record. | the readers and writers listed in facts 1, 3 and 5 to 10 are all there are (search for `MaxSyncedDate` and `clearLastSyncData` over `app/src/main`) |
| 5 | Skip does not connect to the server. For each data type it sets the watermark to the date of the newest visible non-draft row. | `BackupWorker.kt:198-218`; `BackupQueryBuilder.java:84-90`; README lines 82-87 and 245-247 |
| 6 | The first run: the app is in its first run until any watermark key exists. When the first run finds nothing, it writes the sentinel -1 to the SMS and MMS watermarks. The first-backup dialog (Backup or Skip) is shown from the main screen. | `app/src/main/java/com/zegoggles/smssync/preferences/Preferences.java:220-227`; `BackupWorker.kt:254-263`; `app/src/main/java/com/zegoggles/smssync/mail/DataType.java:87`; `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java:396-397` |
| 7 | Restore raises the SMS watermark to the date of each row it inserts, only when that date is higher, after the insert. It never lowers it. | `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt:461-468` |
| 8 | Removing the account and resetting the settings both clear the watermarks of all three data types, and nothing else that records what was backed up. (Reset also forgets the saved default SMS app, and account removal also clears the sign-in tokens and turns auto backup off; neither concerns this requirement.) README says that after Reset all messages on the phone are backed up on the next run. | `app/src/main/java/com/zegoggles/smssync/activity/fragments/MainSettings.java:80-83`, `:92-96`; `app/src/main/java/com/zegoggles/smssync/preferences/DataTypePreferences.java:79-85`; `Preferences.java:254-259`; README lines 272-275 |
| 9 | The stored watermark is shown to the user as "last sync" and is also read by Restore. MMS is stored in seconds and returned in milliseconds; SMS and call log are stored as returned. | `DataTypePreferences.java:56-77`; `app/src/main/java/com/zegoggles/smssync/activity/fragments/AdvancedSettings.java:230`; `app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java:370`; `BackupQueryBuilder.java:118-123` |
| 10 | MMS and call log use the same query shape (`date > ?`) with their own watermarks, and their own tests. | `BackupQueryBuilder.java:118-147`; `BackupQueryBuilderTest.java:56-87` |
| 11 | The per-run item limit is shared across the data types: the rows one type returns reduce what the next type may return. | `app/src/main/java/com/zegoggles/smssync/service/BulkFetcher.java:19-36` |
| 12 | When auto backup is on, a content trigger over the SMS provider starts a backup when the provider changes, through the debounce of CNTR-MODERNIZATION-004 INV-4 (WorkManager, API 24 and above). It exists only when auto backup is on and the incoming-message timeout is above zero. | `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt:233`; `app/src/main/java/com/zegoggles/smssync/App.java:376-383` |
| 13 | The e-mail identity of a backed-up SMS is derived from the row's date, its address and a type value (the message type), so a row uploaded twice carries the same identity. `X-smssync-backup-time` is set to the time of each run, so it differs between uploads of the same row. The app does not rely on the server removing duplicates; whether the mail server does so is not established in any artifact. | `HeaderGenerator.java:50`, `:53`, `:108-128`; `app/src/main/java/com/zegoggles/smssync/mail/MessageGenerator.java:95`, `:118` |
| 14 | Android's own app backup is on, and its descriptor excludes only the sign-in file and the pinned-certificate file. The default preferences file, which holds the watermarks, is therefore copied to a new phone. | `app/src/main/AndroidManifest.xml:95-96`; `app/src/main/res/xml/backup_descriptor.xml:4-7` |

README line 101 concerns Restore skipping messages already on the phone. It is not about Skip.

### What the platform does (quoted from the research; nothing is added)

Source: `sdlc/artifacts/build/research/2026-11-21-latest-stable-versions.md`.

- Section 4.5, "Behaviour changes, apps targeting API 37 (Android 17)", row "OTP protection for
  standard SMS messages", anchor `#sms-otp-protection`, source
  https://developer.android.com/about/versions/17/behavior-changes-17 (page last updated
  2026-11-11): for most apps targeting 37, an SMS containing a one-time code "do not become
  available until three hours after receipt"; `SMS_RECEIVED_ACTION` is withheld and "SMS provider
  database queries are filtered" during the delay. The default SMS app is exempt.
- Section 4.6, "Behaviour changes, all apps on Android 17", row "SMS OTP protection", anchor
  `#sms-otp-all-apps`, source https://developer.android.com/about/versions/17/behavior-changes-all
  (page last updated 2026-11-09): the three-hour delay now also covers WebOTP-format messages for
  apps that are not the intended recipient, for all target levels; the broadcast is withheld and
  provider queries are filtered. The default SMS app is exempt.

What the research does not say, and this requirement therefore does not assume: whether a released
message keeps its receipt-time date; whether anything triggers a backup at release; which apps are
"most apps"; what makes a message "contain a one-time code" or be "WebOTP-format"; whether the app
can be "the intended recipient". The platform measurement named in Notes exists to answer the first
two.

### What follows

On a phone in normal use with auto backup on, an incoming message triggers a backup within minutes
(fact 12). So a message withheld for three hours, with its original date, is very likely to be
passed by the watermark before it is released. Today's query would then never return it, and nothing
reports it as missing. The logic defect holds for any row that becomes visible late with an old date,
whatever the cause; Android 17 is the one cause known today. The platform reach, by build:

| Build | Affected message |
|-------|------------------|
| Today's build (targetSdk 35) on an Android 17 device | WebOTP-format message only (section 4.6) |
| Today's build on Android 16 or older | None; the research lists no such rule below Android 17 |
| After targetSdk is raised to 37 | WebOTP-format message and standard SMS containing a one-time code (sections 4.5 and 4.6) |

Which of the owner's devices run Android 17 is not known. The app is private, for its owner's own
use, and the mail server is Gmail.

### Existing requirements read before writing

REQ-MODERNIZATION-017 (Constraints: "Behavior preserved") and the same sentence in
REQ-MODERNIZATION-014, REQ-MODERNIZATION-015 and the epic: related, not contradicted. They compare
before and after; this requirement states the absolute rule. A search of the approved requirements
for "watermark" and "max synced" found no match, so nothing duplicates this. No standard-type
requirement constrains the content of the backup. REQ-MODERNIZATION-019 (mail library replacement)
is related because it replaces the transport that the backup appends through (see Constraints).

## Requirements

### Completeness of the SMS backup

#### Acceptance Criteria

- [ ] AC-1: Given an SMS with date D0 becomes visible to the app, where D1 is the SMS watermark when
  it becomes visible, D0 is at or below D1, and D1 minus D0 is less than W, then the first backup
  that completes without error after it became visible uploads it. If more late-visible SMS are
  waiting than the per-run item limit allows, the rest are uploaded by the following backups and
  none is lost because of the limit. This holds unless a restore raised the SMS watermark to W or
  more above D0 before that backup (AC-5), and except for an SMS that became visible between an
  upgrade from the version before the fix and the first backup after that upgrade, which AC-18
  governs. It does not matter how long after becoming visible that backup runs, and it does not
  matter that backups ran in between before it became visible.
  Evidence: a unit test with a provider double that evaluates the query it is given: message B (date
  2000) is backed up, the watermark is 2000, message A (date 1000) is then added, and the next backup
  uploads A; and a unit test with a per-run limit of N and N + 1 late-visible SMS, which asserts that
  N are uploaded by the first backup and the last one by the second.
- [ ] AC-2: A late-visible message is subject to the same selection rules as any other SMS. A draft
  is never uploaded. When the user has chosen a contact group, a late-visible message is uploaded
  only if the same rules that apply to a message arriving in order would upload it. Evidence: a unit
  test with one late draft and one late message outside the chosen group; neither is uploaded.
- [ ] AC-3: W is 6 hours (21,600,000 ms), stated once as a named value with its derivation (see
  Rationale). Evidence: reading the value and its comment in the code.
- [ ] AC-4: A message that becomes visible with its date W minus 1 ms below the watermark is uploaded
  by the first backup that completes without error after it became visible. Evidence: a boundary unit
  test.
- [ ] AC-5: A message that becomes visible with its date W or more below the watermark, or whose date
  a restore has put W or more below the watermark before the next backup (AC-1), is not guaranteed to
  be backed up, and no test requires it to be uploaded. The limit is written for the
  owner in `README.md`, after the Reset answer (`README.md:274-275`), in these words or plainer ones
  that keep every fact: "SMS Backup+ backs up a message that Android shows it late, for example a
  message containing a one-time code that Android 17 holds back for three hours, provided Android
  shows it within 6 hours of the message's date. A message shown later than that may not be backed
  up. A Reset backs up every message on the phone again." Evidence: reading the README.
- [ ] AC-6: An SMS that becomes visible with a date above the watermark is uploaded as it is today,
  by the next backup, in ascending `date` order and with the same per-run item limit semantics.
  Evidence: the existing backup tests pass without edits, and a unit test with in-order rows.

### No message stored twice

"The same message" is judged by the mechanism, but the test must be able to tell: the same provider
row presented to the backup more than once, identified in the test by the row it came from and, in
the mail transport double, by the identity headers of each appended message.

#### Acceptance Criteria

- [ ] AC-7: Over at least three consecutive backups, none of them interrupted, on a provider that
  keeps the same rows and receives a late-visible row between runs, no provider row is appended more
  than once. At least one row that was uploaded in an earlier run and is dated within W below the
  watermark is part of the scenario. Evidence: a unit test that records every append and asserts that
  no row, and no message identity header, occurs twice across all runs.
- [ ] AC-8: A row whose type changes between two backups (for example from outbox to sent), with its
  date unchanged and within W below the watermark, is not appended a second time as a result of the
  fix. Evidence: a unit test.
- [ ] AC-9: When a backup is interrupted, or the append fails, before the server confirms the
  append, neither the watermark nor the bookkeeping has changed for that batch, and the next backup
  appends the batch. No message is lost (the confirmed-append rule of BUG-010 still holds). Evidence:
  a unit test, and `BackupWorkerWatermarkTest` passing.
- [ ] AC-10: When a backup is interrupted after the server confirms the append and before the
  watermark and bookkeeping are saved, the next backup may append that batch again. This duplicate is
  accepted, because a duplicate is preferred to a loss. It is limited to the messages of the one
  append call that was in flight, which is the same exposure as today, and a further backup after
  that appends none of them. Evidence: a unit test that interrupts at that point and asserts all
  three statements.
- [ ] AC-11: No sequence of interruptions leaves a message treated as backed up when the server has
  not confirmed it. Evidence: a unit test that interrupts at each point between "append requested"
  and "state saved" and asserts that every message the server did not confirm is appended by a later
  backup.

### The watermark

#### Acceptance Criteria

- [ ] AC-12: A backup never moves the SMS watermark backwards, and the "last sync" dates shown to the
  user (`AdvancedSettings.java:230`, `StatusPreference.java:370`) are still read from the stored
  watermarks, so uploading a late-visible SMS older than the SMS watermark does not change them. Evidence: a unit
  test that uploads a batch made only of a late-visible, older row and asserts the stored watermark
  afterwards is equal to or higher than before, and `git diff` showing the two display lines
  unchanged.

### Skip, Reset, restore, first run, upgrade and Android's own backup

#### Acceptance Criteria

- [ ] AC-13: Skip, messages visible at Skip time: Skip connects to no server and uploads nothing.
  Every SMS visible to the app at the time of Skip is treated as backed up until a Reset or an
  account removal: no later backup uploads it, whatever its date relative to the watermark.
  Evidence: a unit test that skips over a provider with rows dated W or more below the new watermark
  and rows dated within W below it, runs a backup, and asserts nothing is uploaded.
- [ ] AC-14: Skip, message hidden at Skip time: an SMS that exists in the provider but is not visible
  to the app at Skip time, and becomes visible later with its date less than W below the watermark,
  is uploaded by the first backup after it became visible. This is the product decision for BUG-021
  DP-3 (Record 14 row 10): Skip means "do not send what I can see now"; the app cannot know about a
  hidden row, and a backed-up extra message costs less than a lost one. The README's description of
  Skip (`README.md:82-87`) says so. Evidence: a unit test that adds the row after Skip, and reading
  the README.
- [ ] AC-15: Reset and account removal: after either, the app is in its first run
  (`Preferences.isFirstBackup()` is true) and nothing left from earlier backups suppresses any SMS.
  If the user then chooses Backup, every SMS visible to the app is uploaded once by the backups that
  follow (more than one backup when the maximum items per sync is smaller than the number of SMS),
  and a further backup uploads none of them again. If the user chooses Skip, AC-13 applies.
  Evidence: a unit test that runs backups, adds a late-visible row, resets, backs up until nothing is
  left, backs up again, and asserts the upload counts.
- [ ] AC-16: Restore: after a restore, a later backup does not upload any SMS that the restore
  inserted, and does not skip a late-visible SMS that AC-1 requires to be uploaded. The restore's own
  check for messages already on the phone, and its watermark update, are unchanged. This is stricter
  than today on purpose: the watermark is raised after each insert (`RestoreWorker.kt:461-468`), so
  an interruption between the two can leave an inserted row above it, which today's next backup would
  upload, and uploading a message that came from the server is a duplicate the fix must not add.
  Evidence: a unit test that restores rows dated within W below the watermark and asserts the next
  backup uploads none of them; a second unit test that adds a late-visible row, restores a row dated 2
  hours above it, and asserts the next backup uploads the late row.
- [ ] AC-17: First run after installation, no watermark: behaviour is identical to today. Every
  visible SMS is selected, subject to the maximum items per sync, and uploaded once; when the first
  run finds nothing, the sentinel -1 is written to the SMS and MMS watermarks as today
  (`BackupWorker.kt:257-261`); the app stays in its first run until a watermark exists. Evidence:
  the existing first-run tests pass without edits.
- [ ] AC-18: First run after upgrading from the current version, with a stored watermark and no
  bookkeeping: the first backup after the upgrade uploads no SMS that is visible at its start with a
  date at or below the watermark, and uploads every SMS dated above the watermark as before. An SMS
  released between the upgrade and that first backup, with a date at or below the watermark, is
  therefore not uploaded; that is today's outcome. An SMS that becomes visible after that first backup
  is uploaded as in AC-1. Messages that the earlier version lost stay lost (Out of scope). Evidence:
  a unit test that starts from a stored watermark and an empty bookkeeping, with rows both below and
  above the watermark inside W, and asserts exactly which rows are uploaded.
- [ ] AC-19: After Android restores the app's data onto another phone (fact 14), the first backup
  there uploads no SMS dated at or below the restored watermark that the old phone had backed up,
  unless the platform changed the row's date, address or text. In that case a duplicate is accepted,
  never a loss, and no test requires a duplicate. Evidence: a unit test that copies to a fresh store
  exactly the files that `backup_descriptor.xml` lets Android copy, over a provider holding the same
  rows with new row ids, runs a backup, and asserts nothing is uploaded; and a second unit test in
  which the new phone also holds an SMS dated within W below the restored watermark that the old
  phone never backed up, asserting that this SMS is uploaded.

### MMS and call log

#### Acceptance Criteria

- [ ] AC-20: MMS and call log are unchanged: their queries, watermarks (including MMS seconds and
  milliseconds handling) and tests. Evidence: the `BackupQueryBuilderTest` cases for MMS and call log
  (`:56-87`) pass without edits, and `git diff` shows no change to their query code.

### Cost

#### Acceptance Criteria

- [ ] AC-21: The extra SMS rows that a backup examines beyond today's are limited to rows dated
  within W below the watermark at the start of the run. Evidence: a unit test with a provider double
  that records which rows it returned, asserting that no row dated W or more below the watermark at
  the start of the run is returned to the backup.
- [ ] AC-22: A late-visible SMS that a run uploads counts against the per-run item limit like any
  other row (BUG-021 AC-7). An SMS already backed up, examined again only because its date is within W
  below the watermark, is not counted against the limit and is not uploaded. Evidence: a unit test
  with limit N, at least N SMS already backed up and dated within W below the watermark, one
  late-visible SMS and N SMS dated above the watermark; the run uploads N items, the late-visible SMS
  among them, and the next run uploads the rest.
- [ ] AC-23: The local storage the fix adds does not grow with the age of the installation. Its size
  is bounded by the number of SMS dated within W below the watermark, and the fix story states the
  largest size it expects, with its reasoning. Keeping the size bounded never causes an SMS dated
  within W below the watermark to be appended twice or lost. Evidence: a unit test that runs 200
  backups of 50 SMS each, in date order, each group dated more than W after the one before, and
  asserts that the size after the 200th backup is no larger than after the 2nd.

### Platforms that never produce late-visible messages

#### Acceptance Criteria

- [ ] AC-24: When no row ever becomes visible below the watermark, a sequence of backups uploads the
  same messages, in the same order, with the same headers apart from `X-smssync-backup-time` and, if the version code changes,
  `X-smssync-version`, to the
  same folder, and leaves the same watermark after each run, as the code before the fix does.
  Evidence: `BackupWorkerWatermarkTest` and `BackupWorkerIntegrationTest` pass without edits (so the
  signature of `appendBatchAndUpdateWatermark`, `BackupWorker.kt:404`, is frozen), and a unit test
  compares the uploads of an in-order sequence.

### Device check of the fixed build

#### Acceptance Criteria

- [ ] AC-25: Device check of the fixed build (BUG-021 AC-12), run by the orchestrator or QA as
  DES-MODERNIZATION-014 D14 "After the fix" describes, for each case whose S5-I1 outcome was "gap
  confirmed" or "gap not ruled out". The first manual backup after release logs `backup complete,
  backedUp=1` (`BackupWorker.kt:369`), and the next two manual backups log `nothing to backup` or
  `backedUp=0`. For a case whose outcome was "measured not affected", "inconclusive" or "not run", the
  record states that outcome and the check is recorded as not reached. Evidence: text only (log lines
  and command output); a step not reached is recorded as not reached, never as passed.

## Rationale

**Why a requirement.** The product's one job is to back up the owner's messages. A message that is
silently and permanently missing is the worst failure it has, and no requirement said that
completeness was the rule. The comparison "identical before and after" in REQ-MODERNIZATION-017
cannot be met at targetSdk 37 if the gap is real, because the behaviour is no longer identical
(design DES-MODERNIZATION-014, D14). Putting the rule in its own requirement, rather than in a
change record against REQ-MODERNIZATION-017, keeps a lasting product promise out of a requirement
that is closed once level 37 has landed. That is the Solution Architect's advice on BUG-021 and the
decision of Record 14 row 10.

**Why a bound.** The only known cause of late rows is the platform's three-hour delay. A complete
record of every row ever uploaded would grow with the age of the installation (BUG-021, DP-1). A
bound keeps the cost fixed. What lies beyond the bound is stated as not guaranteed, and the owner is
told (AC-5).

**Why W is 6 hours.** The platform's delay is three hours after receipt (research sections 4.5 and
4.6). A margin is added for release-time jitter and for differences between the clock that dates a
message and the clock that releases it. Neither is measured. The margin chosen is another three
hours, equal to the delay itself, so W = 3 h + 3 h = 6 h = 21,600,000 ms. This is the value the bug
recommends (AC-2 of BUG-021). It is measured against the watermark and not against the time the
backup runs, and that is why the backup itself may run arbitrarily late. Only a backup or a restore
moves the watermark up after the first run (Skip runs only in the first run). A backup cannot move it
past the release time before it has seen the released message, because messages carry receipt-time
dates and none is later than the clock. A restore can, because it raises the watermark to the dates
of the messages it inserts. That case is not guaranteed (AC-1, AC-5). A row dated in the future (set
by a sender, or by a clock error) can also push the watermark past the clock and shorten the
effective window; large skew was not examined, and the margin covers small skew only. Run A of the
platform measurement (DES-MODERNIZATION-014 D14, S5-I1) records whether the message is visible to
the app by T0 + 3 h 10 min. When Run A ends in `nothing to backup`, Run T checks every 30 minutes
from T0 + 3 h 10 min up to T0 + 6 h and records when the message first appears. That is the only
evidence of the delay's length: to 30 minutes, on one emulator, in one run, and nothing beyond 6
hours. Jitter is not measured. If a delay longer than 3 hours is observed, or the message is not
visible by T0 + 6 h, W is revisited by a change record and must be at least the observed delay plus a
margin.

**Why a duplicate is preferred to a loss.** A duplicate in the mail folder is visible and can be
removed. A lost message is not noticed. This is BUG-021's decision DP-5 and Record 14 row 10. It
governs only the cases that cannot be made to meet both rules: the interruption after a confirmed
append (AC-10), and a row that the platform changed between two phones (AC-19). AC-7, AC-8, AC-16,
AC-19 (for every row the platform did not change) and AC-23 forbid
any duplicate that the fix itself introduces, because the mail server's append operation is not
idempotent and the app cannot undo it.

**Why Android's own backup is in scope.** Android copies the preferences file that holds the
watermark to a new phone (fact 14). The new phone's provider then holds the same messages under
different row ids, and a fix that recognised messages by row id, or that kept its own record outside
that file, would upload them all again on the new phone. AC-19 states the outcome and leaves the
mechanism to the story.

**Why priority is high.** What is lost is a text message, silently, on the app's main path; that
weighs towards high. BUG-021 is rated medium severity (narrow reach: only the messages the platform
withholds; an Android 17 device is needed; unmeasured platform behaviour; a private app with one
user; a Reset recovers everything at the cost of re-uploading all). The priority is high because the
sequencing of Record 13 row 4 puts this fix before `targetSdkVersion 37`, so the platform level
requirement and everything that depends on it wait for this one, and because today's build is
already exposed to the WebOTP-format case on Android 17.

## Constraints

- No dependence on the mail library that REQ-MODERNIZATION-019 Part B replaces. The behaviour is
  reached through the existing `MailTransport` port only; a server-side search would need a library
  call that the port does not have, and is not assumed.
- No change to the `MailTransport` port contract (CNTR-MODERNIZATION-007) unless a change record says
  so first.
- No new permission in the manifest. This is intended: the fix reads the same providers the app reads
  today and adds nothing the app is not already allowed to do.
- Do not rely on the mail server removing duplicates by `Message-ID`. Whether it does is not
  established.
- The device steps are run by the orchestrator or QA, never by a subagent with a background shell.
  The emulator named `Pixel_10_Pro` is never started or touched. Only the owner handles the account
  password. The emulator clock is never changed.
- Build on JDK 25, dependency verification stays enforced, and no per-package JaCoCo line floor is
  lowered. `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`
  exits 0 with 0 failures and with no more skipped tests than before the change. The authoritative
  test count is taken with the `git grep` command in the project's CLAUDE.md before and after, and
  any difference is explained in the story.
- The public `com.zegoggles.smssync.BACKUP` broadcast contract (CNTR-MODERNIZATION-005) is unchanged.
- Behaviour preserved: apart from the late-visible messages this requirement adds, backup and restore
  results are identical for the user before and after (the Skip outcome of AC-14 and the restore
  outcome of AC-16 differ on purpose, for the reasons given there).

## Out of scope

- Recovering messages that were already skipped before the fix. The upgrade keeps today's outcome for
  them (AC-18). A one-off check of the phone against the server is a separate decision for the
  owner.
- MMS and call-log late rows (BUG-021, DP-2: SMS only; Record 14 row 10). The research names
  "standard SMS messages" only. Any real late-row case for MMS or call log would be added by a
  recorded decision, as new numbered criteria.
- Messages that other apps insert with old dates, and a second device or profile that syncs old rows
  into the provider. These are not designed for, measured or tested. The criteria are written on what
  the app can observe, so a row from such a source that lies within W of the watermark is caught as
  well, but no claim is made for any source other than the platform's one-time-code delay. (Android's
  own app backup to another phone is a different case and is in scope: AC-19.)
- Messages that become visible W or more after their date (AC-5).
- Any change to what "Skip" or "Reset" means to the user beyond AC-14 and the README sentences in
  AC-5 and AC-14.
- Publishing and release decisions (epic Out of Scope).

## Verification Method

Unit level (no emulator, no account, no network): AC-1, AC-2, AC-4, AC-6 to AC-19 and AC-21 to
AC-24, using a provider double that evaluates the query and a recording stub of the mail transport.
Documentation: AC-3, AC-5 and AC-14 (the README part) by reading the code comment and the README.
Static: AC-12 (display lines), AC-20 and the constraints by `git diff` (no change under
`k9mail-vendored`, no new manifest permission, no change to the MMS or call-log query code, no new
call into the mail library other than the existing port). Device: AC-25 on the S5-I1 emulator, with
text evidence; a step not reached is recorded as not reached, never as passed. The regression test
that fails on the code before the fix and passes after is owned by BUG-021 AC-9 (see Notes).

## Notes

**Relation to EPIC-MODERNIZATION-006.** The epic's five requirements are dependency-currency
requirements: toolchain and minSdk 24, runtime libraries, platform level 37 (REQ-MODERNIZATION-017),
donation removal (REQ-MODERNIZATION-018) and mail library replacement (REQ-MODERNIZATION-019). This
requirement is none of them. It is a correctness requirement that the epic's platform-level
requirement depends on at targetSdk 37. REQ-MODERNIZATION-017 AC-3 requires every behaviour change at
level 37 to be dispositioned, and AC-5 requires every change dispositioned "applicable and fixed" to
land with the level raise or earlier. DES-MODERNIZATION-014 row 37-5 disposes of the one-time-code
change as fixed by this requirement's fix before the raise
(`sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-toolchain-runtime-libraries-platform-level-design.md`,
D13 table, row 37-5).
It is linked under the epic by its `epic` field and is cited by the story that raises targetSdk to
37. The epic's text had no line for a requirement of this kind. Since 2026-11-21 (CR-016) the epic's
Related Requirements and Sequencing carry one.

**Dependencies and sequencing.**

- Before the targetSdk 37 change of REQ-MODERNIZATION-017 (Record 13 row 4). compileSdk 37 does not
  wait for it. The platform measurement may run early with today's build, because it needs no code.
- BUG-021 is the defect record. Its fix story implements this requirement and plans the mechanism.
  BUG-021's acceptance criteria and this requirement overlap; where they differ, this requirement
  states what the product promises and BUG-021's criteria state the fix's own checks. One difference
  is deliberate: AC-22 says what BUG-021 AC-7 says about the limit, and adds that already-backed-up
  window rows are not counted.
- BUG-022 (the worker cancellation checks) and the runtime-library work (WorkManager and coroutines)
  touch the same file, `BackupWorker.kt`, and the loop around `BackupWorker.kt:330-331`. They must
  not be changed in parallel with this fix; one after the other. Record 14 row 11 schedules BUG-022
  after story U-066 and before the toolchain story. The Solution Architect advised BUG-022 first,
  then this fix, then the runtime-library story. That order belongs to the plans.
- REQ-MODERNIZATION-019 Part B replaces the mail transport underneath. This requirement does not
  depend on library behaviour (Constraints), so the order of the two does not matter to it.

**Platform measurement (not a criterion).** What Android 17 does is measured by
DES-MODERNIZATION-014 S5-I1, which owns the single procedure (counting control, positive control,
preconditions, repeated checks). Its outcome decides sequencing only. If it shows the app picking the
message up after release, with a passing positive control, the targetSdk 37 change of
REQ-MODERNIZATION-017 does not wait for this requirement. For any other outcome, or if it is not run,
it waits (the "inconclusive means wait" rule is in the outcomes table under DES-MODERNIZATION-014 D14 "Stop rule"). Every
criterion here stays in force whatever the outcome, because the rule is a completeness guarantee for
any late-visible message and costs little. If the platform turns out not to produce late-visible
messages, BUG-021's severity falls to low (BUG-021, Technical Context) and this requirement's
priority falls to medium. The other sources of late rows are listed in Out of scope.

**Regression test.** The unit-level reproduction that fails on the code before the fix and passes
after, and is not ignored, is BUG-021 AC-9 (with its Steps to Reproduce (a) and Verification Step 1,
which require the failing run on the commit before the fix). It is kept there and not repeated here.
AC-1 states the scenario it must reproduce.

**Not known and not assumed.** Whether Gmail removes duplicates on append; which owner devices run
Android 17; what text the platform treats as a one-time code; whether the emulator's delay matches a
device's. Each is recorded, none is relied on.

**Facts not verified here.** Whether IMAP append is idempotent is taken from the protocol and from
BUG-021 (AC-3); it is not tested against the server.

Traceability: `sdlc/artifacts/stories/BUG-021-delayed-otp-sms-skipped-by-backup-watermark.md`;
`sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-toolchain-runtime-libraries-platform-level-design.md`
(D14, rows 37-5 and 17-2, measurement S5-I1);
`sdlc/artifacts/build/research/2026-11-21-latest-stable-versions.md` (sections 4.5 and 4.6);
`sdlc/artifacts/requirements/modernization/EPIC-MODERNIZATION-006-dependency-currency-latest-stable.md`;
`sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-017-platform-level-current-stable-api.md`.

Amended 2026-11-21 by CR-016: cross-references only (Context sentence on REQ-MODERNIZATION-017; the epic sentence in Notes). No acceptance criterion, constraint or priority is changed.
