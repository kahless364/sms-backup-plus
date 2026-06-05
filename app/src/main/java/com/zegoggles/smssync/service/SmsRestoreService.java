package com.zegoggles.smssync.service;

import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import android.util.Log;
// U-020: import com.squareup.otto.Produce removed (AC-9)
// U-020: import com.squareup.otto.Subscribe removed (AC-9)
// U-026 AC-6: k-9 MessagingException import removed; replaced by MailException
// U-026 AC-6: k-9 BinaryTempFileBody import removed;
//             setTempDirectory() call moved behind K9MailTransport adapter constructor
// U-031 AC-7: RestoreTask import removed — class deleted
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.R;
import com.zegoggles.smssync.mail.transport.MailException;
import com.zegoggles.smssync.scheduler.BackupScheduler;
import com.zegoggles.smssync.scheduler.RestoreSchedulerConfig;
import com.zegoggles.smssync.scheduler.ScheduledJob;
import com.zegoggles.smssync.service.exception.SmsProviderNotWritableException;
import com.zegoggles.smssync.service.state.RestoreState;
import com.zegoggles.smssync.service.state.SmsSyncState;
import dagger.hilt.android.AndroidEntryPoint;
import kotlinx.coroutines.Job;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import static com.zegoggles.smssync.App.CHANNEL_ID;
import static com.zegoggles.smssync.App.LOCAL_LOGV;
import static com.zegoggles.smssync.App.TAG;
import static com.zegoggles.smssync.compat.SmsReceiver.isSmsBackupDefaultSmsApp;
import static com.zegoggles.smssync.mail.DataType.CALLLOG;
import static com.zegoggles.smssync.mail.DataType.SMS;
import static com.zegoggles.smssync.service.state.SmsSyncState.ERROR;

/**
 * U-032: @AndroidEntryPoint applied (deferred from U-023). Hilt member injection fires
 * in Hilt_SmsRestoreService.onCreate() — ServiceBase.preferences and
 * ServiceBase.authPreferences are populated before SmsRestoreService.onCreate() runs.
 * Robolectric service tests migrated off anonymous-subclass/setupService pattern (AC-9/AC-10).
 *
 * Service that performs the actual SMS/call-log restore from IMAP.
 *
 * <p>U-026 AC-6: k-9 MessagingException and BinaryTempFileBody imports removed.
 * The BinaryTempFileBody.setTempDirectory(getCacheDir()) call has been
 * moved behind the K9MailTransport adapter constructor (called in
 * {@code ServiceBase.getMailTransport()}) per CNTR-MODERNIZATION-007 §Notes.
 * All uses of MessagingException are replaced by {@link MailException}.
 *
 * <p>U-031: AsyncTask execution path removed.
 * {@code handleIntent()} now calls
 * {@code getScheduler().scheduleRestore(new RestoreSchedulerConfig(...))} instead of
 * constructing/executing {@code RestoreTask}. A {@link WorkInfo} observer bridge is registered
 * immediately after enqueue to drive the existing {@code restoreStateChanged()} foreground/stop
 * driver from worker progress/terminal state (DES-MODERNIZATION-012 §Integration Design, Option b1).
 * {@code FULL_WAKE_LOCK} is acquired by this service around the enqueue + observe window and
 * released in the terminal state handler (Option b per design recommendation).
 * Cancel rewired: {@code SyncEvent.Cancel(USER)} from the repository reaches
 * {@code WorkManager.cancelUniqueWork()} via {@link WorkManagerCancelCollector} (R-4 mitigation).
 * {@code getRestoreTask()} factory and {@code RestoreTask} class are deleted.
 */
@AndroidEntryPoint
public class SmsRestoreService extends ServiceBase {
    private static final int RESTORE_ID = 2;

    // U-020: static service field deleted (AC-9a).
    @NonNull private RestoreState state = new RestoreState();

    // U-031: WorkInfo observer reference — held so it can be removed on terminal state.
    @Nullable private Observer<List<WorkInfo>> workInfoObserver;

    // U-037 (BUG-005): Cache the LiveData alongside the observer so teardown can always
    // call removeObserver() regardless of whether uniqueWorkName is available.
    @Nullable private androidx.lifecycle.LiveData<List<WorkInfo>> workInfoLiveData;

