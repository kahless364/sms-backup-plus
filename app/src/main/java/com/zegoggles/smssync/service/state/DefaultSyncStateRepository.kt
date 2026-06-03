package com.zegoggles.smssync.service.state

import android.util.Log
import com.zegoggles.smssync.App
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * U-019: Step 1 (Facade) implementation of SyncStateRepository.
 *
 * Delegates every emitState/emitEvent/tryEmitEvent call to App.bus (Otto) with
 * ZERO runtime behavior change. Consumers still register with Otto via @Subscribe.
 * The StateFlow/SharedFlow backing fields are stubs initialized to satisfy the
 * interface contract at the type level; they do not carry live values in this step
 * because no collector is wired up and emission still flows through Otto.
 *
 * The backing fields use the CNTR-MODERNIZATION-006 §Backing-field requirements
 * configuration so Step 2 (U-020) can activate them without changing field declarations:
 *   - state: MutableStateFlow seeded with BackupState() (non-null, non-running INITIAL)
 *   - events: MutableSharedFlow(replay=0, extraBufferCapacity>=1, onBufferOverflow=SUSPEND)
 *
 * DES-MODERNIZATION-007 §Migration path Step 1: facade is REVERSIBLE and has no
 * observable change. The swap to live Flow backing happens in U-020.
 *
 * Manual singleton (no Hilt): constructed in App.onCreate and exposed via
 * App.syncStateRepository(). Hilt injection deferred to U-022 (DES-MODERNIZATION-008).
 */
class DefaultSyncStateRepository : SyncStateRepository {

    companion object {
        private const val TAG = "DefaultSyncStateRepo"
    }

    // Backing StateFlow: seeded with BackupState() per CNTR-MODERNIZATION-006 Validation
    // Rule 2 (initial value must be non-null, non-running INITIAL state). Stub only in
    // Step 1 — not yet the live channel; Otto still owns dispatch.
    private val _state = MutableStateFlow<SyncState>(BackupState())

    // Backing SharedFlow: replay=0 (one-shot), extraBufferCapacity=1 (so a single
    // non-coroutine tryEmitEvent from a BroadcastReceiver succeeds without suspension),
    // onBufferOverflow=SUSPEND (tryEmit returns false on overflow, emitEvent suspends).
    // Configuration per CNTR-MODERNIZATION-006 §Backing-field requirements.
    // Stub only in Step 1 — not yet the live channel; Otto still owns dispatch.
    private val _events = MutableSharedFlow<SyncEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    override val state: StateFlow<SyncState> = _state.asStateFlow()

    override val events: SharedFlow<SyncEvent> = _events.asSharedFlow()

    /**
     * Engine publish surface for sync state.
     * Delegates to App.post(newState) — identical to the current App.post(state) call
     * path at SmsBackupService.java:194.
     *
     * Also updates the stub StateFlow value so that Step 2 (U-020) can activate the
     * Flow path without changing this method body.
     */
    override fun emitState(newState: SyncState) {
        _state.value = newState
        App.post(newState)
    }

    /**
     * Suspending publish for one-shot events from coroutine call sites.
     * Suspend modifier is present to satisfy the interface; the body calls App.post
     * synchronously with no actual suspension. The Kotlin compiler inserts a
     * Continuation parameter and returns Unit synchronously — correct for a
     * suspend function with no actual suspension point.
     *
     * Delegates to App.post(event).
     */
    override suspend fun emitEvent(event: SyncEvent) {
        App.post(event)
    }

    /**
     * Non-suspending publish for one-shot events from NON-coroutine call sites.
     * Otto's bus.post() is synchronous and does not signal back-pressure, so this
     * facade always returns true. When the Flow-backed implementation replaces this
     * in Step 2, tryEmitEvent will delegate to MutableSharedFlow.tryEmit() and may
     * return false. Callers MUST handle a false return (log a warning at minimum).
     *
     * Delegates to App.post(event) and returns true.
     */
    override fun tryEmitEvent(event: SyncEvent): Boolean {
        App.post(event)
        return true
    }
}
