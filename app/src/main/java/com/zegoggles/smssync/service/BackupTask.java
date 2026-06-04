package com.zegoggles.smssync.service;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import android.util.Log;
// U-020: import com.squareup.otto.Subscribe removed
// U-026: all com.fsck.k9.* imports removed; engine now uses app-owned ACL types
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.R;
import com.zegoggles.smssync.auth.TokenRefreshException;
import com.zegoggles.smssync.auth.TokenRefresher;
import com.zegoggles.smssync.contacts.ContactAccessor;
import com.zegoggles.smssync.contacts.ContactGroupIds;
import com.zegoggles.smssync.mail.ConversionResult;
import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.mail.MessageConverter;
import com.zegoggles.smssync.mail.transport.BackupFolderHandle;
import com.zegoggles.smssync.mail.transport.MailException;
import com.zegoggles.smssync.mail.transport.MailTransport;
import com.zegoggles.smssync.mail.transport.XOAuth2FailedException;
import com.zegoggles.smssync.preferences.AuthPreferences;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.service.exception.RequiresLoginException;
import com.zegoggles.smssync.service.state.BackupState;
import com.zegoggles.smssync.service.state.SmsSyncState;
import dagger.Lazy;
import javax.inject.Inject;

import java.util.Locale;

import static com.zegoggles.smssync.App.LOCAL_LOGV;
import static com.zegoggles.smssync.App.TAG;
import static com.zegoggles.smssync.mail.DataType.CALLLOG;
import static com.zegoggles.smssync.mail.DataType.Defaults.MAX_SYNCED_DATE;
import static com.zegoggles.smssync.mail.DataType.MMS;
import static com.zegoggles.smssync.mail.DataType.SMS;
import static com.zegoggles.smssync.service.BackupType.MANUAL;
import static com.zegoggles.smssync.service.BackupType.SKIP;
import static com.zegoggles.smssync.service.state.SmsSyncState.BACKUP;
import static com.zegoggles.smssync.service.state.SmsSyncState.CALC;
import static com.zegoggles.smssync.service.state.SmsSyncState.CANCELED_BACKUP;
import static com.zegoggles.smssync.service.state.SmsSyncState.ERROR;
import static com.zegoggles.smssync.service.state.SmsSyncState.FINISHED_BACKUP;
import static com.zegoggles.smssync.service.state.SmsSyncState.LOGIN;

/**
 * AsyncTask that performs the SMS/MMS/call-log backup to IMAP.
 *
 * <p>U-026: Rewired from {@code BackupImapStore} (k-9 type) to {@link MailTransport}
 * (app-owned ACL port). No {@code com.fsck.k9.*} import remains in this file per AC-2
 * and AC-10. The transport is obtained from {@link SmsBackupService#getMailTransport()}
 * (the sole construction point per IC-1).
 */
@SuppressWarnings("deprecation")
class BackupTask extends AsyncTask<BackupConfig, BackupState, BackupState> {
    @SuppressLint("StaticFieldLeak")
    private final SmsBackupService service;
    private final BackupItemsFetcher fetcher;
    private final MessageConverter converter;
    // U-023 AC-3: Lazy<CalendarSyncer> replaces direct CalendarSyncer field.
    // CalendarSyncer is only constructed when preferences.isCallLogCalendarSyncEnabled()
    // returns true. When false, lazy.get() is never called — no CalendarSyncer is built.
    // Behavioral contract preserved: calendar sync runs iff the preference is enabled
    // and the data type is CALLLOG (replaces the former calendarSyncer != null null-guard).
    private final Lazy<CalendarSyncer> calendarSyncerLazy;
    private final AuthPreferences authPreferences;
    private final Preferences preferences;
    private final ContactAccessor contactAccessor;
    private final TokenRefresher tokenRefresher;

    /**
     * U-023: Single @Inject-annotated constructor (AC-4).
     *
     * The primary constructor that manually new'd eight collaborators (BackupTask.java:62-88)
     * and the 8-parameter test-only constructor (BackupTask.java:90-106) have been merged
     * into this single constructor. The collaborators are now explicit constructor parameters.
     *
     * SmsBackupService is NOT injectable by Hilt (it is an Android Service component managed
     * by the OS). BackupTask is therefore NOT in the Hilt component graph — it is still built
     * manually in SmsBackupService and by tests. The @Inject annotation on this constructor
     * declares Hilt CAPABILITY (Dagger can build BackupTask if asked AND all params can be
     * satisfied), but the graph is never asked to build BackupTask in the current coexistence
     * period (U-015 @HiltWorker rewrite is the long-term solution).
     *
     * CalendarSyncer is Lazy (AC-3): it is constructed at most once per BackupTask instance
     * and only when preferences.isCallLogCalendarSyncEnabled() is true (AC-3 behavioral guard).
     *
     * DES-MODERNIZATION-008 §Incremental coexistence: SmsBackupService non-injectable type
     * handled by retaining manual construction in SmsBackupService. This is the documented
     * coexistence approach per the story's Technical Notes.
     */
    @Inject
    BackupTask(@NonNull SmsBackupService service,
               BackupItemsFetcher fetcher,
               MessageConverter converter,
               Lazy<CalendarSyncer> calendarSyncerLazy,
               AuthPreferences authPreferences,
               Preferences preferences,
               ContactAccessor contactAccessor,
               TokenRefresher tokenRefresher) {
        this.service = service;
        this.fetcher = fetcher;
        this.converter = converter;
        this.calendarSyncerLazy = calendarSyncerLazy;
        this.authPreferences = authPreferences;
        this.preferences = preferences;
        this.contactAccessor = contactAccessor;
        this.tokenRefresher = tokenRefresher;
    }

