---
type: story
status: review
sprint: '021'
artifact_type: user-story
priority: medium
complexity: low
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-064
title: Close two gaps in the TLS baseline tests - pinned expiry check and the SNI safety net on its own
pipeline: ''
domain: modernization
requirement_source: cr:CR-009
updated_at: '2026-11-21T03:15:21.247Z'
current_build_phase: validation
---

# U-064: Close Two Gaps in the TLS Baseline Tests (Pinned Expiry Check and the SNI Safety Net)

## Story
As the product owner and sole user of this private, unpublished SMS Backup+ build,
I want two tests that U-063 left out: one showing that the pinned factory rejects an expired enrolled certificate in a real handshake, and one showing that a host name which the SNI guard lets through but the JDK's `SNIHostName` rejects still cannot make `createSocket` throw,
So that when the TLS socket-factory code is moved into app-owned code (CR-009), the move cannot drop `enrolledCert.checkValidity()` or the `catch (IllegalArgumentException)` while the whole suite stays green.

Product owner decision (2026-11-20, option "Story: two missing tests"): the two gaps that the U-063 reviews and QA recorded are closed by one small, tests-only story before the CR-009 move starts. U-063 is done (commits `5a5321d4` and `cfbeada5`). This story adds to its baseline; it changes nothing it recorded.

## Scope and Delivery Shape

One story, tests only. No production behaviour changes, no code moves, no contract edit. Three new tests (the count is stated once, in IC-4).

| Gap | Source | What no test protects today | Closed by |
|-----|--------|-----------------------------|-----------|
| 1 | U-063 security review W-1 (Medium); U-063 scope concern SC-6 | The call `enrolledCert.checkValidity()` in `PinnedX509TrustManager.checkServerTrusted` (`app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java:149`). It could be deleted with every test green. | AC-1 |
| 2 | U-063 code review W1; U-063 mutation M-A1c | The `catch (IllegalArgumentException)` in `DefaultTrustedSocketFactory.setSniViaSSLParameters` (`k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java:248-253`), which is the only thing that stops an exception for a host name that passes the guard and that `SNIHostName` rejects. It could be deleted with every test green. | AC-2, AC-3 |

### Finding: gap 2 is real (checked against the code, the tests and the mutation log)

- The 12 tests in `DefaultTrustedSocketFactorySniTest` use these host names: `imap.gmail.com`, `mail.example.org`, `imap.example.com` (valid DNS names, accepted by `SNIHostName`), `192.168.1.10`, `::1`, `fe80::1`, the eight-group IPv6 literal, `""` and `"   "` (all stopped by the guard `isIpOrBlankHostname` before `SNIHostName` is reached), plus two tests of the reflective fallback that do not reach this code. The five tests of `DefaultTrustedSocketFactoryJdkProviderTest` use `imap.right.test` and the eight-group literal.
- None of them presents a name that passes the guard and is rejected by `SNIHostName`. So none reaches the `catch` while the guard is in place.
- The BUG-015 tests (`DefaultTrustedSocketFactorySniTest`, the IPv4, IPv6, empty and blank tests) look as if they cover the catch, and the comment at `DefaultTrustedSocketFactorySniTest.java:168` says "SNIHostName constructor throws IllegalArgumentException for IP literals". That is true for IPv6 (a colon is not a letter, digit or hyphen) but not for IPv4: a dotted-decimal name is made of digits and dots, which `SNIHostName` accepts. U-063's mutation M-A1g shows it: with the guard deleted and the catch kept, the IPv4 test fails (`NeverWantedButInvoked`, so `SNIHostName` accepted the name and `setSSLParameters` was called), while the empty and blank tests pass (`SNIHostName` rejects those, and the catch rescues them). The IPv4 test therefore depends on the guard alone, and the empty and blank tests depend on the guard or the catch, either one.
- Mutation M-A1c (the `catch` removed, the guard kept) left all 16 tests of the two classes green (`sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/mutation-log.md:126-135`). That is why the gap exists: every name that reaches `SNIHostName` in the current tests is one it accepts.
- Names that do reach the catch: the code reviewer ran a small JDK program and found that `new SNIHostName("bad_host.example")`, `"host.example."` and `"-x.example"` each throw `IllegalArgumentException` and `"imap.right.test"` does not (`sdlc/artifacts/build/sprints/sprint-020/U-063/review-code.md:40` and `:162`). I read `isIpOrBlankHostname` (`DefaultTrustedSocketFactory.java:216-226`): none of the three contains a colon, none is blank, and none matches the four-groups-of-digits pattern, so all three pass the guard. A user-entered IMAP host with an underscore or a trailing dot would crash `createSocket` if the catch were lost. I could not run code; the developer's first step confirms it (see Technical Context, "Developer's first step").

