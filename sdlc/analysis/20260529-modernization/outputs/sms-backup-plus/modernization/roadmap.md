---
prompt: assessments/prompts/modernization/06-modernization-roadmap.md
step_id: "4.1"
title: Modernization Roadmap — SMS Backup+
assessment_date: 2026-05-29
phase: modernization
prompt_id: modernization/06-modernization-roadmap
step: "4.1"
date: 2026-05-29
analyst: Solution Architect (assessment agent)
assessment_id: 20260529-modernization
status: draft
subject: sms-backup-plus
version: "1.0"
owner: "Single senior Android maintainer (OSS, maintenance-mode)"
critical_path: "MU-001 (build/SDK gate) → MU-002 (test/coverage net, co-req) → MU-006 (Hexagonal ports) → MU-005 (Scheduler→WorkManager) → checkpoint half of MU-005/CAP-001"
universal_gate: MU-001
numbering_authority: "migration-units.md MU-001..MU-011 (Step 2.5.6). The prompt's 'MU-01 → MU-04' is reconciled in §0 to gate → scheduler/worker; no new numbering is invented."
---

# Modernization Roadmap — SMS Backup+

**Engagement:** 20260529-modernization · EXECUTE Step 4.1 (Modernization Roadmap)
**Subject:** P01 `app` — `com.zegoggles.smssync` (single Android module; 107 production Java files, ~10,635 prod LOC; 36 test files, ~3,550 test LOC)
**Quality bar:** `knowledge/standards/expert-exceeding-depth.md`
**Method:** Synthesis of the verified Step 2.1–3.2 corpus (assessment, target-state, gaps, candidates, approach, migration-units, prioritized-backlog) into a phased, dependency-aware, governed roadmap. WSJF sequencing inherited from `prioritized-backlog.md`; per-component dispositions inherited from `approach.md`; seam mechanics inherited from `candidates.md`. **All paths relative to repo root.**

---

## 0. Numbering Reconciliation, Provenance, and Critical-Path Anchoring (read first)

This roadmap **builds on the existing migration units and WSJF backlog — it invents no new numbering.** Two upstream artifacts use *different* MU schemes, and the orchestrator's stated critical path ("MU-01 → MU-04") matches neither literally. Resolving this transparently is a precondition for a usable roadmap, so it is done here first.

### 0.1 The two upstream MU schemes (both verified, read in full this session)

| Scheme | Source artifact | What "scheduler/WorkManager" is called there |
|---|---|---|
| **A — authoritative** | `migration-units.md` (Step 2.5.6) — IDs **MU-001..MU-011** | **MU-005** = Scheduler → WorkManager |
| **B — WSJF synthesis layer** | `prioritized-backlog.md` (Step 2.5.7) — IDs **MU-01..MU-11** | **MU-07** = WorkManager (its MU-04 = JaCoCo coverage gate) |

`prioritized-backlog.md` itself flags (its §0.1, §Limitations) that its `MU-*` IDs are a thin synthesis layer assigned because the canonical `migration-units.md` was absent in *its* session. **`migration-units.md` now exists and is authoritative.** Per Trust-But-Verify, this roadmap therefore **anchors on Scheme A (MU-001..MU-011)** — the canonical decomposition with full per-unit boundaries, shared-file allocation, and acyclic dependency DAG — and carries Scheme B's WSJF *scores* across via the crosswalk below. Where Scheme B and Scheme A disagree on a number, **Scheme A governs** (it is the artifact with the verified source-level boundaries; the prioritized-backlog is a prioritization pass over an absent input).

### 0.2 Reconciling the prompt's "critical path: MU-01 → MU-04"

The orchestrator restated two hard constraints: **MU-01 is the universal gate**, and the **critical path runs gate → scheduler/worker**. Mapped onto the authoritative Scheme A:

- **"MU-01" (build/SDK gate) → `migration-units.md` MU-001** (Build & Platform Uplift). Exact intent match.
- **"MU-04" (scheduler/worker) → `migration-units.md` MU-005** (Scheduler → WorkManager). The prompt's "MU-04" is the *fourth idea on the dependent spine* (gate → test-net → ports → **scheduler**), which is MU-005 in the canonical scheme and "MU-07/WorkManager" in the prioritized-backlog scheme. **The referent is unambiguous — the scheduler/worker migration — only the integer differs across schemes.** This roadmap uses the canonical **MU-005** and states the mapping wherever the spine is named, so no reader is misled by the integer.

The **full critical path** (from `approach.md` §critical-path, `migration-units.md` §critical path, and `prioritized-backlog.md` §3, all in agreement on the *topology* if not the integers) is:

```
MU-001 (SDK/build gate) ─► MU-002 (test+coverage net, co-requisite) ─► MU-006 (Hexagonal ports)
   ─► MU-005 (Scheduler→WorkManager) ─► durable-restore-checkpoint half of MU-005/MU-010
```

This is the gate → safety-net → structural-precondition → largest-blast-radius-refactor → data-integrity-terminus spine. It is the constraint that overrides raw WSJF where a high-score unit is blocked by a lower-score predecessor.

### 0.3 MU crosswalk (Scheme A authoritative ↔ Scheme B WSJF ↔ gaps ↔ candidates ↔ approach disposition)

| MU (this roadmap = Scheme A) | Unit name | WSJF (Scheme B id · score) | gaps.md id(s) | candidates.md | approach.md disposition |
|---|---|---|---|---|---|
| **MU-001** | Build & Platform Uplift (the gate) | MU-01 · 4.67 | DEP-001, DEP-006, QUAL-001, ARCH-005 | (replatform; not a strangle) | **Replatform** (gate) |
| **MU-002** | Test-Harness & Coverage Gate (safety net) | MU-03+MU-04 · 5.50/6.50 | OPS-001, OPS-002, DEP-004(test) | infra enabler | Refactor (additive) |
| **MU-003** | Transport Security Hardening (trust-all TLS) | MU-02 · 5.50 | SEC-001, PROC-003(redaction) | CAND-008 · 74 | **Refactor** (security) |
| **MU-004** | Secret-at-Rest (EncryptedSharedPreferences) | MU-08 · 3.50 | SEC-002 | CAND-003 · 84 | **Refactor** |
| **MU-005** | **Scheduler → WorkManager** (spine) | MU-07 · 4.33 | DEP-002, ARCH-003 | CAND-001 · 88 | **Replace dep + scoped Rewrite shell** |
| **MU-006** | Hexagonal ports + ACL | MU-06 · 4.00 | ARCH-001 | (port = the facade for all) | **Refactor** (structural precondition) |
| **MU-007** | Hilt DI Formalization | MU-09(partial) | TECH-005 | CAND-007 · 52 | **Refactor** |
| **MU-008** | Mail ACL & k-9 Unpin | MU-09/MU-11(k-9) | DEP-005, ARCH-001(leak) | CAND-004 · 70 | **Replace coord / Vendor + ACL** |
| **MU-009** | Play Billing 7.x | MU-11(billing) · 2.00 | DEP-004(billing) | CAND-005 · 80 | **Replace version + Refactor 3 files** |
| **MU-010** | Contacts → People API | MU-11(contacts) | (CAP-001 search adjacent) | CAND-006 · 66 | **Replace API + Refactor (evaluate)** |
| **MU-011** | Dead-Code & minSdk Cleanup | MU-11(cleanup) | ARCH-005 | (retire; not a strangle) | **Retire / Decommission** |
| **MU-000** | Preserved Core (NON-UNIT) | — | — | do-not-strangle | **Retain (verbatim)** |

