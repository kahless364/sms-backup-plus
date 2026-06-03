---
artifact_type: review-security
story_id: U-029
verdict: PASS
agent: Developer
timestamp: "2026-06-02"
---

# Security Review: U-029 — ContactsPort + People API Adapter

## Summary

No new security risks introduced. Existing PII concern (account email logged to logcat) is now closed by gating behind `BuildConfig.DEBUG`. People API call uses HTTPS only (`HttpsURLConnection`). No new secrets, credentials, or permissions introduced.

## Findings

### PII Log Gate (ARCH-010 — CLOSED)

The log statement `Log.d(TAG, "got token ..., username="+username)` emitted the signed-in account email in all build types, including release. This is now gated:

```java
if (BuildConfig.DEBUG) {
    Log.d(TAG, "got token " + token.getTokenForLogging() + ", username=" + username);
}
```

In release builds (`BuildConfig.DEBUG == false`) the email is not written to logcat. ARCH-010 is closed.

### HTTPS-only Transport

`PeopleApiContactsAdapter` uses `javax.net.ssl.HttpsURLConnection` (not `HttpURLConnection`). The URL `https://people.googleapis.com/...` is TLS-only. No plaintext HTTP path exists in the new adapter.

### Access Token Handling

The access token is passed as `Authorization: Bearer <accessToken>` in the request header, consistent with the existing `OAuth2Client` style (verified `getUsernameFromContacts` line 215 used the same pattern). The token is not logged.

### No New Permissions

The People API `people.get` call uses the existing OAuth scopes (`userinfo.email` + `openid`). No new Android permissions are required. The permission model is unchanged.

### Scope Reduction (Positive)

The withdrawn `https://www.google.com/m8/feeds/` scope is removed from `DEFAULT_SCOPE`. This reduces the consent surface — users will no longer be prompted for the deprecated contacts scope. The new scopes (`userinfo.email`, `openid`) are narrower in their contact-data exposure.

### GDataContactsAdapter (Transitional)

Contains the deprecated m8/feeds URL but is only reachable via explicit binding flip (`new OAuth2Client(clientId, new GDataContactsAdapter())`). It is not used by default in production code. Scheduled for deletion after People API verification.

### No Hardcoded Credentials

No secrets, API keys, client IDs, or tokens are hardcoded in any of the new files.

## Verdict

PASS. ARCH-010 closed. No new security risks.
