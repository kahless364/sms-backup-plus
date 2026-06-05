---
artifact_type: implementation-log
story_id: "U-036"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-04"
files_changed: 2
files_created: 1
tests_added: 3
tests_passing: 615
---

# Implementation Log: U-036

## Summary
Fixed BUG-004: unified the two `FlowSyncStateRepository` instances into one. The fix is in
`EventModule.kt`: `provideSyncStateRepository()` now returns `App.syncStateRepository()` (the
static instance initialized in `App.onCreate()`) instead of constructing a second
`FlowSyncStateRepository`. Engine callers (services, workers via `App.syncStateRepository()`)
and Hilt consumers (`MainViewModel`) now share exactly one `@Singleton` instance.

## Single-Instance Mechanism

**Chosen approach**: `EventModule.provideSyncStateRepository()` returns the App static instance.

**Alternative considered**: `@Inject SyncStateRepository` field in `App` — rejected because it
would require assigning `syncStateRepositoryInstance` AFTER `super.onCreate()` Hilt injection,
but the assignment is at line 160 (AFTER super.onCreate on line 154). The chosen approach avoids
any ordering concern by making EventModule a thin delegator to the already-initialized static.

**Race-safety**: `App.syncStateRepository()` is set in `App.onCreate()` before `super.onCreate()`
returns... correction: `super.onCreate()` is called on line 154, then the static is assigned on
line 160. Hilt injects `App`'s own fields (`@Inject Preferences`, `@Inject HiltWorkerFactory`)
during `super.onCreate()` — but NOT `SyncStateRepository`, because App does not declare an
`@Inject SyncStateRepository` field. Therefore, `EventModule.provideSyncStateRepository()` is
never invoked during App's own Hilt injection. It is invoked only when a Hilt consumer first
requests `SyncStateRepository` (e.g., when `MainActivity` creates `MainViewModel`), which
happens after `App.onCreate()` completes. The static is non-null at that point.

## Files Modified

| File | Change |
|------|--------|
| `app/src/main/java/com/zegoggles/smssync/di/EventModule.kt` | `provideSyncStateRepository()`: removed `FlowSyncStateRepository()` constructor call; replaced with `App.syncStateRepository()`. Updated KDoc with race-safety explanation. Removed unused `FlowSyncStateRepository` import. |
| `app/src/main/java/com/zegoggles/smssync/App.java` | Updated `syncStateRepositoryInstance` field comment (documents single-instance guarantee). Updated `syncStateRepository()` method KDoc. Removed stale TODO comments. |

## Files Created

| File | Purpose |
|------|---------|
| `app/src/test/java/com/zegoggles/smssync/di/EventModuleSingleInstanceTest.kt` | AC-1/AC-2 identity + emission test. 3 tests. |

## Test Results

- `EventModuleSingleInstanceTest.provideSyncStateRepository_returns_same_instance_as_App_syncStateRepository` — AC-1 identity assertion
- `EventModuleSingleInstanceTest.state_emitted_via_App_syncStateRepository_is_observed_via_EventModule_instance` — AC-2 emission test
- `EventModuleSingleInstanceTest.provideSyncStateRepository_called_twice_returns_same_instance` — idempotency guard

All 615 tests pass (3 new). Build: assembleDebug green. jacoco green.

## Regression Results

No regressions. `MainViewModel` still injects `SyncStateRepository` via Hilt and gets the same
(and only) instance. All existing `App.syncStateRepository()` callers (services, workers,
`FlowCollectHelper`) continue to work unchanged.

## Integration Verification

New code integration path: `EventModule.provideSyncStateRepository()` → `App.syncStateRepository()` → `syncStateRepositoryInstance`. The Hilt SingletonComponent calls `provideSyncStateRepository()` when `MainViewModel` is first constructed by Hilt's ViewModel factory. This path is triggered by `MainActivity` starting (existing production entry point, `@AndroidEntryPoint`, `@HiltViewModel`).

## Notes

- On-device confirmation (restore → `restoreDefaultSmsProvider` runs; UI reflects engine state) is recommended per AC-6 when a device is available. The identity/emission unit test is the device-free guard.
- The `FlowSyncStateRepository` import in `EventModule.kt` was removed (no longer needed — the module no longer constructs an instance).

## Phase Completion Report
---
story_id: "U-036"
phase: "implementation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-006/U-036/implementation-log.md"
story_status: "done"
current_build_phase: "done"
files_changed: [
  "app/src/main/java/com/zegoggles/smssync/di/EventModule.kt",
  "app/src/main/java/com/zegoggles/smssync/App.java"
]
tests_run: 615
tests_passed: 615
errors: []
---
