package com.zegoggles.smssync.preferences;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.fsck.k9.mail.AuthType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(RobolectricTestRunner.class)
public class AuthPreferencesTest {
    private AuthPreferences authPreferences;
    private SharedPreferences prefs;
    // U-011: All AuthPreferences tests now inject InMemorySecretStore so they run
    // on the plain JVM without requiring a real Android Keystore.
    private InMemorySecretStore secretStore;

    @Before public void before() {
        openMocks(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application);
        // Clear any preference state left by previous tests to prevent ordering dependency.
        prefs.edit().clear().commit();
        secretStore = new InMemorySecretStore();
        // U-011: Use the two-arg constructor (AC-8) to inject the test fake.
        authPreferences = new AuthPreferences(RuntimeEnvironment.application, secretStore);
    }

    @Test public void testStoreUri() throws Exception {
        prefs.edit()
            .putString("server_address", "foo.com:993")
            .putString("server_protocol", "+ssl+")
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.setImapUser("a:user");
        authPreferences.setImapPassword("password:has:colons");
        assertThat(authPreferences.getStoreUri()).isEqualTo("imap+ssl+://PLAIN:a%253Auser:password%253Ahas%253Acolons@foo.com:993");
    }

    @Test public void testStoreUriWithXOAuth2() throws Exception {
        prefs.edit()
            .putString("server_address", "imap.gmail.com:993")
            .putString("server_protocol", "+ssl+")
            .putString("server_authentication", "xoauth")
            .commit();

        authPreferences.setOauth2Token("user", "token", null);

        assertThat(authPreferences.getStoreUri()).isEqualTo("imap+ssl+://XOAUTH2:user:dXNlcj11c2VyAWF1dGg9QmVhcmVyIHRva2VuAQE%253D@imap.gmail.com:993");
    }

    // -------------------------------------------------------------------------
    // U-006 characterization tests for AuthPreferences.migrate()
    // These tests now reflect the target state delivered by U-007 (DES-MODERNIZATION-002).
    // -------------------------------------------------------------------------

    /**
     * AC-1 (U-007): migrate() must NOT silently enable SERVER_TRUST_ALL_CERTIFICATES.
     * Previously @Ignore-d as AUTHORED RED in U-006 to document the target state.
     * @Ignore removed in U-007 — this is now the passing acceptance criterion.
     *
     * Covers: legacy +ssl protocol, non-XOAUTH user.
     */
    @Test public void migrate_legacySslTls_doesNotEnableTrustAll() {
        // Setup: simulate a legacy +ssl account (non-XOAUTH) that triggers the migrate() branch.
        // The old migrate() at AuthPreferences.java:281-287 would fire when protocol is "+ssl" or "+tls"
        // and silently write trust_all=true. The new migrate() must NOT do this.
        prefs.edit()
            .putString("server_protocol", "+ssl")
            .putString("server_authentication", "plain")  // non-XOAUTH so early-return is not hit
            .commit();

        authPreferences.migrate();

        // After migration, trust-all must NOT be silently enabled (AC-1 / DES-002 Decision 6).
        assertThat(authPreferences.isTrustAllCertificates()).isFalse();
    }

    /**
     * AC-9 / U-006-AC-2: documents that a clean-state account (never through legacy +ssl/+tls
     * path, no stale trust_all) never receives a trust-all grant from migrate().
     */
    @Test public void migrate_cleanState_doesNotEnableTrustAll() {
        // Setup: default +ssl+ protocol (the "+" suffix distinguishes new-style from legacy).
        // migrate() only fires on "+ssl" or "+tls" (without trailing "+"), so this path is a no-op.
        prefs.edit()
            .putString("server_protocol", "+ssl+")  // new-style protocol — migrate() ignores this
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.migrate();

        // No server_trust_all_certificates key is written — isTrustAllCertificates() defaults false.
        assertThat(authPreferences.isTrustAllCertificates()).isFalse();
        // No notice flag set for unaffected users (AC-4 / U-007).
        assertThat(prefs.getBoolean(AuthPreferences.TRANSPORT_SECURITY_NOTICE_PENDING, false)).isFalse();
    }

