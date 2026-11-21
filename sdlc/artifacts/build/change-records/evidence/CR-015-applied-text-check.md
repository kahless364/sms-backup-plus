# CR-015 applied-text check

| Field | Value |
|-------|-------|
| Record | `sdlc/artifacts/build/change-records/CR-015-cntr-004-content-trigger-pre-api-24-fallback-removed.md` (approved) |
| Files checked | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md`, `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md`, `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md` (uncommitted working tree, `git diff`) |
| Code read | `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt`, `app/src/main/java/com/zegoggles/smssync/worker/BackupTriggerWorker.kt`, `app/src/main/java/com/zegoggles/smssync/receiver/SmsBroadcastReceiver.java`, `app/src/main/AndroidManifest.xml`, `app/build.gradle`, `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt` |
| Date | 2026-11-21 |
| Method | Read-only. No artifact or source edited. No Gradle run. |

Verdict: FAIL

The edits match the record word for word, and nothing else in the three files changed. The verdict is
FAIL because of one must-fix: an unedited sentence in DES-005 (finding 1) contradicts the edited
text, and no planned work would ever correct it. Findings 2 to 4 are should-fix. All four are
one-line edits that the record did not order, so they need a short amendment to CR-015, or a follow-on
record, before they are applied. The ruling, the v3 step and the code-truth of the edited text all hold.

---

## 1. Edits applied as the record orders

| Edit | Location after edit | Result |
|------|---------------------|--------|
| C-0 | contract :5-14 (frontmatter) | Matches |
| C-1 | contract :41 | Matches |
| C-2 | contract :66-68 | Matches |
| C-3 | contract :173 | Matches |
| C-4a/b/c | contract :243 | Matches |
| C-5 | contract :245 (after the INV-4 row, before the "Degenerate execution window" quote) | Matches the Suggested Approach replacement, which is the same as the table text |
| C-6 | contract :251 | Matches |
| C-7 | contract :255 (above v2) | Matches (items 2 and 4 in amended form) |
| C-8 / C-9 | contract :266 / :267 | Match the amended form |
| C-10a / C-10b | contract :369 / :387-389 | Match |
| C-11 | contract :411 (after the v2 implementation note) | Matches the amended form ("moves to API 24") |
| D-0 | DES-005 :15-16 | Matches |
| D-1, D-2, D-3 | DES-005 :89, :239, :282 | Match |
| D-4 | DES-005 :318-325 | Matches, including the amended second bullet and ", for incoming SMS only" in "History:" |
| D-5 | DES-005 :379 | Matches the amended last sentence |
| D-6 | DES-005 :436 | Matches |
| D-7 | DES-005 :511 | Matches the amended form ("now specifies the API 24 assertion") |
| R-1a | REQ-005 :17-18 | Matches |
| R-1b | REQ-005 :87 | Reads "no test of it is required". The record's final order is that wording: the table at CR-015 :442 and the Suggested Approach ruling (CR-015 :542-543, :615: drop "or possible"). Confirmed |

The diff contains only these hunks (contract 31 lines, DES-005 40 lines, REQ-005 5 lines). `id`, `status`
and `artifact_type` are untouched in all three files. The REQ-005 LF/CRLF warning is a working-copy
notice; the diff shows no line-ending change across the whole file.

## 2. Truth against today's code

- The branch still exists: `WorkManagerScheduler.kt:224-230` (`SDK_INT < VERSION_CODES.N`, logs, `return null`). The build declares `minSdkVersion 21` (`app/build.gradle:24`). No edited sentence claims that the deletion has already happened: C-1 (contract :41), C-5 (:245, "are removed and retargeted by story S1 ... states the target"), C-7 item 7, C-9, C-11, D-4 (:325, "after story S1"), D-5 (:379, "the test that carries this after story S1") and D-7 all put it in S1. This complies with option A.
- C-4c (contract :243, "the triggered worker enqueues the delayed `OneTimeWorkRequest(initialDelay = incomingTimeoutSecs)` as unique work `INCOMING` (REPLACE)") is true: `BackupTriggerWorker.kt:61-71`.
- C-5 and the D-4 second bullet (receiver conditions; calls `scheduleIncoming()`; no version check) are true: `SmsBroadcastReceiver.java:58-64`, with the conditions `autoBackupEnabled && loginInformationSet && !firstBackup`. The manifest entry is at `AndroidManifest.xml:146-153` (`SMS_RECEIVED`, `exported="false"` at :148). "That enqueues ... `INCOMING`, REPLACE" is true when `scheduleIncoming()` does not return early (`WorkManagerScheduler.kt:135-144`). That is acceptable at contract level.
- D-5 test names exist: `inv4_contentTrigger_callLogEnabled_bothUrisPresent` (:332) and `inv4_contentTrigger_callLogDisabled_onlySmsUri` (:356), both under the class-level `@Config(sdk = [29])` (:43). `inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast` is at :377-384 (`sdk = [23]`, asserts `isNull()` only). `CONTENT_TRIGGER_UNIQUE_NAME = "CONTENT_TRIGGER"` (`WorkManagerScheduler.kt:94`). The scheduler tests are in `app/src/test/java/com/zegoggles/smssync/service/`. The directory `.../scheduler/` named in the dispatch does not exist.
- One sentence is false today. See finding 4 (REQ-005 :87).

## 3. Findings

### F-1 (must-fix): DES-005 Components row still says the adapter version-branches the content trigger (leftover a)

- Location: DES-005 :258, `WorkManagerScheduler` row: "Sole production impl; maps port calls to `WorkRequest`s; version-branches content-URI trigger".
- Why: C-1 removed this same phrase from the contract, so that the text is "true before and after S1". DES-005 :325 (D-4) now says that the content trigger "needs no version branch". The row contradicts D-4 and becomes false when S1 lands. S1 plans no DES-005 edit, so nobody would correct it later. The record's verification grep (CR-015 :750) has no pattern that matches "version-branches", which is why the row was missed.
- Ruling on leftover a: **must fix now.**
- Replacement (cell 3 of the row): `Sole production impl; maps port calls to `WorkRequest`s; the content-URI trigger needs no version branch at the minSdk 24 floor (CR-015; the code branch is removed by story S1 of DES-MODERNIZATION-014)`

### F-2 (should-fix): D-7 cites the wrong line (leftover b)

- Location: DES-005 :511, "...and the driver table (line 121) describe...".
- Why: line 121 is now the Firebase JobDispatcher row. The "API 14–22" row is at :122, moved down one line by D-0. A line number in a permanent note goes stale again with every edit.
- Ruling on leftover b: **fix now**, in the same edit as F-1.
- Replacement: change "the driver table (line 121)" to "the "AlarmManager fallback driver" row of the System Architecture table".

### F-3 (should-fix): the contract's Status note still says v2 and contains an absolute local path (leftover e)

- Location: contract :409, "- **Status:** approved (v2 amended in place per DES-MODERNIZATION-012 / REQ-MODERNIZATION-013). All paths relative to repo root (<absolute local repository path>)."
- Why: CR-015 created this contradiction. Before v3 the note was correct. It now disagrees with :251 ("Current version: v3"). The absolute path breaks the workspace path convention. It predates CR-015, but it is in the same sentence, so it is cheapest to fix now.
- Ruling on leftover e, line 409: **fix now.**
- Replacement: `- **Status:** approved (v3 amended in place by CR-015; v2 amended in place per DES-MODERNIZATION-012 / REQ-MODERNIZATION-013). All paths are relative to the repository root.`
- Ruling on leftover e, line 24 ("Draft; not finalized."): **leave.** It is older than CR-015, is not about the content trigger, and was ruled out of scope (CR-015 Scope Concern 4, :723-726). The next contract revision should clear it.

### F-4 (should-fix): the REQ-005 note states a test fact that is false until S1

- Location: REQ-005 :87, "...AC-3(d) is met by the content-URI trigger tests on every supported level, the lowest being API 24."
- Why: today the content-URI tests run at API 29 (`WorkManagerSchedulerTest.kt:43`), and the only lower-level test (:377-384, API 23) asserts `null`. No API 24 test exists until S1-I3. The record's "Order" list (CR-015 :466-477) missed this sentence, although the Suggested Approach reworded C-11 and D-7 for exactly this reason. An S1 author could take it to mean the API 24 test already exists.
- Replacement for that clause: `...AC-3(d) is met by the content-URI trigger tests; their lowest-level assertion is made at API 24 once story S1 of DES-MODERNIZATION-014 retargets `inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast` (CNTR-MODERNIZATION-004 v3, INV-4 note).`

### F-5 (note): the example at contract :384-385 is left on purpose (leftover c)

- The leftover premise needs a correction: C-4c and C-5 do not contradict :384. Both say only that the worker's follow-up is unique work `INCOMING`, REPLACE, and that is also what `scheduleIncoming()` produces. The text that conflicts with :384 is the code (`BackupTriggerWorker.kt:61-71` builds the request itself: no network constraint, no backoff, no degraded or auto-backup gate). The contract does not conflict with itself.
- Is leaving it defensible? Yes. The contract states the intended behaviour. The divergence is a code defect with its own scope and its own two-route decision (BUG-023 AC-10, which cites contract :384-385 correctly). Rewriting :384 to match the code inside CR-015 would decide BUG-023's Route B without its analysis.
- Can a reader of the contract alone tell which statement is true today? No. Nothing in the contract says the trigger path bypasses `scheduleIncoming()`. Ruling: **leave** in CR-015. Optional, once BUG-023 is approved: add a Notes pointer, "Known divergence: the trigger worker does not call `scheduleIncoming()` today (BUG-023)".
- Side note for the BUG-023 author: it cites DES-005 :504 for the Risks row (now :495) and :442 for the port table (now :434).

### F-6 (note): DES-005 Risks row, "INV-4 test asserts the debounce delay" (leftover d)

- Location: DES-005 :495. No test asserts the delay. The only mention of `initialDelay` in `WorkManagerSchedulerTest.kt` is the comment at :388. This is a claim about the debounce, not about the pre-24 branch.
- Ruling: **leave.** BUG-023 AC-7 provides that test and AC-10 updates the row.

### F-7 (note): the v3 "non-breaking" claim holds under the contract's own rules

- Consumers: the only edits inside the port and value blocks are comment changes (C-2 :66-68, C-3 :173). No signature, `BackupType` value or `SchedulerState` variant changed. Non-breaking by the rule at :266.
- Behavioural: read literally, the old rule ("any change to the four invariants") covers the change, because INV-4's wording changed. The v3 claim depends on "no invariant's substance" (:251), and C-8 changes the rule in the same version. That is defensible for three reasons: v2's own lines (:260 vs :261) contradicted each other, the old :261 made the adapter-internal API-level branch non-breaking, and the removed branch only returns `null`. The C-8 text states the self-amendment openly. The contract has no rule tying breaking or non-breaking changes to a version number, so v3 follows the whole-integer precedent (v1 to v2).
- Condition: the claim is true only while the branch is deleted together with or after the minSdk raise. DES-014 S1 already orders S1-I4 (minSdk 24) before S1-I5 (delete the branch). What WorkManager does with a content-URI trigger below API 24 if the branch were gone was not verified here.

### F-8 (note): points for the S1 (minSdk 24) author

1. Build against contract v3: C-8 :266, C-9 :267, and the INV-4 note :245.
2. The retargeted test must also assert `triggerForDescendants = true` (DES-005 D-5 :379, contract INV-4 :243). DES-014 table T (its :838) names only "the SMS URI trigger". DES-005 is the stricter text, and S1 should meet it.
3. `SmsBroadcastReceiver.java`, its manifest entry (`AndroidManifest.xml:146-153`) and `BackupTriggerWorker.kt` must show empty diffs. The `BackupTriggerWorker.kt:39` and `WorkManagerSchedulerTest.kt:537` comments belong to BUG-023, not S1.
4. Keep S1-I4 before S1-I5 (F-7).
5. The INV-4 tests are in `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt`, not in a `scheduler/` test package.

### F-9 (note): the record's own bookkeeping is not yet filled in

CR-015 Metadata says `Status | Open` (:36) while the frontmatter says `approved`. "Changes Made" says "Not yet applied" (:739). Verification Evidence and Commit Traceability are empty, and the Fix Checklist is unticked. The "Order" section (:496) requires checklist items 1 to 4 to be ticked before S1 is planned. Fill these in when the commit is made, together with the amendment that carries F-1 to F-4.

---

## Rework

1. Amend CR-015, or raise a follow-on record, to add F-1 (D-8), F-2 (D-7 wording), F-3 (C-12) and F-4 (R-1b wording) with the replacement text above. Then apply those edits.
2. Re-run the record's verification grep (CR-015 :750), adding the pattern `version-branch`.
3. Fill in CR-015's Execution section (F-9).

---

## Delta check after Amendment 1 (2026-11-21)

Scope: CR-015 section "Amendment 1 (2026-11-21): four texts the applied-text check found" (CR-015 :459-489) and the
working-tree diff of the same three files. Read-only; no Gradle.

### (1) The amendment fits the rest of the record and changes no ruling

- It is insert-only and adds D-8, D-9, C-12 and R-1c (:471-476). The ruling (not a behavioural break), the retargeting of INV-4 to API 24, option A and the "Order relative to the code" rules are restated as unchanged (:463). No other section of the record was altered for it.
- "No new version step" (:465) is right. None of the four edits touches a port signature, a `BackupType` value, a `SchedulerState` variant or an invariant, so the v3 claims of C-6 and C-7 still hold.
- The precedence rule for D-9 over D-7 and R-1c over R-1b (:463) is explicit. The original tables at :435 and :442 keep the superseded text as history, which is acceptable under that rule.
- The reason D-9 differs from the report's wording (:478) checks out. A second row "AlarmManager fallback driver" exists in the Overview table (DES-005 :40). The heading D-9 names exists verbatim at DES-005 :114, and the row it points to is :122.
- Minor, no action: Amendment note 1 (:486) says that BUG-023's design references moved "about nine lines". The actual shifts are :504 to :495 (Risks row) and :442 to :434 (port table).

### (2) The four edits are applied exactly as given, and are true today and after S1

| Edit | Location | Applied text | True today / after S1 |
|------|----------|--------------|-----------------------|
| D-8 | DES-005 :258 | Matches CR-015 :473 | Yes: "needs no version branch at the minSdk 24 floor ... removed by story S1", the same wording as C-1 (contract :41) and D-4 (DES-005 :325). The branch still exists at `WorkManagerScheduler.kt:224-230`, and the text does not claim it is gone |
| D-9 | DES-005 :511 (last sentence) | Matches CR-015 :474 | Yes: the row at :122 under the heading at :114 names API 14–22, and there is no line number to go stale |
| C-12 | contract :409 | Matches CR-015 :475 | Yes: agrees with "Current version: v3" (:251). No absolute path left (search for a drive-letter path in the three files: no match) |
| R-1c | REQ-005 :87 | Matches CR-015 :476; the rest of the R-1b note is unchanged | Yes: today the content-URI tests run at API 29 (`WorkManagerSchedulerTest.kt:43`) and the API 23 test (:377-384) still exists. "once story S1 ... retargets" is true before and after S1 |

The applier's extra check (CR-015 :482) passes. A search for `version-branch` in the three files finds no match.

### (3) Nothing else changed

`git diff --stat` is now contract 33, DES-005 42, REQ-005 5 lines, against 31, 40 and 5 at the first check. The listed changed lines differ from the first check only in:

- the contract Status bullet (+/- one line);
- the DES-005 Components row (+/- one line);
- the text of the two lines already added by D-7 and R-1b.

`id`, `status` and `artifact_type` are unchanged in all three files. The contract's "Draft; not finalized." line (:24) is untouched, as the amendment rules (:480).

### (4) Earlier findings

| Finding | Status |
|---------|--------|
| F-1 (must-fix) DES-005 Components row | Closed by D-8 |
| F-2 (should-fix) D-7 line reference | Closed by D-9 |
| F-3 (should-fix) contract Status bullet v2 / absolute path | Closed by C-12 |
| F-4 (should-fix) REQ-005 present-tense test fact | Closed by R-1c |
| F-5 to F-8 (notes) | Unchanged; recorded in Amendment 1 notes (:486-489) |
| F-9 (note) record bookkeeping | Still open by design: the orchestrator fills in CR-015's Metadata Status, Changes Made, Verification Evidence, Commit Traceability and Fix Checklist at implementation. The checklist should then name C-0 to C-12, D-0 to D-9, R-1 and R-1c (:488) |

Verdict after Amendment 1: PASS

Still open, non-blocking: F-9 bookkeeping at commit. The optional BUG-023 pointer in the contract's Notes (F-5) can be added once BUG-023 is approved.