    // U-031: Cancel collector job — collects SyncEvent.Cancel from repository and routes to WM.
    @Nullable private Job cancelCollectorJob;

    @Override @NonNull
    public RestoreState getState() {
        return state;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        asyncClearCache();
        // U-026 AC-6: BinaryTempFileBody.setTempDirectory(getCacheDir()) removed from here.
        // The call is now made inside K9MailTransport's constructor (getMailTransport() seam),
        // ensuring it occurs before the first restore body fetch — behavioral contract preserved.
        // BinaryTempFileBody type no longer appears in this file.
        // U-020: service = this; deleted (AC-9a)
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (LOCAL_LOGV) Log.v(TAG, "SmsRestoreService#onDestroy(state"+getState()+")");
        // U-031: tear down observer and cancel collector on destroy (cleanup safety net)
        // U-037 (BUG-005): no-arg teardown calls removeObserver() via cached LiveData.
        tearDownObserverAndCollector();
        // U-020: service = null; deleted (AC-9a)
    }

    /**
     * Android KitKat and above require SMS Backup+ to be the default SMS application in order to
     * write to the SMS Provider.
     * <p>
     * Protected to allow test subclasses to override without requiring PackageManager initialization.
     */
    protected boolean canWriteToSmsProvider() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ||
               isSmsBackupDefaultSmsApp(this);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void handleIntent(final Intent intent) {
        if (isWorking()) return;

        final boolean restoreCallLog = getPreferences().getDataTypePreferences().isRestoreEnabled(CALLLOG);
        final boolean restoreSms     = getPreferences().getDataTypePreferences().isRestoreEnabled(SMS);

        if (restoreSms && !canWriteToSmsProvider()) {
            postError(new SmsProviderNotWritableException());
            return;
        }

        // U-031 AC-9 (Option b): Acquire FULL_WAKE_LOCK before enqueue so the screen stays on
        // for the duration of the restore + the window where the user must re-select the SMS app.
        // Released in restoreStateChanged() terminal branch alongside stopSelf().
        acquireLocks();

        // U-031 AC-2: Dispatch to WorkManager via scheduleRestore.
        // Replaces getRestoreTask().execute(config) (RestoreTask deleted).
        // RestoreWorker.RESTORE_WORK_NAME = "RESTORE" is used as both the unique-work name
        // and the durable checkpoint key, matching the existing RestoreWorker inputData contract.
        // U-031: getMailTransport()/RestoreConfig construction removed — RestoreWorker builds
        // its own transport. No checked exception can be thrown in this path anymore.
        ScheduledJob job =
            getScheduler().scheduleRestore(
                new RestoreSchedulerConfig(
                    RestoreWorker.RESTORE_WORK_NAME,
                    RestoreWorker.RESTORE_WORK_NAME));

        if (job != null) {
            Log.d(TAG, "SmsRestoreService.handleIntent: enqueued via scheduleRestore, uniqueWork=" + job.tag);
            // U-031 Step 2a (AC-1): register WorkInfo observer bridge.
            // Drives restoreStateChanged() from worker progress/terminal WorkInfo state.
            // DES-MODERNIZATION-012 §Integration Design Option (b1).
            registerRestoreWorkInfoObserver(RestoreWorker.RESTORE_WORK_NAME);
            // U-031 R-4 (AC-5): register cancel collector to route SyncEvent.Cancel to WM.
            registerCancelCollector(RestoreWorker.RESTORE_WORK_NAME);
        } else {
            Log.w(TAG, "SmsRestoreService.handleIntent: scheduleRestore returned null");
            releaseLocks();
            postError(new MailException("scheduleRestore returned null"));
        }
    }

