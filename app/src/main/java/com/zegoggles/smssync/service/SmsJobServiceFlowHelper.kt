package com.zegoggles.smssync.service

import com.zegoggles.smssync.service.state.BackupState
import com.zegoggles.smssync.service.state.SyncStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * U-020: Kotlin helper that bridges StateFlow observation for SmsJobService.
 * Replaces the Otto @Subscribe backupStateChanged pattern.
 */
object SmsJobServiceFlowHelper {

    @JvmStatic
    fun observeBackupState(repository: SyncStateRepository, service: SmsJobService): Job {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        return scope.launch {
            repository.state
                .filterIsInstance<BackupState>()
                .collect { state -> service.onBackupStateChanged(state) }
        }
    }
}
