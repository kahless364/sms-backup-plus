---
artifact_type: qa-results
story_id: "U-063"
gate_id: "qa-validation"
checkpoint: "quality-gate"
verdict: "PASS"                        # PASS | FAIL
agent: "QA Analyst"
timestamp: "2026-11-20"
ac_total: 37                           # AC-1..AC-24 plus IC-1..IC-13
ac_passed: 37                          # 20 MET, 17 MET WITH DEVIATION (each named and judged below)
ac_failed: 0
tests_run: 710                         # my one full gate run on the committed tree (JUnit XML of that run only)
tests_passed: 708                      # 0 failures, 0 errors, 2 skipped (the two baseline skips)
---

# QA Validation: U-063

## Verdict: PASS

## Summary

U-063 adds characterisation tests for the two TLS socket factories and changes no production, vendored, build or dependency file. I validated the committed tree at `5a5321d4` against the story start commit `395000956bb46cb2acc6d66e62d4b5ef933a2d90`. All 24 acceptance criteria and 13 integrity criteria are met: 20 outright and 17 with a deviation. Each deviation is recorded in the story's Implementation Notes, was decided by the right party, and I judge each one justified against the story's own text. No criterion is unmet.

What I derived myself:
- **Full gate run, once, on the committed tree.** 710 tests, 0 failures, 0 errors, 2 skipped, exit 0, daemon JVM 25.0.5.1. Per class: SNI 12, JDK-provider 5, Hardening 9, Handshake 8, pinned create-socket 4, existing pinned 8.
- **Literal IC-2, IC-4, IC-6 and IC-8 forms over `<base>..HEAD`.**
- **The fixtures.** I decoded all ten constants independently (openssl): subjects, issuers, SANs, dates and SHA-256 fingerprints. Each leaf key pairs with its own certificate and with neither CA.
- **The mutation log, checked mechanically.** All 39 entries and their 39 case files agree, with no mismatches. Verdict totals: 34 DETECTED, 2 BY-DESIGN, 2 COMPILE_FAIL, 1 NOT DETECTED.
- **Two re-demonstrated mutations, both reproduced exactly and restored with hash proof:** M-C1r in `TrustManagerFactory.java` and M-E4 in `PinnedCertificateSocketFactory.java`.
- **A post-restore targeted run.** 46 of 46 pass across the six TLS classes and the existing pinned test.

What rests on recorded evidence, read and cross-checked but not re-run:
- the I0 baseline (IC-1, IC-3 at I0) and the environment probe (IC-11);
- the three IC-6 repeat runs and the two IC-9 ordered-suite runs;
- the other 37 mutation runs;
- the `jacocoTestReport` figures (my gate list did not regenerate the report; the report on disk is from the fix-cycle gate over the same blobs).

One statement in my brief is contradicted by the file. During validation the story frontmatter read `status: review` (with `current_build_phase: validation`), not `in-progress`. I did not change it. After I wrote this file, an automatic process outside my session rewrote the story frontmatter at 2026-11-21T00:20:36Z: `status: done`, `resolution: done`, and `current_build_phase` removed (Issue 1). Every other factual statement in the brief that I tested held. That covers the commit contents (164 files: 7 test files, the story file, 156 workspace files), the uncommitted set, the counts (682 to 710) and the three expected IC-8 matches.

## Acceptance and Integrity Criteria Results

