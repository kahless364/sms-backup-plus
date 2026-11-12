---
status: implemented
severity: low
artifact_type: change-request
requirements:
  - REQ-MODERNIZATION-010
  - REQ-MODERNIZATION-018
design_docs:
  - DES-MODERNIZATION-010
  - DES-MODERNIZATION-013
related_stories:
  - U-061
  - U-028
tags: []
id: CR-004
title: Supersede REQ-MODERNIZATION-010 (Play Billing 7.x upgrade) and DES-MODERNIZATION-010 with REQ-MODERNIZATION-018 and DES-MODERNIZATION-013 (donation flow removed)
type: RCR
domain: change-records
summary: 'REQ-MODERNIZATION-010 (Upgrade Play Billing to 7.x) is superseded by REQ-MODERNIZATION-018 (remove the donation flow and the Play Billing dependency); DES-MODERNIZATION-010 is superseded by DES-MODERNIZATION-013. Product-owner decision 2026-11-12: remove the donation flow.'
updated_at: '2026-11-12T21:21:19.153Z'
---

# CR-004: REQ-MODERNIZATION-010 and DES-MODERNIZATION-010 superseded by the donation-flow removal

## Metadata

| Field | Value |
|-------|-------|
| **CR Number** | CR-004 |
| **Type** | RCR (Requirement Change) |
| **Severity** | Low |
| **Status** | Implemented (2026-11-12) |
| **Reported By** | Michael Horsley |
| **Date Reported** | 2026-11-12 |
| **Source** | Sprint work: sprint-018 story U-061 (AC-10, IC-14) and DES-MODERNIZATION-013 decision D8 |

---

## Finding

### Description

REQ-MODERNIZATION-010 ("Upgrade Play Billing to 7.x (ProductDetails API)", `approved`) and
DES-MODERNIZATION-010 (`approved`) describe keeping the in-app donation flow and upgrading its billing
library. The product owner decided on 2026-11-12 to remove the donation flow instead. That decision is
recorded as REQ-MODERNIZATION-018 and DES-MODERNIZATION-013 and implemented by U-061. Both older
artifacts are still `approved` with `change_records: []` and nothing in them says they are superseded.
A reader of REQ-010 or DES-010 today would conclude that a billing integration is required.

This CR is the supersession record that REQ-018 AC-10, U-061 AC-10 / IC-14 and DES-013 D8 require.

### Source of Truth

- **Product owner decision, 2026-11-12 (verbatim):** "remove the donation flow". On scope: "I don't own
  the old app store listing. this is just going to be a private app for me".
- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-018-remove-donation-flow-and-play-billing.md`
  (approved, amended by CR-003): AC-10 and the Notes line "Supersedes the intent of REQ-MODERNIZATION-010".
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-013-donation-flow-removal-design.md`
  (approved): decision D8, and the section listing the REQ-010 / DES-010 annotations.
- `sdlc/artifacts/build/sprints/sprint-018/U-061-remove-donation-flow-and-play-billing.md` (AC-10, IC-14).

### Evidence

Verified 2026-11-12 by reading the repo (read-only).

| Claim | Verification |
|-------|--------------|
| The donation flow is removed | `app/src/main/java/com/zegoggles/smssync/activity/donation/` does not exist (`ls` fails). Commit 68d06796 "U-061 I3: delete the donation package, its tests and Consts.Billing" |
| The entry points and strings are removed | 5cb305f8 (I2: settings, MainSettings, `Keys.DONATE`, manifest); 9639f5c7 (I4: donation strings from all 22 `strings.xml` files) |
| The billing dependency and its JaCoCo rule are removed | `grep -i billing app/build.gradle` returns nothing. 8cffa46f (I5: remove the Play Billing dependency and its stale lint-baseline entry); 5b3c5907 (I6: retire the `activity.donation` JaCoCo rule) |
| The commits implementing the removal | `git log --oneline e0c51d42..7dfd651b`: 8866f805 (I0) through 7dfd651b (I9), ten commits. 151763a4 (I7) updated README, CHANGES and `code-location.md`; 463f860f (I8) recorded the re-measured coverage |
| REQ-010 and DES-010 are not yet marked | Both `status: approved`, `change_records: []` |
| REQ-018 already points back | `related_requirements: [REQ-MODERNIZATION-010]`, `related_design_docs: [DES-MODERNIZATION-010, DES-MODERNIZATION-013]`, `change_records: [CR-003]` |
| The earlier billing upgrade was U-028 | `sdlc/artifacts/build/sprints/sprint-001/U-028-upgrade-play-billing-v7.md` (`status: done`); merge commit cbfc4c25 "Merge U-028: upgrade Play Billing 2.1.0 to 7.x". It built DES-010 and satisfied REQ-010 AC-1..AC-4, then U-061 deleted that code |

