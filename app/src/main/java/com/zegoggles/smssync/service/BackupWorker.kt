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
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.zegoggles.smssync.App
import com.zegoggles.smssync.R
import com.zegoggles.smssync.activity.MainActivity
import com.zegoggles.smssync.auth.TokenRefreshException
import com.zegoggles.smssync.auth.TokenRefresher
import com.zegoggles.smssync.contacts.ContactAccessor
import com.zegoggles.smssync.di.MailTransportFactory
import com.zegoggles.smssync.mail.ConversionResult
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.mail.MessageConverter
import com.zegoggles.smssync.mail.PersonLookup
import com.zegoggles.smssync.mail.transport.BackupFolderHandle
import com.zegoggles.smssync.mail.transport.MailException
import com.zegoggles.smssync.mail.transport.MailTransport
import com.zegoggles.smssync.mail.transport.XOAuth2FailedException
import com.zegoggles.smssync.preferences.AuthPreferences
import com.zegoggles.smssync.preferences.DataTypePreferences
import com.zegoggles.smssync.preferences.Preferences
import com.zegoggles.smssync.scheduler.WorkManagerScheduler
import com.zegoggles.smssync.service.exception.RequiresLoginException
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ensureActive
import java.util.EnumSet
import java.util.Locale
import kotlin.coroutines.coroutineContext

