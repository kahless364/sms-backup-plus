package com.zegoggles.smssync.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.WorkerAssistedFactory
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.preferences.DataTypePreferences
import com.zegoggles.smssync.preferences.Preferences
import com.zegoggles.smssync.preferences.SecretStore
import com.zegoggles.smssync.scheduler.BackupScheduler
import com.zegoggles.smssync.scheduler.WorkManagerScheduler
import com.zegoggles.smssync.service.BackupType
import com.zegoggles.smssync.service.BackupWorker
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.util.UUID
import java.util.concurrent.Executors

/**
 * U-069 (BUG-023): the content-trigger worker starts the incoming backup through
 * [WorkManagerScheduler.scheduleIncoming], so the trigger path obeys the rules of every other
 * incoming backup (CNTR-MODERNIZATION-004 INV-1 to INV-4, the U-054 encryption-degraded gate, the
 * auto-backup and delay rule) and keeps the two-stage debounce (INV-4).
 *
 * Harness as in WorkManagerSchedulerTest: WorkManagerTestInitHelper with a single-thread executor,
 * mocked preferences and secret store, a real WorkManagerScheduler. The worker is built with
 * TestListenableWorkerBuilder and a factory that passes that scheduler and those preferences, as
 * HiltWorkerFactory passes the injected port in production; doWork() is called directly. Each test
 * registers the content trigger with the happy-path stubs first, then changes the stub it is about,
 * then runs the worker, so the rules are shown to hold at run time. The clock is not advanced and no
 * constraint is marked met, so INCOMING work stays ENQUEUED.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class BackupTriggerWorkerTest {

    private lateinit var context: Context
    private lateinit var scheduler: WorkManagerScheduler

    @Mock private lateinit var preferences: Preferences
    @Mock private lateinit var dataTypePreferences: DataTypePreferences
    @Mock private lateinit var secretStore: SecretStore

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = RuntimeEnvironment.getApplication()

        // The harness of WorkManagerSchedulerTest.setUp. BackupWorker would be created by its test
        // factory if WorkManager ran it; it does not run here.
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .setWorkerFactory(BackupWorker.TestableBackupWorkerFactory())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        // The allowed state; each test changes the stub it is about after the trigger is registered.
        `when`(preferences.dataTypePreferences).thenReturn(dataTypePreferences)
        `when`(preferences.isAutoBackupEnabled).thenReturn(true)
        `when`(preferences.incomingTimeoutSecs).thenReturn(60)
        `when`(preferences.regularTimeoutSecs).thenReturn(3600)
        `when`(preferences.isWifiOnly).thenReturn(false)
        `when`(preferences.isCallLogBackupAfterCallEnabled()).thenReturn(false)
        `when`(dataTypePreferences.isBackupEnabled(DataType.CALLLOG)).thenReturn(false)
        `when`(secretStore.isEncryptionDegraded()).thenReturn(false)

        scheduler = WorkManagerScheduler(context, preferences, secretStore)
    }

    // -----------------------------------------------------------------------
    // AC-1, AC-2: INV-2 on the trigger path
    // -----------------------------------------------------------------------

    @Test
    fun ac1_wifiOnlyOn_incomingWorkRequiresUnmeteredNetwork() {
        registerContentTrigger()
        `when`(preferences.isWifiOnly).thenReturn(true)

        val result = runTriggerWorker()

        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
        val work = incomingWork()
        assertWithMessage("INCOMING work entries").that(work).hasSize(1)
        assertWithMessage("required network type").that(work[0].constraints.requiredNetworkType)
            .isEqualTo(NetworkType.UNMETERED)
    }

    @Test
    fun ac2_wifiOnlyOff_incomingWorkRequiresConnectedNetwork() {
        registerContentTrigger()
        `when`(preferences.isWifiOnly).thenReturn(false)

        val result = runTriggerWorker()

        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
        val work = incomingWork()
        assertWithMessage("INCOMING work entries").that(work).hasSize(1)
        assertWithMessage("required network type").that(work[0].constraints.requiredNetworkType)
            .isEqualTo(NetworkType.CONNECTED)
    }

    // -----------------------------------------------------------------------
    // AC-3: the request the trigger path leaves equals the one scheduleIncoming() leaves
    // -----------------------------------------------------------------------

    @Test
    fun ac3_wifiOnlyOn_triggerRequestEqualsScheduleIncomingRequest() {
        assertTriggerRequestEqualsScheduleIncomingRequest(wifiOnly = true, expectedNetwork = NetworkType.UNMETERED)
    }

    @Test
    fun ac3_wifiOnlyOff_triggerRequestEqualsScheduleIncomingRequest() {
        assertTriggerRequestEqualsScheduleIncomingRequest(wifiOnly = false, expectedNetwork = NetworkType.CONNECTED)
    }

    // -----------------------------------------------------------------------
    // AC-4 to AC-6: the scheduler declines; doWork() still returns success (AC-9)
    // -----------------------------------------------------------------------

    @Test
    fun ac4_encryptionDegradedAtRunTime_noIncomingWork_success() {
        registerContentTrigger()
        `when`(secretStore.isEncryptionDegraded()).thenReturn(true)

        val result = runTriggerWorker()

        assertWithMessage("INCOMING work entries").that(incomingWork()).isEmpty()
        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun ac5_autoBackupOffAtRunTime_noIncomingWork_success() {
        registerContentTrigger()
        `when`(preferences.isAutoBackupEnabled).thenReturn(false)

        val result = runTriggerWorker()

        assertWithMessage("INCOMING work entries").that(incomingWork()).isEmpty()
        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun ac6_incomingDelayZero_noIncomingWork_success() {
        registerContentTrigger()
        `when`(preferences.incomingTimeoutSecs).thenReturn(0)

        val result = runTriggerWorker()

        assertWithMessage("INCOMING work entries").that(incomingWork()).isEmpty()
        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun ac6_incomingDelayNegative_noIncomingWork_success() {
        registerContentTrigger()
        `when`(preferences.incomingTimeoutSecs).thenReturn(-1)

        val result = runTriggerWorker()

        assertWithMessage("INCOMING work entries").that(incomingWork()).isEmpty()
        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
    }

    // -----------------------------------------------------------------------
    // AC-7: the debounce is kept (INV-4); the delay length is asserted
    // -----------------------------------------------------------------------

    @Test
    fun ac7_debounce_incomingWorkWaitsEnqueuedForTheIncomingDelay() {
        registerContentTrigger()
        `when`(preferences.incomingTimeoutSecs).thenReturn(60)

        val result = runTriggerWorker()

        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
        val work = incomingWork()
        assertWithMessage("INCOMING work entries").that(work).hasSize(1)
        assertWithMessage("state (BackupWorker has not run)").that(work[0].state).isEqualTo(WorkInfo.State.ENQUEUED)
        assertWithMessage("run attempts").that(work[0].runAttemptCount).isEqualTo(0)
        assertWithMessage("initial delay ms").that(work[0].initialDelayMillis).isEqualTo(60L * 1000L)
    }

    // -----------------------------------------------------------------------
    // AC-8: single flight (INV-1); the new id shows that a new request replaced the old one
    // -----------------------------------------------------------------------

    @Test
    fun ac8a_triggerTwice_oneActiveIncomingWork_secondReplacesFirst() {
        registerContentTrigger()
        runTriggerWorker()
        val first = singleActiveIncomingId()

        runTriggerWorker()

        assertReplaced(first)
    }

    @Test
    fun ac8b_triggerThenScheduleIncoming_oneActiveIncomingWork_secondReplacesFirst() {
        registerContentTrigger()
        runTriggerWorker()
        val first = singleActiveIncomingId()

        assertWithMessage("scheduleIncoming() job").that(scheduler.scheduleIncoming()).isNotNull()

        assertReplaced(first)
    }

    @Test
    fun ac8c_scheduleIncomingThenTrigger_oneActiveIncomingWork_secondReplacesFirst() {
        registerContentTrigger()
        assertWithMessage("scheduleIncoming() job").that(scheduler.scheduleIncoming()).isNotNull()
        val first = singleActiveIncomingId()

        runTriggerWorker()

        assertReplaced(first)
    }

    // -----------------------------------------------------------------------
    // AC-9: the worker's own contract (log line byte for byte, success in every case)
    // -----------------------------------------------------------------------

    @Test
    fun ac9_allowedState_logsTheTriggerLineAtDebugBeforeTheSchedulerEnqueues() {
        registerContentTrigger()
        `when`(preferences.incomingTimeoutSecs).thenReturn(75)

        val result = runTriggerWorker()

        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
        val logs = ShadowLog.getLogsForTag(TAG)
        val trigger = assertOneTriggerLine(logs, 75)
        val enqueued = logs.indexOfFirst {
            (it.msg ?: "").startsWith("WorkManagerScheduler.scheduleIncoming: enqueued INCOMING")
        }
        assertWithMessage("index of the scheduler's enqueued line, after the trigger line")
            .that(enqueued).isGreaterThan(trigger)
        val work = incomingWork()
        assertWithMessage("INCOMING work entries").that(work).hasSize(1)
        assertWithMessage("initial delay ms (the current setting, not the default)").that(work[0].initialDelayMillis)
            .isEqualTo(75L * 1000L)
    }

    @Test
    fun ac9_encryptionDegraded_logsTheTriggerLineThenTheSchedulersBlockedLine_success() {
        registerContentTrigger()
        `when`(secretStore.isEncryptionDegraded()).thenReturn(true)

        val result = runTriggerWorker()

        assertWithMessage("doWork() result").that(result).isEqualTo(ListenableWorker.Result.success())
        val logs = ShadowLog.getLogsForTag(TAG)
        val trigger = assertOneTriggerLine(logs, 60)
        val blocked = logs.indexOfFirst {
            (it.msg ?: "").startsWith("WorkManagerScheduler.scheduleIncoming: blocked")
        }
        assertWithMessage("index of the scheduler's blocked line, after the trigger line")
            .that(blocked).isGreaterThan(trigger)
    }

    // -----------------------------------------------------------------------
    // IC-1, IC-2 (b): the production wiring, read from the classes generated for the worker
    // -----------------------------------------------------------------------

    /**
     * HiltWorkerFactory finds a worker's assisted factory in a map keyed by the class name that
     * WorkManager stores with the work. Hilt does not run under Robolectric here, so this reads the
     * classes the androidx.hilt compiler generates for the annotated worker: the assisted factory
     * exists, it is bound into that map under the persisted class name, and the worker has exactly one
     * assisted-inject constructor with the two assisted parameters first. Without the HiltWorker
     * annotation the generated classes are missing and this fails. That the app's component installs
     * the module and the factory builds the worker on a device is IC-3's.
     */
    @Test
    fun ic1_hiltWiring_assistedFactoryIsBoundUnderThePersistedClassName() {
        assertWithMessage("worker class name").that(BackupTriggerWorker::class.java.name)
            .isEqualTo(PERSISTED_CLASS_NAME)

        val assistedFactory = generatedClass("_AssistedFactory")
        val hiltModule = generatedClass("_HiltModule")
        assertWithMessage("generated ${PERSISTED_CLASS_NAME}_AssistedFactory").that(assistedFactory).isNotNull()
        assertWithMessage("generated ${PERSISTED_CLASS_NAME}_HiltModule").that(hiltModule).isNotNull()
        assertWithMessage("assisted factory type")
            .that(WorkerAssistedFactory::class.java.isAssignableFrom(assistedFactory!!)).isTrue()
        assertWithMessage("assisted factory annotation")
            .that(assistedFactory.isAnnotationPresent(AssistedFactory::class.java)).isTrue()

        val bind = hiltModule!!.getDeclaredMethod("bind", assistedFactory)
        assertWithMessage("map binding").that(bind.isAnnotationPresent(IntoMap::class.java)).isTrue()
        assertWithMessage("map key").that(bind.getAnnotation(StringKey::class.java)?.value)
            .isEqualTo(PERSISTED_CLASS_NAME)

        val assistedConstructors = BackupTriggerWorker::class.java.declaredConstructors
            .filter { it.isAnnotationPresent(AssistedInject::class.java) }
        assertWithMessage("assisted-inject constructors").that(assistedConstructors).hasSize(1)
        assertWithMessage("constructor parameters").that(assistedConstructors[0].parameterTypes.toList())
            .containsExactly(
                Context::class.java, WorkerParameters::class.java,
                BackupScheduler::class.java, Preferences::class.java
            ).inOrder()
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Builds the trigger worker with the test's port and preferences, as HiltWorkerFactory does with the injected ones. */
    private inner class TriggerWorkerFactory : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? =
            if (workerClassName == BackupTriggerWorker::class.java.name) {
                BackupTriggerWorker(appContext, workerParameters, scheduler, preferences)
            } else {
                null
            }
    }

    /** Fields of one INCOMING request: public WorkInfo fields, plus backoff and input data from the WorkSpec. */
    private data class IncomingRequest(
        val tags: Set<String>,
        val initialDelayMillis: Long,
        val constraints: Constraints,
        val backoffPolicy: BackoffPolicy,
        val backoffDelayDuration: Long,
        val input: Data
    )

    private fun registerContentTrigger() {
        assertWithMessage("content trigger registered with the allowed state")
            .that(scheduler.scheduleContentTrigger()).isNotNull()
    }

    private fun runTriggerWorker(): ListenableWorker.Result =
        TestListenableWorkerBuilder<BackupTriggerWorker>(context)
            .setWorkerFactory(TriggerWorkerFactory())
            .build()
            .doWork()

    private fun incomingWork(): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(INCOMING).get()

    private fun activeIncomingWork(): List<WorkInfo> = incomingWork().filter { !it.state.isFinished }

    private fun singleActiveIncomingId(): UUID {
        val active = activeIncomingWork()
        assertWithMessage("active INCOMING work entries").that(active).hasSize(1)
        return active[0].id
    }

    private fun assertReplaced(first: UUID) {
        val survivor = singleActiveIncomingId()
        assertWithMessage("id of the surviving INCOMING work (the second request)").that(survivor).isNotEqualTo(first)
        val old: WorkInfo? = WorkManager.getInstance(context).getWorkInfoById(first).get()
        if (old != null) {
            assertWithMessage("state of the first INCOMING work").that(old.state).isEqualTo(WorkInfo.State.CANCELLED)
        }
    }

    private fun assertTriggerRequestEqualsScheduleIncomingRequest(wifiOnly: Boolean, expectedNetwork: NetworkType) {
        registerContentTrigger()
        `when`(preferences.isWifiOnly).thenReturn(wifiOnly)

        assertWithMessage("doWork() result").that(runTriggerWorker()).isEqualTo(ListenableWorker.Result.success())
        val viaTrigger = captureActiveIncomingRequest()
        WorkManager.getInstance(context).cancelUniqueWork(INCOMING).result.get()
        assertWithMessage("scheduleIncoming() job").that(scheduler.scheduleIncoming()).isNotNull()
        val viaScheduler = captureActiveIncomingRequest()

        assertWithMessage("request via the trigger worker, against the request via scheduleIncoming()")
            .that(viaTrigger).isEqualTo(viaScheduler)
        // The equal pair is also tied to the contract's values, so "equal but both wrong" cannot pass.
        assertWithMessage("required network type (INV-2)").that(viaTrigger.constraints.requiredNetworkType)
            .isEqualTo(expectedNetwork)
        assertWithMessage("backoff policy (INV-3)").that(viaTrigger.backoffPolicy).isEqualTo(BackoffPolicy.EXPONENTIAL)
        assertWithMessage("backoff delay ms (INV-3)").that(viaTrigger.backoffDelayDuration)
            .isEqualTo(WorkManagerScheduler.BACKOFF_INITIAL_SECS * 1000L)
        assertWithMessage("initial delay ms (INV-4)").that(viaTrigger.initialDelayMillis).isEqualTo(60L * 1000L)
        assertWithMessage("tags").that(viaTrigger.tags).contains(INCOMING)
    }

    // WorkInfo exposes neither backoff nor input data (WorkManager 2.9.1 and 2.12.0); the internal WorkSpec does,
    // through the same members in both. WorkSpec, WorkSpecDao and WorkDatabase are library-internal, and WorkDatabase
    // extends Room's RoomDatabase, which is a runtime-only dependency of work-runtime and so not on the unit-test
    // compile classpath. The database, its DAO and the spec are therefore reached by reflection, through the public
    // declaring types; only the public Data and BackoffPolicy types are named in this source.
    private fun captureActiveIncomingRequest(): IncomingRequest {
        val active = activeIncomingWork()
        assertWithMessage("active INCOMING work entries").that(active).hasSize(1)
        val info = active[0]
        val spec = workSpecOf(info.id)
        assertWithMessage("WorkSpec of the active INCOMING work").that(spec).isNotNull()
        val specClass = Class.forName("androidx.work.impl.model.WorkSpec")
        return IncomingRequest(
            tags = info.tags,
            initialDelayMillis = info.initialDelayMillis,
            constraints = info.constraints,
            backoffPolicy = specClass.getField("backoffPolicy").get(spec) as BackoffPolicy,
            backoffDelayDuration = specClass.getField("backoffDelayDuration").getLong(spec),
            input = specClass.getField("input").get(spec) as Data
        )
    }

    /** WorkManagerImpl.getWorkDatabase().workSpecDao().getWorkSpec(id), by reflection (see above). */
    private fun workSpecOf(id: UUID): Any? {
        val workManager = WorkManager.getInstance(context)
        val database = Class.forName("androidx.work.impl.WorkManagerImpl").getMethod("getWorkDatabase").invoke(workManager)
        val dao = Class.forName("androidx.work.impl.WorkDatabase").getMethod("workSpecDao").invoke(database)
        return Class.forName("androidx.work.impl.model.WorkSpecDao").getMethod("getWorkSpec", String::class.java)
            .invoke(dao, id.toString())
    }

    /** Asserts the trigger line once, at debug, byte for byte as CR-015, BUG-021 AC-12 and DES-014 Run R read it. */
    private fun assertOneTriggerLine(logs: List<ShadowLog.LogItem>, incomingTimeoutSecs: Int): Int {
        val expected = TRIGGER_LINE_PREFIX + incomingTimeoutSecs + "s"
        val indices = logs.indices.filter { logs[it].msg == expected }
        assertWithMessage("count of the trigger line \"$expected\"").that(indices).hasSize(1)
        assertWithMessage("level of the trigger line").that(logs[indices[0]].type).isEqualTo(Log.DEBUG)
        return indices[0]
    }

    private fun generatedClass(suffix: String): Class<*>? =
        try {
            Class.forName(PERSISTED_CLASS_NAME + suffix)
        } catch (e: ClassNotFoundException) {
            null
        }

    companion object {
        private const val TAG = "SMSBackup+"
        private const val PERSISTED_CLASS_NAME = "com.zegoggles.smssync.worker.BackupTriggerWorker"
        private const val TRIGGER_LINE_PREFIX =
            "BackupTriggerWorker.doWork: content-URI trigger fired; enqueueing delayed incoming backup with initialDelay="
        private val INCOMING = BackupType.INCOMING.name
    }
}
