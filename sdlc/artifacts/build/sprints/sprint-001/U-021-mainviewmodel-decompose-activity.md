---
type: story
status: done
sprint: '000001'
artifact_type: user-story
priority: high
complexity: high
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-007
design_docs:
  - DES-MODERNIZATION-007
integration_contracts:
  - CNTR-MODERNIZATION-006
dependencies:
  - U-020
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-021
title: 'Introduce MainViewModel and decompose MainActivity: migrate all 9 @Subscribe handlers to lifecycle-aware Flow collection'
pipeline: ''
domain: modernization
requirement_source: authored
updated_at: '2026-06-03T20:48:29.420Z'
resolution: done
---

# U-021: Introduce MainViewModel and decompose MainActivity — migrate all 9 @Subscribe handlers to lifecycle-aware Flow collection

## Story

As a maintainer of SMS Backup+,
I want `MainActivity` to hold a `MainViewModel` that collects `SyncState` and `SyncEvent` via `repeatOnLifecycle(STARTED)`, with all nine of its `@Subscribe` handlers relocated into the ViewModel and `Dialogs.java`, and `StatusPreference` migrated to the same lifecycle-aware collect pattern,
so that `MainActivity` contains no `@Subscribe` annotations, the God-Activity coupling to the Otto bus is fully dissolved for the presentation layer, and rotation no longer re-subscribes the activity to the bus.

## Acceptance Criteria

**AC-1 — `MainActivity` contains zero `@Subscribe` annotations after this story**

Given the implementation of this story is merged and compiles cleanly,
when a developer runs `grep -n "@Subscribe" app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` from the repository root,
then the command exits with status 0 and produces zero lines of output, confirming that none of the nine handlers (`restoreStateChanged` at `:265`, `backupStateChanged` at `:271`, `onOAuth2Callback` at `:280`, `onConnect` at `:289`, `handleFallbackAuth` at `:298`, `themeChangedEvent` at `:306`, `performAction` at `:331`, `doPerform` at `:345`, plus any additional `@Subscribe`-annotated method in the class at the time of implementation) remain in `MainActivity.java`.

**AC-2 — `MainActivity.onStart` / `onStop` `App.register` / `App.unregister` calls are removed and replaced with a single `repeatOnLifecycle` collection block**

Given the implementation is merged,
when a developer reads `MainActivity.java`:
- the `App.register(this)` call previously at `:155` inside `onStart` is absent;
- the `App.unregister(this)` call previously at `:160` inside `onStop` is absent;
- `MainActivity.onCreate` (or a dedicated setup method called from it) contains exactly one `lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { ... } }` block that launches at least two child coroutines — one collecting `viewModel.state` and one collecting `viewModel.events`; and
- no other `App.register` or `App.unregister` call targeting `this` (the Activity instance) exists anywhere in `MainActivity.java`.

**AC-3 — `MainViewModel` is introduced as a manual `ViewModel` with a `@HiltViewModel` placeholder comment, holds a `SyncStateRepository`, and exposes `state` and `events` flows**

Given the implementation is merged,
when a developer reads `app/src/main/java/com/zegoggles/smssync/activity/MainViewModel.kt` (or `.java`):
- the class extends `androidx.lifecycle.ViewModel`;
- it accepts a `SyncStateRepository` constructor parameter (manual DI; no Hilt injection wiring at this stage — a `// TODO @HiltViewModel` comment marks the deferred step consistent with the MU-007 sequencing constraint in DES-MODERNIZATION-007 §Hilt injection);
- it exposes `val state: StateFlow<SyncState>` delegating to `repository.state`;
- it exposes `val events: SharedFlow<SyncEvent>` delegating to `repository.events`; and
- it exposes any ViewModel-owned actions (`onConfigChanged()`, engine-trigger methods) needed by the `MainActivity` collect block and Dialogs delegates — each call site that formerly called `App.post(...)` from `MainActivity` (`:283`, `:451`) now calls a ViewModel method that calls `repository.tryEmitEvent` or `repository.emitEvent` appropriately.

