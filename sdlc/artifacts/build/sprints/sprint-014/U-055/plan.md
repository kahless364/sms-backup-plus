---
artifact_type: plan
story_id: U-055
verdict: PASS
agent: Solution Architect
timestamp: "2026-06-23"
---

# Plan: U-055 — Move security-crypto off alpha + encryption-scheme regression test

## Version Selection

**Chosen version: `androidx.security:security-crypto:1.1.0` (stable)**

Justification:
- `security-crypto:1.1.0` stable was released (confirmed locally cached from Google Maven:
  `~/.gradle/caches/modules-2/files-2.1/androidx.security/security-crypto/1.1.0/`).
- It is the first non-alpha/non-beta release on the 1.1.x line.
- `MasterKey.Builder` (including `setRequestStrongBoxBacked()`) is present in the AAR
  (confirmed by inspecting `classes.jar`: `MasterKey$Builder.class` exists).
- `1.0.0` stable does NOT have `MasterKey.Builder` — only the deprecated `MasterKeys` class.
  Moving to `1.0.0` would require a code rewrite and loss of StrongBox best-effort support.
- No code changes are required: API surface for `MasterKey.Builder` and
  `EncryptedSharedPreferences.create()` is identical between `1.1.0-alpha06` and `1.1.0`.
- Data at rest is compatible: both versions use Tink-backed AES-256-GCM/SIV scheme.

## Implementation Steps

1. Update `app/build.gradle:137` from `1.1.0-alpha06` to `1.1.0`.
2. Run `./gradlew --write-verification-metadata sha256 help` to regenerate `gradle/verification-metadata.xml`.
3. Update Javadoc comment in `EncryptedPrefsSecretStore.java` to reflect the stable version.
4. Create `EncryptedPrefsSchemeRegressionTest.java` with 5 tests covering:
   - Constant-name pinning for `PrefKeyEncryptionScheme.AES256_SIV`
   - Constant-name pinning for `PrefValueEncryptionScheme.AES256_GCM`
   - Constant-name pinning for `MasterKey.KeyScheme.AES256_GCM`
   - Builder-capture: production code uses the correct constants
   - Round-trip via SchemeCapturingStore
5. Run full build gate: `assembleDebug + testDebugUnitTest + jacocoTestCoverageVerification`.
