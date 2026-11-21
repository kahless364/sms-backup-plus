---
artifact_type: gate-result
gate: code-review
gate_id: "code-review"
checkpoint: quality-gate
story_id: "U-065"
verdict: PASS
iteration: 1
agent: "Code Reviewer"
timestamp: "2026-11-21T05:21:30Z"
base_commit: "b2c2dfd8d7f0f5de8627575e160215b24b7cc5d9"
blockers: 0
warnings: 2
notes: 7
---

# Code Review: U-065

## Review Summary

| Aspect | Status |
|--------|--------|
| Production correctness | PASS. Both files equal plan edits D1 to D6 and P1 to P4 (diff read against HEAD). Nothing older than TLS 1.2 can be enabled by any path, including the initialiser-failure branch. |
| Conformance to plan and story | PASS. No difference from the plan's code text. Trust-manager lines of the pinned factory untouched. Signatures unchanged. |
| Test correctness | PASS. Each of the 7 changed and 19 new tests asserts what its criterion says. No test that must stay unchanged was edited. No deletion, no `@Ignore`. |
| Mutation evidence | PASS. 14 entries, each with named failing tests of type assertion, Mockito verification error or the stated `IllegalArgumentException`. No compile error or timeout. Restore is proven by hash and I re-verified it. |
| Maintainability | PASS with notes. |

I found no blocker. Two warnings (W1, W2) are about things the evidence cannot show. Neither changes the verdict.

## Verified by me, not taken from the log

- Working-tree blob hashes (`git hash-object`): default factory `43e5a78ead7c4c32a6af2734514ab8ba5d5ccbd3`, pinned factory `21cc5f345977ab6fa161b2aa6925a04aea6a1479`. Both equal the values in `evidence/mutation-log.md`.
- Hash of the whole production diff (`git diff -- app/src/main k9mail-vendored | git hash-object --stdin`): `6d9a52e7fa59c26a9993da0611e6b50fd8266704`, equal to `PROD_DIFF`.
- `grep -n "if (false)"` over both production files: no match. No mutation text is present.
- Authoritative `@Test` count: 732 (HEAD was 713; +19). `git diff b2c2dfd8 -- app/src/test` shows 0 removed lines containing an `@Test` annotation. No `@Ignore` under `app/src/test/java/com/zegoggles/smssync/mail`.
- Hunk ranges of the five test files against the plan's allowed ranges: every hunk sits in imports, class Javadoc, constants, one of the seven changed tests, or an insertion after an unchanged method. No hunk touches U-064's three tests, the SNI test class (not in the diff), `PinnedCertificateSocketFactoryTest` (not in the diff), the eight existing handshake tests, or the three guard methods (the JDK guard region `:120-166` and the Hardening guard `:149-173` have no hunk).
- `git check-ignore -v` on `compiled-pipeline.json` of this workspace: matched by line 38 of the local exclude file.
- Search of the evidence folder, the implementation log and the touched test directory for a user name or profile path: no hit.
- No Gradle run by me. The tree equals the state the I6, I8 and I9 runs used (hashes above), so I did not repeat them.

## 1. Production correctness

`k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`

`protocolsToEnable` (`:172-185`): `reorder` over the supported list, then a filter by `ALLOWED_PROTOCOLS`, keeping order.

| Provider supports | Result |
|-------------------|--------|
| SSLv2Hello, SSLv3, TLSv1, TLSv1.1, TLSv1.2, TLSv1.3 | `[TLSv1.2, TLSv1.3]`. `reorder` puts the known `TLSv1.2` first, drops SSLv3, appends unknown names in original order. The filter then keeps two. Asserted by H3 case (i). |
| TLSv1.2 only | `[TLSv1.2]` (H3 case (ii)) |
| TLSv1.3 only | `[TLSv1.3]` (follows from the filter; not a named H3 case, see N4) |
| nothing in the allow-list | empty array, non-null |

