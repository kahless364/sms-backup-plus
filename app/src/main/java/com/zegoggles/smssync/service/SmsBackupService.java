/* Copyright (c) 2009 Christoph Studer <chstuder@gmail.com>
 * Copyright (c) 2010 Jan Berkel <jan.berkel@gmail.com>
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Observer;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import android.util.Log;
// U-020: import com.squareup.otto.Produce removed (AC-8)
// U-020: import com.squareup.otto.Subscribe removed (AC-8)
// U-026: k-9 MessagingException import removed; replaced by MailException (AC-5)
// U-031: BackupTask import removed (AC-7) — class deleted
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.R;
import com.zegoggles.smssync.activity.MainActivity;
import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.mail.transport.MailException;
import com.zegoggles.smssync.scheduler.BackupScheduler;
import com.zegoggles.smssync.scheduler.ScheduledJob;
import com.zegoggles.smssync.service.exception.BackupDisabledException;
import com.zegoggles.smssync.service.exception.MissingPermissionException;
import com.zegoggles.smssync.service.exception.RequiresLoginException;
import com.zegoggles.smssync.service.state.BackupState;
import com.zegoggles.smssync.service.state.SmsSyncState;
import dagger.hilt.android.AndroidEntryPoint;
import kotlinx.coroutines.Job;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.R.drawable.stat_sys_warning;
import static com.zegoggles.smssync.App.CHANNEL_ID;
import static com.zegoggles.smssync.App.LOCAL_LOGV;
import static com.zegoggles.smssync.App.TAG;
import static com.zegoggles.smssync.activity.AppPermission.formatMissingPermissionDetails;
import static com.zegoggles.smssync.service.BackupType.MANUAL;
import static com.zegoggles.smssync.service.BackupType.REGULAR;
import static com.zegoggles.smssync.service.BackupType.SKIP;
import static com.zegoggles.smssync.service.state.SmsSyncState.ERROR;
import static com.zegoggles.smssync.service.state.SmsSyncState.FINISHED_BACKUP;
import static com.zegoggles.smssync.service.state.SmsSyncState.INITIAL;

/**
 * Service that performs the actual SMS/call-log backup.
 * <p>
 * U-013 CS-5/CS-7: replaced the private {@code getBackupJobs()} factory and direct
 * {@code BackupJobs} usage with the injected {@link BackupScheduler} port.
 * The {@code getBackupJobs()} factory method is removed; {@code getScheduler()} provides
 * the same test-override surface.
 * <p>
 * The {@code scheduleNextBackup()} method previously used the returned Firebase {@code Job}
 * object to extract the trigger window start for logging. After migration, the port's
 * {@link ScheduledJob} carries a tag and description string; the log message now uses the
 * description rather than parsing a {@code JobTrigger.ExecutionWindowTrigger}.
 *
 * <p>U-032: @AndroidEntryPoint applied (deferred from U-023). Hilt member injection
 * fires in Hilt_SmsBackupService.onCreate() — ServiceBase.preferences and
 * ServiceBase.authPreferences are populated before SmsBackupService.onCreate() runs.
 * The Robolectric service tests are migrated off anonymous-subclass pattern (AC-9/AC-10).
 *
 * <p>U-026: k-9 MessagingException import removed (AC-5).
 * All uses of MessagingException in catch/throws declarations are replaced with
 * {@link MailException} (app-owned).
 *
 * <p>U-031: AsyncTask execution path removed.
 * {@code backup()} now calls {@code getScheduler().scheduleManual(backupType)} instead of
 * constructing/executing {@code BackupTask}. A {@link WorkInfo} observer bridge is registered
 * immediately after enqueue to drive the existing {@code backupStateChanged()} foreground/stop
 * driver from worker progress/terminal state (DES-MODERNIZATION-012 §Integration Design, Option b1).
 * The {@code getBackupTask()} factory and {@code BackupTask} class are deleted.
 * Cancel rewired: {@code SyncEvent.Cancel(USER)} from the repository reaches
 * {@code WorkManager.cancelUniqueWork()} via {@link WorkManagerCancelCollector} (R-4 mitigation).
 */
@AndroidEntryPoint
public class SmsBackupService extends ServiceBase {
    private static final int BACKUP_ID = 1;
    private static final int NOTIFICATION_ID_WARNING = 1;

