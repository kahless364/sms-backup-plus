package com.zegoggles.smssync.service

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.fsck.k9.mail.store.imap.ImapMessage
import com.google.common.truth.Truth.assertThat
import com.zegoggles.smssync.contacts.ContactGroup
import com.zegoggles.smssync.mail.ConversionResult
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.mail.Headers
import com.zegoggles.smssync.mail.MessageConverter
import com.zegoggles.smssync.mail.transport.MailTransport
import com.zegoggles.smssync.preferences.Preferences
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.stubbing.Answer
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.EnumSet

/**
 * U-067 (BUG-022): the backup loop's cancellation check in [BackupWorker.backupCursors]
 * (site 1) observes the job of the coroutine that runs the loop.
 *
 * Under the unit-test harness setProgress is never a cancellation point (work-testing's
 * progress updater returns a completed future, and WorkManager 2.9.1 returns a completed
 * future without looking at the job), so the loop's own check is the only place the loop can
 * stop. Each test runs backupCursors in a child coroutine that it holds (launch LAZY, then
 * join), so cancelling that child is what WorkManager's stop does to the worker's job.
 * The cancelling test cancels the child from inside the transport's second append; the pass
 * in flight completes, and the check at the top of the next pass stops the loop.
 *
 * Not asserted: which Result a cancelled run returns (BUG-022 SC-1). Not observable here:
 * the closing of the cursors, which is done by the caller fetchAndBackupItems, outside the seam.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class BackupWorkerLoopCancellationTest {

    private lateinit var context: Context
    private lateinit var preferences: Preferences

    companion object {
        /** One SMS row per pass; each row's date is the date its conversion result carries. */
        private val ROW_DATES = listOf(1_700_000_101_000L, 1_700_000_102_000L, 1_700_000_103_000L, 1_700_000_104_000L)
    }

    /** Counters of one backup run. */
    private class BackupRun {
        var converts = 0
        var appends = 0
        var closes = 0
        var result: ListenableWorker.Result? = null
    }

    @Before
    fun setUp() {
        @Suppress("DEPRECATION")
        context = RuntimeEnvironment.application
        preferences = Preferences(context)
        preferences.dataTypePreferences.setMaxSyncedDate(DataType.SMS, DataType.Defaults.MAX_SYNCED_DATE)
    }

    /**
     * AC-1: four rows; the job is cancelled during the second append. Exactly two conversions
     * and two appends happen, the cursor stays on the second row, the watermark is the date the
     * second append confirmed, closeFolders runs once, and no completed run is reported.
     */
    @Test
    fun backupCursors_cancelledAfterSecondAppend_stopsAtNextPass() = runBlocking<Unit> {
        val run = BackupRun()
        lateinit var workJob: Job
        val transport = countingTransport(run, cancelOnAppend = 2) { workJob.cancel() }
        val cursor = smsCursor()
        val cursors = BackupCursors().apply { add(DataType.SMS, cursor) }
        val worker = buildWorker(transport)
        val converter = dateEchoConverter(run)

        workJob = launch(start = CoroutineStart.LAZY) {
            run.result = worker.backupCursors(cursors, backupConfig(transport), preferences, converter, null, cursors.count())
        }
        workJob.join()

        assertThat(workJob.isCancelled).isTrue()
        assertThat(run.appends).isEqualTo(2)
        assertThat(run.converts).isEqualTo(2)
        assertThat(cursor.position).isEqualTo(1)
        assertThat(preferences.dataTypePreferences.getMaxSyncedDate(DataType.SMS)).isEqualTo(ROW_DATES[1])
        assertThat(run.closes).isEqualTo(1)
        val result = run.result
        val completedEveryRow = result is ListenableWorker.Result.Success &&
            result.outputData.getInt(BackupWorker.PROGRESS_KEY_BACKED_UP, -1) == ROW_DATES.size
        assertThat(completedEveryRow).isFalse()
        cursors.close()
    }

    /**
     * AC-5 control: the same fixture and harness with no cancellation backs up every row,
     * leaves the watermark at the last confirmed date and reports a finished run.
     */
    @Test
    fun backupCursors_notCancelled_appendsEveryRow() = runBlocking<Unit> {
        val run = BackupRun()
        lateinit var workJob: Job
        val transport = countingTransport(run, cancelOnAppend = 0) { workJob.cancel() }
        val cursor = smsCursor()
        val cursors = BackupCursors().apply { add(DataType.SMS, cursor) }
        val worker = buildWorker(transport)
        val converter = dateEchoConverter(run)

        workJob = launch(start = CoroutineStart.LAZY) {
            run.result = worker.backupCursors(cursors, backupConfig(transport), preferences, converter, null, cursors.count())
        }
        workJob.join()

        assertThat(workJob.isCancelled).isFalse()
        assertThat(run.appends).isEqualTo(ROW_DATES.size)
        assertThat(run.converts).isEqualTo(ROW_DATES.size)
        assertThat(cursor.position).isEqualTo(ROW_DATES.size - 1)
        assertThat(preferences.dataTypePreferences.getMaxSyncedDate(DataType.SMS)).isEqualTo(ROW_DATES.last())
        assertThat(run.closes).isEqualTo(1)
        val result = run.result
        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        val data = (result as ListenableWorker.Result.Success).outputData
        assertThat(data.getInt(BackupWorker.PROGRESS_KEY_BACKED_UP, -1)).isEqualTo(ROW_DATES.size)
        assertThat(data.getString(BackupWorker.PROGRESS_KEY_STATE)).isEqualTo(BackupWorker.STATE_FINISHED)
        cursors.close()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildWorker(transport: MailTransport): BackupWorker =
        TestListenableWorkerBuilder<BackupWorker>(context)
            .setWorkerFactory(BackupWorker.TestableBackupWorkerFactoryWithTransport(transport))
            .build() as BackupWorker

    private fun backupConfig(transport: MailTransport): BackupConfig =
        BackupConfig(transport, 0, ROW_DATES.size, ContactGroup(-1L), BackupType.MANUAL, EnumSet.of(DataType.SMS), false)

    /** An SMS cursor with one row per entry of [ROW_DATES]. */
    private fun smsCursor(): MatrixCursor =
        MatrixCursor(arrayOf("_id", "date")).apply {
            ROW_DATES.forEachIndexed { i, date -> addRow(arrayOf<Any>(i + 1L, date)) }
        }

    /**
     * [StubMailTransport] (confirmed date = the result's max date), counting appends and
     * closeFolders; on append number [cancelOnAppend] (1-based; 0 = never) [onCancel] runs
     * before the append returns, so the pass in flight completes.
     */
    private fun countingTransport(run: BackupRun, cancelOnAppend: Int, onCancel: () -> Unit): MailTransport {
        val stub = StubMailTransport(appendMessagesBehavior = {
            run.appends++
            if (run.appends == cancelOnAppend) onCancel()
        })
        return object : MailTransport by stub {
            override fun closeFolders() {
                run.closes++
                stub.closeFolders()
            }
        }
    }

    /**
     * A converter that returns, for the cursor's current row, a one-message result dated
     * ROW_DATES[row]. Mocked, so the test does not depend on the real converter's columns.
     */
    private fun dateEchoConverter(run: BackupRun): MessageConverter {
        val results = ROW_DATES.map { conversionResult(it) }
        return mock(MessageConverter::class.java, Answer<Any?> { invocation ->
            if (invocation.method.name == "convertMessages") {
                run.converts++
                results[invocation.getArgument<Cursor>(0).position]
            } else {
                Mockito.RETURNS_DEFAULTS.answer(invocation)
            }
        })
    }

    private fun conversionResult(date: Long): ConversionResult {
        val result = ConversionResult(DataType.SMS)
        val message = mock(ImapMessage::class.java)
        `when`(message.getHeader(Headers.DATE)).thenReturn(arrayOf(date.toString()))
        result.add(message, emptyMap())
        return result
    }
}
