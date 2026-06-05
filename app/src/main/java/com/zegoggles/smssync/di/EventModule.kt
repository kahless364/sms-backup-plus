package com.zegoggles.smssync.di

import com.zegoggles.smssync.App
import com.zegoggles.smssync.service.state.SyncStateRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds SyncStateRepository -> FlowSyncStateRepository for the SingletonComponent.
 *
 * U-022: AC-5 — EventModule is ACTIVE because FlowSyncStateRepository (the live
 * Flow-backed implementation) was delivered by U-020 (DES-007). FlowSyncStateRepository
 * does not yet have an @Inject constructor (that's U-023), so @Provides is used rather
 * than @Binds.
 *
 * U-036 (BUG-004 fix): Previously this method constructed a second FlowSyncStateRepository
 * instance, splitting the engine's emission path from the UI's collection path. The fix
 * returns the App-level static instance (App.syncStateRepository()) so both the engine
 * (services + workers via App.syncStateRepository()) and the Hilt graph (MainViewModel)
 * share exactly one instance.
 *
 * Race-safety: App.syncStateRepository() is guaranteed non-null by the time Hilt first
 * instantiates this @Singleton — App.onCreate() assigns syncStateRepositoryInstance
 * before any Activity/MainViewModel can start. The Hilt SingletonComponent is scoped to
 * the Application and provideSyncStateRepository() is not called during App's own field
 * injection (App injects only Preferences and HiltWorkerFactory, not SyncStateRepository),
 * so the static is always initialized before this method is invoked.
 *
 * DES-MODERNIZATION-008 §Module layout: EventModule | @Binds SyncStateRepository <- DefaultSyncStateRepository.
 * (The implementation class name changed from DefaultSyncStateRepository to FlowSyncStateRepository;
 * the binding intent is the same.)
 * TODO(U-023): convert to @Binds once FlowSyncStateRepository has @Inject constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
object EventModule {

    @Provides
    @Singleton
    fun provideSyncStateRepository(): SyncStateRepository = App.syncStateRepository()
}
