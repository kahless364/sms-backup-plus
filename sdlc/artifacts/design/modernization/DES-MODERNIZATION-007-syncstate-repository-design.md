---
status: approved
artifact_type: design-document
applicable_prompts:
  - system-architecture
  - integration-design
  - design-validation
related_requirements:
  - REQ-MODERNIZATION-007
related_stories: []
related_design_docs: []
integration_contracts:
  - CNTR-MODERNIZATION-006
change_records: []
type: ''
id: DES-MODERNIZATION-007
title: ''
domain: modernization
---

# DES-MODERNIZATION-007: SyncStateRepository — Otto → StateFlow/SharedFlow

## Overview

This design replaces the abandoned Square Otto event bus (`com.squareup:otto:1.3.8`,
`app/build.gradle:57`) with an app-owned `SyncStateRepository` that exposes a sticky
`StateFlow<SyncState>` (reproducing Otto's `@Produce` last-value semantics via
`StateFlow.value`) plus a `SharedFlow<SyncEvent>` for one-shot events. It removes the
process-global `App.bus` static singleton, the two `private static` service
back-references, and the silent `IllegalArgumentException` swallow in `App.register()`;
it decomposes the 499-LOC `MainActivity` God-Activity (STRUCT-004) behind a
`MainViewModel` that collects state lifecycle-aware via `repeatOnLifecycle`. It
implements REQ-MODERNIZATION-007 / migration unit MU-006 and conforms to
target-state.md ADR-003.

This is **modernization by branch-by-abstraction at the eventing seam, not a rewrite**:
`App.bus` is first wrapped behind the `SyncStateRepository` interface with zero behavior
change (Feathers "Introduce Instance Delegator", *Working Effectively with Legacy Code*
Ch. 25), then the implementation is swapped to Flow per event type, and Otto is deleted
only after the last importer migrates.

## Context

Verified against live source this session (all paths relative to repo root):

- **15 Otto import lines across 12 distinct files** — grep-confirmed
  (`grep -rc com.squareup.otto app/src/main/java`). `App.java`,
  `service/SmsBackupService.java`, and `service/SmsRestoreService.java` each carry two
  `com.squareup.otto.*` imports (`Bus`/`Subscribe`, or `Produce`/`Subscribe`); the other
  nine carry one each (2 + 2 + 2 + 9 = 15 import lines, 12 distinct files). The
  requirement's "all 12 importing files" (REQ-MODERNIZATION-007 AC-2) is correct and keys
  on **12 distinct files** — both this design and MU-006 enumerate the same 12 distinct
  files. The full set: `App.java`, `activity/Dialogs.java`, `activity/MainActivity.java`,
  `activity/StatusPreference.java`, `activity/auth/OAuth2WebAuthActivity.java`,
  `activity/fragments/AdvancedSettings.java`, `activity/fragments/MainSettings.java`,
  `service/BackupTask.java`, `service/RestoreTask.java`, `service/SmsBackupService.java`,
  `service/SmsJobService.java`, `service/SmsRestoreService.java`.
- **28 `@Subscribe`/`@Produce` handlers** — grep-confirmed across the 12 files (the
  patterns.md figure of "18 handlers / 11 classes" predates the current tree; the live
  count this session is 28 annotated methods, of which two are `@Produce`).
- **`App.bus`** is `private static final Bus bus = new Bus()` (`App.java:59`), exposed via
  static `register`/`unregister`/`post` (`App.java:116-134`). `register()` and
  `unregister()` both **catch `IllegalArgumentException` and only `Log.w`**
  (`App.java:116-130`) — the silent-swallow defect AC-4 targets.
- **Two `@Produce` sticky producers**: `SmsBackupService.produceLastState()` returns the
  last `BackupState` (`SmsBackupService.java:202-204`); `SmsRestoreService.produceLastState()`
  returns the last `RestoreState` (`SmsRestoreService.java:157-159`). These are the sticky
  semantics AC-1 must preserve.
- **Two `private static` service back-references**: `SmsBackupService.service`
  (`:66`) and `SmsRestoreService.service` (`:40`), set in `onCreate`/nulled in
  `onDestroy`, queried by `isServiceWorking()` (`SmsBackupService.java:311-313`) and
  `isServiceIdle()` (`SmsRestoreService.java:172-174`). `SmsBackupService.handleIntent`
  calls `SmsRestoreService.isServiceIdle()` (`SmsBackupService.java:101`) — the
  bidirectional static coupling AC-2 deletes.
