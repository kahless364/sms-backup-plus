---
status: completed
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-015
sprint: '000015'
end_date: 2026-06-24
velocity_calculated: true
---

# Sprint Plan: 015 — Toolchain Currency (batch D)

## Goals
- Pay down forward-compat debt: Kotlin 2.x + KSP + JVM 17, refresh the vendored k-9 deps, and flip the AGP-8 deferrals + repo hygiene + CI coverage verification (findings BT-001/003/002+005+006).

## Story Summary

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-057 | Migrate to Kotlin 2.x + KSP, raise JVM target to 17 | Android | developer | sonnet | lead | — | done | medium |
| U-058 | Refresh vendored k-9 deps, drop org.apache.http.legacy | Android | developer | sonnet | lead | — | done | medium |
| U-059 | Flip AGP-8 R-class defaults, repo hygiene, CI coverage verify | Android | developer | sonnet | lead | — | done | medium |

## Execution Waves

### Wave 1
| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-057 | Kotlin 2.x + KSP + JVM 17 | Android | developer | sonnet | lead | — | done | medium |
| U-058 | Refresh vendored k-9 deps | Android | developer | sonnet | lead | — | done | medium |
| U-059 | AGP-8 defaults + repo hygiene + CI | Android | developer | sonnet | lead | — | done | medium |

## Wave Summary
| Wave | Stories | Can Parallel? | Gate |
|------|---------|---------------|------|
| wave-1 | U-057, U-058, U-059 | Sequence (shared build files) | wave-1-gate |

## Capacity and Sequencing Notes
- Team: single maintainer (lead); `developer`/`sonnet`. Stories from assessment 20260623-post-migration-assessment. Scheduled, not executed.
- Note: U-057 (kapt→KSP), U-059 (gradle.properties / AGP flags) and U-058 (vendored build.gradle) touch overlapping build config — execute sequentially despite the single wave. U-057 is the largest item.

## Resolution Log
### Planner Override
`node lib/index.mjs plan --process story-build` — process YAML not found. Resolved `developer`/`sonnet`/`lead`.
### Contract Gate Check
- All `integration_contracts: []`. PASS. U-058 preserves the MailTransport ACL.
### SA Sprint Completeness Delegation
- `design_docs: []` → SKIPPED.
### Freshness Check
- Stories `status: ready`. PASS.
