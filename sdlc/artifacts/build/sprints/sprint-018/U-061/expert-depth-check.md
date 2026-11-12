---
artifact_type: gate-result
story_id: "U-061"
gate: expert-depth-check
gate_id: "expert-depth-check"
checkpoint: plan-gate
pipeline: story-build
artifact_under_review: C:/Code/Android/sms-backup-plus/sdlc/artifacts/build/sprints/sprint-018/U-061/plan.md
producing_agent: amp/agents/solution-architect
verdict: FAIL
severity: warning
agent: "Solution Architect (separate_instance reviewer)"
reviewer_agent: amp/agents/solution-architect
reviewer_mode: separate_instance
iteration: 2
timestamp: "2026-11-12"
produced_at: 2026-11-12T00:00:00Z
recommendations_count: 0
criterion_results:
  - criterion: "Pattern grounding"
    verdict: satisfied_with_recommendation
    deficit_present: false
  - criterion: "Anti-pattern recognition"
    verdict: deficient
    deficit_present: true
  - criterion: "Trade-off articulation"
    verdict: satisfied
    deficit_present: false
  - criterion: "Citation depth"
    verdict: satisfied_with_recommendation
    deficit_present: false
  - criterion: "NFR and quality-attribute coverage"
    verdict: satisfied
    deficit_present: false
  - criterion: "Tooling evidence"
    verdict: satisfied
    deficit_present: false
  - criterion: "Sequencing rigor"
    verdict: satisfied
    deficit_present: false
  - criterion: "Anti-AI-slop check"
    verdict: satisfied
    deficit_present: false
---

# Gate Report: gate-result — U-061

## Gate: expert-depth-check @ plan-gate

## Verdict: FAIL (severity: warning, observe-only; does not block implementation)

## Summary

The plan (`sdlc/artifacts/build/sprints/sprint-018/U-061/plan.md`, iteration 2, 1137 lines) is operationally
very strong. It has line-anchored edits verified at `21838718`, fail-closed helper tools that were
executed during planning, full ISO 25010 coverage, measured JaCoCo, dependency-graph and dex baselines,
and a per-increment commit protocol. It contains no canonical grounding at all: no pattern names, no
anti-pattern names, no canonical-source citations, and no explicit statement that these do not apply.
Most architectural decisions belong to DES-MODERNIZATION-013 (D1–D8, D4a; for example the `HostActivity`
re-host is D2), so for Criteria 1 and 4 that silence costs little and is recorded as a recommendation.
Criterion 2 is different. The plan removes code that has visible canonical anti-patterns and describes
one of them only in passing, without naming or analysing it. That criterion is deficient, so the gate
FAILs under the rubric. The fix is a short section, not a re-plan.

## Findings

### Criterion 1 — Pattern grounding

**Verdict:** satisfied_with_recommendation

**Evidence present:** In Story Overview the plan says "The work is deletion only … The design fixes every
decision. This plan turns them into ordered, verifiable increments (I0–I8)". This is an implicit reason
why the plan makes no new architectural choice. The one structural code change, the nested
`HostActivity extends ThemeActivity` (I1.5), is design decision D2 in
`sdlc/artifacts/design/modernization/DES-MODERNIZATION-013-donation-flow-removal-design.md` (line 124).
It was not chosen by the plan.

**Evidence missing:** The plan does not explicitly say that no canonical pattern is selected at plan
level, and it does not name the patterns it applies. The applied patterns are the I1 re-host (a
Test-Specific Subclass), the IC-9 breach route ("characterization test of real behavior", I6.4), and the
I0→I8 before/after evidence capture.

**Passage:** Story Overview (lines 30–36); I1 (lines 348–400); I6.4 (lines 491–495).

**Recommendation / Remediation:** Add one sentence to Story Overview: "No architectural pattern is
selected at plan level. Decisions are owned by DES-013 D1–D8." Name the applied patterns where they
occur:
- I1: Test-Specific Subclass (Meszaros, *xUnit Test Patterns*, Ch. 26 "Test-Specific Subclass"). It
  replaces a production class used as a test fixture host.
