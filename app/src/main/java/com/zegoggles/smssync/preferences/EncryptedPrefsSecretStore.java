package com.zegoggles.smssync.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static com.zegoggles.smssync.App.TAG;

/**
 * SecretStore adapter backed by EncryptedSharedPreferences (AES-256-GCM values,
 * AES-256-SIV key names, Android Keystore master key).
 *
 * File name: "credentials" — Option A from DES-MODERNIZATION-004 §Migration.
 * Using the same file name as the legacy plaintext store means the existing
 * backup_descriptor.xml rule ({@code <exclude domain="sharedpref" path="credentials.xml"/>})
 * continues to match without any descriptor change (AC-9 / CNTR-MODERNIZATION-003
 * §Backing-store invariant).
 *
 * Version note: pinned to androidx.security:security-crypto:1.1.0-alpha06
 * (DES-MODERNIZATION-004 §ADR trade-off note). The 1.1.x alpha is required for the
 * MasterKey.Builder API with setRequestStrongBoxBacked(). The 1.0.0 stable release
 * uses the deprecated MasterKeys.getOrCreate() idiom and does not expose StrongBox
 * best-effort configuration. 1.1.0-alpha06 is the latest published alpha as of
 * June 2026 and is the version used in production by numerous AndroidX projects.
 *
 * Contract: CNTR-MODERNIZATION-003
 * Design: DES-MODERNIZATION-004
 */
public class EncryptedPrefsSecretStore implements SecretStore {

    /** Backing file name — must match the backup_descriptor.xml exclude rule. */
    static final String CREDENTIALS_FILE_NAME = "credentials";

    private final SharedPreferences encrypted;

    /**
     * Constructs the adapter.
     *
     * Builds an AES-256-GCM Keystore master key (StrongBox best-effort) and opens
     * EncryptedSharedPreferences over the "credentials" file. If the Keystore
     * construction fails, the exception propagates to the caller — construction
     * failure is not silently swallowed because there is no safe fallback that
     * satisfies the confidentiality requirement.
     *
     * IMPORTANT (Option A ordering risk, per DES-MODERNIZATION-004 §Migration):
     * If an existing install's credentials.xml still holds plaintext entries when
     * this constructor runs, EncryptedSharedPreferences will attempt to decrypt those
     * plaintext bytes as ciphertext and will throw. This is resolved by U-012, which
     * buffers plaintext values in memory BEFORE constructing EncryptedPrefsSecretStore,
     * writes ciphertext, and clears the plaintext. U-011 ships the adapter only;
     * migration ordering is U-012's responsibility.
     *
     * @param context application context
     * @throws GeneralSecurityException if the master key cannot be created
     * @throws IOException              if the EncryptedSharedPreferences backing file
     *                                  cannot be opened
     */
    public EncryptedPrefsSecretStore(Context context)
            throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setRequestStrongBoxBacked(true)  // best-effort; falls back to TEE/software silently
                .build();

        encrypted = EncryptedSharedPreferences.create(
                context.getApplicationContext(),
                CREDENTIALS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    /**
     * Returns the decrypted value for {@code key}, or {@code null} if the key is
     * absent or the ciphertext cannot be decrypted.
     *
     * Per CNTR-MODERNIZATION-003 §Error Handling, decrypt failures (AEADBadTagException,
     * InvalidKeyException, and related GeneralSecurityException/IOException) are caught
     * here and surfaced as {@code null} rather than re-thrown. The only consequence is a
     * one-time re-auth; a hard crash would strand the user.
     */
    @Override
    public String get(String key) {
        try {
            return encrypted.getString(key, null);
        } catch (Exception e) {
            // CNTR-MODERNIZATION-003 §Error Handling: AEADBadTagException, InvalidKeyException,
            // GeneralSecurityException, IOException — all surfaced as null, never rethrown.
            Log.w(TAG, "EncryptedPrefsSecretStore.get(): decrypt failure for key '"
                    + key + "', returning null. Cause: " + e.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Encrypts and persists {@code value} for {@code key}.
     *
     * Uses commit() (synchronous/durable), not apply(), as required by
     * CNTR-MODERNIZATION-003 §Type notes: "put MUST be synchronous and durable on
     * return (commit() semantics) so the migration durability barrier is meaningful."
     */
    @Override
    public void put(String key, String value) {
        encrypted.edit().putString(key, value).commit();
    }

    /**
     * Removes the entry for {@code key}.
     *
     * Uses commit() for durability consistency with put().
     */
    @Override
    public void remove(String key) {
        encrypted.edit().remove(key).commit();
    }

    /**
     * Returns true iff the store contains an entry for {@code key}.
     */
    @Override
    public boolean contains(String key) {
        return encrypted.contains(key);
    }

    /**
     * Removes all entries from the store.
     */
    @Override
    public void clear() {
        encrypted.edit().clear().commit();
    }
}
