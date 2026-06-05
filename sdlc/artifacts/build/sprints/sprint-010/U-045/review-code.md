---
artifact_type: review-code
story_id: U-045
verdict: PASS
agent: "Code Reviewer"
timestamp: "2026-06-05"
blockers: 0
warnings: 2
---

# Code Review: U-045

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | PASS — CNTR-MODERNIZATION-001 invariants fully preserved; no trust model touched |
| Test coverage | PASS — 4 targeted unit tests; public-API path and private reflective fallback both exercised |
| Code quality | PASS — minimal, surgical diff; correct API-level guards; one latent warning (empty-host IAE) |

## Verdict: PASS

The fix is correct, minimal, and well-contained. SNI is now set via `SSLParameters.setServerNames` on API 24+ (eliminating the reflective call on modern platforms), the reflective fallback catch is downgraded from `Log.e`/stack-trace to `Log.d`/message-only, and no trust, cert, or cipher behaviour is altered. All four ACs are verifiable statically. No blockers found.

---

## Findings

### Blockers

None.

### Warnings

**W-1 — `SNIHostName(hostname)` throws `IllegalArgumentException` if `hostname` is null or an empty/invalid DNS name**

- File: `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`, line 214
- `setSniViaSSLParameters` calls `new SNIHostName(hostname)` without a null or empty check. `SNIHostName` (javax.net.ssl) throws `IllegalArgumentException` for null, empty, or syntactically invalid hostnames. In the `createSocket` call chain the host value comes from a parsed IMAP URI (`ImapConnection.java:209`), so it will rarely be blank in practice — but an IP-address literal (e.g. `192.168.1.1`) would also throw `IllegalArgumentException` from `SNIHostName` because SNI is defined for DNS names only (RFC 6066 §3).  The pre-existing code had no such guard either, but the new public-API path surfaced the concern more clearly.
- Severity: warning, not blocker. Current callers supply a resolved DNS name. The `setHostnameViaReflection` path also has no guard and was not flagged pre-fix. Fix recommendation: add an `if (hostname == null || hostname.isEmpty() || isIpAddress(hostname))` guard before constructing `SNIHostName`, consistent with the way older SDKs handle IP-addressed IMAP connections.

**W-2 — Test for `setSniViaSSLParameters` uses a verified `params` reference mutation, but `setSSLParameters` receives the same object instance mutated before the verify call**

- File: `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java`, lines 52–59
- The test calls `DefaultTrustedSocketFactory.setSniViaSSLParameters(mockSocket, "imap.gmail.com")`, then reads `serverNames` from the same `params` object to assert correctness. Because `mockSocket.getSSLParameters()` returns the same mutable `SSLParameters` instance both in the production call and in the test assertion, the assertion always reads the already-mutated object — this is correct behaviour, but it is an implicit aliasing assumption. If `setSniViaSSLParameters` were ever refactored to call `getSSLParameters()` a second time internally, the test might silently pass with a stale read. The assertion on `params.getServerNames()` rather than on the argument captured by the `verify()` call is a minor test-design smell. Not a defect in the current code.
- Severity: warning (test robustness). Recommendation: capture the `SSLParameters` argument via `ArgumentCaptor` and assert on the captured value rather than on the original `params` reference.

### Observations

**O-1 — `Build.VERSION_CODES.N` (24) correctly covers API 35+ (`Java8EngineSocket`) — confirmed**

The guard `Build.VERSION.SDK_INT >= Build.VERSION_CODES.N` at line 192 is the right threshold. `SNIHostName` and `SSLParameters.setServerNames` were added in API 24 (Android N). On API 35+ (conscrypt `Java8EngineSocket`) the reflective path is now bypassed entirely. The pre-existing `JELLY_BEAN_MR1` (API 17) branch for `SSLCertificateSocketFactory` is correctly retained for the API 17–23 gap, and the pre-17 reflective path remains for completeness. The three-tier dispatch is logically sound.

**O-2 — `Collections.singletonList` (already imported) is correct here**

`SSLParameters.setServerNames` accepts `List<? extends SNIServerName>`. Using `Collections.singletonList(new SNIHostName(hostname))` is idiomatic and produces an unmodifiable single-element list, which is the correct contract (one SNI name per connection). No issue.

**O-3 — `WebDavSocketFactory.java:67` also calls `setSniHost` — verified unaffected**

