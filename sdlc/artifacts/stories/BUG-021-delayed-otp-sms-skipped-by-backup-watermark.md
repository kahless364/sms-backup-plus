---
type: bug
status: ready
artifact_type: bug
severity: medium
priority: high
complexity: high
parallel_eligible: true
iteration: 1
requirements:
  - REQ-MODERNIZATION-017
design_docs:
  - DES-MODERNIZATION-014
integration_contracts:
  - CNTR-MODERNIZATION-004
  - CNTR-MODERNIZATION-007
dependencies: []
related_items:
  - DES-MODERNIZATION-014
  - REQ-MODERNIZATION-017
  - REQ-MODERNIZATION-019
  - BUG-010
platforms: []
tags:
  - sms-backup
  - watermark
  - android-17
  - data-completeness
id: BUG-021
title: A text message that becomes visible to the app late, with its original date, is never backed up once a later message has moved the backup watermark past it
domain: build
origin: code-review
updated_at: '2026-11-21T08:18:32.222Z'
---

# BUG-021: A text message that becomes visible to the app late, with its original date, is never backed up once a later message has moved the backup watermark past it

## Description

The SMS backup remembers one number per data type: the date of the newest message it has backed up (the "watermark", stored as `max_synced_date`). Each run asks the SMS provider only for rows whose `date` is greater than that number, and after each confirmed upload it sets the number to the newest date in the batch. It keeps no look-back and no record of which rows it uploaded. A row that becomes visible to the app after the watermark has passed its `date` is therefore never selected, and nothing reports it as missing.

