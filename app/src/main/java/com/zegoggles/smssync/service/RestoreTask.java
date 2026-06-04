package com.zegoggles.smssync.service;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CallLog;
import android.provider.Telephony;
import androidx.annotation.NonNull;
import android.util.Log;
// U-020: import com.squareup.otto.Subscribe removed
// U-026: all com.fsck.k9.* imports removed; engine now uses app-owned ACL types
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.Consts;
import com.zegoggles.smssync.auth.TokenRefreshException;
import com.zegoggles.smssync.auth.TokenRefresher;
import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.mail.MessageConverter;
import com.zegoggles.smssync.mail.transport.BackupFolderHandle;
import com.zegoggles.smssync.mail.transport.MailException;
import com.zegoggles.smssync.mail.transport.MailMessageHandle;
import com.zegoggles.smssync.mail.transport.MailTransport;
import com.zegoggles.smssync.mail.transport.MessageImportResult;
import com.zegoggles.smssync.mail.transport.XOAuth2FailedException;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.service.exception.RequiresLoginException;
import com.zegoggles.smssync.service.state.RestoreState;
import com.zegoggles.smssync.service.state.SmsSyncState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

import static com.zegoggles.smssync.App.LOCAL_LOGV;
import static com.zegoggles.smssync.App.TAG;
import static com.zegoggles.smssync.mail.DataType.CALLLOG;
import static com.zegoggles.smssync.mail.DataType.SMS;
import static com.zegoggles.smssync.service.state.SmsSyncState.CALC;
import static com.zegoggles.smssync.service.state.SmsSyncState.CANCELED_RESTORE;
import static com.zegoggles.smssync.service.state.SmsSyncState.FINISHED_RESTORE;
import static com.zegoggles.smssync.service.state.SmsSyncState.LOGIN;
import static com.zegoggles.smssync.service.state.SmsSyncState.RESTORE;
import static com.zegoggles.smssync.service.state.SmsSyncState.UPDATING_THREADS;

/**
 * AsyncTask that performs the SMS/call-log restore from IMAP.
 *
 * <p>U-026 AC-3: Rewired from {@code BackupImapStore} (k-9 type) to {@link MailTransport}
 * (app-owned ACL port). No {@code com.fsck.k9.*} import remains in this file per AC-3
 * and AC-10. The transport is obtained from {@link SmsRestoreService#getMailTransport()}
 * (the sole construction point per IC-1).
 */
@SuppressWarnings("deprecation")
class RestoreTask extends AsyncTask<RestoreConfig, RestoreState, RestoreState> {
    private static final String ERROR = "error";
    private Set<String> smsIds = new HashSet<String>();
    private Set<String> callLogIds = new HashSet<String>();
    private Set<String> uids = new HashSet<String>();

    @SuppressLint("StaticFieldLeak")
    private final SmsRestoreService service;
    private final ContentResolver resolver;
    private final MessageConverter converter;
    private final TokenRefresher tokenRefresher;
    private final Preferences preferences;

    /**
     * U-023: @Inject annotation added to existing single constructor (AC-5).
     * No manual new-wiring exists to remove and no test-only constructor is present —
     * this is the sole constructor. The body is unchanged.
     * SmsRestoreService (an Android Service) is not injectable by Hilt; RestoreTask
     * is therefore not in the Hilt component graph (same coexistence approach as BackupTask).
     */
    @Inject
    RestoreTask(SmsRestoreService service,
                MessageConverter converter,
                ContentResolver resolver,
                TokenRefresher tokenRefresher) {
        this.service = service;
        this.converter = converter;
        this.resolver = resolver;
        this.tokenRefresher = tokenRefresher;
        this.preferences = service.getPreferences();
    }

    @Override
    protected void onPreExecute() {
        // U-020: App.register(this) removed. Cancel events collected via BackupCancelCollector.
    }

    /** U-020: called by BackupCancelCollector when SyncEvent.Cancel is received. */
    void onCancelRequested(boolean mayInterrupt) {
        cancel(mayInterrupt);
    }

