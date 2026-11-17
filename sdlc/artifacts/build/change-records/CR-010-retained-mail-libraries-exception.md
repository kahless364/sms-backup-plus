---
status: approved
severity: medium
artifact_type: change-request
requirements:
  - REQ-MODERNIZATION-019
design_docs: []
related_stories:
  - U-062
tags:
  - change-record
  - modernization
  - thunderbird-migration
id: CR-010
title: Retained exception for commons-io, apache-mime4j, jzlib and jutf7 in EPIC-MODERNIZATION-006 criterion 5 and REQ-MODERNIZATION-019 AC-B2 (Thunderbird migration)
type: RCR
domain: change-records
summary: 'PO-10: EPIC-MODERNIZATION-006 success criterion 5 and REQ-MODERNIZATION-019 AC-B2 require commons-io, apache-mime4j, jzlib and jutf7 to leave the runtime classpath; upstream Thunderbird still uses all four, so the criterion can only be met with a recorded retained exception naming each. Found by the accepted feasibility analysis (U-062).'
updated_at: '2026-11-17T18:01:48.658Z'
---

# CR-010: Retained exception for commons-io, apache-mime4j, jzlib and jutf7

## Metadata

| Field | Value |
|-------|-------|
| **CR Number** | CR-010 |
| **Type** | RCR (Requirement Change) |
| **Severity** | Medium |
| **Status** | Open |
| **Reported By** | Requirements Analyst, on the product owner's batch approval (Record 4, 2026-11-17) |
| **Date Reported** | 2026-11-17 |
| **Source** | Sprint work: the accepted feasibility analysis of story U-062 (item PO-10); no design or story for the migration exists yet |

This is one of the seven change records that the product owner approved as a batch on 2026-11-17
(Record 4, last row: "Approve as a batch (Recommended)"). Nothing is blocked today. This record only
describes and proposes text; no requirement, epic, build file or code is edited by it.

---

## Finding

### Description

Two approved texts say the four libraries of the old mail stack must be gone from the resolved
runtime classpath after the Thunderbird migration:

- EPIC-MODERNIZATION-006, success criterion 5, second part.
- REQ-MODERNIZATION-019, Part B first bullet and AC-B2.

Both contain an escape clause, so the text is not wrong as written. The clause reads "unless the
accepted feasibility analysis records a specific retained exception" (epic) and "unless Part A
recorded each as a retained exception" (AC-B2). The accepted analysis shows that the clause will
be needed for every one of the four libraries: Thunderbird's mail modules still use all of them
(Q2.d). Without a named exception, criterion 5 and AC-B2 would fail on the first verification run
of the migration, and the failure would look like a defect of the migration instead of a
property of the chosen upstream code.

The analysis itself closes the gap in two places. PO-10 asks for "a change record revising
criterion 5 and AC-B2 to name them", and condition 11 of Q12.e requires "change record approved
naming commons-io, apache-mime4j, jzlib and jutf7". The product owner approved that change record
on 2026-11-17. This record is that change record.

Counting note: the analysis counts four libraries, but apache-mime4j is two Maven coordinates
(`apache-mime4j-core` and `apache-mime4j-dom`), so the exception names **four libraries and five
coordinates**.

### Source of Truth

- **Accepted feasibility analysis** `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`:
  Section 2 Q2.d (lines 203-212, the table of the four libraries), Section 12 Q12.d row PO-10 (line 1339),
  Q12.e condition 11 (line 1359), and Q8.e (lines 839-914, verification metadata).
  Accepted by the product owner on 2026-11-17 (Record 3).
- **Product owner decision, Record 4, 2026-11-17** (analysis lines 2180): PO-10 is approved together
  with the other forced change records, option "Approve as a batch (Recommended)".
- **The epic's own exception clause** (EPIC-MODERNIZATION-006 lines 92-95), which allows exactly
  this kind of exception and asks that it be "specific".
- Upstream pin used throughout the analysis: `thunderbird-android` commit
  `ac1f0e85f63c1b600ac443d1fb2d939bffff7de5` (citations C-037, C-038, C-233 and others).

### Evidence

All repository lines below were opened on 2026-11-17. Upstream facts are quoted from the analysis
and its citation ids; the upstream repository and the Maven POMs were not re-opened for this
record (see "Not verified").

