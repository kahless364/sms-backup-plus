package com.zegoggles.smssync.service.state

/**
 * U-019: SyncState is a type alias for the existing immutable State hierarchy.
 * No new Kotlin sealed type is introduced in this unit (deferred to MU-012).
 * BackupState and RestoreState are the concrete carried payloads, unmodified
 * (Preserved Core, MU-000).
 *
 * CNTR-MODERNIZATION-006 §Type: SyncState
 */
typealias SyncState = State