- **register/unregister call sites (verified by grep + read this session)**: the bus
  lifecycle is driven from `ServiceBase.onCreate`/`onDestroy` (`App.register(this)` `:76`,
  `App.unregister(this)` `:83`) for both services; from `MainActivity.onStart`/`onStop`
  (`App.register(this)` `MainActivity.java:155`, `App.unregister(this)` `:160` — note it
  registers **only `this`**, in `onStart`/`onStop`, NOT `onResume`/`onPause`, and there is
  **no separate `dialogs` registration**: the `Dialogs` `@Subscribe` handlers physically
  reside on `MainActivity` itself); from `StatusPreference.onBindViewHolder`/`onDetached`
  (`App.register(this)` `:127`, `unregister` `:97`); and `App.onCreate` registers itself
  (`App.java:105`). `MainActivity` also posts `AccountAddedEvent` via `App.post`
  (`:283,:451`). Every one of these register/collect pairs is replaced by lifecycle-aware
  Flow collection.
- **State model (Preserved Core, MU-000)**: `State` is an immutable base with `final`
  `state`/`exception`/`dataType` (`State.java:18-27`); `BackupState`/`RestoreState`
  extend it with `transition(newState, e)` returning a **new** instance
  (`BackupState.java:42-45`, `RestoreState.java:51-54`). `SmsSyncState` is an 11-constant
  enum (`SmsSyncState.java:3-15`). `State.getErrorMessage` string-matches the k-9 magic
  string `"Unable to get IMAP prefix"` (`State.java:32-33`) — that line is owned by
  **MU-008**, not this unit.
- **Build**: `targetSdk`/`compileSdk` 29, `minSdk` 14, Java 8, `warningsAsErrors true`
  (`app/build.gradle:41`), `-Werror -Xlint:deprecation` (`:73-77`), no Kotlin/coroutines
  on the classpath yet. Kotlin + coroutines arrive via REQ-MODERNIZATION-001 (the AGP/SDK
  uplift, MU-001) — a hard prerequisite for this design.

---

## System Architecture

### Design Decision: App-owned SyncStateRepository replacing the Otto bus

`SyncStateRepository` is a single app-owned interface that becomes the sole channel for
engine → UI state propagation, replacing `App.bus` and both static service queries. It
exposes two reactive surfaces and one publish surface, mapping directly onto Otto's two
distinct usages observed in source:

| Otto usage (current source) | Replacement | Rationale |
|---|---|---|
| `@Produce produceLastState()` sticky last `BackupState`/`RestoreState` (`SmsBackupService.java:202-204`, `SmsRestoreService.java:157-159`) | `StateFlow<SyncState>` — `.value` holds the current state, replayed to every new collector | `StateFlow` is a `BehaviorSubject`-equivalent: it always has a value and replays it on subscription, which is exactly the `@Produce` late-subscriber contract (target-state.md ADR-003) |
| Transient `@Subscribe` POJO events: `PerformAction`, `MissingPermissionsEvent`, `CancelEvent`, account/settings/theme/auth events | `SharedFlow<SyncEvent>` — `replay = 0`, `extraBufferCapacity ≥ 1` | One-shot events must NOT be sticky (re-emitting "permissions missing" on every rotation is a defect); `SharedFlow` with no replay is the correct one-shot channel |
| `App.post(state)` from the engine (e.g. `SmsBackupService.moveToState`→`App.post` `:194`; `SmsRestoreService.postError`→`App.post` `:111`) | `repository.emitState(SyncState)` | single typed publish surface; no global static |
| `App.post(event)` transient (e.g. `StatusPreference` posts `PerformAction`/`CancelEvent` `:209,215,222,226`) | `repository.emitEvent(SyncEvent)` | typed; suspending or `tryEmit` per call site |
| `isServiceWorking()` / `isServiceIdle()` static queries (`StatusPreference.java:207,221`; `SmsBackupService.java:101`) | `repository.state.value.isRunning()` (and, post-MU-005, a `WorkInfo` read) | the "is a sync running?" question becomes a read of the single source of truth, not a static `Service` field |

**Proposed interface (Kotlin; final signatures are fixed by CNTR-MOD-007-001):**

