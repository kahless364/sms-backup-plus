package com.zegoggles.smssync.service

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.Telephony
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.zegoggles.smssync.Consts
import com.zegoggles.smssync.preferences.Preferences
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

/**
 * Fault-injection tests for the durable restore checkpoint (U-016).
 *
 * Verifies the canonical AC-4 requirement: given N distinct SMS messages, crash after K
 * successful inserts, re-execute the worker, and assert final row count == N with no duplicates.
 *
 * Also verifies AC-1..AC-7 individually.
 *
 * **Test architecture (IC-4):**
 * - [InMemoryCheckpointStore] — cross-execution store that persists between worker instances,
 *   simulating the durable SharedPreferences backing.
 * - [RestoreInsertInterceptor.CrashAfterK] — throws [RestoreInsertInterceptor.SimulatedCrashException]
 *   after K confirmed inserts, simulating a process kill.
 * - [RestoreWorker.TestableRestoreWorkerFactory] — injects both into the worker via
 *   [TestListenableWorkerBuilder.setWorkerFactory].
 * - [RestoreWorker.executeRestoreWithValues] — the test-path restore loop that bypasses the
 *   IMAP fetch and operates directly on [ContentValues] items (the fault-injection test seam).
 * - [FakeSmsContentProvider] — an in-memory SMS provider registered via Robolectric's
 *   [ShadowContentResolver.registerProviderInternal], enabling real insert/query operations
 *   without a full Android system.
 *
 * **Provider note:** Robolectric 4.12 does not include a working shadow of the SMS system
 * content provider. [FakeSmsContentProvider] is registered for "sms" authority to handle
 * inserts/queries with date+address+type dedup semantics matching the real smsExists() guard.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class RestoreWorkerCheckpointTest {

    private lateinit var context: Context
    private lateinit var checkpointStore: InMemoryCheckpointStore
    private lateinit var preferences: Preferences
    private lateinit var fakeProvider: FakeSmsContentProvider

    @Before
    fun setUp() {
        @Suppress("DEPRECATION")
        context = RuntimeEnvironment.application
        checkpointStore = InMemoryCheckpointStore()
        preferences = Preferences(context)

        // Register fake SMS content provider for "sms" authority
        fakeProvider = FakeSmsContentProvider()
        fakeProvider.attachInfo(context, null)
        ShadowContentResolver.registerProviderInternal("sms", fakeProvider)
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Creates N distinct SMS ContentValues with unique date/address combinations. */
    private fun createSmsItems(n: Int): List<ContentValues> {
        return (0 until n).map { i ->
            ContentValues().apply {
                put(Telephony.TextBasedSmsColumns.DATE, (1_700_000_000_000L + i * 1000))
                put(Telephony.TextBasedSmsColumns.ADDRESS, "+1555000${i.toString().padStart(4, '0')}")
                put(Telephony.TextBasedSmsColumns.BODY, "Test message $i")
                put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX)
                put(Telephony.TextBasedSmsColumns.READ, 1)
            }
        }
    }

    /** Counts rows in the fake SMS provider. */
    private fun countSmsRows(): Int = fakeProvider.rowCount()

    /** Returns duplicate date+address+type keys in the fake SMS provider. */
    private fun findDuplicateRows(): List<String> = fakeProvider.findDuplicates()

    /**
     * Builds a [RestoreWorker] with the given checkpoint store and interceptor.
     */
    private fun buildWorker(
        store: RestoreCheckpointStore = checkpointStore,
        interceptor: RestoreInsertInterceptor = RestoreInsertInterceptor.NoOp
    ): RestoreWorker {
        val factory = RestoreWorker.TestableRestoreWorkerFactory(store, interceptor)
        return TestListenableWorkerBuilder<RestoreWorker>(context)
            .setWorkerFactory(factory)
            .build() as RestoreWorker
    }

    // -----------------------------------------------------------------------
    // AC-4 (canonical): N=20, K=7 fault-injection test
    // -----------------------------------------------------------------------

    /**
     * AC-4 canonical fault-injection test.
     *
     * Seeds N=20 distinct SMS messages. Injects a crash (SimulatedCrashException) after
     * exactly K=7 successful inserts. Then re-executes the worker and asserts:
     * - final SMS provider row count == 20
     * - no date+address+type duplicate rows
     * - WorkInfo.State terminal == SUCCEEDED (via Result.Success)
     *
     * How the checkpoint math works with CrashAfterK(7):
     * - Items 0..6 are inserted (7 inserts = K)
     * - For each insert: write(workName, i) is called BEFORE afterInsert(i)
     * - So at the moment of crash: checkpoint = 6 (last write before crash triggers)
     * - On re-execution: start from checkpoint+1 = 7
     * - Items 7..19 are inserted (13 inserts)
     * - Final count = 7 + 13 = 20
     */
    @Test
    fun ac4_faultInjection_N20_K7_countEqualsN_noDuplicates() = runBlocking {
        val n = 20
        val k = 7
        val items = createSmsItems(n)
        val workName = "test_restore_fault_injection"

        // --- First execution: crash after K=7 inserts ---
        val crashInterceptor = RestoreInsertInterceptor.CrashAfterK(k)
        val firstWorker = buildWorker(interceptor = crashInterceptor)

        try {
            firstWorker.executeRestoreWithValues(items, 0, preferences, context, workName)
            // Should not reach here; CrashAfterK throws after K inserts
        } catch (_: RestoreInsertInterceptor.SimulatedCrashException) {
            // Simulated crash — expected
        }

        // After crash: K rows should be in the provider (items 0..K-1)
        val rowsAfterCrash = countSmsRows()
        assertThat(rowsAfterCrash).isEqualTo(k)

        // Checkpoint holds index K-1 (last confirmed insert before crash)
        val checkpointAfterCrash = checkpointStore.read(workName)
        assertThat(checkpointAfterCrash).isEqualTo(k - 1)

        // --- Second execution (simulates WorkManager re-execution after process death) ---
        val secondWorker = buildWorker(interceptor = RestoreInsertInterceptor.NoOp)
        val secondResult = secondWorker.executeRestoreWithValues(items, 0, preferences, context, workName)

        // Worker must succeed
        assertThat(secondResult).isInstanceOf(ListenableWorker.Result.Success::class.java)

        // Final row count must be exactly N
        assertThat(countSmsRows()).isEqualTo(n)

        // No duplicate rows (date+address+type unique)
        assertThat(findDuplicateRows()).isEmpty()

        // Checkpoint cleared on SUCCEEDED (AC-6)
        assertThat(checkpointStore.read(workName)).isEqualTo(RestoreCheckpointStore.NO_CHECKPOINT)
    }

    // -----------------------------------------------------------------------
    // AC-1: Resume after process kill
    // -----------------------------------------------------------------------

    /**
     * AC-1: Worker resumes from checkpoint after simulated process kill.
     * Seed 10 messages, commit 5, simulate crash, re-execute, assert 10 rows.
     */
    @Test
    fun ac1_resumeAfterCrash_10messages_crash5_finalCount10() = runBlocking {
        val n = 10
        val k = 5
        val items = createSmsItems(n)
        val workName = "test_restore_ac1"

        // First execution — crash after K inserts
        try {
            buildWorker(interceptor = RestoreInsertInterceptor.CrashAfterK(k))
                .executeRestoreWithValues(items, 0, preferences, context, workName)
        } catch (_: RestoreInsertInterceptor.SimulatedCrashException) { /* expected */ }

        assertThat(countSmsRows()).isEqualTo(k)
        assertThat(checkpointStore.read(workName)).isEqualTo(k - 1)

        // Second execution (resume)
        val result = buildWorker(interceptor = RestoreInsertInterceptor.NoOp)
            .executeRestoreWithValues(items, 0, preferences, context, workName)

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        assertThat(countSmsRows()).isEqualTo(n)
        assertThat(findDuplicateRows()).isEmpty()
    }

    // -----------------------------------------------------------------------
    // AC-2: Checkpoint write ordering
    // -----------------------------------------------------------------------

    /**
     * AC-2: Checkpoint is written AFTER insert and BEFORE loop advances.
     *
     * Uses a RecordingCheckpointStore that logs write() calls and an interceptor that
     * reads back the checkpoint to verify it equals the insert index at that moment.
     */
    @Test
    fun ac2_checkpointWrittenAfterInsert_beforeLoopAdvance() = runBlocking {
        val items = createSmsItems(3)
        val workName = "test_restore_ac2"
        val writeLog = mutableListOf<Int>()
        val checkpointsAtInterceptTime = mutableListOf<Int>()

        val recordingStore = object : InMemoryCheckpointStore() {
            override suspend fun write(workName: String, index: Int) {
                super.write(workName, index)
                writeLog.add(index)
            }
        }

        val interceptor = object : RestoreInsertInterceptor {
            override suspend fun afterInsert(insertIndex: Int) {
                val cp = recordingStore.read(workName)
                checkpointsAtInterceptTime.add(cp)
            }
        }

        val factory = RestoreWorker.TestableRestoreWorkerFactory(recordingStore, interceptor)
        val worker = TestListenableWorkerBuilder<RestoreWorker>(context)
            .setWorkerFactory(factory)
            .build() as RestoreWorker

        worker.executeRestoreWithValues(items, 0, preferences, context, workName)

        // writeLog should have entries [0, 1, 2]
        assertThat(writeLog).containsExactly(0, 1, 2).inOrder()

        // At interceptor call time, checkpoint already equals the insert index
        assertThat(checkpointsAtInterceptTime).containsExactly(0, 1, 2).inOrder()
    }

    // -----------------------------------------------------------------------
    // AC-3: Zero duplicates across crash window
    // -----------------------------------------------------------------------

    /**
     * AC-3: Zero duplicates even with crash BEFORE checkpoint write (worst-case crash window).
     *
     * Simulates: item 2 is inserted into the provider, but the checkpoint write for item 2
     * throws (process death between insert and checkpoint write). On re-execution, item 2 is
     * re-encountered but the smsExists() dedup guard prevents a duplicate insert.
     */
    @Test
    fun ac3_zeroDuplicates_crashWindowAfterInsertBeforeCheckpoint() = runBlocking {
        val n = 5
        val items = createSmsItems(n)
        val workName = "test_restore_ac3"

        val crashWindowStore = object : InMemoryCheckpointStore() {
            var dropIndex: Int? = 2
            override suspend fun write(workName: String, index: Int) {
                if (index == dropIndex) {
                    // Crash BEFORE checkpoint write — item 2 is in the provider but NOT checkpointed
                    throw RestoreInsertInterceptor.SimulatedCrashException(
                        "Crash before write for index $index"
                    )
                }
                super.write(workName, index)
            }
        }

        val factory = RestoreWorker.TestableRestoreWorkerFactory(crashWindowStore, RestoreInsertInterceptor.NoOp)

        try {
            (TestListenableWorkerBuilder<RestoreWorker>(context)
                .setWorkerFactory(factory).build() as RestoreWorker)
                .executeRestoreWithValues(items, 0, preferences, context, workName)
        } catch (_: RestoreInsertInterceptor.SimulatedCrashException) { /* expected */ }

        // After crash: items 0..2 inserted (checkpoint only covers 0..1 since write(2) crashed)
        val rowsAfterCrash = countSmsRows()
        assertThat(rowsAfterCrash).isAtLeast(2)

        val checkpointAfterCrash = crashWindowStore.read(workName)
        assertThat(checkpointAfterCrash).isAtMost(1)  // write(2) was dropped

        // Second execution — resume from checkpoint+1, items 2+ re-encountered
        crashWindowStore.dropIndex = null
        val result = (TestListenableWorkerBuilder<RestoreWorker>(context)
            .setWorkerFactory(factory).build() as RestoreWorker)
            .executeRestoreWithValues(items, 0, preferences, context, workName)

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        // Final count: exactly N (smsExists() dedup prevents re-inserting items 0..2)
        assertThat(countSmsRows()).isEqualTo(n)
        assertThat(findDuplicateRows()).isEmpty()
    }

    // -----------------------------------------------------------------------
    // AC-5: Resume from checkpoint, not from startIndex=0
    // -----------------------------------------------------------------------

    /**
     * AC-5: Worker resumes from checkpoint value K, NOT from startIndex=0.
     *
     * Pre-write checkpoint=5 before worker executes (simulates a prior partial execution).
     * Worker should start from index 6 (checkpoint+1), skipping items 0..5.
     * Since items 0..5 were NOT actually inserted (we only wrote the checkpoint), the
     * final count = items 6..9 = 4 rows.
     */
    @Test
    fun ac5_resumesFromCheckpoint_notFromZero() = runBlocking {
        val n = 10
        val k = 5
        val items = createSmsItems(n)
        val workName = "test_restore_ac5"

        // Pre-write checkpoint to k=5 (simulates items 0..5 were committed in a prior run)
        checkpointStore.write(workName, k)

        val result = buildWorker().executeRestoreWithValues(items, 0, preferences, context, workName)

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        // Started from k+1=6, so items 6..9 = 4 rows inserted
        assertThat(countSmsRows()).isEqualTo(n - k - 1)

        // Checkpoint cleared on SUCCEEDED (AC-6)
        assertThat(checkpointStore.read(workName)).isEqualTo(RestoreCheckpointStore.NO_CHECKPOINT)
    }

    // -----------------------------------------------------------------------
    // AC-6: Checkpoint cleared on SUCCEEDED
    // -----------------------------------------------------------------------

    /**
     * AC-6: Checkpoint is cleared after a full successful restore.
     */
    @Test
    fun ac6_checkpointClearedOnSucceeded() = runBlocking {
        val items = createSmsItems(5)
        val workName = "test_restore_ac6"

        val result = buildWorker().executeRestoreWithValues(items, 0, preferences, context, workName)

        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        assertThat(countSmsRows()).isEqualTo(5)
        assertThat(checkpointStore.read(workName)).isEqualTo(RestoreCheckpointStore.NO_CHECKPOINT)
    }

    // -----------------------------------------------------------------------
    // AC-7: Checkpoint retained on interrupted restore
    // -----------------------------------------------------------------------

    /**
     * AC-7: Checkpoint value is retained when the restore is interrupted (not SUCCEEDED).
     *
     * After 4 inserts a crash occurs; checkpoint should hold value 3 (last confirmed index).
     */
    @Test
    fun ac7_checkpointRetainedOnInterruption_notClearedOnCrash() = runBlocking {
        val items = createSmsItems(10)
        val workName = "test_restore_ac7"

        try {
            buildWorker(interceptor = RestoreInsertInterceptor.CrashAfterK(4))
                .executeRestoreWithValues(items, 0, preferences, context, workName)
        } catch (_: RestoreInsertInterceptor.SimulatedCrashException) { /* expected */ }

        // Checkpoint holds index 3 (items 0,1,2,3 committed; crash after 4th insert at K=4)
        assertThat(checkpointStore.read(workName)).isEqualTo(3)
    }

    // -----------------------------------------------------------------------
    // Idempotency
    // -----------------------------------------------------------------------

    /**
     * Idempotency: running the restore twice with the same items produces exactly N rows,
     * no duplicates, thanks to the smsExists() dedup guard.
     */
    @Test
    fun idempotency_secondRunNoDuplicates() = runBlocking {
        val items = createSmsItems(5)
        val workName = "test_restore_idempotency"

        buildWorker().executeRestoreWithValues(items, 0, preferences, context, workName)
        assertThat(countSmsRows()).isEqualTo(5)

        // Reset checkpoint (simulate fresh start)
        checkpointStore.clear(workName)

        // Second run — all items already exist; smsExists() blocks all inserts
        buildWorker().executeRestoreWithValues(items, 0, preferences, context, workName)

        assertThat(countSmsRows()).isEqualTo(5)
        assertThat(findDuplicateRows()).isEmpty()
    }

    // -----------------------------------------------------------------------
    // InMemoryCheckpointStore unit tests
    // -----------------------------------------------------------------------

    @Test
    fun inMemoryStore_read_returnsNoCheckpointWhenEmpty() = runBlocking {
        val store = InMemoryCheckpointStore()
        assertThat(store.read("any")).isEqualTo(RestoreCheckpointStore.NO_CHECKPOINT)
    }

    @Test
    fun inMemoryStore_write_thenRead_returnsValue() = runBlocking {
        val store = InMemoryCheckpointStore()
        store.write("wn", 7)
        assertThat(store.read("wn")).isEqualTo(7)
    }

    @Test
    fun inMemoryStore_clear_resetsToNoCheckpoint() = runBlocking {
        val store = InMemoryCheckpointStore()
        store.write("wn", 12)
        store.clear("wn")
        assertThat(store.read("wn")).isEqualTo(RestoreCheckpointStore.NO_CHECKPOINT)
    }

    // -----------------------------------------------------------------------
    // CrashAfterK unit tests
    // -----------------------------------------------------------------------

    @Test
    fun crashAfterK_doesNotThrowBefore_K() = runBlocking {
        val interceptor = RestoreInsertInterceptor.CrashAfterK(3)
        interceptor.afterInsert(0)
        interceptor.afterInsert(1)
        // Call 3 would throw; test passes if no exception thrown for 1 and 2
    }

    @Test
    fun crashAfterK_throwsOnKthCall() = runBlocking {
        val interceptor = RestoreInsertInterceptor.CrashAfterK(2)
        interceptor.afterInsert(0)
        try {
            interceptor.afterInsert(1)
            assertThat(false).isTrue()  // should not reach
        } catch (e: RestoreInsertInterceptor.SimulatedCrashException) {
            assertThat(e).isNotNull()
        }
    }

    // -----------------------------------------------------------------------
    // SharedPreferencesCheckpointStore
    // -----------------------------------------------------------------------

    @Test
    fun sharedPrefsStore_write_thenRead_roundTrip() = runBlocking {
        val store = SharedPreferencesCheckpointStore(context)
        store.clear("test_prefs_store")
        store.write("test_prefs_store", 42)
        assertThat(store.read("test_prefs_store")).isEqualTo(42)
        store.clear("test_prefs_store")
    }

    @Test
    fun sharedPrefsStore_clear_resetsToNoCheckpoint() = runBlocking {
        val store = SharedPreferencesCheckpointStore(context)
        store.write("test_prefs_clear", 99)
        store.clear("test_prefs_clear")
        assertThat(store.read("test_prefs_clear")).isEqualTo(RestoreCheckpointStore.NO_CHECKPOINT)
    }
}

