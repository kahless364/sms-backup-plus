---
status: active
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-021
sprint: '021'
updated_at: '2026-11-21T02:29:43.597Z'
---

# Sprint Plan: Sprint 021

## Goals
- Close the two gaps that sprint 020 left in the TLS baseline tests: the pinned factory must be shown to reject an expired enrolled certificate, and the safety net for a host name that the SNI guard lets through but the JDK rejects must be shown to stop `createSocket` from throwing (U-064, from CR-009). Tests only.
- Then fix BUG-019 and BUG-020 together (U-065): neither TLS socket factory enables any protocol older than TLS 1.2, and the pinned-certificate factory treats cipher suites and the server name the way the default factory does. This follows the product owner's decisions of 2026-11-20 (Records 8 and 9 in the appendix "Product owner decision" of the U-062 feasibility analysis).
- Order is fixed by the product owner: U-064 first, the TLS fix second, both before the migration's code move (CR-009).

## Stories

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-064 | Close two gaps in the TLS baseline tests - pinned expiry check and the SNI safety net on its own | - | developer | sonnet | (unassigned) | - | review | medium |
| U-065 | Drop TLS versions older than 1.2 from both TLS socket factories, and give the pinned-certificate factory the default factory's cipher-suite and server-name handling | - | developer | sonnet | (unassigned) | U-064 | planned | medium |

## Execution Waves

| Wave | Stories | Dev Agent(s) | Can Parallel? | Gate |
|------|---------|--------------|---------------|------|
| 1 | U-064 | developer | No (single story) | wave-gate |
| 2 | U-065 | developer | No (single story, depends on U-064) | wave-gate |

## Dependency Graph & Wave Assignment

- U-064 has no dependencies (`dependencies: []`), so it is the only member of wave 1.
- U-065 lists `dependencies: [U-064]`, so it is the only member of wave 2. It starts from the tree U-064 leaves.
- No circular dependencies.
- Contract gate (Step 4b): both stories have `integration_contracts: []`. Both work on the two TLS socket factories and their tests inside this one app. The contract CNTR-MODERNIZATION-001 describes those factories, and the product owner has decided its changes are held for one combined revision at the migration, so neither story edits it. Neither crosses a component boundary in the sense of the gate (no second service, API or schema), and the stories do not contradict this. No CONTRACT GATE BLOCK. U-064 reads the contract as context only. U-065 changes code the contract describes but leaves the contract text alone, as the story states.
- Design coverage (Step 4c): skipped. Both stories have empty `design_docs`, so there is no design document whose elements need an owning story.
- Standards reconciliation (domain `modernization`): zero candidates for both stories, nothing attached, no holds.

### File-Overlap Advisories

No same-wave overlap, so no flag is raised (`metadata.file_overlap_flags` is empty). For information, the two stories do share files, in different waves:

- `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java`: U-064 (wave 1) only changes it temporarily, to prove its tests fail when the expiry check is removed, and restores it, leaving it unchanged. U-065 (wave 2) edits it.
- The test classes under `app/src/test/java/com/zegoggles/smssync/mail/` (the SNI test class and the pinned-factory test classes): U-064 adds tests; U-065 changes and adds tests in the same area.

This is ordinary sequencing: the later wave builds on the finished earlier wave. It is also why the stories are in separate waves.

## Notes

- **Origin:** U-064 comes from CR-009 (the two gaps recorded in the sprint 020 reviews). U-065 comes from BUG-019 and also covers BUG-020. The product owner chose both and their order (Record 9 of the feasibility analysis appendix).
- **Bugs closed by U-065:** BUG-019 and BUG-020 are ready bugs that U-065 fixes. They are not sprint items and are not moved out of `sdlc/artifacts/stories/`. They are closed when U-065 is done.
- **BUG-018 not selected:** it is in the backlog and was passed over again. It stays where it is, unmoved and unedited.
- **Freshness (Step 4):** U-064 and U-065 have no REQ or DES links, so the requirement and design checks have nothing to compare. The open change records that mention them are CR-009 (lists U-064), CR-012 and CR-013 (CR-013 lists U-064 and discusses U-065's area). All three are approved, none is stale, and none blocks the sprint: they are applied at the later migration, not now. CR-013's pinned-factory alternative (b) may be applied only once U-065's code and tests exist, which is the combined contract revision after this sprint.
- **Assignment:** `sdlc/config.yaml` has no named team members (single owner, private app), so Assigned To is `(unassigned)`, as in sprint 020. Total points: 0 (the stories carry no point estimates).
- **Risk: U-065 starts from the tree U-064 leaves.** The test count is expected to be 713 after U-064 (710 at the last baseline). U-064 takes blob hashes of two production files for its temporary edits; once U-065 edits a production file, those hashes are out of date. U-065 must record a new start commit, a new test count and new file hashes before its first edit, and must not reuse U-064's baseline.
- **Risk: conditional criteria in U-065.** AC-8 (b) (an old-protocol server is refused) may turn out not demonstrable under Conscrypt; AC-4 (b) and (c) (layered shape and null host) depend on a probe run first; AC-1 (d) (feeding a supported list to the protocol step) depends on how the plan structures the code. Each must be recorded in the story's Implementation Notes, not left silent.
- **Risk: device check IC-1 in U-065.** It installs the debug build over the existing app on the `Pixel_9_Pro` emulator and runs a backup. Only the orchestrator or QA may run it, never a subagent with a background shell. It never touches `Pixel_10_Pro`, never handles the account password, and is reported as not run or not conclusive when it cannot be done properly.
- **Risk: long Gradle runs and endpoint security (from the sprint 020 retro).** Give every Gradle call an explicit timeout above three minutes and do not set `MSYS_NO_PATHCONV=1` for Gradle. Take log timestamps from the clock. Check mutations compile when they are written. Commit only the story's files before QA, with the product owner's permission. If a shell dies without a visible reason (the endpoint security product has flagged the Git shell before), stop and report; do not work around it.

## Audit: Agent Resolution Log

| Artifact | Field | Priority | Source | Value |
|----------|-------|----------|--------|-------|
| U-064 | agent | 3 | {plugin_cache}/registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-064 | model | 2 | sdlc/config.yaml | sonnet |
| U-065 | agent | 3 | {plugin_cache}/registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-065 | model | 2 | sdlc/config.yaml | sonnet |
