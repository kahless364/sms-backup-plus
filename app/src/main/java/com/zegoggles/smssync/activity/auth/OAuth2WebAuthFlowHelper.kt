package com.zegoggles.smssync.activity.auth

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zegoggles.smssync.service.state.SyncEvent
import com.zegoggles.smssync.service.state.SyncStateRepository
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * U-020: Kotlin helper that bridges lifecycle-aware SyncEvent.BrowserAuthResult
 * collection for OAuth2WebAuthActivity.
 *
 * Replaces the Otto @Subscribe onBrowserAuthResult() pattern.
 */
object OAuth2WebAuthFlowHelper {

    @JvmStatic
    fun collectBrowserAuthResult(
        lifecycleOwner: LifecycleOwner,
        repository: SyncStateRepository,
        activity: OAuth2WebAuthActivity
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.events
                    .filterIsInstance<SyncEvent.BrowserAuthResult>()
                    .collect { event -> activity.onBrowserAuthResult(event) }
            }
        }
    }
}