Android 17 gives the app such rows. Quoting the research (`sdlc/artifacts/build/research/2026-11-21-latest-stable-versions.md`, section 4.5, row "OTP protection for standard SMS messages", source https://developer.android.com/about/versions/17/behavior-changes-17, anchor `#sms-otp-protection`): for most apps targeting API 37, an SMS containing a one-time code "do not become available until three hours after receipt"; `SMS_RECEIVED_ACTION` is withheld and "SMS provider database queries are filtered" during the delay; the default SMS app is exempt. Section 4.6 (row "SMS OTP protection", source https://developer.android.com/about/versions/17/behavior-changes-all, anchor `#sms-otp-all-apps`): the delay "now also covers WebOTP-format messages for apps that are not the intended recipient, for all target levels; broadcast withheld and provider queries filtered. Default SMS app exempt."

The bug was found by design DES-MODERNIZATION-014, decision D14 (code-review origin; the platform-level work was being designed when the query and watermark were read side by side).

### What is established, and what is not

Two different kinds of statement are made here and must not be mixed.

**Established by reading the code at HEAD `b9fd172f`** (the logic is a defect for any row that becomes visible late with an old date, whatever caused it):

- The SMS query is `date > ? AND type <> ?`, with the first argument `getMaxSyncedDate(SMS)` and the second the draft type (3). Sort is `date`, with an optional `LIMIT` (`app/src/main/java/com/zegoggles/smssync/service/BackupQueryBuilder.java:103-116`, `:60-63`). A test pins the exact text and arguments: `app/src/test/java/com/zegoggles/smssync/service/BackupQueryBuilderTest.java:38-40`.
- After each confirmed append the watermark is set to the batch's maximum date, unconditionally (`app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt:416-422`; the value is `ConversionResult.getMaxDate()` returned by `K9MailTransport.appendMessages`, `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java:197-206`). That maximum is the largest `X-smssync-date` header (`app/src/main/java/com/zegoggles/smssync/mail/ConversionResult.java:19-32`). The header is the provider's `date` column for SMS (`app/src/main/java/com/zegoggles/smssync/mail/HeaderGenerator.java:67`). Design D14 cites `MessageGenerator.java:109` for this; that line builds the sent date of the e-mail, also from `date`, and is not where the header is set (see Scope Concerns).
- Nothing records which provider rows were uploaded. The only reads and writes of the SMS watermark are listed in Affected Code.
- Consequence: if row A (date 1000) becomes visible after a backup has already moved the SMS watermark to 2000 or more, A is not selected by any later run. The only things that move the watermark back are "Reset" and removing the account (`app/src/main/java/com/zegoggles/smssync/preferences/DataTypePreferences.java:79-85`, called at `app/src/main/java/com/zegoggles/smssync/activity/fragments/MainSettings.java:82`, `:94`).
- If no other SMS is backed up between A's date and A becoming visible, A is still selected (its date is above the old watermark). The loss needs a later SMS to be backed up inside the delay. On a phone in normal use, an incoming message triggers a debounced backup within minutes (CNTR-MODERNIZATION-004 INV-4; content trigger over the SMS provider, `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt:233`), so this is the likely case, not an edge case.

**Depends on platform behaviour that nobody has measured** (see "Not known" below): that Android 17 really hands the app a row, three hours after receipt, that still carries its receipt-time `date`; and that the app is among the apps the withholding applies to.

**Which scenario applies to which build.**

| Build | Applies | Why |
|---|---|---|
| Today's build, `targetSdkVersion 35` (`app/build.gradle:25`) on an Android 17 device | The WebOTP-format message only | Section 4.6: all target levels, WebOTP-format messages, apps that are not the intended recipient. Standard OTP SMS is a target-37 rule (section 4.5) |
| Today's build on Android 16 or older | Not at all | The research lists no such rule below Android 17 |
| After `targetSdkVersion` is raised to 37 | The WebOTP-format message and standard SMS containing a one-time code | Section 4.5 plus 4.6 |

Which of the owner's devices runs Android 17 is not known (product-owner context: all run Android 11 or newer). The orchestrator ruled on 2026-11-21 that this fix comes first and `targetSdkVersion 37` follows it, if the measurement confirms the gap.

### Not known (from the research and from the code; not assumed)

1. Whether a released message keeps its original `date`. The research does not say. If the platform re-dates the row on release (for example to the release time), the existing query would pick it up and the bug would not occur.
2. Whether any trigger fires when the message is released: the `SMS_RECEIVED` broadcast is described as "withheld", not as redelivered; whether the SMS provider sends a change notification (which would fire the content trigger at `WorkManagerScheduler.kt:233`) is not stated. If nothing fires, the released message waits for the next unrelated trigger or the regular schedule.
3. Which apps are in "most apps targeting 37" (section 4.5). Whether this app is withheld from is unmeasured.
4. What makes a message "contain a one-time code" or be "WebOTP-format". The research gives neither rule. A message the platform treats this way may not look like a verification code to the owner.
5. Whether "the intended recipient" in section 4.6 can ever be this app. It is not a default SMS app (except during restore, `RestoreWorker.kt`, where the role is held, per the design) and has no WebOTP client.
6. Whether the emulator implements the delay the way a device does. The measurement below would show it; it is not assumed.

## Steps to Reproduce

Two reproductions. (a) needs no Android 17 and shows the logic defect. (b) shows whether Android 17 produces the situation. (a) is the regression test the fix must keep (AC-9). (b) decides how far the bug reaches (AC-12).

### (a) Platform-independent, unit level

No emulator, no account, no network. Run Gradle in a shell without `MSYS_NO_PATHCONV` set.

1. Use an SMS provider double that evaluates the selection it is given (`date > ? AND type <> ?`). The existing `FakeSmsContentProvider` (`app/src/test/java/com/zegoggles/smssync/service/RestoreWorkerCheckpointTest.kt:512-570`) is not enough as it stands: its query filtering is written for the restore's duplicate check, not for a date comparison (`:553-556`); a test-local provider, or an in-memory database behind the provider, is needed.
2. Insert row B (date 2000, inbox type). Run the backup query built by `BackupQueryBuilder` with the real `DataTypePreferences` (watermark initially -1), upload (a stub transport such as `StubMailTransport` in `BackupWorkerWatermarkTest.kt:355`), and let the watermark move to 2000.
3. Now insert row A (date 1000, inbox type), as if it had just become visible. Run the backup again.
4. Observe on today's code: the second run selects nothing, the stub transport receives no second message, the watermark stays at 2000. A is never uploaded. The selection actually sent is `date > 2000 AND type <> 3` (compare `BackupQueryBuilderTest.java:38-39`).
5. The expected outcome after the fix is in AC-1 and AC-9: A is uploaded by this second run, and B is not uploaded again.

### (b) Device reproduction on an API 37 emulator

Run by the orchestrator or by QA with foreground commands. Never by a subagent with a background shell. The emulator is a new one made from the installed API 37 image (design D16 and PO-2). **The existing emulator `Pixel_10_Pro` is never started or touched.** Only the owner enters the account password. The backup writes test messages to the owner's mail account (design PO-3), so the owner's permission for that is a precondition. The emulator clock is never changed: the three-hour wait is a real wait. Text evidence only (log lines, command output).

Corrections from the design audit, built into the steps: a backup needs a configured account to start, so without one no step below that needs a count can run; the count is read from the app's own log line, not inferred; the shell's content query answers only the date question, because it runs as the shell, not as this app, and may see rows the app does not; and a positive control proves the log line counts the app's own query result.

Preconditions: new API 37 emulator; debug build of HEAD (target 35) installed; account configured by the owner; auto backup off; **only SMS backup enabled** (MMS and call log off, so the count in the log line is an SMS count); the SMS watermark exists (one backup has already run).

1. Control that the log line counts what the query returns. Send two plain messages with `adb -s <serial> emu sms send 5550001 "plain one"` and `... "plain two"`. Run a manual backup. Expect the log line `BackupWorker: starting backup (2 messages)` (`BackupWorker.kt:313`, logged before any server contact; filter logcat by the app's tag). If it does not read 2, stop: the counting method is not valid.
2. At T0, send the message under test. For the today's-build case this is a WebOTP-format message (the format is defined in the platform's WebOTP documentation, which is not cited in the research; fetch it first and record the exact text sent; see Scope Concerns). For the target-37 case, a standard message such as `"Your verification code is 123456"`. Record the time and the device API level.
3. Query the provider from the shell: `adb -s <serial> shell content query --uri content://sms --projection _id:date:type:body`. Record whether the row is listed and its `date`. This is not evidence of what the app sees.
4. At T0 plus 1 minute, send a plain message B. Run a manual backup. **Positive control for the delay:** the log line must read `starting backup (1 messages)`, not 2. A count of 1 with the message under test present in the provider means the app's own query excluded it. If the count is 2, the app saw the message during the delay: stop, the gap is not confirmed for this build and message form.
5. At T0 plus 3 hours plus 10 minutes (the clock never changed), repeat the step 3 query. Record whether the row is listed and its `date`: its original T0 date, or a later one (answers "Not known" 1).
6. Run a manual backup. On today's code the log line `BackupWorker: nothing to backup` (`BackupWorker.kt:262`) means the message was skipped: the gap is confirmed. `starting backup (1 messages)` means the app picked it up (the platform re-dated it, or the watermark was not past it): the gap is not confirmed.
7. Throughout steps 4 to 6, capture logcat for `SmsBroadcastReceiver` and `BackupTriggerWorker` around T0 and around T0 plus 3 hours (answers "Not known" 2).
8. Repeat steps 2 to 7 once per case: WebOTP-format at target 35, standard OTP at target 37 (a build with only `targetSdkVersion` raised, kept out of the repository).
9. After the fix, repeat step 6 on the fixed build for each case. Expected: the first backup after release reports 1 message (the late one) and no message that was already uploaded appears again (AC-12).

Stop rule for the measurement: if step 1 fails, if step 4 shows 2, or if step 5 shows a re-dated row, record the finding and stop. A re-dated row or an app that is not filtered closes the platform reach of this bug for that case; the logic defect in (a) remains, and AC-9 stays as a regression test.

## Expected Behavior

Every SMS that exists in the provider and is visible to the app is backed up by the next backup after it becomes visible, whatever order the rows appeared in and whatever its `date` is relative to the watermark, for delays up to a stated bound. No message is uploaded twice. The first run, the upgrade from the current version, restore, skip and reset keep working.

## Actual Behavior

A row that becomes visible with a `date` at or below the watermark is never selected. No error, notification or log line says a message was missed. Nothing repairs it short of a reset, which re-uploads everything.

## Evidence

| Evidence | What it shows |
|---|---|
| `BackupQueryBuilder.java:103-116` | SMS selection `date > ? AND type <> ?`; argument is the stored watermark; no look-back |
| `BackupQueryBuilder.java:118-136`, `:138-147` | MMS and call-log queries have the same shape (`date > ?`) with their own watermarks |
| `BackupQueryBuilderTest.java:38-40` | Test pinning the exact SMS selection, arguments and sort. A fix that changes the selection changes this test on purpose |
| `BackupWorker.kt:416-423` | `appendBatchAndUpdateWatermark`: `setMaxSyncedDate(type, confirmedMaxDate)` with no comparison to the stored value |
| `K9MailTransport.java:197-206` | Returns `result.getMaxDate()` after the append |
| `ConversionResult.java:19-32` | Maximum of the `X-smssync-date` headers of the batch |
| `HeaderGenerator.java:67`, `:78`, `:86` | The date header per type: SMS `date`, MMS `date`, call log `date` |
| `DataTypePreferences.java:59-70` | Read and write of the watermark (MMS stored in seconds, returned in milliseconds) |
| `BackupWorker.kt:198-218` | SKIP path writes the newest visible date as the watermark without any upload |
| `BackupWorker.kt:257-261` | After a first run that finds nothing, the sentinel -1 is written to the SMS and MMS watermarks |
| `RestoreWorker.kt:464-468` | Restore moves the SMS watermark up to the date of each restored row |
| `MainSettings.java:82`, `:94`, `DataTypePreferences.java:79-85` | Account removal and settings reset clear all watermarks |
| `WorkManagerScheduler.kt:233` | Content trigger over the SMS provider; no trigger for MMS |
| `sdlc/artifacts/build/research/2026-11-21-latest-stable-versions.md` sections 4.5 and 4.6 | The platform rules quoted in the Description |
| DES-MODERNIZATION-014, D14 and rows 37-5 and 17-2 | The design finding and its measurement S5-I1 |
| `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/non-visual-specs/non-visual-specs.md:749` | The original app's semantics, as extracted: the watermark is "the incremental watermark update" after each batch; "only items before the kill are skipped next time". No look-back and no completeness rule is stated |

Neither reproduction has been run for this artifact. (a) follows from the code as read. (b) has not been run on any device.

## Acceptance Criteria

The criteria say what must be true, not how. The design names two candidates: a look-back overlap on the query with a local record of what was uploaded inside the window, or server-side de-duplication by a search on the `Message-ID` header. The Solution Architect advises; the story's plan decides. Whichever is chosen must meet all of these. Let W be the look-back bound (AC-2).

- [ ] AC-1: A message that becomes visible to the app with a `date` at or below the SMS watermark, no more than W after that `date`, is uploaded by the first backup that runs after it became visible. Shown by the unit-level reproduction (a): after the fix the second run uploads A.
- [ ] AC-2: W is a named, documented constant. It is at least the platform's three hours plus a stated margin. Recommended: 6 hours (three hours plus three hours of margin, which covers release-time jitter and clock differences, neither of which is measured). The reasoning is written in the fix: the first backup after a release always sees the message if W exceeds the delay, because the watermark cannot pass the release time before that backup runs, so W need not cover how late the backup itself runs. A test at the boundary (delay just under W is uploaded) is required. What happens beyond W is stated in the story: such a message is not caught, and that limit is recorded in the artifact the fix updates (see decision point DP-1).
- [ ] AC-3: No message is uploaded to the server twice because of the fix. IMAP APPEND is not idempotent, so this is proven, not assumed: a test runs at least three consecutive backups over a provider that holds the same rows and receives a late row between runs, with the stub mail transport recording every appended message by its `Message-ID` (and by `X-smssync-id`), and asserts no value occurs twice across all runs. It also covers a row selected by the look-back that was already uploaded in an earlier run inside the window.
- [ ] AC-4: The watermark never moves backwards as a result of a look-back batch. Today `appendBatchAndUpdateWatermark` writes the batch's maximum with no comparison (`BackupWorker.kt:422`); a batch made only of older rows would lower it. A test uploads only a late, older row and asserts the stored watermark is unchanged (or higher), not set to the older date.
- [ ] AC-5: The other writers of the SMS watermark keep working and cause neither duplicates nor omissions, each with a test: (i) restore moves the watermark up to restored rows' dates (`RestoreWorker.kt:464-468`); after a restore, a later backup does not upload the restored messages again and does not skip a late row inside the window; (ii) SKIP (`BackupWorker.kt:198-218`) still ends with no upload and a current watermark; the behaviour for a message that was hidden at skip time and is released inside the window is stated and tested (decision point DP-3); (iii) clearing the sync data (`DataTypePreferences.java:79-85`) also clears whatever record the fix keeps, so that a reset then behaves as a first run; (iv) the first-run sentinel (`BackupWorker.kt:257-261`) is unchanged.
- [ ] AC-6: First run and upgrade behave correctly. First run (no watermark): behaviour identical to today. Upgrade from the current version (a watermark exists, no record yet): the first backup after the upgrade uploads no message that the earlier version already uploaded, and loses none that it would have uploaded. A test starts from a stored watermark with an empty record and rows both below and above it inside W, and asserts exactly which rows are uploaded; the story states how the empty record is treated and why.
- [ ] AC-7: Cost is bounded and stated. The number of rows the query returns beyond today's is bounded by the rows inside W; look-back rows count against the maximum items per sync (`BulkFetcher.java:23-33`) the same way as any row, and a test with a limit smaller than the window shows nothing is starved or skipped. Any local record has a stated maximum size and is pruned: entries older than the watermark minus W are removed, and a test shows the pruning. No unbounded growth with the age of the install.
- [ ] AC-8: MMS and call log: unchanged unless the decision in DP-2 says otherwise. The queries, watermarks and tests for them (`BackupQueryBuilderTest.java:56-87`) pass without edits if DP-2 is "unchanged". If DP-2 extends the fix to either, it is a separate numbered criterion added by a recorded decision, not done silently.
- [ ] AC-9: The unit-level reproduction (a) exists as a regression test that fails on HEAD `b9fd172f` and passes after the fix, and is not `@Ignore`d. The existing test of the selection text (`BackupQueryBuilderTest.java:38-40`, and `:52-53`) is updated deliberately in the same change, not left failing or deleted, if the selection changes.
- [ ] AC-10: Existing behaviour listed under "Existing Behavior to Preserve" still holds, each with an existing or new test. In particular BUG-010: the watermark and any record change only after a confirmed append, so a failed append leaves both unchanged (`BackupWorkerWatermarkTest.kt` stays green without edits).
- [ ] AC-11: No dependency on the mail library that REQ-MODERNIZATION-019 Part B replaces, or the dependency is stated and costed. A look-back plus local record needs nothing from the library beyond the port `MailTransport.appendMessages` (`CNTR-MODERNIZATION-007`). A server-side search on the `Message-ID` header would need a library call that does not exist on the port, would be rewritten at the migration, and its use must be listed in the story with the migration impact. Whether Gmail removes duplicates on APPEND by `Message-ID` is not established anywhere in the artifacts and is not relied on.
- [ ] AC-12: Device check on API 37, using the corrected procedure in Steps to Reproduce (b), on the fixed build for the cases that the measurement showed to be real: the late message is reported as 1 message in the first backup after release; the plain message B is not reported again; no duplicate appears in the log counts of the two following manual backups. If the measurement showed no reach (re-dated row, or app not filtered), this criterion records that finding with the evidence, and AC-9 alone carries the regression.
- [ ] AC-13: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` exits 0 with 0 failures, the skipped count not higher than before the change, no per-package JaCoCo line floor lowered. The authoritative test count is taken with `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"` before and after, and any difference is explained.
- [ ] AC-14: The documents that state backup semantics are updated or confirmed unchanged, each named in the story: README.md passages on "Skip" and "Reset" (`README.md:82`, `:101`, `:272-274`); the requirement or change record chosen under Impact Analysis, layer 1.

### Integration Criteria (if fix modifies component boundaries)

- [ ] IC-1: Every reader and writer of the SMS watermark is listed in the fix and handled: `BackupQueryBuilder.java:112`, `BackupWorker.kt:207`, `:259`, `:422`, `RestoreWorker.kt:466-467`, `DataTypePreferences.java:59-85`, display only at `AdvancedSettings.java:230` and `StatusPreference.java:370`. The display of "last sync" does not change meaning (it shows the newest backed-up date).
- [ ] IC-2: If `MailTransport`, `ConversionResult`, `BackupQueryBuilder` or `DataTypePreferences` signatures change, every caller and test double is updated in the same change (compile-checked). A change to the `MailTransport` port is a contract change under CNTR-MODERNIZATION-007 and needs its change record first.

## Impact Analysis

Searches run: `max.?synced`, `getMaxSyncedDate`, `setMaxSyncedDate`, `watermark`, `clearLastSyncData`, `date > ?`, `X-smssync-date`, `Message-ID`, `OTP` over the layers below. Counts are from the searches run for this artifact, not exhaustive greps of every layer.

| # | Layer | Affected? | Files and lines | Action needed |
|---|-------|-----------|-----------------|---------------|
| 1 | Requirements (`sdlc/artifacts/requirements`) | A requirement or change record is needed | No requirement states that every message visible to the app is backed up. The nearest statements are constraints: `modernization/REQ-MODERNIZATION-017-platform-level-current-stable-api.md:176-177` ("backup and restore results are identical for the user before and after"), and the same sentence in REQ-MODERNIZATION-014 (`:228-229`), REQ-MODERNIZATION-015 (`:201-202`) and EPIC-006 (`:126-127`). Search for `max synced`, `watermark` in the requirements folder: 0 matches. | Decision DP-4: either a new requirement that states backup completeness (what "backed up" means for late-visible rows), or a change record against REQ-017 recording the completeness rule. The behaviour at HEAD is not "identical before and after" under Android 17 if the gap is real, which is the design's classification (D14, "A requirement gap") |
| 2 | Design and contracts (`sdlc/artifacts/design`) | Yes, likely | `contracts/CNTR-MODERNIZATION-004-backup-scheduler.md`: states INV-4 (debounce) and the content trigger; it does not describe the watermark (0 matches for `watermark`, `max synced`). `contracts/CNTR-MODERNIZATION-007-mail-transport.md:385`: `// ... maxSyncedDate bookkeeping unchanged ...`, which sits in an example, not a rule. `modernization/DES-MODERNIZATION-012-...md:651`: SKIP semantic. `modernization/DES-MODERNIZATION-014-...md` D14: cites `MessageGenerator.java:109` for the header and says restore is unaffected; both need the corrections in Scope Concerns | No contract currently owns the watermark. If the fix adds a record or changes the `MailTransport` port, a contract statement (or change record) is needed. The SA decides where the watermark and look-back rule are written. DES-014 D14 is revised by the orchestrator today |
| 3 | Stories, sprints, change records (`sdlc/artifacts/build`, `sdlc/artifacts/stories`) | Yes | The platform-level story that raises `targetSdkVersion` to 37 (design slice S5-I4) waits for this fix, per the orchestrator's ruling. REQ-MODERNIZATION-019 Part B (mail library replacement) replaces the transport underneath. BUG-010 (U-042) set the confirmed-append rule this fix must keep | Sequence: this fix before the targetSdk 37 story. No story of the migration is changed unless the fix uses a library call (AC-11). The new story's ID is not assigned here |
| 4 | Application code (`app/src/main`) | Yes (root) | `service/BackupQueryBuilder.java:103-116` (SMS selection); `service/BackupWorker.kt:198-218`, `:257-261`, `:416-423`; `preferences/DataTypePreferences.java:59-85`; `service/RestoreWorker.kt:464-468`; `service/BackupItemsFetcher.java:37-40`; `service/BulkFetcher.java:23-33` (limit shared across types); `mail/HeaderGenerator.java:50`, `:67` and `:108-127` (the `Message-ID` is an MD5 of date, address plus "v2", and status); `activity/fragments/MainSettings.java:82`, `:94` | The change itself (AC-1 to AC-7) |
| 5 | Vendored library (`k9mail-vendored`) | No | The fix needs no library change if it uses a look-back plus a local record. Only the server-search option would (AC-11) | None |
| 6 | Tests (`app/src/test`) | Yes | `service/BackupQueryBuilderTest.java:38-40`, `:52-53` (pins today's selection); `service/BackupWorkerWatermarkTest.kt` (BUG-010 cases, `StubMailTransport` at `:355`); `service/BackupWorkerIntegrationTest.kt:230-262`, `:305-313`; `service/RestoreWorkerIntegrationTest.kt:448`, `:496`; `preferences/PreferencesTest.java:31-41`; `mail/transport/K9MailTransportTest.java:453-474`; `service/BackupItemsFetcherTest.java`, `service/BulkFetcherTest.java` | New regression and duplicate-proof tests (AC-3, AC-9); deliberate update of the selection test; the others stay green |
| 7 | Documentation (`README.md`, `sdlc/artifacts/engagement`) | Possibly | `README.md:82`, `:101` (Skip), `:272-274` (Reset). `sdlc/artifacts/engagement`: 1 match for the search terms (`code-location.md`), not a statement of semantics | Check that README still states what Skip and Reset do after the change (AC-14) |

**Cascade analysis** (four layers affected: requirements, design and contracts, code, tests; two more for references: build records and documentation).

- Root layer: application code. The query and watermark logic is the defect. Upstream, no requirement or contract states a completeness rule, so none is violated; the missing piece is a requirement (DP-4) and a contract home for the watermark, not a correction of something wrong.
- Propagation: code to the selection test (yes, deliberate update); to the requirement and change record (yes, DP-4); to the platform-level story (yes, it waits); to the migration's transport work (no, if the fix stays behind the `MailTransport` port); to MMS and call log (no change unless DP-2).
- Fix order: (1) decision points DP-1 to DP-4 answered; (2) unit-level regression test written first and shown failing on HEAD; (3) the fix; (4) duplicate-proof and watermark-writer tests; (5) requirement or change record, contract statement, README check; (6) device check on API 37; (7) the targetSdk 37 story proceeds.

**Interaction with other work.**

- The platform-level story that raises `targetSdkVersion` to 37 (DES-014 S5-I4, REQ-MODERNIZATION-017) waits for this fix if the measurement confirms the gap. The measurement (S5-I1) may run early with today's build; the fix may be planned in parallel with it, but the story's AC-12 needs its result.
- REQ-MODERNIZATION-019 Part B replaces the mail transport. The fix must not rely on library behaviour (AC-11). If the server-side option is chosen, its search code is rewritten at the migration.
- Today's build already has the WebOTP-format exposure on Android 17 devices (design row 17-2), so this fix is not only a precondition for target 37.
- The deterministic `Message-ID` (MD5 of date, address plus "v2", and status; `HeaderGenerator.java:50`, `:108-127`) means a re-upload of the same row carries the same `Message-ID`. That makes a server-side search possible; it does not make APPEND idempotent, and whether two different messages can share date, address and status is not examined here.

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->

Advisory, from the Solution Architect's intake pass. Code facts below were read at HEAD `b9fd172f`; anything marked "not measured" or "not read" is not established. The story plan may revise or replace this section.

**Recommendation in one line.** A look-back query over the SMS window, a local record of what was uploaded inside the window (a client-side Idempotent Receiver, *Enterprise Integration Patterns*), keyed by the message's content identity rather than the provider `_id`, stored in the same preferences file as the watermark and written in the same commit, and a watermark that only moves up in the backup path. No change to the `MailTransport` port and nothing from the mail library.

#### 1. Options

**(a) Look-back plus a local record (recommended).**

- *Query shape.* Keep the forward query exactly as it is (`date > watermark`, with its `LIMIT`; `BackupQueryBuilder.java:103-116`) and add a second, separate SMS query for the window: `date > watermark - W AND date <= watermark AND type <> 3`, with the same contact-group clause (`groupSelection`, `:149-163`; leaving it out would back up rows the user excluded). The window query has no SQL `LIMIT`. Its rows are filtered against the record in memory, and only the rows that remain count against the per-run item budget (`BulkFetcher.java:23-33`). This order matters. If the record were applied after a SQL `LIMIT`, a window full of rows already uploaded would fill the limit with rows that are then thrown away, and the late row would never be reached (the starvation case AC-7 asks about). Keeping the forward query unchanged also means `BackupQueryBuilderTest.java:38-40` and `:52-53` can stay as they are, and the look-back is new code beside it. Both the normal path (`BackupWorker.kt:251`) and the auth-retry path (`:457`) fetch through `BulkFetcher`, so the window belongs in the fetch layer and not in only one of the two worker paths.
- *Key.* The provider `_id` is not sound. A restore inserts rows with new `_id`s (`RestoreWorker.kt:461-463`). Android's own backup copies the default preferences file, which holds the watermark, to a new device (`AndroidManifest.xml:95-96`; `backup_descriptor.xml` excludes only the credentials and pinned-certificate files), and that device's provider numbers its rows differently. Whether the provider reuses an `_id` after the newest row is deleted was not read. The derived `Message-ID` is closer, but it is not sound as it stands either. For SMS it is an MD5 of the `date`, the raw `address` plus "v2", and the message **type** (`HeaderGenerator.java:50`, `:108-127`; the third argument is `messageType` from `MessageGenerator.java:95`, `:118`, not a delivery status). The query selects every type except drafts, so outbox (4), failed (5) and queued (6) rows are uploaded. A row uploaded as outbox that becomes sent (2) inside the window would get a new `Message-ID` and be uploaded a second time. Today the watermark hides it, so the duplicate would come from the fix, which AC-3 forbids. Suggested key: a short digest of `date`, `address` and `body`, without type and without `_id`. Two rows share the key only if they have the same millisecond, the same address and the same text, which in practice is the same message. If a restore or the provider rewrites the address or the body, the key does not match and the result is a duplicate, not a loss, which DP-5 accepts. Keep the entry's `date` beside the digest so that entries can be pruned.
- *Where it lives.* In the default preferences file, next to `max_synced_date`, owned by `DataTypePreferences`, as one serialised string value (not a `StringSet`, whose returned set must not be modified in place). Write it in the same `commit()` as the watermark. One file and one commit means the record and the watermark cannot disagree after a crash, so the crash window is the one that exists today (append confirmed, process dies before the commit, batch sent again) and is no wider. A small SQLite database or a file would add a second store that is not written atomically with the watermark, and a second thing for reset, restore and Android backup to keep in step. No benefit at this size.
- *Size and pruning.* At every write, drop entries dated at or below `watermark - W`. The number of entries is bounded by the SMS rows in W (6 hours). Set a stated hard cap as well (for example 1,000 entries, a few tens of KB). If the cap is reached, drop the oldest entries and log it. A dropped entry can only cause a duplicate, never a loss (DP-5).
- *Lifecycle.* App-data clear: the record and the watermark go together, so the next run is a first run. Reset and account removal: `clearLastSyncData` (`DataTypePreferences.java:79-85`) must remove the record and its version marker too. Restore: each inserted SMS is added to the record when the watermark is raised (`RestoreWorker.kt:464-468`); otherwise the look-back sends restored messages back to the server. Android's backup to a new device: the record travels with the watermark because both are in the same file, and the content key still matches on the new device. Skip: see section 3.

**(b) Look-back plus a search on the server.** Today the port has no search. `getMessages(folder, max, flagged, since)` (`MailTransport.java:102-105`) lists messages by flag and date only. A header search needs a new port operation, which CNTR-MODERNIZATION-007's own policy calls a breaking change for the engine (section "Breaking change policy"), so it needs a change record first. It would be built on the vendored k-9 library and rewritten at REQ-MODERNIZATION-019 Part B (CR-005 renames the packages underneath; CR-006 changes the XOAuth2 error type the search would also have to handle). The cost is at least one network round trip per window candidate, or a batched OR search whose behaviour on Gmail is not established in any artifact. A failed search forces a choice between possibly sending a duplicate and stalling the backup. The key would be `Message-ID`, which includes the type and so has the outbox-to-sent duplicate described above. `X-smssync-id`, named in DES-014 D14, is the provider `_id` and has the restore problem. Its one real advantage is that it survives app-data clear. That does not justify a network dependency inside every backup. Not recommended.

**(c) Track the newest provider `_id` as well as the date.** If the platform hides an existing row (the research says "provider queries are filtered"), the late row keeps the `_id` it got on arrival. Message B, which arrived after it, has a higher one, so an `_id` high-water mark misses it in exactly the way the date does. It helps only if the platform inserts the row again on release with a new `_id`, which has not been measured. Restore inserts rows with high new `_id`s and would push restored messages back to the server unless restore also moves the mark. Not recommended. The measurement should still record the `_id` before and after release, because a new `_id` would mean the platform moves rows, which matters for the key.

**(d) A watermark held W behind the newest message, without a record.** Every run would send the last W again, so it needs the record anyway. It is (a) with a worse cost profile.

#### 2. The watermark-lowering defect (AC-4)

The SMS backup path should store the larger of the stored watermark and the batch's confirmed maximum. Put this in the backup append path (`appendBatchAndUpdateWatermark`, `BackupWorker.kt:416-422`, or a new `DataTypePreferences` method that only that path calls), not in `setMaxSyncedDate` itself. Skip (`:207`) and the first-run sentinel (`:259-260`) set the value outright by design. There is a trap for MMS: `getMaxSyncedDate(MMS)` returns milliseconds while the stored value and the MMS `ConversionResult` maximum are in seconds (`DataTypePreferences.java:59-66`; `HeaderGenerator.java:78`). A general `max(get(type), confirmed)` would write milliseconds into the seconds slot and push the MMS watermark far into the future, so every later MMS would be skipped. Under DP-2 (SMS only) the guard is needed only for SMS. If it is applied to every type, compare stored raw values, never the `get` result. The port's Javadoc ("Callers MUST use this return value ... as the watermark", `MailTransport.java:71-77`) still holds in spirit, since no other date is used, but its wording should be corrected in the same change. The contract text does not mention the watermark, so no contract change follows from this.

#### 3. Skip, first run and upgrade

- **Skip (DP-3 default, a hidden message released later is backed up).** Skip should set the watermark as today and also *seed* the record with every SMS it can see inside the window below the new watermark, without uploading them. A message hidden at Skip time is not in the seed, so it is uploaded when it is released, which is the DP-3 default. This **differs from DES-014 D14**, which says "a SKIP sets a floor below which the look-back does not reach". A floor gives DP-3 option (b): the hidden message is lost. The design text should follow the DP-3 ruling.
- **First run.** With the -1 sentinel the window starts below every row, so the run behaves as today. The sentinel write (`BackupWorker.kt:257-261`) is unchanged.
- **Upgrade (watermark present, no record).** Keep a small version marker for the record. If the marker is absent and a watermark exists, the first run after the upgrade seeds the record from the visible rows in `(watermark - W, watermark]` and uploads only the forward rows. Without seeding, every message in the last W would be sent again (AC-6 forbids that). With seeding, a message that was already released and lost before the upgrade stays lost. That is today's outcome and not a regression, and AC-6 ("loses none that it would have uploaded") is met. Recovering it is Scope Concern 6, not this fix.

#### 4. What must be measured on the API 37 emulator before the choice is final

The recommendation does not depend on the measurement to be safe. If the platform gives the row a new date when it releases it, the window is never needed, and the record still guarantees no duplicate. The measurement decides the following:

1. Whether the released row keeps its `date` (whether the bug reaches the device at all).
2. Whether its `_id` changes on release (option c; whether the platform moves rows).
3. Whether `address` and `body` are byte-identical before and after release (the key's inputs; the shell's view only, as the bug's procedure warns).
4. Whether a trigger fires at release. If none does, the message waits for the next unrelated trigger or the regular schedule. W still covers it (AC-2's reasoning holds because the watermark cannot pass the release time before a backup runs), but the user-visible delay is longer.
5. The observed delay, to confirm that W = 6 hours has margin.

The digest key and storage choice can be fixed before the measurement. Item 3 can only make the key cause a duplicate, not a loss.

#### 5. DP-4: where the completeness rule lives

I agree with the default (b), a new requirement, with two refinements:

- Keep the requirement to behaviour. Every SMS that the app can see is backed up by the first backup after it becomes visible, for a delay of up to W after its date. The fix introduces no duplicate. The watermark never moves down in a backup. Reset returns the app to a first run. The key, the storage and the pruning belong in the story plan, not in the requirement.
- Link it under EPIC-MODERNIZATION-006, and have the targetSdk 37 story cite it. REQ-017's "identical before and after" constraint stays as written, with no change record. A change record against REQ-017 (option a) would put a lasting product promise inside a platform-level requirement that is closed once level 37 lands.

**Contract: none needed, if option (a) is chosen.** The watermark and the record are shared state inside the app (backup worker, restore worker, settings reset), not a boundary between components. CNTR-MODERNIZATION-004 is the scheduler, CNTR-MODERNIZATION-006 is the sync-state and event channel (its scope is the Otto replacement, so it is the wrong home), and CNTR-MODERNIZATION-007's port does not change. Option (b) would need a change record against CNTR-MODERNIZATION-007 first. The mechanism should be written down in the story plan, and DES-014 D14 should point to it.

**Artifact to raise:** the new requirement, before this story's plan gate, so that the plan can cite it. Also the D14 correction (the Skip floor against DP-3, and `_id` against the content key), which falls within today's D14 revision.

#### 6. Layer cascade

- Requirements: the new completeness requirement (section 5).
- Contracts: none under (a). The `MailTransport` Javadoc wording is code documentation, not the contract.
- Design: D14 corrections (section 5).
- Code: `BackupQueryBuilder` (new window query), `BackupItemsFetcher` and `BulkFetcher` (filter, then budget), `BackupWorker.kt` (guard against a lower watermark; Skip seeding; record write in the same commit), `DataTypePreferences` (record, version marker, reset), `RestoreWorker.kt` (record restored rows). The display in `AdvancedSettings.java:230` and `StatusPreference.java:370` does not change meaning.
- Tests: an SMS provider double that evaluates the selection (the bug's reproduction a); a duplicate check over several runs; Skip, restore, reset, upgrade and Android-backup cases; an MMS check that the guard does not touch MMS units. Existing tests stay unedited where the forward query is unchanged.
- Documentation: README Skip (`:82`, `:101`, `:246`) gains one sentence: messages that the phone hides at Skip time and releases later are backed up. Reset (`:272-274`) is unchanged in meaning; confirm it.

#### 7. Story split and ordering

- **Two pieces.** (1) The fix story, which can be built and fully unit-tested now without any emulator, because the logic defect does not depend on the platform. (2) The device measurement (DES-014 S5-I1) plus this bug's AC-12 check, as its own small step run by QA or the orchestrator once the owner has created the API 37 emulator and entered the password. Only AC-12, the confirmation of W, and the targetSdk 37 story wait for (2). If (2) finds that the row gets a new date, the fix stays as harmless protection against late rows from any source, and AC-9 remains its regression test.
- **Ordering on `BackupWorker.kt`.** The first design story (minSdk 24) touches the build, not this file's logic; either order works. BUG-022 changes the loop's cancellation check (`:330-331`) and imports. It is small and should land first, so that this fix rebases onto a stable loop. The runtime-library story (WorkManager, coroutines) changes worker APIs in the same file and should come after this fix, or run strictly one after the other. The frontmatter's `parallel_eligible: true` does not suit this bug against those two stories; the Librarian owns that field. CR-006, carried out with the mail migration, removes the status-400 retry path (`:432-481`). With the window in the fetch layer, the fix does not care which lands first.

#### 8. Planning constraints and risks

- A duplicate on the server cannot be undone by the app. The duplicate proof (AC-3) is the main gate, and it should include the outbox-to-sent change, a restore followed by a backup, and Skip followed by a release.
- The fix does not recover messages that were already skipped (Scope Concern 6). The upgrade seeding deliberately keeps that outcome.
- Upgrade, with an existing watermark and no record: seeding is what keeps it free of duplicates.
- Future-dated rows (a sender-set or skewed `date`) can push the watermark ahead of the clock and shrink the effective window. The margin in W covers small skew; large skew was not examined.
- MMS seconds against milliseconds in the watermark guard (section 2).

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/BackupQueryBuilder.java` (`:103-116`) | SMS rows with `date > watermark`, no look-back | Per the chosen design (AC-1, AC-2, AC-7) |
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` (`:198-218`, `:257-261`, `:416-423`) | Skip writes newest visible date; sentinel on empty first run; watermark set to the batch maximum unconditionally | Keep BUG-010 gating; do not lower the watermark (AC-4); skip behaviour per DP-3 |
| `app/src/main/java/com/zegoggles/smssync/preferences/DataTypePreferences.java` (`:59-85`) | Reads, writes and clears the per-type watermark | Clear any new record with the watermark (AC-5 iii) |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt` (`:464-468`) | Moves the SMS watermark to each restored row's date if higher | Must not cause duplicates or omissions (AC-5 i) |
| `app/src/main/java/com/zegoggles/smssync/mail/HeaderGenerator.java` (`:50`, `:67`, `:108-127`) | Sets `X-smssync-date` and a deterministic `Message-ID` | Read only, unless the design needs a stable identity |
| `app/src/main/java/com/zegoggles/smssync/service/BulkFetcher.java` (`:23-33`) | Shares the item limit across types | Verify the look-back rows do not starve other types (AC-7) |
| `app/src/test/java/com/zegoggles/smssync/service/BackupQueryBuilderTest.java` (`:38-40`, `:52-53`) | Pins today's selection | Updated on purpose with the fix (AC-9) |
| `app/src/test/java/com/zegoggles/smssync/service/` (new tests) | None for late rows or duplicates | Regression test (a), duplicate proof, writer tests (AC-3, AC-5, AC-6, AC-9) |
| `README.md` (`:82`, `:101`, `:272-274`) | Describes Skip and Reset | Confirm or update (AC-14) |

## Existing Behavior to Preserve

- BUG-010: the watermark advances only after a confirmed append. A failed folder open or append leaves it unchanged (`BackupWorker.kt:403-423`; `BackupWorkerWatermarkTest.kt`).
- Per-batch watermark writes as a durable checkpoint (`BackupWorker.kt:298-300`).
- The order of messages in a batch (sorted by `date`) and the `LIMIT` semantics (`BackupQueryBuilder.java:60-63`).
- First run: nothing in the provider and no watermark writes the -1 sentinel for SMS and MMS (`BackupWorker.kt:257-261`); `isFirstBackup()` stays true until a watermark exists (`Preferences.java:220-227`), and the auto-backup receiver stays off during first backup (`SmsBroadcastReceiver.java:66-73`).
- SKIP does not connect to the server (`BackupWorker.kt:198-218`; README `:82`).
- Reset and account removal return the app to a first-run state (`DataTypePreferences.java:79-85`).
- Restore's own duplicate guard (date, address, type; `RestoreWorker.kt:596-602`) and its watermark update.
- The incoming-message debounce and content trigger (CNTR-MODERNIZATION-004 INV-4).
- MMS (seconds in the database, milliseconds in the preference) and call-log handling (`DataTypePreferences.java:59-66`; `BackupQueryBuilder.java:118-147`).
- The existing crash window: if the process dies after a confirmed append and before the watermark is committed, the batch is uploaded again next run. This is not introduced by the fix and is not required to be closed by it; the fix must not widen it (AC-3 includes a run killed between append and record write, with the same outcome as today).

## Verification Steps

For QA, per acceptance criterion. Run Gradle in a shell without `MSYS_NO_PATHCONV`. Device steps are run by the orchestrator or QA, never a subagent with a background shell; `Pixel_10_Pro` is never touched; only the owner enters the password.

1. AC-1, AC-9: run the unit-level reproduction (a) on HEAD `b9fd172f` and confirm it fails with A not uploaded; run it on the fix and confirm A is uploaded exactly once. Confirm the test is not `@Ignore`d.
2. AC-2: read the constant and its documented reasoning; run the boundary test (delay just under W uploaded). Confirm what the story says happens beyond W.
3. AC-3: run the three-run duplicate test; open the recorded `Message-ID` list; confirm no value twice, including a row selected twice by the window and the kill-between-append-and-record case.
4. AC-4: run the test that uploads only an older row; confirm the stored watermark is not lowered.
5. AC-5: run the restore, skip, reset and first-run tests; for skip, confirm the behaviour matches the DP-3 ruling.
6. AC-6: run the upgrade test (stored watermark, empty record) and compare the uploaded rows with the expected list written in the test.
7. AC-7: run the limit-smaller-than-window and pruning tests; read the stated maximum record size.
8. AC-8: confirm `BackupQueryBuilderTest.java:56-87` and the MMS and call-log code are unchanged (`git diff`), or that a recorded DP-2 decision exists for any change.
9. AC-10: run `BackupWorkerWatermarkTest` and `BackupWorkerIntegrationTest` unchanged and green.
10. AC-11: `git diff` shows no change under `k9mail-vendored` and no new call into the mail library from the app other than the existing port; or the story's stated dependency is present.
11. AC-12: on a new API 37 emulator, follow Steps to Reproduce (b) on the fixed build for each case the measurement confirmed. Record the log lines (control `(2 messages)`, positive control `(1 messages)`, post-release `(1 messages)`) and the two following manual backups' counts.
12. AC-13: run `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`; confirm exit 0, 0 failures, skipped not higher; compare the authoritative test count.
13. AC-14: open README and the chosen requirement or change record and confirm the statements.

## Root Cause Analysis
<!-- Filled during planning phase -->

Intake note (not the planning-phase analysis). Root layer: application code. The watermark is a single high-water mark per type. A high-water mark assumes rows appear in date order, so that nothing becomes visible later with an older date. That assumption held when every row the provider holds is visible to every reader at once. Android 17's one-time-code withholding breaks it for the app's reader (if the platform's behaviour is as the research says; not measured). The same shape would be broken today, with no platform change, by any other source of late rows with old dates (see Scope Concerns, observations 3 to 5). The original application's semantics, as extracted, are the same incremental watermark (`non-visual-specs.md:749`).

## Technical Context

- **Severity: medium, kept as given, with reasons.** What is lost is a text message, silently and permanently missing from the backup, with no error: the app's one job is to back up messages, so this is a data-completeness defect, which weighs towards high. What holds it at medium: (1) its reach is limited to messages the platform treats as containing a one-time code (or WebOTP-format ones), a narrow class of mostly short-lived content; (2) it needs an Android 17 device, and which of the owner's devices run it is not known; (3) it needs a later message backed up inside the three hours, which is common, so this does not lower it; (4) the app is private, for the owner's use alone, so no one else is exposed; (5) the platform behaviour that causes the late row is unmeasured, and one possible outcome (the row is re-dated on release) makes today's code safe; (6) a reset recovers everything, at the cost of re-uploading all messages. It rises to high if the measurement confirms the gap for the WebOTP-format case on today's build and the owner has an Android 17 device, or if the withheld class turns out to include ordinary messages. It falls to low if the measurement shows re-dating.
- **How earlier bugs were rated.** BUG-018 (restore reports failure after success) medium/medium; BUG-019 (TLS 1.0 and 1.1 enabled) medium/medium; BUG-020 (pinned factory, no hardening of its own, opt-in path, no exploit shown) low/medium. This one is silent data loss on the main path, so not below BUG-018 and BUG-019; it is not rated above them because of reach and the unmeasured trigger.
- **Priority: high.** The orchestrator ruled on 2026-11-21 that this fix comes first and `targetSdkVersion 37` follows it, if the measurement confirms the gap. The platform-level story waits for it, and so does everything that depends on that story. Today's build is also exposed to the WebOTP-format case on Android 17 devices.
- **Complexity: high.** It changes backup semantics (a window), adds state that must survive upgrade, reset, restore and skip, and needs a duplicate proof against a non-idempotent server operation, plus a device check with a three-hour wait.
- **Why the look-back bound is the delay and not the backup interval.** The watermark cannot move past a withheld message's release time before a backup runs after the release, because messages carry receipt-time dates and none is later than the clock at backup time (device clock skew and future-dated rows aside, which is why a margin is required). So the first backup after release has a watermark no later than the release time, which is the message's date plus the delay. A window of the delay plus a margin therefore reaches the message however late that first backup runs. Which of the sources of "later than the clock" exist (sent messages with a sender-set date, clock changes) was not examined.
- **Product-owner context.** The app is private, for the owner's own use. All the owner's devices run Android 11 or newer; which of them run Android 17 is not known. The mail server is Gmail. Decision records 3 to 12 in the appendix "Product owner decision" of `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` set the direction of the migration; none of them concerns the watermark. The orchestrator's ruling of 2026-11-21 (fix first, then targetSdk 37, if the measurement confirms the gap) is to be recorded there.
- **Device facts.** The emulator that is never touched is `Pixel_10_Pro`. A manual backup without a configured account never starts (design audit). Only the owner handles the account password.

### Decision points for the product owner

Only real decisions. Each has a recommended default that applies unless the owner rules otherwise.

| # | Decision | Options | Recommended default |
|---|----------|---------|---------------------|
| DP-1 | Is a bounded look-back enough, or must every late message, however late, be caught? | (a) Bounded window W of at least the platform delay plus margin; messages released after more than W are not caught. (b) Every row ever visible must be uploaded: a complete record of uploaded rows, or a server-side check per row, at larger cost | (a), W = 6 hours (three hours plus three of margin). The only known cause of late rows is the platform's three-hour delay; the cost of (b) grows with the age of the install. Revisit if the platform delay changes or the measurement shows a longer delay |
| DP-2 | Do MMS and call log get the same treatment now? | (a) No, SMS only. (b) Yes, one or both | (a). The OTP pages name "standard SMS messages" (research 4.5, 4.6); MMS and call log are not named. If the measurement or the other observations (Scope Concerns 3 to 5) show a real late-row case for them, add it as a new criterion by recorded decision |
| DP-3 | A message that existed at the time of a "Skip" but was hidden from the app (Android 17), and is released inside the window afterwards: back it up or leave it skipped? | (a) Back it up. (b) Leave it skipped, which needs a record taken at skip time of what could not be seen, and the app cannot see hidden rows | (a). Skip means "do not send what I can see now"; the app cannot know about a hidden row, and a backed-up extra message costs less than a lost one. Record the decision in the README text |
| DP-4 | Where does the completeness rule live? | (a) A change record against REQ-MODERNIZATION-017, adding the rule. (b) A new requirement stating backup completeness for rows that become visible late. (c) Neither: the fix's acceptance criteria carry it | (b), with REQ-017's "identical before and after" constraint left as written. A new requirement states what the product promises (every message the app can see is backed up); that outlives the targetSdk 37 story. If the owner wants less ceremony, (a) |
| DP-5 | If a trade-off is unavoidable, are duplicates worse than omissions? | (a) Omission is worse: prefer a duplicate to a lost message. (b) Duplicate is worse | (a). A duplicate in the mail folder is visible and removable; a lost message is not noticed. AC-3 still requires that the fix itself introduce no duplicate; this default governs only a case where the design cannot guarantee both |

## Supporting Documentation

- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-toolchain-runtime-libraries-platform-level-design.md` (decision D14, rows 37-5 and 17-2, measurement S5-I1, options for PO-1)
- `sdlc/artifacts/build/research/2026-11-21-latest-stable-versions.md` (sections 4.5 and 4.6; sources https://developer.android.com/about/versions/17/behavior-changes-17 and https://developer.android.com/about/versions/17/behavior-changes-all)
- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-017-platform-level-current-stable-api.md` (Constraints, `:176-177`)
- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-019-replace-vendored-k9-with-thunderbird-mail-libraries.md` (Part B, transport replaced)
- `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` (INV-4, content trigger and debounce)
- `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-007-mail-transport.md` (the `MailTransport` port)
- `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` (appendix "Product owner decision", records 3 to 12)
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/non-visual-specs/non-visual-specs.md` (`:506`, `:716`, `:749`: sentinel, first-run write and per-batch watermark as extracted)
- `sdlc/artifacts/stories/BUG-019-factory-enables-tls10-and-tls11.md`, `sdlc/artifacts/stories/BUG-020-pinned-factory-no-tls-hardening.md` (shape and rating precedents)

## Implementation Notes
<!-- Added by agents during build -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes

### Scope Concerns

None of these changes the approved decisions; each is raised, not acted on.

1. **Design D14 has code citations to correct** (the design is being revised today). The date header is set in `mail/HeaderGenerator.java:67`, not `MessageGenerator.java:109`; the latter is the e-mail's sent date. D14 also says restore is "unaffected": restore does write the SMS watermark (`RestoreWorker.kt:464-468`), which is why AC-5 (i) exists. D14's option (b) names the `X-smssync-id` header (`Headers.java:8`) for a server search; the stable value that repeats on re-upload is `Message-ID` (`HeaderGenerator.java:50`), while `X-smssync-id` is the provider's `_id`, whose reuse after deletion or restore was not examined.
2. **The watermark can be lowered by a look-back batch** (`BackupWorker.kt:422` writes the batch maximum without comparing). Harmless today because the query cannot return rows at or below the watermark; it becomes a real defect under any look-back. Covered by AC-4.
3. **Other late-row sources with the same shape, not measured and not in this bug's scope** (separate observations; any would be fixed by the same window only if DP-2 extends it): (i) SMS restored or imported into the provider by another app with an old date; (ii) messages synced into the provider from a second device or profile with old dates; (iii) incoming MMS: its `date` is in seconds and the platform's choice of date for a message that downloads late is not read here (for the OTP rule MMS is not named); (iv) call-log rows inserted after a later row (the call-log trigger covers only the SMS and call-log URIs, `WorkManagerScheduler.kt:233-237`, and MMS has no content trigger). None is shown to occur.
4. **The WebOTP-format message text is not in the research.** The research says "WebOTP-format" without giving the format; the measurement for today's build needs it. First step: read the platform's WebOTP documentation and record the exact text; stop rule: if the format cannot be established, run the standard-OTP case only and record that the all-targets case is unmeasured.
5. **A message in the withheld class may not look like a code to the owner.** What the platform treats as containing a one-time code is not stated in the research (Not known, 4), so the loss could include messages the owner values. This is a reason for the severity to be rechecked after the measurement.
6. **Nothing in the app reports a skipped late message.** A fix that closes the gap does not tell the owner whether past messages were already lost. A one-off check of the provider against the server for messages already missed is outside this bug; the owner may want it if the measurement confirms the gap (not a decision point now, since the app is private and the loss window is the period on an Android 17 device).
7. **`minSdkVersion` is still 21 at HEAD** (`app/build.gradle:24`) though the requirements are amended to 24 (CR-010, applied to the requirements on 2026-11-21 per the recent commit log); the fix must compile and run at whatever level is in force when it is built. No code difference here, noted so the story does not assume 24.
8. **Two earlier artifacts treat the OTP effect as a platform behaviour to disposition, not a bug**: rows 37-5 and 17-2 of design D14. When this bug is filed, those rows should cite BUG-021 so the disposition ("A-F, fix first") has a number. This is the orchestrator's revision to make.