**1. The approved texts that assert removal**

| File:line | Exact current text |
|-----------|--------------------|
| `EPIC-MODERNIZATION-006-dependency-currency-latest-stable.md:92-97` | "5. **Vendored mail stack retired**: the `:k9mail-vendored` module is absent from `settings.gradle`, and commons-io, apache-mime4j, jzlib, and jutf7 are absent from the resolved runtime classpath unless the accepted feasibility analysis records a specific retained exception; `MailTransport` remains the only mail surface seen by `service.*` (`grep -rn "com\.fsck\.k9\|com\.android\.email" app/src/main/java/com/zegoggles/smssync/service/` returns zero results)." |
| `EPIC-MODERNIZATION-006-...md:58-61` | "- **Mail library replacement** — replace `:k9mail-vendored` and its commons-io / apache-mime4j / jzlib / jutf7 dependencies with Thunderbird for Android's current mail libraries, behind the existing `MailTransport` ACL. A written feasibility analysis must be accepted before any design or implementation story starts." |
| `REQ-MODERNIZATION-019-replace-vendored-k9-with-thunderbird-mail-libraries.md:82` | "- Remove `:k9mail-vendored` from `settings.gradle` and delete `k9mail-vendored/`; absent from the resolved runtime classpath: commons-io, apache-mime4j, jzlib, jutf7 (unless Part A records a specific retained exception)." |
| `REQ-MODERNIZATION-019-...md:103` | "- **AC-B2**: `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` shows none of `commons-io`, `org.apache.james:apache-mime4j-*`, `com.jcraft:jzlib`, `com.beetstra.jutf7` unless Part A recorded each as a retained exception." |

**2. Versions today, versions upstream, and the analysis citations**

Vendored versions are read from `k9mail-vendored/build.gradle` today. Upstream versions and ids
are from analysis Q2.d (lines 207-212).

| Library | Coordinates | Vendored today (file:line, scope) | Upstream at pin | Analysis citation ids |
|---------|-------------|-----------------------------------|-----------------|-----------------------|
| commons-io | `commons-io:commons-io` | 2.16.1 (`k9mail-vendored/build.gradle:107`, `api`) | 2.22.0 (the analysis adds that it is also forced on mime4j, which asks for 2.19.0) | C-084, C-037, C-038 |
| apache-mime4j | `org.apache.james:apache-mime4j-core` | 0.8.11 (`:100`, `api`) | 0.8.15 | C-083, C-087, C-037 |
| apache-mime4j | `org.apache.james:apache-mime4j-dom` | 0.8.11 (`:102`, `api`) | 0.8.15 | C-087, C-037 |
| jzlib | `com.jcraft:jzlib` | 1.0.7 (`:109`, `implementation`) | 1.0.7 | C-085, C-038 |
| jutf7 | `com.beetstra.jutf7:jutf7` | 1.0.0 (`:111`, `implementation`) | 1.0.0 | C-086, C-038 |

What the analysis says about use (Q2.d): commons-io is used "in `:mail:common` and
`:mail:protocols:imap`"; mime4j-core and -dom are used "in `:mail:common`"; jzlib "only by
`:mail:protocols:imap` (compression) and not visible to the app"; jutf7 "only by
`:mail:protocols:imap` (folder-name coding) and not visible to the app". For all four the analysis
verdict is **Retained** (commons-io and mime4j with a changed version, jzlib and jutf7 unchanged).
The analysis ends the table with "Also gone from the closure: nothing."

**3. The app uses two of the libraries directly**

| File:line | Import |
|-----------|--------|
| `app/src/main/java/com/zegoggles/smssync/mail/Attachment.java:13` | `import org.apache.commons.io.IOUtils;` |
| `app/src/main/java/com/zegoggles/smssync/mail/MessageConverter.java:40` | `import org.apache.commons.io.IOUtils;` |
| `app/src/main/java/com/zegoggles/smssync/utils/Sanitizer.java:3` | `import org.apache.james.mime4j.codec.EncoderUtil;` |
| `app/src/test/java/com/zegoggles/smssync/mail/MessageGeneratorTest.java:14` | `import org.apache.james.mime4j.util.MimeUtil;` (test code, not named in the analysis) |

