# Mutation log (U-063, IC-7)

Protocol: plan section "I6 - Mutation evidence" (write-ahead entry, one edit with the Edit tool, one targeted run, restore with a single-file checkout, hash and porcelain check, `restored` line). Paths are repository-relative. Base commit `395000956bb46cb2acc6d66e62d4b5ef933a2d90`.

DTSF = `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`, base blob `d543927166b3c5e1da0aaa8ab584072a43bf7521`.

Recovery note (I2b, A-W4): if a session is lost while an entry below says `APPLYING` and has no `restored` line, nothing else may run (no gate, no review, no commit, no other increment) until a recovery turn has compared the hash of DTSF with the blob above and, if it differs, run the single-file checkout of DTSF.

## Summary (I6 close-out, written 2026-11-20)

39 entries: the I2b pre-check, the 35 planned mutation ids, and 3 additional entries (M-B1r, M-C1r, M-E2-jdk). Verdict totals over the 39 entries: DETECTED 34 (30 planned ids, the pre-check, M-B1r, M-C1r, M-E2-jdk), BY-DESIGN 2 (M-A1g, M-A1c), COMPILE_FAIL 2 (M-B1, M-C1, invalid, not counted, replaced by M-B1r and M-C1r), NOT DETECTED 1 (M-E2, 2026-11-17, default provider; superseded by the move of the AC-19 test and the re-run M-E2-jdk, which is DETECTED). NOTED-AC16: M-D11 only (Hardening second AC-16 test passed, as D7c predicted; both JDK-provider AC-16 tests failed). Each entry below has its `restored` line.

| ID | ACs (tests that failed) | Failing tests / of | Verdict |
|----|-------------------------|--------------------|---------|
| M-A4-precheck | AC-4 | 1 of 4 (JDK-provider class) | DETECTED |
| M-A1 | AC-1, AC-3 (also 5 existing IP/blank tests, not counted) | 7 of 16 | DETECTED |
| M-A1g | none of AC-1/AC-3 (existing IPv4 test only) | 1 of 16 | BY-DESIGN |
| M-A1c | none | 0 of 16 | BY-DESIGN |
| M-A2a | AC-2 | 1 of 16 | DETECTED |
| M-A2b | AC-2 | 1 of 16 | DETECTED |
| M-A3 | AC-3, AC-4 | 2 of 16 | DETECTED |
| M-A4 | AC-4 | 1 of 16 | DETECTED |
| M-D1 | AC-11, AC-14 | 2 of 13 | DETECTED |
| M-D2 | AC-11, AC-14, AC-16 (first JDK test) | 3 of 13 | DETECTED |
| M-D3 | AC-11 (and the first JDK AC-16 test) | 2 of 13 | DETECTED |
| M-D4 | AC-12, AC-14 (and AC-15) | 3 of 13 | DETECTED |
| M-D5 | AC-12, AC-14, AC-16 | 4 of 13 | DETECTED |
| M-D6a | AC-12 (and AC-15, AC-16) | 4 of 13 | DETECTED |
| M-D6b | AC-12, AC-15, AC-16 (and AC-11, AC-13) | 6 of 13 | DETECTED |
| M-D7 | AC-13 | 1 of 13 | DETECTED |
| M-D15 | AC-13 | 1 of 13 | DETECTED |
| M-D8 | AC-15, AC-16 | 4 of 13 | DETECTED |
| M-D9 | AC-15 | 1 of 13 | DETECTED |
| M-D10 | AC-17 | 1 of 13 | DETECTED |
| M-D11 | AC-16 (both JDK tests; Hardening second test passed) | 2 of 13 | DETECTED, NOTED-AC16 |
| M-D12 | AC-14, AC-16 (and AC-12) | 6 of 13 | DETECTED |
| M-D13 | AC-16 | 3 of 13 | DETECTED |
| M-D14 | AC-14 (and AC-15) | 2 of 13 | DETECTED |
| M-B1 | did not compile | 0 of 0 | COMPILE_FAIL (invalid, not counted) |
| M-B1r | AC-6, AC-8 | 2 of 8 | DETECTED (replacement for M-B1) |
| M-B2 | AC-6, AC-8, AC-9, AC-10 | 6 of 8 | DETECTED |
| M-B3 | AC-5, AC-7 | 2 of 8 | DETECTED |
| M-B4 | AC-5, AC-7 (and AC-6, AC-8) | 4 of 8 | DETECTED |
| M-B5 | AC-7, AC-8, AC-9b, AC-10b | 4 of 8 | DETECTED |
| M-C1 | did not compile | 0 of 0 | COMPILE_FAIL (invalid, not counted) |
| M-C1r | AC-9, AC-10 | 4 of 8 | DETECTED (replacement for M-C1) |
| M-C2 | AC-6, AC-8, AC-9, AC-10 | 6 of 8 | DETECTED |
| M-E1 | AC-18 (both) | 2 of 5 (pre-move pinned class) | DETECTED |
| M-E2 | AC-19 (test in the pinned class did not fail) | 0 of 5 (pre-move pinned class) | NOT DETECTED (2026-11-17) |
| M-E2-jdk | AC-19 (moved test, JDK-provider class) | 1 of 9 | DETECTED (re-run after the 2026-11-20 decision) |
| M-E3 | AC-20 | 1 of 4 | DETECTED |
| M-E4 | AC-20, AC-21 | 2 of 4 | DETECTED |
| M-E5 | AC-21 | 1 of 4 | DETECTED |

Per acceptance criterion (the mutations the plan names for it; "demonstrated" only if every one of them is DETECTED, the pair of AC-1 and NOTED-AC16 aside):

| AC | Mutations | Demonstrated |
|----|-----------|--------------|
| AC-1 | M-A1 (DETECTED); the pair M-A1g and M-A1c (BY-DESIGN, as AC-1 itself says) | yes |
| AC-2 | M-A2a, M-A2b | yes |
| AC-3 | M-A1, M-A3 | yes |
| AC-4 | M-A3, M-A4, M-A4-precheck | yes |
| AC-5 | M-B3, M-B4 | yes |
| AC-6 | M-B1r (M-B1 invalid), M-B2, M-C2 | yes |
| AC-7 | M-B3, M-B4, M-B5 | yes |
| AC-8 | M-B1r (M-B1 invalid), M-B2, M-B5, M-C2 | yes |
| AC-9 | M-C1r (M-C1 invalid), M-B2, M-C2, M-B5 (9b) | yes |
| AC-10 | M-C1r (M-C1 invalid), M-B2, M-C2, M-B5 (10b) | yes |
| AC-11 | M-D1, M-D2, M-D3 | yes |
| AC-12 | M-D4, M-D5, M-D6a, M-D6b | yes |
| AC-13 | M-D7, M-D15 | yes |
| AC-14 | M-D1, M-D2, M-D4, M-D5, M-D12, M-D14 | yes |
| AC-15 | M-D8, M-D9, M-D14 | yes |
| AC-16 | M-D2, M-D5, M-D8, M-D11 (NOTED-AC16 for the default-provider second test only), M-D12, M-D13 | yes (with NOTED-AC16) |
| AC-17 | M-D10 | yes |
| AC-18 | M-E1 | yes |
| AC-19 | M-E2 (NOT DETECTED under the default provider, test since moved); M-E2-jdk (DETECTED) | yes, by the moved test only; the 2026-11-17 entry stays as recorded |
| AC-20 | M-E3, M-E4 | yes |
| AC-21 | M-E4, M-E5 | yes |

## Pre-check (I2b, A1.d step 2). Not one of the 35 mutations.

