---
status: implemented
severity: high
artifact_type: change-request
requirements:
  - REQ-MODERNIZATION-001
  - REQ-MODERNIZATION-006
  - REQ-MODERNIZATION-015
  - REQ-MODERNIZATION-016
  - REQ-MODERNIZATION-017
  - REQ-MODERNIZATION-019
  - REQ-MODERNIZATION-020
design_docs:
  - DES-MODERNIZATION-014
related_stories:
  - U-066
  - BUG-021
tags:
  - change-record
  - modernization
  - requirements-wording
  - des-014-preconditions
id: CR-016
title: 'Requirements wording overtaken by the work, before the stories of DES-MODERNIZATION-014: test-count baseline (REQ-015 AC-13, REQ-017 AC-15, epic criterion 7), coverage floors (REQ-016 AC-6, epic criterion 8), literal levels in REQ-001 and REQ-006 (REQ-015 AC-9), and cross-references to REQ-020'
type: RCR
domain: change-records
summary: 'Wording in approved requirements that the work has overtaken and that the stories of design DES-MODERNIZATION-014 cannot meet as written: REQ-MODERNIZATION-015 AC-13 (a test-count baseline of 697; the count is 732 and rising); REQ-MODERNIZATION-016 AC-6 and EPIC-MODERNIZATION-006 success criterion 8 (other packages at or above 70 percent coverage, against 13 configured floors below 70); REQ-MODERNIZATION-001 (a literal API level 35 in AC-1 and AC-29) and the REQ-MODERNIZATION-015 AC-9 items on REQ-MODERNIZATION-001 and -006; a sequencing line in the epic and a note in REQ-MODERNIZATION-017 for the new REQ-MODERNIZATION-020. Design DES-MODERNIZATION-014 names these as preconditions P-2 and P-3; the orchestrator ruled them mandatory (decision appendix, Record 14 row 9).'
updated_at: '2026-11-21T09:33:11.654Z'
---
# CR-016: Requirements wording overtaken by the work, before the stories of DES-MODERNIZATION-014

## Metadata

| Field | Value |
|-------|-------|
| **CR Number** | CR-016 |
| **Type** | RCR (Requirement Change) |
| **Severity** | High |
| **Status** | Applied 2026-11-21 (31 requirement edits in 8 files, and X-1); frontmatter `status` is the record of state |
| **Reported By** | Requirements Analyst, on the orchestrator's instruction (decision appendix, Record 14 row 9) |
| **Date Reported** | 2026-11-21 |
| **Source** | Sprint work: design DES-MODERNIZATION-014 (approved 2026-11-21), its findings FD-VAL-002 and FD-VAL-003 and its preconditions P-2 and P-3 |

Nothing is wrong in production. This record describes and proposes text; no requirement, epic, design,
build file, code or test is edited by it. It is to be APPLIED right after approval, in one pass, by the
Requirements Analyst and checked by the Requirements Reviewer, as CR-010 and CR-011 were on 2026-11-21.

**Counts, stated once.** 25 numbered edits (A-1 to A-6, B-1 to B-6, C-1 and C-2, D-1 to D-11) in 8 requirement
files (REQ-001, -006, -015, -016, -017, -019, -020 and EPIC-MODERNIZATION-006), plus one insert-only design note
(X-1) in DES-MODERNIZATION-014: 26 numbered edits in 9 files. Part E (E-1 to E-3, not counted above) is not applied.
The Solution Architect's rulings (section "Suggested Approach", adopted by the orchestrator) are built into the
tables below; where this record and that section differ in wording, this record's Proposed Change is the final text.
Line numbers below were read on 2026-11-21; when applying, find each place by its quoted text, not by line
number. Line breaks inside a quotation are shown as single spaces.

