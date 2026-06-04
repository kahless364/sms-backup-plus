package com.zegoggles.smssync.di

import com.zegoggles.smssync.service.RestoreCheckpointStore
import com.zegoggles.smssync.service.RestoreInsertInterceptor
import com.zegoggles.smssync.service.SharedPreferencesCheckpointStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides [RestoreCheckpointStore] and [RestoreInsertInterceptor] bindings.
 *
 * U-024: Activates the checkpoint store binding required by [RestoreWorker]'s
 * [@HiltWorker + @AssistedInject] constructor.
 *
 * - [RestoreCheckpointStore] -> [SharedPreferencesCheckpointStore] (@Singleton — one store
 *   per application process, consistent with the former manual singleton construction in
 *   [RestoreWorker.RestoreWorkerFactory]).
 * - [RestoreInsertInterceptor] -> [RestoreInsertInterceptor.NoOp] (production no-op; tests
 *   use [RestoreWorker.TestableRestoreWorkerFactory] which bypasses this binding and injects
 *   a [RestoreInsertInterceptor.CrashAfterK] directly via TestListenableWorkerBuilder).
 *
 * DES-MODERNIZATION-008 §Module layout: CheckpointModule | @Binds RestoreCheckpointStore
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CheckpointModule {

    /**
     * Binds [SharedPreferencesCheckpointStore] as the production [RestoreCheckpointStore].
     *
     * @Singleton scope: one checkpoint store per app process, consistent with how
     * [RestoreWorker.RestoreWorkerFactory] previously instantiated it once per worker creation.
     */
    @Binds
    @Singleton
    abstract fun bindRestoreCheckpointStore(
        impl: SharedPreferencesCheckpointStore
    ): RestoreCheckpointStore

    companion object {
        /**
         * Provides the production [RestoreInsertInterceptor] — always [RestoreInsertInterceptor.NoOp].
         *
         * Fault-injection tests bypass this binding by using [RestoreWorker.TestableRestoreWorkerFactory]
         * with [TestListenableWorkerBuilder.setWorkerFactory], which directly constructs
         * [RestoreWorker] with a [RestoreInsertInterceptor.CrashAfterK] instance.
         */
        @Provides
        fun provideRestoreInsertInterceptor(): RestoreInsertInterceptor =
            RestoreInsertInterceptor.NoOp
    }
}
