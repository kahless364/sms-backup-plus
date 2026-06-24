---
artifact_type: review-security
story_id: "U-055"
verdict: "PASS"
agent: "Security Reviewer"
timestamp: "2026-06-24"
blockers: 0
warnings: 1
---

# Security Review: U-055

## Review Summary

U-055 upgrades `androidx.security:security-crypto` from `1.1.0-alpha06` to `1.1.0` stable. The encryption scheme is unchanged (AES-256-GCM master key, AES256_SIV key encryption, AES256_GCM value encryption via Tink). StrongBox best-effort is retained. SHA-256 checksums for the new artifact and all transitive dependencies are pinned in `gradle/verification-metadata.xml`. The scheme regression test adds a CI guard against future scheme weakening. One low-severity observation about the regression test's coverage fidelity is documented below.

## Verdict: PASS

No blocking security issues. The dependency upgrade eliminates the alpha-channel supply-chain risk without weakening the cryptographic scheme or the StrongBox-backed key derivation. The verification-metadata update provides supply-chain integrity via SHA-256 pinning.

## Findings

### Blockers

None.

### Warnings

#### Low — Scheme regression test captures intent via constant mirroring, not production code delegation

**File:** `app/src/test/java/com/zegoggles/smssync/preferences/EncryptedPrefsSchemeRegressionTest.java`, `SchemeCapturingStore.getEncrypted()` (lines 193-213)

**Description:** `SchemeCapturingStore.getEncrypted()` overrides the production method and re-declares the scheme constants locally (`PrefKeyEncryptionScheme.AES256_SIV`, `PrefValueEncryptionScheme.AES256_GCM`) rather than calling `super.getEncrypted()`. This means the test captures what the *test subclass* passes, not what the production `EncryptedPrefsSecretStore.getEncrypted()` passes. If a future developer modifies `EncryptedPrefsSecretStore.getEncrypted()` to use a different scheme constant but forgets to update `SchemeCapturingStore`, the builder-capture tests (2 of the 5 scheme tests) would continue to pass while the production code regresses.

**Mitigating factors:** The three constant-pinning tests (`schemeRegression_prefKeyEncryptionScheme_mustBeAES256_SIV`, `schemeRegression_prefValueEncryptionScheme_mustBeAES256_GCM`, `schemeRegression_masterKeyScheme_mustBeAES256_GCM`) check that the constants *exist by name* — they would fail only if the library renames/removes the constant, not if the production code switches to a different one. The production code (`EncryptedPrefsSecretStore.java:110-121`) is directly readable and unambiguously passes `AES256_SIV` and `AES256_GCM`. A compile-time check (asserting the production class references these constants) would provide stronger protection, but the current approach is honest about its scope and is significantly better than no scheme guard at all.

**Severity:** Low. The production code at `EncryptedPrefsSecretStore.java:110-121` is clear and correct. The limitation is in the test's coverage fidelity, not in the production cryptographic implementation.

**Recommendation:** In a future test-quality story, refactor `SchemeCapturingStore.getEncrypted()` to record the specific constant values passed and then delegate to a fake that avoids the Keystore call, or add a separate static analysis check that asserts the production class references only the approved scheme constants.

### Security Checklist

- [x] No hardcoded credentials or secrets — version bump only; no credential values appear in any changed file
- [x] Input validation on all user inputs — no new input surfaces introduced
- [x] Output encoding prevents injection attacks — no output rendering changes
- [x] Authentication tokens handled securely — `EncryptedPrefsSecretStore.getEncrypted()` scheme unchanged: AES256_GCM master key (line 111), AES256_SIV key encryption (line 119), AES256_GCM value encryption (line 120); StrongBox best-effort `setRequestStrongBoxBacked(true)` (line 112) retained
- [x] Authorization checks on all protected resources — no authorization logic touched
- [x] Sensitive data encrypted at rest and in transit — encryption scheme is AES-256-GCM/SIV via Tink 1.8.0 (upgraded from 1.7.x; improved implementation, identical scheme); data-at-rest compatibility with alpha06 preserved
- [x] Error messages do not leak internal details — no error-handling changes
- [x] Dependencies have no known critical vulnerabilities — `security-crypto:1.1.0` stable replaces alpha06; Tink upgraded from 1.7.x to 1.8.0 (security improvement); `jspecify:1.0.0` is a null-safety annotation library with no runtime security surface; `androidx.collection:1.4.2` and `androidx.annotation:1.8.1` are utility libraries with no known CVEs at review time

## Detailed Verification

### Encryption scheme unchanged (AC-3)

`EncryptedPrefsSecretStore.getEncrypted()` at lines 110-121 (current HEAD):

- `MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).setRequestStrongBoxBacked(true).build()` — master key scheme: AES-256-GCM, StrongBox best-effort.
- `EncryptedSharedPreferences.create(..., PrefKeyEncryptionScheme.AES256_SIV, PrefValueEncryptionScheme.AES256_GCM)` — key encryption: AES-256-SIV (deterministic, collision-resistant), value encryption: AES-256-GCM (AEAD).

This code was not modified by U-055 (only the Javadoc was updated). The scheme is identical between the pre-U-055 and post-U-055 state. The Tink upgrade from 1.7.x to 1.8.0 uses the same key material format and is backward-compatible at the ciphertext level.

### Supply-chain integrity (AC-2)

`gradle/verification-metadata.xml` additions:
- `androidx.security:security-crypto:1.1.0` AAR SHA-256: `2831ec1b455e8cc2ff861e8384d7893b42b3307c00f3be843feb02af8ac04b56`
- `androidx.security:security-crypto:1.1.0` module SHA-256: `2a04f7f97f61d201ae384082cb016a3cd58e13e3496b051fe735c0f68cd8fb49`
- New transitives: `tink-android:1.8.0`, `jspecify:1.0.0`, `collection:1.4.2`, `annotation:1.8.1` — all SHA-256 pinned.
- `verify-metadata: true` (line 4) ensures Gradle validates these checksums on every build.
- The `trusted-artifacts` configuration trusts `.*-javadoc.jar` and `.*-sources.jar` via regex — this is the pre-existing policy and does not apply to runtime artifacts.
- `verify-signatures: false` (line 5) is the pre-existing project configuration; this means PGP signatures are not verified, but SHA-256 hash verification still blocks substitution attacks.
- Alpha06 entries were retained after regeneration (correct behavior; Gradle does not remove superseded versions automatically).

### No wildcard dependency trust

The `<trusted-artifacts>` regex patterns (`.*-javadoc[.]jar` and `.*-sources[.]jar`) match only documentation and source JARs, not AAR or runtime JAR artifacts. The new `security-crypto:1.1.0.aar` and its transitives all have explicit SHA-256 entries — no wildcard pattern covers them. Supply-chain verification is specific.

### Version is non-alpha/non-beta (AC-1)

`app/build.gradle:137` declares `implementation 'androidx.security:security-crypto:1.1.0'`. No `alpha`, `beta`, or `rc` qualifier. This is the first stable release on the 1.1.x line. The `1.0.0` stable line was correctly rejected because it lacks the `MasterKey.Builder.setRequestStrongBoxBacked()` API.
