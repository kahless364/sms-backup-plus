---
artifact_type: implementation-log
story_id: "U-065"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-11-21T05:20:00Z"
files_changed: 7
files_created: 0
tests_added: 19
tests_passing: 730
---

# Implementation Log: U-065

## Summary

Both TLS socket factories now enable only TLS 1.2 and TLS 1.3 (where the platform supports them), and the
pinned-certificate factory applies the default factory's protocol, cipher-suite and server-name handling by
calling the same two steps in the same order. The plan (`plan.md`, iteration 2) was followed step by step from
I0 to I9; step I11 (hygiene scan) is the last step and its result is in `evidence/I11-scan.txt` (see the end
of this log).

- Production: `DefaultTrustedSocketFactory.java` gets an allow-list constant `ALLOWED_PROTOCOLS`, a floor step
  `protocolsToEnable(SSLSocket)`, and `hardenSocket(SSLSocket)` made public with a protected three-argument
  overload; `PinnedCertificateSocketFactory.java` calls `hardenSocket` and `setSniHost` on both shapes.
- Tests: 19 added, 7 changed (renamed), four test classes plus the loopback server helper edited.
- Gates: before the first edit and after the last (no mutation present) both exit 0. Totals, from the JUnit
  XML written by the read-only init script: start `tests=713 failures=0 errors=0 skipped=2`, end
  `tests=732 failures=0 errors=0 skipped=2` (see AC-10; the numbers are in `evidence/I0-junit-totals.txt`
  and `evidence/I9-junit-totals.txt`).
- Mutations: 14 entries, 14 runs, every one detected as the plan predicted, every one restored and proven by
  blob hash and by the hash of the whole production diff (`evidence/mutation-log.md`).
- No decision-table row that says "stop" was reached. No deviation from the plan's text of the production or
  test code. IC-1 (emulator check) is pending for the orchestrator and was not run.

## Files Modified

Production (2):

| File | Change |
|------|--------|
| `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` | D1 to D6: `ALLOWED_PROTOCOLS`; static initialiser takes `protocolsToEnable(sock)`; new `protocolsToEnable`; `hardenSocket(SSLSocket)` public, new protected `hardenSocket(SSLSocket, String[], String[])` (null protocol list now computes the floor from the socket) |
| `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` | P1 to P4: two imports, class Javadoc, `createSocket` calls `DefaultTrustedSocketFactory.hardenSocket(sslSocket)` then `setSniHost(socketFactory, sslSocket, host)` on both shapes; lines that install the pinning trust manager are untouched |

Tests (5), all under `app/src/test/java/com/zegoggles/smssync/mail/`:

| File | Change |
|------|--------|
| `ssl/DefaultTrustedSocketFactoryHardeningTest.java` | H-imp, H-doc, constants, H1, H2 (changed), H3, H4, H5 (added), helpers; guard code unchanged |
| `ssl/DefaultTrustedSocketFactoryJdkProviderTest.java` | J-imp, J-doc, constants, J1, J2, J3 (changed), J4 to J7 (added), helpers |
| `ssl/DefaultTrustedSocketFactoryHandshakeTest.java` | Javadoc, 6 added handshake tests and 2 helpers; the eight existing tests and the guard unchanged |
| `PinnedCertificateSocketFactoryCreateSocketTest.java` | imports, Javadoc, C1, C2 (changed), 6 added handshake tests, helpers |
| `ssl/LoopbackTlsTestServer.java` | additive `start(Mode, SSLContext, String[])`; the two-argument `start` behaves as before |

Tests renamed (7 changed, none deleted, none `@Ignore`d):

| Old name | New name |
|----------|----------|
| Hardening `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder` | `createSocket_enabledProtocols_areProviderSupportedTls12AndNewer_inRecordedOrder` |
| Hardening `createSocket_legacyProtocols_remainEnabled_whenProviderSupportsThem` | `createSocket_legacyProtocols_notEnabled_althoughProviderEnablesThemByDefault` |
| JDK `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder_jdkProvider` | `createSocket_enabledProtocols_areProviderSupportedTls12AndNewer_inRecordedOrder_jdkProvider` |
| JDK `createSocket_legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff` | `createSocket_legacyProtocols_notEnabledByFactory_whereJdkProviderSupportsThem` |
| Pinned `createSocket_implicit_leavesProviderDefaultsUntouched` | `createSocket_implicit_appliesDefaultFactoryProtocolAndCipherHandling` |
| Pinned `createSocket_layered_leavesProviderDefaultsUntouched` | `createSocket_layered_appliesDefaultFactoryProtocolAndCipherHandling` |
| JDK `pinnedFactory_createSocket_implicit_setsNoServerNames_jdkProvider` | `pinnedFactory_createSocket_implicit_setsDnsHostAsOnlyServerName_jdkProvider` |