```kotlin
interface SyncStateRepository {
    val state: StateFlow<SyncState>          // sticky — replaces @Produce
    val events: SharedFlow<SyncEvent>        // one-shot — replaces transient @Subscribe POJOs
    fun emitState(newState: SyncState)       // engine publish surface (BackupState/RestoreState)
    suspend fun emitEvent(event: SyncEvent)  // suspending publish for back-pressure-safe events
    fun tryEmitEvent(event: SyncEvent): Boolean // non-suspending publish for non-coroutine call sites (receivers)
}
```

`SyncState` is a sealed type covering the existing `State` hierarchy. To preserve the
Preserved-Core invariant (MU-000: no behavior change to `State`/`BackupState`/
`RestoreState`), the **initial migration keeps the existing Java `State` classes as the
payload type** (`StateFlow<State>`), wrapping `BackupState`/`RestoreState` unchanged. The
Kotlin `sealed interface SyncState` promotion noted in patterns.md Deep-Dive 1 is
explicitly deferred (a future MU-012, Phase 3) and is **out of scope here** — this design
does not alter the immutable state machine.

`SyncEvent` is a new `sealed class` that **absorbs** the transient Otto POJOs. Each
variant maps 1:1 to a current event so no consumer behavior is lost:

| Current POJO (verified) | SyncEvent variant | Behavioral note |
|---|---|---|
| `service/CancelEvent.java` — an `Origin` enum {USER, SYSTEM} (default ctor = USER) with `mayInterruptIfRunning()` returning `origin == SYSTEM` (`:5-19`) | `SyncEvent.Cancel(origin: Origin)` preserving `mayInterruptIfRunning()` | **The USER vs SYSTEM Origin distinction is load-bearing** (it drives `mayInterruptIfRunning`) and MUST be preserved — it is NOT a trivial POJO. (Corrects the MU-006 shorthand "Cancel(fromUser)": the real field is an `Origin` enum, not a boolean.) |
| `activity/events/PerformAction.java` (`Actions` enum Backup/BackupSkip/Restore + `boolean confirm`, `:3-17`) | `SyncEvent.PerformAction(action: Actions, confirm: Boolean)` | enum AND the `confirm` flag preserved verbatim (`confirm` gates the confirmation dialog in `Dialogs.performAction` `:331-343`) |
| `activity/events/MissingPermissionsEvent.java` (`List<AppPermission> permissions`, `:7-12`) | `SyncEvent.MissingPermissions(permissions: List<AppPermission>)` | payload is `List<AppPermission>` (NOT `List<String>`); must remain one-shot (non-sticky) |
| `AccountAddedEvent`, `AccountRemovedEvent`, `AccountConnectionChangedEvent`, `AutoBackupSettingsChangedEvent`, `FallbackAuthEvent`, `SettingsResetEvent`, `ThemeChangedEvent` | corresponding `SyncEvent.*` variants | absorb or delete after subscriber migration |
| `OAuth2CallbackTask.OAuth2CallbackEvent` (subscribed in `MainActivity`/`Dialogs`) | `SyncEvent.OAuth2Callback(...)` | preserve payload |

> **Verification gap flagged for implementation (not resolved at design time):** several
> `activity/events/*` POJOs are dispatched/consumed only through Otto reflection. Before
> deleting any POJO, the implementer MUST grep for both `@Subscribe` consumers and
> `App.post(new <Event>` producers and confirm each has a migrated counterpart. This
> mirrors MU-006's "4 of 9 POJOs have no static @Subscribe grep evidence" caveat.

**Trade-off (explicit):** `StateFlow` vs `LiveData` — both reproduce sticky semantics and
are lifecycle-aware. `StateFlow`/`SharedFlow` is chosen because it composes/maps cleanly,
is the current Android "Guide to App Architecture" guidance, is the same primitive
WorkManager-side workers use to publish progress (DES-005), and avoids a second
observation idiom. `LiveData` is in soft-maintenance. **Rejected:** Greenrobot EventBus
(dependencies.md floated it as a near-drop-in) — it perpetuates the global-bus /
Service-Locator anti-pattern (ARCH-001) and *adds* a dependency rather than removing one,
defeating the requirement's whole purpose.

### Design Decision: Eliminate the silent App.register() swallow (AC-4)