    // U-020: static service field deleted (AC-8a). State is read via syncStateRepository().
    @NonNull private BackupState state = new BackupState();

    // U-031: WorkInfo observer reference — held so it can be removed on terminal state.
    @Nullable private Observer<List<WorkInfo>> workInfoObserver;

    // U-031: Cancel collector job — collects SyncEvent.Cancel from repository and routes to WM.
    @Nullable private Job cancelCollectorJob;

    @Override @NonNull
    public BackupState getState() {
        return state;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (LOCAL_LOGV) Log.v(TAG, "SmsBackupService#onCreate");
        // U-020: service = this; deleted (AC-8a)
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (LOCAL_LOGV) Log.v(TAG, "SmsBackupService#onDestroy(state=" + getState() + ")");
        // U-020: service = null; deleted (AC-8a)
        tearDownObserverAndCollector(null);
    }

    @Override
    protected void handleIntent(final Intent intent) {
        if (intent == null) return; // NB: should not happen with START_NOT_STICKY
        final BackupType backupType = BackupType.fromIntent(intent);
        if (LOCAL_LOGV) {
            Log.v(TAG, "handleIntent(" + intent +
                    ", " + (intent.getExtras() == null ? "null" : intent.getExtras().keySet()) +
                    ", " + intent.getAction() +
                    ", type="+backupType+")");
        }

        appLog(R.string.app_log_backup_requested, getString(backupType.resId));
        // U-020: SmsRestoreService.isServiceIdle() replaced by reading state from repository (AC-9).
        boolean restoreIdle = !App.syncStateRepository().getState().getValue().isRunning()
            || !(App.syncStateRepository().getState().getValue() instanceof com.zegoggles.smssync.service.state.RestoreState);
        if (!isWorking() && restoreIdle) {
            backup(backupType);
        } else {
            appLog(R.string.app_log_skip_backup_already_running);
        }
    }

    private void backup(BackupType backupType) {
        getNotifier().cancel(NOTIFICATION_ID_WARNING);

        try {
            // set initial state
            state = new BackupState(INITIAL, 0, 0, backupType, null, null);
            EnumSet<DataType> enabledTypes = getEnabledBackupTypes();
            checkPermissions(enabledTypes);
            if (backupType != SKIP) {
                checkCredentials();
                // U-017: legacyCheckConnectivity() removed — WorkManagerScheduler enforces
                // network constraints via Constraints; no manual pre-flight check needed.
            }
            appLog(R.string.app_log_start_backup, backupType);

            // U-031 AC-2/AC-3: Dispatch to WorkManager via scheduleManual.
            // Replaces getBackupTask().execute(getBackupConfig(backupType, enabledTypes, getMailTransport())).
            // CNTR-MODERNIZATION-004 v2 Validation Rule 8: scheduleManual carries MANUAL/SKIP type.
            ScheduledJob job = getScheduler().scheduleManual(backupType);
            if (job != null) {
                Log.d(TAG, "SmsBackupService.backup: enqueued via scheduleManual, uniqueWork=" + job.tag);
                // U-031 Step 2a (AC-1): register WorkInfo observer bridge.
                // Drives backupStateChanged() from worker progress/terminal WorkInfo state.
                // DES-MODERNIZATION-012 §Integration Design Option (b1).
                registerBackupWorkInfoObserver(backupType.name(), backupType);
                // U-031 R-4 (AC-5): register cancel collector to route SyncEvent.Cancel to WM.
                registerCancelCollector(backupType.name());
            } else {
                Log.w(TAG, "SmsBackupService.backup: scheduleManual returned null for " + backupType);
                moveToState(state.transition(ERROR, new MailException("scheduleManual returned null")));
            }
        // U-031: MailException catch removed — getBackupConfig()/getMailTransport() deleted;
        // no caller in backup() throws MailException anymore. Error-result states are
        // surfaced via moveToState(state.transition(ERROR, ...)) in the specific catch branches.
        } catch (RequiresLoginException e) {
            appLog(R.string.app_log_missing_credentials);
            moveToState(state.transition(ERROR, e));
        } catch (BackupDisabledException e) {
            moveToState(state.transition(FINISHED_BACKUP, e));
        } catch (MissingPermissionException e) {
            moveToState(state.transition(ERROR, e));
        }
    }

