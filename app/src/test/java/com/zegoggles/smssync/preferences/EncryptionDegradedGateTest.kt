package com.zegoggles.smssync.preferences

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.zegoggles.smssync.activity.MainViewModel
import com.zegoggles.smssync.scheduler.WorkManagerScheduler
import com.zegoggles.smssync.service.BackupType
import com.zegoggles.smssync.service.BackupWorker
import com.zegoggles.smssync.service.exception.EncryptionDegradedException
import com.zegoggles.smssync.service.state.BackupState
import com.zegoggles.smssync.service.state.FlowSyncStateRepository
import com.zegoggles.smssync.service.state.RestoreState
import com.zegoggles.smssync.service.state.SmsSyncState
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.Executors

/**
 * U-054 (SE-002, AC-3): Tests that the ENCRYPTION_DEGRADED flag gates backup and restore
 * initiation, that no WorkManager job is enqueued when degraded, and that the appropriate
 * error state carrying [EncryptionDegradedException] is emitted to [SyncStateRepository].
 *
 * Also covers:
 *  - The broadcast path gate: [WorkManagerScheduler.scheduleImmediate] returns null when
 *    degraded (CNTR-MODERNIZATION-005 contract — no work enqueued via broadcast).
 *  - Happy path: degraded=false → backup proceeds and WorkManager job IS enqueued.
 *  - Recovery: once the flag clears (simulated by returning false from isEncryptionDegraded),
 *    [MainViewModel.startBackup] enqueues normally again.
 *
 * The U-040 / BUG-008 retry path is NOT modified by these tests — they exercise the gate
 * layer only, not the migration internals (those are covered by
 * [EncryptedPrefsSecretStoreDegradedTest]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class EncryptionDegradedGateTest {

    @Mock private lateinit var mockPreferences: com.zegoggles.smssync.preferences.Preferences
    @Mock private lateinit var mockDataTypePreferences: com.zegoggles.smssync.preferences.DataTypePreferences

    private lateinit var context: Context
    private lateinit var repository: FlowSyncStateRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = RuntimeEnvironment.application
        repository = FlowSyncStateRepository()

        // WorkManager test harness (required for scheduler tests)
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .setWorkerFactory(BackupWorker.TestableBackupWorkerFactory())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        // Default preference stubs
        `when`(mockPreferences.dataTypePreferences).thenReturn(mockDataTypePreferences)
        `when`(mockPreferences.isAutoBackupEnabled).thenReturn(true)
        `when`(mockPreferences.isWifiOnly).thenReturn(false)
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Creates a SecretStore stub that always returns the given degraded value. */
    private fun secretStore(degraded: Boolean): SecretStore = object : SecretStore {
        override fun get(key: String): String? = null
        override fun put(key: String, value: String) {}
        override fun remove(key: String) {}
        override fun contains(key: String) = false
        override fun clear() {}
        override fun migrateFromPlaintext() {}
        override fun isEncryptionDegraded(): Boolean = degraded
    }

    /** Builds a WorkManagerScheduler with the given degraded state. */
    private fun scheduler(degraded: Boolean) =
        WorkManagerScheduler(context, mockPreferences, secretStore(degraded))

    /** Builds a MainViewModel with the given degraded state. */
    private fun viewModel(degraded: Boolean) =
        MainViewModel(context, repository, scheduler(degraded), secretStore(degraded))

    // -----------------------------------------------------------------------
    // AC-3: backup blocked when degraded=true — no WorkManager job enqueued
    // -----------------------------------------------------------------------

    /**
     * AC-3 (a): When degraded=true, MainViewModel.startBackup(MANUAL) emits an ERROR
     * BackupState carrying EncryptionDegradedException to SyncStateRepository.
     * No WorkManager job should be enqueued.
     */
    @Test
    fun degraded_startBackup_emitsErrorState_withEncryptionDegradedException() {
        val vm = viewModel(degraded = true)

        vm.startBackup(BackupType.MANUAL)

        val state = repository.state.value
        assertThat(state).isInstanceOf(BackupState::class.java)
        assertThat((state as BackupState).state).isEqualTo(SmsSyncState.ERROR)
        assertThat(state.exception).isInstanceOf(EncryptionDegradedException::class.java)
    }

    /**
     * AC-3 (b): When degraded=true, no WorkManager job is enqueued for MANUAL backup.
     */
    @Test
    fun degraded_startBackup_noWorkManagerJobEnqueued() {
        val vm = viewModel(degraded = true)

        vm.startBackup(BackupType.MANUAL)

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(BackupType.MANUAL.name)
            .get()
        assertThat(workInfos).isEmpty()
    }

    /**
     * AC-3 (c): When degraded=true, MainViewModel.startBackup(SKIP) also emits ERROR.
     */
    @Test
    fun degraded_startBackupSkip_emitsErrorState() {
        val vm = viewModel(degraded = true)

        vm.startBackup(BackupType.SKIP)

        val state = repository.state.value
        assertThat(state).isInstanceOf(BackupState::class.java)
        assertThat((state as BackupState).state).isEqualTo(SmsSyncState.ERROR)
        assertThat(state.exception).isInstanceOf(EncryptionDegradedException::class.java)
    }

    /**
     * AC-3 (d): When degraded=true, no WorkManager job is enqueued for SKIP backup.
     */
    @Test
    fun degraded_startBackupSkip_noWorkManagerJobEnqueued() {
        val vm = viewModel(degraded = true)

        vm.startBackup(BackupType.SKIP)

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(BackupType.SKIP.name)
            .get()
        assertThat(workInfos).isEmpty()
    }

    /**
     * AC-3 (e): When degraded=true, MainViewModel.startRestore() emits an ERROR
     * RestoreState carrying EncryptionDegradedException.
     */
    @Test
    fun degraded_startRestore_emitsErrorState_withEncryptionDegradedException() {
        val vm = viewModel(degraded = true)

        vm.startRestore()

        val state = repository.state.value
        assertThat(state).isInstanceOf(RestoreState::class.java)
        assertThat((state as RestoreState).state).isEqualTo(SmsSyncState.ERROR)
        assertThat(state.exception).isInstanceOf(EncryptionDegradedException::class.java)
    }

    // -----------------------------------------------------------------------
    // Happy path: degraded=false → backup proceeds, WorkManager job enqueued
    // -----------------------------------------------------------------------

    /**
     * AC-3 (f): When degraded=false, MainViewModel.startBackup(MANUAL) enqueues a
     * WorkManager job. Repository state stays at the INITIAL value (no error emitted).
     */
    @Test
    fun notDegraded_startBackup_workManagerJobIsEnqueued() {
        val vm = viewModel(degraded = false)

        vm.startBackup(BackupType.MANUAL)

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(BackupType.MANUAL.name)
            .get()
        assertThat(workInfos).isNotEmpty()
    }

    /**
     * AC-3 (g): When degraded=false, no EncryptionDegradedException is emitted.
     * Repository initial state (BackupState with INITIAL) is preserved.
     */
    @Test
    fun notDegraded_startBackup_noEncryptionDegradedExceptionInState() {
        val vm = viewModel(degraded = false)

        vm.startBackup(BackupType.MANUAL)

        // State may be updated by WorkManager observer, but it should NOT be an
        // EncryptionDegradedException ERROR state.
        val state = repository.state.value
        val isEncDegradedError = state.isError() &&
                state.exception is EncryptionDegradedException
        assertThat(isEncDegradedError).isFalse()
    }

    // -----------------------------------------------------------------------
    // Recovery: once degraded flag clears, backup resumes (AC-4 / BUG-008 preservation)
    // -----------------------------------------------------------------------

    /**
     * AC-4: Recovery simulation. When the flag clears (degraded=false after having been true),
     * the next call to startBackup() enqueues a WorkManager job normally.
     *
     * This verifies the gate is CONDITIONAL (checked each call), not a one-time latch.
     * The U-040/BUG-008 retry path — migrateFromPlaintext() on next launch — is what clears
     * the flag; this test simulates the cleared state by using a non-degraded secret store.
     */
    @Test
    fun recovery_afterDegradedClears_startBackupEnqueuesNormally() {
        // Phase 1: degraded — no enqueue
        val degradedVm = viewModel(degraded = true)
        degradedVm.startBackup(BackupType.MANUAL)
        assertThat(WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(BackupType.MANUAL.name).get()).isEmpty()

        // Phase 2: flag cleared (recovery simulated via a fresh ViewModel with non-degraded store)
        val recoveredVm = viewModel(degraded = false)
        recoveredVm.startBackup(BackupType.MANUAL)
        assertThat(WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(BackupType.MANUAL.name).get()).isNotEmpty()
    }

    // -----------------------------------------------------------------------
    // checkDegradedOnLaunch: launch-time warning (AC-2)
    // -----------------------------------------------------------------------

    /**
     * AC-2 (a): checkDegradedOnLaunch() when degraded=true emits ERROR BackupState
     * with EncryptionDegradedException and returns true.
     */
    @Test
    fun checkDegradedOnLaunch_whenDegraded_emitsErrorStateAndReturnsTrue() {
        val vm = viewModel(degraded = true)

        val result = vm.checkDegradedOnLaunch()

        assertThat(result).isTrue()
        val state = repository.state.value
        assertThat(state).isInstanceOf(BackupState::class.java)
        assertThat((state as BackupState).state).isEqualTo(SmsSyncState.ERROR)
        assertThat(state.exception).isInstanceOf(EncryptionDegradedException::class.java)
    }

    /**
     * AC-2 (b): checkDegradedOnLaunch() when degraded=false is a no-op and returns false.
     * The repository INITIAL state is preserved.
     */
    @Test
    fun checkDegradedOnLaunch_whenNotDegraded_isNoOpAndReturnsFalse() {
        val vm = viewModel(degraded = false)
        val stateBefore = repository.state.value

        val result = vm.checkDegradedOnLaunch()

        assertThat(result).isFalse()
        // Repository state unchanged (no error emitted)
        assertThat(repository.state.value).isSameInstanceAs(stateBefore)
    }

    // -----------------------------------------------------------------------
    // Broadcast path gate: WorkManagerScheduler.scheduleImmediate() (CNTR-MODERNIZATION-005)
    // -----------------------------------------------------------------------

    /**
     * AC-1 broadcast path: When degraded=true, WorkManagerScheduler.scheduleImmediate()
     * returns null and does NOT enqueue any WorkManager job.
     *
     * This gates the BackupBroadcastReceiver → scheduleImmediate() path
     * (CNTR-MODERNIZATION-005) so that a Tasker/3rd-party BACKUP broadcast on a degraded
     * device does not proceed with plaintext-on-disk credentials.
     */
    @Test
    fun degraded_scheduleImmediate_returnsNullAndNoJobEnqueued() {
        val sched = scheduler(degraded = true)

        val job = sched.scheduleImmediate()

        assertThat(job).isNull()
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(BackupType.BROADCAST_INTENT.name)
            .get()
        assertThat(workInfos).isEmpty()
    }

    /**
     * Happy path broadcast: When degraded=false, WorkManagerScheduler.scheduleImmediate()
     * returns a non-null ScheduledJob and enqueues the work normally.
     */
    @Test
    fun notDegraded_scheduleImmediate_returnsJobAndEnqueuesWork() {
        val sched = scheduler(degraded = false)

        val job = sched.scheduleImmediate()

        assertThat(job).isNotNull()
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(BackupType.BROADCAST_INTENT.name)
            .get()
        assertThat(workInfos).isNotEmpty()
    }

    /**
     * Broadcast path gate: When degraded=true, WorkManagerScheduler.scheduleManual(MANUAL)
     * returns null and does NOT enqueue any WorkManager job.
     */
    @Test
    fun degraded_scheduleManual_returnsNullAndNoJobEnqueued() {
        val sched = scheduler(degraded = true)

        val job = sched.scheduleManual(BackupType.MANUAL)

        assertThat(job).isNull()
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(BackupType.MANUAL.name)
            .get()
        assertThat(workInfos).isEmpty()
    }
}
