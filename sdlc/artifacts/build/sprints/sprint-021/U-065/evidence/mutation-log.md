# U-065 mutation log

Protocol: U-065 plan section I7 (write-ahead entry, one Edit, one targeted run, restore by copying the pristine file, hash and porcelain check, `restored` line). Repository-relative paths.

- Base commit: `b2c2dfd8d7f0f5de8627575e160215b24b7cc5d9`
- Base blobs: pinned factory `8a0fbb64fb171624591fbebafb818555f28efd58`, default factory `d543927166b3c5e1da0aaa8ab584072a43bf7521`
- I5 blobs (the restore targets, also stored in the object store): default factory `43e5a78ead7c4c32a6af2734514ab8ba5d5ccbd3`, pinned factory `21cc5f345977ab6fa161b2aa6925a04aea6a1479`; hash of the whole production diff against HEAD (`PROD_DIFF`): `6d9a52e7fa59c26a9993da0611e6b50fd8266704`
- Recovery note: if a session is lost while an entry says `APPLYING` and has no `restored` line, nothing else runs (no gate, no review, no commit) until the file's hash has been compared with its I5 blob and, if it differs, the file has been restored from the pristine copy; if that copy is lost, from the object store with `git cat-file blob <DTSF_NEW or PIN_NEW> > <path>` (it writes LF line endings; with `core.autocrlf=true` and the files stored as LF, `git hash-object` and the diff hash still match, and the next Gradle compile reads the file as before); if both are lost, by re-applying section 2.4 of the plan to the base file and checking the hash.
- Declared deviation from AC-7's literal protocol (plan I7): the production change is uncommitted while the mutations run, so (1) the restore copies the pristine file back instead of `git checkout` (which would discard the change), and (2) "empty `git status --porcelain -- app/src/main k9mail-vendored`" becomes "porcelain lists exactly ` M` for the two production files, and the diff hash equals `PROD_DIFF`". The hashes compared are the I5 blobs.
- Tooling: each run is one Gradle call with the read-only init script of U-063 (JUnit XML on), foreground, timeout 600000 ms. Raw logs stay outside the repository; `evidence/mutations/<id>-cases.txt` and `<id>-message.txt` are filtered by `sanitize.py`.
- Failure types: A = an assertion failure (`java.lang.AssertionError` or a subclass); a named class is an exception that escapes the test.

## Summary

| Id | Criterion | Failing / of | Verdict |
|----|-----------|--------------|---------|
| M-P1/M-P8 (one entry) | AC-7 M-P1 (floor removed, default factory; both providers), M-P8 (old-protocol handshake completes) | 18 / 45 | DETECTED |
| M-P2 | AC-7 M-P2 (pinned protocol handling removed) | 7 / 19 | DETECTED |
| M-P3 | AC-7 M-P3 (pinned list widened) | 3 / 9 | DETECTED |
| M-P4 | AC-7 M-P4 (pinned cipher handling removed on its own) | 5 / 31 | DETECTED |
| M-P5 | AC-7 M-P5 (pinned server-name step removed on its own) | 2 / 9 | DETECTED |
| M-P6 (i) | AC-7 M-P6 (cipher order swapped, pinned only) | 5 / 31 | DETECTED |
| M-P6 (ii) | AC-7 M-P6 (floor removed, default only) | 11 / 45 | DETECTED |
| M-P6 (iii) | AC-7 M-P6 (server-name step removed, default only) | 2 / 9 | DETECTED |
| M-P7 | AC-7 M-P7 (handling on the direct shape only) | 4 / 19 | DETECTED |
| M-F1 | AC-1 (e) (null-list fallback removed) | 1 / 12 | DETECTED |
| M-D11r | AC-1 (d) (list built from the enabled protocols) | 1 / 21 | DETECTED by H3 only (no socket-level test) |
| M-S5 | U-064 hand-over (pinned safety net removed) | 4 / 23 | DETECTED |
| M-E4 | AC-7 (re-run: fingerprint comparison inverted) | 5 / 11 | DETECTED (`handshake_certificateOtherThanEnrolled_isRejected` fails) |
| M-E5 | AC-7 (re-run: accept-all trust manager) | 2 / 11 | DETECTED (`handshake_certificateOtherThanEnrolled_isRejected` fails) |

14 entries, 14 runs, all restored; no entry was invalid, mismatched or undetected. The two-sentence reading of M-P1/M-P8: under the shared design the floor is one step, so one edit removes it from both factories; the one-factory forms are M-P2 (pinned) and M-P6 (ii) (default), each of which also fails its refusal tests.

