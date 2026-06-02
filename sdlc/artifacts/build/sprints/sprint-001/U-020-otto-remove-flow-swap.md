---
type: story
status: planned
sprint: "000001"
artifact_type: user-story
priority: high
complexity: high
parallel_eligible: false
iteration: 3
requirements:
  - REQ-MODERNIZATION-007
design_docs:
  - DES-MODERNIZATION-007
integration_contracts:
  - CNTR-MODERNIZATION-006
dependencies:
  - U-019
  - U-015
change_records: []
platforms: []
tags:
  - otto
  - stateflow
  - sharedflow
  - bus-removal
  - viewmodel
  - modernization
gate_additions: []
id: U-020
title: 'Otto removal: swap Otto-delegating SyncStateRepository facade to MutableStateFlow/MutableSharedFlow, migrate all 28 handler sites across 12 files, delete App.bus + static service fields, remove otto dependency'
pipeline: ''
domain: modernization
requirement_source: authored
---

# U-020: Otto removal — swap Otto-delegating SyncStateRepository facade to MutableStateFlow/MutableSharedFlow, migrate all 28 handler sites across 12 files, delete App.bus + static service fields, remove otto dependency

## Story

As a maintainer of SMS Backup+,
I want to replace the Otto-delegating `DefaultSyncStateRepository` (introduced by U-019) with a `MutableStateFlow`/`MutableSharedFlow(replay=0)` backed implementation, migrate every `@Subscribe`/`@Produce` handler across all 12 Otto-importing files to emit or collect from the repository, delete `App.bus` plus the static `register`/`unregister`/`post` helpers, delete the two `private static` service back-references, remove the `ServiceBase` bus lifecycle calls, absorb/delete all Otto event POJOs into `SyncEvent` variants, and remove `com.squareup:otto` from `app/build.gradle`,
so that the abandoned Square Otto bus is entirely unreachable at compile time, the hidden bidirectional coupling between the engine and UI is eliminated, sticky last-state semantics are preserved via `StateFlow.value`, `MainActivity` is decomposed behind a `MainViewModel` that collects state lifecycle-aware, and the silent `IllegalArgumentException` swallow in `App.register()` becomes a compile-time impossibility.

## Acceptance Criteria

- [ ] AC-1: **[Otto dependency removed at compile time]** Given `com.squareup:otto:1.3.8` is listed at `app/build.gradle:57`, when this story is complete, then that line is deleted and the project assembles successfully without otto on the compile or runtime classpath. Verified by: (a) `grep -rn "com.squareup.otto\|com.squareup:otto" app/src/main/java app/build.gradle` returns zero matches across all source files and the build file; (b) `./gradlew :app:assembleDebug` exits with code 0 and produces an APK; (c) `./gradlew :app:dependencies --configuration debugCompileClasspath | grep -i otto` returns zero lines.

- [ ] AC-2: **[All 28 @Subscribe/@Produce sites migrated across all 12 distinct importing files]** Given the 28 `@Subscribe`/`@Produce` annotated methods grep-confirmed across the 12 files listed in DES-MODERNIZATION-007 §Context (`App.java`, `activity/Dialogs.java`, `activity/MainActivity.java`, `activity/StatusPreference.java`, `activity/auth/OAuth2WebAuthActivity.java`, `activity/fragments/AdvancedSettings.java`, `activity/fragments/MainSettings.java`, `service/BackupTask.java`, `service/RestoreTask.java`, `service/SmsBackupService.java`, `service/SmsJobService.java`, `service/SmsRestoreService.java`), when this story is complete, then: (a) `grep -rn "@Subscribe\|@Produce" app/src/main/java` returns zero matches; (b) `grep -rn "import com.squareup.otto" app/src/main/java` returns zero matches across all 12 distinct files (keyed on 12 distinct files, not import-line count — one un-migrated file is a failure); (c) every former emitter calls `repository.emitState(state)` or `repository.tryEmitEvent(event)` in its place; (d) every former subscriber collects from `repository.state` or `repository.events` inside a lifecycle-aware scope.

- [ ] AC-3: **[MutableStateFlow/MutableSharedFlow backed implementation replaces Otto-delegating facade]** Given U-019 introduced a `DefaultSyncStateRepository` that delegates `emitState`/`emitEvent` to `App.bus` and routes `@Subscribe`/`@Produce` to match the Otto contract, when this story swaps the implementation, then the live `DefaultSyncStateRepository` (or a renamed `FlowSyncStateRepository`) is backed by: (a) a `MutableStateFlow<SyncState>` initialised with the idle-equivalent state (`SmsSyncState.IDLE`), exposed as `val state: StateFlow<SyncState>` via `.asStateFlow()`; (b) a `MutableSharedFlow<SyncEvent>` constructed with `replay = 0` and `extraBufferCapacity >= 1`, exposed as `val events: SharedFlow<SyncEvent>` via `.asSharedFlow()`; (c) `emitState(newState)` calls `_state.value = newState` on the main or appropriate dispatcher; (d) `suspend fun emitEvent(event: SyncEvent)` calls `_events.emit(event)` (suspending, back-pressure safe); (e) `fun tryEmitEvent(event: SyncEvent): Boolean` calls `_events.tryEmit(event)` and returns the result to the caller without swallowing it. The Otto-delegating implementation is deleted.