`app/build.gradle:131` declares only `implementation project(':k9mail-vendored')`, so the app
reaches these classes through the vendored module's `api` declarations (`k9mail-vendored/build.gradle:100,102,107`).
The analysis (Q2.d) records that upstream declares commons-io and mime4j as `implementation`,
not `api` [C-037], so after the migration the app has to declare both itself.

**4. `gradle/verification-metadata.xml` today**

| Library | Components now in the file (line) |
|---------|-----------------------------------|
| commons-io | 2.13.0 (3161), 2.16.1 (3169), 2.4 (3177) |
| apache-mime4j-core / -dom / -project | 0.7.2 (3950, 3966, 3982) and 0.8.11 (3958, 3974, 3987) |
| jzlib | 1.0.7 (3007) |
| jutf7 | 1.0.0 (1835) |

---

## Triage

### Classification

- **Type:** RCR. The product owner's decision (Record 4) changes the wording of two approved
  requirement texts so that they name an exception they already allow. It is not a DEF: the epic
  anticipated the exception when it was written, so the text was not wrong. It is not a DCR: no
  design or contract mentions the four libraries (search below), so no design is changing.
- **Root:** Requirements layer (EPIC-MODERNIZATION-006 criterion 5 and REQ-MODERNIZATION-019 Part B).
  Both were written before any evidence existed about what Thunderbird's mail modules depend on, and
  they said "unless the analysis records an exception" without a way to make that record specific.
- **Layers Affected:** 1 layer needs a text edit now (requirements). Build configuration, application
  code and tests are touched later by Part B itself whether or not this record exists; this record
  only fixes what they must end up with.
- **Severity Justification:** Medium. The change is isolated to one theme and two requirement files,
  nothing is built, no story or design exists and no work is blocked. It is not low, because
  Q12.e condition 11 makes an approved record a test condition of Part B, and AC-B2 would fail
  verification if the text stays as written.

---

## Impact Analysis

Searches run on 2026-11-17 (read-only). Pattern: `commons-io|mime4j|jzlib|jutf7` over the layer
paths given by the project's configured layers (no `change_records.layers` in `sdlc/config.yaml`,
so the layers named in the dispatch are used).

### Layer Impact Matrix

| # | Layer | Affected? | Files | Occurrences | Action Needed |
|---|-------|-----------|-------|-------------|---------------|
| 1 | Ground truth: accepted analysis (Q2.d, Q8.e, Q12.d PO-10, Q12.e condition 11) and Record 4 | Ground truth | `thunderbird-mail-libraries-feasibility-analysis.md` | n/a | None (read-only) |
| 2 | Requirements (`sdlc/artifacts/requirements`) | **Yes** | 3 files match: `EPIC-MODERNIZATION-006` (lines 22, 58-59, 93), `REQ-MODERNIZATION-019` (lines 35, 68, 82, 103), `REQ-MODERNIZATION-016` (lines 68, 83). Edit 2 files, 4 passages | 9 lines match; 4 passages edited | Amend EPIC criterion 5 and its In Scope bullet; amend REQ-019 Part B bullet 1 and AC-B2 (below). Others need no edit |
| 3 | Design and contracts (`sdlc/artifacts/design`) | No | 0 | 0 | None. CNTR-MODERNIZATION-001 and -007 and DES-MODERNIZATION-009 do not name the libraries |
| 4 | Stories and sprints (`sdlc/artifacts/build`, `sdlc/artifacts/stories`) | No (history only) | `sdlc/artifacts/stories`: 0 files (it holds only BUG-018). `sdlc/artifacts/build`: 38 files match (not counting this record), all sprint records, evidence files and the analysis (sprint-001 U-027, sprint-015 U-058, sprint-017 U-060, sprint-018 U-061, sprint-019 U-062, `feasibility/`) | not counted individually | None. Historical records are not edited |
| 5 | Build configuration | Not by this record; Part B | `k9mail-vendored/build.gradle` (100, 102, 107, 109, 111; deleted with the module), `app/build.gradle:131`, `settings.gradle:1`, `gradle/verification-metadata.xml` (see below) | 5 + 1 + 1 lines, and 11 components in the metadata file | Part B declares the retained libraries in the app (see Proposed Change). No edit now |
| 6 | Application code (`app/src/main`) | Not by this record; Part B | `Attachment.java:13`, `MessageConverter.java:40`, `Sanitizer.java:3` | 3 | None needed by this record: the imports stay valid because the libraries stay |
| 7 | Tests (`app/src/test`) | Not by this record | `MessageGeneratorTest.java:14` | 1 | None |
| 8 | Documentation | Not by this record | `k9mail-vendored/NOTICE:20-21` (stale version list, goes with the module; REQ-019 AC-B12 replaces it). `README.md`, `.claude/CLAUDE.md`, `sdlc/artifacts/engagement/*.md`: 0 matches | 1 | None |

