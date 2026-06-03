package com.zegoggles.smssync.activity

import com.zegoggles.smssync.service.state.BackupState
import com.zegoggles.smssync.service.state.RestoreState
import com.zegoggles.smssync.service.state.SyncEvent
import com.zegoggles.smssync.service.state.SyncStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * U-020: Kotlin helper that bridges StateFlow/SharedFlow collection for StatusPreference.
 *
 * AC-15: replaces App.register/unregister. Uses a CoroutineScope scoped to the
 * preference's attached lifecycle (cancelled in StatusPreference.onDetached()).
 */
object StatusPreferenceFlowHelper {

    @JvmStatic
    fun startCollection(
        repository: SyncStateRepository,
        preference: StatusPreference
    ): CoroutineScope {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        // State flow: route BackupState / RestoreState
        scope.launch {
            repository.state.collect { state ->
                when (state) {
                    is BackupState -> preference.backupStateChanged(state)
                    is RestoreState -> preference.restoreStateChanged(state)
                    else -> { /* Initial state — no UI update */ }
                }
            }
        }

        // Events flow: route MissingPermissions
        scope.launch {
            repository.events
                .filterIsInstance<SyncEvent.MissingPermissions>()
                .collect { event -> preference.onMissingPermissions(event) }
        }

        return scope
    }

    @JvmStatic
    fun cancelScope(scope: CoroutineScope) {
        scope.cancel()
    }
}