Legend: MET; MET WITH DEVIATION (deviation number as in the story's Implementation Notes, story lines 288-325); NOT MET. Paths are repository-relative. "Story" means `sdlc/artifacts/build/sprints/sprint-020/U-063-tls-socket-factory-characterisation-tests.md`. "Log" means `.../U-063/implementation-log.md`. "ML" means `.../U-063/evidence/mutation-log.md`.

| ID | Criterion (stated unambiguously) | Check I performed | What I observed | Result |
|----|----------------------------------|-------------------|-----------------|--------|
| AC-1 | In the SNI test file, a test calls `setSniViaSSLParameters(mock, "2001:0db8:0000:0000:0000:0000:0000:0001")` and asserts no exception and `setSSLParameters` never called. It must fail when **both** the guard (`DefaultTrustedSocketFactory.java:239-243`) and the catch (`:248`) are removed. Removing one layer is allowed to leave it green. | Read the test against the code; ran it in my gate; read ML M-A1, M-A1g, M-A1c and their case files. | `DefaultTrustedSocketFactorySniTest.java:277-286` (Mockito socket, `verify(..., never()).setSSLParameters`). PASS in my gate. M-A1 (both layers removed) fails it with `IllegalArgumentException` (`mutations/M-A1-cases.txt`). M-A1g and M-A1c leave it green, as AC-1 itself says. The probe confirms `SNIHostName` rejects this literal (`evidence/I0-probe.txt:13`). | MET |
| AC-2 | A test calls package-private `isIpOrBlankHostname` by reflection with the eight-group literal and `1:2:3:4:5:6:7:8` and asserts `true` for both. It must fail if the colon branch (`:221-223`) is removed or narrowed to `"::"`. | Read and ran it; read M-A2a and M-A2b case files. | `DefaultTrustedSocketFactorySniTest.java:295-302`. PASS. M-A2a (branch deleted) and M-A2b (`contains("::")`) each fail exactly this test, 1 of 16 (`mutations/M-A2a-cases.txt`, `M-A2b-cases.txt`). | MET |
| AC-3 | `createSocket(null, <eight-group literal>, 993, null)` on the default factory does not throw, returns an `SSLSocket`, and `getSSLParameters().getServerNames()` is null or empty; the socket is closed in `finally`. It must fail if `setSniHost` reports a name for an IP literal, or if guard and catch are both removed. | Read the test; ran it in my gate; read M-A1 and M-A3 case files; checked the as-built note. | `DefaultTrustedSocketFactoryJdkProviderTest.java:172-184`. PASS. M-A1 fails it (exception escapes `createSocket`). M-A3 fails it (a name appears). Under Conscrypt the observable reads `null` whatever is set (`I0-probe.txt:23-25`), so in the SNI file the test would have been vacuous; that is why it moved. | MET WITH DEVIATION 14. Justified: the story requires that AC-3 "is not vacuous" (AC-4 text), and only the JDK provider makes it falsifiable. Decided by the product owner (story :317). |
| AC-4 | Control: `createSocket(null, "imap.right.test", 993, null)` reports exactly one `SNIHostName` whose ASCII name is `imap.right.test`. It must fail if `setSniHost` at `:177` is removed or made a no-op. | As AC-3; read M-A4, M-A4-precheck and M-A3. | `DefaultTrustedSocketFactoryJdkProviderTest.java:191-203`. PASS. M-A4 and the pre-check fail exactly this test, 1 of 16 and 1 of 4. M-A3 fails it (`ComparisonFailure`). | MET WITH DEVIATION 14 (same judgement as AC-3) |
| AC-5 | Implicit-TLS control: a right-host chain from the trusted test CA is accepted, and a byte the server writes after the handshake is read. It must fail if the host-name check gets the wrong host or `MinimalSslSession.getPeerCertificates()` returns empty. | Read `assertControl`; ran it; read M-B3 and M-B4. | `DefaultTrustedSocketFactoryHandshakeTest.java:206-209, 280-291`: handshake, `read()` equals `0x2A`, and server outcome `OK` (stronger than the AC). PASS. M-B3 and M-B4 fail it with `SSLHandshakeException`. | MET WITH DEVIATION 13. The server always wraps a plain socket (`LoopbackTlsTestServer.java:107-110`). This is server-side only, and the client path the AC describes is unchanged (`HandshakeTest.java:314-326`). Justified; orchestrator ruling. |
| AC-6 | Implicit TLS, other-host chain: `startHandshake()` throws `SSLException`; the direct cause is a `CertificateException` that is a `CertificateChainException`; the cause chain contains "was not verified against the server certificate". It must fail if `verifyHostname(mHost, chain)` (`TrustManagerFactory.java:77`) is removed or the trust manager is replaced by accept-all. | Read `assertRejected` and `assertRejectedByCertificateCheck`; ran it; read M-B1, M-B1r, M-B2 and M-C2. | `HandshakeTest.java:215-218, 297-308, 332-350`. `fail()` sits inside `try`, and `AssertionError` is not an `SSLException`. Both cause types and the marker are asserted (20-step walk that includes `e`). PASS. M-B1r fails exactly AC-6 and AC-8. M-B2 and M-C2 fail it. | MET WITH DEVIATION 15 (M-B1 replaced by M-B1r) and 13 |
| AC-7 | STARTTLS control: the client reads the plain line from a plain socket, then `createSocket(plain, "imap.right.test", port, null)`; the handshake completes and the byte is read. It must also fail if the layered branch (`:172`) returns the plain socket. | Read `connectClient` and `readLine`; ran it; read M-B3, M-B4 and M-B5. | `HandshakeTest.java:224-227, 320-324`. The greeting is asserted; `readLine` is unbuffered (`LoopbackTlsTestServer.java:201-219`). PASS. M-B5 fails it (`ClassCastException`), as do M-B3 and M-B4. | MET WITH DEVIATION 13 |
| AC-8 | As AC-7 with the other-host chain; assertions as AC-6. | As above. | `HandshakeTest.java:232-235`. PASS. Fails under M-B1r, M-B2, M-B5 and M-C2 (case files). | MET WITH DEVIATION 15 and 13 |
| AC-9 | Untrusted chain (right host, signed by a CA that is not an anchor), on both shapes: `SSLException`, direct cause `CertificateChainException`, and **no** host-name text in the cause chain. It must fail if `checkServerTrusted` (`TrustManagerFactory.java:76`) is removed or the key-store fallback (`:91`) accepts. | Read the tests; ran them; read M-C1, M-C1r and M-C2; **re-demonstrated M-C1r myself**. | `HandshakeTest.java:241-252`. PASS. ML M-C1r: exactly the 4 untrusted and expired tests fail. My re-run of M-C1r gave the same 4 failures, each "the handshake must be rejected, but it completed", with the other 4 passing. M-C2 fails it. The fixture's issuer is the untrusted CA and its SAN is `imap.right.test` (my openssl decode). | MET WITH DEVIATION 15 (M-C1 replaced by M-C1r) and 13 |
| AC-10 | Expired leaf for the right host from the trusted CA, validity ended in the past, both shapes; same assertions and mutations as AC-9. | As AC-9; decoded the fixture dates. | `HandshakeTest.java:259-270`. PASS. Fails under M-C1r (also in my re-run), M-B2 and M-C2. `EXPIRED_HOST_CERT`: notBefore 2020-01-01, notAfter 2020-01-31T06:00Z, issuer Trusted CA, SAN `imap.right.test`. The test does not assert that expiry is the reason (no assertion below `CertificateException`, by design). | MET WITH DEVIATION 15 and 13 |
| AC-11 | `reorder` (by reflection) with the 16 blocked names plus two known and one unknown cipher removes every blocked name and keeps the three; the same for protocols `{SSLv3, TLSv1.2, X}`. It must fail if a blocked cipher is removed, `SSLv3` is unblocked, or the removal loop is deleted. | Read and ran it; read M-D1, M-D2 and M-D3. | `DefaultTrustedSocketFactoryHardeningTest.java:178-201`. PASS. M-D1, M-D2 and M-D3 each fail it. | MET |
| AC-12 | `reorder` puts known ciphers first in the literal order, then unknowns in their original order; protocols `{TLSv1, X, TLSv1.2, Y, TLSv1.1}` give `TLSv1.2, TLSv1.1, TLSv1, X, Y`. It must fail if two known entries are swapped or unknowns are put first or dropped. | Read and ran it; read M-D4, M-D5, M-D6a and M-D6b. | `HardeningTest.java:208-224`. The unknowns are interleaved with the knowns, which is stricter than "then U1, U2". The sample uses known entries 1, 2, 20 and 24, so a swap of other entries is caught by AC-14, not AC-12. PASS. All four mutations fail it. | MET |
| AC-13 | A null blocklist keeps every item; empty input gives an empty array. It must fail if the null guard is removed or empty input is mishandled. | Read and ran it; read M-D7 and M-D15. | `HardeningTest.java:229-238`. PASS. M-D7 (NPE) and M-D15 (null result) each fail only this test. | MET |
| AC-14 | The four lists equal literal arrays in the test: 24 known ciphers, 16 blocked, `{TLSv1.2, TLSv1.1, TLSv1}`, `{SSLv3}`. It must fail on any add, remove, reorder or rename. | Compared the arrays line by line with `DefaultTrustedSocketFactory.java:37-89`; ran it; read the cipher and protocol list mutations. | `HardeningTest.java:99-147, 244-261`. The literals equal production entry for entry; the passing `containsExactlyElementsIn(...).inOrder()` proves it. Fails under M-D1, M-D2, M-D4, M-D5, M-D12 and M-D14. | MET |
| AC-15 | The factory socket's enabled suites: (i) as a set, equal the provider defaults minus the literal blocked names; (ii) the known suites present are in literal order; (iii) every known suite precedes every unknown. It must fail if `hardenSocket` (`:176`) or `setEnabledCipherSuites` (`:184`) is removed. | Read it against the probe's cipher list; ran it; read M-D8 and M-D9 and the fix-cycle diff. | `HardeningTest.java:270-328`. (ii) and (iii) run on the list without the provider-fixed suites F, which are measured live (`:496-517`). The bounds on F (`:283-291`), at least 2 known (`:300-302`) and the new at-least-1-unknown precondition (`:303-305`, `evidence/FIX-hardening.diff.txt`) stop a vacuous pass. PASS. M-D8 and M-D9 fail it. | MET WITH DEVIATION 7. Justified: the story says "No test below asserts a property the code does not have", and the factory cannot reorder Conscrypt's fixed TLS 1.3 suites (`I0-probe.txt:30`). Bounded so it cannot pass empty. Orchestrator acceptance after the plan-review gate. |
| AC-16 | Test 1: `SSLv3` not enabled; the enabled set equals the provider's **supported** set minus `SSLv3`; `TLSv1.2` precedes `TLSv1.1` precedes `TLSv1`, and all precede others. Test 2: `TLSv1` and `TLSv1.1` are enabled if and only if supported. It must fail on supported-to-enabled (`:107`), on `TLSv1` or `TLSv1.1` blocked, or on `setEnabledProtocols` (`:187`) removed. If the provider already enables both, report it rather than claim detection. | Read all four tests (two default-provider, two JDK forms); ran them; read M-D2, M-D5, M-D8, M-D11, M-D12 and M-D13. | Default provider: `HardeningTest.java:335-374, 392-414`. JDK forms: `JdkProviderTest.java:211-260` (precondition A1-P1 `:223-226`) and `:270-298` (A1-P2 `:278-283`). All PASS. M-D11 fails both JDK tests and leaves the default-provider tests green (`NOTED-AC16`, story :315, which is the reporting the AC itself prescribes). M-D12 (TLSv1 blocked) fails all four. M-D13 fails test 1 in both classes. | MET WITH DEVIATION 14. Justified: under Conscrypt supported equals enabled (`I0-probe.txt:26-27`), so the `:107` clause is only falsifiable on SunJSSE; product owner decision. The NOTED-AC16 status is the exception AC-16 grants. |
| AC-17 | Over a plain loopback connection (port 0, one accept, no TLS), `createSocket(plain, "imap.right.test", port, null)` gives an `SSLSocket` whose enabled suites and protocols equal the implicit socket's, in order. It must fail if hardening runs only when `socket == null`. | Read and ran it; read M-D10. | `HardeningTest.java:421-448`. PASS. M-D10 fails only this test. It stays green under M-D8 (both sockets unhardened); AC-15 and AC-16 cover that case. | MET |
| AC-18 | Pinned factory, implicit and layered: enabled suites and protocols `Arrays.equals` those of a fresh default `SSLContext("TLS")` socket. It must fail if a call that changes them is added to `PinnedCertificateSocketFactory.createSocket`. | Read and ran them; read M-E1. | `PinnedCertificateSocketFactoryCreateSocketTest.java:123-154` (Truth array equality). PASS. M-E1 (adds `setEnabledProtocols`) fails both. The cipher-call variant was not run (Issue 7). | MET |
| AC-19 | The pinned implicit socket for `imap.right.test` reports server names null or empty (implicit path only). It must fail if `createSocket` sets a server name. | Read the moved test; ran it; read M-E2 (NOT DETECTED, 2026-11-17), M-E2-jdk and the move diffs. | `JdkProviderTest.java:311-324`, as the as-built note says (story :120). PASS. M-E2 left the old Conscrypt-mode test green (0 of 5), and the rule marked AC-19 unmet (ML :544). After the move, M-E2-jdk fails exactly this test, 1 of 9. The move diffs change only the AC-19 test and its Javadoc (`evidence/I6c-ac19-move-*.diff.txt`). | MET WITH DEVIATION 16. Justified: AC-19 says "in contrast to AC-4", and AC-4's observable exists only on SunJSSE. Product owner decision (story :325). The control is judged under Test Quality T6. |
| AC-20 | Pinned factory with the `other-host` leaf enrolled; the server presents that leaf; the client asks for `imap.right.test`; the handshake completes and a byte is read. No host-name verifier is installed. It must fail if host-name verification is added to the pinned path. | Read and ran it; read M-E3 and M-E4; **re-demonstrated M-E4**. | `PinnedTest.java:160-173`. The factory is built with host `imap.right.test` (`:203-206`). No verifier is touched. PASS. M-E3 fails only this test. My M-E4 re-run failed it with `SSLHandshakeException`. | MET WITH DEVIATION 13 |
| AC-21 | Enrolled `other-host`; the server presents the `right-host` leaf; `startHandshake()` throws `SSLException` with direct cause `CertificateException` whose message contains "Pinned certificate mismatch". It must fail if the fingerprint comparison (`:141-145`) is removed or inverted, or the manager is accept-all. | Read and ran it; read M-E4 and M-E5; **re-demonstrated M-E4**. | `PinnedTest.java:180-196`. PASS. ML M-E4 and my re-run: fails ("handshake completed"). M-E5 (accept-all) fails it. | MET WITH DEVIATION 13 |
| AC-22 | Each new test class opens with a Javadoc stating what it can prove, what it cannot (from the story's desktop-JVM list), and the mutation that must break each test. The helper and fixture classes say they are test-only and their keys throwaway. | Read every Javadoc. | Handshake `:47-127`, Hardening `:28-91`, JDK-provider `:37-105` and pinned `:32-99` each carry prove / cannot / mutations and the "After the CR-009 move" paragraph. `TlsTestFixtures.java:21-30` ("TEST-ONLY", "keys are throwaway", "CA private keys were deleted"). `LoopbackTlsTestServer.java:23-24` ("TEST-ONLY"; it holds no keys). The SNI additions (not a new class) carry the same three statements in a comment block (`SniTest.java:242-269`). | MET |
| AC-23 | The story fixes no defect. Safe behaviour stays asserted; unsafe behaviour gets `@Ignore("<reason, bug id>")`. Either way a bug is filed and its id written in the Implementation Notes. At least the TLS 1.0/1.1 bug is expected (DP-2 Option A by default). | Checked that BUG-019 exists, that the two legacy tests name it, that the notes carry the id, that no `@Ignore` was added, and that production is unchanged. | `sdlc/artifacts/stories/BUG-019-factory-enables-tls10-and-tls11.md` exists (`status: ready`, severity medium, related U-063). It is **uncommitted** by design. Named at `HardeningTest.java:376` and `JdkProviderTest.java:268`. Story :323 records the id and Option A. `@Ignore` in the U-063 diff: 0. Production diff: empty. | MET |
| AC-24 | If the I0 probe shows that the loopback handshake controls cannot pass, or would need a production change or a dependency, stop, record and split. Otherwise no split. | Read the probe and diagnostic records, the story notes and the log; checked that production and dependencies are unchanged. | The probe stopped on four rows (story :310). The diagnostic I0c found a test-harness cause; a server-side harness change (deviation 13) made all three controls pass with no production change and no dependency (DP-1 not triggered, :318). The rule therefore did not fire. The two observability gaps outside AC-24's trigger were settled by product-owner decisions instead of a split (:317, :325). | MET |
| IC-1 | Before any edit, the evidence holds the `@Test` count (682), the Gradle totals (682/0/2), the `mail` and `mail.transport` LINE figures, and both dependency trees. | Read the I0 files and their timestamps. | `I0-count.txt` (682), `I0-junit-totals.txt` (682, 0, 0, 2), `I0-jacoco-packages.txt` (mail 531/743, transport 241/287), `I0-deps-*.txt` (408 and 489 lines). All written 2026-11-17 20:48-20:52Z, before `I1-*` (21:45Z onward). On recorded evidence. | MET |
| IC-2 | `git diff --name-only <base>..HEAD` lists only `app/src/test/` and the story's own `sdlc/` files. The diff of production, build, `gradle` and `verification-metadata.xml` is empty. The only existing test modified is the SNI test, additions only. | Ran the literal commands. | 164 paths: 7 under `app/src/test/`, 156 under `.../sprint-020/U-063/`, 1 story file. `git diff --stat <base>..HEAD -- app/src/main k9mail-vendored build.gradle app/build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat` is empty, and so is `verification-metadata.xml`. Name-status: 6 `A` and 1 `M` (SNI, numstat `63 0`, a single hunk after the last existing method). `PinnedCertificateSocketFactoryTest.java` diff is empty. No `@Ignore` added. | MET |
| IC-3 | On JDK 25, without `MSYS_NO_PATHCONV`, `:app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease` exits 0 at I0 and at I7. | Ran the gate set once on the committed tree; read `I0-gates.txt` and `I7-gates.txt`. | My run: exit 0, `BUILD SUCCESSFUL in 1m 37s`, `U063-DAEMON-JVM: java.version=25.0.5.1`, 710/0/0/2. Recorded: I0 exit 0 (`BUILD SUCCESSFUL in 2m 40s`), I7 exit 0 (`3m 12s`), fix-cycle exit 0. | MET |
| IC-4 | The HEAD count rises from 682 to the planned total, Gradle shows it with 0 failures, and skipped stays 2 plus any AC-23(b) ignores. | Ran the CLAUDE.md count at the base and HEAD, counted per file, and summed my gate XML. | Base 682, HEAD 710. Per file: SNI 10 to 12, JDK-provider 5, Hardening 9, Handshake 8, pinned create-socket 4; 2+5+9+8+4 = 28; 682+28 = 710. Gradle (my run): 710 tests, 0 failures, 0 errors, 2 skipped (`AppTest#shouldGetVersionName`, `AuthPreferencesTest#u012_robolectricSmoke_...`). No U-063 test ignored. | MET WITH DEVIATION 14 and 16 (amendment A1): 710 instead of 708, because the two JDK forms of AC-16 are added. Justified as AC-16; product owner decisions. |
| IC-5 | No JaCoCo limit changes (`app/build.gradle` untouched). The `mail` and `mail.transport` LINE figures are not lower than I0. Every new class has `Test` in its simple name. | `git diff` of `app/build.gradle`; read the report on disk; listed the compiled new classes; checked the filter. | `app/build.gradle` diff empty; the filter `'**/*Test*.*'` is present (`app/build.gradle:237`). `jacocoTestCoverageVerification` passed in my run. Report on disk (fix-cycle gate, same blobs as HEAD): mail 538/743 (I0 531), transport 241/287 (I0 241). New compiled classes: `TlsTestFixtures`, `LoopbackTlsTestServer`, `LoopbackTlsTestServer$Mode` and the four test classes, all containing `Test`. | MET (the figures come from the fix-cycle report, not regenerated by my run; Issue 6) |
| IC-6 | No network or name resolution; loopback address object; port 0; no sleep or polling; every blocking call bounded to at most 10 s; daemon threads; sockets closed. The grep shows no hit in new files, and three consecutive runs give identical results. | Ran the literal grep at HEAD; read the sources; read `I7-ic6-*`; compared my two runs. | Grep: 2 hits, both comment lines in the existing `transport/K9MailTransportCreateOpenTest.java:265,267`; 0 in the U-063 files. `TIMEOUT_MILLIS = 10_000` (`LoopbackTlsTestServer.java:66`) is applied to accept, reads, connect and `Future.get` (`:101, 133, 168-169, 191`). Daemon thread at `:81-85`. Sockets closed in `finally` or `@After` in all four classes. Recorded: three `--rerun` runs of 38 cases, identical (`I7-ic6-diff.txt`). Mine: the same 46 cases passed in my gate and in my post-restore run. | MET WITH DEVIATION 5 (`--rerun` for `--rerun-tasks`). Justified: same test-task evidence without recompiling every module; orchestrator. |
| IC-7 | For every "Breaks when" mutation of AC-1 to AC-21: apply, run the group's class, record (file, line, edit, failing tests), revert. Every mutation must fail at least one named test; an undetected one is a finding and its AC unmet. After I6 the production diff is empty. | Full audit of ML and `mutations/` (next section); re-demonstrated two mutations; verified the production hashes now. | 39 entries, 39 `restored` lines (all "hash-ok porcelain-empty"), and all 39 case files agree with the summary table. Every AC has at least one DETECTED mutation per clause (map below). Production blobs equal the base: `d5439271`, `3b448058`, `8a0fbb64`. | MET WITH DEVIATION 15 (replacement mutations) and 16 (AC-19 move after M-E2 not detected). Both justified below. |
| IC-8 | No e-mail address, machine host name, Windows user name or user-profile path in any committed or evidence file. The literal grep over `git diff <base>..HEAD` and the evidence folder shows nothing outside the reserved test names. Generic certificate subjects; filtered Gradle logs. | Ran the literal grep over the diff and the evidence folder; added counts for this machine's host name, user name, short-path form, drive-letter paths and Gradle cache paths (values not written here). | Literal grep over the diff: 3 matches, all classified below as text that describes the pattern itself. None is a real identifier. Evidence folder: 0. Host name 0, user name 0, short-path 0, `C:/Users` or `C:/Code` 0, `.gradle/caches` 0. Subjects are `CN=U-063 Test ... CA` and `CN=imap.*.test, O=U-063 Test Only`. | MET |
| IC-9 | Tests that replace JVM-global state restore it in `@After` and assert the same instance. The full run passes with the new classes first and the SNI test last, and in default order. | Read `setUp` and `tearDown`; my default-order gate; read `I7-ic9*.txt` and the suite sources. | `HandshakeTest.java:148-200`: the trust manager is restored in `finally` with `isSameInstanceAs`, the verifier is asserted unchanged, and leak guards run at start. The pinned and JDK classes change no global state. My default-order full run passed. Recorded: forward and reverse 7-child suites, 93 of 93 each. | MET WITH DEVIATION 9 (ordered throwaway suites instead of `--tests`). Justified: `--tests` does not fix order. The ordered runs rest on recorded evidence; my rules forbid writing a suite file. |
| IC-10 | The 10 existing SNI methods and 8 existing pinned tests still pass, are not edited, and are not duplicated (Group E does not call `getTrustManagerForTesting()`). | Diff, my gate, grep. | SNI: additions only, one hunk. Pinned test: empty diff. All 18 pass in my gate and my post-restore run. `getTrustManagerForTesting` in the U-063 files: 0 code calls. | MET |
| IC-11 | A throwaway probe records: provider; default verifier class and its result for a matching SAN; `SDK_INT`; supported and default-enabled protocols; whether a loopback handshake works; any BouncyCastle or Conscrypt artifact in the compile classpath. | Read `I0-probe.txt`, `I0-deps-check.txt` and story :310-318; checked that no probe source is committed. | `prov_sslcontext_tls=Conscrypt` (:59); `hv_default_class=...AndroidTestEnvironment$1`, `hv_right_on_right=true`, `hv_right_on_other=false` (:16-18); `sdk_int=29` (:56); supported = enabled = TLS 1.0 to 1.3 (:26-27); handshakes TLSv1.3 (:12). `bcprov-jdk18on:1.80` is in the compile tree. The probe is not in the commit. My runs corroborate the provider split (guards A1-G1 and A1-G2 pass) and that handshakes work. On recorded evidence. | MET |
| IC-12 | `git grep -nE "LocalKeyStore\|setKeyStoreLocation\|addCertificate" -- app/src/main` returns nothing, before and after. | Ran it at the base and HEAD. | 0 and 0. | MET |
| IC-13 | The fixture Javadoc records the `keytool` commands, subjects, validity dates and SHA-256 fingerprints. Valid fixtures start on or before 2025-01-01 and end after 2100-01-01; `expired-host` ends before 2025-01-01. No CA private key; keys labelled throwaway. | Decoded all constants independently; compared them with the Javadoc; paired each key with each certificate; searched the commit for other key material. | `TlsTestFixtures.java:34-91`. All six fingerprints match my SHA-256 of the DER. CAs 2019-12-01 to 2119-11-07 (CA:TRUE, pathlen 0); right, other and untrusted leaves 2020-01-01 to 2107-08-13; expired 2020-01-01 to 2020-01-31. Four keys, each matching its own leaf and neither CA. PEM markers in the diff: 0; key or cert files: 0; Base64 DER only in `TlsTestFixtures.java`. | MET |

## My Gate Run

- **Preparation.** I read the init script `evidence/tools/junit-xml.init.gradle` (it enables JUnit XML on `Test` tasks and prints the daemon JVM) and the tools `junit_totals.py` and `junit_cases.py` (read-only XML summarisers). I removed `app/build/test-results` before the run. The production and build paths had empty porcelain beforehand.
- **Command.** `./gradlew -I <init> :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease`, foreground, ten-minute timeout, no `MSYS_NO_PATHCONV`. Started 18:01:59 and finished 18:03:37 local; it was not moved to the background.
- **Result.** Exit 0, `BUILD SUCCESSFUL in 1m 37s`, `U063-DAEMON-JVM: java.version=25.0.5.1`. `testDebugUnitTest`, `jacocoTestCoverageVerification`, `lintVitalRelease`, `extractReleaseVersionControlInfo` and `packageRelease` executed; the rest were up to date.
- **Totals from this run's XML only:** `files=77 tests=710 failures=0 errors=0 skipped=2`.
- **Per class:**

  | Class | Expected | Measured |
  |-------|----------|----------|
  | `DefaultTrustedSocketFactorySniTest` | 12 | 12 pass |
  | `DefaultTrustedSocketFactoryJdkProviderTest` | 5 | 5 pass |
  | `DefaultTrustedSocketFactoryHardeningTest` | 9 | 9 pass |
  | `DefaultTrustedSocketFactoryHandshakeTest` | 8 | 8 pass |
  | `PinnedCertificateSocketFactoryCreateSocketTest` | 4 | 4 pass |
  | `PinnedCertificateSocketFactoryTest` (existing) | 8 | 8 pass |

  All expected figures are confirmed.
- **Targeted runs (3 of the 3 allowed).**
  - M-C1r re-demonstration: Handshake, 8 cases, 4 fail.
  - M-E4 re-demonstration: pinned create-socket, 4 cases, 2 fail.
  - Post-restore check: the six TLS classes plus the existing pinned test, exit 0, 46 of 46 pass.
- **APKs.**
  - The debug APK SHA-256 equals the I0 baseline (`a2a237d6...e4dc`, `evidence/I7-apk-before.txt`).
  - The release APK has the same size (1995017 bytes) but a different hash from that baseline. The cause is that AGP embeds the git revision: `META-INF/version-control-info.textproto` holds `revision: "5a5321d437c5..."`, and HEAD moved from the base commit to the story commit. This involves no code change. The log's "byte-identical" statement was true at I7, when HEAD was still the base commit (Issue 5).

## Literal Integrity Checks

**IC-2.**
- `git diff --name-only 395000956b..HEAD` lists 164 paths, which `git show --stat HEAD` confirms (one commit, parent equals the base).
- 7 are under `app/src/test/java/com/zegoggles/smssync/mail/` (6 added, 1 modified), 1 is the story file and 156 are under `sdlc/artifacts/build/sprints/sprint-020/U-063/`. There are no others.
- Every workspace file on disk is committed except `compiled-pipeline.json`, which `git status --ignored` shows as ignored (listed in the local exclude file).
- Not in the commit, as the brief said: the sprint plan, `execution-plan.json`, BUG-019, the CR-009 line (`+  - U-063`), the audit log and the skill-context file.

**IC-4.** `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"` gives 710; at the base it gives 682. The arithmetic is under IC-4 above. Every `@Test` added by the diff is an annotation line; none is in a comment.

**IC-8.** The literal pattern over `git diff <base>..HEAD` has 3 matches, each classified:

| # | Diff line | File | Text matched | Classification |
|---|-----------|------|--------------|----------------|
| 1 | 2411 | Story, IC-8 criterion | `@[a-z0-9-]+\.[a-z]{2,}`, `Users[/\\\\]`, `AppData` inside the criterion's own grep command | Self-description of the pattern. Not an address, host, user or path. Benign (plan note 7). |
| 2 | 12770 | `review-code.md`, finding W4 | `` `Users[/\\]` ``, `` `AppData` `` quoted as pattern names | Self-description of the pattern. Benign. |
| 3 | 12771 | `review-code.md`, finding W4 | the word `AppData` in "no e-mail address, user name or `AppData` in the six sources" | Self-description. Benign. |

- **Evidence folder on disk:** 0 matches.
- **Workspace on disk:** besides `review-code.md`, the only file that matches is `compiled-pipeline.json`, which holds real plugin-cache paths. It is ignored and not committed (code-review W4).
- **Extra counts over the diff** (the story's pattern cannot catch these): machine host name 0, Windows user name 0, first name 0, account domain 0, short (8.3) user-path form 0, drive-letter `Users` or `Code` paths 0, `.gradle/caches` 0. No match is a real e-mail address, host name, user name or user-profile path. The criterion is met.

**IC-1, IC-10 (production untouched).**
- `git diff --stat <base>..HEAD -- app/src/main k9mail-vendored build.gradle app/build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat` is empty. The working tree on those paths is clean before and after my work.
- The SNI test diff is one insertion hunk (`@@ -238,6 +238,69 @@`, 63 added, 0 deleted). The existing pinned test is unchanged.
- The committed blobs of the seven test files equal `evidence/FIX-manifest.txt` hash for hash, so what the reviews' delta re-checks examined is what was committed.

**IC-6 and IC-12** are under their rows above.

## Mutation-Evidence Audit (IC-7)

**Log against itself.**
- 39 `### ... APPLYING` entries, 39 `verdict:` lines, 39 `restored:` lines, all reading "hash-ok porcelain-empty".
- Verdict totals: DETECTED 34, BY-DESIGN 2, COMPILE_FAIL 2, NOT DETECTED 1. This equals the summary at ML :11 and the story note at :326.
- A script compared each summary-table row ("k of n") with its case file under `evidence/mutations/`: 39 rows, 0 mismatches. In every case file the failing tests are exactly the ones the entry names.
- For nine entries spread over 2026-11-17 and 2026-11-20, the case-file modification times fall 9 to 20 seconds before the logged restore time.

**"Breaks when" clauses mapped to a demonstrating mutation** (named test failed, others passed, per case file):

| AC | Clause and mutation |
|----|---------------------|
| AC-1 | Both layers removed: M-A1 (AC-1 fails; existing IP and blank tests also fail, recorded) |
| AC-2 | Branch removed: M-A2a. Narrowed to `::`: M-A2b. Only AC-2 fails in each. |
| AC-3 | Name for an IP literal: M-A3. Both layers removed: M-A1. |
| AC-4 | `setSniHost` removed: M-A4 (and the M-A4 pre-check). Only AC-4 fails. |
| AC-5 | Wrong host: M-B3. Empty peer certificates: M-B4. |
| AC-6 | `verifyHostname` removed: M-B1r. Accept-all: M-B2. |
| AC-7 | As AC-5, plus layered branch returns the plain socket: M-B5. |
| AC-8 | As AC-6: M-B1r, M-B2 (also M-B5). |
| AC-9 | `checkServerTrusted` removed: M-C1r (re-demonstrated by me). Fallback accepts: M-C2. |
| AC-10 | As AC-9: M-C1r, M-C2. |
| AC-11 | Blocked cipher removed: M-D1. `SSLv3` unblocked: M-D2. Loop deleted: M-D3. |
| AC-12 | Swap: M-D4 and M-D5. Unknowns first: M-D6a. Unknowns dropped: M-D6b. |
| AC-13 | Guard removed: M-D7. Empty input: M-D15. |
| AC-14 | Any list edit: M-D1, M-D2, M-D4, M-D5, M-D12, M-D14. |
| AC-15 | `hardenSocket` removed: M-D8. `setEnabledCipherSuites` removed: M-D9. |
| AC-16 | `:107` changed: M-D11 (both JDK tests). `TLSv1` blocked: M-D12. `setEnabledProtocols` removed: M-D13. |
| AC-17 | Layered skip: M-D10 (only AC-17 fails). |
| AC-18 | Call added: M-E1 (both). |
| AC-19 | Name set: M-E2-jdk (only AC-19 fails). |
| AC-20 | Host check added: M-E3. |
| AC-21 | Inverted: M-E4 (re-demonstrated by me). Accept-all: M-E5. |

Variants named with "or" that were not run separately: `TLSv1.1` blocked (AC-16), a cipher call added to the pinned factory (AC-18), and the fingerprint comparison removed rather than inverted (AC-21). Each takes the same code path as a run variant, and removal is covered in effect by M-E5. I judge them covered (Issue 7).

**Judgements under the plan's Criterion Status Rule** (`plan.md:515-535`):
- **M-A1g and M-A1c (BY-DESIGN).** Rule 1 reserves BY-DESIGN for exactly these two, and AC-1's own text says one layer alone does not break AC-1. Correctly classified. Consequence, stated plainly: with the catch removed (M-A1c), all 16 SNI and JDK-provider tests pass, so the catch at `:248` is protected by no test (code-review W1, deferred). This is a gap the story accepted, not a hidden failure.
- **M-D11 with NOTED-AC16.** Rule 3 allows NOTED-AC16 when D7c holds (Conscrypt enables TLS 1.0 and 1.1, `I0-probe.txt:27`) and M-D11 is still detected by the first AC-16 test. The JDK form detects it. Correct. M-D12 was detected by the default-provider second test as well, and the log does not mislabel it (ML :369).
- **M-B1 and M-C1 (COMPILE_FAIL) and their replacements.** Both case files record `files=0` and the compiler error. Rule 1 says re-edit and re-run. The orchestrator's `if (false)` replacements keep the statement for exception analysis and never execute it, which has the same meaning as deletion. They fail exactly the intended tests (2 and 4). The ruling is recorded (ML :494; story :324). Valid.
- **M-E2 (NOT DETECTED, 2026-11-17) and M-E2-jdk (DETECTED, 2026-11-20).**
  - Rule 2 was applied: AC-19 was declared unmet and work stopped (ML :544).
  - The product owner then moved the test (deviation 16). The re-run uses the identical edit (ML :557 against :538), and only AC-19 fails.
  - The 2026-11-17 entry is kept, not relabelled.
  - Earlier mutation runs used the pre-move classes. The move diffs show only the AC-19 method and its Javadoc changed, so those verdicts stand.
- **Fix-cycle edits after the mutations.** One added precondition (it can only add failures) and one removed impossible condition in a guard. Neither can turn a recorded DETECTED into a pass. I agree with both reviewers' delta re-checks.

**My spot checks.** Both followed the protocol, one file at a time. I have no Edit tool, so each single-line edit was made with an anchored `sed` substitution on the exact line the log names. I inspected `git diff` before running (one line changed).
1. **M-C1r** in `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/TrustManagerFactory.java`.
   - Hash before: `3b448058a393c5455ba9524f4b1a5fe9e2926c88` (equals HEAD and the base).
   - Edit, line 76: `defaultTrustManager.checkServerTrusted(chain, authType);` became `if (false) defaultTrustManager.checkServerTrusted(chain, authType);`. Applied 2026-11-21T00:08:44Z.
   - Run: `--tests "...DefaultTrustedSocketFactoryHandshakeTest"`, exit 1, 8 cases, 4 fail: `implicitTls_untrustedChain_rejected`, `startTls_untrustedChain_rejected`, `implicitTls_expiredChain_rejected`, `startTls_expiredChain_rejected`, each `AssertionError: the handshake must be rejected, but it completed`. The two controls and the two other-host tests pass. This matches ML :509-520 exactly.
   - Restored in the same shell call with `git checkout -- <that file>` at 00:09:38Z. Hash `3b448058...` equals the recorded one; `git status --porcelain -- app/src/main k9mail-vendored` is empty; CRLF line endings are back.
   - `:k9mail-vendored:compileDebugJavaWithJavac` then re-executed from the restored source.
2. **M-E4** in `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java`.
   - Hash before: `8a0fbb64fb171624591fbebafb818555f28efd58`.
   - Edit, line 141: `if (!Arrays.equals(...))` became `if (Arrays.equals(...))`. Applied 00:10:11Z.
   - Run: `--tests "...PinnedCertificateSocketFactoryCreateSocketTest"`, exit 1, 4 cases, 2 fail: `handshake_enrolledCertificateForDifferentHostName_isAccepted` (`SSLHandshakeException`) and `handshake_certificateOtherThanEnrolled_isRejected` (`AssertionError`). Both AC-18 tests pass. This matches ML :580-591 exactly.
   - Restored at 00:10:58Z. Hash `8a0fbb64...` matches; porcelain empty; CRLF back.
   - `:app:compileDebugJavaWithJavac :app:transformDebugClassesWithAsm` then executed. The debug class files are dated after the restore; the release class files predate my mutations, and no assemble task ran while a mutation was in place.
- **After both:** all three production files equal their base blobs, `git status --porcelain` matches its state when I started, and the post-restore targeted run passes 46 of 46.

## Test-Quality Readings (independent of the reviews)

| # | Test | Could it pass for the wrong reason, or with the protection absent? |
|---|------|------------------------------------------------------------------|
| T1 | `implicitTls_otherHost_rejectedByHostnameCheck` (rejection shape 1) | No. The marker text is produced only by `TrustManagerFactory.verifyHostname` (`:147-148`) and is required present. A chain-validation rejection would lack it and fail. A completed handshake reaches `fail()`, whose `AssertionError` escapes the `catch (SSLException)`. A rejection from some unrelated global cause (for example a verifier that rejects everything) is excluded by the control T3 passing in the same class. |
| T2 | `startTls_untrustedChain_rejected` (rejection shape 2) | No. The marker is required absent, and the direct cause must be `CertificateChainException`, which only `:92` throws. With chain validation off (M-C1r, which I re-ran) it fails. It correctly does not depend on the host-name check (the right host is used). Limit: as in AC-10, nothing below `CertificateException` is asserted, so the test does not show *why* the chain failed. My decode shows the untrusted CA as the leaf's only defect. |
| T3 | `implicitTls_rightHost_handshakeCompletes` (control) | By design it passes with every check absent (accept-all). Its job is to fail when a valid chain is wrongly rejected (M-B3, M-B4), and it does. It asserts the byte read **and** server `OK`, so a half-completed handshake cannot pass. |
| T4 | `createSocket_enabledCipherSuites_areProviderDefaultsMinusBlockedWithKnownFirst` (provider-fixed suites F and the new precondition) | No longer vacuous. F is measured live and bounded: no F member may be a baseline name or contain `_WITH_`; at least 2 known and at least 1 unknown suite must remain. Without hardening the Conscrypt default order puts `..._ECDSA_..._128_GCM` before `..._256_GCM` (`I0-probe.txt:30`), so (ii) fails (M-D8 confirmed). Blocked-suite removal cannot be seen here, because the provider offers none of them; AC-11 covers it with synthetic input, which the story's Group D table admits. |
| T5a | `createSocket_legacyProtocols_remainEnabled_whenProviderSupportsThem` (Conscrypt form of AC-16) | Yes, by the story's own admission. Under Conscrypt it passes whether or not the factory touches TLS 1.0 and 1.1 (NOTED-AC16; M-D11 leaves it green). Its Javadoc says so (`HardeningTest.java:382-390`). It is a record, not a protection. |
| T5b | `createSocket_legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff` (JDK form) | No. Precondition A1-P2 asserts that the provider supports TLS 1.0 and 1.1 and leaves them off, then that the factory turns them on. Without the factory's `:107` choice or `setEnabledProtocols` it fails (M-D11, M-D13, M-D8). If a future JDK stops supporting TLS 1.0 and 1.1, A1-P2 fails loudly rather than passing. |
| T6 | `pinnedFactory_createSocket_implicit_setsNoServerNames_jdkProvider` (moved AC-19) and its control | An absence assertion. On its own it would be vacuous if the observable could never report a name, as under Conscrypt. **Is the AC-4 control sufficient?** AC-4 tests the other factory, but both factories obtain their socket from `SSLContext.getInstance("TLS").getSocketFactory().createSocket()` on the same provider, so it is the same socket class and the same getter. AC-4 shows that getter reports a set name on SunJSSE in this class. The decisive proof is M-E2-jdk: the pinned factory made to set a real `SNIHostName` fails exactly this test. Together they suffice. Limit: this shows only that the pinned factory sets no name in the parameters object, not what goes on the wire (the diagnostic found Conscrypt adds a name itself). The Javadoc states this. |
| T7 | `setSniViaSSLParameters_ipv6EightGroupLiteral_doesNotThrowAndSkipsSNI` (AC-1) | Yes, for either single layer: it passes with the guard gone (catch rescues) or the catch gone (guard stops it). The story accepts this. The catch alone is protected by nothing in the suite (M-A1c: 16 of 16 pass). |
| T8 | `handshake_certificateOtherThanEnrolled_isRejected` (AC-21, pinned) | No. The presented chain is valid for the asked host and comes from the test CA, but the pinned trust manager does no path validation, so only the fingerprint can reject. The message marker "Pinned certificate mismatch" is required. Inverted comparison (my M-E4 run) and accept-all (M-E5) both fail it. |
| T9 | `handshake_enrolledCertificateForDifferentHostName_isAccepted` (AC-20, pinned) | Passes with accept-all (it is an acceptance record). It fails if any host check is added, because the factory is built with `imap.right.test` (M-E3), or if the pin logic inverts (M-E4, my run). Paired with T8, the pin is pinned both ways. |
| T10 | `createSocket_layeredOverPlainSocket_appliesSameHardening` (AC-17) | It passes if hardening disappears on **both** paths, since it compares the two factory sockets (M-D8 leaves it green). AC-15 and AC-16 catch that case. Its own target, layered-only skipping, is caught (M-D10). |

Overall, every test does what its name says, and each rejection is pinned to its cause by a marker or by its absence. There are two weak spots, both disclosed in the story or the reviews: the catch layer (T7) and the Conscrypt-form legacy test (T5a).

## Deviations Judged

| # | Deviation (story :289-325) | Decided by | Effect on criteria | My judgement |
|---|----------------------------|------------|--------------------|--------------|
| 1 | No commit per increment | Orchestrator (plan-review found no product-owner need; plan note 9 says to inform the product owner) | Process only | Justified. Commits are the orchestrator's under the product owner's standing rule. I cannot verify that the product owner was told. |
| 2 | IC-2, IC-4, IC-6 and IC-8 in working-tree forms until a commit | Orchestrator | None now: I ran the literal forms | Justified and closed. |
| 3 | I0 and I6 split into sub-increments | Orchestrator | None | Justified. |
| 4 | Fixtures generated in I0b | Orchestrator | Strengthens IC-11 (the probe used the real fixtures) | Justified. |
| 5 | IC-6 `--rerun` instead of `--rerun-tasks` | Orchestrator | Slightly weaker literal form (no recompile); same test-execution evidence | Justified. The endpoint-security constraint is a real reason. |
| 6 | Coverage verification in every increment gate | Orchestrator | Strengthens | Justified. |
| 7 | AC-15 (ii) and (iii) exclude the provider-fixed suites F | Orchestrator, after the plan-review gate | Narrows (ii) and (iii) to what the factory controls; bounded; W2 precondition added | Justified against "No test below asserts a property the code does not have". Not weakening after the fix. |
| 8 | Substitute trust manager uses `"X509"`, not `"PKIX"` | Orchestrator | None (alias; production uses `"X509"`, `TrustManagerFactory.java:106`) | Justified. |
| 9 | IC-9 orders through throwaway ordered suites | Orchestrator | Strengthens (`--tests` does not set order) | Justified. Its evidence is recorded, not re-run by me. |
| 10 | Bugs drafted by the Developer, filed by the orchestrator | Orchestrator | None (the story's Platform Work Breakdown allows "Developer or orchestrator") | Justified. |
| 11 | Fixtures as Base64 DER and PKCS#8 | Orchestrator | None (E6 allows "exported certificates and PKCS8 keys") | Justified. |
| 12 | Option B counts | Orchestrator | Not used (Option A) | Not applicable. |
| 13 | V5 loopback server: always wrap a plain socket, peer host the loopback address string | Orchestrator ruling | Server side only; the client call shapes of AC-5 to AC-10, AC-20 and AC-21 are unchanged | Justified. The story's server description is a "design hint, not a plan". All rejections were unchanged in I0c. |
| 14 | JDK-provider class for AC-3, AC-4 and the `:107` clause of AC-16 (+2 tests) | **Product owner**: "Separate class, JDK provider (Recommended)" (story :317) | Makes three otherwise vacuous or unfalsifiable checks falsifiable; count 708 to 710 | Justified and decided by the right party, since it changes test layout and counts and lifts a plan rule. The quote is the option label of a choice. The fallback the notes attach ("If the SNI observable does not work there either, AC-3 and AC-4 are dropped") is the orchestrator's text, and I cannot trace it to the product owner's own words. It was not used, so nothing rests on it. |
| 15 | M-B1 and M-C1 replaced by `if (false)` forms | Orchestrator ruling | None. The plan's rule says "re-edit it and re-run", and the ruling supplies the re-edit with identical meaning. | Justified. I re-demonstrated M-C1r. |
| 16 | AC-19 test moved to the JDK-provider class after M-E2 was not detected | **Product owner**: "Move it to the JDK-provider class (Recommended)" (story :325) | Turns a vacuous test into a falsified one (M-E2-jdk); total unchanged | Justified and decided by the right party (Rule 2 forbade relabelling). As in 14, the quote is the option label. The conditional "If M-E2 is still not detected there, the test is dropped" is orchestrator text and did not trigger. |

The third product-owner quote, "yes. add tests", which created the story, is traceable verbatim to `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md:2195` (Record 5 item 5). The notes do not overstate any of the three quotes as more than the choice made. The two option labels are recorded only in orchestrator-authored files (story notes, plan revision history). I found no separate record of the prompts.

Also recorded and not a criterion: the `expert-depth-check` gate is FAIL at warning severity (not sent back). Its accepted additions are present: probe keys `jdk_tls_disabled_algorithms` and `negotiated_protocols`; RFC 6066, 6125, 5280 and 8996 in the Javadoc; the fresh-TLS-context and TLS 1.3 statements in both handshake classes. The plan-review A1 delta is PASS with A-W1 to A-W6 binding; A-W1 (the sandbox Javadoc) is visible at `JdkProviderTest.java:55-61`.

## What Is Not Verified (by this story or by me)

- **A device.** Nothing runs on Android. The provider is Conscrypt's desktop build or SunJSSE under Robolectric at simulated API 29. What a device's TLS stack supports, enables, orders, or sends as SNI is not shown.
- **Anything below TLS 1.3.** Every handshake negotiated TLS 1.3 (`I0-probe.txt:12`). The certificate path under TLS 1.2 and session resumption are untested (security W-6).
- **The real platform trust store.** The handshake tests replace the vendored `defaultTrustManager` with a test-CA-only validator. Validation against platform roots, and behaviour when static initialisation fails (null trust manager or null hardening lists), are not exercised (security W-7).
- **Android's host-name verifier semantics.** The tests use Robolectric's verifier (`AndroidTestEnvironment$1`). Wildcards, prefix or suffix, and CN-only matching are untested (security W-4). Cross-host reuse of the per-`host:port` trust-manager cache is untested (W-5).
- **The pinned factory's expiry check** (`PinnedCertificateSocketFactory.java:149`). No test presents an expired enrolled certificate (SC-6, security W-1). The security reviewer would not let the move start without it.
- **The SNI catch layer** (`DefaultTrustedSocketFactory.java:248`). No test protects it alone (code-review W1).
- **Gmail's IMAP endpoint** and what it negotiates. Nothing connects to a real server.
- **`ImapConnection` end to end.** The tests call the factory in its two call shapes, not through `ImapConnection` (out of scope).
- **The client-certificate path** and the pre-API-24 branches.
- **By me, specifically:**
  - I did not re-run the I0 baseline, the probe, the I6c-resume baseline, the three IC-6 repeat runs, the two IC-9 ordered suites, or 37 of the 39 mutations. Those rest on recorded evidence, which I cross-checked against case files and file times.
  - I did not regenerate `jacocoTestReport`.
  - I did not check how Robolectric keys sandboxes. The A1-G1 to A1-G3 guards passed in all my runs.
  - I did not verify that the product owner was informed of deviations 1 and 10.

## Issues Found

1. **Story status differs from the brief, then changed without me.**
   - During validation the story frontmatter read `status: review`, `current_build_phase: validation`. The brief expected `in-progress`. The code-review report also returned `story_status: "review"`.
   - About a minute after I wrote this file, the story file was rewritten by something other than me (modification time 2026-11-21T00:20:36Z). `git diff` shows `status: review` to `done`, `updated_at` bumped, `resolution: done` added and `current_build_phase: validation` removed. It looks like a lifecycle hook reacting to this artifact's PASS verdict.
   - I did not make or revert this change. The working-tree story now differs from the committed one. For the orchestrator to reconcile, including whether `done` is premature before the orchestrator's own close-out.
2. **The I7 section's timestamps in the implementation log are wrong.**
   - The log gives `I7 START 2026-11-20T23:40Z`, `I7.4 START 23:58Z` and `I7.4 done 2026-11-21T00:05Z`.
   - The files those steps produced are older: `I7-ic9.txt` at 23:26Z, `I7-gates.txt` at 23:29Z and `I7-manifest.txt` at 23:31Z (modification times).
   - The fix cycle (`FIX-gates.txt` 23:53Z) and the commit (23:59:53Z) both come before the logged "I7.4 done".
   - The I0 and mutation timestamps agree with their files, and the content of I7 is consistent with the evidence. The I7 header times should not be relied on.
3. **Story text still carries pre-A1 figures in places that have no as-built note.** Verification Steps 1, 4 and 8 (story :184, :187, :191) say "14 methods", "5 pass" and "708 run". The Affected Code rows for the SNI and pinned files still say 4 and 5 tests. The as-built paragraph at :170 corrects the totals. The "Expected totals after A1" bullet (:327) sits after the "Implementation complete" bullet and says "35 mutations" (the planned ids; the log has 39 entries). These are minor and confuse no count I measured.
4. **BUG-019 is not committed** (it was excluded deliberately). AC-23 requires the bug to be filed, not committed, and it exists. Its frontmatter says `origin: qa`, although it came from the Developer's draft through the orchestrator. Its text still says the tests are "new and uncommitted" (`:56`), which is now out of date.
5. **The release APK hash changes with each commit.** This comes from AGP's embedded git revision (`META-INF/version-control-info.textproto`), not from code. Any future "APK byte-identical" check must compare at the same HEAD or ignore that entry.
6. **IC-5 figures come from the fix-cycle report.** My gate list (per the brief) did not include `jacocoTestReport`. The report on disk dates from 2026-11-20 23:53Z, the fix-cycle gate over the same test blobs. Run-to-run jitter in unrelated packages is recorded (`activity` 189 or 199; `preferences` 313 or 314).
7. **Breaks-when variants not run:** `TLSv1.1` blocked (AC-16), a cipher call added to the pinned factory (AC-18), and the fingerprint comparison removed rather than inverted (AC-21). Equivalent variants were run and detected. I judge them covered, not unmet.
8. **Deferred review findings remain open, by design:** code-review W1 (SNI catch layer) and W4 (`compiled-pipeline.json`, resolved by excluding it); security W-1 (pinned expiry), W-2 (BUG-019), W-3 (pinned factory hardening, a bug not yet filed), and W-4 to W-7.
9. **My own procedure.**
   - The two mutations were applied with an anchored single-line `sed`, because I have no Edit tool. `sed` normalised the file's line endings while mutated; `git checkout` restored them, and the hash proves it.
   - Early on, two scratch directory listings were written outside the designated scratch folder by mistake: one in the shell's `/tmp`, one in the user temp folder. Both contained only file paths of this repository. I deleted both within the same minute and verified that they were gone. Nothing was written into the repository except this file.

## Recommendations for the Retro

1. **Before the CR-009 move starts:**
   - add the expired-enrolled pinned test (security W-1) and the SNI catch-only test (code-review W1);
   - file the pinned-hardening bug (W-3);
   - take the BUG-019 decision after the device protocol measurement.

   Each of the first two needs its own mutation.
2. **Give BY-DESIGN mutation verdicts their own line in the summary saying which layer is then unprotected.** "BY-DESIGN" read alone suggests both layers are covered.
3. **Log timestamps should be taken from the clock** (`date -u`) at the moment of writing, never estimated. Then file times and log times can corroborate each other, as they do for the mutation entries.
4. **When a product-owner decision moves a test or changes a count, update every count-bearing line of the story in the same turn,** including Verification Steps and Affected Code rows, not only the AC "as built" notes.
5. **Record the product owner's options and the chosen option somewhere other than orchestrator-authored text** (for example the decision record), so that conditions attached to a choice can be traced to what was offered.
6. **Any "APK unchanged" check should strip or compare `META-INF/version-control-info.textproto` separately,** or compare at the same HEAD.
7. **The JDK-provider class depends on a Robolectric sandbox key and a JVM-wide tripwire (A1-G3).** A separate Gradle test task for it would make the isolation structural; that needs a build change and a decision.
8. **Give the QA agent an editing tool for mutation spot checks,** or plan the spot-check edit as a scripted, hash-verified substitution, so the method is not improvised.

## Phase Completion Report
---
story_id: "U-063"
phase: "validation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-020/U-063/qa-results.md"
story_status: "done"                   # as now on disk; set automatically at 2026-11-21T00:20:36Z, not by QA (it read "review" during validation; Issue 1)
current_build_phase: "validation"
ac_total: 37
ac_passed: 37
ac_failed: 0
errors: []
---
