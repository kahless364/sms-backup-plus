---
status: approved
artifact_type: interface-contract
consumers: []
related_requirements: []
related_design_docs: []
related_stories: []
change_records: []
id: CNTR-MODERNIZATION-006
title: ''
domain: modernization
contract_type: ''
producer: ''
---

# CNTR-MODERNIZATION-006: SyncStateRepository — Sync State/Event Port (Otto Replacement)

## Overview

This contract fixes the interface boundary that replaces the abandoned Square Otto event
bus (`com.squareup:otto:1.3.8`, `app/build.gradle:57`) with an app-owned
`SyncStateRepository`. It is the **single channel for engine → presentation propagation**
of backup/restore sync state and one-shot UI events, per DES-MODERNIZATION-007 and
REQ-MODERNIZATION-007 AC-1.

The repository exposes two reactive read surfaces and a publish surface that map exactly
onto Otto's two observed usages:

- a sticky `StateFlow<SyncState>` reproducing Otto's `@Produce` last-value semantics
  (`SmsBackupService.produceLastState()` `:202-204`,
  `SmsRestoreService.produceLastState()` `:157-159`) via `StateFlow.value`; and
- a one-shot `SharedFlow<SyncEvent>` (`replay = 0`) absorbing the transient `@Subscribe`
  POJOs (`CancelEvent`, `PerformAction`, `MissingPermissionsEvent`, and the account/
  settings/theme/auth events).

This contract is **flagged as CNTR-MOD-007-001 in DES-MODERNIZATION-007** (Integration
Contracts table, line 393) and is the pre-sprint gate for any MU-006 story and for
DES-005's worker-publish story. It is binding on both producer and consumer sides.

## Contract Boundary

| Side | Role | Components (verified against source / design) |
|------|------|-----------------------------------------------|
| **Producer** | Publishes sync state and events into the repository | The backup/restore **engine**: `BackupWorker`/`RestoreWorker` (`CoroutineWorker`, DES-MODERNIZATION-005) once MU-005 lands; pre-MU-005 the `SmsBackupService`/`SmsRestoreService` (and their `BackupTask`/`RestoreTask`) routing through the Step-1 facade. Receivers (`App.onCreate`, `BackupBroadcastReceiver`, `SmsBroadcastReceiver`) publish one-shot events from non-coroutine contexts. |
| **Consumer** | Collects state/events lifecycle-aware | The **presentation layer**: `MainViewModel` (`@HiltViewModel`), `StatusPreference`, `Dialogs`, and the `App`-relocated settings collector. Injected via Hilt per DES-MODERNIZATION-008 (`@Singleton`); held as a manual singleton in migration Steps 1–2 until MU-007 lands. |

The repository is **application-scoped (`@Singleton`)**. There is exactly one instance per
process, and it is the sole source of truth for "what is the current sync state" — replacing
the `App.bus` static, the `SmsBackupService.service`/`SmsRestoreService.service`
back-references, and the `isServiceWorking()`/`isServiceIdle()` static queries
(DES-MODERNIZATION-007 Removal scope).

> **Preserved-Core boundary (MU-000):** This contract **references** the existing immutable
> `State`/`BackupState`/`RestoreState`/`SmsSyncState` types as the carried payload. It does
> **not** redefine, modify, or promote them. The Kotlin `sealed interface` promotion of the
> state hierarchy is explicitly deferred (future MU-012, Phase 3) and is out of scope.

## Contract Definition

### Interface: SyncStateRepository

