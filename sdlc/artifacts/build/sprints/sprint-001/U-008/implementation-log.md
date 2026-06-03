---
artifact_type: implementation-log
story_id: "U-008"
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
files_changed: 4
files_created: 5
tests_added: 29
tests_passing: 339
---

# Implementation Log: U-008

## Summary

Introduced the TLS trust-policy layer per CNTR-MODERNIZATION-001 and DES-MODERNIZATION-002.
Three new production classes, two modified production files, one XML update, one rewritten test
file, and two new test classes. All three verification gates pass:
- `./gradlew :app:compileDebugJavaWithJavac` — BUILD SUCCESSFUL
- `./gradlew :app:testDebugUnitTest` — BUILD SUCCESSFUL (339 total tests, 0 failures)
- `./gradlew :app:jacocoTestCoverageVerification` — BUILD SUCCESSFUL (≥70% gate holds)
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL

## Files Created

### Production

1. `app/src/main/java/com/zegoggles/smssync/mail/TlsTrustPolicy.java`
   - Two-constant enum: `SYSTEM_VALIDATED` and `PINNED_CERTIFICATE`
   - No third constant; satisfies CNTR-MODERNIZATION-001 Validation Rule 1
   - Package: `com.zegoggles.smssync.mail` (IC-1)

2. `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java`
   - Implements `com.fsck.k9.mail.ssl.TrustedSocketFactory`
   - Constructor: `public PinnedCertificateSocketFactory(Context context, String host, X509Certificate enrolledCert)` (IC-2)
   - Inner `PinnedX509TrustManager.checkServerTrusted()`: extracts `chain[0]`, computes SHA-256 of both presented and enrolled cert, compares with `Arrays.equals()`, then calls `enrolledCert.checkValidity()` (AC-2)
   - `getAcceptedIssuers()`: returns `new X509Certificate[]{enrolledCert}` — never null, never empty (AC-2)
   - Package-private test seam `getTrustManagerForTesting()` (IC-4 pattern)

3. `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertStore.java`
   - SharedPreferences file name: `"pinned_certs"`, mode `Context.MODE_PRIVATE` (AC-3)
   - Key format: `"host:port"` (e.g., `"mail.example.org:993"`)
   - `get(host, port)`: returns `null` for unenrolled key; decodes Base64-DER otherwise
   - `put(host, port, cert)`: DER-encodes, Base64-encodes, writes under key
   - `remove(host, port)`: removes key
   - `getTlsTrustPolicy(host, port)`: returns `PINNED_CERTIFICATE` if key present, `SYSTEM_VALIDATED` otherwise (Invariant 5)
   - Package constant `PREFS_NAME = "pinned_certs"` for test verification

### Tests

4. `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryTest.java`
   - 8 `@Test` methods covering AC-8 and AC-2 invariants
   - Two core tests: enrolled cert → no exception; different cert → CertificateException
   - Additional: null chain → CertificateException; getAcceptedIssuers not null/not empty / contains enrolled; constructor with null → IllegalArgumentException

5. `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertStoreTest.java`
   - 10 `@Test` methods covering all of AC-9 (a), (b), (c), (d) plus policy resolution and prefs file name
   - AC-9(a): `get()` returns null for unenrolled key
   - AC-9(b): `put()` + `get()` returns byte-equal certificate
   - AC-9(c): `remove()` + `get()` returns null
   - AC-9(d): two distinct keys are independent; removing one does not affect the other
   - Verifies prefs file name is exactly `"pinned_certs"` and MODE_PRIVATE is used

## Files Modified

1. `app/src/main/java/com/zegoggles/smssync/mail/BackupImapStore.java`
   - Constructor changed from `(Context, String, boolean trustAllCertificates)` to `(Context, String, TrustedSocketFactory socketFactory)` (AC-5, IC-5)
   - Removed inline ternary `trustAllCertificates ? AllTrustedSocketFactory.INSTANCE : new DefaultTrustedSocketFactory(context)`
   - Removed `import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory`
   - Super call: `super(new BackupStoreConfig(uri), socketFactory, (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE))`
   - `AllTrustedSocketFactory` not referenced anywhere in the file (AC-10)
   - `getTrustedSocketFactory()` test seam preserved at same location and visibility (IC-4)

2. `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java`
   - Added imports: `android.net.Uri`, `DefaultTrustedSocketFactory`, `TrustedSocketFactory`, `PinnedCertStore`, `PinnedCertificateSocketFactory`, `TlsTrustPolicy`
   - `getBackupImapStore()` rewritten: parses URI for host:port; instantiates `PinnedCertStore`; calls `getTlsTrustPolicy(host, port)`; selects `new DefaultTrustedSocketFactory(context)` for `SYSTEM_VALIDATED` or `new PinnedCertificateSocketFactory(context, host, pinnedCertStore.get(host, port))` for `PINNED_CERTIFICATE`; passes resolved factory to `new BackupImapStore(context, uri, factory)` (AC-6)
   - `isTrustAllCertificates()` no longer called for socket-factory selection (AC-6)
   - `throws MessagingException` declaration preserved; URI validation preserved (AC-6)

3. `app/src/main/res/xml/backup_descriptor.xml`
   - Added `<exclude domain="sharedpref" path="pinned_certs.xml"/>` (AC-4)

4. `app/src/test/java/com/zegoggles/smssync/mail/BackupImapStoreTest.java`
   - All call sites updated from `(context, uri, false/true)` to `(context, uri, new DefaultTrustedSocketFactory(context))` or `(context, uri, pinnedFactory)` (AC-7)
   - Old trust-all test replaced with `testShouldCreateCorrectTrustFactoryForPinnedCertUrl()` asserting `PinnedCertificateSocketFactory.class` (AC-7(b))
   - Added `loadTestCert()` helper using hardcoded DER literal (valid 2026-2036) (AC-7 / story Technical Notes)
   - 11 test methods, all pass