    /**
     * AC-2 (U-007): stale SERVER_TRUST_ALL_CERTIFICATES=true (set by old migrate()) is
     * actively cleared to false by the new migrate().
     *
     * Updated from U-006 characterization: that test asserted isTrue() to document the OLD
     * behavior where migrate() preserved explicit trust-all. The new behavior (DES-002 Decision 6)
     * actively clears any stale true to close the MITM exposure on first launch of the fixed build.
     * The protocol used here (+ssl+) does not trigger the legacy-protocol branch, but the stale
     * trust_all=true still triggers the clearing branch.
     */
    @Test public void migrate_explicitTrustAll_isPreserved() {
        // Setup: device has SERVER_TRUST_ALL_CERTIFICATES=true (stale, written by old migrate()),
        // with +ssl+ protocol (new-style — does NOT trigger the legacy-protocol branch).
        // The new migrate() detects the stale true and clears it to false, restoring validated TLS.
        prefs.edit()
            .putBoolean("server_trust_all_certificates", true)
            .putString("server_protocol", "+ssl+")  // new-style — does not trigger +ssl/+tls branch
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.migrate();

        // DES-002 Decision 6 / AC-2: stale true is cleared to false, restoring validated TLS.
        // The exposure closes on first launch of the fixed build.
        assertThat(authPreferences.isTrustAllCertificates()).isFalse();
        // AC-3: notice pending set because stale trust_all=true was found and cleared.
        assertThat(prefs.getBoolean(AuthPreferences.TRANSPORT_SECURITY_NOTICE_PENDING, false)).isTrue();
    }

    // -------------------------------------------------------------------------
    // U-007 new tests — comprehensive coverage for the rewritten migrate()
    // -------------------------------------------------------------------------

    /**
     * AC-1 (U-007): Parametrized over +tls legacy protocol. Mirrors the +ssl case above.
     */
    @Test public void migrate_legacyTls_doesNotEnableTrustAll() {
        prefs.edit()
            .putString("server_protocol", "+tls")
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.migrate();

        assertThat(authPreferences.isTrustAllCertificates()).isFalse();
    }

    /**
     * AC-1 (U-007): trust_all is never written true for any non-legacy protocol value.
     * Covers: "ssl", "tls", "", "starttls", null (no stored value).
     */
    @Test public void migrate_nonLegacyProtocols_neverEnableTrustAll() {
        List<String> protocols = Arrays.asList("ssl", "tls", "", "starttls");
        for (String protocol : protocols) {
            // Reset state between iterations.
            prefs.edit().clear().commit();

            prefs.edit()
                .putString("server_protocol", protocol)
                .putString("server_authentication", "plain")
                .commit();

            authPreferences.migrate();

            assertWithMessage("protocol=" + protocol + " must not enable trust-all")
                .that(authPreferences.isTrustAllCertificates())
                .isFalse();
        }

        // Also test null/absent (no stored value — falls back to default "+ssl+").
        prefs.edit().clear().commit();
        prefs.edit().putString("server_authentication", "plain").commit();
        authPreferences.migrate();
        assertWithMessage("protocol=null/absent must not enable trust-all")
            .that(authPreferences.isTrustAllCertificates())
            .isFalse();
    }

    /**
     * AC-2 (U-007): Stale SERVER_TRUST_ALL_CERTIFICATES=true (written by old migrate()) is
     * actively cleared to false regardless of current SERVER_PROTOCOL value.
     */
    @Test public void migrate_staleTrustAll_isClearedToFalse() {
        prefs.edit()
            .putBoolean("server_trust_all_certificates", true)
            .putString("server_protocol", "+ssl+")  // new-style protocol
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.migrate();

        assertThat(authPreferences.isTrustAllCertificates()).isFalse();
    }

    /**
     * AC-3 (U-007): transport_security_notice_pending is set for legacy +ssl protocol users.
     */
    @Test public void migrate_legacySsl_setsNoticePending() {
        prefs.edit()
            .putString("server_protocol", "+ssl")
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.migrate();

        assertThat(prefs.getBoolean(AuthPreferences.TRANSPORT_SECURITY_NOTICE_PENDING, false)).isTrue();
    }

    /**
     * AC-3 (U-007): transport_security_notice_pending is set for legacy +tls protocol users.
     */
    @Test public void migrate_legacyTls_setsNoticePending() {
        prefs.edit()
            .putString("server_protocol", "+tls")
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.migrate();

        assertThat(prefs.getBoolean(AuthPreferences.TRANSPORT_SECURITY_NOTICE_PENDING, false)).isTrue();
    }