## Files Created

No file outside the story workspace. In the story workspace (`sdlc/artifacts/build/sprints/sprint-021/U-065/`):
`implementation-log.md` (this file) and `evidence/` (`I0-*`, `I1-*`, `I2-*`, `I3-*`, `I4-*`, `I5-*`, `I6-*`,
`I8-*`, `I9-*`, `I11-scan.txt`, `mutation-log.md`, `mutations/*-cases.txt`, `mutations/*-message.txt`,
`tools/U065ConscryptProbeTest.java.txt`, `tools/U065JdkProbeTest.java.txt` (the two throwaway probe classes,
kept as text only; they were deleted from `app/src/test` before I3)). No image, no raw log: raw logs and the
pristine copies of the two production files stayed in the developer's work folder outside the repository.

Tests added (19), by class:

- Hardening (3): `protocolsToEnable_givenSupportedList_keepsOnlySupportedTls12AndNewer`,
  `hardenSocket_withNullLists_stillAppliesTheProtocolFloorFromTheSocket`,
  `pinnedFactory_createSocket_enabledCipherSuites_areProviderDefaultsMinusBlockedWithKnownFirst`
- JDK provider (4): `pinnedFactory_createSocket_enablesExactlyTls12AndTls13_bothShapes_jdkProvider`,
  `pinnedFactory_createSocket_ipLiteralAndBlankHosts_setNoServerName_jdkProvider`,
  `pinnedFactory_createSocket_nameRejectedBySniHostName_doesNotThrowAndSetsNoServerName_jdkProvider`,
  `pinnedAndDefaultFactories_sameProtocolsCiphersAndServerNames_jdkProvider`
- Handshake (6): `implicitTls_serverLimitedToTls12_handshakeCompletesAtTls12`,
  `startTls_serverLimitedToTls12_handshakeCompletesAtTls12`, `implicitTls_serverLimitedToTls11_handshakeRefused`,
  `startTls_serverLimitedToTls11_handshakeRefused`, `implicitTls_serverLimitedToTls10_handshakeRefused`,
  `startTls_serverLimitedToTls10_handshakeRefused`
- Pinned create-socket (6): `handshake_implicitTls_serverLimitedToTls12_completesAtTls12`,
  `handshake_startTls_serverLimitedToTls12_completesAtTls12`, `handshake_implicitTls_serverLimitedToTls11_isRefused`,
  `handshake_startTls_serverLimitedToTls11_isRefused`, `handshake_implicitTls_serverLimitedToTls10_isRefused`,
  `handshake_startTls_serverLimitedToTls10_isRefused`

## Test Results

Counts by reference (the numbers are not restated here): see AC-10; Gradle totals in
`evidence/I9-junit-totals.txt` and `evidence/I0-junit-totals.txt`, working-tree `@Test` count in
`evidence/I9-checks.txt` (first figure: working tree; second: HEAD, unchanged until the commit). Per-case
results of the seven affected classes: `evidence/I6-green-cases.txt` (68 cases, all pass), repeated twice
(`I8-run2-cases.txt`, `I8-run3-cases.txt`; both identical to I6, `evidence/I8-checks.txt`).

## Regression Results

Full gate `:app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease`
before the first edit (`evidence/I0-gates.txt`) and after the last edit with no mutation present
(`evidence/I9-gates.txt`): both `BUILD SUCCESSFUL`, `exit=0`, daemon JVM 25 (`U063-DAEMON-JVM` line), 0
failures, 0 errors, skipped 2 at start and 2 at end. Every JaCoCo floor held (the verification task passed;
no `limit` or `minimum` value was edited). The unchanged tests named in AC-6 (d) pass unchanged, including
U-064's three tests and the three isolation guards (I6, I8, I9).

## Notes

### Step log

