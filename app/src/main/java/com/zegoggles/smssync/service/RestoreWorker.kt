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

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.Telephony
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory
import com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException
import com.zegoggles.smssync.Consts
import com.zegoggles.smssync.auth.OAuth2Client
import com.zegoggles.smssync.auth.TokenRefreshException
import com.zegoggles.smssync.auth.TokenRefresher
import com.zegoggles.smssync.contacts.ContactAccessor
import com.zegoggles.smssync.mail.BackupImapStore
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.mail.MessageConverter
import com.zegoggles.smssync.mail.PersonLookup
import com.zegoggles.smssync.mail.PinnedCertStore
import com.zegoggles.smssync.mail.PinnedCertificateSocketFactory
import com.zegoggles.smssync.mail.TlsTrustPolicy
import com.zegoggles.smssync.preferences.AuthPreferences
import com.zegoggles.smssync.preferences.Preferences
import com.zegoggles.smssync.scheduler.WorkManagerScheduler
import kotlinx.coroutines.ensureActive
import java.io.IOException
import java.util.ArrayList
import java.util.Collections
import java.util.HashSet
import kotlin.coroutines.coroutineContext

/**
 * Real CoroutineWorker restore implementation (U-015).
 *
 * This worker contains the restore execution logic ported from [RestoreTask] (AsyncTask-based).
 * It preserves all RestoreTask behaviors:
 *   - early-exit when both restoreSms and restoreCallLog are false (AC-11d / RestoreTask.java:85-86)
 *   - restore loop starting from config.currentRestoredItem (AC restore resume / RestoreTask.java:100,119)
 *   - smsExists() dedup guard: date+address+type (AC-11a / RestoreTask.java:308-327, :259)
 *   - callLogExists() dedup guard: date+number+duration+type (AC-11b / RestoreTask.java:288-306, :280)
 *   - SMS type filter: INBOX and SENT only (AC existing behavior / RestoreTask.java:256-258)
 *   - thread update after any SMS is restored (AC-11d / RestoreTask.java:129)
 *   - XOAuth2 token-refresh retry, at most once, passing currentRestoredItem (AC-11c / RestoreTask.java:157-177)
 *   - Cooperative cancellation via ensureActive() (AC-5, replaces isCancelled() polling)
 *
 * Progress emitted via setProgress(workDataOf(...)) using companion keys (AC-4b).
 * This replaces Otto App.post(RestoreState) / @Subscribe path (AC-6).
 *
 * INV-3 backoff cap: same 300s cap as BackupWorker, enforced by checking runAttemptCount.
 *
 * TODO U-024: Add @HiltWorker/@AssistedInject annotations.
 *
 * Branch-by-abstraction note (U-015): RestoreTask.java and SmsRestoreService are NOT deleted
 * in this story. The AsyncTask path remains the LegacyScheduler production path until U-017.
 */
class RestoreWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // Dedup tracking sets — mirrors RestoreTask.smsIds, callLogIds, uids
    private val smsIds: MutableSet<String> = HashSet()
    private val callLogIds: MutableSet<String> = HashSet()
    private val uids: MutableSet<String> = HashSet()

    override suspend fun doWork(): Result {
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
        val preferences = Preferences(ctx)
        val authPreferences = AuthPreferences(ctx)

        Log.d(TAG, "RestoreWorker.doWork: starting restore, attempt=$runAttempt")

        return try {
            val imapStore = buildImapStore(ctx, authPreferences)
            val restoreSms = preferences.dataTypePreferences.isRestoreEnabled(DataType.SMS)
            val restoreCallLog = preferences.dataTypePreferences.isRestoreEnabled(DataType.CALLLOG)

            // Early-exit path: mirrors RestoreTask.doInBackground:85-86
            if (!restoreSms && !restoreCallLog) {
                Log.d(TAG, "RestoreWorker: nothing to restore (restoreSms=$restoreSms, restoreCallLog=$restoreCallLog)")
                return Result.success(workDataOf(PROGRESS_KEY_STATE to STATE_FINISHED))
            }

            val config = RestoreConfig(
                imapStore,
                0,
                restoreSms,
                restoreCallLog,
                preferences.isRestoreStarredOnly,
                preferences.maxItemsPerRestore,
                0
            )

            val converter = MessageConverter(
                ctx,
                preferences,
                authPreferences.userEmail,
                PersonLookup(ctx.contentResolver),
                ContactAccessor()
            )

            val tokenRefresher = TokenRefresher(
                ctx,
                OAuth2Client(authPreferences.oAuth2ClientId),
                authPreferences
            )

            executeRestore(config, preferences, converter, tokenRefresher, ctx, authPreferences)
        } catch (e: MessagingException) {
            Log.w(TAG, "RestoreWorker: MessagingException — retrying", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "RestoreWorker: unrecoverable error", e)
            Result.failure(workDataOf(KEY_FAILURE_REASON to (e.message ?: "unknown")))
        }
    }

    /**
     * Core restore execution. Mirrors RestoreTask.restore() (RestoreTask.java:97-155).
     */
    private suspend fun executeRestore(
        config: RestoreConfig,
        preferences: Preferences,
        converter: MessageConverter,
        tokenRefresher: TokenRefresher,
        ctx: Context,
        authPreferences: AuthPreferences
    ): Result {
        val imapStore = config.imapStore
        var currentRestoredItem = config.currentRestoredItem

        try {
            // Emit LOGIN state (mirrors publishProgress(LOGIN) at RestoreTask.java:102)
            setProgress(workDataOf(PROGRESS_KEY_STATE to STATE_LOGIN))
            imapStore.checkSettings()

            // Emit CALC state (mirrors publishProgress(CALC) at RestoreTask.java:105)
            setProgress(workDataOf(PROGRESS_KEY_STATE to STATE_CALC))

            val msgs = ArrayList<Message?>()
            if (config.restoreSms) {
                msgs.addAll(imapStore.getFolder(DataType.SMS, preferences.dataTypePreferences)
                    .getMessages(config.maxRestore, config.restoreOnlyStarred, null))
            }
            if (config.restoreCallLog) {
                msgs.addAll(imapStore.getFolder(DataType.CALLLOG, preferences.dataTypePreferences)
                    .getMessages(config.maxRestore, config.restoreOnlyStarred, null))
            }

            val itemsToRestoreCount = if (config.maxRestore <= 0) msgs.size
                                      else minOf(msgs.size, config.maxRestore)

            if (itemsToRestoreCount > 0) {
                // Restore loop mirrors RestoreTask.java:119
                // Starts from currentRestoredItem (resume offset), checks ensureActive() for
                // cooperative cancellation (AC-5, replaces !isCancelled() polling)
                while (currentRestoredItem < itemsToRestoreCount) {
                    coroutineContext.ensureActive()

                    val msg = msgs[currentRestoredItem]
                    val dataType = if (msg != null) importMessage(msg, converter, preferences, ctx) else null

                    msgs[currentRestoredItem] = null // help GC (mirrors RestoreTask.java:122)
                    currentRestoredItem++

                    // Emit progress (AC-4b, replaces publishProgress(RestoreState) + App.post at
                    // RestoreTask.java:123 and RestoreTask.onProgressUpdate)
                    setProgress(workDataOf(
                        PROGRESS_KEY_CURRENT_ITEM to currentRestoredItem,
                        PROGRESS_KEY_ITEMS_TO_RESTORE to itemsToRestoreCount,
                        PROGRESS_KEY_DATA_TYPE to (dataType?.name ?: ""),
                        PROGRESS_KEY_STATE to STATE_RESTORE
                    ))

                    if (currentRestoredItem % 50 == 0) {
                        // Periodic cache clear (mirrors RestoreTask.java:124-127)
                        clearAppCache(ctx)
                    }
                }
                // Thread update after SMS restore (AC-11d / RestoreTask.java:129)
                updateAllThreadsIfAnySmsRestored(ctx)
            } else {
                Log.d(TAG, "RestoreWorker: nothing to restore")
            }

            val restoredCount = smsIds.size + callLogIds.size
            Log.d(TAG, "RestoreWorker: finished (restored=$restoredCount, uids=${uids.size})")
            return Result.success(workDataOf(
                PROGRESS_KEY_CURRENT_ITEM to currentRestoredItem,
                PROGRESS_KEY_ITEMS_TO_RESTORE to itemsToRestoreCount,
                PROGRESS_KEY_RESTORED_COUNT to restoredCount,
                PROGRESS_KEY_STATE to STATE_FINISHED
            ))
        } catch (e: XOAuth2AuthenticationFailedException) {
            return handleAuthError(config, preferences, converter, tokenRefresher, ctx, authPreferences, currentRestoredItem, e)
        } catch (e: AuthenticationFailedException) {
            return Result.failure(workDataOf(KEY_FAILURE_REASON to "auth_failed"))
        } catch (e: MessagingException) {
            Log.e(TAG, "RestoreWorker: MessagingException", e)
            updateAllThreadsIfAnySmsRestored(ctx)
            return Result.retry()
        } catch (e: IllegalStateException) {
            // memory problems (Couldn't init cursor window) — mirrors RestoreTask.java:149-151
            Log.e(TAG, "RestoreWorker: IllegalStateException (possible memory)", e)
            return Result.failure(workDataOf(KEY_FAILURE_REASON to "illegal_state"))
        } finally {
            imapStore.closeFolders()
        }
    }

    /**
     * XOAuth2 token-refresh retry. At most one retry, passing currentRestoredItem as resume.
     * Mirrors RestoreTask.handleAuthError (AC-11c / RestoreTask.java:157-177).
     * The resume offset (currentRestoredItem) is passed to retryWithStore so restore
     * continues from where it left off (RestoreTask.java:165).
     */
    private suspend fun handleAuthError(
        config: RestoreConfig,
        preferences: Preferences,
        converter: MessageConverter,
        tokenRefresher: TokenRefresher,
        ctx: Context,
        authPreferences: AuthPreferences,
        currentRestoredItem: Int,
        e: XOAuth2AuthenticationFailedException
    ): Result {
        if (e.status == 400) {
            Log.d(TAG, "RestoreWorker: XOAuth2 400 — need token refresh")
            if (config.tries < 1) {
                return try {
                    tokenRefresher.refreshOAuth2Token()
                    Log.d(TAG, "RestoreWorker: token refreshed, retrying with currentRestoredItem=$currentRestoredItem")
                    val newStore = buildImapStore(ctx, authPreferences)
                    // Pass currentRestoredItem as resume offset (mirrors RestoreTask.java:165)
                    val retryConfig = config.retryWithStore(currentRestoredItem, newStore)
                    executeRestore(retryConfig, preferences, converter, tokenRefresher, ctx, authPreferences)
                } catch (ignored: MessagingException) {
                    Log.w(TAG, "RestoreWorker: MessagingException during token refresh", ignored)
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
     * Imports a single message (SMS or CALLLOG).
     * Mirrors RestoreTask.importMessage() (RestoreTask.java:217-248).
     */
    @Suppress("UNCHECKED_CAST")
    private fun importMessage(
        message: Message,
        converter: MessageConverter,
        preferences: Preferences,
        ctx: Context
    ): DataType? {
        uids.add(message.uid)

        val fp = FetchProfile()
        fp.add(FetchProfile.Item.BODY)
        var dataType: DataType? = null
        try {
            if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "RestoreWorker: fetching message uid ${message.uid}")
            message.folder.fetch(Collections.singletonList(message), fp, null)
            dataType = converter.getDataType(message)
            when (dataType) {
                DataType.CALLLOG -> importCallLog(message, converter, ctx)
                DataType.SMS     -> importSms(message, converter, preferences, ctx)
                else             -> if (Log.isLoggable(TAG, Log.VERBOSE))
                                        Log.d(TAG, "RestoreWorker: ignoring restore of type: $dataType")
            }
        } catch (e: MessagingException) {
            Log.e(TAG, "RestoreWorker: error", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "RestoreWorker: error", e)
        } catch (e: IOException) {
            Log.e(TAG, "RestoreWorker: error", e)
        }
        return dataType
    }

    /**
     * Imports a single SMS message, gated by type filter and smsExists() dedup guard.
     * Mirrors RestoreTask.importSms() (RestoreTask.java:250-275).
     *
     * Type filter: only INBOX and SENT — avoids re-sending (RestoreTask.java:256-258).
     * Dedup key: date + address + type (AC-11a / RestoreTask.java:308-327, :259).
     */
    @Throws(IOException::class, MessagingException::class)
    private fun importSms(
        message: Message,
        converter: MessageConverter,
        preferences: Preferences,
        ctx: Context
    ) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "RestoreWorker: importSms($message)")
        val values = converter.messageToContentValues(message)
        val type = values.getAsInteger(Telephony.TextBasedSmsColumns.TYPE)

        // Only restore INBOX and SENT (mirrors RestoreTask.java:256-258)
        if (type != null &&
            (type == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX ||
             type == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT) &&
            !smsExists(values, ctx)) {

            val uri = ctx.contentResolver.insert(Consts.SMS_PROVIDER, values)
            if (uri != null) {
                smsIds.add(uri.lastPathSegment!!)
                val timestamp = values.getAsLong(Telephony.TextBasedSmsColumns.DATE)
                if (timestamp != null && preferences.dataTypePreferences.getMaxSyncedDate(DataType.SMS) < timestamp) {
                    preferences.dataTypePreferences.setMaxSyncedDate(DataType.SMS, timestamp)
                }
                if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "RestoreWorker: inserted $uri")
            }
        } else {
            if (Log.isLoggable(TAG, Log.VERBOSE)) Log.d(TAG, "RestoreWorker: ignoring sms")
        }
    }

    /**
     * Imports a single call log entry, gated by callLogExists() dedup guard.
     * Mirrors RestoreTask.importCallLog() (RestoreTask.java:277-286).
     *
     * Dedup key: date + number + duration + type (AC-11b / RestoreTask.java:288-306, :280).
     */
    @Throws(MessagingException::class, IOException::class)
    private fun importCallLog(message: Message, converter: MessageConverter, ctx: Context) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "RestoreWorker: importCallLog($message)")
        val values = converter.messageToContentValues(message)
        if (!callLogExists(values, ctx)) {
            val uri = ctx.contentResolver.insert(Consts.CALLLOG_PROVIDER, values)
            if (uri != null) callLogIds.add(uri.lastPathSegment!!)
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
     * The trick of deleting conversation -1 forces Android to refresh all thread metadata.
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
     * Builds a BackupImapStore with TLS trust policy resolution.
     * Mirrors ServiceBase.getBackupImapStore().
     */
    private fun buildImapStore(ctx: Context, authPreferences: AuthPreferences): BackupImapStore {
        val uri = authPreferences.storeUri
        if (!BackupImapStore.isValidUri(uri)) {
            throw MessagingException("No valid IMAP URI: $uri")
        }
        val parsed = Uri.parse(uri)
        val host = parsed.host
        val port = parsed.port
        val pinnedCertStore = PinnedCertStore(ctx)
        val policy = pinnedCertStore.getTlsTrustPolicy(host, port)
        val factory = if (policy == TlsTrustPolicy.PINNED_CERTIFICATE) {
            PinnedCertificateSocketFactory(ctx, host, pinnedCertStore.get(host, port))
        } else {
            DefaultTrustedSocketFactory(ctx)
        }
        return BackupImapStore(ctx, uri, factory)
    }

    companion object {
        internal const val TAG = "SMSBackup+"

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

        const val STATE_LOGIN = "LOGIN"
        const val STATE_CALC = "CALC"
        const val STATE_RESTORE = "RESTORE"
        const val STATE_FINISHED = "FINISHED_RESTORE"
        const val STATE_CANCELED = "CANCELED_RESTORE"
    }
}
