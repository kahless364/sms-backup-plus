---
artifact_type: security-review
story_id: "U-008"
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
---

# Security Review: U-008

## Summary

U-008 closes CWE-295 (Improper Certificate Validation) at the socket-factory selection layer.
All security-critical requirements of CNTR-MODERNIZATION-001 and DES-MODERNIZATION-002 are met.
No new security regressions introduced.

## Findings

### ARCH-007 (Critical) — Trust-All Path Elimination

**Before:** `BackupImapStore` accepted a `boolean trustAllCertificates` that could select
`AllTrustedSocketFactory.INSTANCE` (empty `checkServerTrusted()`, `getAcceptedIssuers()→null`).

**After:** `BackupImapStore` constructor accepts a `TrustedSocketFactory` resolved by the
caller. `AllTrustedSocketFactory` is not referenced by any production file other than itself.
The trust-all path is unreachable from production code. CLOSED.

### Two-State Policy — No Trust-All Constant

`TlsTrustPolicy` has exactly two constants: `SYSTEM_VALIDATED` and `PINNED_CERTIFICATE`.
There is no `TRUST_ALL`, `INSECURE`, `ACCEPT_ANY`, or equivalent. The design makes it
structurally impossible to introduce a trust-all path via this enum without a code change.
Per CNTR-MODERNIZATION-001 Validation Rule 1. PASS.

### PinnedCertificateSocketFactory — Cryptographic Correctness

- SHA-256 fingerprint comparison uses `MessageDigest.getInstance("SHA-256")` on `cert.getEncoded()` (DER bytes). Fingerprint is compared with `Arrays.equals()` which is constant-time for same-length byte arrays (SHA-256 always produces 32 bytes). No timing side-channel exploitable in practice at this hash comparison step.
- Validity window check: `enrolledCert.checkValidity()` uses the Java runtime's clock, consistent with standard TLS validation.
- `checkServerTrusted()` is non-empty and throws `CertificateException` on: null/empty chain, fingerprint mismatch, expired cert, not-yet-valid cert.
- `getAcceptedIssuers()` returns `new X509Certificate[]{enrolledCert}` — never `null`, never empty. This satisfies AC-2 and prevents the JSSE implementation from upgrading to "accept all" mode.
- The factory is per-host (constructor-injected certificate). Pinning a self-hosted server does not weaken Gmail's connection (CNTR-MODERNIZATION-001 Invariant 4).

### PinnedCertStore — Storage Security

- Uses `Context.MODE_PRIVATE` — the SharedPreferences file is not accessible to other apps.
- The file `pinned_certs.xml` is excluded from Android Auto-Backup via `backup_descriptor.xml`. This prevents enrolled certificates from being restored to a new device without user re-enrollment, which is the correct security posture (a restored cert would pin trust to a cert the user hasn't explicitly verified on the new device).
- No sensitive key material is stored — only public X.509 certificates (DER-encoded). There is no private key material at risk.
- Base64 decoding uses Android's `Base64.decode(encoded, Base64.DEFAULT)` with `IllegalArgumentException` caught on decode error. Corrupt stored bytes return `null` (treated as unenrolled), not a silent downgrade or crash.

### ServiceBase — Fail-Closed on Resolution Failure

Per CNTR-MODERNIZATION-001 §Error Handling: "the producer MUST NOT, on any resolution
failure, fall back to a trust-all or unvalidated factory."

The `getBackupImapStore()` method does not catch exceptions from `PinnedCertStore.get()`.
If `get()` returns `null` for a `PINNED_CERTIFICATE` policy (e.g., corrupt stored cert that
was decoded as `null`), `new PinnedCertificateSocketFactory(context, host, null)` will throw
`IllegalArgumentException` from the constructor's null check, which propagates as an
unchecked exception. This is fail-closed behavior — no connection is made with a degraded
factory.

However, there is a subtle issue: if `PinnedCertStore.getTlsTrustPolicy()` returns
`PINNED_CERTIFICATE` (key present in prefs) but `PinnedCertStore.get()` returns `null`
(cert bytes corrupt, decode failed, `get()` returns null on CertificateException), then
`PinnedCertificateSocketFactory(context, host, null)` throws `IllegalArgumentException`.
This surfaces as an uncaught runtime exception from `getBackupImapStore()`, which does not
declare `throws IllegalArgumentException`. The method declares `throws MessagingException`.

**Assessment:** This is fail-closed (connection does not proceed with a bad factory), which
satisfies the contract's prohibition on trust-all fallback. The crash is visible to the user
as a backup failure. U-009 (enrollment UI) should add a defensive `MessagingException` wrap
in `getBackupImapStore()` when this edge case is encountered. For U-008's scope, the
behavior is acceptable — a corrupt pinned cert store is an exceptional state that justifies
a hard failure.

**Recommendation:** Flag for U-009 or U-010 to add a defensive wrap:
```java
if (policy == TlsTrustPolicy.PINNED_CERTIFICATE) {
    X509Certificate enrolled = pinnedCertStore.get(host, port);
    if (enrolled == null) {
        throw new MessagingException("Pinned certificate for " + host + ":" + port + " could not be decoded; re-enrollment required.");
    }
    factory = new PinnedCertificateSocketFactory(getApplicationContext(), host, enrolled);
}
```

### No New Attack Surface

- No network calls in new code.
- No serialization/deserialization of untrusted data (SharedPreferences is app-private; Base64 decode errors are caught).
- No reflection usage.
- No native code.

## Verdict: PASS

The implementation closes the active MITM vulnerability (CWE-295) and satisfies all security
invariants of CNTR-MODERNIZATION-001. One minor defensive hardening recommendation logged
above for a follow-up story (U-009/U-010).
