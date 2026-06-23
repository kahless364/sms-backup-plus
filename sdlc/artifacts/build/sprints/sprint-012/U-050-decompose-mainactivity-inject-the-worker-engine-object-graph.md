---
type: story
status: done
artifact_type: user-story
priority: medium
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
id: U-050
title: Decompose MainActivity, inject the worker engine object graph, remove stale scheduler seams
pipeline: ''
domain: modernization
requirement_source: assessment:20260623-post-migration-assessment#AR-003
sprint: '000012'
updated_at: '2026-06-23T20:19:28.288Z'
resolution: done
---

# U-050: Decompose MainActivity, inject the worker engine object graph, remove stale scheduler seams

## Story
As a maintainer of the SMS Backup+ codebase, I want `MainActivity` decomposed into its existing `*FlowHelper` collaborators, the `MainViewModelFactory` replaced by Hilt's `by viewModels()`, the per-run worker object graph injected via Hilt rather than hand-`new`-ed inside `BackupWorker`, and all stale seams and KDoc removed, so that the god-class is broken up, the DI graph covers the engine core, and no misleading dead code remains.

## Source
Derived from assessment 20260623-post-migration-assessment, findings AR-003, AR-004, AR-005. See `sdlc/analysis/20260623-post-migration-assessment/outputs/sms-backup-plus/architecture.md`.

## Acceptance Criteria
- [ ] AC-1: `MainActivity` line count is reduced by moving distinct concern blocks (navigation, SMS-role negotiation, dialog flow, scheduler dispatch) into the existing `*FlowHelper` classes or new focused collaborators; the resulting `MainActivity` delegates to those helpers and does not contain inline business logic that belongs in a named collaborator.
- [ ] AC-2: `MainViewModelFactory` is removed; `MainActivity` obtains `MainViewModel` via `by viewModels()` (the `TODO U-022` tag is gone); Hilt provides the ViewModel through the standard `@HiltViewModel` path with no manual factory.
- [ ] AC-3: `MessageConverter`, `PersonLookup`, `TokenRefresher`, `CalendarSyncer`, and `ContactAccessor` are no longer `new`-ed inside `BackupWorker.kt:211-233`; they are injected into `BackupWorker` via Hilt (added to the worker's `@AssistedInject` or `@Inject` constructor as appropriate for WorkManager's assisted-inject pattern).
- [ ] AC-4: `WorkManagerScheduler.observe()` no longer returns a hardcoded stub state ("until U-020 lands" comment removed); the method either returns real observation or is deleted if callers no longer need it; the removed-`LegacyScheduler` KDoc is deleted from `WorkManagerScheduler`; the `Configuration.Provider` reflective test-env branch in `App.java:224` is replaced with a clean test-only override that does not exist in the production path.
- [ ] AC-5: Build green: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` passes; existing `MainActivityRestoreTest` and themed-dialog behavior (U-034) continue to pass without modification.

## Affected Code
| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` (~615 lines) | Mixes navigation, SMS-role negotiation, dialog flow, service dispatch in one class | Delegate distinct concern blocks to `*FlowHelper` classes; reduce line count meaningfully |
| `app/src/main/java/com/zegoggles/smssync/activity/MainViewModelFactory.java` (or `.kt`) | Manual `ViewModelProvider.Factory` for `MainViewModel`; tagged `TODO U-022` | Delete; replace `MainActivity` usage with `by viewModels()` |
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt:211-233` | `new MessageConverter(...)`, `new PersonLookup(...)`, `new TokenRefresher(...)`, `new CalendarSyncer(...)`, `new ContactAccessor(...)` hand-constructed per run | Inject all five via Hilt (assisted-inject or `@Inject` constructor params); remove manual `new` calls |
| `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt` | `observe()` returns hardcoded state stub; KDoc references removed `LegacyScheduler` | Remove/fix stub; delete stale KDoc |
| `app/src/main/java/com/zegoggles/smssync/App.java:224` | `Configuration.Provider` reflective branch contains test-env logic in production | Replace with clean test-only configuration override |

## Existing Behavior to Preserve
- U-034 themed-dialog behavior: all confirmation dialogs shown by `MainActivity` (backup overwrite, restore confirm, account change) must continue to use the AppCompat themed dialog path that U-034 established; decomposition must not regress dialog theming.
- `MainActivityRestoreTest` Robolectric tests: `MainActivity.startRestore()` must still trigger the SMS-role request intent as asserted by the existing test.
- `@HiltViewModel MainViewModel` injection and the `MainViewModel.state`/`events` collection semantics must be unchanged by the factory removal.
- Worker cooperative cancellation (`ensureActive()` calls in `BackupWorker.kt:315`) and `finally` cursor cleanup must be preserved exactly as-is regardless of how the engine object graph is injected.

## Verification Steps
1. Run `./gradlew :app:assembleDebug` — no compilation errors; Hilt graph validation passes for `BackupWorker` with its new injected collaborators.
2. Search for `new MainViewModelFactory` and `MainViewModelFactory(` in production sources — expect zero results.
3. Search for `new MessageConverter(`, `new PersonLookup(`, `new TokenRefresher(`, `new CalendarSyncer(`, `new ContactAccessor(` inside `BackupWorker.kt` — expect zero results.
4. Search for `LegacyScheduler` in all sources — expect zero results.
5. Run `./gradlew :app:testDebugUnitTest` — `MainActivityRestoreTest` passes; themed-dialog tests (if any) pass.
6. Run `./gradlew :app:jacocoTestCoverageVerification` — all gated packages at or above LINE 70%.

## Technical Context
- AR-003 (MainActivity god-class): the existing `*FlowHelper` classes (e.g. `AuthFlowHelper`, `PermissionFlowHelper`) were introduced in a prior sprint specifically to receive decomposed blocks from `MainActivity`; this story completes that decomposition for the remaining inline blocks.
- AR-004 (per-run hand-new): WorkManager supports `HiltWorkerFactory` via `@HiltWorkerFactory`/`@AssistedInject` — injecting engine collaborators into workers requires configuring the custom `WorkerFactory` in `App.java` (this may already be partially in place from prior Hilt work). The five collaborators must be added to the `@AssistedInject` or standard `@Inject` constructor of `BackupWorker`.
- AR-005 (stale seams): U-020 has already landed (WorkManagerScheduler observation works); the hardcoded stub in `observe()` is dead but misleading. The `Configuration.Provider` branch in `App.java:224` is a test-env hack that puts test-env logic in the production `Application` class and should be moved to a test-only subclass or test rule.
- This story should sequence after U-049 (service retirement) because the decomposition of `MainActivity` dispatcher logic is cleaner once `startService` calls are already gone.

## Notes
- Sprint A (DI & Legacy Retirement). Covers AR-003 + AR-004 + AR-005 as a coherent cleanup after U-048 and U-049 establish the unified DI graph.
- AR-005 items (stale seams/docs) are low-complexity; include them here rather than as a separate story to avoid a trivial one-liner story.
