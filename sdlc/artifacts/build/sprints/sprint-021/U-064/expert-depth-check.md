---
artifact_type: gate-result
gate: expert-depth-check
gate_id: "expert-depth-check"
checkpoint: plan-gate
pipeline: story-build
story_id: "U-064"
artifact_under_review: sdlc/artifacts/build/sprints/sprint-021/U-064/plan.md
artifact_reviewed: sdlc/artifacts/build/sprints/sprint-021/U-064/plan.md
producing_agent: amp/agents/solution-architect
verdict: PASS
severity: warning
reviewer_agent: amp/agents/solution-architect
reviewer_mode: separate_instance
agent: "Solution Architect (separate instance, expert-depth-check)"
iteration: 2
produced_at: 2026-11-21T02:55:36Z
timestamp: "2026-11-21T02:55:36Z"
recommendations_count: 0
criterion_results:
  - criterion: "Pattern grounding"
    verdict: satisfied
    deficit_present: false
  - criterion: "Anti-pattern recognition"
    verdict: satisfied
    deficit_present: false
  - criterion: "Trade-off articulation"
    verdict: satisfied
    deficit_present: false
  - criterion: "Citation depth"
    verdict: satisfied
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

# Expert Depth Check: U-064 plan.md (plan-gate, iteration 2)

Scope of the review: the revised `plan.md` (frontmatter `iteration: 2`, timestamp `2026-11-21T02:54:05Z`),
read as a whole against the rubric `registries/pipelines/story-build/gates/expert-depth-check.md` (plugin
cache 1.7.2) and the doctrine `knowledge/standards/expert-exceeding-depth.md`, Self-Check Criteria 1-8.
Depth is judged relative to a low-complexity, tests-only story.

Iteration 1 result: FAIL at warning severity. Criterion 5 was deficient, because section 9 covered four of
the eight ISO/IEC 25010 characteristics. Criteria 1 and 2 had recommendations. Section 0 of the revision
(plan lines 26-51) maps each finding to its change. This review checks each change in the plan body, not
just the section 0 claim.

Grounding spot-checks by this reviewer at HEAD `de03e99b` (read-only), in addition to those of iteration 1,
which still hold:
- `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java:216` declares
  `static boolean isIpOrBlankHostname(String hostname)` (package-private). The test class is in a different
  package, so section 3's "reflection seam for the package-private guard" (plan line 173) is accurate.
  `setSniViaSSLParameters` is `public static` (`:238`), which matches the direct call in AC-2.
- The story file's `IC-8: (evidence hygiene)` criterion is at line 83. This matches I8's stated expected
  self-match (plan line 699).

### Criterion 1 — Pattern grounding

**Verdict:** satisfied

**Evidence present:** Section 3 now opens with an explicit statement (plan lines 172-177): "No architectural
pattern is introduced; the story adds tests and changes no production design". It then names the test
patterns with chapter anchors:
- characterisation testing and the reflection seam (Feathers, *Working Effectively with Legacy Code*,
  ch. 13 and ch. 4 "The Seam Model");
- a Mock Object Test Double for `SSLSocket` (Meszaros, *xUnit Test Patterns*, ch. 23);
- the loopback server as a fixture;
- mutation testing as the adequacy check.

The iteration-1 recommendation has been taken up as written.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 2 — Anti-pattern recognition

**Verdict:** satisfied

**Evidence present:** Section 6 now holds a "Test smells guarded against" paragraph (plan lines 760-769). It
cites Meszaros, *xUnit Test Patterns*, ch. 16 "Behavior Smells" and ch. 15 "Code Smells", and ties each
named smell to a concrete mitigation in the plan:
- Erratic Test, in its Interacting Tests and Nondeterministic Test forms: the three I5 runs, no sleeps, no
  name resolution, guards A1-G1 to A1-G3 and row S10.
- Assertion Roulette: the per-host and per-name messages.
- Fragile Test: AC-1 asserts the exception class, not the provider's message wording.
- Test Code Duplication: a separate class, and no existing test repeated.

The paragraph also states that "The production code under test is not changed, so no production
anti-pattern is in scope". This is the explicit statement of scope that the rubric accepts. The stale
comments SC-1 and SC-2 remain recorded and are deliberately not edited (plan lines 771-772).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 3 — Trade-off articulation

**Verdict:** satisfied

**Evidence present:** The story makes no architectural decision, so this criterion is assessed PASS with
that note. The local choices found satisfied in iteration 1 are unchanged and still state their reason and
alternative: fully qualified names to keep a one-hunk, additions-only diff; per-name catching; the socket
created outside the `try`; `if (false)` for M-N1. The revision adds choices that are reasoned the same way:
- Row S3 (plan line 714) distinguishes the AC-2 substitution, which IC-7 permits, from the AC-3
  substitution. The AC-3 substitution is a recorded deviation for the orchestrator to accept.
- Row S13 (plan line 724) rejects a re-run or a reworded expectation, and cites IC-7's text as the reason.
- I8's count-only output (`grep -l`, never `-n`) is chosen so that a scan cannot itself leak the value it
  looks for (plan lines 651-654).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 4 — Citation depth

**Verdict:** satisfied

