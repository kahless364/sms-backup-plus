---
artifact_type: review-security
story_id: U-045
verdict: PASS
agent: Security Reviewer
timestamp: "2026-06-05"
blockers: 0
warnings: 1
---

# Security Review: U-045

## Review Summary

The change adds a standard-API SNI path for API 24+ and demotes a reflective-fallback error log to DEBUG. All four security-sensitive dimensions assessed (correct host in SNI extension, certificate/hostname validation integrity, log content, trust bypass introduction) are sound. One low-severity surface note is raised regarding the public visibility of `setSniViaSSLParameters`, but it introduces no exploitable risk.

## Verdict: PASS

## Findings

### Blockers

None.

### Warnings

#### Low — `setSniViaSSLParameters` promoted to `public static` unnecessarily widens API surface

**File:** `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`, line 212

The helper was made `public static` solely to facilitate unit testing (`DefaultTrustedSocketFactorySniTest` calls it directly). Within a vendored module there is no external API contract, so widening visibility carries no immediate exploit risk; however it establishes a callable entry point outside `setSniHost`'s API-level guard. A caller that invokes `setSniViaSSLParameters` directly on a pre-API-24 device would receive an `IllegalArgumentException` or `NoSuchMethodError` from the underlying `SSLParameters.setServerNames` method (unavailable before API 24). This is not a trust bypass — it is an API-misuse trap.

Recommendation: Consider narrowing to `package-private` and using `@VisibleForTesting` (guava/Jetpack annotation) so intent is explicit and external callers are discouraged. Not blocking.

### Security Checklist

- [x] No hardcoded credentials or secrets — no credentials appear anywhere in the diff; the `Log.d` message contains `e.getMessage()` (a `NoSuchMethodException` description such as "setHostname") with no credential content.
- [x] Input validation on all user inputs — `hostname` is the IMAP server hostname originating from user-configured preferences. It is passed to `new SNIHostName(hostname)` which validates RFC 6066 syntax and throws `IllegalArgumentException` on invalid input; this is the same validation behavior as `SSLCertificateSocketFactory.setHostname` and the former reflective path. No regression in input handling. The TLS layer will reject any connection whose server certificate does not match this SNI value via `TrustManagerFactory`'s `StrictHostnameVerifier` — SNI cannot be used to redirect the connection to a different host for certificate purposes.
- [x] Output encoding prevents injection attacks — not applicable (no HTTP/HTML output); the LOG_TAG constant is a static string; the logged `e.getMessage()` fragment is a JVM exception message, not user-controlled data.
- [x] Authentication tokens handled securely — no auth tokens touched by this change.
- [x] Authorization checks on all protected resources — no authorization logic in scope.
- [x] Sensitive data encrypted at rest and in transit — the change concerns TLS channel setup only; `hardenSocket` (cipher hardening), `TrustManagerFactory` (certificate chain validation), and `StrictHostnameVerifier` (hostname verification) are byte-for-byte identical to the baseline commit (`370e88cf`). No cipher suite, protocol, or trust-manager code was modified.
- [x] Error messages don't leak internal details — the `Log.d` message includes `e.getMessage()` which for `NoSuchMethodException` on a missing `setHostname` method resolves to a string like `"setHostname"` — no host address, credential, session key, or certificate material is present. Severity is DEBUG; not surfaced to users.
- [x] Dependencies have no known critical vulnerabilities — no dependency changes in this diff. Only `javax.net.ssl.SNIHostName` and `javax.net.ssl.SSLParameters` are newly imported; both are part of the Java SE standard library included in the Android SDK since API 24 with no known CVEs.

---

## Detailed Security Assessment

### 1. SNI Correctness — Correct Host Passed (No SNI Misdirection)

The `hostname` parameter flowing into `setSniViaSSLParameters(socket, hostname)` at line 197 originates from `createSocket(socket, host, port, ...)` at line 152, where `host` is the IMAP server hostname supplied by the caller. The same `host` value is passed simultaneously to:

- `TrustManagerFactory.get(host, port)` at line 155 — binds certificate validation to this hostname
- `socketFactory.createSocket(socket, host, port, true)` at line 172 — TCP connection target
- `setSniHost(socketFactory, sslSocket, host)` at line 177 — SNI extension value

