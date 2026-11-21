---
status: active
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-023
sprint: '023'
updated_at: '2026-11-21T09:45:46.037Z'
---

# Sprint Plan: Sprint 023

## Goals
- Make the three cancellation checks in the backup and restore workers (`BackupWorker.kt` one site, `RestoreWorker.kt` two sites) observe the running coroutine's job, by reading `currentCoroutineContext().ensureActive()` instead of the worker's inherited deprecated `coroutineContext` member (a dispatcher that holds no job, so the check never throws), and prove each of the three sites with a test that cancels the work mid-loop (U-067, the fix for BUG-022). Production changes are three call lines, two import lines and, under Route S (ruled), the widening of `backupCursors` to `internal` with `@VisibleForTesting`; plus tests and one Javadoc correction.
- This follows the product owner's goal "get the app completed" and the order ruled in Record 15 of the decision appendix of `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` (BUG-022, then BUG-023, then BUG-021, before the runtime-library story; S2 after BUG-022). Its cancellation tests are the stop rule of the later Kotlin, coroutines and WorkManager upgrades of DES-MODERNIZATION-014.
- The story's two decision points (DP-1 the Javadoc, DP-2 the backup-loop seam) were ruled by the orchestrator on 2026-11-21 and are recorded in the story's Implementation Notes (DP-1 in, Route S).

## Stories

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-067 | Make the backup and restore workers' three cancellation checks observe the running job, and prove each one with a test that cancels the work mid-loop | - | developer | sonnet | (unassigned) | U-066 (done) | in-progress | medium |

## Execution Waves

| Wave | Stories | Dev Agent(s) | Can Parallel? | Gate |
|------|---------|--------------|---------------|------|
| 1 | U-067 | developer | No (single story) | wave-gate |

## Dependency Graph & Wave Assignment

- U-067 depends on U-066, which is done (commit `1f8ebe3b`, sprint 022) and is outside this sprint, so U-067 is the only member of wave 1. It is the only story in the sprint, so there is one wave and nothing runs in parallel (`parallel_eligible: false`).
- No circular dependencies.
- Contract gate: `integration_contracts: []`. The story changes two production files inside this one app (plus tests). It does not reach a second service, API or schema, so it does not cross a component boundary in the sense of the gate. The scheduler contract CNTR-MODERNIZATION-004 (rule 7) is read, not edited. No CONTRACT GATE BLOCK.
- Design coverage: skipped. The story has empty `design_docs`, so there is no design document whose elements need an owning story.
- Sprint completeness: one story, one wave; the story is self-contained (fix, tests, mutation evidence, gates), and its start condition (U-066 done) is met. No later wave exists to leave in an invalid state.

### File-Overlap Advisories

None. There is one story and one wave, so no same-wave overlap is possible and no flag is raised (`metadata.file_overlap_flags` is empty).

## Notes

- **Deliberately one small story.** This sprint is one small story so that the cancellation fix starts at once. The next sprint (024) takes U-068 (minSdk 24, story S1 of DES-MODERNIZATION-014), U-069 (the BUG-023 fix) and U-070 (the BUG-021 fix), which are being written now. The order is ruled in the decision appendix Record 15: BUG-022, then BUG-023, then BUG-021, before the runtime-library story; S2 after BUG-022.
- **Origin:** U-067 is the fix for BUG-022 (`sdlc/artifacts/stories/BUG-022-worker-cancellation-checks-do-nothing.md`), raised by DES-MODERNIZATION-014 (FD-VAL-006). Source `bug:BUG-022`; severity low, priority medium.
- **Freshness:** U-067 has empty `requirements` and `design_docs`, so the requirement and design checks have nothing to compare. BUG-022 is ready. No open change record lists U-067 as affected.
- **Assignment:** `sdlc/config.yaml` has no named team members (single owner, private app), so Assigned To is `(unassigned)`, as in sprint 022. Total points: 0 (the story carries no point estimate).
- **Risk: Route S is binding.** If the plan finds the backup-loop seam disproportionate it stops and reports; it does not drop site 1 (Implementation Notes).
- **Risk: tests rely on a harness property.** Under work-testing, `setProgress` is never a cancellation point, so the three checks are the only stop points; the mutation table (AC-6, six mutations) proves each test bites its own site. Cancelled runs still return `Result.failure` (out of scope, bug SC-1); the tests do not assert the `Result` type.
- **Risk: long Gradle runs and endpoint security (from the sprint 020 to 022 retros).** Give every Gradle call an explicit timeout above three minutes and do not set `MSYS_NO_PATHCONV=1` for Gradle. If a shell dies without a visible reason, stop and report; do not work around it.
- **No device step.** IC-4: every criterion is shown at unit level; any emulator run is the orchestrator's and optional.

## Audit: Agent Resolution Log

| Artifact | Field | Priority | Source | Value |
|----------|-------|----------|--------|-------|
| U-067 | agent | 3 | {plugin_cache}/registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-067 | model | 2 | sdlc/config.yaml | sonnet |
