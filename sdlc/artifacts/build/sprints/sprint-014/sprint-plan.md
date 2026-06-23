---
status: active
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-014
sprint: '000014'
---

# Sprint Plan: 014 — Security Hardening (batch C)

## Goals
- Close the cheap, high-trust security gaps: enforce the ENCRYPTION_DEGRADED flag (completes BUG-008), move security-crypto off alpha, and add a no-cleartext network config + opt-in copy (findings SE-002/003/004).

## Story Summary

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-054 | Enforce the ENCRYPTION_DEGRADED flag (completes BUG-008) | Android | developer | sonnet | lead | — | planned | medium |
| U-055 | Move security-crypto off alpha + scheme regression test | Android | developer | sonnet | lead | — | planned | low |
| U-056 | Network security config (no cleartext) + opt-in copy | Android | developer | sonnet | lead | — | planned | low |

## Execution Waves

### Wave 1
| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-054 | Enforce ENCRYPTION_DEGRADED | Android | developer | sonnet | lead | — | planned | medium |
| U-055 | security-crypto off alpha | Android | developer | sonnet | lead | — | planned | low |
| U-056 | networkSecurityConfig + opt-in copy | Android | developer | sonnet | lead | — | planned | low |

## Wave Summary
| Wave | Stories | Can Parallel? | Gate |
|------|---------|---------------|------|
| wave-1 | U-054, U-055, U-056 | Yes (disjoint) | wave-1-gate |

## Capacity and Sequencing Notes
- Team: single maintainer (lead); `developer`/`sonnet`. Stories from assessment 20260623-post-migration-assessment. Scheduled, not executed.
- U-056 is config + UI-copy only; it MUST NOT add a permission to BackupBroadcastReceiver (CNTR-MODERNIZATION-005 prohibits it).

## Resolution Log
### Planner Override
`node lib/index.mjs plan --process story-build` — process YAML not found. Resolved `developer`/`sonnet`/`lead`.
### Contract Gate Check
- All `integration_contracts: []`. PASS. U-056 preserves CNTR-MODERNIZATION-005.
### SA Sprint Completeness Delegation
- `design_docs: []` → SKIPPED.
### Freshness Check
- Stories `status: ready`. PASS.
