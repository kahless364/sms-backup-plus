---
artifact_type: gate-result
story_id: "U-060"
gate_id: "expert-depth-check"
gate: expert-depth-check
checkpoint: plan-gate
pipeline: story-build
artifact_under_review: C:/Code/Android/sms-backup-plus/sdlc/artifacts/build/sprints/sprint-017/U-060/plan.md
producing_agent: amp/agents/solution-architect
verdict: FAIL
severity: warning
agent: "Solution Architect"
reviewer_agent: amp/agents/solution-architect
reviewer_mode: separate_instance
iteration: 3
timestamp: "2026-11-11"
produced_at: 2026-11-11T23:18:42Z
recommendations_count: 0
criterion_results:
  - criterion: "Pattern grounding"
    verdict: deficient
    deficit_present: true
  - criterion: "Anti-pattern recognition"
    verdict: satisfied_with_recommendation
    deficit_present: false
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

# Gate Report: expert-depth-check — U-060

## Gate: expert-depth-check @ plan-gate

## Verdict: FAIL (severity: warning — observe-only, does not block implementation)

## Summary

The plan (Revision 3, 1007 lines; all of it was read for this review) is analytically very strong. Its version-selection trade-offs, use of vendor primary sources, tooling-backed baselines, checkpointed sequencing and ISO 25010 coverage are well beyond the usual depth for a toolchain migration. It fails on one criterion only: **Pattern grounding**. The plan applies several recognizable migration and reliability patterns, but it never names a canonical pattern. It also never says that no canonical pattern applies. Two criteria pass with recommendations (anti-pattern naming and citation-depth framing). Under the rubric, any `deficient` criterion makes the gate FAIL, and `recommendations_count` is then 0.

A grep of plan.md for `pattern|Fowler|Feathers|smell|Strangler|Newman|PoEAA|characteriz|parallel` returned no canonical pattern or catalog reference. The only matches were the plain-language phrases "fail-fast guard" (lines 347, 889, 912) and "ISO/IEC 25010" (line 906).

## Findings

### Criterion 1 — Pattern grounding

**Verdict:** deficient

**Evidence present:** The plan makes real architectural and migration choices, and it argues each one well:
- It treats the migration as a single atomic cutover: "Why it is one change set … No intermediate state builds on JDK 25" (Story Overview, lines 44–49).
- It captures a JDK 17 baseline in an isolated tree and compares it with JDK 25 on an equivalence basis: test totals, per-package class counts, LINE counters and the KSP file-name set (Steps 0.3–0.6, 7.2, B1(c)).
- It builds a single shared list of JaCoCo class roots with a "fail-fast guard" (Step 2.2, line 347).
- It moves JDK selection out of an undeclared user-global setting and into a declarative, repository-tracked criteria file (Version Decisions, "Daemon JDK declaration" row, line 70).
- It pre-specifies contingency branches: Branch M, the T3 path and the Step 3.4 fallback.

**Evidence missing:** None of these choices is tied to a canonical pattern, and the plan never documents that no pattern applies. The doctrine's Modernization row (`expert-exceeding-depth.md`, Operational Definition table) requires selecting "from the explicit modernization pattern catalog (Strangler Fig, Branch-by-Abstraction, Anti-Corruption Layer, Parallel Run, …) with rationale for selection over alternatives" and "characterization tests per Feathers". The plan does the substance of this, but leaves the alternatives it implicitly rejected unnamed. In particular, it rejects incremental migration (Branch by Abstraction / Parallel Change) in favor of a big-bang cutover without saying so in pattern terms.

**Passage:** Story Overview "Why it is one change set" (lines 44–49); Step 0 (lines 238–300); Step 2.2 JaCoCo guard (lines 347–368); Step 7.2 (lines 690–692); Version Decisions "Daemon JDK declaration" row (line 70).