    @NonNull protected RestoreState doInBackground(RestoreConfig... params) {
        if (params == null || params.length == 0) throw new IllegalArgumentException("No config passed");
        RestoreConfig config = params[0];

        // U-020: subscribe to Cancel events on this background thread
        if (App.syncStateRepository() != null) {
            BackupCancelCollector.collectForRestore(App.syncStateRepository(), this);
        }

        if (!config.restoreSms && !config.restoreCallLog) {
            return new RestoreState(FINISHED_RESTORE, 0, 0, 0, 0, null, null);
        } else {
            try {
                service.acquireLocks();
                return restore(config);
            } finally {
                service.releaseLocks();
            }
        }
    }

    /**
     * U-026 AC-3: Rewired from BackupImapStore to MailTransport.
     *
     * <p>All IMAP calls go through the port:
     * <ul>
     *   <li>{@code imapStore.checkSettings()} → {@code transport.checkSettings()}</li>
     *   <li>{@code imapStore.getFolder(SMS,...).getMessages(...)} →
     *       {@code transport.openFolder(SMS, prefs)} + {@code transport.getMessages(folder, ...)}</li>
     *   <li>Per-message body fetch → {@code transport.importMessageBody(folder, handle, converter)}</li>
     *   <li>{@code imapStore.closeFolders()} → {@code transport.closeFolders()}</li>
     * </ul>
     * No {@code com.fsck.k9.*} type appears in this method per AC-10.
     */
    private RestoreState restore(RestoreConfig config) {
        // U-026 AC-3(a): obtain MailTransport from config
        final MailTransport transport = config.imapStore;

        int currentRestoredItem = config.currentRestoredItem;
        try {
            publishProgress(LOGIN);
            // U-026 AC-3: checkSettings via transport port
            transport.checkSettings();

            publishProgress(CALC);

            final List<MailMessageHandle> msgs = new ArrayList<MailMessageHandle>();

            if (config.restoreSms) {
                // U-026 AC-3(b): openFolder + getMessages replaces imapStore.getFolder().getMessages()
                BackupFolderHandle smsFolder = transport.openFolder(SMS, preferences.getDataTypePreferences());
                msgs.addAll(transport.getMessages(smsFolder, config.maxRestore, config.restoreOnlyStarred, null));
            }
            if (config.restoreCallLog) {
                // U-026 AC-3(b): openFolder + getMessages for call log
                BackupFolderHandle calllogFolder = transport.openFolder(CALLLOG, preferences.getDataTypePreferences());
                msgs.addAll(transport.getMessages(calllogFolder, config.maxRestore, config.restoreOnlyStarred, null));
            }

            // We also need the folder handles per message for importMessageBody.
            // Re-open folders (K9MailTransport caches them, so this is a no-op on the IMAP level).
            final BackupFolderHandle smsFolderHandle = config.restoreSms
                    ? transport.openFolder(SMS, preferences.getDataTypePreferences()) : null;
            final BackupFolderHandle calllogFolderHandle = config.restoreCallLog
                    ? transport.openFolder(CALLLOG, preferences.getDataTypePreferences()) : null;

            final int itemsToRestoreCount = config.maxRestore <= 0 ? msgs.size() : Math.min(msgs.size(), config.maxRestore);

            if (itemsToRestoreCount > 0) {
                for (; currentRestoredItem < itemsToRestoreCount && !isCancelled(); currentRestoredItem++) {
                    MailMessageHandle handle = msgs.get(currentRestoredItem);
                    // Determine which folder this message is from by checking the data type
                    // after import. We pass the SMS folder as the primary; call-log handles
                    // came from the calllog folder. We detect this by DataType.
                    DataType dataType = importMessage(transport, handle, smsFolderHandle, calllogFolderHandle);

                    msgs.set(currentRestoredItem, null); // help gc
                    publishProgress(new RestoreState(RESTORE, currentRestoredItem, itemsToRestoreCount, 0, 0, dataType, null));
                    if (currentRestoredItem % 50 == 0) {
                        //clear cache periodically otherwise SD card fills up
                        service.clearCache();
                    }
                }
                updateAllThreadsIfAnySmsRestored();
            } else {
                Log.d(TAG, "nothing to restore");
            }

            final int restoredCount = smsIds.size() + callLogIds.size();
            return new RestoreState(isCancelled() ? CANCELED_RESTORE : FINISHED_RESTORE,
                    currentRestoredItem,
                    itemsToRestoreCount,
                    restoredCount,
                    Math.max(0, uids.size() - restoredCount),
                    null, null);
        } catch (XOAuth2FailedException e) {
            // U-026 AC-4: app-owned XOAuth2FailedException replaces k-9 XOAuth2AuthenticationFailedException
            return handleAuthError(config, currentRestoredItem, e);
        } catch (RequiresLoginException e) {
            // U-026 AC-4: app-owned RequiresLoginException replaces k-9 AuthenticationFailedException
            return transition(SmsSyncState.ERROR, e);
        } catch (MailException e) {
            // U-026 AC-4: app-owned MailException replaces k-9 MessagingException
            Log.e(TAG, ERROR, e);
            updateAllThreadsIfAnySmsRestored();
            return transition(SmsSyncState.ERROR, e);
        } catch (IllegalStateException e) {
            // usually memory problems (Couldn't init cursor window)
            return transition(SmsSyncState.ERROR, e);
        } finally {
            // U-026 AC-3(e): closeFolders via transport port (does not throw)
            transport.closeFolders();
        }
    }