`App.register()`/`unregister()` exist solely because Otto throws `IllegalArgumentException`
when a listener has no `@Subscribe` method or is double-(un)registered, and the current
code swallows that to `Log.w` (`App.java:116-130`). In the Flow model there is **no
register step** — collectors simply `collect` the flow inside a coroutine scope. The
class of failure the swallow masked (registering a listener with no handler) becomes a
**compile-time impossibility**: there is no annotation-driven dispatch to misconfigure.
AC-4 is therefore satisfied structurally, not by replacing one silent catch with another.
The repository's `emit`/`tryEmit` surfaces do not swallow: `tryEmitEvent` returns its
boolean result to the caller (back-pressure visible, not hidden), and `emitState` on a
`MutableStateFlow` cannot fail in the way `register` could.

### Design Decision: MainViewModel decomposition with repeatOnLifecycle (AC-3)

`MainActivity` (499 LOC — verified by line count this session — the highest-complexity
file) carries **nine `@Subscribe` handlers** (grep-confirmed at `:265,:271,:280,:289,:298,
:306,:331,:345`): `restoreStateChanged`, `backupStateChanged`, `onOAuth2Callback`,
`onConnect`, `handleFallbackAuth`, `themeChangedEvent`, `performAction`, `doPerform` (the
ninth being the second `@Subscribe` in the `:265–:345` block). It `App.register(this)` in
`onStart` (`:155`) and `App.unregister(this)` in `onStop` (`:160`) — verified this
session. The decomposition:

- A new `MainViewModel` (AndroidX `ViewModel`) holds the injected `SyncStateRepository`
  and exposes the `state`/`events` flows (optionally `map`-ped to a UI model). The
  ViewModel survives configuration changes; the Activity does not re-subscribe on every
  rotation.
