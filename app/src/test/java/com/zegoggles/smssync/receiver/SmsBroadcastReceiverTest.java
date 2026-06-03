package com.zegoggles.smssync.receiver;

import android.content.Context;
import android.content.Intent;
import com.zegoggles.smssync.preferences.AuthPreferences;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.scheduler.BackupScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(RobolectricTestRunner.class)
public class SmsBroadcastReceiverTest {
    Context context;
    // U-013: BackupJobs mock replaced by BackupScheduler mock (getBackupJobs factory removed)
    @Mock BackupScheduler scheduler;
    @Mock Preferences preferences;
    @Mock AuthPreferences authPreferences;
    SmsBroadcastReceiver receiver;

    @Before public void before() {
        openMocks(this);
        context = RuntimeEnvironment.application;
        receiver = new SmsBroadcastReceiver() {
            @Override protected BackupScheduler getScheduler(Context context) {
                return scheduler;
            }

            @Override protected Preferences getPreferences(Context context) {
                return preferences;
            }

            @Override protected AuthPreferences getAuthPreferences(Context context) {
                return authPreferences;
            }
        };
    }

    @Test public void shouldScheduleIncomingBackupAfterIncomingMessage() throws Exception {
        mockScheduled();
        receiver.onReceive(context, new Intent().setAction("android.provider.Telephony.SMS_RECEIVED"));
        verify(scheduler, times(1)).scheduleIncoming();
    }

    @Test public void shouldNotScheduleIfAutoBackupIsDisabled() throws Exception {
        mockScheduled();
        when(preferences.isAutoBackupEnabled()).thenReturn(false);
        receiver.onReceive(context, new Intent().setAction("android.provider.Telephony.SMS_RECEIVED"));
        verifyNoInteractions(scheduler);
    }

    @Test public void shouldNotScheduleIfLoginInformationIsNotSet() throws Exception {
        mockScheduled();
        when(authPreferences.isLoginInformationSet()).thenReturn(false);
        receiver.onReceive(context, new Intent().setAction("android.provider.Telephony.SMS_RECEIVED"));
        verifyNoInteractions(scheduler);
    }

    @Test public void shouldNotScheduleIfFirstBackupHasNotBeenRun() throws Exception {
        mockScheduled();
        when(preferences.isFirstBackup()).thenReturn(true);
        receiver.onReceive(context, new Intent().setAction("android.provider.Telephony.SMS_RECEIVED"));
        verifyNoInteractions(scheduler);
    }

    private void mockScheduled() {
        when(authPreferences.isLoginInformationSet()).thenReturn(true);
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.isFirstBackup()).thenReturn(false);
        when(preferences.isUseOldScheduler()).thenReturn(true);
    }
}
