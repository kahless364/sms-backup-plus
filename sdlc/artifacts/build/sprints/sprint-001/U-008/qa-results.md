---
artifact_type: qa-results
story_id: "U-008"
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
---

# QA Results: U-008

## Build Gate Results

| Gate | Command | Result |
|------|---------|--------|
| Compile | `./gradlew :app:compileDebugJavaWithJavac` | PASS |
| Unit Tests | `./gradlew :app:testDebugUnitTest` | PASS (339 tests, 0 failures) |
| Coverage Gate | `./gradlew :app:jacocoTestCoverageVerification` | PASS (≥70% on service*, mail*, auth*) |
| Assemble | `./gradlew :app:assembleDebug` | PASS |

Note: JDK 17 (`JAVA_HOME=/Users/Michael.Horsley/.jdks/jbr-17.0.14`) required; system PATH
has JDK 22 which triggers `-Werror` on obsolete `-source 8 -target 8` warnings. All gates
executed with explicit `JAVA_HOME` pointing to JDK 17.

## Acceptance Criteria Verification

| AC | Criterion | Result |
|----|-----------|--------|
| AC-1 | `TlsTrustPolicy` has exactly `SYSTEM_VALIDATED` and `PINNED_CERTIFICATE`, no third constant | PASS |
| AC-2 | `PinnedCertificateSocketFactory.checkServerTrusted()` non-empty: SHA-256 + validity; `getAcceptedIssuers()` returns one-element array | PASS |
| AC-3 | `PinnedCertStore` uses SharedPreferences `"pinned_certs"` with `MODE_PRIVATE`; `get/put/remove` work correctly | PASS |
| AC-4 | `backup_descriptor.xml` excludes `pinned_certs.xml` | PASS |
| AC-5 | `BackupImapStore` constructor: `(Context, String, TrustedSocketFactory)`; ternary gone; no `AllTrustedSocketFactory` reference | PASS |
| AC-6 | `ServiceBase.getBackupImapStore()` resolves policy via `PinnedCertStore`, builds factory, passes to `BackupImapStore`; no `isTrustAllCertificates()` call | PASS |
| AC-7 | `BackupImapStoreTest` rewritten: all `false` call sites use `DefaultTrustedSocketFactory`; old trust-all test replaced with `PinnedCertificateSocketFactory` assertion | PASS |
| AC-8 | `BackupImapStoreTest` passes (11 tests, 0 failures); `PinnedCertificateSocketFactoryTest` covers match → no exception and mismatch → CertificateException | PASS |
| AC-9 | `PinnedCertStoreTest` covers null-for-unenrolled, round-trip, remove, two-key independence | PASS |
| AC-10 | `grep -rn "AllTrustedSocketFactory" app/src/main/java` returns only the file itself (self-references); no other production file imports/references it | PASS |
| AC-11 | Canonical names `TlsTrustPolicy`, `SYSTEM_VALIDATED`, `PINNED_CERTIFICATE` used verbatim; no synonyms in modified/created production files | PASS |

## Integration Criteria Verification

| IC | Criterion | Result |
|----|-----------|--------|
| IC-1 | `TlsTrustPolicy` in `com.zegoggles.smssync.mail`; importable without circular dependency | PASS |
| IC-2 | `PinnedCertificateSocketFactory` constructor: `(Context, String, X509Certificate)`; no zero-arg or no-cert constructor | PASS |
| IC-3 | `PinnedCertStore` in `com.zegoggles.smssync.mail`; instantiated inline in `ServiceBase` (pre-Hilt pattern) | PASS |
| IC-4 | `BackupImapStore.getTrustedSocketFactory()` preserved with same return type and visibility | PASS |
| IC-5 | All `BackupImapStore` constructor call sites compile against new signature (verified by BUILD SUCCESSFUL) | PASS |

## Test Coverage

| Test Class | Tests | Result |
|------------|-------|--------|
| `BackupImapStoreTest` | 11 | All PASS |
| `PinnedCertificateSocketFactoryTest` | 8 | All PASS |
| `PinnedCertStoreTest` | 10 | All PASS |
| Full suite | 339 | All PASS |

New test methods added: 29 (11 + 8 + 10, subtracting no test methods were removed — the
trust-all test was replaced, not added, so net new = 29 - 1 (replaced) + 18 (new classes) = 28).
More precisely: BackupImapStoreTest went from 10 methods to 11 (+1 new pinned test, -0 
removed since trust-all test was replaced 1-for-1 with pinned test); 
PinnedCertificateSocketFactoryTest = 8 new; PinnedCertStoreTest = 10 new. 
Total new tests = 1 + 8 + 10 = 19 net new.

## Verification Steps Completed

| Step | Verification | Result |
|------|-------------|--------|
| 1 | Compile gate: BUILD SUCCESSFUL | PASS |
| 2 | `TlsTrustPolicy` structure: 2 constants only, no synonyms | PASS |
| 3 | `PinnedCertificateSocketFactory` trust-manager body: SHA-256 + checkValidity + throws | PASS |
| 4 | `PinnedCertStore` prefs name: exactly `"pinned_certs"`, MODE_PRIVATE | PASS |
| 5 | Backup exclusion: `<exclude domain="sharedpref" path="pinned_certs.xml"/>` | PASS |
| 6 | `BackupImapStore` constructor: new sig, no `trustAllCertificates`, no `AllTrustedSocketFactory` | PASS |
| 7 | `ServiceBase` factory-resolution: no `isTrustAllCertificates()`, TlsTrustPolicy referenced, factory is 3rd arg | PASS |
| 8 | No trust-all production reference: `grep -rn AllTrustedSocketFactory app/src/main/java` → only self-references in `AllTrustedSocketFactory.java` | PASS |
| 9 | `BackupImapStoreTest` full run: 11 tests, 0 failures | PASS |
| 10 | `PinnedCertificateSocketFactoryTest` full run: 8 tests, 0 failures | PASS |
| 11 | `PinnedCertStoreTest` full run: 10 tests, 0 failures | PASS |
| 12 | Full suite: 339 tests, 0 failures | PASS |
