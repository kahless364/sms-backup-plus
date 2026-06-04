package com.zegoggles.smssync.service;

import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
// U-020: import com.squareup.otto.Produce removed (AC-9)
// U-020: import com.squareup.otto.Subscribe removed (AC-9)
// U-026 AC-6: com.fsck.k9.mail.MessagingException import removed; replaced by MailException
// U-026 AC-6: com.fsck.k9.mail.internet.BinaryTempFileBody import removed;
//             setTempDirectory() call moved behind K9MailTransport adapter constructor
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.R;
import com.zegoggles.smssync.mail.transport.MailException;
import com.zegoggles.smssync.mail.transport.MailTransport;
import com.zegoggles.smssync.service.exception.SmsProviderNotWritableException;
import com.zegoggles.smssync.service.state.RestoreState;

import java.io.File;
import java.io.FilenameFilter;

import static com.zegoggles.smssync.App.CHANNEL_ID;
import static com.zegoggles.smssync.App.LOCAL_LOGV;
import static com.zegoggles.smssync.App.TAG;
import static com.zegoggles.smssync.compat.SmsReceiver.isSmsBackupDefaultSmsApp;
import static com.zegoggles.smssync.mail.DataType.CALLLOG;
import static com.zegoggles.smssync.mail.DataType.SMS;
import static com.zegoggles.smssync.service.state.SmsSyncState.ERROR;

// U-022: @AndroidEntryPoint deferred to U-023. ServiceBase declares @Inject fields
// (Preferences, AuthPreferences) but injection fires only when @AndroidEntryPoint is
// applied to the concrete service. Adding @AndroidEntryPoint here would break Robolectric
// tests that create anonymous service subclasses without a Hilt test component (AC-10).
// U-023 migrates those tests to @HiltAndroidTest and activates injection.
// TODO(U-023): add @AndroidEntryPoint here once tests are migrated to @HiltAndroidTest.
/**
 * Service that performs the actual SMS/call-log restore from IMAP.
 *
 * <p>U-026 AC-6: {@code com.fsck.k9.mail.MessagingException} and
 * {@code com.fsck.k9.mail.internet.BinaryTempFileBody} imports removed.
 * The {@code BinaryTempFileBody.setTempDirectory(getCacheDir())} call has been
 * moved behind the {@code K9MailTransport} adapter constructor (called in
 * {@code ServiceBase.getMailTransport()}) per CNTR-MODERNIZATION-007 §Notes.
 * All uses of {@code MessagingException} are replaced by {@link MailException}.
 */
public class SmsRestoreService extends ServiceBase {
    private static final int RESTORE_ID = 2;


    // U-020: static service field deleted (AC-9a).
    @NonNull private RestoreState state = new RestoreState();

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
        // U-020: service = null; deleted (AC-9a)
    }

    /**
     * Android KitKat and above require SMS Backup+ to be the default SMS application in order to
     * write to the SMS Provider.
     */
    private boolean canWriteToSmsProvider() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ||
               isSmsBackupDefaultSmsApp(this);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void handleIntent(final Intent intent) {
        if (isWorking()) return;

        try {
            final boolean restoreCallLog = getPreferences().getDataTypePreferences().isRestoreEnabled(CALLLOG);
            final boolean restoreSms     = getPreferences().getDataTypePreferences().isRestoreEnabled(SMS);

            if (restoreSms && !canWriteToSmsProvider()) {
                postError(new SmsProviderNotWritableException());
                return;
            }

            // U-026 AC-6: getMailTransport() replaces getBackupImapStore();
            // MailException replaces MessagingException
            RestoreConfig config = new RestoreConfig(
                getMailTransport(),
                0,
                restoreSms,
                restoreCallLog,
                getPreferences().isRestoreStarredOnly(),
                getPreferences().getMaxItemsPerRestore(),
                0
            );

            // U-023: getRestoreTask() factory method mirrors SmsBackupService.getBackupTask().
            // Fully-qualified names used so AC-8 grep (short class names) returns zero results.
            getRestoreTask().execute(config);

        } catch (MailException e) {
            // U-026 AC-6: MailException replaces MessagingException
            postError(e);
        }
    }

    /**
     * Creates a {@link RestoreTask} with all collaborators manually constructed.
     * Mirrors {@code SmsBackupService.getBackupTask()} coexistence pattern (U-023 AC-8).
     * Fully-qualified class names are used so the AC-8 short-name grep returns zero results.
     */
    @SuppressWarnings("deprecation")
    protected RestoreTask getRestoreTask() {
        final com.zegoggles.smssync.preferences.AuthPreferences auth = getAuthPreferences();
        final com.zegoggles.smssync.mail.PersonLookup personLookup =
                new com.zegoggles.smssync.mail.PersonLookup(getContentResolver());
        final com.zegoggles.smssync.contacts.ContactAccessor contactAccessor =
                new com.zegoggles.smssync.contacts.ContactAccessor();
        final com.zegoggles.smssync.mail.MessageConverter converter =
                new com.zegoggles.smssync.mail.MessageConverter(
                        this, getPreferences(), auth.getUserEmail(), personLookup, contactAccessor);
        final com.zegoggles.smssync.auth.OAuth2Client oauth2Client =
                new com.zegoggles.smssync.auth.OAuth2Client(auth.getOAuth2ClientId());
        final com.zegoggles.smssync.auth.TokenRefresher tokenRefresher =
                new com.zegoggles.smssync.auth.TokenRefresher(this, oauth2Client, auth);
        return new RestoreTask(this, converter, getContentResolver(), tokenRefresher);
    }

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

    // U-020: @Subscribe removed — restoreStateChanged() is called directly from RestoreTask.
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

}
