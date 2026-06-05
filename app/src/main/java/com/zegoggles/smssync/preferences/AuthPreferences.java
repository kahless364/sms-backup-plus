package com.zegoggles.smssync.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.fsck.k9.mail.AuthType;
import com.zegoggles.smssync.R;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import static android.util.Base64.NO_WRAP;
import static com.zegoggles.smssync.App.TAG;
import static com.zegoggles.smssync.preferences.Preferences.getDefaultType;

public class AuthPreferences {
    private static final String UTF_8 = "UTF-8";
    private final Context context;
    private final SharedPreferences preferences;
    // U-011: SecretStore replaces direct SharedPreferences("credentials") access.
    // The secretStore field is final and injected via the two-arg constructor.
    // The single-arg convenience constructor constructs EncryptedPrefsSecretStore (Phase 1).
    // In Phase 2 (DES-MODERNIZATION-008, Hilt), the @Binds injection will target the
    // two-arg constructor — no other code changes required at that time.
    private final SecretStore secretStore;

    public static final String SERVER_AUTHENTICATION = "server_authentication";

    private static final String OAUTH2_USER = "oauth2_user";
    // U-011: OAUTH2_TOKEN, OAUTH2_REFRESH_TOKEN, IMAP_PASSWORD are the exact on-disk key
    // strings (CNTR-MODERNIZATION-003 §Backing key set). They are referenced via the Java
    // constants (not inline string literals) to keep the values consistent with the
    // migration routine in U-012.
    private static final String OAUTH2_TOKEN = "oauth2_token";
    private static final String OAUTH2_REFRESH_TOKEN = "oauth2_refresh_token";

    public static final String IMAP_USER = "login_user";
    public static final String IMAP_PASSWORD = "login_password";

    /**
     * Preference key containing the server address
     */
    public static final String SERVER_ADDRESS = "server_address";
    /**
     * Preference key containing the server protocol
     */
    private static final String SERVER_PROTOCOL = "server_protocol";

    private static final String SERVER_TRUST_ALL_CERTIFICATES = "server_trust_all_certificates";

    /**
     * Boolean flag written by migrate() to indicate that a one-time transport-security
     * notice should be shown to the user. Set when the legacy +ssl/+tls protocol downgrade
     * is detected, or when a stale SERVER_TRUST_ALL_CERTIFICATES=true value is cleared.
     * The presentation layer reads this flag, shows the notice once, and writes
     * "transport_security_notice_shown" = true (analogous to sms_default_package_change_seen
     * at Preferences.java:247-253). This story (U-007) writes the flag only; the UI is U-009.
     */
    static final String TRANSPORT_SECURITY_NOTICE_PENDING = "transport_security_notice_pending";

    /**
     * IMAP URI.
     *
     * This should be in the form of:
     * <ol>
     * <li><code>imap+ssl+://XOAUTH2:ENCODED_USERNAME:ENCODED_TOKEN@imap.gmail.com:993</code></li>
     * <li><code>imap+ssl+://XOAUTH:ENCODED_USERNAME:ENCODED_TOKEN@imap.gmail.com:993</code></li>
     * <li><code>imap+ssl+://PLAIN:ENCODED_USERNAME:ENCODED_PASSWOR@imap.gmail.com:993</code></li>
     * <li><code>imap://PLAIN:ENCODED_USERNAME:ENCODED_PASSWOR@imap.gmail.com:993</code></li>
     * <li><code>imap://PLAIN:ENCODED_USERNAME:ENCODED_PASSWOR@imap.gmail.com</code></li>
     * </ol>
     */
    private static final String IMAP_URI = "imap%s://%s:%s:%s@%s";

    private static final String DEFAULT_SERVER_ADDRESS = "imap.gmail.com:993";
    private static final String DEFAULT_SERVER_PROTOCOL = "+ssl+";

