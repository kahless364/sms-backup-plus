---
artifact_type: review-security
story_id: "U-066"
verdict: "PASS"
agent: "Security Reviewer"
timestamp: "2026-11-21"
blockers: 0
warnings: 3
---

# Security Review: U-066

## Review Summary

Story U-066 replaces the blocklist-and-reorder cipher handling of `DefaultTrustedSocketFactory` with an allow-list of ten names, intersected with the platform's default-enabled suites. I read both production files in full, the diff of all seven changed files, every TLS-socket creation site under `app/src/main` and `k9mail-vendored/src/main`, the story's rulings and measurements, CR-014 Amendment 1 (as cited by the story), the U-065 security review, and the probe and mutation evidence. No blocking issue was found. F-1 of the U-065 review is closed for every socket the IMAP and SMTP paths create. Three Low findings and several Info items remain; none needs fixing in this cycle. The one thing this review cannot establish is behaviour on Android's own Conscrypt: that is the orchestrator's IC-1 check, and section "What IC-1 must observe" says what makes it meaningful.

Scope of what I verified against the code, not the implementation log:

- `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java:54-65` holds exactly ten names. Nine are strong by construction; the tenth is the SCSV.
- `:182-191` (`ciphersToEnable`) keeps only names that are both in the allow-list and in `getEnabledCipherSuites()`. It never reads supported suites, so it cannot turn on a suite the platform left off. It returns the allow-list order.
- `:242-245` (`hardenSocket`) always calls `setEnabledCipherSuites`: the precomputed list, or, when it is null, a list computed from the socket. The old "null leaves the platform defaults" branch is gone.
- `:85-113`: the static initialiser catches `Exception` only, and on failure leaves both lists null. `hardenSocket` then recomputes from the live socket, and an exception there leaves `createSocket` without returning a socket (fail closed, not default).
- `createSocket` (`:195-223`) calls `hardenSocket` on both socket shapes (null base socket: implicit TLS; layered: STARTTLS) before `setSniHost`.
- `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java:101` calls the same `hardenSocket(SSLSocket)` on both shapes (`:93-97`). The only change in that file is a Javadoc line (`:57`); the code is unchanged.

## Verdict: PASS

## Findings

### Blockers

None (no Critical or High finding).

### Warnings

Classification follows `knowledge/standards/review-finding-classification.md`.

- **W-1 (Low). The certificate-enrollment probe socket is not hardened.** `app/src/main/java/com/zegoggles/smssync/activity/fragments/PinCertificateEnrollmentFlow.java:346-349` creates an `SSLSocket` from a default `SSLContext` with a capture-only trust manager and runs a bare handshake to read the leaf certificate; it sets no protocols and no suites. It sends no credentials or mail data, and the certificate it fetches is shown to the user for explicit consent, so a downgrade on this connection adds no trust that the user's decision does not already carry. The consequence is a consistency one: on a server that offers only pruned suites or TLS 1.1 or older, enrollment can succeed and the following backup connection then fails, which is the intended fail-closed outcome but a confusing one. This socket was not listed in U-065 F-6 (which named only the three `HttpsURLConnection` users). — Classification: defer-justified — no secret crosses the socket and the user confirms the certificate; applying the policy there is a user-visible behaviour change for the owner to decide, not a defect in this story.

- **W-2 (Low). Cipher filtering does not narrow TLS 1.3 under Conscrypt, so "fail closed" on an empty list means "fail closed for TLS 1.2", not "no handshake".** Probe I2 (`sdlc/artifacts/build/sprints/sprint-022/U-066/evidence/I2-probe.txt`, lines for `c_set_empty` and `c_hs_empty_suites`) shows Conscrypt accepts an empty suite array, keeps its three TLS 1.3 suites enabled, and a handshake with it completes at TLS 1.3 with `TLS_AES_128_GCM_SHA256`; SunJSSE enables zero suites (`j_set_empty`). I checked the consequence: the three TLS 1.3 suites are all AEAD with forward secrecy and are all on the allow-list, so a platform that enables none of the allowed TLS 1.2 suites still never negotiates a weak suite. The Javadoc at `DefaultTrustedSocketFactory.java:177-181` says only "no other suite is used instead" and defers the rest to the platform, which is accurate. Nothing to fix; recorded so nobody later reads "empty list" as "connection refused". — Classification: defer-justified — the residual behaviour is safe (only allowed suites), measured, and stated as the platform's in CR-014 "Not stated".

- **W-3 (Low). The shared lists remain mutable static arrays, and a protected overload still takes caller-supplied lists.** `ALLOWED_CIPHERS`, `ENABLED_CIPHERS`, `ENABLED_PROTOCOLS` are `protected static` non-final-content arrays (`DefaultTrustedSocketFactory.java:35-36, 54`), and `hardenSocket(SSLSocket, String[], String[])` (`:242`) is protected. This is U-065 F-4, unchanged: no subclass exists in production, and `setEnabledCipherSuites` copies its argument, so nothing in the shipped app can alter the list after class initialisation. — Classification: defer-justified — inherited finding F-4 of U-065, unreachable from production code, outside this story's scope.

