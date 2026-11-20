---
artifact_type: review-security
story_id: "U-063"
verdict: "PASS"
iteration: 1
agent: "Security Reviewer"
timestamp: "2026-11-20"
blockers: 0
warnings: 7
---

# Security Review: U-063

## Review Summary

U-063 adds 28 characterisation tests for the two TLS socket factories and changes no production file. I checked this myself: the production tree, the vendored tree and the build files equal the start commit, the three TLS files hash to the same blobs, and nothing from the 39 temporary mutations survives in source or in `app/build` and `k9mail-vendored/build`. The release and debug APKs have the same SHA-256 before and after the story's clean rebuild. The baseline is sound for every protection the story claims to pin: for each one, the mutation log shows a realistic weakening turning a specific test red, and I could not find a way for a test stand-in to widen trust or hide a rejection. No CA private key, keystore file or real credential is in the files to be committed; the four leaf keys are clearly marked test-only and are for reserved `.test` names. I found no Critical or High issue. I raised nine warnings (gaps in what the baseline covers, one stale piece of bug text, one bookkeeping line, and the decision points handed to me); W-8 and W-9 were resolved in cycle on 2026-11-20 (see the delta re-check at the end), leaving seven open, all deferred. My DP-2 judgement is in its own section: Option A is acceptable, and BUG-019 should stay at medium.

## Verdict: PASS

Basis: production unchanged (verified); no secret and no CA key in the committed set (verified); trust is not widened in any committed source or evidence tool (verified); every protection the story claims to pin has a mutation that turns a test red (verified from the log and from the test code). No Critical or High finding.

## Findings

### Blockers

None.

### Warnings

**W-1 (Medium). The pinned factory's validity check is not protected by any test (SC-6).**
Location: `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java:149` (`enrolledCert.checkValidity()`).
Risk: this call is the only expiry control on the pinned path, which skips chain validation and host-name checks by design. If it were deleted, or moved after a branch that returns early, the pinned factory would keep accepting an enrolled certificate after it expired. I searched `PinnedCertificateSocketFactoryTest.java` for expiry and found no test. The new `PinnedCertificateSocketFactoryCreateSocketTest` has none either (its Javadoc says so). The fixture set already holds an expired certificate with its key (`EXPIRED_HOST_CERT`, `EXPIRED_HOST_KEY_PKCS8`, valid 2020-01-01 to 2020-01-31), so the missing test is small.
Why it matters for this story: the tests are the security baseline for a move of this code. A weakening here during the move would be invisible.
Recommendation: a small test-only follow-up, before the Part B move starts: enrol `EXPIRED_HOST_CERT`, present it through the loopback server, and assert rejection with a `CertificateException` that is not the "Pinned certificate mismatch" text (an expired but matching certificate). Include a mutation that deletes the call.
Classification: defer-justified — SC-6 is outside the approved acceptance criteria and the story's mutation evidence is closed; this is a new test with its own mutation, best done as one small follow-up item scheduled before the move.

**W-2 (Medium). BUG-019: TLS 1.0 and 1.1 are enabled by the default factory wherever the provider supports them.**
Location: `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java:107, 116-117, 186-188`.
My judgement is in the section "DP-2 and BUG-019" below: Option A is acceptable for this story, medium is the right severity today, and the decision should be taken before the move, with two cheap measurements first.
Classification: defer-justified — the fix is the product owner's keep-or-drop decision under BUG-019 AC-5, and U-063 AC-23 forbids fixing it here.

**W-3 (Low). The pinned-certificate factory applies no cipher or protocol hardening and sets no SNI.**
Location: `PinnedCertificateSocketFactory.java:77-85`; recorded by `createSocket_implicit_leavesProviderDefaultsUntouched` and `createSocket_layered_leavesProviderDefaultsUntouched` (AC-18).
It should be its own bug, severity Low. Reasons: the pinned path trusts exactly one certificate the user enrolled, so an attacker needs that certificate's private key regardless of protocol version. Platform defaults apply, and on this path they are not widened (the default factory re-enables versions from the supported list; the pinned factory does not). The absence of a cipher blocklist matters only if the platform's own defaults are weak, which is not shown. The missing SNI is a functional risk (an SNI-routed server could present another certificate, so the pin fails closed), not a security one. File it so the same AC-5 decision on protocol policy can cover both factories in one record, as BUG-019 itself suggests. Note the consequence for the baseline: AC-18 pins "no hardening" as present behaviour, so a deliberate hardening later will turn AC-18 red; that is correct, and BUG-019's Javadoc approach (retarget knowingly) applies.
Classification: defer-justified — a policy decision about an opt-in path, outside this tests-only story, to be filed by the orchestrator alongside BUG-019's decision record.