    /**
     * U-026 AC-3(d): Per-message body fetch via {@code transport.importMessageBody()} replacing
     * the k-9 {@code message.getFolder().fetch()} call.
     *
     * <p>The bounded-residual {@link MessageConverter} is passed to the transport adapter which
     * can access the k-9 {@code Message} inside the opaque {@link MailMessageHandle} to call the
     * converter — keeping all k-9 types inside {@code mail.transport} per AC-10.
     *
     * @param smsFolderHandle     open SMS folder handle (may be null if SMS not being restored)
     * @param calllogFolderHandle open call-log folder handle (may be null if call log not restored)
     */
    @SuppressWarnings("unchecked")
    private DataType importMessage(MailTransport transport,
                                   MailMessageHandle handle,
                                   BackupFolderHandle smsFolderHandle,
                                   BackupFolderHandle calllogFolderHandle) {
        uids.add(handle.uid);
        if (LOCAL_LOGV) Log.v(TAG, "fetching message uid " + handle.uid);

        // Prefer SMS folder; fall back to calllog folder. The adapter caches by DataType,
        // so the correct folder is used internally for the fetch.
        BackupFolderHandle folderHandle = smsFolderHandle != null ? smsFolderHandle : calllogFolderHandle;

        // U-026 AC-3(d): transport.importMessageBody replaces message.getFolder().fetch() + converter calls
        MessageImportResult result = transport.importMessageBody(folderHandle, handle, converter);

        if (result.failed || result.dataType == null || result.contentValues == null) {
            Log.e(TAG, "importMessageBody failed for uid=" + handle.uid);
            return null;
        }

        DataType dataType = result.dataType;
        try {
            switch (dataType) {
                case CALLLOG:
                    importCallLog(result.contentValues);
                    break;
                case SMS:
                    importSms(result.contentValues);
                    break;
                default:
                    if (LOCAL_LOGV) Log.d(TAG, "ignoring restore of type: " + dataType);
            }
        } catch (IOException e) {
            Log.e(TAG, ERROR, e);
        }
        return dataType;
    }

    private RestoreState handleAuthError(RestoreConfig config, int currentRestoredItem, XOAuth2FailedException e) {
        if (e.getStatus() == 400) {
            Log.d(TAG, "need to perform xoauth2 token refresh");
            if (config.tries < 1) {
                try {
                    tokenRefresher.refreshOAuth2Token();
                    // we got a new token, let's retry one more time - we need to pass in a new transport object
                    // since the auth params on it are immutable
                    // U-026: retryWithStore now accepts MailTransport; getMailTransport() is the seam
                    return restore(config.retryWithStore(currentRestoredItem, service.getMailTransport()));
                } catch (MailException ignored) {
                    // U-026 AC-4: MailException replaces MessagingException for swallowed retry failure
                    Log.w(TAG, ignored);
                } catch (TokenRefreshException refreshException) {
                    Log.w(TAG, refreshException);
                }
            } else {
                Log.w(TAG, "no new token obtained, giving up");
            }
        } else {
            Log.w(TAG, "unexpected xoauth status code " + e.getStatus());
        }
        return transition(SmsSyncState.ERROR, e);
    }

    private void publishProgress(SmsSyncState smsSyncState) {
        publishProgress(transition(smsSyncState, null));
    }