- `MainActivity` replaces the `onStart`/`onStop` `App.register`/`App.unregister` pair
  (`:155`/`:160`) with a single `lifecycleScope.launch { repeatOnLifecycle(
  Lifecycle.State.STARTED) { ... collect } }` block, so collection starts at STARTED and
  stops at STOPPED — the lifecycle-aware contract AC-3 requires (and the correct
  replacement for Otto's manual register).
- **Dialog logic moves to the existing `activity/Dialogs.java`** (AC-3). The dialog-related
  `@Subscribe` handlers verified in `MainActivity.java` are `onOAuth2Callback` (`:280`),
  `onConnect` (`:289`), `handleFallbackAuth` (`:298`), `themeChangedEvent` (`:306`),
  `performAction` (`:331`), and `doPerform` (`:345`), alongside `restoreStateChanged`
  (`:265`) / `backupStateChanged` (`:271`). The decomposition relocates the
  dialog/permission branches into `Dialogs` and drives them off the collected
  `SyncEvent`/`SyncState` flows. `StatusPreference` (which today
  `App.register(this)` in `onBindViewHolder` `:127`, `unregister` in `onDetached` `:97`,
  with `@Subscribe` handlers `restoreStateChanged` `:142`, `backupStateChanged` `:174`,
  `onMissingPermissions` `:202`, and posts `PerformAction`/`CancelEvent` on button click
  `:209,215,222,226`) is migrated to collect the same repository flows lifecycle-aware.

**Trade-off (explicit):** ViewModel decomposition adds a class and an injection edge but
removes the rotation-churn re-subscribe, the manual register/unregister leak surface, and
concentrates state ownership. The alternative — leaving collection in the Activity — keeps
the God-Activity and forfeits the STRUCT-004 win the requirement explicitly bundles into
this change (AC-3, Rationale).

### Removal scope (AC-2) — verified deletions

| Artifact | Location (verified) | Action |
|---|---|---|
| Otto dependency | `app/build.gradle:57` | delete after last importer migrates |
| `App.bus` + static `register`/`unregister`/`post` | `App.java:59,116-134` | delete; `App.onCreate`'s `register(this)` (`:105`) and the `@Subscribe autoBackupSettingsChanged` (`:108`) move to repository collection in a non-static scope |
| `ServiceBase` register/unregister | `ServiceBase.java:76,83` | delete; the service base no longer touches the bus |
| `SmsBackupService.service` static + `isServiceWorking()` | `SmsBackupService.java:66,311-313` | delete; callers read `repository.state` / `WorkInfo` |
| `SmsRestoreService.service` static + `isServiceIdle()` | `SmsRestoreService.java:40,172-174` | delete; remove the cross-service call in `SmsBackupService.handleIntent` (`SmsBackupService.java:101`) |
| `@Produce`/`@Subscribe` methods | the 28 annotated sites (grep-confirmed) | replaced by `emitState`/`collect` |
| transient Otto POJOs | `activity/events/*` (9 files) + `service/CancelEvent.java` | absorbed into `SyncEvent` or deleted (after grep-verified consumer migration) |

### Architecture diagram (target)

```
PRESENTATION
  MainActivity ── owns ──▶ MainViewModel ──┐
  StatusPreference ───────────────────────┤  inject SyncStateRepository
  Dialogs (dialog/permission/OAuth flows) ─┘
        │  collect (repeatOnLifecycle STARTED): state: StateFlow<SyncState>
        │  collect: events: SharedFlow<SyncEvent>
        ▼
  ┌─────────────────────────────────────────────┐
  │ SyncStateRepository (app-owned, @Singleton)  │
  │   state:  StateFlow<SyncState>  (sticky)      │
  │   events: SharedFlow<SyncEvent> (one-shot)    │
  │   emitState / emitEvent / tryEmitEvent        │
  └─────────────────────────────────────────────┘
        ▲ emitState(BackupState/RestoreState)         ▲ emitEvent(Cancel/PerformAction/…)
ENGINE  │                                             │
  BackupWorker / RestoreWorker (CoroutineWorker, DES-005) ── publish progress ──┘
  (post-MU-005 the AsyncTask BackupTask/RestoreTask are gone; pre-MU-005 the
   facade still routes through App.bus — see Integration Design)
```

Traceability: AC-1 → `StateFlow`+`SharedFlow` decision; AC-2 → Removal scope table;
AC-3 → MainViewModel decision; AC-4 → silent-swallow decision; AC-5 → Design Validation.

---

## Integration Design

### Sequencing — this design lands AFTER DES-005 (WorkManager)

REQ-MODERNIZATION-007 Constraints and MU-006 dependencies require this unit to follow
**REQ-MODERNIZATION-005 / DES-005 (WorkManager)** to avoid double-churning the engine.

> **Cross-design status caveat (verified this session):** DES-MODERNIZATION-005 and
> DES-MODERNIZATION-008 are both currently `status: draft` and are **unwritten template
> stubs** (no content beyond the scaffold). The integration described below is the
> *intended* handshake per target-state.md (ADR-001/ADR-008) and the MU dependency graph;
> it is NOT yet anchored to approved sibling-design content. This is a real planning
> dependency, not a satisfied one: a story implementing MU-006 must not be sprint-planned
> until DES-005 (the worker-publish surface this design feeds) is authored and approved,
> and CNTR-MOD-007-001 is approved. Recorded as a risk and surfaced in the verdict.

The integration handshake (target-state.md-intended):

1. **DES-005 first** converts `BackupTask`/`RestoreTask` (AsyncTask) into
   `BackupWorker`/`RestoreWorker` (`CoroutineWorker`) and merges the foreground lifecycle
   of `SmsBackupService`/`SmsRestoreService` into the workers via `setForeground`.
2. **DES-005's workers will publish state** by calling `SyncStateRepository.emitState(...)`
   (and `setProgress` for WorkManager observability). This design **owns the repository
   contract** that DES-005 will consume — hence CNTR-MOD-007-001 must be `approved`, and
   DES-005 authored to consume it, before either DES-005's worker-publish path or this
   design's collectors are implemented.
3. **This design then** removes Otto from the (now-`CoroutineWorker`) engine and the UI,
   deletes `App.bus` and the static service fields, and lands the ViewModel decomposition.

Because the workers are already on coroutines after DES-005, the engine-side `emitState`
call sites are idiomatic; if DES-005 has not landed, the facade step below still works
against the AsyncTasks but on the Otto implementation.

### Migration path — branch-by-abstraction (zero behavior change first)

Per REQ-MODERNIZATION-007 Constraints and Feathers "Introduce Instance Delegator":

- **Step 1 — Facade (REVERSIBLE, no behavior change):** introduce the
  `SyncStateRepository` interface with a single implementation that **delegates to the
  existing `App.bus`** (`emitState`/`emitEvent` call `App.post`; collection still via Otto
  `@Subscribe`). Inject it where `App.post`/`register` are called today. Ship and verify —
  no observable change. This is the "wrap `App.bus` behind the interface first" step the
  requirement mandates.
- **Step 2 — Swap implementation to Flow, per event type:** replace the Otto-delegating
  impl with a `MutableStateFlow`/`MutableSharedFlow`-backed impl. Migrate emitters and
  collectors one event type at a time (sticky state first — it has the clearest
  `@Produce`→`StateFlow.value` mapping — then the transient `SyncEvent` POJOs). Each
  migrated type is independently shippable and reversible until the dep line is removed.
- **Step 3 — Delete Otto:** remove `com.squareup:otto` (`app/build.gradle:57`), `App.bus`,
  the static service fields, and the absorbed POJOs, only after grep confirms zero
  remaining `com.squareup.otto` imports and zero `App.post`/`register` call sites.

### Hilt injection (DES-008)

`SyncStateRepository` is provided as an application-scoped `@Singleton` via Hilt
(DES-MODERNIZATION-008 — **currently `status: draft`/unwritten**, so the injection design
below is the target per target-state.md ADR-008, not an approved sibling-design contract).
Injection targets: `MainViewModel` (via
`@HiltViewModel`), `StatusPreference`/`Dialogs` (field/entry-point injection as
appropriate for non-`@AndroidEntryPoint` preference views), and the
`BackupWorker`/`RestoreWorker` (`@HiltWorker` / `WorkerAssistedFactory`, DES-005+DES-008).
**Sequencing nuance:** DES-008 (Hilt, MU-007) is scheduled *after* this unit in the MU
graph (MU-007 depends on MU-005+MU-006). Therefore the **Step-1 facade and Step-2 swap
must not assume Hilt is present** — the repository is initially constructed/held as a
manual singleton (consistent with the existing dual-constructor manual-DI discipline,
patterns.md §Manual DI) and is *retrofitted* to Hilt injection when MU-007 lands. This
ordering is a design constraint, not an option: assuming Hilt here would create a
backwards dependency on an unlanded unit.

### Receivers and non-coroutine call sites

`App.onCreate` registers itself on the bus and `BackupBroadcastReceiver`/
`SmsBroadcastReceiver` paths post events outside a coroutine scope. These use
`tryEmitEvent` (non-suspending) against the `SharedFlow` (sized with
`extraBufferCapacity` so a one-shot emit from a receiver does not drop). The public
`com.zegoggles.smssync.BACKUP` broadcast contract is owned by MU-005 and is **not** in
this unit's scope; this design must not alter it.

### Threading

Otto delivered cross-thread and relied on the immutable `State` payload for safety
(patterns.md Deep-Dive 1). `StateFlow`/`SharedFlow` are concurrency-safe and the payload
remains the immutable `State`, so the safety property is preserved by construction.
Collection happens on the main dispatcher inside `repeatOnLifecycle`; emission happens on
the worker's `Dispatchers.IO` context — no torn reads possible given immutable payloads.

---

## Design Validation

| Acceptance Criterion (REQ-MODERNIZATION-007) | How this design satisfies it | Verification |
|---|---|---|
| **AC-1** SyncStateRepository exposes sticky `StateFlow<SyncState>` + one-shot `SharedFlow<SyncEvent>`; sticky matches `@Produce` | `StateFlow.value` replays current state to new collectors = `@Produce` late-subscriber contract; `SharedFlow` (replay 0) for one-shot | Unit test: collector subscribing AFTER `emitState` immediately receives the last state (mirrors `produceLastState`); one-shot event NOT replayed to a late collector |
| **AC-2** Otto removed from all importing files + build; `App.bus` and static service back-refs deleted | Removal-scope table enumerates every verified site (all **12 distinct importing files** / 15 import lines, `App.java:59/116-134`, `SmsBackupService.java:66/311-313`, `SmsRestoreService.java:40/172-174`, build `:57`) | Gate keys on **12 distinct files**: `grep -rc com.squareup.otto app/src/main/java` → 0 matches across all 12 files (a gate keyed on 11 could pass with one file un-migrated); grep `App.bus`/`isServiceWorking`/`isServiceIdle` → 0; build resolves without otto |
| **AC-3** `MainActivity` uses `MainViewModel`, collects via `repeatOnLifecycle`; dialog logic → `Dialogs.java` | MainViewModel decision: ViewModel holds repository, Activity collects in `repeatOnLifecycle(STARTED)`, dialogs relocated to existing `Dialogs.java` | Inspect: no `@Subscribe` in MainActivity; collection inside `repeatOnLifecycle`; dialog branches in `Dialogs` |
| **AC-4** silent `IllegalArgumentException` swallow in `App.register()` eliminated, no equivalent silent failure | No register step exists in Flow; the masked failure class becomes compile-time impossible; `tryEmit` returns its result, `emitState` cannot silently fail | Inspect: `App.register`/`unregister` deleted; no `catch (IllegalArgumentException ignored)`; `tryEmitEvent` result is consumed/asserted |
| **AC-5** backup/restore status reaches UI with no regression (tests + UI smoke) | Sticky `StateFlow` preserves the status-row-on-subscribe behavior `@Produce` gave; `StatusPreference`/`MainActivity` collect the same state | Repository unit tests (sticky replay, one-shot non-replay, Cancel `Origin` USER/SYSTEM preserved) + UI smoke: start a backup, rotate device, status row still correct |

**Preserved-Core invariant (MU-000):** This design does **not** modify
`State`/`BackupState`/`RestoreState`/`SmsSyncState` behavior. The payload type stays the
existing immutable `State` hierarchy; the `State.java:32-33` k-9 magic-string line is
owned by MU-008, not touched here. The Kotlin `sealed interface` promotion is deferred
(Phase 3 / future MU-012).

**Note on line-number precision:** All `App.java`, `SmsBackupService.java`,
`SmsRestoreService.java`, `ServiceBase.java`, `StatusPreference.java`, `Dialogs.java`,
`State.java`/`BackupState.java`/`RestoreState.java`/`SmsSyncState.java`, `CancelEvent.java`,
`PerformAction.java`, `MissingPermissionsEvent.java`, and `app/build.gradle` line
references in this design were read in full this session and are exact. The
`MainActivity.java` `@Subscribe` handler lines and its `App.register(this)`/`App.unregister(this)`
sites in `onStart`/`onStop` (`:155`/`:160`) are grep-confirmed and read this session.

**Build-constraint check:** `warningsAsErrors true` + `-Werror -Xlint:deprecation`
(`app/build.gradle:41,73-77`) means removing the deprecated Otto usage *reduces*
deprecation debt — consistent with the project's "each step removes a deprecation"
fitness function (target-state.md Constraints). Kotlin/coroutines must already be on the
classpath (REQ-MODERNIZATION-001 / MU-001) — a hard prerequisite, recorded as a risk.

**Risks:**

| Risk | Mitigation |
|---|---|
| `@Produce` sticky semantics lost → status UI regresses on late subscribe | `StateFlow` is sticky by construction; AC-1/AC-5 tests assert late-collector replay explicitly |
| `CancelEvent` USER vs SYSTEM `Origin` distinction dropped (drives `mayInterruptIfRunning`) | `SyncEvent.Cancel(origin)` preserves the enum + `mayInterruptIfRunning()`; test asserts both branches end-to-end |
| DES-005/DES-008 not yet authored/approved — worker-publish and Hilt-injection surfaces are unanchored | Do not sprint-plan MU-006 until DES-005 is authored+approved and CNTR-MOD-007-001 is approved; recorded as hard predecessor |
| Deleting a POJO whose only consumer is Otto-reflection-dispatched | Step-3 gate: grep both `@Subscribe` consumers and `App.post(new <Event>` producers before any deletion |
| Hilt assumed present before MU-007 lands | Repository held as manual singleton in Steps 1–2; Hilt retrofit deferred to MU-007 (design constraint above) |
| Kotlin/coroutines not yet on classpath | Hard dependency on MU-001; do not start before it lands |

---

## Integration Contracts

> This design introduces a cross-component boundary: the **engine** (workers/services)
> produces sync state/events and the **presentation layer** (ViewModel/preferences/dialogs)
> consumes them, through the app-owned `SyncStateRepository`. DES-005's workers also
> produce into this same surface. The boundary therefore requires an approved CNTR-* before
> any story referencing this design is sprint-planned.

| Boundary | Producer | Consumer(s) | Contract Type | CNTR Artifact | Status |
|----------|----------|-------------|---------------|---------------|--------|
| SyncStateRepository state/event channel | Engine (`BackupWorker`/`RestoreWorker` per DES-005; pre-MU-005 the AsyncTasks) | `MainViewModel`, `StatusPreference`, `Dialogs`, `App` settings collector | service + event | CNTR-MOD-007-001 | needed |

The contract must fix: the `state: StateFlow<SyncState>` and `events: SharedFlow<SyncEvent>`
signatures; sticky vs one-shot semantics (replay/buffer config); the `emitState` /
`emitEvent` / `tryEmitEvent` publish surface and their threading/back-pressure guarantees;
the full `SyncEvent` sealed-variant set (including `Cancel.origin` USER/SYSTEM); and the
`SyncState` payload mapping to the existing `State` hierarchy. DES-005 consumes this same
contract for its worker-publish path, so it must be authored to serve both.

### Contracts Needed (pre-sprint gate)

- [ ] **CNTR-MOD-007-001** — SyncStateRepository service/event contract (state flow, event
  flow, publish surface, SyncEvent variant set, threading/back-pressure semantics).
  Run `/amp:create-contracts` and reach `approved` before any MU-006 story — or DES-005's
  worker-publish story — is sprint-planned.

## Trade-offs

- **StateFlow/SharedFlow over LiveData/EventBus** — composes/maps better, modern guidance,
  same primitive as worker progress (DES-005); LiveData soft-maintained; Greenrobot
  EventBus rejected (re-introduces the global-bus anti-pattern, adds a dependency).
- **Keep the Java `State` payload (defer Kotlin sealed promotion)** — protects the
  Preserved Core (MU-000), keeps this unit a pure eventing swap; the sealed-interface win
  is real but belongs to a later, behavior-preserving Phase-3 unit.
- **Facade-first (delegate to App.bus) before swapping to Flow** — buys a zero-behavior
  -change, independently shippable, reversible first increment at the cost of one throwaway
  delegating impl — the correct legacy-seam discipline (Feathers Ch. 25).

## Risks

See Design Validation §Risks. Top risk: starting before MU-001 (Kotlin/coroutines) or
MU-005 (WorkManager) lands — both are hard predecessors recorded in the requirement
Constraints and the MU-006 dependency edges.

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| REQ-MODERNIZATION-007 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-007-replace-otto-with-stateflow-repository.md | Governing requirement (5 ACs, constraints, boundary) |
| DES-MODERNIZATION-005 | sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md | Predecessor; worker→repository publish surface — **read this session: status draft, unwritten stub** |
| DES-MODERNIZATION-008 | sdlc/artifacts/design/modernization/DES-MODERNIZATION-008-hilt-di-design.md | Hilt injection of SyncStateRepository — **read this session: status draft, unwritten stub** |
| target-state.md (ADR-003) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/target-state.md | StateFlow-vs-Otto ADR; reactive-spine architecture |
| migration-units.md (MU-006) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/migration-units.md | Unit scope, POJO set, sequencing, shared-file allocation |
| patterns.md (ARCH-001/002, Deep-Dives 1/3) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/architecture/patterns.md | Otto singleton, @Produce sticky, immutable State, God-Activity |
| App.java | app/src/main/java/com/zegoggles/smssync/App.java | bus singleton (:59), register swallow (:116-130), post (:132) |
| SmsBackupService.java | app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java | static service (:66), @Produce (:202-204), isServiceWorking (:311-313), cross-service call (:101), App.post (:194) |
| SmsRestoreService.java | app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java | static service (:40), @Produce (:157-159), isServiceIdle (:172-174), App.post (:111) |
| ServiceBase.java | app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java | isWorking()/getState() basis for the static queries |
| State.java / BackupState.java / RestoreState.java / SmsSyncState.java | app/src/main/java/com/zegoggles/smssync/service/state/ | immutable Preserved-Core payload; k-9 magic string (State.java:32-33, MU-008-owned) |
| CancelEvent.java | app/src/main/java/com/zegoggles/smssync/service/CancelEvent.java | Origin USER/SYSTEM distinction driving mayInterruptIfRunning (must preserve) |
| PerformAction.java / MissingPermissionsEvent.java | app/src/main/java/com/zegoggles/smssync/activity/events/ | transient POJO payloads absorbed into SyncEvent |
| MainActivity.java | app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java | 499 LOC; App.register/unregister in onStart/onStop (:155/:160); 9 @Subscribe (:265-345); App.post (:283,:451) |
| StatusPreference.java / Dialogs.java | app/src/main/java/com/zegoggles/smssync/activity/ | UI subscribers; dialog relocation target |
| app/build.gradle | app/build.gradle | otto :57; SDK 29; warningsAsErrors :41; -Werror :73-77 |
| code-location.md | sdlc/artifacts/engagement/code-location.md | tech stack, build/test commands, Java-8 constraint |

---

*Draft — not finalized. Section content for system-architecture, integration-design, and design-validation authored against verified source; frontmatter (id/status/artifact_type/domain/related_requirements) left Artifact-Librarian-owned and unmodified.*
