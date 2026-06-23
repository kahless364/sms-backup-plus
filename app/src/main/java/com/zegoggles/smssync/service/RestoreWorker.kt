/*
 * Copyright (c) 2024 SMS Backup+
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zegoggles.smssync.service

import android.app.Notification
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.zegoggles.smssync.App
import com.zegoggles.smssync.R
import com.zegoggles.smssync.activity.MainActivity
// U-026: all k-9 imports removed; engine now uses app-owned ACL types
// U-038 (BUG-006): removed five unused adapter/transport imports: BackupImapStore,
//   PinnedCertStore, TlsTrustPolicy, K9MailTransport, MailTransportConfig.
//   None appear in any executable expression in this file (KDoc mentions do not count).
import com.zegoggles.smssync.Consts
import com.zegoggles.smssync.auth.TokenRefreshException
import com.zegoggles.smssync.auth.TokenRefresher
import com.zegoggles.smssync.contacts.ContactAccessor
import com.zegoggles.smssync.di.MailTransportFactory
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.mail.MessageConverter
import com.zegoggles.smssync.mail.PersonLookup
import com.zegoggles.smssync.mail.transport.BackupFolderHandle
import com.zegoggles.smssync.mail.transport.MailException
import com.zegoggles.smssync.mail.transport.MailMessageHandle
import com.zegoggles.smssync.mail.transport.MailTransport
import com.zegoggles.smssync.mail.transport.XOAuth2FailedException
import com.zegoggles.smssync.preferences.AuthPreferences
import com.zegoggles.smssync.preferences.Preferences
import com.zegoggles.smssync.scheduler.WorkManagerScheduler
import com.zegoggles.smssync.service.exception.RequiresLoginException
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ensureActive
import java.util.ArrayList
import java.util.HashSet
import kotlin.coroutines.coroutineContext

/**
 * Real CoroutineWorker restore implementation (U-015 + U-016).
 *
 * U-026: Rewired from BackupImapStore (k-9 type) to
 * [MailTransport] (app-owned ACL port). No k-9 transport or MIME import remains in this
 * file. [buildMailTransport] replaces the former [buildImapStore].
 *
 * This worker contains the restore execution logic ported from [RestoreTask] (AsyncTask-based).
 * It preserves all RestoreTask behaviors:
 *   - early-exit when both restoreSms and restoreCallLog are false
 *   - restore loop starting from checkpoint store (U-016) or config.currentRestoredItem
 *   - smsExists() dedup guard: date+address+type (AC-11a / RestoreTask.java:308-327, :259)
 *   - callLogExists() dedup guard: date+number+duration+type (AC-11b / RestoreTask.java:288-306, :280)
 *   - SMS type filter: INBOX and SENT only (RestoreTask.java:256-258)
 *   - thread update after any SMS is restored (RestoreTask.java:129)
 *   - XOAuth2 token-refresh retry, at most once (RestoreTask.java:157-177)
 *   - Cooperative cancellation via ensureActive() (AC-5)
 *
 * **U-016 Durable Checkpoint:**
 * The [checkpointStore] is read once on entry to establish the resume offset (AC-5).
 * After each confirmed provider insert at index i, [RestoreCheckpointStore.write] is called
 * BEFORE advancing the loop index — the load-bearing write-ordering invariant (AC-2).
 * On SUCCEEDED terminal state [RestoreCheckpointStore.clear] is called (AC-6).
 * On CANCELLED the checkpoint is NOT cleared so re-schedule can resume (AC-7).
 *
 * **Fault-injection seam (IC-4):**
 * The [checkpointStore] and [insertInterceptor] are constructor parameters, enabling test
 * doubles that control crash-after-K behaviour without touching production code paths.
 * [TestableRestoreWorkerFactory] supplies these in tests.
 *
 * **Test path (executeRestoreWithValues):**
 * Fault-injection tests call [executeRestoreWithValues] directly with pre-built ContentValues
 * items, bypassing the IMAP fetch. This is the canonical fault-injection test seam.
 *
 * U-024: Annotated @HiltWorker with @AssistedInject constructor. @Assisted Context and
 * @Assisted WorkerParameters are supplied by WorkManager via HiltWorkerFactory; the
 * remaining constructor params (preferences, authPreferences, mailTransportFactory,
 * checkpointStore, insertInterceptor, contactAccessor, engineFactory) are supplied by
 * the Hilt SingletonComponent graph.
 *
 * U-050 AR-004: MessageConverter, PersonLookup, and TokenRefresher are no longer hand-new-ed
 * inside doWork(). They are created per run via the injected [WorkerEngineFactory].
 * ContactAccessor is injected directly (zero-arg @Inject constructor, no per-run state).
 *
 * Fault-injection test seam ([TestableRestoreWorkerFactory]) is retained: tests that need
 * direct control over [checkpointStore] and [insertInterceptor] (U-016 checkpoint tests)
 * use [TestListenableWorkerBuilder.setWorkerFactory] with [TestableRestoreWorkerFactory],
 * which directly constructs RestoreWorker bypassing the Hilt factory. This is the
 * correct pattern for Robolectric unit tests that need to inject test doubles.
 */