Layers needing an edit now: 1 of 8. Severity multiplier: 1x (a change record is still the right
vehicle, because the texts are approved and must not be edited silently).

### Files that assert removal (every line)

| # | File:line | Statement | Needs text change? |
|---|-----------|-----------|--------------------|
| 1 | EPIC-006:93-95 | Criterion 5, second part (quoted above) | **Yes** |
| 2 | EPIC-006:58-59 | In Scope bullet: replace the module "and its commons-io / apache-mime4j / jzlib / jutf7 dependencies" | **Yes** (it says the four are replaced) |
| 3 | EPIC-006:22 | Description: "a vendored copy of the K-9 mail library (`:k9mail-vendored` plus commons-io, apache-mime4j, jzlib, jutf7)" | No: describes today's state |
| 4 | REQ-019:82 | Part B first bullet (quoted above) | **Yes** |
| 5 | REQ-019:103 | AC-B2 (quoted above) | **Yes** |
| 6 | REQ-019:68 | Part A question 2: "(replacing commons-io, mime4j, jzlib, jutf7, or not)" | No: it is the question Part A answered. Rewriting it would change the record of what was asked |
| 7 | REQ-019:35 | Context: versions declared today | No: current-state fact, true today |
| 8 | REQ-016:68-69 and :82-83 | "`jutf7` 1.0.0 (the vendored mail stack is retired by a separate requirement)"; AC-3 asks for a maintenance finding on jutf7 | No change needed. The finding stays relevant, and more so, because jutf7 stays on the classpath after Part B |
| 9 | `CNTR-MODERNIZATION-007` Rule 2 and other contract rules | Do not mention the four libraries (search: 0) | No |

### `gradle/verification-metadata.xml` consequences, as the analysis states them

The analysis did not run the Gradle writer; its numbers are counted from fetched metadata files
(Q8.e, lines 841-861). Relevant statements:

- Against the start commit the file had 803 components and 1,460 artifact entries.
- **jzlib 1.0.7 and jutf7 1.0.0 are already present** at the exact version (lines 893-895 and 914).
  The exception needs no new entries for them.
- **commons-io 2.22.0 and apache-mime4j-core and -dom 0.8.15 are new components** (rows at lines 884-886,
  "n"). Their parents `apache-mime4j-project:0.8.15` and `commons-parent:98` are among the 12 parent or
  BOM components counted (line 861). They are included in the totals the analysis gives for Option 2:
  112 entries (mode M1) or 208 to 256 (mode M2, the route chosen in Record 4).
- "Entries for the versions that would no longer be used (mime4j 0.8.11, commons-io 2.16.1, and the like)
  could be removed; they are not counted" (line 861). Today's file also still holds mime4j 0.7.2 and
  commons-io 2.4 and 2.13.0, which are older leftovers (verified above).
- The exception does not relax REQ-MODERNIZATION-019 AC-B10 or REQ-MODERNIZATION-015 AC-11: every new entry
  is still added by Gradle's own writer, and verification stays enforced.

---

## Proposed Change

Nothing below is applied by this record. The wording is for the Requirements Analyst to apply after
the Requirements Reviewer has checked it (see Timing).

### P1. EPIC-MODERNIZATION-006, In Scope, "Mail library replacement" (lines 58-61)

Current text:

> - **Mail library replacement** — replace `:k9mail-vendored` and its commons-io /
>   apache-mime4j / jzlib / jutf7 dependencies with Thunderbird for Android's current mail
>   libraries, behind the existing `MailTransport` ACL. A written feasibility analysis must be
>   accepted before any design or implementation story starts.

Proposed text:

> - **Mail library replacement** — replace `:k9mail-vendored` with Thunderbird for Android's
>   current mail libraries, behind the existing `MailTransport` ACL. The vendored module's own
>   declarations of commons-io, apache-mime4j, jzlib and jutf7 are replaced by the versions that
>   the Thunderbird mail modules declare; the four libraries themselves stay on the classpath as
>   retained exceptions (see success criterion 5, CR-010). A written feasibility analysis must be
>   accepted before any design or implementation story starts.

### P2. EPIC-MODERNIZATION-006, success criterion 5 (lines 92-97)

Proposed text:

> 5. **Vendored mail stack retired**: the `:k9mail-vendored` module is absent from
>    `settings.gradle`, and no library of the old mail stack remains on the resolved runtime
>    classpath except the retained exceptions below; `MailTransport` remains the only mail
>    surface seen by `service.*`
>    (`grep -rn "com\.fsck\.k9\|com\.android\.email" app/src/main/java/com/zegoggles/smssync/service/`
>    returns zero results).
>    Retained exceptions (CR-010, approved 2026-11-17; feasibility analysis Section 2 Q2.d and
>    item PO-10): four libraries, five coordinates, each kept because the Thunderbird mail modules
>    still use it: `commons-io:commons-io`; `org.apache.james:apache-mime4j-core` and
>    `org.apache.james:apache-mime4j-dom`; `com.jcraft:jzlib`; `com.beetstra.jutf7:jutf7`.
>    The versions are those that the pinned Thunderbird code declares (at the analysis pin:
>    commons-io 2.22.0, apache-mime4j 0.8.15, jzlib 1.0.7, jutf7 1.0.0), not the vendored
>    module's (commons-io 2.16.1, apache-mime4j 0.8.11). The app declares commons-io and
>    apache-mime4j-core itself, because it imports `IOUtils` and `EncoderUtil`. The exception
>    covers these five coordinates only.

### P3. REQ-MODERNIZATION-019, Part B first bullet (line 82)

Proposed text:

> - Remove `:k9mail-vendored` from `settings.gradle` and delete `k9mail-vendored/`. The four
>   libraries commons-io, apache-mime4j (core and dom), jzlib and jutf7 are retained exceptions
>   (CR-010; Part A Q2.d recorded each as retained, because the Thunderbird mail modules use them)
>   and stay on the resolved runtime classpath. The app declares commons-io and
>   apache-mime4j-core itself, because it imports `IOUtils` and `EncoderUtil` and the
>   Thunderbird modules do not expose them (`implementation`, not `api`).

### P4. REQ-MODERNIZATION-019, AC-B2 (line 103)

Proposed text:

> - **AC-B2**: `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` shows no
>   `project :k9mail-vendored` entry and, from the old mail stack, exactly these five coordinates
>   and no other: `commons-io:commons-io`, `org.apache.james:apache-mime4j-core`,
>   `org.apache.james:apache-mime4j-dom`, `com.jcraft:jzlib`, `com.beetstra.jutf7:jutf7`
>   (retained exceptions, CR-010). The vendored module's own versions (commons-io 2.16.1,
>   apache-mime4j 0.8.11) do not appear. The listing is saved as evidence in the story.

### P5. Frontmatter and amendment notes (follows the CR-003 precedent)

Add `CR-010` to `change_records` on EPIC-MODERNIZATION-006 and REQ-MODERNIZATION-019, and a dated
one-line note in each body ("Amended 2026-11-17 by CR-010: ..."), so the change to approved text is
visible in the body and not only in frontmatter. `status: approved` stays unchanged.

### What the exception does NOT cover

- **Any other library.** Only the five coordinates above. Any other coordinate that is reachable
  only through the old stack must still be absent (the analysis condition 11 test says the listing
  shows "exactly those four from the old stack and nothing else"). New libraries that Thunderbird
  brings (okio, moshi, httpclient5, koin, kotlinx libraries and others, analysis Q2.d last paragraph)
  are additions, not retained exceptions, and are governed by the licence and size sections of the
  analysis, not by this record.
- **The vendored module itself.** `:k9mail-vendored` still goes: absent from `settings.gradle`, directory
  deleted (REQ-019 AC-B1, epic criterion 5 first part). This record changes neither.
- **The vendored module's versions.** The exception does not keep commons-io 2.16.1 or mime4j 0.8.11.
- **Verification rules.** Dependency verification stays enforced and entries are added by Gradle's
  writer only.