```kotlin
package com.zegoggles.smssync.service.state   // package fixed at implementation time; see Notes

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * App-owned replacement for the Otto bus. Sole engine -> presentation channel for sync
 * state (sticky) and one-shot events. Application-scoped singleton.
 */
interface SyncStateRepository {

    /**
     * Sticky current sync state. ALWAYS has a value; replays the latest value to every new
     * collector on subscription. This reproduces Otto's @Produce produceLastState() contract
     * (SmsBackupService:202-204, SmsRestoreService:157-159): a collector that subscribes
     * AFTER a state was emitted immediately receives that last state.
     *
     * Initial value MUST be a non-running INITIAL state (see Validation Rules).
     */
    val state: StateFlow<SyncState>

    /**
     * One-shot transient events. replay = 0 — events are NOT sticky and are NOT replayed to
     * late collectors (re-delivering "permissions missing" on every rotation is a defect,
     * DES-007). A collector receives only events emitted while it is actively collecting.
     */
    val events: SharedFlow<SyncEvent>

    /**
     * Engine publish surface for sync state. Non-suspending: backed by
     * MutableStateFlow.value assignment, which cannot fail or suspend. Conflates per
     * StateFlow semantics (a value equal to the current value is not re-emitted; see
     * Validation Rules on equality).
     */
    fun emitState(newState: SyncState)

    /**
     * Suspending publish for one-shot events from coroutine call sites (workers, ViewModel
     * scopes). Suspends until buffer capacity is available — back-pressure-safe, no drop.
     */
    suspend fun emitEvent(event: SyncEvent)

    /**
     * Non-suspending publish for one-shot events from NON-coroutine call sites
     * (BroadcastReceivers, preference-view button handlers). Returns false if the event was
     * dropped due to no buffer capacity. The boolean MUST be observable to the caller — it
     * is NOT swallowed (this is the structural fix for the App.register() silent-swallow,
     * REQ-MODERNIZATION-007 AC-4).
     */
    fun tryEmitEvent(event: SyncEvent): Boolean
}
```

**Backing-field requirements (binding on the implementation):**

| Surface | Backing primitive | Required configuration |
|---------|-------------------|------------------------|
| `state` | `MutableStateFlow<SyncState>` | Constructed with a non-null `INITIAL` seed value (see Validation Rules). Exposed read-only via `.asStateFlow()`. |
| `events` | `MutableSharedFlow<SyncEvent>` | `replay = 0`; `extraBufferCapacity >= 1`; `onBufferOverflow = BufferOverflow.SUSPEND`. `extraBufferCapacity >= 1` is what makes `tryEmitEvent` succeed for a single in-flight one-shot from a receiver rather than always returning false. Exposed read-only via `.asSharedFlow()`. |

### Type: SyncState (payload — wraps the Preserved Core)

For this migration unit `SyncState` is a **type alias / supertype for the existing
immutable `State` hierarchy** — it carries `BackupState` and `RestoreState` unchanged. The
contract does not introduce new state semantics; it carries the existing payload across the
new channel.

```kotlin
// MU-006 binding form: SyncState IS the existing abstract State.
// No new Kotlin sealed type is introduced in this unit (that is deferred to MU-012).
typealias SyncState = com.zegoggles.smssync.service.state.State
// concrete payloads carried, unmodified (Preserved Core, MU-000):
//   com.zegoggles.smssync.service.state.BackupState
//   com.zegoggles.smssync.service.state.RestoreState
//   keyed by com.zegoggles.smssync.service.state.SmsSyncState (11 constants)
```

Carried-payload facts (verified against source this session — referenced, NOT redefined):

```
abstract State              // State.java:18-27
  final SmsSyncState state          // INITIAL|CALC|LOGIN|BACKUP|RESTORE|ERROR|
                                    // CANCELED_BACKUP|CANCELED_RESTORE|
                                    // FINISHED_BACKUP|FINISHED_RESTORE|UPDATING_THREADS
  final Exception  exception
  final @Nullable DataType dataType
  boolean isRunning()   // true for LOGIN|CALC|BACKUP|RESTORE|UPDATING_THREADS  (State.java:63-70)
  boolean isInitialState() / isFinished() / isError() / isCanceled() / isAuthException() /
          isPermissionException() / isConnectivityError()
  State transition(SmsSyncState, Exception)   // returns a NEW instance (immutable)

class BackupState extends State          // BackupState.java
  final BackupType backupType
  final int currentSyncedItems, itemsToSync
  BackupState()  // == (INITIAL, 0, 0, UNKNOWN, null, null)

class RestoreState extends State         // RestoreState.java
  final int currentRestoredCount, itemsToRestore, actualRestoredCount, duplicateCount
  RestoreState()  // == (INITIAL, 0, 0, 0, 0, null, null)
```

