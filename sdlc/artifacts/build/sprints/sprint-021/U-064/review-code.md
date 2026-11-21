---
artifact_type: gate-result
gate: code-review
gate_id: "code-review"
checkpoint: quality-gate
story_id: "U-064"
verdict: PASS
iteration: 1
agent: "Code Reviewer"
timestamp: "2026-11-21T03:11:54Z"
base_commit: "de03e99b51a04d402b64cbd5c7f0d7d19e90f2a9"
blockers: 0
warnings: 1
notes: 5
---

# Code Review: U-064

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract and story compliance | PASS. Tests only. AC-1 to AC-3 are met as written; IC-2, IC-4, IC-5, IC-6, IC-9 and IC-10 hold in the working tree. IC-8 has one open point (W1). IC-12 is pending by design. |
| Test correctness | PASS. Each test asserts what its criterion says, and each was shown to fail for the stated reason under its mutation. No case was found where a test passes for the wrong reason. |
| Evidence | PASS. Failure messages are the named assertion and the named exceptions, not compile errors or timeouts. Both production files verified at their base blobs by me. |
| Maintainability | PASS. Javadoc says what is proved and what is not. Findings are notes. |

I found no blocker. The test code is sound, the mutation evidence is trustworthy, and the additions-only rule holds.
The one warning is a document-hygiene point for the commit step (W1).

## Verdict: PASS

## Independent verification done

- **Production untouched.** `git diff --quiet HEAD -- app/src/main k9mail-vendored` exits 0. `git status --porcelain -- app/src/main k9mail-vendored` is empty.
- **Blobs restored.** `git hash-object` of `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` gives `8a0fbb64fb171624591fbebafb818555f28efd58`, equal to `git rev-parse HEAD:<path>`. For `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` it gives `d543927166b3c5e1da0aaa8ab584072a43bf7521`, also equal.
- **Additions-only.** `git diff -- app/src/test` shows one hunk (`@@ -301,6 +301,93 @@`), 87 insertions, 0 deletions (`git diff HEAD -- <SNI test> | grep -c '^-[^-]'` gives 0). The file is CRLF throughout (446 CR for 446 lines, as at HEAD), and the hunk is CRLF too, so the diff is not inflated by a line-ending change. No import line was changed or added; the added code uses fully qualified names, as `:296` already does.
- **Count.** Working-tree `@Test` count 713 against 710 at HEAD (`evidence/I6-checks.txt`); Gradle totals 710 to 713, skipped 2 to 2 (`evidence/I0-junit-totals.txt`, `evidence/I6-junit-totals.txt`). The three added tests are the only added `@Test` lines. I did not rerun Gradle; no doubt remained that needed a run.
- **Determinism greps.** Over the new file and the added hunk there is no `Thread.sleep`, `getByName`, `getAllByName`, `"localhost"`, `"127.0.0.1"` or host-name `InetSocketAddress`. The client connects through `LoopbackTlsTestServer.connect` (`LoopbackTlsTestServer.java:167`), which uses the loopback address object and a 10 second bound for connect and read.
- **IC-9.** No `setDefaultHostnameVerifier` and no reflection on `defaultTrustManager` in the added code. The only reflection is `getDeclaredMethod("isIpOrBlankHostname", String.class)`.

## Findings

### Blockers

None.

### Warnings

**W1. `compiled-pipeline.json` in the story workspace holds a user-profile path and a user name (IC-8, IC-12).**
- File: `sdlc/artifacts/build/sprints/sprint-021/U-064/compiled-pipeline.json:1` (several occurrences, the plugin cache paths of the form `C:\Users\<name>\.claude\plugins\...`).
- Why: IC-8 forbids a user name or user-profile path in any committed or evidence file, and IC-12 commits "this story and its workspace files". The developer's scan (`evidence/I8-scan.txt`) covered the evidence folder and the story, not this orchestrator-generated file, so it reports clean. U-063 had the same item (its W4).
- Fix: at the IC-12 commit, leave this one file out of the commit (or have the orchestrator decide to scrub it), and add a line to the commit notes. I did not edit it.
- Classification: resolved in cycle
- Resolution (re-checked 2026-11-21): the orchestrator listed `compiled-pipeline.json` in the local git exclude file; `git check-ignore -v` reports it ignored, `git status --short --untracked-files=all` for the workspace no longer lists it, and the two test files (modification times earlier than the review; SNI numstat still 87 additions, 0 deletions) and both production blobs (equal to HEAD) are unchanged.

### Observations