/**
 * Real CoroutineWorker backup implementation — replaces the BackupWorkerStub from U-014.
 *
 * U-026: Rewired from BackupImapStore (k-9 type) to
 * [MailTransport] (app-owned ACL port). No k-9 transport or MIME import remains in this
 * file. [buildMailTransport] replaces the former [buildImapStore].
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
 * U-024: Annotated @HiltWorker with @AssistedInject constructor. @Assisted Context and
 * @Assisted WorkerParameters are supplied by WorkManager via HiltWorkerFactory; the
 * remaining constructor params (preferences, authPreferences, mailTransportFactory,
 * contactAccessor, engineFactory) are supplied by the Hilt SingletonComponent graph.
 *
 * The direct construction of Preferences/AuthPreferences/MailTransport inside doWork() and
 * fetchAndBackupItems() is replaced by injected fields. The buildMailTransport() helper
 * is replaced by mailTransportFactory.create() to preserve the per-run construction semantics
 * (no cached singleton transport — auth-retry path requires a fresh store each time,
 * per DES-MODERNIZATION-008 §Behavior-preservation guarantees, point 2).
 *
 * U-050 AR-004: PersonLookup, MessageConverter, TokenRefresher, and CalendarSyncer are no
 * longer hand-`new`-ed inside fetchAndBackupItems(). They are created per run via the
 * injected [WorkerEngineFactory] (captures Context, Preferences, AuthPreferences as
 * Hilt @Singleton deps). ContactAccessor has a zero-arg @Inject constructor and is
 * injected directly. CalendarSyncer must remain factory-built because it requires a
 * runtime calendarId (Long) and a legacy static CalendarAccessor.Get.instance() —
 * both documented in [WorkerEngineFactory.createCalendarSyncerIfEnabled].
 *
 * U-017: This is now the sole production execution path. The legacy Firebase JobDispatcher
 * path (LegacyScheduler, BackupJobs, the firebase job service) has been deleted (Gate G3).
 */
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferences: Preferences,
    private val authPreferences: AuthPreferences,
    private val mailTransportFactory: MailTransportFactory,
    /** U-050 AR-004: Injected; zero-arg @Inject constructor, no per-run state. */
    private val contactAccessor: ContactAccessor,
    /** U-050 AR-004: Injected factory; creates per-run PersonLookup/MessageConverter/TokenRefresher/CalendarSyncer. */
    private val engineFactory: WorkerEngineFactory
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // U-049 AC-2: Set foreground notification early in doWork() so the backup shows
        // a foreground-service notification on Android 12+ without ANR risk.
        // Uses the same notification channel (App.CHANNEL_ID) and content that
        // SmsBackupService.notifyAboutBackup() previously provided.
        setForeground(createBackupForegroundInfo())

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
        // U-024: preferences and authPreferences are now @AssistedInject-injected fields;
        // no more manual construction here.
        val backupType = inferBackupType()

        Log.d(TAG, "BackupWorker.doWork: starting backup, type=$backupType, attempt=$runAttempt")

        return try {
            // U-024: mailTransportFactory.create() replaces buildMailTransport() — factory is injected
            // via Hilt and constructs a fresh K9MailTransport per run (preserves per-run semantics
            // for the auth-retry path per DES-MODERNIZATION-008 §Behavior-preservation, point 2).
            val transport = mailTransportFactory.create()
            val typesToBackup = getEnabledBackupTypes(preferences)

            val config = BackupConfig(
                transport,
                0,
                preferences.maxItemsPerSync,
                preferences.backupContactGroup,
                backupType,
                typesToBackup,
                preferences.isAppLogDebug
            )

            executeBackup(config, preferences, authPreferences, ctx)
        } catch (e: MailException) {
            // U-026: MailException replaces MessagingException
            Log.w(TAG, "BackupWorker: MailException — retrying", e)
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
     *
     * U-026: catches [XOAuth2FailedException] and [RequiresLoginException] instead of k-9 types.
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
        // U-050 AR-004: collaborators created via injected WorkerEngineFactory instead of hand-new.
        // PersonLookup is per-run (fresh LRU cache each run). MessageConverter reads userEmail from
        // AuthPreferences at creation time — both created via engineFactory, not via 'new'.
        // CalendarSyncer is still created via factory method because it requires a runtime
        // calendarId (Long) and a legacy static CalendarAccessor.Get.instance() — see
        // WorkerEngineFactory.createCalendarSyncerIfEnabled() for the rationale.
        val personLookup = engineFactory.createPersonLookup()
        val converter = engineFactory.createMessageConverter(personLookup, contactAccessor)
        val calendarSyncer: CalendarSyncer? = engineFactory.createCalendarSyncerIfEnabled(personLookup)
        val tokenRefresher = engineFactory.createTokenRefresher()

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
        } catch (e: XOAuth2FailedException) {
            // U-026: XOAuth2FailedException replaces k-9 XOAuth2AuthenticationFailedException
            handleAuthError(config, preferences, ctx, e, fetcher, converter, calendarSyncer, tokenRefresher)
        } catch (e: RequiresLoginException) {
            // U-026: RequiresLoginException replaces k-9 AuthenticationFailedException
            Log.w(TAG, "BackupWorker: auth failed (RequiresLoginException)", e)
            Result.failure(workDataOf(KEY_FAILURE_REASON to "auth_failed"))
        } catch (e: MailException) {
            // U-026: MailException replaces k-9 MessagingException
            Log.w(TAG, "BackupWorker: MailException — retrying", e)
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
     *
     * U-026: uses [MailTransport.openFolder] + [MailTransport.appendMessages] instead of
     * [BackupImapStore.getFolder] + folder.appendMessages.
     *
     * BUG-010 fix (U-042): the watermark ([DataTypePreferences.setMaxSyncedDate]) is advanced
     * ONLY after a confirmed successful IMAP append. The confirmation is the return value of
     * [MailTransport.appendMessages]: it returns the confirmed max-date if and only if the
     * k-9 append returned normally (no exception). If [openFolder] or [appendMessages] throws
     * (folder error, login error, append error, cancellation), the exception propagates out of
     * the loop before [setMaxSyncedDate] can be called, so the watermark is never updated for
     * the failed batch. Per-batch watermark writes are preserved (durable checkpoint behavior):
     * if batch N succeeds and batch N+1 fails, batch N's watermark remains committed and only
     * batch N+1 is retried.
     *
     * U-053 (TE-003): the confirmed-append/watermark gating is now in [appendBatchAndUpdateWatermark],
     * a named `internal` seam exercised directly by [BackupWorkerWatermarkTest].
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

        val transport = config.imapStore

        // Emit LOGIN state (mirrors publish(LOGIN) at BackupTask.java:261)
        setProgress(workDataOf(PROGRESS_KEY_STATE to STATE_LOGIN))
        // U-026: checkSettings via transport port
        transport.checkSettings()

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

                    // U-053 (TE-003): delegate confirmed-append + watermark gating to the
                    // named internal seam so tests can exercise this production code directly.
                    appendBatchAndUpdateWatermark(transport, cursor.type, result, preferences.dataTypePreferences)

                    // Calendar sync per CALLLOG batch (AC-10c / BackupTask.java:282-284)
                    if (cursor.type == DataType.CALLLOG && calendarSyncer != null) {
                        calendarSyncer.syncCalendar(result)
                    }

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
            // U-026: closeFolders via transport port (does not throw)
            transport.closeFolders()
        }
    }

    /**
     * Confirmed-append / watermark-gating seam (U-053, TE-003).
     *
     * Performs the three-step BUG-010 invariant in one named, testable unit:
     *   1. [MailTransport.openFolder] — can throw; watermark never touched if it does
     *   2. [MailTransport.appendMessages] — can throw; watermark never touched if it does
     *   3. [DataTypePreferences.setMaxSyncedDate] — reached ONLY after a confirmed append
     *
     * This is the single production implementation of the "watermark advances only on
     * confirmed append" rule. [BackupWorkerWatermarkTest] invokes this method directly
     * to assert BUG-010 cases against production code (not a re-implementation copy).
     *
     * Visibility: `internal` so it is accessible from tests in the same Gradle module
     * without reflection, while remaining hidden from external callers outside the module.
     *
     * @param transport  the active [MailTransport] for this backup run
     * @param type       the [DataType] whose IMAP folder receives the messages
     * @param result     the [ConversionResult] containing the messages to append
     * @param dataTypePreferences  the preferences used to persist the watermark
     * @throws MailException if [openFolder] or [appendMessages] fails
     * @throws RequiresLoginException if credentials are rejected
     */
    @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)
    internal fun appendBatchAndUpdateWatermark(
        transport: MailTransport,
        type: DataType,
        result: ConversionResult,
        dataTypePreferences: DataTypePreferences
    ) {
        // BUG-010 fix (U-042): openFolder + appendMessages via transport port.
        // appendMessages returns the confirmed max-date of the appended messages.
        // The watermark is advanced to EXACTLY this value — never to a separately-computed
        // date and never ahead of unconfirmed messages.
        // If openFolder or appendMessages throws, confirmedMaxDate is never assigned
        // and setMaxSyncedDate is never called for this batch.
        val folder: BackupFolderHandle = transport.openFolder(type, dataTypePreferences)
        val confirmedMaxDate: Long = transport.appendMessages(folder, result)

        // BUG-010 fix: watermark advances ONLY to confirmedMaxDate (the value
        // returned by appendMessages, not wall-clock time and not a pre-computed
        // result field). Per-batch write for durable checkpoint behavior (U-016).
        dataTypePreferences.setMaxSyncedDate(type, confirmedMaxDate)
        Log.d(TAG, "BackupWorker: watermark advanced for $type to $confirmedMaxDate")
    }

    /**
     * XOAuth2 token-refresh retry. At most one retry (config.currentTry < 1).
     * Mirrors BackupTask.handleAuthError (AC-10b / BackupTask.java:183-205).
     *
     * U-026: catches [MailException] instead of k-9 MessagingException for token-refresh errors.
     */
    private suspend fun handleAuthError(
        config: BackupConfig,
        preferences: Preferences,
        ctx: Context,
        e: XOAuth2FailedException,
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
                    Log.d(TAG, "BackupWorker: token refreshed, retrying with new transport")
                    // U-024: mailTransportFactory.create() constructs a fresh transport per retry
                    // (auth params are immutable; the retry path always needs a new transport
                    // per DES-MODERNIZATION-008 §Behavior-preservation guarantees, point 2).
                    // U-050 AR-004: contactAccessor is now the injected field, not a new ContactAccessor().
                    val newTransport = mailTransportFactory.create()
                    val retryConfig = config.retryWithTransport(newTransport)
                    val retryGroupIds = contactAccessor.getGroupContactIds(
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
                } catch (ignored: MailException) {
                    // U-026: MailException replaces MessagingException
                    Log.w(TAG, "BackupWorker: MailException during token refresh", ignored)
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
     * Returns the set of enabled DataTypes from preferences.
     *
     * U-026: throws [MailException] instead of k-9 MessagingException.
     */
    private fun getEnabledBackupTypes(preferences: Preferences): EnumSet<DataType> {
        val enabled = preferences.dataTypePreferences.enabled()
        if (enabled.isEmpty()) {
            throw MailException("No backup types enabled")
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

    /**
     * U-049 AC-2: Builds the ForegroundInfo used by setForeground() at the start of doWork().
     * Replaces the foreground-service notification that SmsBackupService.notifyAboutBackup()
     * previously posted via startForeground(). Same notification channel (App.CHANNEL_ID),
     * same notification ID (BACKUP_ID = 1), same content as the legacy service notification.
     * The pending intent opens MainActivity so the user can monitor progress.
     */
    @Suppress("DEPRECATION")
    private fun createBackupForegroundInfo(): ForegroundInfo {
        val ctx = applicationContext
        val intent = Intent(ctx, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(ctx)
            .setSmallIcon(R.drawable.ic_notification)
            .setChannelId(App.CHANNEL_ID)
            .setTicker(ctx.getString(R.string.status_backup))
            .setContentTitle(ctx.getString(R.string.status_backup))
            .setContentText(ctx.getString(R.string.status_backup))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setWhen(System.currentTimeMillis())
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(BACKUP_NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(BACKUP_NOTIFICATION_ID, notification)
        }
    }

    companion object {
        internal const val TAG = "SMSBackup+"

        /** Foreground-service notification ID for backup (matches SmsBackupService.BACKUP_ID). */
        const val BACKUP_NOTIFICATION_ID = 1

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

    /**
     * Test-only [WorkerFactory] for [BackupWorker] unit tests.
     *
     * U-024: BackupWorker's @AssistedInject constructor requires Preferences, AuthPreferences,
     * and MailTransportFactory — none of which are available via the default reflective factory.
     * Tests use this factory to construct the worker with production-equivalent instances from
     * context (no real IMAP transport — tests that reach the backup logic will fail with a
     * MailException due to no IMAP URI, which is the expected behaviour for unit tests).
     *
     * Tests that require real transport behaviour or full DI should use the Hilt test harness
     * (@HiltAndroidTest + HiltAndroidRule) with instrumented tests.
     */
    class TestableBackupWorkerFactory : androidx.work.WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): androidx.work.ListenableWorker? {
            return if (workerClassName == BackupWorker::class.java.name) {
                val prefs = Preferences(appContext)
                val authPrefs = AuthPreferences(appContext)
                BackupWorker(
                    appContext,
                    workerParameters,
                    prefs,
                    authPrefs,
                    MailTransportFactory { throw MailException("TestableBackupWorkerFactory: transport not available in unit tests") },
                    // U-050 AR-004: supply ContactAccessor and WorkerEngineFactory for the new params
                    ContactAccessor(),
                    WorkerEngineFactory(appContext, prefs, authPrefs)
                )
            } else null
        }
    }

    /**
     * Test-only [WorkerFactory] for [BackupWorker] watermark regression tests (U-042 / BUG-010).
     *
     * Accepts a pre-built [MailTransport] so tests can inject a mock that either succeeds or
     * throws on [MailTransport.appendMessages]. This enables regression testing of the
     * "watermark advances only on confirmed append" invariant without a live IMAP connection.
     *
     * Usage: pass a Mockito mock (or a lambda-based stub) for [transport]; the factory injects
     * it via [MailTransportFactory] so each [doWork] call receives the same transport instance.
     */
    class TestableBackupWorkerFactoryWithTransport(
        private val transport: MailTransport
    ) : androidx.work.WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): androidx.work.ListenableWorker? {
            return if (workerClassName == BackupWorker::class.java.name) {
                val prefs = Preferences(appContext)
                val authPrefs = AuthPreferences(appContext)
                BackupWorker(
                    appContext,
                    workerParameters,
                    prefs,
                    authPrefs,
                    MailTransportFactory { transport },
                    // U-050 AR-004: supply ContactAccessor and WorkerEngineFactory for the new params
                    ContactAccessor(),
                    WorkerEngineFactory(appContext, prefs, authPrefs)
                )
            } else null
        }
    }
}
