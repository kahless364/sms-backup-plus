package com.zegoggles.smssync.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Provides coroutine dispatchers as @Singleton bindings in the SingletonComponent.
 *
 * U-022: AC-7 — IoDispatcher qualifier + @IoDispatcher CoroutineDispatcher = Dispatchers.IO.
 * This module is always active (no adapter dependency). Downstream consumers (OAuth2Client,
 * TokenRefresher in U-023+) can request @IoDispatcher CoroutineDispatcher.
 *
 * DES-MODERNIZATION-008 §Module layout: DispatcherModule | SingletonComponent |
 * @Provides @IoDispatcher CoroutineDispatcher = Dispatchers.IO.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    /**
     * Provides Dispatchers.IO scoped as a @Singleton — the same instance is injected
     * everywhere @IoDispatcher CoroutineDispatcher is requested.
     *
     * @IoDispatcher qualifies this provider so future @MainDispatcher or @DefaultDispatcher
     * providers can coexist without ambiguity.
     */
    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
