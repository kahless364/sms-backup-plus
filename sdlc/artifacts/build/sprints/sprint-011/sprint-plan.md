---
status: active
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-011
sprint: '000011'
---

# Sprint Plan: 011 — SNIHostName IP-literal guard (BUG-015)

## Goals

- Fix BUG-015 (low): guard `new SNIHostName(host)` in the vendored `DefaultTrustedSocketFactory` so an IP-literal or empty IMAP host skips SNI (RFC 6066) instead of throwing `IllegalArgumentException`. Follow-on hardening to U-045/BUG-012; the default Gmail (DNS) path is unaffected.

## Story Summary

| ID    | Title | Type | Points | Complexity |
|-------|-------|------|--------|------------|
| U-047 | Guard SNIHostName against IP-literal/empty IMAP host (fix BUG-015) | bug | 1 | low |

## Execution Waves

### Wave 1

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-047 | Guard SNIHostName against IP-literal/empty IMAP host (fix BUG-015) | Android | developer | sonnet | lead | — | planned | low |

## Wave Summary

| Wave | Stories | Dev Agent(s) | Can Parallel? | Gate |
|------|---------|--------------|---------------|------|
| wave-1 | U-047 | developer | N/A (single story) | wave-1-gate |

## Capacity and Sequencing Notes

### Team
- **Team size:** Single maintainer (lead). Agent/model `developer`/`sonnet` per `config.yaml` and sprint-001..010 precedent.

### Sprint Framing
Single-story (1 pt) hardening sprint. Touches only `k9mail-vendored/.../mail/ssl/DefaultTrustedSocketFactory.java` (`setSniViaSSLParameters` guard) and its SNI unit test. Unit-test gated (no IP IMAP server available for a device test); the Gmail DNS path is already on-device-validated from U-045.

### Dependencies
`dependencies: []`. Single node, single wave.

### Risk Notes
- Must not change DNS-host SNI behavior (Gmail) or weaken cert validation / pinned-cert (REQ-MODERNIZATION-002); must preserve the BUG-012 no-ERROR-noise behavior. Build gate (assembleDebug + testDebugUnitTest + jacoco LINE ≥70%).

## Resolution Log

### Planner Override
**Trigger:** `node lib/index.mjs plan --process story-build --artifacts U-047 --config sdlc/config.yaml` — process YAML not found for `story-build` (consistent with sprint-001..010).
**Resolution:** Agent/model from `config.yaml` (`developer`/`sonnet`/`lead`). Single wave; execution plan per CNTR-PIPELINE-007.

### Contract Gate Check
- `integration_contracts: []`. Internal to the vendored module. CONTRACT GATE: PASS.

### SA Sprint Completeness Delegation
- `design_docs: []` → SA delegation SKIPPED.

### Dependency Analysis and Wave Assignment
- U-047: `dependencies: []` → wave-1.

### Freshness Check
- BUG-015: `status: ready`. U-047: `status: ready`. PASS.