### Info

- **I-1. Allow-list review (assessment item 1).** Nothing weak is admitted. Three TLS 1.3 suites (RFC 8446 B.4); four ECDHE-with-AES-GCM suites (the RFC 9325 4.2 recommended set); two ECDHE-with-ChaCha20-Poly1305 suites (RFC 7905); the SCSV, which is a signalling value and not a cipher. Every entry is AEAD with forward secrecy. Nothing needed by Gmail is missing: the measurements record TLS 1.3 `TLS_AES_128_GCM_SHA256`, and `TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256` at TLS 1.2. Dropped on purpose and worth the owner knowing: `TLS_DHE_*_GCM` (previously ranked, and an unrecoverable loss for a DHE-only server), all CBC, all static-RSA key exchange, 3DES, DSS, static ECDH.
- **I-2. API 21 to 28 (from the code's logic, not measured).** `ciphersToEnable` drops any allowed name the platform does not enable by default, so TLS 1.3 names and, where absent, the ChaCha20 names simply fall away; the result is whatever allowed ECDHE-GCM suites the platform enables. A TLS 1.3 absence matters only if the server also offers no TLS 1.2 AEAD suite. The protocol list (`protocolsToEnable`, from supported protocols) already keeps TLS 1.2 wherever the platform supports it. If a platform enabled no ECDHE-GCM suite at all the list is empty and the connection fails rather than falling back. I could not confirm from the repository which pre-API-29 releases enable ECDHE-GCM by default (it is believed from API 20 onward and is not measured for U-066: only Android 11 and 16 were). Since the owner's devices are Android 11 or later and minSdk is moving to 24, no known device is affected; if a pre-29 device is ever used, one run of the same app-process measurement would settle it.
- **I-3. Downgrade, order, renegotiation (item 3).** The client list order matters only where a server honours client preference; Gmail does not, and a server can choose only among the ten entries. The protocol floor (U-065) is untouched: `setEnabledProtocols` is still set on every path (`:244`), and a fix-up cannot come from `setEnabledCipherSuites` because suites and protocols are set separately. The app has no retry that lowers the protocol, so TLS_FALLBACK_SCSV is not needed. The SCSV is kept only when the platform enables it; Conscrypt does not support renegotiation, and where it is absent nothing is lost because modern providers send the renegotiation extension regardless.
- **I-4. Every socket path (item 2 and the bypass check).** All IMAP (`ImapConnection.java:209, 315`), SMTP (`SmtpTransport.java:218, 272`) and POP3 (`Pop3Store.java:324, 346`) sockets come from a `TrustedSocketFactory`, and the only implementations are the two factories above, which both call `hardenSocket`. Outside them: the enrollment probe (W-1), the three `HttpsURLConnection` users (U-065 F-6, unchanged, Google endpoints, platform defaults), and `WebDavSocketFactory` (unhardened, but the store, transport and factory are unreferenced from production code after U-058; dead code, not reachable). No `setEnabledCipherSuites`, `setSSLParameters`, `OkHttp` or second `SSLContext` use exists in `app/src/main` that could re-enable a suite.
- **I-5. F-1 of U-065 (item 4).** Closed for the factory sockets: no CBC, no static-RSA, no 3DES, DSS, static ECDH, or unknown-name suite can be enabled, because the filter is an allow-list, and the name-form check in the tests (below) catches a bad future entry. Other U-065 findings: F-2 (empty protocol list at a handshake) unaffected; the cipher side now has its own probe. F-3 and F-4 unchanged (W-3). F-5 (JVM-only evidence) still stands until IC-1. F-6 unchanged (W-1 adds the enrollment socket). F-7 unaffected. Nothing is made worse.
- **I-6. Tests (item 5).** They prove the property. `DefaultTrustedSocketFactoryHandshakeTest.java:369-391` and the pinned twin (`PinnedCertificateSocketFactoryCreateSocketTest.java:312-333`) run real loopback handshakes against a server limited to TLS 1.2 and to one pruned suite (ECDHE-RSA with AES-CBC-SHA1; RSA key exchange with AES-GCM), on implicit TLS and STARTTLS, on both factories, and require an `SSLException` that is not a certificate rejection and a server outcome other than OK; the control (provider defaults complete against the same server) is in probe I2 and the mutation log, so the refusal is attributable to the suite policy and not to anything else. An allowed suite is shown negotiated at TLS 1.3 and TLS 1.2 (`:353-367, 397-412`). The exact-list checks (Hardening, JDK-provider and pinned variants) and the name-form check make an added weak entry fail. Mutation evidence covers the allow-list, the null branch and the empty case (`evidence/mutations/`; M-X1a to M-X10, G-1, G-2). Limit: the refusal tests run on the desktop Conscrypt test provider under Robolectric, not on Android's system Conscrypt; the JDK-provider class asserts exact lists but not refusal (SunJSSE's disabled-algorithms property refuses to enable the pruned suites, noted in the story). TLS 1.3 suites cannot be exercised for refusal under either provider because they are provider-fixed.
- **I-7. Runtime Claims.** The implementation log declares an empty inventory. That is consistent with the story: the behaviour the story claims at handshake is proven by committed tests with real handshakes and controls (above), the empty-list question by probe I2, and the on-device claim is explicitly IC-1, the orchestrator's. No escape hatch was used and no probe wrote to the sibling plugin repository (the probe sources are kept as text files in `evidence/tools/`, and the probe classes were deleted from the tree, `evidence/I2-cleanup.txt`).
- **I-8. Test-only material and secrets (item 6).** The test diff adds no private key, e-mail address, host name, user name or user-profile path. The loopback test server binds to the loopback address; the fixtures are the existing test certificates from U-063/U-064 (not touched by this diff), and `LoopbackTlsTestServer.java` only gained a suite-limiting overload. A scan of the story file, the whole `sdlc/artifacts/build/sprints/sprint-022/U-066/` folder and the diff for e-mail patterns, user and profile names, private-key markers and mail-provider names found only a machine path inside `compiled-pipeline.json` (a framework-generated file that records the artifact path as an absolute path, not authored by the story); no credential. That path is a framework artifact, not story content; the orchestrator may wish to keep it out of any commit that is shared, but it is not a secret. The Gmail endpoint is named in the story's measurement table only as "Gmail IMAP", with no account details.

