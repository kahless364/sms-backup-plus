---
status: active
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-018
sprint: '018'
updated_at: '2026-11-12T18:22:54.585Z'
---

# Sprint Plan: Sprint 018

## Goals
- Remove the donation flow and the Play Billing dependency from the app (REQ-MODERNIZATION-018, as amended by CR-003, under EPIC-MODERNIZATION-006). This is the first build story of the dependency-currency epic.

## Stories

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-061 | Remove the donation flow and the Play Billing dependency | - | developer | sonnet | (unassigned) | - | planned | medium |

## Execution Waves

| Wave | Stories | Dev Agent(s) | Can Parallel? | Gate |
|------|---------|--------------|---------------|------|
| 1 | U-061 | developer | No (single story) | wave-gate |

## Dependency Graph & Wave Assignment

- U-061 has no dependencies (`dependencies: []`), so it is the only member of wave 1.
- No circular dependencies. No other stories in the sprint.
- Contract gate: U-061 is a single-repo removal with `integration_contracts: []`. No approved contract (CNTR-MODERNIZATION-001..008) mentions billing or donations. No CONTRACT GATE BLOCK.
- Standards reconciliation (domain `modernization`): zero-candidate fast path, `{assigned: [], excluded: [], unresolved: [], holds: []}`.
- File-overlap advisories: none (single story).

## Notes

- **Artifact work the Developer pipeline will not do (IC-14 / AC-10):** U-061 requires a supersession change record for REQ-MODERNIZATION-010, a reciprocal entry on REQ-MODERNIZATION-018, and a superseded annotation on DES-MODERNIZATION-010 naming DES-MODERNIZATION-013. The orchestrator must dispatch this to the Artifact Librarian / Requirements Analyst during this sprint.
- **Step 4c audit history:** the first Solution Architect coverage run returned FAIL on one finding (design D8 supersession records). The product owner chose "Auto-revise"; the Requirements Analyst added IC-14 and four wording fixes. The second run returned PASS.
- **On-device check (IC-11):** requires the API 36 emulator AVD `Pixel_9_Pro`.
- **BUG-018** (restore reports failure after success) remains unscheduled; it has no repair story yet.
- **Scope:** the app is private and unpublished, so `metadata/play/**` is out of scope.
- **Freshness (Step 4):** REQ-MODERNIZATION-018 and DES-MODERNIZATION-013 are `approved`; CR-003 is `implemented` and already applied.

## Audit: Agent Resolution Log

| Artifact | Field | Priority | Source | Value |
|----------|-------|----------|--------|-------|
| U-061 | agent | 3 | registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-061 | model | 2 | sdlc/config.yaml | sonnet |
