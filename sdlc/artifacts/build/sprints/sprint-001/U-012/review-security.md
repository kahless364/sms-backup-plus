---
artifact_type: review-security
story_id: "U-012"
verdict: "PASS"
agent: "Security Reviewer"
timestamp: "2026-06-03"
blockers: 0
warnings: 1
---

# Security Review: U-012

## Review Summary

U-012 implements the critical at-rest encryption migration for CWE-312 (Cleartext Storage of
Sensitive Information). The implementation correctly executes the rollback-safe step ordering
that ensures no window exists where credentials are unavailable, and ensures the plaintext
is never cleared before ciphertext is durably committed.

## Verdict: PASS

## Findings

### Blockers

None.

### Warnings

1. **Robolectric fallback removal** — Under test environments where Keystore is unavailable,
   `get()` returns null and migration skips. This is the correct behavior for tests; however,
   a real device that somehow lacks Keystore support would also silently skip migration. The
   graceful skip is logged at WARN level which is appropriate — the alternative (crashing)
   would be worse.

### Security Checklist

- [x] **Plaintext credential handling**: Legacy plaintext values are buffered into local
  Java String variables for the minimum time needed (step 2 to step 3 commit). No persistent
  plaintext is written to any new location.

- [x] **Durability barrier**: `editor.commit()` is used (not `apply()`) in step 3 of
  `EncryptedPrefsSecretStore.migrateFromPlaintext()`. This satisfies CNTR-MODERNIZATION-003's
  requirement that `put()` be synchronous and durable.

- [x] **No plaintext residue after migration**: Step 4 removes `login_password`, `oauth2_token`,
  `oauth2_refresh_token` from the raw `credentials` SharedPreferences file. After step 4,
  the backing file contains only AES-256-GCM ciphertext (EncryptedSharedPreferences).

- [x] **Backup exclusion preserved (AC-5, Option A)**: The file name "credentials" is unchanged,
  so the existing `<exclude domain="sharedpref" path="credentials.xml"/>` rule in
  `backup_descriptor.xml` continues to prevent auto-backup of encrypted credentials.

- [x] **Rollback safety**: Crash between step 3 and step 4 leaves the marker in the encrypted
  store. Next-launch idempotency check detects marker and returns immediately. User is never
  unable to authenticate.

- [x] **Crash between step 2 and step 3**: Plaintext still intact, no marker. Next-launch replays
  from intact plaintext. No credential loss.

- [x] **Authentication/authorization**: This is a credential-storage migration, not an auth
  change. No auth flow changes. Public typed accessors on AuthPreferences are unchanged.

- [x] **Data exposure — key names encrypted**: EncryptedSharedPreferences encrypts both keys and
  values with AES-256-SIV (keys) and AES-256-GCM (values). Even the presence/names of stored
  secrets are not disclosed.

- [x] **Keystore-loss error handling**: `get()` in EncryptedPrefsSecretStore returns null on
  decrypt failure (catching Exception broadly). Consumer AuthPreferences treats null as
  "credential unavailable" and routes to re-auth.

- [x] **No hardcoded secrets or credentials**: Implementation contains no secret values. Only
  key name constants are hardcoded.

- [x] **Input validation**: No user-controlled input is parsed during migration. Raw SharedPreferences
  values are read as-is and written as-is through the EncryptedSharedPreferences API.

- [x] **OAuth2 users protected**: By placing the migration call BEFORE the `useXOAuth()` early
  return, OAuth2 tokens (`oauth2_token`, `oauth2_refresh_token`) are also migrated from plaintext
  to AES-256-GCM encrypted storage.

- [x] **CWE-312 closure**: After a completed migration, no file accessible to a forensic tool
  or ADB backup (on a non-rooted device) contains the Gmail IMAP password or OAuth2 tokens
  in cleartext.

## Phase Completion Report
---
story_id: "U-012"
phase: "security-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-001/U-012/review-security.md"
story_status: "done"
current_build_phase: "qa"
blockers: 0
warnings: 1
errors: []
---
