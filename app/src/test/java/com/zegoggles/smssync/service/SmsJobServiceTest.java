package com.zegoggles.smssync.service;

import android.os.Bundle;
import com.firebase.jobdispatcher.JobParameters;
import com.zegoggles.smssync.scheduler.BackupScheduler;
import com.zegoggles.smssync.scheduler.ScheduledJob;
import com.zegoggles.smssync.scheduler.SchedulerObservable;
import com.zegoggles.smssync.scheduler.SchedulerState;
import com.zegoggles.smssync.service.state.BackupState;
import com.zegoggles.smssync.service.state.SmsSyncState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    /**
     * AC-4 / CNTR-MODERNIZATION-004 INV-4 two-stage debounce:
     * When onStartJob is called with the content-trigger tag, it must call
     * scheduler.scheduleIncoming() and no other schedule operation.
     * This verifies the port routing is correct; the underlying behavior
     * (delayed follow-up rather than immediate backup) is preserved in BackupJobs.
     */
    @Test public void contentTrigger_schedulesIncomingFollowUp_notDirectBackup() {
        final BackupScheduler mockScheduler = mock(BackupScheduler.class);

        // Create SmsJobService subclass that injects the mock scheduler
        SmsJobService service = new SmsJobService() {
            @Override
            protected BackupScheduler getScheduler() {
                return mockScheduler;
            }
        };

        final JobParameters jobParameters = mock(JobParameters.class);
        when(jobParameters.getTag()).thenReturn(BackupJobs.CONTENT_TRIGGER_TAG);

        boolean moreWork = service.onStartJob(jobParameters);

        // content-trigger returns false (no more work — just enqueued follow-up)
        assertThat(moreWork).isFalse();

        // Must call scheduleIncoming — the two-stage debounce follow-up
        verify(mockScheduler).scheduleIncoming();

        // Must NOT call any other schedule operation (no direct backup on raw change)
        verifyNoMoreInteractions(mockScheduler);
    }
}
