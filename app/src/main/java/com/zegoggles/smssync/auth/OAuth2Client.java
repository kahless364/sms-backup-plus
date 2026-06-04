package com.zegoggles.smssync.auth;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.zegoggles.smssync.BuildConfig;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

import static com.zegoggles.smssync.App.TAG;

/**
 * https://developers.google.com/identity/protocols/OAuth2UserAgent
 *
 * <p>Exchanges OAuth 2.0 authorization codes for tokens and refreshes access tokens.
 * Account-email resolution ("username") is delegated to a {@link ContactsPort}
 * (default: {@link PeopleApiContactsAdapter}). Resolution is best-effort; a null
 * username is valid (CNTR-MODERNIZATION-008 VR-3).
 */
public class OAuth2Client {
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/auth";
    private static final String TOKEN_URL = "https://www.googleapis.com/oauth2/v3/token";

    /**
     * When choosing a URI scheme to associate with the app, apps MUST use a
     * URI scheme based on a domain name under their control, expressed in
     * reverse order, as recommended by Section 3.8 of [RFC7595] for
     * private-use URI schemes.
     *
     * For more details, see
     * <a href="https://tools.ietf.org/html/draft-ietf-oauth-native-apps-12#page-8">
     *     OAuth 2.0 for Native Apps
     * </a>
     */
    public static final Uri REDIRECT_URL = Uri.parse("com.zegoggles.smssync:/oauth2redirect");

    /**
     * For installed applications, use a value of code, indicating that the Google OAuth 2.0 endpoint should return an authorization code.
     */
    private static final String RESPONSE_TYPE = "response_type";

    /**
     * Identifies the client that is making the request.
     * The value passed in this parameter must exactly match the value shown in the Google Developers Console.
     */
    private static final String CLIENT_ID = "client_id";

    /**
     * Determines where the response is sent.
     * The value of this parameter must exactly match one of the values that appear in the
     * Credentials page in the Google Developers Console (including the http or https scheme, case, and trailing slash).
     * You may choose between <code>urn:ietf:wg:oauth:2.0:oob</code>,
     * <code>urn:ietf:wg:oauth:2.0:oob:auto</code>, or an <code>http://localhost</code> port.
     * For more details, see <a href="https://developers.google.com/identity/protocols/OAuth2InstalledApp#choosingredirecturi">Choosing a redirect URI</a>.
     */
    private static final String REDIRECT_URI = "redirect_uri";

    /**
     * Space-delimited set of scope strings.
     *
     * Identifies the Google API access that your application is requesting.
     * The values passed in this parameter inform the consent screen that is shown to the user. There may be an inverse
     * relationship between the number of permissions requested and the likelihood
     * of obtaining user consent.
     */
    private static final String SCOPE = "scope";

    /**
     * Provides any state information that might be useful to your application upon receipt
     * of the response. The Google Authorization Server roundtrips this parameter, so your application receives
     * the same value it sent. Possible uses include redirecting the user to the
     * correct resource in your site, nonces, and cross-site-request-forgery mitigations.
     */
    private static final String STATE = "state";

    /**
     * When your application knows which user it is trying to authenticate, it can
     * provide this parameter as a hint to the Authentication Server.
     * Passing this hint will either pre-fill the email box on the sign-in form or select the proper
     * multi-login session, thereby simplifying the login flow.
     */
    private static final String LOGIN_HINT = "login_hint";


    /**
     * If this is provided with the value true, and the authorization request is granted, the authorization will include
     * any previous authorizations granted to this user/application combination
     * for other scopes; see Incremental Authorization.
     */
    private static final String INCLUDE_GRANTED_SCOPES = "include_granted_scopes";

    // Scopes — DES-MODERNIZATION-011 Decision 3 / CNTR-MODERNIZATION-008
    // GMAIL_SCOPE: preserved verbatim (backup/restore dependency).
    // EMAIL_SCOPE + OPENID_SCOPE: replace the withdrawn GData contacts scope.
    // DEFAULT_SCOPE: emitted by requestUrl() into the OAuth consent screen.
    private static final String GMAIL_SCOPE   = "https://mail.google.com/";
    private static final String EMAIL_SCOPE   = "https://www.googleapis.com/auth/userinfo.email";
    private static final String OPENID_SCOPE  = "openid";
    private static final String DEFAULT_SCOPE = GMAIL_SCOPE + " " + EMAIL_SCOPE + " " + OPENID_SCOPE;

    /**
     * As defined in the OAuth 2.0 specification, this field must contain a value of authorization_code.
     */
    private static final String GRANT_TYPE = "grant_type";
    private static final String AUTHORIZATION_CODE = "authorization_code";
    /**
     * The authorization code returned from the initial request.
     */
    private static final String CODE = "code";
    private static final String REFRESH_TOKEN = "refresh_token";

    private final String clientId;

