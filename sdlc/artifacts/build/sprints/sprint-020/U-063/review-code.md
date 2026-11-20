---
artifact_type: review-code
story_id: "U-063"
verdict: "PASS"
agent: "Code Reviewer"
timestamp: "2026-11-20"
blockers: 0
warnings: 2
---

# Code Review: U-063

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | PASS. Tests only. No file under `app/src/main`, `k9mail-vendored`, the build files or `gradle/` changed. Each of the 28 new tests checks what its name, Javadoc and acceptance criterion say. |
| Test coverage | PASS with one gap in the recorded baseline (W1: the `catch (IllegalArgumentException)` safety net in `setSniViaSSLParameters` is covered by no test). W2 (a weak bound) is resolved. All 21 ACs have a detected mutation. |
| Code quality | PASS. Resource handling, bounds and global-state restore are sound. Duplicated helpers are a note. The stale guard string (N2) is resolved. |

The baseline can be trusted for the CR-009 move. I found no blocker. The things that most deserve a reader's attention are:

- **W1.** The move can drop the `catch (IllegalArgumentException)` safety net (BUG-015) with the whole SNI suite still green. The story accepted this as "by design", but a later reader will not see what it means in practice.
- **W4.** One workspace file holds a user-profile path and needs a commit-time decision.
- W2 and W3 were resolved in the cycle; see the delta re-check at the end of the findings.

## Verdict: PASS

## Findings

### Blockers

None.

### Warnings

**W1. The safety net `catch (IllegalArgumentException)` at `DefaultTrustedSocketFactory.java:248` is covered by no test, and the move can drop it silently.**
- Where: mutation entry M-A1c in `evidence/mutation-log.md`, the summary table (BY-DESIGN, "0 of 16"), AC-1 text in the story.
- Evidence: with the catch removed and the guard kept, all 16 tests of the SNI class and the JDK-provider class pass. The ten pre-existing SNI tests feed only IPv4, IPv6, empty and blank values, which the guard stops before `SNIHostName` is reached. The catch matters only for a name that passes the guard and that `SNIHostName` still rejects.
- I ran a small JDK program to check this. `new SNIHostName("bad_host.example")`, `"host.example."` and `"-x.example"` all throw `IllegalArgumentException`, and `"imap.right.test"` does not. A user-entered IMAP host with an underscore or a trailing dot would therefore crash `createSocket` if the catch were lost.
- The story handles the overlap by pairing M-A1g and M-A1c as BY-DESIGN, and AC-2 covers the guard alone. Nothing covers the catch alone, and the summary does not say so. A reader sees "BY-DESIGN" and may assume both layers are protected.
- Suggested fix: a follow-up test `setSniViaSSLParameters_malformedDnsName_doesNotThrowAndSkipsSNI`. It would call `setSniViaSSLParameters(mockSocket, "bad_host.example")` and assert no exception and `setSSLParameters` never called. It would break on M-A1c. Add it as the first increment of the CR-009 move, or before it. Add one sentence to the mutation-log summary saying M-A1c is an uncovered layer.
- Classification: defer-justified. A new test changes the recorded totals (710, 28, 39 mutations) in about ten places, and AC-1 deliberately accepts the overlap, so the addition belongs to a follow-up or the move story and not to this review cycle.

**W2. AC-15 (ii) and (iii): the bound on the degenerate remainder is weak. RESOLVED in cycle (see the delta re-check).**
- Where: `DefaultTrustedSocketFactoryHardeningTest.java:291-323` as first reviewed.
- Evidence: after the provider-fixed suites are removed (deviation 7), the test required only `knownInRemainder.size() >= 2`. Check (iii) is `lastKnown < firstUnknown`, and `firstUnknown` stays `Integer.MAX_VALUE` when the remainder holds no unknown suite. A provider whose default list had two known suites and no unknown one would have passed (iii) vacuously, and the mutation "unknown items put first" (M-D6a) would have gone unseen by AC-15.
- It was not vacuous at the time. The probe (`evidence/I0-probe.txt:30-32`) shows unknown suites in the Conscrypt list (CHACHA20, `TLS_RSA_WITH_AES_*_GCM`), and M-D6a made AC-15 fail. The fixed-suite bounds are sound.
- Suggested fix was: add a precondition in the style of the "D6''" messages requiring at least one unknown suite in the remainder.
- Classification: resolved in cycle (2026-11-20)

