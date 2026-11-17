---
status: approved
severity: medium
artifact_type: change-request
requirements:
  - REQ-MODERNIZATION-019
design_docs:
  - CNTR-MODERNIZATION-007
related_stories:
  - U-062
tags:
  - change-record
  - cntr-007
  - thunderbird-migration
  - part-b-precondition
id: CR-005
title: CNTR-MODERNIZATION-007 Rule 1 and the mail-type translation row, and REQ-MODERNIZATION-019 AC-B3, name only com.fsck.k9; after the mail migration the types sit in com.fsck.k9 and net.thunderbird (P-1)
type: DCR
domain: change-records
summary: 'P-1: CNTR-MODERNIZATION-007 Rule 1 (the import-boundary grep string and its wording), the translation row for the mail types, and REQ-MODERNIZATION-019 AC-B3 name only the package com.fsck.k9; after the migration the replacement types sit in com.fsck.k9.* and net.thunderbird.*. Found by the accepted feasibility analysis (U-062); contract text only, the port is preserved.'
updated_at: '2026-11-17T18:01:48.618Z'
---
> **CONSOLIDATED:** Change Records are now tracked as Bugs (type: bug, tags: ["change-record"]).
> A CR is a bug — a defect found during cross-layer consistency checking. Use the `bug.md`
> template with `origin: cross-layer-audit` and `tags: ["change-record"]` to preserve provenance.
> This template is retained for reference.

# CR-005: CNTR-MODERNIZATION-007 Rule 1, translation row and REQ-MODERNIZATION-019 AC-B3 must name both package roots after the mail migration (P-1)

## Metadata

| Field | Value |
|-------|-------|
| **CR Number** | CR-005 |
| **Type** | DCR (Design Correction) |
| **Severity** | Medium |
| **Status** | Open |
| **Reported By** | Requirements Analyst agent, on the product owner's batch approval (Record 4, 2026-11-17) |
| **Date Reported** | 2026-11-17 |
| **Source** | Sprint work: story U-062 (Thunderbird mail-libraries feasibility analysis, accepted), item P-1 / PO-3 |

---

## Finding

### Description

The mail boundary rule of the app is written, in every place that states it, with one package name:
`com.fsck.k9`. The contract (CNTR-MODERNIZATION-007) says no `com.fsck.k9.*` type may cross the
`MailTransport` port and checks it with a grep for `com.fsck.k9`. The requirement
(REQ-MODERNIZATION-019 AC-B3) checks the same thing for the whole app with a grep for `com\.fsck\.k9`.

The accepted feasibility analysis found that after the migration to Thunderbird for Android's mail
libraries the replacement types live in **two** package roots, not one:

- `com.fsck.k9.*` keeps, for example, `Message`, `MimeMessage`, `FetchProfile`, `TrustedSocketFactory`
  and `AuthType`;
- `net.thunderbird.*` holds, for example, `net.thunderbird.core.common.exception.MessagingException`
  (the base class of every mail failure; it moved out of `com.fsck.k9.mail`), 
  `net.thunderbird.core.common.mail.Flag` and `net.thunderbird.legacy.logging.Log`.

The observable problem is a boundary check that would **pass while the boundary is broken**. After the
migration, an engine class in `service/` that imported `net.thunderbird.core.common.exception.MessagingException`
would leave the contract's grep (`com.fsck.k9` in `service/` returns 0) and AC-B3's grep both at zero,
although a mail-library type then crosses the port. In the other direction, AC-B3 as written ("lists only
files under `.../mail/transport/`") can be read as satisfied by moving every `net.thunderbird` import
outside the adapter, because the grep never sees them. The port itself is not affected: the analysis
rates every port member "preserved" or "preserved with adapter change". Only the text of the rule, the
grep strings and one translation row are wrong for the post-migration state.

The same contract also says, in its Rule 1 text, that library imports are "confined to
`mail.transport.K9MailTransport`". The code today and REQ-019 both describe a wider zone (the adapter
package). That small inconsistency is recorded under Evidence and resolved in the proposed wording.