**W-4 (Low). Host-name matching rules are not pinned, and the host-name verifier in the tests is the desktop one.**
Location: `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/TrustManagerFactory.java:142-150`; tests `implicitTls_otherHost_rejectedByHostnameCheck`, `startTls_otherHost_rejectedByHostnameCheck`.
The production code delegates matching to `HttpsURLConnection.getDefaultHostnameVerifier()`. Under Robolectric that verifier matches a name against the subject alternative names; on a device it is Android's own. The only mismatch fixture is a whole different name (`imap.other.test` against `imap.right.test`). So the tests detect the verifier call being removed (M-B1r), given the wrong host (M-B3), or fed the wrong chain (M-B4). They would not detect a hand-written replacement verifier that accepts a prefix, a suffix, a `contains` match or an over-broad wildcard, and no fixture has a wildcard SAN or a CN-only certificate. This is a realistic risk only if the move replaces the verifier. It also means no test shows how the verifier treats those cases on a device.
Recommendation: when the move story is planned, add fixtures for `imap.right.test.invalid`-style suffix, `xximap.right.test` prefix, a wildcard that must not span labels, and a CN-only leaf, each expected rejected (or accepted, as the platform verifier decides, recorded as such).
Classification: defer-justified — beyond the story's approved ACs (AC-6/8 ask for a mismatch, not for matching rules), and the answer depends on the verifier the move will use.