> The "is a sync running?" query that `isServiceWorking()`/`isServiceIdle()` answered today
> becomes `repository.state.value.isRunning()` — a read of this single source of truth.

### Type: SyncEvent (sealed hierarchy — one-shot transient events)

`SyncEvent` is a **new Kotlin `sealed class`** that absorbs the transient Otto POJOs. Each
variant maps 1:1 to a current event so no consumer behavior is lost. Member signatures are
fixed by this contract.

```kotlin
package com.zegoggles.smssync.service.state   // package fixed at implementation time; see Notes

import com.zegoggles.smssync.activity.AppPermission
import com.zegoggles.smssync.activity.events.PerformAction       // for the Actions enum
import com.zegoggles.smssync.service.OAuth2CallbackTask.OAuth2Callback // payload TBD at impl, see Notes

sealed class SyncEvent {

    /**
     * Replaces service/CancelEvent.java (CancelEvent.java:5-19).
     * The USER vs SYSTEM distinction is LOAD-BEARING: it drives mayInterruptIfRunning(),
     * which returns true ONLY for SYSTEM. This is NOT a trivial POJO — the Origin enum and
     * the mayInterruptIfRunning() semantics MUST be preserved verbatim.
     * Default origin is USER (matches CancelEvent()'s no-arg ctor, CancelEvent.java:9-11).
     * NOTE: the source enum is package-private; the migrated Origin is promoted to public
     * (public-API change required for the sealed-class member; documented in Notes).
     */
    data class Cancel(val origin: Origin = Origin.USER) : SyncEvent() {
        enum class Origin { USER, SYSTEM }
        fun mayInterruptIfRunning(): Boolean = origin == Origin.SYSTEM
    }

    /**
     * Replaces activity/events/PerformAction.java (PerformAction.java:3-17).
     * Both the Actions enum {Backup, BackupSkip, Restore} AND the `confirm` flag are
     * preserved verbatim — `confirm` gates the confirmation dialog in
     * Dialogs.performAction (Dialogs.java:331-343).
     */
    data class PerformActionRequested(
        val action: PerformAction.Actions,   // Backup | BackupSkip | Restore
        val confirm: Boolean
    ) : SyncEvent()

    /**
     * Replaces activity/events/MissingPermissionsEvent.java (MissingPermissionsEvent.java:7-12).
     * Payload is List<AppPermission> (NOT List<String>). MUST remain one-shot (non-sticky):
     * carried on `events`, never on `state`.
     */
    data class MissingPermissions(val permissions: List<AppPermission>) : SyncEvent()

    /**
     * Replaces OAuth2CallbackTask.OAuth2CallbackEvent (subscribed in MainActivity/Dialogs).
     * Exact payload shape carried verbatim from the existing event; see Notes —
     * implementer MUST confirm the source POJO's fields before fixing this signature.
     */
    data class OAuth2Callback(val payload: com.zegoggles.smssync.service.OAuth2CallbackTask.OAuth2Callback) : SyncEvent()

    // --- Account / settings / theme events absorbed from the transient Otto POJOs.
    // Each maps 1:1 to an existing activity/events/* POJO. Payload-less members are
    // `object`; payload-carrying members are `data class`. The implementer MUST grep each
    // POJO for @Subscribe consumers AND App.post(new <Event>...) producers and confirm a
    // migrated counterpart before deleting it (DES-007 verification gap, line 147-151).
    object AccountAdded : SyncEvent()
    object AccountRemoved : SyncEvent()
    object AccountConnectionChanged : SyncEvent()
    object AutoBackupSettingsChanged : SyncEvent()
    object FallbackAuth : SyncEvent()
    object SettingsReset : SyncEvent()
    data class ThemeChanged(val themeId: Int) : SyncEvent()   // payload TBD at impl; see Notes
}
```

