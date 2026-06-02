package com.zegoggles.smssync.service;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobTrigger;
import com.firebase.jobdispatcher.ObservedUri;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.zegoggles.smssync.preferences.DataTypePreferences;
import com.zegoggles.smssync.preferences.Preferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowPackageManager;

import static com.firebase.jobdispatcher.ObservedUri.Flags.FLAG_NOTIFY_FOR_DESCENDANTS;
import static com.google.common.truth.Truth.assertThat;
import static com.zegoggles.smssync.Consts.CALLLOG_PROVIDER;
import static com.zegoggles.smssync.Consts.SMS_PROVIDER;
import static com.zegoggles.smssync.mail.DataType.CALLLOG;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class BackupJobsTest {
    private BackupJobs subject;

    @Mock private Preferences preferences;
    @Mock private DataTypePreferences dataTypePreferences;

    @Before public void before() {
        openMocks(this);
        ShadowPackageManager pm = shadowOf(RuntimeEnvironment.application.getPackageManager());

        Intent executeIntent = new Intent("com.firebase.jobdispatcher.ACTION_EXECUTE");
        executeIntent.setClassName(RuntimeEnvironment.application, "com.zegoggles.smssync.service.SmsJobService");

        ResolveInfo ri = new ResolveInfo();
        ServiceInfo si = new ServiceInfo();
        si.packageName = "com.zegoggles.smssync.service.SmsJobService";
        ri.serviceInfo = si;
        ri.isDefault = true;

        pm.addResolveInfoForIntent(executeIntent, ri);
        subject = new BackupJobs(RuntimeEnvironment.application, preferences);
        when(preferences.getDataTypePreferences()).thenReturn(dataTypePreferences);
    }

    @Test public void shouldScheduleImmediate() throws Exception {
        Job job = subject.scheduleImmediate();
        verifyJobScheduled(job, -1, "BROADCAST_INTENT");
    }

    @Test public void shouldScheduleRegular() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getRegularTimeoutSecs()).thenReturn(2000);
        Job job = subject.scheduleRegular();
        verifyJobScheduled(job, 2000, "REGULAR");
    }

    @Test public void shouldScheduleContentUriTriggerForSMS() throws Exception {
        Job job = subject.scheduleContentTriggerJob();
        assertThat(job.getTrigger()).isInstanceOf(JobTrigger.ContentUriTrigger.class);

        JobTrigger.ContentUriTrigger contentUriTrigger = (JobTrigger.ContentUriTrigger) job.getTrigger();
        assertThat(contentUriTrigger.getUris()).containsExactly(new ObservedUri(SMS_PROVIDER, FLAG_NOTIFY_FOR_DESCENDANTS));
    }

    @Test public void shouldScheduleContentUriTriggerForCallLogIfEnabled() throws Exception {
        when(preferences.isCallLogBackupAfterCallEnabled()).thenReturn(true);
        when(dataTypePreferences.isBackupEnabled(CALLLOG)).thenReturn(true);

        Job job = subject.scheduleContentTriggerJob();
        assertThat(job.getTrigger()).isInstanceOf(JobTrigger.ContentUriTrigger.class);

        JobTrigger.ContentUriTrigger contentUriTrigger = (JobTrigger.ContentUriTrigger) job.getTrigger();
        assertThat(contentUriTrigger.getUris()).containsExactly(
            new ObservedUri(SMS_PROVIDER, FLAG_NOTIFY_FOR_DESCENDANTS),
            new ObservedUri(CALLLOG_PROVIDER, FLAG_NOTIFY_FOR_DESCENDANTS)
        );
    }

    @Test public void shouldScheduleRegularJobAfterBootForOldScheduler() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.isUseOldScheduler()).thenReturn(true);
        Job job = subject.scheduleBootup();
        verifyJobScheduled(job, 60, "REGULAR");
    }

    @Test public void shouldScheduleNothingAfterBootForNewScheduler() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.isUseOldScheduler()).thenReturn(false);
        Job job = subject.scheduleBootup();
        assertThat(job).isNull();
    }

    @Test public void shouldCancelAllJobsAfterBootIfAutoBackupDisabled() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(false);
        Job job = subject.scheduleBootup();
        assertThat(job).isNull();
    }

    @Test public void shouldScheduleIncoming() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getIncomingTimeoutSecs()).thenReturn(2000);
        Job job = subject.scheduleIncoming();
        verifyJobScheduled(job, 2000, "INCOMING");
    }

    @Test public void shouldNotScheduleRegularBackupIfAutoBackupIsDisabled() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(false);
        assertThat(subject.scheduleRegular()).isEqualTo(null);
    }

    @Test public void shouldNotScheduleIncomingBackupIfAutoBackupIsDisabled() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(false);
        assertThat(subject.scheduleIncoming()).isEqualTo(null);
    }

    // -------------------------------------------------------------------------
    // U-006 characterization tests — pins BackupJobs retry/constraint contract
    // before WorkManager migration (DES-005 / U-014). These four tests are the
    // behavioral specification that the WorkManager adapter must preserve.
    // -------------------------------------------------------------------------

    /**
     * AC-4: Pins the retry base (initial) backoff = 30 seconds.
     * Source: BackupJobs.java:205 firebaseJobDispatcher.newRetryStrategy(RETRY_POLICY_EXPONENTIAL, 30, 300)
     * RetryStrategy exposes getInitialBackoff() on firebase-jobdispatcher 0.8.6 (verified via javap).
     * DES-005 WorkManager equivalent: setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).
     */
    @Test public void defaultRetryStrategy_hasBaseIntervalOf30Seconds() {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getRegularTimeoutSecs()).thenReturn(2000);

        Job job = subject.scheduleRegular();

        assertThat(job).isNotNull();
        RetryStrategy retryStrategy = job.getRetryStrategy();
        assertThat(retryStrategy).isNotNull();
        // Pins BackupJobs.java:205 first arg to newRetryStrategy: base interval = 30 seconds.
        assertThat(retryStrategy.getInitialBackoff()).isEqualTo(30);
    }

    /**
     * AC-5: Pins the retry maximum backoff = 300 seconds.
     * Source: BackupJobs.java:205 firebaseJobDispatcher.newRetryStrategy(RETRY_POLICY_EXPONENTIAL, 30, 300)
     * RetryStrategy exposes getMaximumBackoff() on firebase-jobdispatcher 0.8.6 (verified via javap).
     * DES-005 WorkManager equivalent: setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 300, TimeUnit.SECONDS).
     * AC-4 and AC-5 are independent @Test methods so each can fail independently.
     */
    @Test public void defaultRetryStrategy_hasMaxIntervalOf300Seconds() {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getRegularTimeoutSecs()).thenReturn(2000);

        Job job = subject.scheduleRegular();

        assertThat(job).isNotNull();
        RetryStrategy retryStrategy = job.getRetryStrategy();
        assertThat(retryStrategy).isNotNull();
        // Pins BackupJobs.java:205 second arg to newRetryStrategy: max interval = 300 seconds.
        assertThat(retryStrategy.getMaximumBackoff()).isEqualTo(300);
    }

    /**
     * AC-6: Pins the wifi-only (isWifiOnly=true) constraint → ON_UNMETERED_NETWORK.
     * Source: BackupJobs.java:199 isWifiOnly() ? ON_UNMETERED_NETWORK : ON_ANY_NETWORK
     * DES-005 WorkManager equivalent: Constraints.setRequiredNetworkType(NetworkType.UNMETERED).
     * These tests are additive and do NOT modify the existing verifyJobScheduled helper.
     */
    @Test public void scheduleRegular_wifiOnly_constraintIsUnmetered() {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getRegularTimeoutSecs()).thenReturn(2000);
        // Stub wifi-only = true to exercise the ON_UNMETERED_NETWORK branch.
        when(preferences.isWifiOnly()).thenReturn(true);

        Job job = subject.scheduleRegular();

        assertThat(job).isNotNull();
        // On wifi-only, the constraint must be ON_UNMETERED_NETWORK (not ON_ANY_NETWORK).
        assertThat(job.getConstraints()).asList().contains(Constraint.ON_UNMETERED_NETWORK);
        assertThat(job.getConstraints()).asList().doesNotContain(Constraint.ON_ANY_NETWORK);
    }

    /**
     * AC-7: Pins the any-network (isWifiOnly=false) constraint → ON_ANY_NETWORK.
     * Source: BackupJobs.java:199 isWifiOnly() ? ON_UNMETERED_NETWORK : ON_ANY_NETWORK
     * DES-005 WorkManager equivalent: Constraints.setRequiredNetworkType(NetworkType.CONNECTED).
     */
    @Test public void scheduleRegular_anyNetwork_constraintIsAny() {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getRegularTimeoutSecs()).thenReturn(2000);
        // Stub wifi-only = false to exercise the ON_ANY_NETWORK branch.
        when(preferences.isWifiOnly()).thenReturn(false);

        Job job = subject.scheduleRegular();

        assertThat(job).isNotNull();
        // Without wifi-only restriction, the constraint must be ON_ANY_NETWORK (not ON_UNMETERED_NETWORK).
        assertThat(job.getConstraints()).asList().contains(Constraint.ON_ANY_NETWORK);
        assertThat(job.getConstraints()).asList().doesNotContain(Constraint.ON_UNMETERED_NETWORK);
    }

    private void verifyJobScheduled(Job job, int scheduled, String expectedType) {
        assertThat(job).isNotNull();
        if (scheduled <= 0) {
            assertThat(job.getTrigger()).isInstanceOf(JobTrigger.ImmediateTrigger.class);
        } else {
            assertThat(job.getTrigger()).isInstanceOf(JobTrigger.ExecutionWindowTrigger.class);
            JobTrigger.ExecutionWindowTrigger trigger = (JobTrigger.ExecutionWindowTrigger) job.getTrigger();
            JobTrigger.ExecutionWindowTrigger testTrigger = Trigger.executionWindow(scheduled, scheduled);
            assertThat(trigger.getWindowEnd()).isEqualTo(testTrigger.getWindowEnd());
            assertThat(trigger.getWindowStart()).isEqualTo(testTrigger.getWindowStart());
        }
        assertThat(job.getTag()).isEqualTo(expectedType);

        if ("BROADCAST_INTENT".equals(expectedType)) {
            assertThat(job.getConstraints()).isEmpty();
        } else {
            assertThat(job.getConstraints()).asList().contains(Constraint.ON_ANY_NETWORK);
        }
    }
}
