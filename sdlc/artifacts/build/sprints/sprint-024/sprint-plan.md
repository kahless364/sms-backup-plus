---
status: active
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-024
sprint: '024'
updated_at: '2026-11-21T10:00:40.510Z'
---

# Sprint Plan: Sprint 024

## Goals
- Raise the app's minimum Android version to 7.0 (API 24) and remove what the floor makes dead: code branches, two manifest permissions, stale lint baseline entries and stale text; retarget the five tests that ran below SDK 24 and add the two `BackupBroadcastReceiverTest` tests as coverage headroom (U-068, story S1 of DES-MODERNIZATION-014).
- Make the content-trigger worker start the incoming backup through the scheduler's `scheduleIncoming()`, so that it obeys the network, encryption-degraded, auto-backup and delay rules, and give the worker its first tests (U-069, the fix for BUG-023).
- Back up an SMS that becomes visible after newer messages were backed up, exactly once, with a 6-hour look-back below the watermark and a local record of what was uploaded (U-070, the fix for BUG-021, REQ-MODERNIZATION-020).
- Make every automatic backup run as ordinary background work, so that it succeeds when the app is off screen on Android 12 and later, keep the foreground notification only for backups and restores started from the screen, and let a refused foreground start continue instead of failing the run (U-075, the fix for BUG-029; added by the revision of 2026-11-21).
- This follows the product owner's goal "get the app completed" and the order ruled in Records 15 and 16 of the decision appendix of `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`: the floor first, then BUG-023, then BUG-021, then (ruling 60, Record 21) BUG-029, all before the toolchain and runtime-library stories.

## Start condition

Sprint 024 does not start until sprint 023 is completed (U-067, the fix for BUG-022, done and closed). U-069 and U-070 depend on U-067 directly; U-068 depends only on U-066 (done), but runs in the same checkout and is held back with the sprint.

## Stories

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-068 | Raise minSdk to 24 and remove what the floor makes dead (code branches, two manifest permissions, retargeted tests, stale lint baseline entries, stale text) | - | developer | sonnet | (unassigned) | U-066 (done) | done | high |
| U-069 | Make the content-trigger worker start the incoming backup through the scheduler's scheduleIncoming(), and give the worker its first tests | - | developer | sonnet | (unassigned) | U-067 (sprint 023), U-068 | done | high |
| U-070 | Back up an SMS that becomes visible after later messages were backed up, exactly once, with a 6-hour look-back and a local upload record | - | developer | sonnet | (unassigned) | U-067 (sprint 023), U-069 | in-progress | high |
| U-075 | Run automatic backups as ordinary background work so that they succeed when the app is off screen, keep the foreground notification only for backups and restores started from the screen, and let a refused foreground start continue instead of failing the run | - | developer | sonnet | (unassigned) | U-070 | in-progress | high |

## Execution Waves

| Wave | Stories | Dev Agent(s) | Can Parallel? | Gate |
|------|---------|--------------|---------------|------|
| 1 | U-068 | developer | No (single story) | wave-gate |
| 2 | U-069 | developer | No (single story) | wave-gate |
| 3 | U-070 | developer | No (single story) | wave-gate |
| 4 | U-075 | developer | No (single story) | wave-gate |

## Dependency Graph & Wave Assignment

- U-068 depends on U-066 (done, sprint 022), which is outside this sprint, so it is wave 1. U-069 depends on U-067 (sprint 023, external, a start condition of the sprint) and on U-068, so it is wave 2. U-070 depends on U-067 and on U-069, so it is wave 3. The chain is strictly serial: U-068 -> U-069 -> U-070. All three stories have `parallel_eligible: false`.
- The serial order is also required by the files: the stories run in one checkout; U-068 and U-069 edit the same scheduler file and test file (`WorkManagerScheduler.kt`, `WorkManagerSchedulerTest.kt`); U-067 and U-070 share the worker files.
- **Revision, 2026-11-21:** U-075 depends on U-070 (in this sprint), so it is wave 4. The chain is now U-068 -> U-069 -> U-070 -> U-075, strictly serial; U-075 has `parallel_eligible: false`. U-070 and U-075 both edit `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt`, so they must be serial in one checkout. U-075 also edits `RestoreWorker.kt` and adds one helper file and tests.
- No circular dependencies.
- Contract gate: U-068 lists CNTR-MODERNIZATION-004, approved (v3, as set by CR-015). U-069, U-070 and U-075 list no contract (`integration_contracts: []`); each changes code inside this one app and does not cross a component boundary in the sense of the gate (CNTR-MODERNIZATION-004 is read by U-069 and U-075, not edited). No CONTRACT GATE BLOCK.
- Design coverage: U-068 carries design DES-MODERNIZATION-014 (approved), slice S1; the other slices (S2 to S5) are later stories and are out of this sprint on purpose. U-069, U-070 and U-075 have empty `design_docs`. U-075 also has empty `requirements` (source `bug:BUG-029`); it needs no change record, because no contract or design text states that automatic backups run in the foreground (U-075, Integration Contract References).
- Sprint completeness: each wave leaves the tree valid (every story is self-contained: change, tests, gates), and the input each later wave needs (U-068's floor and scheduler text for U-069; U-069's worker and U-067's cancellation checks for U-070; U-070's worker code and test fixtures for U-075) is produced by an earlier wave or by sprint 023. The Solution Architect completeness check for the added wave was not run as a separate delegation in this revision; U-075 was written by the Solution Architect's approach in BUG-029 and ruled as ruling 60, and the plan gates of the story cover it.