**Recommendation / Remediation:** Add a short "Patterns applied" subsection after Story Overview with the following four entries. Each should be one or two sentences that apply the pattern to this story.
1. **Big-bang cutover, with Branch by Abstraction / Parallel Change explicitly rejected.** Cite Fowler, bliki "BranchByAbstraction" and "ParallelChange" (Sato). The rejection reason is the plan's own argument: the Gradle 9 → AGP 9 → KGP ≥ 2.2.10 → KSP2 → Hilt/androidx.hilt chain has no stable intermediate that runs on JDK 25, so no abstraction seam exists to migrate behind. Under that heading, name the uncommitted, explicit-path rollback as the mitigation that replaces incremental safety.
2. **Characterization testing for the baseline-equivalence evidence.** Cite Feathers, *Working Effectively with Legacy Code*, Ch. 13 "I Need to Make a Change, but I Don't Know What Tests to Write" (characterization tests). Also cite Newman, *Monolith to Microservices*, Ch. 3, "Parallel Run" for the two-JDK baseline-versus-after comparison (Steps 0.5–0.6 and 7.2–7.5). State that the comparison is sequential rather than a live parallel run, and why: one machine, one daemon.
3. **Fail Fast** (Shore, "Fail Fast", *IEEE Software* 21(5), 2004) for the JaCoCo class-root guard. Also cite the refactoring "Consolidate Duplicate Conditional Fragments" / DRY (Fowler, *Refactoring* 2nd ed., Ch. 3 "Duplicated Code") for the shared root list.
4. **Configuration as code / declarative environment specification** for `gradle/gradle-daemon-jvm.properties`. Cite Humble & Farley, *Continuous Delivery*, Ch. 2 "Configuration Management" ("keep absolutely everything in version control"). This is the principle that makes AC-7's root-cause fix more than a local workaround.

If the author believes these do not apply, the alternative remediation is one explicit paragraph saying why no canonical pattern applies to a dependency-version migration. Silence does not meet the criterion.

### Criterion 2 — Anti-pattern recognition

**Verdict:** satisfied_with_recommendation

**Evidence present:** The plan finds concrete defects in the current system and in the alternatives it considered, and it addresses each one:
- The undeclared, user-global JDK is the "root cause (an undeclared, user-global JDK)" (line 70).
- The two JaCoCo class-root lists are duplicated "so the two copies cannot drift" (line 347).
- A moved Kotlin class root would produce a silent empty `fileTree` (R2, line 889).
- Stale comments (Step 2.2, line 376).
- A user-global `org.gradle.jvmargs` silently overrides the repository's jvmargs (SC-4).
- A pre-existing dependency-verification gap (SC-3).
- The rejected Fallback B2 is an "undocumented processor-classpath override" (SC-2(4)(iii)).
- The plan forbids opt-out flags that would cause a "suppress-rather-than-fix" problem (Step 2.5, "Explicitly not done (AC-17)").

**Evidence missing:** None of these is labeled with a canonical anti-pattern name. The criterion's FAIL condition ("does not name them") is not met, because each one is named descriptively and remediated. The PASS condition's "canonical catalog" naming is still absent.

**Passage:** Version Decisions line 70; Step 2.2 line 347; SC-3, SC-4; R2; SC-2(4).

**Recommendation / Remediation:** Label the existing findings with their canonical names:
- Undeclared user-global JDK: "Snowflake / works-on-my-machine environment" (Fowler, bliki "SnowflakeServer"; Humble & Farley Ch. 11 "Managing Infrastructure and Environments").
- Duplicated class-root lists: the "Duplicated Code" smell (Fowler, *Refactoring* 2nd ed., Ch. 3).
- SC-4 jvmargs shadowing: "configuration drift".
- Fallback B2: "Lava Flow" / undocumented workaround risk (Brown et al., *AntiPatterns*, 1998, Ch. 5).

### Criterion 3 — Trade-off articulation

**Verdict:** satisfied

