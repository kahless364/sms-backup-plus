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
import android.util.Log;
import com.fsck.k9.mail.MessagingException;
// U-020: import com.squareup.otto.Produce removed (AC-8)
// U-020: import com.squareup.otto.Subscribe removed (AC-8)
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.R;
import com.zegoggles.smssync.activity.MainActivity;
import com.zegoggles.smssync.mail.BackupImapStore;
import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.scheduler.BackupScheduler;
import com.zegoggles.smssync.scheduler.ScheduledJob;
import com.zegoggles.smssync.service.exception.BackupDisabledException;
import com.zegoggles.smssync.service.exception.MissingPermissionException;
import com.zegoggles.smssync.service.exception.RequiresLoginException;
import com.zegoggles.smssync.service.state.BackupState;
import com.zegoggles.smssync.service.state.SmsSyncState;

import java.util.EnumSet;
import java.util.HashSet;
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
 * <p>U-022: @AndroidEntryPoint deferred to U-023. ServiceBase declares @Inject fields
 * (Preferences, AuthPreferences) but injection fires only when @AndroidEntryPoint is
 * applied to the concrete service. Adding @AndroidEntryPoint here would break Robolectric
 * tests that create anonymous service subclasses without a Hilt test component (AC-10).
 * U-023 migrates those tests to @HiltAndroidTest and activates injection.
 * TODO(U-023): add @AndroidEntryPoint here once tests are migrated to @HiltAndroidTest.
 */
public class SmsBackupService extends ServiceBase {
    private static final int BACKUP_ID = 1;
    private static final int NOTIFICATION_ID_WARNING = 1;

    // U-020: static service field deleted (AC-8a). State is read via syncStateRepository().
    @NonNull private BackupState state = new BackupState();

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
            getBackupTask().execute(getBackupConfig(backupType, enabledTypes, getBackupImapStore()));
        } catch (MessagingException e) {
            Log.w(TAG, e);
            moveToState(state.transition(ERROR, e));
        // U-017: catch(ConnectivityException) removed — legacyCheckConnectivity() deleted.
        } catch (RequiresLoginException e) {
            appLog(R.string.app_log_missing_credentials);
            moveToState(state.transition(ERROR, e));
        } catch (BackupDisabledException e) {
            moveToState(state.transition(FINISHED_BACKUP, e));
        } catch (MissingPermissionException e) {
            moveToState(state.transition(ERROR, e));
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

    private BackupConfig getBackupConfig(BackupType backupType,
                                         EnumSet<DataType> enabledTypes,
                                         BackupImapStore imapStore) {
        return new BackupConfig(
            imapStore,
            0,
            getPreferences().getMaxItemsPerSync(),
            getPreferences().getBackupContactGroup(),
            backupType,
            enabledTypes,
            getPreferences().isAppLogDebug()
        );
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

    protected BackupTask getBackupTask() {
        // U-023: Primary BackupTask constructor removed (manual new-wiring deleted per AC-2).
        // SmsBackupService supplies itself and manually builds each collaborator so that the
        // service's ContentResolver/context is used. This mirrors the previous primary ctor
        // body but is now explicit rather than hidden inside BackupTask.
        // SmsBackupService cannot be injected by Hilt (it is an Android Service); the
        // DI coexistence approach is: manually build here until U-015 @HiltWorker removes
        // BackupTask entirely (DES-MODERNIZATION-008 §Incremental coexistence).
        final android.content.Context context = getApplicationContext();
        final com.zegoggles.smssync.preferences.AuthPreferences auth = getAuthPreferences();
        final com.zegoggles.smssync.preferences.Preferences prefs = getPreferences();
        final com.zegoggles.smssync.mail.PersonLookup personLookup =
                new com.zegoggles.smssync.mail.PersonLookup(getContentResolver());
        final com.zegoggles.smssync.contacts.ContactAccessor contactAccessor =
                new com.zegoggles.smssync.contacts.ContactAccessor();
        final com.zegoggles.smssync.service.BackupQueryBuilder queryBuilder =
                new com.zegoggles.smssync.service.BackupQueryBuilder(prefs.getDataTypePreferences());
        final com.zegoggles.smssync.service.BackupItemsFetcher fetcher =
                new com.zegoggles.smssync.service.BackupItemsFetcher(getContentResolver(), queryBuilder);
        final com.zegoggles.smssync.mail.MessageConverter converter =
                new com.zegoggles.smssync.mail.MessageConverter(
                        context, prefs, auth.getUserEmail(), personLookup, contactAccessor);
        final com.zegoggles.smssync.auth.OAuth2Client oauth2Client =
                new com.zegoggles.smssync.auth.OAuth2Client(auth.getOAuth2ClientId());
        final com.zegoggles.smssync.auth.TokenRefresher tokenRefresher =
                new com.zegoggles.smssync.auth.TokenRefresher(this, oauth2Client, auth);
        // Lazy<CalendarSyncer>: only build CalendarSyncer when isCallLogCalendarSyncEnabled().
        // Mirrors the conditional construction that was at BackupTask.java:76-86.
        final dagger.Lazy<com.zegoggles.smssync.service.CalendarSyncer> calendarSyncerLazy = () -> {
            return new com.zegoggles.smssync.service.CalendarSyncer(
                    com.zegoggles.smssync.calendar.CalendarAccessor.Get.instance(getContentResolver()),
                    prefs.getCallLogCalendarId(),
                    personLookup,
                    new com.zegoggles.smssync.mail.CallFormatter(context.getResources())
            );
        };
        return new BackupTask(this, fetcher, converter, calendarSyncerLazy,
                auth, prefs, contactAccessor, tokenRefresher);
    }

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
    // BackupTask (post method) and moveToState(). No Otto registration needed.
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
     */
    /**
     * U-017: isUseOldScheduler() guard removed — WorkManagerScheduler persists periodic
     * work automatically. scheduleRegular() is still called here to ensure the periodic
     * work request is re-queued after a regular backup completes (WorkManager replaces
     * any existing item via ExistingPeriodicWorkPolicy.UPDATE, which is a no-op if already
     * enqueued — safe to call unconditionally).
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