@HiltWorker
class RestoreWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferences: Preferences,
    private val authPreferences: AuthPreferences,
    private val mailTransportFactory: MailTransportFactory,
    /** Durable checkpoint store — injected via Hilt; supplied by CheckpointModule. */
    internal val checkpointStore: RestoreCheckpointStore,
    /**
     * Fault-injection seam (IC-4). Production: [RestoreInsertInterceptor.NoOp] (bound by
     * CheckpointModule). Tests use [TestableRestoreWorkerFactory] to inject [CrashAfterK].
     */
    internal val insertInterceptor: RestoreInsertInterceptor,
    /** U-050 AR-004: Injected; zero-arg @Inject constructor, no per-run state. */
    private val contactAccessor: ContactAccessor,
    /** U-050 AR-004: Injected factory; creates per-run PersonLookup/MessageConverter/TokenRefresher. */
    private val engineFactory: WorkerEngineFactory
) : CoroutineWorker(context, params) {

    // Dedup tracking sets — mirrors RestoreTask.smsIds, callLogIds, uids
    private val smsIds: MutableSet<String> = HashSet()
    private val callLogIds: MutableSet<String> = HashSet()
    private val uids: MutableSet<String> = HashSet()

    override suspend fun doWork(): Result {
        // U-049 AC-2: Set foreground notification early in doWork() so the restore shows
        // a foreground-service notification on Android 12+ without ANR risk.
        // Uses the same notification channel (App.CHANNEL_ID) and content that
        // SmsRestoreService.restoreStateChanged() previously provided via startForeground().
        setForeground(createRestoreForegroundInfo())

        // INV-3 backoff cap: cap effective retry delay at 300s (same logic as BackupWorker)
        val runAttempt = runAttemptCount
        if (runAttempt > 0) {
            val effectiveDelaySecs = WorkManagerScheduler.BACKOFF_INITIAL_SECS shl runAttempt
            if (effectiveDelaySecs > WorkManagerScheduler.BACKOFF_MAX_SECS) {
                Log.w(TAG, "RestoreWorker: backoff cap exceeded at attempt $runAttempt; returning failure")
                return Result.failure(workDataOf(KEY_FAILURE_REASON to "backoff_cap_exceeded"))
            }
        }

        val ctx = applicationContext
        // U-024: preferences, authPreferences are now @AssistedInject-injected fields.

        Log.d(TAG, "RestoreWorker.doWork: starting restore, attempt=$runAttempt")

        return try {
            // U-024: mailTransportFactory.create() replaces buildMailTransport(); factory is injected.
            val transport = mailTransportFactory.create()
            val restoreSms = preferences.dataTypePreferences.isRestoreEnabled(DataType.SMS)
            val restoreCallLog = preferences.dataTypePreferences.isRestoreEnabled(DataType.CALLLOG)

            if (!restoreSms && !restoreCallLog) {
                Log.d(TAG, "RestoreWorker: nothing to restore (restoreSms=$restoreSms, restoreCallLog=$restoreCallLog)")
                return Result.success(workDataOf(PROGRESS_KEY_STATE to STATE_FINISHED))
            }

            val config = RestoreConfig(
                transport,
                0,
                restoreSms,
                restoreCallLog,
                preferences.isRestoreStarredOnly,
                preferences.maxItemsPerRestore,
                0
            )

            // U-050 AR-004: collaborators created via injected WorkerEngineFactory.
            // PersonLookup is per-run (fresh LRU cache). MessageConverter reads userEmail from
            // AuthPreferences at construction time. TokenRefresher captures current clientId.
            val personLookup = engineFactory.createPersonLookup()
            val converter = engineFactory.createMessageConverter(personLookup, contactAccessor)
            val tokenRefresher = engineFactory.createTokenRefresher()

            executeRestore(config, preferences, converter, tokenRefresher, ctx, authPreferences)
        } catch (e: MailException) {
            // U-026: MailException replaces MessagingException
            Log.w(TAG, "RestoreWorker: MailException — retrying", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "RestoreWorker: unrecoverable error", e)
            Result.failure(workDataOf(KEY_FAILURE_REASON to (e.message ?: "unknown")))
        }
    }

    /**
     * Pairs a [MailMessageHandle] with its corresponding open [BackupFolderHandle].
     * Required because [MailTransport.importMessageBody] needs both the folder and the handle;
     * the folder handle is used by [K9MailTransport.importMessageBody] to call folder.fetch().
     */
    private data class MessageWithFolder(val handle: MailMessageHandle, val folder: BackupFolderHandle)

    /**
     * IMAP-backed restore execution. Mirrors RestoreTask.restore() (RestoreTask.java:97-155).
     * Fetches messages from IMAP then delegates to [runRestoreLoop].
     *
     * U-026: uses [MailTransport.openFolder], [MailTransport.getMessages],
     * [MailTransport.closeFolders] instead of BackupImapStore k-9 types. Tracks
     * (handle, folderHandle) pairs so [importMessage] can call [MailTransport.importMessageBody]
     * with the correct folder.
     */
    private suspend fun executeRestore(
        config: RestoreConfig,
        preferences: Preferences,
        converter: MessageConverter,
        tokenRefresher: TokenRefresher,
        ctx: Context,
        authPreferences: AuthPreferences
    ): Result {
        val transport = config.imapStore
        val uniqueWorkName = inputData.getString(KEY_UNIQUE_WORK_NAME) ?: RESTORE_WORK_NAME

        try {
            setProgress(workDataOf(PROGRESS_KEY_STATE to STATE_LOGIN))
            // U-026: checkSettings via transport port
            transport.checkSettings()

            setProgress(workDataOf(PROGRESS_KEY_STATE to STATE_CALC))

            // U-026: openFolder + getMessages via transport port; no k-9 types cross this seam.
            // Track (handle, folderHandle) pairs so importMessageBody receives the correct folder.
            val msgs = ArrayList<MessageWithFolder?>()
            if (config.restoreSms) {
                val smsFolder: BackupFolderHandle = transport.openFolder(DataType.SMS, preferences.dataTypePreferences)
                for (handle in transport.getMessages(smsFolder, config.maxRestore, config.restoreOnlyStarred, null)) {
                    msgs.add(MessageWithFolder(handle, smsFolder))
                }
            }
            if (config.restoreCallLog) {
                val calllogFolder: BackupFolderHandle = transport.openFolder(DataType.CALLLOG, preferences.dataTypePreferences)
                for (handle in transport.getMessages(calllogFolder, config.maxRestore, config.restoreOnlyStarred, null)) {
                    msgs.add(MessageWithFolder(handle, calllogFolder))
                }
            }

            val itemsToRestoreCount = if (config.maxRestore <= 0) msgs.size
                                      else minOf(msgs.size, config.maxRestore)

            return runImapRestoreLoop(msgs, itemsToRestoreCount, config.currentRestoredItem,
                transport, preferences, converter, ctx, uniqueWorkName)

        } catch (e: XOAuth2FailedException) {
            // U-026: XOAuth2FailedException replaces k-9 XOAuth2AuthenticationFailedException
            return handleAuthError(config, preferences, converter, tokenRefresher, ctx,
                authPreferences, RestoreCheckpointStore.NO_CHECKPOINT, e)
        } catch (e: RequiresLoginException) {
            // U-026: RequiresLoginException replaces k-9 AuthenticationFailedException
            return Result.failure(workDataOf(KEY_FAILURE_REASON to "auth_failed"))
        } catch (e: MailException) {
            // U-026: MailException replaces k-9 MessagingException
            Log.e(TAG, "RestoreWorker: MailException", e)
            updateAllThreadsIfAnySmsRestored(ctx)
            return Result.retry()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "RestoreWorker: IllegalStateException (possible memory)", e)
            return Result.failure(workDataOf(KEY_FAILURE_REASON to "illegal_state"))
        } finally {
            // U-026: closeFolders via transport port (does not throw)
            transport.closeFolders()
        }
    }

    /**
     * Restore loop over [MessageWithFolder] pairs (IMAP message handle + open folder handle).
     * Each message is imported (fetched + converted) then inserted via [importSmsValues]/[importCallLogValues].
     * Checkpoint is written after each confirmed insert; cleared on SUCCEEDED.
     *
     * U-026: msgs list is now [MessageWithFolder?] instead of k-9 Message?; transport is
     * passed through for the [importMessage] call to [MailTransport.importMessageBody].
     */
    private suspend fun runImapRestoreLoop(
        msgs: ArrayList<MessageWithFolder?>,
        itemsToRestoreCount: Int,
        initialRestoredItem: Int,
        transport: MailTransport,
        preferences: Preferences,
        converter: MessageConverter,
        ctx: Context,
        uniqueWorkName: String
    ): Result {
        // U-016 AC-5: Read durable checkpoint; authoritative over config.currentRestoredItem.
        val checkpointValue = checkpointStore.read(uniqueWorkName)
        var currentRestoredItem = if (checkpointValue > RestoreCheckpointStore.NO_CHECKPOINT) {
            Log.d(TAG, "RestoreWorker: resuming from checkpoint=$checkpointValue (workName=$uniqueWorkName)")
            checkpointValue + 1
        } else {
            initialRestoredItem
        }

        try {
            if (itemsToRestoreCount > 0) {
                while (currentRestoredItem < itemsToRestoreCount) {
                    coroutineContext.ensureActive()

                    val item = msgs[currentRestoredItem]
                    if (item != null) {
                        importMessage(item.handle, item.folder, transport, converter, preferences, ctx, currentRestoredItem, uniqueWorkName)
                    }

                    msgs[currentRestoredItem] = null
                    currentRestoredItem++

                    setProgress(workDataOf(
                        PROGRESS_KEY_CURRENT_ITEM to currentRestoredItem,
                        PROGRESS_KEY_ITEMS_TO_RESTORE to itemsToRestoreCount,
                        PROGRESS_KEY_STATE to STATE_RESTORE
                    ))

                    if (currentRestoredItem % 50 == 0) {
                        clearAppCache(ctx)
                    }
                }
                updateAllThreadsIfAnySmsRestored(ctx)
            } else {
                Log.d(TAG, "RestoreWorker: nothing to restore")
            }

            val restoredCount = smsIds.size + callLogIds.size
            Log.d(TAG, "RestoreWorker: finished (restored=$restoredCount, uids=${uids.size})")

            // AC-6: Clear checkpoint on SUCCEEDED terminal state.
            checkpointStore.clear(uniqueWorkName)

            return Result.success(workDataOf(
                PROGRESS_KEY_CURRENT_ITEM to currentRestoredItem,
                PROGRESS_KEY_ITEMS_TO_RESTORE to itemsToRestoreCount,
                PROGRESS_KEY_RESTORED_COUNT to restoredCount,
                PROGRESS_KEY_STATE to STATE_FINISHED
            ))
        } catch (e: IllegalStateException) {
            Log.e(TAG, "RestoreWorker: IllegalStateException (possible memory)", e)
            return Result.failure(workDataOf(KEY_FAILURE_REASON to "illegal_state"))
        }
        // SimulatedCrashException from insertInterceptor bubbles uncaught — intentional.
    }

    /**
     * Test-accessible restore loop over pre-built [ContentValues] items.
     *
     * This is the fault-injection test path (AC-4, IC-4). It bypasses the IMAP fetch so
     * tests can seed exact N items and verify checkpoint-resume behaviour.
     *
     * Write ordering (load-bearing invariant, AC-2):
     *   1. Insert into provider
     *   2. [checkpointStore.write](uniqueWorkName, currentIndex) — durable commit
     *   3. [insertInterceptor.afterInsert](currentIndex) — fault-injection hook
     *   4. advance loop to currentIndex+1
     *
     * AC-5: reads durable checkpoint on entry; the checkpoint value overrides [startIndex].
     * AC-6: [checkpointStore.clear] called on SUCCEEDED.
     * AC-7: checkpoint NOT cleared on CANCELLED (cooperative cancellation unwinds before clear).
     *
     * @param items pre-built SMS ContentValues (N distinct items to restore)
     * @param startIndex initial loop index (0 for fresh restore; overridden by checkpoint if any)
     * @param preferences for maxSyncedDate tracking
     * @param ctx for ContentResolver access
     * @param uniqueWorkName checkpoint key
     * @return Result.success (SUCCEEDED) or Result.failure on error
     */
    internal suspend fun executeRestoreWithValues(
        items: List<ContentValues>,
        startIndex: Int,
        preferences: Preferences,
        ctx: Context,
        uniqueWorkName: String
    ): Result {
        val itemsToRestoreCount = items.size

        // U-016 AC-5: Read durable checkpoint on entry; authoritative over startIndex.
        val checkpointValue = checkpointStore.read(uniqueWorkName)
        var currentRestoredItem = if (checkpointValue > RestoreCheckpointStore.NO_CHECKPOINT) {
            Log.d(TAG, "RestoreWorker.executeRestoreWithValues: resuming from checkpoint=$checkpointValue")
            checkpointValue + 1
        } else {
            startIndex
        }

        try {
            if (itemsToRestoreCount > 0) {
                while (currentRestoredItem < itemsToRestoreCount) {
                    coroutineContext.ensureActive()

                    val values = items[currentRestoredItem]
                    insertSmsValues(values, preferences, ctx, currentRestoredItem, uniqueWorkName)

                    currentRestoredItem++

                    setProgress(workDataOf(
                        PROGRESS_KEY_CURRENT_ITEM to currentRestoredItem,
                        PROGRESS_KEY_ITEMS_TO_RESTORE to itemsToRestoreCount,
                        PROGRESS_KEY_STATE to STATE_RESTORE
                    ))

                    if (currentRestoredItem % 50 == 0) {
                        clearAppCache(ctx)
                    }
                }
                updateAllThreadsIfAnySmsRestored(ctx)
            } else {
                Log.d(TAG, "RestoreWorker.executeRestoreWithValues: nothing to restore")
            }

            val restoredCount = smsIds.size + callLogIds.size
            Log.d(TAG, "RestoreWorker.executeRestoreWithValues: finished (restored=$restoredCount)")

            // AC-6: Clear checkpoint on SUCCEEDED.
            checkpointStore.clear(uniqueWorkName)

            return Result.success(workDataOf(
                PROGRESS_KEY_CURRENT_ITEM to currentRestoredItem,
                PROGRESS_KEY_ITEMS_TO_RESTORE to itemsToRestoreCount,
                PROGRESS_KEY_RESTORED_COUNT to restoredCount,
                PROGRESS_KEY_STATE to STATE_FINISHED
            ))
        } catch (e: IllegalStateException) {
            Log.e(TAG, "RestoreWorker.executeRestoreWithValues: IllegalStateException", e)
            return Result.failure(workDataOf(KEY_FAILURE_REASON to "illegal_state"))
        }
        // SimulatedCrashException bubbles uncaught — caught by test harness.
    }

    /**
     * Inserts a single SMS [ContentValues] into the provider, gated by the smsExists() dedup
     * guard and SMS type filter.
     *
     * U-016 AC-2 write ordering: insert → write checkpoint → call interceptor.
     * The checkpoint write is synchronous (SharedPreferences.commit) before returning.
     */
    private suspend fun insertSmsValues(
        values: ContentValues,
        preferences: Preferences,
        ctx: Context,
        currentIndex: Int,
        uniqueWorkName: String
    ) {
        val type = values.getAsInteger(Telephony.TextBasedSmsColumns.TYPE)

        if (type != null &&
            (type == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX ||
             type == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT) &&
            !smsExists(values, ctx)) {

            val uri = ctx.contentResolver.insert(Consts.SMS_PROVIDER, values)
            if (uri != null) {
                smsIds.add(uri.lastPathSegment ?: uri.toString())
                val timestamp = values.getAsLong(Telephony.TextBasedSmsColumns.DATE)
                if (timestamp != null &&
                    preferences.dataTypePreferences.getMaxSyncedDate(DataType.SMS) < timestamp) {
                    preferences.dataTypePreferences.setMaxSyncedDate(DataType.SMS, timestamp)
                }
                if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "RestoreWorker: inserted $uri at $currentIndex")

                // U-016 AC-2: Write checkpoint AFTER confirmed insert, BEFORE advancing loop.
                checkpointStore.write(uniqueWorkName, currentIndex)

                // Fault-injection hook (no-op in production; throws in tests after K inserts).
                insertInterceptor.afterInsert(currentIndex)
            }
        } else {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.d(TAG, "RestoreWorker: skipping item at $currentIndex (dedup or type filter)")
            }
        }
    }

    /**
     * Imports a single message (SMS or CALLLOG) from IMAP using the MailTransport port.
     * Mirrors RestoreTask.importMessage() (RestoreTask.java:217-248).
     * Used by the IMAP [runImapRestoreLoop] path only.
     *
     * U-026: replaces the pattern of fetching a k-9 Message body directly. Instead,
     * calls [MailTransport.importMessageBody] which fetches the body and converts it
     * to a [com.zegoggles.smssync.mail.transport.MessageImportResult] inside mail.transport,
     * keeping all k-9 types confined to the ACL adapter. Engine code never imports k-9 types.
     *
     * The [folder] parameter must be the [BackupFolderHandle] that was used to fetch this
     * handle (from [MailTransport.getMessages]). The K9MailTransport adapter uses this folder
     * to call folder.fetch() to load the message body.
     *
     * After a confirmed insert, writes checkpoint then calls insertInterceptor.
     */
    private suspend fun importMessage(
        handle: MailMessageHandle,
        folder: BackupFolderHandle,
        transport: MailTransport,
        converter: MessageConverter,
        preferences: Preferences,
        ctx: Context,
        currentIndex: Int,
        uniqueWorkName: String
    ) {
        uids.add(handle.uid)

        if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "RestoreWorker: fetching message uid ${handle.uid}")

        // U-026: importMessageBody fetches the body and converts inside mail.transport package
        // so no k-9 Message type crosses the port boundary into service.*
        val importResult = transport.importMessageBody(
            folder,
            handle,
            converter
        )

        if (importResult == null || importResult.failed) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) Log.d(TAG, "RestoreWorker: skipping failed import at $currentIndex (uid=${handle.uid})")
            return
        }

        when (importResult.dataType) {
            DataType.CALLLOG -> importCallLogValues(importResult.contentValues!!, ctx, currentIndex, uniqueWorkName)
            DataType.SMS     -> insertSmsValues(importResult.contentValues!!, preferences, ctx, currentIndex, uniqueWorkName)
            else             -> if (Log.isLoggable(TAG, Log.VERBOSE))
                                    Log.d(TAG, "RestoreWorker: ignoring restore of type: ${importResult.dataType}")
        }
    }

    /**
     * Inserts a call-log [ContentValues] entry into the provider, gated by callLogExists().
     * Mirrors RestoreTask.importCallLog() (RestoreTask.java:277-286).
     * After confirmed insert: writes checkpoint then calls insertInterceptor (AC-2).
     *
     * U-026: takes ContentValues directly instead of k-9 Message — conversion was done
     * inside mail.transport by [MailTransport.importMessageBody].
     */
    private suspend fun importCallLogValues(
        values: ContentValues,
        ctx: Context,
        currentIndex: Int,
        uniqueWorkName: String
    ) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "RestoreWorker: importCallLogValues at $currentIndex")
        if (!callLogExists(values, ctx)) {
            val uri = ctx.contentResolver.insert(Consts.CALLLOG_PROVIDER, values)
            if (uri != null) {
                callLogIds.add(uri.lastPathSegment!!)

                // U-016 AC-2: Write checkpoint AFTER confirmed insert, BEFORE loop advance.
                checkpointStore.write(uniqueWorkName, currentIndex)

                // Fault-injection hook
                insertInterceptor.afterInsert(currentIndex)
            }
        } else {
            if (Log.isLoggable(TAG, Log.VERBOSE)) Log.d(TAG, "RestoreWorker: ignoring call log")
        }
    }

    /**
     * Dedup guard for call log entries.
     * Key: date + number + duration + type — exactly four fields.
     * Mirrors RestoreTask.callLogExists() verbatim (AC-11b / RestoreTask.java:288-306).
     * SQL fragment: "date = ? AND number = ? AND duration = ? AND type = ?"
     */
    private fun callLogExists(values: ContentValues, ctx: Context): Boolean {
        val c: Cursor? = ctx.contentResolver.query(
            Consts.CALLLOG_PROVIDER,
            arrayOf("_id"),
            "date = ? AND number = ? AND duration = ? AND type = ?",
            arrayOf(
                values.getAsString(CallLog.Calls.DATE),
                values.getAsString(CallLog.Calls.NUMBER),
                values.getAsString(CallLog.Calls.DURATION),
                values.getAsString(CallLog.Calls.TYPE)
            ),
            null
        )
        var exists = false
        if (c != null) {
            exists = c.count > 0
            c.close()
        }
        return exists
    }

    /**
     * Dedup guard for SMS messages.
     * Key: date + address + type — exactly three fields.
     * Mirrors RestoreTask.smsExists() verbatim (AC-11a / RestoreTask.java:308-327).
     * SQL fragment: "date = ? AND address = ? AND type = ?"
     */
    private fun smsExists(values: ContentValues, ctx: Context): Boolean {
        val c: Cursor? = ctx.contentResolver.query(
            Consts.SMS_PROVIDER,
            arrayOf("_id"),
            "date = ? AND address = ? AND type = ?",
            arrayOf(
                values.getAsString(Telephony.TextBasedSmsColumns.DATE),
                values.getAsString(Telephony.TextBasedSmsColumns.ADDRESS),
                values.getAsString(Telephony.TextBasedSmsColumns.TYPE)
            ),
            null
        )
        var exists = false
        if (c != null) {
            exists = c.count > 0
            c.close()
        }
        return exists
    }

    /**
     * Triggers thread update if any SMS was restored.
     * Mirrors RestoreTask.updateAllThreadsIfAnySmsRestored() (RestoreTask.java:329-333).
     */
    private fun updateAllThreadsIfAnySmsRestored(ctx: Context) {
        if (smsIds.isNotEmpty()) {
            Log.d(TAG, "RestoreWorker: updating threads")
            ctx.contentResolver.delete(Uri.parse("content://sms/conversations/-1"), null, null)
            Log.d(TAG, "RestoreWorker: finished updating threads")
        }
    }

    /** Clears temp cache files (mirrors SmsRestoreService.clearCache()). */
    private fun clearAppCache(ctx: Context) {
        val tmp = ctx.cacheDir ?: return
        tmp.listFiles { _, name -> name.startsWith("body") }
            ?.forEach { f -> if (!f.delete()) Log.w(TAG, "RestoreWorker: error deleting $f") }
    }

    /**
     * XOAuth2 token-refresh retry. At most one retry.
     * Mirrors RestoreTask.handleAuthError (RestoreTask.java:157-177).
     *
     * U-026: parameter type changed from k-9 XOAuth2AuthenticationFailedException to
     * app-owned [XOAuth2FailedException]. MailException replaces MessagingException.
     */
    private suspend fun handleAuthError(
        config: RestoreConfig,
        preferences: Preferences,
        converter: MessageConverter,
        tokenRefresher: TokenRefresher,
        ctx: Context,
        authPreferences: AuthPreferences,
        currentRestoredItem: Int,
        e: XOAuth2FailedException
    ): Result {
        if (e.status == 400) {
            Log.d(TAG, "RestoreWorker: XOAuth2 400 — need token refresh")
            if (config.tries < 1) {
                return try {
                    tokenRefresher.refreshOAuth2Token()
                    Log.d(TAG, "RestoreWorker: token refreshed, retrying with currentRestoredItem=$currentRestoredItem")
                    // U-024: mailTransportFactory.create() — fresh transport per retry (no singleton)
                    val newTransport = mailTransportFactory.create()
                    val retryConfig = config.retryWithStore(currentRestoredItem, newTransport)
                    executeRestore(retryConfig, preferences, converter, tokenRefresher, ctx, authPreferences)
                } catch (ignored: MailException) {
                    // U-026: MailException replaces MessagingException
                    Log.w(TAG, "RestoreWorker: MailException during token refresh", ignored)
                    Result.failure(workDataOf(KEY_FAILURE_REASON to "auth_error_after_refresh"))
                } catch (refreshEx: TokenRefreshException) {
                    Log.w(TAG, "RestoreWorker: token refresh failed: $refreshEx")
                    Result.failure(workDataOf(KEY_FAILURE_REASON to "token_refresh_failed"))
                }
            } else {
                Log.w(TAG, "RestoreWorker: no new token, giving up")
            }
        } else {
            Log.w(TAG, "RestoreWorker: unexpected XOAuth2 status ${e.status}")
        }
        return Result.failure(workDataOf(KEY_FAILURE_REASON to "auth_error"))
    }

    /**
     * U-049 AC-2: Builds the ForegroundInfo used by setForeground() at the start of doWork().
     * Replaces the foreground-service notification that SmsRestoreService.restoreStateChanged()
     * previously posted via startForeground(). Same notification channel (App.CHANNEL_ID),
     * same notification ID (RESTORE_ID = 2), same content as the legacy service notification.
     * The pending intent opens MainActivity so the user can monitor progress.
     */
    @Suppress("DEPRECATION")
    private fun createRestoreForegroundInfo(): ForegroundInfo {
        val ctx = applicationContext
        val intent = Intent(ctx, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(ctx)
            .setSmallIcon(R.drawable.ic_notification)
            .setChannelId(App.CHANNEL_ID)
            .setTicker(ctx.getString(R.string.status_restore))
            .setContentTitle(ctx.getString(R.string.status_restore))
            .setContentText(ctx.getString(R.string.status_restore))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setWhen(System.currentTimeMillis())
            .build()
        return ForegroundInfo(RESTORE_NOTIFICATION_ID, notification)
    }

    companion object {
        internal const val TAG = "SMSBackup+"

        /** Foreground-service notification ID for restore (matches SmsRestoreService.RESTORE_ID). */
        const val RESTORE_NOTIFICATION_ID = 2

        /** WorkData key: current item index in the restore loop */
        const val PROGRESS_KEY_CURRENT_ITEM = "current_item"
        /** WorkData key: total items to restore */
        const val PROGRESS_KEY_ITEMS_TO_RESTORE = "items_to_restore"
        /** WorkData key: count of actually restored items (smsIds + callLogIds) */
        const val PROGRESS_KEY_RESTORED_COUNT = "restored_count"
        /** WorkData key: current DataType name */
        const val PROGRESS_KEY_DATA_TYPE = "data_type"
        /** WorkData key: current state name */
        const val PROGRESS_KEY_STATE = "state"
        /** WorkData key: failure reason */
        const val KEY_FAILURE_REASON = "failure_reason"
        /**
         * Input data key: unique work name for checkpoint keying.
         * Set by [WorkManagerScheduler.scheduleRestore] when enqueuing the worker.
         */
        const val KEY_UNIQUE_WORK_NAME = "unique_work_name"

        /** Default unique work name when KEY_UNIQUE_WORK_NAME is not set in inputData. */
        const val RESTORE_WORK_NAME = "RESTORE"

        const val STATE_LOGIN = "LOGIN"
        const val STATE_CALC = "CALC"
        const val STATE_RESTORE = "RESTORE"
        const val STATE_FINISHED = "FINISHED_RESTORE"
        const val STATE_CANCELED = "CANCELED_RESTORE"
    }

    /**
     * Test-only [WorkerFactory] for fault-injection tests (U-016, IC-4).
     *
     * U-024: Updated to supply the new @AssistedInject constructor params.
     * [preferences] and [authPreferences] are constructed from context (matching the
     * former production behaviour). [mailTransportFactory] defaults to a no-op factory
     * since fault-injection tests drive [executeRestoreWithValues] directly and never
     * call buildMailTransport. Tests that need real transport behaviour should use the
     * Hilt test harness (@HiltAndroidTest + HiltAndroidRule).
     *
     * Used by checkpoint-resume tests via [TestListenableWorkerBuilder.setWorkerFactory] (IC-4).
     */
    class TestableRestoreWorkerFactory(
        private val checkpointStore: RestoreCheckpointStore,
        private val interceptor: RestoreInsertInterceptor
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {
            return if (workerClassName == RestoreWorker::class.java.name) {
                val prefs = Preferences(appContext)
                val authPrefs = AuthPreferences(appContext)
                RestoreWorker(
                    appContext,
                    workerParameters,
                    prefs,
                    authPrefs,
                    MailTransportFactory { throw MailException("TestableRestoreWorkerFactory: transport not available in unit tests") },
                    checkpointStore,
                    interceptor,
                    // U-050 AR-004: supply ContactAccessor and WorkerEngineFactory for the new params
                    ContactAccessor(),
                    WorkerEngineFactory(appContext, prefs, authPrefs)
                )
            } else null
        }
    }
}