**Evidence present:** Every material decision has all four elements: what is gained, what gives way, the alternative, and why the alternative was not chosen.
- **Gradle 9.5.1 vs 9.3.1, 9.1.0, 9.4.x and 2.4-based triples** (SC-5). Gained: a literal stable listing in both AC-11 sources. Gives way: KGP 2.3.21 sits two minors above the Kotlin table's fully-supported maximum (R16). The alternatives are rejected one by one with quoted page text.
- **AGP 9.0.1 vs Option A (Gradle 9.2.1 + AGP 8.13)** (SC-1). Rejected for three stated reasons: SC-2 resolves the risk, KSP1 is deprecated, and 8.13 is the final 8.x line.
- **Fallback B2 dropped** (SC-2(4)(i)–(iii)).
- **androidx.hilt 1.2.0 kept vs 1.3.0** (Version Decisions row): minSdk 21 is preserved, at the cost of Checkpoint B1 risk.
- **Robolectric 4.15.1 vs 4.16** (row), and the T3 path vs the full Branch M (R2-B1 revision).
- **`-dontoptimize` + optimize file vs the `proguardAndroidTxt` opt-out** (Step 2.2).
- **R14 containment in user-home `gradle.properties` vs a `-D` flag vs the repo `gradle.properties`** (Step 8.1).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 4 — Citation depth

**Verdict:** satisfied_with_recommendation

