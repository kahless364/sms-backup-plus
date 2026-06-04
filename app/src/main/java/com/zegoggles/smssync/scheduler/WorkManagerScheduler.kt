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
package com.zegoggles.smssync.scheduler

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.work.*
import androidx.work.PeriodicWorkRequest
import com.zegoggles.smssync.Consts
import com.zegoggles.smssync.preferences.Preferences
import com.zegoggles.smssync.service.BackupType
import com.zegoggles.smssync.service.BackupWorker
import com.zegoggles.smssync.service.RestoreWorker
import com.zegoggles.smssync.worker.BackupTriggerWorker
import java.util.concurrent.TimeUnit

/**
 * WorkManager implementation of [BackupScheduler] — the second adapter alongside
 * [LegacyScheduler], introduced by U-014.
 *
 * This adapter maps all port operations to [WorkRequest]s while preserving the four
 * invariants mandated by CNTR-MODERNIZATION-004:
 *
 * - **INV-1 single-flight**: [ExistingWorkPolicy.REPLACE] for one-off work;
 *   [ExistingPeriodicWorkPolicy.UPDATE] for periodic. Source: `setReplaceCurrent(true)` on
 *   every job in `BackupJobs.java:188`.
 *
 * - **INV-2 network constraints**: [NetworkType.UNMETERED] when [Preferences.isWifiOnly];
 *   [NetworkType.CONNECTED] otherwise; [Constraints.NONE] for BROADCAST_INTENT (no
 *   network constraint for immediate backup). Source: `jobConstraints()` in
 *   `BackupJobs.java:195-201`; `BROADCAST_INTENT -> new int[0]`.
 *
 * - **INV-3 backoff fidelity**: [BackoffPolicy.EXPONENTIAL], initial 30 seconds. Source:
 *   `newRetryStrategy(RETRY_POLICY_EXPONENTIAL, 30, 300)` at `BackupJobs.java:205`.
 *   **IMPORTANT SPLIT-RESPONSIBILITY**: WorkManager's internal [WorkRequest.MAX_BACKOFF_MILLIS]
 *   is ~5 hours, much larger than 300s. The 300s ceiling MUST be re-imposed at the worker
 *   layer by [BackupWorker] in U-015 (by checking run attempt count and returning
 *   [ListenableWorker.Result.failure] when effective delay would exceed 300s). This is not
 *   implementable here because the WorkManager API does not provide a cap parameter.
 *
 * - **INV-4 content-URI trigger**: API 24+ uses [Constraints.Builder.addContentUriTrigger]
 *   with [triggerForDescendants = true]; below API 24 the broadcast path calls
 *   [scheduleIncoming] instead. The two-stage debounce is preserved: the triggered
 *   [BackupTriggerWorker] enqueues a delayed [BackupWorker] rather than backing up directly.
 *   Source: `Trigger.contentUriTrigger(observedUris())` + `FLAG_NOTIFY_FOR_DESCENDANTS`
 *   at legacy BackupJobs.java:179,181; two-stage debounce preserved in BackupTriggerWorker.
 *
 * **Production binding**: this is the sole production BackupScheduler implementation (U-017).
 * LegacyScheduler, BackupJobs, and the legacy Firebase JobDispatcher classes have been deleted.
 *
 * **Unique work names**: [BackupType.name] for regular/incoming/immediate, plus
 * [CONTENT_TRIGGER_UNIQUE_NAME] for the content-trigger periodic work. Names must be
 * stable across app restarts because WorkManager persists work by unique name.
 *
 * **Ordering constraint**: WorkManager custom initialization via [Configuration.Provider]
 * and [HiltWorkerFactory] is owned by U-024. Until U-024 lands, the default WorkManager
 * initializer (declared in the AndroidX work manifest) is in use; do NOT remove it from
 * the manifest before U-024 merges.
 *
 * **[scheduleRestore]**: stub returning a [ScheduledJob] — full durable restore checkpoint
 * is U-016.
 */
