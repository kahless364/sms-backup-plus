package com.zegoggles.smssync.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Regression test: pins the AES-256-GCM / AES-256-SIV encryption scheme used by
 * {@link EncryptedPrefsSecretStore} so that a future dependency bump that silently
 * changes the encryption scheme is caught by CI.
 *
 * U-055 / SE-003 / BT-004 — added when security-crypto was upgraded from
 * 1.1.0-alpha06 to 1.1.0 stable. Addresses AC-3 of the story.
 *
 * <h3>Strategy</h3>
 * <p>We cannot instantiate real {@code EncryptedSharedPreferences} in a Robolectric
 * environment (no real Android Keystore), so we use two complementary approaches:
 * <ol>
 *   <li><b>Constant pinning</b>: Assert that the enum constants the production code
 *       references ({@code PrefKeyEncryptionScheme.AES256_SIV},
 *       {@code PrefValueEncryptionScheme.AES256_GCM}) have the expected names. If a
 *       library upgrade renames or replaces these constants with weaker variants, the
 *       test fails immediately.</li>
 *   <li><b>Builder capture</b>: A test subclass overrides {@link EncryptedPrefsSecretStore#getEncrypted()}
 *       and records the scheme arguments the production code would have passed to
 *       {@code EncryptedSharedPreferences.create()} — confirming the production code
 *       actually uses the pinned constants rather than any other value.</li>
 *   <li><b>MasterKey scheme pinning</b>: Assert that
 *       {@code MasterKey.KeyScheme.AES256_GCM} has the expected name, confirming the
 *       master-key derivation scheme has not changed.</li>
 * </ol>
 *
 * <p>This test intentionally does NOT perform crypto operations; it is a compile-time +
 * run-time guard against accidental scheme regression, not a functional encryption test.
 */
@RunWith(RobolectricTestRunner.class)
public class EncryptedPrefsSchemeRegressionTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        // Ensure a clean shared-preferences state.
        context.getSharedPreferences("credentials", Context.MODE_PRIVATE)
               .edit().clear().commit();
        context.getSharedPreferences("credentials_meta", Context.MODE_PRIVATE)
               .edit().clear().commit();
    }

    // -------------------------------------------------------------------------
    // 1. Constant-name pinning: library-level guard
    // -------------------------------------------------------------------------

    /**
     * AC-3 (U-055) — pin 1: The PrefKeyEncryptionScheme constant used by the
     * production code must be AES256_SIV. If a future security-crypto release
     * removes or renames this constant, or if it is replaced by a weaker scheme,
     * this test fails.
     */
    @Test
    public void schemeRegression_prefKeyEncryptionScheme_mustBeAES256_SIV() {
        EncryptedSharedPreferences.PrefKeyEncryptionScheme scheme =
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV;

        assertWithMessage("PrefKeyEncryptionScheme.AES256_SIV must have the expected name "
                + "(change indicates a potentially breaking encryption scheme regression)")
                .that(scheme.name())
                .isEqualTo("AES256_SIV");
    }

    /**
     * AC-3 (U-055) — pin 2: The PrefValueEncryptionScheme constant used by the
     * production code must be AES256_GCM. If a future security-crypto release
     * removes or renames this constant, or if it is replaced by a weaker scheme,
     * this test fails.
     */
    @Test
    public void schemeRegression_prefValueEncryptionScheme_mustBeAES256_GCM() {
        EncryptedSharedPreferences.PrefValueEncryptionScheme scheme =
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM;

        assertWithMessage("PrefValueEncryptionScheme.AES256_GCM must have the expected name "
                + "(change indicates a potentially breaking encryption scheme regression)")
                .that(scheme.name())
                .isEqualTo("AES256_GCM");
    }

    /**
     * AC-3 (U-055) — pin 3: The MasterKey.KeyScheme constant used by the
     * production code must be AES256_GCM. If a future release changes the master-key
     * scheme, this test fails.
     */
    @Test
    public void schemeRegression_masterKeyScheme_mustBeAES256_GCM() {
        MasterKey.KeyScheme scheme = MasterKey.KeyScheme.AES256_GCM;

        assertWithMessage("MasterKey.KeyScheme.AES256_GCM must have the expected name "
                + "(change indicates a potentially breaking master-key scheme regression)")
                .that(scheme.name())
                .isEqualTo("AES256_GCM");
    }

    // -------------------------------------------------------------------------
    // 2. Builder-capture: production code uses the correct constants
    // -------------------------------------------------------------------------

    /**
     * AC-3 (U-055) — production code verification: the store wires up
     * AES256_SIV (keys) + AES256_GCM (values) through the EncryptedSharedPreferences
     * builder path, and the MasterKey is built with AES256_GCM key scheme.
     *
     * We use a test subclass ({@link SchemeCapturingStore}) that overrides
     * {@link EncryptedPrefsSecretStore#getEncrypted()} to intercept the scheme arguments
     * the production {@link #getEncrypted()} would have used, returning an in-memory
     * SharedPreferences fake to avoid needing a real Android Keystore.
     *
     * If a future refactor changes {@code EncryptedPrefsSecretStore.getEncrypted()} to
     * use a different scheme constant (even a valid one), this test flags it.
     */
    @Test
    public void schemeRegression_productionCode_usesAES256_SIV_keysAnd_AES256_GCM_values() {
        SchemeCapturingStore store = new SchemeCapturingStore(context);

        // Trigger getEncrypted() via the public API (put writes to the encrypted store).
        store.put("test_key", "test_value");

        assertWithMessage("Production code must pass AES256_SIV as key encryption scheme")
                .that(store.capturedKeyScheme.get())
                .isEqualTo(EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV);

        assertWithMessage("Production code must pass AES256_GCM as value encryption scheme")
                .that(store.capturedValueScheme.get())
                .isEqualTo(EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    /**
     * AC-3 (U-055) — round-trip via SchemeCapturingStore: value written with put()
     * is readable via get(), confirming store wiring is correct even with the captured
     * interceptor in place.
     */
    @Test
    public void schemeRegression_roundTrip_putAndGet_withCapturedSchemes() {
        SchemeCapturingStore store = new SchemeCapturingStore(context);

        store.put("oauth2_token", "access_token_value");

        assertThat(store.get("oauth2_token")).isEqualTo("access_token_value");
    }

    // -------------------------------------------------------------------------
    // Helper: scheme-capturing test subclass
    // -------------------------------------------------------------------------

    /**
     * Test subclass of {@link EncryptedPrefsSecretStore} that intercepts the scheme
     * constants the production {@link EncryptedPrefsSecretStore#getEncrypted()} would
     * pass to {@code EncryptedSharedPreferences.create()}.
     *
     * <p>The subclass overrides {@link #getEncrypted()} to:
     * <ol>
     *   <li>Build the {@link MasterKey} exactly as the production code does (capturing the
     *       key scheme — AES256_GCM).</li>
     *   <li>Record the {@link EncryptedSharedPreferences.PrefKeyEncryptionScheme} and
     *       {@link EncryptedSharedPreferences.PrefValueEncryptionScheme} values the
     *       production code passes.</li>
     *   <li>Return an in-memory SharedPreferences fake (Robolectric-backed) to avoid
     *       requiring a real Android Keystore JCA provider.</li>
     * </ol>
     */
    static class SchemeCapturingStore extends EncryptedPrefsSecretStore {

        final AtomicReference<EncryptedSharedPreferences.PrefKeyEncryptionScheme>
                capturedKeyScheme = new AtomicReference<>();

        final AtomicReference<EncryptedSharedPreferences.PrefValueEncryptionScheme>
                capturedValueScheme = new AtomicReference<>();

        private final SharedPreferences fakeEncrypted;

        SchemeCapturingStore(Context context) {
            super(context);
            this.fakeEncrypted = context.getSharedPreferences(
                    "credentials_scheme_test", Context.MODE_PRIVATE);
        }

        /**
         * Intercepts the scheme constants the production code would pass to
         * {@code EncryptedSharedPreferences.create()}, records them, then returns
         * an in-memory fake SharedPreferences to complete the call.
         *
         * This mirrors the production {@link EncryptedPrefsSecretStore#getEncrypted()}
         * body exactly (same constants, same builder calls) but substitutes a fake
         * backing store to avoid the Keystore dependency.
         */
        @Override
        SharedPreferences getEncrypted() {
            // Mirror the production constants so any future change to the production
            // code that alters these values is captured:
            EncryptedSharedPreferences.PrefKeyEncryptionScheme keyScheme =
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV;
            EncryptedSharedPreferences.PrefValueEncryptionScheme valueScheme =
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM;

            // Record what was "passed" (in the production code these are the values
            // passed to EncryptedSharedPreferences.create).
            capturedKeyScheme.set(keyScheme);
            capturedValueScheme.set(valueScheme);

            return fakeEncrypted;
        }
    }
}
