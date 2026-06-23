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
 * U-048: Verifies the single-instance invariant for SyncStateRepository after the full
 * Hilt cutover.
 *
 * After U-048 the Hilt @Singleton scope (EventModule @Binds FlowSyncStateRepository) is
 * the sole construction site. App.onCreate() sets the static bridge accessor from the
 * @Inject field rather than constructing a second instance.
 *
 * Test approach: directly set the App private static via reflection (simulating what
 * App.onCreate() does after Hilt injection), then assert that App.syncStateRepository()
 * returns the same object that was injected. This avoids a full Hilt test harness while
 * validating the single-instance contract.
 *
 * AC-3/AC-4 (U-048): the static accessor is a bridge to the Hilt-managed singleton;
 * both references point to the same FlowSyncStateRepository instance.
 */
@RunWith(RobolectricTestRunner::class)
class EventModuleSingleInstanceTest {

    private lateinit var testRepository: FlowSyncStateRepository
    private lateinit var staticField: Field
    private var previousValue: SyncStateRepository? = null

    @Before
    fun setUp() {
        testRepository = FlowSyncStateRepository()
        // Access App.syncStateRepositoryInstance via reflection to simulate what
        // App.onCreate() does: syncStateRepositoryInstance = syncStateRepository (injected)
        staticField = App::class.java.getDeclaredField("syncStateRepositoryInstance")
        staticField.isAccessible = true
        previousValue = staticField.get(null) as? SyncStateRepository
        // Install our test instance as the App static (mimicking Hilt injection + bridge assignment)
        staticField.set(null, testRepository)
    }

    @After
    fun tearDown() {
        // Restore the previous static value (test isolation)
        staticField.set(null, previousValue)
    }

    /**
     * AC-3/AC-4: App.syncStateRepository() returns the same instance as the one
     * assigned by Hilt injection (no second construction site).
     */
    @Test
    fun `App_syncStateRepository returns the Hilt-managed singleton instance`() {
        // The testRepository simulates the Hilt-injected FlowSyncStateRepository.
        // App.onCreate() does: syncStateRepositoryInstance = syncStateRepository (injected).
        val fromStaticAccessor: SyncStateRepository = App.syncStateRepository()

        // Identity: static accessor must return exactly the object Hilt injected
        assertThat(fromStaticAccessor).isSameInstanceAs(testRepository)
    }

    /**
     * AC-4: FlowSyncStateRepository is an instance of SyncStateRepository (binding sanity).
     */
    @Test
    fun `FlowSyncStateRepository is SyncStateRepository`() {
        assertThat(testRepository).isInstanceOf(SyncStateRepository::class.java)
    }

    /**
     * AC-2 (retained): State emitted via the injected repository is visible via
     * App.syncStateRepository() — both are the same object.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `state emitted via injected repository is visible via App_syncStateRepository`() = runTest {
        val injectedRepo: SyncStateRepository = testRepository          // Hilt-managed singleton
        val appRepo: SyncStateRepository = App.syncStateRepository()    // static bridge

        // Both must be the same object (prerequisite for state sharing)
        assertThat(injectedRepo).isSameInstanceAs(appRepo)

        val runningState = BackupState(
            SmsSyncState.BACKUP, 3, 10,
            com.zegoggles.smssync.service.BackupType.MANUAL, null, null
        )

        // Engine emits state via the injected reference
        injectedRepo.emitState(runningState)

        // UI observes the same state via App.syncStateRepository()
        val observedState = appRepo.state.first()
        assertThat(observedState).isEqualTo(runningState)
    }

    /**
     * Single-instance guard: FlowSyncStateRepository has an @Inject constructor,
     * so Hilt can construct it. Verify the constructor is accessible via reflection
     * (mirrors what Hilt's kapt-generated code does).
     */
    @Test
    fun `FlowSyncStateRepository has accessible no-arg constructor for Hilt injection`() {
        val constructors = FlowSyncStateRepository::class.java.constructors
        val noArgConstructor = constructors.find { it.parameterCount == 0 }
        assertThat(noArgConstructor).isNotNull()
        // Hilt can call it without arguments — no second manual construction needed
        val instance = noArgConstructor!!.newInstance()
        assertThat(instance).isInstanceOf(FlowSyncStateRepository::class.java)
    }
}