**N1. AC-1 assertion (iii) and the "inverted fingerprint" claim are not exercised by any recorded mutation.**
- File: `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryExpiryTest.java:122-123` and the Javadoc at `:57-60`.
- Why: IC-7 required exactly M-N1 and M-N2, and both were run. Assertion (iii) (`doesNotContain(PIN_MARKER)`) is logically subsumed by (ii): if the fingerprint comparison were inverted, the cause would be a plain `CertificateException`, and (ii) would already fail. So (iii) cannot fail alone and is a reading aid, not an independent guard. The Javadoc sentence "A fingerprint comparison that is inverted also breaks it" is correct by reading but was not run. Nothing misleads: the Javadoc says only that it breaks, not that this was shown.
- Fix: none needed. If the Javadoc is touched again, say "by reading, not mutation-tested".
- Classification: defer-justified. The story's IC-7 set the mutations; adding one would exceed the story's evidence scope, and the claim is true by reading.

**N2. AC-3 assertion has no message, and AC-2's loop stops at the first failing guard assertion.**
- Files: `DefaultTrustedSocketFactorySniTest.java:379-384` (AC-3, `assertThat(socket).isInstanceOf(SSLSocket.class)`) and `:343-353` (AC-2).
- Why: AC-3's text does not require a message, and a failure shows the actual class, so it is readable. In AC-2 the Javadoc says "All names are tried before the no-throw assertion"; that is true for escaped exceptions (collected into `escaped`), but a failed guard assertion (i) or a failed `verify` aborts the loop at that name. Each such failure names the host, so the diagnostic is still adequate. The wording is slightly wider than the behaviour.
- Fix: none required; if the Javadoc is revised, say "all names are tried before the no-throw assertion is evaluated".
- Classification: defer-justified. Editing the added hunk is possible but gains nothing the failure text does not already give; no criterion is affected.

**N3. Style: `com.google.common.truth.Truth.assertWithMessage` is written fully qualified while `assertThat` is statically imported.**
- File: `DefaultTrustedSocketFactorySniTest.java:349` and `:369`.
- Why: the file's existing practice is fully qualified names for reflection types (`:296`, `:402`), and a new static import would be a second hunk and a change to an existing line group, against the additions-only rule. Acceptable, slightly noisy.
- Classification: defer-justified. A static import would break the single-hunk additions-only rule (IC-2); the retargeting at CR-009 can tidy it.

**N4. The `catch (AssertionError e)` wrapper around `verify(..., never())` assumes its own cause.**
- File: `DefaultTrustedSocketFactorySniTest.java:356-361`. The message "so SNIHostName accepted it here" is the correct reading for all three names on the desktop JDK (confirmed in `evidence/I1-first-step.txt`), and it names the host. If a future platform made `setSSLParameters` be called for another reason, the message would be a hint, not a proof. It does not make the test pass wrongly.
- Classification: defer-justified. The wrapper only improves a failure message; behaviour is unaffected.

**N5. Implementation-log housekeeping.**
- File: `sdlc/artifacts/build/sprints/sprint-021/U-064/implementation-log.md`. The claims I checked all hold: 713 and 710 counts (by reference to the evidence), 87 added lines and one hunk, the 15-case runs (1 + 14), the declared difference between the plan's "four" and the code's five fully qualified names (declared under I3, not a hidden deviation), and the empty Runtime Claims section (the story makes no runtime-provable claim of the crash, concurrency, timing or filesystem class; the handshake rejection is shown by the executed test and the mutation run). Two small points: the frontmatter says `files_changed: 1`, which does not count the story file's "As built" note (listed in the table below it); and I8 says "see the final reply" for counts although `evidence/I8-scan.txt` carries them. Neither affects a verdict.
- Classification: defer-justified. Both are descriptive and the log is a record of a finished step; nothing downstream reads them.

## Test-by-test assessment

**AC-1, `PinnedCertificateSocketFactoryExpiryTest.handshake_expiredEnrolledCertificate_isRejectedAsExpired` (`:104-125`).**
- The enrolled certificate and the certificate the server presents are both `EXPIRED_HOST_CERT` (`:105`, `:109-110`), so the fingerprints are equal and step 3 of `checkServerTrusted` passes; only `enrolledCert.checkValidity()` (`PinnedCertificateSocketFactory.java:149`) can then reject. The client is created through `createSocket` (`:111-112`), which installs `PinnedX509TrustManager` (`PinnedCertificateSocketFactory.java:75-86`), not through the `getTrustManagerForTesting()` seam. The host name `RIGHT_HOST_NAME` is passed, so a name mismatch cannot be the cause (the pinned path has none anyway).
- Could it pass for the wrong reason? Assertion (ii) requires the direct cause to be `CertificateExpiredException`. A handshake that failed for a connection, protocol or fixture reason would carry another exception (or none) and fail (ii). Under M-N1 the handshake completes and the `fail(...)` message appears, which shows the server presents a leaf the client would otherwise accept. Together this makes the "wrong reason" cases observable.
- Assertion messages: (ii) carries the cause chain (`causeChain`, `:128-140`, bounded at 10 and guarded against a self-cause); (iii) names the cause message; the `fail` says what was expected. The precondition assertion (`:106-107`) makes a machine clock before January 2020 a labelled failure instead of a puzzling one.
- Resources: `client` and `server` are fields closed in `@After` (`:84-96`), with a close failure swallowed for the socket only (a test socket). The server is created after the precondition, so a failed precondition leaves `server` null, handled. No timing or ordering dependence: no sleep, every blocking call bounded by the helper, no network or name resolution.
- Note on `start`: the server thread is not asserted (`server outcome` omitted), as the story states.