- Empty array: `hardenSocket` (`:236-241`) passes it to `setEnabledProtocols`. The probe shows both Conscrypt (`c_empty_protocols`) and SunJSSE (`j_empty_protocols`) accept it with 0 enabled. A socket with no protocol cannot complete a handshake, so this fails closed at handshake time. No test runs such a handshake (W1). It cannot occur on API 21 and later.
- Order: known first (`TLSv1.2`), then unknown (`TLSv1.3`). The array is not a preference list (documented in H-doc).
- Initialiser-failure branch: `hardenSocket(sock, ciphers, null)` (`:240`) computes `protocolsToEnable(sock)` from the socket. Every name enabled still passes the allow-list, so nothing older than TLS 1.2 is reachable by any path. If `protocolsToEnable` throws in the initialiser (`:118`), the catch (`:119-122`) logs it, `enabledProtocols` stays null and `ENABLED_CIPHERS` keeps its value (assigned before the call). The same method then runs on the first `createSocket` and may throw again. Result: an unchecked exception leaves `createSocket` (fail closed). The log records this as the contract-verification O5 consequence. Before the change the failure branch left the platform's protocols in place (fail open). The cipher list in that branch is unchanged (platform suites stand).
- Null handling: a null `protocols` is the designed fallback. A null `ciphers` leaves the platform suites (as before). A null socket would give a `NullPointerException`, which no caller can reach.
- Static initialisation order: `ALLOWED_PROTOCOLS` is declared at `:97`, textually before the static block (`:100` on). `ORDERED_KNOWN_PROTOCOLS` and `BLACKLISTED_PROTOCOLS` come before it too. `ENABLED_PROTOCOLS` is a `static final` assigned once in the block. No forward-reference problem.
- Behaviour differences beyond the intended ones: the exception-type point above, and the failure-branch protocol fallback. Nothing else. `ENABLED_PROTOCOLS` is now a list that already carries the floor, so the old `reorder` on supported protocols is not repeated.
- Cast safety in the pinned factory: `SSLSocketFactory.createSocket()` and `createSocket(Socket, String, int, boolean)` from `SSLContext.getSocketFactory()` return `SSLSocket` on every provider in use. The default factory already casts the same way (`:210`).

`app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java`

- `:92-103`: both shapes go through one `sslSocket` variable, then `hardenSocket(sslSocket)`, then `setSniHost(socketFactory, sslSocket, host)`. This is the default factory's order (`:212-214` of the default factory). Both shapes get both steps.
- The trust-manager lines (`:88-92` after the edit) are outside every hunk. Constructor and `createSocket` signatures are unchanged. `K9MailTransport.java` has an empty diff.
- Null host: `setSniHost` goes to `setSniViaSSLParameters` on API 24 and later (the sandbox runs API 29), where `isIpOrBlankHostname(null)` is true, so it returns without calling the provider. The layered call `createSocket(socket, null, port, true)` is the same call as before the change. J5 covers a null host on the JDK provider, both shapes. On the pre-N branches the reflective or `SSLCertificateSocketFactory` path is used with a null name; that is the default factory's existing behaviour and is not changed by this story.
- Resource leak: if `hardenSocket` or `setSniHost` throws after `createSocket`, the new socket is not closed, in either factory. The default factory has the same pattern (`:210-214`) and had it before this story. In the pinned factory this is new exposure only in the failure cases named above, where the connection fails anyway (N2).

Comparison with the plan's exact text: D1, D2, D3, D4, D5, D6, P1, P2, P3 and P4 as built are character-for-character what the plan states (read side by side with the diff). No difference found.

## 2. Visibility and API surface

`git grep -n "hardenSocket\|protocolsToEnable\|ALLOWED_PROTOCOLS" -- app/src/main k9mail-vendored` shows these callers only: the initialiser (`:118`), the default factory's `createSocket` (`:213`), the one-argument `hardenSocket` (`:227`), the three-argument overload (`:240`), and the pinned factory (`:101`). Nothing else under `app/src/main` or `k9mail-vendored`. Tests reach the protected members by reflection, as they already do for `reorder`.

The choice (public one-argument `hardenSocket`, protected overload and `protocolsToEnable` and `ALLOWED_PROTOCOLS`) is the narrowest that lets the app package call the step. Under CR-009 the pinned factory's import and two calls must be retargeted. The plan records this and offers a checklist line (item 3 of "Items for the orchestrator"). The Javadoc of the public method names the pinned class, so a person who moves the class will find the dependency. No finding.

## 3. Tests

Seven changed tests (all renamed, none deleted): Hardening H1 and H2, JDK J1, J2 and J3, pinned C1 and C2. Each asserts the new behaviour and keeps or strengthens its preconditions with messages:

- H1, J1: supported-set equals supported intersect allow-list, no duplicates, `TLSv1.2` first. Precondition: the provider supports TLS 1.2.
- H2, J2: none of the old four on the direct and layered socket, TLS 1.2 and 1.3 each enabled iff supported. Preconditions: the provider enables TLSv1 and TLSv1.1 by default (Conscrypt), or supports them and leaves them off (JDK, A1-P2 kept). They cannot pass on provider defaults.
- C1, C2: protocol floor on the pinned socket, then set, order and cipher order against the default factory's socket of the same shape. Preconditions: provider enables TLS 1.0 and 1.1; the default factory's cipher order differs from the provider's. The comparison is not trivially equal because both of those preconditions are asserted.
- J3: exactly one `SNIHostName` equal to the host on the pinned direct socket, then protocols and cipher order asserted on the same socket after the step (AC-4 (d)). A precondition shows an unconnected default socket has no name.

Nineteen new tests:

- H3 (injected supported lists, four cases, allow-list snapshot) and H4 (null lists on a Mockito stand-in, protocols captured, `setEnabledCipherSuites` never called) test the step directly and are not provider-dependent.
- H5 (pinned cipher rule against the literal baselines) reuses a copy of AC-15's body so AC-15 stays unchanged.
- J4 (exact `{1.2, 1.3}`, both shapes), J5 (IP, blank, null hosts), J6 (three names `SNIHostName` rejects, guard asserted false first), J7 (parity per host and per shape with a cipher-order precondition). Class annotations (`@RunWith`, `@ConscryptMode(OFF)`, `@LooperMode(LEGACY)`) and the guard methods are intact (`:122` on, no hunk).
- Handshake: six tests (two at TLS 1.2 on both shapes, four refusals) and pinned create-socket: six tests, built on one helper pair per class.

Checks: all sockets and servers are closed (`finally`, try-with-resources via `Layered`, and the `@After` of the handshake classes). No `Thread.sleep`, no name resolution, no `localhost` text, loopback objects only. The loopback server's waits are bounded.

Unchanged tests: confirmed by diff hunk ranges (see "Verified by me"). The BUG-019 comment is kept above the changed Hardening legacy test (`:371-374` after the edit). The Javadoc of the three changed test classes and of both factories now states the new behaviour; I found no sentence that still says the pinned path leaves provider defaults untouched, except the history sentence that says so deliberately ("Until U-065 ...").

`LoopbackTlsTestServer` (diff read in full): the two-argument `start` has its old signature and body (it now passes null to the private constructor). The new three-argument `start` rejects a null or empty array and clones it. `serve` sets the protocols after `setUseClientMode(false)` and before the handshake. The existing callers are not in the diff.

## 4. Mutation evidence

I read the 14 entries and the per-mutation case files.

| Check | Result |
|-------|--------|
| Entries | M-P1-P8, M-P2, M-P3, M-P4, M-P5, M-P6-i, M-P6-ii, M-P6-iii, M-P7, M-F1, M-D11r, M-S5, M-E4, M-E5 = 14 |
| Failure type | Case files show `AssertionError`, `AssertionErrorWithFacts`, `WantedButNotInvoked` (M-F1), or `IllegalArgumentException` (M-S5, the stated type). Search of the evidence for compile or timeout text: no hit |
| Expected set against actual | Each entry states the expected set before the run and `expected: matches` after. I checked counts (18/45, 7/19, 3/9, 5/31, 2/9, 5/31, 11/45, 2/9, 4/19, 1/12, 1/21, 4/23, 5/11, 2/11) against the case files |
| Restore | Each entry has a `restored` line with `hash-ok` and `diff-hash-ok`. I confirmed the final tree matches those hashes myself |
| Controls | M-P1-P8 shows the eight refusal tests fail with `fail("... must be refused, but it completed at ...")` when the floor is removed, so the refusals are the floor's doing, not the server's. M-E4 and M-E5 show `handshake_certificateOtherThanEnrolled_isRejected` still fails |
| Honest limits | M-D11r is caught only by H3; M-P7 is caught on the JDK provider only by the order check of J7 (J4 passes). Both are stated in the log and the Javadoc |

I4's red run (19 of 68 failing against the old production code; H3 `NoSuchFieldException`, H4 `NoSuchMethodException`) shows the new tests were red before the change. J4, J5 and J6 pass red by design (N3).

## 5. Additive option on `LoopbackTlsTestServer`

Covered in section 3: old signature unchanged, existing callers untouched, additive overload with input check. No finding.

## 6. Implementation log