> One substantive content reconciliation, carried forward and respected: the **durable restore checkpoint** is scoped *inside* MU-005 (RestoreTask→RestoreWorker) by `migration-units.md` MU-005, but `prioritized-backlog.md` MU-10 carves it (+ classified retry + server-bounded IMAP search) into a separate later unit. Both are correct at different granularities. This roadmap keeps the **checkpoint** on the MU-005 critical-path terminus (it is free once WorkManager owns durable work-state) and places **classified-retry + server-bounded search** as a distinct **MU-010-reliability** work item in Phase 3 Optimization (they are independent of, and later than, the scheduler swap). This is stated wherever it matters so the granularity choice is auditable.

---

## Executive Summary

### Vision

SMS Backup+ is the inverse of the typical legacy story: **a reference-quality domain core sitting on a rotted dependency substrate** (`approach.md` Exec Summary; `assessment.md` §Maturity — Architecture 3 / Code-Quality 3 but Technology-Currency 1 / DevOps 1). The modernization end-state is a **Play-Store-shippable, secure-by-default, WorkManager-driven Android backup engine** in which the immutable `State` machine, the `DataType` type-object, the typed-exception hierarchy, and the 36-class characterization-test suite are **preserved verbatim** while the dead substrate beneath them — Firebase JobDispatcher, `AsyncTask`, Square Otto, the SHA-pinned k-9 fork, `targetSdk 29`, and the trust-all TLS path — is **replaced behind app-owned Hexagonal ports via branch-by-abstraction**. The benefit is concrete and non-discretionary: the app can ship again, an active MITM exposure is closed, and the supply-chain single-points-of-failure are eliminated — with **zero loss of user-visible function** and every step independently shippable and reversible.

### Timeline Overview

Cadence assumption: **one senior Android engineer**, part-time maintenance pace (README maintenance-mode). Phases map 1:1 to the four-phase template (Foundation / Quick Wins / Core Transformation / Optimization), folding the gaps.md/target-state Phase 0–3 model into it. Durations are engineer-effort envelopes from `prioritized-backlog.md` §6 widened to calendar at part-time cadence.

| Phase (template) | Maps to | Duration (calendar, part-time) | Key Outcomes |
|---|---|---|---|
| **Foundation** | gaps Phase 0 + CI/coverage | ~3–4 wk | Play eligibility restored; CI + JaCoCo coverage gate live; active MITM exposure closed; supply-chain SPOFs de-risked |
| **Quick Wins** | gaps Phase 0.5/1 | ~2–3 wk (overlaps Foundation) | jcenter/dead-code swept; `FLAG_IMMUTABLE` crash pre-empted; EncryptedSharedPreferences; Billing 7.x optional pull-forward |
| **Core Transformation** | gaps Phase 2 | ~5–7 wk | Hexagonal ports; WorkManager owns scheduling (4 dead deps + lifecycle bypass retired); Otto→Flow; Hilt; durable restore checkpoint |
| **Optimization** | gaps Phase 3 | ~3–4 wk (elective) | k-9 unpin behind ACL; classified retry + server-bounded IMAP search; People API (evaluate); Kotlin promotion; `-Werror` fitness fn restored; observability |
| **Total** | — | **~13 wk core (M0–M2); +3–4 wk elective** | Matches `prioritized-backlog.md` "Likely ~9 engineer-weeks core" / "Pessimistic ~13" envelope |

### Investment Summary

No license or infrastructure spend — the toolchain is free (GitHub Actions free tier for public OSS; all target deps Apache-2.0/Android-SDK-License, `target-state.md` Constraints/License). Cost is **engineer-effort only**.

| Category | Estimate | Basis |
|---|---|---|
| Team effort (core M0–M2) | **~9 engineer-weeks** (likely) | `prioritized-backlog.md` §6; ~42 engineer-days on the dependent spine + parallel tracks |
| Team effort (elective Phase 3) | **+3–4 engineer-weeks** | `prioritized-backlog.md` §6; deferrable indefinitely |
| Infrastructure | **$0** | No server tier (`systems.md`); GitHub Actions free for OSS |
| Licenses / Tools | **$0** | All target deps Apache-2.0 / Android-SDK-License (`target-state.md` License row) |
| Training | **$0** (self-directed) | Single maintainer already domain-expert; WorkManager/Flow/Hilt are first-party, well-documented |
| **Total** | **~12–13 engineer-weeks all-in; $0 cash** | |

### Key Risks (top 3; full register in §Risk Management)

1. **Refactoring an unmeasured codebase** (coverage is unknown, 0.33:1 test:prod LOC — `gaps.md` OPS-002). **Mitigation:** the JaCoCo coverage fitness function (MU-002) is a *hard gate* on entering Core Transformation — no seam is swapped before its behavior is pinned by a characterization test (Feathers Ch. 2). This is the single most important governance rule in the roadmap.
2. **`targetSdk` bump surfaces API 30–34 behavioral breakages** (background-execution limits, FGS type, `POST_NOTIFICATIONS`, `FLAG_IMMUTABLE` crash) — High likelihood (`target-state.md` Risk table). **Mitigation:** staged AGP 4.1.3→7.4→8.x; co-land `FLAG_IMMUTABLE` *inside* the gate (QUAL-001) so the gate does not introduce its own crash; per-API conformance gated by MU-002.
3. **Supply-chain SPOF — scijava mirror / JitPack SHA eviction breaks CI mid-modernization** (`assessment.md` DEP-001, Critical). **Mitigation:** Gradle `verification-metadata.xml` in Foundation; MU-005 deletes the scijava mirror dependency early; MU-008 unpins k-9 behind the ACL.

---

## Roadmap Visualization

### High-Level Timeline (single-engineer, part-time; Q-grid is illustrative quarters of effort, not fixed dates)

```
  Foundation        Quick Wins         Core Transformation              Optimization
  + CI/coverage     (overlaps Found.)  (the substrate swap)             (elective)
├──────────────┼──────────────────┼────────────────────────────────┼──────────────────┤
│▓▓▓▓▓▓▓▓▓▓▓▓▓▓│                  │                                │                  │
│ MU-001 gate  │                  │                                │                  │
│ MU-003 sec ∥ │                  │                                │                  │
│ MU-002 CI/cov│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│                                │                  │
│              │ MU-011 deadcode  │                                │                  │
│              │ MU-004 secrets   │                                │                  │
│              │ (MU-009 Billing∥)│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│                  │
│              │                  │ MU-006 ports                   │                  │
│              │                  │ MU-005 WorkManager (spine)     │                  │
│              │                  │ MU-008 mail ACL (after MU-003) │                  │
│              │                  │ MU-006e Otto→Flow              │                  │
│              │                  │ MU-007 Hilt                    │▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│
│              │                  │ +durable restore checkpoint    │ MU-008 k-9 unpin │
│              │                  │                                │ MU-010 retry/srch│
│              │                  │                                │ MU-010 People API│
│              │                  │                                │ Kotlin promotion │
├──────────────┼──────────────────┼────────────────────────────────┼──────────────────┤
│   M0: SHIP   │   M0.5: hygiene  │   M1/M2: substrate modernized  │  M3: hardened    │
   Milestones
```

