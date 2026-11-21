---
artifact_type: gate-result
gate: expert-depth-check
gate_id: "expert-depth-check"
checkpoint: plan-gate
pipeline: story-build
story_id: "U-066"
artifact_under_review: sdlc/artifacts/build/sprints/sprint-022/U-066/plan.md
artifact_reviewed: sdlc/artifacts/build/sprints/sprint-022/U-066/plan.md
producing_agent: amp/agents/solution-architect
verdict: FAIL
severity: warning
agent: "Solution Architect"
reviewer_agent: amp/agents/solution-architect
reviewer_mode: separate_instance
iteration: 1
timestamp: "2026-11-21T07:40:33Z"
produced_at: 2026-11-21T07:40:33Z
recommendations_count: 0
criterion_results:
  - criterion: "Pattern grounding"
    verdict: deficient
    deficit_present: true
  - criterion: "Anti-pattern recognition"
    verdict: satisfied_with_recommendation
    deficit_present: false
  - criterion: "Trade-off articulation"
    verdict: satisfied_with_recommendation
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

# Gate Report: expert-depth-check — U-066

## Gate: expert-depth-check @ plan-gate

## Verdict: FAIL (severity warning, observe-only)

## Summary

The plan has a lot of evidence behind it. Every line it cites in
`k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` matches HEAD (blob `43e5a78e`,
319 lines; `:34`, `:37`, `:64`, `:110`, `:124-125`, `:134`, `:173`, `:227`, `:237`, `:240`, checked by `grep -n` in
this review). The plan ran a plain-JDK program during planning (section 1.1), derives each mutation from the planned
tests, and has a decision row for every probe outcome. It fails this gate on one criterion. Its security design
decisions are made without naming any canonical pattern or principle, and it does not say why none applies
(Criterion 1). There are also three smaller recommendations on Criteria 2, 3 and 4. Under the rubric, one
`deficient` criterion is enough for FAIL. More than two `satisfied_with_recommendation` verdicts would also cause
FAIL on its own.

Paths in this report are repository-relative, as the delegation requires. The rubric's `artifact_under_review` asks
for an absolute path, but the repository-relative form is used here.

## Findings

### Criterion 1 — Pattern grounding

**Verdict:** deficient

**Evidence present:** The plan reuses patterns from the code itself. `ciphersToEnable` is "modelled on
`protocolsToEnable`" (section 2.1, line 129). The test literals follow "the repository's pattern
(`ALLOWED_PROTOCOLS_BASELINE` is copied per class)" (section 3.2, line 352). The Hardening class Javadoc calls the
tests "Characterisation tests" (line 369). None of these is tied to a canonical catalog.

**Evidence missing:** Each of the decisions this gate was asked to judge has a canonical name, and the plan names
none of them:
- **Allow-list over deny-list** (section 2.1; ruled as DP-2). This is the positive security model, and the
  fail-safe defaults principle: Saltzer and Schroeder, "The Protection of Information in Computer Systems" (1975),
  section I.A.3, design principle (b), "Fail-safe defaults: base access decisions on permission rather than
  exclusion". The deny-list being replaced is the weakness class CWE-184, "Incomplete List of Disallowed Inputs".
- **Fail closed in the failed-initialisation branch** (section 2.2). This is the same Saltzer and Schroeder
  principle. The plan names only the weakness, CWE-636, not the principle that answers it.
- **Removing the blocked list outright** (section 2.1, lines 136-140). This is Fowler's "Remove Dead Code"
  refactoring (*Refactoring*, 2nd ed., Ch. 8). It is also the reason given against a defence-in-depth second list:
  a list that nothing can reach adds no layer.
- **The test design.** The tests are characterisation tests (Feathers, *Working Effectively with Legacy Code*,
  Ch. 13). The reflective test seams are Feathers' object and link seams (Ch. 4). The mutations declared in advance
  as undetectable (section 2.5 item 3; the M-X1 (b) and (e) rows) are equivalent or undetectable mutants in the
  mutation-testing sense.

The plan does not say that no canonical pattern applies. The rubric's FAIL condition therefore applies: "the
subject clearly involves canonical patterns but they are not named".

