---
type: story
status: planned
sprint: "000001"
artifact_type: user-story
priority: high
complexity: medium
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-007
design_docs:
  - DES-MODERNIZATION-007
integration_contracts:
  - CNTR-MODERNIZATION-006
dependencies:
  - U-003
change_records: []
platforms: []
tags:
  - otto
  - stateflow
  - branch-by-abstraction
  - facade
  - syncstate-repository
gate_additions: []
id: U-019
title: 'SyncStateRepository Facade: define interface + SyncEvent sealed class and introduce Otto-delegating DefaultSyncStateRepository with zero runtime behavior change'
pipeline: ''
domain: modernization
requirement_source: authored
---

# U-019: SyncStateRepository Facade — define interface + SyncEvent sealed class and introduce Otto-delegating DefaultSyncStateRepository with zero runtime behavior change

## Story

As a maintainer of the SMS Backup+ codebase,
I want to introduce the `SyncStateRepository` interface and `SyncEvent` sealed class (with all contract-specified variants) alongside a `DefaultSyncStateRepository` that delegates every `emitState`/`emitEvent`/`tryEmitEvent` call to `App.bus` and every collection to Otto `@Subscribe`, injected as a manual application singleton,
so that the eventing seam is seamed behind a typed, lifecycle-aware abstraction without changing any runtime behavior, enabling subsequent steps to swap the backing implementation from Otto to `StateFlow`/`SharedFlow` independently per event type and eventually delete the Otto dependency entirely.

## Acceptance Criteria

- [ ] AC-1: A Kotlin file defines `interface SyncStateRepository` in the package `com.zegoggles.smssync.service.state` with exactly the four members fixed by CNTR-MODERNIZATION-006: `val state: StateFlow<SyncState>`; `val events: SharedFlow<SyncEvent>`; `fun emitState(newState: SyncState)`; `suspend fun emitEvent(event: SyncEvent)`; and `fun tryEmitEvent(event: SyncEvent): Boolean`. No additional public members may be added. Verified by inspecting the Kotlin source file; the five signatures compile without error when Kotlin + `kotlinx-coroutines-core` are on the classpath (MU-001 prerequisite).

- [ ] AC-2: A Kotlin file defines `sealed class SyncEvent` in the package `com.zegoggles.smssync.service.state` with exactly the following members matching CNTR-MODERNIZATION-006 §Type: SyncEvent:
  - `data class Cancel(val origin: Origin = Origin.USER) : SyncEvent()` with nested `enum class Origin { USER, SYSTEM }` and `fun mayInterruptIfRunning(): Boolean = origin == Origin.SYSTEM`. `Origin` is `public`. Default `origin` is `USER`, matching the `CancelEvent()` no-arg constructor behavior (`service/CancelEvent.java:9-11`).
  - `data class PerformActionRequested(val action: PerformAction.Actions, val confirm: Boolean) : SyncEvent()` importing `activity/events/PerformAction.Actions`. Both the `Actions` enum and the `confirm` flag are preserved verbatim (`activity/events/PerformAction.java:3-17`).
  - `data class MissingPermissions(val permissions: List<AppPermission>) : SyncEvent()` with payload type `List<AppPermission>` (not `List<String>`), matching `activity/events/MissingPermissionsEvent.java:7-12`.
  - `data class OAuth2Callback(val payload: OAuth2CallbackTask.OAuth2Callback) : SyncEvent()` — exact payload shape confirmed at implementation by reading `OAuth2CallbackTask.OAuth2CallbackEvent` fields; this AC is satisfied when the field type and name match the source POJO's fields verbatim.
  - `object AccountAdded : SyncEvent()`, `object AccountRemoved : SyncEvent()`, `object AccountConnectionChanged : SyncEvent()`, `object AutoBackupSettingsChanged : SyncEvent()`, `object FallbackAuth : SyncEvent()`, `object SettingsReset : SyncEvent()`.
  - `data class ThemeChanged(val themeId: Int) : SyncEvent()` — payload type confirmed at implementation; this AC is satisfied when the field matches the source theme event's payload verbatim (confirm by reading source POJO before fixing the field).
  Verified by static inspection of the sealed class source: every variant listed above is present; no variant is absent; no additional variants are added without a contract revision.

