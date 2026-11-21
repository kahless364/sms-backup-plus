---
artifact_type: review-code
story_id: "U-066"
verdict: "PASS"
agent: "Code Reviewer"
timestamp: "2026-11-21"
blockers: 0
warnings: 0
---

# Code Review: U-066 (prune weak TLS cipher suites)

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | Met. CR-014 Amendment 1 statements C1 to C4 and B1 hold in the code (read below). No change record, contract or requirement file is edited by this story's files. |
| Test coverage | Met. 14 tests added (732 to 746), 12 changed, none deleted, no `@Ignore` or `Assume`. Each sampled mutation row was checked against the test code and the named tests would fail. |
| Code quality | Good. Small production change, no dead code of the story's making, comments and Javadoc true on read. Findings are Low or Info only. |

## Verdict: PASS

The production change in `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` is correct against every acceptance criterion and binding ruling. The tests are deterministic, close their resources, and detect the behaviour they name. No finding must be fixed in this cycle.

## What I read and ran

- Read in full: the production diff and the final text of `DefaultTrustedSocketFactory.java` lines 1-260; the whole of the story file; the implementation log; `mutation-log.md`; the full diff of `LoopbackTlsTestServer.java`, `DefaultTrustedSocketFactoryHandshakeTest.java`, `DefaultTrustedSocketFactoryJdkProviderTest.java`, `PinnedCertificateSocketFactoryCreateSocketTest.java`; lines 106-490 and 630-991 of `DefaultTrustedSocketFactoryHardeningTest.java`. Plan, plan review and contract verification were searched, not read end to end.
- Ran once: `./gradlew :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.ssl.*" --tests "com.zegoggles.smssync.mail.PinnedCertificateSocketFactory*"` in a shell without `MSYS_NO_PATHCONV`. BUILD SUCCESSFUL (1 task executed, tests ran, none failed).
- `git status --short -- app k9mail-vendored` before and after: the same seven ` M` lines, no `??`, nothing left behind.
- No source or test file was modified by this review.

## Correctness of the production change

1. `ALLOWED_CIPHERS` (`DefaultTrustedSocketFactory.java:51-65`): ten entries, in the ruled order L-A (three TLS 1.3, four TLS 1.2 AES-GCM in the pre-U-066 order, two ChaCha20-Poly1305, SCSV). Identical to the literal in the Hardening and JDK tests and to the story's L-A.
2. `ciphersToEnable(SSLSocket)` (`:182-191`): reads only `getEnabledCipherSuites()`; iterates the allow-list, so the result is in allow-list order, contains no name off the list, and is never a supported-but-not-default suite. An empty result is returned as an empty array with no fallback (fail closed, as ruled). `git grep getSupportedCipherSuites` over `app/src/main` and `k9mail-vendored/src/main` returns nothing.
3. Failed initialisation (`:242-245`): a null cipher list now computes `ciphersToEnable(sock)`; a non-null empty list is set as it is. If `ciphersToEnable` throws, the exception leaves `createSocket`; no socket is returned. The static initialiser (`:85-113`) still leaves both lists null if the provider query throws, and the null branch then covers both lists. Static-initialisation order is correct: `ALLOWED_CIPHERS` is declared before the `static` block that calls `ciphersToEnable`.
4. Both socket shapes of both factories: `createSocket` calls `hardenSocket(sslSocket)` for the direct and layered shapes alike (`:218-220`); the pinned factory reaches the same public `hardenSocket(SSLSocket)` and then `setSniHost` (unchanged code, class files byte-identical per `I9-checks.txt`).
5. Protocol floor, server-name code, trust code and the signatures are not touched: the diff hunks are the constant block, the static initialiser line, the `reorder` comment, the new method, and the two lines of `hardenSocket`.
6. `PinnedCertificateSocketFactory.java`: one Javadoc line (`:57`), comment only, consistent with the P-1 ruling; the class file is identical before and after.
7. `reorder` is not dead: `protocolsToEnable` (`:157-159`) still calls it with `ORDERED_KNOWN_PROTOCOLS` and `BLACKLISTED_PROTOCOLS`. The comment edit at `:138-139` is true ("only `protocolsToEnable` calls this" holds for production; the tests call it by reflection and say so).

