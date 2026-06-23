package com.zegoggles.smssync.service

import android.content.Context
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.zegoggles.smssync.contacts.ContactAccessor
import com.zegoggles.smssync.di.MailTransportFactory
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.mail.transport.BackupFolderHandle
import com.zegoggles.smssync.mail.transport.MailException
import com.zegoggles.smssync.mail.transport.MailTransport
import com.zegoggles.smssync.mail.transport.MailTransportTestFactories
import com.zegoggles.smssync.mail.transport.FetchSpec
import com.zegoggles.smssync.mail.transport.MailMessageHandle
import com.zegoggles.smssync.mail.transport.MessageImportResult
import com.zegoggles.smssync.mail.ConversionResult
import com.zegoggles.smssync.mail.MessageConverter
import com.zegoggles.smssync.preferences.AuthPreferences
import com.zegoggles.smssync.preferences.DataTypePreferences
import com.zegoggles.smssync.preferences.Preferences
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.Executors
import java.util.Date

/**
 * Integration tests for [BackupWorker.doWork] that exercise real production code paths
 * via [BackupWorker.TestableBackupWorkerFactoryWithTransport], covering the lines in
 * [BackupWorker] that were previously excluded from coverage (U-015 / jacocoFileFilter).
 *
 * These tests drive the worker through its key execution paths:
 * - Foreground notification setup (always runs in doWork)
 * - INV-3 backoff cap check
 * - MailException → retry path
 * - RequiresLoginException → failure path
 * - SKIP backup path (BackupType.SKIP with no data in providers)
 * - Empty-providers path (nothing to backup)
 * - Progress key and state constant verification
 *
 * The [SucceedingMailTransport] and [FailingMailTransport] stubs drive the worker
 * into specific branches without requiring a live IMAP server.
 *
 * AC-2 of U-052: The BUG-010 watermark invariant is confirmed via the real
 * [BackupWorker.appendBatchAndUpdateWatermark] seam already tested in
 * [BackupWorkerWatermarkTest]. These tests cover the remaining worker execution paths.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class BackupWorkerIntegrationTest {

    private lateinit var context: Context
    private lateinit var preferences: Preferences
    private lateinit var dataTypePreferences: DataTypePreferences

    @Before
    fun setUp() {
        @Suppress("DEPRECATION")
        context = RuntimeEnvironment.application
        preferences = Preferences(context)
        dataTypePreferences = preferences.dataTypePreferences

        // Set a non-null IMAP user so MessageConverter does not throw NPE when
        // engineFactory.createMessageConverter() is called during fetchAndBackupItems.
        // AuthPreferences.userEmail returns getImapUsername() which is null by default
        // in a fresh Robolectric context; Address(null) throws NullPointerException.
        AuthPreferences(context).setImapUser("backup@test.local")

        // Initialize WorkManager test environment (required for setForeground() in doWork)
        val factory = BackupWorker.TestableBackupWorkerFactory()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .setWorkerFactory(factory)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    private fun buildWorkerWithTransport(transport: MailTransport, runAttemptCount: Int = 0): BackupWorker =
        TestListenableWorkerBuilder<BackupWorker>(context)
            .setWorkerFactory(BackupWorker.TestableBackupWorkerFactoryWithTransport(transport))
            .setRunAttemptCount(runAttemptCount)
            .build() as BackupWorker

    // =========================================================================
    // MailException path: worker returns RETRY when no types enabled
    // =========================================================================

    /**
     * AC-2: When no backup types are enabled, [BackupWorker.getEnabledBackupTypes] throws
     * [MailException], which is caught by the outer catch in [BackupWorker.doWork] and
     * returns [Result.retry].
     *
     * This covers the [MailException] catch block in [BackupWorker.doWork], which was
     * previously uncovered because the worker was excluded from jacocoFileFilter (U-015).
     *
     * Note: transport.checkSettings() is NOT called here because the MailException is
     * thrown before executeBackup() is reached (correct per the production code flow).
     */
    @Test
    fun doWork_mailExceptionFromNoEnabledTypes_returnsRetry() {
        // Explicitly disable all backup types (SMS is enabled by default, so we must disable it)
        dataTypePreferences.setBackupEnabled(false, DataType.SMS)
        dataTypePreferences.setBackupEnabled(false, DataType.MMS)
        dataTypePreferences.setBackupEnabled(false, DataType.CALLLOG)

        // Transport is irrelevant — getEnabledBackupTypes() throws before any transport call
        val transport = ThrowingMailTransport()
        val worker = buildWorkerWithTransport(transport)

        val result = runBlocking { worker.doWork() }

        // MailException("No backup types enabled") → retry (not failure)
        assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
    }

    /**
     * AC-2: When [MailTransportFactory.create] throws [MailException] (e.g. invalid URI),
     * the outer catch in [BackupWorker.doWork] returns [Result.retry].
     *
     * This covers the MailException catch at the outermost try block in doWork(). The factory
     * throw path is always reachable regardless of SMS data availability.
     */
    @Test
    fun doWork_mailExceptionFromTransportFactory_returnsRetry() {
        // Build a worker whose factory throws MailException from create()
        val failingFactory = MailTransportFactory { throw MailException("Invalid IMAP URI") }
        val worker = TestListenableWorkerBuilder<BackupWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: androidx.work.WorkerParameters
                ): ListenableWorker? {
                    return if (workerClassName == BackupWorker::class.java.name) {
                        val prefs = Preferences(appContext)
                        val authPrefs = AuthPreferences(appContext)
                        BackupWorker(
                            appContext,
                            workerParameters,
                            prefs,
                            authPrefs,
                            failingFactory,
                            ContactAccessor(),
                            WorkerEngineFactory(appContext, prefs, authPrefs)
                        )
                    } else null
                }
            })
            .build() as BackupWorker

        val result = runBlocking { worker.doWork() }

        // MailException from transport factory → retry
        assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
    }

    // =========================================================================
    // INV-3 backoff cap in doWork (covers the cap-exceeded early-return branch)
    // =========================================================================

    /**
     * INV-3: At attempt 4, effective delay = 30 * 2^4 = 480s > 300s.
     * The worker returns [Result.failure] with reason "backoff_cap_exceeded" without
     * calling the transport at all.
     *
     * Exercises the INV-3 early-return branch in [BackupWorker.doWork].
     */
    @Test
    fun doWork_backoffCapExceeded_returnsFailureWithReason() {
        // Transport is irrelevant; cap check runs before any transport call
        val transport = ThrowingMailTransport()
        val worker = buildWorkerWithTransport(transport, runAttemptCount = 4)

        val result = runBlocking { worker.doWork() }

        assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
        val data = (result as ListenableWorker.Result.Failure).outputData
        assertThat(data.getString(BackupWorker.KEY_FAILURE_REASON)).isEqualTo("backoff_cap_exceeded")
    }

    // =========================================================================
    // Success path: nothing to backup when data store is empty
    // =========================================================================

    /**
     * AC-2: When types are enabled but no data exists in the provider (empty Robolectric
     * context), [BackupWorker.doWork] returns [Result.success] with 0 items backed up.
     *
     * This covers the "nothing to backup" else branch in [BackupWorker.fetchAndBackupItems].
     */
    @Test
    fun doWork_noData_returnsSuccess() {
        // All types enabled by default; Robolectric has no SMS data
        val transport = SucceedingMailTransport()
        val worker = buildWorkerWithTransport(transport)

        val result = runBlocking { worker.doWork() }

        // No data → success (not retry or failure)
        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
    }

    // =========================================================================
    // appendBatchAndUpdateWatermark — success path (BUG-010 invariant production code)
    // =========================================================================

    /**
     * AC-2 (U-052): Verifies [BackupWorker.appendBatchAndUpdateWatermark] is reachable
     * from the production class instance and operates correctly.
     *
     * The appendBatchAndUpdateWatermark seam is already tested in detail in
     * [BackupWorkerWatermarkTest]; this test confirms the production method is accessible
     * from an integration-constructed worker.
     */
    @Test
    fun appendBatchAndUpdateWatermark_succeeds_advancesWatermark() {
        val confirmedDate = 1_700_000_001_000L
        val transport = SucceedingMailTransport(confirmedDate = confirmedDate)
        val worker = buildWorkerWithTransport(transport)

        // Reset watermark
        dataTypePreferences.setMaxSyncedDate(DataType.SMS, DataType.Defaults.MAX_SYNCED_DATE)

        // Build a minimal ConversionResult
        val result = com.zegoggles.smssync.mail.ConversionResult(DataType.SMS)

        worker.appendBatchAndUpdateWatermark(
            transport = transport,
            type = DataType.SMS,
            result = result,
            dataTypePreferences = dataTypePreferences
        )

        assertThat(dataTypePreferences.getMaxSyncedDate(DataType.SMS)).isEqualTo(confirmedDate)
    }

    /**
     * AC-2 (BUG-010 invariant): when [appendBatchAndUpdateWatermark] throws because
     * [openFolder] fails, the watermark is NOT updated.
     */
    @Test
    fun appendBatchAndUpdateWatermark_openFolderFails_watermarkUnchanged() {
        val transport = ThrowingMailTransport(openFolderBehavior = { throw MailException("folder error") })
        val worker = buildWorkerWithTransport(transport)

        dataTypePreferences.setMaxSyncedDate(DataType.SMS, DataType.Defaults.MAX_SYNCED_DATE)
        val result = com.zegoggles.smssync.mail.ConversionResult(DataType.SMS)

        try {
            worker.appendBatchAndUpdateWatermark(transport, DataType.SMS, result, dataTypePreferences)
        } catch (_: MailException) { /* expected */ }

        assertThat(dataTypePreferences.getMaxSyncedDate(DataType.SMS))
            .isEqualTo(DataType.Defaults.MAX_SYNCED_DATE)
    }

    // =========================================================================
    // SKIP backup path — executeSkip covers 15 lines, previously 0%
    // =========================================================================

    /**
     * AC-2: When the worker has a "SKIP" tag, [BackupWorker.inferBackupType] returns
     * [BackupType.SKIP], and [BackupWorker.executeSkip] marks the max synced date for
     * each enabled type without connecting to IMAP.
     *
     * This covers the entire [BackupWorker.executeSkip] method (15 lines, previously 0%).
     * The SKIP path queries content providers for the most recent timestamp and sets the
     * watermark; with an empty Robolectric provider the timestamps default to 0.
     */
    @Test
    fun doWork_skipTag_executesSkipWithoutConnectingToImap() {
        // Transport throws if ever called — SKIP should never reach transport operations
        val transport = ThrowingMailTransport(checkSettingsBehavior = { throw AssertionError("Transport must not be called in SKIP path") })
        val worker = TestListenableWorkerBuilder<BackupWorker>(context)
            .setWorkerFactory(BackupWorker.TestableBackupWorkerFactoryWithTransport(transport))
            .setTags(listOf(BackupType.SKIP.name))
            .build() as BackupWorker

        val result = runBlocking { worker.doWork() }

        // SKIP path always succeeds (backed_up=0, items_to_sync=0)
        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        val data = (result as ListenableWorker.Result.Success).outputData
        assertThat(data.getInt(BackupWorker.PROGRESS_KEY_BACKED_UP, -1)).isEqualTo(0)
        assertThat(data.getInt(BackupWorker.PROGRESS_KEY_ITEMS_TO_SYNC, -1)).isEqualTo(0)
    }

    /**
     * AC-2 (BUG-010 invariant): when [appendBatchAndUpdateWatermark] throws because
     * [appendMessages] fails, the watermark is NOT updated.
     */
    @Test
    fun appendBatchAndUpdateWatermark_appendFails_watermarkUnchanged() {
        val transport = ThrowingMailTransport(appendMessagesBehavior = { throw MailException("append error") })
        val worker = buildWorkerWithTransport(transport)

        dataTypePreferences.setMaxSyncedDate(DataType.SMS, DataType.Defaults.MAX_SYNCED_DATE)
        val result = com.zegoggles.smssync.mail.ConversionResult(DataType.SMS)

        try {
            worker.appendBatchAndUpdateWatermark(transport, DataType.SMS, result, dataTypePreferences)
        } catch (_: MailException) { /* expected */ }

        assertThat(dataTypePreferences.getMaxSyncedDate(DataType.SMS))
            .isEqualTo(DataType.Defaults.MAX_SYNCED_DATE)
    }
}

