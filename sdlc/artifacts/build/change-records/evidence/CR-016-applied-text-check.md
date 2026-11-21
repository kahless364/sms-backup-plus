# CR-016 applied-text check (Requirements Reviewer)

| Field | Value |
|-------|-------|
| Record | `sdlc/artifacts/build/change-records/CR-016-requirements-wording-overtaken-before-des-014-stories.md` (frontmatter `status: approved`) |
| Checked | Working tree on branch `sdlc/modernization-plan`, HEAD `9e983665`, 2026-11-21 |
| Files | 8 requirement files under `sdlc/artifacts/requirements/modernization/`, plus `sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-...-design.md` |
| Method | Read and Grep only. This agent had no shell tool, so `git diff`, `git grep <commit>` and `git log` were **not run**. Checks of "nothing else changed" are made from line-offset consistency against the line numbers the record quotes (see section 1); the count at `9e983665` is corroborated, not reproduced (section 2) |

## Verdict: FAIL

One must-fix (F-1: the test-count rule leaves open where the end count is taken and which commit is the start commit; the project's own evidence shows both cases happen). Three should-fix items (F-2 to F-4). Every one of the 26 edits is applied with the record's final text.

---

## 1. Edits applied (file:line in the working tree)

| Edit | Where it is now | Result |
|------|-----------------|--------|
| A-1 | `REQ-MODERNIZATION-015-...:165-174` | Verbatim |
| A-2 | `REQ-MODERNIZATION-017-...:158-166` | Verbatim |
| A-3 | `EPIC-MODERNIZATION-006-...:121-127` | Verbatim |
| A-4 | `REQ-MODERNIZATION-016-...:43-45` | Verbatim |
| A-5 | `REQ-MODERNIZATION-019-...:113` | Verbatim |
| A-6 | `REQ-MODERNIZATION-019-...:61` (opening only), `:19` (`CR-016` after `CR-011`), `:143` (dated line after the CR-011 paragraph) | Verbatim |
| B-1, B-2 | `REQ-MODERNIZATION-001-...:12-13`, `:18` (block quote directly under the H1 at `:16`) | Verbatim |
| B-3, B-4 | `REQ-MODERNIZATION-006-...:16-17`, `:24` (under the H1 at `:22`) | Verbatim (final B-4 wording, without "and that was verified") |
| B-5 | `REQ-MODERNIZATION-017-...:202-204` | Verbatim |
| B-6 | `REQ-MODERNIZATION-015-...:32` (after the CR-011 note) | Verbatim |
| C-1 | `REQ-MODERNIZATION-016-...:95-99` | Verbatim (last sentence of the draft is cut, as ruled) |
| C-2 | `EPIC-MODERNIZATION-006-...:128-133` | Verbatim |
| D-1 | `EPIC-MODERNIZATION-006-...:165-171` (second Sequencing bullet) | Verbatim |
| D-2 | `EPIC-MODERNIZATION-006-...:147-150` | Verbatim |
| D-3 | `REQ-MODERNIZATION-017-...:26-29` | Verbatim |
| D-4 | `REQ-MODERNIZATION-017-...:8-9` | Verbatim |
| D-5 | `REQ-MODERNIZATION-017-...:214-225` | Verbatim (final wording "decision Record 13 row 4; AC-5 itself allows no later than the same PR") |
| D-6 | `REQ-MODERNIZATION-020-...:13-14` | Verbatim |
| D-7 | `REQ-MODERNIZATION-020-...:77` | Verbatim |
| D-8 | `REQ-MODERNIZATION-020-...:461-462` | Verbatim |
| D-9 | `REQ-MODERNIZATION-020-...:511` | Verbatim |
| D-10 | `EPIC-MODERNIZATION-006-...:212` | Verbatim (with "and the Description's list of deliberate behaviour changes") |
| D-11 | `EPIC-MODERNIZATION-006-...:30-32` | Verbatim |
| X-1 | `DES-MODERNIZATION-014-...:44-52` (under the H1 at `:42`); `change_records` `:33-34` | Verbatim; `CR-015`, `CR-016` appended after `CR-014` |

Body dated notes: REQ-015 `:246-248` and REQ-016 `:160-161` are present. Both add words beyond the record's quoted note (REQ-015 adds "which adds a dated note and `change_records` to REQ-MODERNIZATION-001 and REQ-MODERNIZATION-006. No other criterion is changed."; REQ-016 adds "No other criterion is changed."). The additions are true and harmless. They are departures and belong in the record's "Departures" section (F-4).

`change_records`: `CR-016` is appended after the existing entries in REQ-015 (`:15-16`), REQ-016 (`:12-13`), REQ-017 (`:15-16`), REQ-019 (`:17-19`) and the epic (`:7-9`, after `CR-010`). It is the only entry in REQ-001, REQ-006 and REQ-020, which had `[]`. No earlier entry was removed.

`status: approved` is present in all eight files and in the design. `id` and `artifact_type` match each file's name and type. `updated_at` was not touched (REQ-015 to -019 and the epic still read `2026-11-12...`).

Nothing else changed (offset check, not a diff). REQ-001: every quoted line moved by exactly +3, the lines that B-1 and B-2 add (AC-1 47→50, AC-2 48→51, criterion 13 82→85, AC-29 134→137, Notes 186→189, end of file 189). REQ-006: AC-3 46→49, AC-5 49→52, Constraints 59→62 (+3). REQ-015: AC-9 147→149, AC-10 150→152 (+2), Verification 219→228 (+2, and +7 from A-1). These match insert-only edits. **The `git diff --stat` and `git diff` checks in the record's Fix Checklist were not run and must still be run by the orchestrator** (F-4).

## 2. Facts in the applied text

- **732 at `9e983665` (corroborated, not reproduced).** U-066 evidence `sdlc/artifacts/build/sprints/sprint-022/U-066/evidence/I0-baseline.txt:2,14` gives 732 at `b9fd172f` from the authoritative command. `I0-junit-totals.txt:1` gives `tests=732 ... skipped=2`. `I9-checks.txt:1-2` gives 746 for the working tree and 732 at HEAD. The working tree now has 746 `@Test` occurrences in 78 files. The U-066 log (`implementation-log.md:118`) says "+14 added, 0 removed". The test files changed since `b9fd172f` are all U-066's uncommitted ones (git status). The commit message of `9e983665` names only artifacts.
- **2 `@Ignore`d (verified for HEAD).** The `@Ignore` annotations are `app/src/test/java/com/zegoggles/smssync/AppTest.java:13` and `.../preferences/AuthPreferencesTest.java:1079`. Neither file is modified in the working tree, so HEAD has the same two. A raw `grep -c "@Ignore"` gives 6 lines; the other 4 are comments (`AuthPreferencesTest.java:81,82,1070,1072`). See N-1.
- **Coverage wording (verified).** `app/build.gradle:340-589` has 18 `PACKAGE`/`LINE`/`COVEREDRATIO` rules in `violationRules` (`:340`). 5 are at 0.70 (`:363,374,385,464,490`) and 13 are below. `receiver*` is at 0.50 (`:534`). No package matches two rules, so "the floor configured for it" has a single meaning. REQ-016 AC-6 and epic criterion 8 are true against the file and agree with REQ-015 AC-14 (`:175-176`) and REQ-019 AC-B10 (`:114`).
- **Supersession notes (verified).** REQ-015 AC-5 (`:124`) sets `minSdkVersion 24`. REQ-017 AC-2 (`:89`) sets compileSdk and targetSdk 37. Both notes say "superseded", which is what REQ-015 AC-9 (`:149-151`) and check 2 of the record's D-5 need. REQ-006 AC-5 "API 21+" (`:52`) is named in its note.
- **D-1 and D-5 against design D14.** These match `DES-...-014:660` and row 37-5 (`:1287`): only "measured not affected" lets the targetSdk step go first, and "not run" waits. They also agree with REQ-020 Notes (`:466`, `:484-486`) and BUG-021 (`:71`).

## 3. Findings

### F-1 (must-fix): the test-count rule does not say where the end count is taken or which commit is the start commit

- **Where:** REQ-015 AC-13 (`:165-174`). The same rule is in REQ-017 AC-15 (`:158-164`) and REQ-019 AC-B9 (`:113`). REQ-016 AC-6 (`:98`) says "the story's start commit" without defining it.
- **What is wrong:**
  1. The authoritative command in `.claude/CLAUDE.md` is fixed to `HEAD`. AC-13 says "At the end of the story the same command gives a count equal to that start count, plus the tests added". If QA checks before the story's commit, the command returns the start count and the equation fails. This already happened on U-066: "the authoritative HEAD count stays 732 until the commit" (`U-066/implementation-log.md:118`; `I9-checks.txt:1-2`, 746 against 732). `git grep` without `HEAD` would also miss new, untracked test files.
  2. AC-13 says "the id of the commit it was taken at: the commit immediately before the story's first commit". When another commit lands during the story, these are two different commits. U-066 took its start at `b9fd172f`, then `9e983665` landed, and U-066 has no commit yet. A QA analyst cannot tell which commit is meant, or how to count tests that other commits add or delete.
- **Required fix:** amend through the record (a CR-016 Amendment 1, as was done for CR-014) before S1 is authored. Use this text:
  - REQ-015 AC-13: replace the whole criterion (`:165-174`) with: "- [ ] AC-13: `testDebugUnitTest` reports zero failures and no more than 2 skipped tests. At its start the story records the unit-test count given by the authoritative count command of the project instructions (`.claude/CLAUDE.md`, "Authoritative test count"), with the id of the commit it was taken at (`HEAD` when the story starts, before its first change). At the end of the story the same command, run with `HEAD` at the story's last commit, gives a count equal to that start count, plus the tests added, minus only the tests deleted under AC-7; each added and each deleted test is listed by name. If the end count is taken before the story's changes are committed, it is taken over the working tree with the same pattern (`git grep --untracked -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`), and the story says so. Tests added or deleted by commits that are not the story's, between the start commit and the end count, are listed with their commit id and are not counted as the story's. The total that the Gradle test run reports (skipped tests included) equals the end count, or the story records and explains the difference. No fixed baseline is stated, because the count changes with every story that adds or removes tests."
  - REQ-017 AC-15: after "not below the count recorded, with its commit id, at the start of the story" insert " (start and end counts taken at the points that REQ-MODERNIZATION-015 AC-13 states)".
  - REQ-019 AC-B9: after "at that story's start" insert " (both counts taken at the points that REQ-MODERNIZATION-015 AC-13 states)".
  - REQ-016 AC-6: replace "from the story's start commit" with "from the commit recorded at the story's start (as REQ-MODERNIZATION-015 AC-13 defines it)".

### F-2 (should-fix): the CR-016 note in REQ-017 says no criterion changes, but AC-15 changed

- **Where:** `REQ-MODERNIZATION-017-...:222-223`: "No acceptance criterion or constraint of this requirement changes; "Behavior preserved" stands as written. AC-15 states the count rule."
- **What is wrong:** CR-016 (A-2) rewrote AC-15. A reader can take the first sentence as covering the whole amendment.
- **Required fix:** replace that text with: "The sequencing changes no acceptance criterion or constraint of this requirement; "Behavior preserved" stands as written. AC-15 is reworded by CR-016 (A-2) to state the count rule."

### F-3 (should-fix): REQ-020 says no criterion of REQ-017 is changed

- **Where:** `REQ-MODERNIZATION-020-...:77`: "No acceptance criterion or constraint of REQ-MODERNIZATION-017 is changed; it carries a sequencing note that points here (CR-016)."
- **What is wrong:** CR-016 changed REQ-017 AC-15. The sentence is true only when read as "by this requirement".
- **Required fix:** replace it with: "This requirement changes no acceptance criterion or constraint of REQ-MODERNIZATION-017; REQ-MODERNIZATION-017 carries a sequencing note that points here (CR-016)."

### F-4 (should-fix): close the record so that the design's conditions and REQ-015 AC-9 can be checked

- **Where:** CR-016, frontmatter `status: approved` and Metadata "Status | Open" (`:39`); `## Execution` is empty (`:881-902`).
- **What is wrong:** X-1 (`DES-...-014:48-49`) treats the P-2 and P-3 conditions as met only "once a record is applied (status `implemented`)". Check 2 of the record's D-5 also needs `implemented`. Check 3 needs a commit that applies the record, so that `git diff` of REQ-001 and REQ-006 shows only the list and the note. Today the edits are uncommitted and sit beside CR-015's edit to REQ-005 and the `_index.md` change.
- **Required fix:**
  1. Run the Fix Checklist greps and `git diff --stat -- sdlc/artifacts/requirements`. That should list the eight files plus the CR-015 and `_index.md` changes, which are excluded from this check.
  2. Commit CR-016's applied edits in a commit of their own, or at least one that touches REQ-001 and REQ-006 for nothing else.
  3. Fill `## Execution`: Changes Made, the commit id, and Departures, which are the REQ-015 `:246-248` and REQ-016 `:160-161` note wording and any F-1 to F-3 amendment.
  4. Set the status to `implemented`, set the Metadata Status to match, and add `EPIC-MODERNIZATION-006` to the record's `requirements` list, since the epic is edited.

### Notes (no action needed for S1 or S2)

- **N-1.** The A-4 text in REQ-016 (`:43-44`) says "the authoritative count command ... gave 732 unit tests, 2 of them `@Ignore`d". The command gives only the 732. The 2 comes from counting `@Ignore` annotations, not raw hits (section 2). It is true as a fact and no criterion depends on it.
- **N-2.** REQ-017 AC-15 lets a story subtract only tests that "another epic requirement" requires it to delete. REQ-017 AC-13 (`:151-152`) can force a test off an unsupported `@Config(sdk=...)`. Retargeting is allowed, so the two do not contradict, but the S3 or S5 story should retarget rather than delete. This wording predates CR-016.
- **N-3.** REQ-016 AC-6 forbids lowering a `minimum` but says nothing about narrowing `includes` or adding `excludes`. Design D8 item 1 forbids both, and the story should cite D8.
- **N-4.** Epic criterion 7 (`:123-126`) applies the start-count record to every story. REQ-016 AC-5 has no count rule, so the S4 author takes the rule from epic criterion 7. REQ-020 Constraints (`:410-413`) are looser: no 2-skip cap and "any difference is explained". They are compatible with epic criterion 7, and the BUG-021 fix story should meet both.

## 4. Rulings on the applier's leftovers

| # | Leftover | Ruling | Reason |
|---|----------|--------|--------|
| a | Epic Description `:20` "697 unit tests green" beside criterion 7 | Leave | A true statement about sprint-017. Criterion 7 says "The epic states no fixed run count", and the record's Fix Checklist expects this one hit |
| b | REQ-018 682 and 697 | Leave | Implemented and closed. Its own text says to re-baseline, and no DES-014 story cites it |
| c | REQ-017 "Behavior preserved" (`:184-185`) and epic criterion 9 (`:134-136`) beside REQ-020 AC-14 and AC-16 | Leave | REQ-017's comparison is across its own level raise, which does not touch Skip or restore. Criterion 9 compares across the mail migration. The epic Description (`:30-32`) now names the two deliberate changes. Caution for the Part B design: take the criterion 9 "before" capture after the BUG-021 fix lands, or allow-list the AC-16 restore outcome |
| d | Epic `:140` "Planned requirements:" with item 6 an approved requirement | Leave (fix at the next epic amendment) | Items 1 to 5 also exist and are approved (`_index.md`), so the word is equally stale for all six and misleads no one. Rewording it is outside CR-016's approved text |
| e | Sequencing "requirements 1 to 4" / "1 to 5" | Leave | `:160` ("in parallel with requirements 1 to 4") is still correct. `:167` ("not one of requirements 1 to 5") correctly sets item 6 apart. REQ-017 notes `:23` "requirement 3 of 5" and REQ-020 `:450` "the epic's five requirements" read the same way |
| f | REQ-020 `related_requirements` does not list the epic or REQ-015 | Leave | The epic is linked by `epic` and `traces_to` (`:12`, `:18`), and `related_requirements` lists REQ-* ids. REQ-020 does not depend on REQ-015. If anything were added, REQ-016 would be more relevant (it is the same `BackupWorker.kt` and has the ordering in Notes `:473-477`), but this is not needed |

## 5. What could mislead the S1 (minSdk 24) and S2 (toolchain) authors

1. F-1 is the one real trap. Both stories verify REQ-015 AC-13 (`DES-...-014:1884`). Unless the text is fixed, write the measurement points into the story's own acceptance criteria: the start at `HEAD` before the first change; the end at the story commit, or `--untracked` over the working tree before it; and other commits listed.
2. S1's AC-9 evidence depends on F-4: CR-016 must be `implemented`, and there must be an applying commit to diff. Until then, S1 cannot cite check 3 of the record's D-5.
3. REQ-001 and REQ-006 still say 35 and 21 in their criteria. This is on purpose, and their notes (`REQ-001:18`, `REQ-006:24`) say these are not the current requirement. The governing values are REQ-015 AC-5 (24) and REQ-017 AC-2 (37).
4. REQ-015 AC-10 and the epic `:184-185` still carry publishing wording (Part E, not applied). Design S1-I7 plans the `CHANGES` entry, and that meets AC-10.

---

## Delta check after Amendment 1 (2026-11-21)

Scope: CR-016 `## Amendment 1` (record `:537-587`) and its six edits. Method as before: Read and Grep only, no shell. The two 746 counts at `1f8ebe3b` are the amendment's own (record `:567-568`) and were not re-run here. Before the commit, the working tree gave 746 `@Test` occurrences (section 2), which agrees with them.

## Verdict after Amendment 1: PASS

The original verdict (FAIL) above is kept as history.

**(1) The amendment is consistent with the record and changes no ruling.** It is insert-only. It states that the count rule and the "no fixed figure" decision stand (`:541`). Its counts are restated without rewriting the earlier text (`:543`, 31 edits in 8 requirement files, plus X-1). It departs from this report's replacement text in two places (`:558-561`), and both are improvements:
- "with the story's last commit in place of `HEAD`" removes the "check it out" reading.
- "changes that are not the story's ... (or, if uncommitted, named as such)" covers another story's uncommitted tests, which the working-tree form also counts.

**(2) The six edits are applied as the amendment gives them:**

| Edit | Where | Result |
|------|-------|--------|
| A-7 | `REQ-MODERNIZATION-015-...:165-180` | Verbatim. The `\|` in the record's table is a Markdown escape; the file correctly has `|` (`:174`) |
| A-8 | `REQ-MODERNIZATION-017-...:161-162` | Verbatim |
| A-9 | `REQ-MODERNIZATION-019-...:113` | Verbatim |
| A-10 | `REQ-MODERNIZATION-016-...:98-99` | Verbatim |
| D-12 | `REQ-MODERNIZATION-017-...:223-225` | Verbatim. The paragraph's line wrap at `:225-226` ("Also:" then a new line) is cosmetic |
| D-13 | `REQ-MODERNIZATION-020-...:77` | Verbatim |

No old phrasing is left: "commit immediately before", "story's start commit" and "is changed; it carries" have zero hits under `sdlc/artifacts/requirements/modernization`.

**(3) AC-13 can now be applied without asking. Worked U-066 case:**
- The start count is 732 at `b9fd172f`, which was `HEAD` before U-066's first change.
- `9e983665` landed mid-story. It is not the story's, and the evidence shows it changed no test (the count stayed 732 at HEAD, `U-066/evidence/I9-checks.txt:2`), so nothing from it is listed.
- The end count uses the committed form with `1f8ebe3b` in place of `HEAD` and gives 746.
- 746 = 732 + 14 added − 0 deleted, with the 14 listed by name.
- The Gradle total is checked against 746.
- Before the commit, the `--untracked` form would have given the same 746.

Both forms, and the interleaved-commit case, now have a stated procedure.

**(4) F-1 to F-3 are closed, and nothing new is contradicted:**
- A-8, A-9 and A-10 point to A-7, so the start and end points are defined once.
- D-12 and D-13 now say who changes what, and no longer contradict A-2.
- No contradiction was found with REQ-015 AC-9 and AC-14, REQ-016 AC-5, REQ-017 AC-13, REQ-020 Constraints (`:410-413`), epic criteria 7 and 8, or the design note X-1, which still says the requirements state "the count at the start of each story".

**F-4 rulings:**
- **The `requirements` list without `EPIC-MODERNIZATION-006`: acceptable.** It follows the CR-010 and CR-011 convention. The epic is named in the record's body and Impact Analysis, and check 2 of the record's D-5 needs only REQ-001 and REQ-006 in that list. No action.
- **Departures:** recorded (`:581-587`). Closed.
- **Status, Execution and commit:** still open, and the orchestrator owns them. Until CR-016 is `implemented` with its commit id recorded, S1 cannot cite check 3 of D-5 (the AC-9 evidence) and X-1's conditions are not met.

**Open items:**
- **O-1 (orchestrator, before S1):** close CR-016. Set the status to `implemented`, fill Execution, and record the applying commit. If that commit is `1f8ebe3b`, it also carries U-066's code. That is acceptable for D-5 check 3 only if `git diff` of REQ-001 and REQ-006 in that commit shows just the list and the note.
- **O-2 (note, no action required):** in A-7, "are listed ... and are not counted as the story's" means they stay in the end count but are not attributed to the story. That is the only reading under which the equation can hold, because a `git grep` count cannot exclude them. If a future amendment touches AC-13, it could add "; the equation adds their net change to the start count".
