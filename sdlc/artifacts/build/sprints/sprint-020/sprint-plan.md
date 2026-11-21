---
status: completed
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-020
sprint: '020'
updated_at: '2026-11-21T00:20:36.864Z'
---

# Sprint Plan: Sprint 020

## Goals
- Record, as tests, how the app's two TLS socket factories behave today: `DefaultTrustedSocketFactory` (`SYSTEM_VALIDATED`) and `PinnedCertificateSocketFactory` (`PINNED_CERTIFICATE`), including real loopback handshakes for host-name mismatch, untrusted chain and expired chain (U-063, from CR-009). The result is a recorded baseline against which the later move of the default factory into app-owned code can be checked.
- Tests only. No production code, build file or dependency change (files-touched rule IC-2 of the story).
- The story's first increment (I0) is an environment probe. If the probe shows a loopback handshake cannot work here, the stop-and-split rule (AC-24) applies: the developer stops and reports, and the sprint may end with the story split into two.

## Stories

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-063 | Characterisation tests for the two TLS socket factories (default and pinned) as they behave today | - | developer | sonnet | (unassigned) | - | done | medium |

## Execution Waves

| Wave | Stories | Dev Agent(s) | Can Parallel? | Gate |
|------|---------|--------------|---------------|------|
| 1 | U-063 | developer | No (single story) | wave-gate |

## Dependency Graph & Wave Assignment

- U-063 has no dependencies (`dependencies: []`), so it is the only member of wave 1.
- No circular dependencies. No other stories in the sprint.
- Contract gate (Step 4b): U-063 has `integration_contracts: []`. It adds tests under `app/src/test` only and changes no component. It reads CNTR-MODERNIZATION-001 (TLS socket factory) as context and edits nothing in it, so it does not cross a component boundary and no contract is required. No CONTRACT GATE BLOCK.
- Design coverage (Step 4c): skipped. The story has empty `design_docs`, so there is no design document whose elements need an owning story.
- Standards reconciliation (domain `modernization`): zero-candidate fast path, `{assigned: [], excluded: [], unresolved: [], holds: []}`.

### File-Overlap Advisories

None (single story, so no same-wave file can be shared).

## Notes

- **Tests only:** no file under `app/src/main`, `k9mail-vendored/`, `build.gradle`, `app/build.gradle`, `settings.gradle` or `gradle/` may change. If a defect is found, the story files a bug (AC-23); it does not fix it.
- **First increment is a probe, and the story may split:** I0 records whether a loopback TLS handshake, the host-name verifier and the test certificates work in this environment. If they do not, or if working around it needs a production change or a new dependency, AC-24 applies: the developer stops and reports, Groups A, D and the non-handshake tests of Group E stay deliverable, and Groups B, C and the pinned handshakes (AC-20, AC-21) move to a second story. The sprint may therefore end with U-063 split rather than done.
- **Two decision points belong to the product owner:**
  - **DP-1, a new test dependency.** None is planned and none is allowed. If the JDK-only certificates cannot be loaded or used, the developer stops and asks. A dependency would change `app/build.gradle` and `gradle/verification-metadata.xml`, which is outside the files-touched rule.
  - **DP-2, how the finding "TLS 1.0 and 1.1 remain enabled" is recorded.** Option A (the default until told otherwise): keep the characterisation test passing and file a bug through `/amp:create-bug`. Option B: mark that test `@Ignore` with the bug id and add an also-ignored test for the desired behaviour, which raises the skipped count by 2. The Security Reviewer judges the safety; the product owner decides.
- **Origin:** the story was created from CR-009 on the product owner's instruction of 2026-11-17 ("yes. add tests"). It is a prerequisite for the later story that moves the TLS factory.
- **Test count:** the story expects the `@Test` count to rise from 682 to 708 (26 tests) if no other story changes the count first; it re-baselines at I0 otherwise.
- **BUG-018 not selected** (offered with U-063, the product owner chose U-063 only). It stays in the backlog, unmoved and unedited.
- **Freshness (Step 4):** U-063 has no REQ or DES links. The only change record naming it is CR-009 (status approved), its source, which lists it in `related_stories`; that is expected and not a staleness flag. No other change record references it.
- **Assignment:** the team has one blank member entry, so Assigned To is `(unassigned)`, as in sprint-019.

## Audit: Agent Resolution Log

| Artifact | Field | Priority | Source | Value |
|----------|-------|----------|--------|-------|
| U-063 | agent | 3 | <plugin_cache>/registries/pipelines/story-build/process.yaml | amp/agents/developer |
| U-063 | model | 2 | sdlc/config.yaml | sonnet |
