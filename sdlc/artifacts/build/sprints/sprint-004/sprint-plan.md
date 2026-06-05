---
status: active
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-004
sprint: '000004'
---

# Sprint Plan: 004 — TLS Pin-Certificate Enrollment Dialog Crash Fix

## Goals

- Fix BUG-002: restore the TLS pin-certificate enrollment dialog so it displays without crashing when a user enrolls a server certificate from Advanced Settings
- Ensure `AlertDialog` is built with an AppCompat-themed Activity context, eliminating the `IllegalStateException` thrown by `AppCompatDelegateImpl.createSubDecor`
- Preserve U-009 acceptance criteria (Trust stores cert, Cancel is safe default, no Activity leak across async fetch)

## Story Summary

| ID    | Title | Type | Points | Complexity |
|-------|-------|------|--------|------------|
| U-034 | Fix TLS pin-certificate enrollment dialog crash: build AlertDialog with a themed Activity context | bug-fix | 3 | medium |

## Execution Waves

### Wave 1

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-034 | Fix TLS pin-certificate enrollment dialog crash: build AlertDialog with a themed Activity context | Android | developer | sonnet | lead | — | planned | high |

## Wave Summary

| Wave | Stories | Can Parallel? | Gate |
|------|---------|---------------|------|
| wave-1 | U-034 | No (single story) | wave-1-gate |

## Capacity and Sequencing Notes

### Team

- **Team size:** Single maintainer (lead). Story assigned to lead.
- **Agent/model:** `developer` / `sonnet` — consistent with sprint-001, sprint-002, sprint-003 precedent and config.yaml `defaults.model_routing.developer: sonnet`.

### Sprint Framing

This is a single-story maintenance sprint targeting BUG-002, a high-priority crash that renders the U-009 TLS pin-certificate enrollment feature completely unusable. The change is structurally focused: dialog construction refactor in `PinCertificateEnrollmentFlow.java`, one interface contract update on `DialogShower`, one call-site update in `AdvancedSettings.java`, and test-double alignment. On-device verification (emulator-5554 API 37 plus physical device if available) is the primary time cost because the defect class (`AppCompatDelegateImpl.createSubDecor` theme check) is not exercised by JVM unit tests.

### Dependencies

U-034 has no intra-sprint dependencies. `depends_on: []` in story frontmatter. All prerequisite artifacts (U-009 implementation, `PinCertificateEnrollmentFlow.java`, `AdvancedSettings.java`) exist on disk from sprint-001.

### Risk Notes

- On-device verification is mandatory for AC-1 (dialog displays without crash on API 37 emulator). The build gate alone is insufficient — the `AppCompatDelegateImpl` crash fires at real dialog inflation, not in unit tests.
- The `AsyncTask` deprecation (`FetchCertTask extends AsyncTask`) is a pre-existing condition from U-009; it is out of scope for this story and must not be changed here.

## Resolution Log

### Planner Override

**Trigger:** `node .../amp/1.5.18/lib/index.mjs plan --process story-build --artifacts U-034 --config sdlc/config.yaml` returned `Error: Process YAML not found for pipeline 'story-build' at project or core paths` (exit code 1).

**Resolution:** Agent/model resolved from config defaults (`defaults.model_routing.developer: sonnet`) and sprint-001/002/003 precedent (all stories: `developer` / `sonnet` / assigned to `lead`). No override of wave structure, gate type, or expected artifacts. Execution plan produced directly per CNTR-PIPELINE-007.

### Contract Gate Check

- `integration_contracts: []` in U-034 frontmatter.
- Scope is a UI bug fix entirely within `app/src/main/java/.../activity/fragments/` — single Android module, no cross-domain boundary, no external service/API/schema references.
- No integration contract required. CONTRACT GATE: PASS (no block).

### SA Sprint Completeness Delegation

- Single-story sprint with `design_docs: []` and `requirements: []`. No multi-story coverage gaps or wave-boundary correctness issues to evaluate.
- Design is fully documented in BUG-002 and the U-034 story body (SA option A rationale, affected code table, anti-leak boundary constraints, and acceptance criteria). No separate DES-* document is required for a bounded bug-fix repair story of this scope.
- SA delegation: SKIPPED per create-sprint Step 4c instruction (`design_docs=[] for the only candidate -> SKIP`).

### Dependency Analysis

- `dependencies: []` in U-034 frontmatter. No intra-sprint dependency graph to sort. Single wave (wave-1) with one story.
- Circular dependency check: N/A (single node, no edges).

### Freshness Check

- BUG-002: `status: ready` (non-draft). PASS.
- U-034: `status: ready` (non-draft) at time of scheduling. PASS.
