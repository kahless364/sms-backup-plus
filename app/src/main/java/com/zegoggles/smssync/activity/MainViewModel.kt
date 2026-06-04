package com.zegoggles.smssync.activity

import androidx.lifecycle.ViewModel
import com.zegoggles.smssync.service.state.SyncEvent
import com.zegoggles.smssync.service.state.SyncState
import com.zegoggles.smssync.service.state.SyncStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

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
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: SyncStateRepository
) : ViewModel() {

    /** Current sync state (sticky — new collectors immediately see the last value). */
    val state: StateFlow<SyncState> = repository.state

    /** One-shot sync events (replay=0 — late collectors do NOT see prior events). */
    val events: SharedFlow<SyncEvent> = repository.events

    /**
     * Emits a one-shot event to the repository (non-suspending).
     * Callers must not silently discard the Boolean return (AC-17).
     */
    fun tryEmitEvent(event: SyncEvent): Boolean = repository.tryEmitEvent(event)
}
