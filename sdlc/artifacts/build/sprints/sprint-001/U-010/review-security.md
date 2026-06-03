---
artifact_type: security-review
story_id: U-010
verdict: PASS
agent: Developer
timestamp: "2026-06-03T00:00:00Z"
---

# Security Review: U-010

## Summary

This story closes the TLS hardening workstream by deleting `AllTrustedSocketFactory` and
confirming that no trust-all path remains in the production source. This review documents
the audited trust-write sites and confirms Gate G1 is warranted.

## Threat Context

CWE-295 (Improper Certificate Validation) / ARCH-007 / SEC-001. A trust-all socket factory
allows on-path attackers to intercept IMAP traffic by presenting a forged certificate. The
deleted factory was the last such path.

## Audited Trust-Write Sites

### Site 1: AllTrustedSocketFactory.java — DELETED

**Was:** `app/src/main/java/com/zegoggles/smssync/mail/AllTrustedSocketFactory.java`

Content at deletion:
- `InsecureX509TrustManager.checkServerTrusted()`: empty body (lines 49-52) — accepted ANY certificate
- `InsecureX509TrustManager.getAcceptedIssuers()`: returned `null` (line 55-56)
- `@SuppressLint("TrustAllX509TrustManager")` at line 20
- Used via `AllTrustedSocketFactory.INSTANCE` (line 22)

**Status: DELETED.** No class file for `AllTrustedSocketFactory` or `InsecureX509TrustManager`
exists in the shipping binary. Confirmed by `assembleDebug` passing and grep-zero.

**AC-1 confirmation:**
```
grep -rn "AllTrustedSocketFactory" app/src/main/java/ → zero output
grep -rn "InsecureX509TrustManager" app/src/main/java/ → zero output
grep -rn "TrustAllX509TrustManager" app/src/main/java/ → zero output
```

### Site 2: AuthPreferences.migrate() — AUDITED, SAFE

**File:** `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java`

**Write at line 361:**
```java
// AC-5 / REQ-MODERNIZATION-002 AC-10: NEVER write SERVER_TRUST_ALL_CERTIFICATES=true.
// AC-2 migration-of-already-downgraded-users: actively clear any stale true left by
// the previous app version's silent downgrade, restoring validated TLS immediately.
if (preferences.getBoolean(SERVER_TRUST_ALL_CERTIFICATES, false)) {
    edit.putBoolean(SERVER_TRUST_ALL_CERTIFICATES, false);   // clears stale true
    markTransportSecurityNoticePending(edit);
}
```

**Value written:** `false` (clearing a stale `true` left by pre-U-007 app versions)
**Reachable from:** `App.onCreate()` → `Preferences.migrate()` → `AuthPreferences.migrate()`
**Security assessment:** SAFE. This is a remediation write — it actively clears the
insecure legacy value. The guard `if (preferences.getBoolean(..., false))` means the write
only fires when a stale `true` is present. Writing `false` restores the secure default.
**No branch writes `true` under any condition.**

### Site 3: AuthPreferences.isTrustAllCertificates() — READ ONLY, NOT A WRITE SITE

**File:** `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java:265`
```java
return preferences.getBoolean(SERVER_TRUST_ALL_CERTIFICATES, false);
```
This is a read site. Default value is `false`. Not a write site.

### Site 4: TransportSecurityNoticeHelper.java:32 — COMMENT ONLY, NOT A WRITE SITE

```java
// {@code SERVER_TRUST_ALL_CERTIFICATES=true} cleared by
```
This is a Javadoc comment reference. Not a write site.

## SERVER_TRUST_ALL_CERTIFICATES Write-Site Summary

| # | File:Line | Method | Value | Type | Assessment |
|---|-----------|--------|-------|------|------------|
| 1 | `AuthPreferences.java:361` | `migrate()` | `false` | Runtime write | SAFE — clears stale legacy value |

**Total write sites that can write `true`: ZERO.**