- [ ] AC-3: A Kotlin class `DefaultSyncStateRepository` implements `SyncStateRepository` and delegates every call to `App.bus` with no new behavior:
  - `emitState(newState: SyncState)` calls `App.post(newState)` — identical to the current `App.post(state)` call path at `SmsBackupService.java:194` and `SmsRestoreService.java:111`.
  - `emitEvent(event: SyncEvent)` (suspending) calls `App.post(event)` — no coroutine suspension in this facade step; the suspend modifier is present to satisfy the interface, but the body is non-suspending in the Otto-delegating impl.
  - `tryEmitEvent(event: SyncEvent): Boolean` calls `App.post(event)` and returns `true` — the Otto bus does not buffer, so no drop scenario exists in the facade step; `true` is the correct delegation result for a bus that does not signal back-pressure.
  - `state` and `events` are not functional reactive surfaces in this facade step; they are stubs declared to satisfy the interface (their implementations are deferred to the Flow-swap step, MU-006 Step 2). The stubs MUST compile and be accessible but are not expected to deliver values in this step. Stubs may be implemented as `MutableStateFlow(BackupState())` and `MutableSharedFlow()` respectively to satisfy the interface contract at the type level while the bus remains the live channel.
  Verified by code inspection: every `SyncStateRepository` method in `DefaultSyncStateRepository` is present; `emitState` and `emitEvent`/`tryEmitEvent` each delegate to `App.post`; no logic beyond delegation is introduced; the class compiles without error.

- [ ] AC-4: `com.squareup:otto` remains on the classpath after this story. Verified by `grep "com.squareup:otto" app/build.gradle` returning the existing Otto dependency coordinate unchanged. No Otto import is removed from any of the 12 importing files. `App.bus`, `App.register`, `App.unregister`, and `App.post` are not deleted. The 28 `@Subscribe`/`@Produce` annotated methods are not removed. No `@Subscribe` handler is converted to a Flow collector in this story. This story is the facade step only; the swap step (removing Otto) is out of scope.

- [ ] AC-5: `DefaultSyncStateRepository` is instantiated as a manual application singleton and held on `App` (or an equivalent application-scoped holder). `App.java` (or its Kotlin replacement) constructs a single `DefaultSyncStateRepository` instance in `onCreate` and exposes it via a static or companion accessor (`App.syncStateRepository()` or equivalent). No Hilt `@Singleton` annotation is introduced — Hilt retrofit is deferred to MU-007. Verified by reading `App.java`/`App.kt`: one construction site exists, no second construction site is present in the codebase (`grep -r "DefaultSyncStateRepository(" app/src` returns exactly one match), and the accessor is reachable from at least one production call site.

- [ ] AC-6: At least one existing `App.post(...)` call site is replaced with a call to `App.syncStateRepository().emitState(...)` or `App.syncStateRepository().tryEmitEvent(...)` as a proof-of-life integration check, demonstrating the facade is wired into the live call path. The chosen call site MUST be one of the verified engine emit sites (`SmsBackupService.java:194` — `App.post(state)` — or `SmsRestoreService.java:111` — the `postError`-driven `App.post`). The replaced call site's behavior is identical at runtime (the facade delegates to `App.post` internally). The remaining 11 importing files are NOT migrated in this story — their migration is out of scope. Verified by `grep "App.post" SmsBackupService.java` (or the target file) showing one fewer direct `App.post` call at the migrated site, and by a passing build.

- [ ] AC-7: `./gradlew test` passes with no newly failing tests after all changes in this story. `./gradlew assembleRelease` exits 0. The `warningsAsErrors true` and `-Werror -Xlint:deprecation` flags in `app/build.gradle:41,73-77` are satisfied: no new deprecation warnings are introduced (the new Kotlin file does not use deprecated APIs; the delegating Otto calls are retained exactly as before).

- [ ] AC-8: A unit test verifies the `Cancel` event's `Origin` semantics: constructing `SyncEvent.Cancel(SyncEvent.Cancel.Origin.USER)` and asserting `mayInterruptIfRunning()` returns `false`; constructing `SyncEvent.Cancel(SyncEvent.Cancel.Origin.SYSTEM)` and asserting `mayInterruptIfRunning()` returns `true`; constructing `SyncEvent.Cancel()` (no-arg default) and asserting `origin == Origin.USER` and `mayInterruptIfRunning()` returns `false`. This test lives in `app/src/test/` and passes in `./gradlew test`. It guards the load-bearing USER/SYSTEM distinction that drives interrupt behavior in the engine.