**AC-4 — All nine former `MainActivity` `@Subscribe` handler behaviors are preserved by the `repeatOnLifecycle` collect block and/or `Dialogs.java`**

Given the collect block runs and a `SyncState` or `SyncEvent` is emitted by the repository,
then:
- **`restoreStateChanged` (`:265`):** when a `RestoreState` with `isFinished() == true` is the emitted state and `isSmsBackupDefaultSmsApp(this)` is true, `restoreDefaultSmsProvider(preferences.getSmsDefaultPackage())` is called — verified by a unit test on `MainViewModel` or by reading the collect-block handler;
- **`backupStateChanged` (`:271`):** when a `BackupState` with `backupType == MANUAL || SKIP` and `isPermissionException() == true` is emitted, `ActivityCompat.requestPermissions(...)` is triggered with the correct request code — verified by reading the collect-block handler;
- **`onOAuth2Callback` (`:280`):** when `SyncEvent.OAuth2Callback` is collected, the token is stored via `authPreferences.setOauth2Token(...)` and `SyncEvent.AccountAdded` is emitted if `event.valid()` is true; otherwise the OAUTH2_ACCESS_TOKEN_ERROR dialog is shown — verified by reading the `Dialogs.java` or collect-block handler;
- **`onConnect` (`:289`):** when `SyncEvent.AccountConnectionChanged` is collected with `connected == true`, `AccountManagerAuthActivity` is launched; otherwise the DISCONNECT dialog is shown;
- **`handleFallbackAuth` (`:298`):** when `SyncEvent.FallbackAuth` is collected with `showDialog == true`, the WEB_CONNECT dialog is shown; otherwise `fallbackAuthIntent` is launched with `startActivityForResult`;
- **`themeChangedEvent` (`:306`):** when `SyncEvent.ThemeChanged` is collected, `recreate()` is called on the Activity;
- **`performAction` (`:331`):** when `SyncEvent.PerformActionRequested` is collected, credentials check and dialog/confirmation/direct-execute branching runs as before — `confirm` flag gates the CONFIRM_ACTION dialog per `Dialogs.performAction` (`:331-343`);
- **`doPerform` (`:345`):** when `Actions.Backup` / `BackupSkip` / `Restore` is dispatched, the corresponding `startBackup` / `startRestore` call executes; and
- each handler runs on the main dispatcher inside `repeatOnLifecycle(STARTED)`, so no handler executes while the Activity is in the STOPPED state — this is a behavioral improvement over the prior `onStart`/`onStop` Otto register/unregister, which had the same window but was manual and error-prone.

**AC-5 — `StatusPreference` `App.register(this)` / `App.unregister(this)` and all four of its `@Subscribe` handlers are migrated to lifecycle-aware repository collection**

Given the implementation is merged:
- `App.register(this)` previously at `StatusPreference.java:127` (inside `onBindViewHolder`) is removed;
- `App.unregister(this)` previously at `StatusPreference.java:97` (inside `onDetached`) is removed;
- the four `@Subscribe` handlers — `restoreStateChanged` (`:142`), `backupStateChanged` (`:174`), `onMissingPermissions` (`:202`), and the implicit handler for backup/restore state display updates — are replaced by collection of `repository.state` and `repository.events` inside a lifecycle-aware scope appropriate for a `PreferenceViewHolder` (e.g., using `ViewTreeLifecycleOwner` or a `CoroutineScope` cancelled in `onDetached`);
- the `SmsBackupService.isServiceWorking()` call at `StatusPreference.java:207` is replaced by `repository.state.value.isRunning()`;
- the `SmsRestoreService.isServiceIdle()` call at `StatusPreference.java:221` is replaced by `!repository.state.value.isRunning()` (or the equivalent boolean); and
- button-click `App.post(new PerformAction(...))` calls at `:209` / `:222` and `App.post(new CancelEvent())` calls at `:215` / `:226` are replaced by `repository.tryEmitEvent(SyncEvent.PerformActionRequested(...))` and `repository.tryEmitEvent(SyncEvent.Cancel(Origin.USER))` respectively, with the boolean return checked (logged as a warning on `false`) — satisfying the AC-4 no-silent-failure requirement from REQ-MODERNIZATION-007.

