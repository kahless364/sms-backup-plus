package com.zegoggles.smssync.service.state

import com.google.common.truth.Truth.assertThat
import com.zegoggles.smssync.activity.AppPermission
import com.zegoggles.smssync.service.BackupType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Before
import org.junit.Test

/**
 * U-020: Unit tests for FlowSyncStateRepository.
 *
 * AC-4:  lateCollector_receivesLastEmittedState   — sticky StateFlow replay
 * AC-5:  lateCollector_doesNotReceivePriorEvent   — one-shot SharedFlow (replay=0)
 * AC-10: cancel_originUser_mayInterruptIfRunning_isFalse
 * AC-10: cancel_originSystem_mayInterruptIfRunning_isTrue
 * AC-16: tryEmitEvent_returnValue_isNotSwallowed
 * AC-3:  initialState_isBackupState               — idle initial state
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncStateRepositoryTest {

    private lateinit var repository: FlowSyncStateRepository

    @Before
    fun setUp() {
        repository = FlowSyncStateRepository()
    }

    // -----------------------------------------------------------------------
    // AC-4: Sticky last-state semantics (StateFlow.value reproduces @Produce)
    // -----------------------------------------------------------------------

    @Test
    fun lateCollector_receivesLastEmittedState() = runTest {
        val runningState = BackupState(SmsSyncState.BACKUP, 0, 10, BackupType.MANUAL, null, null)

        // Emit state BEFORE starting the collector
        repository.emitState(runningState)

        // Late collector via state.first() — should immediately see the emitted value
        val collected = repository.state.first()

        assertThat(collected).isEqualTo(runningState)
    }

    // -----------------------------------------------------------------------
    // AC-5: One-shot non-replay semantics (SharedFlow replay=0)
    // -----------------------------------------------------------------------

    @Test
    fun lateCollector_doesNotReceivePriorEvent() = runTest {
        val permissions: List<AppPermission> = emptyList()
        val event = SyncEvent.MissingPermissions(permissions)

        // Emit event BEFORE starting the collector
        repository.tryEmitEvent(event)

        // Late collector with a 100ms timeout — must NOT receive the prior event
        val received = withTimeoutOrNull(100) {
            repository.events.first()
        }

        assertThat(received).isNull()
    }

    // -----------------------------------------------------------------------
    // AC-10: Cancel.Origin semantics
    // -----------------------------------------------------------------------

    @Test
    fun cancel_originUser_mayInterruptIfRunning_isFalse() = runTest {
        val cancel = SyncEvent.Cancel(SyncEvent.Cancel.Origin.USER)
        assertThat(cancel.mayInterruptIfRunning()).isFalse()
    }

    @Test
    fun cancel_originSystem_mayInterruptIfRunning_isTrue() = runTest {
        val cancel = SyncEvent.Cancel(SyncEvent.Cancel.Origin.SYSTEM)
        assertThat(cancel.mayInterruptIfRunning()).isTrue()
    }

    @Test
    fun cancel_defaultOrigin_isUser() = runTest {
        val cancel = SyncEvent.Cancel()
        assertThat(cancel.origin).isEqualTo(SyncEvent.Cancel.Origin.USER)
        assertThat(cancel.mayInterruptIfRunning()).isFalse()
    }

    // -----------------------------------------------------------------------
    // AC-16: tryEmitEvent return value — not silently swallowed
    // -----------------------------------------------------------------------

    @Test
    fun tryEmitEvent_returnValue_isConsumedByCallerNotSwallowed() = runTest {
        // With no active collector, tryEmit with extraBufferCapacity=1 still succeeds
        val event = SyncEvent.Cancel(SyncEvent.Cancel.Origin.USER)
        val result = repository.tryEmitEvent(event)
        // With extraBufferCapacity=1 and no collector, first emit should succeed
        assertThat(result).isTrue()
    }

    // -----------------------------------------------------------------------
    // AC-3: Initial state is idle (BackupState with INITIAL SmsSyncState)
    // -----------------------------------------------------------------------

    @Test
    fun initialState_isBackupState_withIdleState() {
        val initialState = repository.state.value
        assertThat(initialState).isInstanceOf(BackupState::class.java)
        assertThat(initialState.state).isEqualTo(SmsSyncState.INITIAL)
    }

    // -----------------------------------------------------------------------
    // AC-3e: emitEvent (suspending) delivers event to active collector
    // -----------------------------------------------------------------------

    @Test
    fun emitEvent_deliversToActiveCollector() = runTest {
        val event = SyncEvent.AccountAdded
        var received: SyncEvent? = null

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.events.first { received = it; true }
        }

        repository.emitEvent(event)
        job.join()

        assertThat(received).isEqualTo(SyncEvent.AccountAdded)
    }
}
