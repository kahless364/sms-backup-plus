package com.zegoggles.smssync.di

import com.zegoggles.smssync.service.state.FlowSyncStateRepository
import com.zegoggles.smssync.service.state.SyncStateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds SyncStateRepository -> FlowSyncStateRepository for the SingletonComponent.
 *
 * U-048: Converted from @Provides-new to @Binds abstract. FlowSyncStateRepository now
 * has an @Inject constructor (added by U-048), so Hilt constructs the single @Singleton
 * instance — no manual `new FlowSyncStateRepository()` anywhere in production code.
 *
 * BUG-004 (permanent fix): The previous bridge (App.syncStateRepository() call in
 * provideSyncStateRepository()) is removed. The single instance is now owned entirely
 * by the Hilt @Singleton scope. App.java sets its static accessor from the
 * @Inject-populated field in onCreate(), so all legacy call sites continue to receive
 * the same Hilt-managed instance.
 *
 * DES-MODERNIZATION-008 §Module layout: EventModule | @Binds SyncStateRepository <- FlowSyncStateRepository.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EventModule {

    /**
     * Binds [SyncStateRepository] to [FlowSyncStateRepository] as a @Singleton.
     * Hilt constructs the [FlowSyncStateRepository] via its @Inject constructor
     * and returns the same instance for every injection point in the graph.
     */
    @Binds
    @Singleton
    abstract fun bindSyncStateRepository(impl: FlowSyncStateRepository): SyncStateRepository
}