**AC-6 — `grep -n "@Subscribe" app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java` produces zero output**

Given the implementation is merged,
when a developer runs the above grep command,
then the command exits with status 0 and produces zero lines, confirming all four `@Subscribe` annotations are gone from `StatusPreference.java`.

**AC-7 — No `App.register` or `App.unregister` call targets `MainActivity` or `StatusPreference` anywhere in the codebase**

Given the implementation is merged,
when a developer runs `grep -rn "App\.register\|App\.unregister" app/src/main/java/com/zegoggles/smssync/activity/` from the repository root,
then the output contains zero lines referencing `MainActivity.java` or `StatusPreference.java`; any remaining `App.register`/`unregister` calls in other files (e.g. `ServiceBase`) are out of this story's scope and are expected to be addressed by subsequent stories in MU-006.

**AC-8 — `MainViewModel` is obtainable via `ViewModelProvider` with a manual factory; the Activity does not instantiate it directly with `new`**

Given the implementation is merged,
when a developer reads `MainActivity.java` and `MainViewModel.kt`:
- `MainViewModel` is retrieved in `MainActivity.onCreate` via `new ViewModelProvider(this, new MainViewModelFactory(repository)).get(MainViewModel.class)` (or the Kotlin equivalent `viewModels { MainViewModelFactory(repository) }`);
- `MainViewModelFactory` is a new class that implements `ViewModelProvider.Factory` and constructs `MainViewModel(repository)`;
- `MainActivity` does NOT call `new MainViewModel(...)` directly; and
- the ViewModel instance survives configuration changes — confirmed by the fact that a second call to `ViewModelProvider(...).get(MainViewModel.class)` in the same Activity task returns the same instance without re-constructing it.

**AC-9 — The full CI suite is green with no regressions after this story's changes**

Given the implementation is merged,
when `./gradlew test lint` is run from the repository root (see `sdlc/artifacts/engagement/code-location.md` for the actual CI command),
then:
- all unit tests pass, including new tests that assert `MainViewModel` correctly delegates `state` and `events` to the repository, and that the `StatusPreference` collect scope is cancelled on `onDetached`;
- lint passes with `warningsAsErrors true` (`app/build.gradle:41`) — no new `-Werror` / `-Xlint:deprecation` violations introduced;
- no previously passing test is broken; and
- `grep -rn "@Subscribe" app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` and the same for `StatusPreference.java` both return zero lines in the CI environment.

### Integration Criteria