**W3. The story text is not synchronised with deviations 14 and 16 and with the final counts. RESOLVED in cycle (see the delta re-check).**
- Where: `sdlc/artifacts/build/sprints/sprint-020/U-063-tls-socket-factory-characterisation-tests.md`: AC-3 and AC-4 (named the SNI test file), AC-19 (named the pinned class and an old method name), IC-4 (708, "Group E 5"), the Affected Code table (no JDK-provider row).
- Evidence: the work was in the "Implementation Notes" only; the ACs still pointed to the old classes.
- Suggested fix was: one "as built" line under AC-3, AC-4, AC-19 and IC-4, and a JDK-provider row in Affected Code. This is the orchestrator's edit to the story file.
- Classification: resolved in cycle (2026-11-20)

**W4. `sdlc/artifacts/build/sprints/sprint-020/U-063/compiled-pipeline.json` contains a Windows user-profile path, and the developer's IC-8 scan did not include it.**
- Evidence: my grep for the IC-8 patterns (`Users[/\\]`, `AppData`) over the workspace hit only this file (several paths to the plugin cache). The developer's scan stream (implementation log I7.1) lists the plan, reviews, log and evidence but not this file. The literal IC-8 check, `git diff <base>..HEAD | grep ...`, will match it if the workspace is committed. This is a framework-generated file, and 18 copies are already tracked in the repository, so I classify it as a warning and not a blocker.
- Suggested fix: the orchestrator or QA either leaves this one file out of the story commit or records the match as known benign in the QA notes. The developer's tests and evidence are clean: my grep found no e-mail address, user name or `AppData` in the six sources, the SNI additions or the evidence files.
- Classification: defer-justified. The file is written by the pipeline, not by the story, and the repository already tracks 18 copies; the commit decision belongs to the orchestrator.

### Observations

**N1. The moved AC-19 test and its control (answering the question in the brief).**
- `DefaultTrustedSocketFactoryJdkProviderTest.pinnedFactory_createSocket_implicit_setsNoServerNames_jdkProvider` (`:312-324`) asserts an absence. The control named for it, AC-4 (`:192-203`), exercises the default factory, not the pinned one.
- This is sufficient. Both factories get their socket from `SSLContext.getInstance("TLS").getSocketFactory().createSocket()`, so they return the same socket class and the same observable. AC-4 shows the observable reports a name on that class under SunJSSE. More decisive is M-E2-jdk (`mutation-log.md:554-565`): it makes the pinned factory set a real `SNIHostName`, and exactly this test fails while the other eight pass.
- The test does not prove what a device sends. The Javadoc says so, including that the capture showed a provider adding a name on its own for both factories. The sentence about "the environment diagnostic's capture" (`:307-309`) describes a finding in the past and reads as if it concerned the JDK provider; consider "under the default test provider" for clarity.
- `assertThat(socket).isInstanceOf(SSLSocket.class)` (`:317`) is redundant after the cast.
- Classification: defer-justified. These are wording points; the test is sound and the story is not worth reopening for them.

**N2. Stale string in the Handshake leak guard. RESOLVED in cycle (see the delta re-check).**
- `DefaultTrustedSocketFactoryHandshakeTest.java:173` tested `verifierName.contains("SanDnsHostnameTestVerifier")`. No such class exists in the repository. It was a leftover from the plan's abandoned host-name verifier swap. The second condition (`startsWith("com.zegoggles.smssync.mail.ssl.")`) is the live one.
- Suggested fix was: delete the first condition.
- Classification: resolved in cycle (2026-11-20)

