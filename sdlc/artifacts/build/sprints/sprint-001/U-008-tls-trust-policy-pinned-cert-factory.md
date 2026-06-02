---
type: story
status: planned
sprint: "000001"
artifact_type: user-story
priority: critical
complexity: high
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-002
design_docs:
  - DES-MODERNIZATION-002
integration_contracts:
  - CNTR-MODERNIZATION-001
dependencies:
  - U-007
  - U-003
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-008
title: Introduce TlsTrustPolicy enum, PinnedCertificateSocketFactory, PinnedCertStore, and rewrite BackupImapStore/ServiceBase factory selection
pipeline: ''
domain: modernization
requirement_source: authored
---

# U-008: Introduce TlsTrustPolicy enum, PinnedCertificateSocketFactory, PinnedCertStore, and rewrite BackupImapStore/ServiceBase factory selection

## Story

As a security-conscious contributor to SMS Backup+,
I want the TLS trust model to be expressed as an explicit two-state policy enum (`TlsTrustPolicy.SYSTEM_VALIDATED` / `PINNED_CERTIFICATE`) with a validating `PinnedCertificateSocketFactory` and a per-host certificate store (`PinnedCertStore`), with `BackupImapStore`'s constructor signature changed to accept a resolved `TrustedSocketFactory` and factory selection hoisted into `ServiceBase.getBackupImapStore()`,
so that the application has no code path that can produce a trust-all socket factory, the dangerous boolean `trustAllCertificates` ternary is eliminated at its source, and `BackupImapStoreTest` asserts the correct factory for every policy branch before `AllTrustedSocketFactory` is deleted in U-010.

## Acceptance Criteria

- [ ] AC-1: `mail/TlsTrustPolicy.java` exists with exactly two public constants — `SYSTEM_VALIDATED` and `PINNED_CERTIFICATE` — and no third constant of any kind. Running `grep -rn "TlsTrustPolicy" app/src/main/java` finds the enum declaration and its usage sites only; there is no `TRUST_ALL`, `INSECURE`, `ACCEPT_ANY`, or equivalent constant anywhere in the file.

- [ ] AC-2: `mail/PinnedCertificateSocketFactory.java` implements `com.fsck.k9.mail.ssl.TrustedSocketFactory`. Its inner `X509TrustManager.checkServerTrusted(X509Certificate[] chain, String authType)` body is non-empty: it extracts the leaf certificate from `chain[0]`, computes its SHA-256 fingerprint, compares it to the SHA-256 fingerprint of the constructor-injected enrolled `X509Certificate`, verifies the enrolled certificate's validity window (not-before / not-after) against the current clock, and throws `CertificateException` with a descriptive message on any mismatch. `getAcceptedIssuers()` returns a one-element array containing the enrolled certificate — never `null` and never an empty array.

- [ ] AC-3: `mail/PinnedCertStore.java` reads and writes enrolled certificates from a `SharedPreferences` file named `"pinned_certs"` opened with `Context.MODE_PRIVATE`. Certificates are stored as Base64-encoded DER bytes keyed by the string `"host:port"` (e.g., `"mail.example.org:993"`). `PinnedCertStore.get(String host, int port)` returns `null` when no certificate is enrolled for that key and returns the decoded `X509Certificate` otherwise. `PinnedCertStore.put(String host, int port, X509Certificate cert)` encodes the certificate to DER, Base64-encodes it, and writes it under the key. `PinnedCertStore.remove(String host, int port)` removes the key. No method in `PinnedCertStore` reads from or writes to any other `SharedPreferences` file.

- [ ] AC-4: The `"pinned_certs"` `SharedPreferences` file is listed in `app/src/main/res/xml/backup_descriptor.xml` (or the file that contains `<exclude domain="sharedpref">` entries) with an `<exclude>` element whose `path` attribute covers `pinned_certs.xml`, preventing Android Auto-Backup from copying enrolled certificates to a new device without user re-enrollment.

- [ ] AC-5: `mail/BackupImapStore.java`'s constructor signature is `public BackupImapStore(Context context, String uri, TrustedSocketFactory socketFactory) throws MessagingException`. The old `boolean trustAllCertificates` parameter is gone. The super-constructor call is `super(new BackupStoreConfig(uri), socketFactory, (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE))`. The inline ternary `trustAllCertificates ? AllTrustedSocketFactory.INSTANCE : new DefaultTrustedSocketFactory(context)` is gone. No reference to `AllTrustedSocketFactory` remains in this file.