### M-P1-P8 APPLYING 2026-11-21T04:55:49Z
- mutation (M-P1 and M-P8, one entry: under the shared design the floor is one step): in `protocolsToEnable`, `return enabled.toArray(new String[enabled.size()]);` becomes `return ordered;` (the floor removed from the shared step: the old supported-minus-SSLv3 list, for both factories)
- classes run: Hardening, JdkProvider, Handshake, CreateSocket (the four that carry the expected failures)
- expected failing: H1 (set equality, A), H2 (TLSv1 must not be enabled, A), H3 (case i, A), H4 (captured list, A), J1, J2, J3, J4, C1, C2 (A), the 4 Handshake and the 4 pinned refusal tests (fail(...) as AssertionError, A: the M-P8 control). Not failing: J7, H5, J5, J6, the TLS 1.2 tests
- file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
- hash-before: 43e5a78ead7c4c32a6af2734514ab8ba5d5ccbd3 (equals the I5 blob; diff hash equals PROD_DIFF; porcelain lists exactly the two production files)
- run_exit: exit=1
- cases: files=4 cases=45 pass=27 fail=18 error=0 skip=0 (file evidence/mutations/M-P1-P8-cases.txt)
- failed: 18
- passed: 27
- restored: 2026-11-21T04:56:50Z hash-ok porcelain-two-files-check=ok diff-hash-ok
- expected: matches (the 18 failing tests are exactly H1, H2, H3, H4, J1, J2, J3, J4, C1, C2 and the 8 refusal tests; every failure is an assertion, type A; J7, H5, J5, J6, the TLS 1.2 tests and the unchanged tests pass)
- verdict: DETECTED (M-P1 and M-P8; the M-P8 control: with the floor removed the old-protocol handshakes complete, so the refusal tests fail on `fail("... must be refused, but it completed at ...")`)

### M-P2 APPLYING 2026-11-21T04:57:13Z
- mutation: pinned `createSocket`, `DefaultTrustedSocketFactory.hardenSocket(sslSocket);` becomes `String[] m = sslSocket.getEnabledProtocols(); DefaultTrustedSocketFactory.hardenSocket(sslSocket); sslSocket.setEnabledProtocols(m);` (the pinned protocol handling removed; cipher and server-name handling left in)
- classes run: CreateSocket, JdkProvider
- expected failing: C1, C2 (assertProtocolFloor, A); the 4 pinned refusal tests (fail(...), A); J7 (enabled protocols in order, [TLSv1.3, TLSv1.2] against [TLSv1.2, TLSv1.3], A). Not failing: J3, J4 (sets equal on SunJSSE)
- file: app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
- hash-before: 21cc5f345977ab6fa161b2aa6925a04aea6a1479 (equals the I5 blob; diff hash equals PROD_DIFF; porcelain lists exactly the two production files)
- run_exit: exit=1
- cases: files=2 cases=19 pass=12 fail=7 error=0 skip=0 (file evidence/mutations/M-P2-cases.txt)
- failed: 7
- passed: 12
- restored: 2026-11-21T04:58:05Z hash-ok porcelain-two-files-check=ok diff-hash-ok
- expected: matches (7 failing: C1, C2, the 4 pinned refusal tests, J7; all type A; J3 and J4 pass because the sets are equal on SunJSSE)
- verdict: DETECTED (M-P2: the pinned protocol handling removed; the refusal tests complete the old-protocol handshake and fail)

### M-P3 APPLYING 2026-11-21T04:58:15Z
- mutation: pinned `createSocket`, after `DefaultTrustedSocketFactory.hardenSocket(sslSocket);` a block is added that rebuilds the protocol list from `getSupportedProtocols()` with only `SSLv3` removed and enables it (the widening)
- classes run: JdkProvider
- expected failing: J3 (AC-4 (d) protocols containsExactly, A); J4 ("pinned direct socket" containsExactly, A); J7 ("enabled protocols as a set", DNS host, A)
- file: app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
- hash-before: 21cc5f345977ab6fa161b2aa6925a04aea6a1479 (equals the I5 blob; diff hash equals PROD_DIFF; porcelain lists exactly the two production files)
- run_exit: exit=1
- cases: files=1 cases=9 pass=6 fail=3 error=0 skip=0 (file evidence/mutations/M-P3-cases.txt)
- failed: 3
- passed: 6
- restored: 2026-11-21T04:58:58Z hash-ok porcelain-two-files-check=ok diff-hash-ok
- expected: matches (3 failing: J3, J4, J7; all type A; no other test in the class fails)
- verdict: DETECTED (M-P3: the widened list is caught by the exact-set test J4, by J3 and by the parity test J7)