## Acceptance Criteria

Every criterion names the class under test, the observable, and the mutation that must make the test fail. Mutations follow IC-7. The fixed host names are `imap.right.test` (fixtures) and the three SNI names `bad_host.example`, `host.example.` and `-x.example`. JUnit 4 with `@RunWith(RobolectricTestRunner.class)`, as the neighbouring tests.

### Gap 1: pinned factory, expired enrolled certificate

- [ ] AC-1: In a new class `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryExpiryTest.java`, a test `handshake_expiredEnrolledCertificate_isRejectedAsExpired` builds `new PinnedCertificateSocketFactory(RuntimeEnvironment.getApplication(), RIGHT_HOST_NAME, certificate(EXPIRED_HOST_CERT))`, starts `LoopbackTlsTestServer` in `IMPLICIT_TLS` mode with `serverContext(EXPIRED_HOST_CERT, EXPIRED_HOST_KEY_PKCS8, TRUSTED_CA_CERT)` (so the server presents the same expired leaf that is enrolled and the SHA-256 fingerprints are equal), creates the client with `createSocket(null, RIGHT_HOST_NAME, server.port(), null)`, connects it with `LoopbackTlsTestServer.connect`, and calls `startHandshake()`. It asserts: (i) `startHandshake()` throws an `SSLException`; (ii) the direct `getCause()` is a `java.security.cert.CertificateExpiredException`; (iii) the message of that cause does not contain `Pinned certificate mismatch` (this shows the fingerprint step passed and the validity step failed). The test fails with an explicit message if the handshake completes. Breaks when: M-N1, the call `enrolledCert.checkValidity()` at `PinnedCertificateSocketFactory.java:149` is removed (the handshake then completes and the test fails at "the handshake must be rejected, but it completed"). It also breaks if the check is replaced by one that accepts an expired certificate, and if the fingerprint comparison at `:141-145` is inverted (assertion (iii)).
- [ ] AC-2: (SNI safety net, provider-independent unit level) In `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java`, a new method `setSniViaSSLParameters_nameRejectedBySniHostName_doesNotThrowAndSkipsSNI` loops over `bad_host.example`, `host.example.` and `-x.example`. For each name it uses a fresh Mockito `SSLSocket` whose `getSSLParameters()` returns a new `SSLParameters`, and asserts: (i) `isIpOrBlankHostname(name)` is `false` (called by reflection, as the existing `isIpOrBlankHostname_ipv6EightGroupLiteral_isTrue` does), which pins that the name reaches the catch and not the guard; (ii) `DefaultTrustedSocketFactory.setSniViaSSLParameters(mockSocket, name)` throws nothing; (iii) `setSSLParameters` is never called on the mock. Every assertion message names the host name, so a failure says which name failed. Breaks when: M-N2, the `catch (IllegalArgumentException)` at `DefaultTrustedSocketFactory.java:248-253` is removed with the guard left in place (the exception then escapes, and the test fails for each name). Assertion (i) breaks when the guard is widened to cover these names, which forces a conscious decision about where the protection now lives.
- [ ] AC-3: (SNI safety net, end to end through `createSocket`) In the same file, a new method `createSocket_nameRejectedBySniHostName_doesNotThrow` calls `new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication()).createSocket(null, "bad_host.example", 993, null)` and asserts that no exception is thrown and the result is an `SSLSocket`. The socket is closed in a `finally`. The socket is unconnected, so no network is touched and no name is resolved. It records the property BUG-015 was about: a host name `SNIHostName` rejects must not make the socket creation fail. Breaks when: M-N2 (the exception escapes `setSniViaSSLParameters`, through `setSniHost` at `DefaultTrustedSocketFactory.java:177` and out of `createSocket`), or when `setSniHost` is changed so that it lets such an exception through by another route.

### Observable choice (stated so a reviewer can judge it)

- Gap 2 uses "no exception, and no `setSSLParameters` call" on a mock socket (AC-2) plus "no exception from `createSocket`" (AC-3). Both are provider-independent. When the catch is removed the failure is the escaping exception itself, which every provider shows; a check that the real socket reports no server name would add nothing, because `setServerNames` is never reached when the `SNIHostName` constructor throws. So the tests stay in the default-provider class and the JDK-provider class (`DefaultTrustedSocketFactoryJdkProviderTest`, with its isolation guards) is not touched.
- Gap 1 uses a real handshake, not the package-private seam `getTrustManagerForTesting()` (`PinnedCertificateSocketFactory.java:94`). The seam builds a new trust manager and does not show what `createSocket` installs (U-063 contract-verification note N4). The handshake runs the trust manager that `createSocket` actually installs (`:77`), in the way `PinnedCertificateSocketFactoryCreateSocketTest` AC-20 and AC-21 do. The seam is not used, which also keeps the new test from repeating the 8 tests of `PinnedCertificateSocketFactoryTest`.

