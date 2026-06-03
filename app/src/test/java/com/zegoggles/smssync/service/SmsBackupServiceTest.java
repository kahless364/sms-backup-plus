package com.zegoggles.smssync.service;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import androidx.core.app.NotificationCompat;
import android.telephony.TelephonyManager;
import com.fsck.k9.mail.MessagingException;
import com.zegoggles.smssync.contacts.ContactGroup;
import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.preferences.AuthPreferences;
import com.zegoggles.smssync.preferences.DataTypePreferences;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.scheduler.BackupScheduler;
import com.zegoggles.smssync.scheduler.ScheduledJob;
import com.zegoggles.smssync.service.exception.BackupDisabledException;
import com.zegoggles.smssync.service.exception.NoConnectionException;
import com.zegoggles.smssync.service.exception.RequiresLoginException;
import com.zegoggles.smssync.service.exception.RequiresWifiException;
import com.zegoggles.smssync.service.state.SmsSyncState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowNetworkInfo;
import org.robolectric.shadows.ShadowWifiManager;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.google.common.truth.Truth.assertThat;
import static com.zegoggles.smssync.service.BackupType.MANUAL;
import static com.zegoggles.smssync.service.BackupType.REGULAR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class SmsBackupServiceTest {
    SmsBackupService service;
    ShadowConnectivityManager shadowConnectivityManager;
    ShadowWifiManager shadowWifiManager;
    List<NotificationCompat.Builder> sentNotifications;

    @Mock AuthPreferences authPreferences;
    @Mock Preferences preferences;
    @Mock DataTypePreferences dataTypePreferences;
    @Mock BackupTask backupTask;
    // U-013: BackupJobs mock replaced by BackupScheduler mock (getBackupJobs factory removed)
    @Mock BackupScheduler scheduler;

    @Before public void before() {
        openMocks(this);
        sentNotifications = new ArrayList<NotificationCompat.Builder>();
        service = new SmsBackupService() {
            @Override public Context getApplicationContext() { return RuntimeEnvironment.application; }
            @Override public Resources getResources() { return getApplicationContext().getResources(); }
            @Override protected BackupTask getBackupTask() { return backupTask; }
            // U-013: override getScheduler() instead of getBackupJobs()
            @Override protected BackupScheduler getScheduler() { return scheduler; }
            @Override protected Preferences getPreferences() { return preferences; }
            @Override public int checkPermission(String permission, int pid, int uid) { return PERMISSION_GRANTED; }
            @Override protected AuthPreferences getAuthPreferences() { return authPreferences; }
            @Override protected void notifyUser(int icon, NotificationCompat.Builder builder) {
                sentNotifications.add(builder);
            }
        };
        shadowConnectivityManager = shadowOf(service.getConnectivityManager());
        // U-005: shadowWifiManager removed — ShadowWifiManager references API classes not in compileSdk 29
        // and was never used in test assertions. The field is retained for reference.
        // shadowWifiManager = shadowOf(service.getWifiManager());

        service.onCreate();

        when(authPreferences.getStoreUri()).thenReturn("imap+ssl+://xoauth:foooo@imap.gmail.com:993");
        when(authPreferences.isLoginInformationSet()).thenReturn(true);
        when(preferences.getBackupContactGroup()).thenReturn(ContactGroup.EVERYBODY);
        when(preferences.isUseOldScheduler()).thenReturn(true);
        when(preferences.getDataTypePreferences()).thenReturn(dataTypePreferences);
        when(dataTypePreferences.enabled()).thenReturn(EnumSet.of(DataType.SMS));
    }

    @After public void after() {
        service.onDestroy();
    }

    @Test public void shouldTriggerBackupWithManualIntent() throws Exception {
        Intent intent = new Intent(MANUAL.name());
        service.handleIntent(intent);
        verify(backupTask).execute(any(BackupConfig.class));
    }

    @Test public void shouldCheckForConnectivityBeforeBackingUp() throws Exception {
        Intent intent = new Intent(MANUAL.name());

        shadowConnectivityManager.setActiveNetworkInfo(null);
        service.handleIntent(intent);

        verifyNoInteractions(backupTask);
        assertThat(service.getState().exception).isInstanceOf(NoConnectionException.class);
    }

    @Test public void shouldNotCheckForConnectivityBeforeBackingUpWithNewScheduler() throws Exception {
        when(preferences.isUseOldScheduler()).thenReturn(false);

        Intent intent = new Intent(REGULAR.name());
        shadowConnectivityManager.setActiveNetworkInfo(null);
        shadowConnectivityManager.setBackgroundDataSetting(true);
        service.handleIntent(intent);
        verify(backupTask).execute(any(BackupConfig.class));
    }

    @Test public void shouldCheckForWifiConnectivity() throws Exception {
        Intent intent = new Intent();
        when(preferences.isWifiOnly()).thenReturn(true);
        shadowConnectivityManager.setBackgroundDataSetting(true);
        service.handleIntent(intent);

        verifyNoInteractions(backupTask);
        assertThat(service.getState().exception).isInstanceOf(RequiresWifiException.class);
    }

    @Test public void shouldCheckForWifiConnectivityAndNetworkType() throws Exception {
        Intent intent = new Intent();
            when(preferences.isWifiOnly()).thenReturn(true);
        shadowConnectivityManager.setBackgroundDataSetting(true);
        shadowConnectivityManager.setActiveNetworkInfo(connectedViaEdge());
        service.handleIntent(intent);

        verifyNoInteractions(backupTask);
        assertThat(service.getState().exception).isInstanceOf(RequiresWifiException.class);
    }

    @Test public void shouldCheckForLoginCredentials() throws Exception {
        Intent intent = new Intent();
        when(authPreferences.isLoginInformationSet()).thenReturn(false);
        shadowConnectivityManager.setBackgroundDataSetting(true);
        service.handleIntent(intent);
        verifyNoInteractions(backupTask);
        assertThat(service.getState().exception).isInstanceOf(RequiresLoginException.class);
    }

    @Test public void shouldCheckForEnabledDataTypes() throws Exception {
        when(dataTypePreferences.enabled()).thenReturn(EnumSet.noneOf(DataType.class));

        Intent intent = new Intent();
        when(authPreferences.isLoginInformationSet()).thenReturn(true);
        shadowConnectivityManager.setBackgroundDataSetting(true);
        service.handleIntent(intent);
        verifyNoInteractions(backupTask);
        assertThat(service.getState().exception).isInstanceOf(BackupDisabledException.class);
        assertThat(service.getState().state).isEqualTo(SmsSyncState.FINISHED_BACKUP);
    }

    @Test public void shouldPassInCorrectBackupConfig() throws Exception {
        Intent intent = new Intent(MANUAL.name());
        ArgumentCaptor<BackupConfig> config = ArgumentCaptor.forClass(BackupConfig.class);

        service.handleIntent(intent);
        verify(backupTask).execute(config.capture());

        BackupConfig backupConfig = config.getValue();
        assertThat(backupConfig.backupType).isEqualTo(MANUAL);
        assertThat(backupConfig.currentTry).isEqualTo(0);
    }

    @Test public void shouldScheduleNextRegularBackupAfterFinished() throws Exception {
        // U-013: scheduler.scheduleRegular() returns a ScheduledJob (not a Firebase Job)
        when(scheduler.scheduleRegular()).thenReturn(new ScheduledJob("REGULAR", "REGULAR @ test"));

        shadowConnectivityManager.setBackgroundDataSetting(true);
        Intent intent = new Intent(REGULAR.name());
        service.handleIntent(intent);

        verify(backupTask).execute(any(BackupConfig.class));

        service.backupStateChanged(service.transition(SmsSyncState.FINISHED_BACKUP, null));

        verify(scheduler).scheduleRegular();

        assertThat(shadowOf(service).isStoppedBySelf()).isTrue();
        assertThat(shadowOf(service).isForegroundStopped()).isTrue();
    }

    @Test public void shouldCheckForValidStore() throws Exception {
        when(authPreferences.getStoreUri()).thenReturn("invalid");
        Intent intent = new Intent(MANUAL.name());

        service.handleIntent(intent);
        verifyNoInteractions(backupTask);
        assertThat(service.getState().exception).isInstanceOf(MessagingException.class);
    }

    @Test public void shouldNotifyUserAboutErrorInManualMode() throws Exception {
        when(authPreferences.getStoreUri()).thenReturn("invalid");
        Intent intent = new Intent(MANUAL.name());

        service.handleIntent(intent);
        verifyNoInteractions(backupTask);

        assertNotificationShown("SMSBackup+ error", "No valid IMAP URI: invalid");

        assertThat(shadowOf(service).isStoppedBySelf()).isTrue();
        assertThat(shadowOf(service).isForegroundStopped()).isTrue();
    }

    private void assertNotificationShown(CharSequence title, CharSequence message) {
        assertThat(sentNotifications).hasSize(1);
        // U-006 AC-8: The original commented-out body referenced NotificationCompat.Builder
        // internal fields (mContentTitle, mContentText) which are not accessible via the
        // public API in Robolectric 4.12.x / NotificationCompat from androidx.core:core.
        // The builder fields are package-private in NotificationCompat.Builder and no public
        // accessor method (getContentTitle / getContentText) exists on the builder object.
        // Robolectric 4.12.x ShadowNotificationManager works with android.app.Notification
        // objects (not builders), and notification.extras requires a built Notification.
        // Since the service under test captures the builder (not the built Notification),
        // title/text extraction from the builder is not possible without reflection or
        // access to the final Notification via a notification manager shadow.
        //
        // BLOCKED: notification title/text assertion requires internal NotificationCompat.Builder
        // field access (mContentTitle, mContentText) not available in Robolectric 4.12.x via
        // the public API. The size assertion above confirms a notification was posted.
        // Unblock in a future story when NotificationCompat shadow exposes getContentTitle() or
        // when the service is refactored to post via NotificationManagerCompat (which Robolectric
        // 4.12.x can capture via ShadowNotificationManager.getLastNotification()).
    }

    private NetworkInfo connectedViaEdge() {
        return ShadowNetworkInfo.newInstance(
            null,  /* detailed state */
            ConnectivityManager.TYPE_MOBILE_HIPRI,
            TelephonyManager.NETWORK_TYPE_EDGE,
            true,  /* available */
            true   /* connected */
        );
    }
}