- [ ] AC-6: `service/ServiceBase.getBackupImapStore()` (lines 99–104 before this change) resolves the TLS policy and builds the concrete `TrustedSocketFactory` before constructing `BackupImapStore`. The implementation reads the configured host and port from the URI, calls `pinnedCertStore.getTlsTrustPolicy(host, port)` (or equivalent inline resolution), and selects: `new DefaultTrustedSocketFactory(context)` when the policy is `SYSTEM_VALIDATED`, or `new PinnedCertificateSocketFactory(context, host, pinnedCertStore.get(host, port))` when the policy is `PINNED_CERTIFICATE`. The `getBackupImapStore()` method no longer calls `getAuthPreferences().isTrustAllCertificates()` for socket-factory selection. The resolved factory — never a boolean — is passed as the third argument to the `BackupImapStore` constructor.

- [ ] AC-7: `mail/BackupImapStoreTest.java` is rewritten so that: (a) every call site that previously passed `false` for `trustAllCertificates` now passes a `DefaultTrustedSocketFactory` instance (or constructs the store through `ServiceBase`'s resolved path); (b) the test case that previously constructed the store with `true` and asserted `AllTrustedSocketFactory.class` at line 55 is replaced by a test that constructs a `PinnedCertificateSocketFactory` with a test-only self-signed certificate and asserts that the factory returned by `getTrustedSocketFactory()` is an instance of `PinnedCertificateSocketFactory`; (c) the test that covers the `SYSTEM_VALIDATED` default path asserts that the factory returned by `getTrustedSocketFactory()` is an instance of `DefaultTrustedSocketFactory`; (d) all existing tests in the file pass (no regressions). This test file must compile and all tests must be green before `AllTrustedSocketFactory.java` is deleted in U-010.

- [ ] AC-8: Running `./gradlew :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.BackupImapStoreTest"` from the repo root exits with code 0 and reports zero test failures. `PinnedCertificateSocketFactory`'s `checkServerTrusted()` is covered by at least two unit tests in a `PinnedCertificateSocketFactoryTest` class: one that presents the enrolled certificate and expects no exception, and one that presents a different certificate and expects `CertificateException`.

- [ ] AC-9: `PinnedCertStore` has a unit test class `PinnedCertStoreTest` that covers: (a) `get()` returns `null` for an unenrolled host:port; (b) `put()` followed by `get()` returns a certificate whose encoded form is byte-for-byte equal to the original; (c) `remove()` followed by `get()` returns `null`; (d) two distinct `host:port` keys are independent (enrolling one does not affect the other). Tests use `Robolectric` (consistent with the project test harness per U-005) and `Context.MODE_PRIVATE` is verified by inspecting the preferences file name used in the test's `ShadowApplication`.

- [ ] AC-10: Running `grep -rn "AllTrustedSocketFactory" app/src/main/java` returns zero matches after this story is complete. The class file `app/src/main/java/com/zegoggles/smssync/mail/AllTrustedSocketFactory.java` still physically exists on disk (it is deleted in U-010, not here), but no production Java file imports or references it.

- [ ] AC-11: The canonical type names `TlsTrustPolicy`, `SYSTEM_VALIDATED`, and `PINNED_CERTIFICATE` are used verbatim in all new and modified production files. No synonym (`TlsPolicy`, `VALIDATED`, `PINNED`, `TRUST_ALL`, `trustAllCertificates`) appears in any production file modified or created by this story.

### Integration Criteria

- [ ] IC-1: `mail/TlsTrustPolicy.java` is in package `com.zegoggles.smssync.mail` and is importable by `service/ServiceBase.java` and `mail/BackupImapStore.java` without any circular dependency.
- [ ] IC-2: `mail/PinnedCertificateSocketFactory.java` is in package `com.zegoggles.smssync.mail` and its constructor is `public PinnedCertificateSocketFactory(Context context, String host, X509Certificate enrolledCert)`. There is no zero-argument or no-cert constructor.
- [ ] IC-3: `mail/PinnedCertStore.java` is in package `com.zegoggles.smssync.mail` and is injectable at `ServiceBase` (passed in via constructor or field — whichever the existing `ServiceBase` construction pattern supports without requiring Hilt, which arrives in U-022).
- [ ] IC-4: `BackupImapStore.getTrustedSocketFactory()` (package-private test seam, `BackupImapStore.java:116-118` before this change) is preserved with the same return type and visibility after the constructor signature change. It remains the only way test code accesses the factory.
- [ ] IC-5: All call sites of the `BackupImapStore` constructor in production code compile against the new `(Context, String, TrustedSocketFactory)` signature. Running `./gradlew :app:compileDebugJavaWithJavac` exits with code 0.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/mail/BackupImapStore.java` | Constructor `(Context, String, boolean trustAllCertificates)` selects `AllTrustedSocketFactory.INSTANCE` or `DefaultTrustedSocketFactory` inline via ternary at `:58-63` | Constructor changed to `(Context, String, TrustedSocketFactory socketFactory)`; ternary removed; factory passed straight to `super(...)` |
| `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java` | `getBackupImapStore()` at `:99-105` reads `isTrustAllCertificates()` and passes a boolean to the `BackupImapStore` constructor | Rewritten to resolve `TlsTrustPolicy` for the configured `host:port` via `PinnedCertStore`, build the concrete `TrustedSocketFactory`, and pass it to the new constructor signature |
| `app/src/test/java/com/zegoggles/smssync/mail/BackupImapStoreTest.java` | Call site at `:54` passes `true` and asserts `AllTrustedSocketFactory.class` at `:55`; other call sites pass `false` | Trust-all assertion replaced with `PinnedCertificateSocketFactory` assertion; `false` call sites adapted to new `TrustedSocketFactory` parameter |
| `app/src/main/java/com/zegoggles/smssync/mail/TlsTrustPolicy.java` | Does not exist | Created: two-constant enum `SYSTEM_VALIDATED` / `PINNED_CERTIFICATE` |
| `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` | Does not exist | Created: `TrustedSocketFactory` implementation with validating `X509TrustManager` |
| `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertStore.java` | Does not exist | Created: per-`host:port` `SharedPreferences`-backed certificate store, backup-excluded |
| `app/src/main/res/xml/backup_descriptor.xml` (or equivalent backup rules file) | May or may not exclude `credentials.xml` (pattern from `AuthPreferences.java:221-226`) | Add `<exclude>` entry for `pinned_certs.xml` |

## Existing Behavior to Preserve

- `BackupImapStore.getFolder()`, `getMessages()`, `appendMessages()`, `fetch()`, `getStoreUriForLogging()` (credential masking at `:98-114`), `isValidUri()`, `isValidImapFolder()`, `BackupFolder`, and `MessageComparator` are untouched. This story modifies only the constructor.
- `BackupImapStore.getTrustedSocketFactory()` (package-private test seam at `:116-118`) is preserved with the same name, return type, and visibility.
- `ServiceBase.getBackupImapStore()` retains its `throws MessagingException` declaration and its URI validation call `BackupImapStore.isValidUri(uri)` before any factory resolution.
- All existing `BackupImapStoreTest` test cases for `shouldHaveToString*`, `shouldThrowException*`, `testAccountHasStoreUri`, and `testShouldCreateCorrectTrustFactoryForTrustedSSLUrl` / `testShouldCreateCorrectTrustFactoryForTrustedTLSUrl` must pass without behavioral regression — only the constructor call signatures in those tests change to match the new `(Context, String, TrustedSocketFactory)` form.
- No changes to `service/state/`, `DataType`, the exception hierarchy (`MailException` chain), or the message-conversion components (MU-000 Preserved Core).
- `AuthPreferences.isTrustAllCertificates()` (`:196-198`) is not removed in this story; U-007 owns the `migrate()` rewrite. `ServiceBase` stops calling `isTrustAllCertificates()` for socket-factory selection, but the method itself survives until its caller is gone and any residual reference is removed (U-010 or U-007 as appropriate).

## Verification Steps

1. **Compile gate.** Run `./gradlew :app:compileDebugJavaWithJavac` from the repo root. Confirm exit code 0 and zero errors or warnings referencing the changed files.

2. **New-file structure check for `TlsTrustPolicy`.** Open `app/src/main/java/com/zegoggles/smssync/mail/TlsTrustPolicy.java`. Confirm: (a) it is `public enum TlsTrustPolicy`; (b) it declares exactly `SYSTEM_VALIDATED` and `PINNED_CERTIFICATE`; (c) no other constants are present. Run `grep -c "TRUST_ALL\|INSECURE\|ACCEPT_ANY" app/src/main/java/com/zegoggles/smssync/mail/TlsTrustPolicy.java` — expected output: `0`.

3. **`PinnedCertificateSocketFactory` trust-manager body audit.** Open `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java`. Locate the `checkServerTrusted` method. Confirm: (a) it is not empty; (b) it calls a SHA-256 digest operation; (c) it throws `CertificateException` on mismatch. Locate `getAcceptedIssuers()`. Confirm it returns an array of length 1 containing the enrolled certificate.

4. **`PinnedCertStore` preferences-file name check.** Open `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertStore.java`. Confirm the `SharedPreferences` file name passed to `context.getSharedPreferences(...)` is exactly the string literal `"pinned_certs"` and the mode is `Context.MODE_PRIVATE`.

5. **Backup exclusion check.** Open the backup rules XML file (`app/src/main/res/xml/backup_descriptor.xml` or the file referenced by `android:dataExtractionRules` / `android:fullBackupContent` in `AndroidManifest.xml`). Confirm the presence of an `<exclude domain="sharedpref" path="pinned_certs.xml" />` element (or the equivalent element for the platform's backup API version in use).

6. **`BackupImapStore` constructor signature check.** Open `app/src/main/java/com/zegoggles/smssync/mail/BackupImapStore.java`. Confirm: (a) the constructor parameter list is `(Context context, String uri, TrustedSocketFactory socketFactory)`; (b) the words `trustAllCertificates` and `AllTrustedSocketFactory` do not appear anywhere in the file; (c) the super call is `super(new BackupStoreConfig(uri), socketFactory, ...)`.

7. **`ServiceBase` factory-resolution check.** Open `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java`. In `getBackupImapStore()`: (a) confirm `isTrustAllCertificates()` is not called for factory selection; (b) confirm `TlsTrustPolicy` is referenced; (c) confirm the resolved `TrustedSocketFactory` is the third argument to `new BackupImapStore(...)`.

8. **No trust-all production reference.** Run `grep -rn "AllTrustedSocketFactory" app/src/main/java`. Expected: zero matches. (`AllTrustedSocketFactory.java` itself still exists on disk; it is deleted in U-010.)

9. **BackupImapStoreTest — full test run.** Run `./gradlew :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.BackupImapStoreTest"`. Confirm exit code 0 and zero failures. Inspect the test output to confirm: (a) a test exists that constructs a store with a `PinnedCertificateSocketFactory` and asserts `getTrustedSocketFactory()` returns an instance of `PinnedCertificateSocketFactory`; (b) a test exists that constructs a store with a `DefaultTrustedSocketFactory` and asserts the same seam returns `DefaultTrustedSocketFactory`.

10. **PinnedCertificateSocketFactoryTest — validation paths.** Run `./gradlew :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryTest"`. Confirm: (a) the test with the matching certificate passes with no exception thrown from `checkServerTrusted`; (b) the test with a mismatched certificate records a thrown `CertificateException`.

11. **PinnedCertStoreTest — round-trip and isolation.** Run `./gradlew :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.PinnedCertStoreTest"`. Confirm all four AC-9 scenarios pass: null for unenrolled key; byte-equal round-trip; null after remove; independence of two keys.

12. **Full unit test suite.** Run `./gradlew :app:testDebugUnitTest`. Confirm exit code 0 and zero test failures. This is the regression gate verifying no previously passing test was broken by the constructor signature change.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Java) | All production and test changes — new types, store, factory, constructor rewrite, ServiceBase hoist | Developer |