    /**
     * U-031 Step 2a (AC-1): Registers a WorkInfo observer that bridges worker setProgress state
     * into the existing backupStateChanged() foreground/stop driver.
     * <p>
     * DES-MODERNIZATION-012 §Integration Design Option (b1): the service observes the worker
     * via WorkManager.getWorkInfosForUniqueWorkLiveData() and re-invokes backupStateChanged()
     * to drive startForeground/stopForeground/stopSelf. This keeps the worker byte-unchanged
     * and reuses the verbatim foreground/teardown/notification logic.
     *
     * @param uniqueWorkName the unique-work name used by scheduleManual ("MANUAL" or "SKIP")
     * @param backupType     the BackupType enum value (for constructing BackupState)
     */
    private void registerBackupWorkInfoObserver(final String uniqueWorkName,
                                                final BackupType backupType) {
        // Remove any existing observer first (defensive)
        if (workInfoObserver != null) {
            WorkManager.getInstance(getApplicationContext())
                .getWorkInfosForUniqueWorkLiveData(uniqueWorkName)
                .removeObserver(workInfoObserver);
            workInfoObserver = null;
        }

        workInfoObserver = new Observer<List<WorkInfo>>() {
            @Override
            public void onChanged(List<WorkInfo> workInfoList) {
                if (workInfoList == null || workInfoList.isEmpty()) return;
                WorkInfo workInfo = workInfoList.get(0);
                if (workInfo == null) return;
                BackupState newState = mapWorkInfoToBackupState(workInfo, backupType);
                if (newState != null) {
                    backupStateChanged(newState);
                }
                // On terminal state, tear down observer (backupStateChanged's !isRunning branch
                // handles stopForeground/stopSelf; we tear down here to prevent further callbacks)
                if (workInfo.getState().isFinished()) {
                    tearDownObserverAndCollector(uniqueWorkName);
                }
            }
        };

        // observeForever requires main thread; Service.handleIntent() is called on main thread.
        WorkManager.getInstance(getApplicationContext())
            .getWorkInfosForUniqueWorkLiveData(uniqueWorkName)
            .observeForever(workInfoObserver);

        Log.d(TAG, "SmsBackupService: registered WorkInfo observer for " + uniqueWorkName);
    }

    /**
     * Maps a WorkInfo to a BackupState by reading PROGRESS_KEY_STATE from progress data.
     * Terminal WorkInfo.State values are mapped to SmsSyncState terminal states.
     *
     * @return a BackupState to drive backupStateChanged(), or null if no change needed
     */
    @Nullable
    BackupState mapWorkInfoToBackupState(WorkInfo workInfo, BackupType backupType) {
        WorkInfo.State wmState = workInfo.getState();

        if (wmState == WorkInfo.State.SUCCEEDED) {
            return new BackupState(SmsSyncState.FINISHED_BACKUP, 0, 0, backupType, null, null);
        } else if (wmState == WorkInfo.State.FAILED) {
            return new BackupState(SmsSyncState.ERROR, 0, 0, backupType, null,
                new MailException("BackupWorker failed"));
        } else if (wmState == WorkInfo.State.CANCELLED) {
            return new BackupState(SmsSyncState.CANCELED_BACKUP, 0, 0, backupType, null, null);
        } else if (wmState == WorkInfo.State.RUNNING) {
            String progressState = workInfo.getProgress().getString(BackupWorker.PROGRESS_KEY_STATE);
            if (progressState == null) return null;
            SmsSyncState smsSyncState = mapProgressStateToSmsSyncState(progressState);
            if (smsSyncState == null) return null;
            int backedUp = workInfo.getProgress().getInt(BackupWorker.PROGRESS_KEY_BACKED_UP, 0);
            int toSync = workInfo.getProgress().getInt(BackupWorker.PROGRESS_KEY_ITEMS_TO_SYNC, 0);
            String dataTypeName = workInfo.getProgress().getString(BackupWorker.PROGRESS_KEY_DATA_TYPE);
            DataType dataType = null;
            if (dataTypeName != null && !dataTypeName.isEmpty()) {
                try { dataType = DataType.valueOf(dataTypeName); } catch (Exception ignored) {}
            }
            return new BackupState(smsSyncState, backedUp, toSync, backupType, dataType, null);
        }
        return null;
    }