### M-P4 APPLYING 2026-11-21T04:59:05Z
- mutation: pinned `createSocket`, `DefaultTrustedSocketFactory.hardenSocket(sslSocket);` becomes `String[] m = sslSocket.getEnabledCipherSuites(); DefaultTrustedSocketFactory.hardenSocket(sslSocket); sslSocket.setEnabledCipherSuites(m);` (the cipher-suite handling removed on its own; protocol and server-name handling left in)
- classes run: CreateSocket, Hardening, JdkProvider
- expected failing: C1, C2 (enabled cipher suites in order against the default factory, A); H5 ("(ii) the known suites must appear in the literal order", A); J3 (AC-4 (d) cipher suites in order, A); J7 (enabled cipher suites in order, DNS host, A)
- file: app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
- hash-before: 21cc5f345977ab6fa161b2aa6925a04aea6a1479 (equals the I5 blob; diff hash equals PROD_DIFF; porcelain lists exactly the two production files)
- run_exit: exit=1
- cases: files=3 cases=31 pass=26 fail=5 error=0 skip=0 (file evidence/mutations/M-P4-cases.txt)
- failed: 5
- passed: 26
- restored: 2026-11-21T04:59:56Z hash-ok porcelain-two-files-check=ok diff-hash-ok
- expected: matches (5 failing: C1, C2, H5, J3, J7; all type A)
- verdict: DETECTED (M-P4: the cipher-suite handling removed on its own is caught by the parity tests C1, C2, J7, by the recorded-rule test H5 and by J3; BUG-020 AC-3 asked for this run separately)

### M-P5 APPLYING 2026-11-21T05:00:05Z
- mutation: pinned `createSocket`, `DefaultTrustedSocketFactory.setSniHost(socketFactory, sslSocket, host);` becomes `if (false) DefaultTrustedSocketFactory.setSniHost(socketFactory, sslSocket, host);` (the server-name step removed on its own)
- classes run: JdkProvider
- expected failing: J3 ("server names on the pinned implicit socket" isNotNull, A); J7 ("server names, direct socket for host imap.right.test", A). Not failing: J5, J6 (no step, no name, no exception)
- file: app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
- hash-before: 21cc5f345977ab6fa161b2aa6925a04aea6a1479 (equals the I5 blob; diff hash equals PROD_DIFF; porcelain lists exactly the two production files)
- run_exit: exit=1
- cases: files=1 cases=9 pass=7 fail=2 error=0 skip=0 (file evidence/mutations/M-P5-cases.txt)
- failed: 2
- passed: 7
- restored: 2026-11-21T05:01:01Z hash-ok porcelain-two-files-check=ok diff-hash-ok
- expected: matches (2 failing: J3, J7; both type A; J5 and J6 pass)
- verdict: DETECTED (M-P5: the pinned server-name step removed on its own is caught by the changed AC-4 (a) test J3 and by the parity test J7)

### M-P6-i APPLYING 2026-11-21T05:01:11Z
- mutation (M-P6 (i), cipher order drifts in the pinned factory only): pinned `createSocket`, after `DefaultTrustedSocketFactory.hardenSocket(sslSocket);` a block is added that swaps `TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384` and `TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384` in the enabled cipher list and sets it
- classes run: CreateSocket, Hardening, JdkProvider
- expected failing: C1, C2 (cipher suites in order, A); H5 ((ii) literal order, A); J3 (cipher suites in order, A); J7 (cipher suites in order, A)
- file: app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
- hash-before: 21cc5f345977ab6fa161b2aa6925a04aea6a1479 (equals the I5 blob; diff hash equals PROD_DIFF; porcelain lists exactly the two production files)
- run_exit: exit=1
- cases: files=3 cases=31 pass=26 fail=5 error=0 skip=0 (file evidence/mutations/M-P6-i-cases.txt)
- failed: 5
- passed: 26
- restored: 2026-11-21T05:02:12Z hash-ok porcelain-two-files-check=ok diff-hash-ok
- expected: matches (5 failing: C1, C2, H5, J3, J7; all type A)
- verdict: DETECTED (M-P6 (i): a one-entry cipher-order swap in the pinned factory only fails the parity tests and the literal-order test)

