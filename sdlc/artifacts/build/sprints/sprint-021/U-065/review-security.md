---
artifact_type: gate-result
gate: security-review
gate_id: "security-review"
checkpoint: quality-gate
story_id: "U-065"
verdict: PASS
iteration: 1
agent: "Security Reviewer"
timestamp: "2026-11-21T05:23:55Z"
base_commit: "b2c2dfd8d7f0f5de8627575e160215b24b7cc5d9"
blockers: 0
warnings: 0
---

# Security Review: U-065

## Summary

U-065 makes both TLS socket factories enable only TLS 1.2 and TLS 1.3 (an allow-list, applied after the platform's supported list is read), and gives the pinned factory the default factory's protocol, cipher-suite and server-name handling by calling the same two static steps in the same order. I read both production files in full and the production diff line by line. I found no path in either factory that enables SSLv2Hello, SSLv3, TLS 1.0 or TLS 1.1. The pinned `createSocket` still installs `PinnedX509TrustManager` and a fresh context per call, and neither new call touches the trust path. The mutation evidence is consistent with disk. I found no Critical, High or Medium finding. The residual items are Low or Info: the cipher lists were not changed (and still contain CBC, non-forward-secret and 3DES-ECDHE entries), the empty-list fail-closed claim is argued rather than tested, and no device has measured the result.

## Verdict: PASS

Basis: no code path enables a protocol older than TLS 1.2 (section 1); trust path unchanged (section 4); both production files on disk equal the recorded post-change blobs and no mutation text remains (section 5); the old-protocol refusal is shown at a real handshake with a control (section 6); no personal data in any file that would be committed (section 9).

## Findings

No Critical, High or Medium findings.

### F-1 (Low). Cipher lists still contain suites that current guidance retires (item 2)

`k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java:37-62` (known order) and `:64-81` (blocked list). What remains, after the platform's own enabled list is intersected with them:

- CBC-mode suites with SHA-1 (`TLS_ECDHE_*_AES_*_CBC_SHA`, `TLS_DHE_*_CBC_SHA`, `:44-47`, `:55-58`): usable at TLS 1.2, retired from TLS 1.3. Padding-oracle class risks are mitigated in current platform providers; no known practical attack on this use.
- Suites without forward secrecy: `TLS_RSA_WITH_AES_*_CBC_SHA` (`:50`, `:61`), static `TLS_ECDH_RSA/ECDSA_*` (`:48-49`, `:59-60`); also, as unknown-to-the-list suites that the platform enables, `TLS_RSA_WITH_AES_*_GCM_*` (appended after all known suites, so ranked last).
- 3DES: `TLS_ECDHE_*_WITH_3DES_EDE_CBC_SHA` and `TLS_ECDH_*_WITH_3DES_EDE_CBC_SHA` (`:51-54`) are in the known list, so they are not blocked (only the `SSL_*_3DES_*` names are, `:72-74`). Current platform providers do not enable them, so on a device they normally do not appear; the list does not guarantee that.
- `TLS_DHE_DSS_*` (`:47`, `:58`): DSS keys are retired and platforms rarely enable them.
- The blocked list (`:64-81`) names only the `SSL_*` forms and RC4; it has no entry for NULL, anonymous or export suites under their `TLS_*` names. Platforms do not enable those by default, so the exposure comes only from a platform that does.

Reach: the server picks the suite. Gmail negotiates TLS 1.3 (BUG-019 measurement), where none of this applies, and under TLS 1.2 a server that prefers ECDHE+GCM would pick that. The weak entries matter only for a pinned self-hosted server whose operator prefers old suites, or an active attacker who can already force TLS 1.2 and a weak choice (TLS 1.2's Finished message protects the negotiation, so the attacker would need a break of the weak suite itself). This story was not asked to change the lists, and the behaviour is the same as before for the default factory; the pinned factory now has it too (it had the platform default before, which is not worse).

Recommendation: a later, separate story to drop CBC, non-forward-secret, static-ECDH, 3DES and DSS entries from the known order and to block them by name. The list-keeping approach (a blocklist) has the structural weakness that a suite named nowhere is still enabled; an allow-list of ECDHE+AEAD suites plus the TLS 1.3 suites would be the safer shape. Needs a product-owner decision, because it can cut off an old self-hosted server.

Classification: defer-justified: out of this story's scope by the owner's decisions (Records 8 and 9 changed protocols and parity only), reachable only with a non-Gmail server or an attacker who already has a cipher break, and the change needs a decision about which servers to keep supporting.

### F-2 (Low). Empty protocol list fails closed only by argument; no test or probe shows the handshake failing (item 1)

`DefaultTrustedSocketFactory.java:172-185` returns an empty array when the provider supports neither TLS 1.2 nor TLS 1.3, and `:240` passes it to `setEnabledProtocols`. The Javadoc at `:160-171` says the socket then cannot complete a handshake "(or the platform refuses the empty list)". The probe `evidence/I2-probe.txt:6` and `:26` shows that both Conscrypt and SunJSSE accept an empty list at set time (`accepted enabled_count=0`). No test or probe then starts a handshake with zero enabled protocols. The claim is true by my understanding of both providers (no version is offered, so the handshake cannot reach a hello), and there is no fall-back branch in the code: an empty list is never replaced by the old list. Where it matters least: every supported Android version (minSdk 21, `app/build.gradle:24`) supports TLS 1.2, so the case cannot occur on a conforming platform.

Recommendation: none for the code; if a later story touches this, add one handshake against a socket with an empty enabled list.

Classification: defer-justified: the code has no fall-back, the case cannot occur on any supported platform, and the missing test would only confirm provider behaviour.

### F-3 (Low). Socket-level tests cannot tell "built from supported" from "built from enabled" (M-D11r) (item 5)

`DefaultTrustedSocketFactory.java:173` reads `getSupportedProtocols()`. Mutation M-D11r (`evidence/mutation-log.md`, entry M-D11r) switches it to `getEnabledProtocols()`. Only the unit test H3 fails; no socket-level test fails, because both test providers list the same protocols as supported and as enabled by default (Conscrypt: `I2-probe.txt:2-3`; SunJSSE enables 1.2 and 1.3 and supports both). The consequence of the mutation on a real device would be that TLS 1.2 or 1.3 left off by default by a platform (the stated reason for the supported list, `:112-117`) would stay off. That requirement is checked at the level of the pure function only. Acceptable: H3 does fail, the log records the limit openly, and on the owner's Android versions (11 and newer) the platform enables both.

Classification: defer-justified: the behaviour is covered by a failing unit test and the gap is a property of the test providers, which cannot be changed without a provider that enables fewer versions than it supports.

### F-4 (Low). Mutable shared arrays and a protected overload with caller-supplied lists (item 7)

`DefaultTrustedSocketFactory.java:97-99` (`ALLOWED_PROTOCOLS`), `:34-35` (`ENABLED_CIPHERS`, `ENABLED_PROTOCOLS`) are `protected static final` arrays: `final` does not stop a subclass in the same package or elsewhere from writing an element (for example `ALLOWED_PROTOCOLS[0] = "TLSv1"`). `:236` is `protected static void hardenSocket(SSLSocket, String[], String[])` and applies whatever list it is given. The only callers in production are `hardenSocket(SSLSocket)` (`:213`, `:227`) and the pinned factory (`PinnedCertificateSocketFactory.java:101`, via the public one-argument overload). No subclass of the factory exists in `app/src/main` or `k9mail-vendored/src/main` (searched). The new public `hardenSocket(SSLSocket)` (`:226`) takes no list: app code can call it only to narrow a socket to TLS 1.2 or later with the factory's suite order; it cannot weaken a socket, it throws nothing new, and it reads no trust manager. So there is no bypass of the floor through the new members from code in this repository. This is the same exposure the `ENABLED_*` arrays already had before the change, enlarged by one array.

Recommendation: none now. If the factory is later moved into app-owned code (CR-009), make the lists private or unmodifiable and keep the three-argument overload package-private.

Classification: defer-justified: no caller can use it to weaken a socket today, the arrays were already shared the same way, and the fix belongs with the planned move of the class.

### F-5 (Info). Handshake evidence is for the JVM test providers only (item 6)

The old-protocol refusal tests run under the Robolectric default provider (Conscrypt on a desktop JVM). See section 6 for what they prove and what they do not.

Classification: defer-justified: it needs a device or emulator run, which is the orchestrator's IC-1 step and a possible later measurement; it cannot be made in this review.

### F-6 (Info). Other TLS traffic in the app does not use these factories

`app/src/main/java/com/zegoggles/smssync/auth/OAuth2Client.java:216`, `GDataContactsAdapter.java:61` and `PeopleApiContactsAdapter.java:63` use `HttpsURLConnection` with the platform defaults. No `setEnabledProtocols` or `setSSLParameters` exists anywhere else in `app/src/main` or `k9mail-vendored/src/main` (searched), so nothing re-enables a protocol on the factory sockets (see section 1). The OAuth and contacts connections are outside both bugs; on Android 11 the platform default still includes TLS 1.0 and 1.1 (BUG-019 measurement), but the endpoints involved are Google's and the mail password is not carried on them (the app uses an app password for IMAP).

Classification: defer-justified: out of scope for BUG-019 and BUG-020, which are about the mail connection; a later story could apply the same floor if the owner wants it.

### F-7 (Info). Dead entries in the known protocol order and a possible TLS 1.4 cost

`:83-85` still lists `TLSv1.1` and `TLSv1` in `ORDERED_KNOWN_PROTOCOLS`. They can no longer reach a socket (the allow-list drops them) and the baseline test pins the constant, so they are harmless. Not changed in this story by design.

Classification: defer-justified: a production edit that changes a pinned constant, harmless as it stands.

## Review of the ten questions

### 1. The floor

- Normal path: `protocolsToEnable` (`DefaultTrustedSocketFactory.java:172-185`) removes the blocked list, orders, then keeps only names equal (case-sensitive) to `TLSv1.2` or `TLSv1.3`. `SSLv2Hello`, `SSLv3`, `TLSv1`, `TLSv1.1` and any unknown name cannot pass. Tests: `protocolsToEnable_givenSupportedList_keepsOnlySupportedTls12AndNewer` (Hardening test, with a list holding all six names) and the socket-level tests under both providers.
- Static initialiser failure: when the lists could not be computed, `ENABLED_PROTOCOLS` is `null` (`:101-128`; `enabledProtocols` stays `null` when the `try` throws) and `hardenSocket(sock, ciphers, protocols)` (`:236-241`) computes the list from the socket itself rather than leaving the platform's. If that also throws, the exception goes to the caller and the connection is not made (fail closed). Test: `hardenSocket_withNullLists_stillAppliesTheProtocolFloorFromTheSocket`; mutation M-F1 removes the fallback and H4 fails. The cipher side still leaves the platform's suites when the list is null (`:237`); the protocol floor does not depend on that.
- Provider supports neither allowed name: the list is empty and is set; no fall-back to an older list exists (F-2: fail closed, argued rather than tested).
- A provider with an unexpected name (for example lower case or a name for a future version): dropped, because the match is exact. Fail closed.
- After `createSocket`: the transports call the factory for every socket, including the STARTTLS upgrade (`ImapConnection.java:209` direct, `:315` layered over the plain socket; `SmtpTransport.java:218`, `:272`; `Pop3Store.java:324`, `:346`). A reconnect creates a new socket through the same factory. No code in `app/src/main` or `k9mail-vendored/src/main` calls `setEnabledProtocols`, `setEnabledCipherSuites` or `setSSLParameters` on a socket other than `DefaultTrustedSocketFactory` (searched). `setSSLParameters` at `:299` is the server-name step: it reads the socket's current parameters (`:296`) and writes them back with only the server names changed, so the enabled protocols stay as set by `hardenSocket`, which runs first. (If a provider's `getSSLParameters`/`setSSLParameters` round trip were to restore a protocol list, that would be provider behaviour; the socket-level tests read `getEnabledProtocols()` after the whole `createSocket`, under both providers, and pass, and M-P7 and M-P2 fail them when the step is removed.)
- Allow-list versus blocklist: an allow-list is the safer direction. A future version (a TLS 1.4) stays off until someone edits `ALLOWED_PROTOCOLS`; the cost is that the app would not use it until then, which is a loss of nothing (TLS 1.3 is current) against the alternative of enabling an unreviewed protocol. The comment at `:91-96` states it.
- TLS 1.2 and 1.3 are enabled wherever the platform supports them, even if it leaves one off by default (`:173`, supported list). Acceptable: both are current; the old rationale (supported from API 16, enabled by default from API 20) is why the supported list was used. The risk is a platform that supports a version but ships it off for a reason (for example a known defect); none is known.

### 2. Cipher suites

See F-1. The pinned factory now gets the default factory's order and blocked list, and that is a gain for it (before, it had the platform default).

### 3. Server name on the pinned path

- The host is sent as server name for a DNS name (`PinnedCertificateSocketFactory.java:102`, through `setSniHost`). The name appears in the clear in the client hello; the default factory already sent it and the host is the server's public name, not a secret. For an IP, a blank, a null host, or a name that `SNIHostName` rejects, no name is sent and nothing fails (`DefaultTrustedSocketFactory.java:291-305`; the probe `I2-probe.txt:10-21` lists the cases: IPv4, IPv6, `bad_host.example`, `-x.example`, blank, null).
- Trust: the pin still decides alone. `checkServerTrusted` (`PinnedCertificateSocketFactory.java:144-168`) is unchanged: empty-chain refusal, SHA-256 of the leaf against the enrolled certificate, then `checkValidity()`. The server name selects what the server presents; it can only change which certificate arrives, and the pin accepts or rejects that certificate. A server name that causes the wrong certificate to be served leads to a pin mismatch and a refused connection (fail closed). It cannot lead to acceptance.
- No host-name verification on the pinned path is still by design (the existing test `handshake_enrolledCertificateForDifferentHostName_isAccepted` and the M-E3 test record it). It remains acceptable: the user enrolled one certificate; the match of fingerprints identifies the server more strongly than a name match. Sending a name does not alter that. The statement in the class Javadoc (`:54-61`) is accurate.
- Fail-open on a rejected name: acceptable (the BUG-015 decision). The cost is functional (a virtual-host server may serve a different certificate and the pin then refuses); it is not a weakening. M-S5 covers it on the pinned path (item 5). Condition (2) of the U-064 review (the verifier host comes from the argument and not from the socket) is not relevant on the pinned path, which verifies no host; on the default path `TrustManagerFactory.get(host, port)` at `DefaultTrustedSocketFactory.java:192` still takes the `createSocket` argument.

### 4. Trust untouched

`PinnedCertificateSocketFactory.java:88-91` creates a new `PinnedX509TrustManager` and a new `SSLContext` per call, initialised with it and no key managers, exactly as before. The two added calls (`:101-102`) run after the socket exists and receive only the socket, the socket factory (read by `setSniHost` only for the `SSLCertificateSocketFactory` branch below API 24) and the host. `git diff` of `PinnedCertificateSocketFactory.java` shows no change inside the trust manager class (`:135-186`). The default factory's trust manager (`:192`) and `TrustManagerFactory` are not in the diff. Mutations M-E4 (comparison inverted) and M-E5 (accept-all manager) were re-run (log entries M-E4, M-E5; `evidence/mutations/M-E4-cases.txt`, `M-E5-cases.txt`): both make `handshake_certificateOtherThanEnrolled_isRejected` fail, and the Expiry test fails under both.

### 5. Mutation evidence

All 14 entries are marked `restored ... hash-ok porcelain-two-files-check=ok diff-hash-ok`. I verified the working tree myself:

- `git hash-object` of `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` gives `43e5a78e...` and of `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` gives `21cc5f34...`; both equal the log's I5 blobs.
- The hash of `git diff -- k9mail-vendored/src/main app/src/main` is `6d9a52e7...`, equal to the log's `PROD_DIFF`.
- `git status --porcelain -- app/src/main k9mail-vendored` lists exactly the two production files as modified.
- Equal blobs mean no mutation text is present; I also read both files in full and saw none.

Coverage by behaviour (each has a failing test when removed, per the log): the shared floor (M-P1/M-P8: 18 of 45 fail, including the 4 handshake refusals and the 4 pinned refusals); the pinned floor on its own (M-P2: 7 of 19); the widening of the pinned list (M-P3: 3 of 9); the pinned cipher handling on its own (M-P4: 5 of 31); the pinned server-name step on its own (M-P5: 2 of 9); drift between the factories (M-P6 (i) cipher order on the pinned side, (ii) floor on the default side, (iii) server-name step on the default side: each detected); the layered shape (M-P7: 4 of 19); the null-list fallback (M-F1: H4); the rejected-name safety net on the pinned path (M-S5: J6 and J7, plus U-064's two SNI tests); the trust manager (M-E4, M-E5). The M-P1/M-P8 control shows the old-protocol handshakes complete when the floor is gone, so the refusal tests are not vacuous.

Still without a failing test: (a) the empty-list outcome at a handshake (F-2); (b) the supported-versus-enabled choice at socket level (F-3, only H3 fails); (c) the mutable-array exposure (F-4); (d) the platform provider on a real device (F-5). None is a regression of this change.

The U-064 hand-over list: (1) trust manager untouched and no trust code touched by the hardening calls: yes; (2) SNI does not change what is verified: yes; (3) the 1.2 floor cannot disable the only protocol the loopback server negotiates (TLS 1.3 is allowed, and the TLS 1.2-only server tests complete) and has a mutation: yes (M-P1/M-P8, M-P2); (4) the pinned rejected-name test exists and has a mutation: yes (M-S5); (5) hygiene and hashes re-run, and no `@Ignore` or `Assume` added: confirmed (0 added lines in the test diff; 19 `@Test` lines added, none removed, which matches 713 to 732).

### 6. Handshake tests (TLS 1.2, 1.1, 1.0 servers)

Implementation: `DefaultTrustedSocketFactoryHandshakeTest` and `PinnedCertificateSocketFactoryCreateSocketTest` start a loopback server limited to one version (`LoopbackTlsTestServer.start(mode, ctx, protocols)`, loopback only) and connect through the factory. The refusal helper asserts the failure is an `SSLException` whose cause is not a `CertificateException` and that the server did not report `OK`. The server presents the right-host chain, so only the protocol can stop it. The TLS 1.2 tests complete at TLS 1.2 on both call shapes, so the floor does not exclude 1.2. The control: with the floor removed (M-P1/M-P8) the same handshakes complete and the refusal tests fail on their `fail(...)` message; `I2-probe.txt:7-12` shows the same at probe level (Conscrypt: with the provider default the TLS 1.1 and TLS 1.0 handshakes complete; with the floor they are refused).

Provider: the handshake tests run under the Robolectric default provider (Conscrypt on a desktop JVM), where the provider itself would offer TLS 1.0 and 1.1 by default; that is why the floor, not the provider, is what refuses. Under the JDK provider the JDK also disables them by its own property, so a handshake test there would not isolate the floor; the JDK-provider class tests the enabled list instead.

Not proved: a device's provider (on Android 16 the platform filters TLS 1.0, 1.1 and SSLv3 itself; on Android 11 it enables them); Android 11-era behaviour of `setEnabledProtocols` with this list; a downgrade forced by a third party (an active attacker who strips TLS 1.3 support can force TLS 1.2 but not TLS 1.1 or lower, which the client refuses to offer; TLS 1.2 and 1.3 carry downgrade-protection sentinels); the app's own sockets after the change.

### 7. New public API

See F-4. `hardenSocket(SSLSocket)` is public static, takes no list, and can only narrow. The protected members are reachable only from subclasses or the same package; none exists in production. Nothing in production bypasses the floor.

### 8. Test key material and secrets

The diff of the test files adds no key, certificate, password, URL with a real host or e-mail address; the existing throwaway fixtures are reused (the server uses the existing right-host chain; the names are `.test` and `.example` names). Name resolution and network access: the new tests connect to the loopback address only; I found no `InetAddress.getByName` or external connect in the added lines.

### 9. Evidence hygiene

Count-only searches, with the user name, machine name and user-profile path read from the environment at run time and not written anywhere: zero matches in `git diff`, zero in the other modified and untracked files, zero for a `Users/<name>` path pattern, zero for an e-mail pattern in the diff and in the U-065 workspace. The user name appears once, inside `sdlc/artifacts/build/sprints/sprint-021/U-065/compiled-pipeline.json` only. `git check-ignore -v` reports it ignored through the local exclude file (`.git/info/exclude:38`) and it is not listed by `git ls-files --others --exclude-standard`, so it would not be committed. `sdlc/.skill-context.json` and `sdlc/artifacts/build/sprints/sprint-021/U-064/qa-results.md` (the other untracked files) have zero matches.

### 10. Residual risk, in plain words

See the section below.

## Residual risk statement for the product owner

What is better: the app's mail connection can no longer offer TLS 1.0 or TLS 1.1, and the pinned-certificate connection, which used to take whatever the platform offered, now has the same protocol floor, the same cipher-suite order and the same server-name handling as the normal connection. On Android 11 (where the platform enables TLS 1.0 and 1.1 by default and the old list did not remove them) this changes what the app offers; on Android 16 the platform already filtered them out, so nothing changes there; the same holds for any version whose platform leaves the old versions out (the BUG-019 measurements cover only those two emulators). Gmail negotiates TLS 1.3, so a normal day's backup does not change.

What is still not shown: no measurement of the app's own sockets on a device after the change; the emulator check IC-1 (to be run by the orchestrator) proves only that the app still connects, not which version it negotiates; the old-protocol refusal is shown on a desktop JVM provider only. What is not fixed and not meant to be: the cipher lists (F-1: CBC, non-forward-secret and 3DES entries remain; the server decides, and Gmail does not pick them), and the OAuth and contacts connections (F-6), which use the platform's defaults.

Can both bugs be considered fixed from a security point of view: yes. BUG-019 (no TLS 1.0 or 1.1 from either factory) and BUG-020 (the pinned factory without protocol, cipher and server-name handling) are fixed in code, the pin still decides trust alone, and every security-relevant behaviour of the change has a test that fails when it is removed. The remaining items are Low or Info and do not reopen either bug.

## Security Checklist

- [x] No hardcoded credentials or secrets
- [x] Input validation on all user inputs (the host name is configuration; IP, blank and malformed names are handled without failure)
- [x] Output encoding prevents injection attacks (not applicable to this change)
- [x] Authentication tokens handled securely (not touched)
- [x] Authorization checks on all protected resources (the pin check is unchanged)
- [x] Sensitive data encrypted at rest and in transit (the in-transit floor is raised to TLS 1.2; F-1 lists the remaining suite weaknesses)
- [x] Error messages don't leak internal details (the new log lines name no secret; the existing SNI debug log names the host only)
- [x] Dependencies have no known critical vulnerabilities (no dependency changed)

## Runtime Claims Verified

- [x] `## Runtime Claims` inventory read in `implementation-log.md`: I read the implementation log and its evidence references (the probe file `evidence/I2-probe.txt`, the red and green case files, the mutation log); the security-relevant runtime claims are the old-protocol refusal, the floor on both shapes, and the pin still deciding trust.
- [x] Every security-relevant runtime-behavior claim has an embedded probe or test run with raw output: old-protocol refusal (`I2-probe.txt:7-12`, handshake tests, M-P1/M-P8 control), both providers' enabled lists (`I2-probe.txt`), rejected names (`I2-probe.txt:10-21`, M-S5), trust (M-E4, M-E5).
- [x] Probe design adequate: real handshakes on a loopback server limited to one version (topology), the control with the floor removed (falsifiability), the same right-host chain in every case so only the protocol can stop it (precondition). Limit: JVM providers, not a device (F-5); the empty-list case was probed only to the set step (F-2).
- [x] No escape-hatch justification is used for a security-relevant claim; the device-level claim is declared as not shown in the residual risk statement, not claimed.
- [x] Inventory completeness audited against the security-relevant runtime-behavior ACs: the one gap I found is the empty-list handshake (F-2), Low, because there is no fall-back code and the case cannot occur on a supported platform.
- [x] No probe wrote into `../amp`: the probes and mutations ran in this repository's working tree; the log and the probe cases name no path outside it.
- [x] UNVERIFIED (escape-hatch) claims: None. Disclosed as not shown: behaviour on a device's own provider; the app's own sockets after the change (IC-1 pending).
