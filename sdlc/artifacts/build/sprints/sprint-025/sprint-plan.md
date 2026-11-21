---
status: active
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-025
sprint: '025'
updated_at: '2026-11-21T13:12:34.179Z'
---

# Sprint Plan: Sprint 025

## Goals
- Make a cancelled backup or restore end as cancelled, not as an error line and a failure result, by deciding on the state of the running coroutine's job, and stop the cancel collector warning on every normal finish (U-074, the fix for BUG-024).
- Move the build toolchain, the test tooling and the CI action versions to the latest stable release of each, one component per verified increment, with the compatibility reading recorded and every gate unchanged (U-071, story S2 of DES-MODERNIZATION-014, REQ-MODERNIZATION-015).
- Raise compileSdkVersion from 35 to 37, build only, targetSdk stays 35 (U-072, story S3, REQ-MODERNIZATION-017).
- Raise the runtime libraries to latest stable, one family at a time, and show that backup, restore, scheduling and the default-SMS role behave as before (U-073, story S4, REQ-MODERNIZATION-016).
- This follows the product owner's goal "get the app completed" and the order ruled in Records 15 to 18 of the decision appendix of `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`: BUG-024's fix after U-070 and before the toolchain story, then U-071, U-072, U-073.

## Start condition

Sprint 025 does not make any code change and does not run Gradle until sprint 024 is completed (U-068, U-069 and U-070 built, reviewed and closed). Planning and the plan gates of this sprint's stories (documents only) may be prepared earlier, as was done for sprint 024. U-074 depends on U-067 (done) and U-070 (sprint 024); U-071 on U-067, U-068 and, by ruling, U-069 and U-070 (its start condition SC-9); U-072 on U-071 and U-068; U-073 on U-067 to U-072.

## Stories

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-074 | Make a cancelled backup or restore end as cancelled, not as an error line and a failure result, and stop the cancel collector warning on every normal finish (fix for BUG-024) | - | developer | sonnet | (unassigned) | U-067 (done), U-070 (sprint 024) | planned | medium |
| U-071 | Move the build toolchain, the test tooling and the CI action versions to the latest stable release of each, one component per verified increment | - | developer | sonnet | (unassigned) | U-067 (done), U-068, U-069, U-070 (sprint 024) | in-progress | high |
| U-072 | Raise compileSdkVersion from 35 to 37 (build only, targetSdk stays 35), lint baseline refreshed, Robolectric default kept at 29, CI installs the platform | - | developer | sonnet | (unassigned) | U-071, U-068 | planned | high |
| U-073 | Raise the runtime libraries to latest stable one family at a time; confirm security-crypto, JUnit and jutf7 unchanged | - | developer | sonnet | (unassigned) | U-067 to U-072 | planned | medium |

## Execution Waves

| Wave | Stories | Dev Agent(s) | Can Parallel? | Gate |
|------|---------|--------------|---------------|------|
| 1 | U-074 | developer | No (single story) | wave-gate |
| 2 | U-071 | developer | No (single story) | wave-gate |
| 3 | U-072 | developer | No (single story) | wave-gate |
| 4 | U-073 | developer | No (single story) | wave-gate |

## Dependency Graph & Wave Assignment

- U-074 depends on U-067 (done) and U-070 (sprint 024, external, a start condition of the sprint), so it has no in-sprint dependency and is wave 1. By ruling (Records 15 to 18) it also runs before the toolchain story. U-071 depends on U-067, U-068, U-069 and U-070, all outside this sprint; it is wave 2 by ruling (not by dependency). U-072 depends on U-071 and U-068, so wave 3. U-073 depends on U-067 to U-072, so wave 4. The chain is strictly serial: U-074 -> U-071 -> U-072 -> U-073 (the execution plan encodes each step as depending on the previous). All four stories have `parallel_eligible: false`.
- No circular dependencies.
- Contract gate: all four stories have `integration_contracts: []`; each changes build configuration or code inside this one app and does not cross a component boundary in the sense of the gate. No CONTRACT GATE BLOCK.
- Design coverage: U-071, U-072 and U-073 carry design DES-MODERNIZATION-014 (approved), slices S2, S3 and S4. Slice S1 was sprint 024 (U-068); slice S5 (targetSdk 37) is a later story and is out of this sprint on purpose. U-074 has empty `design_docs` and empty `requirements` (a bug fix).
- Sprint completeness: each wave leaves the tree valid (every story is self-contained: change, tests, gates), and the input each later wave needs (U-071's toolchain for U-072; U-072's compileSdk 37 for U-073) is produced by an earlier wave or by sprint 024. The Solution Architect completeness check is left to the orchestrator's plan gates of each story; it was not run as a separate delegation by this manager.

