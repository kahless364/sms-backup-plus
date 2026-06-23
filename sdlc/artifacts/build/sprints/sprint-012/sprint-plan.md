---
status: active
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-012
sprint: '000012'
---

# Sprint Plan: 012 — DI & Legacy Retirement (batch A)

## Goals
- Complete the Hilt DI cutover and retire the legacy Service dual-dispatch layer — the single largest maintainability win from the post-migration assessment (findings AR-001/002/003).

## Story Summary

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-048 | Complete the Hilt DI migration (@Binds + @Inject, remove static alias) | Android | developer | sonnet | lead | — | planned | high |
| U-049 | Retire the legacy backup/restore Service dual-dispatch layer | Android | developer | sonnet | lead | U-048 | planned | high |
| U-050 | Decompose MainActivity, inject worker engine graph, remove stale seams | Android | developer | sonnet | lead | U-048 | planned | medium |

## Execution Waves

### Wave 1
| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-048 | Complete the Hilt DI migration | Android | developer | sonnet | lead | — | planned | high |

### Wave 2
| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-049 | Retire the legacy Service layer | Android | developer | sonnet | lead | U-048 | planned | high |
| U-050 | Decompose MainActivity + inject worker graph | Android | developer | sonnet | lead | U-048 | planned | medium |

## Wave Summary
| Wave | Stories | Can Parallel? | Gate |
|------|---------|---------------|------|
| wave-1 | U-048 | N/A | wave-1-gate |
| wave-2 | U-049, U-050 | Yes (after U-048) | wave-2-gate |

## Capacity and Sequencing Notes
- Team: single maintainer (lead); agent/model `developer`/`sonnet` per config.yaml + sprint-001..011 precedent.
- Stories derived from assessment 20260623-post-migration-assessment. Scheduled, not executed.
- U-049 and U-050 depend on the U-048 DI cutover. Build gate (assembleDebug + testDebugUnitTest + jacoco LINE >=70%) applies to all.

## Resolution Log
### Planner Override
`node lib/index.mjs plan --process story-build` — process YAML not found (consistent with sprint-001..011). Agent/model resolved from config.yaml (`developer`/`sonnet`/`lead`).
### Contract Gate Check
- All stories `integration_contracts: []`; intra-module. PASS. U-049 MUST preserve CNTR-MODERNIZATION-005 (public broadcast API) and the BUG-005 observer-leak fix.
### SA Sprint Completeness Delegation
- `design_docs: []` for all → SKIPPED.
### Freshness Check
- Source assessment completed; stories `status: ready`. PASS.
