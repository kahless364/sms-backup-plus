package com.zegoggles.smssync.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zegoggles.smssync.service.state.SyncStateRepository

/**
 * U-020: Manual ViewModelProvider.Factory for MainViewModel.
 *
 * AC-18: constructed with the SyncStateRepository manual singleton from App.getRepository().
 * TODO U-022/MU-007: replace with @HiltViewModel
 */
class MainViewModelFactory(
    private val repository: SyncStateRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
