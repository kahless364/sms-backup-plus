package com.zegoggles.smssync.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Unit tests for the degraded-security path introduced in U-040 (BUG-008).
 *
 * Tests that when EncryptedPrefsSecretStore.getEncrypted() throws during
 * migrateFromPlaintext():
 *  (a) the ENCRYPTION_DEGRADED_KEY flag is set in plaintext SharedPreferences,
 *  (b) MIGRATION_COMPLETE_KEY is NOT written (so a later launch retries),
 *  (c) a subsequent successful migration clears the degraded flag and completes migration.
 *
 * Uses a package-private test subclass (ThrowingEncryptedPrefsSecretStore) that overrides
 * the package-private getEncrypted() method to simulate Keystore failure without requiring
 * a real Android Keystore (which Robolectric 4.12.x does not shadow).
 *
 * U-040 / BUG-008 acceptance criteria:
 *  AC-1: failure is surfaced as a persisted degraded-security flag.
 *  AC-2: MIGRATION_COMPLETE_KEY not written on failure; later success completes migration.
 *  AC-3: off-main-thread (no blocking calls introduced here — Robolectric test thread).
 *  AC-4: this test covers the failure path.
 */
@RunWith(RobolectricTestRunner.class)
public class EncryptedPrefsSecretStoreDegradedTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        // Clear the meta prefs file between tests.
        context.getSharedPreferences("credentials_meta", Context.MODE_PRIVATE)
               .edit().clear().commit();
        // Clear the legacy credentials prefs file between tests.
        context.getSharedPreferences("credentials", Context.MODE_PRIVATE)
               .edit().clear().commit();
    }

    @After
    public void tearDown() {
        context.getSharedPreferences("credentials_meta", Context.MODE_PRIVATE)
               .edit().clear().commit();
        context.getSharedPreferences("credentials", Context.MODE_PRIVATE)
               .edit().clear().commit();
    }

    // -------------------------------------------------------------------------
    // Helper: subclass with always-throwing getEncrypted()
    // -------------------------------------------------------------------------

    /**
     * Test subclass that always throws from getEncrypted() to simulate Keystore failure.
     * The constructor is trivial (just passes context up), and getEncrypted() is overridden
     * to simulate a permanent Keystore failure on this instance.
     */
    static class AlwaysThrowingEncryptedPrefsSecretStore extends EncryptedPrefsSecretStore {
        AlwaysThrowingEncryptedPrefsSecretStore(Context context) {
            super(context);
        }

        @Override
        SharedPreferences getEncrypted() {
            throw new RuntimeException("simulated Keystore failure for U-040 test");
        }
    }

    /**
     * Test subclass that succeeds on getEncrypted() by returning an in-memory
     * SharedPreferences-backed implementation suitable for Robolectric.
     * Used to simulate "Keystore recovered on second launch".
     */
    static class SucceedingEncryptedPrefsSecretStore extends EncryptedPrefsSecretStore {
        private final SharedPreferences fakeEncrypted;

        SucceedingEncryptedPrefsSecretStore(Context context) {
            super(context);
            // Use a fresh in-memory (Robolectric-backed) SharedPreferences as the
            // encrypted store, stored under a test-only file name.
            this.fakeEncrypted = context.getSharedPreferences(
                    "credentials_test_encrypted", Context.MODE_PRIVATE);
        }

        @Override
        SharedPreferences getEncrypted() {
            return fakeEncrypted;
        }
    }

    // -------------------------------------------------------------------------
    // AC-1: degraded flag is set when getEncrypted() throws
    // -------------------------------------------------------------------------

    /**
     * AC-1 (U-040): When getEncrypted() throws during migrateFromPlaintext(), the
     * ENCRYPTION_DEGRADED_KEY flag is set in plaintext SharedPreferences.
     *
     * isEncryptionDegraded() must return true after the failure.
     */
    @Test
    public void u040_ac1_keystoreFailure_setsDegradedFlag() {
        // Arrange: legacy credentials present (simulating an upgrading device).
        context.getSharedPreferences("credentials", Context.MODE_PRIVATE)
               .edit()
               .putString("oauth2_token", "tok")
               .putString("oauth2_refresh_token", "ref")
               .commit();

        AlwaysThrowingEncryptedPrefsSecretStore store =
                new AlwaysThrowingEncryptedPrefsSecretStore(context);

        // Act: migration attempt fails (Keystore throws).
        store.migrateFromPlaintext();

        // Assert: degraded flag is set (AC-1).
        assertThat(store.isEncryptionDegraded()).isTrue();
    }

    // -------------------------------------------------------------------------
    // AC-2a: MIGRATION_COMPLETE_KEY is NOT written on failure
    // -------------------------------------------------------------------------

    /**
     * AC-2 (U-040, part a): When getEncrypted() throws, MIGRATION_COMPLETE_KEY is NOT
     * written. The migration will be retried on the next launch.
     *
     * We verify via the SucceedingEncryptedPrefsSecretStore: a second store instance (same
     * context, succeeding getEncrypted()) will NOT short-circuit at the idempotency check
     * because the marker was never written.
     */
    @Test
    public void u040_ac2a_keystoreFailure_doesNotWriteMigrationCompleteKey() {
        // Arrange: legacy credentials present.
        context.getSharedPreferences("credentials", Context.MODE_PRIVATE)
               .edit()
               .putString("login_password", "pw")
               .commit();

        AlwaysThrowingEncryptedPrefsSecretStore failingStore =
                new AlwaysThrowingEncryptedPrefsSecretStore(context);

        // Act: first migration attempt fails.
        failingStore.migrateFromPlaintext();

        // Assert: flag is set (as per AC-1), and no MIGRATION_COMPLETE_KEY in meta prefs.
        // The degraded store's fake encrypted store was never opened, so no marker was written.
        assertThat(failingStore.isEncryptionDegraded()).isTrue();

        // Use a succeeding store to verify: if we migrate now, it should NOT short-circuit.
        // If MIGRATION_COMPLETE_KEY had been written, migrateFromPlaintext() would return early
        // and legacy plaintext would not be cleared. We verify the migration runs by checking
        // the credentials are readable after the successful run.
        context.getSharedPreferences("credentials_test_encrypted", Context.MODE_PRIVATE)
               .edit().clear().commit();
        SucceedingEncryptedPrefsSecretStore successStore =
                new SucceedingEncryptedPrefsSecretStore(context);
        successStore.migrateFromPlaintext();

        // Migration ran (not skipped): credentials should be in the fake encrypted store.
        assertThat(successStore.get("login_password")).isEqualTo("pw");
        // Marker must now be written after the successful run.
        assertThat(successStore.contains(EncryptedPrefsSecretStore.MIGRATION_COMPLETE_KEY)).isTrue();
    }

    // -------------------------------------------------------------------------
    // AC-2b: later success completes migration and clears the degraded flag
    // -------------------------------------------------------------------------

    /**
     * AC-2 (U-040, part b): A subsequent successful migration (Keystore recovered) clears
     * the ENCRYPTION_DEGRADED_KEY flag and writes MIGRATION_COMPLETE_KEY.
     *
     * Simulates:
     *  - Launch 1: getEncrypted() throws → degraded flag set, migration NOT complete.
     *  - Launch 2: getEncrypted() succeeds → migration completes, degraded flag cleared.
     */
    @Test
    public void u040_ac2b_subsequentSuccess_completesMigrationAndClearsDegradedFlag() {
        // Arrange: legacy credentials present.
        context.getSharedPreferences("credentials", Context.MODE_PRIVATE)
               .edit()
               .putString("oauth2_token", "at")
               .putString("oauth2_refresh_token", "rt")
               .commit();
        // Clear the fake encrypted store from any prior test run.
        context.getSharedPreferences("credentials_test_encrypted", Context.MODE_PRIVATE)
               .edit().clear().commit();

        // Launch 1: Keystore fails.
        AlwaysThrowingEncryptedPrefsSecretStore failingStore =
                new AlwaysThrowingEncryptedPrefsSecretStore(context);
        failingStore.migrateFromPlaintext();
        assertThat(failingStore.isEncryptionDegraded()).isTrue();   // degraded after failure

        // Launch 2: Keystore recovers — use the succeeding store.
        SucceedingEncryptedPrefsSecretStore successStore =
                new SucceedingEncryptedPrefsSecretStore(context);
        successStore.migrateFromPlaintext();

        // (a) Migration succeeded: credentials readable via encrypted store.
        assertThat(successStore.get("oauth2_token")).isEqualTo("at");
        assertThat(successStore.get("oauth2_refresh_token")).isEqualTo("rt");

        // (b) Migration complete marker written (AC-2 / U-012 invariant preserved).
        assertThat(successStore.contains(EncryptedPrefsSecretStore.MIGRATION_COMPLETE_KEY)).isTrue();

        // (c) Degraded flag cleared after success (AC-1 recovery path).
        assertThat(successStore.isEncryptionDegraded()).isFalse();
    }

    // -------------------------------------------------------------------------
    // AC-2c: isEncryptionDegraded() returns false when migration succeeds on first attempt
    // -------------------------------------------------------------------------

    /**
     * No-regression: isEncryptionDegraded() returns false when migration succeeds on the
     * first attempt (no Keystore failure, no degraded flag set).
     */
    @Test
    public void u040_noRegression_successOnFirstAttempt_notDegraded() {
        // Arrange: legacy credentials present, succeeding store.
        context.getSharedPreferences("credentials", Context.MODE_PRIVATE)
               .edit()
               .putString("login_password", "pw2")
               .commit();
        context.getSharedPreferences("credentials_test_encrypted", Context.MODE_PRIVATE)
               .edit().clear().commit();

        SucceedingEncryptedPrefsSecretStore successStore =
                new SucceedingEncryptedPrefsSecretStore(context);

        // Act: migration succeeds.
        successStore.migrateFromPlaintext();

        // Assert: not degraded, migration complete.
        assertThat(successStore.isEncryptionDegraded()).isFalse();
        assertThat(successStore.contains(EncryptedPrefsSecretStore.MIGRATION_COMPLETE_KEY)).isTrue();
    }
}