    @Nullable
    private SmsSyncState mapProgressStateToSmsSyncState(String progressState) {
        switch (progressState) {
            case BackupWorker.STATE_LOGIN:    return SmsSyncState.LOGIN;
            case BackupWorker.STATE_CALC:     return SmsSyncState.CALC;
            case BackupWorker.STATE_BACKUP:   return SmsSyncState.BACKUP;
            case BackupWorker.STATE_FINISHED: return SmsSyncState.FINISHED_BACKUP;
            case BackupWorker.STATE_CANCELED: return SmsSyncState.CANCELED_BACKUP;
            default: return null;
        }
    }

    /**
     * U-031 R-4 (AC-5): Registers a cancel collector that routes SyncEvent.Cancel(USER)
     * from the SyncStateRepository to WorkManager.cancelUniqueWork(uniqueWorkName).
     * The worker's cooperative ensureActive() handles the interrupt at the next suspension point.
     * The AC-1 WorkInfo observer drives stopForeground/stopSelf on the CANCELLED terminal state.
     */
    private void registerCancelCollector(final String uniqueWorkName) {
        if (cancelCollectorJob != null) {
            cancelCollectorJob.cancel(null);
            cancelCollectorJob = null;
        }
        cancelCollectorJob = WorkManagerCancelCollector.collect(
            getApplicationContext(), App.syncStateRepository(), uniqueWorkName);
        Log.d(TAG, "SmsBackupService: registered cancel collector for " + uniqueWorkName);
    }

    private void tearDownObserverAndCollector(@Nullable String uniqueWorkName) {
        if (workInfoObserver != null && uniqueWorkName != null) {
            WorkManager.getInstance(getApplicationContext())
                .getWorkInfosForUniqueWorkLiveData(uniqueWorkName)
                .removeObserver(workInfoObserver);
            workInfoObserver = null;
        } else if (workInfoObserver != null) {
            // uniqueWorkName not available (onDestroy path) — just null the reference
            workInfoObserver = null;
        }
        if (cancelCollectorJob != null) {
            cancelCollectorJob.cancel(null);
            cancelCollectorJob = null;
        }
    }

    private void checkPermissions(EnumSet<DataType> enabledTypes) throws MissingPermissionException {
        Set<String> missing = new HashSet<String>();
        for (DataType dataType : enabledTypes) {
            missing.addAll(dataType.checkPermissions(this));
        }
        if (!missing.isEmpty()) {
            throw new MissingPermissionException(missing);
        }
    }

    private EnumSet<DataType> getEnabledBackupTypes() throws BackupDisabledException {
        EnumSet<DataType> dataTypes = getPreferences().getDataTypePreferences().enabled();
        if (dataTypes.isEmpty()) {
            throw new BackupDisabledException();
        }
        return dataTypes;
    }

    private void checkCredentials() throws RequiresLoginException {
        if (!getAuthPreferences().isLoginInformationSet()) {
            throw new RequiresLoginException();
        }
    }

    // U-017: legacyCheckConnectivity() deleted — WorkManagerScheduler enforces
    // network constraints via Constraints; no manual pre-flight check needed.

    // U-031: getBackupTask() factory deleted (AC-7).
    // SmsBackupService now dispatches via getScheduler().scheduleManual(backupType).

    private void moveToState(BackupState state) {
        backupStateChanged(state);
        // U-020: App.syncStateRepository().emitState(state) now writes to MutableStateFlow directly
        // (no Otto delegation). IC-3: the repository is the sole engine→UI channel.
        App.syncStateRepository().emitState(state);
    }

    @Override
    protected boolean isBackgroundTask() {
        return state.backupType.isBackground();
    }

    // U-020: @Produce produceLastState() deleted (AC-8).
    // StateFlow.value provides sticky last-state semantics for late collectors (AC-4).

    // U-020: @Subscribe annotation removed — backupStateChanged() is called directly from
    // the WorkInfo observer (U-031) and moveToState(). No Otto registration needed.
    public void backupStateChanged(BackupState state) {
        if (this.state == state) return;

        this.state = state;
        if (this.state.isInitialState()) return;

        if (state.isError()) {
            handleErrorState(state);
        }

        if (state.isRunning()) {
            if (state.backupType == MANUAL) {
                notifyAboutBackup(state);
            }
        } else {
            appLogDebug(state.toString());
            appLog(state.isCanceled() ? R.string.app_log_backup_canceled : R.string.app_log_backup_finished);
            scheduleNextBackup(state);
            tearDownObserverAndCollector(null);
            stopForeground(true);
            stopSelf();
        }
    }

