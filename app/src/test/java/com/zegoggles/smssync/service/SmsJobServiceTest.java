package com.zegoggles.smssync.service;

import android.os.Bundle;
import com.firebase.jobdispatcher.JobParameters;
import com.zegoggles.smssync.service.state.BackupState;
import com.zegoggles.smssync.service.state.SmsSyncState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.setupService;

@RunWith(RobolectricTestRunner.class)
public class SmsJobServiceTest {

    private SmsJobService smsJobService;

    @Before
    public void setUp() throws Exception {
        smsJobService = setupService(SmsJobService.class);
    }

    @Test public void testOnStartJob() {
        final JobParameters jobParameters = mock(JobParameters.class);
        when(jobParameters.getTag()).thenReturn(BackupJobs.CONTENT_TRIGGER_TAG);

        boolean moreWork = smsJobService.onStartJob(jobParameters);
        assertThat(moreWork).isFalse();
    }

    @Test public void testOnStopJob() {
        final JobParameters jobParameters = mock(JobParameters.class);
        boolean shouldRetry = smsJobService.onStopJob(jobParameters);
        assertThat(shouldRetry).isFalse();
    }

    // U-006 coverage additions for SmsJobService

    @Test public void testOnStartJob_withExtras() {
        final JobParameters jobParameters = mock(JobParameters.class);
        when(jobParameters.getTag()).thenReturn(BackupJobs.CONTENT_TRIGGER_TAG);
        when(jobParameters.getExtras()).thenReturn(new Bundle());

        boolean moreWork = smsJobService.onStartJob(jobParameters);
        assertThat(moreWork).isFalse();
    }

    @Test public void backupStateChanged_forUnknownJob_doesNotCrash() {
        // U-006: covers the backupStateChanged path when job not in map.
        // When no job is registered with the tag, backupStateChanged logs a warning
        // and does nothing (graceful no-op).
        BackupState finishedState = new BackupState(
            SmsSyncState.FINISHED_BACKUP, 0, 0, BackupType.REGULAR, null, null
        );
        // Should not throw even with no job registered
        smsJobService.backupStateChanged(finishedState);
    }

    @Test public void backupStateChanged_withNonFinishedState_isIgnored() {
        // Covers the !state.isFinished() early return path
        BackupState runningState = new BackupState(
            SmsSyncState.BACKUP, 0, 0, BackupType.REGULAR, null, null
        );
        // Should not throw — just returns early
        smsJobService.backupStateChanged(runningState);
    }
}