// ---------------------------------------------------------------------------
// Test helpers
// ---------------------------------------------------------------------------

/**
 * In-memory [RestoreCheckpointStore] for tests.
 * Persists across worker instances (shared reference in the test case).
 */
open class InMemoryCheckpointStore : RestoreCheckpointStore {
    private val data = mutableMapOf<String, Int>()

    override suspend fun read(workName: String): Int =
        data[workName] ?: RestoreCheckpointStore.NO_CHECKPOINT

    override suspend fun write(workName: String, index: Int) {
        data[workName] = index
    }

    override suspend fun clear(workName: String) {
        data.remove(workName)
    }
}

/**
 * In-memory [ContentProvider] that implements insert/query for the SMS authority.
 * Registered via [ShadowContentResolver.registerProviderInternal] in test setUp.
 *
 * Implements the smsExists() dedup key (date + address + type) so that the
 * [RestoreWorker.smsExists] guard produces correct results during tests.
 *
 * Each inserted row is stored as a [ContentValues] in [rows]. Insert returns a URI with
 * a synthetic ID. Query with the smsExists() selection returns matching rows.
 */
class FakeSmsContentProvider : ContentProvider() {

    private val rows = mutableListOf<ContentValues>()
    private var nextId = 1L

    fun rowCount(): Int = rows.size

