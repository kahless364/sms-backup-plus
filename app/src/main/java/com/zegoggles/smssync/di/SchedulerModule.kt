package com.zegoggles.smssync.di

import com.zegoggles.smssync.scheduler.BackupScheduler
import com.zegoggles.smssync.scheduler.WorkManagerScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds BackupScheduler -> WorkManagerScheduler for the SingletonComponent.
 *
 * U-048: Converted from @Provides-new to @Binds abstract. WorkManagerScheduler now
 * has an @Inject constructor (with @ApplicationContext + Preferences; added by U-048),
 * so Hilt constructs the single @Singleton instance — no manual
 * `new WorkManagerScheduler(context, preferences)` anywhere in production code.
 *
 * App.java still holds `scheduler` as an App-level field for legacy callers that use
 * App.getScheduler(context); U-049 will migrate those callers to @Inject BackupScheduler.
 * App.java now receives its scheduler via the Hilt @Inject BackupScheduler field rather
 * than constructing it manually.
 *
 * DES-MODERNIZATION-008 §Module layout: SchedulerModule | @Binds BackupScheduler <- WorkManagerScheduler.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulerModule {

    /**
     * Binds [BackupScheduler] to [WorkManagerScheduler] as a @Singleton.
     * Hilt constructs [WorkManagerScheduler] via its @Inject constructor
     * ([@ApplicationContext] Context, Preferences) and returns the same instance
     * for every injection point in the graph.
     */
    @Binds
    @Singleton
    abstract fun bindBackupScheduler(impl: WorkManagerScheduler): BackupScheduler
}
