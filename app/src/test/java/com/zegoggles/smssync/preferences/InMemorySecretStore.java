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
 * Two-map design for migrateFromPlaintext():
 * - {@code legacyStore} simulates the pre-migration plaintext SharedPreferences("credentials").
 *   Tests pre-seed this map to simulate existing plaintext credentials on an upgrading device.
 * - {@code store} simulates the encrypted store (the post-migration target).
 * Both maps start empty. Tests access {@code legacyStore} directly to seed plaintext and
 * to verify it is cleared after migration.
 *
 * Usage: inject via {@code new AuthPreferences(context, new InMemorySecretStore())} in
 * unit tests to exercise AuthPreferences logic without real crypto infrastructure.
 *
 * IMPORTANT: This class is in src/test/ and MUST NOT appear in production builds.
 */
public class InMemorySecretStore implements SecretStore {

    /** Simulates the encrypted/post-migration store (the primary store for get/put/etc.). */
    private final Map<String, String> store = new HashMap<>();

    /**
     * Simulates the legacy plaintext SharedPreferences("credentials") backing map.
     * Tests pre-seed this map to simulate existing plaintext credentials present before migration.
     * After a successful {@link #migrateFromPlaintext()}, the three credential keys are removed
     * from this map (step 4 of the rollback-safe ordering).
     *
     * Exposed as package-private so test code in the same package can pre-seed it directly:
     * {@code secretStore.legacyStore.put("login_password", "app-password");}
     */
    final Map<String, String> legacyStore = new HashMap<>();

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

    /**
     * One-time, idempotent, rollback-safe migration from the legacy plaintext backing map
     * ({@link #legacyStore}) to the encrypted backing map ({@link #store}).
     *
     * Follows the exact rollback-safe step ordering from CNTR-MODERNIZATION-003:
     * 1. If {@code __secretstore_migration_complete__} is in {@code store}, return immediately.
     * 2. Buffer the three credential values from {@code legacyStore} into local variables.
     * 3. Write each present value to {@code store} plus the marker (simulates durable commit).
     * 4. Only after step 3: remove the three keys from {@code legacyStore}.
     *
     * This implementation is exempt from the crypto and backup-exclusion clauses of
     * CNTR-MODERNIZATION-003 — those apply to EncryptedPrefsSecretStore only.
     */
    @Override
    public void migrateFromPlaintext() {
        // Step 1: idempotent short-circuit — if migration already completed, return immediately.
        if (store.containsKey("__secretstore_migration_complete__")) {
            return;
        }

        // Step 2: buffer legacy plaintext values in memory (read before any write).
        String pw = legacyStore.get("login_password");
        String at = legacyStore.get("oauth2_token");
        String rt = legacyStore.get("oauth2_refresh_token");

        // Step 3: write each present value to the encrypted store, then write the marker.
        // The "commit()" durability barrier is a no-op in the in-memory fake, but the
        // ordering (all writes before the clear in step 4) is preserved.
        if (pw != null) store.put("login_password", pw);
        if (at != null) store.put("oauth2_token", at);
        if (rt != null) store.put("oauth2_refresh_token", rt);
        store.put("__secretstore_migration_complete__", "1");
        // (durability barrier — synchronous commit semantics; no-op for in-memory map)

        // Step 4: ONLY after step 3 is complete, clear the legacy plaintext entries.
        legacyStore.remove("login_password");
        legacyStore.remove("oauth2_token");
        legacyStore.remove("oauth2_refresh_token");
    }
}
