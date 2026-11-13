---
artifact_type: gate-result
story_id: "U-062"
gate: expert-depth-check
gate_id: "expert-depth-check"
checkpoint: plan-gate
pipeline: story-build
artifact_under_review: sdlc/artifacts/build/sprints/sprint-019/U-062/plan.md
producing_agent: amp/agents/solution-architect
verdict: FAIL
severity: warning
agent: "Solution Architect (separate_instance reviewer)"
reviewer_agent: amp/agents/solution-architect
reviewer_mode: separate_instance
iteration: 3
timestamp: "2026-11-12"
produced_at: 2026-11-12T00:00:00Z
recommendations_count: 0
criterion_results:
  - criterion: "Pattern grounding"
    verdict: deficient
    deficit_present: true
  - criterion: "Anti-pattern recognition"
    verdict: deficient
    deficit_present: true
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
    verdict: satisfied_with_recommendation
    deficit_present: false
  - criterion: "Sequencing rigor"
    verdict: satisfied_with_recommendation
    deficit_present: false
  - criterion: "Anti-AI-slop check"
    verdict: satisfied
    deficit_present: false
---

# Gate Report: gate-result — U-062

## Gate: expert-depth-check @ plan-gate

## Verdict: FAIL (severity: warning; recorded, does not block implementation)

## Summary

The plan (`sdlc/artifacts/build/sprints/sprint-019/U-062/plan.md`, iteration 3, 963 lines) was judged as a
research design for a cited feasibility analysis, not as a code-change plan. As a research design it is
strong:
- every repo fact is anchored to `file:line` at `c8f04558`;
- the source of every upstream fact is named, and each is marked "to re-verify at the pin";
- the three decisive capability tests have explicit refute/confirm rules;
- citation integrity is checked mechanically, and the IC-3 hygiene check has a positive control;
- the B/F/G/S/P decision logic is total and mutually exclusive, and labels its own interpretations.

The deliverable is a modernization recommendation. It ends in `go` / `go-with-conditions` / `no-go`, with
fallback options (Section 9) and a Part B split (Section 11). The plan gives the executor no canonical
vocabulary for that recommendation. It names no pattern and no anti-pattern anywhere, and it does not say
that none apply. The subject plainly involves both:
- the `MailTransport` port is the anti-corruption layer named in DES-MODERNIZATION-009's own title;
- the app leaks vendor types past that port into the MIME model.

Criteria 1 and 2 are therefore deficient. The fix is two short additions to the Question Guide, not a
re-plan. Criteria 6 and 7 carry one recommendation each.

## Findings

### Criterion 1 — Pattern grounding

**Verdict:** deficient

**Evidence present:** The plan analyses the port without naming its pattern. Q5.p (lines 521-564)
measures every `MailTransport` member, the constructor, `BinaryTempFileBody` and every CNTR-007
translation row against upstream. It uses the verdict "preserved with adapter change", where "the blocking
moves inside the adapter" (lines 531-532). That is exactly the anti-corruption-layer argument.
Recommendation Logic (lines 681-685) requires an option comparison between `migrate` and
`stay on :k9mail-vendored`, with a trade-off. Story Overview (line 37) says "This plan does not decide the
outcome."

**Evidence missing:** No canonical pattern is named anywhere in the plan. A grep of `plan.md` for
anti-corruption, hexagonal, ports and adapters, strangler, branch by abstraction, parallel run, Feathers,
Fowler, Evans or Newman finds only "ACL" in the DES-009 row of Artifacts Consulted (line 921). There is
also no statement that no pattern applies.

The doctrine's "Modernization recommendation" row says what this deliverable needs: a selection from
Strangler Fig, Branch by Abstraction, Anti-Corruption Layer, Parallel Run and the related patterns, with
rationale. U-062's analysis is that work type (Sections 9, 11 and 12). The plan does make an analytic
choice: the port is treated as the stable boundary, and Section 11 rows "assume CNTR-MODERNIZATION-007 and
-001 unchanged". But it does not tell the executor which migration pattern that choice implies. It also
does not say that Section 11's split should be sized under that pattern rather than as a big-bang library
swap.