### M-A4-precheck APPLYING 2026-11-17T21:59:40Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: delete line 177, `        setSniHost(socketFactory, sslSocket, host);` (M-A4), leaving `hardenSocket(sslSocket);` and `return trustedSocket;` unchanged
class run: com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryJdkProviderTest only
expected: AC-4 (`createSocket_dnsHost_setsServerNameOnRealSocket`) FAILS; AC-3 PASSES; both AC-16 JDK tests PASS
run_exit: 1 (BUILD FAILED, as expected for a detected mutation); cases: `evidence/mutations/M-A4-precheck-cases.txt`
failed: `createSocket_dnsHost_setsServerNameOnRealSocket` (AC-4), failure type `com.google.common.truth.AssertionErrorWithFacts`
passed: `createSocket_ipv6EightGroupLiteralHost_leavesServerNamesUnset` (AC-3), `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder_jdkProvider`, `createSocket_legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff`
expected: AC-4 FAILS, AC-3 PASSES: matches
verdict: DETECTED (branch "observable works", A1.d step 3)
restored: 2026-11-17T22:00:42Z hash-ok porcelain-empty (hash d543927166b3c5e1da0aaa8ab584072a43bf7521 equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

## I6a (Turn 8)

Baseline at I6a start (unmutated, Sni + JDK-provider + Hardening classes in one invocation): `evidence/mutations/I6a-baseline-start-cases.txt`: `files=3 cases=25 pass=25 fail=0 error=0 skip=0`.

### M-A1 APPLYING 2026-11-17T22:35:17Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: delete the guard (lines 239-243, `if (isIpOrBlankHostname(hostname)) {` ... `}`) and replace the try/catch (lines 245-253) with its two body lines `params.setServerNames(Collections.singletonList(new SNIHostName(hostname)));` and `socket.setSSLParameters(params);` (line 244 `SSLParameters params = socket.getSSLParameters();` kept)
class run: DefaultTrustedSocketFactorySniTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-1 (SNI test `setSniViaSSLParameters_ipv6EightGroupLiteral_doesNotThrowAndSkipsSNI`) FAILS; AC-3 (JDK-provider `createSocket_ipv6EightGroupLiteralHost_leavesServerNamesUnset`, the IllegalArgumentException escapes createSocket) FAILS; existing IP/blank tests also fail (recorded, not counted); AC-4 and both AC-16 JDK tests PASS
run_exit: 1 (BUILD FAILED, expected for a detected mutation); cases: `evidence/mutations/M-A1-cases.txt` (`cases=16 pass=9 fail=7`)
failed: Sni `setSniViaSSLParameters_ipv6EightGroupLiteral_doesNotThrowAndSkipsSNI` (AC-1, java.lang.IllegalArgumentException); JDK-provider `createSocket_ipv6EightGroupLiteralHost_leavesServerNamesUnset` (AC-3, java.lang.IllegalArgumentException); existing tests (recorded, not counted): `setSniViaSSLParameters_blankHostname_doesNotThrowAndSkipsSNI`, `..._emptyHostname_...`, `..._ipv6FullLiteral_...`, `..._ipv6Literal_...` (all IllegalArgumentException), `..._ipv4Literal_...` (NeverWantedButInvoked)
passed: Sni `isIpOrBlankHostname_ipv6EightGroupLiteral_isTrue` (AC-2), `setSniViaSSLParameters_dnsHostname_setsSNI`, `..._setsCorrectHostname_forDifferentHost`, `..._setsSSLParametersWithCorrectSNIHostName`, both `reflectiveFallback_*`; JDK-provider `createSocket_dnsHost_setsServerNameOnRealSocket` (AC-4), both AC-16 JDK tests
expected: AC-1 and AC-3 FAIL: matches; nothing unexpected failed
verdict: DETECTED
restored: 2026-11-17T22:36:10Z hash-ok porcelain-empty (hash d543927166b3c5e1da0aaa8ab584072a43bf7521 equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-A1g APPLYING 2026-11-17T22:36:21Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: delete the guard only (lines 239-243, `if (isIpOrBlankHostname(hostname)) {` ... `}` including the log call and `return;`); the try/catch stays
class run: DefaultTrustedSocketFactorySniTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: BY-DESIGN: AC-1 and AC-3 expected to PASS (the catch rescues the IllegalArgumentException for the eight-group literal); record what fails (IPv4 and blank cases are expected to fail as the guard is gone)
run_exit: 1; cases: `evidence/mutations/M-A1g-cases.txt` (`files=2 cases=16 pass=15 fail=1 error=0 skip=0`)
failed: Sni `setSniViaSSLParameters_ipv4Literal_doesNotThrowAndSkipsSNI` (NeverWantedButInvoked; existing test, recorded, not counted)
passed: every other test of both classes (15), including AC-1 (`..._ipv6EightGroupLiteral_...`), AC-2, AC-3, AC-4, both AC-16 JDK tests and the existing blank and empty tests (the catch rescues them)
expected: BY-DESIGN: AC-1 and AC-3 expected to PASS: they passed (the catch layer alone is enough for the eight-group literal); the only failure is the existing IPv4 test, as the guard is what stops an IPv4 literal reaching SNIHostName
verdict: BY-DESIGN
restored: 2026-11-17T22:37:12Z hash-ok porcelain-empty (hash d543927166b3c5e1da0aaa8ab584072a43bf7521 equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-A1c APPLYING 2026-11-17T22:37:17Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: replace the try/catch (lines 245-253) with its two body lines `params.setServerNames(Collections.singletonList(new SNIHostName(hostname)));` and `socket.setSSLParameters(params);`; the guard stays
class run: DefaultTrustedSocketFactorySniTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: BY-DESIGN: AC-1 and AC-3 expected to PASS (the guard stops the eight-group literal before SNIHostName); record what fails
run_exit: 0; cases: `evidence/mutations/M-A1c-cases.txt` (`files=2 cases=16 pass=16 fail=0 error=0 skip=0`)
failed: none (BUILD SUCCESSFUL, all 16 pass)
passed: all 16 tests of both classes, including AC-1, AC-2, AC-3, AC-4 and both AC-16 JDK tests
expected: BY-DESIGN: AC-1 and AC-3 expected to PASS (the guard alone stops the eight-group literal): they passed; nothing failed
verdict: BY-DESIGN
restored: 2026-11-17T22:38:06Z hash-ok porcelain-empty (hash d543927166b3c5e1da0aaa8ab584072a43bf7521 equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-A2a APPLYING 2026-11-17T22:38:07Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: delete lines 221-223 (the IPv6 branch of isIpOrBlankHostname: `if (hostname.indexOf(':') >= 0) {`, `return true;`, `}`)
class run: DefaultTrustedSocketFactorySniTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-2 (Sni `isIpOrBlankHostname_ipv6EightGroupLiteral_isTrue`) FAILS; AC-1 and AC-3 expected to pass (the catch rescues); existing IPv6 tests may pass for the same reason
run_exit: 1; cases: `evidence/mutations/M-A2a-cases.txt` (`files=2 cases=16 pass=15 fail=1 error=0 skip=0`)
failed: Sni `isIpOrBlankHostname_ipv6EightGroupLiteral_isTrue` (AC-2, AssertionErrorWithFacts)
passed: the other 15 tests of both classes, including AC-1, AC-3, AC-4 and both AC-16 JDK tests (the catch layer rescues the IPv6 literals)
expected: AC-2 FAILS: matches; nothing unexpected failed
verdict: DETECTED
restored: 2026-11-17T22:38:59Z hash-ok porcelain-empty (hash d543927166b3c5e1da0aaa8ab584072a43bf7521 equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-A2b APPLYING 2026-11-17T22:39:00Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: line 221 `if (hostname.indexOf(':') >= 0) {` becomes `if (hostname.contains("::")) {`
class run: DefaultTrustedSocketFactorySniTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-2 (Sni `isIpOrBlankHostname_ipv6EightGroupLiteral_isTrue`) FAILS (the eight-group literal has no double colon); other tests expected to pass
run_exit: 1; cases: `evidence/mutations/M-A2b-cases.txt` (`files=2 cases=16 pass=15 fail=1 error=0 skip=0`)
failed: Sni `isIpOrBlankHostname_ipv6EightGroupLiteral_isTrue` (AC-2, AssertionErrorWithFacts)
passed: the other 15 tests of both classes, including AC-1, AC-3, AC-4 and both AC-16 JDK tests
expected: AC-2 FAILS: matches; nothing unexpected failed
verdict: DETECTED
restored: 2026-11-17T22:39:51Z hash-ok porcelain-empty (hash d543927166b3c5e1da0aaa8ab584072a43bf7521 equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-A3 APPLYING 2026-11-17T22:39:52Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: line 197 `setSniViaSSLParameters(socket, hostname);` becomes `setSniViaSSLParameters(socket, "mutant.invalid.test");`
class run: DefaultTrustedSocketFactorySniTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-3 (JDK-provider `createSocket_ipv6EightGroupLiteralHost_leavesServerNamesUnset`) FAILS (a server name is set for the mutant host) and AC-4 (JDK-provider `createSocket_dnsHost_setsServerNameOnRealSocket`) FAILS (name is mutant.invalid.test, not the host); both AC-16 JDK tests PASS; the Sni tests call setSniViaSSLParameters directly and are not expected to move
run_exit: 1; cases: `evidence/mutations/M-A3-cases.txt` (`files=2 cases=16 pass=14 fail=2 error=0 skip=0`)
failed: JDK-provider `createSocket_ipv6EightGroupLiteralHost_leavesServerNamesUnset` (AC-3, AssertionErrorWithFacts) and `createSocket_dnsHost_setsServerNameOnRealSocket` (AC-4, ComparisonFailureWithFacts)
passed: all 12 Sni tests and both AC-16 JDK tests
expected: AC-3 and AC-4 FAIL, both AC-16 JDK tests pass: matches; nothing unexpected failed
verdict: DETECTED
restored: 2026-11-17T22:40:39Z hash-ok porcelain-empty (hash d543927166b3c5e1da0aaa8ab584072a43bf7521 equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-A4 APPLYING 2026-11-17T22:40:39Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: delete line 177 `        setSniHost(socketFactory, sslSocket, host);`, leaving `hardenSocket(sslSocket);` and `return trustedSocket;` unchanged
class run: DefaultTrustedSocketFactorySniTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-4 (JDK-provider `createSocket_dnsHost_setsServerNameOnRealSocket`) FAILS; AC-3 and both AC-16 JDK tests PASS; Sni tests unaffected
run_exit: 1; cases: `evidence/mutations/M-A4-cases.txt` (`files=2 cases=16 pass=15 fail=1 error=0 skip=0`)
failed: JDK-provider `createSocket_dnsHost_setsServerNameOnRealSocket` (AC-4, AssertionErrorWithFacts)
passed: all 12 Sni tests, AC-3 (`createSocket_ipv6EightGroupLiteralHost_leavesServerNamesUnset`) and both AC-16 JDK tests
expected: AC-4 FAILS, AC-3 and both AC-16 JDK tests pass: matches; nothing unexpected failed
verdict: DETECTED
restored: 2026-11-17T22:41:25Z hash-ok porcelain-empty (hash d543927166b3c5e1da0aaa8ab584072a43bf7521 equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-D1 APPLYING 2026-11-17T22:41:26Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: delete line 79 `            "SSL_RSA_WITH_RC4_128_SHA",` (one name of BLACKLISTED_CIPHERS)
class run: DefaultTrustedSocketFactoryHardeningTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-11 (`reorder_removesEveryBlockedItem`) and AC-14 (`cipherConstants_matchRecordedBaseline`) FAIL; the JDK-provider tests are not expected to fail
run_exit: 1; cases: `evidence/mutations/M-D1-cases.txt` (`files=2 cases=13 pass=11 fail=2 error=0 skip=0`)
failed: Hardening `reorder_removesEveryBlockedItem` (AC-11, AssertionErrorWithFacts) and `cipherConstants_matchRecordedBaseline` (AC-14, AssertionErrorWithFacts)
passed: the other 7 Hardening tests and all 4 JDK-provider tests
expected: AC-11 and AC-14 FAIL: matches; nothing unexpected failed
verdict: DETECTED
restored: 2026-11-17T22:42:10Z hash-ok porcelain-empty (hash d543927166b3c5e1da0aaa8ab584072a43bf7521 equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-D2 APPLYING 2026-11-17T22:42:11Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: delete line 88 `            "SSLv3"` (BLACKLISTED_PROTOCOLS becomes an empty array)
class run: DefaultTrustedSocketFactoryHardeningTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-11 (`reorder_removesEveryBlockedItem`) and AC-14 (`protocolConstants_matchRecordedBaseline`) FAIL (Hardening); AC-16 first JDK test (`createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder_jdkProvider`) FAILS (SSLv3 enabled under SunJSSE); AC-16 first Hardening test only if the default provider supports SSLv3 (probe: sslv3_supported=false, so not expected to fail)
run_exit: 1; cases: `evidence/mutations/M-D2-cases.txt` (`files=2 cases=13 pass=10 fail=3 error=0 skip=0`)
failed: Hardening `protocolConstants_matchRecordedBaseline` (AC-14), `reorder_removesEveryBlockedItem` (AC-11); JDK-provider `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder_jdkProvider` (AC-16 first JDK test); all AssertionErrorWithFacts
passed: the other 7 Hardening tests (including the Hardening AC-16 tests, as SSLv3 is not supported by the default provider) and the other 3 JDK-provider tests
expected: AC-11, AC-14 and AC-16 first JDK test FAIL: matches; the Hardening AC-16 tests did not fail, as expected (sslv3_supported=false); nothing unexpected failed
verdict: DETECTED
restored: 2026-11-17T22:42:57Z hash-ok porcelain-empty (hash d543927166b3c5e1da0aaa8ab584072a43bf7521 equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-D3 APPLYING 2026-11-17T22:42:58Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: delete lines 129-133 (the blocked-item removal: `if (blacklisted != null) {`, the inner `for` loop with `unknown.remove(item);`, and both closing braces)
class run: DefaultTrustedSocketFactoryHardeningTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-11 (`reorder_removesEveryBlockedItem`) FAILS; others not expected to fail except as a consequence (the provider lists contain no blocked names; probe blocked_in_default is empty); the JDK-provider class is not expected to fail
run_exit: 1; cases: `evidence/mutations/M-D3-cases.txt` (`files=2 cases=13 pass=11 fail=2 error=0 skip=0`)
failed: Hardening `reorder_removesEveryBlockedItem` (AC-11, AssertionErrorWithFacts). Not named by the plan: JDK-provider `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder_jdkProvider` (AssertionErrorWithFacts) also failed
passed: the other 8 Hardening tests and the other 3 JDK-provider tests (AC-3, AC-4, AC-16 second JDK test)
expected: AC-11 FAILS: matches. One extra failure, explained: the deleted block removes the blocked items for protocols as well as for ciphers, so SSLv3 (supported by SunJSSE) is no longer removed and the AC-16 first JDK test, which asserts SSLv3 is not enabled, fails (same mechanism as M-D2). This is a further detection, not a contradiction of the plan; the Hardening AC-13/AC-14/AC-15/AC-16 tests did not move
verdict: DETECTED
restored: 2026-11-17T22:43:47Z hash-ok porcelain-empty (hash d543927166b3c5e1da0aaa8ab584072a43bf7521 equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-D4 APPLYING 2026-11-17T22:43:48Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: swap lines 38 and 39 (`"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",` and `"TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",` of ORDERED_KNOWN_CIPHERS)
class run: DefaultTrustedSocketFactoryHardeningTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-12 (`reorder_ordersKnownItemsFirst_thenUnknownInOriginalOrder`) and AC-14 (`cipherConstants_matchRecordedBaseline`) FAIL; AC-15 may also fail (order of known ciphers) and would be recorded; JDK-provider tests not expected to fail
run_exit: 1; cases: `evidence/mutations/M-D4-cases.txt` (`files=2 cases=13 pass=10 fail=3 error=0 skip=0`)
failed: Hardening `reorder_ordersKnownItemsFirst_thenUnknownInOriginalOrder` (AC-12), `cipherConstants_matchRecordedBaseline` (AC-14), `createSocket_enabledCipherSuites_areProviderDefaultsMinusBlockedWithKnownFirst` (AC-15); all AssertionErrorWithFacts
passed: the other 6 Hardening tests and all 4 JDK-provider tests
expected: AC-12 and AC-14 FAIL: matches; AC-15 also failed, the possibility the entry allowed for (the order of known ciphers on the real socket); nothing unexpected failed
verdict: DETECTED
restored: 2026-11-17T22:44:29Z hash-ok porcelain-empty (hash d543927166b3c5e1da0aaa8ab584072a43bf7521 equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-D5 APPLYING 2026-11-17T22:44:30Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: line 84 `            "TLSv1.2", "TLSv1.1", "TLSv1"` becomes `            "TLSv1.1", "TLSv1.2", "TLSv1"` (ORDERED_KNOWN_PROTOCOLS)
class run: DefaultTrustedSocketFactoryHardeningTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-12 (`reorder_ordersKnownItemsFirst_...`), AC-14 (`protocolConstants_matchRecordedBaseline`), AC-16 first Hardening test FAIL; AC-16 first JDK test FAILS
run_exit: 1; cases: `evidence/mutations/M-D5-cases.txt` (`files=2 cases=13 pass=9 fail=4 error=0 skip=0`)
failed: Hardening `reorder_ordersKnownItemsFirst_thenUnknownInOriginalOrder` (AC-12), `protocolConstants_matchRecordedBaseline` (AC-14), `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder` (AC-16 first Hardening test); JDK-provider `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder_jdkProvider` (AC-16 first JDK test); all AssertionErrorWithFacts
passed: the other 6 Hardening tests (including the AC-16 second test, NOTED-AC16) and the other 3 JDK-provider tests
expected: AC-12, AC-14, AC-16 first Hardening test and AC-16 first JDK test FAIL: matches; nothing unexpected failed
verdict: DETECTED
restored: 2026-11-17T22:45:12Z hash-ok porcelain-empty (hash d543927166b3c5e1da0aaa8ab584072a43bf7521 equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-D6a APPLYING 2026-11-17T22:45:12Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: line 145 `        result.addAll(unknown);` becomes `        result.addAll(0, unknown);`
class run: DefaultTrustedSocketFactoryHardeningTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-12 (`reorder_ordersKnownItemsFirst_thenUnknownInOriginalOrder`) FAILS; other tests that check unknown-after-known (AC-15, AC-16 first) may also fail and would be recorded
run_exit: 1; cases: `evidence/mutations/M-D6a-cases.txt` (`files=2 cases=13 pass=9 fail=4 error=0 skip=0`)
failed: Hardening `reorder_ordersKnownItemsFirst_thenUnknownInOriginalOrder` (AC-12), `createSocket_enabledCipherSuites_areProviderDefaultsMinusBlockedWithKnownFirst` (AC-15), `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder` (AC-16 first Hardening test); JDK-provider `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder_jdkProvider` (AC-16 first JDK test); all AssertionErrorWithFacts
passed: the other 6 Hardening tests and the other 3 JDK-provider tests
expected: AC-12 FAILS: matches; AC-15 and both AC-16 first tests also failed (they check unknown-after-known), the possibility the entry allowed for; nothing unexpected failed
verdict: DETECTED
restored: 2026-11-17T22:45:55Z hash-ok porcelain-empty (hash d543927166b3c5e1da0aaa8ab584072a43bf7521 equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-D6b APPLYING 2026-11-17T22:45:56Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: delete line 145 `        result.addAll(unknown);`
class run: DefaultTrustedSocketFactoryHardeningTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-12 (`reorder_ordersKnownItemsFirst_thenUnknownInOriginalOrder`) FAILS; AC-15 and AC-16 set checks also expected to fail (unknown items dropped); AC-11 and AC-13 may fail as a consequence; record exactly
run_exit: 1; cases: `evidence/mutations/M-D6b-cases.txt` (`files=2 cases=13 pass=7 fail=6 error=0 skip=0`)
failed: Hardening `reorder_ordersKnownItemsFirst_thenUnknownInOriginalOrder` (AC-12), `createSocket_enabledCipherSuites_areProviderDefaultsMinusBlockedWithKnownFirst` (AC-15), `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder` (AC-16 first Hardening test), `reorder_removesEveryBlockedItem` (AC-11), `reorder_withNullBlocklistAndEmptyInput_behavesAsRecorded` (AC-13); JDK-provider `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder_jdkProvider` (AC-16 first JDK test); all AssertionErrorWithFacts
passed: the other 4 Hardening tests (AC-14 x2, AC-16 second test, AC-17) and the other 3 JDK-provider tests
expected: AC-12, AC-15 and AC-16 set checks FAIL: matches. AC-11 and AC-13 also failed, the consequence the entry allowed for (unknown test items such as the unknown cipher and the null-blocklist items are dropped); nothing unexpected failed
verdict: DETECTED
restored: 2026-11-17T22:46:38Z hash-ok porcelain-empty (hash d543927166b3c5e1da0aaa8ab584072a43bf7521 equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-D7 APPLYING 2026-11-17T22:46:38Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: line 129 `        if (blacklisted != null) {` becomes `        if (true) {`
class run: DefaultTrustedSocketFactoryHardeningTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-13 (`reorder_withNullBlocklistAndEmptyInput_behavesAsRecorded`) FAILS (NullPointerException for the null blocklist); other tests pass
run_exit: 1; cases: `evidence/mutations/M-D7-cases.txt` (`files=2 cases=13 pass=12 fail=1 error=0 skip=0`)
failed: Hardening `reorder_withNullBlocklistAndEmptyInput_behavesAsRecorded` (AC-13, java.lang.reflect.InvocationTargetException wrapping the NullPointerException)
passed: the other 8 Hardening tests and all 4 JDK-provider tests
expected: AC-13 FAILS: matches; nothing else failed, as expected
verdict: DETECTED
restored: 2026-11-17T22:47:22Z hash-ok porcelain-empty (hash d543927166b3c5e1da0aaa8ab584072a43bf7521 equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-D15 APPLYING 2026-11-17T22:47:22Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: insert `        if (enabled.length == 0) { return null; }` as the first statement of `reorder`, before line 125 `List<String> unknown = new ArrayList<String>();`
class run: DefaultTrustedSocketFactoryHardeningTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-13 (`reorder_withNullBlocklistAndEmptyInput_behavesAsRecorded`, empty-input clause) FAILS; the static initialiser only calls reorder with non-empty provider arrays, so other tests are not expected to move
run_exit: 1; cases: `evidence/mutations/M-D15-cases.txt` (`files=2 cases=13 pass=12 fail=1 error=0 skip=0`)
failed: Hardening `reorder_withNullBlocklistAndEmptyInput_behavesAsRecorded` (AC-13, java.lang.NullPointerException from the null result of the empty-input call)
passed: the other 8 Hardening tests and all 4 JDK-provider tests
expected: AC-13 FAILS (empty-input clause), nothing else moved: matches
verdict: DETECTED
restored: 2026-11-17T22:48:05Z hash-ok porcelain-empty (hash d543927166b3c5e1da0aaa8ab584072a43bf7521 equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-D8 APPLYING 2026-11-17T22:54:18Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: delete line 176 `        hardenSocket(sslSocket);`
class run: DefaultTrustedSocketFactoryHardeningTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-15 (`createSocket_enabledCipherSuites_...`) and AC-16 (`createSocket_enabledProtocols_...` Hardening) FAIL; AC-16 first JDK test (`..._jdkProvider`) FAILS; AC-17 (layered vs implicit, both unhardened) may pass
run_exit: 1; cases: `evidence/mutations/M-D8-cases.txt` (`files=2 cases=13 pass=9 fail=4 error=0 skip=0`)
failed: Hardening `createSocket_enabledCipherSuites_areProviderDefaultsMinusBlockedWithKnownFirst` (AC-15) and `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder` (AC-16 first); JDK-provider `..._jdkProvider` (AC-16 first JDK) and `createSocket_legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff` (AC-16 second JDK); all AssertionErrorWithFacts
passed: the other 7 Hardening tests (including AC-17, where both sockets are unhardened, and the AC-16 second Hardening test, NOTED-AC16) and JDK-provider AC-3 and AC-4
expected: AC-15, AC-16 (Hardening) and the AC-16 first JDK test FAIL: matches. The AC-16 second JDK test also failed (without hardening SunJSSE leaves TLS 1.0 and 1.1 off), a consequence consistent with A1.g
verdict: DETECTED
restored: 2026-11-17T22:55:02Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-D9 APPLYING 2026-11-17T22:55:03Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: delete line 184 `            sock.setEnabledCipherSuites(ENABLED_CIPHERS);`
class run: DefaultTrustedSocketFactoryHardeningTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-15 (`createSocket_enabledCipherSuites_...`) FAILS; protocol tests unaffected
run_exit: 1; cases: `evidence/mutations/M-D9-cases.txt` (`files=2 cases=13 pass=12 fail=1 error=0 skip=0`)
failed: Hardening `createSocket_enabledCipherSuites_areProviderDefaultsMinusBlockedWithKnownFirst` (AC-15, AssertionErrorWithFacts)
passed: the other 8 Hardening tests and all 4 JDK-provider tests
expected: AC-15 FAILS, nothing else: matches
verdict: DETECTED
restored: 2026-11-17T22:55:39Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-D10 APPLYING 2026-11-17T22:55:40Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: line 176 `        hardenSocket(sslSocket);` becomes `        if (socket == null) hardenSocket(sslSocket);`
class run: DefaultTrustedSocketFactoryHardeningTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-17 (`createSocket_layeredOverPlainSocket_appliesSameHardening`) FAILS; other tests unaffected (they use the implicit shape)
run_exit: 1; cases: `evidence/mutations/M-D10-cases.txt` (`files=2 cases=13 pass=12 fail=1 error=0 skip=0`)
failed: Hardening `createSocket_layeredOverPlainSocket_appliesSameHardening` (AC-17, AssertionErrorWithFacts)
passed: the other 8 Hardening tests and all 4 JDK-provider tests
expected: AC-17 FAILS, nothing else: matches
verdict: DETECTED
restored: 2026-11-17T22:56:19Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-D11 APPLYING 2026-11-17T22:56:19Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: line 107 `            supportedProtocols = sock.getSupportedProtocols();` becomes `            supportedProtocols = sock.getEnabledProtocols();`
class run: DefaultTrustedSocketFactoryHardeningTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: A1.g: AC-16, both JDK-provider tests FAIL (primary: `..._jdkProvider` and `createSocket_legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff`); the default-provider Hardening AC-16 first test is not expected to fail (D7b: supported equals enabled-minus-nothing under Conscrypt); the Hardening AC-16 second test is NOTED-AC16 (D7c) if it passes
run_exit: 1; cases: `evidence/mutations/M-D11-cases.txt` (`files=2 cases=13 pass=11 fail=2 error=0 skip=0`)
failed: JDK-provider `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder_jdkProvider` (AC-16 first JDK) and `createSocket_legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff` (AC-16 second JDK); both AssertionErrorWithFacts
passed: all 9 Hardening tests, including the default-provider AC-16 first test (D7b, as expected) and the default-provider AC-16 second test `createSocket_legacyProtocols_remainEnabled_whenProviderSupportsThem` (NOTED-AC16, see below), and the JDK AC-3 and AC-4
expected: A1.g: both JDK-provider AC-16 tests FAIL: matches. NOTED-AC16: the default-provider second AC-16 test (Hardening) did not detect M-D11 (it passes because Conscrypt supports and enables TLS 1.0 and 1.1 by default, D7c); M-D11 IS detected by the first JDK AC-16 test as rule 3 requires
verdict: DETECTED (NOTED-AC16 for the Hardening second AC-16 test)
restored: 2026-11-17T22:57:00Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-D12 APPLYING 2026-11-17T22:57:00Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: line 88 `            "SSLv3"` becomes `            "SSLv3", "TLSv1"` (TLSv1 added to BLACKLISTED_PROTOCOLS)
class run: DefaultTrustedSocketFactoryHardeningTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: A1.g: AC-14 (`protocolConstants_matchRecordedBaseline`) FAILS; AC-16 second JDK test (legacy protocols enabled by the factory) FAILS; the Hardening AC-16 second test (default provider, TLSv1 supported) is NOTED-AC16 only if it passes, the Hardening AC-16 first test may fail or pass (record exactly)
run_exit: 1; cases: `evidence/mutations/M-D12-cases.txt` (`files=2 cases=13 pass=7 fail=6 error=0 skip=0`)
failed: Hardening `protocolConstants_matchRecordedBaseline` (AC-14), `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder` (AC-16 first), `createSocket_legacyProtocols_remainEnabled_whenProviderSupportsThem` (AC-16 second, ComparisonFailureWithFacts), `reorder_ordersKnownItemsFirst_thenUnknownInOriginalOrder` (AC-12); JDK-provider `..._jdkProvider` (AC-16 first JDK) and `createSocket_legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff` (AC-16 second JDK); all but one AssertionErrorWithFacts
passed: the other 5 Hardening tests and JDK AC-3 and AC-4
expected: AC-14 and AC-16 second JDK test FAIL: matches. Extra, explained: the blocked list now removes TLSv1 so the protocols no longer equal supported-minus-SSLv3 (Hardening AC-16 first and second, JDK first) and the reorder test, which feeds TLSv1 through the live blocked list, loses it (AC-12). NOTED-AC16 does NOT apply here: the Hardening second AC-16 test did detect M-D12
verdict: DETECTED
restored: 2026-11-17T22:57:47Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-D13 APPLYING 2026-11-17T22:57:48Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: delete line 187 `            sock.setEnabledProtocols(ENABLED_PROTOCOLS);`
class run: DefaultTrustedSocketFactoryHardeningTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: A1.g: AC-16 first test (Hardening; the order changes) FAILS and AC-16 first JDK test FAILS; AC-16 second tests: Hardening may pass (NOTED-AC16 not applicable to M-D13), JDK second FAILS if the legacy protocols are no longer enabled
run_exit: 1; cases: `evidence/mutations/M-D13-cases.txt` (`files=2 cases=13 pass=10 fail=3 error=0 skip=0`)
failed: Hardening `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder` (AC-16 first); JDK-provider `..._jdkProvider` (AC-16 first JDK) and `createSocket_legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff` (AC-16 second JDK); all AssertionErrorWithFacts
passed: the other 8 Hardening tests (the AC-16 second Hardening test passes, as expected under Conscrypt) and JDK AC-3 and AC-4
expected: AC-16 first (Hardening and JDK) FAIL: matches; the AC-16 second JDK test also failed (the legacy protocols are no longer enabled by the factory), as allowed for; nothing unexpected
verdict: DETECTED
restored: 2026-11-17T22:58:30Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-D14 APPLYING 2026-11-17T22:58:31Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: line 61 `            "TLS_RSA_WITH_AES_128_CBC_SHA",` becomes `            "TLS_RSA_WITH_AES_128_CBC_SHA256",`
class run: DefaultTrustedSocketFactoryHardeningTest and DefaultTrustedSocketFactoryJdkProviderTest (one invocation)
expected: AC-14 (`cipherConstants_matchRecordedBaseline`) FAILS; other tests not expected to fail (record exactly)
run_exit: 1; cases: `evidence/mutations/M-D14-cases.txt` (`files=2 cases=13 pass=11 fail=2 error=0 skip=0`)
failed: Hardening `cipherConstants_matchRecordedBaseline` (AC-14) and `createSocket_enabledCipherSuites_areProviderDefaultsMinusBlockedWithKnownFirst` (AC-15); both AssertionErrorWithFacts
passed: the other 7 Hardening tests and all 4 JDK-provider tests
expected: AC-14 FAILS: matches. AC-15 also failed (the real-socket order check sees the known cipher misplaced as an unknown one), a consequence of the edit; nothing unexpected otherwise
verdict: DETECTED
restored: 2026-11-17T22:59:11Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-B1 APPLYING 2026-11-17T22:59:12Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/TrustManagerFactory.java
hash-before: 3b448058a393c5455ba9524f4b1a5fe9e2926c88 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: delete line 77 `                verifyHostname(mHost, chain);` (TrustManagerFactory.java, host-name check)
class run: DefaultTrustedSocketFactoryHandshakeTest
expected: AC-6 (`implicitTls_otherHost_rejectedByHostnameCheck`) and AC-8 (`startTls_otherHost_rejectedByHostnameCheck`) FAIL (the handshake completes); the untrusted and expired rejections and the controls PASS
run_exit: 1; cases: `evidence/mutations/M-B1-cases.txt` (`files=0 cases=0 pass=0 fail=0 error=0 skip=0`)
failed: none ran: the vendored module did not compile (`k9mail-vendored:compileDebugJavaWithJavac` FAILED; `TrustManagerFactory.java:82: error: exception SSLException is never thrown in body of corresponding try statement`, because `verifyHostname` was the only call in the try body that throws `SSLException`); no JUnit XML produced
passed: none ran
expected: no test could fail: not a detection result. The plan says COMPILE_FAIL is invalid and never counted ("re-edit it and re-run"), but the plan supplies no alternative edit and the orchestrator said not to invent one: reported for a ruling. AC-6 and AC-8 remain covered by M-B2 (and M-B3 for AC-5/AC-7) in this turn
verdict: COMPILE_FAIL (invalid, not counted)
restored: 2026-11-17T22:59:49Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-B3 APPLYING 2026-11-17T22:59:50Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/TrustManagerFactory.java
hash-before: 3b448058a393c5455ba9524f4b1a5fe9e2926c88 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: line 77 `                verifyHostname(mHost, chain);` becomes `                verifyHostname(mHost + ".mutant", chain);`
class run: DefaultTrustedSocketFactoryHandshakeTest
expected: AC-5 (`implicitTls_rightHost_handshakeCompletes`) and AC-7 (`startTls_rightHost_handshakeCompletes`) FAIL (the right host no longer verifies); the other-host, untrusted and expired rejections PASS
run_exit: 1; cases: `evidence/mutations/M-B3-cases.txt` (`files=1 cases=8 pass=6 fail=2 error=0 skip=0`)
failed: Handshake `implicitTls_rightHost_handshakeCompletes` (AC-5) and `startTls_rightHost_handshakeCompletes` (AC-7); both javax.net.ssl.SSLHandshakeException
passed: the other 6 Handshake tests (all four rejections of other-host, untrusted and expired chains, which still reject)
expected: AC-5 and AC-7 FAIL: matches; nothing else moved
verdict: DETECTED
restored: 2026-11-17T23:00:32Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-B4 APPLYING 2026-11-17T23:00:32Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/TrustManagerFactory.java
hash-before: 3b448058a393c5455ba9524f4b1a5fe9e2926c88 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: line 165 `            return chain;` (in `MinimalSslSession.getPeerCertificates`) becomes `            return new Certificate[0];`
class run: DefaultTrustedSocketFactoryHandshakeTest
expected: AC-5 and AC-7 (the right-host controls) FAIL (the verifier sees an empty chain); the rejections PASS
run_exit: 1; cases: `evidence/mutations/M-B4-cases.txt` (`files=1 cases=8 pass=4 fail=4 error=0 skip=0`)
failed: Handshake `implicitTls_rightHost_handshakeCompletes` (AC-5) and `startTls_rightHost_handshakeCompletes` (AC-7), both javax.net.ssl.SSLHandshakeException; also `implicitTls_otherHost_rejectedByHostnameCheck` (AC-6) and `startTls_otherHost_rejectedByHostnameCheck` (AC-8), both AssertionErrorWithFacts
passed: the four untrusted-chain and expired-chain rejection tests (AC-9, AC-10)
expected: AC-5 and AC-7 FAIL: matches. AC-6 and AC-8 also failed, explained: the other-host rejection still happens (an empty chain does not match either) but the exception no longer carries the "was not verified against the server certificate" marker the tests require, so the host-name-check marker assertion fails; nothing unexpected otherwise
verdict: DETECTED
restored: 2026-11-17T23:01:20Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-C1 APPLYING 2026-11-17T23:01:21Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/TrustManagerFactory.java
hash-before: 3b448058a393c5455ba9524f4b1a5fe9e2926c88 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: delete line 76 `                defaultTrustManager.checkServerTrusted(chain, authType);`
class run: DefaultTrustedSocketFactoryHandshakeTest
expected: AC-9 (`implicitTls_untrustedChain_rejected`, `startTls_untrustedChain_rejected`) and AC-10 (`implicitTls_expiredChain_rejected`, `startTls_expiredChain_rejected`) FAIL (chain no longer validated); controls and other-host rejections PASS
run_exit: 1; cases: `evidence/mutations/M-C1-cases.txt` (`files=0 cases=0 pass=0 fail=0 error=0 skip=0`)
failed: none ran: the vendored module did not compile (`TrustManagerFactory.java:78: error: exception CertificateException is never thrown in body of corresponding try statement`, because `defaultTrustManager.checkServerTrusted` was the only call in the try body that throws `CertificateException`); no JUnit XML produced
passed: none ran
expected: no test could fail: not a detection result. COMPILE_FAIL is invalid and never counted; the plan supplies no alternative edit and the orchestrator said not to invent one: reported for a ruling. AC-9 and AC-10 remain covered in this turn by M-C2 and M-B2
verdict: COMPILE_FAIL (invalid, not counted)
restored: 2026-11-17T23:01:55Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-C2 APPLYING 2026-11-17T23:01:56Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/TrustManagerFactory.java
hash-before: 3b448058a393c5455ba9524f4b1a5fe9e2926c88 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: line 91 `            if (!keyStore.isValidCertificate(certificate, mHost, mPort)) {` becomes `            if (false) {`
class run: DefaultTrustedSocketFactoryHandshakeTest
expected: AC-6, AC-8, AC-9 (2) and AC-10 (2) FAIL (the silent fallback to the local key store accepts: the chain exception is never thrown); controls PASS
run_exit: 1; cases: `evidence/mutations/M-C2-cases.txt` (`files=1 cases=8 pass=2 fail=6 error=0 skip=0`)
failed: Handshake `implicitTls_otherHost_rejectedByHostnameCheck` (AC-6), `startTls_otherHost_rejectedByHostnameCheck` (AC-8), `implicitTls_untrustedChain_rejected` and `startTls_untrustedChain_rejected` (AC-9), `implicitTls_expiredChain_rejected` and `startTls_expiredChain_rejected` (AC-10); all six java.lang.AssertionError ("handshake completed" for a chain that must be rejected)
passed: the two right-host controls (AC-5, AC-7)
expected: AC-6, AC-8, AC-9 (2) and AC-10 (2) FAIL, controls PASS: matches exactly
verdict: DETECTED
restored: 2026-11-17T23:02:53Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-B2 APPLYING 2026-11-17T23:02:53Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: line 155 `        TrustManager[] trustManagers = new TrustManager[] { TrustManagerFactory.get(host, port) };` becomes an accept-all `TrustManager[]` holding an anonymous `javax.net.ssl.X509TrustManager` whose `checkClientTrusted` and `checkServerTrusted` do nothing and whose `getAcceptedIssuers` returns an empty array (the plan's exact text)
class run: DefaultTrustedSocketFactoryHandshakeTest
expected: AC-6, AC-8, AC-9 (2), AC-10 (2) FAIL (every chain is accepted); controls PASS
run_exit: 1; cases: `evidence/mutations/M-B2-cases.txt` (`files=1 cases=8 pass=2 fail=6 error=0 skip=0`)
failed: Handshake `implicitTls_expiredChain_rejected` and `startTls_expiredChain_rejected` (AC-10), `implicitTls_untrustedChain_rejected` and `startTls_untrustedChain_rejected` (AC-9), `implicitTls_otherHost_rejectedByHostnameCheck` (AC-6), `startTls_otherHost_rejectedByHostnameCheck` (AC-8); all six java.lang.AssertionError
passed: the two right-host controls (AC-5, AC-7)
expected: AC-6, AC-8, AC-9 (2), AC-10 (2) FAIL, controls PASS: matches exactly
verdict: DETECTED
restored: 2026-11-17T23:03:49Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-B5 APPLYING 2026-11-17T23:03:49Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
hash-before: d543927166b3c5e1da0aaa8ab584072a43bf7521 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: line 172 `            trustedSocket = socketFactory.createSocket(socket, host, port, true);` becomes `            trustedSocket = socket;`
class run: DefaultTrustedSocketFactoryHandshakeTest
expected: AC-7, AC-8 (STARTTLS right-host control and other-host rejection), AC-9b and AC-10b (STARTTLS untrusted and expired rejections) FAIL (ClassCastException when the plain socket is cast to SSLSocket); implicit tests PASS
run_exit: 1; cases: `evidence/mutations/M-B5-cases.txt` (`files=1 cases=8 pass=4 fail=4 error=0 skip=0`)
failed: Handshake `startTls_rightHost_handshakeCompletes` (AC-7), `startTls_otherHost_rejectedByHostnameCheck` (AC-8), `startTls_untrustedChain_rejected` (AC-9b), `startTls_expiredChain_rejected` (AC-10b); all four java.lang.ClassCastException
passed: the four implicit-TLS tests (AC-5, AC-6, AC-9a, AC-10a)
expected: AC-7, AC-8, AC-9b, AC-10b FAIL with ClassCastException, implicit tests PASS: matches exactly
verdict: DETECTED
restored: 2026-11-17T23:04:41Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

## I6c (Turn 10)

Baseline at I6c start (unmutated, Handshake class and PinnedCertificateSocketFactoryCreateSocketTest in one invocation): `evidence/mutations/I6c-baseline-start-cases.txt`: `files=2 cases=13 pass=13 fail=0 error=0 skip=0`.

Orchestrator ruling 2026-11-17: M-B1 and M-C1 stay recorded as COMPILE_FAIL (invalid, not counted); the replacements M-B1r and M-C1r below are valid mutations.

### M-B1r APPLYING 2026-11-17T23:10:27Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/TrustManagerFactory.java
hash-before: 3b448058a393c5455ba9524f4b1a5fe9e2926c88 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: replacement for the non-compiling M-B1 by orchestrator ruling of 2026-11-17: in `SecureX509TrustManager.checkServerTrusted` the line `                verifyHostname(mHost, chain);` becomes `                if (false) verifyHostname(mHost, chain);` (the statement still counts for the compiler's exception analysis, so the `catch (SSLException e)` stays legal, and never runs)
class run: DefaultTrustedSocketFactoryHandshakeTest
expected: exactly AC-6 (`implicitTls_otherHost_rejectedByHostnameCheck`) and AC-8 (`startTls_otherHost_rejectedByHostnameCheck`) FAIL (handshake accepted); the controls (AC-5, AC-7) and the four untrusted and expired tests (AC-9, AC-10) PASS; any other set is a stop after restore
run_exit: 1; cases: `evidence/mutations/M-B1r-cases.txt` (`files=1 cases=8 pass=6 fail=2 error=0 skip=0`)
failed: Handshake `implicitTls_otherHost_rejectedByHostnameCheck` (AC-6) and `startTls_otherHost_rejectedByHostnameCheck` (AC-8); both java.lang.AssertionError (handshake completed for the other-host chain)
passed: the 2 controls (AC-5, AC-7) and the 4 untrusted and expired tests (AC-9, AC-10)
expected: exactly AC-6 and AC-8 FAIL, everything else PASS: matches the ruling exactly
verdict: DETECTED (replacement for COMPILE_FAIL M-B1)
restored: 2026-11-17T23:11:19Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-C1r APPLYING 2026-11-17T23:11:20Z
file: k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/TrustManagerFactory.java
hash-before: 3b448058a393c5455ba9524f4b1a5fe9e2926c88 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: replacement for the non-compiling M-C1 by orchestrator ruling of 2026-11-17: in `SecureX509TrustManager.checkServerTrusted` the line `                defaultTrustManager.checkServerTrusted(chain, authType);` becomes `                if (false) defaultTrustManager.checkServerTrusted(chain, authType);` (the statement still counts for the compiler's exception analysis, so the `catch (CertificateException e)` stays legal, and never runs)
class run: DefaultTrustedSocketFactoryHandshakeTest
expected: exactly the four untrusted and expired tests (AC-9 x2, AC-10 x2) FAIL (chain no longer validated, host name correct); the 2 controls and the 2 other-host tests PASS; any other set is a stop after restore
run_exit: 1; cases: `evidence/mutations/M-C1r-cases.txt` (`files=1 cases=8 pass=4 fail=4 error=0 skip=0`)
failed: Handshake `implicitTls_untrustedChain_rejected` and `startTls_untrustedChain_rejected` (AC-9), `implicitTls_expiredChain_rejected` and `startTls_expiredChain_rejected` (AC-10); all four java.lang.AssertionError (handshake completed for a chain that must be rejected)
passed: the 2 controls (AC-5, AC-7) and the 2 other-host tests (AC-6, AC-8)
expected: exactly the four untrusted and expired tests FAIL, everything else PASS: matches the ruling exactly
verdict: DETECTED (replacement for COMPILE_FAIL M-C1)
restored: 2026-11-17T23:12:06Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-E1 APPLYING 2026-11-17T23:12:07Z
file: app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
hash-before: 8a0fbb64fb171624591fbebafb818555f28efd58 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: replace lines 81-85 (the `if (socket == null) { return socketFactory.createSocket(); } else { return socketFactory.createSocket(socket, host, port, true); }` block) with three lines: `Socket s = (socket == null) ? socketFactory.createSocket() : socketFactory.createSocket(socket, host, port, true);`, `((javax.net.ssl.SSLSocket) s).setEnabledProtocols(new String[] {"TLSv1.2"});`, `return s;`
class run: com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest
expected: AC-18 (both: `createSocket_implicit_leavesProviderDefaultsUntouched` and `createSocket_layered_leavesProviderDefaultsUntouched`) FAIL (enabled protocols now [TLSv1.2]); AC-19, AC-20, AC-21 PASS
run_exit: 1; cases: `evidence/mutations/M-E1-cases.txt` (`files=1 cases=5 pass=3 fail=2 error=0 skip=0`)
failed: PinnedCertificateSocketFactoryCreateSocketTest `createSocket_implicit_leavesProviderDefaultsUntouched` and `createSocket_layered_leavesProviderDefaultsUntouched` (AC-18); both ComparisonFailureWithFacts
passed: AC-19 `createSocket_implicit_setsNoServerNames`, AC-20 `handshake_enrolledCertificateForDifferentHostName_isAccepted`, AC-21 `handshake_certificateOtherThanEnrolled_isRejected`
expected: AC-18 (both) FAIL; AC-19, AC-20, AC-21 PASS: matches exactly
verdict: DETECTED
restored: 2026-11-17T23:13:03Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-E2 APPLYING 2026-11-17T23:13:04Z
file: app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
hash-before: 8a0fbb64fb171624591fbebafb818555f28efd58 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: as M-E1 (lines 81-85 replaced by three lines) but the middle line becomes `javax.net.ssl.SSLParameters p = ((javax.net.ssl.SSLSocket) s).getSSLParameters(); p.setServerNames(java.util.Collections.<javax.net.ssl.SNIServerName>singletonList(new javax.net.ssl.SNIHostName(host))); ((javax.net.ssl.SSLSocket) s).setSSLParameters(p);` between `Socket s = (socket == null) ? ... ;` and `return s;`
class run: com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest
expected: AC-19 (`createSocket_implicit_setsNoServerNames`) FAILS (a server name is set); AC-18, AC-20, AC-21 PASS
run_exit: 0; cases: `evidence/mutations/M-E2-cases.txt` (`files=1 cases=5 pass=5 fail=0 error=0 skip=0`)
failed: none (BUILD SUCCESSFUL, run_exit 0): all 5 tests passed, including AC-19 `createSocket_implicit_setsNoServerNames`
passed: all 5 tests of the class (AC-18 x2, AC-19, AC-20, AC-21)
expected: AC-19 expected to FAIL: it PASSED. M-E2 sets a real SNIHostName on the factory socket and the test still sees null or empty. Reason on record: under this provider the observable `getSSLParameters().getServerNames()` of the Conscrypt client socket reports null whatever was set (probe D5b, `sni_obs_pinned_implicit`; I0c: getter null before and after the handshake), and the test Javadoc says so. Criterion Status Rule, rule 2: AC-19 is UNMET; the NOTED-AC16 exception does not cover it. Stopped after restore, no further mutation run
verdict: NOT DETECTED
restored: 2026-11-17T23:14:06Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

## I6c-resume (Turn 10, resumed 2026-11-20)

Context: the I6c turn was interrupted on 2026-11-17 after M-E2 (NOT DETECTED above, which stays as recorded: under the default provider the server-name observable reads nothing whatever is set). Product owner decision of 2026-11-20 (deviation 16): the AC-19 test moved from `PinnedCertificateSocketFactoryCreateSocketTest` to `DefaultTrustedSocketFactoryJdkProviderTest` as `pinnedFactory_createSocket_implicit_setsNoServerNames_jdkProvider`; the pinned class now has 4 tests (AC-18 x2, AC-20, AC-21), the JDK-provider class 5. The move was shown to change nothing else (`evidence/I6c-ac19-move-pinned.diff.txt`, `evidence/I6c-ac19-move-jdk.diff.txt`), so M-E1 (run 2026-11-17 against the pre-move pinned class) is not re-run: the two AC-18 tests it exercised are unchanged.

Baseline at resume (unmutated, both classes): `evidence/mutations/I6c-move-targeted-cases.txt`: pinned class 4 of 4, JDK-provider class 5 of 5, existing `PinnedCertificateSocketFactoryTest` 8 of 8, all PASS.

### M-E2-jdk APPLYING 2026-11-20T23:05:06Z
file: app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
hash-before: 8a0fbb64fb171624591fbebafb818555f28efd58 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: Re-run of M-E2 after the product owner decision of 2026-11-20. Replace lines 81-85 (the `if (socket == null) { return socketFactory.createSocket(); } else { return socketFactory.createSocket(socket, host, port, true); }` block) with three lines: `Socket s = (socket == null) ? socketFactory.createSocket() : socketFactory.createSocket(socket, host, port, true);`, then `javax.net.ssl.SSLParameters p = ((javax.net.ssl.SSLSocket) s).getSSLParameters(); p.setServerNames(java.util.Collections.<javax.net.ssl.SNIServerName>singletonList(new javax.net.ssl.SNIHostName(host))); ((javax.net.ssl.SSLSocket) s).setSSLParameters(p);`, then `return s;` (identical to the 2026-11-17 M-E2 edit)
class run: com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryJdkProviderTest and com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest (one invocation)
expected: `pinnedFactory_createSocket_implicit_setsNoServerNames_jdkProvider` (AC-19, JDK-provider class) FAILS; every other test in both classes (4 + 4) PASSES
run_exit: 1; cases: `evidence/mutations/M-E2-jdk-cases.txt` (`files=2 cases=9 pass=8 fail=1 error=0 skip=0`)
failed: `pinnedFactory_createSocket_implicit_setsNoServerNames_jdkProvider` (AC-19, JDK-provider class), com.google.common.truth.AssertionErrorWithFacts
passed: the other 8 tests: JDK-provider class `createSocket_dnsHost_setsServerNameOnRealSocket` (AC-4), `createSocket_ipv6EightGroupLiteralHost_leavesServerNamesUnset` (AC-3), both AC-16 JDK tests; pinned class AC-18 x2, AC-20, AC-21
expected: AC-19 test FAILS, all other tests of both classes PASS: matches exactly. The observable that was vacuous under the default provider (M-E2, 2026-11-17, NOT DETECTED) works under the JDK provider
verdict: DETECTED (re-run of M-E2 after the 2026-11-20 decision; the 2026-11-17 NOT DETECTED entry stays)
restored: 2026-11-20T23:06:25Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-E3 APPLYING 2026-11-20T23:06:38Z
file: app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
hash-before: 8a0fbb64fb171624591fbebafb818555f28efd58 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: Add the field `    private final String host;` after line 57 (`private final X509Certificate enrolledCert;`); add `        this.host = host;` after line 71 (`this.enrolledCert = enrolledCert;`); after line 145 (end of the fingerprint-mismatch `if`) add `            if (!String.valueOf(presented.getSubjectAlternativeNames()).contains(host)) { throw new CertificateException("U-063 mutant host check"); }` (three insertions, the plan's exact text; three Edit-tool calls)
class run: com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest
expected: AC-20 (`handshake_enrolledCertificateForDifferentHostName_isAccepted`) FAILS (the enrolled other-host leaf has no SAN containing the factory host `imap.right.test`); AC-18 x2 and AC-21 PASS (the fingerprint mismatch is thrown before the new check, so the pin marker is unchanged)
run_exit: 1; cases: `evidence/mutations/M-E3-cases.txt` (`files=1 cases=4 pass=3 fail=1 error=0 skip=0`)
failed: pinned class `handshake_enrolledCertificateForDifferentHostName_isAccepted` (AC-20), javax.net.ssl.SSLHandshakeException (the added host check rejects the certificate)
passed: pinned class `createSocket_implicit_leavesProviderDefaultsUntouched`, `createSocket_layered_leavesProviderDefaultsUntouched` (AC-18), `handshake_certificateOtherThanEnrolled_isRejected` (AC-21)
expected: AC-20 FAILS, AC-18 x2 and AC-21 PASS: matches exactly
verdict: DETECTED
restored: 2026-11-20T23:07:57Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-E4 APPLYING 2026-11-20T23:08:08Z
file: app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
hash-before: 8a0fbb64fb171624591fbebafb818555f28efd58 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: line 141 `            if (!Arrays.equals(presentedFingerprint, enrolledFingerprint)) {` becomes `            if (Arrays.equals(presentedFingerprint, enrolledFingerprint)) {` (the fingerprint comparison inverted)
class run: com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest
expected: AC-20 (`handshake_enrolledCertificateForDifferentHostName_isAccepted`) and AC-21 (`handshake_certificateOtherThanEnrolled_isRejected`) FAIL; AC-18 x2 PASS
run_exit: 1; cases: `evidence/mutations/M-E4-cases.txt` (`files=1 cases=4 pass=2 fail=2 error=0 skip=0`)
failed: pinned class `handshake_enrolledCertificateForDifferentHostName_isAccepted` (AC-20, javax.net.ssl.SSLHandshakeException) and `handshake_certificateOtherThanEnrolled_isRejected` (AC-21, java.lang.AssertionError: handshake completed)
passed: pinned class `createSocket_implicit_leavesProviderDefaultsUntouched`, `createSocket_layered_leavesProviderDefaultsUntouched` (AC-18)
expected: AC-20 and AC-21 FAIL, AC-18 x2 PASS: matches exactly
verdict: DETECTED
restored: 2026-11-20T23:09:10Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)

### M-E5 APPLYING 2026-11-20T23:09:19Z
file: app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
hash-before: 8a0fbb64fb171624591fbebafb818555f28efd58 (equals the base blob; product-clean check passed: `git diff --quiet BASE -- PROD` exit 0, porcelain for `app/src/main k9mail-vendored` empty)
edit: line 77 `        TrustManager[] trustManagers = new TrustManager[]{ new PinnedX509TrustManager() };` becomes an accept-all `TrustManager[]` holding an anonymous `javax.net.ssl.X509TrustManager` whose `checkClientTrusted` and `checkServerTrusted` do nothing and whose `getAcceptedIssuers` returns `new java.security.cert.X509Certificate[0]` (exactly as M-B2)
class run: com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest
expected: AC-21 (`handshake_certificateOtherThanEnrolled_isRejected`) FAILS (every certificate is accepted); AC-18 x2 and AC-20 PASS
run_exit: 1; cases: `evidence/mutations/M-E5-cases.txt` (`files=1 cases=4 pass=3 fail=1 error=0 skip=0`)
failed: pinned class `handshake_certificateOtherThanEnrolled_isRejected` (AC-21, java.lang.AssertionError: handshake completed)
passed: pinned class `createSocket_implicit_leavesProviderDefaultsUntouched`, `createSocket_layered_leavesProviderDefaultsUntouched` (AC-18), `handshake_enrolledCertificateForDifferentHostName_isAccepted` (AC-20)
expected: AC-21 FAILS, AC-18 x2 and AC-20 PASS: matches exactly
verdict: DETECTED
restored: 2026-11-20T23:10:05Z hash-ok porcelain-empty (the edited file's hash equals its hash-before and the base blob; the other two production files are at their base hashes; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty)
