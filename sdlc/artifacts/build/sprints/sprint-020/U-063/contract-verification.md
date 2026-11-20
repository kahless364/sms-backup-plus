---
artifact_type: gate-result
gate: contract-verification
gate_id: "contract-verification"
checkpoint: plan-gate
story_id: "U-063"
verdict: PASS
iteration: 3
agent: "Solution Architect (separate instance)"
timestamp: "2026-11-17"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-020/U-063/plan.md"
base_commit: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
contracts_checked:
  - CNTR-MODERNIZATION-001
  - CNTR-MODERNIZATION-007
blockers: []
warnings: 2
notes: 7
---

# Gate Report: gate-result — U-063

## Gate: contract-verification @ plan-gate

## Verdict: PASS

## Summary

If the plan is carried out exactly, it honours CNTR-MODERNIZATION-001 and CNTR-MODERNIZATION-007. Its
claims about the contracts are true, apart from two harmless line-reference slips (N1). None of the
tests, helpers, fixtures or substitutes adds a permissive trust manager, a verifier that accepts any
host, or a third trust path. Nothing in production can reach the test code. The mutation protocol
restores every production file to its base version byte for byte, and the plan checks that before the
next step. The SC-1 mismatch is real and the plan handles it correctly: the tests follow the code, and
it is a wording problem in the contract, not a code defect, so no bug is needed now. However, no
approved change record carries the correction, and CR-007's proposed text for `:196` keeps the
inaccurate words (W1). Separately, the plan does not say which assertions depend on the vendored trust
manager, and those would have to be rewritten if Part B chooses upstream's trust manager (W2). Neither
finding makes the implementation break a contract or state something false about one. Both can be
fixed without re-planning.

## Sources read in this session

| Artifact | Path |
|----------|------|
| Plan under review (iteration 3) | `sdlc/artifacts/build/sprints/sprint-020/U-063/plan.md` (Contracts Verification, I0b, I1-I6, Global State, Notes, Verdict sections read in full) |
| Story | `sdlc/artifacts/build/sprints/sprint-020/U-063-tls-socket-factory-characterisation-tests.md` |
| CNTR-MODERNIZATION-001 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md` (read in full) |
| CNTR-MODERNIZATION-007 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-007-mail-transport.md` (Validation Rules, Dependencies, Notes) |
| CR-005 | `sdlc/artifacts/build/change-records/CR-005-cntr-007-package-names-after-mail-migration.md` (Rule 1 and AC-B3 scope) |
| CR-007 | `sdlc/artifacts/build/change-records/CR-007-cntr-001-createsocket-exception-class.md` (read in full) |
| CR-008 | `sdlc/artifacts/build/change-records/CR-008-cntr-001-socket-factory-slot-is-factory-parameter.md` (finding) |
| CR-009 | `sdlc/artifacts/build/change-records/CR-009-cntr-001-default-trusted-socket-factory-producer.md` (read in full) |
| U-062 analysis, Records 5 and 6 | `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` (appendix "Product owner decision") |
| Default factory | `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` (read in full) |
| Trust manager | `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/TrustManagerFactory.java` (read in full) |
| Exceptions | `k9mail-vendored/src/main/java/com/fsck/k9/mail/CertificateChainException.java`; `CertificateValidationException.java:10` |
| IMAP connection | `k9mail-vendored/src/main/java/com/fsck/k9/mail/store/imap/ImapConnection.java:95-334`, `:656-734` |
| Pinned factory | `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` (read in full) |
| Existing pinned test | `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryTest.java` (`@Test` and seam usage) |

## Findings

### Blockers

None.

### Warnings

**W1. SC-1 is sent to a revision that no approved change record carries. CR-007's proposed text keeps the wording.**
- Plan: Contracts Verification, row 1 ("the wording belongs to the combined revision"); the section's
  lead-in ("CR-007 and CR-009 own the combined revision"); Notes item 6.