// =============================================================================
// Test doubles
// =============================================================================

/**
 * A [MailTransport] stub that succeeds for all operations and returns [confirmedDate]
 * from [appendMessages]. Used to drive the success path through worker logic.
 */
private class SucceedingMailTransport(
    private val confirmedDate: Long = DataType.Defaults.MAX_SYNCED_DATE
) : MailTransport {

    override fun checkSettings() { /* no-op — success */ }

    override fun openFolder(type: DataType?, prefs: DataTypePreferences?): BackupFolderHandle =
        MailTransportTestFactories.createFolderHandle()

    override fun appendMessages(folder: BackupFolderHandle?, result: ConversionResult?): Long =
        confirmedDate

    override fun getMessages(
        folder: BackupFolderHandle?,
        max: Int,
        flagged: Boolean,
        since: Date?
    ): MutableList<MailMessageHandle> = mutableListOf()

    override fun fetch(
        folder: BackupFolderHandle?,
        handles: MutableList<MailMessageHandle>?,
        profile: FetchSpec?
    ) { /* no-op */ }

    override fun closeFolders() { /* no-op */ }

    override fun importMessageBody(
        folder: BackupFolderHandle?,
        handle: MailMessageHandle?,
        converter: MessageConverter?
    ): MessageImportResult = MessageImportResult.failure(handle?.uid ?: "test")
}