- [ ] IC-1: `MainViewModel` is constructible via `MainViewModelFactory` and is wired to the app-level `SyncStateRepository` singleton before `MainActivity.onCreate` returns
- [ ] IC-2: The `repeatOnLifecycle(STARTED)` collect block in `MainActivity` references `viewModel.state` and `viewModel.events` (from `MainViewModel`), not the repository directly — the ViewModel is the single point of indirection between the Activity and the repository
- [ ] IC-3: All former `App.post(new PerformAction(...))` and `App.post(new CancelEvent())` call sites in `StatusPreference.java` are updated to call `repository.tryEmitEvent(...)` with the correctly typed `SyncEvent` variant per CNTR-MODERNIZATION-006 §Type: SyncEvent

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` | 499 LOC; `App.register(this)` at `:155` / `App.unregister(this)` at `:160`; nine `@Subscribe` handlers at `:265`–`:355`; two `App.post(...)` calls at `:283` / `:451` | Remove `App.register`/`unregister`; remove all nine `@Subscribe` handlers; add `MainViewModel` field and `ViewModelProvider` retrieval; add single `lifecycleScope.launch { repeatOnLifecycle(STARTED) { ... } }` collection block; replace `App.post` call sites with `viewModel` method calls |
| `app/src/main/java/com/zegoggles/smssync/activity/MainViewModel.kt` (new file) | Does not exist | Create `MainViewModel extends ViewModel` with `SyncStateRepository` constructor parameter; expose `state: StateFlow<SyncState>` and `events: SharedFlow<SyncEvent>`; add action methods replacing `App.post` call sites |
| `app/src/main/java/com/zegoggles/smssync/activity/MainViewModelFactory.kt` (new file) | Does not exist | Create `ViewModelProvider.Factory` implementation that constructs `MainViewModel(repository)` |
| `app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java` | `App.register(this)` at `:127`; `App.unregister(this)` at `:97`; four `@Subscribe` handlers at `:142`, `:174`, `:202` (plus the implicit state-display handler); `isServiceWorking()` at `:207`; `isServiceIdle()` at `:221`; `App.post(new PerformAction(...))` at `:209`, `:222`; `App.post(new CancelEvent())` at `:215`, `:226` | Remove `App.register`/`unregister`; remove all four `@Subscribe` handlers; replace `isServiceWorking()`/`isServiceIdle()` with `repository.state.value.isRunning()`; replace `App.post(...)` with `repository.tryEmitEvent(...)` per CNTR-MODERNIZATION-006; add lifecycle-aware collect scope cancelled in `onDetached` |
| `app/src/main/java/com/zegoggles/smssync/activity/Dialogs.java` | Existing class; some dialog-dispatching logic today lives on `@Subscribe` handlers in `MainActivity` | Receive relocated dialog-branch logic from the migrated `onOAuth2Callback`, `onConnect`, `handleFallbackAuth`, and `performAction` handlers; add corresponding public methods if not already present |

## Existing Behavior to Preserve

- Backup progress, restore progress, and final status (FINISHED_BACKUP, FINISHED_RESTORE, CANCELED_BACKUP, CANCELED_RESTORE) updates must continue to appear in `StatusPreference`'s UI row in real time during an active sync.
- A collector subscribing after a state was emitted (e.g., after rotation) must immediately receive the last-emitted state — this is the Otto `@Produce` sticky-semantics contract preserved by `StateFlow.value` and confirmed by AC-5 / CNTR-MODERNIZATION-006 Validation Rule 1.
- One-shot events (`MissingPermissions`, `Cancel`, `PerformActionRequested`, `OAuth2Callback`, auth/account/theme events) must NOT be re-delivered to a new collector after rotation — `SharedFlow(replay = 0)` enforces this by construction.
- The `restoreStateChanged` + `isSmsBackupDefaultSmsApp` → `restoreDefaultSmsProvider` path in `MainActivity` must survive (AC-4 bullet 1).
- The `backupStateChanged` → `requestPermissions` path for MANUAL/SKIP backups with missing permissions must survive (AC-4 bullet 2).
- The `CancelEvent.Origin.USER` vs `Origin.SYSTEM` distinction driving `mayInterruptIfRunning()` must be preserved in every `tryEmitEvent(SyncEvent.Cancel(...))` call site — no flattening to a boolean parameter (CNTR-MODERNIZATION-006 §SyncEvent variant `Cancel`).
- The `PerformAction.confirm` flag that gates the CONFIRM_ACTION dialog in `Dialogs.performAction` (`:331-343`) must be passed through `SyncEvent.PerformActionRequested(action, confirm)` unchanged.
- `StatusPreference` button state resets (`backupButton.setText(...)`, `backupButton.setEnabled(false)`) on in-flight cancel must still occur at the time the user taps the cancel button — these are local UI mutations, not state updates from the repository.
- `App.post(new AccountAddedEvent())` in the `onOAuth2Callback` path at `MainActivity.java:283` must be replaced by `repository.tryEmitEvent(SyncEvent.AccountAdded)` with the result checked — the downstream subscriber behavior (auto-backup settings reload) must be preserved.

## Verification Steps

**AC-1 — No `@Subscribe` in MainActivity:**
1. From the repository root run: `grep -n "@Subscribe" app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java`
2. Confirm zero lines of output and exit status 0.
3. Record the command and its zero-line output in Notes.

**AC-2 — `App.register`/`unregister` removed from MainActivity; `repeatOnLifecycle` block present:**
1. Run: `grep -n "App\.register\|App\.unregister" app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` — confirm zero output.
2. Run: `grep -n "repeatOnLifecycle" app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` — confirm exactly one matching line inside `onCreate` or a setup method called from it.
3. Read the collect block and confirm it launches at least two child `launch` coroutines — one collecting `viewModel.state`, one collecting `viewModel.events`.

**AC-3 — MainViewModel exposes `state` and `events`:**
1. Read `MainViewModel.kt` and confirm it extends `androidx.lifecycle.ViewModel`.
2. Confirm the constructor accepts a `SyncStateRepository` parameter and carries a `// TODO @HiltViewModel` comment or equivalent.
3. Confirm `state` is declared as `val state: StateFlow<SyncState>` and `events` as `val events: SharedFlow<SyncEvent>`.
4. Confirm no direct `App.bus`, `App.register`, or `App.post` call exists in the file.

