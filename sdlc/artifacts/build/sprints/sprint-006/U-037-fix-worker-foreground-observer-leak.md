---
type: bug
status: done
sprint: '000006'
artifact_type: user-story
priority: high
complexity: low
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-037
title: Remove WorkInfo observer on service onDestroy (fix BUG-005 leak)
pipeline: ''
domain: modernization
requirement_source: bug:BUG-005
---

# U-037: Remove WorkInfo observer on service onDestroy

## Story
As a maintainer, I want the worker→foreground `WorkInfo` observer removed when the service is destroyed, so that observers don't leak and `*StateChanged()` callbacks aren't duplicated across service restarts.

## Source
Fixes **BUG-005** (high) — see `sdlc/artifacts/stories/BUG-005-worker-foreground-observer-leak-on-destroy.md`.

## Acceptance Criteria
1. AC-1: `onDestroy` (and terminal-state teardown) unconditionally calls `removeObserver()` on the WorkInfo LiveData; no `observeForever` registration survives service destruction.
2. AC-2: After a destroy+restart cycle (same unique-work name), exactly one observer is active; `backupStateChanged()`/`restoreStateChanged()` are not invoked in duplicate — covered by a test.
3. AC-3: The U-031 foreground promotion / stop signalling for active runs is preserved.
4. AC-4: Build green (assembleDebug, testDebugUnitTest, jacoco ≥70%).
5. IC-1: The `LiveData` handle is cached alongside the observer so teardown can call `removeObserver(observer)` on every path.

## Technical Notes
- Files: `service/SmsBackupService.java:311`, `service/SmsRestoreService.java:270` — `tearDownObserverAndCollector(...)` must call `removeObserver` (cache the LiveData reference). Low-complexity, two services.

## Estimation Guidance
Low.