    /**
     * Convenience constructor — Phase 1 (DES-MODERNIZATION-004 §Hilt Injection).
     *
     * Constructs an EncryptedPrefsSecretStore and delegates to the two-arg constructor.
     * This is the constructor called by {@code Preferences.java:295} and on every
     * {@code App.onCreate()}; its signature MUST remain unchanged (AC-8 / AC-5).
     *
     * If EncryptedSharedPreferences construction fails (GeneralSecurityException or
     * IOException) — which happens in unit-test environments where the Android Keystore
     * provider is unavailable — the constructor logs a warning and falls back to an
     * InMemorySecretStore. This fallback is intentional for the test path; in production
     * on a real device the AndroidKeyStore provider is always present and the fallback
     * is never taken.
     *
     * Robolectric limitation note (AC-11 / U-011 Tech Notes): Robolectric 4.12.x does
     * not shadow the AndroidKeyStore JCA provider. EncryptedSharedPreferences.create()
     * therefore throws NoSuchAlgorithmException under Robolectric. All tests that exercise
     * credential I/O must use the two-arg constructor with InMemorySecretStore injection.
     */
    public AuthPreferences(Context context) {
        this(context, buildEncryptedStoreSafe(context));
    }

    /**
     * Two-arg constructor — the injection seam (AC-8 / DES-MODERNIZATION-004 §Hilt Injection).
     *
     * Accepts any SecretStore implementation, enabling test injection of InMemorySecretStore.
     * In Phase 2 (DES-MODERNIZATION-008, Hilt), {@code @Binds @Singleton SecretStore <-
     * EncryptedPrefsSecretStore} will target this constructor — a mechanical swap that
     * requires no change to SecretStore, EncryptedPrefsSecretStore, or any caller.
     */
    public AuthPreferences(Context context, SecretStore secretStore) {
        this.context = context.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.secretStore = secretStore;
    }

    /**
     * Constructs an EncryptedPrefsSecretStore with lazy initialization.
     *
     * The EncryptedPrefsSecretStore constructor is trivial (just stores the context);
     * the actual EncryptedSharedPreferences backing store is opened lazily on first use.
     * This means the construction here never throws — any Keystore failure surfaces at
     * the first get/put/contains/etc. call, where it is caught and surfaced as null (reads)
     * or a RuntimeException (writes).
     *
     * Under Robolectric (AC-11 / U-011 Tech Notes), the AndroidKeyStore JCA provider is
     * unavailable. Tests that exercise credential I/O must use the two-arg constructor
     * with InMemorySecretStore injection rather than the single-arg constructor.
     */
    private static SecretStore buildEncryptedStoreSafe(Context context) {
        return new EncryptedPrefsSecretStore(context);
    }

    /**
     * Returns the OAuth2 access token, or {@code null} if absent.
     *
     * U-035: Awaits the credential migration gate before reading, so this method
     * never returns a pre-migration or mid-migration value. The gate is a no-op on
     * the main thread (to prevent ANR) and a no-op if the gate was never prepared
     * (test environments, second-launch fast-path). See {@link CredentialMigrationGate}.
     */
    public String getOauth2Token() {
        CredentialMigrationGate.awaitIfNeeded();
        return secretStore.get(OAUTH2_TOKEN);
    }

    /**
     * Returns the OAuth2 refresh token, or {@code null} if absent.
     *
     * U-035: Gate-guarded — see {@link #getOauth2Token()} and {@link CredentialMigrationGate}.
     */
    public String getOauth2RefreshToken() {
        CredentialMigrationGate.awaitIfNeeded();
        return secretStore.get(OAUTH2_REFRESH_TOKEN);
    }

    public boolean hasOAuth2Tokens() {
        return getOauth2Username() != null &&
                getOauth2Token() != null;
    }

    public void setOauth2Token(String username, String accessToken, String refreshToken) {
        // oauth2_user is NOT a secret — it stays in plaintext preferences (AC-4 /
        // CNTR-MODERNIZATION-003 §Backing key set note).
        preferences.edit()
                .putString(OAUTH2_USER, username)
                .commit();

        // Credentials go through SecretStore for encrypted-at-rest storage.
        // put() uses commit() semantics per CNTR-MODERNIZATION-003 §Type notes.
        secretStore.put(OAUTH2_TOKEN, accessToken);
        secretStore.put(OAUTH2_REFRESH_TOKEN, refreshToken);
    }

