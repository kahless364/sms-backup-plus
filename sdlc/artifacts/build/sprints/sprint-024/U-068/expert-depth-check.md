---
artifact_type: gate-result
gate: expert-depth-check
gate_id: "expert-depth-check"
checkpoint: plan-gate
pipeline: story-build
story_id: "U-068"
artifact_under_review: sdlc/artifacts/build/sprints/sprint-024/U-068/plan.md
artifact_reviewed: sdlc/artifacts/build/sprints/sprint-024/U-068/plan.md
producing_agent: amp/agents/solution-architect
verdict: PASS
severity: warning
agent: "Solution Architect"
reviewer_agent: amp/agents/solution-architect
reviewer_mode: separate_instance
iteration: 2
timestamp: "2026-11-21T11:21:17Z"
produced_at: 2026-11-21T11:21:17Z
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

# Gate Report: expert-depth-check — U-068

## Gate: expert-depth-check @ plan-gate

## Verdict: PASS (severity warning, observe-only), no recommendations

## History

- **Iteration 1** (2026-11-21T10:45:24Z, plan at `0d5755c4`): PASS with 2 recommendations. Criterion 3: trade-offs
  without gained / gives way / rejected alternative. Criterion 4: toolchain claims without vendor sources; R8's
  implicit `SDK_INT` assumption missing. Plus an edge-case list.
- **Iteration 2** (this report, plan at `c12a2788`): both recommendations and every edge case taken up.

## Summary

Iteration 2 adds the three things iteration 1 asked for:
- **Section 2.4** (plan.md:249-255): a four-part trade-off table.
- **Section 2.5** (plan.md:257-294): one sourced line per tool. R8's `SDK_INT` folding is kept as a
  `needs_check` (N11, plan.md:147), not stated as fact, and the developer records it at I5 step 7
  (plan.md:1103).
- **Section 2.6** (plan.md:296-305): what each before/after comparison can and cannot show.

The edge cases are written into the steps and rows the developer reads:
- the device below the floor (plan.md:244-247);
- D24's measured `so=0` despite `ndk.abiFilters` (plan.md:1329);
- D26's `$-CC` companions (plan.md:1331);
- D28's library-only qualifiers and the unchanged multidex (plan.md:1333);
- the transient `ObsoleteSdkInt` finding (plan.md:286-288).

The orchestrator-ruled test C-1 (ruling P-5) was checked against all eight criteria and changes none of them. It is
placed in I5 step 1a with four stated reasons (plan.md:1363-1375). It has its own mutant, M-C1, whose predicted
failure is the second assertion (plan.md:1406-1410). Its coverage effect is re-derived from the JaCoCo report: 18/71
with 3 spare lines, against 0 without it (plan.md:216, :221-227). Its failure mode has its own row, D35b.

All eight criteria are `satisfied`.

This review checked, read-only:
- **HEAD.** HEAD is `c12a2788`, which matches the plan's iteration 2 `base_commit`.
- **The new sections.** Sections 2.4 to 2.6, I5 steps 1a to 7, rows D19 and D24 to D39c, and 6.1 were read in
  full.

## Findings

### Criterion 1 — Pattern grounding

**Verdict:** satisfied

**Evidence present:** Section 2.0 is unchanged (plan.md:152-180). It names Remove Dead Code (Fowler ch. 8), the
seam / Subclass and Override Method (Feathers ch. 4, 13, 25), boundary-value analysis (Myers ch. 4), Test Code
Duplication (Meszaros ch. 15), mutation analysis (DeMillo et al. 1978), the fitness-function ratchet
(Ford/Parsons/Kua ch. 2) and small reversible increments (Humble/Farley ch. 1, 10). Each is applied to a named site.
C-1 is also a characterisation test of `SmsReceiver.enable` and `disable`, pinned by M-C1 under the same
mutation-analysis principle.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 2 — Anti-pattern recognition

**Verdict:** satisfied

**Evidence present:** Unchanged from iteration 1:
- Test Code Duplication in the four LOLLIPOP tests (plan.md:167-169);
- the null-only SDK 23 test;
- comments describing a deleted branch (E5.13 to E5.18);
- the stale `scheduler` figure in a build comment (L9).

