package com.zegoggles.smssync.preferences;

import android.preference.PreferenceManager;
import com.fsck.k9.mail.AuthType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(RobolectricTestRunner.class)
public class AuthPreferencesTest {
    private AuthPreferences authPreferences;

    @Before public void before() {
        openMocks(this);
        // Clear any preference state left by previous tests to prevent ordering dependency.
        PreferenceManager
            .getDefaultSharedPreferences(RuntimeEnvironment.application)
            .edit()
            .clear()
            .commit();
        authPreferences = new AuthPreferences(RuntimeEnvironment.application);
    }

    @Test public void testStoreUri() throws Exception {
        PreferenceManager
            .getDefaultSharedPreferences(RuntimeEnvironment.application)
            .edit()
            .putString("server_address", "foo.com:993")
            .putString("server_protocol", "+ssl+")
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.setImapUser("a:user");
        authPreferences.setImapPassword("password:has:colons");
        assertThat(authPreferences.getStoreUri()).isEqualTo("imap+ssl+://PLAIN:a%253Auser:password%253Ahas%253Acolons@foo.com:993");
    }

    @Test public void testStoreUriWithXOAuth2() throws Exception {
        PreferenceManager
            .getDefaultSharedPreferences(RuntimeEnvironment.application)
            .edit()
            .putString("server_address", "imap.gmail.com:993")
            .putString("server_protocol", "+ssl+")
            .putString("server_authentication", "xoauth")
            .commit();

        authPreferences.setOauth2Token("user", "token", null);

        assertThat(authPreferences.getStoreUri()).isEqualTo("imap+ssl+://XOAUTH2:user:dXNlcj11c2VyAWF1dGg9QmVhcmVyIHRva2VuAQE%253D@imap.gmail.com:993");
    }

    // -------------------------------------------------------------------------
    // U-006 characterization tests for AuthPreferences.migrate()
    // These tests pin the behavioral contracts of migrate() before DES-002 modifies it.
    // -------------------------------------------------------------------------

    /**
     * AC-1: AUTHORED RED — pins the target state required by U-007 (DES-MODERNIZATION-002
     * migrate() rewrite). It fails against current code by design.
     *
     * // AUTHORED RED — this test pins the target state required by U-007 (DES-MODERNIZATION-002
     * // migrate() rewrite). It fails against current code by design. Do not change the assertion
     * // to match the current behavior.
     *
     * Current behavior (AuthPreferences.java:284): migrate() SETS trust_all=true for +ssl/+tls.
     * Target behavior (post-DES-002): migrate() must NOT silently enable trust-all.
     *
     * Annotated @Ignore so CI stays green until U-007 rewrites migrate(). The @Ignore itself
     * is the living documentation of the pending work. Remove @Ignore in U-007 as the
     * executable acceptance criterion for that story.
     */
    @Ignore("AUTHORED RED — this test pins the target state required by U-007 " +
            "(DES-MODERNIZATION-002 migrate() rewrite). It fails against current code by design. " +
            "Remove @Ignore in U-007 when migrate() no longer silently enables trust-all.")
    @Test public void migrate_legacySslTls_doesNotEnableTrustAll() {
        // AUTHORED RED — this test pins the target state required by U-007 (DES-MODERNIZATION-002
        // migrate() rewrite). It fails against current code by design. Do not change the assertion
        // to match the current behavior.
        //
        // Setup: simulate a legacy +ssl account (non-XOAUTH) that triggers the migrate() branch.
        // migrate() at AuthPreferences.java:281-287 fires when protocol is "+ssl" or "+tls".
        PreferenceManager
            .getDefaultSharedPreferences(RuntimeEnvironment.application)
            .edit()
            .putString("server_protocol", "+ssl")
            .putString("server_authentication", "plain")  // non-XOAUTH so early-return is not hit
            .commit();

        authPreferences.migrate();

        // After migration, trust-all must NOT be silently enabled.
        // This assertion is FALSE against current code (migrate() sets trust_all=true at :284).
        // It becomes TRUE only when DES-002 rewrites migrate().
        assertThat(authPreferences.isTrustAllCertificates()).isFalse();
    }

    /**
     * AC-2: GREEN — documents that a clean-state account (never through legacy +ssl/+tls path)
     * never receives a trust-all grant from migrate().
     */
    @Test public void migrate_cleanState_doesNotEnableTrustAll() {
        // Setup: default +ssl+ protocol (the "+" suffix distinguishes new-style from legacy).
        // migrate() only fires on "+ssl" or "+tls" (without trailing "+"), so this path is a no-op.
        PreferenceManager
            .getDefaultSharedPreferences(RuntimeEnvironment.application)
            .edit()
            .putString("server_protocol", "+ssl+")  // new-style protocol — migrate() ignores this
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.migrate();

        // No server_trust_all_certificates key is written — isTrustAllCertificates() defaults false.
        assertThat(authPreferences.isTrustAllCertificates()).isFalse();
    }

    /**
     * AC-3: GREEN — documents that migrate() does not clear an EXPLICIT user-set trust-all.
     * User-initiated trust-all is a separate concern from the silent downgrade path.
     */
    @Test public void migrate_explicitTrustAll_isPreserved() {
        // Setup: user has explicitly set trust_all=true, but with a protocol that does not
        // trigger the +ssl/+tls legacy branch (using +ssl+ which is the new-style format).
        PreferenceManager
            .getDefaultSharedPreferences(RuntimeEnvironment.application)
            .edit()
            .putBoolean("server_trust_all_certificates", true)
            .putString("server_protocol", "+ssl+")  // new-style — does not trigger migrate() branch
            .putString("server_authentication", "plain")
            .commit();

        authPreferences.migrate();

        // Explicit user-set trust_all must be preserved by migrate().
        assertThat(authPreferences.isTrustAllCertificates()).isTrue();
    }
}
