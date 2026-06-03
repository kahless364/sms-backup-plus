package com.zegoggles.smssync.receiver;

import android.content.Context;
import android.content.Intent;
import com.zegoggles.smssync.scheduler.BackupScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(RobolectricTestRunner.class)
public class BootReceiverTest {
    // U-013: BackupJobs mock replaced by BackupScheduler mock (getBackupJobs factory removed)
    @Mock BackupScheduler scheduler;
    BootReceiver receiver;

    @Before public void before() {
        openMocks(this);
        receiver = new BootReceiver() {
            @Override protected BackupScheduler getScheduler(Context context) {
                return scheduler;
            }
        };
    }

    @Test
    public void shouldScheduleBootupBackupAfterBootup() throws Exception {
        receiver.onReceive(RuntimeEnvironment.application, new Intent().setAction(Intent.ACTION_BOOT_COMPLETED));
        verify(scheduler, times(1)).scheduleBootup();
    }
}
