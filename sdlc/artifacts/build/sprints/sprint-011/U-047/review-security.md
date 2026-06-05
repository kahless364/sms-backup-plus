---
artifact_type: review-security
story_id: U-047
verdict: PASS
agent: Security Reviewer
timestamp: "2026-06-05"
blockers: 0
warnings: 1
---

# Security Review: U-047

## Review Summary

U-047 adds a two-layer guard in `DefaultTrustedSocketFactory.setSniViaSSLParameters` to
prevent `IllegalArgumentException` crashes when the IMAP host is an IP literal or
empty/blank string. The change is narrowly scoped to SNI hint delivery — a routing
mechanism that is not a security control — and leaves all trust anchors (certificate chain
validation, hostname verification, pinned-cert path) completely untouched.

## Verdict: PASS

---

## Findings

### Blockers

None.

### Warnings

**W-1 — IPv4 regex does not constrain octet range (informational)**

File: `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`, line 225

```java
return hostname.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
```

The pattern accepts `999.999.999.999`, which is not a valid IPv4 address but would still
be correctly classified as "skip SNI" (since `SNIHostName` would reject it anyway, and
the try/catch safety net catches that case). There is no security regression: the
worst-case outcome is that an already-invalid host address skips SNI, falls through to
the try/catch, and the TLS connection proceeds without SNI — the server is still validated
by `TrustManagerFactory.SecureX509TrustManager` regardless. The over-broad regex does not
create a bypass path for a valid DNS hostname.  No remediation required before merge;
tightening to `\b(?:25[0-5]|2[0-4]\d|[01]?\d\d?)(?:\....)` is a quality improvement that
can be addressed in a follow-on story if desired.

---

### Detailed Analysis

#### (1) SNI is a routing hint, not a security control — skipping it for IP/blank hosts does not weaken security

SNI (RFC 6066 §3) is a TLS ClientHello extension that tells the server which virtual-host
certificate to present. Its purpose is multiplexing, not authentication. RFC 6066 §3
explicitly states: "Literal IPv4 and IPv6 addresses are not permitted in 'HostName'." The
prohibition exists because IP-addressed connections do not benefit from name-based virtual
hosting; there is no SNI value to send.

Skipping `SSLParameters.setServerNames` for IP literals means:

- The TLS handshake still occurs.
- The server's certificate chain is still validated by
  `TrustManagerFactory.SecureX509TrustManager.checkServerTrusted()`, which delegates to
  the Android platform `defaultTrustManager` and then applies `StrictHostnameVerifier`.
- If the server presents a self-signed cert not in the local key store, the handshake
  fails with `CertificateChainException` as before.
- There is no bypass, downgrade, or weakening of any validation step.

The SNI omission is not exploitable: an attacker cannot leverage "SNI was not sent" to
intercept traffic, because the certificate presented by the server is still verified
against the trust store and hostname independently of whether SNI guided the server's
certificate selection.

#### (2) DNS path (Gmail / imap.gmail.com) is unchanged

The diff confirms that the existing `setSniViaSSLParameters` code path for valid DNS
hostnames is preserved verbatim:

```java
params.setServerNames(Collections.singletonList(new SNIHostName(hostname)));
socket.setSSLParameters(params);
```

These lines execute for any hostname that returns `false` from `isIpOrBlankHostname`. The
guard is a strict early-return for IP/blank inputs only. For all DNS names, execution
reaches these lines identically to the pre-change code.

#### (3) The guard cannot be tricked into skipping SNI for a real DNS hostname

Verification of `isIpOrBlankHostname`:

- `null` / blank: trivially not a DNS name.
- Colon check (`hostname.indexOf(':') >= 0`): no valid DNS label (RFC 1123) may contain a
  colon. A hostname like `imap.gmail.com` contains no colon. An attacker-controlled value
  of `imap.gmail.com:443` would be caught by the colon check and skip SNI — but this
  input would already be invalid as a TLS hostname and would fail hostname verification
  inside `StrictHostnameVerifier` regardless. The guard does not introduce a new
  exploitable surface.
- IPv4 regex: matches only strings of the form `d{1-3}.d{1-3}.d{1-3}.d{1-3}`. A DNS name
  cannot match this pattern (DNS labels contain alphabetic characters). A hostname like
  `1.2.3.4.xip.io` does not match (`5` dot-groups, not 4).
- Internationalized domain names (IDN/punycode): `isIpOrBlankHostname` returns `false` for
  punycode hostnames (e.g. `xn--nxasmq6b.com`). These pass through to `SNIHostName`,
  which accepts punycode labels. No regression.