The record has five parts and one design note. Parts A to D and X-1 are mandatory (the orchestrator's ruling).
Part E is kept for the record and is **not applied** (decision Record 13 row 6).

---

## Finding

### Description

Design DES-MODERNIZATION-014 (approved 2026-11-21) found requirement wording that the work has overtaken.
The Design Auditor required that the design does not reinterpret approved criteria, so the design sends the
wording to a change record (P-2 before its first story S1, P-3 before its runtime-library story S4; one
record is allowed if raised before S1). This is that record.

1. **Test count.** REQ-MODERNIZATION-015 AC-13 states the test-run equation from a fixed baseline of 697
   tests. The authoritative count is 732 at HEAD and rises with story U-066. The same fixed number is in
   REQ-MODERNIZATION-017 AC-15, EPIC-MODERNIZATION-006 success criterion 7, and (as a current-state fact)
   REQ-MODERNIZATION-016 Context, and REQ-MODERNIZATION-019 (AC-B9 and the Context baseline line). A story that
   starts from 732 cannot meet "697 plus added minus deleted".
2. **Coverage floors.** REQ-MODERNIZATION-016 AC-6 and epic success criterion 8 say "others at or above 70%".
   Of the 18 configured JaCoCo rules, 13 have a floor below 0.70 (listed in Evidence 2). The criterion cannot
   be met, and the gate that actually runs is the configured one.
3. **Literal levels.** REQ-MODERNIZATION-001 states a literal level 35 (AC-1 and AC-29) and a literal minSdk
   21 (AC-2); REQ-MODERNIZATION-006 states minSdk 21 (Description, Context, AC-3, AC-5, Constraints).
   REQ-MODERNIZATION-015 AC-9 requires change records on both for their minSdk 21 text, and
   REQ-MODERNIZATION-017 Notes say REQ-MODERNIZATION-001 "should carry a change record" for its 35. No such
   record exists (`change_records: []` in both files).
4. **REQ-MODERNIZATION-020** (approved 2026-11-21) is a correctness requirement under the epic. The epic's
   Related Requirements and Sequencing do not mention it, and REQ-MODERNIZATION-017 does not mention it, but
   the targetSdk 37 step depends on it (design D14, decision Record 13 row 4). REQ-MODERNIZATION-017 also still
   carries a stale frontmatter note saying that REQ-MODERNIZATION-015 and -016 do not exist on disk yet; both
   exist and are approved.
5. **Store-publishing wording** remains in three places although the app is private and unpublished
   (REQ-MODERNIZATION-015 AC-10; the epic; REQ-MODERNIZATION-017 Constraints). Part E; not applied.
6. **The epic's "behaviour preserved" clause** (Description) does not name the two outcomes that
   REQ-MODERNIZATION-020 changes on purpose (AC-14 Skip, AC-16 restore). D-11.

### Why one record, and not two or five

One record is right:

- The design allows P-2 and P-3 to be one record if raised before S1, and the orchestrator asked for one.
- The files overlap. REQ-MODERNIZATION-016 is edited by Part A (Context) and Part C (AC-6); the epic by Parts
  A (criterion 7), C (criterion 8) and D (Description, Sequencing, Related Requirements); REQ-MODERNIZATION-017
  by Parts A, B and D; REQ-MODERNIZATION-015 by Parts A and B; REQ-MODERNIZATION-019 by Part A. Two records would put two dated notes and two review
  passes on the same files, and would split epic criteria 7 and 8, which are adjacent lines of one list.
- Every edit is wording. None changes what a story must build, except Parts A and C, which replace a figure
  that cannot be met by a rule that can.

Part E is kept in this record only as rows marked "not applied (Record 13 row 6)", so the orchestrator's
ruling is on the page. Nothing else depends on it.

### Source of Truth

- **Design DES-MODERNIZATION-014** (`sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-toolchain-runtime-libraries-platform-level-design.md`):
  findings FD-VAL-002 (lines 1954-1960) and FD-VAL-003 (lines 1962-1964); decision D8 (lines 1005-1054,
  item 5 at 1047-1051); preconditions table (lines 489-493); traceability rows 015 AC-9 (1868), 015 AC-13
  (1872), 016 AC-6 (1883); decision D14 outcomes table (lines 1431-1438).
- **Product owner and orchestrator decisions**, decision appendix of
  `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`: Record 12
  (delegation, line 2278); Record 13 row 4 (fix first, then targetSdk 37; line 2296) and row 6 (private-app
  wording: no change record until a story is blocked; line 2298); Record 14 row 9 (the three change records are
  mandatory; line 2325) and row 10 (REQ-MODERNIZATION-020; line 2326).
- **Code and build configuration:** the JaCoCo rules in `app/build.gradle:340-589`; the levels at
  `app/build.gradle:21,24,25`; the authoritative test count command in `.claude/CLAUDE.md`.
- **Precedent for amending approved requirements by change record:** CR-003 (dated notes), CR-010, CR-011
  (`change_records` in frontmatter, `status` stays `approved`, dated "Amended ... by CR-..." notes), CR-004
  (a requirement whose intent was wholly replaced is `superseded`).

### Evidence

**1. The test count (verified)**

Counted at HEAD (not the working tree, where story U-066 has uncommitted test changes), with the command in
`.claude/CLAUDE.md`:

`git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"` gives **732**
at HEAD `9e983665`, on 2026-11-21. The two `@Ignore`d tests at HEAD are `AppTest.java:13` and
`AuthPreferencesTest.java:1079` (the other `@Ignore` hits in that file are comments).

Where 697 came from: `sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/after-junit-totals.txt:1`
reads `tests=697 failures=0 errors=0 skipped=2` (the JUnit totals include the skipped tests), and the
`git grep` count was also 697 at sprint-015 (`sdlc/artifacts/build/sprints/sprint-015/U-057/implementation-log.md:108`).
It was the sprint-017 end state. Since then the donation tests were deleted (REQ-MODERNIZATION-018 AC-7 expected
682 run: 697 minus 15) and U-063 to U-065 added TLS tests; the net is 732. Story U-066 states its own AC-12
relative to the count at its start (`sdlc/artifacts/build/sprints/sprint-022/U-066-prune-weak-tls-cipher-suites.md:339`);
its plan expects 732 to 746 (`sdlc/artifacts/build/sprints/sprint-022/U-066/plan.md:2164`; a plan figure, not a
measurement).

Every place in the requirements that states the number (search of `sdlc/artifacts/requirements` for `697`):

| File:line | Text | Kind | Treatment |
|-----------|------|------|-----------|
| `REQ-MODERNIZATION-015-...-minsdk-23.md:164` (AC-13) | "The run count equals the sprint-017 baseline of 697 plus any tests added, minus only the tests deleted under AC-7, each listed by name." | Criterion | Part A, A-1 |
| `REQ-MODERNIZATION-017-...md:155` (AC-15) | "the unit-test count not below the sprint-017 baseline (697 run, 2 skipped, adjusted only for tests deleted by other epic requirements and listed in their stories)" | Criterion | Part A, A-2 |
| `EPIC-MODERNIZATION-006-...md:120` (criterion 7) | "no more skipped tests than the sprint-017 baseline (697 run, 2 skipped), allowing for tests deleted with the donation flow, which are listed in the story that deletes them" | Success criterion | Part A, A-3 |
| `REQ-MODERNIZATION-016-...md:42` (Context) | "with 697 unit tests (2 skipped)" | Current-state fact, false today | Part A, A-4 |
| `EPIC-MODERNIZATION-006-...md:19` (Description) | "Sprint-017 moved the build onto JDK 25 ... with 697 unit tests green" | History of sprint-017, true | Leave |
| `REQ-MODERNIZATION-018-...md:128,169` | "The new test baseline is 682 run (697 minus 15)"; "the 682 target assumes no other test changes land first. Re-baseline from the Gradle report if they do." | Implemented in sprint-018; its own text already says to re-baseline | Leave |
| `REQ-MODERNIZATION-019-...md:60` (Context) | "Baseline: 697 unit tests, 2 skipped, including the GreenMail IMAP integration tests ..." | Current-state fact, false today | Part A, A-6 |
| `REQ-MODERNIZATION-019-...md:112` (AC-B9) | "the executed count is at least 697 minus tests deleted with the vendored module, each deletion listed with its replacement" | Criterion (Part B, the mail migration); weaker than the rule | Part A, A-5 |

REQ-MODERNIZATION-015 AC-14, REQ-MODERNIZATION-016 AC-5 and AC-6 and the Constraints of these files state no
test count. "no more than 2 skipped" (REQ-015 AC-13, REQ-016 AC-5) stays true (2 at HEAD) and is kept.

**2. The 18 JaCoCo rules (verified, `app/build.gradle`, unchanged in the working tree)**

All rules are `element = 'PACKAGE'`, `counter = 'LINE'`, `value = 'COVEREDRATIO'`; only `minimum` differs.

| # | `includes` (line) | `minimum` (line) | Below 0.70? | Comment in the file |
|---|-------------------|------------------|-------------|---------------------|
| 1 | `com.zegoggles.smssync.service*` (359) | 0.70 (363) | no | U-006 gate, "DO NOT LOWER" |
| 2 | `com.zegoggles.smssync.mail*` (370) | 0.70 (374) | no | U-006 gate; covers `mail` and `mail.transport` |
| 3 | `com.zegoggles.smssync.auth*` (381) | 0.70 (385) | no | U-006 gate |
| 4 | `com.zegoggles.smssync` (402) | 0.50 (406) | **yes** | "MEASURED 52.9% - TODO: raise to 70%" |
| 5 | `com.zegoggles.smssync.activity` (417) | 0.10 (421) | **yes** | "MEASURED 15.4% (151/983) - TODO: raise to 70%" |
| 6 | `com.zegoggles.smssync.activity.auth*` (428) | 0.00 (432) | **yes** | "MEASURED 0% (0/139) - TODO: raise to 50%+" |
| 7 | `com.zegoggles.smssync.activity.events*` (438) | 0.00 (442) | **yes** | "MEASURED 0% (0/8) - TODO: raise to 70%" |
| 8 | `com.zegoggles.smssync.activity.fragments*` (448) | 0.10 (452) | **yes** | "MEASURED 12.0% (58/483) - TODO: raise to 50%+" |
| 9 | `com.zegoggles.smssync.calendar*` (460) | 0.70 (464) | no | "MEASURED 74.0%" |
| 10 | `com.zegoggles.smssync.compat*` (474) | 0.20 (478) | **yes** | "MEASURED 23.3%; U-060: 21.9% (16/73) - TODO: raise to 70%" |
| 11 | `com.zegoggles.smssync.contacts*` (486) | 0.70 (490) | no | "MEASURED 77.4%" |
| 12 | `com.zegoggles.smssync.di*` (501) | 0.03 (505) | **yes** | "MEASURED 3.1% (1/32)" |
| 13 | `com.zegoggles.smssync.preferences*` (513) | 0.65 (517) | **yes** | "MEASURED 69.4% - TODO: raise to 70%" |
| 14 | `com.zegoggles.smssync.receiver*` (530) | 0.50 (534) | **yes** | "MEASURED 53.4%; U-060: 50.0% (29/58), zero headroom" |
| 15 | `com.zegoggles.smssync.scheduler*` (542) | 0.60 (546) | **yes** | "MEASURED 62.2% - TODO: raise to 70%" |
| 16 | `com.zegoggles.smssync.tasks*` (555) | 0.00 (559) | **yes** | "MEASURED 0% (0/20)" |
| 17 | `com.zegoggles.smssync.utils*` (567) | 0.10 (571) | **yes** | "MEASURED 15.0% - TODO: raise to 70%" |
| 18 | `com.zegoggles.smssync.worker*` (582) | 0.00 (586) | **yes** | "MEASURED 0% - BackupTriggerWorker untested" |

5 rules at 0.70 (1, 2, 3, 9, 11); **13 below 0.70** (4 to 8, 10, 12 to 18). This matches DES-MODERNIZATION-014 D8
(lines 1009-1026).

How the floors were set: story U-051 (TE-001, sprint-013) extended the gate "from 3 packages to all ~22, with
honest measured floors (existing service*/mail*/auth* unchanged at >=70%)"
(`sdlc/artifacts/build/sprints/sprint-013/retro.md:13`); the block comment at `app/build.gradle:389-394` says
"Each floor is set at or just below the measured LINE% so the gate is green today but immediately visible. TODO
comments mark packages that need uplift". REQ-MODERNIZATION-003 set the 70% rule for the three packages
`service`, `mail`, `auth` only. Later stories followed "never lower a floor" (U-060 plan `:692`; U-061 plan `:491`;
DES-MODERNIZATION-013 D3 "lower nothing"). The sentence "others at or above 70%" matches the TODO targets and
the three-package 70% rule; no story ever made 70% the rule for the other packages (assumed, see Not verified).

**3. The levels (verified at HEAD)**

`app/build.gradle:21` `compileSdkVersion 35`; `:24` `minSdkVersion 21`; `:25` `targetSdkVersion 35`.
`k9mail-vendored/build.gradle:52` `compileSdkVersion 35`; `:54` `minSdkVersion 21`. What the requirements say:
REQ-MODERNIZATION-015 sets minSdk 24 (AC-5, as amended by CR-011); REQ-MODERNIZATION-017 sets compileSdk and
targetSdk 37 (AC-2, as amended by CR-011). The build has not changed yet; the requirement files are ahead of the
build, as intended.

**4. Literal levels in the two closed requirements (verified; every hit of a level, not of a line number)**

REQ-MODERNIZATION-001 (`REQ-MODERNIZATION-001-restore-play-store-eligibility.md`, status `approved`,
`change_records: []` at line 12; criteria are numbered 1 to 30, referred to elsewhere as AC-1, AC-2, AC-29):

| Line | Text (shortened only where marked) | What it is |
|------|-------------------------------------|------------|
| 19 | "raised from `compileSdkVersion`/`targetSdkVersion` 29 to SDK 35 ... passes Google Play pre-launch review at targetSdk 35" | Overview |
| 27, 37 | "Raising `targetSdkVersion` from 29 to 35 ..."; "The minSdk raise from 14 to 21 co-lands" | Context |
| 43 | "Raise `compileSdkVersion` and `targetSdkVersion` to 35, `minSdkVersion` to 21" | Requirement statement |
| 47 | "1. `app/build.gradle` declares `compileSdkVersion 35` and `targetSdkVersion 35`. ... returns only lines containing `35`." | AC-1 |
| 48 | "2. `app/build.gradle` declares `minSdkVersion 21`. ... no reference to `minSdkVersion 14` or any value below 21 remains." | AC-2 |
| 82 | "13. The git commit that sets `targetSdkVersion 35` is the same commit as, or is preceded within the same PR by, the commit adding `FLAG_IMMUTABLE`" | Criterion 13 (co-landing rule of the 35 raise) |
| 127 | heading "Signed AAB at targetSdk 35 Passes Play Pre-Launch Review (Gate G0)" | Heading |
| 134 | "29. `aapt2 dump badging <path-to-release.aab> | grep targetSdkVersion` outputs `targetSdkVersion:'35'`." | AC-29 |
| 155, 157 | "cannot compile or run tests against `compileSdkVersion 35`"; "Raising `minSdkVersion` to 21 renders `CalendarAccessorPre40.java` ... unreachable" | Constraints 1 and 3 |
| 179, 186 | "`:10` compileSdkVersion 29; `:14` minSdkVersion 14 ..."; "stable at minSdk 21 ... requires SDK 35 build baseline" | Notes (history) |

REQ-MODERNIZATION-006 (`REQ-MODERNIZATION-006-dead-code-and-minsdk-cleanup.md`, status `approved`,
`change_records: []` at line 16, frontmatter `notes` at line 17):

| Line | Text | What it is |
|------|------|------------|
| 26 | "complete the minSdk-21 cleanup opened by the SDK raise" | Description |
| 32 | "at the target `minSdk 21` (from REQ-MODERNIZATION-001)" | Context |
| 46 | "3. `minSdkVersion` is set to 21 and any now-dead `Build.VERSION` guards below API 21 are removed." | AC-3 |
| 49 | "5. No behavioral change to supported devices (API 21+)." | AC-5 |
| 59 | "Depends on REQ-MODERNIZATION-001 establishing `minSdk 21`" | Constraints |

REQ-MODERNIZATION-015 frontmatter `notes` (lines 22-25) name the same items ("REQ-MODERNIZATION-001 (AC-2) and
REQ-MODERNIZATION-006 (AC-3, and the minSdk 21 target in its Context, which the epic also lists as AC-5)") and end
"Those artifacts need change records (AC-9 below); the orchestrator must create them, because this draft cannot
edit files." The epic names them at lines 48-50 and criterion 2 (lines 87-89).

How these requirements were closed: REQ-MODERNIZATION-001 was implemented by sprint-001 (U-003 and later) and
REQ-MODERNIZATION-006 by the dead-code sweep; their acceptance criteria record what was true and verified then.
The plugin's artifact state machine for requirements is `draft, in-review, approved, superseded`; `superseded`
is terminal and there is no "implemented" state ("requirements do not track implementation status",
`registries/artifact-librarian/state-machines.yaml:25-26` and `:45-55`).

**5. What REQ-MODERNIZATION-017 Notes and REQ-MODERNIZATION-015 AC-9 say (verified)**

- REQ-017 Notes, lines 194-195: "Supersedes the literal "35" in REQ-MODERNIZATION-001 AC-1 and AC-29 only for
  future state; REQ-MODERNIZATION-001 should carry a change record."
- REQ-015 AC-9, lines 147-149: "Change records are created on REQ-MODERNIZATION-001 and REQ-MODERNIZATION-006
  recording that their literal minSdk 21 statements are superseded by this requirement. Neither artifact is
  edited silently." It names minSdk 21 only (not 35), and it names the two records' targets, not the number of
  records. Its verification is listed under "Documentation review" (REQ-015 Verification Method, line 219).

**6. REQ-MODERNIZATION-020 and the stale REQ-MODERNIZATION-017 note (verified)**

- REQ-020 Notes (lines 459-461): "The epic's text has no line for a requirement of this kind. A line has been
  proposed to the orchestrator; the epic is approved and is not edited here." REQ-020 Context line 76: "REQ-MODERNIZATION-017
  itself is not changed." Its Dependencies (lines 465-466): "Before the targetSdk 37 change of REQ-MODERNIZATION-017
  (Record 13 row 4). compileSdk 37 does not wait for it."
- The wording that REQ-020's analyst proposed to the orchestrator is not on disk (the only trace is the Notes
  sentence above). The text in Part D is drafted from REQ-020's own Notes and Dependencies and checked against
  design D14. See Not verified.
- Design D14 outcomes (lines 1431-1438): only "Measured not affected" lets the targetSdk 37 step (S5-I4) go
  without the fix; "Gap confirmed", "Gap not ruled out", both "Inconclusive" outcomes, and "Not run" make it wait.
- REQ-017 frontmatter `notes` (line 25): "Neither REQ-MODERNIZATION-015 nor -016 exists on disk yet; those
  references are unverified." Both exist and are approved (`_index.md:8-9`).

---

## Triage

### Classification

- **Type:** RCR. The approved text changes: fixed figures are replaced by rules that stay true, closed
  requirements gain notes, and cross-references are added. It is not a DCR (the design is correct and approved;
  it sent the wording here) and not a DEF (no code is wrong).
- **Root:** Requirements layer. Two causes: facts moved after approval (the test count; the levels; REQ-020
  appeared), and one wording that was never true (FD-VAL-002: "others at or above 70%" against the configured
  floors).
- **Layers Affected:** 2 layers are edited by this record: requirements (8 files) and design (one insert-only
  note, X-1, in DES-MODERNIZATION-014).
- **Severity: High.** Reasons:
  - It gates the next story. DES-MODERNIZATION-014 makes P-2 a precondition of S1 (the first story) and P-3 a
    precondition of S4; S1 cannot be authored with acceptance criteria that its own start count (732 or more)
    cannot meet.
  - It touches eight requirement files and one design, and shapes the acceptance criteria of at least five stories (S1, S2,
    S3, S4, S5) and of the fix for BUG-021.
  - It is not Critical: no active work is blocked (U-066 is unaffected: its AC-12 is already relative to its own
    start count) and nothing is wrong in production. Rubric note: read literally, "affects 5+ files" gives
    Critical; the same note was made in CR-011, and the files here are text edits to wording. The orchestrator may
    raise it; the next steps are the same.
  - It is not Medium: it is not isolated, and it blocks a planned (not yet active) story.

---

## Impact Analysis

Searches run on 2026-11-21 (read-only). `change_records.layers` is not configured in `sdlc/config.yaml`; the
layers named in the dispatch are used. The impact analysis checklist was followed (per layer: searched, files,
occurrences, accurate or not).

### Layer Impact Matrix

| # | Layer | Affected? | Files | Occurrences | Action Needed |
|---|-------|-----------|-------|-------------|---------------|
| 1 | Ground truth: the approved design DES-MODERNIZATION-014, the code and build configuration, the product owner's decisions | Ground truth | `app/build.gradle:340-589` (18 rules); `.claude/CLAUDE.md` (count command); decision appendix Records 12 to 14 | n/a | None (read-only) |
| 2 | Requirements (`sdlc/artifacts/requirements`) | **Yes** | `REQ-MODERNIZATION-001` (note, frontmatter), `-006` (note, frontmatter), `-015` (AC-13; notes; frontmatter), `-016` (Context, AC-6; notes; frontmatter), `-017` (AC-15; frontmatter notes and list; Notes; frontmatter), `-019` (AC-B9, Context baseline line; Notes; frontmatter), `-020` (Context line 76, Notes; frontmatter), `EPIC-MODERNIZATION-006` (Description; criteria 7 and 8; Related Requirements; Sequencing; Amendments; frontmatter). Part E (not applied): `-015` AC-10, epic Constraints and Risks, `-017` Constraints | 8 files, 25 numbered edits (A 6, B 6, C 2, D 11), plus the `change_records` entries and dated notes | Amend in place (Proposed Change) |
| 3 | Design and contracts (`sdlc/artifacts/design`) | Cites the requirements; no decision changes | `DES-MODERNIZATION-014`: one insert-only dated note under the H1 (X-1) and `change_records` gains `CR-015` and `CR-016`. The 46 lines that say P-1 to P-3 stay as written; the note maps them | 1 file, 1 numbered edit (X-1) | Insert the note (Proposed Change, X-1) |
| 4 | Stories, sprints, change records (`sdlc/artifacts/build`, `sdlc/artifacts/stories`) | Stories S1 to S5 are not yet authored; BUG-021 and BUG-022 exist | No story file for S1 to S5 exists. U-066 (sprint-022, in progress) is unaffected | 0 active | Stories are authored from the amended text |
| 5 | Application code (`app/src/main`) | No | none | 0 | None |
| 6 | Vendored library (`k9mail-vendored`) | No | none | 0 | None |
| 7 | Tests (`app/src/test`) | No | none | 0 | None |
| 8 | Documentation (`README.md`, `sdlc/artifacts/engagement`) | No | `sdlc/artifacts/engagement/code-location.md:27-28` states the current levels (a current-state fact: changes when the build changes) | 0 now | None |
| 9 | Build configuration | No | `app/build.gradle` rules and levels are the evidence, not changed; the floors are not touched | 0 | None |

Layers touched by edits: 2 (requirements, design note). The severity multiplier does not apply as a rework risk: no
story has been built from the old wording.

### What must NOT be touched (for every part)

- No acceptance criterion that this record does not name. In particular REQ-015 AC-1 to AC-8, AC-9, AC-11,
  AC-12, AC-14 to AC-18 (AC-9 is deliberately unchanged: Part B says how it is verified); REQ-016 AC-1 to AC-5
  and AC-7 to AC-13; REQ-017 AC-1 to AC-14 and AC-16; every criterion of REQ-001, REQ-006 and REQ-020; every
  criterion of REQ-019 except AC-B9; epic success criteria other than 7 and 8 (criterion 9 is unchanged: it
  compares IMAP round-trip content across the mail migration, which REQ-020 does not touch).
- `status` on every file (all stay `approved`); `id`, `artifact_type`, `type`, `domain`, `epic`, `traces_to`;
  file names (REQ-015's name keeps `minsdk-23`, as CR-011 decided); `updated_at`.
- The version tables, "Context" facts and "Rationale" text of REQ-015 and REQ-016, apart from REQ-016 Context
  line 42 (A-4).
- `sdlc/artifacts/requirements/modernization/_index.md`: no title changes, so no row changes (REQ-020's row
  already exists).
- In DES-MODERNIZATION-014: every line except the one inserted note under the H1 and the two `change_records`
  entries (X-1). Other designs, contracts, stories, code, tests and the decision appendix.
- Edits that CR-005, CR-006 and CR-014 (Edit 6) queue for REQ-019 are on other lines (AC-B8, contract references);
  they are not applied here and do not conflict.

---

## Proposed Change

Nothing below is applied by this record. "Current text" is quoted from the files as read on 2026-11-21. The
numbered "Decisions" (D-1 to D-7) give reasons; the edit tables (A-1 to X-1) are the final text for the applier.
Do not confuse decision D-n with edit D-n of Part D: edits are always named in the tables.

### Decisions

**D-1. A rule, not a new number (Part A).** Replacing 697 by 732 would be wrong again at the next story (U-066
alone is expected to add about 14). The rule that stays true is the one story U-066 already uses: the count at
the start of the story, plus tests added, minus deleted tests, each named. (Architect's ruling 1, adopted.) The
start count is recorded with the id of the commit it was taken at, the commit just before the story's first
commit, so that anyone can re-run the command there and `git log` shows whether it is the right commit. The same
tool, the authoritative `git grep` command of `.claude/CLAUDE.md`, is used at both ends of the equation, and the
Gradle run total is checked against the end count, with any difference explained. Using one tool at both ends
means a comment line containing `@Test` that is already there cancels out, and one the story adds breaks the
check, so it shows (this replaces the separate wording about comments). The Gradle run count includes skipped
tests (sprint-017 totals: `tests=697 ... skipped=2` against a `git grep` count of 697). The "no more than 2
skipped" limit is kept as it stands. The same rule fits REQ-017 AC-15 (which keeps its "not below" form and its
exception), REQ-019 AC-B9, and epic criterion 7, which cites both REQ-015 AC-13 and REQ-017 AC-15, because AC-13
allows only AC-7 deletions.

**D-2. REQ-017 AC-15 and the epic keep their meaning (Part A).** REQ-017 AC-15's old clause "adjusted only for
tests deleted by other epic requirements and listed in their stories" existed because earlier requirements
deleted tests before this one ran. A start-of-story count already contains those deletions, so the clause is
replaced, not widened: a test may still be subtracted only if another epic requirement's criterion requires its
deletion, and it is then listed by name with that criterion. REQ-015 AC-13 keeps its own narrower clause ("only
the tests deleted under AC-7").

**D-3. Coverage: say what the gate is, add no obligation (Part C).** The criterion becomes "every package meets
the floor configured for it, and no `minimum` is lowered by this work". It does not require the 13 packages below
0.70 to reach 70%: no decision supports raising floors (the epic lists only "lowering" as out of scope; U-051's
floors are measured and carry TODO comments; design D8 item 1 forbids any change to a `minimum`, `includes` or
`excludes` and item 5 says "no floor lowered or raised by this work"). The words "no floor is raised" are
deliberately not added to the requirement: a story that adds tests and, with the owner's agreement, raises a floor
would not break an approved criterion. Whether to commission a coverage uplift is a product question, raised as
Scope Concern SC-2, not decided here.

**D-4. REQ-001 and REQ-006: leave the text, add a dated note (Part B).** These requirements were implemented
and closed long ago. Their acceptance criteria record what was required at the time. Replacing
"35" or "21" in them by a pointer to REQ-015 or REQ-017 would make a closed requirement read as if it had
required something else (for example, REQ-001 AC-1 "returns only lines containing `35`" would read as "37", which
the verification at the time did not and could not show). So, per literal line, one treatment, and it is the same
for all of them:

| Literal (file, line) | Treatment | Reason |
|----------------------|-----------|--------|
| REQ-001 AC-1 (47): `compileSdkVersion 35`, `targetSdkVersion 35` | Leave; covered by the dated note | Records what was required for the 29-to-35 raise; the live value is REQ-017 AC-2 (37) |
| REQ-001 AC-2 (48): `minSdkVersion 21` | Leave; covered by the note | Same; the live value is REQ-015 AC-5 (24). This is the one REQ-015 AC-9 names ("their literal minSdk 21 statements") |
| REQ-001 AC-29 (134): `targetSdkVersion:'35'` from `aapt2 dump badging` | Leave; covered by the note | A verification command and its expected output for the release bundle at that time; rewriting the output to '37' would claim a result nobody has seen |
| REQ-001 Overview 19, Context 27 and 37, Requirement 43, criterion 13 (82), heading 127, Constraints 155 and 157, Notes 179 and 186 | Leave; covered by the note | History and descriptions of the 29-to-35 and 14-to-21 raises |
| REQ-006 Description 26, Context 32, AC-3 (46), AC-5 (49), Constraints 59 | Leave; covered by the note | REQ-006's ACs 1, 2 and 4 (dead code, `StatusPreference`) remain valid; only the minSdk 21 level is superseded, by REQ-015 |

Not recommended: marking REQ-001 or REQ-006 `superseded`. The state machine offers only `superseded`, and it is
terminal for the whole requirement. REQ-001's other criteria (FLAG_IMMUTABLE, `android:exported`, foreground
service type, JCenter removal) and REQ-006's dead-code and `StatusPreference` criteria are not superseded by
anything. CR-004 used `superseded` for REQ-010 because REQ-018 replaced its whole intent; that is not the case
here. The dated note plus `change_records` is the CR-003/CR-010/CR-011 precedent. Raised as SC-3 for completeness.

**Where the note goes.** Directly under the H1 of REQ-001 and REQ-006, as a block quote, because the literals are
in the acceptance criteria that a reader meets first (REQ-001 is 186 lines; its Notes are at the end) and
neither file has a place for amendments. For REQ-015, REQ-016, REQ-017, the epic and REQ-020 the notes go where
CR-011 put them (frontmatter `notes` where one exists, and a dated line in the body Notes or Amendments).

**D-5. How REQ-015 AC-9 is read and verified (Part B).** AC-9 requires change records ON REQ-001 and REQ-006 for
their literal minSdk 21 text: each file's `change_records` names a record, and that record says the literal minSdk
21 statements are superseded by REQ-015. One record covering both files meets it (the criterion says what the
records record, not how many there are). AC-9 itself is not edited. After the edits it is verified by three
checks, which the S1 implementation log cites (design traceability row 015 AC-9, line 1868, and S1-I7, line 536):

1. `CR-016` is listed under `change_records` in `REQ-MODERNIZATION-001-...md` and `REQ-MODERNIZATION-006-...md`
   (`grep -n "CR-016" <both files>` returns the frontmatter line and the dated note).
2. The dated note in each file says that the minSdk 21 statements are superseded by REQ-MODERNIZATION-015, and CR-016
   (status `implemented` once applied) names both files under `requirements`.
3. "Neither artifact is edited silently": `git diff` of each file between the commit before and the commit that
   applies this record shows only the frontmatter list and the added note; no acceptance-criterion line and no
   `status` line is in the diff.

The REQ-017 Notes sentence about REQ-001's literal 35 is satisfied by the same note (it names 35 and 37) and the
sentence is updated (B-5).

**D-6. REQ-020 and REQ-017: a cross-reference, not a change of criteria (Part D).** REQ-020 Context line 76 says
"REQ-MODERNIZATION-017 itself is not changed", and its Rationale keeps the lasting promise out of REQ-017. The
note added to REQ-017 changes no criterion and no constraint (it states sequencing and points at REQ-020), so
both stay true; to avoid a reader seeing a contradiction, edit D-7 rewords line 76 to "No acceptance criterion or
constraint of REQ-MODERNIZATION-017 is changed". The REQ-017 "Behavior preserved" constraint stands as written, because its story comes after the REQ-020 fix.
REQ-020 Constraints (last bullet) and Description already state the two deliberate differences (AC-14, AC-16);
edit D-11 puts them into the epic so that two approved texts do not conflict.

**D-7. One pass.** See Timing.

### Part A (before S1): the test-count rule

| # | File, place | Current text | Proposed text |
|---|-------------|--------------|---------------|
| A-1 | REQ-015 AC-13 (lines 163-165) | "- [ ] AC-13: `testDebugUnitTest` reports zero failures and no more than 2 skipped tests. The run count equals the sprint-017 baseline of 697 plus any tests added, minus only the tests deleted under AC-7, each listed by name." | "- [ ] AC-13: `testDebugUnitTest` reports zero failures and no more than 2 skipped tests. At its start the story records the unit-test count given by the authoritative count command of the project instructions (`.claude/CLAUDE.md`, "Authoritative test count"), with the id of the commit it was taken at: the commit immediately before the story's first commit. At the end of the story the same command gives a count equal to that start count, plus the tests added, minus only the tests deleted under AC-7; each added and each deleted test is listed by name. The total that the Gradle test run reports (skipped tests included) equals the end count, or the story records and explains the difference. No fixed baseline is stated, because the count changes with every story that adds or removes tests." |
| A-2 | REQ-017 AC-15 (lines 154-158) | "... passes on JDK 25 with the unit-test count not below the sprint-017 baseline (697 run, 2 skipped, adjusted only for tests deleted by other epic requirements and listed in their stories), no per-package JaCoCo floor lowered, and `gradle/verification-metadata.xml` updated for any new artifact with dependency verification still enforced." | "... passes on JDK 25 with no more than 2 skipped tests and with the unit-test count, taken by the authoritative count command of the project instructions (`.claude/CLAUDE.md`), not below the count recorded, with its commit id, at the start of the story. The only tests that may be subtracted are tests the story deletes because an acceptance criterion of another epic requirement requires it, each listed by name with that criterion. The Gradle run total (skipped included) equals the end count, or the story explains the difference. No per-package JaCoCo floor is lowered, and `gradle/verification-metadata.xml` is updated for any new artifact with dependency verification still enforced." |
| A-3 | Epic success criterion 7 (lines 119-121) | "7. **Tests green**: `./gradlew :app:testDebugUnitTest` completes with zero failures and no more skipped tests than the sprint-017 baseline (697 run, 2 skipped), allowing for tests deleted with the donation flow, which are listed in the story that deletes them." | "7. **Tests green**: `./gradlew :app:testDebugUnitTest` completes with zero failures and no more skipped tests than the sprint-017 baseline (2 skipped). The run count is accounted for story by story: each story records the count at its start, with the authoritative count command (`.claude/CLAUDE.md`) and the commit it was taken at, and lists by name every test it adds or deletes, as its requirement states (for example REQ-MODERNIZATION-015 AC-13 and REQ-MODERNIZATION-017 AC-15). The epic states no fixed run count." |
| A-4 | REQ-016 Context (line 42) | "The build runs on JDK 25 (Gradle 9.5.1 / AGP 9.0.1) with 697 unit tests (2 skipped)." | "The build runs on JDK 25 (Gradle 9.5.1 / AGP 9.0.1). On 2026-11-21, at commit `9e983665`, the authoritative count command of the project instructions gave 732 unit tests, 2 of them `@Ignore`d. The figure moves as stories add tests, and no criterion below depends on it." |
| A-5 | REQ-019 AC-B9 (line 112) | "- **AC-B9**: `./gradlew :app:testDebugUnitTest` has zero failures; the skipped count is no more than 2; the executed count is at least 697 minus tests deleted with the vendored module, each deletion listed with its replacement." | "- **AC-B9**: `./gradlew :app:testDebugUnitTest` has zero failures; the skipped count is no more than 2. At the end of each story, the unit-test count by the authoritative count command (`.claude/CLAUDE.md`) equals the count recorded, with its commit id, at that story's start, plus tests added, minus tests deleted with the vendored module, each deletion listed with its replacement. The Gradle run total equals the end count, or the story explains the difference." |
| A-6 | REQ-019 Context (line 60), frontmatter, Notes | Line 60 opens "- Baseline: 697 unit tests, 2 skipped, including the GreenMail IMAP integration tests ..." | Replace only the opening "Baseline: 697 unit tests, 2 skipped," with "Baseline: the unit-test count at the start of each story, by the authoritative count command (732 at commit `9e983665` on 2026-11-21), 2 skipped,"; the rest of the line is unchanged. Add `CR-016` to `change_records` after `CR-011`. Add a dated line after the CR-011 paragraph at the end of the file: "Amended 2026-11-21 by CR-016: AC-B9 and the Context baseline state the count rule, not a fixed 697. No other criterion is changed; the edits that CR-005, CR-006 and CR-014 (Edit 6) make to this file are still applied with the Part B design." |

Checked and left: REQ-015 AC-14, REQ-016 AC-5 (no count; "no more than 2 skipped" stays true), REQ-016 Constraints,
REQ-017 AC-15's other clauses, epic Description line 19 (history of sprint-017), REQ-018 lines 128 and 169
(implemented; the text already says to re-baseline). REQ-019 AC-B10 reads correctly against the configured floors.

### Part B (before S1): REQ-001, REQ-006 and REQ-015 AC-9

| # | File, place | Current text | Proposed text |
|---|-------------|--------------|---------------|
| B-1 | REQ-001 frontmatter (line 12) | `change_records: []` | `change_records:` with one item, `CR-016` |
| B-2 | REQ-001, directly under the H1 (line 15) | (none) | Add a block quote: "> Amended 2026-11-21 by CR-016. Note only; no criterion, level or status is changed. The levels this requirement names, wherever they appear in this file (`compileSdkVersion` and `targetSdkVersion` 35, `minSdkVersion` 21), are the levels it required for the 29-to-35 uplift. They are not the current requirement. Its literal minSdk 21 statements are superseded by REQ-MODERNIZATION-015, which sets `minSdkVersion` 24 (AC-5, as amended by CR-011). Its literal 35 is superseded, for future state, by REQ-MODERNIZATION-017, which sets `compileSdkVersion` and `targetSdkVersion` 37 (AC-2). This note is the change record that REQ-MODERNIZATION-015 AC-9 and the Notes of REQ-MODERNIZATION-017 call for. It does not say whether criteria 28 to 30 were met." |
| B-3 | REQ-006 frontmatter (line 16) | `change_records: []` | `change_records:` with one item, `CR-016` |
| B-4 | REQ-006, directly under the H1 (line 21) | (none) | Add a block quote: "> Amended 2026-11-21 by CR-016. Note only; no criterion, level or status is changed. The minSdk value in this requirement (21: Description, Context, AC-3, AC-5 "API 21+", Constraints) is the value it required when it was implemented. These literal minSdk 21 statements are superseded by REQ-MODERNIZATION-015, which sets minSdk 24 (AC-5, as amended by CR-011). The criteria on the dead calendar shim and on `StatusPreference` are not affected. This note is the change record that REQ-MODERNIZATION-015 AC-9 calls for." |
| B-5 | REQ-017 Notes (lines 194-195) | "Supersedes the literal "35" in REQ-MODERNIZATION-001 AC-1 and AC-29 only for future state; REQ-MODERNIZATION-001 should carry a change record." | "Supersedes the literal "35" in REQ-MODERNIZATION-001 AC-1 and AC-29 only for future state; REQ-MODERNIZATION-001 carries the change record (CR-016, 2026-11-21), as a dated note that leaves its criteria unchanged." |
| B-6 | REQ-015 frontmatter `notes` (lines 22-25 end) | "... Those artifacts need change records (AC-9 below); the orchestrator must create them, because this draft cannot edit files." (keep as history) | Append, after the CR-011 note: "Amended 2026-11-21 by CR-016: AC-9 is met by CR-016, which adds a dated note and `change_records` to REQ-MODERNIZATION-001 and REQ-MODERNIZATION-006; AC-13 no longer states a fixed baseline." |

After B-1 to B-6, REQ-015 AC-9 is unchanged and is verified by the three checks in D-5. The epic's criterion 2
("REQ-MODERNIZATION-001 and REQ-MODERNIZATION-006 carry change records recording the supersession") is verified by
the same checks, and needs no edit. The epic's In Scope text (lines 48-50: "those artifacts must be revised
through change records, not silently contradicted") is also met and needs no edit.

### Part C (before S4): coverage floors

| # | File, place | Current text | Proposed text |
|---|-------------|--------------|---------------|
| C-1 | REQ-016 AC-6 (lines 92-93) | "- [ ] AC-6: `./gradlew :app:jacocoTestCoverageVerification` passes with no per-package floor lowered (receiver at or above 50%, other packages at or above 70%)." | "- [ ] AC-6: `./gradlew :app:jacocoTestCoverageVerification` passes, and no per-package floor is lowered. Every package meets the floor configured for it in the `violationRules` block of `app/build.gradle` (`receiver` at or above its 50% floor). No `minimum` in that block is lowered by this work, as `git diff` of `app/build.gradle` from the story's start commit shows. The configured floors differ by package; they are not a uniform 70%." |
| C-2 | Epic success criterion 8 (lines 122-125) | "8. **Gates hold**: `./gradlew :app:jacocoTestCoverageVerification` passes with no per-package floor lowered (receiver stays at or above its 50% floor, others at or above 70%); dependency verification remains enforced and `gradle/verification-metadata.xml` covers every new artifact; `./gradlew :app:assembleDebug` succeeds on JDK 25." | "8. **Gates hold**: `./gradlew :app:jacocoTestCoverageVerification` passes with no per-package floor lowered (every package meets the floor configured for it in `app/build.gradle`, `receiver` stays at or above its 50% floor, and no `minimum` is lowered by the work of this epic); dependency verification remains enforced and `gradle/verification-metadata.xml` covers every new artifact; `./gradlew :app:assembleDebug` succeeds on JDK 25." |

C-1 brings REQ-016 into line with REQ-015 AC-14, which already says "at or above its configured minimum". The
criterion deliberately asks nothing of the 13 packages below 0.70 (D-3, SC-2), and names no package list or
history of the floors (the 0.70 floors on `service`, `mail` and `auth` came from U-006 and REQ-003, and U-051 left
them unchanged; a list would also go stale if a floor were raised). REQ-015 AC-14, REQ-017 AC-15 and REQ-019
AC-B10 already read correctly against the configured floors and are unchanged.

### Part D: cross-references to REQ-MODERNIZATION-020

| # | File, place | Current text | Proposed text |
|---|-------------|--------------|---------------|
| D-1 | Epic Sequencing (first bullet, ends "(analysis condition 1, decision Record 4)." line 152) | "... Requirements 1, 2 and 3 (toolchain and minSdk 24, runtime libraries, platform level 37) are complete before any Part B row that compiles Thunderbird code (analysis condition 1, decision Record 4)." | Add a new bullet after the first: "- REQ-MODERNIZATION-020 (every SMS that is visible to the app is backed up exactly once, including a message that becomes visible late) is a correctness requirement raised under this epic; it is not one of requirements 1 to 5. The targetSdk half of requirement 3 (`targetSdkVersion` 37) comes after its fix, unless the platform measurement owned by design DES-MODERNIZATION-014 (decision D14) records the outcome "measured not affected". Any other outcome, and a measurement that was not run, means the targetSdk step waits for the fix. `compileSdkVersion` 37 does not wait (decision Record 13, row 4)." |
| D-2 | Epic Related Requirements (after item 5, line 138) | "5. Replace the vendored K-9 mail library with Thunderbird for Android mail libraries." | Add: "6. Back up every SMS that is visible to the app exactly once, including a message that becomes visible after newer messages were backed up (REQ-MODERNIZATION-020). A correctness requirement raised under this epic, not a dependency-currency requirement; the targetSdk half of requirement 3 depends on it (see Sequencing)." |
| D-3 | REQ-017 frontmatter `notes` (lines 23-25) | "Sequencing (epic Notes): after the donation-flow removal requirement, and after the toolchain and minSdk 24 requirement (referenced in the brief as REQ-MODERNIZATION-015). Neither REQ-MODERNIZATION-015 nor -016 exists on disk yet; those references are unverified." | "Sequencing (epic Notes): after the donation-flow removal requirement (REQ-MODERNIZATION-018), after the toolchain and minSdk 24 requirement (REQ-MODERNIZATION-015), and, for the targetSdk step, after the fix required by REQ-MODERNIZATION-020 (see Notes). REQ-MODERNIZATION-015, -016, -018 and -020 exist and are approved (checked 2026-11-21)." |
| D-4 | REQ-017 frontmatter `related_requirements` (lines 5-7) | `REQ-MODERNIZATION-001`, `REQ-MODERNIZATION-003` | Add `REQ-MODERNIZATION-015` and `REQ-MODERNIZATION-020` |
| D-5 | REQ-017 Notes, after the CR-011 paragraph (end of file) | (none) | Add: "Amended 2026-11-21 by CR-016: Sequencing for REQ-MODERNIZATION-020 (back up every SMS visible to the app exactly once). The targetSdk change of this requirement (`targetSdkVersion` 37) comes after the fix that REQ-MODERNIZATION-020 requires, unless the platform measurement owned by design DES-MODERNIZATION-014 (decision D14) records "measured not affected"; every other outcome, and a measurement not run, means it waits. `compileSdkVersion` 37 does not wait (decision Record 13, row 4). The one-time-code SMS change at level 37 is dispositioned under AC-3 as usual: where it is "applicable and fixed", the fix lands before the raise (decision Record 13 row 4; AC-5 itself allows no later than the same PR); where the measurement records "measured not affected", it is "not applicable" with the run as evidence. No acceptance criterion or constraint of this requirement changes; "Behavior preserved" stands as written. AC-15 states the count rule. Also: the frontmatter note that REQ-MODERNIZATION-015 and -016 do not exist on disk is corrected; the Notes sentence on REQ-MODERNIZATION-001 is updated (see Part B)." |
| D-6 | REQ-020 frontmatter (line 13) | `change_records: []` | `change_records:` with one item, `CR-016` |
| D-7 | REQ-020 Context (line 76) | "REQ-MODERNIZATION-017 itself is not changed." | "No acceptance criterion or constraint of REQ-MODERNIZATION-017 is changed; it carries a sequencing note that points here (CR-016)." |
| D-8 | REQ-020 Notes (lines 459-461) | "The epic's text has no line for a requirement of this kind. A line has been proposed to the orchestrator; the epic is approved and is not edited here." | "The epic's text had no line for a requirement of this kind. Since 2026-11-21 (CR-016) the epic's Related Requirements and Sequencing carry one." |
| D-9 | REQ-020 Notes, end | (none) | Add: "Amended 2026-11-21 by CR-016: cross-references only (Context sentence on REQ-MODERNIZATION-017; the epic sentence in Notes). No acceptance criterion, constraint or priority is changed." |
| D-10 | Epic Amendments (end of file) | (two bullets: CR-011, CR-010) | Add a dated bullet "Amended 2026-11-21 by CR-016: success criteria 7 and 8 (no fixed test count; floors as configured), Related Requirements item 6 and a Sequencing bullet for REQ-MODERNIZATION-020, and the Description's list of deliberate behaviour changes. Reason: design DES-MODERNIZATION-014 findings FD-VAL-002 and FD-VAL-003; Record 14 rows 9 and 10." |
| D-11 | Epic Description (lines 29-30) | "(other than the removed donation flow and the dropped Android 5.0 to 6.0 support)" | "(other than the removed donation flow, the dropped Android 5.0 to 6.0 support, and the Skip and restore outcomes that REQ-MODERNIZATION-020 AC-14 and AC-16 change on purpose)". Success criterion 9 is unchanged (it compares IMAP round-trip content across the mail migration, which REQ-MODERNIZATION-020 does not touch). |

Checked against the documents as they are now: design D14 outcomes (lines 1431-1438) give exactly the rule in
D-1 and D-5 (only "Measured not affected" lets S5-I4 go without the fix; every other outcome and "Not run" wait);
REQ-020 Dependencies (lines 465-466) say the same and that compileSdk 37 does not wait. The epic's text mentions
a design id: REQ-020 and REQ-017 already cite DES-MODERNIZATION-014, and the outcome name is defined only there, so
the reference is kept (no design step ids such as S5-I1 are used).

### Frontmatter and dated notes (Parts A to D, all files)

Add `CR-016` to `change_records` (appended after the existing entries) on REQ-015 (after `CR-011`), REQ-016 (after
`CR-011`), REQ-017 (after `CR-011`), the epic (after `CR-010`), REQ-019 (after `CR-011`), and set it on REQ-001, REQ-006 and REQ-020
(B-1, B-3, D-6). Date every note 2026-11-21, naming CR-016. `status: approved` stays on all eight files. Dated notes
in body Notes (as CR-011 did): REQ-015 (A-1: "Amended 2026-11-21 by CR-016: AC-13 states the count rule, not a fixed
baseline; AC-9 is met by CR-016"), REQ-016 (A-4 and C-1: "Amended 2026-11-21 by CR-016: Context count restated;
AC-6 states the configured floors"), REQ-017 (D-5 covers AC-15 too; the paragraph says "AC-15 states the count
rule"), REQ-019 (A-6), epic Amendments (D-10), REQ-020 (D-9), REQ-001 and REQ-006 (B-2, B-4).

### Part E: private-app wording (NOT APPLIED, Record 13 row 6)

**Not applied (Record 13 row 6).** The ruling stands: "no change record now; one is raised only if a story is
blocked". The Requirements Reviewer found on 2026-11-21 that none of these lines blocks a story; AC-10's entry is
met by an entry in the repository file `CHANGES` (the file exists, and design S1-I7 already plans the entry at
line 536); the epic's and REQ-017's lines are informational. The rows are kept only so that the text is on the
page if a story is ever blocked. Nothing in Parts A to D, X-1 or the checklists depends on them. (SC-4, REQ-001
criteria 28 to 30, is not recorded beyond the B-2 sentence "It does not say whether criteria 28 to 30 were met";
raise it only if a story cites them.)

| # | File, place | Current text | Proposed text (not applied) |
|---|-------------|--------------|-----------------------------|
| E-1 | REQ-015 AC-10 (lines 150-152) | "- [ ] AC-10: A user-facing changelog or release-notes entry states that Android 5.0, 5.1 and 6.0 (API 21 to 23) are no longer supported, in the location the project already uses for release notes. The story names that location. Publishing and rollout remain out of scope." | "- [ ] AC-10: The repository's `CHANGES` file carries an entry stating that Android 5.0, 5.1 and 6.0 (API 21 to 23) are no longer supported. The story names the entry. The app is private and unpublished; publishing and rollout are out of scope." |
| E-2 | Epic Constraints and Risks (lines 165-166) | "Dropping Android 5.0 to 6.0 (API 21 to 23) is a user-facing support reduction; whether to communicate it is a publishing matter and is not decided here." | "Dropping Android 5.0 to 6.0 (API 21 to 23) reduces the supported devices. The app is private and unpublished, and the owner's devices all run Android 11 or newer (decision Record 5, row 8, as quoted in CR-011); the change is noted in `CHANGES` (REQ-MODERNIZATION-015 AC-10)." |
| E-3 | REQ-017 Constraints (lines 178-179) | "Informational only, not acceptance criteria: Google Play target-level policy deadlines are noted in the story for the product owner; publishing is not decided here (epic Out of Scope)." | "The app is private and unpublished, so Google Play target-level policy deadlines do not apply to it and need no note in the story (the same finding as CR-003 for REQ-MODERNIZATION-018)." |

Left in any case: the epic Out of Scope line 73 ("Publishing and release decisions (store listing, release
signing, rollout)"), which is a scope boundary and stays true; the dated notes of CR-011 that mention these lines.

### X-1: the design note (DES-MODERNIZATION-014; insert-only)

The design is approved. Changing the 46 lines that mention P-1 to P-3 would rewrite an approved design to change
labels only; the labels stay correct once mapped. One dated note discharges the design's open conditions.

| # | File, place | Current text | Proposed text |
|---|-------------|--------------|---------------|
| X-1 | `DES-MODERNIZATION-014-toolchain-runtime-libraries-platform-level-design.md`, directly under the H1; and frontmatter `change_records` | (none); `change_records` lists `CR-009`, `CR-010`, `CR-011` | Insert, as a block quote under the H1, no other line changed: "> Note added 2026-11-21 (references only; no decision of this design changes). The change records this design calls P-1, P-2 and P-3 exist. P-1 is CR-015 (CNTR-MODERNIZATION-004 and DES-MODERNIZATION-005: the pre-API-24 content-trigger fallback). P-2 and P-3 are one record, CR-016 (requirements wording), as this design allows when the record is raised before S1. Throughout, read "change record P-1" as CR-015 and "change record P-2" or "P-3" as CR-016. Once a record is applied (status `implemented`), every condition this design places on it is met. That includes "Conditional on P-3" in the traceability table, "until it rules" in D8 item 5, and the remediations of FD-VAL-002 and FD-VAL-003. After CR-016 is applied, REQ-MODERNIZATION-015 AC-13, REQ-MODERNIZATION-016 AC-6 and REQ-MODERNIZATION-017 AC-15 state the practice this design already follows: the count at the start of each story, and the configured floors." Add `CR-015` and `CR-016` to the design's `change_records` (the Librarian's call whether the field is edited by hand; the note is what matters). |

---

## Amendment 1 (2026-11-21): the count rule's start and end points, and two notes

**Source.** After the 25 requirement edits and design note X-1 were applied (2026-11-21), an independent Requirements Reviewer checked the applied text and wrote `sdlc/artifacts/build/change-records/evidence/CR-016-applied-text-check.md`. Its verdict is `Verdict: FAIL`. Every edit was applied with the record's final text. The FAIL rests on one must-fix, F-1 (the test-count rule does not say where the end count is taken or which commit is the start commit), and three should-fix findings, F-2, F-3 and F-4.

**Ruling (orchestrator, 2026-11-21).** F-1, F-2 and F-3 are fixed now, by this amendment, applied in the same pass. This amendment is insert-only and adds edits A-7, A-8, A-9, A-10, D-12 and D-13. It changes no ruling of this record: the count rule (the count at the start of the story, plus tests added, minus tests deleted, each named) and the "no fixed figure" decision stand. F-4 is handled as follows. The two extra sentences that the check found in the REQ-015 and REQ-016 dated notes are recorded under "Departures from the proposed text" below. The frontmatter `requirements` list is not changed (see "F-4" below). The status change and the Execution and Closure bookkeeping are the orchestrator's, after the commit. Notes N-1 to N-4 and the leftover rulings (a) to (f) of the report are recorded as "no action", with the reviewer's caution on (c) kept for the Part B (mail migration) design.

**Counts, restated.** The "Counts, stated once" paragraph at the top says 25 numbered edits in 8 requirement files plus X-1. With this amendment the record has 25 + 6 = 31 numbered requirement edits (the 6 new ones are A-7 to A-10, D-12 and D-13). The number of requirement files edited is still 8 (REQ-001, -006, -015, -016, -017, -019, -020 and EPIC-MODERNIZATION-006), and the design file count (one, X-1) is unchanged. A-7 to A-10, D-12 and D-13 touch REQ-015, -016, -017, -019 and -020. The earlier wording is not rewritten.

### New edits

"Current text" was read from the working-tree files on 2026-11-21 and is quoted exactly; line breaks inside the files are shown as single spaces. Apply by the quoted text, not by line number. `status: approved` and each file's `id` and `artifact_type` stay; `change_records` already lists `CR-016` in each file.

| # | File and place | Current text | Replacement text | Finding |
|---|----------------|--------------|------------------|---------|
| A-7 | REQ-MODERNIZATION-015 AC-13 (the whole criterion) | "- [ ] AC-13: `testDebugUnitTest` reports zero failures and no more than 2 skipped tests. At its start the story records the unit-test count given by the authoritative count command of the project instructions (`.claude/CLAUDE.md`, "Authoritative test count"), with the id of the commit it was taken at: the commit immediately before the story's first commit. At the end of the story the same command gives a count equal to that start count, plus the tests added, minus only the tests deleted under AC-7; each added and each deleted test is listed by name. The total that the Gradle test run reports (skipped tests included) equals the end count, or the story records and explains the difference. No fixed baseline is stated, because the count changes with every story that adds or removes tests." | "- [ ] AC-13: `testDebugUnitTest` reports zero failures and no more than 2 skipped tests. At its start the story records the unit-test count given by the authoritative count command of the project instructions (`.claude/CLAUDE.md`, "Authoritative test count"), with the id of the commit it was taken at (`HEAD` when the story starts, before its first change). At the end of the story the same command, run with the story's last commit in place of `HEAD`, gives a count equal to that start count, plus the tests added, minus only the tests deleted under AC-7; each added and each deleted test is listed by name. If the end count is taken before the story's changes are committed, it is taken over the working tree with the same pattern (`git grep --untracked -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' \| grep -c "@Test"`), and the story says so. Tests added or deleted by changes that are not the story's, between the start commit and the end count, are listed with their commit id (or, if uncommitted, named as such) and are not counted as the story's. The total that the Gradle test run reports (skipped tests included) equals the end count, or the story records and explains the difference. No fixed baseline is stated, because the count changes with every story that adds or removes tests." | F-1 |
| A-8 | REQ-MODERNIZATION-017 AC-15 (insertion) | "... not below the count recorded, with its commit id, at the start of the story. The only tests that may be subtracted ..." | "... not below the count recorded, with its commit id, at the start of the story (start and end counts taken at the points that REQ-MODERNIZATION-015 AC-13 states). The only tests that may be subtracted ..." | F-1 |
| A-9 | REQ-MODERNIZATION-019 AC-B9 (insertion) | "... equals the count recorded, with its commit id, at that story's start, plus tests added, ..." | "... equals the count recorded, with its commit id, at that story's start (both counts taken at the points that REQ-MODERNIZATION-015 AC-13 states), plus tests added, ..." | F-1 |
| A-10 | REQ-MODERNIZATION-016 AC-6 (phrase replaced) | "... as `git diff` of `app/build.gradle` from the story's start commit shows. ..." | "... as `git diff` of `app/build.gradle` from the commit recorded at the story's start (as REQ-MODERNIZATION-015 AC-13 defines it) shows. ..." | F-1 |
| D-12 | REQ-MODERNIZATION-017 Notes, the CR-016 dated note, sentence that follows the "not applicable" sentence | "No acceptance criterion or constraint of this requirement changes; "Behavior preserved" stands as written. AC-15 states the count rule." | "The sequencing changes no acceptance criterion or constraint of this requirement; "Behavior preserved" stands as written. AC-15 is reworded by CR-016 (A-2) to state the count rule." | F-2 |
| D-13 | REQ-MODERNIZATION-020 Context, last sentence (the sentence of edit D-7) | "No acceptance criterion or constraint of REQ-MODERNIZATION-017 is changed; it carries a sequencing note that points here (CR-016)." | "This requirement changes no acceptance criterion or constraint of REQ-MODERNIZATION-017; REQ-MODERNIZATION-017 carries a sequencing note that points here (CR-016)." | F-3 |

**Where this amendment departs from the check report's replacement texts.** The report's texts are used, with two corrections, both in A-7.

1. **A-7: "run with `HEAD` at the story's last commit" became "run with the story's last commit in place of `HEAD`".** The command in `.claude/CLAUDE.md` has the token `HEAD` in it. "With `HEAD` at the last commit" can be read as "check out that commit first", which fails when later commits exist. Naming the substitution is the same fact and can be done in one step. At the start of a story the text still says `HEAD`.
2. **A-7: "Tests added or deleted by commits that are not the story's" became "by changes that are not the story's ... (or, if uncommitted, named as such)".** The working-tree form counts every uncommitted test in the checkout, including another story's uncommitted tests, which "commits" does not cover. The same wording rule (list it, do not count it as the story's) is applied to them.
The working-tree command is kept as the report wrote it; it is checked below.

**Checks run on 2026-11-21 at HEAD `1f8ebe3b` (read-only, no uncommitted test changes in the checkout).**

- `.claude/CLAUDE.md` line 33 reads: "**Authoritative test count** (agents routinely misreport): `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`." The phrase "the authoritative count command of the project instructions (`.claude/CLAUDE.md`, "Authoritative test count")" matches it.
- The committed form, `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`, prints **746**.
- The working-tree form, `git grep --untracked -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`, is valid and prints **746**. `--untracked` searches untracked, not-ignored files as well as the tracked working-tree files, so it counts new test files and uncommitted edits that the `HEAD` form cannot see. (It is 746, not the 732 of the record's Evidence 1, because story U-066 has since been committed at `1f8ebe3b`.)
- A-8 to A-10 refer to the points that A-7 states, so the rule is defined once.

**Note on F-4 (the `requirements` list).** The frontmatter `requirements` list of this record holds requirement ids. CR-010 and CR-011 edited `EPIC-MODERNIZATION-006` and list only `REQ-MODERNIZATION-*` ids there. The epic is named in this record's body, in edits A-3, C-2, D-1, D-2, D-10 and D-11, and in the Impact Analysis. To follow that convention and not to change a list's meaning without a schema ruling, the list is left as it is. If the orchestrator rules that epics belong in it, the change is one added line, `EPIC-MODERNIZATION-006`, and does not affect any edit.

### Notes (no action)

- **N-1.** The A-4 text says the authoritative command gave "732 unit tests, 2 of them `@Ignore`d". The command gives only the 732; the 2 comes from counting `@Ignore` annotations (`AppTest.java:13`, `AuthPreferencesTest.java:1079`). True as a fact; no criterion depends on it. No action.
- **N-2.** REQ-017 AC-15 lets a story subtract only tests that another epic requirement requires it to delete, and REQ-017 AC-13 can force a test off an unsupported `@Config(sdk=...)`. Retargeting is allowed, so they do not contradict. The S3 or S5 story author retargets such a test and does not delete it. No action.
- **N-3.** REQ-016 AC-6 forbids lowering a `minimum` but says nothing about narrowing `includes` or adding `excludes`. Design D8 item 1 forbids both; the story cites D8. No action.
- **N-4.** Epic criterion 7 applies the start-count record to every story; REQ-016 AC-5 has no count rule, so the S4 author takes the rule from criterion 7. REQ-020 Constraints are looser (no 2-skip cap; "any difference is explained") and compatible. The BUG-021 fix story meets both. No action.
- **Leftover rulings (a) to (f)** of the report: (a) epic Description "697 unit tests green" stays (history of sprint-017); (b) REQ-018 682 and 697 stay; (c) REQ-017 "Behavior preserved" and epic criterion 9 stay beside REQ-020 AC-14 and AC-16; (d) the epic's "Planned requirements:" wording stays until the next epic amendment; (e) "requirements 1 to 4" and "1 to 5" in the Sequencing text stay; (f) REQ-020 `related_requirements` is not extended. All six: no action. **Caution on (c), kept for the Part B (mail migration) design:** take the "before" capture for epic criterion 9 after the BUG-021 fix lands, or allow-list the REQ-020 AC-16 restore outcome.

### Departures from the proposed text

(Recorded here by this amendment, as the orchestrator ruled for F-4. The orchestrator completes the rest of Execution.)

- **REQ-MODERNIZATION-015, body dated note.** It adds, beyond the quoted note of Part A and B-6, the words "which adds a dated note and `change_records` to REQ-MODERNIZATION-001 and REQ-MODERNIZATION-006. No other criterion is changed." The additions are true.
- **REQ-MODERNIZATION-016, body dated note.** It adds, beyond the quoted note, the sentence "No other criterion is changed." The addition is true.
- **Amendment 1 itself** replaces the text of edits A-1 (REQ-015 AC-13), A-2 (REQ-017 AC-15, by insertion), A-5 (REQ-019 AC-B9, by insertion), C-1 (REQ-016 AC-6, one phrase) and D-5 and D-7 (the two "no criterion changes" sentences) as stated in the table above. For these places the amendment's text governs.

---

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->

Advisory, from the Solution Architect (2026-11-21). The record is sound and its Parts A to D are the right
scope. The rulings below change the wording of six edits, add three (two for SC-1, one for SC-5), and give
the design note. Sources were re-read at HEAD `9e983665`. The uncommitted TLS work under `app/src/` and
`k9mail-vendored/` was not used.

### Rulings

1. **Part A: accept the rule, with two changes.** (a) The start count is recorded together with the id of
   the commit it was taken at: the commit just before the story's first commit. That is what makes the
   number hard to fake. Anyone can re-run the command at that commit, and `git log` shows whether it is
   the right commit, so a convenient number does not reproduce. The story's implementation log holds both
   values, as U-066 does today. (b) Both ends of the equation use the same tool. The start and end counts
   both come from the authoritative command, and the Gradle run total is checked against the end count. As
   drafted, A-1 compares a `git grep` start with a Gradle end. Using one tool at both ends also deals with
   SC-7 without a separate rule about comments. A comment line containing `@Test` that is already there
   appears in both counts and cancels out. One that the story adds breaks the equation and the
   cross-check, so it surfaces. Today all 732 matches are real annotation lines, and no test class uses a
   parameterized or theory runner (checked at HEAD). The same rule fits REQ-017 AC-15, which keeps its
   "not below" form and its exception: a test may be subtracted only when another epic requirement's
   criterion requires the deletion. It also fits epic criterion 7. The epic should not cite REQ-015 AC-13
   alone, because AC-13 allows only AC-7 deletions.
2. **SC-1: amend REQ-019 now (A-5, A-6).** The defect is the same and so is the rule. Line 60 ("Baseline:
   697") is false today. AC-B9's "at least 697" is weaker than the rule. A Part B design written against
   the stale text would inherit it. The edits that CR-005, CR-006 and CR-014 (Edit 6) queue for REQ-019 are
   on other lines (AC-B8 and the contract references), are found by their quoted text, and do not
   conflict. The only cost is one more dated note on REQ-019, which those records will add notes to anyway.
   The Librarian adds REQ-MODERNIZATION-019 to this record's `requirements` list. That takes the record to
   8 files.
3. **Part B: confirmed, with one correction.** Use dated notes, leave the literal text as it is, and add no
   `superseded` status, because `superseded` is terminal for the whole requirement and neither file is
   wholly superseded. AC-9's words are: change records "created on" both files "recording that their
   literal minSdk 21 statements are superseded by this requirement", and "neither artifact is edited
   silently". CR-016 meets this when it is listed in both files' `change_records`, its own `requirements`
   names both files, and each note says "superseded". B-4 says it. B-2 does not: it says "not the current
   requirement" and "now governed by". Replace B-2 with the text below. Also drop "and that were verified"
   from B-2 and B-4. That claim cannot be supported for REQ-001 criteria 28 to 30 (SC-4).
4. **Part C: accept. A decision supports nothing that the new text loses.** "Others at or above 70%" was
   never true of the configured gate, and no decision commissions an uplift: design D8 items 1 and 5 say
   no `minimum` changes, and the floors carry TODO comments. REQ-015 AC-14 already uses the configured
   wording ("at or above its configured minimum"), so C-1 brings REQ-016 into line with it. Cut C-1's last
   sentence. It gets the history wrong: the `service`, `mail` and `auth` floors at 0.70 came from U-006
   (REQ-003), and U-051 left them unchanged (Evidence 2). It would also go stale if a floor were raised. D8
   item 2 (the receiver test for headroom) is a design step, not requirement wording, and needs no text
   here. D-3 is right not to add "no floor is raised". The design is stricter for its own stories, and
   that does not conflict.
5. **Part D: D-1 matches the D14 outcome table** (`DES-...-014:1431-1438`): only "Measured not affected"
   lets the targetSdk step go without the fix; every other outcome and "Not run" wait. It also matches
   REQ-020's Dependencies, and compileSdk does not wait (Record 13 row 4). D-4 is right: REQ-017 depends on
   REQ-015 (minSdk 24 in its Constraints) and REQ-020. D-5 has one sentence that overstates: REQ-017 AC-5
   allows "the same PR as the level raise or earlier", so "before the raise" comes from Record 13 row 4,
   not from AC-5 (text below). D-6 to D-9 are right and needed: once Part D is applied, REQ-020's line 76
   ("REQ-MODERNIZATION-017 itself is not changed") and its Notes ("the epic ... is not edited here") would
   be false. The record whose edits make them false is the right record to correct them. No REQ-020
   criterion is touched.
6. **SC-5: add the clause, as edit D-11.** REQ-020's Constraints (lines 415-416) and Description (lines
   48-49) already state the two deliberate differences. Linking REQ-020 into the epic (D-1, D-2) without
   them would leave two approved texts in conflict. This changes no scope: REQ-020 is approved (Record 14
   row 10). Correction to SC-5: epic criterion 9 compares IMAP round-trip content across the mail
   migration, which REQ-020 does not touch, so it needs no edit. REQ-017's "Behavior preserved" also
   stands, because its story comes after the REQ-020 fix.
7. **Part E: leave it** (Record 13 row 6). No story is blocked, and design S1-I7 already plans the
   `CHANGES` entry for AC-10. **SC-4: record nothing now** beyond the B-2 wording, which no longer claims
   the levels were verified and says it does not settle criteria 28 to 30. Marking them "not applicable"
   would decide a closed requirement's history, and no story depends on them. Raise it only if a story
   cites them.
8. **Timing: confirmed.** Apply A to D, with A-5, A-6 and D-11, in one pass after approval and before S1 is
   authored. This satisfies Record 14 row 9 (P-2 before the minSdk story is planned, P-3 before the
   runtime-library story). Applying Part C early costs nothing, since no story before S4 changes a
   `minimum`.
9. **Design follow-through: one dated note under the design's H1 is enough.** Changing the 46 lines that
   mention P-1 to P-3 would rewrite an approved design to change labels only. The labels stay correct once
   they are mapped. The note also has to discharge the design's open conditions ("Conditional on P-3", D8
   item 5's "until it rules", FD-VAL-002 and FD-VAL-003), so that a planner does not read them as still
   open. The text is below. Whether the design's frontmatter gains `change_records` is the Librarian's
   call.

### Replacement text for the record's edits

Only the quoted text changes. Each row keeps the record's "File, place" and "Current text".

- **A-1 (REQ-015 AC-13), proposed text:** "- [ ] AC-13: `testDebugUnitTest` reports zero failures and no
  more than 2 skipped tests. At its start the story records the unit-test count given by the authoritative
  count command of the project instructions (`.claude/CLAUDE.md`, "Authoritative test count"), with the id
  of the commit it was taken at: the commit immediately before the story's first commit. At the end of the
  story the same command gives a count equal to that start count, plus the tests added, minus only the
  tests deleted under AC-7; each added and each deleted test is listed by name. The total that the Gradle
  test run reports (skipped tests included) equals the end count, or the story records and explains the
  difference. No fixed baseline is stated, because the count changes with every story that adds or removes
  tests."
- **A-2 (REQ-017 AC-15), proposed text:** "... passes on JDK 25 with no more than 2 skipped tests and with
  the unit-test count, taken by the authoritative count command of the project instructions
  (`.claude/CLAUDE.md`), not below the count recorded, with its commit id, at the start of the story. The
  only tests that may be subtracted are tests the story deletes because an acceptance criterion of another
  epic requirement requires it, each listed by name with that criterion. The Gradle run total (skipped
  included) equals the end count, or the story explains the difference. No per-package JaCoCo floor is
  lowered, and `gradle/verification-metadata.xml` is updated for any new artifact with dependency
  verification still enforced."
- **A-3 (epic criterion 7), proposed text:** "7. **Tests green**: `./gradlew :app:testDebugUnitTest`
  completes with zero failures and no more skipped tests than the sprint-017 baseline (2 skipped). The run
  count is accounted for story by story: each story records the count at its start, with the authoritative
  count command (`.claude/CLAUDE.md`) and the commit it was taken at, and lists by name every test it adds
  or deletes, as its requirement states (for example REQ-MODERNIZATION-015 AC-13 and REQ-MODERNIZATION-017
  AC-15). The epic states no fixed run count."
- **A-4 (REQ-016 Context), proposed text:** "The build runs on JDK 25 (Gradle 9.5.1 / AGP 9.0.1). On
  2026-11-21, at commit `9e983665`, the authoritative count command of the project instructions gave 732
  unit tests, 2 of them `@Ignore`d. The figure moves as stories add tests, and no criterion below depends
  on it."
- **A-5 (new; REQ-019 AC-B9, line 112), current text:** "- **AC-B9**: `./gradlew :app:testDebugUnitTest`
  has zero failures; the skipped count is no more than 2; the executed count is at least 697 minus tests
  deleted with the vendored module, each deletion listed with its replacement." **Proposed text:** "-
  **AC-B9**: `./gradlew :app:testDebugUnitTest` has zero failures; the skipped count is no more than 2. At
  the end of each story, the unit-test count by the authoritative count command (`.claude/CLAUDE.md`)
  equals the count recorded, with its commit id, at that story's start, plus tests added, minus tests
  deleted with the vendored module, each deletion listed with its replacement. The Gradle run total equals
  the end count, or the story explains the difference."
- **A-6 (new; REQ-019 Context, line 60):** replace only the opening "Baseline: 697 unit tests, 2 skipped,"
  with "Baseline: the unit-test count at the start of each story, by the authoritative count command (732
  at commit `9e983665` on 2026-11-21), 2 skipped,". The rest of the line is unchanged. Add `CR-016` to
  REQ-019's `change_records` after `CR-011`, and add a dated line to its Notes: "Amended 2026-11-21 by
  CR-016: AC-B9 and the Context baseline state the count rule, not a fixed 697. No other criterion is
  changed; the edits that CR-005, CR-006 and CR-014 (Edit 6) make to this file are still applied with the
  Part B design."
- **B-2 (REQ-001 note), proposed text:** "> Amended 2026-11-21 by CR-016. Note only; no criterion, level
  or status is changed. The levels this requirement names, wherever they appear in this file
  (`compileSdkVersion` and `targetSdkVersion` 35, `minSdkVersion` 21), are the levels it required for the
  29-to-35 uplift. They are not the current requirement. Its literal minSdk 21 statements are superseded by
  REQ-MODERNIZATION-015, which sets `minSdkVersion` 24 (AC-5, as amended by CR-011). Its literal 35 is
  superseded, for future state, by REQ-MODERNIZATION-017, which sets `compileSdkVersion` and
  `targetSdkVersion` 37 (AC-2). This note is the change record that REQ-MODERNIZATION-015 AC-9 and the
  Notes of REQ-MODERNIZATION-017 call for. It does not say whether criteria 28 to 30 were met."
- **B-4 (REQ-006 note):** replace "is the value it required, and that was verified, when it was
  implemented. It is superseded by REQ-MODERNIZATION-015" with "is the value it required when it was
  implemented. These literal minSdk 21 statements are superseded by REQ-MODERNIZATION-015". The rest is
  unchanged.
- **C-1 (REQ-016 AC-6), proposed text:** "- [ ] AC-6: `./gradlew :app:jacocoTestCoverageVerification`
  passes, and no per-package floor is lowered. Every package meets the floor configured for it in the
  `violationRules` block of `app/build.gradle` (`receiver` at or above its 50% floor). No `minimum` in that
  block is lowered by this work, as `git diff` of `app/build.gradle` from the story's start commit shows.
  The configured floors differ by package; they are not a uniform 70%."
- **D-5 (REQ-017 Notes):** in the proposed text, replace "the fix lands before the raise, as AC-5
  requires" with "the fix lands before the raise (decision Record 13 row 4; AC-5 itself allows no later
  than the same PR)". The rest is unchanged. Add "AC-15 states the count rule" to the same paragraph, as
  the record already says.
- **D-10 (epic Amendments):** after "a Sequencing bullet for REQ-MODERNIZATION-020", add ", and the
  Description's list of deliberate behaviour changes".
- **D-11 (new; SC-5; epic Description, lines 29-30):** replace "(other than the removed donation flow and
  the dropped Android 5.0 to 6.0 support)" with "(other than the removed donation flow, the dropped Android
  5.0 to 6.0 support, and the Skip and restore outcomes that REQ-MODERNIZATION-020 AC-14 and AC-16 change
  on purpose)". Success criterion 9 is unchanged.