### Source of Truth

- The accepted feasibility analysis, `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`:
  Section 5, sub-section "Q5.p Port preservation (CNTR-MODERNIZATION-007)", row "Public constructor
  `K9MailTransport(Context, MailTransportConfig)`" (line 524) and the "P items" table row P-1 (line 547);
  Section 12 "Q12.d", item PO-3 (line 1332); condition 6 in "Q12.e" (line 1354).
- The product owner's decisions, same file, appendix "Product owner decision": Record 3 (2026-11-17,
  `accepted`, "Accept: full migration", line 2151) and Record 4 (2026-11-17, line 2164), row "PO-3, PO-5,
  PO-6, PO-7, PO-10 (and the two above)" (line 2180): the forced change records are approved as a batch and
  raised before any Part B design. Record 4, row PO-11 (route), also fixes that the code is consumed from a
  fork of thunderbird-android built as Android variants.
- The upstream package facts the analysis cites (citations C-052, C-067, C-079, C-299, C-304, all at upstream
  commit `ac1f0e85f63c1b600ac443d1fb2d939bffff7de5`). I did not re-open the upstream repository for this
  record; I read the analysis's statements and its citation table (lines 1492, 1507, 1519, 1739, 1744).

### Evidence

**1. The contract's rule and grep string** (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-007-mail-transport.md`, lines 345-349):

```
1. **The ACL invariant — no `com.fsck.k9.*` crosses `MailTransport`.** No port method
   signature, return type, field of a crossing value type, or thrown exception names a
   `com.fsck.k9.*` type. Enforced by grep: `com.fsck.k9` in `service/` → **0**; in `State.java`
   → **0** (DES-009 AC-2, scoped per ADR-009-B). k-9 transport imports are confined to
   `mail.transport.K9MailTransport`.
```

**2. The translation row** (same file, line 224):

```
| any other `com.fsck.k9.mail.*` throwable (incl. `IllegalArgumentException` re-wrapped at `BackupImapStore.java:129-133`) | adapter internals | `MailException` (backstop) | generic error |
```

**3. The requirement criterion** (`sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-019-replace-vendored-k9-with-thunderbird-mail-libraries.md`, line 104):

```
- **AC-B3**: `grep -rln "com\.fsck\.k9" app/src/main/java` lists only files under `app/src/main/java/com/zegoggles/smssync/mail/transport/` (or, if Part A shows the package name changed, the equivalent new package string appears only there). This also removes the `App.java`, `AuthPreferences.java`, and `MailModule.kt` references.
```

The same requirement already says, at line 83, "No `com.fsck.k9` type (or the Thunderbird library's equivalent) appears outside `com.zegoggles.smssync.mail.transport`". So the intent is written down; only the checkable grep string in AC-B3 and the contract is one-rooted.

**4. Other statements of the same rule in the contract** (grep `com\.fsck\.k9` on the contract, lines 25-28, 45, 68, 76, 312-313):
"It also fixes the **invariant** that no `com.fsck.k9.*` type may cross this boundary" (27); the diagram label
"ZERO com.fsck.k9.* after MU-008" (45); "Own all `com.fsck.k9.mail.*` transport types" (68); "Never import `com.fsck.k9.*`" (76);
"Validation Rule 1 ("no `com.fsck.k9.*` crosses `MailTransport`" / grep `com.fsck.k9` in `service/` → 0)" (312-313).

