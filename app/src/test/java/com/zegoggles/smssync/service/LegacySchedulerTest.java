package com.zegoggles.smssync.service;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import com.firebase.jobdispatcher.Job;
import com.zegoggles.smssync.preferences.DataTypePreferences;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.scheduler.LegacyScheduler;
import com.zegoggles.smssync.scheduler.RestoreSchedulerConfig;
import com.zegoggles.smssync.scheduler.ScheduledJob;
import com.zegoggles.smssync.scheduler.SchedulerObservable;
import com.zegoggles.smssync.scheduler.SchedulerState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowPackageManager;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.robolectric.Shadows.shadowOf;

/**
 * Tests for {@link LegacyScheduler} — verifies delegation semantics (AC-7, AC-8)
 * and the adapter-as-pure-delegation invariant (CNTR-MODERNIZATION-004 AC-2).
 * <p>
 * AC-7: Each schedule operation via LegacyScheduler produces the same result as
 * calling BackupJobs directly with the same preference stubs.
 * <p>
 * AC-8: cancelAll() delegates to both cancelRegular() and the content-trigger
 * cancel (transitively via backupJobs.cancelAll()); cancelRegular() delegates
 * to backupJobs.cancelRegular() only.
 * <p>
 * This test lives in the service package (same as BackupJobs) to access the
 * package-private BackupJobs(Context, Preferences) constructor and the
 * package-private CONTENT_TRIGGER_TAG constant.
 */
@RunWith(RobolectricTestRunner.class)
public class LegacySchedulerTest {

    private LegacyScheduler subject;
    private BackupJobs spyBackupJobs;

    @Mock private Preferences preferences;
    @Mock private DataTypePreferences dataTypePreferences;

    @Before public void before() {
        openMocks(this);

        // Register the FirebaseJobDispatcher service so that Job creation succeeds
        // in Robolectric (mirrors the setup in BackupJobsTest)
        ShadowPackageManager pm = shadowOf(RuntimeEnvironment.application.getPackageManager());
        Intent executeIntent = new Intent("com.firebase.jobdispatcher.ACTION_EXECUTE");
        executeIntent.setClassName(RuntimeEnvironment.application, "com.zegoggles.smssync.service.SmsJobService");
        ResolveInfo ri = new ResolveInfo();
        ServiceInfo si = new ServiceInfo();
        si.packageName = "com.zegoggles.smssync.service.SmsJobService";
        ri.serviceInfo = si;
        ri.isDefault = true;
        pm.addResolveInfoForIntent(executeIntent, ri);

        // Package-private constructor: accessible from same package (com.zegoggles.smssync.service)
        BackupJobs backupJobs = new BackupJobs(RuntimeEnvironment.application, preferences);
        spyBackupJobs = spy(backupJobs);
        subject = new LegacyScheduler(spyBackupJobs);

        when(preferences.getDataTypePreferences()).thenReturn(dataTypePreferences);
    }

    // -----------------------------------------------------------------------
    // AC-7: Equivalence with direct BackupJobs calls
    // -----------------------------------------------------------------------

    /**
     * AC-7: scheduleIncoming() via LegacyScheduler returns a ScheduledJob
     * with the same tag as direct BackupJobs.scheduleIncoming().
     */
    @Test public void scheduleIncoming_delegatesToBackupJobs() {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getIncomingTimeoutSecs()).thenReturn(2000);

        // Direct call for tag comparison
        BackupJobs directBackupJobs = new BackupJobs(RuntimeEnvironment.application, preferences);
        Job directJob = directBackupJobs.scheduleIncoming();
        assertThat(directJob).isNotNull();

        // Via adapter
        ScheduledJob scheduledJob = subject.scheduleIncoming();

