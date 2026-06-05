package com.zegoggles.smssync.di

import com.google.common.truth.Truth.assertThat
import com.zegoggles.smssync.App
import com.zegoggles.smssync.service.state.BackupState
import com.zegoggles.smssync.service.state.FlowSyncStateRepository
import com.zegoggles.smssync.service.state.SmsSyncState
import com.zegoggles.smssync.service.state.SyncStateRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Field

/**
 * U-036 (BUG-004): Verifies that EventModule.provideSyncStateRepository() returns the
 * SAME instance as App.syncStateRepository() so engine and Hilt consumers share one
 * SyncStateRepository.
 *
 * AC-1: Identity assertion — App.syncStateRepository() === EventModule.provideSyncStateRepository().
 * AC-2: Emission test — a state emitted via App.syncStateRepository() is observed via the
 *        repository returned by EventModule.
 *
 * Test approach: directly set the App private static via reflection (simulating what
 * App.onCreate() does), call EventModule.provideSyncStateRepository(), assert identity.
 * This avoids the full Hilt test harness while validating the wiring contract.
 */
@RunWith(RobolectricTestRunner::class)
class EventModuleSingleInstanceTest {

    private lateinit var testRepository: FlowSyncStateRepository
    private lateinit var staticField: Field
    private var previousValue: SyncStateRepository? = null

    @Before
    fun setUp() {
        testRepository = FlowSyncStateRepository()
        // Access App.syncStateRepositoryInstance via reflection to simulate App.onCreate()
        staticField = App::class.java.getDeclaredField("syncStateRepositoryInstance")
        staticField.isAccessible = true
        previousValue = staticField.get(null) as? SyncStateRepository
        // Install our test instance as the App static
        staticField.set(null, testRepository)
    }

    @After
    fun tearDown() {
        // Restore the previous static value (test isolation)
        staticField.set(null, previousValue)
    }

    /**
     * AC-1: EventModule.provideSyncStateRepository() returns the App static instance.
     * Same object reference — no second FlowSyncStateRepository is constructed.
     */
    @Test
    fun `provideSyncStateRepository returns same instance as App_syncStateRepository`() {
        val fromEventModule: SyncStateRepository = EventModule.provideSyncStateRepository()
        val fromApp: SyncStateRepository = App.syncStateRepository()

        // Identity: same object, not just equals
        assertThat(fromEventModule).isSameInstanceAs(fromApp)
    }

    /**
     * AC-2: State emitted via App.syncStateRepository() is immediately visible via the
     * Hilt-provided repository (the same object). This validates that engine emission
     * reaches the UI's MainViewModel collector.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `state emitted via App_syncStateRepository is observed via EventModule instance`() = runTest {
        val engineRepo: SyncStateRepository = App.syncStateRepository()
        val hiltRepo: SyncStateRepository = EventModule.provideSyncStateRepository()

        val runningState = BackupState(
            SmsSyncState.BACKUP, 3, 10,
            com.zegoggles.smssync.service.BackupType.MANUAL, null, null
        )

        // Engine emits state
        engineRepo.emitState(runningState)

        // Hilt consumer observes the same state (StateFlow.value is shared)
        val observedState = hiltRepo.state.first()
        assertThat(observedState).isEqualTo(runningState)
    }

    /**
     * Additional guard: EventModule constructs no second instance — calling
     * provideSyncStateRepository() twice returns the same object.
     */
    @Test
    fun `provideSyncStateRepository called twice returns same instance`() {
        val first: SyncStateRepository = EventModule.provideSyncStateRepository()
        val second: SyncStateRepository = EventModule.provideSyncStateRepository()
        assertThat(first).isSameInstanceAs(second)
    }
}