**5. The analysis' finding** (analysis line 524, public-constructor row, verdict "needs contract change (CR) (contract text only: P-1)"):
"The replacement types live in two package families, `com.fsck.k9.*` (for example `Message`, `MimeMessage`, `FetchProfile`, `TrustedSocketFactory`, `AuthType`) and `net.thunderbird.*` (`net.thunderbird.core.common.exception.MessagingException` [C-304], `net.thunderbird.core.common.mail.Flag`, `net.thunderbird.legacy.logging.Log` [C-079]). Rule 1's grep string `com.fsck.k9` and REQ-019 AC-B3 ... then miss, or cannot be met by, the 8 of the 14 outside files that use `MessagingException` ... and the file that uses `Flag` ... This is a contract-text change record even with the port kept (plan item 9), not a port break."

**6. What the code shows today** (searches run for this record on 2026-11-17):

| Search | Result |
|--------|--------|
| `com\.fsck\.k9` in `app/src/main/java/com/zegoggles/smssync/service/` | 0 matches (the REQ-012 seal holds today) |
| `net\.thunderbird` in `app/` | 0 matches (the second root does not exist in the app yet) |
| `com\.fsck\.k9`, any form, in `app/src/main/java` | 73 occurrences in 23 files: 9 files in `mail/transport/`, 14 outside it |
| `^import com\.fsck\.k9` in `app/src/main/java` | 62 occurrences in 16 files: 3 in `mail/transport/` (`K9MailTransport.java` 15, `BackupFolderHandle.java` 1, `MailMessageHandle.java` 1), 13 outside |
| an automated test or build check that encodes the grep string | none found (see Impact Analysis, tests layer) |

The 9 and 14 agree with `REQ-MODERNIZATION-019:36`. The 3 files that import inside `mail/transport/` show that the
contract sentence "confined to `mail.transport.K9MailTransport`" is already narrower than the code and than REQ-019.

---

## Triage

### Classification

- **Type:** DCR (Design Correction). Reason: the design specification (the contract, and the requirement
  criterion derived from it) does not match the source of truth (the upstream package layout recorded in the
  accepted analysis). It is not a DEF: no implementation departs from the contract; the live code is still the
  vendored library and fully matches the contract as written. It is not an RCR: the product owner changed no
  scope or behaviour by deciding PO-3; the boundary rule is the same rule, it only needs the right names.
- **Root:** Design and contracts layer. CNTR-MODERNIZATION-007 Rule 1 and the translation row were written
  when `com.fsck.k9` was the only package family of the mail library. REQ-019 AC-B3 derives its grep string
  from that rule; it already allows for a renamed package in a parenthesis, which this record makes concrete.
- **Layers Affected:** 2 (design and contracts; requirements). Code, tests and the vendored library are not
  changed by this record.
- **Severity Justification:** Medium. Isolated to one contract and one acceptance criterion (plus related
  statements listed below), no code, and nothing is blocked today because no migration story exists. It is
  not Low because the failure it prevents is silent: a boundary check that passes while the boundary is
  broken, on the one rule that protects the engine from the mail library. It is not High because no story
  or sprint is affected yet.

---

## Impact Analysis

`change_records.layers` is not configured in `sdlc/config.yaml`. The layers below are the ones the
requester named: requirements, design and contracts, stories and sprints, application code, tests, vendored
library, documentation. Every row was found by a search run for this record (Grep over the paths named).

### Layer Impact Matrix