    private RestoreState transition(SmsSyncState smsSyncState, Exception exception) {
        return service.getState().transition(smsSyncState, exception);
    }

    @Override
    protected void onPostExecute(RestoreState result) {
        if (result != null) {
            Log.d(TAG, "finished (" + result + "/" + uids.size() + ")");
            post(result);
        }
        // U-020: App.unregister(this) removed
    }

    @Override
    protected void onCancelled() {
        Log.d(TAG, "restore cancelled");
        post(transition(CANCELED_RESTORE, null));
        // U-020: App.unregister(this) removed
    }

    @Override
    protected void onProgressUpdate(RestoreState... progress) {
        if (progress != null && progress.length > 0 && !isCancelled()) {
            post(progress[0]);
        }
    }

    private void post(RestoreState changed) {
        if (changed == null) return;
        // U-020: App.post(changed) replaced by repository.emitState + direct service callback (IC-3)
        if (App.syncStateRepository() != null) {
            App.syncStateRepository().emitState(changed);
        }
        service.restoreStateChanged(changed);
    }

    private void importSms(final ContentValues values) throws IOException {
        final Integer type = values.getAsInteger(Telephony.TextBasedSmsColumns.TYPE);

        // only restore inbox messages and sent messages - otherwise sms might get sent on restore
        if (type != null &&
              (type == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX ||
               type == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT) &&
            !smsExists(values)) {

            final Uri uri = resolver.insert(Consts.SMS_PROVIDER, values);
            if (uri != null) {
                smsIds.add(uri.getLastPathSegment());
                Long timestamp = values.getAsLong(Telephony.TextBasedSmsColumns.DATE);

                if (timestamp != null && preferences.getDataTypePreferences().getMaxSyncedDate(SMS) < timestamp) {
                    preferences.getDataTypePreferences().setMaxSyncedDate(SMS, timestamp);
                }

                if (LOCAL_LOGV) Log.v(TAG, "inserted " + uri);
            }
        } else {
            if (LOCAL_LOGV) Log.d(TAG, "ignoring sms");
        }
    }

    private void importCallLog(final ContentValues values) {
        if (!callLogExists(values)) {
            final Uri uri = resolver.insert(Consts.CALLLOG_PROVIDER, values);
            if (uri != null) callLogIds.add(uri.getLastPathSegment());
        } else {
            if (LOCAL_LOGV) Log.d(TAG, "ignoring call log");
        }
    }

    private boolean callLogExists(ContentValues values) {
        Cursor c = resolver.query(Consts.CALLLOG_PROVIDER,
            new String[] { "_id" },
            "date = ? AND number = ? AND duration = ? AND type = ?",
            new String[]{
                values.getAsString(CallLog.Calls.DATE),
                values.getAsString(CallLog.Calls.NUMBER),
                values.getAsString(CallLog.Calls.DURATION),
                values.getAsString(CallLog.Calls.TYPE)
            },
            null
        );
        boolean exists = false;
        if (c != null) {
            exists = c.getCount() > 0;
            c.close();
        }
        return exists;
    }

    private boolean smsExists(ContentValues values) {
        // just assume equality on date+address+type
        Cursor c = resolver.query(Consts.SMS_PROVIDER,
            new String[] {"_id" },
            "date = ? AND address = ? AND type = ?",
            new String[] {
                values.getAsString(Telephony.TextBasedSmsColumns.DATE),
                values.getAsString(Telephony.TextBasedSmsColumns.ADDRESS),
                values.getAsString(Telephony.TextBasedSmsColumns.TYPE)
            },
            null
        );

        boolean exists = false;
        if (c != null) {
            exists = c.getCount() > 0;
            c.close();
        }
        return exists;
    }

    private void updateAllThreadsIfAnySmsRestored() {
        if (smsIds.size() > 0) {
            updateAllThreads();
        }
    }

    private void updateAllThreads() {
        // thread dates + states might be wrong, we need to force a full update
        // unfortunately there's no direct way to do that in the SDK, but passing a
        // negative conversation id to delete should to the trick
        publishProgress(UPDATING_THREADS);
        Log.d(TAG, "updating threads");
        resolver.delete(Uri.parse("content://sms/conversations/-1"), null, null);
        Log.d(TAG, "finished");
    }

    protected Set<String> getSmsIds() {
        return smsIds;
    }
}
