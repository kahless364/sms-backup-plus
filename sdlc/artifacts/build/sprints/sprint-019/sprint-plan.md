---
status: active
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-019
sprint: '019'
updated_at: '2026-11-12T22:13:54.860Z'
---

# Sprint Plan: Sprint 019

## Goals
- Produce the cited feasibility analysis of replacing the vendored K-9 mail library with Thunderbird for Android mail libraries (REQ-MODERNIZATION-019 Part A, questions 1-12) at `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`, ending in exactly one recommendation (go / go-with-conditions / no-go) for the product owner's written decision. No product code, build file or dependency changes.

## Stories

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-062 | Feasibility analysis for replacing the vendored K-9 mail library with Thunderbird for Android mail libraries (REQ-MODERNIZATION-019 Part A) | - | developer | sonnet | (unassigned) | - | in-progress | high |

## Execution Waves

| Wave | Stories | Dev Agent(s) | Can Parallel? | Gate |
|------|---------|--------------|---------------|------|
| 1 | U-062 | developer | No (single story) | wave-gate |

## Dependency Graph & Wave Assignment

- U-062 has no dependencies (`dependencies: []`), so it is the only member of wave 1.
- No circular dependencies. No other stories in the sprint.
- Contract gate: U-062 has `integration_contracts: []` and changes no product code. It reads CNTR-MODERNIZATION-007 (approved) as context only, so no contract is required. Confirmed by reading the story. No CONTRACT GATE BLOCK.
- Standards reconciliation (domain `modernization`): zero-candidate fast path, `{assigned: [], excluded: [], unresolved: [], holds: []}`.

### File-Overlap Advisories

None (single story, so no same-wave file can be shared).

## Notes

- **Cannot reach `done` by agents alone:** AC-15 (REQ-MODERNIZATION-019 AC-A8) needs the product owner's written decision on the analysis. After implementation and reviews, the orchestrator presents the analysis to the product owner and records the decision before QA validates AC-15. If the sprint would otherwise end first, the story carries over; it is not closed without the decision.
- **No Part B before acceptance:** no Part B (migration) design or story may be created in this sprint or before the product owner's acceptance date.
- **Nature of the work:** "implementation" is research and writing and needs web access. Experiments (cloning or building thunderbird-android) run outside the repository working tree per the story's experiment rules, and nothing from them is committed.
- **Agent and risk:** the implementing agent resolves to `developer` on model `sonnet` by the planner's defaults (unchanged). Risk: the work is research-heavy and every claim needs primary-source verification, so review and QA must open the citations rather than trust them.
- **Gate set:** the end-of-story gate set includes `:app:assembleRelease` (sprint-018 retro recommendation 1), alongside `:app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`. The test-count baseline is fixed at 682.
- **BUG-018 not selected** (restore reports failure after success): left unscheduled until it has a repair story, the same handling as BUG-017 -> U-060 in sprint-017. The cross-story audit found no shared file, dependency or conflict with U-062, and scheduling U-062 alone keeps its test-count baseline stable. Audit observations for its eventual repair story: (a) its "blocked by BUG-017" constraint is obsolete, as BUG-017 is done; (b) its AC-4 (restored/duplicate counts) is a second defect across the worker-to-ViewModel boundary that the repair story should split out or scope explicitly.
- **DES-MODERNIZATION-009 is partly stale:** it names CNTR-MODERNIZATION-009-mailtransport, whereas the approved contract is CNTR-MODERNIZATION-007. It needs revising only if Part B goes ahead; this story does not touch it.
- **Step 4c audit:** Solution Architect verdict PASS (coherence PASS, coverage PASS). U-062 is analysis-only and implements none of DES-MODERNIZATION-009, whose components are owned by done stories U-010, U-025, U-026 and U-027.
- **Freshness (Step 4):** REQ-MODERNIZATION-019 and DES-MODERNIZATION-009 are `approved`; no change record references U-062.
- **Selection basis:** the product owner approved starting the Thunderbird feasibility work (2026-11-12). Single eligible story; no selection prompt was issued.
- **Assignment:** the team has one blank member entry, so Assigned To is `(unassigned)`, as in sprint-018.

## Audit: Agent Resolution Log

| Artifact | Field | Priority | Source | Value |
|----------|-------|----------|--------|-------|
| U-062 | agent | 3 | registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-062 | model | 2 | sdlc/config.yaml | sonnet |
