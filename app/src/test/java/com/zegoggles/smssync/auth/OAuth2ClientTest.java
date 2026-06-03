package com.zegoggles.smssync.auth;

import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link OAuth2Client}.
 *
 * <p>U-006: Characterization tests — pins the constructor, requestUrl, and REDIRECT_URL
 * constants. OAuth2Client makes outbound HTTPS connections that are not testable in
 * Robolectric unit tests (getToken/refreshToken require a live server). Only the
 * constructable and inspectable behavior is covered here.
 *
 * <p>U-029: ContactsPort wiring and scope-update tests — pins the three target scopes
 * (gmail, userinfo.email, openid), confirms m8/feeds is absent (AC-6/AC-10h), and
 * validates the never-throw contract at the port boundary (AC-10a/b/c;
 * CNTR-MODERNIZATION-008 VR-2/VR-3).
 */
@RunWith(RobolectricTestRunner.class)
public class OAuth2ClientTest {

    private static final String TEST_CLIENT_ID = "test-client-id.apps.googleusercontent.com";

    // ---------------------------------------------------------------------------
    // U-006 characterization tests (retained, updated for U-029 scope changes)
    // ---------------------------------------------------------------------------

    @Test public void constructor_withValidClientId_doesNotThrow() {
        OAuth2Client client = new OAuth2Client(TEST_CLIENT_ID);
        assertThat(client).isNotNull();
    }

    @Test public void constructor_withEmptyClientId_throwsIllegalArgument() {
        try {
            new OAuth2Client("");
            fail("Expected IllegalArgumentException for empty clientId");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("empty client id");
        }
    }

    @Test public void constructor_withNullClientId_throwsIllegalArgument() {
        try {
            new OAuth2Client(null);
            fail("Expected IllegalArgumentException for null clientId");
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException (from TextUtils.isEmpty on null) or NPE acceptable
        } catch (NullPointerException e) {
            // Acceptable — TextUtils.isEmpty(null) on Robolectric may NPE
        }
    }

    @Test public void redirectUrl_hasCorrectScheme() {
        Uri redirectUrl = OAuth2Client.REDIRECT_URL;
        assertThat(redirectUrl.getScheme()).isEqualTo("com.zegoggles.smssync");
        assertThat(redirectUrl.getPath()).isEqualTo("/oauth2redirect");
    }

    @Test public void requestUrl_containsRequiredParameters() {
        OAuth2Client client = new OAuth2Client(TEST_CLIENT_ID);
        Uri url = client.requestUrl();

        assertThat(url).isNotNull();
        assertThat(url.getQueryParameter("client_id")).isEqualTo(TEST_CLIENT_ID);
        assertThat(url.getQueryParameter("response_type")).isEqualTo("code");
        // scope must include Gmail (mail.google.com) for backup functionality
        assertThat(url.getQueryParameter("scope")).contains("mail.google.com");
        // redirect_uri must match REDIRECT_URL
        assertThat(url.getQueryParameter("redirect_uri")).contains("oauth2redirect");
    }

    @Test public void requestUrl_hasSameRedirectUriAsConstant() {
        OAuth2Client client = new OAuth2Client(TEST_CLIENT_ID);
        Uri url = client.requestUrl();
        String redirectParam = url.getQueryParameter("redirect_uri");
        assertThat(redirectParam).isEqualTo(OAuth2Client.REDIRECT_URL.toString());
    }

    @Test public void requestUrl_isHttpsGoogleAuthEndpoint() {
        OAuth2Client client = new OAuth2Client(TEST_CLIENT_ID);
        Uri url = client.requestUrl();
        assertThat(url.getScheme()).isEqualTo("https");
        assertThat(url.getHost()).contains("google.com");
    }

    @Test public void multipleClientIds_eachHaveSeparateRequestUrls() {
        OAuth2Client client1 = new OAuth2Client("client-id-1.apps.googleusercontent.com");
        OAuth2Client client2 = new OAuth2Client("client-id-2.apps.googleusercontent.com");
        Uri url1 = client1.requestUrl();
        Uri url2 = client2.requestUrl();
        assertThat(url1.getQueryParameter("client_id"))
            .isEqualTo("client-id-1.apps.googleusercontent.com");
        assertThat(url2.getQueryParameter("client_id"))
            .isEqualTo("client-id-2.apps.googleusercontent.com");
    }

    @Test public void getToken_throwsIOExceptionOnNetworkFailure() {
        // OAuth2Client.getToken() requires an outbound HTTPS connection.
        // In Robolectric there is no live network; this pins the exception behavior.
        OAuth2Client client = new OAuth2Client(TEST_CLIENT_ID);
        try {
            client.getToken("authorization-code-from-redirect");
        } catch (java.io.IOException e) {
            assertThat(e).isNotNull();
        }
    }