Scope Concern: EPIC-MODERNIZATION-006 success criterion 1 (currency) applies to "every dependency
and plugin named in the in-scope list", and the four libraries are named in the Mail library
replacement bullet. After this record they stay, at the versions the pinned fork declares. The
approved decision (retain them) is followed as given. Whether criterion 1 must then also hold for
them (latest stable at planning time; jzlib 1.0.7 and jutf7 1.0.0 may have no newer release, not
checked) is not decided here, and P1 and P2 do not say. The product owner should settle this before
Part B design. REQ-016 AC-3 already asks for a maintenance finding on jutf7.

Scope Concern: the migration is planned "as its own epic" afterwards (Record 3, "Next"). If criterion 5
and AC-B2 move to a new epic or requirement, the amended wording must be carried over, not the
original.

---

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->

Advisory only — refined during story planning.

**What changes.** A requirement text names an exception it already allowed. No architecture moves. RCR and Medium: agreed. Timing, before Part B design and in the same pass as CR-011: agreed.

**Disagreements with the proposed wording.**
1. P2's "The versions are those that the pinned Thunderbird code declares" cannot be tested as a fixed statement. Gradle resolves to the highest version requested. If the app declares commons-io itself (P2, P3) under EPIC-MODERNIZATION-006 criterion 1 ("latest stable"), the resolved version may be above the fork's. Suggested reading: "at or above the versions the fork declares; the resolved versions are recorded in the AC-B2 evidence". That also settles half of the analyst's currency Scope Concern.
2. P2 and P3 prescribe "the app declares commons-io and apache-mime4j-core itself". That is a build-design choice, and it belongs in the Part B design, not in an epic success criterion. Keep the requirement to the outcome: the app's direct uses compile (`Attachment.java:13`, `MessageConverter.java:40`, `Sanitizer.java:3`).
3. P4's "and no other from the old stack" can be tested only against a list. Before Part B starts, capture today's `releaseRuntimeClasspath` closure under `:k9mail-vendored` as a baseline file. Condition 11 then has an oracle for "nothing else".

**Cross-record.**
- EPIC-MODERNIZATION-006 criterion 5 is also the subject of CR-005's Scope Concern: the `service/` grep at line 96, which P2 keeps unchanged. Decide both in this pass. I recommend that P2's grep become `com\.fsck\.k9\|com\.android\.email\|net\.thunderbird`.
- CR-011 edits other lines of the same epic, with no overlapping lines: this record touches 58-61 and 92-97; CR-011 touches 25, 28, 39, 44-48, 53-54, 82-88, 115-117, 125-129 and 142-147. Apply both in one pass with one review.
- REQ-019 is edited by four records: CR-005 (AC-B3, line 104), this record (lines 82 and 103), CR-011 (optional, line 128) and CR-006 (a cross-reference at line 84). Same advice: one pass, one review, before any design.

**Fork wiring (left open by Record 4).** The verification-metadata figures the record cites (208 to 256 entries, mode M2) assume the fork is built inside the app's build. With published artifacts, both the count and the set of entries differ. The record's proposed wording does not depend on this; only its sizing does.

**Risks to carry into Part B planning.** The five retained coordinates were not checked against security advisories (record, "Not verified"); Part B design should run that check. jutf7 1.0.0 already has a maintenance finding pending under REQ-016 AC-3. The currency Scope Concern (does criterion 1 apply to retained libraries?) is for the product owner.

**Story shape and order.** No story of its own. It is a text amendment in the single requirements pass that comes before the design of REQ-015 and REQ-017 (and therefore before Part B). Its verification, the AC-B2 listing, lands as acceptance evidence in the Part B story that removes the module.

---

## Cascade Analysis

**Root Layer:** Requirements (EPIC-MODERNIZATION-006 criterion 5, REQ-MODERNIZATION-019 Part B)
**Root Defect:** The texts require the four libraries to be absent and rely on a later record to
excuse them. The accepted analysis shows that all four will stay, so the exception must be named
now, or AC-B2 fails on its first run.
**Cascade Direction:** Mid-stream down, but nothing is built below the root. Isolated to the requirements layer today.

### Propagation Path