### What IC-1 must observe to be meaningful

A successful backup against Gmail alone is weak evidence: Gmail also accepts the platform defaults, so a no-op change would pass it. To show the filter is live on a device, the run must record, for the app's own IMAP socket (a temporary, uncommitted log line or debugger read of `getEnabledCipherSuites()`, `getSession().getProtocol()` and `getCipherSuite()` after the handshake):

1. Enabled suites on the Android 11 and Android 16 emulators equal the allowed ten in the allow-list order (three TLS 1.3, six ECDHE, SCSV), and none of the eight pruned names (four ECDHE-CBC-SHA, four static-RSA) is present.
2. Negotiated protocol TLS 1.3 and suite `TLS_AES_128_GCM_SHA256`, with the backup completing (login and at least one message appended).
3. Preferably one run with the same logging on the pinned-certificate path, since that shape is the second factory; a local self-hosted server limited to CBC-only or RSA-key-exchange suites, offered from the host, would give the negative case on a real Android provider, and is optional.

The temporary instrumentation must be removed before commit.

## Residual Risks

- A TLS 1.2-only server with no ECDHE AEAD suite (including a DHE-only one) can no longer be used. None is known to be in use; this is the product owner's accepted cost under Record 12, with the recorded fallback of a wider list.
- A later TLS suite or protocol is off until one entry is added to `ALLOWED_CIPHERS` or `ALLOWED_PROTOCOLS` (stated in the code and in AC-13 (e)).
- Evidence is from desktop providers plus two emulator measurements of the default lists; Android versions between 11 and 16 and pre-API-29 releases are unmeasured, and only Gmail was tried (I-2, I-6).
- The enrollment probe and OAuth/contacts connections keep platform defaults (W-1, U-065 F-6).

### Security Checklist

- [x] No hardcoded credentials or secrets (diff and story workspace scanned; test keys are existing fixtures)
- [x] Input validation: the only inputs are the platform's cipher lists; unknown or unexpected names are never enabled
- [x] Output encoding: not applicable to this change
- [x] Authentication/authorization: no trust-manager or host-name code changed; the pinned factory's trust logic is untouched
- [x] Data exposure: in-transit protection raised (CBC, static-RSA, 3DES, DSS removed); no data written
- [x] Error handling (no information leakage): no new messages; a failing computation propagates and returns no socket
- [x] Dependencies: no dependency changed

### Runtime Claims Verified

- [x] `## Runtime Claims` inventory read in the implementation log (declared empty, not omitted)
- [x] Security-relevant runtime-behaviour claims have real-handshake tests with controls (I-6, I-7); the on-device claim is IC-1, not a declared claim
- [x] Probe designs adequate on topology (real TLS handshakes on loopback, both socket shapes), failure mode (suite-only refusal, with the certificate chain held right), precondition (right-host chain, TLS 1.2 limit) and falsifiability (provider-default control; mutations detected); limit is the desktop provider
- [x] No escape-hatch justification used
- [x] Inventory completeness audited against the story's security-relevant ACs: no gap
- [x] No probe wrote into the sibling plugin repository
- [x] UNVERIFIED (escape-hatch) claims: None. Disclosed unmeasured: behaviour on Android's system Conscrypt (IC-1), and API 21 to 28 default suites (I-2)

## Phase Completion Report
---
story_id: "U-066"
phase: "security-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-022/U-066/review-security.md"
story_status: "in-review"
current_build_phase: "security-review"
blockers: 0
warnings: 3
errors: []
---