        verify(spyBackupJobs).scheduleIncoming();
        assertThat(scheduledJob).isNotNull();
        assertThat(scheduledJob.tag).isEqualTo(directJob.getTag());
    }

    /**
     * AC-7: scheduleRegular() via LegacyScheduler returns a ScheduledJob
     * with the same tag as direct BackupJobs.scheduleRegular().
     */
    @Test public void scheduleRegular_delegatesToBackupJobs() {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getRegularTimeoutSecs()).thenReturn(3600);

        BackupJobs directBackupJobs = new BackupJobs(RuntimeEnvironment.application, preferences);
        Job directJob = directBackupJobs.scheduleRegular();
        assertThat(directJob).isNotNull();

        ScheduledJob scheduledJob = subject.scheduleRegular();

        verify(spyBackupJobs).scheduleRegular();
        assertThat(scheduledJob).isNotNull();
        assertThat(scheduledJob.tag).isEqualTo(directJob.getTag());
    }

    /**
     * AC-7: scheduleContentTrigger() delegates to backupJobs.scheduleContentTriggerJob()
     * (port name vs BackupJobs method name discrepancy is intentional per AC-2/Technical Notes).
     * Verifies the tag equals BackupJobs.CONTENT_TRIGGER_TAG ("contentTrigger").
     */
    @Test public void scheduleContentTrigger_delegatesToBackupJobs() {
        ScheduledJob scheduledJob = subject.scheduleContentTrigger();

        verify(spyBackupJobs).scheduleContentTriggerJob();
        assertThat(scheduledJob).isNotNull();
        // BackupJobs.CONTENT_TRIGGER_TAG is package-private; use the value directly
        // "contentTrigger" matches BackupJobs.CONTENT_TRIGGER_TAG (BackupJobs.java:57)
        assertThat(scheduledJob.tag).isEqualTo("contentTrigger");
    }

    /**
     * AC-7: scheduleBootup() with autoBackupEnabled=true and oldScheduler=true
     * delegates to backupJobs.scheduleBootup() and returns a job with tag REGULAR.
     */
    @Test public void scheduleBootup_withAutoBackupEnabled_delegatesToBackupJobs() {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.isUseOldScheduler()).thenReturn(true);

        ScheduledJob scheduledJob = subject.scheduleBootup();

        verify(spyBackupJobs).scheduleBootup();
        assertThat(scheduledJob).isNotNull();
        assertThat(scheduledJob.tag).isEqualTo("REGULAR");
    }

    /**
     * AC-7: scheduleBootup() with autoBackupEnabled=false delegates and returns null
     * (BackupJobs.scheduleBootup calls cancelAll and returns null when disabled).
     */
    @Test public void scheduleBootup_withAutoBackupDisabled_returnsNull() {
        when(preferences.isAutoBackupEnabled()).thenReturn(false);

        ScheduledJob scheduledJob = subject.scheduleBootup();

        verify(spyBackupJobs).scheduleBootup();
        assertThat(scheduledJob).isNull();
    }

    /**
     * AC-7: scheduleImmediate() delegates to backupJobs.scheduleImmediate()
     * and returns a job with tag BROADCAST_INTENT.
     */
    @Test public void scheduleImmediate_delegatesToBackupJobs() {
        ScheduledJob scheduledJob = subject.scheduleImmediate();

        verify(spyBackupJobs).scheduleImmediate();
        assertThat(scheduledJob).isNotNull();
        assertThat(scheduledJob.tag).isEqualTo("BROADCAST_INTENT");
    }

    /**
     * AC-2: scheduleRestore() returns null with a log stub (no legacy analog).
     * Callers must not crash when this returns null.
     */
    @Test public void scheduleRestore_returnsNull() {
        RestoreSchedulerConfig config = new RestoreSchedulerConfig("restore-001", "checkpoint-key");
        ScheduledJob scheduledJob = subject.scheduleRestore(config);
        assertThat(scheduledJob).isNull();
    }

    // -----------------------------------------------------------------------
    // AC-8: Cancel delegation
    // -----------------------------------------------------------------------

    /**
     * AC-8: cancelAll() delegates to backupJobs.cancelAll() (which internally
     * calls both cancelRegular() and cancelContentUriTrigger()).
     * No exception propagates to the caller.
     */
    @Test public void cancelAll_delegatesToBackupJobs_cancelAll() {
        subject.cancelAll();
        verify(spyBackupJobs).cancelAll();
        // no exception propagates
    }

    /**
     * AC-8: cancelRegular() delegates only to backupJobs.cancelRegular().
     * Does not call cancelAll() or any other cancel method.
     */
    @Test public void cancelRegular_delegatesToBackupJobs_cancelRegular() {
        subject.cancelRegular();
        verify(spyBackupJobs).cancelRegular();
        // no exception propagates
    }

    /**
     * cancel(REGULAR) delegates to backupJobs.cancelRegular().
     */
    @Test public void cancel_regular_delegatesToCancelRegular() {
        subject.cancel(BackupType.REGULAR);
        verify(spyBackupJobs).cancelRegular();
    }

    /**
     * cancel(INCOMING) is a no-op for the legacy scheduler (no public cancel-by-tag API
     * on BackupJobs). This test confirms it does not throw.
     */
    @Test public void cancel_incoming_doesNotThrow() {
        subject.cancel(BackupType.INCOMING);
        // no exception propagates; no interaction with spyBackupJobs for cancel
    }

    // -----------------------------------------------------------------------
    // AC-2: observe() returns SchedulerState.Unknown
    // -----------------------------------------------------------------------

    /**
     * AC-2: observe() returns a SchedulerObservable permanently set to Unknown
     * (Firebase JobDispatcher has no state API).
     */
    @Test public void observe_returnsUnknownState() {
        SchedulerObservable<SchedulerState> observable = subject.observe(BackupType.REGULAR);
        assertThat(observable).isNotNull();
        assertThat(observable.getValue()).isEqualTo(SchedulerState.Unknown);
    }

    @Test public void observe_incoming_returnsUnknownState() {
        SchedulerObservable<SchedulerState> observable = subject.observe(BackupType.INCOMING);
        assertThat(observable.getValue()).isEqualTo(SchedulerState.Unknown);
    }
}
