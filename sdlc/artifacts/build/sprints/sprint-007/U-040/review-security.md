---
artifact_type: security-review
story_id: U-040
verdict: PASS
agent: Developer
timestamp: 2026-06-04
---

# Security Review: U-040

## Finding M-002 (BUG-008): RESOLVED

### Vulnerability
Plaintext credentials silently persisted when Keystore failure prevented encryption during
migration. WARN log only, no user-visible signal, no retry signal. Credentials could remain
in plaintext indefinitely.

### Fix Applied
1. A persisted `ENCRYPTION_DEGRADED_KEY` boolean flag is written to `credentials_meta`
   SharedPreferences when `getEncrypted()` throws.
2. `MIGRATION_COMPLETE_KEY` is NOT written on failure — next launch retries migration.
3. `isEncryptionDegraded()` provides a readable state for future backup gating / UI.
4. On successful migration (Keystore recovered), the degraded flag is cleared.

### Security Properties
- Plaintext credentials are NOT cleared on failure (correct: clearing without encryption
  backup would be worse — data loss with no recovery path).
- The degraded flag itself is stored in plaintext (`credentials_meta`). The flag value
  is a boolean health signal, not a credential — no sensitive data is in the flag.
- The `credentials_meta` file name is intentionally distinct from `credentials.xml` so
  the backup exclusion rule (`<exclude domain="sharedpref" path="credentials.xml"/>`)
  does not cover it. This means the flag could be backed up — acceptable because:
  (a) the flag contains no sensitive data, and (b) even if restored false (indicating
  healthy) on a degraded device, the migration will re-attempt and re-set the flag.

### Residual Risk
- No user-visible notification is generated in this story (explicitly deferred per scope).
  The flag is set and readable, but only surfaced to the user if a future story consumes it.
  Until that story ships, users on affected devices will have a log warning but no UI signal.
  This is an improvement over the pre-fix state (no log of the persisted degraded state).
- Backup gating is not implemented — backups will proceed even when degraded. This is the
  current pre-fix behavior; the hook for gating is in place for a future story.

## Conclusion
BUG-008 / M-002 security-medium is substantively resolved. The silent-failure path is
eliminated; the degraded state is surfaced via a persisted flag and log. The deferred
notification and backup gating are explicitly bounded and documented.