**SyncEvent variant traceability (verified against source POJOs this session):**

| SyncEvent member | Source POJO (verified path / lines) | Preserved semantics |
|------------------|--------------------------------------|---------------------|
| `Cancel(origin)` + `mayInterruptIfRunning()` | `service/CancelEvent.java:5-19` | `Origin{USER,SYSTEM}`, default USER, `mayInterruptIfRunning() == origin==SYSTEM` |
| `PerformActionRequested(action, confirm)` | `activity/events/PerformAction.java:3-17` | `Actions{Backup,BackupSkip,Restore}` + `confirm` flag verbatim |
| `MissingPermissions(permissions)` | `activity/events/MissingPermissionsEvent.java:7-12` | `List<AppPermission>` payload; one-shot only |
| `OAuth2Callback(payload)` | `OAuth2CallbackTask.OAuth2CallbackEvent` (consumed MainActivity/Dialogs) | payload verbatim — confirm at impl |
| `AccountAdded` / `AccountRemoved` / `AccountConnectionChanged` / `AutoBackupSettingsChanged` / `FallbackAuth` / `SettingsReset` / `ThemeChanged` | `activity/events/*` (9 files) | absorbed; grep-verify consumer + producer before deletion |

## Versioning

- **Current version:** v1
- **Breaking change policy.** The following are breaking and require a new contract version
  and coordinated producer+consumer migration:
  - Changing `state` from `StateFlow<SyncState>` to a non-sticky flow (would silently drop
    the late-subscriber replay AC-1/AC-5 depend on).
  - Changing `events` to `replay > 0` (would make one-shot events sticky — re-delivering
    `MissingPermissions`/`Cancel` on rotation; an AC-defined defect).
  - Adding, removing, renaming, or re-typing any `SyncEvent` member, or removing
    `Cancel.Origin` / `Cancel.mayInterruptIfRunning()`.
  - Altering the carried `State`/`BackupState`/`RestoreState` payload shape (owned by MU-000;
    forbidden here).
  - Changing the threading/back-pressure guarantee of any publish method (e.g. making
    `emitState` suspend, or making `tryEmitEvent` swallow its boolean).
- **Backward-compatible (non-breaking).** Adding a new `SyncEvent` member is **source-breaking
  for exhaustive `when`** in Kotlin, so it is treated as a minor-version change requiring
  consumers to add a branch but not a contract re-version; producers may begin emitting it
  only once all consumers handle it. Increasing `extraBufferCapacity`, or swapping the
  concrete backing implementation (e.g. facade → Flow per migration Step 2) while preserving
  every signature and semantic above, is fully backward-compatible.

## Validation Rules

Both sides MUST honor these:

1. **Sticky-replay invariant (AC-1).** `state` is a `StateFlow`; it always has a value and
   replays the latest to every new collector. A collector subscribing *after* `emitState(s)`
   MUST receive `s` as its first emission, with no producer re-emit. This is the binding
   reproduction of Otto's `@Produce` contract.
2. **Initial value.** `state` MUST be seeded with a non-null, non-running `INITIAL` state
   (`new BackupState()` or `new RestoreState()`, both `INITIAL`) so the first collector
   before any engine activity sees a coherent idle state and `state.value.isRunning()` is
   `false`.
3. **One-shot invariant (AC-1).** `events` MUST have `replay = 0`. `Cancel`,
   `PerformActionRequested`, `MissingPermissions`, and auth/account/settings events MUST be
   emitted on `events`, NEVER on `state`. A late collector MUST NOT receive a previously
   emitted event.