Not verified here: the 682 passing tests and the review and QA gates of U-061. These belong to the story.

---

## Triage

### Classification

- **Type:** RCR. A stakeholder decision changed scope, and the older requirement and design need a
  supersession record. It is not a DEF (the code is already correct) and not a DCR (the design, DES-013,
  is already correct).
- **Root:** Requirements layer. REQ-018 supersedes REQ-010 but the reciprocal record on REQ-010 was
  deliberately deferred to this CR (U-061 IC-14).
- **Layers Affected:** 2 need edits (requirements, design). Stories, code and tests need none. Engagement
  docs (a docs-layer item) are flagged as a follow-up. `change_records.layers` is not configured in
  `sdlc/config.yaml`, so the layers used are requirements / design / stories / code / tests / docs.
- **Severity Justification:** Low. Only metadata and a dated note on two approved artifacts change. The
  code change is complete under U-061 and no work is blocked. The risk if left is a misleading
  "approved" requirement for a feature that no longer exists, plus failure of U-061 AC-10.

---

## Impact Analysis

Greps run 2026-11-12 over `sdlc/`, `app/` and the repo root for `REQ-MODERNIZATION-010`,
`DES-MODERNIZATION-010`, `MU-009`, `U-028` and "Play Billing".

| # | Layer | Affected? | Files | Action Needed |
|---|-------|-----------|-------|---------------|
| 1 | Ground truth: product owner decision and REQ-018 / DES-013 | n/a | REQ-018, DES-013 | Ground truth |
| 2 | Requirements | Yes | `requirements/modernization/REQ-MODERNIZATION-010-...md` (target); `REQ-MODERNIZATION-018-...md` (reciprocal); `EPIC-MODERNIZATION-004-...md` (lists REQ-010 in frontmatter and body, lines 38, 50, 57); `EPIC-MODERNIZATION-001-...md` (lines 25, 60, 119: phase sequencing mention of MU-009); `EPIC-MODERNIZATION-005-...md` (line 68: mention); `EPIC-MODERNIZATION-006-...md` (line 57 already says "supersedes REQ-MODERNIZATION-010"); `REQ-MODERNIZATION-001-...md` (mentions billing only as Play-eligibility context); `_index.md` (no REQ-010 row; REQ-018 row present) | REQ-010 and REQ-018 edits (below). Epics: no edit, see 3d |
| 3 | Design | Yes | `design/modernization/DES-MODERNIZATION-010-...md` (target; 6 mentions of REQ-010); `DES-MODERNIZATION-013-...md` (already names both, lines 12, 349-367, 506-507) | DES-010 edit (below) |
| 4 | Stories | No edit | `sprints/sprint-001/U-028-upgrade-play-billing-v7.md` and `U-028/implementation-log.md` (built REQ-010 / DES-010, `done`, historical record); `sprints/sprint-001/{sprint-plan.md,execution-plan.json,_smoke/wave5-smoke-results.md}`; `sprints/sprint-017/U-060/implementation-log.md`; `sprints/sprint-018/U-061-...md`, `U-061/plan.md`, `sprint-018/sprint-plan.md` (all already cite the supersession) | None. Completed stories are history |
| 5 | Code | No | `app/` and repo root: zero hits for REQ-010 / DES-010. `app/build.gradle` has no billing | None |
| 6 | Tests | No | Donation tests deleted in 68d06796 | None |
| 7 | Docs, engagement | Yes (stale) | `engagement/endpoints.md:62` ("In-app donations: Google Play Billing (`com.android.billingclient:billing`)"); `engagement/systems.md:55` ("Billing: Google Play Billing (in-app donations)"). `code-location.md` was already updated in 151763a4 | Follow-up, not resolved here |
| 8 | Docs, retros | Mention only | `sprints/sprint-002/retro.md:48` lists Billing among deferred live verification; historical | None |
| 9 | Analysis (historical) | Mention only | 17 files under `sdlc/analysis/20260529-modernization/` (migration-units, roadmap, candidates, approach, modernization-plan, SCR-DON-001 and others) | None. They are frozen assessment output that the requirements were derived from |