## Findings

### Blockers

None.

### Warnings

None.

### Observations

- **O-1 (Low). `DefaultTrustedSocketFactory.java:51-65` and `:182-191`, the empty-result behaviour is platform-dependent and recorded, not asserted.** If a platform's default list holds no allowed suite, the producer sets an empty list. Probe I2 shows Conscrypt still reports and negotiates its three TLS 1.3 suites with an empty array, while SunJSSE refuses. On the two measured emulators this cannot occur (all ten names enabled by default). The Javadoc says the platform's behaviour is the platform's, which is true, and CR-014 leaves it "Not stated". The code is right for the ruling; no action. Must fix in this cycle: no. — Classification: defer-justified — the contract's "Not stated" and the orchestrator's ruling leave the platform's handling of an empty list open, and both measured devices enable all ten suites.
- **O-2 (Low). `DefaultTrustedSocketFactoryHardeningTest.java:886-904` and `DefaultTrustedSocketFactoryJdkProviderTest.java` (end of file): `weakCategories` is duplicated verbatim in two test classes.** A future change to W1 to W12 must be made twice. Harmless today because both copies are exercised by exact-list assertions. Must fix in this cycle: no. — Classification: defer-justified — a shared test helper is a cross-class refactor outside the story's scope, and the CR-009 move retargets these classes anyway.
- **O-3 (Low). `DefaultTrustedSocketFactoryHandshakeTest.java` `assertRefusedByCipherPolicy` and `PinnedCertificateSocketFactoryCreateSocketTest.java` `assertPinnedHandshakeRefusedByCipherPolicy`: the refusal tests assert "an `SSLException` whose direct cause is not a `CertificateException`, server not `OK`", not the TLS alert.** A refusal for an unrelated reason would also pass. The positive control that the server does serve that suite is not in the test itself; it is in probe I2 and in mutations M-X1 (a)/(b), M-X2, M-X5 (i)/(ii), M-X6, where the same handshake completes and these tests fail. That control is sufficient evidence for this story; the weakness is only that the in-tree test cannot show it later. Must fix in this cycle: no. — Classification: defer-justified — the criterion (AC-6) is written at exactly this strength and the mutation evidence supplies the control.
- **O-4 (Info). `DefaultTrustedSocketFactoryHardeningTest.java:726-734`/`:736-757` (`providerFixedSuites` Javadoc).** The Javadoc line wraps unevenly ("the first allowed suite with _WITH_ (a TLS 1.2 suite) that the provider lists"); it is accurate. No action. — Classification: defer-justified — cosmetic only, and the file is retargeted at the CR-009 move.
- **O-5 (Info). Handshake and pinned test classes carry the same `TLS13_SUITES` and `AEAD12_SUITES` literals** (each as the full name lists). They are literals by design (AC-1 (b): not computed from the factory). No action. — Classification: defer-justified — duplication of literals is the criterion's own requirement.
- **O-6 (Info). The `reorder` unknown-item comment is now only true of the protocol path, and `reorder`'s `blacklisted == null` branch is reached only by a test (`reorder_withNullBlocklistAndEmptyInput_behavesAsRecorded`).** Not dead code (the null check is pre-existing vendored behaviour); the story forbids edits to the protocol code. No action. — Classification: defer-justified — the vendored `reorder` is left as it was by the story's AC-11 and the CR-009 move owns it.

## Mutation-log verification (sampled by reading the tests, not by running)

I read eight rows against the test code. All eight would fail as logged:

| Row | Read against | Why it fails |
|-----|--------------|--------------|
| M-X7 (null-branch fallback removed) | `hardenSocket_withNullLists_appliesTheAllowedSuitesAndTheProtocolFloorFromTheSocket` `:646-651` | `verify(socket).setEnabledCipherSuites(...)` finds no call: `WantedButNotInvoked`. Logged exception type matches. |
| M-X10 (non-null empty list skipped) | same test, case (c), `:663-668` | `hardenSocket(emptyList, new String[0], ...)` then `verify(emptyList).setEnabledCipherSuites` finds no call. Logged type matches. |
| M-X9 (supported instead of enabled) | `ciphersToEnable_...` case (iii) `:425-431` and H9 case (b) `:656-661` | Case (iii) enables four suites and supports all 62; the mutation returns more than the expected three. Case (b) enables none of the allowed suites and supports the input; the mutation returns ten, `isEmpty()` fails. Real-socket tests cannot tell (both providers support all ten), which the log states. |
| M-X8 (two TLS 1.2 entries swapped) | `cipherConstants_matchRecordedBaseline` `:353-354`, `assertAllowedCipherList` `:858-860` | `containsExactlyElementsIn(...).inOrder()` against a literal. |
| M-X6 (layered shape skipped) | `createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes` `:382-386`; `startTls_serverLimitedTo*_handshakeRefused` | The layered socket keeps the provider defaults, not the ten; the STARTTLS client offers the pruned suite and completes, so `fail(...)` fires. Direct-shape tests pass, as logged. |
| M-X1 (e) (static-ECDH name put back, offered by no provider) | `allowedCipherConstant_holdsOnlyTheAllowedNameForms` `:459-471`, `hasAllowedForm` `:907-925`; step test (i) | `TLS_ECDH_RSA_...` does not start `TLS_ECDHE_`, so the form check fails; step test (i) feeds the name in via `KNOWN_CIPHERS_BEFORE_U066`. Socket-level tests cannot detect it (no provider enables it), as the story expects. |
| M-X4 (ii) (SCSV removed) | `assertAllowedCipherList` on Conscrypt and JDK | The factory's list lacks SCSV, which both providers enable; exact-list fails. `allowedCipherConstant_...` passes (forms hold), as logged. |
| M-X5 (i) (pinned `hardenSocket` call under `if (false)`) | `pinnedFactory_..._bothShapes` `:679-687`, the four refusal tests | Pinned socket keeps provider defaults; list differs; server-limited-to-pruned-suite handshake completes. |

The narrowings the log declares (M-X4 (i) cannot fail the Conscrypt exact-list or handshake tests because the TLS 1.3 suites are provider-fixed there; M-X5 (ii) cannot fail the pinned exact-list tests) are true by construction and are covered on the JDK-provider class and by the parity tests. M-X1 (e) being detectable only at the constant and step level is the story's own expectation. The one MISMATCH (M-X5 (ii), first run: a Robolectric `.robolectric-download-lock` class-level error) was an environment fault; it was repeated once with the expected 17 failures and logged honestly.

## Consistency of the numbers

- 732 to 746: `git grep` on HEAD gives 732, a count over the working tree gives 746; the difference of 14 equals the tests named in the log (Hardening 2, JDK 2, Handshake 5, pinned 5).
- 82 cases: `I6-green-cases.txt`, `I9-all7-cases.txt` each have 82 PASS rows over seven classes (15 + 1 + 8 + 19 + 14 + 11 + 14); the red run (`I4-red-cases.txt`) has 65 PASS and 17 failing rows (5 + 8 + 2 + 1 + 1 by exception type), matching the log's "17 expected".
- 15 mutation runs plus 2 guard runs: counted from `mutation-log.md`: M-X1 a to e (5), M-X2, M-X4 i and ii, M-X5 (i), M-X5 (ii) repeat, M-X6, M-X7, M-X8, M-X9, M-X10 = 15; G-1 and G-2 = 2. The non-counted first M-X5 (ii) run is disclosed. Every entry has a write-ahead line and a `restored` line with hash, porcelain and diff-hash checks.
- Gate: skipped 2 before and after (`I9-junit-totals.txt`: tests 746, failures 0, errors 0, skipped 2).

## Test determinism and resource handling