**AC-4 — Nine handler behaviors preserved:**
1. For each of the nine former `@Subscribe` handlers, trace the corresponding `when (event)` or `when (state)` branch in the collect block and confirm the method call it dispatches matches the original handler body (read side-by-side against `MainActivity.java` lines `:265`–`:355`).
2. Specifically: confirm `SyncEvent.ThemeChanged` → `recreate()` is present; confirm `SyncEvent.PerformActionRequested` → `confirm` flag is read; confirm `SyncEvent.OAuth2Callback` → `event.valid()` branch is present.
3. Run the new `MainViewModelTest` (or equivalent) and confirm all tests pass.

**AC-5 and AC-6 — StatusPreference migrated:**
1. Run: `grep -n "@Subscribe" app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java` — confirm zero output; record in Notes.
2. Run: `grep -n "App\.register\|App\.unregister" app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java` — confirm zero output.
3. Read `StatusPreference.java` and confirm `isServiceWorking()` and `isServiceIdle()` are absent; their replacements call `repository.state.value.isRunning()`.
4. Confirm `tryEmitEvent(SyncEvent.Cancel(Origin.USER))` is called (not the old `new CancelEvent()`) and the boolean return is checked.
5. Confirm a coroutine scope is created and cancelled in `onDetached` (no scope leak after the preference view is detached).

**AC-7 — No stray `App.register`/`unregister` calls targeting activity classes:**
1. Run: `grep -rn "App\.register\|App\.unregister" app/src/main/java/com/zegoggles/smssync/activity/`
2. Confirm zero lines reference `MainActivity.java` or `StatusPreference.java`.
3. Any lines referencing other files (e.g. fragments) are out of this story's scope and are noted, not treated as failures here.

**AC-8 — ViewModel obtained via factory, not `new`:**
1. Read `MainActivity.java` and confirm `ViewModelProvider` (or the `viewModels { }` Kotlin delegate) is used to retrieve `MainViewModel`.
2. Confirm `MainViewModelFactory` implements `ViewModelProvider.Factory` and constructs `MainViewModel(repository)`.
3. Confirm no `new MainViewModel(...)` direct instantiation exists in `MainActivity.java`.

**AC-9 — CI green:**
1. Run `./gradlew test lint` (or the project CI command from `sdlc/artifacts/engagement/code-location.md`).
2. Confirm `BUILD SUCCESSFUL` with zero test failures and zero lint errors.
3. Confirm the two `@Subscribe` grep checks for `MainActivity.java` and `StatusPreference.java` both return zero lines when run in CI.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (all) | Introduce `MainViewModel` and `MainViewModelFactory`; refactor `MainActivity` collect block; migrate `StatusPreference` to repository collection; relocate dialog branches to `Dialogs.java`; write unit tests for ViewModel and StatusPreference collect scope cancellation | Developer |