Artifacts that reference REQ-MODERNIZATION-010 or DES-MODERNIZATION-010 by id: REQ-010, REQ-018,
DES-010, DES-013, EPIC-MODERNIZATION-004, EPIC-MODERNIZATION-006, CR-003, CR-004, U-028, U-061
(story, `plan.md`, sprint-plan), `U-028/implementation-log.md`, and the analysis files for MU-009.

### Cascade

- **Root Layer:** Requirements. **Cascade direction:** mid-stream down, already complete below the
  design. Design (DES-013), stories (U-061) and code are done. Only the two superseded artifacts lack
  the record.
- **Affected in-progress work:** U-061 (sprint-018) is blocked from closing AC-10 until the amendments
  are applied. No rework.

---

## Suggested Approach

No code story is needed. The code change is U-061. Resolution is four artifact amendments, applied
after this CR is approved. The orchestrator performs the status transitions through the Artifact
Librarian; the text edits below are mechanical.

**a. REQ-MODERNIZATION-010** (`sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-010-upgrade-play-billing-to-v7.md`)

- Frontmatter: `change_records: []` becomes
  ```yaml
  change_records:
    - CR-004
  ```
- Frontmatter: `status: approved` becomes `status: superseded` (allowed by the requirement state
  machine; via the Librarian).
- Append to the `notes:` block (and leave the body unedited):
  ```
  2026-11-12 (CR-004): Superseded. On 2026-11-12 the product owner decided to remove the donation flow ("remove the donation flow"). The intent of this requirement is superseded by REQ-MODERNIZATION-018, and the donation flow and the Play Billing dependency are removed by U-061. Acceptance criteria AC-1..AC-4 are not applicable. The body text below is kept unedited as the historical record of U-028.
  ```

**b. REQ-MODERNIZATION-018** (`.../REQ-MODERNIZATION-018-remove-donation-flow-and-play-billing.md`)

- Frontmatter: `change_records` becomes
  ```yaml
  change_records:
    - CR-003
    - CR-004
  ```
- No other change. `related_requirements` and `related_design_docs` already name REQ-010 and DES-010.

**c. DES-MODERNIZATION-010** (`sdlc/artifacts/design/modernization/DES-MODERNIZATION-010-play-billing-v7-design.md`)

- Frontmatter: `change_records: []` becomes
  ```yaml
  change_records:
    - CR-004
  ```
- Frontmatter: `status: approved` becomes `status: superseded` (via the Librarian).
- Insert directly under the `# DES-MODERNIZATION-010: ...` heading, before `## Overview`:
  ```
  > **SUPERSEDED (2026-11-12, CR-004):** This design is superseded by DES-MODERNIZATION-013 (donation flow removal), which implements REQ-MODERNIZATION-018. The donation subsystem and the `com.android.billingclient:billing` dependency it describes were removed by U-061. The text below is retained unedited as the record of the U-028 upgrade.
  ```

**d. Indexes and epics**

- `_index.md`: none. It has no REQ-010 row and the REQ-018 row is already correct.
- EPIC-MODERNIZATION-004 (lists REQ-010 and "donation purchase flow verified"): no edit proposed here.
  It is a record of the elective M4 plan, and EPIC-006 line 57 already states the supersession. If the
  product owner wants the epic marked too, that is a separate amendment (flagged, not decided here).
