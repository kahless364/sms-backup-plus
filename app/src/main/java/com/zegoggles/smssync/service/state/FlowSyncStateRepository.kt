package com.zegoggles.smssync.service.state

import android.util.Log
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * U-020: Flow-backed SyncStateRepository. Replaces the Otto-delegating
 * DefaultSyncStateRepository introduced by U-019.
 *
 * AC-3:  MutableStateFlow<SyncState> + MutableSharedFlow<SyncEvent>(replay=0,
 *        extraBufferCapacity=1, DROP_OLDEST) as required by CNTR-MODERNIZATION-006.
 * AC-4:  StateFlow is sticky — late collectors immediately receive the latest value.
 * AC-5:  SharedFlow replay=0 — late collectors do NOT see prior events.
 * AC-17: tryEmitEvent() return value is logged when false, NOT silently discarded.
 *
 * U-048: @Inject constructor added so Hilt can construct this as a @Singleton without
 * any manual `new FlowSyncStateRepository()` call in modules or App. EventModule binds
 * SyncStateRepository -> FlowSyncStateRepository via @Binds.
 */
class FlowSyncStateRepository @Inject constructor() : SyncStateRepository {

    // Seed with BackupState() — the non-null, non-running INITIAL state.
    // CNTR-MODERNIZATION-006 Validation Rule 2.
    private val _state = MutableStateFlow<SyncState>(BackupState())

    override val state: StateFlow<SyncState> = _state.asStateFlow()

    // replay=0  → one-shot, non-sticky (AC-5, CNTR-MODERNIZATION-006)
    // extraBufferCapacity=1 → tryEmit from receivers/button-handlers succeeds under normal
    //                         conditions (AC-17, CNTR-MODERNIZATION-006 §Backing-field)
    // DROP_OLDEST → prefer losing a stale event over deadlocking the emit path
    private val _events = MutableSharedFlow<SyncEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val events: SharedFlow<SyncEvent> = _events.asSharedFlow()

    /**
     * Thread-safe, non-suspending state update.
     * MutableStateFlow.value assignment is thread-safe and cannot fail or suspend.
     * Must NOT be suspend — engine code calls this from non-coroutine contexts (AC-3c).
     */
    override fun emitState(newState: SyncState) {
        _state.value = newState
    }

    /**
     * Suspending emit for one-shot events from coroutine call sites.
     * Suspends until buffer capacity is available — back-pressure-safe (AC-3d).
     */
    override suspend fun emitEvent(event: SyncEvent) {
        _events.emit(event)
    }

    /**
     * Non-suspending tryEmit for one-shot events from non-coroutine call sites
     * (BroadcastReceivers, button handlers, App.onCreate).
     * Returns false if the buffer is full. Callers MUST NOT silently discard this
     * return value — log a warning at minimum (AC-17, CNTR-MODERNIZATION-006 rule 6).
     */
    override fun tryEmitEvent(event: SyncEvent): Boolean {
        val emitted = _events.tryEmit(event)
        if (!emitted) {
            Log.w(TAG, "tryEmitEvent: buffer full, event dropped: $event")
        }
        return emitted
    }

    companion object {
        private const val TAG = "FlowSyncStateRepo"
    }
}