The `SERVER_TRUST_ALL_CERTIFICATES=true` write that existed in the old `migrate()` (U-007
scope) has been removed. No remaining code path writes `true` for this key.

## X509TrustManager Implementations Audit

### PinnedCertificateSocketFactory.PinnedX509TrustManager

**File:** `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java:117-158`
**Used on:** Data connection path (IMAP TLS handshake when `PINNED_CERTIFICATE` policy)

`checkServerTrusted()` (lines 126-150):
1. Rejects null/empty chain: `throw new CertificateException("Server presented an empty certificate chain")`
2. Extracts leaf cert: `chain[0]`
3. Computes SHA-256 fingerprints of presented and enrolled certs
4. Throws `CertificateException` on fingerprint mismatch: `"Pinned certificate mismatch"`
5. Calls `enrolledCert.checkValidity()` — throws on expired/not-yet-valid cert

`getAcceptedIssuers()`: returns `new X509Certificate[]{ enrolledCert }` — never null, never empty.

**Security assessment: SECURE.** Non-empty body. Inspects `chain`. Throws on mismatch.
Satisfies CNTR-MODERNIZATION-001 Validation Rule 2.

### PinCertificateEnrollmentFlow.EnrollmentCaptureTrustManager

**File:** `app/src/main/java/com/zegoggles/smssync/activity/fragments/PinCertificateEnrollmentFlow.java:226-251`
**Used on:** Enrollment TLS handshake ONLY (capture leaf cert for user display)
**NOT used on:** Any IMAP data connection

`checkServerTrusted()` (lines 235-240):
```java
public void checkServerTrusted(X509Certificate[] chain, String authType) {
    // Capture the leaf (index 0); do not reject — enrollment display-only.
    if (chain != null && chain.length > 0) {
        leafCert = chain[0];
    }
}
```

**Security assessment:** This trust manager intentionally does not reject any certificate —
its purpose is to capture the cert for display to the user, not to validate it. This is the
approved enrollment design (CNTR-MODERNIZATION-002). Key mitigations:
1. This connection transmits NO user data or IMAP credentials.
2. The captured cert is displayed to the user for verification before any enrollment.
3. This class is ephemeral — created per enrollment attempt, not stored.
4. It is not reachable from `BackupImapStore` or any data-transmission path.
5. The `@SuppressLint("TrustAllX509TrustManager")` annotation is NOT present (class was
   designed clean from the start in U-009).

This pattern is the accepted security trade-off for certificate enrollment (the user must
see the cert to decide whether to pin it). Risk is bounded: the only thing exposed by MITM
during enrollment is that the user would see an attacker's cert — which they would then
(knowingly) reject or (mistakenly) pin. The user has the final decision.

## DefaultTrustedSocketFactory — System Path

The system-validated path uses `com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory` from
k9mail-vendored. This delegates to Android's platform `TrustManager` which provides full
CA chain validation. Not audited here (k-9 vendored code, not app code).

## Gate G1 / Milestone M1 Security Assessment

All five AC security conditions confirmed:

1. **AllTrustedSocketFactory DELETED** — grep-zero confirmed.
2. **No empty checkServerTrusted()** — PinnedX509TrustManager validates fingerprint + validity.
   EnrollmentCaptureTrustManager is enrollment-only (non-data-path), approved design.
3. **SERVER_TRUST_ALL_CERTIFICATES write-site audit** — zero writes of `true` remain.
4. **CI green** — `assembleDebug`, `testDebugUnitTest` (561 active tests, 0 failures),
   `jacocoTestCoverageVerification` all BUILD SUCCESSFUL.
5. **Gate G1 declared** — see implementation-log.md.

**The insecure-by-default TLS posture (CWE-295 / ARCH-007 / SEC-001) has been demonstrably
eliminated from the shipping binary.**

## Residual Risk

None introduced by this story. The only residual risk in the TLS domain is the enrollment
capture trust manager, which was approved in U-009 and is bounded as described above.

## Verdict: PASS