**Passage:** Q5.p (lines 521-564); Q9 Fallbacks (lines 618-626); Q11 Sizing (lines 633-637);
Recommendation Logic › option comparison (lines 681-687).

**Recommendation / Remediation:** Implementer note. Add a short "Canonical framing" paragraph at the top of
the Question Guide, and have the analysis use these names in Sections 5, 9, 11 and 12:
- **Port and adapter.** `MailTransport` is a Port with `K9MailTransport` as its Adapter (Cockburn,
  "Hexagonal Architecture", 2005). For the transport it serves as an Anti-Corruption Layer (Evans,
  *Domain-Driven Design*, Ch. 14 "Maintaining Model Integrity" — Anti-Corruption Layer), as
  DES-MODERNIZATION-009 names it. A Q5.p verdict of "preserved with adapter change" means the ACL absorbs
  the change. Section 5 should say so in those words.
- **Migration style for Section 11.** The executor states which style each Part B row assumes, with one
  line of rationale against the alternative:
  - Branch by Abstraction (Newman, *Monolith to Microservices*, Ch. 3 "Branch by Abstraction"). The port
    already is the abstraction for the six transport files, so a second adapter can be built behind it
    and switched.
  - Parallel Run (Newman, ibid., Ch. 3 "Parallel Run"). It matches REQ-019's golden-master and
    live-account checks (AC-B4, AC-B7).
  - Big-bang replacement, which is the only option for code outside the port (see Criterion 2).

  Naming a migration style is sizing rationale, not interface design, so it stays inside the plan's own
  Part B boundary (Risk 6).
- **Section 9 options as context-map relationships** (Evans, Ch. 14 "Conformist", "Separate Ways").
  "Stay on `:k9mail-vendored`" and "depend on a fork" keep the app Conformist to a frozen model.
  "Migrate" makes it Conformist to a moving upstream unless the ACL is widened. The option comparison
  states which relationship each option creates.

### Criterion 2 — Anti-pattern recognition

**Verdict:** deficient

**Evidence present:** The plan records several current-state defects as facts, without naming them as
anti-patterns:
- "Unable to get IMAP prefix" text matching at six `K9MailTransport.java` sites (Verified Facts, line 66),
  and the folder-not-ready text match `NONEXISTENT` / `did not find message count` (line 68). Q5.p item 3
  turns both into discriminator checks (lines 535-545).
- `mail/BackupImapStore.java` as a legacy class whose statics alone are live (lines 61, 64).
- CNTR-007 text drift and DES-009's stale contract id, routed to the Artifact Librarian (lines 857-865).
- Section 2's per-file table (line 276).

**Evidence missing:** The plan names no anti-pattern and does not state that anti-pattern analysis was
done. The subject visibly has anti-patterns that set the Part B cost and the recommendation. The reviewer
measured these at `c8f04558`:
1. **Leaky anti-corruption layer (vendor-model leakage past the port).** The story's recount grep (all of
   `app/src/main/java`, minus the `mail/transport` directory) returns 14 files. `MessageConverter.java:27-31`
   imports `com.fsck.k9.mail.{Address,Flag,Message,MessagingException}` and `internet.MimeUtility`. The
   port's `importMessageBody` hands a k9 `Message` to that converter (`K9MailTransport.java:304-305`, plan
   line 68).

   So the ACL covers the transport only. For the MIME model, the app is Conformist to the vendored model
   (Evans, Ch. 14 "Conformist"). The adapter cannot absorb those 8 files: they change whatever happens at
   the port. Q5.p item 7 (lines 552-554) touches this case but does not classify it. Without this,
   Section 11 may under-size Part B, and Section 12 may read "port preserved" as "migration contained".
