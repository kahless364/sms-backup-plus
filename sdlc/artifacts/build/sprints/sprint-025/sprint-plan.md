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
- Keep the regular backup registered across a phone restart, re-register it where an earlier build left a one-time entry in its place, and make the regular schedule "Never" schedule nothing (U-076, the fix for BUG-027 and BUG-026, scheduler contract v4, CR-017).
- Remove the SMS-received receiver that never gets the system broadcast, so that the content trigger is the one incoming path in the code as in the contract (U-077, CR-017).
- Correct the incoming-schedule help text and the README so that they no longer promise a backup after the configured delay (U-078, text only, the fix for BUG-025).
- Raise the runtime libraries to latest stable, one family at a time, and show that backup, restore, scheduling and the default-SMS role behave as before (U-073, story S4, REQ-MODERNIZATION-016).
- This follows the product owner's goal "get the app completed" and the order ruled in Records 15 to 18 of the decision appendix of `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`: BUG-024's fix after U-070 and before the toolchain story, then U-071, U-072, U-073.

## Revision, 2026-11-21

BUG-029 (every automatic backup that starts off screen fails on Android 12 and later) is fixed by story U-075, which the orchestrator's ruling 60 (decision appendix Record 21) places directly after U-070 and before U-074. U-075 is now wave 4 of sprint 024. Effects on this sprint:

- **Sprint 025 now starts after U-075 (sprint 024, wave 4) is done**, not after U-070. The start condition below is read with "U-075" in place of "U-070" as the last story of sprint 024. U-074's and U-071's dependencies on U-070 are met earlier than that; the sprint as a whole waits for U-075.
- **U-074's plan must list the new helper's catch as "left on purpose".** U-075 adds one `catch (e: IllegalStateException)` in `WorkerForeground.kt` (rethrows a `CancellationException` first, continues only on `ForegroundServiceStartNotAllowedException`). U-074's search for catch sites (its IC-1) will find it; it is "left on purpose" (U-075 IC-2, hand-over H-1 to the orchestrator). U-074's story file is not edited by this revision.
- The story files of this sprint were not moved or edited by this revision. (The three follow-on stories expected here were written and are placed by Revision 2 below.)

## Revision 2, 2026-11-21

The three stories expected in the first revision are written and `planned`. By ruling 75 (decision appendix Record 23) they run in this sprint in the order U-076, U-077, U-078, after U-072 and before U-073, each after U-075 (sprint 024, wave 4) and U-074.

