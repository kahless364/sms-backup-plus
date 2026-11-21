---
status: completed
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-022
sprint: '022'
updated_at: '2026-11-21T09:40:46.525Z'
---

# Sprint Plan: Sprint 022

## Goals
- Prune the weak cipher suites from the lists both TLS socket factories share, so that only forward-secret suites with authenticated encryption stay enabled (the TLS 1.3 suites, six ECDHE suites for TLS 1.2, and the renegotiation signalling value), and close the branch where the cipher list could not be worked out in advance (U-066). One production file changes (the vendored default TLS socket factory), plus tests.
- This follows the product owner's decision of 2026-11-21 (Record 11, answer 2, "2 yes") and the goal "get the app completed" (Record 12), in the appendix "Product owner decision" of `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`. It runs before the migration's code move (CR-009), so the move carries lists that are already pruned and tested.
- The story's four decision points (target policy, allow-list, failed-initialisation branch, order) were ruled by the orchestrator on 2026-11-21 and are recorded in the story's Implementation Notes.

## Stories

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-066 | Prune the weak cipher suites from the lists both TLS socket factories share, and close the failed-initialisation branch for cipher suites | - | developer | sonnet | (unassigned) | - | done | medium |

## Execution Waves

| Wave | Stories | Dev Agent(s) | Can Parallel? | Gate |
|------|---------|--------------|---------------|------|
| 1 | U-066 | developer | No (single story) | wave-gate |

## Dependency Graph & Wave Assignment

- U-066 has no dependencies (`dependencies: []`), so it is the only member of wave 1. It is the only story in the sprint, so there is one wave and nothing runs in parallel (`parallel_eligible: false`).
- No circular dependencies.
- Contract gate (Step 4b): `integration_contracts: []`. The story changes one production file (the vendored default TLS socket factory) and tests inside this one app. It does not reach a second service, API or schema, so it does not cross a component boundary in the sense of the gate, and the story text does not contradict that. The contract CNTR-MODERNIZATION-001 describes that factory; the product owner holds all its changes for one combined revision at the migration, and CR-014 (with its dated Amendment 1 for this story) carries the statements. The story edits no contract and no change record. No CONTRACT GATE BLOCK.
- Design coverage (Step 4c): skipped. The story has empty `design_docs`, so there is no design document whose elements need an owning story.
- Standards reconciliation (domain `modernization`): zero candidates, nothing attached, no holds.

### File-Overlap Advisories

None. There is one story and one wave, so no same-wave overlap is possible and no flag is raised (`metadata.file_overlap_flags` is empty).

For information, other work runs in the same checkout at the same time: a design document for requirements REQ-MODERNIZATION-015 to -017 is being written under `sdlc/artifacts/design/` and `sdlc/artifacts/build/research/`. It shares no file with this story, which touches only the vendored socket factory, its tests under `app/src/test/java/com/zegoggles/smssync/mail/`, and its own folder under this sprint.

## Notes

- **Origin:** U-066 comes from finding F-1 (Low) of the U-065 security review. The product owner chose it on 2026-11-21 (Record 11 row 2) and set the goal "get the app completed" (Record 12), under which the orchestrator schedules it.
- **Not selected:** BUG-018 (ready, a small tidy-up) was passed over again. It stays in `sdlc/artifacts/stories/`, unmoved and unedited. BUG-019 and BUG-020 are done and stay where they are.
- **Freshness (Step 4):** U-066 has empty `requirements` and `design_docs`, so the requirement and design checks have nothing to compare. No open change record lists U-066 as affected in its frontmatter. Three approved change records mention the code the story changes: CR-014 (its dated Amendment 1 of 2026-11-21 sets the allow-list for this story; the amendment applies only once U-066 is done and its tests exist), CR-009 (the move of the default factory) and CR-013 (server-name statements). None is stale and none blocks the sprint; all are applied at the later migration. The story's step S0-2 (CR-014 reworded before any production edit) is completed by the orchestrator, not by this story.
- **Assignment:** `sdlc/config.yaml` has no named team members (single owner, private app), so Assigned To is `(unassigned)`, as in sprint 021. Total points: 0 (the story carries no point estimate).
- **Risk: the allow-list must keep the TLS 1.3 suite names.** If `TLS_AES_128_GCM_SHA256`, `TLS_AES_256_GCM_SHA384` and `TLS_CHACHA20_POLY1305_SHA256` are left out, TLS 1.3 breaks (at least under the JDK provider). The renegotiation signalling value stays too. Mutation M-X4 and probe S0-4 (b) cover this.
- **Risk: the isolation guards.** Three test classes (Hardening, JDK provider, Handshake) use a DHE suite in the factory's cipher list as the sign of which provider the lists were computed under. Once DHE suites are gone the JDK class's guard fails on every run and the other two can never fire. They must be reworked or removed with the reason recorded (AC-8).
- **Risk: conditional criterion AC-6.** Refusing a server that offers only pruned suites depends on a probe (S0-4 (d)): the desktop Conscrypt test server must be able to serve a pruned suite, and the control (handshake completes with the cipher handling removed) must hold. If not, AC-6 is reported not demonstrable with the evidence; it is not left silent.
- **Risk: emulator check IC-1.** Only the orchestrator or QA runs it, never a subagent with a background shell. It installs the debug build over the existing app on the `Pixel_9_Pro` emulator and runs a backup; it never touches `Pixel_10_Pro`, never handles the account password, and is reported not run or not conclusive when it cannot be done properly. The story says who provides something to back up: if the run reports 0 messages, the orchestrator may send one test SMS through the emulator console, under the product owner's standing statement that adding SMS on the emulator is safe.
- **Risk: shared checkout.** Other work runs here at the same time (see the advisory above): no file overlap, and only this story's agents may run Gradle.
- **Risk: long Gradle runs and endpoint security (from the sprint 020 and 021 retros).** Give every Gradle call an explicit timeout above three minutes and do not set `MSYS_NO_PATHCONV=1` for Gradle. If a shell dies without a visible reason (the endpoint security product has flagged the Git shell before), stop and report; do not work around it.
- **Lessons from the sprint 021 retro, applied here:**
  - Test totals come only from a run made with the read-only init script, with the results folder deleted first (JUnit XML is switched off in the build, so a plain run leaves another run's XML on disk).
  - Each row of the mutation table is derived against the planned test code, stating the first failing assertion and the failure type.
  - The evidence-hygiene scan is the last step and writes outside the folders it scans.
  - The emulator check says who provides something to back up (see the IC-1 risk above).

## Audit: Agent Resolution Log

| Artifact | Field | Priority | Source | Value |
|----------|-------|----------|--------|-------|
| U-066 | agent | 3 | {plugin_cache}/registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-066 | model | 2 | sdlc/config.yaml | sonnet |