2. **Discriminating errors by parsing exception detail messages.** Typed error translation depends on
   upstream message text, which is not an API (8 matching lines in `app/src/main/java` for
   "Unable to get IMAP prefix" alone). Bloch, *Effective Java* 3rd ed., Item 70, says recovery information
   should be exposed through accessor methods rather than left for callers to parse. Q5.p checks only
   whether the text is still present at the pin. Even when it is present, the fragility carries into
   Part B and should be costed as a risk.
3. **Lava Flow** (Brown et al., *AntiPatterns*, 1998, Ch. 5 "Lava Flow"). `BackupImapStore` is a legacy
   class that keeps an `Item.DATE` site (`:181`) and live statics (`MailModule.kt:80`,
   `AdvancedSettings.java:533`). It is one of the 14 files in Section 2. Whether its row is "replace" or
   "delete the dead body, keep the statics" changes the Section 11 file count.

**Passage:** Verified Facts (lines 55, 61, 64, 66, 68); Q2 Modules (lines 440-449); Q5.p items 3 and 7
(lines 535-554); Q11 Sizing (lines 633-637); Quality Attribute Coverage › Maintainability (lines 898-899).

**Recommendation / Remediation:** Implementer note. Add an "Anti-pattern analysis (current state)"
instruction to Q2, carried into Sections 2, 11 and 12:
- (a) Section 2's per-file table gets a column `Behind the port / leaks past the port`. Each of the 14
  rows is classified with its import lines cited. Section 11 counts the "leaks past the port" files
  separately, as Conformist-model work that the ACL cannot absorb.
- (b) The Q5.p exception-mapping sub-table names message-text discrimination as an anti-pattern (Bloch
  Item 70) in its Notes column. A "discriminator present" verdict still records the residual fragility.
- (c) The `BackupImapStore` row in Section 2 states whether its migration is a replacement or a Lava Flow
  removal, with the live statics cited.
- (d) One closing sentence states that these three were the anti-patterns found. It names the greps that
  were run, so that "none other found" rests on analysis and not on absence of observation. Upstream's own
  "legacy" and `@Discouraged` markers (already under Maintainability) are reported in the same place as
  upstream-declared deprecation.

### Criterion 3 — Trade-off articulation

**Verdict:** satisfied

**Evidence present:** Each of the plan's own decisions states what is gained, what gives way, the
alternative and why it was rejected:
- **D1** (lines 82-94). Chosen: the research pin. Gained: the input's line numbers stay usable, and there
  is no re-anchoring. Gives way: currency; this is managed by a recorded drift list. Rejected: a moving pin,
  because it "buys nothing and costs a full re-anchor". A fallback applies if history was rewritten.
- **D2 (c)/(d)** (lines 102-113). Gained: no untrusted upstream build logic, and no 1-2 GB of downloads.
  Gives way: the size delta becomes an estimate, and the plan departs from the story's Estimation Guidance.
  Both costs are stated and must be disclosed to the product owner at I5. The alternative is rejected
  because it "cannot change the recommendation's class", and the plan explains why.
- **D3** (lines 123-125). Gained: a minimal IC-4 delta and no machine paths in tracked files. Given up: the
  tracked raw evidence.
- **D4.7** (lines 163-167). QA is deferred against the compiled pipeline. The rationale is that otherwise
  `on_any_fail → blocked`.
- **Recommendation Logic** (lines 681-686) makes the analysis itself state "what is gained, what gives way,
  the alternative not chosen and why".

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 4 — Citation depth

**Verdict:** satisfied

**Evidence present:** The few external normative sources the plan does cite are anchored to sections:
- Apache-2.0 §4(d) (line 465);
- MPL-2.0 §3.1-3.3, including the Larger Work clause (line 456);
- CNTR-MODERNIZATION-007 and -001 clauses by line (lines 69, 845-850);
- REQ-015/016/017 targets by line (line 77);
- EPIC-006 SC5/SC9 by line (line 76).