Iteration 2 also names "the 21-versus-24 ambiguity" that keeping or re-annotating the guards would preserve
(plan.md:254). It turns the zero-headroom ratchet that iteration 1 flagged as P-5 into a ruled fix, C-1 (2.2).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 3 — Trade-off articulation

**Verdict:** satisfied

**Evidence present:** Section 2.4 (plan.md:251-255) gives all four elements (gained, gives way, not chosen, why) for
each of the three decisions iteration 1 named:
- **Commits.**
  - Gained: bisectable and revertible increments.
  - Gives way: seven interleaved commits; D51 fix commits; and a newest-first rollback order. Reverting I4 while
    keeping I5 is the one order CNTR-MODERNIZATION-004 v3's non-breaking claim excludes. This goes beyond what
    iteration 1 asked.
  - Not chosen, and why: a squashed commit loses attribution and would mix the floor with the deletions, which
    CR-015 Amendment 1 note 4 keeps apart.
- **Guards.**
  - Not chosen: keeping them, or `@RequiresApi` / `@ChecksSdkIntAtLeast`.
  - Why: either keeps lint findings and the ambiguity alive, and keeping them contradicts REQ-015 AC-6.
  - Gives way: nothing on a supported level, and the release build may not change at all (N11).
- **Baseline.**
  - Not chosen: (a) committing the regenerated file, which "admits every NOT-ADDED finding (the 39 current errors)";
    (b) regenerating in place, which risks a regenerated real file if a session is lost.
  - Gives way: the cost is stated (a second configuration, two lint runs, STALE-OTHER entries kept on purpose).

The trade-offs of the plan's own smaller choices are stated where they are made:
- C-1 placement (6.1, four reasons, with I3 and I4 rejected);
- declining contract-verification O-1, with a reason (plan.md:73);
- `git -c core.autocrlf=false archive` (B-2, D39b).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 4 — Citation depth

**Verdict:** satisfied

**Evidence present:** Section 2.5 names the vendor source for each toolchain claim at document-and-section level:
- **AGP:** the DSL reference `JniLibsPackaging`, and Android Developers, `<application>` element,
  `android:extractNativeLibs`.
- **AAPT2:** "AAPT2", link options.
- **D8:** Android Developers, "Java 8+ language features and APIs".
- **Lint:** the lint check reference, `ObsoleteSdkInt` and `NewApi`.
- **Robolectric:** "Configuring Robolectric", dependency resolution, with `robolectric.offline` and
  `robolectric.dependency.dir`.

The plan is open that these were not re-read in this session (plan.md:259-260). Each claim the evidence depends on
is therefore also a `needs_check` (N3, N4, N11) or measured by a step. That is the right pairing: a citation for the
mechanism, a measurement for the outcome.

R8's assumption is handled exactly as recommended. It is not stated as fact. The developer quotes the release-note
line with its R8 version, or records "not confirmed". No decision row depends on the outcome, because the equivalence
proof is carried by the debug bytecode and the tests either way (plan.md:147, :278-283, :1103).

The literature citations stay anchored to chapters (2.0). The project citations stay anchored to line ranges.

**Evidence missing:** (none at a level that affects the verdict). One accuracy note for the developer, which needs
no plan change:
- 2.5 item 3 says lambdas are "native only from 26". In practice D8 desugars lambdas at every min-api, because ART
  provides no `LambdaMetafactory`. The conclusion the plan draws is unaffected: the lambda handling does not change
  between 21 and 24.

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 5 — NFR and quality-attribute coverage

**Verdict:** satisfied

**Evidence present:** Section 11 (plan.md:1637 onward) evaluates all eight ISO/IEC 25010 characteristics by name.
Usability is marked not applicable, with the reason. Iteration 2 updates two rows:
- **Maintainability** records the `compat` margin of 3 spare lines with C-1, 0 without.
- **Reliability** adds the newest-first rollback order (contract-verification O-4).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 6 — Tooling evidence

**Verdict:** satisfied