All three usages reference the same variable. There is no code path in which the SNI extension could name a host different from the one being connected to and the one being certificate-validated. The `SNIHostName` constructor enforces RFC 6066 A-label syntax, rejecting IP addresses (which per RFC must not appear in SNI), non-ASCII labels that fail IDN validation, and strings with embedded NUL bytes. This is strictly equivalent to or stronger than the legacy reflective `setHostname` shim.

### 2. Certificate Validation, Hostname Verification, Cipher/Protocol Hardening — Unchanged

A line-by-line diff confirms:

- `hardenSocket(SSLSocket)` — identical; `ENABLED_CIPHERS` and `ENABLED_PROTOCOLS` arrays and static initializer are untouched.
- `TrustManagerFactory.get(host, port)` call site — untouched; `SecureX509TrustManager` implementation (with `StrictHostnameVerifier`) is untouched.
- `KeyChainKeyManager` path — untouched.
- `PinnedCertificateSocketFactory` — not referenced in the diff; confirmed present and unmodified in production sources.
- `AllTrustedSocketFactory` — not referenced; the REQ-MODERNIZATION-002 deletion of this class was a prior story; `BackupImapStore` no longer accepts a `trustAllCertificates` boolean parameter (confirmed by reading `BackupImapStore.java:71-76`).

The API-level dispatch logic adds a new leading branch (`SDK_INT >= N`) before the existing API 17-23 (`SSLCertificateSocketFactory`) and pre-API-17 (reflective) branches. On API 24+ the new branch short-circuits before reaching either legacy branch, which means the `SSLCertificateSocketFactory` check and reflective fallback are now dead code on modern devices. This is the intended behaviour and introduces no bypass — the SSLParameters path is the correct, documented Android API for SNI on API 24+.

### 3. Log Demotion — No Credential or PII Exposure

The changed log statement is:

```
Log.d(LOG_TAG, "SSLSocket#setHostname(String) not available (expected on API 35+): " + e.getMessage())
```

`e.getMessage()` for a `NoSuchMethodException` thrown by `Class.getMethod("setHostname", String.class)` returns the method descriptor string (e.g., `"setHostname"`). It does not contain the `hostname` argument, any credential material, session keys, or user-identifiable information. The `hostname` variable is not interpolated into this log message. Severity is DEBUG (`Log.d`), which is suppressed in production builds by default on Android (debug logs require a debuggable build or explicit logcat tag enablement). Even on a development device where DEBUG logs are visible, no sensitive data is exposed.

The previous `Log.e` statement included the full `Throwable e` (stack trace), which was also innocuous — but its ERROR severity caused operational alarm and support noise on API 35+ devices. The demotion is correct and does not weaken any security monitoring posture since the exception signals an absent legacy method, not a security event.

### 4. Trust Bypass — None Introduced

The entire `DefaultTrustedSocketFactory` class was read in full. No changes were made to:

- The `TrustManager` array construction
- The `SSLContext` initialization
- The `socketFactory.createSocket` call
- The `hardenSocket` cipher/protocol filter
- Any return-early-or-skip pattern around `setSniHost`

There is no `trustAll`, `ALLOW_ALL_HOSTNAME_VERIFIER`, `NullTrustManager`, empty `checkServerTrusted`, or equivalent bypass pattern anywhere in the current or changed code. The `WebDavSocketFactory.java:67` call to `DefaultTrustedSocketFactory.setSniHost(...)` is also unaffected — it invokes the public static method whose behavior is now improved on API 24+.

### 5. REQ-MODERNIZATION-002 (CWE-295) Posture — Preserved

REQ-MODERNIZATION-002 requires elimination of the `AllTrustedSocketFactory` (CWE-295) and the silent `migrate()` security-downgrade path. This story (U-045) touches only the SNI-setting helper within `DefaultTrustedSocketFactory`. Reading `BackupImapStore.java` confirms `AllTrustedSocketFactory` is no longer in scope: the constructor accepts a pre-resolved `TrustedSocketFactory` with no `trustAllCertificates` boolean parameter. The trust-all bypass eliminated by prior stories remains eliminated. U-045 is orthogonal to REQ-MODERNIZATION-002's primary AC-1 through AC-10 deliverables and does not regress any of them.
