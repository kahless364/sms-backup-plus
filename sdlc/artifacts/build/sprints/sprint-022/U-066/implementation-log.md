---
artifact_type: implementation-log
story_id: "U-066"
gate_id: "implementation"
checkpoint: "implementation"
verdict: "PASS"
agent: "Developer"
iteration: 1
base_commit: "b9fd172fbc397ed43c451379a2c41a9cae1fc605"
---

# Implementation Log: U-066 - Allow-list of cipher suites for both TLS socket factories

Plan: `sdlc/artifacts/build/sprints/sprint-022/U-066/plan.md` (iteration 1, Amendment A1). Evidence is under `sdlc/artifacts/build/sprints/sprint-022/U-066/evidence/`. Raw logs and pristine copies were kept in a work folder outside the repository. Nothing was committed, staged or pushed.

## Result in one paragraph

`DefaultTrustedSocketFactory` now enables only the ten suites of `ALLOWED_CIPHERS` (three TLS 1.3, six ECDHE AEAD TLS 1.2, the renegotiation SCSV), filtered from the socket's default-enabled suites by the new `ciphersToEnable(SSLSocket)`, in the allowed list's order. The failed-initialisation branch computes the suites from the socket. `ORDERED_KNOWN_CIPHERS` and `BLACKLISTED_CIPHERS` are removed; `reorder` is unchanged in code (the protocol step uses it). One comment line in `PinnedCertificateSocketFactory.java` was corrected (E8). Final gate: `exit=0`, `tests=746 failures=0 errors=0 skipped=2`, JaCoCo passes. 15 mutation runs and 2 guard runs are all detected.

## Contract Adherence

The story lists no `integration_contracts` (`integration_contracts: []`). `CNTR-MODERNIZATION-001` is reached only through CR-014 Amendment 1, which this story does not apply; its statements C1 to C4 and B1 are made true by the code (see "Items for the orchestrator").

## Capabilities Inventory (existing code that was changed)

| Capability of the old code | Status | Where |
|---|---|---|
| Static initialiser reads the provider's default-enabled suites and builds `ENABLED_CIPHERS`; both lists null if the query throws | RETAINED (suites now through `ciphersToEnable`) | `DefaultTrustedSocketFactory.java:94-110` |
| Blocked cipher list removed from the enabled set | INTENTIONALLY REMOVED (DP-2 allow-list: a suite not on the allowed list is never enabled) | story DP-2, AC-3 |
| Preferred order placed first, unknown suites appended | INTENTIONALLY REMOVED (DP-2, DP-4: unknown suites stay off; the allowed list is the order) | story DP-2, DP-4 |
| `reorder` (blocked removed, known first, unknown appended) | RETAINED unchanged in code (protocol step) | `DefaultTrustedSocketFactory.java:134-158` area, call in `protocolsToEnable` |
| Protocol step, protocol constants, server-name code, trust code, `createSocket` and public `hardenSocket(SSLSocket)` signatures | RETAINED, no hunk intersects them (I5 hunk check) | `evidence/I5-production.txt` |
| Null cipher list leaves the platform's suites | INTENTIONALLY REMOVED (DP-3 option 1: computed from the socket, C4) | `DefaultTrustedSocketFactory.java:243` |
| Pinned factory reaches the shared step through `hardenSocket(sslSocket)` then `setSniHost` | RETAINED, code byte-identical (class files identical, I9) | `PinnedCertificateSocketFactory.java:101-102` |
| Isolation guards using a DHE suite as the provider marker | INTENTIONALLY REPLACED (AC-8) with live-provider checks | three test classes |

No item is a REGRESSION.

## Runtime Claims

Empty. The story's behavioural claims are proven by unit tests under two desktop providers and by the mutation evidence; no crash, concurrency, timing, filesystem or hook claim is made. IC-1 (emulator) is the orchestrator's.

## Steps