    /**
     * U-031 Step 2a (AC-1): Registers a WorkInfo observer that bridges worker setProgress state
     * into the existing restoreStateChanged() foreground/stop driver.
     * <p>
     * DES-MODERNIZATION-012 §Integration Design Option (b1): the service observes the worker
     * via WorkManager.getWorkInfosForUniqueWorkLiveData() and re-invokes restoreStateChanged()
     * to drive startForeground/stopForeground/stopSelf. This keeps the worker byte-unchanged
     * and reuses the verbatim foreground/teardown/notification logic.
     *
     * @param uniqueWorkName the unique-work name used by scheduleRestore ("RESTORE")
     */
    private void registerRestoreWorkInfoObserver(final String uniqueWorkName) {
        // Remove any existing observer first (defensive, using cached LiveData — U-037)
        if (workInfoObserver != null && workInfoLiveData != null) {
            workInfoLiveData.removeObserver(workInfoObserver);
            workInfoObserver = null;
            workInfoLiveData = null;
        }

        workInfoObserver = new Observer<List<WorkInfo>>() {
            @Override
            public void onChanged(List<WorkInfo> workInfoList) {
                if (workInfoList == null || workInfoList.isEmpty()) return;
                WorkInfo workInfo = workInfoList.get(0);
                if (workInfo == null) return;
                RestoreState newState = mapWorkInfoToRestoreState(workInfo);
                if (newState != null) {
                    restoreStateChanged(newState);
                }
                // On terminal state, tear down observer (restoreStateChanged's !isRunning branch
                // handles stopForeground/stopSelf; we tear down here to prevent further callbacks)
                if (workInfo.getState().isFinished()) {
                    tearDownObserverAndCollector();
                }
            }
        };

        // U-037 (BUG-005): Cache the LiveData reference so teardown can always call
        // removeObserver() regardless of whether uniqueWorkName is available at teardown time.
        // observeForever requires main thread; Service.handleIntent() is called on main thread.
        workInfoLiveData = WorkManager.getInstance(getApplicationContext())
            .getWorkInfosForUniqueWorkLiveData(uniqueWorkName);
        workInfoLiveData.observeForever(workInfoObserver);

        Log.d(TAG, "SmsRestoreService: registered WorkInfo observer for " + uniqueWorkName);
    }

    /**
     * Maps a WorkInfo to a RestoreState by reading PROGRESS_KEY_STATE from progress data.
     * Terminal WorkInfo.State values are mapped to SmsSyncState terminal states.
     *
     * @return a RestoreState to drive restoreStateChanged(), or null if no change needed
     */
    @Nullable
    RestoreState mapWorkInfoToRestoreState(WorkInfo workInfo) {
        WorkInfo.State wmState = workInfo.getState();

        if (wmState == WorkInfo.State.SUCCEEDED) {
            return new RestoreState(SmsSyncState.FINISHED_RESTORE, 0, 0, 0, 0, null, null);
        } else if (wmState == WorkInfo.State.FAILED) {
            return new RestoreState(SmsSyncState.ERROR, 0, 0, 0, 0, null,
                new MailException("RestoreWorker failed"));
        } else if (wmState == WorkInfo.State.CANCELLED) {
            return new RestoreState(SmsSyncState.CANCELED_RESTORE, 0, 0, 0, 0, null, null);
        } else if (wmState == WorkInfo.State.RUNNING) {
            String progressState = workInfo.getProgress().getString(RestoreWorker.PROGRESS_KEY_STATE);
            if (progressState == null) return null;
            SmsSyncState smsSyncState = mapProgressStateToSmsSyncState(progressState);
            if (smsSyncState == null) return null;
            int currentItem = workInfo.getProgress().getInt(RestoreWorker.PROGRESS_KEY_CURRENT_ITEM, 0);
            int itemsToRestore = workInfo.getProgress().getInt(RestoreWorker.PROGRESS_KEY_ITEMS_TO_RESTORE, 0);
            int restoredCount = workInfo.getProgress().getInt(RestoreWorker.PROGRESS_KEY_RESTORED_COUNT, 0);
            return new RestoreState(smsSyncState, currentItem, itemsToRestore, restoredCount, 0, null, null);
        }
        return null;
    }