**AC-2, `setSniViaSSLParameters_nameRejectedBySniHostName_doesNotThrowAndSkipsSNI` (`DefaultTrustedSocketFactorySniTest.java:343-376`).**
- Assertion (i) calls the real private-package guard by reflection and requires `false` for each name, so a name that never reaches `SNIHostName` (stopped by the guard) fails loudly instead of passing as a no-op. Those three names and their `IllegalArgumentException` were confirmed in `evidence/I1-first-step.txt` (guard false; messages "Contains non-LDH ASCII characters", "cannot have the trailing dot", "Has leading or trailing hyphen").
- The mock is set up so it can fail: `getSSLParameters()` returns a fresh `SSLParameters`, `verify(mockSocket, never()).setSSLParameters(any())` is a real check (the neighbouring IPv4 test fails under M-A1g exactly because `SNIHostName` accepted a name and the call happened). The exception path is observed first, so the verify runs only when nothing escaped.
- Under M-N2 the exception escapes `setSniViaSSLParameters`, caught as `RuntimeException` in the loop and collected, and the final assertion fails with all three names listed (`evidence/mutations/M-N2-message.txt`). Not a compile error, not a timeout.

**AC-3, `createSocket_nameRejectedBySniHostName_doesNotThrow` (`:379-390`).**
- `createSocket(null, "bad_host.example", 993, null)` reaches `setSniHost` (`DefaultTrustedSocketFactory.java:191`) because Robolectric sdk 29 takes the `SDK_INT >= N` branch (U-063 environment facts, which `evidence/I3-sni-cases.txt` is consistent with), then `setSniViaSSLParameters`, passes the guard and reaches `SNIHostName`. The socket is unconnected (`socketFactory.createSocket()` with a null base socket, `DefaultTrustedSocketFactory.java:~151`), so no connection and no name resolution happen. `TrustManagerFactory.get(host, port)` is called with the name as a map key only. The socket is closed in `finally`. Under M-N2 it fails with `IllegalArgumentException: Contains non-LDH ASCII characters`, the exception that escapes through `setSniHost` and out of `createSocket`.
- The assertion is `isInstanceOf(SSLSocket.class)`; a null return or a plain socket would fail it. It does not detect a mutation that makes `setSniHost` swallow the exception but also skip hardening; that is outside AC-3.

## Mutation evidence

- **M-N1** (`evidence/mutations/M-N1-cases.txt`, `M-N1-message.txt`): 13 cases, 12 pass, 1 fail, the failure being `PinnedCertificateSocketFactoryExpiryTest#handshake_expiredEnrolledCertificate_isRejectedAsExpired` with `java.lang.AssertionError: the handshake must be rejected, but it completed`. This is the named assertion. The write-ahead entry carries a hash-before equal to the base blob and a clean porcelain and diff; the `restored` line carries the hash and empty porcelain and diff.
- **M-N2** (`evidence/mutations/M-N2-cases.txt`, `M-N2-message.txt`): 19 cases, 17 pass, 2 fail: the AC-2 test with a Truth failure listing all three names, each with its `IllegalArgumentException` message, and the AC-3 test with `java.lang.IllegalArgumentException`. The 12 existing SNI tests and 5 JDK-provider tests pass. This is the same pattern as U-063's M-A1c, now detected.
- Timestamps in the log are consistent with the order (M-N1 03:03:27Z to 03:04:34Z restored; M-N2 03:04:42Z to 03:05:43Z restored; implementation log 03:09:48Z), and I confirmed the final blobs myself (above). The mutation was not present when the final gate ran: the order in the log (I4, I5, I6) and the blob check at `evidence/I6-checks.txt` agree.
- I did not re-apply a mutation: the evidence is internally consistent, the failure texts are specific, and restoring was verified, so a spot check would add risk without adding information.

## Robustness to U-065 (what the next story will meet)

U-065 adds a TLS 1.2 floor to both factories and gives the pinned factory the default factory's cipher and server-name handling.

