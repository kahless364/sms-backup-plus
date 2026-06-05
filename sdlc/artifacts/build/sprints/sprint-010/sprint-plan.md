---
status: active
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-010
sprint: '000010'
---

# Sprint Plan: 010 — SNI log noise + edge-to-edge insets (BUG-012, BUG-014)

## Goals

- Fix BUG-012 (low): stop the vendored k-9 `DefaultTrustedSocketFactory` from logging a `NoSuchMethodException` ERROR on every IMAP connect on API 35+ — set SNI via the supported public API and degrade quietly otherwise.
- Fix BUG-014 (medium): handle window insets so the app respects Android 15 forced edge-to-edge — the toolbar sits below the status bar (title no longer overlaps the clock) and content clears the navigation bar.

## Story Summary

| ID    | Title | Type | Points | Complexity |
|-------|-------|------|--------|------------|
| U-045 | Quiet conscrypt SNI fallback in vendored DefaultTrustedSocketFactory (fix BUG-012) | bug | 2 | low |
| U-046 | Handle window insets for edge-to-edge so content clears the system bars (fix BUG-014) | bug | 3 | medium |

## Execution Waves

### Wave 1

| Story ID | Title | Platforms | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|----------|-------|-----------|-----------|-------|-------------|------------|--------|----------|
| U-045 | Quiet conscrypt SNI fallback in vendored DefaultTrustedSocketFactory (fix BUG-012) | Android | developer | sonnet | lead | — | planned | high |
| U-046 | Handle window insets for edge-to-edge so content clears the system bars (fix BUG-014) | Android | developer | sonnet | lead | — | planned | high |

## Wave Summary

| Wave | Stories | Dev Agent(s) | Can Parallel? | Gate |
|------|---------|--------------|---------------|------|
| wave-1 | U-045, U-046 | developer | Yes (disjoint files) | wave-1-gate |

## Capacity and Sequencing Notes

### Team
- **Team size:** Single maintainer (lead).
- **Agent/model:** `developer` / `sonnet` — per `config.yaml` `defaults.model_routing.developer: sonnet` and sprint-001..009 precedent.

### Sprint Framing
Two-story maintenance sprint (5 points) targeting two API-35-era issues found in exploratory testing. Disjoint file sets:
- **U-045**: vendored `k9mail-vendored/.../mail/ssl/DefaultTrustedSocketFactory.java` (SNI shim).
- **U-046**: `app/.../activity/MainActivity.java`, `app/src/main/res/layout/main.xml`, a new insets helper under `app/.../utils/`, and other top-level activities (Advanced settings / auth) for consistent insets.
No file overlap → both `parallel_eligible`, single wave.

### On-Device Validation Is Post-Merge
- U-045: confirm no `NoSuchMethodException` in logcat on a real backup (API 37).
- U-046: before/after screenshots (main screen + one other activity) — the primary acceptance gate, since insets are hard to unit-test. Captured post-merge on emulator-5554.

### Dependencies
Both `dependencies: []`. Two isolated nodes, single wave.

### Risk Notes
- **U-045**: must not drop SNI where it currently works or weaken cert validation / pinned-cert path (REQ-MODERNIZATION-002). Prefer the public `SSLParameters.setServerNames` API (guard API 24+). Confined to the vendored module.
- **U-046**: must preserve the edge-to-edge toolbar look (background to top edge) while insetting content; handle landscape/cutout left-right insets; no regression on API 21–34; keep the BUG-002/U-034 themed-dialog behavior. Prefer a shared helper applied by all top-level activities.
- Build gate (assembleDebug + testDebugUnitTest + jacoco LINE ≥70%) applies to both.

## Resolution Log

### Planner Override
**Trigger:** `node lib/index.mjs plan --process story-build --artifacts U-045,U-046 --config sdlc/config.yaml` — process YAML not found for `story-build` (consistent with sprint-001..009).
**Resolution:** Agent/model from `config.yaml` (`developer`/`sonnet`/`lead`) and prior-sprint precedent. Single wave; execution plan per CNTR-PIPELINE-007.

### Contract Gate Check
- `integration_contracts: []` for both. U-045 is internal to the vendored module; U-046 is internal Android UI. No cross-domain boundary. CONTRACT GATE: PASS.

### SA Sprint Completeness Delegation
- `design_docs: []` for both → SA delegation SKIPPED.

### Dependency Analysis and Wave Assignment
- U-045, U-046: `dependencies: []` → wave-1. Two nodes, zero edges.

### Freshness Check
- BUG-012, BUG-014: `status: ready`. U-045, U-046: `status: ready`. PASS.