### I0 - Baseline (2026-11-21T08:15:58Z)
Evidence `I0-baseline.txt`, `I0-gates.txt`, `I0-junit-totals.txt`, `I0-pinned-classes.txt`. HEAD `b9fd172f`, ancestor check 0, `app`/`k9mail-vendored`/build paths clean, three blobs equal, count 732, `toolchainVersion=25`, every S0-2 line present in HEAD and working tree. Gate `exit=0`, JDK 25.0.5.1, 732 tests, 0 failures, skipped 2 (the start). Four pinned class hashes recorded. S0-1 and S0-3 are the orchestrator's entries in the story; S0-2 is `b9fd172f`. No decision row.

### I1 - Server option (test only)
`LoopbackTlsTestServer` gained `start(Mode, SSLContext, String[] protocols, String[] suites)`; the two existing overloads are unchanged (`I1-lts-diff-check.txt`: one file, 7 hunks in the planned places).

### I2 - Probes (one Gradle run, `I2-probe.txt`, `I2-probe-cases.txt`)
Both probe classes passed; both were deleted before I3 (`I2-cleanup.txt`: only ` M LoopbackTlsTestServer.java`, 0 probe files; sources kept as `evidence/tools/*.java.txt`). Rows:
- D3: `c_provider=Conscrypt`, 18 defaults equal to `I0-probe.txt:30` (compared after normalising): continue.
- D4: `c_set_allowed` is exactly L-A: continue.
- D5: `c_set_allowed_reversed` and `c_set_aead12_only` put the three TLS 1.3 names first in the provider's order: FIX13 true. SCSV absent when not set: FIXS false. The reversed set puts SCSV fourth (after the TLS 1.3 names), then the rest reversed.
- D6: `c_roundtrip_allowed_kept` and `j_roundtrip_allowed_kept` true.
- D7, D8, D9: `c_hs12_today_list`, `c_hs12_allowed`, `c_hs12_provider_default` all `TLSv1.2` with `TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256` (in AEAD12): PDF true. `c_hs12_client_order` negotiated `TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384`, not the client's first (CBC) choice: O false (the server does not honour the client's order).
- D10: `c_hs13_allowed` TLSv1.3 `TLS_AES_128_GCM_SHA256`.
- D11 for both pruned suites: provider-default client completes with that suite, allowed-only client refused with `SSLHandshakeException`, server `FAILED:javax.net.ssl.SSLHandshakeException`: write the four tests per suite.
- D16, D17: `j_provider=SunJSSE`, 31 defaults equal to `I0c-diag.txt:14`, `j_set_allowed` is L-A.
- D18 (record only): `c_set_empty` accepted with the three TLS 1.3 suites still reported; `c_set_unknown` threw `IllegalArgumentException`; `c_hs_empty_suites` completed at TLSv1.3; `j_set_aead12_only` accepted with protocols `TLSv1.2,TLSv1.3`; `j_set_empty` accepted with 0 enabled.
- D19: both `*_supported_has_all_allowed` true: SUP true.
Outcomes used by I7: O false, PDF true, FIX13 true, FIXS false, SUP true.

### I3 - Test edits (`I3-test-diff-check.txt`)
Sections 3.3 to 3.6 applied. The Hardening class was written as a whole file from the plan's text (the changed regions are the plan's; the diff shows no hunk in the byte-identical regions HP, HR1, HR2, HL, HT and the unchanged helpers). Result: every hunk in the editable regions, `untouched-exit=0`, count 746, `ignore-exit=1`, `old-names-exit=1`, `prod-clean-exit=0`. No decision row.

### I4 - Red run (`I4-red-cases.txt`)
Compiled; 82 cases; exactly the 17 expected failed (H4, H8 `NoSuchFieldException`; H7 `NoSuchMethodException`; H9 `WantedButNotInvoked`; H5, H6, J1, J2, J3 assertion; K4-K7 and P4-P7 assertion); the other 65 passed. Row D24.

### I5 - Production change (`I5-production.txt`, `I5-production.diff.txt`)
E1 to E7 applied to the default factory (working tree stays CRLF: all 320+ lines CRLF checked), E8 to the pinned factory. Exactly two files changed. No default-factory hunk intersects a protected range. Pinned: one hunk `@@ -57 +57 @@`, 0 non-comment changed lines. `removed-names-exit=1`, `supported-exit=1`; `hardenSocket(`/`ciphersToEnable` callers as expected. Saved copies in the work folder and blobs stored: `DTSF_NEW` `2be4e4a21f6b0aa47cb9ad67d64270f6ba3ee8ca`, `PIN_NEW` `1448bb7d3a9a4544fe1e4df0b526d83aa3befea3`, `PROD_DIFF` `72b9e009a3b254400f88cb13e7a54ddde3efd32d`. Row: none needed.

