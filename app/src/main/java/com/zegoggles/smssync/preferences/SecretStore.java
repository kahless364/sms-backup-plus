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
}