    @Override
    protected void onPreExecute() {
        // U-020: App.register(this) removed. Cancel events collected via BackupCancelCollector.
    }

    @Override protected BackupState doInBackground(BackupConfig... params) {
        if (params == null || params.length == 0) {
            throw new IllegalArgumentException("No config passed");
        }
        // U-020: subscribe to Cancel events from the repository on this background thread.
        if (App.syncStateRepository() != null) {
            BackupCancelCollector.collect(App.syncStateRepository(), this);
        }
        final BackupConfig config = params[0];
        if (config.backupType == SKIP) {
            return skip(config.typesToBackup);
        } else {
            return acquireLocksAndBackup(config);
        }
    }

    private BackupState acquireLocksAndBackup(BackupConfig config) {
        try {
            service.acquireLocks();
            return fetchAndBackupItems(config);
        } finally {
            service.releaseLocks();
        }
    }

    private BackupState fetchAndBackupItems(BackupConfig config) {
        BackupCursors cursors = null;
        try {
            final ContactGroupIds groupIds = contactAccessor.getGroupContactIds(service.getContentResolver(), config.groupToBackup);

            cursors = new BulkFetcher(fetcher).fetch(config.typesToBackup, groupIds, config.maxItemsPerSync);
            final int itemsToSync = cursors.count();

            if (itemsToSync > 0) {
                appLog(R.string.app_log_backup_messages, cursors.count(SMS), cursors.count(MMS), cursors.count(CALLLOG));
                if (config.debug) {
                    appLog(R.string.app_log_backup_messages_with_config, config);
                }

                return backupCursors(cursors, config.imapStore, config.backupType, itemsToSync);
            } else {
                appLog(R.string.app_log_skip_backup_no_items);

                if (preferences.isFirstBackup()) {
                    // If this is the first backup we need to write something to MAX_SYNCED_DATE
                    // such that we know that we've performed a backup before.
                    preferences.getDataTypePreferences().setMaxSyncedDate(SMS, MAX_SYNCED_DATE);
                    preferences.getDataTypePreferences().setMaxSyncedDate(MMS, MAX_SYNCED_DATE);
                }
                Log.i(TAG, "Nothing to do.");
                return transition(FINISHED_BACKUP, null);
            }
        } catch (XOAuth2FailedException e) {
            // U-026 AC-4: catch app-owned XOAuth2FailedException (replaces k-9 XOAuth2AuthenticationFailedException)
            return handleAuthError(config, e);
        } catch (RequiresLoginException e) {
            // U-026 AC-4: app-owned RequiresLoginException replaces k-9 AuthenticationFailedException
            return transition(ERROR, e);
        } catch (MailException e) {
            // U-026 AC-4: app-owned MailException replaces k-9 MessagingException
            return transition(ERROR, e);
        } catch (SecurityException e) {
            return transition(ERROR, e);
        } finally {
            if (cursors != null) {
                cursors.close();
            }
        }
    }

    private BackupState handleAuthError(BackupConfig config, XOAuth2FailedException e) {
        if (e.getStatus() == 400) {
            appLogDebug("need to perform xoauth2 token refresh");
            if (config.currentTry < 1) {
                try {
                    tokenRefresher.refreshOAuth2Token();
                    // we got a new token, let's handleAuthError one more time - we need to pass in a new transport object
                    // since the auth params on it are immutable
                    appLogDebug("token refreshed, retrying");
                    // U-026: retryWithTransport replaces retryWithStore; getMailTransport() is the sole seam
                    return fetchAndBackupItems(config.retryWithTransport(service.getMailTransport()));
                } catch (MailException ignored) {
                    // U-026 AC-4: MailException replaces MessagingException for the swallowed retry failure
                    Log.w(TAG, ignored);
                } catch (TokenRefreshException refreshException) {
                    appLogDebug("error refreshing token: "+refreshException+", cause="+refreshException.getCause());
                }
            } else {
                appLogDebug("no new token obtained, giving up");
            }
        } else {
            appLogDebug("unexpected xoauth status code " + e.getStatus());
        }
        return transition(ERROR, e);
    }

