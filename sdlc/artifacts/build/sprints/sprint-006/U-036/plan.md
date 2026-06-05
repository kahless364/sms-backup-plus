---
artifact_type: plan
story_id: "U-036"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-04"
contracts_verified: []
risks_identified: 1
---

# Implementation Plan: U-036

## Story Overview
Unify the two `FlowSyncStateRepository` instances into one. Pre-fix: `App.java:160` creates a
manual static; `EventModule.kt` creates a SEPARATE one via `@Provides @Singleton`. Engine
emits to the static; `MainViewModel` (via Hilt) collects from the EventModule instance. State
never reaches the UI.

## Acceptance Criteria Mapping

| AC | Implementation Step | Notes |
|----|-------------------|-------|
| AC-1 | EventModule returns App static instance | Identity guaranteed by reference |
| AC-2 | Emission test in EventModuleSingleInstanceTest | StateFlow.value is shared |
| AC-3 | EventModule no longer constructs second FlowSyncStateRepository | Removed `FlowSyncStateRepository()` call |
| AC-4 | Engine + MainViewModel paths unchanged | Only EventModule changed |
| AC-5 | assembleDebug + testDebugUnitTest + jacoco green | Verified |
| AC-6 | Identity/emission test is device-free guard | Added EventModuleSingleInstanceTest |

## Implementation Steps

1. Modify `EventModule.kt`: change `provideSyncStateRepository()` to return `App.syncStateRepository()` instead of constructing a new `FlowSyncStateRepository()`.
2. Update `App.java` comments: document the single-instance guarantee and remove the stale TODO.
3. Add `EventModuleSingleInstanceTest.kt` with identity assertion, emission test, and idempotency guard.

## Files Modified/Created

| File | Action |
|------|--------|
| `app/src/main/java/com/zegoggles/smssync/di/EventModule.kt` | Changed `provideSyncStateRepository()` to return `App.syncStateRepository()` |
| `app/src/main/java/com/zegoggles/smssync/App.java` | Updated field/method comments, removed stale TODO |
| `app/src/test/java/com/zegoggles/smssync/di/EventModuleSingleInstanceTest.kt` | NEW — 3 tests |

## Race-Safety Argument

`EventModule.provideSyncStateRepository()` is a Hilt `@Singleton` — it is called at most once, the first time a `SyncStateRepository` is requested from the Hilt SingletonComponent. This request happens when `MainViewModel` is first created, which occurs when `MainActivity` starts. `App.onCreate()` completes (including `syncStateRepositoryInstance = new FlowSyncStateRepository()`) well before any Activity starts. Additionally, `App` itself only injects `Preferences` and `HiltWorkerFactory` — NOT `SyncStateRepository` — so the singleton is never instantiated during `App`'s own Hilt field injection (which happens in `super.onCreate()`). Therefore `App.syncStateRepository()` is always non-null when `provideSyncStateRepository()` is called.

## Contracts Verification

No `integration_contracts` listed in story frontmatter. CNTR-MODERNIZATION-006 (SyncStateRepository interface) is preserved: the same `FlowSyncStateRepository` instance is used, semantics unchanged.

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| App.syncStateRepository() null if called during App Hilt injection | N/A — SyncStateRepository is not injected into App; static is set before any consumer can call it |

## Test Strategy

Unit test `EventModuleSingleInstanceTest.kt` using reflection to set the App static, then asserting identity and emission. No Hilt test harness required.

## Phase Completion Report
---
story_id: "U-036"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-006/U-036/plan.md"
story_status: "done"
current_build_phase: "done"
errors: []
---