**Evidence present:** Iteration 1's tooling evidence (greps, blobs, JaCoCo, lint census, Robolectric cache census)
remains. Iteration 2 adds new measured signals, each interpreted:
- **Per-line JaCoCo for C-1:** `enable` ci 6/6/1, `disable` ci 0, which gives 18/71 (L21, plan.md:148, :216).
- **CRLF counts of the `git archive` output:** 260 with the default, 0 with `-c core.autocrlf=false` (L3, B-2).
- **The AC-8 hit count corrected** to 29 and split by folder (L5).
- **Every line and hunk count re-derived** (plan.md:81-95).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 7 — Sequencing rigor

**Verdict:** satisfied

**Evidence present:** The increment order and its reasons are unchanged (I1 to I7). C-1 is sequenced explicitly
inside I5, in step 1a:
- it runs before the E5 edits;
- it runs "while `app/src/main` still equals HEAD", so that M-C1 meets the mutation protocol's precondition and
  its recovery command cannot wipe E5 edits (plan.md:1097, :1371-1372);
- "Only then go to step 2".

The plan rejects I3 and I4 as homes for C-1, with reasons (plan.md:1369-1370). The running count is stated per
increment: `C0 + 2` after I2 to I4, `C0 + 3` from I5 on.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 8 — Anti-AI-slop check

**Verdict:** satisfied

**Evidence present:**
- **Section 2.6 states the limits of the evidence**, which senior reviewers usually have to ask for:
  - the release-build comparisons cannot show equivalence;
  - the bytecode diff cannot show the behaviour of untested code beyond the identity of the remaining
    instructions;
  - the tests do not cover the pre-Q branch of `SmsDefaultRoleHelper.startRestore`;
  - IC-3 shows nothing at API 24.
- **N11 is not used as a crutch.** No row depends on it, and the plan says so.
- **The C-1 coverage claim is grounded in the measured per-line report**, not in a guess.
- **The predicted M-C1 failure** names the assertion and the Truth message.

None of the nine doctrine anti-patterns is present.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

## Edge-case review (iteration 1's list, re-checked)

| Edge case | Iteration 2 coverage |
|-----------|----------------------|
| Device below the new floor | 2.3 (plan.md:244-247): `INSTALL_FAILED_OLDER_SDK`, the installed build keeps running, the reverse `install -r` rollback is allowed |
| `extractNativeLibs` / floor-derived attributes | 2.5 item 1 and D24: the argument rests on the measured `so=0` despite `ndk.abiFilters`; any other derived attribute stops the story |
| Multidex and desugaring | 2.5 item 3, D26 (`$-CC` companions as a known, explainable cause), D28 (dex count only; multidex unchanged) |
| R8 API-level optimisation | 2.5 item 4, N11, 2.6, I5 step 7 |
| Resource qualifiers | 2.5 item 2 and D28: the app's own `res/` has only `-v26`, so effects come from library resources |
| Lint `ObsoleteSdkInt` / `NewApi` | 2.5 item 5, with the transient I4-to-I5 finding and why it is not observed |
| Robolectric SDK jar | 2.5 item 6; D19 (a) to (e) under ruling P-1, now including the `.pom` and its `.sha512` |
| Windows line endings | L3, with the `git archive` CRLF fix (B-2, D39b) |
| What each comparison can and cannot show | 2.6 |

**Consistency note (for the developer, outside the eight criteria):** I5 step 3 (plan.md:1099) still lists "the
four `SmsReceiverTest` cases" among the tests that must pass. After step 1a the class has five. The same step
expects `tests=C0+3` and 1a expects `cases=5`, so no check is weakened, but the phrase should read "five".

## Overall Verdict

**Verdict:** PASS

**Rationale:** Iteration 2 closes both iteration 1 recommendations:
- **Criterion 3:** a four-part trade-off table for commits, guard deletion and the baseline method. It includes
  the rollback-order constraint the contract imposes.
- **Criterion 4:** sourced toolchain lines, with R8's assumption kept as a `needs_check` that no decision depends
  on.

The added section on the limits of each comparison makes the evidence design explicit. C-1 is correctly placed,
mutated and accounted for, and changes no criterion. All eight criteria are satisfied, with no recommendations.

**Iteration:** 2

**Rework routing (FAIL only):** Not applicable (PASS).

## Phase Completion Report
---
story_id: "U-068"
phase: "plan-gate/expert-depth-check"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-024/U-068/expert-depth-check.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