    @Nullable
    private SmsSyncState mapProgressStateToSmsSyncState(String progressState) {
        switch (progressState) {
            case RestoreWorker.STATE_LOGIN:    return SmsSyncState.LOGIN;
            case RestoreWorker.STATE_CALC:     return SmsSyncState.CALC;
            case RestoreWorker.STATE_RESTORE:  return SmsSyncState.RESTORE;
            case RestoreWorker.STATE_FINISHED: return SmsSyncState.FINISHED_RESTORE;
            case RestoreWorker.STATE_CANCELED: return SmsSyncState.CANCELED_RESTORE;
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
        Log.d(TAG, "SmsRestoreService: registered cancel collector for " + uniqueWorkName);
    }

    /**
     * U-037 (BUG-005): Tears down the WorkInfo observer using the cached LiveData reference.
     * Previously the onDestroy path nulled the observer without calling removeObserver(),
     * leaving the observeForever registration alive and causing duplicate callbacks on restart.
     * Fix: always call removeObserver() via the cached workInfoLiveData field — works on all
     * paths (terminal state and onDestroy) without needing uniqueWorkName.
     */
    private void tearDownObserverAndCollector() {
        if (workInfoObserver != null && workInfoLiveData != null) {
            workInfoLiveData.removeObserver(workInfoObserver);
        }
        workInfoObserver = null;
        workInfoLiveData = null;
        if (cancelCollectorJob != null) {
            cancelCollectorJob.cancel(null);
            cancelCollectorJob = null;
        }
    }

    // U-031 AC-7: getRestoreTask() factory deleted — RestoreTask class deleted.
    // SmsRestoreService now dispatches via getScheduler().scheduleRestore(...).

    private void postError(Exception exception) {
        // U-020: App.post() replaced by repository.emitState() (IC-3)
        RestoreState errorState = state.transition(ERROR, exception);
        restoreStateChanged(errorState);
        App.syncStateRepository().emitState(errorState);
    }

    private void asyncClearCache() {
        new Thread("clearCache") {
            @Override
            public void run() {
                clearCache();
            }
        }.start();
    }

    synchronized void clearCache() {
        File tmp = getCacheDir();
        if (tmp == null) return; // not sure why this would return null

        Log.d(TAG, "clearing cache in " + tmp);
        for (File f : tmp.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("body");
            }
        })) {
            if (LOCAL_LOGV) Log.v(TAG, "deleting " + f);
            if (!f.delete()) Log.w(TAG, "error deleting " + f);
        }
    }

    // U-020: @Subscribe removed — restoreStateChanged() is called directly from
    // the WorkInfo observer (U-031). No Otto registration needed.
    @SuppressWarnings("deprecation")
    public void restoreStateChanged(final RestoreState state) {
        this.state = state;
        if (this.state.isInitialState()) return;

        if (this.state.isRunning()) {
            notification = createNotification(R.string.status_restore)
                    .setContentTitle(getString(R.string.status_restore))
                    .setContentText(state.getNotificationLabel(getResources()))
                    .setContentIntent(getPendingIntent(null))
                    .build();

            startForeground(RESTORE_ID, notification);
        } else {
            Log.d(TAG, "stopping service, state"+ this.state);
            // U-031 AC-9 (Option b): release FULL_WAKE_LOCK on terminal state.
            // Paired with acquireLocks() call in handleIntent() before enqueue.
            releaseLocks();
            stopForeground(true);
            stopSelf();
        }
    }

    // U-020: @Produce produceLastState() deleted (AC-9).
    // StateFlow.value provides sticky last-state semantics for late collectors (AC-4).

    // U-020: isServiceIdle() deleted (AC-9b).
    // Callers read App.syncStateRepository().getState().getValue().isRunning() instead.

    @SuppressWarnings("deprecation")
    @Override protected int wakeLockType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // hold a full wake lock when restoring on newer version of Android, since
            // the user needs to switch  back the sms app afterwards
            return PowerManager.FULL_WAKE_LOCK;
        } else {
            return super.wakeLockType();
        }
    }

    /**
     * Returns the application-scoped {@link BackupScheduler}.
     * <p>
     * Protected to allow test subclasses to inject a mock scheduler.
     * Mirrors {@code SmsBackupService.getScheduler()}.
     * Will be replaced by Hilt {@code @Inject} field injection in U-022.
     */
    protected BackupScheduler getScheduler() {
        return App.getScheduler(this);
    }

}
