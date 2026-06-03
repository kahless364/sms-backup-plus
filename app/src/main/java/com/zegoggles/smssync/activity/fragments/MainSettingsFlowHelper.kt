package com.zegoggles.smssync.activity.fragments

import com.zegoggles.smssync.service.state.SyncEvent
import com.zegoggles.smssync.service.state.SyncStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * U-020: Kotlin helper that bridges SyncEvent collection for MainSettings.
 *
 * Replaces Otto @Subscribe handlers:
 * - onAccountAdded(AccountAddedEvent)
 * - onAccountRemoved(AccountRemovedEvent)
 * - onAutoBackupSettingsChanged(AutoBackupSettingsChangedEvent)
 * - onSettingsReset(SettingsResetEvent)
 */
object MainSettingsFlowHelper {

    @JvmStatic
    fun startCollection(
        repository: SyncStateRepository,
        fragment: MainSettings
    ): Job {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        return scope.launch {
            repository.events.collect { event ->
                when (event) {
                    is SyncEvent.AccountAdded -> fragment.onAccountAdded()
                    is SyncEvent.AccountRemoved -> fragment.onAccountRemoved()
                    is SyncEvent.AutoBackupSettingsChanged -> fragment.onAutoBackupSettingsChanged()
                    is SyncEvent.SettingsReset -> fragment.onSettingsReset()
                    else -> { /* not handled here */ }
                }
            }
        }
    }
}