- I6.4: Characterization Test (Feathers, *Working Effectively with Legacy Code*, Ch. 13 "I Need to Make a
  Change, but I Don't Know What Tests to Write").

### Criterion 2 — Anti-pattern recognition

**Verdict:** deficient

**Evidence present:** Quality Attribute Coverage › Reliability says "A swallowed-exception path and an
asynchronous billing-client lifecycle are removed." Deviation 7 (lines 116–117) records doc drift in
`endpoints.md:62` and `systems.md:55`. I5.2 removes a stale `lint-baseline.xml` entry for billing 2.1.0,
a version the build no longer uses.

**Evidence missing:** The plan does not state that it analysed anti-patterns, and it does not name the
anti-patterns visibly present in the code it modifies. In the source at `21838718`:
1. `MainSettings.checkUserDonationStatus()` (MainSettings.java:164–182) wraps the billing call in
   `catch (Exception e) { Log.w(TAG, e); }`. This is Error Hiding / Swallowed Exception (CWE-390,
   "Detection of Error Condition Without Action"; CWE-396, "Declaration of Catch for Generic Exception").
   The plan mentions the "swallowed-exception path" only as a benefit in one line. It does not say
   whether the same idiom exists elsewhere in `activity.fragments`, or whether removing it changes any
   observable behaviour. The second question matters because the swallowed failure previously hid
   billing-unavailable states on settings resume.
2. Every `onResume()` started a Play Billing IPC connection, which is remote I/O in a UI lifecycle
   callback. The plan describes this under Performance Efficiency but does not identify it as the
   anti-pattern that design D-series removes.
3. `EnrollmentDialogThemeTest` used the production `DonationActivity` as its AppCompat host. A test
   fixture was coupled to an unrelated production feature, which is a Fragile Test caused by
   fixture/interface sensitivity (Meszaros, *xUnit Test Patterns*, Ch. 16 "Fragile Test"). This coupling
   is the reason I1 exists. Naming it explains why I1 must come before I3, and why the replacement is
   nested and test-only.
4. The stale `lint-baseline.xml` billing 2.1.0 entry and the doc drift in deviation 7 are suppression
   rot / Lava Flow residue (Brown et al., *AntiPatterns*, Ch. 5 "Lava Flow"). The plan removes the first
   but leaves the second without classifying it.

**Passage:** Quality Attribute Coverage › Reliability (lines 820–821); Deviations 7 (lines 116–117);
I1 (lines 348–400); I2.2 (lines 407–410).

**Recommendation / Remediation:** Add a short "Anti-Pattern Analysis" section before Risks & Mitigations
with one row per item above. Each row gives the canonical name and citation, the location at `BASE`, the
increment that removes or leaves it, and the residual risk. Close the section with an explicit statement
of what was checked for remaining instances, for example a grep for `catch (Exception` in
`app/src/main/java/com/zegoggles/smssync/activity/fragments/` with the result cited. For deviation 7, say
that the remaining Lava Flow doc residue is deliberately left for a follow-up and name the owner. This is
needed to keep it from being an unjustified out-of-scope deferral.

### Criterion 3 — Trade-off articulation

**Verdict:** satisfied

**Evidence present:** Architectural decisions are owned by DES-013, so the plan's own decisions are
procedural, and each one states the alternative and the reason it was rejected:
- Deviation 3: dexdump instead of `apkanalyzer` (JAXB failure) or `unzip -l` (lists only `classes*.dex`).
- Deviation 8: scratch copy instead of scratch branch ("Developer may not create branches"), with "better
  isolation and no change to git state" as the gain.
- Deviation 9: gate before commit, argued equivalent because the tree is byte-identical.
- The Commit Mechanism: snapshot-and-replay was dropped in favour of one increment per turn.
- I5.3: a version pin is "not pre-approved", so drift escalates instead of adding `constraints {}`.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none). One minor note: the Commit Mechanism does not state its cost,
which is nine Developer turns and orchestrator overhead against the single-turn alternative. This is not
counted as a recommendation.

### Criterion 4 — Citation depth

**Verdict:** satisfied_with_recommendation

**Evidence present:** The plan makes no canonical-source citations, so there are no unanchored
citations. Its internal citations are unusually precise. Almost every factual claim is anchored to
`file:line` at a named commit (for example `Preferences.java:179`, `preferences.xml:213`,
`build.gradle:24-30`), or to a named evidence file and probe command in the Assumption Ledger.

**Evidence missing:** There is no explicit statement that no canonical citations are applicable. Two
places could carry anchored citations: the characterization-test route (I6.4) and the test re-host (I1).

**Passage:** I1; I6.4; Test Strategy (lines 831–844).

**Recommendation / Remediation:** Either add the anchored citations proposed under Criteria 1 and 2
(Meszaros Ch. 26 and Ch. 16; Feathers Ch. 13; CWE-390/CWE-396; Brown et al. Ch. 5), or add one line to
Test Strategy: "No canonical-source citations are made. Design rationale is cited in DES-013."

### Criterion 5 — NFR and quality-attribute coverage

**Verdict:** satisfied