## Technical Notes

**Why the constructor takes a resolved `TrustedSocketFactory` rather than a `TlsTrustPolicy`.**
The k-9 `ImapStore` super-constructor slot (`BackupImapStore.java:60-62`) accepts a `TrustedSocketFactory` (verified: the third argument in the super call, type `com.fsck.k9.mail.ssl.TrustedSocketFactory`). Passing the enum to the constructor would require the constructor to do the factory-selection logic, which would re-introduce a trust branch inside the store. The design deliberately makes the store a passive recipient of an already-resolved factory (DES-MODERNIZATION-002 Decision 2 §Note on scope; CNTR-MODERNIZATION-001 Invariant point 1 and 3). The policy enum is used at the selection layer (`ServiceBase`) and must not be visible as a branch inside `BackupImapStore` itself.

**`PinnedCertificateSocketFactory` SHA-256 + validity-window check.**
The trust manager must check both fingerprint equality and the validity window. Fingerprint equality alone would accept an enrolled certificate even after it has expired; an attacker who obtained the expired cert DER could present it. The validity-window check closes this. Use `enrolledCert.checkValidity()` (throws `CertificateExpiredException` / `CertificateNotYetValidException`, both subclasses of `CertificateException`) after the fingerprint match, so the `CertificateException` hierarchy is preserved for the caller.

