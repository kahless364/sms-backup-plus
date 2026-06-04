---
artifact_type: plan
story_id: "U-033"
verdict: "PASS"
agent: "Developer (orchestrator-applied)"
timestamp: "2026-06-04T00:00:00Z"
---

# Plan: U-033 — Trust source/javadoc jars in Gradle dependency verification (BUG-001 fix)

## Objective
Resolve BUG-001: Gradle dependency verification fails for all `*-sources.jar` artifacts when
an IDE sync / source resolution runs through a detachedConfiguration, because
`gradle/verification-metadata.xml` has no checksums for source/javadoc variants and no trust rule.

## Approach (config-only; BUG-001 Suggested Approach Option A)
Add a narrowly-scoped `<trusted-artifacts>` block inside `<configuration>` trusting ONLY
`.*-sources[.]jar` and `.*-javadoc[.]jar` (regex). Keep `<verify-metadata>true</verify-metadata>`
and all 478 production-artifact checksums untouched. No production source/test/build-logic change.

## Execution note (deviation)
The sprint-execution worktree for this story branched from upstream `master` (pre-modernization),
not from the `sdlc/modernization-plan` HEAD, so its build verification ran against the wrong base
and its branch could not be merged (it would have reverted the entire engagement). The bad branch
was discarded; the orchestrator applied the identical config change directly to the real tree and
re-verified there. See implementation-log.md. (Logged for the retro as a worktree-base defect.)

## Acceptance / Verification
- Additive `<trusted-artifacts>` block; `verify-metadata` stays true; zero checksum changes.
- A previously-failing source jar resolves through verification.
- The three CLI gates stay green with verification enabled.
