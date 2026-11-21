---
status: implemented
severity: high
artifact_type: change-request
requirements:
  - REQ-MODERNIZATION-005
  - REQ-MODERNIZATION-001
design_docs:
  - DES-MODERNIZATION-005
  - DES-MODERNIZATION-008
  - DES-MODERNIZATION-014
  - DES-MODERNIZATION-001
related_stories:
  - BUG-023
  - BUG-025
  - BUG-026
  - BUG-027
  - BUG-029
  - U-069
  - U-073
  - U-075
tags:
  - change-record
  - modernization
  - cntr-004
  - backup-scheduler
  - regular-backup
  - boot
  - content-trigger
  - sms-receiver
  - product-owner-decision
id: CR-017
title: 'CNTR-MODERNIZATION-004 v4 and its design: a regular schedule of "Never" schedules nothing, the boot path keeps the regular backup, the content-trigger example matches the code, and the SMS-received receiver is no longer an incoming path'
type: ''
domain: change-records
summary: 'CNTR-MODERNIZATION-004 (backup scheduler, v3) and DES-MODERNIZATION-005 need four corrections that the scheduler work of 2026-11-21 produced: (1) the regular schedule value "Never" must schedule nothing (BUG-026: today it registers a 15-minute backup); (2) the boot path must not replace the periodic regular backup with a one-time one under the same unique name (BUG-027, confirmed on the Android 16 emulator: after a restart no regular backup is scheduled); (3) the content-trigger example shows a network constraint and a backoff on the trigger registration, which the code does not have and WorkManager does not apply there (found while story U-069 was planned; orchestrator ruling SC-1 of that story); (4) the SMS-received receiver, which is never delivered the system broadcast, is removed by the product owner''s decision of 2026-11-21 (decision appendix, Record 20, ruling 56). One record, one contract version step, by rulings 55 and 56 and the close-out note of story U-069.'
updated_at: '2026-11-21T17:50:24.094Z'
---

# CR-017: Scheduler contract v4: "Never", the boot path, the content-trigger example and the SMS-received receiver

## Metadata

| Field | Value |
|-------|-------|
| **CR Number** | CR-017 |
| **Type** | DCR (Design Correction), with dated notes on two approved requirements (edits R-1, R-2) that change no criterion |
| **Severity** | High |
| **Status** | Applied 2026-11-21: the edits of the record (C-0 to C-21, D-0 to D-19, H-0 to H-2, E-0 to E-4, P-0, P-1, R-1a to R-1c, R-2a, R-2b), of Amendment 1 (C-22 to C-30, D-20 to D-27, E-5, E-6, R-1d) and of Amendment 2 (E-7, E-8). The close-out edits X-1, X-2 and X-3 are applied when their stories close. Frontmatter `status` is the record of state |
| **Reported By** | Requirements Analyst, on BUG-026, BUG-027, BUG-023 and BUG-025, story U-069 (SC-1, Review Findings) and the orchestrator's rulings 55 and 56 (decision appendix, Record 20) |
| **Date Reported** | 2026-11-21 |
| **Source** | Sprint work: the code review and plan review that raised BUG-023, BUG-025, BUG-026 and BUG-027; the Android 16 emulator observation of 2026-11-21 (Record 21, ruling 62); the product owner's decision on the SMS-received receiver (Record 20) |

This record is **applied inline**, right after it is approved, like CR-015. It amends approved artifacts (one contract,
four designs, two requirements). It is **not a story to schedule**: the code changes are made later by two stories
that do not have IDs yet, the story that fixes BUG-026 and BUG-027 (ruling 55) and the story that removes the
SMS-received receiver (ruling 56). This record edits no code and no test.