### M-P6-ii APPLYING 2026-11-21T05:02:19Z
- mutation (M-P6 (ii), the floor removed from the default factory only): in the default factory's `createSocket`, `hardenSocket(sslSocket);` becomes `hardenSocket(sslSocket); sslSocket.setEnabledProtocols(reorder(sslSocket.getSupportedProtocols(), ORDERED_KNOWN_PROTOCOLS, BLACKLISTED_PROTOCOLS));`
- classes run: CreateSocket, Hardening, JdkProvider, Handshake
- expected failing: C1, C2 ("enabled protocols as a set" in assertSameHandling, A); H1 (set equality, A); H2 (TLSv1 must not be enabled, direct, A); J1 (set equality, A); J2 (TLSv1 must not be enabled, direct, A); J7 (enabled protocols as a set, DNS host, A); the 4 Handshake refusal tests (fail(...), A). Not failing: H3, H4 (they call the step directly), the layered-equals-implicit test, J3, J4
- file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
- hash-before: 43e5a78ead7c4c32a6af2734514ab8ba5d5ccbd3 (equals the I5 blob; diff hash equals PROD_DIFF; porcelain lists exactly the two production files)
- run_exit: exit=1
- cases: files=4 cases=45 pass=34 fail=11 error=0 skip=0 (file evidence/mutations/M-P6-ii-cases.txt)
- failed: 11
- passed: 34
- restored: 2026-11-21T05:03:24Z hash-ok porcelain-two-files-check=ok diff-hash-ok
- expected: matches (11 failing: C1, C2, H1, H2, J1, J2, J7 and the 4 Handshake refusal tests; failures are type A; H3, H4, J3, J4 and the layered-equals-implicit test pass)
- verdict: DETECTED (M-P6 (ii): the floor removed from the default factory only; the pinned floor itself still holds, C1 and C2 fail on the parity assertion "enabled protocols as a set")

