package com.zegoggles.smssync.activity

import com.google.common.truth.Truth.assertThat
import com.zegoggles.smssync.preferences.SecretStore
import com.zegoggles.smssync.scheduler.BackupScheduler
import com.zegoggles.smssync.service.state.BackupState
import com.zegoggles.smssync.service.state.FlowSyncStateRepository
import com.zegoggles.smssync.service.state.RestoreState
import com.zegoggles.smssync.service.state.SmsSyncState
import com.zegoggles.smssync.service.state.SyncEvent
import com.zegoggles.smssync.service.state.SyncStateRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * U-021: Unit tests for MainViewModel.
 *
 * AC-3: MainViewModel exposes state: StateFlow<SyncState> and events: SharedFlow<SyncEvent>
 *       delegating to the repository — verified by asserting that values emitted to the
 *       repository are immediately visible through the ViewModel's flows.
 * AC-9: New tests added per the story requirement (AC-9 bullet 1).
 *
 * U-049: Updated to pass Context and BackupScheduler args (new constructor parameters).
 * Uses RobolectricTestRunner for the ApplicationContext; BackupScheduler is mocked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    @Mock private lateinit var mockScheduler: BackupScheduler
    @Mock private lateinit var mockSecretStore: SecretStore

    private lateinit var repository: FlowSyncStateRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = FlowSyncStateRepository()
        // Default: encryption not degraded (happy path — does not change existing test behaviour)
        `when`(mockSecretStore.isEncryptionDegraded()).thenReturn(false)
        viewModel = MainViewModel(RuntimeEnvironment.getApplication(), repository, mockScheduler, mockSecretStore)
    }

    // -----------------------------------------------------------------------
    // AC-3: state delegates to repository.state
    // -----------------------------------------------------------------------

    @Test
    fun `state initial value delegates to repository initial state`() {
        // SyncStateRepository seeds with BackupState() — the non-running INITIAL state.
        val vmState = viewModel.state.value
        val repoState = repository.state.value
        assertThat(vmState).isSameInstanceAs(repoState)
    }

    @Test
    fun `state reflects repository state after emitState`() {
        val newState = BackupState()
        repository.emitState(newState)

        assertThat(viewModel.state.value).isSameInstanceAs(newState)
    }

    @Test
    fun `state is the same StateFlow instance as repository state`() {
        // IC-2: the ViewModel exposes the repository's StateFlow directly (no copy).
        assertThat(viewModel.state).isSameInstanceAs(repository.state)
    }

    @Test
    fun `state collects emitted BackupState from repository`() = runTest {
        val newState = BackupState()
        repository.emitState(newState)

        val collected = viewModel.state.first()
        assertThat(collected).isSameInstanceAs(newState)
    }

    // -----------------------------------------------------------------------
    // AC-3: events delegates to repository.events
    // -----------------------------------------------------------------------

    @Test
    fun `events is the same SharedFlow instance as repository events`() {
        // IC-2: the ViewModel exposes the repository's SharedFlow directly (no copy).
        assertThat(viewModel.events).isSameInstanceAs(repository.events)
    }

    @Test
    fun `events collector receives event emitted to repository`() = runTest {
        var received: SyncEvent? = null
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { received = it }
        }

        val event = SyncEvent.AccountAdded
        repository.tryEmitEvent(event)

        // Allow the coroutine to process the emission
        testScheduler.advanceUntilIdle()

        assertThat(received).isSameInstanceAs(SyncEvent.AccountAdded)
        job.cancel()
    }

    @Test
    fun `events collector receives Cancel event with Origin USER`() = runTest {
        var received: SyncEvent? = null
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { received = it }
        }

        val event = SyncEvent.Cancel(SyncEvent.Cancel.Origin.USER)
        repository.tryEmitEvent(event)
        testScheduler.advanceUntilIdle()

        assertThat(received).isInstanceOf(SyncEvent.Cancel::class.java)
        assertThat((received as SyncEvent.Cancel).origin).isEqualTo(SyncEvent.Cancel.Origin.USER)
        job.cancel()
    }

    // -----------------------------------------------------------------------
    // AC-3: tryEmitEvent delegates to repository.tryEmitEvent and returns boolean
    // -----------------------------------------------------------------------

    @Test
    fun `tryEmitEvent delegates to repository and returns true when buffer available`() {
        val event = SyncEvent.ThemeChanged
        val result = viewModel.tryEmitEvent(event)
        // With extraBufferCapacity=1 and no active collector, first emit should succeed.
        assertThat(result).isTrue()
    }

    @Test
    fun `tryEmitEvent return value is not silently discarded`() {
        // Verifies AC-9 no-silent-failure rule: the Boolean is returned, not swallowed.
        val event = SyncEvent.Cancel(SyncEvent.Cancel.Origin.USER)
        val result: Boolean = viewModel.tryEmitEvent(event)
        // Result is observable by the caller (it is returned and assigned here).
        assertThat(result).isTrue()
    }

    // -----------------------------------------------------------------------
    // BUG-005: onCleared() cancels the viewModelScope (no observer leak)
    // -----------------------------------------------------------------------

    @Test
    fun `onCleared cancels viewModelScope preventing observer leak (BUG-005)`() {
        // viewModelScope must be active before clear.
        assertThat(viewModel.viewModelScope.isActive).isTrue()

        // Simulate ViewModel destruction. ViewModel.clear() is the package-private method
        // that ViewModelStore calls to tear down each ViewModel; it calls onCleared() and
        // cancels the viewModelScope. Call via reflection to avoid access restrictions.
        val clearMethod = androidx.lifecycle.ViewModel::class.java.getDeclaredMethod("clear")
        clearMethod.isAccessible = true
        clearMethod.invoke(viewModel)

        // After clear(), viewModelScope is cancelled — all background coroutines
        // (backup/restore observers, cancel collectors) are terminated.
        // This is the BUG-005 no-leak guarantee.
        assertThat(viewModel.viewModelScope.isActive).isFalse()
    }
}