Claims I tested: counts (713, 732, +19, 7 renamed, 14 mutation entries, 68 cases, 2 skipped), hashes, the file list and the "no probe class in the test tree" claim (only the string `U065_UNKNOWN_PROTOCOL` in H3 matches `U065` under `app/src`) all hold. The declared deviations (hyphenated mutation ids, Javadoc applied as several exact edits, two helper scripts outside the repository) are acceptable and do not affect the result. The runtime-claims section is declared empty and says why. The story makes behavioural claims (handshake refused, TLS 1.2 completes), which are proven by running real loopback handshakes and by M-P1-P8, so declaring none of the five runtime classes is fair.

Small inconsistencies (N7): the summary says "no deviation from the plan's text of the production or test code" while deviation 4 lists edit granularity (the result is equal text, so it is harmless). Step I2's clock column reads "2026-11-21 (run started after the clock shown in the work folder)", which is not a timestamp.

## 7. Files to be committed

- The seven files under `app/` and `k9mail-vendored/`, the story file's Implementation Notes, and the workspace documents. The three pipeline files outside this story (`sdlc/.skill-context.json`, U-064 `qa-results.md`, the sprint plan and U-064 story edits) are outside this review.
- `compiled-pipeline.json`: ignored by the local exclude file, verified.
- No raw log, no image, no probe class in `app/src/test`. Search for user name and profile path across the evidence and tests: none.
- `evidence/tools/U065ConscryptProbeTest.java.txt` and `U065JdkProbeTest.java.txt`: acceptable evidence. They are inert text (not compiled, not under a source set) and they let a reader reproduce the I2 probe, which the AC-8 (b) and AC-4 reasoning depend on. Keep them (N6).

## Findings

### Blockers

None.

### Warnings

- **W1** `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java:160-169` (Javadoc of `protocolsToEnable`), and `:228-240`. Warning. The Javadoc says an empty result means "the socket then cannot complete a handshake (or the platform refuses the empty list), so the connection fails instead of falling back to an older version." No test or probe runs a handshake from a socket whose enabled list is empty, under Conscrypt or SunJSSE. The probe shows only that both providers accept the empty array. On SunJSSE no usable protocol certainly means no handshake, and Conscrypt is expected to behave the same, but that is a read, not a measurement. The case cannot occur on API 21 and later, so risk is low. Suggested fix: when a later story next touches these tests, add one loopback handshake against a stand-in with `setEnabledProtocols(new String[0])` on both providers, or soften the Javadoc to "expected to fail". Classification: defer-justified: unreachable on the supported Android versions, and the production code is the only fail-closed behaviour available; a probe is a follow-up, not a defect.
- **W2** `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHardeningTest.java:177-188` and `.../DefaultTrustedSocketFactoryHandshakeTest.java:458-468`. Warning. After this story the `SSLv2Hello` clause of guard A1-G2 is dead (the factory never enables SSLv2Hello), and the method Javadoc still says "SSLv2Hello and DHE suites appear only under the JDK provider". The class Javadoc states the new reading truthfully (`Hardening:104-105`, `Handshake:128`), and the plan keeps the guard code byte-identical so that AC-6 (d) holds literally. The protection of the guard is intact because the DHE clause still fires. The failure message of the dead clause is the only text that could mislead a later reader. Suggested fix: after the commit, in a later story that is allowed to touch the guards, drop the dead clause or add a sentence at the method Javadoc. Classification: defer-justified: AC-6 (d) requires the guards to stay unchanged in this story, and the class Javadoc already records the reading.

### Notes

