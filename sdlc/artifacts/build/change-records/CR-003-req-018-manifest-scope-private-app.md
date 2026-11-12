---
status: implemented
severity: medium
artifact_type: change-request
requirements:
  - REQ-MODERNIZATION-018
design_docs:
  - DES-MODERNIZATION-013
related_stories: []
tags: []
id: CR-003
title: 'REQ-MODERNIZATION-018 amendment: complete the AC-3 manifest removal list, allow a test-only host (AC-6), widen the AC-8 re-measure list, and drop store-listing and donor wording (private app)'
type: RCR
domain: change-records
summary: 'Amend REQ-MODERNIZATION-018: billing removal also removes transitive Play services/datatransport manifest entries (AC-3); allow a test-only host activity (AC-6); extend coverage re-measure list (AC-8); drop store-listing and donor wording because the app is private and unpublished (AC-11, AC-12).'
updated_at: '2026-11-12T18:05:35.892Z'
---

# CR-003: REQ-MODERNIZATION-018 amendment (manifest scope, test host, coverage list, private-app scope)

## Metadata

| Field | Value |
|-------|-------|
| **CR Number** | CR-003 |
| **Type** | RCR (Requirement Change) |
| **Severity** | Medium |
| **Status** | Open |
| **Reported By** | Michael Horsley |
| **Date Reported** | 2026-11-12 |
| **Source** | Sprint work: design of REQ-MODERNIZATION-018 (DES-MODERNIZATION-013), before any story existed |

---

## Finding

### Description

REQ-MODERNIZATION-018 (approved 2026-11-12, no story, no code changed) needs six amendments.

1. **AC-3 cannot pass as written.** It says the merged manifest differs "only by" billing and
   `DonationActivity` entries. Removing `com.android.billingclient:billing:7.1.1` also removes its
   transitive libraries, which nothing else in the app uses, and those libraries contribute five more
   manifest entries: `com.google.android.gms.common.api.GoogleApiActivity`; the
   `com.google.android.gms.version` meta-data;
   `com.google.android.datatransport.runtime.backends.TransportBackendDiscovery` (with its
   `backend:com.google.android.datatransport.cct.CctBackendFactory` meta-data);
   `com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService`; and
   `com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver`.
   The whole `<queries>` element also goes, because its only two intents are billing's. `INTERNET` and
   `ACCESS_NETWORK_STATE` stay because the app declares them itself. The Notes line saying the release
   manifest was never diffed is out of date.
2. **AC-6** requires "an AppCompat activity that remains in the app" as the new host for
   `EnrollmentDialogThemeTest`. The approved design (D2) uses a test-only nested subclass of the
   production `ThemeActivity` instead, and the stale `DonationActivity` type and javadoc references in
   that test must also be corrected.
3. **AC-8** omits `preferences` and `utils` from the coverage re-measure list.
   `Keys.DONATE` is a covered line in `preferences`, and `BundleBuilder` is in `utils`.
4. **AC-11 and inventory item 15** require store-listing text edits. The product owner has withdrawn
   that scope (decision below).
5. **AC-12 and "Persisted donation state"** contain wording about previously donated users and
   Google-side purchases. Only the engineering facts should remain.
6. **Inventory item 7** says per-file matching lines range "from 3 to 10". The range is 3 to 12.

### Source of Truth