4. **Payload immutability / threading.** Carried `State` payloads are immutable
   (Preserved Core). Emission may occur on `Dispatchers.IO` (worker context); collection
   occurs on the main dispatcher inside `repeatOnLifecycle(STARTED)`. No torn reads are
   possible given immutable payloads — both sides rely on this and MUST NOT mutate a carried
   `State` after emission.
5. **Conflation note for `state`.** `StateFlow` does not re-emit a value `equals` to the
   current one. `BackupState`/`RestoreState` do not override `equals`, so distinct instances
   are reference-unequal and every `emitState` of a new instance propagates — which is the
   intended progress-update behavior. Implementers MUST NOT add `equals` to the `State`
   classes (would re-enter Preserved-Core scope and could conflate progress ticks).
6. **No silent failure (AC-4).** There is no register step to misconfigure. `tryEmitEvent`
   MUST return its boolean to the caller (drop is visible, not logged-and-swallowed);
   `emitState` MUST NOT catch-and-suppress. No `catch (IllegalArgumentException ignored)`
   equivalent may be reintroduced.
7. **`Cancel.Origin` is load-bearing.** Producers cancelling for system reasons (low
   resources, OS) MUST emit `Cancel(Origin.SYSTEM)`; user-initiated cancels emit
   `Cancel(Origin.USER)` (or the default). Consumers driving interruption MUST branch on
   `mayInterruptIfRunning()`, not on identity.

## Error Handling

- **Errors are state, not exceptions across the boundary.** Engine failures are conveyed as
  a `State` with `state == ERROR` and a non-null `exception` (see `State.isError()`,
  `State.getErrorMessage()`), emitted via `emitState`. The boundary does NOT throw across
  itself; consumers render the error from the carried `State`.
- **Back-pressure.** `emitEvent` (suspending) never drops — it suspends until buffer
  capacity frees. `tryEmitEvent` (non-suspending) returns `false` on drop; non-coroutine
  callers MUST handle `false` (e.g. log a warning, retry, or escalate) rather than ignore it.
  With `extraBufferCapacity >= 1` a single one-shot from a receiver succeeds.
- **No cross-boundary checked exceptions.** All publish methods are total (cannot throw for
  ordinary use). `emitState` cannot fail (a `MutableStateFlow.value` set). This is the
  structural elimination of the `App.register()` silent-`IllegalArgumentException` swallow.

## Example Payloads

**Producer — engine publishing sticky state (worker / service, on Dispatchers.IO):**

```kotlin
// Backup progress tick (engine side, DES-005 BackupWorker or pre-MU-005 SmsBackupService):
val next: SyncState = current.transition(SmsSyncState.BACKUP, /* exception = */ null)
repository.emitState(next)        // non-suspending; replaces App.post(state) at SmsBackupService:194

// Error path (replaces SmsRestoreService.postError -> App.post, :111):
repository.emitState(current.transition(SmsSyncState.ERROR, messagingException))

// One-shot event from a coroutine context (suspending, back-pressure-safe):
repository.emitEvent(SyncEvent.MissingPermissions(missingAppPermissions))

// One-shot from a NON-coroutine context (BroadcastReceiver / preference button handler):
val delivered = repository.tryEmitEvent(SyncEvent.Cancel(SyncEvent.Cancel.Origin.USER))
if (!delivered) Log.w(TAG, "cancel event dropped — no buffer capacity")  // AC-4: visible, not swallowed
```

**Consumer — presentation collecting lifecycle-aware (replaces App.register/unregister):**

