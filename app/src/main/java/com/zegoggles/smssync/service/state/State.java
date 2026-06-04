package com.zegoggles.smssync.service.state;

import android.content.res.Resources;
import androidx.annotation.Nullable;
import android.text.TextUtils;
// U-026 AC-7: com.fsck.k9.mail.AuthenticationFailedException import removed
// U-026 AC-7: com.fsck.k9.mail.MessagingException import removed
// U-026 AC-7: com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException import removed
import com.zegoggles.smssync.R;
import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.mail.transport.XOAuth2FailedException;
import com.zegoggles.smssync.service.exception.ConnectivityException;
import com.zegoggles.smssync.service.exception.LocalizableException;
import com.zegoggles.smssync.service.exception.MissingPermissionException;
import com.zegoggles.smssync.service.exception.RequiresLoginException;

import java.util.EnumSet;

/**
 * Abstract base for backup/restore state objects.
 *
 * <p>U-026 AC-7: Deleted the {@code MessagingException} string-match block at lines 32–34.
 * {@code TemporaryImapException} (from U-025, {@code extends MailException implements
 * LocalizableException}) feeds the retained {@code LocalizableException} branch type-safely.
 * The three k-9 imports at lines 6–8 are removed. No behavior change on any error path
 * other than the deletion of the dead string-match branch.
 *
 * <p>U-026 AC-8: {@code isAuthException()} rewritten to use app-owned
 * {@link XOAuth2FailedException} and {@link RequiresLoginException} instead of k-9 types.
 */
public abstract class State {
    public final SmsSyncState state;
    public final Exception exception;
    public final @Nullable DataType dataType;

    State(SmsSyncState state, @Nullable DataType dataType, Exception exception) {
        this.state = state;
        this.exception = exception;
        this.dataType = dataType;
    }

    public String getErrorMessage(Resources resources) {
        if (exception == null) return null;

        // U-026 AC-7: The MessagingException magic-string block (lines 32–34) is deleted.
        // TemporaryImapException (implements LocalizableException) falls through to the
        // LocalizableException branch below, returning R.string.status_gmail_temp_error —
        // same localized string as the deleted string-match path.
        if (exception instanceof LocalizableException) {
            return resources.getString(((LocalizableException) exception).errorResourceId());
        } else {
            return exception.getLocalizedMessage();
        }
    }

    public String getDetailedErrorMessage(Resources resources) {
        final String msg = getErrorMessage(resources);
        if (msg != null && exception != null) {
            final String underlying = exception.getCause() != null ? exception.getCause().toString() : null;
            final StringBuilder message = new StringBuilder().append(msg)
                    .append(" (exception: ")
                    .append(exception.toString());

            if (!TextUtils.isEmpty(underlying)) {
                message.append(", underlying=").append(underlying);
            }
            return message.append(")").toString();
        } else {
            return null;
        }
    }

    public boolean isInitialState() {
        return state == SmsSyncState.INITIAL;
    }

    public boolean isRunning() {
        return EnumSet.of(
            SmsSyncState.LOGIN,
            SmsSyncState.CALC,
            SmsSyncState.BACKUP,
            SmsSyncState.RESTORE,
            SmsSyncState.UPDATING_THREADS).contains(state);
    }

    public boolean isFinished() {
        return !isInitialState() && !isRunning();
    }

    public abstract State transition(SmsSyncState newState, Exception exception);

    /**
     * U-026 AC-8: Rewritten to use app-owned types.
     *
     * <p>{@code exception instanceof XOAuth2AuthenticationFailedException} replaced by
     * {@link XOAuth2FailedException} (app-owned). {@code exception instanceof
     * AuthenticationFailedException} removed — the ACL maps AuthenticationFailed to
     * {@link RequiresLoginException}, which is already checked in the {@code ||} chain.
     * Same runtime behavior: returns {@code true} for OAuth2 failures and login failures.
     */
    public boolean isAuthException() {
        return exception instanceof XOAuth2FailedException ||
               exception instanceof RequiresLoginException;
    }

    public boolean isPermissionException() {
        return exception instanceof MissingPermissionException;
    }

    public boolean isConnectivityError() {
        return exception instanceof ConnectivityException;
    }

    public boolean isError() {
        return state == SmsSyncState.ERROR;
    }

    public boolean isCanceled() {
        return state == SmsSyncState.CANCELED_BACKUP || state == SmsSyncState.CANCELED_RESTORE;
    }

    public String getNotificationLabel(Resources resources) {
        switch (state) {
            case LOGIN: return resources.getString(R.string.status_login_details);
            case CALC:  return resources.getString(R.string.status_calc_details);
            case ERROR: return getErrorMessage(resources);
            default: return null;
        }
    }

    public String[] getMissingPermissions() {
        if (isPermissionException()) {
            MissingPermissionException mpe = (MissingPermissionException)exception;
            return mpe.permissions.toArray(new String[mpe.permissions.size()]);
        } else {
            return new String[0];
        }
    }
}