### Integration Criteria

- [ ] IC-1: `DefaultSyncStateRepository` is constructed exactly once, at `App.onCreate`, and is accessible via `App.syncStateRepository()` (or the chosen static accessor name). Verified by reading `App.java`/`App.kt` and confirming a single construction site and a single accessor; `grep -rn "DefaultSyncStateRepository(" app/src` returns exactly one match.
- [ ] IC-2: The proof-of-life call site (AC-6) reaches `DefaultSyncStateRepository.emitState` or `DefaultSyncStateRepository.tryEmitEvent` through the production code path. Verified by adding a temporary log line to `DefaultSyncStateRepository.emitState` during development, triggering a backup, and confirming the log line appears; or by a unit test that constructs `DefaultSyncStateRepository` with a test double for `App.bus` and asserts `App.post` is called when `emitState` is called.
- [ ] IC-3: All call sites that have been migrated to the new `emitState`/`emitEvent`/`tryEmitEvent` surface compile against the exact signatures fixed by CNTR-MODERNIZATION-006: `fun emitState(newState: SyncState)`, `suspend fun emitEvent(event: SyncEvent)`, `fun tryEmitEvent(event: SyncEvent): Boolean`. No signature mismatch exists. Verified by `./gradlew assembleRelease` exiting 0.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/state/SyncStateRepository.kt` (new) | Does not exist | Create: defines `interface SyncStateRepository` with `state`, `events`, `emitState`, `emitEvent`, `tryEmitEvent` per CNTR-MODERNIZATION-006 |
| `app/src/main/java/com/zegoggles/smssync/service/state/SyncEvent.kt` (new) | Does not exist | Create: defines `sealed class SyncEvent` with all 11 variants per CNTR-MODERNIZATION-006 §Type: SyncEvent |
| `app/src/main/java/com/zegoggles/smssync/service/state/DefaultSyncStateRepository.kt` (new) | Does not exist | Create: implements `SyncStateRepository` by delegating every emit to `App.bus`; stubs `state`/`events` as inert reactive surfaces |
| `app/src/main/java/com/zegoggles/smssync/App.java` | Holds `private static final Bus bus`; `register`, `unregister`, `post` static helpers | Add construction of `DefaultSyncStateRepository` singleton in `onCreate`; expose via `static SyncStateRepository syncStateRepository()` accessor; no other modifications |
| `app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java` | `App.post(state)` at `:194` (inside `moveToState`) | Replace with `App.syncStateRepository().emitState(state)` — the proof-of-life migration (AC-6); facade delegates back to `App.post` internally, so runtime behavior is identical |
| `app/src/test/java/com/zegoggles/smssync/service/state/SyncEventTest.kt` (new) | Does not exist | Create: unit tests for `Cancel` `Origin` semantics (AC-8) |

## Existing Behavior to Preserve

- All 28 `@Subscribe`/`@Produce` annotated handler methods across the 12 Otto-importing files continue to fire exactly as before. Otto is still on the classpath and still dispatches every event.
- `App.bus`, `App.register`, `App.unregister`, and the existing `App.post(Object)` static method are not removed or modified.
- `SmsBackupService.produceLastState()` (`@Produce`, `:202-204`) and `SmsRestoreService.produceLastState()` (`@Produce`, `:157-159`) continue to provide last-state sticky semantics to all `@Subscribe` handlers that register after the service is running.
- The two `private static` service back-references `SmsBackupService.service` (`:66`) and `SmsRestoreService.service` (`:40`), and their `isServiceWorking()`/`isServiceIdle()` queries, are not modified; they are deleted only in Step 3 (Otto removal), which is out of scope here.
- `BackupState`, `RestoreState`, `State`, and `SmsSyncState` (the Preserved Core, MU-000) are not modified in any way — no new fields, no new methods, no `equals`/`hashCode` overrides, no supertype changes.
- `./gradlew test` continues to pass with no regressions; the Robolectric suite executes unmodified.
- The `com.zegoggles.smssync.BACKUP` public broadcast receiver contract is not altered.

## Verification Steps

1. **AC-1 — Interface exists with correct signatures:** Run `grep -n "interface SyncStateRepository" app/src/main/java/com/zegoggles/smssync/service/state/SyncStateRepository.kt`. Confirm the file exists and the interface is declared. Then inspect the file for the five member signatures: `val state: StateFlow<SyncState>`, `val events: SharedFlow<SyncEvent>`, `fun emitState(newState: SyncState)`, `suspend fun emitEvent(event: SyncEvent)`, `fun tryEmitEvent(event: SyncEvent): Boolean`. Run `./gradlew assembleRelease` and confirm exit 0 — the interface compiles.

2. **AC-2 — SyncEvent sealed class:** Inspect `app/src/main/java/com/zegoggles/smssync/service/state/SyncEvent.kt`. Confirm `sealed class SyncEvent` is declared. Confirm each of the 11 variants is present: `Cancel`, `PerformActionRequested`, `MissingPermissions`, `OAuth2Callback`, `AccountAdded`, `AccountRemoved`, `AccountConnectionChanged`, `AutoBackupSettingsChanged`, `FallbackAuth`, `SettingsReset`, `ThemeChanged`. Confirm `Cancel.Origin` is a `public` nested `enum class` with values `USER` and `SYSTEM`. Confirm `PerformActionRequested` has both `action: PerformAction.Actions` and `confirm: Boolean`. Confirm `MissingPermissions` payload is `List<AppPermission>`. Run `./gradlew assembleRelease` and confirm exit 0.

3. **AC-3 — DefaultSyncStateRepository delegation:** Inspect `app/src/main/java/com/zegoggles/smssync/service/state/DefaultSyncStateRepository.kt`. Confirm `class DefaultSyncStateRepository : SyncStateRepository`. Confirm `emitState` body calls `App.post(newState)`. Confirm `emitEvent` body calls `App.post(event)`. Confirm `tryEmitEvent` body calls `App.post(event)` and returns `true`. Confirm `state` is initialized (e.g. `MutableStateFlow(BackupState())`). Confirm `events` is initialized (e.g. `MutableSharedFlow()`). Run `./gradlew assembleRelease` and confirm exit 0.

4. **AC-4 — Otto still present:** Run `grep "com.squareup:otto" app/build.gradle`. Confirm the Otto coordinate is present and unchanged. Run `grep -rc "com.squareup.otto" app/src/main/java`. Confirm the count across all 12 files is unchanged from pre-story (15 import lines across 12 distinct files). Run `grep -c "App.bus" app/src/main/java/com/zegoggles/smssync/App.java`. Confirm `App.bus` is still declared.

5. **AC-5 — Manual singleton:** Run `grep -rn "DefaultSyncStateRepository(" app/src`. Confirm exactly one construction site exists. Inspect `App.java`/`App.kt` and confirm a `syncStateRepository()` static accessor is present and returns the singleton. Confirm no `@Singleton` Hilt annotation is present on `DefaultSyncStateRepository`.

6. **AC-6 — Proof-of-life migration:** Run `grep -n "App.post" app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java`. Confirm the `:194` call site is no longer `App.post(state)` but instead `App.syncStateRepository().emitState(state)`. Run `./gradlew assembleRelease` and confirm exit 0. (Runtime verification: trigger a backup via the app and observe that the status row updates — the facade delegation through `App.post` means Otto still dispatches the state to all `@Subscribe` handlers.)

7. **AC-7 — Build and test pass:** Run `./gradlew test`. Confirm exit 0 and zero newly failing tests. Run `./gradlew assembleRelease`. Confirm exit 0. Run `./gradlew lint`. Confirm exit 0 (no new deprecation warnings introduced).

8. **AC-8 — Cancel Origin unit test:** Run `./gradlew test --tests "*.SyncEventTest"` (or equivalent Robolectric test class name). Confirm the test class exists and all three `Cancel` Origin assertions pass: `Cancel(Origin.USER).mayInterruptIfRunning()` is `false`; `Cancel(Origin.SYSTEM).mayInterruptIfRunning()` is `true`; `Cancel()` (default) has `origin == Origin.USER` and `mayInterruptIfRunning()` is `false`.

9. **IC-2 — Delegation end-to-end:** In a unit test (or by inspection), construct a `DefaultSyncStateRepository` test double where `App.post` is intercepted. Call `emitState(BackupState())`. Assert that `App.post` was called with the same `BackupState` instance. This confirms the delegation wiring is correct before any live device test.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android / Kotlin | Define `SyncStateRepository` interface, `SyncEvent` sealed class, `DefaultSyncStateRepository` delegation impl; wire singleton into `App`; migrate one proof-of-life call site; write `SyncEventTest` | Developer |

## Technical Context

**This story implements Step 1 (Facade) of the three-step branch-by-abstraction migration described in DES-MODERNIZATION-007 §Migration path and REQ-MODERNIZATION-007 §Constraints.** It is the zero-behavior-change increment: `DefaultSyncStateRepository` wraps `App.bus` without replacing it. The backing implementation swap (Step 2: `MutableStateFlow`/`MutableSharedFlow`) and Otto deletion (Step 3) are each separate stories.

**Why facade before swap:** Feathers' "Introduce Instance Delegator" pattern (*Working Effectively with Legacy Code*, Ch. 25) establishes the typed seam first with no observable change, providing a shippable, independently testable, reversible increment. Swapping the implementation behind the interface in Step 2 can then proceed per event type without touching the interface or the call sites.

**SyncState is a typealias for the existing State hierarchy (CNTR-MODERNIZATION-006 §Type: SyncState):** `typealias SyncState = com.zegoggles.smssync.service.state.State`. `BackupState` and `RestoreState` are the concrete carried payloads, unmodified (MU-000 Preserved-Core invariant). Do NOT add `equals`/`hashCode` to any `State` subclass — this would cause `StateFlow` to conflate progress ticks in Step 2.

**`Cancel.Origin` is load-bearing (CNTR-MODERNIZATION-006 §Validation Rules rule 7):** The USER/SYSTEM distinction drives `mayInterruptIfRunning()`, which in turn drives whether the backup thread may be interrupted. `CancelEvent.java:5-19` carries a package-private `Origin` enum; the migration promotes it to `public` as a deliberate minimal public-API surface change. The no-arg default origin is `USER`, matching `CancelEvent()`'s behavior.

**`PerformActionRequested.confirm` is load-bearing:** The `confirm` flag gates the confirmation dialog path in `Dialogs.performAction` (`Dialogs.java:331-343`). Dropping it would silently skip the confirmation prompt for destructive actions.

**Otto still owns the live dispatch in this step:** Every `emitState`/`emitEvent`/`tryEmitEvent` in `DefaultSyncStateRepository` calls `App.post(...)`, which calls `bus.post(...)`. All existing `@Subscribe` handlers continue to receive events via Otto reflection. No Flow collection path is active in this step.

**Manual singleton, no Hilt:** DES-MODERNIZATION-008 (Hilt, MU-007) is scheduled after this unit. `DefaultSyncStateRepository` must be held as a manual singleton on `App` using the existing dual-constructor manual-DI discipline documented in patterns.md §Manual DI. Introducing `@Singleton` or any Hilt annotation here would create a backwards dependency on an unlanded unit (DES-MODERNIZATION-007 §Hilt injection).

**`tryEmitEvent` returns `true` from the facade:** Otto's `bus.post()` is synchronous and does not signal back-pressure. In the Otto-delegating implementation, `tryEmitEvent` always returns `true`. When the Flow-backed implementation replaces this in Step 2, `tryEmitEvent` will delegate to `MutableSharedFlow.tryEmit()` and may return `false`. Callers added in this step must therefore already handle a `false` return (log a warning at minimum) so they are already correct for Step 2.

**`emitEvent` suspend modifier in the facade:** The suspending signature is present to satisfy the `SyncStateRepository` interface contract (CNTR-MODERNIZATION-006). In `DefaultSyncStateRepository`, the body calls `App.post(event)` synchronously; no suspension occurs. The Kotlin compiler will insert a `Continuation` parameter and return `Unit` synchronously, which is fully correct for a coroutine-safe suspend function with no actual suspension point.

**`state`/`events` stubs in the facade:** The `state` and `events` properties are required by the interface. In the facade step they may be initialized as `MutableStateFlow(BackupState())` and `MutableSharedFlow()` respectively. They will not carry live values in Step 1 because no collector is wired up and emission still goes through Otto. They become live in Step 2. The stub initialization must not throw and must compile cleanly.

**`warningsAsErrors true` and `-Xlint:deprecation` (`app/build.gradle:41,73-77`):** The new Kotlin files must not use any deprecated Android or Kotlin API. Kotlin coroutines APIs used (`StateFlow`, `SharedFlow`, `MutableStateFlow`, `MutableSharedFlow`, `asStateFlow`, `asSharedFlow`) are all stable in the `kotlinx-coroutines-core` version that arrives with MU-001.

**Key file locations (all paths relative to repo root):**

| File | Relevant lines |
|------|---------------|
| `app/src/main/java/com/zegoggles/smssync/App.java` | `:59` `App.bus`; `:105` `App.register(this)` in `onCreate`; `:116-130` register/unregister swallow; `:132` `App.post` |
| `app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java` | `:194` `App.post(state)` proof-of-life migration target; `:202-204` `@Produce produceLastState()` (do not touch); `:66` static service field (do not touch) |
| `app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java` | `:111` `postError`-driven `App.post`; `:157-159` `@Produce`; `:40` static service field (do not touch) |
| `app/src/main/java/com/zegoggles/smssync/service/CancelEvent.java` | `:5-19` `Origin` enum USER/SYSTEM; `:9-11` no-arg ctor |
| `app/src/main/java/com/zegoggles/smssync/activity/events/PerformAction.java` | `:3-17` `Actions` enum + `confirm` flag |
| `app/src/main/java/com/zegoggles/smssync/activity/events/MissingPermissionsEvent.java` | `:7-12` `List<AppPermission>` payload |
| `app/build.gradle` | `:57` Otto dependency; `:41` warningsAsErrors; `:73-77` -Werror |

## Supporting Documentation

- `REQ-MODERNIZATION-007` §Constraints (facade-first migration strategy, MU-001 prerequisite, MU-005 sequencing)
- `DES-MODERNIZATION-007` §Migration path — branch-by-abstraction (Step 1 Facade specification)
- `DES-MODERNIZATION-007` §Hilt injection (manual-singleton constraint for Steps 1–2)
- `DES-MODERNIZATION-007` §Removal scope (verified deletion list — all items out of scope for this story)
- `CNTR-MODERNIZATION-006` §Interface: SyncStateRepository (binding signatures)
- `CNTR-MODERNIZATION-006` §Type: SyncEvent (binding variant set)
- `CNTR-MODERNIZATION-006` §Validation Rules (sticky/one-shot invariants, no silent failure, Cancel.Origin)
- `CNTR-MODERNIZATION-006` §Versioning (breaking-change policy; adding a variant is source-breaking for exhaustive `when`)

## Integration Contract References

- `CNTR-MODERNIZATION-006` §Interface: SyncStateRepository — five member signatures (`state`, `events`, `emitState`, `emitEvent`, `tryEmitEvent`) are fixed by this contract and MUST be implemented verbatim. Deviation is a contract violation.
- `CNTR-MODERNIZATION-006` §Type: SyncEvent — eleven variant signatures are fixed. The `Cancel.Origin` enum and `mayInterruptIfRunning()` method are load-bearing and MUST be preserved verbatim.
- `CNTR-MODERNIZATION-006` §Backing-field requirements — `state` backed by `MutableStateFlow`, `events` backed by `MutableSharedFlow(replay=0, extraBufferCapacity>=1, onBufferOverflow=SUSPEND)`. The facade step initializes these as stubs; they must be configured with the correct parameters so Step 2 can activate them without changing the field declarations.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Facade step only (DES-MODERNIZATION-007 Step 1): define `SyncStateRepository` interface + `SyncEvent` sealed class + `DefaultSyncStateRepository` delegating to `App.bus`; zero runtime behavior change; Otto remains on classpath; one proof-of-life call site migrated at `SmsBackupService.java:194`; manual singleton on `App`; Hilt deferred to MU-007.
