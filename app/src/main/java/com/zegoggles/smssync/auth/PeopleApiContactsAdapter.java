package com.zegoggles.smssync.auth;

import android.text.TextUtils;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import static com.zegoggles.smssync.App.TAG;

/**
 * {@link ContactsPort} adapter that resolves the signed-in Google account's primary
 * email address via the Google People API.
 *
 * <p>Issues {@code GET https://people.googleapis.com/v1/people/me?personFields=emailAddresses}
 * with a Bearer authorization header, parses the {@code emailAddresses} JSON array, and
 * returns the entry where {@code metadata.primary == true} (falling back to index 0 if
 * none is flagged primary). Returns {@code null} on any failure without throwing
 * (CNTR-MODERNIZATION-008 VR-2).
 *
 * <p>Uses {@code javax.net.ssl.HttpsURLConnection} and {@code org.json} — both already
 * present on the Android platform. No People API client SDK is added (DES-MODERNIZATION-011
 * Decision 2).
 *
 * <p>Required OAuth scopes (precondition supplied by the consumer, not enforced here):
 * <ul>
 *   <li>{@code https://www.googleapis.com/auth/userinfo.email}</li>
 *   <li>{@code openid}</li>
 * </ul>
 */
public class PeopleApiContactsAdapter implements ContactsPort {

    private static final String PEOPLE_API_URL =
            "https://people.googleapis.com/v1/people/me?personFields=emailAddresses";

    /**
     * U-048: No-arg @Inject constructor so Hilt can construct this as the @Singleton
     * ContactsPort without any manual {@code new PeopleApiContactsAdapter()} call in
     * modules. ContactsModule binds ContactsPort -> PeopleApiContactsAdapter via @Binds.
     */
    @Inject
    public PeopleApiContactsAdapter() {}

    /**
     * {@inheritDoc}
     *
     * <p>Performs a synchronous HTTPS GET to the People API. Any exception (network,
     * non-200, JSON parse error) is caught and logged internally; {@code null} is
     * returned in all failure cases (CNTR-MODERNIZATION-008 VR-2).
     */
    @Override
    public String resolveAccountEmail(String accessToken) {
        if (TextUtils.isEmpty(accessToken)) {
            Log.w(TAG, "PeopleApiContactsAdapter: null/blank accessToken, returning null");
            return null;
        }
        try {
            HttpsURLConnection connection =
                    (HttpsURLConnection) new URL(PEOPLE_API_URL).openConnection();
            connection.setRequestMethod("GET");
            connection.addRequestProperty("Authorization", "Bearer " + accessToken);

            final int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                Log.w(TAG, "PeopleApiContactsAdapter: unexpected response code " + responseCode);
                return null;
            }

            final InputStream inputStream = connection.getInputStream();
            final String body = readStream(inputStream);
            inputStream.close();
            return parseEmail(body);

        } catch (Exception e) {
            Log.e(TAG, "PeopleApiContactsAdapter: error resolving account email", e);
            return null;
        }
    }

    /**
     * Parse the People API JSON response body and return the primary email address.
     *
     * <p>Selection rules (CNTR-MODERNIZATION-008 VR-1):
     * <ol>
     *   <li>Return the {@code value} of the entry where {@code metadata.primary == true}.</li>
     *   <li>If no entry is flagged primary, return {@code emailAddresses[0].value}.</li>
     *   <li>If the array is absent or empty, return {@code null}.</li>
     *   <li>A non-null return is always a non-empty string (VR-6).</li>
     * </ol>
     *
     * <p>Any parse exception causes {@code null} to be returned (VR-2).
     *
     * @param body raw JSON response body; may be null, blank, or malformed
     * @return resolved email string, or {@code null}
     */
    /* package-private for unit testing */ String parseEmail(String body) {
        if (TextUtils.isEmpty(body)) {
            return null;
        }
        try {
            final JSONObject root = new JSONObject(body);
            final JSONArray addresses = root.optJSONArray("emailAddresses");
            if (addresses == null || addresses.length() == 0) {
                return null;
            }

            // Pass 1: look for primary == true
            for (int i = 0; i < addresses.length(); i++) {
                final JSONObject entry = addresses.optJSONObject(i);
                if (entry == null) continue;
                final JSONObject metadata = entry.optJSONObject("metadata");
                if (metadata != null && metadata.optBoolean("primary", false)) {
                    final String value = entry.optString("value", null);
                    if (!TextUtils.isEmpty(value)) {
                        return value;
                    }
                }
            }

            // Pass 2: fall back to first non-empty entry
            final JSONObject first = addresses.optJSONObject(0);
            if (first != null) {
                final String value = first.optString("value", null);
                if (!TextUtils.isEmpty(value)) {
                    return value;
                }
            }
            return null;

        } catch (Exception e) {
            Log.e(TAG, "PeopleApiContactsAdapter: error parsing JSON response", e);
            return null;
        }
    }

    private static String readStream(InputStream inputStream) throws java.io.IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final byte[] buffer = new byte[8192];
        int n;
        while ((n = inputStream.read(buffer)) != -1) {
            bos.write(buffer, 0, n);
        }
        return bos.toString("UTF-8");
    }
}
