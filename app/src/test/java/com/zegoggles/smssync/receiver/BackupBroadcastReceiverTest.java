package com.zegoggles.smssync.receiver;

import android.content.Context;
import android.content.Intent;
import androidx.preference.PreferenceManager;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.scheduler.BackupScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * U-068 (DES-MODERNIZATION-014 D8, increment S1-I2): the public BACKUP broadcast's opt-in guard, both branches
 * (CNTR-MODERNIZATION-005, frozen). The production receiver is used unchanged; the scheduler is supplied
 * through its protected getScheduler(Context) seam. The preference is the real Preferences over the default
 * shared preferences, so the persisted key is part of what is pinned.
 */
@RunWith(RobolectricTestRunner.class)
public class BackupBroadcastReceiverTest {
    /** Persisted key of the third-party integration setting (Preferences.Keys.THIRD_PARTY_INTEGRATION). */
    private static final String THIRD_PARTY_INTEGRATION_KEY = "third_party_integration";

    @Mock BackupScheduler scheduler;
    private Context context;
    private BackupBroadcastReceiver receiver;

    @Before public void before() {
        openMocks(this);
        context = RuntimeEnvironment.getApplication();
        receiver = new BackupBroadcastReceiver() {
            @Override protected BackupScheduler getScheduler(Context context) {
                return scheduler;
            }
        };
    }

    @Test public void onReceive_integrationOn_schedulesImmediateOnce() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(THIRD_PARTY_INTEGRATION_KEY, true)
                .commit();
        assertThat(new Preferences(context).isAllow3rdPartyIntegration()).isTrue();

        receiver.onReceive(context, new Intent(BackupBroadcastReceiver.BACKUP_ACTION));

        verify(scheduler, times(1)).scheduleImmediate();
        verifyNoMoreInteractions(scheduler);
    }

    @Test public void onReceive_integrationOff_neverSchedules() {
        assertThat(new Preferences(context).isAllow3rdPartyIntegration()).isFalse();

        receiver.onReceive(context, new Intent(BackupBroadcastReceiver.BACKUP_ACTION));

        verifyNoInteractions(scheduler);
    }
}
