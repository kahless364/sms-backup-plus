---
type: bug
status: ready
artifact_type: bug
severity: critical
priority: high
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items:
  - U-024
  - U-022
  - U-020
platforms: []
tags: []
id: BUG-004
title: 'Dual SyncStateRepository instances: engine state never reaches MainViewModel/UI'
domain: build
origin: code-review
---

# BUG-004: Dual SyncStateRepository instances — engine state never reaches the UI

## Description
There are **two** distinct `FlowSyncStateRepository` instances at runtime. The backup/restore engine (services + CoroutineWorkers) emits state to a **manual static** instance created in `App.onCreate`, while `MainActivity`/`MainViewModel` (made Hilt-injected in U-024) collect from a **separate Hilt-managed** instance provided by `EventModule`. State emitted by the engine therefore never reaches the UI's ViewModel. Introduced by U-024 (the `@HiltViewModel` switch): before U-024, `MainViewModelFactory` used `App.getRepository()` (the same static instance), so it worked.

## Steps to Reproduce
1. Configure an account and run a restore (or backup) so the engine emits `RestoreState`/`BackupState`.
2. Observe `MainActivity`/`MainViewModel`'s collected state.
3. Observe: the ViewModel never receives the engine's state — its `state`/`events` flows come from the EventModule instance, not the one the engine writes to.

## Expected Behavior
A single shared `SyncStateRepository` (`@Singleton`) instance is used by both the engine (via `App.syncStateRepository()`) and the Hilt graph (`MainViewModel`). State emitted by the engine is observed by `MainViewModel`.

## Actual Behavior
Two instances exist. UI-side consequences: post-restore `MainActivity.onRestoreStateChanged()` never fires → `restoreDefaultSmsProvider(...)` is silently skipped (user's default SMS app not restored); backup/restore progress/terminal updates routed through `MainViewModel` do not appear; permission-request callbacks and account/theme event routing in `onSyncEvent()` are broken.

## Evidence
- `App.java:160` — `syncStateRepositoryInstance = new FlowSyncStateRepository();` (manual static singleton; engine emits here via `App.syncStateRepository()`).
- `di/EventModule.kt` — `@Provides @Singleton SyncStateRepository` constructs a **separate** `new FlowSyncStateRepository()`.
- `activity/MainViewModel.kt:24` — `@HiltViewModel @Inject constructor(... SyncStateRepository ...)` (gets the EventModule instance).
- `activity/MainActivity.java:111,151-154` — `@AndroidEntryPoint`; `MainViewModel` obtained via Hilt's factory (the manual `MainViewModelFactory` that used `App.getRepository()` is no longer used).
- Consolidated code review: `sdlc/artifacts/build/reviews/consolidated-2026-06/code-review.md` finding B-1.

## Acceptance Criteria
- [ ] AC-1: Exactly one `SyncStateRepository` instance exists at runtime; `App.syncStateRepository()` returns the same object Hilt injects into `MainViewModel` (assert identity).
- [ ] AC-2: State emitted by the engine (`App.syncStateRepository().emitState(...)`) is observed by a collector of the Hilt-injected repository — verified by a unit/integration test.
- [ ] AC-3: Post-restore default-SMS-provider restoration, permission-request callbacks, and event routing through `MainViewModel` function (regression of the U-021/U-009 behaviors).
- [ ] AC-4: Build green (assembleDebug, testDebugUnitTest, jacocoTestCoverageVerification ≥70% per-package).

### Integration Criteria
- [ ] IC-1: `App` obtains the repository from Hilt (e.g. `@Inject SyncStateRepository`) and assigns `syncStateRepositoryInstance` from it, OR `EventModule` returns the App static instance — unifying both paths to one `@Singleton`.
- [ ] IC-2: All `App.syncStateRepository()` callers (services, workers) and the Hilt-injected `MainViewModel` resolve to the same instance.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `App.java:160` | Creates a manual static `FlowSyncStateRepository` | Source the repository from Hilt (`@Inject`) and assign the static from it (single instance) |
| `di/EventModule.kt` | `@Provides` constructs a second `FlowSyncStateRepository` | Provide the single shared instance (or have App consume Hilt's) |
| `activity/MainViewModel.kt` | Injects the EventModule instance | Unchanged once instances are unified |

## Existing Behavior to Preserve
- Engine emission path `App.syncStateRepository().emitState/tryEmitEvent/emitEvent` (services + workers) must keep working.
- `MainViewModel.state`/`events` and `StatusPreference` collection semantics (StateFlow conflation, SharedFlow replay=0) unchanged.
- Hilt `@HiltViewModel` MainViewModel creation (U-024) preserved.

## Verification Steps
1. Add a test asserting `App.syncStateRepository()` is the same object as the Hilt-provided `SyncStateRepository` (identity), and that a value emitted via one is visible via the other.
2. On-device (when available): run a restore; confirm `restoreDefaultSmsProvider` runs and UI reflects engine state.

## Root Cause Analysis
<!-- Filled during planning phase -->

## Technical Context
- Root cause: `EventModule.provideSyncStateRepository()` constructs its own `FlowSyncStateRepository` rather than returning the App-level static singleton the engine uses; U-024 switched the UI from the manual factory (which used the static) to Hilt, splitting the instances. Recommended fix (per review B-1): `@Inject SyncStateRepository` into `App` and assign the static accessor from it so non-Hilt callers (workers/services via `App.syncStateRepository()`) and Hilt consumers share one `@Singleton`.

## Supporting Documentation
- `sdlc/artifacts/build/reviews/consolidated-2026-06/code-review.md` (B-1)
- DES-MODERNIZATION-007 (SyncStateRepository), DES-MODERNIZATION-008 (Hilt)

## Notes
Highest-severity finding of the consolidated review. Functional regression invisible to build/unit/launch-smoke (both instances construct fine); only runtime state-flow reveals the split. On-device confirmation recommended after fix; the identity/emission unit test is the primary device-free guard.