With these, the pass has 25 numbered edits in 8 files.

### The note for DES-MODERNIZATION-014 (directly under its H1; no other line changes)

"> Note added 2026-11-21 (references only; no decision of this design changes). The change records this
design calls P-1, P-2 and P-3 exist. P-1 is CR-015 (CNTR-MODERNIZATION-004 and DES-MODERNIZATION-005: the
pre-API-24 content-trigger fallback). P-2 and P-3 are one record, CR-016 (requirements wording), as this
design allows when the record is raised before S1. Throughout, read "change record P-1" as CR-015 and
"change record P-2" or "P-3" as CR-016. Once a record is applied (status `implemented`), every condition
this design places on it is met. That includes "Conditional on P-3" in the traceability table, "until it
rules" in D8 item 5, and the remediations of FD-VAL-002 and FD-VAL-003. After CR-016 is applied,
REQ-MODERNIZATION-015 AC-13, REQ-MODERNIZATION-016 AC-6 and REQ-MODERNIZATION-017 AC-15 state the practice
this design already follows: the count at the start of each story, and the configured floors."

---

## Cascade Analysis

**Root Layer:** Requirements
**Root Defect:** Approved wording states fixed figures (a test count, a coverage percentage, API levels) that the
work has since overtaken or that were never true, and two requirements (REQ-001, REQ-006) carry literal levels
with no change record; the new REQ-020 is not cross-referenced.
**Cascade Direction:** Mid-stream down (text only). Nothing is built from the old wording; the design already
works around it (it records counts per story and says no floor changes).