## Technical Context

- **Hard prerequisite — U-020 must be merged first.** U-020 delivers the `SyncStateRepository` interface, its `MutableStateFlow`/`MutableSharedFlow`-backed implementation, and the `SyncEvent` sealed hierarchy as fixed by CNTR-MODERNIZATION-006. `MainViewModel` holds the repository injected by manual DI; it cannot be compiled or tested without the repository class on the classpath.
- **Manual DI only — no Hilt at this stage.** Per DES-MODERNIZATION-007 §Hilt injection, the Hilt `@HiltViewModel` retrofit is deferred to MU-007 (DES-MODERNIZATION-008). `MainViewModelFactory` is a vanilla `ViewModelProvider.Factory`. Mark the deferred step with a `// TODO @HiltViewModel — retrofit in MU-007 (U-0xx)` comment in `MainViewModel.kt`.
- **The repository is the app-level manual singleton** constructed in `App.onCreate` (or equivalent) as part of U-020's delivery. `MainActivity` retrieves it from there (e.g., `((App) getApplication()).getSyncStateRepository()` or a static accessor consistent with U-020's design) and passes it to `MainViewModelFactory`. No new singleton mechanism is introduced in this story.
- **`repeatOnLifecycle(STARTED)` exactly matches the prior `onStart`/`onStop` register/unregister window.** The Activity was previously registered at `onStart` and unregistered at `onStop`; `STARTED` lifecycle state corresponds to the same visibility window, so the subscription/unsubscription boundary is preserved with zero behavioral change. Using `CREATED` or `RESUMED` instead would be incorrect.
- **`StatusPreference` lifecycle scope.** `StatusPreference` is a `Preference` subclass, not a Fragment or Activity, so it has no built-in `lifecycleScope`. The correct approach is to create a `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` field in `StatusPreference`, start collection in `onBindViewHolder`, and cancel the scope in `onDetached`. Alternatively, use `ViewTreeLifecycleOwner.get(holder.itemView)?.lifecycleScope` if the view tree exposes a lifecycle owner. The implementer should choose the approach that avoids a scope leak and document the choice in Implementation Notes.
- **Nine `@Subscribe` handlers confirmed by grep in DES-MODERNIZATION-007.** Lines `:265`, `:271`, `:280`, `:289`, `:298`, `:306`, `:331`, `:345` — all verified against the live source this session. The implementation must handle all nine; the `doPerform(Actions)` handler at `:345` receives an `Actions` enum directly (not a `PerformAction` POJO) — in the Flow model it is dispatched as a second step from the `PerformActionRequested` handler, or inlined into it.
- **`warningsAsErrors true` and `-Xlint:deprecation` are active** (`app/build.gradle:41`, `:73-77`). Any use of a deprecated API — including continuing to import `com.squareup.otto.Subscribe` or calling `App.register` — will fail the build. This is a compile-time enforcement of AC-1 and AC-7.
- **`SyncEvent.Cancel.Origin` is load-bearing** (CNTR-MODERNIZATION-006 Validation Rule 7). Every former `App.post(new CancelEvent())` (default constructor = `Origin.USER`) must be replaced by `tryEmitEvent(SyncEvent.Cancel(Origin.USER))`. A future system-originated cancel must use `Origin.SYSTEM`. Do not flatten to a boolean; the enum and `mayInterruptIfRunning()` semantics must remain callable by downstream consumers.
- **Two `App.post` calls in `MainActivity` become ViewModel methods.** `App.post(new AccountAddedEvent())` at `:283` (inside the former `onOAuth2Callback` handler) and `:451` (in another context — implementer must read the surrounding code) become `viewModel.emitEvent(SyncEvent.AccountAdded)` or equivalent. These must be routed through the ViewModel, not called directly on the repository from the Activity.
- **`Dialogs.java` is the target for dialog-branch relocation.** The existing `Dialogs.java` already houses dialog construction logic. The OAuth2Callback, connect/disconnect, fallback-auth, and performAction dialog branches move there as public or package-private methods callable from the Activity's collect block. The implementer must confirm that `Dialogs.java` can be called from the collect block without introducing a cyclic dependency.