**`PinnedCertStore` construction and `ServiceBase` wiring.**
Until Hilt arrives in U-022, `PinnedCertStore` cannot be constructor-injected via DI. Instantiate it directly in `ServiceBase.getBackupImapStore()` (e.g., `new PinnedCertStore(getApplicationContext())`), consistent with how `DefaultTrustedSocketFactory` was constructed inline at `:62` before this change. The object is lightweight (one `SharedPreferences` read per call) and stateless beyond the context; no singleton wrapping is required at this stage.

**Backup exclusion API level.** SMS Backup+ targets API 35 (after U-003). From API 31+, backup rules use `android:dataExtractionRules` pointing to an XML file with `<cloud-backup>` / `<device-transfer>` elements. From API 23–30, `android:fullBackupContent` uses `<exclude>` elements directly. Both forms may coexist in the manifest for compatibility. Whichever form the project already uses for `credentials.xml` exclusion (the precedent from `AuthPreferences.java:221-226`) must be extended to also exclude `pinned_certs.xml` using the same pattern.

**Test seam (`getTrustedSocketFactory()`) and test self-signed certificate.** The `BackupImapStoreTest` `PinnedCertificateSocketFactory` assertion test needs an `X509Certificate` to construct the factory. Generate a test-only self-signed certificate programmatically in the test class using `BouncyCastle` (already available in the build via the k-9 dependency tree) or use a hardcoded DER literal with a far-future expiry. Do not add a new test dependency for this alone. The test certificate's validity window must not expire before 2035 to prevent time-dependent CI failures.