### I6 - Green run (`I6-green-cases.txt`)
82 cases, 0 failures, including the three guards. Test blobs saved: `HARD_NEW` `c5a37a932a5f0d0f517bc07726a7f8936f19e34c`, `HSK_NEW` `64b8827a26bf50b847dc531b072da44df87c3f4c`, `JDKT_NEW` `f120808628f19100d95fbe8086e66cfd47aca22e`. Row D29.

### I7 - Mutations (`mutation-log.md`, `mutations/`)
All 15 counted mutation runs and both guard checks were detected, each with a write-ahead entry and a `restored` line with hash, porcelain and diff-hash checks. Every failing set equals the plan row's set (listed in the log). Declared narrowings: M-X4 (i) and M-X5 (ii) (plan 2.5). M-X1 (e) is undetectable at socket level on both providers, as the plan row expects. M-X3 not applicable. One deviation: the first run of M-X5 (ii) hit a Robolectric class-level error in the Handshake class (`Couldn't create lock file <home>/.robolectric-download-lock`) before any of its tests; decision row D33 applied: restored, logged as MISMATCH (not counted), the mutation re-applied once as entry "M-X5 (ii) repeat" (17 failures as expected). Summary table at the end of the mutation log.

### I9 - Final gate (`I9-gates.txt`, `I9-junit-totals.txt`, `I9-checks.txt`)
`exit=0`; `files=78 tests=746 failures=0 errors=0 skipped=2` (skipped not above the start); JaCoCo passed; `determinism-diff-exit=0` (the 82 cases equal I6's); `pinned-classes-diff-exit=0` (the four pinned class files are byte-identical to the I0 build); empty build-file diff; `vm-exit=0`; `untouched-exit=0` (`K9MailTransport.java`, `TlsTestFixtures.java`); `supported-exit=1`; `ignore-exit=1`; no `U066` probe file. IC-2 grep accounted for below.

Observation, not a deviation: the plan's `records-end` check (`git status --porcelain -- sdlc/artifacts/build/change-records sdlc/artifacts/design/contracts sdlc/artifacts/requirements`) is not empty at I9. It lists files that other agents are working on in this checkout: modified `CNTR-MODERNIZATION-004-backup-scheduler.md`, `REQ-MODERNIZATION-005-...md`, `requirements/modernization/_index.md`, and new `CR-015-...md`, `CR-016-...md`, `REQ-MODERNIZATION-020-...md`. This story touched none of them (I never wrote under those paths; no git command of mine touched them). CR-014 and CNTR-MODERNIZATION-001 are not in the list.

### I11 - Hygiene scan
Last line below.

## Files

Created: `evidence/` files (listed by name in each step), `evidence/mutations/*`, `evidence/tools/U066ConscryptProbeTest.java.txt`, `evidence/tools/U066JdkProbeTest.java.txt`, this log. Created and deleted within I2: the two probe classes.

Modified: `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` (E1 to E7); `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` (E8, one comment line); `app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java`; `.../DefaultTrustedSocketFactoryHardeningTest.java`; `.../DefaultTrustedSocketFactoryJdkProviderTest.java`; `.../DefaultTrustedSocketFactoryHandshakeTest.java`; `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryCreateSocketTest.java`; the story's Implementation Notes (developer entries only).

## Tests

Added (14, count 732 to 746): `ciphersToEnable_givenDefaultEnabledList_keepsOnlyAllowedSuitesInAllowedOrder` (H7), `allowedCipherConstant_holdsOnlyTheAllowedNameForms` (H8), `createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes_jdkProvider` (J2), `pinnedFactory_createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes_jdkProvider` (J3), `implicitTls_unrestrictedServer_negotiatesTls13WithAnAllowedSuite` (K3), `implicitTls_serverLimitedToEcdheCbcSha_handshakeRefused`, `startTls_serverLimitedToEcdheCbcSha_handshakeRefused`, `implicitTls_serverLimitedToRsaKeyExchangeGcm_handshakeRefused`, `startTls_serverLimitedToRsaKeyExchangeGcm_handshakeRefused` (K4-K7), `handshake_implicitTls_unrestrictedServer_negotiatesTls13WithAnAllowedSuite` (P3), `handshake_implicitTls_serverLimitedToEcdheCbcSha_isRefused`, `handshake_startTls_serverLimitedToEcdheCbcSha_isRefused`, `handshake_implicitTls_serverLimitedToRsaKeyExchangeGcm_isRefused`, `handshake_startTls_serverLimitedToRsaKeyExchangeGcm_isRefused` (P4-P7).

Changed (12; 3 renamed): `cipherConstants_matchRecordedBaseline`; `createSocket_enabledCipherSuites_areProviderDefaultsMinusBlockedWithKnownFirst` renamed `createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes`; the pinned twin renamed `pinnedFactory_createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes`; `hardenSocket_withNullLists_stillAppliesTheProtocolFloorFromTheSocket` renamed `hardenSocket_withNullLists_appliesTheAllowedSuitesAndTheProtocolFloorFromTheSocket`; the three `reorder` tests (retargeted to literals); `pinnedFactory_createSocket_implicit_setsDnsHostAsOnlyServerName_jdkProvider`; the four TLS 1.2 handshake tests through their helpers. Plus three guards reworked. None removed, no `@Ignore`/`Assume`.

## Criterion-to-evidence table

| Criterion | Result | Evidence |
|---|---|---|
| S0-1, S0-3, IC-3 | Orchestrator entries, by reference to the story's Implementation Notes | story |
| S0-2 | Commit `b9fd172f`; every CR-014 line present | `I0-baseline.txt` |
| S0-4 (a)-(d) | (a) `c_/j_set_allowed` = L-A; (b) plain-JDK result in plan L11 (silent TLS 1.2 without a TLS 1.3 suite) and `j_set_aead12_only`; (c) `c_hs12_*` all in AEAD12; (d) control completes, allowed-only client refused | `I2-probe.txt` |
| AC-1 | Met: H5 (Conscrypt), J2 (JDK) literal L-A, preconditions, both shapes | `I6-green-cases.txt`, mutations M-X1/2/4/5/6/8 |
| AC-2 | Met: H6, J3, J1 (literal after server-name step); parity C1, C2, J7 unchanged and green; pinned code untouched except E8 | `I6-green-cases.txt`, `I5-production.txt`, `I9-checks.txt` |
| AC-3 | Met: (a) W loop in the exact-list tests; (b) H8 name forms, W1-W12, TLS 1.3 and 1.2 present; (c) H7 (i)-(iv) 62-name input | `I6-green-cases.txt`, M-X1, M-X4 |
| AC-4 | Met: H9 (a), (b), (c); M-X7, M-X10 detected; initialiser failure not forced | `mutation-log.md` |
| AC-5 | Met: four TLS 1.2 tests assert an AEAD12 suite; K3/P3 TLS 1.3 with a TLS 1.3 suite | `I6-green-cases.txt` |
| AC-6 | Met under Conscrypt for both suites, both factories, both shapes; control from probe and M-X5 | `I2-probe.txt`, `mutation-log.md` |
| AC-7 | Met: table in "Tests" and the story notes; unchanged set green | `I3-test-diff-check.txt`, `I9-all7-cases.txt` |
| AC-8 | Met: guards reworked, G-1 33/33, G-2 11/11 | `mutations/G-1-*`, `G-2-*` |
| AC-9 | Met: 15 counted mutation runs plus 2 guard runs detected; narrowings declared | `mutation-log.md` |
| AC-10 | Checklist below; no change record, contract or requirement edited by this story | this log |
| AC-11 | (a) one file changed in code, one in a comment (E8, as the orchestrator's ruling reads it); (b) protocol code no hunk; (c) server-name and trust code no hunk; (d) signatures unchanged, `K9MailTransport.java` untouched; (e) empty build diff, `vm-exit=0` | `I5-production.txt`, `I9-checks.txt` |
| AC-12 | Met: before and after gate `exit=0`, 0 failures, skipped 2 then 2; count 732 then 746 (+14 added, 0 removed); JaCoCo passed, no floor edited. Note: the authoritative HEAD count stays 732 until the commit | `I0-*`, `I9-*` |
| AC-13 | Developer entries in the story's Implementation Notes | story |
| IC-1 | PENDING for the orchestrator (emulator check, plan section 9) | none yet |
| IC-2 | Met: full gate compiles every caller; grep (below) accounted for | `I9-checks.txt` |
| IC-3 | By reference to the story's measurement entry | story |

IC-2 occurrences after the change (`git grep` in `I9-checks.txt`): production default factory `:35` (`ENABLED_CIPHERS` declaration), `:95-96` (the comment and call in the initialiser), `:110` (assignment), `:182` (`ciphersToEnable`), `:219` (`createSocket` call), `:232-233` (public `hardenSocket`), `:242-243` (three-argument `hardenSocket`); pinned factory `:56` (Javadoc), `:101` (call); tests: Hardening 20 lines (constants by reflection, guard, step and branch calls), JDK `:193`, Handshake `:558`, `:569`, create-socket `:53` (Javadoc). `ORDERED_KNOWN_CIPHERS` and `BLACKLISTED_CIPHERS` appear nowhere.

## Final state of the tree

`git status --short -- app k9mail-vendored`: seven ` M` lines (the two production files, `LoopbackTlsTestServer.java`, and the four test classes); no `??` line. No probe class. No mutation present: `git hash-object` of the default and pinned factories equals `DTSF_NEW` and `PIN_NEW` (the saved post-change copies), the production diff hash equals `PROD_DIFF`, the three reworked test classes equal their saved blobs (checked after every mutation and before the final gate).

## Deviations

1. Restore by copy from the pristine files for both production files (plan I7, declared).
2. M-X4 (i) and M-X5 (ii) narrowings; M-X10 added (plan, declared).
3. M-X5 (ii) was run twice because of a Robolectric lock-file error (decision row D33); the first run is logged as not counted.
4. The Hardening test class was written as a whole file rather than region by region with the Edit tool; the resulting diff is inside the plan's editable regions (I3 check).
5. The mutation log header and result lines were appended with a small shell `printf`/heredoc call (log text only, no code). Code edits used only the Edit and Write tools.
6. `records-end` check (I9) lists other agents' files, outside this story (see I9).
7. E8 is the comment-only edit allowed by the orchestrator's ruling on scope concern P-1; AC-11 (a) read as that ruling words it.

## Items for the orchestrator

1. IC-1 (emulator check, plan section 9) is yours. Debug APK `app/build/outputs/apk/debug/app-debug.apk`, 6572602 bytes, built by the I9 gate from the final tree, SHA-256 `d5b91acaa4c1fe3ae6f7a098ec70ae754a0c4352936d9b740685a05da809edfa`. No mutation entry lacks `restored`.
2. Scope concern P-1 is resolved here: E8's three proofs are the single `@@ -57 +57 @@` hunk with comment lines only, identical class files (`I9-checks.txt`, `pinned-classes-diff-exit=0`), and `createSocket` not edited (so M-E4 and M-E5 were not re-run).
3. CR-009's dated note after this story's fix commit (the plan's text, values to fill from this log):
   > **Note: what story U-066 changed (added <date>, after commit <fix sha>).**
   > (a) T5 now reads: "`hardenSocket`, `protocolsToEnable`, `ciphersToEnable`, `reorder`: allowed cipher suites taken from the platform's default-enabled set, in the allowed list's order; protocols limited to `ALLOWED_PROTOCOLS` from the supported set. Exist (U-063, U-065, U-066)." This replaces the 2026-11-21 suggestion "blocked and ordered cipher suites".
   > (b) The baseline the move must keep green is the tests as U-063 to U-066 left them (746 tests at <fix sha>, 2 skipped). The classes now also reach `ALLOWED_CIPHERS` and `ciphersToEnable` by reflection. `ORDERED_KNOWN_CIPHERS` and `BLACKLISTED_CIPHERS` no longer exist.
   > (c) U-066 renamed three Hardening tests: `createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes`, `pinnedFactory_createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes` and `hardenSocket_withNullLists_appliesTheAllowedSuitesAndTheProtocolFloorFromTheSocket`. It added exact-list tests on both providers, the name-form check `allowedCipherConstant_holdsOnlyTheAllowedNameForms`, and handshake refusals for pruned suites. Retarget them; do not drop them.
   > (d) In the note's last paragraph, "C1 to C3" reads "C1 to C4" (CR-014 Amendment 1).
   Seams to retarget at the move, not drop: `ALLOWED_CIPHERS`, `ciphersToEnable`, the three-argument `hardenSocket`, `ENABLED_CIPHERS` and `ENABLED_PROTOCOLS` in the three guards. F-4 (private or unmodifiable arrays) applies there.
4. AC-10 checklist against CR-014 Amendment 1 (no new wording):

| Item | This story's answer (evidence) |
|---|---|
| Point 1 (block what is pruned) | Pruned names are absent from `ALLOWED_CIPHERS` and the filter drops names on no list (H7 (i), H8; M-X2) |
| Point 2 (same lists for both) | One shared step, no pinned code edit (E8 is one comment line, identical class files; C1, C2, J7, H6, J3; M-X5) |
| Point 3 (shape changed) | Allow-list ruled; CR-014 reworded first (`b9fd172f`); Amendment 1 Parts A, C, D, E, F govern, Part E's Versioning directions, not the story's AC-10 text (Part I) |
| Point 4 (baselines in the same step) | H4 and the literals (AC-7 (a)) |
| Point 5 (a C2 test not a snapshot) | H8 against C2's forms; H7 at the step's input |
| Point 6 (V1-V5 and the null branch) | Protocol code and tests unchanged; the cipher null branch is changed and recorded (H9, C4) |
| Point 7 (no contract edit) | No file under `sdlc/artifacts/design/contracts` or `change-records` edited by this story; the I9 `records-end` list shows other agents' files (see I9) |
| Point 8 (suites on neither list) | DP-4: dropped unless allowed; the two ChaCha20 TLS 1.2 suites joined the allowed list |
| Point 9 (filter the default-enabled set) | `ciphersToEnable` reads `getEnabledCipherSuites()`; the `getSupportedCipherSuites` search returns nothing; H7 (iii); M-X9 |
| Point 10 (TLS 1.3 names stay) | In L-A (H4, H8's TLS 1.3 clause, J2, J3); M-X4 (i) |
| Point 11 / Scope Concern A (C4) | DP-3 option 1: H9 and M-X7, M-X10 in place, so C4 applies and Part D deletes the failed-initialisation bullet |
| Scope Concern B | Moot under the allow-list; fed in at the step's input (H7 (i)) |
| Part G (Traceability, for the applier) | C1 -> H7 (iii), the search, M-X9. C2 -> H5, H6, J2, J3, H8, H7 (i), (iv), H9 (b), (c); M-X1, M-X2, M-X4, M-X10. C3 -> order in H4-H7, J1-J3; M-X8. C4 -> H9; M-X7. B1 -> C1, C2, J7, H6, J3; M-X5. Limits -> K1, K2, P1, P2, K3, P3, K4-K7, P4-P7 |
| Part H (applier's checks) | 1 tests exist and are green (I9); 2 H8 checks the shipped constant against C2's forms; 3 C4 applies; 4 H8's TLS 1.3 clause; 5 Part E governs |

5. AC-10 readings for QA: the change-record check is `git diff --name-only b9fd172f..<fix commit> -- sdlc/artifacts/build/change-records sdlc/artifacts/design/contracts` (expected empty for this story's files); point 3 is superseded by Amendment 1 Part I.
6. The probe results for the empty list differ by provider (Conscrypt keeps its TLS 1.3 suites and a TLS 1.3 handshake completes; SunJSSE refuses): recorded, not asserted, as CR-014 Part D leaves it "Not stated".
7. The other agents' uncommitted files that appear under `sdlc/artifacts/requirements` and `design/contracts` in the plan's `records-end` check (I9) are yours or another agent's, not this story's.

## Hygiene scan

Step I11, result in `evidence/I11-scan.txt`.
