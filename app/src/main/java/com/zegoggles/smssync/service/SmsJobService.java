/* Copyright (c) 2017 Jan Berkel <jan.berkel@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zegoggles.smssync.service;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.squareup.otto.Subscribe;
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.scheduler.BackupScheduler;
import com.zegoggles.smssync.service.state.BackupState;

import java.util.HashMap;
import java.util.Map;

import static com.zegoggles.smssync.App.LOCAL_LOGV;
import static com.zegoggles.smssync.App.TAG;
import static com.zegoggles.smssync.service.BackupType.REGULAR;
import static com.zegoggles.smssync.service.CancelEvent.Origin.SYSTEM;


/**
 * Firebase JobDispatcher entry point that bridges job callbacks to
 * {@link SmsBackupService}.
 * <p>
 * U-013 CS-4/IC-3: replaced the private {@code getBackupJobs()} factory and all
 * direct {@code BackupJobs} usage with the injected {@link BackupScheduler} port.
 * The {@code getScheduler()} protected factory method provides the same
 * test-override surface that {@code getBackupJobs()} previously offered.
 * <p>
 * IC-3 compliance: {@code SmsJobService} holds no direct reference to
 * {@code BackupJobs} after this story. Both {@code shouldRun()}'s
 * {@code cancelRegular()} call and the content-trigger
 * {@code scheduleIncoming()} call are routed through the port.
 */
public class SmsJobService extends JobService {
    /** job parameters keyed by job tag / {@link BackupType} */
    private Map<String, JobParameters> jobs = new HashMap<String, JobParameters>();

    @Override
    public void onCreate() {
        super.onCreate();
        App.register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        App.unregister(this);
    }

    /**
     * The entry point to your Job. Implementations should offload work to another thread of execution
     * as soon as possible because this runs on the main thread. If work was offloaded, call {@link
     * JobService#jobFinished(JobParameters, boolean)} to notify the scheduling service that the work
     * is completed.
     *
     * <p>If a job with the same service and tag was rescheduled during execution {@link
     * #onStopJob(JobParameters)} will be called and the wakelock will be released. Please
     * make sure that all reschedule requests happen at the end of the job.
     *
     * @return {@code true} if there is more work remaining in the worker thread, {@code false} if the
     * job was completed.
     */
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        final Bundle extras = jobParameters.getExtras();
        if (LOCAL_LOGV) {
            Log.v(TAG, "onStartJob(" + jobParameters + ", extras=" + extras + ")");
        }

        if (wasTriggeredByContentUri(jobParameters)) {
            if (LOCAL_LOGV) {
                Log.v(TAG, "scheduling follow-up job for content triggered job "+jobParameters);
            }
            // AC-4 / INV-4 two-stage debounce: content-URI trigger enqueues a
            // delayed incoming backup rather than backing up on the raw content change.
            getScheduler().scheduleIncoming();
            return false;
        } else if (shouldRun(jobParameters)) {
            // Since API level 26, an app in background cannot start a background service,
            // so just instantiate service manually
            // https://developer.android.com/about/versions/oreo/background.html#services
            SmsBackupService service = new SmsBackupService();
            service.attachBaseContext(this);
            service.handleIntent(new Intent(jobParameters.getTag()).putExtras(extras));

            jobs.put(jobParameters.getTag(), jobParameters);
            return true;
        } else {
            Log.d(TAG, "skipping run");
            return false;
        }
    }

    /**
     * Called when the scheduling engine has decided to interrupt the execution of a running job, most
     * likely because the runtime constraints associated with the job are no longer satisfied. The job
     * must stop execution.
     *
     * @return true if the job should be retried
     */
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "onStopJob(" + jobParameters + ", extras=" + jobParameters.getExtras() + ")");
        }
        App.post(new CancelEvent(SYSTEM));
        return false;
    }

    @Subscribe
    public void backupStateChanged(BackupState state) {
        if (!state.isFinished()) {
            return;
        }

        final JobParameters jobParameters = jobs.remove(state.backupType.name());
        if (jobParameters != null) {
            final boolean needsReschedule = state.isError() && !state.isPermissionException();
            if (LOCAL_LOGV) {
                Log.v(TAG, "jobFinished(" + jobParameters + ", isError=" + state.isError() + ", needsReschedule="+needsReschedule+")");
            }
            jobFinished(jobParameters, needsReschedule);
        } else {
            Log.w(TAG, "unknown job for state "+state);
        }
    }

    private boolean wasTriggeredByContentUri(JobParameters jobParameters) {
        return BackupJobs.CONTENT_TRIGGER_TAG.equals(jobParameters.getTag());
    }

    private boolean shouldRun(JobParameters jobParameters) {
        if (BackupType.fromName(jobParameters.getTag()) == REGULAR) {
            final Preferences prefs = new Preferences(this);
            final boolean autoBackupEnabled = prefs.isAutoBackupEnabled();
            if (!autoBackupEnabled) {
                // was disabled in meantime, cancel via port (IC-3)
                getScheduler().cancelRegular();
            }
            return autoBackupEnabled;
        } else {
            return true;
        }
    }

    /**
     * Returns the application-scoped {@link BackupScheduler}.
     * <p>
     * Protected to allow test subclasses to inject a mock scheduler (replaces the
     * old {@code getBackupJobs()} factory). Will be replaced by Hilt
     * {@code @Inject} field injection in U-022.
     */
    protected BackupScheduler getScheduler() {
        return App.getScheduler(this);
    }
}