| # | Layer | Affected? | Files and lines | Occurrences | Action needed |
|---|-------|-----------|-----------------|-------------|---------------|
| 1 | Ground truth: accepted analysis, Record 3 and Record 4, upstream at the pin | — | `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` lines 524, 547, 1332, 1354, 2151-2180 | — | Ground truth (read-only) |
| 2 | Design and contracts | YES (root) | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-007-mail-transport.md`: Rule 1 lines 345-349 (required); translation row line 224 (required); consequential wording lines 25-28, 45, 68, 76, 312-313; type-name statements that follow the same root, listed under "Same-root follow-through" below | 2 required, 6 consequential | Amend with Part B design (see Timing) |
| 2a | Design: approved designs of finished work | Described, not amended | `sdlc/artifacts/design/modernization/DES-MODERNIZATION-009-mail-acl-k9-unpin-design.md:418`; `sdlc/artifacts/design/modernization/DES-MODERNIZATION-012-modernization-debt-closure-design.md:83-84, 166, 359, 380, 781` | 7 grep statements | None. They describe the vendored state, which stays true for it; Part B design supersedes them in its own scope |
| 3 | Requirements | YES (derived) | `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-019-replace-vendored-k9-with-thunderbird-mail-libraries.md:104` (AC-B3) | 1 required | Amend AC-B3 with the contract |
| 3a | Requirements: same blind spot, not named by P-1 | Scope Concern | `REQ-MODERNIZATION-012-seal-mailtransport-acl-exception-boundary.md:43, 45` (AC-1, AC-2: grep `com\.fsck\.k9` in `service/`); `EPIC-MODERNIZATION-006-dependency-currency-latest-stable.md:96` (success criterion 5: grep `com\.fsck\.k9\|com\.android\.email` in `service/`); `EPIC-MODERNIZATION-005-modernization-debt-closure.md:80, 84` | 5 | See Scope Concern below. No change proposed by this record |
| 4 | Stories and sprints | NO action | No migration story exists (`sdlc/artifacts/build/sprints/` holds U-060, U-061, U-062 as the latest; U-062 is done). Closed stories and records repeat the grep (U-025, U-026, U-030 and sprint-001/002 records; list from a search of `sdlc/artifacts` for a grep line naming `com.fsck.k9`: 32 files, most of them closed-sprint evidence) | historical | None. Closed records are not rewritten |
| 5 | Application code (`app/src/main`) | NO change under this record | The files the amended rule will constrain are Part B scope: 14 files outside `mail/transport/` (`App.java` 1, `mail/Attachment.java` 5, `mail/BackupImapStore.java` 11, `mail/BackupStoreConfig.java` 2, `mail/ConversionResult.java` 1, `mail/HeaderGenerator.java` 2, `mail/Headers.java` 1, `mail/MessageConverter.java` 5, `mail/MmsSupport.java` 5, `mail/MessageGenerator.java` 8, `mail/PersonRecord.java` 1, `mail/PinnedCertificateSocketFactory.java` 2, `di/MailModule.kt` 1, `preferences/AuthPreferences.java` 1) | 14 files, 46 occurrences | None here. Listed so the design can be sized |
| 6 | Tests (`app/src/test`) | NO change under this record | No test encodes the grep string or the boundary: no ArchUnit or equivalent library in any build or source file (search for `archunit`, `ArchRule`, `noClasses`, `ClassFileImporter`: 0 files); the only test that reads files from the source tree is `NetworkSecurityConfigTest.java:27-28` (manifest and network config). The rule is enforced by a person running a grep. 18 test files import `com.fsck.k9` (104 occurrences) and move with the migration; tests are outside the rule's scope (`app/src/main`) | 0 encoding the rule | None. See Observation below |
| 7 | Vendored library (`k9mail-vendored`) | NO | The vendored module keeps its own package `com.fsck.k9.mail` (`k9mail-vendored/src/main/AndroidManifest.xml:2`, `k9mail-vendored/build.gradle:51`) and is removed by Part B | — | None |
| 8 | Documentation (`README.md`, `sdlc/artifacts/engagement`) | NO | `README.md`: 0 matches for `fsck`; `sdlc/artifacts/engagement`: 0 matches for `com.fsck.k9` | 0 | None |
| | **Total** | | 2 layers changed by this record (design, requirements) | 3 required lines, 6 consequential lines | |

### Same-root follow-through (named for completeness; not asked by P-1)

The contract also names the old library's types by full class name. After the migration a few of those names
stop existing, for the same reason as Rule 1. The analysis says which: `com.fsck.k9.mail.MessagingException` is gone
(replaced by `net.thunderbird.core.common.exception.MessagingException`, C-304), while `AuthenticationFailedException`
(C-299), `TrustedSocketFactory` (C-067), `Message` (C-052) keep their names. The lines naming the old
`MessagingException` are CNTR-MODERNIZATION-007 lines 245, 249, 252-253, 263, 278, 281, 295, 325 (changelog) and the example at 423.
Lines 157 (`FetchProfile.Item`, whose `DATE` item is gone, analysis line 521) and 197 (`XOAuth2AuthenticationFailedException`)
are covered by the port-table drift and by CR-006. I am not proposing wording for these here (see Scope Concern).

### Cascade Analysis

**Root Layer:** Design and contracts (CNTR-MODERNIZATION-007).
**Root Defect:** Rule 1 and the translation row name one package root; the library to be adopted uses two.
**Cascade Direction:** Mid-stream down (contract, then requirement; no code or test change).

#### Propagation Path

| Step | From Layer | To Layer | Propagated? | Evidence |
|------|-----------|----------|-------------|----------|
| 1 | Contract Rule 1 grep string | REQ-019 AC-B3 grep string | YES | Same string `com\.fsck\.k9`; AC-B3 line 104 |
| 2 | Contract Rule 1 | REQ-012 AC-1/AC-2, EPIC-005 criteria 1-2, EPIC-006 criterion 5 | YES (earlier work) | Lines in matrix row 3a; these were written against the vendored library and are true for it |
| 3 | Contract / requirement | Application code | NO | Code still uses only `com.fsck.k9`; `net.thunderbird` has 0 matches in `app/` |
| 4 | Contract / requirement | Tests | NO | No test encodes the string |
| 5 | Contract / requirement | Stories and sprints | NO | No migration story exists |

#### Fix Order

1. [ ] Design and contracts: amend CNTR-MODERNIZATION-007 Rule 1, the translation row and the consequential wording — ROOT FIX
2. [ ] Requirements: amend REQ-MODERNIZATION-019 AC-B3 to the same grep — CASCADE FIX
3. [ ] Verification: on the amended texts, the grep strings in Rule 1 and AC-B3 are identical in both documents, and run on the repository at that moment give the same result as today (0 in `service/`, 14 + 9 files for the whole tree) — GATE

#### Affected In-Progress Work

| Story | Sprint | Phase | Action |
|-------|--------|-------|--------|
| None | — | — | No story is in progress against these texts. U-062 is done and sprint-019 is closed |

### Scope Concern (raised, not acted on)

- The batch approval in Record 4 names P-1 as the contract's Rule 1, the translation row, and REQ-019 AC-B3. The
  same one-rooted grep is also in REQ-012 AC-1/AC-2, EPIC-005 criteria 1-2 and EPIC-006 success criterion 5.
  EPIC-006 criterion 5 is live (approved, and its check is the engine seal after the migration); the others describe
  finished work. I have not widened this record to them. The Solution Architect and product owner should say whether
  EPIC-006 criterion 5 (and, if wanted, REQ-012 AC-1/2) take the same change in the same revision.
- The type-name follow-through above is the same root as P-1 but is wider than the lines Record 4 named.

### Observation (not a finding against the record)

The boundary is enforced only by a manual grep. Part B design may want an automated check that fails the build, in
which case the two package roots in the proposed text below are the strings to encode. This record does not ask for it.

---

## Proposed Change

Nothing is amended by this record. These are the exact changes to apply later (see Timing). "Current text" is quoted from
the files as read on 2026-11-17.

### Change 1 — CNTR-MODERNIZATION-007 Rule 1 (lines 345-349), the import-boundary rule

**Current text:**

```
1. **The ACL invariant — no `com.fsck.k9.*` crosses `MailTransport`.** No port method
   signature, return type, field of a crossing value type, or thrown exception names a
   `com.fsck.k9.*` type. Enforced by grep: `com.fsck.k9` in `service/` → **0**; in `State.java`
   → **0** (DES-009 AC-2, scoped per ADR-009-B). k-9 transport imports are confined to
   `mail.transport.K9MailTransport`.
