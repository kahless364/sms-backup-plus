---
artifact_type: gate-result
gate: expert-depth-check
checkpoint: synthesis-gate
pipeline: assessment-execution
artifact_under_review: sdlc/analysis/20260529-modernization/reports/modernization-plan.md
producing_agent: amp/agents/technical-writer
verdict: PASS
severity: warning
reviewer_agent: amp/agents/solution-architect
reviewer_mode: routine
iteration: 1
produced_at: 2026-05-29T00:00:00Z
recommendations_count: 2
criterion_results:
  - criterion: "Pattern grounding"
    verdict: satisfied
    deficit_present: false
  - criterion: "Anti-pattern recognition"
    verdict: satisfied
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

# expert-depth-check — synthesis-gate

**Assessment:** 20260529-modernization
**Artifact under review:** `sdlc/analysis/20260529-modernization/reports/modernization-plan.md`
**Doctrine:** `knowledge/standards/expert-exceeding-depth.md`
**Reviewer mode:** routine (reviewer = solution-architect; producer = technical-writer; no role conflict, per CNTR-FRAMEWORK-000003 R1)

The deliverable was read in full. Its three canonical-ID source outputs (`outputs/sms-backup-plus/modernization/assessment.md`, `gaps.md`, `candidates.md`) were read in full as corroborating context to confirm that the synthesis preserves — rather than dilutes — the depth present upstream.

---

### Criterion 1 — Pattern grounding

**Verdict:** satisfied

**Evidence present:** The deliverable names canonical patterns by catalog and applies each to the specific subject, not as ornament. Modernization catalog: Branch-by-Abstraction and Strangler Fig are distinguished and the correct one selected for an on-device single-process app (Strangler Candidate Summary, lines 199-216; "the highest-leverage finding: CAND-001 (Scheduler) is already 50% built. The `Driver` Strategy at `BackupJobs.java:66-72` is exactly the 'first adapter' slot that branch-by-abstraction requires"). Hexagonal Ports & Adapters and the Anti-Corruption Layer are named and tied to concrete boundaries (`STRUCT-001`, lines 127-131; Target Architecture lines 236-243; ADR-004 line 392). GoF Strategy is identified in the existing `Driver` indirection. Feathers' "Introduce Instance Delegator" (Ch. 25) is cited for the Otto→Flow seam (line 137). Feathers characterization tests (Ch. 2) gate every seam swap (line 189). The protected domain core is explicitly named (immutable State machine, `DataType` type-object, typed exception hierarchy, Preferences facade) as a deliberate preserve-verbatim decision (lines 216, 414).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

---

### Criterion 2 — Anti-pattern recognition

**Verdict:** satisfied

**Evidence present:** Anti-patterns are named from canonical catalogs and located in the subject. God-Activity / SRP violation on `MainActivity` (499 LOC, "standard God-Activity pattern," line 145, `STRUCT-004`). Process-global Singleton + Service-Locator in the static `App.bus` Otto bus reached from 12 files across all layers, with hidden bidirectional coupling and a swallowed `IllegalArgumentException` (lines 103, 133-135, `STRUCT-002`). Leaky abstraction — the domain `State.java:32` magic-string match against a k-9 library message (lines 129, 414, 444). Service-as-helper lifecycle bypass in `SmsJobService` (lines 139-141, `STRUCT-003`). On the negative-space side, the deliverable also performs the harder anti-pattern judgment: the "Components explicitly not candidates for strangling" call (line 216) is an explicit anti-Golden-Hammer determination grounded in analysis, not absence of observation.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

---

### Criterion 3 — Trade-off articulation

**Verdict:** satisfied_with_recommendation

