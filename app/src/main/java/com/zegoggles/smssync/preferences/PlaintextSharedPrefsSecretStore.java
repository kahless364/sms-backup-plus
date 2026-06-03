package com.zegoggles.smssync.preferences;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Fallback SecretStore backed by plain (unencrypted) SharedPreferences.
 *
 * This class exists ONLY to handle the case where EncryptedPrefsSecretStore cannot be
 * constructed — which happens in Robolectric unit-test environments where the Android
 * Keystore JCA provider is not available (Robolectric 4.12.x limitation, AC-11).
 *
 * SECURITY NOTE: This class provides NO encryption. It MUST NOT be used as the primary
 * SecretStore in production. On a real Android device, EncryptedPrefsSecretStore is
 * always constructed successfully and this class is never instantiated.
 *
 * The reason this is NOT InMemorySecretStore is that InMemorySecretStore lives in the
 * test source set (src/test/) and cannot be referenced from production src/main/ code.
 * This class lives in src/main/ so it can be referenced as a fallback in the
 * single-arg AuthPreferences constructor without pulling a test class into production.
 *
 * File name: "credentials_fallback" — intentionally different from "credentials" to
 * avoid interfering with the EncryptedPrefsSecretStore's backing file.
 *
 * This class is exempt from the cryptographic guarantee and backup-exclusion invariant
 * of CNTR-MODERNIZATION-003 — those clauses apply to EncryptedPrefsSecretStore only.
 *
 * @deprecated Use EncryptedPrefsSecretStore for all production code.
 */
@Deprecated
class PlaintextSharedPrefsSecretStore implements SecretStore {

    private static final String FALLBACK_FILE_NAME = "credentials_fallback";

    private final SharedPreferences prefs;

    PlaintextSharedPrefsSecretStore(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(FALLBACK_FILE_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public String get(String key) {
        return prefs.getString(key, null);
    }

    @Override
    public void put(String key, String value) {
        prefs.edit().putString(key, value).commit();
    }

    @Override
    public void remove(String key) {
        prefs.edit().remove(key).commit();
    }

    @Override
    public boolean contains(String key) {
        return prefs.contains(key);
    }

    @Override
    public void clear() {
        prefs.edit().clear().commit();
    }

    /**
     * No-op: this fallback implementation is used only in Robolectric test environments
     * where the Android Keystore is unavailable. In those environments there are no real
     * plaintext credentials to migrate, so this method is a safe no-op.
     *
     * On a real device, EncryptedPrefsSecretStore is always used and this method is
     * never called.
     */
    @Override
    public void migrateFromPlaintext() {
        // No-op: the fallback store is used only under Robolectric (no real Keystore).
        // There are no plaintext credentials to migrate in that environment.
    }
}