**W-5 (Low). The per-host trust-manager cache key is not tested for cross-host reuse.**
Location: `TrustManagerFactory.java:50-61` (`host + ":" + port` key).
A regression that keys the cache by port only would hand the manager built for host A to host B on the same port, and the host-name check would run against the wrong name. The handshake tests use a fresh OS-chosen port per test, and the cache lives for the JVM, so the same-port case is not reliably exercised. I did not find a test that connects two different hosts on one port. The leaf-ignoring variant ("validate the chain but ignore the leaf") is covered: M-B4 (empty peer certificates) and the control tests turn red.
Classification: defer-justified — outside the approved ACs; belongs with the W-4 follow-up as one extra test (two factories, two host names, one port, the second host's certificate not matching).

**W-6 (Low). The certificate path is only exercised at TLS 1.3, and session resumption is not guarded.**
Location: `DefaultTrustedSocketFactoryHandshakeTest.java` Javadoc ("What they cannot prove"); `evidence/I0-probe.txt:12`.
Every handshake negotiates TLS 1.3. Nothing is shown for TLS 1.2, which is the version a hardened floor (BUG-019) would rely on and where the trust manager receives a real key-exchange auth type. The Javadoc is honest about this and about the resumption caveat: the rejection tests depend on a fresh context per `createSocket`, and a change that caches a context would not turn them red. Recommendation: run the six rejection cases and the two controls again with the loopback server limited to TLS 1.2 (the server context can set enabled protocols), and add one resumption case (same factory context, second connection to a server presenting a bad chain). This also answers BUG-019's over-tightening question for a TLS 1.2 floor.
Classification: defer-justified — new tests beyond the approved scope, best done with the BUG-019 decision.

**W-7 (Low). The production trust path (platform roots, static-initialisation failure, pinned-factory fallback) is not exercised.**
The handshake tests replace the vendored `defaultTrustManager` with a validating manager whose only anchor is the test CA. That is the right choice for the test, but it means `tmf.init(null)` with the platform roots, the null default trust manager that results if static initialisation fails (it would throw a `NullPointerException` at the first handshake, which fails closed but is not shown), and `getAcceptedIssuers()` of the default factory (contract-verification note N3) are not tested. Likewise, a change that made the pinned factory fall back to the platform trust manager on a pin mismatch could not be detected here, because the test CA is not trusted by the platform. The `getAcceptedIssuers` matter has no server-certificate effect (it is used for client-certificate selection), so I rate it a note.
Classification: defer-justified — these need a real platform trust store (a device or emulator test) or a design change; they are not reachable from a desktop JVM without widening what the tests stand in for.

**W-8 (Low). BUG-019 contained two stale statements about the "Bug id: pending" comment. RESOLVED 2026-11-20.**
Location: `sdlc/artifacts/stories/BUG-019-factory-enables-tls10-and-tls11.md`, the Evidence list (the line that said a comment at `:372-373` reads "Bug id: pending") and acceptance criterion AC-2 (the clause that the comment names BUG-019). In the test source, `DefaultTrustedSocketFactoryHardeningTest.java:372` already reads "Bug id: BUG-019 (filed)", so the bug text described a state that no longer existed.
Recommendation: reword both places to say the comment already names BUG-019 and must stay when the tests are updated. Done; verified in the delta re-check below.
Classification: resolved in cycle (2026-11-20)

**W-9 (Low). Bookkeeping: the `## Runtime Claims` line of the implementation log was not revisited at I7. RESOLVED 2026-11-20.**
Location: `sdlc/artifacts/build/sprints/sprint-020/U-063/implementation-log.md:16` (then "none declared at I0a ... revisited at I7") and `:1022` ("revisited at I7, as planned"). I audited the substance rather than the form. Every security-relevant runtime behaviour in the ACs (other-host, untrusted and expired rejection on both call shapes; the pin accept and reject; the SNI and hardening state of real sockets) is proved by a real loopback handshake or a real socket, with raw per-test output in `evidence/I4-handshake-cases.txt`, `evidence/I5-pinned-cases.txt`, `evidence/I2b-jdk-cases.txt` and `evidence/I3-hardening-cases.txt`, and with falsifiability shown by 39 logged mutations. The probe design is therefore adequate (real topology, real failure modes, preconditions asserted by control tests, falsified by mutation). No probe wrote outside this repository: all 39 mutations and all evidence are under paths in this repository, and this project has no sibling plugin repository. I treated the unrevised "none declared" as a documentation gap because no claim lacks a probe.
Recommendation: replace the line with "none: the deliverable is the tests; their runtime evidence is the I2b to I5 case files and the mutation log", and name the one limit (device behaviour). Done; verified in the delta re-check below.
Classification: resolved in cycle (2026-11-20)

### Notes (no action needed, recorded for completeness)

- **N-1. Fixtures.** `TlsTestFixtures.java` embeds ten constants: six certificates and four leaf PKCS#8 keys. I checked the constant names (no CA key constant exists), searched the six sources, the SNI test, the story folder, `evidence/` and `evidence/tools/` and BUG-019 for PEM headers, `PRIVATE KEY`, and for `.p12 .pfx .jks .pem .key .crt .csr .keystore .bks` files: none, apart from prose in the log and plan describing the keys. The class Javadoc opens with "TEST-ONLY certificates and private keys", states the keys are throwaway, that the two CA private keys were deleted, and that the password literal protects nothing. The leaf subjects and SANs are `imap.right.test` and `imap.other.test` (the `.test` top-level name is reserved by RFC 2606 and cannot resolve on the public internet), with the organisation `U-063 Test Only`. The CAs are named "U-063 Test Trusted CA" and "U-063 Test Untrusted CA". Residual risk of committing the leaf keys: negligible. A leaf key signs only for those two reserved names; a client would trust the certificates only if it had the test CA installed as an anchor, and the CA keys no longer exist, so nobody can mint new certificates. The sole practical cost is a possible false hit from a secret scanner; this repository has none configured (`.github/workflows` holds `ci.yml` only), so nothing to allow-list today.
- **N-2. Inline accept-all text in documentation.** `plan.md:1385` and `evidence/mutation-log.md` lines 467 and 596 describe the accept-all trust manager used for mutations M-B2 and M-E5, one as a code string in a table cell. It is documentation under `sdlc/`, not compiled by any source set, and it is labelled as a mutation. No action.
- **N-3. Scope concerns that are not security matters.** SC-2 (the file's package and location in CR-009) affects only the move's file list. SC-4 (`isIpOrBlankHostname` accepts `999.999.999.999` as an IPv4 literal) only skips SNI for a string that cannot be a DNS host name, so no host is wrongly given SNI and the host-name check is unaffected. SC-5 (protocol order is not a preference) is not a defect. The default-provider observation that the factory's own SNI call has no observable effect, and that the provider sends a name from the connect address on the implicit shape, is a functional matter (virtual-host routing); SNI plays no part in certificate validation, which runs against the host passed to the trust manager. A1-G3 is a test-isolation guard, not a security control; it is a sound tripwire and its failure message says it points at another test. The forward and reverse ordered-suite runs (`evidence/I7-ic9.txt`) show the guards hold.
- **N-4. SC-1 (contract wording).** The contract says the `CertificateException` is "surfaced from `createSocket(...)`"; in the code it surfaces at the handshake. This is not a vulnerability: TLS always handshakes before application data, and the tests assert the real shape. The risk is a reader of the contract looking in the wrong place. It needs a contract and change-record correction (contract-verification W1 found no approved record carrying it). Route it to the Solution Architect as an addendum to CR-007 or CR-009 before the move story is planned.

## DP-2 and BUG-019

**Is recording the TLS 1.0 and 1.1 behaviour as a passing test (Option A) acceptable?** Yes, for this story, on three conditions that are all met today: the tests carry the behaviour in their names (`...legacyProtocols_remainEnabled_...`, `...legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff`), their Javadoc says "recorded as a characterisation, not as approval" and cites BUG-019, and the bug is filed with the evidence. Option B (`@Ignore` plus a "desired state" test, also ignored) would raise the skipped count, would remove from the baseline the one test that shows the factory's own contribution, and would defeat the story's purpose, which is to let the move prove it changed nothing. A passing characterisation is not approval, and the suite does not read as approval to anyone who reads a test name. One caveat for QA and the move: the Conscrypt-form test (`createSocket_legacyProtocols_remainEnabled_whenProviderSupportsThem`) cannot tell the factory from the provider (status NOTED-AC16, mutations M-D11 and M-D12 do not touch it). The JDK-provider test is the one with teeth. Neither should be read as evidence the behaviour is safe.

**Severity.** Medium is right for now. What supports it: the factory is the default path for every `SYSTEM_VALIDATED` connection, carries the app password and the mail content, and, shown under the JDK provider, switches the two old versions on where the provider leaves them off; RFC 8996 says clients must not negotiate them. What holds it below High: certificate validation and host-name checks still run on every version (the handshake tests show validation, though only at TLS 1.3), an attacker would need an on-path position and a server that still offers a legacy version, and a TLS 1.3-capable server lets a modern client detect forced downgrade through the downgrade marker in the server random, which only helps if the server supports 1.3. None of that has been measured here: no handshake below 1.3 was run; what the owner's Android 11 or newer devices enable by default and support was not measured; what Gmail's IMAP endpoint accepts was not checked. The rating could fall to Low if the device default already excludes 1.0 and 1.1 and the endpoint refuses them, or rise to High if the platform default excludes them (so the factory is what re-enables them, as under SunJSSE) and the endpoint still negotiates them.

**What to measure first.** In this order:
1. On one owner device (and an API 24 emulator for the minimum), log `getSupportedProtocols()` and `getEnabledProtocols()` of a default `SSLContext` socket from a debug build. This one answers the question the whole bug turns on: whether the factory is re-enabling something the platform turned off, or merely repeating the platform default.
2. From a workstation, run a probe client limited to TLS 1.1 and one limited to TLS 1.2 against the owner's mail endpoint. This tells the product owner what dropping 1.0 and 1.1 costs (expected: nothing for a current provider, but it is unmeasured).
3. Run a TLS 1.2-limited loopback handshake (W-6) so a chosen floor of 1.2 cannot be over-tightened by mistake.

**Timing.** My advice follows BUG-019's own reasoning: take the decision (BUG-019 AC-5) before the Part B move starts, so that the move either carries the fix as an explicit input or is explicitly behaviour-preserving with the decision recorded. If the fix is made first, the two pinned tests, `protocolConstants_matchRecordedBaseline`, and the two "supported minus SSLv3" tests change in the same step, as the bug's impact table already lists. A floor of TLS 1.2 (allow-list, which also drops `SSLv2Hello` under the JDK provider) is the option I would put to the product owner; "keep" is also a defensible recorded decision for a private app if the two measurements show the endpoint requires 1.2 anyway.

**The pinned factory.** See W-3: its own bug, Low.

## Security Checklist

- [x] No hardcoded credentials or secrets (the only literal is the test-only key password, `TEST_ONLY_KEY_PASSWORD`, which protects an in-memory store)
- [x] No CA private key, keystore file or real credential in the files to be committed
- [x] Input validation: not applicable to tests; production input handling unchanged
- [x] Output encoding: not applicable
- [x] Authentication tokens handled securely: not touched
- [x] Authorization checks: not touched
- [x] Sensitive data encrypted in transit: characterised; see W-6 and W-7 for the limits
- [x] Error messages do not leak internal details: not touched
- [x] Dependencies: none added; `gradle/verification-metadata.xml` and build files unchanged
- [x] Trust is not widened in committed test sources or evidence tooling: the one substituted trust manager is the JDK's PKIX validator with the test CA as sole anchor; no mock, no accept-all, no disabled check, no host-name verifier replacement

## Runtime Claims Verified

- [x] `## Runtime Claims` line read in `implementation-log.md` (first pass: `none declared`, not revisited at I7, W-9; now replaced, see the delta re-check)
- [x] Every security-relevant runtime-behaviour AC has a real loopback handshake or real socket with raw per-test output and a mutation proving it can fail
- [x] Probe design is adequate: real TLS handshakes over loopback on both call shapes (topology), real certificate failures (failure-mode), control tests that read the server's data byte and assert server `OK` so a rejection cannot pass for the wrong reason (precondition), 39 logged mutations (falsifiability)
- [x] No escape-hatch entries
- [x] No probe wrote into a sibling plugin repository (this project has none); every mutation touched one of three in-repo production files and was restored
- UNVERIFIED claims: none resolved by escape hatch. See "What these tests cannot tell anyone about a device" for what is outside any probe.

## What I verified myself

| # | Check | Result |
|---|-------|--------|
| 1 | `git status --porcelain -- app/src/main k9mail-vendored` | empty |
| 2 | `git diff --stat 395000956bb46cb2acc6d66e62d4b5ef933a2d90 -- app/src/main k9mail-vendored build.gradle app/build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat` | empty |
| 3 | `git hash-object` against `git rev-parse <start>:<path>` for `DefaultTrustedSocketFactory.java`, `TrustManagerFactory.java`, `LocalKeyStore.java`, `CertificateChainException.java`, `PinnedCertificateSocketFactory.java` | all five equal (`d5439271...`, `3b448058...`, `25df2868...`, `2dc1af20...`, `8a0fbb64...`) |
| 4 | Whole-tree `git status --porcelain` | only the SNI test modified; six new test files; the story folder; BUG-019; `CR-009` one added line (`- U-063` under related stories); two sdlc bookkeeping files |
| 5 | SNI test diff | 63 insertions, 0 deletions; two tests and a comment block; no trust change |
| 6 | Compiled output: searched `app/build` and `k9mail-vendored/build` for `mutant` (the marker the log names, from M-A3 and M-B3) | no hit. No `DefaultTrustedSocketFactory$*.class` and no `PinnedCertificateSocketFactory$1` exists, which an accept-all mutation (M-B2, M-E5) would have produced. Compiled `SecureX509TrustManager` still references `verifyHostname`, `checkServerTrusted` and `isValidCertificate`. Compiled output timestamps are from the story's final clean rebuild |
| 7 | Mutation protocol as a control | Each of 39 entries has a write-ahead entry before the edit, one targeted `testDebugUnitTest` run, a single-file checkout, a hash check against the start blob and an empty-porcelain check, with a `restored` line. Mutated files were exposed for under one minute each, and only the unit-test task ran in that window; no `assemble` task is logged during any entry. A recovery rule forbids any other action after a lost session until the hash is compared (the one interruption, a machine shutdown, ended with no mutation in place). The release APK (1995017 bytes, `0b7e6aa7...2b42a`) and debug APK (6468693 bytes, `a2a237d6...e4dc`) built by the I0a gate on 2026-11-17 at 14:50 and 14:51 local, before the first mutation, are byte-identical (SHA-256) to those of the clean rebuild after the last mutation (`evidence/I7-apk-before.txt`, `evidence/I7-greps.txt`, log I7.0). A mutated class cannot have reached an APK that leaves the machine. Not verifiable from the repository: the user-level Gradle build cache, if enabled, may hold mutated outputs; they are keyed by input and inert |
| 8 | Evidence hygiene: ran the external checker over all 157 files to be committed (six sources, SNI test, story folder including `evidence/`, `evidence/tools/`, `evidence/mutations/`, BUG-019), plus `CR-009` and the skill-context file | the only hit is the story file's own IC-8 description line (known and benign); e-mail, user-name and host-name patterns: 0 hits in all files |
| 9 | Key material search (see N-1) | none |
| 10 | Read in full: `TlsTestFixtures.java`, `LoopbackTlsTestServer.java`, `DefaultTrustedSocketFactoryHandshakeTest.java`, `PinnedCertificateSocketFactoryCreateSocketTest.java`, the three TLS production files, BUG-019, the SNI diff, the DP-2 tests; the mutation log in full; story Implementation Notes; the key-generation script in `evidence/tools/` | read |
| 11 | Global state in the new tests: grep for `Security.`, `setProperty`, `insertProvider`, `setDefault`, `setAccessible` | the only global write is the vendored `defaultTrustManager` in the handshake class (set in `@Before`, restored in `@After` with a same-instance assertion); `Security.getProviders()` is read-only; no host-name verifier is replaced; leak guards fail the test if a U-063 CA or a test verifier is found in place at start |
| 12 | One Gradle run: `DefaultTrustedSocketFactoryHandshakeTest` and `PinnedCertificateSocketFactoryCreateSocketTest` | BUILD SUCCESSFUL; 8 of 8 and 4 of 4 pass; production files unchanged afterwards |

## Baseline soundness, protection by protection

| Protection | Would a realistic weakening turn a test red? | Evidence |
|-----------|----------------------------------------------|----------|
| Chain validation: untrusted CA, expired chain, both call shapes | Yes. Removing the check (M-C1r), an accept-all manager (M-B2), a fallback that accepts (M-C2): all four untrusted and expired tests fail. Right-host names, so the host check cannot be the cause (marker text asserted absent). Layered-only skipping would fail the three STARTTLS rejection tests (M-B5 replaces the layered branch) | mutation log, M-C1r, M-B2, M-C2, M-B5 |
| Host-name verification, both shapes | Yes for removal, wrong host, wrong chain (M-B1r, M-B3, M-B4, M-B2, M-C2). Not for lenient matching rules (W-4) | log |
| Pinned fingerprint comparison | Yes: inverted compare (M-E4), accept-all (M-E5). A compare on subject or issuer would also fail, because the "other" certificate has a different subject and the same issuer as the enrolled one | log; AC-21 design |
| Pin independent of host name | Yes: adding a SAN comparison (M-E3) fails AC-20 | log |
| Pin expiry check | No test at all (W-1) | existing pinned test, grep |
| SNI: no name for IP literals, name for DNS host | Yes under the JDK provider (M-A1, M-A3, M-A4 and the pre-check); AC-1/AC-2 detect guard removal at the helper level (M-A2a, M-A2b). Under the default provider the call has no observable effect, so the socket-level checks run in the JDK-provider class by product-owner decision | log |
| Cipher blocklist and order | Yes: dropped entry, swapped pair, reordered list, wrong append position (M-D1, M-D3, M-D4, M-D6a, M-D6b, M-D14) | log |
| Protocol list | Yes: SSLv3 unblocked (M-D2), reordered (M-D5), built from the enabled list (M-D11, detected by the two JDK-provider tests only), TLSv1 added to the blocked list (M-D12), the protocol call deleted (M-D13). The Conscrypt form cannot see the supported-versus-enabled difference (NOTED-AC16) | log |
| Hardening applied to both shapes | Yes: hardening skipped on the layered shape only (M-D10, AC-17) | log |

## Weakenings not tried, and where each should go

| Weakening | In this story's scope? | Where it should go |
|-----------|------------------------|--------------------|
| Validation skipped only on the layered path | Covered in effect (STARTTLS rejection tests, M-B5) | none |
| Host-name check accepts wildcard, prefix, suffix, CN-only | No | W-4 follow-up, before the move |
| Trust manager validates the chain but ignores the leaf | Covered (M-B4, controls) | none |
| `getAcceptedIssuers()` null or all (contract Validation Rule 2; note N3) | No, and no server-certificate effect | W-7 note; add a one-line test when the default factory moves |
| Cached trust manager reused across hosts | No | W-5 follow-up |
| Session resumed without re-validation | No (stated in the Javadoc) | W-6 follow-up |
| Pinned factory accepts after the enrolled certificate has expired (SC-6) | No (named as out of scope) | W-1 follow-up, before the move |
| Fallback to a permissive or platform factory on error | Partly: M-C2 covers the local key-store fallback; a pinned fallback to platform trust is not detectable on a desktop JVM | W-7 |
| Fail-open when static initialisation fails (`ENABLED_CIPHERS` or `ENABLED_PROTOCOLS` null, so no hardening is applied) | No | W-7; the result is platform defaults, not a permissive factory |

## Test stand-ins: can any of them make an unsafe configuration pass?

- **Substitute trust manager.** It is `javax.net.ssl.TrustManagerFactory.getInstance("X509")` initialised with only the test CA(s). It narrows trust and never widens it. It could hide a regression only in the platform-roots path (W-7). It is written into a `private static` field of a vendored class by reflection from a test; production code never calls the test fixtures (no production file references them), and the unit-test classes are not packaged (APKs identical).
- **Reflective write and restore.** `@After` restores the original in a `finally` and asserts the same instance; `@Before` refuses to start if a U-063 CA is already trusted. A test killed between the two would leave the narrower manager in a JVM that is discarded anyway.
- **Loopback server.** Bound to the loopback address, one accept, no client-certificate request, bounded waits, daemon thread. It cannot mask a rejection: the rejection tests demand an `SSLException` whose direct cause is `CertificateException` (and `CertificateChainException` on the default factory), `fail()` sits inside the `try` but throws `AssertionError`, which the `catch (SSLException)` does not swallow, and the controls prove the same harness completes a real handshake.
- **Verifier.** No verifier is installed. The tests use the environment's own, and fail if a test verifier is found in place.
- **Accept-all anywhere in committed sources or evidence tools:** none (search of all sources and `evidence/tools/`).

## What these tests cannot tell anyone about a device

They run on a desktop JVM, under Robolectric at a simulated API level 29, with Conscrypt's desktop build (or the JDK's own provider for one class), and every handshake negotiates TLS 1.3. So the following are unverified for the real app:

- What protocols and cipher suites a device's platform provider supports and enables at each API level, including the owner's Android 11 or newer devices (and so whether BUG-019 is exposed there).
- How Android's own host-name verifier treats wildcards, prefixes and CN-only certificates.
- Certificate validation against the platform's real root store, including revocation or pinning policy that a vendor applies, and the behaviour of the vendored trust manager when its static initialisation fails.
- Any handshake below TLS 1.3, and session resumption.
- What goes on the wire on a device for SNI (here the default provider reports no server name whatever the factory sets).
- The behaviour of the IMAP connection class around these sockets (where it triggers the handshake and how it maps the exception), and anything about Gmail's endpoint.

What would verify them: (a) an instrumented test or a debug build run on one owner device and an API 24 emulator that logs supported and enabled protocols and suites and runs the six certificate scenarios against a loopback server inside the device or against a LAN test server with the test CA added as an anchor in the test-only network configuration; (b) the TLS 1.2-limited and resumption cases from W-6 on the desktop; (c) a one-off probe client against the owner's mail endpoint at TLS 1.1 and 1.2; (d) a packet capture of one real connection if SNI on the wire matters.

## Recommendations

1. Accept the story: tests-only, production unchanged, no secret, baseline sound. Commit only the files listed in the story; leave the two bookkeeping files (`sdlc/.skill-delegation-audit.log`, `sdlc/.skill-context.json`) to the orchestrator's usual handling.
2. The two documentation-only corrections (W-8, W-9) are done; see the delta re-check below.
3. Before the Part B move is planned: take the BUG-019 decision (do the two measurements first, device protocols first); file the pinned-factory hardening bug (Low); and plan one small test-only follow-up covering W-1 (expired enrolled certificate), W-4 and W-5 (matching rules, cross-host cache) and W-6 (TLS 1.2, resumption). W-1 is the one I would not let the move start without.
4. Route SC-1 (contract wording) and SC-2 (file location) to the Solution Architect as an addendum to CR-007 or CR-009, so the move's contract and file list are correct.
5. After the commit, QA re-runs the literal forms of IC-2, IC-4, IC-6 and IC-8, as the story's deviation 2 says.

## Delta re-check after fix-in-cycle remediation (2026-11-20)

**Re-check verdict: PASS.** The remediation contains only the changes the named items require; W-8 and W-9 are resolved; no new finding. Warnings drop from 9 to 7 (W-1 to W-7, all defer-justified and unchanged). Blockers 0.

### Controls re-verified

| Check | Result |
|-------|--------|
| `git status --porcelain -- app/src/main k9mail-vendored` | empty |
| `git diff --stat 395000956bb46cb2acc6d66e62d4b5ef933a2d90 -- app/src/main k9mail-vendored build.gradle app/build.gradle settings.gradle gradle gradle.properties` | empty |
| Hashes of `DefaultTrustedSocketFactory.java`, `TrustManagerFactory.java`, `LocalKeyStore.java` against the start commit's blobs | all three equal |
| Whole-tree porcelain | same set as in my first pass: the SNI test modified, six new test files, the story folder, BUG-019, the two bookkeeping files and `CR-009`; nothing new outside the story's paths |
| Hardening and handshake test hashes on disk | `7e034437...` and `5704390e...`, equal to the "after" hashes the remediation states; the "before" hashes equal those recorded in the I4 to I6c manifests, so these are the files I reviewed, plus the diffs below |
| Release and debug APK SHA-256 after the remediation's clean rebuild | `0b7e6aa7...` and `a2a237d6...`, identical to the pre-mutation baseline |
| Accept-all, mock or verifier swap introduced | none: the two test diffs contain neither, and the repository has no class of the name the removed condition mentioned |
| Hygiene scan with the same checker over the two test sources, the log, the eight `evidence/FIX-*` files, the story file and the bug file | no e-mail, user-name, host-name or user-profile-path hit; the only line reported is the story file's own IC-8 description (now at a later line number), known and benign |
| Remediation evidence read | `FIX-targeted-cases.txt` (Hardening 9 of 9, Handshake 8 of 8 pass), `FIX-junit-totals.txt` (77 files, 710 tests, 0 failures, 0 errors, 2 skipped), `FIX-gates.txt` (BUILD SUCCESSFUL, exit 0, clean rebuild with the release build), `FIX-manifest.txt` (production paths clean; the same seven test files) |

### W-8 and W-9

- **W-8: resolved.** In the bug file I read the three places. The Evidence list line now says the comment at `:372-373` "already names BUG-019 (it read 'Bug id: pending' until this bug was filed) and must stay when the tests are updated". AC-2 now says the comment "which already names BUG-019, is kept". Verification step 2 now says the id is found "at the comment above the legacy-protocol test". Severity is still `medium` (front matter and the reasoning paragraph), AC-1, AC-3, AC-4 and AC-5 read as I reviewed them (AC-5 still leaves keep-or-drop to the product owner with the Security Reviewer's judgement), and the file is still 251 lines. The file is untracked and I have no copy of the pre-edit version, so the "nothing else changed" conclusion rests on the unchanged line count, the unchanged hash-independent reading of every other section I read in the first pass, and the coordinator's list of three rewordings; I cannot prove it byte for byte.
- **W-9: resolved.** `implementation-log.md` line 16 now reads "Runtime Claims: none. The deliverable is the tests themselves. Their runtime evidence is the per-test case files of increments I2b to I5 ... and the mutation log ... The one limit is device behaviour, which these desktop-JVM tests cannot show." That is the wording I recommended. The rest of the log is as I read it: the I0a to I7 records are at the same line numbers (the old "revisited at I7" line is still at 1022, the I7.0 heading at 1056), and the only new material is the appended `## FIX-IN-CYCLE START` to `## FIX-IN-CYCLE END` section and its handoff block, which lists exactly three items (the two code-review test edits and this log line) and states nothing else changed. The old line 1022 is left as history, which is fine.

### Diff-scope audit: every changed region and its owner

| Region | Belongs to | In scope? |
|--------|-----------|-----------|
| `BUG-019...md`: Evidence list line, AC-2, verification step 2 | W-8 (mine) | yes |
| `implementation-log.md` line 16 | W-9 (mine) | yes |
| `implementation-log.md`: appended FIX-IN-CYCLE section and handoff block | the Developer's record of the remediation | yes (record of the items, adds no behaviour) |
| `DefaultTrustedSocketFactoryHardeningTest.java`: one Javadoc sentence extended; one added assertion after the "at least 2 known ciphers" precondition | code-review W2 | yes |
| `DefaultTrustedSocketFactoryHandshakeTest.java`: one condition removed from the verifier leak guard | code-review N2 | yes |
| Story file: five insert-only "As built" lines under AC-3, AC-4, AC-19 and IC-4 and one paragraph after the test table (found by search; I did not separately locate the "Affected Code" row the coordinator mentions, which does not alter my conclusion) | code-review items (documentation of the as-built moves, deviations 14 and 16, and the 28/710 counts) | yes; descriptive, they change no criterion's wording, and nothing in them touches security behaviour |
| `evidence/FIX-*` (eight files) | evidence of the above | yes; output only |

I found no changed region outside these.

### The three judgements

1. **Does the leak-guard edit weaken the guard? No.** The removed condition tested the class name for `SanDnsHostnameTestVerifier`. That name appears in no source under `app` or `k9mail-vendored` (the only place it occurred was this guard line), so the condition could never be true. The remaining condition rejects any default host-name verifier whose class name starts with `com.zegoggles.smssync.mail.ssl.`, which is the package of every test class in this story; it is the same check that was effective before. The guard still fails the test on a leaked test verifier in that package, and the trust-manager leak guard beside it is untouched. The only theoretical loss is a hypothetical class of that name in another package, and none exists.
2. **Does the added precondition change what the cipher test proves? No, it only prevents an empty pass.** The AC-15 test checks, after removing provider-fixed suites, that known suites are in the recorded order (ii) and that unknown suites come after them (iii). The new assertion requires at least one unknown suite to remain after that removal, so check (iii) can no longer pass for lack of anything to compare. It adds a red path and removes no assertion; the unmutated run still passes (Hardening 9 of 9), so the provider does leave an unknown suite. It makes the test stricter, and the Javadoc sentence says so.
3. **Does either source edit put a mutation verdict in doubt? No.** The mutation verdicts depend on which tests fail under a mutated production file. The leak guard runs in `@Before` and can fire only if a test verifier or a U-063 CA is already in place, which no mutation introduces, so it cannot have contributed to any DETECTED verdict nor hidden a failure. The added cipher precondition can only turn more runs red. Mutations that were DETECTED by the cipher-order test (M-D4, M-D6a, M-D6b) failed on the existing ordering and set assertions, which are unchanged; the one that deletes the unknown suites (M-D6b) would now also trip the new precondition, still red. The verdicts that were BY-DESIGN or NOT DETECTED (M-A1g, M-A1c, M-E2) concern other classes the remediation did not touch. I did not re-run the mutations, and the remediation did not claim to.

### Status of deferred findings

W-1 to W-7: unchanged by the remediation and still defer-justified, each with the same classification and follow-up as in the first pass. Nothing in the remediation touched the pinned factory, the host-name matching, the trust-manager cache, the TLS 1.2 or resumption coverage, or the platform trust path. BUG-019's severity (medium) and my DP-2 judgement are unchanged. Notes N-1 to N-4 unchanged.

No Gradle run was made in this re-check; the evidence files and the unchanged hashes settled every question.

## Phase Completion Report
---
story_id: "U-063"
phase: "security-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-020/U-063/review-security.md"
story_status: "review"
current_build_phase: "review"
blockers: 0
warnings: 7
errors: []
---
