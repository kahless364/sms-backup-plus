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
// U-020: import com.squareup.otto.Subscribe removed
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.scheduler.BackupScheduler;
import com.zegoggles.smssync.service.state.BackupState;
import com.zegoggles.smssync.service.state.SyncEvent;

import java.util.HashMap;
import java.util.Map;

import static com.zegoggles.smssync.App.LOCAL_LOGV;
import static com.zegoggles.smssync.App.TAG;
import static com.zegoggles.smssync.service.BackupType.REGULAR;
import static com.zegoggles.smssync.service.state.SyncEvent.Cancel.Origin.SYSTEM;


/**
 * Firebase JobDispatcher entry point that bridges job callbacks to SmsBackupService.
 *
 * U-013: replaced BackupJobs with BackupScheduler port.
 * U-020: Otto removed. App.register/unregister removed. @Subscribe backupStateChanged
 * replaced by StateFlow observation via SmsJobServiceFlowHelper.
 * App.post(new CancelEvent(SYSTEM)) replaced by repository.tryEmitEvent (AC-17).
 */
public class SmsJobService extends JobService {
    /** job parameters keyed by job tag / {@link BackupType} */
    private Map<String, JobParameters> jobs = new HashMap<String, JobParameters>();
    // U-020: StateFlow observation job; cancelled in onDestroy
    private kotlinx.coroutines.Job stateObservationJob = null;

    @Override
    public void onCreate() {
        super.onCreate();
        // U-020: App.register(this) removed — replaced by StateFlow observation below
        if (App.syncStateRepository() != null) {
            stateObservationJob = SmsJobServiceFlowHelper.observeBackupState(
                App.syncStateRepository(), this);
        }
    }

    @Override
    public void onDestroy() {
        // U-020: App.unregister(this) removed
        if (stateObservationJob != null) {
            stateObservationJob.cancel(null);
            stateObservationJob = null;
        }
        super.onDestroy();
    }

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
            getScheduler().scheduleIncoming();
            return false;
        } else if (shouldRun(jobParameters)) {
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

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "onStopJob(" + jobParameters + ", extras=" + jobParameters.getExtras() + ")");
        }
        // U-020: App.post(new CancelEvent(SYSTEM)) replaced by tryEmitEvent (AC-17)
        if (App.syncStateRepository() != null) {
            boolean emitted = App.syncStateRepository().tryEmitEvent(new SyncEvent.Cancel(SYSTEM));
            if (!emitted) {
                Log.w(TAG, "onStopJob: tryEmitEvent(Cancel(SYSTEM)) returned false");
            }
        }
        return false;
    }

    // U-020: @Subscribe backupStateChanged() replaced by StateFlow observation.
    // Called by SmsJobServiceFlowHelper on the Main dispatcher.
    void onBackupStateChanged(BackupState state) {
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
                getScheduler().cancelRegular();
            }
            return autoBackupEnabled;
        } else {
            return true;
        }
    }

    protected BackupScheduler getScheduler() {
        return App.getScheduler(this);
    }
}