    /**
     * Port that resolves the signed-in account's email from an access token.
     * Best-effort: never throws; may return null (CNTR-MODERNIZATION-008 VR-2/VR-3).
     */
    private final ContactsPort contactsPort;

    /**
     * Primary constructor. Allows the {@link ContactsPort} adapter to be supplied
     * explicitly — used in tests and for the binding-flip reversibility path
     * (DES-MODERNIZATION-011 §Reversibility; IC-3/IC-4).
     *
     * @param clientId     OAuth 2.0 client identifier; must not be empty.
     * @param contactsPort account-email resolution port; must not be null.
     */
    public OAuth2Client(String clientId, ContactsPort contactsPort) {
        if (TextUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("empty client id");
        }
        if (contactsPort == null) {
            throw new IllegalArgumentException("contactsPort must not be null");
        }
        this.clientId = clientId;
        this.contactsPort = contactsPort;
    }

    /**
     * Convenience constructor. Self-supplies {@link PeopleApiContactsAdapter} as the
     * {@link ContactsPort}. Keeps all five existing construction sites source-compatible
     * without modification (DES-MODERNIZATION-011 §Integration Design; AC-7/IC-4).
     *
     * U-023: @Inject annotation added so Dagger/Hilt can build OAuth2Client when requested.
     * The String clientId is provided by EngineModule as @Named("oauth2ClientId").
     * Tests continue to call this constructor directly (OAuth2ClientTest.java:33-36).
     *
     * @param clientId OAuth 2.0 client identifier; must not be empty.
     */
    @Inject
    public OAuth2Client(String clientId) {
        this(clientId, new PeopleApiContactsAdapter());
    }

    public Uri requestUrl() {
        return Uri.parse(AUTH_URL)
            .buildUpon()
            .appendQueryParameter(SCOPE, DEFAULT_SCOPE)
            .appendQueryParameter(CLIENT_ID, clientId)
            .appendQueryParameter(RESPONSE_TYPE, "code")
            .appendQueryParameter(REDIRECT_URI, REDIRECT_URL.toString()).build();
    }

    public OAuth2Token getToken(String code) throws IOException {
        HttpsURLConnection connection = postTokenEndpoint(getAccessTokenPostData(code));
        final int responseCode = connection.getResponseCode();
        if (responseCode == HttpsURLConnection.HTTP_OK) {
            OAuth2Token token = parseResponse(connection.getInputStream());
            // Resolve account email via ContactsPort (best-effort; null is acceptable).
            // CNTR-MODERNIZATION-008: never-throw contract — any exception inside
            // resolveAccountEmail is absorbed by the adapter; username is null on failure.
            String username = contactsPort.resolveAccountEmail(token.accessToken);
            // AC-8 / ARCH-010: resolved email is account PII; gate behind BuildConfig.DEBUG.
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "got token " + token.getTokenForLogging() + ", username=" + username);
            }
            return new OAuth2Token(token.accessToken, token.tokenType, token.refreshToken, token.expiresIn, username);
        } else {
            Log.e(TAG, "error: " + responseCode);
            throw new IOException("Invalid response from server:" + responseCode);
        }
    }

    public OAuth2Token refreshToken(String refreshToken) throws IOException {
        HttpsURLConnection connection = postTokenEndpoint(getRefreshTokenPostData(refreshToken));
        final int responseCode = connection.getResponseCode();
        if (responseCode == HttpsURLConnection.HTTP_OK) {
            return parseResponse(connection.getInputStream());
        } else {
            Log.e(TAG, "error: " + responseCode);
            throw new IOException("Invalid response from server:" + responseCode);
        }
    }

    private OAuth2Token parseResponse(InputStream inputStream) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = inputStream.read(buffer)) != -1) {
            bos.write(buffer, 0, n);
        }
        inputStream.close();
        return OAuth2Token.fromJSON(bos.toString("UTF-8"));
    }

    private HttpsURLConnection postTokenEndpoint(String payload) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(TOKEN_URL).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        final OutputStream os = connection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(payload);
        writer.flush();
        writer.close();
        os.close();
        return connection;
    }

    private String getAccessTokenPostData(String code) {
        final Uri uri = Uri.parse(TOKEN_URL)
            .buildUpon()
            .appendQueryParameter(GRANT_TYPE, AUTHORIZATION_CODE)
            .appendQueryParameter(REDIRECT_URI, REDIRECT_URL.toString())
            .appendQueryParameter(CLIENT_ID, clientId)
            .appendQueryParameter(CODE, code)
            .build();
        return uri.getEncodedQuery();
    }

    private String getRefreshTokenPostData(String refreshToken) {
        final Uri uri = Uri.parse(TOKEN_URL)
            .buildUpon()
            .appendQueryParameter(GRANT_TYPE, REFRESH_TOKEN)
            .appendQueryParameter(REFRESH_TOKEN, refreshToken)
            .appendQueryParameter(CLIENT_ID, clientId)
            .build();
        return uri.getEncodedQuery();
    }
}
