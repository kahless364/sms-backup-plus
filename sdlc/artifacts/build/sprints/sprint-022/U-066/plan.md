---
artifact_type: plan
story_id: "U-066"
gate_id: "planning"
checkpoint: "planning"
verdict: "PASS"
agent: "Solution Architect"
timestamp: "2026-11-21T07:25:14Z"
iteration: 1
base_commit: "b9fd172fbc397ed43c451379a2c41a9cae1fc605"
contracts_verified:
  - CNTR-MODERNIZATION-001
risks_identified: 11
---

# Implementation Plan: U-066 — Prune the Weak Cipher Suites by an Allow-List, and Close the Failed-Initialisation Branch for Cipher Suites

Story: `sdlc/artifacts/build/sprints/sprint-022/U-066-prune-weak-tls-cipher-suites.md`. Model: U-065's plan
(`sdlc/artifacts/build/sprints/sprint-021/U-065/plan.md`, iteration 2), whose structure, conventions and tools
this plan follows.

**Starting state, verified at planning time.** HEAD is `b9fd172f` on `sdlc/modernization-plan`, one commit after
`fba733e5`: `b9fd172f` ("CR-014 Amendment 1: cipher suites by allow-list") changes only
`sdlc/artifacts/build/change-records/CR-014-…md` (`git diff --stat fba733e5 b9fd172f`: one file). It is the
orchestrator's step S0-2. `<start>` for this story's commit-based checks stays `fba733e5`, as briefed. Every
`app/` and `k9mail-vendored/` check against `<start>` reads the same as against `b9fd172f`. The one exception is
the AC-10 change-record check, which section 7 handles. The authoritative test count at HEAD is 732, and
`git status --porcelain -- app k9mail-vendored` prints nothing. The other work in the checkout (the audit log,
`.skill-context.json`, `sdlc/artifacts/build/research/`, the untracked DES-MODERNIZATION-014, the sprint-022
folder) is left alone.

**Size.** One production file changes in code (`k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`).
One more production file changes in one comment line only (`app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java`,
edit E8; the orchestrator's ruling, story Implementation Notes "Ruling on the plan's scope concern P-1").
`LoopbackTlsTestServer` gets an additive option, and four test classes change. 14 tests are added, 8 of them
conditional on probe I2 ([R]). 12 tests are changed, 3 of them renamed. The count goes from 732 to 746 (or 742
or 738, rows D11 and D12). There are 15 mutation runs and 2 guard runs, and 22 Gradle runs in all.

## Amendment A1 (after the plan gates, 2026-11-21)

The plan gates of iteration 1 were plan-review PASS, contract-verification PASS and expert-depth-check FAIL
(warning only, not blocking), in `plan-review.md`, `contract-verification.md` and `expert-depth-check.md` in this
workspace. No rework iteration was required, and `iteration: 1` stays. This amendment takes up every non-blocking
finding. Each change is made in place, where the developer reads it; the table only says where to look.

| Finding | Change, and where |
|---------|-------------------|
| plan-review O-1 | The Hardening constants region is now `:118-167`, and the replacement block ends with the two protocol lines that are at `:166-167` today, so a literal replacement declares each field once and compiles (3.3, "Constants"). Every other replacement region in section 3 was re-read against its file. Each starts at a `/**` or a statement and ends at the matching `}` or `*/`, and none re-declares a line outside its range. |
| plan-review O-2 | I5 saves both production files (`cp $DTSF $PIN`), stores both blobs, and records `DTSF_NEW` and `PIN_NEW`. I7's clean check, restore (step 5) and recovery note use the saved copy for both files, with no `git checkout` (I5, I7). |
| plan-review O-3 | M-X5 (ii) is declared as a narrowing of the story's table, next to M-X4 (i): with only the default factory's call removed, the pinned exact-list tests H6 and J3 cannot fail (2.5 item 4, I10, Verdict). |
| plan-review O-4 | The M-X2 row gives "{O or PDF}" as the reason K1, K2, P1, P2 pass (I7). |
| plan-review O-5 | The verdict no longer calls M-X1 (b) and (e) a deviation; they follow the story's own scoping (Verdict). |
| plan-review O-6 | H1 has "two times", not "three times" (3.3). E6's `old_string` is "the four lines" (2.7). |
| plan-review O-7 | I9's `git status --porcelain -- app k9mail-vendored` now has its exact expected output: seven ` M` lines and no `??` line. The other uncommitted documents of the checkout lie outside these paths and do not appear (I9). |
| plan-review O-8 | IC-1 step 1 now covers exactly one physical device and no emulator: nothing is sent, stop and ask the owner (section 9). |
| plan-review O-9, contract-verification O-2 | The orchestrator ruled on 2026-11-21: a comment-only correction of the pinned factory's class Javadoc is allowed, as an exception to AC-2 (c) and AC-11 (a). New edit E8 (2.7). It replaces one line with one line, so line numbers do not move. Proof is the comment-only diff (I5) plus identical compiled class files from the I0 and I9 builds (I0 step 2, I9 step 4; row D41). `createSocket` is not edited, so M-E4 and M-E5 are not re-run. The file counts, the production-diff hash, the saved copies, the untouched lists, L4, 2.4, 2.6, section 7 and Scope Concern P-1 are adjusted. |
| contract-verification O-1 | H9 gains case (c): a non-null empty cipher list is set as it is and never replaced by the platform's suites. New mutation M-X10 has its row (3.3, I7). Tests and cases are unchanged; mutation runs 15, Gradle runs 22. |
| contract-verification O-3, O-4 | Section 7, "Items for the orchestrator", now carries the lines CR-009's dated note needs after this story (the gate's text; line (e) is not needed, because E8 makes the fix here), and the declared reading of AC-10 point 3 (superseded by Amendment 1 Part I). The developer edits no change record. |
| contract-verification O-6 | 2.3 cites the empty-intersection rule as C2's second sentence, quoted. |
| expert-depth-check, criteria 1 to 4 | Text only. New 2.0, "Principles applied": fail-safe defaults (Saltzer and Schroeder) for the allow-list and the fail-closed branch; CWE-184 for the old deny-list; CWE-636; Remove Dead Code (Fowler); characterisation tests and seams (Feathers); the old DHE guard as a Fragile Test (Meszaros); undetectable mutants. 2.1 gains the RFC sources of each part of the list and the order's status, plus trade-off blocks for the allow-list, the single constant and the guard rework. E2's Javadoc names the RFC sources. The CWE-636 title is corrected to its current form. No design, test or mutation change follows from these. |

Counts after the amendment, each stated once here and used consistently below:
- **Tests:** 14 added (8 [R]), 12 changed (3 renamed); 732 → 746 (or 742 or 738).
- **Cases:** 82 in the seven classes.
- **Mutation-log entries:** M-X1 (a) to (e), M-X2, M-X4 (i) and (ii), M-X5 (i) and (ii), M-X6 to M-X10, giving 15 mutation runs, plus G-1 and G-2 (17 runs). M-X3 is logged as not applicable.
- **Gradle runs:** 22.

## 0. Binding Inputs