- **DES-MODERNIZATION-013** (`sdlc/artifacts/design/modernization/DES-MODERNIZATION-013-donation-flow-removal-design.md`,
  status: approved): decisions D2, D3, D5, D7 and the Notes section ("Scope Concerns: required
  REQ-MODERNIZATION-018 change record", SC-1 to SC-3). The Design Auditor confirmed each point
  against the repo.
- **Product owner decision, 2026-11-12 (verbatim):** "I don't own the old app store listing. this is
  just going to be a private app for me". This is the only basis for amendments 4 and 5.
- **Evidence:** `sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/` (manifests, dependency trees)
  and `app/build/outputs/logs/manifest-merger-debug-report.txt` (local build output, not committed).

### Evidence

Facts re-verified in this session (2026-11-12) against the repo and evidence files:

| Claim | Verification |
|-------|--------------|
| Five transitive manifest entries exist in both variants | Present in `baseline-manifest-debug.txt` lines 233, 236, 326, 332, 336 and `baseline-manifest-release.txt` lines 232, 235, 325, 331, 335 (same in `after-*`, since U-060 did not change them). The `backend:` meta-data is a child of the `TransportBackendDiscovery` service (`after-manifest-release.txt` lines 325-328) |
| `<queries>` holds only billing intents | `after-manifest-release.txt` lines 70-76: one `queries` element, two intents (`InAppBillingService.BIND`, `BillingOverrideService.BIND`). The merger report shows both `ADDED from [com.android.billingclient:billing:7.1.1]` |
| Debug and release manifests are equivalent | `diff` of `baseline-manifest-release.txt` against `after-manifest-release.txt` with `(line=N)` stripped is empty. The release manifest therefore exists and the Notes line is stale |
| `INTERNET`, `ACCESS_NETWORK_STATE` declared by the app | `app/src/main/AndroidManifest.xml` lines 56 and 61. `DonationActivity` entry is line 129 |
| `DonationActivity` references in the test | `EnrollmentDialogThemeTest.java`: line 9 (import), 114 (comment), 117, 164 (javadoc `{@link}`), 179, 244. The three `@Test` host calls are 117, 179, 244 |
| `EnrollmentDialogThemeTest.HostActivity` does not exist yet | Grep for `class HostActivity` and `ThemeActivity` in the test: no match other than the two `DonationActivity` comment lines. It is a design proposal, not existing code |
| Matching-line range in strings files is 3 to 12 | Counted over `values/` (12) and the 21 locale files (min 3 `values-it`, max 12 `values-zh-rTW`). Note: 25 `values*/strings.xml` exist, and the 3 not in the inventory (`cs`, `es`, `nb-rNO`) have 0 matches |
| `Keys.DONATE` is used only by `MainSettings` | `Preferences.java:101` (definition); `MainSettings.java:24` and `:172`. The only other hits are `DonationActivity` enum constants `DONATED`/`NOT_DONATED`, which are different symbols |
| Lint baseline billing entry | `app/lint-baseline.xml` lines 114-123 (GradleDependency, billing 2.1.0) |
| Store text with donation wording | `metadata/play/text/{en,nl,da}/full_description.txt` (en lines 10, 12; da, nl line 10). Out of scope after this CR |

Not verified (labelled): per-package coverage figures in AC-8 and the "Predicted" values in DES-013 D3
are estimates from the local JaCoCo report; only the story's I8 re-measurement is binding.

---

## Triage

### Classification

- **Type:** RCR. The product owner's decision changes scope (store listing and donor wording are
  withdrawn), and the manifest, test-host and coverage items refine the requirement to match what the
  repo and design show. It is not a DEF (no implementation exists) and is not purely a DCR, because the
  requirement layer, not the design, is where the text is wrong.
- **Root:** Requirements layer (REQ-MODERNIZATION-018). Its AC-3 was written from the debug merged
  manifest and a view of billing's own AAR only, without the dependency tree. The design layer
  (DES-013) already reflects the amended reading.
- **Layers Affected:** 2 requested for edit (requirements; stories to be authored against the amended
  text), plus 3 with follow-up only (code, tests, docs). `change_records.layers` is not configured in
  `sdlc/config.yaml`, so the layers used are requirements / design / stories / code / tests / docs.
- **Severity Justification:** Medium. The defect is isolated to one approved requirement, no story or
  code exists, and no in-flight work is blocked. If left unfixed, AC-3 would fail on the first
  verification run, so it must be fixed before stories are created.

---

## Impact Analysis

Greps run on 2026-11-12 (read-only).