`WebDavSocketFactory` calls `DefaultTrustedSocketFactory.setSniHost(mSocketFactory, sslSocket, host)` directly. The method signature is unchanged; the factory argument it passes is an `SSLCertificateSocketFactory` (the `mSocketFactory` field). On API 24+ the new N-branch would be taken (API level, not factory type), overriding the `SSLCertificateSocketFactory` path that previously handled WebDav SNI on API 17+. This is a silent behaviour change for WebDav on API 24+: instead of going through `SSLCertificateSocketFactory.setHostname()` it now goes through `SSLParameters.setServerNames()`. The outcome is equivalent (SNI is set), but reviewers of future WebDav work should be aware of this routing change. No defect.

**O-4 — `setSniViaSSLParameters` exposed as `public static` for test access is reasonable**

Extracting the method as `public static` rather than using `@VisibleForTesting` or package-private is a minor visibility widening, but given the method is already an extension of the `public static setSniHost` it is consistent. No issue.

**O-5 — Capabilities Inventory in implementation-log.md is complete and accurate**

All six RETAINED capabilities verified at the cited file:line references. No capability silently dropped.

**O-6 — Domain standard conformance (CNTR-MODERNIZATION-001, REQ-MODERNIZATION-002)**

CNTR-MODERNIZATION-001 governs the `TrustedSocketFactory` handoff boundary — trust resolution, never-trust-all invariant, and the `DefaultTrustedSocketFactory` contract. U-045 modifies only the SNI-setting internals of `DefaultTrustedSocketFactory`; it does not alter the `TrustManager` construction (`TrustManagerFactory.get` at line 155), the `createSocket` method signature, the `hardenSocket` cipher/protocol filter, or the client-certificate/KeyChain path. The factory still fully validates the TLS chain against the system CA store. REQ-MODERNIZATION-002 (eliminate MITM exposure) is unaffected — no code path is relaxed, no trust-all factory is reintroduced. Domain standard compliance is confirmed.

---

## Patterns Verified

- [x] Follows existing code patterns — three-tier `setSniHost` dispatch pattern is the established form; new tier prepended cleanly
- [x] Error handling is appropriate — `catch (Throwable e)` scope is unchanged and intentional (catches both `NoSuchMethodException` and any invocation error); log level correctly downgraded
- [x] Tests cover new functionality — `setSniViaSSLParameters` (two tests) and `setHostnameViaReflection` fallback (two tests); FakeSSLSocket correctly mirrors `Java8EngineSocket` absence of `setHostname`
- [x] No hardcoded values that should be configurable — `Build.VERSION_CODES.N` (24) is the correct SDK constant, not a raw integer
- [x] No unnecessary complexity — +34/-3 lines; extracted helper is small and single-purpose

## Integration Verified

- [x] New code is reachable from production entry points — `ImapConnection.createSocket` → `DefaultTrustedSocketFactory.createSocket:177` → `setSniHost:191` → `setSniViaSSLParameters:197` (API 24+ path); also reached via `WebDavSocketFactory.java:67`
- [x] Registries/dispatch maps updated for new implementations — N/A (no registry pattern here; `setSniHost` is a static utility method)
- [x] Function signatures match at all call sites — `setSniHost(SSLSocketFactory, SSLSocket, String)` unchanged at all callers
- [x] No dead code introduced — all three branches of `setSniHost` are reachable at their respective API levels
- [x] Integration path documented in implementation-log.md — yes, at the "Integration Path" section

## Regression Check (not a replacement/rewrite story — capabilities audit conducted as precaution)

- [x] Capabilities Inventory present in implementation-log.md
- [x] Every RETAINED item verified present in new code (file:line cited in implementation-log and confirmed in review)
- [x] No INTENTIONALLY REMOVED items — the change adds a new branch and quiets a log; nothing removed
- [x] No capabilities missing from inventory

## Contract Verification

- [x] All consumers of modified interfaces identified — `ImapConnection`, `WebDavSocketFactory`, `SmtpTransport`, `Pop3Store` (all via `setSniHost`; signature unchanged)
- [x] Field names, event names, data shapes agree between producer and consumer — no data contract crossing; SNI is internal to socket setup
- [x] No contract mismatches between backend and frontend — no frontend component involved

## Phase Completion Report
---
story_id: "U-045"
phase: "code-review"
verdict: "PASS"
artifact_path: "C:/Code/Android/sms-backup-plus/sdlc/artifacts/build/sprints/sprint-010/U-045/review-code.md"
story_status: "review"
current_build_phase: "validation"
blockers: 0
warnings: 2
errors: []
notes: "W-1: SNIHostName constructor throws IAE for null/empty/IP-address hostname — no guard present, latent risk at non-DNS hosts. W-2: test params-mutation aliasing smell, not a defect. Neither is a blocker."
---