### Freshness Checks (Step 4)

- REQ-MODERNIZATION-015 (U-071): `approved`. REQ-MODERNIZATION-017 (U-072): `approved`. REQ-MODERNIZATION-016 (U-073): `approved`. DES-MODERNIZATION-014: `approved`. U-074 links no requirement or design.
- Change records: CR-015 and CR-016 are `implemented`. No change record in `sdlc/artifacts/build/change-records/` mentions U-071, U-072, U-073 or U-074 (searched). No open change record applies.
- U-071, U-072 and U-073: alignment audit `passed` (as stated by the orchestrator). All four stories were `ready`.

### File-Overlap Advisories

None. Each wave holds exactly one story, and a file-overlap flag needs two or more stories in the same wave, so the result is empty by construction (`metadata.file_overlap_flags` is `[]`). The `detectFileOverlaps` module was not invoked for this trivially empty case, to keep shell use modest after the endpoint-security event. Overlaps between waves (the build files, version catalog and lint baseline shared by U-071, U-072 and U-073; the backup and restore code shared by U-074 and U-073's runtime-library work) are expected and are the reason the waves are serial.

## Notes

- **Serial, one checkout.** No parallelism: one checkout, one Gradle. Each story is committed before the next starts. Sprint 025 makes no code change before sprint 024 is completed; planning and plan gates (documents only) may be prepared earlier.
- **Device checks are run by the orchestrator only**, never by a subagent. They run on the Android 16 emulator `Pixel_9_Pro`; the emulator `Pixel_10_Pro` is never started or touched.
- **Product owner decisions.** Several device and CI criteria of U-071 and U-073 need the product owner's decisions (items PO-A, PO-B, PO-G, PO-H of Records 15 and 17). Criteria whose decision is missing are recorded "not run", by ruling 31.
- **Commits.** U-071, U-072 and U-073 use per-increment local commits by the developer (ruling 29). U-074 does not: the orchestrator commits after its reviews. No push, no merge to `master`.
- **Comparison-tool rule (Record 18, endpoint-security event of 2026-11-21).** No agent calls the external `diff` or `cmp`; comparisons use `git diff --no-index` or hashes. Shell activity is kept modest. An agent stops and reports if a tool disappears. This applies to every plan and build of this sprint.
- **Version research (ruling 30).** U-071's planning uses the version research of its planning date (`sdlc/artifacts/build/research/`), refreshed by the orchestrator if the date has changed.
- **Follow-up after this sprint:** the targetSdk 37 story (S5), the change records named by U-069 and U-074, and the Thunderbird mail-library migration.
- **Assignment:** `sdlc/config.yaml` has no named team members (single owner, private app), so Assigned To is `(unassigned)`, as in earlier sprints. Total points: 0 (the stories carry no point estimate).
- **Risk: long Gradle runs and endpoint security.** Give every Gradle call an explicit timeout above three minutes and do not set `MSYS_NO_PATHCONV=1` for Gradle. If a shell dies without a visible reason, stop and report; do not work around it.
- **Risk: the stories' scope concerns and orchestrator rulings** are recorded in each story's Implementation Notes ("Orchestrator rulings before planning") and are binding on the plans.

## Audit: Agent Resolution Log

| Artifact | Field | Priority | Source | Value |
|----------|-------|----------|--------|-------|
| U-074 | agent | 3 | {plugin_cache}/registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-074 | model | 2 | sdlc/config.yaml | sonnet |
| U-071 | agent | 3 | {plugin_cache}/registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-071 | model | 2 | sdlc/config.yaml | sonnet |
| U-072 | agent | 3 | {plugin_cache}/registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-072 | model | 2 | sdlc/config.yaml | sonnet |
| U-073 | agent | 3 | {plugin_cache}/registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-073 | model | 2 | sdlc/config.yaml | sonnet |
