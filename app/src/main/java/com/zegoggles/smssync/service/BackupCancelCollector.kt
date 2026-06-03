package com.zegoggles.smssync.service

import com.zegoggles.smssync.service.state.SyncEvent
import com.zegoggles.smssync.service.state.SyncStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * U-020: Collects the first SyncEvent.Cancel from the repository and propagates
 * it to a BackupTask or RestoreTask via onCancelRequested().
 *
 * This replaces the Otto @Subscribe canceled(CancelEvent) pattern.
 * Uses flow.first() — the coroutine completes after one Cancel event is received.
 *
 * Functions are internal because BackupTask and RestoreTask are package-private.
 */
internal object BackupCancelCollector {

    @JvmStatic
    fun collect(repository: SyncStateRepository, task: BackupTask): Job {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return scope.launch {
            val event = repository.events
                .filterIsInstance<SyncEvent.Cancel>()
                .first()
            task.onCancelRequested(event.mayInterruptIfRunning())
        }
    }

    @JvmStatic
    fun collectForRestore(repository: SyncStateRepository, task: RestoreTask): Job {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return scope.launch {
            val event = repository.events
                .filterIsInstance<SyncEvent.Cancel>()
                .first()
            task.onCancelRequested(event.mayInterruptIfRunning())
        }
    }
}