| Step | Clock (UTC) | What was run | Result against expected | Rows used |
|------|-------------|--------------|-------------------------|-----------|
| I0.1 | 2026-11-21T04:43:50Z | baseline evidence (`evidence/I0-baseline.txt`) | HEAD `b2c2dfd8...`, branch `sdlc/modernization-plan`, nothing before `porcelain-end`, both base blobs equal HEAD, count 713, `toolchainVersion=25`, `record9-ancestor-exit=0`, Record 9 commit `bd1d5398` (2026-11-20T21:19:21-06:00), Record 8 commit `6cc49ab4` (2026-11-20T19:36:14-06:00) | none |
| I0.2 | before 04:51 | four-task gate, init script on | `exit=0`, `java.version=25.0.5.1`, `tests=713 failures=0 errors=0 skipped=2` | none (D1, D33 not triggered) |
| I1 | before 04:51 | `LoopbackTlsTestServer` edit (section 3.7); `evidence/I1-lts-diff-check.txt` | 26 insertions, 4 deletions (constructor signature and submit line, `start` body, `serve` signature); file stays LF | none |
| I2 | 2026-11-21 (run started after the clock shown in the work folder) | one Gradle run with the two probe classes; `evidence/I2-probe.txt`, `I2-probe-cases.txt`; probe classes archived as text and deleted (`evidence/I2-cleanup.txt`: only the server helper modified, 0 probe files, production clean) | every key as expected (table below) | D2 not triggered; D3 for TLSv1.1 and TLSv1; D8, D10, D11 not triggered; D9 record only; D12 for `j_layered_0`; D14, D15 not triggered (no layered host threw); D16 not triggered |
| I3 | before 04:51 | all test edits (sections 3.3 to 3.6); `evidence/I3-test-diff-check.txt` | every hunk inside the allowed ranges; `untouched-exit=0`; working-tree count 732; `ignore-exit=1`; BUG-019 comment above the changed legacy test; `prod-clean-exit=0` | none (D17 not triggered; see deviation 2 on the `-518,0` hunk) |
| I4 | 2026-11-21T04:51:49Z | red run of the seven classes against the unchanged production code | compiled; 68 cases, exactly 19 failing (Hardening H1 to H5; JDK J1, J2, J3, J7; the four Handshake refusal tests; pinned C1, C2 and the four pinned refusal tests), the rest pass (J4, J5, J6, the four TLS 1.2 tests and every unchanged test); H3 failed with `NoSuchFieldException` and H4 with `NoSuchMethodException` as predicted | D21 |
| I5 | before 04:53 | production edits D1 to D6 and P1 to P4; `evidence/I5-production.txt`, `I5-production.diff.txt` | exactly the two production files; no hunk of the default factory touches `:37-81`, `:83-89`, `:152-180` or `:191-266` (the D1 insertion shows as `-90,0`); `hardenSocket(` appears only in the default factory (the `createSocket` call, the two definitions, the one-argument body) and once in the pinned factory | D22 not triggered |
| I6 | 2026-11-21T04:53:51Z | green run of the seven classes | `exit=0`, 68 cases, all pass, including U-064's three tests and the three guards | D26 |
| I7 | 04:55:49Z to 05:10:57Z | 14 mutations, one at a time (`evidence/mutation-log.md`) | every run matched its row in test and failure type; every entry has its `restored` line | none (D27 to D30 not triggered) |
| I8 | after 05:10:57Z | two more runs of the seven classes with `--rerun`; `evidence/I8-checks.txt` | `diff-2-exit=0`, `diff-3-exit=0`, `grep-exit=1` | D31 not triggered |
| I9 | after I8 | final gate; `evidence/I9-gates.txt`, `I9-junit-totals.txt`, `I9-checks.txt` | `exit=0`, `tests=732 failures=0 errors=0 skipped=2`; production diff hash equals `PROD_DIFF`; no `@Ignore`; build-file diffs empty; five construction sites | D32, D34 not triggered |

Blob and diff hashes of the production change (the restore targets): default factory
`43e5a78ead7c4c32a6af2734514ab8ba5d5ccbd3`, pinned factory `21cc5f345977ab6fa161b2aa6925a04aea6a1479`, hash of
the whole production diff `6d9a52e7fa59c26a9993da0611e6b50fd8266704` (`evidence/I5-production.txt`; equal at
I8, I9 and at the end of this log).

### Probe outcome per key (I2, `evidence/I2-probe.txt`)