    /**
     * AC-3 (U-007): transport_security_notice_pending is set for users with stale trust_all=true,
     * even when SERVER_PROTOCOL is not a legacy +ssl/+tls value.
     */
    @Test public void migrate_staleTrustAllOnly_setsNoticePending() {
        prefs.edit()
            .putBoolean("server_trust_all_certificates", true)
            .putString("server_protocol", "+ssl+")  // NOT a legacy protocol
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.migrate();

        assertThat(prefs.getBoolean(AuthPreferences.TRANSPORT_SECURITY_NOTICE_PENDING, false)).isTrue();
    }

    /**
     * AC-3 (U-007): transport_security_notice_pending is set when BOTH conditions hold
     * (legacy protocol AND stale trust_all=true simultaneously).
     */
    @Test public void migrate_legacySslAndStaleTrustAll_setsNoticePending() {
        prefs.edit()
            .putBoolean("server_trust_all_certificates", true)
            .putString("server_protocol", "+ssl")
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.migrate();

        assertThat(prefs.getBoolean(AuthPreferences.TRANSPORT_SECURITY_NOTICE_PENDING, false)).isTrue();
        assertThat(authPreferences.isTrustAllCertificates()).isFalse();
    }

    /**
     * AC-4 (U-007): transport_security_notice_pending is NOT set for unaffected users.
     * Covers: "ssl+", "tls+", "ssl", "tls", "starttls", "", and absent/null.
     * All have trust_all=false (or absent).
     */
    @Test public void migrate_unaffectedUsers_doesNotSetNoticePending() {
        List<String> unaffectedProtocols = Arrays.asList("ssl+", "tls+", "ssl", "tls", "starttls", "");
        for (String protocol : unaffectedProtocols) {
            prefs.edit().clear().commit();

            prefs.edit()
                .putString("server_protocol", protocol)
                .putString("server_authentication", "plain")
                // trust_all absent (defaults to false)
                .commit();

            authPreferences.migrate();

            assertWithMessage("protocol=" + protocol + " must not set notice pending")
                .that(prefs.getBoolean(AuthPreferences.TRANSPORT_SECURITY_NOTICE_PENDING, false))
                .isFalse();
        }

        // Also test absent protocol value.
        prefs.edit().clear().commit();
        prefs.edit().putString("server_authentication", "plain").commit();
        authPreferences.migrate();
        assertWithMessage("absent protocol must not set notice pending")
            .that(prefs.getBoolean(AuthPreferences.TRANSPORT_SECURITY_NOTICE_PENDING, false))
            .isFalse();
    }

    /**
     * AC-5 (U-007): Protocol normalization is preserved — +ssl becomes +ssl+, +tls becomes +tls+.
     * Trust-all is NOT written true in either case.
     */
    @Test public void migrate_legacySsl_normalizesProtocolWithoutTrustAll() {
        prefs.edit()
            .putString("server_protocol", "+ssl")
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.migrate();

        assertThat(prefs.getString("server_protocol", null)).isEqualTo("+ssl+");
        assertThat(authPreferences.isTrustAllCertificates()).isFalse();
    }

    /**
     * AC-5 (U-007): Protocol normalization for +tls — +tls becomes +tls+.
     */
    @Test public void migrate_legacyTls_normalizesProtocolWithoutTrustAll() {
        prefs.edit()
            .putString("server_protocol", "+tls")
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.migrate();

        assertThat(prefs.getString("server_protocol", null)).isEqualTo("+tls+");
        assertThat(authPreferences.isTrustAllCertificates()).isFalse();
    }

    /**
     * AC-6 (U-007): useXOAuth() true causes early return — no preference writes of any kind.
     * OAuth2-configured users are completely unaffected.
     */
    @Test public void migrate_xoauth_returnsEarlyWithNoWrites() {
        // Setup: xoauth authentication mode triggers the early-return guard in migrate().
        // Also set a legacy protocol that would otherwise trigger migration, and trust_all=true
        // that would otherwise be cleared. With xoauth, none of these should be touched.
        prefs.edit()
            .putString("server_protocol", "+ssl")
            .putString("server_authentication", "xoauth")
            .putBoolean("server_trust_all_certificates", true)
            .commit();

        authPreferences.migrate();

        // Early return: no preferences are written.
        // trust_all must remain exactly as set (true) — the method returned before any write.
        assertThat(authPreferences.isTrustAllCertificates()).isTrue();
        // Protocol must remain unchanged.
        assertThat(prefs.getString("server_protocol", null)).isEqualTo("+ssl");
        // Notice flag must not be set.
        assertThat(prefs.getBoolean(AuthPreferences.TRANSPORT_SECURITY_NOTICE_PENDING, false)).isFalse();
    }

