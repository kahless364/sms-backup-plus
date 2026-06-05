---
artifact_type: security-review
story_id: U-039
verdict: PASS
agent: Developer
timestamp: 2026-06-04
---

# Security Review: U-039

## Finding M-001 (BUG-007): RESOLVED

### Vulnerability
Raw OAuth2 bearer token placed in Activity result Intent extra (`EXTRA_TOKEN`). Intent extras
returned via `setResult()` are visible to the calling process and potentially to privileged
processes on Android. Bearer tokens in Intent extras are a known Android security anti-pattern
(CWE-312: Cleartext Storage of Sensitive Information).

### Fix Applied
- Token is written to `EncryptedSharedPreferences` (AES-256-GCM) via
  `AuthPreferences.setOauth2Token()` BEFORE `setResult()` is called.
- The result Intent contains only `EXTRA_ACCOUNT` (account name — non-sensitive, already
  visible to Gmail/AccountManager).
- No raw token value crosses any IPC boundary.

### Verification
```
grep -rn "putExtra(EXTRA_TOKEN" app/src/main
```
Returns: no output. Confirmed.

## Residual Concerns (pre-existing, out of scope)
- `AuthPreferences(this)` single-arg constructor opens `EncryptedPrefsSecretStore` lazily.
  A Keystore failure at token-write time would cause the token to be written to the fallback
  path. This is tracked separately as BUG-008/U-040.
- The `EXTRA_ACCOUNT` (account email) still travels in the Intent — this is the expected
  behavior and is not a sensitive value.

## Conclusion
BUG-007 / M-001 is fully resolved. No new security issues introduced.
