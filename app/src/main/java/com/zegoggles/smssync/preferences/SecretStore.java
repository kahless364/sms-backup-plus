package com.zegoggles.smssync.preferences;

/**
 * Port interface for credential storage.
 *
 * Abstracts the credential persistence mechanism so that the backing store can be
 * swapped (from plaintext SharedPreferences to EncryptedSharedPreferences, or to a
 * test fake) without changing any caller.
 *
 * This interface is intentionally kept free of android.* and androidx.* imports so it
 * can be implemented by a plain-JVM fake (InMemorySecretStore) usable in Robolectric
 * unit tests without requiring a real Android Keystore.
 *
 * Contract: CNTR-MODERNIZATION-003
 * Design: DES-MODERNIZATION-004
 * Requirement: REQ-MODERNIZATION-004 (MU-004, CWE-312)
 */
public interface SecretStore {

    /**
     * Returns the decrypted plaintext value for the given key, or {@code null} if
     * the key is absent or the value cannot be decrypted.
     * This method MUST NOT throw; decrypt failures are surfaced as {@code null}.
     */
    String get(String key);

    /**
     * Encrypts and persists the given value synchronously.
     * Implementations MUST use commit() semantics (synchronous and durable on return),
     * not apply() — required so the migration durability barrier in U-012 is meaningful.
     */
    void put(String key, String value);

    /**
     * Removes the entry for the given key synchronously.
     */
    void remove(String key);

    /**
     * Returns true iff the store contains an entry for the given key.
     */
    boolean contains(String key);

    /**
     * Removes all entries from the store.
     */
    void clear();

    /**
     * One-time, idempotent, rollback-safe migration of legacy plaintext credentials
     * to the encrypted store.
     *
     * Implementations MUST follow the rollback-safe step ordering defined in
     * CNTR-MODERNIZATION-003 §Validation Rules §One-time migration semantics:
     *
     * 1. If the {@code __secretstore_migration_complete__} marker is present, return immediately.
     * 2. Read the three legacy plaintext values ({@code login_password}, {@code oauth2_token},
     *    {@code oauth2_refresh_token}) into memory.
     * 3. Write each present value as ciphertext, then write the marker, then commit
     *    synchronously (the durability barrier).
     * 4. ONLY after step-3 commit succeeds: clear the legacy plaintext entries.
     *
     * Safety invariant: plaintext is NEVER cleared until ciphertext is durably committed.
     * A process kill between step 3 and step 4 leaves the marker in the encrypted store,
     * so the next launch short-circuits at step 1 — the user is never unable to authenticate.
     *
     * Contract: CNTR-MODERNIZATION-003 §Migration entry point
     */
    void migrateFromPlaintext();

    /**
     * Returns {@code true} if the Keystore/EncryptedSharedPreferences store was unavailable
     * during the last migration attempt, leaving credentials stored in plaintext (U-040/BUG-008).
     *
     * <p>The default implementation returns {@code false} — implementations that do not
     * support Keystore-backed encryption (e.g. in-memory test fakes) are never in the degraded
     * state. Only {@link EncryptedPrefsSecretStore} overrides this to return the persisted flag.
     *
     * <p>U-054 (SE-002): callers (e.g. WorkManagerScheduler, MainViewModel) gate backup
     * operations and surface a user-visible warning when this returns {@code true}.
     *
     * @return {@code true} iff credentials may still be in plaintext due to a prior Keystore
     *         failure; {@code false} otherwise.
     */
    default boolean isEncryptionDegraded() {
        return false;
    }
}
