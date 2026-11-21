# CR-017 applied-text check

| Field | Value |
|-------|-------|
| Record | `sdlc/artifacts/build/change-records/CR-017-cntr-004-scheduler-never-boot-example-and-receiver.md` (approved) |
| Files checked | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md`, `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md`, `sdlc/artifacts/design/modernization/DES-MODERNIZATION-008-hilt-di-design.md`, `sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-toolchain-runtime-libraries-platform-level-design.md`, `sdlc/artifacts/design/modernization/DES-MODERNIZATION-001-restore-play-store-eligibility-design.md`, `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md`, `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-001-restore-play-store-eligibility.md` (uncommitted working tree, `git diff`) |
| Code read (at `fd70325a`, `git show`) | `scheduler/WorkManagerScheduler.kt`, `scheduler/BackupScheduler.java`, `worker/BackupTriggerWorker.kt`, `receiver/BootReceiver.java`, `receiver/SmsBroadcastReceiver.java`, `receiver/PackageReplacedReceiver.java`, `compat/SmsReceiver.java`, `preferences/Preferences.java`, `App.java`, `AndroidManifest.xml`, `res/values/arrays.xml`; tests `service/WorkManagerSchedulerTest.kt`, `worker/BackupTriggerWorkerTest.kt`, `receiver/BootReceiverTest.java` (all under `app/src/main/java/com/zegoggles/smssync/`, `app/src/main/`, or `app/src/test/java/com/zegoggles/smssync/`). Legacy at `d095fe02^`: `service/BackupJobs.java`, `service/AlarmManagerDriver.java`. Commits `50f3b34f` and `d7a69974` (stat and message) |
| Other sources read | `sdlc/artifacts/stories/BUG-026-…md` and `BUG-027-…md` (acceptance criteria), `sdlc/artifacts/build/research/2026-11-21-background-backup-and-boot-observation.md` (row 09:27:45), model report `sdlc/artifacts/build/change-records/evidence/CR-015-applied-text-check.md` |
| Date | 2026-11-21 |
| Method | Read-only. No artifact or source file edited. No Gradle, tests, device or network. The working tree under `app/` was not read. |

Verdict: FAIL

Every edit id is in its file word for word, at the place the record means, and nothing else in the seven files
changed. Frontmatter, tables, fences and line endings are intact. The ruling, the v4 step and the K-list hold, and the
corrected content-trigger example is true against `scheduleContentTrigger()` at `fd70325a`.

The verdict is FAIL because of two must-fix items. In both, a present-tense sentence that the record did not edit is
now contradicted by v4 text in the same contract. Neither is covered by the K-list or by a ruling, and no planned story
would correct it (the same pattern as CR-015's F-1):

- **M-1:** the `SchedulerConfig.existingPolicy` field still says `REPLACE` is "VERIFIED universal" (contract `:194`).
- **M-2:** the call-log condition of the content trigger is stated as `isCallLogBackupAfterCallEnabled()` alone in
  three places (contract `:73-75`, `:224-225`; DES-005 `:318`, `:322`). The corrected example (C-19) and the code
  require call-log backup to be enabled as well.

There are eight should-fix items (S-1 to S-8). The most important are S-1 (Rule 4 and Rule 4a disagree about cancelling
under "Never"), S-4 (the test rows can be satisfied vacuously), and S-7 (the "if the owner answers yes" paragraph misses
several sentences). All are one-line or one-cell texts that the record did not order, so they need an amendment to
CR-017 (as CR-015's Amendment 1) before they are applied.

Line numbers in this report are working-tree line numbers after the edits, unless marked "`fd70325a`".

---

## 1. Edits applied as the record orders

### 1.1 One row per edit id

| Edit | Location after edit | Result |
|------|---------------------|--------|
| C-0 | contract :15 (`change_records`) | Matches |
| C-1 | contract :44 (Consumers row; cells 3 and 4 unchanged) | Matches |
| C-2 | contract :63-71 | Matches |
| C-3 | contract :76-83 | Matches |
| C-4 | contract :160-161 (after the unique-name bullet, before the `inferBackupType` bullet) | Matches |
| C-5 | contract :168-179, directly before `### Value object: \`SchedulerConfig\` (core) — constraint + backoff configuration` (:181) | Matches. The record quoted only the prefix of the heading line. The applier inserted the block before the full heading line and left that line unchanged (diff context, :181). Correct |
| C-6 | contract :192-193 | Matches |
| C-7 | contract :199-200 | Matches |
| C-8 | contract :207-210 | Matches |
| C-9 | contract :218-221 | Matches |
| C-10 | contract :274 (last cell of the INV-1 row) | Matches |
| C-11 | contract :277 (last sentence of the INV-4 realization) | Matches |
| C-12 | contract :279 (INV-4 note): the receiver sentences are replaced. The sentences before them (up to "…was never implemented.") and after them ("Test gate: …") are unchanged | Matches |
| C-13 | contract :285 | Matches |
| C-14 | contract :287-289, between the Current-version line and `### Changelog` (:291) | Matches |
| C-15 | contract :293, above the v3 entry (:294, unchanged) | Matches |
| C-16 | contract :313, appended at the end of Rule 2 with a leading space | Matches |
| C-17 | contract :315 (Rule 4 replaced), :316 (Rule 4a) | Matches |
| C-18 (1), (2), (3) | contract :396, :401, :403 (regular example only; "// isWifiOnly() == true" occurs only there) | Matches |
| C-19 | contract :412-434. The heading (:409) and the two fence lines (:411, :435) stay. The blank line that preceded "// Separately…" is gone, as the record's New block has none | Matches |
| C-20 | contract :454 | Matches |
| C-21 | contract :457 (last bullet of the file) | Matches |
| D-0 | DES-005 :17 | Matches |
| D-1 | DES-005 :89 | Matches |
| D-2 | DES-005 :91 | Matches |
| D-3 | DES-005 :122 | Matches |
| D-4 | DES-005 :264 (after the `RestoreWorker` row) | Matches |
| D-5 | DES-005 :282 | Matches |
| D-6 | DES-005 :289 (middle cell only) | Matches |
| D-7 | DES-005 :323 (the whole bullet replaced) | Matches |
| D-8 | DES-005 :359 | Matches |
| D-9 | DES-005 :378 | Matches |
| D-10 | DES-005 :381 | Matches |
| D-11 | DES-005 :382 (the row directly after the INV-4 row) | Matches |
| D-12 | DES-005 :438 | Matches |
| D-13 | DES-005 :440 | Matches |
| D-14 | DES-005 :452 | Matches |
| D-15 | DES-005 :457 | Matches |
| D-16 | DES-005 :458 | Matches |
| D-17 | DES-005 :460 | Matches |
| D-18 | DES-005 :500, after the `(30,300)` row (:499) | Matches. The applier reported that the row was first split and then repaired. The final state is one 3-cell row, and the `(30,300)` row is unchanged (no `-` line for it in the diff) |
| D-19 | DES-005 :517, after the CR-015 paragraph (:515), with a blank line between | Matches |
| H-0 | DES-008 frontmatter: `change_records: []` becomes a list containing `CR-017` | Matches |
| H-1 | DES-008 :114 | Matches |
| H-2 | DES-008 :499, after a blank line at the end of the file | Matches |
| E-0 | DES-014 :35 | Matches |
| E-1 | DES-014 :1423 | Matches |
| E-2 | DES-014 :2254 | Matches |
| E-3 | DES-014 :1339 | Matches |
| E-4 | DES-014 :55-59, after the top note's last line (:53) and a blank line | Matches |
| P-0 | DES-001 frontmatter: `change_records: []` becomes a list containing `CR-017` | Matches |
| P-1 | DES-001 :308, directly before `## Artifacts Consulted` (:310), blank line kept | Matches |
| R-1a | REQ-005 :19 | Matches |
| R-1b | REQ-005 :88 (the CR-015 note; only the named sentence replaced) | Matches |
| R-1c | REQ-005 :90 (new last paragraph) | Matches |
| R-2a | REQ-001 :15 | Matches |
| R-2b | REQ-001 :104, after "Synced with DES-MODERNIZATION-001." and a blank line | Matches |
| X-1, X-2 | — | Not applied, as ordered. K-1 and K-2 are present (contract :288, :289) |

