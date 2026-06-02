package com.zegoggles.smssync.auth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
public class OAuth2TokenTest {
    @Test public void testFromJSON() throws Exception {
        final String response = "{\n" +
                "  \"access_token\":\"1/fFAGRNJru1FTz70BzhT3Zg\",\n" +
                "  \"expires_in\":3920,\n" +
                "  \"token_type\":\"Bearer\",\n" +
                "  \"refresh_token\":\"1/xEoDL4iW3cxlI7yDbSRFYNG01kVKM2C-259HOF2aQbI\"\n" +
                "}";


        final OAuth2Token token = OAuth2Token.fromJSON(response);

        assertThat(token.accessToken).isEqualTo("1/fFAGRNJru1FTz70BzhT3Zg");
        assertThat(token.tokenType).isEqualTo("Bearer");
        assertThat(token.refreshToken).isEqualTo("1/xEoDL4iW3cxlI7yDbSRFYNG01kVKM2C-259HOF2aQbI");
        assertThat(token.expiresIn).isEqualTo(3920);
    }

    @Test public void testFromJSONWithMissingFields() throws Exception {
        final String response = "{\n" +
                "  \"access_token\":\"1/fFAGRNJru1FTz70BzhT3Zg\"\n" +
                "}";
        final OAuth2Token token = OAuth2Token.fromJSON(response);

        assertThat(token.accessToken).isEqualTo("1/fFAGRNJru1FTz70BzhT3Zg");
        assertThat(token.tokenType).isNull();
        assertThat(token.refreshToken).isNull();
        assertThat(token.expiresIn).isEqualTo(-1);
    }

    @Test public void testFromJSONWithoutRefreshToken() throws Exception {
        final String response = "{\n" +
                "  \"access_token\":\"1/fFAGRNJru1FTz70BzhT3Zg\",\n" +
                "  \"expires_in\":3920,\n" +
                "  \"token_type\":\"Bearer\"\n" +
                "}";


        final OAuth2Token token = OAuth2Token.fromJSON(response);

        assertThat(token.accessToken).isEqualTo("1/fFAGRNJru1FTz70BzhT3Zg");
        assertThat(token.tokenType).isEqualTo("Bearer");
        assertThat(token.refreshToken).isNull();
        assertThat(token.expiresIn).isEqualTo(3920);
    }

    @Test public void testTokenForLogging() throws Exception {
        OAuth2Token token = new OAuth2Token("secret", "type", "secret", 100, "Test");
        assertThat(token.getTokenForLogging()).doesNotContain("secret");
        assertThat(token.toString()).doesNotContain("secret");
    }

    // U-006 coverage additions: error paths in fromJSON

    @Test public void testFromJSONWithInvalidJson_throwsIOException() {
        try {
            OAuth2Token.fromJSON("not valid json {{{");
            org.junit.Assert.fail("Expected IOException");
        } catch (java.io.IOException e) {
            // Expected — JSON parse error
        }
    }

    @Test public void testFromJSONWithNonObjectJson_throwsIOException() {
        try {
            OAuth2Token.fromJSON("[\"array\", \"not\", \"object\"]");
            org.junit.Assert.fail("Expected IOException for non-object JSON");
        } catch (java.io.IOException e) {
            assertThat(e.getMessage()).contains("Invalid JSON data");
        }
    }

    @Test public void testFromJSONWithMissingAccessToken_throwsIOException() {
        // access_token is required — missing it should throw
        try {
            OAuth2Token.fromJSON("{\"token_type\": \"Bearer\"}");
            org.junit.Assert.fail("Expected IOException for missing access_token");
        } catch (java.io.IOException e) {
            // Expected — access_token is required by getString()
        }
    }
}
