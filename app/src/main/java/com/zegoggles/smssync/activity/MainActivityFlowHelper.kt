package com.zegoggles.smssync.activity

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zegoggles.smssync.service.state.BackupState
import com.zegoggles.smssync.service.state.RestoreState
import com.zegoggles.smssync.service.state.SyncEvent
import kotlinx.coroutines.launch

/**
 * U-020: Kotlin helper that wires lifecycle-aware Flow collection for MainActivity.
 *
 * AC-14b: replaces the onStart App.register(this) / onStop App.unregister(this) pair
 * with a single lifecycleScope.launch { repeatOnLifecycle(STARTED) { ... } } block.
 */
object MainActivityFlowHelper {

    @JvmStatic
    fun startCollection(activity: MainActivity, viewModel: MainViewModel) {
        // State flow: separate routing for BackupState and RestoreState
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is BackupState -> activity.onBackupStateChanged(state)
                        is RestoreState -> activity.onRestoreStateChanged(state)
                        else -> { /* Initial/unknown state — no UI action */ }
                    }
                }
            }
        }

        // Events flow: route SyncEvents to the activity's onSyncEvent dispatcher
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    activity.onSyncEvent(event)
                }
            }
        }
    }
}