### Integration Criteria (integrity criteria; no production component is created)

These follow the pattern of U-063's IC-n criteria. IC numbers do not map to U-063's.

- [ ] IC-1: (baseline before any edit) Under `sdlc/artifacts/build/sprints/<sprint>/U-064/evidence/` (text only): the authoritative `@Test` count at HEAD (`git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`; 710 when this story was written, verified on 2026-11-20); the Gradle totals of a full `:app:testDebugUnitTest` run before any edit (expected 710 run, 0 failures, 2 skipped); the blob hashes of the two production files to be mutated (`git rev-parse HEAD:app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` and `HEAD:k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`). If another story has changed the count first, re-baseline here and use the new figures in IC-4.
- [ ] IC-2: (files-touched rule) `git diff --name-only <base>..HEAD` lists only paths under `app/src/test/` and this story's workspace files under `sdlc/`. The only app-test paths are the new `PinnedCertificateSocketFactoryExpiryTest.java` and the modified `DefaultTrustedSocketFactorySniTest.java` (additions only: the 12 existing methods and the existing comments are byte-identical; no `@Ignore` is added to them). `TlsTestFixtures.java` and `LoopbackTlsTestServer.java` are used, not edited (unless the product owner chooses option B of DP-1). `git diff --stat <base>..HEAD -- app/src/main k9mail-vendored build.gradle app/build.gradle settings.gradle gradle` is empty, and so is the diff of `gradle/verification-metadata.xml`. No dependency is added.
- [ ] IC-3: (gates) On JDK 25 as declared by `gradle/gradle-daemon-jvm.properties`, in a shell without `MSYS_NO_PATHCONV=1`, `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease` exits 0 before any edit (IC-1) and again at the end, with no mutation present. Every Gradle call has an explicit timeout above three minutes.
- [ ] IC-4: (count arithmetic) The authoritative count rises from 710 to 713: exactly 3 tests added (AC-1, AC-2, AC-3). The Gradle report shows 713 run, 0 failures, skipped unchanged at 2 (a test marked `@Ignore` under IC-11 adds one skipped and is listed with its bug id). If option B of DP-1 is chosen, the figure is 714.
- [ ] IC-5: (no coverage floor changes) No `limit` or `minimum` value in the JaCoCo block of `app/build.gradle` changes (the file is not edited at all, IC-2). The name of the new class contains `Test`, because the JaCoCo file filter `**/*Test*.*` (`app/build.gradle:237`) is what keeps test classes out of the production-coverage report. No new helper class is added.
- [ ] IC-6: (determinism) The new tests use no network and no name resolution: the client connects through `LoopbackTlsTestServer.connect` (loopback address object, port chosen by the operating system), never `localhost`, `127.0.0.1` as a string, `InetAddress.getByName` or a host-name `InetSocketAddress`; there is no `Thread.sleep` and no polling loop; every blocking call is bounded by the helper's 10 second timeout; every socket and the server are closed in `@After` or `finally`. Check: `git grep -nE "Thread\.sleep|getByName|getAllByName|\"localhost\"|\"127\.0\.0\.1\"|new InetSocketAddress\(\"" -- app/src/test/java/com/zegoggles/smssync/mail` shows no hit in the new or added code, and the two classes pass three consecutive runs (`./gradlew :app:testDebugUnitTest --tests "<class>" --rerun`) with identical results.
- [ ] IC-7: (mutation evidence, for the security reviewer) Two mutations, each named, temporary, single-file, and recorded in `sdlc/artifacts/build/sprints/<sprint>/U-064/evidence/mutation-log.md` using U-063's protocol: (a) a write-ahead entry before the edit (id, file, line, edit, class run, expected result), with the timestamp taken from the clock, not typed; (b) the tree is production-clean first (`git diff --quiet HEAD -- app/src/main k9mail-vendored` exit 0) and the file's blob hash equals the one in IC-1; (c) one edit with the Edit tool; (d) one targeted run of the classes named below; (e) an immediate single-file restore (`git checkout -- <file>`); (f) a `restored` line recording the hash equal to the base blob, an empty `git status --porcelain -- app/src/main k9mail-vendored` and an empty `git diff --stat HEAD -- app/src/main k9mail-vendored`; (g) the mutation is never committed and never present when a gate (IC-3) runs, a review starts or a commit is made. If a session is lost while an entry says `APPLYING` and has no `restored` line, nothing else runs until the hash is compared and the file restored.
  - **M-N1** (AC-1): in `PinnedCertificateSocketFactory.java`, replace line 149 `enrolledCert.checkValidity();` with `if (false) { enrolledCert.checkValidity(); }`. Run `PinnedCertificateSocketFactoryExpiryTest`, `PinnedCertificateSocketFactoryCreateSocketTest` and `PinnedCertificateSocketFactoryTest` in one invocation. Expected: the AC-1 test fails with "the handshake must be rejected, but it completed"; the 12 other tests pass. The failure must be that assertion, not a compile error and not a timeout.
  - **M-N2** (AC-2, AC-3): in `DefaultTrustedSocketFactory.java`, the same edit as U-063's M-A1c: replace the try/catch (`:245-253`) by its two body lines `params.setServerNames(Collections.singletonList(new SNIHostName(hostname)));` and `socket.setSSLParameters(params);`, guard kept. Run `DefaultTrustedSocketFactorySniTest` and `DefaultTrustedSocketFactoryJdkProviderTest` in one invocation. Expected: exactly the AC-2 and AC-3 tests fail (an `IllegalArgumentException`); the 12 existing SNI tests and the 5 JDK-provider tests pass, as they did under M-A1c.
  - **Written in a compiling form.** U-063 found that deleting a statement can leave a `catch` for an exception that can no longer be thrown, which does not compile (its M-B1 and M-C1, replaced by `if (false)` forms M-B1r and M-C1r). Neither mutation here strands a catch clause: line 149 is not inside a try block, and the method declares `throws CertificateException`; M-N2 removes the whole try/catch, and `IllegalArgumentException` is unchecked. The developer still checks that the edit compiles. A compile failure is logged as INVALID with the compiling replacement (for M-N1 the `if (false)` form above; for M-N2 keep the try and make the catch body `throw e;`), and is never counted as a detection.
  - A mutation that no test detects is reported as a finding, not hidden, and the criterion that named it is marked unmet. For AC-2, a name that does not make the test fail under M-N2 is a name `SNIHostName` accepts: it is replaced by one it rejects, or dropped, and the mutation log says so.
