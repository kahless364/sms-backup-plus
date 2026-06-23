---
status: completed
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-013
sprint: '000013'
end_date: 2026-06-23
velocity_calculated: true
---

# Sprint Plan: 013 — Test Visibility (batch B)

## Goals
- Make the riskiest code measurable: widen the jacoco gate to all packages, add IMAP integration coverage to un-exclude the worker/transport classes, and test the real BackupWorker body (findings TE-001/002/003).

## Story Summary

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-051 | Expand the jacoco coverage gate to all packages | Android | developer | sonnet | lead | — | done | high |
| U-052 | Add IMAP integration coverage + remove worker/transport exclusions | Android | developer | sonnet | lead | U-051 | done | high |
| U-053 | Cover the real BackupWorker body via a testable seam | Android | developer | sonnet | lead | — | done | medium |

## Execution Waves

### Wave 1
| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-051 | Expand the jacoco coverage gate | Android | developer | sonnet | lead | — | done | high |
| U-053 | Cover the real BackupWorker body | Android | developer | sonnet | lead | — | done | medium |

### Wave 2
| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-052 | IMAP integration + remove exclusions | Android | developer | sonnet | lead | U-051 | done | high |

## Wave Summary
| Wave | Stories | Can Parallel? | Gate |
|------|---------|---------------|------|
| wave-1 | U-051, U-053 | Yes | wave-1-gate |
| wave-2 | U-052 | after U-051 | wave-2-gate |

## Capacity and Sequencing Notes
- Team: single maintainer (lead); `developer`/`sonnet`. Stories from assessment 20260623-post-migration-assessment. Scheduled, not executed.
- U-052 removes the jacoco exclusions for BackupImapStoreDelegate*/BackupWorker*/RestoreWorker* only after integration coverage exists; sequence after U-051.

## Resolution Log
### Planner Override
`node lib/index.mjs plan --process story-build` — process YAML not found. Resolved `developer`/`sonnet`/`lead` from config.yaml.
### Contract Gate Check
- All `integration_contracts: []`; intra-module/test. PASS.
### SA Sprint Completeness Delegation
- `design_docs: []` → SKIPPED.
### Freshness Check
- Stories `status: ready`. PASS.