    /**
     * AC-9 (U-007): Already-normalized TLS users (e.g. +ssl+, +tls+) experience no change.
     * SERVER_TRUST_ALL_CERTIFICATES remains at its default (false), SERVER_PROTOCOL unchanged,
     * and no notice flag is set.
     */
    @Test public void migrate_alreadyNormalizedProtocol_noChanges() {
        prefs.edit()
            .putString("server_protocol", "+ssl+")
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.migrate();

        assertThat(authPreferences.isTrustAllCertificates()).isFalse();
        assertThat(prefs.getString("server_protocol", null)).isEqualTo("+ssl+");
        assertThat(prefs.getBoolean(AuthPreferences.TRANSPORT_SECURITY_NOTICE_PENDING, false)).isFalse();
    }

    // -------------------------------------------------------------------------
    // U-011: SecretStore injection tests (AC-5, AC-7, AC-8)
    // -------------------------------------------------------------------------

    /**
     * AC-8 (U-011): two-arg constructor accepts InMemorySecretStore without error.
     * Verifies the injection seam (AC-8(d)).
     */
    @Test public void twoArgConstructor_acceptsInMemorySecretStore() {
        InMemorySecretStore store = new InMemorySecretStore();
        // Must construct without throwing — confirms the injection seam exists.
        AuthPreferences ap = new AuthPreferences(RuntimeEnvironment.application, store);
        assertThat(ap).isNotNull();
    }

    /**
     * AC-5 (U-011): getOauth2Token() returns injected value via InMemorySecretStore.
     * Confirms the public typed-accessor signature is unchanged and routes through SecretStore.
     */
    @Test public void getOauth2Token_returnsValueFromSecretStore() {
        secretStore.put("oauth2_token", "my_access_token");
        assertThat(authPreferences.getOauth2Token()).isEqualTo("my_access_token");
    }

    /**
     * AC-5 (U-011): getOauth2RefreshToken() returns injected value via InMemorySecretStore.
     */
    @Test public void getOauth2RefreshToken_returnsValueFromSecretStore() {
        secretStore.put("oauth2_refresh_token", "my_refresh_token");
        assertThat(authPreferences.getOauth2RefreshToken()).isEqualTo("my_refresh_token");
    }

    /**
     * AC-5 (U-011): getOauth2Token() returns null when key is absent (SecretStore null-on-absent
     * contract, CNTR-MODERNIZATION-003 §Type notes).
     */
    @Test public void getOauth2Token_absentKey_returnsNull() {
        assertThat(authPreferences.getOauth2Token()).isNull();
    }

    /**
     * AC-5 (U-011): setOauth2Token() writes access and refresh tokens to SecretStore;
     * username stays in plaintext preferences (AC-4).
     */
    @Test public void setOauth2Token_writesCredentialsToSecretStore() {
        prefs.edit()
            .putString("server_authentication", "xoauth")
            .commit();

        authPreferences.setOauth2Token("user@example.com", "access123", "refresh456");

        // Credentials in SecretStore
        assertThat(secretStore.get("oauth2_token")).isEqualTo("access123");
        assertThat(secretStore.get("oauth2_refresh_token")).isEqualTo("refresh456");
        // Username in plaintext prefs (AC-4 — not a secret)
        assertThat(prefs.getString("oauth2_user", null)).isEqualTo("user@example.com");
    }

    /**
     * AC-5 (U-011): clearOauth2Data() removes access and refresh tokens from SecretStore.
     * hasOAuth2Tokens() returns false after clear (AC-5 behavioral contract).
     */
    @Test public void clearOauth2Data_removesCredentialsFromSecretStore() {
        prefs.edit()
            .putString("server_authentication", "xoauth")
            .commit();
        secretStore.put("oauth2_token", "sometoken");
        secretStore.put("oauth2_refresh_token", "somerefresh");
        prefs.edit().putString("oauth2_user", "user@example.com").commit();

        authPreferences.clearOauth2Data();

        assertThat(authPreferences.getOauth2Token()).isNull();
        assertThat(authPreferences.getOauth2RefreshToken()).isNull();
        assertThat(authPreferences.hasOAuth2Tokens()).isFalse();
    }