**Evidence present:** Every material technology decision carries the four trade-off elements. The ADR register (lines 387-402) plus the "Key rejected alternatives" block (lines 398-402) state, for each major choice, what is gained, what gives way, the alternative not chosen, and why: Rewrite rejected (discards correct core + 35 tests + 25-locale surface, 3-5x cost, zero user-visible gain); Greenrobot EventBus rejected (perpetuates the global-bus anti-pattern, adds rather than removes a dependency); raw JobScheduler rejected (does not subsume the AlarmManager fallback, no Jetpack lifecycle/test support); Koin rejected (runtime resolution vs. Hilt's compile-time validation). The trust-all removal trade-off is explicit (breaks genuine self-signed-IMAP users → mitigated with user-pinned-cert path + one-time notice, Risk Register line 375).

**Evidence missing:** A small number of secondary items in the Recommendations section (lines 406-421) state the directive and the gain without the rejected-alternative element that the ADRs supply for the primary decisions — e.g., #2 "Establish the JaCoCo coverage fitness function" and #7 "Consider vendoring the k-9 IMAP library" assert the choice but do not name the alternative weighed against it inline (the k-9 alternative — pin-to-released-coordinate — does appear in ADR/Risk rows, but is not carried into the recommendation itself).

**Passage:** Recommendations §, lines 406-421.

**Recommendation / Remediation:** Non-blocking. For the two secondary recommendations noted, carry the one-line rejected-alternative already present elsewhere in the document into the recommendation itself (for #7: "pin to a maintained released coordinate — rejected until a verified API-compatible release exists, hence vendoring as the interim"). This raises consistency with the ADR-level rigor; it does not change the verdict.

---

### Criterion 4 — Citation depth

**Verdict:** satisfied_with_recommendation

**Evidence present:** Source-code citations are anchored to file:line throughout and are the dominant evidence form — `app/build.gradle:10,15`; `AllTrustedSocketFactory.java:42-57`; `AuthPreferences.java:221-226,276-288`; `BackupJobs.java:66-72`; `State.java:32`; `OAuth2Client.java:145`; the Evidence Index (lines 424-465) and Appendix (lines 488-503) map every major claim to its source file and canonical ID. This is the anchored, verifiable form the doctrine demands and it is applied consistently. Canonical methodology sources are named with chapter anchors where load-bearing: Feathers Ch. 2 (characterization tests), Ch. 25 (Introduce Instance Delegator).

**Evidence missing:** A minority of canonical-source methodology citations are named without a chapter/section anchor in the deliverable body — e.g., the reactive/resilience discussion (CAP-001, lines 157-163) references Circuit Breaker behavior without re-anchoring it to Nygard *Release It!* Ch. 5 (the upstream `gaps.md`/`assessment.md` do anchor it), and ISO 25010 is invoked implicitly through the cited `ilities-assessment.md` rather than by characteristic name in the deliverable body. The code-citation backbone is ~95% anchored, comfortably above the 80% threshold, so this is a polish item, not a deficiency.

**Passage:** CAP-001 (lines 157-163); cross-reference to `ilities-assessment.md` (line 450).

**Recommendation / Remediation:** Non-blocking. Where a methodology source is named in the deliverable body (Nygard, ISO 25010), carry the chapter/characteristic anchor that already exists in the source outputs (Nygard Ch. 5; ISO 25010 Reliability/Security/Maintainability/Portability by name). Threshold is already met; this tightens the remaining unanchored minority.

---

### Criterion 5 — NFR and quality-attribute coverage

**Verdict:** satisfied

**Evidence present:** All eight ISO 25010 characteristics are consciously addressed across the deliverable and its cited `ilities-assessment.md` (ARCH-001..021, line 450). Security: trust-all TLS / CWE-295 and plaintext credentials / CWE-312, the two highest-priority axes (SEC-001/SEC-002). Reliability: durable restore checkpoint, idempotency, classified retry (CAP-001). Performance Efficiency: O(mailbox)→O(page) IMAP search bounding (CAP-001, line 161). Maintainability: the entire ports/seams thesis, coverage fitness function, God-Activity decomposition. Portability: targetSdk gate, AGP/Gradle currency, minSdk uplift, abandoned-dependency retirement (the dominant theme). Compatibility: the public `com.zegoggles.smssync.BACKUP` broadcast contract preservation (line 302) and below-API-24 broadcast fallback. Functional Suitability: behavior-preservation discipline (preserve domain core verbatim; characterization tests pin behavior before each swap). Usability: the one-time migration notice for affected self-hosted-IMAP users and the FLAG_IMMUTABLE crash avoidance (user-facing reliability). The upstream Cloud Readiness analysis explicitly reasons about which 12-factor/N-A dimensions do not apply rather than silently dropping them.

**Evidence missing:** (none) — coverage is present and, for the two characteristics that are weakest fits to a backendless on-device app, the non-applicability is reasoned rather than omitted.

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

---

### Criterion 6 — Tooling evidence

**Verdict:** satisfied

**Evidence present:** Deterministic signals are cited and, critically, interpreted architecturally rather than tabulated raw. test:prod LOC ratio 0.33:1 is cited and interpreted as a coverage-adequacy risk that gates the refactor sequence (line 187, OPS-002). `MainActivity` 499 LOC / highest branch-token complexity is cited and tied to the God-Activity finding and its untested state (line 145). Dependency usage counts are grep-verified upstream and interpreted here: Otto in 12 files, JobDispatcher in 4, AsyncTask in 3 — each count drives a blast-radius and sequencing conclusion. CWE IDs (295, 312) anchor the security findings. Supply-chain signals (scijava mirror, JitPack SHA pin) are interpreted as build-reproducibility single points of failure. The deliverable and its sources are explicit and honest about the tooling NOT run (no live JaCoCo/McCabe/OWASP-dependency-check execution — Limitations sections state coverage remains unmeasured and "latest version" rests on analyst knowledge), which is the correct treatment of tooling-absence: justified, not silent.

**Evidence missing:** (none) — no tooling-substitution (every metric is interpreted) and no unjustified tooling-absence (the gaps are named with reasons and folded into the PROC-002 remediation that installs the missing measurement).

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

---

### Criterion 7 — Sequencing rigor

**Verdict:** satisfied

**Evidence present:** This is the deliverable's strongest dimension. The Remediation Roadmap (lines 263-347) is time-banded with explicit dependency notation, parallelism, and ordering rationale. The Critical Path is drawn explicitly (lines 327-337): MU-001 (SDK gate) → MU-002 (safety net, co-req) → MU-006 (Hexagonal ports) → MU-005 (WorkManager) → MU-010 (durable restore), with independent parallel tracks (MU-003 trust-all removal, MU-002 CI) annotated "start day 1, no upstream dependencies." Ordering rationale is stated for each edge: Robolectric is a co-requisite of the SDK bump (caps at SDK 29); ports must precede the WorkManager swap; Otto→Flow follows WorkManager "to avoid double-churning the engine"; the security track gates Play release but parallelizes with the gate. Decision gates G0-G3 (lines 284-306) make the ordering testable. The "one binding flip retires four dead dependencies" leverage analysis (line 301) is the kind of cross-cutting sequencing insight the doctrine rewards.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

---

### Criterion 8 — Anti-AI-slop check

**Verdict:** satisfied

**Evidence present:** Read as a senior architect reviewing AI output, this survives depth pushback. None of the nine named anti-patterns is present at a value-degrading level: patterns are applied to the subject (no surface enumeration); citations are anchored to file:line and canonical IDs (no unanchored authority); trade-offs resolve to decisions with rationale (no symmetric trade-off bullets); pattern names are integrated, not dropped (the `Driver`-Strategy-as-ready-made-port insight is the antithesis of name-dropping); language is decisive, not hedged (no maximalist "could be considered, where applicable"); metrics are interpreted (no tooling substitution); recommendations are specific and act-on-able with file/line/version targets (no "improve modularity"); the one genuinely out-of-scope item — Gmail REST API as a product decision (line 418) — is justified, not deferred for convenience; and claims carry evidence, not "best practice" appeals. The traceability from every deliverable claim back to an A-/G-/C-/MU- canonical ID and a source file is exactly the auditable rigor that distinguishes expert-exceeding synthesis from plausible-sounding slop.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

---

## Overall Verdict

**Verdict:** PASS

**Rationale:** The synthesized deliverable meets or exceeds the expert-exceeding-depth bar on all eight Self-Check Criteria. Six criteria are `satisfied` outright; two (Trade-off articulation, Citation depth) are `satisfied_with_recommendation` for minor polish — carrying the rejected-alternative element into a couple of secondary recommendations, and re-anchoring the small minority of methodology citations (Nygard Ch. 5; ISO 25010 by characteristic) that the source outputs already anchor. Neither rises to a deficit: the code-citation backbone clears the 80% anchoring threshold comfortably, and every material decision carries full trade-off rationale in the ADR register. The aggregate `satisfied_with_recommendation` count is 2, within the cap of 2 permitted on PASS per REQ-FRAMEWORK-000003 AC-5. No criterion is `deficient`. The deliverable demonstrates the framework's value proposition — multi-framework synthesis (modernization catalog, Hexagonal/ACL, Feathers seams, GoF, ISO 25010, WSJF, ATAM-style sensitivity reasoning) anchored in grep-verified file:line evidence and an explicit, dependency-aware, parallelism-annotated critical path — at a depth no surface-level pass would reach.

**Iteration:** 1

**Rework routing (FAIL only):** N/A — verdict is PASS.