| Key | Observed | Row |
|-----|----------|-----|
| `c_provider` | `Conscrypt` | D8 ok |
| `c_supported`, `c_default_enabled` | `TLSv1,TLSv1.1,TLSv1.2,TLSv1.3` both | D8 ok |
| `c_roundtrip_protocols`, `c_roundtrip_cipher_order_kept` | `TLSv1.2,TLSv1.3`; `true` | D10 ok (the server-name round trip keeps the floor and a reordered cipher list under Conscrypt) |
| `c_empty_protocols` | `accepted enabled_count=0` | D9, record only |
| `c_hs_TLSv1.2_client_provider_default` and `_client_floor` | `completed protocol=TLSv1.2 byte=42 server=OK` | D2 ok |
| `c_hs_TLSv1.1_client_provider_default` | `completed protocol=TLSv1.1 byte=42 server=OK` | D3 (N1: the desktop Conscrypt server serves TLS 1.1) |
| `c_hs_TLSv1.1_client_floor` | `refused type=javax.net.ssl.SSLHandshakeException server=FAILED:javax.net.ssl.SSLHandshakeException` | D3 (an `SSLException` subclass; D5, D6, D7 not triggered) |
| `c_hs_TLSv1_client_provider_default` | `completed protocol=TLSv1 byte=42 server=OK` | D3 |
| `c_hs_TLSv1_client_floor` | `refused type=javax.net.ssl.SSLHandshakeException server=FAILED:javax.net.ssl.SSLHandshakeException` | D3 |
| `j_provider`, `j_unconnected_names` | `SunJSSE`; `none` | D11, D16 ok |
| `j_layered_0` | `host='imap.right.test' names=imap.right.test` | D12 (the provider sets the layered name itself) |
| `j_layered_1` to `_5`, `_7` to `_10` | `names=none`, none threw (including the null host, `j_layered_10`) | D14, D15 not triggered |
| `j_layered_6` | `host='host.example.' names=host.example` | D16, informational: matches the plan's L12 |
| `j_empty_protocols` | `accepted enabled_count=0` | D9, record only |

### Deviations

1. **Declared in the plan (I7):** restore by copying the pristine file (not `git checkout`), and "clean
   porcelain" read as "exactly the two production files modified and the diff hash equal to `PROD_DIFF`". Used
   for every entry. Mutation entries are named `M-P1-P8`, `M-P6-i`, `M-P6-ii`, `M-P6-iii` (the plan writes
   `M-P1/M-P8`, `M-P6 (i)` and so on) so that the ids are usable as file names.
2. **Orchestrator note 1 (hunk check):** the helper insertion in the Hardening class shows as `@@ -518,0 ...`
   (an insertion after line 518, the blank line after `providerFixedSuites`). It is an insertion next to the
   allowed range, like `-90,0` in I5, and is not a D17 violation. The same reading applies to the other
   zero-length hunks (`-114,0`, `-349,0` in the JDK class; `-271,0` in the Handshake class; `-197,0`, `-214,0` in
   the pinned class).
3. **Orchestrator note 2 (M-S5, J6):** J6 listed all three names, direct and layered (the `continue` makes the
   direct entries the ones that appear first); the failing test and its type (A) are as the row states; the
   message evidence is in `evidence/mutations/M-S5-message.txt`.
4. **Edit granularity:** the Javadoc blocks that the plan replaces whole (H-doc, J-doc, the pinned class
   Javadoc) were applied as several exact `Edit` calls whose result is the plan's text, because the plan marks
   paragraphs "copied unchanged". The resulting text equals the plan's. Imports of the Hardening class were
   applied as three edits with the same result as H-imp.
5. **Scripts:** the write-ahead entry (clock from `date`), the clean check, the Gradle run, the restore and the
   hash check of each mutation were done by two small shell scripts kept in the developer's scratch folder
   (outside the repository); the entries' descriptive text, expected sets and verdicts were written into the
   mutation log with the editing tool before the mutation was applied (descriptions) or after the run (verdict).
6. **No file was altered by the line-ending rules:** the production files stay CRLF in the working tree (the
   blob in the object store is LF, as before) and the test files stay LF.

### Decision rows used

D3 (twice: TLSv1.1, TLSv1), D9 (record only, twice), D12, D21, D26. D1, D2, D4 to D8, D10, D11, D13 to D20, D22 to D25, D27 to
D39 were not triggered.

### Capabilities Inventory (pre-implementation audit; the story modifies existing code significantly)

`DefaultTrustedSocketFactory.java` (line numbers are those of the final file):

