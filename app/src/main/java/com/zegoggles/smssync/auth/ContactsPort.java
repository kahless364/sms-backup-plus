package com.zegoggles.smssync.auth;

/**
 * Resolves the signed-in Google account's primary email address ("username")
 * from an OAuth access token. The remote account-identity seam that replaces the
 * deprecated GData Contacts feed call (DES-MODERNIZATION-011).
 *
 * <p>Resolution is BEST-EFFORT: implementations return {@code null} on any failure
 * and MUST NOT throw into the caller (CNTR-MODERNIZATION-008 VR-2). This interface
 * is the only type that crosses the boundary; no People API / GData SDK type may
 * appear in its signature (VR-4).
 *
 * <p>This port governs only the remote account-username resolution boundary. It is
 * explicitly disjoint from the local device-contacts path
 * ({@code contacts/ContactAccessor.java}, {@code mail/PersonLookup.java}) — VR-5.
 */
public interface ContactsPort {

    /**
     * Resolve the primary email address for the account that owns {@code accessToken}.
     *
     * @param accessToken a valid OAuth 2.0 bearer access token (the
     *                    {@code OAuth2Token.accessToken} value). Never null when
     *                    called from getToken; implementations treat null/blank as a
     *                    resolution failure and return null.
     * @return the account's primary email address, or {@code null} if resolution
     *         fails for ANY reason (network, HTTP error, auth/scope, empty result,
     *         or parse error). NEVER throws.
     */
    String resolveAccountEmail(String accessToken);
}