- Contract: `CNTR-MODERNIZATION-001:191-192` ("exceptions thrown from `createSocket(...)` at
  TLS-handshake time") and `:196` ("surfaced from `createSocket(...)`").
- Evidence. The code bears out the story's SC-1:
  - `DefaultTrustedSocketFactory.createSocket` (`:152-180`) returns either an unconnected socket
    (`:170`) or a layered socket (`:172`). It never starts a handshake.
  - Implicit TLS: `ImapConnection` connects at `:214`, and the first read `readInitialResponse()`
    (`:119`) triggers the handshake.
  - STARTTLS: the first I/O after the upgrade is `requestCapabilities()` (`:324`), which writes and then
    reads.
  - The `SSLException` reaches `open()`'s catch (`:132`): `executeSimpleCommand` and `sendCommand`
    rethrow `IOException`, at `:672-674` and `:701-703`. `handleSslException` (`:146-152`) then wraps it
    in `CertificateValidationException`, which is a `MessagingException` (`CertificateValidationException.java:10`).
  - The connection therefore fails closed, and the guarantee in `:196`'s third column holds. Only the
    place of origin is misstated. The contract's own lead-in (`:191-192`) already says "at TLS-handshake
    time".
  - This is a contract-wording defect (a design correction). It is not a code defect. Opening a bug now
    would ask for a code change that is not needed. The plan's choice, tests that follow the code with
    the mismatch recorded, is correct.
- The gap. CR-007's Proposed Change for `:196` (CR-007 lines 184-194) carries "surfaced from
  `createSocket(...)`" over unchanged, and says "The claim about the wrapping mechanism is carried over
  unchanged". CR-008 and CR-009 do not touch the phrase either. CR-009 changes only the possessive in
  `:196`'s first cell. If the combined revision is applied as approved, the inaccurate wording survives
  into v2. The only record of SC-1 would then be the story's Implementation Notes, which the CRs do not
  tell the revision author to read. Record 5 tells that author to read Record 5, not U-063.
- Change required (orchestrator, no re-planning):
  1. Put SC-1 where the revision author will see it, through the change-record process: an addendum to
     CR-007's Proposed Change for `:191-192` and `:196`, or a new low-severity design correction against
     CNTR-MODERNIZATION-001 linked to CR-007 to CR-009. Candidate wording: "surfaced at the TLS handshake
     of the socket that `createSocket(...)` returns (on its first read or write, or an explicit
     `startHandshake()`), as an `SSLException` whose direct cause is the `CertificateException`; k-9
     wraps it as `CertificateValidationException` (a `MessagingException`) in `ImapConnection.handleSslException`".
  2. In the story's Implementation Notes, record SC-1 as "to be carried by <record id>", not "belongs to
     the combined revision".
  3. Do not edit the contract now. CR-007's Timing forbids any edit to CNTR-MODERNIZATION-001 text before
     the revision.

**W2. The plan does not say which assertions are tied to the vendored trust manager, or what a failure means after the CR-009 move.**
- Plan:
  - "Test mechanics" and Quality Attribute Coverage, Maintainability ("Reflection targets are named in
    one place per class, so the move retargets them with the class").
  - I4, the global-state set-up (the `defaultTrustManager` field) and the assertion helper
    (`com.fsck.k9.mail.CertificateChainException` and the marker text).
- Change record: CR-009 "What moves" (lines 199-201) and Suggested Approach "Which trust manager"
  (line 268). Whether the moved trust manager is the vendored `TrustManagerFactory` or upstream's public
  one is a Part B design choice. CR-009 recommends the vendored one, but that is not decided.
- Evidence. Five interfaces the tests depend on are not in any contract. The plan acknowledges only
  "retarget":
  1. The private static non-final field `TrustManagerFactory.defaultTrustManager` (`:33`), replaced by
     reflection (I4, P8).
  2. `isIpOrBlankHostname`, package-private (`:216`), called by reflection (AC-2).
  3. `reorder` and the four lists, protected static (`:34-89`, `:124`), read by reflection (AC-11 to
     AC-14).
  4. The marker text `was not verified against the server certificate` (`TrustManagerFactory.java:147-148`),
     which is the AC-6 and AC-8 versus AC-9 and AC-10 discriminator.
  5. The exception class `com.fsck.k9.mail.CertificateChainException` (`CertificateChainException.java:1,11`).
     SC-2 already notes that CR-009 lists it under `ssl/`.

  Items 2 and 3 survive any move: reflection plus `setAccessible` works across packages. Items 1, 4 and 5
  survive only if the vendored trust manager is the one moved. If upstream's is chosen, the static-field
  substitution, the marker and the exception class all disappear by design. Group B and C set-up and
  discriminators must then be rewritten, not just retargeted.

  Retargeting is the wrong word for the assertions that carry the behaviour:
  - rejection is an `SSLException` whose direct cause is a `CertificateException`;
  - the controls complete;
  - the wrong-host rejection is distinguishable from the chain rejection.

  Those must keep holding after the move. The plan does not say so, and it does not say how to tell a
  seam failure from a regression.

  `getTrustManagerForTesting()` is not a dependency. Group E deliberately never calls it (I5; IC-10),
  which is correct.
- Change required (Developer, inside AC-22's Javadoc, no re-planning): in the Javadoc of
  `DefaultTrustedSocketFactoryHandshakeTest`, `DefaultTrustedSocketFactoryHardeningTest` and the Group A
  section comment, add a short "After the CR-009 move" paragraph. It should name the seams above and
  state that:
  - a `NoSuchFieldException`, `NoSuchMethodException` or `ClassNotFoundException` from a seam means the
    seam must be retargeted (Part B task), not that behaviour has regressed;
  - a failed behavioural assertion (a control that no longer completes, a rejection whose direct cause is
    not a `CertificateException`, or a wrong-host rejection that cannot be told apart from a chain
    rejection) is a regression and must not be "fixed" by editing the test;
  - if Part B adopts upstream's trust manager, the trust-anchor substitution and the AC-6, AC-8, AC-9 and
    AC-10 discriminator must be re-designed to give the same three behavioural assertions, and CR-009's
    T9 applies.

  The orchestrator should add the same one-line pointer to the story's Implementation Notes.

### Notes

**N1. Two harmless line-reference slips in the plan's Contracts Verification, row 1.** "`:191` adds 'at
TLS-handshake time'": that phrase is on contract `:192` (the sentence spans `:191-192`). "The first read in
`ImapConnection` (`:119`, `:324`)": at `:324` the first I/O after STARTTLS is the `CAPABILITY` write in
`requestCapabilities()` → `executeSimpleCommand`, followed by the read. Neither changes any assertion or
conclusion.

**N2. CNTR-MODERNIZATION-007, Rule 1, has nothing to say about test code, and the plan complies.** The
plan does not mention this contract. Rule 1 (`:345-349`) is enforced by grep over `service/` and
`State.java`, and its confinement sentence is about production imports. CR-005's AC-B3 grep is scoped to
`app/src/main/java`, and documented residuals already sit inside `mail.*`
(`CNTR-MODERNIZATION-007:457-459`). The new tests import `com.fsck.k9.mail.ssl.*` and
`com.fsck.k9.mail.CertificateChainException`. They live in the test packages `com.zegoggles.smssync.mail.ssl`
and `com.zegoggles.smssync.mail`, which are neither `service/` nor `State.java`, and they add nothing to
`app/src/main`. They do not affect the rule, and the contract does not extend it to tests. The existing
`DefaultTrustedSocketFactorySniTest` already follows the same import pattern. No rule is inferred here.

**N3. Validation Rule 2's `getAcceptedIssuers()` clause is not observed by any new test for the default
factory.** `SecureX509TrustManager.getAcceptedIssuers()` delegates to the static field
(`TrustManagerFactory.java:96-98`). For the pinned factory the clause is already asserted by the existing
tests (`PinnedCertificateSocketFactoryTest.java:124-146`). The empty-body clause is observed through
mutation for both producers:
- M-B2 (accept-all at `DefaultTrustedSocketFactory.java:155`) must be caught by AC-6, AC-8, AC-9 and
  AC-10;
- M-E5 (accept-all at `PinnedCertificateSocketFactory.java:77`) must be caught by AC-21.

This is outside the story's scope. It is recorded so the reviewer does not read Rule 2 as fully
characterised for producer 1.

**N4. Group E closes a real gap around the existing seam.** `getTrustManagerForTesting()`
(`PinnedCertificateSocketFactory.java:94-96`) returns a new `PinnedX509TrustManager`, not the one that
`createSocket` installs (`:77`). Until now nothing showed that the installed manager is the validating
one. AC-21, together with M-E5, does.

**N5. Error Handling row 2's "outside validity window" is not observed.** No test enrols an expired
certificate (`PinnedCertificateSocketFactory.java:149`). The story already records this as SC-6. It is
consistent with scope.

**N6. Throwaway probe P8: the value it writes into the field is unspecified.** P8's `tmf_field_rw`
writes "a probe value" into `defaultTrustManager` and then restores it (I0b, P8 row). The P8 and P9 blocks
run in the same JVM. The `p8_restore_ok` and `p9_restore_ok` keys and row D9r are designed to catch a
failed restore. Even so, the Developer should use the test-CA substitute (`trustManagerFor(TRUSTED_CA_CERT)`)
or the original instance as that value, never a Mockito mock or an accept-all manager. That keeps the
rule "no permissive trust manager, even in test code" true without exception. It is throwaway code that
is never committed, so this is a note, not a warning.

**N7. Story wording on AC-20 compared with the contract.** The story says adding host-name verification
to the pinned path is a "behaviour change that the contract owner must approve". The contract's
Versioning section (`:288-289`) classes "adding hostname-match on top of fingerprint" as a
backward-compatible tightening. The plan records this correctly (Contracts Verification row 4; Notes item
6). The test still fails if that change is made, so it is changed knowingly. Nothing more is needed.

## Question-by-question results

### 1. CNTR-MODERNIZATION-001, line by line where the tests touch it

**Producers and trust-manager duties (`:89-96`):**

| Contract text | Planned test that observes it | Assertion matches | Difference recorded |
|---------------|-------------------------------|-------------------|---------------------|
| `:90-91` `createSocket` returns an `SSLSocket` whose context uses a non-permissive `X509TrustManager` | AC-3 and AC-4 (`isInstanceOf(SSLSocket)`); AC-5 to AC-10 (the installed manager validates); AC-21 for the pinned factory | Contract and code agree | n/a |
| `:95` `SYSTEM_VALIDATED`: `checkServerTrusted()` throws `CertificateException` on any untrusted, expired or host-name-mismatched chain | AC-6 and AC-8 (wrong host), AC-9 (untrusted), AC-10 (expired). Each asserts a direct cause that is a `CertificateException` and a `CertificateChainException`; the host-name marker is present only for wrong host | Contract and code agree. In the code the host-name `SSLException` (`TrustManagerFactory.java:147`) is folded into `CertificateChainException` (`:92`), which is the contract's `CertificateException` | n/a |
| `:95` "against the system CA store" | Not observable. The anchor is a test CA (story E4; plan I4) | Test limit | Yes: Contracts Verification row 3, Javadoc (AC-22), risk R1 |
| `:96` `PINNED_CERTIFICATE`: SHA-256 fingerprint equality, then validity, `CertificateException` on mismatch | AC-21 (message `Pinned certificate mismatch`, `:141-145`); AC-20 (an enrolled certificate is accepted whatever the host name) | Contract and code agree. Validity is not observed (N5) | SC-6 |
| `:96` `getAcceptedIssuers()` returns the enrolled certificate, never `null` | Not a new test. Existing `PinnedCertificateSocketFactoryTest.java:124-146` | n/a | N3 |

**Invariant (`:165-187`):**

| Invariant | Observed by | Result |
|-----------|-------------|--------|
| 1. Fully resolved | Not touched: the tests construct the factories directly and drive no consumer | Out of scope, consistent |
| 2. Never trust-all (no empty `checkServerTrusted`, no `null` `getAcceptedIssuers`) | Behaviourally: M-B2 → AC-6, AC-8, AC-9, AC-10; M-E5 → AC-21; M-C2 (the silent key-store fallback at `TrustManagerFactory.java:91`) → AC-6, AC-8, AC-9, AC-10 | Matches the contract's words |
| 3. Consumer never re-decides | Not touched (no consumer runs) | Consistent |
| 4. Per-host scoping is the producer's job | AC-20 shows that the pinned manager ignores the requested host. Scoping comes from building one factory per host, which the contract assigns to the producer | Consistent. Recorded in row 4 and N7 |
| 5. Default is secure | Not touched (selection is `K9MailTransportTest`, CR-009 T7) | Consistent |

**Error Handling (`:189-202`), the "what fails closed, how, where it surfaces" table:**

| Row | What the plan's tests assert | Agreement |
|-----|------------------------------|-----------|
| `:196` (untrusted, expired or wrong-host chain under `SYSTEM_VALIDATED`) | Fails closed at `startHandshake()` as an `SSLException` whose direct cause is a `CertificateException`. This is exactly the precondition of `ImapConnection.handleSslException` (`:146-152`) | **Mechanism and outcome match. The place it surfaces does not:** the contract says `createSocket(...)`, the code says the handshake. The tests follow the code (SC-1). See W1 for routing |
| `:197` (pinned mismatch) | AC-21: an `SSLException` whose direct cause is a `CertificateException` with the pin message | Matches. Validity is not observed (N5) |
| `:198` (producer cannot resolve) | Not touched | Out of scope |
| `:200-202` (no silent fallback) | M-C2 makes the key-store fallback accept; it must break AC-6, AC-8, AC-9 and AC-10 | Observed |

SC-1 ruling: the plan's handling is right. It is not a defect that should become a bug now. The code
fails closed, and k-9 maps the failure to `CertificateValidationException`, a `MessagingException`, as
`:196` promises. The handshake timing is already conceded by the contract at `:191-192`. What is missing
is a record that carries the wording fix (W1).

**Validation Rules (`:204-219`):**

| Rule | Observed by | Result |
|------|-------------|--------|
| 1. Exactly two `TlsTrustPolicy` constants | Not touched. The tests add no policy, enum constant or `TrustedSocketFactory` implementation | Honoured |
| 2. No empty `checkServerTrusted`, no `null` `getAcceptedIssuers` | Empty body: observed through M-B2 and M-E5. `null` issuers: pinned only, by existing tests (N3) | Honoured; partly observed |
| 3. The consumer never re-decides | Not touched | Honoured |
| 4. Pinning is set only by enrolment | Not touched. Constructing `PinnedCertificateSocketFactory` directly in a unit test is not the production policy state, and the existing tests already do it | Honoured |
| 5. Canonical names | The story uses `SYSTEM_VALIDATED` and `PINNED_CERTIFICATE` verbatim | Honoured |

**Test seams the contract names:** `BackupImapStore.getTrustedSocketFactory()` (`:161-163`) is not used
by any new test. `BackupImapStoreTest` appears only as a child of the I7.4 ordered suites, unchanged. No
contract seam is altered.

### 2. Do the substitutes, helpers or fixtures break a contract rule while they run? Can production reach test code?

- **Trust-anchor substitute.** `trustManagerFor(TRUSTED_CA_CERT)` (I1) is the JDK's own validating
  manager (`TrustManagerFactory.getInstance("X509")` initialised with a key store that holds only the
  test CA).
  - It has a real `checkServerTrusted` and non-null issuers, so it narrows trust and never widens it.
  - The production `SecureX509TrustManager` still wraps it. Validation (`:76`), the host-name check
    (`:77`) and the key-store fallback (`:91`) all still run.
  - It is not a third trust path: it is the anchor set of the existing default path, swapped only inside
    the test JVM's sandbox and restored in `@After` with an identity check (IC-9).
- **Test-owned host-name verifier (D3a only).** It accepts only when a type-2 (`dNSName`) SAN entry of
  `getPeerCertificates()[0]` equals the requested host, compared case-insensitively. It has no wildcard,
  no common-name fallback and no IP entries, and an empty array or any exception gives `false`. It is
  stricter than a real verifier, so it cannot make a mismatch pass:
  - The `other-host` fixture carries only `imap.other.test`.
  - If the fixture were wrong, AC-6 and AC-8 would see a completed handshake, the D10d stop at I0b would
    fire, and the tests would fail rather than pass.
  - M-B3 (wrong host passed to `verifyHostname`) and M-B4 (an empty peer chain) must break the controls
    AC-5 and AC-7. That proves the verifier really uses the host and the chain.
  - Probe key `hs_implicit_right_no_verifier_swap` = `REJECTED … marker=true` (D10e') proves that the swap
    is needed, so the controls cannot pass by accident.
- **Server-side helpers.** `serverContext` uses `init(kms, null, null)`. It requests no client
  authentication and adds no client trust manager. The probe's `SSLSession` stub (P3) is not a trust
  manager. P8's probe value is unspecified (N6).
- **Production reaching test code.** It cannot. Every new class is under `app/src/test`. IC-2 shows no
  change under `app/src/main`, `k9mail-vendored`, the build files or `gradle/`. The reflective swap
  touches the sandbox copy of the static, and the verifier swap touches the test JVM's
  `HttpsURLConnection`. Neither exists in the APK.

### 3. Mutation protocol

- **Mutations that break the contract while applied:** M-B2 and M-E5 install accept-all managers, the
  shape `:116-120` and Rule 2 forbid. M-C2 enables a silent fallback. M-E3 adds a host check (a tightening
  under `:288-289`). This is intended, and each is caught by a named test (I6b, I6c).
- **Controls that stop a mutation being left in place or taken for a real change** (I6 protocol, RECOVER-I6,
  Notes item 8, I7):
  1. A write-ahead log entry before each edit.
  2. One file and one edit at a time, made with the Edit tool.
  3. No gate while a file is mutated.
  4. Restore with `git checkout -- <file>`, then require `git hash-object <file>` to equal the base blob
     `git rev-parse $BASE:<file>`. The base is the commit, not the index, so a staged mutation would also
     be caught.
  5. The product-clean check `git diff --quiet $BASE -- $PROD` after each restore.
  6. An `I6-clean.txt` record.
  7. A recovery turn that has priority over everything else, and an orchestrator hold.
  8. The I7 IC-2 diff against the base.
  9. A `clean` rebuild before `assembleRelease` at I7, so no compiled mutant class can reach an APK.
- **Assessment.** Byte identity with the base commit is a stronger proof than any grep for the mutant
  text. From a contract standpoint the proof of restoration is sufficient.

### 4. Interfaces outside any contract

These are the private static field `defaultTrustManager`, the reflected `reorder` and its lists, the
package-private `isIpOrBlankHostname`, the exception class and the marker text. The story ("Test
mechanics") and the plan (Maintainability) treat them as characterisations of today's vendored code that
Part B retargets. `getTrustManagerForTesting()` is not used, which is correct (IC-10). What is missing is
what a failure means after the move, and the case where the move breaks the seam by design (upstream's
trust manager). See W2.

### 5. CNTR-MODERNIZATION-007 Rule 1 and test code

The contract says nothing about tests. The enforcement greps cover `service/` and `State.java`, and
CR-005's AC-B3 covers `app/src/main/java`. The new test packages and imports do not affect the rule. The
plan does not mention this contract, which is acceptable (N2).

### 6. Consistency with CR-005 to CR-009 and with Records 5 and 6

- **Blocklist on the default path only (Record 6).**
  - No planned assertion says that no blocklist exists, and none checks the order of checks in a way a
    blocklist would contradict.
  - The test certificates are not on the blocklist, so a blocklist placed before system validation
    cannot change any outcome.
  - Record 6 says a listed chain should be "a certificate error that fails closed, like an untrusted
    chain". Group C's assertion pattern is consistent with that.
- **SNI to be stated in the next revision (Record 6).**
  - Group A records exactly the behaviour Record 6 describes: SNI for DNS names; none for IPv4 or IPv6
    literals or blank hosts; no exception when the platform rejects the name.
  - AC-18 and AC-19 characterise the pinned path's "neither SNI nor hardening", which Record 6 says is
    "not a decision" and is to be characterised by U-063.
  - Nothing here anticipates a decision.
- **The default factory becoming app-owned (CR-009).** The tests are written against today's class
  names. They do not choose a package for the moved code (the placement in CR-005 and CR-009 is left
  open).
- **Assertions that would have to be deleted when CR-007 to CR-009 are applied.** None:
  - CR-007 changes only the exception class in the `throws` clause, and no test names it.
  - CR-008 changes only the store slot, which no test touches.
  - CR-009 renames the producer. AC-3, AC-4, AC-15 to AC-17 and AC-5 to AC-10 move with the class.
  - The assertions tied to the vendored trust manager would have to be rewritten, not deleted, if Part B
    chooses upstream's trust manager. The plan does not say so (W2).
  - AC-16's second test (TLS 1.0 and 1.1 stay enabled) can change only through DP-2 or its bug, not
    through these CRs. The plan says so (DP-2).

### 7. Is every statement in the plan's "Contracts Verification" section true?

| Statement (plan, Contracts Verification) | Checked against | True? |
|------------------------------------------|-----------------|-------|
| The plan reads CNTR-001 (approved, v1) and changes nothing in it | Contract frontmatter `status: approved`; `:281` "Current version: v1"; the plan's never-touched list | Yes |
| `integration_contracts: []`; no new boundary | Story frontmatter line 12; no new `TrustedSocketFactory` implementation is planned | Yes |
| The tests contradict no guarantee of the contract | Question 1 above | Yes |
| Row 1: `:191` and `:196` place the exception at `createSocket`; the code surfaces it at the handshake | `DefaultTrustedSocketFactory.java:152-180`; `ImapConnection.java:119`, `:132-133`, `:146-152`, `:324` | Yes, with the line slips in N1. The "belongs to the combined revision" routing is not yet backed by a CR (W1) |
| Row 2: wrapped as `MessagingException` through `handleSslException` → `CertificateValidationException` | `ImapConnection.java:146-152`; `CertificateValidationException.java:10` | Yes |
| Row 3: `:95` system CA store versus a test CA | `:95`; plan I4 | Yes |
| Row 4: `:96`, Invariant 4 `:181-184`, and `:288-289` class host-name matching as a backward-compatible tightening | Contract `:96`, `:181-184`, `:288-289`; `PinnedCertificateSocketFactory.java:67-72`, `:126-150` | Yes |
| Row 5: SNI and cipher or protocol hardening are not stated in the contract | Case-insensitive search of the contract for `SNI`, `cipher`, `protocol`, `server name`, `IP literal`, `host name`: the only matches are `:95`, `:196` and `:289`, all about host-name *verification* | Yes |
| Row 6: the contract says nothing about protocol versions | Same search | Yes |
| Lead-in: Record 6 adds the SNI statement | Analysis, Record 6, third row ("Stated in the contract's next revision (with CR-007 to CR-009)") | Yes |

## Phase Completion Report
---
story_id: "U-063"
phase: "plan-gate/contract-verification"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-020/U-063/contract-verification.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
