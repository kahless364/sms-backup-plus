---
status: active
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-006
sprint: '000006'
---

# Sprint Plan: 006 — Consolidated Code-Review Bug Fixes (BUG-004, BUG-005, BUG-006)

## Goals

- Fix BUG-004 (critical): unify `SyncStateRepository` to a single shared instance so engine-emitted state actually reaches the UI — dual-repository wiring causes post-restore default-SMS restoration, progress/terminal updates, and permission prompts to silently fail
- Fix BUG-005 (high): remove the `WorkInfo` observer on service `onDestroy` so observers don't leak across service restarts and `*StateChanged()` is not invoked in duplicate
- Fix BUG-006 (medium): remove five dead mail-transport imports from `RestoreWorker.kt` to enforce `service.*` ACL purity (CNTR-MODERNIZATION-007)

## Story Summary

| ID    | Title | Type | Points | Complexity |
|-------|-------|------|--------|------------|
| U-036 | Unify SyncStateRepository to a single shared instance (fix BUG-004) | bug | 3 | medium |
| U-037 | Remove WorkInfo observer on service onDestroy (fix BUG-005 leak) | bug | 1 | low |
| U-038 | Remove dead mail-transport imports from RestoreWorker.kt (fix BUG-006) | bug | 1 | low |

## Execution Waves

### Wave 1

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-036 | Unify SyncStateRepository to a single shared instance (fix BUG-004) | Android | developer | sonnet | lead | — | planned | high |
| U-037 | Remove WorkInfo observer on service onDestroy (fix BUG-005 leak) | Android | developer | sonnet | lead | — | planned | high |
| U-038 | Remove dead mail-transport imports from RestoreWorker.kt (fix BUG-006) | Android | developer | sonnet | lead | — | planned | medium |

## Wave Summary

| Wave | Stories | Can Parallel? | Gate |
|------|---------|---------------|------|
| wave-1 | U-036, U-037, U-038 | Yes (disjoint files) | wave-1-gate |

## Capacity and Sequencing Notes

### Team

- **Team size:** Single maintainer (lead). All stories assigned to lead.
- **Agent/model:** `developer` / `sonnet` — consistent with sprint-001 through sprint-005 precedent and `config.yaml` `defaults.model_routing.developer: sonnet`.

### Sprint Framing

This is a three-story maintenance sprint targeting the three code-review findings (BUG-004, BUG-005, BUG-006) logged during the sprint-002 retrospective. Total estimated effort: 5 story points (3 + 1 + 1). The three stories touch fully disjoint file sets:

- **U-036**: `App.java`, `di/EventModule.kt`, `activity/MainViewModel.kt` / `activity/MainActivity.java`
- **U-037**: `service/SmsBackupService.java`, `service/SmsRestoreService.java`
- **U-038**: `service/RestoreWorker.kt`

No file appears in more than one story. There are no intra-sprint `depends_on` edges. All three stories are `parallel_eligible: true`. The dependency graph has three nodes and zero edges — a single wave of three parallel-eligible stories.

U-036 is the highest-risk story (critical bug, DI wiring change requiring ordering care and an identity test). U-037 is a targeted teardown addition in two services. U-038 is trivially scoped (import removal only).

### Dependencies

All three stories have `dependencies: []` in frontmatter. No intra-sprint edges. Dependency graph: three isolated nodes. Wave assignment: all three in wave-1.

### Risk Notes

- **U-036**: Hilt inject-into-Application ordering must be verified — Hilt injects `App` fields after `super.onCreate()`, so the static accessor assignment must occur before any consumer call sites. If ordering is ambiguous the alternative (`EventModule.provideSyncStateRepository()` returns the App static) is acceptable. The AC-1 identity test is the primary correctness gate.
- **U-037**: All `observeForever` registration sites must be matched with `removeObserver` calls. Both service classes must be covered; missing either would fail AC-1/AC-2.
- **U-038**: Low risk. Each of the five import symbols must be confirmed unused (not just in code, but also in KDoc/comments that don't constitute genuine usage) before removal.
- Build gate (assembleDebug + testDebugUnitTest + jacocoTestCoverageVerification LINE ≥70%) applies to all three.

## Resolution Log

### Planner Override

**Trigger:** `node C:/Users/Michael.Horsley/.claude/plugins/cache/ai-value-lab-dev/amp/1.5.18/lib/index.mjs plan --process story-build --artifacts U-036,U-037,U-038 --config sdlc/config.yaml` — process YAML not found for pipeline `story-build` at project or core paths (consistent with sprint-001 through sprint-005 behavior).

**Resolution:** Agent/model resolved from `config.yaml` `defaults.model_routing.developer: sonnet` and sprint-001 through sprint-005 precedent (all prior stories used `developer` / `sonnet` / assigned to `lead`). No override of wave structure, gate type, or expected artifacts. Execution plan produced directly per CNTR-PIPELINE-007.

### Contract Gate Check

- `integration_contracts: []` for all three stories (U-036, U-037, U-038).
- All three fixes are fully internal to the Android application module. No cross-domain service boundary, no external API, schema, or protocol reference.
  - U-036: DI wiring internal to `App` and `di/` — CNTR-MODERNIZATION-003 is a referenced constraint, not a new boundary-crossing contract.
  - U-037: Service lifecycle teardown within `service/` — no external boundary.
  - U-038: Import removal within `service/RestoreWorker.kt` — CNTR-MODERNIZATION-007 is a referenced constraint to reinforce, not a new contract.
- CONTRACT GATE: PASS for all three stories. No block.

### SA Sprint Completeness Delegation

- `design_docs: []` for all three stories. Per Step 4c instruction (`design_docs=[] for all candidates -> SKIP`), SA delegation is skipped.
- All three stories are scoped exclusively from BUG-* artifacts. Each BUG artifact documents root cause, evidence, affected code locations, and fix approach in full. No separate DES-* design document is required for bounded bug-fix repair stories of this scope.
- The three stories touch disjoint files and have no wave-boundary artifact dependencies — no story in wave-1 depends on an artifact created by another wave-1 story. No partial-state risk at wave boundaries.
- SA delegation: SKIPPED.

### Dependency Analysis and Wave Assignment

- U-036: `dependencies: []` — no intra-sprint edges. Wave 1.
- U-037: `dependencies: []` — no intra-sprint edges. Wave 1.
- U-038: `dependencies: []` — no intra-sprint edges. Wave 1.
- Intra-sprint dependency graph: three isolated nodes, zero edges.
- Circular dependency check: N/A (no edges, no cycles possible).
- Wave assignment: wave-1 = {U-036, U-037, U-038} (all three in parallel).

### Freshness Check

- BUG-004: `status: ready` (non-draft). PASS.
- BUG-005: `status: ready` (non-draft). PASS.
- BUG-006: `status: ready` (non-draft). PASS.
- U-036: `status: ready` at time of scheduling. PASS.
- U-037: `status: ready` at time of scheduling. PASS.
- U-038: `status: ready` at time of scheduling. PASS.