| # | Layer | Affected? | Files | Occurrences | Action Needed |
|---|-------|-----------|-------|-------------|---------------|
| 1 | Source of truth: DES-013 + evidence + product owner decision | Ground truth | `DES-MODERNIZATION-013-...md` | D2, D3, D5, D7, Notes SC-1..3 | None (read-only) |
| 2 | Requirements | Yes | `REQ-MODERNIZATION-018-...md` (edit); `_index.md` (lists 018, no text change) | AC-3, 6, 8, 11, 12; items 7, 11, 15; Context "Persisted donation state"; Notes line 126 | Amend in place (below); record CR-003 in `change_records` |
| 2a | Requirements, siblings (flag only) | Yes, wording | `EPIC-MODERNIZATION-006-...md`; `REQ-MODERNIZATION-015-...md`; `REQ-MODERNIZATION-017-...md` | see Siblings | Do not edit here; separate follow-up |
| 3 | Design | Already aligned | `DES-MODERNIZATION-013` (`change_records: []`) | D2/D3/D5/D7 already amended | Add CR-003 to its `change_records` when closed; D8 note on DES-010 unaffected |
| 4 | Stories | No stories exist | `grep -rlE "REQ-MODERNIZATION-018"` over `sdlc/` finds only the requirement, this CR, DES-013 and `_index.md` | 0 | Create stories after the amendment |
| 5 | Code | Not yet; future story | `AndroidManifest.xml`, `app/build.gradle`, `Preferences.java`, `MainSettings.java`, `Consts.java`, `preferences.xml`, 22 `strings.xml`, `lint-baseline.xml` | per inventory | No change from this CR; the story does the work |
| 6 | Tests | Not yet; future story | `EnrollmentDialogThemeTest.java` (lines 9, 114, 117, 164, 179, 244); `DonationActivityTest`, `SkuTest` deleted | 6 lines in one file | Story re-hosts the test (AC-6) |
| 7 | Docs | Reduced | `README.md` 43-44, 241; `CHANGES`; `code-location.md` 37. `metadata/play/text/*/full_description.txt` removed from scope | 3 files remain | Story edits the three; store text untouched |

Layers affected: 7 of 7 are touched in some way, but only layer 2 is edited by this CR. All others are
already aligned, absent, or follow-up work that existed before the CR. The severity multiplier
therefore does not apply as a rework risk.

### Siblings touched by the same decision (flagged, not edited)

| Artifact | Where | Issue |
|----------|-------|-------|
| `EPIC-MODERNIZATION-006-dependency-currency-latest-stable.md` | Line 24 ("with ... the donation flow ... removed"), line 28, line 68 ("Publishing and release decisions (store listing, release signing, rollout)"), line 143 ("a publishing matter and is not decided here") | Publishing and communication wording assumes a public store release. The owner has now stated the app is private. Needs a wording review |
| `REQ-MODERNIZATION-015-toolchain-and-test-tooling-latest-stable-minsdk-23.md` | AC-10 (line 136): "A user-facing changelog or release-notes entry states that Android 5.0 and 5.1 are no longer supported" | Presumes users who need a release note. Review whether a `CHANGES` entry is still required for a private app |
| `REQ-MODERNIZATION-017-platform-level-current-stable-api.md` | Line 171: "Informational only ... Google Play target-level policy deadlines" | Play policy note is moot for a private, unpublished app. Informational, so low risk |

---

## Suggested Approach

Advisory only — refined during story planning.

### Architect triage notes (Solution Architect, 2026-11-12)