- [ ] IC-8: (evidence hygiene) No e-mail address, machine host name, Windows user name or user-profile path appears in any committed file or evidence file. Check on the full diff and the evidence folder: `git diff <base>..HEAD | grep -nEi "@[a-z0-9-]+\.[a-z]{2,}|Users[/\\\\]|AppData"` shows nothing outside the reserved test names `imap.right.test` and `imap.other.test`. Gradle logs saved as evidence are filtered to the test summary lines so that cache paths do not enter the repository.
- [ ] IC-9: (no global state) The new tests replace no JVM-global state: no host-name verifier is installed and the vendored `defaultTrustManager` field is not touched (the pinned factory builds its own trust manager; AC-2 and AC-3 call the factory only). Check: the added code contains no `setDefaultHostnameVerifier` and no reflection on `defaultTrustManager`.
- [ ] IC-10: (existing behaviour untouched) The 12 methods of `DefaultTrustedSocketFactorySniTest`, the 8 of `PinnedCertificateSocketFactoryTest` and the 4 of `PinnedCertificateSocketFactoryCreateSocketTest` still pass, are not edited and are not duplicated.
- [ ] IC-11: (defect rule, from U-063 AC-23) The story does not fix a defect. If a new test fails on unmutated production code, for example the pinned factory accepts the expired certificate, or `createSocket` throws for a rejected name, the developer stops, records the result in Implementation Notes and files a bug through `/amp:create-bug`; production code is not edited. After the bug is filed, the test is marked `@Ignore("<reason, bug id>")` the way `AuthPreferencesTest.java:1079` marks its known-limitation test, so the suite stays green, and IC-4 is read with one more skipped test.
- [ ] IC-12: (commit-based checks) Before QA starts, with the product owner's permission, the story's files are committed in one commit limited to the story's own paths (the new test class, the modified SNI test class, this story and its workspace files), so that the literal forms of IC-2, IC-4 and IC-8 can run on a committed tree. Timestamps in the implementation log and the mutation log are taken from the clock. Each count appears once in the story; later notes refer to IC-4.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` (169 lines) | `createSocket` `:75-86` installs `PinnedX509TrustManager`; `checkServerTrusted` `:126-150` compares SHA-256 fingerprints (`:141-145`) then calls `enrolledCert.checkValidity()` (`:149`). Only the second step is unprotected by a test. | None (read only; mutated temporarily under IC-7, M-N1) |
| `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` (267 lines) | `setSniHost` `:191-205` (SDK_INT 29 in tests takes the `setSniViaSSLParameters` branch); guard `isIpOrBlankHostname` `:216-226`; `setSniViaSSLParameters` `:238-254` with the guard `:239-243`, the `SNIHostName` construction `:246` and the catch `:248-253` | None (read only; mutated temporarily under IC-7, M-N2) |
| `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryExpiryTest.java` (new) | n/a | Test for AC-1; opens with a Javadoc block (what it proves, what it cannot, the mutation that breaks it, the "after the CR-009 move" paragraph, as U-063's classes) |
| `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java` | 12 `@Test`; U-063's Group A section ends at `:302`, the helper section starts at `:304` | Add 2 methods (AC-2, AC-3) between them, with a short comment block in the style of `:241-270`; additions only |
| `app/src/test/java/com/zegoggles/smssync/mail/ssl/TlsTestFixtures.java`, `LoopbackTlsTestServer.java` | Hold `EXPIRED_HOST_CERT` (`:280`), `EXPIRED_HOST_KEY_PKCS8` (`:300`), `TRUSTED_CA_CERT`, `RIGHT_HOST_NAME`, `certificate(...)` (`:338`), `serverContext(...)` (`:354`); the server helper with `start`, `connect`, `awaitOutcome` | None (used as they are) |
| `sdlc/artifacts/build/sprints/<sprint>/U-064/` | n/a | Implementation log and evidence (text only) |

## Existing Behavior to Preserve

- All production behaviour. No file under `app/src/main`, `k9mail-vendored/`, `build.gradle`, `app/build.gradle`, `settings.gradle` or `gradle/` changes (IC-2).
- The 12 SNI tests, the 8 pinned tests and the 4 pinned create-socket tests, unedited and passing (IC-10).
- Every JaCoCo floor, unchanged (IC-5); the skipped-test baseline of 2 (IC-4).
- Dependency verification: `gradle/verification-metadata.xml` untouched, with `verify-metadata=true`.
- U-063's fixtures and server helper, unedited, and their documented validity dates.
- The comment at `DefaultTrustedSocketFactorySniTest.java:168` and the stale sentence in the `PinnedCertificateSocketFactoryCreateSocketTest` Javadoc (`:77-79`, "An expired enrolled certificate is not presented ... by any test") are not edited, because an edit would break the additions-only rule. The new class's Javadoc points to them and says that the second is superseded by AC-1 (see Scope Concerns).

## Verification Steps

Run Gradle in a shell without `MSYS_NO_PATHCONV=1`. The JDK is chosen by `gradle/gradle-daemon-jvm.properties`; check with `./gradlew --version`.

1. AC-1: `./gradlew :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryExpiryTest"`; the test passes. Read the source: the enrolled certificate and the server's certificate are both `EXPIRED_HOST_CERT`; the three assertions of AC-1 are present; the client is created through `createSocket`, not through `getTrustManagerForTesting()`.
2. AC-2, AC-3: `./gradlew :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactorySniTest"`; the 14 methods pass (12 existing, 2 new). Confirm the 12 original methods are unchanged with `git diff <base>..HEAD -- app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java` (additions only).
3. IC-1, IC-3, IC-4: run the full gate set, then the count command; compare with the IC-1 evidence. The Gradle report must read 713 run, 0 failures, 2 skipped (plus any IC-11 skip).
4. IC-2: run the `git diff --name-only` and `git diff --stat` commands of IC-2 and confirm the allowed paths only.
5. IC-5: confirm that `PinnedCertificateSocketFactoryExpiryTest` is the only new class and that its name contains `Test`.
6. IC-6, IC-8, IC-9: run the greps stated in the criteria.
7. IC-7: read `evidence/mutation-log.md`: two entries, each with a write-ahead line, a result naming the failing tests, and a `restored` line with the hash. Spot-check one mutation by re-applying it, watching the named test fail, and restoring it (the retro suggests scripting the substitution with a hash check, because a QA analyst has no editing tool).
8. IC-10: the unedited tests pass in the full run.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Java, Gradle, Robolectric) | The new test class, two added methods, two mutations, gates | Developer |
| Defect tracking | `/amp:create-bug` only if IC-11 is triggered | Developer or orchestrator |

