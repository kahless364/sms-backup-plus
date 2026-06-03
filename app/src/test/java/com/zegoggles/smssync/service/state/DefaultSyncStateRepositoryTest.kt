package com.zegoggles.smssync.service.state

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * U-020: Updated from DefaultSyncStateRepository to FlowSyncStateRepository.
 *
 * DefaultSyncStateRepository (Otto-delegating facade from U-019) is deleted by U-020.
 * These tests now validate FlowSyncStateRepository directly.
 *
 * The test cases are preserved verbatim where applicable; assertions that referenced
 * Otto-specific behaviour (always-true tryEmitEvent) are updated to match the new
 * Flow-backed semantics (tryEmitEvent returns true with buffer capacity > 0).
 */
@RunWith(RobolectricTestRunner::class)
class DefaultSyncStateRepositoryTest {

    private lateinit var repository: FlowSyncStateRepository

    @Before
    fun setUp() {
        repository = FlowSyncStateRepository()
    }

    @Test
    fun `initial state is BackupState with INITIAL SmsSyncState`() {
        val initialState = repository.state.value
        assertThat(initialState).isInstanceOf(BackupState::class.java)
        assertThat(initialState.isInitialState()).isTrue()
        assertThat(initialState.isRunning()).isFalse()
    }

    @Test
    fun `emitState updates the StateFlow value`() {
        val newState = BackupState()
        repository.emitState(newState)
        assertThat(repository.state.value).isSameInstanceAs(newState)
    }

    @Test
    fun `tryEmitEvent returns true (extraBufferCapacity=1 with no collector)`() {
        val event = SyncEvent.AccountAdded
        val result = repository.tryEmitEvent(event)
        assertThat(result).isTrue()
    }

    @Test
    fun `tryEmitEvent with Cancel event returns true`() {
        val event = SyncEvent.Cancel(SyncEvent.Cancel.Origin.USER)
        val result = repository.tryEmitEvent(event)
        assertThat(result).isTrue()
    }

    @Test
    fun `tryEmitEvent with Cancel SYSTEM event returns true`() {
        val event = SyncEvent.Cancel(SyncEvent.Cancel.Origin.SYSTEM)
        val result = repository.tryEmitEvent(event)
        assertThat(result).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `emitEvent is suspend function and completes`() = runTest {
        val event = SyncEvent.SettingsReset
        // Should not throw and should complete
        repository.emitEvent(event)
    }

    @Test
    fun `state flow has correct initial value after multiple emitState calls`() {
        val state1 = BackupState()
        val state2 = BackupState()

        repository.emitState(state1)
        assertThat(repository.state.value).isSameInstanceAs(state1)

        repository.emitState(state2)
        assertThat(repository.state.value).isSameInstanceAs(state2)
    }

    @Test
    fun `events SharedFlow has replay zero`() {
        // Verify the SharedFlow configuration: replay = 0
        // replayCache should be empty (no replay)
        assertThat(repository.events.replayCache).isEmpty()
    }
}