### Propagation Path

| Step | From Layer | To Layer | Propagated? | Evidence |
|------|-----------|----------|-------------|----------|
| 1 | Requirements (REQ-015, -016, -017, epic) | Requirements (REQ-001, -006, -019, -020) | Yes, wording | 8 files in Impact matrix row 2 |
| 2 | Requirements | Design | References only | DES-MODERNIZATION-014 cites "change record P-2/P-3"; after this record they name CR-016. One insert-only note (X-1) maps the labels; the design's 46 lines stay as written |
| 3 | Requirements | Stories | No | No story S1 to S5; U-066 states its AC-12 relative to its start count (`U-066-prune-weak-tls-cipher-suites.md:339`) |
| 4 | Requirements | Build configuration, code, tests | No | The floors and levels are the evidence; nothing in them changes |

### Lines in the design that X-1 covers (not edited one by one)

Where the design says "change record P-2" or "P-3" (or "FD-VAL-002/003 remediation"), the record is now CR-016;
X-1 says so once. Lines found by search on 2026-11-21: 84-89 (Overview item 5), 137-142 (test-count paragraph), 188, 468-477 (order
diagram), 489-493 (preconditions table: rows P-2 and P-3; P-1 is the separate CR-015), 522 (S1 preconditions),
536 (S1-I7, "The AC-9 evidence is change record P-2"), 604 (S4 preconditions), 1047-1051 (D8 item 5), 1868
(015 AC-9 row), 1872 (015 AC-13 row), 1883 (016 AC-6 row, "Conditional on P-3"), 1930-1931 (counts of
conditional criteria), 1936, 1954-1964 (FD-VAL-002 and FD-VAL-003 remediation), 2008-2009, 2095-2097, 2140, 2181 and
2185. The design's statement "This design does not reinterpret the approved '697' wording" (lines 140-141) stays
true; once CR-016 is applied the wording is the rule in A-1 and the design's practice (count at start of story)
is the approved rule. The traceability row for 016 AC-6 stops being "Conditional on P-3" once Part C is applied;
X-1 says so.