```kotlin
// In MainActivity.onCreate (replaces App.register(this) onStart:155 / unregister onStop:160):
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
            // sticky: on (re)subscribe this immediately receives the LAST state
            // == the @Produce produceLastState() contract.
            viewModel.state.collect { s: SyncState ->
                statusRow.render(s)                 // s.isRunning(), s.getNotificationLabel(resources), ...
            }
        }
        launch {
            // one-shot: only events emitted while STARTED are received; not replayed on rotation.
            viewModel.events.collect { event: SyncEvent ->
                when (event) {
                    is SyncEvent.MissingPermissions -> dialogs.requestPermissions(event.permissions)
                    is SyncEvent.Cancel             -> if (event.mayInterruptIfRunning()) engine.interrupt()
                    is SyncEvent.PerformActionRequested ->
                        dialogs.performAction(event.action, event.confirm)   // confirm gates the dialog
                    is SyncEvent.OAuth2Callback     -> dialogs.onOAuth2Callback(event.payload)
                    SyncEvent.FallbackAuth          -> dialogs.handleFallbackAuth()
                    is SyncEvent.ThemeChanged       -> recreate()
                    SyncEvent.AccountAdded,
                    SyncEvent.AccountRemoved,
                    SyncEvent.AccountConnectionChanged,
                    SyncEvent.AutoBackupSettingsChanged,
                    SyncEvent.SettingsReset         -> viewModel.onConfigChanged()
                }   // exhaustive `when` — adding a member forces every consumer to handle it (compile-time)
            }
        }
    }
}

// "Is a sync running?" — replaces isServiceWorking()/isServiceIdle() static queries:
val busy: Boolean = viewModel.state.value.isRunning()
```

## Dependencies

- **DES-MODERNIZATION-007** — the owning design (this contract realizes CNTR-MOD-007-001).
- **REQ-MODERNIZATION-007** — governing requirement (AC-1 sticky/one-shot; AC-4 no silent
  failure; AC-5 no status regression).
- **Preserved Core (MU-000)** — `State`/`BackupState`/`RestoreState`/`SmsSyncState` carried
  unchanged; this contract depends on their current shape and MUST NOT alter it.
- **REQ-MODERNIZATION-001 / MU-001** — Kotlin + `kotlinx.coroutines` on the classpath
  (`StateFlow`/`SharedFlow`). Hard prerequisite; not yet present.
- **DES-MODERNIZATION-005 / MU-005 (currently draft, unwritten)** — the
  `BackupWorker`/`RestoreWorker` producer that consumes this same contract for its
  worker-publish path. This contract is authored to serve both the pre-MU-005 service
  facade and the post-MU-005 workers.
- **DES-MODERNIZATION-008 / MU-007 (currently draft, unwritten)** — Hilt `@Singleton`
  provision of `SyncStateRepository`. Steps 1–2 MUST NOT assume Hilt is present (held as a
  manual singleton until MU-007 lands).

## Notes

Implementation-time confirmations the implementer MUST resolve before fixing the affected
signatures — flagged but not invented here: (a) the **`OAuth2Callback` payload shape** —
read `OAuth2CallbackTask.OAuth2CallbackEvent` and carry its fields verbatim; (b) the
**`ThemeChanged` payload** — confirm whether the source theme event carries an id/enum or is
payload-less and adjust the member accordingly; (c) the **final Kotlin package** for
`SyncStateRepository`/`SyncEvent` (shown as `service.state` here) — align with the DES-008
Hilt module location at implementation; (d) `Cancel.Origin` is **promoted from package-private
to public** to be a usable sealed-class member — a deliberate, minimal public-API surface
change. Per DES-007 (lines 147-151), grep each `activity/events/*` POJO for both `@Subscribe`
consumers and `App.post(new <Event>...)` producers and confirm a migrated counterpart before
deleting it. Carried-payload facts (`State`/`BackupState`/`RestoreState`/`SmsSyncState`,
`CancelEvent`, `PerformAction`, `MissingPermissionsEvent`) were read in full this session and
are exact.

*Draft — not finalized. Body authored against verified source and DES-MODERNIZATION-007;
frontmatter left Artifact-Librarian-owned and unmodified.*