**Evidence present:** Every citation in the plan is anchored. Sources are version-pinned vendor URLs (e.g. `docs.gradle.org/9.5.1/userguide/gradle_daemon.html`) with verbatim quotes, specific issue numbers (google/dagger #5177, #5190; KSP #2774, #2772), named release-note entries (KT-85405; Dagger 2.60 "minSDK for Hilt is now 23"), POM/AAR manifest inspections, and published checksums. No citation is unanchored, so the FAIL trigger ("unanchored citations where anchors could exist") is not met. For a version-compatibility decision, the vendor primary sources are the governing authority, and they are cited more precisely than most book-chapter citations.

**Evidence missing:** The plan makes no canonical-literature citations. It also does not state explicitly that canonical literature is not the governing authority for this decision class, which is what the criterion's PASS clause asks for.

**Passage:** Version Decisions table (lines 57–71); SC-5; Artifacts Consulted "External" row (line 995).

**Recommendation / Remediation:** Add one sentence to the Version Decisions preamble (line 55): "For version-compatibility claims the vendor compatibility matrices and release notes are the authoritative sources; canonical architecture literature is cited only for the migration and reliability patterns in 'Patterns applied'." Then make sure the Criterion 1 additions carry chapter-level anchors, as given above.

### Criterion 5 — NFR and quality-attribute coverage

**Verdict:** satisfied

**Evidence present:** The "Quality Attribute Coverage (ISO/IEC 25010)" section (lines 906–915) evaluates all eight characteristics by their ISO names: Functional Suitability, Performance Efficiency, Compatibility, Usability, Reliability, Security, Maintainability and Portability. Each one has a story-specific finding rather than a placeholder. Examples:
- Performance Efficiency: the KSP2 Windows recompilation fix and `-dontoptimize` keeping APK runtime unchanged.
- Security: wrapper checksum present from the first download, additions-only verification metadata, and no foojay auto-download.
- Compatibility: SC-5 and the minSdk branch consequences.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 6 — Tooling evidence

**Verdict:** satisfied

**Evidence present:** Deterministic signals were gathered during planning and used to drive decisions:
- `@Test` count 697 from the authoritative `git grep` command (Step 0.2).
- JaCoCo `di` package at 3.1% (1/32) against its 0.03 floor, which is what makes R4 a stop condition (Step 7.2).
- A reproduced SC-3 dependency-verification failure on JDK 17 at HEAD.
- `javap` of `gradle-api-9.0.1.jar` confirming DSL methods (Step 2.2, line 375).
- The internal artifact type `BUILT_IN_KOTLINC` found in AGP 9.0.1, which predicts the class-root move (B5).
- A scratch JDK 25 `javac -source 8` test and the `-Werror` scope found by grep (Step 2.3).
- A lint baseline with 39 issues, 10 of them `ObsoleteSdkInt` (M.5).
- 546 components in the verification metadata.
- `--version -Dorg.gradle.java.home` precedence verified on this machine (Step 0.4).
- Scope-verified `buildDir` line list (line 347).

The plan also builds new tooling (the JaCoCo inventory script and the additions-only XML diff script) to turn equivalence claims into checkable outputs.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 7 — Sequencing rigor

**Verdict:** satisfied

**Evidence present:** Steps 0–13 are gated by named checkpoints (0, A, B with B1–B5 and B-javac, C), each with explicit pass/fail criteria. The ordering rationale is stated wherever ordering matters:
- Step 1 bootstraps the wrapper because the old Gradle cannot run the `wrapper` task on JDK 25 or on JDK 17 (SC-3). Step 5 then regenerates it on 9.5.1.
- Step 6 runs after the exploratory writer passes so they leave no stale entries. T3 and late fallbacks re-run Step 6 explicitly (M.6, T.4).
- Daemons are stopped *before* restore, because the restored 8.14.1 wrapper cannot run on JDK 25 (Rollback procedure item 1).
- Step 12 (docs) follows Checkpoint C and is written from observed M2/M4 results.
- Step 5 may optionally run after Step 6, which is an explicit reordering allowance.
- Checkpoint C gives an ordered remediation precedence.

The work is inherently serial on one machine and one Gradle daemon, so a missing parallelism map does not make this a bare list.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 8 — Anti-AI-slop check

**Verdict:** satisfied

**Evidence present:** None of the nine doctrine anti-patterns appears at a level that reduces analytical value:
- No maximalist hedging. Each uncertainty becomes a numbered checkpoint with a trigger, a pre-specified outcome and a saved-evidence path (B1, B-javac, B5, T1/T2/T3/T3-forced, R16).
- No generic recommendations. Every edit has a file, a line and exact text.
- No authority appeal without grounding. Even the R14 containment location is justified with quoted 9.5.1 doc text.
- No out-of-scope deferral without reason. README line 38 and the stale code-location rows are deferred with the reason (outside story scope) and recorded as follow-up.
- No tooling substitution. Every metric is interpreted; for example, `di` at 3.1% becomes a stop condition, not a dashboard entry.

A senior build/release architect would accept this plan without depth pushback on its analysis. The Criterion 1 gap is a framing omission, not a hole in the analysis.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

## Overall Verdict

**Verdict:** FAIL

**Rationale:** The plan is strong. Its version-compatibility trade-off analysis is exhaustive and quotes the vendors verbatim (SC-1, SC-5). Its contingency branches are fully pre-specified, and its tooling-backed baselines turn every equivalence claim into a checkable artifact. The FAIL comes from one deficient criterion, **Criterion 1 (Pattern grounding)**. The plan carries out a big-bang cutover with explicit rejection of incremental migration, characterization/parallel-run baseline equivalence, a Fail-Fast guard and a configuration-as-code fix for a snowflake environment, all without naming any canonical pattern or saying why none applies. That is exactly the gap the doctrine's Modernization row targets. The fix is a short "Patterns applied" subsection with chapter-anchored citations (Feathers Ch. 13; Newman *Monolith to Microservices* Ch. 3 "Parallel Run"; Fowler *Refactoring* 2nd ed. Ch. 3; Humble & Farley Ch. 2; Shore 2004). It changes no implementation step. Criteria 2 and 4 would each carry a minor recommendation (canonical anti-pattern labels; a one-sentence authority-scope statement). Gate severity is `warning`, so this result is recorded and does not block implementation.

**Iteration:** 3

**Rework routing (FAIL only):** Routes to amp/agents/solution-architect at step `planning` for iteration 4. Cap is 3 reworks (4 total evaluations). On cap exhaustion, escalates via AskUserQuestion. Because this gate's severity is `warning` (observe-only), the orchestrator may record this FAIL and proceed to implementation without rework.

## Phase Completion Report
---
story_id: "U-060"
phase: "plan-gate/expert-depth-check"
verdict: "FAIL"
artifact_path: "sdlc/artifacts/build/sprints/sprint-017/U-060/expert-depth-check.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