- Loopback sockets use OS-assigned ports and bounded waits (class Javadocs record 10-second bounds); the server thread is a daemon on a single-thread executor and the server is `AutoCloseable`. No sleeps, no retries.
- `Layered` (Hardening, JDK) closes the TLS socket, both plain sockets and the listener, and closes already-opened plain sockets if construction throws. `enabledCiphersOf` and `providerFixedSuites` close their sockets in `finally`.
- The cipher overload of `LoopbackTlsTestServer.start` validates both arrays and clones them; the two existing overloads delegate with `null` suites and behave as before.
- The isolation guards are live: G-1 (33 of 33 fail naming SunJSSE) and G-2 (11 of 11 fail naming Conscrypt) show both can fire, and the dead DHE clauses are gone. The `ConscryptMode`-dependent guard in the JDK class keeps the A1-G3 initialiser tripwire.

## Scope check

The seven files in `git status --short -- app k9mail-vendored` are exactly the planned set. No dependency, build file, `K9MailTransport.java` or `TlsTestFixtures.java` edit. The records-end hygiene observation in the implementation log (other agents' uncommitted files under `sdlc/artifacts/requirements` and `design/contracts`) is correct: those files are not part of this story's diff.

## Patterns Verified

- [x] Follows existing code patterns (allow-list as a `protected static final String[]`, a step modelled on `protocolsToEnable`, reflection seams as in U-065)
- [x] Error handling is appropriate (fail closed on an empty result; a throwing step surfaces from `createSocket`)
- [x] Tests cover new functionality (exact lists on both providers, both shapes, both factories; name forms; step-level input; null branch; negotiated suites; refusals)
- [x] No hardcoded values that should be configurable (the allow-list is the intended single place; the trade is recorded for the product owner)
- [x] No unnecessary complexity

## Integration Verified

- [x] New code is reachable from production entry points: `DefaultTrustedSocketFactory.createSocket` (both shapes) -> `hardenSocket(SSLSocket)` -> three-argument `hardenSocket` -> `ciphersToEnable`; the pinned factory -> the same public `hardenSocket`; the static initialiser -> `ciphersToEnable`
- [x] Registries/dispatch maps: none introduced
- [x] Function signatures match at all call sites (`ciphersToEnable` has one production caller pair and the test seam; the removed constants appear nowhere)
- [x] No dead code introduced
- [x] Integration path documented in implementation-log.md (IC-2 grep accounted for)

## Regression Check (replacement/rewrite)

- [x] Capabilities Inventory present in implementation-log.md
- [x] RETAINED items verified: initialiser (`:85-113`), `reorder` still called by `protocolsToEnable` (`:157-159`), protocol and server-name code unchanged, pinned call sequence (`PinnedCertificateSocketFactory.java:101-102`)
- [x] INTENTIONALLY REMOVED items cite DP-2, DP-3, DP-4 and AC-8
- [x] No capability missing from the inventory

## Contract Verification

- [x] Consumers of the modified interface identified: `createSocket` callers (`K9MailTransport`, vendored `Transport`/`RemoteStore`) and the pinned factory; signatures unchanged
- [x] No field, event or shape change; behaviour only (the enabled suite list)
- [x] No contract mismatch. CR-014 Amendment 1: C1 (default-enabled only), C2 (nothing off the list is enabled), C3 (allow-list order), C4 (null branch computes from the socket), B1 (one shared step) are each made true and tested.

## Runtime Claims Verified

- [x] `## Runtime Claims` inventory read: declared empty in the implementation log, with the reason (behavioural claims proven by unit tests under two desktop providers and mutation evidence; no crash, concurrency, timing, filesystem or hook claim). The story's runtime-behaviour criteria are IC-1 and IC-3, both assigned to the orchestrator or QA, so no under-declaration.
- [x] No declared claim lacks a probe; none is resolved by escape hatch
- [x] No probe wrote into the sibling plugin repository (probes were test classes in `app/src/test`, deleted before I3; sources kept as `evidence/tools/*.java.txt`)
- [x] UNVERIFIED (escape-hatch) claims: None
- Disclosed, not a claim of this story: IC-1 (emulator backup run) is still PENDING with the orchestrator; nothing in this review verifies it.

## Phase Completion Report
---
story_id: "U-066"
phase: "code-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-022/U-066/review-code.md"
---