### 1.2 Nothing else changed

- `git diff --stat`: contract 106, DES-005 36, DES-008 7, DES-014 13, DES-001 5, REQ-001 3, REQ-005 5 lines (123
  insertions, 52 deletions). I read every hunk, and each belongs to an edit id above. The contract has 11 hunks (C-0;
  C-1; C-2/C-3; C-4/C-5; C-6 to C-8; C-9; C-10 to C-12; C-13 to C-15; C-16/C-17; C-18; C-19; C-20/C-21). DES-005 has 12.
  DES-008 has 3. DES-014 has 4. DES-001 has 2. Each REQ file has 2.
- `id`, `status` and `artifact_type` are unchanged in all seven files: no `-`/`+` line in the diff touches them. The
  contract has no frontmatter version field. Its version is in the text, which now says v4 in the Current-version line
  (:285), the Changelog (:293) and the Status bullet (:454).
- Tables: every added or changed table row has the same column count as the row it replaces, or as its table's header.
  Contract: C-1, C-10, C-11 have 4 columns, and the new C-5 table has 5 (header, separator and 6 rows). DES-005: D-3,
  D-4, D-5, D-6, D-12 to D-17 and D-18 have 3. D-9, D-10 and D-11 have 2, the same as the `| Invariant | Test |` header.
  DES-008 H-1 has 3. DES-014 E-2 has 3.
- Fences: the counts are unchanged (contract 22, DES-005 4, DES-014 4, DES-001 4, others 0) and balanced.
- Line endings: in the working tree, CNTR-004, DES-005 and DES-008 have a CR before every LF (457/457, 517/517,
  499/499). DES-014, DES-001, REQ-005 and REQ-001 contain no CR (2281, 330, 90, 192 LF lines). The repository blobs are
  LF-normalised for all seven, and the diff is not a whole-file rewrite, so each file keeps its checkout style. The
  "LF will be replaced by CRLF" warnings on the four LF files are git working-copy notices, not changes.
- No absolute path or drive-letter path occurs in any added line.

---

## 2. Internal consistency after the edits

### 2.1 The five places the appliers flagged

