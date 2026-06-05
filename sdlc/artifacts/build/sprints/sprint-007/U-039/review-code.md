---
artifact_type: code-review
story_id: U-039
verdict: PASS
agent: Developer
timestamp: 2026-06-04
---

# Code Review: U-039

## Summary
The fix is minimal and correct. `AccountManagerAuthActivity` now persists the token before
delivering any Intent result, and `MainActivity` reads from `AuthPreferences` rather than
the Intent. No dead code, no orphaned references.

## Review Findings

### Positive
- `authPreferences.setOauth2Token()` is called before `setResult()` — the ordering is
  correct (token is durable before Activity result fires).
- Only `EXTRA_ACCOUNT` (non-sensitive) remains in the result Intent.
- `@Deprecated` annotation on `EXTRA_TOKEN` with explanatory Javadoc prevents future misuse.
- `MainActivity.handleAccountManagerAuth()` correctly uses `hasOAuth2Tokens()` as the
  presence check — this reads from the encrypted store, not from a cleared/stale Intent.
- Webflow path (`OAuth2WebAuthActivity`) is completely untouched.

### Concerns / Notes
- `AccountManagerAuthActivity.useToken()` now has a dependency on `AuthPreferences`. This
  creates a minor tight coupling, but it is the same dependency pattern used in `MainActivity`
  and `App`. Acceptable for the legacy Activity path.
- The `AuthPreferences(this)` single-arg constructor uses `EncryptedPrefsSecretStore` lazily;
  a Keystore failure at this point would cause the token write to fail silently (the store
  catches exceptions in `put()` paths). This pre-existing concern is outside U-039's scope.
- `EXTRA_TOKEN` constant kept for binary-compatibility only; annotated `@Deprecated` correctly.

## No Issues Found
All changes are within the stated scope of U-039. No regressions.