class WorkManagerScheduler(
    private val context: Context,
    private val preferences: Preferences
) : BackupScheduler {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    companion object {
        private const val TAG = "SMSBackup+"

        /** Stable unique-work name for the content-trigger periodic job. */
        const val CONTENT_TRIGGER_UNIQUE_NAME = "CONTENT_TRIGGER"

        /** Boot delay from BackupJobs.java:56 (BOOT_BACKUP_DELAY = 60 seconds). */
        private const val BOOT_BACKUP_DELAY_SECS = 60L

        /**
         * WorkManager backoff initial delay (seconds) matching
         * newRetryStrategy(RETRY_POLICY_EXPONENTIAL, 30, 300) at BackupJobs.java:205.
         */
        const val BACKOFF_INITIAL_SECS = 30L

        /**
         * Maximum effective backoff delay (seconds) — matches the cap parameter of
         * newRetryStrategy(RETRY_POLICY_EXPONENTIAL, 30, 300) at BackupJobs.java:205.
         * This cap MUST be re-imposed at the worker layer (U-015); the WorkManager API
         * does not accept a cap parameter. Declared here for documentation and test reference.
         */
        const val BACKOFF_MAX_SECS = 300L

        /**
         * Minimum interval for PeriodicWorkRequest is 15 minutes per WorkManager API.
         * This fallback is used when getRegularTimeoutSecs() returns a value below 15 minutes.
         */
        private val PERIODIC_MIN_INTERVAL_SECS = TimeUnit.MINUTES.toSeconds(15)
    }

    // -----------------------------------------------------------------------
    // Scheduling operations
    // -----------------------------------------------------------------------

    /**
     * Schedules a delayed one-off incoming backup.
     * INV-1: REPLACE; INV-2: UNMETERED or CONNECTED per isWifiOnly(); INV-3: EXPONENTIAL/30s.
     * Delay = [Preferences.getIncomingTimeoutSecs] seconds.
     * Returns null if autoBackupEnabled is false or timeout <= 0.
     */
    override fun scheduleIncoming(): ScheduledJob? {
        val timeoutSecs = preferences.incomingTimeoutSecs
        if (!preferences.isAutoBackupEnabled || timeoutSecs <= 0) {
            Log.d(TAG, "WorkManagerScheduler.scheduleIncoming: skipped (autoBackup=" +
                    "${preferences.isAutoBackupEnabled}, timeout=$timeoutSecs)")
            return null
        }

        val request = OneTimeWorkRequest.Builder(BackupWorker::class.java)
            .setConstraints(networkConstraints(BackupType.INCOMING))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_INITIAL_SECS, TimeUnit.SECONDS)
            .setInitialDelay(timeoutSecs.toLong(), TimeUnit.SECONDS)
            .addTag(BackupType.INCOMING.name)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(BackupType.INCOMING.name, ExistingWorkPolicy.REPLACE, request)

        Log.d(TAG, "WorkManagerScheduler.scheduleIncoming: enqueued INCOMING with " +
                "delay=${timeoutSecs}s, network=${constraintDescription(BackupType.INCOMING)}")
        return ScheduledJob(BackupType.INCOMING.name, "WorkManagerScheduler:INCOMING @ ${timeoutSecs}s")
    }

    /**
     * Schedules a recurring backup.
     * INV-1: UPDATE on stable unique name; INV-2: UNMETERED or CONNECTED; INV-3: EXPONENTIAL/30s.
     * Interval = [Preferences.getRegularTimeoutSecs] seconds (minimum 15 minutes per WM API).
     * Returns null if autoBackupEnabled is false.
     */
    override fun scheduleRegular(): ScheduledJob? {
        if (!preferences.isAutoBackupEnabled) {
            Log.d(TAG, "WorkManagerScheduler.scheduleRegular: skipped (autoBackup disabled)")
            return null
        }

        val intervalSecs = maxOf(preferences.regularTimeoutSecs.toLong(), PERIODIC_MIN_INTERVAL_SECS)

        val request = PeriodicWorkRequest.Builder(
            BackupWorker::class.java,
            intervalSecs,
            TimeUnit.SECONDS
        )
            .setConstraints(networkConstraints(BackupType.REGULAR))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_INITIAL_SECS, TimeUnit.SECONDS)
            .addTag(BackupType.REGULAR.name)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                BackupType.REGULAR.name,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )

        Log.d(TAG, "WorkManagerScheduler.scheduleRegular: enqueued REGULAR with " +
                "interval=${intervalSecs}s, network=${constraintDescription(BackupType.REGULAR)}")
        return ScheduledJob(BackupType.REGULAR.name, "WorkManagerScheduler:REGULAR @ ${intervalSecs}s")
    }

    /**
     * Schedules the content-URI observer over the SMS provider (and call-log when enabled).
     *
     * API 24+: uses [Constraints.Builder.addContentUriTrigger] with triggerForDescendants=true.
     * Below API 24: the SMS-received broadcast path calls [scheduleIncoming] instead; this
     * method returns null to signal that the API-24+ trigger was not registered.
     *
     * INV-4: two-stage debounce — the triggered [BackupTriggerWorker] enqueues a delayed
     * incoming backup rather than backing up directly.
     */
    override fun scheduleContentTrigger(): ScheduledJob? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // Below API 24: content-URI triggers are unavailable.
            // The broadcast fallback (SmsBroadcastReceiver → scheduleIncoming()) covers this path.
            Log.d(TAG, "WorkManagerScheduler.scheduleContentTrigger: API < 24, " +
                    "using broadcast fallback (SmsBroadcastReceiver)")
            return null
        }

        val constraintsBuilder = Constraints.Builder()
            .addContentUriTrigger(Consts.SMS_PROVIDER, true)

        if (preferences.dataTypePreferences.isBackupEnabled(com.zegoggles.smssync.mail.DataType.CALLLOG)
            && preferences.isCallLogBackupAfterCallEnabled()) {
            constraintsBuilder.addContentUriTrigger(Consts.CALLLOG_PROVIDER, true)
        }

        val request = PeriodicWorkRequest.Builder(
            BackupTriggerWorker::class.java,
            PERIODIC_MIN_INTERVAL_SECS,
            TimeUnit.SECONDS
        )
            .setConstraints(constraintsBuilder.build())
            .addTag(CONTENT_TRIGGER_UNIQUE_NAME)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                CONTENT_TRIGGER_UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )

        Log.d(TAG, "WorkManagerScheduler.scheduleContentTrigger: enqueued CONTENT_TRIGGER (API 24+)")
        return ScheduledJob(CONTENT_TRIGGER_UNIQUE_NAME, "WorkManagerScheduler:CONTENT_TRIGGER")
    }

    /**
     * Schedules (or cancels) jobs on device boot.
     * If !isAutoBackupEnabled: cancels all; returns null.
     * Otherwise: schedules regular backup with BOOT_BACKUP_DELAY (60s) initial delay.
     * Source: BackupJobs.java:86-97; BOOT_BACKUP_DELAY = 60 (line 56).
     */
    override fun scheduleBootup(): ScheduledJob? {
        if (!preferences.isAutoBackupEnabled) {
            Log.d(TAG, "WorkManagerScheduler.scheduleBootup: autoBackup disabled, cancelling all")
            cancelAll()
            return null
        }

        val request = OneTimeWorkRequest.Builder(BackupWorker::class.java)
            .setConstraints(networkConstraints(BackupType.REGULAR))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_INITIAL_SECS, TimeUnit.SECONDS)
            .setInitialDelay(BOOT_BACKUP_DELAY_SECS, TimeUnit.SECONDS)
            .addTag(BackupType.REGULAR.name)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(BackupType.REGULAR.name, ExistingWorkPolicy.REPLACE, request)

        Log.d(TAG, "WorkManagerScheduler.scheduleBootup: enqueued bootup REGULAR with " +
                "${BOOT_BACKUP_DELAY_SECS}s delay")
        return ScheduledJob(BackupType.REGULAR.name, "WorkManagerScheduler:bootup @ ${BOOT_BACKUP_DELAY_SECS}s")
    }

    /**
     * Schedules an immediate one-off backup with NO network constraint.
     * INV-1: REPLACE; INV-2: Constraints.NONE (no network for BROADCAST_INTENT).
     * Source: BackupJobs.java:99; jobConstraints BROADCAST_INTENT returns new int[0] (line 197).
     *
     * IMPORTANT: Constraints.NONE means NetworkType.NOT_REQUIRED — not CONNECTED.
     * Using CONNECTED would defer backups on devices without active connectivity at trigger
     * time, breaking Tasker / third-party automation that sends the BACKUP broadcast.
     */
    override fun scheduleImmediate(): ScheduledJob? {
        val request = OneTimeWorkRequest.Builder(BackupWorker::class.java)
            .setConstraints(Constraints.NONE)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_INITIAL_SECS, TimeUnit.SECONDS)
            .addTag(BackupType.BROADCAST_INTENT.name)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(BackupType.BROADCAST_INTENT.name, ExistingWorkPolicy.REPLACE, request)

        Log.d(TAG, "WorkManagerScheduler.scheduleImmediate: enqueued BROADCAST_INTENT with Constraints.NONE")
        return ScheduledJob(BackupType.BROADCAST_INTENT.name, "WorkManagerScheduler:BROADCAST_INTENT immediate")
    }

    /**
     * Schedules an immediate one-off manual backup carrying the engine [BackupType].
     *
     * U-031 / CNTR-MODERNIZATION-004 v2 (Validation Rule 8): dispatches manual backup/skip from
     * [SmsBackupService]. Preserves MANUAL vs SKIP semantics by tagging with [BackupType.name],
     * so [BackupWorker.inferBackupType] resolves the correct type.
     *
     * Constraint-identical to [scheduleImmediate] (Constraints.NONE, REPLACE, EXPONENTIAL/30s)
     * but unique-work name = backupType.name() ("MANUAL" / "SKIP") — distinct from
     * "BROADCAST_INTENT", so manual and automation-broadcast runs do NOT replace each other.
     *
     * Accepts only [BackupType.MANUAL] or [BackupType.SKIP]; other values are contract violations.
     *
     * INV-1: REPLACE; INV-2: Constraints.NONE; INV-3: EXPONENTIAL/30s; INV unique-name: distinct.
     */
    override fun scheduleManual(backupType: BackupType): ScheduledJob? {
        require(backupType == BackupType.MANUAL || backupType == BackupType.SKIP) {
            "scheduleManual accepts only MANUAL or SKIP, got $backupType"
        }
        val request = OneTimeWorkRequest.Builder(BackupWorker::class.java)
            .setConstraints(Constraints.NONE)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_INITIAL_SECS, TimeUnit.SECONDS)
            .addTag(backupType.name)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(backupType.name, ExistingWorkPolicy.REPLACE, request)

        Log.d(TAG, "WorkManagerScheduler.scheduleManual: enqueued ${backupType.name} with Constraints.NONE")
        return ScheduledJob(backupType.name, "WorkManagerScheduler:${backupType.name} manual")
    }

    /**
     * Schedules a one-off restore job backed by [RestoreWorker].
     *
     * The full durable-checkpoint implementation (persist cursor after each insert,
     * resume from checkpointKey on re-execution) is U-016. This implementation enqueues
     * the real [RestoreWorker] so the restore executes via WorkManager; U-016 adds
     * durability/resumability on top without changing this enqueue call.
     *
     * INV-1: REPLACE semantics via [ExistingWorkPolicy.REPLACE].
     * No network constraint for restore (restore is user-initiated, not background-triggered).
     */
    override fun scheduleRestore(config: RestoreSchedulerConfig): ScheduledJob? {
        val request = OneTimeWorkRequest.Builder(RestoreWorker::class.java)
            .setConstraints(Constraints.NONE)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_INITIAL_SECS, TimeUnit.SECONDS)
            .addTag(config.uniqueName)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(config.uniqueName, ExistingWorkPolicy.REPLACE, request)

        Log.d(TAG, "WorkManagerScheduler.scheduleRestore: enqueued RestoreWorker for ${config.uniqueName}")
        return ScheduledJob(config.uniqueName, "WorkManagerScheduler:restore(${config.uniqueName})")
    }

    // -----------------------------------------------------------------------
    // Cancellation operations
    // -----------------------------------------------------------------------

    /** Cancels both the periodic backup job and the content-trigger job. */
    override fun cancelAll() {
        cancelRegular()
        WorkManager.getInstance(context).cancelUniqueWork(CONTENT_TRIGGER_UNIQUE_NAME)
        Log.d(TAG, "WorkManagerScheduler.cancelAll: cancelled REGULAR + CONTENT_TRIGGER")
    }

    /** Cancels the periodic backup job only. */
    override fun cancelRegular() {
        WorkManager.getInstance(context).cancelUniqueWork(BackupType.REGULAR.name)
        Log.d(TAG, "WorkManagerScheduler.cancelRegular: cancelled REGULAR")
    }

    /** Single-flight cancellation by job kind. */
    override fun cancel(jobKind: BackupType) {
        // U-031: MANUAL and SKIP use backupType.name() as unique-work names (distinct from
        // BROADCAST_INTENT). Cancel the correct unique-work name for each type.
        val uniqueName = when (jobKind) {
            BackupType.REGULAR -> BackupType.REGULAR.name
            BackupType.INCOMING -> BackupType.INCOMING.name
            BackupType.BROADCAST_INTENT -> BackupType.BROADCAST_INTENT.name
            BackupType.MANUAL -> BackupType.MANUAL.name
            BackupType.SKIP -> BackupType.SKIP.name
            else -> {
                Log.d(TAG, "WorkManagerScheduler.cancel($jobKind): no unique-work mapping; no-op")
                return
            }
        }
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName)
        Log.d(TAG, "WorkManagerScheduler.cancel($jobKind): cancelled $uniqueName")
    }

    // -----------------------------------------------------------------------
    // Observability
    // -----------------------------------------------------------------------

    /**
     * Returns a [SchedulerObservable] backed by a static [SchedulerState.Enqueued] value.
     *
     * Full WorkManager [WorkInfo] → SchedulerState live observable is U-020 (Otto→StateFlow).
     * Until that story lands, this returns a static state so callers get a non-null observable
     * without a crash.
     */
    override fun observe(jobKind: BackupType): SchedulerObservable<SchedulerState> {
        return SchedulerObservable(SchedulerState.Enqueued)
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Builds [Constraints] for a given [BackupType].
     *
     * INV-2: BROADCAST_INTENT → Constraints.NONE (no network).
     * All other types: UNMETERED when isWifiOnly, CONNECTED otherwise.
     * requiresCharging: NEVER set (no charging constraint anywhere in legacy jobConstraints).
     */
    private fun networkConstraints(backupType: BackupType): Constraints {
        return when (backupType) {
            BackupType.BROADCAST_INTENT -> Constraints.NONE
            else -> Constraints.Builder()
                .setRequiredNetworkType(
                    if (preferences.isWifiOnly) NetworkType.UNMETERED
                    else NetworkType.CONNECTED
                )
                .build()
        }
    }

    private fun constraintDescription(backupType: BackupType): String {
        return when (backupType) {
            BackupType.BROADCAST_INTENT -> "NONE"
            else -> if (preferences.isWifiOnly) "UNMETERED" else "CONNECTED"
        }
    }
}