### M-P6-iii APPLYING 2026-11-21T05:03:34Z
- mutation (M-P6 (iii), the server-name step removed from the default factory only): in the default factory's `createSocket`, `setSniHost(socketFactory, sslSocket, host);` becomes `if (false) setSniHost(socketFactory, sslSocket, host);`
- classes run: JdkProvider
- expected failing: `createSocket_dnsHost_setsServerNameOnRealSocket` (`assertThat(names).isNotNull()`, A); J7 (server names, DNS host, A)
- file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
- hash-before: 43e5a78ead7c4c32a6af2734514ab8ba5d5ccbd3 (equals the I5 blob; diff hash equals PROD_DIFF; porcelain lists exactly the two production files)
- run_exit: exit=1
- cases: files=1 cases=9 pass=7 fail=2 error=0 skip=0 (file evidence/mutations/M-P6-iii-cases.txt)
- failed: 2
- passed: 7
- restored: 2026-11-21T05:04:34Z hash-ok porcelain-two-files-check=ok diff-hash-ok
- expected: matches (2 failing: `createSocket_dnsHost_setsServerNameOnRealSocket`, J7; both type A)
- verdict: DETECTED (M-P6 (iii): the server-name step removed from the default factory only is caught by the default factory's own control test and by the parity test)

### M-P7 APPLYING 2026-11-21T05:04:43Z
- mutation (M-P7, the handling applied to the direct shape only): in the pinned `createSocket`, the two lines `DefaultTrustedSocketFactory.hardenSocket(sslSocket);` and `DefaultTrustedSocketFactory.setSniHost(socketFactory, sslSocket, host);` are each prefixed with `if (socket == null) `
- classes run: CreateSocket, JdkProvider
- expected failing: C2 (assertProtocolFloor, "TLSv1 must not be enabled", layered, A); the 2 pinned STARTTLS refusal tests (fail(...), A); J7 ("layered socket for host imap.right.test: enabled protocols in order", [TLSv1.3, TLSv1.2] against [TLSv1.2, TLSv1.3], A; the set check passes). Not failing: J4 (sets), J5, J6, C1, the pinned STARTTLS TLS 1.2 test
- file: app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
- hash-before: 21cc5f345977ab6fa161b2aa6925a04aea6a1479 (equals the I5 blob; diff hash equals PROD_DIFF; porcelain lists exactly the two production files)
- run_exit: exit=1
- cases: files=2 cases=19 pass=15 fail=4 error=0 skip=0 (file evidence/mutations/M-P7-cases.txt)
- failed: 4
- passed: 15
- restored: 2026-11-21T05:05:35Z hash-ok porcelain-two-files-check=ok diff-hash-ok
- expected: matches (4 failing: C2, the 2 pinned STARTTLS refusal tests, J7; all type A; J4, J5, J6, C1 and the pinned STARTTLS TLS 1.2 test pass)
- verdict: DETECTED (M-P7: the handling applied to the direct shape only is caught on the layered shape by C2, the STARTTLS refusal tests and, by order, J7; J4 compares sets and does not detect it, as stated in the plan)

### M-F1 APPLYING 2026-11-21T05:05:44Z
- mutation (M-F1, the fallback of the null-list branch removed; the behaviour before U-065: nothing applied): in the three-argument `hardenSocket`, `sock.setEnabledProtocols(protocols != null ? protocols : protocolsToEnable(sock));` becomes `if (protocols != null) sock.setEnabledProtocols(protocols);`
- classes run: Hardening
- expected failing: H4 (`verify(socket).setEnabledProtocols(...)`, A, `org.mockito.exceptions.verification.WantedButNotInvoked`)
- file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
- hash-before: 43e5a78ead7c4c32a6af2734514ab8ba5d5ccbd3 (equals the I5 blob; diff hash equals PROD_DIFF; porcelain lists exactly the two production files)
- run_exit: exit=1
- cases: files=1 cases=12 pass=11 fail=1 error=0 skip=0 (file evidence/mutations/M-F1-cases.txt)
- failed: 1
- passed: 11
- restored: 2026-11-21T05:06:28Z hash-ok porcelain-two-files-check=ok diff-hash-ok
- expected: matches (1 failing: H4, `org.mockito.exceptions.verification.WantedButNotInvoked`, an AssertionError subclass, type A)
- verdict: DETECTED (M-F1: the fallback of the null-list branch is covered by H4; AC-1 (e))

### M-D11r APPLYING 2026-11-21T05:06:36Z
- mutation (U-063's M-D11 on the new code: the protocol list built from the enabled instead of the supported protocols): in `protocolsToEnable`, `reorder(socket.getSupportedProtocols(), ORDERED_KNOWN_PROTOCOLS,` becomes `reorder(socket.getEnabledProtocols(), ORDERED_KNOWN_PROTOCOLS,`
- classes run: Hardening, JdkProvider
- expected failing: H3 only (case (iii), result [TLSv1.3], A). The log records that no socket-level test detects it: both test providers give the same list from the enabled and the supported protocols (plan section 1.2)
- file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
- hash-before: 43e5a78ead7c4c32a6af2734514ab8ba5d5ccbd3 (equals the I5 blob; diff hash equals PROD_DIFF; porcelain lists exactly the two production files)
- run_exit: exit=1
- cases: files=2 cases=21 pass=20 fail=1 error=0 skip=0 (file evidence/mutations/M-D11r-cases.txt)
- failed: 1
- passed: 20
- restored: 2026-11-21T05:07:18Z hash-ok porcelain-two-files-check=ok diff-hash-ok
- expected: matches (1 failing: H3, type A; no socket-level test in Hardening or JdkProvider fails)
- verdict: DETECTED by H3 only, as predicted. No socket-level test detects it: both test providers give the same list from the enabled and the supported protocols (AC-7 last bullet; AC-1 (d))

### M-S5 APPLYING 2026-11-21T05:07:39Z
- mutation (U-064's M-N2 edit): in `setSniViaSSLParameters`, the `try { ... } catch (IllegalArgumentException e) { ... }` block (9 lines) is replaced by its two body lines `params.setServerNames(Collections.singletonList(new SNIHostName(hostname)));` and `socket.setSSLParameters(params);` (the safety net removed)
- classes run: JdkProvider, Sni
- expected failing: J6 ("the pinned createSocket must not throw ...; escaped" lists the names, A); J7 (`pinnedSocket("bad_host.example")` throws, ERROR, `java.lang.IllegalArgumentException`); SNI `setSniViaSSLParameters_nameRejectedBySniHostName_doesNotThrowAndSkipsSNI` (escaped list, A); SNI `createSocket_nameRejectedBySniHostName_doesNotThrow` (ERROR, `java.lang.IllegalArgumentException`). Not failing: J5, J3, the other 12 SNI tests
- file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
- hash-before: 43e5a78ead7c4c32a6af2734514ab8ba5d5ccbd3 (equals the I5 blob; diff hash equals PROD_DIFF; porcelain lists exactly the two production files)
- run_exit: exit=1
- cases: files=2 cases=23 pass=19 fail=4 error=0 skip=0 (file evidence/mutations/M-S5-cases.txt)
- failed: 4
- passed: 19
- restored: 2026-11-21T05:08:31Z hash-ok porcelain-two-files-check=ok diff-hash-ok
- expected: matches (4 failing: J6 type A; J7 ERROR `java.lang.IllegalArgumentException`; SNI `setSniViaSSLParameters_nameRejectedBySniHostName_doesNotThrowAndSkipsSNI` type A; SNI `createSocket_nameRejectedBySniHostName_doesNotThrow` ERROR `java.lang.IllegalArgumentException`; J3, J5 and the other 12 SNI tests pass)
- verdict: DETECTED (M-S5: the safety net removed is caught on the pinned path by J6 and J7, and by U-064's two SNI tests)

### M-E4 APPLYING 2026-11-21T05:08:45Z
- mutation (U-063's M-E4, re-run because the pinned `createSocket` was edited): in the pinning trust manager, `if (!Arrays.equals(presentedFingerprint, enrolledFingerprint)) {` becomes `if (Arrays.equals(presentedFingerprint, enrolledFingerprint)) {` (fingerprint comparison inverted)
- classes run: CreateSocket, Expiry
- expected failing: `handshake_enrolledCertificateForDifferentHostName_isAccepted` (ERROR, `javax.net.ssl.SSLHandshakeException` or another SSLException subclass); `handshake_certificateOtherThanEnrolled_isRejected` (`fail("the handshake must be rejected, but it completed")`, A); the 2 pinned TLS 1.2 tests (ERROR, SSLException subclass); the Expiry test (direct cause isInstanceOf CertificateExpiredException, A). Not failing: C1, C2, the refusal tests (they stop at the protocol, before any certificate)
- file: app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
- hash-before: 21cc5f345977ab6fa161b2aa6925a04aea6a1479 (equals the I5 blob; diff hash equals PROD_DIFF; porcelain lists exactly the two production files)
- run_exit: exit=1
- cases: files=2 cases=11 pass=6 fail=5 error=0 skip=0 (file evidence/mutations/M-E4-cases.txt)
- failed: 5
- passed: 6
- restored: 2026-11-21T05:09:41Z hash-ok porcelain-two-files-check=ok diff-hash-ok
- expected: matches (5 failing: `handshake_certificateOtherThanEnrolled_isRejected` type A (`java.lang.AssertionError`); `handshake_enrolledCertificateForDifferentHostName_isAccepted`, `handshake_implicitTls_serverLimitedToTls12_completesAtTls12` and `handshake_startTls_serverLimitedToTls12_completesAtTls12` ERROR `javax.net.ssl.SSLHandshakeException`; the Expiry test `handshake_expiredEnrolledCertificate_isRejectedAsExpired` type A; C1, C2 and the 4 refusal tests pass)
- verdict: DETECTED (M-E4 re-run: `handshake_certificateOtherThanEnrolled_isRejected` fails, as BUG-020 AC-6 and AC-7 require)

### M-E5 APPLYING 2026-11-21T05:09:51Z
- mutation (U-063's M-E5, re-run because the pinned `createSocket` was edited): `TrustManager[] trustManagers = new TrustManager[]{ new PinnedX509TrustManager() };` becomes the same statement with an accept-all `javax.net.ssl.X509TrustManager` (empty `checkClientTrusted` and `checkServerTrusted`, empty `getAcceptedIssuers`)
- classes run: CreateSocket, Expiry
- expected failing: `handshake_certificateOtherThanEnrolled_isRejected` (`fail(...)`, A); the Expiry test (`fail("the handshake must be rejected, but it completed")`, A)
- file: app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
- hash-before: 21cc5f345977ab6fa161b2aa6925a04aea6a1479 (equals the I5 blob; diff hash equals PROD_DIFF; porcelain lists exactly the two production files)
- run_exit: exit=1
- cases: files=2 cases=11 pass=9 fail=2 error=0 skip=0 (file evidence/mutations/M-E5-cases.txt)
- failed: 2
- passed: 9
- restored: 2026-11-21T05:10:57Z hash-ok porcelain-two-files-check=ok diff-hash-ok
- expected: matches (2 failing: `handshake_certificateOtherThanEnrolled_isRejected` and the Expiry test `handshake_expiredEnrolledCertificate_isRejectedAsExpired`, both `java.lang.AssertionError`, type A; the other 9 pass)
- verdict: DETECTED (M-E5 re-run: an accept-all trust manager makes `handshake_certificateOtherThanEnrolled_isRejected` fail, as BUG-020 AC-6 and AC-7 require)
