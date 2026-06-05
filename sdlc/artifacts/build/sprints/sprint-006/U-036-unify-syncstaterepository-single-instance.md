---
type: bug
status: done
sprint: '000006'
artifact_type: user-story
priority: high
complexity: medium
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
id: U-036
title: Unify SyncStateRepository to a single shared instance (fix BUG-004)
pipeline: ''
domain: modernization
requirement_source: bug:BUG-004
---

# U-036: Unify SyncStateRepository to a single shared instance

## Story
As a user of SMS Backup+, I want backup/restore state emitted by the engine to actually reach the UI, so that post-restore default-SMS restoration, progress/terminal updates, permission prompts, and event routing work — by making the engine and the Hilt graph share ONE `SyncStateRepository` instance.

## Source
Fixes **BUG-004** (critical) — see `sdlc/artifacts/stories/BUG-004-dual-syncstaterepository-instances-state-not-delivered.md` for full root cause/evidence.

## Acceptance Criteria
1. AC-1: Exactly one `SyncStateRepository` (`@Singleton`) exists at runtime; `App.syncStateRepository()` returns the SAME object Hilt injects into `MainViewModel` — assert identity in a test.
2. AC-2: A value emitted via `App.syncStateRepository().emitState(...)` is observed by a collector of the Hilt-injected `SyncStateRepository` (unit/integration test).
3. AC-3: `EventModule` no longer constructs a second `FlowSyncStateRepository`; the App static accessor and Hilt resolve to one instance (recommended: `@Inject SyncStateRepository` into `App`, assign `syncStateRepositoryInstance` from it — preserve the static accessor for non-Hilt callers: workers/services).
4. AC-4: Engine emission path (`emitState`/`tryEmitEvent`/`emitEvent` from services + workers) and `MainViewModel.state`/`events` + `StatusPreference` collection semantics preserved; `@HiltViewModel` MainViewModel creation (U-024) preserved.
5. AC-5: Build green — assembleDebug, testDebugUnitTest, jacocoTestCoverageVerification (per-package LINE ≥70%).
6. AC-6 (verification note): on-device confirmation (restore → `restoreDefaultSmsProvider` runs; UI reflects engine state) recommended when device available; the AC-1/AC-2 identity+emission test is the device-free guard.

## Technical Notes
- Files: `App.java:160` (static instance), `di/EventModule.kt` (second instance), `activity/MainViewModel.kt`/`MainActivity.java` (Hilt consumer). Per BUG-004 / review B-1: `@Inject SyncStateRepository` into `App`; assign the static from the injected singleton so workers/services (via `App.syncStateRepository()`) and Hilt consumers share it. Hilt injects App fields after super.onCreate — ensure the static is assigned before any consumer reads it (verify ordering; App.onCreate constructs many things). If ordering is awkward, alternative: `EventModule.provideSyncStateRepository()` returns the App static instance.
- This is the critical fix of the batch; prioritize correctness of the single-instance wiring + the identity test.

## Estimation Guidance
Medium — DI wiring change + ordering care + an identity/emission test.