- **N1** `k9mail-vendored/.../DefaultTrustedSocketFactory.java:97`. `ALLOWED_PROTOCOLS` is a mutable `protected static final String[]`, like the other lists in the class. Within the vendored package only. Classification: defer-justified: it follows the file's existing pattern and CR-009 moves the class.
- **N2** Resource handling in `PinnedCertificateSocketFactory.java:92-103` and `DefaultTrustedSocketFactory.java:210-214`. If a step throws after `createSocket`, the socket is not closed. The default factory had this before the story; the pinned factory now shares it. A throw needs a platform that refuses a cipher or protocol list, and the connection fails in that case. Classification: defer-justified: same pattern as the existing default factory, and the fix (a try/catch that closes) belongs in a change that covers both factories together.
- **N3** J4 (`pinnedFactory_createSocket_enablesExactlyTls12AndTls13_bothShapes_jdkProvider`) can pass against an untouched pinned socket under SunJSSE, because the provider's default is already `{1.3, 1.2}`. The test Javadoc says so and names M-P3 as its detector; parity test J7 detects the untouched case by order (M-P7). Not a test that passes for the wrong reason, because its stated job is the widening. Classification: defer-justified: the limitation is documented and covered by J7.
- **N4** H3 has no case for a platform that supports TLS 1.3 only. The filter and the allow-list make it follow from case (iii), and the plan's check shows `[TLSv1.3]`. Cheap to add when someone next edits H3. Classification: defer-justified: behaviour follows from the covered cases.
- **N5** Duplicated test helpers: `assertProtocolFloor`, `OLD_PROTOCOLS`, `ALLOWED_PROTOCOLS_BASELINE`, `assertSameHandling`, `quote`, `Layered` and `quietClose` appear in two to four test classes. The plan accepts this (private helpers per class, as U-063 did). The next protocol-list change costs four edits, and at CR-009 the retarget is likewise spread across four classes. Classification: defer-justified: stated plan decision; a shared test helper is a separate refactor and `TlsTestFixtures` was kept untouched on purpose.
- **N6** The two probe sources kept as `.java.txt` under `evidence/tools/` are fine as evidence (see section 7). Classification: defer-justified: they document how the I2 results were obtained and contain no personal data.
- **N7** Implementation-log wording points in section 6 (summary says "no deviation", step I2 has no clock). Classification: defer-justified: documentation only; deviation 4 declares the point and the evidence is complete.

## Patterns Verified

- [x] Follows existing code patterns (extraction of an existing step, `protected static` like `reorder`, per-class private test helpers)
- [x] Error handling is appropriate (fail closed for protocols; unchecked exception type change noted)
- [x] Tests cover new functionality (19 added, 7 changed, each shown red then green, each mutation detected)
- [x] No hardcoded values that should be configurable (allow-list is a named constant with a documented trade-off)
- [x] No unnecessary complexity

## Integration Verified

- [x] New code is reachable from production entry points: `K9MailTransport.java:145` and `:148` build both factories; the default factory is also built at `Transport.java:26` and `RemoteStore.java:52`, `:56`. `createSocket` of each calls the new steps
- [x] Registries or dispatch maps: none apply
- [x] Function signatures match at all call sites: `hardenSocket(SSLSocket)` callers are the default factory and the pinned factory; the three-argument overload is called from the one-argument form only (and by test reflection)
- [x] No dead code introduced (the dead guard clause in tests is noted as W2)
- [x] Integration path documented in implementation-log.md

## Regression Check (replacement or rewrite)

- [x] Capabilities Inventory present in implementation-log.md
- [x] RETAINED items verified present (default factory `createSocket`, `reorder`, lists, server-name methods have no hunk; pinned trust code has no hunk)
- [x] INTENTIONALLY REMOVED items cite story requirements (old protocols: AC-1, Record 8; pinned "provider defaults untouched": AC-2 to AC-4, Records 8 and 9)
- [x] No capability missing from the inventory

## Contract Verification

- [x] Consumers identified: five construction sites, `WebDavSocketFactory.java:67` (`setSniHost`, unchanged)
- [x] No field name, event name or data shape changed; `TrustedSocketFactory.createSocket` signature of both factories unchanged
- [x] The contract's trust behaviour for both producers is untouched (trust tests unchanged and passing; M-E4 and M-E5 detected)

## Runtime Claims Verified

- [x] `## Runtime Claims` inventory read: declared empty with a reason, not omitted
- [x] No declared claim, so no probe to judge; the behavioural claims are tested by real loopback handshakes and the control mutation
- [x] Probe design adequacy: not applicable (no declared claim). The I2 probe ran in the Gradle test sandbox only, with sources kept as text
- [x] Escape-hatch justifications: none
- [x] Completeness audit against the story's runtime-behaviour ACs: AC-8 (handshakes) is covered by tests and mutation evidence; no AC lacks coverage. The empty-protocol-list handshake is the one statement that rests on a read (W1)
- [x] No probe wrote into the sibling plugin-source repository (this project has none; the probes wrote only to the test tree, which was cleaned before I3, per `I2-cleanup.txt`)
- [x] UNVERIFIED (escape-hatch) claims: None

## Phase Completion Report
---
story_id: "U-065"
phase: "code-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-021/U-065/review-code.md"
story_status: "in-progress"
current_build_phase: "review"
errors: []
---