- EPIC-001, EPIC-005: none (sequencing mentions only).

**Documentation follow-up (flagged, not resolved here):** `sdlc/artifacts/engagement/endpoints.md:62`
and `sdlc/artifacts/engagement/systems.md:55` still describe Google Play Billing for in-app donations.
They are now wrong. A documentation task (Technical Writer or the next sprint-018 housekeeping step)
should remove those two lines.

### Architect's notes (advisory)

Advisory only — refined when the amendments are applied.

- **Transition safety.** The plugin state machine allows `approved -> superseded` for both
  `requirement` and `design-document`. `superseded` is terminal for both (`superseded: []`), so the
  transition cannot be undone. The only checks that require a referenced artifact to be `approved` are
  user-story checks in the Librarian. `review()` raises `cross_reference_not_approved` for a story's
  `requirements`, `design_docs` and `integration_contracts` (it also runs in the closing bookend), and
  `appendRequirements()` applies the same check. U-061 lists only REQ-018 and DES-013, so its closing
  review is unaffected. U-028 lists REQ-010 and DES-010, but it is `done` and nothing re-reviews it
  unless it is reopened. In that case the finding would be correct. The requirement-type and
  design-type reviews do not check the status of `related_requirements` or `related_design_docs`, so
  REQ-018 and DES-013 stay valid. `related_requirements_approved` runs only when a design is created,
  and DES-013 cites REQ-018 only. Engine cascade hooks fire only on `-> approved`.
- **Minimum versus proposed.** U-061 AC-10 / IC-14 require only the `change_records` entries and the
  DES-010 annotation. D8 adds only "its status follows the artifact state machine", which allows the
  transition but does not require it. Recommendation: do the transitions. The Finding names a
  misleading `approved` status as the defect, the validators above make it safe, and
  `appendRequirements()` will then stop new stories from tracing to REQ-010. This is the first
  `superseded` artifact in the workspace, so record the transition in the Librarian audit trail and
  do not hand-edit it.
- **Schema fit.** `notes` (string) and `change_records` (array) are declared for requirements, and
  `change_records` is declared for design-document. REQ-010 `notes` is a `|` block scalar, so
  appending a line keeps it a string. The DES-010 blockquote is body text, which the schema does not
  govern. It also satisfies the AC-10 (c) grep for `DES-MODERNIZATION-013`. The plugin `Write|Edit`
  hooks guard paths only, and none blocks edits to `approved` artifacts. CR-001 to CR-003 set the
  precedent for in-place edits under a change record. Side note: DES-010 has `title: ''`. That passes
  review because only null or missing fields fail, but the title could be backfilled in the same edit.
- **EPIC-MODERNIZATION-004.** Agree with leaving it unedited. EPIC-006 already records the
  supersession, and the epic is a historical plan record.

---

## Plan

- **Execution Window:** Inline, within sprint-018 (U-061 cannot close AC-10 until applied).
- **Assigned To:** Requirements Analyst / Artifact Librarian, dispatched by the orchestrator.
- **Estimated Effort:** Under one hour (three frontmatter edits, two notes, two status transitions).

### Fix Checklist

- [x] Requirements: REQ-010 `change_records` and dated note
- [x] Requirements: REQ-018 `change_records` gains CR-004
- [x] Design: DES-010 `change_records` and superseded annotation (title backfilled)
- [x] Status approved to superseded for REQ-010 and DES-010 (Artifact Librarian, orchestrator; 2026-11-12)
- [x] Verification (2026-11-12): `grep -l "CR-004"` lists REQ-010, REQ-018 and DES-010; REQ-010 body diff against HEAD shows only frontmatter and `notes:` changes
- [ ] Follow-up raised: engagement docs `endpoints.md` and `systems.md` (recorded in this CR and in U-061 implementation log deviation 7; not yet a story)

---

## Closure

- **Root Cause Category:** Stakeholder scope change (requirement superseded by a later decision)
- **Prevention Recommendation:** When a requirement is created to supersede another, create the
  supersession change record in the same step, not deferred to a story AC.