**Evidence present:** All canonical-source citations are anchored:
- RFC 5280 section 4.1.2.5 (unchanged);
- Feathers ch. 13 and ch. 4 (plan lines 173-175);
- Meszaros ch. 23 (plan line 176);
- Meszaros ch. 16 and ch. 15 (plan lines 760-761).

Internal citations remain line-anchored. Section 8 adds CR-013 `:317` and `:46`, CR-012 `:247` and `:292`,
the contract `:191-192` and `:196`, and the existing BUG-015 tests at `DefaultTrustedSocketFactorySniTest.java:180`,
`:195`, `:209`, `:224` and `:238`. No bare author or "best practice" citations.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 5 — NFR and quality-attribute coverage

**Verdict:** satisfied

**Evidence present:** Section 9 (plan lines 843-857) opens with "All eight characteristics were evaluated;
those that do not apply are marked so", and now has a row for each characteristic:
- Functional suitability: AC-1 against contract `:96` and `:197`; AC-2 and AC-3 against CR-013 S5;
  mutation discrimination; completeness reduced by DP-1.
- Performance efficiency: not applicable to production, with a reason. The test cost is stated (one
  loopback handshake, waits bounded at 10 s, no sleeps).
- Compatibility: co-existence with `DefaultTrustedSocketFactoryJdkProviderTest` in one JVM, checked via the
  guards and row S10. The plan states that interoperability with a real IMAP server is not shown.
- Usability: not applicable, with a reason (no user-facing change), plus the indirect protection AC-3 gives.
- Security: unchanged from iteration 1.
- Reliability: unchanged from iteration 1.
- Maintainability: two rows (testability and modifiability; analysability), unchanged from iteration 1.
- Portability: unchanged from iteration 1.

Each finding is specific to this story, not generic. The iteration-1 deficit is resolved.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 6 — Tooling evidence

**Verdict:** satisfied

**Evidence present:** These are unchanged from iteration 1 and still present:
- the authoritative `@Test` count (L1);
- blob hashes (L2, I4 step 6);
- the JaCoCo filter and floor (L20);
- verbatim `jshell` output on the build JDK (section 1.1);
- per-run expected pass and fail counts under mutation (I4);
- the reused JUnit XML tools (L22).

The revision adds I8's grep scans with exit codes stated for each command (0 hit, 1 clean, 2 error; plan
lines 696-698). Section 10's Verdict also records that the I8 commands' exit-code behaviour was tried in a
scratch folder outside the repository, and that a read-only pre-run over the workspace gave no match (plan
lines 906-908).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 7 — Sequencing rigor

**Verdict:** satisfied

**Evidence present:** There is no regression from iteration 1. The ordering I0 to I7 and its rationale are
unchanged: baseline before edit, I2 and I3 as the unmutated baselines for M-N1 and M-N2, M-N2 only after
M-N1's `restored` line, and the pre-checks at I5 and I6. The new step I8 is placed explicitly and its
dependency is stated:
- its heading reads "last step; after the log and every evidence file are final" (plan line 649);
- I6 step 4 hands off to it (plan line 621);
- I7 is "the last write to the log before I8" (plan line 632);
- re-run rules after a fix: "I8 is run again in full" (plan lines 635, 706).

The expected count of Gradle invocations is still 9 (plan line 179). This is consistent, because I8 runs no
Gradle. Serial execution remains justified by the shared `$R` results directory and the binding
foreground-only constraints (section 7, items 1-3).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 8 — Anti-AI-slop check

**Verdict:** satisfied

**Evidence present:** There is no regression. The new rows S13 to S15 (plan lines 724-726) follow the same
observation-and-action form as S1 to S12, with specific triggers:
- S13: a socket timeout, an `SSLException` with another cause, a differing failure type;
- S14: exit 2, or `value-unavailable`;
- S15: a match, with Read-tool inspection and regeneration through `sanitize.py`.

Each has a definite action and no hedging. The added text in section 8 is analytical, not padding. It
passes on to U-065 a concrete constraint: AC-2 (iii) is stricter than S5, and the plan names how that could
mislead if U-065 calls `setSSLParameters` for protocols (plan lines 832-839). None of the nine doctrine
anti-patterns is present. The added length serves the evidence-hygiene protocol that another gate required,
not depth theatre.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

## Overall Verdict

**Verdict:** PASS

**Rationale:** The revision resolves the iteration-1 deficit. Section 9 now evaluates all eight ISO/IEC
25010 characteristics with findings specific to this story, and marks Performance Efficiency and Usability
as not applicable with reasons. The revision also takes up both recommendations:
- Section 3 states that no architectural pattern applies and names the test patterns with chapter anchors
  (Feathers ch. 4 and ch. 13; Meszaros ch. 23).
- Section 6 names the guarded test smells with their mitigations (Meszaros ch. 15 and ch. 16).

None of the criteria satisfied in iteration 1 has regressed. The new step I8 is explicitly sequenced last,
its exit codes are stated, and rows S13 to S15 keep the decision table's specificity. The plan's code claims
re-checked at `de03e99b` hold: the guard is package-private, `setSniViaSSLParameters` is public static, and
the story's IC-8 criterion is at line 83. All eight criteria are satisfied, and `recommendations_count` is 0.

**Iteration:** 2

**Rework routing (FAIL only):** Not applicable (PASS).