### Fix Order

1. [ ] Requirements: Parts A to D in one pass, REQ-015 first (it states the rule others point to), then REQ-016,
   REQ-017, REQ-019, REQ-001, REQ-006, REQ-020, epic: ROOT FIX
2. [ ] Requirements Reviewer: check the criteria whose verification changes
3. [ ] Design: insert note X-1 (and the two `change_records` entries) in DES-MODERNIZATION-014: CASCADE
4. [ ] Stories: S1 to S5 and the BUG-021 fix story are authored from the amended text: CASCADE
5. [ ] Verification: the greps in the Fix Checklist

### Affected In-Progress Work

| Story | Sprint | Phase | Action |
|-------|--------|-------|--------|
| U-066 (prune weak TLS cipher suites) | sprint-022 | In progress, uncommitted changes under `app/src/` and `k9mail-vendored/` | Continue. Its AC-12 is already relative to its start count; its count will raise the figure the next story records |
| BUG-021 (delayed OTP SMS skipped by the watermark) | unscheduled | Raised; fix story not started | Continue; its fix story implements REQ-020, now cross-referenced in the epic and REQ-017 |
| S1 to S5 (design DES-MODERNIZATION-014) | not yet in a sprint | Not authored | Hold until Parts A and B (S1), Part C (S4) are applied; this record applies all of them first |