- **AC-1 (Expiry test): robust, no declared change expected.** The loopback server's context negotiates TLS 1.3 under the test provider (U-063 probe); a TLS 1.2 floor (minimum 1.2, TLS 1.3 kept) leaves that unchanged. Server-name handling will make the pinned factory call `setSniViaSSLParameters` for `imap.right.test`, a valid DNS name; Conscrypt accepts it, and the trust manager (fingerprint, then `checkValidity`) is unchanged. A cipher list that reorders or removes suites still leaves TLS 1.3 suites in place. The test would fail only if U-065 changes the validity check, or makes the handshake fail before the trust manager is consulted, and then it fails with the cause-chain message, which is the right signal. One caution: if U-065 ever caches a context per factory, a resumed session would skip the trust manager (stated in the Javadoc at `:53-55`).
- **AC-3: robust.** It asserts only "no exception, an `SSLSocket`". A TLS 1.2 floor goes through `hardenSocket`; nothing there depends on the host name.
- **AC-2: depends on three seams and will need a declared change if they move.** It calls the public static `DefaultTrustedSocketFactory.setSniViaSSLParameters(SSLSocket, String)`, reads the guard `isIpOrBlankHostname` by reflection, and asserts no `setSSLParameters` call for a rejected name. If U-065 extracts SNI handling into a shared helper for both factories and renames, moves or changes the visibility of these, the test fails with `NoSuchMethodException` or a compile error, which the class comment ("After the CR-009 move") already declares as a retarget, not a regression. If U-065 widens the guard to cover these names, assertion (i) fails by design and forces the decision the criterion describes. If U-065 makes the pinned factory share the same helper, AC-2 still tests the default factory's copy only: the pinned factory then has the same safety net with no test of its own. U-065 should add one call-through test for the pinned factory; I flag it here as information for that story.
- None of the three tests assume the pinned factory sets no server names or no cipher/protocol restrictions. The existing U-063 tests that do assume this (AC-18 and the JDK-provider pinned test) are the ones U-065 must change, not these.

## Patterns Verified

- [x] Follows existing code patterns: JUnit 4 with `RobolectricTestRunner`, `LoopbackTlsTestServer`, `TlsTestFixtures`, the `@After` shape and the `fail` plus `catch SSLException` shape of `PinnedCertificateSocketFactoryCreateSocketTest.handshake_certificateOtherThanEnrolled_isRejected`.
- [x] Error handling is appropriate: sockets and the server always closed; close failures of a test socket swallowed; no swallowed assertion.
- [x] Tests cover new functionality: AC-1, AC-2, AC-3, each shown to fail under its mutation.
- [x] No hardcoded values that should be configurable: the three host names and port 993 are fixed test inputs, named in the criteria.
- [x] No unnecessary complexity: one small private helper (`causeChain`) used in a failure message only; no dead code, no unused import (imports of the new class: all used; the SNI file has no import change).

## Integration Verified

- [x] New code reachable from production entry points: not applicable (tests only); the tests are discovered by `:app:testDebugUnitTest` and ran in the full gate (78 files, 713 tests, 0 failures, 2 skipped).
- [x] Registries or dispatch maps updated: not applicable.
- [x] Function signatures match at all call sites: no signature changed; the tests call `PinnedCertificateSocketFactory(Context, String, X509Certificate)`, `createSocket(Socket, String, int, String)`, `DefaultTrustedSocketFactory.setSniViaSSLParameters(SSLSocket, String)` and the package-private `isIpOrBlankHostname(String)` as they exist at HEAD.
- [x] No dead code introduced.
- [x] Integration path documented in implementation-log.md ("Integration verification").

## Regression Check

Not applicable: no file was deleted, replaced or rewritten. The implementation log states "Capabilities Inventory: Not applicable", which is correct. The 12 existing SNI methods are byte-identical (deletion count 0).

## Contract Verification

- [x] The story lists no `integration_contracts`. The behaviour AC-1 tests (validity-window check after the fingerprint step) matches the producer table and error table of CNTR-MODERNIZATION-001 as quoted in the story (`:96`, `:197`). No consumer or interface changed.

## Runtime Claims Verified

- [x] `## Runtime Claims` read in the implementation log: it is declared and empty, not omitted.
- [x] No declared claim, so no probe or escape-hatch justification to assess.
- [x] Completeness audit against the story's ACs: AC-1 to AC-3 are test-behaviour criteria, shown by the executed tests and the two mutation runs; none is a crash, concurrency, timing, filesystem or hook claim, so no inventory entry is missing.
- [x] No probe wrote into the sibling plugin-source repository: none exists, and the story's plugin source is "not applicable". The two mutations touched only the two production files named in IC-7, in this repository.
- [x] UNVERIFIED (escape-hatch) claims: None.

## Notes for the orchestrator

- Before the IC-12 commit: handle W1 (leave `compiled-pipeline.json` out, or scrub).
- The commit must contain only the new test class, the modified SNI test, the story, and the workspace files; the other modified files in the tree belong to other work.
- After the commit, QA can run the literal `git diff <base>..HEAD` forms of IC-2, IC-4 and IC-8.