| Step | From Layer | To Layer | Propagated? | Evidence |
|------|-----------|----------|-------------|----------|
| 1 | Requirements | Design and contracts | No | 0 matches for the four names under `sdlc/artifacts/design` |
| 2 | Requirements | Stories | No | No story exists for REQ-015/016/017/019; `sdlc/artifacts/stories` holds only BUG-018 |
| 3 | Requirements | Build configuration | Not yet | Part B will remove `k9mail-vendored/build.gradle:100,102,107,109,111` and add app-level declarations for commons-io and mime4j-core |
| 4 | Requirements | Application code and tests | No | `Attachment.java:13`, `MessageConverter.java:40`, `Sanitizer.java:3`, `MessageGeneratorTest.java:14` stay valid |

### Fix Order

1. [ ] Requirements: amend EPIC-MODERNIZATION-006 (P1, P2) and REQ-MODERNIZATION-019 (P3, P4); record CR-010 in both `change_records` (P5) — ROOT FIX
2. [ ] Requirements Reviewer pass on criterion 5 and AC-B2, because they change what verification counts as a pass
3. [ ] Design (Part B): cite the exception; design the app-level declarations of commons-io and mime4j-core — CASCADE
4. [ ] Stories (Part B): AC-B2 text carries into the story that removes the module — CASCADE
5. [ ] Verification: after the amendment, `grep -nE "commons-io|mime4j|jzlib|jutf7"` over the epic and REQ-019 shows only the exception wording and current-state facts

### Affected In-Progress Work

| Story | Sprint | Phase | Action |
|-------|--------|-------|--------|
| None. Sprint-019 (U-062) is closed and no Part B story exists | n/a | n/a | n/a |

---

## Plan

- **Execution Window:** Inline: apply before the Part B design starts. Applying it in the same pass
  as CR-011 is recommended (see Timing).
- **Assigned To:** Requirements Analyst (text), Requirements Reviewer (check)
- **Estimated Effort:** Under 1 hour
- **Sprint:** None

### Timing

Apply P1 to P5 **before any Part B design** (the date set in Record 4, "Next"). Reason: no design
touches these texts earlier, so there is no earlier deadline. The edit is cheap, and the same
epic file is edited by CR-011, so doing both in one pass avoids two reviews of one file. No reason
to defer past that point was found. Do not apply it as part of this record; the dispatch forbids
amending requirements here.

### Fix Checklist

- [ ] Requirements: apply P1 to P4 to the epic and REQ-019; add `CR-010` to `change_records`; add the dated note (P5)
- [ ] Requirements Reviewer: check criterion 5 and AC-B2
- [ ] Product owner: settle the currency question in the Scope Concern above
- [ ] Verification: 0 remaining unqualified "absent" statements about the four libraries in the epic and REQ-019

### Next steps (Medium severity)

Run `/amp:create-stories --from-cr CR-010` when ready. This record is a **precondition of Part B design
work**, not a story to schedule on its own: the work is a text amendment, and its verification
(the AC-B2 listing) lands as acceptance criteria in the Part B stories.

### Not verified

- Neither this record nor the analysis ran a build. Upstream's build was not run (analysis
  experiment (c), "not run"), so the claim that the release runtime classpath will list exactly
  these five coordinates is a prediction from build files, not a measurement.
- The upstream facts in the Evidence tables (versions at the pin, which module uses which library,
  `implementation` versus `api`) come from the analysis and its citation ids. The citations were
  not re-opened for this record.
- Whether other coordinates survive from the old stack's closure. The analysis says "Also gone from
  the closure: nothing" and its condition 11 requires the listing to prove "nothing else"; that test
  has not been run.
- Whether the retained versions have known vulnerabilities. The analysis ran no advisory scan.
- Whether the retained libraries are at the latest stable version (the currency question above).

---

## Execution

### Changes Made

| File | Change | Layer |
|------|--------|-------|
| (none yet) | Proposed only | |

### Verification Evidence

Not started.

### Commit Traceability

| Commit | Date | Branch | Scope | Author |
|--------|------|--------|-------|--------|
| *(pending)* | | sdlc/modernization-plan | | |

---

## Closure

- **Root Cause Category:** (pending)
- **Prevention Recommendation:** When a requirement says a dependency will be removed "unless an exception is recorded", record the exception in the same session in which the evidence for it is produced, naming each coordinate, so that the removal criterion never exists in an unmeetable form.
- **Closed By:** (pending)
- **Date Closed:** (pending)