| Capability | Status |
|------------|--------|
| `ENABLED_CIPHERS` computed once from the provider's enabled suites through `reorder` (`:124-125`) | RETAINED |
| Known cipher order list (24 names) and blocked cipher list (16 names) (`:37-81`) | RETAINED, byte-identical (AC-9 (d); `cipherConstants_matchRecordedBaseline` passes unchanged) |
| `ORDERED_KNOWN_PROTOCOLS` and `BLACKLISTED_PROTOCOLS` (SSLv3 blocked) (`:83-89`) | RETAINED, byte-identical (`protocolConstants_matchRecordedBaseline` passes unchanged) |
| `ENABLED_PROTOCOLS` computed once from the provider's *supported* protocols, reordered, SSLv3 removed (`:118`, `:127`, `:172-185`) | RETAINED for the supported-list input, the block of SSLv3 and the ordering; TLS 1.0, TLS 1.1 and SSLv2Hello are INTENTIONALLY REMOVED (story AC-1; Record 8: "drop anything older than tls 1.2") |
| Initialiser failure branch: the error is logged and both lists stay null (`:119-125`) | RETAINED for the cipher list (platform suites then stand); for protocols INTENTIONALLY CHANGED: the floor is now computed from the socket in `hardenSocket` (story AC-1 (e)) |
| `reorder` (blocked removed, known first, unknown appended) (`:134-157`) | RETAINED, unchanged |
| `createSocket`: trust manager from `TrustManagerFactory.get(host, port)`, optional client-certificate key manager, a new `SSLContext` per call, direct and layered shape, then hardening and server name (`:190-216`) | RETAINED, unchanged (AC-9 (c), (d)) |
| `hardenSocket(SSLSocket)` applied both lists to every socket, a null list applied nothing (`:226-241`) | RETAINED; visibility private to public (the plan's one visibility change); a null cipher list still leaves the platform's suites; a null protocol list INTENTIONALLY CHANGED (AC-1 (e)) |
| `setSniHost` branch by API level (`:243-257`), `isIpOrBlankHostname` (`:268`), `setSniViaSSLParameters` with the IP/blank guard and the `IllegalArgumentException` safety net (`:290-306`), `setHostnameViaReflection` (`:308`) | RETAINED, unchanged (`git diff` shows no hunk inside them; U-064's tests and the 12 SNI tests pass unchanged) |

`PinnedCertificateSocketFactory.java`:

| Capability | Status |
|------------|--------|
| Constructor rejects a null enrolled certificate (`:78-82`) | RETAINED |
| A new `PinnedX509TrustManager` and a new TLS `SSLContext` per `createSocket` call (`:88-92`) | RETAINED, byte-identical |
| Direct and layered shape (`:93-97`), signatures of the constructor and `createSocket` | RETAINED (AC-9 (b)) |
| Pin comparison, validity window, empty or null chain rejected, `getAcceptedIssuers` (`:135-174`), the package-private test seam (`:113`) | RETAINED, unchanged; M-E4 and M-E5 re-run and detected |
| Returns the provider's socket untouched (provider protocols, provider suites, no server name) | INTENTIONALLY REMOVED (story AC-2 to AC-4; Records 8 and 9) |

`LoopbackTlsTestServer.java` (test-only): the two-argument `start`, the always-wrap-a-plain-socket rule, the
bounded waits and the outcome strings are RETAINED unchanged; the protocol option is additive.

No item is a REGRESSION.

### Contract Adherence

The story lists `integration_contracts: []`. `CNTR-MODERNIZATION-001` was read (by the plan's contract
verification) and is not edited. Its stated behaviour for both producers is trust behaviour only; the trust
code is unchanged: `PinnedCertificateSocketFactory.java:88-92` and `:135-174` are byte-identical, and the
trust tests pass unchanged (`handshake_enrolledCertificateForDifferentHostName_isAccepted`,
`handshake_certificateOtherThanEnrolled_isRejected`, the Expiry test, the eight `PinnedCertificateSocketFactoryTest`
tests, the eight original Handshake tests; M-E4 and M-E5 detected). No field name, type, event name or error
shape of a contract is used or changed.

### Integration path

New code is reached from existing entry points without any registration: `ALLOWED_PROTOCOLS` and
`protocolsToEnable` are called by the static initialiser of `DefaultTrustedSocketFactory` (`:118`) and by the
three-argument `hardenSocket` (`:240`); `hardenSocket(SSLSocket)` is called by
`DefaultTrustedSocketFactory.createSocket` (`:213`, unchanged) and by `PinnedCertificateSocketFactory.createSocket`
(`:101`). Both `createSocket` methods are reached from `K9MailTransport.java:145` and `:148` (unchanged), and
the default factory also from the vendored `Transport.java:26` and `RemoteStore.java:52`, `:56`
(`evidence/I9-checks.txt`, construction sites). The full gate compiled every caller and ran the test suite
(IC-2). The loopback server option is called by the 12 new handshake tests.

### Criterion-to-evidence table

| Criterion | Status | Evidence |
|-----------|--------|----------|
| AC-1 (a), (b), (c) | Met | H1 `createSocket_enabledProtocols_areProviderSupportedTls12AndNewer_inRecordedOrder`, H2 `createSocket_legacyProtocols_notEnabled_althoughProviderEnablesThemByDefault` (Conscrypt precondition: provider default enables TLSv1 and TLSv1.1; direct and layered), J1, J2 (JDK; precondition A1-P2 kept; direct and layered); red run fails them (`I4-red-cases.txt`), green passes (`I6-green-cases.txt`); M-P1/M-P8 and M-P6 (ii) detected |
| AC-1 (d) | Met | H3 `protocolsToEnable_givenSupportedList_keepsOnlySupportedTls12AndNewer` (injected supported lists, including TLS 1.2 supported but off by default); M-D11r is detected by H3 only, as the log states |
| AC-1 (e) | Met (code of the branch tested; the initialiser failure itself is not forced) | H4 `hardenSocket_withNullLists_stillAppliesTheProtocolFloorFromTheSocket`; M-F1 detected. See Implementation Notes in the story |
| AC-2 | Met | C1, C2 (Conscrypt, preconditions held), J4 (JDK exact set on both shapes); M-P2, M-P3, M-P7 detected |
| AC-3 | Met | (a) C1, C2, J7 parity in order under both providers; (b) H5 against the literal baselines; (c) the blocked list is applied by the shared step, covered by the unchanged `reorder_removesEveryBlockedItem` and `cipherConstants_matchRecordedBaseline`, and the call by M-P4 (detected by C1, C2, H5, J3, J7). `TlsTestFixtures.java` not edited |
| AC-4 | Met; (b) second branch | J3 (S1 and S6; floor and cipher order asserted on the same socket after the server-name step), J5 (S2 to S4), J6 (S5); probe `j_layered_0` shows the provider sets the layered name itself, so for the layered shape only "no exception, an `SSLSocket` is returned" is asserted (reason in the story's Implementation Notes); (c) a null host is accepted on both shapes (probe `j_layered_10`); M-P5 and M-S5 detected |
| AC-5 | Met | C1, C2, J7; M-P6 (i), (ii), (iii) all detected |
| AC-6 | Met | table below; no `@Ignore` (`I9-checks.txt`, `ignore-exit=1`); the BUG-019 comment is kept above the changed legacy test (`I3-test-diff-check.txt`) |
| AC-7 | Met | `evidence/mutation-log.md`: 14 entries, each with a write-ahead line, the failing test names, `restored`; M-E4 and M-E5 make `handshake_certificateOtherThanEnrolled_isRejected` fail. None invalid, none undetected, none mismatched |
| AC-8 | (a) Met; (b) Met under Conscrypt | (a) four tests complete at TLSv1.2 and the server reports OK; (b) probe D3 for TLSv1.1 and TLSv1: the provider-default client completes, so the refusal is the floor's doing; eight refusal tests; under M-P1/M-P8 the handshakes complete and all eight fail; under the JDK provider (b) is not asserted (disabled-algorithms) |
| AC-9 | Met | (a) `I5-production.txt` and `I9-checks.txt` list exactly the two files; (b) constructors and `createSocket` signatures are unchanged, `K9MailTransport.java` has an empty diff; (c) trust tests pass; (d) no hunk inside `:37-81`, `:83-89`, `:152-180`, `:191-266`; the visibility change is `hardenSocket`, with the protected overload and `protocolsToEnable` added next to it; (e) the build-file diffs and the verification-metadata diff are empty |
| AC-10 | Met | start `tests=713 failures=0 errors=0 skipped=2`, end `tests=732 failures=0 errors=0 skipped=2`, both `exit=0`; difference +19 equals the 19 tests added (listed above), none removed; JaCoCo gate part of both runs |
| AC-11 | Met for the records the story assigns here | (a) Record 8 in `6cc49ab4`, Record 9 in `bd1d5398`, both ancestors of the fix commit (`I0-baseline.txt`); (b) `SSLv2Hello` does not stay (Record 8); (c) bug-criterion table in the story Implementation Notes (BUG-020 AC-5 not satisfied here; AC-4 does not apply); (d) no edit to the bugs, the contract or any change record |
| IC-1 | PENDING for the orchestrator | not run by the Developer; the debug APK is named below |
| IC-2 | Met | the full gate compiled every call site; construction sites in `I9-checks.txt`: `K9MailTransport.java:145` and `:148`, `Transport.java:26`, `RemoteStore.java:52` and `:56` |

AC-6, row by row of the plan's table 3.1 (the story's table): the four protocol tests and the three pinned
tests named in AC-6 (a) are changed and renamed as listed under "Files Modified" (reasons: they pinned the old
behaviour; their preconditions are kept or strengthened). Unchanged and passing: `protocolConstants_matchRecordedBaseline`,
`reorder_removesEveryBlockedItem`, `reorder_ordersKnownItemsFirst_thenUnknownInOriginalOrder`,
`createSocket_layeredOverPlainSocket_appliesSameHardening`, the cipher rule, cipher constants and null-blocklist
tests, both `checkIsolationGuardA1G2` guards (code unchanged; the `SSLv2Hello` clause can no longer fire, the
DHE clause alone carries the guard, AC-6 (e), recorded in both class Javadocs), the two JDK server-name
tests of the default factory, the two pinned handshake tests, `PinnedCertificateSocketFactoryTest` (8),
the eight original Handshake tests, `DefaultTrustedSocketFactorySniTest` (14), `PinnedCertificateSocketFactoryExpiryTest`,
`K9MailTransportTest`, `BackupImapStoreTest`.

### Items for the orchestrator

1. **BUG-020 AC-5 and the missing record** (contract-verification O1, O7): one question with two options:
   (a) raise one change record against CNTR-MODERNIZATION-001, a sibling of CR-013 applied in the same
   combined revision, stating for both producers that no protocol older than TLS 1.2 is enabled, TLS 1.2 and
   1.3 are enabled where the platform supports them, a protocol name not in the allowed list is never
   enabled, the cipher suites are the platform's enabled suites minus the blocked list with the known suites
   first in the recorded order, and that the pinned producer applies exactly the default producer's handling;
   with a validation rule and a Versioning line saying whether such a change is a contract revision (the
   contract's backward-compatible list does not name it today; the change is backward-compatible only by
   elimination); or (b) a dated note to Record 9 (or a new record) giving the reason not to state them, which
   meets AC-5's "or the record says why not", with CR-013 `:332` reworded at the revision. Whether BUG-020
   closes with AC-5 open stays the pipeline's and the product owner's call.
2. **CR-013, second pass with alternative (b)** (contract-verification O2, O3), suggested dated note: "extend
   rule 6 to both producers; replace the Edit 4 pinned clause ('not yet stated'); name the U-065 pinned tests
   (`pinnedFactory_createSocket_implicit_setsDnsHostAsOnlyServerName_jdkProvider`,
   `pinnedFactory_createSocket_ipLiteralAndBlankHosts_setNoServerName_jdkProvider`,
   `pinnedFactory_createSocket_nameRejectedBySniHostName_doesNotThrowAndSetsNoServerName_jdkProvider`,
   `pinnedAndDefaultFactories_sameProtocolsCiphersAndServerNames_jdkProvider`); alternative (a) no longer
   describes the code (U-065)", and for the pinned subsection: "For the socket layered over a connected
   socket, the statements rest on reading the code: the test providers set a server name on that socket
   themselves, so no test can tell the factory's step from the provider's (CR-013 Scope Concern D; U-065
   probe)."
3. **CR-009** (contract-verification O4; story Scope Concern 6), suggested additions: checklist line
   "`PinnedCertificateSocketFactory.createSocket` calls `DefaultTrustedSocketFactory.hardenSocket` and
   `setSniHost` (U-065, commit `<fix>`); retarget both calls and the import to the app-owned default factory
   in the same change; the shared step may become package-private once both classes share a package"; test
   row T11 "Pinned/default parity: protocols (set and order), cipher suites (order) and server names of the
   two factories' sockets are equal per shape and provider (U-065
   `createSocket_*_appliesDefaultFactoryProtocolAndCipherHandling`,
   `pinnedAndDefaultFactories_sameProtocolsCiphersAndServerNames_jdkProvider`); exist; retarget"; T5 reworded
   "`hardenSocket`, `protocolsToEnable`, `reorder`: blocked and ordered cipher suites; protocols limited to
   `ALLOWED_PROTOCOLS` from the supported list; exist (U-063, U-065)"; and rows `:87` and `:234` ("producer 2
   unchanged") no longer describe the code.
4. **The error table** (contract-verification O6): a server limited to an old protocol makes the handshake fail
   with a non-certificate `SSLException`, which per CR-012 Scope Concern B (`:305`) leaves
   `ImapConnection.open()` as an `IOException`; a candidate row for the record of item 1 or the combined revision.
5. **The AC-8 (b) reading, if row D6 was reached:** not reached. Both old versions were refused with
   `javax.net.ssl.SSLHandshakeException`, an `SSLException` subclass; no reading is needed.
6. **IC-1 (emulator check)** belongs to the orchestrator and is pending (plan section 9). The hand-off artifact
   is the debug APK built by the I9 gate from the final tree: path `app/build/outputs/apk/debug/app-debug.apk`,
   size 6622678 bytes, SHA-256 `d34f0d58c51c392dd47ba781ce1eed2c972f7fade5810fb4f3c5e5acb38a8415` (`evidence/I9-checks.txt`).
   No mutation entry lacks `restored`. No emulator or device was started, stopped or sent a command.
7. **The `ALLOWED_PROTOCOLS` trade** (story Scope Concern 7, for the product owner): an unknown protocol name,
   including a future TLS version, is never enabled; a later version needs one entry in
   `DefaultTrustedSocketFactory.ALLOWED_PROTOCOLS`.

### Runtime Claims

The story makes no claim of one of the five runtime-claim classes (crash or interrupt resilience,
concurrency or races, timing or expiry, git or file-system mechanics, hook firing); this section is declared
empty, not omitted. The behavioural assertions it does make (an old-protocol handshake is refused, a TLS 1.2
handshake completes, the floor and cipher order survive the server-name step) are tested by running real
loopback handshakes and probes, with the probe lines in `evidence/I2-probe.txt` and the control mutation
M-P1/M-P8 in `evidence/mutation-log.md` (the same handshakes complete with the floor removed). They are run
under Conscrypt only, not under the JDK provider. The mutation restores were proven by blob hash and diff hash
after every run.

### Final state of the tree

`git status --short` at the end (the SDLC documents of the previous story and the sprint files were already
modified or untracked at hand-over and were left alone; the story file's `## Implementation Notes` section is
the only part of it this phase edited):

```
 M app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
 M app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryCreateSocketTest.java
 M app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHandshakeTest.java
 M app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHardeningTest.java
 M app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java
 M app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java
 M k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
 M sdlc/artifacts/build/sprints/sprint-021/U-064-tls-baseline-gap-tests-pinned-expiry-and-sni-catch.md
 M sdlc/artifacts/build/sprints/sprint-021/U-065-tls12-floor-both-factories-and-pinned-factory-parity.md
 M sdlc/artifacts/build/sprints/sprint-021/sprint-plan.md
?? sdlc/.skill-context.json
?? sdlc/artifacts/build/sprints/sprint-021/U-064/qa-results.md
?? sdlc/artifacts/build/sprints/sprint-021/U-065/
```

- No probe class is present (0 files matching `U065` under `app/src/test`).
- No mutation is present: every mutation entry has its `restored` line, and the two production files equal
  the saved post-change copies byte for byte (`cmp`); their blob hashes are `43e5a78e...` (default factory) and
  `21cc5f34...` (pinned factory) and the hash of the whole production diff is `6d9a52e7...`, equal to the I5
  values (full hashes above).
- Nothing was staged, committed or pushed. No emulator or device was touched.

Hygiene scan: step I11, result in `evidence/I11-scan.txt`

## Phase Completion Report
---
story_id: "U-065"
phase: "implementation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-021/U-065/implementation-log.md"
story_status: "in-progress"
current_build_phase: "implementation"
files_changed:
  - "k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java"
  - "app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java"
  - "app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHardeningTest.java"
  - "app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java"
  - "app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHandshakeTest.java"
  - "app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryCreateSocketTest.java"
  - "app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java"
tests_run: 732
tests_passed: 730
errors: []
---
