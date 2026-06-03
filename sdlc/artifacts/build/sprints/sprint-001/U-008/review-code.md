---
artifact_type: code-review
story_id: "U-008"
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
---

# Code Review: U-008

## Summary

All new and modified production code is correct, follows existing patterns, and satisfies
the acceptance criteria. No blocking findings.

## Review Findings

### TlsTrustPolicy.java

- Two constants only: `SYSTEM_VALIDATED` and `PINNED_CERTIFICATE`. No third constant. PASS.
- Package is `com.zegoggles.smssync.mail`. PASS.
- Javadoc references `PinnedCertStore` and `PinnedCertificateSocketFactory` cross-links. PASS.
- No synonym names anywhere in the file. PASS.

### PinnedCertificateSocketFactory.java

- Implements `com.fsck.k9.mail.ssl.TrustedSocketFactory`. PASS.
- Constructor signature: `(Context context, String host, X509Certificate enrolledCert)`. PASS.
- No zero-argument or no-cert constructor. PASS.
- `checkServerTrusted()` is non-empty: extracts `chain[0]`, computes SHA-256 with `MessageDigest.getInstance("SHA-256")`, compares via `Arrays.equals()`, then calls `enrolledCert.checkValidity()`. PASS.
- Throws `CertificateException` on null/empty chain, fingerprint mismatch, and expired cert. PASS.
- `getAcceptedIssuers()` returns `new X509Certificate[]{enrolledCert}` — never null, never empty. PASS.
- Package-private test seam `getTrustManagerForTesting()`. PASS.
- `createSocket()` mirrors the pattern of `AllTrustedSocketFactory` (SSLContext.getInstance("TLS"), sslContext.init, sslContext.getSocketFactory()) but with a validating TrustManager. PASS.

### PinnedCertStore.java

- SharedPreferences file name: exactly `"pinned_certs"` (line 62). PASS.
- Mode: `Context.MODE_PRIVATE` (line 62). PASS.
- Key format: `host + ":" + port` (line 126). PASS.
- `get()` returns `null` for missing key and for corrupt data (catches `CertificateException` and `IllegalArgumentException`). PASS.
- `put()` uses `cert.getEncoded()` + `Base64.encodeToString(der, Base64.DEFAULT)` + `apply()`. PASS.
- `remove()` removes the key. PASS.
- `getTlsTrustPolicy()` returns `PINNED_CERTIFICATE` if key present, `SYSTEM_VALIDATED` otherwise. PASS.
- Package constant `PREFS_NAME` for test verification. PASS.
- Uses `context.getApplicationContext()` in constructor to avoid activity leaks. PASS.
- No method reads from or writes to any other SharedPreferences file. PASS.

### BackupImapStore.java

- Constructor signature: `(Context, String, TrustedSocketFactory)`. PASS (AC-5).
- Boolean `trustAllCertificates` parameter gone. PASS.
- `AllTrustedSocketFactory` not mentioned. PASS (AC-10).
- Super call: `super(new BackupStoreConfig(uri), socketFactory, ...)`. PASS.
- `import DefaultTrustedSocketFactory` removed (not needed, factory injected). PASS.
- `getTrustedSocketFactory()` test seam preserved with same name, return type, visibility. PASS (IC-4).
- All other methods unchanged. PASS.

### ServiceBase.java

- `isTrustAllCertificates()` no longer called for factory selection. PASS (AC-6).
- `TlsTrustPolicy` referenced at line 119. PASS.
- Host/port parsed from URI with `Uri.parse(uri).getHost()/.getPort()`. PASS.
- `PinnedCertStore` instantiated inline (consistent with pre-Hilt pattern per story notes). PASS.
- `SYSTEM_VALIDATED` path: `new DefaultTrustedSocketFactory(getApplicationContext())`. PASS.
- `PINNED_CERTIFICATE` path: `new PinnedCertificateSocketFactory(getApplicationContext(), host, pinnedCertStore.get(host, port))`. PASS.
- Resolved factory passed as third arg to `new BackupImapStore(...)`. PASS.
- `throws MessagingException` and URI validation preserved. PASS.

### backup_descriptor.xml

- `<exclude domain="sharedpref" path="pinned_certs.xml"/>` added. PASS (AC-4).
- Existing `credentials.xml` exclusion preserved. PASS.
- Uses `fullBackupContent` form (API 23+) consistent with existing project pattern. PASS.

### BackupImapStoreTest.java

- All `false/true` boolean call sites updated to `DefaultTrustedSocketFactory` instances. PASS (AC-7).
- Old trust-all assertion replaced with `testShouldCreateCorrectTrustFactoryForPinnedCertUrl()` asserting `PinnedCertificateSocketFactory.class`. PASS (AC-7(b)).
- SYSTEM_VALIDATED path tests assert `DefaultTrustedSocketFactory.class`. PASS (AC-7(c)).
- Hardcoded DER cert valid until 2036. PASS (story Technical Notes).
- No import or use of `AllTrustedSocketFactory`. PASS (AC-10).
- 11 tests, all passing. PASS (AC-8).

### PinnedCertificateSocketFactoryTest.java

- Covers enrolled cert → no exception and different cert → CertificateException. PASS (AC-8).
- Covers `getAcceptedIssuers()` not-null, not-empty, contains enrolled cert. PASS (AC-2).
- Covers null chain → CertificateException (fail-closed). PASS.
- Constructor null cert → IllegalArgumentException. PASS (IC-2).
- Uses Robolectric. PASS.

### PinnedCertStoreTest.java

- AC-9(a): null for unenrolled. PASS.
- AC-9(b): round-trip byte-equal. PASS.
- AC-9(c): null after remove. PASS.
- AC-9(d): two keys independent; remove one, other unaffected. PASS.
- Prefs file name verified via `getSharedPreferences("pinned_certs", MODE_PRIVATE)`. PASS.
- Uses Robolectric. PASS.

## No Blocking Issues

All acceptance criteria and integration criteria are satisfied. The implementation
correctly follows CNTR-MODERNIZATION-001 invariants and DES-MODERNIZATION-002 decisions.