**Passage:** Section 2.1 (lines 123-145), section 2.2 (lines 147-154), section 2.5 item 3 (lines 186-191), section
3.2 (lines 351-353).

**Recommendation / Remediation:** Add a "Patterns applied" paragraph to section 2, or a line per decision in 2.1,
2.2 and 2.5:
1. Allow-list: Saltzer and Schroeder 1975, section I.A.3 (b), fail-safe defaults. The replaced deny-list is CWE-184.
   Say that an unknown future suite staying off (R9) is the cost this principle accepts.
2. Null branch: the same principle, answering CWE-636.
3. List removal: Fowler, *Refactoring* 2nd ed., Ch. 8, "Remove Dead Code". Say that a second, unreachable list is
   not defence in depth, because no filter output can depend on it, and no test can detect it being emptied.
4. Tests: Feathers Ch. 13 (characterisation tests) and Ch. 4 (seams). Name M-X4 (i) on Conscrypt and M-X1 (b) and
   (e) on SunJSSE as undetectable-mutant declarations, and give the provider fact that makes each one undetectable.

### Criterion 2 — Anti-pattern recognition

**Verdict:** satisfied_with_recommendation

**Evidence present:** Section 2.2 (lines 151-153) names the fail-open branch in the current code as CWE-636, "Not
Failing Securely". The code confirms it: `if (ciphers != null)` at `:237` leaves the platform's suites in place.
Section 2.1 (lines 136-138) identifies the dead-list hazard ("a blocked list would be dead … no test could fail if
it were emptied"). Section 2.6 states the mutable shared arrays (U-065 F-4 and N1) and hands them to CR-009 with a
reason. The plan treats the old reflection-coupled sandbox guard (DHE clause) as an inherited defect: with the DHE
clause left in, "would fail every run" (line 966).

**Evidence missing:** The deny-list shape being removed is not named as an anti-pattern (CWE-184, "Incomplete List
of Disallowed Inputs"). Dead code is described, but not named as the canonical smell ("Dead Code"; Fowler,
*Refactoring* 2nd ed., Ch. 3). The old guard's coupling of a sandbox-isolation check to the production cipher list
is not named either. It is a test assertion that depends on incidental production data, an instance of Meszaros'
"Fragile Test" smell (*xUnit Test Patterns*, Ch. 16).

**Passage:** Section 2.1, lines 136-140. Section 3.3, guard Javadoc, lines 423-429.

**Recommendation / Remediation:** In section 2.1, name CWE-184 as the current state that the allow-list removes. In
the guard rework (sections 3.3 and 3.4, lines 423-429 and 953-966), name the old DHE clause as a Fragile Test
(Meszaros Ch. 16): it broke because production data it never governed changed.

### Criterion 3 — Trade-off articulation

**Verdict:** satisfied_with_recommendation

**Evidence present:** Most decisions give a reason and the alternative they reject.
- One ordered constant (line 125-128). Alternative: filtering after `reorder` with a second preferred-order list.
  Why it was not chosen: "no added meaning". Gain: one snapshot test and one form test.
- Removing the lists (lines 136-140). The alternative and the reason against it are both given.
- Default-enabled, not supported (lines 130-132). Reason: C1 and point 9, plus the measured JDK throw on an
  unsupported name (L11). Cost: R5.
- Empty result (section 2.3). Fallback is rejected. The reason for recording rather than asserting the platform
  outcome is that SunJSSE and Conscrypt differ (L11), and CR-014 Part D says "Not stated".
- Fail closed (section 2.2). Gain and cost: no socket is returned.
- Guard rework (lines 423-429). Reason: the cached lists no longer differ by provider. The rework is proven by G-1
  and G-2.
- Undetectable mutations (section 2.5 item 3). The plan contrasts its expectations with the story's table and
  gives the provider-fixed fact that explains the difference.

**Evidence missing:** For several decisions the four elements are scattered across sections, or one is missing.
- **One constant:** the cost is not stated. Set and order cannot vary independently, so the factory can never
  express "allowed but not preferred", and any reorder changes H4 and every exact-list literal in the same step.
- **Allow-list:** the plan sends the reason the deny-list lost to the ruling ("Not re-opened", line 41; "M-X3 not
  applicable … ruled (DP-2)", line 1814). The cost appears separately in R9, the Compatibility row of section 11,
  and AC-13 (e). A reader cannot see gain, cost, alternative and reason in one place.
- **Guard rework:** the alternative of a different provider-distinguishing signal is not considered. Nor is the
  cost of checking the live `SSLContext` provider, which no longer observes the factory's static cache.

**Passage:** Section 2.1 (lines 125-140). Section 3.3 guard Javadoc (lines 423-429).

**Recommendation / Remediation:** Give each decision in section 2 one four-part block: gained, gives way,
alternative not taken, why. Example for the constant: "Gained: one source of truth, one snapshot test (H4) and one
form test (H8). Gives way: set and order are coupled, so 'allowed but not preferred' cannot be expressed, and a
reorder is a contract-visible edit under CR-014 Part E. Alternative: filter with `reorder` against an allowed set
and a separate preferred order. Not taken: the second list carries no meaning under DP-4." Write a matching block
for the guard rework and the allow-list, quoting DP-2's ruling rationale from the story rather than only citing it.

### Criterion 4 — Citation depth

**Verdict:** satisfied_with_recommendation

**Evidence present:** The plan makes two canonical citations, and both are anchored: "RFC 9325 sections 4.1, 4.2"
(section 11, line 2172) and "CWE-636, 'Not Failing Securely'" (line 152). The anchors are correct. RFC 9325 section
4.1 is the general cipher-suite guidance and section 4.2 covers cipher suites for TLS 1.2. CWE-636's title is "Not
Failing Securely ('Failing Open')". The rate is 100%, above the 80% threshold.

**Evidence missing:**
- **TLS 1.3 suites:** RFC 9325 section 4.3 (cipher suites for TLS 1.3) is not cited, although the story's
  references cite it (story line 288). Nor is RFC 8446, which defines the three TLS 1.3 suites in Appendix B.4 and
  makes `TLS_AES_128_GCM_SHA256` mandatory to implement in section 9.1.
- **Signalling value:** `TLS_EMPTY_RENEGOTIATION_INFO_SCSV` stays on the list without citing RFC 5746 section 3.3,
  which defines it as a signalling value and not a cipher.
- **ChaCha20 TLS 1.2 suites:** the story's Scope Concern 4 (story line 272) says they go beyond RFC 9325 section
  4.2's four AES-GCM suites. The plan does not carry that point into its design. The constant's Javadoc (lines
  222-225) gives an order, "strongest first and RSA before ECDSA", backed only by "the order used before U-066".
- **Placement:** RFC 9325 appears only in the quality table (section 11), not where the allowed list is decided
  (sections 2.1 and 2.7, E2).

**Passage:** Section 2.1 (lines 123-132). Section 2.7, E2 Javadoc (lines 218-230). Section 11, Security row (line
2172).

**Recommendation / Remediation:** In section 2.1, cite each part of L-A to its source:
- the TLS 1.3 three to RFC 8446 Appendix B.4, plus section 9.1 for the mandatory suite, and RFC 9325 section 4.3;
- the four AES-GCM TLS 1.2 suites to RFC 9325 section 4.2;
- the two ChaCha20 suites to RFC 7905 section 2, recorded as the product owner's addition beyond RFC 9325 section
  4.2 (story Scope Concern 4);
- the signalling value to RFC 5746 section 3.3.

Say that the TLS 1.2 order is inherited precedent, not a recommendation from RFC 9325.

### Criterion 5 — NFR and quality-attribute coverage

**Verdict:** satisfied

**Evidence present:** Section 11 (lines 2164-2175) evaluates all eight ISO/IEC 25010 characteristics by name.
Usability is marked "Not applicable (no UI)". Each of the others has a finding specific to this story: the cost
of the ten-step loop for Performance Efficiency, the Gmail and TLS 1.2-only server costs for Compatibility, and
for Portability, two emulator images measured with other devices "inferred, not measured".

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 6 — Tooling evidence

**Verdict:** satisfied

**Evidence present:**
- Scope is verified by grep: L5 and L6, 30 occurrences in 6 files, cited line by line.
- Coverage tooling is interpreted: JaCoCo's class roots exclude the vendored file, and the `mail*` floor is 0.70
  (L12). The plan therefore uses mutation evidence for the production change rather than claiming coverage.
- Fourteen mutations and two guard checks are planned, with expected failing tests and failure types (section I7).
- Provider probe outputs from earlier stories are cited by file and line (L9, L10).
- A deterministic JDK program was run during planning, and its output is shown verbatim (section 1.1).
- The JUnit XML totals and skipped-count baseline are measured (L13).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 7 — Sequencing rigor

**Verdict:** satisfied

**Evidence present:** Steps I0 to I11 state their dependencies and reasons for their order:
- I0: "no edit before this step passes".
- I2: "before any other test edit or any production edit". The expected lists depend on its outcomes.
- I3 "starts only when every I2 key has a row that lets it continue".
- I4 (red) comes before I5 (production) to prove the tests detect the change. D21 says "a test listed as failing
  passes … it does not test the change".
- I7 runs "one at a time", with write-ahead and restore.
- I11 is "last step, after every file is final".

The plan rules out parallel work and gives the reasons: foreground only, endpoint security, mutations of a shared
uncommitted file (section 10, items 1 to 3; I7). I8 explains why no separate determinism run is needed.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 8 — Anti-AI-slop check

**Verdict:** satisfied

**Evidence present:** A senior reviewer would not push back on the analysis. The claims are measured, not
asserted:
- SunJSSE silently negotiates TLS 1.2 when TLS 1.3 is enabled with no TLS 1.3 suite (L11, line 110). This is the
  hazard behind R1 and M-X4 (i).
- SunJSSE refuses `TLS_RSA_*` whatever is enabled (line 114). This is why AC-6 is asserted under Conscrypt only.
- The two places where the story's mutation table expects more than can fail are declared in advance, with the
  provider fact behind each (section 2.5 item 3; the M-X1 (b) and (e) rows).
- The empty-result behaviour is recorded per provider rather than pinned (section 2.3).

None of the nine doctrine anti-patterns occurs at a level that weakens the analysis. There is no maximalist
hedging, no generic recommendation and no ungrounded "best practice" claim. The out-of-scope items (section 8.4)
are each tied to an acceptance criterion or to CR-009. The missing canonical grounding is a real gap, but it is
recorded under Criterion 1 and not counted twice here.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

## Overall Verdict

**Verdict:** FAIL

**Rationale:** The plan is concrete, grounded in the code, and backed by measurement. Its line citations match
HEAD. Its riskiest claims, such as the silent TLS 1.2 fallback, the empty-array behaviour and the provider-fixed
TLS 1.3 suites, were measured or are put to a probe with a decision row for each outcome. Its mutation design
declares in advance what cannot be detected. It fails on Criterion 1. It makes security design choices (allow-list
over deny-list, fail closed, removing the blocked list, characterisation tests through reflective seams) without
naming the principle or pattern each one applies: Saltzer and Schroeder's fail-safe defaults, CWE-184 for the
replaced deny-list, Fowler's "Remove Dead Code", and Feathers Ch. 13 and Ch. 4. It also does not say that no
canonical pattern applies. Three recommendations come with it:
- Criterion 2: name CWE-184, and the old guard as a Fragile Test.
- Criterion 3: write four-part trade-off blocks, in particular the coupling cost of the single constant.
- Criterion 4: cite RFC 8446 Appendix B.4 and section 9.1, RFC 9325 section 4.3, RFC 5746 section 3.3 and RFC
  7905 at the point where the allowed list is decided, and record the ChaCha20 suites as going beyond RFC 9325
  section 4.2.

The four fixes are additions to the text. None changes a design decision, a test or a mutation row. This gate has
severity warning, so the verdict is recorded and does not block the story by itself.

**Iteration:** 1

**Rework routing (FAIL only):** Routes to amp/agents/solution-architect at step `planning` for iteration 2. Cap is
3 reworks (4 total evaluations). On cap exhaustion, escalates via AskUserQuestion.

## Phase Completion Report
---
story_id: "U-066"
phase: "plan-gate/expert-depth-check"
verdict: "FAIL"
artifact_path: "sdlc/artifacts/build/sprints/sprint-022/U-066/expert-depth-check.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