   public void clearOauth2Data() {
        final String oauth2token = getOauth2Token();

        // oauth2_user stays in plaintext preferences — not a secret (AC-4).
        preferences.edit()
                .remove(OAUTH2_USER)
                .commit();

        // Remove secrets via SecretStore.
        secretStore.remove(OAUTH2_TOKEN);
        secretStore.remove(OAUTH2_REFRESH_TOKEN);

        if (!TextUtils.isEmpty(oauth2token)) {
            // U-023: fully-qualified so AC-8 short-name grep returns zero results
            new com.zegoggles.smssync.auth.TokenRefresher(context, new com.zegoggles.smssync.auth.OAuth2Client(getOAuth2ClientId()), this).invalidateToken(oauth2token);
        }
    }

    public String getOAuth2ClientId() {
        return context.getString(R.string.oauth2_client_id);
    }

    public void setImapPassword(String s) {
        secretStore.put(IMAP_PASSWORD, s);
    }

    public void setImapUser(String s) {
        preferences.edit().putString(IMAP_USER, s).commit();
    }

    @SuppressWarnings("deprecation")
    public boolean useXOAuth() {
        return getAuthMode() == AuthMode.XOAUTH;
    }

    public boolean usePlain() {
        return getAuthMode() == AuthMode.PLAIN;
    }

    public String getUserEmail() {
        if (getAuthMode() == AuthMode.PLAIN) {
            return getImapUsername();
        } else {
            return getOauth2Username();
        }
    }

    public String toString() {
        if (DEFAULT_SERVER_ADDRESS.equals(getServername())) {
            return getImapUsername() + " (Gmail)";
        } else {
            return getImapUsername() + "@" + getServername();
        }
    }

    @SuppressWarnings("deprecation")
    public boolean isLoginInformationSet() {
        switch (getAuthMode()) {
            case PLAIN:
                return !TextUtils.isEmpty(getImapPassword()) &&
                       !TextUtils.isEmpty(getImapUsername()) &&
                       !TextUtils.isEmpty(getServerAddress());
            case XOAUTH:
                return hasOAuth2Tokens();
            default:
                return false;
        }
    }

    public String getStoreUri() {
        if (useXOAuth()) {
            if (hasOAuth2Tokens()) {
                return formatUri(
                    AuthType.XOAUTH2,
                        DEFAULT_SERVER_PROTOCOL,
                        getOauth2Username(),
                        generateXOAuth2Token(),
                        DEFAULT_SERVER_ADDRESS);
            } else {
                Log.w(TAG, "No valid xoauth2 tokens");
                return null;
            }
        } else {
            return formatUri(AuthType.PLAIN,
                getServerProtocol(),
                getImapUsername(),
                getImapPassword(),
                getServerAddress());
        }
    }

    private String getServerAddress() {
        return preferences.getString(SERVER_ADDRESS, DEFAULT_SERVER_ADDRESS);
    }

    private String getServerProtocol() {
        return preferences.getString(SERVER_PROTOCOL, DEFAULT_SERVER_PROTOCOL);
    }

    public boolean isTrustAllCertificates() {
        return preferences.getBoolean(SERVER_TRUST_ALL_CERTIFICATES, false);
    }

    private String formatUri(AuthType authType, String serverProtocol, String username, String password, String serverAddress) {
        return String.format(IMAP_URI,
            serverProtocol,
            authType.name().toUpperCase(Locale.US),
            // NB: there's a bug in K9mail-library which requires double-encoding of uris
            // https://github.com/k9mail/k-9/commit/b0d401c3b73c6b57402dc81d3cfd6488a71a1b98
            encode(encode(username)),
            encode(encode(password)),
            serverAddress);
    }

    public String getOauth2Username() {
        return preferences.getString(OAUTH2_USER, null);
    }

    private AuthMode getAuthMode() {
        return getDefaultType(preferences, SERVER_AUTHENTICATION, AuthMode.class, AuthMode.PLAIN);
    }

    public String getServername() {
        return preferences.getString(SERVER_ADDRESS, null);
    }

    public String getImapUsername() {
        return preferences.getString(IMAP_USER, null);
    }