    /**
     * AC-5 (U-011): setImapPassword() and getImapPassword()-backed isLoginInformationSet()
     * work via SecretStore injection.
     */
    @Test public void setImapPassword_storedInSecretStore_isLoginInformationSet() {
        prefs.edit()
            .putString("server_address", "imap.example.com:993")
            .putString("server_authentication", "plain")
            .commit();
        authPreferences.setImapUser("user@example.com");
        authPreferences.setImapPassword("s3cret");

        assertThat(secretStore.get("login_password")).isEqualTo("s3cret");
        assertThat(authPreferences.isLoginInformationSet()).isTrue();
    }

    /**
     * AC-4 (U-011): Only secret keys cross the SecretStore boundary.
     * oauth2_user is NOT stored in SecretStore — confirmed by checking the plaintext prefs.
     */
    @Test public void setOauth2Token_usernameNotInSecretStore() {
        authPreferences.setOauth2Token("user@example.com", "access123", "refresh456");

        // Username must NOT be in the SecretStore (AC-4 / CNTR-MODERNIZATION-003 §Backing key set note)
        assertThat(secretStore.contains("oauth2_user")).isFalse();
        // Username IS in plaintext preferences
        assertThat(prefs.getString("oauth2_user", null)).isEqualTo("user@example.com");
    }

    // -------------------------------------------------------------------------
    // U-009: AC-7 (negative) — migrate() never writes PinnedCertStore
    // These tests live here because AuthPreferences.migrate() is package-private.
    // -------------------------------------------------------------------------

    /**
     * AC-7 (U-009): migrate() with +ssl protocol does NOT write the PinnedCertStore.
     * The PinnedCertStore is verified by checking the "pinned_certs" SharedPreferences file.
     */
    @Test public void u009_ac7_migrateWithLegacySsl_doesNotWritePinnedCertStore() {
        prefs.edit()
            .putString("server_protocol", "+ssl")
            .putString("server_address", "imap.example.org:993")
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.migrate();

        // The pinned_certs prefs file must remain empty — migrate() never pins certs.
        android.content.SharedPreferences pinnedPrefs =
            RuntimeEnvironment.application.getSharedPreferences("pinned_certs",
                android.content.Context.MODE_PRIVATE);
        assertThat(pinnedPrefs.getAll()).isEmpty();
    }

    /**
     * AC-7 (U-009): migrate() with stale trust_all=true does NOT write PinnedCertStore.
     */
    @Test public void u009_ac7_migrateWithStaleTrustAll_doesNotWritePinnedCertStore() {
        prefs.edit()
            .putBoolean("server_trust_all_certificates", true)
            .putString("server_protocol", "+ssl+")
            .putString("server_address", "imap.example.org:993")
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.migrate();

        android.content.SharedPreferences pinnedPrefs =
            RuntimeEnvironment.application.getSharedPreferences("pinned_certs",
                android.content.Context.MODE_PRIVATE);
        assertThat(pinnedPrefs.getAll()).isEmpty();
    }

