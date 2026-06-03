package com.zegoggles.smssync.di

import android.content.Context
import com.zegoggles.smssync.preferences.Preferences
import com.zegoggles.smssync.scheduler.BackupScheduler
import com.zegoggles.smssync.scheduler.WorkManagerScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds BackupScheduler -> WorkManagerScheduler for the SingletonComponent.
 *
 * U-022: AC-5 — SchedulerModule is ACTIVE because the WorkManagerScheduler adapter
 * exists (DES-005 / U-013/U-014/U-017 delivered it). WorkManagerScheduler does not yet
 * have an @Inject constructor (that's U-023), so @Provides is used here rather than @Binds.
 * Once U-023 adds @Inject to WorkManagerScheduler, this can be converted to @Binds.
 *
 * The @Singleton scope ensures the same WorkManagerScheduler is injected everywhere —
 * consistent with App.scheduler being a single field throughout the application lifecycle.
 *
 * DES-MODERNIZATION-008 §Module layout: SchedulerModule | @Binds BackupScheduler <- WorkManagerScheduler.
 * TODO(U-023): convert to @Binds once WorkManagerScheduler has @Inject constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
object SchedulerModule {

    @Provides
    @Singleton
    fun provideBackupScheduler(
        @ApplicationContext context: Context,
        preferences: Preferences
    ): BackupScheduler = WorkManagerScheduler(context, preferences)
}
