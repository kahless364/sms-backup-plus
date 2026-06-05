---
artifact_type: implementation-log
story_id: U-039
verdict: PASS
agent: Developer
timestamp: 2026-06-04
files_changed: 2
files_created: 0
tests_added: 4
tests_passing: 523
---

# Implementation Log: U-039

## Summary
Fixed BUG-007: raw OAuth2 access token no longer travels in an Activity result Intent extra.
The token is now persisted directly to encrypted storage (via `AuthPreferences.setOauth2Token()`)
inside `AccountManagerAuthActivity.useToken()` BEFORE `setResult()` is called. The consumer
(`MainActivity.handleAccountManagerAuth()`) now reads from `AuthPreferences` rather than the
Intent extra.

## Files Modified

### `app/src/main/java/com/zegoggles/smssync/activity/auth/AccountManagerAuthActivity.java`
- Added `import com.zegoggles.smssync.preferences.AuthPreferences`.
- Added `private AuthPreferences authPreferences` field.
- Initialized `authPreferences = new AuthPreferences(this)` in `onCreate()`.
- `useToken()`: added `authPreferences.setOauth2Token(account.name, token, null)` call BEFORE
  `setResult()` (AC-1).
- Removed `putExtra(EXTRA_TOKEN, token)` from the result Intent in `useToken()` (AC-1).
- Kept `EXTRA_ACCOUNT` in result Intent so `MainActivity` can identify which account was added.
- Deprecated `EXTRA_TOKEN` constant with `@Deprecated` and Javadoc explaining it is no longer
  populated (binary compatibility only).

### `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java`
- `handleAccountManagerAuth()`: removed `EXTRA_TOKEN` read from Intent; replaced condition
  `!TextUtils.isEmpty(token) && !TextUtils.isEmpty(account)` with
  `!TextUtils.isEmpty(account) && authPreferences.hasOAuth2Tokens()`.
- Removed the `setOauth2Token(account, token, null)` call that was previously in
  `handleAccountManagerAuth()` (now done inside `AccountManagerAuthActivity.useToken()`).
- Replaced `EXTRA_TOKEN` static import with a comment explaining the removal.

## Files Created
None.

## Test Results
- 523 total tests; 1 @Ignored (Robolectric/AndroidKeyStore smoke test — pre-existing).
- 4 new tests added in `AuthPreferencesTest`:
  - `u039_ac1_setOauth2Token_persistsTokenInSecretStore_noIntentExtra`
  - `u039_ac2_hasOAuth2Tokens_trueAfterDirectPersistence`
  - `u039_ac3_consumerPathUsesAuthPreferencesNotIntentExtra`
  - `u039_ac2_tokenStoredAndUsableForAuthentication`
- Build: assembleDebug PASS, testDebugUnitTest PASS, jacocoTestCoverageVerification PASS.

## Regression Results
Capabilities inventory (pre-existing behavior preserved):
- RETAINED: AccountManager OAuth flow results in stored token — `authPreferences.setOauth2Token()` now called in `AccountManagerAuthActivity.useToken()` at AccountManagerAuthActivity.java:165
- RETAINED: `AccountAdded` event emitted after successful authentication — MainActivity.java:502-503
- RETAINED: `EXTRA_ACCOUNT` delivered in result Intent so consumer knows which account — AccountManagerAuthActivity.java:167-168
- RETAINED: Error handling paths (`EXTRA_ERROR`, `EXTRA_DENIED`) unchanged
- RETAINED: Webflow path (OAuth2WebAuthActivity) completely untouched
- INTENTIONALLY REMOVED: `EXTRA_TOKEN` placed in result Intent — this was the security vulnerability (BUG-007 / M-001); removed per AC-1

## Integration Path
`AccountManagerAuthActivity.useToken()` → `AuthPreferences.setOauth2Token()` → `SecretStore.put()` → `EncryptedPrefsSecretStore` (production) / `InMemorySecretStore` (test). The token is then read by `MainActivity.handleAccountManagerAuth()` via `authPreferences.hasOAuth2Tokens()` and by `BackupService` via `authPreferences.getOauth2Token()`.

## AC Compliance
- AC-1: Token persisted via `authPreferences.setOauth2Token()` at AccountManagerAuthActivity.java:165; `EXTRA_TOKEN` NOT in result Intent (grep confirmed, no `putExtra(EXTRA_TOKEN` in code).
- AC-2: AccountManager OAuth flow still results in stored token; downstream `getStoreUri()` uses it correctly; webflow path unchanged.
- AC-3: `grep` confirms no `putExtra(EXTRA_TOKEN` on the AccountManager path.
- AC-4: Build green; 4 tests added.
