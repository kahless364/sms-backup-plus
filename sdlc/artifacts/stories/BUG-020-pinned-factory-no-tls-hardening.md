---
type: bug
status: ready
artifact_type: bug
severity: low
priority: medium
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items:
  - U-063
  - BUG-019
  - U-065
platforms: []
tags: []
id: BUG-020
title: Pinned-certificate socket factory applies no protocol, cipher or server-name handling of its own
domain: build
origin: code-review
updated_at: '2026-11-21T00:41:53.995Z'
---

# BUG-020: Pinned-certificate socket factory applies no protocol, cipher or server-name handling of its own

## Description

The app has two TLS socket factories. The default one (`k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`) is used for every connection unless the owner has enrolled a certificate for the host. The pinned one (`app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java`) is used only for a host the owner has enrolled. It is chosen at `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java:145`, the only place in production code that constructs it.

The two factories treat the new socket differently:

| | Default factory | Pinned factory |
|---|---|---|
| Builds the socket | `createSocket` `:152-180` | `createSocket` `:75-86` |
| Enabled protocols | Replaced by its own list (`hardenSocket`, `:176`, `:186-188`) | Not touched. The platform provider's defaults apply |
| Enabled cipher suites | Replaced by its own list, which removes its blocked suites (`:176`, `:183-185`, blocked list `:64-81`) | Not touched. The provider's defaults apply |
| Server name (SNI) | Set by the factory for a DNS host name, skipped for IP literals and blank hosts (`:177`, `:191-254`) | Not set. No call to any server-name method anywhere in the file |

What "no hardening" means in practice, from the evidence:

- **Protocols and cipher suites.** The pinned socket has whatever the provider enables by default. Under the default test provider (Conscrypt) that is TLS 1.0, 1.1, 1.2 and 1.3 (`sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/I0-probe.txt:27`) and the provider's default suite list (`I0-probe.txt:30`). U-063's AC-18 tests show the pinned socket's enabled protocols and suites equal those of a plain default socket, in order, for both socket shapes (`I0-probe.txt:54-55`). The default factory would, on the same provider, put its own order on the suites (`I0-probe.txt:47`), remove SSLv3 and remove its blocked suites.
- **What that difference is worth in the test providers.** Less than the table suggests. None of the default factory's blocked suites is in either test provider's default list (`I0-probe.txt:33` for Conscrypt; for the JDK's own provider I read the default list at `I0c-diag.txt:14` against `DefaultTrustedSocketFactory.java:64-81` and found none). SSLv3 is not supported under Conscrypt (`I0-probe.txt:45`) and is supported but off by default under the JDK provider (`I0c-diag.txt:7-8`). So in these tests the blocked-suite and SSLv3 removal changes nothing. The two real differences are the suite order and, below, the server name.
- **The pinned socket is not weaker than the default factory's on protocols in these tests.** Under the JDK provider a plain context enables only TLS 1.3 and 1.2 (`I0c-diag.txt:8`) while the default factory's socket enables TLS 1.2, 1.1, 1.0, 1.3 and SSLv2Hello (`I0c-diag.txt:15`). The pinned socket under that provider was not measured. Its code makes no change, so it should get the provider's defaults; that is an inference from the code, not an observation. The security review of U-063 makes the same point (`sdlc/artifacts/build/sprints/sprint-020/U-063/review-security.md:44`). The issue is therefore that the pinned path has no protection of its own and depends entirely on the platform, not that it is looser than the default path today.
- **Server name.** The factory sets none. The test `pinnedFactory_createSocket_implicit_setsNoServerNames_jdkProvider` asserts the pinned implicit socket reports no server names, and its control, `createSocket_dnsHost_setsServerNameOnRealSocket` in the same class, shows the default factory's socket does report one (`app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java:192`, `:311-324`).
- **"Sets no server name" is not "sends none".** A capture of the first handshake message under the default test provider found that the provider puts a name in by itself. The pinned factory's capture matches the default factory's in every case captured: implicit socket, name taken from the connect address (`I0c-diag.txt:271` against `:262`); layered socket, the host given to `createSocket` (`:273` against `:264`); layered socket with an IPv6 literal, no name (`:275` against `:266`); and the same three when the address's own name differs from the host given (`:313`, `:316`, `:325` against `:304`, `:307`, `:319`). The same provider reports no server names through the socket's parameters whatever the factory sets (`I0c-diag.txt:89`, `:91`, `:110`, `:112`), which is why the check on the pinned factory sits in the JDK-provider class (`sdlc/artifacts/build/sprints/sprint-020/U-063-tls-socket-factory-characterisation-tests.md:315`, `:326`). The test server in that capture could not finish its handshake (a harness restriction, `I0c-diag.txt:256`), so only the first message was captured.

Limits, stated plainly:

- **The pin itself is enforced.** The trust manager compares the SHA-256 fingerprint of the presented leaf with the enrolled certificate and throws on any difference (`PinnedCertificateSocketFactory.java:141-145`), then checks the enrolled certificate's validity window (`:149`). U-063 AC-20 and AC-21 prove it with real handshakes, and mutations M-E4 (comparison inverted) and M-E5 (accept-all trust manager) make them fail (`sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/mutation-log.md:52-53`). This bug is about protocol and cipher strength on a connection to a server the owner chose to pin. It is not about accepting a wrong certificate.
- **What a device's platform enables by default was not measured.** Tests run under Robolectric at a simulated API level 29 on a desktop JDK (`I0-probe.txt:56`). Neither test provider is a device's. This matters more here than for the default factory, because the pinned path adds nothing on top of the platform. The security review lists the open measurement (`review-security.md:174-185`).
- **No handshake below TLS 1.3 was run.** Every test handshake negotiated TLS 1.3 (`I0-probe.txt:12`). "Enabled" is not "negotiable".
- **Whether the owner has any host enrolled today was not checked.** The path is opt-in (see Technical Context).

**Whether the difference between the two factories is intended is not recorded anywhere.** What I searched and found:

- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-002-transport-security-hardening-design.md`, the design that introduced the pinned factory: Decision 2 (`:146-186`) describes the pinned factory only as validating the enrolled certificate, by fingerprint equality plus validity window (`:168`, `:174-179`). Decision 3 (`:188-191`) covers enrolment as opt-in. A search of the whole `sdlc/artifacts/design` folder for cipher, protocol version, TLS version, SNI and server name found nothing about either factory (the only "cipher" hits are "ciphertext" in the credential-store design and contract).
- The class's own Javadoc (`PinnedCertificateSocketFactory.java:35-54`) lists the two checks and says nothing about hardening.
- U-063's test Javadoc calls it "present behaviour, not an approved design; whether it should change is a separate matter and is not decided here" (`app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryCreateSocketTest.java:52-55`).
- The feasibility analysis reports the difference as "Also reported, not a decision" (`sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md:2212`, also `:707`). Do not confuse it with the decision just above it (`:2208`): that one is about the certificate blocklist of compromised-authority key hashes, which applies to the default path only. It says nothing about cipher, protocol or server-name handling.
- The contract `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md` states neither hardening nor server-name behaviour for either producer. A case-insensitive search for `SNI`, `cipher`, `protocol`, `server name` and `harden` matches only lines about host-name verification (`:95`, `:196`, `:289`). The pinned producer row (`:96`) and Invariant 2 (`:173-176`) describe trust behaviour only. The contract-verification report for U-063 reached the same finding.

Why it is filed: the security review of U-063 raised it as finding W-3 (Low) and said it should be its own bug so that one decision can cover both factories (`review-security.md:42-45`, `:97`, `:191`). QA carried it forward (`sdlc/artifacts/build/sprints/sprint-020/U-063/qa-results.md:277`, `:285-286`). The product owner approved filing it on 2026-11-20. BUG-019 asks for the same thing from its side (`sdlc/artifacts/stories/BUG-019-factory-enables-tls10-and-tls11.md:191`).

## Steps to Reproduce

All three reproductions are test runs. The tests belong to U-063 (sprint-020), committed on 2026-11-20 (commit `5a5321d4`). Run Gradle in a shell without `MSYS_NO_PATHCONV` set.

1. Protocols and suites, both socket shapes (default test provider, Conscrypt):
   `./gradlew :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest.createSocket_implicit_leavesProviderDefaultsUntouched" --tests "com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest.createSocket_layered_leavesProviderDefaultsUntouched"`
   Each builds a pinned socket and asserts its enabled suites and protocols equal, in order, those of an unhardened socket from a fresh default TLS context (`PinnedCertificateSocketFactoryCreateSocketTest.java:123-134` and `:140-154`). Both pass. They show the pinned factory changes nothing. Under this provider the unchanged protocols include TLS 1.0 and 1.1.
2. Server name (JDK provider, a separate class with its own sandbox):
   `./gradlew :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryJdkProviderTest.pinnedFactory_createSocket_implicit_setsNoServerNames_jdkProvider"`
   It builds a pinned implicit socket for a DNS host and asserts `getSSLParameters().getServerNames()` is null or empty (`DefaultTrustedSocketFactoryJdkProviderTest.java:311-324`). It passes. For contrast, run the control in the same class, `createSocket_dnsHost_setsServerNameOnRealSocket` (`:192`), which asserts the default factory's socket reports one name.
3. Observe: **all three pinned tests pass today.** They assert present behaviour (a characterisation), not a desired state. Passing is not evidence the behaviour is acceptable.
4. To run the three together: `./gradlew :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest" --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryJdkProviderTest"` (this runs every test in both classes, not only the three).

## Expected Behavior

Conditional on a decision (AC-1). One of:

- **(a) Same handling.** The pinned factory gives its sockets the same protocol, cipher-suite and server-name handling as the default factory, so that whatever protocol floor is chosen under BUG-019 applies to both factories.
- **(b) Intended difference.** The difference is recorded as intended, with its reason, and the tests and contract wording say so instead of calling it undecided.

Which of the two is right is the product owner's decision, with the Security Reviewer's judgement of safety. This bug does not choose.

## Actual Behavior

The pinned factory returns the provider's socket untouched (`PinnedCertificateSocketFactory.java:80-85`): the provider's default protocols, the provider's default suites, and no server name set by the factory. The provider may add a server name itself, as it does under the default test provider. No document records this as intended or as a gap to close.

## Evidence

All paths are workspace-relative. Evidence files are under `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/`.

| Evidence | What it shows |
|----------|---------------|
| `PinnedCertificateSocketFactory.java:77-85` | `createSocket`: a "TLS" context with the pinning trust manager, then the provider's socket returned unchanged (direct `:82`, layered `:84`) |
| `PinnedCertificateSocketFactory.java:67-72`, `:75` | Constructor does not store the host; `createSocket` ignores the client-certificate alias |
| `PinnedCertificateSocketFactory.java:141-145`, `:149` | Pin comparison and validity-window check (enforced, not in question) |
| `DefaultTrustedSocketFactory.java:176-177`, `:182-189`, `:191-254` | Contrast: hardening and server-name calls on the default factory's socket |
| `PinnedCertificateSocketFactoryCreateSocketTest.java:123-134`, `:140-154` | AC-18 tests: pinned socket's suites and protocols equal a plain default socket's |
| `DefaultTrustedSocketFactoryJdkProviderTest.java:311-324`, control `:192` | AC-19 test: pinned implicit socket reports no server names; default factory's does |
| `I0-probe.txt:27`, `:30` | Conscrypt default protocols include TLS 1.0 and 1.1; default suite list |
| `I0-probe.txt:54-55` | `pinned_implicit_equals_default=true`, `pinned_layered_equals_default=true` |
| `I0-probe.txt:33`, `:45`, `:47` | No blocked suite in Conscrypt's defaults; SSLv3 unsupported; default factory's suite order differs |
| `I0-probe.txt:12`, `:56` | Every handshake negotiated TLS 1.3; simulated API level 29 |
| `I0c-diag.txt:7-8`, `:14`, `:15` | JDK provider: supported and default protocols; default suites; default factory's protocol list |
| `I0c-diag.txt:262`, `:264`, `:266` against `:271`, `:273`, `:275` | First handshake message: default factory and pinned factory send the same server names (implicit, layered DNS host, layered IPv6) |
| `I0c-diag.txt:304`, `:307`, `:319` against `:313`, `:316`, `:325` | Same comparison when the address's own name differs from the host given |
| `I0c-diag.txt:89`, `:91`, `:110`, `:112` | Under Conscrypt the socket reports no server names before or after the handshake, for both factories |
| `mutation-log.md:48-53` | M-E1 (protocol call added to pinned factory) detected by AC-18; M-E2-jdk (server name set) detected by AC-19; M-E3 to M-E5 detect pin changes |
| `qa-results.md:173`, `:276` | The cipher-call variant of M-E1 was not run separately |

### Measurements on two emulators (2026-11-20)

Added by the orchestrator. The platform's protocol lists and bare handshakes to Gmail's IMAP endpoint were measured on an Android 16 and an Android 11 emulator on 2026-11-20; the table and its limits are in `sdlc/artifacts/stories/BUG-019-factory-enables-tls10-and-tls11.md`, section "Measurements on two emulators (2026-11-20)". For this bug the relevant rows are "Enabled by default": a pinned socket gets exactly that list, TLS 1.2 and 1.3 on Android 16, and TLS 1.0 to 1.3 on Android 11. The product owner said on 2026-11-20, when asked whether any host is enrolled for a pinned certificate today: "I don't think so".

## Acceptance Criteria
**Decision recorded (AC-1, cipher suites and server name), 2026-11-20:** the product owner decided that the pinned factory is handled the same as the default factory for cipher suites and for the server name ("Same as default"). With the protocol part above, AC-1 is now decided in full: the decision is "same handling", so AC-2 and AC-3 apply and AC-4 does not. The record is Record 9 in the appendix "Product owner decision" of `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`. Added by the orchestrator.

**Decision recorded (AC-1, protocol part), 2026-11-20:** the product owner decided that neither factory enables a protocol version older than TLS 1.2 ("Yes drop anything older than tls 1.2"). The record is Record 8 in the appendix "Product owner decision" of `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`. Still to be decided for this bug: cipher handling and server-name handling on the pinned factory. Added by the orchestrator.


AC-1 is always required. AC-2 and AC-3 apply if the decision is to give the pinned factory the same handling as the default factory. AC-4 applies if the decision is to record the difference as intended. AC-5 to AC-7 apply either way.

- [ ] AC-1: A dated written record exists, made before any fix merges, that names who decided (the product owner, with the Security Reviewer's judgement). For each of protocols, cipher suites and server name it states whether the pinned factory matches the default factory or differs, with the reason wherever it differs. It is made together with the BUG-019 AC-5 decision (one record, or two that cite each other), so that any protocol floor chosen there is stated as applying to the pinned factory or as not applying with a reason. It is found by searching the workspace for "BUG-020".
- [ ] AC-2: (decision: same handling) The defect no longer reproduces following the steps above. After the change, a socket from the pinned factory, in both shapes (direct and layered over an existing plain connection), has the decided protocols and cipher suites enabled, asserted under the Conscrypt test provider. The decided server-name behaviour is asserted under the JDK test provider, because the observable works only there.
- [ ] AC-3: (decision: same handling) The three characterisation tests (`createSocket_implicit_leavesProviderDefaultsUntouched`, `createSocket_layered_leavesProviderDefaultsUntouched`, `pinnedFactory_createSocket_implicit_setsNoServerNames_jdkProvider`) are updated in the same change to assert the new behaviour. They are not deleted and not marked `@Ignore`. The updated assertions are not satisfied by the provider's own defaults. For each of the three aspects, protocols, cipher suites and server name, removing the handling makes at least one of the tests fail, shown by a recorded mutation. The cipher-suite mutation is run, because the earlier evidence did not run it separately.
- [ ] AC-4: (decision: intended difference) No hardening code is added. The three tests still pass unchanged in behaviour. Their Javadoc no longer says the behaviour is undecided: the passage at `PinnedCertificateSocketFactoryCreateSocketTest.java:52-55` and the AC-19 Javadoc at `DefaultTrustedSocketFactoryJdkProviderTest.java:300-310` cite the record from AC-1.
- [ ] AC-5: The pinned producer's protocol, cipher-suite and server-name behaviour, as decided, is stated in the contract revision that the feasibility analysis already plans for the SNI statement (`thunderbird-mail-libraries-feasibility-analysis.md:2210`), or the AC-1 record says why it is not. Search the contract for the pinned producer row (`CNTR-MODERNIZATION-001-tls-socket-factory.md:96`) and confirm.
- [ ] AC-6: Existing functionality is not broken. `handshake_enrolledCertificateForDifferentHostName_isAccepted` and `handshake_certificateOtherThanEnrolled_isRejected` (U-063 AC-20 and AC-21, `PinnedCertificateSocketFactoryCreateSocketTest.java:161`, `:181`) and all eight tests of `PinnedCertificateSocketFactoryTest` pass unchanged. If `createSocket` was edited, the mutations M-E4 and M-E5 are run again and still make AC-21 fail.
- [ ] AC-7: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` exits 0 with 0 failures, and the skipped count is no higher than before the change (2 at the close of U-063).

### Integration Criteria (if fix modifies component boundaries)
- [ ] IC-1: The constructor and `createSocket` signatures of `PinnedCertificateSocketFactory` are unchanged, so the call site `K9MailTransport.java:145` and the test call sites compile without edits. If a signature changes, every caller is updated in the same change and listed in the change record.

## Impact Analysis

Searches run over the layers configured for change records in this project. Search terms: `PinnedCertificateSocketFactory`, `SNI`, `server name`, `cipher`, `protocol`, `TLSv1`, `TLS 1.`, `harden`, `setEnabledProtocols`, `setEnabledCipherSuites`, `setServerNames`, `BUG-020`.

| # | Layer | Affected? | Files and lines | Action needed |
|---|-------|-----------|-----------------|---------------|
| 1 | Requirements (`sdlc/artifacts/requirements`) | No change required | `modernization/REQ-MODERNIZATION-019-...md:50` (lists the pinned factory), `:73`, `:84`, `:95`, `:109` (migration must preserve TLS, pinning and SNI protections; AC-B8 tests a pinned accept and a pinned reject). No requirement states a protocol, cipher or server-name rule for the pinned path | None. Note that "SNI protection preserved" cannot refer to the pinned path, which has none. The gap is the same one BUG-019 records: no requirement sets a protocol policy |
| 2 | Design and contracts (`sdlc/artifacts/design`) | Possibly | `contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md:96` (pinned row: trust behaviour only), `:173-176` (Invariant 2), `:215-216` (rule 4: pinned set only by enrolment), `:288-289` (lists "tightening `PinnedCertificateSocketFactory` validation" as backward-compatible; whether protocol, cipher or server-name handling counts as that is not stated). `design/modernization/DES-MODERNIZATION-002-...md:146-186` (Decision 2), `:375` (file table). `contracts/CNTR-MODERNIZATION-002-pinned-cert-enrollment.md`: enrolment only, not socket settings | AC-5. Whether the change is backward-compatible under `:288-289` is for the Solution Architect |
| 3 | Stories, sprints, change records (`sdlc/artifacts/build`, `sdlc/artifacts/stories`) | Yes (references and one decision) | `stories/BUG-019-...md:112` (AC-5), `:191` (pinned paragraph), `:210` ("do not change it under this bug"); U-063 story `:315`, `:326`, `:337`; `review-security.md:42-45`, `:97`, `:191`; `qa-results.md:277`, `:285-286`; `sprint-020/retro.md:143`; `change-records/CR-009-...md:86`, `:199`, `:213` (T5, hardening test row, written for the default factory), `:266` (the pinned factory must also move to `mail/transport/`), `:274`; feasibility analysis `:707`, `:2194`, `:2208`, `:2212`. Untracked draft scaffolds `stories/U-064-...md` and `change-records/CR-012-...md` have empty titles; their content was not assessed | AC-1 decision with BUG-019. CR-009 T5 wording needs to say whether it covers the pinned factory if (a) is chosen |
| 4 | Application code (`app/src/main`) | Yes (root) | `mail/PinnedCertificateSocketFactory.java:74-86`. Construction: `mail/transport/K9MailTransport.java:145` only. Mentions only: `mail/BackupImapStore.java:65`, `mail/TlsTrustPolicy.java:27`, `:39`, `mail/transport/MailTransportConfig.java:39`, `K9MailTransport.java:79`, `:104`; `res/xml/network_security_config.xml:8` (comment) | The change itself, if (a). Nothing if (b) |
| 5 | Vendored library (`k9mail-vendored`) | Contrast only | `ssl/DefaultTrustedSocketFactory.java:152-254`. The only `setEnabledCipherSuites`, `setEnabledProtocols` and `setServerNames` calls in the vendored tree are at `:184`, `:187`, `:246` (the reflective `setHostname` fallbacks at `:201` and `:258` are server-name calls too, for pre-API-24 platforms) | None under this bug. BUG-019 may change `:83-89`, `:107`, `:116-117`, `:124-148` |
| 6 | Tests (`app/src/test`) | Yes | `mail/PinnedCertificateSocketFactoryCreateSocketTest.java:122-154` (two AC-18 tests) and Javadoc `:45-55`, `:96-98`; `mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java:300-324` (AC-19), control `:192`. Not affected: `mail/PinnedCertificateSocketFactoryTest.java` (trust manager through its seam, never `createSocket`), `mail/transport/K9MailTransportTest.java:80-89` and `mail/BackupImapStoreTest.java:95-104` (type checks only) | Update with the fix (AC-3) or reword (AC-4) |
| 7 | Documentation (`README.md`, `sdlc/artifacts/engagement`) | No | 0 matches in `README.md` for the pinned factory, TLS or SNI; 0 matches for `pinned`, `TLS`, `SNI` in `sdlc/artifacts/engagement` | None |

**Cascade analysis** (four layers touched: application code, tests, design and contracts, build records).

- Root layer: application code. The requirements and the contract are silent, so no upstream statement is wrong and none is violated. The missing piece is a decision, not a correction.
- Propagation: pinned factory to its three characterisation tests and their Javadoc (yes); to the contract revision that states SNI behaviour (yes, if the pinned producer is to be covered); to CR-009 T5 wording and the BUG-019 decision record (yes, wording and cross-reference). To requirements and documentation: none.
- Test-seam rule: the pinned class's Javadoc says a failed behavioural assertion after the CR-009 move is a regression and the test is never edited to pass (`PinnedCertificateSocketFactoryCreateSocketTest.java:96-98`). A deliberate change must therefore be decided and recorded first, or the retargeting rule will read it as a regression.
- Fix order: (1) decision record with BUG-019 (AC-1); (2) factory; (3) the three tests in the same change; (4) contract revision and CR-009 T5 wording.
- Change-record question: a change touching these layers would normally call for a change record. CR-009 is the record that moves this class. Whether this fix needs its own is not decided here.

**Interaction with the migration (stated as options, not chosen).** CR-009 makes the default factory app-owned code; the pinned factory already is, and CR-009 puts both under `mail/transport/` (`CR-009-...md:266`). After that move both factories are app code, so a hardening step that both call becomes possible in one place. Whether to share one, copy, or leave the pinned factory alone is a design choice for the Solution Architect. The timing options are: change the pinned factory before the move (no vendored file is involved, unlike BUG-019); fold it into the move; or after the move. Each has the cost BUG-019 describes for its own fix: folding a behaviour change into a move that CR-009 and U-063 treat as behaviour-preserving removes the characterisation tests' power to prove the move changed nothing.

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->

Advisory only — refined during story planning.

**Architecture.** One contract covers two producers, and they treat the socket differently. The default factory replaces the enabled protocols and suites and sets the server name (`DefaultTrustedSocketFactory.java:176-177`). The pinned factory returns the provider's socket as it is (`PinnedCertificateSocketFactory.java:80-85`). The contract governs trust only (`CNTR-MODERNIZATION-001-tls-socket-factory.md:93-96`, Invariant 2 `:173-176`), so neither treatment breaks it. The gap is that nobody has decided whether the difference is intended. After CR-009 both classes are app-owned and both move under `mail/transport/` (CR-009 `:266`). Only then can one shared hardening step serve both factories without editing vendored code or opening up its private `hardenSocket` (`:182`). Before the move, "share" can only mean "copy".

**What the pin protects.** It protects server identity: an attacker needs the enrolled certificate's private key, whatever protocol is negotiated (`:141-145`), and the enrolled certificate's validity dates are checked (`:149`). It does not choose the protocol version or the cipher used with the genuine server, so confidentiality on this path depends entirely on the platform's defaults. It also sets no server name. That is a functional risk, not a security one: a server that routes by name may present another certificate, and the pin then fails closed.

**The trap in "add the default factory's hardening".** The default factory builds its protocol list from the provider's *supported* protocols (`DefaultTrustedSocketFactory.java:107`, `:116-117`). It builds its cipher-suite list from the *enabled* suites (`:100`, `:113-114`). So its suite step can only reorder or remove, but its protocol step can switch on a version the platform had turned off. The analyst's JDK-provider evidence shows exactly that: TLS 1.0, TLS 1.1 and SSLv2Hello are added. Copying that step onto the pinned socket while BUG-019 is undecided would widen protocols on what is probably the narrower path today. The analyst also found that none of the blocked suites is in either test provider's defaults, so in practice the cipher step only changes the order. If a shared step is chosen, it should narrow the set the platform enables (a floor plus a removal list). It should never rebuild that set from what the platform supports.

**Layer cascade.** Three characterisation tests pin today's behaviour: `PinnedCertificateSocketFactoryCreateSocketTest.java:124` and `:141`, and `DefaultTrustedSocketFactoryJdkProviderTest.java:312`. Their retargeting rule counts any changed assertion as a regression (`PinnedCertificateSocketFactoryCreateSocketTest.java:96-98`). So the decision record must exist before those tests change, as the analyst says. In the contract, the pinned row (`:96`) states trust behaviour only. The SNI statement planned for the next revision (Record 6) must say what the pinned factory does, so it waits for this decision. The Impact Analysis leaves one question to me. My answer: a protocol, cipher or server-name change in either producer is backward-compatible in the contract's own terms, because no type, name, invariant or consumer duty changes (`:279-291`). The same revision should say so in Versioning, which today lists only "tightening ... validation". CR-009's T5 wording follows the decision.

**One decision with BUG-019, separate work.** Make one dated record together with BUG-019 AC-5, as AC-1 asks: the protocol floor is the same question seen from both factories. Keep the fixes separate. BUG-019 may land in the vendored file before the move. BUG-020 is one small story under either option:
- Option (b) changes only Javadoc and contract text, and can follow the decision at any time.
- Option (a) I would place after the CR-009 move, which must itself change no behaviour, and build on the shared app-owned step. It can be its own story, or one story with BUG-019 if BUG-019 is also deferred. Putting it inside the move would stop the characterisation tests from proving that the move changed nothing.
- U-064 goes first under either option. It mutates `PinnedCertificateSocketFactory.java:149` against a recorded file hash (U-064 IC-1, IC-7), so any earlier edit to this file forces it to take a new baseline.
- The pinned `createSocket` already receives the host on both connection paths (`ImapConnection.java:209`, `:315`). Server-name handling therefore needs no constructor change, and IC-1 holds.

**Constraints and risks for planning.** The product owner decides, and the decision is recorded before any fix. Whether the result is safe is the Security Reviewer's judgement. Measure first, on one of the owner's devices: the enabled and supported protocols and suites for both factories, and whether any host is enrolled at all. If no host is enrolled, option (b), or a deferred (a), costs nothing now. Nothing in the evidence shows an exploit path. Every test handshake negotiated TLS 1.3, and a version being enabled does not mean it can be negotiated. Severity low: agreed. Three refinements to the criteria:
- AC-2 should also assert, under the JDK provider, that the pinned socket gains no protocol the provider had disabled (the widening above).
- In AC-3, no blocked suite is present in either provider, so the cipher assertion can avoid being "satisfied by the provider's own defaults" only through suite order. AC-3 should say so.
- AC-1 should also record whether SSLv2Hello stays, as the BUG-019 review asked.

**Order with the related items.** CR-012 erratum (independent) → U-064 → joint decision with BUG-019 → SNI change record → BUG-019 fix (before or after the move) → CR-009 move (no behaviour change) → BUG-020 fix → combined contract revision. Three orders conflict:
- This fix before the BUG-019 decision risks the widening described above.
- This fix inside the move removes the characterisation tests' proof.
- An SNI record that says "the pinned factory sets no server name" before this decision would make option (a) a contract contradiction.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` (`:74-86`) | Returns the provider's socket with its defaults and no server name set | Per the AC-1 decision; none if the difference is recorded as intended |
| `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryCreateSocketTest.java` (`:45-55`, `:122-154`) | Pins "protocols and suites equal the provider's defaults" for both shapes | Updated in the same change (AC-3), or Javadoc reworded (AC-4) |
| `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java` (`:300-324`) | Pins "pinned implicit socket reports no server names" | Updated in the same change (AC-3), or Javadoc reworded (AC-4) |
| `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md` (`:96`) | Silent on pinned protocol, cipher and server-name handling | Statement added in the planned revision, or reason recorded (AC-5). Owned by the Solution Architect |

## Existing Behavior to Preserve

- The pin comparison: SHA-256 of the presented leaf against the enrolled certificate, exact equality, with a `CertificateException` on any difference (`PinnedCertificateSocketFactory.java:134-145`).
- The validity-window check on the enrolled certificate (`:149`).
- Fail closed: an empty or null chain is rejected (`:128-131`); there is no catch that turns a failure into acceptance.
- No trust-all fallback of any kind; CNTR-MODERNIZATION-001 Invariant 2 (`:173-176`).
- `getAcceptedIssuers()` returns the one enrolled certificate, never null and never empty (`:152-157`).
- No host-name verification on the pinned path: the pin replaces it (U-063 AC-20; `PinnedCertificateSocketFactoryCreateSocketTest.java:56-59`).
- A new TLS context for every `createSocket` call (`:78-79`). The rejection tests rely on it because a resumed session skips the certificate check (`PinnedCertificateSocketFactoryCreateSocketTest.java:71-74`).
- The constructor rejects a null certificate (`:68-70`), and both socket shapes (direct, layered) behave alike.
- The default factory's behaviour is not changed by this bug (BUG-019 owns that).

## Verification Steps

For QA, per acceptance criterion. Run Gradle in a shell without `MSYS_NO_PATHCONV`.

1. AC-1: search the workspace for "BUG-020" and open the decision record. Confirm the date, the decider, the outcome for each of protocols, cipher suites and server name, the reason wherever it differs, the link to the BUG-019 AC-5 decision, and that its date is before the fix's commit.
2. AC-2: run the single-test commands from Steps to Reproduce against the changed code. Confirm each updated assertion fails if the provider's defaults alone were returned, for the direct and the layered shape, and that the server-name assertion runs in the JDK-provider class.
3. AC-3: `git diff` shows the three test methods still present (renamed if needed) and none carries `@Ignore`. For each aspect, remove the handling locally, confirm a named test fails, restore it, and record the result next to the existing mutation log (`mutation-log.md`). Confirm the cipher-suite mutation was run.
4. AC-4: confirm the diff of `PinnedCertificateSocketFactory.java` is empty and the Javadoc passages cite the record.
5. AC-5: open the contract revision, find the pinned producer row, and confirm it states the decided behaviour; or confirm the AC-1 record gives the reason it does not.
6. AC-6: run `./gradlew :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest" --tests "com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryTest"` and confirm both pass. If `createSocket` was edited, repeat M-E4 and M-E5 and confirm AC-21 fails each time.
7. AC-7: run `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`; confirm exit 0, 0 failures, skipped count not higher. Authoritative test count: `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`; compare with the count before the change and explain any difference.

## Root Cause Analysis
<!-- Filled during planning phase -->

## Technical Context

- **Why severity is low (the analyst's reasoning, not a safety judgement).** The pinned path is opt-in: the resolver yields `PINNED_CERTIFICATE` only for a host the owner has enrolled, and only the enrolment flow sets it (`CNTR-MODERNIZATION-001-tls-socket-factory.md:215-216`; `DES-MODERNIZATION-002-...md:188-191`). The certificate identity is pinned, so, in the security review's words, an attacker needs that certificate's private key whatever the protocol version (`review-security.md:44`). Under the JDK test provider the pinned socket is, by inference, narrower than the default factory's. No exploit path has been shown, and nothing in U-063 connects to a real server. The missing server name is a functional risk (a server that routes by name could present another certificate, and the pin then fails closed), not a security one, per the same review.
- **Who judges.** Whether the behaviour is safe is the Security Reviewer's judgement. Whether the pinned factory should match the default one, and at what compatibility cost, is the product owner's decision, taken together with BUG-019 AC-5.
- **Test environment limits.** Robolectric at a simulated API level 29, JDK 25, the desktop build of Conscrypt, and the JDK's own provider for one class. Neither is the provider on the owner's devices. The default test provider reports no server names through socket parameters whatever is set, so the server-name check lives in the JDK-provider class (`U-063-...md:315`, `:326`).
- **Deployment facts.** The app is private and unpublished, for its owner's own use. Devices run Android 11 or newer and sign-in is by app password (`BUG-019-...md:231`). Whether the owner currently has any host enrolled was not checked, so it is not known whether this path is exercised in practice.
- **Server name on a device.** What goes on the wire on a device for the pinned path is not shown (`review-security.md:182`; feasibility analysis `:707` records the layered case as untested).

## Supporting Documentation

- `sdlc/artifacts/stories/BUG-019-factory-enables-tls10-and-tls11.md` (the default factory's protocol decision, AC-5)
- `sdlc/artifacts/build/sprints/sprint-020/U-063-tls-socket-factory-characterisation-tests.md` (AC-18 to AC-21, `:315`, `:326`, `:337`)
- `sdlc/artifacts/build/sprints/sprint-020/U-063/review-security.md` (W-3 `:42-45`; limits `:174-185`; recommendations `:191`)
- `sdlc/artifacts/build/sprints/sprint-020/U-063/qa-results.md` (`:173`, `:276-277`, `:285-286`)
- `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/I0-probe.txt`, `evidence/I0c-diag.txt`, `evidence/mutation-log.md`
- `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md` (silent on hardening and server name)
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-002-transport-security-hardening-design.md` (Decisions 2 and 3)
- `sdlc/artifacts/build/change-records/CR-009-cntr-001-default-trusted-socket-factory-producer.md` (`:86`, `:199`, `:213`, `:266`)
- `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` (`:707`, `:2194`, `:2208`, `:2212`)

## Implementation Notes
<!-- Added by agents during build -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes

- Filed from U-063 (sprint-020) on the product owner's approval of 2026-11-20, following security finding W-3 (Low) and QA's carry-forward. The behaviour is recorded by three passing characterisation tests; none is evidence of acceptability.
- Intended to be decided together with BUG-019, which concerns the same two factories from the other side. The fixes may ship separately.
