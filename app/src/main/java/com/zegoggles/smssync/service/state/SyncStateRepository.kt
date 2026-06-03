package com.zegoggles.smssync.service.state

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * App-owned replacement for the Otto bus. Sole engine -> presentation channel for sync
 * state (sticky) and one-shot events. Application-scoped singleton.
 *
 * U-019: This is Step 1 (Facade) of the branch-by-abstraction migration described in
 * DES-MODERNIZATION-007. The default implementation delegates to the existing App.bus
 * (Otto) with zero runtime behavior change.
 *
 * CNTR-MODERNIZATION-006 §Interface: SyncStateRepository — all five signatures are
 * fixed by this contract. Deviation is a contract violation.
 */
interface SyncStateRepository {

    /**
     * Sticky current sync state. ALWAYS has a value; replays the latest value to every new
     * collector on subscription. Reproduces Otto's @Produce produceLastState() contract
     * (SmsBackupService:202-204, SmsRestoreService:157-159).
     *
     * Initial value MUST be a non-running INITIAL state (BackupState()).
     */
    val state: StateFlow<SyncState>

    /**
     * One-shot transient events. replay = 0 — events are NOT sticky and are NOT replayed
     * to late collectors. A collector receives only events emitted while actively collecting.
     */
    val events: SharedFlow<SyncEvent>

    /**
     * Engine publish surface for sync state. Non-suspending: backed by
     * MutableStateFlow.value assignment, which cannot fail or suspend.
     */
    fun emitState(newState: SyncState)

    /**
     * Suspending publish for one-shot events from coroutine call sites.
     * Suspends until buffer capacity is available — back-pressure-safe, no drop.
     */
    suspend fun emitEvent(event: SyncEvent)

    /**
     * Non-suspending publish for one-shot events from NON-coroutine call sites
     * (BroadcastReceivers, preference-view button handlers). Returns false if the
     * event was dropped due to no buffer capacity. The boolean MUST be observable
     * to the caller — it is NOT swallowed (CNTR-MODERNIZATION-006 §Validation Rules rule 6).
     */
    fun tryEmitEvent(event: SyncEvent): Boolean
}