Stories that will be authored from the amended text: S1 (REQ-015 AC-5 to AC-10, AC-13, AC-14: count at its start,
AC-9 evidence is CR-016), S2 (REQ-015 AC-13 again), S3 (REQ-017 AC-2 compileSdk half, AC-15), S4 (REQ-016 AC-6:
configured floors), S5 (REQ-017 targetSdk step, sequenced after the REQ-020 fix unless "measured not affected"),
the BUG-021 fix story (REQ-020).

---

## Plan

- **Execution Window:** Inline. Apply in one pass right after this record is approved and before story S1 is
  authored.
- **Assigned To:** Requirements Analyst (apply), Requirements Reviewer (check)
- **Estimated Effort:** 1 to 2 hours (26 numbered edits in 9 files, plus notes and review)
- **Sprint:** None

### Timing

Apply Parts A to D, with A-5, A-6 and D-11, and X-1, **in one pass, right after approval, before story S1 is
authored** (this meets Record 14 row 9: P-2 before the minSdk story is planned, P-3 before the runtime-library
story). Reasons:

- Parts A and B are preconditions of S1 (design P-2): S1's acceptance criteria are written from AC-13 and AC-9.
- Part C is a precondition of S4 only; the design allows it to follow S1. **It may wait, but it should not.**
  The same files (REQ-016, the epic) are edited by Parts A and D. Applying C later would open the same files a
  second time, add a second dated note, and need a second review, for no benefit: the C text depends only on the
  configured rules, which no story before S4 changes (design D8 item 1: no `minimum` changes).