**Evidence present:** "Quality Attribute Coverage (ISO/IEC 25010)" (lines 807–829) names all eight
characteristics. Each gets a story-specific finding:
- Performance Efficiency: the onResume IPC is removed, along with 12 modules and about 965 mapping
  entries.
- Compatibility: the `<queries>` entries are removed, and `com.android.vending` remains only as a
  string constant.
- Usability: lint `ExtraTranslation` is avoided.
- Security: one permission, two queries, six components and one IPC client are removed, and verification
  metadata is untouched.
- Portability: the upgrade installs in place with `install -r`.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 6 — Tooling evidence

**Verdict:** satisfied

**Evidence present:** The Assumption Ledger cites measured signals, and they drive decisions:
- JaCoCo per-package LINE coverage (root 18/30 60.0%, `receiver` 29/58 50.0% with zero headroom, which
  becomes risk R2).
- Dependency-graph reachability (exactly 12 billing-only modules, which becomes `EXPECTED_REMOVED` in
  `deps_drift.py`).
- dexdump class counts (190 / 16).
- `@Test` count 697 and JUnit XML totals.
- Strings inventory of 235 lines across 22 files, with per-file `EXPECTED`.
- EOL and BOM probes, which drive the byte-preserving strings script.

The data is interpreted (risks, stop conditions, predicted root 58.6%), not just listed.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 7 — Sequencing rigor

**Verdict:** satisfied

**Evidence present:** Increments I0–I8 are explicitly ordered, one per turn, and the plan gives the
reason for each dependency:
- I1 runs the IC-3 isolated run "before the old host is deleted" (Test Strategy; R4 mitigation "I1
  isolated run before I2").
- I2.6 notes that the donation tests still pass against the undeclared `DonationActivity`, so tests stay
  green between I2 and I3.
- I3 deletes code, tests and `Consts.Billing` "together".
- AC-1 is asserted only from I6, because `build.gradle` contains "donation" until then (deviation 5).
- I6 line numbers are "shifted −1 by I5".
- I8 has an explicit internal order: edit → greps → final gate → evidence on the unchanged tree (B1).

Parallelism is deliberately excluded by the one-increment-per-turn mechanism (B2).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none). One minor note: the reason I4 (strings) must follow I3 is
implicit. The deleted donation classes reference those `R.string` ids, so removing the strings first
would break compilation. Stating this would make the ordering self-evident.

### Criterion 8 — Anti-AI-slop check

**Verdict:** satisfied

**Evidence present:** None of the nine doctrine anti-patterns appears at a level that reduces analytical
value:
- No hedging. Every step has a pass/fail condition and an exit code.
- No generic recommendations. Edits are given by symbol and line at `BASE`.
- No tooling substitution. Metrics drive stop conditions.
- No authority appeal.

The only out-of-scope item, deviation 7, comes with a reason (AC-11 names only `code-location.md`). A
senior architect would push back on the absence of canonical vocabulary (Criteria 1, 2 and 4), not on
the analytical substance. That is why this criterion passes while Criterion 2 is assessed on its own.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

## Overall Verdict

**Verdict:** FAIL

**Rationale:** Seven of eight criteria are satisfied. Criteria 1 and 4 carry recommendations, which is
within the limit of two. Criterion 2 (Anti-pattern recognition) is deficient. The plan modifies code
with visible canonical anti-patterns and neither names them nor states that it analysed them:
- the generic swallowed exception in `MainSettings.checkUserDonationStatus()` (CWE-390/CWE-396);
- the billing IPC in the `onResume()` lifecycle callback;
- the production-class test-fixture host behind I1 (Meszaros Fragile Test);
- the stale lint-baseline and doc residue (Lava Flow).

The highest-severity gap is the swallowed exception. The plan mentions it in one line and does not check
for remaining instances. The plan's operational depth is otherwise exemplary. Remediation is a single
"Anti-Pattern Analysis" section plus two short naming sentences (Criteria 1 and 4). The gate severity is
`warning`, so this FAIL is recorded and does not block implementation.

**Iteration:** 2

**Rework routing (FAIL only):** Routes to amp/agents/solution-architect at step `planning` for iteration 3. Cap is 3 reworks (4 total evaluations). On cap exhaustion, escalates via AskUserQuestion. This gate is warning severity, so the orchestrator may record the FAIL and proceed without rework.

## Phase Completion Report
---
story_id: "U-061"
phase: "plan-gate/expert-depth-check"
verdict: "FAIL"
artifact_path: "sdlc/artifacts/build/sprints/sprint-018/U-061/expert-depth-check.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
