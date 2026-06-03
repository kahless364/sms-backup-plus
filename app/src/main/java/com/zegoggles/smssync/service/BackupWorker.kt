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

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory
import com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException
import com.zegoggles.smssync.auth.OAuth2Client
import com.zegoggles.smssync.auth.TokenRefreshException
import com.zegoggles.smssync.auth.TokenRefresher
import com.zegoggles.smssync.calendar.CalendarAccessor
import com.zegoggles.smssync.contacts.ContactAccessor
import com.zegoggles.smssync.mail.BackupImapStore
import com.zegoggles.smssync.mail.CallFormatter
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
import java.util.EnumSet
import java.util.Locale
import kotlin.coroutines.coroutineContext

/**
 * Real CoroutineWorker backup implementation — replaces the BackupWorkerStub from U-014.
 *
 * This worker contains the backup execution logic ported from [BackupTask] (AsyncTask-based).
 * It preserves all BackupTask behaviors:
 *   - SKIP type early-return path (AC-10a / BackupTask.java:207-218)
 *   - XOAuth2 token-refresh retry, at most once (AC-10b / BackupTask.java:183-205)
 *   - CalendarSyncer.syncCalendar per CALLLOG batch (AC-10c / BackupTask.java:282-284)
 *   - setMaxSyncedDate per type after append (AC-10d / BackupTask.java:285)
 *   - Cooperative cancellation via ensureActive() (AC-5, replaces isCancelled() polling)
 *
 * Progress is emitted via setProgress(workDataOf(...)) using keys defined in companion,
 * replacing the Otto App.post(BackupState) path (AC-6).
 *
 * INV-3 backoff cap: WorkManager's MAX_BACKOFF_MILLIS is ~5 hours. Per CNTR-MODERNIZATION-004
 * §INV-3, the 300s cap is enforced at the worker: once effective exponential backoff would
 * exceed [WorkManagerScheduler.BACKOFF_MAX_SECS], worker returns Result.failure().
 * Effective delay at attempt N = BACKOFF_INITIAL_SECS * 2^N. Cap exceeded when N >= 4
 * (30 * 16 = 480 > 300).
 *
 * TODO U-024: Add @HiltWorker/@AssistedInject annotations and replace direct constructor
 * calls with injected collaborators via HiltWorkerFactory.
 *
 * U-017: This is now the sole production execution path. The legacy Firebase JobDispatcher
 * path (LegacyScheduler, BackupJobs, the firebase job service) has been deleted (Gate G3).
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // INV-3 backoff cap: cap effective retry delay at 300s.
        // At attempt N, effective delay = 30 * 2^N seconds.
        // N=0: 30s, N=1: 60s, N=2: 120s, N=3: 240s, N=4: 480s → cap exceeded.
        val runAttempt = runAttemptCount
        if (runAttempt > 0) {
            val effectiveDelaySecs = WorkManagerScheduler.BACKOFF_INITIAL_SECS shl runAttempt
            if (effectiveDelaySecs > WorkManagerScheduler.BACKOFF_MAX_SECS) {
                Log.w(TAG, "BackupWorker: backoff cap exceeded at attempt $runAttempt " +
                        "(effective=${effectiveDelaySecs}s > max=${WorkManagerScheduler.BACKOFF_MAX_SECS}s); " +
                        "returning failure to prevent further retries beyond 300s cap")
                return Result.failure(workDataOf(KEY_FAILURE_REASON to "backoff_cap_exceeded"))
            }
        }

        val ctx = applicationContext
        val preferences = Preferences(ctx)
        val authPreferences = AuthPreferences(ctx)
        val backupType = inferBackupType()

        Log.d(TAG, "BackupWorker.doWork: starting backup, type=$backupType, attempt=$runAttempt")

        return try {
            val imapStore = buildImapStore(ctx, authPreferences)
            val typesToBackup = getEnabledBackupTypes(preferences)

            val config = BackupConfig(
                imapStore,
                0,
                preferences.maxItemsPerSync,
                preferences.backupContactGroup,
                backupType,
                typesToBackup,
                preferences.isAppLogDebug
            )

            executeBackup(config, preferences, authPreferences, ctx)
        } catch (e: MessagingException) {
            Log.w(TAG, "BackupWorker: MessagingException — retrying", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "BackupWorker: unrecoverable error", e)
            Result.failure(workDataOf(KEY_FAILURE_REASON to (e.message ?: "unknown")))
        }
    }

    /**
     * Executes the backup. Mirrors BackupTask.doInBackground.
     *
     * Note: wakelock/wifi-lock (ServiceBase.acquireLocks/releaseLocks) is not reproduced
     * here because WorkManager manages wakelock internally for CoroutineWorker. The
     * service-layer wakelock will be removed in U-017 (cutover story).
     */
    private suspend fun executeBackup(
        config: BackupConfig,
        preferences: Preferences,
        authPreferences: AuthPreferences,
        ctx: Context
    ): Result {
        return if (config.backupType == BackupType.SKIP) {
            executeSkip(config, preferences)
        } else {
            fetchAndBackupItems(config, preferences, authPreferences, ctx)
        }
    }

    /**
     * SKIP path: marks max synced date for each type without connecting to IMAP.
     * Preserves BackupTask.skip() (AC-10a / BackupTask.java:207-218).
     */
    private fun executeSkip(config: BackupConfig, preferences: Preferences): Result {
        Log.i(TAG, "BackupWorker: SKIP backup — marking max synced dates without IMAP")
        val resolver = applicationContext.contentResolver
        val fetcher = BackupItemsFetcher(
            resolver,
            BackupQueryBuilder(preferences.dataTypePreferences)
        )
        for (type in config.typesToBackup) {
            try {
                preferences.dataTypePreferences.setMaxSyncedDate(
                    type,
                    fetcher.getMostRecentTimestamp(type)
                )
            } catch (e: SecurityException) {
                Log.w(TAG, "BackupWorker: SecurityException during SKIP for $type", e)
                return Result.failure(workDataOf(KEY_FAILURE_REASON to "missing_permission"))
            }
        }
        Log.i(TAG, "BackupWorker: SKIP complete")
        return Result.success(workDataOf(PROGRESS_KEY_BACKED_UP to 0, PROGRESS_KEY_ITEMS_TO_SYNC to 0))
    }

    /**
     * Main fetch-and-backup path. Mirrors BackupTask.fetchAndBackupItems.
     * Handles XOAuth2 token-refresh retry (at most once, AC-10b).
     */
    private suspend fun fetchAndBackupItems(
        config: BackupConfig,
        preferences: Preferences,
        authPreferences: AuthPreferences,
        ctx: Context
    ): Result {
        val resolver = ctx.contentResolver
        val fetcher = BackupItemsFetcher(
            resolver,
            BackupQueryBuilder(preferences.dataTypePreferences)
        )
        val personLookup = PersonLookup(resolver)
        val contactAccessor = ContactAccessor()
        val converter = MessageConverter(
            ctx,
            preferences,
            authPreferences.userEmail,
            personLookup,
            contactAccessor
        )
        val calendarSyncer: CalendarSyncer? = if (preferences.isCallLogCalendarSyncEnabled) {
            CalendarSyncer(
                CalendarAccessor.Get.instance(resolver),
                preferences.callLogCalendarId.toLong(),
                personLookup,
                CallFormatter(ctx.resources)
            )
        } else null

        val tokenRefresher = TokenRefresher(
            ctx,
            OAuth2Client(authPreferences.oAuth2ClientId),
            authPreferences
        )

        var cursors: BackupCursors? = null
        return try {
            val groupIds = contactAccessor.getGroupContactIds(resolver, config.groupToBackup)
            cursors = BulkFetcher(fetcher).fetch(config.typesToBackup, groupIds, config.maxItemsPerSync)
            val itemsToSync = cursors.count()

            if (itemsToSync > 0) {
                backupCursors(cursors, config, preferences, converter, calendarSyncer, itemsToSync)
            } else {
                // First-backup sentinel: write MAX_SYNCED_DATE so future runs know a backup was done
                if (preferences.isFirstBackup) {
                    preferences.dataTypePreferences.setMaxSyncedDate(DataType.SMS, DataType.Defaults.MAX_SYNCED_DATE)
                    preferences.dataTypePreferences.setMaxSyncedDate(DataType.MMS, DataType.Defaults.MAX_SYNCED_DATE)
                }
                Log.i(TAG, "BackupWorker: nothing to backup")
                Result.success(workDataOf(PROGRESS_KEY_BACKED_UP to 0, PROGRESS_KEY_ITEMS_TO_SYNC to 0))
            }
        } catch (e: XOAuth2AuthenticationFailedException) {
            handleAuthError(config, preferences, authPreferences, ctx, e, fetcher, converter, calendarSyncer, tokenRefresher)
        } catch (e: AuthenticationFailedException) {
            Log.w(TAG, "BackupWorker: auth failed", e)
            Result.failure(workDataOf(KEY_FAILURE_REASON to "auth_failed"))
        } catch (e: MessagingException) {
            Log.w(TAG, "BackupWorker: MessagingException — retrying", e)
            Result.retry()
        } catch (e: SecurityException) {
            Log.w(TAG, "BackupWorker: SecurityException (missing permission)", e)
            Result.failure(workDataOf(KEY_FAILURE_REASON to "missing_permission"))
        } finally {
            cursors?.close()
        }
    }

    /**
     * Core backup loop. Mirrors BackupTask.backupCursors() (BackupTask.java:258-302).
     * Uses ensureActive() for cooperative cancellation (AC-5, replaces isCancelled() polling).
     * Emits setProgress at each iteration (AC-3b, replaces publishProgress + App.post).
     */
    private suspend fun backupCursors(
        cursors: BackupCursors,
        config: BackupConfig,
        preferences: Preferences,
        converter: MessageConverter,
        calendarSyncer: CalendarSyncer?,
        itemsToSync: Int
    ): Result {
        Log.i(TAG, String.format(Locale.ENGLISH, "BackupWorker: starting backup (%d messages)", itemsToSync))

        // Emit LOGIN state (mirrors publish(LOGIN) at BackupTask.java:261)
        setProgress(workDataOf(PROGRESS_KEY_STATE to STATE_LOGIN))
        config.imapStore.checkSettings()

        try {
            // Emit CALC state (mirrors publish(CALC) at BackupTask.java:265)
            setProgress(workDataOf(PROGRESS_KEY_STATE to STATE_CALC))
            var backedUpItems = 0
            var currentItemsToSync = itemsToSync

            // Loop mirrors: while (!isCancelled() && cursors.hasNext()) at BackupTask.java:267
            // Cooperative cancellation: ensureActive() replaces isCancelled() polling (AC-5)
            while (cursors.hasNext()) {
                coroutineContext.ensureActive()

                val cursor = cursors.next()
                Log.v(TAG, "BackupWorker: backing up: $cursor")

                val result = converter.convertMessages(cursor.cursor, cursor.type)
                if (!result.isEmpty) {
                    val messages = result.messages

                    Log.v(TAG, String.format(Locale.ENGLISH,
                        "BackupWorker: sending %d %s message(s) to server.",
                        messages.size, cursor.type))

                    // Append to IMAP folder (mirrors BackupTask.java:280)
                    config.imapStore.getFolder(cursor.type, preferences.dataTypePreferences)
                        .appendMessages(messages)

                    // Calendar sync per CALLLOG batch (AC-10c / BackupTask.java:282-284)
                    if (cursor.type == DataType.CALLLOG && calendarSyncer != null) {
                        calendarSyncer.syncCalendar(result)
                    }

                    // setMaxSyncedDate per type (AC-10d / BackupTask.java:285)
                    preferences.dataTypePreferences.setMaxSyncedDate(cursor.type, result.maxDate)
                    backedUpItems += messages.size
                } else {
                    Log.w(TAG, "BackupWorker: no messages converted")
                    currentItemsToSync -= 1
                }

                // Emit progress (AC-3b, replaces publishProgress(BackupState) + App.post at
                // BackupTask.java:292 and BackupTask.onProgressUpdate)
                setProgress(workDataOf(
                    PROGRESS_KEY_BACKED_UP to backedUpItems,
                    PROGRESS_KEY_ITEMS_TO_SYNC to currentItemsToSync,
                    PROGRESS_KEY_DATA_TYPE to (cursor.type?.name ?: ""),
                    PROGRESS_KEY_STATE to STATE_BACKUP
                ))
            }

            Log.i(TAG, "BackupWorker: backup complete, backedUp=$backedUpItems")
            return Result.success(workDataOf(
                PROGRESS_KEY_BACKED_UP to backedUpItems,
                PROGRESS_KEY_ITEMS_TO_SYNC to currentItemsToSync,
                PROGRESS_KEY_STATE to STATE_FINISHED
            ))
        } finally {
            config.imapStore.closeFolders()
        }
    }

    /**
     * XOAuth2 token-refresh retry. At most one retry (config.currentTry < 1).
     * Mirrors BackupTask.handleAuthError (AC-10b / BackupTask.java:183-205).
     */
    private suspend fun handleAuthError(
        config: BackupConfig,
        preferences: Preferences,
        authPreferences: AuthPreferences,
        ctx: Context,
        e: XOAuth2AuthenticationFailedException,
        fetcher: BackupItemsFetcher,
        converter: MessageConverter,
        calendarSyncer: CalendarSyncer?,
        tokenRefresher: TokenRefresher
    ): Result {
        if (e.status == 400) {
            Log.d(TAG, "BackupWorker: XOAuth2 400 — need token refresh")
            if (config.currentTry < 1) {
                return try {
                    tokenRefresher.refreshOAuth2Token()
                    Log.d(TAG, "BackupWorker: token refreshed, retrying with new store")
                    // new store required because auth params are immutable (BackupTask.java:192)
                    val newStore = buildImapStore(ctx, authPreferences)
                    val retryConfig = config.retryWithStore(newStore)
                    val retryGroupIds = ContactAccessor().getGroupContactIds(
                        ctx.contentResolver, retryConfig.groupToBackup
                    )
                    val retryCursors = BulkFetcher(fetcher).fetch(
                        retryConfig.typesToBackup, retryGroupIds, retryConfig.maxItemsPerSync
                    )
                    val retryItemsToSync = retryCursors.count()
                    if (retryItemsToSync > 0) {
                        backupCursors(retryCursors, retryConfig, preferences, converter, calendarSyncer, retryItemsToSync)
                    } else {
                        Result.success(workDataOf(PROGRESS_KEY_BACKED_UP to 0, PROGRESS_KEY_ITEMS_TO_SYNC to 0))
                    }
                } catch (ignored: MessagingException) {
                    Log.w(TAG, "BackupWorker: MessagingException during token refresh", ignored)
                    Result.failure(workDataOf(KEY_FAILURE_REASON to "auth_error_after_refresh"))
                } catch (refreshEx: TokenRefreshException) {
                    Log.w(TAG, "BackupWorker: token refresh failed: $refreshEx")
                    Result.failure(workDataOf(KEY_FAILURE_REASON to "token_refresh_failed"))
                }
            } else {
                Log.w(TAG, "BackupWorker: no new token obtained, giving up (currentTry=${config.currentTry})")
            }
        } else {
            Log.w(TAG, "BackupWorker: unexpected XOAuth2 status ${e.status}")
        }
        return Result.failure(workDataOf(KEY_FAILURE_REASON to "auth_error"))
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

    /**
     * Returns the set of enabled DataTypes from preferences.
     */
    private fun getEnabledBackupTypes(preferences: Preferences): EnumSet<DataType> {
        val enabled = preferences.dataTypePreferences.enabled()
        if (enabled.isEmpty()) {
            throw MessagingException("No backup types enabled")
        }
        return enabled
    }

    /**
     * Infers BackupType from worker tags (set by WorkManagerScheduler).
     * Falls back to BROADCAST_INTENT if no recognized tag is found.
     */
    private fun inferBackupType(): BackupType {
        for (tag in tags) {
            val type = BackupType.fromName(tag)
            if (type != BackupType.UNKNOWN) return type
        }
        return BackupType.BROADCAST_INTENT
    }

    companion object {
        internal const val TAG = "SMSBackup+"

        /** WorkData key: number of messages backed up so far */
        const val PROGRESS_KEY_BACKED_UP = "backed_up"
        /** WorkData key: total messages to sync */
        const val PROGRESS_KEY_ITEMS_TO_SYNC = "items_to_sync"
        /** WorkData key: current DataType name */
        const val PROGRESS_KEY_DATA_TYPE = "data_type"
        /** WorkData key: current state name */
        const val PROGRESS_KEY_STATE = "state"
        /** WorkData key: failure reason in Result.failure output */
        const val KEY_FAILURE_REASON = "failure_reason"

        const val STATE_LOGIN = "LOGIN"
        const val STATE_CALC = "CALC"
        const val STATE_BACKUP = "BACKUP"
        const val STATE_FINISHED = "FINISHED_BACKUP"
        const val STATE_CANCELED = "CANCELED_BACKUP"
    }
}