**N3. The `LoopbackTlsTestServer` and `TlsTestFixtures` review.**
- Resources: `close()` closes the accepted socket and the listener, then calls `shutdownNow()`. It is idempotent and never throws. The thread is a daemon. A client that never connects ends in `FAILED:SocketTimeoutException` after 10 s. A client that connects and goes quiet is bounded by the 10 s read timeout set on the accepted socket. `awaitOutcome()` is bounded and returns a visible `TIMEOUT`.
- No sleeps, no name lookups (the loopback address object is used throughout), port 0, backlog 1. IC-6 greps over the six new files and the SNI additions return nothing.
- `readLine` is deliberately unbuffered and says why.
- Fixtures: I decoded every `*_CERT` constant with a JDK program. The six SHA-256 fingerprints, subjects, SAN entries and validity dates equal the Javadoc (IC-13). Valid certificates run 2020-01-01 to 2107-08-13, the CAs to 2119-11-07, and the expired leaf ends 2020-01-31. There are four leaf keys, all PKCS#8 RSA, and no CA private key. A repository grep for PEM private-key markers returns nothing.
- Running the JDK PKIX validator against the trusted CA (a separate check by me, not the code under test): `right-host` and `other-host` are accepted; `expired-host` fails with `CertificateExpiredException` only; `untrusted-host` fails with "unable to find valid certification path". So each rejection fixture has exactly the defect its test name claims.
- Both helper classes contain `Test` in their names, and so do their inner classes, so the `**/*Test*.*` jacoco filter excludes them (`app/build.gradle:237`). `evidence/I7-jacoco-packages.txt` is consistent with that. Nothing in `app/src/main` can reach them.
- Classification: defer-justified. No change is needed; this is a record of what I checked.

**N4. Rejection-shape assertions in the Handshake class (brief item 1).**
- `assertRejected` calls `fail(...)` inside the `try`; the `AssertionError` is not caught by `catch (SSLException)`, so a handshake that completes fails the test.
- The direct cause is asserted twice (`CertificateException`, `CertificateChainException`). The host-name marker is required for the other-host tests and forbidden for the untrusted and expired ones, over a bounded cause walk.
- The controls read the byte the server writes after its own handshake and require the server outcome `OK`. These controls prove that the substitute trust manager and Robolectric's verifier accept the right chain, so a rejection cannot be a global failure. Mutations M-B3, M-B4 and M-B5 show them failing.
- The expired test does not assert the reason is expiry. I checked the fixture with the PKIX validator (N3) and the defect is expiry alone.
- Classification: defer-justified. The plan forbids asserting below `CertificateException`; I see no reason to change that.

