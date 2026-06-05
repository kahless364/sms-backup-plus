---
status: active
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-005
sprint: '000005'
---

# Sprint Plan: 005 — Move Credential Migration Off Main Thread (BUG-003)

## Goals

- Fix BUG-003: move the one-time plaintext-to-encrypted credential migration out of `App.onCreate()` onto a background thread so startup is no longer at risk of an ANR from blocking Keystore IPC and disk I/O on the main thread
- Eliminate the StrictMode disk-write violation (and associated `penaltyFlashScreen()` screen flash) attributable to the migration path on first-run-after-upgrade
- Preserve all U-012 correctness invariants: idempotency, rollback safety, Option-A ordering, OAuth2 not-logged-out, and CNTR-MODERNIZATION-003 durability semantics (`commit()` not `apply()`)

## Story Summary

| ID    | Title | Type | Points | Complexity |
|-------|-------|------|--------|------------|
| U-035 | Move plaintext-to-encrypted credential migration off the main thread | bug | 3 | medium |

## Execution Waves

### Wave 1

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-035 | Move plaintext-to-encrypted credential migration off the main thread | Android | developer | sonnet | lead | — | planned | low |

## Wave Summary

| Wave | Stories | Can Parallel? | Gate |
|------|---------|---------------|------|
| wave-1 | U-035 | No (single story) | wave-1-gate |

## Capacity and Sequencing Notes

### Team

- **Team size:** Single maintainer (lead). Story assigned to lead.
- **Agent/model:** `developer` / `sonnet` — consistent with sprint-001 through sprint-004 precedent and `config.yaml` `defaults.model_routing.developer: sonnet`.

### Sprint Framing

This is a single-story maintenance sprint targeting BUG-003, a low-priority StrictMode violation where the one-time credential migration runs blocking Keystore IPC and synchronous `commit()` disk writes on the main thread inside `App.onCreate()`. The penalty is `penaltyFlashScreen()` only (no `penaltyDeath()`), so there is no crash today, but the latent ANR risk is real on slow-storage devices at first-run-after-upgrade. The implementation work is concentrated in the dispatch change at `App.java:163` and the completion gate wiring across the three credential-read methods in `AuthPreferences`; the internal `migrateFromPlaintext()` logic in `EncryptedPrefsSecretStore` is unchanged.

### Dependencies

U-035 lists `dependencies: [U-012, U-015]` in story frontmatter. These are informational back-references only — both U-012 and U-015 completed in sprint-001 and their artifacts exist on disk. There are no intra-sprint dependencies; wave-1 has a single story and no dependency edges to resolve.

### Risk Notes

- The completion gate (AC-4) is the primary correctness risk: the gate must cover all credential-read entry points and must signal immediately (no suspend) on second launch when migration is already complete. Race-safety reasoning must be documented in code comments co-located with the gate declaration.
- `EncryptedPrefsSecretStore` requires a real Android Keystore provider; test seaming via `InMemorySecretStore` or an equivalent test double is required for unit coverage. Robolectric or on-device confirmation is recommended for AC-2 StrictMode-absence but is not a mandatory CI gate given the low priority.
- Both `commit()` calls in `migrateFromPlaintext()` must remain `commit()` and must not be replaced with `apply()` (CNTR-MODERNIZATION-003 durability barrier). Moving only `getEncrypted()` off-thread is a known incomplete fix (AC-3).

## Resolution Log

### Planner Override

**Trigger:** `node C:/Users/Michael.Horsley/.claude/plugins/cache/ai-value-lab-dev/amp/1.5.18/lib/index.mjs plan --process story-build --artifacts U-035 --config sdlc/config.yaml` returned `Error: Process YAML not found for pipeline 'story-build' at project or core paths` (exit code 1).

**Resolution:** Agent/model resolved from `config.yaml` defaults (`defaults.model_routing.developer: sonnet`) and sprint-001 through sprint-004 precedent (all prior stories: `developer` / `sonnet` / assigned to `lead`). No override of wave structure, gate type, or expected artifacts. Execution plan produced directly per CNTR-PIPELINE-007.

### Contract Gate Check

- `integration_contracts: []` in U-035 frontmatter.
- Scope is a threading/startup fix entirely within the Android application module (`App.java`, `Preferences.java`, `AuthPreferences.java`, `EncryptedPrefsSecretStore.java`). No cross-domain boundary, no external service, API, or schema references.
- CNTR-MODERNIZATION-003 is referenced in the story body as a constraint to preserve (durability semantics), not as a boundary-crossing integration contract that needs a new CNTR-* artifact.
- No integration contract required. CONTRACT GATE: PASS (no block).

### SA Sprint Completeness Delegation

- Single-story sprint with `design_docs: []` and `requirements: []`. No multi-story coverage gaps or wave-boundary correctness issues to evaluate.
- Design is fully documented in BUG-003 and the U-035 story body (SA option A rationale, affected code table, eight acceptance criteria, three integration criteria, verification steps, and technical context). No separate DES-* document is required for a bounded bug-fix repair story of this scope.
- SA delegation: SKIPPED per create-sprint Step 4c instruction (`design_docs=[] for the only candidate -> SKIP`).

### Dependency Analysis

- `dependencies: [U-012, U-015]` in U-035 frontmatter — both are informational back-references to already-completed stories (sprint-001). Neither is in this sprint's candidate set.
- Intra-sprint dependency graph: single node (U-035), zero edges. Topological sort is trivial: wave-1 = {U-035}.
- Circular dependency check: N/A (single node, no edges).

### Freshness Check

- BUG-003: `status: ready` (non-draft). PASS.
- U-035: `status: ready` (non-draft) at time of scheduling. PASS.
