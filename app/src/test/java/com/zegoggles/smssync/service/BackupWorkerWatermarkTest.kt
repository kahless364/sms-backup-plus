package com.zegoggles.smssync.service

import android.content.Context
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.zegoggles.smssync.mail.ConversionResult
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.mail.Headers
import com.zegoggles.smssync.mail.transport.BackupFolderHandle
import com.zegoggles.smssync.mail.transport.FetchSpec
import com.zegoggles.smssync.mail.transport.MailException
import com.zegoggles.smssync.mail.transport.MailMessageHandle
import com.zegoggles.smssync.mail.transport.MailTransport
import com.zegoggles.smssync.mail.transport.MailTransportTestFactories
import com.zegoggles.smssync.mail.transport.MessageImportResult
import com.zegoggles.smssync.mail.MessageConverter
import com.zegoggles.smssync.preferences.DataTypePreferences
import com.zegoggles.smssync.preferences.Preferences
import com.zegoggles.smssync.service.exception.RequiresLoginException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import com.fsck.k9.mail.store.imap.ImapMessage
import java.util.Date

/**
 * Regression tests for BUG-010 / U-042: watermark advances ONLY after confirmed IMAP append.
 *
 * U-053 (TE-003): tests now invoke [BackupWorker.appendBatchAndUpdateWatermark] directly —
 * the named `internal` production seam extracted in U-053. The previous local copy of the
 * watermark loop has been deleted. Any regression introduced into the
 * production watermark-gating code will now be caught by these tests.
 *
 * Tests cover AC-3 scenarios:
 *   (a) append/folder failure → watermark unchanged  (3 variants)
 *   (b) successful batch → watermark = max appended message date  (2 variants)
 *   (c) partial success (batch 1 ok, batch 2 fails) → watermark = batch 1 max date only
 *
 * Architecture:
 * - [StubMailTransport] — configurable stub [MailTransport] that either throws or returns
 *   a confirmed date from [MailTransport.appendMessages].
 * - The subject under test is a real [BackupWorker] instance constructed via
 *   [BackupWorker.TestableBackupWorkerFactoryWithTransport], ensuring production code
 *   (not a copy) is exercised for every assertion.
 * - [buildConversionResult] — creates a [ConversionResult] with a known max date so tests
 *   can assert exact watermark values.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class BackupWorkerWatermarkTest {

    private lateinit var context: Context
    private lateinit var preferences: Preferences
    private lateinit var dataTypePreferences: DataTypePreferences

    companion object {
        // Test SMS dates — realistic epoch-ms values (NOT wall-clock "now")
        const val SMS_DATE_1 = 1_700_000_001_000L   // 2023-11-14 ~22:13:21 UTC
        const val SMS_DATE_2 = 1_700_000_002_000L   // 1 second later
    }

    @Before
    fun setUp() {
        @Suppress("DEPRECATION")
        context = RuntimeEnvironment.application
        preferences = Preferences(context)
        dataTypePreferences = preferences.dataTypePreferences

        // Reset watermarks to default (-1) before each test
        dataTypePreferences.setMaxSyncedDate(DataType.SMS, DataType.Defaults.MAX_SYNCED_DATE)
        dataTypePreferences.setMaxSyncedDate(DataType.MMS, DataType.Defaults.MAX_SYNCED_DATE)
        dataTypePreferences.setMaxSyncedDate(DataType.CALLLOG, DataType.Defaults.MAX_SYNCED_DATE)
    }

    /**
     * Constructs a real [BackupWorker] with a [StubMailTransport] injected via
     * [BackupWorker.TestableBackupWorkerFactoryWithTransport].
     *
     * U-053: this is the production seam bridge — tests call [BackupWorker.appendBatchAndUpdateWatermark]
     * on this worker instance, exercising the real production watermark-gating code.
     */
    private fun buildWorker(transport: StubMailTransport): BackupWorker =
        TestListenableWorkerBuilder<BackupWorker>(context)
            .setWorkerFactory(BackupWorker.TestableBackupWorkerFactoryWithTransport(transport))
            .build() as BackupWorker

    // -------------------------------------------------------------------------
    // AC-3(a): append/folder failure → watermark unchanged
    // -------------------------------------------------------------------------

    /**
     * AC-3(a): When [MailTransport.openFolder] throws [MailException], the watermark must
     * remain at its initial value (-1). No append was confirmed, so the watermark must not advance.
     *
     * Root cause guard (BUG-010): the confirmed-append contract requires that
     * [DataTypePreferences.setMaxSyncedDate] is NEVER called unless [MailTransport.appendMessages]
     * returns normally. If openFolder throws, appendMessages is never called, and
     * setMaxSyncedDate is unreachable.
     *
     * U-053: assertion is against production [BackupWorker.appendBatchAndUpdateWatermark].
     */
    @Test
    fun ac3a_openFolderFails_watermarkUnchanged() {
        val transport = StubMailTransport(openFolderBehavior = { throw MailException("folder NONEXISTENT") })
        val worker = buildWorker(transport)

        try {
            worker.appendBatchAndUpdateWatermark(
                transport = transport,
                type = DataType.SMS,
                result = buildConversionResult(DataType.SMS, SMS_DATE_1),
                dataTypePreferences = dataTypePreferences
            )
        } catch (_: MailException) {
            // Expected: openFolder threw → watermark write never reached
        }

        // Watermark must remain at -1 (DataType.Defaults.MAX_SYNCED_DATE)
        assertThat(dataTypePreferences.getMaxSyncedDate(DataType.SMS))
            .isEqualTo(DataType.Defaults.MAX_SYNCED_DATE)
    }

    /**
     * AC-3(a) variant: [MailTransport.appendMessages] throws [MailException].
     *
     * openFolder succeeds; appendMessages throws (e.g. k9 silently failed to append,
     * returned a server error, and the adapter threw a MailException). Watermark unchanged.
     *
     * U-053: assertion is against production [BackupWorker.appendBatchAndUpdateWatermark].
     */
    @Test
    fun ac3a_appendMessagesFails_watermarkUnchanged() {
        val transport = StubMailTransport(
            appendMessagesBehavior = { throw MailException("NO [NONEXISTENT] folder gone") }
        )
        val worker = buildWorker(transport)

        try {
            worker.appendBatchAndUpdateWatermark(
                transport = transport,
                type = DataType.SMS,
                result = buildConversionResult(DataType.SMS, SMS_DATE_1),
                dataTypePreferences = dataTypePreferences
            )
        } catch (_: MailException) {
            // Expected: appendMessages threw → watermark write unreachable
        }

        assertThat(dataTypePreferences.getMaxSyncedDate(DataType.SMS))
            .isEqualTo(DataType.Defaults.MAX_SYNCED_DATE)
    }

    /**
     * AC-3(a) variant: login failure → [MailTransport.openFolder] throws [RequiresLoginException].
     *
     * No append occurs → watermark unchanged.
     *
     * U-053: assertion is against production [BackupWorker.appendBatchAndUpdateWatermark].
     */
    @Test
    fun ac3a_loginFails_watermarkUnchanged() {
        val transport = StubMailTransport(
            openFolderBehavior = { throw RequiresLoginException() }
        )
        val worker = buildWorker(transport)

        try {
            worker.appendBatchAndUpdateWatermark(
                transport = transport,
                type = DataType.SMS,
                result = buildConversionResult(DataType.SMS, SMS_DATE_1),
                dataTypePreferences = dataTypePreferences
            )
        } catch (_: RequiresLoginException) {
            // Expected
        }

        assertThat(dataTypePreferences.getMaxSyncedDate(DataType.SMS))
            .isEqualTo(DataType.Defaults.MAX_SYNCED_DATE)
    }

    // -------------------------------------------------------------------------
    // AC-3(b): successful batch → watermark = max appended message date
    // -------------------------------------------------------------------------

    /**
     * AC-3(b): When [MailTransport.appendMessages] returns the confirmed max date, the watermark
     * is advanced to EXACTLY that value.
     *
     * The confirmed date comes FROM the return value of [appendMessages], not from any
     * separately-computed variable. This is the BUG-010 fix: the watermark is gated on
     * the confirmed return, not on an optimistic pre-computed value.
     *
     * The value SMS_DATE_1 is a real message timestamp (2023-11-14), NOT wall-clock "now".
     * This verifies that confirmed dates are message-derived, not time-derived.
     *
     * U-053: assertion is against production [BackupWorker.appendBatchAndUpdateWatermark].
     */
    @Test
    fun ac3b_successfulBatch_watermarkAdvancesToConfirmedDate() {
        val transport = StubMailTransport(confirmedDateToReturn = SMS_DATE_1)
        val worker = buildWorker(transport)

        worker.appendBatchAndUpdateWatermark(
            transport = transport,
            type = DataType.SMS,
            result = buildConversionResult(DataType.SMS, SMS_DATE_1),
            dataTypePreferences = dataTypePreferences
        )

        // Watermark must be exactly the confirmed date
        assertThat(dataTypePreferences.getMaxSyncedDate(DataType.SMS))
            .isEqualTo(SMS_DATE_1)
    }

    /**
     * AC-3(b) variant: two successive successful batches result in the watermark advancing
     * to the second (later) batch's confirmed date.
     *
     * This verifies that per-batch watermark updates are cumulative: each successful
     * batch advances the watermark independently.
     *
     * U-053: assertion is against production [BackupWorker.appendBatchAndUpdateWatermark] called twice.
     */
    @Test
    fun ac3b_twoSuccessfulBatches_watermarkAdvancesToSecondDate() {
        var callCount = 0
        val transport = StubMailTransport(
            appendMessagesBehaviorFn = {
                callCount++
                if (callCount == 1) SMS_DATE_1 else SMS_DATE_2
            }
        )
        val worker = buildWorker(transport)

        worker.appendBatchAndUpdateWatermark(
            transport = transport,
            type = DataType.SMS,
            result = buildConversionResult(DataType.SMS, SMS_DATE_1),
            dataTypePreferences = dataTypePreferences
        )
        worker.appendBatchAndUpdateWatermark(
            transport = transport,
            type = DataType.SMS,
            result = buildConversionResult(DataType.SMS, SMS_DATE_2),
            dataTypePreferences = dataTypePreferences
        )

        // After two successful batches, watermark must be SMS_DATE_2 (later date)
        assertThat(dataTypePreferences.getMaxSyncedDate(DataType.SMS))
            .isEqualTo(SMS_DATE_2)
    }

    // -------------------------------------------------------------------------
    // AC-3(c): partial success — batch 1 ok, batch 2 fails → watermark = batch 1 max
    // -------------------------------------------------------------------------

    /**
     * AC-3(c): batch 1 (SMS_DATE_1) succeeds; batch 2 (SMS_DATE_2) fails with [MailException].
     * Watermark must equal SMS_DATE_1 only — not SMS_DATE_2, not "now", not -1.
     *
     * This verifies per-batch durable checkpoint behavior (U-016 compatible):
     * - Batch 1 commits watermark = SMS_DATE_1 (durable, before batch 2 is processed)
     * - Batch 2 fails → exception propagates, no further writes
     * - Net result: watermark = SMS_DATE_1 (batch 1 committed; batch 2 pending retry)
     *
     * On the next backup run, the worker fetches messages with date > SMS_DATE_1,
     * so the second message (SMS_DATE_2) is correctly re-attempted.
     *
     * U-053: assertion is against production [BackupWorker.appendBatchAndUpdateWatermark].
     */
    @Test
    fun ac3c_partialSuccess_watermarkEqualsBatch1MaxDate() {
        var callCount = 0
        val transport = StubMailTransport(
            appendMessagesBehaviorFn = {
                callCount++
                if (callCount >= 2) {
                    throw MailException("folder closed unexpectedly on second append")
                }
                SMS_DATE_1
            }
        )
        val worker = buildWorker(transport)

        // Batch 1 — succeeds, watermark = SMS_DATE_1
        worker.appendBatchAndUpdateWatermark(
            transport = transport,
            type = DataType.SMS,
            result = buildConversionResult(DataType.SMS, SMS_DATE_1),
            dataTypePreferences = dataTypePreferences
        )

        // Batch 2 — fails, watermark must not advance
        try {
            worker.appendBatchAndUpdateWatermark(
                transport = transport,
                type = DataType.SMS,
                result = buildConversionResult(DataType.SMS, SMS_DATE_2),
                dataTypePreferences = dataTypePreferences
            )
        } catch (_: MailException) {
            // Expected: batch 2 threw
        }

        // Watermark must be exactly SMS_DATE_1 (batch 1 committed)
        assertThat(dataTypePreferences.getMaxSyncedDate(DataType.SMS))
            .isEqualTo(SMS_DATE_1)

        // Guard: watermark must NOT be SMS_DATE_2 (the failing batch must not advance the watermark)
        assertThat(dataTypePreferences.getMaxSyncedDate(DataType.SMS))
            .isNotEqualTo(SMS_DATE_2)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a [ConversionResult] for [type] containing one mock message with
     * [date] in its DATE header. This causes [ConversionResult.getMaxDate] to return [date].
     */
    private fun buildConversionResult(type: DataType, date: Long): ConversionResult {
        val result = ConversionResult(type)
        val mockMsg = mock(ImapMessage::class.java)
        `when`(mockMsg.getHeader(Headers.DATE)).thenReturn(arrayOf(date.toString()))
        result.add(mockMsg, emptyMap())
        return result
    }
}

