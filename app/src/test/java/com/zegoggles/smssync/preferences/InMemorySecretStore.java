package com.zegoggles.smssync.preferences;

import java.util.HashMap;
import java.util.Map;

/**
 * Test-only in-memory implementation of SecretStore backed by a HashMap.
 *
 * This class has NO android.* or androidx.* dependencies, making it runnable on the
 * plain JVM in Robolectric unit tests without requiring a real Android Keystore.
 *
 * It is exempt from the cryptographic guarantee, backup-exclusion invariant, and
 * commit()-durability clauses of CNTR-MODERNIZATION-003 — those clauses apply to the
 * production adapter (EncryptedPrefsSecretStore) only.
 *
 * Usage: inject via {@code new AuthPreferences(context, new InMemorySecretStore())} in
 * unit tests to exercise AuthPreferences logic without real crypto infrastructure.
 *
 * IMPORTANT: This class is in src/test/ and MUST NOT appear in production builds.
 */
public class InMemorySecretStore implements SecretStore {

    private final Map<String, String> store = new HashMap<>();

    /**
     * Returns the value for {@code key}, or {@code null} if absent.
     */
    @Override
    public String get(String key) {
        return store.get(key);
    }

    /**
     * Stores {@code value} under {@code key}.
     */
    @Override
    public void put(String key, String value) {
        store.put(key, value);
    }

    /**
     * Removes the entry for {@code key}.
     */
    @Override
    public void remove(String key) {
        store.remove(key);
    }

    /**
     * Returns true iff the store contains an entry for {@code key}.
     */
    @Override
    public boolean contains(String key) {
        return store.containsKey(key);
    }

    /**
     * Removes all entries from the store.
     */
    @Override
    public void clear() {
        store.clear();
    }
}