- Part D has no precondition in the design but changes the same epic Sequencing and REQ-017 Notes that other parts
  touch, and the stale REQ-017 note is wrong today.
- Part E is not applied, so nothing is deferred.

Apply in this order so that each file is reviewed once: REQ-015 (A-1, B-6, notes), REQ-016 (A-4, C-1, notes),
REQ-017 (A-2, B-5, D-3 to D-5), REQ-019 (A-5, A-6), REQ-001 (B-1, B-2), REQ-006 (B-3, B-4), REQ-020 (D-6 to D-9),
epic (A-3, C-2, D-1, D-2, D-10, D-11), then the design note X-1. The pass is the same one that CR-010 and CR-011
used.

### Fix Checklist

- [x] Requirements Analyst: apply A-1 to A-6, B-1 to B-6, C-1 and C-2, D-1 to D-11 (25 edits in 8 requirement
  files); add `CR-016` to `change_records` on the eight files; add the dated notes; do not touch what the "must NOT
  be touched" list names
- [x] Requirements Analyst: insert X-1 under the H1 of DES-MODERNIZATION-014 and add `CR-015` and `CR-016` to its
  `change_records` (26th edit, 9th file)
- Part E: not applied (Record 13 row 6); no checklist item
- [x] Requirements Reviewer: check the criteria whose verification changes: REQ-015 AC-13, REQ-016 AC-6,
  REQ-017 AC-15, REQ-019 AC-B9, epic criteria 7 and 8; check that AC-9 reads as verified by the three checks in D-5
  (each note says "superseded"); check that a
  proposed sentence does not invent a route or an exception that no decision supports (D-2 in particular); check that
  the REQ-001 and REQ-006 notes do not make a closed requirement read as if it required something else; check
  D-1 and D-5 against design D14's outcome table
