---
artifact_type: review-security
story_id: "U-011"
verdict: "PASS"
agent: "Developer (Security Review)"
timestamp: "2026-06-03"
blockers: 0
warnings: 1
---

# Security Review: U-011

## Review Summary

U-011 introduces the encrypted credential storage seam. This review assesses the cryptographic properties, backup exclusion, key material handling, error surfacing, and test-environment fallback security posture.

## Verdict: PASS

## Findings

### Blockers

None.

### Warnings

**SEC-W-1: PlaintextSharedPrefsSecretStore stores credentials unencrypted in test environments**

The `buildEncryptedStoreSafe()` fallback constructs a `PlaintextSharedPrefsSecretStore` when the Android Keystore is unavailable. On a real device this path is never taken. In CI/test environments, non-credential tests that trigger `App.onCreate()` will use the plaintext fallback for the `AuthPreferences` instance created by `Preferences.migrate()`. Since no credential I/O occurs in those test paths (they test state machines, scheduler logic, etc.), no actual secrets are written to the plaintext fallback file.

The risk is bounded:
- The file name `"credentials_fallback"` is distinct from `"credentials"`, so there is no overlap with the production encrypted file
- No test writes real user credentials to this store; test credentials are written via `InMemorySecretStore` injection
- The class is `@Deprecated` and the fallback is logged at WARN level

Recommendation: U-012 may add a Robolectric shadow for AndroidKeyStore (or a test application subclass that overrides `buildEncryptedStoreSafe()`), eliminating this fallback entirely. This is out of scope for U-011.

### Security Checklist

- [x] **Input validation**: SecretStore keys and values are `String`. `EncryptedSharedPreferences` handles null values (null is a valid stored value); no additional validation required at the port level.
- [x] **Path traversal prevention**: File name `"credentials"` is a hardcoded constant. Android's `SharedPreferences` file system API does not accept path traversal in file names.
- [x] **Authentication/authorization**: The Android Keystore provides hardware-backed authentication. `setRequestStrongBoxBacked(true)` is a best-effort hint for StrongBox-backed keys (silent fallback to TEE or software if unavailable). The master key is non-exportable by design (`MasterKey.KeyScheme.AES256_GCM`).
- [x] **Data exposure at rest (CWE-312)**: Three secrets (`login_password`, `oauth2_token`, `oauth2_refresh_token`) are encrypted with AES-256-GCM at rest. Key names are encrypted with AES-256-SIV. The `backup_descriptor.xml` excludes `credentials.xml` from Android auto-backup (verified unchanged).
- [x] **Data exposure in memory**: Credentials are held in memory as `String` during in-flight operations. This is the same as the pre-U-011 behavior; no regression. Long-term zeroing of Strings in memory is out of scope.
- [x] **Error handling (no information leakage)**: `EncryptedPrefsSecretStore.get()` catches all exceptions and returns `null`. The only information logged is the exception class name (not the message, which might contain key material). No stacktrace is logged for decrypt failures.
- [x] **commit() durability**: All write operations use `.commit()`, ensuring atomic flush. No `.apply()` calls exist in `EncryptedPrefsSecretStore` (verified by code search).
- [x] **Backup exclusion invariant**: Option A (file name `"credentials"`) preserves the existing `<exclude domain="sharedpref" path="credentials.xml"/>` rule without any descriptor edit. The encrypted file is never included in Android auto-backup.
- [x] **No hardcoded secrets**: Zero hardcoded credentials or cryptographic keys in any new file.
- [x] **No plaintext credential logging**: `EncryptedPrefsSecretStore.get()` logs only the key name on failure (not the value). Key names (`"login_password"`, etc.) are not sensitive.
- [x] **Key material does not leave Keystore**: `MasterKey.KeyScheme.AES256_GCM` with Keystore backing ensures the master key is non-exportable. EncryptedSharedPreferences uses the Tink library internally to wrap/unwrap data encryption keys.

### Cryptographic Assessment

| Property | Value | Requirement | Met? |
|----------|-------|-------------|------|
| Value encryption | AES-256-GCM (authenticated) | CNTR-003 §Cryptographic guarantee | Yes |
| Key encryption | AES-256-SIV (deterministic) | CNTR-003 §Cryptographic guarantee | Yes |
| Master key algorithm | AES-256-GCM | CNTR-003 §Cryptographic guarantee | Yes |
| Master key storage | Android Keystore (non-exportable) | CNTR-003 §Cryptographic guarantee | Yes |
| StrongBox | Best-effort (hardware-backed if available) | AC-2 / CNTR-003 | Yes |
| Backup exclusion | credentials.xml excluded | CNTR-003 §Backing-store invariant | Yes |

## Phase Completion Report
---
story_id: "U-011"
phase: "security-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-001/U-011/review-security.md"
story_status: "done"
current_build_phase: "validation"
blockers: 0
warnings: 1
errors: []
---