Every repo claim is `file:line` at a named commit. The Citation Mechanics section (lines 291-325) imposes
40-hex permalinks and line ranges on the deliverable, and checks them mechanically. Every citation the
plan makes is anchored, well above the 80% threshold.

**Evidence missing:** (none). The missing canonical-pattern citations are assessed under Criteria 1 and 2,
not double-counted here.

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 5 — NFR and quality-attribute coverage

**Verdict:** satisfied

**Evidence present:** "Quality Attribute Coverage (ISO/IEC 25010)" (lines 884-901) names all eight
characteristics and maps each to a story-specific evaluation point in the analysis:
- Functional Suitability: the APPEND date, header search and date-only fetch against AC-B4/B5.
- Performance Efficiency: the cost of an ENVELOPE fetch over a DATE fetch, plus APK bytes and methods.
- Compatibility: Kotlin metadata direction, minSdk and the Hilt/2.4 coupling.
- Usability: explicitly N/A, with a reason.
- Reliability: `suspend` on worker threads and the `Log.logger` `lateinit` bootstrap.
- Security: hostname verification, pinning semantics, the SNI guard and the supply chain.
- Maintainability: upstream "legacy" status and fork re-application cost.
- Portability: building outside the upstream tree, and JDK 25.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 6 — Tooling evidence

**Verdict:** satisfied_with_recommendation

**Evidence present:** The plan prescribes deterministic signals and uses them in its conclusions:
- dexdump method and class counts, with a three-attempt ladder (I0.4, lines 348-357);
- per-prefix retention from `mapping.txt`, which feeds the retention factors k_b and m of the Q8 estimate
  (lines 597-616);
- JUnit XML totals from the reused init script, and the `@Test` count of 682 measured at HEAD;
- `git rev-list --left-right --count` for ahead/behind;
- the drift list;
- a `verification-metadata.xml` count per coordinate;
- a class-declaration regex that must be validated on 5 sample files before use.

At planning time the plan measured the input's SHA-256 and line count, with the 290/291 discrepancy
recorded (line 78).

**Evidence missing:** Section 11 sizing (Q11, lines 633-637) counts files only. A coupling signal is
available and cheap: the number of `com.fsck.k9` import lines and distinct k9 types per file. At
`c8f04558` the reviewer measured 61 `import com.fsck.k9…` lines across 16 files, and 33 distinct
`com.fsck.k9.*` names in `app/src/main/java`. These are efferent coupling (Ce) numbers (Martin, *Clean
Architecture*, Ch. 14 "Component Coupling" — Stable Dependencies Principle, instability metric). They size
Part B better than a file count, because one file with 20 vendor types costs more than one with a single
`MessagingException`.

**Passage:** Q11 Sizing (lines 633-637).

**Recommendation / Remediation:** Implementer note. Add to Q11 the two counts
`grep -rh "^import com\.fsck\.k9" app/src/main/java | wc -l` and
`grep -rhoE "com\.fsck\.k9(\.[A-Za-z0-9_]+)+" app/src/main/java | sort -u | wc -l`, plus a per-file count.
Record these in the Section 11 counts table as Ce. Cross-reference them to each Section 2 row, so the
"leaks past the port" class (Criterion 2a) carries a measured weight.

### Criterion 7 — Sequencing rigor

**Verdict:** satisfied_with_recommendation

**Evidence present:** I0-I5 are ordered one turn each, with optional I1a/b and I2a/b splits (lines
216-218). The plan gives ordering reasons:
- the I0 baseline precedes Q8 because Q8 uses the I0 numbers (line 387);
- Q5.p feeds predicate P in Section 12 (lines 653-666);
- the I4 consistency pass recomputes cross-section counts;
- QA follows the AC-15 record (Acceptance Sequencing step 6, with the deadlock argument).