> Naming note in the diagram: `MU-006` = ports (CAND-006/ARCH-001); `MU-006e` = the *eventing* unit Otto→Flow (Scheme A MU-006 in `migration-units.md` is the eventing unit; the *ports* in Scheme A are folded into MU-006/ARCH-001 of gaps). To avoid the exact collision the two upstream schemes created, **this roadmap uses descriptive labels alongside every MU integer** — "MU-006 (Hexagonal ports)" vs "MU-006e (Otto→Flow eventing)" — see §0.3 crosswalk. The integers are anchored to the gaps/candidates IDs, which are unambiguous.

### Value Delivery Curve (why this sequence front-loads value)

```
Business Value
     ▲
100% │                                          ●──────────  full modernization
     │                                     ●───┘
 80% │                            ●───────┘   (M2: Otto gone, restore durable)
     │                      ●─────┘
 60% │              ●───────┘  (M1: WorkManager; 4 dead deps retired)
     │       ●──────┘
 40% │   ●──┘  (M0.5: hygiene, secrets, FLAG_IMMUTABLE)
     │ ●─┘
 20% │●  ◀── M0 alone restores Play eligibility + closes active MITM (the two
     │       non-discretionary outcomes) — steepest value step is FIRST
   0%└────────────────────────────────────────────────────────────►  effort
       Foundation   QuickWins   Core Transformation    Optimization
```

**The curve is deliberately front-loaded.** M0 (Foundation) alone delivers the two *non-discretionary* outcomes — shippability and the active-security fix — so the highest-value step is the first step and is independently valuable even if the program stops there (`approach.md` §Alternative-if-Conditions-Not-Met). This is the WSJF cost-of-delay logic made visual: the units that unblock distribution and close active harm have the highest cost-of-delay and are scheduled earliest.

---

## Phase 1: Foundation

**Duration:** ~3–4 wk (part-time) · **Investment:** ~2–3 engineer-weeks · **Theme:** Restore shippability, stand up the safety net, close the active security exposure. *Maps to gaps.md Phase 0 + the CI/coverage gates.*

### Objectives