| Place | What sits next to the applied text | Disposition |
|-------|-------------------------------------|-------------|
| contract :279 (INV-4 note) | "Below API 24 that clause was met at most by the SMS-received receiver, for incoming SMS only, and its API ≤23 assertion was never implemented." | **History, correctly left.** It is past tense, it says nothing about v4, and the record's verification step expects it. The test-gate sentence later in the same note (CR-015 text) is stale against the code; see N-4 (not CR-017's subject) |
| contract :294 (v3 changelog) | "…the SMS-received broadcast path is unchanged…"; "added a note stating what the broadcast path covers" | **History, correctly left.** It is a dated changelog entry describing v3 (the record's "Not edited", `fd70325a` :255) |
| contract :206 | The `CONNECTED` line of `NetworkRequirement`, next to C-8 | **No conflict.** `UNMETERED`/`CONNECTED` apply to backup work, and C-8 now scopes `NONE` the same way. I could not tell which sentence the applier meant by :206, so I read the whole `SchedulerConfig` block (:186-227). The real conflict in that block is at :194; see **M-1**. The `ContentUriTrigger.uri` comment at :224-225 is **M-2** |
| DES-005 :381 (INV-4 row) | The CR-015 sentences about the API 24 test and the API 29 call-log tests | **True as they stand.** The D-10 sentence that follows them is true: `BackupTriggerWorkerTest` asserts stage 2 (`ac3_*_triggerRequestEqualsScheduleIncomingRequest`, `ac8a/b/c_*`) |
| DES-005 :280 (INV-1 mapping row) | "periodic: `enqueueUniquePeriodicWork(name, REPLACE/UPDATE, …)`" | **True, correctly left.** The phrase is permissive. It covers `UPDATE` and the fallback (the deprecated periodic `REPLACE` is `CANCEL_AND_REENQUEUE`). The contract's INV-1 realization keeps the same words and C-10 narrows them. The record lists it (`fd70325a` :278) as true |

### 2.2 Contract and DES-005, read whole: present-tense statements against v4

History, correctly left:

- Contract: the v3 and v2 changelog entries (:294-301) and the CR-015 Notes bullet (:456).
- DES-005: the "History:" paragraph (:325), the CR-015 note (:515) and the Behavioral-preservation note (:134-139).

Present tense and still true:

- Contract: the INV-2/INV-3 rows; Rules 1, 3, 5 to 8; Error Handling (including "re-executed … after process
  death/reboot", :326); the `BackupType` notes on `CONTENT_TRIGGER_TAG` (:151-153), which describe the legacy tag; the
  Breaking change policy (:305), with which the v4 ruling is consistent.
- DES-005: `cancelAll` rows (:94, :443-444) and the integration-contracts table (:430).

Present tense, now contradicted by v4 and not covered: **M-1** (contract :194) and **M-2** (contract :73-75,
:224-225; DES-005 :318, :322). New text that contradicts other new text: **S-1** (Rule 4 against Rule 4a) and **S-2**
(D-4 against D-10). Naming that does not match the contract's new table or the code: **S-3**.

### 2.3 Search of the other DES-, CNTR- and REQ-MODERNIZATION files

I ran `git grep -n` on the working tree, over `sdlc/artifacts/design/*MODERNIZATION-*` and
`sdlc/artifacts/requirements/*MODERNIZATION-*` except CNTR-004 and DES-005. The patterns were `SmsBroadcastReceiver`,
`SMS_RECEIVED`, `SMS-received`, `scheduleBootup`, `BOOT_COMPLETED`, `bootup`, `scheduleRegular`, `CONTENT_TRIGGER`,
`BackupTriggerWorker`; then `"Never"`, `regular schedule`, `BOOT_BACKUP_DELAY`, `boot delay`, `REPLACE/UPDATE`,
`\bREGULAR\b`, `at boot`, `on boot`, `after reboot`; then `SmsBroadcastReceiverTest`, `BootReceiverTest`; then lists of
`@HiltWorker` classes.

| Hit | Disposition |
|-----|-------------|
| DES-001 :38, :82, :83, :89, :245 | Export declarations of U-001. :82's rationale is covered by P-1 (:308). The others are true |
| REQ-001 :35, :95, :96, :109 (AC-16), :188 | Covered by R-2b (:104). The others are true |
| DES-007 :313, :325 | History (record, "Not edited") |
| DES-012 :183, :794 | History (:183); true after the fix story (:794) |
| DES-014 :845 | S1 test table, not a CR-017 fact |
| DES-014 :1294 (row 37-5) | "`SmsBroadcastReceiver` listens to `SMS_RECEIVED`": true until the removal (manifest `:132-139` at `fd70325a`); E-4 marks it as history |
| DES-014 :1829-1846 | The CR-015 argument; E-4 marks it as history |
| DES-014 :2176 | File list; history (E-4) |
| REQ-013 :212, :264 | "scheduled (REGULAR) backup … via the REGULAR periodic request": true now and after the fix |
| REQ-020 :108 | A quote of the platform page; true |
| `@HiltWorker` lists | Only DES-008 :114 (edited, H-1) and DES-007 :313 (history) |

No further present-tense statement of the four facts was found outside the edited files.

### 2.4 Left by ruling 7 or 8 (not findings)

Current line numbers in the contract (`fd70325a` line in brackets):

- :25 [:24] "Draft; not finalized."
- :43 [:42] the debug-only `LegacyScheduler`/`CompositeScheduler` producer row
- :47 [:46] and :49 [:48] the `BackupJobs` consumer and caller notes
- :60-117 [:59-103] the `Unit` and `Flow` return types
- :306 [:267] the swap sentence
- :445 [:400] the DES-008 row, "`CompositeScheduler` in debug"
- :455 [:410] the "v2 implementation note", which says "nine"

Left by ruling 8: CNTR-MODERNIZATION-006 :44 (`SmsBroadcastReceiver` "publish one-shot events").

The same class as ruling 7(b), but not on its list (for the housekeeping record, not a CR-017 finding): contract :270,
"the parallel-run `CompositeScheduler` exists to prove it before the binding flip"; DES-005 :141-156, :260-261, :366,
:407-412 (`LegacyScheduler`/`CompositeScheduler`, deleted per `WorkManagerScheduler.kt:64-65`).

---

## 3. Truth against the code at `fd70325a`

| v4 statement | Code | Status |
|--------------|------|--------|
| C-19 example: `networkType = NONE`, `requiresCharging = false`, `backoff = null`, `existingPolicy = UPDATE`, `uniqueName = "CONTENT_TRIGGER"`, `initialDelaySecs = null`, recurring every 15 min, SMS URI plus the call-log URI "only when call-log backup is enabled and `isCallLogBackupAfterCallEnabled()`", both with descendants | `WorkManagerScheduler.kt:224-246`: `Constraints.Builder().addContentUriTrigger(SMS_PROVIDER, true)`, plus `CALLLOG_PROVIDER` when `isBackupEnabled(CALLLOG) && isCallLogBackupAfterCallEnabled()` (:227-230). `PeriodicWorkRequest` of `BackupTriggerWorker` every `PERIODIC_MIN_INTERVAL_SECS` = 15 min (:116, :232-236). No `setRequiredNetworkType`, no `setBackoffCriteria`, no initial delay. `addTag(CONTENT_TRIGGER_UNIQUE_NAME)` (:238) = `"CONTENT_TRIGGER"` (:93). `enqueueUniquePeriodicWork(CONTENT_TRIGGER, UPDATE)` (:241-246) | **True today.** One nit: the comment "(CONTENT_TRIGGER_TAG)" names the legacy constant, whose value was `"contentTrigger"` (`BackupJobs.java:57` at `d095fe02^`); see S-3 |
| C-19: the worker calls `scheduleIncoming()`; the `INCOMING` one-off carries REPLACE, network per `isWifiOnly()`, EXPONENTIAL 30 s; `scheduleIncoming()` is its only builder | `BackupTriggerWorker.kt:59-71`; `WorkManagerScheduler.kt:145-153` (`networkConstraints(INCOMING)`, `EXPONENTIAL` 30 s, `REPLACE`); no other builder of `INCOMING` in `app/src/main` | **True today** |
| C-5 table: `INCOMING`, `CONTENT_TRIGGER`, `BROADCAST_INTENT`, `MANUAL`/`SKIP` and restore rows | `:145-153`; `:224-246`; `:309-316`; `:350-357`; `:385-392` | **True today** |
| C-5 table, `REGULAR` row; C-2; C-3; C-10; C-17 (Rules 4 and 4a); C-18 boot and "Never" parts | `scheduleRegular()` clamps −1 to 900 s (:180) and has no fallback (:192-197). `scheduleBootup()` enqueues a one-time `REGULAR` with `REPLACE` (:273-281) | **Not yet true; named in K-1** (:288). "Encryption-degraded check first" (Rules 4 and 4a) is true today (:171-174, :263-266) |
| C-4 bullet "it registers the REGULAR periodic work" (:160-161); C-7 boot comment (:199-200); C-9 `REGULAR` fallback (:218-221) | Same as above | **Not yet true.** K-1's description of the code covers them, but K-1's own "Affected here" list does not name them (the record's "Order" table does). See S-5 |
| C-6, C-8, C-9 (trigger and `UPDATE` parts), C-16 | No backoff and no network type on the trigger (:232-239); `Constraints.NONE` for immediate and manual (:310, :351); `UPDATE` for `REGULAR` (:195) and `CONTENT_TRIGGER` (:244); the trigger worker always returns success (`BackupTriggerWorker.kt:70`) | **True today** |
| C-11, C-12, K-2, D-7, D-10: the receiver is not an incoming path in fact; "has not been delivered … since 2026-06-02, when U-001 declared it `android:exported="false"`"; "`RECEIVE_SMS` is never requested at run time" | Manifest `:132-139` (`exported="false"`). Commit `50f3b34f` (2026-06-02, U-001) added `exported="false"` to it. `RECEIVE_SMS` occurs in `app/src/main` only at `AndroidManifest.xml:17`. The class and its toggle remain (`SmsBroadcastReceiver.java:44-111`, `App.java:46`, `:359`) | **True as worded; the code still present is named in K-2** |
| C-12: "`scheduleIncoming()` … its production caller is the content-trigger worker" | `BackupTriggerWorker.kt:68`; also `SmsBroadcastReceiver.java:60`, which is never delivered | **True in fact; the remaining call is covered by K-2** |
| D-4, D-8, H-1: `BackupTriggerWorker` is a `@HiltWorker` plain `Worker`, injected with `BackupScheduler` and `Preferences` | `BackupTriggerWorker.kt:51-57`; not a Hilt worker before `d7a69974` (`class BackupTriggerWorker(` at its parent) | **True today.** D-4's "stage 1" label conflicts with D-10 and the code; see S-2 |
| D-6: a pending `INCOMING` ran after the restart (Run 1, 09:27:45) | Research file row 28 | **True** |
| D-5, D-13: legacy guards and boot branches | `BackupJobs.java:86-100`, `:132` at `d095fe02^`. The old-scheduler branch set one `AlarmManager` alarm 60 s after boot (`AlarmManagerDriver.schedule`, `alarmManager.set`), since nothing survived a reboot; the default branch returned `null` | **True.** "Translates both legacy branches" is fair |
| D-11: offered values 1800, 3600, 7200, 21600, 86400; "Never" = −1 | `res/values/arrays.xml` `auto_backup_schedule_values` | **True** |
| C-1 Consumers row | The port is called by `App.rescheduleJobs()` (:373-385), `BackupBroadcastReceiver` (:54), `BootReceiver` (:33), `BackupTriggerWorker` (:68) and `MainViewModel` (:143, :179). `PackageReplacedReceiver` only emits `AutoBackupSettingsChanged` | **Partly true; the gaps predate CR-017** (N-2). The receiver clause is true |
| `RECEIVE_SMS` stays | Manifest `:17`. `compat/SmsReceiver` is declared for `SMS_DELIVER` (`:167-173`, `exported="true"`, `BROADCAST_SMS`) and stores the message while the app holds the SMS role (`SmsReceiver.onReceive`) | **No applied sentence says it stays, and none says it goes.** Nothing is false, but the receiver story's file list in C-12 and D-7 is silent on it; see S-6 |
| P-1, R-2b: the export rationale was wrong; the other five receivers are unaffected | Manifest `:132-180` | **True** |

No v4 statement is false today and also missing from the K-list, apart from S-5's three gaps in K-1's "Affected here"
list. Those are listing gaps, not silent ones: K-1's description of the code covers them.

---

## 4. The owner's open questions PO-1 and PO-2

The applied text states "no" to both in C-15 item 2 ("v4 states the answer 'no' to both") and in R-1c ("not kept
unless the product owner decides otherwise"). The record's marked paragraph ("If the owner later answers 'yes'") is
**not complete** against the applied text; see **S-7**. Under PO-1 "yes", these sentences also become false or need to
change, and the paragraph does not list them:

1. C-4, the first sentence of the bullet (contract :160): "The boot path has no unique name of its own…". The paragraph
   changes only "No BOOT unique name is used".
2. C-5, the sentence under the table (:179): "No `BOOT` unique name is used." The applied C-5 table also has **no** `BOOT`
   row. The paragraph says "the `BOOT` row reads …", which presumes the record's internal table (which has a "Not used"
   `BOOT` row), not the applied one. A row must be **added**.
3. C-17, Rule 4 (:315): "…and starts no separate boot backup". The paragraph changes only the "Never" sentence of Rule 4.
4. C-15, Changelog v4 item 2 (:293): "v4 states the answer 'no' to both". Under PO-2, also "with a registered regular
   backup, a restart no longer starts an extra backup a minute later".
5. C-13, Current-version line (:285): under PO-2 the step is v5 (the paragraph says so), so this line changes as well.
6. DES-005 D-2 (:91) "(no one-off)", D-3 (:122) "not a one-off", D-6 (:289) "enqueues no one-off", and D-16 (:458) "the
   boot path uses `REGULAR`". The paragraph names only D-11 and D-13.
7. C-7 (:199-200) and D-17 (:460): the 60 s delay would also be the delay of the `BOOT` one-off. They become incomplete,
   not false.

The PO-2 "yes" part reuses "the same four places" and inherits every gap above.

---

## 5. What the two later stories will be held to

**The fix story (BUG-026 and BUG-027).** The contract gates the boot-path break on "the boot tests in DES-005 §Design
Validation" (C-15, :293). Those tests are D-9 (:378) and D-11 (:382). Both cite the bug ACs, which are precise (BUG-026
AC-1 to AC-4; BUG-027 AC-1 to AC-5 and AC-11 state the preconditions, the same `id`, the `generation`, the three
sequences, and "fails today"). But the two rows, which a story writer reads first, can be satisfied vacuously; see
**S-4**:

- D-11 does not state "auto backup on, encryption not degraded". With auto backup off, `scheduleRegular()` returns `null`
  and `scheduleBootup()` cancels everything, so "returns no job and leaves no non-terminal `REGULAR` entry" passes on
  today's code (`WorkManagerScheduler.kt:175-178`, `:267-271`).
- D-9's "does not block `scheduleRegular()` or `scheduleBootup()`" names no observable result. A test that only checks
  that no exception is thrown would pass.
- D-9's "after any sequence of the two and `cancelAll()`" is unbounded. BUG-027 AC-4 fixes three sequences.
- Neither row requires the new tests to fail on the scheduler code at `fd70325a`. Without that, "gated by tests" carries
  no evidence (BUG-026 Verification 2 and BUG-027 Verification 2 do require it).

The "kept green with no edit" list in the record is compatible with the fix. `WorkManagerSchedulerTest` `setUp` stubs
`isAutoBackupEnabled = true` and `regularTimeoutSecs = 3600` (`:71`, `:73` at `fd70325a`), so
`ac5_scheduleBootup_withAutoBackupEnabled_isEnqueued` (`:428-438`, non-empty, not `FAILED`/`CANCELLED`) still passes
when the boot path registers the periodic entry. `BootReceiverTest.shouldScheduleBootupBackupAfterBootup` only verifies
the call.

**The receiver-removal story.** D-10 removes `SmsBroadcastReceiverTest` as INV-4 evidence and adds no test, which is
right. No applied text requires a check that the manifest no longer declares an `SMS_RECEIVED` receiver and still
declares `RECEIVE_SMS`. The record (Impact Analysis, item 3; ruling 3) binds the story to keep the permission, but the
contract and the designs do not; see **S-6**.

---

## 6. Findings

### M-1 (must-fix): `existingPolicy` still says `REPLACE` is universal

- **File, line:** `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md:194-195`
- **Present text:**
  ```
    existingPolicy: ExistingPolicy = REPLACE // VERIFIED universal
                                             // (setReplaceCurrent(true), :188)
  ```
- **Why it is wrong:** v4 states in the same block that periodic work uses `UPDATE`: the `ExistingPolicy` enum
  `{ REPLACE, UPDATE }` (:218-221, C-9), the unique-name table (:172, :174, C-5), the regular example `existingPolicy =
  UPDATE` (:401, C-18) and the trigger example (:421, C-19). "Universal" is now false for both periodic kinds. The
  record edited DES-005's twin row for exactly this reason (D-15, DES-005 :457), but not this field. Neither the K-list
  nor the "Not edited, on purpose" list mentions it, and no story edits it.
- **Replacement (lines 194-195, same column layout):**
  ```
    existingPolicy: ExistingPolicy = REPLACE // one-off work; periodic work (REGULAR,
                                             // CONTENT_TRIGGER) uses UPDATE (v4; enum below).
                                             // Legacy: setReplaceCurrent(true) on every job (:188)
  ```

### M-2 (must-fix): the call-log condition of the content trigger is stated incompletely in three places

- **Files, lines, present text:**
  - contract :73-75: "// recurring content-URI observer over SMS provider (+ call-log when //
    isCallLogBackupAfterCallEnabled()); available on every supported level // (minSdk 24). (BackupJobs.java:82)"
  - contract :224-225: "uri: String // SMS provider URI; + call-log URI when // isCallLogBackupAfterCallEnabled()"
  - DES-005 :318: "(and the call-log provider when call-log-after-call is enabled, `:180-181`)"
  - DES-005 :322: "plus the call-log URI under the same `isCallLogBackupAfterCallEnabled()` condition."
- **Why it is wrong:** C-19 now gives the condition as "call-log backup is enabled and `isCallLogBackupAfterCallEnabled()`"
  (:412-414, :427), which is what the code does (`WorkManagerScheduler.kt:227-228`) and what the legacy job did
  (`observedUris()`, `BackupJobs.java:180` at `d095fe02^`). `isCallLogBackupAfterCallEnabled()` reads only its own
  preference (`Preferences.java:135-137`). With call-log backup off and "after call" on, no call-log URI is observed, so
  the four sentences above are false as stated, and they contradict the corrected example. They are not in the K-list or
  the "Not edited" list. The INV-4 clause (:277, "when call-log-after-call is enabled") has the same gap but is left by
  the record's "INV-4 clause: Untouched" ruling; see N-5.
- **Replacements:**
  - contract :73-75:
    ```
          // recurring content-URI observer over SMS provider (+ call-log when
          // call-log backup is enabled and isCallLogBackupAfterCallEnabled());
          // available on every supported level (minSdk 24). (BackupJobs.java:82)
    ```
  - contract :224-225:
    ```
      uri: String                              // SMS provider URI; + call-log URI when
                                               // call-log backup is enabled and
                                               // isCallLogBackupAfterCallEnabled()
    ```
  - DES-005 :318: "(and the call-log provider when call-log backup and call-log-after-call are both enabled, `:180-181`)"
  - DES-005 :322: "plus the call-log URI under the same condition as the legacy job: call-log backup enabled and
    `isCallLogBackupAfterCallEnabled()` (`BackupJobs.java:180`; CR-017)."
  - Hand-over, not this record: the port javadoc `BackupScheduler.java:71-72` has the same gap. The fix story already
    edits that javadoc (ruling 12) and can correct it.

### S-1 (should-fix): Rule 4 forbids the cancel that Rule 4a requires under "Never"

- **File, line:** contract :315 (Rule 4, C-17) against :316 (Rule 4a).
- **Present text (Rule 4):** "…It MUST NOT enqueue a one-time work under `REGULAR`, MUST NOT cancel or reset a registered
  or running regular backup, and starts no separate boot backup. With a non-positive regular interval ("Never") it
  registers nothing (Rule 4a)…"
- **Why it is wrong:** Rule 4a says the boot path "MUST cancel an existing `REGULAR` work" when the interval is ≤ 0.
  Rule 4 forbids cancelling a registered regular backup without exception. The conflict is real in the upgrade case: a
  user on "Never" has a 15-minute periodic `REGULAR` that today's build registered (BUG-026). After the fix and a
  restart, Rule 4a requires the boot path to cancel it and Rule 4 forbids that. BUG-027 AC-11 resolves it in the tests,
  but the contract should not need the bug to be read.
- **Replacement (Rule 4, from "It MUST NOT enqueue" to "(Rule 4a)."):** "It MUST NOT enqueue a one-time work under
  `REGULAR` and starts no separate boot backup. With a positive regular interval it MUST NOT cancel or reset a
  registered or running regular backup. With a non-positive regular interval ("Never") it registers nothing and cancels
  an existing `REGULAR` work (Rule 4a)." If this is adopted, the record's PO-1 item 2 must quote the new sentence.

### S-2 (should-fix): D-4 calls the trigger worker stage 1; D-10 and the code call its work stage 2

- **File, line:** DES-005 :264 (D-4), against :381 (D-10) and `BackupTriggerWorker.kt:39-41`.
- **Present text:** "`@HiltWorker` `Worker` (U-069); stage 1 of the INV-4 debounce: runs on a content-URI change and calls
  `BackupScheduler.scheduleIncoming()`; never backs up itself".
- **Why it is wrong:** D-10 says "Stage 2 of the debounce (the trigger worker calls `scheduleIncoming()`)". The worker's
  KDoc says the same thing: stage 1 is the content change firing the worker, and stage 2 is the worker enqueuing through
  `scheduleIncoming()`. These are two new sentences of the same record, and they use different labels.
- **Replacement (cell 3):** "`@HiltWorker` `Worker` (U-069); the content-URI-triggered worker of the INV-4 debounce: it
  runs on a content-URI change (stage 1) and calls `BackupScheduler.scheduleIncoming()` to enqueue the delayed incoming
  backup (stage 2); never backs up itself"

### S-3 (should-fix): the content-trigger name and tag are written three ways

- **Files, lines, present text:**
  - contract :422 (C-19): `uniqueName = "CONTENT_TRIGGER", // also its tag (CONTENT_TRIGGER_TAG)`
  - DES-005 :443: "`cancelUniqueWork(REGULAR)` + `cancelUniqueWork(contentTrigger)`"
  - DES-005 :458 (D-16 kept it): "+ `contentTrigger` tag"
- **Why it is wrong:** in the code, the unique name and the tag are both `"CONTENT_TRIGGER"`
  (`WorkManagerScheduler.CONTENT_TRIGGER_UNIQUE_NAME`, `:93`, `:238`, `:243`, `:405`). The contract's new table says
  `CONTENT_TRIGGER` (:174). `CONTENT_TRIGGER_TAG` is the legacy constant, whose value was `"contentTrigger"`
  (`BackupJobs.java:57` at `d095fe02^`), and no constant of that name exists in the WorkManager code. The example
  therefore equates the new tag with a legacy constant of a different value, and DES-005 names the work by the legacy
  value. Exact-name consistency across the contract, the design and the code fails.
- **Replacements:**
  - contract :422: `uniqueName = "CONTENT_TRIGGER", // also its tag (WorkManagerScheduler.CONTENT_TRIGGER_UNIQUE_NAME; the legacy job's tag was CONTENT_TRIGGER_TAG = "contentTrigger")`
  - DES-005 :443, column 3: "`cancelUniqueWork(REGULAR)` + `cancelUniqueWork(CONTENT_TRIGGER)`"
  - DES-005 :458, column 2: replace "+ `contentTrigger` tag" with "+ `CONTENT_TRIGGER` (unique name and tag)"

### S-4 (should-fix): the D-9 and D-11 test rows can be satisfied vacuously

- **File, line:** DES-005 :378 (the D-9 sentences) and :382 (D-11).
- **Why:** see §5. They omit the preconditions, name no observable result for the one-time-entry case, leave the
  sequences unbounded, and do not require the tests to fail on the old code.
- **Replacement for the D-9 sentences (after "…the equivalent of `setReplaceCurrent(true)`."):** "For `REGULAR`
  (CNTR-MODERNIZATION-004 v4, Rule 4), with auto backup on and encryption not degraded: after `scheduleRegular()` with
  3600, `scheduleBootup()` leaves exactly one periodic `ENQUEUED` entry under `REGULAR` with the same `id`, a repeat
  interval of 3 600 000 ms and a `generation` raised by exactly one. With nothing registered, `scheduleBootup()` leaves
  exactly one periodic `ENQUEUED` entry with the configured interval, `initialDelayMillis == 60000`, the regular network
  constraint (for both `isWifiOnly()` values) and the tag `REGULAR`. With a one-time `BackupWorker` entry seeded under
  `REGULAR` directly through WorkManager, in `ENQUEUED` and in `CANCELLED` state, each of `scheduleRegular()` and
  `scheduleBootup()` leaves exactly one periodic `ENQUEUED` entry under `REGULAR`. After each of the three sequences of
  BUG-027 AC-4, `getWorkInfosByTag("REGULAR")` holds at most one non-terminal item, and no unique name other than
  `REGULAR` holds a `REGULAR`-tagged item. These tests are added by the story that fixes BUG-026 and BUG-027 (BUG-027
  AC-1 to AC-5); the story records that the first, third and fourth fail on the scheduler code at commit `fd70325a`."
- **Replacement for the D-11 test cell:** "With auto backup on and encryption not degraded: with `regularTimeoutSecs` of
  −1 and of 0, `scheduleRegular()` returns `null` and leaves no non-terminal entry under `REGULAR`, also after an
  earlier registration with 3600 and without a `cancelAll()` in between; `scheduleBootup()` with −1 or 0 does the same;
  a positive value below 900 (60, 899) and 900 give `repeatIntervalMillis` 900 000; each offered value (1800, 3600,
  7200, 21600, 86400) gives its own interval in milliseconds. The story records that the −1, 0 and 3600-then-−1 tests
  fail on the scheduler code at `fd70325a`. Added by the story that fixes BUG-026 and BUG-027 (BUG-026 AC-1 to AC-4;
  BUG-027 AC-11)."

### S-5 (should-fix): K-1's "Affected here" list omits three statements it covers

- **File, line:** contract :288 (K-1).
- **Present text:** "Affected here: the `scheduleRegular()` and `scheduleBootup()` blocks, the `REGULAR` row of the
  unique-name table, the INV-1 realization, Rules 4 and 4a, and the regular example."
- **Why:** these are also not true until the fix story: the boot-path bullet of the `BackupType` notes (C-4, :160-161:
  "it registers the REGULAR periodic work"), the `initialDelaySecs` boot comment (C-7, :199-200), and the `REGULAR`
  fallback in the `ExistingPolicy` note (C-9, :218-221). The record's "Order relative to the code" table lists C-4, C-7
  and C-9 (fallback) under K-1, but the applied K-1 does not. The orchestrator deletes K-1 at X-1, so the list should be
  complete until then.
- **Replacement:** "Affected here: the `scheduleRegular()` and `scheduleBootup()` blocks, the boot-path bullet of the
  `BackupType` notes, the boot comment of `initialDelaySecs`, the `REGULAR` fallback in the `ExistingPolicy` note, the
  `REGULAR` row of the unique-name table, the INV-1 realization, Rules 4 and 4a, and the regular example."

### S-6 (should-fix): no applied text says `RECEIVE_SMS` stays

- **File, line:** DES-005 :323 (D-7); contract :289 (K-2).
- **Present text (D-7):** "…`RECEIVE_SMS` is never requested at run time … The story that removes it deletes the class,
  its manifest entry, its enable/disable in `App` and its test."
- **Why:** the only applied sentence about the permission is "never requested at run time". A story writer reading the
  design alone could take it to mean the permission is unused and remove it together with "its manifest entry". Ruling 3
  keeps it: the `SMS_DELIVER` receiver (`compat/SmsReceiver`, manifest `:167-173`) needs it while the app holds the SMS
  role during a restore. The record binds the story, but the design should carry the fact, since the story plans
  against the design.
- **Replacement:** append to the D-7 bullet: "`RECEIVE_SMS` (`AndroidManifest.xml:17`) stays: the default-SMS
  `SMS_DELIVER` receiver (`compat/SmsReceiver`) needs it while the app holds the SMS role during a restore (CR-017,
  ruling 3). The story checks, by static inspection of the merged manifest, that no `<receiver>` for `SMS_RECEIVED`
  remains and that `RECEIVE_SMS` is still declared." Append to K-2: "`RECEIVE_SMS` is kept (ruling 3)."

### S-7 (should-fix, record text): the "if the owner answers yes" paragraph is incomplete

- **File, line:** `sdlc/artifacts/build/change-records/CR-017-cntr-004-scheduler-never-boot-example-and-receiver.md:412-432`.
- **Why:** see §4, items 1 to 7. The paragraph says "Only these sentences change; nothing else in v4 does". That is not
  so: it omits C-4 sentence 1, the C-5 trailing sentence (and the `BOOT` row is missing, not merely changed), Rule 4's
  "starts no separate boot backup", C-15 item 2, C-13 (under PO-2), and DES-005 D-2, D-3, D-6 and D-16.
- **Replacement:** add these places to PO-1 items 1 to 4 (with "`BOOT` row: **added** to the C-5 table" instead of
  "reads"). Replace "Only these sentences change; nothing else in v4 does" with "These sentences change; before applying
  a 'yes', re-run the record's verification searches for `one-off`, `one-time`, `BOOT` and `separate boot backup` over
  the seven files."

### S-8 (should-fix, low): "is removed" in present tense while the class is still in the code

- **Files, lines, present text:**
  - DES-014 :1423 (E-1): "(the SMS-received receiver is not delivered the broadcast and is removed, CR-017)"
  - DES-014 :1339 (E-3): "(the SMS-received receiver is removed, CR-017)"
  - REQ-005 :90 (R-1c): "has not been delivered the broadcast since U-001 and is removed"
- **Why:** K-2 says the class, its manifest entry and its toggle are still in the code. Each file has a clarifying
  sentence nearby (E-4, R-1b), but the parentheticals read as statements about the code today.
- **Replacement:** in each, "is removed" becomes "is to be removed by its own story (ruling 56)".

---

## 7. Notes (no action required by this record)

- **N-1, INV-1 clause.** The clause "Enqueuing a job kind that is already pending MUST replace it" (contract :274) sits
  beside a realization in which `UPDATE` keeps the registration and its `id`, and beside Rule 4's "MUST NOT … reset".
  The record rules the clause untouched (ruling 1; "Invariant and rule texts after the change"), so this is left by
  ruling. A future revision could read "MUST replace or update it in place".
- **N-2, Consumers row (C-1, :44).** It omits `MainViewModel` (`scheduleManual`, `scheduleRestore`; `MainViewModel.kt:143`,
  `:179` at `fd70325a`). It lists the package-replaced receiver, which only emits `AutoBackupSettingsChanged`, so `App`
  is the actual caller. This predates CR-017 and is hedged by the "Open verification items" bullet (:453). For the
  housekeeping record of ruling 7.
- **N-3, `CompositeScheduler`/`LegacyScheduler` sentences** not on ruling 7's list: contract :270; DES-005 :141-156,
  :260-261, :366, :407-412. For the housekeeping record.
- **N-4, INV-4 note test gate (contract :279, CR-015 text).** "…are removed and retargeted by story S1 … This contract
  states the target the code reaches with S1." The code has no `SDK_INT` branch at `fd70325a`
  (`WorkManagerScheduler.kt:57-59`, `:207-209`), so the code part of S1 has landed. This is not a CR-017 subject; for the
  housekeeping record.
- **N-5, INV-4 clause call-log wording (:277).** "when call-log-after-call is enabled" has M-2's gap. It is left by the
  record's "INV-4 clause: Untouched"; adjust it in the next contract revision.
- **N-6, record bookkeeping.** CR-017 Metadata says "Status | Open (draft)" (:49) while the frontmatter says `approved`.
  "Changes Made" says "Not applied (record is `draft`)" (:1405). The Fix Checklist is unticked and Commit Traceability is
  empty. The record's own unique-name table (:396) has a `BOOT` row that the applied C-5 table does not. Fill these in
  at commit, together with the amendment.

---

## Rework

1. Amend CR-017 (as CR-015's Amendment 1) with M-1 and M-2, then apply them. Recommended in the same amendment: S-1 to
   S-6 and S-8 as edits to the seven files, and S-7 as a change to the record's own paragraph.
2. Re-run the record's verification searches. Add `VERIFIED universal`, `isCallLogBackupAfterCallEnabled`,
   `contentTrigger`, `stage 1` and `is removed` as patterns over the contract and DES-005 (and `is removed` over DES-014
   and REQ-005).
3. Fill in CR-017's Execution section and Metadata status (N-6).

---

## Could not verify

- **WorkManager `UPDATE` behaviour:** that `UPDATE` fails on a one-time entry under a periodic name, and what it does
  with the 60 s initial delay of a registration that has not completed a period. C-3's "keeps an existing registration
  (its id and schedule)" rests on BUG-027's reading of the library; I did not read the library. If the delay applies
  from the original enqueue time, a registration that has not yet run may run shortly after a restart, and "its
  schedule" would be an over-promise. Recommend that the fix story's plan confirms it.
- **The platform rule that `SMS_DELIVER` is delivered only to a holder of `RECEIVE_SMS`** (ruling 3). Recalled by the
  record; not fetched.
- **Delivery facts on a device** (the receiver has 0 deliveries since U-001). Taken from BUG-023 and the emulator
  evidence; not re-run.
- **Which contract sentence the appliers meant by ":206".** Line 206 is the `CONNECTED` line. I checked the whole
  `SchedulerConfig` block instead (M-1, M-2).
- **The working copies' line endings before the edits.** Only the repository blobs (LF-normalised) and the current
  working tree were compared. The diff shows no whole-file change, and each file is uniform today.

---

## Delta check after Amendment 1 (2026-11-21)

Scope: CR-017 section "Amendment 1 (2026-11-21): texts the applied-text check found" (record :1216-1577), and the
working-tree diff of the seven files. Read-only. Code only at `fd70325a` (`git show`). No Gradle.

### (1) The amendment edits are applied word for word, and nothing else changed

| Edit | Location (working tree) | Result |
|------|--------------------------|--------|
| C-22 | contract :73-75 (3-line block) | Matches |
| C-23 | contract :196-198 (3-line block) | Matches |
| C-24 | contract :227-229 (3-line block) | Matches |
| C-25 | contract :319 (Rule 4, middle sentences; the rest of the rule unchanged) | Matches |
| C-26 | contract :292 (K-1, last sentence) | Matches |
| C-27 | contract :152-154 (3-line block) | Matches |
| C-28 | contract :159 (substring; the rest of the line kept) | Matches |
| C-29 | contract :426 | Matches |
| C-30 | contract :283 (INV-4 note, directly after the `scheduleIncoming()` sentence, before "Test gate:") | Matches |
| D-20 | DES-005 :318 | Matches |
| D-21 | DES-005 :322 | Matches |
| D-22 | DES-005 :264 (third cell) | Matches |
| D-23 | DES-005 :378 (the D-9 sentences replaced; the original first sentences of the cell kept) | Matches |
| D-24 | DES-005 :382 (test cell) | Matches |
| D-25 | DES-005 :443 | Matches |
| D-26 | DES-005 :458 (second cell; the "Source of truth" cell kept) | Matches |
| D-27 | DES-005 :323 (after the third sentence of the D-7 bullet) | Matches |
| E-5 | DES-014 :1423 | Matches |
| E-6 | DES-014 :1339 | Matches |
| R-1d | REQ-005 :90 | Matches |

Method:

- Each New block was taken by line range from the record. It was found exactly once: as consecutive lines for the
  3-line blocks, and as a substring for the others. Every replaced Old text now has 0 occurrences.
- **Nothing else changed since the first check.** I reversed the 20 edits in scratch copies (New replaced by Old, each
  found exactly once) and compared them with `HEAD`. For the contract, the changed lines are byte-identical to the diff
  saved at the first check (same SHA-256 of the `+`/`-` lines). DES-005, DES-014 and REQ-005 return to exactly the
  first check's 36, 13 and 5 changed lines. DES-008, DES-001 and REQ-001 still show 7, 5 and 3 changed lines,
  unchanged since the first check.
- Frontmatter: no `-`/`+` line touches `id`, `status` or `artifact_type`.
- Fences: contract 22, DES-005 4, DES-014 4, REQ-005 0, the same as before.
- Column counts of the amended rows: DES-005 :264, :443, :458 have 3; :378, :382 have 2.
- Line endings: the contract has a CR on every line (461/461) and DES-005 likewise (517/517). DES-014 (2281 lines) and
  REQ-005 (90) contain no CR.
- X-1 and X-2 are not applied; K-1 (:292) and K-2 (:293) are present.
- The applier's searches (record :1570-1576) give the expected results:
  - `VERIFIED universal`: 0.
  - `isCallLogBackupAfterCallEnabled`: every hit in the contract and DES-005 is paired with "call-log backup is enabled"
    or "both enabled" (:74, :229, :418, :431; DES-005 :318, :322). The INV-4 clause is left by ruling.
  - `contentTrigger`: only DES-005 :136 and :316 (legacy), the quotations at :154 and :426, and test names.
  - `CONTENT_TRIGGER_TAG`: only legacy citations.
  - `stage 1`: only "(stage 1)" at DES-005 :264.
  - Rule 4 no longer forbids the cancel without "With a positive regular interval" before it.

### (2) The first check's findings, judged on the applied text

| Finding | Applied text | Status |
|---------|--------------|--------|
| M-1 | C-23, contract :196-198 | **Closed.** It now agrees with the enum (:220-223), the unique-name table and both examples |
| M-2 | C-22, C-24, D-20, D-21 | **Closed.** Each of the four places names both conditions. This is true against `WorkManagerScheduler.kt:227-228` and legacy `BackupJobs.java:180`. The port javadoc is handed to the fix story; the INV-4 clause is left by the record's "INV-4 clause: Untouched" ruling (N-5) |
| S-1 | C-25, contract :319; D-24 (3) | **Closed, and better than proposed.** Rule 4 now limits the "MUST NOT cancel or reset" clause to a positive interval. Under "Never" it requires the cancel, names the upgrade case and defers to Rule 4a. Rule 4 and Rule 4a no longer disagree. The upgrade case is a test obligation: D-24 (3) seeds the entry the code at `fd70325a` registers for "Never" (periodic, 900 s, tag `REGULAR`: `WorkManagerScheduler.kt:180-197`). Both of its calls fail at `fd70325a`: `scheduleRegular()` keeps the clamped periodic entry under `UPDATE`, and `scheduleBootup()` leaves a non-terminal one-time entry (`:273-281`). See note D-1 |
| S-2 | D-22, DES-005 :264 | **Closed.** "(stage 1)" and "(stage 2)" now agree with D-10 (:381) and `BackupTriggerWorker.kt:39-41` |
| S-3 | C-27, C-28, C-29, D-25, D-26 | **Closed, and the extension is correct.** Contract :152-154 and :159 were present-tense statements of the current tag and unique name. I had read them as legacy description; they now carry the code's value (`WorkManagerScheduler.kt:93`, `:238`, `:243`, `:405`) and quote the legacy constant as such. What is left as history is legacy description: DES-005 :136 and :316 describe the legacy `contentTrigger` job, and the third cell of :458 is a legacy citation |
| S-4 | D-23, D-24 | **Closed.** See below |
| S-5 | C-26, contract :292 | **Closed.** K-1's "Affected here" now matches the record's "Order" table. The new upgrade obligation is covered by "Rules 4 and 4a" |
| S-6 | C-30, contract :283; D-27, DES-005 :323 | **Closed.** Putting the sentence in the contract's receiver note instead of K-2 is better: K-2 is deleted at X-2, and the fact must outlive it. The sentence is true: `AndroidManifest.xml:17`; `compat/SmsReceiver` is declared for `SMS_DELIVER` at `:167-173` and stores messages while the app holds the SMS role. The D-27 static check of the merged manifest (no `SMS_RECEIVED` receiver, `RECEIVE_SMS` declared) gives the receiver story an obligation that cannot be met vacuously |
| S-7 | record :1492-1550 | **Mostly closed; one residual for PO-2 (DS-1 below).** Against the applied text, including the amendment's own edits, PO-1 is complete. I checked every hit of `one-off`, `one-time`, `BOOT`, `boot path`, `at boot`, `separate boot backup`, `no unique name`, `REGULAR`-tagged and `getWorkInfosByTag` in the contract, DES-005 and REQ-005. Every hit that a "yes" would make false is in items 1 to 10, or is explicitly kept: Rule 4a, the v4 changelog as history, K-1, C-21 (:461, still true because `BOOT` is not `REGULAR`), D-18 and D-19. The `BOOT` row is now "added", and the sentence under the C-5 table is listed. The original paragraph is kept and marked superseded (:1494) |
| S-8 | E-5, E-6, R-1d | **Closed.** The receiver is no longer stated as already removed in DES-014 :1339, :1423 or REQ-005 :90 |

**S-4 in detail: can the rows still be satisfied vacuously? No.**

- **Preconditions.** Both rows now state "auto backup on, encryption not degraded". D-23 also fixes 3600 as the
  interval. This removes the vacuous pass through `WorkManagerScheduler.kt:175-178` and `:267-271`.
- **Observable results.** Each D-23 group names the state, kind, `id`, `generation`, interval, delay, constraint or tag
  that the test must read. Group (3) names the seeding method (directly through WorkManager), so the test does not
  depend on the old code. The three sequences in (4) are written out. "Exactly one non-terminal item, periodic, under
  `REGULAR`" makes (4) fail at `fd70325a`, which the proposed "at most one" did not: today's `REPLACE` also leaves one
  item, but it is one-time. The author's correction of my proposal is right.
- **Every D-23 group fails at `fd70325a`.**
  - (1): the boot path replaces the periodic entry with a new one-time id.
  - (2): the entry is one-time.
  - (3): in the `ENQUEUED` case, `scheduleRegular()`'s `UPDATE` leaves the one-time entry and `scheduleBootup()`
    replaces it with another one-time entry.
  - (4): the first sequence ends with a one-time entry.

  The `CANCELLED` cases of (3) and the third sequence of (4) depend on how WorkManager's `UPDATE` treats a cancelled
  one-time entry, which I did not verify. Each group has at least one case that fails regardless, so "each of (1) to
  (4) fails" can be met.
- **The departure for the "Never" row's interval tests is acceptable.** They are written as guards that pass at
  `fd70325a` by design (BUG-026 AC-3, AC-4) and are not counted as gate evidence. They cannot be satisfied vacuously:
  they assert `repeatIntervalMillis` equal to a given value, which needs an enqueued periodic entry (under the row's
  stated preconditions). They pin that the fix does not turn small positive values into "Never". The gate evidence of
  the row is (1) to (3), and each fails at `fd70325a`: (1) the clamp registers 900 s; (2) the boot path enqueues a
  one-time entry; (3) see S-1. "Every new test must fail at `fd70325a`" would contradict BUG-026's own ACs for the
  guards, so the row's narrower wording is the correct one.

### (3) DES-014 :56 (the E-4 note): "is not delivered `SMS_RECEIVED` and is removed by its own story"

**Acceptable as it stands.** Unlike the E-1 and E-3 parentheses, which said "is removed, CR-017" with no agent and so
read as a statement about the code today, this sentence names the agent ("by its own story"). It states who removes
the receiver, which is true before and after that story. The record keeps the same construction in the contract's v4
changelog (:297, "it is removed by its own story"), and the amendment ruled that true before and after (record :1261).
"Is not delivered `SMS_RECEIVED`" is true now and stays trivially true once the class is gone.

Optional, for uniform wording with E-5, E-6 and R-1d (not required): replace "and is removed by its own story
(product owner's" with "and is to be removed by its own story (product owner's".

### (4) What the amendment's edits newly contradict

No sentence in the seven files is made false by C-22 to C-30, D-20 to D-27, E-5, E-6 or R-1d. Checked:

- C-23 against the enum, the examples and C-10.
- C-25 against C-3 ("the same 'Never' rule"), Rule 4a, D-12, D-13 and K-1.
- C-27 to C-29 against the unique-name table and DES-005 :381 and :443.
- C-30 against the earlier "never requested at run time" sentence of the same note, and against `RECEIVE_MMS`, which no
  text mentions.
- D-22 against D-10.
- D-23 and D-24 against D-18, the changelog's "gated by the boot tests", and the record's "kept green with no edit"
  list (`setUp` stubs 3600 and auto backup on; `WorkManagerSchedulerTest.kt:71`, `:73` at `fd70325a`).

Notes (no action required):

- **D-1.** Rule 4's "With a positive regular interval it MUST NOT cancel or reset a registered or running regular
  backup" sits beside the permitted `CANCEL_AND_REENQUEUE` fallback, which cancels a one-time `REGULAR` entry left by an
  earlier build. A reader could call that entry "a registered … regular backup". The same rule names the fallback, so
  the reading is resolvable. A future revision could add "(a periodic one)" after "regular backup".
- **D-2.** D-13 (DES-005 :440) says "'Never' registers nothing" without "and cancels `REGULAR`", which D-12 and Rule 4
  state. It is incomplete, not false.

### Delta findings

**DS-1 (should-fix, record text): PO-2 "yes" would make D-23 (4) false, and the paragraph does not list it.**

- **Place:** record :1547 (PO-2 item 4), against DES-005 :378, D-23 group (4).
- **Present text:** D-23 (4) reads "…`getWorkInfosByTag("REGULAR")` holds exactly one non-terminal item, it is periodic
  and it is under `REGULAR`, and no other unique name holds a `REGULAR`-tagged item." PO-2 item 4 says only "the INV-1
  row gains the guard test".
- **Why:** under PO-2 every boot also enqueues the one-time `BOOT` backup with tag `REGULAR` (PO-1 item 3's row,
  reused by PO-2). After the two sequences containing `scheduleBootup()`, two non-terminal `REGULAR`-tagged items exist,
  one of them under `BOOT`. BUG-027 AC-4 has the same wording; it is story text, outside the seven files. Under PO-1,
  D-23 runs at 3600, so there is no conflict.
- **Replacement for PO-2 item 4:** "DES-005 D-9/D-23: the INV-1 row gains the guard test, and in (4) '…holds exactly
  one non-terminal item, it is periodic and it is under `REGULAR`, and no other unique name holds a `REGULAR`-tagged
  item' becomes '…holds exactly one non-terminal periodic item, under `REGULAR`, and at most one other non-terminal
  `REGULAR`-tagged item, the one-time `BOOT` backup'; BUG-027 AC-4 follows. D-18 (Risks) gains 'two backups of the
  same items when `BOOT` and `REGULAR` overlap', mitigated by the guard."

### Earlier findings

| Finding | Status |
|---------|--------|
| M-1, M-2 (must-fix) | Closed by C-23; C-22, C-24, D-20, D-21 |
| S-1 to S-6, S-8 | Closed by C-25 and D-24; D-22; C-27 to C-29, D-25, D-26; D-23, D-24; C-26; C-30, D-27; E-5, E-6, R-1d |
| S-7 | Closed for PO-1; one PO-2 residual (DS-1) |
| N-1 to N-5 | Unchanged; left by ruling or to the housekeeping record of ruling 7 (record :1262) |
| N-6 bookkeeping | The Metadata status is corrected by the amendment. Execution, Commit Traceability and Closure remain for the orchestrator at commit |

Verdict after Amendment 1: PASS

Still open, non-blocking:

- DS-1 (a one-line completion of the record's PO-2 item 4).
- The optional DES-014 :56 wording.
- Notes D-1 and D-2.

Not verified in this delta:

- WorkManager's handling of `UPDATE` over a `CANCELLED` one-time entry (the `CANCELLED` cases of D-23 (3) and the
  third sequence of D-23 (4)).
- The initial-delay behaviour of `UPDATE` (from the first check).
- The `SMS_DELIVER`/`RECEIVE_SMS` platform rule (from the first check).

---

## Quick check of Amendment 2 (2026-11-21)

Scope: CR-017 section "Amendment 2 (2026-11-21): the foreground sentence of DES-014 row 16-1, DS-1, one wording"
(record :1594-1676), and the working tree of DES-MODERNIZATION-014. Read-only. Code only at `fd70325a`. No Gradle.

### (1) E-7 and E-8 are applied word for word, and nothing else changed

| Edit | Location | Result |
|------|----------|--------|
| E-7 | DES-014 :1282, row 16-1, third cell | Matches: the New text (record :1631) occurs once, and the Old text 0 times |
| E-8 | DES-014 :56 (E-4 note) | Matches: the New text (record :1641) occurs once, and the Old text 0 times |

- Row 16-1 is still one line with 4 columns, the same as at `HEAD`. Cells 1, 2 and 4 are unchanged.
- **Nothing else changed.** With E-7 and E-8 reversed in a scratch copy, the file's diff against `HEAD` is exactly the
  delta check's 10 added and 3 removed lines. The other six files still show the delta check's numbers (contract 86/36,
  DES-005 24/18, DES-008 5/2, DES-001 4/1, REQ-005 4/1, REQ-001 3/0).
- Line endings: DES-014 has 2281 LF and no CR.
- X-3 is not applied, as ordered.

### (2) Truth of the new 16-1 cell

**"Today" half: true at `fd70325a`.**

- "Today every backup asks for a `dataSync` foreground service": the first statement of `BackupWorker.doWork` is
  `setForeground(createBackupForegroundInfo())` (`BackupWorker.kt:123`). Its `ForegroundInfo` carries
  `FOREGROUND_SERVICE_TYPE_DATA_SYNC` (`:535`). Nit: on API 24 to 28 the info carries no type (`:537`). The row belongs
  to the Android 16 table, where the type applies, so this is not a finding.
- "a start the system refuses (off screen, Android 12 and later) fails the run (BUG-029)": the device record shows it.
  Run 1, 09:28:57: the `REGULAR` boot backup was refused with `ForegroundServiceStartNotAllowedException`, then
  "`REGULAR FAILED`". Run 2, 09:47:36: an `INCOMING` backup, "Worker result FAILURE". The record's summary says both
  failed at `setForeground`, the first statement of `doWork`.

**"From story U-075" half: consistent with the story.**

- Automatic types (`INCOMING`, `REGULAR` including the boot run, `BROADCAST_INTENT`, and a run with no tag) do not ask
  for the foreground: story Terms :84, AC-3.
- `MANUAL` and `SKIP` still ask: AC-4. Every restore still asks: AC-6.
- A refused user-started start continues: AC-1, AC-6, and the warning in Terms :88.
- "Ordinary jobs under the job quota" restates the story's title ("ordinary background work") and its out-of-scope line
  (:77, no expedited work, no scheduling change). The quota is the platform consequence named in cell 2
  (`#job-quota-opt`), which the story does not contradict.
- The cell omits the no-tag case. That is acceptable for a summary.

**X-3 (record :1648) is well-formed.** It is one cell with no pipe character. It drops exactly the "Today …" sentence,
names U-075, keeps "WorkManager runs on JobScheduler", and is true after U-075 by the same story references. Cell 4
("REQ-016 AC-13 scheduled run") stays consistent with it.

### (3) DS-1

**Closed.** PO-2 item 4 (record :1557-1561) now carries the replacement I proposed, with the author's clarification
("the one-time `BOOT` backup under the unique name `BOOT`"), which makes the second item exact. It is marked
"(Replaced by Amendment 2, DS-1.)". BUG-027 AC-4 and D-18 follow as proposed.

The record's E-3 correction (:255-258) is a dated note under the sentence it corrects. It names the one place (row
16-1) and why the original search missed it (no pattern for "long work").

### (4) Spot-check of the foreground search

I ran `git grep -n -i` over `sdlc/artifacts/design` and `sdlc/artifacts/requirements` with two sets of patterns:

- `long work`, `long-running`, `foreground service`, `setForeground`, `progress notification`, `in the foreground`,
  `foreground (promotion|notification|worker)`, `runs as a … foreground`, `shows … notification`;
- `(scheduled|automatic|incoming|regular|periodic|background)` within 60 characters of `foreground`, in either order.

| Hit | Disposition |
|-----|-------------|
| DES-005 :55, :132; REQ-005 :42 | A capability ("owns … foreground promotion") and a conditional ("where a user-visible notification is required"); true |
| DES-007 :273 | The retired services' notification moving into the workers; history |
| DES-014 :1109 | A library release note |
| DES-014 :1282 | Row 16-1, now E-7 |
| DES-014 :1305 (AC4-a) | The manifest invariant; "AC-9 starts the foreground worker" refers to the manual backup of REQ-017 AC-9, which stays true |
| DES-014 :1487 | The operator; unrelated |
| DES-014 :1918; REQ-017 :129 | A manual backup; true after U-075 |
| REQ-016 :49-53 | "`BackupWorker.kt` and `RestoreWorker.kt` call `setForeground()` with `FOREGROUND_SERVICE_TYPE_DATA_SYNC`": true after U-075 (still called, for user-started runs); a description for the merged-manifest check |
| The second pattern set | Only DES-014 :1487 (unrelated) |

The historical U-001, DES-012 and REQ-013 hits of the first pattern set are about the retired services and the manual
path, as the author's table says. None of them is caught by the second pattern set, which would match a sentence about
scheduled or automatic work in the foreground.

**The claim holds.** Row 16-1 was the only present-tense sentence in the designs and requirements saying that automatic
or scheduled work runs in the foreground or shows the progress notification.

Quick check of Amendment 2: PASS

Left:

- X-1, X-2 and X-3 are for the orchestrator at each story's close.
- The record's Execution, Commit Traceability and Closure sections are to be filled in at commit (N-6).
- Notes D-1 and D-2 of the delta check (no action required).

Not verified: as before, the WorkManager `UPDATE` details and the `SMS_DELIVER`/`RECEIVE_SMS` platform rule. U-075's
"from the story" behaviour is checked against the story text only, not against code; U-075 is not built at `fd70325a`.