## Technical Context

### Facts established (verified by reading on 2026-11-20 unless marked)

- **T1. The check being protected.** `PinnedX509TrustManager.checkServerTrusted` (`PinnedCertificateSocketFactory.java:126-150`) throws `CertificateException` for an empty chain, then compares the SHA-256 fingerprint of the presented leaf with that of the enrolled certificate (`:141-145`), then calls `enrolledCert.checkValidity()` (`:149`), which throws `CertificateExpiredException` or `CertificateNotYetValidException` (both subclasses of `CertificateException`). The validity-window check is part of the contract: `CNTR-MODERNIZATION-001` producer table (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md:96`) and error table (`:197`) both name it. So AC-1 tests required behaviour, not a quirk.
- **T2. What the existing pinned tests cover.** `PinnedCertificateSocketFactoryTest` (8 tests) calls the trust manager through `getTrustManagerForTesting()` with the enrolled certificate (accepted), a different certificate, a null chain, `getAcceptedIssuers()` three ways and the null-certificate constructor; none presents an expired certificate. `PinnedCertificateSocketFactoryCreateSocketTest` (4 tests) runs real handshakes, but its `factory()` helper always enrols `OTHER_HOST_CERT` (valid until 2107) and its servers present `OTHER_HOST_CERT` or `RIGHT_HOST_CERT`; none involves an expired certificate. Nothing is duplicated by AC-1.
- **T3. The fixtures suffice for AC-1.** `TlsTestFixtures` holds `EXPIRED_HOST_CERT` and `EXPIRED_HOST_KEY_PKCS8` (subject `CN=imap.right.test,O=U-063 Test Only`, issuer the trusted test CA, valid 2020-01-01 to 2020-01-31, with its private key). `certificate(...)` decodes the certificate for enrolment and `serverContext(EXPIRED_HOST_CERT, EXPIRED_HOST_KEY_PKCS8, TRUSTED_CA_CERT)` gives a server that presents it. `LoopbackTlsTestServer.start(IMPLICIT_TLS, context)` serves it. No new key material, no new helper. The certificate is expired for every date after 2020-01-31, so no clock control is needed. The pinned path does not validate the chain, so the issuer plays no part. Its server key manager uses the default algorithm and U-063's AC-10 already showed the server presents this leaf (the client rejected it on the expiry path of the default factory); the developer's mutation M-N1 shows it again (with the check removed, the handshake completes).
- **T4. A not-yet-valid case is not feasible without new key material.** Every fixture in `TlsTestFixtures` has a start date on or before 2020-01-01 (header at `:64-88`); there is no certificate valid only in the future. The only ways to get one are a new `keytool` certificate and key (new key material, a new fixture constant: decision point DP-1) or a Mockito stand-in for `X509Certificate`. I do not assume either. AC-1 covers the same code line (`checkValidity()` is one call that handles both windows), so the mutation M-N1 is fully detected without it; what the second case would add is protection against replacing the call with a check of the end date only.
- **T5. Test environment (not re-probed).** The probe facts are recorded in U-063's story (`sdlc/artifacts/build/sprints/sprint-020/U-063-tls-socket-factory-characterisation-tests.md`, Implementation Notes, "Environment probe (IC-11), I0b and diagnostic I0c") and evidence (`sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/I0-probe.txt`, `I0c-diag.txt`, `I0-fixtures-summary.txt`). The relevant ones: Robolectric sdk 29, so `setSniHost` takes the `setSniViaSSLParameters` branch; the default test provider is Conscrypt, under which a client socket reports no server names whatever the factory sets (this is why AC-2 and AC-3 observe exceptions and mock calls, not server names); loopback handshakes work; the loopback server wraps the accepted plain socket with the loopback address string as host (deviation 13), which the helper already does; handshakes ran at TLS 1.3; the Robolectric host-name verifier needs no swap and is not involved in the pinned path. No environment surprise is expected. If the first run of AC-1 contradicts any of this, stop and report instead of working around it.
- **T6. Why the pinned handshake reaches the validity check.** The pinned factory builds a fresh `SSLContext("TLS")` with only its own trust manager per `createSocket` call (`:77-80`), so a resumed session cannot skip the check. The client side does not check certificate dates itself when a custom trust manager is installed; the date check in this path is the `checkValidity()` call alone, which is what the test isolates.
- **T7. Coverage gate.** The vendored module is not in any JaCoCo rule (`app/build.gradle:326-336`, U-063 E7); `mail` package coverage can only rise from AC-1. No floor is at risk.

### Developer's first step (what remains unconfirmed)

I could not run code. Before the tests are written, the developer confirms, with a throwaway local check that is not committed (a `jshell` session or a scratch test deleted afterwards): (a) `new SNIHostName("bad_host.example")`, `new SNIHostName("host.example.")` and `new SNIHostName("-x.example")` each throw `IllegalArgumentException` on JDK 25 (the code reviewer's run said so; my reading of the JDK's rules agrees: the underscore and a leading hyphen fail the strict letter-digit-hyphen rules of `IDN.USE_STD3_ASCII_RULES`, and a trailing dot is rejected explicitly), and `isIpOrBlankHostname` is `false` for all three; (b) `certificate(EXPIRED_HOST_CERT).checkValidity()` throws `CertificateExpiredException` under the Robolectric run (the certificate parser may be the JDK's or Conscrypt's; the exception class is the same by contract). The mutation runs of IC-7 then prove each test, so a wrong guess shows up as an undetected mutation, not as a wrong green. The direct cause of the `SSLException` being the `CertificateExpiredException` itself (AC-1, assertion (ii)) is expected from U-063's AC-21, where the direct cause was the `CertificateException` thrown by the same trust manager; if the provider wraps it, assert the nearest `CertificateException` in the cause chain and say why in the Javadoc.

### Design hints (only where they remove ambiguity; not a plan)

- AC-1 mirrors `PinnedCertificateSocketFactoryCreateSocketTest.handshake_certificateOtherThanEnrolled_isRejected` (`:180-196`): same server start, same `connect`, same try/catch shape, same `@After` closing the client and the server; the difference is the factory (enrolling the expired leaf) and the assertions on the cause. Build the factory with `RIGHT_HOST_NAME` as host, as that class does, so that a host-name comparison is not what rejects it. The server's outcome on a rejected handshake is not asserted (it may hang up or reset).
- Do not edit `PinnedCertificateSocketFactoryCreateSocketTest` to add the test there: it would need a new factory helper and would change a Javadoc sentence that U-063 wrote (IC-2, additions-only rule). A small separate class is the cleaner shape.
- AC-2: keep the three names in a `String[]` local to the method; use `org.mockito.ArgumentMatchers.any()` as the existing tests do.
- Class Javadoc for the new class: what the test proves (the fingerprint step passes, the validity step rejects, through the trust manager that `createSocket` installs), what it cannot prove (a device's provider; not-yet-valid certificates, see DP-1; the device clock; chain or host-name checks, which the pinned path deliberately omits), the mutation M-N1, and the "After the CR-009 move" paragraph (a missing member or class means the seam needs retargeting; a failed behavioural assertion is a regression and the test is never edited to pass).

### Known limits

- Desktop JVM, not a device: the TLS provider is Conscrypt under Robolectric or the JDK's, and `IDN` and `SNIHostName` run as the JDK implements them. Whether Android's own `SNIHostName` accepts or rejects the same three names is not shown; the catch is the safety net for whichever names the running platform rejects.
- `checkValidity()` reads the device clock at connection time. A wrong device clock changes the outcome; this story tests only that the check runs against the real clock for a certificate expired years ago.
- Only the end-of-validity case is covered (DP-1).
- AC-3 shows no exception from `createSocket`; it does not show what a device sends on the wire for such a name (none, by design).

### Scope Concerns (raised, not decided)

- **SC-1 (stale comment in the SNI test).** `DefaultTrustedSocketFactorySniTest.java:168` says `SNIHostName` throws for IP literals. It does for IPv6 but not for IPv4, so the IPv4 test relies on the guard alone (M-A1g). Not edited here (existing tests are unchanged); noted for the moved code's reviewer.
- **SC-2 (stale sentence in U-063's pinned test Javadoc).** `PinnedCertificateSocketFactoryCreateSocketTest.java:77-79` states that no test presents an expired enrolled certificate. After AC-1 that is no longer true. Left unedited under the additions-only rule; the new class's Javadoc corrects the record. If the product owner prefers the sentence fixed, it is a comment-only edit to be approved separately.
- **SC-3 (not-yet-valid case).** See DP-1.

### Decision points

- **DP-1: add a not-yet-valid case?** Option A (proposed, applies by default): no. AC-1 closes the gap named in the security review (W-1: a test that rejects an expired enrolled certificate, with a mutation that deletes the call). Option B: add one more test, `handshake_notYetValidEnrolledCertificate_isRejectedAsNotYetValid`, using a new certificate and private key generated once, offline, with the JDK `keytool` (a self-signed leaf is enough, because the pinned path does not check the chain) with a start date far enough ahead that no plausible machine clock reaches it, stored as Base64 constants in `TlsTestFixtures` with the provenance Javadoc U-063's IC-13 required (commands, subject, dates, SHA-256 fingerprint, "throwaway key"). This edits `TlsTestFixtures.java` (additions only), adds one test (IC-4 then reads 714) and one more mutation (the call replaced by an end-date-only check). A Mockito stand-in is not recommended: it would test the seam and a fake, not a certificate. Recommendation: option A, because the single `checkValidity()` call is already protected and the extra case guards against a less likely edit. The product owner decides; until told otherwise, option A applies. If the product owner chooses option B, new key material enters the repository, which is why it is a decision and not an assumption.

### Estimation Guidance

Complexity: low. 3 tests (IC-4), one new file and one extended file, no fixtures, no new helper, no environment unknowns (the probe facts exist and are cited). Most of the effort is the two logged mutations with their restore checks and the gates around them. Expect about eight Gradle invocations: the baseline gate, one targeted run per class, one per mutation, the repeated-run check, and the final gate.

Fits one sprint, as one story, and well within it. Do not split: the two gaps share a source (the U-063 reviews), a protocol (IC-7) and a deadline (before the CR-009 move). If option B of DP-1 is chosen the story is still one story.

Priority: medium. The security reviewer rated the pinned-expiry gap Medium, and nothing is wrong in production today: the check exists and works; only its protection against a later edit is missing. The private, unpublished app has no urgency from users. The reason for the ranking is ordering, not risk: the reviewer would not let the CR-009 move start without the expiry test (U-063 security review, recommendation 3), and the code reviewer wants the SNI catch covered before the same move. So this story must be done before the Part B TLS-move story and has no other deadline.

Risks: (1) a chosen SNI name turns out to be accepted on the running JDK, which the first step and M-N2 catch; (2) the provider wraps the exception so the direct cause is not `CertificateExpiredException` (fallback stated in the first step); (3) an environment surprise contradicting U-063's facts, in which case the developer stops and reports.

## Supporting Documentation
- CR-009 (`sdlc/artifacts/build/change-records/CR-009-cntr-001-default-trusted-socket-factory-producer.md`): the move these tests protect; "Tests that must exist".
- U-063 (`sdlc/artifacts/build/sprints/sprint-020/U-063-tls-socket-factory-characterisation-tests.md`): the baseline, its integrity criteria and Implementation Notes; scope concern SC-6.
- U-063 reviews and QA: `sdlc/artifacts/build/sprints/sprint-020/U-063/review-code.md` (W1, `:37-40`), `review-security.md` (W-1, `:30-35`; recommendation 3, `:191`), `qa-results.md`; mutation log `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/mutation-log.md` (M-A1g, M-A1c, M-B1/M-C1 and their replacements).
- U-063 retro (`sdlc/artifacts/build/sprints/sprint-020/retro.md`): lessons carried in (compile-checked mutations, counts stated once, clock timestamps, explicit Gradle timeouts, a commit before QA).
- BUG-015 and U-047 (`sdlc/artifacts/build/sprints/sprint-011/`): why the guard and the catch exist. Related, out of scope: BUG-019 (TLS 1.0 and 1.1 enabled) and BUG-020 (`sdlc/artifacts/stories/BUG-020-pinned-factory-no-tls-hardening.md`, the pinned factory's missing hardening), CR-012 (`sdlc/artifacts/build/change-records/CR-012-cntr-001-rejection-surfaces-at-handshake.md`).
- RFC 6066 section 3 (SNI carries DNS host names only); RFC 5280 section 4.1.2.5 (certificate validity period).

## Integration Contract References
`CNTR-MODERNIZATION-001#producer-table` (`:96`) and `#error-handling` (`:197`): the pinned producer's validity-window check and the fail-closed rule on a certificate outside its window. Read, not edited or produced. The contract wording about where a rejection surfaces (U-063 SC-1) is not this story's concern.

## Implementation Notes
**Product owner decision on DP-1, 2026-11-20** (recorded by the orchestrator; asked whether to leave out the "not yet valid" certificate case): "leave out". The story has three tests (710 to 713); no new fixture certificate is generated.

**As built (developer):** the three tests are in place and both mutations were detected and restored. Details, evidence and the criterion table: `sdlc/artifacts/build/sprints/sprint-021/U-064/implementation-log.md`. Counts: see IC-4 and `sdlc/artifacts/build/sprints/sprint-021/U-064/evidence/I6-junit-totals.txt`.

<!-- Added by agents during build. Record: the first-step results; before and after counts (IC-4); the mutation log location (IC-7); any bug id (IC-11); the DP-1 outcome. -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes
- Dependencies: none that block it. It should land before the Part B TLS-move story (CR-009). It needs nothing from BUG-019 or BUG-020.
- The 710 and 713 figures assume no other test-changing story lands first (IC-1, IC-4).
- The IP-literal and blank-host tests protect the guard; the new tests protect the catch. Between them, each layer of the BUG-015 fix is now covered on its own: the guard by the IPv4 test (and AC-2 of U-063), the catch by AC-2 and AC-3 of this story.
