---
type: story
status: planned
artifact_type: user-story
priority: low
complexity: low
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-055
title: Move androidx.security-crypto off alpha and add an encryption-scheme regression test
pipeline: ''
domain: modernization
requirement_source: assessment:20260623-post-migration-assessment#SE-003
sprint: '000014'
---

# U-055: Move androidx.security-crypto off alpha and add an encryption-scheme regression test

## Story
As a maintainer of the SMS Backup+ codebase, I want `androidx.security:security-crypto` upgraded from `1.1.0-alpha06` to the latest stable release and a regression test added that pins the AES-256-GCM/SIV encryption scheme, so that the credential-encryption critical path no longer depends on an alpha artifact and any future accidental scheme downgrade is caught by CI.

## Source
Derived from assessment 20260623-post-migration-assessment, findings SE-003 and BT-004 (merged). See `sdlc/analysis/20260623-post-migration-assessment/outputs/sms-backup-plus/security.md` and `build-toolchain.md`.

## Acceptance Criteria
- [ ] AC-1: `app/build.gradle:137` is updated from `androidx.security:security-crypto:1.1.0-alpha06` to the latest stable `1.x` release (e.g. `1.0.0` or the highest non-alpha/non-beta release available at implementation time); no `alpha` or `beta` qualifier remains in the declared version.
- [ ] AC-2: `gradle/verification-metadata.xml` is updated with the SHA-256 checksums for the new stable artifact and all transitive dependencies it introduces or modifies; `./gradlew --write-verification-metadata sha256 help` is used to regenerate, and the result committed.
- [ ] AC-3: A unit test (JVM/Robolectric, runnable under `testDebugUnitTest`) asserts that `EncryptedPrefsSecretStore` constructs its `MasterKey` using `AES256_GCM` key scheme and creates `EncryptedSharedPreferences` with `AES256_SIV` key encryption and `AES256_GCM` value encryption; any future change to a weaker scheme causes the test to fail.
- [ ] AC-4: No credential-migration regression: the existing `EncryptedPrefsSecretStoreDegradedTest` tests all pass without modification, confirming the stable release is API-compatible with the alpha for the APIs the app uses.
- [ ] AC-5: Build green: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` passes with the new dependency version and the scheme-pinning test present.

## Affected Code
| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/build.gradle:137` | `implementation 'androidx.security:security-crypto:1.1.0-alpha06'` | Bump to latest stable `1.x` release |
| `gradle/verification-metadata.xml` | SHA-256 pinned for `security-crypto:1.1.0-alpha06` and its transitives | Update to pin the new stable version's checksums |
| `app/src/test/java/.../auth/` (new test file) | No encryption-scheme pinning test exists | Add `EncryptedPrefsSchemeRegressionTest` (or similar) asserting AES-256-GCM/SIV scheme constants |

## Existing Behavior to Preserve
- Credential encryption at rest: credentials stored by `EncryptedPrefsSecretStore` using `1.1.0-alpha06` must remain readable after the version bump — `EncryptedSharedPreferences` must migrate transparently, as both alpha and stable use the same Tink-backed AES-256-GCM/SIV scheme at the data layer.
- StrongBox-best-effort `MasterKey` construction (`EncryptedPrefsSecretStore.java:109-120`) must remain unchanged; the API surface used (`MasterKey.Builder`, `EncryptedSharedPreferences.create`) must be available in the stable release.
- The `commit()` durability guarantee for credential writes must not change.
- The rollback-safe migration path (`EncryptedPrefsSecretStore.java:273-285`) must be verified unbroken by the existing `EncryptedPrefsSecretStoreDegradedTest`.

## Verification Steps
1. Run `./gradlew dependencies --configuration debugRuntimeClasspath | grep security-crypto` — confirm the resolved version is the new stable release with no `alpha`/`beta` qualifier.
2. Run `./gradlew :app:testDebugUnitTest` — `EncryptedPrefsSecretStoreDegradedTest` passes; new scheme-pinning test passes.
3. Run `./gradlew :app:assembleDebug` — build succeeds; no API-compatibility errors from `security-crypto` stable vs alpha.
4. Run `./gradlew --write-verification-metadata sha256 help` and confirm `gradle/verification-metadata.xml` is updated; then run `./gradlew :app:assembleDebug` with metadata verification active and confirm it passes (no SHA-256 mismatch).

## Technical Context
- SE-003 / BT-004 root cause: `security-crypto:1.1.0-alpha06` was pinned because the `1.0.x` stable line lacked the `MasterKey.Builder` API that the app uses. As of `1.1.0-alpha06`, Tink-backed `EncryptedSharedPreferences` with AES-256-GCM/SIV is the scheme for both alpha and stable branches. Check the AndroidX release notes to confirm the stable `1.x` version that graduated the `MasterKey.Builder` API (likely `1.1.0` stable if released, otherwise check `1.0.0` API surface compatibility).
- The scheme-pinning test should use Robolectric to instantiate `EncryptedSharedPreferences` in-JVM and assert the key/value encryption scheme constants (`EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV`, `PrefValueEncryptionScheme.AES256_GCM`) are what the `EncryptedPrefsSecretStore` passes; it does not need to actually write to Keystore.

## Notes
- Sprint C (Security Hardening). SE-003 and BT-004 are the same alpha-dep finding viewed from Security and Build dimensions respectively; merged into this single story per the health report's de-duplication note.
- Low effort (S). Can run in parallel with other Sprint C items.