- [x] Librarian: `_index.md` needs no change (no title changes); confirm that the eight requirement files keep
  `status: approved`, that the design keeps its status, and that no file name changed
- [x] Verification (0 remaining incorrect values):
  - Part A: `grep -n "697" ` over REQ-015, -016, -017, -019 and the epic returns only the epic Description (line 19,
    history) and dated notes; `grep -n "sprint-017 baseline"` over the same files returns only the epic criterion 7
    ("baseline (2 skipped)") and notes; `grep -n "at least 697\|Baseline: 697"` over REQ-019 returns nothing
  - Part B: `grep -n "CR-016"` over REQ-001, REQ-006, REQ-015, REQ-017, REQ-019 shows the frontmatter entries and
    the notes; `grep -n "superseded"` over REQ-001 and REQ-006 finds the note (and no `status: superseded`); `git diff` of REQ-001 and REQ-006 shows only the frontmatter list and the block quote (no criterion
    line, no `status` line); `grep -n "should carry a change record"` over REQ-017 returns nothing
  - Part C: `grep -n "at or above 70\|others at or above"` over REQ-016 and the epic returns nothing
  - Part D: `grep -n "REQ-MODERNIZATION-020"` shows the epic (Related Requirements, Sequencing, Amendments),
    REQ-017 (frontmatter and Notes) and REQ-020; `grep -n "exist.* on disk yet\|exists on disk yet"` over REQ-017
    returns nothing; `grep -n "other than the removed donation flow"` over the epic shows the D-11 wording
  - Design: `grep -n "CR-016" ` over DES-MODERNIZATION-014 shows the note and the `change_records` entry; `git diff`
    of the design shows insertions only
  - All: `git diff --stat -- sdlc/artifacts/requirements` lists only the eight files; `grep -c "^status: approved"`
    unchanged; `git status` shows no rename

### Next steps (High severity)

CR filed. This is a **text amendment applied inline** by the Requirements Analyst and checked by the Requirements
Reviewer (25 edits in 8 requirement files and one insert-only design note); it is **not a story** and needs no
`/amp:create-stories` run of its own. This should go into the current
sprint's close or the next planning step as a **precondition of the first story of design DES-MODERNIZATION-014**
(S1). After it is applied, the stories S1 to S5 and the BUG-021 fix are authored from the amended text (run
`/amp:create-stories` for them, not `--from-cr CR-016`).

### Scope Concerns

- **SC-1. RESOLVED: REQ-MODERNIZATION-019 AC-B9 and its Context baseline stated the same fixed number.** Lines 60
  and 112 said 697 ("at least 697 minus tests deleted"). The orchestrator adopted the Solution Architect's ruling 2:
  amended now, as A-5 and A-6, with `CR-016` in REQ-019's `change_records` and a dated note.
- **SC-2. 13 of 18 floors are below 0.70, and no decision says whether to raise them.** The build file carries
  TODO comments ("raise to 70%", "raise to 50%+") on all 13; no requirement, epic or decision commissions the uplift;
  design D8 says this work neither lowers nor raises a floor. Part C therefore adds no obligation. If the owner wants
  the uplift, it is a new requirement with its own stories, not a rewording. Not decided here.
- **SC-3. Whether REQ-001 or REQ-006 should carry a "superseded" or "implemented" marker.** The state machine
  allows only `superseded` (terminal, whole requirement) and has no "implemented" state. Neither requirement is
  wholly superseded, so both stay `approved` with a dated note (D-4). If the orchestrator wants a marker, the only
  honest one is a status the machine does not offer; that would be a process change for the Engine Librarian,
  not a wording change.
- **SC-4. Not recorded (ruling 7).** Whether REQ-001 criteria 28 to 30 (signed AAB, Play Console submission) were
  met is not settled here. The only trace is the B-2 sentence "It does not say whether criteria 28 to 30 were met".
  Raise it only if a story cites them.
- **SC-5. RESOLVED: the epic's "behaviour preserved" clause.** Added as edit D-11 (ruling 6). Criterion 9 compares
  IMAP round-trip content across the mail migration, which REQ-020 does not touch, so it needs no edit.
- **SC-6. Other approved requirements with numbers the work has overtaken.** REQ-MODERNIZATION-014 AC-10 (lines
  195-197) requires ":app" instruction coverage "at or above 70%"; that is a different measure from the gate and was
  not measured here (not verified); REQ-014 is implemented. REQ-018 lines 128 and 169 state 682 (implemented; the
  text says to re-baseline). REQ-019 line 72 and the REQ-017 title ("current stable Android API level" while the
  level is 37) were left by CR-011 on purpose. None blocks a story.
- **SC-7. RESOLVED: "Authoritative test count" relies on `@Test` matching.** The same command is used at both
  ends and the Gradle total is checked against the end count (A-1, A-2, A-5), so a comment line containing `@Test`
  cancels out if it was already there, and one that a story adds breaks the check and shows.

### Not verified

- **The wording that REQ-020's analyst proposed** for the epic and for REQ-017 is not on disk (only the Notes
  sentence at REQ-020 lines 459-461 refers to it). Part D is drafted from REQ-020's Notes and Dependencies and
  design D14; if the analyst's own wording differs, the orchestrator should compare before approval.
- **The Gradle run count for 732.** The count is `git grep` at HEAD; no Gradle run was made (the brief forbids it).
  That the run count equals the `@Test` count rests on sprint-017 (`tests=697 ... skipped=2`) and sprint-015 (697 by
  both), and the REQ-018 cross-check wording.
- **Where "others at or above 70%" came from.** Inferred from REQ-003 (70% on three packages) and the TODO comments
  of U-051; the sprint-013 U-051 story was not read in full. (The architect re-read the sources and states that the
  0.70 floors came from U-006 and REQ-003 and that U-051 left them unchanged; C-1 no longer tells that history.)
- **Record 5 row 8** ("all my devices are 11 or newer", used in the not-applied row E-2) is quoted from CR-011
  Execution, not from the appendix itself.
- **That no test class uses a parameterized or theory runner**, and that all 732 matches are real annotation
  lines, is the architect's check at HEAD (Suggested Approach, ruling 1); not re-done here.
- **The uncommitted working tree.** Other agents have changes under `app/src/` and `k9mail-vendored/`; the count was
  taken at HEAD `9e983665`, and `app/build.gradle` shows no modification in `git status`, so the rule listing is
  the same in the working tree.
- **The plan figure 732 to 746 for U-066** is from its plan, not a measurement.

---

## Execution

(To be filled by the applier, as in CR-010 and CR-011: which edits were applied, where the text departed from this
record and why, the grep output, the Reviewer's findings, and the commit.)

### Changes Made

Applied 2026-11-21 in one pass, before story S1 is authored. All files are under `sdlc/artifacts/requirements/modernization/` unless a path is given.

| File | Section or criterion | Edit id | What changed | Layer |
|------|----------------------|---------|--------------|-------|
| REQ-MODERNIZATION-015 | AC-13; frontmatter `notes`; Notes | A-1, A-7, B-6 | The test-count rule (start count with its commit, same command at both ends, working-tree form before a commit); the AC-9 note | Requirements |
| REQ-MODERNIZATION-016 | Context; AC-6 | A-4, A-10, C-1 | The dated count; coverage against the configured floors, from the commit recorded at the story's start | Requirements |
| REQ-MODERNIZATION-017 | AC-15; Notes; frontmatter | A-2, A-8, B-5, D-3, D-4, D-5, D-12 | The count rule by reference; sequencing with REQ-MODERNIZATION-020 | Requirements |
| REQ-MODERNIZATION-019 | AC-B9; Context; note | A-5, A-6, A-9 | The fixed 697 replaced by the count rule | Requirements |
| REQ-MODERNIZATION-001, REQ-MODERNIZATION-006 | `change_records`; note under the H1 | B-1 to B-4 | "Superseded" notes for the literal minSdk 21 and level 35; no criterion text changed | Requirements |
| EPIC-MODERNIZATION-006 | Success criteria 7 and 8; Description; Sequencing; Related Requirements; Amendments | A-3, C-2, D-1, D-2, D-10, D-11 | The count rule, the configured floors, REQ-MODERNIZATION-020 | Requirements |
| REQ-MODERNIZATION-020 | `change_records`; Context; Notes | D-6 to D-9, D-13 | Cross-references to REQ-MODERNIZATION-017 | Requirements |
| `sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-toolchain-runtime-libraries-platform-level-design.md` | Note under the H1; `change_records` | X-1 | P-1 is CR-015; P-2 and P-3 are CR-016 | Design and contracts |

Part E was not applied (Record 13 row 6). Applied by a Requirements Analyst (requirement files) and a Solution Architect (X-1).

### Departures from the proposed text

See the "Departures" subsection of Amendment 1 (two extra sentences in the dated notes of REQ-MODERNIZATION-015 and REQ-MODERNIZATION-016; two phrases of A-7 changed from the reviewer's suggested text, with the reasons). `EPIC-MODERNIZATION-006` is not added to this record's `requirements` list, as in CR-010 and CR-011; the reviewer accepted that.

### Verification Evidence

The applier ran the Fix Checklist searches for Parts A to D (results in its report to the orchestrator: every remaining hit is intended history or a dated note; no `status` line changed). The authoritative count command and its working-tree form both print 746 at commit `1f8ebe3b`. Independent check: `sdlc/artifacts/build/change-records/evidence/CR-016-applied-text-check.md`.

### Reviewer corrections

Requirements Reviewer, 2026-11-21: first verdict FAIL (F-1 must-fix: the count rule did not say where the end count is taken or which commit is the start; F-2 and F-3 should-fix). Fixed by Amendment 1 (A-7 to A-10, D-12, D-13). "Verdict after Amendment 1: PASS". Open note O-2 of the delta check (a possible later sentence about other stories' tests in the equation) needs no action now.

### Commit Traceability

| Commit | Date | Branch | Scope | Author |
|--------|------|--------|-------|--------|
| The commit with the subject "CR-016 applied: requirement wording brought up to date before the DES-014 stories" (find it with `git log --grep`) | 2026-11-21 | sdlc/modernization-plan | The eight requirement files (REQ-MODERNIZATION-020 is new in that commit), the design DES-MODERNIZATION-014 (new in that commit, with X-1), `_index.md`, this record and the check report | Orchestrator (text by the Requirements Analyst and the Solution Architect, checked by the Requirements Reviewer) |

---

## Closure

- **Root Cause Category:** Measured quantities written into acceptance criteria as fixed figures, overtaken by later work
- **Prevention Recommendation:** Do not write a measured quantity (a test count, a coverage percentage, an API
  level) into an acceptance criterion as a fixed figure. State the rule that produces it ("the count at the start of
  the story plus added minus deleted"; "the floor configured in the build file"), and put the dated figure in
  Context where it is allowed to age. When a requirement is implemented and closed, its literal values are history:
  amend by a dated note, and point to the requirement that now governs the value.
- **Closed By:** Orchestrator, after the Requirements Reviewer's delta check (PASS)
- **Date Closed:** 2026-11-21