| Source | Content |
|--------|---------|
| Story rulings (S0-1; Implementation Notes "Rulings") | DP-1 **A**: ten names (TLS13, AEAD12, SCSV). DP-2 **allow-list**, filtered from the platform's *default-enabled* suites. DP-3 **(1)**: the null branch computes suites from the socket. DP-4 **as written**: order L-A (`TLS_AES_128_GCM_SHA256`, `TLS_AES_256_GCM_SHA384`, `TLS_CHACHA20_POLY1305_SHA256`, `TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384`, `TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384`, `TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256`, `TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256`, `TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256`, `TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256`, `TLS_EMPTY_RENEGOTIATION_INFO_SCSV`); the two ChaCha20 TLS 1.2 suites stay. Ruled by the orchestrator on 2026-11-21. Not re-opened |
| Story S0-3 and IC-3 | Done by the orchestrator before planning (Implementation Notes, "Measurement for S0-3 and IC-3"). Both emulators (API 36 `Pixel_9_Pro`, API 30 `Pixel_4_2`) list the same 18 default names as desktop Conscrypt (`I0-probe.txt:30`). Filtering keeps all ten. Gmail at TLS 1.2 chose `TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256`, which is allowed. S0-3's stop rule is not triggered. The developer runs neither |
| CR-014 Amendment 1, as corrected (committed in `b9fd172f`) | The contract text this behaviour must make true: Part A (C1 to C4, with C2 stated as positive name forms), C (B1, B2), D ("Not stated"), E (rule 7, Versioning, changelog), F (Edit 6), G (Traceability), H (the applier's checks). Corrections (a) to (e) of the Solution Architect's "Check of Amendment 1" are present at HEAD. I checked this by search: the C2 forms sentence, the "every TLS 1.3 suite" breaking clause, "Which suites the platform enables or places itself", the governs sentence and the pointers are in both HEAD and the working tree, and `git diff HEAD` on CR-014 is empty |
| Coordinator, 2026-11-21 (the Check's five points for the story) | **B1** AC-3 (b) checks the allowed constant against the C2 name **forms**, not literals; W1 to W12 stay as an extra assertion; the exact-list tests stay literal (H8). **B2** One added case: defaults with no allowed name give an empty list, with no fallback. What the platform then does is recorded, not asserted, because it differs by provider (H7 (iv), H9 (b), probe keys `*_set_empty`). **B3** Desktop Conscrypt reports its TLS 1.3 suites as enabled whatever is set. The expected lists and the "cannot pass on provider defaults" argument allow for it (section 2.5, H5, H6, M-X4). **B4** Amendment 1 Part E governs the Versioning wording. The developer's AC-10 hand-over is a checklist against the amendment, not new wording (section 7). **B5** The JDK test that checks the cipher order after the server-name step gets the new literal list (J1) |

## 1. Assumption Ledger

`verified` = read or run in this planning session, with its location. `needs_check` = a developer step with a
decision row for each outcome. Nothing material is left `assumed` without a row.

| # | Claim | Status | Evidence |
|---|-------|--------|----------|
| L1 | HEAD `b9fd172f…`; its parent is `fba733e5`, and `b9fd172f` touches only CR-014; the count is 732; `app` and `k9mail-vendored` are clean | verified | `git rev-parse`, `git log`, `git diff --stat fba733e5 b9fd172f`, the CLAUDE.md count command, `git status --porcelain -- app k9mail-vendored` |
| L2 | Blobs, equal at HEAD and in the working tree: default factory `43e5a78ead7c4c32a6af2734514ab8ba5d5ccbd3` (319 lines, working tree CRLF); pinned factory `21cc5f345977ab6fa161b2aa6925a04aea6a1479`; `LoopbackTlsTestServer.java` `a0899262f0d858dd399ba078c21311fc2d763cab`. Hardening, JDK, Handshake, create-socket test and server are LF | verified | `git rev-parse HEAD:<p>`, `git hash-object <p>`, `git ls-files --eol` |
| L3 | Default factory: `ENABLED_CIPHERS` `protected static final` `:34`; `ORDERED_KNOWN_CIPHERS` `:37-62` (24); `BLACKLISTED_CIPHERS` `:64-81` (16); protocol constants `:83-99`; static initialiser `:101-128` (suites read `:110`, `reorder` applied `:124-125`, both null if the query threw `:119-122`); `reorder` `:134-158` (comment `:153-154`); `protocolsToEnable` `:160-185`; `createSocket` `:189-217` (`hardenSocket(sslSocket)` `:213`, then `setSniHost` `:214`); public `hardenSocket(SSLSocket)` `:219-228`; three-argument `hardenSocket` `:230-241` (`if (ciphers != null)` `:237-239`, protocol line `:240`); SNI code `:243-318` | verified | file read in full |
| L4 | Pinned factory `createSocket` `:85-104` calls `DefaultTrustedSocketFactory.hardenSocket(sslSocket)` `:101`, then `setSniHost` `:102`. Its class Javadoc `:54-62` says "the same cipher-suite order and blocked list" (`:57`), which becomes false after this story. A1: the orchestrator's ruling allows a comment-only correction (E8). The working tree is CRLF and the index LF. The javac class files are at `app/build/intermediates/javac/{debug/compileDebugJavaWithJavac,release/compileReleaseJavaWithJavac}/classes/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory{,$PinnedX509TrustManager}.class` | verified | file read in full; `git ls-files --eol`; `find app/build/intermediates`; story `:329` |
| L5 | `reorder` callers after the change: `protocolsToEnable` `:173` and the Hardening tests. Before: also `:125`. No production code calls `getSupportedCipherSuites` | verified | `git grep -n "reorder("`; `git grep -n getSupportedCipherSuites -- k9mail-vendored/src/main app/src/main` exit 1 (scope verified via grep) |
| L6 | IC-2: 30 occurrences of `ENABLED_CIPHERS\|ORDERED_KNOWN_CIPHERS\|BLACKLISTED_CIPHERS\|hardenSocket\|ciphersToEnable` in 6 files: production 9 (`:34`, `:37`, `:64`, `:124`, `:125`, `:213`, `:226`, `:227`, `:236`); pinned 2 (`:56` Javadoc, `:101`); Hardening 14 (`:89`, `:182`, `:193`, `:212`, `:242`, `:257`, `:262`, `:272`, `:274`, `:513`, `:517`, `:616`, `:617`, `:619`); JDK 2 (`:175`, `:191`); Handshake 2 (`:462`, `:473`); create-socket 1 (`:53`). Accounted for in section 2.6 | verified | `git grep -n -E …` (scope verified via grep) |
| L7 | Test classes at HEAD: Hardening 12 tests (guard `:179-199`; baselines `:119`, `:147`; `reorder` tests `:205`, `:235`, `:256`; constants `:271`, `:282`; cipher rule `:297`; protocol tests `:363`, `:405`; layered `:441`; `protocolsToEnable` `:480`; null lists `:513` with `never()` at `:522`; pinned twin `:532`; `providerFixedSuites` `:585-606`; `stubSocket` `:626-631`; `assertCipherRule` `:662-711`). JDK class 9 (guards `:149-195`, DHE clause `:171-194`; J1 `:330`, cipher order `:350-353`; parity `:495`, precondition `:499-502`). Handshake 14 (TLS 1.2 `:290`, `:296`; refusals `:306-326`; `assertCompletesAt` `:329-341`; guard `:460-479`). Create-socket 10 (parity `:152`, `:176`; TLS 1.2 `:251`, `:257`; `assertPinnedHandshakeCompletesAt` `:323-336`; `assertCipherOrderIsNotTheProviders` `:365-370`). SNI 14, Expiry 1, `PinnedCertificateSocketFactoryTest` 8. Seven classes, 68 cases | verified | files read in full (SNI, Expiry, PTT counted by `grep -c @Test`) |
| L8 | `LoopbackTlsTestServer`: constructor `:84-93`; `start(Mode, SSLContext)` `:99-101`; `start(…, String[] protocols)` `:108-114`; `serve` `:116-141`, protocols set `:129-131`, before `startHandshake` `:132` | verified | file read in full |
| L9 | Conscrypt (desktop, Robolectric `sdk=29`): provider name `Conscrypt`. Its 18 default suites are listed (`I0-probe.txt:30`). The factory's socket reports the TLS 1.3 suites first in the provider's order (`:31` against `:32`). The provider-fixed suites are those three (`:34`). Supported protocols equal default enabled, TLS 1.0 to 1.3 (`:26-27`). Every handshake so far negotiated `TLS_AES_128_GCM_SHA256` (`:1`). The server-name round trip keeps a reordered cipher list (`I2-probe.txt:5`). `setEnabledProtocols(new String[0])` is accepted (`I2-probe.txt:6`) | verified (by reading evidence) | `sprint-020/U-063/evidence/I0-probe.txt`, `sprint-021/U-065/evidence/I2-probe.txt` |
| L10 | SunJSSE in the JDK class's sandbox: provider name `SunJSSE`, Conscrypt not registered, 31 default suites (`I0c-diag.txt:14`). The JDK class runs no handshake (a SunJSSE server inside Robolectric failed with `NoClassDefFoundError`, `I0c-diag.txt:81-86`). Disabled algorithms include `TLS_RSA_*`, `ECDH`, `3DES_EDE_CBC`, `anon`, `NULL` (`I0-probe.txt:65`) | verified (by reading evidence) | as cited |
| L11 | Plain JDK 25.0.5.1 (outside Robolectric), section 1.1. The 31 defaults equal `I0c-diag.txt:14`. The planned filter (re-typed) turns both recorded defaults into exactly L-A. Setting the ten names reads back L-A in that order, and a server-name round trip keeps it. An unknown name throws `IllegalArgumentException`, and an empty array is accepted (0 enabled). A client with TLS 1.3 enabled and no TLS 1.3 suite **negotiates TLS 1.2 silently** (story S0-4 (b), handshake level; protocols still read `TLSv1.2,TLSv1.3`). A client with empty suites is refused (`SSLHandshakeException`, "No appropriate protocol"). Against a server limited to `TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA`, a default client completes and an L-A client is refused (`SSLHandshakeException`). The AC-3 (c) input has 62 distinct names, holds all of W1 to W12, and gives L-A in both orders. Every pruned name matches at least one of W1 to W12. The C2 forms admit exactly the ten among the 62 and reject all 12 escape names of the Check (static DH, PSK, CCM, CCM_8, ARIA, Camellia, integrity-only TLS 1.3, `TLS_FALLBACK_SCSV`). The test fixture (RSA 2048) negotiates `TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384` at TLS 1.2 with an L-A client | verified (outside Robolectric) | throwaway program in the planner's session scratch folder, compiled with `TlsTestFixtures.java` read-only from the repository; nothing written to the repository. Output in 1.1 |
| L12 | JaCoCo class roots are the app module's own class directories, so the vendored file is not measured. The `mail*` floor is LINE 0.70 | verified | `app/build.gradle:326-336`, `:369-374` |
| L13 | JUnit XML is off (`build.gradle:30`); `maxParallelForks = 4`, `forkEvery = 100`. The read-only init script `sprint-020/U-063/evidence/tools/junit-xml.init.gradle` turns XML on. Skipped count 2 at U-065's end (`sprint-021/U-065/evidence/I9-junit-totals.txt:1`) | verified | as cited |
| L14 | IC-1 log lines: `BackupWorker: starting backup (%d messages)` `BackupWorker.kt:313`; the connection opens at `transport.checkSettings()` `:320`, after the count; `BackupWorker: backup complete, backedUp=` `:369`; `BackupWorker: unrecoverable error` `:169`. Debug APK `app/build/outputs/apk/debug/app-debug.apk` | verified | file read; `ls` |
| L15 | `compiled-pipeline.json` of this workspace is excluded locally (`.git/info/exclude:39`); the story file has 0 matches of the hygiene pattern | verified | `grep -n`; count-only `grep -cEi` |
| L16 | CNTR-MODERNIZATION-001 contains no `cipher` (case-insensitive) | verified | `grep -n -i cipher` on the contract: no line |
| N1 | Under Robolectric Conscrypt: setting L-A reads back L-A; setting L-A reversed puts the three TLS 1.3 suites first in the provider's order; setting only AEAD12 still reports the TLS 1.3 suites (and whether SCSV appears unset) | needs_check | probe I2 keys `c_set_*`; rows D3 to D5 |
| N2 | Under Robolectric Conscrypt, at TLS 1.2: the suite negotiated by today's list, by L-A and by provider defaults, and whether the server honours the client's order | needs_check | keys `c_hs12_*`; rows D6 to D9 |
| N3 | The desktop Conscrypt server serves each of the two pruned suites to a provider-default client, and refuses an L-A client (the AC-6 control) | needs_check | keys `c_hs_<suite>_*`; rows D11 to D15 |
| N4 | Inside the JDK sandbox: 31 defaults; L-A reads back as set; the server-name round trip keeps it; AEAD12 with TLS 1.3 enabled (list level); empty array | needs_check | keys `j_*`; rows D16 to D18 |
| N5 | Both providers' supported suites contain all ten names (they decide M-X9's row) | needs_check | keys `*_supported_has_all_allowed`; row D19 |
| N6 | The daemon JVM is 25; a debug build installs over the emulator's app without losing data | needs_check | I0 (`U063-DAEMON-JVM`), row D34; IC-1 (orchestrator) |

### 1.1 Throwaway JDK check run while planning (output, verbatim; plain `java`, JDK 25.0.5.1)

The program re-typed the planned `ciphersToEnable`, the C2 form predicate and the W1 to W12 predicate from
sections 2 and 3, and ran handshakes between two SunJSSE sockets over loopback with the repository's RSA 2048
fixture.

```
U066-SA java.version=25.0.5.1
U066-SA provider=SunJSSE
U066-SA jdk_default_count=31
U066-SA jdk_default=TLS_AES_256_GCM_SHA384,TLS_AES_128_GCM_SHA256,TLS_CHACHA20_POLY1305_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256,TLS_DHE_DSS_WITH_AES_256_GCM_SHA384,TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_DSS_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_RSA_WITH_AES_256_CBC_SHA256,TLS_DHE_DSS_WITH_AES_256_CBC_SHA256,TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_DSS_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_256_CBC_SHA,TLS_DHE_DSS_WITH_AES_256_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_EMPTY_RENEGOTIATION_INFO_SCSV
U066-SA jdk_default_enabled_protocols=TLSv1.3,TLSv1.2
U066-SA filter_jdk_default=TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384,TLS_CHACHA20_POLY1305_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256,TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,TLS_EMPTY_RENEGOTIATION_INFO_SCSV
U066-SA filter_jdk_equals_allowed=true
U066-SA filter_conscrypt_default_equals_allowed=true
U066-SA conscrypt_default_count=18
U066-SA set_ten_readback=TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384,TLS_CHACHA20_POLY1305_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256,TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,TLS_EMPTY_RENEGOTIATION_INFO_SCSV
U066-SA set_ten_readback_equals=true
U066-SA roundtrip_ten_kept=true
U066-SA default_differs_from_allowed=true
U066-SA unknown_name=threw java.lang.IllegalArgumentException: Unsupported CipherSuite: TLS_U066_UNKNOWN
U066-SA empty_array=accepted enabled_count=0
U066-SA union_size=62
U066-SA filter_union_equals_allowed=true
U066-SA filter_reversed_union_equals_allowed=true
U066-SA union_categories=[W1, W10, W11, W12, W2, W3, W4, W5, W6, W7, W8, W9]
U066-SA allowed_with_form=10
U066-SA form_admits_not_allowed_in_union=[]
U066-SA allowed_matching_W=[]
U066-SA pruned_names_matching_no_W=[]
U066-SA forms_on_escapes=[TLS_DH_RSA_WITH_AES_128_GCM_SHA256=false, TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256=false, TLS_PSK_WITH_AES_128_GCM_SHA256=false, TLS_ECDHE_ECDSA_WITH_AES_128_CCM=false, TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8=false, TLS_ECDHE_RSA_WITH_ARIA_128_GCM_SHA256=false, TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256=false, TLS_AES_128_CCM_SHA256=false, TLS_AES_128_CCM_8_SHA256=false, TLS_SHA256_SHA256=false, TLS_SHA384_SHA384=false, TLS_FALLBACK_SCSV=false, TLS_DHE_RSA_WITH_AES_128_GCM_SHA256=false, TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256=false]
U066-SA filter_no_allowed_name=[]
U066-SA hs_unrestricted_allowed=completed protocol=TLSv1.3 suite=TLS_AES_256_GCM_SHA384 byte=42 server=OK
U066-SA hs_server_tls12_allowed=completed protocol=TLSv1.2 suite=TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 byte=42 server=OK
U066-SA hs_tls13_enabled_no_tls13_suite=completed protocol=TLSv1.2 suite=TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 byte=42 server=OK
U066-SA client_tls13_enabled_no_tls13_suite_protocols=TLSv1.2,TLSv1.3
U066-SA hs_server_ecdhe_cbc_client_default=completed protocol=TLSv1.2 suite=TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA byte=42 server=OK
U066-SA hs_server_ecdhe_cbc_client_allowed=refused type=javax.net.ssl.SSLHandshakeException cause=none msg=(handshake_failure) Received fatal alert: handshake_failure server=FAILED:javax.net.ssl.SSLHandshakeException
U066-SA hs_server_rsa_gcm_client_default=refused type=java.net.SocketException cause=none msg=An established connection was aborted by the software in your host machine server=FAILED:javax.net.ssl.SSLHandshakeException
U066-SA hs_client_empty_suites=refused type=javax.net.ssl.SSLHandshakeException cause=none msg=No appropriate protocol (protocol is disabled or cipher suites are inappropriate) server=FAILED:javax.net.ssl.SSLHandshakeException
```

`hs_server_rsa_gcm_client_default` shows that SunJSSE refuses `TLS_RSA_*` whatever is enabled (L10). That is
why AC-6 is asserted under Conscrypt only.

## 2. Design (decided)

### 2.0 Principles applied (A1, expert-depth-check criteria 1 and 2)

Book and paper chapters below are cited as the expert-depth-check gives them; I did not re-read them in this
session. The RFC and CWE citations in 2.0 and 2.1 were checked against their published text on 2026-11-21.

- **Allow-list (DP-2) and the fail-closed null branch (2.2): fail-safe defaults.** Saltzer and Schroeder, "The
  Protection of Information in Computer Systems" (1975), section I.A.3, design principle (b): "base access
  decisions on permission rather than exclusion". A suite is enabled only because it is named, never because it
  is absent from a list. The cost this principle accepts is R9: a future suite stays off until someone adds it.
- **The deny-list being replaced: CWE-184, "Incomplete List of Disallowed Inputs".** The old blocked list held
  only `SSL_*` forms and RC4 names. Any weak `TLS_*` name nobody had listed stayed enabled (CR-014 Scope
  Concern B).
- **The old null branch: CWE-636, "Not Failing Securely"** (the current title). Fail-safe defaults answers it.
- **Removing the old lists rather than keeping a second, blocked list:** Fowler, *Refactoring* (2nd ed.), "Remove
  Dead Code" (ch. 8). A blocked list beside an allow-list is not defence in depth. No filter output can depend
  on it and no test can detect it being emptied, so it is a second place to edit with no effect.
- **The test design:** characterisation tests (Feathers, *Working Effectively with Legacy Code*, ch. 13) reached
  through seams (ch. 4): reflection on `protected` members and stand-in sockets. The mutation runs declare in
  advance which mutants cannot be killed on a given provider, and the provider fact that makes each so:
  - **Undetectable on Conscrypt:** M-X4 (i), because TLS 1.3 suites are provider-fixed there.
  - **Undetectable on SunJSSE:** M-X1 (b) and (e), because neither name is in SunJSSE's defaults.
  - **Undetectable at the socket level on both providers:** M-X9, because both support all ten.
- **The old DHE-based isolation guard: a Fragile Test** (Meszaros, *xUnit Test Patterns*, ch. 16). It inferred
  the sandbox's provider from production data that the guard did not govern (which cipher suites survive the
  lists), so it broke as soon as that data changed for unrelated reasons (this story's pruning). The rework
  (3.3, 3.4) checks the property the guard exists for: the live provider.

### 2.1 The allowed list and the step

**Where each name comes from** (checked on 2026-11-21 against the RFC Editor texts):
- **The three TLS 1.3 suites** are defined in RFC 8446 Appendix B.4. Section 9.1 says an implementation "MUST
  implement the TLS_AES_128_GCM_SHA256" and "SHOULD implement" the other two. RFC 9325 section 4.3 ("Cipher
  Suites for TLS 1.3") "does not specify any cipher suites for TLS 1.3" and refers to RFC 8446 section 9.1.
- **The four ECDHE AES-GCM TLS 1.2 suites** are exactly the set RFC 9325 section 4.2 ("Cipher Suites for TLS
  1.2") marks RECOMMENDED. They meet section 4.1's "MUST support and prefer to negotiate cipher suites offering
  forward secrecy" and "SHOULD NOT negotiate cipher suites based on RSA key transport".
- **The two ECDHE ChaCha20-Poly1305 TLS 1.2 suites** are defined in RFC 7905 section 2 ("ChaCha20 Cipher
  Suites"). They go **beyond** RFC 9325 section 4.2, which does not mention ChaCha20. They stay by the ruling
  (DP-1, DP-4; story Scope Concern 4): they give forward secrecy and authenticated encryption, and Android
  enables them.
- **`TLS_EMPTY_RENEGOTIATION_INFO_SCSV`** is defined in RFC 5746 section 3.3 as a signalling value: "not a true
  cipher suite ... and cannot be negotiated". It is kept as the no-change choice (DP-4).
- **The TLS 1.2 order** (AES-256 before AES-128, RSA before ECDSA, then ChaCha20) is inherited precedent: the
  factory's order before U-066. It is not a recommendation of RFC 9325, which ranks none of these suites.

- **One ordered constant, `ALLOWED_CIPHERS`**, holding the ten names of L-A in L-A order. It is the allowed set
  and the order at once (CR-014 C3: "the enabled suites are in the allowed list's order"). Filtering after
  `reorder` (the protocol shape) would need a second, preferred-order list with no added meaning. With one
  constant, one snapshot test (H4) and one form test (H8) cover everything.

**Trade-offs** (A1, expert-depth-check criterion 3):

| Decision | Gained | Gives way | Alternative not taken | Why |
|----------|--------|-----------|-----------------------|-----|
| Allow-list (DP-2, ruled) | "No weak suite is enabled" becomes true for names nobody has listed (fail-safe defaults); the same shape as `ALLOWED_PROTOCOLS` | A future suite needs a code change (R9); a TLS 1.2-only server without an ECDHE AEAD suite is refused (DP-1's cost) | Extend the blocked list with the 25 recorded weak names | The story's DP-2 rationale: "only an allow-list can make 'no weak suite is enabled' true for names no one has listed". A deny-list reaches only the names someone records (CWE-184) |
| One constant as set and order | One source of truth; one snapshot (H4) and one form test (H8) | Set and order cannot vary independently: "allowed but not preferred" cannot be expressed, and every reorder changes H4 and each exact-list literal in the same step (a recorded revision under CR-014 Part E) | An allowed set filtered after `reorder` with a separate preferred order (the protocol shape) | Under DP-4 the order is fixed and is the list, so the second list would carry no meaning and would be one more snapshot to keep in step |
| Guard rework: check the live provider | It fires exactly when the sandbox's provider is wrong (G-1, G-2) and no longer depends on cipher data | It no longer observes the factory's static cache. That is safe only while the cache is provider-independent (both providers now give L-A and `[TLSv1.2, TLSv1.3]`, plan-review "When a guard stays silent"); a future change that made the cache differ by provider again would need a cache-based marker again (stated in the class Javadocs) | A different cache-based marker that still differs by provider | None exists: after U-066 both static lists are identical on both providers, so any cache-based marker would be a guard that can never fire (AC-8 (b)) |
- **A new step, `ciphersToEnable(SSLSocket)`**, modelled on `protocolsToEnable`. It walks `ALLOWED_CIPHERS` in
  order and keeps each name that `socket.getEnabledCipherSuites()` contains. It reads the **default-enabled**
  suites, never the supported ones: C1, CR-014 point 9, and the JDK throws on an unsupported name (L11). The
  output is in allowed order whatever order the platform uses (H7 (ii)).
- **The static initialiser** calls the step on its probe socket (`:110`), and `ENABLED_CIPHERS` becomes its
  result (`:124-125`). The try block keeps its order, suites and then protocols, so a throw still leaves both
  lists null, as today.
- **`ORDERED_KNOWN_CIPHERS` and `BLACKLISTED_CIPHERS` are removed.** Kept beside the allow-list, a blocked list
  would be dead: nothing it names can pass the filter, so no test could fail if it were emptied. That is the
  story's own rule ("otherwise it is dead and goes"), and M-X3 does not apply. **`reorder` stays byte-identical
  in code** because `protocolsToEnable` uses it (`:173`). Only its comment at `:153-154`, which states a
  cipher-suite rationale that no longer applies, is rewritten (E5).
- **Tests that read the removed constants by reflection** (L6: Hardening `:212`, `:242`, `:257`, `:262`,
  `:272`, `:274`) are retargeted. The three `reorder` tests pass the pre-U-066 lists as test literals
  (`KNOWN_CIPHERS_BEFORE_U066`, `BLOCKED_CIPHERS_BEFORE_U066`, the old baselines renamed), so they still pin
  `reorder`, the protocol step's function (AC-7 (b)). `cipherConstants_matchRecordedBaseline` reads
  `ALLOWED_CIPHERS` (AC-7 (a)). The old values stay visible in the test as those two literals.

### 2.2 The failed-initialisation branch (DP-3 option 1, C4)

`sock.setEnabledCipherSuites(ciphers != null ? ciphers : ciphersToEnable(sock));` mirrors the protocol line.
The branch now sets exactly the allowed suites the socket enables by default, once. If the step throws, the
exception leaves `createSocket` and no socket is returned (fail closed, as for protocols; CR-014 C4). Before
this change, the cipher side failed open (CWE-636, "Not Failing Securely"). The protocol side was closed by
U-065, and this closes the cipher side by fail-safe defaults (2.0). The real initialiser failure is not forced:
it runs once per class loader (U-065 AC-1 (e)). H9 calls the branch directly.

### 2.3 Empty result (CR-014 C2's second sentence; Check point 4; B2)

C2's second sentence reads: "If the platform enables none of the allowed suites by default, the producer
enables none and never falls back to another suite." If the platform enables none of the ten, the step returns
an empty array and `hardenSocket` sets it, so the producer enables none and never falls back. The tests show
this in three places:
- at the step, H7 (iv);
- in the null branch, H9 (b);
- on the normal path with a non-null empty list, H9 (c), against M-X10 (A1, contract-verification O-1).

What the platform does with an empty array is **recorded, not asserted**. On plain SunJSSE the array is accepted (0 enabled) and a handshake is refused with
`SSLHandshakeException` (L11). Under Conscrypt it is expected to be accepted with the TLS 1.3 suites still
reported (provider-fixed), so a TLS 1.3 handshake could still complete. The probe measures this (`c_set_empty`,
`c_hs_empty_suites`, `j_set_empty`). The two providers behave differently, so pinning either result would pin a
platform detail that CR-014 Part D leaves "Not stated". Implementation Notes record the probe lines.

### 2.4 Unchanged: protocols, server name, trust, signatures

`ALLOWED_PROTOCOLS`, `ORDERED_KNOWN_PROTOCOLS`, `BLACKLISTED_PROTOCOLS`, `protocolsToEnable` (`:160-185`) and
the protocol line `:240` are untouched, and so are `createSocket` (`:189-217`), the public `hardenSocket`
(`:219-228`) and the SNI code (`:243-318`). `PinnedCertificateSocketFactory.java` changes in one Javadoc line
only (E8, A1; its code, including `createSocket`, is byte-identical and its compiled classes are shown to be
identical). `K9MailTransport.java`, `TlsTestFixtures.java` and the build files are not edited. Call order on the socket stays: suites
and protocols, then the server name. The round trip in `setSniViaSSLParameters` keeps the ten names in order
on SunJSSE (L11), and probe keys `c_/j_roundtrip_allowed_kept` confirm it under Robolectric. J1 asserts it on
the pinned socket (B5).

### 2.5 Provider-fixed suites (DP-4, B3)

Under Conscrypt the three TLS 1.3 suites are reported enabled whatever is set, first, in the provider's order
(`I0-probe.txt:34`). That order is L-A's first three entries. Consequences, all built into the tests and rows:
1. On Conscrypt the exact-list assertion's first three entries are the provider's doing. H5 and H6 assert this
   environment fact (`assertTls13SuitesAreProviderFixed`). Non-vacuity rests on the other seven entries and on
   the absence of the pruned suites: the provider's own default enables `TLS_ECDHE_*_CBC_SHA` and `TLS_RSA_*`,
   and its list differs from L-A (preconditions, AC-1 (a)).
2. On SunJSSE nothing is fixed, so the TLS 1.3 names in the list are the factory's doing (AC-1 (c)). J2 and J3
   carry that check.
3. **M-X4 (i)** (TLS 1.3 names removed from the allowed list) **cannot fail the Conscrypt exact-list tests, nor
   any Conscrypt handshake test**. They are logged as expected-passing. The detectors are J1 to J3 (JDK), H4,
   H7, H8 (the "holds a TLS 1.3 suite" clause, Part H 4) and H9. This differs from the story's table, which
   expects the AC-1 and AC-2 exact-list tests to fail on both providers. The story's own M-X4 note already says
   the handshake tests cannot detect it. Under SunJSSE the removal would have let TLS 1.3 be enabled with no
   TLS 1.3 suite, silently negotiating TLS 1.2 (L11), which is the risk of CR-014 point 10.
4. **M-X5 (ii)** (A1, plan-review O-3), a second declared narrowing of the story's table, which expects "the
   AC-2 tests (exact list, parity) for each". With only the default factory's call removed, the pinned
   factory's sockets are still hardened, so its exact-list tests H6 and J3 cannot fail and are logged as
   expected-passing. M-X5 (ii) is detected by the default factory's exact lists (H5, J2), the parity tests
   (C1, C2, J7, J1's reference comparison) and the refusal tests. Likewise M-X5 (i) leaves H5 and J2 passing.

### 2.6 Public surface, mutability, IC-2

- Nothing new is public. `ALLOWED_CIPHERS` is `protected static final String[]` and `ciphersToEnable` is
  `protected static`, matching `ALLOWED_PROTOCOLS` and `protocolsToEnable`. The tests reach them by reflection,
  as U-065's do. `protected` keeps them out of the app's code (another package, no subclass) and open to
  reflection.
- The arrays stay mutable, as the U-065 reviews accepted (F-4, N1): making them private or unmodifiable belongs
  with the CR-009 move, which must then retarget the reflection-reading tests (H4, H7, H8, H9, the three
  guards) rather than drop them. Handed over in section 7.
- IC-2: after the change, `ORDERED_KNOWN_CIPHERS` and `BLACKLISTED_CIPHERS` appear nowhere
  (`git grep` exit 1, I9). `ENABLED_CIPHERS` stays (`:34`, the new assignment, the public `hardenSocket`, the
  three guards). `hardenSocket` callers are unchanged (`:213`; pinned `:101`), and `ciphersToEnable` appears
  only in the production file and in Hardening. The pinned Javadoc `:56` stays true for `hardenSocket`. `:57`
  would become stale, and E8 corrects it (Scope Concern P-1, ruled 2026-11-21).

### 2.7 Exact production code (`DefaultTrustedSocketFactory.java`, CRLF; Edit tool; seven edits E1 to E7; plus E8, one comment line in `PinnedCertificateSocketFactory.java`)

**E1** (imports, `:8`). `old_string`: `import java.util.ArrayList;` → `new_string`:
```java
import java.util.ArrayList;
import java.util.Arrays;
```

**E2** (`:37-81`: both cipher arrays, from `    protected static final String[] ORDERED_KNOWN_CIPHERS = {` through the `    };` that closes `BLACKLISTED_CIPHERS`; the blank line `:82` stays) →
```java
    /**
     * The only cipher suites this factory ever enables, in this order, and only those of them that the
     * platform enables by default (story U-066: finding F-1 of the U-065 security review; product owner
     * decision Record 11, answer 2; the orchestrator's rulings of 2026-11-21 under Record 12). Each gives
     * forward secrecy and authenticated encryption: first the three TLS 1.3 suites, in the order in which
     * Conscrypt, the Android provider, lists them; then the four TLS 1.2 ECDHE suites with AES-GCM,
     * strongest first and RSA before ECDSA, the order used before U-066; then the two TLS 1.2 ECDHE suites
     * with ChaCha20-Poly1305; last the renegotiation signalling value, which is not a cipher and is kept
     * as the platform lists it. A suite not in this list is never enabled, including a suite the factory
     * does not know, so a later suite needs an entry here before it is used, as a later protocol version
     * needs one in {@link #ALLOWED_PROTOCOLS}. Until U-066 the factory removed a blocked list, placed a
     * preferred list first and kept every other suite the platform enabled. Sources: RFC 8446 Appendix B.4
     * and section 9.1 (the TLS 1.3 suites); RFC 9325 sections 4.1 and 4.2 (the four AES-GCM suites, which
     * it recommends); RFC 7905 section 2 (the two ChaCha20-Poly1305 suites, beyond RFC 9325's four);
     * RFC 5746 section 3.3 (the signalling value). The TLS 1.2 order is inherited, not an RFC's.
     */
    protected static final String[] ALLOWED_CIPHERS = {
            "TLS_AES_128_GCM_SHA256",
            "TLS_AES_256_GCM_SHA384",
            "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
    };
```
(The constant must precede the static initialiser that reads it. It takes the place of the old arrays, which
did. No Javadoc above contains a quoted suite name, so every mutation `old_string` in I7 is unique.)

**E3** (`:110`). `old_string`: `            enabledCiphers = sock.getEnabledCipherSuites();` → 
```java
            // Only the allowed suites that the platform enables by default, in the allowed
            // order (U-066): see ciphersToEnable.
            enabledCiphers = ciphersToEnable(sock);
```

**E4** (`:124-125`). `old_string`: the two lines `        ENABLED_CIPHERS = (enabledCiphers == null) ? null :` and `                reorder(enabledCiphers, ORDERED_KNOWN_CIPHERS, BLACKLISTED_CIPHERS);` → `        ENABLED_CIPHERS = enabledCiphers;`

**E5** (`:153-154`, comment only). `old_string`: the two lines `        // Add unknown items at the end. This way security won't get worse when unknown ciphers` and `        // start showing up in the future.` →
```java
        // Add unknown items at the end, in their original order. Since U-066 only
        // protocolsToEnable calls this, and it then keeps only ALLOWED_PROTOCOLS.
```

**E6** (insertion after the `    }` that closes `protocolsToEnable`, `:185`; `old_string` is the four lines `        return enabled.toArray(new String[enabled.size()]);`, `    }`, blank, `    private Context context;`, which are unique together) → the same four lines with this inserted between `    }` and the blank line:
```java

    /**
     * The cipher suites this factory enables on {@code socket}: those of {@link #ALLOWED_CIPHERS} that the
     * platform enables by default on it ({@code getEnabledCipherSuites()}, never the supported suites, so
     * the factory never turns on a suite the platform leaves off), in the order of
     * {@link #ALLOWED_CIPHERS}. Every other suite stays off, whatever its name. If the platform enables
     * none of the allowed suites, the result is empty and no other suite is used instead (fail closed);
     * what a platform then does with the empty list, and whether a handshake can complete, is the
     * platform's (CR-014, "Not stated"). The two emulators measured for U-066 (Android 11 and 16) enable
     * all of the allowed suites by default.
     */
    protected static String[] ciphersToEnable(SSLSocket socket) {
        List<String> offered = Arrays.asList(socket.getEnabledCipherSuites());
        List<String> suites = new ArrayList<String>();
        for (String allowed : ALLOWED_CIPHERS) {
            if (offered.contains(allowed)) {
                suites.add(allowed);
            }
        }
        return suites.toArray(new String[suites.size()]);
    }
```

**E7** (`:230-239`: the Javadoc of the three-argument `hardenSocket` and its first three body lines; the
signature line `:236` and the protocol line `:240` are kept byte-identical). Replace the Javadoc `:230-235`
with:
```java
    /**
     * Applies the given lists. A null list (the lists could not be computed at class initialisation)
     * does not leave the platform's suites or protocols: each is computed from {@code sock} itself, so
     * the allowed suites (U-066) and the TLS 1.2 floor (Record 8) still apply. If that computation
     * throws, the exception leaves the calling {@code createSocket} and no socket is returned.
     */
```
and the three lines `        if (ciphers != null) {`, `            sock.setEnabledCipherSuites(ciphers);`, `        }` with:
```java
        sock.setEnabledCipherSuites(ciphers != null ? ciphers : ciphersToEnable(sock));
```

The resulting method:
```java
    protected static void hardenSocket(SSLSocket sock, String[] ciphers, String[] protocols) {
        sock.setEnabledCipherSuites(ciphers != null ? ciphers : ciphersToEnable(sock));
        sock.setEnabledProtocols(protocols != null ? protocols : protocolsToEnable(sock));
    }
```

**Protected ranges** (HEAD lines that no hunk's old range may intersect): `:83-99`, `:112-118`, `:127`,
`:160-185` (E6 shows as an insertion after `:185`, outside), `:187-229`, `:236`, `:240-319`. The class Javadoc
`:30-32` and the public `hardenSocket` Javadoc `:219-225` stay true and are not edited.

**E8** (A1; `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java`, CRLF in the
working tree; one comment line; the orchestrator's ruling of 2026-11-21, story Implementation Notes "Ruling on
the plan's scope concern P-1"). `old_string`, the whole of HEAD line `:57`:
```java
 * newer, where the platform supports them; the same cipher-suite order and blocked list), then
```
→ `new_string`, one line:
```java
 * newer, where the platform supports them; the same allowed cipher suites, in the same order), then
```
The sentence then reads: every socket "gets exactly the default factory's handling, by calling the same two
steps ... {@code hardenSocket} (only TLS 1.2 and newer, where the platform supports them; the same allowed
cipher suites, in the same order), then {@code setSniHost} ...". That is the same allowed suite list and protocol
handling as the default factory, and it names no constant.

**Why the compiled class cannot change:**
- One comment line replaces one comment line, so no line number moves (the `LineNumberTable` is unchanged).
- javac does not compile comments.
- The class reaches `DefaultTrustedSocketFactory` only through method calls (`hardenSocket`, `setSniHost`, `:101-102`), never through a compile-time constant, so E1 to E7 cannot inline anything into it.

**How it is shown:**
- In I5: `git diff -U0 HEAD -- $PIN` has exactly one hunk, `@@ -57 +57 @@`, both of whose lines start with ` * `.
- In I0 and I9: the SHA-256 of the four javac class files (debug and release, the class and `PinnedCertificateSocketFactory$PinnedX509TrustManager`) is equal after the I0 gate and after the I9 gate (row D41).

**Consequences:**
- `createSocket` (`:85-104`) is not edited, so story AC-9's re-run condition for M-E4 and M-E5 does not apply.
- AC-11 (a) is read as the ruling says: "one production file changes in code; one further production file changes in a comment".

## 3. Test Design

### 3.1 Test impact (every row of the story's table)

| Story row | Decision | Test(s) here |
|-----------|----------|--------------|
| Hardening `cipherConstants_matchRecordedBaseline` `:271` | Changed: reads `ALLOWED_CIPHERS` against the new literal `ALLOWED_CIPHERS_BASELINE`; old lists kept as `KNOWN_CIPHERS_BEFORE_U066`, `BLOCKED_CIPHERS_BEFORE_U066` | H4 |
| Hardening `createSocket_enabledCipherSuites_areProviderDefaultsMinusBlockedWithKnownFirst` `:297` | Changed and renamed `createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes` (the old name is false). Exact L-A on direct and layered, preconditions, W check | H5 |
| Pinned twin `:532` | Changed and renamed `pinnedFactory_createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes` | H6 |
| Helper `assertCipherRule` `:662` | Replaced by `assertAllowedCipherList` and the precondition helpers (its rule no longer describes the code) | 3.3 |
| `reorder_ordersKnownItemsFirst_thenUnknownInOriginalOrder` `:235` | Changed: reads the 24-name literal `KNOWN_CIPHERS_BEFORE_U066` (indexes `[0]`, `[1]`, `[19]`, `[23]` still exist), not the removed constant | H2 |
| `reorder_removesEveryBlockedItem` `:205`; `reorder_withNullBlocklistAndEmptyInput_behavesAsRecorded` `:256` | Changed (AC-7 (b), structure forces it: the constants are removed); retargeted to the literals, not dropped | H1, H3 |
| `hardenSocket_withNullLists_stillAppliesTheProtocolFloorFromTheSocket` `:513` | Changed and renamed `hardenSocket_withNullLists_appliesTheAllowedSuitesAndTheProtocolFloorFromTheSocket`; `never()` `:522` removed; suites captured once = L-A; protocols unchanged; empty case | H9 |
| Guards `:180`, `:460`, `:150` | Changed (AC-8): Hardening and Handshake DHE clauses → live-provider check; JDK DHE clause removed (A1-G1 provider check and A1-G3 kept) | 3.3, 3.4, 3.5 |
| JDK `pinnedFactory_createSocket_implicit_setsDnsHostAsOnlyServerName_jdkProvider` `:330` | Changed (B5): adds the literal L-A assertion after the server-name step | J1 |
| JDK `pinnedAndDefaultFactories_sameProtocolsCiphersAndServerNames_jdkProvider` `:495` | Unchanged; precondition holds (JDK default starts `TLS_AES_256_GCM_SHA384`, L-A starts `TLS_AES_128_GCM_SHA256`) | J7 |
| Pinned `createSocket_implicit_…` `:152`, `createSocket_layered_…` `:176` | Unchanged; precondition holds (Conscrypt default 18 names, L-A 10) | C1, C2 |
| Handshake and pinned TLS 1.2 tests `:290`, `:296`; `:251`, `:257` | Changed through their helpers (bodies unchanged): negotiated suite in AEAD12 | K1, K2, P1, P2 |
| All protocol, server-name, trust and selection tests | Unchanged | AC-7 (c) list |
| New | AC-1 JDK: J2. AC-2 JDK: J3. AC-3 (b): H8. AC-3 (c) and B2: H7. AC-5 (b): K3, P3. AC-6 [R]: K4 to K7, P4 to P7 | |

Short names used below. **HARD** (14): H1 to H9 as above; HP `protocolConstants_matchRecordedBaseline`; HR1
`createSocket_enabledProtocols_areProviderSupportedTls12AndNewer_inRecordedOrder`; HR2
`createSocket_legacyProtocols_notEnabled_althoughProviderEnablesThemByDefault`; HL
`createSocket_layeredOverPlainSocket_appliesSameHardening`; HT
`protocolsToEnable_givenSupportedList_keepsOnlySupportedTls12AndNewer`. **JDK** (11): JA `…ipv6EightGroupLiteralHost…`;
JB `…dnsHost_setsServerNameOnRealSocket`; JR1 `…enabledProtocols…_jdkProvider`; JR2
`…legacyProtocols_notEnabledByFactory…`; J1; J4 `pinnedFactory_createSocket_enablesExactlyTls12AndTls13_bothShapes_jdkProvider`;
J5 `…ipLiteralAndBlankHosts…`; J6 `…nameRejectedBySniHostName…`; J7 parity; J2, J3 (new). **HSK** (19): eight
certificate tests (KC); K1, K2; KR = the four old-protocol refusals; K3; K4 to K7. **CST** (15): C1, C2; CA
`handshake_enrolledCertificateForDifferentHostName_isAccepted`; CR `handshake_certificateOtherThanEnrolled_isRejected`;
P1, P2; PR = the four old-protocol refusals; P3; P4 to P7. With SNI 14, Expiry 1 and PTT 8, **ALL7 = 82 cases**
(68 + 14).

### 3.2 Conventions

Literals are copied into each class, never computed from factory constants (AC-1 (b)), following the
repository's pattern (`ALLOWED_PROTOCOLS_BASELINE` is copied per class). Every message names its criterion.
Every wait is bounded by the server's 10 s. No sleeps, no retries, no `Assume`, no `@Ignore`. Test files are LF.

### 3.3 `DefaultTrustedSocketFactoryHardeningTest.java`

Editable HEAD regions: `:18-34`, `:36-111`, `:118-167`, `:175-199`, `:201-276`, `:289-354`, `:504-537`,
`:580-606`, `:657-711`. Everything else is byte-identical: HP `:278-287`, HR1, the BUG-019 comment, HR2, HL,
HT `:356-502`, helpers `:543-578`, `:608-655`, `:713-766`.

**Imports** (`:18-34`): add `import java.util.Collections;` after `import java.util.Arrays;` and
`import java.util.LinkedHashSet;` after `import java.util.HashSet;`. Delete
`import static org.mockito.ArgumentMatchers.any;` and `import static org.mockito.Mockito.never;`, whose only
use was `:522`.

**Class Javadoc** (`:36-111`) → replace the whole comment with:
```java
/**
 * Characterisation tests for the cipher and protocol hardening of {@link DefaultTrustedSocketFactory}
 * (story U-063, Group D, acceptance criteria AC-11 to AC-17; protocol tests changed and tests added by
 * story U-065; cipher tests changed and added by story U-066), run under Robolectric's default TLS
 * provider. At the environment probes (U-063 I0b, U-066 I2) that provider was Conscrypt.
 *
 * <p><b>What the code does, and what these tests pin down.</b> The factory computes two static lists
 * once per class loader. Cipher suites, since U-066 (finding F-1 of the U-065 security review; product
 * owner decision Record 11, answer 2): the suites of a fixed allowed list of ten that the provider
 * enables by default, in the allowed list's order; every other suite stays off. Protocols: the
 * provider's <em>supported</em> protocols with SSLv3 removed, TLS 1.2, 1.1, 1.0 placed first in that
 * order, and then only TLS 1.2 and TLS 1.3 kept (U-065; BUG-019; Record 8). Every socket a factory
 * creates gets both lists; if they could not be computed at class initialisation, both are computed
 * from the socket itself. The tests check the reordering function (still the protocol step's), the
 * protocol constants and the allowed cipher list against literal copies kept here (snapshots, so that
 * an edit to a list is a conscious, reviewed change), the allowed cipher list against the name forms of
 * CR-014's statement C2 (a check that does not depend on the snapshot), the cipher and protocol steps
 * fed given lists through a stand-in socket, and the exact suites on the direct and layered sockets of
 * both factories (BUG-020; Record 9, "Same as default"). Expected lists are literals; the provider
 * default lists recorded by U-063 appear only as input to the cipher step.
 *
 * <p><b>What they cannot prove.</b>
 * <ul>
 *   <li>The lists here are those of a desktop test provider, not of a device (the two emulators measured
 *       for U-066 list the same 18 default suites as this provider; the owner's devices are not shown).
 *   <li>"Enabled" is not "negotiated": no handshake is run in this class; the handshake classes show
 *       which suite and protocol are negotiated and that a server with only pruned suites is refused.
 *   <li>The array of enabled protocols is not a preference order (SC-5); it is recorded as it is.
 *   <li>This provider reports its three TLS 1.3 suites as enabled whatever is set, first, in its own
 *       order, which is the allowed list's (U-066 DP-4). On this provider the first three entries of the
 *       exact cipher list are therefore the provider's doing; the tests assert that fact, and the JDK
 *       provider class shows the factory keeping them where nothing is provider-fixed.
 *   <li>The class initialiser's failure branch (both static lists null) cannot be made to run: it runs
 *       once per class loader and the lists are final. Its code is called directly with null lists.
 *   <li>Under this provider the supported protocols equal the default enabled ones, so the stand-in
 *       socket tests carry the "supported, not enabled" checks for protocols and the "enabled by
 *       default, not supported" check for cipher suites.
 * </ul>
 *
 * <p><b>Mutations that must break each test</b> (ids as in the U-063, U-065 and U-066 mutation logs).
 * <ul>
 *   <li>AC-11 to AC-13 (reorder): M-D1 to M-D7, M-D15 of U-063, which ran against the code before U-065;
 *       since U-066 the cipher lists are test literals, and reorder is the protocol step's function.
 *   <li>U-066 exact lists (both factories, both shapes): a pruned suite put back (M-X1 (a), (b)), the
 *       allow filter removed (M-X2), the signalling value removed (M-X4 (ii)), the step removed from one
 *       factory (M-X5), the layered shape skipped (M-X6, default factory), two entries swapped (M-X8).
 *       Removing the TLS 1.3 names (M-X4 (i)) cannot fail them on this provider (provider-fixed).
 *   <li>U-066 allowed-list snapshot and name forms: M-X1 (a) to (e), M-X4 (i), (ii) (snapshot), M-X8
 *       (snapshot); the forms check also fails under M-X1 and M-X4 (i).
 *   <li>U-066 cipher step: M-X1, M-X2, M-X4, M-X8, and the step reading the supported instead of the
 *       enabled suites (M-X9).
 *   <li>U-066 null-lists test: the cipher fallback removed (M-X7); M-X9 in its empty case.
 *   <li>U-065 protocol tests: M-P1/M-P8, M-P6 (ii), M-D10, M-D11r, M-F1, as recorded there.
 * </ul>
 *
 * <p><b>Isolation guard A1-G2.</b> The before-method fails, naming A1-G2, if the TLS context's provider
 * is not Conscrypt, which would mean the JDK-provider class's Robolectric sandbox was reused here. Until
 * U-066 the guard read this from the factory's static cipher list (a DHE suite appeared only under the
 * JDK provider). Since U-066 both providers give the same ten allowed suites, so the cached lists no
 * longer differ by provider and cannot be wrong for this class, and the guard checks the live provider
 * instead (U-066 AC-8). The SSLv2Hello clause on the protocol list can no longer fire since U-065 and is
 * kept unchanged. On the wrong provider the preconditions of the exact-list tests fail as well.
 *
 * <p><b>After the CR-009 move:</b> a missing field, method or class means this test's seam needs
 * retargeting to the moved class; a failed behavioural assertion is a regression, and the test is
 * never edited to pass.
 */
```

**Constants** (`:118-167`, from the Javadoc line `    /** The 24 ordered known cipher names of the factory, in order (snapshot). */` at `:118` through `    private static final String[] BLOCKED_PROTOCOLS_BASELINE = {"SSLv3"};` at `:167`; A1, plan-review O-1) → replace with the block below. The 24 and 16 names are unchanged, only renamed. The block ends with the two protocol lines of `:166-167`, unchanged, so each field is declared once:
```java
    /**
     * The allowed cipher suites of the factory, in order (snapshot; U-066, DP-1 option A, DP-4). A literal,
     * never read from the factory, so a wrong constant cannot make a test agree with itself.
     */
    private static final String[] ALLOWED_CIPHERS_BASELINE = {
            "TLS_AES_128_GCM_SHA256",
            "TLS_AES_256_GCM_SHA384",
            "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
    };

    /**
     * The factory's 24 ordered known cipher names before U-066 (the U-063 snapshot), kept as test input:
     * the reorder tests feed them to reorder, which the protocol step still uses, and the cipher step's
     * input test feeds them to the cipher step. The factory no longer holds this list.
     */
    private static final String[] KNOWN_CIPHERS_BEFORE_U066 = {
            <the 24 names of HEAD :120-143, unchanged, in order>
    };

    /** The factory's 16 blocked cipher names before U-066 (the U-063 snapshot), kept as test input as above. */
    private static final String[] BLOCKED_CIPHERS_BEFORE_U066 = {
            <the 16 names of HEAD :148-163, unchanged, in order>
    };

    /** This provider's (Conscrypt's) default-enabled suites as recorded by U-063 (I0-probe.txt:30). Input only. */
    private static final String[] CONSCRYPT_DEFAULT_CIPHERS_RECORDED = {
            "TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_RSA_WITH_AES_128_GCM_SHA256", "TLS_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA",
            "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
    };

    /** The JDK provider's (SunJSSE's) default-enabled suites as recorded by U-063 (I0c-diag.txt:14). Input only. */
    private static final String[] JDK_DEFAULT_CIPHERS_RECORDED = {
            "TLS_AES_256_GCM_SHA384", "TLS_AES_128_GCM_SHA256", "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384", "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
            "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_256_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA", "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
    };

    /** One name each of W8 (null), W9 (anonymous) and W10 (export), held by no provider and no list (AC-3 (c)). */
    private static final String[] NULL_ANON_EXPORT_EXAMPLES = {
            "TLS_RSA_WITH_NULL_SHA", "TLS_DH_anon_WITH_AES_128_CBC_SHA", "TLS_RSA_EXPORT_WITH_RC4_40_MD5",
    };

    /** Default-enabled suites none of which is allowed (CR-014 C2: the producer then enables none). */
    private static final String[] NO_ALLOWED_SUITE = {
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
    };

    /**
     * Names that a deny-list by W1 to W12 would let through and that the name forms of CR-014 C2 must
     * refuse (CR-014, Check of Amendment 1, point 1): static DH, PSK, CCM, CCM_8, ARIA and Camellia GCM,
     * integrity-only TLS 1.3 suites and the fallback signalling value.
     */
    private static final String[] FORM_ESCAPES = {
            "TLS_DH_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256",
            "TLS_PSK_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_CCM",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8", "TLS_ECDHE_RSA_WITH_ARIA_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256", "TLS_AES_128_CCM_SHA256",
            "TLS_AES_128_CCM_8_SHA256", "TLS_SHA256_SHA256", "TLS_SHA384_SHA384", "TLS_FALLBACK_SCSV",
    };

    /** The renegotiation signalling value: listed with the suites, never negotiated, exempt from W1 to W12. */
    private static final String SCSV = "TLS_EMPTY_RENEGOTIATION_INFO_SCSV";

    /** The weak categories of U-066 AC-3. */
    private static final String[] WEAK_CATEGORY_IDS = {
            "W1", "W2", "W3", "W4", "W5", "W6", "W7", "W8", "W9", "W10", "W11", "W12",
    };

    private static final String[] KNOWN_PROTOCOLS_BASELINE = {"TLSv1.2", "TLSv1.1", "TLSv1"};
    private static final String[] BLOCKED_PROTOCOLS_BASELINE = {"SSLv3"};
```
(A1, plan-review O-1: the region is `:118-167`, so the two protocol lines are replaced by themselves and
appear once. In the diff the hunk ends before them, because they are byte-identical. The blank line `:168` and
the `ALLOWED_PROTOCOLS_BASELINE` Javadoc from `:169` follow unchanged.) The developer copies the 24 and 16 names
from the Read output of `:120-143` and `:148-163`, not by retyping.

A1 re-check of every other replacement region of section 3 against its file at HEAD:
- **Hardening:** class Javadoc `:36` `/**` to `:111` ` */`; guard `:175` `/**` to `:199` `}`; H4 `:266` to `:276`; H5 region `:289` to `:354`; H9 and H6 `:504` to `:537`; `assertCipherRule` `:657` to `:711`.
- **JDK:** guard `:171` comment to `:194` `}`, followed by the method's own `}` at `:195`; firstEntries `:667` to `:673`.
- **Handshake:** guard `:456` `/**` to `:479` `}`; `assertCompletesAt` `:328` to `:341`.
- **Create-socket test:** `:323` to `:336`.

Each starts at a comment or statement start and ends at the matching close. No replacement text re-declares a
line that lies outside its region, and the new helper names (`pinnedFactory`, the four-argument `stubSocket`,
`ciphersToEnable`, `defaultEnabledCiphers`) do not exist in their classes today.

**Guard** (`:175-199`) → 
```java
    /**
     * Isolation guard A1-G2: this class must run on its own provider, Conscrypt. Until U-066 the guard read
     * the provider from the factory's cipher list (a DHE suite meant the JDK provider); since U-066 both
     * providers give the same ten allowed suites, so it checks the TLS context's provider (U-066 AC-8).
     */
    @Before
    public void checkIsolationGuardA1G2() throws Exception {
        String[] protocols = constant("ENABLED_PROTOCOLS");
        String[] ciphers = constant("ENABLED_CIPHERS");
        String provider = SSLContext.getInstance("TLS").getProvider().getName();

        if (protocols != null && Arrays.asList(protocols).contains("SSLv2Hello")) {
            fail("A1-G2: the factory's ENABLED_PROTOCOLS contains SSLv2Hello, so it was computed "
                    + "under the JDK provider: TLS context provider=" + provider
                    + ", first entries=" + firstEntries(protocols));
        }
        if (!"Conscrypt".equals(provider)) {
            fail("A1-G2: this class must run on the Conscrypt provider, but the TLS context provider is "
                    + provider + " (the JDK-provider class's sandbox was probably reused); the factory's"
                    + " ENABLED_CIPHERS first entries=" + firstEntries(ciphers));
        }
    }
```
Why it cannot fire falsely: the provider name is `Conscrypt` in every recorded run of this class's sandbox
(`I0-probe.txt:59`, `I2-probe.txt:1`), and probe I2 re-reads it. A Conscrypt release that renamed itself would
make the guard fire loudly, which is a visible stop, never a silent pass. Why it cannot stay silent when it
matters: since U-066 the only provider-dependent inputs of these tests are read live (the provider's default
socket, the provider-fixed suites, the handshakes). A reused sandbox running this class under SunJSSE gives a
live provider of `SunJSSE`, so the guard fires. A reused sandbox that still reports Conscrypt computes
identical static lists, so there is nothing to catch, and the Conscrypt preconditions (`TLS_RSA_*` in the
defaults) would fail first under SunJSSE (AC-8 (d)). G-1 shows it firing (I7).

**H1 to H3** (`:201-264`): in H1 replace `BLOCKED_CIPHERS_BASELINE` (two times, `:206` and `:214`) by
`BLOCKED_CIPHERS_BEFORE_U066`, and `constant("ORDERED_KNOWN_CIPHERS"), constant("BLACKLISTED_CIPHERS")` by
`KNOWN_CIPHERS_BEFORE_U066, BLOCKED_CIPHERS_BEFORE_U066`. In H2 replace `KNOWN_CIPHERS_BASELINE` (four times)
by `KNOWN_CIPHERS_BEFORE_U066`, and the same two `constant(…)` arguments as in H1. In H3 replace
`String[] known = constant("ORDERED_KNOWN_CIPHERS");` by `String[] known = KNOWN_CIPHERS_BEFORE_U066;` and
`constant("BLACKLISTED_CIPHERS")` by `BLOCKED_CIPHERS_BEFORE_U066`. To each of the three Javadocs, append the
sentence: ` Since U-066 the cipher lists are the factory's lists before U-066, given as literals (the factory no
longer holds them); reorder is still the protocol step's function.` The protocol halves and every assertion are
unchanged.

**H4** (`:266-276`) →
```java
    /**
     * AC-14 (ciphers), as changed by U-066 (AC-7 (a)): the factory's allowed cipher list equals the recorded
     * baseline, in order. A snapshot: any edit fails here and must be a conscious, reviewed change. The two
     * lists recorded here until U-066 (24 known, 16 blocked) are kept above as test input. Why each name may
     * be on the list is checked by allowedCipherConstant_holdsOnlyTheAllowedNameForms, which does not depend
     * on this snapshot.
     */
    @Test
    public void cipherConstants_matchRecordedBaseline() throws Exception {
        assertThat(Arrays.asList(constant("ALLOWED_CIPHERS")))
                .containsExactlyElementsIn(ALLOWED_CIPHERS_BASELINE).inOrder();
    }
```

**H5, H7, H8** (`:289-354`, replacing AC-15's test) →
```java
    /**
     * U-066 AC-1 (this provider), AC-3 (a): the default factory's direct and layered sockets enable exactly
     * the ten allowed suites, in the allowed order. Preconditions (AC-1 (a)): this provider's own default
     * socket enables ECDHE suites with CBC and SHA-1 and RSA key-exchange suites, and its list is not the
     * allowed list, so neither their absence nor the order can be the provider's. The three TLS 1.3 suites
     * are provider-fixed here (asserted, DP-4): the factory's part is the other seven. On another provider
     * the preconditions fail (AC-8 (d)). Was AC-15 of U-063 (provider defaults minus the blocked names, known
     * names first), whose rule the allow-list replaced.
     */
    @Test
    public void createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes() throws Exception {
        assertConscryptDefaultsOfferPrunedSuites(enabledCiphersOf(defaultSocket()));
        assertTls13SuitesAreProviderFixed();
        assertAllowedCipherList("default factory, direct socket", enabledCiphersOf(hardenedSocket()));
        try (Layered layered = new Layered(
                new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication()), HOST)) {
            assertAllowedCipherList("default factory, layered socket",
                    Arrays.asList(layered.socket.getEnabledCipherSuites()));
        }
    }

    /**
     * U-066 AC-3 (c), CR-014 C1 and C2: the cipher step, fed through a stand-in socket, keeps exactly the
     * allowed suites that the socket enables by default, in the allowed order. (i) The input is the union of
     * the two recorded provider defaults, the 40 names of the factory's lists before U-066 and one null,
     * anonymous and export name each (62 names, every category W1 to W12 present, asserted first); (ii) the
     * same names reversed; (iii) a socket that supports every name but enables four by default gives only
     * the allowed ones among those four (never a suite the platform only supports: C1); (iv) a socket that
     * enables none of the allowed suites gives an empty list, with no fallback (C2; the model is case (iv) of
     * the protocol step's test). Mutations: M-X1, M-X2, M-X4, M-X8 ((i)); M-X9 ((iii)).
     */
    @Test
    public void ciphersToEnable_givenDefaultEnabledList_keepsOnlyAllowedSuitesInAllowedOrder() throws Exception {
        String[] protocols = {"TLSv1.2", "TLSv1.3"};
        String[] input = cipherStepInput();
        assertWithMessage("precondition: the input holds 62 distinct names").that(input).hasLength(62);
        for (String category : WEAK_CATEGORY_IDS) {
            boolean present = false;
            for (String name : input) {
                if (weakCategories(name).contains(category)) {
                    present = true;
                    break;
                }
            }
            assertWithMessage("precondition AC-3 (c): the input holds a suite of category " + category)
                    .that(present).isTrue();
        }

        assertWithMessage("(i) provider defaults, the lists before U-066, one null, anonymous and export name")
                .that(Arrays.asList(ciphersToEnable(stubSocket(protocols, protocols, input, input))))
                .containsExactlyElementsIn(ALLOWED_CIPHERS_BASELINE).inOrder();
        List<String> reversed = new ArrayList<>(Arrays.asList(input));
        Collections.reverse(reversed);
        String[] backwards = reversed.toArray(new String[0]);
        assertWithMessage("(ii) the same names reversed: the allowed order, not the platform's")
                .that(Arrays.asList(ciphersToEnable(stubSocket(protocols, protocols, backwards, backwards))))
                .containsExactlyElementsIn(ALLOWED_CIPHERS_BASELINE).inOrder();
        String[] fourByDefault = {
                "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384"};
        assertWithMessage("(iii) only suites the socket enables by default, never one it only supports (C1)")
                .that(Arrays.asList(ciphersToEnable(stubSocket(protocols, protocols, fourByDefault, input))))
                .containsExactly("TLS_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256").inOrder();
        assertWithMessage("(iv) no allowed suite enabled by default: an empty list, no fallback (C2)")
                .that(ciphersToEnable(stubSocket(protocols, protocols, NO_ALLOWED_SUITE, input))).isEmpty();
    }

    /**
     * U-066 AC-3 (b), CR-014 C2: every name on the factory's allowed list, read by reflection, is of one of
     * the allowed name forms (TLS 1.3: starts TLS_AES_ and contains _GCM_, or starts TLS_CHACHA20_POLY1305_;
     * TLS 1.2: TLS_ECDHE_RSA_WITH_ or TLS_ECDHE_ECDSA_WITH_ followed by AES_128_GCM_, AES_256_GCM_ or
     * CHACHA20_POLY1305_; or the signalling value), matches none of W1 to W12, appears once, and the list
     * holds a TLS 1.3 suite and a TLS 1.2 suite (CR-014 Part H 4, V2). Not a snapshot: it says why a name may
     * be there. First the form check is shown able to refuse: every non-allowed name of the step's input and
     * each name a deny-list would miss is refused by it. Mutations: M-X1 (a) to (e), M-X4 (i).
     */
    @Test
    public void allowedCipherConstant_holdsOnlyTheAllowedNameForms() throws Exception {
        List<String> literal = Arrays.asList(ALLOWED_CIPHERS_BASELINE);
        for (String name : cipherStepInput()) {
            if (!literal.contains(name)) {
                assertWithMessage("check of the forms: " + name + " is not allowed, so no allowed form may"
                        + " match it").that(hasAllowedForm(name)).isFalse();
            }
        }
        for (String name : FORM_ESCAPES) {
            assertWithMessage("check of the forms: " + name + " must not match an allowed form")
                    .that(hasAllowedForm(name)).isFalse();
        }

        List<String> allowed = Arrays.asList(constant("ALLOWED_CIPHERS"));
        assertWithMessage("the allowed list holds no name twice; allowed=" + allowed)
                .that(allowed).containsNoDuplicates();
        boolean tls13 = false;
        boolean tls12 = false;
        for (String name : allowed) {
            assertWithMessage("CR-014 C2: " + name + " is not of an allowed name form")
                    .that(hasAllowedForm(name)).isTrue();
            assertWithMessage("AC-3 (b): " + name + " matches a weak category")
                    .that(weakCategories(name)).isEmpty();
            tls13 |= name.startsWith("TLS_AES_") || name.startsWith("TLS_CHACHA20_POLY1305_");
            tls12 |= name.startsWith("TLS_ECDHE_");
        }
        assertWithMessage("CR-014 C2 and V2: the allowed list must hold a TLS 1.3 suite; allowed=" + allowed)
                .that(tls13).isTrue();
        assertWithMessage("CR-014 C2 and V2: the allowed list must hold a TLS 1.2 suite; allowed=" + allowed)
                .that(tls12).isTrue();
    }
```

**H9 and H6** (`:504-537`) →
```java
    /**
     * U-066 AC-4 (DP-3 option 1, CR-014 C4), was U-065 AC-1 (e): the branch taken when the provider query
     * failed at class initialisation (both static lists null). The initialiser cannot be made to fail here
     * (it runs once per class loader and the lists are final), so the branch's code is called directly on
     * stand-in sockets. (a) A socket that enables the step's 62-name input by default and supports every old
     * protocol: the suites set, in one call, are exactly the allowed list, and the protocols are exactly TLS
     * 1.2 and 1.3. (b) A socket that enables none of the allowed suites: the branch sets an empty list and
     * does not fall back to the platform's suites. (c) Outside the null branch, a computed list that is empty
     * (a platform whose defaults hold no allowed suite) is set as it is and never replaced by the platform's
     * suites (CR-014 C2). Until U-066 this test asserted that the suites were left as they were. Mutations:
     * the cipher fallback removed (M-X7); the step reading the supported suites (M-X9, case (b)); an empty
     * list skipped (M-X10, case (c)); the protocol fallback removed (M-F1 of U-065).
     */
    @Test
    public void hardenSocket_withNullLists_appliesTheAllowedSuitesAndTheProtocolFloorFromTheSocket()
            throws Exception {
        String[] everything = {"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};
        String[] input = cipherStepInput();
        SSLSocket socket = stubSocket(everything, everything, input, input);

        hardenSocket(socket, null, null);

        ArgumentCaptor<String[]> suites = ArgumentCaptor.forClass(String[].class);
        verify(socket).setEnabledCipherSuites(suites.capture());
        assertWithMessage("(a) suites set in the null branch").that(Arrays.asList(suites.getValue()))
                .containsExactlyElementsIn(ALLOWED_CIPHERS_BASELINE).inOrder();
        ArgumentCaptor<String[]> protocols = ArgumentCaptor.forClass(String[].class);
        verify(socket).setEnabledProtocols(protocols.capture());
        assertThat(Arrays.asList(protocols.getValue())).containsExactly("TLSv1.2", "TLSv1.3").inOrder();

        SSLSocket none = stubSocket(everything, everything, NO_ALLOWED_SUITE, input);
        hardenSocket(none, null, null);
        ArgumentCaptor<String[]> noneSuites = ArgumentCaptor.forClass(String[].class);
        verify(none).setEnabledCipherSuites(noneSuites.capture());
        assertWithMessage("(b) no allowed suite enabled by default: the branch enables none, no fallback")
                .that(noneSuites.getValue()).isEmpty();

        SSLSocket emptyList = stubSocket(everything, everything, input, input);
        hardenSocket(emptyList, new String[0], new String[] {"TLSv1.2", "TLSv1.3"});
        ArgumentCaptor<String[]> emptySuites = ArgumentCaptor.forClass(String[].class);
        verify(emptyList).setEnabledCipherSuites(emptySuites.capture());
        assertWithMessage("(c) a computed empty list is set as it is, never replaced by the platform's suites (C2)")
                .that(emptySuites.getValue()).isEmpty();
    }

    /**
     * U-066 AC-2 (a) (this provider), AC-3 (a), was U-065 AC-3 (b): the pinned-certificate factory's direct
     * and layered sockets enable exactly the ten allowed suites in the allowed order, checked against the
     * literal, so that the two factories cannot be wrong the same way (BUG-020, Record 9). Preconditions as
     * the default factory's test. Mutations: the pinned call to the shared step removed (M-X5 (i)); M-X1
     * (a), (b); M-X2; M-X4 (ii); M-X8.
     */
    @Test
    public void pinnedFactory_createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes()
            throws Exception {
        assertConscryptDefaultsOfferPrunedSuites(enabledCiphersOf(defaultSocket()));
        assertTls13SuitesAreProviderFixed();
        assertAllowedCipherList("pinned factory, direct socket", enabledCiphersOf(pinnedSocket()));
        try (Layered layered = new Layered(pinnedFactory(), HOST)) {
            assertAllowedCipherList("pinned factory, layered socket",
                    Arrays.asList(layered.socket.getEnabledCipherSuites()));
        }
    }
```

**`providerFixedSuites`** (`:580-606`) → the same method with the Javadoc's "enable only the first baseline
known suite that the provider lists" changed to "enable only the first allowed suite with `_WITH_` (a TLS 1.2
suite) that the provider lists", and the loop changed to:
```java
            for (String name : ALLOWED_CIPHERS_BASELINE) {
                if (name.contains("_WITH_") && enabled.contains(name)) {
                    first = name;
                    break;
                }
            }
            if (first == null) {
                fail("DP-4: no allowed TLS 1.2 suite in the provider default list: " + enabled);
            }
```
(Everything else in the method is byte-identical.)

**`assertCipherRule`** (`:657-711`) → replaced by these helpers (the stand-in overload sits here, so
`stubSocket` `:625-631` stays byte-identical):
```java
    /** The pinned-certificate factory (the other-host leaf enrolled; no handshake in this class). */
    private static PinnedCertificateSocketFactory pinnedFactory() throws Exception {
        return new PinnedCertificateSocketFactory(RuntimeEnvironment.getApplication(), HOST,
                TlsTestFixtures.certificate(TlsTestFixtures.OTHER_HOST_CERT));
    }

    /** A stand-in socket with the given protocols and the given default-enabled and supported suites (U-066). */
    private static SSLSocket stubSocket(String[] supported, String[] enabled, String[] enabledSuites,
                                        String[] supportedSuites) {
        SSLSocket socket = stubSocket(supported, enabled);
        when(socket.getEnabledCipherSuites()).thenReturn(enabledSuites.clone());
        when(socket.getSupportedCipherSuites()).thenReturn(supportedSuites.clone());
        return socket;
    }

    /** Calls the protected static ciphersToEnable of the factory by reflection (U-066). */
    private static String[] ciphersToEnable(SSLSocket socket) throws Exception {
        Method method = DefaultTrustedSocketFactory.class.getDeclaredMethod("ciphersToEnable", SSLSocket.class);
        method.setAccessible(true);
        return (String[]) method.invoke(null, socket);
    }

    /**
     * AC-1 (a) for this provider: its own default socket enables suites of the categories the factory
     * prunes here (ECDHE with CBC and SHA-1; RSA key exchange) and is not the allowed list, so their absence
     * on the factory's socket, and its order, can only be the factory's doing.
     */
    private static void assertConscryptDefaultsOfferPrunedSuites(List<String> defaults) {
        assertWithMessage("precondition AC-1 (a): the provider's default socket must enable an ECDHE suite with"
                + " CBC and SHA-1; defaults=" + defaults)
                .that(containsMatch(defaults, "TLS_ECDHE_", "_CBC_SHA")).isTrue();
        assertWithMessage("precondition AC-1 (a): the provider's default socket must enable an RSA"
                + " key-exchange suite; defaults=" + defaults)
                .that(containsMatch(defaults, "TLS_RSA_", "")).isTrue();
        assertWithMessage("precondition AC-1 (a): the provider's own list must not be the allowed list")
                .that(defaults).isNotEqualTo(Arrays.asList(ALLOWED_CIPHERS_BASELINE));
    }

    /**
     * DP-4, an environment fact: this provider reports exactly the three TLS 1.3 suites as enabled whatever
     * is set, in its own order, which is the allowed list's first three entries (U-066 probe I2).
     */
    private static void assertTls13SuitesAreProviderFixed() throws Exception {
        assertWithMessage("environment (DP-4): the suites this provider reports whatever is set")
                .that(providerFixedSuites())
                .containsExactly(ALLOWED_CIPHERS_BASELINE[0], ALLOWED_CIPHERS_BASELINE[1],
                        ALLOWED_CIPHERS_BASELINE[2]).inOrder();
    }

    /** AC-1, AC-2: exactly the allowed list, in order; AC-3 (a): no suite of W1 to W12. */
    private static void assertAllowedCipherList(String label, List<String> enabled) {
        assertWithMessage(label + ": the enabled cipher suites must be exactly the allowed list, in its order")
                .that(enabled).containsExactlyElementsIn(ALLOWED_CIPHERS_BASELINE).inOrder();
        for (String suite : enabled) {
            assertWithMessage(label + ": AC-3 (a): " + suite + " matches a weak category")
                    .that(weakCategories(suite)).isEmpty();
        }
    }

    /**
     * The input of AC-3 (c), 62 distinct names in this order: the two recorded provider defaults (18 and 31),
     * the 40 names of the factory's lists before U-066, one null, anonymous and export name.
     */
    private static String[] cipherStepInput() {
        Set<String> names = new LinkedHashSet<>();
        Collections.addAll(names, CONSCRYPT_DEFAULT_CIPHERS_RECORDED);
        Collections.addAll(names, JDK_DEFAULT_CIPHERS_RECORDED);
        Collections.addAll(names, KNOWN_CIPHERS_BEFORE_U066);
        Collections.addAll(names, BLOCKED_CIPHERS_BEFORE_U066);
        Collections.addAll(names, NULL_ANON_EXPORT_EXAMPLES);
        return names.toArray(new String[0]);
    }

    /**
     * The weak categories of U-066 AC-3 that a name matches: W1 CBC, W2 SHA-1 MAC, W3 RSA key exchange, W4
     * static ECDH, W5 DHE, W6 DES and 3DES, W7 DSS, W8 NULL, W9 anonymous, W10 export, W11 RC4, W12 MD5. The
     * renegotiation signalling value is exempt by name: it is not a cipher.
     */
    private static List<String> weakCategories(String name) {
        List<String> found = new ArrayList<>();
        if (SCSV.equals(name)) {
            return found;
        }
        if (name.contains("_CBC_")) found.add("W1");
        if (name.endsWith("_SHA")) found.add("W2");
        if (name.startsWith("TLS_RSA_") || name.startsWith("SSL_RSA_")) found.add("W3");
        if (name.startsWith("TLS_ECDH_")) found.add("W4");
        if (name.startsWith("TLS_DHE_") || name.startsWith("SSL_DHE_")) found.add("W5");
        if (name.contains("DES")) found.add("W6");
        if (name.contains("_DSS_")) found.add("W7");
        if (name.contains("_NULL_")) found.add("W8");
        if (name.toLowerCase(java.util.Locale.ROOT).contains("_anon_")) found.add("W9");
        if (name.contains("EXPORT")) found.add("W10");
        if (name.contains("RC4")) found.add("W11");
        if (name.contains("MD5")) found.add("W12");
        return found;
    }

    /** True if {@code name} is of one of the allowed name forms of CR-014 C2. */
    private static boolean hasAllowedForm(String name) {
        if (SCSV.equals(name)) {
            return true;
        }
        if (name.startsWith("TLS_AES_") && name.contains("_GCM_")) {
            return true;
        }
        if (name.startsWith("TLS_CHACHA20_POLY1305_")) {
            return true;
        }
        for (String exchange : new String[] {"TLS_ECDHE_RSA_WITH_", "TLS_ECDHE_ECDSA_WITH_"}) {
            if (name.startsWith(exchange)) {
                String rest = name.substring(exchange.length());
                return rest.startsWith("AES_128_GCM_") || rest.startsWith("AES_256_GCM_")
                        || rest.startsWith("CHACHA20_POLY1305_");
            }
        }
        return false;
    }

    /** True if a name in {@code names} starts with {@code prefix} and ends with {@code suffix}. */
    private static boolean containsMatch(List<String> names, String prefix, String suffix) {
        for (String name : names) {
            if (name.startsWith(prefix) && name.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
```
(The one-line `if … found.add` statements are as written. The repository's checkstyle does not run on tests,
and no lint gate reads them. If the reviewer prefers braces, adding them is behaviour-neutral.)

### 3.4 `DefaultTrustedSocketFactoryJdkProviderTest.java`

Editable HEAD regions: `:67-80`, `:101-128`, `:134-142`, `:171-194`, `:319-360`, insertion after `:529`,
`:659-673`. Everything else is byte-identical.

- **Javadoc** `:67-80`: append to the paragraph's last sentence: ` Since U-066 (finding F-1 of the U-065
  security review; Record 11) both factories' direct and layered sockets enable exactly the ten allowed cipher
  suites, in the allowed order, although this provider enables 31 by default, among them DHE, DSS and CBC
  suites; nothing is provider-fixed here, so the three TLS 1.3 suites in that list are the factory's.`
- **Javadoc** `:101-119` (mutation list): add before `</ul>`:
  ` *   <li>U-066 exact lists (J2, J3) and the pinned cipher literal after the server-name step (J1): a pruned
  suite this provider offers put back (M-X1 (a), (c), (d)); the allow filter removed (M-X2); the TLS 1.3
  names or the signalling value removed (M-X4); the step removed from one factory (M-X5); the layered shape
  skipped in the default factory (M-X6, J2 and the parity test); two entries swapped (M-X8).`
- **Javadoc** `:121-123`: append: ` Until U-066, guard A1-G1 also required a TLS_DHE_ suite in the factory's cipher
  list; since U-066 both providers give the same ten suites, so that clause is removed (it would fail every run)
  and the provider check carries A1-G1 (U-066 AC-8).`
- **Constants** `:136`: delete `    private static final int CIPHER_ENTRIES_IN_MESSAGES = 5;` (its only use goes).
  After `:142` add:
```java

    /** The allowed cipher suites of the factory, in order (U-066; literal, never read from the factory). */
    private static final String[] ALLOWED_CIPHERS_BASELINE = {
            "TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
    };
```
- **Guard** `:171-194` (from `        // A1-G1, the factory's reflected cipher list must come from SunJSSE: it has DHE suites,` through the `        }` closing `if (!hasDhe)`) →
```java
        // A1-G3 via the factory's static initialiser: reading its cipher list runs it. Until U-066 the list
        // also told the providers apart (a TLS_DHE_ suite meant SunJSSE); since U-066 both providers give the
        // same ten allowed suites, so the provider check above carries A1-G1 (U-066 AC-8).
        try {
            constant("ENABLED_CIPHERS");
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            fail("A1-G3 (via factory static init): the factory's static initialiser could not run: " + e);
        }
```
  Why the JDK guard still guards: its provider check `:159-169` (unchanged since U-063) fails if the context
  provider is not `SunJSSE` or if `Conscrypt` is registered, which is exactly the reused-sandbox case. G-2
  shows it firing after the DHE clause is gone. Left in place, the DHE clause would fail every run (AC-8 (a)).
- **J1** `:319-360`: append to its Javadoc ` Since U-066 the same socket's suites are also compared with the
  allowed list as a literal (CR-014 Check of Amendment 1, point 6).` After the `.containsExactlyElementsIn(Arrays.asList(reference.getEnabledCipherSuites())).inOrder();` statement (`:353`) insert:
```java
            assertWithMessage("U-066 AC-2 (a): cipher suites on the same socket after the server-name step,"
                    + " the allowed list as recorded")
                    .that(Arrays.asList(socket.getEnabledCipherSuites()))
                    .containsExactlyElementsIn(ALLOWED_CIPHERS_BASELINE).inOrder();
```
- **J2, J3** inserted after the parity test's closing `    }` (`:529`), before the blank line and `// Helpers`:
```java

    /**
     * U-066 AC-1 (this provider), AC-3 (a): the default factory's direct and layered sockets enable exactly
     * the ten allowed suites, in the allowed order. Preconditions (AC-1 (a)): this provider's own default
     * socket enables DHE-RSA, DHE-DSS, ECDHE CBC with SHA-1 and CBC with SHA-256 or SHA-384 suites, and its
     * list is not the allowed list. Nothing is provider-fixed here, so the TLS 1.3 suites in the result are
     * the factory's (AC-1 (c)). On another provider the preconditions fail (AC-8 (d)).
     */
    @Test
    public void createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes_jdkProvider()
            throws Exception {
        assertJdkDefaultsOfferPrunedSuites(defaultEnabledCiphers());
        SSLSocket direct = hardenedSocket(DNS_HOST);
        try {
            assertAllowedCipherList("default factory, direct socket",
                    Arrays.asList(direct.getEnabledCipherSuites()));
        } finally {
            closeQuietly(direct);
        }
        try (Layered layered = new Layered(
                new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication()), DNS_HOST)) {
            assertAllowedCipherList("default factory, layered socket",
                    Arrays.asList(layered.socket.getEnabledCipherSuites()));
        }
    }

    /**
     * U-066 AC-2 (a) (this provider), AC-3 (a): the pinned-certificate factory's direct and layered sockets
     * enable exactly the ten allowed suites, in the allowed order, against the literal. Preconditions as
     * above.
     */
    @Test
    public void pinnedFactory_createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes_jdkProvider()
            throws Exception {
        assertJdkDefaultsOfferPrunedSuites(defaultEnabledCiphers());
        SSLSocket direct = pinnedSocket(DNS_HOST);
        try {
            assertAllowedCipherList("pinned factory, direct socket",
                    Arrays.asList(direct.getEnabledCipherSuites()));
        } finally {
            closeQuietly(direct);
        }
        try (Layered layered = new Layered(pinnedFactory(), DNS_HOST)) {
            assertAllowedCipherList("pinned factory, layered socket",
                    Arrays.asList(layered.socket.getEnabledCipherSuites()));
        }
    }
```
- **Helpers** `:659-673` (`closeQuietly(SSLSocket)` stays as is; `firstEntries` is deleted, since its only
  use was the DHE message) → after `closeQuietly(SSLSocket)`:
```java

    /** The default-enabled cipher suites of an untouched default socket of this provider. */
    private static List<String> defaultEnabledCiphers() throws Exception {
        SSLSocket plain = defaultSocket();
        try {
            return Arrays.asList(plain.getEnabledCipherSuites());
        } finally {
            closeQuietly(plain);
        }
    }

    /** AC-1 (a) for this provider: the pruned categories it offers are in its default list, which is not the allowed list. */
    private static void assertJdkDefaultsOfferPrunedSuites(List<String> defaults) {
        assertWithMessage("precondition AC-1 (a): a TLS_DHE_RSA_ suite in the defaults; defaults=" + defaults)
                .that(containsMatch(defaults, "TLS_DHE_RSA_", "")).isTrue();
        assertWithMessage("precondition AC-1 (a): a TLS_DHE_DSS_ suite in the defaults; defaults=" + defaults)
                .that(containsMatch(defaults, "TLS_DHE_DSS_", "")).isTrue();
        assertWithMessage("precondition AC-1 (a): an ECDHE suite with CBC and SHA-1 in the defaults")
                .that(containsMatch(defaults, "TLS_ECDHE_", "_CBC_SHA")).isTrue();
        assertWithMessage("precondition AC-1 (a): a CBC suite with SHA-256 or SHA-384 in the defaults")
                .that(containsMatch(defaults, "", "_CBC_SHA256") || containsMatch(defaults, "", "_CBC_SHA384"))
                .isTrue();
        assertWithMessage("precondition AC-1 (a): the provider's own list must not be the allowed list")
                .that(defaults).isNotEqualTo(Arrays.asList(ALLOWED_CIPHERS_BASELINE));
    }

    /** AC-1, AC-2: exactly the allowed list, in order; AC-3 (a): no suite of W1 to W12. */
    private static void assertAllowedCipherList(String label, List<String> enabled) {
        assertWithMessage(label + ": the enabled cipher suites must be exactly the allowed list, in its order")
                .that(enabled).containsExactlyElementsIn(ALLOWED_CIPHERS_BASELINE).inOrder();
        for (String suite : enabled) {
            assertWithMessage(label + ": AC-3 (a): " + suite + " matches a weak category")
                    .that(weakCategories(suite)).isEmpty();
        }
    }
```
  and then the same `weakCategories` and `containsMatch` methods as in 3.3, copied verbatim. In this class,
  write `"TLS_EMPTY_RENEGOTIATION_INFO_SCSV".equals(name)` in place of `SCSV.equals(name)`.

### 3.5 `DefaultTrustedSocketFactoryHandshakeTest.java`

Editable: imports `:19` (add `import java.util.List;` after `import java.util.Arrays;`); Javadoc `:73-79`,
`:103-115`, `:125-130`; constants after `:145`; insertion after `:326`; `:328-341`; insertion after `:360`;
guard `:456-479`. Everything else is byte-identical.

- **Javadoc** after the protocol-floor bullet (ends `:78`), add:
  ` *   <li>Cipher suites (U-066; F-1 of the U-065 security review; Record 11): the TLS 1.2 handshakes negotiate a
  TLS 1.2 suite of the allowed list (ECDHE with AES-GCM or ChaCha20-Poly1305); against a server that limits
  nothing the handshake negotiates TLS 1.3 with a TLS 1.3 suite; against a server limited to TLS 1.2 and to
  one pruned suite (ECDHE with CBC and SHA-1, or RSA key exchange with GCM) the handshake fails with an
  {@link SSLException} that is not a certificate rejection, and the server does not report {@code OK}.</li>`
- **Javadoc** `:112-114` mutation list: add
  ` *   <li>U-066 AC-6: the refusal tests break when a pruned suite is put back (M-X1 (a) or (b), each for its own
  suite), the allow filter is removed (M-X2), the default factory's step is removed (M-X5 (ii)), or, for the
  STARTTLS ones, the layered shape is skipped (M-X6). The TLS 1.3 test is a control.</li>`
- **Javadoc** `:125-130` (guard paragraph) → ` * <p><b>Isolation guard A1-G2.</b> The before-method fails,
  naming A1-G2, if the TLS context's provider is not Conscrypt, which would mean the JDK-provider class's
  Robolectric sandbox was reused here. Until U-066 the guard read this from the factory's static cipher list (a
  DHE suite appeared only under the JDK provider); since U-066 both providers give the same ten allowed suites,
  so it checks the live provider (U-066 AC-8). The SSLv2Hello clause can no longer fire since U-065 and is kept
  unchanged.`
- **Constants** after `private static final int MAX_CAUSE_STEPS = 20;`:
```java

    /** The TLS 1.3 suites of the factory's allowed list (U-066). */
    private static final List<String> TLS13_SUITES = Arrays.asList(
            "TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256");

    /** The TLS 1.2 suites of the factory's allowed list (U-066): ECDHE with AES-GCM or ChaCha20-Poly1305. */
    private static final List<String> AEAD12_SUITES = Arrays.asList(
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256");
```
- **K3 to K7**, inserted after `startTls_serverLimitedToTls10_handshakeRefused` (`:326`):
```java

    /**
     * U-066 AC-5 (b), default factory: against a server that limits nothing, the handshake completes at TLS
     * 1.3 with one of the three TLS 1.3 suites, so the pruning leaves TLS 1.3 its suites. A control: under this
     * provider the TLS 1.3 suites are provider-fixed (DP-4), so no U-066 mutation is expected to break it; the
     * JDK provider class's exact-list tests carry the TLS 1.3 names (M-X4 (i)).
     */
    @Test
    public void implicitTls_unrestrictedServer_negotiatesTls13WithAnAllowedSuite() throws Exception {
        server = LoopbackTlsTestServer.start(IMPLICIT_TLS,
                serverContext(RIGHT_HOST_CERT, RIGHT_HOST_KEY_PKCS8, TRUSTED_CA_CERT));
        SSLSocket ssl = connectClient(IMPLICIT_TLS);

        ssl.startHandshake();
        int data = ssl.getInputStream().read();

        assertWithMessage("the byte the server writes after the handshake").that(data)
                .isEqualTo(LoopbackTlsTestServer.PAYLOAD);
        assertWithMessage("negotiated protocol").that(ssl.getSession().getProtocol()).isEqualTo("TLSv1.3");
        assertWithMessage("negotiated suite (U-066 AC-5 (b))").that(TLS13_SUITES)
                .contains(ssl.getSession().getCipherSuite());
        assertWithMessage("server outcome").that(server.awaitOutcome()).isEqualTo("OK");
    }

    /** U-066 AC-6, implicit TLS: a server limited to TLS 1.2 and to ECDHE with AES-CBC and SHA-1 is refused. */
    @Test
    public void implicitTls_serverLimitedToEcdheCbcSha_handshakeRefused() throws Exception {
        assertRefusedByCipherPolicy(IMPLICIT_TLS, "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
    }

    /** U-066 AC-6, STARTTLS, ECDHE with AES-CBC and SHA-1. */
    @Test
    public void startTls_serverLimitedToEcdheCbcSha_handshakeRefused() throws Exception {
        assertRefusedByCipherPolicy(STARTTLS, "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
    }

    /** U-066 AC-6, implicit TLS: a server limited to TLS 1.2 and to RSA key exchange with AES-GCM is refused. */
    @Test
    public void implicitTls_serverLimitedToRsaKeyExchangeGcm_handshakeRefused() throws Exception {
        assertRefusedByCipherPolicy(IMPLICIT_TLS, "TLS_RSA_WITH_AES_128_GCM_SHA256");
    }

    /** U-066 AC-6, STARTTLS, RSA key exchange with AES-GCM. */
    @Test
    public void startTls_serverLimitedToRsaKeyExchangeGcm_handshakeRefused() throws Exception {
        assertRefusedByCipherPolicy(STARTTLS, "TLS_RSA_WITH_AES_128_GCM_SHA256");
    }
```
- **`assertCompletesAt`** `:328-341`: after the `negotiated protocol` assertion (`:339`) insert:
```java
        assertWithMessage("negotiated suite at " + protocol + " (U-066 AC-5 (a))")
                .that("TLSv1.3".equals(protocol) ? TLS13_SUITES : AEAD12_SUITES)
                .contains(ssl.getSession().getCipherSuite());
```
  and append to its Javadoc ` Since U-066 the negotiated suite must be an allowed suite of that protocol.`
- **Helper**, inserted after `assertRefusedByProtocolFloor` (`:360`):
```java

    /**
     * U-066 AC-6: the right-host chain, a server limited to TLS 1.2 and to one pruned suite; the handshake must
     * fail, not on the certificate, and the server must not report OK. Control (probe I2 and M-X5 (ii)): a
     * client with the provider's default suites completes the same handshake.
     */
    private void assertRefusedByCipherPolicy(LoopbackTlsTestServer.Mode mode, String prunedSuite)
            throws Exception {
        server = LoopbackTlsTestServer.start(mode,
                serverContext(RIGHT_HOST_CERT, RIGHT_HOST_KEY_PKCS8, TRUSTED_CA_CERT),
                new String[] {"TLSv1.2"}, new String[] {prunedSuite});
        SSLSocket ssl = connectClient(mode);

        try {
            ssl.startHandshake();
            fail("the handshake against a server limited to " + prunedSuite + " must be refused, but it"
                    + " completed with " + ssl.getSession().getCipherSuite());
        } catch (SSLException e) {
            assertWithMessage("the refusal must not be a certificate rejection; chain=" + describeChain(e))
                    .that(e.getCause() instanceof CertificateException).isFalse();
            assertWithMessage("server outcome against a client that offers none of the server's suites")
                    .that(server.awaitOutcome()).isNotEqualTo("OK");
        }
    }
```
- **Guard** `:456-479` → the Hardening guard body of 3.3, as a `private static void checkIsolationGuardA1G2()
  throws Exception`, with its Javadoc, except that the second `fail` uses
  `(ciphers == null ? "null" : firstEntries(ciphers))`. This class's `firstEntries` has no null check
  (`:498-500`) and stays byte-identical.

### 3.6 `PinnedCertificateSocketFactoryCreateSocketTest.java`

Editable: Javadoc `:45-48`, `:50-60`, `:76-80`, `:101-113`; insertion after `:283`; constants after `:303`;
`:323-336`; insertion after `:354`. Everything else is byte-identical.

- **Javadoc** `:47-48`: replace `Its cipher suites are also checked against the recorded rule in` / `{@code
  DefaultTrustedSocketFactoryHardeningTest} (U-065 AC-3 (b)).` with `Its cipher suites are also checked against
  the allowed list in {@code DefaultTrustedSocketFactoryHardeningTest} and the JDK provider class (U-066 AC-2).`
  In `:54`, replace `the default factory's cipher-suite order and blocked list` with `the default factory's
  allowed cipher suites in their order (since U-066)`. After the U-065 AC-8 bullet (ends `:79`), add a bullet:
  ` *   <li>U-066 AC-5, AC-6: the TLS 1.2 handshakes negotiate an allowed TLS 1.2 suite; against a server that
  limits nothing the handshake negotiates TLS 1.3 with a TLS 1.3 suite; against a server limited to TLS 1.2 and
  to one pruned suite the handshake fails without a certificate rejection and the server does not report
  {@code OK}.</li>` In the mutation list (`:101-113`), add ` *   <li>U-066 AC-6: the pinned call to the shared
  step removed (M-X5 (i)), a pruned suite put back (M-X1 (a), (b)), the allow filter removed (M-X2).</li>`
- **P3 to P7**, inserted after `handshake_startTls_serverLimitedToTls10_isRefused` (`:283`):
```java

    /** U-066 AC-5 (b), pinned factory: a server that limits nothing; TLS 1.3 with a TLS 1.3 suite (a control). */
    @Test
    public void handshake_implicitTls_unrestrictedServer_negotiatesTls13WithAnAllowedSuite() throws Exception {
        server = LoopbackTlsTestServer.start(LoopbackTlsTestServer.Mode.IMPLICIT_TLS,
                serverContext(OTHER_HOST_CERT, OTHER_HOST_KEY_PKCS8, TRUSTED_CA_CERT));
        SSLSocket ssl = connectPinned(LoopbackTlsTestServer.Mode.IMPLICIT_TLS);

        ssl.startHandshake();
        int data = ssl.getInputStream().read();

        assertWithMessage("the byte the server writes after the handshake").that(data)
                .isEqualTo(LoopbackTlsTestServer.PAYLOAD);
        assertWithMessage("negotiated protocol").that(ssl.getSession().getProtocol()).isEqualTo("TLSv1.3");
        assertWithMessage("negotiated suite (U-066 AC-5 (b))").that(TLS13_SUITES)
                .contains(ssl.getSession().getCipherSuite());
        assertWithMessage("server outcome").that(server.awaitOutcome()).isEqualTo("OK");
    }

    /** U-066 AC-6, implicit TLS: a server limited to TLS 1.2 and ECDHE with AES-CBC and SHA-1 is refused. */
    @Test
    public void handshake_implicitTls_serverLimitedToEcdheCbcSha_isRefused() throws Exception {
        assertPinnedHandshakeRefusedByCipherPolicy(LoopbackTlsTestServer.Mode.IMPLICIT_TLS,
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
    }

    /** U-066 AC-6, STARTTLS, ECDHE with AES-CBC and SHA-1. */
    @Test
    public void handshake_startTls_serverLimitedToEcdheCbcSha_isRefused() throws Exception {
        assertPinnedHandshakeRefusedByCipherPolicy(LoopbackTlsTestServer.Mode.STARTTLS,
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
    }

    /** U-066 AC-6, implicit TLS: a server limited to TLS 1.2 and RSA key exchange with AES-GCM is refused. */
    @Test
    public void handshake_implicitTls_serverLimitedToRsaKeyExchangeGcm_isRefused() throws Exception {
        assertPinnedHandshakeRefusedByCipherPolicy(LoopbackTlsTestServer.Mode.IMPLICIT_TLS,
                "TLS_RSA_WITH_AES_128_GCM_SHA256");
    }

    /** U-066 AC-6, STARTTLS, RSA key exchange with AES-GCM. */
    @Test
    public void handshake_startTls_serverLimitedToRsaKeyExchangeGcm_isRefused() throws Exception {
        assertPinnedHandshakeRefusedByCipherPolicy(LoopbackTlsTestServer.Mode.STARTTLS,
                "TLS_RSA_WITH_AES_128_GCM_SHA256");
    }
```
- **Constants**: after `OLD_PROTOCOLS` (`:303`), the same `TLS13_SUITES` and `AEAD12_SUITES` as in 3.5.
- **`assertPinnedHandshakeCompletesAt`** `:323-336`: after its `negotiated protocol` assertion (`:334`), the
  same three-line suite assertion as in 3.5.
- **Helper**, inserted after `assertPinnedHandshakeRefused` (`:354`):
```java

    /** U-066 AC-6: the enrolled certificate, a server limited to TLS 1.2 and one pruned suite; the handshake must fail. */
    private void assertPinnedHandshakeRefusedByCipherPolicy(LoopbackTlsTestServer.Mode mode, String prunedSuite)
            throws Exception {
        server = LoopbackTlsTestServer.start(mode,
                serverContext(OTHER_HOST_CERT, OTHER_HOST_KEY_PKCS8, TRUSTED_CA_CERT),
                new String[] {"TLSv1.2"}, new String[] {prunedSuite});
        SSLSocket ssl = connectPinned(mode);

        try {
            ssl.startHandshake();
            fail("the handshake against a server limited to " + prunedSuite + " must be refused, but it"
                    + " completed with " + ssl.getSession().getCipherSuite());
        } catch (SSLException e) {
            assertWithMessage("the refusal must not be a certificate rejection; direct cause="
                    + e.getCause()).that(e.getCause() instanceof CertificateException).isFalse();
            assertWithMessage("server outcome against a client that offers none of the server's suites")
                    .that(server.awaitOutcome()).isNotEqualTo("OK");
        }
    }
```

### 3.7 `LoopbackTlsTestServer.java` (test-only, additive; AC-6)

- After the "Protocol limit (U-065)" paragraph (`:39-42`) add:
  ` * <p><b>Cipher-suite limit (U-066).</b> {@link #start(Mode, SSLContext, String[], String[])} also limits the
  server's TLS socket to the given cipher suites, set right after the protocols and before its handshake, so that
  a test can show that a client refuses a server offering only pruned suites. The other two {@code start} methods
  leave the suites as the context enables them, exactly as before.`
- Constructor `:84-93`: the signature gets `, final String[] serverSuites` after `serverProtocols`, and the
  submit becomes `serve(mode, serverContext, serverProtocols, serverSuites)`.
- `start(Mode, SSLContext)` `:100`: `new LoopbackTlsTestServer(mode, serverContext, null, null)`.
  `start(…, String[])` `:113`: `new LoopbackTlsTestServer(mode, serverContext, serverProtocols.clone(), null)`.
  Behaviour of both is unchanged.
- New method after `:114`:
```java

    /**
     * Starts a server as {@link #start(Mode, SSLContext, String[])} does, whose TLS socket also enables only
     * the given cipher suites (story U-066, AC-6). Both arrays must name at least one entry; the client is not
     * touched. The caller must {@link #close()} it.
     */
    public static LoopbackTlsTestServer start(Mode mode, SSLContext serverContext, String[] serverProtocols,
                                              String[] serverSuites) throws IOException {
        if (serverProtocols == null || serverProtocols.length == 0) {
            throw new IllegalArgumentException("serverProtocols must name at least one protocol");
        }
        if (serverSuites == null || serverSuites.length == 0) {
            throw new IllegalArgumentException("serverSuites must name at least one cipher suite");
        }
        return new LoopbackTlsTestServer(mode, serverContext, serverProtocols.clone(), serverSuites.clone());
    }
```
- `serve` `:116`: the signature gets `, String[] serverSuites`. After the protocol block `:129-131` insert:
```java
            if (serverSuites != null) {
                tls.setEnabledCipherSuites(serverSuites);
            }
```

## 4. Implementation Steps

### 4.0 Environment and conventions (every step)

```bash
# From the repository root. Never set MSYS_NO_PATHCONV for Gradle.
unset MSYS_NO_PATHCONV
W=sdlc/artifacts/build/sprints/sprint-022/U-066; E=$W/evidence; M=$E/mutations
T=sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools      # U-063 tools, read-only
INIT=$T/junit-xml.init.gradle
R=app/build/test-results/testDebugUnitTest
DTSF=k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
PIN=app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
TD=app/src/test/java/com/zegoggles/smssync/mail
LTS=$TD/ssl/LoopbackTlsTestServer.java
HARD=$TD/ssl/DefaultTrustedSocketFactoryHardeningTest.java
JDKT=$TD/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java
HSK=$TD/ssl/DefaultTrustedSocketFactoryHandshakeTest.java
CST=$TD/PinnedCertificateSocketFactoryCreateSocketTest.java
START=fba733e5787b196b76c253a756f3f1c82b487cf2
BASE=b9fd172fbc397ed43c451379a2c41a9cae1fc605
DTSF_BLOB=43e5a78ead7c4c32a6af2734514ab8ba5d5ccbd3
PIN_BLOB=21cc5f345977ab6fa161b2aa6925a04aea6a1479
LTS_BLOB=a0899262f0d858dd399ba078c21311fc2d763cab
CR14=sdlc/artifacts/build/change-records/CR-014-cntr-001-state-protocol-floor-and-cipher-handling.md
PINC=app/build/intermediates/javac      # A1: the pinned factory's four javac class files live under it
PINCLS="$PINC/debug/compileDebugJavaWithJavac/classes/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.class $PINC/debug/compileDebugJavaWithJavac/classes/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory\$PinnedX509TrustManager.class $PINC/release/compileReleaseJavaWithJavac/classes/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.class $PINC/release/compileReleaseJavaWithJavac/classes/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory\$PinnedX509TrustManager.class"
WORK=<a u066-work folder inside the developer's own session scratch folder>   # raw logs, pristine copies
```
Shell state does not persist, so each call sets what it needs. `WORK` is the only absolute path and is never
written into a repository file.

- **Class sets** (`--tests`): HARD-C `com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryHardeningTest`;
  JDK-C `…ssl.DefaultTrustedSocketFactoryJdkProviderTest`; HSK-C `…ssl.DefaultTrustedSocketFactoryHandshakeTest`;
  SNI-C `…ssl.DefaultTrustedSocketFactorySniTest`; CST-C `com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest`;
  EXP-C `…mail.PinnedCertificateSocketFactoryExpiryTest`; PTT-C `…mail.PinnedCertificateSocketFactoryTest`.
  **ALL7** = all seven (82 cases at the end). **FOUR** = HARD-C, JDK-C, HSK-C, CST-C.
- **TR(name, classes…)**: every Gradle call runs in the foreground with the tool timeout at 600000 ms. The
  results folder is deleted first and the init script is always passed:
  ```bash
  rm -rf $R; ./gradlew -I "$INIT" --console=plain :app:testDebugUnitTest --tests "<fqcn1>" [--tests "<fqcn2>" ...] > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> $WORK/logs/<name>.log
  python -I -X utf8 $T/sanitize.py $WORK/logs/<name>.log $WORK/logs/<name>.san --grep '^(BUILD |FAILURE|U063-DAEMON-JVM|exit=)|tests completed|FAILED$'
  python -I -X utf8 $T/junit_cases.py $R <filter...> > <dest>-cases.txt
  ```
  For failure messages: `grep -o 'message="[^"]*"' $R/TEST-<fqcn>.xml > $WORK/logs/<name>-msg.log; python -I -X utf8 $T/sanitize.py $WORK/logs/<name>-msg.log <dest>-message.txt`.
- **Gate filter G**: `--grep '^(BUILD |FAILURE|> Task :app:(assemble|test|jacoco)|U063-DAEMON-JVM|exit=)|tests completed|FAILED$'`.
- **Timestamps** come from `date -u +%Y-%m-%dT%H:%M:%SZ`, run at that moment.
- **Expected Gradle invocations: 22.** I0 gate (1), I2 probe (1), I4 red (1), I6 green (1), I7 (15 mutations
  and 2 guard checks), I9 gate (1). Rows D2, D16, D31 and D33 may each add one logged repeat. Determinism is
  shown without extra runs: I9's ALL7 cases are compared with I6's.

### I0 — Baseline, S0 checks and the first gate (no edit before this step passes)

1. `mkdir -p "$WORK/logs" "$WORK/pristine" "$M" "$E/tools"`. One call into `$WORK/logs/I0-baseline.log`,
   sanitised to `$E/I0-baseline.txt`:
   ```bash
   date -u +%Y-%m-%dT%H:%M:%SZ
   git rev-parse HEAD; git rev-parse --abbrev-ref HEAD
   git merge-base --is-ancestor $BASE HEAD; echo "base-ancestor-exit=$?"
   git diff --name-only $START $BASE; echo "s0-2-commit-files-end"
   git status --porcelain -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat; echo "porcelain-end"
   git rev-parse HEAD:$DTSF HEAD:$PIN HEAD:$LTS; git hash-object $DTSF $PIN $LTS
   git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"
   grep -v '^#' gradle/gradle-daemon-jvm.properties
   # S0-2: CR-014 Amendment 1 and its check, committed or in the working tree
   for s in '^status: approved' '^### Amendment 1' 'Check of Amendment 1' 'Every name on the allowed list is of one of these forms' 'removing from it every TLS 1.3 suite' 'Which suites the platform enables or places itself'; do echo "head=$(git show HEAD:$CR14 | grep -c -- "$s") wt=$(grep -c -- "$s" $CR14) :: $s"; done
   git diff --stat HEAD -- $CR14; echo "cr14-wt-diff-end"
   ```
   Expected: HEAD `b9fd172f…` or a descendant, with `base-ancestor-exit=0`. `s0-2-commit-files-end` is preceded
   by `$CR14` only. Nothing before `porcelain-end`. The three HEAD and three working-tree hashes equal the three
   blobs. Count 732; `toolchainVersion=25`. Every S0-2 line has `head ≥ 1` or `wt ≥ 1`. **Stop:** rows D1 (and
   D1-S0 for the S0-2 lines).
2. Gate: `rm -rf $R; ./gradlew -I "$INIT" --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease > $WORK/logs/I0-gates.log 2>&1; echo "exit=$?" >> $WORK/logs/I0-gates.log`.
   Sanitise it with filter G into `$E/I0-gates.txt`, then run
   `python -I -X utf8 $T/junit_totals.py $R > $E/I0-junit-totals.txt`. Expected: `exit=0`,
   `U063-DAEMON-JVM: java.version=25…`, `tests=732 failures=0 errors=0 skipped=2`. Record the skipped count as
   "the start". Then (A1, E8's proof) run `for f in $PINCLS; do echo "$(sha256sum "$f" | cut -d' ' -f1) ${f#$PINC/}"; done > $E/I0-pinned-classes.txt`.
   That writes one hash and the repository-relative path under `app/build/intermediates/javac/` for each of the
   four class files, as built from the HEAD source. Expected: four lines. **Stop:** rows D1, D34, and D41 for a
   missing class file.
3. Create `$W/implementation-log.md` (a header, then the I0 entry with the clock time; it records that S0-1
   and S0-3 are the orchestrator's entries in the story and that S0-2 is `b9fd172f`).

### I1 — The server's cipher-suite option (test-only)

Apply section 3.7 with the Edit tool; no Gradle. Write `git diff --stat HEAD -- $LTS` and
`git diff -U0 HEAD -- $LTS | grep '^@@'` to `$E/I1-lts-diff-check.txt`. Expected: one file; hunks only at the
Javadoc, the constructor, the two `start` bodies, the new method, the `serve` signature and the inserted
block.

### I2 — Probes (one Gradle run; before any other test edit or any production edit)

1. Write two throwaway classes with the Write tool, `$TD/ssl/U066ConscryptProbeTest.java` and
   `$TD/ssl/U066JdkProbeTest.java`:
```java
package com.zegoggles.smssync.mail.ssl;

import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.RIGHT_HOST_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.RIGHT_HOST_KEY_PKCS8;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.TRUSTED_CA_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.serverContext;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.trustManagerFor;

/** THROWAWAY probe for U-066 step I2 under the default (Conscrypt) provider. Deleted after one run. */
@RunWith(RobolectricTestRunner.class)
public class U066ConscryptProbeTest {

    static final String[] ALLOWED = {
            "TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"};
    static final String[] AEAD12 = Arrays.copyOfRange(ALLOWED, 3, 9);
    static final String[] PRUNED = {"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_128_GCM_SHA256"};

    @Test
    public void probe() throws Exception {
        out("provider", SSLContext.getInstance("TLS").getProvider().getName());
        SSLSocket s = clientSocket(null);
        out("default_count", String.valueOf(s.getEnabledCipherSuites().length));
        out("default", String.join(",", s.getEnabledCipherSuites()));
        out("supported_has_all_allowed",
                String.valueOf(Arrays.asList(s.getSupportedCipherSuites()).containsAll(Arrays.asList(ALLOWED))));
        s.close();
        out("set_allowed", setAndRead(ALLOWED));
        List<String> reversed = new ArrayList<>(Arrays.asList(ALLOWED));
        Collections.reverse(reversed);
        out("set_allowed_reversed", setAndRead(reversed.toArray(new String[0])));
        out("set_aead12_only", setAndRead(AEAD12));
        out("set_empty", setAndRead(new String[0]));
        out("set_unknown", setAndRead(new String[] {"TLS_U066_UNKNOWN_SUITE"}));
        out("roundtrip_allowed_kept", roundTrip());
        Field f = DefaultTrustedSocketFactory.class.getDeclaredField("ENABLED_CIPHERS");
        f.setAccessible(true);
        String[] today = (String[]) f.get(null);
        out("today_list_count", String.valueOf(today.length));
        String[] tls12 = {"TLSv1.2"};
        out("hs12_today_list", handshake(tls12, null, today));
        out("hs12_allowed", handshake(tls12, null, ALLOWED));
        out("hs12_provider_default", handshake(tls12, null, null));
        out("hs12_client_order", handshake(tls12, null,
                new String[] {"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"}));
        out("hs13_allowed", handshake(null, null, ALLOWED));
        out("hs_empty_suites", handshake(null, null, new String[0]));
        for (String suite : PRUNED) {
            out("hs_" + suite + "_provider_default", handshake(tls12, new String[] {suite}, null));
            out("hs_" + suite + "_allowed", handshake(tls12, new String[] {suite}, ALLOWED));
        }
    }

    private static String setAndRead(String[] suites) throws Exception {
        SSLSocket s = clientSocket(null);
        try {
            s.setEnabledCipherSuites(suites);
            return "accepted " + String.join(",", s.getEnabledCipherSuites());
        } catch (Exception e) {
            return "threw " + e.getClass().getName();
        } finally {
            s.close();
        }
    }

    private static String roundTrip() throws Exception {
        SSLSocket s = clientSocket(null);
        try {
            s.setEnabledCipherSuites(ALLOWED);
            s.setEnabledProtocols(new String[] {"TLSv1.2", "TLSv1.3"});
            String[] before = s.getEnabledCipherSuites();
            SSLParameters p = s.getSSLParameters();
            p.setServerNames(Collections.singletonList(new SNIHostName("imap.right.test")));
            s.setSSLParameters(p);
            return String.valueOf(Arrays.equals(before, s.getEnabledCipherSuites()));
        } finally {
            s.close();
        }
    }

    /** Client suites null: provider defaults, protocols untouched; otherwise those suites with TLS 1.2 and 1.3. */
    private static String handshake(String[] serverProtocols, String[] serverSuites, String[] clientSuites) {
        LoopbackTlsTestServer server = null;
        SSLSocket client = null;
        try {
            SSLContext ctx = serverContext(RIGHT_HOST_CERT, RIGHT_HOST_KEY_PKCS8, TRUSTED_CA_CERT);
            LoopbackTlsTestServer.Mode mode = LoopbackTlsTestServer.Mode.IMPLICIT_TLS;
            if (serverSuites != null) {
                server = LoopbackTlsTestServer.start(mode, ctx, serverProtocols, serverSuites);
            } else if (serverProtocols != null) {
                server = LoopbackTlsTestServer.start(mode, ctx, serverProtocols);
            } else {
                server = LoopbackTlsTestServer.start(mode, ctx);
            }
            client = clientSocket(new TrustManager[] {trustManagerFor(TRUSTED_CA_CERT)});
            if (clientSuites != null) {
                client.setEnabledCipherSuites(clientSuites);
                client.setEnabledProtocols(new String[] {"TLSv1.2", "TLSv1.3"});
            }
            LoopbackTlsTestServer.connect(client, server.port());
            client.startHandshake();
            int b = client.getInputStream().read();
            return "completed protocol=" + client.getSession().getProtocol() + " suite="
                    + client.getSession().getCipherSuite() + " byte=" + b + " server=" + server.awaitOutcome();
        } catch (Throwable e) {
            return "refused type=" + e.getClass().getName()
                    + " server=" + (server == null ? "none" : server.awaitOutcome());
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ignored) {
                    // probe
                }
            }
            if (server != null) {
                server.close();
            }
        }
    }

    private static SSLSocket clientSocket(TrustManager[] tms) throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, tms, null);
        return (SSLSocket) ctx.getSocketFactory().createSocket();
    }

    private static void out(String key, String value) {
        System.out.println("U066-PROBE c_" + key + "=" + value);
    }
}
```
```java
package com.zegoggles.smssync.mail.ssl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.ConscryptMode;
import org.robolectric.annotation.LooperMode;

import java.util.Arrays;
import java.util.Collections;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

import static org.robolectric.annotation.ConscryptMode.Mode.OFF;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

/** THROWAWAY probe for U-066 step I2 under the JDK provider (the JDK class's annotations). No handshake. */
@RunWith(RobolectricTestRunner.class)
@ConscryptMode(OFF)
@LooperMode(LEGACY)
public class U066JdkProbeTest {

    static final String[] ALLOWED = U066ConscryptProbeTest.ALLOWED;
    static final String[] AEAD12 = U066ConscryptProbeTest.AEAD12;

    @Test
    public void probe() throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, null, null);
        out("provider", ctx.getProvider().getName());
        SSLSocket s = (SSLSocket) ctx.getSocketFactory().createSocket();
        out("default_count", String.valueOf(s.getEnabledCipherSuites().length));
        out("default", String.join(",", s.getEnabledCipherSuites()));
        out("supported_has_all_allowed",
                String.valueOf(Arrays.asList(s.getSupportedCipherSuites()).containsAll(Arrays.asList(ALLOWED))));
        s.setEnabledCipherSuites(ALLOWED);
        out("set_allowed", "accepted " + String.join(",", s.getEnabledCipherSuites()));
        s.setEnabledProtocols(new String[] {"TLSv1.2", "TLSv1.3"});
        String[] before = s.getEnabledCipherSuites();
        SSLParameters p = s.getSSLParameters();
        p.setServerNames(Collections.singletonList(new SNIHostName("imap.right.test")));
        s.setSSLParameters(p);
        out("roundtrip_allowed_kept", String.valueOf(Arrays.equals(before, s.getEnabledCipherSuites())));
        s.close();
        SSLSocket a = (SSLSocket) ctx.getSocketFactory().createSocket();
        a.setEnabledCipherSuites(AEAD12);
        a.setEnabledProtocols(new String[] {"TLSv1.2", "TLSv1.3"});
        out("set_aead12_only", "accepted " + String.join(",", a.getEnabledCipherSuites())
                + " protocols=" + String.join(",", a.getEnabledProtocols()));
        a.close();
        SSLSocket e = (SSLSocket) ctx.getSocketFactory().createSocket();
        try {
            e.setEnabledCipherSuites(new String[0]);
            out("set_empty", "accepted enabled_count=" + e.getEnabledCipherSuites().length);
        } catch (Exception ex) {
            out("set_empty", "threw " + ex.getClass().getName());
        }
        e.close();
    }

    private static void out(String key, String value) {
        System.out.println("U066-PROBE j_" + key + "=" + value);
    }
}
```
   (`U066JdkProbeTest` reads only the two static arrays of the other class. They are plain `String[]`
   initialisers, so no Conscrypt code runs in the JDK sandbox. Values have no brackets, so the extraction
   below cannot cut them.)
2. One run: TR(`I2-probe`, `--tests "com.zegoggles.smssync.mail.ssl.U066ConscryptProbeTest" --tests "com.zegoggles.smssync.mail.ssl.U066JdkProbeTest"`),
   with cases written to `$E/I2-probe-cases.txt` (filter `U066`). Then
   `grep -ho 'U066-PROBE [^]<]*' $R/TEST-com.zegoggles.smssync.mail.ssl.U066*ProbeTest.xml > $WORK/logs/I2-probe.log`
   and `python -I -X utf8 $T/sanitize.py $WORK/logs/I2-probe.log $E/I2-probe.txt`.
3. Copy both sources to `$E/tools/<Name>.java.txt` and delete them from `$TD/ssl/`. Record in
   `$E/I2-cleanup.txt` the output of `git status --porcelain -- app/src/test` (expected: only ` M $LTS`) and
   `ls $TD/ssl | grep -c U066` (expected `0`).
4. Expected lines (the decision rows cover every other value; "∈ AEAD12" means one of the six):
```
U066-PROBE c_provider=Conscrypt
U066-PROBE c_default_count=18
U066-PROBE c_default=<the 18 names of I0-probe.txt:30, comma-joined, same order>
U066-PROBE c_supported_has_all_allowed=true                                          (row D19)
U066-PROBE c_set_allowed=accepted <L-A, comma-joined>
U066-PROBE c_set_allowed_reversed=accepted TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384,TLS_CHACHA20_POLY1305_SHA256,<the other seven of L-A reversed>
U066-PROBE c_set_aead12_only=accepted TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384,TLS_CHACHA20_POLY1305_SHA256,<the six AEAD12 in set order>
U066-PROBE c_set_empty=<accepted … | threw …>                                         (record only)
U066-PROBE c_set_unknown=<accepted … | threw …>                                       (record only)
U066-PROBE c_roundtrip_allowed_kept=true
U066-PROBE c_today_list_count=18
U066-PROBE c_hs12_today_list=completed protocol=TLSv1.2 suite=<∈ AEAD12> byte=42 server=OK
U066-PROBE c_hs12_allowed=completed protocol=TLSv1.2 suite=<∈ AEAD12> byte=42 server=OK
U066-PROBE c_hs12_provider_default=completed protocol=TLSv1.2 suite=<∈ AEAD12> byte=42 server=OK
U066-PROBE c_hs12_client_order=completed protocol=TLSv1.2 suite=<TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA | TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384> byte=42 server=OK
U066-PROBE c_hs13_allowed=completed protocol=TLSv1.3 suite=<one of the three TLS 1.3 suites> byte=42 server=OK
U066-PROBE c_hs_empty_suites=<completed … | refused …>                                (record only)
U066-PROBE c_hs_TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA_provider_default=completed protocol=TLSv1.2 suite=TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA byte=42 server=OK
U066-PROBE c_hs_TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA_allowed=refused type=<an SSLException subclass> server=FAILED:<class>
U066-PROBE c_hs_TLS_RSA_WITH_AES_128_GCM_SHA256_provider_default=completed protocol=TLSv1.2 suite=TLS_RSA_WITH_AES_128_GCM_SHA256 byte=42 server=OK
U066-PROBE c_hs_TLS_RSA_WITH_AES_128_GCM_SHA256_allowed=refused type=<an SSLException subclass> server=FAILED:<class>
U066-PROBE j_provider=SunJSSE
U066-PROBE j_default_count=31
U066-PROBE j_default=<the 31 names of I0c-diag.txt:14, comma-joined, same order>
U066-PROBE j_supported_has_all_allowed=true                                          (row D19)
U066-PROBE j_set_allowed=accepted <L-A, comma-joined>
U066-PROBE j_roundtrip_allowed_kept=true
U066-PROBE j_set_aead12_only=accepted <the six AEAD12 in set order> protocols=TLSv1.2,TLSv1.3   (record only)
U066-PROBE j_set_empty=accepted enabled_count=0                                      (record only)
```
   "SSLException subclass" means `javax.net.ssl.SSLException`, `SSLHandshakeException` or
   `SSLProtocolException`. Log the decision row applied to each key in the implementation log, and record
   the outcome of each conditional row as O, PDF, FIX13, FIXS, SUP, used by the I7 table:
   - **O** = `c_hs12_client_order` negotiated `TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA`, i.e. the server honours the
     client's order.
   - **PDF** = `c_hs12_provider_default`'s suite ∈ AEAD12.
   - **FIX13** = `c_set_aead12_only` holds the three TLS 1.3 names.
   - **FIXS** = `c_set_aead12_only` holds the signalling value although it was not set.
   - **SUP** = both `*_supported_has_all_allowed=true`.

### I3 — Tests (all test edits; no production edit yet)

I3 starts only when every I2 key has a row that lets it continue (a D14 stop needs its reading first). Apply
sections 3.3, 3.4, 3.5, 3.6 with the Edit tool, in that order, leaving out any [R] test that I2 ruled out (rows
D12, D14-b): remove only those methods and keep the helpers. Under D4-b, apply D4-b's text change. Then one
call writing `$E/I3-test-diff-check.txt`:
```bash
for f in $HARD $JDKT $HSK $CST $LTS; do echo "== $f"; git diff -U0 HEAD -- $f | grep '^@@'; done
git diff --stat HEAD -- app/src/test
git diff --quiet HEAD -- $TD/ssl/DefaultTrustedSocketFactorySniTest.java $TD/PinnedCertificateSocketFactoryExpiryTest.java $TD/PinnedCertificateSocketFactoryTest.java $TD/ssl/TlsTestFixtures.java; echo "untouched-exit=$?"
git grep -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"
git grep -n "@Ignore\|Assume" -- $TD/ssl $TD/Pinned*.java; echo "ignore-exit=$?"
git grep -n -E "ORDERED_KNOWN_CIPHERS|BLACKLISTED_CIPHERS|KNOWN_CIPHERS_BASELINE|BLOCKED_CIPHERS_BASELINE" -- app/src/test; echo "old-names-exit=$?"
git diff --quiet HEAD -- app/src/main k9mail-vendored; echo "prod-clean-exit=$?"
```
Expected: every hunk inside the editable regions of 3.3 to 3.7; `untouched-exit=0`; working-tree count 746 (or
as reduced); `ignore-exit=1`; `old-names-exit=1`; `prod-clean-exit=0`. **Stop:** row D20.

### I4 — Red run (tests against the unchanged production code)

TR(`I4-red`, ALL7), with cases written to `$E/I4-red-cases.txt` (filters
`DefaultTrustedSocketFactory PinnedCertificateSocketFactory`). Expected: it compiles, 82 cases (or as reduced),
and **exactly these 17 fail** (8 fewer per suite ruled out by D12):

| Test | First failure (type) |
|------|-----------------------|
| H4 | `constant("ALLOWED_CIPHERS")` (ERROR `java.lang.NoSuchFieldException`) |
| H5 | "default factory, direct socket: the enabled cipher suites must be exactly the allowed list" (A); its preconditions and the DP-4 environment assertion pass first |
| H6 | "pinned factory, direct socket: …exactly the allowed list" (A) |
| H7 | preconditions pass; case (i) reflection (ERROR `java.lang.NoSuchMethodException`) |
| H8 | form self-checks pass; `constant("ALLOWED_CIPHERS")` (ERROR `java.lang.NoSuchFieldException`) |
| H9 | `verify(socket).setEnabledCipherSuites(…)` (A, `org.mockito.exceptions.verification.WantedButNotInvoked`) |
| J1 | the new literal assertion (A); the reference comparison passes (both factories still share the old list) |
| J2, J3 | "…direct socket: …exactly the allowed list" (A); preconditions pass |
| K4 to K7, P4 to P7 [R] | `fail("…must be refused, but it completed with …")` (A, `java.lang.AssertionError`): today's list contains both pruned suites (`I0-probe.txt:31`) |

**Passing by design:** H1 to H3 (`reorder` does not change), the 2 + 2 TLS 1.2 tests (when `c_hs12_today_list`
∈ AEAD12, row D7), K3 and P3 (controls), the guards, and every unchanged test. **Stop:** rows D21 to D24.

### I5 — Production change

1. Apply E1 to E7 (section 2.7) to `$DTSF`, and E8 (A1) to `$PIN`. Nothing else in `app/src/main` or
   `k9mail-vendored`.
2. One call writing `$E/I5-production.txt`:
   ```bash
   git diff --name-only HEAD -- app/src/main k9mail-vendored
   git diff -U0 HEAD -- $DTSF | grep '^@@'
   git diff -U0 HEAD -- $PIN | grep '^@@'; echo "pin-hunks-end"
   git diff -U0 HEAD -- $PIN | grep -E '^[-+]' | grep -vE '^(\+\+\+|---) ' | grep -vcE '^[-+] \* '; echo "pin-non-comment-lines-end"
   git hash-object $DTSF $PIN
   git diff HEAD -- app/src/main k9mail-vendored | git hash-object --stdin
   git grep -n -E "ORDERED_KNOWN_CIPHERS|BLACKLISTED_CIPHERS" -- app/src k9mail-vendored/src; echo "removed-names-exit=$?"
   git grep -n "getSupportedCipherSuites" -- k9mail-vendored/src/main app/src/main; echo "supported-exit=$?"
   git grep -n "ciphersToEnable\|hardenSocket(" -- app/src/main k9mail-vendored/src/main
   ```
   Then:
   - write `git diff HEAD -- app/src/main k9mail-vendored > $E/I5-production.diff.txt`;
   - copy both production files to `$WORK/pristine/` with `cp $DTSF $PIN "$WORK/pristine/"` (A1, plan-review O-2), so the pinned factory's saved copy is the post-E8 file;
   - store both blobs with `git hash-object -w $DTSF $PIN`;
   - record `DTSF_NEW`, `PIN_NEW` and the diff hash `PROD_DIFF` in the implementation log.

   Expected:
   - exactly the two files `$DTSF` and `$PIN`;
   - no `$DTSF` hunk whose old range intersects the protected ranges of 2.7;
   - for `$PIN`, exactly one hunk `@@ -57 +57 @@` before `pin-hunks-end`, and `0` before `pin-non-comment-lines-end` (every changed line starts with ` * `);
   - `removed-names-exit=1` and `supported-exit=1`;
   - `hardenSocket(` only at the `createSocket` call, the two definitions and the one-argument body, plus the pinned `:101`.

   **Stop:** rows D25, D41.

### I6 — Green run

TR(`I6-green`, ALL7), with cases written to `$E/I6-green-cases.txt`. Expected: `exit=0`, 82 cases (or as
reduced), all pass, including the three guards. Then save the final test files: `cp $HARD $HSK $JDKT "$WORK/pristine/"`
and `git hash-object -w $HARD $HSK $JDKT`, recording `HARD_NEW`, `HSK_NEW`, `JDKT_NEW`. This run is the
unmutated baseline. **Stop:** rows D26 to D29.

### I7 — Mutations and guard checks (write-ahead and restore protocol)

`$E/mutation-log.md` header: the title; "Protocol: U-066 plan section I7 (write-ahead entry, one Edit, one
targeted run, restore by copying the pristine file, hash and porcelain check, `restored` line).
Repository-relative paths."; `<start>`, base commit, the base blobs and `DTSF_NEW`, `PIN_NEW`, `HARD_NEW`,
`HSK_NEW`, `JDKT_NEW`, `PROD_DIFF`; the I2 outcomes O, PDF, FIX13, FIXS, SUP; and the recovery note: "If a
session is lost while an entry says `APPLYING` and has no `restored` line, nothing else runs (no gate, no
review, no commit) until the file's hash has been compared with its saved blob (`DTSF_NEW`, `PIN_NEW` or the
test file's `*_NEW`) and, if it differs, the file has been restored from its pristine copy. If that copy is
lost, it is restored from the object store with `git cat-file blob <blob> > <path>` (this writes LF line
endings; with the files stored LF in the index, `git hash-object` and the diff hash still match). If both are
lost, the plan's text (E1 to E8, section 3) is re-applied to the HEAD file and the hash checked. `git checkout`
is never used for a production file: it would discard E1 to E8."

**Declared deviation from AC-9's literal protocol.** The production and test changes are uncommitted while the
mutations run. The restore therefore copies the pristine file back rather than running `git checkout`, for both
production files alike. The check "`git status --porcelain -- app/src/main k9mail-vendored` lists only this
story's files" is read as follows:
- it lists exactly ` M $DTSF` and ` M $PIN` (the two files of I5);
- `git hash-object $DTSF $PIN` equals `DTSF_NEW` and `PIN_NEW`;
- `git diff HEAD -- app/src/main k9mail-vendored | git hash-object --stdin` equals `PROD_DIFF`.

For G-1 and G-2, the test file's hash must equal its `*_NEW` blob.

For each entry, one at a time:
1. Clean check: the hashes equal their blobs, porcelain is as above, and the diff hash equals `PROD_DIFF`.
2. Write-ahead entry `### <id> APPLYING <clock>`: the file, its hash before, the edit, the class run and the
   expected result.
3. One Edit. Read the lines first and copy `old_string` from the Read output.
4. One TR, with cases written to `$M/<id>-cases.txt` and failure messages to `$M/<id>-message.txt`.
5. Restore at once by `cp` from the pristine copy, whatever the result.
6. Verify as in step 1, then append `run_exit`, `cases`, `failed`, `passed`,
   `expected: matches | does not match`, `verdict`, and
   `restored: <clock> hash-ok porcelain-ok diff-hash-ok`.

No gate, review or commit runs while an entry lacks its `restored` line.

**Failure types** (as `junit_cases.py` prints them after `FAIL`/`ERROR`): **A** is any `java.lang.AssertionError`
or subclass (Truth's `AssertionErrorWithFacts` and `ComparisonFailureWithFacts`, JUnit's `fail(...)`, Mockito's
verification errors). A named class means that exception escapes the test (`ERROR`). "First failing assertion"
quotes the message the test gives.

**Derivation.** Each row was walked against the test code of section 3 and the production code of 2.7. The
plain-Java facts come from L11. The Conscrypt facts come from L9 and N1 to N5, with the I2 outcomes in braces.
Every edit compiles: `if (false) stmt;` is the conditional-compilation form; added names are string literals
inside the array initialiser; removed lines leave a legal initialiser (a trailing comma is allowed); `reorder`,
`protocolsToEnable` and `ALLOWED_CIPHERS` are visible inside the class.

| Id | File | Exact edit (`old_string` → `new_string`) | Classes | Expected failing (all others in those classes pass): test — first failing assertion (type) |
|----|------|------------------------------------------|---------|-------------------------------------------------------------------------------------|
| M-X1 (a) | DTSF | `            "TLS_CHACHA20_POLY1305_SHA256",` → `            "TLS_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",` (put back as the first TLS 1.2 entry; both providers enable it by default) | FOUR | H4 — `containsExactlyElementsIn(…).inOrder()` (A); H5, H6 — "…direct socket: …exactly the allowed list" (A); H7 — case (i) (A); H8 — "CR-014 C2: TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA is not of an allowed name form" (A); H9 — "(a) suites set in the null branch" (A); J1 — the U-066 literal assertion (A; the reference comparison passes); J2, J3 — direct exact (A); K4, K5, P4, P5 [R] — `fail("…must be refused, but it completed with TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA")` (A); {O}: K1, K2, P1, P2 — "negotiated suite at TLSv1.2 (U-066 AC-5 (a))" (A). Not failing: C1, C2, J7 (parity), K6, K7, P6, P7, K3, P3, HL, the guards |
| M-X1 (b) | DTSF | as (a) with `"TLS_RSA_WITH_AES_128_GCM_SHA256"` (Conscrypt enables it; SunJSSE does not) | FOUR | H4, H5, H6, H7 (i), H8 (that name), H9 (a), as (a) (A); K6, K7, P6, P7 [R] — `fail(…completed with TLS_RSA_WITH_AES_128_GCM_SHA256)` (A); {O}: K1, K2, P1, P2 — negotiated suite (A). **Not failing, logged as expected:** J1, J2, J3 (SunJSSE's defaults lack `TLS_RSA_*`, so its list stays L-A), K4, K5, P4, P5 |
| M-X1 (c) | DTSF | as (a) with `"TLS_DHE_RSA_WITH_AES_128_GCM_SHA256"` (SunJSSE only) | HARD-C, JDK-C | H4 — (A); H7 (i) (A); H8 — that name not of an allowed form (A); H9 (a) (A); J1 — literal (A); J2, J3 — direct exact (A). Not failing: H5, H6 (Conscrypt lacks DHE) |
| M-X1 (d) | DTSF | as (a) with `"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"` (SunJSSE only) | HARD-C, JDK-C | as (c) |
| M-X1 (e) | DTSF | as (a) with `"TLS_ECDH_RSA_WITH_AES_128_CBC_SHA"` (no provider enables it) | HARD-C, JDK-C | H4 (A); H7 (i) (A); H8 — that name (A); H9 (a) (A). **Not failing, as the story expects:** H5, H6, J1, J2, J3 |
| M-X2 | DTSF | `        return suites.toArray(new String[suites.size()]);` → `        return reorder(socket.getEnabledCipherSuites(), ALLOWED_CIPHERS, null);` (allowed names first, every other enabled suite appended: today's shape) | FOUR | H5, H6 — direct exact (A); H7 — case (i) (A); H9 — (a) (A); J1 — literal (A); J2, J3 — direct exact (A); K4 to K7, P4 to P7 [R] — `fail(…completed with …)` (A). Not failing: H4, H8 (the constant is unchanged), parity tests, K1, K2, P1, P2 ({O or PDF}: under O the first TLS 1.2 suite offered is still AEAD; otherwise the server's own preference decides, which PDF shows is an AEAD suite for this key; A1, plan-review O-4), K3, P3 |
| M-X4 (i) | DTSF | the three lines `            "TLS_AES_128_GCM_SHA256",` / `            "TLS_AES_256_GCM_SHA384",` / `            "TLS_CHACHA20_POLY1305_SHA256",` (one `old_string`, with their line ends) → empty | FOUR | H4 (A); H7 (i) (A); H8 — "CR-014 C2 and V2: the allowed list must hold a TLS 1.3 suite" (A); H9 (a) (A); J1 — literal (A); J2, J3 — direct exact (A). {FIX13}: **not failing, logged as expected (2.5):** H5, H6, K3, P3, K1, K2, P1, P2. {not FIX13}: H5, H6 also fail (direct exact, A), and K3, P3 fail ("negotiated protocol", A) |
| M-X4 (ii) | DTSF | `            "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",` → empty | HARD-C, JDK-C | H4 (A); H5, H6 — direct exact (A); H7 (i) (A); H9 (a) (A); J1 — literal (A); J2, J3 — direct exact (A). Not failing: H8 (forms hold). {FIXS}: H5 and H6 do not fail; logged |
| M-X5 (i) | PIN (the post-E8 file; restored by `cp` from its pristine copy, hash `PIN_NEW`) | `        DefaultTrustedSocketFactory.hardenSocket(sslSocket);` → `        if (false) DefaultTrustedSocketFactory.hardenSocket(sslSocket);` (pinned factory only) | HARD-C, JDK-C, CST-C | H6 — "pinned factory, direct socket: …exactly the allowed list" (A); J1 — "AC-4 (d): cipher suites on the same socket after the server-name step, in the default factory's order" (A; the protocol `containsExactly` before it compares sets and passes, U-065 1.2); J3 — direct exact (A); J7 — "direct socket for host \"imap.right.test\": enabled protocols in order" (A); C1, C2 — `assertProtocolFloor` "TLSv1 must not be enabled" (A); the 4 PR — `fail(…completed at TLSv1.1 / TLSv1)` (A); P4 to P7 [R] — `fail(…completed with …)` (A); {not PDF}: P1, P2 — negotiated suite (A). Not failing: J4 (sets), J5, J6, CA, CR, P3, H5 |
| M-X5 (ii) | DTSF | `        hardenSocket(sslSocket);` → `        if (false) hardenSocket(sslSocket);` (default factory only) | FOUR | H5 — direct exact (A); HR1 — "enabled protocols must be the supported protocols that are TLS 1.2 or newer" (A); HR2 — "default factory, direct socket: TLSv1 must not be enabled" (A); J1 — "AC-4 (d): cipher suites … in the default factory's order" (A); J2 — direct exact (A); J7 — "precondition: the default factory's cipher order must differ from the provider's own" (A); JR1 — "TLSv1.2 must come first" (A; SunJSSE's default is `[TLSv1.3, TLSv1.2]`, its set equals the allowed one); C1, C2 — "precondition AC-3 (a)" (A); the 4 KR — `fail(…completed at …)` (A); K4 to K7 [R] — `fail(…completed with …)` (A); {not PDF}: K1, K2 — negotiated suite (A). Not failing: HL (both unhardened, equal), H6, J3, J4, JR2 (SunJSSE enables only 1.3 and 1.2 by default), K3, the certificate tests |
| M-X6 | DTSF | `        hardenSocket(sslSocket);` → `        if (socket == null) hardenSocket(sslSocket); else sslSocket.setEnabledProtocols(protocolsToEnable(sslSocket));` (default factory, layered shape: suites skipped, protocols kept) | FOUR | H5 — "default factory, layered socket: …exactly the allowed list" (A; direct passes); HL — layered against implicit cipher suites `inOrder` (A); J2 — layered exact (A); J7 — "layered socket for host \"imap.right.test\": enabled cipher suites in order" (A; protocols equal); C2 — "precondition AC-3 (a)" (A); K5, K7 [R] — `fail(…completed with …)` (A); {not PDF}: K2 — negotiated suite (A). Not failing: H6, J3, C1, K4, K6, HR2, JR2 (layered protocols still floored), the STARTTLS old-protocol refusals |
| M-X7 | DTSF | `        sock.setEnabledCipherSuites(ciphers != null ? ciphers : ciphersToEnable(sock));` → `        if (ciphers != null) sock.setEnabledCipherSuites(ciphers);` | HARD-C | H9 — `verify(socket).setEnabledCipherSuites(…)` (A, `WantedButNotInvoked`). Every other test passes (the static lists are not null) |
| M-X10 | DTSF | `        sock.setEnabledCipherSuites(ciphers != null ? ciphers : ciphersToEnable(sock));` → `        if (ciphers == null \|\| ciphers.length > 0) sock.setEnabledCipherSuites(ciphers != null ? ciphers : ciphersToEnable(sock));` (A1, contract-verification O-1: a non-null empty list is skipped, so the platform's suites stay; `\|\|` is Java's logical or, escaped for this table) | HARD-C | H9 — case (c), `verify(emptyList).setEnabledCipherSuites(…)` (A, `WantedButNotInvoked`). Cases (a) and (b) pass first (null lists still go through the step), and every other test passes: the static `ENABLED_CIPHERS` holds ten names, so no socket test reaches the skipped branch |
| M-X8 | DTSF | the two lines `            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",` / `            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",` → the same two lines swapped | HARD-C, JDK-C | H4 (A); H5, H6 — direct exact, order (A); H7 (i) (A); H9 (a) (A); J1 — literal (A); J2, J3 — direct exact (A). Not failing: H8, J7 |
| M-X9 | DTSF | `        List<String> offered = Arrays.asList(socket.getEnabledCipherSuites());` → `        List<String> offered = Arrays.asList(socket.getSupportedCipherSuites());` (C1: supported instead of enabled by default) | HARD-C, JDK-C | H7 — case (iii) (A); H9 — "(b) …no fallback" `isEmpty` (A). {SUP}: nothing else fails, logged (both providers support all ten, so the socket-level lists are unchanged: as U-065's M-D11r, only the stand-in tests can tell). {not SUP for a provider}: that provider's exact-list tests (H5, H6 or J1 to J3) also fail (A) |
| G-1 | HARD, HSK | in each file `@RunWith(RobolectricTestRunner.class)` → that line followed by a new line `@ org.robolectric.annotation.ConscryptMode(org.robolectric.annotation.ConscryptMode.Mode.OFF)` (the space after `@` is legal Java and keeps the hygiene pattern clear); two Edits, one entry | HARD-C, HSK-C | All 14 HARD and all 19 HSK cases — "A1-G2: this class must run on the Conscrypt provider, but the TLS context provider is SunJSSE" (A). This shows the reworked guard fires when the sandbox's provider is the JDK's |
| G-2 | JDKT | the line `@ConscryptMode(OFF)` → empty (the imports stay; unused imports compile) | JDK-C | All 11 JDK cases — "A1-G1: this class is not running on the JDK provider: TLS context provider=Conscrypt" (A). This shows A1-G1 still fires with the DHE clause gone |

Order: M-X1 (a) to (e), M-X2, M-X4 (i), (ii), M-X5 (i), (ii), M-X6, M-X7, M-X8, M-X9, M-X10, G-1, G-2
(15 mutation runs and 2 guard runs, 17 in all).
**M-X3** is logged as "not applicable: the allow-list was ruled (DP-2)". At the end, add a summary table under
the header (id, criterion, failing / of, verdict).

### I8 — (no separate determinism runs)

Determinism is checked in I9 by comparing ALL7's cases from the full gate with I6's. A run of its own would
add Gradle time and show nothing more.

### I9 — Final gate, counts and file checks

1. Pre-check as in I7 step 1; every entry has its `restored` line.
2. Run the gate with the same command as I0 step 2, into `$E/I9-gates.txt` and `$E/I9-junit-totals.txt`.
   Expected: `exit=0`, `tests=746 failures=0 errors=0 skipped=2` (746 or as reduced; skipped not above the
   start).
3. `python -I -X utf8 $T/junit_cases.py $R DefaultTrustedSocketFactory PinnedCertificateSocketFactory > $E/I9-all7-cases.txt; diff $E/I6-green-cases.txt $E/I9-all7-cases.txt; echo "determinism-diff-exit=$?"` (expected 0).
4. `$E/I9-checks.txt`:
   ```bash
   git grep -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"     # 746 (working tree)
   git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"   # 732 until the commit
   git status --porcelain -- app k9mail-vendored                              # the seven lines below
   git diff --name-only HEAD -- app/src/main k9mail-vendored                  # $PIN and $DTSF
   git diff HEAD -- app/src/main k9mail-vendored | git hash-object --stdin    # equals PROD_DIFF
   git hash-object $DTSF $PIN                                                 # DTSF_NEW, PIN_NEW
   git diff -U0 HEAD -- $PIN | grep '^@@'                                     # one hunk, @@ -57 +57 @@
   for f in $PINCLS; do echo "$(sha256sum "$f" | cut -d' ' -f1) ${f#$PINC/}"; done > $E/I9-pinned-classes.txt; diff $E/I0-pinned-classes.txt $E/I9-pinned-classes.txt; echo "pinned-classes-diff-exit=$?"   # 0
   git diff --stat $START -- build.gradle app/build.gradle k9mail-vendored/build.gradle settings.gradle gradle   # empty
   git diff --quiet $START -- gradle/verification-metadata.xml; echo "vm-exit=$?"                       # 0
   git diff --quiet $START -- app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java $TD/ssl/TlsTestFixtures.java; echo "untouched-exit=$?"   # 0
   git status --porcelain -- sdlc/artifacts/build/change-records sdlc/artifacts/design/contracts sdlc/artifacts/requirements; echo "records-end"   # nothing
   git grep -n -E "ENABLED_CIPHERS|ORDERED_KNOWN_CIPHERS|BLACKLISTED_CIPHERS|hardenSocket|ciphersToEnable" -- app/src k9mail-vendored/src | cut -d: -f1,2
   git grep -n "getSupportedCipherSuites" -- k9mail-vendored/src/main app/src/main; echo "supported-exit=$?"   # 1
   git grep -n "@Ignore" -- $TD/ssl $TD/Pinned*.java; echo "ignore-exit=$?"                              # 1
   ls -l app/build/outputs/apk/debug/app-debug.apk | awk '{print $5, $6, $7, $8, $9}'
   sha256sum app/build/outputs/apk/debug/app-debug.apk
   ```
   The `git grep` listing is accounted for line by line in the implementation log (IC-2; section 2.6).

   **Expected output of the `app k9mail-vendored` status line** (A1, plan-review O-7). Exactly seven lines, in
   `git status` order, each starting with ` M`:
   - `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java`
   - `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryCreateSocketTest.java`
   - `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHandshakeTest.java`
   - `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHardeningTest.java`
   - `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java`
   - `app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java`
   - `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`

   There is no `??` line. A leftover probe file would show as `??`, which is row D35. The other uncommitted
   documents in the checkout (the audit log, `.skill-context.json`, `sdlc/artifacts/build/research/`,
   DES-MODERNIZATION-014, the sprint-022 folder) lie outside `app` and `k9mail-vendored` and do not appear on
   this line.

   **The compiled pinned factory** (E8's proof): `pinned-classes-diff-exit=0`, meaning all four class files
   built by the I9 gate are byte-identical to those built by the I0 gate from the HEAD source. Otherwise, row
   D41.

### I10 — Implementation log and hand-off

For each step, log the clock time, a command summary (repository-relative paths; `$WORK` named only as "the
work folder"), the result line from the evidence file, and any decision row used. Close with:
- **Counts**, by reference to the evidence files, not restated: the tests added, by name; the 12 changed tests
  with old and new names; the mutation log location and its verdicts; the I2 outcome for each row; the declared
  deviations:
  - I7's restore by copy for both production files;
  - two declared narrowings of the story's mutation table (section 2.5 items 3 and 4): M-X4 (i), where the Conscrypt exact-list and handshake tests cannot fail, and M-X5 (ii), where the pinned exact-list tests H6 and J3 cannot fail (A1, plan-review O-3);
  - M-X10, added beyond the story's table (A1);
  - E8, the comment-only edit of `$PIN` under the orchestrator's ruling, with the I5 hunk check and the I9 class-file comparison, and the statement that `createSocket` was not edited, so M-E4 and M-E5 were not re-run;
  - AC-11 (a) read as the ruling words it: "one production file changes in code; one further production file changes in a comment";
  - the declared reading of AC-10 point 3 (section 7).
- **Text for the story's Implementation Notes:**
  - AC-4: the real initialiser failure is not forced. The normal path's empty list is pinned by H9 (c) and M-X10.
  - The empty-list measurements: `c_set_empty`, `c_hs_empty_suites`, `j_set_empty`, and L11's plain-JDK
    handshake.
  - The AC-7 table 3.1, with a reason for each change.
  - AC-8: the DHE clauses replaced or removed and why; G-1 and G-2.
  - AC-6: the outcome for each suite.
  - AC-10: the checklist of section 7.
  - AC-13 (a) to (e): (a) and (b) refer to the existing rulings and measurement entries; (c) the cost is the
    TLS 1.2-only server class in DP-1's row, and Gmail's TLS 1.3 path is unaffected; (d) no change record was
    edited; (e) the allow-list trade: "a later TLS 1.3 or 1.2 suite stays off until one entry is added to the
    allowed list, as a later protocol version needs one in `ALLOWED_PROTOCOLS`; the trade is for the product
    owner".
- **The debug APK**: `app/build/outputs/apk/debug/app-debug.apk`, with its size and SHA-256 from
  `I9-checks.txt`.
- **"Runtime Claims"**, listing only what evidence files show.
- **Last line**: "Hygiene scan: step I11, result in `evidence/I11-scan.txt`".

The developer edits neither the story nor any record. Section 7 lists the items for the orchestrator.

### I11 — Evidence-hygiene scan (last step, after every file is final)

The scan covers every file under `$W` except `compiled-pipeline.json` (excluded locally, L15), plus the code
diff, and writes its outputs to the work folder, outside the scanned folders. One call:
```bash
P='@[a-z0-9-]+\.[a-z]{2,}|Users[/\\]|App''Data'
L=$WORK/logs
git diff HEAD -- app/src k9mail-vendored > $L/I11-code.diff; echo "code-diff-exit=$?"
{
  grep -rlEi --exclude=compiled-pipeline.json "$P" $W $L/I11-code.diff > $L/I11-pattern-files.log
  echo "pattern-scan exit=$? files=$(wc -l < $L/I11-pattern-files.log)"
  for label in user-name host-name; do
    case $label in user-name) v="${USERNAME:-$USER}" ;; host-name) v="${COMPUTERNAME:-$(hostname)}" ;; esac
    if [ ${#v} -lt 3 ]; then echo "$label-scan value-unavailable"; continue; fi
    grep -rliF --exclude=compiled-pipeline.json -e "$v" $W $L/I11-code.diff > $L/I11-$label-files.log
    echo "$label-scan exit=$? files=$(wc -l < $L/I11-$label-files.log)"
  done
  S=sdlc/artifacts/build/sprints/sprint-022/U-066-prune-weak-tls-cipher-suites.md
  echo "story-pattern-count=$(grep -cEi "$P" $S)"
} > $L/I11-scan.log 2>&1
cat $L/I11-pattern-files.log $L/I11-user-name-files.log $L/I11-host-name-files.log
```
Expected: every scan `exit=1 files=0`; `story-pattern-count=0`. Sanitise `$L/I11-scan.log` into
`$E/I11-scan.txt`. Exit codes: 1 clean, 0 hit, 2 error. **Stop:** rows D36, D37.

## 5. Decision Table

| Row | Observation | What the developer does |
|-----|-------------|-------------------------|
| D1 | I0: HEAD is not `b9fd172f` or a descendant; a production, test or build path is dirty; a hash differs; the count is not 732; the toolchain is not 25; or the gate fails | Stop and report with the sanitised evidence (nothing has been edited). A later HEAD that changes nothing under `app`, `k9mail-vendored` or the build files: record it and continue |
| D1-S0 | I0: an S0-2 line has `head=0` and `wt=0` (the amendment, its check, the corrected C2 forms or the Part E clause is missing), or CR-014 is not `approved` | Stop before any edit and report "S0-2 incomplete" with the line; the story's S0-2 stop rule. If the working tree has the text and HEAD does not, continue and record that the amendment was read uncommitted |
| D2 | I2: a probe class errors, or a key is missing | Repeat I2 once (logged). Same result: stop and report |
| D3 | I2: `c_provider` not `Conscrypt`, `c_default_count` not 18, or `c_default` differs from `I0-probe.txt:30` | Stop and report: the environment changed; the preconditions and expected lists were derived from it |
| D4 | I2: `c_set_allowed` not exactly L-A | **D4-a** if it is L-A with the three TLS 1.3 names in another order: stop and report (DP-4's order would need re-ruling; the literal would be wrong). **D4-b** if it is L-A with only the signalling value moved: record; in H5 and H6 replace `assertAllowedCipherList(…)` for the Conscrypt sockets by a call that compares with the observed list, which the orchestrator must accept first: stop and ask before I3. Any other difference: stop and report |
| D5 | I2: `c_set_allowed_reversed`, `c_set_aead12_only` | TLS 1.3 names first in provider order in both: FIX13; keep `assertTls13SuitesAreProviderFixed`. TLS 1.3 names absent from `c_set_aead12_only`: not FIX13; delete the call `assertTls13SuitesAreProviderFixed();` and the method from HARD (H5, H6), and use M-X4 (i)'s {not FIX13} branch. Signalling value present although not set: FIXS. Record each |
| D6 | I2: `c_roundtrip_allowed_kept` or `j_roundtrip_allowed_kept` is `false` | Stop and report: the server-name step would undo the list (B5); the step order needs a design decision |
| D7 | I2: `c_hs12_today_list` | Suite ∈ AEAD12: the four TLS 1.2 tests pass by design in I4. Outside: record; continue; in I4 the four TLS 1.2 tests are expected to fail ("negotiated suite", A); report in the hand-off. The story's S0-4 (c) stop rule is about the fixture's ability to negotiate an allowed suite, which D8 decides |
| D8 | I2: `c_hs12_allowed` not `completed protocol=TLSv1.2` with a suite ∈ AEAD12 | Stop and report (story S0-4 stop rule: the fixture's key or suites would have to change; a Scope Concern) |
| D9 | I2: `c_hs12_provider_default`, `c_hs12_client_order` | Set PDF and O as defined in I2 step 4; record; continue. Any `refused`: stop and report |
| D10 | I2: `c_hs13_allowed` not `completed protocol=TLSv1.3` with a TLS 1.3 suite | Stop and report: AC-5 (b) cannot hold |
| D11 | I2, per pruned suite S: `c_hs_S_provider_default=completed protocol=TLSv1.2 suite=S … server=OK` and `c_hs_S_allowed=refused type=<SSLException subclass> server=FAILED:…` | Continue: write the four tests for S (two per factory) |
| D12 | I2, per S: `c_hs_S_provider_default` not `completed` with suite S | Do not write the four tests for S. Record AC-6 for S as "not demonstrable under Conscrypt" with the probe line (the story's condition); AC-1 to AC-3 stand alone for "refused". Reduce every count by 4; remove S's tests from the I4 and I7 expectations |
| D13 | I2, per S: `c_hs_S_allowed=completed …` | Stop and report: a client offering only allowed suites agreed with a server offering only S (contradiction) |
| D14 | I2, per S: refused, but `type=` is not an SSLException subclass | **Stop after I2, before I3**, report the line, and wait for the orchestrator's reading (record it with its date). **D14-a** (X accepted): write S's four tests with `} catch (SSLException e) {` replaced, in both cipher-policy helpers, by `} catch (java.io.IOException e) {` and, as the catch's first statement, `assertWithMessage("refusal type: " + e.getClass().getName()).that(e instanceof SSLException \|\| e instanceof X).isTrue();` (X fully qualified from the line; `\|\|` is Java's `||`). **D14-b** (not accepted): as D12, with AC-6 for S reported "not met as worded" |
| D15 | I2, per S: refused with an SSLException subclass but `server=OK` | Stop and report (contradictory) |
| D16 | I2: `j_provider` not `SunJSSE`, `j_default_count` not 31, or `j_default` differs from `I0c-diag.txt:14` | Repeat I2 once with the JDK probe alone (logged). Same result: stop and report |
| D17 | I2: `j_set_allowed` not exactly L-A | Stop and report |
| D18 | I2: `*_set_empty`, `c_set_unknown`, `c_hs_empty_suites`, `j_set_aead12_only` | Record only (section 2.3; CR-014 "Not stated"); no test change |
| D19 | I2: either `*_supported_has_all_allowed` | `true` for both: SUP. `false` for a provider: use M-X9's {not SUP} branch for that provider. Record |
| D20 | I3: a hunk outside the editable regions, `untouched-exit` not 0, an `@Ignore` or `Assume`, an old name left, or production dirty | Undo that file (`git checkout -- <test file>`, test files only; for `$LTS` re-apply I1 afterwards) and redo its edits; production dirty: stop and report |
| D21 | I4: a test listed as failing passes | Stop and report: it does not test the change; never weaken or reword it to fail |
| D22 | I4: a test listed as passing by design fails, or the failure type differs from the I4 table | Stop and report with the sanitised message |
| D23 | I4: an unchanged test fails, or a compile error | Unchanged test: stop and report. Compile error: compare the test source with section 3 character by character, fix the test source only, repeat I4 once (logged) |
| D24 | I4: matches | Continue |
| D25 | I5: another file changed, a hunk in a protected range, or an unexpected `hardenSocket(` caller | Undo the deviation to match 2.7 exactly; if the plan text does not compile, stop and report the compiler message (no improvised production code) |
| D26 | I6: a new or changed test fails | Compare with the plan; fix a typing error in a test and repeat I6 once. If test and production match the plan: stop and report |
| D27 | I6: an unchanged test fails | Stop and report; no test edit |
| D28 | I6 or later: guard A1-G1, A1-G2 or A1-G3 fails outside G-1 and G-2 | Stop and report: a sandbox was shared; do not change annotations or guards |
| D29 | I6: all pass | Continue |
| D30 | A mutation edit's `old_string` is not found or not unique | Re-read the file; if it matches the plan and the text still differs, stop and report (plan error) |
| D31 | A mutation does not compile | Restore at once, log `COMPILE_FAIL (invalid, not counted)` with its `restored` line, and apply a compiling replacement as a new entry `<id>r` (an `if (false)` form by deleting the statement; a list edit by the equivalent line edit); if none compiles, stop and report |
| D32 | A mutation or guard check is not detected (every expected test passes) | Restore, log `NOT DETECTED`; the criterion that named it is unmet: stop and report |
| D33 | A run differs from its row: an expected test passes, an unexpected test fails, a timeout, or a type differs from the row (an **A** entry matches any `AssertionError` subclass; a named exception matches that class; for an SSLException any subclass). A run that matches its row in tests and types is never a mismatch | Restore, log `MISMATCH (not counted)` with the actual set, types and messages; stop and report. A Robolectric class-level error such as `Couldn't create lock file` before any test: repeat once with the mutation still applied, keep both summaries, and name the repeat as a deviation |
| D34 | I0 or I9: daemon JVM not 25, or "Could not find or load main class" | Not 25: stop. Main-class error: confirm `MSYS_NO_PATHCONV` is unset and repeat once |
| D35 | I9: a JaCoCo floor missed, totals differ from the expected count, skipped above the start, `determinism-diff-exit` not 0, or a file check differs | Check first that no `U066*Probe` file remains under `app/src/test`; if one does, delete it and repeat the gate once. Otherwise stop and report; no floor, limit or build edit |
| D36 | I11: a scan hit | Read the listed file; remove the text from a hand-written file, or regenerate an evidence file from its raw log through `sanitize.py`; re-run I11 in full. A hit in `plan.md` or another agent's file: report, do not edit |
| D37 | I11: a `grep` exit 2 or `value-unavailable` | Correct the command and re-run; an unavailable value is reported as "not checked" |
| D38 | `sanitize.py` refuses a file | Narrow the filter; never edit raw logs or evidence by hand |
| D39 | "bash.exe … not found", a process killed without a visible reason, or a session lost | Stop and report (endpoint security). If an entry lacks `restored`, the next turn first restores from the pristine copy and checks the hashes |
| D40 | Anything else not foreseen here, outside a probe line that matched its expected value or a row above | Restore any applied mutation, then stop and report |
| D41 | A1, E8. Any of: I0 does not find the four pinned class files; I5 shows a `$PIN` hunk other than `@@ -57 +57 @@`, or a changed line that does not start with ` * `; I9's `pinned-classes-diff-exit` is not 0 | Missing class file at I0: check that the I0 gate ran `:app:assembleDebug :app:assembleRelease` (exit 0); if so, stop and report the path. Hunk or line wrong at I5: redo E8 from the plan text with the Edit tool, and stop if it is still wrong. Class files differ at I9: stop and report. Do not commit, and do not offer the ruling's fallback ("the diff is shown to touch no code token") on your own; the orchestrator decides |

## 6. Criterion-to-Step Map

| Criterion | Steps | Shown by | Evidence |
|-----------|-------|----------|----------|
| S0-1 | (orchestrator) | Story Implementation Notes, Rulings table | story |
| S0-2 | I0 | CR-014 lines, commit `b9fd172f` (D1-S0) | `I0-baseline.txt` |
| S0-3 | (orchestrator) | Story Implementation Notes, measurement | story |
| S0-4 (a)–(d) | I2 | `c_/j_set_allowed`; `j_set_aead12_only` plus L11 (handshake level, plain JDK); `c_hs12_*` and I4's TLS 1.2 tests; `c_hs_<S>_*` | `I2-probe.txt` |
| AC-1 (a)–(c) | I3, I4, I6, I7 | H5 (Conscrypt), J2 (JDK): literals, preconditions, both shapes; TLS 1.3 names in L-A (fixed on Conscrypt, the factory's on JDK); M-X1, M-X2, M-X4, M-X5 (ii), M-X6, M-X8 | `I4-red-cases.txt`, `I6-green-cases.txt`, `mutation-log.md` |
| AC-2 (a)–(c) | I3, I6, I7, I9 | H6, J3, J1 (literal after the server-name step); parity C1, C2, J7 unchanged; `$PIN`'s code untouched (E8 is one comment line: I5 hunk check; class files identical, I9); M-X5 (i), (ii) | as above, `I5-production.txt`, `I9-checks.txt`, `I0-`/`I9-pinned-classes.txt` |
| AC-3 (a) | I3, I6 | `assertAllowedCipherList`'s W loop in H5, H6, J2, J3 | `I6-green-cases.txt` |
| AC-3 (b) | I3, I7 | H8 (forms first, W second, TLS 1.3 and 1.2 present; the form check's own self-test); H4 snapshot; M-X1 (a)–(e), M-X4 (i) | `mutations/M-X1*`, `M-X4*` |
| AC-3 (c) | I3, I7 | H7 (i)–(iv); M-X1, M-X2, M-X4, M-X8, M-X9 | `mutations/` |
| AC-4 | I3, I4, I7 | H9 (changed; `never()` gone; case (c) for the normal path's empty list); M-X7, M-X10; Implementation Notes (failure not forced) | `mutations/M-X7*`, `M-X10*` |
| AC-5 (a)–(c) | I2, I3, I6, I7 | K1, K2, P1, P2 (suite ∈ AEAD12); K3, P3 (TLS 1.3, controls); the 8 KC and the pinned tests unchanged; {O} M-X1 (a), (b) | `I2-probe.txt`, `I6-green-cases.txt` |
| AC-6 | I1, I2, I3, I4, I7 | K4–K7, P4–P7 [R]; control `c_hs_<S>_provider_default` and M-X5 (i)/(ii); not asserted under JDK (L10, L11) | `I2-probe.txt`, `I4-red-cases.txt`, `mutation-log.md` |
| AC-7 | I3, I4, I6, I10 | table 3.1; diff checks; `@Ignore` and old-name greps | `I3-test-diff-check.txt`, implementation log |
| AC-8 (a)–(d) | I3, I6, I7 | three guards reworked; Javadocs; G-1, G-2; the preconditions | `mutations/G-*` |
| AC-9 | I5, I7 | 15 mutation entries (the story's table plus M-X10) and G-1, G-2; M-X3 not applicable; the restore deviation and the narrowings of M-X4 (i) and M-X5 (ii) declared | `mutation-log.md`, `mutations/` |
| AC-10 | I9, I10 | section 7 checklist; `records-end` empty; the change-record diff note | `I9-checks.txt`, implementation log |
| AC-11 (a)–(e) | I5, I9 | name list (one file in code, `$PIN` in one comment line, as the ruling reads AC-11 (a)), protected ranges, `$PIN` class files identical, `K9MailTransport`/`TlsTestFixtures` untouched, empty build diff | `I5-production.txt`, `I9-checks.txt` |
| AC-12 | I0, I9 | four-task gate before and after; counts 732 → 746; skipped not above the start; JaCoCo | `I0-*`, `I9-*` |
| AC-13 | I10 | Implementation Notes text | implementation log |
| IC-1 | orchestrator | section 9 | `IC1-emulator.txt` (orchestrator) |
| IC-2 | I5, I9 | full gate compiles every caller; the grep accounted for | `I9-checks.txt` |
| IC-3 | (orchestrator) | Story Implementation Notes, TLS 1.2 row: met, with its limits | story |

## 7. CR-014 Checklist (AC-10), Records (AC-13) and Items for the Orchestrator

**AC-10's change-record check.** `git diff --name-only <start>..HEAD` will list CR-014, because the
orchestrator's S0-2 commit `b9fd172f` lies in that range. The check that this story edited no change record is
therefore `git diff --name-only b9fd172f..<fix commit> -- sdlc/artifacts/build/change-records sdlc/artifacts/design/contracts`
(expected empty), and, before the commit, `records-end` in I9. QA should read it that way. The I10 hand-off says
so.

**AC-10 point 3, a declared reading** (A1, contract-verification O-4). The story's AC-10 point 3 quotes
hand-over wording, including Versioning directions the wrong way round. CR-014 Amendment 1 Part I ("Corrections
to the story's hand-over wording") and binding input B4 supersede that wording. The developer therefore hands
over the checklist below, with no wording of its own. QA should not mark AC-10 point 3 unmet because the quoted
wording is not reproduced.

**The checklist the developer hands over** (one line per point of CR-014 "The cipher-pruning story" and Scope
Concerns A and B, against Amendment 1 as corrected; no new wording, B4):

| Item | This story's answer (evidence) |
|------|--------------------------------|
| Point 1 (block what is pruned) | Allow-list: pruned names are absent from `ALLOWED_CIPHERS`, and the filter drops names on no list (H7 (i), H8; M-X2) |
| Point 2 (same lists for both) | One shared step, no pinned code edit (`$PIN` changes in one Javadoc line only, E8, with identical class files; C1, C2, J7, H6, J3; M-X5) |
| Point 3 (shape changed) | Allow-list ruled; CR-014 reworded first (Amendment 1, `b9fd172f`); the wording governing is Amendment 1 Parts A, C, D, E, F (Part E's Versioning directions, not the story's AC-10 text; Part I) |
| Point 4 (baselines in the same step) | H4 and the literals (AC-7 (a)) |
| Point 5 (a C2 test that is not a snapshot) | H8 against C2's forms; H7 at the step's input |
| Point 6 (V1–V5 and the null branch) | Protocol code and tests unchanged (AC-11 (b)); the cipher null branch is changed and recorded (H9, C4) |
| Point 7 (no contract edit) | No file under `sdlc/artifacts/design/contracts` or `…/change-records` edited (I9) |
| Point 8 (suites on neither list) | DP-4: dropped unless allowed; the two ChaCha20 TLS 1.2 suites joined the allowed list |
| Point 9 (filter the default-enabled set) | `ciphersToEnable` reads `getEnabledCipherSuites()`; the `getSupportedCipherSuites` search returns nothing; H7 (iii); M-X9 |
| Point 10 (TLS 1.3 names stay) | In L-A (H4, H8's TLS 1.3 clause, J2, J3); plain-JDK evidence of the failure mode it prevents (L11); M-X4 (i) |
| Point 11 / Scope Concern A (C4) | DP-3 option 1: H9 and M-X7 in place, so C4 applies, and Part D deletes the failed-initialisation bullet |
| Scope Concern B (`TLS_*` null, anonymous, export) | Moot under the allow-list; fed in at the step's input (H7 (i)) |
| Part G (Traceability, for the applier) | C1 → H7 (iii), the search, M-X9. C2 → H5, H6, J2, J3 (exact), H8 (forms), H7 (i), (iv), H9 (b), (c); M-X1, M-X2, M-X4, M-X10. C3 → the order in H4 to H7, J1 to J3; M-X8. C4 → H9; M-X7. B1 → C1, C2, J7, H6, J3; M-X5. Limits → K1, K2, P1, P2, K3, P3, K4–K7, P4–P7. Lines are refreshed at application |
| Part H (the applier's checks) | 1: tests listed above exist and are green (I9). 2: H8 checks the shipped constant against C2's forms. 3: C4 applies (H9, M-X7). 4: H8's TLS 1.3 clause. 5: Part E governs |

**Items for the orchestrator** (the developer edits none of these):
1. **Scope Concern P-1: resolved in this story.** The orchestrator ruled on 2026-11-21 (story Implementation
   Notes, "Ruling on the plan's scope concern P-1") that a comment-only correction is allowed. E8 makes it. The
   hand-off reports E8's three proofs: the one `@@ -57 +57 @@` hunk with comment lines only (I5), identical class
   files (I9), and `createSocket` not edited, so M-E4 and M-E5 were not re-run. No CR-009 checklist line is
   needed for it.
2. **CR-009's dated note: lines needed after this story** (A1, contract-verification O-3). This is the gate's
   text. The orchestrator adds it through the Artifact Librarian after U-066's fix commit, and fills the
   bracketed values from the implementation log. The developer edits no change record.
   > **Note: what story U-066 changed (added <date>, after commit <fix sha>).**
   > (a) T5 now reads: "`hardenSocket`, `protocolsToEnable`, `ciphersToEnable`, `reorder`: allowed cipher suites
   > taken from the platform's default-enabled set, in the allowed list's order; protocols limited to
   > `ALLOWED_PROTOCOLS` from the supported set. Exist (U-063, U-065, U-066)." This replaces the 2026-11-21
   > suggestion "blocked and ordered cipher suites".
   > (b) The baseline the move must keep green is the tests as U-063 to U-066 left them (<count> tests at <fix
   > sha>, <skipped> skipped). The classes now also reach `ALLOWED_CIPHERS` and `ciphersToEnable` by reflection.
   > `ORDERED_KNOWN_CIPHERS` and `BLACKLISTED_CIPHERS` no longer exist.
   > (c) U-066 renamed three Hardening tests: `createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes`,
   > `pinnedFactory_createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes` and
   > `hardenSocket_withNullLists_appliesTheAllowedSuitesAndTheProtocolFloorFromTheSocket`. It added exact-list
   > tests on both providers, the name-form check `allowedCipherConstant_holdsOnlyTheAllowedNameForms`, and
   > handshake refusals for pruned suites. Retarget them; do not drop them.
   > (d) In the note's last paragraph, "C1 to C3" reads "C1 to C4" (CR-014 Amendment 1).

   The gate's line (e), a checklist line for the pinned Javadoc phrase, is not needed, because E8 corrects it in
   this story (item 1). The seams to retarget at the move, not drop, are `ALLOWED_CIPHERS`, `ciphersToEnable`,
   the three-argument `hardenSocket`, and `ENABLED_CIPHERS` and `ENABLED_PROTOCOLS` in the three guards. F-4
   (private or unmodifiable arrays) applies there.
3. The AC-10 readings above (the change-record diff range, and point 3 superseded by Amendment 1 Part I), for QA.

## 8. Contracts Verified, Scope Concerns, Risks, Out of Scope

### 8.1 CNTR-MODERNIZATION-001 (approved; read through CR-014, not edited)

- **Today** the contract states nothing about cipher suites (no line contains "cipher", L16). Its producer
  table and Invariant 2 state trust behaviour only. This story changes nothing the contract states: trust and
  host-name code are untouched (AC-11 (c)), there is no type or signature change, and there is no new boundary.
  `integration_contracts: []` is correct and the contract gate does not block.
- **After the story**, statement by statement against Amendment 1 (as corrected, `b9fd172f`):
  - **C1**: the step reads only the default-enabled suites (2.1, H7 (iii), M-X9). True.
  - **C2**: only allowed suites are enabled, other than provider-fixed ones (H5, H6, J2, J3; on Conscrypt the
    fixed ones are the TLS 1.3 three, which are allowed). An empty intersection enables none, with no fallback,
    at the step, in the null branch and on the normal path (H7 (iv), H9 (b), (c); M-X10). Every name has an
    allowed form (H8). True.
  - **C3**: the allowed list's order, other than suites the platform places itself (exact `inOrder` on both
    providers; M-X8). True.
  - **C4**: the null branch works the suites out from the socket and applies them with the floor; a throw
    leaves `createSocket` (2.2, H9, M-X7). True.
  - **B1**: one shared allowed list (no pinned code edit; E8 is a comment; M-X5). True.
  - **B2**: the server-name step does not change the suites (J1, probe D6). True.
  - **B3, V1–V5**: unchanged code and tests. True.
  - **Part D's "Not stated"**: what a platform does with an empty list is recorded, not asserted (2.3).
  - **Part E**: adding a suite outside the forms, or removing every TLS 1.3 suite, would fail H8 (the test
    pins the breaking directions); in-form additions and reordering change H4 and the exact-list literals in
    the same step.

### 8.2 The story's Scope Concerns

| # | Answer |
|---|--------|
| 1 | Done before planning (S0-2, `b9fd172f`); checked in I0 (D1-S0) |
| 2 | Dealt with: the DHE coupling is removed. Live-provider guards in Hardening and Handshake, the provider check in the JDK class, G-1 and G-2 show them firing, and the preconditions fail on the wrong provider (AC-8) |
| 3 | Measured by the orchestrator (S0-3): both emulators list the same 18; filtering keeps ten |
| 4 | Kept as ruled (DP-1, DP-4); the forms of C2 admit them |
| 5 | Measured by the orchestrator (IC-3), with its limits |
| 6 | Recorded for the product owner (AC-13 (e), I10) |
| 7 | Kept: `protected static final`, as `ALLOWED_PROTOCOLS`; the retarget list for CR-009 is in section 7 |
| 8 | Kept last on the list; exempt from W1 to W12 by name; one of C2's forms |
| 9 | Not relevant under A (no CBC suite allowed) |
| 10 | Kept as written (section 9) |
| 11 | Applies to them by construction (the same `createSocket`); no test of their own, as the story says |
| 12 | Edit 6 as reworded in Amendment 1 Part F; not edited here |
| P-1 (new) | The pinned Javadoc phrase: ruled 2026-11-21, corrected by E8 (comment only, proven in I5 and I9); section 7, item 1 |

### 8.3 Risks

| # | Risk | Likelihood / impact | Mitigation |
|---|------|---------------------|------------|
| R1 | TLS 1.3 names dropped from the list later; on SunJSSE-like providers TLS 1.3 then silently negotiates TLS 1.2 | Low / medium | L11 measured it; H4, H8's TLS 1.3 clause, J1–J3; M-X4 (i) |
| R2 | The Conscrypt exact lists cannot see the TLS 1.3 names (provider-fixed) | Certain / low | Stated (2.5); the JDK tests and H8 carry it |
| R3 | Desktop Conscrypt cannot serve a pruned suite, so AC-6 cannot be shown | Low / low | D11 to D15; AC-1 to AC-3 stand alone |
| R4 | A later Conscrypt changes the order of its TLS 1.3 suites | Low / exact-list tests fail | Reported as a regression, never edited to pass (story risk 6); D4 |
| R5 | A device whose defaults hold no allowed suite gets an empty list | Not seen on the two emulators / connection fails closed | C2 wording; H7 (iv), H9 (b); probe records platform behaviour |
| R6 | A mutation left in place while the change is uncommitted | Low / high | Pristine copies, blobs, diff hash, write-ahead, recovery note |
| R7 | Unintended edits in heavily edited test classes | Medium / medium | Editable regions, the I3 check, D20 |
| R8 | The guard's provider name changes with a Robolectric or Conscrypt upgrade | Low / loud failure | The guard fails visibly (D28), never silently |
| R9 | A future suite stays off without a code change | Certain in the long term / low | Recorded trade (AC-13 (e)) |
| R10 | Endpoint security kills a shell mid-run | Low / medium | Foreground only; D39; recovery note |
| R11 | Evidence leaks a machine path or name (IC-1 logcat may hold the account) | Low / medium | `sanitize.py`, I11, filtered IC-1 lines |

### 8.4 Out of scope

The protocol floor; server-name, trust and host-name code; CR-009's move and making the arrays private; the
contract and every change record; REQ-MODERNIZATION-019; the OAuth and contacts connections; any user interface;
`TlsTestFixtures.java`, `K9MailTransport.java`, the SNI, Expiry and `PinnedCertificateSocketFactoryTest` classes;
any build or dependency file; S0-3 and IC-3 (done by the orchestrator).

## 9. IC-1 — Emulator Check (for the orchestrator; the developer does not run it)

Preconditions: the hand-off names `app/build/outputs/apk/debug/app-debug.apk` with its size and SHA-256 from
`evidence/I9-checks.txt` (built by the I9 gate from the final tree), and no mutation entry lacks `restored`. Run
in the foreground. No command below needs `MSYS_NO_PATHCONV=1`; never set it for Gradle. Raw output goes to the
orchestrator's work folder outside the repository.

1. Run `adb devices -l` first; it addresses no device.
   - If no device is listed: ask the owner to start the `Pixel_9_Pro` emulator. Never start or touch
     `Pixel_10_Pro`.
   - If more than one device is listed (emulator or physical): stop, ask the owner which serial is the
     `Pixel_9_Pro`, and send no command to any device until the owner answers.
   - If exactly one emulator is listed: run `adb -s <serial> emu avd name`. If its first line is not
     `Pixel_9_Pro`, stop, send nothing more, and ask the owner.
   - If exactly one device is listed and it is a physical device (no emulator), or the listing shows anything
     else: send nothing to it, stop and ask the owner (A1, plan-review O-8).

   Every later command uses only that serial, with `-s`.
2. `adb -s <serial> shell getprop ro.build.version.sdk` → `36`. Then, before the install:
   `adb -s <serial> shell dumpsys package com.zegoggles.smssync | grep -E "versionCode|lastUpdateTime"`.
3. `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk` → `Success`. On any failure (a
   signing mismatch, `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, a downgrade): stop and report "not run". Never
   uninstall, never use `-d`, never clear data. Repeat the `dumpsys` line after the install.
4. `T=$(adb -s <serial> shell "date '+%m-%d %H:%M:%S.000'" | tr -d '\r')`. The format is one quoted argument,
   so the device shell passes it to `date` as one word; check that `T` is not empty. Then start a backup:
   launch with `adb -s <serial> shell monkey -p com.zegoggles.smssync -c android.intent.category.LAUNCHER 1`,
   and either the owner taps the backup action, or the orchestrator takes a screenshot
   (`adb -s <serial> exec-out screencap -p > <work>/ic1-screen.png`) and taps with
   `adb -s <serial> shell input tap <x> <y>`. If the app asks for the account password or a sign-in: stop and
   report "not run" (only the owner handles it). No system setting is changed.
5. Wait in the foreground (tool timeout 600000):
   `adb -s <serial> logcat -T "$T" -m 1 -e 'BackupWorker: (backup complete, backedUp=|unrecoverable error)'`.
   Then write `adb -s <serial> logcat -d -v threadtime -T "$T" > <work>/ic1-logcat.txt` and run
   `adb -s <serial> shell pidof com.zegoggles.smssync`.
6. **Nothing to back up.** If the start line reads `starting backup (0 messages)`, or the worker finished
   without one, the run opened no connection (`BackupWorker.kt:313`, `:320`). The orchestrator may then send
   one test SMS through the emulator console, under the product owner's standing statement that adding SMS
   messages on the emulator is safe (U-065 review summary), with
   `adb -s <serial> emu sms send 5551234 "U-066 IC-1 test message"`. It records that it did, takes a new `T`
   (step 4), and starts the backup again once. If there is still nothing to back up, the verdict is "not
   conclusive: nothing to back up".
7. Search `<work>/ic1-logcat.txt` for:
   - `BackupWorker: starting backup (` (read N);
   - `BackupWorker: backup complete, backedUp=`;
   - `FATAL EXCEPTION` (count only those followed by `Process: com.zegoggles.smssync`);
   - `SSLHandshakeException`, `SSLProtocolException` and `BackupWorker: unrecoverable error` (count only lines
     with the app's pid or the tag `SMSBackup+`).
8. Verdict: pass only with the start line (N ≥ 1), the completion line, and zero of the four error kinds from
   the app's process. Otherwise report "fail" with the lines, or "not run" or "not conclusive" as above.
9. Record `sdlc/artifacts/build/sprints/sprint-022/U-066/evidence/IC1-emulator.txt` through `sanitize.py` (it
   refuses e-mail-like text). Include the clock time, AVD name, SDK level, install result, versionCode before
   and after, whether a test SMS was sent, the matching start and completion lines, the four error counts, the
   APK SHA-256 compared with the hand-off, and the verdict. Never copy raw logcat into the repository.

**What it proves:** after the change, the app still connects to Gmail and completes a backup through the
default factory, on a platform whose provider enables all ten allowed suites. **What it does not prove:** which
suite was negotiated (the app does not log it; Gmail negotiates TLS 1.3, so the pruned TLS 1.2 list is probably
not exercised); that the removed suites are off on the device; anything about Android 11-era devices, the
pinned factory (no host enrolled) or the owner's physical devices.

## 10. Constraints for the Developer (binding)

1. Run Gradle in the foreground with the tool timeout set to 600000 ms on every call, and never with
   `MSYS_NO_PATHCONV=1` set.
2. If the tool moves a run to the background by itself, wait for its completion notice. Never poll, and start
   no other command meanwhile.
3. No background or detached shells, no `&`, no `sleep` or polling loops, no helper agents for shell work, no
   heredocs for editing (Write and Edit tools only).
4. On "bash.exe … not found" or a process killed without a visible reason: stop and report (endpoint
   security). Do not work around it.
5. Raw logs and pristine copies stay in the work folder outside the repository. Only filtered evidence (4.0
   filters) enters `$W`.
6. Take timestamps from `date -u +%Y-%m-%dT%H:%M:%SZ`; never type them.
7. Commit nothing. The orchestrator commits with the product owner's permission.
8. Edit production code only as section 2.7 says, plus the logged mutations, each restored and proven by blob
   hash and diff hash.
9. No dependency, Gradle, build or verification-metadata change. No edit to `$PIN` other than E8 (one comment line) and the logged M-X5 (i), nor to `K9MailTransport.java`,
   `TlsTestFixtures.java`, or the SNI, Expiry or `PinnedCertificateSocketFactoryTest` classes. No `@Ignore` or
   `Assume`.
10. The probe classes exist only during I2 and are deleted before I3. No other new file goes under `app/`.
11. Do not touch the other work in the checkout: `sdlc/.skill-delegation-audit.log`, `sdlc/.skill-context.json`,
    `sdlc/artifacts/build/research/`, `sdlc/artifacts/design/` (including the untracked
    DES-MODERNIZATION-014), every change record, and any sprint-022 file other than this story's workspace
    files.

## 11. Quality Attribute Coverage (ISO/IEC 25010)

| Attribute | How this story serves it |
|-----------|--------------------------|
| Functional suitability | DP-1 to DP-4 are implemented as ruled and pinned on both providers, both shapes and both factories; each aspect has a mutation |
| Performance efficiency | One static computation as before (a ten-step loop over a list of 18 to 31 names); no per-socket cost added except in the null branch. Tests use loopback only, with every wait bounded at 10 s |
| Compatibility | Interoperability: Gmail is unaffected (TLS 1.3; at TLS 1.2 it chooses an allowed suite, IC-3). Accepted cost: TLS 1.2-only servers without ECDHE-AEAD. Co-existence: the sandbox guards still hold after the rework |
| Usability | Not applicable (no UI) |
| Security | Forward secrecy and AEAD only (RFC 9325 sections 4.1, 4.2; RFC 8446 section 9.1 for TLS 1.3), by allow-list (fail-safe defaults; the CWE-184 deny-list removed); the fail-open cipher branch is closed (CWE-636, "Not Failing Securely"); trust untouched |
| Reliability | The empty intersection fails closed and is stated; TLS 1.3 keeps its suites (no silent fallback to TLS 1.2, L11) |
| Maintainability | One constant is both set and order; the dead blocked list is removed; the form check makes a future edit's legitimacy mechanically checkable at the CR-009 move; seams to retarget are listed |
| Portability | Desktop providers in tests; two emulator images measured (S0-3); other devices inferred, not measured |

## 12. Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| U-066 story | `sdlc/artifacts/build/sprints/sprint-022/U-066-prune-weak-tls-cipher-suites.md` | Read in full: DP-1 to DP-4 with rulings, S0, AC-1 to AC-13, IC-1 to IC-3, tables, Scope Concerns, measurement |
| CR-014 | `sdlc/artifacts/build/change-records/CR-014-cntr-001-state-protocol-floor-and-cipher-handling.md` | Amendment 1 Parts A to I (`:319-477`); the pruning-story points (`:797-815`); Scope Concerns (`:819-829`); Suggested Approach including the Check of Amendment 1 (`:859-949`); HEAD and working tree compared |
| CNTR-MODERNIZATION-001 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md` | Status; searched for cipher (none) |
| U-065 plan and reviews | `sdlc/artifacts/build/sprints/sprint-021/U-065/plan.md`, `plan-review.md` | The model (sections 0, 1, 4 to 10); the B-1 and O-1 to O-10 lessons |
| Sprint-021 retro | `sdlc/artifacts/build/sprints/sprint-021/retro.md` | Lessons: rows derived against test code; JUnit XML via the init script; hygiene scan last |
| Provider evidence | `sprint-020/U-063/evidence/I0-probe.txt`, `I0c-diag.txt` (`:1-242`), `sprint-021/U-065/evidence/I2-probe.txt`, `I9-junit-totals.txt` | Provider facts L9, L10, L13 |
| U-063 tools | `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools/` | Usage lines of `junit_cases.py`, `sanitize.py`; init script |
| Default factory | `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` | Read in full |
| Pinned factory | `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` | Read in full |
| Tests | `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHardeningTest.java`, `…JdkProviderTest.java`, `…HandshakeTest.java`, `LoopbackTlsTestServer.java`, `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryCreateSocketTest.java` | Read in full; other classes counted |
| Fixtures | `app/src/test/java/com/zegoggles/smssync/mail/ssl/TlsTestFixtures.java` | Public members; compiled read-only for the scratch check |
| Build | `build.gradle:25-31`, `app/build.gradle:320-374`, `app/src/test/resources/robolectric.properties` | Test config, JaCoCo roots and floors, SDK |
| App log lines | `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` `:144`, `:169`, `:313`, `:319-320`, `:369` | IC-1 |
| REQ and DES corpus | `sdlc/artifacts/requirements/modernization/`, `sdlc/artifacts/design/modernization/` | Not re-read in full this session. The story has `requirements: []` and `design_docs: []`; REQ-MODERNIZATION-019 AC-B8 is reached through CR-014 Edit 6 (Part F), which this story does not apply; U-065's plan (section 12) found no REQ of `type: standard` and no conflicting clause, and no REQ or DES was changed since except by the CR-010 and CR-011 pass in `fba733e5`, which concerns minSdk, levels and libraries, not cipher suites |

## Verdict

**PASS.** The story's facts hold against the code at `b9fd172f` (L2 to L8). The one HEAD movement since the
brief is the orchestrator's S0-2 commit, which section 7 accounts for. The design meets every criterion as
ruled:
- One ordered allow-list filtered from the default-enabled suites (C1 to C3).
- The fail-open null branch closed (C4).
- The empty intersection fails closed and is stated.
- The dead blocked list removed, with `reorder` kept for the protocol step.
- No pinned-side code edit (only the ruled comment correction E8, shown not to change the compiled class) and
  no protocol, server-name or trust edit.

The coordinator's five inputs are built in: the forms test (H8), the empty case (H7 (iv), H9 (b), (c)), the
provider-fixed allowance (2.5, H5, H6, M-X4), Part E governing, and the JDK literal after the server-name step
(J1). Every production, test and mutation edit is given as exact text. Each mutation row was derived against
the planned test code with the first failing assertion and its type, and names the tests that must keep
passing.

Two places where the story's table expects more than can fail are declared in advance (2.5 items 3 and 4):
- M-X4 (i): the Conscrypt exact-list and handshake tests;
- M-X5 (ii): the pinned exact-list tests H6 and J3.

M-X1 (b) and (e) are not deviations: their rows follow the story's own scoping ("under the provider that
offers it"; AC-3 (b) and (c) only for (e)) (A1, plan-review O-5).

The plain-Java behaviour was checked on the build JDK during planning (L11). Whatever needs Robolectric or
Conscrypt is one probe run with a row for every outcome. Amendment A1 takes up every non-blocking finding of the
three plan gates without changing the design; it adds one test case (H9 (c)) and one mutation (M-X10).

## Phase Completion Report
---
story_id: "U-066"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-022/U-066/plan.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