All file and line references in this record are at commit `fd70325a` (read with `git show` and `git grep`, never the
working tree under `app/`, which has a developer's uncommitted edits). The artifact files this record edits have no
uncommitted change (`git status` at the start of this work lists none of them).

---

## Finding

### Description

The scheduler contract, CNTR-MODERNIZATION-004 (v3, `status: approved`), and its design, DES-MODERNIZATION-005, are
wrong or incomplete on four facts that the scheduler work of 2026-11-21 established.

1. **"Never" (BUG-026).** The "Regular schedule" setting offers "Never" with the value −1. The contract says the adapter
   is "a mechanical translation of the current `BackupJobs.java` behavior" (CNTR-004 `:32`) and maps `scheduleRegular`
   to the legacy method (`:108`). The legacy rule was "schedule only if `inSeconds > 0`" (`BackupJobs.java:132` at
   `d095fe02^`, read for this record). The contract never says so, and the code turns −1 into a 15-minute periodic
   backup (`WorkManagerScheduler.kt:180`). DES-005's mapping row reads the legacy `inSeconds <= 0` case as "run now"
   (DES-005 `:280`), which was true only for the forced immediate job (`BackupJobs.java:99-100`).
2. **The boot path (BUG-027).** `scheduleBootup()` enqueues a one-time `BackupWorker` under the unique name `REGULAR`
   with `ExistingWorkPolicy.REPLACE` (`WorkManagerScheduler.kt:273-281`). `REPLACE` deletes the periodic regular backup
   registered under the same name (`WorkManagerScheduler.kt:192-197`), and nothing registers it again; the next
   `scheduleRegular()` fails silently on the one-time entry (BUG-027, Description items 3 to 5). The orchestrator observed
   this twice on the Android 16 emulator on 2026-11-21: after the restart the scheduler held the content trigger and one
   one-time job, no periodic job (`sdlc/artifacts/build/research/2026-11-21-background-backup-and-boot-observation.md`,
   Run 1 and "What it shows" item 3). The contract's `scheduleBootup()` text and Validation Rule 4 ("else schedule
   regular with the 60s boot delay", CNTR-004 `:69-71`, `:276`) do not say the boot job may replace the periodic one;
   DES-005 specifies the one-time shape (`:121`) and says the adapter "mirrors" the legacy branches (`:437`), which it
   does not.
3. **The content-trigger example (U-069, BUG-023 Suggested Approach §2).** The example block (CNTR-004 `:369-390`)
   puts `networkType = CONNECTED`, `backoff = BackoffConfig(EXPONENTIAL, 30s, 300s)` and `existingPolicy = REPLACE` on
   the trigger registration. The code registers the trigger with content-URI triggers only, no backoff, and
   `ExistingPeriodicWorkPolicy.UPDATE` (`WorkManagerScheduler.kt:218-250`). U-069 ruled that none is added (U-069 story,
   "Out of scope", line 66; SC-1, line 226 and 290; Review Findings, line 278). The example also still describes the
   SMS-received receiver as a second path (`:387-389`). DES-005 and DES-008 list only two `@HiltWorker` workers, but
   `BackupTriggerWorker` became the third in U-069 (`worker/BackupTriggerWorker.kt:51-57`, commit `d7a69974`).
4. **The SMS-received receiver (Record 20, ruling 56).** `SmsBroadcastReceiver` is declared `android:exported="false"`
   (`AndroidManifest.xml:132-139`) and `RECEIVE_SMS` (`:17`) is never requested at run time (BUG-023, "Severity, and the
   SMS-received receiver"). It is not delivered `SMS_RECEIVED`. The product owner decided on 2026-11-21: "Remove it
   (Recommended)" (decision appendix, `:2468`). The contract v3 still describes it as an unchanged path that "runs beside"
   the content trigger (CNTR-004 `:243`, `:245`, `:387-389`); DES-005 does the same (`:321`, `:379`), and REQ-005's CR-015
   note says "The `SMS_RECEIVED` path is not removed" (REQ-005 `:87`).

The contract version step, the ruling on whether any part is breaking, and the order against the two later stories
must be settled before those stories are written; both must cite the contract they are built against.

### Source of Truth

- **Bugs (status `ready`):** BUG-026 (`sdlc/artifacts/stories/BUG-026-regular-schedule-never-still-schedules-a-backup.md`,
  Expected Behavior, AC-1 to AC-6, Suggested Approach); BUG-027
  (`sdlc/artifacts/stories/BUG-027-regular-backup-removed-by-the-boot-backup.md`, Evidence (the dated device observation),
  Expected Behavior, AC-1 to AC-11, Suggested Approach, PO-1 and PO-2); BUG-023
  (`sdlc/artifacts/stories/BUG-023-content-trigger-backup-skips-scheduling-checks.md`, Suggested Approach §2 and
  "Severity, and the SMS-received receiver"); BUG-025
  (`sdlc/artifacts/stories/BUG-025-incoming-sms-backup-delayed-by-trigger-period-and-job-quota.md`, owner's decision note
  at `:191`).
- **Story U-069** (`sdlc/artifacts/build/sprints/sprint-024/U-069-content-trigger-backup-goes-through-the-scheduler.md`):
  "Out of scope" `:66`, AC-12 `:103`, Implementation Notes `:226`, `:236`, Review Findings `:278`, SC-1 `:290`, SC-7 `:296`.
- **Decisions** (decision appendix,
  `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`): Record 20 (`:2459-2481`),
  the product owner's answers on BUG-025 (`:2467`, "Only correct the text") and on the receiver (`:2468`, "Remove it
  (Recommended)"); rulings 54 (`:2474`), 55 (`:2475`), 56 (`:2476`); the open BUG-027 questions (`:2479`: "the fix uses the
  safe answer, no, for both until decided"). Record 21 (`:2483-2508`), rulings 60 (`:2496`, BUG-029 and its fix story
  U-075) and 62 (`:2498`).
- **Device observation:** `sdlc/artifacts/build/research/2026-11-21-background-backup-and-boot-observation.md`.
- **Code at `fd70325a`** (read, not edited): cited per fact in Evidence.
- **Legacy code** at `d095fe02^` (the parent of the commit that deleted it): `service/BackupJobs.java:74-100`,
  `:127-145`, `:160-172`, read for this record.

### Evidence

#### E-1. The code at `fd70325a`

| Fact | What the code does | Lines |
|------|--------------------|-------|
| "Never" | `scheduleRegular()` checks the encryption-degraded state and auto backup, then `intervalSecs = maxOf(preferences.regularTimeoutSecs.toLong(), PERIODIC_MIN_INTERVAL_SECS)`; −1 becomes 900 s. Periodic `BackupWorker`, `networkConstraints(REGULAR)`, `EXPONENTIAL` 30 s, tag `REGULAR`, `enqueueUniquePeriodicWork(REGULAR, UPDATE)`; the returned `Operation` is ignored | `scheduler/WorkManagerScheduler.kt:170-202` (`:180`, `:192-197`); constant `:112-116`; KDoc `:160-169` |
| "Never", contrast | `scheduleIncoming()` returns `null` for `timeoutSecs <= 0` | `WorkManagerScheduler.kt:138-143` |
| Only caller of `scheduleRegular()` | `App.rescheduleJobs()`: `cancelAll()`, then `scheduleRegular()` if auto backup is on, then `scheduleContentTrigger()` if `getIncomingTimeoutSecs() > 0` | `App.java:373-385` |
| Boot path | `scheduleBootup()`: degraded gate (`:263-266`), auto backup off → `cancelAll()` and `null` (`:267-271`), else a **one-time** `BackupWorker` with the regular network constraint, `EXPONENTIAL` 30 s, `setInitialDelay(60 s)`, tag `REGULAR`, `enqueueUniqueWork(REGULAR, ExistingWorkPolicy.REPLACE, …)` | `WorkManagerScheduler.kt:262-286` (`:273-281`); KDoc `:252-261`; class comment `:40-42`, `:67-69` |
| Boot caller | `BootReceiver.bootup()` calls `scheduleBootup()` on `ACTION_BOOT_COMPLETED` | `receiver/BootReceiver.java:24`, `:31-34`; manifest `:141-147`, `:62` |
| Content trigger | Encryption-degraded gate (`:219-222`); `Constraints.Builder().addContentUriTrigger(SMS_PROVIDER, true)` plus `CALLLOG_PROVIDER` when call-log backup is enabled **and** `isCallLogBackupAfterCallEnabled()` (`:224-230`); `PeriodicWorkRequest` of `BackupTriggerWorker` every `PERIODIC_MIN_INTERVAL_SECS` = 15 min (`:232-236`); `.setConstraints(...)` with the URI triggers only and `.addTag(CONTENT_TRIGGER)` (`:237-239`), **no** `setBackoffCriteria`, **no** network type; `enqueueUniquePeriodicWork(CONTENT_TRIGGER, UPDATE, …)` (`:241-246`) | `WorkManagerScheduler.kt:204-250` |
| Trigger worker | `@HiltWorker`, `@AssistedInject constructor(@Assisted context, @Assisted params, scheduler: BackupScheduler, preferences: Preferences) : Worker`; `doWork()` logs the trigger line, calls `scheduler.scheduleIncoming()` and returns `Result.success()` | `worker/BackupTriggerWorker.kt:51-71` |
| Trigger worker tests | 15 `@Test` | `app/src/test/java/com/zegoggles/smssync/worker/BackupTriggerWorkerTest.kt` |
| SMS receiver | Listens to `android.provider.Telephony.SMS_RECEIVED`; `shouldSchedule` requires auto backup on, login set and the first backup done; then `getScheduler(context).scheduleIncoming()`; publishes no event | `receiver/SmsBroadcastReceiver.java:44-111` (`:51`, `:58-63`, `:66-84`) |
| SMS receiver, manifest | `enabled="true"`, `exported="false"`, `permission="android.permission.BROADCAST_SMS"`, filter `SMS_RECEIVED` | `AndroidManifest.xml:132-139` |
| SMS receiver, `App` | `setBroadcastReceiversEnabled(isAutoBackupEnabled())` at start and on every settings event; it toggles `SmsBroadcastReceiver` and `BootReceiver` | `App.java:46` (import), `:255-258`, `:289`, `:358-361` (`:359`) |
| SMS receiver, tests | `SmsBroadcastReceiverTest`: 4 `@Test` (`shouldScheduleIncomingBackupAfterIncomingMessage` `:45`, `shouldNotScheduleIfAutoBackupIsDisabled` `:51`, `shouldNotScheduleIfLoginInformationIsNotSet` `:58`, `shouldNotScheduleIfFirstBackupHasNotBeenRun` `:65`) | `app/src/test/java/com/zegoggles/smssync/receiver/SmsBroadcastReceiverTest.java` |
| SMS receiver, other references | Port javadoc names it among "All call sites" | `scheduler/BackupScheduler.java:32-33` |
| Other references to the receiver outside `app/src` and `sdlc` | None (`git grep` of the whole tree outside those two) | — |
| Coverage floor of the receiver package | JaCoCo LINE ≥ 0.50; the comment records "U-060: 50.0% (29/58), zero headroom" | `app/build.gradle:521-534` |
| Test count | 771 `@Test` (the authoritative command run at `fd70325a`, which is `HEAD`) | — |

**`RECEIVE_SMS`: who else needs it (checked, as ruling 56 asks).** The permission is declared at
`AndroidManifest.xml:17`. No Java or Kotlin file under `app/src/main` names it (`git grep RECEIVE_SMS`); the only other
occurrence is a test that uses the string as data (`service/exception/ExceptionCoverageTest.java:31`). But the
default-SMS-app components need it:

- `.compat.SmsReceiver` is declared for `android.provider.Telephony.SMS_DELIVER`, `exported="true"`, permission
  `BROADCAST_SMS` (`AndroidManifest.xml:166-173`). While the app holds the SMS role, it stores each delivered message in
  the provider (`compat/SmsReceiver.java:33-42`, `:55-91`).
- The app takes the SMS role for a restore: `SmsDefaultRoleHelper.requestDefaultSmsPackageChange()` enables
  `SmsReceiver` and requests `ROLE_SMS` (`activity/SmsDefaultRoleHelper.kt:105-110`), and gives it back afterwards
  (`:128-131`).
- The app promises that messages arriving during a restore are delivered: "Any incoming SMS messages during restore will
  get delivered as usual but won't generate notifications." (`app/src/main/res/values/strings.xml:346-347`); the README
  explains the role hand-over (`README.md:352-360`).
- The platform delivers `SMS_DELIVER` to the default SMS app only if that app holds `RECEIVE_SMS` (the broadcast is sent
  with `RECEIVE_SMS` as the required permission). **This platform rule is recalled, not fetched or read in platform
  source for this record.** The SMS role grants `RECEIVE_SMS` to its holder only if the app declares it.

**Finding: `RECEIVE_SMS` is still needed** (by the `SMS_DELIVER` receiver during a restore). The receiver-removal story
keeps `AndroidManifest.xml:17`. Removing it is out of scope of ruling 56, which says "if nothing else needs it".
`RECEIVE_MMS` (`:18`) is not touched either.

#### E-2. The legacy code at `d095fe02^` (read for this record)

- `BackupJobs.scheduleRegular()` = `schedule(getRegularTimeoutSecs(), REGULAR, false)` (`:78-80`); `schedule()` creates a
  job only `if (force || (isAutoBackupEnabled() && inSeconds > 0))` (`:132`), else logs and returns `null` (`:141-144`).
- `scheduleBootup()` (`:86-97`): auto backup off → `cancelAll()`; old scheduler → `schedule(BOOT_BACKUP_DELAY, REGULAR,
  false)` (`:91-92`); otherwise **nothing** ("everything else should be persistent by GCM", `:93-95`).
- `scheduleImmediate()` = `schedule(-1, BROADCAST_INTENT, true)` (`:99-100`): the only forced job, so `Trigger.NOW`
  (`:162`, `inSeconds <= 0 ? NOW : …`) was reachable only from it.
- `createJob` sets `Lifetime` `FOREVER` for recurring, `UNTIL_NEXT_BOOT` otherwise (`:164`).
- `createContentUriTriggerJob()` = `createBuilder(INCOMING)` with the content-URI trigger, recurring, `FOREVER`
  (`:168-172`): **the legacy trigger job did carry the `INCOMING` network constraint and retry strategy**, because
  `createBuilder` attaches them. The WorkManager code does not (E-1). BUG-023 Suggested Approach §2 argues the net effect
  is the same (the backup still waits for a permitted network, through the `INCOMING` work). This record relies on that
  argument for the example (subject 3) and says so in the ruling.

#### E-3. Search of every statement of each fact (the lesson of CR-015, Amendment 1)

Scope: `sdlc/artifacts/design` (contracts and designs), `sdlc/artifacts/requirements`, `README.md`, at `fd70325a`, with
`git grep -n` (case-insensitive and fixed-string unless marked). Change records, stories and sprint records under
`sdlc/artifacts/build` and `sdlc/artifacts/stories` are history and were not edited; they were read where cited.
"Edited (id)" names the edit in "Proposed Change". `BUGS.md`, `DEVELOPMENT.md` and `metadata/` were also searched for
`RECEIVE_SMS`, `SMS_RECEIVED`, `boot`, `regular schedule` and `incoming`: no hit about the scheduler.

**Fact 1 and 2 ("Never", the boot path, `REGULAR`):**

| Pattern | Hits | Per hit |
|---------|------|---------|
| `scheduleRegular` | 5 | CNTR-004 `:62` edited (C-2); `:108` (surface provenance) true as it stands. DES-005 `:88` edited (D-1); `:100` true; `:435` edited (D-12) |
| `scheduleBootup` | 6 | CNTR-004 `:69` edited (C-3); `:108` true; `:276` edited (C-17). DES-005 `:90` edited (D-2); `:101` true; `:437` edited (D-13) |
| `bootup` | 9 | The six above, plus CNTR-004 `:171` edited (C-7); DES-005 `:121` edited (D-3); DES-012 `:185` (a "before" diagram of 2026-06) historical record, left |
| `boot` (other forms) | CNTR-004 7, DES-005 11 | CNTR-004 `:43` (consumer list) edited for the receiver (C-1), "boot receiver" true; `:286` ("re-executed by WorkManager after process death/reboot") true. DES-005 `:109-111` (verified-caller note), `:167` (restore), `:191` (ADR-005-A), `:248` (topology), `:427` (contracts table) true as they stand; `:287` edited (D-6); `:457` edited (D-17) |
| `BOOT_COMPLETED` | 2 | DES-001 `:82`, REQ-001 `:95` (the receiver's export value) true as they stand |
| `BootReceiver` | 9 | DES-001 `:37`, `:82`, `:88`, `:244`; REQ-001 `:34`, `:95`, `:106`, `:185`: export declarations, true. DES-012 `:183` historical record, left |
| `REGULAR` (case-sensitive) | 15 | CNTR-004 8: `:120`, `:133-134` (enum, `isRecurring`) true; `:144` (unique-name note) kept, new bullet after it (C-4); `:258` (v2 changelog) historical record, left; `:276` edited (C-17); `:280` (Rule 8) true; `:362` (example `uniqueName`) true, its neighbours edited (C-18). DES-005 4: `:435`, `:437` edited (D-12, D-13); `:440-441` (cancel rows) true; `:455` edited (D-16). DES-012 `:794`, REQ-013 `:212`, `:264` ("scheduled REGULAR completes", "via the REGULAR periodic request") true as they stand, and true after the fix |
| `Never` (case-sensitive) | 15 | None is about the regular schedule: DES-005 `:152` ("Never present in release builds"), DES-014 7 (selection rules, device rules), CNTR-001/002/007/008, DES-002, REQ-002: unrelated, true |
| `-1` / `−1` | 791 lines for `-1` (identifier noise: `DES-001`, `REQ-…-1x`, dates). Narrowed pattern `(^\|[ (=])-1\b\|−1` over CNTR-004, DES-005, REQ-005, README: 0 | No document states the value −1 of the setting |
| `inSeconds <= 0` | 1 | DES-005 `:280` edited (D-5) |
| `UPDATE` (case-sensitive) | 22 | CNTR-004 `:64` edited (C-2), `:187` edited (C-9), `:240` edited (C-10), `:361` edited (C-18). DES-005 `:278` ("REPLACE/UPDATE") true as it stands; `:435` edited (D-12). DES-001 15, REQ-001 1: SDK "update" text, unrelated |
| `reboot` (case-sensitive) | 4 | README `:238` ("Run at startup - used to enable automatic backups after reboot"): true after the fix story, hand-over (README row below); CNTR-004 `:286` true; DES-005 `:167` (restore) true, `:287` edited (D-6) |

**Fact 3 (content-trigger example, `@HiltWorker` lists):**

| Pattern | Hits | Per hit |
|---------|------|---------|
| `CONTENT_TRIGGER` | 6 | CNTR-004 `:140`, `:145` true; `:377` edited (C-19). DES-005 `:379` true; `:455` edited (D-16, the `CONTENT_TRIGGER_TAG` part stays). DES-014 `:838` (the S1 test table) true |
| `CONTENT_TRIGGER_TAG` | 4 | Among the above |
| `backoff` | 51 | CNTR-004 22: all state INV-3 for backup work and are true, except `:165` (the `backoff` field, edited C-6) and `:375` (the example, edited C-19). DES-005 19: INV-3 and the mapping, true. DES-003 2, EPIC-001 1, EPIC-002 1, REQ-003 3, REQ-005 `:55`, REQ-012 2: not about the trigger, true |
| `networkType` / `NONE` | CNTR-004 `:162`, `:176-179`, `:310`, `:338`, `:358`, `:374`; DES-005 `:449` | `:178-179` edited (C-8; "BROADCAST_INTENT only" is also false since v2, because `MANUAL`/`SKIP` have `NONE` by Rule 8); `:374` edited (C-19); DES-005 `:449` edited (D-14); the rest true |
| `HiltWorker` | 32 | CNTR-004 `:400` (DES-008 owns the wiring) true. DES-005 `:68`, `:361` true; `:261-262` (components) edited by adding a row (D-4); `:357` edited (D-8). DES-008 15: `:113` (the worker list) edited (H-1); `:59`, `:204-216`, `:259-282`, `:305-316`, `:351`, `:372`, `:465` describe the mechanism, true. DES-007 `:313`, DES-012 `:114`, `:175`, `:589`, `:938` (statements about the two backup/restore workers, from completed designs) historical record, left. DES-014 `:1102`, EPIC-003 `:71`, REQ-008 `:48`, `:61`, REQ-014 `:48`, `:266`: the mechanism, true |
| `BackupTriggerWorker` | 2 | DES-014 `:1416`, `:2247` edited for the receiver (E-1, E-2) |

**Fact 4 (the SMS-received receiver):**

| Pattern | Hits | Per hit |
|---------|------|---------|
| `SmsBroadcastReceiver` | 22 | CNTR-004 `:243` edited (C-11), `:245` edited (C-12). CNTR-006 `:44` ("Receivers (`App.onCreate`, `BackupBroadcastReceiver`, `SmsBroadcastReceiver`) publish one-shot events"): **false today** for the SMS receiver (it publishes no event, `SmsBroadcastReceiver.java:44-111`), outside this record's subjects, left (Scope Concern 8). DES-001 `:37`, `:81`, `:88`, `:244`: historical design of U-001, left, dated note added (P-1). DES-005 `:379` edited (D-10). DES-007 `:325`, DES-012 `:183`: historical record, left. DES-014 `:1287` ("listens to `SMS_RECEIVED`") true as it stands until the removal, covered by the dated note (E-4); `:1332` edited (E-3); `:1416` edited (E-1); `:1823-1824`, `:1829` (the argument CR-015 ruled on) historical record, left, covered by E-4; `:2169` (files read for D14) historical record, left; `:2247` edited (E-2). REQ-001 `:34`, `:94`, `:106`, `:185`: dated note added (R-2) |
| `SMS_RECEIVED` | 9 | CNTR-004 `:245` edited (C-12). DES-001 `:81` (P-1). DES-014 `:1287` (E-4 note), `:1332` edited (E-3), `:1822`, `:1837` historical record, left. REQ-001 `:94` (R-2). REQ-005 `:87` edited (R-1b). REQ-020 `:108` (a quote of the platform page) true as it stands |
| `SMS-received` | 6 | CNTR-004 `:243` (C-11), `:245` (C-12), `:255` (v3 changelog) historical record, left, `:387` edited (C-19). DES-005 `:321` edited (D-7), `:379` edited (D-10) |
| `receiver` (CNTR-004, DES-005 only) | 10, 15 | CNTR-004 `:43` edited (C-1); `:46`, `:48` (consumer and caller notes of v1) historical, left; `:294-300`, `:396` (the `BACKUP` receiver) true; `:408` (open verification items) left. DES-005 `:110-111`, `:122`, `:191`, `:248`, `:329-337`, `:427-428`, `:472`, `:491`, `:508`: the `BACKUP` receiver, the boot receiver, WorkManager's own receiver, true; `:379` edited (D-10) |
| `RECEIVE_SMS` | 0 | No document states the permission (see E-1 for the code) |
| `broadcast path` | 4 | The CNTR-004 lines above |

**Notes to carry (BUG-025 and BUG-029), searched with `foreground`, `notification`, `incoming backup`, `content trigger`,
`scheduleIncoming`:**

| Place | Statement | Disposition |
|-------|-----------|-------------|
| CNTR-004 `:60`, INV-4 `:243`, Rule 5 `:277` | the incoming backup is enqueued with delay = `getIncomingTimeoutSecs()` | True as it stands: states the requested initial delay, not how soon the system runs it. No latency bound is promised anywhere in the contract, DES-005 or REQ-005. Under ruling 54 no edit is required; an optional sentence is in Scope Concern 9 |
| DES-005 `:87`, `:320`, `:434` | the same delay | True as it stands |
| REQ-020 `:94` (row 12) | the content trigger starts a backup through the INV-4 debounce | True as it stands |
| `README.md:298-302` | "any incoming SMS will trigger a backup after 3 minutes … show up in Gmail shortly after arrival" | **False today** (BUG-025, two mechanisms). Hand-over to BUG-025's text-only story (ruling 54), not edited here |
| `app/src/main/res/values/strings.xml:170` | "When to backup incoming SMS (after arrival)" | Same hand-over (BUG-025 AC-5) |
| `README.md:304-305` | "Regular schedule … 2 hours here means that the device will try to backup all messages every 2 hours" | **False today after a restart** (BUG-027). True after the fix story; no README edit needed then. Hand-over: the fix story's QA reads it |
| `README.md:238` | "Run at startup - used to enable automatic backups after reboot" | True after the fix story (the boot path keeps or registers the regular backup) |
| CNTR-004 `:280` (Rule 8) | "the manual notification/foreground semantics" | True as it stands; U-075 keeps the foreground for `MANUAL`/`SKIP` |
| DES-005 `:54`, `:129-131`; REQ-005 `:41` | WorkManager "owns … foreground promotion"; "where a user-visible notification is required it uses `setForeground`" | True as they stand, also after U-075 (automatic backups no longer ask for the foreground; manual runs and restores do) |
| REQ-016 `:49-53` | "`BackupWorker.kt` and `RestoreWorker.kt` call `setForeground()`" | True after U-075 (still called, for user-started runs); a description for the library check, not a promise |
| REQ-013 `:160-172`, DES-008 `:185-188`, DES-012 `:652`, `:697-750` | foreground promotion of the manual path | True; history of the manual-dispatch design |

No document states that automatic backups run in the foreground or show a notification, so U-075 makes no contract or
design text false. No edit is proposed for BUG-025 or BUG-029.

> **Corrected 2026-11-21 by Amendment 2.** The sentence above is wrong for one place: DES-014 row 16-1 (`:1282` in the
> working tree) says "long work runs as a dataSync foreground service", which U-075 makes false for automatic backups
> (U-075 contract verification, B-1). This search had no pattern for "long work". Edit E-7 corrects the cell; no other
> present-tense sentence of that kind exists (Amendment 2, "The foreground search").

#### E-4. The applied-text check (added by Amendment 1)

An independent Design Auditor checked the applied edits on 2026-11-21:
`sdlc/artifacts/build/change-records/evidence/CR-017-applied-text-check.md`. First verdict: **FAIL** (two must-fix
texts the record had not edited, M-1 and M-2, and eight should-fix points, S-1 to S-8; every ordered edit matched word
for word and nothing else changed). The search of E-3 lacked the patterns `VERIFIED universal` and the call-log condition.
Amendment 1 answers every finding. A delta check by the Design Auditor follows once Amendment 1 is applied; its verdict
is recorded here then.

Delta check of Amendment 1 (same report, end): **"Verdict after Amendment 1: PASS"**, with DS-1 and an optional wording
point open; the S-4 departure (interval tests as guards) was accepted by the auditor and the orchestrator. Amendment 2
answers DS-1 and the wording point and adds E-7 (U-075 contract verification, B-1); its own quick check is pending.

---

## Triage

### Classification

- **Type:** DCR. The contract and the design are the root of three of the four facts (they omit the "Never" rule, they
  specify or tolerate the one-time boot job, and the example is wrong), and they must follow a product-owner decision on
  the fourth. The two requirement notes (R-1, R-2) change no criterion and no value.
- **Root:** Design and contracts. For "Never" and the boot path the defect is also in the code (fixed by the story that
  fixes BUG-026 and BUG-027); the design misread the legacy code first (DES-005 `:280`, `:121`, `:437`).
- **Layers affected:** design and contracts (5 files: CNTR-004, DES-005, DES-008, DES-014, DES-001), requirements
  (2 files, notes), code and tests (later, two stories), documentation (README: hand-overs, no edit here).
- **Severity justification:** High, by the create-cr rubric CR-015 used (critical: blocks active work or 5+ files; high:
  affects multiple stories, does not block current work; medium: isolated). It affects three stories (the fix story,
  the receiver-removal story, and U-073, whose "tests unmodified" baseline must include the fix story's tests) and is a
  precondition for writing two of them. It does not block current work: U-075 and the sprint-025 stories touch none of
  the edited statements. It touches seven files, which reads "critical" on the letter of the rubric; it is not, because
  four of the seven get only a dated note or a one-line edit, and no current story waits on it. The orchestrator may
  read it as Critical; the steps are the same.

---

## The ruling this record makes

### The question

The contract's own rule is under **Versioning → "Breaking change policy"** (CNTR-004 `:264-267`): breaking for
**consumers** is "any change to a method signature, the `BackupType` enum values, or the `SchedulerState` variant set";
breaking for the **behavioral contract** is "any change to the four invariants (different `ExistingPolicy`, a network
constraint on `BROADCAST_INTENT`, a backoff value other than `EXPONENTIAL`/30s/300s, or losing the content-trigger or its
debounce on any supported level)", which "MUST be gated by the INV-1..4 tests". Which of the four parts is a break?

None of the four changes a port signature, a `BackupType` value or a `SchedulerState` variant (`scheduleRegular()` and
`scheduleBootup()` already return a nullable `ScheduledJob`, `BackupScheduler.java:55-56`, `:67-68`, `:91-92`). So
**nothing is breaking for consumers.** The question is the behavioural contract, part by part.

### Part 1: "Never" schedules nothing

*For a break:* observable behaviour changes. A user with "Never" today gets a periodic backup every 15 minutes
(`WorkManagerScheduler.kt:180`); after the fix, none.

*Against:* (1) The contract says the adapter is a mechanical translation of `BackupJobs.java` (`:32`) and maps
`scheduleRegular` to `BackupJobs.java:78` (`:108`); the legacy method scheduled nothing for `inSeconds <= 0`
(`BackupJobs.java:132`, E-2). The clamp was never in the contract; the code drifted from the contract's own claim.
(2) No invariant is touched: the positive-value request keeps `REGULAR`, `UPDATE`, the network rule and the backoff
(BUG-026 AC-4). (3) `scheduleIncoming()` already declines `<= 0` (`WorkManagerScheduler.kt:138-143`), so the port
already has the same rule for its other delayed operation.

**Recommended ruling: not a break.** A correction that states the legacy rule the contract always claimed to carry.

### Part 2: the boot path keeps the regular backup

*For a break:* (1) The boot path's policy changes from `ExistingWorkPolicy.REPLACE` on a one-off to
`ExistingPeriodicWorkPolicy.UPDATE` on the periodic work, and the policy names "different `ExistingPolicy`". (2) The
meaning of Validation Rule 4 changes: "schedule regular with the 60s boot delay" was realized as one extra backup about a
minute after every restart; after the fix, with a regular backup already registered, a restart starts no extra backup
(BUG-027, "What changes for the user"). The owner can notice that. (3) BUG-027's Solution Architect rules it a
behavioural break (BUG-027 Suggested Approach, "Ruling").

*Against:* (1) INV-1's clause, "at most one non-terminal work item per unique name", is better met, not changed. (2) INV-1's
realization already allows `REPLACE/UPDATE` for periodic work (CNTR-004 `:240`); the boot path now enqueues periodic
work, so it uses a policy the invariant already permits for that kind. (3) Rule 4 says "schedule regular"; the new
behaviour does exactly that, closer to the legacy branches (the old-scheduler branch restarted the recurring regular job
60 s after boot; the default branch left the persisted job alone, `BackupJobs.java:91-95`).

**Recommended ruling: a behavioural break of the boot path, not breaking for consumers.** The deciding point is that
a user-visible scheduling effect changes on purpose (no extra backup after a restart when a regular backup is
registered), and the contract's policy is written to make such a change explicit and test-gated, not to argue it away.
The INV-1 clause keeps its wording; its realization text gains one sentence (C-10). The gate: the INV-1 row of
DES-005 §Design Validation gains the boot tests (D-9), which the fix story adds (BUG-027 AC-1 to AC-4). No consumer
migration. If the orchestrator rules "not a break" instead, only the wording of changelog item 2 (C-15) and of the
Current-version line (C-13) changes: replace "a behavioural break of the boot path" by "a behavioural correction of the
boot path, ruled not a break"; nothing else.

### Part 3: the content-trigger example

*For a break:* the example drops a network constraint and a backoff from the trigger, and the legacy trigger job had
both (`createBuilder(INCOMING)`, `BackupJobs.java:168-172`, E-2). Read as a statement about INV-2, the trigger's network
requirement changes.

*Against:* (1) An example restated to match the code is not a change to an invariant: INV-2's and INV-3's wording is not
touched. (2) INV-2 and INV-3 are rules for backup work. The trigger worker does no network I/O and always returns
success, so a backoff never applies (`BackupTriggerWorker.kt:59-71`); a network constraint on it would only delay the
start of the debounce (BUG-023 §2). The `INCOMING` backup it enqueues carries INV-2 and INV-3 through
`scheduleIncoming()` (`WorkManagerScheduler.kt:145-150`; U-069). (3) The code already has this shape (E-1), so nothing
changes on any device. (4) `UPDATE` is within INV-1's "REPLACE/UPDATE" for periodic work.

**Recommended ruling: not a break.** The example, the `NONE` and `ExistingPolicy` notes, and one scoping sentence in
Rule 2 (C-16: "INV-2 and INV-3 govern backup work") are corrected. If the orchestrator holds that the scoping sentence
changes INV-2's substance, the part becomes a behavioural break with no code change (the code is already there), and
only changelog item 3 changes its label.

### Part 4: the SMS-received receiver is no longer an incoming path

*For a break:* the contract v3 describes the receiver as a path that, when delivered, enqueues `INCOMING`
(`:245`); removing it removes a described behaviour.

*Against:* (1) v3 says, in the same note: "Whether that path is itself a binding clause is not decided here; removing
it would be a separate, recorded change." This record is that recorded change. (2) INV-4's obligation, "an SMS (and,
when enabled, call-log) content change MUST trigger backup", is met by the content trigger alone, which also sees sent
messages and call-log changes the receiver never saw. The policy protects "the content-trigger or its debounce", and
both stay. (3) The receiver has not been delivered `SMS_RECEIVED` since U-001 made it non-exported on 2026-06-02
(BUG-023 "Severity": commit `50f3b34f`; `RECEIVE_SMS` never requested); a debuggable emulator build showed 0 `onReceive(`
lines (BUG-023, citing `sdlc/artifacts/build/sprints/sprint-022/U-066/evidence/IC1-emulator.txt:27`). So nothing
observable changes on any device. (4) Removing a consumer of the port changes no port signature.

**Recommended ruling: not a break.** The product owner's decision of 2026-11-21 (Record 20) is carried out.

### The version step

The contract has no major or minor rule; it uses whole integers (v1 → v2 additive, v2 → v3 non-breaking, CR-015
"Version step"). Recommendation: **v4**, in place (`status: approved` stays). The changelog entry states in its first
lines that v4 is non-breaking for consumers and that its boot-path part is a behavioural break gated by tests. Rejected:
a decimal (no precedent); two version steps (rulings 55 and 56 and U-069's close-out ask for one record and one step).

BUG-025's Suggested Approach called a possible trigger change "CNTR-MODERNIZATION-004 v4" (BUG-025 `:241`). Ruling 54
withdrew that change (text only, no code). If a trigger change is decided later, it takes the next free version (v5).
BUG-027's Suggested Approach anticipated this numbering ("BUG-025's planned record … becomes v5").

### Invariant and rule texts after the change

| Text | Change | Edit |
|------|--------|------|
| INV-1 clause | Untouched | — |
| INV-1 realization | One sentence added: `REGULAR` and `CONTENT_TRIGGER` use `UPDATE`; `REGULAR` falls back to `CANCEL_AND_REENQUEUE` only when `UPDATE` fails; the boot path never enqueues a one-off under `REGULAR` | C-10 |
| INV-2 (clause and realization) | Untouched | — |
| INV-3 (clause and realization) | Untouched | — |
| INV-4 clause | Untouched | — |
| INV-4 realization | Last sentence: the receiver sentence replaced by "The content trigger is the one incoming path" | C-11 |
| INV-4 note (v3) | The receiver sentences replaced | C-12 |
| Rule 1, Rule 3, Rules 5 to 8 | Untouched | — |
| Rule 2 | One scoping sentence appended (INV-2 and INV-3 govern backup work; the trigger observer and its `INCOMING` follow-up) | C-16 |
| Rule 4 | Reworded: the boot path registers the regular periodic backup, never a one-off under `REGULAR` | C-17 |
| Rule 4a (new; numbered so that Rules 5 to 8, which other artifacts cite by number, keep their numbers) | A non-positive regular interval means "Never" | C-17 |

### Unique work names and policies after the change (true once the two stories are done)

| Unique name | Work | Request | Policy | Enqueued by |
|-------------|------|---------|--------|-------------|
| `REGULAR` | `BackupWorker`, tag `REGULAR` | periodic, interval `max(getRegularTimeoutSecs(), 900 s)`; network per `isWifiOnly()`; `EXPONENTIAL` 30 s; first-run delay 60 s when the boot path makes a new registration | `UPDATE`; `CANCEL_AND_REENQUEUE` only when `UPDATE` fails | `scheduleRegular()`, `scheduleBootup()`; nothing is registered, and an existing entry is cancelled, when `getRegularTimeoutSecs() <= 0` |
| `BOOT` | — | — | — | **Not used.** Exists only if the owner answers PO-1 or PO-2 "yes" (below) |
| `INCOMING` | `BackupWorker`, tag `INCOMING` | one-off, initial delay `getIncomingTimeoutSecs()`; network per `isWifiOnly()`; `EXPONENTIAL` 30 s | `REPLACE` | `scheduleIncoming()` only; its production caller is `BackupTriggerWorker` (the receiver's call goes with the receiver) |
| `CONTENT_TRIGGER` | `BackupTriggerWorker`, tag `CONTENT_TRIGGER` | periodic 15 min with content-URI triggers (SMS; call log when enabled); no network constraint; no backoff set | `UPDATE` | `scheduleContentTrigger()` |
| `BROADCAST_INTENT` | `BackupWorker` | one-off, `Constraints.NONE`, `EXPONENTIAL` 30 s, no delay | `REPLACE` | `scheduleImmediate()` |
| `MANUAL`, `SKIP` | `BackupWorker`, tag = name | one-off, `Constraints.NONE`, `EXPONENTIAL` 30 s, no delay | `REPLACE` | `scheduleManual(type)` |
| restore name (`config.uniqueName`) | `RestoreWorker` | one-off, `Constraints.NONE`, `EXPONENTIAL` 30 s | `REPLACE` | `scheduleRestore(config)` |

Sources: `WorkManagerScheduler.kt:145-154`, `:182-197`, `:232-246`, `:309-316`, `:350-357`, `:385-392` (the `REGULAR`
row's boot and "Never" parts are the target of the fix story).

### The product owner's open questions PO-1 and PO-2 (BUG-027)

The applied text states the recommended answer **"no" to both** (Record 20, `:2479`: "the fix uses the safe answer,
no, for both until decided"): with "Never" a restart starts no backup (PO-1), and a restart does not start a backup about
a minute after boot when a regular backup is registered (PO-2).

**Superseded by Amendment 1 (S-7):** the list below was incomplete against the applied text; the completed list is in
"Amendment 1", section "S-7". This paragraph is kept as written.

> **If the owner later answers "yes" (a mechanical amendment).** Only these sentences change; nothing else in v4 does.
>
> *PO-1 "yes"* (one backup after a restart when the regular schedule is "Never"):
> 1. C-3 (`scheduleBootup()` block), the sentence "same \"Never\" rule" becomes "with a non-positive regular interval:
>    cancels `REGULAR`, then enqueues one one-time `BackupWorker` under the unique name `BOOT` (`REPLACE`, initial delay
>    60 s, network per `isWifiOnly()`, `EXPONENTIAL` 30 s, tag `REGULAR` so the worker infers type `REGULAR`)", and "starts
>    no separate boot backup" gains "except under \"Never\" (PO-1)".
> 2. C-17, Rule 4: "With a non-positive regular interval (\"Never\") it registers nothing (Rule 4a)" becomes "With a
>    non-positive regular interval (\"Never\") it registers no regular backup and enqueues the one-time `BOOT` backup
>    (Rule 4a)"; Rule 4a's "(and the boot path)" is deleted.
> 3. C-5, the unique-name table: the `BOOT` row reads "`BackupWorker`, tag `REGULAR` | one-off, delay 60 s, network per
>    `isWifiOnly()`, `EXPONENTIAL` 30 s | `REPLACE` | `scheduleBootup()`, only when the regular interval is non-positive".
>    C-4's bullet "No `BOOT` unique name is used" becomes "`BOOT` is used only under \"Never\" (PO-1)".
> 4. DES-005 D-13 and D-11 follow the same sentences. Ruling: non-breaking (a new one-off kind under its own name; INV-1 holds per name; no `REGULAR` exists then, so no overlap).
>
> *PO-2 "yes"* (a backup about a minute after every restart):
> 1. The same four places, with "only when the regular interval is non-positive" replaced by "at every boot", and one
>    sentence added to the INV-1 realization (C-10): "A `BOOT` run never starts while a `REGULAR` run is enqueued or
>    running; the adapter guards this (the guard is designed by the planner, BUG-027 PO-2)."
> 2. Changelog: a new version (v5) records it as a behavioural change of the boot path (an added backup), gated by a test
>    of the guard.

---

## Impact Analysis

`change_records.layers` is not configured in `sdlc/config.yaml`; the layers used by CR-015 are used. The searches are in
Evidence E-3.

### Layer Impact Matrix

| # | Layer | Affected? | Files | Occurrences | Action Needed |
|---|-------|-----------|-------|-------------|---------------|
| 1 | Ground truth: the four bugs, the device observation, Record 20 and 21, the code at `fd70325a`, the legacy code at `d095fe02^` | Ground truth | see Source of Truth | n/a | None (read only) |
| 2 | Requirements (`sdlc/artifacts/requirements`) | **Yes, notes** | REQ-MODERNIZATION-005 (`:17-18`, `:87`); REQ-MODERNIZATION-001 (`:12-13`, after `:101`) | 2 files, 5 changes | R-1a to R-1c, R-2a, R-2b |
| 3 | Design and contracts (`sdlc/artifacts/design`) | **Yes** | CNTR-MODERNIZATION-004 (22 edits); DES-MODERNIZATION-005 (20); DES-MODERNIZATION-008 (3); DES-MODERNIZATION-014 (5); DES-MODERNIZATION-001 (2). CNTR-MODERNIZATION-006 `:44`: false today, not edited (Scope Concern 8) | 5 files, 52 changes | C-0 to C-21, D-0 to D-19, H-0 to H-2, E-0 to E-4, P-0, P-1 |
| 4 | Stories, sprints, change records (`sdlc/artifacts/build`, `sdlc/artifacts/stories`) | Two stories to write | The fix story (BUG-026 + BUG-027) and the receiver-removal story do not exist yet (sprint-025 plan `:25`: "No story IDs and no waves are assigned"); U-073 is planned against "existing tests unedited" | 0 active | Both stories cite this record (below). U-069's Implementation Notes ask for this record's number (U-069 `:226`) |
| 5 | Application code (`app/src/main`) | **Later (two stories)** | Fix story: `scheduler/WorkManagerScheduler.kt:112-116`, `:160-202`, `:252-286`, comments `:40-42`, `:67-69`; `scheduler/BackupScheduler.java:36` ("Contract: CNTR-MODERNIZATION-004 v2"), `:58-68`, `:83-92` (javadoc). Receiver story: `receiver/SmsBroadcastReceiver.java` (deleted), `AndroidManifest.xml:132-139`, `App.java:46`, `:255-257`, `:359`, `scheduler/BackupScheduler.java:32-33` | 2 + 4 files | Not by this record |
| 6 | Vendored library | No | — | 0 | None |
| 7 | Tests (`app/src/test`) | **Later (two stories)** | Fix story: new tests in `service/WorkManagerSchedulerTest.kt` (BUG-026 AC-1 to AC-5, BUG-027 AC-1 to AC-7, AC-11), no existing test edited. Receiver story: `receiver/SmsBroadcastReceiverTest.java` deleted (4 `@Test`) | 2 files | Not by this record |
| 8 | Documentation (`README.md`, help texts) | Hand-overs only | `README.md:298-302`, `strings.xml:170` (BUG-025's text story); `README.md:304-305`, `:238` (true after the fix story) | 3 places | None here |

Layers touched: 5 of 8 (2 now, 3 later). Layers edited by this record: 2 (design and contracts; requirements notes).
After Amendment 1: design and contracts 5 files, 71 changes (contract 31, DES-005 28, DES-008 3, DES-014 7, DES-001 2);
requirements 2 files, 6 changes (REQ-005 4, REQ-001 2); 77 in all. After Amendment 2: DES-014 9 (E-0 to E-8), design and
contracts 73, 79 in all, plus close-out edits X-1 to X-3.

### What the story that fixes BUG-026 and BUG-027 must cite

Read with Amendment 1, "Changes to obligations elsewhere in this record": C-25 and C-26 are added to item 1; D-23 and D-24
replace the test descriptions of item 2 (every test of the changed behaviour, including the upgrade case, fails on the
code at `fd70325a`, and the story records that run); item 5 adds the call-log condition of `BackupScheduler.java:71-72`.

1. This record and contract v4: the `scheduleRegular()` and `scheduleBootup()` blocks (C-2, C-3), the unique-name table
   (C-5), the INV-1 realization (C-10), Rules 4 and 4a (C-17), the regular example (C-18); the ruling "behavioural break of
   the boot path, gated by tests; non-breaking for consumers".
2. The tests the invariant-test list gains (DES-005 D-9 and D-11): in the INV-1 row, boot after a registered regular
   backup keeps one periodic entry with the same id (BUG-027 AC-1, with `generation` raised by one, AC-5); boot with
   nothing registered registers one periodic entry with a 60 s initial delay and the regular constraint (AC-2); a
   one-time entry under `REGULAR` in `ENQUEUED` and `CANCELLED` state does not block `scheduleRegular()` or
   `scheduleBootup()` (AC-3, `SUCCEEDED` if the harness can drive it); at most one non-terminal `REGULAR`-tagged item after
   the three sequences (AC-4). In the new "Never" row: −1 and 0 give no entry (BUG-026 AC-1); 3600 then −1 leaves no
   non-terminal entry (AC-2); 60, 899 and 900 give 900 000 ms (AC-3); the five offered values give their own interval
   (AC-4); the boot path with −1 or 0 leaves no `REGULAR` entry (BUG-027 AC-11, answer "no").
3. Tests kept green with no edit: `ac5_scheduleBootup_withAutoBackupEnabled_isEnqueued`,
   `ac5_scheduleBootup_autoBackupDisabled_returnsNull`, `se002_scheduleBootup_encryptionDegraded_returnsNullNoWork`,
   every existing `scheduleRegular()` test (`WorkManagerSchedulerTest.kt:98-112`, `:162-`, `:181-`, `:219-`, `:282-`,
   `:533-`), `BootReceiverTest.shouldScheduleBootupBackupAfterBootup`, the `inv4_*` tests and `BackupTriggerWorkerTest`.
4. The PO-1 and PO-2 answer used ("no", Record 20 `:2479`), recorded in its Implementation Notes.
5. The code comments that must agree (BUG-026 AC-6, BUG-027 AC-8): `WorkManagerScheduler.kt:112-115`, `:160-169`,
   `:252-261`, `:40-42`, `:67-69`; and the port javadoc `BackupScheduler.java:36` (says "v2"), `:58-68`, `:83-92`.
6. Empty diffs: `App.java`, `BootReceiver.java`, `SmsBroadcastReceiver.java`, `BackupTriggerWorker.kt`, `BackupWorker.kt`,
   `AndroidManifest.xml` (BUG-027 AC-7).
7. At its close, the orchestrator applies close-out edit X-1 (deletes item K-1 from "Code not yet at v4").
8. BUG-027 AC-9 (device) needs a backup that starts off screen to succeed: U-075 is placed before this story (ruling 60);
   until U-075 is built, ruling 61 applies.

### What the story that removes the SMS-received receiver must cite

Read with Amendment 1: C-30 and D-27 are added to item 1; item 3 adds a static check of the merged manifest (no
`<receiver>` for `SMS_RECEIVED` remains; `RECEIVE_SMS` is still declared).

1. This record and contract v4: the consumer row (C-1), the INV-4 realization and note (C-11, C-12), the example (C-19);
   DES-005 D-7 and D-10; DES-014 E-1 to E-3; the ruling "not a break".
2. **Removed:** `app/src/main/java/com/zegoggles/smssync/receiver/SmsBroadcastReceiver.java` (the class);
   its `<receiver>` element (`AndroidManifest.xml:132-139`); in `App.java` the import (`:46`), the line
   `enableOrDisableComponent(enabled, SmsBroadcastReceiver.class)` (`:359`) and the receiver's name in the comment
   (`:255-257`); `app/src/test/java/com/zegoggles/smssync/receiver/SmsBroadcastReceiverTest.java` (4 tests; the authoritative
   count falls by 4 from the count at the start of the story); the receiver's name in the port javadoc
   (`BackupScheduler.java:32-33`).
3. **Kept:** `RECEIVE_SMS` (`AndroidManifest.xml:17`), because the `SMS_DELIVER` receiver needs it while the app holds the
   SMS role during a restore (Evidence E-1; platform rule recalled, Scope Concern 3). `BootReceiver` and its toggle in
   `setBroadcastReceiversEnabled` (`App.java:358-361`, `:360`) stay.
4. **Coverage:** the receiver package floor is 0.50 with no headroom (`app/build.gradle:521-534`). The story measures it
   after the deletion and may not lower it (Scope Concern 4).
5. The invariant-test list loses `SmsBroadcastReceiverTest.shouldScheduleIncomingBackupAfterIncomingMessage` as INV-4
   evidence (D-10). No test is added for INV-4; `BackupTriggerWorkerTest` and the `inv4_*` tests carry it.
6. At its close, the orchestrator applies close-out edit X-2 (deletes item K-2).

### Interaction with U-073 (runtime libraries, WorkManager 2.12.0)

Both stories are placed after U-072 and before U-073 (rulings 55 and 56). U-073 then plans with the fix story's tests
present and `SmsBroadcastReceiverTest` gone, and REQ-MODERNIZATION-016 AC-8 ("every test covering
CNTR-MODERNIZATION-004 passes unmodified") guards the boot and "Never" tests across the library move. The WorkManager
behaviour the fix relies on (`REPLACE` deletes, `UPDATE`'s type check, `CANCEL_AND_REENQUEUE`) was read in both 2.9.1 and
2.12.0 by BUG-027's author (BUG-027, "Library"); not re-read here.

---

## Proposed Change

Applied inline, right after this record is approved (see "Order relative to the code"). "Old" is quoted from the files
at `fd70325a`, which equal the working tree for these files; each old text was checked to occur exactly once in its file
(`grep -c -F` on the `git show` output). Apply by the quoted text, not by line number. `status: approved` and each file's
`id` and `artifact_type` stay. Where an edit inserts, "Old" is the anchor and "New" repeats it followed by the insertion.

### Dated-note and frontmatter convention

As CR-015 did (and CR-001, CR-011): the record id is added to `change_records` of every file edited; the contract gets a
version bump, a changelog entry and a Notes bullet; each design and requirement gets one body note "Amended 2026-11-21
by CR-017 …". New in this record, because two stories follow: the contract carries a short list **"Code not yet at v4"**
directly under its Current-version line (C-14), with one item per later story. Each item names the clauses the code does
not yet meet and the bug that describes the difference. The orchestrator deletes an item when its story closes
(close-out edits X-1, X-2). Normative clauses ("MUST") state the target; the list makes the known gap visible, so the
contract is never silently false. Descriptive sentences about code that a story will delete are worded so that they are
true before and after ("the story that removes the receiver deletes …").

Stories are named by description, not by ID, because they have no ID yet; the descriptions stay true after IDs are
assigned.

### CNTR-MODERNIZATION-004 (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md`), edits C-0 to C-21

**C-0. Frontmatter, `change_records`.**
Old:
~~~text
  - CR-015
~~~
New:
~~~text
  - CR-015
  - CR-017
~~~

**C-1. Contract Boundary, Consumers row (first two cells; the rest of the row is unchanged).**
Old:
~~~text
| **Consumers** | Broadcast receiver(s), boot receiver, package-replaced receiver, content-URI observer path, settings UI | app |
~~~
New:
~~~text
| **Consumers** | The `com.zegoggles.smssync.BACKUP` broadcast receiver (CNTR-MODERNIZATION-005), boot receiver, package-replaced receiver, content-URI observer path (the content-trigger worker), settings UI. From v4 the SMS-received receiver is not a consumer (CR-017) | app |
~~~

**C-2. Interface, `scheduleRegular()`.**
Old:
~~~text
  scheduleRegular(): Unit
      // recurring backup; network per isWifiOnly(); EXPONENTIAL 30s→300s backoff;
      // REPLACE/UPDATE on a stable unique name. (BackupJobs.java:78)
~~~
New:
~~~text
  scheduleRegular(): Unit
      // recurring backup; network per isWifiOnly(); EXPONENTIAL 30s→300s backoff;
      // UPDATE on the stable unique name REGULAR; if UPDATE fails (a one-time entry
      // under REGULAR left by an earlier build), it re-registers with
      // CANCEL_AND_REENQUEUE (the periodic REPLACE). (BackupJobs.java:78)
      // interval = getRegularTimeoutSecs(); a positive value below 15 min is raised to
      // WorkManager's 15-min minimum. A non-positive value ("Never" is -1; 0 too)
      // registers nothing, cancels an existing REGULAR and returns no job: the legacy
      // `inSeconds > 0` rule (BackupJobs.java:132). (v4, CR-017; Rule 4a)
~~~

**C-3. Interface, `scheduleBootup()`.**
Old:
~~~text
  scheduleBootup(): Unit
      // if !isAutoBackupEnabled() -> cancelAll(); else schedule regular.
      // boot delay = BOOT_BACKUP_DELAY = 60s. (BackupJobs.java:86, :56,:92)
~~~
New:
~~~text
  scheduleBootup(): Unit
      // if !isAutoBackupEnabled() -> cancelAll(); else registers the regular periodic
      // backup exactly as scheduleRegular() does (same REGULAR request, UPDATE, the
      // CANCEL_AND_REENQUEUE fallback, the same "Never" rule), with
      // BOOT_BACKUP_DELAY = 60s as the first-run delay of a new registration. UPDATE
      // keeps an existing registration (its id and schedule; a running regular backup
      // is not cancelled). Never enqueues a one-time REGULAR work and starts no separate
      // boot backup. (BackupJobs.java:86-97, :56; v4, CR-017; Rule 4)
~~~

**C-4. Enum `BackupType`, Notes: a new bullet after the unique-name bullet.**
Old:
~~~text
    manual run and an automation-broadcast run do not REPLACE each other.
~~~
New:
~~~text
    manual run and an automation-broadcast run do not REPLACE each other.
  - The boot path has no unique name of its own: it registers the REGULAR periodic
    work, because it is the regular backup (v4, CR-017). No BOOT unique name is used.
~~~

**C-5. New subsection before the `SchedulerConfig` heading.**
Old:
~~~text
### Value object: `SchedulerConfig` (core)
~~~
New:
~~~text
### Unique work names and policies (v4)

| Unique name | Work | Request | Policy | Enqueued by |
|-------------|------|---------|--------|-------------|
| `REGULAR` | `BackupWorker`, tag `REGULAR` | periodic, interval `max(getRegularTimeoutSecs(), 15 min)`; network per `isWifiOnly()`; `EXPONENTIAL` 30s; first-run delay 60s when the boot path makes a new registration | `UPDATE`; `CANCEL_AND_REENQUEUE` only when `UPDATE` fails | `scheduleRegular()`, `scheduleBootup()`; nothing is registered, and an existing entry is cancelled, when `getRegularTimeoutSecs() <= 0` (Rule 4a) |
| `INCOMING` | `BackupWorker`, tag `INCOMING` | one-off, initial delay `getIncomingTimeoutSecs()`; network per `isWifiOnly()`; `EXPONENTIAL` 30s | `REPLACE` | `scheduleIncoming()`, called by the content-trigger worker |
| `CONTENT_TRIGGER` | `BackupTriggerWorker`, tag `CONTENT_TRIGGER` | periodic, 15 min, content-URI triggers (SMS; call log when enabled); no network constraint; no backoff set | `UPDATE` | `scheduleContentTrigger()` |
| `BROADCAST_INTENT` | `BackupWorker` | one-off, `Constraints.NONE`, `EXPONENTIAL` 30s, no delay | `REPLACE` | `scheduleImmediate()` |
| `MANUAL`, `SKIP` | `BackupWorker`, tag = name | one-off, `Constraints.NONE`, `EXPONENTIAL` 30s, no delay | `REPLACE` | `scheduleManual(type)` |
| restore name | `RestoreWorker` | one-off, `Constraints.NONE`, `EXPONENTIAL` 30s | `REPLACE` | `scheduleRestore(config)` |

No `BOOT` unique name is used. See "Code not yet at v4" for the rows the code reaches only with a later story.

### Value object: `SchedulerConfig` (core)
~~~

**C-6. `SchedulerConfig`, the `backoff` field.**
Old:
~~~text
  backoff: BackoffConfig                   // see below
~~~
New:
~~~text
  backoff: BackoffConfig?                  // see below; null = none set: the content-trigger
                                           // observer only, which never retries (v4)
~~~

**C-7. `SchedulerConfig`, the boot line of `initialDelaySecs`.**
Old:
~~~text
                                           // bootup = BOOT_BACKUP_DELAY = 60 (:56,:92)
~~~
New:
~~~text
                                           // boot path: BOOT_BACKUP_DELAY = 60 (:56) as the
                                           // first-run delay of a new REGULAR registration (v4)
~~~

**C-8. `NetworkRequirement.NONE`.**
Old:
~~~text
  NONE        // adapter -> Constraints.NONE / NOT_REQUIRED ; BROADCAST_INTENT only
              //            (jobConstraints returns new int[0], BackupJobs.java:197)
~~~
New:
~~~text
  NONE        // adapter -> Constraints.NONE / NOT_REQUIRED ; for backup work:
              //   BROADCAST_INTENT (jobConstraints returns new int[0], BackupJobs.java:197)
              //   and MANUAL/SKIP (Rule 8). The content-trigger observer also has NONE: it
              //   is not backup work; INV-2 applies to the INCOMING backup it enqueues (v4)
~~~

**C-9. `ExistingPolicy`.**
Old:
~~~text
Enum: ExistingPolicy { REPLACE }           // one-off: REPLACE; periodic: REPLACE/UPDATE
~~~
New:
~~~text
Enum: ExistingPolicy { REPLACE, UPDATE }   // one-off: REPLACE; periodic: UPDATE (REGULAR,
                                           // CONTENT_TRIGGER); REGULAR falls back to
                                           // CANCEL_AND_REENQUEUE (the periodic REPLACE)
                                           // when UPDATE fails (v4)
~~~

**C-10. INV-1 realization (last cell of the INV-1 row).**
Old:
~~~text
with a stable unique name per `BackupType`. |
~~~
New:
~~~text
with a stable unique name per `BackupType`. `REGULAR` and `CONTENT_TRIGGER` use `UPDATE`; `REGULAR` falls back to `CANCEL_AND_REENQUEUE` (the periodic `REPLACE`) only when `UPDATE` fails. One work kind per unique name: the boot path registers the `REGULAR` periodic work and never enqueues a one-off under `REGULAR` (v4, CR-017). |
~~~

**C-11. INV-4 realization, last sentence.**
Old:
~~~text
The SMS-received broadcast path (`SmsBroadcastReceiver` → `scheduler.scheduleIncoming()`) is unchanged and has no version check; see the note below the table.
~~~
New:
~~~text
The content trigger is the one incoming path (v4); see the note below the table.
~~~

**C-12. The INV-4 note (blockquote under the table), the receiver sentences.** The sentences before them ("…its API ≤23
assertion was never implemented.") and after them ("Test gate: …") are unchanged.
Old:
~~~text
The SMS-received broadcast path (`SmsBroadcastReceiver`, manifest receiver for `SMS_RECEIVED`, no version check) is unchanged. When it is delivered, and auto backup is on, login information is set and the first backup has run, it calls `scheduler.scheduleIncoming()`. That enqueues a delayed one-off of `BackupWorker` under the same unique name (`INCOMING`, REPLACE) as the content trigger's follow-up, so INV-1 collapses the two at enqueue. It covers incoming SMS only: sent messages and call-log changes are seen only by the content trigger. Whether that path is itself a binding clause is not decided here; removing it would be a separate, recorded change.
~~~
New:
~~~text
**The content trigger is the one incoming path (v4, CR-017).** The SMS-received receiver (`SmsBroadcastReceiver`, a manifest receiver for `SMS_RECEIVED`) is not part of this contract from v4. It has not been delivered the broadcast since 2026-06-02, when U-001 declared it `android:exported="false"`, and `RECEIVE_SMS` is never requested at run time (BUG-023). The product owner decided on 2026-11-21 to remove it (decision appendix, Record 20, ruling 56). INV-4 never depended on it: the content trigger sees incoming and sent messages and, when enabled, call-log changes. The story that removes the receiver deletes the class, its manifest entry and its enable/disable in `App`; until then the class is in the code but is not an incoming path in fact (see "Code not yet at v4"). `scheduleIncoming()` stays a port operation; its production caller is the content-trigger worker.
~~~

**C-13. Versioning, Current version.**
Old:
~~~text
- **Current version:** v3 (amended in place by CR-015, 2026-11-21; v2 amended in place by DES-MODERNIZATION-012 / REQ-MODERNIZATION-013; v1 introduced by MU-005 / DES-MODERNIZATION-005). v3 is non-breaking: it changes no port signature, no `BackupType` value, no `SchedulerState` variant and no invariant's substance, so no consumer migration is required; `status` remains `approved`.
~~~
New:
~~~text
- **Current version:** v4 (amended in place by CR-017, 2026-11-21; v3 amended in place by CR-015, 2026-11-21; v2 amended in place by DES-MODERNIZATION-012 / REQ-MODERNIZATION-013; v1 introduced by MU-005 / DES-MODERNIZATION-005). v4 is non-breaking for consumers: it changes no port signature, no `BackupType` value and no `SchedulerState` variant, so no consumer migration is required. Its boot-path part is a behavioural break of the boot path, gated by tests (Changelog, v4, item 2); no invariant's clause changes. `status` remains `approved`.
~~~

**C-14. New blockquote directly after the Current-version line (before `### Changelog`).**
Old:
~~~text
### Changelog
~~~
New:
~~~text
> **Code not yet at v4 (CR-017).** v4 states the target. Until the story named in an item is done, the code differs as the item says; the orchestrator deletes the item when that story closes.
> - **K-1, until the story that fixes BUG-026 and BUG-027:** `scheduleRegular()` raises a non-positive interval to 15 minutes and registers a periodic backup (BUG-026). `scheduleBootup()` enqueues a one-time `BackupWorker` under `REGULAR` with `REPLACE`, which deletes the periodic regular backup at every restart, and the next `scheduleRegular()` then fails silently on that entry (BUG-027). Affected here: the `scheduleRegular()` and `scheduleBootup()` blocks, the `REGULAR` row of the unique-name table, the INV-1 realization, Rules 4 and 4a, and the regular example.
> - **K-2, until the story that removes the SMS-received receiver (ruling 56):** `SmsBroadcastReceiver`, its manifest entry and its enable/disable in `App` are still in the code. The receiver is not delivered `SMS_RECEIVED` and is not an incoming path in fact (BUG-023).

### Changelog
~~~

**C-15. Changelog, new entry above the v3 entry.**
Old:
~~~text
- **v3 — 2026-11-21 (CR-015;
~~~
New (the old text follows unchanged after the new entry):
~~~text
- **v4 — 2026-11-21 (CR-017; BUG-026, BUG-027, BUG-023; product owner's decision of 2026-11-21, decision appendix Record 20, rulings 55 and 56).** Non-breaking for consumers: no port signature, `BackupType` value or `SchedulerState` variant changes. 1. **"Never" schedules nothing (BUG-026).** A non-positive regular interval registers no regular backup and cancels an existing one; a positive interval below 15 minutes is raised to WorkManager's minimum (new Rule 4a; `scheduleRegular()` text). This states the legacy rule (`BackupJobs.java:132`) the contract always claimed to translate; not a break. 2. **The boot path keeps the regular backup (BUG-027).** `scheduleBootup()` registers the regular periodic backup through the same request and policy as `scheduleRegular()` (`REGULAR`, `UPDATE`, `CANCEL_AND_REENQUEUE` only when `UPDATE` fails), with the 60s boot delay as the first-run delay of a new registration. It no longer enqueues a one-time `REGULAR` backup with `REPLACE`, which deleted the periodic work at every restart. Rule 4 and the INV-1 realization say so. Ruled a behavioural break of the boot path (its policy changes from one-off `REPLACE` to periodic `UPDATE`; with a registered regular backup, a restart no longer starts an extra backup a minute later); INV-1's clause is unchanged; gated by the boot tests in DES-MODERNIZATION-005 §Design Validation. The product owner's questions PO-1 (a backup after a restart when the schedule is "Never") and PO-2 (a backup about a minute after every restart) are open; v4 states the answer "no" to both, which the fix uses until the owner decides (Record 20). 3. **Content-trigger example corrected (U-069; BUG-023, Suggested Approach §2).** The registration carries no network constraint and no backoff, and uses `UPDATE`; INV-2 and INV-3 apply to the `INCOMING` backup it enqueues (Rule 2, one scoping sentence). The `NetworkRequirement.NONE` and `ExistingPolicy` notes and the `backoff` field follow. The code has this shape (`WorkManagerScheduler.kt:218-250`); not a break. 4. **The SMS-received receiver is no longer part of the contract (BUG-023; ruling 56).** INV-4 is met by the content trigger alone; the receiver has not been delivered `SMS_RECEIVED` since U-001; it is removed by its own story. Not a break: nothing observable changes, and no consumer signature changes. 5. **Unique work names and policies** are stated in one table. 6. Frontmatter: `CR-017` in `change_records`. 7. The code reaches items 1 and 2 with the story that fixes BUG-026 and BUG-027, and item 4 with the story that removes the receiver; until then see "Code not yet at v4".
~~~

**C-16. Validation Rule 2, one sentence appended at the end of the rule.**
Old:
~~~text
2. **The four invariants (INV-1..INV-4) hold** for every enqueue path
~~~
Apply: keep the rule as it is and append at its end (after "…`contentUriTriggers`)."):
~~~text
 INV-2 and INV-3 govern backup work: the content-trigger observer, which does no network I/O and never retries, carries no network constraint and no backoff of its own, and the `INCOMING` backup it enqueues through `scheduleIncoming()` carries both (v4, CR-017).
~~~

**C-17. Validation Rule 4, replaced, and a new Rule 4a.**
Old:
~~~text
4. **`scheduleBootup` honors `isAutoBackupEnabled()`.** If auto-backup is off, it MUST `cancelAll()` (not schedule); else schedule regular with the 60s boot delay.
~~~
New:
~~~text
4. **`scheduleBootup` honors `isAutoBackupEnabled()` and keeps the regular backup.** If auto-backup is off, it MUST `cancelAll()` (not schedule). Else it MUST register the regular periodic backup through the same request and policy as `scheduleRegular()` (`REGULAR`, `UPDATE`, `CANCEL_AND_REENQUEUE` only when `UPDATE` fails), with the 60s boot delay as the first-run delay of a new registration. It MUST NOT enqueue a one-time work under `REGULAR`, MUST NOT cancel or reset a registered or running regular backup, and starts no separate boot backup. With a non-positive regular interval ("Never") it registers nothing (Rule 4a). The encryption-degraded check comes first and changes nothing (U-054). (v4, CR-017)
4a. **A non-positive regular interval means "Never".** `scheduleRegular()` (and the boot path) MUST register no regular backup when `getRegularTimeoutSecs() <= 0`, MUST cancel an existing `REGULAR` work, and returns no job. A positive interval below 15 minutes is raised to WorkManager's 15-minute minimum; every other positive value is used as given. The encryption-degraded check comes first and changes nothing (U-054). (v4, CR-017; the legacy rule, `BackupJobs.java:132`. Numbered 4a so that Rules 5 to 8 keep their numbers.)
~~~

**C-18. Example "a regular Wi-Fi-only scheduled backup", three lines.**
Old (1):
~~~text
// isWifiOnly() == true
~~~
New (1):
~~~text
// isWifiOnly() == true; getRegularTimeoutSecs() == 7200 (a value <= 0 registers nothing, Rule 4a)
~~~
Old (2):
~~~text
  existingPolicy   = REPLACE,               // INV-1 (periodic: REPLACE/UPDATE on unique name)
~~~
New (2):
~~~text
  existingPolicy   = UPDATE,                // INV-1 (periodic); CANCEL_AND_REENQUEUE only if UPDATE fails
~~~
Old (3):
~~~text
  initialDelaySecs = null,                  // periodic interval, not a delay
~~~
New (3):
~~~text
  initialDelaySecs = null,                  // periodic interval, not a delay; 60 when the boot path makes a new registration
~~~

**C-19. Example "content-URI trigger and its debounce follow-up", the code block's content.** The heading line and the
two fence lines stay.
Old:
~~~text
// scheduleContentTrigger() — recurring observer; SMS provider + call-log when enabled
SchedulerConfig(
  networkType        = CONNECTED,           // or UNMETERED per isWifiOnly()
  backoff            = BackoffConfig(EXPONENTIAL, 30s, 300s),
  existingPolicy     = REPLACE,
  uniqueName         = "CONTENT_TRIGGER",   // CONTENT_TRIGGER_TAG
  recurring          = true,
  contentUriTriggers = [
    ContentUriTrigger(uri = "<sms provider uri>",     triggerForDescendants = true),
    ContentUriTrigger(uri = "<call-log provider uri>", triggerForDescendants = true)  // only when isCallLogBackupAfterCallEnabled()
  ]
)
// On trigger, the worker does NOT back up directly — it calls scheduler.scheduleIncoming(),
// enqueuing a delayed one-off (delay = getIncomingTimeoutSecs()). INV-4 two-stage debounce.

// Separately, the SMS-received broadcast path (incoming SMS only) calls scheduler.scheduleIncoming() ->
//   OneTimeWorkRequest(initialDelay = getIncomingTimeoutSecs())   // same debounce, same unique name INCOMING;
//   INV-1 (REPLACE) collapses it with the content trigger's follow-up.
~~~
New:
~~~text
// scheduleContentTrigger() — the recurring observer (BackupTriggerWorker) over the SMS
// provider, + the call-log provider when call-log backup is enabled and
// isCallLogBackupAfterCallEnabled()
SchedulerConfig(
  networkType        = NONE,                // the observer does no network I/O; INV-2 applies
                                            //   to the INCOMING backup it enqueues (Rule 2)
  requiresCharging   = false,
  backoff            = null,                // none set: the worker always returns success, so it
                                            //   never retries; INV-3 applies to the INCOMING backup
  existingPolicy     = UPDATE,              // INV-1 (periodic)
  uniqueName         = "CONTENT_TRIGGER",   // also its tag (CONTENT_TRIGGER_TAG)
  initialDelaySecs   = null,
  recurring          = true,                // PeriodicWorkRequest, every 15 min (WorkManager's minimum)
  contentUriTriggers = [
    ContentUriTrigger(uri = "<sms provider uri>",     triggerForDescendants = true),
    ContentUriTrigger(uri = "<call-log provider uri>", triggerForDescendants = true)  // only when call-log backup is enabled and isCallLogBackupAfterCallEnabled()
  ]
)
// On trigger, the worker does NOT back up directly — it calls scheduler.scheduleIncoming(),
// enqueuing a delayed one-off (delay = getIncomingTimeoutSecs()). INV-4 two-stage debounce.
// That INCOMING one-off carries INV-1 (REPLACE), INV-2 (network per isWifiOnly()) and
// INV-3 (EXPONENTIAL 30s); scheduleIncoming() is its only builder. There is no second
// incoming path (v4).
~~~

**C-20. Notes, Status bullet.**
Old:
~~~text
- **Status:** approved (v3 amended in place by CR-015; v2 amended in place per DES-MODERNIZATION-012 / REQ-MODERNIZATION-013). All paths are relative to the repository root.
~~~
New:
~~~text
- **Status:** approved (v4 amended in place by CR-017; v3 amended in place by CR-015; v2 amended in place per DES-MODERNIZATION-012 / REQ-MODERNIZATION-013). All paths are relative to the repository root.
~~~

**C-21. Notes, new last bullet (after the "Amended 2026-11-21 by CR-015 (v3)" bullet).**
Old:
~~~text
- **Amended 2026-11-21 by CR-015 (v3):**
~~~
Apply: keep that bullet and add after it, as the last bullet of the file:
~~~text
- **Amended 2026-11-21 by CR-017 (v4):** a regular schedule of "Never" schedules nothing (Rule 4a); the boot path registers the regular periodic backup and never a one-time `REGULAR` work (Rule 4; a behavioural break of the boot path, gated by tests); the content-trigger example matches the code (no network constraint, no backoff, `UPDATE`); the SMS-received receiver is no longer part of the contract, by the product owner's decision of 2026-11-21. The code follows in two stories; see "Code not yet at v4" and the Changelog, v4.
~~~

### DES-MODERNIZATION-005 (`sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md`), edits D-0 to D-19

**D-0. Frontmatter, `change_records`.** Old: `  - CR-015`. New: `  - CR-015` then `  - CR-017` on the next line.

**D-1. Port block, `scheduleRegular()`.**
Old:
~~~text
    scheduleRegular()                   // periodic / recurring, constraint-bound
~~~
New:
~~~text
    scheduleRegular()                   // periodic / recurring, constraint-bound; "Never" (<= 0): none, REGULAR cancelled
~~~

**D-2. Port block, `scheduleBootup()`.**
Old:
~~~text
    scheduleBootup()                    // honors isAutoBackupEnabled(); cancelAll() if off
~~~
New:
~~~text
    scheduleBootup()                    // honors isAutoBackupEnabled(); cancelAll() if off; else registers the REGULAR periodic (no one-off)
~~~

**D-3. Legacy-mechanism table, Firebase JobDispatcher row, last cell.**
Old:
~~~text
`OneTimeWorkRequest` (incoming, bootup, immediate, restore) |
~~~
New:
~~~text
`OneTimeWorkRequest` (incoming, immediate, manual, restore). The boot path registers the regular `PeriodicWorkRequest`, not a one-off (CR-017) |
~~~

**D-4. Components table, a new row after the `RestoreWorker` row.**
Old:
~~~text
| `RestoreWorker` | app | `@HiltWorker` `CoroutineWorker`; durable checkpoint; calls core restore use-case |
~~~
New:
~~~text
| `RestoreWorker` | app | `@HiltWorker` `CoroutineWorker`; durable checkpoint; calls core restore use-case |
| `BackupTriggerWorker` | app | `@HiltWorker` `Worker` (U-069); stage 1 of the INV-4 debounce: runs on a content-URI change and calls `BackupScheduler.scheduleIncoming()`; never backs up itself |
~~~

**D-5. Contract-mapping table, the `Trigger.NOW` row.**
Old:
~~~text
| `Trigger.NOW` when `inSeconds <= 0` (`:162`) | `OneTimeWorkRequest` with no initial delay | — |
~~~
New:
~~~text
| `Trigger.NOW` when `inSeconds <= 0` (`:162`), reachable only for a forced job, that is `scheduleImmediate()` (`:99-100`); a job that was not forced and had `inSeconds <= 0` was not scheduled at all (`schedule()`, `:132`) | `OneTimeWorkRequest` with no initial delay (immediate and manual only); a non-positive regular interval ("Never") registers nothing and cancels `REGULAR` (CNTR-MODERNIZATION-004 v4, Rule 4a; CR-017) | — |
~~~

**D-6. Contract-mapping table, the lifetime row, middle cell.**
Old:
~~~text
one-off `UNTIL_NEXT_BOOT` semantics are emulated by enqueueing at boot, not by a lifetime flag
~~~
New:
~~~text
one-off work survives it too (a pending `INCOMING` from before a restart ran after it: `sdlc/artifacts/build/research/2026-11-21-background-backup-and-boot-observation.md`, Run 1, 09:27:45), so legacy `UNTIL_NEXT_BOOT` is not reproduced. At boot the app registers only the regular periodic backup (`UPDATE`, which keeps an existing registration) and enqueues no one-off (CR-017)
~~~

**D-7. "Incoming-SMS trigger (AC-3d)", the SMS-received bullet (one line, replaced whole).**
Old:
~~~text
- **SMS-received broadcast (no version check, unchanged):**
~~~
Apply: replace that whole bullet (to the end of its line, "…it does not see sent messages or call-log changes.") by:
~~~text
- **No SMS-received path (CR-017).** The SMS-received receiver is not an incoming path: it has not been delivered `SMS_RECEIVED` since U-001 made it non-exported (2026-06-02; `RECEIVE_SMS` is never requested at run time; BUG-023), and the product owner decided on 2026-11-21 to remove it (decision appendix, Record 20, ruling 56). The story that removes it deletes the class, its manifest entry, its enable/disable in `App` and its test. The content trigger above is the one incoming path; it also sees sent messages and, when enabled, call-log changes.
~~~

**D-8. "Hilt worker construction", first sentence.**
Old:
~~~text
`BackupWorker` and `RestoreWorker` are annotated `@HiltWorker` with `@AssistedInject` constructors
~~~
New:
~~~text
`BackupWorker`, `RestoreWorker` and, since U-069, `BackupTriggerWorker` (a plain `Worker`, injected with the `BackupScheduler` port and `Preferences`) are annotated `@HiltWorker` with `@AssistedInject` constructors
~~~

**D-9. Design Validation, INV-1 row, sentences appended to its test cell.**
Old:
~~~text
the equivalent of `setReplaceCurrent(true)`. |
~~~
New:
~~~text
the equivalent of `setReplaceCurrent(true)`. For `REGULAR` (CNTR-MODERNIZATION-004 v4, Rule 4): after `scheduleRegular()`, `scheduleBootup()` leaves one periodic `ENQUEUED` entry with the same id; with nothing registered, `scheduleBootup()` registers one periodic entry with a 60 s initial delay and the regular network constraint; a one-time entry under `REGULAR` (`ENQUEUED` or `CANCELLED`) does not block `scheduleRegular()` or `scheduleBootup()`; at most one non-terminal `REGULAR`-tagged item exists after any sequence of the two and `cancelAll()`. These tests are added by the story that fixes BUG-026 and BUG-027 (BUG-027 AC-1 to AC-5). |
~~~

**D-10. Design Validation, INV-4 row, last sentence.**
Old:
~~~text
The SMS-received path's receiver logic is asserted by `SmsBroadcastReceiverTest.shouldScheduleIncomingBackupAfterIncomingMessage` (`scheduleIncoming()` is called once); delivery of the broadcast to the receiver is not unit-tested. |
~~~
New:
~~~text
There is no SMS-received path from CNTR-MODERNIZATION-004 v4 (CR-017): `SmsBroadcastReceiverTest` is not INV-4 evidence and is deleted with the receiver by the story that removes it. Stage 2 of the debounce (the trigger worker calls `scheduleIncoming()`) is asserted by `BackupTriggerWorkerTest` (U-069). |
~~~

**D-11. Design Validation, a new row directly after the INV-4 row (as edited by D-10).** Insert as the next table line:
~~~text
| **Regular schedule "Never" (CNTR-MODERNIZATION-004 v4, Rule 4a)** | With `regularTimeoutSecs` of −1 and of 0, `scheduleRegular()` returns no job and leaves no non-terminal `REGULAR` entry, also after an earlier registration with 3600; `scheduleBootup()` with −1 or 0 does the same; a positive value below 900 (60, 899) and 900 give a repeat interval of 900 000 ms; each offered value (1800, 3600, 7200, 21600, 86400) gives its own interval. Added by the story that fixes BUG-026 and BUG-027 (BUG-026 AC-1 to AC-4; BUG-027 AC-11). |
~~~

**D-12. Port table, `scheduleRegular()` row.**
Old:
~~~text
| `scheduleRegular()` | recurring backup, network per `isWifiOnly()`, exponential backoff (30s→300s) | `enqueueUniquePeriodicWork(REGULAR, REPLACE/UPDATE)` |
~~~
New:
~~~text
| `scheduleRegular()` | recurring backup, network per `isWifiOnly()`, exponential backoff (30s→300s); a non-positive interval ("Never") registers nothing and cancels `REGULAR`; a positive interval below 15 min is raised to 15 min | `enqueueUniquePeriodicWork(REGULAR, UPDATE)`; `CANCEL_AND_REENQUEUE` only when `UPDATE` fails (CR-017) |
~~~

**D-13. Port table, `scheduleBootup()` row.**
Old:
~~~text
| `scheduleBootup()` | if `!isAutoBackupEnabled()` → `cancelAll()`; else schedule regular | per-branch, mirrors `BackupJobs.scheduleBootup` |
~~~
New:
~~~text
| `scheduleBootup()` | if `!isAutoBackupEnabled()` → `cancelAll()`; else registers the regular periodic backup as `scheduleRegular()` does, with the 60 s boot delay as the first-run delay of a new registration; no one-off, no separate boot backup; "Never" registers nothing | the same periodic `REGULAR` request, `UPDATE` (fallback `CANCEL_AND_REENQUEUE`), `setInitialDelay(60 s)`. Translates both legacy branches: the old-scheduler branch restarted the recurring regular job 60 s after boot, the default branch kept the persisted job (`BackupJobs.java:91-95`) (CR-017) |
~~~

**D-14. Value-object table, `networkType` row, middle cell.**
Old:
~~~text
| `networkType` | `UNMETERED` (wifi-only) \| `CONNECTED` (any) \| `NONE` (immediate) |
~~~
New:
~~~text
| `networkType` | `UNMETERED` (wifi-only) \| `CONNECTED` (any) \| `NONE` (immediate, manual; also the content-trigger observer, which is not backup work, CR-017) |
~~~

**D-15. Value-object table, `existingPolicy` row.**
Old:
~~~text
| `existingPolicy` | `REPLACE` | `setReplaceCurrent(true)`, universal (`:188`) |
~~~
New:
~~~text
| `existingPolicy` | `REPLACE` (one-off); `UPDATE` (periodic: `REGULAR`, content trigger), `REGULAR` falling back to `CANCEL_AND_REENQUEUE` when `UPDATE` fails (CR-017) | `setReplaceCurrent(true)`, universal (`:188`) |
~~~

**D-16. Value-object table, `uniqueName` row, middle cell.**
Old:
~~~text
| `uniqueName` | per `BackupType` (`REGULAR`/`INCOMING`/`BROADCAST_INTENT`) + `contentTrigger` tag + restore |
~~~
New:
~~~text
| `uniqueName` | per `BackupType` (`REGULAR`/`INCOMING`/`BROADCAST_INTENT`/`MANUAL`/`SKIP`) + `contentTrigger` tag + restore; the boot path uses `REGULAR`, because it registers the regular backup (CR-017) |
~~~

**D-17. Value-object table, `bootDelaySecs` row.**
Old:
~~~text
| `bootDelaySecs` | `60` (`BOOT_BACKUP_DELAY`) | `BackupJobs.java:56,92` |
~~~
New:
~~~text
| `bootDelaySecs` | `60` (`BOOT_BACKUP_DELAY`): the first-run delay of a new `REGULAR` registration made at boot (CR-017) | `BackupJobs.java:56,92` |
~~~

**D-18. Risks table, a new row after the `(30,300)` row.**
Old:
~~~text
| Misreading `(30,300)` as two strategies | Medium |
~~~
Apply: keep that row and add after it:
~~~text
| A one-time entry under a periodic unique name blocks `UPDATE` (WorkManager rejects the change of work type) silently, because the returned `Operation` is not read; the regular backup is then gone until WorkManager prunes the entry | Medium | The boot path never enqueues a one-off under `REGULAR`; `scheduleRegular()` and the boot path re-register with `CANCEL_AND_REENQUEUE` when `UPDATE` fails; tests with a one-time entry in `ENQUEUED` and `CANCELLED` state (BUG-027 AC-3; CR-017) |
~~~

**D-19. Notes, a new last paragraph (after the CR-015 paragraph).**
Old:
~~~text
Amended 2026-11-21 by CR-015: the pre-API-24
~~~
Apply: keep that paragraph and add after it:
~~~text
Amended 2026-11-21 by CR-017 (CNTR-MODERNIZATION-004 v4): (1) a regular schedule of "Never" (a non-positive interval) registers nothing and cancels `REGULAR`, and the `Trigger.NOW` mapping row is corrected (it applied only to the forced immediate job); (2) the boot path registers the regular periodic backup (`UPDATE`, fallback `CANCEL_AND_REENQUEUE`) instead of a one-time `REGULAR` backup, and the legacy-mechanism table, the lifetime row, the port table, the value-object rows and a new risk row say so; (3) `BackupTriggerWorker` is listed as a `@HiltWorker` (U-069) and the content-trigger observer as carrying no network constraint; (4) the SMS-received receiver is no longer described as an incoming path (product owner's decision, Record 20, ruling 56). The code reaches (1) and (2) with the story that fixes BUG-026 and BUG-027 and loses the receiver with the story of ruling 56; until then see the contract's "Code not yet at v4". The Design Validation rows for (1) and (2) name tests those stories add.
~~~

### DES-MODERNIZATION-008 (`sdlc/artifacts/design/modernization/DES-MODERNIZATION-008-hilt-di-design.md`), edits H-0 to H-2

**H-0. Frontmatter.** Old: `change_records: []`. New: `change_records:` then `  - CR-017` on the next line.

**H-1. Component graph and scope, the `@HiltWorker` row.**
Old:
~~~text
| `@HiltWorker` (assisted, per-work) | `BackupWorker`, `RestoreWorker` |
~~~
New:
~~~text
| `@HiltWorker` (assisted, per-work) | `BackupWorker`, `RestoreWorker`, `BackupTriggerWorker` (since U-069; a plain `Worker` injected with the `BackupScheduler` port and `Preferences`) |
~~~

**H-2. Notes, a new last paragraph (end of file, after "…`@Binds`/`@Provides` entry.").**
~~~text

Amended 2026-11-21 by CR-017: the `@HiltWorker` row lists `BackupTriggerWorker`, the content-trigger worker that story U-069 made a Hilt worker (commit `d7a69974`). It is created by `HiltWorkerFactory` like the other two; nothing else in this design changes.
~~~

### DES-MODERNIZATION-014 (`sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-toolchain-runtime-libraries-platform-level-design.md`), edits E-0 to E-4

**E-0. Frontmatter, `change_records`.** Old: `  - CR-016`. New: `  - CR-016` then `  - CR-017` on the next line.

**E-1. D14, Run R.**
Old:
~~~text
only the `SmsBroadcastReceiver` and `BackupTriggerWorker` log lines are read: around T0, and around
~~~
New:
~~~text
only the `BackupTriggerWorker` log lines are read (the SMS-received receiver is not delivered the broadcast and is removed, CR-017): around T0, and around
~~~

**E-2. Design Validation table, the "trigger at release" row.**
Old:
~~~text
reading only the `SmsBroadcastReceiver` and `BackupTriggerWorker` lines;
~~~
New:
~~~text
reading only the `BackupTriggerWorker` lines (CR-017);
~~~

**E-3. D14, the OTP paragraph.**
Old:
~~~text
The `SMS_RECEIVED` trigger (`SmsBroadcastReceiver.java:58-60`) is also withheld for such a message.
~~~
New:
~~~text
The `SMS_RECEIVED` broadcast is also withheld for such a message; the app has no receiver for it that is delivered (the SMS-received receiver is removed, CR-017), so the content trigger is the only trigger concerned.
~~~

**E-4. A dated note after the existing top note.**
Old:
~~~text
> this design already follows: the count at the start of each story, and the configured floors.
~~~
Apply: keep that line and add after it a blank line and:
~~~text
> Note added 2026-11-21 by CR-017 (no decision of this design changes). The SMS-received receiver
> (`SmsBroadcastReceiver`) is not delivered `SMS_RECEIVED` and is removed by its own story (product owner's
> decision, decision appendix Record 20, ruling 56). Run R and its Design Validation row read only the
> `BackupTriggerWorker` line. The behaviour-change row 37-5, the "Integration Contracts" argument for CR-015
> and the D14 file list name the receiver as it was when they were written; they are history and are not edited.
~~~

### DES-MODERNIZATION-001 (`sdlc/artifacts/design/modernization/DES-MODERNIZATION-001-restore-play-store-eligibility-design.md`), edits P-0 and P-1

**P-0. Frontmatter.** Old: `change_records: []`. New: `change_records:` then `  - CR-017` on the next line.

**P-1. Notes, a new last paragraph, directly before `## Artifacts Consulted`.**
Old:
~~~text
## Artifacts Consulted
~~~
New:
~~~text
Amended 2026-11-21 by CR-017 (note only; no target value changes). The export rationale for `SmsBroadcastReceiver` in the receiver table ("Receives system `SMS_RECEIVED` implicit broadcast; not a third-party contract") was wrong: the broadcast is sent by the telephony process, not by this app, so a receiver declared `android:exported="false"` is not delivered it (BUG-023, Suggested Approach, "Severity, and the SMS-received receiver"). The receiver has not run since this design was built. The product owner decided on 2026-11-21 to remove it (decision appendix, Record 20, ruling 56); the scheduler contract CNTR-MODERNIZATION-004 v4 no longer describes it. The rows for the other five receivers are unaffected.

## Artifacts Consulted
~~~

### REQ-MODERNIZATION-005 (`sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md`), edits R-1a to R-1c

**R-1a. Frontmatter, `change_records`.** Old: `  - CR-015`. New: `  - CR-015` then `  - CR-017` on the next line.

**R-1b. Notes, the CR-015 note, its last-but-one sentence.**
Old:
~~~text
The `SMS_RECEIVED` path is not removed.
~~~
New:
~~~text
CR-015 did not remove the `SMS_RECEIVED` path; CR-017 removes it from the contract on the product owner's decision of 2026-11-21 (decision appendix, Record 20, ruling 56), and its own story removes it from the code.
~~~

**R-1c. Notes, a new last paragraph (after the CR-015 note).**
~~~text

Amended 2026-11-21 by CR-017: no criterion changes. AC-3(d) is met by the content-URI trigger alone; the SMS-received receiver, which it never relied on, has not been delivered the broadcast since U-001 and is removed. "Preserving all current scheduling behavior" (Description) is read with two corrections that CNTR-MODERNIZATION-004 v4 makes, each restoring the legacy behaviour: a regular schedule of "Never" schedules nothing (BUG-026), and a restart keeps the regular backup (BUG-027). The extra backup about a minute after every restart, which the migrated code ran while it deleted the regular schedule, is not kept unless the product owner decides otherwise (BUG-027, PO-1 and PO-2).
~~~

### REQ-MODERNIZATION-001 (`sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-001-restore-play-store-eligibility.md`), edits R-2a and R-2b

**R-2a. Frontmatter, `change_records`.** Old: `  - CR-016`. New: `  - CR-016` then `  - CR-017` on the next line.

**R-2b. A note after the "Scope correction" blockquote of the export section.**
Old:
~~~text
Synced with DES-MODERNIZATION-001.
~~~
Apply: keep that line (the end of the "Scope correction" blockquote) and add after it a blank line and:
~~~text
> **Amended 2026-11-21 by CR-017 (note only; no criterion changes).** The rationale above for `SmsBroadcastReceiver` was wrong: `SMS_RECEIVED` is sent by the telephony process, not by this app, so a receiver declared `android:exported="false"` is not delivered it (BUG-023, Suggested Approach). The receiver has not run since the change. The product owner decided on 2026-11-21 to remove it (decision appendix, Record 20, ruling 56). Once its story is done, AC-15 and AC-16 apply to the five remaining receivers, and their `SmsBroadcastReceiver` clause has no case.
~~~

### Close-out edits (applied by the orchestrator when each story closes, not when this record is applied)

| # | When | File and place | Old | New |
|---|------|----------------|-----|-----|
| X-1 | The story that fixes BUG-026 and BUG-027 closes | CNTR-004, the "Code not yet at v4" blockquote (C-14) | the line starting `> - **K-1, until the story that fixes BUG-026 and BUG-027:**` | (deleted) |
| X-2 | The story that removes the SMS-received receiver closes | the same blockquote | the line starting `> - **K-2, until the story that removes the SMS-received receiver (ruling 56):**` | (deleted) |

When both are applied, the whole blockquote (its first line too) is deleted. No version step: they remove a statement
of divergence, not a rule. (X-1 and X-2 also apply to K-1 and K-2 as amended by C-26; see Amendment 1.)

### Not edited, on purpose

- **CNTR-004 lines outside the four subjects that are false today** (Scope Concern 7): the "Draft; not finalized" note
  (`:24`), the debug-only producer row (`:42`), the `Unit` and `Flow` return types (`:59-103`), the swap sentence of
  `:267`, the DES-008 row's "`CompositeScheduler` in debug" (`:400`) and the "v2 implementation note" (`:410`).
- **CNTR-004 `:255`** (the v3 changelog, "the SMS-received broadcast path is unchanged") and **`:258`**: history.
- **CNTR-004 INV-2 and INV-3 rows, Rules 1, 3, 5 to 8, Error Handling, Dependencies**: true as they stand.
- **CNTR-006 `:44`**: false today for the SMS receiver (it publishes no event), outside this record (Scope Concern 8).
- **DES-005 `:39`, `:107-112`, `:133-138`, `:184-206` (ADR-005-A), `:230-242` (traceability), `:278-279`, `:282-286`,
  `:320`, `:376-378`, `:434`, `:436`, `:438-443`, `:495`**: true as they stand.
- **DES-007 `:313`, `:325`; DES-012 `:114`, `:183-185`; DES-014 `:1287`, `:1815-1838`, `:2169`**: history (E-4 covers DES-014).
- **REQ-020 `:94`, `:108`; REQ-013 `:212`, `:264`; REQ-016 `:49-53`**: true as they stand.
- **README and help texts**: hand-overs (Evidence, last table).
- **Code, tests, sprint plans, stories, earlier change records.**

---

## Amendment 1 (2026-11-21): texts the applied-text check found

**Source.** After edits C-0 to C-21, D-0 to D-19, H-0 to H-2, E-0 to E-4, P-0, P-1, R-1a to R-1c, R-2a and R-2b were
applied to the working tree (2026-11-21, uncommitted; X-1 and X-2 not applied, as ordered), an independent Design
Auditor checked the applied text and wrote `sdlc/artifacts/build/change-records/evidence/CR-017-applied-text-check.md`.
Its verdict is `Verdict: FAIL`. Every edit is in its file word for word and nothing else changed (report §1). The FAIL
rests on two must-fix texts this record never edited, M-1 and M-2: present-tense sentences that v4 text in the same
contract now contradicts. The report adds eight should-fix points, S-1 to S-8. This record's search (Evidence E-3) had no
pattern for `VERIFIED universal` or for the call-log condition, which is how M-1 and M-2 were missed.

**What this amendment does.** It is insert-only in this record. It adds edits C-22 to C-30, D-20 to D-27, E-5, E-6 and
R-1d to the files, and completes the record's own "If the owner later answers yes" paragraph (S-7). It changes no ruling
of this record: v4, the boot-path ruling, the four part rulings, the K-list and option A stand, and so do the
orchestrator's rulings on the twelve scope concerns. Where an edit below replaces text an earlier edit of this record
wrote (C-25 replaces part of C-17; C-26 part of C-14; C-29 part of C-19; C-30 extends C-12; D-22 replaces part of D-4;
D-23 part of D-9; D-24 replaces D-11's test cell; D-27 extends D-7; E-5 part of E-1; E-6 part of E-3; R-1d part of R-1c),
the amendment's text governs for that part.

**No new version step.** The contract stays v4. The edits correct descriptions (M-1, M-2, S-3, S-5), remove a
contradiction between two new rules without changing what either requires (S-1: Rule 4a already required the cancel
under "Never"), tighten test rows (S-4), and state a fact already ruled (S-6, ruling 3). None changes a port signature,
a `BackupType` value, a `SchedulerState` variant, an invariant's clause or any value.

**Code facts checked for this amendment** (at `fd70325a`): `WorkManagerScheduler.kt:227-229` adds the call-log URI only
when `preferences.dataTypePreferences.isBackupEnabled(DataType.CALLLOG) && preferences.isCallLogBackupAfterCallEnabled()`;
`Preferences.java:135-137` shows `isCallLogBackupAfterCallEnabled()` reads only its own key (`CALLLOG_BACKUP_AFTER_CALL`,
default `false`); `DataTypePreferences.java:26-28` reads the data type's own backup-enabled key. The legacy job had the
same two conditions (`BackupJobs.java:180` at `d095fe02^`). The unique name and tag of the trigger are both
`CONTENT_TRIGGER_UNIQUE_NAME = "CONTENT_TRIGGER"` (`WorkManagerScheduler.kt:93`, `:238`, `:243`, `:405`); the legacy
constant was `CONTENT_TRIGGER_TAG = "contentTrigger"` (`BackupJobs.java:57` at `d095fe02^`). The port javadoc has the
same call-log gap as M-2 (`BackupScheduler.java:71-72`); it is code, handed to the fix story (below).

### Findings and dispositions

| Finding | Place (working tree) | Disposition | Edits |
|---------|----------------------|-------------|-------|
| **M-1** (must-fix): `existingPolicy … = REPLACE // VERIFIED universal` | contract `:194-195` | **Adopted as proposed** | C-23 |
| **M-2** (must-fix): the call-log condition given as `isCallLogBackupAfterCallEnabled()` alone | contract `:73-75`, `:224-225`; DES-005 `:318`, `:322` | **Adopted as proposed**, after verifying the code (above). The INV-4 clause (contract `:277`, report N-5) keeps its wording by this record's "INV-4 clause: Untouched" ruling; it goes to the housekeeping record of ruling 7. The port javadoc goes to the fix story | C-22, C-24, D-20, D-21 |
| **S-1**: Rule 4 forbids the cancel Rule 4a requires under "Never" | contract `:315` | **Adopted with changed text.** By the coordinator's ruling Rule 4a wins under "Never"; the replacement also names the upgrade case (a periodic `REGULAR` that an earlier build registered under "Never") and says which rule governs. The fix story gains a test obligation for the upgrade case (D-24 and the citation list) | C-25 |
| **S-2**: D-4 calls the trigger worker "stage 1"; D-10 and the worker's KDoc call its enqueue stage 2 | DES-005 `:264` | **Adopted as proposed** | D-22 |
| **S-3**: the trigger's name written three ways (`CONTENT_TRIGGER`, `CONTENT_TRIGGER_TAG`, `contentTrigger`) | contract `:422`; DES-005 `:443`, `:458` | **Adopted, extended.** Two further present-tense contract lines name the legacy constant as the current tag and unique name (`:152`, `:157`; the report read them as legacy description, report §2.2); by the coordinator's ruling the code's value governs present-tense text. Left as history: DES-005 `:136`, `:316` (they describe the legacy `contentTrigger` job), and the "Source of truth" column of DES-005 `:458` (`CONTENT_TRIGGER_TAG` (`:57`), a legacy citation) | C-27, C-28, C-29, D-25, D-26 |
| **S-4**: the D-9 and D-11 test rows can pass vacuously | DES-005 `:378`, `:382` | **Adopted with changed text.** Preconditions, observable results and the failing-at-`fd70325a` requirement as proposed. Changed: (a) the three sequences are spelt out and each must end with exactly one non-terminal `REGULAR` item that is **periodic**, so that every new INV-1 test fails on the code at `fd70325a` (as the report's wording stood, the single-flight test passes there, because today's `REPLACE` also leaves one entry); (b) all four INV-1 groups are required to fail at `fd70325a`, not only three (the "nothing registered" test also fails there: the boot path enqueues a one-time entry); (c) in the "Never" row, the interval tests (60, 899, 900 and the five offered values) pin unchanged behaviour and pass at `fd70325a` by design (BUG-026 AC-3 and AC-4 say so); the row says so and does not count them as gate evidence. Every test of the changed behaviour must fail there; (d) the upgrade case of S-1 is added | D-23, D-24 |
| **S-5**: K-1's "Affected here" omits three statements it covers | contract `:288` | **Adopted as proposed.** The upgrade case of S-1 falls under "Rules 4 and 4a", which K-1 already names | C-26 |
| **S-6**: no applied text says `RECEIVE_SMS` stays | DES-005 `:323` (D-7); contract `:279` (C-12) | **Adopted with changed placement.** As the coordinator ruled, the sentence goes into the contract's receiver note (C-12), not into K-2 (K-2 is deleted at X-2, the note stays), and into the DES-005 removal passage as proposed, with the static manifest check | C-30, D-27 |
| **S-7**: the record's "If the owner later answers yes" paragraph is incomplete | this record, "The product owner's open questions PO-1 and PO-2" | **Adopted.** Completed against the applied text and this amendment's edits (below). The original paragraph is kept and marked superseded | (record text) |
| **S-8**: "is removed" in the present tense while the receiver is still in the code | DES-014 `:1423`, `:1339`; REQ-005 `:90` | **Adopted as proposed** ("is to be removed by its own story (ruling 56)"). Checked the seven files for other present-tense removals: contract `:293` ("it is removed by its own story", the v4 changelog) states what the story does and is true before and after; the others are about the pre-24 branch or the U-001 design, not CR-017's | E-5, E-6, R-1d |
| N-1 to N-5 (notes) | — | No action here: N-1 and N-5 are left by this record's ruling that the INV-1 and INV-4 clauses are untouched; N-2 to N-5 go to the housekeeping record of ruling 7 | — |
| N-6 (record bookkeeping) | this record | The Metadata status row is corrected by this amendment. Execution, Commit Traceability and Closure stay for the orchestrator after the delta check | (record text) |

### New edits

"Old" was checked against the working-tree files on 2026-11-21 (`tr -d '\r' < file | grep -c -F`), each exactly once.
Apply by the quoted text, not by line number. `status: approved` and each file's `id` and `artifact_type` stay. The
three CRLF files (contract, DES-005, DES-008) keep their line endings.

#### CNTR-MODERNIZATION-004, edits C-22 to C-30

**C-22 (M-2). Interface, `scheduleContentTrigger()` comment.**
Old:
~~~text
      // recurring content-URI observer over SMS provider (+ call-log when
      // isCallLogBackupAfterCallEnabled()); available on every supported level
      // (minSdk 24). (BackupJobs.java:82)
~~~
New:
~~~text
      // recurring content-URI observer over SMS provider (+ call-log when
      // call-log backup is enabled and isCallLogBackupAfterCallEnabled());
      // available on every supported level (minSdk 24). (BackupJobs.java:82)
~~~

**C-23 (M-1). `SchedulerConfig`, `existingPolicy` field.**
Old:
~~~text
  existingPolicy: ExistingPolicy = REPLACE // VERIFIED universal
                                           // (setReplaceCurrent(true), :188)
~~~
New:
~~~text
  existingPolicy: ExistingPolicy = REPLACE // one-off work; periodic work (REGULAR,
                                           // CONTENT_TRIGGER) uses UPDATE (v4; enum below).
                                           // Legacy: setReplaceCurrent(true) on every job (:188)
~~~

**C-24 (M-2). `ContentUriTrigger.uri` comment.**
Old:
~~~text
  uri: String                              // SMS provider URI; + call-log URI when
                                           // isCallLogBackupAfterCallEnabled()
~~~
New:
~~~text
  uri: String                              // SMS provider URI; + call-log URI when
                                           // call-log backup is enabled and
                                           // isCallLogBackupAfterCallEnabled()
~~~
(The first old line is unique; the second is matched as the line directly after it.)

**C-25 (S-1). Validation Rule 4, the middle sentences (rest of the rule unchanged).**
Old:
~~~text
It MUST NOT enqueue a one-time work under `REGULAR`, MUST NOT cancel or reset a registered or running regular backup, and starts no separate boot backup. With a non-positive regular interval ("Never") it registers nothing (Rule 4a).
~~~
New:
~~~text
It MUST NOT enqueue a one-time work under `REGULAR` and starts no separate boot backup. With a positive regular interval it MUST NOT cancel or reset a registered or running regular backup. With a non-positive regular interval ("Never") it registers nothing and cancels an existing `REGULAR` work, including a periodic one that an earlier build registered under "Never"; there Rule 4a governs.
~~~

**C-26 (S-5). "Code not yet at v4", K-1, its last sentence.**
Old:
~~~text
Affected here: the `scheduleRegular()` and `scheduleBootup()` blocks, the `REGULAR` row of the unique-name table, the INV-1 realization, Rules 4 and 4a, and the regular example.
~~~
New:
~~~text
Affected here: the `scheduleRegular()` and `scheduleBootup()` blocks, the boot-path bullet of the `BackupType` notes, the boot comment of `initialDelaySecs`, the `REGULAR` fallback in the `ExistingPolicy` note, the `REGULAR` row of the unique-name table, the INV-1 realization, Rules 4 and 4a, and the regular example.
~~~

**C-27 (S-3). Enum `BackupType`, Notes, the content-trigger bullet, its second line.**
Old:
~~~text
    and is identified by a stable tag CONTENT_TRIGGER_TAG (BackupJobs.java:57),
~~~
New:
~~~text
    and is identified by the stable unique name and tag CONTENT_TRIGGER
    (WorkManagerScheduler.CONTENT_TRIGGER_UNIQUE_NAME; the legacy job's tag was
    CONTENT_TRIGGER_TAG = "contentTrigger", BackupJobs.java:57),
~~~

**C-28 (S-3). Enum `BackupType`, Notes, the unique-name bullet (substring of its second line; the rest of the line stays).**
Old:
~~~text
    the CONTENT_TRIGGER_TAG tag + a restore unique name.
~~~
New:
~~~text
    the CONTENT_TRIGGER unique name (also its tag) + a restore unique name.
~~~

**C-29 (S-3). Content-trigger example, the `uniqueName` line.**
Old:
~~~text
  uniqueName         = "CONTENT_TRIGGER",   // also its tag (CONTENT_TRIGGER_TAG)
~~~
New:
~~~text
  uniqueName         = "CONTENT_TRIGGER",   // also its tag (WorkManagerScheduler.CONTENT_TRIGGER_UNIQUE_NAME; the legacy job's tag was CONTENT_TRIGGER_TAG = "contentTrigger")
~~~

**C-30 (S-6). INV-4 note, after the receiver sentences of C-12.**
Old:
~~~text
`scheduleIncoming()` stays a port operation; its production caller is the content-trigger worker.
~~~
New:
~~~text
`scheduleIncoming()` stays a port operation; its production caller is the content-trigger worker. `RECEIVE_SMS` (`AndroidManifest.xml:17`) stays: the default-SMS `SMS_DELIVER` receiver (`compat/SmsReceiver`) needs it while the app holds the SMS role during a restore (CR-017, ruling 3); the story that removes the SMS-received receiver removes no permission.
~~~

#### DES-MODERNIZATION-005, edits D-20 to D-27

**D-20 (M-2). "Incoming-SMS trigger (AC-3d)", the legacy description.**
Old:
~~~text
call-log provider when call-log-after-call is enabled, `:180-181`)
~~~
New:
~~~text
call-log provider when call-log backup and call-log-after-call are both enabled, `:180-181`)
~~~

**D-21 (M-2). The same section, the content-URI trigger bullet.**
Old:
~~~text
plus the call-log URI under the same `isCallLogBackupAfterCallEnabled()` condition.
~~~
New:
~~~text
plus the call-log URI under the same condition as the legacy job: call-log backup enabled and `isCallLogBackupAfterCallEnabled()` (`BackupJobs.java:180`; CR-017).
~~~

**D-22 (S-2). Components table, the `BackupTriggerWorker` row (D-4), third cell.**
Old:
~~~text
stage 1 of the INV-4 debounce: runs on a content-URI change and calls `BackupScheduler.scheduleIncoming()`; never backs up itself
~~~
New:
~~~text
the content-URI-triggered worker of the INV-4 debounce: it runs on a content-URI change (stage 1) and calls `BackupScheduler.scheduleIncoming()` to enqueue the delayed incoming backup (stage 2); never backs up itself
~~~

**D-23 (S-4). Design Validation, INV-1 row, the sentences D-9 added.**
Old (from "For `REGULAR`" to the end of the cell, before " |"):
~~~text
For `REGULAR` (CNTR-MODERNIZATION-004 v4, Rule 4): after `scheduleRegular()`, `scheduleBootup()` leaves one periodic `ENQUEUED` entry with the same id; with nothing registered, `scheduleBootup()` registers one periodic entry with a 60 s initial delay and the regular network constraint; a one-time entry under `REGULAR` (`ENQUEUED` or `CANCELLED`) does not block `scheduleRegular()` or `scheduleBootup()`; at most one non-terminal `REGULAR`-tagged item exists after any sequence of the two and `cancelAll()`. These tests are added by the story that fixes BUG-026 and BUG-027 (BUG-027 AC-1 to AC-5).
~~~
New:
~~~text
For `REGULAR` (CNTR-MODERNIZATION-004 v4, Rule 4), with auto backup on, encryption not degraded and `regularTimeoutSecs` 3600: (1) after `scheduleRegular()`, `scheduleBootup()` leaves exactly one entry under `REGULAR`, periodic and `ENQUEUED`, with the same `id`, a repeat interval of 3 600 000 ms and a `generation` raised by exactly one; (2) with nothing registered, `scheduleBootup()` leaves exactly one periodic `ENQUEUED` entry with the configured interval, `initialDelayMillis == 60000`, the regular network constraint (for both `isWifiOnly()` values) and the tag `REGULAR`; (3) with a one-time `BackupWorker` entry seeded under `REGULAR` directly through WorkManager, in `ENQUEUED` and in `CANCELLED` state, each of `scheduleRegular()` and `scheduleBootup()` leaves exactly one entry under `REGULAR`, periodic and `ENQUEUED`; (4) after each of the sequences `scheduleRegular()`, `scheduleBootup()`; `scheduleBootup()`, `scheduleRegular()`; and `scheduleRegular()`, `scheduleBootup()`, `cancelAll()`, `scheduleRegular()`, `getWorkInfosByTag("REGULAR")` holds exactly one non-terminal item, it is periodic and it is under `REGULAR`, and no other unique name holds a `REGULAR`-tagged item. These tests are added by the story that fixes BUG-026 and BUG-027 (BUG-027 AC-1 to AC-5); each of (1) to (4) fails on the scheduler code at commit `fd70325a`, and the story records that run.
~~~

**D-24 (S-4, S-1). Design Validation, the "Never" row (D-11), its test cell.**
Old (from "With `regularTimeoutSecs`" to the end of the cell, before " |"):
~~~text
With `regularTimeoutSecs` of −1 and of 0, `scheduleRegular()` returns no job and leaves no non-terminal `REGULAR` entry, also after an earlier registration with 3600; `scheduleBootup()` with −1 or 0 does the same; a positive value below 900 (60, 899) and 900 give a repeat interval of 900 000 ms; each offered value (1800, 3600, 7200, 21600, 86400) gives its own interval. Added by the story that fixes BUG-026 and BUG-027 (BUG-026 AC-1 to AC-4; BUG-027 AC-11).
~~~
New:
~~~text
With auto backup on and encryption not degraded: (1) with `regularTimeoutSecs` of −1 and of 0, `scheduleRegular()` returns `null` and leaves no non-terminal entry under `REGULAR`, also after an earlier registration with 3600 and without a `cancelAll()` in between; (2) `scheduleBootup()` with −1 or 0 does the same; (3) the upgrade case: with a periodic `BackupWorker` entry seeded under `REGULAR` with a 900 s interval and tag `REGULAR` (what the code at `fd70325a` registers for "Never") and `regularTimeoutSecs` of −1, each of `scheduleBootup()` (the next boot) and `scheduleRegular()` (the next settings event) leaves no non-terminal entry under `REGULAR`. Each test of (1) to (3) fails on the scheduler code at `fd70325a`, and the story records that run. Guards of unchanged behaviour, which pass at `fd70325a` by design (BUG-026 AC-3, AC-4) and are not gate evidence: a positive value below 900 (60, 899) and 900 give `repeatIntervalMillis` 900 000; each offered value (1800, 3600, 7200, 21600, 86400) gives its own interval in milliseconds. Added by the story that fixes BUG-026 and BUG-027 (BUG-026 AC-1 to AC-4; BUG-027 AC-11).
~~~

**D-25 (S-3). Port table, `cancelAll()` row, third cell.**
Old:
~~~text
`cancelUniqueWork(REGULAR)` + `cancelUniqueWork(contentTrigger)`
~~~
New:
~~~text
`cancelUniqueWork(REGULAR)` + `cancelUniqueWork(CONTENT_TRIGGER)`
~~~

**D-26 (S-3). Value-object table, `uniqueName` row, second cell (substring; the third cell stays).**
Old:
~~~text
+ `contentTrigger` tag + restore; the boot path uses
~~~
New:
~~~text
+ `CONTENT_TRIGGER` (unique name and tag) + restore; the boot path uses
~~~

**D-27 (S-6). "Incoming-SMS trigger (AC-3d)", the "No SMS-received path" bullet (D-7), after its third sentence.**
Old:
~~~text
The story that removes it deletes the class, its manifest entry, its enable/disable in `App` and its test.
~~~
New:
~~~text
The story that removes it deletes the class, its manifest entry, its enable/disable in `App` and its test. `RECEIVE_SMS` (`AndroidManifest.xml:17`) stays: the default-SMS `SMS_DELIVER` receiver (`compat/SmsReceiver`) needs it while the app holds the SMS role during a restore (CR-017, ruling 3). The story checks, by static inspection of the merged manifest, that no `<receiver>` for `SMS_RECEIVED` remains and that `RECEIVE_SMS` is still declared.
~~~

#### DES-MODERNIZATION-014, edits E-5 and E-6

**E-5 (S-8). D14, Run R (E-1's parenthesis).**
Old:
~~~text
is not delivered the broadcast and is removed, CR-017)
~~~
New:
~~~text
is not delivered the broadcast and is to be removed by its own story (ruling 56), CR-017)
~~~

**E-6 (S-8). D14, the OTP paragraph (E-3's parenthesis).**
Old:
~~~text
(the SMS-received receiver is removed, CR-017)
~~~
New:
~~~text
(the SMS-received receiver is to be removed by its own story (ruling 56), CR-017)
~~~

#### REQ-MODERNIZATION-005, edit R-1d

**R-1d (S-8). The CR-017 note (R-1c), first sentence.**
Old:
~~~text
has not been delivered the broadcast since U-001 and is removed.
~~~
New:
~~~text
has not been delivered the broadcast since U-001 and is to be removed by its own story (ruling 56).
~~~

### S-7: the completed "If the owner later answers yes" paragraph (record text)

This paragraph supersedes the one under "The product owner's open questions PO-1 and PO-2". It is written against the
applied text including C-22 to C-30 and D-20 to D-27. Each quoted phrase is the present wording. Before applying a
"yes": re-check that each quoted phrase is still unique in its file, and re-run the searches for `one-off`, `one-time`,
`BOOT`, `separate boot backup` and `no unique name` over the seven files; any further hit is added to that amendment.

> *PO-1 "yes"* (one backup after a restart when the regular schedule is "Never"). Ruling: non-breaking (a new one-off
> kind under its own name; INV-1 holds per name; under "Never" no `REGULAR` exists, so the two cannot overlap). Contract
> step v5.
> 1. **Contract, `scheduleBootup()` block (C-3):** "the same "Never" rule" becomes "under "Never": cancels `REGULAR`, then
>    enqueues one one-time `BackupWorker` under the unique name `BOOT` (`REPLACE`, initial delay 60s, network per
>    isWifiOnly(), EXPONENTIAL 30s, tag `REGULAR` so the worker infers type `REGULAR`)"; "Never enqueues a one-time
>    REGULAR work and starts no separate boot backup" becomes "Never enqueues a one-time work under the unique name
>    REGULAR; starts no separate boot backup except the `BOOT` one under "Never" (PO-1)".
> 2. **Contract, `BackupType` notes, boot bullet (C-4):** the whole bullet becomes "The boot path registers the REGULAR
>    periodic work, because it is the regular backup; under "Never" it enqueues one one-time backup under its own unique
>    name BOOT (PO-1)."
> 3. **Contract, unique-name table (C-5):** a `BOOT` row is **added** after the `INCOMING` row: "| `BOOT` | `BackupWorker`,
>    tag `REGULAR` | one-off, initial delay 60s; network per `isWifiOnly()`; `EXPONENTIAL` 30s | `REPLACE` |
>    `scheduleBootup()`, only when `getRegularTimeoutSecs() <= 0` (PO-1) |"; the sentence "No `BOOT` unique name is used."
>    becomes "`BOOT` is used only under "Never" (PO-1)."
> 4. **Contract, `initialDelaySecs` (C-7):** "first-run delay of a new REGULAR registration (v4)" gains "; and the delay of
>    the one-time BOOT backup under "Never" (PO-1)".
> 5. **Contract, INV-1 realization (C-10):** after "never enqueues a one-off under `REGULAR` (v4, CR-017)" add "; under
>    "Never" it enqueues one one-off under `BOOT` (PO-1)".
> 6. **Contract, Rule 4 as amended by C-25:** "It MUST NOT enqueue a one-time work under `REGULAR` and starts no separate
>    boot backup." becomes "It MUST NOT enqueue a one-time work under `REGULAR`, and starts no separate boot backup except
>    under "Never"."; "it registers nothing and cancels an existing `REGULAR` work, including a periodic one that an earlier
>    build registered under "Never"; there Rule 4a governs." becomes "it registers no regular backup, cancels an existing
>    `REGULAR` work (including a periodic one that an earlier build registered under "Never") and enqueues the one-time
>    `BOOT` backup; Rule 4a governs `REGULAR`." Rule 4a stays as it is (it speaks only of the regular backup).
> 7. **Contract, Versioning (C-13) and Changelog (C-15):** the Current-version line becomes v5; a v5 entry records the
>    change. The v4 entry ("v4 states the answer "no" to both") stays as history.
> 8. **Contract, K-list (C-14, C-26):** unchanged if the fix story is not yet closed; if it is, nothing (K-1 is gone).
> 9. **DES-005:** D-2 "(no one-off)" becomes "(no one-off except, under "Never", one `BOOT` one-off)"; D-3 "The boot path
>    registers the regular `PeriodicWorkRequest`, not a one-off (CR-017)" gains "; under "Never" it enqueues one `BOOT`
>    one-off (PO-1)"; D-6 "and enqueues no one-off (CR-017)" becomes "and, only under "Never", one `BOOT` one-off (CR-017;
>    PO-1)"; D-13 "no one-off, no separate boot backup; "Never" registers nothing" becomes "no one-off and no separate boot
>    backup except, under "Never", one `BOOT` one-off; "Never" registers no regular backup"; D-16 "the boot path uses
>    `REGULAR`, because it registers the regular backup" gains "; under "Never", `BOOT`"; D-17 gains "; also the delay of
>    the `BOOT` one-off under "Never""; D-24 (the "Never" row) (2) becomes "`scheduleBootup()` with −1 or 0 leaves no
>    non-terminal entry under `REGULAR` and exactly one `ENQUEUED` one-off under `BOOT` with `initialDelayMillis == 60000`".
> 10. **REQ-005, R-1c:** a dated note: "Amended by CR-0nn: under "Never" a restart starts one backup (PO-1, answered yes)."
>
> *PO-2 "yes"* (a backup about a minute after every restart). Ruling: a behavioural break of the boot path (an added
> backup), gated by a test of the guard below. Contract step v5 (or v6 if PO-1 was answered first).
> 1. Items 1 to 6 and 9 above, with "under "Never"" / "only when `getRegularTimeoutSecs() <= 0`" replaced by "at every
>    boot", and in item 6 the positive-interval sentence of Rule 4 kept and extended: "With a positive regular interval it
>    MUST NOT cancel or reset a registered or running regular backup, and enqueues the one-time `BOOT` backup, which never
>    starts while a `REGULAR` run is enqueued or running."
> 2. INV-1 realization (C-10): add "A `BOOT` run never starts while a `REGULAR` run is enqueued or running; the adapter
>    guards this (the guard is designed by the planner, BUG-027 PO-2)."
> 3. C-15 item 2's sentence "with a registered regular backup, a restart no longer starts an extra backup a minute later"
>    becomes history; the v5 (or v6) entry says the extra backup is back, by the owner's decision.
> 4. (Replaced by Amendment 2, DS-1.) DES-005 D-9/D-23: the INV-1 row gains the guard test, and in (4) "…holds exactly
>    one non-terminal item, it is periodic and it is under `REGULAR`, and no other unique name holds a `REGULAR`-tagged
>    item" becomes "…holds exactly one non-terminal periodic item, under `REGULAR`, and at most one other non-terminal
>    `REGULAR`-tagged item, the one-time `BOOT` backup under the unique name `BOOT`"; BUG-027 AC-4 follows. D-18 (Risks)
>    gains "two backups of the same items when `BOOT` and `REGULAR` overlap", mitigated by the guard.
> 5. REQ-005, R-1c: "The extra backup about a minute after every restart … is not kept unless the product owner decides
>    otherwise" becomes false; a dated note records the decision.

### Changes to obligations elsewhere in this record

- **The fix story** (list "What the story that fixes BUG-026 and BUG-027 must cite"): item 1 adds C-25 (Rule 4 as
  amended) and C-26 (K-1 as amended); item 2 is read with D-23 and D-24, which replace the test descriptions: the four
  INV-1 groups and the "Never" tests (1) to (3), **including the upgrade case** (a periodic `REGULAR` registered by the
  code at `fd70325a` under "Never" is cancelled at the next `scheduleBootup()` and the next `scheduleRegular()`), must
  each fail on the scheduler code at `fd70325a`, and the story records that run; the interval tests are guards. Item 5
  adds the port javadoc's call-log condition (`BackupScheduler.java:71-72`: "and the call-log URI when
  `isCallLogBackupAfterCallEnabled()`"), which must say "when call-log backup is enabled and
  `isCallLogBackupAfterCallEnabled()`" (M-2's code hand-over).
- **The receiver-removal story** (list "What the story that removes the SMS-received receiver must cite"): item 1 adds
  C-30 and D-27; item 3 adds the static check of the merged manifest (no `<receiver>` for `SMS_RECEIVED`, `RECEIVE_SMS`
  still declared).
- **Edit counts:** the record's edits are now 77 file edits in 7 files (contract 31: C-0 to C-30; DES-005 28: D-0 to
  D-27; DES-008 3; DES-014 7: E-0 to E-6; DES-001 2; REQ-005 4: R-1a to R-1d; REQ-001 2), plus X-1 and X-2.

### Applier's check, added

Re-run the record's verification searches with these patterns over the contract and DES-005: `VERIFIED universal`
(expected: no match), `isCallLogBackupAfterCallEnabled` (every hit is paired with "call-log backup is enabled" or "both
enabled", except the INV-4 clause `:277`, which says "when call-log-after-call is enabled" and is left by ruling), `contentTrigger`
(expected only in DES-005 `:136`, `:316`, and in the legacy quotations of C-27 and C-29), `CONTENT_TRIGGER_TAG`
(expected only in legacy citations), `stage 1` (in DES-005 only with "(stage 1)" as D-22 writes it, and in history), and
`is removed` over DES-014 and REQ-005 (expected: no hit about the SMS-received receiver). Check that Rule 4 no longer says
"MUST NOT cancel or reset a registered or running regular backup" without "With a positive regular interval" before it.
Then a delta check by the Design Auditor; its verdict is added to Evidence E-4.

---

## Amendment 2 (2026-11-21): the foreground sentence of DES-014 row 16-1, DS-1, one wording

**Source.** The delta check of Amendment 1 passed ("Verdict after Amendment 1: PASS", end of
`sdlc/artifacts/build/change-records/evidence/CR-017-applied-text-check.md`), with DS-1 and an optional wording point
open. Separately, the contract-verification gate of story U-075 found that an approved design sentence states that
scheduled long work runs as a `dataSync` foreground service (`sdlc/artifacts/build/sprints/sprint-024/U-075/contract-verification.md`,
B-1). That contradicts this record's Evidence E-3 ("No document states that automatic backups run in the foreground"),
which is corrected below. Insert-only, except item 4 of PO-2 in the S-7 paragraph, which is replaced (DS-1). No ruling
of this record changes, and the contract stays v4 (no contract text is edited).

**The fact (from U-075's plan, `sdlc/artifacts/build/sprints/sprint-024/U-075/plan.md`, section 2.1 and its rulings table).**
Today every backup run asks WorkManager for the foreground, and on Android 12 and later a start the system refuses while
the app is off screen fails the run (BUG-029; device record Run 1 and Run 2). From story U-075 (sprint 024, wave 4,
ruling 60), foreground promotion is kept for `MANUAL`, `SKIP` and every restore, and dropped for `INCOMING`, `REGULAR`,
`BROADCAST_INTENT` and a run without a type tag (plan `:154-155`); a refused promotion is not a failure: the run continues
without it (plan `:71`, `:155`).

### Findings and dispositions

| Finding | Place (working tree) | Disposition | Edits |
|---------|----------------------|-------------|-------|
| **U-075 B-1**: "WorkManager runs on JobScheduler; long work runs as a dataSync foreground service" | DES-014 `:1282`, row 16-1, third cell | **Adopted.** One cell, true today and after U-075 by this record's convention (it names the story); the "today" half is dropped by close-out edit X-3 when U-075 closes. The row's other cells stay | E-7, X-3 |
| Evidence E-3, the sentence "No document states that automatic backups run in the foreground …" | this record | **Corrected** by a dated note under that sentence (record text) | (record text) |
| **DS-1** (should-fix): PO-2 item 4 misses that the `BOOT` backup's tag `REGULAR` makes D-23 (4) false | this record, S-7 paragraph, PO-2 item 4 | **Adopted with one clarification**: "the one-time `BOOT` backup" becomes "the one-time `BOOT` backup under the unique name `BOOT`", so the second item's name is stated, not only its kind | (record text) |
| **Point 3** (optional): DES-014 `:56` "and is removed by its own story (product owner's" | DES-014 `:56` (the E-4 note) | **Adopted** for uniformity with E-5, E-6 and R-1d | E-8 |

### New edits (DES-MODERNIZATION-014)

"Old" was checked against the working-tree file (`tr -d '\r' < file | grep -c -F`), each exactly once.

**E-7 (U-075 B-1). Android 16 behaviour-change table, row 16-1, third cell.**
Old:
~~~text
WorkManager runs on JobScheduler; long work runs as a dataSync foreground service
~~~
New:
~~~text
WorkManager runs on JobScheduler. Today every backup asks for a `dataSync` foreground service, and a start the system refuses (off screen, Android 12 and later) fails the run (BUG-029). From story U-075, automatic backups (incoming, regular, after a restart, the `BACKUP` broadcast) run as ordinary jobs under the job quota, and only work the user starts (manual backup, Skip, restore) asks for the foreground; a refused start continues without it (CR-017, Amendment 2)
~~~

**E-8 (point 3). The CR-017 note at the top (E-4).**
Old:
~~~text
and is removed by its own story (product owner's
~~~
New:
~~~text
and is to be removed by its own story (product owner's
~~~

### Close-out edit X-3 (applied by the orchestrator when story U-075 closes)

| # | When | File and place | Old | New |
|---|------|----------------|-----|-----|
| X-3 | Story U-075 closes | DES-014, row 16-1, third cell (as written by E-7) | the whole cell text of E-7 | "WorkManager runs on JobScheduler. Automatic backups (incoming, regular, after a restart, the `BACKUP` broadcast) run as ordinary jobs under the job quota, and only work the user starts (manual backup, Skip, restore) asks for a `dataSync` foreground service; a refused start continues without it (U-075; CR-017, Amendment 2)" |

No version step (a design cell). X-3 is independent of X-1 and X-2.

### The foreground search (what else was checked)

`git grep -n -i -e "foreground" -- sdlc/artifacts/design sdlc/artifacts/requirements` on the working tree: 112 lines in
14 files (CNTR-004 1, DES-001 14, DES-005 3, DES-007 2, DES-008 1, DES-012 43, DES-013 1, DES-014 13, EPIC-001 1, REQ-001 8,
REQ-005 1, REQ-013 11, REQ-016 6, REQ-017 7).
Every present-tense sentence about who asks for the foreground was read; U-075's gate statement table (29 rows) was
used as a cross-check, not copied. Result: **row 16-1 is the only present-tense sentence that says automatic or
scheduled work runs in the foreground or shows the progress notification.** The rest:

| Place | Statement | Disposition |
|-------|-----------|-------------|
| CNTR-004 `:324` (Rule 8) | the manual notification/foreground semantics of `scheduleManual` | True after U-075 (kept for `MANUAL`, `SKIP`) |
| DES-005 `:55`, `:130-132`; REQ-005 `:42` | WorkManager "owns … foreground promotion"; "where a user-visible notification is required it uses `setForeground`" | True: a capability, and conditional |
| DES-014 AC4-a (`:1305`), `:1918` (017 AC-9 "DV-2 foreground worker"), REQ-017 AC-9 (`:129-131`) | a **manual** backup starts the foreground worker and shows its notification | True after U-075 |
| REQ-016 `:49-53` | "`BackupWorker.kt` and `RestoreWorker.kt` call `setForeground()`" | True after U-075 (still called, for user-started work); a description for the manifest check |
| REQ-016 AC-13 (`:118-121`) | a scheduled backup runs once with no `ForegroundServiceStartNotAllowedException` in the log | A criterion; it can fail today off screen (BUG-029) and is satisfiable after U-075, which U-073 follows (ruling 60) |
| DES-014 `:320`, `:1595`, `:1643`, `:1667`, `:1903`, `:2043`; DES-013 `:307`; REQ-016 `:103-104`; REQ-017 `:59-60`, `:102-103`, `:155` | the `SystemForegroundService` manifest override (BUG-016), lint categories, log patterns, the library risk | True; about the manifest and checks, not about which work asks |
| DES-014 `:1359`, `:1487` | "foreground commands", "wait in the foreground" (the operator) | Unrelated |
| DES-014 `:1109` (the WorkManager 2.12.0 release-note row), `:1297` (row 37-7, the notification `PendingIntent`) | library notes; a tapped notification | True; not about which work asks |
| DES-001 `:39-253`; REQ-001 `:37-129`; EPIC-001 `:74`; REQ-013 `:56-170`; DES-007 `:272-273`; DES-008 `:187`; DES-012 `:56-991` | the foreground types and promotion of the retired `SmsBackupService`/`SmsRestoreService` and of the manual path | History of completed designs and requirements; left |

### Counts after Amendment 2

79 file edits in 7 files (contract 31; DES-005 28; DES-008 3; DES-014 9: E-0 to E-8; DES-001 2; REQ-005 4; REQ-001 2),
plus close-out edits X-1, X-2 and X-3. A quick check of E-7 and E-8 follows; its verdict goes to Evidence E-4.

---

## Order relative to the code

**State today (`fd70325a`).** Subject 3 is already true in the code (U-069 is done: `BackupTriggerWorker.kt:51-71`,
`WorkManagerScheduler.kt:218-250`). Subjects 1 and 2 are not: the code clamps "Never" and the boot path deletes the
regular backup. Subject 4 is half true: the receiver is not an incoming path in fact, but it is still in the code.

**Which statements hold only after which story:**

| Statements | True now? | True from | How the applied text marks it |
|------------|-----------|-----------|-------------------------------|
| C-6, C-8, C-9 (trigger parts), C-16, C-19, D-4, D-8, D-14, H-1 | Yes | now | Nothing to mark |
| C-2, C-3, C-4, C-5 (`REGULAR` row), C-7, C-9 (`REGULAR` fallback), C-10, C-17, C-18; D-1 to D-3, D-5, D-6 (boot part), D-12, D-13, D-15 to D-17 | No (BUG-026, BUG-027) | the story that fixes BUG-026 and BUG-027 (after U-072, before U-073; ruling 55; U-075 before it, ruling 60) | Normative text states the target; item K-1 of "Code not yet at v4" (C-14) names the gap; DES-005's note (D-19) points to it |
| D-9, D-11 (the tests) | The tests do not exist yet | the same story | Worded "These tests are added by the story that fixes BUG-026 and BUG-027", true before and after |
| C-1, C-11, C-12, C-19 (last line), D-7, D-10, E-1 to E-4, P-1, R-1b, R-1c, R-2b | Yes as worded: the receiver is not an incoming path in fact, and each sentence about the deletion says what "the story that removes it" does | the receiver-removal story for the deletion itself (after U-072, before U-073; ruling 56) | Item K-2 names the code still present |
| X-1, X-2 | — | the close of each story | Applied then by the orchestrator |

No sentence of the applied text is false after both stories and X-1, X-2, and none needs a second edit then.

**Recommendation: apply the text edits now, in their own commit, before either story is written (option A, as CR-015).**

- Both stories must cite the contract they are built against (rulings 55 and 56: "one change record"; the sprint-025
  plan expects them "with its change record"). If the text waited for the code, the stories' plans would cite a contract
  that says the opposite (v3 Rule 4, v3 INV-4 note), which is the silent revision the process exists to avoid.
- The example correction (subject 3) is true now and is the U-069 close-out item (U-069 `:226`, `:278`).
- With K-1 and K-2 the interval is visible, not silent. A contract ahead of its code is this project's normal state for
  approved but unbuilt work (CR-015, "Order", point 5).

**Option B, not recommended:** apply each part with its story. It splits one record into three commits, leaves v3's
false example in place until then, and makes the two story plans review an unratified ruling.

**Order between the two stories.** They share no file: the fix story edits `WorkManagerScheduler.kt` and
`WorkManagerSchedulerTest.kt`; the receiver story edits `SmsBroadcastReceiver.java`, its test, `AndroidManifest.xml`,
`App.java` and a javadoc line of `BackupScheduler.java`. BUG-027 AC-7 requires empty diffs for the receiver story's files
in the fix story, and the receiver story must not touch the scheduler. Either order works; recommended: the fix story
first (BUG-027 is priority high and affects the owner after every restart), the receiver story second. Not concurrently
if both edit `BackupScheduler.java` javadoc (fix story `:36`, `:58-92`; receiver story `:32-33`): the planner of the second
re-reads its start state. Each story's test count is relative to its own start (REQ-MODERNIZATION-015 AC-13 as amended
by CR-016).

Stage only the seven artifact files and this record in the commit that applies it; `app/` has a developer's uncommitted
work.

---

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->

Not written yet. The record's author recommends that the Solution Architect checks, before approval: (1) the boot-path
ruling (Part 2) and the scoping sentence of Rule 2 (C-16); (2) that C-3 and C-17 do not over-promise what `UPDATE` does
with the initial delay of an existing registration (BUG-027, "Not verified" 5); (3) the `RECEIVE_SMS` finding (Scope
Concern 3).

---

## Cascade Analysis

**Root Layer:** Design and contracts (CNTR-MODERNIZATION-004, DES-MODERNIZATION-005).
**Root Defect:** The contract omits the legacy "Never" rule and tolerates a one-time boot job under the periodic name;
the design misreads the legacy `inSeconds <= 0` case and specifies the one-time boot shape; the example contradicts the
code; the receiver the owner removed is still described as a path.
**Cascade Direction:** Full downstream for subjects 1 and 2 (design → code → tests); design-only for subject 3 (the code is
right); decision-driven for subject 4 (owner → contract and design → code and tests).

### Propagation Path

| Step | From Layer | To Layer | Propagated? | Evidence |
|------|-----------|----------|-------------|----------|
| 1 | Design (DES-005 `:121`, `:280`, `:437`) | Code | Yes | `WorkManagerScheduler.kt:180`, `:273-281` |
| 2 | Code | Tests | Yes, untested | No test of "Never", of periodic-then-boot or of a one-time entry under `REGULAR` (BUG-026 item 8, BUG-027 item 9) |
| 3 | Contract example (`:369-390`) | Code | No | The code never followed the example (E-1) |
| 4 | Manifest (U-001) | Contract and design | Yes, stale description | CNTR-004 `:243`, `:245`, `:387-389`; DES-005 `:321`, `:379`; REQ-005 `:87` |
| 5 | Design and contracts | Documentation | Partly | `README.md:304-305` false after a restart (BUG-027); `README.md:298-302` (BUG-025, separate) |
| 6 | Design and contracts | Requirements | Notes only | REQ-005 `:87`; REQ-001 `:94`, `:106` |

### Fix Order

1. [ ] Design and contracts: amend CNTR-MODERNIZATION-004 (C-0 to C-21, v4) — ROOT FIX
2. [ ] Design and contracts: amend DES-MODERNIZATION-005 (D-0 to D-19), DES-MODERNIZATION-008 (H-0 to H-2), DES-MODERNIZATION-014 (E-0 to E-4), DES-MODERNIZATION-001 (P-0, P-1) — CASCADE FIX
3. [ ] Requirements: notes on REQ-MODERNIZATION-005 (R-1a to R-1c) and REQ-MODERNIZATION-001 (R-2a, R-2b) — CASCADE FIX
4. [ ] Design Auditor: applied-text check of the seven files against this record (as CR-015's check report did)
5. [ ] Stories: the fix story (BUG-026 + BUG-027) and the receiver-removal story are written citing CR-017 and v4 — CASCADE
6. [ ] Code and tests: the two stories — CASCADE (not this record)
7. [ ] Close-out: X-1 and X-2 when the stories close
8. [ ] Verification (below)

### Affected In-Progress Work

| Story | Sprint | Phase | Action |
|-------|--------|-------|--------|
| U-075 (BUG-029) | sprint-024, wave 4 | in progress | None: edits `BackupWorker.kt` and a new helper; no edited statement is about it |
| U-071, U-072, U-074 | sprint-025 | planned | None: no scheduler or receiver file |
| U-073 (runtime libraries) | sprint-025 | planned | Plans after both stories; its "existing tests unedited" baseline then includes the new tests and excludes `SmsBroadcastReceiverTest` |
| The fix story (BUG-026 + BUG-027), no ID | sprint-025 (expected) | not written | Written after this record is applied; cites it |
| The receiver-removal story, no ID | sprint-025 (expected) | not written | Same |
| BUG-025's text-only story, no ID | sprint-025 (expected) | not written | None from this record (no contract text promises a latency) |

---

## Plan

- **Execution Window:** Inline. A text amendment applied right after approval and before the two stories are written. **Not a story to schedule.**
- **Assigned To:** Solution Architect (contract and designs), Requirements Analyst (requirement notes); Design Auditor (check)
- **Estimated Effort:** about 2 hours (57 changes in 7 files), plus the check; Amendment 1 adds 20 changes (77 in all), plus the delta check; Amendment 2 adds 2 (79 in all), plus a quick check
- **Sprint:** None

### Fix Checklist

- [x] Design and contracts: CNTR-MODERNIZATION-004 C-0 to C-21 (v4) (applied to the working tree 2026-11-21, uncommitted)
- [x] Design and contracts: DES-MODERNIZATION-005 D-0 to D-19 (same)
- [x] Design and contracts: DES-MODERNIZATION-008 H-0 to H-2 (same)
- [x] Design and contracts: DES-MODERNIZATION-014 E-0 to E-4 (same)
- [x] Design and contracts: DES-MODERNIZATION-001 P-0, P-1 (same)
- [x] Requirements: REQ-MODERNIZATION-005 R-1a to R-1c; REQ-MODERNIZATION-001 R-2a, R-2b (no criterion changed) (same)
- [x] Orchestrator: rule on Part 2 (behavioural break of the boot path, recommended) and the version step (v4) before the edits are applied (ruled as recommended; "Orchestrator rulings on the scope concerns")
- [x] Design Auditor: applied-text check; FAIL items become an amendment of this record, as CR-015's Amendment 1 (FAIL, M-1, M-2, S-1 to S-8: Amendment 1)
- [x] Amendment 1: contract C-22 to C-30; DES-005 D-20 to D-27; DES-014 E-5, E-6; REQ-005 R-1d
- [x] Design Auditor: delta check of Amendment 1 (PASS; Evidence E-4)
- [ ] Amendment 2: DES-014 E-7, E-8 (and the record-text changes: DS-1, the E-3 correction)
- [ ] Quick check of Amendment 2 (verdict into Evidence E-4)
- [ ] Close-out X-3 when story U-075 closes
- [ ] The fix story and the receiver-removal story cite CR-017 and v4 (the two lists under Impact Analysis)
- [ ] Close-out X-1 and X-2 at each story's close
- [ ] Verification: the searches below return only the intended text

### Next steps (High severity)

The create-cr wording for High ("Run `/amp:create-stories --from-cr CR-017` to create a repair story") does not apply as
written: the code work is already two stories by rulings 55 and 56, expected in sprint 025. Apply this record, have it
checked, then write those two stories from BUG-026 + BUG-027 and from ruling 56, each citing this record.

### Scope Concerns

1. **Scope Concern: the boot-path ruling.** Recommended: a behavioural break of the boot path, non-breaking for
   consumers, v4, gated by the INV-1 boot tests. Alternative: "not a break" (INV-1's clause holds and its realization
   already allows `UPDATE` for periodic work); then only the labels in C-13 and C-15 change. Recommendation: break,
   because a user-visible effect (no backup a minute after a restart) changes on purpose and the policy exists to make
   that explicit.
2. **Scope Concern: PO-1 and PO-2 are open.** The text states "no" to both, as the orchestrator ruled (Record 20,
   `:2479`). The "If the owner later answers yes" paragraph lists every sentence that changes. Recommendation: keep "no"
   until the owner answers; put both to the owner with the next batch of questions; a "yes" is a small amendment (PO-1:
   non-breaking; PO-2: a new version, behavioural).
3. **Scope Concern: `RECEIVE_SMS` stays, on a recalled platform rule.** The `SMS_DELIVER` receiver needs it during a
   restore (Evidence E-1). That the platform requires `RECEIVE_SMS` for `SMS_DELIVER` delivery is recalled, not fetched.
   Recommendation: the receiver-removal story keeps the permission and records the reason; if the orchestrator wants it
   confirmed, the story may run a restore on the Android 16 emulator (allowed by Record 20, PO-G) with an SMS injected
   during the role hand-over and read `adb shell dumpsys package com.zegoggles.smssync` for the grant; removing the
   permission would need its own record.
4. **Scope Concern: the receiver package's coverage floor has no headroom.** `com.zegoggles.smssync.receiver*` LINE ≥ 0.50,
   measured 50.0% (29/58) at U-060 (`app/build.gradle:521-534`). Deleting a tested class may push the remainder
   (`BootReceiver`, `PackageReplacedReceiver`, `BackupBroadcastReceiver`) below the floor; whether it does was not
   computed. Recommendation: the story measures; if it breaches, it adds tests for the remaining receivers and does not
   lower the floor; lowering it needs an orchestrator ruling recorded in the story.
5. **Scope Concern: notes on two closed U-001 artifacts (P-1 on DES-001, R-2 on REQ-001).** Their rationale for the
   receiver's export value is the root of the receiver defect. Recommendation: apply the notes (they change no value and
   stop a reader from restoring a non-exported SMS receiver on the same reasoning). Alternative: leave both as history
   and drop P-0, P-1, R-2a, R-2b.
6. **Scope Concern: DES-014 Run R (E-1 to E-4).** This closes BUG-023's hand-over (a) and U-069's SC-7 for Run R.
   Recommendation: apply; the S5 measurement reads the trigger line only.
7. **Scope Concern: statements in CNTR-004 that are false today for reasons outside the four subjects.**
   (a) `:24` "Draft; not finalized." (the status is `approved`); (b) `:42`, `:267`, `:400`: `LegacyScheduler` and
   `CompositeScheduler` (no such class under `app/src` at `fd70325a`; `WorkManagerScheduler.kt:64-65` says they were
   deleted); (c) `:59-103`: operations return `Unit` and `observe` returns `Flow<SchedulerState>`, but the port returns
   `ScheduledJob?` and `SchedulerObservable` (`BackupScheduler.java:36-38`, `:55-56`, `:67-68`, `:80-81`, `:91-92`);
   (d) `:410`: the javadoc "currently reads nine", but it reads "ten" (`BackupScheduler.java:36`); (e) `:46`, `:48`:
   `BackupJobs` callers (deleted by U-017). Recommendation: a separate small housekeeping record after the two stories,
   so that v4's ruling is not mixed with unrelated corrections; if the orchestrator prefers to fold (a) and (d) in now,
   the replacements are: `:24` drop " Draft; not finalized."; `:410` replace the bullet by "- **v2 implementation note:**
   done; the port javadoc names ten operations (`BackupScheduler.java:36`)." Neither changes a rule.
8. **Scope Concern: CNTR-MODERNIZATION-006 `:44`** names `SmsBroadcastReceiver` among receivers that "publish one-shot
   events"; it publishes none (`SmsBroadcastReceiver.java:44-111`). False today, and stale after the removal.
   Recommendation: the next revision of CNTR-006 drops the name; not edited here (another contract, another version
   line).
9. **Scope Concern: no contract text bounds how soon an incoming SMS is backed up** (BUG-025 AC-6, remaining part
   after ruling 54). Nothing in CNTR-004, DES-005 or REQ-005 is false against the decided behaviour. Optional sentence,
   not recommended for this record: append to the INV-4 note "The contract states the requested delay; it does not bound
   how soon the system runs the work (the trigger's 15-minute period and the app's job quota can hold it; BUG-025)."
   Recommendation: leave it to BUG-025's text story's checklist, which owns what the app promises.
10. **Scope Concern: one-off work survives a reboot under WorkManager; the legacy dropped it (`UNTIL_NEXT_BOOT`).** D-6
    states this. A pending `INCOMING` ran right after a restart in Run 1 of the device observation. It is harmless (one
    backup of new items) and older than this record. Recommendation: information only.
11. **Scope Concern: Rule 2's scoping sentence (C-16) and the legacy trigger's constraint.** The legacy trigger job did
    carry the `INCOMING` network constraint (`BackupJobs.java:168-172` at `d095fe02^`). The code dropped it, and BUG-023
    §2 argues the net effect is the same. Recommendation: accept the ruling "not a break"; if the orchestrator disagrees,
    the label of changelog item 3 changes and nothing else, because the code already has this shape.
12. **Scope Concern: the port javadoc says "Contract: CNTR-MODERNIZATION-004 v2"** (`BackupScheduler.java:36`), stale
    since v3. Recommendation: the fix story updates it to the current version with its other javadoc edits (it is in its
    citation list, item 5).

**Orchestrator rulings on the scope concerns (2026-11-21; decision appendix, Record 23).** They are binding for the application of this record and for the two stories.

| Concern | Ruling |
|---------|--------|
| 1 | As recommended: the boot-path change is a behavioural break of the boot path, not a break for consumers; the contract goes to v4; the new boot tests in the INV-1 row gate it |
| 2 | As recommended and as ruling 68 says: the text states "no" to PO-1 and PO-2; both stay listed for the product owner; a "yes" is an amendment of this record |
| 3 | `RECEIVE_SMS` stays. The receiver-removal story keeps the permission and records the reason. The grant and the delivery to the default-SMS receiver during a restore are read on the Android 16 emulator when a restore is next run there (story U-073's device check, allowed by Record 20); removing the permission would need its own record |
| 4 | As recommended: the receiver-removal story's plan computes the package's expected line ratio from the coverage report before the build; if the deletion would breach the floor, the story adds tests for the remaining receivers. The floor is not lowered |
| 5 | Apply the notes on the two closed artifacts (DES-MODERNIZATION-001, REQ-MODERNIZATION-001). They change no value |
| 6 | Apply the edits to DES-MODERNIZATION-014 (Run R reads the trigger line only) |
| 7 | Not folded in. The statements that are false for other reasons (items a to e) go into a separate housekeeping record after the two stories, together with concern 8 |
| 8 | As recommended: not edited here; the housekeeping record of concern 7 carries it |
| 9 | As recommended: no sentence is added here; what the app promises about the delay belongs to the text-only story for BUG-025 |
| 10 | Information only |
| 11 | As recommended: not a break; the code already has this shape |
| 12 | As recommended: the fix story for BUG-026 and BUG-027 updates the port's javadoc version reference with its other javadoc edits |

Order: the text is applied now, before the two stories are written, with the "Code not yet at v4" list (K-1, K-2) in the contract until each story closes; the close-out edits X-1 and X-2 are applied by the orchestrator when each story closes.


---

## Execution

### Changes Made

Applied 2026-11-21, artifacts first; the code follows in three stories (the fix for BUG-026 and BUG-027, the removal of the SMS-received receiver, and story U-075 for the foreground sentence). Until each closes, the contract's "Code not yet at v4" list (K-1, K-2) and the "today" half of the DES-MODERNIZATION-014 row 16-1 name the gap.

| File | Change | Layer |
|------|--------|-------|
| `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` | C-0 to C-21 (v4), and C-22 to C-30 (Amendment 1) | Design and contracts |
| `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` | D-0 to D-19, and D-20 to D-27 (Amendment 1) | Design and contracts |
| `sdlc/artifacts/design/modernization/DES-MODERNIZATION-008-hilt-di-design.md` | H-0 to H-2 | Design and contracts |
| `sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-toolchain-runtime-libraries-platform-level-design.md` | E-0 to E-4, E-5 and E-6 (Amendment 1), E-7 and E-8 (Amendment 2) | Design and contracts |
| `sdlc/artifacts/design/modernization/DES-MODERNIZATION-001-restore-play-store-eligibility-design.md` | P-0, P-1 (dated note) | Design and contracts |
| `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md` | R-1a to R-1c, and R-1d (Amendment 1); notes, no requirement value changed | Requirements |
| `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-001-restore-play-store-eligibility.md` | R-2a, R-2b (note) | Requirements |

Applied by a Solution Architect (contract and designs) and a Requirements Analyst (requirement notes). Checked by a Design Auditor: `sdlc/artifacts/build/change-records/evidence/CR-017-applied-text-check.md` (first verdict FAIL on two unedited sentences the v4 text contradicted and eight smaller points; "Verdict after Amendment 1: PASS"; "Quick check of Amendment 2: PASS").

### Verification Evidence

Run when the edits are applied. Expected after applying:

```
git grep -n -E "SmsBroadcastReceiver|SMS-received|SMS_RECEIVED" -- sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md
```
Only: the INV-4 note (C-12, "not part of this contract from v4", and its older sentence "Below API 24 that clause was
met at most by the SMS-received receiver"), K-2 (C-14), the v4 and v3 changelog entries, and in the design D-7, D-10 and
D-19. None in the content-trigger example (C-19).

```
git grep -n -E "bootup|scheduleBootup|BOOT_BACKUP_DELAY|one-time|one-off" -- <the same two files>
```
Hits about incoming, immediate, manual and restore work being one-off are expected. Every hit about the boot path says
that it registers the regular periodic backup, or that it enqueues no one-off (C-3, C-4, C-7, C-17, D-2, D-3, D-6, D-13,
D-17); none says the boot job is a one-off.

```
git grep -n -E "networkType *= *CONNECTED|BackoffConfig\(EXPONENTIAL, 30s, 300s\)," -- sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md
```
Expected: only the regular example's `backoff` line and the immediate and manual examples; nothing in the
content-trigger example.

Also: `CR-017` in `change_records` of the seven files; the contract's Current version and Status bullet both say v4; the
"Code not yet at v4" blockquote has K-1 and K-2; no absolute path in any edited text.

### Commit Traceability

| Commit | Date | Branch | Scope | Author |
|--------|------|--------|-------|--------|
| The commit with the subject "CR-017 applied: scheduler contract v4 ('Never', boot path, content-trigger example, SMS receiver)" (find it with `git log --grep "CR-017 applied"`) | 2026-11-21 | sdlc/modernization-plan | The contract, four designs, two requirement notes, this record and the check report | Orchestrator (text by the Requirements Analyst, applied by the Solution Architect and the Requirements Analyst, checked by the Design Auditor) |

---

## Not verified

- **The platform rule that `SMS_DELIVER` is delivered only to an app holding `RECEIVE_SMS`**, and that the SMS role grants
  it only when declared. Recalled; not fetched (Scope Concern 3).
- **What WorkManager does with the initial delay when `UPDATE` changes an existing periodic registration**, in particular
  one that has not run yet (BUG-027, "Not verified" 5). C-3 says only "UPDATE keeps an existing registration (its id and
  schedule)", which BUG-027 read in the library (`WorkerUpdater.kt:69-80`, 2.12.0); the exact next-run time is not claimed.
- **Whether the receiver package's coverage falls below its floor** after the deletion (Scope Concern 4).
- **What happens to the persisted enabled state of a component that a later build no longer declares**
  (`App.java:358-371` sets it for `SmsBroadcastReceiver`). Expected to be dropped by the package manager on upgrade;
  harmless either way; not checked.
- **WorkManager 2.9.1's default backoff value** for a request with no `setBackoffCriteria` (the constant's name was seen by
  U-069, not its value; 2.12.0 has 30 000 ms). It does not matter for the trigger, which never retries.
- **The library behaviour BUG-027 relies on** was read by BUG-027's author in 2.9.1 bytecode and 2.12.0 sources; not
  re-read for this record.
- **Story placement** is from rulings 55, 56 and 60 and the sprint-025 plan (`:21-25`); the stories have no IDs yet.

---

## Closure

- **Root Cause Category:** A design misread of the legacy scheduler (the `inSeconds <= 0` case; the boot branches) was
  carried into the code without a test, and a manifest change in an unrelated story (U-001) silently disabled a path the
  contract kept describing.
- **Prevention Recommendation:** When a design maps a legacy method, it names every guard of the legacy helper the method
  delegates to (here `schedule()`'s `inSeconds > 0`), and the contract's tests assert each guard. When two work kinds
  share a unique name, the contract says so and names the policy for each pair. When a story changes a receiver's export
  value, it states who sends the broadcast and checks delivery on a device.
- **Closed By:** Orchestrator, after the Design Auditor's checks (PASS after Amendment 1; PASS for Amendment 2)
- **Date Closed:** 2026-11-21 (the artifact edits; the code changes stay with their three stories, and the close-out edits X-1 to X-3 with their closings)
