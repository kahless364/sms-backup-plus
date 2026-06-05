---
status: completed
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-009
sprint: '000009'
end_date: 2026-06-05
velocity_calculated: true
---

# Sprint Plan: 009 — Fresh-label retry-window fix (BUG-013)

## Goals

- Fix BUG-013 (medium): widen the create→select retry window in `K9MailTransport.openWithRetryAfterCreate` so the **first** backup to a brand-new Gmail label succeeds in a single run, instead of failing the first attempt and only succeeding on the WorkManager retry (~38 s later). Follow-on to U-043/BUG-011; surfaced in sprint-008 live testing.

## Story Summary

| ID    | Title | Type | Points | Complexity |
|-------|-------|------|--------|------------|
| U-044 | Widen create-then-select retry window for fresh IMAP label propagation (fix BUG-013) | bug | 2 | medium |

## Execution Waves

### Wave 1

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-044 | Widen create-then-select retry window for fresh IMAP label propagation (fix BUG-013) | Android | developer | sonnet | lead | — | done | high |

## Wave Summary

| Wave | Stories | Can Parallel? | Gate |
|------|---------|---------------|------|
| wave-1 | U-044 | N/A (single story) | wave-1-gate |

## Capacity and Sequencing Notes

### Team
- **Team size:** Single maintainer (lead).
- **Agent/model:** `developer` / `sonnet` — per `config.yaml` `defaults.model_routing.developer: sonnet` and sprint-001 through sprint-008 precedent.

### Sprint Framing
Single-story maintenance sprint (2 points). U-044 touches only `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java` (the `openWithRetryAfterCreate` / `MAX_CREATE_OPEN_RETRIES` / `getRetryDelayMs` retry tuning) and its unit test `K9MailTransportCreateOpenTest.java`. No structural change; the fix direction from U-043 is correct and only the retry budget needs widening (more attempts + exponential backoff to a bounded, cancellable ~15–30 s cap).

### On-Device Tests Are Post-Merge
Per U-044 AC-4, the on-device confirmation (first backup to a brand-new label succeeds in one run) is run post-merge against the live Gmail account on emulator-5554. Worktree execution runs in-worktree unit tests only.

### Dependencies
U-044 has `dependencies: []`. Single node, single wave.

### Risk Notes
- Must preserve the U-042 watermark invariant and the U-043 NONEXISTENT-only guard; the longer wait must be bounded and cancellable (respect coroutine cancellation) and must NOT add latency to the already-exists fast path.
- Build gate (assembleDebug + testDebugUnitTest + jacocoTestCoverageVerification LINE ≥70%) applies.

## Resolution Log

### Planner Override
**Trigger:** `node lib/index.mjs plan --process story-build --artifacts U-044 --config sdlc/config.yaml` — process YAML not found for pipeline `story-build` (consistent with sprint-001 through sprint-008).
**Resolution:** Agent/model resolved from `config.yaml` (`developer`/`sonnet`/`lead`) and prior-sprint precedent. Single wave; execution plan produced per CNTR-PIPELINE-007.

### Contract Gate Check
- `integration_contracts: []`. Fully internal to the Android app's mail.transport module (retry tuning within `K9MailTransport`). No cross-domain boundary. CONTRACT GATE: PASS.

### SA Sprint Completeness Delegation
- `design_docs: []`. Per the skip rule, SA delegation SKIPPED.

### Dependency Analysis and Wave Assignment
- U-044: `dependencies: []` → wave-1. Single node, zero edges.

### Freshness Check
- BUG-013: `status: ready`. U-044: `status: ready`. PASS.