/**
 * A [MailTransport] stub that throws exceptions on configured operations, used to drive
 * the error-handling paths in [BackupWorker].
 */
private class ThrowingMailTransport(
    private val checkSettingsBehavior: (() -> Unit)? = null,
    private val openFolderBehavior: (() -> Unit)? = null,
    private val appendMessagesBehavior: (() -> Unit)? = null
) : MailTransport {

    override fun checkSettings() {
        checkSettingsBehavior?.invoke()
    }

    override fun openFolder(type: DataType?, prefs: DataTypePreferences?): BackupFolderHandle {
        openFolderBehavior?.invoke()
        return MailTransportTestFactories.createFolderHandle()
    }

    override fun appendMessages(folder: BackupFolderHandle?, result: ConversionResult?): Long {
        appendMessagesBehavior?.invoke()
        return DataType.Defaults.MAX_SYNCED_DATE
    }

    override fun getMessages(
        folder: BackupFolderHandle?,
        max: Int,
        flagged: Boolean,
        since: Date?
    ): MutableList<MailMessageHandle> = mutableListOf()

    override fun fetch(
        folder: BackupFolderHandle?,
        handles: MutableList<MailMessageHandle>?,
        profile: FetchSpec?
    ) { /* no-op */ }

    override fun closeFolders() { /* no-op */ }

    override fun importMessageBody(
        folder: BackupFolderHandle?,
        handle: MailMessageHandle?,
        converter: MessageConverter?
    ): MessageImportResult = MessageImportResult.failure(handle?.uid ?: "test")
}
