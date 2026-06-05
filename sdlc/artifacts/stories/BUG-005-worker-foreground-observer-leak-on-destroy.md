---
type: bug
status: done
artifact_type: bug
severity: high
priority: high
complexity: low
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items:
  - U-037
platforms: []
tags: []
id: BUG-005
title: Worker->foreground WorkInfo observer leaked on onDestroy (no removeObserver)
domain: build
origin: code-review
---

# BUG-005: Worker→foreground WorkInfo observer leaked on onDestroy

## Description
The U-031 worker→foreground bridge registers a `WorkInfo` LiveData observer via `observeForever` in `SmsBackupService`/`SmsRestoreService`, but the `onDestroy` teardown path nulls the observer reference **without** calling `removeObserver()`. The observer stays permanently registered. On service restart with the same unique-work name, a second observer is registered → duplicate `backupStateChanged()`/`restoreStateChanged()` callbacks (and a leak).

## Steps to Reproduce
1. Start a backup/restore (registers the WorkInfo observer).
2. Let the service be destroyed (`onDestroy`) and restarted (new run, same unique work name).
3. Observe: duplicate state callbacks fire; the prior `observeForever` registration is never removed.

## Expected Behavior
The WorkInfo observer is removed via `removeObserver()` on `onDestroy` (and on terminal state), so exactly one observer is active per run and none leaks across restarts.

## Actual Behavior
`tearDownObserverAndCollector(null)` (from `onDestroy`) nulls `workInfoObserver` but does not call `removeObserver()`; the LiveData retains the registration. Restarts accumulate observers → duplicate callbacks.

## Evidence
- `service/SmsBackupService.java:311` and `service/SmsRestoreService.java:270` — teardown nulls the observer without `removeObserver`.
- Consolidated code review: `sdlc/artifacts/build/reviews/consolidated-2026-06/code-review.md` finding B-2.

## Acceptance Criteria
- [ ] AC-1: `onDestroy` (and terminal-state teardown) unconditionally calls `removeObserver()` on the WorkInfo LiveData; no `observeForever` registration survives service destruction.
- [ ] AC-2: After a service destroy+restart cycle, exactly one observer is active and `*StateChanged()` is not invoked in duplicate.
- [ ] AC-3: Foreground promotion / stop behavior from the U-031 bridge is preserved.
- [ ] AC-4: Build green (assembleDebug, testDebugUnitTest, jacoco ≥70%).

### Integration Criteria
- [ ] IC-1: The `LiveData` reference is cached alongside `workInfoObserver` so teardown can call `removeObserver(observer)` regardless of path.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `service/SmsBackupService.java:311` | teardown nulls observer, no removeObserver | cache LiveData; call removeObserver in onDestroy/terminal |
| `service/SmsRestoreService.java:270` | same | same |

## Existing Behavior to Preserve
- The worker→foreground signalling (startForeground/stopForeground/stopSelf driven by `*StateChanged`) from U-031 must keep working for active runs.

## Verification Steps
1. Add a Robolectric/unit test simulating destroy+restart and asserting the observer is removed (no duplicate callback).
2. Code-review confirm `removeObserver` is called on every teardown path.

## Root Cause Analysis
<!-- Filled during planning phase -->

## Technical Context
- `observeForever` requires an explicit `removeObserver`; the fix caches the LiveData handle so teardown can remove the exact observer. Low-complexity, contained to the two services.

## Supporting Documentation
- `sdlc/artifacts/build/reviews/consolidated-2026-06/code-review.md` (B-2)

## Notes
Introduced by U-031's worker→foreground bridge. Part of the immediate B-1/B-2/B-3 fix batch.