**N5. Global state and isolation (brief item 2).**
- The trust-manager substitution is reflection on the private static `defaultTrustManager`, restored in `@After` inside `finally` with an `isSameInstanceAs` check. The `substituted` flag prevents a restore when setup failed first.
- The leak guard refuses to start if the current trust manager trusts a "U-063" issuer.
- The vendored class is loaded once per Robolectric sandbox, and the four forks are separate JVMs, so the field is not shared across forks. Tests inside one JVM run one at a time.
- The per-host `SecureX509TrustManager` cache holds only host and port. It reads the static field on every call (`TrustManagerFactory.java:76`), so a cached manager cannot carry a trust decision from one test to another. This holds for the production code as read.
- The default host-name verifier (Robolectric's `AndroidTestEnvironment$1`, `I0-probe.txt:16-18`) is never replaced. It is JVM-global, and the class asserts it is unchanged after each test. The Handshake class depends on that verifier's semantics: accept a SAN match, reject another name. The control AC-5 fails loudly if a Robolectric upgrade changes it, so there is no silent pass.
- A1-G1 and A1-G2 are deterministic, because they depend on the sandbox key. A1-G3 depends on what else ran in that JVM. A SunJSSE initialisation that fails under Conscrypt poisons the JDK TLS classes JVM-wide (`plan.md` row D10b; `plan-review.md:313-317`). If a future test in the same fork touches SunJSSE first, the JDK-provider class fails with a named message ("another test is the cause"). That is loud and actionable, and it can be intermittent by fork assignment. The evidence is two ordered 7-class suites in both orders (`I7-ic9.txt`), three repeat runs of the five classes (identical, `I7-ic6-diff.txt`), and the full gates run at each increment (about a dozen). I consider this enough for today. It is not a structural guarantee. For the retro, see R2.
- I ran one Gradle invocation of the six TLS-related classes (`--rerun`): `BUILD SUCCESSFUL in 31s`; the results XML show Handshake 8, Hardening 9, JDK-provider 5, SNI 12, pinned create-socket 4 and the existing pinned test 8, all with 0 failures.
- Classification: defer-justified. A structural fix needs a build change, which this story may not make.

**N6. The Hardening class against the code (brief item 1, Group D).**
- The literal baselines (24 known, 16 blocked ciphers; `TLSv1.2, TLSv1.1, TLSv1`; `SSLv3`) equal `DefaultTrustedSocketFactory.java:37-89` entry by entry and in order.
- AC-17 compares the layered socket with the implicit one. If hardening vanished on both paths together (M-D8), AC-17 alone would stay green; AC-15 and AC-16 catch it, and M-D8 shows them failing. The class Javadoc does not say that AC-17 depends on those two. It could say so in one sentence.
- Second AC-16 test under Conscrypt (`NOTED-AC16`): the Javadoc says it cannot tell the factory from the provider and points to the JDK-provider form, which fails under M-D11 and M-D12. That is what the story's AC-16 text asks for (report, do not claim).
- The JDK-provider preconditions fail loudly: A1-P1 requires supported to differ from default-enabled, A1-P2 requires TLS 1.0 and 1.1 to be supported and not enabled by default. Neither skips silently if a future JDK changes its defaults.
- Classification: defer-justified. The wording point on AC-17 is optional and the coverage exists elsewhere.

**N7. Duplicated helpers and the retargeting cost for the CR-009 mover.**
- `constant(String)` (reflection on `DefaultTrustedSocketFactory`) is written three times (Hardening, Handshake, JDK-provider). `reorder` reflection, `defaultSocket()`, `closeQuietly`, `firstEntries` and the whole A1-G2 guard are copied between the Hardening and Handshake classes. The Handshake class also reflects on `TrustManagerFactory.defaultTrustManager`, and the SNI additions reflect on `isIpOrBlankHostname`.
- When the class moves, at least five places must be retargeted, and the two copies of the A1-G2 guard must be edited the same way. Each Javadoc says what to do ("After the CR-009 move"), which is good. A shared package-private helper would make it one edit.
- Classification: defer-justified. A refactor of the finished baseline just before it is used would add risk; the move story can do it as its first step.

**N8. Naming and comments.**
- Test names follow the neighbouring SNI tests (`subject_condition_expectation`). Javadoc cites RFC 6066 section 3, RFC 6125 section 6, RFC 5280 and RFC 8996 where they apply. The fresh-TLS-context statement is correct: both factories build a new `SSLContext` per `createSocket`, so a resumed session cannot skip the check. The TLS 1.3 statement matches the probe (`I0-probe.txt:1,5,10,12`).
- The `@LooperMode(LEGACY)` explanation is accurate and honest about being a sandbox key. The compiler's deprecation note for that annotation is expected and recorded.
- Classification: defer-justified. No change is needed.

## Patterns Verified

- [x] Follows existing code patterns (Robolectric runner, Truth, short-form annotations; the neighbouring SNI and pinned tests)
- [x] Error handling is appropriate (rejection shape asserted; server failure reported as a string and never thrown; teardown in `finally`)
- [x] Tests cover new functionality (28 tests; every "Breaks when" clause has a detected mutation except the by-design layer, see W1)
- [x] No hardcoded values that should be configurable (fixture hosts are fixed by the story; bounds are named constants)
- [x] No unnecessary complexity (the duplication in N7 is a small exception; the stale string in N2 is removed)

## Integration Verified

- [x] New code is reachable from production entry points. N/A: tests only. The tests call `createSocket` in the same two shapes as `ImapConnection` (`createSocket(null, ...)` and `createSocket(plain, ...)`), as the story's scope says; they do not drive `ImapConnection`.
- [x] Registries or dispatch maps: none apply.
- [x] Function signatures: no production signature changed. `git diff --stat` over the production paths is empty.
- [x] No dead code introduced (the stale string in N2 is removed).
- [x] Integration path documented in the implementation log.

## Regression Check

Not a replacement or rewrite story. No file was deleted or replaced, so no Capabilities Inventory is required.

## Contract Verification

No interface changed. `CNTR-MODERNIZATION-001` is read only. The `contract-verification.md` conclusions stand; the SC-1 wording issue (rejection surfaces at the handshake, not in `createSocket`) is correctly followed by the tests and routed to a change-record addendum, not to this story.

## Runtime Claims Verified

- [x] The story makes no claim beyond what the tests assert at the handshake and socket-parameter level. Runtime-behaviour acceptance criteria (AC-5 to AC-10, AC-20, AC-21) are proved by real loopback handshakes; the controls and the mutation log are the embedded evidence.
- [x] The sibling plugin-source rule does not apply: the delegation states this project does not modify the plugin, and the mutations edited only files in this repository (each restored and hash-checked in the log).
- [x] UNVERIFIED (escape-hatch) claims: None.

## What I Verified Myself

| Check | Result |
|-------|--------|
| `git status --porcelain` and `git diff --stat 395000956bb46cb2acc6d66e62d4b5ef933a2d90 -- app/src/main k9mail-vendored build.gradle app/build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat` | Diff stat empty. Porcelain shows only the SNI test (modified), the six new test files, and `sdlc/` files. |
| `git diff` of `DefaultTrustedSocketFactorySniTest.java` | Insertions only (63 added, 0 deleted). The ten original methods are untouched. `PinnedCertificateSocketFactoryTest.java` is not modified. |
| Test count, `git grep -h --untracked "@Test" ... \| grep -c "@Test"` and the HEAD form | 710 and 682. |
| Imports from `org.conscrypt` or `org.bouncycastle` in `app/src/test` | None. `ConscryptMode` and `LooperMode` appear only in `DefaultTrustedSocketFactoryJdkProviderTest.java`. |
| IC-6 grep (sleep, `getByName`, `localhost`, `127.0.0.1`, string `InetSocketAddress`) over the new files | No hit. |
| IC-12 `git grep` of `LocalKeyStore\|setKeyStoreLocation\|addCertificate` in `app/src/main` | No hit. |
| IC-8 pattern grep over the six sources and the workspace | One hit: `compiled-pipeline.json` (W4). Nothing in the tests or evidence. |
| Fixture facts | All six certificate fingerprints, subjects, SANs and dates equal the Javadoc; four leaf keys, no CA key (N3). The PKIX validator agrees with the intended defect of each fixture. |
| `SNIHostName` on `bad_host.example`, `host.example.`, `-x.example` | All throw `IllegalArgumentException` (W1). |
| Gradle, one run: the five new/modified classes plus `PinnedCertificateSocketFactoryTest`, `--rerun` | `BUILD SUCCESSFUL in 31s`; 8, 9, 5, 12, 4 and 8 tests, 0 failures. |
| Mutation log | Read the summary tables and about 30 entries against the test sources: M-A1, M-A1g, M-A1c, M-A2a, M-A2b, M-A3, M-A4, M-D6a, M-D6b, M-D7, M-D15, M-D8, M-D10, M-D11, M-D12, M-D13, M-D14, M-B4, M-C1 (compile fail), M-C2, M-B2, M-B5, M-B1r, M-C1r, M-E1, M-E2, M-E2-jdk, M-E3, M-E4, M-E5. Every verdict matches the recorded failing and passing tests. |

Mutation judgement under the plan's Criterion Status Rule:
- The two BY-DESIGN entries (M-A1g, M-A1c) are justified by AC-1's own text, and AC-2 covers the guard alone. The catch alone is uncovered (W1).
- `NOTED-AC16` applies to M-D11 only: the Conscrypt-mode second test passes, the two JDK-provider tests fail, as the story allows.
- M-B1 and M-C1 did not compile. The `if (false)` replacements keep the compiler's exception analysis legal and have the same meaning. They fail exactly the intended tests (two and four).
- M-E2 was not detected under the default provider. The Criterion Status Rule marked AC-19 unmet, the product owner moved the test, and M-E2-jdk detected it. This is the rule applied correctly.
- Every "Breaks when" clause of AC-1 to AC-21 has a detected mutation. The one clause not run is a cipher-call variant for AC-18 (M-E1 used a protocol call); the same code path is exercised, so I do not count it.

Deviations 7, 9, 13, 14, 15 and 16 are each recorded in the story's Implementation Notes, the log and the relevant Javadoc. None weakens a criterion beyond what those notes say. Deviation 7 left the W2 weakness, which the notes did not admit; W2 is now resolved.

## What I Did Not Verify

- I did not re-run mutations and I did not run the full gate. I relied on `evidence/I7-gates.txt` (710 tests, 0 failures, 2 skipped, release build) for that.
- I did not re-run the ordered suites for IC-9, the three repeat runs, or any run under a different fork distribution. I read their recorded outputs.
- I did not check how Robolectric 4.15.1 keys its sandboxes. I rely on the developer's I2b observation and on the guards, which would fail with a name if the isolation broke.
- I did not run the literal commit-based forms of IC-2, IC-4 and IC-8, because nothing is committed.
- Behaviour on a real device (Conscrypt on Android) is not covered by these tests, as the story states.

## Recommendations for the Retro

- **R1.** Treat "BY-DESIGN" mutation verdicts as a finding class of their own: when the plan accepts that one layer of a two-layer guard is unproved, the log should say which layer and what breaks if it goes (W1).
- **R2.** The JDK-provider class relies on a Robolectric sandbox key and a JVM-wide tripwire. If another Conscrypt-mode test ever needs SunJSSE, the structural answer is a separate Gradle test task or fork for the JDK-provider class. That needs a build change and a decision.
- **R3.** When the product owner moves a criterion to another class (deviations 14 and 16), update the story's ACs and counts in the same turn, not only the Implementation Notes (W3).
- **R4.** The planned verifier-swap guard left a stale string (N2). A plan step "remove guards for abandoned mechanisms" would help.
- **R5.** The `activity` package LINE figure alternates between 189 and 199 with production unchanged (log I7.6). It is not caused by this story, but it makes the "not lower than at I0" rule of IC-5 fragile for any package that has such a figure.

## Delta re-check after fix-in-cycle remediation (2026-11-20)

**Verdict of the re-check: PASS.** W2, W3 and N2 are resolved, the diff contains only what those items require, and no recorded mutation verdict changes.

### Item status

| Item | Status | Basis |
|------|--------|-------|
| W2 (AC-15 precondition) | Resolved | New assertion in the Hardening test, see region 1 below. |
| W3 (story synchronised) | Resolved | Five "As built" insertions in the story file, see regions 3 to 7. |
| N2 (stale verifier string) | Resolved | One condition removed in the Handshake test, see region 2. |

### Diff-scope audit: every changed region found

I did not rely on the saved diffs alone. I checked the post hashes with `git hash-object` and read both saved diffs in full. Because the files are untracked, I also compared them with my first-pass notes: line 173 in the Handshake file and the AC-15 test at `:291-323` in the Hardening file are the regions I had named.

| # | File and region | Item | Finding |
|---|-----------------|------|---------|
| 1 | `DefaultTrustedSocketFactoryHardeningTest.java`, AC-15 test: Javadoc sentence extended (2 lines replaced by 2 lines), and a new `assertWithMessage("D6'': no unknown suite remains ...").that(remainder.size() - knownInRemainder.size()).isAtLeast(1)` placed right after the existing "fewer than 2 known" assertion | W2 | Correct. `knownInRemainder` holds the baseline-known suites present in `remainder`, so the difference is the number of unknown suites. It is placed before checks (ii) and (iii), so those can no longer pass on an empty set of unknown suites. The message names D6'' like its neighbours. No other line in the diff hunk. |
| 2 | `DefaultTrustedSocketFactoryHandshakeTest.java:173`: `verifierName.contains("SanDnsHostnameTestVerifier") \|\|` deleted; the `startsWith("com.zegoggles.smssync.mail.ssl.")` condition and the `fail(...)` stay | N2 | Correct, and the only change in that file. |
| 3 to 6 | Story file, one indented "As built" line under AC-3 (line 82), AC-4 (84), AC-19 (120), IC-4 (138) | W3 | Accurate: AC-3 and AC-4 point to `DefaultTrustedSocketFactoryJdkProviderTest.java` (deviation 14); AC-19 names `pinnedFactory_createSocket_implicit_setsNoServerNames_jdkProvider` (deviation 16), which matches the method I read; IC-4 says 682 to 710, 28 tests, split 2/5/9/8/4, which matches the test files, `I7-count.txt` and my own count. The AC text before each line is intact as far as I can read it. |
| 7 | Story, Affected Code: new table row for `DefaultTrustedSocketFactoryJdkProviderTest.java` (line 160), and one "As built" paragraph (line 170) | W3 | Accurate: "5 tests ... AC-3, AC-4, AC-19 and the two JDK forms of AC-16" matches the class; the paragraph's split and 710 are correct. |

Limits of the story check: the file is untracked and I do not have the pre-edit copy (hash `bee92c8be45f7f61cd0d99ef9b933ab70ce9eec0` was given). The old length was 329 lines and it is now 335, which is 4 AC lines, 1 table row and 1 paragraph line, so the edit is consistent with insert-only. I cannot prove byte-level preservation of the AC text, and I state that as unverified.

Other facts checked:
- The five other U-063 test files have the hashes in `evidence/I7-manifest.txt` and `evidence/FIX-manifest.txt` (`TlsTestFixtures` 4e013b43, `LoopbackTlsTestServer` 4eafda53, SNI 36eedaee, JDK-provider 049a1152, pinned create-socket dac59cca). The pre- and post-remediation hashes of the two changed files match the coordinator's figures: Hardening ed535dc9 (in `I7-manifest.txt`) to 7e034437 (today), Handshake 22a57393 to 5704390e.
- Production is clean: `git status --porcelain -- app/src/main k9mail-vendored` is empty, and the `git diff --stat` against the start commit over those paths, the build files, `gradle` and `gradle.properties` is empty.
- The working-tree test count is 710. `git status --porcelain -- app/src/test` shows the same seven entries as before (the modified SNI test and the six untracked files), so nothing was added or removed under `app/src/test`.
- Evidence: `FIX-targeted-cases.txt` ends `files=1 cases=8 pass=8`, and `FIX-junit-totals.txt` shows `files=2 tests=17 failures=0` for the targeted run and `files=77 tests=710 failures=0 errors=0 skipped=2` for the full gate. I did not run Gradle; the evidence settles the matter and I had no specific doubt.
- Items from the security review (the Runtime Claims line and the appended section in `implementation-log.md`, and the three rewordings in `BUG-019-factory-enables-tls10-and-tls11.md`) are outside my items. I did not audit their content, and I note that none of them is one of W2, W3 or N2.

I found no change in the diff that is outside what W2, W3 and N2 require.

### Mutation verdicts

**No recorded verdict is affected and no mutation needs re-running.**
- The W2 line is an added assertion that fails only when the remainder holds no unknown suite. The tests that failed under M-D6a, M-D6b, M-D8, M-D9, M-D14 and the others are the same tests as before. Every mutation that previously made the AC-15 test fail still makes it fail, at the same or an earlier assertion. The only change in behaviour is a new failure on a provider with no unknown suite, and the probe shows unknown suites today (the unmutated run after the edit passes, `FIX-targeted-cases.txt`). A mutation that left the AC-15 test green before can now only fail it if it removed all unknown suites, and that is M-D6b, which already failed it.
- The N2 line removed a condition that could never be true, because no class with that name exists (my first-pass grep). The guard's behaviour is identical, so M-B1 to M-B5 and M-C1 to M-C2 results stand.

### Status of the deferred findings

Unchanged, because the remediation touched none of them: W1 (defer-justified, the catch-only safety net is still uncovered), W4 (defer-justified, `compiled-pipeline.json`), and observations N1, N3 to N8 (defer-justified). The frontmatter `warnings` count is now 2 (W1 and W4), because W2 and W3 are resolved. `blockers` stays 0 and `verdict` stays PASS.

## Phase Completion Report
---
story_id: "U-063"
phase: "code-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-020/U-063/review-code.md"
story_status: "review"
current_build_phase: "review"
blockers: 0
warnings: 2
errors: []
---