- [ ] AC-4: **[Sticky semantics preserved — StateFlow.value reproduces @Produce late-subscriber contract]** Given `SmsBackupService.produceLastState()` (`:202-204`) and `SmsRestoreService.produceLastState()` (`:157-159`) currently provide sticky last-state to late Otto subscribers via `@Produce`, when a new collector subscribes to `repository.state` after one or more `emitState` calls have already occurred, then the collector immediately receives the most recently emitted `SyncState` value without any additional emit — exactly as `@Produce` delivered to late-registering subscribers. Verified by a unit test: emit `BackupState(SmsSyncState.RUNNING, ...)` to the repository; collect `repository.state.first()` from a new coroutine started after the emit; assert the collected value equals the emitted `BackupState` without any intervening emission.

- [ ] AC-5: **[One-shot events are not replayed to late collectors]** Given `SharedFlow` is constructed with `replay = 0`, when a `SyncEvent` (e.g. `SyncEvent.MissingPermissions(...)`) is emitted before a new collector subscribes, then the late collector does NOT receive that event. Verified by a unit test: emit a `SyncEvent.MissingPermissions(...)` to the repository; start a new coroutine collecting `repository.events`; assert the collector receives no event within a 100 ms timeout. This must pass for every `SyncEvent` variant, particularly `MissingPermissions` (where re-delivering on rotation is a UX defect).

- [ ] AC-6: **[App.bus + static register/unregister/post deleted; silent swallow eliminated]** Given `App.java:59` declares `private static final Bus bus = new Bus()` and `:116-134` provide static `register(Object)`, `unregister(Object)`, and `post(Object)` helpers that catch `IllegalArgumentException` and log it silently, when this story is complete, then: (a) all three of those static methods are deleted from `App.java`; (b) `App.bus` field declaration at `:59` is deleted; (c) `grep -n "App\.bus\|App\.register\|App\.unregister\|App\.post\|IllegalArgumentException" app/src/main/java/com/zegoggles/smssync/App.java` returns zero matches (for the bus-related lines specifically — the static helpers are gone and the catch block that swallowed the exception no longer exists); (d) `App.onCreate` no longer calls `App.register(this)` (`:105`); the `@Subscribe autoBackupSettingsChanged` handler that previously lived on `App` is migrated to collect from `repository.events` in a non-static, Application-scoped coroutine scope.

- [ ] AC-7: **[ServiceBase bus lifecycle deleted]** Given `ServiceBase.java:76` calls `App.register(this)` in `onCreate` and `:83` calls `App.unregister(this)` in `onDestroy`, when this story is complete, then both call sites are deleted from `ServiceBase`. `grep -n "App\.register\|App\.unregister" app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java` returns zero matches.

- [ ] AC-8: **[SmsBackupService.service static + isServiceWorking() deleted]** Given `SmsBackupService.java:66` declares `private static SmsBackupService service` (set in `onCreate`, nulled in `onDestroy`) and `:311-313` expose `static boolean isServiceWorking()` by reading that field, when this story is complete, then: (a) the `service` static field declaration at `:66` is deleted; (b) the `isServiceWorking()` static method at `:311-313` is deleted; (c) the cross-service call `SmsRestoreService.isServiceIdle()` at `SmsBackupService.java:101` is deleted (the question is now answered by reading `repository.state.value`); (d) `grep -n "isServiceWorking\|static SmsBackupService service" app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java` returns zero matches.

- [ ] AC-9: **[SmsRestoreService.service static + isServiceIdle() deleted]** Given `SmsRestoreService.java:40` declares `private static SmsRestoreService service` and `:172-174` expose `static boolean isServiceIdle()`, when this story is complete, then: (a) both are deleted; (b) all callers that previously called `SmsRestoreService.isServiceIdle()` are updated to read `repository.state.value` (or `repository.state.value.isRunning()` per DES-MODERNIZATION-007 §System Architecture); (c) `grep -n "isServiceIdle\|static SmsRestoreService service" app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java` returns zero matches.

- [ ] AC-10: **[SyncEvent.Cancel preserves CancelEvent.Origin USER/SYSTEM distinction and mayInterruptIfRunning()]** Given `service/CancelEvent.java` declares an `Origin` enum with values `USER` and `SYSTEM` (default constructor = `USER`) and `mayInterruptIfRunning()` returns `origin == SYSTEM` (`:5-19`), when this story absorbs `CancelEvent` into `SyncEvent.Cancel`, then: (a) `SyncEvent.Cancel` carries an `origin: CancelEvent.Origin` (or an equivalent inline `Origin` enum if `CancelEvent.java` is deleted) with values `USER` and `SYSTEM`; (b) a `mayInterruptIfRunning()` method (or equivalent property) returns `true` when `origin == SYSTEM` and `false` when `origin == USER`, preserving the exact semantics; (c) all call sites that previously posted `new CancelEvent()` (default: USER) and `new CancelEvent(Origin.SYSTEM)` are migrated to `SyncEvent.Cancel(Origin.USER)` and `SyncEvent.Cancel(Origin.SYSTEM)` respectively; (d) all `@Subscribe` handlers that previously received `CancelEvent` and branched on `mayInterruptIfRunning()` are migrated to collect `SyncEvent.Cancel` from `repository.events` and branch on the same condition; (e) `CancelEvent.java` is deleted only after grep confirms zero remaining `CancelEvent` references in production source.

