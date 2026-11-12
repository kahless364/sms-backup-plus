---
status: completed
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-017
sprint: '017'
updated_at: '2026-11-12T16:27:53.974Z'
---

# Sprint Plan: sprint-017

## Goals
- Restore a working build on this machine by making the Gradle build, compilers, and unit-test runtime run on JDK 25 (repairs BUG-017), with all three gates green and bytecode targets unchanged (app 17, vendored module Java 8).

## Stories

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-060 | Run the build on JDK 25 (Gradle 9.x / AGP 9.x toolchain upgrade) while keeping the Java 17 bytecode target |  | developer | sonnet |  |  | done | medium |

## Execution Waves

| Wave | Stories | Dev Agent(s) | Can Parallel? | Gate |
|------|---------|--------------|---------------|------|
| 1 | U-060 | developer | No (single story) | wave-1-gate (deterministic, blocking) |

## Dependency Graph & Wave Assignment

- U-060 has no dependencies (`dependencies: []`); it is the only story, so wave 1 contains it alone.
- No circular dependencies. No file-overlap advisories (single story).
- Contract gate: U-060 is build tooling in a single repo with `integration_contracts: []` and no cross-boundary API/schema/event consumption; no contract gate block.
- Freshness: `requirements: []`, `design_docs: []`; nothing can be stale. Standards reconciliation: zero candidates (domain `build`).
- Sprint completeness validation (plan Step 5a, Solution Architect delegation): not performed by this planning pass because no agent-delegation tool was available to the Sprint Manager here; the orchestrator should run it if required. Story has no design docs to cover.

## Notes

- BUG-017 is the source bug and is resolved by U-060; it is not executed separately and stays in `sdlc/artifacts/stories/`.
- BUG-018 (restore reports failure after success) is deferred to the next sprint: it cannot be built or tested until U-060 lands a working build.
- U-060's on-device acceptance criteria (AC-5) need an API 36 emulator.
- U-060 is high complexity (atomic Gradle 9 + AGP 9 + KGP + KSP + Hilt upgrade); if planning finds it cannot fit one sprint, report back for a split. Parts are not separately buildable on JDK 25.
- `.claude/CLAUDE.md` edits (AC-6) must route through the Engine Librarian.
- Story points are not set on U-060 (planner reports 0).

### Resolution Log (Step 8b planner, story-build pipeline)

| Artifact | Field | Priority | Source | Value |
|----------|-------|----------|--------|-------|
| U-060 | agent | 3 | registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-060 | model | 2 | sdlc/config.yaml | sonnet |