## Capabilities Inventory

This story modifies `BackupImapStore` constructor and `ServiceBase.getBackupImapStore()`.

### BackupImapStore.java

| Capability | Status |
|-----------|--------|
| Constructor creates IMAP store | RETAINED (constructor still exists, signature changed) |
| `getFolder()` - open BackupFolder | RETAINED (untouched) |
| `closeFolders()` | RETAINED (untouched) |
| `toString()` with credential masking | RETAINED (untouched) |
| `getStoreUriForLogging()` - credential masking | RETAINED (untouched) |
| `getTrustedSocketFactory()` - test seam | RETAINED at same location/visibility (IC-4) |
| `createAndOpenFolder()` | RETAINED (untouched) |
| `getStoreUri()` | RETAINED (untouched) |
| `BackupFolder` inner class | RETAINED (untouched) |
| `MessageComparator` | RETAINED (untouched) |
| `isValidImapFolder()` | RETAINED (untouched) |
| `isValidUri()` | RETAINED (untouched) |

### ServiceBase.java

| Capability | Status |
|-----------|--------|
| URI validation in `getBackupImapStore()` | RETAINED (first line of method body) |
| `throws MessagingException` declaration | RETAINED |
| `getAuthPreferences()` accessor | RETAINED |
| `getPreferences()` accessor | RETAINED |
| Notification management | RETAINED |
| Wake/WiFi lock management | RETAINED |
| `appLog()` / `appLogDebug()` | RETAINED |
| All other service lifecycle methods | RETAINED (untouched) |

## Contract Adherence

**CNTR-MODERNIZATION-001** — all requirements satisfied:

| Requirement | File:Line | Status |
|-------------|-----------|--------|
| `TlsTrustPolicy` enum with exactly `SYSTEM_VALIDATED` and `PINNED_CERTIFICATE` | `TlsTrustPolicy.java:31-42` | SATISFIED |
| No third constant (TRUST_ALL / INSECURE / ACCEPT_ANY) | `TlsTrustPolicy.java` — only two constants | SATISFIED |
| `PinnedCertificateSocketFactory` implements `TrustedSocketFactory` | `PinnedCertificateSocketFactory.java:55` | SATISFIED |
| Constructor: `(Context context, String host, X509Certificate enrolledCert)` | `PinnedCertificateSocketFactory.java:67` | SATISFIED |
| Non-empty `checkServerTrusted()` body (SHA-256 + validity) | `PinnedCertificateSocketFactory.java:124-146` | SATISFIED |
| `getAcceptedIssuers()` returns one-element array, never null | `PinnedCertificateSocketFactory.java:150-153` | SATISFIED |
| `BackupImapStore` constructor: `(Context, String, TrustedSocketFactory)` | `BackupImapStore.java:58-73` | SATISFIED |
| Super call: `super(new BackupStoreConfig(uri), socketFactory, ...)` | `BackupImapStore.java:68-70` | SATISFIED |
| Producer resolution in `ServiceBase.getBackupImapStore()` | `ServiceBase.java:105-134` | SATISFIED |
| `SYSTEM_VALIDATED` → `new DefaultTrustedSocketFactory(context)` | `ServiceBase.java:128-129` | SATISFIED |
| `PINNED_CERTIFICATE` → `new PinnedCertificateSocketFactory(context, host, cert)` | `ServiceBase.java:125-127` | SATISFIED |
| Consumer (BackupImapStore) never re-decides trust | `BackupImapStore.java:58-73` — factory passed straight through | SATISFIED |
| `PinnedCertStore.getTlsTrustPolicy(host, port)` returns SYSTEM_VALIDATED by default | `PinnedCertStore.java:109-113` | SATISFIED |
| `CertificateException` on mismatch, fail-closed | `PinnedCertificateSocketFactory.java:133-135` | SATISFIED |
| No trust-all fallback on resolution failure | `ServiceBase.java:105-134` — no fallback path | SATISFIED |

## Integration Verification

1. **New code reachable from production entry point**: `ServiceBase.getBackupImapStore()` is called by `SmsBackupService` and `SmsRestoreService` (both subclass `ServiceBase`). The new `TlsTrustPolicy`, `PinnedCertStore`, and `PinnedCertificateSocketFactory` are reached via this method.
2. **Constructor call sites updated**: `BackupImapStore` constructor is called only from `ServiceBase.getBackupImapStore()` in production (verified by grep). All test call sites updated in `BackupImapStoreTest.java`.
3. **No dead code**: All three new production classes are referenced from production code (`TlsTrustPolicy` and `PinnedCertStore` in `ServiceBase.java`; `PinnedCertificateSocketFactory` in `ServiceBase.java` and test code).
4. **AllTrustedSocketFactory**: still exists on disk (U-010 deletion), but zero production files import or reference it.

## Notes

- Test certificates generated with keytool (RSA-2048, SHA256withRSA, valid 2026-2036). DER bytes are hardcoded as base64 literals in test classes — no BouncyCastle dependency needed and no time-dependent CI failures before 2036.
- `PinnedCertificateSocketFactory.getTrustManagerForTesting()` is a package-private test seam that returns a new `PinnedX509TrustManager` instance, enabling direct unit testing of `checkServerTrusted()` and `getAcceptedIssuers()` without network calls.
- JDK 17 (`JAVA_HOME=/Users/Michael.Horsley/.jdks/jbr-17.0.14`) must be used for compilation; the system PATH has JDK 22 which triggers `-Werror` on obsolete `-source 8 -target 8` warnings.
