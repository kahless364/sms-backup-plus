package com.zegoggles.smssync.auth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;

/**
 * Unit tests for {@link PeopleApiContactsAdapter#parseEmail(String)} — the People API
 * JSON parsing logic. The HTTP layer is NOT reached; we supply raw JSON fixture strings
 * directly to {@code parseEmail} (package-private for testing).
 *
 * <p>Covers AC-10d/e/f/g (story U-029) and CNTR-MODERNIZATION-008 VR-1/VR-2/VR-6.
 *
 * <p><strong>Live-account verification gap:</strong> The actual HTTP call to
 * {@code https://people.googleapis.com/v1/people/me?personFields=emailAddresses}
 * cannot be verified without a live Google account and network access. The integration
 * path ({@code PeopleApiContactsAdapter.resolveAccountEmail} → HTTP GET → JSON parse
 * → return) is exercised end-to-end only by the manual step documented in
 * U-029/qa-results.md.
 */
@RunWith(RobolectricTestRunner.class)
public class PeopleApiContactsAdapterTest {

    private PeopleApiContactsAdapter adapter;

    @Before
    public void setUp() {
        adapter = new PeopleApiContactsAdapter();
    }

    // ---------------------------------------------------------------------------
    // AC-10d — primary-preferred selection (two entries, one primary)
    // ---------------------------------------------------------------------------

    /**
     * AC-10d: When one entry has {@code metadata.primary == true}, that entry's value
     * is returned (VR-1 primary preference).
     */
    @Test
    public void parseEmail_primaryEntry_returnsPrimaryValue() {
        String json = "{"
                + "\"resourceName\":\"people/c123\","
                + "\"emailAddresses\":["
                + "  {\"metadata\":{\"primary\":false},\"value\":\"secondary@example.com\"},"
                + "  {\"metadata\":{\"primary\":true},\"value\":\"primary@example.com\"}"
                + "]}";

        String result = adapter.parseEmail(json);

        assertThat(result).isEqualTo("primary@example.com");
    }

    // ---------------------------------------------------------------------------
    // AC-10e — no primary flag — returns first entry value
    // ---------------------------------------------------------------------------

    /**
     * AC-10e: When no entry has {@code metadata.primary == true}, the first entry's
     * value is returned (VR-1 fallback).
     */
    @Test
    public void parseEmail_noPrimaryFlag_returnsFirstEntry() {
        String json = "{"
                + "\"resourceName\":\"people/c123\","
                + "\"emailAddresses\":["
                + "  {\"metadata\":{\"primary\":false},\"value\":\"first@example.com\"},"
                + "  {\"metadata\":{\"primary\":false},\"value\":\"second@example.com\"}"
                + "]}";

        String result = adapter.parseEmail(json);

        assertThat(result).isEqualTo("first@example.com");
    }

    /**
     * AC-10e variant: When the {@code metadata} object is absent entirely, falls back
     * to the first entry.
     */
    @Test
    public void parseEmail_noMetadataField_returnsFirstEntry() {
        String json = "{"
                + "\"emailAddresses\":["
                + "  {\"value\":\"first@example.com\"},"
                + "  {\"value\":\"second@example.com\"}"
                + "]}";

        String result = adapter.parseEmail(json);

        assertThat(result).isEqualTo("first@example.com");
    }

    // ---------------------------------------------------------------------------
    // AC-10f — empty emailAddresses array returns null
    // ---------------------------------------------------------------------------

    /**
     * AC-10f: Empty {@code emailAddresses} array returns {@code null} (VR-1/VR-6).
     */
    @Test
    public void parseEmail_emptyEmailAddressesArray_returnsNull() {
        String json = "{\"resourceName\":\"people/c123\",\"emailAddresses\":[]}";

        String result = adapter.parseEmail(json);

        assertThat(result).isNull();
    }

    /**
     * AC-10f variant: Absent {@code emailAddresses} key returns {@code null}.
     */
    @Test
    public void parseEmail_missingEmailAddressesKey_returnsNull() {
        String json = "{\"resourceName\":\"people/c123\"}";

        String result = adapter.parseEmail(json);

        assertThat(result).isNull();
    }

    // ---------------------------------------------------------------------------
    // AC-10g — malformed / empty body returns null without throwing
    // ---------------------------------------------------------------------------

    /**
     * AC-10g: Malformed JSON returns {@code null} without throwing (VR-2).
     */
    @Test
    public void parseEmail_malformedJson_returnsNullWithoutThrowing() {
        String result = adapter.parseEmail("{not valid json{{{{");

        assertThat(result).isNull();
    }

    /**
     * AC-10g variant: Null body returns {@code null} without throwing (VR-2).
     */
    @Test
    public void parseEmail_nullBody_returnsNull() {
        String result = adapter.parseEmail(null);

        assertThat(result).isNull();
    }

    /**
     * AC-10g variant: Empty string body returns {@code null} without throwing (VR-2).
     */
    @Test
    public void parseEmail_emptyBody_returnsNull() {
        String result = adapter.parseEmail("");

        assertThat(result).isNull();
    }

    /**
     * AC-10g variant: JSON array (not object) returns {@code null} without throwing.
     */
    @Test
    public void parseEmail_jsonArrayBody_returnsNull() {
        String result = adapter.parseEmail("[\"not\",\"an\",\"object\"]");

        assertThat(result).isNull();
    }

    // ---------------------------------------------------------------------------
    // VR-6 — non-null return is always non-empty
    // ---------------------------------------------------------------------------

    /**
     * VR-6: A single entry with a valid non-empty value returns that value (not empty).
     */
    @Test
    public void parseEmail_singleValidEntry_returnsNonEmptyValue() {
        String json = "{"
                + "\"emailAddresses\":["
                + "  {\"metadata\":{\"primary\":true},\"value\":\"user@example.com\"}"
                + "]}";

        String result = adapter.parseEmail(json);

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).isEqualTo("user@example.com");
    }

    // ---------------------------------------------------------------------------
    // resolveAccountEmail — null/blank token returns null (VR-2)
    // ---------------------------------------------------------------------------

    /**
     * resolveAccountEmail with null token returns null without throwing (VR-2).
     * No HTTP call is issued.
     */
    @Test
    public void resolveAccountEmail_nullToken_returnsNull() {
        String result = adapter.resolveAccountEmail(null);

        assertThat(result).isNull();
    }

    /**
     * resolveAccountEmail with blank token returns null without throwing (VR-2).
     */
    @Test
    public void resolveAccountEmail_blankToken_returnsNull() {
        String result = adapter.resolveAccountEmail("   ");

        assertThat(result).isNull();
    }
}