    fun findDuplicates(): List<String> {
        val seen = mutableSetOf<String>()
        val duplicates = mutableListOf<String>()
        for (row in rows) {
            val date = row.getAsString(Telephony.TextBasedSmsColumns.DATE) ?: ""
            val address = row.getAsString(Telephony.TextBasedSmsColumns.ADDRESS) ?: ""
            val type = row.getAsString(Telephony.TextBasedSmsColumns.TYPE) ?: ""
            val key = "$date|$address|$type"
            if (!seen.add(key)) duplicates.add(key)
        }
        return duplicates
    }

    override fun onCreate(): Boolean = true

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (values == null) return null
        val copy = ContentValues(values)
        val id = nextId++
        copy.put("_id", id)
        rows.add(copy)
        return Uri.parse("content://sms/$id")
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val projCols = projection ?: arrayOf("_id", "date", "address", "type", "body")
        val cursor = MatrixCursor(projCols)

        val matchingRows = if (selection == null) {
            rows
        } else {
            rows.filter { row -> matchesSelection(row, selection, selectionArgs) }
        }

        for (row in matchingRows) {
            val rowData = projCols.map { col ->
                when (col) {
                    "_id" -> row.getAsString("_id")
                    Telephony.TextBasedSmsColumns.DATE -> row.getAsString(Telephony.TextBasedSmsColumns.DATE)
                    Telephony.TextBasedSmsColumns.ADDRESS -> row.getAsString(Telephony.TextBasedSmsColumns.ADDRESS)
                    Telephony.TextBasedSmsColumns.TYPE -> row.getAsString(Telephony.TextBasedSmsColumns.TYPE)
                    Telephony.TextBasedSmsColumns.BODY -> row.getAsString(Telephony.TextBasedSmsColumns.BODY)
                    else -> row.getAsString(col)
                }
            }
            cursor.addRow(rowData)
        }
        return cursor
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun getType(uri: Uri): String? = null

    /**
     * Evaluates the smsExists() selection fragment against [row].
     * Handles "date = ? AND address = ? AND type = ?" (3-arg form).
     */
    private fun matchesSelection(row: ContentValues, selection: String, args: Array<out String>?): Boolean {
        if (args == null) return false

        return when {
            selection.contains("date = ? AND address = ? AND type = ?") && args.size >= 3 -> {
                row.getAsString(Telephony.TextBasedSmsColumns.DATE) == args[0] &&
                row.getAsString(Telephony.TextBasedSmsColumns.ADDRESS) == args[1] &&
                row.getAsString(Telephony.TextBasedSmsColumns.TYPE) == args[2]
            }
            else -> false
        }
    }
}
