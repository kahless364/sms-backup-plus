package com.zegoggles.smssync

import com.zegoggles.smssync.service.state.SyncEvent
import com.zegoggles.smssync.service.state.SyncStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * U-020: Kotlin helper that bridges Flow collection into Java-friendly callbacks.
 * Used by App.java to collect SyncEvent.AutoBackupSettingsChanged from the
 * application-scoped SyncStateRepository without direct Kotlin coroutine API from Java.
 */
object FlowCollectHelper {

    /**
     * Collects [SyncEvent.AutoBackupSettingsChanged] events from [repository]
     * and calls [onChanged] on the Main dispatcher.
     * Returns a [Job] that survives for the application's lifetime (fire-and-forget).
     */
    @JvmStatic
    fun collectAutoBackupSettings(
        repository: SyncStateRepository,
        onChanged: Runnable
    ): Job {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        return scope.launch {
            repository.events
                .filterIsInstance<SyncEvent.AutoBackupSettingsChanged>()
                .collect { onChanged.run() }
        }
    }
}