The safety-net `try/catch IllegalArgumentException` covers any residual malformed hostname
that passes all explicit checks. In this catch branch, SNI is skipped but TLS continues;
hostname and certificate validation remain operative.

#### (4) DEBUG log content — no credential or secret exposure

Line 241:
```java
Log.d(LOG_TAG, "Skipping SNI for non-DNS hostname (IP literal or blank): " + hostname);
```

Line 251-252:
```java
Log.d(LOG_TAG, "Skipping SNI — SNIHostName rejected hostname: "
        + hostname + " — " + e.getMessage());
```

Both log calls emit only the `hostname` string (an IMAP server address — not a credential,
password, token, or private data). `hostname` is the IP or invalid string that triggered
the guard, not a username, password, or OAuth token. Logging at `DEBUG` level (not
`INFO`, `WARN`, or `ERROR`) means this output is suppressed in release builds on Android
unless debug logging is explicitly enabled. The BUG-012 requirement (no ERROR-level noise)
is preserved.

No OAuth tokens, IMAP passwords, message content, contact identifiers, or user PII appear
in any log line introduced by this change.

#### (5) No trust-all / bypass introduced

The diff introduces:
- One `isIpOrBlankHostname` helper (pure predicate, no socket interaction).
- One early-return in `setSniViaSSLParameters` for IP/blank inputs.
- One `try/catch` wrapping the existing `SNIHostName` construction.

Verified that:
- `TrustManagerFactory.get()` call at `createSocket` line 155 is unchanged.
- `hardenSocket()` call (cipher suite + protocol hardening) at line 176 is unchanged.
- `setSniHost()` dispatch logic at lines 192-205 is unchanged.
- `AllTrustedSocketFactory` is not referenced anywhere in the changed file.
- No `X509TrustManager` implementation is introduced or modified.
- No `TrustManager[]` array initialization is changed.
- No `SSLContext.init()` arguments are changed.

#### (6) REQ-MODERNIZATION-002 (CWE-295) posture preserved

REQ-MODERNIZATION-002 mandates elimination of the `AllTrustedSocketFactory` / empty
`checkServerTrusted` path and requires that every IMAP TLS connection use
`DefaultTrustedSocketFactory` by default. This story touches only `DefaultTrustedSocketFactory`
and its test. The critical properties of REQ-MODERNIZATION-002 are:

| Property | Status after U-047 |
|----------|-------------------|
| `checkServerTrusted()` performs real validation | UNCHANGED — `SecureX509TrustManager.checkServerTrusted()` delegates to `defaultTrustManager` + `StrictHostnameVerifier` + `LocalKeyStore` check |
| `AllTrustedSocketFactory` not in scope | NOT TOUCHED — U-047 does not reference or modify `AllTrustedSocketFactory` |
| `migrate()` non-downgrade constraint | NOT TOUCHED — `AuthPreferences` is outside U-047's scope |
| `SERVER_TRUST_ALL_CERTIFICATES` write sites | NOT TOUCHED |
| Cipher suite hardening (`hardenSocket`) | UNCHANGED |
| Protocol blacklist (SSLv3 excluded) | UNCHANGED |

U-047 makes no change that weakens, bypasses, or defers any of REQ-MODERNIZATION-002's
acceptance criteria. CWE-295 posture is fully preserved.

---

### Security Checklist

- [x] No hardcoded credentials or secrets — only a server hostname (not a credential) appears in log output
- [x] Input validation on all user inputs — `isIpOrBlankHostname` validates the hostname before SNIHostName construction; try/catch safety net covers residual edge cases
- [x] Output encoding prevents injection attacks — no output encoding concern; hostname is logged as a debug string with no rendering context
- [x] Authentication tokens handled securely — no authentication tokens are referenced, stored, or logged in the changed code
- [x] Authorization checks on all protected resources — N/A; this is a TLS socket-layer helper with no authorization surface
- [x] Sensitive data encrypted at rest and in transit — TLS connection proceeds in all code paths; encryption posture unchanged
- [x] Error messages do not leak internal details — DEBUG-only log lines emit only hostname; exception messages from `SNIHostName` (e.g. "Invalid SNI host name") contain no user data
- [x] Dependencies have no known critical vulnerabilities — no new dependencies introduced; vendored k9mail SSL classes unchanged except for this targeted guard
- [x] Trust-all / bypass not introduced — verified; `checkServerTrusted` and `StrictHostnameVerifier` are both active and unmodified
- [x] RFC 6066 §3 compliance — SNI correctly omitted for IP literals and empty/blank hosts as required by the RFC
- [x] BUG-012 preservation (no ERROR-level log noise) — all new log calls are `Log.d` (DEBUG level)