### Freshness Checks (Step 4)

- REQ-MODERNIZATION-015 (U-068): `approved`. REQ-MODERNIZATION-020 (U-070): `approved`. DES-MODERNIZATION-014: `approved`. CNTR-MODERNIZATION-004: `approved`.
- Change records: CR-015 and CR-016 are `implemented`. No change record lists U-068, U-069 or U-070 as an affected story (searched `sdlc/artifacts/build/change-records/`). No open change record applies.
- BUG-023 and BUG-021 are `ready`. U-068's alignment audit is `passed`.
- Revision, 2026-11-21: BUG-029 is `ready` (severity high, priority high); U-075 was `ready` and is now `planned`. No change record lists U-075 as an affected story (U-075 states none is required before the build).

### File-Overlap Advisories

None. The deterministic check (`detectFileOverlaps`) was run over the three stories with waves 1, 2 and 3 and returned no flags (`metadata.file_overlap_flags` is empty). Same-wave overlap is impossible by construction (one story per wave). Overlaps between waves (the scheduler file and test file between U-068 and U-069; the worker files between U-067 and U-070) are expected and are the reason the waves are serial.

Revision, 2026-11-21: with U-075 in wave 4 the check was not run again; every wave still holds exactly one story, so same-wave overlap is impossible by construction and `metadata.file_overlap_flags` stays `[]`. The expected overlap between waves is `BackupWorker.kt` (U-070 in wave 3, U-075 in wave 4); it is the reason U-075 is serial after U-070.

## Notes

- **Revision, 2026-11-21: U-075 added as wave 4, mid-sprint.** On 2026-11-21 the Android 16 emulator showed that every automatic backup that starts while the app is off screen fails at once (BUG-029, severity high, priority high). By the orchestrator's ruling 60 (decision appendix Record 21) the fix, story U-075, runs directly after U-070 and before U-074, the toolchain stories U-071 to U-073 and the later scheduler fixes, so it joins this sprint. State at the revision: U-068 done; U-069 built and in review and validation; U-070 planned for wave 3, its plan through its gates, not built. U-069 and U-070 are not changed by this revision. U-075 lists `requirements: []`, `design_docs: []`, `integration_contracts: []` (source `bug:BUG-029`); agent developer, model sonnet, as the other stories. Its file was moved from `sdlc/artifacts/stories/` into this sprint's folder and its status set to `planned` through the engine. Sprint 025 now starts after U-075 is done (see its plan).
- **Serial, one checkout.** No parallelism. Each story is committed before the next starts. Sprint 024 starts only after sprint 023 is completed.
- **Orchestrator note, 2026-11-21: planning of U-068 begins while sprint 023 is still building.** The start condition exists because the stories share one checkout and one Gradle. Planning and the plan gates read files and write only documents in the story's workspace, so U-068's plan and its plan gates are prepared while U-067 is built. No code change and no Gradle run for any sprint 024 story happens before sprint 023 is completed, and U-068's implementation re-reads its start state (its baseline step) at the commit that closes U-067. U-067 and U-068 share no file (U-068 AC-9: empty diff for both workers).
- **Device checks are run by the orchestrator only**, never by a subagent. U-068 IC-3, U-069 IC-3 and U-075 AC-12 run on the Android 16 emulator. U-068 IC-4 (an API 24 device) needs the product owner's decision PO-A; without it, it is recorded "not run" (the story states what stands instead).
- **U-068 commits.** U-068's developer makes per-increment local commits (S1-I1 to S1-I7), by ruling 22. No push, no merge to `master`.
- **BUG-021 does not close with U-070** (ruling 27): the Android 17 measurement and the device check of the fixed build belong to the targetSdk 37 story and its QA step (REQ-MODERNIZATION-020 AC-25).
- **Follow-up after this sprint:** the remaining stories of DES-MODERNIZATION-014 (S2 toolchain, S3 compileSdk 37, S4 runtime libraries, S5 targetSdk 37), and a change record for the contract example named by U-069.
- **Assignment:** `sdlc/config.yaml` has no named team members (single owner, private app), so Assigned To is `(unassigned)`, as in sprints 022 and 023. Total points: 0 (the stories carry no point estimate).
- **Risk: long Gradle runs and endpoint security (from the sprint 020 to 022 retros).** Give every Gradle call an explicit timeout above three minutes and do not set `MSYS_NO_PATHCONV=1` for Gradle. If a shell dies without a visible reason, stop and report; do not work around it.
- **Risk: the stories' scope concerns and orchestrator rulings** are recorded in each story's Implementation Notes ("Orchestrator rulings before planning") and are binding on the plans.

## Audit: Agent Resolution Log

| Artifact | Field | Priority | Source | Value |
|----------|-------|----------|--------|-------|
| U-068 | agent | 3 | {plugin_cache}/registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-068 | model | 2 | sdlc/config.yaml | sonnet |
| U-069 | agent | 3 | {plugin_cache}/registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-069 | model | 2 | sdlc/config.yaml | sonnet |
| U-070 | agent | 3 | {plugin_cache}/registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-070 | model | 2 | sdlc/config.yaml | sonnet |
| U-075 | agent | 3 | {plugin_cache}/registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-075 | model | 2 | sdlc/config.yaml | sonnet |
