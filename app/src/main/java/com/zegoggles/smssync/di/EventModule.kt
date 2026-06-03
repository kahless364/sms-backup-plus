package com.zegoggles.smssync.di

import com.zegoggles.smssync.service.state.FlowSyncStateRepository
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
 * The @Singleton scope ensures the same repository instance is shared between the engine,
 * services, activities, and workers — consistent with the App.syncStateRepository() static
 * accessor that existed before U-022. The static accessor App.syncStateRepository() continues
 * to return the manually-constructed instance during the coexistence period; the Hilt binding
 * here is the compile-time-verified seam that U-023 will activate once injection sites
 * are wired.
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
    fun provideSyncStateRepository(): SyncStateRepository = FlowSyncStateRepository()
}