    /**
     * AC-7 (U-009): xoauth migrate() early-return — PinnedCertStore still empty.
     */
    @Test public void u009_ac7_migrateXoauth_doesNotWritePinnedCertStore() {
        prefs.edit()
            .putString("server_protocol", "+ssl")
            .putString("server_address", "imap.gmail.com:993")
            .putString("server_authentication", "xoauth")
            .commit();

        authPreferences.migrate();

        android.content.SharedPreferences pinnedPrefs =
            RuntimeEnvironment.application.getSharedPreferences("pinned_certs",
                android.content.Context.MODE_PRIVATE);
        assertThat(pinnedPrefs.getAll()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // U-009: AC-8/AC-9 end-to-end notice flag lifecycle (via migrate())
    // -------------------------------------------------------------------------

    /**
     * AC-8 (U-009): migrate() with +ssl sets the pending flag;
     * consumeTransportSecurityNotice clears it; second call returns false.
     */
    @Test public void u009_ac8_legacySslMigrateSetsFlagThenNoticeConsumed() {
        prefs.edit()
            .putString("server_protocol", "+ssl")
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.migrate();
        assertThat(prefs.getBoolean(AuthPreferences.TRANSPORT_SECURITY_NOTICE_PENDING, false)).isTrue();

        // Consuming the notice clears the flag.
        boolean shown = com.zegoggles.smssync.activity.TransportSecurityNoticeHelper
            .checkAndClearNoticePending(RuntimeEnvironment.application);
        assertThat(shown).isTrue();
        assertThat(prefs.getBoolean(AuthPreferences.TRANSPORT_SECURITY_NOTICE_PENDING, true)).isFalse();

        // Second call — must NOT re-show.
        boolean shownAgain = com.zegoggles.smssync.activity.TransportSecurityNoticeHelper
            .checkAndClearNoticePending(RuntimeEnvironment.application);
        assertThat(shownAgain).isFalse();
    }

    /**
     * AC-9 (U-009): migrate() with +ssl+ (already-normalized) does NOT set the pending flag;
     * consumeTransportSecurityNotice returns false for this user.
     */
    @Test public void u009_ac9_normalizedProtocol_noFlagSet_noticeNotShown() {
        prefs.edit()
            .putString("server_protocol", "+ssl+")
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.migrate();

        assertThat(prefs.getBoolean(AuthPreferences.TRANSPORT_SECURITY_NOTICE_PENDING, false)).isFalse();

        boolean shown = com.zegoggles.smssync.activity.TransportSecurityNoticeHelper
            .checkAndClearNoticePending(RuntimeEnvironment.application);
        assertThat(shown).isFalse();
    }

    /**
     * AC-9 (U-009): xoauth user — migrate() early-returns; no flag set; no notice shown.
     */
    @Test public void u009_ac9_xoauthUser_noFlagSet_noticeNotShown() {
        prefs.edit()
            .putString("server_protocol", "+ssl")
            .putString("server_authentication", "xoauth")
            .commit();

        authPreferences.migrate();

        assertThat(prefs.getBoolean(AuthPreferences.TRANSPORT_SECURITY_NOTICE_PENDING, false)).isFalse();

        boolean shown = com.zegoggles.smssync.activity.TransportSecurityNoticeHelper
            .checkAndClearNoticePending(RuntimeEnvironment.application);
        assertThat(shown).isFalse();
    }

    // -------------------------------------------------------------------------
    // U-012: Plaintext-to-encrypted credential migration tests (AC-1 through AC-4)
    // All tests use InMemorySecretStore (injected via the two-arg constructor) so
    // they run on the plain JVM without requiring a real Android Keystore.
    // See U-011 Tech Notes: Robolectric 4.12.x does not shadow AndroidKeyStore.
    // -------------------------------------------------------------------------

    /**
     * AC-1 (U-012): After a completed migration, all three plaintext credential keys are
     * absent from the legacy backing map, and all three values are readable through the
     * encrypted store.
     *
     * Verification step 1 from U-012: seed the InMemorySecretStore's legacy map with all
     * three keys, run migration, assert legacyStore cleared and secretStore populated.
     */
    @Test public void u012_ac1_migration_clearsPlaintextAndEncryptsCredentials() {
        // Seed the legacy side with all three plaintext credential keys.
        secretStore.legacyStore.put("login_password", "app-password");
        secretStore.legacyStore.put("oauth2_token", "access");
        secretStore.legacyStore.put("oauth2_refresh_token", "refresh");

        secretStore.migrateFromPlaintext();

        // All three plaintext entries must be cleared from the legacy map (AC-1).
        assertThat(secretStore.legacyStore.containsKey("login_password")).isFalse();
        assertThat(secretStore.legacyStore.containsKey("oauth2_token")).isFalse();
        assertThat(secretStore.legacyStore.containsKey("oauth2_refresh_token")).isFalse();

        // All three values must be readable through the encrypted store (AC-1).
        assertThat(secretStore.get("login_password")).isEqualTo("app-password");
        assertThat(secretStore.get("oauth2_token")).isEqualTo("access");
        assertThat(secretStore.get("oauth2_refresh_token")).isEqualTo("refresh");

        // Migration marker must be set inside the encrypted store (AC-1).
        assertThat(secretStore.contains("__secretstore_migration_complete__")).isTrue();
    }

    /**
     * AC-2 (U-012): Migration is idempotent — running it a second time produces no
     * exception and leaves values and the marker stable.
     *
     * Verification step 2 from U-012: seed legacy map, run migration twice, assert
     * no exception, stable values, and a single marker.
     */
    @Test public void u012_ac2_migration_isIdempotent() {
        // Seed the legacy side with all three keys.
        secretStore.legacyStore.put("login_password", "app-password");
        secretStore.legacyStore.put("oauth2_token", "access");
        secretStore.legacyStore.put("oauth2_refresh_token", "refresh");

        // First run.
        secretStore.migrateFromPlaintext();
        String pw1   = secretStore.get("login_password");
        String at1   = secretStore.get("oauth2_token");
        String rt1   = secretStore.get("oauth2_refresh_token");

        // Second run — must not throw and must not change values or add duplicate marker.
        secretStore.migrateFromPlaintext();
        String pw2   = secretStore.get("login_password");
        String at2   = secretStore.get("oauth2_token");
        String rt2   = secretStore.get("oauth2_refresh_token");

        assertThat(pw2).isEqualTo(pw1);
        assertThat(at2).isEqualTo(at1);
        assertThat(rt2).isEqualTo(rt1);
        assertThat(secretStore.contains("__secretstore_migration_complete__")).isTrue();
    }

    /**
     * AC-3 (U-012): Interruption safety — a simulated crash between step-3 commit and
     * step-4 plaintext clear (marker present, plaintext not yet cleared) leaves credentials
     * reachable and migration completable on next launch.
     *
     * Verification step 3 from U-012: seed the encrypted store directly with all three
     * secrets and the marker (simulating completed step-3 but uncompleted step-4), leave
     * legacy map intact, call migrateFromPlaintext(), assert method returns without throwing
     * and all three secrets are reachable.
     */
    @Test public void u012_ac3_migration_interruptionSafe_markerPresentPlaintextNotCleared() {
        // Simulate a mid-flight state: encrypted store has the marker and all three secrets
        // (step-3 committed) but legacy map still has the plaintext (step-4 not yet run).
        secretStore.put("login_password", "app-password");
        secretStore.put("oauth2_token", "access");
        secretStore.put("oauth2_refresh_token", "refresh");
        secretStore.put("__secretstore_migration_complete__", "1");
        // Legacy still has plaintext (step-4 was not reached before the simulated crash).
        secretStore.legacyStore.put("login_password", "app-password");
        secretStore.legacyStore.put("oauth2_token", "access");
        secretStore.legacyStore.put("oauth2_refresh_token", "refresh");

        // Next launch: migrateFromPlaintext detects marker and returns immediately.
        secretStore.migrateFromPlaintext();

        // All three secrets must be reachable through the encrypted store.
        assertThat(secretStore.get("login_password")).isEqualTo("app-password");
        assertThat(secretStore.get("oauth2_token")).isEqualTo("access");
        assertThat(secretStore.get("oauth2_refresh_token")).isEqualTo("refresh");
        assertThat(secretStore.contains("__secretstore_migration_complete__")).isTrue();

        // Safety invariant: no window where both stores lack the secrets.
        // (At the simulated crash point, both stores held the secrets.)
    }

    /**
     * AC-4 (U-012): Fresh install — no legacy plaintext; first writes via setImapPassword()
     * and setOauth2Token() go directly through the encrypted store; legacy map stays empty.
     *
     * Verification step 4 from U-012: construct AuthPreferences with InMemorySecretStore
     * that has an empty legacy map and an empty encrypted map; call setters; assert
     * round-trip correctness and absence of legacy plaintext entries.
     */
    @Test public void u012_ac4_freshInstall_writesOnlyToEncryptedStore() {
        // Fresh install: both legacy and encrypted maps are empty (default @Before state).
        // Confirm there are no pre-existing legacy entries.
        assertThat(secretStore.legacyStore).isEmpty();

        // Write via the public typed accessors (unchanged signatures per AC-6).
        authPreferences.setImapPassword("test-pw");
        authPreferences.setOauth2Token("user@example.com", "at", "rt");

        // Credentials must be readable from the encrypted store.
        assertThat(secretStore.get("login_password")).isEqualTo("test-pw");
        assertThat(secretStore.get("oauth2_token")).isEqualTo("at");
        assertThat(secretStore.get("oauth2_refresh_token")).isEqualTo("rt");

        // Legacy map must remain empty — no plaintext written (AC-4).
        assertThat(secretStore.legacyStore.containsKey("login_password")).isFalse();
        assertThat(secretStore.legacyStore.containsKey("oauth2_token")).isFalse();
        assertThat(secretStore.legacyStore.containsKey("oauth2_refresh_token")).isFalse();
    }

    /**
     * AC-6 (U-012): Public typed-accessor signatures are unchanged; hasOAuth2Tokens()
     * returns false when SecretStore.get() returns null (null-means-absent contract,
     * CNTR-MODERNIZATION-003 §Consumer obligations).
     */
    @Test public void u012_ac6_hasOAuth2Tokens_returnsFalseWhenSecretStoreReturnsNull() {
        // No tokens in the secret store (default @Before state).
        assertThat(secretStore.get("oauth2_token")).isNull();
        assertThat(authPreferences.hasOAuth2Tokens()).isFalse();
    }

    /**
     * AC-1+IC-2 (U-012): Calling AuthPreferences.migrate() triggers migrateFromPlaintext()
     * for non-OAuth2 users; plaintext credentials are removed and encrypted after migrate().
     */
    @Test public void u012_ic2_migrateCallsThroughToMigrateFromPlaintext() {
        prefs.edit()
            .putString("server_protocol", "+ssl+")
            .putString("server_authentication", "plain")
            .commit();
        // Seed legacy plaintext.
        secretStore.legacyStore.put("login_password", "imap-pw");
        secretStore.legacyStore.put("oauth2_token", "at");
        secretStore.legacyStore.put("oauth2_refresh_token", "rt");

        authPreferences.migrate();

        // Encrypted store must have all three values.
        assertThat(secretStore.get("login_password")).isEqualTo("imap-pw");
        assertThat(secretStore.get("oauth2_token")).isEqualTo("at");
        assertThat(secretStore.get("oauth2_refresh_token")).isEqualTo("rt");
        // Legacy map must be cleared.
        assertThat(secretStore.legacyStore.containsKey("login_password")).isFalse();
    }

    /**
     * U-012 Robolectric smoke test for EncryptedPrefsSecretStore.migrateFromPlaintext().
     *
     * NOTE: This test exercises the EncryptedPrefsSecretStore DIRECTLY rather than via
     * AuthPreferences to verify the production migration path at the adapter level.
     * However, Robolectric 4.12.x does NOT shadow the AndroidKeyStore JCA provider
     * (U-011 Tech Notes: AC-11). EncryptedSharedPreferences.create() throws
     * NoSuchAlgorithmException under Robolectric because the KeyStore provider is absent.
     *
     * This test is @Ignore-d with a comment per the story's instruction: "if Robolectric
     * does not support the security-crypto Keystore path at the available shadow version,
     * this test should be @Ignored with a comment referencing the shadow limitation rather
     * than deleted (preserving the intent for future CI infrastructure upgrades)."
     *
     * TODO: Un-ignore when Robolectric adds AndroidKeyStore shadow support or when the
     * security-crypto library ships a Robolectric-compatible test artifact.
     */
    @Test
    @Ignore("Robolectric 4.12.x does not shadow AndroidKeyStore JCA provider — " +
            "EncryptedSharedPreferences.create() throws NoSuchAlgorithmException under " +
            "Robolectric. See U-011 Tech Notes (AC-11). Un-ignore when shadow support lands.")
    public void u012_robolectricSmoke_encryptedPrefsSecretStore_migrateFromPlaintext() throws Exception {
        // Arrange: write legacy plaintext into SharedPreferences("credentials").
        android.content.SharedPreferences legacy =
            RuntimeEnvironment.application.getSharedPreferences(
                "credentials", android.content.Context.MODE_PRIVATE);
        legacy.edit()
            .putString("login_password", "smoke-pw")
            .putString("oauth2_token", "smoke-at")
            .putString("oauth2_refresh_token", "smoke-rt")
            .commit();

        // Act: construct and migrate using the production adapter.
        EncryptedPrefsSecretStore store =
            new EncryptedPrefsSecretStore(RuntimeEnvironment.application);
        store.migrateFromPlaintext();

        // Assert: values readable and marker present.
        assertThat(store.get("login_password")).isEqualTo("smoke-pw");
        assertThat(store.get("oauth2_token")).isEqualTo("smoke-at");
        assertThat(store.get("oauth2_refresh_token")).isEqualTo("smoke-rt");
        assertThat(store.contains("__secretstore_migration_complete__")).isTrue();

        // Assert: legacy plaintext cleared.
        assertThat(legacy.getString("login_password", null)).isNull();
        assertThat(legacy.getString("oauth2_token", null)).isNull();
        assertThat(legacy.getString("oauth2_refresh_token", null)).isNull();
    }
}
