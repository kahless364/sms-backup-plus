package com.zegoggles.smssync.activity

import com.zegoggles.smssync.service.state.SyncEvent
import com.zegoggles.smssync.service.state.SyncStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * U-020: Kotlin helper that bridges SyncEvent collection for Dialogs inner classes.
 *
 * OAuth2AccessTokenProgress previously used Otto @Subscribe + App.register/unregister.
 * This replaces that with a coroutine-based flow collector.
 */
object DialogsFlowHelper {

    @JvmStatic
    fun collectOAuth2Callback(
        repository: SyncStateRepository,
        dialog: Dialogs.OAuth2AccessTokenProgress
    ): Job {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        return scope.launch {
            repository.events
                .filterIsInstance<SyncEvent.OAuth2Callback>()
                .collect { event -> dialog.onOAuth2Callback(event) }
        }
    }
}