    private void handleErrorState(BackupState state) {
        if (state.isAuthException()) {
            appLog(R.string.app_log_backup_failed_authentication, state.getDetailedErrorMessage(getResources()));

            if (shouldNotifyUser(state)) {
                notifyUser(NOTIFICATION_ID_WARNING, notificationBuilder(stat_sys_warning,
                    getString(R.string.notification_auth_failure),
                    getString(getAuthPreferences().useXOAuth() ? R.string.status_auth_failure_details_xoauth : R.string.status_auth_failure_details_plain)));
            }
        } else if (state.isConnectivityError()) {
            appLog(R.string.app_log_backup_failed_connectivity, state.getDetailedErrorMessage(getResources()));
        } else if (state.isPermissionException()) {
            if (state.backupType != MANUAL) {
                Bundle extras = new Bundle();
                extras.putStringArray(MainActivity.EXTRA_PERMISSIONS, state.getMissingPermissions());

                notifyUser(NOTIFICATION_ID_WARNING, notificationBuilder(R.drawable.ic_notification,
                    getString(R.string.notification_missing_permission),
                    formatMissingPermissionDetails(getResources(), state.getMissingPermissions()))
                    .setContentIntent(getPendingIntent(extras)));
            }
        } else {
            appLog(R.string.app_log_backup_failed_general_error, state.getDetailedErrorMessage(getResources()));

            if (shouldNotifyUser(state)) {
                notifyUser(NOTIFICATION_ID_WARNING, notificationBuilder(stat_sys_warning,
                    getString(R.string.notification_general_error),
                    state.getErrorMessage(getResources())));
            }
        }
    }

    private boolean shouldNotifyUser(BackupState state) {
        return state.backupType == MANUAL ||
               (getPreferences().isNotificationEnabled() && !state.isConnectivityError());
    }

    private void notifyAboutBackup(BackupState state) {
        NotificationCompat.Builder builder = createNotification(R.string.status_backup);
        notification = builder.setContentTitle(getString(R.string.status_backup))
                .setContentText(state.getNotificationLabel(getResources()))
                .setContentIntent(getPendingIntent(null))
                .build();
        startForeground(BACKUP_ID, notification);
    }

    /**
     * Schedules the next regular backup after one completes.
     * <p>
     * U-013 CS-7: migrated from {@code getBackupJobs().scheduleRegular()} (Firebase
     * Job return type) to {@code getScheduler().scheduleRegular()} (port-level
     * {@link ScheduledJob} return type). The log message uses the port's description
     * field instead of parsing a {@code JobTrigger.ExecutionWindowTrigger}; behavior
     * is functionally identical (a next-sync time is logged when available).
     * <p>
     * U-017: isUseOldScheduler() guard removed — WorkManagerScheduler persists periodic
     * work automatically. scheduleRegular() is still called here to ensure the periodic
     * work request is re-queued after a regular backup completes.
     */
    private void scheduleNextBackup(BackupState state) {
        if (state.backupType == REGULAR) {
            final ScheduledJob nextSync = getScheduler().scheduleRegular();
            if (nextSync != null) {
                appLog(R.string.app_log_scheduled_next_sync, nextSync.description);
            } else {
                appLog(R.string.app_log_no_next_sync);
            }
        } // WorkManager persists periodic work across restarts
    }

    void notifyUser(int notificationId, NotificationCompat.Builder builder) {
        getNotifier().notify(notificationId, builder.build());
    }

    @SuppressWarnings("deprecation")
    private NotificationCompat.Builder notificationBuilder(int icon, String title, String text) {
        return new NotificationCompat.Builder(this)
            .setSmallIcon(icon)
            .setChannelId(CHANNEL_ID)
            .setWhen(System.currentTimeMillis())
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentText(text)
            .setTicker(getString(R.string.app_name))
            .setContentTitle(title)
            .setContentIntent(getPendingIntent(null));
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

    // U-020: isServiceWorking() deleted (AC-8b).
    // Callers now read App.syncStateRepository().getState().getValue().isRunning() directly.

    public BackupState transition(SmsSyncState newState, Exception e) {
        return state.transition(newState, e);
    }
}