## Supporting Documentation

- REQ-MODERNIZATION-007 §"Acceptance Criteria" AC-3 — `MainActivity` uses `MainViewModel`, collects via `repeatOnLifecycle`, dialog logic → `Dialogs.java`
- REQ-MODERNIZATION-007 §"Context" — God-Activity pattern (STRUCT-004) and the bus migration rationale
- DES-MODERNIZATION-007 §"MainViewModel decomposition with repeatOnLifecycle (AC-3)" — full decomposition specification including verified line numbers for all nine `@Subscribe` handlers and both `StatusPreference` register/unregister sites
- DES-MODERNIZATION-007 §"Hilt injection (DES-008)" — sequencing constraint requiring manual DI (not Hilt) in this story
- DES-MODERNIZATION-007 §"Architecture diagram (target)" — shows `MainActivity → MainViewModel → SyncStateRepository` and `StatusPreference → SyncStateRepository` collect paths
- CNTR-MODERNIZATION-006 §"Interface: SyncStateRepository" — the `state`/`events`/`tryEmitEvent` signatures this story's code must conform to
- CNTR-MODERNIZATION-006 §"Type: SyncEvent" — the sealed class hierarchy and variant payloads (particularly `Cancel.Origin`, `PerformActionRequested.confirm`, `MissingPermissions.permissions`)
- CNTR-MODERNIZATION-006 §"Validation Rules" — rules 1–7 are all enforced by this story's collect block (sticky replay, one-shot non-replay, no silent failure, immutable payload, `Cancel.Origin` load-bearing)
- CNTR-MODERNIZATION-006 §"Example Payloads" — consumer-side `repeatOnLifecycle` code pattern to follow verbatim

## Integration Contract References

- CNTR-MODERNIZATION-006 §"Interface: SyncStateRepository" — `MainViewModel` wraps `repository.state` and `repository.events`; `StatusPreference` calls `repository.tryEmitEvent`; the return boolean from `tryEmitEvent` must be consumed (AC-5, AC-4 silent-failure rule)
- CNTR-MODERNIZATION-006 §"Type: SyncEvent — `Cancel`" — `Cancel.Origin.USER` default must be used for all user-initiated cancel button taps in `StatusPreference`; `mayInterruptIfRunning()` semantics are downstream-consumed and must not be broken
- CNTR-MODERNIZATION-006 §"Type: SyncEvent — `PerformActionRequested`" — `confirm` flag carried verbatim into `SyncEvent.PerformActionRequested`; gates the CONFIRM_ACTION dialog in `Dialogs.performAction`
- CNTR-MODERNIZATION-006 §"Validation Rules" rule 3 — one-shot invariant (`replay = 0`): the collect block in `MainActivity` must never hold a reference to a past event across configuration changes; `SharedFlow(replay = 0)` enforces this at the repository, but the Activity's collect block must restart clean on each `STARTED` entry

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Dependency on U-020: this story cannot be started until U-020's `SyncStateRepository`, `SyncEvent`, and the Flow-backed implementation are merged and the repository is constructible via manual DI. Scope boundary: this story removes all `@Subscribe` annotations from `MainActivity` and `StatusPreference` only. `ServiceBase`, `SmsBackupService`, `SmsRestoreService`, and the fragment/settings classes retain their Otto wiring until their respective MU-006 sub-stories; this story must not pre-emptively delete `App.bus` or the Otto dependency — that deletion is the closing step of the final MU-006 story.