// ---------------------------------------------------------------------------
// StubMailTransport — configurable MailTransport stub for watermark tests
// ---------------------------------------------------------------------------

/**
 * Configurable stub [MailTransport] for [BackupWorkerWatermarkTest].
 *
 * Configuration options:
 * - [openFolderBehavior]: invoked in [openFolder]; throw here to simulate folder failure
 * - [appendMessagesBehavior]: invoked in [appendMessages] BEFORE the return; throw here
 *   to simulate append failure without a confirmedDate
 * - [appendMessagesBehaviorFn]: if set, called to produce the confirmed date return value;
 *   takes precedence over [confirmedDateToReturn]
 * - [confirmedDateToReturn]: the fixed return value for [appendMessages]; if null, returns
 *   result.getMaxDate() (the conversion result's own max date)
 *
 * All other methods are no-ops.
 */
class StubMailTransport(
    private val openFolderBehavior: (() -> Unit)? = null,
    private val appendMessagesBehavior: (() -> Unit)? = null,
    private val appendMessagesBehaviorFn: (() -> Long)? = null,
    private val confirmedDateToReturn: Long? = null
) : MailTransport {

    override fun checkSettings() { /* no-op */ }

    override fun openFolder(type: DataType?, prefs: DataTypePreferences?): BackupFolderHandle {
        openFolderBehavior?.invoke()
        return MailTransportTestFactories.createFolderHandle()
    }

    override fun appendMessages(folder: BackupFolderHandle?, result: ConversionResult?): Long {
        appendMessagesBehavior?.invoke()
        return appendMessagesBehaviorFn?.invoke()
            ?: confirmedDateToReturn
            ?: (result?.getMaxDate() ?: DataType.Defaults.MAX_SYNCED_DATE)
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
    ): MessageImportResult = MessageImportResult.failure(handle?.uid ?: "unknown")
}
