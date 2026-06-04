package com.zegoggles.smssync.service

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import com.zegoggles.smssync.service.state.SyncEvent
import com.zegoggles.smssync.service.state.SyncStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * U-031: Replaces [BackupCancelCollector] — routes SyncEvent.Cancel(USER) to
 * [WorkManager.cancelUniqueWork] rather than to AsyncTask.onCancelRequested().
 *
 * Collects the first [SyncEvent.Cancel] from the repository and calls
 * [WorkManager.cancelUniqueWork] for the given unique-work name.
 * The worker's cooperative ensureActive() handles the interrupt at the next suspension point.
 * The AC-1 WorkInfo observer in the service drives stopForeground/stopSelf on the resulting
 * CANCELLED terminal WorkInfo.State.
 *
 * Used by both SmsBackupService (backup unique-work names: "MANUAL" or "SKIP") and
 * SmsRestoreService (restore unique-work name: RestoreWorker.RESTORE_WORK_NAME = "RESTORE").
 *
 * Functions are internal so they are accessible from the Java service classes in the same package.
 */
internal object WorkManagerCancelCollector {

    private val TAG = "SMSBackup+"

    /**
     * Starts collecting [SyncEvent.Cancel] events from [repository] and cancels the WorkManager
     * unique-work item with [uniqueWorkName] when one arrives.
     *
     * @return a [Job] that can be cancelled (e.g. in Service.onDestroy()) to stop listening.
     */
    @JvmStatic
    fun collect(context: Context, repository: SyncStateRepository, uniqueWorkName: String): Job {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return scope.launch {
            try {
                // Wait for the first Cancel event (any origin — USER or SYSTEM)
                repository.events
                    .filterIsInstance<SyncEvent.Cancel>()
                    .first()
                Log.d(TAG, "WorkManagerCancelCollector: Cancel received, cancelling $uniqueWorkName")
                WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)
            } catch (e: Exception) {
                Log.w(TAG, "WorkManagerCancelCollector: error collecting cancel for $uniqueWorkName", e)
            }
        }
    }
}