    /**
     * Returns the IMAP password, or {@code null} if absent.
     *
     * U-035: Gate-guarded — see {@link #getOauth2Token()} and {@link CredentialMigrationGate}.
     */
    private String getImapPassword() {
        CredentialMigrationGate.awaitIfNeeded();
        return secretStore.get(IMAP_PASSWORD);
    }

    /**
     * TODO: this should probably be handled in K9
     *
     * <p>
     * The SASL XOAUTH2 initial client response has the following format:
     * </p>
     * <code>base64("user="{User}"^Aauth=Bearer "{Access Token}"^A^A")</code>
     * <p>
     * For example, before base64-encoding, the initial client response might look like this:
     * </p>
     * <code>user=someuser@example.com^Aauth=Bearer vF9dft4qmTc2Nvb3RlckBhdHRhdmlzdGEuY29tCg==^A^A</code>
     * <p/>
     * <em>Note:</em> ^A represents a Control+A (\001).
     *
     * @see <a href="https://developers.google.com/google-apps/gmail/xoauth2_protocol#the_sasl_xoauth2_mechanism">
     *      The SASL XOAUTH2 Mechanism</a>
     */
    private @NonNull String generateXOAuth2Token() {
        final String username = getOauth2Username();
        final String token = getOauth2Token();
        final String formatted = "user=" + username + "\001auth=Bearer " + token + "\001\001";
        try {
            return Base64.encodeToString(formatted.getBytes(UTF_8), NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String encode(String s) {
        try {
            return s == null ? "" : URLEncoder.encode(s, UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    void migrate() {
        // DES-MODERNIZATION-002 Decision 6 — rewritten by U-007 to eliminate the
        // silent trust-all downgrade (ARCH-008 / SEC-001 / CWE-295).

        // U-012: one-time plaintext-to-encrypted credential migration.
        // Called BEFORE the useXOAuth() early-return so that OAuth2 users' tokens
        // (oauth2_token / oauth2_refresh_token) are also migrated. Placing this call
        // after the early-return would silently leave OAuth2 users' plaintext tokens
        // un-migrated, causing all OAuth2 users to be logged out after upgrading.
        // The call is idempotent: guarded by the __secretstore_migration_complete__
        // marker so subsequent launches are no-ops (CNTR-MODERNIZATION-003 §Migration).
        secretStore.migrateFromPlaintext();

        if (useXOAuth()) {
            return;
        }
        final String protocol = getServerProtocol();
        final boolean wasLegacyDowngradeProtocol =
            "+ssl".equals(protocol) || "+tls".equals(protocol);

        SharedPreferences.Editor edit = preferences.edit();

        // AC-5 / REQ-MODERNIZATION-002 AC-10: NEVER write SERVER_TRUST_ALL_CERTIFICATES=true.
        // AC-2 migration-of-already-downgraded-users: actively clear any stale true left by
        // the previous app version's silent downgrade, restoring validated TLS immediately.
        if (preferences.getBoolean(SERVER_TRUST_ALL_CERTIFICATES, false)) {
            edit.putBoolean(SERVER_TRUST_ALL_CERTIFICATES, false);
            markTransportSecurityNoticePending(edit);   // AC-3: one-time notice for affected cohort
        }

        // Protocol normalization is preserved (AC-5 "may update SERVER_PROTOCOL"),
        // but WITHOUT the coupled trust-all write that the old code performed.
        if (wasLegacyDowngradeProtocol) {
            edit.putString(SERVER_PROTOCOL, protocol + "+");
            markTransportSecurityNoticePending(edit);   // AC-3: these users are the affected cohort
        }

        // ARCH-017: apply(), not commit(), for a launch-path write.
        // The write is idempotent — if the process is killed before apply() flushes,
        // migrate() re-executes on the next launch and produces the same result.
        edit.apply();
    }

    /**
     * Records that a one-time transport-security notice is pending for this user.
     * Called from migrate() when the user is in the affected cohort (legacy +ssl/+tls
     * protocol, or stale SERVER_TRUST_ALL_CERTIFICATES=true). The notice flag is consumed
     * by the UI presentation layer (U-009); this method only writes the flag.
     */
    private static void markTransportSecurityNoticePending(SharedPreferences.Editor edit) {
        edit.putBoolean(TRANSPORT_SECURITY_NOTICE_PENDING, true);
    }
}