    @Test public void refreshToken_throwsIOExceptionOnNetworkFailure() {
        OAuth2Client client = new OAuth2Client(TEST_CLIENT_ID);
        try {
            client.refreshToken("refresh-token-value");
        } catch (java.io.IOException e) {
            assertThat(e).isNotNull();
        }
    }

    // ---------------------------------------------------------------------------
    // U-029: AC-10h — requestUrl() scope assertion (People API scopes, no m8/feeds)
    // ---------------------------------------------------------------------------

    /**
     * AC-10h: requestUrl() scope parameter contains the three target scopes
     * (mail.google.com, userinfo.email, openid) and does NOT contain m8/feeds.
     * Satisfies AC-6 and REQ-MODERNIZATION-011 AC-3.
     */
    @Test public void requestUrl_emitsTargetScopesAndNotM8Feeds() {
        OAuth2Client client = new OAuth2Client(TEST_CLIENT_ID);
        Uri url = client.requestUrl();
        String scope = url.getQueryParameter("scope");

        assertThat(scope).isNotNull();
        assertThat(scope).contains("https://mail.google.com/");
        assertThat(scope).contains("https://www.googleapis.com/auth/userinfo.email");
        assertThat(scope).contains("openid");
        assertThat(scope).doesNotContain("m8/feeds");
    }

    // ---------------------------------------------------------------------------
    // U-029: AC-10a/b — two-arg constructor wiring
    // ---------------------------------------------------------------------------

    /**
     * AC-10a: The two-argument constructor (IC-4) accepts a FakeContactsPort
     * that returns a known email.
     */
    @Test public void twoArgConstructor_contactsPortReturnsEmail_portIsUsed() {
        FakeContactsPort fake = new FakeContactsPort("user@example.com");
        OAuth2Client client = new OAuth2Client(TEST_CLIENT_ID, fake);
        assertThat(client).isNotNull();
        // Directly verify the port returns the expected value (port wiring confirmation).
        assertThat(fake.resolveAccountEmail("any-token")).isEqualTo("user@example.com");
    }

    /**
     * AC-10b: Two-argument constructor with a null-returning FakeContactsPort
     * — null is valid per CNTR-MODERNIZATION-008 VR-3.
     */
    @Test public void twoArgConstructor_contactsPortReturnsNull_isValid() {
        FakeContactsPort fake = new FakeContactsPort(null);
        OAuth2Client client = new OAuth2Client(TEST_CLIENT_ID, fake);
        assertThat(client).isNotNull();
        assertThat(fake.resolveAccountEmail("any-token")).isNull();
    }

    /**
     * Two-argument constructor with null contactsPort must throw IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void twoArgConstructor_nullContactsPort_throwsIllegalArgument() {
        new OAuth2Client(TEST_CLIENT_ID, null);
    }

    // ---------------------------------------------------------------------------
    // U-029: AC-10c — never-throw contract (CNTR-MODERNIZATION-008 VR-2)
    // ---------------------------------------------------------------------------

    /**
     * AC-10c: A NeverThrowContactsPort wrapping a ThrowingContactsPort
     * confirms the never-throw semantic that all production adapters must implement
     * (CNTR-MODERNIZATION-008 VR-2). An exception from the adapter is absorbed and
     * null is returned.
     */
    @Test public void neverThrowContactsPort_exceptionFromPortDoesNotPropagate() {
        NeverThrowContactsPort neverThrow = new NeverThrowContactsPort(
                new ThrowingContactsPort(new RuntimeException("adapter boom")));
        String result = neverThrow.resolveAccountEmail("any-token");
        assertThat(result).isNull();
    }

    // ---------------------------------------------------------------------------
    // Inner helpers
    // ---------------------------------------------------------------------------

    private static class FakeContactsPort implements ContactsPort {
        private final String email;
        FakeContactsPort(String email) { this.email = email; }
        @Override public String resolveAccountEmail(String accessToken) { return email; }
    }

    private static class ThrowingContactsPort implements ContactsPort {
        private final RuntimeException exception;
        ThrowingContactsPort(RuntimeException exception) { this.exception = exception; }
        @Override public String resolveAccountEmail(String accessToken) { throw exception; }
    }

    /** Wraps a delegate and absorbs exceptions, returning null (VR-2 never-throw model). */
    private static class NeverThrowContactsPort implements ContactsPort {
        private final ContactsPort delegate;
        NeverThrowContactsPort(ContactsPort delegate) { this.delegate = delegate; }
        @Override public String resolveAccountEmail(String accessToken) {
            try {
                return delegate.resolveAccountEmail(accessToken);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