1. Make the app **Play-eligible again** (the dominant business driver — `assessment.md` Drivers, `target-state.md` ADR-006).
2. Stand up the **regression safety net** (CI + JaCoCo coverage fitness function) *before* any refactor touches a seam (Feathers Ch. 2; `target-state.md` Technology Principle #4).
3. **Close the active MITM exposure** (trust-all TLS) — independent of the SDK gate, so it runs day-1 in parallel.

### Work Items

#### F1 / MU-001: Build & Platform Uplift — the universal gate (Replatform)

| Attribute | Value |
|---|---|
| **Priority** | Critical (universal gate) · **WSJF** 4.67 |
| **Effort** | L (~12 engineer-days; stage AGP in 3 commits) |
| **Owner** | Maintainer |
| **Dependencies** | None upstream · **co-requisite MU-002** (Robolectric 4.3.1 caps SDK≤29 — moves in the same build transaction) |
| **Blocks** | MU-003*, MU-004, MU-005, MU-006, MU-008, MU-009, MU-011, and *all distribution* |

**Description:** compile/target SDK 29→35, minSdk 14→21, AGP 4.1.3→7.4→8.x (staged), Gradle 7.2→8.x, build-tools 34.x; remove `jcenter()`×2 + bintray; make the four `<receiver>` elements `android:exported`-explicit; add `POST_NOTIFICATIONS` + `FOREGROUND_SERVICE_DATA_SYNC`; **co-land `FLAG_IMMUTABLE`** on the two `PendingIntent` sites (`AlarmManagerDriver.java:127`, `ServiceBase.java:204` — line corrected to 204 per `approach.md` §Source-Verification). Disposition: **Replatform** (`approach.md` Component L) — not Refactor (no logic), not Rewrite (no code).

**Scope:**
- [ ] AGP 4.1.3→7.4→8.x in three independently-buildable commits; Gradle wrapper to 8.x.
- [ ] compile/target SDK 35, minSdk 21, build-tools 34.x.
- [ ] Remove `jcenter()` (build.gradle:4,21) + `jcenter.bintray.com` (:25); verify resolution against google/mavenCentral/jitpack.
- [ ] `android:exported` on all four receivers; add `POST_NOTIFICATIONS` + `FOREGROUND_SERVICE_DATA_SYNC`.
- [ ] `FLAG_IMMUTABLE` on both PendingIntent sites (QUAL-001 — co-land or the gate *creates* an API-31+ crash).

**Success Criteria:**
- [ ] A signed AAB with targetSdk ≥ 34 builds, passes lint, and is accepted by Play pre-launch review.
- [ ] No `jcenter()`/bintray declaration remains; clean build resolves with the removed repos gone.
- [ ] No `IllegalArgumentException` on the API 31+ scheduling path (emulator check).

**Risks:** | Risk | Mitigation | |---|---| | API 30–34 behavioral breakage (High) | Staged AGP; per-API conformance gated by MU-002; `FLAG_IMMUTABLE` co-landed | | `-Werror` blocks build mid-bump (High likelihood) | Relax to lint-baseline during the bump; restore as a fitness function in Phase 4 (PROC-003) |

**Rollback:** The staged 3-commit AGP path means any commit is independently revertible to the last buildable state; minSdk/SDK raise is a single revert.

#### F2 / MU-002: Test-Harness & Coverage Gate — the safety net (additive Refactor)

| Attribute | Value |
|---|---|
| **Priority** | Critical — **hard gate on entering Core Transformation** · **WSJF** 6.50 (highest in the program) |
| **Effort** | M |
| **Owner** | Maintainer |
| **Dependencies** | **co-requisite MU-001** (Robolectric); CI part has none |
| **Blocks** | MU-005, MU-006, MU-008, MU-004, MU-006e, durable-checkpoint — *every refactor* |

**Description:** Stand up GitHub Actions CI (`./gradlew test lint jacocoTestReport assembleRelease` on every PR) + `gradle/verification-metadata.xml`; upgrade the test toolchain (Robolectric 4.3.1→4.12+, mockito-all 1.10.17→mockito-core 5.x, junit 4.12→4.13.2 [CVE-2020-15250], truth 0.39→1.4.x); wire JaCoCo + a coverage fitness function (≥70% on `service`/`mail`/`auth`); **backfill characterization tests on the high-risk untested paths *before* they are touched** — `AuthPreferences.migrate()`/`getStoreUri()` (protects MU-003), `BackupJobs` retry 30/300 + constraints (protects MU-005), `MainActivity` branches (TD-001), `OAuth2Client` (TD-003); fill the empty `SmsBackupServiceTest:212` body.

**Success Criteria:**
- [ ] CI green on every PR; `verification-metadata.xml` enforced.
- [ ] JaCoCo fails CI below the agreed engine-package threshold.
- [ ] `AuthPreferences.migrate()` pinned by a test *before* MU-003 begins; `BackupJobs` retry/constraints pinned *before* MU-005 begins.
- [ ] No empty/TODO test bodies remain.

**Risks:** | Risk | Mitigation | |---|---| | Robolectric 4.3.1 hard-blocks the SDK bump | Treated as a co-requisite of MU-001 — one combined build sweep, not a sequence | **Rollback:** Additive only — no production behavior changes; the coverage gate is a fitness function, not a runtime artifact.

#### F3 / MU-003: Transport Security Hardening — trust-all TLS removal (Refactor, security)

| Attribute | Value |
|---|---|
| **Priority** | Critical (active MITM exposure) · **WSJF** 5.50 · **CAND-008** |
| **Effort** | M | **Dependencies** | MU-002 (characterization-test `migrate()` first); **parallel to MU-001 from day 1** — no SDK dependency | **Blocks** | MU-008 (ACL wraps the validated transport) |

**Description:** Delete `AllTrustedSocketFactory`; replace the trust-all path with validated TLS + an explicit user-pinned-cert opt-in for self-hosted IMAP; rewrite `AuthPreferences.migrate()` so it never silently sets `SERVER_TRUST_ALL_CERTIFICATES=true` (an active CWE-295/312 exposure the SDK upgrade *worsens*); centralize token/URI redaction (closes ARCH-010). Disposition **Refactor** (`approach.md` Component F).

**Migration Strategy (security-first deletion, not a binding flip):**
```
Day 1-2: characterization-test migrate()/getStoreUri() (via MU-002 backfill)
Day 3-4: delete AllTrustedSocketFactory; validated-TLS-only + user-pinned-cert path
Day 5-6: rewrite migrate() (preserve validated TLS + one-time consent notice); centralize redaction
Ship:    independently shippable in Foundation; no parallel-run needed (one process)
```

**Success Criteria:** [ ] No code path accepts an unvalidated cert (CWE-295 closed) · [ ] `migrate()` preserves validated TLS + one-time notice; never silently downgrades · [ ] `BackupImapStoreTest` asserts the validated/pinned factory.

**Rollback Plan:** Single revert of the deletion + migration rewrite; the backfilled `AuthPreferencesTest` gates the change so a regression is caught pre-merge. Trade-off accepted: the small self-signed-IMAP user set must re-pin a cert (one-time notice; ADR-documented).

### Phase 1 Milestones

| Milestone | Criteria | Status |
|---|---|---|
| M1.1: Play-eligible build | AAB accepted at targetSdk 35; no API-31+ scheduling crash | ⬜ |
| M1.2: CI + coverage gate live | CI green on every PR; JaCoCo threshold enforced | ⬜ |
| M1.3: Active MITM exposure closed | grep verifies `AllTrustedSocketFactory` removed; `migrate()` never silent-downgrades | ⬜ |

### Phase 1 Exit Criteria (= Milestone **M0**, independently shippable)

- [ ] App ships to Play again (targetSdk ≥ 34).
- [ ] CI operational with JaCoCo coverage fitness function enforced.
- [ ] Trust-all TLS path removed; migration never silently downgrades.
- [ ] Supply-chain SPOFs reduced: jcenter gone, `verification-metadata.xml` in place.
- [ ] Characterization tests backfilled on every path the next phase will touch.

> **This phase alone restores distribution viability and closes the active security exposure** — the two non-discretionary outcomes. If the program stops here, it has still delivered the highest-value work (`approach.md` §Alternative).

---

## Phase 2: Quick Wins

**Duration:** ~2–3 wk, **overlapping Foundation** · **Investment:** ~1–2 engineer-weeks · **Theme:** Low-risk, high-reversibility value that builds momentum without touching the engine spine. *Maps to gaps Phase 0.5/1.*

### Objectives

1. Remove provably-dead code made dead by the minSdk raise (opportunistic, near-zero risk).
2. Harden secrets at rest behind a clean single seam (highest interface-clarity strangle in the codebase — CAND-003 score 84).
3. (Optional pull-forward) De-risk the donation flow against Play's billing-API deadline.

### Work Items

#### Q1 / MU-011: Dead-Code & minSdk Cleanup (Retire / Decommission)

| Attribute | Value |
|---|---|
| **Priority** | Low · **Effort** | S · **Dependencies** | MU-001 (minSdk 14→21 makes the API-level branches provably dead) · **Approach** | Retire (CAND not-a-strangle; `approach.md` Component K) |

**Current → Target:** `CalendarAccessorPre40` + factory branch (unreachable at minSdk 21) and sub-minSdk inline branches → deleted; collapsed compat surface. **EXCLUDED:** migration-coupled deletions (SmsJobService, AlarmManagerDriver, Otto POJOs, AllTrustedSocketFactory) are owned by their units (MU-005/006e/003), not duplicated here.

**Implementation:** Fold the `Pre40` removal into the MU-001 minSdk-raise sweep; collapse `DataType.java:21`, `TokenRefresher.java:82`, `CalendarAccessor.java:49-51` inline branches. **Caveat (carried from `migration-units.md` MU-011 risk):** the KitKat (API 19) else-branches touch restore write-permission/default-SMS compat — review before removing; `AuthMode.XOAUTH` is LIVE for legacy users — **do not remove until MU-004 migrates legacy creds**.

**Success Criteria:** [ ] `CalendarAccessorPre40` deleted, factory collapsed, both call sites updated, `CalendarSyncerTest` green · [ ] no dead else-branch warnings · [ ] `AuthMode.XOAUTH` retained (deferred). **Rollback:** pure deletion — single revert.

#### Q2 / MU-004: Secret-at-Rest Hardening — EncryptedSharedPreferences (Refactor)

| Attribute | Value |
|---|---|
| **Priority** | Should · **WSJF** 3.50 · **CAND-003** 84 (cleanest port boundary) · **Effort** | M · **Dependencies** | MU-001 (Keystore ergonomics ≥ API 23), MU-002 (round-trip test) · **Approach** | Refactor behind `SecretStore` port |

**Current → Target:** plaintext `SharedPreferences("credentials")` → `EncryptedSharedPreferences` (Jetpack Security, Keystore AES-256) behind a `SecretStore` port; single `getCredentials()` seam.

**Migration Strategy (port-first with one-time data migration — the on-device analog of strangler data-sync, one-shot not continuous):**
```
Bind SecretStore; first read falls through to plaintext → migrates → serves encrypted thereafter.
Preserve the credentials.xml backup-exclusion. Reversible by re-binding the plaintext adapter
until the plaintext store is cleared (then a re-auth is required).
```

**Success Criteria:** [ ] credentials AES-256/Keystore-backed (CWE-312 closed) · [ ] `credentials.xml` remains backup-excluded · [ ] one-time migration leaves no plaintext; round-trip test passes. **Rollback:** adapter binding flip back to plaintext (with re-auth) until plaintext cleared.

#### Q3 (optional pull-forward) / MU-009: Play Billing 7.x (Replace version + contained Refactor)

| Attribute | Value |
|---|---|
| **Priority** | Low (deferrable to Phase 4) but **pull-forward eligible** — isolated, highest reversibility (CAND-005 90 risk-score) · **Effort** | M · **Dependencies** | MU-001 only · **Approach** | Replace 2.1.0→7.x; rewrite 3 donation files |

**Rationale for *optional* placement here:** self-contained (3 files, no engine coupling), failure cannot cascade past the donation screen, and the Play billing-API deadline is a forward-policy risk. It is a *quick win* only if the maintainer wants to clear the deadline early; otherwise it defers cleanly to Optimization. **Success:** [ ] `ProductDetails`/`queryProductDetailsAsync`; no `SkuDetails` · [ ] `DonationActivityTest` green on Billing 7.x.

### Phase 2 Milestones

| Milestone | Criteria | Status |
|---|---|---|
| M2.1: Compat surface swept | `CalendarAccessorPre40` gone; no dead-branch warnings | ⬜ |
| M2.2: Secrets encrypted at rest | round-trip + one-time migration test green; no plaintext post-migration | ⬜ |

### Phase 2 Exit Criteria (= Milestone **M0.5**)

- [ ] Dead/compat code removed (XOAUTH deferred per gate); minSdk-21-clean.
- [ ] Credentials encrypted at rest behind the `SecretStore` seam.
- [ ] (If pulled forward) Billing on 7.x ahead of the Play deadline.
- [ ] Coverage threshold held through every change (MU-002 gate).

---

## Phase 3: Core Transformation

**Duration:** ~5–7 wk · **Investment:** ~5–6 engineer-weeks · **Theme:** Swap the dead substrate behind ports — the critical-path spine. *Maps to gaps Phase 2.* **Team:** 1 senior engineer (no parallel streams available at one-maintainer cadence; "streams" below are logical, executed serially with the noted parallel-eligible tails).

### Objectives

1. Introduce the **Hexagonal ports** that convert every substrate swap from a vendor-type edit into a reversible binding flip.
2. Migrate **scheduling/worker to WorkManager** — the single largest-leverage move (retires 4 dead deps + the lifecycle bypass).
3. Replace **Otto with Flow**, formalize DI with **Hilt**, and land the **durable restore checkpoint**.

### Work Streams

#### Stream A — Structural precondition (MU-006: Hexagonal ports + ACL)

| Work Item | Effort | Dependencies | Disposition |
|---|---|---|---|
| MU-006: four ports (`BackupScheduler`, `MailTransport`+ACL, `SecretStore`, `Contacts/CalendarPort`) | L | MU-001, MU-002 | Refactor (additive structure) |

**Why first in this phase (overrides raw WSJF):** the ports are the *structural precondition* (`prioritized-backlog.md` §3; `gaps.md` ARCH-001) that makes MU-005/008/004/011 mechanical and reversible. The existing `Driver` Strategy (`BackupJobs.java:66-72`) is the ready-made first-adapter slot — the indirection is already 50% built (`candidates.md` Key Finding). Each port lands as a Feathers "Introduce Instance Delegator" wrap with no behavior change, gated by a characterization test.

#### Stream B — The spine (MU-005: Scheduler → WorkManager) — **critical-path "MU-04" referent**

| Work Item | Effort | Dependencies | Disposition |
|---|---|---|---|
| MU-005: `BackupScheduler`→`WorkManagerScheduler`; `BackupTask`/`RestoreTask` `AsyncTask`→`CoroutineWorker`; retire jobdispatcher + scijava mirror + AsyncTask + AlarmManager fallback + `SmsJobService` lifecycle bypass | L (split-eligible: MU-005a port+first-adapter / MU-005b WorkManager swap+deletions) | MU-006, MU-002, MU-001 | **Replace dep + scoped Rewrite of the AsyncTask shell** |

**Branch-by-Abstraction cutover (the in-process strangler analog — `candidates.md` CAND-001):**
```
Phase 1 (facade, no behavior Δ):  Engine → BackupScheduler(port) → BackupJobs (first adapter)  [ship + verify]
Phase 2 (new adapter, dark):      Engine → BackupScheduler(port) → {BackupJobs | WorkManagerScheduler}  [both compile]
Phase 3 (flip + delete):          Engine → BackupScheduler(port) → WorkManagerScheduler  [delete jobdispatcher :60, scijava mirror, AlarmManagerDriver, SmsJobService, manifest <service>/ACTION_EXECUTE]
Risk mitigation: Parallel-Run the new scheduler against the old in a DEBUG build before cut-over.
```
**Contract preserved line-for-line** (`target-state.md` §Scheduling Contract Mapping): `setReplaceCurrent(true)`→`enqueueUniqueWork(REPLACE)`; `RETRY_POLICY_EXPONENTIAL 30,300`→`setBackoffCriteria(EXPONENTIAL,30s)`; `ON_UNMETERED_NETWORK`→`NetworkType.UNMETERED`; `contentUriTrigger`→`addContentUriTrigger` (API 24+) with `SmsBroadcastReceiver`→enqueue fallback below 24. The public `com.zegoggles.smssync.BACKUP` broadcast contract is **PRESERVED — Hard Requirement**. The lone genuine **Rewrite** in the program — `AsyncTask`→`CoroutineWorker` shell, forced because `AsyncTask` is *removed* at API 33 — is contained here, shell-only, re-hosting the preserved `State` machine verbatim (`approach.md` Component D, Condition-for-Success #3).

#### Stream C — Eventing, DI, and data integrity (after the engine is on WorkManager, to avoid double-churn)

| Work Item | Effort | Dependencies | Disposition |
|---|---|---|---|
| MU-008: Mail ACL & k-9 ACL boundary (translation layer; State.java magic-string removal) | M | MU-006, MU-002, **MU-003 first** | Refactor (the k-9 *unpin* itself defers to Optimization) |
| MU-006e: Otto → `StateFlow`/`SharedFlow` behind `SyncStateRepository` (+ `MainActivity`→ViewModel) | M(–L) | MU-005 (avoid double-churn), MU-002 | Replace dep (first-party Flow) + Refactor 11-file wiring |
| MU-007: Hilt DI formalization | M | MU-005, MU-006e (graph must be stable) | Refactor |
| Durable restore checkpoint (MU-005 terminus) | M | MU-005 (consumes WorkManager durable work-state) | Refactor (reliability) |

**Architecture Evolution (this phase):**
```
Current                          Intermediate                       Target
┌────────────────────┐          ┌────────────────────┐            ┌────────────────────┐
│ Engine → vendor SDK │   ──►    │ Engine → PORT →     │    ──►     │ Engine → PORT →     │
│ types directly      │          │ {old | new} adapter │            │ new adapter only;   │
│ (jobdispatcher,k-9, │          │ (both compile;      │            │ dead deps DELETED;  │
│  Otto, AsyncTask)   │          │  binding = old)     │            │ Otto→Flow; Hilt DI  │
└────────────────────┘          └────────────────────┘            └────────────────────┘
   Preserved State machine is RETAINED VERBATIM through all three columns (MU-000).
```

### Phase 3 Milestones

| Milestone | Criteria | Status |
|---|---|---|
| M3.1: Ports landed | four ports as no-behavior-change wraps; each gated by a characterization test | ⬜ |
| M3.2: WorkManager owns scheduling | jobdispatcher/scijava/AsyncTask/AlarmManager/bypass retired; Parallel-Run shows 0 missed/dup backups; `BACKUP` broadcast still triggers | ⬜ |
| M3.3: Eventing + DI modernized | Otto dep deleted (0 imports); sticky last-state preserved; Hilt graph explicit; test-only constructors deleted | ⬜ |
| M3.4: Restore is durable | interrupted-restore test shows 0 loss/0 duplicate | ⬜ |

### Phase 3 Exit Criteria (= Milestones **M1** + **M2**)

- [ ] All four ports in place; every dead dependency sits behind an app-owned interface.
- [ ] 0 abandoned scheduling deps; WorkManager owns scheduling with the contract preserved line-for-line.
- [ ] Otto dependency deleted; status UI keeps sticky last-state via `StateFlow.value`.
- [ ] Hilt DI graph explicit; Service-Locator `new Preferences(this)` calls removed.
- [ ] Durable restore checkpoint: interrupted restore neither loses nor duplicates messages.
- [ ] Coverage threshold held through every refactor (MU-002 gate, continuously).

---

## Phase 4: Optimization

**Duration:** ~3–4 wk (**elective — deferrable indefinitely without blocking M0–M2 value**) · **Investment:** ~3–4 engineer-weeks · **Theme:** Strategic hardening, supply-chain closure, performance. *Maps to gaps Phase 3.*

### Objectives

1. Close the **k-9 supply-chain SPOF** (unpin behind the now-existing ACL).
2. **Performance + reliability** beyond the checkpoint: classified retry, server-bounded IMAP search.
3. **Strategic hardening:** People API (evaluate), Kotlin promotion of the core, `-Werror` fitness function restored, observability.

### Work Items

#### O1 / MU-008 (completion): k-9 Unpin behind the ACL (Replace coordinate / Vendor)

**Approach (`approach.md` Component B — the most-scrutinized rewrite candidate, REJECTED for from-scratch rewrite):** with the `MailTransport` ACL already in place (Phase 3), replace the JitPack SHA `eaf689025e` with a maintained versioned coordinate **if an API-compatible release exists (EVALUATE, do not assume)**; else **vendor the k-9 IMAP subset** (Asset Capture). **NEVER a from-scratch IMAP/MIME rewrite** — that scored 12/100 (the protocol edge-case knowledge is safety-critical and field-only). **Decision gate G-k9 (§Governance):** if neither coordinate nor vendor is viable and a rewrite is ever proposed, **escalate** — that is the one scenario that breaks the program's risk profile (`approach.md` Condition-for-Success #2).

#### O2 / MU-010-reliability: Classified retry + server-bounded IMAP search

**Target Improvements (`gaps.md` ARCH-006 / CAP-001; `target-state.md` Perf QAS):**
| Metric | Current | Target | Approach |
|---|---|---|---|
| Restore peak memory | O(mailbox) — `UID SEARCH 1:*` + in-memory sort before cap | O(page size) | server-side `SORT`/`SINCE`/UID windowing |
| Failure handling | `catch(MessagingException)→ERROR` (transient=permanent) | classified retry-with-jitter + optional IMAP Circuit Breaker | transient/permanent classifier (Nygard *Release It!* Ch.5) |

**Note:** the server-bounded-search half is **independent/parallel** (no scheduler dependency) and could be pulled into Phase 3 as a parallel tail if capacity allows.

#### O3 / MU-010-contacts: Contacts → People API (Replace API, EVALUATE first)

Replace the legacy GData `m8/feeds/` call behind `ContactsPort`; **verify the GData endpoint liveness and that the OAuth2 contacts/calendar path is not broken before committing** (`target-state.md` Constraints — Gmail policy). Confidence Medium (endpoint-liveness contingent). Close the `OAuth2Client.java:145` token-log redaction cross-cut (ARCH-010) regardless of whether the People API migration proceeds.

#### O4: Strategic hardening (Kotlin promotion, `-Werror` fitness fn, observability)

- Promote `State`→Kotlin `sealed interface` (behavior-preserving; future MU-012; `approach.md` Component E — optional, Phase 3+).
- Restore `-Werror` as a *managed deprecation-debt fitness function* with staged removal (PROC-003).
- Add WorkManager `WorkInfo` observability (replaces static `isServiceWorking()`); opt-in, ADR-documented crash reporting.

### Phase 4 Exit Criteria (= Milestone **M3**)

- [ ] 0 abandoned dependencies total; k-9 on a reproducible coordinate or vendored.
- [ ] Interrupted/large-mailbox restore: memory bounded by page size; classified retry replaces blanket `→ERROR`.
- [ ] `-Werror` restored as a fitness function with staged deprecation removal.
- [ ] (If pursued) People API behind `ContactsPort`; OAuth2 contacts/calendar path intact; token redaction closed.

---

## Governance Framework

### Decision Gates

| Gate | Timing | Decision | Approver |
|---|---|---|---|
| **G1: Foundation Complete (M0)** | End Phase 1 | Proceed to Quick Wins / Core. **Pass iff** AAB Play-accepted + CI/JaCoCo enforced + trust-all gone. | Maintainer |
| **G2: Safety-net adequacy** | Before *first* seam swap | **Hard gate:** no MU-005/006/006e/008 begins until the path it touches is pinned by a characterization test and coverage ≥ threshold. | Maintainer (self-attest against JaCoCo) |
| **G3: WorkManager cut-over** | Mid Phase 3 (MU-005 Phase-2→3) | Flip binding only after Parallel-Run (debug build) shows 0 missed/duplicate backups. | Maintainer |
| **G-k9: k-9 disposition** | Start of O1 | Coordinate vs. Vendor vs. **escalate** (never rewrite). Escalate if neither is viable. | Maintainer |
| **G4: Core Complete (M2)** | End Phase 3 | Proceed to elective Optimization or **stop with full core value banked**. | Maintainer |
| **G5: Initiative close (M3)** | End Phase 4 | Close. | Maintainer |

### Health Metrics (DORA-aligned + modernization-specific, from `assessment.md` Success Metrics)

| Metric | Baseline | Target | Status signal |
|---|---|---|---|
| Deployment frequency | **Blocked** (cannot ship to Play) | On-demand post-M0 | 🔴→🟢 at M0 |
| targetSdk vs. Play floor | 29 (below 34) | ≥ 34 | 🔴→🟢 at M0 |
| Measured coverage (service/mail/auth) | Unknown (0.33:1 LOC) | ≥ 70% (fitness fn) | 🔴→🟢 at M0 (gate live) |
| Abandoned dependencies | 3 (Otto, jobdispatcher, k-9 SHA) | 0 | 🔴→🟢 by M2/M3 |
| Trust-all TLS code paths | 1 (active) | 0 | 🔴→🟢 at M0 |
| Supply-chain mirrors / SHA pins | 1 mirror + 1 SHA | 0 | 🔴→🟡(M0)→🟢(M1/M3) |
| Build success rate (CI) | Unmeasured (no CI) | > 95% | 🟢 once CI live |

### Escalation Matrix

| Trigger | Action | Owner |
|---|---|---|
| Coverage gate cannot be stood up before refactors | **Narrow scope to M0 + MU-003 only** (`approach.md` §Alternative); defer all substrate swaps | Maintainer |
| scijava/JitPack outage breaks CI mid-program | Accelerate MU-005 (deletes the mirror) / MU-008 (unpins k-9); use `verification-metadata.xml` cache | Maintainer |
| k-9: no compatible coordinate AND vendoring infeasible | **Escalate — do NOT rewrite the IMAP client** (catastrophic risk) | Maintainer → seek contributor help |
| `targetSdk` bump reveals unfixable behavioral break | Stage back one API level; isolate the breaking surface behind a guarded path | Maintainer |
| Scope creep on the AsyncTask→CoroutineWorker rewrite | Enforce shell-only discipline (Second-System guard, Condition #3) | Maintainer |

---

## Resource Plan

### Team Structure

There is no team — this is a **single-maintainer OSS project in maintenance mode** (`target-state.md` Constraints; README). The "resource plan" is therefore an *effort-allocation and skills* plan for one senior Android engineer, not an org chart. The most important resource decision is **sequencing to keep every phase independently shippable**, so the maintainer can stop after any milestone with banked value (`assessment.md` Risk — "single maintainer / modernization stalls").

### Effort Allocation by Phase (engineer-weeks; one engineer)

| Activity | Foundation | Quick Wins | Core Transformation | Optimization |
|---|:---:|:---:|:---:|:---:|
| Build/SDK/CI | 1.5 | — | — | 0.5 (-Werror) |
| Security | 1.0 | 0.5 | 0.5 (ACL) | 0.5 (redaction/observability) |
| Scheduling/engine | — | — | 3.0 | — |
| Eventing/DI | — | — | 1.5 | — |
| Reliability/perf | — | — | 0.5 (checkpoint) | 1.5 |
| Cleanup/strategic | — | 0.5–1.0 | — | 1.0 (k-9, Kotlin) |
| **Total (eng-weeks)** | **~2.5** | **~1–1.5** | **~6** | **~3–4** |

### Skills Matrix

| Skill | Required for | Maintainer has? | Plan if gap |
|---|---|---|---|
| Gradle/AGP migration | MU-001 | Likely (long-time maintainer) | Self-directed; staged AGP path de-risks |
| WorkManager + coroutines | MU-005 | **Acquire** | First-party docs; Parallel-Run validates correctness |
| Feathers characterization testing | MU-002 (gate) | Partial (36-test suite exists) | The discipline, not the tool, is the gap — codify the "test before swap" rule |
| Hilt | MU-007 | **Acquire** | Formalizes seams the code already builds by hand — low conceptual gap |
| Kotlin Flow | MU-006e | **Acquire** | Rides in with WorkManager (Kotlin-first) — carrier, not standalone track |
| IMAP/k-9 internals | MU-008 | Deep (years of integration) | This is the asset being protected; the ACL contains it |

---

## Risk Management

### Risk Register (scored Likelihood×Impact, 1–4 each → score; from `assessment.md`/`target-state.md`/`prioritized-backlog.md` risk tables)

| ID | Risk | Likelihood | Impact | Score | Mitigation | Owner |
|---|---|---|---|:---:|---|---|
| R1 | Refactoring an unmeasured codebase regresses silently | Med (3) | High (4) | **12** | MU-002 coverage fitness function is a HARD gate (G2) before any swap; characterization tests backfilled first | Maintainer |
| R2 | `targetSdk` bump surfaces API 30–34 behavioral breakage | High (4) | Med (3) | **12** | Staged AGP; co-land `FLAG_IMMUTABLE`; per-API conformance gated by MU-002 | Maintainer |
| R3 | scijava/JitPack supply-chain eviction breaks CI mid-program | Med (3) | High (4) | **12** | `verification-metadata.xml` (Foundation); MU-005 deletes mirror early; MU-008 unpins k-9 | Maintainer |
| R4 | WorkManager lifecycle differs → missed/duplicate scheduled backups | Med (3) | High (4) | **12** | Characterization-test retry/constraints first; Parallel-Run new vs old in debug build (G3) | Maintainer |
| R5 | k-9 has no compatible coordinate; rewrite tempting | Med (3) | Critical (4) | **12** | ACL makes coordinate/vendor swappable; **escalate, never rewrite** (G-k9) | Maintainer |
| R6 | Single-maintainer stall | Med (3) | Med (3) | **9** | Every phase independently shippable; M0 alone banks the non-discretionary value | Maintainer |
| R7 | Removing trust-all breaks self-signed-IMAP users | Med (3) | Med (2) | **6** | One-time consent notice + user-pinned-cert path; ADR-documented | Maintainer |
| R8 | AsyncTask→CoroutineWorker rewrite scope-creeps | Low (2) | Med (3) | **6** | Shell-only discipline; re-host State machine verbatim (Condition #3) | Maintainer |
| R9 | `-Werror` blocks build mid-migration | High (4) | Low (1) | **4** | Lint-baseline during migration; restore `-Werror` as a fitness function in Phase 4 | Maintainer |

### Contingency Plans

**If the test net cannot be stood up before refactors (R1 cannot be mitigated):**
- Narrow scope to **M0 (Foundation) + MU-003 only** — restore Play eligibility and close the MITM exposure; **defer all substrate swaps** (`approach.md` §Alternative). Do *not* attempt swaps without the net; do *not* substitute a rewrite under schedule pressure (a rewrite is slower to first shippable value, not faster).

**If Core Transformation stalls (R6):**
- Stop at the last completed milestone. M0/M0.5/M1/M2 are each independently shippable with banked value. No big-bang exists to leave half-done.

**If k-9 has no viable maintained coordinate (R5):**
- Vendor the IMAP subset (Asset Capture) behind the ACL. If even vendoring is infeasible, **escalate and keep the SHA pin contained behind the ACL + `verification-metadata.xml`** rather than rewrite — the contained risk is far smaller than a from-scratch IMAP client.

---

## Communication Plan

For a single-maintainer OSS project, "communication" is **changelog + ADR discipline + release notes**, not stakeholder meetings.

| Audience | Content | Frequency | Channel |
|---|---|---|---|
| Users (Play/F-Droid) | Migration-affecting changes (trust-all removal re-pin notice; credential migration) | Per release | Release notes + in-app one-time notice |
| Contributors / future maintainers | ADRs (WorkManager, Otto→Flow, k-9 ACL, secrets) | Per decision | `docs/adr/` (carry forward the 8 ADRs from `target-state.md`) |
| Third-party `BACKUP` broadcast consumers | Contract is PRESERVED — no comms needed unless it ever changes | Only on change | README integration section |

---

## Success Criteria

### Business Success

| Metric | Baseline | Target | Measurement |
|---|---|---|---|
| Play distribution | Blocked | Shippable | AAB accepted at targetSdk ≥ 34 |
| User function preserved | 100% | 100% (zero regression) | Feature-parity table (`target-state.md` §Feature Parity) holds across M0–M2 |
| Privacy posture | On-device, no telemetry | Unchanged (any crash reporting opt-in) | 0 new mandatory network endpoints |

### Technical Success

| Metric | Baseline | Target | Measurement |
|---|---|---|---|
| Abandoned deps | 3 | 0 | dep manifest audit |
| targetSdk | 29 | ≥ 35 | `app/build.gradle` |
| Measured coverage (engine pkgs) | Unknown | ≥ 70% | JaCoCo |
| Trust-all TLS paths | 1 active | 0 | grep `AllTrustedSocketFactory` |
| Supply-chain mirrors/SHA | 1 + 1 | 0 | repo block audit |
| Restore data integrity | loss/dup possible on kill | RPO ≈ 0 | interrupted-restore test |

### Team (Maintainer) Success

| Metric | Baseline | Target | Measurement |
|---|---|---|---|
| Engine unit-testable without static rigging | No (Otto/static) | Yes | tests run without static bus setup |
| Test-only dual constructors | present | 0 (Hilt) | grep |
| Modern-stack familiarity | Java-8-only | Kotlin/Flow/WorkManager/Hilt | seams migrated |

---

## Appendices

### A: Critical Path & Dependency Summary

```
MU-001 (gate, ~12d) ─► MU-002 (test+coverage, co-req) ─► MU-006 (ports, ~8d)
   ─► MU-005 (WorkManager, ~12d) ─► durable restore checkpoint (terminus)

Parallel day-1 tracks (do NOT extend the critical path):
   MU-003 (trust-all TLS, security)   ∥   MU-002-CI (GitHub Actions)
Hard gate on entering Core: MU-002 coverage fitness function.
```
Critical-path length ≈ 42 engineer-days on the spine (`prioritized-backlog.md` §3), ≈ 8–9 calendar weeks at sustainable single-engineer cadence; consistent with the `assessment.md` 6–9 engineer-week core envelope.

### B: Phase ↔ Milestone ↔ MU ↔ gaps Phase Map

| Template Phase | Milestone | MUs | gaps.md Phase |
|---|---|---|---|
| Foundation | M0 | MU-001, MU-002, MU-003 | Phase 0 + CI/coverage |
| Quick Wins | M0.5 | MU-011, MU-004, (MU-009 opt) | Phase 0.5 / 1 |
| Core Transformation | M1, M2 | MU-006, MU-005, MU-008(ACL), MU-006e, MU-007, checkpoint | Phase 2 |
| Optimization | M3 | MU-008(k-9 unpin), MU-010(retry/search/People API), Kotlin, -Werror, observability | Phase 3 |

### C: Technology Decisions (carried from `target-state.md` ADR-001..008; do not re-litigate)

| Decision | Choice | Rejected | Rationale |
|---|---|---|---|
| Background execution | WorkManager | bare JobScheduler; FGS-only | Subsumes JobScheduler+AlarmManager; durable work-state; owns FG promotion (ADR-001) |
| Eventing | StateFlow/SharedFlow | Greenrobot EventBus | First-party primitive *removes* a dep vs. swapping one anti-pattern bus for another (ADR-003) |
| DI | Hilt | Koin; manual DI | Compile-time-verified; formalizes existing Humble-Object seams (ADR-008) |
| k-9 | versioned coordinate / vendor behind ACL | from-scratch IMAP rewrite | Protocol edge-case knowledge is safety-critical; rewrite scored 12/100 (ADR-004; approach.md Component B) |
| Macro-architecture | layered monolith + Hexagonal ports | microservices; full Kotlin rewrite | No backend exists; rewrite discards the 35-class safety net for zero user value (ADR target-state §Architecture Style) |

### D: Glossary

| Term | Definition |
|---|---|
| Branch-by-Abstraction | Fowler's in-process strangler analog: introduce a port, make the existing impl the first adapter (no behavior change), add the new adapter, flip the DI binding, delete the old. The "facade" is the port; the "routing decision" is the binding. |
| Characterization test | Feathers (Ch. 2): a test that pins *current* behavior before a refactor, so the refactor's safety can be verified. The gate before every swap. |
| Preserved Core (MU-000) | The reference-quality domain logic (immutable State machine, DataType, exception hierarchy, converters) that all units protect and none may behaviorally change. |
| Universal gate (MU-001) | The build/SDK uplift that blocks all distribution and 8 downstream units; nothing ships until it lands. |

---

## Artifacts Consulted

| Artifact | Path | Purpose |
|---|---|---|
| Roadmap prompt (Step 06) | assessments/prompts/modernization/06-modernization-roadmap.md | Required output structure (phases, governance, value curve) |
| Expert-Exceeding Depth | knowledge/standards/expert-exceeding-depth.md | Quality bar |
| Prioritized Backlog (Step 2.5.7) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/prioritized-backlog.md | WSJF scores, additive cross-check, critical path, milestones, risk-adjusted timeline (Scheme B MU-01..11) |
| Migration Units (Step 2.5.6) | .../modernization/requirements/migration-units.md | AUTHORITATIVE unit decomposition MU-001..011, dependency DAG, shared-file allocation, contract preservation |
| Approach / Rewrite-vs-Refactor (Step 3.2) | .../modernization/approach.md | Per-component Refactor/Replace/Rewrite/Retain dispositions; critical path; conditions for success |
| Strangler Candidates (Step 3.1) | .../modernization/candidates.md | Branch-by-abstraction seam mechanics, suitability scores, do-not-strangle classification |
| Gap Analysis (Step 2.3) | .../modernization/gaps.md | 18 gaps, dependency matrix, WSJF order, phase map, heat map |
| Modernization Assessment (Step 2.1) | .../modernization/assessment.md | Drivers, maturity, R-disposition, Phase 0–3 roadmap, risk/resource/success metrics |
| Target State (Step 2.2) | .../modernization/target-state.md | Ports, ADR-001..008, scheduling contract mapping, migration path, QAS, constraints |

## Limitations / Truth-and-Accuracy Notes

1. **Two upstream MU numbering schemes were reconciled, not merged blindly (§0).** This roadmap anchors on the authoritative `migration-units.md` (MU-001..011) and carries `prioritized-backlog.md` WSJF scores across via the §0.3 crosswalk. The prompt's literal "MU-04" is mapped to the scheduler/worker unit (MU-005 canonical) by referent, not integer. No third numbering scheme is introduced.
2. **Effort and timeline are order-of-magnitude** (S/M/L → 1/2/3 day-class mapping inherited from gaps/prioritized-backlog), assuming one senior Android engineer and the existing 36-test safety net holding. Not velocity-calibrated.
3. **Several load-bearing source facts are inherited** from the verified Step 2.1–3.2 corpus (which read `app/build.gradle`, root `build.gradle`, `AndroidManifest.xml`, and the Otto/jobdispatcher/AsyncTask greps at source). This roadmap is a *synthesis* step; it did not re-grep source. The two line-number corrections from `approach.md` (State.java:32→33; ServiceBase:201→204) are carried forward.
4. **k-9 coordinate availability and People API endpoint liveness are EVALUATE-not-assume** (carried from target-state Risk table). O1/O3 dispositions are firm; their *path* carries Medium confidence pending live checks, with vendoring/escalation fallbacks specified.

---

## Verdict

**PASS** — A phased modernization roadmap (Foundation → Quick Wins → Core Transformation → Optimization) synthesized from the full verified Step 2.1–3.2 corpus, reconciled to the authoritative migration units (MU-001..MU-011) and the WSJF backlog without inventing new numbering, with the prompt's "MU-01 → MU-04" critical path mapped transparently to gate (MU-001) → scheduler/worker (MU-005) and the full spine (gate → test-net → ports → WorkManager → durable checkpoint) made explicit. Every phase has objectives, dependency-aware work items, milestones with go/no-go criteria, success criteria, risk mitigations, and rollback plans; governance defines six decision gates (including the hard coverage gate G2 and the k-9 escalation gate G-k9), DORA-aligned health metrics, and an escalation matrix; the risk register is scored with mitigations traceable to upstream artifacts; resource and skills plans are honest about the single-maintainer reality. Per-component dispositions, branch-by-abstraction mechanics, ADRs, and the preserved-core boundary are all carried forward consistently. Expert-depth self-check satisfied: patterns named from canonical catalogs (Branch-by-Abstraction, Strangler Fig, ACL, Feathers characterization tests, WSJF, Nygard Circuit Breaker), trade-offs explicit, sequencing rigorous with parallelism identified, and the numbering reconciliation demonstrates source-of-truth discipline rather than blind formula-following.

---

*Assessment status: in-progress (Step 4.1 of EXECUTE).*

## Phase Completion Report
---
artifact_id: 20260529-modernization
phase: execute
verdict: PASS
artifact_path: sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/roadmap.md
assessment_status: in-progress
---
