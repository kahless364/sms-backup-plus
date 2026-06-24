package com.zegoggles.smssync.service.exception;

import com.zegoggles.smssync.R;

/**
 * U-054 (SE-002): Thrown / carried as a state exception when
 * {@link com.zegoggles.smssync.preferences.SecretStore#isEncryptionDegraded()} returns
 * {@code true} at the point a backup or restore is initiated.
 *
 * <p>Implements {@link LocalizableException} so that {@code State.getErrorMessage()} returns
 * a user-visible, localized string ({@code R.string.status_encryption_degraded_details})
 * via the existing {@code LocalizableException} branch in {@code State.getErrorMessage()}.
 *
 * <p>This exception is NOT a re-thrown Keystore error. It is a sentinel that the
 * scheduler / ViewModel uses to signal the degraded state to the UI layer.
 */
public class EncryptionDegradedException extends Exception implements LocalizableException {

    public EncryptionDegradedException() {
        super("Credential encryption degraded — backup blocked pending re-authentication");
    }

    @Override
    public int errorResourceId() {
        return R.string.status_encryption_degraded_details;
    }
}