**CNTR-MODERNIZATION-001 compliance.** This story is the primary implementation story for the producer side of CNTR-MODERNIZATION-001. The canonical names `TlsTrustPolicy`, `SYSTEM_VALIDATED`, and `PINNED_CERTIFICATE` must appear verbatim. The invariant "no third constant" must hold. The `PinnedCertificateSocketFactory` `checkServerTrusted()` must be non-empty and must examine the chain (CNTR-MODERNIZATION-001 §Validation Rules 1–2).

**Estimation: High.** Three new production classes (`TlsTrustPolicy`, `PinnedCertificateSocketFactory`, `PinnedCertStore`), two modified production files (`BackupImapStore`, `ServiceBase`), one modified test file (`BackupImapStoreTest`), two new test classes (`PinnedCertificateSocketFactoryTest`, `PinnedCertStoreTest`), and a backup-rules XML change. The cryptographic correctness of `checkServerTrusted()` and the backup-exclusion plumbing require careful implementation.

## Supporting Documentation

- REQ-MODERNIZATION-002 — AC-1, AC-2, AC-3, AC-8; §Implementation Sequence steps 3–5; §Constraints 1 and 3; §Code Locations table
- DES-MODERNIZATION-002 — Decision 1 (delete trust-all, validated TLS as default), Decision 2 (TlsTrustPolicy enum, BackupImapStore reshape), Decision 5 (PinnedCertStore), §Integration Design §Socket-factory selection in BackupImapStore, §Components table
- DES-MODERNIZATION-002 §Architecture (data-flow diagram showing ServiceBase → resolved TrustedSocketFactory → BackupImapStore super-constructor)

## Integration Contract References

- CNTR-MODERNIZATION-001 §Contract Definition — producer: `ServiceBase.getBackupImapStore()` + `PinnedCertStore`; carried type: `com.fsck.k9.mail.ssl.TrustedSocketFactory`; invariants 1–5; handoff signatures (current consumer: `BackupImapStore(Context, String, TrustedSocketFactory)`)
- CNTR-MODERNIZATION-001 §Trust-policy enum — canonical names `TlsTrustPolicy` / `SYSTEM_VALIDATED` / `PINNED_CERTIFICATE`; no third constant
- CNTR-MODERNIZATION-001 §Validation Rules 1–3 — two-constant enum; non-empty `checkServerTrusted()`; consumer never re-decides trust
- CNTR-MODERNIZATION-001 §Error Handling — `CertificateException` on mismatch, fail-closed; no trust-all fallback on resolution failure

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Story scope is the security-policy plumbing layer only: new types, store, factory, constructor signature change, ServiceBase hoist, and test rewrites. The pinned-certificate enrollment UI (the user-facing flow that writes to `PinnedCertStore`) is scoped to U-009. Deletion of `AllTrustedSocketFactory.java` and the repository-wide grep verification gate are scoped to U-010. `migrate()` rewrite is scoped to U-007.