- **Resolution path: confirmed.** This is a requirement-text amendment with no code story under this CR.
  The implementation stays one story (DES-013 D1), authored after the amendment. Caveat on precedent:
  CR-001 and CR-002 amended approved *contracts*, which carry a version and changelog (CNTR-004 v1 to v2).
  REQ-018 has no version field. So that the change to approved text is visible in the body as well as in
  frontmatter, add a dated one-line amendment note to REQ-018 Notes ("Amended 2026-11-12 by CR-003:
  AC-3, 6, 8, 11, 12, items 7 and 15, Context"). Run a Requirements Reviewer pass on AC-3, AC-6 and AC-8
  before story creation, because they change what verification counts as a pass, not just wording.
- **Replacement text checked against DES-013:** no contradictions found.
  - R4/D5 match: the same five transitive entries, the whole `<queries>` element, `INTERNET` and
    `ACCESS_NETWORK_STATE` retained, and both variants checked. R4 also requires a merged *release*
    manifest diff (`:app:processReleaseManifest`). D5 requires only the debug merged diff plus packaged
    `aapt2` diffs for both variants, so the requirement is a superset of the design. The story plan should
    follow R4.
  - R5/D2 match on the host, the import, the three call sites and unchanged assertions. R5 cites comment
    lines 114 and 164, but D2 covers 114-116 and 164-167 and requires the corrected text to say the theme
    comes from `ThemeActivity.onCreate()`. Cite the ranges, or drop line numbers, which shift at I1 anyway.
  - R6/D3 match: six rules re-measured, only the root value goes into the Rule 4 comment, and the other
    five values go in the implementation log. One tension already in the text survives: the retained second
    AC-8 bullet says "the only legitimate edits to the JaCoCo block are the deletion of the donation rule
    and its comment", while the third bullet requires a root-rule comment edit. Consider widening the second
    bullet to name all three edits so the two bullets cannot be read against each other.
  - R7/D7 and R8/Post-removal shape match.
- **Frontmatter risks (Librarian or Analyst).** (a) R10 says "set `change_records: [CR-003]`". DES-013 D8 and
  AC-10 later add the reciprocal REQ-010 supersession record to the same list, so treat this as an append,
  not a replace. (b) REQ-018 `related_design_docs` lists only DES-MODERNIZATION-010, the superseded design,
  and not DES-MODERNIZATION-013, the governing one. Add DES-013 in the same amendment.
- **Drift risk after amendment.** Once REQ-018 is amended, several DES-013 passages describe the
  *pre-amendment* text: Context facts 1 and 5, the D2 closing paragraph, Risk R3 ("REQ note") and Notes
  SC-1 to SC-3 ("must be in place"). They become historical, not contradictory. Adding CR-003 to DES-013
  `change_records` with no body edit is enough. Where the design describes the requirement, story planning
  should treat the amended REQ-018 as authoritative.
- **Siblings: use a separate follow-up CR (one RCR covering all three), not this one.** They are different
  approved artifacts with their own review, and CR-003 should be closable once REQ-018 is amended.
  REQ-015 AC-10 removes a deliverable rather than rewording it, so it needs its own product owner decision.
  Note that R7 keeps a `CHANGES` entry for REQ-018, so keeping REQ-015 AC-10 as a `CHANGES` entry with
  "user-facing release note" wording dropped would be the consistent choice. A grep of `sdlc/` finds no
  stories for REQ-015 or REQ-017, so nothing in flight is affected and the follow-up does not block REQ-018.
- **Story count:** zero stories from this CR, and one implementation story from the amended REQ-018.

### Intake steps

1. The Requirements Analyst amends REQ-MODERNIZATION-018 in place using the text below, adds `CR-003`
   to its `change_records` frontmatter, and leaves `status: approved`. This follows CR-001 and CR-002,
   which amended approved artifacts in place and recorded the change.
2. Add `CR-003` to `change_records` on DES-MODERNIZATION-013 (no body edit needed: D2, D3, D5, D7 and
   its Notes already match).
3. Create the story from the amended requirement and DES-013 (D1: one story, eight increments). The
   story's AC-3 evidence covers debug and release.
4. Raise the three sibling artifacts as a separate wording review.

### Replacement text (apply mechanically)

All locations refer to the current text of
`sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-018-remove-donation-flow-and-play-billing.md`.
"Replace" means the whole item or paragraph named. Anything not listed stays unchanged.

**R1. Context, inventory item 7 (table row 7).** In the Item cell, replace
`per-file counts of matching lines range from 3 to 10` with `per-file counts of matching lines range from 3 to 12`.
Nothing else in the row changes.

**R2. Context, inventory item 15 (table row 15).** Replace the whole row with:

`| 15 | (Withdrawn by CR-003. Store listing text under metadata/play/** is inherited upstream text for a listing the product owner does not own, and is out of scope and untouched.) | none |`

Do not renumber: AC-1 and AC-12 cite items by number.

**R3. Context, manifest paragraph (the bullet list after "The library's AAR manifest contributes", and the sentence after it).**
This is a necessary consequence of R4 and is not in the intake list, so confirm it when applying. Replace the
sentence "This merged manifest is a local build artifact, so the merged-manifest check in AC-3 must be re-run on a fresh build." with:

> Removing `billing` also removes its transitive libraries (`com.google.android.gms:*`,
> `com.google.android.datatransport:*`, `com.google.firebase:firebase-encoders*`), which appear under
> `billing:7.1.1` and nowhere else in the U-060 runtime dependency trees. Those libraries contribute five more entries to the
> merged manifest in both debug and release builds: the activity
> `com.google.android.gms.common.api.GoogleApiActivity`; the `com.google.android.gms.version` meta-data; the service
> `com.google.android.datatransport.runtime.backends.TransportBackendDiscovery` with its
> `backend:com.google.android.datatransport.cct.CctBackendFactory` meta-data; the service
> `com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService`; and the receiver
> `com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver`.
> The two `<queries>` intents above are the only children of the `<queries>` element, so the whole element is removed.
> `INTERNET` and `ACCESS_NETWORK_STATE` stay, because the app declares both itself
> (`app/src/main/AndroidManifest.xml` lines 56 and 61). The merged manifest is a local build artifact, so the
> check in AC-3 must be re-run on a fresh build. Evidence: `sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/`
> (`baseline-manifest-{debug,release}.txt`).

**R4. AC-3.** Replace with:

> 3. For both the debug and the release build, a fresh manifest differs from the pre-change manifest
>    only by the removal of the following, and nothing else is added, removed or changed. The check uses
>    the merged manifest (`./gradlew :app:processDebugManifest` and `:app:processReleaseManifest`) and the
>    packaged manifest (`aapt2 dump xmltree --file AndroidManifest.xml`, with `(line=N)` stripped and app
>    resource IDs (`@0x7f[0-9a-f]{6}`) normalised before diffing; renumbered app resource IDs are not a
>    difference, because the merged-manifest diff checks those references by name):
>    - the `com.android.vending.BILLING` permission
>    - the whole `<queries>` element (both billing intents)
>    - the `com.google.android.play.billingclient.version` meta-data
>    - the `ProxyBillingActivity` and `ProxyBillingActivityV2` activities
>    - the `.activity.donation.DonationActivity` activity
>    - from billing's transitive libraries, which nothing else in the app uses:
>      `com.google.android.gms.common.api.GoogleApiActivity`; the `com.google.android.gms.version` meta-data;
>      `com.google.android.datatransport.runtime.backends.TransportBackendDiscovery` (with its `backend:` meta-data);
>      `com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService`;
>      `com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver`
>
>    `INTERNET` and `ACCESS_NETWORK_STATE` remain, because the app declares both itself.

**R5. AC-6.** Replace with:

> 6. Tests deleted are exactly `DonationActivityTest` (10 `@Test`) and `SkuTest` (5 `@Test`), 15 in total.
>    `EnrollmentDialogThemeTest` keeps all 3 `@Test` methods with their assertions unchanged. Only its host
>    activity changes from `DonationActivity` to a test-only subclass of the production `ThemeActivity`,
>    declared as a nested class of that test (`EnrollmentDialogThemeTest.HostActivity`). The import, the
>    local-variable type `DonationActivity` (lines 117, 179, 244), and the javadoc and comment references
>    to `DonationActivity` (lines 114 and 164) are corrected to match. No other test is deleted, ignored or weakened.

**R6. AC-8, third bullet and fourth bullet.** Keep the first two bullets. Replace the third and fourth with:

> - The `com.zegoggles.smssync` root-package rule (floor 0.50, last measured 60.0%, 18/30) must be
>   re-measured after `Consts.Billing` is removed, because `Consts` sits in that package. The new measured value is
>   recorded in that rule's comment in `app/build.gradle` (the only comment edit besides the Rule 5 note) and must
>   still be at least 50%. If it is below 50% the floor is not lowered. Instead, coverage is added until the rule
>   passes, and the shortfall is escalated.
> - The `activity` (0.10), `activity.fragments` (0.10), `receiver` (0.50), `preferences` (0.65) and `utils` (0.10)
>   rules are re-measured and must still pass without their floors being edited. `MainSettings` is in
>   `activity.fragments`, where removing `checkUserDonationStatus()` reduces both covered and total lines.
>   `Keys.DONATE` is a covered line in `preferences`. These five re-measured values are recorded in the story's
>   implementation log, not in `app/build.gradle`.

**R7. AC-11.** Replace with:

> 11. `CHANGES` has a new entry under the unreleased 1.6.0 heading, in the existing bullet style, stating that the
>     donation feature and the in-app billing dependency were removed. `README.md` no longer contains the
>     donation paragraph (lines 43-44) or the billing permission bullet (line 241), and the sentence at lines 40-41
>     about there never being a paid version is retained. `sdlc/artifacts/engagement/code-location.md` line 37 is
>     updated. `metadata/play/**` is inherited upstream store text and is out of scope: it is not edited.

**R8. AC-12 and the "Persisted donation state" paragraph.** Replace the paragraph (Context, "Persisted donation state: ...") with:

> Persisted donation state: none was found. The `donate` preference is an intent-only `Preference` with no
> stored value. `Keys.DONATE` was never stored, and searches of `app/src/main/java/com/zegoggles/smssync/preferences`
> found no code that reads or writes a donation value. `Preferences.reset()` does not touch it.

Replace AC-12 with:

> 12. No `SharedPreferences` key is migrated, written or deleted. The enum constant `Keys.DONATE` is removed,
>     and no stored value exists for it (see Context), so no cleanup code or schema version change is required.
>     A unit test or reviewed code evidence confirms that `Preferences` never persisted `donate`. The removal does
>     not clear user preferences, and `Preferences.reset()` behavior is unchanged. The `app/lint-baseline.xml`
>     billing entry (inventory item 12) is deleted, with no other baseline entry edited.

**R9. Notes, last bullet (line 126).** Replace with:

> - The release manifest was verified from `sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/after-manifest-release.txt`,
>   which is equivalent to the debug manifest apart from line numbers and the debug-only `android:debuggable` attribute. AC-3 applies to both variants and the story re-runs it.

**R10. Frontmatter and Description wording.** Set `change_records: [CR-003]` (the Librarian or Analyst may use the
repo's existing list form). No change to `status`. The Description and Rationale need no edit; the Rationale's
"a merged-manifest permission and two `queries` entries" understates the removal but is not wrong.

---

## Cascade Analysis

**Root Layer:** Requirements (REQ-MODERNIZATION-018)

**Root Defect:** AC-3 lists only billing and `DonationActivity` manifest entries, but the billing removal also
removes five transitive-library entries. The other amendments refine the test-host, coverage-list and
documentation scope, and the product owner's decision withdraws store-listing and donor wording.

**Cascade Direction:** Mid-stream down (requirement to stories; the design is already aligned). Nothing is built.

### Propagation Path

| Step | From Layer | To Layer | Propagated? | Evidence |
|------|-----------|----------|-------------|----------|
| 1 | Requirements | Design (DES-013) | No | D2, D3, D5, D7 and Notes already carry the amended reading |
| 2 | Requirements | Stories | No | No story references REQ-MODERNIZATION-018 (grep of `sdlc/`) |
| 3 | Requirements | Code and tests | No | No code changed. HEAD still contains `DonationActivity` |
| 4 | Decision (private app) | Sibling requirements/epic | Yes, wording only | See Siblings. Not blocking |

### Fix Order

1. [ ] Requirements: amend REQ-MODERNIZATION-018 with R1-R10 and record CR-003 — ROOT FIX
2. [ ] Design: add CR-003 to DES-MODERNIZATION-013 `change_records` — CASCADE FIX
3. [ ] Stories: create the story from the amended requirement and DES-013 — CASCADE FIX
4. [ ] Siblings: wording review of EPIC-006, REQ-015 AC-10, REQ-017 note — separate follow-up
5. [ ] Verification: `grep -nE "queries|GoogleApiActivity|TransportBackendDiscovery|store listing|metadata/play|previously donated|Google's side" <REQ-018>` shows only the amended wording

### Affected In-Progress Work

| Story | Sprint | Phase | Action |
|-------|--------|-------|--------|
| None. Sprint-017 is completed and no REQ-018 story exists | n/a | n/a | n/a |

---

## Plan

- **Execution Window:** Inline: amend before stories are created.
- **Assigned To:** Requirements Analyst
- **Estimated Effort:** Under 1 hour (text edit and two frontmatter updates)
- **Sprint:** None

### Fix Checklist

- [ ] Requirements: apply R1-R10 to REQ-MODERNIZATION-018; add `CR-003` to `change_records`
- [ ] Design: add `CR-003` to DES-MODERNIZATION-013 `change_records`
- [ ] Verification: the five manifest entries appear in AC-3 and Context; "10" in item 7 becomes "12"; no store-listing or donor wording remains
- [ ] Stories: create from the amended text

---

## Execution

### Changes Made

| File | Change | Layer |
|------|--------|-------|
| `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-018-remove-donation-flow-and-play-billing.md`, frontmatter | Appended `CR-003` to `change_records`; appended `DES-MODERNIZATION-013` to `related_design_docs`; `traces_to` file paths replaced with `EPIC-MODERNIZATION-006`. `status` unchanged (approved) | Requirements |
| REQ-MODERNIZATION-018, Context inventory item 7 | R1: per-file match range "3 to 10" changed to "3 to 12" | Requirements |
| REQ-MODERNIZATION-018, Context inventory item 15 | R2: row replaced with the "Withdrawn by CR-003" row; not renumbered | Requirements |
| REQ-MODERNIZATION-018, Context manifest paragraph | R3: stale "local build artifact" sentence replaced with the transitive-library, `<queries>` and retained-permission text | Requirements |
| REQ-MODERNIZATION-018, Context "Persisted donation state" | R8: replaced with the engineering-facts-only paragraph | Requirements |
| REQ-MODERNIZATION-018, AC-3 | R4: both variants, merged and packaged manifest, whole `<queries>` element and five transitive entries added to the removal list | Requirements |
| REQ-MODERNIZATION-018, AC-6 | R5: host is test-only nested `EnrollmentDialogThemeTest.HostActivity` extending `ThemeActivity`. Triage correction: line ranges 114-116 and 164-167, and the corrected comment must name `ThemeActivity.onCreate()` | Requirements |
| REQ-MODERNIZATION-018, AC-8 | R6: third and fourth bullets replaced (adds `preferences` and `utils`). Triage correction: second bullet widened to permit exactly the donation-rule and Rule 5 note deletion plus the root-rule comment update | Requirements |
| REQ-MODERNIZATION-018, AC-11 | R7: store-listing clause and handover sentence replaced by the `metadata/play/**` out-of-scope statement | Requirements |
| REQ-MODERNIZATION-018, AC-12 | R8: "Previously donated users:" lead-in removed | Requirements |
| REQ-MODERNIZATION-018, Notes | R9: stale "not verified" release-manifest bullet replaced; dated "Amended 2026-11-12 by CR-003" note added with the verbatim owner decision | Requirements |
| `sdlc/artifacts/design/modernization/DES-MODERNIZATION-013-donation-flow-removal-design.md`, frontmatter | Appended `CR-003` to `change_records`; no body or other frontmatter edit | Design |

### Reviewer corrections (2026-11-12)

- RAR-001 (blocking), REQ-018 AC-3: the packaged-manifest diff now normalises app resource IDs (`@0x7f[0-9a-f]{6}`) as well as `(line=N)`, because removing play-services-base renumbers app resources. The R4 replacement text above was corrected to match.
- REQ-018 Notes (R9): "apart from line numbers" corrected to "apart from line numbers and the debug-only `android:debuggable` attribute". The R9 text above was corrected to match.
- REQ-018 AC-6: "both corrected passages" (lines 114-116 and 164-167) name `ThemeActivity.onCreate()`.
- REQ-018 AC-6: the `DonationActivity.class` argument on lines 117, 179 and 244 is swapped along with the local-variable type, and `HostActivity` is a concrete `public static` nested class because `ThemeActivity` is abstract.
- REQ-018 AC-8: the root-package rule has two comments (header lines 397-399, trailing `// MEASURED` comment on line 406). Either or both may be updated, and deleting the donation rule includes its own comment lines.
- REQ-018 frontmatter `updated_at`: left as is (engine-managed).

### Verification Evidence

Amendment applied 2026-11-12. The Requirements Reviewer pass on AC-3, AC-6 and AC-8 recommended by the architect is not yet run.

### Commit Traceability

| Commit | Date | Branch | Scope | Author |
|--------|------|--------|-------|--------|
| *(pending)* | | sdlc/modernization-plan | | |

---

## Closure

- **Root Cause Category:** Requirement authored from a partial view: AC-3 used the library's own manifest and not the dependency tree, and the scope assumed a published app.
- **Prevention Recommendation:** When a requirement removes a dependency, derive the expected manifest and APK delta from the resolved dependency tree (transitive closure), not from the library's own AAR, and run the check on every build variant before approval. Record the audience and distribution of the app in the product context so publishing-related ACs are not written for an unpublished app.
- **Closed By:** (pending)
- **Date Closed:** (pending)