- **U-076** fixes BUG-026 and BUG-027 against the scheduler contract at v4 (change record CR-017, `implemented`). Dependencies: U-072, U-075. It edits the scheduler (`WorkManagerScheduler.kt`, the `BackupScheduler.java` javadoc, `BootReceiver`) and adds tests.
- **U-077** removes `SmsBroadcastReceiver` (product owner's decision; CR-017). Dependencies: U-075, U-076. It edits `App.java`, the manifest and the `BackupScheduler.java` javadoc, and deletes one class and its test file (4 tests).
- **U-078** is text only (BUG-025): one string in `values/strings.xml`, 24 translated copies removed, one README paragraph. Dependencies: U-075, U-077 (order).
- **Waves.** The new serial order is U-074, U-071, U-072, U-076, U-077, U-078, U-073, one story per wave. Waves 1 to 3 (U-074, U-071, U-072) keep their numbers; U-076, U-077 and U-078 are waves 4, 5 and 6; U-073 is renumbered from wave 4 to wave 7 and now depends on U-078 in the execution plan.
- **U-074** gets U-075 in its `dependencies` frontmatter: U-075's helper adds a catch that U-074's catch-site search finds, and U-075's plan hands it over as "left on purpose".
- **Start condition** stays "U-075 done" (sprint 024, wave 4).
- **Planning fact: gate run time.** Since story U-070 a full gate run takes about 10 minutes (one slow test class, the 200-backup test, about five and a half minutes by itself). This lengthens every gate run of the seven stories and so the duration of the sprint; give every Gradle call a timeout to match.

## Start condition

Sprint 025 does not make any code change and does not run Gradle until sprint 024 is completed (U-068, U-069 and U-070 built, reviewed and closed). Planning and the plan gates of this sprint's stories (documents only) may be prepared earlier, as was done for sprint 024. U-074 depends on U-067 (done) and U-070 (sprint 024); U-071 on U-067, U-068 and, by ruling, U-069 and U-070 (its start condition SC-9); U-072 on U-071 and U-068; U-073 on U-067 to U-072.

## Stories

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-074 | Make a cancelled backup or restore end as cancelled, not as an error line and a failure result, and stop the cancel collector warning on every normal finish (fix for BUG-024) | - | developer | sonnet | (unassigned) | U-067 (done), U-070 (sprint 024) | in-progress | medium |
| U-071 | Move the build toolchain, the test tooling and the CI action versions to the latest stable release of each, one component per verified increment | - | developer | sonnet | (unassigned) | U-067 (done), U-068, U-069, U-070 (sprint 024) | in-progress | high |
| U-072 | Raise compileSdkVersion from 35 to 37 (build only, targetSdk stays 35), lint baseline refreshed, Robolectric default kept at 29, CI installs the platform | - | developer | sonnet | (unassigned) | U-071, U-068 | planned | high |
| U-076 | Keep the regular backup registered across a phone restart, re-register it where an earlier build left a one-time entry in its place, and make the regular schedule "Never" schedule nothing (fix for BUG-026 and BUG-027) | - | developer | sonnet | (unassigned) | U-072, U-075 (sprint 024) | in-progress | high |
| U-077 | Remove the SMS-received receiver that never gets the system broadcast (the class, its manifest entry, its enable/disable in App, its test and its javadoc mention), keep RECEIVE_SMS and the default-SMS receiver | - | developer | sonnet | (unassigned) | U-075 (sprint 024), U-076 | planned | medium |
| U-078 | Correct the Incoming schedule help text and the README so that they no longer promise a backup after the configured delay, and remove the 24 translations of the changed string (text only, fix for BUG-025) | - | developer | sonnet | (unassigned) | U-075 (sprint 024), U-077 | planned | medium |
| U-073 | Raise the runtime libraries to latest stable one family at a time; confirm security-crypto, JUnit and jutf7 unchanged | - | developer | sonnet | (unassigned) | U-067 to U-072, U-076 to U-078 | planned | medium |

## Execution Waves

| Wave | Stories | Dev Agent(s) | Can Parallel? | Gate |
|------|---------|--------------|---------------|------|
| 1 | U-074 | developer | No (single story) | wave-gate |
| 2 | U-071 | developer | No (single story) | wave-gate |
| 3 | U-072 | developer | No (single story) | wave-gate |
| 4 | U-076 | developer | No (single story) | wave-gate |
| 5 | U-077 | developer | No (single story) | wave-gate |
| 6 | U-078 | developer | No (single story) | wave-gate |
| 7 | U-073 | developer | No (single story) | wave-gate |

## Dependency Graph & Wave Assignment

- **Revision 2, 2026-11-21:** the order is now U-074 (wave 1) -> U-071 (2) -> U-072 (3) -> U-076 (4) -> U-077 (5) -> U-078 (6) -> U-073 (7). U-076 depends on U-072 and U-075 (sprint 024); U-077 on U-075 and U-076; U-078 on U-075 and U-077 (order); U-073 moves from wave 4 to wave 7. All seven stories have `parallel_eligible: false`. The paragraph below describes the original four-story order; read its "wave 4" for U-073 as wave 7.
- U-074 depends on U-067 (done) and U-070 (sprint 024, external, a start condition of the sprint), so it has no in-sprint dependency and is wave 1. By ruling (Records 15 to 18) it also runs before the toolchain story. U-071 depends on U-067, U-068, U-069 and U-070, all outside this sprint; it is wave 2 by ruling (not by dependency). U-072 depends on U-071 and U-068, so wave 3. U-073 depends on U-067 to U-072, so wave 4. The chain is strictly serial: U-074 -> U-071 -> U-072 -> U-073 (the execution plan encodes each step as depending on the previous). All four stories have `parallel_eligible: false`.
- No circular dependencies.
- Contract gate (Revision 2): U-076 and U-077 list CNTR-MODERNIZATION-004, which is `approved` at v4 (CR-017 `implemented`); U-078 lists none. No CONTRACT GATE BLOCK. For the original four stories: all have `integration_contracts: []`; each changes build configuration or code inside this one app and does not cross a component boundary in the sense of the gate. No CONTRACT GATE BLOCK.
- Design coverage: U-071, U-072 and U-073 carry design DES-MODERNIZATION-014 (approved), slices S2, S3 and S4. Slice S1 was sprint 024 (U-068); slice S5 (targetSdk 37) is a later story and is out of this sprint on purpose. U-074 has empty `design_docs` and empty `requirements` (a bug fix). Revision 2: U-076, U-077 and U-078 have no `design_docs`, so no alignment audit applies to them.
- Sprint completeness: each wave leaves the tree valid (every story is self-contained: change, tests, gates), and the input each later wave needs (U-071's toolchain for U-072; U-072's compileSdk 37 for U-073) is produced by an earlier wave or by sprint 024. The Solution Architect completeness check is left to the orchestrator's plan gates of each story; it was not run as a separate delegation by this manager.

### Freshness Checks (Step 4)

- REQ-MODERNIZATION-015 (U-071): `approved`. REQ-MODERNIZATION-017 (U-072): `approved`. REQ-MODERNIZATION-016 (U-073): `approved`. DES-MODERNIZATION-014: `approved`. U-074 links no requirement or design.
- Revision 2: CR-017 is `implemented` (CNTR-MODERNIZATION-004 at v4, `approved`); BUG-025, BUG-026 and BUG-027 are covered by U-078 and U-076. U-076, U-077 and U-078 were `ready` and are now `planned`.
- Change records: CR-015 and CR-016 are `implemented`. No change record in `sdlc/artifacts/build/change-records/` mentions U-071, U-072, U-073 or U-074 (searched). No open change record applies.
- U-071, U-072 and U-073: alignment audit `passed` (as stated by the orchestrator). All four stories were `ready`.

### File-Overlap Advisories

Revision 2, 2026-11-21: the check was not run again; every wave still holds one story, so same-wave overlap is impossible (`metadata.file_overlap_flags` stays `[]`). Expected overlaps between waves, which are the reason for the serial order: U-076 and U-077 both edit the `BackupScheduler.java` javadoc; U-078 and U-070 (sprint 024) both edit `README.md` (different sprints, serial).

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
| U-076 | agent | 3 | {plugin_cache}/registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-076 | model | 2 | sdlc/config.yaml | sonnet |
| U-077 | agent | 3 | {plugin_cache}/registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-077 | model | 2 | sdlc/config.yaml | sonnet |
| U-078 | agent | 3 | {plugin_cache}/registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-078 | model | 2 | sdlc/config.yaml | sonnet |
| U-073 | model | 2 | sdlc/config.yaml | sonnet |