```

**Proposed text:**

```
1. **The ACL invariant — no mail-library type crosses `MailTransport`.** The mail library's
   types live in two package roots: `com.fsck.k9` and `net.thunderbird`. No port method
   signature, return type, field of a crossing value type, or thrown exception names a type
   from either root. Enforced by grep: `com\.fsck\.k9\|net\.thunderbird` in `service/` → **0**;
   in `State.java` → **0** (DES-009 AC-2, scoped per ADR-009-B). Imports of either root are
   confined to the adapter package `com.zegoggles.smssync.mail.transport` (today
   `K9MailTransport` and the package-private handles `BackupFolderHandle` and
   `MailMessageHandle`). Once REQ-MODERNIZATION-019 Part B is complete, the whole-tree
   check `grep -rln "com\.fsck\.k9\|net\.thunderbird" app/src/main/java` lists only files
   under `app/src/main/java/com/zegoggles/smssync/mail/transport/`. A further package root
   introduced by the library (for example by the fork) is a contract revision, not a silent
   extension of the grep.
```

What the proposal keeps: the rule still forbids mail-library imports outside the adapter package, now for both roots; the
`service/` and `State.java` greps keep their scope and their "0" result. Which files may import: only files in
`app/src/main/java/com/zegoggles/smssync/mail/transport/`; no file in `service/`, `mail/` (outside `transport/`),
`di/`, `preferences/`, `activity/` or the application root (`App.java`) may. The grep is an alternation, so it is a
superset of today's check and cannot make the live vendored code fail anything it passes now.

### Change 2 — CNTR-MODERNIZATION-007 translation row (line 224)

**Current text:**

```
| any other `com.fsck.k9.mail.*` throwable (incl. `IllegalArgumentException` re-wrapped at `BackupImapStore.java:129-133`) | adapter internals | `MailException` (backstop) | generic error |
```

**Proposed text:**

```
| any other mail-library throwable (a class in `com.fsck.k9.*` or `net.thunderbird.*`, including the library's base `MessagingException`; incl. `IllegalArgumentException` re-wrapped at `BackupImapStore.java:129-133`) | adapter internals | `MailException` (backstop) | generic error |
```

(The stale line citation `BackupImapStore.java:129-133` is pre-existing drift that the analysis reports, line 552 item (v), and is left as is.)

### Change 3 — CNTR-MODERNIZATION-007 consequential wording (lines 25-28, 45, 68, 76, 312-313)

Each occurrence of "`com.fsck.k9.*` type" that states the rule becomes "mail-library type (`com.fsck.k9.*` or `net.thunderbird.*`)".
Examples: line 27 "no `com.fsck.k9.*` type may cross this boundary" becomes "no mail-library type (`com.fsck.k9.*` or `net.thunderbird.*`)
may cross this boundary"; line 76 "Never import `com.fsck.k9.*`." becomes "Never import `com.fsck.k9.*` or `net.thunderbird.*`.";
line 45 "ZERO com.fsck.k9.* after MU-008" becomes "ZERO com.fsck.k9.* / net.thunderbird.* after MU-008". Line 68 ("Own all
`com.fsck.k9.mail.*` transport types") becomes "Own all mail-library transport types (`com.fsck.k9.mail.*` and the `net.thunderbird.*` types the library uses)".
These carry no new rule; they keep the text consistent with Change 1.

### Change 4 — REQ-MODERNIZATION-019 AC-B3 (line 104)

**Current text:**

```
- **AC-B3**: `grep -rln "com\.fsck\.k9" app/src/main/java` lists only files under `app/src/main/java/com/zegoggles/smssync/mail/transport/` (or, if Part A shows the package name changed, the equivalent new package string appears only there). This also removes the `App.java`, `AuthPreferences.java`, and `MailModule.kt` references.
```

**Proposed text:**

```
- **AC-B3**: `grep -rln "com\.fsck\.k9\|net\.thunderbird" app/src/main/java` lists only files under `app/src/main/java/com/zegoggles/smssync/mail/transport/`. Part A found that the replacement types sit in both package roots (`com.fsck.k9.*` and `net.thunderbird.*`); both are named in the grep. This also removes the `App.java`, `AuthPreferences.java`, and `MailModule.kt` references.
```

Testable reading of Changes 1 and 4: with `grep -rln` as written, a file outside the adapter package that contains either string,
including in a comment or javadoc, fails the criterion. This is the same strictness REQ-012 AC-1 already applies to `service/`.

---

## Timing

The contract describes the vendored implementation, and the vendored implementation stays the live code until the migration
stories land. So:

- **When to apply.** Apply Changes 1 to 4 with the Part B design: the contract revision (a new version of
  CNTR-MODERNIZATION-007) and the AC-B3 edit are made, reviewed and approved together with that design, so the design is
  written against the final wording. These change records are preconditions of Part B design (Record 4, "raised before any
  Part B design"; analysis conditions 6 and the row 6 and row 10 owners). They are approved first; the text is applied when
  the design opens. Change 2 and the type-name follow-through describe types that do not exist until the library is swapped,
  so they take effect (are true) only with the migration story that replaces the vendored module.
- **What may be applied early.** Change 1's grep and Change 4's grep are supersets of today's checks and are true of today's
  code (0 matches for `net.thunderbird`). Applying them at design time therefore makes no statement false.
- **What must not happen before then.** (a) No edit to the contract, REQ-019 or any other artifact because of this record
  before it is approved and the design opens. (b) The existing greps in REQ-012 AC-1/2 and the EPIC criteria must not be
  loosened or replaced while the vendored library is live; they are the gates that pass today. (c) No code or test is
  changed under this record. (d) The rule must not be applied "retroactively" to the 14 files outside the adapter package
  before the migration: they legitimately use `com.fsck.k9` today and are moved by Part B.
- **Different timing considered.** Applying the whole change now would leave the contract describing an adapter and
  exception types that do not exist; I rejected it for that reason.

---

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->

Advisory only — refined during story planning.

**What the rule protects.** Rule 1 is the anti-corruption-layer invariant of the `MailTransport` port (Evans, *Domain-Driven Design*, Ch. 14): the engine sees only app-owned types, so the library can change behind the adapter. The rewording keeps the invariant and widens only the detector, from one package root to two. The alternation grep is a superset of today's check and is true of today's code (`net\.thunderbird` has no match under `app/`), so it cannot turn a passing gate red. Classification (DCR), severity (Medium) and the proposed wording: agreed.

**Consequences the record does not name (checked against the code).** Under Change 1 and AC-B3 every file that imports either root must sit under `mail/transport/`. Part B design must settle three placements up front:
- `mail/PinnedCertificateSocketFactory.java` (package `com.zegoggles.smssync.mail`, imports `com.fsck.k9.mail.ssl.TrustedSocketFactory` at line 20) moves under `mail/transport/`, and so does CR-009's app-owned default factory. CNTR-MODERNIZATION-001 line 96 names the pinned factory by its current full name, and none of CR-007, CR-008 or CR-009 rewords that line.
- `App.java:260` calls `K9MailLib.setDebugStatus`. The analysis (Q2 inventory, row 12) says the new library needs a `Logger` assigned on `net.thunderbird.legacy.logging.Log` before any mail call. `App.java` can only reach that through an app-owned entry point in the adapter package.
- CR-006's new `OAuth2TokenProvider` implementation implements a library interface. It therefore lives in `mail/transport/` and depends on `auth.TokenRefresher` (the adapter depends on auth, never the reverse).

**Cascade.** The record itself changes two layers (contract and requirement) and no code. Its Scope Concern matters more than it looks. EPIC-MODERNIZATION-006 success criterion 5 (line 96) carries the one-root grep `com\.fsck\.k9\|com\.android\.email` over `service/`, and CR-010 (P2) rewrites that same sentence. Decide both in one pass. I recommend adding `\|net\.thunderbird` there, because criterion 5 is the live gate after the migration. REQ-012 AC-1/2 describe finished work; one supersession note on REQ-012 can cover this record together with CR-006 (AC-5) and CR-008 (line 75).

**Story shape and order.** This record needs no story of its own. Change 4 (AC-B3) is true today and can go into the single requirements pass with CR-010 and CR-011, before any design. Changes 1 to 3 go into one CNTR-MODERNIZATION-007 v3 revision together with CR-006 (the current version is v2, contract line 319).

**Disagreement on timing.** This record applies the contract text "with the Part B design". CR-007 to CR-009 instead hold CNTR-MODERNIZATION-001 back until the code makes it true. Both contracts are checked against code by sprint contract verifications, so one policy should govern both (see CR-007's section). Change 2 names `net.thunderbird` throwables that do not exist until the library swap, so it belongs with whichever policy is chosen. It should not be applied early along with the grep.

**Fitness function.** Today a person running a grep is the only enforcement. Part B design should weigh an automated dependency-rule test ("no class outside `..mail.transport..` depends on `com.fsck.k9..` or `net.thunderbird..`") that fails the build. Gain: no human step. Cost: a test-only dependency. The grep stays as the written criterion because it also catches comments, which a bytecode check does not.

**Risk.** A fork patch that introduced a third package root would silently escape the grep. The record's last sentence of Change 1 (a new root needs a contract revision) handles this; keep it.

---

## Plan

- **Execution Window:** Backlog. Precondition of Part B design: approve before the design starts; apply with the design.
- **Assigned To:** TBD (Solution Architect for the contract revision; Requirements Analyst for AC-B3)
- **Estimated Effort:** Text-only change to two artifacts, plus review. No code.
- **Sprint:** Not scheduled. This record is not a story to schedule on its own.

### Fix Checklist

- [ ] Design and contracts: CNTR-MODERNIZATION-007 Rule 1, translation row, consequential wording amended (Changes 1-3), changelog entry added
- [ ] Requirements: REQ-MODERNIZATION-019 AC-B3 amended (Change 4)
- [ ] Decision recorded on the Scope Concern (EPIC-006 criterion 5; REQ-012 AC-1/2)
- [ ] Verification: the grep strings in Rule 1 and AC-B3 are identical, and on the repository at that moment return 0 in `service/` and 14 + 9 files for the whole tree

### Next Steps

Severity is Medium, so the create-cr wording applies: CR filed and queued for sprint planning. Run `/amp:create-stories --from-cr CR-005` when ready to schedule.
Read this with the note in Plan: CR-005 is a precondition of Part B design, not a story to schedule on its own. The product owner approved
it as part of a batch (Record 4); the work is the text edit at design time. Any story created from it should be the contract and criterion
revision inside the Part B design work, not a separate repair story.

---

## Execution

### Changes Made

| File | Change | Layer |
|------|--------|-------|
| | (pending) | |

### Verification Evidence

(pending)

### Commit Traceability

| Commit | Date | Branch | Scope | Author |
|--------|------|--------|-------|--------|
| | | | | |

---

## Closure

- **Root Cause Category:** (pending; `root_cause_categories` is not configured in `sdlc/config.yaml`)
- **Prevention Recommendation:** (pending) Candidate: when a contract states a boundary as a grep over a package name, record the package roots of the replacement as a design input before approving the contract, and where the project can afford it, encode the boundary as an automated test so a renamed package fails the build instead of passing silently.
- **Closed By:** (pending)
- **Date Closed:** (pending)