- [ ] AC-11: **[SyncEvent.PerformAction preserves Actions enum and confirm flag]** Given `activity/events/PerformAction.java` carries a three-value `Actions` enum (`Backup`, `BackupSkip`, `Restore`) and a `boolean confirm` field (`:3-17`), where `confirm` gates the confirmation dialog in `Dialogs.performAction` (`:331-343`), when this story absorbs `PerformAction` into `SyncEvent.PerformAction`, then: (a) `SyncEvent.PerformAction` carries `action: Actions` and `confirm: Boolean` preserving both fields verbatim; (b) `Dialogs.performAction` continues to branch on `confirm` with identical logic; (c) `PerformAction.java` is deleted only after grep confirms zero remaining `PerformAction` references in production source.

- [ ] AC-12: **[SyncEvent.MissingPermissions preserves List<AppPermission> payload]** Given `activity/events/MissingPermissionsEvent.java` carries `List<AppPermission> permissions` (`:7-12`), when this story absorbs it into `SyncEvent.MissingPermissions`, then: (a) the payload type is `List<AppPermission>` (not `List<String>`); (b) the event is one-shot (non-sticky, confirmed by AC-5); (c) `MissingPermissionsEvent.java` is deleted after grep-verified consumer migration.

- [ ] AC-13: **[Remaining activity/events/* POJOs absorbed or deleted after grep-verified migration]** Given the `activity/events/` directory contains additional POJOs (`AccountAddedEvent`, `AccountRemovedEvent`, `AccountConnectionChangedEvent`, `AutoBackupSettingsChangedEvent`, `FallbackAuthEvent`, `SettingsResetEvent`, `ThemeChangedEvent`) and `OAuth2CallbackTask.OAuth2CallbackEvent`, when each is migrated, then: (a) for each POJO, the implementer greps both `@Subscribe` consumers and `App.post(new <EventClass>` producers and confirms each has a migrated counterpart in `SyncEvent.*` before deleting the POJO; (b) any POJO for which grep confirms zero `@Subscribe` consumers AND zero `App.post(new <EventClass>` producers is documented in Implementation Notes before deletion (as DES-MODERNIZATION-007 §Design Decision flags — 4 of 9 POJOs may have no static grep evidence); (c) after migration, `grep -rn "activity/events\|import.*events\." app/src/main/java` returns zero matches; (d) the `activity/events/` directory no longer exists in the source tree.

- [ ] AC-14: **[MainActivity decomposed behind MainViewModel with repeatOnLifecycle collection]** Given `MainActivity.java` (499 LOC) carries nine `@Subscribe` handlers (verified at `:265,:271,:280,:289,:298,:306,:331,:345`) and registers via `App.register(this)` at `onStart` (`:155`) / `App.unregister(this)` at `onStop` (`:160`), and posts `AccountAddedEvent` via `App.post` at `:283` and `:451`, when this story is complete, then: (a) a new `MainViewModel` (`@HiltViewModel` if Hilt is present per MU-007, or manual-DI singleton if not) holds an injected `SyncStateRepository` and exposes `state` and `events` flows; (b) `MainActivity` replaces the `onStart`/`onStop` `App.register`/`App.unregister` pair with a single `lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { ... } }` block that collects from `viewModel.state` and `viewModel.events`; (c) dialog-related event handling (`onOAuth2Callback` `:280`, `onConnect` `:289`, `handleFallbackAuth` `:298`, `themeChangedEvent` `:306`, `performAction` `:331`, `doPerform` `:345`) is relocated to `activity/Dialogs.java` and driven off collected flows; (d) `App.post(AccountAddedEvent)` at `:283` and `:451` is replaced by `repository.tryEmitEvent(SyncEvent.AccountAdded(...))` or equivalent; (e) `grep -n "@Subscribe\|App\.register\|App\.unregister\|App\.post" app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` returns zero matches after migration.

- [ ] AC-15: **[StatusPreference migrated to lifecycle-aware Flow collection]** Given `StatusPreference.java` currently: calls `App.register(this)` in `onBindViewHolder` (`:127`), `App.unregister(this)` in `onDetached` (`:97`), carries `@Subscribe` handlers `restoreStateChanged` (`:142`), `backupStateChanged` (`:174`), `onMissingPermissions` (`:202`), and posts `SyncEvent.PerformAction`/`SyncEvent.Cancel` on button clicks (`:209,:215,:222,:226`), when this story is complete, then: (a) `App.register`/`App.unregister` calls are deleted; (b) state and event collection uses a `CoroutineScope` scoped to the preference's attached lifecycle (cancelled in `onDetached`); (c) button-click handlers call `repository.tryEmitEvent(SyncEvent.PerformAction(...))` and `repository.tryEmitEvent(SyncEvent.Cancel(Origin.USER))` respectively; (d) `grep -n "@Subscribe\|App\.register\|App\.unregister\|App\.post" app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java` returns zero matches.

- [ ] AC-16: **[Repository unit tests — sticky replay, one-shot non-replay, Cancel Origin preserved]** Given the `FlowSyncStateRepository` is the live implementation, when unit tests run (`./gradlew :app:test` or `:core:test`), then all of the following new tests pass: (a) **sticky-replay test**: emit a `BackupState(SmsSyncState.RUNNING, ...)`, then collect `repository.state.first()` from a new coroutine; assert the value equals the emitted state (no additional emit needed); (b) **one-shot non-replay test**: emit a `SyncEvent.MissingPermissions(...)`, then collect `repository.events` from a new coroutine with a 100 ms timeout; assert no event is received; (c) **Cancel Origin USER test**: collect `repository.events` in a coroutine, emit `SyncEvent.Cancel(Origin.USER)`, assert collected value has `mayInterruptIfRunning() == false`; (d) **Cancel Origin SYSTEM test**: emit `SyncEvent.Cancel(Origin.SYSTEM)`, assert `mayInterruptIfRunning() == true`; (e) **tryEmitEvent return value test**: call `tryEmitEvent` with no active collector; assert the return value is consumed by the caller (not silently swallowed); (f) all tests pass with zero emulator dependency.

- [ ] AC-17: **[Non-coroutine call sites (receivers/App.onCreate) use tryEmitEvent; result is not swallowed]** Given `BackupBroadcastReceiver` / `SmsBroadcastReceiver` paths and `App.onCreate` post events outside a coroutine scope, when migrated, then: (a) these sites call `repository.tryEmitEvent(event)` (non-suspending); (b) the `Boolean` return value of `tryEmitEvent` is checked or logged — it is NOT silently discarded (to prevent the silent-swallow defect AC-6 eliminates at the `register` layer from re-emerging at the `emit` layer); (c) the `MutableSharedFlow` is constructed with `extraBufferCapacity >= 1` so a single synchronous emit from a receiver does not return `false` under normal conditions.

- [ ] AC-18: **[Hilt injection deferred — repository held as manual singleton until MU-007]** Given DES-MODERNIZATION-008 (Hilt, MU-007) is not yet approved and MU-007 is scheduled after this unit, when the `FlowSyncStateRepository` is introduced, then: (a) it is constructed and held as a manual application-scoped singleton (consistent with the existing dual-constructor manual-DI discipline in `App.java`), NOT wired via `@Singleton`/`@Provides` or any Hilt annotation; (b) constructor injection points for `MainViewModel`, `StatusPreference`, and the workers carry a `// TODO U-022/MU-007: replace manual singleton with Hilt @Singleton` comment marking the retrofit site; (c) no `@HiltAndroidApp`, `@HiltViewModel`, `@HiltWorker`, or `@Inject` annotation is introduced by this story (those belong to U-022 / MU-007).

### Integration Criteria

- [ ] IC-1: The `FlowSyncStateRepository` singleton instance is constructed in `App.onCreate()` and accessible via a stable reference (e.g. a static `App.getRepository()` accessor or an injected field) to all consumers (`MainViewModel`, `StatusPreference`, `Dialogs`, `BackupWorker`, `RestoreWorker`, `App` settings collector) — confirm the accessor exists and is non-null at the point each consumer first collects from it.
- [ ] IC-2: `MainViewModel` is reachable from `MainActivity` via `ViewModelProvider` (or equivalent manual-DI pattern consistent with IC-1 above): `val viewModel by viewModels<MainViewModel>()` compiles and returns a non-null instance. Verify with an instrumented or Robolectric test that creates `MainActivity` and asserts `viewModel.state.value` is not null.
- [ ] IC-3: Every call site that previously called `App.post(state)` in `SmsBackupService` and `SmsRestoreService` (as the engine-side emit) is replaced by `repository.emitState(state)`. After this story, `grep -rn "App\.post" app/src/main/java/com/zegoggles/smssync/service/` returns zero matches (scoped to the service package — the bus-post surface is fully removed from the engine layer).

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/build.gradle` | `implementation 'com.squareup:otto:1.3.8'` at line 57 | Line deleted; otto unreachable at compile time |
| `app/src/main/java/com/zegoggles/smssync/App.java` | `private static final Bus bus = new Bus()` at `:59`; static `register` (`:116-122`), `unregister` (`:123-130`), `post` (`:132-134`); `register(this)` at `onCreate` `:105`; `@Subscribe autoBackupSettingsChanged` at `:108` | Bus field and three static helpers deleted; `autoBackupSettingsChanged` migrated to collect `SyncEvent` from repository in an Application-scoped coroutine |
| `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java` | `App.register(this)` at `:76`; `App.unregister(this)` at `:83` | Both call sites deleted |
| `app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java` | `private static SmsBackupService service` at `:66`; `isServiceWorking()` at `:311-313`; `SmsRestoreService.isServiceIdle()` at `:101`; `@Produce produceLastState()` at `:202-204`; `App.post(state)` at `:194`; two `com.squareup.otto.*` imports | Static field + query method deleted; cross-service call deleted; `@Produce` deleted; `App.post` replaced by `repository.emitState`; imports removed |
| `app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java` | `private static SmsRestoreService service` at `:40`; `isServiceIdle()` at `:172-174`; `@Produce produceLastState()` at `:157-159`; `App.post(state)` at `:111`; two `com.squareup.otto.*` imports | Static field + query method deleted; `@Produce` deleted; `App.post` replaced by `repository.emitState`; imports removed |
| `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` | 499 LOC; `App.register(this)` at `onStart` `:155`; `App.unregister(this)` at `onStop` `:160`; nine `@Subscribe` handlers at `:265,:271,:280,:289,:298,:306,:331,:345`; `App.post(AccountAddedEvent)` at `:283,:451` | Register/unregister pair replaced by `repeatOnLifecycle(STARTED)` collection; nine handlers migrated to `when` branches inside collect lambdas; dialog-related handlers relocated to `Dialogs.java`; `App.post` calls replaced by `repository.tryEmitEvent` |
| `app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java` | `App.register(this)` at `onBindViewHolder` `:127`; `App.unregister` at `onDetached` `:97`; three `@Subscribe` handlers (`:142,:174,:202`); button-click `App.post` calls at `:209,:215,:222,:226` | Register/unregister replaced by scoped coroutine collection; handlers migrated; `App.post` replaced by `repository.tryEmitEvent` |
| `app/src/main/java/com/zegoggles/smssync/activity/Dialogs.java` | Receives dialog-related `@Subscribe` handlers currently physically resident on `MainActivity`; one Otto import | Dialog-handling logic migrated here from `MainActivity`; collects from `repository.events`; Otto import removed |
| `app/src/main/java/com/zegoggles/smssync/activity/auth/OAuth2WebAuthActivity.java` | One `com.squareup.otto.*` import; Otto usage | Migrated to repository collect; Otto import removed |
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/AdvancedSettings.java` | One `com.squareup.otto.*` import; Otto usage | Migrated to repository collect/emit; Otto import removed |
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/MainSettings.java` | One `com.squareup.otto.*` import; Otto usage | Migrated to repository collect/emit; Otto import removed |
| `app/src/main/java/com/zegoggles/smssync/service/BackupTask.java` | Otto imports; `@Subscribe` handler(s); `App.register`/`App.post`/`App.unregister` | Fully migrated or already handled by U-015 (CoroutineWorker rewrite); Otto references removed; file deleted if U-015 has landed |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreTask.java` | Otto imports; `@Subscribe` handler(s); `App.register`/`App.post`/`App.unregister` | Fully migrated or already handled by U-015; Otto references removed; file deleted if U-015 has landed |
| `app/src/main/java/com/zegoggles/smssync/service/SmsJobService.java` | One `com.squareup.otto.*` import; `@Subscribe backupStateChanged`; `App.post(new CancelEvent())` at `onStopJob` | Deleted by U-015; if not yet landed, Otto import and `@Subscribe` migrated here before deletion |
| `app/src/main/java/com/zegoggles/smssync/service/CancelEvent.java` | `Origin` enum `{USER, SYSTEM}`; `mayInterruptIfRunning()` returns `origin == SYSTEM` | Absorbed into `SyncEvent.Cancel(origin: Origin)`; file deleted after grep-verified migration |
| `app/src/main/java/com/zegoggles/smssync/activity/events/PerformAction.java` | `Actions` enum + `boolean confirm` | Absorbed into `SyncEvent.PerformAction`; file deleted after grep-verified migration |
| `app/src/main/java/com/zegoggles/smssync/activity/events/MissingPermissionsEvent.java` | `List<AppPermission> permissions` | Absorbed into `SyncEvent.MissingPermissions`; file deleted after grep-verified migration |
| `app/src/main/java/com/zegoggles/smssync/activity/events/` (remaining 7 POJOs) | Various single-field Otto event POJOs | Each grep-verified (consumer + producer) before absorption into `SyncEvent.*` variant or deletion; directory removed when empty |
| `app/src/main/java/com/zegoggles/smssync/service/` — new `SyncStateRepository.kt` | Does not exist (introduced by U-019 facade) | Updated: `FlowSyncStateRepository` replaces Otto-delegating impl; `MutableStateFlow`+`MutableSharedFlow(replay=0)` backed |
| `app/src/main/java/com/zegoggles/smssync/activity/` — new `MainViewModel.kt` | Does not exist | Created: `ViewModel` holding injected `SyncStateRepository`, exposing `state`/`events` flows |

## Existing Behavior to Preserve

- The sticky last-state contract: a UI component binding after a backup has already started must immediately receive the current `BackupState` (or `RestoreState`) without waiting for the next emission. `StateFlow.value` reproduces this exactly (as `@Produce produceLastState()` did).
- `CancelEvent.Origin.USER` vs `Origin.SYSTEM` distinction: `mayInterruptIfRunning()` returning `false` for USER and `true` for SYSTEM drives whether an in-progress backup task is interrupted or finishes its current message. This branch must be identical after migration — no boolean-flattening of the enum is acceptable.
- `PerformAction.confirm` boolean: the confirmation dialog branch in `Dialogs.performAction` (`:331-343`) is gated on this field. The field must survive as a property of `SyncEvent.PerformAction`.
- `MissingPermissionsEvent.permissions` is `List<AppPermission>` (not `List<String>`): the permission-request flow uses the typed enum. Widening to `List<String>` would break the request flow.
- The public `com.zegoggles.smssync.BACKUP` broadcast contract is owned by MU-005 and must not be altered. The `tryEmitEvent` call that replaces `App.post` in the broadcast receiver path must not change the broadcast's external semantics.
- `StatusPreference`'s `onSaveInstanceState`/`onRestoreInstanceState` rotation-state behavior introduced by U-018 must be preserved unmodified.
- `State`/`BackupState`/`RestoreState`/`SmsSyncState` payload behavior (the Preserved-Core invariant, MU-000): this story is a pure eventing-transport swap. No field, constructor, transition method, or enum constant of these classes may be modified. The Kotlin `sealed interface SyncState` promotion is explicitly deferred to Phase 3 / MU-012.
- `State.java:32-33` k-9 magic-string `"Unable to get IMAP prefix"` handling is owned by MU-008 — do not touch.
- Threading safety of state payloads: `StateFlow`/`SharedFlow` are concurrency-safe; the immutable `State` hierarchy ensures no torn reads. Do not introduce mutable state into the payload.

## Verification Steps

1. **AC-1 (otto removed from build):** Run `./gradlew :app:assembleDebug`. Must succeed. Then run `grep -rn "com.squareup.otto\|com.squareup:otto" app/src/main/java app/build.gradle`. Must return zero matches. Then run `./gradlew :app:dependencies --configuration debugCompileClasspath | grep -i otto`. Must return zero lines. If any of these fail, otto is still reachable.

2. **AC-2 (all 28 sites migrated, 12 files clean):** Run `grep -rn "@Subscribe\|@Produce\|import com.squareup.otto" app/src/main/java`. Must return zero matches. Confirm independently that the 12 named files have zero `com.squareup.otto` imports by running the grep scoped to each file path. One remaining file is a failure.

3. **AC-3 (Flow-backed implementation):** Open `FlowSyncStateRepository.kt` (or equivalent). Confirm `MutableStateFlow` and `MutableSharedFlow(replay = 0, extraBufferCapacity = ...)` declarations are present. Confirm no `Bus`, `App.post`, `App.register`, or `@Subscribe` reference. Confirm the Otto-delegating implementation class from U-019 no longer exists in the source tree.

4. **AC-4 (sticky semantics — unit test):** Run `./gradlew :app:test --tests "*SyncStateRepositoryTest.lateCollector_receivesLastEmittedState"`. Must pass. The test emits a `BackupState`, creates a new coroutine, collects `repository.state.first()`, and asserts equality.

5. **AC-5 (one-shot non-replay — unit test):** Run `./gradlew :app:test --tests "*SyncStateRepositoryTest.lateCollector_doesNotReceivePriorEvent"`. Must pass. The test emits a `SyncEvent.MissingPermissions`, starts a new collector with a 100 ms `withTimeoutOrNull`, and asserts null (no event received).

6. **AC-6 (App.bus and static helpers deleted):** Run `grep -n "static.*Bus\|App\.bus\|App\.register\|App\.unregister\|App\.post\|catch.*IllegalArgumentException" app/src/main/java/com/zegoggles/smssync/App.java`. Must return zero matches on the bus-related patterns.

7. **AC-7 (ServiceBase bus lifecycle deleted):** Run `grep -n "App\.register\|App\.unregister" app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java`. Must return zero matches.

8. **AC-8 (SmsBackupService static service + isServiceWorking deleted):** Run `grep -n "isServiceWorking\|static SmsBackupService service\|isServiceIdle\|SmsRestoreService\.isServiceIdle" app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java`. Must return zero matches.

9. **AC-9 (SmsRestoreService static service + isServiceIdle deleted):** Run `grep -n "isServiceIdle\|static SmsRestoreService service" app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java`. Must return zero matches.

10. **AC-10 (CancelEvent.Origin preserved end-to-end):** Run `./gradlew :app:test --tests "*SyncStateRepositoryTest.cancel_originUser_mayInterruptIfRunning_isFalse"` and `"*SyncStateRepositoryTest.cancel_originSystem_mayInterruptIfRunning_isTrue"`. Both must pass. Also confirm `CancelEvent.java` no longer exists: check the file path at `app/src/main/java/com/zegoggles/smssync/service/CancelEvent.java` — must be absent or the build references it nowhere.

11. **AC-13 (activity/events/ directory removed):** Run `ls app/src/main/java/com/zegoggles/smssync/activity/events/` (or equivalent `Glob`). Must return "no such directory" or empty. Then run `grep -rn "import.*activity\.events\." app/src/main/java`. Must return zero matches.

12. **AC-14 (MainActivity migrated — zero Otto surface):** Run `grep -n "@Subscribe\|App\.register\|App\.unregister\|App\.post" app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java`. Must return zero matches. Also confirm `MainViewModel.kt` exists in `app/src/main/java/com/zegoggles/smssync/activity/` and `lifecycleScope.launch` + `repeatOnLifecycle` appear in `MainActivity`.

13. **AC-16 (repository unit tests):** Run `./gradlew :app:test --tests "*SyncStateRepositoryTest"`. All six test methods (sticky-replay, one-shot non-replay, Cancel USER, Cancel SYSTEM, tryEmit return value) must pass with exit code 0. No emulator required.

14. **Full build smoke after all changes:** Run `./gradlew :app:assembleDebug` and `./gradlew :app:test`. Both must succeed. The `warningsAsErrors true` + `-Werror -Xlint:deprecation` build flags (`app/build.gradle:41,73-77`) must produce zero new deprecation warnings (removing Otto is expected to reduce the deprecation count, not increase it).

15. **UI smoke (AC-5 / no status-row regression):** Install the debug build on an API 29 device or emulator. Start a backup. Rotate the device while backup is in progress. Assert the status row in `StatusPreference` continues to display the in-progress state and does not flash to idle. This verifies both the sticky `StateFlow` semantics and the U-018 rotation-state preservation.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android / Kotlin | `FlowSyncStateRepository.kt` — `MutableStateFlow`+`MutableSharedFlow` impl; `SyncEvent` sealed class with all variants; `SyncStateRepository` interface finalization; deletion of Otto-delegating impl | Developer |
| Android / Kotlin | `MainViewModel.kt` — new `ViewModel`; `MainActivity.java` migration (register/unregister → `repeatOnLifecycle`; nine `@Subscribe` handlers → collect branches; `App.post` → `tryEmitEvent`) | Developer |
| Android / Java | `App.java` — delete `bus`/`register`/`unregister`/`post`; migrate `autoBackupSettingsChanged` to Application-scoped coroutine; `ServiceBase.java` — delete two bus lifecycle calls | Developer |
| Android / Java | `SmsBackupService.java`, `SmsRestoreService.java` — delete static service fields + query methods + cross-service call; replace `App.post(state)` with `repository.emitState(state)`; delete `@Produce` methods | Developer |
| Android / Java + Kotlin | `StatusPreference.java`, `Dialogs.java`, `OAuth2WebAuthActivity.java`, `AdvancedSettings.java`, `MainSettings.java` — migrate `@Subscribe` handlers to Flow collection; remove `App.register`/`App.unregister`; replace `App.post` with `repository.tryEmitEvent` | Developer |
| Android / Java | `service/CancelEvent.java`, `activity/events/*.java` — grep-verify each POJO's consumer and producer sites, absorb into `SyncEvent.*`, delete files | Developer |
| Android / Kotlin (unit tests) | `SyncStateRepositoryTest.kt` — six repository unit tests (sticky-replay, one-shot non-replay, Cancel USER/SYSTEM, tryEmit return value); must run JVM-only | Developer |
| `app/build.gradle` | Delete `implementation 'com.squareup:otto:1.3.8'` at line 57 | Developer |

## Technical Notes

**Dependency sequencing — must land after U-019 and U-015.** U-019 introduces the `SyncStateRepository` interface and the Otto-delegating `DefaultSyncStateRepository`. This story swaps that implementation; the interface must already exist and be injected at all sites before this story starts (U-019 is the Step-1 Facade; this story is Step-2 Swap + Step-3 Delete). U-015 converts `BackupTask`/`RestoreTask` to `CoroutineWorker` — once landed, the engine-side `emitState` calls are idiomatic coroutine emits inside `doWork()`. If U-015 has not landed when this story is implemented, `BackupTask`/`RestoreTask` still use `AsyncTask` and their Otto registration/deregistration must be migrated here too (replace `App.register(this)` / `App.post(state)` / `App.unregister(this)` in `BackupTask` and `RestoreTask` with `repository.emitState(state)` direct calls). The IC-3 grep gate checks the service package; the implementer must extend it to `BackupTask`/`RestoreTask` if those files still exist.

**`MutableSharedFlow` constructor parameters — back-pressure contract.** Construct `MutableSharedFlow` as `MutableSharedFlow<SyncEvent>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)`. `replay = 0` enforces the one-shot non-sticky contract (AC-5). `extraBufferCapacity = 1` ensures a `tryEmit` from a broadcast receiver or non-coroutine call site (AC-17) succeeds under normal conditions (no active collector fast enough to drain the buffer). `DROP_OLDEST` is the safe choice for UI events: dropping a stale "permissions missing" event is preferable to a deadlocked emit. The `extraBufferCapacity` must be `>= 1` — using `0` with `tryEmit` would make it lossy under any concurrent emit, breaking AC-17.

**`MutableStateFlow` threading.** `_state.value = newState` is thread-safe on `MutableStateFlow` — multiple engine threads may call `emitState` concurrently without a lock. The immutable `State` payload ensures no torn reads at the consumer. `emitState` must NOT be `suspend fun` — services and workers call it from their execution context and a suspending call would require a coroutine scope at the call site. The existing emission cadence (once per state transition, not per message) is preserved.

**`suspend fun emitEvent` vs `tryEmitEvent` call-site allocation.** With `extraBufferCapacity = 1`: (a) callers inside a coroutine scope (e.g., `MainViewModel`, `BackupWorker.doWork`) use `suspend fun emitEvent` for back-pressure safety; (b) callers outside a coroutine scope (broadcast receivers, button-click listeners, `App.onCreate` non-coroutine paths) use `tryEmitEvent` and MUST check or log the return value per AC-17. The implementer must audit every `App.post(event)` call site and assign it to the correct surface.

**POJO grep verification discipline before deletion.** DES-MODERNIZATION-007 §Design Decision flags that four of nine `activity/events/*` POJOs may have no static `@Subscribe` grep evidence. Before deleting any POJO: run `grep -rn "new <EventClass>\|<EventClass>(" app/src/main/java` (producer sites) and `grep -rn "@Subscribe.*<EventClass>\|<EventClass> " app/src/main/java` (consumer sites). If both return zero, the POJO was dead code — document in Implementation Notes and delete. If producer sites exist but consumer sites do not, the events were being posted to a bus with no listener — also dead, document and delete. Only delete after this audit.

**`App.autoBackupSettingsChanged` migration scope.** `App.java:108` carries a `@Subscribe autoBackupSettingsChanged` handler that reacts to `AutoBackupSettingsChangedEvent`. After this story, `App.onCreate` must collect `SyncEvent.AutoBackupSettingsChanged` from the repository in an Application-scoped `CoroutineScope` (e.g. `ProcessLifecycleOwner.get().lifecycleScope` or a manually-managed `CoroutineScope(SupervisorJob() + Dispatchers.Main)`). The scope must be cancelled in `App.onTerminate()` or equivalent. The behavior of `autoBackupSettingsChanged` itself (what it does when it receives the event) must be preserved verbatim.

**`MainViewModel` injection — manual-DI pattern.** Because MU-007 (Hilt) has not landed, `MainViewModel` cannot use `@HiltViewModel`. Instead, provide it via a custom `ViewModelProvider.Factory` that takes the `SyncStateRepository` singleton from `App.getRepository()` (or equivalent accessor). `MainActivity` instantiates it via `ViewModelProvider(this, MainViewModelFactory(App.getRepository(this)))`. Mark all factory wiring with `// TODO U-022/MU-007: replace with @HiltViewModel`. The ViewModel must survive configuration changes — verified by the rotation smoke test in Verification Step 15.

**`warningsAsErrors` / `-Werror` build constraint.** `app/build.gradle:41,73-77` enforces `warningsAsErrors true` and `-Werror -Xlint:deprecation`. Removing the deprecated Otto usage reduces the deprecation count. The new Kotlin code (Flow, ViewModel, coroutines) must produce zero deprecation warnings — use AndroidX Lifecycle 2.6+ APIs (`repeatOnLifecycle`, `flowWithLifecycle`) which are stable, not deprecated. Do not use `GlobalScope` (a coroutine anti-pattern that would also generate a lint warning in strict builds).

**`SyncState` payload type — deferred promotion.** The `state: StateFlow<SyncState>` signature in `SyncStateRepository` uses `SyncState` as a type alias or marker interface for the existing `State`/`BackupState`/`RestoreState` hierarchy. The Kotlin `sealed interface SyncState` promotion (replacing the Java `State` class hierarchy with a proper sealed type) is explicitly out of scope here per DES-MODERNIZATION-007 §Overview. Do not introduce a new Kotlin sealed interface or change the existing Java classes. The `MutableStateFlow` is typed `MutableStateFlow<State>` (using the existing Java base class) for this story.

**CNTR-MODERNIZATION-006 status.** The task brief references `CNTR-MODERNIZATION-006` as the integration contract. At time of writing, the contract artifact file was not found in `sdlc/artifacts/contracts/` — this mirrors the DES-MODERNIZATION-007 §Integration Contracts note that CNTR-MOD-007-001 is "needed" (not yet approved). This story must not be sprint-planned until that contract is authored and reaches `approved` status. The implementer must confirm contract approval before starting.

## Supporting Documentation

- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-007-replace-otto-with-stateflow-repository.md` — governing requirement (AC-1 through AC-5, Constraints, boundary)
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-007-syncstate-repository-design.md` — full removal-scope table; sticky vs one-shot decision; MainViewModel decomposition; silent-swallow elimination; migration path (Step 2: swap; Step 3: delete); threading; back-pressure; Hilt deferral
- `sdlc/artifacts/stories/U-019-syncstate-repository-facade.md` — Step-1 Facade (predecessor); the `SyncStateRepository` interface and Otto-delegating impl this story replaces
- `sdlc/artifacts/stories/U-015-modernization-coroutineworker-backup-restore-rewrite.md` — CoroutineWorker rewrite (predecessor); `BackupTask`/`RestoreTask` deletion and Otto removal from the execution path

## Integration Contract References

- `CNTR-MODERNIZATION-006` — `SyncStateRepository` service/event contract: `state: StateFlow<SyncState>` and `events: SharedFlow<SyncEvent>` signatures; sticky vs one-shot semantics (replay/buffer config); `emitState` / `emitEvent` / `tryEmitEvent` publish surface and threading/back-pressure guarantees; full `SyncEvent` sealed-variant set including `Cancel.origin` USER/SYSTEM. **Must reach `approved` before this story is sprint-planned.** Referenced in AC-3, AC-10, IC-1.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Draft content authored against DES-MODERNIZATION-007 (verified live source line-numbers for App.java, SmsBackupService.java, SmsRestoreService.java, ServiceBase.java, MainActivity.java, StatusPreference.java, CancelEvent.java, app/build.gradle) and REQ-MODERNIZATION-007 ACs. NOT finalized. Load-bearing open item for review: CNTR-MODERNIZATION-006 does not yet exist as a file — sprint-planning gate requires it to reach `approved` before this story starts.
