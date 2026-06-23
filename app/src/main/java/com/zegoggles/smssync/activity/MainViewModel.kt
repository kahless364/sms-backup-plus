package com.zegoggles.smssync.activity

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.mail.transport.MailException
import com.zegoggles.smssync.scheduler.BackupScheduler
import com.zegoggles.smssync.scheduler.RestoreSchedulerConfig
import com.zegoggles.smssync.service.BackupType
import com.zegoggles.smssync.service.BackupWorker
import com.zegoggles.smssync.service.RestoreWorker
import com.zegoggles.smssync.service.WorkManagerCancelCollector
import com.zegoggles.smssync.service.state.BackupState
import com.zegoggles.smssync.service.state.RestoreState
import com.zegoggles.smssync.service.state.SmsSyncState
import com.zegoggles.smssync.service.state.SyncEvent
import com.zegoggles.smssync.service.state.SyncState
import com.zegoggles.smssync.service.state.SyncStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * U-020: ViewModel that exposes SyncStateRepository flows to MainActivity.
 *
 * AC-14: holds an injected SyncStateRepository; exposes state and events flows.
 *
 * U-024: @HiltViewModel + @Inject constructor — replaces the manual MainViewModelFactory.
 * Hilt validates at compile time that SyncStateRepository is bound in the component graph
 * (it is — EventModule.provideSyncStateRepository() is @Singleton in SingletonComponent,
 * accessible from ActivityRetainedComponent via parent component inheritance).
 * Callers obtain this ViewModel via by viewModels() (no explicit factory needed).
 *
 * U-049: Added WorkInfo observation for backup and restore workers (AC-3). The ViewModel
 * observes WorkManager via getWorkInfosForUniqueWorkLiveData() and maps WorkInfo to
 * BackupState/RestoreState, emitting to SyncStateRepository. This replaces the
 * SmsBackupService/SmsRestoreService WorkInfo observer bridge (deleted in U-049).
 * Also adds direct scheduler dispatch (AC-4): startBackup() and startRestore() call
 * the injected BackupScheduler rather than starting Services.
 * BUG-005 fix is preserved: viewModelScope-launched jobs are cancelled in onCleared()
 * automatically by the ViewModel lifecycle — no manual observer deregistration needed.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SyncStateRepository,
    private val scheduler: BackupScheduler
) : ViewModel() {

    private val TAG = "SMSBackup+"

    /** Current sync state (sticky — new collectors immediately see the last value). */
    val state: StateFlow<SyncState> = repository.state

    /** One-shot sync events (replay=0 — late collectors do NOT see prior events). */
    val events: SharedFlow<SyncEvent> = repository.events

    /**
     * U-049 AC-3 / BUG-005: Active WorkInfo observation jobs.
     * These are launched in viewModelScope, which is automatically cancelled in onCleared().
     * This is the BUG-005-equivalent cleanup for the ViewModel-owned observer.
     */
    private var backupObserverJob: Job? = null
    private var restoreObserverJob: Job? = null

    /**
     * U-049 AC-3 / BUG-005: Cancel collector jobs for routing SyncEvent.Cancel to WorkManager.
     * Also cleaned up in onCleared() via job.cancel().
     */
    private var backupCancelCollectorJob: kotlinx.coroutines.Job? = null
    private var restoreCancelCollectorJob: kotlinx.coroutines.Job? = null

    /**
     * Emits a one-shot event to the repository (non-suspending).
     * Callers must not silently discard the Boolean return (AC-17).
     */
    fun tryEmitEvent(event: SyncEvent): Boolean = repository.tryEmitEvent(event)

    /**
     * U-049 AC-4: Schedules a manual backup (MANUAL or SKIP type) via the injected
     * BackupScheduler and starts observing WorkInfo for progress. Replaces
     * MainActivity.startService(SmsBackupService.class) + the service-layer WorkInfo bridge.
     */
    fun startBackup(backupType: BackupType) {
        // Cancel any existing backup observation before starting a new one
        backupObserverJob?.cancel()
        backupCancelCollectorJob?.cancel()

        val job = scheduler.scheduleManual(backupType)
        if (job != null) {
            Log.d(TAG, "MainViewModel.startBackup: enqueued $backupType via scheduleManual, uniqueWork=${job.tag}")
            observeBackupWork(job.tag, backupType)
            startBackupCancelCollector(job.tag)
        } else {
            Log.w(TAG, "MainViewModel.startBackup: scheduleManual returned null for $backupType")
            repository.emitState(BackupState(SmsSyncState.ERROR, 0, 0, backupType, null,
                MailException("scheduleManual returned null")))
        }
    }

    /**
     * U-049 AC-4: Schedules a restore via the injected BackupScheduler and starts observing
     * WorkInfo for progress. Replaces MainActivity.startService(SmsRestoreService.class) +
     * the service-layer WorkInfo bridge.
     */
    fun startRestore() {
        // Cancel any existing restore observation before starting a new one
        restoreObserverJob?.cancel()
        restoreCancelCollectorJob?.cancel()

        val job = scheduler.scheduleRestore(
            RestoreSchedulerConfig(RestoreWorker.RESTORE_WORK_NAME, RestoreWorker.RESTORE_WORK_NAME)
        )
        if (job != null) {
            Log.d(TAG, "MainViewModel.startRestore: enqueued restore via scheduleRestore, uniqueWork=${job.tag}")
            observeRestoreWork(job.tag)
            startRestoreCancelCollector(job.tag)
        } else {
            Log.w(TAG, "MainViewModel.startRestore: scheduleRestore returned null")
            repository.emitState(RestoreState(SmsSyncState.ERROR, 0, 0, 0, 0, null,
                MailException("scheduleRestore returned null")))
        }
    }

    /**
     * U-049 AC-3: Observes WorkInfo for the given backup unique-work name and maps
     * each WorkInfo to a BackupState, emitting to SyncStateRepository.
     * Runs in viewModelScope — cancelled automatically in onCleared() (BUG-005 fix).
     */
    private fun observeBackupWork(uniqueWorkName: String, backupType: BackupType) {
        backupObserverJob = viewModelScope.launch {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(uniqueWorkName)
                .collect { workInfoList ->
                    if (workInfoList.isEmpty()) return@collect
                    val workInfo = workInfoList[0] ?: return@collect
                    val newState = mapWorkInfoToBackupState(workInfo, backupType)
                    if (newState != null) {
                        repository.emitState(newState)
                    }
                    if (workInfo.state.isFinished) {
                        backupObserverJob?.cancel()
                        backupCancelCollectorJob?.cancel()
                    }
                }
        }
    }

    /**
     * U-049 AC-3: Observes WorkInfo for the restore unique-work name and maps
     * each WorkInfo to a RestoreState, emitting to SyncStateRepository.
     * Runs in viewModelScope — cancelled automatically in onCleared() (BUG-005 fix).
     */
    private fun observeRestoreWork(uniqueWorkName: String) {
        restoreObserverJob = viewModelScope.launch {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(uniqueWorkName)
                .collect { workInfoList ->
                    if (workInfoList.isEmpty()) return@collect
                    val workInfo = workInfoList[0] ?: return@collect
                    val newState = mapWorkInfoToRestoreState(workInfo)
                    if (newState != null) {
                        repository.emitState(newState)
                    }
                    if (workInfo.state.isFinished) {
                        restoreObserverJob?.cancel()
                        restoreCancelCollectorJob?.cancel()
                    }
                }
        }
    }

    /**
     * U-049 AC-3: Maps a WorkInfo to a BackupState. Terminal and RUNNING states are handled.
     * Mirrors SmsBackupService.mapWorkInfoToBackupState().
     */
    private fun mapWorkInfoToBackupState(workInfo: WorkInfo, backupType: BackupType): BackupState? {
        return when (workInfo.state) {
            WorkInfo.State.SUCCEEDED ->
                BackupState(SmsSyncState.FINISHED_BACKUP, 0, 0, backupType, null, null)
            WorkInfo.State.FAILED ->
                BackupState(SmsSyncState.ERROR, 0, 0, backupType, null,
                    MailException("BackupWorker failed"))
            WorkInfo.State.CANCELLED ->
                BackupState(SmsSyncState.CANCELED_BACKUP, 0, 0, backupType, null, null)
            WorkInfo.State.RUNNING -> {
                val progressState = workInfo.progress.getString(BackupWorker.PROGRESS_KEY_STATE)
                    ?: return null
                val smsSyncState = mapBackupProgressState(progressState) ?: return null
                val backedUp = workInfo.progress.getInt(BackupWorker.PROGRESS_KEY_BACKED_UP, 0)
                val toSync = workInfo.progress.getInt(BackupWorker.PROGRESS_KEY_ITEMS_TO_SYNC, 0)
                val dataTypeName = workInfo.progress.getString(BackupWorker.PROGRESS_KEY_DATA_TYPE)
                val dataType = dataTypeName?.takeIf { it.isNotEmpty() }?.let {
                    try { DataType.valueOf(it) } catch (e: Exception) { null }
                }
                BackupState(smsSyncState, backedUp, toSync, backupType, dataType, null)
            }
            else -> null
        }
    }

    private fun mapBackupProgressState(progressState: String): SmsSyncState? {
        return when (progressState) {
            BackupWorker.STATE_LOGIN    -> SmsSyncState.LOGIN
            BackupWorker.STATE_CALC     -> SmsSyncState.CALC
            BackupWorker.STATE_BACKUP   -> SmsSyncState.BACKUP
            BackupWorker.STATE_FINISHED -> SmsSyncState.FINISHED_BACKUP
            BackupWorker.STATE_CANCELED -> SmsSyncState.CANCELED_BACKUP
            else -> null
        }
    }

    /**
     * U-049 AC-3: Maps a WorkInfo to a RestoreState. Terminal and RUNNING states are handled.
     * Mirrors SmsRestoreService.mapWorkInfoToRestoreState().
     */
    private fun mapWorkInfoToRestoreState(workInfo: WorkInfo): RestoreState? {
        return when (workInfo.state) {
            WorkInfo.State.SUCCEEDED ->
                RestoreState(SmsSyncState.FINISHED_RESTORE, 0, 0, 0, 0, null, null)
            WorkInfo.State.FAILED ->
                RestoreState(SmsSyncState.ERROR, 0, 0, 0, 0, null,
                    MailException("RestoreWorker failed"))
            WorkInfo.State.CANCELLED ->
                RestoreState(SmsSyncState.CANCELED_RESTORE, 0, 0, 0, 0, null, null)
            WorkInfo.State.RUNNING -> {
                val progressState = workInfo.progress.getString(RestoreWorker.PROGRESS_KEY_STATE)
                    ?: return null
                val smsSyncState = mapRestoreProgressState(progressState) ?: return null
                val currentItem = workInfo.progress.getInt(RestoreWorker.PROGRESS_KEY_CURRENT_ITEM, 0)
                val itemsToRestore = workInfo.progress.getInt(RestoreWorker.PROGRESS_KEY_ITEMS_TO_RESTORE, 0)
                val restoredCount = workInfo.progress.getInt(RestoreWorker.PROGRESS_KEY_RESTORED_COUNT, 0)
                RestoreState(smsSyncState, currentItem, itemsToRestore, restoredCount, 0, null, null)
            }
            else -> null
        }
    }

    private fun mapRestoreProgressState(progressState: String): SmsSyncState? {
        return when (progressState) {
            RestoreWorker.STATE_LOGIN    -> SmsSyncState.LOGIN
            RestoreWorker.STATE_CALC     -> SmsSyncState.CALC
            RestoreWorker.STATE_RESTORE  -> SmsSyncState.RESTORE
            RestoreWorker.STATE_FINISHED -> SmsSyncState.FINISHED_RESTORE
            RestoreWorker.STATE_CANCELED -> SmsSyncState.CANCELED_RESTORE
            else -> null
        }
    }

    /**
     * U-049 AC-3: Starts a cancel collector for backup that routes SyncEvent.Cancel to
     * WorkManager.cancelUniqueWork(). Uses WorkManagerCancelCollector (same as the deleted service).
     */
    private fun startBackupCancelCollector(uniqueWorkName: String) {
        backupCancelCollectorJob?.cancel()
        backupCancelCollectorJob = WorkManagerCancelCollector.collect(context, repository, uniqueWorkName)
    }

    /**
     * U-049 AC-3: Starts a cancel collector for restore.
     */
    private fun startRestoreCancelCollector(uniqueWorkName: String) {
        restoreCancelCollectorJob?.cancel()
        restoreCancelCollectorJob = WorkManagerCancelCollector.collect(context, repository, uniqueWorkName)
    }

    /**
     * U-049 AC-3 / BUG-005: Cancel all active observation jobs on ViewModel destruction.
     * This is the BUG-005 fix equivalent — observers are deregistered when the ViewModel
     * is cleared (Activity destroyed), preventing leaks.
     */
    override fun onCleared() {
        super.onCleared()
        backupObserverJob?.cancel()
        restoreObserverJob?.cancel()
        backupCancelCollectorJob?.cancel()
        restoreCancelCollectorJob?.cancel()
    }
}