Parallelism is identified: code and security review run in parallel, and Part A runs in parallel with
REQ-015/016/017 (story Notes). A resume rule (lines 210-212) makes the sequence restartable.

**Evidence missing:** There is one unacknowledged backward dependency. Q3 runs in I1 (line 381). Its
"Android variant" depth rule (lines 463-464) says: "If Section 4 keeps Android-variant consumption as a
live mode, add the KMP `androidMain` dependencies". But Section 4 is produced by Q4 in I2 (line 386).
Section 2's transitive list (Q2, also I1) has the same dependency. Both feed the Q8 byte estimate and the
verification-metadata count. As written, the executor either guesses the consumption mode at I1 or
silently leaves those dependencies out.

**Passage:** Q3 › Android variant (lines 463-464); I1.3 (line 381); I2 (lines 386-389).

**Recommendation / Remediation:** Implementer note. Add to the I2 end checks: "If Section 4 keeps
Android-variant consumption live, return to Sections 2 and 3, add the `androidMain` closure rows, and
re-run the I4 cross-count before Q8 uses Section 2." An alternative is to settle the list of viable
consumption modes as the last step of I1, before Q3, and cite the build files it rests on.

### Criterion 8 — Anti-AI-slop check

**Verdict:** satisfied

**Evidence present:** None of the nine doctrine anti-patterns degrades the plan's analytical value:
- **No maximalist hedging.** Each question has a source list, a command, and a verdict rule that states
  what refutes and what confirms. The three P4 capability tests (lines 505-509) are an example.
- **No generic recommendations.** Even the size estimate is given as a formula with named inputs.
- **No tooling substitution.** Metrics feed a verdict or a range.
- **No unjustified out-of-scope deferral.** Experiments (c) and (d) are justified against AC-9 and AC-10
  and against the recommendation's class. Their consequence is converted into a cost range or a numbered
  condition, never "unknown". The CNTR-007 and DES-009 drift is routed to a named owner.
- **No authority appeal.** Plan-level interpretations beyond AC-14 are labelled as such (lines 648-650,
  670-673).

A senior reviewer would push back on the missing canonical vocabulary (Criteria 1 and 2), not on the
specificity or rigour of the research design. That is assessed separately.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

## Overall Verdict

**Verdict:** FAIL

**Rationale:** Five criteria are satisfied, and Criteria 6 and 7 carry one recommendation each, which is
within the limit of two. The gate fails on two deficient criteria:
- **Criterion 1 (Pattern grounding).** The deliverable is a modernization recommendation built around a
  port that DES-009 calls an anti-corruption layer. The plan never names that pattern, or any
  migration-style pattern (Branch by Abstraction, Parallel Run), for Sections 9, 11 and 12 to reason with.
- **Criterion 2 (Anti-pattern recognition).** The highest-severity gap is the unnamed leaky ACL: 8
  MIME-model files and the converter behind `importMessageBody` depend directly on `com.fsck.k9` types, so
  a "port preserved" verdict does not mean the migration is contained. The plan also leaves message-text
  error discrimination and the `BackupImapStore` Lava Flow unnamed.

The remediation is two Question Guide paragraphs plus one column in Section 2. It can be handed to the
executor as notes without re-planning. The gate severity is `warning`, so this FAIL is recorded and does
not block implementation.

**Iteration:** 3

**Rework routing (FAIL only):** Routes to amp/agents/solution-architect at step `planning` for iteration 4. Cap is 3 reworks (4 total evaluations). On cap exhaustion, escalates via AskUserQuestion. This gate is warning severity, so the orchestrator may record the FAIL, pass the remediation notes above to the implementer, and proceed without rework.

## Phase Completion Report
---
story_id: "U-062"
phase: "plan-gate/expert-depth-check"
verdict: "FAIL"
artifact_path: "sdlc/artifacts/build/sprints/sprint-019/U-062/expert-depth-check.md"
story_status: "in-progress"
current_build_phase: "implementation"
errors: []
---