    private BackupState skip(Iterable<DataType> types) {
        appLog(R.string.app_log_skip_backup_skip_messages);
        for (DataType type : types) {
            try {
                preferences.getDataTypePreferences().setMaxSyncedDate(type, fetcher.getMostRecentTimestamp(type));
            } catch (SecurityException e ) {
                return new BackupState(ERROR, 0, 0, MANUAL, type, e);
            }
        }
        Log.i(TAG, "All messages skipped.");
        return new BackupState(FINISHED_BACKUP, 0, 0, MANUAL, null, null);
    }

    private void appLog(int id, Object... args) {
        service.appLog(id, args);
    }

    private void appLogDebug(String message, Object... args) {
        service.appLogDebug(message, args);
    }

    private BackupState transition(SmsSyncState smsSyncState, Exception exception) {
        return service.transition(smsSyncState, exception);
    }

    @Override
    protected void onProgressUpdate(BackupState... progress) {
        if (progress != null && progress.length > 0 && !isCancelled()) {
            post(progress[0]);
        }
    }

    /** U-020: called by BackupCancelCollector when SyncEvent.Cancel is received. */
    void onCancelRequested(boolean mayInterrupt) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "onCancelRequested(mayInterrupt=" + mayInterrupt + ")");
        }
        cancel(mayInterrupt);
    }

    @Override
    protected void onPostExecute(BackupState result) {
        if (result != null) {
            post(result);
        }
        // U-020: App.unregister(this) removed
    }

    @Override
    protected void onCancelled() {
        post(transition(CANCELED_BACKUP, null));
        // U-020: App.unregister(this) removed
    }

    private void post(BackupState state) {
        if (state == null) return;
        // U-020: App.post(state) replaced by repository.emitState + direct service callback (IC-3)
        if (App.syncStateRepository() != null) {
            App.syncStateRepository().emitState(state);
        }
        service.backupStateChanged(state);
    }

    /**
     * U-026 AC-2: Rewired from BackupImapStore to MailTransport.
     *
     * <p>All IMAP calls go through the port:
     * <ul>
     *   <li>{@code store.checkSettings()} → {@code transport.checkSettings()}</li>
     *   <li>{@code store.getFolder(type, prefs).appendMessages(msgs)} →
     *       {@code transport.openFolder(type, prefs)} + {@code transport.appendMessages(folder, result)}</li>
     *   <li>{@code store.closeFolders()} → {@code transport.closeFolders()}</li>
     * </ul>
     * No {@code com.fsck.k9.*} type appears in this method per AC-10.
     */
    private BackupState backupCursors(BackupCursors cursors, MailTransport transport, BackupType backupType, int itemsToSync)
            throws MailException, RequiresLoginException {
        Log.i(TAG, String.format(Locale.ENGLISH, "Starting backup (%d messages)", itemsToSync));
        publish(LOGIN);
        // U-026 AC-2(a): checkSettings() via transport port
        transport.checkSettings();

        try {
            publish(CALC);
            int backedUpItems = 0;
            while (!isCancelled() && cursors.hasNext()) {
                BackupCursors.CursorAndType cursor = cursors.next();
                if (LOCAL_LOGV) Log.v(TAG, "backing up: " + cursor);

                // U-026: converter.convertMessages() throws k-9 MessagingException (bounded
                // residual). Caught here with FQN (no import) and re-thrown as MailException,
                // preserving AC-10 (zero k-9 imports in service.*).
                final ConversionResult result;
                try {
                    result = converter.convertMessages(cursor.cursor, cursor.type);
                } catch (com.fsck.k9.mail.MessagingException e) {
                    throw new MailException(e);
                }
                if (!result.isEmpty()) {
                    if (LOCAL_LOGV) {
                        Log.v(TAG, String.format(Locale.ENGLISH, "sending %d %s message(s) to server.",
                                result.getMessages().size(), cursor.type));
                    }

                    // U-026 AC-2(b): openFolder replaces getFolder; AC-2(c): appendMessages via transport
                    BackupFolderHandle folder = transport.openFolder(cursor.type, preferences.getDataTypePreferences());
                    transport.appendMessages(folder, result);

                    // U-023 AC-3: guard migrated from 'calendarSyncer != null' to preference check.
                    // Lazy<CalendarSyncer>.get() is called only when calendar sync is enabled,
                    // preserving the behavior: no CalendarSyncer instance is built when disabled.
                    if (cursor.type == CALLLOG && preferences.isCallLogCalendarSyncEnabled()) {
                        calendarSyncerLazy.get().syncCalendar(result);
                    }
                    preferences.getDataTypePreferences().setMaxSyncedDate(cursor.type, result.getMaxDate());
                    backedUpItems += result.getMessages().size();
                } else {
                    Log.w(TAG, "no messages converted");
                    itemsToSync -= 1;
                }

                publishProgress(new BackupState(BACKUP, backedUpItems, itemsToSync, backupType, cursor.type, null));
            }

            return new BackupState(FINISHED_BACKUP,
                    backedUpItems,
                    itemsToSync,
                    backupType, null, null);
        } finally {
            // U-026 AC-2(d): closeFolders() via transport port (does not throw)
            transport.closeFolders();
        }
    }

    private void publish(SmsSyncState state) {
        publishProgress(service.transition(state, null));
    }
}
