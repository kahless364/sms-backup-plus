---
status: completed
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-007
sprint: '000007'
end_date: 2026-06-05
velocity_calculated: true
---

# Sprint Plan: 007 — Consolidated Security-Review Fixes (BUG-007, BUG-008)

## Goals

- Fix BUG-007 (security medium): remove the OAuth2 token from the AccountManager result Intent extra — persist it directly via `AuthPreferences` inside `useToken()` before `setResult` so no bearer token travels in an Activity Intent
- Fix BUG-008 (security medium): surface Keystore failure during the plaintext-to-encrypted credential migration — emit a user-visible degraded-security signal and/or gate backup operations rather than silently retaining plaintext credentials

## Story Summary

| ID    | Title | Type | Points | Complexity |
|-------|-------|------|--------|------------|
| U-039 | Persist OAuth2 token directly instead of via Intent extra (fix BUG-007) | bug | 1 | low |
| U-040 | Surface Keystore failure during credential migration (fix BUG-008) | bug | 3 | medium |

## Execution Waves

### Wave 1

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-039 | Persist OAuth2 token directly instead of via Intent extra (fix BUG-007) | Android | developer | sonnet | lead | — | done | medium |
| U-040 | Surface Keystore failure during credential migration (fix BUG-008) | Android | developer | sonnet | lead | — | done | medium |

## Wave Summary

| Wave | Stories | Can Parallel? | Gate |
|------|---------|---------------|------|
| wave-1 | U-039, U-040 | Yes (disjoint files) | wave-1-gate |

## Capacity and Sequencing Notes

### Team

- **Team size:** Single maintainer (lead). Both stories assigned to lead.
- **Agent/model:** `developer` / `sonnet` — consistent with sprint-001 through sprint-006 precedent and `config.yaml` `defaults.model_routing.developer: sonnet`.

### Sprint Framing

This is a two-story maintenance sprint targeting the two security findings (BUG-007, BUG-008) logged during the consolidated security review (2026-06). Total estimated effort: 4 story points (1 + 3). The two stories touch fully disjoint file sets:

- **U-039**: `AccountManagerAuthActivity.java` (lines 148–153), and potentially `MainActivity.java` if it consumes the removed `EXTRA_TOKEN` extra
- **U-040**: `EncryptedPrefsSecretStore.java` (lines 229–235), plus a degraded-security flag/notification mechanism and coordination with `CredentialMigrationGate` (U-035)

No file appears in more than one story. There are no intra-sprint `depends_on` edges. Both stories are `parallel_eligible: true`. The dependency graph has two nodes and zero edges — a single wave of two parallel-eligible stories.

U-040 is the higher-complexity story: failure-path handling must preserve U-012/U-035 invariants (rollback-safe, exactly-once, off-main-thread) while adding a testable user-visible signal. U-039 is low-complexity: a targeted refactor to move token persistence ahead of `setResult` and drop the `EXTRA_TOKEN` extra.

### Dependencies

Both stories have `dependencies: []` in frontmatter. No intra-sprint edges. Dependency graph: two isolated nodes. Wave assignment: both in wave-1.

### Risk Notes

- **U-039**: Confirm no other code path passes the token in an Intent extra; verify `MainActivity` (or any other consumer) no longer reads from `EXTRA_TOKEN` and instead reads from `AuthPreferences`. AC-3 grep check is the primary gate.
- **U-040**: The surfacing mechanism (notification vs. UI badge vs. backup gate) is decided during planning; must not introduce main-thread blocking (U-035 preservation). Retry/idempotency interplay with U-012 invariants is the primary correctness risk. The failure-path test (injecting a throwing `SecretStore`) is mandatory per AC-4.
- Build gate (assembleDebug + testDebugUnitTest + jacocoTestCoverageVerification LINE ≥70%) applies to both stories.

## Resolution Log

### Planner Override

**Trigger:** `node C:/Users/Michael.Horsley/.claude/plugins/cache/ai-value-lab-dev/amp/1.5.18/lib/index.mjs plan --process story-build --artifacts U-039,U-040 --config sdlc/config.yaml` — process YAML not found for pipeline `story-build` at project or core paths (consistent with sprint-001 through sprint-006 behavior).

**Resolution:** Agent/model resolved from `config.yaml` `defaults.model_routing.developer: sonnet` and sprint-001 through sprint-006 precedent (all prior stories used `developer` / `sonnet` / assigned to `lead`). No override of wave structure, gate type, or expected artifacts. Execution plan produced directly per CNTR-PIPELINE-007.

### Contract Gate Check

- `integration_contracts: []` for both stories (U-039, U-040).
- Both fixes are fully internal to the Android application module. No cross-domain service boundary, no external API, schema, or protocol reference.
  - U-039: Legacy AccountManager OAuth path within `AccountManagerAuthActivity.java` and `MainActivity.java` — no boundary crossing.
  - U-040: Credential migration path within `EncryptedPrefsSecretStore.java` — coordination with U-035's `CredentialMigrationGate` is intra-module; no external contract boundary.
- CONTRACT GATE: PASS for both stories. No block.

### SA Sprint Completeness Delegation

- `design_docs: []` for both stories. Per Step 4c instruction (`design_docs=[] for all candidates -> SKIP`), SA delegation is skipped.
- Both stories are scoped exclusively from BUG-* artifacts. Each BUG artifact documents root cause, evidence, affected code locations, and fix approach in full. No separate DES-* design document is required for bounded bug-fix repair stories of this scope.
- The two stories touch disjoint files and have no wave-boundary artifact dependencies — no story in wave-1 depends on an artifact created by the other wave-1 story. No partial-state risk at wave boundaries.
- SA delegation: SKIPPED.

### Dependency Analysis and Wave Assignment

- U-039: `dependencies: []` — no intra-sprint edges. Wave 1.
- U-040: `dependencies: []` — no intra-sprint edges. Wave 1.
- Intra-sprint dependency graph: two isolated nodes, zero edges.
- Circular dependency check: N/A (no edges, no cycles possible).
- Wave assignment: wave-1 = {U-039, U-040} (both in parallel).

### Freshness Check

- BUG-007: `status: ready` (non-draft). PASS.
- BUG-008: `status: ready` (non-draft). PASS.
- U-039: `status: ready` at time of scheduling. PASS.
- U-040: `status: ready` at time of scheduling. PASS.
