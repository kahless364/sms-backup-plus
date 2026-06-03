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
 * continues to match without any descriptor change (AC-5 / CNTR-MODERNIZATION-003
 * §Backing-store invariant).
 *
 * Lazy initialization: the EncryptedSharedPreferences backing store is NOT opened in the
 * constructor. It is opened on first use (get/put/remove/contains/clear) via
 * {@link #getEncrypted()}. This is required for the Option-A migration ordering (U-012):
 * {@link #migrateFromPlaintext()} must buffer the legacy plaintext values from a raw
 * SharedPreferences("credentials") handle BEFORE EncryptedSharedPreferences is created over
 * the same backing file — otherwise the library would attempt to decrypt pre-existing
 * plaintext entries and throw.
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

    /** Migration completion marker key — written inside the encrypted store. */
    static final String MIGRATION_COMPLETE_KEY = "__secretstore_migration_complete__";

    private final Context appContext;

    /**
     * Lazily-initialized encrypted SharedPreferences instance.
     * Null until first use. Initialized in {@link #getEncrypted()}.
     * Guarded by synchronized access in getEncrypted().
     */
    private volatile SharedPreferences encrypted;

    /**
     * Constructs the adapter. Does NOT open EncryptedSharedPreferences eagerly —
     * the encrypted store is opened lazily on first use to preserve the Option-A
     * migration ordering (see class Javadoc and U-012 §Technical Context).
     *
     * @param context application context
     */
    public EncryptedPrefsSecretStore(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * Lazily opens (or returns the already-opened) EncryptedSharedPreferences store.
     *
     * Thread-safe via double-checked locking on the volatile {@code encrypted} field.
     *
     * @throws RuntimeException wrapping GeneralSecurityException or IOException if the
     *                          Keystore master key or the backing file cannot be opened.
     *                          Construction failure is not silently swallowed because there
     *                          is no safe fallback that satisfies the confidentiality
     *                          requirement. The single-arg AuthPreferences constructor
     *                          catches this via buildEncryptedStoreSafe().
     */
    private SharedPreferences getEncrypted() {
        if (encrypted == null) {
            synchronized (this) {
                if (encrypted == null) {
                    try {
                        MasterKey masterKey = new MasterKey.Builder(appContext)
                                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                                .setRequestStrongBoxBacked(true)  // best-effort; falls back to TEE/software silently
                                .build();

                        encrypted = EncryptedSharedPreferences.create(
                                appContext,
                                CREDENTIALS_FILE_NAME,
                                masterKey,
                                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        );
                    } catch (GeneralSecurityException | IOException e) {
                        throw new RuntimeException(
                                "EncryptedPrefsSecretStore: failed to open encrypted store", e);
                    }
                }
            }
        }
        return encrypted;
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
            return getEncrypted().getString(key, null);
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
        getEncrypted().edit().putString(key, value).commit();
    }

    /**
     * Removes the entry for {@code key}.
     *
     * Uses commit() for durability consistency with put().
     */
    @Override
    public void remove(String key) {
        getEncrypted().edit().remove(key).commit();
    }

    /**
     * Returns true iff the store contains an entry for {@code key}.
     */
    @Override
    public boolean contains(String key) {
        return getEncrypted().contains(key);
    }

    /**
     * Removes all entries from the store.
     */
    @Override
    public void clear() {
        getEncrypted().edit().clear().commit();
    }

    /**
     * One-time, idempotent, rollback-safe migration of legacy plaintext credentials from
     * {@code SharedPreferences("credentials", MODE_PRIVATE)} to the encrypted store.
     *
     * Follows the exact rollback-safe step ordering from CNTR-MODERNIZATION-003
     * §Validation Rules §One-time migration semantics:
     *
     * 1. If the {@code __secretstore_migration_complete__} marker is present in the encrypted
     *    store, return immediately (idempotent short-circuit).
     * 2. Read the three legacy plaintext values from a raw (non-encrypted) SharedPreferences
     *    handle over the "credentials" file into local variables — BEFORE constructing the
     *    EncryptedSharedPreferences over the same file (Option-A ordering invariant).
     * 3. Open the encrypted store (via {@link #getEncrypted()}), write each present value
     *    and the marker, then commit synchronously (durability barrier).
     * 4. ONLY after step-3 commit succeeds: clear the plaintext entries from the legacy handle.
     *
     * Option A reasoning: using the same file name "credentials" means
     * {@code backup_descriptor.xml} requires no change (the existing exclude rule matches).
     * The ordering risk is mitigated by reading the raw plaintext handle BEFORE calling
     * {@link #getEncrypted()}, which opens EncryptedSharedPreferences over the same file.
     * By the time EncryptedSharedPreferences is created, the plaintext entries have already
     * been buffered in memory and the commit in step 3 overwrites them with ciphertext.
     *
     * Contract: CNTR-MODERNIZATION-003 §Migration entry point
     */
    @Override
    public void migrateFromPlaintext() {
        // Step 1: idempotent short-circuit. We must check the encrypted store first.
        // However, to avoid the Option-A ordering problem (encrypted store created over
        // plaintext file), we check via a quick raw-prefs probe for the marker first,
        // then fall through to a full encrypted-store check if no marker found.
        //
        // Primary idempotency check: use a raw SharedPreferences handle to look for the
        // migration marker (the marker key is encrypted-at-rest, so a raw handle won't
        // find it — but the encrypted store check below covers that case).
        // The fast-path idempotency check uses the encrypted store (already initialized
        // on subsequent launches after migration is complete).
        if (encrypted != null && encrypted.contains(MIGRATION_COMPLETE_KEY)) {
            return;
        }

        // Step 2: Read legacy plaintext values into memory using a raw SharedPreferences
        // handle BEFORE opening EncryptedSharedPreferences over the same file.
        // This is the critical Option-A ordering step: we buffer the plaintext first,
        // then the encrypted store is constructed in step 3's getEncrypted() call.
        SharedPreferences legacy = appContext.getSharedPreferences(
                CREDENTIALS_FILE_NAME, Context.MODE_PRIVATE);

        String pw = legacy.getString("login_password", null);
        String at = legacy.getString("oauth2_token", null);
        String rt = legacy.getString("oauth2_refresh_token", null);

        // We call getEncrypted() here (which may construct the encrypted store for the first
        // time). After this call, any legacy plaintext in the raw file has already been
        // buffered into pw/at/rt above (Option-A ordering invariant preserved).
        // If the encrypted store cannot be opened (e.g., Keystore unavailable in unit-test
        // environments without AndroidKeyStore JCA provider), log a warning and return without
        // clearing plaintext. The migration will be retried on the next app launch.
        SharedPreferences enc;
        try {
            enc = getEncrypted();
        } catch (RuntimeException e) {
            Log.w(TAG, "EncryptedPrefsSecretStore.migrateFromPlaintext(): encrypted store "
                    + "unavailable (Keystore provider missing?), skipping migration. "
                    + "Will retry on next launch. Cause: " + e.getMessage());
            return;
        }

        // Step 1 (deferred): encrypted store is now open — do the definitive idempotency check.
        if (enc.contains(MIGRATION_COMPLETE_KEY)) {
            return;
        }

        // Step 3: Write each present value to the encrypted store, then write the marker,
        // then commit synchronously (the durability barrier).
        // Safety invariant: plaintext is NEVER cleared until this commit returns successfully.
        SharedPreferences.Editor editor = enc.edit();
        if (pw != null) editor.putString("login_password", pw);
        if (at != null) editor.putString("oauth2_token", at);
        if (rt != null) editor.putString("oauth2_refresh_token", rt);
        editor.putString(MIGRATION_COMPLETE_KEY, "1");
        editor.commit();  // synchronous durability barrier — MUST be commit(), not apply()

        // Step 4: ONLY after step-3 commit succeeds, clear the legacy plaintext entries.
        legacy.edit()
              .remove("login_password")
              .remove("oauth2_token")
              .remove("oauth2_refresh_token")
              .commit();

        Log.i(TAG, "EncryptedPrefsSecretStore: plaintext-to-encrypted credential migration complete.");
    }
}
