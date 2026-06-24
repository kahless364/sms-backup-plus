---
artifact_type: review-security
story_id: "U-058"
verdict: "PASS"
agent: "Security Reviewer"
timestamp: "2026-06-24"
blockers: 0
warnings: 2
---

# Security Review: U-058

## Review Summary

U-058 is the security-critical story of sprint-015. It removes `org.apache.http.legacy`, replaces the deprecated `StrictHostnameVerifier` with a custom `MinimalSslSession` shim feeding `HttpsURLConnection.getDefaultHostnameVerifier()`, bumps `commons-io` 2.4 -> 2.16.1 and `apache-mime4j` 0.7.2 -> 0.8.11, drops the webdav dead-code package, and migrates mime4j API surface changes in five source files. After a line-by-line read of every changed file and the interacting SSL layer, the hostname-verification replacement is sound: the Android default hostname verifier (conscrypt's `OkHostnameVerifier` on API 21+) receives the real peer certificate chain via `MinimalSslSession.getPeerCertificates()` and the actual target hostname string, executing a genuine RFC 2818/6125 SAN/CN match. No weakening, bypass, or trust-all regression was found. The PinnedCertificateSocketFactory cert-pinning path is unchanged and intact. REQ-MODERNIZATION-002 trust posture is preserved.

## Verdict: PASS

## Findings

### Blockers

None.

### Warnings

**W-001 — `MinimalSslSession.isValid()` returns `false`; confirm verifier does not short-circuit on it (Medium / already confirmed safe)**

File: `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/TrustManagerFactory.java`, line 174

`MinimalSslSession.isValid()` returns `false`. The concern is whether Android's `OkHostnameVerifier` (the concrete implementation returned by `HttpsURLConnection.getDefaultHostnameVerifier()` on API 21+) consults `SSLSession.isValid()` before performing hostname matching. It does not: `OkHostnameVerifier.verify(String host, SSLSession session)` calls only `session.getPeerCertificates()` to retrieve the leaf certificate, then applies RFC 2818 SAN/CN matching against the supplied `host` string. The `isValid()`, `getId()`, `getSessionContext()`, `getProtocol()`, `getCipherSuite()`, and other session-metadata methods are not consulted by the verifier. The stub values (`false`, `""`, `null`, etc.) are therefore safe no-ops for this specific use case.

This is documented for future maintainers: if Android ever ships a different default hostname verifier that inspects `isValid()` before matching, the shim would need to return `true`. The risk is low and bounded — verification still runs on the real certificate chain; the worst case of a session-validity check is that verification is skipped (returning `false` from `verify()`), which `verifyHostname()` already converts to an `SSLException`. Any regression here would be caught in existing SSL transport tests. No code change required.

Severity: Medium / informational. Does not block.

**W-002 — Lenient address parsing in `Address.java`: RFC 5321 injection surface analysis (Low)**

File: `k9mail-vendored/src/main/java/com/fsck/k9/mail/Address.java`, line 144

The migration from `AddressBuilder` to `LenientAddressParser` makes the parser more permissive for malformed addresses. In SMS Backup+, `Address.parse()` is called during IMAP backup to format SMS sender/recipient fields as RFC 2822 headers. The lenient parser will accept addresses that `AddressBuilder` might have rejected; the fallback path (line 156-159) populates `mAddress = null` and `mPersonal = addressList` verbatim. The critical downstream question is whether this verbatim string reaches an IMAP APPEND command unescaped.

Read of `Address.toEncodedString()` (line 201-207) shows that the personal name goes through `EncoderUtil.encodeAddressDisplayName()` before reaching the wire, which RFC 2047-encodes non-ASCII and special characters. The address part (`mAddress = null`) would serialize as an empty address `<>` via `toString()`, which is RFC 2822-valid and not an injection vector. The lenient parsing therefore does not open a new IMAP header-injection path. The security boundary is the same as before.

Severity: Low / informational. No code change required.

### Security Checklist

- [x] No hardcoded credentials or secrets — no credential-handling code touched
- [x] Input validation — mime4j 0.8.x `LenientAddressParser` is lenient by design for RFC 2822 address parsing; downstream encoding (`EncoderUtil.encodeAddressDisplayName`) mitigates injection risk on the IMAP wire
- [x] Output encoding — `EncoderUtil.java` now uses `StandardCharsets.*` instead of `CharsetUtil.*`; the charset constants (`UTF-8`, `US-ASCII`, `ISO-8859-1`) are semantically identical; the encoding behavior is unchanged
- [x] Authentication tokens handled securely — no auth code changed
- [x] Authorization checks on all protected resources — no auth/authz code changed; webdav branches removed cleanly from `Transport.java` and `RemoteStore.java`, and those branches are dead code in SMS Backup+ (IMAP/SMTP-only)
- [x] Sensitive data encrypted at rest and in transit — no change to encryption-at-rest path; TLS hostname verification analyzed in detail below
- [x] Error messages don't leak internal details — `SSLException` message on hostname mismatch (`"Hostname '%s' was not verified against the server certificate"`) contains only the target hostname (which the app already knows) and no certificate internals; appropriate
- [x] Dependencies have no known critical vulnerabilities — `commons-io 2.16.1` and `apache-mime4j 0.8.11` are current maintained releases; both are significant upgrades from 2012/2015-vintage versions; no known CVEs at review date
- [x] Supply-chain verification — 10 new SHA-256 entries added, all specific to exact artifact/version coordinates; no wildcard entries introduced; `verify-metadata=true` remains active

## Hostname Verification Deep-Dive (REQ-MODERNIZATION-002 / BT-003)

**Claim being verified:** `HttpsURLConnection.getDefaultHostnameVerifier()` + `MinimalSslSession` is equivalent to the removed `StrictHostnameVerifier`, and does not weaken hostname verification.

**Old path (pre-U-058):** `new StrictHostnameVerifier().verify(mHost, certificate)` — Apache HttpClient 4.x `StrictHostnameVerifier` performs RFC 2818 SAN/CN matching against the leaf certificate's Subject Alternative Names (preferred) or Common Name (fallback), throwing `SSLException` on mismatch.

**New path (post-U-058):**
```
verifyHostname(mHost, chain)
  -> HttpsURLConnection.getDefaultHostnameVerifier().verify(mHost, new MinimalSslSession(chain))
  -> if (!verified) throw new SSLException(...)
```

**What `getDefaultHostnameVerifier()` returns on Android API 21+:** Conscrypt's `OkHostnameVerifier` (via `com.android.org.conscrypt`), which implements RFC 2818 §3.1 hostname matching: it checks Subject Alternative Names of type `dNSName` or `iPAddress` first; falls back to the Common Name only if no SANs are present; supports wildcard certs per RFC 2818 §3.1 rules; performs case-insensitive ASCII comparison. This is at least as strict as Apache's `StrictHostnameVerifier`.

**What `MinimalSslSession` provides:** `getPeerCertificates()` returns the full `chain` passed by `checkServerTrusted()` — the real server certificate chain extracted from the TLS handshake. All other `SSLSession` methods return safe no-ops (`""`, `0`, `null`, `false`, empty arrays) that are not consulted by `OkHostnameVerifier.verify()`.

**Call sequence integrity:** `checkServerTrusted()` in `SecureX509TrustManager` calls `defaultTrustManager.checkServerTrusted(chain, authType)` first (platform trust chain validation), then `verifyHostname(mHost, chain)`. The `chain` argument is the same array presented by the TLS handshake — it is not filtered, substituted, or truncated before being passed to `verifyHostname`. A server cannot bypass hostname verification by providing an empty chain because `chain[0]` in `checkServerTrusted` would throw `ArrayIndexOutOfBoundsException` before `verifyHostname` is reached.

**Local-keystore bypass:** The existing local-keystore path (`keyStore.isValidCertificate(certificate, mHost, mPort)`) fires only after both `defaultTrustManager.checkServerTrusted` and `verifyHostname` throw exceptions. This is the user-enrolled pinned-cert path (REQ-MODERNIZATION-002 AC-4) and is unchanged by this story.

**Conclusion:** Hostname verification is not weakened. The replacement is structurally equivalent, uses a modern Android platform verifier that implements RFC 2818/6125, and passes the real server certificate chain. The change removes a deprecated library dependency without reducing security posture.

**PinnedCertificateSocketFactory (cert-pinning path):** Read `PinnedCertificateSocketFactory.java` in full. This path is invoked independently of `TrustManagerFactory` (it constructs its own `SSLContext` with a `PinnedX509TrustManager`). It is untouched by U-058. The SHA-256 fingerprint equality check and validity-window check are intact. REQ-MODERNIZATION-002 trust posture is preserved end-to-end.

**webdav removal:** Confirmed zero production references to `com.fsck.k9.mail.store.webdav.*` outside the webdav/ directory itself. `Transport.java` and `RemoteStore.java` now throw `MessagingException`/`IllegalArgumentException` on any `webdav://` URI, which is correct defensive behavior (no such URI can be legitimately constructed in SMS Backup+'s settings flow). The webdav source files remain on disk (not deleted) but are excluded from compilation via `sourceSets.main.java.exclude`. The exclusion strategy is clean; dead class bytes are not emitted into the APK.

## Phase Completion Report
---
story_id: "U-058"
phase: "security-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-015/U-058/review-security.md"
story_status: "security-reviewed"
current_build_phase: "security-review"
blockers: 0
warnings: 2
errors: []
---
